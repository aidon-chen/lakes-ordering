#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
在微信云托管 MySQL 上执行批次 6（酒水商城 C-10）增量脚本。

背景：云托管库是内网地址，本地开发机默认连不上 → 需先开启「外网访问」拿到公网域名，
      或用控制台自带的「数据库管理」网页控制台手工执行（见同目录 README.md）。

本脚本做的事：
  1) 连接云库（automatically 重试 —— 云 MySQL 是 Serverless，首次连接常报"未活跃"，重试即可）
  2) 可选 --backup：把本脚本将要 DELETE 的表先导出成 INSERT 到本地，便于回滚
  3) 逐条执行 SQL（默认执行 11-酒水商城C10-增量.sql），失败即回滚并指出第几条
  4) 自动跑一遍验证查询（新列是否存在、种子数据行数、唯一索引）

用法：
  python3 apply-cloud-sql.py --host <公网域名> --user root --password <密码> --db siam_db --dry-run
  python3 apply-cloud-sql.py --host <公网域名> --user root --password <密码> --db siam_db --backup
  python3 apply-cloud-sql.py --host <公网域名> --user root --password <密码> --db siam_db

注意：本文件是**运维脚本**，不参与 Spring Boot 构建；密码只在本机进程内使用，不落盘。
"""
import argparse
import datetime
import os
import re
import sys
import time

try:
    import pymysql
except ImportError:
    sys.exit("缺少 pymysql：请用 /Users/aidon/.workbuddy/binaries/python/envs/default/bin/python 运行本脚本")

HERE = os.path.dirname(os.path.abspath(__file__))
DEFAULT_SQL = os.path.join(HERE, "11-酒水商城C10-增量.sql")

# 本增量脚本会清空并在其中重灌数据的表（--backup 用）
SEED_TABLES = [
    "tb_points_mall_menu",
    "tb_points_mall_goods",
    "tb_points_mall_menu_goods_relation",
    "tb_points_mall_goods_specification",
    "tb_points_mall_goods_specification_option",
    "tb_points_mall_full_reduction_rule",
]

VERIFY_SQL = """
SELECT 'tb_points_mall_order 新增列' AS item,
       (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_points_mall_order'
           AND COLUMN_NAME IN ('pickup_code','pickup_deadline','verify_time','verifier_id')) AS n,
       4 AS expect
UNION ALL SELECT 'tb_points_mall_order_refund 新增列 refund_no',
       (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_points_mall_order_refund'
           AND COLUMN_NAME = 'refund_no'), 1
UNION ALL SELECT '唯一索引 uk_pickup_code / uk_refund_no',
       (SELECT COUNT(DISTINCT CONCAT(TABLE_NAME,'.',INDEX_NAME)) FROM information_schema.STATISTICS
         WHERE TABLE_SCHEMA = DATABASE()
           AND ((TABLE_NAME = 'tb_points_mall_order' AND INDEX_NAME = 'uk_pickup_code')
             OR (TABLE_NAME = 'tb_points_mall_order_refund' AND INDEX_NAME = 'uk_refund_no'))), 2
UNION ALL SELECT '分类数 tb_points_mall_menu',
       (SELECT COUNT(*) FROM tb_points_mall_menu), 12
UNION ALL SELECT '商品数 tb_points_mall_goods',
       (SELECT COUNT(*) FROM tb_points_mall_goods), 12
UNION ALL SELECT '售罄商品数 (status=4)',
       (SELECT COUNT(*) FROM tb_points_mall_goods WHERE status = 4), 1
UNION ALL SELECT '满减规则名=满99减10',
       (SELECT COUNT(*) FROM tb_points_mall_full_reduction_rule WHERE name = '满99减10'), 1
"""


def log(msg):
    print(msg, flush=True)


def split_statements(sql_text):
    """去掉纯注释行后按 ; 切分（本脚本的 SQL 不含字符串内分号）。"""
    lines = [l for l in sql_text.split("\n") if not l.strip().startswith("--")]
    return [s.strip() for s in " ".join(lines).split(";") if s.strip()]


def connect(args):
    """Serverless MySQL 首次连接常报未活跃 → 重试；第一次报错会自动触发实例活跃。"""
    last = None
    for attempt in range(1, 6):
        try:
            conn = pymysql.connect(host=args.host, port=args.port, user=args.user,
                                   password=args.password, database=args.db,
                                   charset="utf8mb4", autocommit=False,
                                   connect_timeout=15)
            if attempt > 1:
                log(f"  连接成功（第 {attempt} 次尝试）")
            return conn
        except Exception as e:  # noqa: BLE001
            last = e
            wait = 10
            log(f"  第 {attempt} 次连接失败：{str(e).splitlines()[0]}")
            if attempt == 1:
                log(f"  （云 MySQL 为 Serverless，首次连接会触发实例活跃）等待 {wait}s 后重试…")
            if attempt < 5:
                time.sleep(wait)
    sys.exit(f"连接失败，已重试 5 次：{last}")


def do_backup(conn, out_dir):
    os.makedirs(out_dir, exist_ok=True)
    stamp = datetime.datetime.now().strftime("%Y%m%d-%H%M%S")
    path = os.path.join(out_dir, f"cloud-backup-{stamp}.sql")
    total = 0
    with open(path, "w", encoding="utf-8") as f:
        f.write(f"-- 云库备份（执行批次6 §11 前自动导出）{datetime.datetime.now()}\n\n")
        cur = conn.cursor(pymysql.cursors.DictCursor)
        for t in SEED_TABLES:
            try:
                cur.execute(f"SELECT * FROM `{t}`")
                rows = cur.fetchall()
                if not rows:
                    continue
                cols = list(rows[0].keys())
                f.write(f"DELETE FROM `{t}` WHERE id IN ({','.join(str(r['id']) for r in rows)});\n")
                collist = "`,`".join(cols)
                for r in rows:
                    vals = []
                    for c in cols:
                        v = r[c]
                        if v is None:
                            vals.append("NULL")
                        elif isinstance(v, (int, float)):
                            vals.append(str(v))
                        elif isinstance(v, (datetime.datetime, datetime.date)):
                            vals.append(f"'{v}'")
                        else:
                            vals.append("'" + str(v).replace("\\", "\\\\").replace("'", "\\'") + "'")
                    f.write(f"INSERT INTO `{t}` (`{collist}`) VALUES ({','.join(vals)});\n")
                f.write("\n")
                total += len(rows)
                log(f"    {t:44s} {len(rows)} 行")
            except Exception as e:  # noqa: BLE001
                log(f"    {t:44s} 跳过（{str(e).splitlines()[0]}）")
    log(f"  备份已写出：{path}（共 {total} 行）")
    return path


def main():
    ap = argparse.ArgumentParser(description="在微信云托管 MySQL 上执行批次6增量脚本")
    ap.add_argument("--host", required=False, help="云托管 MySQL 公网域名（或内网地址）")
    ap.add_argument("--port", type=int, default=3306)
    ap.add_argument("--user", default="root")
    ap.add_argument("--password", default=os.environ.get("MYSQL_PASSWORD", ""))
    ap.add_argument("--db", default="siam_db")
    ap.add_argument("--file", default=DEFAULT_SQL, help="要执行的 SQL 文件")
    ap.add_argument("--dry-run", action="store_true", help="只解析并打印语句数，不连库")
    ap.add_argument("--backup", action="store_true", help="执行前备份将被清空的 6 张种子表到本地")
    args = ap.parse_args()

    log("=" * 74)
    log("批次 6 · 酒水商城 C-10 —— 云托管库增量执行")
    log("=" * 74)

    if not os.path.exists(args.file):
        sys.exit(f"SQL 文件不存在：{args.file}")
    sql_text = open(args.file, encoding="utf-8").read()
    stmts = split_statements(sql_text)
    log(f"SQL 文件：{os.path.basename(args.file)}")
    log(f"语句数  ：{len(stmts)}")

    if args.dry_run:
        log("\n--dry-run：不连接数据库。前 3 条语句预览：")
        for s in stmts[:3]:
            log("  " + " ".join(s.split())[:96])
        log("\n去掉 --dry-run 即会真正执行。")
        return

    if not args.host:
        sys.exit("缺少 --host（云库外网域名；见微信云托管控制台 → MySQL → 外网地址）")
    if not args.password:
        sys.exit("缺少密码：用 --password 或环境变量 MYSQL_PASSWORD")

    log(f"\n目标库：{args.user}@{args.host}:{args.port}/{args.db}")
    conn = connect(args)

    if args.backup:
        log("\n① 备份将被清空的种子表（本地文件）")
        do_backup(conn, os.path.join(HERE, "backup"))

    log("\n② 执行增量语句")
    cur = conn.cursor()
    ok = 0
    for n, s in enumerate(stmts, 1):
        try:
            cur.execute(s)
            ok += 1
        except Exception as e:  # noqa: BLE001
            conn.rollback()
            log(f"  [{n:02d}] ❌ {str(e).splitlines()[0]}")
            log("      语句：" + " ".join(s.split())[:140])
            sys.exit(f"\n执行中断，已回滚（成功 {ok}/{len(stmts)} 条）。请检查后重跑，本段幂等可重入。")
    conn.commit()
    log(f"  提交成功：{ok}/{len(stmts)} 条")

    log("\n③ 验证")
    cur = conn.cursor()
    cur.execute(VERIFY_SQL)
    rows = cur.fetchall()
    all_ok = True
    log(f"  {'检查项':46s} {'实际':>6s} {'期望':>6s}  结果")
    for item, n, expect in rows:
        good = (str(n) == str(expect))
        all_ok = all_ok and good
        log(f"  {item:46s} {str(n):>6s} {str(expect):>6s}  {'✅' if good else '❌'}")
    log("")
    log("✅ 全部通过，云库已具备批次 6 结构。" if all_ok else "⚠️ 有检查项不符，请逐条排查（脚本幂等，可重跑）。")
    conn.close()


if __name__ == "__main__":
    main()
