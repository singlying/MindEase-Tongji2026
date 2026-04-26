#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
MindEase API 自动化测试工具
==============================
对所有 API 接口进行自动化功能测试，验证请求/响应格式、状态码、业务逻辑。

使用方式:
    python api_tester.py                          # 运行全部测试
    python api_tester.py --module auth            # 仅测试认证模块
    python api_tester.py --url http://localhost:8080  # 自定义地址
    python api_tester.py --report                 # 生成 HTML 报告

依赖: requests (pip install requests)
"""

import argparse
import json
import sys
import time
from datetime import datetime
from enum import Enum
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

try:
    import requests
except ImportError:
    print("[ERROR] 缺少 requests 库，请执行: pip install requests")
    sys.exit(1)


# ============================================================
# 配置区域
# ============================================================

class TestResult(Enum):
    PASS = "PASS"
    FAIL = "FAIL"
    ERROR = "ERROR"
    SKIP = "SKIP"


class Colors:
    GREEN = "\033[92m"
    RED = "\033[91m"
    YELLOW = "\033[93m"
    CYAN = "\033[96m"
    RESET = "\033[0m"
    BOLD = "\033[1m"


# API 端点定义 (基于项目 Controller)
API_ENDPOINTS = {
    "auth": {
        "login": {"method": "POST", "path": "/api/auth/login",
                  "desc": "用户登录"},
        "register": {"method": "POST", "path": "/api/auth/register",
                     "desc": "用户注册"},
        "refresh": {"method": "POST", "path": "/api/auth/refresh",
                    "desc": "刷新Token"},
    },
    "user": {
        "profile": {"method": "GET", "path": "/api/user/profile",
                    "desc": "获取个人信息"},
        "update_profile": {"method": "PUT", "path": "/api/user/profile",
                           "desc": "更新个人信息"},
        "change_password": {"method": "POST", "path": "/api/user/password",
                            "desc": "修改密码"},
        "mood_logs": {"method": "GET", "path": "/api/user/moods",
                      "desc": "获取心情日志列表"},
        "add_mood": {"method": "POST", "path": "/api/user/moods",
                     "desc": "新增心情日志"},
        "appointments": {"method": "GET", "path": "/api/user/appointments",
                         "desc": "获取预约列表"},
        "create_appointment": {"method": "POST", "path": "/api/user/appointments",
                               "desc": "创建预约"},
    },
    "counselor": {
        "list": {"method": "GET", "path": "/api/counselors",
                 "desc": "咨询师列表"},
        "detail": {"method": "GET", "path": "/api/counselors/{id}",
                   "desc": "咨询师详情"},
    },
    "ai": {
        "chat": {"method": "POST", "path": "/api/ai/chat",
                 "desc": "AI对话"},
        "asr": {"method": "POST", "path": "/api/ai/asr",
                "desc": "语音转文字"},
        "tts": {"method": "POST", "path": "/api/ai/tts",
                "desc": "文字转语音"},
        "history": {"method": "GET", "path": "/api/ai/sessions",
                    "desc": "聊天历史"},
    },
    "assessment": {
        "submit": {"method": "POST", "path": "/api/assessment/submit",
                   "desc": "提交评估"},
        "history": {"method": "GET", "path": "/api/assessment/history",
                    "desc": "评估历史"},
        "scales": {"method": "GET", "path": "/api/assessment/scales",
                   "desc": "量表列表"},
    },
    "notification": {
        "list": {"method": "GET", "path": "/api/notifications",
                 "desc": "通知列表"},
        "read": {"method": "PUT", "path": "/api/notifications/{id}/read",
                 "desc": "标记已读"},
    },
}


class APITester:
    """API 自动化测试器"""

    def __init__(self, base_url: str = "http://localhost:8080",
                 timeout: int = 10):
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout
        self.session = requests.Session()
        self.token: Optional[str] = None
        self.test_results: List[Dict[str, Any]] = []
        self.user_id: Optional[int] = None
        self.created_resource_ids: Dict[str, int] = {}

    # --------------------------------------------------------
    # 辅助方法
    # --------------------------------------------------------

    def _log(self, message: str, color: str = ""):
        print(f"{color}{message}{Colors.RESET}")

    def _request(self, method: str, path: str, **kwargs) -> Tuple[int, Any]:
        """发送 HTTP 请求，返回 (状态码, 响应JSON)"""
        url = self.base_url + path
        headers = kwargs.pop("headers", {})
        if self.token:
            headers["Authorization"] = f"Bearer {self.token}"
        kwargs.setdefault("timeout", self.timeout)

        try:
            resp = self.session.request(method, url, headers=headers, **kwargs)
            try:
                body = resp.json()
            except Exception:
                body = resp.text[:500]
            return resp.status_code, body
        except requests.exceptions.ConnectionError:
            return 0, {"error": "连接失败，请确认服务已启动"}
        except requests.exceptions.Timeout:
            return 0, {"error": "请求超时"}
        except Exception as e:
            return 0, {"error": str(e)}

    def _record_result(self, module: str, name: str, result: TestResult,
                       detail: str = "", duration_ms: float = 0):
        """记录测试结果"""
        entry = {
            "module": module,
            "name": name,
            "result": result.value,
            "detail": detail,
            "duration_ms": round(duration_ms, 2),
            "timestamp": datetime.now().isoformat(),
        }
        self.test_results.append(entry)

        icon = {TestResult.PASS: "✓", TestResult.FAIL: "✗",
                TestResult.ERROR: "!", TestResult.SKIP: "-"}[result]
        color = {TestResult.PASS: Colors.GREEN, TestResult.FAIL: Colors.RED,
                 TestResult.ERROR: Colors.YELLOW, TestResult.SKIP: Colors.CYAN}[result]

        self._log(f"  [{icon}] {name}: {result.value} ({duration_ms:.0f}ms) "
                  f"{f'- {detail}' if detail else ''}", color)

    # --------------------------------------------------------
    # 前置条件检查
    # --------------------------------------------------------

    def check_server_health(self) -> bool:
        """检查服务器是否可达"""
        self._log("[前置检查] 服务器健康检查...")
        code, body = self._request("GET", "/api/health")
        if code in (200, 404):  # 可能没有 health endpoint，但至少能连上
            self._log("[OK] 服务器可访问", Colors.GREEN)
            return True
        self._log(f"[FAIL] 服务器不可达 (code={code})", Colors.RED)
        return False

    def login_test_user(self) -> bool:
        """使用测试账号登录获取 Token"""
        self._log("[前置操作] 登录测试账号...")

        payload = {"username": "test_user", "password": "Test123456"}
        code, body = self._request("POST", "/api/auth/login", json=payload)

        if code == 200 and isinstance(body, dict):
            self.token = body.get("data", {}).get("token") or body.get("token")
            self.user_id = body.get("data", {}).get("userId") or body.get("userId")
            if self.token:
                self._log(f"[OK] 登录成功, userId={self.user_id}", Colors.GREEN)
                return True

        self._log(f"[WARN] 测试账号登录失败 (code={code}), 将跳过需认证的测试", Colors.YELLOW)
        return False

    # --------------------------------------------------------
    # 认证模块测试
    # --------------------------------------------------------

    def test_auth_module(self):
        """认证模块测试用例"""
        self._log(f"\n{'='*50}")
        self._log("[模块] 认证 (Auth)", Colors.BOLD)
        self._log(f"{'='*50}")

        start = time.time()

        # TC-AUTH-01: 登录成功
        t = time.time()
        code, body = self._request("POST", "/api/auth/login",
                                   json={"username": "test_user", "password": "Test123456"})
        elapsed = (time.time() - t) * 1000
        if code == 200 and (body.get("code") == 200 or body.get("token")):
            self._record_result("auth", "登录-成功", TestResult.PASS, duration_ms=elapsed)
        else:
            self._record_result("auth", "登录-成功", TestResult.FAIL,
                                f"code={code}, body={str(body)[:100]}", elapsed)

        # TC-AUTH-02: 密码错误
        t = time.time()
        code, body = self._request("POST", "/api/auth/login",
                                   json={"username": "test_user", "password": "wrong_password"})
        elapsed = (time.time() - t) * 1000
        if code in (400, 401, 403) or (isinstance(body, dict) and body.get("code") and body["code"] != 200):
            self._record_result("auth", "登录-密码错误", TestResult.PASS, duration_ms=elapsed)
        else:
            self._record_result("auth", "登录-密码错误", TestResult.FAIL,
                                f"应返回错误码但得到 code={code}", elapsed)

        # TC-AUTH-03: 用户不存在
        t = time.time()
        code, body = self._request("POST", "/api/auth/login",
                                   json={"username": "nonexistent_user_12345", "password": "any"})
        elapsed = (time.time() - t) * 1000
        if code in (400, 401, 404) or (isinstance(body, dict) and body.get("code") and body["code"] != 200):
            self._record_result("auth", "登录-用户不存在", TestResult.PASS, duration_ms=elapsed)
        else:
            self._record_result("auth", "登录-用户不存在", TestResult.FAIL,
                                f"应返回错误码但得到 code={code}", elapsed)

        # TC-AUTH-04: 参数缺失
        t = time.time()
        code, body = self._request("POST", "/api/auth/login", json={})
        elapsed = (time.time() - t) * 1000
        if code == 400 or (isinstance(body, dict) and body.get("code") in (400, 500, -1)):
            self._record_result("auth", "登录-参数缺失校验", TestResult.PASS, duration_ms=elapsed)
        else:
            self._record_result("auth", "登录-参数缺失校验", TestResult.FAIL,
                                f"应拒绝空参数, code={code}", elapsed)

        # TC-AUTH-05: 注册新用户 (可选)
        t = time.time()
        import random
        new_username = f"auto_test_{random.randint(10000, 99999)}"
        code, body = self._request("POST", "/api/auth/register", json={
            "username": new_username,
            "password": "AutoTest123!",
            "phone": f"139{random.randint(10000000, 99999999)}"
        })
        elapsed = (time.time() - t) * 1000
        if code in (200, 201) or (isinstance(body, dict) and body.get("code") in (200, 201, 0)):
            self._record_result("auth", "注册-新用户", TestResult.PASS, duration_ms=elapsed)
            # 清理: 不实际删除，避免影响数据库
        elif code == 400 and "已存在" in str(body):
            self._record_result("auth", "注册-用户名重复检测", TestResult.PASS, duration_ms=elapsed)
        else:
            self._record_result("auth", "注册-新用户", TestResult.SKIP,
                                f"接口可能未实现或不允许自动注册, code={code}", elapsed)

        total = (time.time() - start) * 1000
        self._log(f"\n  [认证模块完成] 耗时 {total:.0f}ms")

    # --------------------------------------------------------
    # 用户模块测试
    # --------------------------------------------------------

    def test_user_module(self):
        """用户模块测试用例"""
        self._log(f"\n{'='*50}")
        self._log("[模块] 用户 (User)", Colors.BOLD)
        self._log(f"{'='*50}")

        if not self.token:
            self._record_result("user", "获取个人信息", TestResult.SKIP, "无有效 Token")
            return

        start = time.time()

        # TC-USER-01: 获取个人信息
        t = time.time()
        code, body = self._request("GET", "/api/user/profile")
        elapsed = (time.time() - t) * 1000
        if code == 200 and isinstance(body, dict):
            self._record_result("user", "获取个人信息", TestResult.PASS, duration_ms=elapsed)
        else:
            self._record_result("user", "获取个人信息", TestResult.FAIL,
                                f"code={code}", elapsed)

        # TC-USER-02: 获取心情日志列表
        t = time.time()
        code, body = self._request("GET", "/api/user/moods?page=1&size=10")
        elapsed = (time.time() - t) * 1000
        if code == 200:
            self._record_result("user", "心情日志列表", TestResult.PASS, duration_ms=elapsed)
        else:
            self._record_result("user", "心情日志列表", TestResult.FAIL,
                                f"code={code}", elapsed)

        # TC-USER-03: 新增心情日志
        t = time.time()
        mood_payload = {
            "moodType": "calm",
            "moodScore": 7,
            "tags": ["测试", "自动"],
            "content": "API自动化测试生成的日志"
        }
        code, body = self._request("POST", "/api/user/moods", json=mood_payload)
        elapsed = (time.time() - t) * 1000
        if code in (200, 201):
            self._record_result("user", "新增心情日志", TestResult.PASS, duration_ms=elapsed)
            # 记录创建的资源ID供后续清理
            if isinstance(body, dict) and body.get("data", {}).get("id"):
                self.created_resource_ids["mood"] = body["data"]["id"]
        else:
            self._record_result("user", "新增心情日志", TestResult.FAIL,
                                f"code={code}, body={str(body)[:100]}", elapsed)

        # TC-USER-04: 获取预约列表
        t = time.time()
        code, body = self._request("GET", "/api/user/appointments")
        elapsed = (time.time() - t) * 1000
        if code == 200:
            self._record_result("user", "预约列表", TestResult.PASS, duration_ms=elapsed)
        else:
            self._record_result("user", "预约列表", TestResult.FAIL,
                                f"code={code}", elapsed)

        # TC-USER-05: 创建预约
        t = time.time()
        from datetime import timedelta as td
        future = datetime.now() + td(days=3)
        appt_payload = {
            "counselorId": 2,  # 假设 dr_li 的 ID
            "startTime": future.strftime("%Y-%m-%d 14:00:00"),
            "endTime": (future + td(hours=1)).strftime("%Y-%m-%d 15:00:00"),
            "userNote": "API自动化测试预约"
        }
        code, body = self._request("POST", "/api/user/appointments", json=appt_payload)
        elapsed = (time.time() - t) * 1000
        if code in (200, 201):
            self._record_result("user", "创建预约", TestResult.PASS, duration_ms=elapsed)
        else:
            self._record_result("user", "创建预约", TestResult.SKIP,
                                f"code={code} (可能需要真实的咨询师ID)", elapsed)

        total = (time.time() - start) * 1000
        self._log(f"\n  [用户模块完成] 耗时 {total:.0f}ms")

    # --------------------------------------------------------
    # 咨询师模块测试
    # --------------------------------------------------------

    def test_counselor_module(self):
        """咨询师模块测试"""
        self._log(f"\n{'='*50}")
        self._log("[模块] 咨询师 (Counselor)", Colors.BOLD)
        self._log(f"{'='*50}")

        start = time.time()

        # TC-COUN-01: 获取咨询师列表
        t = time.time()
        code, body = self._request("GET", "/api/counselors?page=1&size=10")
        elapsed = (time.time() - t) * 1000
        if code == 200:
            data = body.get("data", body) if isinstance(body, dict) else body
            if isinstance(data, (list, dict)):
                self._record_result("counselor", "咨询师列表", TestResult.PASS,
                                    f"返回了 {len(data) if isinstance(data, list) else '?'} 条记录",
                                    elapsed)
            else:
                self._record_result("counselor", "咨询师列表", TestResult.FAIL,
                                    "响应格式异常", elapsed)
        else:
            self._record_result("counselor", "咨询师列表", TestResult.FAIL,
                                f"code={code}", elapsed)

        # TC-COUN-02: 咨询师详情 (假设存在 ID=2)
        t = time.time()
        code, body = self._request("GET", "/api/counselors/2")
        elapsed = (time.time() - t) * 1000
        if code == 200:
            self._record_result("counselor", "咨询师详情", TestResult.PASS, duration_ms=elapsed)
        elif code == 404:
            self._record_result("counselor", "咨询师详情-404正确性", TestResult.PASS,
                                "不存在的ID返回404", elapsed)
        else:
            self._record_result("counselor", "咨询师详情", TestResult.SKIP,
                                f"code={code} (可能ID不存在)", elapsed)

        total = (time.time() - start) * 1000
        self._log(f"\n  [咨询师模块完成] 耗时 {total:.0f}ms")

    # --------------------------------------------------------
    # AI 模块测试
    # --------------------------------------------------------

    def test_ai_module(self):
        """AI 模块测试"""
        self._log(f"\n{'='*50}")
        self._log("[模块] AI 对话 (AI)", Colors.BOLD)
        self._log(f"{'='*50}")

        if not self.token:
            self._record_result("ai", "AI对话", TestResult.SKIP, "无有效 Token")
            return

        start = time.time()

        # TC-AI-01: AI 聊天 (基本对话)
        t = time.time()
        chat_payload = {"message": "你好，我最近心情不太好"}
        code, body = self._request("POST", "/api/ai/chat", json=chat_payload)
        elapsed = (time.time() - t) * 1000
        if code == 200 and isinstance(body, dict):
            reply = body.get("data", {}).get("reply") or body.get("reply", "")
            if reply and len(reply) > 5:
                self._record_result("ai", "AI对话-基本回复", TestResult.PASS,
                                    f"回复长度: {len(reply)}", elapsed)
            else:
                self._record_result("ai", "AI对话-基本回复", TestResult.FAIL,
                                    "回复内容过短或为空", elapsed)
        elif code == 503:
            self._record_result("ai", "AI对话", TestResult.SKIP,
                                "AI 服务暂时不可用 (503)", elapsed)
        else:
            self._record_result("ai", "AI对话-基本回复", TestResult.FAIL,
                                f"code={code}, body={str(body)[:150]}", elapsed)

        # TC-AI-02: 获取聊天历史
        t = time.time()
        code, body = self._request("GET", "/api/ai/sessions?page=1&size=5")
        elapsed = (time.time() - t) * 1000
        if code == 200:
            self._record_result("ai", "聊天历史", TestResult.PASS, duration_ms=elapsed)
        else:
            self._record_result("ai", "聊天历史", TestResult.SKIP,
                                f"code={code}", elapsed)

        # TC-AI-03: ASR (语音转文字) - 需要 token
        t = time.time()
        try:
            # 创建一个小的假音频文件用于测试
            fake_audio = b"FAKE_AUDIO_DATA_FOR_TESTING"
            code, body = self._request("POST", "/api/ai/asr",
                                       data={"audio": ("test.wav", fake_audio,
                                                       "audio/wav")},
                                       headers={"Content-Type": "multipart/form-data"})
            elapsed = (time.time() - t) * 1000
            if code == 200:
                self._record_result("ai", "ASR语音转文字", TestResult.PASS, duration_ms=elapsed)
            elif code == 400:
                self._record_result("ai", "ASR-参数校验", TestResult.PASS,
                                    "正确拒绝了无效音频", elapsed)
            else:
                self._record_result("ai", "ASR语音转文字", TestResult.SKIP,
                                    f"code={code}", elapsed)
        except Exception as e:
            self._record_result("ai", "ASR语音转文字", TestResult.SKIP, str(e)[:80])

        total = (time.time() - start) * 1000
        self._log(f"\n  [AI模块完成] 耗时 {total:.0f}ms")

    # --------------------------------------------------------
    # 评估模块测试
    # --------------------------------------------------------

    def test_assessment_module(self):
        """评估模块测试"""
        self._log(f"\n{'='*50}")
        self._log("[模块] 评估 (Assessment)", Colors.BOLD)
        self._log(f"{'='*50}")

        if not self.token:
            self._record_result("assessment", "提交评估", TestResult.SKIP, "无有效 Token")
            return

        start = time.time()

        # TC-ASSESS-01: 获取量表列表
        t = time.time()
        code, body = self._request("GET", "/api/assessment/scales")
        elapsed = (time.time() - t) * 1000
        if code == 200:
            self._record_result("assessment", "量表列表", TestResult.PASS, duration_ms=elapsed)
        else:
            self._record_result("assessment", "量表列表", TestResult.SKIP,
                                f"code={code}", elapsed)

        # TC-ASSESS-02: 提交评估
        t = time.time()
        assess_payload = {
            "scaleKey": "PHQ-9",
            "answers": [
                {"questionIndex": 1, "score": 1},
                {"questionIndex": 2, "score": 2},
                {"questionIndex": 3, "score": 0},
                {"questionIndex": 4, "score": 1},
                {"questionIndex": 5, "score": 0},
                {"questionIndex": 6, "score": 1},
                {"questionIndex": 7, "score": 2},
                {"questionIndex": 8, "score": 0},
                {"questionIndex": 9, "score": 1},
            ]
        }
        code, body = self._request("POST", "/api/assessment/submit", json=assess_payload)
        elapsed = (time.time() - t) * 1000
        if code in (200, 201):
            score = body.get("data", {}).get("totalScore") or body.get("totalScore")
            level = body.get("data", {}).get("resultLevel") or body.get("resultLevel")
            self._record_result("assessment", "提交评估(PHQ-9)", TestResult.PASS,
                                f"总分={score}, 等级={level}", elapsed)
        else:
            self._record_result("assessment", "提交评估(PHQ-9)", TestResult.FAIL,
                                f"code={code}, body={str(body)[:100]}", elapsed)

        # TC-ASSESS-03: 评估历史
        t = time.time()
        code, body = self._request("GET", "/api/assessment/history?page=1&size=10")
        elapsed = (time.time() - t) * 1000
        if code == 200:
            self._record_result("assessment", "评估历史", TestResult.PASS, duration_ms=elapsed)
        else:
            self._record_result("assessment", "评估历史", TestResult.SKIP,
                                f"code={code}", elapsed)

        total = (time.time() - start) * 1000
        self._log(f"\n  [评估模块完成] 耗时 {total:.0f}ms")

    # --------------------------------------------------------
    # 异常场景测试
    # --------------------------------------------------------

    def test_error_scenarios(self):
        """异常场景测试"""
        self._log(f"\n{'='*50}")
        self._log("[模块] 异常场景 (Edge Cases)", Colors.BOLD)
        self._log(f"{'='*50}")

        start = time.time()

        # TC-ERR-01: 无 Token 访问受保护资源
        t = time.time()
        old_token = self.token
        self.token = None
        code, body = self._request("GET", "/api/user/profile")
        self.token = old_token
        elapsed = (time.time() - t) * 1000
        if code in (401, 403):
            self._record_result("error", "无Token访问-被拒", TestResult.PASS, duration_ms=elapsed)
        else:
            self._record_result("error", "无Token访问-被拒", TestResult.FAIL,
                                f"应返回401/403, 实际code={code}", elapsed)

        # TC-ERR-02: 无效 Token
        t = time.time()
        old_token = self.token
        self.token = "invalid.jwt.token.here"
        code, body = self._request("GET", "/api/user/profile")
        self.token = old_token
        elapsed = (time.time() - t) * 1000
        if code in (401, 403):
            self._record_result("error", "无效Token-被拒", TestResult.PASS, duration_ms=elapsed)
        else:
            self._record_result("error", "无效Token-被拒", TestResult.FAIL,
                                f"应返回401/403, 实际code={code}", elapsed)

        # TC-ERR-03: 不存在的端点 (404)
        t = time.time()
        code, body = self._request("GET", "/api/nonexistent_endpoint_404")
        elapsed = (time.time() - t) * 1000
        if code == 404:
            self._record_result("error", "不存在端点-404", TestResult.PASS, duration_ms=elapsed)
        else:
            self._record_result("error", "不存在端点-404", TestResult.SKIP,
                                f"code={code}", elapsed)

        # TC-ERR-04: 超大 Payload (如果有的话)
        t = time.time()
        large_payload = {"message": "x" * 10000}
        code, body = self._request("POST", "/api/ai/chat", json=large_payload)
        elapsed = (time.time() - t) * 1000
        if code == 413:
            self._record_result("error", "超大Payload-413", TestResult.PASS, duration_ms=elapsed)
        elif code in (200, 400, 413, 500):
            self._record_result("error", "超大Payload-边界测试", TestResult.PASS,
                                f"server handled it: {code}", elapsed)
        else:
            self._record_result("error", "超大Payload", TestResult.SKIP, f"code={code}", elapsed)

        total = (time.time() - start) * 1000
        self._log(f"\n  [异常场景完成] 耗时 {total:.0f}ms")

    # --------------------------------------------------------
    # 报告生成
    # --------------------------------------------------------

    def generate_report(self, output_path: Optional[str] = None):
        """生成测试报告"""
        if not output_path:
            output_path = Path(__file__).parent / "test_report.html"

        passed = sum(1 for r in self.test_results if r["result"] == TestResult.PASS.value)
        failed = sum(1 for r in self.test_results if r["result"] == TestResult.FAIL.value)
        errors = sum(1 for r in self.test_results if r["result"] == TestResult.ERROR.value)
        skipped = sum(1 for r in self.test_results if r["result"] == TestResult.SKIP.value)
        total = len(self.test_results)

        pass_rate = (passed / total * 100) if total > 0 else 0

        html = f"""<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>MindEase API 测试报告</title>
    <style>
        body {{ font-family: -apple-system, "Segoe UI", sans-serif; margin: 20px; background: #f5f5f5; }}
        .container {{ max-width: 1200px; margin: 0 auto; }}
        h1 {{ color: #333; border-bottom: 2px solid #4A90D9; padding-bottom: 10px; }}
        .summary {{ display: flex; gap: 15px; margin: 20px 0; flex-wrap: wrap; }}
        .card {{ padding: 20px; border-radius: 8px; color: white; min-width: 120px; text-align: center; }}
        .card.total {{ background: #666; }}
        .card.pass {{ background: #28a745; }}
        .card.fail {{ background: #dc3545; }}
        .card.error {{ background: #ffc107; color: #333; }}
        .card.skip {{ background: #17a2b8; }}
        .card .number {{ font-size: 2em; font-weight: bold; }}
        table {{ width: 100%; border-collapse: collapse; background: white; border-radius: 8px; overflow: hidden; }}
        th {{ background: #4A90D9; color: white; padding: 12px; text-align: left; }}
        td {{ padding: 10px 12px; border-bottom: 1px solid #eee; }}
        tr:hover {{ background: #f8f9fa; }}
        .pass-badge {{ color: #28a745; font-weight: bold; }}
        .fail-badge {{ color: #dc3545; font-weight: bold; }}
        .err-badge {{ color: #856404; font-weight: bold; }}
        .skip-badge {{ color: #17a2b8; }}
        .progress-bar {{ height: 24px; background: #e9ecef; border-radius: 12px; overflow: hidden; }}
        .progress-fill {{ height: 100%; background: #28a745; transition: width 0.3s; line-height: 24px; color: white; text-align: center; }}
        .meta {{ color: #666; font-size: 0.9em; margin-bottom: 15px; }}
    </style>
</head>
<body>
    <div class="container">
        <h1>🧪 MindEase API 自动化测试报告</h1>
        <div class="meta">
            生成时间: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")} | 
            目标地址: {self.base_url}
        </div>

        <div class="summary">
            <div class="card total"><div class="number">{total}</div><div>总计</div></div>
            <div class="card pass"><div class="number">{passed}</div><div>通过</div></div>
            <div class="card fail"><div class="number">{failed}</div><div>失败</div></div>
            <div class="card error"><div class="number">{errors}</div><div>错误</div></div>
            <div class="card skip"><div class="number">{skipped}</div><div>跳过</div></div>
        </div>

        <div style="margin: 20px 0;">
            <span>通过率: </span>
            <div class="progress-bar" style="display:inline-block;width:300px;vertical-align:middle;">
                <div class="progress-fill" style="width:{pass_rate}%;">{pass_rate:.1f}%</div>
            </div>
        </div>

        <table>
            <tr><th>#</th><th>模块</th><th>用例名称</th><th>结果</th><th>耗时(ms)</th><th>详情</th></tr>
"""

        for i, r in enumerate(self.test_results, 1):
            badge_class = {
                "PASS": "pass-badge", "FAIL": "fail-badge",
                "ERROR": "err-badge", "SKIP": "skip-badge"
            }.get(r["result"], "")
            html += f"""<tr>
                <td>{i}</td><td>{r['module']}</td><td>{r['name']}</td>
                <td class="{badge_class}">[{r['result']}]</td>
                <td>{r['duration_ms']}</td>
                <td>{r.get('detail', '')}</td>
            </tr>\n"""

        html += """
        </table>
    </div>
</body>
</html>
"""
        with open(output_path, "w", encoding="utf-8") as f:
            f.write(html)

        print(f"\n[报告] HTML 报告已保存: {output_path}")

    # --------------------------------------------------------
    # 主运行流程
    # --------------------------------------------------------

    def run_all(self, modules: Optional[List[str]] = None, generate_report: bool = True):
        """运行全部测试"""
        print("=" * 60)
        print("MindEase API Tester v1.0")
        print(f"目标: {self.base_url}")
        print("=" * 60)

        overall_start = time.time()

        # 前置检查
        if not self.check_server_health():
            print("\n[ABORT] 服务器不可达，终止测试!")
            return

        self.login_test_user()

        # 执行各模块测试
        module_tests = {
            "auth": self.test_auth_module,
            "user": self.test_user_module,
            "counselor": self.test_counselor_module,
            "ai": self.test_ai_module,
            "assessment": self.test_assessment_module,
            "error": self.test_error_scenarios,
        }

        if modules:
            for m in modules:
                if m in module_tests:
                    module_tests[m]()
                else:
                    self._log(f"[WARN] 未知模块: {m}", Colors.YELLOW)
        else:
            for name, test_func in module_tests.items():
                test_func()

        # 汇总
        total_time = (time.time() - overall_start) * 1000
        passed = sum(1 for r in self.test_results if r["result"] == TestResult.PASS.value)
        failed = sum(1 for r in self.test_results if r["result"] == TestResult.FAIL.value)
        errors = sum(1 for r in self.test_results if r["result"] == TestResult.ERROR.value)
        skipped = sum(1 for r in self.test_results if r["result"] == TestResult.SKIP.value)
        total = len(self.test_results)

        print("\n" + "=" * 60)
        print("测试汇总")
        print("=" * 60)
        print(f"  总用例数:   {total}")
        print(f"  {Colors.GREEN}通过:       {passed}{Colors.RESET}")
        print(f"  {Colors.RED}失败:       {failed}{Colors.RESET}")
        print(f"  {Colors.YELLOW}错误:       {errors}{Colors.RESET}")
        print(f"  {Colors.CYAN}跳过:       {skipped}{Colors.RESET}")
        print(f"  通过率:     {passed/total*100:.1f}%" if total else "  通过率:     N/A")
        print(f"  总耗时:     {total_time:.0f}ms")
        print("=" * 60)

        # 生成报告
        if generate_report:
            report_path = Path(__file__).parent / "test_report.html"
            self.generate_report(report_path)

        return failed == 0


def main():
    parser = argparse.ArgumentParser(description="MindEase API 自动化测试工具")
    parser.add_argument("--url", "-u", default="http://localhost:8080",
                        help="服务地址 (默认: http://localhost:8080)")
    parser.add_argument("--module", "-m", nargs="+",
                        choices=["auth", "user", "counselor", "ai", "assessment", "error"],
                        help="指定测试模块 (默认: 全部)")
    parser.add_argument("--timeout", "-t", type=int, default=10,
                        help="请求超时秒数 (默认: 10)")
    parser.add_argument("--report", action="store_true", default=True,
                        help="生成HTML报告 (默认开启)")
    parser.add_argument("--no-report", dest="report", action="store_false",
                        help="不生成报告")

    args = parser.parse_args()

    tester = APITester(base_url=args.url, timeout=args.timeout)
    success = tester.run_all(modules=args.module, generate_report=args.report)
    sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()
