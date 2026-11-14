-- ============================================================================
-- 39-商城商品销量偏移与猜你喜欢表.sql
-- 来源：sql/mysql/update.sql「第 12 段」（2026-10-02 商城猜你喜欢 / 推荐好物增量）
-- 编号说明：37 已被《37-商城分类加禁用列.sql》占用，本条顺延为 39
-- ============================================================================
-- 批次：2026-09-29/30「商城分类表格改造」＋ 2026-10-02「猜你喜欢 / 推荐好物」
-- 配套代码：siam-server 正本 siam-system
--   · 推荐好物：PointsMallGoods 实体加 salesOffset、Mapper(.java/.xml) 补排序
--     （ORDER BY total_sales + IFNULL(sales_offset,0) DESC，见 §2.1-11）
--   · 猜你喜欢：GuessYouLike 实体／GuessYouLikeMapper（4 端点，见 §2.1-9~12）
-- 前置备份：本地已备份表结构
--   order-project/01-solution/local-run/backup/2026-09-30-tb_points_mall_menu-schema-pre-37.sql
--   上云前须对生产库另做一次备份（执行器 apply_39_ddl.py 会自动备份受影响表）
-- 状态：⏳ 本地 siam_db 已执行（10-01 写入第 12 段）；【云端尚未执行】
-- ============================================================================

-- ▼▼▼ 生产执行区 ▼▼▼
-- 39.1）商城商品加销量偏移列（幂等：列已存在则跳过）
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'tb_points_mall_goods'
             AND COLUMN_NAME = 'sales_offset');
SET @ddl = IF(@c = 0,
  'ALTER TABLE `tb_points_mall_goods` ADD `sales_offset` int(11) DEFAULT NULL
     COMMENT ''销量偏移量(展示销量=累计销量+偏移)'' AFTER `total_sales`',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 39.2）猜你喜欢关联表（IF NOT EXISTS 幂等）
CREATE TABLE IF NOT EXISTS `tb_guess_you_like` (
  `id`           int(11)      NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `shop_id`      int(11)               DEFAULT NULL COMMENT '店铺id(NULL=平台级挑选)',
  `goods_id`     int(11)      NOT NULL COMMENT '商城商品id(tb_points_mall_goods.id)',
  `sort_number`  int(11)      NOT NULL DEFAULT 0 COMMENT '排序号(升序展示)',
  `create_time`  datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_shop_goods` (`shop_id`,`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商城猜你喜欢关联表';

-- 说明：
-- · 回滚：ALTER TABLE tb_points_mall_goods DROP COLUMN sales_offset;
--          DROP TABLE tb_guess_you_like;
--   （回滚前须先把配套后端 bar-mvp-045 一并回滚，否则排序查询 Unknown column → 500）
-- · 幂等重跑安全：39.1 判列存在即跳过；39.2 为 IF NOT EXISTS。
