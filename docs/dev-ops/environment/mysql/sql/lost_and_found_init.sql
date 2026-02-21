
-- ************************************************************
-- Lost_And_Find SQL Schema Initialization (No Snowflake IDs)
-- Version: 1.0
-- Compatible with Docker container first-run setup
-- ************************************************************

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
SET NAMES utf8mb4;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE='NO_AUTO_VALUE_ON_ZERO', SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

CREATE DATABASE IF NOT EXISTS `Lost_And_Find` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `Lost_And_Find`;

-- ----------------------
-- 用户表
-- ----------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
  email VARCHAR(255) UNIQUE NOT NULL COMMENT '用户邮箱',
  password_hash VARCHAR(255) NOT NULL COMMENT '加密后的密码',
  nickname VARCHAR(50) COMMENT '用户昵称',
  status TINYINT DEFAULT 0 COMMENT '用户状态：0-正常，1-封禁，2-注销',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ----------------------
-- 统一物品表
-- ----------------------
DROP TABLE IF EXISTS `items`;
CREATE TABLE items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
  user_id BIGINT NOT NULL COMMENT '发布用户的数据库主键ID',
  type TINYINT NOT NULL COMMENT '物品类型：0-挂失（丢失），1-招领（捡到），2-卡证',
  event_time DATETIME COMMENT '事件发生时间（丢失或捡到时间）',
  event_place VARCHAR(255) COMMENT '事件发生地点（丢失或捡到地点）',
  status TINYINT DEFAULT 0 COMMENT '物品状态：0-有效，1-结束',
  is_deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
  description TEXT CHECK (CHAR_LENGTH(description) <= 1000) COMMENT '物品描述信息（限制1000字）',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物品表';

-- ----------------------
-- 标签表
-- ----------------------
DROP TABLE IF EXISTS `tags`;
CREATE TABLE tags (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
  name VARCHAR(50) UNIQUE NOT NULL COMMENT '标签名称，唯一'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签表';

-- ----------------------
-- 物品标签关联表
-- ----------------------
DROP TABLE IF EXISTS `item_tags`;
CREATE TABLE item_tags (
  item_id BIGINT NOT NULL COMMENT '物品ID',
  tag_id BIGINT NOT NULL COMMENT '标签ID',
  PRIMARY KEY (item_id, tag_id),
  FOREIGN KEY (item_id) REFERENCES items(id),
  FOREIGN KEY (tag_id) REFERENCES tags(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物品-标签关联表';

-- ----------------------
-- 图片表
-- ----------------------
DROP TABLE IF EXISTS `images`;
CREATE TABLE images (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
  object_key VARCHAR(500) UNIQUE NOT NULL COMMENT '图片对象键（唯一）',
  url VARCHAR(500) NOT NULL COMMENT '图片访问URL'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图片表';

-- ----------------------
-- 图搜临时图片表
-- ----------------------
DROP TABLE IF EXISTS `image_search`;
CREATE TABLE image_search (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
  url VARCHAR(500) NOT NULL COMMENT '图片访问URL',
  object_key VARCHAR(500) UNIQUE NOT NULL COMMENT '图片对象键（唯一）',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  expire_time DATETIME NOT NULL COMMENT '过期时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图搜临时图片表';

-- ----------------------
-- 物品图片关联表
-- ----------------------
DROP TABLE IF EXISTS `item_images`;
CREATE TABLE item_images (
  item_id BIGINT NOT NULL COMMENT '物品ID',
  image_id BIGINT NOT NULL COMMENT '图片ID',
  PRIMARY KEY (item_id, image_id),
  FOREIGN KEY (item_id) REFERENCES items(id),
  FOREIGN KEY (image_id) REFERENCES images(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物品-图片关联表';

-- ----------------------
-- 用户收藏表
-- ----------------------
DROP TABLE IF EXISTS `user_collect`;
CREATE TABLE user_collect (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '唯一标识',
  user_id BIGINT NOT NULL COMMENT '关联用户ID（外键）',
  item_id BIGINT NOT NULL COMMENT '关联物品ID（外键）',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间（自动生成）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_item (user_id, item_id),
  CONSTRAINT fk_collect_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_collect_item FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户物品收藏关联表';

-- ----------------------
-- 索引补充
-- ----------------------
CREATE INDEX idx_items_type_status_event_time ON items(type, status, event_time);
CREATE INDEX idx_items_is_deleted_status ON items(is_deleted, status);
CREATE INDEX idx_item_tags_item_id ON item_tags(item_id);
CREATE INDEX idx_item_tags_tag_id ON item_tags(tag_id);
CREATE INDEX idx_item_tags_tag_item ON item_tags(tag_id, item_id);
CREATE INDEX idx_item_images_item_id ON item_images(item_id);
CREATE INDEX idx_item_images_image_id ON item_images(image_id);
CREATE INDEX idx_image_search_expire_time ON image_search(expire_time);
CREATE INDEX idx_users_email ON users(email);

INSERT INTO users (email, password_hash, nickname, status)
VALUES
    ('user01@whut.edu.cn', '$2a$10$7EqJtq98hPqEX7fNZaFWoOeS2S3hYjzQ6lQq8KcQ5kYy1nE1M8y2m', '拾物同学01', 0),
    ('user02@whut.edu.cn', '$2a$10$7EqJtq98hPqEX7fNZaFWoOeS2S3hYjzQ6lQq8KcQ5kYy1nE1M8y2m', '拾物同学02', 0),
    ('user03@whut.edu.cn', '$2a$10$7EqJtq98hPqEX7fNZaFWoOeS2S3hYjzQ6lQq8KcQ5kYy1nE1M8y2m', '拾物同学03', 0),
    ('user04@whut.edu.cn', '$2a$10$7EqJtq98hPqEX7fNZaFWoOeS2S3hYjzQ6lQq8KcQ5kYy1nE1M8y2m', '拾物同学04', 0),
    ('user05@whut.edu.cn', '$2a$10$7EqJtq98hPqEX7fNZaFWoOeS2S3hYjzQ6lQq8KcQ5kYy1nE1M8y2m', '拾物同学05', 0),
    ('user06@whut.edu.cn', '$2a$10$7EqJtq98hPqEX7fNZaFWoOeS2S3hYjzQ6lQq8KcQ5kYy1nE1M8y2m', '拾物同学06', 0),
    ('user07@whut.edu.cn', '$2a$10$7EqJtq98hPqEX7fNZaFWoOeS2S3hYjzQ6lQq8KcQ5kYy1nE1M8y2m', '拾物同学07', 0),
    ('user08@whut.edu.cn', '$2a$10$7EqJtq98hPqEX7fNZaFWoOeS2S3hYjzQ6lQq8KcQ5kYy1nE1M8y2m', '拾物同学08', 0),
    ('user09@whut.edu.cn', '$2a$10$7EqJtq98hPqEX7fNZaFWoOeS2S3hYjzQ6lQq8KcQ5kYy1nE1M8y2m', '拾物同学09', 0),
    ('user10@whut.edu.cn', '$2a$10$7EqJtq98hPqEX7fNZaFWoOeS2S3hYjzQ6lQq8KcQ5kYy1nE1M8y2m', '拾物同学10', 0);


-- 先清空 items 表（可选，避免重复插入）
-- TRUNCATE TABLE items;

-- 批量插入 100 条物品数据，关联已存在的用户 ID
INSERT INTO items (
    user_id, type, event_time, event_place, status, is_deleted, description, created_at, updated_at
)
WITH RECURSIVE seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 100
),
-- 临时表：获取 users 表的 ID 并编号（1-10）
               user_ids AS (
                   SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS row_num
                   FROM users
                   LIMIT 10 -- 只取前 10 个用户
               )
SELECT
    -- 按行号匹配，确保 user_id 是 users 表中真实存在的 ID
    (SELECT id FROM user_ids WHERE row_num = ((n - 1) % 10) + 1) AS user_id,
    CASE
        WHEN n % 10 IN (0, 5, 9) THEN 2
        ELSE n % 2
    END AS type, -- 0/1/2 区分挂失/招领/卡证
    DATE_SUB(NOW(), INTERVAL (n * 3) HOUR) AS event_time,
    -- 随机取 8 个地点
    ELT(((n - 1) % 8) + 1, '南湖食堂', '鉴湖主楼', '图书馆', '西院操场', '梅苑宿舍', '东院教学楼', '北门快递点', '校医院') AS event_place,
    0 AS status,
    0 AS is_deleted,
    -- 拼接描述信息
    CONCAT(
            CASE
                WHEN n % 10 IN (0, 5, 9) THEN '卡证：'
                WHEN n % 2 = 0 THEN '挂失：'
                ELSE '招领：'
            END,
            ELT(((n - 1) % 10) + 1, '黑色双肩包', '校园卡', '银色水杯', '蓝牙耳机', '机械键盘', '雨伞', 'U盘', '眼镜盒', '白色充电器', '笔记本电脑'),
            '，编号#', LPAD(n, 3, '0'),
            '，请失主/拾主尽快联系。'
    ) AS description,
    DATE_SUB(NOW(), INTERVAL (n * 3 - 1) HOUR) AS created_at,
    DATE_SUB(NOW(), INTERVAL (n * 3 - 1) HOUR) AS updated_at
FROM seq;

-- ----------------------
-- 测试种子数据：随机100个标签
-- ----------------------
INSERT INTO tags (name)
WITH RECURSIVE seq_tag AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM seq_tag WHERE n < 100
)
SELECT CONCAT(
               ELT(((n - 1) % 12) + 1, '黑色', '白色', '蓝色', '红色', '防水', '轻便', '金属', '塑料', '电子', '证件', '学习', '生活'),
               '-',
               ELT(((n - 1) % 10) + 1, '背包', '钥匙', '耳机', '水杯', '雨伞', '校园卡', '眼镜', '充电器', 'U盘', '外套'),
               '-',
               LPAD(n, 3, '0')
       ) AS name
FROM seq_tag;

INSERT IGNORE INTO item_tags (item_id, tag_id)
SELECT i.id, ((i.id - 1) % 100) + 1
FROM items i
UNION DISTINCT
SELECT i.id, ((i.id + 16) % 100) + 1
FROM items i
UNION DISTINCT
SELECT i.id, ((i.id + 42) % 100) + 1
FROM items i;



/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
