# MindEase 前端仓库

同济大学软件工程与管理课程设计 - 智能心理健康支持平台

## 1. 仓库结构

```
front/                                  # 前端根目录
├── html_example/                       # UI原型设计(!!不具备实际功能)
│
├── mindease-frontend/                  # Vue3 实际开发项目
│   ├── src/
│   │   ├── api/                       # API接口封装
│   │   │   ├── request.ts            # Axios封装（已完成）
│   │   │   ├── auth.ts               # 认证接口（已完成）
│   │   │   ├── mood.ts               # 情绪日记接口（已完成）
│   │   │   ├── counselor.ts          # 咨询师审核接口（已完成）
│   │   │   └── user.ts               # 用户中心接口（已完成）
│   │   ├── components/
│   │   │   ├── layout/
│   │   │   │   └── MainLayout.vue    # 主布局（已完成）
│   │   │   └── common/               # 公共组件（已完成）
│   │   ├── views/
│   │   │   ├── auth/                 # 认证模块（通用）
│   │   │   │   ├── LoginView.vue     # 登录页（已完成）
│   │   │   │   └── RegisterView.vue  # 注册页（已完成）
│   │   │   ├── user/                 # 普通用户端
│   │   │   │   ├── HomeView.vue      # 首页（已完成）
│   │   │   │   ├── mood/             # 情绪日记（已完成）
│   │   │   │   ├── chat/             # AI咨询（已完成）
│   │   │   │   ├── assessment/       # 心理测评（已完成）
│   │   │   │   ├── counselor/        # 咨询师推荐（未完成）
│   │   │   │   ├── appointment/      # 预约管理（已完成）
│   │   │   │   ├── profile/          # 个人中心（已完成）
│   │   │   │   └── report/           # 情绪报告（已初步完成）
│   │   │   ├── counselor/            # 咨询师端
│   │   │   │   ├── DashboardView.vue     # 工作台（待开发）
│   │   │   │   └── AuditPendingView.vue  # 审核页面（已完成）
│   │   │   └── admin/                # 管理员端
│   │   │       └── DashboardView.vue     # 管理员工作台（待开发）
│   │   ├── stores/
│   │   │   └── user.ts               # 用户状态管理（已完成）
│   │   ├── router/
│   │   │   └── index.ts              # 路由配置（已完成）
│   │   └── main.ts                   # 入口文件（已完成）
│   ├── public/                        # 静态资源
│   ├── .env.development               # 开发环境配置
│   ├── .env.production                # 生产环境配置
│   ├── package.json                   # 项目依赖
│   ├── vite.config.ts                 # Vite构建配置
│   └── tsconfig.json                  # TypeScript配置
│
├── backend/                            # 后端工程（供前端参考）
├── 1205问题汇总.md                   # 待解决问题列表
├── .gitignore                          # Git忽略规则
└── README.md                           # 本文件
```

## 2. 技术栈

- **框架**: Vue 3 + TypeScript
- **UI 组件库**: Element Plus
- **状态管理**: Pinia
- **路由**: Vue Router
- **HTTP 请求**: Axios
- **图表**: ECharts
- **构建工具**: Vite
- **原型设计**: Tailwind CSS

## 3. 快速开始

### 前置要求

- Node.js 20.19.0+ 或 22.12.0+
- npm 或 pnpm

### 安装依赖

```bash
cd mindease-frontend
npm install
```

### 启动开发服务器

```bash
npm run dev
```

### 访问地址

- **Vue3 开发项目**: http://localhost:5173 (具体端口号详见启动后终端)

### 默认路由

- `/login` - 登录页
- `/register` - 注册页
- `/home` - 首页（需登录）
- `/mood-diary` - 情绪日记（需登录）
- `/ai-chat` - AI 咨询（需登录）
- `/counselor-list` - 咨询师列表（需登录）

## 4. 环境配置

### 开发环境配置(有后端之后)

编辑 `mindease-frontend/.env.development`：

1. 与后端联调：
   mock 开关设置为 false

```env
VITE_API_BASE_URL=http://localhost:8080/api
# Mock开关（true=使用假数据，false=调用真实API）
# 后端未完成时设置为 true，后端完成后改成 false
VITE_USE_MOCK=false
```

注意：当前状态由于前后端进度差异，联调时部分功能可能有各种报错

2. 本地测试(仅测试前端显示)：
   mock 开关设置为 true
   运行前端后，在登录页点击演示模式按钮进入演示模式

```env
VITE_API_BASE_URL=http://localhost:8080/api
# Mock开关（true=使用假数据，false=调用真实API）
# 后端未完成时设置为 true，后端完成后改成 false
VITE_USE_MOCK=true
```

### 生产环境配置(待定)

编辑 `mindease-frontend/.env.production`：

```env
VITE_API_BASE_URL=https://api.mindease.com/api
```

## 5. API 接口清单

初步的 api 规范见根目录下的`api_word.md`
后续等待后端提供的接口文档或 swagger、Apifox 文档

## 6. 待开发的计划(初步规划)

### 优先级 P0（第一周）

- [x] 登录/注册页面
- [x] 主布局框架
- [x] 情绪日记编辑页
- [x] 首页 Dashboard（情绪趋势图）

### 优先级 P1（第二周）

- [x] 心理测评页面
- [x] AI 咨询聊天界面
- [x] 咨询师列表页

### 优先级 P2（第三周）

- [x] 咨询师预约页面
- [x] 我的预约管理
- [x] 情绪报告页面

### 优先级 P3（第四周）

- [x] 个人中心
- [ ] 通知中心
- [ ] UI 优化和测试

## 7. 日志

### 11.28

1. 前端仓库初始化
2. 登录/注册页面
3. HTML 原型设计整理

### 12.02

1. 情绪日记模块 基本完成
2. 添加了一些公共组件详见 src/components/
3. 完善了公共样式详见 src/assets/main.css
4. 由于暂时没有后端，动态 Mock 系统替代静态数据测试

### 12.03

1. 基本完成个人中心(个人中心目前缺少：图片上传接口)
2. 前后端初步联调

### 12.04

1. 完善适配咨询师审核流程：适配后端受限 Token 机制，实现三端分离
2. 重构文件架构：按 user/counselor/admin 三端组织视图文件，但目前 counselor 和 admin 端仅有空白占位页面，后续待开发 如果测试时需要审批操作可以在 apifox 页面纯后端操作，配合测试
3. 修复多个路由问题：角色权限控制、status 字段缺失、菜单显示等
4. 优化个人中心：根据角色动态显示内容

### 12.05

1. 完成用户端心理测评模块
2. 修复头像显示 bug
3. 修复部分 api 字段缺失

### 12.07

1. 初步对接情绪报告模块
2. 对接咨询师预约模块
3. 注：
   情绪报告模块导出有一点小问题
   目前咨询师模块缺乏有效测试数据，且咨询师端未完成，测试不完全
4. 当前用户端待开发：通知中心 ai 咨询

### 12.13
1. 对接后端ai对话模块
2. 目前用户端基本完成

**MindEase Frontend Repository** | 同济大学软件工程课程设计 2025
