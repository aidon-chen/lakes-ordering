-- ============================================================================
-- 批次D：tb_order 补「出品状态」列  —— 云托管库控制台粘贴版
-- 背景：需求书 M-04「出品进度跟踪」；工程原零出品字段。
--       bar-mvp-017 已上线的实体已映射该列，**云库不加会报 Unknown column**
--       （现象：/rest/merchant/order/updateProductionStatus 返回「系统异常」）
-- 用法：微信云托管控制台 → 数据库 → SQL 执行 → 粘贴本文件 → 执行
-- 幂等：重复执行会报 "Duplicate column"，属预期，无副作用
-- ============================================================================

ALTER TABLE tb_order
  ADD COLUMN production_status TINYINT NOT NULL DEFAULT 1
  COMMENT '出品状态 1=待制作 2=出品中 3=已出品（批次D新增，服务 M-04 出品进度跟踪）'
  AFTER is_printed;

-- 执行后自检（应返回 1 行）
SELECT column_name, column_type, column_default, column_comment
FROM information_schema.columns
WHERE table_schema = 'siam_db' AND table_name = 'tb_order' AND column_name = 'production_status';

-- 回滚（与上面严格互逆）
-- ALTER TABLE tb_order DROP COLUMN production_status;
