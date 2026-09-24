/**
 * 实名 / 身份认证 · 敏感个人信息处理告知 —— 面向用户的数值单一源
 *
 * 为什么单独抽一个文件：
 *   身份证件照片的「保存期限」同时出现在两处面向用户的文本里 ——
 *     ① pages/internal/agreement/privacy.vue 第 4.5 条（隐私政策）
 *     ② pages/mine/identityAuth/realname.vue 《实名认证个人信息处理告知书》弹窗
 *   两处各写死一个数字，极易改一处漏一处，直接重演「告知与实际不一致 / 告知与告知
 *   不一致」的缺陷（本项目 2026-09-23 已在 privacy.vue 第 1.5 条踩过一次）。
 *   故此处为唯一来源，两处一律引用，不再出现字面量。
 *
 * 2026-09-23 用户拍板：证件图在**审核结论作出后保留 30 天**，期满自动删除。
 *
 * ⚠️ 与后端的耦合（跨语言，无法共享常量，改一处必须改另一处）：
 *   后端实际执行删除的天数在 `application-mvp.yml` 的 `siam.identityAuth.keepDays`
 *   （见 MemberIdentityAuthServiceImpl.cleanExpiredImages）。本文件只是**告知文案**用的值。
 *   两边不一致 = 政策承诺与实际行为不符，改动时务必同步。
 *
 * 实现方式（「审核即弃」）：mvp profile 下 @Scheduled 全域不启动，故不用定时任务，
 *   改为「审核出结论时」+「打开审核列表时」顺带清理（带节流），见
 *   MemberIdentityAuthServiceImpl#cleanExpiredImagesThrottled。
 */
const IDENTITY_KEEP_DAYS = 30;

export default {
	IDENTITY_KEEP_DAYS
};
