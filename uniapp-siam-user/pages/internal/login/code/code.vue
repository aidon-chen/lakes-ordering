<template>
	<!--
		E-09b 验证码登录页（品牌化重构）
		设计稿：主画布 726120934925304 · 页 2:5 · 板 5:879「E-09b 验证码登录（优化版）」
		        （原稿 726124180849143 · 页 2:53 · 板 2:380，两稿参数一致）
		  · 标题行（节点 5:882）at(45,138)：主色小图标方框 26×26 r7 + 白色酒杯 16×16，标题「验证码登录」20px 600
		  · 副标题「未注册的手机号验证后将自动创建账号」12px #999（节点 5:885 at 45,180）
		  · 输入卡 335×112 r12 at(20,216)（节点 5:886）：手机号行 55 高 / 分隔线 1px #EDEDED / 验证码行 56 高
		    重发胶囊 88×26 r13（节点 5:892），底 #E8F0FA → 绿色系浅色派生 #EEF3E4，文字 11px 主色
		  · 确定按钮 335×46 r4 实心主色 at(20,352)（节点 5:894）
		  · 底部提示 11px #B0B0B0 at y=432「收不到验证码？试试微信手机号一键登录」（节点 5:896）
		⚠️ 改动说明：
		  1) 原 van-button 上硬编码的 color="#5f7d22" 已去掉，改为全局主题类 theme-bg
		  2) 设计稿本页（5:879）**没有**协议勾选行，只在确定按钮下有底部提示（5:896 y=432）。
		     曾按「合规」补过一行协议行，实测会把底部提示下推约 21px、与设计稿不符，已按设计稿摘除。
		     合规由 E-08b（choose）承担：漏斗入口 App.vue checkIsAuth 与 mine 页都先走 choose 再进本页，
		     choose 页的协议行是必经同意点，故本页不再重复。
		  3) 发验证码按钮去掉 open-type="getUserInfo"，改为纯 @tap（原授权弹窗会拦截发送）
	-->
	<view class="code-page">
		<view class="head">
			<view class="head-icon theme-bg">
				<image class="head-icon-img" src="/static/assets/icons/login-glass-white-48.png" mode="aspectFit"></image>
			</view>
			<view class="head-title">验证码登录</view>
		</view>
		<view class="head-sub">未注册的手机号验证后将自动创建账号</view>

		<view class="input-card">
			<view class="input-row">
				<input
					class="field"
					placeholder="请输入手机号"
					placeholder-class="field-ph"
					type="number"
					maxlength="11"
					:focus="true"
					:value="phone"
					@input="phoneKey" />
			</view>
			<view class="divider"></view>
			<view class="input-row input-row-code">
				<input
					class="field"
					placeholder="请输入验证码"
					placeholder-class="field-ph"
					type="number"
					maxlength="6"
					:value="code"
					@input="codeKey" />
				<view class="resend-pill" :class="!canSend ? 'resend-pill-off' : 'theme-bg-light theme-color'" @tap="send">
					{{ time }}
				</view>
			</view>
		</view>

		<button class="confirm-btn theme-bg" :class="{ 'confirm-btn-off': !canSubmit }" @tap="loginTap">确定</button>

		<view class="bottom-hint" @tap="wxPhoneLogin">收不到验证码？试试微信手机号一键登录</view>
	</view>
</template>

<script>
	import https from '../../../../utils/http';
	import authService from '../../../../utils/auth';
	import toastService from '../../../../utils/toast.service';
	import utilHelper from '../../../../utils/util';
	import dateHelper from '../../../../utils/date-helper';

	var interval = null;
	//获取应用实例
	let app = null;

	export default {
		data() {
			return {
				time: '获取验证码',
				currentTime: 60,
				counting: false,
				inviterId: '',
				userInfo: '',
				timestamp: '',
				phone: '',
				code: ''
			};
		},
		computed: {
			//utilHelper.verifyPhone 返回匹配数组或 null，统一收敛为布尔
			phoneOk() {
				return !!utilHelper.verifyPhone(this.phone);
			},
			//手机号合法且不在倒计时中，才允许发验证码
			canSend() {
				return this.phoneOk && !this.counting;
			},
			//手机号合法 + 6 位验证码，才允许提交
			canSubmit() {
				return this.phoneOk && this.code.length === 6;
			}
		},
		onLoad(options) {
			app = getApp();
			const opts = options || {};
			if (opts.inviterId) {
				this.inviterId = opts.inviterId;
			}
			console.log('options.inviterId = ' + this.inviterId);
			if (app.globalData.userInfo) {
				this.userInfo = app.globalData.userInfo;
			} else {
				app.globalData.userInfoReadyCallback = (res) => {
					this.userInfo = res.userInfo;
				};
			}
		},
		onShow() {
			this.getTimestamp();
		},
		onHide() {
			this.clearTimer();
		},
		onUnload() {
			//离开页面时清掉倒计时，避免定时器泄漏
			this.clearTimer();
		},
		methods: {
			clearTimer() {
				if (interval) {
					clearInterval(interval);
					interval = null;
				}
			},

			getTimestamp() {
				this.timestamp = dateHelper.getTimestamp();
			},

			phoneKey(e) {
				this.phone = e.detail.value;
			},

			codeKey(e) {
				this.code = e.detail.value;
			},

			send() {
				if (this.counting) return;
				if (!this.phoneOk) {
					toastService.showToast('请输入正确的电话号码');
					return;
				}
				toastService.showLoading('正在发送...', true);
				https.request('/rest/smsLog/sendMobileCode', {
					mobile: this.phone,
					type: 'login'
				}).then((result) => {
					toastService.hideLoading();
					if (result.success) {
						toastService.showToast(result.message);
						this.startCountdown();
					}
				}).catch(() => {
					//网络层异常时也要收起 loading，否则遮罩会永久卡住
					toastService.hideLoading();
				});
			},

			startCountdown() {
				this.clearTimer();
				this.counting = true;
				this.currentTime = 60;
				this.time = '59s后重发';
				interval = setInterval(() => {
					this.currentTime -= 1;
					if (this.currentTime <= 0) {
						this.clearTimer();
						this.counting = false;
						this.currentTime = 60;
						this.time = '获取验证码';
					} else {
						this.time = this.currentTime + 's后重发';
					}
				}, 1000);
			},

			wxPhoneLogin() {
				//微信手机号快捷登录依赖小程序认证 + 付费开通「手机号快速验证」组件，暂不可用（批次4 D3）
				toastService.showToast('暂不支持微信手机号一键登录，请使用验证码登录');
			},

			loginTap() {
				if (!this.phoneOk) {
					toastService.showToast('请输入正确的电话号码');
					return;
				}
				if (!this.code || this.code.length !== 6) {
					toastService.showToast('请输入 6 位验证码');
					return;
				}
				toastService.showLoading('正在登录...', true);
				// 不再依赖 getUserInfo 的返回值：uni.getUserInfo / uni.getUserProfile 自 2022-10 起只返回匿名头像与「微信用户」昵称，
				// 旧流程里这个回调成功后再 setData 会抛错，导致 verificationLogin 永远不被调用（登录点了没反应）
				// #ifdef APP-PLUS||H5
				this.verificationLogin('');
				// #endif
				// #ifdef MP-WEIXIN||MP-ALIPAY
				uni.login({
					success: (res) => {
						this.verificationLogin(res.code);
					},
					fail: () => {
						this.verificationLogin('');
					}
				});
				// #endif
			},

			verificationLogin(code) {
				const info = this.userInfo || {};
				https.request('/rest/member/verification/login', {
					mobile: this.phone,
					mobileCode: this.code,
					headImg: info.avatarUrl,
					username: info.nickName,
					sex: info.gender,
					code: code,
					inviterId: this.inviterId ? this.inviterId : ''
				}).then((result) => {
					//无论成功失败都要收起 loading，否则失败时页面会一直卡在遮罩上
					toastService.hideLoading();
					if (result.success) {
						authService.setToken(result.data.token);
						authService.setOpenId(result.data.openId);
						authService.setPhoneNumber(this.phone);
						app.globalData.getUserInfo();
						toastService.showSuccess('登录成功');
						const timeout = setTimeout(() => {
							clearTimeout(timeout);
							//漏斗入口 authorization → choose → code 是三层，原先 navigateBack(2) 会退回到授权页形成死循环，
							//统一落到「我的」Tab，对两条入口（漏斗 / 我的页直达）都是正确落点
							uni.switchTab({
								url: '/pages/mine/index/index'
							});
						}, 1000);
					}
				}).catch(() => {
					toastService.hideLoading();
				});
			}
		}
	};
</script>

<style>
	.code-page {
		min-height: 100vh;
		background: #ffffff;
		padding: 0 40rpx;
		box-sizing: border-box;
	}

	/* ── 标题区（左对齐到 45px = 90rpx）────────────────── */
	.head {
		margin: 100rpx 0 0 50rpx;
		display: flex;
		flex-direction: row;
		align-items: center;
	}

	/* 底色来自全局 theme-bg */
	.head-icon {
		width: 52rpx;
		height: 52rpx;
		border-radius: 14rpx;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.head-icon-img {
		width: 32rpx;
		height: 32rpx;
	}

	/* 行高锁 52rpx：设计稿「标题行」高 26px 由品牌标 26×26 决定（节点 5:882），
	   标题 20px 的行盒必须 ≤26 才不会把行撑高 */
	.head-title {
		margin-left: 20rpx;
		font-size: 40rpx;
		font-weight: 600;
		color: #333333;
		line-height: 52rpx;
	}

	/* 标题行底 164 → 副文案顶 180，间距 16px = 32rpx */
	.head-sub {
		margin: 32rpx 0 0 50rpx;
		font-size: 24rpx;
		color: #999999;
		line-height: 34rpx;
	}

	/* ── 输入卡 335×112 r12 ─────────────────────────────
	   设计稿（5:886 / 2:445）输入卡为白底**无描边**、无投影，与白页面融为一体，
	   仅靠内部 1px 分割线 #EDEDED 划分两行（画板实测卡边缘像素全为 #FFFFFF）。
	   曾自加 2rpx #EDEDED 描边，与设计稿不符，已移除；
	   若希望输入区更「可见」，在此恢复 `border: 2rpx solid #ededed;` 即可（一行） */
	.input-card {
		margin-top: 38rpx;
		box-sizing: border-box;
		padding: 0 48rpx;
		border-radius: 24rpx;
		background: #ffffff;
	}

	.input-row {
		height: 110rpx;
		display: flex;
		align-items: center;
	}

	.input-row-code {
		height: 112rpx;
		justify-content: space-between;
	}

	.divider {
		height: 2rpx;
		background: #ededed;
	}

	.field {
		flex: 1;
		height: 110rpx;
		font-size: 30rpx;
		color: #333333;
	}

	.field-ph {
		color: #c0c0c0;
		font-size: 30rpx;
	}

	/* 重发胶囊 88×26 r13，浅绿派生底 */
	.resend-pill {
		flex-shrink: 0;
		width: 176rpx;
		height: 52rpx;
		line-height: 52rpx;
		border-radius: 26rpx;
		text-align: center;
		font-size: 22rpx;
		margin-left: 20rpx;
	}

	.resend-pill-off {
		background: #f5f5f5;
		color: #b0b0b0;
	}

	/* ── 确定按钮 335×46 r4 ───────────────────────────── */
	.confirm-btn {
		margin-top: 48rpx;
		width: 100%;
		height: 92rpx;
		line-height: 92rpx;
		border-radius: 8rpx;
		font-size: 30rpx;
		font-weight: 500;
		padding: 0;
	}

	.confirm-btn-off {
		opacity: 0.5;
	}

	.confirm-btn::after {
		border: none;
	}

	/* 确定按钮底 310 → 提示顶 344，间距 34px */
	.bottom-hint {
		margin-top: 68rpx;
		font-size: 22rpx;
		color: #b0b0b0;
		text-align: center;
		line-height: 32rpx;
	}
</style>
