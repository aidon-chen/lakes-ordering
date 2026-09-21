-- =============================================================================
-- 存酒临期提醒记录表（批次 F · 收银端 W-19）
-- 背景：需求 C-16「临期与过期提醒」；通知方式按用户拍板 = **小程序端弹窗提示**（不做短信）
--       → 记录「何时对哪条存酒弹过窗」，收银端 W-19 的「通知状态」列由此表驱动
-- 幂等：IF NOT EXISTS，可重复执行
-- =============================================================================

CREATE TABLE IF NOT EXISTS `tb_wine_remind_log` (
  `id`                 int(11)     NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `shop_id`            int(11)     NOT NULL                COMMENT '门店id',
  `member_id`          int(11)     NOT NULL                COMMENT '会员id',
  `storage_id`         int(11)     NOT NULL                COMMENT '存酒记录id（tb_wine_storage.id）',
  `channel`            int(2)      DEFAULT '1'             COMMENT '通知渠道 1=小程序弹窗',
  `expire_date_at_notify` date     DEFAULT NULL            COMMENT '提醒时的有效期（快照）',
  `create_time`        datetime    DEFAULT NULL            COMMENT '提醒时间',
  PRIMARY KEY (`id`),
  KEY `idx_storage` (`storage_id`),
  KEY `idx_shop_create` (`shop_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='存酒临期提醒记录表（小程序弹窗）';

-- 执行后自检（应返回 1 行）
SELECT table_name, table_comment FROM information_schema.tables
WHERE table_schema = 'siam_db' AND table_name = 'tb_wine_remind_log';

-- 回滚（与上面严格互逆）
-- DROP TABLE IF EXISTS `tb_wine_remind_log`;
