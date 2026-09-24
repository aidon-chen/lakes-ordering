-- 【云托管控制台粘贴版】营业时段叠加设计（分类级 sale_period + 单品级关联表 + 门店时段配置）
-- 用法：云托管控制台 → 环境 prod-d6gd6a2twe27f592a → MySQL → 数据库管理 → SQL 窗口，整段粘贴执行。
-- 幂等：可重复执行；执行后对现有菜单零影响（存量分类 sale_period 全为 NULL=全天）。
-- 回滚见 31-营业时段-叠加设计.sql 文件末尾。
-- ⚠️ 值里不能出现分号（控制台按 ; 切分语句）。

-- ---------- 0) tb_shop_setting 兜底建表（云库已有→跳过；本地库缺→补上，与 22- 脚本一致） ----------
CREATE TABLE IF NOT EXISTS `tb_shop_setting` (
  `id`            int(11)      NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `shop_id`       int(11)      NOT NULL                COMMENT '门店id',
  `setting_key`   varchar(64)  NOT NULL                COMMENT '设置项键',
  `setting_value` varchar(500) DEFAULT NULL            COMMENT '设置项值（统一存字符串，前端按类型解析）',
  `create_time`   datetime     DEFAULT NULL            COMMENT '创建时间',
  `update_time`   datetime     DEFAULT NULL            COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shop_key` (`shop_id`, `setting_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='门店营业设置表（键值对）';

-- ---------- 1) tb_menu.sale_period（分类级时段） ----------
-- 放在 is_can_order 之后，便于对照阅读
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_menu' AND COLUMN_NAME = 'sale_period');
SET @ddl = IF(@c = 0, 'ALTER TABLE `tb_menu` ADD `sale_period` varchar(16) DEFAULT NULL COMMENT ''售卖时段：day=白天(咖啡)/night=夜间(酒水)/all=全天/NULL=全天'' AFTER `is_can_order`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 索引：菜单接口按 (shop_id, sale_period) 过滤
SET @c = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_menu' AND INDEX_NAME = 'idx_shop_period');
SET @ddl = IF(@c = 0, 'ALTER TABLE `tb_menu` ADD INDEX `idx_shop_period` (`shop_id`, `sale_period`)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 2) tb_goods_sale_period（单品级覆盖） ----------
CREATE TABLE IF NOT EXISTS `tb_goods_sale_period` (
  `id`          int(11)     NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `goods_id`    int(11)     NOT NULL               COMMENT '商品id（tb_goods.id）',
  `period_code` varchar(16) NOT NULL               COMMENT '时段：day/night（与 tb_menu.sale_period 取值一致）',
  `create_time` datetime             DEFAULT NULL  COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_goods_period` (`goods_id`, `period_code`),
  KEY `idx_period` (`period_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='商品-售卖时段关联（未配置=跟随分类；配了多行=跨时段）';

-- ---------- 3) 时段定义（门店级，两键；不存在才插入，不覆盖人工改过的值） ----------
-- salePeriodEnabled 默认 false —— 咖啡分类与商品还没建，先不开总开关，避免白天菜单变空
INSERT INTO `tb_shop_setting` (`shop_id`, `setting_key`, `setting_value`, `create_time`, `update_time`)
VALUES (13, 'salePeriodEnabled', 'false', NOW(), NOW())
ON DUPLICATE KEY UPDATE `setting_key` = `setting_key`;

-- 时段窗口：HH:mm-HH:mm=code，多段用【英文逗号】分隔；支持跨零点（end < start）；不在任何窗口内 = 打烊空档(closed)
-- ⚠️ 分隔符**不能用分号**：apply-cloud-sql.py 与云托管控制台都按 `;` 切分语句，值里带分号会把 INSERT 截断（本脚本首跑即踩）
INSERT INTO `tb_shop_setting` (`shop_id`, `setting_key`, `setting_value`, `create_time`, `update_time`)
VALUES (13, 'salePeriodRules', '07:00-17:00=day,17:00-02:00=night', NOW(), NOW())
ON DUPLICATE KEY UPDATE `setting_key` = `setting_key`;

-- ---------- 4) 分类时段归属：**默认全留 NULL（=全天可售）**，以下为待启用示例，按需取消注释 ----------
-- 前提：先把「咖啡」分类建出来（分类是数据，不是建表）：
-- INSERT INTO `tb_menu` (`shop_id`,`name`,`description`,`sort_number`,`is_can_order`,`sale_period`,`create_time`,`update_time`)
-- VALUES (13,'咖啡·甄选',NULL,1,1,'day',NOW(),NOW()), (13,'咖啡·奶咖',NULL,2,1,'day',NOW(),NOW());
--
-- 然后按分类批量打标（示例用 shop13 现有分类 id，请按实际调整）：
-- UPDATE `tb_menu` SET `sale_period`='day'   WHERE `shop_id`=13 AND `id` IN (/* 咖啡分类 id */);
-- UPDATE `tb_menu` SET `sale_period`='all'   WHERE `shop_id`=13 AND `id` IN (19,20);                 -- 气泡特调/无酒精特调：全天
-- UPDATE `tb_menu` SET `sale_period`='night' WHERE `shop_id`=13 AND `id` IN (17,18,21,22,23,24,26);  -- 酒水：夜间
--
-- 单品级覆盖示例（给「气泡特调」里某款做成只白天）：
-- INSERT INTO `tb_goods_sale_period` (`goods_id`,`period_code`,`create_time`) VALUES (75,'day',NOW())
--   ON DUPLICATE KEY UPDATE `period_code` = `period_code`;
-- 「全时段单品」写两行（day + night）即可表达跨时段。

-- ---------- 5) 自检 ----------
SELECT 'tables' AS chk, table_name FROM information_schema.tables
 WHERE table_schema = DATABASE() AND table_name IN ('tb_shop_setting','tb_goods_sale_period');
SELECT 'menu cols' AS chk, GROUP_CONCAT(column_name ORDER BY ordinal_position) FROM information_schema.columns
 WHERE table_schema = DATABASE() AND table_name = 'tb_menu'
   AND column_name IN ('is_can_order','sale_period');
SELECT 'menu index' AS chk, index_name FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'tb_menu' AND index_name = 'idx_shop_period';
SELECT 'settings' AS chk, setting_key, setting_value FROM tb_shop_setting
 WHERE shop_id = 13 AND setting_key IN ('salePeriodEnabled','salePeriodRules');
-- 期望：tables 2 行 / menu cols 含 is_can_order,sale_period / menu index 2 行（复合索引含 2 列，正常）/
--       settings 2 行（salePeriodEnabled=false，salePeriodRules=07:00-17:00=day,17:00-02:00=night）

-- ============================================================================
-- 回滚（按需手动执行；结构变更通常无需回滚）
-- DROP TABLE IF EXISTS `tb_goods_sale_period`;
-- ALTER TABLE `tb_menu` DROP INDEX `idx_shop_period`;
-- ALTER TABLE `tb_menu` DROP COLUMN `sale_period`;
-- DELETE FROM `tb_shop_setting` WHERE `shop_id`=13 AND `setting_key` IN ('salePeriodEnabled','salePeriodRules');
-- ============================================================================
