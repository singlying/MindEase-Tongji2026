#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
MindEase 性能压力测试工具 (异步重构版)
=======================================
对核心 API 进行并发压测，支持高并发、速率限制、详细统计和多格式报告。

使用方式:
    python load_test.py                          # 默认配置运行
    python load_test.py --url http://localhost:8080
    python load_test.py --concurrent 50 --requests 1000  # 高并发模式
    python load_test.py --module ai --target /api/ai/chat  # 指定端点
    python load_test.py --rate 20                # 限制每秒20个请求
    python load_test.py --report-format html     # 生成HTML报告

依赖: pip install aiohttp tqdm (可选)
"""

import argparse
import asyncio
import csv
import json
import sys
import time
from collections import defaultdict
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple, Union

try:
    import aiohttp
    from aiohttp import ClientTimeout, TCPConnector
except ImportError:
    print("[ERROR] 需要安装 aiohttp: pip install aiohttp")
    sys.exit(1)

try:
    from tqdm import tqdm
except ImportError:
    tqdm = None  # 降级使用简易进度


# ============================================================
# 配置与颜色
# ============================================================
class Colors:
    GREEN = "\033[92m"
    RED = "\033[91m"
    YELLOW = "\033[93m"
    CYAN = "\033[96m"
    BOLD = "\033[1m"
    RESET = "\033[0m"


# ============================================================
# 内置压测场景
# ============================================================
BUILTIN_SCENARIOS = {
    "auth_login": {
        "name": "登录接口",
        "method": "POST",
        "path": "/api/auth/login",
        "payload": {"username": "test_user", "password": "Test123456"},
        "headers": {},
        "expected_status": 200,
        "description": "测试用户登录接口的并发性能",
    },
    "user_profile": {
        "name": "获取个人信息",
        "method": "GET",
        "path": "/api/user/profile",
        "payload": None,
        "need_auth": True,
        "expected_status": 200,
    },
    "mood_list": {
        "name": "心情日志列表",
        "method": "GET",
        "path": "/api/user/moods?page=1&size=10",
        "payload": None,
        "need_auth": True,
        "expected_status": 200,
    },
    "counselor_list": {
        "name": "咨询师列表",
        "method": "GET",
        "path": "/api/counselors?page=1&size=10",
        "payload": None,
        "need_auth": False,
        "expected_status": 200,
    },
    "ai_chat": {
        "name": "AI对话",
        "method": "POST",
        "path": "/api/ai/chat",
        "payload": {"message": "你好，今天心情怎么样？"},
        "need_auth": True,
        "expected_status": 200,
        "slow_threshold": 5.0,
    },
    "assessment_submit": {
        "name": "提交评估",
        "method": "POST",
        "path": "/api/assessment/submit",
        "payload": {
            "scaleKey": "PHQ-9",
            "answers": [{"questionIndex": i, "score": i % 4} for i in range(1, 10)]
        },
        "need_auth": True,
        "expected_status": 200,
    },
    "notification_list": {
        "name": "通知列表",
        "method": "GET",
        "path": "/api/notifications?page=1&size=10",
        "payload": None,
        "need_auth": True,
        "expected_status": 200,
    },
    "dashboard_stats": {
        "name": "仪表盘统计",
        "method": "GET",
        "path": "/api/dashboard/stats",
        "payload": None,
        "need_auth": True,
        "expected_status": 200,
    },
}


# ============================================================
# 数据类
# ============================================================
@dataclass
class RequestResult:
    """单次请求结果"""
    success: bool
    status_code: int
    response_time_ms: float
    error_type: str = ""  # "timeout", "network", "http_error", "validation"
    error_msg: str = ""
    response_body: Optional[Dict] = None  # 用于验证


@dataclass
class ScenarioReport:
    """场景报告"""
    name: str
    results: List[RequestResult] = field(default_factory=list)
    start_time: float = 0.0
    end_time: float = 0.0

    @property
    def total_requests(self) -> int:
        return len(self.results)

    @property
    def success_count(self) -> int:
        return sum(1 for r in self.results if r.success)

    @property
    def fail_count(self) -> int:
        return self.total_requests - self.success_count

    @property
    def success_rate(self) -> float:
        return self.success_count / self.total_requests * 100 if self.total_requests else 0.0

    @property
    def response_times(self) -> List[float]:
        return [r.response_time_ms for r in self.results]

    def percentile(self, p: float) -> float:
        times = sorted(self.response_times)
        if not times:
            return 0.0
        idx = int(len(times) * p / 100)
        return times[min(idx, len(times)-1)]

    @property
    def avg(self) -> float:
        return sum(self.response_times) / len(self.response_times) if self.response_times else 0.0

    @property
    def median(self) -> float:
        return self.percentile(50)

    @property
    def p90(self) -> float:
        return self.percentile(90)

    @property
    def p95(self) -> float:
        return self.percentile(95)

    @property
    def p99(self) -> float:
        return self.percentile(99)

    @property
    def min_time(self) -> float:
        return min(self.response_times) if self.response_times else 0.0

    @property
    def max_time(self) -> float:
        return max(self.response_times) if self.response_times else 0.0

    @property
    def stddev(self) -> float:
        if len(self.response_times) < 2:
            return 0.0
        mean = self.avg
        variance = sum((x - mean) ** 2 for x in self.response_times) / (len(self.response_times) - 1)
        return variance ** 0.5

    @property
    def throughput(self) -> float:
        duration = self.end_time - self.start_time
        return self.total_requests / duration if duration > 0 else 0.0

    @property
    def error_types(self) -> Dict[str, int]:
        counts = defaultdict(int)
        for r in self.results:
            if not r.success:
                counts[r.error_type] += 1
        return dict(counts)

    @property
    def status_code_counts(self) -> Dict[int, int]:
        counts = defaultdict(int)
        for r in self.results:
            counts[r.status_code] += 1
        return dict(counts)

    def to_dict(self) -> Dict[str, Any]:
        return {
            "name": self.name,
            "total": self.total_requests,
            "success": self.success_count,
            "failed": self.fail_count,
            "success_rate_pct": round(self.success_rate, 2),
            "response_time_ms": {
                "avg": round(self.avg, 2),
                "median": round(self.median, 2),
                "p90": round(self.p90, 2),
                "p95": round(self.p95, 2),
                "p99": round(self.p99, 2),
                "min": round(self.min_time, 2),
                "max": round(self.max_time, 2),
                "stddev": round(self.stddev, 2),
            },
            "throughput_rps": round(self.throughput, 2),
            "status_code_counts": self.status_code_counts,
            "error_types": self.error_types,
        }


# ============================================================
# 异步压测引擎
# ============================================================
class AsyncLoadTester:
    def __init__(self, base_url: str, timeout: int = 30,
                 max_connections: int = 100, retry: int = 0,
                 rate_limit: Optional[float] = None):
        self.base_url = base_url.rstrip("/")
        self.timeout = ClientTimeout(total=timeout)
        self.retry = retry
        self.rate_limit = rate_limit
        self.token: Optional[str] = None
        self.reports: Dict[str, ScenarioReport] = {}
        self._session: Optional[aiohttp.ClientSession] = None
        self._semaphore: Optional[asyncio.Semaphore] = None
        self._rate_limiter: Optional["RateLimiter"] = None
        self._connector = TCPConnector(limit=max_connections, limit_per_host=max_connections)

    async def _get_token(self) -> bool:
        """获取认证 token (同步请求，仅用于初始化)"""
        try:
            async with aiohttp.ClientSession() as session:
                async with session.post(
                    f"{self.base_url}/api/auth/login",
                    json={"username": "test_user", "password": "Test123456"},
                    timeout=self.timeout
                ) as resp:
                    if resp.status == 200:
                        body = await resp.json()
                        self.token = body.get("data", {}).get("token") or body.get("token")
                        return bool(self.token)
        except Exception:
            pass
        return False

    async def _make_request(self, session: aiohttp.ClientSession,
                            method: str, path: str,
                            payload: Optional[Dict] = None,
                            headers: Optional[Dict] = None,
                            expected_status: int = 200,
                            validate_fields: Optional[List[str]] = None) -> RequestResult:
        """执行单个异步请求，带重试"""
        url = self.base_url + path
        req_headers = headers or {}
        if self.token:
            req_headers["Authorization"] = f"Bearer {self.token}"

        start = time.perf_counter()
        for attempt in range(self.retry + 1):
            try:
                async with session.request(
                    method, url, json=payload, headers=req_headers,
                    timeout=self.timeout
                ) as resp:
                    elapsed = (time.perf_counter() - start) * 1000
                    body = None
                    try:
                        body = await resp.json()
                    except:
                        body = None

                    # 检查状态码
                    if resp.status != expected_status:
                        return RequestResult(
                            success=False,
                            status_code=resp.status,
                            response_time_ms=elapsed,
                            error_type="http_error",
                            error_msg=f"expected {expected_status}, got {resp.status}",
                            response_body=body
                        )

                    # 校验响应字段（如果指定）
                    if validate_fields and body:
                        for field in validate_fields:
                            if field not in body:
                                return RequestResult(
                                    success=False,
                                    status_code=resp.status,
                                    response_time_ms=elapsed,
                                    error_type="validation",
                                    error_msg=f"missing field '{field}'",
                                    response_body=body
                                )

                    return RequestResult(
                        success=True,
                        status_code=resp.status,
                        response_time_ms=elapsed,
                        response_body=body
                    )

            except asyncio.TimeoutError:
                elapsed = (time.perf_counter() - start) * 1000
                if attempt == self.retry:
                    return RequestResult(
                        success=False,
                        status_code=0,
                        response_time_ms=elapsed,
                        error_type="timeout",
                        error_msg="request timeout"
                    )
                # 否则重试
            except aiohttp.ClientError as e:
                elapsed = (time.perf_counter() - start) * 1000
                if attempt == self.retry:
                    return RequestResult(
                        success=False,
                        status_code=0,
                        response_time_ms=elapsed,
                        error_type="network",
                        error_msg=str(e)[:100]
                    )
            except Exception as e:
                elapsed = (time.perf_counter() - start) * 1000
                return RequestResult(
                    success=False,
                    status_code=0,
                    response_time_ms=elapsed,
                    error_type="unknown",
                    error_msg=str(e)[:100]
                )
        # 理论上不会到达
        return RequestResult(success=False, status_code=0, response_time_ms=0, error_type="unknown")

    async def run_scenario(self, scenario_key: str, config: Dict,
                           num_requests: int, concurrency: int) -> ScenarioReport:
        """异步运行单个压测场景"""
        report = ScenarioReport(name=scenario_key)
        report.start_time = time.time()

        method = config.get("method", "GET")
        path = config.get("path", "/")
        payload = config.get("payload")
        need_auth = config.get("need_auth", False)
        expected_status = config.get("expected_status", 200)
        validate_fields = config.get("validate_fields", [])
        headers = config.get("headers", {})

        # 如果 need_auth 但 token 缺失，跳过
        if need_auth and not self.token:
            print(f"[SKIP] 场景 '{scenario_key}' 需要认证但无 Token")
            report.end_time = time.time()
            return report

        self._semaphore = asyncio.Semaphore(concurrency)
        self._rate_limiter = RateLimiter(self.rate_limit) if self.rate_limit else None

        async with aiohttp.ClientSession(connector=self._connector) as session:
            sem = self._semaphore
            limiter = self._rate_limiter

            async def bounded_request():
                async with sem:
                    if limiter:
                        await limiter.acquire()
                    return await self._make_request(
                        session, method, path, payload,
                        headers, expected_status, validate_fields
                    )

            # 并发提交所有任务
            tasks = [asyncio.create_task(bounded_request()) for _ in range(num_requests)]

            # 进度显示
            if tqdm:
                pbar = tqdm(total=num_requests, desc=f"{scenario_key}", unit="req")
                for coro in asyncio.as_completed(tasks):
                    result = await coro
                    report.results.append(result)
                    pbar.update(1)
                pbar.close()
            else:
                # 简易进度
                completed = 0
                for coro in asyncio.as_completed(tasks):
                    result = await coro
                    report.results.append(result)
                    completed += 1
                    if completed % max(1, num_requests // 20) == 0:
                        pct = completed / num_requests * 100
                        print(f"\r  进度: {pct:.0f}% ({completed}/{num_requests})", end="", flush=True)
                print()

        report.end_time = time.time()
        self.reports[scenario_key] = report
        return report

    def _print_report(self, report: ScenarioReport):
        """打印单个场景报告"""
        d = report.to_dict()
        status_color = Colors.GREEN if d["success_rate_pct"] >= 95 else (
            Colors.YELLOW if d["success_rate_pct"] >= 80 else Colors.RED)

        print(f"\n  ┌─ {'═' * 54} ┐")
        print(f"  │  场景: {report.name}")
        print(f"  ├─ {'─' * 54} ┤")
        print(f"  │  总请求数:     {d['total']:<10} 成功: {d['success']} "
              f"失败: {d['failed']}")
        print(f"  │  成功率:       {status_color}{d['success_rate_pct']:.1f}%{Colors.RESET}")
        print(f"  │  吞吐量:       {d['throughput_rps']:.2f} req/s")
        print(f"  ├─ {'─' * 54} ┤")
        print(f"  │  响应时间统计 (ms):")
        print(f"  │    平均值:    {d['response_time_ms']['avg']:>10.2f}")
        print(f"  │    中位数:    {d['response_time_ms']['median']:>10.2f}")
        print(f"  │    P90:       {d['response_time_ms']['p90']:>10.2f}")
        print(f"  │    P95:       {d['response_time_ms']['p95']:>10.2f}")
        print(f"  │    P99:       {d['response_time_ms']['p99']:>10.2f}")
        print(f"  │    最小值:    {d['response_time_ms']['min']:>10.2f}")
        print(f"  │    最大值:    {d['response_time_ms']['max']:>10.2f}")
        print(f"  │    标准差:    {d['response_time_ms']['stddev']:>10.2f}")
        if d["status_code_counts"]:
            codes = ", ".join(f"{k}:{v}" for k, v in d["status_code_counts"].items())
            print(f"  │  状态码分布:  {codes}")
        if d["error_types"]:
            errors = ", ".join(f"{k}:{v}" for k, v in d["error_types"].items())
            print(f"  │  错误类型:    {errors}")
        print(f"  └─ {'═' * 54} ┘")

        # 评估
        if d["success_rate_pct"] < 95:
            print(f"  ⚠️  成功率低于95%，需关注!", Colors.RED)
        if d["response_time_ms"]["p99"] > 5000:
            print(f"  ⚠️  P99超过5秒，存在慢请求风险!", Colors.RED)
        elif d["response_time_ms"]["p95"] > 2000:
            print(f"  ⚠️  P95超过2秒，建议优化", Colors.YELLOW)
        elif d["response_time_ms"]["p95"] < 200:
            print(f"  ✅ P95表现优秀 (<200ms)", Colors.GREEN)

    async def run_all_scenarios(self, scenarios: List[str],
                                num_requests: int, concurrency: int,
                                warmup: int = 0):
        """运行所有指定场景"""
        print("=" * 65)
        print(" MindEase Performance Load Test (Async)")
        print(f" Target: {self.base_url}")
        print(f" Requests per scenario: {num_requests}, Concurrency: {concurrency}")
        if self.rate_limit:
            print(f" Rate limit: {self.rate_limit} req/s")
        if warmup > 0:
            print(f" Warmup requests: {warmup} per scenario")
        print("=" * 65)

        # 获取 Token
        auth_needed = any(BUILTIN_SCENARIOS.get(s, {}).get("need_auth", False)
                          for s in scenarios if s in BUILTIN_SCENARIOS)
        if auth_needed:
            print("\n[认证] 正在获取测试Token...")
            if await self._get_token():
                print("[OK] Token 获取成功", Colors.GREEN)
            else:
                print("[WARN] Token 获取失败，需要认证的测试将跳过", Colors.YELLOW)

        overall_start = time.time()

        # 预热
        if warmup > 0:
            print(f"\n{'─'*60}")
            print(f"🔥 预热阶段: 每场景 {warmup} 个预热请求", Colors.YELLOW)
            print(f"{'─'*60}")
            warmup_scenarios = [s for s in scenarios if s in BUILTIN_SCENARIOS][:3]
            async with aiohttp.ClientSession(connector=self._connector) as session:
                for s in warmup_scenarios:
                    cfg = BUILTIN_SCENARIOS[s]
                    method = cfg.get("method", "GET")
                    path = cfg.get("path", "/")
                    payload = cfg.get("payload")
                    need_auth = cfg.get("need_auth", False)
                    headers = {}
                    if need_auth and self.token:
                        headers["Authorization"] = f"Bearer {self.token}"
                    try:
                        await session.request(method, self.base_url + path, json=payload, headers=headers,
                                              timeout=self.timeout)
                    except:
                        pass
                    await asyncio.sleep(0.2)
            print("[OK] 预热完成\n", Colors.GREEN)

        for scenario_key in scenarios:
            if scenario_key not in BUILTIN_SCENARIOS:
                print(f"[SKIP] 未知场景: {scenario_key}", Colors.YELLOW)
                continue

            config = BUILTIN_SCENARIOS[scenario_key]
            report = await self.run_scenario(scenario_key, config, num_requests, concurrency)
            self._print_report(report)

        total_time = time.time() - overall_start
        self._print_summary(total_time)

    def _print_summary(self, total_time: float):
        """打印总体汇总"""
        print(f"\n{'='*65}")
        print(" 📋 压测总体汇总", Colors.BOLD)
        print(f"{'='*65}")

        all_success = 0
        all_total = 0
        for report in self.reports.values():
            all_success += report.success_count
            all_total += report.total_requests

        print(f"\n  场景数:         {len(self.reports)}")
        print(f"  总请求数:       {all_total}")
        print(f"  总成功数:       {all_success}")
        print(f"  总成功率:       {all_success/all_total*100:.1f}%" if all_total else "")
        print(f"  总耗时:         {total_time:.1f}s")

    async def run_custom_target(self, target_url: str, method: str = "GET",
                                payload: Optional[str] = None,
                                num_requests: int = 100,
                                concurrency: int = 10):
        """对自定义目标进行压测"""
        if target_url.startswith(self.base_url):
            path = target_url[len(self.base_url):]
        else:
            path = target_url

        custom_config = {
            "name": f"Custom ({method} {path})",
            "method": method.upper(),
            "path": path,
            "payload": json.loads(payload) if payload else None,
            "need_auth": False,
            "expected_status": 200,
        }
        report = await self.run_scenario("custom", custom_config, num_requests, concurrency)
        self._print_report(report)


# ============================================================
# 速率限制器（令牌桶）
# ============================================================
class RateLimiter:
    def __init__(self, rate: float):
        self.rate = rate  # 请求/秒
        self.tokens = 1.0
        self.updated_at = time.monotonic()
        self._lock = asyncio.Lock()

    async def acquire(self):
        async with self._lock:
            now = time.monotonic()
            self.tokens += (now - self.updated_at) * self.rate
            self.updated_at = now
            if self.tokens > 1.0:
                self.tokens = 1.0
            if self.tokens < 1.0:
                wait = (1.0 - self.tokens) / self.rate
                await asyncio.sleep(wait)
                self.tokens = 0.0
            else:
                self.tokens -= 1.0


# ============================================================
# 报告导出
# ============================================================
def export_report_json(reports: Dict[str, ScenarioReport], output_path: Path):
    data = {
        "generated_at": datetime.now().isoformat(),
        "scenarios": {k: v.to_dict() for k, v in reports.items()}
    }
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)


def export_report_csv(reports: Dict[str, ScenarioReport], output_path: Path):
    with open(output_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(["场景", "总请求", "成功", "失败", "成功率%", "平均ms", "中位数ms", "P95ms", "P99ms", "吞吐量req/s"])
        for name, report in reports.items():
            d = report.to_dict()
            writer.writerow([
                name,
                d["total"],
                d["success"],
                d["failed"],
                d["success_rate_pct"],
                d["response_time_ms"]["avg"],
                d["response_time_ms"]["median"],
                d["response_time_ms"]["p95"],
                d["response_time_ms"]["p99"],
                d["throughput_rps"],
            ])


def export_report_html(reports: Dict[str, ScenarioReport], output_path: Path):
    html = """<!DOCTYPE html><html><head><meta charset="UTF-8"><title>压测报告</title>
    <style>body{font-family:sans-serif;margin:20px;background:#f5f5f5}
    table{border-collapse:collapse;width:100%;background:#fff;box-shadow:0 0 10px rgba(0,0,0,0.1)}
    th,td{padding:8px 12px;border:1px solid #ddd;text-align:right}
    th{background:#2c3e50;color:white}
    td:first-child{text-align:left;font-weight:bold}
    .good{color:green}.bad{color:red}.warn{color:orange}
    </style></head><body><h1>压测报告</h1>"""
    html += f"<p>生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}</p>"
    html += "<table><tr><th>场景</th><th>总请求</th><th>成功</th><th>失败</th><th>成功率%</th><th>平均ms</th><th>P95ms</th><th>P99ms</th><th>吞吐量(req/s)</th></tr>"
    for name, report in reports.items():
        d = report.to_dict()
        rate = d["success_rate_pct"]
        color = "good" if rate >= 95 else ("warn" if rate >= 80 else "bad")
        html += f"<tr><td>{name}</td><td>{d['total']}</td><td>{d['success']}</td><td>{d['failed']}</td>"
        html += f"<td class='{color}'>{rate:.1f}</td>"
        html += f"<td>{d['response_time_ms']['avg']:.2f}</td>"
        html += f"<td>{d['response_time_ms']['p95']:.2f}</td>"
        html += f"<td>{d['response_time_ms']['p99']:.2f}</td>"
        html += f"<td>{d['throughput_rps']:.2f}</td></tr>"
    html += "</table></body></html>"
    with open(output_path, "w", encoding="utf-8") as f:
        f.write(html)


# ============================================================
# 场景文件加载（YAML/JSON）
# ============================================================
def load_scenarios_from_file(filepath: Path) -> Dict[str, Dict]:
    """从 JSON 或 YAML 文件加载自定义场景（YAML需要 pyyaml）"""
    ext = filepath.suffix.lower()
    if ext == ".json":
        with open(filepath, "r", encoding="utf-8") as f:
            data = json.load(f)
        return data
    elif ext in (".yaml", ".yml"):
        try:
            import yaml
            with open(filepath, "r", encoding="utf-8") as f:
                data = yaml.safe_load(f)
            return data
        except ImportError:
            print("[ERROR] 需要安装 pyyaml 来读取 YAML 文件: pip install pyyaml")
            sys.exit(1)
    else:
        raise ValueError(f"不支持的文件格式: {ext}")


# ============================================================
# 主入口
# ============================================================
async def main_async(args):
    # 合并内置场景和自定义场景
    global BUILTIN_SCENARIOS
    if args.scenario_file:
        custom_scenarios = load_scenarios_from_file(Path(args.scenario_file))
        # 覆盖同名的内置场景
        BUILTIN_SCENARIOS.update(custom_scenarios)

    tester = AsyncLoadTester(
        base_url=args.url,
        timeout=args.timeout,
        max_connections=args.concurrent * 2,
        retry=args.retry,
        rate_limit=args.rate
    )

    if args.target:
        await tester.run_custom_target(
            target_url=args.target,
            method=args.method,
            payload=args.payload,
            num_requests=args.requests,
            concurrency=args.concurrent,
        )
    else:
        scenarios = args.scenarios or list(BUILTIN_SCENARIOS.keys())
        await tester.run_all_scenarios(
            scenarios=scenarios,
            num_requests=args.requests,
            concurrency=args.concurrent,
            warmup=args.warmup,
        )

    # 导出报告
    if args.report_format:
        out_dir = Path(".")
        base_name = f"load_test_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
        if args.report_format == "json":
            export_report_json(tester.reports, out_dir / f"{base_name}.json")
            print(f"  报告已导出为 JSON: {base_name}.json")
        elif args.report_format == "csv":
            export_report_csv(tester.reports, out_dir / f"{base_name}.csv")
            print(f"  报告已导出为 CSV: {base_name}.csv")
        elif args.report_format == "html":
            export_report_html(tester.reports, out_dir / f"{base_name}.html")
            print(f"  报告已导出为 HTML: {base_name}.html")


def main():
    parser = argparse.ArgumentParser(
        description="MindEase 性能压力测试工具 (异步版)",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
示例:
  python load_test.py                              # 默认配置 (每场景100请求, 10并发)
  python load_test.py --concurrent 50 -n 500       # 高压模式
  python load_test.py --scenarios auth_login user_profile  # 指定场景
  python load_test.py --target /api/ai/chat POST '{"message":"hi"}'
  python load_test.py --rate 20                    # 限速20 req/s
  python load_test.py --report-format html         # 生成HTML报告
  python load_test.py --scenario-file custom_scenarios.json  # 加载自定义场景
        """,
    )
    parser.add_argument("--url", "-u", default="http://localhost:8080",
                        help="服务地址 (默认: http://localhost:8080)")
    parser.add_argument("--requests", "-n", type=int, default=100,
                        help="每个场景的请求数 (默认: 100)")
    parser.add_argument("--concurrent", "-c", type=int, default=10,
                        help="并发数 (默认: 10)")
    parser.add_argument("--timeout", "-t", type=int, default=30,
                        help="单个请求超时秒数 (默认: 30)")
    parser.add_argument("--scenarios", "-s", nargs="+",
                        choices=list(BUILTIN_SCENARIOS.keys()),
                        help="指定压测场景 (默认: 全部)")
    parser.add_argument("--target", default=None,
                        help="自定义目标路径，如 /api/ai/chat")
    parser.add_argument("--method", choices=["GET", "POST", "PUT", "DELETE"],
                        default="GET", help="自定义目标的HTTP方法")
    parser.add_argument("--payload", default=None,
                        help='自定义目标的JSON载荷 (如 \'{"message":"hello"}\')')
    parser.add_argument("--warmup", "-w", type=int, default=0,
                        help="每个场景的预热请求数 (默认: 0)")
    parser.add_argument("--rate", "-r", type=float, default=None,
                        help="限制每秒请求数 (默认: 不限)")
    parser.add_argument("--retry", type=int, default=0,
                        help="失败重试次数 (默认: 0)")
    parser.add_argument("--report-format", choices=["json", "csv", "html"], default=None,
                        help="导出报告格式 (json/csv/html)")
    parser.add_argument("--scenario-file", type=str, default=None,
                        help="从 JSON/YAML 文件加载自定义场景")

    args = parser.parse_args()

    try:
        asyncio.run(main_async(args))
    except KeyboardInterrupt:
        print("\n[ABORT] 用户中断")
        sys.exit(1)


if __name__ == "__main__":
    main()