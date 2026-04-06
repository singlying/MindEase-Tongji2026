# MindEase\-WBS\-ProjectCharter

# Project General Information

- **Project Name \(项目名称\):** MindEase: 基于 LLM的智能心理支持与虚拟情感陪护系统

- **Team Leader \(项目经理/组长\):** 2252748 王昭慧

- **Team Members \(团队成员\):** 2253377 李航 2351883 陈奕名 2250832 李杜若

- **Project Start Date \(项目启动日期\):** 2026\-03\-30

- **Project End Date \(项目结项日期\):** 2026\-06\-14

- **Total Duration \(总日历周期\):** 77 天 \(11 周\)

- **Total Estimated Effort \(预估总工时\):** 570 工作小时

# Requirement Breakdown Structure \(RBS\)

## **项目业务定义与管理需求 \(Business Definition \& PM Requirements\)**

- **2\.1\.1 团队与流程规范 \(Team \& Process\)**

    - *2\.1\.1\.1 核心团队职责边界与审批流体系*

- **2\.1\.2 市场对标与立项规格 \(Market \& Specifications\)**

    - *2\.1\.2\.1 陪伴类竞品（如Woebot）痛点与隐私分析*

    - *2\.1\.2\.2 项目章程与系统需求规格说明书*

## **系统蓝图与架构设计需求 \(Architecture \& Design Requirements\)**

- **2\.2\.1 核心软硬件架构选型 \(Architecture Topology\)**

    - *2\.2\.1\.1 高并发低延迟大模型与向量库（Milvus等）拓扑结构*

    - *2\.2\.1\.2 生产级高可用云环境与 GPU 服务器部署标准*

- **2\.2\.2 核心数据引擎底座 \(Data Foundation\)**

    - *2\.2\.2\.1 支撑高安全存储的数据库 ER 图与核心表结构，包含针对 JSON 字段的虚拟列与 B\-Tree 索引优化机制*

- **2\.2\.3 沉浸式产品交互蓝图 \(UX/UI Blueprint\)**

    - *2\.2\.3\.1 覆盖三端（用户/医生/管理）的高保真全套视觉原型与拟人动效*

## 核心功能与 AI 实现需求 \(Core System \& AI Requirements\)

- **2\.3\.1 虚拟情感陪护模块 \(Virtual Emotional Companionship\)**

    - *2\.3\.1\.1 深度共情对话特性：* 基于大模型与 RAG 语料库的逻辑连贯生成引擎，并引入流式数据传输（Streaming/SSE）以保障低延迟的打字机交互体验

    - *2\.3\.1\.2 多模态沉浸交互特性：* 语音/文本转换前端与虚拟代理\(Avatar\)视效反馈。

- **2\.3\.2 日常情绪追踪模块 \(Daily Emotion Tracking\)**

    - *2\.3\.2\.1 NLP 智能标签算法特性：* 针对用户输入的多维情绪自动化标注算法。

    - *2\.3\.2\.2 轻量化记录前端特性：* Emoji与短文本/语音日记交互组件。

- **2\.3\.3 专业测评与服务闭环模块 \(Assessment \& Service Loop\)**

    - *2\.3\.3\.1 标准化测评与预约前台特性：* 量表UI、咨询师浏览与双向排期日历交互。

    - *2\.3\.3\.2 智能匹配与危机干预特性：* 医生对口匹配算法与 100% 极危语义拦截逻辑。包含紧急干预机制（Emergency Override）：高危触发时立即冻结AI对话并强制弹出救援热线 UI

- **2\.3\.4 后端基座与权限模块 \(Backend \& Authentication\)**

    - *2\.3\.4\.1 多角色隔离特性：* 用户鉴权、医生/管理员权限隔离与基础增删改查 API。

## **测试、验证与安全性需求 \(Testing \& QA Requirements\)**

- **2\.4\.1 健壮性与连贯打通 \(Robustness \& Integration\)**

    - *2\.4\.1\.1 跨模块业务联调特性：* 对话、画像与预约等多数据流的全链路集成。

    - *2\.4\.1\.2 微服务接口高抗压测试基准*

- **2\.4\.2 AI 安全与验收标准 \(AI Safety \& Acceptance\)**

    - *2\.4\.2\.1 多模态延迟、AI 幻觉及高危干预 100% 达标验证*

    - *2\.4\.2\.2 灰度验收满意度达 85% 以上的用户体验基准*

## 交付**、部署与运维归档需求 \(Deployment \& Closing Requirements\)**

- **2\.5\.1 系统运维支撑 \(System Maintenance Docs\)**

    - *2\.5\.1\.1 标准化的操作手册、维护文档及 API 接口白皮书*

- **2\.5\.2 资产沉淀与展示 \(Asset Archiving\)**

    - *2\.5\.2\.1 系统成果 Demo 视频、结项报告与全套源码归档库*

![Image](https://internal-api-drive-stream.feishu.cn/space/api/box/stream/download/authcode/?code=MWIzOGNiOTAxMzVkNTZkNmUxODE3ZjU4ZDY2NmRiNjNfYzM3MjgxMDQ0Yzk0Njc0M2M5MTYwNDUzOWQ3Yjc4MGRfSUQ6NzYyNzg5NDg0ODk1NTQwMzIwMl8xNzgyNzI4NTcyOjE3ODI4MTQ5NzJfVjM)

# Work Breakdown Structure \(WBS\)

## **Phase ****1****: 项目定义与需求细化 \(Initiation \& Requirements\) **

- **1\.1 项目启动与分工：** 确定核心团队职责边界；建立沟通机制与协作工具流。 `[需求 2.1.1.1]`

- **1\.2 需求收集与竞品分析：** 调研陪伴竞品；细化痛点并制定隐私保障策略。 `[需求 2.1.2.1]`

- **1\.3 编写 Project Charter 及需求书：** 输出《项目章程》与《规格书》，转化技术指标。 `[需求 2.1.2.2]`

## **Phase ****2****: 系统设计与蓝图构建 \(Design \& Architecture\)**

- **2\.1 系统整体架构设计与选型：** 确定大模型方案与向量库选型；设计高并发系统拓扑。 `[需求 2.2.1.1]`

- **2\.2 数据库 ER 图与表结构设计：** 设计支撑情绪记录、量表与医生排期的核心数据库表结构。 `[需求 2.2.2.1]`

- **2\.3 UI/UX 高保真原型设计：** 设计虚拟形象\(Avatar\)动效方案，完成三端全套视觉原型图。 `[需求 2.2.3.1]`

## **Phase ****3****: 系统开发与 AI 模型实现 \(Implementation\)**

- **3\.1 后端基础环境与 API 开发：** 搭建服务端框架，实现鉴权、权限隔离及基础增删改查，并完成 JSON 字段性能索引优化。 `[需求 2.3.4.1]`

- **3\.2 LLM 接入与 RAG 知识库构建：** 核心攻关：利用语料库完成向量入库，实现共情检索，并开发 SSE 流式数据接口以降低首字延迟**。** `[需求 2.3.1.1]`

- **3\.3 前端：陪护与日记界面开发：** 开发多模态组件、沉浸式对话与 Emoji/语音日记录入前端。 `[需求 2.3.1.2, 2.3.2.2]`

- **3\.4 前端：问卷与预约后台开发：** 实现量表测评界面交互、咨询师浏览与在线排期预约功能，抽象全局规范 UI 组件，并开发紧急热线弹窗前端。 `[需求 2.3.3.1]`

- **3\.5 后端：核心算法与复杂逻辑实现：** 编写 NLP情感算法、智能匹配算法及 100%危预警拦截逻辑，编写极危阈值触发与系统接管后逻辑。 `[需求 2.3.2.1, 2.3.3.2]`

- **3\.6 前后端联调与 AI 模块集成：** 打通全链路数据流转，确保分析与预约系统顺畅运行。 `[需求 2.4.1.1]`

## **Phase ****4****: 测试、验证与安全性评估 \(Testing \& QA\)**

- **4\.1 编写测试计划与用例：** 根据 10 轮对话等成功标准编写全覆盖的测试用例。 `[需求全体系]`

- **4\.2 单元测试与 API 联调测试：** 对微服务与接口进行压力测试，确保数据库读写健壮性。 `[需求 2.4.1.2]`

- **4\.3 性能、延迟与 AI 安全性测试：** 评估延迟及幻觉风险，验证高危语义拦截是否达 100%。 `[需求 2.4.2.1]`

- **4\.4 UAT 用户验收与缺陷修复：** 模拟真实用户灰度测试，修复 Bug 确保满意度达标。 `[需求 2.4.2.2]`

## **Phase ****5****: 交付、部署与结项归档 \(Deployment \& Closing\) **

- **5\.1 服务器配置与生产环境部署：** 配置 GPU 服务器与云环境，部署代码保障高可用。 `[需求 2.2.1.2]`

- **5\.2 编写用户手册与维护文档：** 输出《操作手册》、《系统说明》及《接口标准文档》。 `[需求 2.5.1.1]`

- **5\.3 结项报告、PPT与视频录制：** 整理项目资产，编写结项报告并录制系统演示 Demo。 `[需求 2.5.2.1]`

- **5\.4 答辩复盘与归档：** 总结应用经验，完成过程文档、测试报告与全部源代码的归档。 `[需求 2.5.2.1]`

![Image](https://internal-api-drive-stream.feishu.cn/space/api/box/stream/download/authcode/?code=ZWE4OTkxZjUzMDcyNDU0MzNlN2ZjMjQyZTljOTQ5NDJfNThmMGUyYTQ3MTJkMDcyODkyYTgzZjJiMWRjYzc2ZTNfSUQ6NzYyNzg5NDkwNzEzMDEyMTE0OF8xNzgyNzI4NTcyOjE3ODI4MTQ5NzJfVjM)



# Activity Schedule \& Dependencies

|**WBS ID**|**Activity Name \(任务活动名称\)**|**Duration \(日历天数\)**|**Predecessors \(前置任务\)**|**Est\. Hours \(预估工时\)**|
|---|---|---|---|---|
|**1\.1**|项目启动与分工|2|\-|8|
|**1\.2**|需求收集与竞品分析|5|1\.1|20|
|**1\.3**|编写 Project Charter 及需求书|5|1\.2|20|
|**2\.1**|系统整体架构设计与选型|3|1\.3|20|
|**2\.2**|数据库 ER 图与表结构设计|4|2\.1|20|
|**2\.3**|UI/UX 高保真原型设计|4|1\.3|30|
|**3\.1**|后端基础环境与 API 开发|7|2\.2|34|
|**3\.2**|LLM 接入与 RAG 知识库构建|10|2\.1|46|
|**3\.3**|前端：陪护与日记界面开发|10|2\.3|40|
|**3\.4**|前端：问卷与预约后台开发|10|2\.3|45|
|**3\.5**|后端：核心算法与复杂逻辑实现|10|3\.1|55|
|**3\.6**|前后端联调与 AI 模块集成|10|3\.2, 3\.3, 3\.4, 3\.5|30|
|**4\.1**|编写测试计划与用例|3|2\.3|16|
|**4\.2**|单元测试与 API 联调测试|4|3\.6|24|
|**4\.3**|性能、延迟与 AI 安全性测试|4|4\.2|30|
|**4\.4**|UAT 用户验收与缺陷修复|6|4\.3|54|
|**5\.1**|服务器配置与生产环境部署|5|4\.4|24|
|**5\.2**|编写用户手册与维护文档|3|5\.1|20|
|**5\.3**|结项报告、PPT与视频录制|3|5\.1|24|
|**5\.4**|答辩复盘与归档|2|5\.3|10|
|**TOTAL**|**项目总计**|**77 天**|\-|**5****7****0 小时**|

# **Project Gantt Chart \(项目甘特图\)**

时间轴分布参考（Timeline mapping）：

Phase 1 \(需求与启动\): 第 1\-2 周 \(3\.30 \- 4\.10\)

Phase 2 \(设计与架构\): 第 2\-3 周 \(4\.11 \- 4\.21\)

Phase 3 \(核心开发联调\): 第 4\-7 周 \(4\.22 \- 5\.18\) *\(多线并行\)*

Phase 4 \(测试与修复\): 第 8\-9 周 \(5\.19 \- 6\.04\)

Phase 5 \(部署与结项\): 第 10\-11 周 \(6\.05 \- 6\.14\)

![Image](https://internal-api-drive-stream.feishu.cn/space/api/box/stream/download/authcode/?code=OGExOGMxMzA3ODNmYmNmYWZlNWNiNmE2ZDRjZTZiMzBfNzliOTdmMzMzMTljNmY2OWIyMzY4ODM2MTQzM2ZlNGNfSUQ6NzYyMjY5ODYzODA5OTEwNjc2MV8xNzgyNzI4NTcyOjE3ODI4MTQ5NzJfVjM)



