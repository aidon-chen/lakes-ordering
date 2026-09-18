<template>
	<view class="shop-detail-page" v-if="shop">
		<!-- 头部：门店信息 -->
		<view class="header-card">
			<image class="shop-logo" :src="formatImg(shop.shopLogoImg)" mode="aspectFill"></image>
			<view class="header-info">
				<view class="name-row">
					<text class="shop-name">{{ shop.name }}</text>
					<text :class="isOperating ? 'operating-tag open' : 'operating-tag closed'">
						{{ isOperating ? '营业中' : '已打烊' }}
					</text>
				</view>
				<text class="shop-time" v-if="shop.startTime && shop.endTime">营业时间 {{ shop.startTime }}-{{ shop.endTime }}</text>
				<text class="shop-address" @tap="openLocation">{{ formatAddress() }} ＞</text>
			</view>
		</view>

		<!-- 联系与评级 -->
		<view class="action-card">
			<view class="action-item" @tap="callShop" v-if="shop.contactPhone">
				<van-icon name="phone-o" />
				<text class="action-text">联系门店</text>
			</view>
			<view class="action-divider" v-if="shop.contactPhone"></view>
			<view class="action-item">
				<van-icon name="star-o" />
				<text class="action-text">评分 {{ shop.serviceRating || '-' }}</text>
			</view>
		</view>

		<!-- 门店简介 -->
		<view class="section-card" v-if="shop.briefIntroduction">
			<text class="section-title">门店简介</text>
			<text class="section-content">{{ shop.briefIntroduction }}</text>
		</view>

		<!-- 店内环境 -->
		<view class="section-card" v-if="withinImages.length > 0">
			<text class="section-title">店内环境</text>
			<view class="within-images">
				<image class="within-image" v-for="(img, index) in withinImages" :key="index"
					:src="formatImg(img)" mode="aspectFill" @tap="previewImage(index)"></image>
			</view>
		</view>

		<!-- 台位类型说明（散台/卡座/低消） -->
		<view class="section-card">
			<text class="section-title">台位类型</text>
			<view class="table-type-list" v-if="tableTypes.length > 0">
				<view class="table-type-item" v-for="(type, index) in tableTypes" :key="index">
					<view class="type-head">
						<text class="type-name">{{ type.name }}</text>
						<text class="type-min-consume">低消 ¥{{ type.minConsume }}</text>
					</view>
					<text class="type-desc" v-if="type.description">{{ type.description }}</text>
				</view>
			</view>
			<text class="empty-tip" v-else>门店暂未配置台位类型</text>
		</view>

		<!-- 底部操作 -->
		<view class="footer-bar">
			<view class="footer-btn" @tap="chooseAndGo">选ta下单</view>
		</view>
	</view>
</template>

<script>
	import https from '../../../utils/http';
	import GlobalConfig from '../../../utils/global-config';
	import toastService from '../../../utils/toast.service';

	export default {
		data() {
			return {
				shopId: null,
				shop: null,
				isOperating: false,
				tableTypes: [],
				withinImages: []
			};
		},
		onLoad(options) {
			this.shopId = options.id;
			this.loadDetail();
		},
		methods: {
			loadDetail() {
				const _this = this;
				https.request('/rest/shop/detailForPickup', {
					id: Number(this.shopId)
				}).then((result) => {
					if (result.success && result.data) {
						_this.shop = result.data.shop;
						_this.isOperating = !!result.data.isOperatingOfShop;
						_this.tableTypes = result.data.tableTypes || [];
						// shopWithinImg 可能是 JSON 数组字符串
						const raw = _this.shop.shopWithinImg;
						if (raw) {
							try {
								const parsed = JSON.parse(raw);
								_this.withinImages = Array.isArray(parsed) ? parsed : (raw ? [raw] : []);
							} catch (e) {
								_this.withinImages = raw.indexOf(',') > -1 ? raw.split(',') : [raw];
							}
						}
					} else {
						toastService.showToast(result.message || '获取门店详情失败');
					}
				});
			},
			callShop() {
				uni.makePhoneCall({
					phoneNumber: this.shop.contactPhone
				});
			},
			openLocation() {
				const _this = this;
				if (!this.shop.longitude || !this.shop.latitude) return;
				uni.openLocation({
					longitude: Number(_this.shop.longitude),
					latitude: Number(_this.shop.latitude),
					name: _this.shop.name,
					address: _this.formatAddress()
				});
			},
			previewImage(index) {
				const urls = this.withinImages.map((img) => this.formatImg(img));
				uni.previewImage({
					current: index,
					urls: urls
				});
			},
			chooseAndGo() {
				uni.setStorageSync('currentShopId', this.shop.id);
				uni.setStorageSync('currentShopName', this.shop.name);
				getApp().globalData.currentShopId = this.shop.id;
				uni.switchTab({
					url: '/pages/menu/index/index'
				});
			},
			formatImg(path) {
				if (!path) return '/static/assets/images/user-head.png';
				return path.indexOf('http') === 0 ? path : GlobalConfig.ossUrl + path;
			},
			formatAddress() {
				const s = this.shop;
				return (s.province || '') + (s.city || '') + (s.area || '') + (s.street || '') + (s.houseNumber || '');
			}
		}
	};
</script>

<style lang="scss" scoped>
	.shop-detail-page {
		min-height: 100vh;
		background-color: #F5F5F5;
		padding-bottom: 160rpx;
	}

	.header-card {
		display: flex;
		background-color: #FFFFFF;
		padding: 32rpx 30rpx;

		.shop-logo {
			width: 140rpx;
			height: 140rpx;
			border-radius: 16rpx;
			flex-shrink: 0;
			background-color: #F5F5F5;
		}

		.header-info {
			flex: 1;
			margin-left: 24rpx;
			overflow: hidden;

			.name-row {
				display: flex;
				align-items: center;

				.shop-name {
					font-size: 36rpx;
					font-weight: 600;
					color: #222222;
				}

				.operating-tag {
					margin-left: 14rpx;
					font-size: 20rpx;
					padding: 4rpx 12rpx;
					border-radius: 6rpx;
					flex-shrink: 0;

					&.open {
						color: #5f7d22;
						background-color: rgba(0, 76, 160, 0.08);
					}

					&.closed {
						color: #999999;
						background-color: #F5F5F5;
					}
				}
			}

			.shop-time {
				display: block;
				margin-top: 12rpx;
				font-size: 24rpx;
				color: #999999;
			}

			.shop-address {
				display: block;
				margin-top: 12rpx;
				font-size: 26rpx;
				color: #333333;
				overflow: hidden;
				text-overflow: ellipsis;
				white-space: nowrap;
			}
		}
	}

	.action-card {
		display: flex;
		align-items: center;
		background-color: #FFFFFF;
		margin-top: 20rpx;
		padding: 26rpx 0;

		.action-item {
			flex: 1;
			display: flex;
			align-items: center;
			justify-content: center;
			color: #5f7d22;

			.action-text {
				margin-left: 10rpx;
				font-size: 26rpx;
			}
		}

		.action-divider {
			width: 1rpx;
			height: 40rpx;
			background-color: #EEEEEE;
		}
	}

	.section-card {
		background-color: #FFFFFF;
		margin-top: 20rpx;
		padding: 30rpx;

		.section-title {
			display: block;
			font-size: 30rpx;
			font-weight: 600;
			color: #222222;
		}

		.section-content {
			display: block;
			margin-top: 16rpx;
			font-size: 26rpx;
			color: #666666;
			line-height: 1.7;
		}

		.empty-tip {
			display: block;
			margin-top: 16rpx;
			font-size: 24rpx;
			color: #999999;
		}
	}

	.within-images {
		display: flex;
		flex-wrap: wrap;
		margin-top: 20rpx;

		.within-image {
			width: 212rpx;
			height: 212rpx;
			border-radius: 12rpx;
			margin: 0 12rpx 12rpx 0;
			background-color: #F5F5F5;
		}
	}

	.table-type-list {
		margin-top: 8rpx;

		.table-type-item {
			padding: 24rpx 0;
			border-bottom: 1rpx solid #F5F5F5;

			&:last-child {
				border-bottom: none;
			}

			.type-head {
				display: flex;
				align-items: center;
				justify-content: space-between;

				.type-name {
					font-size: 28rpx;
					font-weight: 500;
					color: #222222;
				}

				.type-min-consume {
					font-size: 26rpx;
					color: #6F4E20;
					background-color: #edf3df;
					padding: 4rpx 16rpx;
					border-radius: 8rpx;
				}
			}

			.type-desc {
				display: block;
				margin-top: 12rpx;
				font-size: 24rpx;
				color: #666666;
				line-height: 1.6;
			}
		}
	}

	.footer-bar {
		position: fixed;
		left: 0;
		right: 0;
		bottom: 0;
		background-color: #FFFFFF;
		padding: 20rpx 30rpx calc(20rpx + env(safe-area-inset-bottom));

		.footer-btn {
			height: 88rpx;
			line-height: 88rpx;
			text-align: center;
			background-color: #5f7d22;
			color: #FFFFFF;
			font-size: 30rpx;
			border-radius: 44rpx;
		}
	}
</style>
