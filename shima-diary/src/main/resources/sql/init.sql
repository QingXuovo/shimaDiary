-- =============================================
-- 拾码日记 - Navicat 可执行 SQL 脚本
-- 功能：日志记录、打卡系统、任务清单
-- 使用方法：在 Navicat 中新建查询，复制此脚本执行
-- =============================================

-- 1. 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS shima_diary 
DEFAULT CHARACTER SET utf8mb4 
DEFAULT COLLATE utf8mb4_general_ci;

-- 2. 使用数据库
USE shima_diary;

-- =============================================
-- 3. 创建表结构
-- =============================================

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '密码(MD5加密)',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `role` VARCHAR(20) DEFAULT 'user' COMMENT '角色(admin/user)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除(0-未删除,1-已删除)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 日记表
CREATE TABLE IF NOT EXISTS `diary` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `title` VARCHAR(100) NOT NULL COMMENT '日记标题',
    `content` TEXT NOT NULL COMMENT '日记内容',
    `mood` VARCHAR(20) DEFAULT 'normal' COMMENT '心情标签(happy/sad/normal/excited/tired)',
    `weather` VARCHAR(20) DEFAULT 'sunny' COMMENT '天气(sunny/cloudy/rainy/snowy)',
    `location` VARCHAR(100) DEFAULT NULL COMMENT '地点',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT '标签(逗号分隔)',
    `diary_date` DATE NOT NULL COMMENT '日记日期',
    `user_id` BIGINT DEFAULT 1 COMMENT '用户ID',
    `category_id` BIGINT DEFAULT NULL COMMENT '分类ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除(0-未删除,1-已删除)',
    `archived` TINYINT DEFAULT 0 COMMENT '归档状态(0-未归档,1-已归档)',
    PRIMARY KEY (`id`),
    KEY `idx_user_date` (`user_id`, `diary_date`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日记表';

-- 打卡记录表
CREATE TABLE IF NOT EXISTS `check_in` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `check_date` DATE NOT NULL COMMENT '打卡日期',
    `check_type` VARCHAR(50) NOT NULL COMMENT '打卡类型',
    `check_name` VARCHAR(100) NOT NULL COMMENT '打卡项目名称',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '打卡描述',
    `duration` INT DEFAULT 0 COMMENT '持续时长(分钟)',
    `score` INT DEFAULT 5 COMMENT '完成评分(1-10)',
    `user_id` BIGINT DEFAULT 1 COMMENT '用户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除(0-未删除,1-已删除)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_date_type` (`user_id`, `check_date`, `check_type`),
    KEY `idx_check_date` (`check_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打卡记录表';

-- 打卡项目配置表
CREATE TABLE IF NOT EXISTS `check_in_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `check_type` VARCHAR(50) NOT NULL COMMENT '打卡类型',
    `check_name` VARCHAR(100) NOT NULL COMMENT '打卡项目名称',
    `icon` VARCHAR(50) DEFAULT 'star' COMMENT '图标标识',
    `color` VARCHAR(20) DEFAULT '#667eea' COMMENT '颜色',
    `target_days` INT DEFAULT 30 COMMENT '目标天数',
    `is_active` TINYINT DEFAULT 1 COMMENT '是否启用(0-否,1-是)',
    `user_id` BIGINT DEFAULT 1 COMMENT '用户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_type` (`user_id`, `check_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打卡项目配置表';

-- 任务清单表
CREATE TABLE IF NOT EXISTS `task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `title` VARCHAR(200) NOT NULL COMMENT '任务标题',
    `description` TEXT DEFAULT NULL COMMENT '任务描述',
    `priority` TINYINT DEFAULT 2 COMMENT '优先级(1-高,2-中,3-低)',
    `status` TINYINT DEFAULT 0 COMMENT '状态(0-待办,1-进行中,2-已完成,3-已取消)',
    `category` VARCHAR(50) DEFAULT 'default' COMMENT '分类',
    `due_date` DATE DEFAULT NULL COMMENT '截止日期',
    `reminder_time` DATETIME DEFAULT NULL COMMENT '提醒时间',
    `estimated_time` INT DEFAULT 0 COMMENT '预计耗时(分钟)',
    `actual_time` INT DEFAULT 0 COMMENT '实际耗时(分钟)',
    `progress` INT DEFAULT 0 COMMENT '进度(0-100)',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父任务ID',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `user_id` BIGINT DEFAULT 1 COMMENT '用户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `complete_time` DATETIME DEFAULT NULL COMMENT '完成时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除(0-未删除,1-已删除)',
    PRIMARY KEY (`id`),
    KEY `idx_user_status` (`user_id`, `status`),
    KEY `idx_due_date` (`due_date`),
    KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务清单表';

-- 任务标签表
CREATE TABLE IF NOT EXISTS `task_tag` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `task_id` BIGINT NOT NULL COMMENT '任务ID',
    `tag_name` VARCHAR(50) NOT NULL COMMENT '标签名称',
    `tag_color` VARCHAR(20) DEFAULT '#667eea' COMMENT '标签颜色',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务标签表';

-- 日记分类表
CREATE TABLE IF NOT EXISTS `category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `icon` VARCHAR(50) DEFAULT 'folder' COMMENT '图标标识',
    `color` VARCHAR(20) DEFAULT '#667eea' COMMENT '分类颜色',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '分类描述',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除(0-未删除,1-已删除)',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日记分类表';

-- 日记分享表
CREATE TABLE IF NOT EXISTS `diary_share` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `diary_id` BIGINT NOT NULL COMMENT '日记ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `share_token` VARCHAR(64) NOT NULL COMMENT '分享令牌',
    `expire_time` DATETIME NOT NULL COMMENT '过期时间',
    `view_count` INT DEFAULT 0 COMMENT '浏览次数',
    `is_active` TINYINT DEFAULT 1 COMMENT '是否有效(0-无效,1-有效)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_share_token` (`share_token`),
    KEY `idx_diary_id` (`diary_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日记分享表';

-- 好友关系表
CREATE TABLE IF NOT EXISTS `friend` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `friend_id` BIGINT NOT NULL COMMENT '好友ID',
    `status` TINYINT DEFAULT 0 COMMENT '状态(0-待验证,1-已验证,2-已拒绝)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_friend` (`user_id`, `friend_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友关系表';

-- =============================================
-- 4. 插入初始化数据
-- =============================================

-- 初始化默认用户 (密码: 123456 的MD5值)
INSERT IGNORE INTO `user` (`username`, `password`, `nickname`) VALUES
('admin', 'e10adc3949ba59abbe56e057f20f883e', '管理员'),
('user', 'e10adc3949ba59abbe56e057f20f883e', '普通用户');

-- 初始化打卡项目配置
INSERT IGNORE INTO `check_in_config` (`check_type`, `check_name`, `icon`, `color`, `target_days`) VALUES
('study', '学习打卡', 'book', '#409EFF', 30),
('exercise', '运动打卡', 'run', '#67C23A', 30),
('reading', '阅读打卡', 'read', '#E6A23C', 30),
('meditation', '冥想打卡', 'meditation', '#909399', 21),
('early_rise', '早起打卡', 'sun', '#F56C6C', 30);

-- 初始化示例日记
INSERT IGNORE INTO `diary` (`title`, `content`, `mood`, `weather`, `tags`, `diary_date`) VALUES
('项目启动', '今天开始拾码日记项目的开发，使用SpringBoot+MyBatis技术栈，希望能做出一个好用的日记应用。', 'excited', 'sunny', '工作,开发', CURDATE()),
('学习笔记', '学习了MyBatis Plus的使用，感觉比原生MyBatis方便很多，特别是分页和条件构造器。', 'happy', 'cloudy', '学习,技术', CURDATE());

-- 初始化示例任务
INSERT IGNORE INTO `task` (`title`, `description`, `priority`, `category`, `due_date`) VALUES
('完成日记模块开发', '实现日记的增删改查功能', 1, '开发', DATE_ADD(CURDATE(), INTERVAL 3 DAY)),
('完成打卡模块开发', '实现打卡记录和统计功能', 1, '开发', DATE_ADD(CURDATE(), INTERVAL 5 DAY)),
('完成任务清单模块开发', '实现任务的CRUD和状态管理', 2, '开发', DATE_ADD(CURDATE(), INTERVAL 7 DAY));

-- =============================================
-- 5. 创建视图（可选）
-- =============================================
DROP VIEW IF EXISTS `v_check_in_stats`;
CREATE VIEW `v_check_in_stats` AS
SELECT 
    user_id,
    check_type,
    check_name,
    COUNT(*) as total_days,
    MIN(check_date) as start_date,
    MAX(check_date) as last_date,
    DATEDIFF(MAX(check_date), MIN(check_date)) + 1 as span_days,
    ROUND(AVG(score), 1) as avg_score
FROM check_in
WHERE deleted = 0
GROUP BY user_id, check_type, check_name;

-- =============================================
-- 执行完成提示
-- =============================================
SELECT '✅ 拾码日记数据库初始化完成！' as result;
