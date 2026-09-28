-- ============================================================
-- 31-成长值种子迁移：tb_member_billing_record(coin_type=6) → tb_member_points_log
-- ============================================================
-- 背景（2026-09-27 拍板）：「积分＝仅等级成长」只有一个概念，账本统一为
--   tb_member_points_log（package_points 模块，09-24 de3b310 起，
--   支付/充值/退款红冲三处入账均已接线）。
--   19 号 SQL 给演示号 17 造的成长值流水（billing coin_type=6）与
--   tb_member.growth 字段从此退役为死数，前端 C-08/C-03/C-01 已换源
--   到 /rest/member/points/myPoints|myLog（读 points_log）。
-- 本脚本：把 19 号种子流水搬进 points_log（保留原 create_time 与文案），
--   并把全会员 growth 校准为积分流水净额，使旧字段与账本一致（仅兜底展示用）。
--
-- type 映射（billing.type → points_log.biz_type）：
--   23 消费获得成长值 → 1 消费
--   24 退款扣回成长值 → 4 退款红冲
--   25 新用户注册赠送 → 2 活动
--
-- 幂等：biz_id 统一打 'seed-19sql-{原billing行id}' 标记，先删后插；
--   不触碰任何真实积分流水（biz_id 无该前缀的行原样保留）。
-- balance_after：按种子固定 6 行的时间序手算快照；⚠️ id 按**云端**实际自增值
--   （179→500/184→485/180→1105/183→404/181→824/182→844；本地为 185/190/186/189/187/188，
--   19 号种子两库各自插入 ⇒ id 不同、number/快照值相同。换库执行前先查实际 id）
-- 回滚：DELETE FROM tb_member_points_log WHERE biz_id LIKE 'seed-19sql-%';
--   （growth 校准语句重复执行结果一致，无需回滚）
-- 上云：⏳ 待用户确认后执行（DML，无 DDL）。
-- ============================================================

-- 1) 清理本脚本的旧迁移痕迹（幂等）
DELETE FROM tb_member_points_log WHERE member_id = 17 AND biz_id LIKE 'seed-19sql-%';

-- 2) 种子流水迁移（保留原 create_time/message；biz_type 按 23/24/25 → 1/4/2 映射）
INSERT INTO tb_member_points_log
  (member_id, biz_type, biz_id, change_points, balance_after, remark, create_time)
SELECT
  r.member_id,
  CASE r.type WHEN 23 THEN 1 WHEN 24 THEN 4 WHEN 25 THEN 2 ELSE 5 END AS biz_type,
  CONCAT('seed-19sql-', r.id) AS biz_id,
  IF(r.operate_type = 1, r.number, -r.number) AS change_points,
  CASE r.id WHEN 179 THEN 500 WHEN 184 THEN 485 WHEN 180 THEN 1105
            WHEN 183 THEN 404 WHEN 181 THEN 824 WHEN 182 THEN 844 END AS balance_after,
  r.message AS remark,
  r.create_time
FROM tb_member_billing_record r
WHERE r.member_id = 17 AND r.coin_type = 6;

-- 3) 全会员 growth 校准为积分流水净额（此后 growth 仅作兜底展示，账本以 points_log 为准）
UPDATE tb_member m
SET m.growth = IFNULL((
  SELECT SUM(r.change_points) FROM tb_member_points_log r WHERE r.member_id = m.id
), 0);

-- 4) 自检：预期 member 17 净额 = 844(种子) + 123(真实流水，随本地测试增长) ；
--    SELECT 里 balance_after 末行应等于该会员 SUM(change_points)
SELECT r.member_id, COUNT(*) AS rows_cnt, SUM(r.change_points) AS net,
       (SELECT m2.growth FROM tb_member m2 WHERE m2.id = r.member_id) AS member_growth
FROM tb_member_points_log r
WHERE r.biz_id LIKE 'seed-19sql-%' OR r.member_id = 17
GROUP BY r.member_id;
