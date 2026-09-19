-- ============================================================================
-- 批次 5 结构对齐：tb_invoice_apply.uk_order + tb_wine_storage.location
--                      （附 §9.4 tb_order_detail.remark）
--
-- 为什么需要这个文件：
--   2026-09-19 05:18 有一次**整份 update.sql** 执行（本地库），但它用的是
--   「批次 5 改动前」的 DDL；而 cloudrun/12-批次5与台位-补表.sql 的建表 DDL
--   又是从**本地库 SHOW CREATE TABLE 导出**的 → 把同样两处缺陷一起带给了云库。
--   实测（本地库 84 表）确认两处均未落库：
--     · tb_invoice_apply 仍是 KEY idx_order —— 应为 UNIQUE KEY uk_order
--     · tb_wine_storage 缺 location 列 —— 存酒柜位（C-16）依赖它
--
-- 适用：本地 siam_db 与云托管 siam_db 都可执行
-- 特性：★全幂等（information_schema 守卫），执行失败/重复执行均安全
-- 顺序：云库请在本文件之前先跑完 12-批次5与台位-补表.sql（建表）；本地可直接跑
-- ============================================================================


-- ---------------------------------------------------------------------------
-- 1) tb_invoice_apply：idx_order(普通) → uk_order(唯一)
--    先查重复：若已有订单重复申请记录，ADD UNIQUE 会失败 → 此时只告警不改结构
-- ---------------------------------------------------------------------------
SET @dup = (SELECT COUNT(*) FROM (
              SELECT `order_id` FROM `tb_invoice_apply`
              GROUP BY `order_id` HAVING COUNT(*) > 1
            ) d);
SET @has_uk = (SELECT COUNT(*) FROM information_schema.STATISTICS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_invoice_apply'
                 AND INDEX_NAME = 'uk_order');
SET @has_idx = (SELECT COUNT(*) FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_invoice_apply'
                  AND INDEX_NAME = 'idx_order');
SET @ddl = IF(@dup > 0,
              'SELECT ''SKIP: tb_invoice_apply 已有重复 order_id，请先去重再补唯一索引''',
              IF(@has_uk > 0,
                 'SELECT 1',
                 IF(@has_idx > 0,
                    'ALTER TABLE `tb_invoice_apply` DROP INDEX `idx_order`, ADD UNIQUE KEY `uk_order` (`order_id`)',
                    'ALTER TABLE `tb_invoice_apply` ADD UNIQUE KEY `uk_order` (`order_id`)')));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 2) tb_wine_storage：补 location（存放柜位）
-- ---------------------------------------------------------------------------
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_wine_storage'
            AND COLUMN_NAME = 'location');
SET @ddl = IF(@c = 0,
              'ALTER TABLE `tb_wine_storage` ADD `location` varchar(50) DEFAULT NULL COMMENT ''存放柜位，如 B 区 03 号柜（批次5新增）'' AFTER `storage_code`',
              'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 3) tb_order_detail：补 remark（update.sql §9.4；C-08 单品备注缺口）
--    云库由 12-...sql 已覆盖，此处再兜一次；本地库实测缺失
-- ---------------------------------------------------------------------------
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_order_detail'
            AND COLUMN_NAME = 'remark');
SET @ddl = IF(@c = 0,
              'ALTER TABLE `tb_order_detail` ADD `remark` varchar(200) DEFAULT NULL COMMENT ''单品备注'' AFTER `after_coupons_discount_price`',
              'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;


-- ---------------------------------------------------------------------------
-- 4) 验证（4 项应全为 expect 值）
-- ---------------------------------------------------------------------------
SELECT 'tb_invoice_apply.uk_order 存在' AS item,
       (SELECT COUNT(*) FROM information_schema.STATISTICS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_invoice_apply'
           AND INDEX_NAME = 'uk_order') AS n, 1 AS expect
UNION ALL SELECT 'tb_invoice_apply.idx_order 已移除',
       (SELECT COUNT(*) FROM information_schema.STATISTICS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_invoice_apply'
           AND INDEX_NAME = 'idx_order'), 0
UNION ALL SELECT 'tb_wine_storage.location 存在',
       (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_wine_storage'
           AND COLUMN_NAME = 'location'), 1
UNION ALL SELECT 'tb_order_detail.remark 存在',
       (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_order_detail'
           AND COLUMN_NAME = 'remark'), 1;
