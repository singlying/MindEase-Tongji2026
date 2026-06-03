# MindEase - 心理健康平台

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.8-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![MyBatis](https://img.shields.io/badge/MyBatis-3.0.4-red.svg)](https://mybatis.org/mybatis-3/)
[![MySQL](https://img.shields.io/badge/MySQL-8.x-blue.svg)](https://www.mysql.com/)

MindEase 是一个综合性的心理健康服务平台，提供心理咨询师推荐、在线预约、心理测评、情绪日记等功能，致力于为用户提供专业、便捷的心理健康服务。

---

## 📋 目录

- [项目简介](#项目简介)
- [核心功能](#核心功能)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
- [API 接口文档](#api-接口文档)
- [数据库设计](#数据库设计)

---

## 🎯 项目简介

MindEase 平台面向三类用户：

- **普通用户**：可以进行心理测评、记录情绪日记、预约咨询师、获取推荐服务
- **咨询师**：可以提交资质审核、设置排班、管理预约、接收评价
- **管理员**：可以审核咨询师资质、管理量表题目、监控系统运行

---

## ✨ 核心功能

### 1️⃣ 用户认证与管理
- ✅ 用户注册登录（支持普通用户、咨询师角色）
- ✅ JWT Token 鉴权
- ✅ 个人信息查看与更新
- ✅ 用户 Dashboard 数据展示
- ✅ 系统通知管理

### 2️⃣ 咨询师资质审核
- ✅ 咨询师提交资质材料（证书、身份证）
- ✅ 管理员审核流程（通过/驳回）
- ✅ 审核状态查询
- ✅ 审核记录管理

### 3️⃣ 智能推荐系统
- ✅ 基于用户心理状态智能推荐咨询师
- ✅ 支持关键词搜索、领域筛选
- ✅ 多种排序方式（智能推荐、价格、评分）
- ✅ 咨询师详情展示
- ✅ 推荐状态检测（测评完成度、情绪日志）

### 4️⃣ 预约管理
- ✅ 咨询师设置排班（工作日、工作时段）
- ✅ 查询咨询师可用时段
- ✅ 用户创建预约
- ✅ 咨询师确认/取消预约
- ✅ 预约列表查询（支持状态筛选）
- ✅ 预约详情查看

### 5️⃣ 心理测评模块
- ✅ 量表列表展示（GAD-7、PHQ-9 等）
- ✅ 量表详情与题目获取
- ✅ 测评答案提交与自动评分
- ✅ 测评历史记录查询
- ✅ 测评结果详情展示
- ✅ **管理员**：创建/更新量表配置
- ✅ **管理员**：管理量表题目（新增、修改、删除）

### 6️⃣ 评价系统
- ✅ 用户提交咨询评价
- ✅ 咨询师评价列表展示
- ✅ 评分统计与展示

---

## 🛠 技术栈

### 后端框架
- **Spring Boot 3.5.8** - 主框架
- **MyBatis 3.0.4** - ORM 框架
- **MySQL 8.x** - 数据库
- **JWT 0.12.6** - Token 鉴权
- **Knife4j 4.3.0** - API 文档

### 开发工具
- **Java 21** - 编程语言
- **Maven** - 项目管理
- **Lombok** - 代码简化
- **Jackson** - JSON 处理

---

## 📁 项目结构

```
mindease/
├── src/main/java/com/mindease/
│   ├── common/                          # 公共模块
│   │   ├── constant/                    # 常量定义
│   │   │   ├── JwtClaimsConstant.java   # JWT 声明常量
│   │   │   ├── MessageConstant.java     # 消息常量
│   │   │   └── StatusConstant.java      # 状态常量
│   │   ├── exception/                   # 自定义异常
│   │   │   ├── BaseException.java
│   │   │   ├── AccountNotFoundException.java
│   │   │   ├── PasswordErrorException.java
│   │   │   └── ...
│   │   ├── result/                      # 统一响应结果
│   │   │   └── Result.java
│   │   └── utils/                       # 工具类
│   │       └── JwtUtil.java
│   │
│   ├── config/                          # 配置类
│   │   └── WebMvcConfiguration.java     # Web MVC 配置
│   │
│   ├── controller/                      # 控制器层
│   │   ├── admin/                       # 管理员端控制器
│   │   │   ├── AdminAssessmentController.java  # 测评管理
│   │   │   ├── AdminAuditController.java       # 审核管理
│   │   │   └── CommonController.java           # 管理员登录
│   │   ├── appointment/                 # 预约控制器
│   │   │   └── AppointmentController.java
│   │   ├── assessment/                  # 心理测评控制器
│   │   │   └── AssessmentController.java
│   │   ├── auth/                        # 认证控制器
│   │   │   └── AuthController.java
│   │   ├── counselor/                   # 咨询师控制器
│   │   │   ├── CounselorController.java       # 咨询师推荐
│   │   │   └── CounselorAuditController.java  # 资质审核
│   │   └── user/                        # 用户中心控制器
│   │       └── UserCenterController.java
│   │
│   ├── handler/                         # 全局处理器
│   │   └── GlobalExceptionHandler.java  # 全局异常处理
│   │
│   ├── interceptor/                     # 拦截器
│   │   └── JwtTokenInterceptor.java     # JWT Token 拦截器
│   │
│   ├── mapper/                          # MyBatis Mapper 接口
│   │   ├── UserMapper.java
│   │   ├── CounselorProfileMapper.java
│   │   ├── CounselorAuditRecordMapper.java
│   │   ├── AppointmentMapper.java
│   │   ├── AssessmentScaleMapper.java
│   │   ├── AssessmentQuestionMapper.java
│   │   ├── AssessmentRecordMapper.java
│   │   ├── AssessmentAnswerMapper.java
│   │   ├── MoodLogMapper.java
│   │   ├── CounselorReviewMapper.java
│   │   └── SysNotificationMapper.java
│   │
│   ├── pojo/                            # 实体类
│   │   ├── dto/                         # 数据传输对象
│   │   │   ├── UserLoginDTO.java
│   │   │   ├── UserRegisterDTO.java
│   │   │   ├── UserUpdateDTO.java
│   │   │   ├── AuditSubmitDTO.java
│   │   │   ├── AuditProcessDTO.java
│   │   │   ├── AppointmentCreateDTO.java
│   │   │   ├── AppointmentCancelDTO.java
│   │   │   ├── ScheduleSetDTO.java
│   │   │   ├── AssessmentSubmitDTO.java
│   │   │   ├── ScaleSaveDTO.java
│   │   │   ├── QuestionManageDTO.java
│   │   │   └── ReviewSubmitDTO.java
│   │   ├── entity/                      # 数据库实体
│   │   │   ├── User.java
│   │   │   ├── CounselorProfile.java
│   │   │   ├── CounselorAuditRecord.java
│   │   │   ├── Appointment.java
│   │   │   ├── AssessmentScale.java
│   │   │   ├── AssessmentQuestion.java
│   │   │   ├── AssessmentRecord.java
│   │   │   ├── AssessmentAnswer.java
│   │   │   ├── MoodLog.java
│   │   │   ├── CounselorReview.java
│   │   │   └── SysNotification.java
│   │   └── vo/                          # 视图对象
│   │       ├── UserLoginVO.java
│   │       ├── UserRegisterVO.java
│   │       ├── UserProfileVO.java
│   │       ├── DashboardVO.java
│   │       ├── NotificationListVO.java
│   │       ├── AuditStatusVO.java
│   │       ├── AuditListVO.java
│   │       ├── RecommendResultVO.java
│   │       ├── CounselorDetailVO.java
│   │       ├── AppointmentListVO.java
│   │       ├── AppointmentDetailVO.java
│   │       ├── ScaleListVO.java
│   │       ├── ScaleDetailVO.java
│   │       ├── AssessmentSubmitVO.java
│   │       ├── AssessmentRecordListVO.java
│   │       └── ...
│   │
│   ├── service/                         # 服务接口
│   │   ├── UserService.java
│   │   ├── UserCenterService.java
│   │   ├── CounselorService.java
│   │   ├── CounselorAuditService.java
│   │   ├── AppointmentService.java
│   │   ├── AssessmentService.java
│   │   └── impl/                        # 服务实现类
│   │       ├── UserServiceImpl.java
│   │       ├── UserCenterServiceImpl.java
│   │       ├── CounselorServiceImpl.java
│   │       ├── CounselorAuditServiceImpl.java
│   │       ├── AppointmentServiceImpl.java
│   │       └── AssessmentServiceImpl.java
│   │
│   └── MindeaseApplication.java         # 启动类
│
├── src/main/resources/
│   ├── application.yml                  # 主配置文件
│   ├── application-dev.yml              # 开发环境配置
│   └── mapper/                          # MyBatis XML 映射文件
│       ├── AppointmentMapper.xml
│       ├── CounselorProfileMapper.xml
│       ├── AssessmentQuestionMapper.xml
│       └── AssessmentAnswerMapper.xml
│
├── database.md                          # 数据库设计文档
├── api.md                              # API 接口文档
├── pom.xml                             # Maven 配置
└── README.md                           # 项目说明文档
```

---

## 🚀 快速开始

### 1. 环境要求

- **JDK 21+**
- **Maven 3.6+**
- **MySQL 8.0+**

### 2. 克隆项目

```bash
git clone <repository-url>
cd mindease
```

### 3. 配置数据库

1. 创建数据库：
```sql
CREATE DATABASE mindease CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 执行数据库脚本（参考 `database.md` 文件）

3. 修改配置文件 `src/main/resources/application-dev.yml`：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mindease?useUnicode=true&characterEncoding=utf-8
    username: your_username
    password: your_password
```

### 4. 配置 JWT 密钥

在 `application-dev.yml` 中配置：
```yaml
mindease:
  jwt:
    secret-key: your-secret-key
    ttl: 86400000  # 24小时
```

### 5. 启动项目

```bash
# 使用 Maven 编译
mvn clean compile

# 启动应用
mvn spring-boot:run
```

### 6. 访问应用

- **应用地址**：http://localhost:8080
- **API 文档**：http://localhost:8080/doc.html (Knife4j)

---

## 📚 API 接口文档

### 1️⃣ 认证模块 (`/auth`)

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | `/auth/register` | 用户注册 | ❌ |
| POST | `/auth/login` | 用户登录 | ❌ |
| GET | `/auth/profile` | 获取个人信息 | ✅ |
| PUT | `/auth/profile` | 更新个人信息 | ✅ |

#### 注册示例
```json
POST /auth/register
{
  "username": "user123",
  "password": "password123",
  "nickname": "小明",
  "phone": "13800138000",
  "role": "user"  // user: 普通用户, counselor: 咨询师
}
```

#### 登录示例
```json
POST /auth/login
{
  "username": "user123",
  "password": "password123"
}

// 响应
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 1,
    "username": "user123",
    "nickname": "小明",
    "role": "USER",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

---

### 2️⃣ 用户中心模块 (`/user`)

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| GET | `/user/dashboard` | 获取 Dashboard 数据 | ✅ |
| GET | `/user/notifications` | 获取通知列表 | ✅ |
| PUT | `/user/notification/{id}/read` | 标记通知已读 | ✅ |

---

### 3️⃣ 咨询师资质审核模块 (`/counselor/audit`)

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | `/counselor/audit/submit` | 提交资质审核 | ✅ |
| GET | `/counselor/audit/status` | 获取审核状态 | ✅ |

#### 提交审核示例
```json
POST /counselor/audit/submit
{
  "realName": "张医生",
  "qualificationUrl": "https://example.com/cert.jpg",
  "idCardUrl": "https://example.com/id.jpg"
}
```

---

### 4️⃣ 咨询师推荐模块 (`/counselor`)

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| GET | `/counselor/recommend/status` | 检查推荐状态 | ✅ |
| GET | `/counselor/recommend` | 智能推荐咨询师 | ✅ |
| GET | `/counselor/{id}` | 获取咨询师详情 | ❌ |
| GET | `/counselor/{id}/reviews` | 获取咨询师评价 | ❌ |
| POST | `/counselor/review` | 提交评价 | ✅ |

#### 推荐咨询师示例
```json
GET /counselor/recommend?keyword=焦虑&filterSpecialty=焦虑症&sort=smart

// 响应
{
  "code": 200,
  "data": {
    "counselors": [
      {
        "userId": 10,
        "realName": "李医生",
        "title": "资深心理咨询师",
        "specialty": ["焦虑症", "抑郁症"],
        "pricePerHour": 300.00,
        "rating": 4.8,
        "matchScore": 95
      }
    ],
    "recommendContext": {
      "primaryMood": "焦虑",
      "moodScore": 7,
      "moodCount": 10
    }
  }
}
```

---

### 5️⃣ 预约管理模块 (`/appointment`)

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | `/appointment/schedule` | 设置排班（咨询师） | ✅ |
| GET | `/appointment/available-slots` | 查询可用时段 | ❌ |
| POST | `/appointment/create` | 创建预约 | ✅ |
| GET | `/appointment/my-appointments` | 获取预约列表 | ✅ |
| GET | `/appointment/{id}` | 获取预约详情 | ✅ |
| PUT | `/appointment/{id}/cancel` | 取消预约 | ✅ |
| PUT | `/appointment/{id}/confirm` | 确认预约（咨询师） | ✅ |

#### 设置排班示例
```json
POST /appointment/schedule
{
  "workDays": [1, 2, 3, 4, 5],  // 周一到周五
  "workHours": [
    { "start": "09:00", "end": "12:00" },
    { "start": "14:00", "end": "18:00" }
  ]
}
```

#### 创建预约示例
```json
POST /appointment/create
{
  "counselorId": 10,
  "startTime": "2025-12-15 10:00:00",
  "endTime": "2025-12-15 11:00:00",
  "userNote": "我最近感到焦虑"
}
```

---

### 6️⃣ 心理测评模块 (`/assessment`)

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| GET | `/assessment/scales` | 获取量表列表 | ❌ |
| GET | `/assessment/scale/{scaleKey}` | 获取量表详情 | ❌ |
| POST | `/assessment/submit` | 提交测评 | ✅ |
| GET | `/assessment/records` | 获取测评历史 | ✅ |
| GET | `/assessment/record/{id}` | 获取测评详情 | ✅ |

#### 获取量表列表示例
```json
GET /assessment/scales

// 响应
{
  "code": 200,
  "data": {
    "scales": [
      {
        "id": 1,
        "scaleKey": "gad-7",
        "title": "GAD-7 焦虑症筛查量表",
        "coverUrl": "https://example.com/gad7.jpg",
        "description": "用于评估焦虑程度",
        "status": "active"
      }
    ]
  }
}
```

#### 提交测评示例
```json
POST /assessment/submit
{
  "scaleKey": "gad-7",
  "answers": [
    { "questionId": 1, "score": 2 },
    { "questionId": 2, "score": 1 },
    { "questionId": 3, "score": 3 }
  ]
}

// 响应
{
  "code": 200,
  "data": {
    "recordId": 8001,
    "totalScore": 12,
    "resultLevel": "中度焦虑",
    "resultDesc": "建议寻求心理咨询师帮助"
  }
}
```

---

### 7️⃣ 管理员模块

#### 审核管理 (`/admin/audit`)

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| GET | `/admin/audit/list` | 获取待审核列表 | ✅ Admin |
| POST | `/admin/audit/process` | 审核操作（通过/驳回） | ✅ Admin |

#### 审核操作示例
```json
POST /admin/audit/process
{
  "auditId": 5,
  "action": "APPROVED",  // APPROVED: 通过, REJECTED: 驳回
  "remark": "资质符合要求"
}
```

#### 测评管理 (`/admin/assessment`)

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | `/admin/assessment/scale` | 创建/更新量表 | ✅ Admin |
| POST | `/admin/assessment/question` | 管理量表题目 | ✅ Admin |

#### 创建量表示例
```json
POST /admin/assessment/scale
{
  "scaleKey": "gad-7",
  "title": "GAD-7 焦虑症筛查量表",
  "coverUrl": "https://example.com/gad7.jpg",
  "description": "本量表用于评估焦虑程度",
  "status": "active",
  "scoringRules": [
    { "min": 0, "max": 4, "level": "没有焦虑", "desc": "情绪状态良好" },
    { "min": 5, "max": 9, "level": "轻度焦虑", "desc": "建议适当放松" },
    { "min": 10, "max": 14, "level": "中度焦虑", "desc": "建议寻求咨询" },
    { "min": 15, "max": 21, "level": "重度焦虑", "desc": "强烈建议就诊" }
  ]
}

// 响应
{
  "code": 200,
  "message": "量表创建成功",
  "data": {
    "scaleId": 1,
    "isUpdate": false
  }
}
```

#### 管理题目示例
```json
POST /admin/assessment/question
{
  "scaleKey": "gad-7",
  "questions": [
    {
      "id": 1,
      "deleted": true  // 删除题目
    },
    {
      "id": 2,
      "questionText": "更新的题目内容",
      "sortOrder": 1,
      "options": [
        { "label": "完全没有", "score": 0 },
        { "label": "有几天", "score": 1 }
      ]
    },
    {
      "questionText": "新题目内容",
      "sortOrder": 2,
      "options": [...]
    }
  ]
}

// 响应
{
  "code": 200,
  "message": "题目保存成功",
  "data": {
    "success": true,
    "count": 2
  }
}
```

---

## 🗄️ 数据库设计

### 核心数据表

| 表名 | 说明 |
|------|------|
| `sys_user` | 系统用户表（存储所有角色用户） |
| `counselor_profile` | 咨询师公开资料表 |
| `counselor_audit_record` | 咨询师资质审核记录表 |
| `appointment` | 预约订单表 |
| `counselor_review` | 咨询评价表 |
| `assessment_scale` | 量表定义表 |
| `assessment_question` | 量表题目表 |
| `assessment_record` | 测评记录表 |
| `assessment_answer` | 测评答案详情表 |
| `mood_log` | 情绪日记表 |
| `sys_notification` | 系统通知表 |

详细的数据库设计请查看 `database.md` 文件。

---

## 🔐 安全说明

### JWT Token 使用

所有需要鉴权的接口都需要在请求头中携带 Token：

```
Headers:
  token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 拦截器配置

```java
// 需要 Token 的路径
/auth/profile
/counselor/**
/appointment/**
/user/**
/admin/**
/assessment/submit
/assessment/records
/assessment/record/**

// 不需要 Token 的路径（公开接口）
/auth/register
/auth/login
/appointment/available-slots
/assessment/scales
/assessment/scale/**
/counselor/{id}
/counselor/{id}/reviews
```

---

## 📝 开发规范

### 1. 统一响应格式

所有接口返回统一使用 `Result<T>` 包装：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

### 2. 异常处理

使用全局异常处理器 `GlobalExceptionHandler` 统一处理异常。

### 3. 密码加密

使用 MD5 对用户密码进行加密（32位小写十六进制）。

> ⚠️ **安全建议**：生产环境建议使用 BCrypt 或 Argon2 等更安全的加密算法。

---

## 📈 项目特色

### 1. 智能推荐算法
基于用户的情绪日记和心理测评结果，智能匹配最适合的咨询师。

### 2. 灵活的排班系统
咨询师可以自定义工作日和工作时段，系统自动计算可用时段。

### 3. 完整的审核流程
咨询师资质审核支持提交、审核、驳回、重新提交的完整流程。

### 4. 可扩展的测评系统
- 支持动态创建量表和题目
- 灵活的评分规则配置
- 完整的测评记录追溯

### 5. 实时通知系统
重要操作（审核结果、预约确认等）会自动发送系统通知。

---

## 🎯 后续规划

- [ ] 实现情绪日记模块
- [ ] 集成 AI 咨询功能
- [ ] 添加支付功能
- [ ] 实现实时聊天功能
- [ ] 添加数据统计与报表
- [ ] 完善单元测试
- [ ] 添加 Redis 缓存
- [ ] 实现文件上传（OSS）

---

## 📄 许可证

本项目仅供学习交流使用。

---

## 👥 联系方式

如有问题或建议，欢迎提 Issue 或 PR。

---

**MindEase Team** © 2025
