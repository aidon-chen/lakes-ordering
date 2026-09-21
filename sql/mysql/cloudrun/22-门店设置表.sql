-- =============================================================================
-- 门店营业设置表（批次 G · 收银端 W-23）
-- 背景：W-23 的 9 项设置（营业状态/订单提醒/自动接单/出品超时/云打印/本地打印/打印模式/默认打印机）
--       工程**没有任何存储** —— tb_setting 是**平台级单行表**（积分、配送费），与门店营业设置无关；
--       tb_shop 只有 checkout_printer_id / kitchen_total_order_printer_id 两个打印机绑定，
--       其 status 是「上下架」而非「营业/打烊」。
-- 设计：键值对，避免给 tb_shop 这个高频实体反复加列（本季已因缺列出过一次线上事故）
-- 幂等：IF NOT EXISTS，可重复执行
-- =============================================================================

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

-- 执行后自检（应返回 1 行）
SELECT table_name, table_comment FROM information_schema.tables
WHERE table_schema = 'siam_db' AND table_name = 'tb_shop_setting';

-- 回滚（与上面严格互逆）
-- DROP TABLE IF EXISTS `tb_shop_setting`;
