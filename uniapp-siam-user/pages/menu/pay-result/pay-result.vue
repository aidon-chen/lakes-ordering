<template>
	<loading-toast />
	<view class="page">
		<view class="top-space"></view>
		<!-- 结果区 -->
		<view class="result-area">
			<view class="success-icon">
				<view class="check-mark"></view>
			</view>
			<text class="result-title">支付成功</text>
			<text class="result-amount">¥{{ amountText }} · {{ payChannelText }}</text>
			<text class="result-shop">{{ shopText }}</text>
		</view>
		<view class="middle-space"></view>
		<!-- 按钮行 -->
		<view class="btn-row">
			<view class="btn-plain" @tap="goOrderDetail">查看订单</view>
			<view class="btn-primary" @tap="goHome">返回首页</view>
		</view>
	</view>
</template>

<script>
	export default {
		data() {
			return {
				orderId: '',
				amount: 0,
				payChannel: '微信支付',
				shopName: '',
				orderMode: ''
			};
		},
		computed: {
			amountText() {
				return Number(this.amount || 0).toFixed(2);
			},
			payChannelText() {
				return this.payChannel || '微信支付';
			},
			shopText() {
				let text = this.shopName || '';
				if (this.orderMode) {
					text += (text ? ' · ' : '') + this.orderMode;
				}
				return text || '感谢惠顾';
			}
		},
		onLoad(options) {
			this.orderId = (options && options.id) || '';
			this.amount = parseFloat((options && options.amount) || 0) || 0;
			// 09-25 修复：channel 与 shop/mode 同样经 encodeURIComponent 编码传入（pay.vue:1156），
			// 此前漏了解码 → 支付成功页把「微信支付」显示成 %E5%BE%AE... 乱码
			this.payChannel = (options && options.channel) ? decodeURIComponent(options.channel) : '微信支付';
			this.shopName = (options && options.shop) ? decodeURIComponent(options.shop) : '';
			this.orderMode = (options && options.mode) ? decodeURIComponent(options.mode) : '';
		},
		methods: {
			goOrderDetail() {
				if (!this.orderId) {
					uni.switchTab({
						url: '/pages/order/index/index'
					});
					return;
				}
				uni.redirectTo({
					url: '/pages/order/detail/detail?id=' + this.orderId
				});
			},
			goHome() {
				uni.switchTab({
					url: '/pages/index/index'
				});
			}
		}
	};
</script>

<style>
	.page {
		background: #ffffff;
		min-height: 100vh;
		display: flex;
		flex-direction: column;
		padding-top: var(--status-bar-height);
	}

	.top-space {
		height: 200rpx;
	}

	.result-area {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 20rpx;
	}

	.success-icon {
		width: 144rpx;
		height: 144rpx;
		border-radius: 72rpx;
		background: #5f7d22;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.check-mark {
		width: 68rpx;
		height: 34rpx;
		border-left: 8rpx solid #ffffff;
		border-bottom: 8rpx solid #ffffff;
		transform: rotate(-45deg);
		margin-top: -12rpx;
	}

	.result-title {
		font-size: 40rpx;
		font-weight: 500;
		color: #333333;
		margin-top: 4rpx;
	}

	.result-amount {
		font-size: 26rpx;
		color: #999999;
	}

	.result-shop {
		font-size: 24rpx;
		color: #999999;
	}

	.middle-space {
		flex: 1;
	}

	.btn-row {
		display: flex;
		flex-direction: row;
		gap: 20rpx;
		padding: 32rpx;
	}

	.btn-plain {
		width: 300rpx;
		height: 88rpx;
		border: 1rpx solid #e0e0e0;
		border-radius: 44rpx;
		background: #ffffff;
		color: #666666;
		font-size: 28rpx;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.btn-primary {
		width: 300rpx;
		height: 88rpx;
		border-radius: 44rpx;
		background: #5f7d22;
		color: #ffffff;
		font-size: 28rpx;
		font-weight: 500;
		display: flex;
		align-items: center;
		justify-content: center;
	}
</style>
