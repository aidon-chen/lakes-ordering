-- ============================================================
-- 17-酒水商城-商品种子数据.sql
-- ⚠️⚠️ 未执行 —— 用户已确认「商城界面是对的，保持乐器周边」。
--     本脚本仅备查（若日后真要把商城商品换成酒水，可直接跑；回滚见 archive/points-mall-seed-乐器周边-备份-20260919.sql）。
--     当前切勿误执行：它会把商城 12 个商品从乐器周边改成酒水整瓶。
-- 目标: 酒水商城 C-10（tb_points_mall_*）由「乐器周边」改为「酒水（外带自提）」
--      12 个分类 / 12 个商品（13 条分类关联）/ 2 规格项 / 4 规格选项
-- 生成: 2026-09-19 15:38:35
-- 幂等: 全部 UPDATE，可重复执行；商品 id 不变（购物车/订单明细安全）
-- 回滚: sql/mysql/archive/points-mall-seed-乐器周边-备份-20260919.sql
-- 顺带修复: 原来 main_image / sub_images / detail_images 全为 NULL，
--           而商城详情页有 `result.data.subImages.split(',')` → 详情页会直接抛错；本脚本补齐三个图片字段。
-- ============================================================

SET NAMES utf8mb4;

-- ---------- 1. 分类名（tb_points_mall_menu） ----------
UPDATE `tb_points_mall_menu` SET `name`='精酿啤酒', `sort_number`=1, `update_time`=NOW() WHERE `id`=1;
UPDATE `tb_points_mall_menu` SET `name`='威士忌', `sort_number`=2, `update_time`=NOW() WHERE `id`=2;
UPDATE `tb_points_mall_menu` SET `name`='葡萄酒', `sort_number`=3, `update_time`=NOW() WHERE `id`=3;
UPDATE `tb_points_mall_menu` SET `name`='清酒·梅酒', `sort_number`=4, `update_time`=NOW() WHERE `id`=4;
UPDATE `tb_points_mall_menu` SET `name`='起泡酒·香槟', `sort_number`=5, `update_time`=NOW() WHERE `id`=5;
UPDATE `tb_points_mall_menu` SET `name`='预调酒·果酒', `sort_number`=6, `update_time`=NOW() WHERE `id`=6;
UPDATE `tb_points_mall_menu` SET `name`='无酒精饮品', `sort_number`=7, `update_time`=NOW() WHERE `id`=7;
UPDATE `tb_points_mall_menu` SET `name`='苏打·汤力水', `sort_number`=8, `update_time`=NOW() WHERE `id`=8;
UPDATE `tb_points_mall_menu` SET `name`='优惠专区', `sort_number`=9, `update_time`=NOW() WHERE `id`=9;
UPDATE `tb_points_mall_menu` SET `name`='会员专享', `sort_number`=10, `update_time`=NOW() WHERE `id`=10;
UPDATE `tb_points_mall_menu` SET `name`='新品上架', `sort_number`=11, `update_time`=NOW() WHERE `id`=11;
UPDATE `tb_points_mall_menu` SET `name`='酒具·伴手礼', `sort_number`=12, `update_time`=NOW() WHERE `id`=12;

-- ---------- 2. 商品（tb_points_mall_goods） ----------
-- 字段: 名称/价格/状态/热门/新品/促销/库存/月销/累计销/评价/包装费/详情/主图/子图/详情图/排序
UPDATE `tb_points_mall_goods` SET `name`='单一麦芽威士忌 700ml', `price`=399.00, `status`=2, `is_hot`=1, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=60, `monthly_sales`=86, `total_sales`=344, `total_comments`=17, `packing_charges`=0.00, `product_time`=0.00, `detail`='48% vol，雪莉桶陈酿，闻香有干果与蜂蜜，余韵悠长。外带整瓶，到店自提。', `main_image`='data/images/admin/1/deerspot_1591847965973.jpg', `sub_images`='data/images/admin/1/deerspot_1591847965973.jpg', `detail_images`='data/images/admin/1/deerspot_1591847965973.jpg', `sort_number`=1, `update_time`=NOW() WHERE `id`=101;
UPDATE `tb_points_mall_goods` SET `name`='日式梅酒 720ml', `price`=98.00, `status`=2, `is_hot`=1, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=120, `monthly_sales`=142, `total_sales`=568, `total_comments`=28, `packing_charges`=0.00, `product_time`=0.00, `detail`='本格梅酒，青梅原果浸渍，酸甜平衡，冰镇或加苏打皆宜。', `main_image`='data/images/admin/1/deerspot_1592374898933.jpg', `sub_images`='data/images/admin/1/deerspot_1592374898933.jpg', `detail_images`='data/images/admin/1/deerspot_1592374898933.jpg', `sort_number`=2, `update_time`=NOW() WHERE `id`=102;
UPDATE `tb_points_mall_goods` SET `name`='赤霞珠干红 750ml', `price`=168.00, `status`=2, `is_hot`=1, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=80, `monthly_sales`=108, `total_sales`=432, `total_comments`=21, `packing_charges`=0.00, `product_time`=0.00, `detail`='13.5% vol，黑加仑与雪松香气，单宁细腻，适合配红肉。', `main_image`='data/images/admin/1/deerspot_1592803789993.jpg', `sub_images`='data/images/admin/1/deerspot_1592803789993.jpg', `detail_images`='data/images/admin/1/deerspot_1592803789993.jpg', `sort_number`=3, `update_time`=NOW() WHERE `id`=103;
UPDATE `tb_points_mall_goods` SET `name`='普罗塞克起泡酒 750ml', `price`=138.00, `status`=2, `is_hot`=1, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=90, `monthly_sales`=96, `total_sales`=384, `total_comments`=19, `packing_charges`=0.00, `product_time`=0.00, `detail`='11% vol，青苹果与白花香，气泡细密，开胃首选。', `main_image`='data/images/admin/1/deerspot_1592202810151.jpg', `sub_images`='data/images/admin/1/deerspot_1592202810151.jpg', `detail_images`='data/images/admin/1/deerspot_1592202810151.jpg', `sort_number`=4, `update_time`=NOW() WHERE `id`=104;
UPDATE `tb_points_mall_goods` SET `name`='低醇精酿啤酒 6 瓶装', `price`=118.00, `status`=2, `is_hot`=0, `is_new`=1, `is_sale`=0, `sale_price`=0.00, `stock`=100, `monthly_sales`=64, `total_sales`=256, `total_comments`=12, `packing_charges`=0.00, `product_time`=0.00, `detail`='2.5% vol 低醇配方，麦芽香气足，冰镇后更清爽。', `main_image`='data/images/admin/1/deerspot_1589253795582.jpg', `sub_images`='data/images/admin/1/deerspot_1589253795582.jpg', `detail_images`='data/images/admin/1/deerspot_1589253795582.jpg', `sort_number`=5, `update_time`=NOW() WHERE `id`=105;
UPDATE `tb_points_mall_goods` SET `name`='精酿啤酒 12 瓶混合装', `price`=268.00, `status`=4, `is_hot`=0, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=0, `monthly_sales`=38, `total_sales`=152, `total_comments`=7, `packing_charges`=0.00, `product_time`=0.00, `detail`='IPA／白啤／黑啤各 4 瓶，一次喝遍三款（当前售罄，补货中）。', `main_image`='data/images/admin/1/deerspot_1591622581910.jpg', `sub_images`='data/images/admin/1/deerspot_1591622581910.jpg', `detail_images`='data/images/admin/1/deerspot_1591622581910.jpg', `sort_number`=6, `update_time`=NOW() WHERE `id`=106;
UPDATE `tb_points_mall_goods` SET `name`='单一麦芽威士忌 200ml 迷你瓶', `price`=199.00, `status`=2, `is_hot`=0, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=40, `monthly_sales`=52, `total_sales`=208, `total_comments`=10, `packing_charges`=0.00, `product_time`=0.00, `detail`='会员专享小瓶装，出差或独饮刚好，配冰球模具更佳。', `main_image`='data/images/admin/1/deerspot_1591616292814.jpg', `sub_images`='data/images/admin/1/deerspot_1591616292814.jpg', `detail_images`='data/images/admin/1/deerspot_1591616292814.jpg', `sort_number`=7, `update_time`=NOW() WHERE `id`=107;
UPDATE `tb_points_mall_goods` SET `name`='汤力水 12 瓶装', `price`=58.00, `status`=2, `is_hot`=0, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=200, `monthly_sales`=186, `total_sales`=744, `total_comments`=37, `packing_charges`=0.00, `product_time`=0.00, `detail`='调酒基酒伴侣，奎宁风味，配金酒即是一杯金汤力。', `main_image`='data/images/admin/1/deerspot_1591264346926.jpg', `sub_images`='data/images/admin/1/deerspot_1591264346926.jpg', `detail_images`='data/images/admin/1/deerspot_1591264346926.jpg', `sort_number`=8, `update_time`=NOW() WHERE `id`=108;
UPDATE `tb_points_mall_goods` SET `name`='无酒精气泡饮 6 罐装', `price`=45.00, `status`=2, `is_hot`=0, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=200, `monthly_sales`=158, `total_sales`=632, `total_comments`=31, `packing_charges`=0.00, `product_time`=0.00, `detail`='0% vol，柚子与柠檬草风味，气泡感足，开车也能喝。', `main_image`='data/images/admin/1/deerspot_1591613166071.jpg', `sub_images`='data/images/admin/1/deerspot_1591613166071.jpg', `detail_images`='data/images/admin/1/deerspot_1591613166071.jpg', `sort_number`=9, `update_time`=NOW() WHERE `id`=109;
UPDATE `tb_points_mall_goods` SET `name`='果味预调酒 6 罐装', `price`=79.00, `status`=2, `is_hot`=0, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=150, `monthly_sales`=124, `total_sales`=496, `total_comments`=24, `packing_charges`=0.00, `product_time`=0.00, `detail`='5% vol，混合莓果风味，开罐即饮，适合聚会。', `main_image`='data/images/admin/1/deerspot_1589525885573.jpg', `sub_images`='data/images/admin/1/deerspot_1589525885573.jpg', `detail_images`='data/images/admin/1/deerspot_1589525885573.jpg', `sort_number`=10, `update_time`=NOW() WHERE `id`=110;
UPDATE `tb_points_mall_goods` SET `name`='手工水晶威士忌杯（对杯礼盒）', `price`=129.00, `status`=2, `is_hot`=0, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=60, `monthly_sales`=46, `total_sales`=184, `total_comments`=9, `packing_charges`=0.00, `product_time`=0.00, `detail`='手工吹制水晶杯，厚底直壁，礼盒装两只。', `main_image`='data/images/admin/1/deerspot_1591264167859.jpg', `sub_images`='data/images/admin/1/deerspot_1591264167859.jpg', `detail_images`='data/images/admin/1/deerspot_1591264167859.jpg', `sort_number`=11, `update_time`=NOW() WHERE `id`=111;
UPDATE `tb_points_mall_goods` SET `name`='精酿啤酒 4 瓶尝鲜装（促销）', `price`=39.90, `status`=2, `is_hot`=0, `is_new`=0, `is_sale`=1, `sale_price`=29.90, `stock`=300, `monthly_sales`=210, `total_sales`=840, `total_comments`=42, `packing_charges`=0.00, `product_time`=0.00, `detail`='4 瓶尝鲜组合，含 IPA 与白啤各 2 瓶，优惠专区限量。', `main_image`='data/images/admin/1/deerspot_1591719311481.jpg', `sub_images`='data/images/admin/1/deerspot_1591719311481.jpg', `detail_images`='data/images/admin/1/deerspot_1591719311481.jpg', `sort_number`=12, `update_time`=NOW() WHERE `id`=112;

-- ---------- 3. 分类关联（tb_points_mall_menu_goods_relation） ----------
-- 沿用原关联结构：menu1 下 2 件（106 售罄 + 112 促销）、menu9 与 menu1 共用 112，其余 1:1
UPDATE `tb_points_mall_menu_goods_relation` SET `menu_id`=1, `update_time`=NOW() WHERE `goods_id`=106 AND `menu_id` IN (1,2,3,4,5,6,7,8,9,10,11,12);
UPDATE `tb_points_mall_menu_goods_relation` SET `menu_id`=1, `update_time`=NOW() WHERE `goods_id`=112 AND `menu_id` IN (1,2,3,4,5,6,7,8,9,10,11,12);
UPDATE `tb_points_mall_menu_goods_relation` SET `menu_id`=2, `update_time`=NOW() WHERE `goods_id`=101 AND `menu_id` IN (1,2,3,4,5,6,7,8,9,10,11,12);
UPDATE `tb_points_mall_menu_goods_relation` SET `menu_id`=3, `update_time`=NOW() WHERE `goods_id`=103 AND `menu_id` IN (1,2,3,4,5,6,7,8,9,10,11,12);
UPDATE `tb_points_mall_menu_goods_relation` SET `menu_id`=4, `update_time`=NOW() WHERE `goods_id`=102 AND `menu_id` IN (1,2,3,4,5,6,7,8,9,10,11,12);
UPDATE `tb_points_mall_menu_goods_relation` SET `menu_id`=5, `update_time`=NOW() WHERE `goods_id`=104 AND `menu_id` IN (1,2,3,4,5,6,7,8,9,10,11,12);
UPDATE `tb_points_mall_menu_goods_relation` SET `menu_id`=6, `update_time`=NOW() WHERE `goods_id`=110 AND `menu_id` IN (1,2,3,4,5,6,7,8,9,10,11,12);
UPDATE `tb_points_mall_menu_goods_relation` SET `menu_id`=7, `update_time`=NOW() WHERE `goods_id`=109 AND `menu_id` IN (1,2,3,4,5,6,7,8,9,10,11,12);
UPDATE `tb_points_mall_menu_goods_relation` SET `menu_id`=8, `update_time`=NOW() WHERE `goods_id`=108 AND `menu_id` IN (1,2,3,4,5,6,7,8,9,10,11,12);
UPDATE `tb_points_mall_menu_goods_relation` SET `menu_id`=9, `update_time`=NOW() WHERE `goods_id`=112 AND `menu_id` IN (1,2,3,4,5,6,7,8,9,10,11,12);
UPDATE `tb_points_mall_menu_goods_relation` SET `menu_id`=10, `update_time`=NOW() WHERE `goods_id`=107 AND `menu_id` IN (1,2,3,4,5,6,7,8,9,10,11,12);
UPDATE `tb_points_mall_menu_goods_relation` SET `menu_id`=11, `update_time`=NOW() WHERE `goods_id`=105 AND `menu_id` IN (1,2,3,4,5,6,7,8,9,10,11,12);
UPDATE `tb_points_mall_menu_goods_relation` SET `menu_id`=12, `update_time`=NOW() WHERE `goods_id`=111 AND `menu_id` IN (1,2,3,4,5,6,7,8,9,10,11,12);

-- ---------- 4. 规格项（tb_points_mall_goods_specification，全部属于商品 101） ----------
UPDATE `tb_points_mall_goods_specification` SET `name`='规格', `sort_number`=1, `update_time`=NOW() WHERE `id`=1 AND `goods_id`=101;
UPDATE `tb_points_mall_goods_specification` SET `name`='赠品', `sort_number`=2, `update_time`=NOW() WHERE `id`=2 AND `goods_id`=101;

-- ---------- 5. 规格选项（tb_points_mall_goods_specification_option） ----------
UPDATE `tb_points_mall_goods_specification_option` SET `name`='双瓶礼盒', `price`=300.00, `update_time`=NOW() WHERE `id`=1;  -- 原：23 寸
UPDATE `tb_points_mall_goods_specification_option` SET `name`='单瓶 700ml', `price`=0.00, `update_time`=NOW() WHERE `id`=2;  -- 原：26 寸
UPDATE `tb_points_mall_goods_specification_option` SET `name`='无赠品', `price`=0.00, `update_time`=NOW() WHERE `id`=3;  -- 原：裸琴
UPDATE `tb_points_mall_goods_specification_option` SET `name`='含冰球模具', `price`=0.00, `update_time`=NOW() WHERE `id`=4;  -- 原：含琴包

-- 满减规则 tb_points_mall_full_reduction_rule 保持不动（满99减10）

-- ---------- 6. 自检 ----------
SELECT m.`name` AS 分类, g.`name` AS 商品, g.`price` AS 价格, g.`status` AS 状态
FROM `tb_points_mall_menu` m LEFT JOIN `tb_points_mall_menu_goods_relation` r ON r.`menu_id`=m.`id`
LEFT JOIN `tb_points_mall_goods` g ON g.`id`=r.`goods_id` ORDER BY m.`sort_number`;
-- 期望: 13 行（12 分类，其中「精酿啤酒」2 件），价格 39.90–399，状态 4 仅 1 条
