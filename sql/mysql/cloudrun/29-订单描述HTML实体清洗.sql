-- ============================================================
-- 29 - 订单描述 HTML 实体清洗
-- ------------------------------------------------------------
-- 背景（2026-09-23 取证）：
--   后端生成订单描述时拼的是 HTML 实体：`"&nbsp;&nbsp;等" + N + "件"`，
--   实体作为**普通字符**落库。而 HTML 实体只在 HTML 解析环境或
--   uni-app `<text :decode="true">` 下才会还原成空格：
--     小程序 `<text>`（decode 默认 false）、`<view>`、Vue `{{ }}` 文本插值、
--     el-table 未挂 formatter 的单元格 —— **一律原样显示 `&nbsp;`**。
--   现象：顾客端「订单中心」卡片显示「芒果奶酒（新）&nbsp;&nbsp;等2件」。
--
-- 配套（已完成的代码改动）：
--   OrderServiceImpl.java / PointsMallOrderServiceImpl.java 改输出全角空格 U+3000；
--   顾客端、商家端、收银端补兜底解码。
--   本脚本负责把**存量**脏数据洗干净。
--
-- 影响范围（本地库实测）：tb_order 4/15 行、tb_points_mall_order 2/10 行。
-- 适用对象：本地库 siam_db 与云库，**两边都要跑，内容相同**。
--
-- 风险：只改 description 一个文本列，不触碰金额 / 状态 / 时间。
-- 回滚：执行前先跑 §1 并**保存输出**，需要还原时用 §4 的模板逐行写回。
-- 执行方式：建议在控制台**分节**粘贴（先 §1 取备份 → 再 §2 清洗 → 最后 §3 验证）。
-- ============================================================


-- ── §1 备份：先把受影响的原始值查出来，务必留存输出 ──────────────
SELECT id, description FROM tb_order            WHERE description LIKE '%&nbsp;%';
SELECT id, description FROM tb_points_mall_order WHERE description LIKE '%&nbsp;%';


-- ── §2 清洗 ──────────────────────────────────────────────────
-- U+3000（全角空格）用 UTF-8 字节 0xE38080 表示，避免脚本文件本身的编码被改动后失效；
-- 先替换两连（还原成后端新写法的一个全角空格），再把漏网的单个实体替换成半角空格。
UPDATE tb_order
   SET description = REPLACE(
                       REPLACE(description, '&nbsp;&nbsp;', CONVERT(UNHEX('E38080') USING utf8mb4)),
                       '&nbsp;', ' ')
 WHERE description LIKE '%&nbsp;%';

UPDATE tb_points_mall_order
   SET description = REPLACE(
                       REPLACE(description, '&nbsp;&nbsp;', CONVERT(UNHEX('E38080') USING utf8mb4)),
                       '&nbsp;', ' ')
 WHERE description LIKE '%&nbsp;%';


-- ── §3 验证：两个计数都应为 0 ────────────────────────────────
SELECT COUNT(*) AS order_left  FROM tb_order            WHERE description LIKE '%&nbsp;%';
SELECT COUNT(*) AS points_left FROM tb_points_mall_order WHERE description LIKE '%&nbsp;%';

-- 抽查清洗结果（应能看到全角空格，且不再出现 &nbsp;）
SELECT id, description FROM tb_order WHERE id IN (9014, 9010, 9009, 9004);


-- ── §4 回滚模板（把 §1 保存的原值填回去，一行一条）────────────
-- UPDATE tb_order            SET description = '<§1 里的原值>' WHERE id = <§1 里的 id>;
-- UPDATE tb_points_mall_order SET description = '<§1 里的原值>' WHERE id = <§1 里的 id>;
