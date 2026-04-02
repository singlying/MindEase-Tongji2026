这份**MindEase 智能心理支持与咨询系统项目开发规划方案**严格遵循了您提供的SRS需求文档、提案报告以及特定的技术架构约束。方案以**4周（敏捷开发）**为周期，针对**2名前端+2名后端**的团队配置进行了详细的任务拆分。

---

### 1. 项目架构细化设计

#### 1.1 架构合理性说明
本项目采用**前后端分离 + 分层架构**，前端通过 API 网关（Nginx）请求后端，后端负责业务逻辑并对接数据库与外部 AI 服务。
*   **适配性：**
    *   **开发效率**：4人团队，前后端解耦允许并行开发，互不阻塞。
    *   **维护成本**：分层清晰（Controller/Service/Dao），便于定位 Bug（例如 AI 响应慢查 Service，数据存取错查 Dao）。
    *   **扩展性**：未来若需开发移动端 App，只需复用后端 API，无需重构核心逻辑。

#### 1.2 核心模块划分及交互链路

严格遵循：前端 → API 网关 → 后端服务 → 持久层 → 数据库/缓存。

| 模块名称 | 归属层级 | 职责描述 | 交互细节 |
| :--- | :--- | :--- | :--- |
| **网关层** | API Gateway (Nginx) | 静态资源托管、反向代理、跨域处理 (CORS) | 前端请求 `/api` 转发至后端 8080 端口；请求 `/` 返回 index.html。 |
| **用户中心** | 后端 Service | 注册、登录、JWT 鉴权、个人档案管理 | 前端发送 JSON，后端校验后返回 JWT Token，Token 存入 Redis 做白名单/黑名单。 |
| **情绪追踪** | 后端 Service | 情绪日记 CRUD、情绪趋势统计 | 读取 MySQL 日记表；写入时触发 AI 情绪分析（异步或同步）。 |
| **AI 咨询核心** | 后端 Service + External | AI 对话管理、Prompt 组装、外部 API 调用 | **禁止前端直接调 LLM**。后端 Service 封装 LangChain4j 调用 DeepSeek/OpenAI，结果经 WebSocket 推送给前端。 |
| **心理评估** | 后端 Service | 问卷分发 (PHQ-9)、自动评分 | 评分逻辑在后端计算，结果存入 MySQL，前端只负责展示题目和结果。 |
| **咨询师预约** | 后端 Service | 咨询师筛选、日程管理、预约锁单 | 涉及事务处理（MySQL），利用 Redis 做预约库存的原子性扣减（防止超卖）。 |

---

### 2. 技术栈确认与选型

#### 2.1 前端层 (Frontend)
*   **开发语言**：TypeScript (增强代码健壮性，减少类型错误)。
*   **框架**：**Vue 3 (Composition API)**。SRS文档提及Vue.js，Vue 3 适合快速构建 SPA。
*   **UI 组件库**：**Element Plus**。提供现成的表单、日历、卡片组件，极大缩短开发时间。
*   **状态管理**：**Pinia**。比 Vuex 更轻量，适合管理用户信息、AI 对话上下文。
*   **网络请求**：**Axios**。配置拦截器统一处理 JWT Header 和 401/403 错误。
*   **图表库**：**ECharts**。用于“7天情绪趋势图”的绘制。

#### 2.2 API 网关 (Gateway)
*   **选型**：**Nginx**。
*   **理由**：SRS架构图中明确标注 Nginx。对于单体/模块化单体应用，Nginx 配置简单，性能极高，足以处理静态资源和 API 路由转发。
*   **配置**：配置 `/api/` 路径反向代理到 Spring Boot 后端，配置 `gzip` 压缩提升加载速度（<2s 性能指标）。

#### 2.3 后端服务 (Backend Service)
*   **语言**：**Java 17+**。
*   **框架**：**Spring Boot 3.x**。生态成熟，配合 SRS 提到的架构，能快速集成 Web、Data 和 Security。
*   **AI 集成**：**LangChain4j**。Java 版的 LangChain，方便在 Spring 环境中统一管理 Prompt 和 LLM 模型切换。
*   **实时通信**：**Spring WebSocket**。用于 AI 咨询的流式输出（打字机效果），满足“AI 响应 <5s”的感官体验。

#### 2.4 持久层 (Persistence)
*   **ORM 框架**：**MyBatis-Plus**。
*   **理由**：相比原生 MyBatis，它内置了 CRUD 方法，无需手写基础 SQL，开发效率高。
*   **规范**：Entity (数据库映射) -> Mapper (接口) -> XML (仅用于复杂联表查询)。

#### 2.5 数据库与缓存
*   **数据库**：**MySQL 8.0**。存储用户、日记、预约等结构化数据。
*   **缓存**：**Redis**。
    *   *用途*：存储用户 Session Token、AI 对话的短期上下文（Context Window）、咨询师的实时可用时间段。
    *   *策略*：Cache-Aside（旁路缓存），读多写少的数据（如咨询师列表）设置 30 分钟过期。

#### 2.6 外部服务对接
*   **LLM API**：DeepSeek 或 Qwen (千问)。通过 HTTP/REST 对接，API Key 存储在后端 `application.yml` 中（严禁暴露在前端）。
*   **OSS**：阿里云 OSS / 腾讯云 COS。用于存储用户头像、咨询师资质证书图片。

---

### 3. 四周开发详细规划（敏捷迭代）

**时间表**：Week 1 (基础架构) -> Week 2 (核心功能) -> Week 3 (高级功能) -> Week 4 (测试与交付)

#### **人员分工代号：**
*   **FE-A (前端1)**：主攻公共组件、用户中心、情绪可视化。
*   **FE-B (前端2)**：主攻 AI 对话界面、测评问卷、预约交互。
*   **BE-A (后端1)**：主攻数据库设计、用户/权限、情绪业务、OSS。
*   **BE-B (后端2)**：主攻 AI 服务集成、算法匹配、WebSocket、预约逻辑。

---

#### **第一周：环境搭建、数据库设计与用户体系**

**目标**：跑通前后端联调，完成注册/登录/个人中心。

*   **共同任务**：
    *   Day 1：确认 API 接口文档（使用 Apifox/Swagger 定义），确定数据库 ER 图。
    *   Day 2：Git 仓库初始化，搭建本地开发环境（JDK, Node, MySQL, Redis）。

*   **后端开发 (BE)**：
    *   **BE-A**：搭建 Spring Boot 脚手架，整合 MyBatis-Plus, Redis, JWT。设计并创建 `users` 表。实现登录/注册 API。
    *   **BE-B**：设计 `counselors` (咨询师) 表和 `assessments` (测评) 表。集成 LangChain4j 并跑通一个简单的 LLM "Hello World" 测试接口。

*   **前端开发 (FE)**：
    *   **FE-A**：搭建 Vue3 + Vite + ElementPlus 项目。封装 Axios 请求类（拦截器）。实现登录、注册页面 UI。
    *   **FE-B**：设计全局 Layout（侧边栏/顶部导航）。开发首页 Dashboard 的静态布局（暂无数据）。

*   **交付物**：可运行的登录系统，Swagger 接口文档。

---

#### **第二周：情绪日记（核心）与 心理测评**

**目标**：完成 SRS 中的“记录情绪日记”和“参与心理健康测评”。

*   **后端开发 (BE)**：
    *   **BE-A**：实现 `mood_logs` 的 CRUD。开发“获取过去7天情绪数据”的聚合统计 SQL。
    *   **BE-B**：实现 `assessments` 接口（获取 PHQ-9 题目，提交答案计算分数）。集成 AI 简单分析接口（非对话，仅对日记文本进行情感打标：积极/消极）。

*   **前端开发 (FE)**：
    *   **FE-A**：开发“情绪日记”编辑页（文本域+标签选择）。集成 ECharts 实现首页“7天情绪趋势图”对接真实数据。
    *   **FE-B**：开发“心理测评”向导式表单页面。实现测评结果的展示页（分数+建议）。

*   **交付物**：用户可以写日记并看到分析标签，可以做题并看到分数。

---

#### **第三周：AI 咨询（难点）与 咨询师预约**

**目标**：完成 SRS 中的“AI 咨询”和“预约咨询师”。

*   **后端开发 (BE)**：
    *   **BE-A**：开发咨询师列表接口（支持按标签筛选）。设计 `appointments` 表，处理预约状态流转（待确认->已预约->已取消）。
    *   **BE-B**：**攻坚** WebSocket 端点，实现 AI 流式对话。在后端维护对话上下文（Context），存储于 Redis。实现简单的基于标签的咨询师匹配逻辑。

*   **前端开发 (FE)**：
    *   **FE-A**：开发咨询师列表页（卡片式展示）。开发“我的预约”管理页面。
    *   **FE-B**：**攻坚** 仿微信/ChatGPT 的聊天界面。实现 WebSocket 客户端连接，处理消息气泡的流式渲染。

*   **交付物**：可以与 AI 实时对话，可以查看并预约咨询师。

---

#### **第四周：系统联调、测试与部署**

**目标**：UI 美化，Bug 修复，性能优化，文档编写。

*   **后端开发 (BE)**：
    *   **BE-A & BE-B**：
        *   配置 Nginx 反向代理。
        *   进行接口压力测试（JMeter），确保 AI 响应不超时。
        *   数据脱敏处理（确保返回给前端的咨询师数据不含敏感字段）。
        *   导出 API 文档和部署脚本。

*   **前端开发 (FE)**：
    *   **FE-A & FE-B**：
        *   全站 UI 走查（统一色调、字体）。
        *   增加 Loading 状态和错误提示（网络异常处理）。
        *   配合后端进行端到端测试（从注册到预约全流程）。

*   **交付物**：最终可演示系统，项目汇报 PPT。

---

### 4. 数据库设计概览（核心表）

1.  **users**: `id`, `username`, `password_hash`, `role` (USER/COUNSELOR/ADMIN), `avatar`.
2.  **mood_logs**: `id`, `user_id`, `content` (日记文本), `tags` (JSON: 焦虑/开心...), `ai_analysis` (AI建议), `score` (0-10), `created_at`.
3.  **assessments**: `id`, `user_id`, `type` (PHQ-9), `score`, `result_text`, `created_at`.
4.  **counselors**: `id`, `user_id`, `real_name`, `specialty` (擅长领域), `bio`, `qualification_img`.
5.  **appointments**: `id`, `user_id`, `counselor_id`, `start_time`, `end_time`, `status`, `notes`.

### 5. 风险控制与应对

*   **风险1：AI API 响应过慢或超时。**
    *   *应对*：前端实现“正在输入中...”的骨架屏动画；后端设置超时熔断，若 AI 挂了，返回“AI 正在休息，请稍后再试”或仅保存日记不分析。
*   **风险2：Websocket 调试困难。**
    *   *应对*：Week 3 若 WebSocket 进度受阻，立即降级为 HTTP 轮询 (Polling) 或简单的 POST 请求（等待全部生成完再返回），优先保证功能可用性。
*   **风险3：前后端接口不一致。**
    *   *应对*：严格要求 Week 1 Day 1 产出 Swagger/Apifox 文档，后端修改接口必须通知前端，禁止私自改字段名。

### 6. 开发环境与工具清单

*   **代码管理**：GitHub / Gitee (Feature Branch Workflow)。
*   **接口管理**：Apifox / Postman (团队协作版)。
*   **IDE**：IntelliJ IDEA (Backend), VS Code (Frontend)。
*   **数据库GUI**：DBeaver / Navicat。

这个方案将任务明确到人和天，同时给出了技术决策的依据，能帮助你的团队在四周内高效完成课程项目。
