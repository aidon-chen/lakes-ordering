<template>
	<view class="plt">
		<!-- C-02 首页活动弹窗 · 超链接测试落地页（独立测试界面）
		     用途：商家端「营销中心 → 首页活动弹窗」把超链接(imageLinkUrl)配成本页路径，
		     客户端点击弹窗图片后跳到本页并回显参数，即验证「配置 → 弹窗 → 点击跳转」整条链路。
		     注意：客户端 activityImageTap 会先校验登录态，未登录会先引导登录再跳。
		     ⚠️ 本文件属正本 uniapp-siam-user：改动请落正本再 cp 到 customer-cli/src，直接改 src 会被 sync 覆盖（09-24 踩坑） -->
		<view class="plt-hero">
			<view class="plt-hero-check">✓</view>
			<view class="plt-hero-title">弹窗超链接跳转成功</view>
			<view class="plt-hero-sub">你已从「首页活动弹窗」跳转到本测试页</view>
		</view>

		<view class="plt-card">
			<view class="plt-card-title">到达时间</view>
			<view class="plt-card-value">{{ arriveTime }}</view>
		</view>

		<view class="plt-card">
			<view class="plt-card-title">收到的跳转参数（options）</view>
			<view class="plt-param-row" v-for="(value, key) in options" :key="key">
				<text class="plt-param-key">{{ key }}</text>
				<text class="plt-param-eq">=</text>
				<text class="plt-param-value">{{ value }}</text>
			</view>
			<view class="plt-card-empty" v-if="!hasParams">无参数（配置的超链接路径后没带 ? 参数）</view>
		</view>

		<view class="plt-card">
			<view class="plt-card-title">本页路径（复制到商家端「超链接」输入框即可复测）</view>
			<view class="plt-path">{{ pagePath }}</view>
			<view class="plt-copy" @tap="copyPath">复制路径</view>
		</view>

		<view class="plt-tip">
			验证口径：本页能到达并正确回显参数 = 弹窗超链接链路通。展示频次/生效时间由弹窗数据控制；
			点击跳转前客户端会校验登录态（未登录先引导登录）。
		</view>

		<view class="plt-back" @tap="goHome">返回首页</view>
	</view>
</template>

<script>
	import dateHelper from '../../../utils/date-helper';
	export default {
		data() {
			return {
				arriveTime: '',
				options: {},
				pagePath: '/pages/index/popup-link-test/popup-link-test'
			};
		},
		computed: {
			hasParams() {
				return Object.keys(this.options).length > 0;
			}
		},
		onLoad(options) {
			// uni-app 会把 query 以字符串形式给到 onLoad，原样回显不做解码加工（所见即商家端所配）
			const normalized = {};
			if (options) {
				Object.keys(options).forEach((key) => {
					normalized[key] = String(options[key]);
				});
			}
			this.options = normalized;
			this.arriveTime = dateHelper.formatTime();
		},
		methods: {
			copyPath() {
				uni.setClipboardData({
					data: this.pagePath,
					success: () => {
						uni.showToast({
							title: '路径已复制',
							icon: 'success'
						});
					}
				});
			},
			// 首页是 tabBar 页，必须 switchTab（navigateTo 跳 tabBar 会静默失败）
			goHome() {
				uni.switchTab({
					url: '/pages/index/index'
				});
			}
		}
	};
</script>
<style>
	page {
		background: #f5f5f5;
	}

	.plt {
		min-height: 100vh;
		background: #f5f5f5;
		padding: 24rpx 30rpx 60rpx;
		box-sizing: border-box;
	}

	.plt-hero {
		background: #5F7D22;
		border-radius: 24rpx;
		padding: 48rpx 30rpx;
		text-align: center;
	}

	.plt-hero-check {
		width: 96rpx;
		height: 96rpx;
		line-height: 96rpx;
		text-align: center;
		border-radius: 50%;
		background: rgba(255, 255, 255, 0.2);
		color: white;
		font-size: 56rpx;
		margin: 0 auto;
	}

	.plt-hero-title {
		font-size: 34rpx;
		font-weight: bold;
		color: white;
		margin-top: 24rpx;
	}

	.plt-hero-sub {
		font-size: 24rpx;
		color: rgba(255, 255, 255, 0.8);
		margin-top: 12rpx;
	}

	.plt-card {
		background: white;
		border-radius: 24rpx;
		padding: 28rpx;
		margin-top: 24rpx;
	}

	.plt-card-title {
		font-size: 24rpx;
		color: #999999;
	}

	.plt-card-value {
		font-size: 30rpx;
		color: #333333;
		margin-top: 12rpx;
	}

	.plt-card-empty {
		font-size: 26rpx;
		color: #bbbbbb;
		margin-top: 12rpx;
	}

	.plt-param-row {
		display: flex;
		flex-direction: row;
		align-items: center;
		margin-top: 14rpx;
	}

	.plt-param-key {
		font-size: 28rpx;
		color: #5F7D22;
		font-weight: 500;
	}

	.plt-param-eq {
		font-size: 28rpx;
		color: #999999;
		margin: 0 12rpx;
	}

	.plt-param-value {
		font-size: 28rpx;
		color: #333333;
		word-break: break-all;
	}

	.plt-path {
		font-size: 26rpx;
		color: #333333;
		background: #f7f7f7;
		border-radius: 12rpx;
		padding: 16rpx 20rpx;
		margin-top: 12rpx;
		word-break: break-all;
	}

	.plt-copy {
		display: inline-block;
		margin-top: 20rpx;
		padding: 12rpx 48rpx;
		background: #5F7D22;
		color: white;
		font-size: 24rpx;
		border-radius: 32rpx;
	}

	.plt-tip {
		font-size: 22rpx;
		color: #999999;
		line-height: 1.7;
		margin-top: 24rpx;
	}

	.plt-back {
		margin-top: 40rpx;
		background: white;
		color: #5F7D22;
		border: 1rpx solid #5F7D22;
		font-size: 28rpx;
		text-align: center;
		padding: 22rpx 0;
		border-radius: 44rpx;
	}
</style>
