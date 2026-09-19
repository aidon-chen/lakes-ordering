import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import { fileURLToPath, URL } from 'node:url'

// 收银端前端：Vue3 + Vite + Pinia + Tailwind
// 后端代理：本地 siam-server system-provider 端口 9200（application-local.yml）
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
  plugins: [vue(), tailwindcss()],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) }
  },
  server: {
    port: 5270,
    proxy: {
      // 后端 context-path = /siam-server，vite 的 /rest 需加前缀
      '/rest': { target: 'http://localhost:9200', changeOrigin: true, rewrite: p => '/siam-server' + p }
    }
  }
}))
