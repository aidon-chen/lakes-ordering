/**
 * 后端「订单描述」里的 HTML 实体解码。
 *
 * ⚠️ 实测坑（09-23 取证）：后端在生成 `tb_order.description` 时拼的是 HTML 实体
 * （如「布丁奶酒特调&nbsp;&nbsp;等2件」），实体作为普通字符落库。
 * 而 Vue 的 `{{ }}` 是**文本插值**、不解析实体 → 不处理就会在页面上原样看到 `&nbsp;`。
 *
 * 只做实体解码 + 折叠连续空白，**不使用 v-html**（避免 XSS）。
 * 后端已改为输出全角空格（`OrderServiceImpl` / `PointsMallOrderServiceImpl`），
 * 此函数用于兜住存量老数据的实体。
 */
export function decodeEntities(s: string | null | undefined): string {
  return String(s ?? '')
    .replace(/&nbsp;/gi, ' ')
    .replace(/&amp;/gi, '&')
    .replace(/&lt;/gi, '<')
    .replace(/&gt;/gi, '>')
    .replace(/&quot;/gi, '"')
    .replace(/&#39;/g, "'")
    .replace(/\s+/g, ' ')
}
