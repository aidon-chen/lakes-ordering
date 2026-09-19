-- ============================================================================
-- 批次 5 联调种子数据（公告 C-14 / 存酒 C-16 / 可开票订单 C-15）
-- 依据：《批次5-DDL落库执行计划-本地与云库-v1.1.md》§4 灌数
-- 特性：★全幂等 —— 每块先按唯一键 DELETE 再按显式 id INSERT，可重复执行
-- 覆盖：公告 4 条（含 1 条 status=0 下线，验证列表过滤）
--       存酒 3 条（覆盖 存续中 / 将过期 / 已过期 三态）
--       已完成订单 2 张（status=6 且 is_invoice=0，供 C-15 发票页选择；金额对齐画板 ¥94.00）
-- 生成时间：2026-09-19 10:51（生日基准日 2026-09-19）
-- 适用：本地 siam_db 与云托管 siam_db（两库会员/门店数据一致：member 2=siam, 17=15100000000；shop 13）
-- ============================================================================


-- ---------------------------------------------------------------------------
-- 一、公告 tb_notice（C-14）
-- ---------------------------------------------------------------------------
DELETE FROM `tb_notice` WHERE `id` IN (1,2,3,4);
INSERT INTO `tb_notice` (`id`,`shop_id`,`title`,`content`,`is_top`,`status`,`publish_time`,`create_time`,`update_time`) VALUES
 (1, NULL, '中秋营业时间调整通知',
  '<p>中秋假期（9 月 25 日至 9 月 27 日）门店营业时间调整为 <strong>17:00 - 02:00</strong>。</p><p>假期期间正常提供堂食与外带自提服务，建议提前到店或电话预约卡座。</p><p>地址：××区 ××路 ××号。本通知最终解释权归门店所有。</p>',
  1, 1, '2026-09-17 10:00:00', NOW(), NOW()),
 (2, 13, '新酒到店：麦卡伦 12 年',
  '<p>麦卡伦 12 年雪莉桶单一麦芽威士忌已到店，限量 24 瓶。</p><p>到店可整瓶购买或单杯品鉴，存酒服务同步开放。</p>',
  0, 1, '2026-09-16 15:30:00', NOW(), NOW()),
 (3, NULL, '会员存酒服务升级公告',
  '<p>自 10 月 1 日起，存酒有效期统一调整为 <strong>90 天</strong>，临期将通过短信提醒。</p><p>存酒可到店出示存酒码取酒，支持分次取用与转赠。</p>',
  0, 1, '2026-09-15 09:20:00', NOW(), NOW()),
 (4, NULL, '（历史）五一活动已结束',
  '<p>五一主题活动已于 5 月 6 日结束，感谢参与。</p>',
  0, 0, '2026-05-06 20:00:00', NOW(), NOW());   -- 下线项：列表不应出现

-- ---------------------------------------------------------------------------
-- 二、存酒 tb_wine_storage（C-16；三态：存续中 / 将过期 / 已过期）
--   判定：status=2 已取完 > (status=3 或 expire_date<今天) 已过期 > 剩余≤30天 将过期 > 存续中
-- ---------------------------------------------------------------------------
DELETE FROM `tb_wine_storage` WHERE `storage_code` IN ('0001','0002','0003');
INSERT INTO `tb_wine_storage` (`id`,`member_id`,`shop_id`,`goods_name`,`bottle_count`,`remain_count`,`storage_code`,`location`,`storage_date`,`expire_date`,`status`,`remark`,`create_time`,`update_time`) VALUES
 (1, 17, 13, '麦卡伦 12 年', 2, 2, '0001', 'B 区 03 号柜', '2026-09-19', '2026-12-18', 1, '整瓶存入', NOW(), NOW()),
 (2, 17, 13, '山崎 12 年',   1, 1, '0002', 'B 区 05 号柜', '2026-07-11', '2026-10-09', 1, '临期提醒', NOW(), NOW()),
 (3, 17, 13, '響 21 年',     3, 1, '0003', 'C 区 01 号柜', '2026-06-11', '2026-09-09', 1, '已过期（已取 2 瓶）', NOW(), NOW());

-- ---------------------------------------------------------------------------
-- 三、可开票订单 tb_order + 明细（C-15 发票页需要 status=6 且 is_invoice=0）
--   订单 1：member 17（15100000000）¥94.00，对齐画板 C-15「SH20260917001 / 开票金额 ¥94.00」
--   订单 2：member 2（siam）¥186.00，用于第二账号验证
-- ---------------------------------------------------------------------------
DELETE FROM `tb_order_detail` WHERE `order_id` IN (9001,9002);
DELETE FROM `tb_order` WHERE `id` IN (9001,9002);
INSERT INTO `tb_order` (`id`,`member_id`,`order_no`,`goods_total_quantity`,`goods_total_price`,`actual_price`,`shopping_way`,`shop_id`,`shop_name`,`table_id`,`table_no`,`table_name`,`dining_num`,`checkout_mode`,`is_payment`,`order_scene`,`status`,`is_invoice`,`description`,`create_time`,`payment_success_time`,`order_completion_time`,`update_time`) VALUES
 (9001, 17, 'SH20260917001', 9, 94.00, 94.00, 1, 13, '鹿点(辰泰园区体验店)', 1, 'A01', 'A区·A01', 2, 1, 1, 1, 6, 0, '批次5联调种子单（可开票）', '2026-09-17 20:15:00', '2026-09-17 20:16:00', '2026-09-17 22:30:00', NOW()),
 (9002,  2, 'SH20260917002', 20, 186.00, 186.00, 1, 13, '鹿点(辰泰园区体验店)', 2, 'A02', 'A区·A02', 4, 1, 1, 1, 6, 0, '批次5联调种子单（可开票）', '2026-09-16 21:05:00', '2026-09-16 21:06:00', '2026-09-16 23:40:00', NOW());

INSERT INTO `tb_order_detail` (`id`,`order_id`,`goods_id`,`goods_name`,`main_image`,`price`,`number`,`subtotal`) VALUES
 (9001, 9001, 28, '布丁仙草', 'data/images/admin/1/deerspot_1591847965973.jpg', 10.00, 4, 40.00),
 (9002, 9001, 33, '姜汁撞奶', 'data/images/admin/1/deerspot_1589253795582.jpg', 12.00, 2, 24.00),
 (9003, 9001, 41, '卡布基诺', 'data/images/admin/1/deerspot_1590563935696.jpg', 10.00, 3, 30.00),
 (9004, 9002, 45, '抹茶拿铁', 'data/images/admin/1/deerspot_1591705887087.jpg', 8.00, 7, 56.00),
 (9005, 9002, 41, '卡布基诺', 'data/images/admin/1/deerspot_1590563935696.jpg', 10.00, 13, 130.00);

-- ---------------------------------------------------------------------------
-- 四、执行后自检（应为：公告 3 条发布 / 存酒 3 条 / 可开票订单 2 张）
-- ---------------------------------------------------------------------------
SELECT '公告-已发布' AS item, (SELECT COUNT(*) FROM tb_notice WHERE status=1) AS n, 3 AS expect
UNION ALL SELECT '公告-含下线',   (SELECT COUNT(*) FROM tb_notice), 4
UNION ALL SELECT '存酒-在存',     (SELECT COUNT(*) FROM tb_wine_storage WHERE status=1), 3
UNION ALL SELECT '可开票订单',    (SELECT COUNT(*) FROM tb_order WHERE status=6 AND is_invoice=0), 2;
