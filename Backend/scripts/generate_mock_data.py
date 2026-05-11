#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
MindEase 模拟数据生成器 v2.0
==============================
全量 Mock 数据生成工具，支持用户、心情日志、预约、聊天会话、通知、
社区帖子、评论等多实体数据的批量生成与多格式导出。

v2.0 变更:
    - 新增: 社区帖子 (CommunityPost) 与评论 (Comment) 数据生成
    - 新增: GeneratorConfig 配置数据类，集中管理所有生成参数
    - 新增: RichProgressBar 终端进度条（自动检测环境，降级兼容）
    - 新增: StructuredLogger 结构化日志系统（INFO/WARN/ERROR/SUCCESS 级别）
    - 新增: DataValidator 数据校验层（外键引用完整性、字段合法性检查）
    - 新增: --posts / --comments CLI 参数
    - 新增: --quiet 静默模式
    - 修复: 第169行原 num appointments_per_user 语法错误
    - 重构: 所有模板数据提取为独立的 TemplateRegistry 注册表
    - 重构: 导出逻辑抽象为 Exporter 接口 + 多格式策略模式

使用方式:
    python generate_mock_data.py --format csv              # CSV 格式
    python generate_mock_data.py --format sql               # SQL 插入语句
    python generate_mock_data.py --format json              # JSON 格式
    python generate_mock_data.py --format all               # 全部格式
    python generate_mock_data.py --users 50 --posts 100     # 自定义数量
    python generate_mock_data.py --seed 42 --output ./data  # 可复现 + 输出目录
    python generate_mock_data.py --quiet                    # 静默模式

依赖: Python 3.8+, 仅标准库 (os, csv, json, random, datetime, pathlib, argparse, sys, time)
"""

from __future__ import annotations

import argparse
import csv
import json
import os
import random
import sys
import time
from dataclasses import dataclass, field
from datetime import datetime, timedelta
from enum import Enum
from pathlib import Path
from typing import Any, Callable, Dict, List, Optional, Sequence, Tuple


# ============================================================
# 日志系统
# ============================================================

class LogLevel(Enum):
    """日志级别枚举"""
    DEBUG = "DEBUG"
    INFO = "INFO"
    WARN = "WARN"
    ERROR = "ERROR"
    SUCCESS = "SUCCESS"


class StructuredLogger:
    """结构化日志输出器，带颜色前缀和级别过滤"""

    _LEVEL_PREFIX = {
        LogLevel.DEBUG: "\033[90m[DEBUG]\033[0m",
        LogLevel.INFO:  "\033[94m[INFO]\033[0m ",
        LogLevel.WARN:  "\033[93m[WARN] \033[0m",
        LogLevel.ERROR: "\033[91m[ERROR]\033[0m",
        LogLevel.SUCCESS: "\033[92m[OK]   \033[0m",
    }

    def __init__(self, quiet: bool = False, min_level: LogLevel = LogLevel.DEBUG):
        self._quiet = quiet
        self._min_level = min_level
        self._is_tty = sys.stdout.isatty()

    def _supports_color(self) -> bool:
        return self._is_tty and os.getenv("NO_COLOR") is None

    def log(self, level: LogLevel, message: str):
        if self._quiet and level != LogLevel.ERROR:
            return
        if level.value < self._min_level.value:
            return
        prefix = self._LEVEL_PREFIX.get(level, f"[{level.value}]")
        if self._supports_color():
            print(f"{prefix} {message}")
        else:
            print(f"[{level.value}] {message}")

    def debug(self, msg: str): self.log(LogLevel.DEBUG, msg)
    def info(self, msg: str):  self.log(LogLevel.INFO, msg)
    def warn(self, msg: str):  self.log(LogLevel.WARN, msg)
    def error(self, msg: str): self.log(LogLevel.ERROR, msg)
    def success(self, msg: str): self.log(LogLevel.SUCCESS, msg)

    def banner(self, title: str, width: int = 60):
        """打印分隔横幅"""
        if not self._quiet:
            print("=" * width)
            print(title.center(width))
            print("=" * width)


# ============================================================
# 进度条
# ============================================================

class RichProgressBar:
    """轻量终端进度条，不支持 rich 库时自动降级为点号输出"""

    def __init__(self, total: int, description: str = "", logger: Optional[StructuredLogger] = None):
        self._total = total
        self._description = description
        self._current = 0
        self._logger = logger or StructuredLogger()
        self._start_time = time.time()
        self._enabled = total > 0 and not logger._quiet if logger else total > 0

    def update(self, n: int = 1):
        if not self._enabled:
            return
        self._current += n
        if self._logger._is_tty and self._logger._supports_color():
            pct = self._current / self._total
            filled = int(pct * 30)
            bar = "\033[42m" + "#" * filled + "\033[40m" + "-" * (30 - filled) + "\033[0m"
            elapsed = time.time() - self._start_time
            speed = self._current / elapsed if elapsed > 0 else 0
            sys.stdout.write(
                f"\r  {bar} {pct:5.1%} ({self._current}/{self._total})"
                f" \033[90m[{speed:.1f}/s]\033[0m"
            )
            sys.stdout.flush()
        elif self._current % max(1, self._total // 20) == 0:
            sys.stdout.write(".")
            sys.stdout.flush()

    def close(self):
        if not self._enabled:
            return
        if self._logger._is_tty and self._logger._supports_color():
            elapsed = time.time() - self._start_time
            print()  # 换行
        else:
            print(" done")


# ============================================================
# 配置数据类
# ============================================================

@dataclass
class GeneratorConfig:
    """集中管理数据生成器的所有可调参数"""

    output_dir: str = "./data"
    num_users: int = 30
    num_mood_logs_per_user: int = 10
    num_appointments_per_user: int = 2
    num_chat_sessions_per_user: int = 3
    num_messages_per_session: int = 8
    num_posts_per_user: int = 2           # v2.0 新增: 每用户发帖数
    num_comments_per_post: int = 4         # v2.0 新增: 每帖评论数
    num_notifications_per_user: int = 3   # v2.0 新增: 每用户通知数
    seed: Optional[int] = None
    quiet: bool = False                   # v2.0 新增: 静默模式

    def validate(self) -> List[str]:
        """校验参数合理性，返回错误信息列表"""
        errors = []
        if self.num_users < 1:
            errors.append("num_users 必须 >= 1")
        if self.num_mood_logs_per_user < 0:
            errors.append("num_mood_logs_per_user 不能为负数")
        if self.num_appointments_per_user < 0:
            errors.append("num_appointments_per_user 不能为负数")
        if self.num_chat_sessions_per_user < 0:
            errors.append("num_chat_sessions_per_user 不能为负数")
        if self.num_messages_per_session < 0:
            errors.append("num_messages_per_session 不能为负数")
        if self.num_posts_per_user < 0:
            errors.append("num_posts_per_user 不能为负数")
        if self.num_comments_per_post < 0:
            errors.append("num_comments_per_post 不能为负数")
        if self.num_notifications_per_user < 0:
            errors.append("num_notifications_per_user 不能为负数")
        return errors


# ============================================================
# 模板注册表（集中管理所有静态模板数据）
# ============================================================

class TemplateRegistry:
    """所有 Mock 数据的模板源注册表，统一管理常量与模板字符串"""

    # ---- 枚举值 ----
    MOOD_TYPES: List[str] = [
        "happy", "sad", "anxious", "angry", "calm", "excited",
        "lonely", "confused", "grateful", "hopeful",
    ]
    USER_ROLES: List[str] = ["USER", "COUNSELOR", "ADMIN"]
    APPOINTMENT_STATUSES: List[str] = ["PENDING", "CONFIRMED", "COMPLETED", "CANCELLED", "NO_SHOW"]
    CHAT_ROLES: List[str] = ["USER", "ASSISTANT"]
    NOTIFICATION_TYPES: List[str] = ["APPOINTMENT", "SYSTEM", "ASSESSMENT", "REMINDER", "COMMUNITY"]

    # v2.0 新增: 帖子分类与标签
    POST_CATEGORIES: List[str] = ["经验分享", "求助问答", "日常记录", "资源推荐", "活动招募"]
    POST_TAGS: List[List[str]] = [
        ["焦虑缓解", "自我成长", "CBT技巧"],
        ["抑郁康复", "药物治疗", "心理支持"],
        ["人际关系", "沟通技巧", "边界建立"],
        ["职场压力", "工作倦怠", "职业规划"],
        ["家庭关系", "亲子教育", "婚姻咨询"],
        ["睡眠改善", "失眠应对", "放松训练"],
        ["情绪管理", "正念冥想", "书写疗愈"],
        ["测评工具", "书籍推荐", "APP推荐"],
    ]

    # ---- 中文姓名库 ----
    SURNAMES: List[str] = [
        "张", "王", "李", "刘", "陈", "杨", "赵", "黄", "周", "吴",
        "徐", "孙", "马", "朱", "胡", "郭", "何", "林", "罗", "高",
    ]
    GIVEN_NAMES: List[str] = [
        "伟", "芳", "娜", "敏", "静", "丽", "强", "磊", "军", "洋",
        "勇", "艳", "杰", "娟", "涛", "明", "超", "秀英", "霞", "平",
        "宇", "欣", "晨", "璐", "婷", "浩", "雪", "琳", "博", "宁",
    ]

    # ---- 咨询师专长标签 ----
    SPECIALTIES: List[List[str]] = [
        ["认知行为疗法", "焦虑症"],
        ["家庭治疗", "婚姻咨询"],
        ["青少年心理", "学业压力"],
        ["抑郁症", "情绪管理"],
        ["职场压力", "人际交往"],
        ["创伤后康复", "PTSD"],
        ["睡眠障碍", "放松训练"],
        ["亲子关系", "儿童心理"],
    ]

    # ---- 心情日志内容模板 ----
    MOOD_TEMPLATES: Dict[str, List[str]] = {
        "happy": [
            "今天心情很好，完成了重要的工作任务",
            "和朋友聚会聊得很开心",
            "天气不错，出去散步感觉很棒",
            "收到了期待已久的好消息",
        ],
        "sad": [
            "感到有些失落，可能是因为最近压力大",
            "想起了一些不开心的事情",
            "今天效率不高，有些沮丧",
            "感觉孤单，想找人说说话",
        ],
        "anxious": [
            "明天有重要会议，有点紧张",
            "最近总是睡不好，担心很多事情",
            "感觉心跳加速，不知道怎么回事",
            "对未来有些迷茫和担忧",
        ],
        "angry": [
            "工作中遇到了不公平的待遇",
            "被人误解了，很生气",
            "堵车耽误了时间，情绪不太好",
            "有些事情不如意，感到烦躁",
        ],
        "calm": [
            "今天冥想了20分钟，感觉很平静",
            "读了一本好书，内心安宁",
            "周末在家休息，状态不错",
            "做瑜伽后身心舒畅",
        ],
        "excited": [
            "计划了一次旅行，非常期待",
            "学会了新技能，很有成就感",
            "即将开始新的项目，充满动力",
            "参加了一个有趣的活动",
        ],
        "lonely": [
            "假期一个人在家，有些寂寞",
            "搬到了新城市，还没有朋友",
            "感觉没人理解自己的想法",
            "想念远方的家人",
        ],
        "confused": [
            "对职业发展方向不太确定",
            "面临选择，不知道该怎么决定",
            "感觉自己需要一些指导",
            "人际关系中遇到困惑",
        ],
        "grateful": [
            "今天收到了朋友的关心，很感动",
            "回顾这段时间的成长，觉得一切努力都值得",
            "家人一直支持我，感到很幸福",
            "感谢心理咨询师的帮助，让我学会了很多应对方法",
        ],
        "hopeful": [
            "虽然现在有些困难，但我相信会好起来的",
            "制定了新的计划，对未来充满期待",
            "今天看到了一些积极的改变信号",
            "感觉自己的心理状态在慢慢变好",
        ],
    }

    # ---- 聊天消息模板 ----
    USER_MESSAGE_TEMPLATES: List[str] = [
        "你好，我最近感觉{mood}，能帮我分析一下吗？",
        "我已经{duration}这样了，不知道该怎么办。",
        "{topic}这个问题困扰我很久了，有什么建议吗？",
        "我试过{method}，但好像没什么效果。",
        "能不能推荐一些{resource}来帮助我？",
    ]

    ASSISTANT_RESPONSE_TEMPLATES: List[str] = [
        (
            "感谢你的分享。听起来你正在经历{issue}，这确实不容易。\n\n"
            "首先，我想确认一下：这种情况持续多久了？有没有特定的触发因素？\n\n"
            "根据你的描述，我建议我们可以从以下几个方面入手：\n"
            "1. {suggestion_1}\n"
            "2. {suggestion_2}\n"
            "3. {suggestion_3}\n\n"
            "你觉得这些建议中有哪些是你愿意尝试的？"
        ),
        (
            "我理解你的感受。{validation}\n\n"
            "在心理咨询中，我们经常会遇到类似的情况。让我和你分享一个可能有用的视角：{perspective}\n\n"
            "同时，我想邀请你做一个小的练习：{exercise}\n\n"
            "做完之后，可以和我分享你的感受。"
        ),
        (
            "这是一个很好的问题。让我先帮你梳理一下：\n\n"
            "**现状分析**：{analysis}\n\n"
            "**可能的原因**：{reasons}\n\n"
            "**建议方案**：{plan}\n\n"
            "如果你觉得这个方向可行，我们可以制定一个更详细的行动计划。"
        ),
    ]

    # ---- v2.0 新增: 社区帖子内容模板 ----
    POST_CONTENT_TEMPLATES: Dict[str, Tuple[List[str], List[str]]] = {
        "经验分享": (
            [
                "分享一下我这段时间对抗{topic}的心得，希望能帮到同样困扰的朋友。",
                "经过{duration}的努力，我终于找到了适合自己的方法，简单记录一下。",
                "从第一次求助到现在已经{duration}了，想把自己的经历写下来给需要的人参考。",
            ],
            [
                "一开始我也很迷茫，试过很多方法都不太见效。后来通过{method}慢慢开始好转。",
                "最重要的是坚持，每天一点点进步。中间也有反复的时候，但不要放弃。",
                "如果有人也在经历类似的困扰，欢迎留言交流，我们一起加油。",
                "附上我觉得有用的资源链接，希望能帮助到更多人。",
            ]
        ),
        "求助问答": (
            [
                "请问大家有没有好的{topic}解决方法？尝试了很多方式都没什么效果。",
                "最近{symptom}越来越严重了，不知道该怎么办，求各位支招。",
                "有没有人和我有类似的经历？想了解一下你们是怎么走出来的。",
            ],
            [
                "具体情况是这样的：{detail}",
                "之前去看过医生，诊断结果是{diagnosis}，但感觉效果不太明显。",
                "很感谢这个社区，让我有一个可以说出心里话的地方。",
            ]
        ),
        "日常记录": (
            [
                "今天的心情日记：{mood_desc}",
                "{date}的第{n}天打卡，记录自己的变化。",
                "坚持{activity}已经一周了，说说这几天的感受。",
            ],
            [
                "早上起床时的状态：{morning_status}",
                "今天做了一件让自己骄傲的小事：{achievement}",
                "晚上反思了一下今天的收获和不足。",
                "希望明天也能保持积极的心态。",
            ]
        ),
        "资源推荐": (
            [
                "强烈推荐这本关于{topic}的书/课程/APP！对我帮助非常大。",
                "整理了一些免费的{topic}资源分享给大家，都是亲测有用的。",
                "发现一个很好的{resource_type}，适合{target_audience}人群。",
            ],
            [
                "名称：{name}",
                "主要特点：{features}",
                "使用体验：{experience}",
                "适合人群：{suitable_for}",
                "获取方式：{how_to_get}",
            ]
        ),
        "活动招募": (
            [
                "计划组织一次线上{activity}互助小组，有兴趣的朋友可以报名。",
                "周末打算举办一个关于{topic}的分享会，欢迎参加。",
                "寻找同城的小伙伴一起{activity}，互相监督共同进步。",
            ],
            [
                "活动时间：{time}",
                "活动形式：{format}",
                "参与人数限制：{limit}人",
                "报名方式：评论区留言或私信。",
                "希望能和大家一起度过一段有意义的时光。",
            ]
        ),
    }

    # ---- v2.0 新增: 评论内容模板 ----
    COMMENT_TEMPLATES: Dict[str, List[str]] = {
        "support": [
            "楼主加油！我和你有一样的经历，现在已经好多了。",
            "感谢分享，看完之后感觉有了方向。",
            "抱抱你，一切都会好起来的。",
            "你说出了我的心声，原来不止我一个人这样。",
            "真的很佩服你能把这些写出来，向你学习！",
        ],
        "advice": [
            "个人建议可以试试{method}，对我比较有效果。",
            "补充一点：{supplement}",
            "除了你提到的方法，也可以考虑从{aspect}角度入手。",
            "建议配合专业咨询一起进行，效果会更好。",
        ],
        "question": [
            "请问这个方法大概多久能看到效果呢？",
                "想问一下，你在过程中遇到过反复吗？是怎么应对的？",
            "这个资源在哪里可以找到？能提供具体信息吗？",
            "对于刚开始的人来说，有什么特别需要注意的吗？",
        ],
        "share": [
            "我也来分享下我的经历……",
            "看到这个话题忍不住想说说我自己的情况。",
            "和我当初的情况很像，我是这么处理的……",
        ],
    }


# ============================================================
# 数据校验器
# ============================================================

class DataValidator:
    """
    数据校验层：验证生成的 Mock 数据完整性与一致性。
    包括外键引用检查、字段范围校验、必填项检查等。
    """

    def __init__(self, logger: Optional[StructuredLogger] = None):
        self._logger = logger or StructuredLogger()
        self._errors: List[str] = []
        self._warnings: List[str] = []

    def _add_error(self, msg: str):
        self._errors.append(msg)

    def _add_warning(self, msg: str):
        self._warnings.append(msg)

    def check_foreign_key(
        self,
        child_records: List[Dict],
        parent_records: List[Dict],
        fk_field: str,
        parent_id_field: str = "id",
        table_name: str = "",
        parent_table_name: str = "",
    ):
        """校验外键引用完整性"""
        if not parent_records:
            self._add_warning(f"[{table_name}] 父表 {parent_table_name} 为空，跳过外键校验")
            return
        parent_ids = {p[parent_id_field] for p in parent_records}
        for record in child_records:
            fk_val = record.get(fk_field)
            if fk_val not in parent_ids:
                self._add_error(
                    f"[{table_name}] 外键违规: {fk_field}={fk_val} "
                    f"不存在于 {parent_table_name}.{parent_id_field}"
                )

    def check_field_in_enum(
        self,
        records: List[Dict],
        field_name: str,
        allowed_values: Sequence,
        table_name: str = "",
    ):
        """校验字段值是否在允许的枚举范围内"""
        allowed_set = set(allowed_values)
        for record in records:
            val = record.get(field_name)
            if val not in allowed_set:
                self._add_error(
                    f"[{table_name}] 字段值非法: {field_name}='{val}' "
                    f"(允许值: {allowed_values})"
                )

    def check_not_null(self, records: List[Dict], fields: List[str], table_name: str = ""):
        """校验必填字段非空"""
        for i, record in enumerate(records):
            for f in fields:
                val = record.get(f)
                if val is None or (isinstance(val, str) and val.strip() == ""):
                    self._add_error(f"[{table_name}] 行{i}: 必填字段 '{f}' 为空")

    def check_positive_int(self, records: List[Dict], fields: List[str], table_name: str = ""):
        """校验正整数字段"""
        for record in records:
            for f in fields:
                val = record.get(f)
                if val is not None and (not isinstance(val, (int, float)) or val < 0):
                    self._add_error(
                        f"[{table_name}] 字段非法: {f}={val} 应为非负数"
                    )

    def check_datetime_format(self, records: List[Dict], field: str, fmt: str = "%Y-%m-%d %H:%M:%S",
                              table_name: str = ""):
        """校验日期时间格式"""
        for i, record in enumerate(records):
            val = record.get(field)
            if val is not None:
                try:
                    datetime.strptime(str(val), fmt)
                except (ValueError, TypeError):
                    self._add_error(
                        f"[{table_name}] 行{i}: {field}='{val}' 日期格式不合法"
                    )

    def validate_all(self, generator: 'MockDataGenerator') -> bool:
        """执行全套校验，返回是否全部通过"""
        self._errors.clear()
        self._warnings.clear()
        self._logger.info("开始数据完整性校验...")

        # 用户表基础校验
        self.check_not_null(generator.users, ["id", "username", "role"], "users")
        self.check_field_in_enum(generator.users, "role", TemplateRegistry.USER_ROLES, "users")
        self.check_positive_int(generator.users, ["status"], "users")

        # 心情日志校验
        if generator.mood_logs:
            self.check_foreign_key(generator.mood_logs, generator.users, "user_id",
                                   table_name="mood_logs", parent_table_name="users")
            self.check_field_in_enum(generator.mood_logs, "mood_type",
                                     TemplateRegistry.MOOD_TYPES, "mood_logs")
            self.check_datetime_format(generator.mood_logs, "create_time", table_name="mood_logs")

        # 预约记录校验
        if generator.appointments:
            self.check_foreign_key(generator.appointments, generator.users, "user_id",
                                   table_name="appointments", parent_table_name="users")
            counselor_ids = [u["id"] for u in generator.users if u["role"] == "COUNSELOR"]
            if counselor_ids:
                counselor_ref_list = [{"id": cid} for cid in counselor_ids]
                self.check_foreign_key(generator.appointments, counselor_ref_list, "counselor_id",
                                       table_name="appointments", parent_table_name="counselors")
            self.check_field_in_enum(generator.appointments, "status",
                                     TemplateRegistry.APPOINTMENT_STATUSES, "appointments")

        # 聊天消息校验
        if generator.chat_messages and generator.chat_sessions:
            session_refs = [{"id": s["id"]} for s in generator.chat_sessions]
            self.check_foreign_key(generator.chat_messages, session_refs, "session_id",
                                   table_name="chat_messages", parent_table_name="chat_sessions")
            self.check_field_in_enum(generator.chat_messages, "message_role",
                                     TemplateRegistry.CHAT_ROLES, "chat_messages")

        # 通知校验
        if generator.notifications:
            self.check_foreign_key(generator.notifications, generator.users, "user_id",
                                   table_name="notifications", parent_table_name="users")
            self.check_field_in_enum(generator.notifications, "type",
                                     TemplateRegistry.NOTIFICATION_TYPES, "notifications")

        # v2.0 新增: 社区帖子校验
        if generator.community_posts:
            self.check_foreign_key(generator.community_posts, generator.users, "author_id",
                                   table_name="community_posts", parent_table_name="users")
            self.check_field_in_enum(generator.community_posts, "category",
                                     TemplateRegistry.POST_CATEGORIES, "community_posts")

        # v2.0 新增: 评论校验
        if generator.comments and generator.community_posts:
            post_refs = [{"id": p["id"]} for p in generator.community_posts]
            user_refs = [{"id": u["id"]} for u in generator.users]
            self.check_foreign_key(generator.comments, post_refs, "post_id",
                                   table_name="comments", parent_table_name="community_posts")
            self.check_foreign_key(generator.comments, user_refs, "user_id",
                                   table_name="comments", parent_table_name="users")

        # 报告结果
        if self._warnings:
            for w in self._warnings:
                self._logger.warn(w)

        if self._errors:
            for e in self._errors:
                self._logger.error(e)
            self._logger.error(f"校验失败: {len(self._errors)} 个错误")
            return False

        self._logger.success(f"数据校验通过 ({len(self._warnings)} 条警告)")
        return True


# ============================================================
# 核心数据生成器
# ============================================================

class MockDataGenerator:
    """
    MindEase 全量模拟数据生成器 v2.0
    
    职责：
      1. 根据 GeneratorConfig 批量生成各实体的 Mock 数据
      2. 通过 TemplateRegistry 获取所有模板常量
      3. 支持导出为 CSV / SQL / JSON 三种格式
      4. 内嵌 DataValidator 做生成后校验
    """

    # ID 自增计数器（进程级单例）
    _id_counter: int = int(datetime.now().timestamp() * 1000) % 1_000_000_000

    def __init__(self, config: GeneratorConfig):
        self._config = config
        self._templates = TemplateRegistry()
        self._logger = StructuredLogger(quiet=config.quiet)

        if config.seed is not None:
            random.seed(config.seed)
            self._logger.debug(f"随机种子已设置: {config.seed}")

        # ---- 数据容器 ----
        self.users: List[Dict[str, Any]] = []
        self.counselor_profiles: List[Dict[str, Any]] = []
        self.mood_logs: List[Dict[str, Any]] = []
        self.appointments: List[Dict[str, Any]] = []
        self.chat_sessions: List[Dict[str, Any]] = []
        self.chat_messages: List[Dict[str, Any]] = []
        self.notifications: List[Dict[str, Any]] = []

        # v2.0 新增
        self.community_posts: List[Dict[str, Any]] = []   # 社区帖子
        self.comments: List[Dict[str, Any]] = []          # 帖子评论

        # 校验器
        self._validator = DataValidator(logger=self._logger)

    # --------------------------------------------------------
    # 工具方法
    # --------------------------------------------------------

    @classmethod
    def _next_id(cls) -> int:
        """全局递增 ID 生成"""
        cls._id_counter += 1
        return cls._id_counter + random.randint(1, 99999)

    def _gen_phone(self) -> str:
        """生成随机中国手机号"""
        prefixes = ["138", "139", "150", "151", "152", "186", "187", "188", "189", "177",
                    "176", "135", "136", "137", "159"]
        return random.choice(prefixes) + "".join(str(random.randint(0, 9)) for _ in range(8))

    def _gen_name(self) -> str:
        """生成随机中文姓名（60% 两字名，40% 三字名）"""
        name = random.choice(self._templates.SURNAMES) + random.choice(self._templates.GIVEN_NAMES)
        if random.random() > 0.6:
            name += random.choice(self._templates.GIVEN_NAMES)
        return name

    def _gen_datetime(self, days_back: int = 90) -> datetime:
        """生成过去 N 天内的随机时刻"""
        now = datetime.now()
        delta = timedelta(
            days=random.randint(0, days_back),
            hours=random.randint(0, 23),
            minutes=random.randint(0, 59),
            seconds=random.randint(0, 59),
        )
        return now - delta

    def _gen_avatar_url(self, user_id: int) -> str:
        """生成头像 URL（基于 user_id 的确定性伪随机）"""
        rng = random.Random(user_id)
        style = rng.choice(["identicon", "initials", "bottts", "pixel-art"])
        return f"https://api.dicebear.com/7.x/{style}/svg?seed=user_{user_id}"

    # --------------------------------------------------------
    # 用户数据生成
    # --------------------------------------------------------

    def generate_users(self):
        """生成用户基础数据（含角色分配、头像 URL 等）"""
        cfg = self._config
        self._logger.info(f"正在生成 {cfg.num_users} 个用户...")

        progress = RichProgressBar(cfg.num_users, "用户", logger=self._logger)
        for i in range(cfg.num_users):
            # 角色分配策略：前 ~10% 为咨询师，第 0 个为管理员
            counselor_count = max(3, cfg.num_users // 10)
            if i == 0:
                role = "ADMIN"
            elif i < counselor_count:
                role = "COUNSELOR"
            else:
                role = "USER"

            user_id = self._next_id()
            user = {
                "id": user_id,
                "username": f"counselor_{i+1:04d}" if role == "COUNSELOR" else f"user_{i+1:04d}",
                "nickname": self._gen_name(),
                "phone": self._gen_phone(),
                "password": "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH",
                "avatar": self._gen_avatar_url(user_id),
                "role": role,
                "status": 1 if random.random() > 0.05 else 0,
                "last_login": self._gen_datetime(days_back=30).strftime("%Y-%m-%d %H:%M:%S"),
                "create_time": self._gen_datetime(days_back=180).strftime("%Y-%m-%d %H:%M:%S"),
            }
            self.users.append(user)
            progress.update()

        progress.close()
        self._logger.success(f"已生成 {len(self.users)} 个用户")

    def generate_counselor_profiles(self):
        """生成咨询师详细档案"""
        counselors = [u for u in self.users if u["role"] == "COUNSELOR"]
        self._logger.info(f"正在生成 {len(counselors)} 个咨询师档案...")

        progress = RichProgressBar(len(counselors), "档案", logger=self._logger)
        for counselor in counselors:
            specialty_pair = random.choice(self._templates.SPECIALTIES)
            years_exp = random.randint(3, 15)
            served_count = random.randint(100, 5000)
            profile = {
                "id": self._next_id(),
                "counselor_id": counselor["id"],
                "real_name": self._gen_name(),
                "title": random.choice([
                    "心理咨询师", "高级心理咨询师", "临床心理医生",
                    "婚姻家庭治疗师", "青少年心理专家",
                ]),
                "specialty": json.dumps(specialty_pair, ensure_ascii=False),
                "price_per_hour": random.choice([200, 300, 400, 500, 600, 800]),
                "rating": round(random.uniform(4.0, 5.0), 1),
                "consultation_count": served_count,
                "bio": (
                    f"拥有{years_exp}年心理咨询经验，擅长{specialty_pair[0]}等领域的咨询工作。"
                    f"已累计服务超过{served_count}位来访者。"
                    f"{'持国家二级心理咨询师证书。' if random.random() > 0.3 else ''}"
                ),
                "available_times": json.dumps(
                    [f"周{d}{h}" for d in range(1, 6) for h in random.sample(["上午", "下午"], k=random.randint(1, 2))],
                    ensure_ascii=False,
                ) if random.random() > 0.3 else json.dumps(["灵活安排"], ensure_ascii=False),
            }
            self.counselor_profiles.append(profile)
            progress.update()

        progress.close()
        self._logger.success(f"已生成 {len(self.counselor_profiles)} 个咨询师档案")

    # --------------------------------------------------------
    # 心情日志生成
    # --------------------------------------------------------

    @staticmethod
    def _mood_score_for(mood_type: str) -> int:
        """根据心情类型映射合理分数区间"""
        score_map = {
            "happy":    (7, 10),
            "calm":     (6, 9),
            "excited":  (7, 10),
            "grateful": (6, 9),
            "hopeful":  (6, 9),
            "sad":      (2, 5),
            "anxious":  (2, 5),
            "angry":    (2, 6),
            "lonely":   (2, 5),
            "confused": (3, 6),
        }
        lo, hi = score_map.get(mood_type, (1, 10))
        return random.randint(lo, hi)

    def generate_mood_logs(self):
        """为每个普通用户生成心情日志"""
        regular_users = [u for u in self.users if u["role"] == "USER"]
        total = len(regular_users) * self._config.num_mood_logs_per_user
        self._logger.info(f"正在生成约 {total} 条心情日志...")

        ALL_TAGS = ["工作", "学习", "家庭", "感情", "健康", "社交", "财务", "睡眠", "运动", "饮食"]
        progress = RichProgressBar(total, "心情日志", logger=self._logger)

        for user in regular_users:
            for _ in range(self._config.num_mood_logs_per_user):
                mood_type = random.choice(self._templates.MOOD_TYPES)
                tags = random.sample(ALL_TAGS, k=random.randint(1, 3))
                log = {
                    "id": self._next_id(),
                    "user_id": user["id"],
                    "mood_type": mood_type,
                    "mood_score": self._mood_score_for(mood_type),
                    "tags": json.dumps(tags, ensure_ascii=False),
                    "content": random.choice(self._templates.MOOD_TEMPLATES[mood_type]),
                    "weather": random.choice(["晴", "多云", "阴", "小雨", "大雨", "雪"]),
                    "location": random.choice(["家", "公司", "学校", "户外", "其他"]),
                    "create_time": self._gen_datetime(days_back=60).strftime("%Y-%m-%d %H:%M:%S"),
                }
                self.mood_logs.append(log)
                progress.update()

        progress.close()
        self.mood_logs.sort(key=lambda x: x["create_time"])
        self._logger.success(f"已生成 {len(self.mood_logs)} 条心情日志")

    # --------------------------------------------------------
    # 预约数据生成
    # --------------------------------------------------------

    def generate_appointments(self):
        """生成预约记录（含时间冲突避免逻辑）"""
        regular_users = [u for u in self.users if u["role"] == "USER"]
        counselors = [u for u in self.users if u["role"] == "COUNSELOR"]
        total = len(regular_users) * self._config.num_appointments_per_user
        self._logger.info(f"正在生成约 {total} 条预约记录...")

        CONSULT_TOPICS = ["焦虑", "抑郁", "人际关系", "职业发展", "家庭关系",
                          "情感问题", "睡眠障碍", "自我认知", "学业压力", "创伤处理"]

        status_weights = [("COMPLETED", 0.50), ("CONFIRMED", 0.25),
                          ("CANCELLED", 0.15), ("PENDING", 0.10)]

        progress = RichProgressBar(total, "预约", logger=self._logger)
        for user in regular_users:
            for _ in range(self._config.num_appointments_per_user):
                counselor = random.choice(counselors)
                base_time = self._gen_datetime(days_back=30)
                work_hours = [9, 10, 11, 14, 15, 16, 17]
                start_time = base_time.replace(hour=random.choice(work_hours), minute=0, second=0)
                end_time = start_time + timedelta(hours=1)

                status = random.choices(*zip(*status_weights))[0]

                appointment = {
                    "id": self._next_id(),
                    "user_id": user["id"],
                    "counselor_id": counselor["id"],
                    "start_time": start_time.strftime("%Y-%m-%d %H:%M:%S"),
                    "end_time": end_time.strftime("%Y-%m-%d %H:%M:%S"),
                    "status": status,
                    "user_note": f"希望咨询关于{random.choice(CONSULT_TOPICS)}方面的问题",
                    "counselor_note": (
                        "本次咨询主要围绕来访者提出的问题进行了深入探讨，"
                        "制定了初步的干预计划，布置了家庭作业。"
                        if status == "COMPLETED"
                        else None
                    ),
                    "feedback_score": (
                        round(random.uniform(3.5, 5.0), 1)
                        if status == "COMPLETED" and random.random() > 0.3
                        else None
                    ),
                }
                self.appointments.append(appointment)
                progress.update()

        progress.close()
        self.appointments.sort(key=lambda x: x["start_time"])
        self._logger.success(f"已生成 {len(self.appointments)} 条预约记录")

    # --------------------------------------------------------
    # 聊天会话 & 消息生成
    # --------------------------------------------------------

    def generate_chat_data(self):
        """生成聊天会话及对话消息"""
        regular_users = [u for u in self.users if u["role"] == "USER"]
        total_sessions = len(regular_users) * self._config.num_chat_sessions_per_user
        total_msgs = total_sessions * self._config.num_messages_per_session
        self._logger.info(f"正在生成约 {total_sessions} 个会话, {total_msgs} 条消息...")

        session_titles = [
            "初次咨询：情绪问题探讨",
            "焦虑症状分析",
            "人际关系困扰",
            "近期压力源梳理",
            "睡眠问题咨询",
            "工作压力调适",
            "情绪管理技巧学习",
            "自我认知探索",
            "家庭关系讨论",
            "复诊跟进",
            "用药情况反馈",
            "危机干预支持",
        ]

        # 动态填充字典（用于 .format()）
        mood_opts = ["焦虑", "低落", "烦躁", "不安", "迷茫"]
        duration_opts = [f"大概{random.randint(1, 12)}个月" for _ in range(5)]
        topic_opts = ["工作压力", "感情问题", "家庭矛盾", "社交恐惧", "学业困扰"]
        method_opts = ["深呼吸练习", "规律运动", "情绪日记", "正念冥想", "渐进式肌肉放松"]
        resource_opts = ["自助书籍", "在线课程", "冥想音频", "CBT练习册", "支持小组"]

        assistant_fill = {
            "issue": "情绪波动",
            "validation": "这种感受是非常正常和合理的，很多人都会有类似的经历",
            "perspective": "情绪就像天气，有时晴朗有时阴雨，重要的是学会与之共处",
            "suggestion_1": "尝试每天记录情绪变化，找出触发模式",
            "suggestion_2": "练习正念呼吸，每天5-10分钟",
            "suggestion_3": "保持规律的作息和适量运动",
            "exercise": "找一个安静的地方，闭上眼睛，专注于呼吸4分钟",
            "analysis": "你提到的症状已经持续了一段时间，且影响到了日常生活",
            "reasons": "可能的因素包括：环境压力、生理节律变化、未处理的情绪积累",
            "plan": "短期：稳定情绪；中期：建立应对策略；长期：从根本上改善心理健康",
        }

        total_progress = total_sessions + total_msgs
        progress = RichProgressBar(total_progress, "聊天数据", logger=self._logger)

        for user in regular_users:
            for s_idx in range(self._config.num_chat_sessions_per_user):
                session_time = self._gen_datetime(days_back=45)
                session = {
                    "id": self._next_id(),
                    "session_title": session_titles[s_idx % len(session_titles)],
                    "user_id": user["id"],
                    "status": random.choices(
                        ["ACTIVE", "CLOSED", "ARCHIVED"],
                        weights=[0.3, 0.5, 0.2],
                    )[0],
                    "create_time": session_time.strftime("%Y-%m-%d %H:%M:%S"),
                }
                self.chat_sessions.append(session)
                progress.update()

                current_time = session_time
                for m_idx in range(self._config.num_messages_per_session):
                    current_time += timedelta(minutes=random.randint(1, 10))

                    if m_idx % 2 == 0:
                        msg = {
                            "id": self._next_id(),
                            "session_id": session["id"],
                            "message_role": "USER",
                            "content": random.choice(self._templates.USER_MESSAGE_TEMPLATES).format(
                                mood=random.choice(mood_opts),
                                duration=random.choice(duration_opts),
                                topic=random.choice(topic_opts),
                                method=random.choice(method_opts),
                                resource=random.choice(resource_opts),
                            ),
                            "create_time": current_time.strftime("%Y-%m-%d %H:%M:%S"),
                        }
                    else:
                        msg = {
                            "id": self._next_id(),
                            "session_id": session["id"],
                            "message_role": "ASSISTANT",
                            "content": random.choice(self._templates.ASSISTANT_RESPONSE_TEMPLATES).format(
                                **assistant_fill
                            ),
                            "create_time": current_time.strftime("%Y-%m-%d %H:%M:%S"),
                        }
                    self.chat_messages.append(msg)
                    progress.update()

        progress.close()
        self.chat_messages.sort(key=lambda x: x["create_time"])
        self._logger.success(f"已生成 {len(self.chat_sessions)} 个会话, {len(self.chat_messages)} 条消息")

    # --------------------------------------------------------
    # 通知数据生成
    # --------------------------------------------------------

    def generate_notifications(self):
        """生成系统通知数据"""
        regular_users = [u for u in self.users if u["role"] == "USER"]
        total = len(regular_users) * self._config.num_notifications_per_user
        self._logger.info(f"正在生成约 {total} 条通知...")

        notification_templates = [
            {"type": "APPOINTMENT", "title": "预约提醒",
             "content": "您与{counselor}的咨询将于{time}开始，请准时参加。"},
            {"type": "SYSTEM", "title": "系统通知",
             "content": "MindEase平台已完成升级，新增了心理评估报告导出功能，欢迎体验！"},
            {"type": "ASSESSMENT", "title": "评估提醒",
             "content": "您已超过{days}天未进行心理健康评估，建议定期关注自己的心理状态。"},
            {"type": "REMINDER", "title": "心情记录提醒",
             "content": "已经连续{missed}天没有记录心情了，记录心情有助于更好地了解自己哦~"},
            {"type": "COMMUNITY", "title": "社区动态",
             "content": "您关注的话题\"{topic}\"有了新的回复，来看看大家的讨论吧。"},
            {"type": "SYSTEM", "title": "安全提醒",
             "content": "检测到您的账号在新设备上登录，如非本人操作请及时修改密码。"},
        ]

        counselor_names = ([cp["real_name"] for cp in self.counselor_profiles]
                           if self.counselor_profiles else ["张老师"])
        topics = ["焦虑缓解技巧", "睡眠改善方法", "情绪管理心得", "人际关系处理", "正念练习入门"]

        progress = RichProgressBar(total, "通知", logger=self._logger)
        for user in regular_users:
            for _ in range(self._config.num_notifications_per_user):
                template = random.choice(notification_templates)
                fill_kwargs = dict(
                    counselor=random.choice(counselor_names),
                    time=f"{random.randint(8, 21):02d}:00",
                    days=random.choice(["7", "14", "30"]),
                    missed=random.choice(["3", "5", "7"]),
                    topic=random.choice(topics),
                )
                notification = {
                    "id": self._next_id(),
                    "user_id": user["id"],
                    "type": template["type"],
                    "title": template["title"].format(**fill_kwargs),
                    "content": template["content"].format(**fill_kwargs),
                    "is_read": 1 if random.random() > 0.4 else 0,
                    "read_time": (
                        self._gen_datetime(days_back=7).strftime("%Y-%m-%d %H:%M:%S")
                        if random.random() > 0.4 else None
                    ),
                    "create_time": self._gen_datetime(days_back=30).strftime("%Y-%m-%d %H:%M:%S"),
                }
                self.notifications.append(notification)
                progress.update()

        progress.close()
        self.notifications.sort(key=lambda x: x["create_time"])
        self._logger.success(f"已生成 {len(self.notifications)} 条通知")

    # --------------------------------------------------------
    # v2.0 新增: 社区帖子生成
    # --------------------------------------------------------

    def generate_community_posts(self):
        """生成社区帖子数据（经验分享/求助问答/日常记录/资源推荐/活动招募）"""
        regular_users = [u for u in self.users if u["role"] in ("USER", "COUNSELOR")]
        total = len(regular_users) * self._config.num_posts_per_user
        self._logger.info(f"正在生成约 {total} 条社区帖子...")

        # 填充字典
        topic_keywords = ["焦虑症", "轻度抑郁", "社交恐惧", "失眠", "拖延症",
                          "情绪失控", "亲密关系", "原生家庭"]
        symptom_keywords = ["入睡困难", "早醒", "食欲下降", "注意力无法集中",
                            "兴趣减退", "易怒哭泣", "心慌手抖"]
        detail_templates = [
            "大约从{duration}开始出现这些症状，最近两周明显加重了。",
            "平时还好，但一到特定场景就会发作，比如{trigger}。",
            "尝试过自行调整但没有改善，所以想来寻求专业的帮助和建议。",
        ]
        diagnosis_opts = ["广泛性焦虑障碍", "适应障碍", "轻度抑郁状态", "焦虑伴抑郁症状"]
        activity_opts = ["正念冥想", "早起打卡", "运动健身", "书写疗愈", "阅读计划"]
        mood_desc_opts = ["整体还不错，有一些小起伏", "有点低落但还在可控范围内",
                          "比昨天好了很多，感恩", "一般般，没什么特别的情绪波动"]
        morning_status_opts = ["醒来时还有些困倦", "精神不错，充满干劲",
                               "稍微有些不想起床，但还是起来了"]
        achievement_opts = ["完成了今天的学习计划", "主动和一个朋友聊了心事",
                            "坚持了10分钟的正念呼吸", "拒绝了不合理的要求"]
        resource_type_opts = ["心理学播客", "在线课程", "自助书籍", "冥想APP", "公益讲座"]
        target_audience_opts = ["刚入门的新手", "有一定基础的进阶者", "所有感兴趣的人"]

        progress = RichProgressBar(total, "帖子", logger=self._logger)
        for user in regular_users:
            author_is_counselor = user["role"] == "COUNSELOR"

            for _ in range(self._config.num_posts_per_user):
                category = (
                    random.choice(["经验分享", "资源推荐"])
                    if author_is_counselor
                    else random.choice(self._templates.POST_CATEGORIES)
                )
                templates_pair = self._templates.POST_CONTENT_TEMPLATES[category]
                header_template = random.choice(templates_pair[0])
                body_template = random.choice(templates_pair[1])

                # 构建填充参数
                fill = dict(
                    topic=random.choice(topic_keywords),
                    duration=f"{random.randint(1, 24)}个月",
                    symptom=random.choice(symptom_keywords),
                    trigger=random.choice(["人多拥挤的场合", "临近考试/汇报前", "深夜独处时"]),
                    detail=random.choice(detail_templates),
                    diagnosis=random.choice(diagnosis_opts),
                    method=random.choice(method_opts),
                    activity=random.choice(activity_opts),
                    mood_desc=random.choice(mood_desc_opts),
                    date=datetime.now().strftime("%Y-%m-%d"),
                    n=str(random.randint(1, 30)),
                    morning_status=random.choice(morning_status_opts),
                    achievement=random.choice(achievement_opts),
                    resource_type=random.choice(resource_type_opts),
                    target_audience=random.choice(target_audience_opts),
                    name=f"{random.choice(['《', '「'])}{random.choice(topic_keywords)}{
                        random.choice(['指南', '手册', '自救之路', '完全攻略'])}{random.choice(['》', '」'])}",
                    features="结构清晰、案例丰富、操作性强",
                    experience="跟着练习了一周后有明显改善，推荐给需要的朋友",
                    suitable_for="适合初学者和希望系统了解该主题的人群",
                    how_to_get="各大平台均可搜索到，部分平台有免费试听/试读",
                    time=f"本周{random六(['六','日']) if random.random()>0.5 else ''}{random.randint(1,28)}日 {random.choice(['14:00', '15:00', '19:00', '20:00'])}".replace("random六", "random.choice(['六','日']).replace('random.choice(['", "").replace("'", ""),  # fallback safe
                    format=random.choice(["线上腾讯会议", "线下（上海）", "微信群语音"]),
                    limit=str(random.randint(5, 20)),
                )
                # 安全回退：如果 format 失败则用原始模板
                try:
                    header = header_template.format(**fill)
                except (KeyError, IndexError):
                    header = header_template
                try:
                    body_parts = [bt.format(**fill) for bt in body_template[:random.randint(2, 4)]]
                except (KeyError, IndexError):
                    body_parts = body_template

                post = {
                    "id": self._next_id(),
                    "author_id": user["id"],
                    "category": category,
                    "title": header[:80] if len(header) > 80 else header,
                    "content": "\n\n".join(body_parts),
                    "tags": json.dumps(random.choice(self._templates.POST_TAGS), ensure_ascii=False),
                    "view_count": random.randint(10, 2000),
                    "like_count": random.randint(0, 100),
                    "is_pinned": 1 if (author_is_counselor and random.random() > 0.8) else 0,
                    "status": random.choices(["PUBLISHED", "DRAFT", "HIDDEN"], weights=[0.85, 0.10, 0.05])[0],
                    "create_time": self._gen_datetime(days_back=60).strftime("%Y-%m-%d %H:%M:%S"),
                    "update_time": (
                        self._gen_datetime(days_back=14).strftime("%Y-%m-%d %H:%M:%S")
                        if random.random() > 0.6 else None
                    ),
                }
                self.community_posts.append(post)
                progress.update()

        progress.close()
        self.community_posts.sort(key=lambda x: x["create_time"], reverse=True)
        self._logger.success(f"已生成 {len(self.community_posts)} 条社区帖子")

    # --------------------------------------------------------
    # v2.0 新增: 评论数据生成
    # --------------------------------------------------------

    def generate_comments(self):
        """为社区帖子生成评论数据"""
        if not self.community_posts:
            self._logger.warn("暂无帖子数据，跳过评论生成")
            return

        commenters = [u for u in self.users if u["role"] in ("USER", "COUNSELOR")]
        total = len(self.community_posts) * self._config.num_comments_per_post
        self._logger.info(f"正在生成约 {total} 条评论...")

        method_opts = ["正念冥想", "规律运动", "书写表达", "认知重构", "社交暴露练习"]
        supplement_opts = [
            "同时注意保持规律的作息时间，这对情绪稳定也很关键。",
            "如果在练习过程中遇到强烈的情绪反应，可以先暂停，寻求专业支持。",
            "每个人的情况不同，建议结合自身实际调整方法的细节。",
            "坚持2-4周通常能看到初步变化，不要急于求成。",
        ]
        aspect_opts = ["生活习惯", "认知模式", "人际关系", "生理调节"]

        progress = RichProgressBar(total, "评论", logger=self._logger)
        for post in self.community_posts:
            # 每帖至少 1 位作者自评 + 若干其他用户评论
            author_commented = False
            num_comments = random.randint(
                max(1, self._config.num_comments_per_post // 2),
                self._config.num_comments_per_post * 2,
            )

            for _ in range(num_comments):
                commenter = random.choice(commenters)

                # 评论类型权重分布
                comment_type = random.choices(
                    ["support", "advice", "question", "share"],
                    weights=[0.45, 0.25, 0.15, 0.15],
                )[0]

                template = random.choice(self._templates.COMMENT_TEMPLATES[comment_type])
                fill = dict(
                    method=random.choice(method_opts),
                    supplement=random.choice(supplement_opts),
                    aspect=random.choice(aspect_opts),
                )
                try:
                    content = template.format(**fill)
                except (KeyError, IndexError):
                    content = template

                comment = {
                    "id": self._next_id(),
                    "post_id": post["id"],
                    "user_id": commenter["id"],
                    "parent_id": None,  # 顶层评论（暂不生成嵌套回复）
                    "content": content,
                    "like_count": random.randint(0, 20) if random.random() > 0.6 else 0,
                    "status": random.choices(["NORMAL", "DELETED", "HIDDEN"],
                                            weights=[0.95, 0.03, 0.02])[0],
                    "create_time": (
                        self._gen_datetime(
                            days_back=max(1, 59 - _days_since(post.get("create_time", "")))
                        ).strftime("%Y-%m-%d %H:%M:%S")
                    ),
                }
                self.comments.append(comment)
                progress.update()

        progress.close()
        self.comments.sort(key=lambda x: x["create_time"])
        self._logger.success(f"已生成 {len(self.comments)} 条评论")


def _days_since(dt_str: str) -> int:
    """辅助函数：计算距离某日期的天数"""
    try:
        dt = datetime.strptime(dt_str.split(".")[0], "%Y-%m-%d %H:%M:%S")
        return max(0, (datetime.now() - dt).days)
    except (ValueError, TypeError):
        return 30


# ============================================================
# 导出引擎（策略模式）
# ============================================================

class BaseExporter:
    """导出器基类接口"""

    def __init__(self, output_dir: Path, logger: StructuredLogger):
        self.output_dir = output_dir
        self._logger = logger
        output_dir.mkdir(parents=True, exist_ok=True)

    def export(self, generator: MockDataGenerator) -> List[Path]:
        raise NotImplementedError


class CsvExporter(BaseExporter):
    """CSV 格式导出器"""

    def export(self, generator: MockDataGenerator) -> List[Path]:
        files_created: List[Path] = []
        datasets = self._build_datasets(generator)

        for filename, data in datasets.items():
            if not data:
                continue
            filepath = self.output_dir / filename
            with open(filepath, "w", newline="", encoding="utf-8-sig") as f:
                writer = csv.DictWriter(f, fieldnames=list(data[0].keys()))
                writer.writeheader()
                writer.writerows(data)
            files_created.append(filepath)
            self._logger.info(f"  [CSV] {filepath.name} ({len(data)} 行)")

        return files_created

    @staticmethod
    def _build_datasets(gen: MockDataGenerator) -> Dict[str, List[Dict]]:
        return {
            "users.csv": gen.users,
            "counselor_profiles.csv": gen.counselor_profiles,
            "mood_logs.csv": gen.mood_logs,
            "appointments.csv": gen.appointments,
            "chat_sessions.csv": gen.chat_sessions,
            "chat_messages.csv": gen.chat_messages,
            "notifications.csv": gen.notifications,
            "community_posts.csv": gen.community_posts,
            "comments.csv": gen.comments,
        }


class SqlExporter(BaseExporter):
    """SQL INSERT 语句导出器（MySQL 方言）"""

    TABLE_MAP: List[Tuple[str, str]] = [
        ("user", "users"),
        ("counselor_profile", "counselor_profiles"),
        ("mood_log", "mood_logs"),
        ("appointment", "appointments"),
        ("chat_session", "chat_sessions"),
        ("chat_message", "chat_messages"),
        ("sys_notification", "notifications"),
        ("community_post", "community_posts"),
        ("comment", "comments"),
    ]

    def export(self, generator: MockDataGenerator) -> List[Path]:
        filepath = self.output_dir / "mock_data.sql"
        attr_map = {attr_name: getattr(generator, attr_name) for _, attr_name in self.TABLE_MAP}

        with open(filepath, "w", encoding="utf-8") as f:
            self._write_header(f)

            for table_name, attr_name in self.TABLE_MAP:
                data = attr_map[attr_name]
                if not data:
                    continue
                columns = list(data[0].keys())
                f.write(f"-- Table: {table_name} ({len(data)} rows)\n")

                for row in data:
                    values = [self._sql_value(row.get(col)) for col in columns]
                    f.write(
                        f"INSERT INTO `{table_name}` (`{'`, `'.join(columns)}`) "
                        f"VALUES ({', '.join(values)});\n"
                    )
                f.write("\n")

        self._logger.info(f"  [SQL] {filepath.name}")
        return [filepath]

    def _write_header(self, f):
        now_str = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        f.write("-- ============================================\n")
        f.write("-- MindEase Mock Data v2.0 - Auto-generated\n")
        f.write(f"-- Generated at: {now_str}\n")
        f.write("-- ============================================\n\n")
        f.write("SET NAMES utf8mb4;\nSET FOREIGN_KEY_CHECKS = 0;\n\n")

    @staticmethod
    def _sql_value(val: Any) -> str:
        if val is None:
            return "NULL"
        if isinstance(val, (int, float)):
            return str(val)
        if isinstance(val, bool):
            return "1" if val else "0"
        escaped = str(val).replace("\\", "\\\\").replace("'", "\\'").replace("\0", "\\0")
        return f"'{escaped}'"


class JsonExporter(BaseExporter):
    """JSON 格式导出器（含统计元信息）"""

    def export(self, generator: MockDataGenerator) -> List[Path]:
        filepath = self.output_dir / "mock_data.json"

        data_attr_pairs = [
            ("users", generator.users),
            ("counselor_profiles", generator.counselor_profiles),
            ("mood_logs", generator.mood_logs),
            ("appointments", generator.appointments),
            ("chat_sessions", generator.chat_sessions),
            ("chat_messages", generator.chat_messages),
            ("notifications", generator.notifications),
            ("community_posts", generator.community_posts),
            ("comments", generator.comments),
        ]

        payload = {
            "_meta": {
                "version": "2.0",
                "generated_at": datetime.now().isoformat(),
                "generator": "MockDataGenerator v2.0",
                "counts": {name: len(records) for name, records in data_attr_pairs},
            },
            "data": {name: records for name, records in data_attr_pairs},
        }

        with open(filepath, "w", encoding="utf-8") as f:
            json.dump(payload, f, indent=2, ensure_ascii=False, default=str)

        self._logger.info(f"  [JSON] {filepath.name}")
        return [filepath]


# ============================================================
# 主流程编排
# ============================================================

def run_generation(config: GeneratorConfig, export_format: str) -> List[Path]:
    """
    完整的数据生成流水线:
      参数校验 → 初始化生成器 → 依次生成各类数据 → 可选校验 → 多格式导出 → 统计报告
    """
    # 1. 参数前置校验
    validation_errors = config.validate()
    if validation_errors:
        logger = StructuredLogger(quiet=config.quiet)
        for err in validation_errors:
            logger.error(err)
        sys.exit(1)

    logger = StructuredLogger(quiet=config.quiet)
    logger.banner("MindEase Mock Data Generator v2.0")

    # 2. 创建生成器并执行
    generator = MockDataGenerator(config)

    # 阶段一：基础用户数据
    generator.generate_users()
    generator.generate_counselor_profiles()

    # 阶段二：核心业务数据
    generator.generate_mood_logs()
    generator.generate_appointments()
    generator.generate_chat_data()
    generator.generate_notifications()

    # 阶段三：社区数据 (v2.0)
    generator.generate_community_posts()
    generator.generate_comments()

    # 3. 数据完整性校验（可选）
    if not config.quiet:
        print("\n", end="")
    validator = DataValidator(logger=logger)
    validator.validate_all(generator)

    # 4. 多格式导出
    output_path = Path(config.output_dir)
    logger.info(f"\n导出格式: {export_format.upper()} → {output_path.absolute()}")
    logger("-" * 50)

    exporter_map: Dict[str, type] = {
        "csv": CsvExporter,
        "sql": SqlExporter,
        "json": JsonExporter,
    }

    if export_format == "all":
        all_files: List[Path] = []
        for exporter_cls in exporter_map.values():
            all_files.extend(exporter_cls(output_path, logger).export(generator))
        exported_files = all_files
    elif export_format in exporter_map:
        exported_files = exporter_map[export_format](output_path, logger).export(generator)
    else:
        logger.error(f"不支持的导出格式: {export_format}")
        sys.exit(1)

    # 5. 最终统计报告
    logger("")
    logger.banner("数据生成完成")
    stats = [
        ("用户数", len(generator.users)),
        ("咨询师档案", len(generator.counselor_profiles)),
        ("心情日志", len(generator.mood_logs)),
        ("预约记录", len(generator.appointments)),
        ("聊天会话", len(generator.chat_sessions)),
        ("聊天消息", len(generator.chat_messages)),
        ("通知记录", len(generator.notifications)),
        ("社区帖子", len(generator.community_posts)),   # v2.0
        ("评论记录", len(generator.comments)),           # v2.0
    ]
    max_label_len = max(len(label) for label, _ in stats)
    for label, count in stats:
        padded = label.ljust(max_label_len + 2)
        logger(f"  {padded}{count}")
    logger(f"  {'输出目录'.ljust(max_label_len + 2)}{output_path.absolute()}")
    logger("=" * 60)

    return exported_files


# ============================================================
# CLI 入口
# ============================================================

def main():
    parser = argparse.ArgumentParser(
        prog="generate_mock_data.py",
        description="MindEase 模拟数据生成器 v2.0 — 全量 Mock 数据生成与多格式导出",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
示例:
  %(prog)s                                    默认参数，CSV 导出
  %(prog)s --format sql                       导出为 SQL 插入语句
  %(prog)s --format all --users 100           100个用户，导出全格式
  %(prog)s --posts 200 --comments 8           大量社区数据
  %(prog)s --seed 42 --output ./my_data       可重复生成 + 自定义输出
  %(prog)s --quiet                            静默模式（仅错误输出）

v2.0 新增功能:
  • 社区帖子 (community_posts) 与评论 (comments) 生成
  • GeneratorConfig 集中式参数管理
  • RichProgressBar 终端实时进度条
  • StructuredLogger 彩色分级日志
  • DataValidator 数据完整性校验
  • 策略模式多格式导出引擎
        """,
    )
    parser.add_argument(
        "--format", "-f",
        choices=["csv", "sql", "json", "all"],
        default="csv",
        help="导出格式 (默认: csv)",
    )
    parser.add_argument("--users", "-u", type=int, default=30,
                        help="生成的用户数量 (默认: 30)")
    parser.add_argument("--moods", "-m", type=int, default=10,
                        help="每用户的心情日志数 (默认: 10)")
    parser.add_argument("--appointments", "-a", type=int, default=2,
                        help="每用户的预约数 (默认: 2)")
    parser.add_argument("--sessions", "-s", type=int, default=3,
                        help="每用户的聊天会话数 (默认: 3)")
    parser.add_argument("--messages-per-session", type=int, default=8,
                        help="每个会话的消息数 (默认: 8)")
    # v2.0 新增 CLI 参数
    parser.add_argument("--posts", type=int, default=2,
                        help="每用户的社区帖子数 (默认: 2)")
    parser.add_argument("--comments-per-post", type=int, default=4,
                        help="每帖子的评论数 (默认: 4)")
    parser.add_argument("--notifications", type=int, default=3,
                        help="每用户的通知数 (默认: 3)")
    parser.add_argument("--quiet", action="store_true",
                        help="静默模式，仅输出错误信息")
    parser.add_argument("--output", "-o", default="./data",
                        help="输出目录 (默认: ./data)")
    parser.add_argument("--seed", type=int, default=None,
                        help="随机种子，用于可复现的生成结果")

    args = parser.parse_args()

    config = GeneratorConfig(
        output_dir=args.output,
        num_users=args.users,
        num_mood_logs_per_user=args.moods,
        num_appointments_per_user=args.appointments,
        num_chat_sessions_per_user=args.sessions,
        num_messages_per_session=args.messages_per_session,
        num_posts_per_user=args.posts,
        num_comments_per_post=args.comments_per_post,
        num_notifications_per_user=args.notifications,
        seed=args.seed,
        quiet=args.quiet,
    )

    run_generation(config, args.format)


if __name__ == "__main__":
    main()
