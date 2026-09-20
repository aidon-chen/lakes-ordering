-- ============================================================
-- 16-管理端M组-弹窗与优惠券扩展.sql
-- 说明：管理端「营销组 M-01~M-04」后端字段扩展
--   M-01/M-02：首页活动弹窗配置（tb_advertisement 列已存在，仅补种子数据）
--   M-03/M-04：优惠券双券型（tb_coupons 补 3 列）
-- 注意：
--   1) ALTER TABLE 重复执行会报 1060（Duplicate column name），
--      即「列已存在时跳过」——若报 1060 属于正常现象，可忽略后执行剩余语句。
--   2) 其余语句（UPDATE / INSERT ... WHERE NOT EXISTS）均可重复执行。
-- ============================================================

-- tb_coupons 扩展：券型 / 使用须知 / 发放对象（M-03/M-04）
-- 列已存在时跳过（会报 1060）
ALTER TABLE tb_coupons
  ADD COLUMN issue_type tinyint NOT NULL DEFAULT 2 COMMENT '券型 1=统一发放 2=自行领取',
  ADD COLUMN notice varchar(500) NULL COMMENT '使用须知（客户端展示）',
  ADD COLUMN target_type tinyint NULL COMMENT '发放对象 1=全部用户 2=新用户 3=会员（仅统一发放券型使用）';
UPDATE tb_coupons SET issue_type = 2 WHERE issue_type = 1 AND target_type IS NULL AND id IS NOT NULL AND 1=0;

-- 弹窗种子数据（M-01/M-02 验收用：type=2 为首页活动弹窗）
INSERT INTO tb_advertisement (image_name, image_path, description, type, sort_number, image_link_url, start_time, end_time, popup_frequency, create_time, update_time)
SELECT '开业活动弹窗', '/upload/popup-activity.png', '开业活动弹窗', 2, 1, '', '2026-09-01 00:00:00', '2026-12-31 23:59:59', 2, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM tb_advertisement WHERE type = 2);