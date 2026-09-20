-- =============================================================================
-- 批次 D：订单出品进度（需求书 M-04「出品进度跟踪」）
-- 背景：工程原无任何出品字段（tb_order 仅 is_printed，tb_order_detail 无出品列）
--      → W-11「出品中」标签页与 W-14「出品进度看板（待制作/出品中/已出品）」无数据源
-- 影响：新增 1 列；TINYINT DEFAULT 1（默认「待制作」，存量订单自动落到该态）
-- 执行：本地 + 云库（云库走控制台粘贴版 / 外网连接，按既有流程）
-- =============================================================================
ALTER TABLE tb_order
  ADD COLUMN production_status TINYINT NOT NULL DEFAULT 1
  COMMENT '出品状态 1=待制作 2=出品中 3=已出品（批次D新增，服务 M-04 出品进度跟踪）'
  AFTER is_printed;

-- 回滚（与上面严格互逆）
-- ALTER TABLE tb_order DROP COLUMN production_status;
