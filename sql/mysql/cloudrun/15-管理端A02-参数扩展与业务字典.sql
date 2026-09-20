-- ============================================================================
-- 管理端 A-02 平台参数与字典配置
--   任务一：tb_setting 参数扩展（5 列）
--   任务二：业务字典（tb_dict_type / tb_dict_item）建表 + 种子数据
--
-- 幂等说明（手工执行）：
--   1) tb_setting 的 5 列使用 ALTER TABLE ... ADD COLUMN，MySQL 不支持 ADD COLUMN IF NOT EXISTS，
--      若目标库已存在同名列请手工跳过该列（重复执行会报 1060 Duplicate column name，属预期）。
--   2) 建表使用 CREATE TABLE IF NOT EXISTS，重复执行安全。
--   3) 种子数据使用 INSERT ... SELECT ... WHERE NOT EXISTS，重复执行不会产生重复数据。
--
-- 执行：mysql -uroot -proot siam_db < 15-管理端A02-参数扩展与业务字典.sql
-- ============================================================================


-- ---- 任务一：tb_setting 参数扩展（5 列）----
-- 若下列列已存在则跳过（见头部幂等说明第 1 条）
ALTER TABLE tb_setting
  ADD COLUMN business_hours_desc varchar(255) NULL COMMENT '营业时间说明',
  ADD COLUMN user_service_agreement text NULL COMMENT '用户服务协议',
  ADD COLUMN privacy_policy text NULL COMMENT '隐私政策',
  ADD COLUMN platform_logo varchar(255) NULL COMMENT '平台logo图',
  ADD COLUMN home_poster varchar(255) NULL COMMENT '首页海报位图';


-- ---- 任务二：业务字典表 ----
CREATE TABLE IF NOT EXISTS tb_dict_type (
  id int NOT NULL AUTO_INCREMENT,
  code varchar(50) NOT NULL COMMENT '类型编码',
  name varchar(50) NOT NULL COMMENT '类型名称',
  remark varchar(255) NULL COMMENT '备注',
  create_time datetime NULL, update_time datetime NULL,
  PRIMARY KEY (id), UNIQUE KEY uk_dict_type_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务字典类型';

CREATE TABLE IF NOT EXISTS tb_dict_item (
  id int NOT NULL AUTO_INCREMENT,
  type_code varchar(50) NOT NULL COMMENT '所属类型编码',
  label varchar(50) NOT NULL COMMENT '字典项名称',
  value varchar(50) NOT NULL COMMENT '字典项键值',
  sort int NOT NULL DEFAULT 0 COMMENT '排序',
  status tinyint NOT NULL DEFAULT 1 COMMENT '状态 1=启用 2=停用',
  remark varchar(255) NULL COMMENT '备注',
  create_time datetime NULL, update_time datetime NULL,
  PRIMARY KEY (id), KEY idx_dict_item_type_code (type_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务字典项';


-- ---- 种子数据：字典类型（3 条）----
INSERT INTO tb_dict_type (code, name, remark, create_time, update_time)
SELECT 'cup_size', '杯型', '杯型规格', NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM tb_dict_type WHERE code = 'cup_size');

INSERT INTO tb_dict_type (code, name, remark, create_time, update_time)
SELECT 'alcohol_degree', '酒水度数', '酒水度数档位', NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM tb_dict_type WHERE code = 'alcohol_degree');

INSERT INTO tb_dict_type (code, name, remark, create_time, update_time)
SELECT 'produce_area', '出品区', '出品区域', NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM tb_dict_type WHERE code = 'produce_area');


-- ---- 种子数据：字典项（杯型 2 条 / 酒水度数 3 条 / 出品区 2 条，共 7 条）----
INSERT INTO tb_dict_item (type_code, label, value, sort, status, remark, create_time, update_time)
SELECT 'cup_size', '中杯', 'middle', 1, 1, NULL, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM tb_dict_item WHERE type_code = 'cup_size' AND value = 'middle');

INSERT INTO tb_dict_item (type_code, label, value, sort, status, remark, create_time, update_time)
SELECT 'cup_size', '大杯', 'large', 2, 1, NULL, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM tb_dict_item WHERE type_code = 'cup_size' AND value = 'large');

INSERT INTO tb_dict_item (type_code, label, value, sort, status, remark, create_time, update_time)
SELECT 'alcohol_degree', '低度', 'low', 1, 1, NULL, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM tb_dict_item WHERE type_code = 'alcohol_degree' AND value = 'low');

INSERT INTO tb_dict_item (type_code, label, value, sort, status, remark, create_time, update_time)
SELECT 'alcohol_degree', '中度', 'middle', 2, 1, NULL, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM tb_dict_item WHERE type_code = 'alcohol_degree' AND value = 'middle');

INSERT INTO tb_dict_item (type_code, label, value, sort, status, remark, create_time, update_time)
SELECT 'alcohol_degree', '高度', 'high', 3, 1, NULL, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM tb_dict_item WHERE type_code = 'alcohol_degree' AND value = 'high');

INSERT INTO tb_dict_item (type_code, label, value, sort, status, remark, create_time, update_time)
SELECT 'produce_area', '吧台', 'bar', 1, 1, NULL, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM tb_dict_item WHERE type_code = 'produce_area' AND value = 'bar');

INSERT INTO tb_dict_item (type_code, label, value, sort, status, remark, create_time, update_time)
SELECT 'produce_area', '后厨', 'kitchen', 2, 1, NULL, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM tb_dict_item WHERE type_code = 'produce_area' AND value = 'kitchen');