<template>
	<view class="page">
		<!-- 顶部分栏Tabs + 状态子Tab（C-11） -->
		<view class="top-tabs">
			<view class="main-tabs">
				<view class="main-tab-item" v-for="(item, index) in tabList" :key="index" @tap="clickTab(index)">
					<text :class="'main-tab-text ' + (currentTab == index ? 'active' : '')">{{ item.modeName }}</text>
					<view class="tab-indicator" v-if="currentTab == index"></view>
				</view>
			</view>
			<view class="sub-tabs">
				<scroll-view scroll-x class="sub-tab-scroll" :show-scrollbar="false">
					<view class="sub-tab-list">
						<view class="sub-tab-item" v-for="(item, index) in activeSubTabs" :key="index"
							@tap="clickOrderTab(index)">
							<view class="sub-tab-inner">
								<text :class="'sub-tab-text ' + (currentOrderTab == index ? 'active' : '')">{{ item.modeName }}</text>
								<view class="badge" v-if="item.number > 0">{{ item.number > 99 ? '99+' : item.number }}</view>
							</view>
							<view class="sub-indicator" v-if="currentOrderTab == index"></view>
						</view>
					</view>
				</scroll-view>
			</view>
		</view>

		<!-- 订单列表 -->
		<view class="order-list" v-if="activeList.length > 0">
			<view class="order-card" v-for="(item, index) in activeList" :key="item.id" hover-class="card-hover"
				@tap="goDetail(item)">
				<view class="card-header">
					<text class="shop-name">{{ item.shopName }}</text>
					<text :class="'status-text ' + (item.statusActive ? 'blue' : 'gray')">{{ item.statusText }}</text>
				</view>
				<text class="goods-summary">{{ item.description }}</text>
				<view class="money-row">
					<text class="money-label">合计</text>
					<text class="money-value">¥{{ item.actualPrice }}</text>
				</view>
				<view class="btn-row" v-if="cardButtons(item).length > 0">
					<view v-for="btn in cardButtons(item)" :key="btn.key"
						:class="'card-btn ' + (btn.type == 'primary' ? 'btn-primary' : 'btn-stroke')"
						hover-class="btn-hover" @tap.stop="onCardButton(btn.key, item)">{{ btn.text }}</view>
				</view>
			</view>
			<view class="loading-box" v-if="isLoading">
				<van-loading />
			</view>
			<view class="end-tip" v-if="!isLoading && isEndList">没有更多啦</view>
		</view>

		<!-- 空状态 -->
		<van-empty v-if="!isLoading && activeList.length <= 0"
			:description="'暂无' + activeSubTabs[currentOrderTab].modeName + '订单'">
			<van-button type="primary" size="small" color="#5f7d22" round @tap="goDrink">
				{{ currentTab == 0 ? '去喝一杯' : '去逛逛' }}
			</van-button>
		</van-empty>
	</view>
</template>

<script>
	import https from '../../../utils/http';
	import authService from '../../../utils/auth';
	import toastService from '../../../utils/toast.service';
	import dateHelper from '../../../utils/date-helper';
	import systemStatus from '../../../utils/system-status';

	const PAGE_SIZE = 10;
	// 进行中状态（头行状态字标蓝），其余（已完成/售后/取消）标灰
	const ACTIVE_STATUSES = [1, 2, 3, 4, 5];

	export default {
		data() {
			return {
				currentTab: 0,
				currentOrderTab: 0,
				tabList: [{
						modeId: 0,
						modeName: '点单订单'
					},
					{
						modeId: 1,
						modeName: '商城订单'
					}
				],
				// 点单订单状态子Tab：本项目纯堂食 + 到店自提，无配送 → 已去掉「待收货」（外卖口径）
				// 后端 tabType 取值见 OrderMapper：waitPayment=status1、waitPickedUp=status2/3、afterSales=7/8/9/11
				shopOrderTabList: [{
						modeId: 0,
						modeType: 'all',
						modeName: '全部'
					},
					{
						modeId: 1,
						modeType: 'waitPayment',
						modeName: '待付款'
					},
					{
						modeId: 2,
						modeType: 'waitPickedUp',
						modeName: '待自提'
					},
					{
						modeId: 3,
						modeType: 'afterSales',
						modeName: '退款/售后'
					}
				],
				pointsOrderTabList: [{
						modeId: 0,
						modeType: 'all',
						modeName: '全部'
					},
					{
						modeId: 1,
						modeType: 'waitPayment',
						modeName: '待付款'
					},
					{
						modeId: 2,
						modeType: 'waitDelivered',
						modeName: '待发货'
					},
					{
						modeId: 3,
						modeType: 'waitReceived',
						modeName: '待收货'
					},
					{
						modeId: 4,
						modeType: 'afterSales',
						modeName: '退款/售后'
					}
				],
				list: [],
				mallList: [],
				pageNo: 1,
				isLoading: false,
				isEndList: false
			};
		},
		computed: {
			activeSubTabs() {
				return this.currentTab == 0 ? this.shopOrderTabList : this.pointsOrderTabList;
			},
			activeList() {
				return this.currentTab == 0 ? this.list : this.mallList;
			}
		},
		onLoad: function(options) {
			// 首次打开：兼容旧版 URL 带参入口
			this.applyEntry(options || {});
		},
		onShow: function() {
			// tabBar 页后续每次进入只触发 onShow：消费「我的」页经 globalData 传入的入口状态
			const app = getApp();
			const entry = app.globalData.orderCenterEntry;
			if (entry) {
				app.globalData.orderCenterEntry = null;
				this.applyEntry(entry);
			}
			authService.checkIsLogin().then((result) => {
				if (result) {
					this.reload();
					this.refreshTabNumbers();
					return;
				}
				app.globalData.checkIsAuth('scope.userInfo');
			});
		},
		onPullDownRefresh: function() {
			this.reload();
			uni.stopPullDownRefresh();
		},
		onReachBottom: function() {
			this.loadMore();
		},
		onShareAppMessage: function() {},
		methods: {
			// 解析入口状态：{ currentTab, modeType, currentOrderTab }
			applyEntry(entry) {
				this.currentTab = Number(entry.currentTab) || 0;
				const tabs = this.currentTab == 0 ? this.shopOrderTabList : this.pointsOrderTabList;
				let idx = 0;
				if (entry.modeType) {
					const found = tabs.findIndex((t) => t.modeType == entry.modeType);
					if (found >= 0) idx = found;
				} else if (entry.currentOrderTab !== undefined) {
					idx = Number(entry.currentOrderTab) || 0;
				}
				this.currentOrderTab = idx;
			},
			reload() {
				this.pageNo = 1;
				this.isEndList = false;
				this.fetchList(false);
			},
			loadMore() {
				if (this.isEndList || this.isLoading) return;
				this.pageNo++;
				this.fetchList(true);
			},
			fetchList(append) {
				if (this.isLoading) return;
				this.isLoading = true;
				const isShop = this.currentTab == 0;
				const url = isShop ? '/rest/member/order/list' : '/rest/member/pointsMall/order/list';
				https.request(url, {
					pageNo: this.pageNo,
					pageSize: PAGE_SIZE,
					tabType: this.activeSubTabs[this.currentOrderTab].modeType,
					keyWords: ''
				}).then((result) => {
					this.isLoading = false;
					if (!result.success) return;
					const records = result.data.records || [];
					records.forEach((res) => {
						res.createTime = dateHelper.fmtDate(res.createTime);
						res.statusText = isShop ? systemStatus.statusText(res.status) : systemStatus
							.statusMallText(res.status);
						res.statusActive = ACTIVE_STATUSES.indexOf(Number(res.status)) != -1;
					});
					this.isEndList = records.length < PAGE_SIZE;
					if (isShop) {
						this.list = append ? this.list.concat(records) : records;
					} else {
						this.mallList = append ? this.mallList.concat(records) : records;
					}
				});
			},
			clickTab(index) {
				if (this.currentTab === index) return;
				this.currentTab = index;
				this.currentOrderTab = 0;
				this.reload();
			},
			clickOrderTab(index) {
				if (this.currentOrderTab === index) return;
				this.currentOrderTab = index;
				this.reload();
			},
			refreshTabNumbers() {
				https.request('/rest/member/order/selectTabNumber', {}).then((result) => {
					if (result.success) {
						this.shopOrderTabList.forEach((item) => {
							if (item.modeType == 'all') return;
							for (let key in result.data) {
								if (key.indexOf(item.modeType) != -1) {
									item.number = result.data[key];
									break;
								}
							}
						});
					}
				});
				https.request('/rest/member/pointsMall/order/selectTabNumber', {}).then((result) => {
					if (result.success) {
						this.pointsOrderTabList.forEach((item) => {
							if (item.modeType == 'all') return;
							for (let key in result.data) {
								if (key.indexOf(item.modeType) != -1) {
									item.number = result.data[key];
									break;
								}
							}
						});
					}
				});
			},
			// C-11 订单卡按钮：待付款[取消/立即支付] 进行中[申请退款/取消订单] 已完成[评价/再来一单]
			cardButtons(item) {
				const s = Number(item.status);
				const btns = [];
				if (s === 1) {
					btns.push({
						key: 'cancel',
						text: '取消订单',
						type: 'stroke'
					});
					btns.push({
						key: 'pay',
						text: '立即支付',
						type: 'primary'
					});
					return btns;
				}
				if (s >= 2 && s <= 5) {
					btns.push({
						key: 'refund',
						text: '申请退款',
						type: 'stroke'
					});
					if (s <= 3) {
						btns.push({
							key: 'cancel',
							text: '取消订单',
							type: 'stroke'
						});
					}
				} else if (s === 6) {
					if (item.isAllowAppraise) {
						btns.push({
							key: 'evaluate',
							text: '评价',
							type: 'stroke'
						});
					}
				}
				if (s === 6 || s === 10 || s === 11) {
					btns.push({
						key: 'reorder',
						text: '再来一单',
						type: 'primary'
					});
				}
				return btns;
			},
			onCardButton(key, item) {
				const s = Number(item.status);
				if (key === 'cancel') {
					if (s === 1) {
						// 未付款：列表页直接取消
						toastService.showModal(null, '确定取消这个订单吗?', () => {
							https.request('/rest/member/order/cancelOrder', {
								id: item.id
							}).then((result) => {
								if (result.success) {
									toastService.showSuccess('取消成功', true);
									this.reload();
									this.refreshTabNumbers();
								}
							});
						}, null);
					} else {
						// 已付款：到详情页走无理由取消（需选原因）
						this.goDetail(item);
					}
					return;
				}
				if (key === 'pay') {
					this.goDetail(item);
					return;
				}
				if (key === 'refund') {
					uni.navigateTo({
						url: '../refund/apply/apply?orderId=' + item.id
					});
					return;
				}
				if (key === 'evaluate') {
					uni.navigateTo({
						url: '../evaluate/evaluate?orderId=' + item.id + '&shopId=' + item.shopId
					});
					return;
				}
				if (key === 'reorder') {
					uni.switchTab({
						url: this.currentTab == 0 ? '/pages/menu/index/index' :
							'/pages/internal/mall/index/index'
					});
				}
			},
			goDetail(item) {
				if (this.currentTab == 0) {
					uni.navigateTo({
						url: '../detail/detail?id=' + item.id
					});
				} else {
					uni.navigateTo({
						url: '../../internal/mall/order/detail/detail?id=' + item.id
					});
				}
			},
			goDrink() {
				uni.switchTab({
					url: this.currentTab == 0 ? '/pages/menu/index/index' :
						'/pages/internal/mall/index/index'
				});
			}
		}
	};
</script>
<style>
	page {
		background: #f5f5f5;
		width: 100%;
		margin: 0;
	}

	.page {
		min-height: 100vh;
		background: #f5f5f5;
		padding-bottom: 40rpx;
	}

	/* ===== 顶部分栏Tabs（C-11 163:71） ===== */
	.top-tabs {
		position: sticky;
		top: 0;
		z-index: 10;
	}

	.main-tabs {
		display: flex;
		background: #ffffff;
		height: 88rpx;
	}

	.main-tab-item {
		flex: 1;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
	}

	.main-tab-text {
		font-size: 28rpx;
		color: #666666;
	}

	.main-tab-text.active {
		color: #5f7d22;
		font-weight: 500;
	}

	.tab-indicator {
		width: 48rpx;
		height: 6rpx;
		border-radius: 4rpx;
		background: #5f7d22;
		margin-top: 8rpx;
	}

	/* ===== 状态子Tab ===== */
	.sub-tabs {
		background: #ffffff;
		border-top: 1rpx solid #f0f0f0;
	}

	.sub-tab-scroll {
		width: 100%;
		white-space: nowrap;
	}

	.sub-tab-list {
		display: inline-flex;
		padding: 0 12rpx;
	}

	.sub-tab-item {
		display: inline-flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		height: 76rpx;
		padding: 0 26rpx;
		position: relative;
	}

	.sub-tab-inner {
		position: relative;
	}

	.sub-tab-text {
		font-size: 24rpx;
		color: #666666;
	}

	.sub-tab-text.active {
		color: #5f7d22;
		font-weight: 500;
	}

	.sub-indicator {
		position: absolute;
		bottom: 4rpx;
		left: 50%;
		margin-left: -16rpx;
		width: 32rpx;
		height: 4rpx;
		border-radius: 2rpx;
		background: #5f7d22;
	}

	.badge {
		position: absolute;
		top: -10rpx;
		right: -24rpx;
		min-width: 28rpx;
		height: 28rpx;
		line-height: 28rpx;
		padding: 0 6rpx;
		box-sizing: border-box;
		border-radius: 999rpx;
		background-color: #ee0a24;
		font-size: 18rpx;
		color: #ffffff;
		text-align: center;
	}

	/* ===== 订单卡（C-11 163:78/163:89） ===== */
	.order-list {
		padding: 24rpx;
	}

	.order-card {
		background: #ffffff;
		border-radius: 24rpx;
		padding: 24rpx;
		margin-bottom: 20rpx;
	}

	.card-hover {
		opacity: 0.85;
	}

	.card-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.shop-name {
		font-size: 28rpx;
		font-weight: 600;
		color: #333333;
		flex: 1;
		margin-right: 16rpx;
		overflow: hidden;
		white-space: nowrap;
		text-overflow: ellipsis;
	}

	.status-text {
		font-size: 24rpx;
		flex-shrink: 0;
	}

	.status-text.blue {
		color: #5f7d22;
	}

	.status-text.gray {
		color: #666666;
	}

	.goods-summary {
		display: block;
		font-size: 24rpx;
		color: #999999;
		margin-top: 16rpx;
		overflow: hidden;
		white-space: nowrap;
		text-overflow: ellipsis;
	}

	.money-row {
		display: flex;
		align-items: baseline;
		justify-content: space-between;
		margin-top: 16rpx;
	}

	.money-label {
		font-size: 24rpx;
		color: #999999;
	}

	.money-value {
		font-size: 30rpx;
		font-weight: 700;
		color: #f01414;
	}

	.btn-row {
		display: flex;
		align-items: center;
		justify-content: flex-end;
		margin-top: 20rpx;
	}

	.card-btn {
		min-width: 152rpx;
		height: 56rpx;
		padding: 0 24rpx;
		box-sizing: border-box;
		border-radius: 28rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 24rpx;
	}

	.card-btn + .card-btn {
		margin-left: 16rpx;
	}

	.btn-hover {
		opacity: 0.8;
	}

	.btn-stroke {
		background: #ffffff;
		border: 1rpx solid #e0e0e0;
		color: #666666;
	}

	.btn-primary {
		background: #5f7d22;
		color: #ffffff;
	}

	.loading-box {
		display: flex;
		justify-content: center;
		padding: 24rpx 0;
	}

	.end-tip {
		text-align: center;
		color: #999999;
		font-size: 24rpx;
		padding: 24rpx 0 20rpx;
	}
</style>
