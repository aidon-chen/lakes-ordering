-- =============================================================================
-- 取酒记录表补 update_time 列（批次 F / W-18 取酒核销）
-- 背景：WineStorageRecord 实体声明了 updateTime，MyBatis-Plus 的 insert 会带上该列：
--   INSERT INTO tb_wine_storage_record (storage_id, take_count, operator_id, take_time, create_time, update_time)
--   而 tb_wine_storage_record **从来没有 update_time 列**（仓库 sql/mysql/siam_db.sql 里也没有）
--   → 取酒核销接口 /rest/merchant/wineStorage/take 必然 500「系统异常」
-- 影响：仅新增 1 列；不改动既有数据
-- 幂等：重复执行报 1060 Duplicate column，可忽略
-- =============================================================================

ALTER TABLE tb_wine_storage_record
  ADD COLUMN update_time datetime DEFAULT NULL COMMENT '修改时间'
  AFTER create_time;

-- 执行后自检（应返回 1 行）
SELECT column_name, column_type, column_comment
FROM information_schema.columns
WHERE table_schema = 'siam_db' AND table_name = 'tb_wine_storage_record' AND column_name = 'update_time';

-- 回滚（与上面严格互逆）
-- ALTER TABLE tb_wine_storage_record DROP COLUMN update_time;
