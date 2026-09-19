# 云托管库执行批次 6 增量 —— 操作说明

> 目的：把商城 C-10 的自提码/核销/退款单号字段与种子数据，以及批次 5（公告/发票/存酒）+ 台位作业的缺失表，执行到**云托管库**。
> 环境：EnvId `prod-d6gd6a2twe27f592a`、服务 `bar-mvp`、库名 `siam_db`。
> 前提：本地开发机**连不上云库内网地址**，需按下面两条路径之一（或已开启外网时直接用脚本）。

---

## 0. 脚本清单与执行顺序（★ 顺序不能反）

| 顺序 | 文件 | 作用 | 幂等 |
|---|---|---|---|
| 1 | `11-酒水商城C10-增量.sql` | **商城 C-10 结构与种子数据**：`tb_points_mall_order` 补 4 列 + 唯一索引、`tb_points_mall_order_refund` 补 `refund_no`；分类/商品/规格/满减**重灌为乐器周边** | ✅ 全幂等 |
| 2 | `13-结构对齐-缺失列.sql` | **存量表补列 + 补索引**：3 张表共 15 列（`tb_order` 缺 `merchant_id`/`order_channel` 等 6 列、`tb_dining_table` 6 列、`tb_advertisement` 3 列）+ `tb_dining_table` 唯一键 `uk_shop_table_no` | ✅ 逐列/逐索引判断 |
| 3 | `12-批次5与台位-补表.sql` | **补 7 张缺失表**（`tb_notice`/`tb_invoice_apply`/`tb_wine_storage(+record)`/`tb_dining_session`/`tb_table_zone`/`tb_table_type`）+ 台位测试数据（区域 3 / 类型 3 / 桌台 19，`shop_id=13`） | ✅ 建表 `IF NOT EXISTS`、数据先清后插 |
| 4 | `13-批次5结构对齐-uk_order与location.sql` | **批次 5 两处结构细节**：`tb_invoice_apply` 的 `idx_order`(普通) → `uk_order`(唯一)；`tb_wine_storage` 补 `location`(存放柜位)；顺带 `tb_order_detail.remark` | ✅ 全幂等（含查重守卫） |
| 5 | `14-批次5联调种子数据.sql` | **联调灌数**：公告 4 条（含 1 条下线用于验证过滤）+ 存酒 3 条（存续中/将过期/已过期三态）+ 可开票订单 2 张（`status=6 & is_invoice=0`，¥94.00 对齐画板 C-15 / ¥186.00）+ 明细 5 行 | ✅ 先 DELETE 再按显式 id INSERT |
| 6 | `16-点单酒水菜单-种子数据.sql` | **点单菜单酒水化**：`shop_id=13` 的 9 个分类 / 40 个商品由鹿点奶茶 demo 残留改为音乐酒水主题；规格项与选项同步改名 | ✅ 全 UPDATE，可重复执行 |

> **`12-` 已修掉两处旧结构 DDL**（2026-09-19 10:52）：`tb_invoice_apply` 建表改为 `UNIQUE KEY uk_order`、`tb_wine_storage` 建表内补 `location` 列。
> 在此之前这两处要靠第 4 步的 `13-批次5结构对齐` 兜底；现在**新建库一次即正确**（第 4 步仍保留，用于已建库的补齐与复跑校验）。

> **为什么是这个顺序**
> - `13-`（补列）必须在 `12-`（建表+台位数据）**之前**：台位测试数据含 `type_id`/`zone_id`，列没补上会报 `Unknown column`。
> - `13-批次5结构对齐` 必须在 `12-` **之后**：它要改的 `tb_invoice_apply`/`tb_wine_storage` 正是 `12-` 建出来的表。
> - `14-`（灌数）最后跑：公告/存酒的**业务数据**依赖表已建好；订单灌数让 C-15 发票页有可选订单。
> - **本地库同样需要跑这两份 13- 与 14-**（本地也缺 `uk_order`/`location`，且联调数据两库都要有）。
> `tb_mall_goods` / `tb_mall_order` / `tb_mall_order_item`（update.sql §4/§5）按裁决**不启用**，各脚本都不涉及。

执行脚本（自动重试 / 备份 / 失败回滚 / 验证）：

```bash
cd /Volumes/app/project/wechat/siam-server/sql/mysql/cloudrun
P=/Users/aidon/.workbuddy/binaries/python/envs/default/bin/python
export MYSQL_PASSWORD='<密码>'        # 不落盘，仅本次进程内使用
H=<外网域名>; PORT=<端口>

$P apply-cloud-sql.py --host $H --port $PORT --user root --db siam_db --file 11-酒水商城C10-增量.sql --backup
$P apply-cloud-sql.py --host $H --port $PORT --user root --db siam_db --file 13-结构对齐-缺失列.sql
$P apply-cloud-sql.py --host $H --port $PORT --user root --db siam_db --file 12-批次5与台位-补表.sql
$P apply-cloud-sql.py --host $H --port $PORT --user root --db siam_db --file 13-批次5结构对齐-uk_order与location.sql
```

---

## 1. 执行记录（2026-09-19 已实际执行 ✅）

| 脚本 | 结果 | 备注 |
|---|---|---|
| `11-` | **46/46 条提交**，验证 7/7 全绿 | 执行前备份原美妆 demo 数据 → `backup/cloud-backup-20260919-102514.sql` |
| `13-缺失列` | **81/81 条提交**（15 列 + 1 索引） | 15 列补齐 + `uk_shop_table_no` |
| `12-补表` | **35/35 条提交** | 7 张表 + 台位测试数据；执行前备份 → `backup/cloud-prebackup-20260919-102657.sql` |
| `13-批次5结构` | **18/18 条提交** | `uk_order`(唯一) + `location` 列（云库与**本地库**各执行一次） |

**最终复核（云库 vs 本地）**：

| 维度 | 结果 |
|---|---|
| 表 | 本地 84 / 云库 81，差异**仅 `tb_mall_*` 三张**（按裁决不启用） |
| 列 | **差异 0 ✅** |
| 索引 | **差异 0 ✅** |
| 关键项 | `tb_invoice_apply.uk_order` ✅ ｜ `tb_wine_storage.location` ✅ ｜ `tb_order.merchant_id/order_channel` ✅ ｜ `tb_points_mall_order.pickup_code` ✅ ｜ 台位 19 桌 / 3 区域 / 3 类型 ✅ ｜ 商城商品 12 条（乐器周边）✅ |

> 期间踩到并修掉的两个坑（写进脚本了）：① 幂等 ALTER 里 `COMMENT`/`DEFAULT` 的**单引号必须双写转义**（`''`），否则 `PREPARE` 报语法错；② 待补列要**按本地实际列顺序**排列，否则 `AFTER` 会指向尚未创建的列。

---

## 1.1 点单菜单酒水化（`16-`，2026-09-19）

**动因**：顾客端点单页读的是 `tb_menu`(分类) + `tb_goods`(商品) + `tb_menu_goods_relation` + 规格两表；演示店 `shopId=13` 里塞的还是原鹿点奶茶 demo 残留（红豆奶茶、椰果奶茶、芝士桃桃……），与「音乐酒水」主题不符，且混着一条 `test-a`（¥10000）。

**做法原则 —— 原地 UPDATE、商品 id 不动**：

- `tb_goods` 40 行的 id 全部保留 → 历史订单（`tb_order_detail` 存名称快照，不受影响）、购物车、收藏、`tb_coupons_goods_relation`（195 行）全部安全；
- 图片沿用各商品原有的 OSS 图（`siam-hangzhou` 桶只有读权限、无 AccessKey 不能上传，桶内 533 张全是鹿点奶茶/果汁照）→ **按图片观感反推酒名**，不引入新图床依赖；
- 分类 9 个沿用原 id，只改名与 `sort_number`；
- `status` 语义：`1=待上架 2=已上架 3=已下架 4=售罄`；顾客端 `MenuServiceImpl` 只取 `status ∈ (2,4)`，所以下架项不会出现在点单页。

**改造后结构**（9 分类 / 40 商品 / 1 售罄 / 价格区间 ¥32–128）：

| 排序 | 分类 | 条数 |
|---|---|---|
| 1 | 本店招牌 | 4 |
| 2 | 精酿啤酒 | 4（含 1 售罄「季节限定·南瓜艾尔」） |
| 3 | 经典鸡尾酒 | 6 |
| 4 | 威士忌·洋酒 | 3 |
| 5 | 葡萄酒 | 4 |
| 6 | 气泡特调 | 4 |
| 7 | 无酒精特调 | 4 |
| 8 | 微醺奶茶·甜品酒 | 7 |
| 9 | 本周新酒 | 4 |

**规格改造**：规格项 `规格→杯型`、`温度→冰量`、`添加→加料`、`糖度→甜度`；选项 `中杯→标准杯`、`冰→加冰`、`热→热饮`、`正常糖→标准糖`、`珍珠→加柠檬片`、`红豆→加薄荷叶`、`椰果→加冰块`、`花生→加苏打水`、`葡萄→加青柠角`、`布丁→加糖浆`、`芒果→加果汁`、`奶盖→加奶霜`（加料类同时写入价差 ¥0–3）。

**回滚**：`sql/mysql/archive/shop13-点单菜单-奶茶demo-备份-20260919.sql`（含 9 分类 / 40 商品 / 40 关联 / 106 规格 / 165 选项，先 DELETE 再 INSERT）。

**执行记录**：

| 库 | 结果 | 时间 |
|---|---|---|
| 本地 `siam_db` | **129/129 条提交**，复核 9 分类 / 40 商品 / 售罄 1 条 ✅ | 2026-09-19 14:5x |
| 云托管库 `sh-cynosdbmysql-grp-80tj5ocw.sql.tencentcdb.com:25613` | **129/129 条提交**，9/9 项复核全绿 ✅；**云端接口 `POST /rest/menu/listWithGoods {"shopId":13}` 已返回酒水菜单**（9 分类 / 40 商品 / 1 售罄 / `sortNumber` 1–9 / `packingCharges` 0） | 2026-09-19 15:30 |

云库执行走**外网访问 + 脚本**（即第 3 节路径 B）：

```bash
export MYSQL_PASSWORD='<密码>'
$P apply-cloud-sql.py --host sh-cynosdbmysql-grp-80tj5ocw.sql.tencentcdb.com --port 25613 \
   --user root --db siam_db --file 16-点单酒水菜单-种子数据.sql --backup
```

也可用交互版（密码 `read -s` 不回显、不落盘，执行前自动探测+二次确认）：

```bash
cd sql/mysql/cloudrun && ./apply-to-cloud-db.sh 16-点单酒水菜单-种子数据.sql
```

> ⚠️ **`apply-cloud-sql.py` 的自动备份只覆盖商城 6 张 `tb_points_mall_*` 表（写死），不含菜单表**。
> 菜单的回滚文件是 `archive/shop13-点单菜单-奶茶demo-备份-20260919.sql`，已逐值核对与云库改前状态一致（如 goods 76 → 14.00/stock 2、82 → 16.00/stock 7、261 → 10000.00/stock 0）。
> **用完记得关闭云库「外网访问」。**

---

## 1.2 单店模式：其余门店下架（`18-`，2026-09-19）

**为什么需要**：顾客端点单页会**自动选店**，不是固定 13 号店 ——

```
pages/menu/index/index.vue:911  getShopList()
  → POST /rest/shop/list { pageNo:-1, pageSize:1, position: 定位 }
  → result.data.records[0]            ← 取排序第一的店
  → getMenuList(该店 id)
```

后端 `ShopMapper.xml:67` 的取数是 `where status = 2 and distance <= 配送距离上限 order by distance asc`。
原库 10 家店 `status` 全是 2，坐标又全挤在长沙一带（112.88, 28.23），**导致鹿点(13) 排到第 4 位，自动选店永远轮不到它** → 点单页显示的是「小气茶 / 满杯烧仙草 / 橘小胖」这些原模板残留店的奶茶菜单。

**做法**：把除鹿点外的门店 `status` 置 3（已下架），顾客端即只剩一家。

```sql
UPDATE `tb_shop` SET `status`=3, `update_time`=NOW() WHERE `id` <> 13 AND `status` = 2;   -- 影响 9 行
```

**为什么不用 DELETE**：这些店名下有 **195 个商品**与历史订单（`tb_goods` / `tb_order` / `tb_dining_table` 等），物理删除会留下孤儿数据、并可能让商家端/管理端查询报错。改 `status` 零副作用、可一键恢复（`archive/tb_shop-单店模式前-备份-20260919.sql`）。

**验证（实测，云端）**：

| 调用 | 结果 |
|---|---|
| `/rest/shop/list` pageSize=1（杭州 / 北京 / 长沙 三种定位） | 均返回 **(13, 鹿点(辰泰园区体验店))** ✅ |
| `/rest/shop/list` pageSize=10 | 只有 1 家 ✅ |
| `/rest/shop/listForPickup`（首页用） | 只有 1 家 ✅ |
| `/rest/menu/listWithGoods {shopId:13}` | 9 分类 / 40 商品，全部酒水 ✅ |

**双库均已执行**：本地 `siam_db` 与云托管库各影响 9 行，`tb_shop` 现状 `status=2` 仅 1 家（鹿点）。

---

## 2. 路径 A：控制台网页控制台（不装客户端也能执行）

1. 微信云托管控制台 → 环境 `prod-d6gd6a2twe27f592a` → 左侧 **「MySQL」** → 点 **「数据库管理」**（自动带入实例信息）
2. 输密码登录网页版管理台 → SQL 窗口 → **按上表顺序**依次粘贴三份 SQL 执行

**两个已知坑**：云 MySQL 是 Serverless，**首次登录常报"实例未活跃"，等 10~30s 重试即可**；一次提交多条语句若被拒，就按分号逐段执行（`ALTER` 的 `SET/PREPARE/EXECUTE` 需成组）。

## 3. 路径 B：开启外网访问（本次即走此路径）

控制台 MySQL 页 → 开启 **「外网访问」** → 复制外网域名与端口 → 用上面第 0 节的命令执行。

**安全建议**：执行完可**关闭外网访问**；若要长期开，建议新建**限定来源 IP** 的账号（不要长期用 root + 全网开放）。

---

## 4. 回滚

| 场景 | 方式 |
|---|---|
| 商城种子数据被误改 | 执行 `backup/cloud-backup-*.sql`（含原美妆 demo 数据） |
| 台位/门店/广告数据被误改 | 执行 `backup/cloud-prebackup-*.sql` |
| 新增列/表 | 无需回滚（幂等段，留着不影响业务） |

## 5. 后续上线建议

云库是**早于本地的快照**，曾同时缺表（10 张）与缺列（15 列）。**以后每次上线前都应先做一次"表 + 列"全量差异比对**，只跑增量脚本容易只解决一半。比对方法见技能 `siam-server-add-db-field` 的「云端结构对齐」一节。

