-- ============================================================================
-- 乐章点餐 一期增量脚本（2026-09-18 M1 起，接续 siam_db.sql 建库后执行）
-- 说明：
--   1) 本文件为增量通道：新表用 DROP+CREATE（可重复执行）；ALTER 不幂等，
--      重复执行会报 duplicate column，请按需挑选执行段。
--   2) 原项目遗留的台位增量（旧版 tb_dining_table + tb_order 台位列，
--      均未在云托管库执行过）已并入本文件并按《客户端缺失功能设计方案
--      -C02-03-04-10-14-15-16-v1.0.md》调整；旧版内容不再保留。
--   3) 并入 siam_db.sql 主脚本的对应内容见各段末尾注释标记 [主脚本已并入]。
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1) 台位表（C-04 扫码开台主表）
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `tb_dining_table`;
CREATE TABLE `tb_dining_table` (
  `id` bigint(11) NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `shop_id` int(11) DEFAULT NULL COMMENT '店铺id',
  `table_no` varchar(50) DEFAULT NULL COMMENT '餐桌编号',
  `table_name` varchar(50) NOT NULL COMMENT '餐桌名称',
  `type_id` int(11) DEFAULT NULL COMMENT '台位类型id（tb_table_type.id）',
  `capacity` int(11) DEFAULT 4 COMMENT '容纳人数',
  `status` int(2) DEFAULT 1 COMMENT '状态 1=空闲 2=占用 3=停用（2=占用态随收银端启用，一期不流转）',
  `qr_scene` varchar(64) DEFAULT NULL COMMENT '桌码scene快照 shopId,tableNo',
  `qr_code_url` varchar(500) DEFAULT NULL COMMENT '桌码图片(OSS)',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shop_table_no` (`shop_id`,`table_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='餐桌/台位表';
-- [主脚本已并入：siam_db.sql 末尾「乐章点餐一期新增表」区块]

-- ---------------------------------------------------------------------------
-- 2) 台位类型表（散台/卡座 + 低消；C-03 门店详情台位说明、C-04 低消核算）
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `tb_table_type`;
CREATE TABLE `tb_table_type` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `shop_id` int(11) NOT NULL COMMENT '门店id',
  `name` varchar(50) NOT NULL COMMENT '类型名 散台/卡座',
  `min_consume` decimal(10,2) DEFAULT '0.00' COMMENT '低消(元)',
  `description` varchar(200) DEFAULT NULL COMMENT '说明',
  `sort_number` int(11) DEFAULT 1 COMMENT '排序号',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='台位类型表';

-- ---------------------------------------------------------------------------
-- 3) 开台会话表（C-04：扫码开台；归集同台位多笔订单/加酒）
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `tb_dining_session`;
CREATE TABLE `tb_dining_session` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `shop_id` int(11) NOT NULL COMMENT '门店id',
  `table_id` bigint(11) NOT NULL COMMENT '台位id（tb_dining_table.id）',
  `member_id` int(11) NOT NULL COMMENT '开台会员id',
  `member_num` int(11) DEFAULT 1 COMMENT '就餐人数',
  `status` int(2) DEFAULT 1 COMMENT '状态 1=进行中 2=已结台 3=已失效',
  `open_time` datetime DEFAULT NULL COMMENT '开台时间',
  `close_time` datetime DEFAULT NULL COMMENT '结台时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_table_status` (`table_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='开台会话表';

-- ---------------------------------------------------------------------------
-- 6) 发票申请表（C-15：基座仅有 tb_order.is_invoice/invoice_id 标记，无主表）
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `tb_invoice_apply`;
CREATE TABLE `tb_invoice_apply` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `member_id` int(11) NOT NULL COMMENT '用户id',
  `order_id` int(11) NOT NULL COMMENT '订单id',
  `order_no` varchar(50) DEFAULT NULL COMMENT '订单编号(快照)',
  `amount` decimal(10,2) DEFAULT '0.00' COMMENT '开票金额(=订单实付)',
  `title_type` int(2) DEFAULT 1 COMMENT '抬头类型 1=个人 2=企业',
  `title` varchar(100) NOT NULL COMMENT '发票抬头',
  `tax_no` varchar(50) DEFAULT NULL COMMENT '税号(企业抬头必填)',
  `email` varchar(64) NOT NULL COMMENT '接收邮箱',
  `status` int(2) DEFAULT 1 COMMENT '状态 1=待处理 2=已开具 3=已驳回',
  `remark` varchar(200) DEFAULT NULL COMMENT '驳回原因/备注',
  `apply_time` datetime DEFAULT NULL COMMENT '申请时间',
  `finish_time` datetime DEFAULT NULL COMMENT '处理完成时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_member_status` (`member_id`,`status`),
  UNIQUE KEY `uk_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='发票申请表';

-- ---------------------------------------------------------------------------
-- 7) 存酒记录表 + 取酒记录（C-16：商家端录入，顾客端查询/出示存酒码，分次取酒）
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `tb_wine_storage`;
CREATE TABLE `tb_wine_storage` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `member_id` int(11) NOT NULL COMMENT '会员id',
  `shop_id` int(11) NOT NULL COMMENT '存入门店id',
  `goods_name` varchar(100) NOT NULL COMMENT '品名',
  `bottle_count` int(11) NOT NULL COMMENT '存入瓶数',
  `remain_count` int(11) NOT NULL COMMENT '剩余瓶数',
  `storage_code` varchar(16) NOT NULL COMMENT '存酒码(唯一)',
  `location` varchar(50) DEFAULT NULL COMMENT '存放柜位，如 B 区 03 号柜（批次5新增）',
  `storage_date` date NOT NULL COMMENT '存入日期',
  `expire_date` date NOT NULL COMMENT '有效期截止日期',
  `status` int(2) DEFAULT 1 COMMENT '状态 1=在存 2=已取完 3=已过期',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_storage_code` (`storage_code`),
  KEY `idx_member_status` (`member_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='存酒记录表';

DROP TABLE IF EXISTS `tb_wine_storage_record`;
CREATE TABLE `tb_wine_storage_record` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `storage_id` int(11) NOT NULL COMMENT '存酒记录id',
  `take_count` int(11) NOT NULL COMMENT '本次取酒瓶数',
  `operator_id` int(11) DEFAULT NULL COMMENT '操作人id(商家端)',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `take_time` datetime DEFAULT NULL COMMENT '取酒时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_storage` (`storage_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='取酒记录表';

-- ---------------------------------------------------------------------------
-- 8) 公告表（C-14：列表+详情；NULL 门店=全店通用）
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `tb_notice`;
CREATE TABLE `tb_notice` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `shop_id` int(11) DEFAULT NULL COMMENT '门店id NULL=全店通用',
  `title` varchar(100) NOT NULL COMMENT '公告标题',
  `content` text COMMENT '公告内容(富文本)',
  `is_top` tinyint(1) DEFAULT 0 COMMENT '是否置顶 0=否 1=是',
  `status` tinyint(1) DEFAULT 1 COMMENT '状态 0=下线 1=发布',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_shop_status` (`shop_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='公告表';

-- ---------------------------------------------------------------------------
-- 9) 存量表 ALTER（原项目遗留段已按新设计调整；未在云托管库执行过）
-- ---------------------------------------------------------------------------
-- 9.1 tb_order：台位/会话/人数/订单场景（C-04）
ALTER TABLE `tb_order`
  ADD `checkout_mode` int(2) DEFAULT '1' COMMENT '结账模式 1=先付款后就餐 2=先就餐后付款' AFTER shop_logo_img,
  ADD `table_id` bigint(11) DEFAULT NULL COMMENT '台位id（tb_dining_table.id）' AFTER checkout_mode,
  ADD `table_no` varchar(50) DEFAULT NULL COMMENT '餐桌编号(快照)' AFTER table_id,
  ADD `table_name` varchar(50) DEFAULT NULL COMMENT '餐桌名称(快照)' AFTER table_no,
  ADD `dining_num` int(11) DEFAULT 0 COMMENT '就餐人数(堂食)' AFTER table_name,
  ADD `dining_session_id` int(11) DEFAULT NULL COMMENT '开台会话id' AFTER dining_num,
  ADD `order_scene` int(2) DEFAULT 1 COMMENT '订单场景 1=堂食 2=外带自提' AFTER dining_session_id,
  ADD `is_payment` tinyint(1) DEFAULT '0' COMMENT '是否已支付' AFTER order_scene;
-- 9.2 tb_shop：结账模式（遗留字段保留，我方主场景=线上支付+到店结台）
ALTER TABLE `tb_shop`
  ADD `checkout_mode` int(2) DEFAULT '1' COMMENT '结账模式 1=先付款后就餐 2=先就餐后付款' AFTER latitude;
-- 9.3 tb_advertisement：活动弹窗（C-02）——复用广告表，type 追加 5=首页活动弹窗
ALTER TABLE `tb_advertisement`
  ADD `start_time` datetime DEFAULT NULL COMMENT '生效开始时间' AFTER image_link_url,
  ADD `end_time` datetime DEFAULT NULL COMMENT '生效结束时间' AFTER start_time,
  ADD `popup_frequency` int(2) DEFAULT 1 COMMENT '展示频次 1=每次进入 2=每天一次 3=仅一次' AFTER end_time;
-- 9.4 tb_order_detail：单品备注（C-08 缺口，随 M1 一并补）
ALTER TABLE `tb_order_detail`
  ADD `remark` varchar(200) DEFAULT NULL COMMENT '单品备注' AFTER after_coupons_discount_price;

-- ---------------------------------------------------------------------------
-- 10) 收银台台位作业增量（2026-09-19，grill Round 0-10 定稿：开台/清台/转台）
-- ---------------------------------------------------------------------------
-- 10.1 tb_dining_session：开台无会员（收银开台为空，扫码开台后续回填）
ALTER TABLE `tb_dining_session` MODIFY `member_id` int(11) NULL COMMENT '开台会员id（收银开台为空，扫码开台回填）';

-- 10.2 台位区域表（桌台图分区 W-07；Round 1 拍板）
DROP TABLE IF EXISTS `tb_table_zone`;
CREATE TABLE `tb_table_zone` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `shop_id` int(11) NOT NULL COMMENT '门店id',
  `name` varchar(50) NOT NULL COMMENT '区域名 A区/K区/V区',
  `description` varchar(200) DEFAULT NULL COMMENT '区域说明',
  `sort_number` int(11) DEFAULT 1 COMMENT '排序号',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='台位区域表';

-- 10.3 tb_dining_table 加区域列（幂等：已存在则跳过）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_dining_table' AND COLUMN_NAME = 'zone_id');
SET @ddl = IF(@col_exists = 0, 'ALTER TABLE `tb_dining_table` ADD `zone_id` int(11) DEFAULT NULL COMMENT ''区域id（tb_table_zone.id）'' AFTER `type_id`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 10.4 测试数据段（Round 10：3 区域 + 3 类型 + 18 桌；shop_id=13 = 商家 siam）
INSERT INTO `tb_table_zone` (`shop_id`,`name`,`description`,`sort_number`) VALUES
  (13,'A区散台','散台区',1),(13,'K区卡座','卡座区',2),(13,'V区包间','包间区',3);
INSERT INTO `tb_table_type` (`shop_id`,`name`,`min_consume`,`sort_number`) VALUES
  (13,'散台',0.00,1),(13,'卡座',0.00,2),(13,'包间',0.00,3);
INSERT INTO `tb_dining_table` (`shop_id`,`table_no`,`table_name`,`type_id`,`zone_id`,`capacity`,`status`) VALUES
  (13,'A01','A区·A01',1,1,4,1),(13,'A02','A区·A02',1,1,4,1),(13,'A03','A区·A03',1,1,4,1),
  (13,'A04','A区·A04',1,1,4,1),(13,'A05','A区·A05',1,1,4,1),(13,'A06','A区·A06',1,1,4,1),
  (13,'A07','A区·A07',1,1,4,1),(13,'A08','A区·A08',1,1,4,1),(13,'A09','A区·A09',1,1,4,1),
  (13,'A10','A区·A10',1,1,4,1),(13,'A11','A区·A11',1,1,4,1),(13,'A12','A区·A12',1,1,4,1),
  (13,'K01','K区·K01',2,2,6,1),(13,'K02','K区·K02',2,2,6,1),(13,'K03','K区·K03',2,2,6,1),
  (13,'K04','K区·K04',2,2,6,1),(13,'V01','V区·V01',3,3,10,1),(13,'V02','V区·V02',3,3,10,1);

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

-- ============================================================================
-- 12) 商城「猜你喜欢 / 推荐好物」管理增量（2026-10-02）
--
--   推荐好物：改按展示销量倒序 = total_sales + IFNULL(sales_offset,0)；
--     is_hot 保留为「参与推荐」开关（商家端「首页推荐」开关继续可用）。
--   猜你喜欢：商家手动挑选关联表（物理删除关联行，不影响商品本体）；
--     shop_id 可空 = 平台级挑选（本部署商城商品 shop_id 全为 NULL，顾客端查询为平台级）。
-- ============================================================================
-- 12.1 tb_points_mall_goods 加销量偏移列（幂等）
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_points_mall_goods' AND COLUMN_NAME = 'sales_offset');
SET @ddl = IF(@c = 0, 'ALTER TABLE `tb_points_mall_goods` ADD `sales_offset` int(11) DEFAULT NULL COMMENT ''销量偏移量(展示销量=累计销量+偏移)'' AFTER `total_sales`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 12.2 猜你喜欢关联表（IF NOT EXISTS 幂等）
CREATE TABLE IF NOT EXISTS `tb_guess_you_like` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `shop_id` int(11) DEFAULT NULL COMMENT '店铺id(NULL=平台级挑选)',
  `goods_id` int(11) NOT NULL COMMENT '商城商品id(tb_points_mall_goods.id)',
  `sort_number` int(11) NOT NULL DEFAULT 0 COMMENT '排序号(升序展示)',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_goods_id` (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='猜你喜欢-商家手动挑选关联表';
