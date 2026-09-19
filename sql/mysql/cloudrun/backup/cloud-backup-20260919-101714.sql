-- 云库备份（执行批次6 §11 前自动导出）2026-09-19 10:17:14.497451

DELETE FROM `tb_points_mall_menu` WHERE id IN (1,2,3,4,5,6,7,8,9,10,11,12);
INSERT INTO `tb_points_mall_menu` (`id`,`shop_id`,`name`,`description`,`sort_number`,`icon`,`create_time`,`update_time`) VALUES (1,NULL,'吉他','',1,NULL,NULL,NULL);
INSERT INTO `tb_points_mall_menu` (`id`,`shop_id`,`name`,`description`,`sort_number`,`icon`,`create_time`,`update_time`) VALUES (2,NULL,'尤克里里','',2,NULL,NULL,NULL);
INSERT INTO `tb_points_mall_menu` (`id`,`shop_id`,`name`,`description`,`sort_number`,`icon`,`create_time`,`update_time`) VALUES (3,NULL,'打击乐','',3,NULL,NULL,NULL);
INSERT INTO `tb_points_mall_menu` (`id`,`shop_id`,`name`,`description`,`sort_number`,`icon`,`create_time`,`update_time`) VALUES (4,NULL,'管乐','',4,NULL,NULL,NULL);
INSERT INTO `tb_points_mall_menu` (`id`,`shop_id`,`name`,`description`,`sort_number`,`icon`,`create_time`,`update_time`) VALUES (5,NULL,'弦乐配件','',5,NULL,NULL,NULL);
INSERT INTO `tb_points_mall_menu` (`id`,`shop_id`,`name`,`description`,`sort_number`,`icon`,`create_time`,`update_time`) VALUES (6,NULL,'音响设备','',6,NULL,NULL,NULL);
INSERT INTO `tb_points_mall_menu` (`id`,`shop_id`,`name`,`description`,`sort_number`,`icon`,`create_time`,`update_time`) VALUES (7,NULL,'琴弦','',7,NULL,NULL,NULL);
INSERT INTO `tb_points_mall_menu` (`id`,`shop_id`,`name`,`description`,`sort_number`,`icon`,`create_time`,`update_time`) VALUES (8,NULL,'乐器包','',8,NULL,NULL,NULL);
INSERT INTO `tb_points_mall_menu` (`id`,`shop_id`,`name`,`description`,`sort_number`,`icon`,`create_time`,`update_time`) VALUES (9,NULL,'优惠专区','',9,NULL,NULL,NULL);
INSERT INTO `tb_points_mall_menu` (`id`,`shop_id`,`name`,`description`,`sort_number`,`icon`,`create_time`,`update_time`) VALUES (10,NULL,'会员专享','',10,NULL,NULL,NULL);
INSERT INTO `tb_points_mall_menu` (`id`,`shop_id`,`name`,`description`,`sort_number`,`icon`,`create_time`,`update_time`) VALUES (11,NULL,'新品上架','',11,NULL,NULL,NULL);
INSERT INTO `tb_points_mall_menu` (`id`,`shop_id`,`name`,`description`,`sort_number`,`icon`,`create_time`,`update_time`) VALUES (12,NULL,'周边文创','',12,NULL,NULL,NULL);

DELETE FROM `tb_points_mall_goods` WHERE id IN (101,102,103,104,105,106,107,108,109,110,111,112);
INSERT INTO `tb_points_mall_goods` (`id`,`shop_id`,`name`,`main_image`,`sub_images`,`detail`,`detail_images`,`price`,`stock`,`is_hot`,`is_new`,`status`,`is_sale`,`sale_price`,`monthly_sales`,`total_sales`,`total_comments`,`preferential_name`,`packing_charges`,`product_time`,`exchange_points`,`sort_number`,`create_time`,`update_time`) VALUES (101,NULL,'尤克里里 23 寸',NULL,NULL,'23 寸尤克里里，桃花心木面板，音色明亮通透；琴颈顺滑易按，附赠琴包与拨片。',NULL,'69.00',100,1,0,2,0,'0.00',328,328,0,NULL,'0.00','0.00',NULL,1,NULL,NULL);
INSERT INTO `tb_points_mall_goods` (`id`,`shop_id`,`name`,`main_image`,`sub_images`,`detail`,`detail_images`,`price`,`stock`,`is_hot`,`is_new`,`status`,`is_sale`,`sale_price`,`monthly_sales`,`total_sales`,`total_comments`,`preferential_name`,`packing_charges`,`product_time`,`exchange_points`,`sort_number`,`create_time`,`update_time`) VALUES (102,NULL,'口琴 24 孔',NULL,NULL,'24 孔 C 调口琴，金属簧片，适合民谣与蓝调入门。',NULL,'39.00',200,1,0,2,0,'0.00',156,156,0,NULL,'0.00','0.00',NULL,2,NULL,NULL);
INSERT INTO `tb_points_mall_goods` (`id`,`shop_id`,`name`,`main_image`,`sub_images`,`detail`,`detail_images`,`price`,`stock`,`is_hot`,`is_new`,`status`,`is_sale`,`sale_price`,`monthly_sales`,`total_sales`,`total_comments`,`preferential_name`,`packing_charges`,`product_time`,`exchange_points`,`sort_number`,`create_time`,`update_time`) VALUES (103,NULL,'节拍器',NULL,NULL,'机械节拍器，速度 40-208 BPM，木质外壳。',NULL,'49.00',80,1,0,2,0,'0.00',87,87,0,NULL,'0.00','0.00',NULL,3,NULL,NULL);
INSERT INTO `tb_points_mall_goods` (`id`,`shop_id`,`name`,`main_image`,`sub_images`,`detail`,`detail_images`,`price`,`stock`,`is_hot`,`is_new`,`status`,`is_sale`,`sale_price`,`monthly_sales`,`total_sales`,`total_comments`,`preferential_name`,`packing_charges`,`product_time`,`exchange_points`,`sort_number`,`create_time`,`update_time`) VALUES (104,NULL,'吉他拨片套装',NULL,NULL,'3 片装拨片，0.73/0.88/1.0mm 三种厚度。',NULL,'25.00',500,1,0,2,0,'0.00',210,210,0,NULL,'0.00','0.00',NULL,4,NULL,NULL);
INSERT INTO `tb_points_mall_goods` (`id`,`shop_id`,`name`,`main_image`,`sub_images`,`detail`,`detail_images`,`price`,`stock`,`is_hot`,`is_new`,`status`,`is_sale`,`sale_price`,`monthly_sales`,`total_sales`,`total_comments`,`preferential_name`,`packing_charges`,`product_time`,`exchange_points`,`sort_number`,`create_time`,`update_time`) VALUES (105,NULL,'卡林巴琴 17 音',NULL,NULL,'17 音拇指琴，桃花心木琴身，附调音锤与琴包。',NULL,'59.00',60,0,1,2,0,'0.00',44,44,0,NULL,'0.00','0.00',NULL,5,NULL,NULL);
INSERT INTO `tb_points_mall_goods` (`id`,`shop_id`,`name`,`main_image`,`sub_images`,`detail`,`detail_images`,`price`,`stock`,`is_hot`,`is_new`,`status`,`is_sale`,`sale_price`,`monthly_sales`,`total_sales`,`total_comments`,`preferential_name`,`packing_charges`,`product_time`,`exchange_points`,`sort_number`,`create_time`,`update_time`) VALUES (106,NULL,'民谣吉他 40 寸',NULL,NULL,'40 寸民谣吉他，云杉面板，缺货补货中。',NULL,'399.00',0,0,0,4,0,'0.00',0,0,0,NULL,'0.00','0.00',NULL,6,NULL,NULL);
INSERT INTO `tb_points_mall_goods` (`id`,`shop_id`,`name`,`main_image`,`sub_images`,`detail`,`detail_images`,`price`,`stock`,`is_hot`,`is_new`,`status`,`is_sale`,`sale_price`,`monthly_sales`,`total_sales`,`total_comments`,`preferential_name`,`packing_charges`,`product_time`,`exchange_points`,`sort_number`,`create_time`,`update_time`) VALUES (107,NULL,'拇指琴 17 音',NULL,NULL,'17 音拇指琴，精选单板，音色清亮。',NULL,'199.00',30,0,0,2,0,'0.00',12,12,0,NULL,'0.00','0.00',NULL,7,NULL,NULL);
INSERT INTO `tb_points_mall_goods` (`id`,`shop_id`,`name`,`main_image`,`sub_images`,`detail`,`detail_images`,`price`,`stock`,`is_hot`,`is_new`,`status`,`is_sale`,`sale_price`,`monthly_sales`,`total_sales`,`total_comments`,`preferential_name`,`packing_charges`,`product_time`,`exchange_points`,`sort_number`,`create_time`,`update_time`) VALUES (108,NULL,'尤克里里琴包',NULL,NULL,'加厚防摔琴包，可背可提，适配 23/26 寸。',NULL,'29.00',120,0,0,2,0,'0.00',65,65,0,NULL,'0.00','0.00',NULL,8,NULL,NULL);
INSERT INTO `tb_points_mall_goods` (`id`,`shop_id`,`name`,`main_image`,`sub_images`,`detail`,`detail_images`,`price`,`stock`,`is_hot`,`is_new`,`status`,`is_sale`,`sale_price`,`monthly_sales`,`total_sales`,`total_comments`,`preferential_name`,`packing_charges`,`product_time`,`exchange_points`,`sort_number`,`create_time`,`update_time`) VALUES (109,NULL,'尤克里里琴弦套装',NULL,NULL,'尼龙琴弦一套，防锈耐用。',NULL,'19.00',300,0,0,2,0,'0.00',88,88,0,NULL,'0.00','0.00',NULL,9,NULL,NULL);
INSERT INTO `tb_points_mall_goods` (`id`,`shop_id`,`name`,`main_image`,`sub_images`,`detail`,`detail_images`,`price`,`stock`,`is_hot`,`is_new`,`status`,`is_sale`,`sale_price`,`monthly_sales`,`total_sales`,`total_comments`,`preferential_name`,`packing_charges`,`product_time`,`exchange_points`,`sort_number`,`create_time`,`update_time`) VALUES (110,NULL,'便携蓝牙音箱',NULL,NULL,'便携蓝牙音箱，续航 8 小时，适合小型演出。',NULL,'159.00',45,0,0,2,0,'0.00',23,23,0,NULL,'0.00','0.00',NULL,10,NULL,NULL);
INSERT INTO `tb_points_mall_goods` (`id`,`shop_id`,`name`,`main_image`,`sub_images`,`detail`,`detail_images`,`price`,`stock`,`is_hot`,`is_new`,`status`,`is_sale`,`sale_price`,`monthly_sales`,`total_sales`,`total_comments`,`preferential_name`,`packing_charges`,`product_time`,`exchange_points`,`sort_number`,`create_time`,`update_time`) VALUES (111,NULL,'乐队主题帆布袋',NULL,NULL,'乐队主题印花帆布袋，加厚棉麻。',NULL,'39.00',150,0,0,2,0,'0.00',31,31,0,NULL,'0.00','0.00',NULL,11,NULL,NULL);
INSERT INTO `tb_points_mall_goods` (`id`,`shop_id`,`name`,`main_image`,`sub_images`,`detail`,`detail_images`,`price`,`stock`,`is_hot`,`is_new`,`status`,`is_sale`,`sale_price`,`monthly_sales`,`total_sales`,`total_comments`,`preferential_name`,`packing_charges`,`product_time`,`exchange_points`,`sort_number`,`create_time`,`update_time`) VALUES (112,NULL,'民谣吉他拨片（促销）',NULL,NULL,'民谣吉他拨片，优惠专区限量促销。',NULL,'19.90',200,0,0,2,1,'12.90',66,66,0,'优惠专区','0.00','0.00',NULL,12,NULL,NULL);

DELETE FROM `tb_points_mall_menu_goods_relation` WHERE id IN (43,44,45,46,47,48,49,50,51,52,53,54,55);
INSERT INTO `tb_points_mall_menu_goods_relation` (`id`,`menu_id`,`goods_id`,`create_time`,`update_time`) VALUES (43,1,106,NULL,NULL);
INSERT INTO `tb_points_mall_menu_goods_relation` (`id`,`menu_id`,`goods_id`,`create_time`,`update_time`) VALUES (44,1,112,NULL,NULL);
INSERT INTO `tb_points_mall_menu_goods_relation` (`id`,`menu_id`,`goods_id`,`create_time`,`update_time`) VALUES (45,2,101,NULL,NULL);
INSERT INTO `tb_points_mall_menu_goods_relation` (`id`,`menu_id`,`goods_id`,`create_time`,`update_time`) VALUES (46,3,103,NULL,NULL);
INSERT INTO `tb_points_mall_menu_goods_relation` (`id`,`menu_id`,`goods_id`,`create_time`,`update_time`) VALUES (47,4,102,NULL,NULL);
INSERT INTO `tb_points_mall_menu_goods_relation` (`id`,`menu_id`,`goods_id`,`create_time`,`update_time`) VALUES (48,5,104,NULL,NULL);
INSERT INTO `tb_points_mall_menu_goods_relation` (`id`,`menu_id`,`goods_id`,`create_time`,`update_time`) VALUES (49,6,110,NULL,NULL);
INSERT INTO `tb_points_mall_menu_goods_relation` (`id`,`menu_id`,`goods_id`,`create_time`,`update_time`) VALUES (50,7,109,NULL,NULL);
INSERT INTO `tb_points_mall_menu_goods_relation` (`id`,`menu_id`,`goods_id`,`create_time`,`update_time`) VALUES (51,8,108,NULL,NULL);
INSERT INTO `tb_points_mall_menu_goods_relation` (`id`,`menu_id`,`goods_id`,`create_time`,`update_time`) VALUES (52,9,112,NULL,NULL);
INSERT INTO `tb_points_mall_menu_goods_relation` (`id`,`menu_id`,`goods_id`,`create_time`,`update_time`) VALUES (53,10,107,NULL,NULL);
INSERT INTO `tb_points_mall_menu_goods_relation` (`id`,`menu_id`,`goods_id`,`create_time`,`update_time`) VALUES (54,11,105,NULL,NULL);
INSERT INTO `tb_points_mall_menu_goods_relation` (`id`,`menu_id`,`goods_id`,`create_time`,`update_time`) VALUES (55,12,111,NULL,NULL);

DELETE FROM `tb_points_mall_goods_specification` WHERE id IN (1,2);
INSERT INTO `tb_points_mall_goods_specification` (`id`,`goods_id`,`name`,`sort_number`,`create_time`,`update_time`) VALUES (1,101,'尺寸',1,NULL,NULL);
INSERT INTO `tb_points_mall_goods_specification` (`id`,`goods_id`,`name`,`sort_number`,`create_time`,`update_time`) VALUES (2,101,'配件',2,NULL,NULL);

DELETE FROM `tb_points_mall_goods_specification_option` WHERE id IN (1,2,3,4);
INSERT INTO `tb_points_mall_goods_specification_option` (`id`,`goods_id`,`goods_specification_id`,`name`,`price`,`stock`,`sort_number`,`create_time`,`update_time`) VALUES (1,101,1,'23 寸','0.00',100,1,NULL,NULL);
INSERT INTO `tb_points_mall_goods_specification_option` (`id`,`goods_id`,`goods_specification_id`,`name`,`price`,`stock`,`sort_number`,`create_time`,`update_time`) VALUES (2,101,1,'26 寸','0.00',50,2,NULL,NULL);
INSERT INTO `tb_points_mall_goods_specification_option` (`id`,`goods_id`,`goods_specification_id`,`name`,`price`,`stock`,`sort_number`,`create_time`,`update_time`) VALUES (3,101,2,'裸琴','0.00',100,1,NULL,NULL);
INSERT INTO `tb_points_mall_goods_specification_option` (`id`,`goods_id`,`goods_specification_id`,`name`,`price`,`stock`,`sort_number`,`create_time`,`update_time`) VALUES (4,101,2,'含琴包','0.00',80,2,NULL,NULL);

DELETE FROM `tb_points_mall_full_reduction_rule` WHERE id IN (2);
INSERT INTO `tb_points_mall_full_reduction_rule` (`id`,`shop_id`,`name`,`status`,`limited_price`,`reduced_price`,`create_time`,`update_time`) VALUES (2,NULL,'满99减10',1,'99.00','10.00','2021-04-20 23:44:45','2026-09-19 05:09:56');

