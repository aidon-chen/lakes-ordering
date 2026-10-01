-- ============================================================================
-- 37-商城分类加禁用列.sql
-- 编号说明：36 已被《36-删除商家推荐商品表.sql》占用（上批结转、待执行），本条顺延为 37
-- ============================================================================
-- 批次：2026-09-29/30「商城分类表格改造」A3 启用/禁用恢复
-- 计划文档：order-project/docs/_业务/商城分类表格改造-实施计划-v1.0.md
-- 配套代码：siam-server 正本 siam-system —— PointsMallMenu 实体加 isDisabled、
--           PointsMallMenuMapper(.java/.xml) 补映射与过滤、顾客端 listWithGoods
--           加 AND m.is_disabled = 0（禁用分类对顾客端隐藏）
-- 前置备份：本地已备份表结构
--   order-project/01-solution/local-run/backup/2026-09-30-tb_points_mall_menu-schema-pre-37.sql
--   上云前须对生产库另做一次备份
-- 状态：⏳ 本地已执行（09-30 23:5x）；【云端未执行，须用户确认后走铁律 0】
-- ============================================================================

-- ▼▼▼ 生产执行区 ▼▼▼
-- 商城分类加禁用开关：0=正常 1=禁用；存量 12 行默认 0，顾客端不受影响
ALTER TABLE tb_points_mall_menu
  ADD COLUMN is_disabled tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否禁用 0=正常 1=禁用' AFTER sort_number;

-- 说明：
-- · 列名与商品表「状态」语义对齐（禁用≠删除）；管理端列表/筛选/表单随本批前端一起上。
-- · 回滚：ALTER TABLE tb_points_mall_menu DROP COLUMN is_disabled;
--   （回滚前须先把配套前端/后端版本一并回滚，否则新前端读不到该列会全显「正常」）
