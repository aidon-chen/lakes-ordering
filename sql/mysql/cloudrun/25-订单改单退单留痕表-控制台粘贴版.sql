-- =============================================================================
-- 订单改单/退单留痕表（批次 G · 收银端 W-13）
-- 背景：需求 M-02「接单、改单、退单，订单状态流转」。退款数据模型**已存在**
--       （tb_order_refund / _goods / _process，21 列 7 状态，本项目暂未使用）→ 退单复用它；
--       但「改单」没有对应表，故本表专门记录改单/退单动作留痕（谁、何时、改了什么、差额多少）。
-- 幂等：IF NOT EXISTS，可重复执行
-- =============================================================================

CREATE TABLE IF NOT EXISTS `tb_order_change_log` (
  `id`             int(11)      NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `shop_id`        int(11)      NOT NULL                COMMENT '门店id',
  `order_id`       int(11)      NOT NULL                COMMENT '订单id',
  `order_no`       varchar(50)  DEFAULT NULL            COMMENT '订单编号',
  `change_type`    int(2)       NOT NULL                COMMENT '变更类型 1=改单 2=退单',
  `before_amount`  decimal(10,2) DEFAULT NULL           COMMENT '变更前应收',
  `after_amount`   decimal(10,2) DEFAULT NULL           COMMENT '变更后应收（退单时为 0 或不填）',
  `diff_amount`    decimal(10,2) DEFAULT NULL           COMMENT '差额（正=需补收，负=应退，0=无差额）',
  `refund_amount`  decimal(10,2) DEFAULT NULL           COMMENT '退款金额（退单用）',
  `refund_way`     int(2)       DEFAULT NULL            COMMENT '退款方式 1=全额 2=部分（退单用）',
  `reason_code`    int(4)       DEFAULT NULL            COMMENT '原因码（退单复用 tb_order_refund.refund_reason 枚举）',
  `reason_text`    varchar(200) DEFAULT NULL            COMMENT '原因文字（改单可为空）',
  `detail_snapshot` text                                COMMENT '变更前后明细快照（JSON，便于事后追溯）',
  `operator_id`    int(11)      DEFAULT NULL            COMMENT '操作人（商家id）',
  `create_time`    datetime     DEFAULT NULL            COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_shop_create` (`shop_id`, `create_time`),
  KEY `idx_type` (`change_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='订单改单/退单留痕表';

-- 执行后自检（应返回 1 行）
SELECT table_name, table_comment FROM information_schema.tables
WHERE table_schema = 'siam_db' AND table_name = 'tb_order_change_log';

-- 回滚（与上面严格互逆）
-- DROP TABLE IF EXISTS `tb_order_change_log`;
