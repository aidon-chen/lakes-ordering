-- ============================================================
-- 【云托管控制台粘贴版】
-- 用法：云托管控制台 → 环境 prod-d6gd6a2twe27f592a → MySQL → 数据库管理 → SQL 窗口，整段粘贴执行。
-- 幂等：可重复执行；不影响配送单（pickup_code 为 NULL）。
-- ⚠️ 值里不能出现分号（控制台按 ; 切分语句）—— 本文件已校验通过。
-- ⚠️ 正文与 34-自提码唯一键改当日复合.sql 逐字一致，仅抬头不同；改一处要同步另一处。
-- 34 · 商城自提码唯一键改为「当日 + 码」复合唯一
-- 2026-09-25
--
-- 【现象】云端商城（积分商城）下单/模拟支付必失败，返回「系统异常，请稍后重试」；
--        堂食点单链路正常。
--
-- 【根因】取餐号是「当日序列」，自提码却按「全局唯一」约束 —— 两者语义打架：
--   · 取餐号生成：PointsMallOrderMapper.findMaxQueueNo()
--       select max(queue_no) from tb_points_mall_order where to_days(create_time) = to_days(now())
--     ⇒ 每天从 1 重算 ⇒ 自提码 A-001 每天都在表里再出现一次
--   · 表约束：uk_pickup_code 是【全局唯一】
--   ⇒ 每天第一张门店自提单都会撞「任意历史日」的同名码：
--       Duplicate entry 'A-001' for key 'tb_points_mall_order.uk_pickup_code'
--   ⇒ MockPayController 类级 @Transactional(rollbackFor = Exception.class) 整单回滚
--   ⇒ 商城下单/付款必失败，且【重试同样失败】（当天表内无行，max 恒为 NULL，永远算 A-001）
--
--   对照：堂食 tb_order 用同一套按日取餐号，但【没有】唯一键 ⇒ 所以「点单跑通、商城跑不通」。
--
-- 【处置】新增 STORED 生成列 pickup_date = DATE(create_time)，
--        唯一键由 (pickup_code) 改为 (pickup_date, pickup_code)：
--          · 同日唯一  → 保留原防重意图（同日并发/重复提交仍会被拦下）
--          · 跨天可重复 → 与「当日取餐号」语义、与堂食 tb_order 的口径一致
--
-- 【幂等】逐项查 information_schema 后再 DDL，可反复执行。
-- 【安全】先加新键、后删旧键 —— 中间任何一步失败都不会出现「无唯一键保护」的窗口。
-- 【部署】本地库与云库都要执行（本地同样会在下一张自提单上触发）。
-- ============================================================

-- 0) 前置校验（只读）：按 (日期, 自提码) 查重。
--    ⚠️ 若这里返回任何行，说明已存在「同一天同一个码」的脏数据，第 3 步会失败 —— 先人工处理。
SELECT DATE(`create_time`) AS `pickup_date`, `pickup_code`, COUNT(*) AS `dup_cnt`
  FROM `tb_points_mall_order`
 WHERE `pickup_code` IS NOT NULL
 GROUP BY DATE(`create_time`), `pickup_code`
HAVING `dup_cnt` > 1;

-- 1) 新增生成列 pickup_date（幂等：列已存在则跳过）
--    用 STORED 而非 VIRTUAL：旧版 MySQL 对 VIRTUAL 列建唯一键支持不稳，STORED 更保险（本表数据量很小）
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_points_mall_order' AND COLUMN_NAME = 'pickup_date');
SET @ddl = IF(@c = 0, 'ALTER TABLE `tb_points_mall_order` ADD COLUMN `pickup_date` DATE GENERATED ALWAYS AS (DATE(`create_time`)) STORED COMMENT ''自提码所属日期(与 pickup_code 组成"当日唯一"键)'' AFTER `pickup_deadline`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) 先加新键：当日复合唯一（幂等：索引已存在则跳过）
--    允许多个 NULL ⇒ 配送单（pickup_code 为 NULL）不受影响
SET @c = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_points_mall_order' AND INDEX_NAME = 'uk_pickup_date_code');
SET @ddl = IF(@c = 0, 'ALTER TABLE `tb_points_mall_order` ADD UNIQUE KEY `uk_pickup_date_code` (`pickup_date`,`pickup_code`)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) 再删旧键：全局单列唯一（幂等：不存在则跳过）
SET @c = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_points_mall_order' AND INDEX_NAME = 'uk_pickup_code');
SET @ddl = IF(@c > 0, 'ALTER TABLE `tb_points_mall_order` DROP INDEX `uk_pickup_code`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4) 复核：应看到 uk_pickup_date_code(pickup_date, pickup_code) 且不再有 uk_pickup_code
SHOW INDEX FROM `tb_points_mall_order`;

-- ============================================================
-- 【回滚】恢复成全局唯一（注意：若已有跨天重码数据，回滚会失败，需先清理）
--   ALTER TABLE `tb_points_mall_order` DROP INDEX `uk_pickup_date_code`;
--   ALTER TABLE `tb_points_mall_order` ADD UNIQUE KEY `uk_pickup_code` (`pickup_code`);
--   ALTER TABLE `tb_points_mall_order` DROP COLUMN `pickup_date`;
-- ============================================================
