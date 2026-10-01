<template>
	<loading-toast />
	<!--
		E-07b 微信授权登录页（品牌化重构）
		设计稿：主画布 726120934925304 · 页 2:5 · 板 5:737「E-07b 品牌化授权登录」
		        （原稿 726124180849143 · 页 2:53 · 板 2:378，两稿参数逐项一致，可互为校验）
		  · 状态栏 0~44 + 导航栏 44~88（白色，页面内容坐标系从 88 起）
		  · 品牌渐变区 375×280（节点 5:740，垂直居中，元素间距 14px）
		  · 音符水印 96×96 纯白不透明 绝对定位于右上 (x272,y8)（节点 5:741）
		  · 白色 Logo 方块 88×88 r20 + 主色酒杯 48×48（节点 5:746 / 5:747~750）
		  · 主标题 21px 600 白 + 副标题 13px 白 100%（节点 5:751 / 5:752）
		  · 白色卡片 335×216 r12 at (20,344)，内边距 26/24，间距 8px（节点 5:753）
		  · 协议勾选行 at (70,596)（节点 5:760）→ 抽成公共组件 login-agreement
		配色：设计稿蓝 #004CA0 → 主题绿 #5f7d22（渐变暗端 #003A7A → #485F1A，同比例换算）
		单位：设计稿 375 宽，1px = 2rpx
	-->
	<view class="auth-page">
		<view class="brand-hero">
			<image class="hero-note" src="/static/assets/icons/login-note-white.png" mode="aspectFit"></image>
			<view class="hero-logo">
				<image class="hero-logo-icon" src="/static/assets/icons/login-glass-green.png" mode="aspectFit"></image>
			</view>
			<view class="hero-title">{{ brandName }}</view>
			<view class="hero-slogan">{{ slogan }}</view>
		</view>

		<view class="login-card">
			<view class="card-title">登录湖里Lakes，开启你的深夜现场</view>
			<view class="card-sub">首次登录将自动创建会员账号</view>
			<button class="card-btn theme-bg" @tap="wxAuthLogin">微信授权登录</button>
			<button class="card-btn card-btn-ghost theme-color theme-border-color" @tap="phoneLogin">手机号验证码登录</button>
		</view>

		<view class="agreement-wrap">
			<login-agreement v-model="agreed" />
		</view>
	</view>
</template>

<script>
	import toastService from '../../../../utils/toast.service';
	// .vue 组件必须在脚本里 import + components 注册（本工程 easycom.autoscan=false，
	// 且 pages.json 的 usingComponents 只对 wxcomponents 原生组件生效，不会打包 .vue）
	import loginAgreement from '@/components/login-agreement/login-agreement.vue';

	//获取应用实例
	let app = null;

	export default {
		components: {
			'login-agreement': loginAgreement
		},
		data() {
			return {
				brandName: '湖里Lakes',
				slogan: '一杯敬深夜 · 一场敬热爱',
				agreed: true
			};
		},
		onLoad(options) {
			app = getApp();
		},
		methods: {
			//未勾选协议时统一拦截
			checkAgreed() {
				if (this.agreed) return true;
				toastService.showToast('请先阅读并同意《用户协议》和《隐私政策》');
				return false;
			},

			goChoose() {
				uni.redirectTo({
					url: '../choose/choose'
				});
			},

			wxAuthLogin() {
				if (!this.checkAgreed()) return;
				// uni.getUserProfile 自 2022-10 起已废弃（只返回匿名头像与「微信用户」昵称），
				// 这里保留调用仅为呈现微信授权弹窗这一「视觉门面」；
				// 无论成功、失败还是直接抛错，都必须放行到下一页，否则登录漏斗会卡死在本页。
				try {
					uni.getUserProfile({
						desc: '用于完善会员资料',
						success: (res) => {
							app.globalData.userInfo = (res && res.userInfo) ? res.userInfo : null;
							this.goChoose();
						},
						fail: () => {
							app.globalData.userInfo = null;
							this.goChoose();
						}
					});
				} catch (err) {
					console.warn('getUserProfile 不可用，直接进入下一步', err);
					app.globalData.userInfo = null;
					this.goChoose();
				}
			},

			phoneLogin() {
				if (!this.checkAgreed()) return;
				uni.navigateTo({
					url: '../code/code'
				});
			}
		}
	};
</script>

<style>
	.auth-page {
		min-height: 100vh;
		background: #ffffff;
	}

	/* ── 品牌渐变区 375×280 → 750×560rpx ─────────────────
	   角度与止点由设计稿画板实测反解（不是 135° 铺满整条对角线）：
	   设计稿渐变 transform [[1,1,0],[-1,1,0.5]]，实测亮度剖面拟合出
	     方向 ≈143°（CSS 角），暗端在渐变线 50% 处即到位。
	   换言之：亮端只占左上角一小片扇区，右下大半区已是暗色。
	   若写成 135deg 0%→100%，暗色只出现在最右下角，与设计稿明显不一致。 */
	.brand-hero {
		position: relative;
		height: 560rpx;
		background: linear-gradient(143deg, #5f7d22 0%, #485f1a 50%);
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		overflow: hidden;
	}

	/* 音符水印：96×96 白色，**不透明**（设计稿 5:741 / 2:384 的矢量填充为白色 opacity 1，
	   画板像素实测最亮值 rgb(255,255,255)，确认无透明度），右上角 */
	.hero-note {
		position: absolute;
		top: 16rpx;
		right: 14rpx;
		width: 192rpx;
		height: 192rpx;
	}

	.hero-logo {
		width: 176rpx;
		height: 176rpx;
		border-radius: 40rpx;
		background: #ffffff;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.hero-logo-icon {
		width: 96rpx;
		height: 96rpx;
	}

	.hero-title {
		margin-top: 28rpx;
		font-size: 42rpx;
		font-weight: 600;
		color: #ffffff;
		line-height: 60rpx;
	}

	.hero-slogan {
		margin-top: 28rpx;
		font-size: 26rpx;
		color: #ffffff;
		line-height: 38rpx;
	}

	/* ── 白色登录卡 335×216 r12，上移 24px 压住渐变 ──────
	   高度必须锁定 432rpx：卡内实际内容只有 204px，设计稿却是 216px（Figma 固定高、
	   内容 MIN 对齐，底部留 12px 空白）。不锁高会让协议行整体上移 12px。 */
	.login-card {
		position: relative;
		width: 670rpx;
		height: 432rpx;
		margin: -48rpx auto 0 auto;
		box-sizing: border-box;
		padding: 52rpx 48rpx;
		border-radius: 24rpx;
		background: #ffffff;
		display: flex;
		flex-direction: column;
	}

	/* 行盒严格对齐设计稿：标题 16px→行盒 24（370..394）、副标题 12px→行盒 16（402..418），
	   中间 8px 间距；三者相加 24+8+16+8=56，故主按钮顶 = 卡顶 344+26+56 = 426（与画板实测一致） */
	.card-title {
		font-size: 32rpx;
		font-weight: 600;
		color: #333333;
		line-height: 48rpx;
	}

	.card-sub {
		margin-top: 16rpx;
		font-size: 24rpx;
		color: #999999;
		line-height: 32rpx;
	}

	.card-btn {
		margin-top: 16rpx;
		width: 100%;
		height: 88rpx;
		line-height: 88rpx;
		border-radius: 8rpx;
		font-size: 30rpx;
		font-weight: 500;
		padding: 0;
	}

	/* 首个按钮与副标题的间距按设计稿 16rpx，其后按钮同间距 */
	.card-sub + .card-btn {
		margin-top: 16rpx;
	}

	.card-btn-ghost {
		background: #ffffff;
		/* 颜色由全局 theme-border-color 提供，此处只给宽度与线型 */
		border-width: 2rpx;
		border-style: solid;
	}

	.card-btn::after {
		border: none;
	}

	/* ── 协议勾选行 ─────────────────────────────────────── */
	.agreement-wrap {
		margin-top: 72rpx;
		display: flex;
		justify-content: center;
	}
</style>
