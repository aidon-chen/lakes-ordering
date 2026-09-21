-- =============================================================================
-- 管理端 A-08 操作日志增强：tb_operate_log 加 business_type / request_param / response_result
-- 背景与枚举口径见 24-管理端A08-操作日志增强.sql
-- 幂等：重复执行报 1060 Duplicate column，可忽略
-- =============================================================================

ALTER TABLE tb_operate_log
  ADD COLUMN business_type   tinyint(2) DEFAULT 0 COMMENT '操作类型 0=其它 1=新增 2=修改 3=删除 4=授权 5=导出 6=导入' AFTER action,
  ADD COLUMN request_param   text       DEFAULT NULL COMMENT '请求参数(JSON,敏感字段已脱敏,超长截断)' AFTER business_id,
  ADD COLUMN response_result text       DEFAULT NULL COMMENT '返回结果(JSON,超长截断)' AFTER request_param;

-- 执行后自检（应返回 3 行）
SELECT column_name, column_type, column_comment
FROM information_schema.columns
WHERE table_schema = 'siam_db' AND table_name = 'tb_operate_log'
  AND column_name IN ('business_type', 'request_param', 'response_result');

-- 回滚（与上面严格互逆）
-- ALTER TABLE tb_operate_log DROP COLUMN business_type;
-- ALTER TABLE tb_operate_log DROP COLUMN request_param;
-- ALTER TABLE tb_operate_log DROP COLUMN response_result;