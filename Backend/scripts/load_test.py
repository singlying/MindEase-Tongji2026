#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
MindEase 性能压力测试工具
==========================
对核心 API 进行并发压测，检测系统在高负载下的性能表现。
输出响应时间、吞吐量、错误率等指标。

使用方式:
    python load_test.py                          # 默认配置运行
    python load_test.py --url http://localhost:8080
    python load_test.py --concurrent 50 --requests 1000  # 高并发模式
    python load_test.py --module ai --target /api/ai/chat  # 指定端点

依赖: pip install requests aiohttp (可选)
"""

import argparse
import asyncio
import json
import statistics
import sys
import threading
import time
from collections import defaultdict
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

try:
    import requests as sync_requests
except ImportError:
    print("[ERROR] 需要安装 requests: pip install requests")
    sys.exit(1)


# ============================================================
# 配置区域
# ============================================================

class Colors:
    GREEN = "\033[92m"
    RED = "\033[91m"
    YELLOW = "\033[93m"
    CYAN = "\033[96m"
    BOLD = "\033[1m"
    RESET = "\033[0m"


# 内置压测场景
LOAD_TEST_SCENARIOS = {
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
        "slow_threshold": 5.0,  # AI 接口可能较慢
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


class LoadTestResult:
    """单次请求的结果"""

    def __init__(self, success: bool, status_code: int, response_time_ms: float,
                 error_msg: str = ""):
        self.success = success
        self.status_code = status_code
        self.response_time_ms = response_time_ms
        self.error_msg = error_msg


class LoadTestReport:
    """压测报告汇总"""

    def __init__(self, scenario_name: str):
        self.scenario_name = scenario_name
        self.results: List[LoadTestResult] = []
        self.start_time: Optional[float] = None
        self.end_time: Optional[float] = None

    @property
    def total_requests(self) -> int:
        return len(self.results)

    @property
    def success_count(self) -> int:
        return sum(1 for r in self.results if r.success)

    @property
    def fail_count(self) -> int:
        return sum(1 for r in self.results if not r.success)

    @property
    def success_rate(self) -> float:
        return self.success_count / self.total_requests * 100 if self.total_requests > 0 else 0

    @property
    def avg_response_time(self) -> float:
        times = [r.response_time_ms for r in self.results]
        return statistics.mean(times) if times else 0

    @property
    def median_response_time(self) -> float:
        times = [r.response_time_ms for r in self.results]
        return statistics.median(times) if times else 0

    @property
    def p95_response_time(self) -> float:
        """P95 响应时间"""
        times = sorted(r.response_time_ms for r in self.results)
        if not times:
            return 0
        idx = int(len(times) * 0.95)
        return times[min(idx, len(times) - 1)]

    @property
    def p99_response_time(self) -> float:
        """P99 响应时间"""
        times = sorted(r.response_time_ms for r in self.results)
        if not times:
            return 0
        idx = int(len(times) * 0.99)
        return times[min(idx, len(times) - 1)]

    @property
    def min_response_time(self) -> float:
        times = [r.response_time_ms for r in self.results]
        return min(times) if times else 0

    @property
    def max_response_time(self) -> float:
        times = [r.response_time_ms for r in self.results]
        return max(times) if times else 0

    @property
    def std_response_time(self) -> float:
        times = [r.response_time_ms for r in self.results]
        return statistics.stdev(times) if len(times) > 1 else 0

    @property
    def throughput_per_sec(self) -> float:
        """每秒请求数 (RPS/QPS)"""
        duration = (self.end_time or 0) - (self.start_time or 0)
        return self.total_requests / duration if duration > 0 else 0

    def to_dict(self) -> Dict[str, Any]:
        return {
            "scenario": self.scenario_name,
            "total": self.total_requests,
            "success": self.success_count,
            "failed": self.fail_count,
            "success_rate_pct": round(self.success_rate, 2),
            "avg_ms": round(self.avg_response_time, 2),
            "median_ms": round(self.median_response_time, 2),
            "p95_ms": round(self.p95_response_time, 2),
            "p99_ms": round(self.p99_response_time, 2),
            "min_ms": round(self.min_response_time, 2),
            "max_ms": round(self.max_response_time, 2),
            "std_ms": round(self.std_response_time, 2),
            "throughput_rps": round(self.throughput_per_sec, 2),
        }


class LoadTester:
    """性能压力测试器"""

    def __init__(self, base_url: str = "http://localhost:8080",
                 timeout: int = 30):
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout
        self.token: Optional[str] = None
        self.reports: Dict[str, LoadTestReport] = {}
        self.lock = threading.Lock()

    def _log(self, msg: str, color: str = ""):
        print(f"{color}{msg}{Colors.RESET}")

    def _get_token(self) -> bool:
        """获取认证 Token"""
        try:
            resp = sync_requests.post(
                f"{self.base_url}/api/auth/login",
                json={"username": "test_user", "password": "Test123456"},
                timeout=self.timeout
            )
            if resp.status_code == 200:
                body = resp.json()
                self.token = body.get("data", {}).get("token") or body.get("token")
                return bool(self.token)
        except Exception:
            pass
        return False

    def _make_request(self, method: str, path: str,
                      payload: Optional[Dict] = None,
                      need_auth: bool = False) -> LoadTestResult:
        """执行单次请求并返回结果"""
        url = self.base_url + path
        headers = {}
        if need_auth and self.token:
            headers["Authorization"] = f"Bearer {self.token}"

        start = time.perf_counter()
        try:
            if payload and method == "POST":
                resp = sync_requests.request(method, url, json=payload,
                                             headers=headers, timeout=self.timeout)
            else:
                resp = sync_requests.request(method, url, headers=headers,
                                             timeout=self.timeout)

            elapsed = (time.perf_counter() - start) * 1000  # ms

            # 判断是否成功
            expected = 200
            is_success = resp.status_code == expected

            return LoadTestResult(
                success=is_success,
                status_code=resp.status_code,
                response_time_ms=elapsed,
            )
        except Exception as e:
            elapsed = (time.perf_counter() - start) * 1000
            return LoadTestResult(
                success=False,
                status_code=0,
                response_time_ms=elapsed,
                error_msg=str(e)[:100],
            )

    def run_scenario(self, scenario_key: str, scenario_config: Dict,
                     num_requests: int, concurrency: int) -> LoadTestReport:
        """运行单个压测场景"""
        report = LoadTestReport(scenario_key)
        report.start_time = time.time()

        method = scenario_config.get("method", "GET")
        path = scenario_config.get("path", "/")
        payload = scenario_config.get("payload")
        need_auth = scenario_config.get("need_auth", False)
        slow_threshold = scenario_config.get("slow_threshold", 1.0)

        self._log(f"\n{'─'*60}")
        self._log(f"📊 场景: {scenario_config.get('name', scenario_key)}", Colors.BOLD)
        self._log(f"   端点: {method} {path}")
        self._log(f"   请求数: {num_requests} | 并发数: {concurrency}")
        self._log(f"   慢请求阈值: {slow_threshold}s")
        self._log(f"{'─'*60}")

        completed = 0
        progress_interval = max(1, num_requests // 20)  # 每5%更新一次进度

        with ThreadPoolExecutor(max_workers=concurrency) as executor:
            futures = [
                executor.submit(self._make_request, method, path, payload, need_auth)
                for _ in range(num_requests)
            ]

            for future in as_completed(futures):
                result = future.result()
                with self.lock:
                    report.results.append(result)
                    completed += 1

                if completed % progress_interval == 0:
                    pct = completed / num_requests * 100
                    bar_len = int(pct / 5)
                    bar = "█" * bar_len + "░" * (20 - bar_len)
                    print(f"\r  进度: [{bar}] {pct:.0f}% ({completed}/{num_requests})",
                          end="", flush=True)

        report.end_time = time.time()
        print()  # 进度条换行

        self.reports[scenario_key] = report
        return report

    def _print_report(self, report: LoadTestReport):
        """打印单个场景报告"""
        d = report.to_dict()
        status_color = Colors.GREEN if d["success_rate_pct"] >= 95 else (
            Colors.YELLOW if d["success_rate_pct"] >= 80 else Colors.RED)

        print(f"\n  ┌─ {'═' * 54} ┐")
        print(f"  │  场景: {report.scenario_name}")
        print(f"  ├─ {'─' * 54} ┤")
        print(f"  │  总请求数:     {d['total']:<10} 成功: {d['success']} "
              f"失败: {d['failed']}")
        print(f"  │  成功率:       {status_color}{d['success_rate_pct']:.1f}%{Colors.RESET}")
        print(f"  │  吞吐量:       {d['throughput_rps']:.2f} req/s")
        print(f"  ├─ {'─' * 54} ┤")
        print(f"  │  响应时间统计 (ms):")
        print(f"  │    平均值:    {d['avg_ms']:>10.2f}")
        print(f"  │    中位数:    {d['median_ms']:>10.2f}")
        print(f"  │    P95:       {d['p95_ms']:>10.2f}")
        print(f"  │    P99:       {d['p99_ms']:>10.2f}")
        print(f"  │    最小值:    {d['min_ms']:>10.2f}")
        print(f"  │    最大值:    {d['max_ms']:>10.2f}")
        print(f"  │    标准差:    {d['std_ms']:>10.2f}")
        print(f"  └─ {'═' * 54} ┘")

        # 性能评估
        if d["success_rate_pct"] < 95:
            self._log(f"  ⚠️  成功率低于95%，需关注!", Colors.RED)
        if d["p99_ms"] > 5000:
            self._log(f"  ⚠️  P99超过5秒，存在慢请求风险!", Colors.RED)
        elif d["p95_ms"] > 2000:
            self._log(f"  ⚠️  P95超过2秒，建议优化", Colors.YELLOW)
        elif d["p95_ms"] < 200:
            self._log(f"  ✅ P95表现优秀 (<200ms)", Colors.GREEN)

    def run_all_scenarios(self, scenarios: List[str],
                          num_requests: int, concurrency: int,
                          warmup: int = 0):
        """运行所有指定场景（新增warmup预热参数）"""
        print("=" * 65)
        print(" MindEase Performance Load Test")
        print(f" Target: {self.base_url}")
        print(f" Requests per scenario: {num_requests}, Concurrency: {concurrency}")
        if warmup > 0:
            print(f" Warmup requests: {warmup} per scenario")
        print("=" * 65)

        # 获取 Token
        auth_needed = any(LOAD_TEST_SCENARIOS[s].get("need_auth", False)
                          for s in scenarios if s in LOAD_TEST_SCENARIOS)
        if auth_needed:
            self._log("\n[认证] 正在获取测试Token...")
            if self._get_token():
                self._log("[OK] Token 获取成功", Colors.GREEN)
            else:
                self._log("[WARN] Token 获取失败，需要认证的测试将跳过", Colors.YELLOW)

        overall_start = time.time()

        # 预热阶段
        if warmup > 0:
            self._log(f"\n{'─'*60}")
            self._log(f"🔥 预热阶段: 每场景 {warmup} 个预热请求", Colors.YELLOW)
            self._log(f"{'─'*60}")
            warmup_scenarios = [s for s in scenarios if s in LOAD_TEST_SCENARIOS]
            for scenario_key in warmup_scenarios[:3]:  # 仅对前3个场景预热
                config = LOAD_TEST_SCENARIOS[scenario_key]
                method = config.get("method", "GET")
                path = config.get("path", "/")
                payload = config.get("payload")
                need_auth = config.get("need_auth", False)
                self._make_request(method, path, payload, need_auth)
                time.sleep(0.2)  # 预热间隔
            self._log("[OK] 预热完成\n", Colors.GREEN)

        for scenario_key in scenarios:
            if scenario_key not in LOAD_TEST_SCENARIOS:
                self._log(f"[SKIP] 未知场景: {scenario_key}", Colors.YELLOW)
                continue

            config = LOAD_TEST_SCENARIOS[scenario_key]
            report = self.run_scenario(scenario_key, config, num_requests, concurrency)
            self._print_report(report)

        total_time = time.time() - overall_start

        # 总体汇总
        self._print_summary(total_time)

    def _print_summary(self, total_time: float):
        """打印总体汇总"""
        self._log(f"\n{'='*65}")
        self._log(" 📋 压测总体汇总", Colors.BOLD)
        self._log(f"{'='*65}")

        all_success = 0
        all_total = 0
        for key, report in self.reports.items():
            all_success += report.success_count
            all_total += report.total_requests

        print(f"\n  场景数:         {len(self.reports)}")
        print(f"  总请求数:       {all_total}")
        print(f"  总成功数:       {all_success}")
        print(f"  总成功率:       {all_success/all_total*100:.1f}%" if all_total else "")
        print(f"  总耗时:         {total_time:.1f}s")

        # 输出 JSON 结果供后续分析
        output_path = Path(__file__).parent / "load_test_results.json"
        results_json = {
            "generated_at": datetime.now().isoformat(),
            "base_url": self.base_url,
            "scenarios": {k: v.to_dict() for k, v in self.reports.items()},
        }
        with open(output_path, "w", encoding="utf-8") as f:
            json.dump(results_json, f, indent=2, ensure_ascii=False)
        self._log(f"\n  详细结果已保存: {output_path}", Colors.CYAN)

    def run_custom_target(self, target_url: str, method: str = "GET",
                          payload: Optional[str] = None,
                          num_requests: int = 100,
                          concurrency: int = 10):
        """对自定义目标进行压测"""
        # 解析自定义 URL 为 path
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
        }

        report = self.run_scenario("custom", custom_config, num_requests, concurrency)
        self._print_report(report)


def main():
    parser = argparse.ArgumentParser(
        description="MindEase 性能压力测试工具",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
示例:
  python load_test.py                              # 默认配置 (每场景100请求, 10并发)
  python load_test.py --concurrent 50 -n 500       # 高压模式
  python load_test.py --scenarios auth_login user_profile  # 指定场景
  python load_test.py --target /api/ai/chat POST '{"message":"hi"}'
        """,
    )
    parser.add_argument("--url", "-u", default="http://localhost:8080",
                        help="服务地址 (默认: http://localhost:8080)")
    parser.add_argument("--requests", "-n", type=int, default=100,
                        help="每个场景的请求数 (默认: 100)")
    parser.add_argument("--concurrent", "-c", type=int, default=10,
                        help="并发线程数 (默认: 10)")
    parser.add_argument("--timeout", "-t", type=int, default=30,
                        help="单个请求超时秒数 (默认: 30)")
    parser.add_argument("--scenarios", "-s", nargs="+",
                        choices=list(LOAD_TEST_SCENARIOS.keys()),
                        help="指定压测场景 (默认: 全部)")
    parser.add_argument("--target", default=None,
                        help="自定义目标路径，如 /api/ai/chat")
    parser.add_argument("--method", choices=["GET", "POST", "PUT", "DELETE"],
                        default="GET", help="自定义目标的HTTP方法")
    parser.add_argument("--payload", default=None,
                        help='自定义目标的JSON载荷 (如 \'{"message":"hello"}\')')
    parser.add_argument("--warmup", "-w", type=int, default=0,
                        help="每个场景的预热请求数 (默认: 0, 不预热)")

    args = parser.parse_args()

    tester = LoadTester(base_url=args.url, timeout=args.timeout)

    if args.target:
        tester.run_custom_target(
            target_url=args.target,
            method=args.method,
            payload=args.payload,
            num_requests=args.requests,
            concurrency=args.concurrent,
        )
    else:
        scenarios = args.scenarios or list(LOAD_TEST_SCENARIOS.keys())
        tester.run_all_scenarios(
            scenarios=scenarios,
            num_requests=args.requests,
            concurrency=args.concurrent,
            warmup=args.warmup,
        )


if __name__ == "__main__":
    main()
