-- ============================================================================
-- A-08 操作日志与审计（管理端）
--   1) tb_operate_log  操作日志（AOP 自动采集 + 业务侧 LogContext 记录变更前后值）
--   2) tb_login_log    登录日志（管理端/商家端登录成功与失败均记录）
-- 幂等：CREATE TABLE IF NOT EXISTS，重复执行不报错
-- ============================================================================

-- 1) 操作日志
CREATE TABLE IF NOT EXISTS `tb_operate_log` (
  `id`            bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `operator`      varchar(50)  DEFAULT NULL COMMENT '操作人账号',
  `account_type`  tinyint(2)   DEFAULT NULL COMMENT '账号类型 1=管理员 2=商家 3=用户',
  `module`        varchar(30)  DEFAULT NULL COMMENT '模块code(LOGIN/GOODS_PRICE/ORDER/STOCK/TABLE/WINE_STORAGE)',
  `module_name`   varchar(50)  DEFAULT NULL COMMENT '模块名称',
  `action`        varchar(50)  DEFAULT NULL COMMENT '动作(如 开台/转台/清台/修改商品价格)',
  `business_id`   varchar(50)  DEFAULT NULL COMMENT '业务ID',
  `request_method` varchar(10) DEFAULT NULL COMMENT '请求方式(GET/POST/PUT/DELETE)',
  `request_url`   varchar(255) DEFAULT NULL COMMENT '请求地址',
  `before_value`  text         DEFAULT NULL COMMENT '变更前值(JSON)',
  `after_value`   text         DEFAULT NULL COMMENT '变更后值(JSON)',
  `result`        tinyint(2)   DEFAULT NULL COMMENT '操作结果 1=成功 2=失败',
  `error_msg`     varchar(500) DEFAULT NULL COMMENT '失败原因',
  `ip`            varchar(50)  DEFAULT NULL COMMENT '操作IP',
  `ua`            varchar(255) DEFAULT NULL COMMENT 'User-Agent',
  `cost_ms`       bigint(20)   DEFAULT NULL COMMENT '耗时(毫秒)',
  `create_time`   datetime     DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_module` (`module`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';

-- 2) 登录日志
CREATE TABLE IF NOT EXISTS `tb_login_log` (
  `id`            bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `account`       varchar(50)  DEFAULT NULL COMMENT '登录账号',
  `account_type`  tinyint(2)   DEFAULT NULL COMMENT '账号类型 1=管理员 2=商家 3=用户',
  `ip`            varchar(50)  DEFAULT NULL COMMENT '登录IP',
  `region`        varchar(50)  DEFAULT NULL COMMENT '登录地点(预留，暂不解析)',
  `browser`       varchar(50)  DEFAULT NULL COMMENT '浏览器',
  `os`            varchar(50)  DEFAULT NULL COMMENT '操作系统',
  `result`        tinyint(2)   DEFAULT NULL COMMENT '登录结果 1=成功 2=失败',
  `message`       varchar(255) DEFAULT NULL COMMENT '提示信息(失败原因)',
  `create_time`   datetime     DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志';