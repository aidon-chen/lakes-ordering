-- =============================================================================
-- 会员身份认证申请记录表（顾客端 C-17 / C-17a / C-17b 原型）
-- 背景：原型 C-17「身份认证」两阶段 —— ① 实名认证（姓名 + 身份证人像面/国徽面）
--       ② 身份认证（学生证 或 教师资格证）；全部人工审核，不接核验 API、不做 OCR
-- 设计要点：
--   1. 独立建表，**不动 tb_member**（会员主表会被 getLoginMemberInfo 等接口整对象带出，
--      敏感材料字段挂上去会跟着进日志/缓存/前端本地存储）
--   2. 图片路径统一相对 ossUrl：https://siam-hangzhou.oss-cn-hangzhou.aliyuncs.com/ + 本字段
--   3. 驳回后重新提交 = 新建一条记录（保留完整审核历史），故以 create_time 倒序取最新一条为准
--   4. 合规「审核即弃」：原图 N 天后由后台任务清理，本表只留审核痕迹（见实施计划 §8）
-- 幂等：IF NOT EXISTS，可重复执行
-- =============================================================================

CREATE TABLE IF NOT EXISTS `tb_member_identity_auth` (
  `id`                   int(11)      NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `member_id`            int(11)      NOT NULL                COMMENT '会员id（tb_member.id）',
  `auth_type`            tinyint(2)   NOT NULL                COMMENT '认证类型 1=实名认证(身份证) 2=身份认证(学生/教师)',
  `identity_type`        tinyint(2)   DEFAULT NULL            COMMENT '身份类型 1=学生 2=教师；auth_type=2 时有值',
  `real_name`            varchar(64)  DEFAULT NULL            COMMENT '用户填写的真实姓名',
  `name_mask`            varchar(32)  DEFAULT NULL            COMMENT '脱敏姓名，对外展示用（如 张*）',
  `id_card_front`        varchar(255) DEFAULT NULL            COMMENT '身份证人像面图片路径（相对ossUrl）',
  `id_card_back`         varchar(255) DEFAULT NULL            COMMENT '身份证国徽面图片路径（相对ossUrl）',
  `student_card_img`     varchar(255) DEFAULT NULL            COMMENT '学生证信息页图片路径',
  `student_card_reg_img` varchar(255) DEFAULT NULL            COMMENT '学生证注册章页图片路径',
  `teacher_cert_img`     varchar(255) DEFAULT NULL            COMMENT '教师资格证图片路径',
  `school_name`          varchar(128) DEFAULT NULL            COMMENT '学校名称（选填）',
  `student_no`           varchar(64)  DEFAULT NULL            COMMENT '学号（选填）',
  `cert_no`              varchar(64)  DEFAULT NULL            COMMENT '教师资格证证书编号（选填）',
  `audit_status`         tinyint(2)   NOT NULL DEFAULT '1'    COMMENT '审核状态 1=待审核 2=通过 3=驳回',
  `audit_reason`         varchar(255) DEFAULT NULL            COMMENT '驳回理由（驳回时必填）',
  `audit_admin_id`       int(11)      DEFAULT NULL            COMMENT '审核人id（tb_admin.id）',
  `audit_time`           datetime     DEFAULT NULL            COMMENT '审核时间',
  `create_time`          datetime     DEFAULT NULL            COMMENT '提交时间',
  `update_time`          datetime     DEFAULT NULL            COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_member_type` (`member_id`, `auth_type`, `audit_status`),
  KEY `idx_audit_status` (`audit_status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='会员身份认证申请记录表（图片收集+人工审核）';

-- 执行后自检（应返回 1 行）
SELECT table_name, table_comment FROM information_schema.tables
WHERE table_schema = 'siam_db' AND table_name = 'tb_member_identity_auth';

-- 回滚（与上面严格互逆）
-- DROP TABLE IF EXISTS `tb_member_identity_auth`;
