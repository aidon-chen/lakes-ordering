-- =============================================================================
-- 存酒取酒 · 删除 tb_wine_storage.storage_code（收尾步骤，勿提前执行！）
-- 前置条件（三者全满足才可执行本脚本）：
--   ① 25- 脚本已执行（storage_code 已改 NULL）
--   ② 后端新版本已发布（实体/Service/Vo 均不再引用 storage_code）
--   ③ 线上各端（顾客端/收银端/管理端）均已换新版，确认无处读写 storage_code
-- 内容：删除 storage_code 列（其唯一索引 uk_storage_code 随列一并删除）
-- 执行库：本地 siam_db ＋ 云库（两库都执行）
-- 影响：仅删 1 列；tb_wine_storage 现 3 行（开发版），无业务风险
-- =============================================================================

ALTER TABLE tb_wine_storage DROP COLUMN storage_code;

-- 执行后自检（应返回 0 行）
SELECT column_name FROM information_schema.columns
WHERE table_schema = 'siam_db' AND table_name = 'tb_wine_storage' AND column_name = 'storage_code';

-- 回滚（重建为可空列，历史值无法恢复——开发版 3 行无实际数据，可忽略）
-- ALTER TABLE tb_wine_storage ADD COLUMN storage_code varchar(16) NULL COMMENT '存酒码(唯一，已废弃)' AFTER remain_count;
