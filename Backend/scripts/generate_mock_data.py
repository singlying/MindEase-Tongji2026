#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
MindEase 模拟数据生成器
========================
生成用户、心情日志、预约、聊天会话等模拟数据，支持导出为 CSV 或 SQL。

使用方式:
    python generate_mock_data.py --format csv     # 导出为 CSV 文件
    python generate_mock_data.py --format sql      # 导出为 SQL 插入语句
    python generate_mock_data.py --users 50        # 生成 50 个用户
    python generate_mock_data.py --output ./data   # 指定输出目录

依赖: Python 3.7+, 无需额外安装包 (仅使用标准库)
"""

import argparse
import csv
import json
import os
import random
import sys
from datetime import datetime, timedelta
from pathlib import Path


# ============================================================
# 配置区域：根据实际实体字段调整
# ============================================================

MOOD_TYPES = ["happy", "sad", "anxious", "angry", "calm", "excited", "lonely", "confused", "grateful", "hopeful"]
USER_ROLES = ["USER", "COUNSELOR", "ADMIN"]
APPOINTMENT_STATUSES = ["PENDING", "CONFIRMED", "COMPLETED", "CANCELLED", "NO_SHOW"]
CHAT_ROLES = ["USER", "ASSISTANT"]

# 通知类型（用于生成通知数据）
NOTIFICATION_TYPES = ["APPOINTMENT", "SYSTEM", "ASSESSMENT", "REMINDER", "COMMUNITY"]

# 中文姓名库（常见姓氏 + 名字）
SURNAMES = ["张", "王", "李", "刘", "陈", "杨", "赵", "黄", "周", "吴",
            "徐", "孙", "马", "朱", "胡", "郭", "何", "林", "罗", "高"]
GIVEN_NAMES = ["伟", "芳", "娜", "敏", "静", "丽", "强", "磊", "军", "洋",
               "勇", "艳", "杰", "娟", "涛", "明", "超", "秀英", "霞", "平",
               "宇", "欣", "晨", "璐", "婷", "浩", "雪", "琳", "博", "宁"]

# 咨询师专长标签
SPECIALTIES = [
    ["认知行为疗法", "焦虑症"],
    ["家庭治疗", "婚姻咨询"],
    ["青少年心理", "学业压力"],
    ["抑郁症", "情绪管理"],
    ["职场压力", "人际交往"],
    ["创伤后康复", "PTSD"],
    ["睡眠障碍", "放松训练"],
    ["亲子关系", "儿童心理"],
]

# 心情相关模板内容
MOOD_TEMPLATES = {
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

# 聊天消息模板（用户侧）
USER_MESSAGE_TEMPLATES = [
    "你好，我最近感觉{mood}，能帮我分析一下吗？",
    "我已经{duration}这样了，不知道该怎么办。",
    "{topic}这个问题困扰我很久了，有什么建议吗？",
    "我试过{method}，但好像没什么效果。",
    "能不能推荐一些{resource}来帮助我？",
]

ASSISTANT_RESPONSE_TEMPLATES = [
    "感谢你的分享。听起来你正在经历{issue}，这确实不容易。\n\n"
    "首先，我想确认一下：这种情况持续多久了？有没有特定的触发因素？\n\n"
    "根据你的描述，我建议我们可以从以下几个方面入手：\n"
    "1. {suggestion_1}\n"
    "2. {suggestion_2}\n"
    "3. {suggestion_3}\n\n"
    "你觉得这些建议中有哪些是你愿意尝试的？",

    "我理解你的感受。{validation}\n\n"
    "在心理咨询中，我们经常会遇到类似的情况。让我和你分享一个可能有用的视角：{perspective}\n\n"
    "同时，我想邀请你做一个小的练习：{exercise}\n\n"
    "做完之后，可以和我分享你的感受。",

    "这是一个很好的问题。让我先帮你梳理一下：\n\n"
    "**现状分析**：{analysis}\n\n"
    **可能的原因**：{reasons}\n\n"
    "**建议方案**：{plan}\n\n"
    "如果你觉得这个方向可行，我们可以制定一个更详细的行动计划。",
]


# ============================================================
# 数据生成器类
# ============================================================

class MockDataGenerator:
    """MindEase 模拟数据生成器"""

    def __init__(self, output_dir: str = "./data", num_users: int = 30,
                 num_mood_logs_per_user: int = 10,
                 num_appointments_per_user: int = 2,
                 num_chat_sessions_per_user: int = 3,
                 num_messages_per_session: int = 8,
                 seed: int = None):
        self.output_dir = Path(output_dir)
        self.num_users = num_users
        self.num_mood_logs_per_user = num_mood_logs_per_user
        self.num_appointments_per_user = num appointments_per_user
        self.num_chat_sessions_per_user = num_chat_sessions_per_user
        self.num_messages_per_session = num_messages_per_session

        if seed is not None:
            random.seed(seed)

        self.users = []
        self.counselor_profiles = []
        self.mood_logs = []
        self.appointments = []
        self.chat_sessions = []
        self.chat_messages = []
        self.notifications = []  # 新增：通知数据
        self.assessment_records = []  # 新增：评估记录数据（与评估生成器联动）

    def _gen_phone(self) -> str:
        """生成随机手机号"""
        prefixes = ["138", "139", "150", "151", "152", "186", "187", "188", "189", "177"]
        return random.choice(prefixes) + "".join([str(random.randint(0, 9)) for _ in range(8)])

    def _gen_name(self) -> str:
        """生成随机中文姓名"""
        return random.choice(SURNAMES) + random.choice(GIVEN_NAMES) + (
            random.choice(GIVEN_NAMES) if random.random() > 0.6 else ""
        )

    def _gen_datetime(self, days_back: int = 90) -> datetime:
        """生成过去 N 天内的随机时间"""
        now = datetime.now()
        random_days = random.randint(0, days_back)
        random_hours = random.randint(0, 23)
        random_minutes = random.randint(0, 59)
        return now - timedelta(days=random_days, hours=random_hours, minutes=random_minutes)

    def _gen_id(self) -> int:
        """生成递增ID（简单实现）"""
        return int(datetime.now().timestamp() * 1000000) % 1000000000 + random.randint(1, 99999)

    # --------------------------------------------------------
    # 用户数据生成
    # --------------------------------------------------------

    def generate_users(self):
        """生成用户数据"""
        print(f"[INFO] 正在生成 {self.num_users} 个用户...")

        for i in range(self.num_users):
            role = "COUNSELOR" if i < max(3, self.num_users // 10) else (
                "ADMIN" if i == 0 else "USER"
            )
            user = {
                "id": self._gen_id(),
                "username": f"user_{i+1:04d}" if role != "COUNSELOR" else f"counselor_{i+1:04d}",
                "phone": self._gen_phone(),
                "password": "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH",  # bcrypt mock
                "role": role,
                "status": 1 if random.random() > 0.05 else 0,  # 95% 正常状态
                "create_time": self._gen_datetime(days_back=180).strftime("%Y-%m-%d %H:%M:%S"),
            }
            self.users.append(user)

        print(f"[OK] 已生成 {len(self.users)} 个用户")

    def generate_counselor_profiles(self):
        """生成咨询师档案"""
        counselors = [u for u in self.users if u["role"] == "COUNSELOR"]
        print(f"[INFO] 正在生成 {len(counselors)} 个咨询师档案...")

        for counselor in counselors:
            profile = {
                "id": self._gen_id(),
                "counselor_id": counselor["id"],
                "real_name": self._gen_name(),
                "title": random.choice(["心理咨询师", "高级心理咨询师", "临床心理医生",
                                         "婚姻家庭治疗师", "青少年心理专家"]),
                "specialty": json.dumps(random.choice(SPECIALTIES), ensure_ascii=False),
                "price_per_hour": random.choice([200, 300, 400, 500, 600, 800]),
                "rating": round(random.uniform(4.0, 5.0), 1),
                "bio": f"拥有{random.randint(3, 15)}年心理咨询经验，擅长{
                    random.choice(SPECIALTIES)[0]}等领域的咨询工作。"
                       f"已累计服务超过{random.randint(100, 5000)}位来访者。",
            }
            self.counselor_profiles.append(profile)

        print(f"[OK] 已生成 {len(self.counselor_profiles)} 个咨询师档案")

    # --------------------------------------------------------
    # 心情日志生成
    # --------------------------------------------------------

    def generate_mood_logs(self):
        """生成心情日志"""
        regular_users = [u for u in self.users if u["role"] == "USER"]
        total = len(regular_users) * self.num_mood_logs_per_user
        print(f"[INFO] 正在生成约 {total} 条心情日志...")

        for user in regular_users:
            for _ in range(self.num_mood_logs_per_user):
                mood_type = random.choice(MOOD_TYPES)
                mood_score = {
                    "happy": random.randint(7, 10),
                    "calm": random.randint(6, 9),
                    "excited": random.randint(7, 10),
                    "sad": random.randint(2, 5),
                    "anxious": random.randint(2, 5),
                    "angry": random.randint(2, 6),
                    "lonely": random.randint(2, 5),
                    "confused": random.randint(3, 6),
                }.get(mood_type, random.randint(1, 10))

                tags = random.sample(
                    ["工作", "学习", "家庭", "感情", "健康", "社交", "财务", "睡眠"],
                    k=random.randint(1, 3)
                )

                log = {
                    "id": self._gen_id(),
                    "user_id": user["id"],
                    "mood_type": mood_type,
                    "mood_score": mood_score,
                    "tags": json.dumps(tags, ensure_ascii=False),
                    "content": random.choice(MOOD_TEMPLATES[mood_type]),
                    "create_time": self._gen_datetime(days_back=60).strftime("%Y-%m-%d %H:%M:%S"),
                }
                self.mood_logs.append(log)

        # 按时间排序
        self.mood_logs.sort(key=lambda x: x["create_time"])
        print(f"[OK] 已生成 {len(self.mood_logs)} 条心情日志")

    # --------------------------------------------------------
    # 预约数据生成
    # --------------------------------------------------------

    def generate_appointments(self):
        """生成预约记录"""
        regular_users = [u for u in self.users if u["role"] == "USER"]
        counselors = [u for u in self.users if u["role"] == "COUNSELOR"]
        total = len(regular_users) * self.num_appointments_per_user
        print(f"[INFO] 正在生成约 {total} 条预约记录...")

        for user in regular_users:
            for j in range(self.num_appointments_per_user):
                counselor = random.choice(counselors)
                base_time = self._gen_datetime(days_back=30)
                start_time = base_time.replace(
                    hour=random.choice([9, 10, 11, 14, 15, 16, 17]),
                    minute=0,
                    second=0
                )
                end_time = start_time + timedelta(hours=1)

                status_weights = [("COMPLETED", 0.5), ("CONFIRMED", 0.25),
                                  ("CANCELLED", 0.15), ("PENDING", 0.1)]
                status = random.choices(*zip(*status_weights))[0]

                appointment = {
                    "id": self._gen_id(),
                    "user_id": user["id"],
                    "counselor_id": counselor["id"],
                    "start_time": start_time.strftime("%Y-%m-%d %H:%M:%S"),
                    "end_time": end_time.strftime("%Y-%m-%d %H:%M:%S"),
                    "status": status,
                    "user_note": f"希望咨询关于{random.choice(['焦虑', '抑郁', '人际关系', '职业发展',
                                                          '家庭关系', '情感问题', '睡眠障碍'])}方面的问题",
                    "counselor_note": "本次咨询主要围绕来访者提出的问题进行了深入探讨..."
                        if status == "COMPLETED" else None,
                }
                self.appointments.append(appointment)

        self.appointments.sort(key=lambda x: x["start_time"])
        print(f"[OK] 已生成 {len(self.appointments)} 条预约记录")

    # --------------------------------------------------------
    # 聊天会话与消息生成
    # --------------------------------------------------------

    def generate_chat_data(self):
        """生成聊天会话和消息"""
        regular_users = [u for u in self.users if u["role"] == "USER"]
        total_sessions = len(regular_users) * self.num_chat_sessions_per_user
        total_messages = total_sessions * self.num_messages_per_session
        print(f"[INFO] 正在生成约 {total_sessions} 个会话, {total_messages} 条消息...")

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
        ]

        for user in regular_users:
            for s_idx in range(self.num_chat_sessions_per_user):
                session_time = self._gen_datetime(days_back=45)

                session = {
                    "id": self._gen_id(),
                    "session_title": session_titles[s_idx % len(session_titles)],
                    "user_id": user["id"],
                    "create_time": session_time.strftime("%Y-%m-%d %H:%M:%S"),
                }
                self.chat_sessions.append(session)

                # 生成该会话下的消息
                current_time = session_time
                for m_idx in range(self.num_messages_per_session):
                    current_time += timedelta(minutes=random.randint(1, 10))

                    if m_idx % 2 == 0:  # 用户消息
                        msg = {
                            "id": self._gen_id(),
                            "session_id": session["id"],
                            "message_role": "USER",
                            "content": random.choice(USER_MESSAGE_TEMPLATES).format(
                                mood=random.choice(["焦虑", "低落", "烦躁", "不安"]),
                                duration=f"大概{random.randint(1, 12)}个月",
                                topic=random.choice(["工作压力", "感情问题", "家庭矛盾", "社交恐惧"]),
                                method=random.choice(["深呼吸", "运动", "写日记"]),
                                resource=random.choice(["书籍", "课程", "练习方法"]),
                            ),
                            "create_time": current_time.strftime("%Y-%m-%d %H:%M:%S"),
                        }
                    else:  # AI/咨询师回复
                        msg = {
                            "id": self._gen_id(),
                            "session_id": session["id"],
                            "message_role": "ASSISTANT",
                            "content": random.choice(ASSISTANT_RESPONSE_TEMPLATES).format(
                                issue="情绪波动",
                                validation="这种感受是非常正常和合理的，很多人都会有类似的经历",
                                perspective="情绪就像天气，有时晴朗有时阴雨，重要的是学会与之共处",
                                suggestion_1="尝试每天记录情绪变化，找出触发模式",
                                suggestion_2="练习正念呼吸，每天5-10分钟",
                                suggestion_3="保持规律的作息和适量运动",
                                exercise="找一个安静的地方，闭上眼睛，专注于呼吸4分钟",
                                analysis="你提到的症状已经持续了一段时间，且影响到了日常生活",
                                reasons="可能的因素包括：环境压力、生理节律变化、未处理的情绪积累",
                                plan="短期：稳定情绪；中期：建立应对策略；长期：从根本上改善心理健康",
                            ),
                            "create_time": current_time.strftime("%Y-%m-%d %H:%M:%S"),
                        }
                    self.chat_messages.append(msg)

        self.chat_messages.sort(key=lambda x: x["create_time"])
        print(f"[OK] 已生成 {len(self.chat_sessions)} 个会话, {len(self.chat_messages)} 条消息")

    # --------------------------------------------------------
    # 通知数据生成 (新增)
    # --------------------------------------------------------

    def generate_notifications(self):
        """生成系统通知数据"""
        regular_users = [u for u in self.users if u["role"] == "USER"]
        total = len(regular_users) * 3  # 每用户3条通知
        print(f"[INFO] 正在生成约 {total} 条通知...")

        notification_templates = [
            {"type": "APPOINTMENT", "title": "预约提醒", "content": "您与{counselor}的咨询将于{time}开始，请准时参加。"},
            {"type": "SYSTEM", "title": "系统通知", "content": "MindEase平台已完成升级，新增了心理评估报告导出功能，欢迎体验！"},
            {"type": "ASSESSMENT", "title": "评估提醒", "content": "您已超过{days}天未进行心理健康评估，建议定期关注自己的心理状态。"},
            {"type": "REMINDER", "title": "心情记录提醒", "content": "已经连续{missed}天没有记录心情了，记录心情有助于更好地了解自己哦~"},
            {"type": "COMMUNITY", "title": "社区动态", "content": "您关注的话题\"{topic}\"有了新的回复，来看看大家的讨论吧。"},
        ]

        counselor_names = [cp["real_name"] for cp in self.counselor_profiles] if self.counselor_profiles else ["张老师"]
        topics = ["焦虑缓解技巧", "睡眠改善方法", "情绪管理心得", "人际关系处理"]

        for user in regular_users:
            for _ in range(3):
                template = random.choice(notification_templates)
                is_read = random.random() > 0.4  # 60% 已读
                notification = {
                    "id": self._gen_id(),
                    "user_id": user["id"],
                    "type": template["type"],
                    "title": template["title"].format(
                        counselor=random.choice(counselor_names),
                        time=f"{random.randint(8, 21):02d}:00",
                        days=random.choice(["7", "14", "30"]),
                        missed=random.choice(["3", "5", "7"]),
                        topic=random.choice(topics),
                    ),
                    "content": template["content"].format(
                        counselor=random.choice(counselor_names),
                        time=f"{random.randint(8, 21):02d}:00",
                        days=random.choice(["7", "14", "30"]),
                        missed=random.choice(["3", "5", "7"]),
                        topic=random.choice(topics),
                    ),
                    "is_read": 1 if is_read else 0,
                    "create_time": self._gen_datetime(days_back=30).strftime("%Y-%m-%d %H:%M:%S"),
                }
                self.notifications.append(notification)

        self.notifications.sort(key=lambda x: x["create_time"])
        print(f"[OK] 已生成 {len(self.notifications)} 条通知")

    # --------------------------------------------------------
    # 导出功能
    # --------------------------------------------------------

    def export_csv(self):
        """导出为 CSV 文件"""
        self.output_dir.mkdir(parents=True, exist_ok=True)
        files_created = []

        datasets = {
            "users.csv": self.users,
            "counselor_profiles.csv": self.counselor_profiles,
            "mood_logs.csv": self.mood_logs,
            "appointments.csv": self.appointments,
            "chat_sessions.csv": self.chat_sessions,
            "chat_messages.csv": self.chat_messages,
            "notifications.csv": self.notifications,
        }

        for filename, data in datasets.items():
            if not data:
                continue
            filepath = self.output_dir / filename
            with open(filepath, "w", newline="", encoding="utf-8-sig") as f:
                writer = csv.DictWriter(f, fieldnames=data[0].keys())
                writer.writeheader()
                writer.writerows(data)
            files_created.append(filepath)
            print(f"  [CSV] {filepath} ({len(data)} 行)")

        return files_created

    def export_sql(self):
        """导出为 SQL 插入语句"""
        self.output_dir.mkdir(parents=True, exist_ok=True)
        filepath = self.output_dir / "mock_data.sql"

        table_map = {
            "user": self.users,
            "counselor_profile": self.counselor_profiles,
            "mood_log": self.mood_logs,
            "appointment": self.appointments,
            "chat_session": self.chat_sessions,
            "chat_message": self.chat_messages,
            "sys_notification": self.notifications,
        }

        with open(filepath, "w", encoding="utf-8") as f:
            f.write("-- ============================================\n")
            f.write("-- MindEase Mock Data - Generated by generate_mock_data.py\n")
            f.write(f"-- Generated at: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
            f.write("-- ============================================\n\n")

            for table_name, data in table_map.items():
                if not data:
                    continue
                columns = list(data[0].keys())
                f.write(f"-- Table: {table_name} ({len(data)} rows)\n")

                for row in data:
                    values = []
                    for col in columns:
                        val = row.get(col)
                        if val is None:
                            values.append("NULL")
                        elif isinstance(val, (int, float)):
                            values.append(str(val))
                        elif isinstance(val, str):
                            escaped = val.replace("'", "''")
                            values.append(f"'{escaped}'")
                        else:
                            values.append(f"'{val}'")
                    f.write(f"INSERT INTO `{table_name}` (`{'`, `'.join(columns)}`) "
                            f"VALUES ({', '.join(values)});\n")
                f.write("\n")

        print(f"  [SQL] {filepath}")
        return [filepath]

    def export_json(self):
        """导出为 JSON 文件（便于调试）"""
        self.output_dir.mkdir(parents=True, exist_ok=True)
        filepath = self.output_dir / "mock_data.json"

        all_data = {
            "meta": {
                "generated_at": datetime.now().isoformat(),
                "counts": {
                    "users": len(self.users),
                    "counselor_profiles": len(self.counselor_profiles),
                    "mood_logs": len(self.mood_logs),
                    "appointments": len(self.appointments),
                    "chat_sessions": len(self.chat_sessions),
                    "chat_messages": len(self.chat_messages),
                    "notifications": len(self.notifications),
                },
            },
            "data": {
                "users": self.users,
                "counselor_profiles": self.counselor_profiles,
                "mood_logs": self.mood_logs,
                "appointments": self.appointments,
                "chat_sessions": self.chat_sessions,
                "chat_messages": self.chat_messages,
                "notifications": self.notifications,
            },
        }

        with open(filepath, "w", encoding="utf-8") as f:
            json.dump(all_data, f, indent=2, ensure_ascii=False, default=str)

        print(f"  [JSON] {filepath}")
        return [filepath]

    # --------------------------------------------------------
    # 主流程
    # --------------------------------------------------------

    def run(self, export_format: str = "csv"):
        """执行完整的数据生成流程"""
        print("=" * 60)
        print("MindEase Mock Data Generator")
        print("=" * 60)

        # 1. 生成基础数据
        self.generate_users()
        self.generate_counselor_profiles()

        # 2. 生成业务数据
        self.generate_mood_logs()
        self.generate_appointments()
        self.generate_chat_data()
        self.generate_notifications()  # 新增：生成通知数据

        # 3. 导出
        print(f"\n[INFO] 导出格式: {export_format.upper()}")
        print("-" * 40)

        if export_format == "csv":
            files = self.export_csv()
        elif export_format == "sql":
            files = self.export_sql()
        elif export_format == "json":
            files = self.export_json()
        else:
            # 导出所有格式
            files = self.export_csv() + self.export_sql() + self.export_json()

        # 4. 输出统计
        print("\n" + "=" * 60)
        print("数据生成完成！统计信息:")
        print(f"  用户数:           {len(self.users)}")
        print(f"  咨询师档案:       {len(self.counselor_profiles)}")
        print(f"  心情日志:         {len(self.mood_logs)}")
        print(f"  预约记录:         {len(self.appointments)}")
        print(f"  聊天会话:         {len(self.chat_sessions)}")
        print(f"  聊天消息:         {len(self.chat_messages)}")
        print(f"  通知记录:         {len(self.notifications)}")
        print(f"  输出目录:         {self.output_dir.absolute()}")
        print("=" * 60)

        return files


# ============================================================
# CLI 入口
# ============================================================

def main():
    parser = argparse.ArgumentParser(
        description="MindEase 模拟数据生成器 - 生成测试用的假数据",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
示例:
  python generate_mock_data.py                     # 使用默认参数生成 CSV
  python generate_mock_data.py --format sql         # 生成 SQL 插入语句
  python generate_mock_data.py --users 100 --format all  # 100个用户，导出全部格式
        """,
    )
    parser.add_argument(
        "--format", "-f",
        choices=["csv", "sql", "json", "all"],
        default="csv",
        help="导出格式 (默认: csv)"
    )
    parser.add_argument(
        "--users", "-u",
        type=int,
        default=30,
        help="生成的用户数量 (默认: 30)"
    )
    parser.add_argument(
        "--moods", "-m",
        type=int,
        default=10,
        help="每用户的心情日志数 (默认: 10)"
    )
    parser.add_argument(
        "--appointments", "-a",
        type=int,
        default=2,
        help="每用户的预约数 (默认: 2)"
    )
    parser.add_argument(
        "--sessions", "-s",
        type=int,
        default=3,
        help="每用户的聊天会话数 (默认: 3)"
    )
    parser.add_argument(
        "--messages-per-session",
        type=int,
        default=8,
        help="每个会话的消息数 (默认: 8)"
    )
    parser.add_argument(
        "--output", "-o",
        default="./data",
        help="输出目录 (默认: ./data)"
    )
    parser.add_argument(
        "--seed",
        type=int,
        default=None,
        help="随机种子，用于可重复生成"
    )

    args = parser.parse_args()

    generator = MockDataGenerator(
        output_dir=args.output,
        num_users=args.users,
        num_mood_logs_per_user=args.moods,
        num_appointments_per_user=args.appointments,
        num_chat_sessions_per_user=args.sessions,
        num_messages_per_session=args.messages_per_session,
        seed=args.seed,
    )
    generator.run(export_format=args.format)


if __name__ == "__main__":
    main()
