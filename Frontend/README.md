# MindEase 前端仓库

同济大学软件工程管理与经济课程设计 - 智能心理健康支持平台

## 1. 仓库结构

```
front/                                  # 前端根目录
├── html_example/                       # UI原型设计
│
├── mindease-frontend/                  # Vue3 实际开发项目
│   ├── src/
│   ├── public/
│   ├── .env.development               # 开发环境配置
│   ├── .env.production                # 生产环境配置
│   ├── package.json                   # 项目依赖
│   ├── vite.config.ts                 # Vite构建配置
│   └── tsconfig.json                  # TypeScript配置
│
├── .gitignore
└── README.md                           # 本文件

```

## 2. 技术栈(暂定)

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
