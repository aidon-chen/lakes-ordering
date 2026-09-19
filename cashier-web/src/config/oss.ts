/**
 * OSS 静态资源地址
 * 对齐顾客端 `uniapp-siam-user/utils/global-config.js` 的 `GlobalConfig.ossUrl`
 *
 * 背景：后端库中 `tb_goods.main_image` / `tb_merchant.head_img` / `tb_shop.shop_logo_img`
 * 等字段存的是**相对路径**（如 `data/images/admin/1/deerspot_1591847965973.jpg`），
 * 必须拼上 OSS 前缀才是可访问 URL；直接当 src 用会命中前端路由回退（返回 index.html）→ 浏览器裂图。
 */
export const OSS_URL = 'https://siam-hangzhou.oss-cn-hangzhou.aliyuncs.com/'

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
