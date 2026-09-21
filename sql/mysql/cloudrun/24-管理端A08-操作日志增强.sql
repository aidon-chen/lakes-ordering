-- ============================================================================
-- 24-管理端 A-08 操作日志增强：tb_operate_log 加 business_type / request_param / response_result
-- ============================================================================
-- 背景：A-08 v1.0 已交付（表 + AOP + 两页），对照开源实现（RuoYi sys_oper_log、
--       yudao system_operate_log）存在三项能力缺口：
--   1) 无「操作类型」枚举 —— RuoYi 有 business_type（新增/修改/删除/授权/导出/导入），
--      本项目只有 module + action 文本，无法按增删改查维度筛选与统计；
--   2) 无「请求参数 / 返回结果」—— RuoYi 记 oper_param / json_result，
--      本项目非变更类操作（下单、导出、取酒）的详情页「变更内容」恒为空；
--   3) business_id 恒为 null（DDL 已预留列，本次不在范围内）。
--
-- 字段命名：沿用 RuoYi sys_oper_log.business_type 口径（不叫 operate_type，
--          避免与 tb_member_billing_record.operate_type「1=加 2=减」语义撞车）。
-- 新增枚举：
--   business_type  0=其它 1=新增 2=修改 3=删除 4=授权 5=导出 6=导入
--
-- 说明：本次仅加列，不改动既有列与既有数据；request_param / response_result 为 text，
--       写入侧（OperateLogAop）已做敏感字段脱敏与长度截断。
-- 幂等：加列前判 information_schema，可重复执行。
-- 回滚：见文件末尾注释。
-- ============================================================================

-- 1) 操作类型
SET @exist := (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = 'tb_operate_log' AND column_name = 'business_type');
SET @ddl := IF(@exist = 0,
  'ALTER TABLE tb_operate_log ADD COLUMN business_type tinyint(2) DEFAULT 0 COMMENT ''操作类型 0=其它 1=新增 2=修改 3=删除 4=授权 5=导出 6=导入'' AFTER action',
  'SELECT ''business_type 列已存在，跳过'' AS notice');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) 请求参数（JSON，敏感字段以 *** 脱敏）
SET @exist := (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = 'tb_operate_log' AND column_name = 'request_param');
SET @ddl := IF(@exist = 0,
  'ALTER TABLE tb_operate_log ADD COLUMN request_param text DEFAULT NULL COMMENT ''请求参数(JSON,敏感字段已脱敏,超长截断)'' AFTER business_id',
  'SELECT ''request_param 列已存在，跳过'' AS notice');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3) 返回结果（JSON，超长截断）
SET @exist := (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = 'tb_operate_log' AND column_name = 'response_result');
SET @ddl := IF(@exist = 0,
  'ALTER TABLE tb_operate_log ADD COLUMN response_result text DEFAULT NULL COMMENT ''返回结果(JSON,超长截断)'' AFTER request_param',
  'SELECT ''response_result 列已存在，跳过'' AS notice');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4) 执行后自检（应返回 3 行）
SELECT column_name, column_type, column_comment
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'tb_operate_log'
  AND column_name IN ('business_type', 'request_param', 'response_result');

-- 回滚（与上面严格互逆）
-- ALTER TABLE tb_operate_log DROP COLUMN business_type;
-- ALTER TABLE tb_operate_log DROP COLUMN request_param;
-- ALTER TABLE tb_operate_log DROP COLUMN response_result;