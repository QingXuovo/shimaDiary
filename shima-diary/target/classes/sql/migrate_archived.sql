-- =============================================
-- 迁移脚本：为diary表添加archived和category_id字段
-- 使用方法：在Navicat中针对shima_diary数据库执行此脚本
-- =============================================

USE shima_diary;

-- 添加category_id字段（如果不存在）
ALTER TABLE `diary` 
ADD COLUMN IF NOT EXISTS `category_id` BIGINT DEFAULT NULL COMMENT '分类ID' AFTER `user_id`;

-- 添加archived字段（如果不存在）
ALTER TABLE `diary` 
ADD COLUMN IF NOT EXISTS `archived` TINYINT DEFAULT 0 COMMENT '归档状态(0-未归档,1-已归档)' AFTER `deleted`;

-- 添加索引（如果不存在）
ALTER TABLE `diary` 
ADD INDEX IF NOT EXISTS `idx_category_id` (`category_id`);

-- 执行完成提示
SELECT '✅ diary表字段迁移完成！' as result;