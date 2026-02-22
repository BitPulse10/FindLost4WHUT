-- 新增图搜临时图片表（如不存在）
CREATE TABLE IF NOT EXISTS image_search (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    url VARCHAR(500) NOT NULL COMMENT '图片访问URL',
    object_key VARCHAR(500) UNIQUE NOT NULL COMMENT '图片对象键（唯一）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    expire_time DATETIME NOT NULL COMMENT '过期时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图搜临时图片表';

-- 兼容已存在旧表缺少 expire_time 的场景：按需补列
SET @sql_add_expire_time = (
    SELECT CONCAT('ALTER TABLE image_search ADD COLUMN expire_time DATETIME NOT NULL COMMENT ''过期时间''')
    WHERE NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'image_search'
          AND COLUMN_NAME = 'expire_time'
    )
    LIMIT 1
);
SET @sql_add_expire_time = IFNULL(@sql_add_expire_time, 'SELECT 1');
PREPARE stmt_add_expire_time FROM @sql_add_expire_time;
EXECUTE stmt_add_expire_time;
DEALLOCATE PREPARE stmt_add_expire_time;

-- 创建过期时间索引（如不存在）
SET @sql_add_idx_expire = (
    SELECT 'CREATE INDEX idx_image_search_expire_time ON image_search(expire_time)'
    WHERE NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'image_search'
          AND INDEX_NAME = 'idx_image_search_expire_time'
    )
    LIMIT 1
);
SET @sql_add_idx_expire = IFNULL(@sql_add_idx_expire, 'SELECT 1');
PREPARE stmt_add_idx_expire FROM @sql_add_idx_expire;
EXECUTE stmt_add_idx_expire;
DEALLOCATE PREPARE stmt_add_idx_expire;
