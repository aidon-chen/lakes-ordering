-- ============================================================================
-- 云端结构对齐：补齐 3 张存量表缺失的 15 列（云库 = 早期快照，本地已多次 ALTER）
-- 列定义从本地库 information_schema 读取；**按本地列顺序排序**保证 AFTER 依赖先创建
-- 特性：全幂等；不删列、不改既有列。PREPARE 外层字符串内单引号已双写转义
-- 重点：tb_order 缺 merchant_id / order_channel —— 不补则线上点餐下单直接报错
-- ============================================================================


-- ---- tb_advertisement（按本地列顺序）----
-- tb_advertisement.start_time
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_advertisement' AND COLUMN_NAME = 'start_time');
SET @ddl = IF(@c = 0, 'ALTER TABLE `tb_advertisement` ADD `start_time` datetime NULL DEFAULT NULL COMMENT ''生效开始时间'' AFTER `image_link_url`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- tb_advertisement.end_time
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_advertisement' AND COLUMN_NAME = 'end_time');
SET @ddl = IF(@c = 0, 'ALTER TABLE `tb_advertisement` ADD `end_time` datetime NULL DEFAULT NULL COMMENT ''生效结束时间'' AFTER `start_time`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- tb_advertisement.popup_frequency
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_advertisement' AND COLUMN_NAME = 'popup_frequency');
SET @ddl = IF(@c = 0, 'ALTER TABLE `tb_advertisement` ADD `popup_frequency` int NULL DEFAULT ''1'' COMMENT ''展示频次 1=每次进入 2=每天一次 3=仅一次'' AFTER `end_time`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;


-- ---- tb_dining_table（按本地列顺序）----
-- tb_dining_table.type_id
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_dining_table' AND COLUMN_NAME = 'type_id');
SET @ddl = IF(@c = 0, 'ALTER TABLE `tb_dining_table` ADD `type_id` int NULL DEFAULT NULL COMMENT ''台位类型id（tb_table_type.id）'' AFTER `table_name`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- tb_dining_table.zone_id
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_dining_table' AND COLUMN_NAME = 'zone_id');
SET @ddl = IF(@c = 0, 'ALTER TABLE `tb_dining_table` ADD `zone_id` int NULL DEFAULT NULL COMMENT ''区域id（tb_table_zone.id）'' AFTER `type_id`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- tb_dining_table.capacity
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_dining_table' AND COLUMN_NAME = 'capacity');
SET @ddl = IF(@c = 0, 'ALTER TABLE `tb_dining_table` ADD `capacity` int NULL DEFAULT ''4'' COMMENT ''容纳人数'' AFTER `zone_id`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- tb_dining_table.status
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_dining_table' AND COLUMN_NAME = 'status');
SET @ddl = IF(@c = 0, 'ALTER TABLE `tb_dining_table` ADD `status` int NULL DEFAULT ''1'' COMMENT ''状态 1=空闲 2=占用 3=停用（2=占用态随收银端启用，一期不流转）'' AFTER `capacity`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- tb_dining_table.qr_scene
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_dining_table' AND COLUMN_NAME = 'qr_scene');
SET @ddl = IF(@c = 0, 'ALTER TABLE `tb_dining_table` ADD `qr_scene` varchar(64) NULL DEFAULT NULL COMMENT ''桌码scene快照 shopId,tableNo'' AFTER `status`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- tb_dining_table.qr_code_url
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_dining_table' AND COLUMN_NAME = 'qr_code_url');
SET @ddl = IF(@c = 0, 'ALTER TABLE `tb_dining_table` ADD `qr_code_url` varchar(500) NULL DEFAULT NULL COMMENT ''桌码图片(OSS)'' AFTER `qr_scene`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;


-- ---- tb_order（按本地列顺序）----
-- tb_order.order_channel
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_order' AND COLUMN_NAME = 'order_channel');
SET @ddl = IF(@c = 0, 'ALTER TABLE `tb_order` ADD `order_channel` int NULL DEFAULT NULL COMMENT ''订单渠道 5=收银台'' AFTER `shop_logo_img`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- tb_order.merchant_id
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_order' AND COLUMN_NAME = 'merchant_id');
SET @ddl = IF(@c = 0, 'ALTER TABLE `tb_order` ADD `merchant_id` int NULL DEFAULT NULL COMMENT ''商家id'' AFTER `order_channel`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- tb_order.table_id
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_order' AND COLUMN_NAME = 'table_id');
SET @ddl = IF(@c = 0, 'ALTER TABLE `tb_order` ADD `table_id` bigint NULL DEFAULT NULL COMMENT ''台位id（tb_dining_table.id）'' AFTER `checkout_mode`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- tb_order.dining_num
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_order' AND COLUMN_NAME = 'dining_num');
SET @ddl = IF(@c = 0, 'ALTER TABLE `tb_order` ADD `dining_num` int NULL DEFAULT ''0'' COMMENT ''就餐人数(堂食)'' AFTER `table_name`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- tb_order.dining_session_id
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_order' AND COLUMN_NAME = 'dining_session_id');
SET @ddl = IF(@c = 0, 'ALTER TABLE `tb_order` ADD `dining_session_id` int NULL DEFAULT NULL COMMENT ''开台会话id'' AFTER `dining_num`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- tb_order.order_scene
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_order' AND COLUMN_NAME = 'order_scene');
SET @ddl = IF(@c = 0, 'ALTER TABLE `tb_order` ADD `order_scene` int NULL DEFAULT ''1'' COMMENT ''订单场景 1=堂食 2=外带自提'' AFTER `dining_session_id`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 索引对齐：tb_dining_table 缺唯一键 uk_shop_table_no(shop_id, table_no)
-- （云库该表是旧结构；本地为 UNIQUE。先查重，有重复则跳过并告警）
-- ---------------------------------------------------------------------------
SET @dup = (SELECT COUNT(*) FROM (SELECT `shop_id`,`table_no` FROM `tb_dining_table` GROUP BY `shop_id`,`table_no` HAVING COUNT(*) > 1) d);
SET @has = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_dining_table' AND INDEX_NAME = 'uk_shop_table_no');
SET @ddl = IF(@dup > 0, 'SELECT ''SKIP: tb_dining_table 存在重复(shop_id,table_no)，请先去重''',
              IF(@has > 0, 'SELECT 1', 'ALTER TABLE `tb_dining_table` ADD UNIQUE KEY `uk_shop_table_no` (`shop_id`,`table_no`)'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
