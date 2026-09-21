-- =============================================================================
-- 存酒取酒 · 登录态「取酒申请」改造（方案 v1.1）· 结构变更
-- 归属：C-16 存酒查询（范围扩展）＋ M-06 存酒管理（商家端）
-- 内容：
--   1) 新增取酒申请表 tb_wine_take_apply
--   2) tb_wine_storage_record 增列 apply_id（取酒记录 ↔ 取酒申请 对账）
--   3) tb_wine_storage.storage_code 改 NULL（为删列铺路，本步不可省）
-- 说明：storage_code 的 DROP 在后端发版且各端无引用后单独执行（见 26-）
-- 执行库：本地 siam_db ＋ 云库（两库都要，先库后发版）
-- 幂等：CREATE TABLE IF NOT EXISTS；重复 ALTER 报 1060/1091 可忽略
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1) 取酒申请表（状态 1 起步，与 tb_wine_storage 同惯例）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tb_wine_take_apply (
  id           bigint       NOT NULL AUTO_INCREMENT COMMENT '主键id',
  member_id    int          NOT NULL COMMENT '会员id',
  shop_id      int          NOT NULL COMMENT '门店id',
  storage_id   int          NOT NULL COMMENT '存酒记录id(tb_wine_storage.id，列类型与之一致 int)',
  take_count   int          NOT NULL COMMENT '申请取酒瓶数',
  take_code    varchar(6)   NOT NULL COMMENT '一次性取酒码(6位随机，首位非0)',
  status       tinyint      NOT NULL DEFAULT 1 COMMENT '1待核销 2已核销 3已取消 4已过期',
  apply_source tinyint      NOT NULL DEFAULT 1 COMMENT '1顾客自助 2商家代发起',
  apply_by     int          NULL     COMMENT '代发起人(商家id)，顾客自助为null',
  apply_time   datetime     NOT NULL COMMENT '申请时间',
  expire_time  datetime     NULL     COMMENT '取酒码有效期截止(apply_time+24h)',
  confirm_time datetime     NULL     COMMENT '核销时间',
  confirm_by   int          NULL     COMMENT '核销操作人(商家id)',
  cancel_time  datetime     NULL     COMMENT '取消时间',
  remark       varchar(200) NULL     COMMENT '备注(商家代发起可填)',
  create_time  datetime     NULL,
  update_time  datetime     NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_take_code (take_code),
  KEY idx_member (member_id),
  KEY idx_shop_status (shop_id, status),
  KEY idx_storage (storage_id),
  CONSTRAINT fk_wta_storage FOREIGN KEY (storage_id) REFERENCES tb_wine_storage (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='取酒申请表';

-- ---------------------------------------------------------------------------
-- 2) 取酒记录表增列 apply_id（该表现 0 行，零迁移风险）
-- ---------------------------------------------------------------------------
ALTER TABLE tb_wine_storage_record
  ADD COLUMN apply_id bigint NULL COMMENT '取酒申请id(tb_wine_take_apply.id)' AFTER storage_id;

-- ---------------------------------------------------------------------------
-- 3) storage_code 改 NULL
--    作用不是客户端兼容，而是避免「新后端不写该列」撞上「列仍 NOT NULL」
--    先改 NULL，则新旧后端都不报错；之后再 DROP。
-- ---------------------------------------------------------------------------
ALTER TABLE tb_wine_storage
  MODIFY COLUMN storage_code varchar(16) NULL COMMENT '存酒码(唯一，4位数字，已废弃)';

-- ---------------------------------------------------------------------------
-- 执行后自检
-- ---------------------------------------------------------------------------
-- a) 新表存在（应 1 行）
SELECT table_name FROM information_schema.tables
WHERE table_schema = 'siam_db' AND table_name = 'tb_wine_take_apply';
-- b) apply_id 存在（应 1 行）
SELECT column_name, column_type, is_nullable FROM information_schema.columns
WHERE table_schema = 'siam_db' AND table_name = 'tb_wine_storage_record' AND column_name = 'apply_id';
-- c) storage_code 已可空（is_nullable 应为 YES）
SELECT column_name, is_nullable FROM information_schema.columns
WHERE table_schema = 'siam_db' AND table_name = 'tb_wine_storage' AND column_name = 'storage_code';

-- ---------------------------------------------------------------------------
-- 回滚（与上面严格互逆）
-- ---------------------------------------------------------------------------
-- ALTER TABLE tb_wine_storage MODIFY COLUMN storage_code varchar(16) NOT NULL COMMENT '存酒码(唯一)';
-- ALTER TABLE tb_wine_storage_record DROP COLUMN apply_id;
-- DROP TABLE tb_wine_take_apply;
