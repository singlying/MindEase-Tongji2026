# MindEase API 接口文档

## 在线飞书文档：（定期查看确保实时更新）
https://ucn2vriuswz5.feishu.cn/wiki/MiIXwSfIeiG0eUkHkTdcZNgLn1e

## \!\!初步能想到的 不完善\!\!

## 基础约定

- **Base URL**: `/api`
- **响应格式**: `{code: 200, message: "success", data: {}}`
- **认证方式**: JWT Token (Header: `Authorization: Bearer <token>`)
- **日期格式**: `YYYY-MM-DD HH:mm:ss`

---

## MindEase API

**🟢 接口详细清单**

### 📦 1. 用户认证模块 (Auth)

**状态定义 (status)**：

1.  **启用 (Active)** - 普通用户注册后默认为此状态。
2.  **待审核 (Pending)** - 咨询师注册后默认为此状态，**不可登录**。
3.  **禁用 (Banned)** - 被管理员手动封禁。

**注册逻辑**：

- 若 role 为 user -\> 状态设为 1，直接返回 Token，允许登录。
- 若 role 为 counselor -\> 状态设为 2，**不返回 Token**，提示“等待审核”。

**登录逻辑**：

- 登录时检查 status。如果是 2，报错提示“账号审核中”；如果是 0，提示“账号已禁用”。

#### 1.1 用户注册 (Auth Register)

**变更说明**：注册接口回归纯粹的账号创建。咨询师注册后状态为 2 (待审核)，此时还未提交资料。

- **方法**: `POST`
- **路径**: `/auth/register`

**请求 Body (JSON)**:

codeJSON
Plain Text

```json
{
  "username": "doctor_zhang",
  "password": "Password123!",
  "nickname": "张医生",
  "phone": "13800138000",
  "role": "counselor", // user, counselor, admin
  "invitationCode": "" // admin 注册仍需邀请码
}
```

_(移除了原有的 counselorProfile 对象)_

**响应 (咨询师注册成功)**:

codeJSON
Plain Text

```json
{
  "code": 200,
  "message": "账号注册成功，请登录并提交资质证明进行审核。",
  "data": {
    "userId": 2001,
    "role": "counselor",
    "status": 2, // 待审核/资料未完善
    "token": "eyJ..." // 注意：这里可以返回一个临时 Token（权限仅限于上传资质），或者不返回 Token 要求前端跳转登录页
  }
}
```

_建议：为了体验流畅，可以给咨询师返回一个带有 PRE_AUTH 权限的 Token，仅允许调用“提交资质”和“查询审核状态”接口，不允许接单。_

#### 1.2 用户登录 (Auth Login)

- **名称**: 用户登录
- **方法**: `POST`
- **路径**: `/auth/login`
- **逻辑说明**: 后端需校验密码并检查 status。

**请求 Body (JSON)**:

Plain Text

```json
{
  "username": "doctor_zhang",
  "password": "Password123!"
}
```

**响应 - 登录成功 (status=1)**:

Plain Text

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 1001,
    "username": "user001",
    "role": "user",
    "token": "eyJhbGciOi..."
  }
}
```

**响应 - 登录失败 (status=2 待审核)**:

Plain Text

```json
{
  "code": 403,
  "message": "您的账号正在审核中，请耐心等待或联系管理员。",
  "data": null
}
```

**响应 - 登录失败 (status=0 已禁用)**:

Plain Text

```json
{
  "code": 403,
  "message": "您的账号已被禁用。",
  "data": null
}
```

#### 1.3 获取个人信息

- **名称:** 获取个人信息
- **方法:** `GET`
- **路径:** `/auth/profile`

**响应 (JSON):**

Plain Text

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 1001,
    "username": "user001",
    "nickname": "小明",
    "status": 1,
    "avatar": "https://example.com/avatar.jpg",
    "role": "user",
    "createTime": "2023-10-01 12:00:00"
  }
}
```

#### 1.4 更新个人信息

- **名称:** 更新个人信息
- **方法:** `PUT`
- **路径:** `/auth/profile`

**请求 Body (JSON):**

Plain Text

```json
{
  "nickname": "大明",
  "avatar": "https://example.com/new_avatar.jpg"
}
```

**响应 (JSON):**

Plain Text

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "success": true
  }
}
```

---

### 📦 2. 情绪日记模块 (Mood)

#### 2.1 提交情绪日记

- **名称:** 提交情绪日记
- **方法:** `POST`
- **路径:** `/mood/log`

**请求 Body (JSON):**

Plain Text

```json
{
  "moodType": "Happy",
  "moodScore": 8,
  "content": "今天天气很好，心情不错。",
  "tags": ["天气", "运动"],
  "logDate": "2023-10-27 14:00:00"
}
```

**响应 (JSON):**

Plain Text

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "logId": 5001,
    "aiAnalysis": "检测到积极情绪，建议保持。"
  }
}
```

#### 2.2 获取情绪日记列表

- **名称:** 获取情绪日记列表
- **方法:** `GET`
- **路径:** `/mood/logs`
- **Query 参数:** `limit=10`, `offset=0`

**响应 (JSON):**

Plain Text

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 50,
    "logs": [
      {
        "id": 5001,
        "logDate": "2023-10-27 14:00:00",
        "moodType": "Happy",
        "moodScore": 8,
        "content": "今天天气很好...",
        "emoji": "😄"
      }
    ]
  }
}
```

#### 2.3 获取单条日记详情

- **名称:** 获取单条日记详情
- **方法:** `GET`
- **路径:** `/mood/log/{id}` (注意在 ApiFox 路径参数中添加 id)

**响应 (JSON):**

Plain Text

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 5001,
    "userId": 1001,
    "moodType": "Happy",
    "moodScore": 8,
    "content": "今天天气很好...",
    "tags": ["天气"],
    "aiAnalysis": "积极情绪...",
    "logDate": "2023-10-27 14:00:00",
    "createTime": "2023-10-27 14:05:00"
  }
}
```

#### 2.4 删除日记

- **名称:** 删除日记
- **方法:** `DELETE`
- **路径:** `/mood/log/{id}`

**响应 (JSON):**

Plain Text

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "success": true
  }
}
```

#### 2.5 获取情绪趋势

- **名称:** 获取情绪趋势
- **方法:** `GET`
- **路径:** `/mood/trend`
- **Query 参数:** `days=7`

**响应 (JSON):**

Plain Text

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "dates": ["10-21", "10-22", "10-23"],
    "scores": [6, 7, 5],
    "avgScore": 6.0,
    "positiveRate": 0.66,
    "continuousDays": 3
  }
}
```

#### 2.6 获取情绪统计

- **名称:** 获取情绪统计
- **方法:** `GET`
- **路径:** `/mood/statistics`

**响应 (JSON):**

Plain Text

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "distribution": {
      "happy": "35%",
      "calm": "30%",
      "sad": "10%",
      "anxious": "25%"
    },
    "totalLogs": 150,
    "avgScore": 6.5
  }
}
```

---

### 📦 3. 心理测评模块 (Assessment)

#### 3.1 获取量表列表

- **名称:** 获取量表列表
- **方法:** `GET`
- **路径:** `/assessment/scales`

**响应 (JSON):**

Plain Text

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "scales": [
      {
        "scaleKey": "gad-7",
        "title": "焦虑症筛查量表",
        "description": "用于评估焦虑程度...",
        "status": "active"
      }
    ]
  }
}
```

#### 3.2 获取量表详情

- **名称:** 获取量表题目
- **方法:** `GET`
- **路径:** `/assessment/scale/{scaleKey}`

**响应 (JSON):**

Plain Text

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "scaleKey": "gad-7",
    "title": "GAD-7",
    "description": "请回答以下问题...",
    "questions": [
      {
        "id": 1,
        "text": "感觉紧张、焦虑或急切？",
        "options": [
          { "label": "完全没有", "score": 0 },
          { "label": "有几天", "score": 1 }
        ]
      }
    ]
  }
}
```

#### 3.3 提交测评

- **名称:** 提交测评答案
- **方法:** `POST`
- **路径:** `/assessment/submit`

**请求 Body (JSON):**

Plain Text

```json
{
  "scaleKey": "gad-7",
  "answers": [
    { "questionId": 1, "score": 1 },
    { "questionId": 2, "score": 0 }
  ]
}
```

**响应 (JSON):**

Plain Text

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "recordId": 8001,
    "totalScore": 5,
    "resultLevel": "轻度焦虑",
    "resultDesc": "建议进行适当放松..."
  }
}
```

#### 3.4 获取测评历史列表

- **名称:** 获取测评历史列表
- **方法:** `GET`
- **路径:** `/assessment/records`
- **Query 参数:** `limit=10`

**响应 (JSON):**

Plain Text

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 8001,
        "scaleKey": "gad-7",
        "totalScore": 5,
        "resultLevel": "轻度焦虑",
        "createTime": "2023-10-27 15:00:00"
      }
    ]
  }
}
```

#### 3.5 获取单次测评详情

- **名称:** 获取测评结果详情
- **方法:** `GET`
- **路径:** `/assessment/record/{id}`

**响应 (JSON):**

Plain Text

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 8001,
    "scaleKey": "gad-7",
    "totalScore": 5,
    "resultLevel": "轻度焦虑",
    "resultDesc": "建议放松...",
    "answersDetail": [{ "questionId": 1, "score": 1, "questionText": "..." }],
    "createTime": "2023-10-27 15:00:00"
  }
}
```

---

### 📦 4. AI 咨询模块 (Chat)

#### 4.1 创建会话

- **名称:** 创建 AI 会话
- **方法:** `POST`
- **路径:** `/chat/session`

**请求 Body (JSON):**

Plain Text

```json
{
  "title": "关于失眠的咨询"
}
```

**响应 (JSON):**

Plain Text

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "sessionId": "sess_123456",
    "title": "关于失眠的咨询",
    "createTime": "2023-10-27 16:00:00"
  }
}
```

#### 4.2 获取会话列表

- **名称:** 获取会话列表
- **方法:** `GET`
- **路径:** `/chat/sessions`
- **Query 参数:** `limit=20`

**响应 (JSON):**

Plain Text

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "sessions": [
      {
        "sessionId": "sess_123456",
        "title": "关于失眠的咨询",
        "lastMessage": "你可以尝试喝热牛奶...",
        "createTime": "2023-10-27 16:00:00"
      }
    ]
  }
}
```

#### 4.3 发送消息 (流式)

- **名称:** 发送消息(Stream)
- **方法:** `POST`
- **路径:** `/chat/message`
- **注意:** ApiFox 中请在“高级设置”里开启“返回响应”为 **Event Stream** 模拟流式，或者作为普通 JSON 处理。

**请求 Body (JSON):**

Plain Text

```json
{
  "sessionId": "sess_123456",
  "content": "我最近总是睡不着"
}
```

**响应 (文本流):**

Plain Text

```
data: {"messageId":"msg_1", "sender":"AI", "content":"听起来", "timestamp":...}
data: {"messageId":"msg_1", "sender":"AI", "content":"你很困扰", "timestamp":...}
```

#### 4.4 获取历史记录

- **名称:** 获取历史记录
- **方法:** `GET`
- **路径:** `/chat/history/{sessionId}`
- **Query 参数:** `limit=50`

**响应 (JSON):**

Plain Text

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "sessionId": "sess_123456",
    "messages": [
      {
        "sender": "user",
        "content": "睡不着",
        "timestamp": "2023-10-27 16:01:00"
      },
      {
        "sender": "AI",
        "content": "试着放松...",
        "timestamp": "2023-10-27 16:01:05"
      }
    ]
  }
}
```

#### 4.5 删除会话

- **名称:** 删除会话
- **方法:** `DELETE`
- **路径:** `/chat/session/{sessionId}`

**响应 (JSON):**

Plain Text

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "success": true
  }
}
```

---

### 📦 5. 咨询师推荐模块 (Counselor)

#### 5.1 智能推荐咨询师

- **名称:** 智能推荐咨询师
- **方法:** `GET`
- **路径:** `/counselor/recommend`
- **Query 参数:** `specialty=anxiety`, `location=Beijing`, `sortBy=rating`

**响应 (JSON):**

Plain Text

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "aiRecommendReason": "根据您的焦虑测评，为您推荐擅长 CBT 疗法的专家。",
    "counselors": [
      {
        "id": 2001,
        "realName": "张医生",
        "avatar": "http://...",
        "title": "资深心理咨询师",
        "experience": "10 年",
        "specialty": ["焦虑", "失眠"],
        "rating": 4.9,
        "availability": "今天有空",
        "pricePerHour": 500.0
      }
    ]
  }
}
```

#### 5.2 获取咨询师详情

- **名称:** 获取咨询师详情
- **方法:** `GET`
- **路径:** `/counselor/{id}`

**响应 (JSON):**

Plain Text

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 2001,
    "realName": "张医生",
    "avatar": "http://...",
    "title": "资深心理咨询师",
    "specialty": ["焦虑", "失眠"],
    "bio": "毕业于北大心理系...",
    "qualificationUrl": "http://cert.jpg",
    "rating": 4.9,
    "reviewCount": 120,
    "pricePerHour": 500.0,
    "location": "北京朝阳"
  }
}
```

#### 5.3 获取咨询师评价

- **名称:** 获取咨询师评价
- **方法:** `GET`
- **路径:** `/counselor/reviews/{counselorId}`
- **Query 参数:** `limit=10`

**响应 (JSON):**

Plain Text

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "reviews": [
      {
        "userId": 1002,
        "username": "u***",
        "rating": 5,
        "content": "非常专业，很有帮助。",
        "createTime": "2023-10-20"
      }
    ]
  }
}
```

#### 5.4 提交评价

- **名称:** 提交评价
- **方法:** `POST`
- **路径:** `/counselor/review`

**请求 Body (JSON):**

Plain Text

```json
{
  "counselorId": 2001,
  "appointmentId": 3001,
  "rating": 5,
  "content": "很有耐心。"
}
```

**响应 (JSON):**

Plain Text

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "reviewId": 6001
  }
}
```

---

### 📦 6. 预约管理模块 (Appointment)

#### 6.1 查询可用时段

- **名称:** 查询可用时段
- **方法:** `GET`
- **路径:** `/appointment/available-slots`
- **Query 参数:** `counselorId=2001`, `date=2023-11-01`

**响应 (JSON):**

Plain Text

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "date": "2023-11-01",
    "slots": [
      { "startTime": "09:00", "endTime": "10:00", "available": true },
      { "startTime": "10:00", "endTime": "11:00", "available": false }
    ]
  }
}
```

#### 6.2 创建预约

- **名称:** 创建预约
- **方法:** `POST`
- **路径:** `/appointment/create`

**请求 Body (JSON):**

Plain Text

```json
{
  "counselorId": 2001,
  "startTime": "2023-11-01 09:00:00",
  "endTime": "2023-11-01 10:00:00",
  "userNote": "希望能聊聊工作压力"
}
```

**响应 (JSON):**

Plain Text

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "appointmentId": 3001,
    "status": "PENDING"
  }
}
```

#### 6.3 获取我的预约

- **名称:** 获取我的预约
- **方法:** `GET`
- **路径:** `/appointment/my-appointments`
- **Query 参数:** `status=PENDING`, `limit=10`

**响应 (JSON):**

Plain Text

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "appointments": [
      {
        "id": 3001,
        "counselorId": 2001,
        "counselorName": "张医生",
        "counselorAvatar": "http://...",
        "startTime": "2023-11-01 09:00:00",
        "endTime": "2023-11-01 10:00:00",
        "status": "PENDING",
        "userNote": "工作压力",
        "createTime": "2023-10-27"
      }
    ]
  }
}
```

#### 6.4 获取预约详情

- **名称:** 获取预约详情
- **方法:** `GET`
- **路径:** `/appointment/{id}`

**响应 (JSON):**

Plain Text

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 3001,
    "counselorId": 2001,
    "counselorName": "张医生",
    "startTime": "2023-11-01 09:00:00",
    "endTime": "2023-11-01 10:00:00",
    "status": "PENDING",
    "userNote": "工作压力",
    "cancelReason": null
  }
}
```

#### 6.5 取消预约

- **名称:** 取消预约
- **方法:** `PUT`
- **路径:** `/appointment/{id}/cancel`

**请求 Body (JSON):**

Plain Text

```json
{
  "cancelReason": "临时有事"
}
```

**响应 (JSON):**

Plain Text

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "success": true
  }
}
```

#### 6.6 确认预约 (咨询师)

- **名称:** 确认预约(咨询师)
- **方法:** `PUT`
- **路径:** `/appointment/{id}/confirm`

**响应 (JSON):**

Plain Text

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "success": true
  }
}
```

---

### 📦 7. 情绪报告模块 (Report)

#### 7.1 生成情绪报告

- **名称:** 生成情绪报告
- **方法:** `GET`
- **路径:** `/report/emotion`
- **Query 参数:** `startDate=2023-10-01`, `endDate=2023-10-31`

**响应 (JSON):**

Plain Text

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "period": "2023-10",
    "avgScore": 7.2,
    "positiveRate": 0.7,
    "continuousDays": 5,
    "trendData": {
      "dates": ["10-01", "10-02"],
      "scores": [6, 8]
    },
    "distribution": { "happy": "40%", "neutral": "60%" },
    "recentLogs": [
      {
        "date": "10-30",
        "moodType": "Happy",
        "score": 8,
        "content": "Nice day"
      }
    ],
    "aiSuggestions": ["建议保持运动", "睡前冥想"]
  }
}
```

#### 7.2 导出报告 PDF

- **名称:** 导出报告 PDF
- **方法:** `GET`
- **路径:** `/report/export`
- **Query 参数:** `format=pdf`
- **注意:** ApiFox 响应类型需设置为 Blob/File 才能预览下载。

**响应:** (Binary File Stream)

---

### 📦 8. 用户个人中心 (User)

#### 8.1 获取 Dashboard 数据

- **名称:** 用户 Dashboard
- **方法:** `GET`
- **路径:** `/usery/dashboard`

**响应 (JSON):**

Plain Text

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "moodSummary": {
      "avgScore": 7.5,
      "continuousDays": 12
    },
    "upcomingAppointments": [
      { "id": 3005, "time": "明天 10:00", "counselor": "李医生" }
    ],
    "unreadNotifications": 3
  }
}
```

#### 8.2 获取通知列表

- **名称:** 获取通知列表
- **方法:** `GET`
- **路径:** `/user/notifications`
- **Query 参数:** `limit=20`

**响应 (JSON):**

Plain Text

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "notifications": [
      {
        "id": 9001,
        "type": "system",
        "title": "预约提醒",
        "content": "您的预约将在 1 小时后开始",
        "isRead": false,
        "createTime": "2023-10-27 09:00:00"
      }
    ]
  }
}
```

#### 8.3 标记通知已读

- **名称:** 标记通知已读
- **方法:** `PUT`
- **路径:** `/user/notification/{id}/read`

**响应 (JSON):**

Plain Text

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "success": true
  }
}
```

---

### 🔌 WebSocket 接口

#### WS.1 AI 实时对话

在 ApiFox 中新建“WebSocket 接口”

- **URL:** `ws://127.0.0.1:8080/ws/chat`
- **握手 Params (Query):** `token=eyJ...` (JWT Token 通常放在 URL 参数中进行 WS 握手)

**发送消息示例 (Message):**

Plain Text

```json
{
  "type": "message",
  "sessionId": "sess_001",
  "content": "你好，AI"
}
```

**接收消息示例 (Message):**

Plain Text

```json
{
  "type": "ai_response",
  "content": "你好！有什么我可以帮你的吗？",
  "isComplete": false
}
```

---

### 📦 9. 咨询师资质审核模块 (Counselor Audit - NEW)

#### 9.1 提交/更新资质审核申请 (User Side) - [新增]

- **名称**: 提交资质审核
- **权限**: 登录用户 (Role=counselor)
- **方法**: `POST`
- **路径**: `/counselor/audit/submit`

**逻辑说明**:

- 插入数据到 `counselor_audit_record` 表，状态为 `PENDING`。
- 如果是被驳回后重新提交，生成一条新的记录（保留历史驳回记录）。

**请求 Body (JSON)**:

Plain Text

```json
{
  "realName": "张伟",
  "qualificationUrl": "https://oss.com/license_2024.jpg",
  "idCardUrl": "https://oss.com/idcard_back.jpg" // 可选
}
```

**响应**:

codeJSON
Plain Text

```json
{
  "code": 200,
  "message": "资质提交成功，请等待管理员审核",
  "data": {
    "auditId": 501 // 返回审核记录 ID
  }
}
```

#### 9.2 获取审核状态 (User Side) - [新增]

- **名称**: 获取我的审核状态
- **方法**: `GET`
- **路径**: `/counselor/audit/status`

**响应**:

Plain Text

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "latestStatus": "REJECTED", // PENDING, APPROVED, REJECTED
    "auditRemark": "图片模糊，请重新上传", // 驳回原因
    "submitTime": "2023-11-01 10:00:00"
  }
}
```

---

### 📦 10. 管理员审核接口 (Admin Audit - Modified)

#### 10.1 获取待审核列表 - [修改]

- **方法**: `GET`
- **路径**: `/admin/audit/list`
- **逻辑**: 查询 `counselor_audit_record` 表中 `status = 'PENDING'` 的记录。

**响应**:

Plain Text

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "auditId": 501,
        "userId": 2001,
        "username": "doctor_zhang",
        "realName": "张伟",
        "qualificationUrl": "https://oss.com/license.jpg",
        "submitTime": "2023-11-01 10:00:00"
      }
    ]
  }
}
```

#### 10.2 审核操作 (通过/拒绝) - [修改]

- **方法**: `POST`
- **路径**: `/admin/audit/process`

**逻辑说明**:

1.  **校验**: 获取当前登录的管理员 ID (从 Token 中解析)。
2.  **更新审核表**: 更新 `counselor_audit_record`，设置 `status`，`audit_remark`，`audit_time`，以及最重要的 **auditor_id** (记录是谁审的)。
3.  **联动更新**:
    - 如果 **PASS**:
      - 更新 `sys_user` 的 status 为 1 (正常)。
      - 更新/插入 `counselor_profile`，将审核表中的 `real_name` 和 `qualification_url` 同步过去。
    - 如果 **REJECT**:
      - 更新 `sys_user` 的 status 保持 2 或其他驳回状态。
      - 可以通过系统通知 (sys_notification) 发送驳回原因给用户。

**请求 Body**:

Plain Text

```json
{
  "auditId": 501, // 审核记录 ID
  "action": "PASS", // PASS 或 REJECT
  "remark": "资质合规" // 如果是 REJECT，此项必填
}
```

**响应**:

codeJSON
Plain Text

```json
{
  "code": 200,
  "message": "审核完成",
  "data": { "success": true }
}
```
