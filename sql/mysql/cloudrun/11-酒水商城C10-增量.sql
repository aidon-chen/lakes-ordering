-- ============================================================================
-- 酒水商城（C-10）批次 6 云端增量脚本
-- 来源：sql/mysql/update.sql 的 §11 段（自动导出，勿手工分叉维护）
-- 用途：在**云托管库**上执行本文件，使线上商城具备自提码/核销/退款单号等新列与新种子数据
-- 特性：**全段幂等** —— ALTER 走 information_schema 判断；种子数据先 DELETE 再按显式 id INSERT
--       可安全重复执行；建议先备份（见同目录 apply-cloud-sql.py --backup）
-- 执行方式：见同目录 README.md
-- ============================================================================

-- 11) 酒水商城（C-10）批次 6 增量（2026-09-19 定稿）
--
-- ★ 表体系裁决（以工程现状为准）：商城走基座**现成且在库**的 tb_points_mall_* 体系
--   （17 张表 + 全套 rest/pointsMall 接口 + 客户端 9 页已实现）。
--   §4/§5 的 tb_mall_goods / tb_mall_order / tb_mall_order_item **不启用**：
--   实测该三表从未在任何库建过、后端零代码、前端零页面；本次仅**吸收其字段设计**
--   （pickup_code / pickup_deadline / verify_time / verifier_id）落到 tb_points_mall_order。
-- ============================================================================

-- 11.1 tb_points_mall_order 补自提要素（幂等：列已存在则跳过）
--   pickup_code      自提码（画板 B-09「A-038」，唯一）
--   pickup_deadline  取货截止时间（超时置 status=10 由定时任务处理）
--   verify_time      核销时间（顾客「确认取货」或商家端 M-03 核销时写入）
--   verifier_id      核销人 id（商家端商家 id；顾客自助核销为 NULL）
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_points_mall_order' AND COLUMN_NAME = 'pickup_code');
SET @ddl = IF(@c = 0, 'ALTER TABLE `tb_points_mall_order` ADD `pickup_code` varchar(16) DEFAULT NULL COMMENT ''自提码(画板 A-038 口径，唯一)'' AFTER `shop_address`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_points_mall_order' AND COLUMN_NAME = 'pickup_deadline');
SET @ddl = IF(@c = 0, 'ALTER TABLE `tb_points_mall_order` ADD `pickup_deadline` datetime DEFAULT NULL COMMENT ''取货截止时间'' AFTER `pickup_code`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_points_mall_order' AND COLUMN_NAME = 'verify_time');
SET @ddl = IF(@c = 0, 'ALTER TABLE `tb_points_mall_order` ADD `verify_time` datetime DEFAULT NULL COMMENT ''自提核销时间（顾客确认取货/商家核销）'' AFTER `pickup_deadline`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_points_mall_order' AND COLUMN_NAME = 'verifier_id');
SET @ddl = IF(@c = 0, 'ALTER TABLE `tb_points_mall_order` ADD `verifier_id` int(11) DEFAULT NULL COMMENT ''核销人id(商家端；顾客自助为空)'' AFTER `verify_time`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 11.1.1 pickup_code 唯一索引（幂等：索引已存在则跳过；允许多个 NULL = 非自提订单）
SET @c = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_points_mall_order' AND INDEX_NAME = 'uk_pickup_code');
SET @ddl = IF(@c = 0, 'ALTER TABLE `tb_points_mall_order` ADD UNIQUE KEY `uk_pickup_code` (`pickup_code`)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 11.2 tb_points_mall_order_refund 补退款单号（画板 B-08「退款单号 RF202609130001」）
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_points_mall_order_refund' AND COLUMN_NAME = 'refund_no');
SET @ddl = IF(@c = 0, 'ALTER TABLE `tb_points_mall_order_refund` ADD `refund_no` varchar(32) DEFAULT NULL COMMENT ''退款单号(RF+yyyyMMdd+4位序号)'' AFTER `order_id`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_points_mall_order_refund' AND INDEX_NAME = 'uk_refund_no');
SET @ddl = IF(@c = 0, 'ALTER TABLE `tb_points_mall_order_refund` ADD UNIQUE KEY `uk_refund_no` (`refund_no`)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 11.3 订单状态机口径（只定文案映射，**不改基座值域**，避免动到点餐/管理端既有逻辑）
--   基座 status：1=未付款 4=待发货 4→ 商城自取场景映射「待自提」 5=已发货 6=已完成 7=售后处理中 9=售后完成 10=已取消
--   商城（shopping_way=1 门店自提）映射：
--     下单未支付            → 1  → 前端文案「待支付」
--     到店支付完成/门店备货完成 → 4  → 前端文案「待自提」（B-06 状态头）
--     顾客确认取货 / 商家核销   → 6  → 「已完成」，同时写 verify_time、verifier_id
--     主动取消 / 超时未支付     → 10 → 「已取消」
--     退款申请中 / 退款成功     → 7 / 9 → B-07/B-08
--   → 文案映射在 VO 与前端按 shopping_way 判定，**后端不改 status 取值**

-- 11.4 商城种子数据：重置为「乐器周边」商品池（画板 B-01~B-04 口径）
--   原美妆 demo 数据（25 商品/1 分类/3 规格组/14 选项/1 满减）已备份：
--   sql/mysql/archive/points-mall-seed-美妆demo-备份-20260919.sql
--   ⚠️ 本段为**演示种子数据**：会清空并重灌分类/商品/规格。正式环境执行前先确认
--      无真实商城订单引用（订单明细 tb_points_mall_order_detail 存商品快照，
--      但购物车 tb_points_mall_shopping_cart 与收藏表会引用 goods_id）。
DELETE FROM `tb_points_mall_menu_goods_relation`;
DELETE FROM `tb_points_mall_goods_specification_option`;
DELETE FROM `tb_points_mall_goods_specification`;
DELETE FROM `tb_points_mall_goods`;
DELETE FROM `tb_points_mall_menu`;

-- 11.4.1 分类 12 项（shop_id=NULL 全店通用；排序与画板 B-02 左栏一致；「全部」为前端虚拟项不入库）
INSERT INTO `tb_points_mall_menu` (`id`,`shop_id`,`name`,`description`,`sort_number`,`icon`) VALUES
  (1,NULL,'吉他','',1,NULL),(2,NULL,'尤克里里','',2,NULL),(3,NULL,'打击乐','',3,NULL),
  (4,NULL,'管乐','',4,NULL),(5,NULL,'弦乐配件','',5,NULL),(6,NULL,'音响设备','',6,NULL),
  (7,NULL,'琴弦','',7,NULL),(8,NULL,'乐器包','',8,NULL),(9,NULL,'优惠专区','',9,NULL),
  (10,NULL,'会员专享','',10,NULL),(11,NULL,'新品上架','',11,NULL),(12,NULL,'周边文创','',12,NULL);

-- 11.4.2 商品 12 条（id 101+ 便于识别；status 2=已上架 4=售罄；main_image 暂空=前端灰底占位，与画板图位一致）
INSERT INTO `tb_points_mall_goods`
 (`id`,`shop_id`,`name`,`main_image`,`sub_images`,`detail`,`detail_images`,`price`,`stock`,`is_hot`,`is_new`,`status`,`is_sale`,`sale_price`,`monthly_sales`,`total_sales`,`total_comments`,`preferential_name`,`packing_charges`,`product_time`,`exchange_points`,`sort_number`) VALUES
 (101,NULL,'尤克里里 23 寸',NULL,NULL,'23 寸尤克里里，桃花心木面板，音色明亮通透；琴颈顺滑易按，附赠琴包与拨片。',NULL,69.00,100,1,0,2,0,0.00,328,328,0,NULL,0.00,0.00,NULL,1),
 (102,NULL,'口琴 24 孔',NULL,NULL,'24 孔 C 调口琴，金属簧片，适合民谣与蓝调入门。',NULL,39.00,200,1,0,2,0,0.00,156,156,0,NULL,0.00,0.00,NULL,2),
 (103,NULL,'节拍器',NULL,NULL,'机械节拍器，速度 40-208 BPM，木质外壳。',NULL,49.00,80,1,0,2,0,0.00,87,87,0,NULL,0.00,0.00,NULL,3),
 (104,NULL,'吉他拨片套装',NULL,NULL,'3 片装拨片，0.73/0.88/1.0mm 三种厚度。',NULL,25.00,500,1,0,2,0,0.00,210,210,0,NULL,0.00,0.00,NULL,4),
 (105,NULL,'卡林巴琴 17 音',NULL,NULL,'17 音拇指琴，桃花心木琴身，附调音锤与琴包。',NULL,59.00,60,0,1,2,0,0.00,44,44,0,NULL,0.00,0.00,NULL,5),
 (106,NULL,'民谣吉他 40 寸',NULL,NULL,'40 寸民谣吉他，云杉面板，缺货补货中。',NULL,399.00,0,0,0,4,0,0.00,0,0,0,NULL,0.00,0.00,NULL,6),
 (107,NULL,'拇指琴 17 音',NULL,NULL,'17 音拇指琴，精选单板，音色清亮。',NULL,199.00,30,0,0,2,0,0.00,12,12,0,NULL,0.00,0.00,NULL,7),
 (108,NULL,'尤克里里琴包',NULL,NULL,'加厚防摔琴包，可背可提，适配 23/26 寸。',NULL,29.00,120,0,0,2,0,0.00,65,65,0,NULL,0.00,0.00,NULL,8),
 (109,NULL,'尤克里里琴弦套装',NULL,NULL,'尼龙琴弦一套，防锈耐用。',NULL,19.00,300,0,0,2,0,0.00,88,88,0,NULL,0.00,0.00,NULL,9),
 (110,NULL,'便携蓝牙音箱',NULL,NULL,'便携蓝牙音箱，续航 8 小时，适合小型演出。',NULL,159.00,45,0,0,2,0,0.00,23,23,0,NULL,0.00,0.00,NULL,10),
 (111,NULL,'乐队主题帆布袋',NULL,NULL,'乐队主题印花帆布袋，加厚棉麻。',NULL,39.00,150,0,0,2,0,0.00,31,31,0,NULL,0.00,0.00,NULL,11),
 (112,NULL,'民谣吉他拨片（促销）',NULL,NULL,'民谣吉他拨片，优惠专区限量促销。',NULL,19.90,200,0,0,2,1,12.90,66,66,0,'优惠专区',0.00,0.00,NULL,12);

-- 11.4.3 分类-商品挂载（每个分类至少 1 个商品，保证 B-01/B-02 分类切换无空态）
INSERT INTO `tb_points_mall_menu_goods_relation` (`menu_id`,`goods_id`) VALUES
  (1,106),(1,112),(2,101),(3,103),(4,102),(5,104),(6,110),(7,109),(8,108),(9,112),(10,107),(11,105),(12,111);

-- 11.4.4 商品规格（画板 B-04-SKU：两组「尺寸 / 配件」，挂在尤克里里 23 寸 id=101）
INSERT INTO `tb_points_mall_goods_specification` (`id`,`goods_id`,`name`,`sort_number`) VALUES
  (1,101,'尺寸',1),(2,101,'配件',2);
INSERT INTO `tb_points_mall_goods_specification_option` (`id`,`goods_id`,`goods_specification_id`,`name`,`price`,`stock`,`sort_number`) VALUES
  (1,101,1,'23 寸',0.00,100,1),(2,101,1,'26 寸',0.00,50,2),
  (3,101,2,'裸琴',0.00,100,1),(4,101,2,'含琴包',0.00,80,2);

-- 11.4.5 满减规则（画板 B-04 满减条 / B-05 金额卡：满99减10）
UPDATE `tb_points_mall_full_reduction_rule`
   SET `name`='满99减10', `status`=1, `limited_price`=99.00, `reduced_price`=10.00,
       `update_time`=NOW()
 WHERE `id`=2;
