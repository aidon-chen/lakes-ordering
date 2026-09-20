#!/usr/bin/env python3
"""
云库 vs 本地库 三维度差异比对（表 / 列 / 索引）
依据 wxcloudrun-deploy-troubleshoot 技能 §三 的铁律：发版前必做且很便宜。
用法：MYSQL_PASSWORD=xxx python3 diff-cloud-local.py --host H --port P
"""
import os, sys, argparse
import pymysql

def snapshot(conn, db):
    cur = conn.cursor()
    cur.execute("""select table_name, column_name, column_type, is_nullable, column_default, column_comment
                   from information_schema.columns where table_schema=%s""", (db,))
    cols = {}
    for t, c, ty, nul, dflt, cmt in cur.fetchall():
        cols.setdefault(t, {})[c] = (ty, nul, dflt, cmt or '')
    cur.execute("""select table_name, index_name, non_unique, seq_in_index, column_name
                   from information_schema.statistics where table_schema=%s
                   order by table_name, index_name, seq_in_index""", (db,))
    idx = {}
    for t, n, nu, seq, c in cur.fetchall():
        idx.setdefault(t, {}).setdefault(n, {'unique': nu == 0, 'cols': []})['cols'].append(c)
    return cols, idx

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--host', required=True)
    ap.add_argument('--port', type=int, default=3306)
    ap.add_argument('--user', default='root')
    ap.add_argument('--db', default='siam_db')
    ap.add_argument('--local-host', default='127.0.0.1')
    ap.add_argument('--local-port', type=int, default=3306)
    ap.add_argument('--local-user', default='root')
    ap.add_argument('--local-password', default='root')
    a = ap.parse_args()

    pw = os.environ.get('MYSQL_PASSWORD')
    if not pw:
        print('缺少 MYSQL_PASSWORD 环境变量'); sys.exit(2)

    lc = pymysql.connect(host=a.local_host, port=a.local_port, user=a.local_user,
                         password=a.local_password, database=a.db, charset='utf8mb4')
    cc = pymysql.connect(host=a.host, port=a.port, user=a.user,
                         password=pw, database=a.db, charset='utf8mb4')
    lcols, lidx = snapshot(lc, a.db)
    ccols, cidx = snapshot(cc, a.db)

    print(f"=== 表数：本地 {len(lcols)} ｜ 云库 {len(ccols)} ===")
    only_local = sorted(set(lcols) - set(ccols))
    only_cloud = sorted(set(ccols) - set(lcols))
    if only_local:
        print(f"\n🔴 云库缺表（{len(only_local)}）：{only_local}")
    if only_cloud:
        print(f"\n⚠️ 仅云库有表（{len(only_cloud)}）：{only_cloud}")
    if not only_local and not only_cloud:
        print("✅ 表集合一致")

    print("\n=== 列差异（本地 vs 云库逐表）===")
    col_diffs = 0
    for t in sorted(set(lcols) & set(ccols)):
        miss = sorted(set(lcols[t]) - set(ccols[t]))
        extra = sorted(set(ccols[t]) - set(lcols[t]))
        if miss or extra:
            col_diffs += 1
            print(f"  {t}:")
            if miss:  print(f"    🔴 云库缺列({len(miss)}): {miss}")
            if extra: print(f"    ⚠️ 仅云库有({len(extra)}): {extra}")
    if not col_diffs:
        print("  ✅ 共有表的列完全一致")

    print("\n=== 索引差异 ===")
    idx_diffs = 0
    for t in sorted(set(lidx) & set(cidx)):
        for n in sorted(set(lidx[t]) | set(cidx[t])):
            L, C = lidx[t].get(n), cidx[t].get(n)
            lsig = f"{'U' if L and L['unique'] else 'N'}:{','.join(L['cols'])}" if L else '（本地无）'
            csig = f"{'U' if C and C['unique'] else 'N'}:{','.join(C['cols'])}" if C else '（云库无）'
            if lsig != csig:
                idx_diffs += 1
                print(f"  {t}.{n}: 本地[{lsig}] vs 云库[{csig}]")
    if not idx_diffs:
        print("  ✅ 共有表索引一致（含唯一性）")

    print("\n=== 结论 ===")
    total = len(only_local) + col_diffs + idx_diffs
    if total == 0:
        print("  ✅ 结构与本地一致，无需迁移")
    else:
        print(f"  🔴 存在 {total} 处差异，需迁移后再发版")
    lc.close(); cc.close()

if __name__ == '__main__':
    main()
