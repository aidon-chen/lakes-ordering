import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import basicSsl from '@vitejs/plugin-basic-ssl'
import { fileURLToPath, URL } from 'node:url'

/*
 * 收银端前端：Vue3 + Vite + Pinia + Tailwind
 * 后端代理：本地 siam-server system-provider 端口 9411（由 local-run 的 env.sh PORT_BACKEND 注入）
 *
 * ★ 平板扫码（2026-09-24）：调摄像头有两个硬前提
 *   1. server.host 必须开 —— 否则 dev server 只监听 localhost，平板按 IP 根本进不来
 *   2. 必须 https —— 摄像头属「安全上下文」能力，http://<局域网IP> 下浏览器把
 *      navigator.mediaDevices 直接设为 undefined，代码怎么写都调不起来
 *      （localhost 是例外，所以桌面调试不受影响）
 *   → 桌面调试：npm run dev                （http，无证书警告）
 *   → 平板调试：CASHIER_HTTPS=1 npm run dev，浏览器访问 https://<本机IP>:5270
 *     首次会提示「证书不受信任」→ 选「继续访问/高级 → 继续」即可（自签证书，仅本地用）
 */
const useHttps = process.env.CASHIER_HTTPS === '1'

export default defineConfig(({ mode }) => ({
  /*
   * base 必须是「带子路径的绝对路径」，三种取值实测：
   *   - './'  → 深链（如 /cashier/order/goods-status）下相对资源解析成
   *             /cashier/order/assets/xxx，命中 nginx 的 SPA 回退返回 text/html
   *             → 浏览器拒绝当模块执行 → **白屏**（2026-09-19 线上实测）
   *   - '/'   → 资源指向 /assets/xxx，在 /cashier/ 子目录下 404
   *   - '/cashier/' → 正确 ✅（开发环境仍用 '/'，保持 localhost:5270 直接可访问）
   */
  base: mode === 'production' ? '/cashier/' : '/',
  plugins: [vue(), tailwindcss(), ...(useHttps ? [basicSsl()] : [])],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) }
  },
  server: {
    host: true, // 监听 0.0.0.0：平板/手机同网段可按 <本机IP>:5270 访问
    port: 5270,
    proxy: {
      // 后端 context-path = /siam-server，vite 的 /rest 需加前缀
      '/rest': { target: 'https://bar-mvp-315579-12-1488393615.sh.run.tcloudbase.com', changeOrigin: true, rewrite: p => '/siam-server' + p }
    }
  }
}))
