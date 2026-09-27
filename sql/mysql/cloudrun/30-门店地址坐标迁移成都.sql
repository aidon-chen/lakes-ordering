-- ============================================================
-- 30-门店地址坐标迁移成都.sql
-- 作用：门店(shopId=13) 云端由「鹿点(辰泰园区体验店)/长沙」整体切换为「湖里Lakes/成都」
--       （09-24 用户拍板：鹿点旧名废弃；不 DELETE 行——订单/商品外键挂 shop_id=13，UPDATE 覆盖等效）
-- 范围：仅 tb_shop 一行（id=13）；订单/存酒等历史快照不动（快照口径）
-- 状态：✅ 已于 2026-09-24 经线上 admin API（PUT /rest/admin/shop/update）执行
--       （云库密码不落盘，故走 API 而非 apply-to-cloud-db.sh；本文件为留档与回滚依据）
-- 本地：已于 2026-09-23 在 siam_db 应用（原值备份
--       local-run/backup/2026-09-23-chengdu-migration/tb_shop_13_before.sql）
-- 坐标口径：tb_shop 存百度 bd09（ShopController.listForPickup 将小程序
--           高德 gcj02 定位 gaoDeToBaidu 后再比距离）；成都 IFS
--           gcj02=104.081534,30.657186 → bd09=104.088083,30.663008
-- 前端配套（已在本地 customer-cli 落地并编译）：
--   App.vue / shop-list.vue / replace.vue 兜底坐标与默认定位点、
--   mall/pay.vue 自提卡兜底坐标 → 同步改为成都 gcj02
-- 回滚：见文件末尾注释块（回滚=恢复鹿点长沙原值）
-- ============================================================

UPDATE tb_shop SET
  name        = '湖里Lakes',
  province    = '四川',
  city        = '成都',
  area        = '锦江区',
  street      = '成都市锦江区红星路三段',
  house_number = '1号IFS国际金融中心',
  longitude   = 104.088083,
  latitude    = 30.663008
WHERE id = 13;

-- 验证（预期 1 行：name=湖里Lakes，地址=成都市锦江区红星路三段，坐标 104.088083/30.663008）
SELECT id, name, province, city, area, street, house_number, longitude, latitude
FROM tb_shop WHERE id = 13;

-- ---------------- 回滚（恢复鹿点长沙原值，09-24 线上实测快照） ----------------
-- UPDATE tb_shop SET
--   name='鹿点(辰泰园区体验店)',
--   province='湖南', city='长沙', area='岳麓区',
--   street='长沙市岳麓区辰泰科技园-D座', house_number='一楼',
--   longitude=112.883120, latitude=28.236586
-- WHERE id = 13;
