<template>
	<!--
		E-08b 登录方式选择页（品牌化重构）
		设计稿：文件 726120934925304 · 页 2:5 · 板 5:769「E-08b 欢迎登录优化」
		  · 头像占位 72×72 圆 r36 底 #F5F5F5 + 白描边（节点 5:777），人形 38×38 #C3C3C3（5:778）
		  · 主标题「欢迎回来」24px 600 #333（节点 5:782）；副标题 13px #808080（5:783）
		  · 主按钮 325×44 r4 实心主色「微信手机号码登录」（节点 5:784）
		  · 次按钮 325×44 r4 白底主色描边「手机号验证码登录」（节点 5:786）
		  · 协议勾选行（节点 5:788）→ 公共组件 login-agreement
		⚠️ 本板**没有品牌方框**：节点 5:772~5:776（品牌标 + 酒杯 + 3 个矢量）已被设计删除，
		   页首是 63px 留白后直接是头像。原稿文件 726124180849143 的 2:379 版本仍有该方框，
		   两版不一致时以本板（用户指定的交接画布）为准。
		纵向节奏（页面内容坐标系，1px = 2rpx）：
		   头像 63~135 · 标题 159.5~194.5 · 副标题 197.5~216.5 · 按钮 262~306 / 322~366 · 协议行 406
		副标题手机号按设计稿要求脱敏展示（D5）：138****5678
		配色：设计稿蓝 #004CA0 → 主题绿 #5f7d22
	-->
	<view class="choose-page">
		<view class="avatar-ring">
			<image class="avatar-img" v-if="avatarUrl" :src="avatarUrl" mode="aspectFill"></image>
			<image class="avatar-img" v-else src="/static/assets/icons/login-person-gray.png" mode="aspectFit"></image>
		</view>

		<view class="welcome-title">欢迎回来</view>
		<view class="welcome-sub" v-if="subText">{{ subText }}</view>

		<view class="btn-group">
			<button class="choose-btn theme-bg" @tap="wxPhoneLogin">微信手机号码登录</button>
			<button class="choose-btn choose-btn-ghost theme-color theme-border-color" @tap="codeLogin">手机号验证码登录</button>
		</view>

		<view class="agreement-wrap">
			<login-agreement v-model="agreed" />
		</view>
	</view>
</template>

<script>
	import toastService from '../../../../utils/toast.service';
	import authService from '../../../../utils/auth';
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
				agreed: true,
				avatarUrl: '',
				nickname: '',
				phone: ''
			};
		},
		computed: {
			//设计稿副标题格式：昵称 · 138****5678（手机号必须脱敏）
			subText() {
				const masked = this.maskPhone(this.phone);
				return [this.nickname, masked].filter(Boolean).join(' · ');
			}
		},
		onLoad(options) {
			app = getApp();
			const opts = options || {};
			console.log(opts.scene);
			this.syncUser();
		},
		onShow() {
			//页面可能从 code 页登录成功后退回，这里重新同步一次展示信息
			this.syncUser();
			uni.login({
				success: (res) => {
					// 发送 res.code 到后台换取 openId, sessionKey, unionId
					app.globalData.code = res.code;
				}
			});
		},
		methods: {
			maskPhone(phone) {
				const s = String(phone || '').replace(/\D/g, '');
				if (s.length !== 11) return '';
				return s.slice(0, 3) + '****' + s.slice(7);
			},

			syncUser() {
				const info = app.globalData.userInfo || {};
				this.avatarUrl = info.avatarUrl || '';
				this.nickname = info.nickName || info.username || '';
				authService.getPhoneNumber().then((phone) => {
					this.phone = phone || '';
				});
			},

			//未勾选协议时统一拦截
			checkAgreed() {
				if (this.agreed) return true;
				toastService.showToast('请先阅读并同意《用户协议》和《隐私政策》');
				return false;
			},

			wxPhoneLogin() {
				if (!this.checkAgreed()) return;
				//微信手机号快捷登录需要小程序已认证并开通「手机号快速验证」组件，
				//后端旧接口 /rest/wxLogin/getWxPhone 走的是已废弃的 encryptedData 解密流，暂不可用（见批次4 D3）
				toastService.showToast('暂不支持微信手机号登录，请选择验证码登录');
			},

			codeLogin() {
				if (!this.checkAgreed()) return;
				uni.navigateTo({
					url: '../code/code'
				});
			}
		}
	};
</script>

<style>
	.choose-page {
		min-height: 100vh;
		background: #ffffff;
		padding: 0 50rpx;
		box-sizing: border-box;
	}

	/* 头像占位 72×72 圆（设计稿 y=63 起，即 126rpx） */
	.avatar-ring {
		width: 144rpx;
		height: 144rpx;
		margin: 126rpx auto 0 auto;
		border-radius: 50%;
		background: #f5f5f5;
		border: 2rpx solid #ffffff;
		overflow: hidden;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.avatar-img {
		width: 76rpx;
		height: 76rpx;
		border-radius: 50%;
	}

	/* 头像底 135 → 标题顶 159.5，间距 24.5px；行高锁 35px 对齐设计稿文本块高度 */
	.welcome-title {
		margin-top: 49rpx;
		font-size: 48rpx;
		font-weight: 600;
		color: #333333;
		text-align: center;
		line-height: 70rpx;
	}

	/* 标题底 194.5 → 副标题顶 197.5，间距 3px */
	.welcome-sub {
		margin-top: 6rpx;
		font-size: 26rpx;
		color: #808080;
		text-align: center;
		line-height: 38rpx;
	}

	/* 副标题底 216.5 → 按钮顶 262，间距 45.5px */
	.btn-group {
		margin-top: 91rpx;
		display: flex;
		flex-direction: column;
	}

	.choose-btn {
		width: 100%;
		height: 88rpx;
		line-height: 88rpx;
		border-radius: 8rpx;
		font-size: 30rpx;
		font-weight: 500;
		padding: 0;
	}

	.choose-btn + .choose-btn {
		margin-top: 32rpx;
	}

	.choose-btn-ghost {
		background: #ffffff;
		/* 颜色由全局 theme-border-color 提供，此处只给宽度与线型 */
		border-width: 2rpx;
		border-style: solid;
	}

	.choose-btn::after {
		border: none;
	}

	.agreement-wrap {
		margin-top: 80rpx;
		display: flex;
		justify-content: center;
	}
</style>
