import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import { fileURLToPath, URL } from 'node:url'

// 收银端前端：Vue3 + Vite + Pinia + Tailwind
// 后端代理：本地 siam-server system-provider 端口 9200（application-local.yml）
export default defineConfig({
  // 相对路径构建：收银台部署在静态服务 /cashier/ 子目录，绝对路径 /assets 会 404
  base: './',
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
})
