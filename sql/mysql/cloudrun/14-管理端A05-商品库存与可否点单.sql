-- ============================================================================
-- A-05 商品与库存管理（管理端）
--   1) tb_menu.is_can_order      分类「可否点单」开关（1=可点单 0=不可点单），C 端菜单接口按此过滤（即时生效）
--   2) tb_goods.bottle_stock     按瓶库存（人工维护，无自动扣减）
--   3) tb_goods.cup_stock        按杯库存（人工维护，无自动扣减）
--   4) tb_goods.stock_warning_threshold  库存预警阈值（仅前端标色，不参与校验）
--   5) 存量迁移：原 tb_goods.stock → bottle_stock（原口径按件/瓶计）
-- 全部幂等：列已存在则跳过；迁移仅对 bottle_stock IS NULL 的行执行一次
-- ============================================================================

-- 1) tb_menu.is_can_order
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_menu' AND COLUMN_NAME = 'is_can_order');
SET @ddl = IF(@c = 0, 'ALTER TABLE `tb_menu` ADD `is_can_order` tinyint(1) DEFAULT ''1'' COMMENT ''分类是否可点单(1=可点单 0=不可点单)'' AFTER `sort_number`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) tb_goods.bottle_stock
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_goods' AND COLUMN_NAME = 'bottle_stock');
SET @ddl = IF(@c = 0, 'ALTER TABLE `tb_goods` ADD `bottle_stock` int(11) DEFAULT NULL COMMENT ''按瓶库存(人工维护)'' AFTER `stock`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) tb_goods.cup_stock
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_goods' AND COLUMN_NAME = 'cup_stock');
SET @ddl = IF(@c = 0, 'ALTER TABLE `tb_goods` ADD `cup_stock` int(11) DEFAULT NULL COMMENT ''按杯库存(人工维护)'' AFTER `bottle_stock`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4) tb_goods.stock_warning_threshold
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_goods' AND COLUMN_NAME = 'stock_warning_threshold');
SET @ddl = IF(@c = 0, 'ALTER TABLE `tb_goods` ADD `stock_warning_threshold` int(11) DEFAULT NULL COMMENT ''库存预警阈值(仅前端标色)'' AFTER `cup_stock`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5) 存量迁移：stock → bottle_stock（幂等：仅迁 bottle_stock 为空的行）
UPDATE `tb_goods` SET `bottle_stock` = `stock` WHERE `bottle_stock` IS NULL AND `stock` IS NOT NULL;