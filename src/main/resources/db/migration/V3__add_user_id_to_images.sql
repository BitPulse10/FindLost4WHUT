-- 为 images 表添加 user_id 字段（如果不存在）
SET @sql_add_user_id = (
    SELECT CONCAT('ALTER TABLE images ADD COLUMN user_id BIGINT NULL COMMENT ''上传用户ID''')
    WHERE NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'images'
          AND COLUMN_NAME = 'user_id'
    )
    LIMIT 1
);
SET @sql_add_user_id = IFNULL(@sql_add_user_id, 'SELECT 1');
PREPARE stmt_add_user_id FROM @sql_add_user_id;
EXECUTE stmt_add_user_id;
DEALLOCATE PREPARE stmt_add_user_id;

-- 为 images.user_id 添加索引（如果不存在）
SET @sql_add_index = (
    SELECT 'CREATE INDEX idx_images_user_id ON images(user_id)'
    WHERE NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'images'
          AND INDEX_NAME = 'idx_images_user_id'
    )
    LIMIT 1
);
SET @sql_add_index = IFNULL(@sql_add_index, 'SELECT 1');
PREPARE stmt_add_index FROM @sql_add_index;
EXECUTE stmt_add_index;
DEALLOCATE PREPARE stmt_add_index;
