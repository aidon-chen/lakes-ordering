-- ============================================================================
-- 35-删除奖励金分销体系表与列.sql
-- ============================================================================
-- 批次：2026-09-28「奖励金/分销/邀请关系整链下线」
-- 配套代码：siam-server 删 MemberWithdrawRecord*/MemberInviteRelation*/Reward*
--           三组类 + OrderServiceImpl 等调用点（本地已编译验证，未上云）
-- 前置备份：执行前先对生产库跑
--   mysqldump <inst> siam_db tb_member_withdraw_record tb_member_invite_relation
--   mysqldump <inst> siam_db --no-data tb_member
-- 状态：✅ 已执行（09-29 10:0x，先备份后删；备份见 local-run/backup/2026-09-29-cloud-35-pre.sql）
-- ============================================================================

-- ▼▼▼ 生产执行区 ▼▼▼
-- 1) 删奖励金提现记录表（含历史提现申请数据；如需保留审计请先归档备份）
DROP TABLE IF EXISTS tb_member_withdraw_record;

-- 2) 删邀请关系表
DROP TABLE IF EXISTS tb_member_invite_relation;

-- 3) tb_member 删奖励金/邀请体系列（invite_code 为管理端搜索字段，保留）
ALTER TABLE tb_member
  DROP COLUMN invite_reward_amount,             -- 可提现奖励金
  DROP COLUMN unreceived_invite_reward_amount,  -- 未结算奖励金
  DROP COLUMN total_withdraw_invite_reward_amount, -- 累计已提现奖励金
  DROP COLUMN invite_suncode;                   -- 邀请分享太阳码路径

-- 说明：
-- · tb_member_billing_record 存量佣金/奖励金账单行【保留】，仅作历史审计留痕；
--   代码层已删除对应 TYPE_/COIN_TYPE_ 常量与结算分支。
-- · tb_setting 的佣金比例列（invitee_consume_commission 等）保留不动（无 UI 消费）。
-- · 商家提现链路（tb_merchant_withdraw_record）不受影响。

-- ▼▼▼ 回滚区（如需回退） ▼▼▼
-- 回滚表结构（数据须从备份恢复）：
-- CREATE TABLE tb_member_withdraw_record (...);   -- 结构见执行前 mysqldump 备份
-- CREATE TABLE tb_member_invite_relation (...);   -- 同上
-- ALTER TABLE tb_member
--   ADD COLUMN invite_reward_amount decimal(10,2) DEFAULT NULL,
--   ADD COLUMN unreceived_invite_reward_amount decimal(10,2) DEFAULT NULL,
--   ADD COLUMN total_withdraw_invite_reward_amount decimal(10,2) DEFAULT NULL,
--   ADD COLUMN invite_suncode varchar(255) DEFAULT NULL;
-- 代码回滚：git revert 对应提交后重新构建发版。
