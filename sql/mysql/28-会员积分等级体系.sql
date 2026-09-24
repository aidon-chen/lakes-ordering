-- ============================================================
-- 28 · 会员积分等级体系（优惠券三类型·会员门槛前置工程）
-- 设计文档：order-project-整理/01-solution/优惠券三类型与会员积分等级体系-设计文档-v1.0.md
-- 拍板记录：2026-09-23 —— 积分与「用户↔会员等级」独立成表；
--           消费 ¥1=1分；充值同比例给分；退款红冲扣回；
--           活动分值走本配置表（积分管理界面可改，不硬编码）；
--           积分不可抵扣消费，仅驱动等级。
-- 兼容性：不动 tb_member.points/total_points/growth 旧字段（历史遗留，
--         旧注册赠送逻辑原样保留）；等级一律从本流水表 SUM 实时计算。
-- ============================================================

-- 1) 会员积分流水表（每笔获取/红冲一行，可审计）
CREATE TABLE IF NOT EXISTS `tb_member_points_log` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `member_id` int(11) NOT NULL COMMENT '会员id',
  `biz_type` tinyint(4) NOT NULL COMMENT '业务类型 1=消费 2=活动 3=充值 4=退款红冲 5=后台调整',
  `biz_id` varchar(64) DEFAULT NULL COMMENT '关联业务单号(订单号/充值单号/活动标识)',
  `change_points` int(11) NOT NULL COMMENT '变动积分，正数加负数减',
  `balance_after` int(11) NOT NULL COMMENT '变动后累计积分快照',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注(展示用)',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_member_id` (`member_id`),
  KEY `idx_biz` (`biz_type`,`biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='会员积分流水表';

-- 2) 会员等级阈值配置表（积分管理后台可改）
CREATE TABLE IF NOT EXISTS `tb_member_level_setting` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `level` tinyint(4) NOT NULL COMMENT '等级 1~5',
  `name` varchar(20) NOT NULL COMMENT '等级名称(青铜/白银/黄金/铂金/钻石)',
  `threshold` int(11) NOT NULL COMMENT '累计积分达到该值即为该等级',
  `benefits` varchar(255) DEFAULT NULL COMMENT '等级权益说明',
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_level` (`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='会员等级阈值配置表';

-- 3) 积分获取规则配置表（积分管理后台可改，业务侧只读）
CREATE TABLE IF NOT EXISTS `tb_member_points_setting` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `item_key` varchar(40) NOT NULL COMMENT '配置键',
  `item_value` int(11) NOT NULL COMMENT '配置值(比例或固定分)',
  `item_desc` varchar(100) DEFAULT NULL COMMENT '说明',
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_item_key` (`item_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='积分获取规则配置表';

-- 初始数据：等级阈值（对齐前端 member-level.js）
INSERT INTO `tb_member_level_setting` (`level`,`name`,`threshold`,`benefits`,`update_time`) VALUES
  (1,'青铜',0,  NULL,NOW()),
  (2,'白银',300,'白银会员专享优惠',NOW()),
  (3,'黄金',600,'黄金会员专享优惠',NOW()),
  (4,'铂金',1000,'铂金会员专享优惠',NOW()),
  (5,'钻石',2000,'钻石会员专享优惠',NOW());

-- 初始数据：积分规则（活动分值为占位默认值，待积分管理界面自定义）
INSERT INTO `tb_member_points_setting` (`item_key`,`item_value`,`item_desc`,`update_time`) VALUES
  ('consume_rate',     1, '消费积分比例：每实付1元得X分', NOW()),
  ('recharge_rate',    1, '充值积分比例：每充值1元得X分', NOW()),
  ('register_points', 50, '新用户注册赠送积分', NOW()),
  ('first_order_points',30,'完成首单赠送积分', NOW()),
  ('appraise_points',  10, '评价订单赠送积分', NOW()),
  ('invite_points',   100, '邀请好友注册并下单赠送积分', NOW());
