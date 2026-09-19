-- ============================================================================
-- 商城优惠券种子数据（C-10 结算页「优惠券」链路打通）
-- 背景：tb_points_mall_coupons / tb_points_mall_coupons_member_relation 两表**均为 0 行**
--       → 结算页恒显「无可用优惠券」，选券链路无从验证
-- 特性：★全幂等 —— 先按显式 id DELETE 再 INSERT，可重复执行
-- 适用：本地 siam_db 与云托管 siam_db（会员 2=siam、17=15100000000）
-- ============================================================================

-- 一、商城优惠券模板（3 张：满50减8 对齐画板 -￥8.00 / 满100减15 / 满50打9折）
DELETE FROM `tb_points_mall_coupons` WHERE `id` IN (1,2,3);
INSERT INTO `tb_points_mall_coupons`
 (`id`,`name`,`preferential_type`,`discount_amount`,`limited_price`,`reduced_price`,`description`,`valid_type`,`valid_start_time`,`valid_end_time`,`valid_days`,`is_delete`,`source`,`create_time`,`update_time`) VALUES
 (1, '满50减8',   2, 1.00, 50.00,  8.00, '订单满 50 元可用，减 8 元',  1, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 90 DAY), 0, 0, 1, NOW(), NOW()),
 (2, '满100减15', 2, 1.00, 100.00, 15.00,'订单满 100 元可用，减 15 元', 1, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 90 DAY), 0, 0, 1, NOW(), NOW()),
 (3, '满50打9折',  1, 0.90, 50.00,  0.00, '订单满 50 元享 9 折',        1, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 90 DAY), 0, 0, 1, NOW(), NOW());

-- 二、发券给演示会员（会员 17=15100000000、2=siam 各 2 张未使用）
DELETE FROM `tb_points_mall_coupons_member_relation` WHERE `id` IN (1,2,3,4);
INSERT INTO `tb_points_mall_coupons_member_relation`
 (`id`,`coupons_id`,`coupons_name`,`member_id`,`start_time`,`end_time`,`is_used`,`is_expired`,`is_valid`,`create_time`,`update_time`) VALUES
 (1, 1, '满50减8',   17, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 90 DAY), 0, 0, 1, NOW(), NOW()),
 (2, 2, '满100减15', 17, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 90 DAY), 0, 0, 1, NOW(), NOW()),
 (3, 1, '满50减8',    2, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 90 DAY), 0, 0, 1, NOW(), NOW()),
 (4, 3, '满50打9折',  2, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 90 DAY), 0, 0, 1, NOW(), NOW());

-- 三、自检（应为 券模板 3 / 已发券 4 / 会员17可用 2）
SELECT '券模板' AS item, (SELECT COUNT(*) FROM tb_points_mall_coupons) AS n, 3 AS expect
UNION ALL SELECT '已发券', (SELECT COUNT(*) FROM tb_points_mall_coupons_member_relation), 4
UNION ALL SELECT '会员17可用', (SELECT COUNT(*) FROM tb_points_mall_coupons_member_relation WHERE member_id=17 AND is_used=0 AND is_valid=1 AND end_time > NOW()), 2;
