-- ============================================================================
-- 云端补表脚本（一）：缺失的 7 张表建表 + 台位测试数据
-- 背景：云库（74 表）比本地（84 表）少 10 张；本文件负责 7 张，tb_mall_* 三张不启用
-- 生成：DDL 从本地库 SHOW CREATE TABLE 导出（与本地一致）
-- 修订(2026-09-19 10:5x)：修正两处导出的旧结构 —— ①tb_invoice_apply 的 KEY idx_order → UNIQUE KEY uk_order；
--                        ②tb_wine_storage 补 location 列（存放柜位）。此后本文件不再依赖 13- 兜底
-- 特性：★全幂等 —— CREATE TABLE IF NOT EXISTS（不 DROP）；测试数据先清 shop_id=13 再插
-- 分工：**列差异由 13-结构对齐-缺失列.sql 负责**，本文件不含 ALTER，避免转义踩坑
-- 执行顺序：先执行 13-，再执行本文件（否则 tb_dining_table 缺列会导致测试数据插入失败）
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 一、缺失表建表
-- ---------------------------------------------------------------------------

-- tb_invoice_apply
CREATE TABLE IF NOT EXISTS `tb_invoice_apply` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `member_id` int NOT NULL COMMENT '用户id',
  `order_id` int NOT NULL COMMENT '订单id',
  `order_no` varchar(50) DEFAULT NULL COMMENT '订单编号(快照)',
  `amount` decimal(10,2) DEFAULT '0.00' COMMENT '开票金额(=订单实付)',
  `title_type` int DEFAULT '1' COMMENT '抬头类型 1=个人 2=企业',
  `title` varchar(100) NOT NULL COMMENT '发票抬头',
  `tax_no` varchar(50) DEFAULT NULL COMMENT '税号(企业抬头必填)',
  `email` varchar(64) NOT NULL COMMENT '接收邮箱',
  `status` int DEFAULT '1' COMMENT '状态 1=待处理 2=已开具 3=已驳回',
  `remark` varchar(200) DEFAULT NULL COMMENT '驳回原因/备注',
  `apply_time` datetime DEFAULT NULL COMMENT '申请时间',
  `finish_time` datetime DEFAULT NULL COMMENT '处理完成时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_member_status` (`member_id`,`status`),
  UNIQUE KEY `uk_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='发票申请表';

-- tb_notice
CREATE TABLE IF NOT EXISTS `tb_notice` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `shop_id` int DEFAULT NULL COMMENT '门店id NULL=全店通用',
  `title` varchar(100) NOT NULL COMMENT '公告标题',
  `content` text COMMENT '公告内容(富文本)',
  `is_top` tinyint(1) DEFAULT '0' COMMENT '是否置顶 0=否 1=是',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态 0=下线 1=发布',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_shop_status` (`shop_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='公告表';

-- tb_wine_storage
CREATE TABLE IF NOT EXISTS `tb_wine_storage` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `member_id` int NOT NULL COMMENT '会员id',
  `shop_id` int NOT NULL COMMENT '存入门店id',
  `goods_name` varchar(100) NOT NULL COMMENT '品名',
  `bottle_count` int NOT NULL COMMENT '存入瓶数',
  `remain_count` int NOT NULL COMMENT '剩余瓶数',
  `storage_code` varchar(16) NOT NULL COMMENT '存酒码(唯一)',
  `location` varchar(50) DEFAULT NULL COMMENT '存放柜位，如 B 区 03 号柜（批次5新增）',
  `storage_date` date NOT NULL COMMENT '存入日期',
  `expire_date` date NOT NULL COMMENT '有效期截止日期',
  `status` int DEFAULT '1' COMMENT '状态 1=在存 2=已取完 3=已过期',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_storage_code` (`storage_code`),
  KEY `idx_member_status` (`member_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='存酒记录表';

-- tb_wine_storage_record
CREATE TABLE IF NOT EXISTS `tb_wine_storage_record` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `storage_id` int NOT NULL COMMENT '存酒记录id',
  `take_count` int NOT NULL COMMENT '本次取酒瓶数',
  `operator_id` int DEFAULT NULL COMMENT '操作人id(商家端)',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `take_time` datetime DEFAULT NULL COMMENT '取酒时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_storage` (`storage_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='取酒记录表';

-- tb_dining_session
CREATE TABLE IF NOT EXISTS `tb_dining_session` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `shop_id` int NOT NULL COMMENT '门店id',
  `table_id` bigint NOT NULL COMMENT '台位id（tb_dining_table.id）',
  `member_id` int DEFAULT NULL COMMENT '开台会员id（收银开台为空，扫码开台回填）',
  `member_num` int DEFAULT '1' COMMENT '就餐人数',
  `status` int DEFAULT '1' COMMENT '状态 1=进行中 2=已结台 3=已失效',
  `open_time` datetime DEFAULT NULL COMMENT '开台时间',
  `close_time` datetime DEFAULT NULL COMMENT '结台时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_table_status` (`table_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb3 COMMENT='开台会话表';

-- tb_table_zone
CREATE TABLE IF NOT EXISTS `tb_table_zone` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `shop_id` int NOT NULL COMMENT '门店id',
  `name` varchar(50) NOT NULL COMMENT '区域名 A区/K区/V区',
  `description` varchar(200) DEFAULT NULL COMMENT '区域说明',
  `sort_number` int DEFAULT '1' COMMENT '排序号',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb3 COMMENT='台位区域表';

-- tb_table_type
CREATE TABLE IF NOT EXISTS `tb_table_type` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `shop_id` int NOT NULL COMMENT '门店id',
  `name` varchar(50) NOT NULL COMMENT '类型名 散台/卡座',
  `min_consume` decimal(10,2) DEFAULT '0.00' COMMENT '低消(元)',
  `description` varchar(200) DEFAULT NULL COMMENT '说明',
  `sort_number` int DEFAULT '1' COMMENT '排序号',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb3 COMMENT='台位类型表';


-- ---------------------------------------------------------------------------
-- 二、台位测试数据（shop_id=13 = 商家 siam；先清后插）
-- ---------------------------------------------------------------------------

-- tb_table_zone
DELETE FROM `tb_table_zone` WHERE shop_id=13;
INSERT INTO `tb_table_zone` (`id`,`shop_id`,`name`,`description`,`sort_number`,`create_time`,`update_time`) VALUES (1,13,'A区散台','散台区',1,NULL,NULL);
INSERT INTO `tb_table_zone` (`id`,`shop_id`,`name`,`description`,`sort_number`,`create_time`,`update_time`) VALUES (2,13,'K区卡座','卡座区',2,NULL,NULL);
INSERT INTO `tb_table_zone` (`id`,`shop_id`,`name`,`description`,`sort_number`,`create_time`,`update_time`) VALUES (3,13,'V区包间','包间区',3,NULL,NULL);

-- tb_table_type
DELETE FROM `tb_table_type` WHERE shop_id=13;
INSERT INTO `tb_table_type` (`id`,`shop_id`,`name`,`min_consume`,`description`,`sort_number`,`create_time`,`update_time`) VALUES (1,13,'散台','0.00',NULL,1,NULL,NULL);
INSERT INTO `tb_table_type` (`id`,`shop_id`,`name`,`min_consume`,`description`,`sort_number`,`create_time`,`update_time`) VALUES (2,13,'卡座','0.00',NULL,2,NULL,NULL);
INSERT INTO `tb_table_type` (`id`,`shop_id`,`name`,`min_consume`,`description`,`sort_number`,`create_time`,`update_time`) VALUES (3,13,'包间','0.00',NULL,3,NULL,NULL);

-- tb_dining_table
DELETE FROM `tb_dining_table` WHERE shop_id=13;
INSERT INTO `tb_dining_table` (`id`,`shop_id`,`table_no`,`table_name`,`type_id`,`zone_id`,`capacity`,`status`,`qr_scene`,`qr_code_url`,`create_time`,`update_time`) VALUES (1,13,'A01','A区·A01',2,1,3,2,NULL,NULL,NULL,'2026-09-19 09:47:45');
INSERT INTO `tb_dining_table` (`id`,`shop_id`,`table_no`,`table_name`,`type_id`,`zone_id`,`capacity`,`status`,`qr_scene`,`qr_code_url`,`create_time`,`update_time`) VALUES (2,13,'A02','A区·A02',1,1,4,1,NULL,NULL,NULL,'2026-09-19 05:29:58');
INSERT INTO `tb_dining_table` (`id`,`shop_id`,`table_no`,`table_name`,`type_id`,`zone_id`,`capacity`,`status`,`qr_scene`,`qr_code_url`,`create_time`,`update_time`) VALUES (3,13,'A03','A区·A03',1,1,4,1,NULL,NULL,NULL,'2026-09-19 05:30:13');
INSERT INTO `tb_dining_table` (`id`,`shop_id`,`table_no`,`table_name`,`type_id`,`zone_id`,`capacity`,`status`,`qr_scene`,`qr_code_url`,`create_time`,`update_time`) VALUES (4,13,'A04','A区·A04',1,1,4,1,NULL,NULL,NULL,NULL);
INSERT INTO `tb_dining_table` (`id`,`shop_id`,`table_no`,`table_name`,`type_id`,`zone_id`,`capacity`,`status`,`qr_scene`,`qr_code_url`,`create_time`,`update_time`) VALUES (5,13,'A05','A区·A05',1,1,4,1,NULL,NULL,NULL,NULL);
INSERT INTO `tb_dining_table` (`id`,`shop_id`,`table_no`,`table_name`,`type_id`,`zone_id`,`capacity`,`status`,`qr_scene`,`qr_code_url`,`create_time`,`update_time`) VALUES (6,13,'A06','A区·A06',1,1,4,1,NULL,NULL,NULL,NULL);
INSERT INTO `tb_dining_table` (`id`,`shop_id`,`table_no`,`table_name`,`type_id`,`zone_id`,`capacity`,`status`,`qr_scene`,`qr_code_url`,`create_time`,`update_time`) VALUES (7,13,'A07','A区·A07',1,1,4,1,NULL,NULL,NULL,NULL);
INSERT INTO `tb_dining_table` (`id`,`shop_id`,`table_no`,`table_name`,`type_id`,`zone_id`,`capacity`,`status`,`qr_scene`,`qr_code_url`,`create_time`,`update_time`) VALUES (8,13,'A08','A区·A08',1,1,4,1,NULL,NULL,NULL,NULL);
INSERT INTO `tb_dining_table` (`id`,`shop_id`,`table_no`,`table_name`,`type_id`,`zone_id`,`capacity`,`status`,`qr_scene`,`qr_code_url`,`create_time`,`update_time`) VALUES (9,13,'A09','A区·A09',1,1,4,1,NULL,NULL,NULL,NULL);
INSERT INTO `tb_dining_table` (`id`,`shop_id`,`table_no`,`table_name`,`type_id`,`zone_id`,`capacity`,`status`,`qr_scene`,`qr_code_url`,`create_time`,`update_time`) VALUES (10,13,'A10','A区·A10',1,1,4,1,NULL,NULL,NULL,NULL);
INSERT INTO `tb_dining_table` (`id`,`shop_id`,`table_no`,`table_name`,`type_id`,`zone_id`,`capacity`,`status`,`qr_scene`,`qr_code_url`,`create_time`,`update_time`) VALUES (11,13,'A11','A区·A11',1,1,4,1,NULL,NULL,NULL,NULL);
INSERT INTO `tb_dining_table` (`id`,`shop_id`,`table_no`,`table_name`,`type_id`,`zone_id`,`capacity`,`status`,`qr_scene`,`qr_code_url`,`create_time`,`update_time`) VALUES (12,13,'A12','A区·A12',1,1,4,1,NULL,NULL,NULL,NULL);
INSERT INTO `tb_dining_table` (`id`,`shop_id`,`table_no`,`table_name`,`type_id`,`zone_id`,`capacity`,`status`,`qr_scene`,`qr_code_url`,`create_time`,`update_time`) VALUES (13,13,'K01','K区·K01',2,2,6,1,NULL,NULL,NULL,NULL);
INSERT INTO `tb_dining_table` (`id`,`shop_id`,`table_no`,`table_name`,`type_id`,`zone_id`,`capacity`,`status`,`qr_scene`,`qr_code_url`,`create_time`,`update_time`) VALUES (14,13,'K02','K区·K02',2,2,6,1,NULL,NULL,NULL,NULL);
INSERT INTO `tb_dining_table` (`id`,`shop_id`,`table_no`,`table_name`,`type_id`,`zone_id`,`capacity`,`status`,`qr_scene`,`qr_code_url`,`create_time`,`update_time`) VALUES (15,13,'K03','K区·K03',2,2,6,1,NULL,NULL,NULL,NULL);
INSERT INTO `tb_dining_table` (`id`,`shop_id`,`table_no`,`table_name`,`type_id`,`zone_id`,`capacity`,`status`,`qr_scene`,`qr_code_url`,`create_time`,`update_time`) VALUES (16,13,'K04','K区·K04',2,2,6,1,NULL,NULL,NULL,NULL);
INSERT INTO `tb_dining_table` (`id`,`shop_id`,`table_no`,`table_name`,`type_id`,`zone_id`,`capacity`,`status`,`qr_scene`,`qr_code_url`,`create_time`,`update_time`) VALUES (17,13,'V01','V区·V01',3,3,10,1,NULL,NULL,NULL,NULL);
INSERT INTO `tb_dining_table` (`id`,`shop_id`,`table_no`,`table_name`,`type_id`,`zone_id`,`capacity`,`status`,`qr_scene`,`qr_code_url`,`create_time`,`update_time`) VALUES (18,13,'V02','V区·V02',3,3,10,1,NULL,NULL,NULL,NULL);
INSERT INTO `tb_dining_table` (`id`,`shop_id`,`table_no`,`table_name`,`type_id`,`zone_id`,`capacity`,`status`,`qr_scene`,`qr_code_url`,`create_time`,`update_time`) VALUES (19,13,'A13','A区·A13',1,1,4,1,NULL,NULL,'2026-09-19 05:29:58','2026-09-19 05:29:58');
