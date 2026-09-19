-- ============================================================
-- 回滚：19-会员成长值-加列与种子数据.sql
-- 执行前状态：tb_member 无 growth 列；tb_member_billing_record 无 coin_type=6 的行
-- ============================================================

-- 1) 删除演示号成长值流水
DELETE FROM tb_member_billing_record WHERE coin_type = 6;

-- 2) 删除 growth 列（若不存在则跳过）
SET @exist := (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = 'tb_member' AND column_name = 'growth');
SET @ddl := IF(@exist = 1,
  'ALTER TABLE tb_member DROP COLUMN growth',
  'SELECT ''growth 列本来就不存在，跳过'' AS notice');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3) 自检
SELECT COUNT(*) AS growth_ledger_rows FROM tb_member_billing_record WHERE coin_type = 6;
