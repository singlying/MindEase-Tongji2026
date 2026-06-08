# 🔗 前后端 API 对接完全指南

> 从后端文档到前端代码的完整流程

---

## 📚 目录

1. [前后端是怎么连接的？](#1-前后端是怎么连接的)
2. [API 文档转前端代码](#2-api文档转前端代码)
3. [后端未完成时的 Mock 方案](#3-后端未完成时的mock方案)
4. [完整实战案例](#4-完整实战案例)

---

## 1. 前后端是怎么连接的？

### 🔄 通信流程

```
【前端】                    【网络】                    【后端】
Vue页面                                              Spring Boot
  ↓
调用API函数
(在 src/api/ 里)
  ↓
axios发送HTTP请求  ────────────────→  接收请求
(自动加上Token)                      ↓
                                   处理业务逻辑
                                   查询数据库
                                   ↓
接收返回数据       ←────────────────  返回JSON
  ↓
处理数据
  ↓
更新页面显示
```

---

### 🔧 连接的关键点

#### **关键 1：Base URL（服务器地址）**

**配置位置：** `mindease-frontend/.env.development`

```env
# 开发环境
VITE_API_BASE_URL=http://localhost:8080/api

# 如果后端在其他机器上
# VITE_API_BASE_URL=http://192.168.1.100:8080/api
```

**配置位置 2：** `src/api/request.ts`（已经配置好了）

```typescript
const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "/api",
  //     ↑ 读取环境变量，默认 /api
  timeout: 15000,
});
```

---

#### **关键 2：Token 认证（登录凭证）**

**流程：**

```
1. 用户登录成功
   ↓
2. 后端返回Token（一串密钥）
   ↓
3. 前端保存Token到localStorage
   ↓
4. 之后每次请求，axios自动在Header里加上Token
   ↓
5. 后端验证Token，确认是哪个用户
```

**代码位置：** `src/api/request.ts`（已配置）

```typescript
// 请求拦截器：每次请求自动加Token
service.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
    //                                    ↑ 后端要求的格式
  }
  return config;
});
```

---

#### **关键 3：统一响应格式**

**后端返回格式：** （根据 api_word.md）

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

**前端处理：** `src/api/request.ts`（已配置）

```typescript
// 响应拦截器：统一处理返回数据
service.interceptors.response.use(
  (response) => {
    const res = response.data;

    // code不是200，说明出错了
    if (res.code !== 200) {
      ElMessage.error(res.message || "请求失败");
      return Promise.reject(new Error(res.message));
    }

    return response; // 返回完整响应
  },
  (error) => {
    // 网络错误、401未授权等
    ElMessage.error(error.message || "网络错误");
    return Promise.reject(error);
  }
);
```

---

## 2. API 文档转前端代码

### 📖 转换步骤（5 步法）

#### **第 1 步：找到对应的 API 文件**

按模块分类：

```
src/api/
├── auth.ts         ← 用户认证（登录、注册）
├── mood.ts         ← 情绪日记
├── chat.ts         ← AI聊天
├── assessment.ts   ← 心理测评
├── counselor.ts    ← 咨询师
├── appointment.ts  ← 预约管理
├── report.ts       ← 情绪报告
└── user.ts         ← 用户信息
```

---

#### **第 2 步：看懂 API 文档**

**示例：** `api_word.md` 中的"提交情绪日记"

```
方法: POST
路径: /mood/log

请求Body:
{
  "moodType": "Happy",
  "moodScore": 8,
  "content": "今天天气很好...",
  "tags": ["天气", "运动"],
  "logDate": "2023-10-27 14:00:00"
}

响应:
{
  "code": 200,
  "message": "success",
  "data": {
    "logId": 5001,
    "aiAnalysis": "检测到积极情绪..."
  }
}
```

---

#### **第 3 步：定义 TypeScript 类型**

**文件：** `src/api/mood.ts`

```typescript
// 1. 定义请求参数的类型
export interface CreateMoodParams {
  moodType: string; // 情绪类型
  moodScore: number; // 情绪分数 1-10
  content: string; // 日记内容
  tags?: string[]; // 标签（可选）
  logDate: string; // 日期
}

// 2. 定义响应数据的类型
export interface MoodLogData {
  logId: number; // 日记ID
  aiAnalysis: string; // AI分析
}
```

---

#### **第 4 步：编写 API 函数**

**文件：** `src/api/mood.ts`

```typescript
import request from "./request";
import type { ApiResponse } from "./request";

// 提交情绪日记
export function createMoodLog(data: CreateMoodParams) {
  return request.post<ApiResponse<MoodLogData>>("/mood/log", data);
  //            ↑ 请求方法   ↑ 路径      ↑ 发送的数据
}
```

**完整写法对照：**

| API 文档                  | 前端代码                                               |
| ------------------------- | ------------------------------------------------------ |
| `POST /mood/log`          | `request.post('/mood/log', data)`                      |
| `GET /mood/logs?limit=10` | `request.get('/mood/logs', { params: { limit: 10 } })` |
| `GET /mood/log/{id}`      | `request.get(`/mood/log/${id}`)`                       |
| `PUT /mood/log/{id}`      | `request.put(`/mood/log/${id}`, data)`                 |
| `DELETE /mood/log/{id}`   | `request.delete(`/mood/log/${id}`)`                    |

---

#### **第 5 步：在页面中调用**

**文件：** `src/views/mood/MoodDiaryView.vue`

```vue
<script setup lang="ts">
import { createMoodLog, type CreateMoodParams } from "@/api/mood";
import { ElMessage } from "element-plus";

// 提交表单
const handleSubmit = async () => {
  const formData: CreateMoodParams = {
    moodType: "Happy",
    moodScore: 8,
    content: "今天心情不错",
    tags: ["天气好"],
    logDate: "2024-12-01 14:00:00",
  };

  try {
    // 调用API
    const res = await createMoodLog(formData);

    // 成功
    ElMessage.success("提交成功");
    console.log("返回数据:", res.data.data);
    console.log("AI分析:", res.data.data.aiAnalysis);
  } catch (error) {
    // 失败（request.ts已经自动显示错误提示）
    console.error("提交失败", error);
  }
};
</script>
```

---

### 🎯 完整示例：情绪日记 API 文件

**文件：** `src/api/mood.ts`

```typescript
import request from "./request";
import type { ApiResponse } from "./request";

// ==================== 类型定义 ====================

// 提交情绪日记参数
export interface CreateMoodParams {
  moodType: string;
  moodScore: number;
  content: string;
  tags?: string[];
  logDate: string;
}

// 情绪日记项
export interface MoodLogItem {
  id: number;
  logDate: string;
  moodType: string;
  moodScore: number;
  content: string;
  emoji: string;
}

// 情绪日记列表响应
export interface MoodLogsResponse {
  total: number;
  logs: MoodLogItem[];
}

// 提交响应
export interface CreateMoodResponse {
  logId: number;
  aiAnalysis: string;
}

// ==================== API函数 ====================

/**
 * 提交情绪日记
 * POST /mood/log
 */
export function createMoodLog(data: CreateMoodParams) {
  return request.post<ApiResponse<CreateMoodResponse>>("/mood/log", data);
}

/**
 * 获取情绪日记列表
 * GET /mood/logs?limit=10&offset=0
 */
export function getMoodLogs(limit = 10, offset = 0) {
  return request.get<ApiResponse<MoodLogsResponse>>("/mood/logs", {
    params: { limit, offset },
  });
}

/**
 * 获取单条日记详情
 * GET /mood/log/{id}
 */
export function getMoodLogDetail(id: number) {
  return request.get<ApiResponse<MoodLogItem>>(`/mood/log/${id}`);
}

/**
 * 更新情绪日记
 * PUT /mood/log/{id}
 */
export function updateMoodLog(id: number, data: CreateMoodParams) {
  return request.put<ApiResponse<any>>(`/mood/log/${id}`, data);
}

/**
 * 删除情绪日记
 * DELETE /mood/log/{id}
 */
export function deleteMoodLog(id: number) {
  return request.delete<ApiResponse<any>>(`/mood/log/${id}`);
}
```

---

## 3. 后端未完成时的 Mock 方案

### 🎭 什么是 Mock？

**Mock = 模拟数据**

- 后端还没写好接口时，前端自己造假数据
- 前端照常开发，不用等后端
- 后端完成后，只需**改一行配置**，立即切换到真实 API

---

### 🛠️ Mock 方案 1：简单版（推荐新手）

**直接在 API 函数里返回假数据**

**文件：** `src/api/mood.ts`

```typescript
// 提交情绪日记（Mock版本）
export function createMoodLog(data: CreateMoodParams) {
  // ========== Mock数据 ==========
  const MOCK_ENABLED = true; // ← 开关：true=用假数据，false=调真接口

  if (MOCK_ENABLED) {
    // 模拟延迟（让它看起来像真的在请求）
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          data: {
            code: 200,
            message: "success",
            data: {
              logId: Math.floor(Math.random() * 10000), // 随机ID
              aiAnalysis: "检测到积极情绪，建议保持。",
            },
          },
        });
      }, 500); // 延迟500毫秒
    });
  }
  // ========== Mock数据结束 ==========

  // 真实请求（后端完成后启用）
  return request.post<ApiResponse<CreateMoodResponse>>("/mood/log", data);
}
```

**优点：**

- ✅ 简单，一看就懂
- ✅ 不需要安装额外工具

**缺点：**

- ❌ 每个函数都要写 Mock 数据
- ❌ 切换真/假数据要改多个地方

---

### 🛠️ Mock 方案 2：集中管理版（推荐团队）

**步骤 1：创建 Mock 数据文件**

**文件：** `src/mock/mood.ts`

```typescript
// 情绪日记的Mock数据
export const mockMoodData = {
  // 提交情绪日记的Mock响应
  createMoodLog: {
    code: 200,
    message: "success",
    data: {
      logId: 5001,
      aiAnalysis: "检测到积极情绪，建议保持。",
    },
  },

  // 获取列表的Mock响应
  getMoodLogs: {
    code: 200,
    message: "success",
    data: {
      total: 50,
      logs: [
        {
          id: 5001,
          logDate: "2024-11-27 14:00:00",
          moodType: "Happy",
          moodScore: 8,
          content: "今天天气很好，心情不错。",
          emoji: "😄",
        },
        {
          id: 5002,
          logDate: "2024-11-26 10:30:00",
          moodType: "Calm",
          moodScore: 7,
          content: "工作顺利完成，感觉平静。",
          emoji: "😌",
        },
        {
          id: 5003,
          logDate: "2024-11-25 16:00:00",
          moodType: "Anxious",
          moodScore: 4,
          content: "明天有个重要会议，有点焦虑。",
          emoji: "😰",
        },
      ],
    },
  },
};
```

---

**步骤 2：创建 Mock 开关配置**

**文件：** `.env.development`

```env
# 后端API地址
VITE_API_BASE_URL=http://localhost:8080/api

# Mock开关（true=用假数据，false=用真接口）
VITE_USE_MOCK=true
```

---

**步骤 3：在 API 函数中使用**

**文件：** `src/api/mood.ts`

```typescript
import { mockMoodData } from "@/mock/mood";

// 读取Mock开关
const USE_MOCK = import.meta.env.VITE_USE_MOCK === "true";

// 提交情绪日记
export function createMoodLog(data: CreateMoodParams) {
  // Mock数据
  if (USE_MOCK) {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({ data: mockMoodData.createMoodLog });
      }, 500);
    });
  }

  // 真实请求
  return request.post<ApiResponse<CreateMoodResponse>>("/mood/log", data);
}

// 获取列表
export function getMoodLogs(limit = 10, offset = 0) {
  // Mock数据
  if (USE_MOCK) {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({ data: mockMoodData.getMoodLogs });
      }, 300);
    });
  }

  // 真实请求
  return request.get<ApiResponse<MoodLogsResponse>>("/mood/logs", {
    params: { limit, offset },
  });
}
```

---

**步骤 4：切换到真实 API**

后端完成后，**只需要改一行：**

**文件：** `.env.development`

```env
# 改成 false，就自动切换到真接口了！
VITE_USE_MOCK=false
```

---

### 🛠️ Mock 方案 3：专业版（使用 Mock 工具）

**推荐工具：** `vite-plugin-mock`

**步骤 1：安装**

```bash
npm install vite-plugin-mock -D
```

**步骤 2：配置**

**文件：** `vite.config.ts`

```typescript
import { viteMockServe } from "vite-plugin-mock";

export default defineConfig({
  plugins: [
    vue(),
    viteMockServe({
      mockPath: "mock", // Mock文件夹
      enable: true, // 启用Mock
    }),
  ],
});
```

**步骤 3：创建 Mock 接口**

**文件：** `mock/mood.ts`

```typescript
import { MockMethod } from "vite-plugin-mock";

export default [
  // POST /api/mood/log
  {
    url: "/api/mood/log",
    method: "post",
    response: () => {
      return {
        code: 200,
        message: "success",
        data: {
          logId: 5001,
          aiAnalysis: "检测到积极情绪，建议保持。",
        },
      };
    },
  },

  // GET /api/mood/logs
  {
    url: "/api/mood/logs",
    method: "get",
    response: () => {
      return {
        code: 200,
        message: "success",
        data: {
          total: 50,
          logs: [
            {
              id: 5001,
              logDate: "2024-11-27 14:00:00",
              moodType: "Happy",
              moodScore: 8,
              content: "今天天气很好...",
              emoji: "😄",
            },
          ],
        },
      };
    },
  },
] as MockMethod[];
```

**优点：**

- ✅ 完全模拟真实 API
- ✅ 支持拦截请求、延迟响应
- ✅ 不需要改 API 函数代码

---

## 4. 完整实战案例

### 🎯 需求：开发"情绪日记列表页"

---

### 📋 开发流程

#### **第 1 步：查看后端 API 文档**

**文档位置：** `api_word.md` → "2.2 获取情绪日记列表"

```
方法: GET
路径: /mood/logs
参数: limit=10, offset=0

响应:
{
  "code": 200,
  "data": {
    "total": 50,
    "logs": [...]
  }
}
```

---

#### **第 2 步：创建 Mock 数据**

**文件：** `src/mock/mood.ts`

```typescript
export const mockMoodData = {
  getMoodLogs: {
    code: 200,
    message: "success",
    data: {
      total: 3,
      logs: [
        {
          id: 5001,
          logDate: "2024-11-27 14:00:00",
          moodType: "Happy",
          moodScore: 8,
          content:
            "今天天气很好，心情不错。完成了所有工作任务，还去公园散了步。",
          emoji: "😄",
        },
        {
          id: 5002,
          logDate: "2024-11-26 10:30:00",
          moodType: "Calm",
          moodScore: 7,
          content: "工作顺利完成，感觉平静。",
          emoji: "😌",
        },
        {
          id: 5003,
          logDate: "2024-11-25 16:00:00",
          moodType: "Anxious",
          moodScore: 4,
          content: "明天有个重要会议，有点焦虑。",
          emoji: "😰",
        },
      ],
    },
  },
};
```

---

#### **第 3 步：编写 API 函数**

**文件：** `src/api/mood.ts`

```typescript
import request from "./request";
import type { ApiResponse } from "./request";
import { mockMoodData } from "@/mock/mood";

const USE_MOCK = import.meta.env.VITE_USE_MOCK === "true";

// 类型定义
export interface MoodLogItem {
  id: number;
  logDate: string;
  moodType: string;
  moodScore: number;
  content: string;
  emoji: string;
}

export interface MoodLogsResponse {
  total: number;
  logs: MoodLogItem[];
}

// 获取情绪日记列表
export function getMoodLogs(limit = 10, offset = 0) {
  // Mock数据
  if (USE_MOCK) {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({ data: mockMoodData.getMoodLogs });
      }, 300);
    });
  }

  // 真实请求
  return request.get<ApiResponse<MoodLogsResponse>>("/mood/logs", {
    params: { limit, offset },
  });
}
```

---

#### **第 4 步：开发页面**

**文件：** `src/views/mood/MoodDiaryView.vue`

```vue
<template>
  <div class="mood-diary-page">
    <h1>情绪日记</h1>

    <!-- 加载中 -->
    <div v-if="loading" class="loading">加载中...</div>

    <!-- 列表 -->
    <div v-else class="mood-list">
      <div v-for="item in moodList" :key="item.id" class="mood-item">
        <div class="mood-emoji">{{ item.emoji }}</div>
        <div class="mood-info">
          <div class="mood-header">
            <span class="mood-type">{{ item.moodType }}</span>
            <span class="mood-score">{{ item.moodScore }}/10</span>
          </div>
          <div class="mood-content">{{ item.content }}</div>
          <div class="mood-date">{{ item.logDate }}</div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="!loading && moodList.length === 0" class="empty">暂无日记</div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { getMoodLogs, type MoodLogItem } from "@/api/mood";
import { ElMessage } from "element-plus";

// 数据
const loading = ref(false);
const moodList = ref<MoodLogItem[]>([]);
const total = ref(0);

// 获取列表
const fetchMoodLogs = async () => {
  loading.value = true;

  try {
    const res = await getMoodLogs(10, 0);

    // 保存数据
    moodList.value = res.data.data.logs;
    total.value = res.data.data.total;

    console.log("获取到", total.value, "条日记");
  } catch (error) {
    ElMessage.error("获取失败");
    console.error(error);
  } finally {
    loading.value = false;
  }
};

// 页面加载时执行
onMounted(() => {
  fetchMoodLogs();
});
</script>

<style scoped>
.mood-diary-page {
  padding: 20px;
}

.mood-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.mood-item {
  display: flex;
  gap: 16px;
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.mood-emoji {
  font-size: 48px;
}

.mood-info {
  flex: 1;
}

.mood-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.mood-type {
  font-weight: bold;
  color: #7b9e89;
}

.mood-score {
  color: #666;
}

.mood-content {
  color: #333;
  margin-bottom: 8px;
}

.mood-date {
  font-size: 12px;
  color: #999;
}

.loading,
.empty {
  text-align: center;
  padding: 40px;
  color: #999;
}
</style>
```

---

#### **第 5 步：配置 Mock 开关**

**文件：** `.env.development`

```env
# 使用Mock数据（后端未完成时）
VITE_USE_MOCK=true
```

---

#### **第 6 步：运行测试**

```bash
npm run dev
```

访问 http://localhost:5173/mood-diary

**看到效果：**

- ✅ 显示 3 条 Mock 数据
- ✅ 每条都有 emoji、内容、日期
- ✅ 点击没反应（因为是假数据）

---

#### **第 7 步：后端完成后切换真接口**

**只需要改一行！**

**文件：** `.env.development`

```env
# 改成false
VITE_USE_MOCK=false

# 确保后端地址正确
VITE_API_BASE_URL=http://localhost:8080/api
```

**刷新页面，自动切换到真实数据！** ✅

---

## 📝 总结：预留连接逻辑的位置

### 🎯 问题 2 答案：后端未完成，前端怎么办？

#### **第 1 步：在 `.env.development` 预留配置**

```env
# API地址（后端告诉你）
VITE_API_BASE_URL=http://localhost:8080/api

# Mock开关（开发时用假数据）
VITE_USE_MOCK=true
```

---

#### **第 2 步：在 `src/api/` 预留接口函数**

**按模块创建文件：**

```
src/api/
├── mood.ts          ← 已有部分
├── chat.ts          ← 新建，先写Mock
├── assessment.ts    ← 新建，先写Mock
├── counselor.ts     ← 新建，先写Mock
├── appointment.ts   ← 新建，先写Mock
```

**每个文件的模板：**

```typescript
import request from './request'
import type { ApiResponse } from './request'

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

// 类型定义（根据api_word.md）
export interface XXXParams { ... }
export interface XXXResponse { ... }

// API函数
export function createXXX(data: XXXParams) {
  // Mock数据（先这样写）
  if (USE_MOCK) {
    return Promise.resolve({
      data: {
        code: 200,
        data: { /* 假数据 */ }
      }
    })
  }

  // 真实请求（后端完成后启用）
  return request.post<ApiResponse<XXXResponse>>('/xxx/path', data)
}
```

---

#### **第 3 步：在 `src/mock/` 预留假数据**

```
src/mock/
├── mood.ts          ← 情绪日记假数据
├── chat.ts          ← AI聊天假数据
├── assessment.ts    ← 测评假数据
└── ...
```

---

#### **第 4 步：页面照常开发**

**不用等后端！** 直接用 Mock 数据开发 UI 和交互。

---

#### **第 5 步：后端完成后，一键切换**

```env
VITE_USE_MOCK=false  # 改这一行！
```

---

## ⚠️ 注意事项

### 1. **保持 API 路径和后端一致**

```typescript
// ❌ 错误：路径不对
request.get("/moodLog/list");

// ✅ 正确：和api_word.md一致
request.get("/mood/logs");
```

### 2. **参数名要一致**

```typescript
// 后端要求的参数名是 moodScore
// ❌ 错误
{
  score: 8;
}

// ✅ 正确
{
  moodScore: 8;
}
```

### 3. **TypeScript 类型要对应**

```typescript
// 后端返回 logId 是数字
// ❌ 错误
logId: string;

// ✅ 正确
logId: number;
```

### 4. **Token 认证已自动处理**

**不需要手动加 Token！** `request.ts`已经配置好了拦截器。

---

## 🎯 快速检查清单

开发新模块时，按这个清单检查：

- [ ] 看了 `api_word.md` 文档
- [ ] 在 `src/api/` 创建了对应文件
- [ ] 定义了 TypeScript 类型
- [ ] 编写了 API 函数
- [ ] 在 `src/mock/` 创建了假数据
- [ ] API 函数中加了 Mock 开关
- [ ] `.env.development` 设置 `VITE_USE_MOCK=true`
- [ ] 页面中成功调用并显示数据
- [ ] 后端完成后改成 `VITE_USE_MOCK=false` 测试

---

**记住核心：前后端通过 HTTP + JSON 连接，中间层是 `src/api/` 文件夹！** 🚀
