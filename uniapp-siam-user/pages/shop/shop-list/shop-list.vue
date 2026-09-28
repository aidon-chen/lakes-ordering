<template>
	<view class="shop-list-page">
		<!-- 门店列表 -->
		<view class="shop-list">
			<view class="shop-card" :class="{ 'is-current': isCurrent(item) }" v-for="(item, index) in shopList"
				:key="index" @tap="tapShop(item)">
				<image class="shop-logo" :src="formatImg(item.shopLogoImg)" mode="aspectFill"></image>
				<view class="shop-info">
					<view class="shop-name-row">
						<text class="shop-name">{{ item.name }}</text>
						<text class="current-tag" v-if="isCurrent(item)">当前门店</text>
					</view>
					<text class="shop-address">{{ formatAddress(item) }}</text>
					<view class="shop-status-row">
						<text :class="item.isOperatingOfShop ? 'operating open' : 'operating closed'">
							{{ item.isOperatingOfShop ? '营业中' : '休息中' }}
						</text>
						<text class="shop-table-info" v-if="item.isOperatingOfShop">散台 · 卡座 · 包间</text>
					</view>
					<view class="shop-extra-row">
						<text class="shop-distance" v-if="item.distanceText">{{ item.distanceText }}</text>
						<text class="shop-time" v-if="item.startTime && item.endTime">营业 {{ item.startTime }}-{{ item.endTime }}</text>
					</view>
				</view>
				<van-icon name="arrow" class="shop-arrow" />
			</view>

			<van-empty v-if="!isLoading && shopList.length === 0" description="附近暂无门店" />
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
				shopList: [],
				isLoading: true,
				location: '',
				currentShopId: null,
				currentShopName: ''
			};
		},
		onShow() {
			this.currentShopId = uni.getStorageSync('currentShopId') || null;
			this.currentShopName = uni.getStorageSync('currentShopName') || '';
			const cached = getApp().globalData.deliveryAndSelfTaking.location;
			if (cached) {
				this.location = cached;
				this.loadList();
			} else {
				this.relocateTap();
			}
		},
		onPullDownRefresh() {
			this.loadList().finally(() => {
				uni.stopPullDownRefresh();
			});
		},
		methods: {
			relocateTap() {
				const _this = this;
				uni.getLocation({
					type: 'gcj02',
					success(res) {
						_this.location = res.longitude + ',' + res.latitude;
						getApp().globalData.deliveryAndSelfTaking.location = _this.location;
						_this.loadList();
					},
					fail() {
						// 定位失败回退默认坐标（App.vue 初始值）
						_this.location = getApp().globalData.deliveryAndSelfTaking.location || '112.885538,28.232363';
						_this.loadList();
					}
				});
			},
			loadList() {
				const _this = this;
				this.isLoading = true;
				return https.request('/rest/shop/listForPickup', {
					position: this.location,
					pageNo: 1,
					pageSize: 50
				}).then((result) => {
					_this.isLoading = false;
					if (result.success && result.data) {
						_this.shopList = result.data.records || [];
					} else {
						toastService.showToast(result.message || '获取门店列表失败');
					}
				}).catch(() => {
					_this.isLoading = false;
				});
			},
			isCurrent(item) {
				return this.currentShopId && item.id === this.currentShopId;
			},
			tapShop(item) {
				const _this = this;
				const oldId = uni.getStorageSync('currentShopId');
				if (oldId && oldId != item.id) {
					uni.showModal({
						title: '切换门店',
						content: '切换后点单将使用「' + item.name + '」，原门店购物车将保留。',
						success(res) {
							if (res.confirm) {
								_this.choose(item);
							}
						}
					});
				} else {
					this.choose(item);
				}
			},
			choose(item) {
				uni.setStorageSync('currentShopId', item.id);
				uni.setStorageSync('currentShopName', item.name);
				getApp().globalData.currentShopId = item.id;
				// 若从上一页进入，选完即返回；从详情页「选ta下单」则由详情页负责跳转
				const pages = getCurrentPages();
				if (pages.length > 1) {
					uni.navigateBack();
				} else {
					// 选门店恢复⑤：无上级页（直接打开）时回落点餐 tab，保证手选生效
					uni.switchTab({ url: '/pages/menu/index/index' });
				}
			},
			formatImg(path) {
				if (!path) return '/static/assets/images/user-head.png';
				return path.indexOf('http') === 0 ? path : GlobalConfig.ossUrl + path;
			},
			formatAddress(item) {
				return (item.province || '') + (item.city || '') + (item.area || '') + (item.street || '');
			}
		}
	};
</script>

<style lang="scss" scoped>
	.shop-list-page {
		min-height: 100vh;
		background-color: #F5F5F5;
		padding-bottom: 60rpx;
	}

	.shop-list {
		padding: 24rpx 30rpx 0;
	}

	.shop-card {
		display: flex;
		align-items: center;
		background-color: #FFFFFF;
		border: 2rpx solid transparent;
		border-radius: 16rpx;
		padding: 28rpx 24rpx;
		margin-bottom: 24rpx;
		/* C-03 当前门店：蓝色描边高亮 */
		&.is-current {
			border-color: #5f7d22;
		}

		.shop-logo {
			width: 120rpx;
			height: 120rpx;
			border-radius: 12rpx;
			flex-shrink: 0;
			background-color: #F5F5F5;
		}

		.shop-info {
			flex: 1;
			margin: 0 20rpx;
			overflow: hidden;

			.shop-name-row {
				display: flex;
				align-items: center;

				.shop-name {
					font-size: 32rpx;
					font-weight: 600;
					color: #222222;
					max-width: 300rpx;
					overflow: hidden;
					text-overflow: ellipsis;
					white-space: nowrap;
				}

				/* C-03 「当前门店」标签：主蓝底白字 */
				.current-tag {
					flex-shrink: 0;
					margin-left: 12rpx;
					font-size: 20rpx;
					color: #FFFFFF;
					background-color: #5f7d22;
					padding: 4rpx 14rpx;
					border-radius: 6rpx;
				}
			}

			.shop-address {
				display: block;
				margin-top: 10rpx;
				font-size: 24rpx;
				color: #666666;
				overflow: hidden;
				text-overflow: ellipsis;
				white-space: nowrap;
			}

			.shop-status-row {
				display: flex;
				align-items: center;
				margin-top: 10rpx;

				.operating {
					font-size: 24rpx;
					font-weight: 500;

					/* C-03 营业状态：营业中绿色 / 休息中灰 */
					&.open {
						color: #438A47;
					}

					&.closed {
						color: #999999;
					}
				}

				.shop-table-info {
					margin-left: 20rpx;
					font-size: 22rpx;
					color: #999999;
				}
			}

			.shop-extra-row {
				display: flex;
				align-items: center;
				margin-top: 10rpx;

				.shop-distance {
					font-size: 24rpx;
					color: #5f7d22;
					font-weight: 500;
					margin-right: 20rpx;
				}

				.shop-time {
					font-size: 22rpx;
					color: #999999;
				}
			}
		}

		.shop-arrow {
			color: #CCCCCC;
		}
	}
</style>
