#!/usr/bin/env bash
# ============================================================
# 云托管库 · 增量 SQL 交互式执行器
#
# 用法：
#   ./apply-to-cloud-db.sh <sql文件>            # 交互式询问连接信息
#   ./apply-to-cloud-db.sh 16-点单酒水菜单-种子数据.sql
#
# 前置：微信云托管控制台 → 环境 → MySQL → 开启「外网访问」，拿到外网域名与端口。
# 说明：密码用 read -s 读取，不回显、不落盘、不写入任何文件；
#       实际执行交给同目录的 apply-cloud-sql.py（自带重试 / 备份 / 验证）。
# ============================================================
set -e

export PATH="$HOME/.npm-global/bin:$PATH"
PY=/Users/aidon/.workbuddy/binaries/python/envs/default/bin/python
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

SQL_FILE="${1:-}"
if [[ -z "$SQL_FILE" ]]; then
  echo "用法：$0 <sql文件>"
  echo "可选文件："
  ls -1 "$DIR"/*.sql 2>/dev/null | xargs -n1 basename | sed 's/^/  /'
  exit 1
fi
[[ -f "$DIR/$SQL_FILE" ]] && SQL_FILE="$DIR/$SQL_FILE"
[[ -f "$SQL_FILE" ]] || { echo "错误：找不到 SQL 文件：$SQL_FILE"; exit 1; }

echo "─────────────────────────────────────────────"
echo "目标：云托管库（外网通道）"
echo "脚本：$(basename "$SQL_FILE")"
echo "语句：$(grep -vcE '^\s*(--.*)?$' "$SQL_FILE") 行可执行"
echo "─────────────────────────────────────────────"

read -r -p "外网域名（如 xxxxx.sh.cdb.tencentcloudcr.com）: " HOST
read -r -p "端口 [默认 3306]: " PORT
PORT="${PORT:-3306}"
read -r -p "用户名 [默认 root]: " DBUSER
DBUSER="${DBUSER:-root}"
read -r -p "库名 [默认 siam_db]: " DBNAME
DBNAME="${DBNAME:-siam_db}"
read -r -s -p "密码: " PW; echo

echo ">> 先探测连通性..."
MYSQL_PASSWORD="$PW" "$PY" - "$HOST" "$PORT" "$DBUSER" "$DBNAME" <<'PYEOF'
import sys, pymysql
host, port, user, db = sys.argv[1], int(sys.argv[2]), sys.argv[3], sys.argv[4]
import os
try:
    c = pymysql.connect(host=host, port=port, user=user, password=os.environ["MYSQL_PASSWORD"],
                        database=db, charset="utf8mb4", connect_timeout=15)
except Exception as e:
    print("连接失败：", e); sys.exit(1)
cur = c.cursor()
cur.execute("select count(*) from information_schema.tables where table_schema=%s", (db,))
print("连接成功；表数：", cur.fetchone()[0])
cur.execute("select count(*) from tb_goods where shop_id=13")
print("shop_id=13 商品数（改前）：", cur.fetchone()[0])
PYEOF

echo
read -r -p "确认对该云库执行 ${SQL_FILE##*/}？(yes/NO) " OK
[[ "$OK" == "yes" ]] || { echo "已取消，未做任何改动。"; exit 0; }

MYSQL_PASSWORD="$PW" "$PY" "$DIR/apply-cloud-sql.py" \
  --host "$HOST" --port "$PORT" --user "$DBUSER" --db "$DBNAME" \
  --file "$SQL_FILE" --backup

unset PW MYSQL_PASSWORD
echo
echo ">> 完成。建议：去控制台复核结果后，关闭云库「外网访问」。"
