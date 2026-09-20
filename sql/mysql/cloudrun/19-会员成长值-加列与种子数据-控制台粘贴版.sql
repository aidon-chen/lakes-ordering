-- ============================================================
-- 19-会员成长值 · 控制台粘贴版（微信云托管控制台 → MySQL → 数据库管理）
-- 库：siam_db
-- 用法：整段粘贴执行；若第 ① 条报 1060 Duplicate column name 'growth'，
--       说明列已加过 —— 跳过①，继续执行 ②③④⑤ 即可。
-- 特点：不用 PREPARE/用户变量（防控制台逐条执行丢会话）；
--       按手机号定位演示会员（不写死 id=17），跨库安全。
-- ============================================================

-- ① 加列（已存在则报 1060，跳过即可）
ALTER TABLE tb_member
  ADD COLUMN growth INT NOT NULL DEFAULT 0
  COMMENT '累计成长值(只增不减,仅用于判定会员等级)'
  AFTER total_consume_points;

-- ② 清掉该会员旧的成长值流水（幂等，首次执行影响 0 行）
DELETE FROM tb_member_billing_record
 WHERE coin_type = 6
   AND member_id = (SELECT id FROM tb_member WHERE mobile = '15100000000' ORDER BY id DESC LIMIT 1);

-- ③ 播成长值流水（复用 tb_member_billing_record，新增 coin_type=6）
--    type 23=消费获得 / 24=退款扣回 / 25=新客注册赠送；operate_type 1=加 2=减
INSERT INTO tb_member_billing_record
  (member_id, type, operate_type, coin_type, number, service_fee, message,
   order_id, points_mall_order_id, is_return, is_settled, create_time)
SELECT m.id, v.t, v.op, 6, v.n, 0.00, v.msg, NULL, NULL, 0, 0, v.dt
FROM tb_member m
JOIN (
            SELECT 25 t, 1 op, 500.00 n, '新客注册赠送成长值'         msg, DATE_SUB(NOW(), INTERVAL 18 DAY) dt
  UNION ALL SELECT 23,   1,    620.00, '消费获得成长值-精酿拼盘',     DATE_SUB(NOW(), INTERVAL 11 DAY)
  UNION ALL SELECT 23,   1,    420.00, '消费获得成长值-午夜世涛',     DATE_SUB(NOW(), INTERVAL  7 DAY)
  UNION ALL SELECT 23,   1,     20.00, '消费获得成长值-桂花小麦艾尔', NOW()
  UNION ALL SELECT 24,   2,    701.00, '退款扣回成长值-精酿拼盘',     DATE_SUB(NOW(), INTERVAL  9 DAY)
  UNION ALL SELECT 24,   2,     15.00, '退款扣回成长值-午夜世涛',     DATE_SUB(NOW(), INTERVAL 14 DAY)
) v
WHERE m.mobile = '15100000000';

-- ④ 回填当前成长值 = 该会员成长值流水的净额（不写死数字）
UPDATE tb_member m
   SET m.growth = (SELECT IFNULL(SUM(IF(r.operate_type = 1, r.number, -r.number)), 0)
                     FROM tb_member_billing_record r
                    WHERE r.member_id = m.id AND r.coin_type = 6)
 WHERE m.mobile = '15100000000';

-- ⑤ 自检（期望：growth=844；ledger_rows=6）
SELECT id, mobile, growth FROM tb_member WHERE mobile = '15100000000';
SELECT COUNT(*) AS ledger_rows,
       SUM(IF(operate_type = 1, number, 0)) AS positive_total,
       SUM(IF(operate_type = 2, number, 0)) AS deduct_total
  FROM tb_member_billing_record
 WHERE coin_type = 6
   AND member_id = (SELECT id FROM tb_member WHERE mobile = '15100000000' ORDER BY id DESC LIMIT 1);
