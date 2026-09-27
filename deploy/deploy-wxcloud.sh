#!/usr/bin/env bash
# ============================================================
# siam-server · 微信云托管部署脚本（CLI 直发）
#
# 用法：
#   ./deploy-wxcloud.sh login                 # 首次微信授权登录
#   ./deploy-wxcloud.sh list                  # 查看环境与服务列表（需先登录）
#   ./deploy-wxcloud.sh --env <环境ID> --svc <服务名>
#   ./deploy-wxcloud.sh -e prod-xxx -s bar-mvp --yes   # 跳过二次确认直接发
#
# 前置：已安装 @wxcloud/cli 并已 wxcloud login 授权
# 说明：本机 CLI (v1.1.8) 无 wxcloud deploy 命令，实际发布用 run:deploy
#
# ★ 关键约束（2026-09-19 实测，踩过坑，勿改）：
#   1) run:deploy 的 PATH 只能是「裁剪后的构建上下文」CONTEXT_DIR，
#      绝不能传仓库根目录 —— CLI 打包用的 zipDir() 不读 .dockerignore/.gitignore
#      （lib/commands/run/deploy.js 调用时没传 ignore 参数），
#      否则会把 .git + vue-siam-shop(288M) + vue-siam-admin(248M) + cashier-web(112M)
#      共约 837M 全打进 zip 上传，版本会长时间卡在 creating。
#   2) --targetDir 必须显式给，否则 DeployType 会走 inquirer 交互菜单，非交互终端下卡死。
#   3) wxcloud 装在 ~/.npm-global/bin，不在默认 PATH 里，脚本内已补。
# ============================================================
set -e

export PATH="$HOME/.npm-global/bin:$PATH"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"   # 项目根目录 = siam-server
CONTEXT_DIR="$SCRIPT_DIR/context"         # 裁剪后的构建上下文（见上方约束 1）

# 默认参数（与 Dockerfile / application-mvp.yml 对齐）
ENV_ID=""
SERVICE_NAME=""
REGION="ap-shanghai"
CONTAINER_PORT=9200
REMARK=""
YES=false

parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      login) ACTION="login"; shift ;;
      list)  ACTION="list"; shift ;;
      -e|--env|--envId) ENV_ID="$2"; shift 2 ;;
      -s|--svc|--serviceName) SERVICE_NAME="$2"; shift 2 ;;
      -r|--region) REGION="$2"; shift 2 ;;
      -p|--containerPort) CONTAINER_PORT="$2"; shift 2 ;;
      -m|--remark) REMARK="$2"; shift 2 ;;
      --yes|--dry-run) YES=true; shift ;;
      -h|--help)
        echo "用法：deploy-wxcloud.sh [login|list] [-e 环境ID] [-s 服务名] [-r 地域] [-p 端口] [-m 备注] [--yes]"
        echo "  --yes   跳过二次确认并全量发布（等价于 CLI 的 --noConfirm）"
        exit 0 ;;
      *) echo "未知参数：$1"; exit 1 ;;
    esac
  done
}
parse_args "$@"

# ---------- REMARK 必填 + 自动时间前缀（2026-09-25 根治「忘传 -m → run:deploy 交互等输入 → 后台任务假死」） ----------
if [[ "$ACTION" != "login" && "$ACTION" != "list" ]]; then
  if [[ -z "$REMARK" ]]; then
    echo "错误：缺少版本备注（-m）。备注是版本列表里唯一的人类线索，缺失会导致 CLI 交互式等待输入、后台任务假死。"
    echo "用法：$0 -e <环境ID> -s <服务名> -m \"主题：改动1 + 改动2\" --yes"
    exit 1
  fi
  REMARK="[$(date '+%m-%d %H:%M')] $REMARK"
fi

if ! command -v wxcloud &>/dev/null; then
  echo "错误：未找到 @wxcloud/cli，请先执行：npm install -g @wxcloud/cli"
  exit 1
fi

cd "$PROJECT_ROOT"

case "$ACTION" in
  login)
    echo ">> 开始微信授权（会打开浏览器 / 或输出设备码）..."
    wxcloud login
    echo ">> 授权完成"
    exit 0 ;;
  list)
    echo ">> 环境列表："
    wxcloud env:list
    echo ">> 服务列表："
    wxcloud service:list --json 2>/dev/null || echo "（service:list 参数见 wxcloud service --help）"
    exit 0 ;;
esac

# ---------- 1. 同步源码 → 构建上下文（与 Dockerfile 的 COPY 清单严格一致）----------
# siam-monitor 不能少：根 pom.xml 的 <modules> 声明了它，缺了云端 Maven 会报 Child module 不存在
echo ">> 同步源码到构建上下文：$CONTEXT_DIR"
mkdir -p "$CONTEXT_DIR"
rsync -a --delete \
  --exclude 'target' --exclude '.git' --exclude '.gitignore' \
  --exclude '.DS_Store' --exclude 'node_modules' \
  "$PROJECT_ROOT/pom.xml" \
  "$PROJECT_ROOT/Dockerfile" \
  "$PROJECT_ROOT/.dockerignore" \
  "$PROJECT_ROOT/siam-common" \
  "$PROJECT_ROOT/siam-weixin" \
  "$PROJECT_ROOT/siam-system" \
  "$PROJECT_ROOT/siam-monitor" \
  "$CONTEXT_DIR"/

CTX_SIZE="$(du -sh "$CONTEXT_DIR" | cut -f1)"
CTX_FILES="$(find "$CONTEXT_DIR" -type f | wc -l | tr -d ' ')"
echo ">> 上下文体积：$CTX_SIZE / $CTX_FILES 个文件"

if [ "$CTX_FILES" -gt 5000 ]; then
  echo "错误：上下文文件数异常（>5000），疑似误把整个仓库当作上下文，已中止（见脚本头约束 1）"
  exit 1
fi

# ---------- 2. run:deploy 参数组装 ----------
DEPLOY_ARGS=("$CONTEXT_DIR")
[[ -n "$ENV_ID" ]]       && DEPLOY_ARGS+=("--envId" "$ENV_ID")
[[ -n "$SERVICE_NAME" ]] && DEPLOY_ARGS+=("--serviceName" "$SERVICE_NAME")
DEPLOY_ARGS+=("--region" "$REGION")
DEPLOY_ARGS+=("--containerPort" "$CONTAINER_PORT")
DEPLOY_ARGS+=("--targetDir" ".")          # 必须显式给，详见脚本头约束 2
DEPLOY_ARGS+=("--dockerfile" "Dockerfile")
[[ -n "$REMARK" ]] && DEPLOY_ARGS+=("--remark" "$REMARK")
if [[ "$YES" == true ]]; then
  DEPLOY_ARGS+=("--releaseType" "FULL" "--noConfirm")
fi

echo ">> 部署目录：$CONTEXT_DIR"
echo ">> 执行：wxcloud run:deploy ${DEPLOY_ARGS[*]}"
wxcloud run:deploy "${DEPLOY_ARGS[@]}"
