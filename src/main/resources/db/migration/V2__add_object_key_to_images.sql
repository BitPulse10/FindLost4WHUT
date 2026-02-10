-- 添加 object_key 列到 images 表（如果不存在）
-- 使用 PREPARE 和 EXECUTE 动态执行 SQL
SET @sql = (
    SELECT CONCAT('ALTER TABLE images ADD COLUMN object_key VARCHAR(500) UNIQUE NOT NULL COMMENT ''图片对象键（唯一）''')
    WHERE NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'images'
        AND COLUMN_NAME = 'object_key'
    )
    LIMIT 1
);
SET @sql = IFNULL(@sql, 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;