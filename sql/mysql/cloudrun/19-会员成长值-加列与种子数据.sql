-- ============================================================
-- 19-会员成长值：tb_member 加 growth 列 + 演示号成长值流水种子数据
-- ============================================================
-- 背景：《点餐消费系统-开发方案》§七「积分体系改会员成长值（不消费/不兑换，只判等级）」
--       → 成长值是一等概念，但工程里既无列也无流水，C-08「我的成长值」页无源可依。
-- 本脚本：给 tb_member 加 growth 列（累计成长值），并给演示号 member_id=17
--       造一份**自洽**的成长值流水（复用 tb_member_billing_record，新增 coin_type=6=成长值）。
--
-- 约定（新增枚举，仅语义扩展，不改列定义）：
--   tb_member_billing_record.coin_type  6 = 成长值
--   tb_member_billing_record.type      23 = 消费获得成长值
--                                      24 = 退款扣回成长值
--                                      25 = 新用户注册赠送成长值
--   operate_type                        1 = 加  2 = 减
--
-- 数据自洽性（演示号 17）：
--   正向合计 500 + 620 + 420 + 20 = 1560   → 「累计成长值」
--   扣回合计 701 + 15            =  716
--   当前成长值 = 1560 - 716      =  844    → 写入 tb_member.growth
--   消费获得   = 620 + 420 + 20  = 1060
--   今日获得   = 20（该行用 NOW()，任何日期跑都算「今天」）
--   等级 V4 阈值 1000 → 844 属 V3，距 V4 还 156
--
-- 幂等：加列前判 information_schema；流水先按 (member_id, coin_type=6) 删除再插入。
-- 回滚：sql/mysql/archive/tb_member-growth-加列前-备份-20260919.sql
-- ============================================================

-- 1) 加列（MySQL 无 ADD COLUMN IF NOT EXISTS，用预处理语句判存在）
SET @exist := (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = 'tb_member' AND column_name = 'growth');
SET @ddl := IF(@exist = 0,
  'ALTER TABLE tb_member ADD COLUMN growth INT NOT NULL DEFAULT 0 COMMENT ''累计成长值(只增不减,仅用于判定会员等级)'' AFTER total_consume_points',
  'SELECT ''growth 列已存在，跳过'' AS notice');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) 演示号 member_id = 17 的成长值流水（先清后插，保证幂等）
DELETE FROM tb_member_billing_record WHERE member_id = 17 AND coin_type = 6;

INSERT INTO tb_member_billing_record
  (member_id, type, operate_type, coin_type, number, service_fee, message, order_id, points_mall_order_id, is_return, is_settled, create_time)
VALUES
  (17, 25, 1, 6, 500.00, 0.00, '新客注册赠送成长值',            NULL, NULL, 0, 0, DATE_SUB(NOW(), INTERVAL 18 DAY)),
  (17, 23, 1, 6, 620.00, 0.00, '消费获得成长值-精酿拼盘',        NULL, NULL, 0, 0, DATE_SUB(NOW(), INTERVAL 11 DAY)),
  (17, 23, 1, 6, 420.00, 0.00, '消费获得成长值-午夜世涛',        NULL, NULL, 0, 0, DATE_SUB(NOW(), INTERVAL  7 DAY)),
  (17, 23, 1, 6,  20.00, 0.00, '消费获得成长值-桂花小麦艾尔',    NULL, NULL, 0, 0, NOW()),
  (17, 24, 2, 6, 701.00, 0.00, '退款扣回成长值-精酿拼盘',        NULL, NULL, 0, 0, DATE_SUB(NOW(), INTERVAL  9 DAY)),
  (17, 24, 2, 6,  15.00, 0.00, '退款扣回成长值-午夜世涛',        NULL, NULL, 0, 0, DATE_SUB(NOW(), INTERVAL 14 DAY));

-- 3) 回填当前成长值 = 该会员成长值流水的净额（不写死数字，与流水自洽）
UPDATE tb_member m
SET m.growth = (
  SELECT IFNULL(SUM(IF(r.operate_type = 1, r.number, -r.number)), 0)
  FROM tb_member_billing_record r
  WHERE r.member_id = m.id AND r.coin_type = 6
)
WHERE m.id = 17;

-- 4) 自检
SELECT id, username, mobile, growth FROM tb_member WHERE id = 17;
SELECT type, operate_type, number, message,
       DATE(create_time) AS d
FROM tb_member_billing_record
WHERE member_id = 17 AND coin_type = 6
ORDER BY id DESC;
