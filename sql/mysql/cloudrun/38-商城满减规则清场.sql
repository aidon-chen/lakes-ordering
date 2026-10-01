-- ============================================================================
-- 38-商城满减规则清场.sql
-- ============================================================================
-- 批次：2026-10-01「商城满减规则清场 B 批·B2」（批次文档 未发布-20260930-barmvp045-barweb053-mp1.2.2，§2.4-5）
-- 决策：ADR-0002「商城满减并入券体系（满减规则下线）」；「列在表不在」阅读困惑由其解释
-- 配套代码（bar-mvp-045，含 B1-1，已落码待上云）：
--   删商城域满减 8 文件（实体/Example/Service/Impl/Mapper/Mapper.xml/admin+member 两 Controller）
--   PointsMallOrderServiceImpl 删「规则套用校验」分支 → 后端已无任何该表引用
--   → 线上端点 /rest/{admin,}pointsMall/fullReductionRule/** 将随 bar-mvp-045 变 404
-- 本地库：siam_db 已于 2026-10-01 12:4x 执行本脚本（含同库备份表 tb_points_mall_full_reduction_rule_bak_20261001），
--   另备 dump 文件 order-project/local-run/backup/20261001-drop-tb_points_mall_full_reduction_rule.sql（含 1 行数据）
-- 云库：已于 2026-10-01 13:0x 经公网通道执行（3/3 提交；备份表同名在云；执行时 bar-mvp-045 未上线——
--   经确认 ≤1.2.0 带 ruleId 流量为 0（门店未发布）、风险≈0；如遇复核/重跑场景仍建议后端先行）
--
-- 前置预检（执行前先查，见下「复核」）：
--   SHOW TABLES LIKE 'tb_points_mall_full_reduction_rule';
--   SELECT * FROM tb_points_mall_full_reduction_rule;   -- 期望 1 行：id=2「满99减10」（99.00/10.00）
--   ⚠ 执行顺序前置：bar-mvp-045（B1-1）先上线。旧后端 bar-mvp-044 内仍有「规则套用校验」
--     引用（老版 ≤1.2.0 客户端带 ruleId 下单会命中）；当前实测无此类流量，但按序执行最稳。
-- ============================================================================

-- ▼▼▼ 生产执行区 ▼▼▼

-- ① 备份到同库备份表（重跑语义：整脚本仅在「主表存在」时可跑；若 ③ 已执行则已完成、无需重跑）
DROP TABLE IF EXISTS `tb_points_mall_full_reduction_rule_bak_20261001`;
CREATE TABLE `tb_points_mall_full_reduction_rule_bak_20261001` AS
  SELECT * FROM `tb_points_mall_full_reduction_rule`;

-- ② 清场：删除规则表（数据已备份至 tb_points_mall_full_reduction_rule_bak_20261001）
DROP TABLE IF EXISTS `tb_points_mall_full_reduction_rule`;

-- 说明：
-- · 该表＝商城「自动满减规则」（门店级）。满减能力已并入券体系（券模板优惠形态=2，领券后生效），
--   规则表清除后商城优惠路径唯一＝优惠券，消除「假开关」「一单双减」两个资金风险点。
-- · 订单侧历史列（full_reduction_rule_id / full_reduction_rule_description / limited_price /
--   reduced_price / 退款表 is_used_full_reduction_rule）**保留不删**：历史单退款金额口径仍读它们。
--   ⇒「列在表不在」为预期现象，解释见 ADR-0002，禁止据此把规则表接回来。
-- · 表为独立配置表，无外键；订单表存的是历史值快照，不依赖本表存在。

-- ▼▼▼ 回滚区（如需回退） ▼▼▼
-- 1) 从同库备份表恢复（推荐，保留原始数据）:
--    CREATE TABLE `tb_points_mall_full_reduction_rule` LIKE `tb_points_mall_full_reduction_rule_bak_20261001`;
--    INSERT INTO `tb_points_mall_full_reduction_rule` SELECT * FROM `tb_points_mall_full_reduction_rule_bak_20261001`;
--    -- 恢复后核对：SELECT * FROM tb_points_mall_full_reduction_rule;  → 1 行 id=2「满99减10」
-- 2) 仅恢复空表结构（数据不恢复）:
--    CREATE TABLE `tb_points_mall_full_reduction_rule` (
--      `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键id',
--      `shop_id` int(11) DEFAULT NULL COMMENT '店铺id',
--      `name` varchar(100) DEFAULT NULL COMMENT '规则名称',
--      `status` int(11) DEFAULT NULL COMMENT '活动状态，1=开启，2=关闭',
--      `limited_price` decimal(10,2) DEFAULT '0.00' COMMENT '满足价格（元，满足该价格才能使用）',
--      `reduced_price` decimal(10,2) DEFAULT '0.00' COMMENT '减价额度(元)',
--      `create_time` datetime DEFAULT NULL COMMENT '创建时间',
--      `update_time` datetime DEFAULT NULL COMMENT '修改时间',
--      PRIMARY KEY (`id`)
--    ) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;
-- 3) 本地库恢复：回放 order-project/local-run/backup/20261001-drop-tb_points_mall_full_reduction_rule.sql
-- 4) 代码回滚：git checkout 8 个后端文件 + PointsMallOrderServiceImpl → 带锁重建 jar 发版。
--    ⚠ 顺序：先回滚代码（或至少与之同批），否则线上代码找不到表会报错（反向同理）。
-- ============================================================================
