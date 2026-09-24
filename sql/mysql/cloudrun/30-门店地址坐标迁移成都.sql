-- ============================================================
-- 30-门店地址坐标迁移成都.sql
-- 作用：湖里Lakes(shopId=13) 演示门店地址/坐标由长沙迁至成都
-- 范围：仅 tb_shop 一行（id=13）；订单/存酒等历史快照不动（快照口径）
-- 状态：⏳ 待用户确认后才在云端执行（铁律 0：上云须确认）
-- 本地：已于 2026-09-23 在 siam_db 应用（原值备份
--       local-run/backup/2026-09-23-chengdu-migration/tb_shop_13_before.sql）
-- 坐标口径：tb_shop 存百度 bd09（ShopController.listForPickup 将小程序
--           高德 gcj02 定位 gaoDeToBaidu 后再比距离）；成都 IFS
--           gcj02=104.081534,30.657186 → bd09=104.088083,30.663008
-- 前端配套（已在本地 customer-cli 落地并编译）：
--   App.vue / shop-list.vue / replace.vue 兜底坐标与默认定位点、
--   mall/pay.vue 自提卡兜底坐标 → 同步改为成都 gcj02
-- 回滚：见文件末尾注释块
-- ============================================================

UPDATE tb_shop SET
  province    = '四川',
  city        = '成都',
  area        = '锦江区',
  street      = '成都市锦江区红星路三段',
  house_number = '1号IFS国际金融中心',
  longitude   = 104.088083,
  latitude    = 30.663008
WHERE id = 13;

-- 验证（预期 1 行，地址=成都市锦江区红星路三段，坐标 104.088083/30.663008）
SELECT id, name, province, city, area, street, house_number, longitude, latitude
FROM tb_shop WHERE id = 13;

-- ---------------- 回滚（恢复长沙原值） ----------------
-- UPDATE tb_shop SET
--   province='湖南', city='长沙', area='岳麓区',
--   street='长沙市岳麓区辰泰科技园-D座', house_number='一楼',
--   longitude=112.883120, latitude=28.236586
-- WHERE id = 13;
