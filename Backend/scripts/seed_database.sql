-- ============================================
-- MindEase 数据库种子数据
-- ================================
-- 用途: 为开发/测试环境提供基础参考数据
-- 使用方式: mysql -u root -p mindease_db < seed_database.sql
-- 注意: 此 SQL 会先清理再插入，生产环境禁止执行!
-- ============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------------------
-- 1. 清理旧数据（仅限测试环境!）
-- ----------------------------------------

DROP TABLE IF EXISTS `chat_message`;
DROP TABLE IF EXISTS `chat_session`;
DROP TABLE IF EXISTS `appointment`;
DROP TABLE IF EXISTS `assessment_record`;
DROP TABLE IF EXISTS `mood_log`;
DROP TABLE IF EXISTS `counselor_profile`;
DROP TABLE IF EXISTS `sys_notification`;
DROP TABLE IF EXISTS `user`;

-- ----------------------------------------
-- 2. 建表语句 (与项目实体一致)
-- ----------------------------------------

CREATE TABLE `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username`    VARCHAR(64)  NOT NULL COMMENT '用户名',
    `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `password`    VARCHAR(255) NOT NULL COMMENT '密码(加密)',
    `role`        VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色: USER/COUNSELOR/ADMIN',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 1正常 0禁用',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE `counselor_profile` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT,
    `counselor_id`    BIGINT        NOT NULL COMMENT '关联用户ID',
    `real_name`       VARCHAR(64)   DEFAULT NULL COMMENT '真实姓名',
    `title`           VARCHAR(100)  DEFAULT NULL COMMENT '职称',
    `specialty`       JSON          DEFAULT NULL COMMENT '专长标签(JSON数组)',
    `price_per_hour`  DECIMAL(10,2) DEFAULT NULL COMMENT '每小时价格(元)',
    `rating`          DECIMAL(2,1)  DEFAULT 5.0 COMMENT '评分(1.0-5.0)',
    `bio`             TEXT          DEFAULT NULL COMMENT '个人简介',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_counselor_id` (`counselor_id`),
    KEY `idx_rating` (`rating`),
    KEY `idx_price` (`price_per_hour`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='咨询师档案表';

CREATE TABLE `mood_log` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT       NOT NULL COMMENT '用户ID',
    `mood_type`   VARCHAR(32)  NOT NULL COMMENT '心情类型: happy/sad/anxious等',
    `mood_score`  TINYINT      NOT NULL DEFAULT 5 COMMENT '心情评分(1-10)',
    `tags`        JSON         DEFAULT NULL COMMENT '标签(JSON数组)',
    `content`     TEXT         DEFAULT NULL COMMENT '日志内容',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_mood_type` (`mood_type`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='心情日志表';

CREATE TABLE `appointment` (
    `id`              BIGINT    NOT NULL AUTO_INCREMENT,
    `user_id`         BIGINT    NOT NULL COMMENT '来访用户ID',
    `counselor_id`    BIGINT    NOT NULL COMMENT '咨询师用户ID',
    `start_time`      DATETIME  NOT NULL COMMENT '开始时间',
    `end_time`        DATETIME  NOT NULL COMMENT '结束时间',
    `status`          VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/CONFIRMED/COMPLETED/CANCELLED',
    `user_note`       TEXT     DEFAULT NULL COMMENT '用户备注',
    `counselor_note`  TEXT     DEFAULT NULL COMMENT '咨询师备注',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_counselor_id` (`counselor_id`),
    KEY `idx_status` (`status`),
    KEY `idx_start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预约表';

CREATE TABLE `assessment_record` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`        BIGINT       NOT NULL COMMENT '用户ID',
    `scale_key`      VARCHAR(32)  NOT NULL COMMENT '量表标识: PHQ-9/GAD-7等',
    `total_score`    INT          NOT NULL DEFAULT 0 COMMENT '总分',
    `result_level`   VARCHAR(64)  DEFAULT NULL COMMENT '结果等级',
    `answers`        JSON         DEFAULT NULL COMMENT '详细作答(JSON数组)',
    `recommendation` TEXT         DEFAULT NULL COMMENT '建议内容',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_scale_key` (`scale_key`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评估记录表';

CREATE TABLE `chat_session` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `session_title` VARCHAR(200) DEFAULT NULL COMMENT '会话标题',
    `user_id`       BIGINT       NOT NULL COMMENT '用户ID',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天会话表';

CREATE TABLE `chat_message` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `session_id`    BIGINT       NOT NULL COMMENT '会话ID',
    `message_role`  VARCHAR(20)  NOT NULL COMMENT '角色: USER/ASSISTANT',
    `content`       TEXT         NOT NULL COMMENT '消息内容',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息表';

CREATE TABLE `sys_notification` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT       NOT NULL COMMENT '用户ID',
    `type`        VARCHAR(32)  NOT NULL DEFAULT 'SYSTEM' COMMENT '类型: APPOINTMENT/SYSTEM',
    `title`       VARCHAR(200) NOT NULL COMMENT '通知标题',
    `content`     TEXT         DEFAULT NULL COMMENT '通知内容',
    `is_read`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读: 0未读 1已读',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_is_read` (`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统通知表';

-- ----------------------------------------
-- 3. 插入种子数据
-- ----------------------------------------

-- 3.1 管理员账号
INSERT INTO `user` (`username`, `phone`, `password`, `role`, `status`, `create_time`) VALUES
('admin', '13800000000', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'ADMIN', 1, NOW());

-- 3.2 测试用户账号 (密码均为: Test123456)
INSERT INTO `user` (`username`, `phone`, `password`, `role`, `status`, `create_time`) VALUES
('test_user',   '13800001001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'USER',      1, NOW()),
('demo_user',   '13800001002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'USER',      1, NOW()),
('alice',       '13800001003', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'USER',      1, NOW()),
('bob',         '13800001004', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'USER',      1, NOW()),
('carol',       '13800001005', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'USER',      1, NOW());

-- 3.3 咨询师账号
INSERT INTO `user` (`username`, `phone`, `password`, `role`, `status`, `create_time`) VALUES
('dr_zhang',  '13800002001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'COUNSELOR', 1, NOW()),
('dr_li',     '13800002002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'COUNSELOR', 1, NOW()),
('dr_wang',   '13800002003', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'COUNSELOR', 1, NOW()),
('dr_chen',   '13800002004', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'COUNSELOR', 1, NOW());

-- 3.4 咨询师档案
INSERT INTO `counselor_profile` (`counselor_id`, `real_name`, `title`, `specialty`, `price_per_hour`, `rating`, `bio`) VALUES
((SELECT id FROM user WHERE username = 'dr_zhang'),
 '张明远', '高级心理咨询师', '["认知行为疗法", "抑郁症", "焦虑症"]', 500.00, 4.8,
 '拥有12年临床心理咨询经验，擅长CBT取向的个体治疗。北京大学心理学博士，中国心理学会注册督导师。累计咨询时长超过8000小时。'),

((SELECT id FROM user WHERE username = 'dr_li'),
 '李婷', '临床心理医生', '["家庭治疗", "婚姻咨询", "青少年心理"]', 600.00, 4.9,
 '医学心理学硕士，专注于家庭系统和亲密关系领域。曾在三甲医院心理科工作8年，擅长从系统角度理解并解决关系困扰。'),

((SELECT id FROM user WHERE username = 'dr_wang'),
 '王建国', '资深咨询师', '["创伤后康复", "PTSD", "情绪管理"]', 800.00, 4.7,
 'EMDR认证治疗师，在创伤治疗领域有丰富经验。曾参与灾后心理援助工作，对于复杂PTSD有独到的治疗方案。'),

((SELECT id FROM user WHERE username = 'dr_chen'),
 '陈思雨', '青少年心理专家', '["青少年心理", "学业压力", "家庭关系", "网络成瘾"]', 450.00, 4.9,
 '北京师范大学发展心理学博士，专注于青少年心理健康领域超过8年。擅长处理学业焦虑、亲子冲突、网络依赖等问题，深受学生和家长信赖。');

-- 3.5 示例心情日志
INSERT INTO `mood_log` (`user_id`, `mood_type`, `mood_score`, `tags`, `content`, `create_time`) VALUES
((SELECT id FROM user WHERE username = 'test_user'), 'anxious', 3, '["工作", "压力"]', '明天有重要汇报，有点紧张', DATE_SUB(NOW(), INTERVAL 1 DAY)),
((SELECT id FROM user WHERE username = 'test_user'), 'calm', 7, '["运动", "放松"]', '今天跑步后感觉很舒服', DATE_SUB(NOW(), INTERVAL 2 DAY)),
((SELECT id FROM user WHERE username = 'demo_user'), 'sad', 4, '["人际关系", "孤独"]', '最近和朋友有些疏远了', DATE_SUB(NOW(), INTERVAL 1 DAY)),
((SELECT id FROM user WHERE username = 'alice'), 'happy', 8, '["成就", "工作"]', '项目终于上线了!', DATE_SUB(NOW(), INTERVAL 3 HOUR)),
((SELECT id FROM user WHERE username = 'bob'), 'grateful', 7, '["生活", "感恩"]', '今天收到了朋友的生日祝福，很温暖', DATE_SUB(NOW(), INTERVAL 12 HOUR)),
((SELECT id FROM user WHERE username = 'carol'), 'hopeful', 6, '["成长", "期待']', '虽然最近有些困难，但感觉自己在慢慢变好', DATE_SUB(NOW(), INTERVAL 6 HOUR));

-- 3.6 示例预约
INSERT INTO `appointment` (`user_id`, `counselor_id`, `start_time`, `end_time`, `status`, `user_note`, `counselor_note`) VALUES
((SELECT id FROM user WHERE username = 'test_user'),
 (SELECT id FROM user WHERE username = 'dr_zhang'),
 DATE_ADD(DATE(NOW()), INTERVAL 2 DAY) + INTERVAL 10:00 HOUR_MINUTE,
 DATE_ADD(DATE(NOW()), INTERVAL 2 DAY) + INTERVAL 11:00 HOUR_MINUTE,
 'CONFIRMED', '想咨询关于焦虑的问题', NULL),

((SELECT id FROM user WHERE username = 'demo_user'),
 (SELECT id FROM user WHERE username = 'dr_li'),
 DATE_SUB(NOW(), INTERVAL 5 DAY) + INTERVAL 14:00 HOUR_MINUTE,
 DATE_SUB(NOW(), INTERVAL 5 DAY) + INTERVAL 15:00 HOUR_MINUTE,
 'COMPLETED', '家庭沟通方面有问题', '本次咨询围绕家庭成员间的沟通模式进行了探讨，布置了观察作业。'),

((SELECT id FROM user WHERE username = 'alice'),
 (SELECT id FROM user WHERE username = 'dr_wang'),
 DATE_SUB(NOW(), INTERVAL 1 DAY) + INTERVAL 9:00 HOUR_MINUTE,
 DATE_SUB(NOW(), INTERVAL 1 DAY) + INTERVAL 10:00 HOUR_MINUTE,
 'CANCELLED', '临时有事冲突', NULL);

-- 3.7 示例评估记录
INSERT INTO `assessment_record` (`user_id`, `scale_key`, `total_score`, `result_level`, `answers`, `recommendation`, `create_time`) VALUES
((SELECT id FROM user WHERE username = 'test_user'),
 'PHQ-9', 8, '轻度抑郁',
 '[{"question_index":1,"score":1},{"question_index":2,"score":2},{"question_index":3,"score":0},'
 '{"question_index":4,"score":1},{"question_index":5,"score":0},{"question_index":6,"score":1},'
 '{"question_index":7,"score":2},{"question_index":8,"score":0},{"question_index":9,"score":1}]',
 '建议进行自我调节，包括规律运动、保持社交、充足睡眠。如症状持续超过2周，建议寻求专业帮助。',
 DATE_SUB(NOW(), INTERVAL 3 DAY)),

((SELECT id FROM user WHERE username = 'demo_user'),
 'GAD-7', 12, '中度焦虑',
 '[{"question_index":1,"score":2},{"question_index":2,"score":2},{"question_index":3,"score":1},'
 '{"question_index":4,"score":1},{"question_index":5,"score":2},{"question_index":6,"score":2},'
 '{"question_index":7,"score":2}]',
 '建议咨询心理咨询师，学习认知行为疗法等有效的应对策略。',
 DATE_SUB(NOW(), INTERVAL 7 DAY)),

((SELECT id FROM user WHERE username = 'alice'),
 'PSS-10', 18, '中等压力水平',
 '[{"question_index":1,"score":2},{"question_index":2,"score":1},{"question_index":3,"score":2},'
 '{"question_index":4,"score":2},{"question_index":5,"score":2},{"question_index":6,"score":1},'
 '{"question_index":7,"score":2},{"question_index":8,"score":2},{"question_index":9,"score":2},'
 '{"question_index":10,"score":2}]',
 '当前压力处于中等水平，建议通过运动、正念练习等方式进行压力管理。',
 DATE_SUB(NOW(), INTERVAL 1 DAY));

-- 3.8 示例聊天会话和消息
INSERT INTO `chat_session` (`session_title`, `user_id`, `create_time`) VALUES
('关于近期焦虑情绪的讨论', (SELECT id FROM user WHERE username = 'test_user'), DATE_SUB(NOW(), INTERVAL 3 DAY));

SET @session_id = LAST_INSERT_ID();

INSERT INTO `chat_message` (`session_id`, `message_role`, `content`, `create_time`) VALUES
(@session_id, 'USER', '你好，我最近总是感觉很焦虑，特别是晚上睡不着的时候。', DATE_SUB(NOW(), INTERVAL 3 DAY) + INTERVAL 0 MINUTE),
(@session_id, 'ASSISTANT',
 '感谢你的分享。听起来你正在经历焦虑情绪，这确实不容易。\n\n'
 '首先我想了解一下：这种感觉大概持续多久了？有没有什么特定的事情或情境会让你更焦虑？\n\n'
 '同时，我想和你分享一些可能有用的应对思路：\n'
 '1. **识别触发因素** - 记录下什么时候焦虑感最强\n'
 '2. **呼吸练习** - 尝试4-7-8呼吸法缓解急性焦虑\n'
 '3. **建立睡前仪式** - 帮助改善睡眠质量',
 DATE_SUB(NOW(), INTERVAL 3 DAY) + INTERVAL 1 MINUTE),
(@session_id, 'USER',
 '大概有一个多月了。主要是工作上的压力比较大，最近项目赶进度，经常加班到很晚。一到晚上躺在床上脑子就停不下来。',
 DATE_SUB(NOW(), INTERVAL 3 DAY) + INTERVAL 3 MINUTE),
(@session_id, 'ASSISTANT',
 '我理解你的感受。工作压力大加上睡眠受影响，这确实容易形成恶性循环。\n\n'
 '**分析**：你目前的情况属于典型的"压力-焦虑-失眠"循环，这在现代职场人中很常见。\n\n'
 '**建议方案**：\n'
 '1. **短期（本周）**：\n'
 '   - 设定" worry time"，每天固定15分钟专门处理担忧，其他时间提醒自己"这不是现在要想的"\n'
 '   - 睡前1小时远离电子屏幕\n\n'
 '2. **中期（2-4周）**：\n'
 '   - 练习渐进式肌肉放松\n'
 '   - 和上司沟通工作量，看能否合理分配\n\n'
 '3. **如果持续不缓解**：\n'
 '   - 建议预约一次专业的心理咨询\n'
 '   - 考虑做一个全面的评估（如GAD-7量表）\n\n'
 '你觉得这个计划中有哪些是你愿意先尝试的？',
 DATE_SUB(NOW(), INTERVAL 3 DAY) + INTERVAL 5 MINUTE);

-- 3.9 示例系统通知数据
INSERT INTO `sys_notification` (`user_id`, `type`, `title`, `content`, `is_read`, `create_time`) VALUES
((SELECT id FROM user WHERE username = 'test_user'), 'APPOINTMENT', '预约提醒', '您与张明远老师的咨询将于明天10:00开始，请准时参加。', 0, DATE_ADD(NOW(), INTERVAL 1 DAY) - INTERVAL 6 HOUR),
((SELECT id FROM user WHERE username = 'test_user'), 'SYSTEM', '系统通知', 'MindEase平台已完成升级，新增了心理评估报告导出功能，欢迎体验！', 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
((SELECT id FROM user WHERE username = 'demo_user'), 'ASSESSMENT', '评估提醒', '您已超过14天未进行心理健康评估，建议定期关注自己的心理状态。', 0, NOW()),
((SELECT id FROM user WHERE username = 'alice'), 'REMINDER', '心情记录提醒', '已经连续5天没有记录心情了，记录心情有助于更好地了解自己哦~', 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
((SELECT id FROM user WHERE username = 'bob'), 'COMMUNITY', '社区动态', '您关注的话题"焦虑缓解技巧"有了新的回复，来看看大家的讨论吧。', 0, NOW());

-- ----------------------------------------
-- 4. 验证数据完整性
-- ----------------------------------------

SELECT '-- ========================================' AS '';
SELECT '-- 种子数据导入完成!' AS info;
SELECT CONCAT('-- 用户总数: ', COUNT(*)) AS '' FROM `user`;
SELECT CONCAT('-- 咨询师档案: ', COUNT(*)) AS '' FROM `counselor_profile`;
SELECT CONCAT('-- 心情日志: ', COUNT(*)) AS '' FROM `mood_log`;
SELECT CONCAT('-- 预约记录: ', COUNT(*)) AS '' FROM `appointment`;
SELECT CONCAT('-- 评估记录: ', COUNT(*)) AS '' FROM `assessment_record`;
SELECT CONCAT('-- 聊天会话: ', COUNT(*)) AS '' FROM `chat_session`;
SELECT CONCAT('-- 聊天消息: ', COUNT(*)) AS '' FROM `chat_message`;
SELECT CONCAT('-- 系统通知: ', COUNT(*)) AS '' FROM `sys_notification`;
SELECT '-- 默认账号:' AS '';
SELECT '  admin / Admin123456 (管理员)' AS account UNION ALL
SELECT '  test_user / Test123456 (普通用户)' UNION ALL
SELECT '  demo_user / Test123456 (普通用户)' UNION ALL
SELECT '  alice / Test123456 (普通用户)' UNION ALL
SELECT '  bob / Test123456 (普通用户)' UNION ALL
SELECT '  carol / Test123456 (普通用户)' UNION ALL
SELECT '  dr_zhang / Test123456 (咨询师)' UNION ALL
SELECT '  dr_li / Test123456 (咨询师)' UNION ALL
SELECT '  dr_wang / Test123456 (咨询师)' UNION ALL
SELECT '  dr_chen / Test123456 (咨询师)';
SELECT '-- ========================================' AS '';

SET FOREIGN_KEY_CHECKS = 1;
