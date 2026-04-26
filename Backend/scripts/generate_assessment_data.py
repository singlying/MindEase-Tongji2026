#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
MindEase 心理评估数据生成器
============================
生成 PHQ-9、GAD-7 等标准化心理量表评估记录，用于测试评估模块。

支持量表:
    - PHQ-9 (患者健康问卷-9项): 抑郁筛查
    - GAD-7 (广泛性焦虑障碍-7项): 焦虑筛查
    - PSQI (匹兹堡睡眠质量指数): 睡眠质量
    - PSS (知觉压力量表): 压力水平

使用方式:
    python generate_assessment_data.py --users 50
    python generate_assessment_data.py --scales phq-9 gad-7 --format sql
"""

import argparse
import csv
import json
import random
from datetime import datetime, timedelta
from pathlib import Path


# ============================================================
# 量表定义（基于真实评分标准）
# ============================================================

SCALE_DEFINITIONS = {
    "PHQ-9": {
        "name": "患者健康问卷 (PHQ-9)",
        "description": "抑郁症状自评量表",
        "questions": [
            "做事时提不起劲或没有兴趣",
            "感到心情低落、沮丧或绝望",
            "入睡困难、睡不着或睡眠过多",
            "感觉疲倦或没有活力",
            "食欲不振或吃得太多",
            "觉得自己很糟——或觉得自己很失败",
            "对事物集中注意力困难",
            "动作或说话速度缓慢到别人已经察觉",
            "有想死或伤害自己的念头",
        ],
        "options": ["完全不会", "好几天", "一半以上的天数", "几乎每天"],
        "score_range": (0, 27),  # 每题0-3分，共9题
        "result_levels": {
            (0, 4): "正常",
            (5, 9): "轻度抑郁",
            (10, 14): "中度抑郁",
            (15, 19): "中重度抑郁",
            (20, 27): "重度抑郁",
        },
        "risk_weights": {"normal": 0.35, "mild": 0.30, "moderate": 0.20,
                         "moderately severe": 0.10, "severe": 0.05},
    },
    "GAD-7": {
        "name": "广泛性焦虑障碍量表 (GAD-7)",
        "description": "焦虑症状自评量表",
        "questions": [
            "感到过度担心",
            "无法控制担心",
            "对各种事情担心太多",
            "很难放松下来",
            "坐立不安，难以静坐",
            "容易变得烦躁或急躁",
            "感到害怕，好像有什么可怕的事情会发生",
        ],
        "options": ["完全不会", "好几天", "一半以上的天数", "几乎每天"],
        "score_range": (0, 21),
        "result_levels": {
            (0, 4): "正常",
            (5, 9): "轻度焦虑",
            (10, 14): "中度焦虑",
            (15, 21): "重度焦虑",
        },
        "risk_weights": {"minimal": 0.40, "mild": 0.25, "moderate": 0.20, "severe": 0.15},
    },
    "PSQI": {
        "name": "匹兹堡睡眠质量指数 (PSQI)",
        "description": "睡眠质量评估量表",
        "questions": [
            "通常上床时间",
            "通常入睡需要多少分钟",
            "通常早晨起床时间",
            "实际睡眠时长(小时)",
            "过去一个月是否有以下情况：入睡困难(30分钟内不能入睡)",
            "过去一个月是否有以下情况：夜间易醒或早醒",
            "过去一个月是否有以下情况：因不适而夜起(如上厕所、呼吸不畅)",
            "过去一个月是否有以下情况：服用安眠药物",
            "总体评价过去一个月的睡眠质量",
            "过去一个月是否经常感到困倦或精力不足",
            "过去一个月是否在开车、吃饭或参加社交活动时困倦",
            "过去一个月当你打算做事时是否提不起劲来",
            "对目前的睡眠模式/作息是否满意",
            *["主观睡眠质量", "入睡时间", "睡眠时间", "睡眠效率",
              "睡眠障碍", "催眠药物", "日间功能障碍"],  # 7个成分评分
        ],
        "options": None,  # PSQI 有特殊计分方式
        "score_range": (0, 21),
        "result_levels": {
            (0, 6): "睡眠质量良好",
            (7, 10): "睡眠质量一般",
            (11, 21): "睡眠质量较差",
        },
        "risk_weights": {"good": 0.45, "fair": 0.30, "poor": 0.25},
    },
    "PSS-10": {
        "name": "知觉压力量表 (PSS-10)",
        "description": "压力感知程度评估",
        "questions": [
            "在过去一个月里，你有多少次因为发生意料之外的事情而感到心烦意乱？",
            "在过去一个月里，你有多少次感觉到无法控制生活中的重要事情？",
            "在过去一个月里，你有多少次感到紧张和压力？",
            "在过去一个月里，你有多少次成功地处理了令人烦恼的生活麻烦？",
            "在过去一个月里，你有多少次感觉到能够有效处理生活中重要的事情？",
            "在过去一个月里，你有多少次感觉到事情按自己的意愿发展？",
            "在过去一个月里，你有多少次发现不能处理所有自己必须做的事情？",
            "在过去一个月里，你有多少次能够控制自己生活中的愤怒情绪？",
            "在过去一个月里，你有多少件事情是觉得自己可以驾驭的？",
            "在过去一个月里，你有多少次感觉到事情超出了自己的能力范围？",
        ],
        options=["从来没有", "几乎从来没有", "有时", "相当频繁", "非常频繁"],
        "score_range": (0, 40),  # 正向题反向计分
        "result_levels": {
            (0, 13): "低压力水平",
            (14, 26): "中等压力水平",
            (27, 40): "高压力水平",
        },
        "risk_weights": {"low": 0.35, "moderate": 0.40, "high": 0.25},
    },
}

# 评估结果建议文本（根据等级生成）
RECOMMENDATIONS = {
    "PHQ-9": {
        "正常": "继续保持良好的心理状态，建议定期进行自我监测。",
        "轻度抑郁": "建议进行自我调节，包括规律运动、保持社交、充足睡眠。如症状持续超过2周，建议寻求专业帮助。",
        "中度抑郁": "强烈建议尽快预约心理咨询师进行专业评估和治疗。",
        "中重度抑郁": "需要立即寻求精神科医生的专业诊断和治疗，可能需要药物治疗配合心理治疗。",
        "重度抑郁": "请立即前往医院精神科就诊，这是严重的心理健康问题，需要专业医疗干预。如有伤害自己的念头，请立即联系紧急服务。",
    },
    "GAD-7": {
        "正常": "焦虑水平在正常范围内，继续保持良好的生活习惯。",
        "轻度焦虑": "尝试放松训练、正念冥想等自助方法，减少咖啡因摄入。",
        "中度焦虑": "建议咨询心理咨询师，学习认知行为疗法等有效的应对策略。",
        "重度焦虑": "强烈建议寻求精神科医生帮助，可能需要药物治疗配合心理治疗。",
    },
}


class AssessmentDataGenerator:
    """心理评估数据生成器"""

    def __init__(self, output_dir: str = "./data", num_users: int = 50,
                 scales: list = None, assessments_per_user: int = 3,
                 seed: int = None):
        self.output_dir = Path(output_dir)
        self.num_users = num_users
        self.scales = scales or list(SCALE_DEFINITIONS.keys())
        self.assessments_per_user = assessments_per_user

        if seed is not None:
            random.seed(seed)

        self.records = []
        # 预生成用户 ID 列表（与 generate_mock_data.py 保持一致）
        self.user_ids = [1000000 + i * 111 + random.randint(1, 999) for i in range(num_users)]

    def _gen_score_for_level(self, scale_key: str) -> tuple:
        """根据风险权重分布生成分数和等级"""
        definition = SCALE_DEFINITIONS[scale_key]
        weights = list(definition["risk_weights"].values())
        levels = list(definition["risk_weights"].keys())

        chosen_level = random.choices(levels, weights=weights)[0]

        # 根据选中的等级，在对应分数范围内随机取值
        for (low, high), level_name in definition["result_levels"].items():
            if level_name == chosen_level or level_name.lower() == chosen_level.lower():
                score = random.randint(low, high)
                return score, level_name

        # fallback
        min_score, max_score = definition["score_range"]
        return random.randint(min_score, max_score), levels[0]

    def _gen_answers(self, scale_key: str, total_score: int) -> list:
        """根据总分反向生成各题答案"""
        definition = SCALE_DEFINITIONS[scale_key]
        num_questions = len(definition["questions"])
        answers = []

        if scale_key in ("PHQ-9", "GAD-7"):
            # 0-3 分制，共 N 题
            remaining_score = total_score
            max_per_question = 3

            for i in range(num_questions):
                if i == num_questions - 1:
                    # 最后一题分配剩余分值
                    score = min(max_per_question, max(0, remaining_score))
                else:
                    # 前面的题目随机分配，但保证总和接近目标
                    avg_remaining = remaining_score / (num_questions - i)
                    score = int(random.gauss(avg_remaining, 0.8))
                    score = max(0, min(max_per_question, score))

                remaining_score -= score
                answers.append({
                    "question_index": i + 1,
                    "question_text": definition["questions"][i],
                    "score": score,
                    "option": definition["options"][score] if definition["options"] else str(score),
                })

        elif scale_key == "PSS-10":
            # PSS-10 特殊计分：正向题(4,5,6,7,9) 反向计分
            positive_questions = {4, 5, 6, 7, 9}
            remaining_score = total_score

            for i in range(num_questions):
                is_positive = (i + 1) in positive_questions
                if i == num_questions - 1:
                    raw = min(4, max(0, remaining_score))
                else:
                    avg = remaining_score / (num_questions - i)
                    raw = int(random.gauss(avg, 0.6))
                    raw = max(0, min(4, raw))

                if is_positive:
                    display_score = 4 - raw  # 反向计分
                else:
                    display_score = raw

                remaining_score -= raw

                answers.append({
                    "question_index": i + 1,
                    "question_text": definition["questions"][i],
                    "score": display_score,
                    "raw_score": raw,
                    "is_reversed": is_positive,
                    "option": definition["options"][display_score],
                })

        else:  # PSQI 等
            for i in range(num_questions):
                answers.append({
                    "question_index": i + 1,
                    "question_text": definition["questions"][i],
                    "score": random.randint(0, 3),
                    "option": str(random.randint(0, 3)),
                })

        return answers

    def _gen_datetime(self, days_back: int = 90) -> datetime:
        now = datetime.now()
        return now - timedelta(
            days=random.randint(0, days_back),
            hours=random.randint(0, 23),
            minutes=random.randint(0, 59),
        )

    def _gen_id(self) -> int:
        return int(datetime.now().timestamp() * 1000000) % 1000000000 + random.randint(1, 99999)

    def generate(self):
        """生成评估数据"""
        print("=" * 60)
        print("MindEase Assessment Data Generator")
        print("=" * 60)
        print(f"[INFO] 用户数: {self.num_users}")
        print(f"[INFO] 量表:   {', '.join(self.scales)}")
        print(f"[INFO] 人均评估次数: {self.assessments_per_user}")
        print()

        total = self.num_users * self.assessments_per_user
        done = 0

        for user_id in self.user_ids:
            for _ in range(self.assessments_per_user):
                scale_key = random.choice(self.scales)
                score, result_level = self._gen_score_for_level(scale_key)
                answers = self._gen_answers(scale_key, score)

                # 获取建议文本
                rec_map = RECOMMENDATIONS.get(scale_key, {})
                recommendation = rec_map.get(result_level,
                    "建议根据评估结果采取相应的心理健康措施，必要时寻求专业帮助。")

                record = {
                    "id": self._gen_id(),
                    "user_id": user_id,
                    "scale_key": scale_key,
                    "scale_name": SCALE_DEFINITIONS[scale_key]["name"],
                    "total_score": score,
                    "result_level": result_level,
                    "answers": json.dumps(answers, ensure_ascii=False),
                    "recommendation": recommendation,
                    "create_time": self._gen_datetime(days_back=90).strftime("%Y-%m-%d %H:%M:%S"),
                }
                self.records.append(record)

            done += self.assessments_per_user
            if done % 500 == 0:
                print(f"[进度] 已生成 {done}/{total} 条记录...")

        self.records.sort(key=lambda r: r["create_time"])
        print(f"[OK] 共生成 {len(self.records)} 条评估记录")

    def export_csv(self):
        self.output_dir.mkdir(parents=True, exist_ok=True)

        filepath = self.output_dir / "assessment_records.csv"
        with open(filepath, "w", newline="", encoding="utf-8-sig") as f:
            writer = csv.DictWriter(f, fieldnames=self.records[0].keys())
            writer.writeheader()
            writer.writerows(self.records)
        print(f"  [CSV] {filepath} ({len(self.records)} 行)")
        return str(filepath)

    def export_sql(self):
        self.output_dir.mkdir(parents=True, exist_ok=True)

        filepath = self.output_dir / "assessment_data.sql"
        columns = list(self.records[0].keys())

        with open(filepath, "w", encoding="utf-8") as f:
            f.write("-- MindEase Assessment Data\n")
            f.write(f"-- Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
            f.write(f"-- Total records: {len(self.records)}\n\n")

            for record in self.records:
                values = []
                for col in columns:
                    val = record[col]
                    if val is None:
                        values.append("NULL")
                    elif isinstance(val, (int, float)):
                        values.append(str(val))
                    elif isinstance(val, str):
                        values.append(f"'{val.replace(\"'\", \"''\")}'")
                    else:
                        values.append(f"'{val}'")
                f.write(f"INSERT INTO `assessment_record` (`{'`, `'.join(columns)}`) "
                        f"VALUES ({', '.join(values)});\n")

        print(f"  [SQL] {filepath}")
        return str(filepath)

    def export_statistics(self):
        """导出统计数据摘要"""
        self.output_dir.mkdir(parents=True, exist_ok=True)
        filepath = self.output_dir / "assessment_statistics.json"

        stats = {}
        for scale in self.scales:
            scale_records = [r for r in self.records if r["scale_key"] == scale]
            scores = [r["total_score"] for r in scale_records]

            level_counts = {}
            for r in scale_records:
                level_counts[r["result_level"]] = level_counts.get(r["result_level"], 0) + 1

            stats[scale] = {
                "total_count": len(scale_records),
                "score_avg": round(sum(scores) / len(scores), 2) if scores else 0,
                "score_min": min(scores) if scores else 0,
                "score_max": max(scores) if scores else 0,
                "level_distribution": level_counts,
            }

        output = {
            "generated_at": datetime.now().isoformat(),
            "statistics": stats,
        }

        with open(filepath, "w", encoding="utf-8") as f:
            json.dump(output, f, indent=2, ensure_ascii=False)

        print(f"  [STAT] {filepath}")

        # 打印摘要
        print("\n--- 评估数据统计摘要 ---")
        for scale, s in stats.items():
            print(f"\n[{scale}] {SCALE_DEFINITIONS[scale]['name']}")
            print(f"  总记录数: {s['total_count']}")
            print(f"  平均分:  {s['score_avg']:.1f} / {SCALE_DEFINITIONS[scale]['score_range'][1]}")
            print(f"  分数范围: {s['score_min']} ~ {s['score_max']}")
            print(f"  等级分布:")
            for level, count in sorted(s['level_distribution'].items(),
                                        key=lambda x: x[1], reverse=True):
                pct = count / s['total_count'] * 100
                bar = "#" * int(pct / 2)
                print(f"    {level:12s}: {count:4d} ({pct:5.1f}%) {bar}")

        return str(filepath)

    def run(self, export_format: str = "all"):
        self.generate()

        print(f"\n[INFO] 导出格式: {export_format.upper()}")
        print("-" * 40)

        files = []
        if export_format in ("csv", "all"):
            files.append(self.export_csv())
        if export_format in ("sql", "all"):
            files.append(self.export_sql())
        if export_format in ("json", "all"):
            files.append(self.export_statistics())

        print("\n" + "=" * 60)
        print("评估数据生成完成！")
        print(f"输出目录: {self.output_dir.absolute()}")
        print("=" * 60)


def main():
    parser = argparse.ArgumentParser(description="MindEase 心理评估数据生成器")
    parser.add_argument("--users", "-u", type=int, default=50, help="用户数量 (默认: 50)")
    parser.add_argument("--scales", "-s", nargs="+",
                        choices=list(SCALE_DEFINITIONS.keys()),
                        default=list(SCALE_DEFINITIONS.keys()),
                        help="要生成的量表 (默认: 全部)")
    parser.add_argument("--assessments-per-user", "-a", type=int, default=3,
                        help="每人评估次数 (默认: 3)")
    parser.add_argument("--output", "-o", default="./data", help="输出目录")
    parser.add_argument("--format", "-f", choices=["csv", "sql", "json", "all"], default="all")
    parser.add_argument("--seed", type=int, default=None, help="随机种子")

    args = parser.parse_args()

    generator = AssessmentDataGenerator(
        output_dir=args.output,
        num_users=args.users,
        scales=args.scales,
        assessments_per_user=args.assessments_per_user,
        seed=args.seed,
    )
    generator.run(args.format)


if __name__ == "__main__":
    main()
