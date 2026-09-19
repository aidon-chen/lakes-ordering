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
-- 4) 外带商城商品表（C-10：礼盒/整瓶外带，独立于堂食 tb_goods）
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `tb_mall_goods`;
CREATE TABLE `tb_mall_goods` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `shop_id` int(11) NOT NULL COMMENT '门店id',
  `name` varchar(100) NOT NULL COMMENT '商品名称',
  `main_image` varchar(128) DEFAULT NULL COMMENT '商品主图',
  `sub_images` varchar(1024) DEFAULT NULL COMMENT '副图 JSON',
  `goods_type` int(2) DEFAULT 1 COMMENT '商品类型 1=礼盒 2=整瓶外带',
  `price` decimal(10,2) NOT NULL COMMENT '售价',
  `stock` int(11) DEFAULT 0 COMMENT '库存',
  `net_content` varchar(50) DEFAULT NULL COMMENT '净含量',
  `detail` text COMMENT '图文详情',
  `is_sale` tinyint(1) DEFAULT 1 COMMENT '是否上架 0=下架 1=上架',
  `sort_number` int(11) DEFAULT 1 COMMENT '排序号',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='外带商城商品表';

-- ---------------------------------------------------------------------------
-- 5) 外带自提订单表 + 明细（C-10：线上预订锁库存，到店支付后核销，无在线支付）
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `tb_mall_order`;
CREATE TABLE `tb_mall_order` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `member_id` int(11) NOT NULL COMMENT '用户id',
  `shop_id` int(11) NOT NULL COMMENT '自提门店id',
  `shop_name` varchar(100) DEFAULT NULL COMMENT '自提门店名称(快照)',
  `order_no` varchar(50) NOT NULL COMMENT '订单编号',
  `goods_total_price` decimal(10,2) DEFAULT '0.00' COMMENT '预估金额(到店支付)',
  `goods_total_quantity` int(11) DEFAULT 0 COMMENT '商品总数量',
  `contact_name` varchar(50) DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(11) DEFAULT NULL COMMENT '联系电话',
  `pickup_code` varchar(16) NOT NULL COMMENT '自提码(唯一)',
  `pickup_deadline` datetime DEFAULT NULL COMMENT '取货截止时间',
  `status` int(2) DEFAULT 1 COMMENT '状态 1=待自提 2=已完成(已核销) 3=已取消 4=已过期',
  `verify_time` datetime DEFAULT NULL COMMENT '核销时间',
  `verifier_id` int(11) DEFAULT NULL COMMENT '核销人id(商家端)',
  `cancel_reason` varchar(200) DEFAULT NULL COMMENT '取消原因',
  `is_printed` tinyint(1) DEFAULT 0 COMMENT '是否已打印 0=否 1=是',
  `create_time` datetime DEFAULT NULL COMMENT '下单时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pickup_code` (`pickup_code`),
  KEY `idx_member_status` (`member_id`,`status`),
  KEY `idx_shop_status` (`shop_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='外带自提订单表';

DROP TABLE IF EXISTS `tb_mall_order_item`;
CREATE TABLE `tb_mall_order_item` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `order_id` int(11) NOT NULL COMMENT '外带订单id',
  `goods_id` int(11) NOT NULL COMMENT '外带商品id',
  `goods_name` varchar(100) NOT NULL COMMENT '商品名称(快照)',
  `goods_type` int(2) DEFAULT 1 COMMENT '商品类型 1=礼盒 2=整瓶外带(快照)',
  `main_image` varchar(128) DEFAULT NULL COMMENT '商品主图(快照)',
  `spec_text` varchar(200) DEFAULT NULL COMMENT '规格说明',
  `price` decimal(10,2) DEFAULT '0.00' COMMENT '单价',
  `number` int(11) DEFAULT 1 COMMENT '数量',
  `subtotal` decimal(10,2) DEFAULT '0.00' COMMENT '小计',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='外带自提订单明细表';

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
