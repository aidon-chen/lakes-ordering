<template>
	<view class="page" :style="{ paddingTop: statusBarHeight + 'px' }">

		<!-- C-02 首页活动弹窗 -->
		<view class="activity-mask" v-if="activityDialogShow" @tap="closeActivityDialog">
			<view class="activity-dialog" @tap.stop>
				<image v-if="activityAd.imagePath" :src="activityAd.imagePath" class="activity-image" mode="widthFix"
					@tap="activityImageTap" />
				<view class="activity-close" @tap="closeActivityDialog">
					<text class="activity-close-icon">✕</text>
				</view>
			</view>
		</view>

		<!-- A-01 露出式轮播：左右各露出 55px -->
		<swiper class="expose-swiper" previous-margin="110rpx" next-margin="110rpx" :autoplay="true"
			:interval="5000" :duration="500" :circular="true" @change="swiperChange">
			<swiper-item class="expose-swiper-item" v-for="(item, index) in carouselUrls" :key="index">
				<image :src="item.imagePath" class="expose-image" :class="swiperCurrent === index ? 'is-active' : ''"
					mode="aspectFill" :data-imageLinkUrl="item.imageLinkUrl" @tap="carouseCommodityDetailTap" />
			</swiper-item>
		</swiper>

		<!-- A-01 会员入口条（活动展示位） -->
		<view class="member-bar" hover-class="hover-class-public" @tap="memberCenterTap">
			<view class="member-bar-left">
				<view class="member-bar-title">会员中心</view>
				<view class="member-bar-desc">成长值 · 优惠券 · 会员专属价</view>
			</view>
			<van-icon name="arrow" class="member-bar-arrow" />
		</view>

		<!-- A-01 入口卡片区：堂食点餐 / 自提外带 -->
		<view class="entry-cards">
			<view class="entry-card" hover-class="hover-class-public" @tap="dineScanTap">
				<view class="entry-card-title">堂食点餐</view>
				<view class="entry-card-desc">扫桌台码 · 自动带入桌号</view>
				<van-icon name="scan" class="entry-card-icon" />
			</view>
			<view class="entry-card" hover-class="hover-class-public" @tap="selfPickupTap">
				<view class="entry-card-title">自提外带</view>
				<view class="entry-card-desc">下单免排队 · 到店即取</view>
				<van-icon name="bag-o" class="entry-card-icon" />
			</view>
		</view>
	</view>
</template>

<script>
	import GlobalConfig from '../../utils/global-config';
	import https from '../../utils/http';
	import authService from '../../utils/auth';
	//获取应用实例
	let app = null;
	export default {
		data() {
			return {
				statusBarHeight: 0,
				swiperCurrent: 0,
				carouselUrls: [],
				// C-02 活动弹窗
				activityDialogShow: false,
				activityAd: {
					imagePath: '',
					imageLinkUrl: ''
				}
			};
		},
		onLoad: function() {
			app = getApp();
			this.statusBarHeight = app.globalData.systemInfoSync.statusBarHeight;
			this.getCarouselList();
			this.getActivityDialog();
		},
		onShow: function() {
			const addressInfo = app.globalData.deliveryAndSelfTaking.initRegeoInfo;
			if (addressInfo) {
				app.globalData.deliveryAndSelfTaking.regeoInfo = addressInfo;
				app.globalData.deliveryAndSelfTaking.location = addressInfo.location;
			}
		},
		onPullDownRefresh() {
			this.getCarouselList();
			setTimeout(() => {
				uni.stopPullDownRefresh();
			}, 1000);
		},
		onHide() {
			this.activityDialogShow = false;
		},
		methods: {
			swiperChange(e) {
				this.swiperCurrent = e.detail.current;
			},

			getCarouselList() {
				https.request('/rest/advertisement/list', {
					type: 1,
					pageNo: -1,
					pageSize: 20
				}).then((result) => {
					if (result.success) {
						result.data.records.forEach(function(item) {
							item.imagePath = GlobalConfig.ossUrl + item.imagePath;
						});
						this.carouselUrls = result.data.records;
					}
				});
			},

			// C-02 首页活动弹窗：取弹窗广告（type=2），无数据则不展示
			getActivityDialog() {
				https.request('/rest/advertisement/list', {
					type: 2,
					pageNo: -1,
					pageSize: 1
				}).then((result) => {
					if (result.success && result.data && result.data.records && result.data.records
						.length > 0) {
						const ad = result.data.records[0];
						this.activityAd = {
							imagePath: GlobalConfig.ossUrl + ad.imagePath,
							imageLinkUrl: ad.imageLinkUrl || ''
						};
						this.activityDialogShow = true;
					}
				});
			},
			closeActivityDialog() {
				this.activityDialogShow = false;
			},
			activityImageTap() {
				if (!this.activityAd.imageLinkUrl) return;
				this.activityDialogShow = false;
				let _this = this;
				authService.checkIsLogin().then((result) => {
					if (result) {
						_this.navigateToAd(_this.activityAd.imageLinkUrl);
						return;
					}
					app.globalData.checkIsAuth('scope.userInfo');
				});
			},
			navigateToAd(url) {
				https.request('/rest/member/getLoginMemberInfo', {}).then((result) => {
					if (result.success) {
						uni.navigateTo({
							url: url
						});
					} else {
						app.globalData.checkIsAuth('scope.userInfo');
					}
				});
			},

			// 轮播图跳转（保留原登录检查逻辑）
			carouseCommodityDetailTap(e) {
				let _this = this;
				authService.checkIsLogin().then((result) => {
					if (result) {
						_this.navigateToAd(e.currentTarget.dataset.imagelinkurl);
						return;
					}
					app.globalData.checkIsAuth('scope.userInfo');
				});
			},

			// A-01 会员入口条
			memberCenterTap() {
				uni.navigateTo({
					url: '/pages/mine/member/index/index'
				});
			},

			// A-01 堂食点餐：扫桌台码 → 写入 dineInfo → 进点餐台（与点餐台切换逻辑一致）
			dineScanTap() {
				uni.scanCode({
					success: (res) => {
						const text = res.result || '';
						// 桌台码内容支持：tid=05&area=A 或含这些参数的 URL
						const match = text.match(/(?:\?|&)?(tid|tableId|tableNo)=(\w+)/i);
						if (!match) {
							uni.showToast({
								title: '桌台码无效，请扫描桌台二维码',
								icon: 'none'
							});
							return;
						}
						const area = (text.match(/[?&]area=(\w+)/i) || [])[1] || '';
						const tableId = match[2];
						app.globalData.deliveryAndSelfTaking.dineInfo = {
							tableId: tableId,
							area: area,
							tableNo: (area ? area + '区' : '') + tableId + '桌',
							tableLabel: (area ? area + ' 区 · ' : '') + '散台 ' + tableId,
							personCount: 4
						};
						uni.switchTab({
							url: '/pages/menu/index/index'
						});
					},
					fail: () => {}
				});
			},

			// A-01 自提外带：切自提模式进点餐台
			selfPickupTap() {
				app.globalData.deliveryAndSelfTaking.selfOutActiveIndex = 0;
				app.globalData.deliveryAndSelfTaking.dineInfo = null;
				app.globalData.deliveryAndSelfTaking.ifIndexSwitchTab = true;
				app.globalData.deliveryAndSelfTaking.ifChooseBack = false;
				app.globalData.deliveryAndSelfTaking.ifChoosePayBack = false;
				uni.switchTab({
					url: '/pages/menu/index/index'
				});
			}
		}
	};
</script>
<style>
	page {
		width: 100%;
		background: #FFFFFF;
	}

	.page {
		min-height: 100vh;
		padding-bottom: 40rpx;
	}

	/* ===== A-01 露出式轮播 ===== */
	.expose-swiper {
		height: 558rpx;
	}

	.expose-swiper-item {
		height: 100%;
	}

	.expose-image {
		display: block;
		width: 550rpx;
		height: 558rpx;
		margin: 0 auto;
		border-radius: 20rpx;
		transform: scale(0.92);
		opacity: 0.6;
		transition: transform 0.3s ease, opacity 0.3s ease;
	}

	.expose-image.is-active {
		transform: scale(1);
		opacity: 1;
	}

	/* ===== A-01 会员入口条 ===== */
	.member-bar {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin: 30rpx 30rpx 0;
		padding: 28rpx 30rpx;
		background: #FFF7E6;
		border-radius: 20rpx;
	}

	.member-bar-left {
		display: flex;
		flex-direction: column;
	}

	.member-bar-title {
		font-size: 32rpx;
		font-weight: 600;
		color: #333333;
	}

	.member-bar-desc {
		margin-top: 8rpx;
		font-size: 24rpx;
		color: #9C7A3C;
	}

	.member-bar-arrow {
		font-size: 32rpx;
		color: #C9A86A;
	}

	/* ===== A-01 入口卡片区 ===== */
	.entry-cards {
		display: grid;
		grid-template-columns: 1fr 1fr;
		grid-column-gap: 20rpx;
		margin: 30rpx 30rpx 0;
	}

	.entry-card {
		position: relative;
		height: 350rpx;
		background: #F6F6F6;
		border-radius: 20rpx;
		padding: 32rpx;
		box-sizing: border-box;
		overflow: hidden;
	}

	.entry-card-title {
		font-size: 34rpx;
		font-weight: 600;
		color: #222222;
	}

	.entry-card-desc {
		margin-top: 12rpx;
		font-size: 24rpx;
		color: #646566;
	}

	.entry-card-icon {
		position: absolute;
		right: 28rpx;
		bottom: 24rpx;
		font-size: 88rpx;
		color: #D9D9D9;
	}

	/* ===== C-02 活动弹窗 ===== */
	.activity-mask {
		position: fixed;
		left: 0;
		top: 0;
		right: 0;
		bottom: 0;
		z-index: 9999;
		display: flex;
		align-items: center;
		justify-content: center;
		background: rgba(0, 0, 0, 0.55);
	}

	.activity-dialog {
		width: 560rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
	}

	.activity-image {
		width: 560rpx;
		border-radius: 24rpx;
		background: #FFFFFF;
	}

	.activity-close {
		margin-top: 32rpx;
		width: 64rpx;
		height: 64rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		border: 2rpx solid rgba(255, 255, 255, 0.8);
		border-radius: 50%;
	}

	.activity-close-icon {
		font-size: 32rpx;
		color: #FFFFFF;
		line-height: 1;
	}
</style>
