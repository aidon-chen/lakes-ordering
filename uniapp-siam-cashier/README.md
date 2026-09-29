# uniapp-siam-cashier（收银端 · ⚠️ 未上线工程）

> ## ⚠️ 本工程从未上线。线上收银台不是它。
>
> - **线上 `/cashier`（云托管 bar-web）部署的是 `siam-server/cashier-web/dist`**（Vue3 版）。
>   2026-09-29 实测：线上产物与本地 `cashier-web/dist` **md5 一致**；发版脚本 `deploy-web.sh`
>   的收银端同步源即 `cashier-web/dist`。
> - **改动本工程不会随 bar-web 发版**——要「随 bar-web 上线」的收银端改动**必须落在 `cashier-web/src`**。
> - 本工程当前用途：① 本地 :5174 dev 联调（`order-project/01-solution/local-run/cashier-cli`
>   是其 CLI 化副本，`src/` 为生成物）；② 作为点单/结算逻辑的参照源码。
>
> **落码前自问**：这个改动是要上线的吗？→ 是 ⇒ 去改 `cashier-web`。
>
> 口径依据：《云托管/未发布-20260928-barweb052.md》§2.2-#5 盘点、
> `order-project/.workbuddy/memory/MEMORY-附录-A`「收银台部署源铁律」。
