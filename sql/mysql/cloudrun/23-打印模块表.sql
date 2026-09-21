-- =============================================================================
-- 打印模块两张表（批次 G · 收银端 W-25 打印队列与告警 / W-21 打印模板与补打记录）
-- 背景：工程侧「队列表 + 模板表」此前**完全不存在**（计划里标注「缺表缺服务」）。
-- 设计：tb_print_task 一张表同时承担
--   · W-25 队列与告警（按 status 过滤 + retry_count/next_retry_time 重试）
--   · W-21 补打记录（整表即打印尝试流水，is_reprint/operator_id 区分补打与操作人）
-- 幂等：IF NOT EXISTS，可重复执行
-- =============================================================================

CREATE TABLE IF NOT EXISTS `tb_print_template` (
  `id`           int(11)      NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `shop_id`      int(11)      NOT NULL                COMMENT '门店id',
  `name`         varchar(64)  DEFAULT NULL            COMMENT '模板名',
  `scene`        int(2)       DEFAULT NULL            COMMENT '小票类型 1=接单 2=出品 3=结账 4=自提',
  `paper_width`  int(4)       DEFAULT '58'            COMMENT '纸宽 58 / 80 (mm)',
  `footer_text`  varchar(200) DEFAULT NULL            COMMENT '页脚文字',
  `copies`       int(4)       DEFAULT '1'             COMMENT '打印份数',
  `show_qr`      tinyint(1)   DEFAULT '1'             COMMENT '是否打印二维码',
  `is_default`   tinyint(1)   DEFAULT '0'             COMMENT '是否该类型默认模板',
  `create_time`  datetime     DEFAULT NULL            COMMENT '创建时间',
  `update_time`  datetime     DEFAULT NULL            COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_shop_scene` (`shop_id`, `scene`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='打印模板表';

CREATE TABLE IF NOT EXISTS `tb_print_task` (
  `id`               int(11)      NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `shop_id`          int(11)      NOT NULL                COMMENT '门店id',
  `order_no`         varchar(64)  DEFAULT NULL            COMMENT '业务单号',
  `ticket_type`      int(2)       DEFAULT NULL            COMMENT '小票类型 1=接单 2=出品 3=结账 4=自提',
  `printer_id`       int(11)      DEFAULT NULL            COMMENT '目标打印机id',
  `template_id`      int(11)      DEFAULT NULL            COMMENT '所用模板id',
  `copies`           int(4)       DEFAULT '1'             COMMENT '打印份数',
  `content`          text                                 COMMENT '打印内容快照',
  `status`           int(2)       DEFAULT '1'             COMMENT '状态 1=待打印 2=打印中 3=成功 4=失败 5=已忽略',
  `retry_count`      int(4)       DEFAULT '0'             COMMENT '已重试次数',
  `max_retry`        int(4)       DEFAULT '3'             COMMENT '最大重试次数',
  `fail_reason`      varchar(200) DEFAULT NULL            COMMENT '失败原因',
  `dispatch_time`    datetime     DEFAULT NULL            COMMENT '下发时间',
  `next_retry_time`  datetime     DEFAULT NULL            COMMENT '下次重试时间',
  `print_time`       datetime     DEFAULT NULL            COMMENT '打印成功时间',
  `operator_id`      int(11)      DEFAULT NULL            COMMENT '操作人id（补打时记录）',
  `is_reprint`       tinyint(1)   DEFAULT '0'             COMMENT '是否补打',
  `create_time`      datetime     DEFAULT NULL            COMMENT '创建时间',
  `update_time`      datetime     DEFAULT NULL            COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_shop_status` (`shop_id`, `status`),
  KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='打印任务表（队列 + 补打记录）';

-- 执行后自检（应返回 2 行）
SELECT table_name, table_comment FROM information_schema.tables
WHERE table_schema = 'siam_db' AND table_name IN ('tb_print_template', 'tb_print_task');

-- 回滚（与上面严格互逆）
-- DROP TABLE IF EXISTS `tb_print_task`;
-- DROP TABLE IF EXISTS `tb_print_template`;
