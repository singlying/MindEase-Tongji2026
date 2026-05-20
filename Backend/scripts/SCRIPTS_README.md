# MindEase 脚本工具集

> 本目录下的所有脚本均为**独立工具**，不会修改项目源代码，可安全用于开发、测试和运维场景。

---

## 目录

| 脚本 | 类型 | 用途 | 依赖 |
|------|------|------|------|
| `generate_mock_data.py` | 数据生成 | 生成模拟用户/心情/预约/聊天/**通知**数据 | Python 3.7+ (仅标准库) |
| `generate_assessment_data.py` | 数据生成 | 生成 PHQ-9/GAD-7/PSQI/PSS-10/**SDS** 等心理评估数据 | Python 3.7+ (仅标准库) |
| `seed_database.sql` | 数据库种子 | 建表 + 插入基础测试数据 (**4咨询师** + 通知示例) | MySQL 客户端 |
| `api_tester.py` | 质量保证 | API 自动化功能测试 (**8模块27+用例**) | `pip install requests` |
| `load_test.py` | 质量保证 | 性能压力测试 (并发/响应时间/吞吐量/**预热**) | `pip install requests` |
| `health_check.py` | 运维监控 | 服务健康检查 (后端/DB/Redis/AI/磁盘/**日志**/**连接池**) | 部分功能需 pymysql/redis |

---

## 快速开始

```bash
# 进入脚本目录
cd scripts/

# 安装通用依赖
pip install requests pymysql redis  # 可选，部分脚本会优雅降级
```

---

## 1. 模拟数据生成器 (`generate_mock_data.py`)

生成多维度测试数据，支持 CSV / SQL / JSON 三种格式导出。

### 基本用法

```bash
# 默认: 生成30个用户, 导出为 CSV 到 ./data/
python generate_mock_data.py

# 自定义参数
python generate_mock_data.py --users 100 --moods 20 --sessions 5 --format sql

# 导出全部格式 (CSV + SQL + JSON)
python generate_mock_data.py --format all -o ./test-data

# 使用随机种子确保可重复性
python generate_mock_data.py --seed 42 --format csv
```

### 参数说明

| 参数 | 缩写 | 默认值 | 说明 |
|------|------|--------|------|
| `--users` | `-u` | 30 | 用户数量 |
| `--moods` | `-m` | 10 | 每用户心情日志数 |
| `--appointments` | `-a` | 2 | 每用户预约数 |
| `--sessions` | `-s` | 3 | 每用户聊天会话数 |
| `--messages-per-session` | — | 8 | 每会话消息数 |
| `--format` | `-f` | `csv` | 输出格式: `csv` / `sql` / `json` / `all` |
| `--output` | `-o` | `./data` | 输出目录 |
| `--seed` | — | None | 随机种子 |

### 生成的数据实体

- **User**: 用户账号（含 USER/COUNSELOR/ADMIN 角色）
- **CounselorProfile**: 咨询师档案（专长/评分/简介）
- **MoodLog**: 心情日志（**10种**情绪类型含 grateful/hopeful + 中文内容模板）
- **Appointment**: 预约记录（含 NO_SHOW 状态）
- **ChatSession / ChatMessage**: 聊天会话和多轮对话消息
- **SysNotification**: 系统通知（APPOINTMENT/SYSTEM/ASSESSMENT/REMINDER/COMMUNITY）

### 输出结构

```
data/
├── users.csv              # 用户数据
├── counselor_profiles.csv # 咨询师档案
├── mood_logs.csv          # 心情日志
├── appointments.csv       # 预约记录
├── chat_sessions.csv      # 聊天会话
├── chat_messages.csv      # 聊天消息
├── notifications.csv      # 系统通知 (新增)
├── mock_data.sql          # SQL 插入语句 (--format sql 时)
└── mock_data.json         # 完整 JSON (--format json 时)
```

---

## 2. 心理评估数据生成器 (`generate_assessment_data.py`)

生成符合真实评分标准的心理量表测试数据。

### 支持的量表

| 量表 | 全称 | 分数范围 | 用途 |
|------|------|----------|------|
| **PHQ-9** | 患者健康问卷 | 0-27 | 抑郁筛查 |
| **GAD-7** | 广泛性焦虑障碍量表 | 0-21 | 焦虑筛查 |
| **PSQI** | 匹兹堡睡眠质量指数 | 0-21 | 睡眠质量 |
| **PSS-10** | 知觉压力量表 | 0-40 | 压力水平 |
| **SDS** (新增) | 抑郁自评量表 | 20-80 | 抑郁程度（含正向/反向计分） |

### 基本用法

```bash
# 默认: 50个用户, 全部量表, 每人3次评估
python generate_assessment_data.py

# 仅生成 PHQ-9 和 GAD-7
python generate_assessment_data.py --scales phq-9 gad-7

# 200个用户, 每人5次评估, 仅输出SQL
python generate_assessment_data.py --users 200 -a 5 -f sql

# 生成统计摘要 (JSON)
python generate_assessment_data.py -f json
```

### 特色功能

- **真实评分分布**: 按临床比例分配正常/轻度/中重度案例
- **反向答案推导**: 从总分反推各题作答，保证分数一致性
- **PSS-10 特殊计分**: 正确处理正向题反向计分规则
- **SDS 标准分转换** (新增): 支持抑郁自评量表的20题反向计分
- **等级建议文本**: 根据结果等级自动生成专业建议
- **统计摘要**: 自动输出各量表得分分布直方图

---

## 3. 数据库种子数据 (`seed_database.sql`)

一键初始化开发/测试环境的数据库。

### 使用方法

```bash
# 方式1: 直接导入
mysql -u root -p mindease_db < seed_database.sql

# 方式2: 在 MySQL CLI 中执行
mysql -u root -p
source scripts/seed_database.sql
```

### 包含内容

| 内容 | 数量 | 说明 |
|------|------|------|
| 管理员账号 | 1 | `admin / Admin123456` |
| 测试用户 | 5 | `test_user`, `demo_user`, `alice`, `bob`, `carol` |
| 测试咨询师 | 4 | `dr_zhang`(CBT), `dr_li`(家庭), `dr_wang`(创伤), `dr_chen`(青少年) |
| 咨询师档案 | 4 | 含完整专长/评分/简介 |
| 心情日志 | 6 | 含 grateful/hopeful 新类型示例 |
| 预约记录 | 3 | 含 PENDING/CONFIRMED/COMPLETED/CANCELLED 各状态 |
| 评估记录 | 3 | PHQ-9 / GAD-7 / PSS-10 各一例 |
| 聊天会话+消息 | 1会话+4条 | 多轮对话示例 |
| **系统通知** (新增) | **5条** | APPOINTMENT/SYSTEM/ASSESSMENT/REMINDER/COMMUNITY 各一 |

### 注意事项

> ⚠️ 此 SQL 包含 `DROP TABLE` + `CREATE TABLE` 语句，**仅限开发/测试环境使用**！

---

## 4. API 自动化测试 (`api_tester.py`)

对全部 API 接口进行自动化功能验证。

### 基本用法

```bash
# 运行全部测试 (服务需先启动)
python api_tester.py

# 指定目标地址
python api_tester.py --url http://localhost:8080

# 仅测试特定模块
python api_tester.py --module auth user assessment

# 不生成报告
python api_tester.py --no-report
```

### 测试覆盖

| 模块 | 用例数 | 测试内容 |
|------|--------|----------|
| **Auth (认证)** | 5 | 正常登录、密码错误、用户不存在、参数缺失、注册新用户 |
| **User (用户)** | 5 | 个人信息、心情日志CRUD、预约列表与创建 |
| **Counselor (咨询师)** | 2 | 列表查询、详情获取(含404正确性) |
| **AI 对话** | 3 | AI对话回复、聊天历史、ASR接口 |
| **Assessment (评估)** | 3 | 量表列表、提交评估(PHQ-9)、历史查询 |
| **Notification (通知)** (新增) | 3 | 通知列表、未读通知数、标记已读 |
| **Dashboard (仪表盘)** (新增) | 2 | 统计数据概览、心情趋势图 |
| **异常场景** | 4 | 无Token访问、无效Token、不存在端点、超大Payload |

### 输出产物

```
scripts/
└── test_report.html    # HTML 可视化测试报告 (含通过率/耗时/详情表)
```

### 前置条件

1. 后端服务已启动 (`mvn spring-boot:run`)
2. 已执行 `seed_database.sql` 初始化测试数据
3. 或至少存在 `test_user / Test123456` 账号

---

## 5. 性能压力测试 (`load_test.py`)

对核心 API 进行并发压测。

### 基本用法

```bash
# 默认配置: 每场景100请求, 10并发
python load_test.py

# 高压模式
python load_test.py --concurrent 50 --requests 500

# 只压测登录接口
python load_test.py -s auth_login

# 压测自定义端点
python load_test.py --target "/api/counselors" --method GET
python load_test.py --target "/api/ai/chat" POST '{"message":"hello"}'
```

### 内置压测场景

| 场景标识 | 名称 | 认证 | 说明 |
|----------|------|------|------|
| `auth_login` | 登录接口 | 否 | 用户登录 |
| `user_profile` | 个人信息 | 是 | GET 用户资料 |
| `mood_list` | 心情日志列表 | 是 | 分页查询 |
| `counselor_list` | 咨询师列表 | 否 | 分页查询 |
| `ai_chat` | AI对话 | 是 | AI回复 (较慢) |
| `assessment_submit` | 提交评估 | 是 | PHQ-9 提交 |
| `notification_list` (新增) | 通知列表 | 是 | 分页查询通知 |
| `dashboard_stats` (新增) | 仪表盘统计 | 是 | 获取统计数据 |

### 预热机制 (新增)

```bash
# 使用预热：每场景先发10个请求预热JVM连接池，再开始正式压测
python load_test.py --warmup 10 -c 50 -n 200
```

> 预热请求会在正式压测前执行，帮助 JVM JIT 编译和数据库连接池达到稳定状态。

### 输出指标

每个场景自动计算并展示：
- **吞吐量**: req/s (每秒请求数)
- **响应时间**: 平均值 / 中位数 / P95 / P99 / 最大值 / 标准差
- **成功率**: 通过百分比
- **性能评级**: 绿色优秀 (<200ms) / 黄色警告 (>2s) / 红色危险 (>5s)

### 输出产物

```
scripts/
└── load_test_results.json   # 详细结果JSON (供CI/CD或二次分析)
```

---

## 6. 服务健康检查 (`health_check.py`)

全面检测系统各组件状态，支持 CI/CD 集成和告警通知。

### 基本用法

```bash
# 完整检查 (所有组件)
python health_check.py

# 快速模式 (仅核心三项: 后端+DB+Redis)
python health_check.py --quick

# JSON 格式输出 (便于解析)
python health_check.py --json

# 保存到文件
python health_check.py -o health_report.json

# 发送告警 (钉钉机器人)
python health_check.py --webhook https://oapi.dingtalk.com/robot/send?access_token=xxx

# CI/CD 集成 (根据健康状况返回退出码)
python health_check.py --exit-code && echo "healthy" || echo "unhealthy"
```

### 检查项

| 组件 | 快速模式 | 检查内容 | 降级方案 |
|------|----------|----------|----------|
| **Backend Service** | ✅ | HTTP 连通性 / Health Endpoint | Socket 端口检测 |
| **Database (MySQL)** | ✅ | 连接 / 版本 / 表数量 | mysql-cli 命令行 |
| **Redis Cache** | ✅ | 连接 / 内存 / 客户端数 | redis-cli 命令行 |
| **AI Service (DashScope)** | — | API Key / 网络连通 / 模型调用 | — |
| **Disk Space** | — | 可用空间 / 使用率 | — |
| **JVM Process** | — | 进程存活 / 内存占用(RSS) / Actuator | pgrep + ps 检测 |
| **Connection Pool** (新增) | — | 数据库连接池健康状态 (Actuator) | 降级为 UNKNOWN |
| **Log Files** (新增) | — | 日志文件大小 / 数量 / 大文件告警 | 目录不存在时跳过 |

### 状态说明

| 状态 | 图标 | 含义 | 建议 |
|------|------|------|------|
| `HEALTHY` | ✅ | 组件运行正常 | 无需操作 |
| `DEGRADED` | ⚠️ | 组件可用但有问题 | 参考下方修复建议 |
| `UNHEALTHY` | ❌ | 组件不可用 | 立即排查 |
| `UNKNOWN` | ❓ | 无法判断 | 可能缺少依赖库 |

---

## 组合工作流示例

### 开发环境初始化

```bash
# 1. 初始化数据库
mysql -u root -p mindease_db < scripts/seed_database.sql

# 2. 批量生成测试数据
python scripts/generate_mock_data.py --users 50 --format sql
python scripts/generate_assessment_data.py --users 50 -f sql

# 3. 启动后端服务
mvn spring-boot:run

# 4. 健康检查确认一切正常
python scripts/health_check.py

# 5. 运行API功能测试
python scripts/api_tester.py

# 6. (可选) 性能基准测试
python scripts/load_test.py --concurrent 20 --requests 200
```

### CI/CD 流水线集成

```yaml
# .gitlab-ci.yml 示意
stages:
  - health-check
  - test
  - load-test

health-check:
  stage: health-check
  script:
    - python scripts/health_check.py --quick --exit-code

api-test:
  stage: test
  script:
    - python scripts/api_tester.py --no-report
  artifacts:
    paths:
      - scripts/test_report.html

load-test:
  stage: load-test
  script:
    - python scripts/load_test.py --concurrent 30 --requests 300
  artifacts:
    paths:
      - scripts/load_test_results.json
  allow_failure: true  # 压测失败不阻断流水线
```

### 定期监控告警 (Cron + Webhook)

```bash
# crontab -e
# 每5分钟检查一次，异常时发送钉钉告警
*/5 * * * * cd /path/to/scripts && python health_check.py --quick --webhook $DINGTALK_WEBHOOK_URL
```

---

## 常见问题

### Q: 脚本报错缺少模块？
```bash
pip install requests pymysql redis
```
所有脚本在缺少可选依赖时会**优雅降级**，不会直接崩溃。

### Q: 如何自定义数据生成的参数？
每个 Python 脚本都支持 `--help` 查看完整参数列表：
```bash
python generate_mock_data.py --help
python generate_assessment_data.py --help
python api_tester.py --help
python load_test.py --help
python health_check.py --help
```

### Q: 生成的数据会影响生产环境？
不会。所有脚本默认输出到 `scripts/data/` 或当前目录下的子目录。`seed_database.sql` 需要手动执行，且文件开头有明确的安全提示。

### Q: 数据是否安全？
所有模拟数据均为程序随机生成，不包含任何真实个人信息。
