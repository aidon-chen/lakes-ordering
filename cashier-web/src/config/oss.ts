/**
 * OSS 静态资源地址
 * 对齐顾客端 `uniapp-siam-user/utils/global-config.js` 的 `GlobalConfig.ossUrl`
 *
 * 背景：后端库中 `tb_goods.main_image` / `tb_merchant.head_img` / `tb_shop.shop_logo_img`
 * 等字段存的是**相对路径**（如 `data/images/admin/1/deerspot_1591847965973.jpg`），
 * 必须拼上 OSS 前缀才是可访问 URL；直接当 src 用会命中前端路由回退（返回 index.html）→ 浏览器裂图。
 */
/**
 * 图片前缀
 * - 本地开发（vite dev）：后端「本地存储端点」= 启动器注入的
 *   `--aliyun.oss.localStorageDir` 目录，经 context-path /siam-server 映射到 /upload/
 * - 生产构建（vite build）：阿里云 OSS 公网域名
 *
 * 两种模式同构：库里存的都是相对路径（data/images/...），只换前缀。
 * 本地目录里没有的图（存量商品图）由后端自动回源 OSS，所以本地开发也能正常显示。
 * 判定口径与 `src/api/http.ts` 的 API_BASE 一致（import.meta.env.PROD）。
 */
export const OSS_URL = import.meta.env.PROD
  ? 'https://lakes-public.oss-cn-chengdu.aliyuncs.com/'
  : 'http://localhost:9411/siam-server/upload/'

/**
 * 相对路径 → 可访问 URL
 * - 已是 http/https 绝对地址：原样返回（兼容管理端后台上传的全量地址）
 * - 空值：返回空串（调用方据此走占位图分支）
 */
export function ossUrl(path?: string | null): string {
  if (!path) return ''
  if (path.indexOf('http') === 0) return path
  return OSS_URL + path.replace(/^\/+/, '')
}
