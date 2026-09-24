-- 回滚脚本：订单描述 HTML 实体清洗（2026-09-23）
-- 生成时间：2026-09-23 14:29:27
-- 用途：把 tb_order / tb_points_mall_order 的 description 还原为清洗前的原值
-- 用法：在 siam_db（本地）或云库控制台整段执行

-- tb_order
UPDATE tb_order SET description = '布丁奶酒特调&nbsp;&nbsp;等2件' WHERE id = 9004;
UPDATE tb_order SET description = '布丁奶酒特调&nbsp;&nbsp;等2件' WHERE id = 9009;
UPDATE tb_order SET description = '布丁奶酒特调&nbsp;&nbsp;等2件' WHERE id = 9010;
UPDATE tb_order SET description = '芒果奶酒（新）&nbsp;&nbsp;等2件' WHERE id = 9014;

-- tb_points_mall_order
UPDATE tb_points_mall_order SET description = '乐队主题帆布袋&nbsp;&nbsp;等5件' WHERE id = 47;
UPDATE tb_points_mall_order SET description = '乐队主题帆布袋&nbsp;&nbsp;等2件' WHERE id = 49;
