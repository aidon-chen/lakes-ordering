-- ============================================================
-- 32-会话表索引与唯一键（并发登录修复配套 DDL）
-- 背景：2026-09-24 L4 并发压测实证——三张 DB 会话表只有主键索引：
--   1) deleteByToken / getSession 全表扫描
--   2) 并发同账号登录的 delete/insert 构成 InnoDB 死锁环（280+ 次 Deadlock found）
--   3) 与「登录请求持有 controller 级事务 + LoginLog REQUIRES_NEW 第二连接」叠加 → 连接池自锁
-- 配套代码：TokenUtil token 唯一化(d2a52ca)、createSession 改单语句 upsert、LoginLog 去 REQUIRES_NEW
-- 执行顺序：本文件（DDL）→ 再发含 upsert 代码的后端版本（upsert 依赖唯一索引生效）
-- 幂等性：重复执行报 Duplicate key name，可忽略；去重 DELETE 幂等
-- ============================================================

-- 0) 前置去重：同账号只保留最新一条会话（upsert 依赖唯一键，必须先清历史重复行）
DELETE t1 FROM tb_admin_token t1 JOIN tb_admin_token t2 ON t1.admin_id = t2.admin_id AND t1.id < t2.id;
DELETE t1 FROM tb_merchant_token t1 JOIN tb_merchant_token t2 ON t1.merchant_id = t2.merchant_id AND t1.id < t2.id;
DELETE t1 FROM tb_member_token t1 JOIN tb_member_token t2 ON t1.member_id = t2.member_id AND t1.id < t2.id;

-- 1) token 查询索引（getSession 每次鉴权都要按 token 查）
ALTER TABLE tb_admin_token ADD INDEX idx_token (token);
ALTER TABLE tb_merchant_token ADD INDEX idx_token (token);
ALTER TABLE tb_member_token ADD INDEX idx_token (token);

-- 2) 账号唯一索引（单会话语义落库 + createSession 单语句 upsert 的前提）
ALTER TABLE tb_admin_token ADD UNIQUE KEY uk_admin_id (admin_id);
ALTER TABLE tb_merchant_token ADD UNIQUE KEY uk_merchant_id (merchant_id);
ALTER TABLE tb_member_token ADD UNIQUE KEY uk_member_id (member_id);
