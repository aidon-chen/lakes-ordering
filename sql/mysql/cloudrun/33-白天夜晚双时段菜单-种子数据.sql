-- =====================================================================
-- 33-白天夜晚双时段菜单-种子数据.sql
-- 目标：shop13「湖里Lakes」分类整体换成白天/夜晚双时段主题，商品按观感重新挂载
--   白天(day)   ：咖啡 / 奶茶果茶 / 甜品 / 其他        （18 条商品，全部无酒精）
--   夜晚(night)：鸡尾酒 / 调酒 / 啤酒 / 小食 / 其他   （22 条商品，酒饮＋小食）
-- 原则：一律 UPDATE 按 id 定位（商品/分类 id 不动，购物车/券/订单快照零影响）；
--       仅小食档 4 条商品（原葡萄酒系）的旧饮品规格行做定向 DELETE
--       （tb_goods_specification(_option) 无任何外部引用，仅点单流程读）。
-- 回滚：sql/mysql/archive/shop13-点单菜单-酒水demo-备份-20260924.sql
-- 幂等：可重复执行，第二次结果不变
-- ⚠ 云库依赖：tb_menu.sale_period 由 31-营业时段-叠加设计.sql 建立（云库尚未执行），
--   云库执行顺序＝先 31 → 再本脚本；本地已就绪。
-- =====================================================================
SET NAMES utf8mb4;

-- ① 分类改名 + 时段打标 + 排序补齐（sort 1..4=白天，5..9=夜晚）
UPDATE tb_menu SET name='咖啡',     sort_number=1, sale_period='day',   update_time=NOW() WHERE id=24 AND shop_id=13;
UPDATE tb_menu SET name='奶茶果茶', sort_number=2, sale_period='day',   update_time=NOW() WHERE id=17 AND shop_id=13;
UPDATE tb_menu SET name='甜品',     sort_number=3, sale_period='day',   update_time=NOW() WHERE id=20 AND shop_id=13;
UPDATE tb_menu SET name='其他',     sort_number=4, sale_period='day',   update_time=NOW() WHERE id=19 AND shop_id=13;
UPDATE tb_menu SET name='鸡尾酒',   sort_number=5, sale_period='night', update_time=NOW() WHERE id=22 AND shop_id=13;
UPDATE tb_menu SET name='调酒',     sort_number=6, sale_period='night', update_time=NOW() WHERE id=18 AND shop_id=13;
UPDATE tb_menu SET name='啤酒',     sort_number=7, sale_period='night', update_time=NOW() WHERE id=21 AND shop_id=13;
UPDATE tb_menu SET name='小食',     sort_number=8, sale_period='night', update_time=NOW() WHERE id=23 AND shop_id=13;
UPDATE tb_menu SET name='其他',     sort_number=9, sale_period='night', update_time=NOW() WHERE id=26 AND shop_id=13;

-- ② 商品改名（白天档改无酒精身份、小食档改餐食身份；id/价格/图片/热门·新品标记/上下架·售罄状态全部不动）
-- 咖啡（24）
UPDATE tb_goods SET name='招牌燕麦拿铁', detail='双份浓缩与丝滑燕麦奶调和，坚果香明显，冰热皆宜。', update_time=NOW() WHERE id=41 AND shop_id=13;
UPDATE tb_goods SET name='宇治抹茶拿铁', detail='宇治抹茶现调鲜奶，茶香清苦回甘。', update_time=NOW() WHERE id=45 AND shop_id=13;
UPDATE tb_goods SET name='草莓拿铁', detail='鲜草莓果酱与拿铁融合，粉色治愈系。', update_time=NOW() WHERE id=67 AND shop_id=13;
-- 奶茶果茶（17）
UPDATE tb_goods SET name='生椰奶茶', detail='生椰乳与红茶底调和，清甜顺滑。', update_time=NOW() WHERE id=39 AND shop_id=13;
UPDATE tb_goods SET name='招牌黄金珍珠奶茶', detail='琥珀红茶底配黑糖珍珠，珍珠每日现煮。', update_time=NOW() WHERE id=51 AND shop_id=13;
UPDATE tb_goods SET name='满杯西柚水果茶（新）', detail='整颗西柚果肉与四季春茶底，满杯果感。', update_time=NOW() WHERE id=69 AND shop_id=13;
-- 甜品（20）
UPDATE tb_goods SET name='焦糖布丁奶茶', detail='现烤焦糖布丁与奶茶同杯，布丁每日限量。', update_time=NOW() WHERE id=28 AND shop_id=13;
UPDATE tb_goods SET name='仙草奶茶', detail='台式仙草冻配鲜奶茶底，滑嫩清爽。', update_time=NOW() WHERE id=57 AND shop_id=13;
UPDATE tb_goods SET name='黑糖芋圆鲜奶', detail='手作芋圆配黑糖鲜奶，浓郁绵密。', update_time=NOW() WHERE id=58 AND shop_id=13;
UPDATE tb_goods SET name='草莓奥利奥奶昔', detail='草莓与奥利奥碎打成奶昔，甜品杯口感。', update_time=NOW() WHERE id=82 AND shop_id=13;
-- 其他·白天（19）
UPDATE tb_goods SET name='红豆厚牛乳', detail='红豆沙与厚牛乳分层，甜香浓郁。', update_time=NOW() WHERE id=49 AND shop_id=13;
UPDATE tb_goods SET name='白桃气泡水（新）', detail='白桃果肉与气泡水，冰镇清爽。', update_time=NOW() WHERE id=59 AND shop_id=13;
UPDATE tb_goods SET name='青瓜薄荷气泡水（新）', detail='青瓜薄片与薄荷气泡水，草本清新。', update_time=NOW() WHERE id=61 AND shop_id=13;
UPDATE tb_goods SET name='冰酪蜜桃乌龙（新）', detail='蜜桃乌龙茶顶冰酪，先吃酪再喝茶。', update_time=NOW() WHERE id=73 AND shop_id=13;
-- 小食（23，原葡萄酒系 4 条转餐食）
UPDATE tb_goods SET name='香辣炸鸡拼盘', detail='现炸鸡腿肉配辣粉与双蘸酱，佐酒首选。', update_time=NOW() WHERE id=60 AND shop_id=13;
UPDATE tb_goods SET name='炭烤鸡翅', detail='炭火烤制，蜂蜜芥末与孜然双酱可选。', update_time=NOW() WHERE id=63 AND shop_id=13;
UPDATE tb_goods SET name='黄金虾球', detail='整虾裹浆炸至金黄，配泰式甜辣酱。', update_time=NOW() WHERE id=74 AND shop_id=13;
UPDATE tb_goods SET name='混合坚果拼盘', detail='腰果巴旦木蔓越莓组合，佐酒小食。', update_time=NOW() WHERE id=80 AND shop_id=13;

-- ③ 分类挂载重排（每条商品仍只挂一个分类，共 40 条）
-- 白天·咖啡（24）
UPDATE tb_menu_goods_relation SET menu_id=24 WHERE goods_id=41;
UPDATE tb_menu_goods_relation SET menu_id=24 WHERE goods_id=45;
UPDATE tb_menu_goods_relation SET menu_id=24 WHERE goods_id=67;
-- 白天·奶茶果茶（17）
UPDATE tb_menu_goods_relation SET menu_id=17 WHERE goods_id=39;
UPDATE tb_menu_goods_relation SET menu_id=17 WHERE goods_id=51;
UPDATE tb_menu_goods_relation SET menu_id=17 WHERE goods_id=69;
UPDATE tb_menu_goods_relation SET menu_id=17 WHERE goods_id=62;
UPDATE tb_menu_goods_relation SET menu_id=17 WHERE goods_id=66;
UPDATE tb_menu_goods_relation SET menu_id=17 WHERE goods_id=77;
-- 白天·甜品（20）
UPDATE tb_menu_goods_relation SET menu_id=20 WHERE goods_id=28;
UPDATE tb_menu_goods_relation SET menu_id=20 WHERE goods_id=57;
UPDATE tb_menu_goods_relation SET menu_id=20 WHERE goods_id=58;
UPDATE tb_menu_goods_relation SET menu_id=20 WHERE goods_id=82;
UPDATE tb_menu_goods_relation SET menu_id=20 WHERE goods_id=78;
-- 白天·其他（19）
UPDATE tb_menu_goods_relation SET menu_id=19 WHERE goods_id=49;
UPDATE tb_menu_goods_relation SET menu_id=19 WHERE goods_id=59;
UPDATE tb_menu_goods_relation SET menu_id=19 WHERE goods_id=61;
UPDATE tb_menu_goods_relation SET menu_id=19 WHERE goods_id=73;
-- 夜晚·鸡尾酒（22）
UPDATE tb_menu_goods_relation SET menu_id=22 WHERE goods_id=38;
UPDATE tb_menu_goods_relation SET menu_id=22 WHERE goods_id=46;
UPDATE tb_menu_goods_relation SET menu_id=22 WHERE goods_id=48;
UPDATE tb_menu_goods_relation SET menu_id=22 WHERE goods_id=52;
UPDATE tb_menu_goods_relation SET menu_id=22 WHERE goods_id=53;
UPDATE tb_menu_goods_relation SET menu_id=22 WHERE goods_id=65;
UPDATE tb_menu_goods_relation SET menu_id=22 WHERE goods_id=72;
UPDATE tb_menu_goods_relation SET menu_id=22 WHERE goods_id=81;
-- 夜晚·调酒（18）
UPDATE tb_menu_goods_relation SET menu_id=18 WHERE goods_id=47;
UPDATE tb_menu_goods_relation SET menu_id=18 WHERE goods_id=64;
UPDATE tb_menu_goods_relation SET menu_id=18 WHERE goods_id=68;
UPDATE tb_menu_goods_relation SET menu_id=18 WHERE goods_id=75;
-- 夜晚·啤酒（21）
UPDATE tb_menu_goods_relation SET menu_id=21 WHERE goods_id=33;
UPDATE tb_menu_goods_relation SET menu_id=21 WHERE goods_id=56;
UPDATE tb_menu_goods_relation SET menu_id=21 WHERE goods_id=71;
UPDATE tb_menu_goods_relation SET menu_id=21 WHERE goods_id=76;
-- 夜晚·小食（23）
UPDATE tb_menu_goods_relation SET menu_id=23 WHERE goods_id=60;
UPDATE tb_menu_goods_relation SET menu_id=23 WHERE goods_id=63;
UPDATE tb_menu_goods_relation SET menu_id=23 WHERE goods_id=74;
UPDATE tb_menu_goods_relation SET menu_id=23 WHERE goods_id=80;
-- 夜晚·其他（26）
UPDATE tb_menu_goods_relation SET menu_id=26 WHERE goods_id=70;
UPDATE tb_menu_goods_relation SET menu_id=26 WHERE goods_id=261;

-- ④ 小食档旧饮品规格清理（杯型/冰量等对餐食无意义；定向 DELETE，幂等——第二次执行删 0 行）
DELETE FROM tb_goods_specification_option WHERE goods_specification_id IN (SELECT id FROM tb_goods_specification WHERE goods_id IN (60,63,74,80));
DELETE FROM tb_goods_specification WHERE goods_id IN (60,63,74,80);

-- ⑤ 自检（分类×商品数×时段）
SELECT m.sort_number AS 序, m.name AS 分类, m.sale_period AS 时段, COUNT(r.goods_id) AS 商品数
FROM tb_menu m LEFT JOIN tb_menu_goods_relation r ON r.menu_id=m.id
WHERE m.shop_id=13 GROUP BY m.id, m.sort_number, m.name, m.sale_period ORDER BY m.sort_number;
-- 白天档商品应为 18、夜晚档 22；售罄 1 条（56 南瓜艾尔，挂啤酒）
SELECT m.sale_period AS 时段, COUNT(*) AS 商品数
FROM tb_menu m JOIN tb_menu_goods_relation r ON r.menu_id=m.id
JOIN tb_goods g ON g.id=r.goods_id
WHERE m.shop_id=13 AND g.status IN (2,4) GROUP BY m.sale_period;
