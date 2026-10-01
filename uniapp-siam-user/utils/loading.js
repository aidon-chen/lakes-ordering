import { reactive } from 'vue';

// 全局「小 loading」状态：用来替代原生 uni.showLoading 的皮肤。
// 形态＝屏幕中间的小卡片（不遮挡页面），与「整页纯白遮罩」是两套独立状态，互不干扰。
// 说明：小程序 WXML 不支持 <svg> 标签，音符图形在组件里以 base64 background-image 渲染。
export const loadingState = reactive({
	visible: false,
	text: '',
	mask: false
});

let pendingCount = 0;   // 租约计数：show +1 / hide -1，归零才收起
let bailTimer = null;   // 3s 兜底
let hideTimer = null;   // 收起宽限

const HIDE_GRACE = 250; // 收起宽限：串行调用的间隙不闪（新调用会取消收起）
const BAIL_OUT = 3000;  // 3s 兜底：存量存在未配对的 hideLoading（如 apply.vue:356 为注释态），
                        // 没有兜底会让 loading 永久卡住 —— 与请求层自动配对的遮罩不同，这里必须保留。

/**
 * 显示小 loading
 * @param {string} text 文案，留空则不显示文字
 * @param {boolean} mask 是否阻断点击（对应原生 uni.showLoading 的 mask 语义）
 */
export function showLoading(text = '', mask = false) {
	// 宽限期内又有调用 → 取消收起，卡片保持连续显示
	if (hideTimer) {
		clearTimeout(hideTimer);
		hideTimer = null;
	}
	pendingCount++;
	loadingState.visible = true;
	loadingState.mask = mask;
	if (text) {
		loadingState.text = text;
	}
	// 每次 show 都续一次兜底（3s 内无 hide 则强制收起）
	if (bailTimer) {
		clearTimeout(bailTimer);
	}
	bailTimer = setTimeout(function() {
		bailTimer = null;
		pendingCount = 0;
		loadingState.visible = false;
	}, BAIL_OUT);
}

/** 隐藏小 loading */
export function hideLoading() {
	pendingCount = Math.max(0, pendingCount - 1);
	if (pendingCount > 0) {
		return; // 仍有未完成的租约，不收
	}
	if (bailTimer) {
		clearTimeout(bailTimer);
		bailTimer = null;
	}
	if (hideTimer) {
		return; // 已在收起倒计时中
	}
	hideTimer = setTimeout(function() {
		hideTimer = null;
		loadingState.visible = false;
	}, HIDE_GRACE);
}

/** 强制复位（异常兜底用） */
export function resetLoading() {
	pendingCount = 0;
	if (bailTimer) {
		clearTimeout(bailTimer);
		bailTimer = null;
	}
	if (hideTimer) {
		clearTimeout(hideTimer);
		hideTimer = null;
	}
	loadingState.visible = false;
}
