-- 从 images 表删除 user_id 列（如果存在）
SET @sql_drop_user_id_images = (
    SELECT CONCAT('ALTER TABLE images DROP COLUMN user_id')
    WHERE EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'images'
          AND COLUMN_NAME = 'user_id'
    )
    LIMIT 1
);
SET @sql_drop_user_id_images = IFNULL(@sql_drop_user_id_images, 'SELECT 1');
PREPARE stmt_drop_user_id_images FROM @sql_drop_user_id_images;
EXECUTE stmt_drop_user_id_images;
DEALLOCATE PREPARE stmt_drop_user_id_images;

-- 从 image_search 表删除 user_id 列（如果存在）
SET @sql_drop_user_id_image_search = (
    SELECT CONCAT('ALTER TABLE image_search DROP COLUMN user_id')
    WHERE EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'image_search'
          AND COLUMN_NAME = 'user_id'
    )
    LIMIT 1
);
SET @sql_drop_user_id_image_search = IFNULL(@sql_drop_user_id_image_search, 'SELECT 1');
PREPARE stmt_drop_user_id_image_search FROM @sql_drop_user_id_image_search;
EXECUTE stmt_drop_user_id_image_search;
DEALLOCATE PREPARE stmt_drop_user_id_image_search;
