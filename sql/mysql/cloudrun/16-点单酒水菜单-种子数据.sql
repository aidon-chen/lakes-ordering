-- ============================================================
-- 16-点单酒水菜单-种子数据.sql
-- 目标: shopId=13「鹿点(辰泰园区体验店)」点单菜单，由鹿点奶茶 demo 残留 → 音乐酒水主题菜单
--      9 个分类 / 40 个商品 / 40 条分类关联 / 111 条规格 / 165 条规格选项
-- 生成: 2026-09-19 14:49:28
-- 幂等: 全部为 UPDATE，按主键/唯一条件定位；可重复执行
-- 回滚: sql/mysql/archive/shop13-点单菜单-奶茶demo-备份-20260919.sql
-- 说明: 商品 id 保持不变（历史订单/购物车/收藏/券关联不受影响）
--       图片沿用各商品原有 OSS 图（依图片观感反推酒名，未新增图床依赖）
-- ============================================================

SET NAMES utf8mb4;

-- ---------- 1. 分类名（tb_menu） ----------
UPDATE `tb_menu` SET `name`='本店招牌', `sort_number`=1, `update_time`=NOW() WHERE `id`=24 AND `shop_id`=13;
UPDATE `tb_menu` SET `name`='精酿啤酒', `sort_number`=2, `update_time`=NOW() WHERE `id`=21 AND `shop_id`=13;
UPDATE `tb_menu` SET `name`='经典鸡尾酒', `sort_number`=3, `update_time`=NOW() WHERE `id`=22 AND `shop_id`=13;
UPDATE `tb_menu` SET `name`='威士忌·洋酒', `sort_number`=4, `update_time`=NOW() WHERE `id`=18 AND `shop_id`=13;
UPDATE `tb_menu` SET `name`='葡萄酒', `sort_number`=5, `update_time`=NOW() WHERE `id`=23 AND `shop_id`=13;
UPDATE `tb_menu` SET `name`='气泡特调', `sort_number`=6, `update_time`=NOW() WHERE `id`=19 AND `shop_id`=13;
UPDATE `tb_menu` SET `name`='无酒精特调', `sort_number`=7, `update_time`=NOW() WHERE `id`=20 AND `shop_id`=13;
UPDATE `tb_menu` SET `name`='微醺奶茶·甜品酒', `sort_number`=8, `update_time`=NOW() WHERE `id`=17 AND `shop_id`=13;
UPDATE `tb_menu` SET `name`='本周新酒', `sort_number`=9, `update_time`=NOW() WHERE `id`=26 AND `shop_id`=13;

-- ---------- 2. 商品（tb_goods） ----------
-- 字段: name/price/status/热门/新品/库存/月销/累计销/评价/出品时长/详情/排序
UPDATE `tb_goods` SET `name`='招牌薄荷莫吉托', `price`=58.00, `status`=2, `is_hot`=1, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=60, `monthly_sales`=186, `total_sales`=1117, `total_comments`=46, `packing_charges`=0.00, `product_time`=6.00, `detail`='白朗姆为底，青柠、新鲜薄荷与苏打水，清爽薄荷尾韵。', `update_time`=NOW() WHERE `id`=53 AND `shop_id`=13;
UPDATE `tb_goods` SET `name`='蓝色泻湖 Blue Lagoon', `price`=68.00, `status`=2, `is_hot`=1, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=50, `monthly_sales`=142, `total_sales`=854, `total_comments`=35, `packing_charges`=0.00, `product_time`=6.00, `detail`='蓝橙利口酒配柠檬苏打，冰蓝渐变，酒精度偏低。', `update_time`=NOW() WHERE `id`=52 AND `shop_id`=13;
UPDATE `tb_goods` SET `name`='爱尔兰咖啡（威士忌）', `price`=62.00, `status`=2, `is_hot`=1, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=40, `monthly_sales`=118, `total_sales`=711, `total_comments`=29, `packing_charges`=0.00, `product_time`=8.00, `detail`='热咖啡注入爱尔兰威士忌，覆一层厚奶油，暖身首选。', `update_time`=NOW() WHERE `id`=41 AND `shop_id`=13;
UPDATE `tb_goods` SET `name`='草莓庆典特调', `price`=66.00, `status`=2, `is_hot`=1, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=45, `monthly_sales`=96, `total_sales`=580, `total_comments`=24, `packing_charges`=0.00, `product_time`=7.00, `detail`='新鲜草莓现摇，气泡收尾，微醺果香。', `update_time`=NOW() WHERE `id`=67 AND `shop_id`=13;
UPDATE `tb_goods` SET `name`='比利时小麦白啤', `price`=42.00, `status`=2, `is_hot`=0, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=80, `monthly_sales`=210, `total_sales`=1265, `total_comments`=52, `packing_charges`=0.00, `product_time`=3.00, `detail`='云雾状酒体，橙皮与芫荽籽香气，330ml 冰镇出品。', `update_time`=NOW() WHERE `id`=33 AND `shop_id`=13;
UPDATE `tb_goods` SET `name`='百香果西达（果啤）', `price`=46.00, `status`=2, `is_hot`=0, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=70, `monthly_sales`=168, `total_sales`=1014, `total_comments`=42, `packing_charges`=0.00, `product_time`=3.00, `detail`='百香果发酵果啤，酸甜脆爽，酒精度 4.5%。', `update_time`=NOW() WHERE `id`=71 AND `shop_id`=13;
UPDATE `tb_goods` SET `name`='芒果小麦果啤', `price`=48.00, `status`=2, `is_hot`=0, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=70, `monthly_sales`=132, `total_sales`=799, `total_comments`=33, `packing_charges`=0.00, `product_time`=3.00, `detail`='芒果风味小麦啤酒，热带气息，配炸物最佳。', `update_time`=NOW() WHERE `id`=76 AND `shop_id`=13;
UPDATE `tb_goods` SET `name`='季节限定·南瓜艾尔', `price`=46.00, `status`=4, `is_hot`=0, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=0, `monthly_sales`=88, `total_sales`=536, `total_comments`=22, `packing_charges`=0.00, `product_time`=3.00, `detail`='南瓜与肉桂风味琥珀艾尔，秋冬限定。', `update_time`=NOW() WHERE `id`=56 AND `shop_id`=13;
UPDATE `tb_goods` SET `name`='金汤力 Gin & Tonic', `price`=62.00, `status`=2, `is_hot`=0, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=60, `monthly_sales`=174, `total_sales`=1053, `total_comments`=43, `packing_charges`=0.00, `product_time`=5.00, `detail`='金酒配汤力水，青柠角提香，气泡冽爽。', `update_time`=NOW() WHERE `id`=46 AND `shop_id`=13;
UPDATE `tb_goods` SET `name`='青柠罗勒菲士', `price`=58.00, `status`=2, `is_hot`=0, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=55, `monthly_sales`=146, `total_sales`=886, `total_comments`=36, `packing_charges`=0.00, `product_time`=6.00, `detail`='金酒、青柠与新鲜罗勒摇匀，草本清香。', `update_time`=NOW() WHERE `id`=72 AND `shop_id`=13;
UPDATE `tb_goods` SET `name`='威士忌酸 Whisky Sour', `price`=68.00, `status`=2, `is_hot`=0, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=50, `monthly_sales`=128, `total_sales`=779, `total_comments`=32, `packing_charges`=0.00, `product_time`=6.00, `detail`='波本威士忌、柠檬与少量糖浆，蛋白霜收口。', `update_time`=NOW() WHERE `id`=38 AND `shop_id`=13;
UPDATE `tb_goods` SET `name`='青苹果菲士', `price`=58.00, `status`=2, `is_hot`=0, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=50, `monthly_sales`=112, `total_sales`=684, `total_comments`=28, `packing_charges`=0.00, `product_time`=5.00, `detail`='青苹果利口酒配柠檬苏打，清甜带酸。', `update_time`=NOW() WHERE `id`=48 AND `shop_id`=13;
UPDATE `tb_goods` SET `name`='长岛冰茶 Long Island', `price`=78.00, `status`=2, `is_hot`=0, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=40, `monthly_sales`=104, `total_sales`=637, `total_comments`=26, `packing_charges`=0.00, `product_time`=8.00, `detail`='五种基酒调和，可乐收尾，后劲偏足。', `update_time`=NOW() WHERE `id`=81 AND `shop_id`=13;
UPDATE `tb_goods` SET `name`='彩虹长饮 Rainbow', `price`=58.00, `status`=2, `is_hot`=0, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=45, `monthly_sales`=92, `total_sales`=566, `total_comments`=23, `packing_charges`=0.00, `product_time`=6.00, `detail`='分层果味长饮，颜色随灯光变幻，适合拍照。', `update_time`=NOW() WHERE `id`=65 AND `shop_id`=13;
UPDATE `tb_goods` SET `name`='单一麦芽威士忌（单杯）', `price`=88.00, `status`=2, `is_hot`=0, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=30, `monthly_sales`=76, `total_sales`=471, `total_comments`=19, `packing_charges`=0.00, `product_time`=3.00, `detail`='45ml 单一麦芽，纯饮或加一滴水。', `update_time`=NOW() WHERE `id`=70 AND `shop_id`=13;
UPDATE `tb_goods` SET `name`='抹茶嗨棒 Matcha Highball', `price`=72.00, `status`=2, `is_hot`=0, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=35, `monthly_sales`=68, `total_sales`=424, `total_comments`=17, `packing_charges`=0.00, `product_time`=5.00, `detail`='威士忌与抹茶苏打，苦甜平衡。', `update_time`=NOW() WHERE `id`=45 AND `shop_id`=13;
UPDATE `tb_goods` SET `name`='威士忌冰球套餐（双人）', `price`=128.00, `status`=2, `is_hot`=0, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=20, `monthly_sales`=42, `total_sales`=269, `total_comments`=10, `packing_charges`=0.00, `product_time`=5.00, `detail`='双人份，含冰球威士忌两杯与佐酒坚果。', `update_time`=NOW() WHERE `id`=261 AND `shop_id`=13;
UPDATE `tb_goods` SET `name`='赤霞珠干红（单杯）', `price`=58.00, `status`=2, `is_hot`=0, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=40, `monthly_sales`=96, `total_sales`=594, `total_comments`=24, `packing_charges`=0.00, `product_time`=3.00, `detail`='单杯 150ml，黑果香明显，单宁柔和。', `update_time`=NOW() WHERE `id`=80 AND `shop_id`=13;
UPDATE `tb_goods` SET `name`='莓果桑格利亚', `price`=62.00, `status`=2, `is_hot`=0, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=45, `monthly_sales`=110, `total_sales`=679, `total_comments`=27, `packing_charges`=0.00, `product_time`=6.00, `detail`='红酒基底浸渍混合莓果，冰镇饮用。', `update_time`=NOW() WHERE `id`=60 AND `shop_id`=13;
UPDATE `tb_goods` SET `name`='蜜桃起泡酒', `price`=52.00, `status`=2, `is_hot`=0, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=50, `monthly_sales`=124, `total_sales`=764, `total_comments`=31, `packing_charges`=0.00, `product_time`=3.00, `detail`='微甜起泡，蜜桃香气，酒精度低。', `update_time`=NOW() WHERE `id`=74 AND `shop_id`=13;
UPDATE `tb_goods` SET `name`='西瓜莫斯卡托', `price`=48.00, `status`=2, `is_hot`=0, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=50, `monthly_sales`=118, `total_sales`=729, `total_comments`=29, `packing_charges`=0.00, `product_time`=4.00, `detail`='莫斯卡托配鲜榨西瓜，夏日限定。', `update_time`=NOW() WHERE `id`=63 AND `shop_id`=13;
UPDATE `tb_goods` SET `name`='星空气泡特调', `price`=48.00, `status`=2, `is_hot`=0, `is_new`=1, `is_sale`=0, `sale_price`=0.00, `stock`=60, `monthly_sales`=158, `total_sales`=970, `total_comments`=39, `packing_charges`=0.00, `product_time`=5.00, `detail`='蓝紫渐变气泡，含可食用闪粉，视觉系特调。', `update_time`=NOW() WHERE `id`=75 AND `shop_id`=13;
UPDATE `tb_goods` SET `name`='柚子气泡酒', `price`=42.00, `status`=2, `is_hot`=0, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=65, `monthly_sales`=140, `total_sales`=863, `total_comments`=35, `packing_charges`=0.00, `product_time`=4.00, `detail`='柚子果肉配气泡酒，酸甜解腻。', `update_time`=NOW() WHERE `id`=47 AND `shop_id`=13;
UPDATE `tb_goods` SET `name`='橙香阿佩罗气泡', `price`=48.00, `status`=2, `is_hot`=0, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=55, `monthly_sales`=126, `total_sales`=780, `total_comments`=31, `packing_charges`=0.00, `product_time`=5.00, `detail`='阿佩罗开胃酒配普罗塞克，苦橙回甘。', `update_time`=NOW() WHERE `id`=68 AND `shop_id`=13;
UPDATE `tb_goods` SET `name`='草莓气泡酒', `price`=48.00, `status`=2, `is_hot`=0, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=60, `monthly_sales`=112, `total_sales`=697, `total_comments`=28, `packing_charges`=0.00, `product_time`=5.00, `detail`='草莓果泥配气泡酒，粉红气泡感。', `update_time`=NOW() WHERE `id`=64 AND `shop_id`=13;
UPDATE `tb_goods` SET `name`='芒果冰沙（无酒精）', `price`=38.00, `status`=2, `is_hot`=0, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=70, `monthly_sales`=132, `total_sales`=818, `total_comments`=33, `packing_charges`=0.00, `product_time`=4.00, `detail`='鲜芒果打制冰沙，无酒精。', `update_time`=NOW() WHERE `id`=62 AND `shop_id`=13;
UPDATE `tb_goods` SET `name`='芒果酸乳气泡（无酒精）', `price`=36.00, `status`=2, `is_hot`=0, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=70, `monthly_sales`=120, `total_sales`=747, `total_comments`=30, `packing_charges`=0.00, `product_time`=4.00, `detail`='芒果配乳酸菌气泡，无酒精。', `update_time`=NOW() WHERE `id`=66 AND `shop_id`=13;
UPDATE `tb_goods` SET `name`='柠檬酸乳气泡（无酒精）', `price`=32.00, `status`=2, `is_hot`=0, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=75, `monthly_sales`=116, `total_sales`=724, `total_comments`=29, `packing_charges`=0.00, `product_time`=4.00, `detail`='柠檬与乳酸菌，微酸清爽，无酒精。', `update_time`=NOW() WHERE `id`=77 AND `shop_id`=13;
UPDATE `tb_goods` SET `name`='椰香芒果奶昔（无酒精）', `price`=42.00, `status`=2, `is_hot`=0, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=60, `monthly_sales`=108, `total_sales`=677, `total_comments`=27, `packing_charges`=0.00, `product_time`=5.00, `detail`='椰浆与芒果现打，无酒精。', `update_time`=NOW() WHERE `id`=78 AND `shop_id`=13;
UPDATE `tb_goods` SET `name`='微醺黄金珍奶（含酒精）', `price`=48.00, `status`=2, `is_hot`=0, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=60, `monthly_sales`=152, `total_sales`=942, `total_comments`=38, `packing_charges`=0.00, `product_time`=6.00, `detail`='奶香基底加少量朗姆，含珍珠，微醺口感。', `update_time`=NOW() WHERE `id`=51 AND `shop_id`=13;
UPDATE `tb_goods` SET `name`='椰香朗姆微醺奶茶', `price`=46.00, `status`=2, `is_hot`=0, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=60, `monthly_sales`=138, `total_sales`=859, `total_comments`=34, `packing_charges`=0.00, `product_time`=6.00, `detail`='椰奶与白朗姆调和，含椰果。', `update_time`=NOW() WHERE `id`=39 AND `shop_id`=13;
UPDATE `tb_goods` SET `name`='红豆威士忌奶酒', `price`=52.00, `status`=2, `is_hot`=0, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=55, `monthly_sales`=124, `total_sales`=776, `total_comments`=31, `packing_charges`=0.00, `product_time`=6.00, `detail`='奶酒加威士忌，顶部红豆，甜厚。', `update_time`=NOW() WHERE `id`=49 AND `shop_id`=13;
UPDATE `tb_goods` SET `name`='仙草奶酒', `price`=46.00, `status`=2, `is_hot`=0, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=55, `monthly_sales`=118, `total_sales`=741, `total_comments`=29, `packing_charges`=0.00, `product_time`=6.00, `detail`='奶酒配仙草冻，台式微醺风味。', `update_time`=NOW() WHERE `id`=57 AND `shop_id`=13;
UPDATE `tb_goods` SET `name`='布丁奶酒特调', `price`=48.00, `status`=2, `is_hot`=0, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=55, `monthly_sales`=106, `total_sales`=670, `total_comments`=26, `packing_charges`=0.00, `product_time`=6.00, `detail`='奶酒加布丁，走甜品酒路线。', `update_time`=NOW() WHERE `id`=28 AND `shop_id`=13;
UPDATE `tb_goods` SET `name`='黑糖芋圆奶酒', `price`=50.00, `status`=2, `is_hot`=0, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=50, `monthly_sales`=98, `total_sales`=623, `total_comments`=24, `packing_charges`=0.00, `product_time`=6.00, `detail`='黑糖奶酒配芋圆，浓郁绵密。', `update_time`=NOW() WHERE `id`=58 AND `shop_id`=13;
UPDATE `tb_goods` SET `name`='草莓奥利奥奶酒', `price`=52.00, `status`=2, `is_hot`=0, `is_new`=0, `is_sale`=0, `sale_price`=0.00, `stock`=50, `monthly_sales`=92, `total_sales`=588, `total_comments`=23, `packing_charges`=0.00, `product_time`=6.00, `detail`='草莓奶酒撒奥利奥碎，甜品酒收官。', `update_time`=NOW() WHERE `id`=82 AND `shop_id`=13;
UPDATE `tb_goods` SET `name`='青瓜罗勒特调（新）', `price`=62.00, `status`=2, `is_hot`=0, `is_new`=1, `is_sale`=0, `sale_price`=0.00, `stock`=40, `monthly_sales`=46, `total_sales`=313, `total_comments`=11, `packing_charges`=0.00, `product_time`=6.00, `detail`='青瓜、罗勒与金酒，清新草本。', `update_time`=NOW() WHERE `id`=61 AND `shop_id`=13;
UPDATE `tb_goods` SET `name`='蜜桃奶油利口酒（新）', `price`=58.00, `status`=2, `is_hot`=0, `is_new`=1, `is_sale`=0, `sale_price`=0.00, `stock`=45, `monthly_sales`=52, `total_sales`=350, `total_comments`=13, `packing_charges`=0.00, `product_time`=5.00, `detail`='蜜桃利口酒加奶油，甜润。', `update_time`=NOW() WHERE `id`=73 AND `shop_id`=13;
UPDATE `tb_goods` SET `name`='芒果奶酒（新）', `price`=62.00, `status`=2, `is_hot`=0, `is_new`=1, `is_sale`=0, `sale_price`=0.00, `stock`=45, `monthly_sales`=44, `total_sales`=303, `total_comments`=11, `packing_charges`=0.00, `product_time`=6.00, `detail`='芒果果泥与奶酒，热带甜香。', `update_time`=NOW() WHERE `id`=69 AND `shop_id`=13;
UPDATE `tb_goods` SET `name`='百利甜椰香特调（新）', `price`=58.00, `status`=2, `is_hot`=0, `is_new`=1, `is_sale`=0, `sale_price`=0.00, `stock`=45, `monthly_sales`=40, `total_sales`=280, `total_comments`=10, `packing_charges`=0.00, `product_time`=6.00, `detail`='百利甜配椰浆，冰摇出品。', `update_time`=NOW() WHERE `id`=59 AND `shop_id`=13;

-- ---------- 3. 分类关联（tb_menu_goods_relation） ----------
UPDATE `tb_menu_goods_relation` SET `menu_id`=24, `update_time`=NOW() WHERE `goods_id`=53;
UPDATE `tb_menu_goods_relation` SET `menu_id`=24, `update_time`=NOW() WHERE `goods_id`=52;
UPDATE `tb_menu_goods_relation` SET `menu_id`=24, `update_time`=NOW() WHERE `goods_id`=41;
UPDATE `tb_menu_goods_relation` SET `menu_id`=24, `update_time`=NOW() WHERE `goods_id`=67;
UPDATE `tb_menu_goods_relation` SET `menu_id`=21, `update_time`=NOW() WHERE `goods_id`=33;
UPDATE `tb_menu_goods_relation` SET `menu_id`=21, `update_time`=NOW() WHERE `goods_id`=71;
UPDATE `tb_menu_goods_relation` SET `menu_id`=21, `update_time`=NOW() WHERE `goods_id`=76;
UPDATE `tb_menu_goods_relation` SET `menu_id`=21, `update_time`=NOW() WHERE `goods_id`=56;
UPDATE `tb_menu_goods_relation` SET `menu_id`=22, `update_time`=NOW() WHERE `goods_id`=46;
UPDATE `tb_menu_goods_relation` SET `menu_id`=22, `update_time`=NOW() WHERE `goods_id`=72;
UPDATE `tb_menu_goods_relation` SET `menu_id`=22, `update_time`=NOW() WHERE `goods_id`=38;
UPDATE `tb_menu_goods_relation` SET `menu_id`=22, `update_time`=NOW() WHERE `goods_id`=48;
UPDATE `tb_menu_goods_relation` SET `menu_id`=22, `update_time`=NOW() WHERE `goods_id`=81;
UPDATE `tb_menu_goods_relation` SET `menu_id`=22, `update_time`=NOW() WHERE `goods_id`=65;
UPDATE `tb_menu_goods_relation` SET `menu_id`=18, `update_time`=NOW() WHERE `goods_id`=70;
UPDATE `tb_menu_goods_relation` SET `menu_id`=18, `update_time`=NOW() WHERE `goods_id`=45;
UPDATE `tb_menu_goods_relation` SET `menu_id`=18, `update_time`=NOW() WHERE `goods_id`=261;
UPDATE `tb_menu_goods_relation` SET `menu_id`=23, `update_time`=NOW() WHERE `goods_id`=80;
UPDATE `tb_menu_goods_relation` SET `menu_id`=23, `update_time`=NOW() WHERE `goods_id`=60;
UPDATE `tb_menu_goods_relation` SET `menu_id`=23, `update_time`=NOW() WHERE `goods_id`=74;
UPDATE `tb_menu_goods_relation` SET `menu_id`=23, `update_time`=NOW() WHERE `goods_id`=63;
UPDATE `tb_menu_goods_relation` SET `menu_id`=19, `update_time`=NOW() WHERE `goods_id`=75;
UPDATE `tb_menu_goods_relation` SET `menu_id`=19, `update_time`=NOW() WHERE `goods_id`=47;
UPDATE `tb_menu_goods_relation` SET `menu_id`=19, `update_time`=NOW() WHERE `goods_id`=68;
UPDATE `tb_menu_goods_relation` SET `menu_id`=19, `update_time`=NOW() WHERE `goods_id`=64;
UPDATE `tb_menu_goods_relation` SET `menu_id`=20, `update_time`=NOW() WHERE `goods_id`=62;
UPDATE `tb_menu_goods_relation` SET `menu_id`=20, `update_time`=NOW() WHERE `goods_id`=66;
UPDATE `tb_menu_goods_relation` SET `menu_id`=20, `update_time`=NOW() WHERE `goods_id`=77;
UPDATE `tb_menu_goods_relation` SET `menu_id`=20, `update_time`=NOW() WHERE `goods_id`=78;
UPDATE `tb_menu_goods_relation` SET `menu_id`=17, `update_time`=NOW() WHERE `goods_id`=51;
UPDATE `tb_menu_goods_relation` SET `menu_id`=17, `update_time`=NOW() WHERE `goods_id`=39;
UPDATE `tb_menu_goods_relation` SET `menu_id`=17, `update_time`=NOW() WHERE `goods_id`=49;
UPDATE `tb_menu_goods_relation` SET `menu_id`=17, `update_time`=NOW() WHERE `goods_id`=57;
UPDATE `tb_menu_goods_relation` SET `menu_id`=17, `update_time`=NOW() WHERE `goods_id`=28;
UPDATE `tb_menu_goods_relation` SET `menu_id`=17, `update_time`=NOW() WHERE `goods_id`=58;
UPDATE `tb_menu_goods_relation` SET `menu_id`=17, `update_time`=NOW() WHERE `goods_id`=82;
UPDATE `tb_menu_goods_relation` SET `menu_id`=26, `update_time`=NOW() WHERE `goods_id`=61;
UPDATE `tb_menu_goods_relation` SET `menu_id`=26, `update_time`=NOW() WHERE `goods_id`=73;
UPDATE `tb_menu_goods_relation` SET `menu_id`=26, `update_time`=NOW() WHERE `goods_id`=69;
UPDATE `tb_menu_goods_relation` SET `menu_id`=26, `update_time`=NOW() WHERE `goods_id`=59;

-- ---------- 4. 规格项重命名（tb_goods_specification） ----------
UPDATE `tb_goods_specification` SET `name`='杯型', `update_time`=NOW() WHERE `name`='规格' AND `goods_id` IN (SELECT id FROM `tb_goods` WHERE `shop_id`=13);
UPDATE `tb_goods_specification` SET `name`='冰量', `update_time`=NOW() WHERE `name`='温度' AND `goods_id` IN (SELECT id FROM `tb_goods` WHERE `shop_id`=13);
UPDATE `tb_goods_specification` SET `name`='加料', `update_time`=NOW() WHERE `name`='添加' AND `goods_id` IN (SELECT id FROM `tb_goods` WHERE `shop_id`=13);
UPDATE `tb_goods_specification` SET `name`='甜度', `update_time`=NOW() WHERE `name`='糖度' AND `goods_id` IN (SELECT id FROM `tb_goods` WHERE `shop_id`=13);

-- ---------- 5. 规格选项重命名（tb_goods_specification_option） ----------
UPDATE `tb_goods_specification_option` SET `name`='标准杯', `update_time`=NOW() WHERE `name`='中杯' AND `goods_id` IN (SELECT id FROM `tb_goods` WHERE `shop_id`=13);
UPDATE `tb_goods_specification_option` SET `name`='标准杯', `update_time`=NOW() WHERE `name`='中杯/热' AND `goods_id` IN (SELECT id FROM `tb_goods` WHERE `shop_id`=13);
UPDATE `tb_goods_specification_option` SET `name`='大杯', `update_time`=NOW() WHERE `name`='大杯' AND `goods_id` IN (SELECT id FROM `tb_goods` WHERE `shop_id`=13);
UPDATE `tb_goods_specification_option` SET `name`='250ml', `update_time`=NOW() WHERE `name`='约250ML' AND `goods_id` IN (SELECT id FROM `tb_goods` WHERE `shop_id`=13);
UPDATE `tb_goods_specification_option` SET `name`='500ml', `update_time`=NOW() WHERE `name`='约500ML' AND `goods_id` IN (SELECT id FROM `tb_goods` WHERE `shop_id`=13);
UPDATE `tb_goods_specification_option` SET `name`='500ml', `update_time`=NOW() WHERE `name`='500ML' AND `goods_id` IN (SELECT id FROM `tb_goods` WHERE `shop_id`=13);
UPDATE `tb_goods_specification_option` SET `name`='700ml', `update_time`=NOW() WHERE `name`='700ML' AND `goods_id` IN (SELECT id FROM `tb_goods` WHERE `shop_id`=13);
UPDATE `tb_goods_specification_option` SET `name`='加冰', `update_time`=NOW() WHERE `name`='冰' AND `goods_id` IN (SELECT id FROM `tb_goods` WHERE `shop_id`=13);
UPDATE `tb_goods_specification_option` SET `name`='加冰', `update_time`=NOW() WHERE `name`='常规冰' AND `goods_id` IN (SELECT id FROM `tb_goods` WHERE `shop_id`=13);
UPDATE `tb_goods_specification_option` SET `name`='热饮', `update_time`=NOW() WHERE `name`='热' AND `goods_id` IN (SELECT id FROM `tb_goods` WHERE `shop_id`=13);
UPDATE `tb_goods_specification_option` SET `name`='常温', `update_time`=NOW() WHERE `name`='温' AND `goods_id` IN (SELECT id FROM `tb_goods` WHERE `shop_id`=13);
UPDATE `tb_goods_specification_option` SET `name`='去冰', `update_time`=NOW() WHERE `name`='去冰' AND `goods_id` IN (SELECT id FROM `tb_goods` WHERE `shop_id`=13);
UPDATE `tb_goods_specification_option` SET `name`='多冰', `update_time`=NOW() WHERE `name`='多冰' AND `goods_id` IN (SELECT id FROM `tb_goods` WHERE `shop_id`=13);
UPDATE `tb_goods_specification_option` SET `name`='少冰', `update_time`=NOW() WHERE `name`='少冰' AND `goods_id` IN (SELECT id FROM `tb_goods` WHERE `shop_id`=13);
UPDATE `tb_goods_specification_option` SET `name`='常温', `update_time`=NOW() WHERE `name`='常温' AND `goods_id` IN (SELECT id FROM `tb_goods` WHERE `shop_id`=13);
UPDATE `tb_goods_specification_option` SET `name`='标准糖', `update_time`=NOW() WHERE `name`='正常糖' AND `goods_id` IN (SELECT id FROM `tb_goods` WHERE `shop_id`=13);
UPDATE `tb_goods_specification_option` SET `name`='标准糖', `update_time`=NOW() WHERE `name`='常规糖' AND `goods_id` IN (SELECT id FROM `tb_goods` WHERE `shop_id`=13);
UPDATE `tb_goods_specification_option` SET `name`='常规', `update_time`=NOW() WHERE `name`='常规' AND `goods_id` IN (SELECT id FROM `tb_goods` WHERE `shop_id`=13);
UPDATE `tb_goods_specification_option` SET `name`='不加糖', `update_time`=NOW() WHERE `name`='不加糖' AND `goods_id` IN (SELECT id FROM `tb_goods` WHERE `shop_id`=13);
UPDATE `tb_goods_specification_option` SET `name`='不加糖', `update_time`=NOW() WHERE `name`='不另外加糖' AND `goods_id` IN (SELECT id FROM `tb_goods` WHERE `shop_id`=13);
UPDATE `tb_goods_specification_option` SET `name`='无糖', `update_time`=NOW() WHERE `name`='女士专选无糖' AND `goods_id` IN (SELECT id FROM `tb_goods` WHERE `shop_id`=13);
UPDATE `tb_goods_specification_option` SET `name`='微甜', `update_time`=NOW() WHERE `name`='低糖' AND `goods_id` IN (SELECT id FROM `tb_goods` WHERE `shop_id`=13);
UPDATE `tb_goods_specification_option` SET `name`='半甜', `update_time`=NOW() WHERE `name`='中糖' AND `goods_id` IN (SELECT id FROM `tb_goods` WHERE `shop_id`=13);
UPDATE `tb_goods_specification_option` SET `name`='半甜', `update_time`=NOW() WHERE `name`='半糖' AND `goods_id` IN (SELECT id FROM `tb_goods` WHERE `shop_id`=13);
UPDATE `tb_goods_specification_option` SET `name`='微甜', `update_time`=NOW() WHERE `name`='微糖' AND `goods_id` IN (SELECT id FROM `tb_goods` WHERE `shop_id`=13);
UPDATE `tb_goods_specification_option` SET `name`='不加', `update_time`=NOW() WHERE `name`='不加' AND `goods_id` IN (SELECT id FROM `tb_goods` WHERE `shop_id`=13);
UPDATE `tb_goods_specification_option` SET `name`='加柠檬片', `price`=2.00, `update_time`=NOW() WHERE `name`='珍珠' AND `goods_id` IN (SELECT id FROM `tb_goods` WHERE `shop_id`=13);
UPDATE `tb_goods_specification_option` SET `name`='加薄荷叶', `price`=2.00, `update_time`=NOW() WHERE `name`='红豆' AND `goods_id` IN (SELECT id FROM `tb_goods` WHERE `shop_id`=13);
UPDATE `tb_goods_specification_option` SET `name`='加冰块', `price`=0.00, `update_time`=NOW() WHERE `name`='椰果' AND `goods_id` IN (SELECT id FROM `tb_goods` WHERE `shop_id`=13);
UPDATE `tb_goods_specification_option` SET `name`='加苏打水', `price`=3.00, `update_time`=NOW() WHERE `name`='花生' AND `goods_id` IN (SELECT id FROM `tb_goods` WHERE `shop_id`=13);
UPDATE `tb_goods_specification_option` SET `name`='加青柠角', `price`=2.00, `update_time`=NOW() WHERE `name`='葡萄' AND `goods_id` IN (SELECT id FROM `tb_goods` WHERE `shop_id`=13);
UPDATE `tb_goods_specification_option` SET `name`='加糖浆', `price`=2.00, `update_time`=NOW() WHERE `name`='布丁' AND `goods_id` IN (SELECT id FROM `tb_goods` WHERE `shop_id`=13);
UPDATE `tb_goods_specification_option` SET `name`='加果汁', `price`=3.00, `update_time`=NOW() WHERE `name`='芒果' AND `goods_id` IN (SELECT id FROM `tb_goods` WHERE `shop_id`=13);
UPDATE `tb_goods_specification_option` SET `name`='加奶霜', `price`=3.00, `update_time`=NOW() WHERE `name`='奶盖' AND `goods_id` IN (SELECT id FROM `tb_goods` WHERE `shop_id`=13);

-- ---------- 6. 自检 ----------
SELECT m.`name` AS 分类, COUNT(g.`id`) AS 商品数, MIN(g.`price`) AS 最低价, MAX(g.`price`) AS 最高价
FROM `tb_menu` m LEFT JOIN `tb_menu_goods_relation` r ON r.`menu_id`=m.`id`
LEFT JOIN `tb_goods` g ON g.`id`=r.`goods_id` AND g.`status` IN (2,4)
WHERE m.`shop_id`=13 GROUP BY m.`id` ORDER BY m.`sort_number`;
-- 期望: 9 行，商品数合计 40，最低价 32，最高价 128
