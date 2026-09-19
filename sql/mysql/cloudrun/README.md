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

