import GlobalConfig from './global-config';
import { showLoading as globalShowLoading, hideLoading as globalHideLoading } from './loading.js';

// loading 已改为「品牌小卡片」形态（见 components/loading-toast/loading-toast.vue），
// 不再调用原生 uni.showLoading —— 全站约 100 处 toastService.showLoading() 调用点零改动、自动升级。
// 租约计数与 3s 兜底下沉到 utils/loading.js：
// 兜底必须保留，存量存在未配对的 hideLoading（如 order/refund/apply/apply.vue:356 为注释态），
// 没有兜底会让 loading 永久卡住。

export default {
	showToast: function(title = '', duration) {
		uni.showToast({
			title: title,
			icon: 'none',
			duration: duration ? duration : 3000
		});
	},
	/**
	 * 成功
	 */
	showSuccess: function(title, mask = true, duration = 3000) {
		uni.showToast({
			title: title,
			mask: mask,
			image: '/static/assets/images/success.png',
			duration: duration
		});
	},
	/**
	 * 警告
	 */
	showWarning: function(title, duration = 3000) {
		uni.showToast({
			title: title,
			image: '/static/assets/images/warning.png',
			duration: duration
		});
	},
	/**
	 * 错误
	 */
	showError: function(title, mask = false, duration = 3000) {
		uni.showToast({
			title: title,
			mask: mask,
			image: '/static/assets/images/error.png',
			duration: duration
		});
	},
	/**
	 * 显示加载
	 */
	showLoading: function(title = '正在加载……', mask = true) {
		globalShowLoading(title, mask);
	},
	/**
	 * 隐藏加载
	 */
	hideLoading: function() {
		globalHideLoading();
	},
	/**
	 * 显示模态窗口
	 */
	showModal: function(title = null, content = null, confirm = null, cancel = null, showCancel = true) {
		uni.showModal({
			title: title ? title : '温馨提示',
			content: content || '',
			showCancel: showCancel,
			success(res) {
				if (res.confirm) {
					//console.log('用户点击确定');
					if (confirm) {
						confirm();
					}
				} else if (res.cancel) {
					//console.log('用户点击取消');
					if (cancel) {
						cancel();
					}
				}
			}
		});
	}
};