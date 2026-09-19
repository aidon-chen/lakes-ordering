<template>
	<view class="pickup-page">
		<!-- 自提码卡（画板 B-09） -->
		<view class="card code-card">
			<view class="code-icon theme-bg-light">
				<van-icon name="qr" class="theme-color code-icon-inner" />
			</view>
			<view class="code-main">
				<view class="code-label">自提码</view>
				<view class="code-value">{{ pickupCode || '--' }}</view>
			</view>
			<view class="code-copy" @tap="copyPickupCode">复制</view>
		</view>

		<!-- 自提流程卡：倒序时间线，最新节点高亮（与 B-08 退款进度同一视觉口径） -->
		<view class="card process-card">
			<view class="card-title">自提流程</view>
			<view class="process-list">
				<view class="process-item" v-for="(item, index) in reachedList" :key="index">
					<view class="process-axis">
						<view :class="'process-dot ' + (index === 0 ? 'process-dot-active theme-bg' : 'process-dot-normal')"></view>
						<view class="process-line" v-if="index !== reachedList.length - 1"></view>
					</view>
					<view class="process-body">
						<view :class="'process-name ' + (index === 0 ? 'theme-color process-name-active' : '')">{{ item.name }}</view>
						<view :class="'process-time ' + (index === 0 ? 'theme-color' : '')">{{ formatTime(item.time) }}</view>
					</view>
				</view>
				<view v-if="reachedList.length === 0" class="process-empty">暂无自提进度</view>
			</view>
		</view>

		<!-- 门店卡 -->
		<view class="card shop-card">
			<van-icon name="location-o" class="theme-color shop-icon" />
			<view class="shop-main">
				<view class="shop-name">{{ shopName || '乐章 LIVE 酒馆' }}</view>
				<view class="shop-addr">{{ shopAddress || '请到门店前台出示自提码取货' }}</view>
			</view>
		</view>

		<view class="tip-text" v-if="pickupDeadlineText">取货截止：{{ pickupDeadlineText }}</view>

		<!-- 底部操作栏 -->
		<view class="footer-bar">
			<view class="btn btn-plain" @tap="contactShop">联系门店</view>
			<view class="btn btn-primary theme-bg" @tap="confirmPickup">{{ verifyTime ? '已取货' : '确认取货' }}</view>
		</view>
	</view>
</template>

<script>
	import https from '../../../../utils/http';
	import authService from '../../../../utils/auth';
	import toastService from '../../../../utils/toast.service';
	let app = null;
	export default {
		data() {
			return {
				orderId: null,
				pickupCode: '',
				pickupDeadline: null,
				pickupDeadlineText: '',
				verifyTime: null,
				status: null,
				shopName: '',
				shopAddress: '',
				processList: []
			};
		},
		computed: {
			// 只渲染已到达的节点（倒序由后端返回：最新在上）
			reachedList() {
				return (this.processList || []).filter(function(item) {
					return item && item.reached;
				});
			}
		},
		onLoad: function(options) {
			app = getApp();
			this.orderId = options.id;
			this.getPickupProcess();
		},
		onShow: function() {},
		methods: {
			getPickupProcess() {
				if (!this.orderId) {
					toastService.showToast('缺少订单参数');
					return;
				}
				https.request('/rest/member/pointsMall/order/selectPickupProcess', {
					id: this.orderId
				}, authService.getToken()).then((result) => {
					if (result.success) {
						const data = result.data || {};
						this.pickupCode = data.pickupCode;
						this.pickupDeadline = data.pickupDeadline;
						this.pickupDeadlineText = this.formatTime(data.pickupDeadline);
						this.verifyTime = data.verifyTime;
						this.status = data.status;
						this.shopName = data.shopName;
						this.shopAddress = data.shopAddress;
						this.processList = data.processList || [];
					}
				});
			},

			copyPickupCode() {
				if (!this.pickupCode) {
					toastService.showToast('暂无自提码');
					return;
				}
				uni.setClipboardData({
					data: String(this.pickupCode),
					success: () => {
						toastService.showToast('自提码已复制');
					}
				});
			},

			// 确认取货：调商家端共用的核销状态机（顾客自助核销，verifier_id 留空）
			confirmPickup() {
				if (this.verifyTime) {
					toastService.showToast('该订单已取货');
					return;
				}
				uni.showModal({
					title: '确认取货',
					content: '请确认已到店领取全部商品',
					success: (res) => {
						if (!res.confirm) {
							return;
						}
						toastService.showLoading('处理中...');
						https.request('/rest/member/pointsMall/order/confirmReceipt', {
							id: this.orderId
						}, authService.getToken()).then((result) => {
							toastService.hideLoading();
							if (result.success) {
								toastService.showToast('已确认取货');
								this.getPickupProcess();
							} else {
								toastService.showToast(result.message || '确认失败');
							}
						});
					}
				});
			},

			// 联系门店：优先拨号（门店电话见门店详情页），地址可复制
			contactShop() {
				if (this.shopAddress) {
					uni.setClipboardData({
						data: this.shopAddress,
						success: () => {
							toastService.showToast('门店地址已复制');
						}
					});
					return;
				}
				toastService.showToast('暂无门店联系方式');
			},

			// 统一时间显示：yyyy-MM-dd HH:mm
			formatTime(value) {
				if (!value) {
					return '';
				}
				let str = String(value).replace('T', ' ');
				// 兼容 "2026-09-14 18:20:00" 与 ISO 串
				return str.length > 16 ? str.substring(0, 16) : str;
			}
		}
	};
</script>

<style>
	page {
		background-color: #f5f5f5;
	}

	.pickup-page {
		padding: 20rpx 0 140rpx 0;
	}

	.card {
		background-color: #ffffff;
		border-radius: 24rpx;
		margin: 0 24rpx 20rpx 24rpx;
	}

	/* 自提码卡 */
	.code-card {
		display: flex;
		align-items: center;
		padding: 30rpx 24rpx;
	}

	.code-icon {
		width: 76rpx;
		height: 76rpx;
		border-radius: 12rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		margin-right: 20rpx;
	}

	.code-icon-inner {
		font-size: 40rpx;
	}

	.code-main {
		flex: 1;
	}

	.code-label {
		font-size: 30rpx;
		font-weight: 500;
		color: #333333;
	}

	.code-value {
		font-size: 30rpx;
		color: #999999;
		margin-top: 8rpx;
		letter-spacing: 2rpx;
	}

	.code-copy {
		padding: 8rpx 22rpx;
		background-color: #f2f2f2;
		border-radius: 8rpx;
		font-size: 22rpx;
		color: #666666;
	}

	/* 流程卡 */
	.process-card {
		padding: 30rpx 24rpx;
	}

	.card-title {
		font-size: 30rpx;
		font-weight: 500;
		color: #333333;
		margin-bottom: 24rpx;
	}

	.process-item {
		display: flex;
	}

	.process-axis {
		width: 40rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
	}

	.process-dot {
		border-radius: 50%;
		margin-top: 8rpx;
	}

	.process-dot-active {
		width: 24rpx;
		height: 24rpx;
	}

	.process-dot-normal {
		width: 20rpx;
		height: 20rpx;
		background-color: #d8d8d8;
	}

	.process-line {
		flex: 1;
		width: 2rpx;
		background-color: #e8e8e8;
		min-height: 46rpx;
	}

	.process-body {
		flex: 1;
		padding-bottom: 30rpx;
	}

	.process-name {
		font-size: 26rpx;
		color: #202020;
		line-height: 36rpx;
	}

	.process-name-active {
		font-weight: 500;
	}

	.process-time {
		font-size: 24rpx;
		color: #999999;
		margin-top: 6rpx;
	}

	.process-empty {
		font-size: 26rpx;
		color: #999999;
		text-align: center;
		padding: 30rpx 0;
	}

	/* 门店卡 */
	.shop-card {
		display: flex;
		align-items: flex-start;
		padding: 30rpx 24rpx;
	}

	.shop-icon {
		font-size: 40rpx;
		margin-right: 16rpx;
		line-height: 44rpx;
	}

	.shop-main {
		flex: 1;
	}

	.shop-name {
		font-size: 30rpx;
		font-weight: 500;
		color: #202020;
	}

	.shop-addr {
		font-size: 26rpx;
		color: #909090;
		margin-top: 8rpx;
		line-height: 36rpx;
	}

	.tip-text {
		font-size: 24rpx;
		color: #999999;
		text-align: center;
		margin-top: 10rpx;
	}

	/* 底部操作栏 */
	.footer-bar {
		position: fixed;
		left: 0;
		right: 0;
		bottom: 0;
		height: 128rpx;
		background-color: #ffffff;
		display: flex;
		align-items: center;
		padding: 0 24rpx;
		box-sizing: border-box;
		border-top: 1rpx solid #f7f7f7;
	}

	.btn {
		height: 80rpx;
		border-radius: 40rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 30rpx;
	}

	.btn-plain {
		width: 220rpx;
		border: 1rpx solid #dbdbdb;
		color: #666666;
		margin-right: 20rpx;
	}

	.btn-primary {
		flex: 1;
		color: #ffffff;
		font-weight: 500;
	}
</style>
