这是一个完整的、更新后的数据库设计文档。

该文档整合了我们之前讨论的所有优化，特别是**咨询师资质审核与账号注册的分离**。设计采用了 MySQL 8.x 标准，使用 `utf8mb4` 字符集，并利用 JSON 类型存储复杂数据。

---

# MindEase 项目数据库设计文档 (V2.0)

## 1. 设计概述
*   **数据库名**: `mindease`
*   **字符集**: `utf8mb4` (支持 Emoji 表情)
*   **排序规则**: `utf8mb4_unicode_ci`
*   **核心设计原则**:
    1.  **审核分离**: 注册仅创建账号，资质审核通过独立表管理，记录审核人(`auditor_id`)与审核历史。
    2.  **JSON利用**: 标签、选项、擅长领域等数组数据使用 `JSON` 类型，减少关联表。
    3.  **逻辑外键**: 表之间通过 ID 逻辑关联，不强制使用物理外键约束，以提高开发灵活性。

---

## 2. 表结构详细说明

### 2.1 用户与权限模块 (User & Auth)

#### 1. 系统用户表 (`sys_user`)
存储所有角色的登录凭证和基础状态。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| **id** | BIGINT | ✅ | - | **主键**，自增 ID |
| username | VARCHAR(64) | ✅ | - | **唯一索引**，登录账号 |
| password | VARCHAR(128) | ✅ | - | 加密后的密码 (BCrypt) |
| nickname | VARCHAR(64) | | NULL | 用户昵称 |
| avatar | VARCHAR(512) | | NULL | 头像 URL |
| phone | VARCHAR(20) | | NULL | 手机号 |
| role | VARCHAR(32) | ✅ | 'user' | 角色: `user`(普通), `counselor`(咨询师), `admin`(管理员) |
| status | TINYINT | ✅ | 1 | **状态**: <br>1-正常 (Active)<br>2-待审核/资料未完善 (Pending)<br>0-禁用 (Banned) |
| create_time | DATETIME | ✅ | NOW | 注册时间 |

#### 2. [新增] 咨询师资质审核记录表 (`counselor_audit_record`)
**核心变更表**：用于存储咨询师提交的资质文件、审核状态以及**审核管理员的记录**。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| **id** | BIGINT | ✅ | - | **主键**，自增 |
| user_id | BIGINT | ✅ | - | 申请人 ID (关联 `sys_user.id`) |
| real_name | VARCHAR(64) | ✅ | - | 真实姓名 |
| qualification_url | VARCHAR(512) | ✅ | - | **资质证书图片 URL** |
| id_card_url | VARCHAR(512) | | NULL | 身份证/执照图片 URL (可选) |
| status | VARCHAR(20) | ✅ | 'PENDING' | 审核状态: `PENDING`(待审), `APPROVED`(通过), `REJECTED`(驳回) |
| **auditor_id** | BIGINT | | NULL | **审核管理员ID** (关联 `sys_user.id`，记录是谁审的) |
| audit_time | DATETIME | | NULL | 审核操作时间 |
| audit_remark | VARCHAR(255) | | NULL | 审核备注 / 驳回原因 |
| create_time | DATETIME | ✅ | NOW | 提交申请时间 |

#### 3. 咨询师公开资料表 (`counselor_profile`)
仅存储**审核通过后**，用于对外展示的业务数据。

| 字段名 | 类型 | 必填 | 说明 |
| :--- | :--- | :--- | :--- |
| **user_id** | BIGINT | ✅ | **主键**，关联 `sys_user.id` |
| real_name | VARCHAR(64) | ✅ | 真实姓名 (审核通过后同步至此) |
| title | VARCHAR(64) | | 头衔 (如: 资深心理咨询师) |
| experience_years | INT | | 从业年限 |
| specialty | JSON | | 擅长领域数组 `["焦虑", "亲子关系"]` |
| bio | TEXT | | 个人简介 |
| qualification_url | VARCHAR(512) | | 当前展示的证书 URL (冗余字段，便于前端查询) |
| location | VARCHAR(128) | | 所在地区 |
| price_per_hour | DECIMAL(10,2) | | 咨询价格/小时 |
| rating | DECIMAL(3,1) | | 综合评分 (默认 5.0) |
| review_count | INT | | 评价总数 |

#### 4. 系统通知表 (`sys_notification`)
| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| **id** | BIGINT | **主键** |
| user_id | BIGINT | 接收人 ID |
| type | VARCHAR(32) | 类型: `system`, `audit` (审核结果), `appointment` |
| title | VARCHAR(128) | 标题 |
| content | TEXT | 内容 |
| is_read | TINYINT | 0-未读, 1-已读 |

---

### 2.2 情绪日记模块 (Mood)

#### 5. 情绪日记表 (`mood_log`)
| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| **id** | BIGINT | **主键** |
| user_id | BIGINT | 用户 ID |
| mood_type | VARCHAR(32) | 情绪类型 (Happy, Sad, Anxious...) |
| mood_score | INT | 评分 (0-10) |
| content | TEXT | 日记内容（🔒 AES-256-GCM 加密存储） |
| tags | TEXT | 标签数组（🔒 AES-256-GCM 加密存储，应用层解析为 JSON） |
| ai_analysis | TEXT | AI 分析建议（🔒 AES-256-GCM 加密存储） |
| log_date | DATETIME | 日记归属日期 |

---

### 2.3 心理测评模块 (Assessment)

#### 6. 量表定义表 (`assessment_scale`)
| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| **id** | BIGINT | **主键** |
| scale_key | VARCHAR(32) | **唯一标识** (如 `gad-7`, `phq-9`) |
| title | VARCHAR(64) | 量表标题 |
| description | VARCHAR(512) | 描述 |

#### 7. 量表题目表 (`assessment_question`)
| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| **id** | BIGINT | **主键** |
| scale_key | VARCHAR(32) | 所属量表 Key |
| question_text | TEXT | 题目内容 |
| options | JSON | 选项配置 `[{"label":"没有","score":0}, {"label":"经常","score":3}]` |
| sort_order | INT | 排序 |

#### 8. 测评记录表 (`assessment_record`)
| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| **id** | BIGINT | **主键** |
| user_id | BIGINT | 用户 ID |
| scale_key | VARCHAR(32) | 量表 Key |
| total_score | INT | 总分 |
| result_level | VARCHAR(64) | 结果等级 (如: 重度抑郁) |
| result_desc | TEXT | 结果建议 |

#### 9. 测评答案详情表 (`assessment_answer`)
| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| **id** | BIGINT | **主键** |
| record_id | BIGINT | 关联记录 ID |
| question_id | BIGINT | 题目 ID |
| score | INT | 该题得分 |
| answer_text | VARCHAR(255) | 选项文本快照 |

---

### 2.4 AI 咨询模块 (Chat)

#### 10. 会话列表表 (`chat_session`)
| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| **id** | VARCHAR(64) | **主键** (UUID/String) |
| user_id | BIGINT | 用户 ID |
| title | VARCHAR(128) | 会话标题 |
| update_time | DATETIME | 最后活跃时间 |

#### 11. 消息记录表 (`chat_message`)
| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| **id** | BIGINT | **主键** |
| session_id | VARCHAR(64) | 会话 ID |
| sender | VARCHAR(20) | 发送者 (`user`, `AI`) |
| content | TEXT | 消息内容（🔒 AES-256-GCM 加密存储） |

---

### 2.5 预约管理模块 (Appointment)

#### 12. 预约订单表 (`appointment`)
| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| **id** | BIGINT | **主键** |
| user_id | BIGINT | 预约人 ID |
| counselor_id | BIGINT | 咨询师 ID |
| start_time | DATETIME | 开始时间 |
| end_time | DATETIME | 结束时间 |
| status | VARCHAR(20) | `PENDING`, `CONFIRMED`, `CANCELLED`, `COMPLETED` |
| user_note | VARCHAR(512) | 用户备注 |
| cancel_reason | VARCHAR(255) | 取消原因 |

#### 13. 咨询评价表 (`counselor_review`)
| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| **id** | BIGINT | **主键** |
| appointment_id | BIGINT | 关联预约 ID |
| counselor_id | BIGINT | 咨询师 ID |
| user_id | BIGINT | 评价人 ID |
| rating | INT | 评分 (1-5) |
| content | TEXT | 评价内容 |

---

## 3. 完整建库 SQL 语句

请直接在数据库管理工具（如 Navicat, DBeaver, MySQL Workbench）中执行以下脚本。

```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS `mindease` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `mindease`;

-- ==========================================
-- 1. 用户与审核模块
-- ==========================================

-- 1.1 系统用户表
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username` VARCHAR(64) NOT NULL COMMENT '用户名/账号',
    `password` VARCHAR(128) NOT NULL COMMENT '加密密码',
    `nickname` VARCHAR(64) DEFAULT NULL COMMENT '昵称',
    `avatar` VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `role` VARCHAR(32) NOT NULL DEFAULT 'user' COMMENT '角色: user, counselor, admin',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-正常, 2-待审核/资料未完善, 0-禁用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 1.2 [新增] 咨询师资质审核记录表
DROP TABLE IF EXISTS `counselor_audit_record`;
CREATE TABLE `counselor_audit_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '申请人ID (关联sys_user)',
    `real_name` VARCHAR(64) NOT NULL COMMENT '真实姓名',
    `qualification_url` VARCHAR(512) NOT NULL COMMENT '资质证书图片URL',
    `id_card_url` VARCHAR(512) DEFAULT NULL COMMENT '身份证/执照URL(可选)',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING(待审), APPROVED(通过), REJECTED(驳回)',
    `auditor_id` BIGINT DEFAULT NULL COMMENT '审核管理员ID (关联sys_user)',
    `audit_time` DATETIME DEFAULT NULL COMMENT '审核处理时间',
    `audit_remark` VARCHAR(255) DEFAULT NULL COMMENT '审核备注/驳回原因',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '提交申请时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_status` (`user_id`, `status`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='咨询师资质审核记录表';

-- 1.3 咨询师公开资料表

DROP TABLE IF EXISTS `counselor_profile`;
CREATE TABLE `counselor_profile` (
    `user_id` BIGINT NOT NULL COMMENT '关联sys_user.id',
    `real_name` VARCHAR(64) NOT NULL COMMENT '真实姓名(同步自审核表)',
    `title` VARCHAR(64) COMMENT '头衔(如:资深咨询师)',
    `experience_years` INT COMMENT '从业年限',
    `specialty` JSON COMMENT '擅长领域JSON数组',
    `bio` TEXT COMMENT '个人简介',
    `qualification_url` VARCHAR(512) COMMENT '当前展示的证书URL',
    `location` VARCHAR(128) COMMENT '所在地区',
    `work_schedule` JSON DEFAULT NULL COMMENT '排班配置: {"workDays":[1,2], "workHours":[{"start":"09:00","end":"12:00"}]}',
    `price_per_hour` DECIMAL(10, 2) DEFAULT 0.00 COMMENT '咨询价格/小时',
    `rating` DECIMAL(3, 1) DEFAULT 5.0 COMMENT '综合评分',
    `review_count` INT DEFAULT 0 COMMENT '评价总数',
    PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='咨询师公开资料表';



-- 1.4 系统通知表
DROP TABLE IF EXISTS `sys_notification`;
CREATE TABLE `sys_notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '接收用户ID',
    `type` VARCHAR(32) NOT NULL COMMENT '类型: system, appointment, audit',
    `title` VARCHAR(128) NOT NULL COMMENT '标题',
    `content` TEXT COMMENT '内容',
    `is_read` TINYINT(1) DEFAULT 0 COMMENT '0-未读, 1-已读',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_read` (`user_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统通知表';

-- ==========================================
-- 2. 情绪日记模块
-- ==========================================

DROP TABLE IF EXISTS `mood_log`;
CREATE TABLE `mood_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `mood_type` VARCHAR(32) NOT NULL COMMENT '情绪类型',
    `mood_score` INT NOT NULL COMMENT '评分(0-10)',
    `content` TEXT COMMENT '日记内容（AES-256-GCM加密存储）',
    `tags` TEXT COMMENT '标签JSON数组（AES-256-GCM加密存储）',
    `ai_analysis` TEXT COMMENT 'AI分析建议（AES-256-GCM加密存储）',
    `log_date` DATETIME NOT NULL COMMENT '日记归属日期',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_date` (`user_id`, `log_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='情绪日记表';

-- ==========================================
-- 3. 心理测评模块
-- ==========================================

-- 量表定义
DROP TABLE IF EXISTS `assessment_scale`;
CREATE TABLE `assessment_scale` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `scale_key` VARCHAR(32) NOT NULL COMMENT '量表唯一标识(如gad-7)',
    `title` VARCHAR(64) NOT NULL COMMENT '量表标题',
    `cover_url` VARCHAR(512) DEFAULT NULL COMMENT '量表封面图片URL',
    `description` VARCHAR(512) DEFAULT NULL COMMENT '量表描述/引导语',
    `scoring_rules` JSON DEFAULT NULL COMMENT '评分规则JSON数组: [{"min":0,"max":5,"level":"正常","desc":"建议..."}]',
    `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态: active上架, inactive下架',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_scale_key` (`scale_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='心理量表定义表';

-- 量表题目
DROP TABLE IF EXISTS `assessment_question`;
CREATE TABLE `assessment_question` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `scale_key` VARCHAR(32) NOT NULL COMMENT '所属量表Key',
    `question_text` TEXT NOT NULL COMMENT '题目内容',
    `options` JSON NOT NULL COMMENT '选项配置JSON',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    PRIMARY KEY (`id`),
    KEY `idx_scale_key` (`scale_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='量表题目表';

-- 测评记录
DROP TABLE IF EXISTS `assessment_record`;
CREATE TABLE `assessment_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `scale_key` VARCHAR(32) NOT NULL COMMENT '量表Key',
    `total_score` INT NOT NULL COMMENT '总分',
    `result_level` VARCHAR(64) COMMENT '结果等级',
    `result_desc` TEXT COMMENT '结果建议',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '测评时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_scale` (`user_id`, `scale_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测评记录主表';

-- 测评答案详情
DROP TABLE IF EXISTS `assessment_answer`;
CREATE TABLE `assessment_answer` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `record_id` BIGINT NOT NULL COMMENT '关联记录ID',
    `question_id` BIGINT NOT NULL COMMENT '题目ID',
    `score` INT NOT NULL COMMENT '得分',
    `answer_text` VARCHAR(255) COMMENT '选项快照',
    PRIMARY KEY (`id`),
    KEY `idx_record_id` (`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测评详细答案表';

-- ==========================================
-- 4. AI 咨询模块
-- ==========================================

-- 会话列表
DROP TABLE IF EXISTS `chat_session`;
CREATE TABLE `chat_session` (
    `id` VARCHAR(64) NOT NULL COMMENT '会话ID (UUID)',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `title` VARCHAR(128) COMMENT '会话标题',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_time` (`user_id`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI会话列表';

-- 消息记录
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID',
    `sender` VARCHAR(20) NOT NULL COMMENT 'user 或 AI',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_session_time` (`session_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI聊天记录表';

-- ==========================================
-- 5. 预约管理模块
-- ==========================================

-- 预约订单
DROP TABLE IF EXISTS `appointment`;
CREATE TABLE `appointment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '预约用户ID',
    `counselor_id` BIGINT NOT NULL COMMENT '咨询师ID',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME NOT NULL COMMENT '结束时间',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING, CONFIRMED, CANCELLED, COMPLETED',
    `user_note` VARCHAR(512) COMMENT '用户备注',
    `cancel_reason` VARCHAR(255) COMMENT '取消原因',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_counselor_date` (`counselor_id`, `start_time`),
    KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约订单表';

-- 咨询评价
DROP TABLE IF EXISTS `counselor_review`;
CREATE TABLE `counselor_review` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `appointment_id` BIGINT NOT NULL COMMENT '关联预约ID',
    `counselor_id` BIGINT NOT NULL COMMENT '咨询师ID',
    `user_id` BIGINT NOT NULL COMMENT '评价人ID',
    `rating` INT NOT NULL COMMENT '评分1-5',
    `content` TEXT COMMENT '评价内容',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_counselor_id` (`counselor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='咨询评价表';
```



INSERT INTO sys_user (
    username, 
    password, 
    nickname, 
    role, 
    status, 
    create_time
) 
VALUES (
    'admin',
    'e10adc3949ba59abbe56e057f20f883e',
    '系统管理员',
    'admin',
    1,
    NOW()
);