import GlobalConfig from './global-config';

// loading 租约计数。
// 微信要求 showLoading / hideLoading 成对出现，次数不匹配就会在控制台刷
// 「请注意 showLoading 与 hideLoading 必须配对使用」。
// 旧实现每次 show 都挂一个「3s 后无条件 hideLoading」的定时器，和业务代码里的
// hideLoading 互相踩踏 —— A 的定时器把 B 的 loading 关掉，B 那句 hide 就打空。
// 改为计数：show +1、hide -1，只有归零才真正调平台 API；3s 兜底也走计数。
let loadingCount = 0;
const hideLoadingCounted = function() {
	if (loadingCount <= 0) {
		return;
	}
	loadingCount--;
	if (loadingCount === 0) {
		uni.hideLoading();
	}
};

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
	showLoading: function(title = '正在加载...', mask = true) {
		if (loadingCount === 0) {
			uni.showLoading({
				title: title,
				mask: mask
			});
		}
		loadingCount++;
		//3s 兜底：调用方忘了 hideLoading 也能自己收起来（走计数，不会多减）
		setTimeout(function() {
			hideLoadingCounted();
		}, 3000);
	},
	/**
	 * 隐藏加载
	 */
	hideLoading: function() {
		hideLoadingCounted();
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