-- ============================================================
-- 18-单店模式-其余门店下架.sql
-- 目标: 顾客端只用一家店（鹿点 / shop_id=13）。
--       把其余门店 tb_shop.status 置为 3（已下架）→ 顾客端 /rest/shop/list 只取 status=2，
--       于是点单页自动选店（menu/index.vue:911 取 records[0]）必定落到鹿点，看到酒水菜单。
-- 生成: 2026-09-19 15:46:06
-- 为什么不用 DELETE: 这些店名下有 195 个商品与历史订单（tb_goods / tb_order / tb_dining_table 等），
--       物理删除会留下孤儿数据、并可能让商家端/管理端查询报错；改 status 可一键恢复、零副作用。
-- 幂等: 是（status=2 的行才改）
-- 回滚: sql/mysql/archive/tb_shop-单店模式前-备份-20260919.sql
-- 影响面: 顾客端门店列表只剩鹿点；商家端/管理端仍能看到这些店（只是「已下架」）
-- ============================================================

SET NAMES utf8mb4;

-- 其余门店全部下架（只保留 status=2 的鹿点 13）
UPDATE `tb_shop` SET `status`=3, `update_time`=NOW() WHERE `id` <> 13 AND `status` = 2;

-- 自检：应当只有 1 行（鹿点）
SELECT `id`,`name`,`status` FROM `tb_shop` WHERE `status` = 2 ORDER BY `id`;
-- 期望: (13, '鹿点(辰泰园区体验店)', 2)
