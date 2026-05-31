#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
MindEase 服务健康检查脚本
==========================
自动检测后端服务、数据库、Redis、外部AI服务等各组件的健康状态，
适用于 CI/CD 流水线、运维监控、开发调试。

使用方式:
    python health_check.py                       # 完整检查所有组件
    python health_check.py --quick               # 快速模式 (仅关键项)
    python health_check.py --json                # 输出 JSON 格式结果
    python health_check.py --webhook https://hooks.example.com/notify  # 告警通知

依赖: pip install requests pymysql redis (可选，部分功能降级)
"""

import argparse
import json
import os
import socket
import subprocess
import sys
import time
from dataclasses import dataclass, field
from datetime import datetime
from enum import Enum
from pathlib import Path
from typing import Any, Callable, Dict, List, Optional, Tuple


# ============================================================
# 数据结构定义
# ============================================================

class HealthStatus(Enum):
    HEALTHY = ("healthy", "✅", "\033[92m")      # 绿色
    DEGRADED = ("degraded", "⚠️", "\033[93m")     # 黄色
    UNHEALTHY = ("unhealthy", "❌", "\033[91m")    # 红色
    UNKNOWN = ("unknown", "❓", "\033[37m")        # 灰色

    def __init__(self, code: str, icon: str, color: str):
        self.code = code
        self.icon = icon
        self.color = color


@dataclass
class CheckResult:
    """单项检查结果"""
    name: str
    status: HealthStatus
    message: str = ""
    response_time_ms: float = 0
    details: Dict[str, Any] = field(default_factory=dict)
    timestamp: str = field(default_factory=lambda: datetime.now().isoformat())

    @property
    def formatted(self) -> str:
        icon = self.status.icon
        name = self.name
        rt = f"{self.response_time_ms:.0f}ms" if self.response_time_ms > 0 else ""
        msg = f" - {self.message}" if self.message else ""
        rt_str = f" ({rt})" if rt else ""
        return f"{self.status.color}{icon} [{name:>18s}] {self.status.code.upper():>12s}{rt_str}{msg}\033[0m"


@dataclass
class HealthReport:
    """整体健康报告"""
    checks: List[CheckResult] = field(default_factory=list)
    started_at: str = field(default_factory=datetime.now().isoformat)
    finished_at: str = ""

    @property
    def overall_status(self) -> HealthStatus:
        if not self.checks:
            return HealthStatus.UNKNOWN
        statuses = set(c.status for c in self.checks)
        if HealthStatus.UNHEALTHY in statuses:
            return HealthStatus.UNHEALTHY
        if HealthStatus.DEGRADED in statuses:
            return HealthStatus.DEGRADED
        if HealthStatus.UNKNOWN in statuses:
            return HealthStatus.DEGRADED  # 有未知项视为降级
        return HealthStatus.HEALTHY

    def to_dict(self) -> Dict[str, Any]:
        return {
            "overall_status": self.overall_status.code,
            "started_at": self.started_at,
            "finished_at": self.finished_at,
            "total_checks": len(self.checks),
            "summary": {
                s.value[0]: sum(1 for c in self.checks if c.status == s)
                for s in HealthStatus
            },
            "checks": [
                {
                    "name": c.name,
                    "status": c.status.code,
                    "message": c.message,
                    "response_time_ms": round(c.response_time_ms, 2),
                    "details": c.details,
                    "timestamp": c.timestamp,
                }
                for c in self.checks
            ],
        }


# ============================================================
# 健康检查器
# ============================================================

class HealthChecker:
    """服务健康检查器"""

    def __init__(self, base_url: str = "http://localhost:8080"):
        self.base_url = base_url.rstrip("/")
        self.report = HealthReport()

        # 从 application.yml 提取的默认配置
        self.db_config = {
            "host": os.environ.get("SPRING_DATASOURCE_HOST", "localhost"),
            "port": int(os.environ.get("SPRING_DATASOURCE_PORT", 3306)),
            "database": os.environ.get("SPRING_DATASOURCE_DATABASE", "mindease"),
            "username": os.environ.get("SPRING_DATASOURCE_USERNAME", "root"),
            "password": os.environ.get("SPRING_DATASOURCE_PASSWORD", ""),
        }
        self.redis_config = {
            "host": os.environ.get("REDIS_HOST", "localhost"),
            "port": int(os.environ.get("REDIS_PORT", 6379)),
        }

    def _check(self, name: str, check_func: Callable[..., Tuple[HealthStatus, str, float]],
               **kwargs) -> CheckResult:
        """执行单项检查并记录结果"""
        try:
            start = time.perf_counter()
            status, message, details = check_func(**kwargs)
            elapsed = (time.perf_counter() - start) * 1000
        except Exception as e:
            status = HealthStatus.UNHEALTHY
            message = f"检查异常: {str(e)[:150]}"
            elapsed = (time.perf_counter() - start) * 1000
            details = {}

        result = CheckResult(name=name, status=status, message=message,
                              response_time_ms=elapsed, details=details or {})
        self.report.checks.append(result)
        return result

    # --------------------------------------------------------
    # 各组件检查方法
    # --------------------------------------------------------

    def check_backend_service(self) -> Tuple[HealthStatus, str, dict]:
        """检查后端 Spring Boot 服务是否可达"""
        try:
            import urllib.request
            import urllib.error

            start = time.perf_counter()
            try:
                req = urllib.request.Request(f"{self.base_url}/api/health",
                                             headers={"Accept": "application/json"},
                                             method="GET")
                with urllib.request.urlopen(req, timeout=5) as resp:
                    body = json.loads(resp.read().decode())
                    elapsed = (time.perf_counter() - start) * 1000
                    return (
                        HealthStatus.HEALTHY,
                        f"服务正常, HTTP {resp.status}",
                        {"http_status": resp.status, "body": body, "response_time_ms": elapsed},
                    )
            except urllib.error.HTTPError as e:
                # 即使返回404也说明服务在运行
                if e.code < 500:
                    return (
                        HealthStatus.HEALTHY,
                        f"服务可达 (HTTP {e.code}, 可能无health endpoint)",
                        {"http_status": e.code},
                    )
                return HealthStatus.UNHEALTHY, f"服务器错误 HTTP {e.code}", {}
            except urllib.error.URLError as e:
                return HealthStatus.UNHEALTHY, f"连接失败: {str(e.reason)}", {}

        except ImportError:
            # fallback: 用 socket 检查端口
            return self._check_port("backend", 8080)

    def check_database_connection(self) -> Tuple[HealthStatus, str, dict]:
        """检查 MySQL 连接"""
        try:
            import pymysql
            start = time.perf_counter()
            conn = pymysql.connect(
                host=self.db_config["host"],
                port=self.db_config["port"],
                user=self.db_config["username"],
                password=self.db_config["password"],
                database=self.db_config["database"],
                connect_timeout=5,
                read_timeout=3,
            )
            elapsed = (time.perf_counter() - start) * 1000

            with conn.cursor() as cur:
                cur.execute("SELECT VERSION()")
                version = cur.fetchone()[0]
                cur.execute("SELECT COUNT(*) FROM information_schema.tables "
                            "WHERE table_schema=%s", (self.db_config["database"],))
                table_count = cur.fetchone()[0]

            conn.close()

            return (
                HealthStatus.HEALTHY,
                f"MySQL {version[:12]}, {table_count} 张表",
                {"version": version, "table_count": table_count, "db": self.db_config["database"]},
            )
        except ImportError:
            # 尝试用 mysql 命令行
            return self._check_mysql_cli()
        except Exception as e:
            return HealthStatus.UNHEALTHY, f"MySQL 连接失败: {str(e)[:100]}", {}

    def check_redis_connection(self) -> Tuple[HealthStatus, str, dict]:
        """检查 Redis 连接"""
        try:
            import redis
            start = time.perf_counter()
            client = redis.Redis(host=self.redis_config["host"],
                                 port=self.redis_config["port"],
                                 socket_connect_timeout=5,
                                 socket_timeout=3)
            info = client.info()
            elapsed = (time.perf_counter() - start) * 1000

            used_mem = info.get("used_memory_human", "N/A")
            connected_clients = info.get("connected_clients", 0)
            uptime = info.get("uptime_in_seconds", 0)

            return (
                HealthStatus.HEALTHY,
                f"Redis OK, {connected_clients} clients, 运行 {uptime}s",
                {"used_memory": used_mem, "clients": connected_clients,
                 "uptime_seconds": uptime, "response_time_ms": elapsed},
            )
        except ImportError:
            return self._check_redis_cli()
        except Exception as e:
            return HealthStatus.UNHEALTHY, f"Redis 连接失败: {str(e)[:100]}", {}

    def check_ai_service(self) -> Tuple[HealthStatus, str, dict]:
        """检查 DashScope AI 服务可用性"""
        api_key = os.environ.get("DASHSCOPE_API_KEY", "")
        if not api_key:
            return HealthStatus.UNKNOWN, "未配置 DASHSCOPE_API_KEY (跳过)", {}

        try:
            import urllib.request
            start = time.perf_counter()

            # 发送一个简单的模型调用测试
            data = json.dumps({
                "model": "qwen-turbo",
                "input": {"messages": [{"role": "user", "content": "hi"}]},
                "parameters": {"max_tokens": 5},
            }).encode()

            req = urllib.request.Request(
                "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation",
                data=data,
                headers={
                    "Content-Type": "application/json",
                    "Authorization": f"Bearer {api_key}",
                },
                method="POST",
            )

            with urllib.request.urlopen(req, timeout=15) as resp:
                body = json.loads(resp.read().decode())
                elapsed = (time.perf_counter() - start) * 1000

                if body.get("output"):
                    return (
                        HealthStatus.HEALTHY,
                        f"DashScope API 可用, 响应时间 {elapsed:.0f}ms",
                        {"model": "qwen-turbo", "response_time_ms": elapsed},
                    )
                else:
                    err = body.get("message", "未知错误")
                    return HealthStatus.DEGRADED, f"API 返回异常: {err[:80]}", {}

        except urllib.error.HTTPError as e:
            elapsed = (time.perf_counter() - start) * 1000
            body = e.read().decode() if hasattr(e, 'read') else ""
            return HealthStatus.UNHEALTHY, f"DashScope HTTP {e.code}: {body[:80]}", {}
        except Exception as e:
            return HealthStatus.UNKNOWN, f"无法验证 AI 服务: {str(e)[:100]}", {}

    def check_disk_space(self, path: str = ".") -> Tuple[HealthStatus, str, dict]:
        """检查磁盘空间"""
        import shutil
        try:
            usage = shutil.disk_usage(path)
            total_gb = usage.total / (1024 ** 3)
            free_gb = usage.free / (1024 ** 3)
            used_pct = usage.used / usage.total * 100

            if free_gb < 1:
                status = HealthStatus.UNHEALTHY
            elif free_gb < 5:
                status = HealthStatus.DEGRADED
            else:
                status = HealthStatus.HEALTHY

            return (
                status,
                f"{free_gb:.1f}GB 可用 / {total_gb:.1f}GB 总计 ({used_pct:.1f}% 已用)",
                {"total_gb": round(total_gb, 2), "free_gb": round(free_gb, 2),
                 "used_pct": round(used_pct, 1)},
            )
        except Exception as e:
            return HealthStatus.UNKNOWN, f"磁盘检查异常: {str(e)[:80]}", {}
        except NameError:
            import shutil
            return self.check_disk_space(path)  # 重试
        except Exception as e:
            return HealthStatus.UNKNOWN, f"磁盘检查异常: {str(e)[:80]}", {}

    def check_jvm_memory(self) -> Tuple[HealthStatus, str, dict]:
        """通过 actuator 或进程检查 JVM 内存状态"""
        # 尝试访问 Spring Boot Actuator
        try:
            import urllib.request
            req = urllib.request.Request(f"{self.base_url}/actuator/info",
                                         method="GET")
            with urllib.request.urlopen(req, timeout=3) as resp:
                body = json.loads(resp.read().decode())
                return HealthStatus.HEALTHY, "Actuator 可达", {"info": str(body)[:200]}
        except Exception:
            pass

        # fallback: 检查 Java 进程是否存在
        try:
            result = subprocess.run(["pgrep", "-f", "spring-boot:run|java.*mindease"],
                                    capture_output=True, text=True, timeout=5)
            pids = result.stdout.strip().split('\n')
            pids = [p for p in pids if p]
            if pids:
                # 尝试获取JVM内存使用情况
                mem_info = ""
                for pid in pids[:3]:  # 最多检查前3个进程
                    try:
                        mem_result = subprocess.run(
                            ["ps", "-o", "rss=", "-p", pid],
                            capture_output=True, text=True, timeout=3
                        )
                        rss_kb = int(mem_result.stdout.strip()) if mem_result.stdout.strip() else 0
                        rss_mb = rss_kb / 1024
                        mem_info += f" PID{pid[:6]}={rss_mb:.0f}MB"
                    except Exception:
                        pass
                return HealthStatus.HEALTHY, f"Java 进程运行中 ({mem_info})", {"pids": len(pids)}
            return HealthStatus.DEGRADED, "未检测到 Java 进程", {}
        except Exception:
            return HealthStatus.UNKNOWN, "无法检查 JVM 进程", {}

    def check_log_files(self, log_dir: str = "../logs") -> Tuple[HealthStatus, str, dict]:
        """检查日志文件大小，防止磁盘被日志占满"""
        log_path = Path(log_dir)
        if not log_path.exists():
            return HealthStatus.UNKNOWN, f"日志目录不存在: {log_dir}", {}

        total_size = 0
        file_count = 0
        large_files = []
        for log_file in list(log_path.rglob("*.log")) + list(log_path.rglob("*.gz")):
            size = log_file.stat().st_size
            total_size += size
            file_count += 1
            if size > 100 * 1024 * 1024:  # > 100MB
                large_files.append(f"{log_file.name}: {size / 1024 / 1024:.1f}MB")

        total_mb = total_size / 1024 / 1024

        if total_mb > 500:  # 超过500MB
            status = HealthStatus.UNHEALTHY
        elif total_mb > 200 or large_files:
            status = HealthStatus.DEGRADED
        else:
            status = HealthStatus.HEALTHY

        detail_msg = f"{file_count} 个文件, 总计 {total_mb:.1f}MB"
        if large_files:
            detail_msg += f"; 大文件: {', '.join(large_files[:2])}"

        return (
            status,
            detail_msg,
            {"total_mb": round(total_mb, 2), "file_count": file_count,
             "large_files": len(large_files)},
        )

    def check_connection_pool(self) -> Tuple[HealthStatus, str, dict]:
        """通过 Actuator 检查数据库连接池状态（需要 Spring Boot Actuator）"""
        try:
            import urllib.request
            req = urllib.request.Request(f"{self.base_url}/actuator/health",
                                        method="GET",
                                        headers={"Accept": "application/json"})
            with urllib.request.urlopen(req, timeout=5) as resp:
                body = json.loads(resp.read().decode())

                # 检查各组件状态
                components = body.get("components", {})
                db_status = components.get("db", {}).get("status", "unknown")
                disk_status = components.get("diskSpace", {}).get("status", "unknown")

                all_healthy = all(
                    c.get("status") == "UP" for c in components.values()
                    if isinstance(c, dict)
                ) if components else None

                if all_healthy is True:
                    return (
                        HealthStatus.HEALTHY,
                        f"所有组件健康 (DB={db_status}, Disk={disk_status})",
                        {"components": list(components.keys()), "detail": str(body)[:300]},
                    )
                elif db_status == "UP":
                    return HealthStatus.DEGRADED, f"DB正常但部分组件异常: {body}", {}
                else:
                    return HealthStatus.UNHEALTHY, f"组件异常: {body}", {}

        except urllib.error.HTTPError as e:
            if e.code == 404:
                return HealthStatus.UNKNOWN, "Actuator endpoint 未启用 (404)", {}
            return HealthStatus.DEGRADED, f"Actuator 返回 HTTP {e.code}", {}
        except Exception as e:
            return HealthStatus.UNKNOWN, "无法访问 Actuator 健康端点", {}

    # --------------------------------------------------------
    # Fallback 方法 (当缺少 Python 库时使用 CLI 工具)
    # --------------------------------------------------------

    def _check_port(self, service_name: str, port: int) -> Tuple[HealthStatus, str, dict]:
        """用 socket 检查端口是否开放"""
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(3)
        start = time.perf_counter()
        try:
            result = sock.connect_ex(("localhost", port))
            elapsed = (time.perf_counter() - start) * 1000
            sock.close()
            if result == 0:
                return HealthStatus.HEALTHY, f"端口 {port} 开放", {"port": port}
            return HealthStatus.UNHEALTHY, f"端口 {port} 未监听", {"port": port}
        except Exception as e:
            return HealthStatus.UNKNOWN, f"端口检查异常: {str(e)[:60]}", {}

    def _check_mysql_cli(self) -> Tuple[HealthStatus, str, dict]:
        """用 mysql 命令行检查"""
        try:
            result = subprocess.run(
                ["mysql", "-h", self.db_config["host"], "-P", str(self.db_config["port"]),
                 "-u", self.db_config["username"], "-e", "SELECT 1;"],
                capture_output=True, text=True, timeout=5,
            )
            if result.returncode == 0:
                return HealthStatus.HEALTHY, "MySQL CLI 连接成功", {}
            return HealthStatus.UNHEALTHY, f"mysql CLI 失败: {result.stderr[:80]}", {}
        except FileNotFoundError:
            return HealthStatus.UNKNOWN, "未安装 mysql 客户端 (跳过)", {}
        except Exception as e:
            return HealthStatus.UNKNOWN, f"MySQL 检查异常: {str(e)[:80]}", {}

    def _check_redis_cli(self) -> Tuple[HealthStatus, str, dict]:
        """用 redis-cli 检查"""
        try:
            result = subprocess.run(
                ["redis-cli", "-h", self.redis_config["host"], "-p",
                 str(self.redis_config["port"]), "ping"],
                capture_output=True, text=True, timeout=5,
            )
            if "PONG" in result.stdout:
                return HealthStatus.HEALTHY, "Redis CLI 连通 (PONG)", {}
            return HealthStatus.UNHEALTHY, f"redis-cli: {result.stdout.strip()[:60]}", {}
        except FileNotFoundError:
            return HealthStatus.UNKNOWN, "未安装 redis-cli (跳过)", {}
        except Exception as e:
            return HealthStatus.UNKNOWN, f"Redis 检查异常: {str(e)[:80]}", {}

    # --------------------------------------------------------
    # 主执行流程
    # --------------------------------------------------------

    def run_full_check(self, quick_mode: bool = False) -> HealthReport:
        """执行完整的健康检查"""
        print("=" * 70)
        print(" MindEase Service Health Checker")
        print(f" Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print(f" Target: {self.base_url}")
        print("=" * 70)

        # 核心检查 (始终执行)
        print(f"\n{'─'*70}")
        print(" 🔍 核心服务")
        print(f"{'─'*70}")

        self._check("Backend Service", self.check_backend_service)
        self._check("Database (MySQL)", self.check_database_connection)
        self._check("Redis Cache", self.check_redis_connection)

        # 扩展检查 (quick 模式跳过)
        if not quick_mode:
            print(f"\n{'─'*70}")
            print(" 🔍 扩展服务")
            print(f"{'─'*70}")

            self._check("AI Service (DashScope)", self.check_ai_service)
            self._check("Disk Space", lambda: self.check_disk_space("."))
            self._check("JVM Process", self.check_jvm_memory)

            print(f"\n{'─'*70}")
            print(" 🔍 运维诊断 (新增)")
            print(f"{'─'*70}")

            self._check("Connection Pool (Actuator)", self.check_connection_pool)
            self._check("Log Files", lambda: self.check_log_files("../logs"))

        # 输出结果
        self.report.finished_at = datetime.now().isoformat()
        self._print_results()

        return self.report

    def _print_results(self):
        """打印检查结果"""
        print(f"\n{'='*70}")
        print(" 📋 检查结果")
        print(f"{'='*70}")

        for check in self.report.checks:
            print(check.formatted)

        # 汇总
        overall = self.report.overall_status
        total = len(self.report.checks)
        summary = {s: sum(1 for c in self.report.checks if c.status == s)
                   for s in HealthStatus}

        print(f"\n{'─'*70}")
        print(f" 总体状态: {overall.color}{overall.icon} {overall.code.upper()}{' ' * 30}\033[0m")
        print(f" 汇总: "
              f"✅{summary.get(HealthStatus.HEALTHY, 0)}  "
              f"⚠️{summary.get(HealthStatus.DEGRADED, 0)}  "
              f"❌{summary.get(HealthStatus.UNHEALTHY, 0)}  "
              f"❓{summary.get(HealthStatus.UNKNOWN, 0)}"
              f"  共 {total} 项")

        # 建议
        if overall != HealthStatus.HEALTHY:
            self._print_recommendations()

        print(f"\n 耗时: {(datetime.fromisoformat(self.report.finished_at) - "
              f"datetime.fromisoformat(self.report.started_at)).total_seconds():.1f}s")
        print(f"{'='*70}")

    def _print_recommendations(self):
        """打印修复建议"""
        recommendations = {
            "Backend Service": [
                "确认 Spring Boot 应用已启动: mvn spring-boot:run 或 java -jar *.jar",
                "检查端口 8080 是否被占用: lsof -i :8080",
                "查看应用日志排查错误",
            ],
            "Database (MySQL)": [
                "确认 MySQL 服务已运行: brew services start mysql / systemctl start mysqld",
                "检查连接参数是否正确 (见 application.yml)",
                "测试连接: mysql -h localhost -u root -p mindease_db",
            ],
            "Redis Cache": [
                "确认 Redis 服务已运行: redis-server / systemctl start redis",
                "检查 Redis 端口: redis-cli ping",
                "如不需要缓存功能可暂时忽略此项",
            ],
            "AI Service (DashScope)": [
                "检查 DASHSCOPE_API_KEY 环境变量是否设置",
                "确认阿里云 DashScope 服务账户余额充足",
                "网络是否能访问 dashscope.aliyuncs.com (可能需要代理)",
            ],
            "Disk Space": [
                "清理不必要的文件释放磁盘空间",
                "检查日志文件大小: du -sh logs/",
                "清理 Maven 缓存: rm -rf ~/.m2/repository (谨慎)",
            ],
        }

        print(f"\n 💡 修复建议:")
        for check in self.report.checks:
            if check.status in (HealthStatus.UNHEALTHY, HealthStatus.DEGRADED):
                recs = recommendations.get(check.name, ["请参考项目文档进行排查"])
                print(f"\n   [{check.status.icon} {check.name}]")
                for rec in recs[:2]:  # 只显示前2条
                    print(f"     → {rec}")

    def send_webhook(self, webhook_url: str):
        """发送告警到 Webhook (企业微信/钉钉等)"""
        try:
            import urllib.request
            report_data = self.report.to_dict()

            # 构造消息体
            if "oapi.dingtalk.com" in webhook_url:
                # 钉钉格式
                msg = {
                    "msgtype": "markdown",
                    "markdown": {
                        "title": f"[{report_data['overall_status'].upper()}] MindEase 健康检查",
                        "text": f"""## MindEase 服务健康检查报告\n\n"""
                                f"""**状态**: {report_data['overall_status'].upper()}\n"""
                                f"""**时间**: {report_data['finished_at']}\n\n"""
                                f"""| 组件 | 状态 | 详情 |\n"""
                                f"""|------|------|------|\n""" +
                                "\n".join(
                                    f"| {c['name']} | {c['status']} | {c.get('message','')} |"
                                    for c in report_data['checks']
                                ),
                    },
                }
            else:
                # 通用格式
                msg = {
                    "text": f"[{report_data['overall_status'].upper()}] MindEase 健康检查完成",
                    "details": report_data,
                }

            data = json.dumps(msg).encode()
            req = urllib.request.Request(webhook_url, data=data,
                                         headers={"Content-Type": "application/json"})
            urllib.request.urlopen(req, timeout=10)
            print(f"\n[OK] 告警已发送至: {webhook_url}")
        except Exception as e:
            print(f"\n[WARN] Webhook 发送失败: {e}")


def main():
    parser = argparse.ArgumentParser(description="MindEase 服务健康检查工具")
    parser.add_argument("--url", "-u", default="http://localhost:8080",
                        help="服务地址 (默认: http://localhost:8080)")
    parser.add_argument("--quick", "-q", action="store_true",
                        help="快速模式 (仅检查后端+DB+Redis)")
    parser.add_argument("--json", action="store_true",
                        help="以 JSON 格式输出结果")
    parser.add_argument("--output", "-o", default=None,
                        help="保存结果到文件")
    parser.add_argument("--webhook", "-w", default=None,
                        help="告警通知 webhook URL (钉钉/企业微信)")
    parser.add_argument("--exit-code", action="store_true",
                        help="根据健康状况设置退出码 (0=healthy, 1=unhealthy)")

    args = parser.parse_args()

    checker = HealthChecker(base_url=args.url)
    report = checker.run_full_check(quick_mode=args.quick)

    # JSON 输出
    if args.json:
        print("\n--- JSON Output ---")
        print(json.dumps(report.to_dict(), indent=2, ensure_ascii=False))

    # 保存文件
    if args.output:
        with open(args.output, "w", encoding="utf-8") as f:
            json.dump(report.to_dict(), f, indent=2, ensure_ascii=False)
        print(f"\n结果已保存至: {args.output}")

    # Webhook 通知
    if args.webhook:
        checker.send_webhook(args.webhook)

    # 退出码
    if args.exit_code:
        sys.exit(0 if report.overall_status == HealthStatus.HEALTHY else 1)


if __name__ == "__main__":
    main()
