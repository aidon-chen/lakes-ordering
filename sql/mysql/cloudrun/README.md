# 云托管库执行批次 6 增量 —— 操作说明

> 目的：把 `update.sql §11`（酒水商城 C-10 的自提码/核销/退款单号字段 + 乐器周边种子数据）执行到**云托管库**。
> 环境：EnvId `prod-d6gd6a2twe27f592a`、服务 `bar-mvp`、库名 `siam_db`。
> 前提：本地开发机**连不上云库内网地址**（这也是为什么需要下面两条路径之一）。

---

## 路径 A：控制台网页控制台（推荐，最快，无需开外网）

1. 打开 **微信云托管控制台** → 选择环境 `prod-d6gd6a2twe27f592a`
2. 左侧菜单进入 **「MySQL」** 页
3. 点击 **「数据库管理」** 按钮 → 页面跳转到数据库管理页并**自动带入实例信息**
4. 输入数据库密码登录 → 进入网页版 MySQL 管理台 → 找到 **SQL 执行 / 命令窗口**
5. 把 `11-酒水商城C10-增量.sql` **全文粘贴**进去执行

**两个已知坑**：
- 云 MySQL 是 **Serverless** 架构，**首次登录常报错**（实例未活跃）→ 这是正常的，**等 10~30 秒重试一次即可成功**。
- 网页控制台对**一次提交多条语句**可能有限制：若报错，就按分号**逐段执行**（本文件共 **46 条**：前 35 条是 ALTER/索引，后 11 条是数据；ALTER 段有 `SET/PREPARE/EXECUTE` 三连，需**成组执行**）。

> 本文件**全段幂等**：ALTER 走 `information_schema` 判断列/索引是否存在；种子数据先 `DELETE` 再按显式 id `INSERT`。**执行失败可直接重跑**。

---

## 路径 B：开启外网访问 + 用脚本执行（可复用，适合以后所有改表）

适合后续还会多次改表的场景（本次之后还有批次 7+）。

1. 控制台 → **MySQL** 页 → **开启「外网访问」** → 复制生成的**外网地址与端口**
2. 本机执行（脚本已就绪，含连接重试、备份、逐条执行、自动验证）：

```bash
cd /Volumes/app/project/wechat/siam-server/sql/mysql/cloudrun

# 先干跑，确认语句数（不连库）
/Users/aidon/.workbuddy/binaries/python/envs/default/bin/python apply-cloud-sql.py --dry-run

# 正式执行（--backup 会先把将被清空的 6 张种子表导出到 ./backup/）
/Users/aidon/.workbuddy/binaries/python/envs/default/bin/python apply-cloud-sql.py \
  --host <外网域名> --port <端口> --user root --password '<密码>' --db siam_db --backup
```

脚本会自动：
- **连接重试 5 次**（应对 Serverless 首次未活跃）
- `--backup` → 导出 `backup/cloud-backup-<时间戳>.sql`（回滚用）
- 逐条执行，**任一失败即整体回滚**并指出第几条
- 跑完自动验证 7 项（新列 / 唯一索引 / 分类 12 / 商品 12 / 售罄 1 / 满减名）

**安全建议**：执行完可**关闭外网访问**；若需长期开，建议新建一个**限定来源 IP** 的账号（root 账号主机设为 `%` 风险较高），不要长期用 root + 全网开放。

---

## 执行前 / 执行后

| 阶段 | 动作 |
|---|---|
| 执行前 | 确认云上没有真实商城订单（当前为演示数据，风险低）；路径 B 建议带 `--backup` |
| 执行中 | 关注是否有报错；**报错即回滚，可直接重跑** |
| 执行后 | 脚本已自动验证；另建议在小程序里走一遍「商城首页 → 加购 → 结算 → 下单」，确认下单不再因缺列失败 |
| 回滚 | 用 `backup/cloud-backup-*.sql` 恢复种子数据；新增列无需回滚（幂等段，留着无害） |

---

## 与本段无关、易混淆的两点

1. **`update.sql §4/§5` 的 `tb_mall_goods` / `tb_mall_order` / `tb_mall_order_item` 不启用**。
   2026-09-19 05:18 有一次完整的 `update.sql` 执行，**这三张表已被建出来（各 0 行）**，但 Java 侧零代码、前端零页面 → 本项目商城走的是 `tb_points_mall_*` 体系，**本增量与它们无关，也不需要删掉它们**（留着不影响，删掉也无副作用）。
2. **本增量只动 6 张表 + 2 张表加列**：
   - 加列：`tb_points_mall_order`（4 列 + 1 唯一索引）、`tb_points_mall_order_refund`（1 列 + 1 唯一索引）
   - 重灌种子：`tb_points_mall_menu` / `tb_points_mall_goods` / `tb_points_mall_menu_goods_relation` / `tb_points_mall_goods_specification` / `tb_points_mall_goods_specification_option` / `tb_points_mall_full_reduction_rule`

---

## 附：本地库已完成（无需重复）

本地 `siam_db`（127.0.0.1:3306）已于 2026-09-19 执行过同一段，**幂等复测 0 错误、验证 7/7 通过**；本地开发/联调可直接用。
