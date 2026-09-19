#!/usr/bin/env bash
# ============================================================
# 重新给 @wxcloud/cli 打「代码包上传 proxy:false」补丁
#
# 为什么需要它：
#   CLI 的 lib/api/files.js 里 uploadVersionPackage() 用 axios.put() 且未设 proxy，
#   而 axios 0.24 的 http 适配器在 config.proxy 为空时会【自动读 HTTPS_PROXY 环境变量】，
#   并把请求改写成「明文绝对 URI 发给代理」→ 代理侧 nginx 返回
#       400 The plain HTTP request was sent to HTTPS port
#   → CLI 表现为「✖ 代码包上传失败 Error: Request failed with status code 400」，
#     极易被误判成云托管平台故障。
#   （lib/api/base.js 早就有同样的 proxy:false 补丁，files.js 被漏掉了：
#     所以「只读命令 env:list / version:list 正常，唯独发版必失败」。）
#
# 何时需要重跑：CLI 重装 / 升级 / node_modules 被清理后（补丁在 node_modules 内，会丢）。
#
# 用法：
#   ./patch-wxcloud-cli.sh          # 打补丁（幂等，已打过会跳过）
#   ./patch-wxcloud-cli.sh --check  # 只检查当前状态，不改动
#
# 详见 skill: wxcloudrun-deploy-troubleshoot §三 坑 18
# ============================================================
set -euo pipefail

export PATH="$HOME/.npm-global/bin:$PATH"
CHECK_ONLY=false
[[ "${1:-}" == "--check" ]] && CHECK_ONLY=true

if ! command -v wxcloud &>/dev/null; then
  echo "错误：PATH 中找不到 wxcloud。先 export PATH=\"\$HOME/.npm-global/bin:\$PATH\" 或安装 @wxcloud/cli" >&2
  exit 1
fi

# 用 node 定位真正被加载的 files.js（wxcloud 常是软链，不能靠猜路径）
FILES="$(node -e '
const fs=require("fs"),path=require("path"),cp=require("child_process");
const bin=cp.execSync("command -v wxcloud").toString().trim();
let real; try { real=fs.realpathSync(bin); } catch(e){ real=bin; }
let d=path.dirname(real);
while (d!=="/" && !fs.existsSync(path.join(d,"lib","api","files.js"))) d=path.dirname(d);
const f=path.join(d,"lib","api","files.js");
if (!fs.existsSync(f)) { console.error("NOTFOUND"); process.exit(1); }
process.stdout.write(f);
' 2>/dev/null || true)"

if [[ -z "$FILES" || "$FILES" == "NOTFOUND" ]]; then
  echo "错误：未能定位 @wxcloud/cli 的 lib/api/files.js" >&2
  exit 1
fi
echo ">> 目标文件：$FILES"

if grep -q 'WXCLOUD_FORCE_PROXY' "$FILES"; then
  echo "✅ 补丁已存在，无需处理。"
  exit 0
fi

if [[ "$CHECK_ONLY" == true ]]; then
  echo "⚠️  补丁【未打】—— 发版会在「代码包上传中…」处报 400。去掉 --check 即可补上。"
  exit 1
fi

# 备份（沿用 base.js 既有补丁的命名习惯）
BAK="$FILES.bak-$(date +%Y%m%d-%H%M%S)"
cp -n "$FILES" "$BAK" && echo ">> 已备份：$BAK"

# 用 node 做精确替换，避免 sed 在多行/转义上出错
node -e '
const fs=require("fs");
const f=process.argv[1];
let s=fs.readFileSync(f,"utf8");
const oldBlock=/return await axios_1\.default\.put\(url, zipFile, \{\s*headers:\s*\{\s*"content-type":\s*"application\/zip",?\s*\},?\s*\}\);/;
const newBlock=`return await axios_1.default.put(url, zipFile, {
        headers: {
            "content-type": "application/zip",
        },
        //本地补丁（见 deploy/patch-wxcloud-cli.sh）：
        //axios 0.24 在 config.proxy 为空时会自动读 HTTPS_PROXY，把 HTTPS 请求以「明文绝对 URI」
        //发给代理 → 代理侧回 400 "The plain HTTP request was sent to HTTPS port"
        //→ CLI 报「代码包上传失败 400」。直连实测该签名地址返回 200，故固定 proxy:false。
        //需强制走代理：WXCLOUD_FORCE_PROXY=http://host:port
        proxy: process.env.WXCLOUD_FORCE_PROXY
            ? (() => {
                try {
                    const u = new URL(process.env.WXCLOUD_FORCE_PROXY);
                    return { host: u.hostname, port: Number(u.port) };
                }
                catch (e) {
                    return false;
                }
            })()
            : false,
    });`;
if(!oldBlock.test(s)){ console.error("ERR: 未匹配到 axios.put 代码块，CLI 版本可能已变更，请人工检查"); process.exit(2); }
fs.writeFileSync(f, s.replace(oldBlock, newBlock));
console.log(">> 补丁已写入");
' "$FILES"

echo ">> 语法自检："
node --check "$FILES" && echo "✅ 语法 OK —— 补丁完成。"
