# 🟢 接口详细清单

## 📦 1. 用户认证模块 (Auth)

1. 状态定义 (status)：

- 1: 启用 (Active) - 普通用户注册后默认为此状态。
- 2: 待审核 (Pending) - 咨询师注册后默认为此状态，不可登录。
- 0: 禁用 (Banned) - 被管理员手动封禁。

2. 注册角色与逻辑：

- 若 role 为 user -> 状态设为 1，直接返回 Token，允许登录。
- 若 role 为 counselor -> 状态设为 2，不返回 Token，提示“等待审核”。
- role 字段只允许三种角色， user(普通), counselor(咨询师), admin(管理员)。

3. 登录逻辑：

- 登录时检查 status。如果是 2，报错提示“账号审核中”；如果是 0，提示“账号已禁用”。

### 1.1 用户注册 (Auth Register)

- 变更说明：注册接口回归纯粹的账号创建。咨询师注册后状态为 2 (待审核)，此时还未提交资料。
- 方法: POST
- 路径: /auth/register
- 请求 Body (JSON):
- codeJSON
  {
  "username": "doctor_zhang",
  "password": "Password123!",
  "nickname": "张医生",
  "phone": "13800138000",
  "role": "counselor", // user, counselor, admin"invitationCode": "" // admin 注册仍需邀请码
  }
- (移除了原有的 counselorProfile 对象)
- 响应 (咨询师注册成功):
- codeJSON
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
- 建议：为了体验流畅，可以给咨询师返回一个带有 PRE_AUTH 权限的 Token，仅允许调用“提交资质”和“查询审核状态”接口，不允许接单。

### 1.2 用户登录 (Auth Login)

- 名称: 用户登录
- 方法: POST
- 路径: /auth/login
- 逻辑说明: 后端需校验密码并检查 status。
- 请求 Body (JSON):
  {
  "username": "doctor_zhang",
  "password": "Password123!"
  }
- 响应 - 登录成功 (status=1):
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
- 响应 - 登录失败 (status=2 待审核):
  {
  "code": 403,
  "message": "您的账号正在审核中，请耐心等待或联系管理员。",
  "data": null
  }
- 响应 - 登录失败 (status=0 已禁用):
  {
  "code": 403,
  "message": "您的账号已被禁用。",
  "data": null
  }

---

### 1.3 获取个人信息

- 名称: 获取个人信息
- 方法: GET
- 路径: /auth/profile
- 响应 (JSON):
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

### 1.4 更新个人信息

- 名称: 更新个人信息
- 方法: PUT
- 路径: /auth/profile
- 请求 Body (JSON):
  {
  "nickname": "大明",
  "avatar": "https://example.com/new_avatar.jpg"
  }
- 响应 (JSON):
  {
  "code": 200,
  "message": "success",
  "data": {
  "success": true
  }
  }

---

## 📦 2. 情绪日记模块 (Mood)

### 2.1 提交情绪日记

- 名称: 提交情绪日记
- 方法: POST
- 路径: /mood/log
- 请求 Body (JSON):
  {
  "moodType": "Happy",
  "moodScore": 8,
  "content": "今天天气很好，心情不错。",
  "tags": ["天气", "运动"],
  "logDate": "2023-10-27 14:00:00"
  }
- 响应 (JSON):
  {
  "code": 200,
  "message": "success",
  "data": {
  "logId": 5001,
  "aiAnalysis": "检测到积极情绪，建议保持。"
  }
  }

### 2.2 获取情绪日记列表

- 名称: 获取情绪日记列表
- 方法: GET
- 路径: /mood/logs
- Query 参数: limit=10, offset=0
- 响应 (JSON):
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

### 2.3 获取单条日记详情

- 名称: 获取单条日记详情
- 方法: GET
- 路径: /mood/log/{id} (注意在 ApiFox 路径参数中添加 id)
- 响应 (JSON):
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

### 2.4 删除日记

- 名称: 删除日记
- 方法: DELETE
- 路径: /mood/log/{id}
- 响应 (JSON):
  {
  "code": 200,
  "message": "success",
  "data": {
  "success": true
  }
  }

### 2.5 获取情绪趋势

- 名称: 获取情绪趋势
- 方法: GET
- 路径: /mood/trend
- Query 参数: days=7
- 响应 (JSON):
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

### 2.6 获取情绪统计

- 名称: 获取情绪统计
- 方法: GET
- 路径: /mood/statistics
- 响应 (JSON):
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

---

## 📦 3. 心理测评模块 (Assessment)

### 3.1 获取量表列表

- 名称: 获取量表列表
- 方法: GET
- 路径: /assessment/scales
- 响应 (JSON):
  {
  "code": 200,
  "message": "success",
  "data": {
  "scales": [
  {
  "scaleKey": "gad-7",
  "title": "焦虑症筛查量表",
  "coverUrl": "",
  "description": "用于评估焦虑程度...",
  "status": "active"
  }
  ]
  }
  }

### 3.2 获取量表详情

- 名称: 获取量表题目
- 方法: GET
- 路径: /assessment/scale/{scaleKey}
- 响应 (JSON):
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

### 3.3 提交测评

- 名称: 提交测评答案
- 方法: POST
- 路径: /assessment/submit
- 请求 Body (JSON):
  {
  "scaleKey": "gad-7",
  "answers": [
  { "questionId": 1, "score": 1 },
  { "questionId": 2, "score": 0 }
  ]
  }
- 响应 (JSON):
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

### 3.4 获取测评历史列表

- 名称: 获取测评历史列表
- 方法: GET
- 路径: /assessment/records
- Query 参数: limit=10
- 响应 (JSON):
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

### 3.5 获取单次测评详情

- 名称: 获取测评结果详情
- 方法: GET
- 路径: /assessment/record/{id}
- 响应 (JSON):
  {
  "code": 200,
  "message": "success",
  "data": {
  "id": 8001,
  "scaleKey": "gad-7",
  "totalScore": 5,
  "resultLevel": "轻度焦虑",
  "resultDesc": "建议放松...",
  "answersDetail": [
  { "questionId": 1, "score": 1, "questionText": "..." }
  ],
  "createTime": "2023-10-27 15:00:00"
  }
  }

### 3.6 创建/更新量表 (Admin)

此接口用于管理员在后台配置量表的基础信息，特别是核心的评分规则。

- 名称: 创建/更新量表配置
- 权限: 管理员 (Admin)
- 方法: POST
- 路径: /admin/assessment/scale
- 逻辑说明:
  - 如果传了 id，则执行更新操作。
  - 如果没传 id，则执行创建操作（需校验 scaleKey 唯一性）。
- 请求 Body (JSON):
  {
  "id": null, // 如果是更新，请填入 ID
  "scaleKey": "gad-7",
  "title": "GAD-7 焦虑症筛查量表",
  "coverUrl": "https://oss.com/images/gad7.jpg",
  "description": "本量表用于评估您在过去两周内的焦虑程度。",
  "status": "active",
  "scoringRules": [
  {
  "min": 0,
  "max": 4,
  "level": "没有焦虑",
  "desc": "您的情绪状态良好，请继续保持。"
  },
  {
  "min": 5,
  "max": 9,
  "level": "轻度焦虑",
  "desc": "建议适当进行放松训练。"
  },
  {
  "min": 10,
  "max": 14,
  "level": "中度焦虑",
  "desc": "建议寻求心理咨询师帮助。"
  },
  {
  "min": 15,
  "max": 21,
  "level": "重度焦虑",
  "desc": "强烈建议前往医院就诊。"
  }
  ]
  }
- 响应 (JSON):
- codeJSON
  {
  "code": 200,
  "message": "保存成功",
  "data": {
  "scaleId": 1
  }
  }

### 3.7 管理量表题目 (Admin)

此接口用于批量保存或更新某个量表下的所有题目。

- 名称: 管理量表题目
- 权限: 管理员 (Admin)
- 方法: POST
- 路径: /admin/assessment/question
- 逻辑说明:
  - 这是一个全量保存或批量更新接口。
  - 后端根据 scaleKey 找到对应量表。
  - 遍历 questions 数组：
    - 如果题目对象带 id，则更新该题目。
    - 如果题目对象不带 id，则新增该题目。
    - (可选逻辑) 也可以设计为：先删除该 scaleKey 下所有旧题目，再插入新题目（实现简单，但要注意 ID 变化）。
- 请求 Body (JSON):
  {
  "scaleKey": "gad-7",
  "questions": [
  {
  "id": 1, // 如果是修改现有题目，带上 ID
  "questionText": "1. 感觉紧张、焦虑或急切？",
  "sortOrder": 1,
  "options": [
  { "label": "完全没有", "score": 0 },
  { "label": "有几天", "score": 1 },
  { "label": "一半以上天数", "score": 2 },
  { "label": "几乎每天", "score": 3 }
  ]
  },
  {
  "id": null, // 如果是新增题目，ID 为空
  "questionText": "2. 不能停止或控制担忧？",
  "sortOrder": 2,
  "options": [
  { "label": "完全没有", "score": 0 },
  { "label": "有几天", "score": 1 }
  // ... 省略其他选项
  ]
  }
  ]
  }
- 响应 (JSON):
- codeJSON
  {
  "code": 200,
  "message": "题目保存成功",
  "data": {
  "success": true,
  "count": 2 // 成功处理的题目数量
  }
  }

## 📦 4. AI 咨询模块 (Chat)

4.1 创建会话
- 名称: 创建AI会话
- 方法: POST
- 路径: /chat/session
- 响应 (JSON):
{
  "code": 200,
  "message": "success",
  "data": {
    "sessionId": "sess_123456"
  }
}
4.2 获取会话列表
- 名称: 获取会话列表
- 方法: GET
- 路径: /chat/sessions
- Query 参数: limit=20
- 响应 (JSON):
 {
  "code": 200,
  "message": "success",
  "data": {
    "total": 1,
    "sessions": [
      {
        "sessionId": "sess_123456",
        "session_title": "关于失眠的咨询",
        "createTime": "2023-10-27T16:00:00"
      }
    ]
  }
}
4.3 发送消息 (流式)
- 名称: 发送消息(Stream)
- 方法: POST
- 路径: /chat/message
- 注意: ApiFox 中请在“高级设置”里开启“返回响应”为 Event Stream 模拟流式，或者作为普通JSON处理。
- 请求 Body (JSON):
{
  "sessionId": "sess_123456",
  "content": "我最近总是睡不着"
}
- 响应 (文本流):
流式响应
4.4 获取历史记录
- 名称: 获取历史记录
- 方法: GET
- 路径: /chat/history/{sessionId}
- Query 参数: limit=50
- 响应 (JSON):
{
  "code": 200,
  "message": "success",
  "data": {
    "sessionId": "sess_123456",
    "messages": [
      {
        "sender": "user",
        "content": "睡不着",
        "createTime": "2025-12-12 16:05:00"
      },
      {
        "sender": "AI",
        "content": "试着放松...",
        "createTime": "2025-12-12 16:05:00"
      }
    ]
  }
}
4.5 删除会话
- 名称: 删除会话
- 方法: DELETE
- 路径: /chat/session/{sessionId}
- 响应 (JSON):
{
  "code": 200,
  "message": "success",
  "data": {
    "success": true
  }
}

4.6 检测敏感词
- 名称: 检测敏感词
- 方法: POST
- 路径: /chat/check-sensitive-words
- 响应 (JSON):
{
  "code": 200,
  "message": "success",
  "data": {
        "containsSensitiveWord": true,
        "sensitiveWords": ["自杀"],
        "originalText": "我今天心情很差，甚至想到了自杀"
    }
}
---

## 📦 5. 咨询师推荐模块 (Counselor Recommendation)

### 5.1 智能推荐咨询师 (核心接口)

- 名称: 智能推荐咨询师
- 方法: GET
- 路径: /counselor/recommend
- Query 参数（可选）:
- keyword (String): 模糊搜索关键词。支持匹配 realName、title、bio、location 以及 specialty(JSON)。
- sort (String): smart(默认)、price_asc、rating_desc
- 后端处理逻辑（伪代码对齐当前实现）:

1. 获取用户上下文

- 最近 7 天 mood_log：平均分 < 4 → isUrgent=true。
- 最近 1 次 assessment_record：提取 result_level/scale_key 关键词（如含“焦虑”“抑郁”“失眠”）。

2. 构建查询条件

- 若手动 keyword 存在，优先采用；否则使用测评/情绪关键词。
- 关键词用于 fuzzy 匹配：JSON_SEARCH(cp.specialty, '%keyword%') OR real_name/title/bio/location LIKE %keyword%。

3. 加权/排序

- SQL 中按 match_score（专长/姓名/职称/简介/地区的模糊命中加权）与评分/价格排序：
- smart: match_score DESC, rating DESC
- price_asc: price ASC, rating DESC
- rating_desc: rating DESC, price ASC

- 响应 (JSON):
  {
  "code": 200,
  "message": "success",
  "data": {
  // 推荐上下文：告诉前端本次推荐是基于什么生成的
  "recommendContext": {
  "strategy": "assessment_based", // 策略: assessment_based(基于测评), mood_based(基于情绪), hot_list(无数据兜底/热门)
  "basedOn": "GAD-7 焦虑测评结果",
  "userTags": ["焦虑", "北京"]
  },
  "counselors": [
  {
  "id": 2001,
  "realName": "张医生",
  "avatar": "https://oss.com/avatar/2001.jpg",
  "title": "资深心理咨询师",
  "experienceYears": 8,
  "specialty": ["焦虑", "职场压力", "睡眠障碍"],
  "rating": 4.9,
  "pricePerHour": 500.00,
  "location": "北京朝阳",
  "nextAvailableTime": "2023-10-28 14:00:00", // 下一个可用时段(用于展示紧迫感)// 核心字段：单项匹配理由
  "matchReason": "擅长处理焦虑问题，且距离您较近。",
  "tags": ["今日可约", "CBT 疗法"]
  },
  {
  "id": 2005,
  "realName": "李医生",
  "title": "心理咨询师",
  "experienceYears": 3,
  "specialty": ["倾听", "焦虑"],
  "rating": 4.8,
  "pricePerHour": 300.00,
  "location": "线上咨询",
  "nextAvailableTime": "2023-10-29 09:00:00",
  "matchReason": "性价比高，适合轻度焦虑咨询。",
  "tags": ["价格亲民"]
  }
  ]
  }
  }

### 5.2 检查推荐前置状态 (辅助接口)

用于前端判断页面展示逻辑。如果用户是新用户（无数据），前端可能展示“去测评”的引导卡片；如果有数据，则直接展示推荐列表。

- 名称: 检查推荐前置状态
- 方法: GET
- 路径: /counselor/recommend/status
- 响应 (JSON):
  {
  "code": 200,
  "message": "success",
  "data": {
  "hasAssessment": true, // 是否做过测评
  "hasMoodLog": true, // 是否写过日记
  "lastAssessmentLevel": "重度焦虑", // 用于前端展示文案 "根据您的[重度焦虑]结果推荐"
  "recommendationReady": true // 是否具备智能推荐条件
  }
  }

### 5.3 获取咨询师详情

- 名称: 获取咨询师详情
- 方法: GET
- 路径: /counselor/{id}
- 响应 (JSON):
  {
  "code": 200,
  "message": "success",
  "data": {
  "id": 2001,
  "realName": "张医生",
  "avatar": "https://oss.com/avatar/2001.jpg",
  "title": "资深心理咨询师",
  "experienceYears": 10,
  "specialty": ["焦虑", "失眠", "亲子关系"],
  "bio": "毕业于北大心理系，拥有 10 年临床经验，擅长认知行为疗法(CBT)...",
  "qualificationUrl": "https://oss.com/cert.jpg", // 资质展示
  "rating": 4.9,
  "reviewCount": 120,
  "pricePerHour": 500.00,
  "location": "北京朝阳",
  "isOnline": true, // 是否支持视频咨询
  "tags": ["耐心", "专业", "回复快"] // 从评论中提取的高频词
  }
  }

### 5.4 获取咨询师评价列表

- 名称: 获取咨询师评价
- 方法: GET
- 路径: /counselor/{id}/reviews
- Query 参数: limit=10, offset=0
- 响应 (JSON):
  {
  "code": 200,
  "message": "success",
  "data": {
  "total": 120,
  "avgRating": 4.9,
  "reviews": [
  {
  "id": 6001,
  "userId": 1002,
  "nickname": "用户_8821", // 匿名化处理
  "avatar": "http://...",
  "rating": 5,
  "content": "张医生非常专业，仅仅一次咨询就让我感觉好多了。",
  "createTime": "2023-10-20 14:30:00"
  }
  ]
  }
  }

### 5.5 提交评价

- 名称: 提交评价
- 方法: POST
- 路径: /counselor/review
- 请求 Body (JSON):
  {
  "appointmentId": 3001, // 必须关联具体的预约订单"rating": 5, // 1-5 分
  "content": "很有耐心，引导得很好。",
  "isAnonymous": true // 是否匿名评价
  }
- 响应 (JSON):
  {
  "code": 200,
  "message": "评价提交成功",
  "data": {
  "reviewId": 6001
  }
  }

---

## 📦 6. 预约管理模块 (Appointment)

### 6.0 [新增] 设置排班 (咨询师端)

这是 6.1 能正常工作的前提。

- 名称: 设置/更新排班
- 权限: 咨询师
- 方法: POST
- 路径: /appointment/schedule
- 请求 Body:
  {
  "workDays": [1, 2, 3, 4, 5], // 周一到周五"workHours": [
  { "start": "09:00", "end": "12:00" },
  { "start": "14:00", "end": "18:00" }
  ]
  }
- 响应: { "code": 200, "message": "排班设置成功" }

### 6.1 查询可用时段

- 名称: 查询可用时段
- 方法: GET
- 路径: /appointment/available-slots
- Query 参数: counselorId=2001, date=2023-11-01
- 响应 (JSON):
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
- 后端需结合“咨询师排班配置”和“已有订单”计算可用性。

### 6.2 创建预约

- 名称: 创建预约
- 方法: POST
- 路径: /appointment/create
- 请求 Body (JSON):
  {
  "counselorId": 2001,
  "startTime": "2023-11-01 09:00:00",
  "endTime": "2023-11-01 10:00:00",
  "userNote": "希望能聊聊工作压力"
  }
- 响应 (JSON):
  {
  "code": 200,
  "message": "success",
  "data": {
  "appointmentId": 3001,
  "status": "PENDING"
  }
  }

### 6.3 获取我的预约

- 名称: 获取我的预约
- 方法: GET
- 路径: /appointment/my-appointments
- Query 参数: status=PENDING, page=1, pageSize=10
- 响应 (JSON):
  {
  "code": 200,
  "message": "success",
  "data": {
  "total": 50,
  "list": [
  {
  "id": 3001,
  "startTime": "2023-11-01 09:00:00",
  "endTime": "2023-11-01 10:00:00",
  "status": "PENDING",
  // 动态返回：如果是用户查，返回咨询师信息
  "targetName": "张医生",
  "targetAvatar": "http://...",
  "targetRole": "counselor",
  // 如果是咨询师查，返回用户信息
  // "targetName": "小明",
  // "targetRole": "user"
  }
  ]
  }
  }

### 6.4 获取预约详情

- 名称: 获取预约详情
- 方法: GET
- 路径: /appointment/{id}
- 响应 (JSON):
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

### 6.5 取消预约

- 名称: 取消预约
- 方法: PUT
- 路径: /appointment/{id}/cancel
- 请求 Body (JSON):
  {
  "cancelReason": "临时有事"
  }
- 响应 (JSON):
  {
  "code": 200,
  "message": "success",
  "data": {
  "success": true
  }
  }

### 6.6 确认预约 (咨询师)

- 名称: 确认预约(咨询师)
- 方法: PUT
- 路径: /appointment/{id}/confirm
- 响应 (JSON):
  {
  "code": 200,
  "message": "success",
  "data": {
  "success": true
  }
  }

---

## 📦 7. 情绪报告模块 (Report)

### 7.1 生成情绪报告

- 名称: 生成情绪报告
- 方法: GET
- 路径: /report/emotion
- Query 参数: startDate=2023-10-01, endDate=2023-10-31
- 响应 (JSON):
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
  { "date": "10-30", "moodType": "Happy", "score": 8, "content": "Nice day" }
  ],
  "aiSuggestions": ["建议保持运动", "睡前冥想"]
  }
  }

### 7.2 导出报告 PDF

- 名称: 导出报告 PDF
- 方法: GET
- 路径: /report/export
- Query 参数: format=pdf
- 注意: ApiFox 响应类型需设置为 Blob/File 才能预览下载。
- 响应: (Binary File Stream)

### 7.3 咨询师查看用户报告
- 名称: 咨询师查看用户的情绪档案
- 方法: GET
- 路径: /report/export/{userId}
- Query 参数: format=pdf
- 注意: ApiFox 响应类型需设置为 Blob/File 才能预览下载。
- 响应: (Binary File Stream)
---

## 📦 8. 用户个人中心 (User)

### 8.1 获取 Dashboard 数据

- 名称: 用户 Dashboard
- 方法: GET
- 路径: /user/dashboard
- 响应 (JSON):
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

### 8.2 获取通知列表

- 名称: 获取通知列表
- 方法: GET
- 路径: /user/notifications
- Query 参数: limit=20
- 响应 (JSON):
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

### 8.3 标记通知已读

- 名称: 标记通知已读
- 方法: PUT
- 路径: /user/notification/{id}/read
- 响应 (JSON):
  {
  "code": 200,
  "message": "success",
  "data": {
  "success": true
  }
  }

---

## 🔌 WebSocket 接口

WS.1 AI 实时对话

- 在 ApiFox 中新建“WebSocket 接口”
- URL: ws://127.0.0.1:8080/ws/chat
- 握手 Params (Query): token=eyJ... (JWT Token 通常放在 URL 参数中进行 WS 握手)
- 发送消息示例 (Message):
  {
  "type": "message",
  "sessionId": "sess_001",
  "content": "你好，AI"
  }
- 接收消息示例 (Message):
  {
  "type": "ai_response",
  "content": "你好！有什么我可以帮你的吗？",
  "isComplete": false
  }

## 📦 9. 咨询师资质审核模块 (Counselor Audit )

### 9.1 提交/更新资质审核申请 (User Side)

- 名称: 提交资质审核
- 权限: 登录用户 (Role=counselor)
- 方法: POST
- 路径: /counselor/audit/submit
- 逻辑说明:
  1. 插入数据到 counselor_audit_record 表，状态为 PENDING。
  2. 如果是被驳回后重新提交，生成一条新的记录（保留历史驳回记录）。
- 请求 Body (JSON):
  {
  "realName": "公冶婷方",
  "qualificationUrl": "https://frilly-dandelion.name/",
  "idCardUrl": "https://golden-wombat.info/",
  "title": "资深心理咨询师",
  "experienceYears": 5,
  "specialty": [
  "焦虑",
  "紧张"
  ],
  "bio": "二级咨询师",
  "location": "上海嘉定",
  "pricePerHour": 500
  }
- 响应:
  {
  "code": 200,
  "message": "资质提交成功，请等待管理员审核",
  "data": {
  "auditId": 501 // 返回审核记录 ID
  }
  }

### 9.2 获取审核状态 (User Side)

- 名称: 获取我的审核状态
- 方法: GET
- 路径: /counselor/audit/status
- 响应:
  {
  "code": 200,
  "message": "success",
  "data": {
  "latestStatus": "REJECTED", // PENDING, APPROVED, REJECTED
  "auditRemark": "图片模糊，请重新上传", // 驳回原因
  "submitTime": "2023-11-01 10:00:00"
  }
  }

---

## 📦 10. 管理员审核接口 (Admin Audit)

### 10.1 获取待审核列表

- 方法: GET
- 路径: /admin/audit/list
- Query 参数: page=1, pageSize=20 (管理员后台通常数据量大，必须分页)
- 响应 (JSON):
  {
  "code": 200,
  "message": "success",
  "data": {
  "total": 5,
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

### 10.2 审核操作 (通过/拒绝)

- 方法: POST
- 路径: /admin/audit/process
- 请求 Body:
  {
  "auditId": 501,
  "action": "PASS", // 或 "REJECT"
  "remark": "您的资质已确认无误" // 驳回时必填，通过时可选
  }
- 后端详细逻辑流程:
  1. 校验权限: 确认当前操作者是管理员。
  2. 获取数据: 根据 auditId 查询审核记录，获取申请人的 user_id。
  3. 更新审核表 (counselor_audit_record):
  - 更新 status, audit_time, auditor_id。
  - 记录 audit_remark。
  4. 处理业务状态 (事务内):
  - 如果是 PASS (通过):
    - 修改 sys_user 表：status = 1 (正常)。
    - 同步/更新 counselor_profile 表数据。
    - 【新增】写入通知: 向 sys_notification 插入一条记录。
      - type: "audit"
      - title: "资质审核通过"
      - content: "恭喜您，您的咨询师资质审核已通过！您现在可以设置排班并开始接单了。"
  - 如果是 REJECT (拒绝):
    - 修改 sys_user 表：status 保持 2 (或视业务需求而定)。
    - 【新增】写入通知: 向 sys_notification 插入一条记录。
      - type: "audit"
      - title: "资质审核未通过"
      - content: "很遗憾，您的资质审核被驳回。原因：" + remark + "。请修改资料后重新提交。"
  5. 返回响应: 成功。
