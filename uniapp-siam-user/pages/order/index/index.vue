<template>
	<view class="page">
		<!-- 顶部分栏Tabs（C-11）：状态子Tab（全部/待付款/待自提/退款售后）已按 2026-09-24 拍板移除，
		     只保留「点单订单/商城订单」主 Tab，列表固定拉全量 -->
		<view class="top-tabs">
			<view class="main-tabs">
				<view class="main-tab-item" v-for="(item, index) in tabList" :key="index" @tap="clickTab(index)">
					<!-- ⚠️ 条件类名不能用 active：app.wxss 有全局 .active{border-bottom:4rpx}，会撞出第二道下划线 -->
					<text :class="'main-tab-text ' + (currentTab == index ? 'main-tab-on' : '')">{{ item.modeName }}</text>
					<view class="tab-indicator" v-if="currentTab == index"></view>
				</view>
			</view>
		</view>

		<!-- 订单列表 -->
		<view class="order-list" v-if="activeList.length > 0">
			<template v-for="(item, index) in activeList" :key="item.id">
				<!-- 订单卡·复刻版（2026-09-23 设计稿；09-25 商城订单同步此卡片，删除旧商城分支） -->
				<view class="order-card" hover-class="card-hover" @tap="goDetail(item)">
					<view class="card-header">
						<text class="shop-name">{{ item.shopName }}</text>
						<text class="header-arrow">›</text>
						<text :class="'status-text ' + (item.statusActive ? 'blue' : 'gray')">{{ item.statusText }}</text>
					</view>
					<view class="card-main">
						<view class="thumb-row">
							<view class="thumb-item" v-for="n in thumbCount(item)" :key="n">
								<image class="thumb-img" v-if="thumbSrc(item, n - 1)" :src="thumbSrc(item, n - 1)"
									mode="aspectFill" />
								<view class="thumb-placeholder" v-else></view>
							</view>
						</view>
						<view class="price-col">
							<view class="price-line">
								<text class="price-symbol">¥</text>
								<text class="price-num">{{ fmtPrice(item.actualPrice) }}</text>
							</view>
							<text class="price-count">共{{ totalQty(item) }}件</text>
						</view>
					</view>
					<text class="goods-names" :decode="true">{{ goodsNames(item) }}</text>
					<view class="time-row">
						<text class="time-text">下单: {{ (item.createTime || '').slice(0, 16) }}</text>
					</view>
					<view class="card-footer" v-if="hasMore(item) || cardButtons(item).length > 0">
						<text class="more-link" v-if="hasMore(item)" hover-class="btn-hover"
							@tap.stop="onMore(item)">更多</text>
						<view class="btn-row" v-if="cardButtons(item).length > 0">
							<view v-for="btn in cardButtons(item)" :key="btn.key" :class="'card-btn btn-' + btn.type"
								hover-class="btn-hover" @tap.stop="onCardButton(btn.key, item)">{{ btn.text }}</view>
						</view>
					</view>
				</view>
			</template>
			<view class="loading-box" v-if="isLoading">
				<van-loading />
			</view>
			<view class="end-tip" v-if="!isLoading && isEndList">没有更多啦</view>
		</view>

		<!-- 空状态 -->
		<van-empty v-if="!isLoading && activeList.length <= 0" description="暂无订单">
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
	import GlobalConfig from '../../../utils/global-config';

	const PAGE_SIZE = 10;
	// 进行中状态（头行状态字标蓝），其余（已完成/售后/取消）标灰
	const ACTIVE_STATUSES = [1, 2, 3, 4, 5];

	export default {
		data() {
			return {
				currentTab: 0,
				tabList: [{
						modeId: 0,
						modeName: '点单订单'
					},
					{
						modeId: 1,
						modeName: '商城订单'
					}
				],
				// 状态子Tab（全部/待付款/待自提/退款售后 等）已按 2026-09-24 拍板移除：
				// 列表固定 tabType='all' 拉全量，后端 tabType 口径见 OrderMapper（waitPayment=status1、
				// waitPickedUp=status2/3、afterSales=7/8/9/11），后续若恢复筛选直接复用该口径
				list: [],
				mallList: [],
				pageNo: 1,
				isLoading: false,
				isEndList: false
			};
		},
		computed: {
			activeList() {
				return this.currentTab == 0 ? this.list : this.mallList;
			}
		},
		onLoad: function(options) {
			// 首次打开：兼容旧版 URL 带参入口
			this.applyEntry(options || {});
		},
		onShow: function() {
			// tabBar 页后续每次进入只触发 onShow：消费经 globalData 传入的入口状态。
			// 状态子Tab已移除（09-24 拍板），旧入口参数里的 modeType/currentOrderTab 不再消费
			const app = getApp();
			const entry = app.globalData.orderCenterEntry;
			if (entry) {
				app.globalData.orderCenterEntry = null;
				this.applyEntry(entry);
			}
			authService.checkIsLogin().then((result) => {
				if (result) {
					this.reload();
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

			/**
			 * 出品小标（2026-09-23 新增）：仅「进行中」订单展示。
			 * 口径：status ∈ {2,3,4}（已支付未终态）时，按出品状态给标：
			 *   production_status=2 → 出品中；=3 → 已出餐·可自取（自提）/ 已出品（堂食）
			 * ⚠️ 列表接口返回的是**下划线键 `production_status`**（`select o.*` 的 Map 原样列名），
			 *    与同一响应里的驼峰键（queueNo / status）**混用** —— 别读成 productionStatus（会拿到 undefined）。
			 */
			prodBadge(item) {
				const s = Number(item.status);
				if ([2, 3, 4].indexOf(s) === -1) return '';
				const p = Number(item.production_status);
				if (p === 2) return '出品中';
				if (p === 3) {
					const isTakeaway = !item.tableName || !String(item.tableName).trim();
					return isTakeaway ? '已出餐 · 可自取' : '已出品';
				}
				return '';
			},
			// 解析入口状态：只保留主 Tab（点单/商城）；旧参数 modeType/currentOrderTab 兼容但不消费
			applyEntry(entry) {
				this.currentTab = Number(entry.currentTab) || 0;
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
					tabType: 'all',
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
				this.reload();
			},
		// 订单卡按钮（2026-09-25 商城同步复刻 + grill 裁决）：两套 tab 按钮矩阵完全一致
		// 待付款[立即支付] 进行中[更多+再来一单] 已完成[评价/再来一单] 已取消[再来一单]
		// ⚠️ 卡片不放「取消订单」——取消是申请式动作，入口统一在详情页（grill Round 3 裁决，CONTEXT.md 有词条）
		cardButtons(item) {
			const s = Number(item.status);
			const btns = [];
			if (s === 1) {
				btns.push({
					key: 'pay',
					text: '立即支付',
					type: 'primary'
				});
				return btns;
			}
			if (s === 6 && item.isAllowAppraise) {
				btns.push({
					key: 'evaluate',
					text: '评价',
					type: 'stroke'
				});
			}
			// 进行中(s2-5)与已完成/已取消都带「再来一单」（描边胶囊，设计稿口径）
			const showReorder = s === 6 || s === 10 || s === 11 || (s >= 2 && s <= 5);
			if (showReorder) {
				btns.push({
					key: 'reorder',
					text: '再来一单',
					type: 'strokeGreen'
				});
			}
			return btns;
		},
		// 「更多」入口：进行中订单（点单 s2-5 / 商城 s4-5），申请退款收在里面（09-25 拍板商城同步）
		hasMore(item) {
			const s = Number(item.status);
			return s >= 2 && s <= 5;
		},
		onMore(item) {
			uni.showActionSheet({
				itemList: ['申请退款'],
				itemColor: '#333333',
				success: (res) => {
					if (res.tapIndex === 0) {
						// 商城单走商城退款页（参数同名 orderId），别跳到点单退款页
						const url = this.currentTab == 0
							? '../refund/apply/apply?orderId=' + item.id
							: '../../internal/mall/order/refund/apply/apply?orderId=' + item.id;
						uni.navigateTo({
							url: url
						});
					}
				}
			});
		},
		// ===== 复刻卡片数据辅助 =====
		// 共N件：tb_order.goods_total_quantity（列表 SQL 为 select o.*，Map 键驼峰/下划线都兜住）
		totalQty(item) {
			const raw = item.goodsTotalQuantity != null ? item.goodsTotalQuantity : item.goods_total_quantity;
			const q = Number(raw);
			return q > 0 ? q : 1;
		},
		thumbCount(item) {
			// 设计稿一行最多 4 张缩略图；有真实明细时按明细条数（同商品×N 算一张），否则兜底件数
			const list = item.orderDetailList;
			if (list && list.length > 0) return Math.min(list.length, 4);
			return Math.min(Math.max(this.totalQty(item), 1), 4);
		},
		// 商品缩略图：列表接口已附 orderDetailList（2026-09-25 后端批量补）；mainImage 为相对路径，拼 ossUrl 前缀
		thumbSrc(item, idx) {
			const list = item.orderDetailList;
			const d = list && list[idx];
			if (d && d.mainImage) {
				return d.mainImage.indexOf('http') === 0 ? d.mainImage : GlobalConfig.ossUrl + d.mainImage;
			}
			return '';
		},
		// 商品名行：优先明细逐项名称（设计稿口径），无明细时兜底 description 摘要
		goodsNames(item) {
			const list = item.orderDetailList;
			if (list && list.length > 0) {
				return list.map((d) => d.goodsName).filter(Boolean).join('  ');
			}
			return item.description || '';
		},
		// ¥19.8：去掉多余尾零（"19.80" → 19.8）
		fmtPrice(price) {
			const n = Number(price);
			return isNaN(n) ? price : String(n);
		},
			onCardButton(key, item) {
				// 「取消订单」已从卡片下线（grill Round 3 裁决：申请式动作，入口统一详情页），cancel 分支一并废弃
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
		/* 状态子Tab行已移除（09-24 拍板），分隔线收进主 Tab 行自身 */
		border-bottom: 1rpx solid #f0f0f0;
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

	/* 激活态类名用 main-tab-on（不用全局 active，避免撞出第二道下划线，见模板注释） */
	.main-tab-text.main-tab-on {
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

	/* ===== 状态子Tab样式已随该行一并移除（09-24 拍板） ===== */

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

	/* 出品小标（2026-09-23） */
	.prod-badge {
		align-self: flex-start;
		font-size: 22rpx;
		color: #5f7d22;
		background: #EDF2DE;
		border-radius: 8rpx;
		padding: 4rpx 12rpx;
		margin-top: 12rpx;
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

	.btn-strokeGreen {
		background: #ffffff;
		border: 1rpx solid #5f7d22;
		color: #5f7d22;
	}

	/* ===== 订单卡·复刻版（2026-09-23 设计稿「订单卡片·复刻」） ===== */
	.header-arrow {
		font-size: 28rpx;
		color: #999999;
		margin: 0 8rpx 0 4rpx;
		flex-shrink: 0;
	}

	.card-main {
		display: flex;
		align-items: center;
		margin-top: 20rpx;
	}

	.thumb-row {
		flex: 1;
		display: flex;
		overflow: hidden;
	}

	.thumb-item {
		width: 120rpx;
		height: 120rpx;
		border-radius: 16rpx;
		overflow: hidden;
		flex-shrink: 0;
		margin-right: 12rpx;
	}

	.thumb-img {
		width: 100%;
		height: 100%;
		display: block;
	}

	.thumb-placeholder {
		width: 100%;
		height: 100%;
		background: #f5f1e8;
	}

	.price-col {
		flex-shrink: 0;
		margin-left: 8rpx;
		display: flex;
		flex-direction: column;
		align-items: flex-end;
	}

	.price-line {
		display: flex;
		align-items: baseline;
	}

	.price-symbol {
		font-size: 24rpx;
		font-weight: 700;
		color: #1a1a1a;
	}

	.price-num {
		font-size: 40rpx;
		font-weight: 700;
		color: #1a1a1a;
		line-height: 1;
	}

	.price-count {
		font-size: 22rpx;
		color: #999999;
		margin-top: 8rpx;
	}

	.goods-names {
		display: block;
		font-size: 24rpx;
		color: #666666;
		margin-top: 16rpx;
		overflow: hidden;
		white-space: nowrap;
		text-overflow: ellipsis;
	}

	.time-row {
		margin-top: 20rpx;
	}

	.time-text {
		font-size: 22rpx;
		color: #999999;
	}

	.card-footer {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-top: 16rpx;
		min-height: 56rpx;
	}

	.card-footer .btn-row {
		margin-top: 0;
	}

	.more-link {
		font-size: 26rpx;
		color: #666666;
		padding: 8rpx 16rpx 8rpx 0;
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
