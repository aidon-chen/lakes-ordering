<template>
	<view>
		<scroll-view :scroll-y="true" :style="'height:' + winHeight + 'px;'">
			<checkbox-group class="cart-group" @change="checkboxChange">
				<view class="cart-list">
					<view class="cart-cell" v-for="(item, index) in items" :key="index">
						<van-swipe-cell :right-width="65">
							<view :class="'cart-item ' + (item.disable ? 'cart-item-disabled' : '')">
								<view class="cart-check">
									<checkbox :value="index" :index="index" class="theme-color theme-border-color"
										:checked="item.checked" :disabled="item.disable" iconColor="#FFFFFF" />
								</view>

								<image v-if="item.mainImage" :src="item.mainImage" mode="aspectFill" class="cart-img"
									@tap="commodityDetailTap" :data-id="item.goodsId"></image>
								<view v-else class="cart-img cart-img-empty" @tap="commodityDetailTap"
									:data-id="item.goodsId"></view>

								<view class="cart-info">
									<view
										:class="'cart-name out_of_range one_row ' + (item.disable ? 'cart-name-disabled' : '')">
										{{ item.goodsName }}
									</view>
									<view class="cart-spec out_of_range one_row" v-if="item.restructure">{{ item.restructure }}
									</view>
									<view class="cart-bottom">
										<view :class="'cart-price ' + (item.disable ? 'cart-price-disabled' : '')">
											<text class="price-symbol">￥</text>
											<text class="price-value">{{ item.price }}</text>
										</view>
										<block v-if="!item.disable">
											<view class="cart-stepper">
												<view class="step-btn flex_center" @tap.stop.prevent="changeNumber(index, -1)">－
												</view>
												<text class="step-num">{{ item.number }}</text>
												<view class="step-btn flex_center" @tap.stop.prevent="changeNumber(index, 1)">＋
												</view>
											</view>
										</block>
										<view class="cart-sold-out" v-else>已售罄</view>
									</view>
								</view>
							</view>

							// #ifdef APP-PLUS||H5
							<template #right>
								<view class="swipe-del flex_center"
									@tap="slideButtonTap($event, { checkboxIndex: index })">
									<van-icon name="delete-o" class="swipe-del-icon" />
									<text class="swipe-del-text">删除</text>
								</view>
							</template>
							// #endif
							// #ifdef MP-WEIXIN||MP-ALIPAY
							<view slot="right" class="swipe-del flex_center" :data-checkboxIndex="index"
								@tap="slideButtonTap($event, { checkboxIndex: index })">
								<van-icon name="delete-o" class="swipe-del-icon" />
								<text class="swipe-del-text">删除</text>
							</view>
							// #endif
						</van-swipe-cell>
					</view>
				</view>
			</checkbox-group>

			<van-empty v-if="items.length <= 0" description="您的购物车有点寂寞"></van-empty>
			<view class="gotogg theme-bg" @tap="goToDrink" v-if="items.length <= 0">去逛逛</view>

			<!-- 猜你喜欢：画板 B-03 为 3 列小卡（111.67×144） -->
			<view class="like-list-view" v-if="likeList.length > 0">
				<view class="title-like-view theme-color">
					<view class="title-view">猜你喜欢</view>
				</view>
				<view class="like-items">
					<view class="like-card" v-for="(item, index) in likeList" :key="index" @tap="commodityDetailTap"
						:data-id="item.id">
						<image v-if="item.mainImage" :src="item.mainImage" mode="aspectFill" class="like-card-img"></image>
						<view v-else class="like-card-img like-card-img-empty"></view>
						<view class="like-card-name out_of_range one_row">{{ item.name }}</view>
						<view class="like-card-sub out_of_range one_row">{{ item.detail }}</view>
						<view class="like-card-bottom">
							<view class="like-card-price">
								<text class="price-symbol">￥</text>
								<text class="like-card-price-value">{{ item.price }}</text>
							</view>
							<view class="like-card-add theme-bg">+</view>
						</view>
					</view>
				</view>
			</view>
		</scroll-view>

		<!-- 结算栏：全选 + 合计 + 去结算(件数)（画板 B-03） -->
		<view class="settlement-view" v-if="items.length > 0">
			<view class="settle-left" @tap="toggleSelectAll">
				<view :class="'settle-check ' + (isAllChecked ? 'theme-bg' : 'settle-check-plain')">
					<van-icon name="success" v-if="isAllChecked" class="settle-check-icon" />
				</view>
				<text class="settle-all-text">全选</text>
			</view>
			<view class="settle-right">
				<view class="settle-total">
					<text class="settle-total-label">合计</text>
					<text class="settle-total-symbol">￥</text>
					<text class="settle-total-money">{{ totalPrice }}</text>
				</view>
				<view class="to-pay-view theme-bg" hover-class="hover-class-public" @tap="goToPay">
					去结算({{ checkedNum }})
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	import GlobalConfig from '../../../../utils/global-config';
	import https from '../../../../utils/http';
	import authService from '../../../../utils/auth';
	import toastService from '../../../../utils/toast.service';
	let app = null;
	var winHeight = 0;
	export default {
		data() {
			return {
				items: [],
				totalPrice: '0.00',
				winHeight: '',
				packingCharges: 0,
				fullPriceReduction: '',
				fullReductionRuleName: '',
				fullPriceReductionIsHidden: false,
				likeList: []
			};
		},
		computed: {
			// 选中件数（结算按钮「去结算(N)」）
			checkedNum() {
				return (this.items || []).reduce((sum, it) => sum + (it.checked ? it.number : 0), 0);
			},
			// 是否全选（失效商品不参与判定）
			isAllChecked() {
				const valid = (this.items || []).filter((it) => !it.disable);
				return valid.length > 0 && valid.every((it) => it.checked);
			}
		},
		onLoad: function(option) {

		},
		onShow: function() {
			app = getApp();
			toastService.showLoading();
			authService.checkIsLogin().then((result) => {
				this.getLoveIt();
				if (result) {
					this.getShoppingCartList();
					return;
				}
				toastService.hideLoading();
				this.items = [];
				this.totalPrice = '0.00';
				app.globalData.checkIsAuth('scope.userInfo');
			});
		},
		methods: {
			/**
			 * 统一重算：金额 / 包装费 / 角标 / 满减。
			 * C-10 批次6：原实现把这段逻辑散在 initData / checkboxChange / 点击减号 / 点击加号 四处，
			 * 极易算错；且 vue3 运行时未挂载小程序数据刷新方法（调用即抛错并中断后续语句）→ 统一为响应式赋值。
			 */
			recalc(skipFullReduction) {
				let total = 0;
				let packing = 0;
				let num = 0;
				(this.items || []).forEach((it) => {
					if (it.checked) {
						total += it.price * it.number;
						packing += (it.packingCharges || 0) * it.number;
						num += it.number;
					}
				});
				this.totalPrice = total.toFixed(2);
				this.packingCharges = packing;
				this.setTabBarBadge(num);
				this.items = this.items.slice();
				if (!skipFullReduction) {
					this.getFullReductionRule(total, packing);
				}
			},

			/** 初始化：默认全选（失效商品不可选） */
			initData() {
				(this.items || []).forEach((it) => {
					it.checked = !it.disable;
				});
				this.recalc();
				this.getAutoHeight();
			},

			getAutoHeight() {
				var _this = this;
				uni.getSystemInfo({
					success: function(res) {
						var h = res.windowHeight;
						var height = 0;
						setTimeout(function timeout() {
							uni.createSelectorQuery()
								.in(_this)
								.selectAll('.settlement-view')
								.boundingClientRect(function(rects) {
									rects.forEach(function(rect, index) {
										height = height + rect.height;
									});
									if (rects.length > 0) {
										h = h - height;
									}
									_this.winHeight = h;
								}).exec();
						}, 500);
					}
				});
			},

			/** 全选 / 取消全选 */
			toggleSelectAll() {
				const next = !this.isAllChecked;
				(this.items || []).forEach((it) => {
					if (!it.disable) {
						it.checked = next;
					}
				});
				this.recalc();
			},

			/** 左滑删除 */
			slideButtonTap(e) {
				var that = this;
				const idx = e.currentTarget.dataset.checkboxindex;
				const item = this.items[idx];
				if (!item) {
					return;
				}
				toastService.showModal(null, '确定不要这个了吗？', function confirm() {
					that.updateNumber(item.id, item.number, 0, function callback() {
						that.items.splice(idx, 1);
						that.recalc();
						that.getAutoHeight();
					});
				}, null);
			},

			checkboxChange(e) {
				const values = e.detail.value;
				(this.items || []).forEach((it, i) => {
					it.checked = values.indexOf(String(i)) > -1;
				});
				this.recalc();
			},

			/** 步进器：加 / 减（原「点击减号/加号」两个方法合并，减到 0 时确认删除） */
			changeNumber(index, delta) {
				const item = this.items[index];
				if (!item || item.disable) {
					return;
				}
				if (!item.checked) {
					toastService.showToast('未选择商品');
					return;
				}
				if (delta < 0 && item.number <= 1) {
					var that = this;
					toastService.showModal(null, '确定不要这个了吗？', function confirm() {
						that.updateNumber(item.id, 1, 0, function callback() {
							that.items.splice(index, 1);
							that.recalc();
							that.getAutoHeight();
						});
					}, null);
					return;
				}
				const next = item.number + delta;
				item.number = next;
				this.updateNumber(item.id, Math.abs(delta), delta > 0 ? 1 : 0, () => {
					this.recalc();
				});
			},

			setTabBarBadge(num) {
				if (app && app.globalData && app.globalData.setTabBarBadge) {
					app.globalData.setTabBarBadge(num);
				}
			},

			goToPay() {
				const list = this.items;
				const orderDetail = {};
				orderDetail.actualPrice = this.totalPrice;
				orderDetail.packingCharges = this.packingCharges;
				orderDetail.orderDetailList = [];
				let isTap = false;
				for (let key in list) {
					if (list[key].checked) {
						isTap = true;
						orderDetail.orderDetailList.push({
							goodsId: list[key].goodsId,
							specList: list[key].specList,
							number: list[key].number,
							goodsName: list[key].goodsName,
							restructure: list[key].restructure,
							price: list[key].price,
							id: list[key].id,
							packingCharges: list[key].packingCharges
						});
					}
				}
				if (isTap) {
					uni.navigateTo({
						url: '../pay/pay?orderDetail=' + JSON.stringify(orderDetail) + '&payType=car'
					});
					return;
				}
				this.goToTap();
			},

			goToDrink() {
				uni.switchTab({
					url: '../../mall/index/index'
				});
			},

			commodityDetailTap(e) {
				uni.navigateTo({
					url: '../detail/detail?id=' + e.currentTarget.dataset.id
				});
			},

			getShoppingCartList() {
				https.request('/rest/member/pointsMall/shoppingCart/list', {
					pageNo: -1,
					pageSize: 20
				}, authService.getToken()).then((result) => {
					if (result.success) {
						const records = (result.data && result.data.records) || [];
						records.forEach((item) => {
							// 种子商品 main_image 可能为空 → 交给模板渲染灰底占位
							if (item.mainImage) {
								item.mainImage = GlobalConfig.ossUrl + item.mainImage;
							}
							let specList = '';
							let specMap = {};
							try {
								specMap = JSON.parse(item.specList) || {};
							} catch (e) {
								specMap = {};
							}
							for (let key in specMap) {
								specList = (specList ? specList + '/' : specList) + specMap[key];
							}
							item.restructure = specList;
							item.disable = item.goodsStatus == 1 || item.goodsStatus == 3 || item.goodsStatus == 4;
						});
						this.items = records;
						this.initData();
					}
				});
			},

			/** 满减规则（金额口径沿用工程现状：totalPrice + packingCharges 达门槛） */
			getFullReductionRule(total, packing) {
				const totalPrice = total === undefined ? Number(this.totalPrice) : total;
				const packingCharges = packing === undefined ? this.packingCharges : packing;
				https.request('/rest/pointsMall/fullReductionRule/list', {
					pageNo: -1,
					pageSize: 1
				}).then((result) => {
					if (result.success) {
						let fullPriceReduction = 0;
						let fullReductionRuleName = '';
						let fullPriceReductionIsHidden = false;
						let limitedPrice = 0;
						for (let i = 0; i < result.data.records.length; i++) {
							if (totalPrice + packingCharges >= result.data.records[i].limitedPrice) {
								if (result.data.records[i].limitedPrice > limitedPrice) {
									limitedPrice = result.data.records[i].limitedPrice;
									fullPriceReduction = totalPrice - result.data.records[i].reducedPrice;
									fullReductionRuleName = result.data.records[i].name;
									fullPriceReductionIsHidden = true;
								}
							}
						}
						this.fullPriceReduction = fullPriceReduction;
						this.fullReductionRuleName = fullReductionRuleName;
						this.fullPriceReductionIsHidden = fullPriceReductionIsHidden;
					}
				});
			},

			/** 猜你喜欢（画板为 3 列小卡，取前 6 条） */
			getLoveIt() {
				toastService.showLoading();
				https.request('/rest/pointsMall/goods/guessLikeGoodsList', {}).then((result) => {
					toastService.hideLoading();
					if (result.success) {
						const list = (result.data || []).slice(0, 6);
						list.forEach(function(item) {
							if (item.mainImage) {
								item.mainImage = GlobalConfig.ossUrl + item.mainImage;
							}
						});
						this.likeList = list;
					}
				});
			},

			updateNumber(id, number, type, callbak) {
				https.request('/rest/member/pointsMall/shoppingCart/updateNumber', {
					id: id,
					number: number,
					type: type
				}, authService.getToken()).then((result) => {
					if (result.success) {
						if (callbak) {
							callbak();
						}
					} else {
						toastService.showToast(result.message || '操作失败');
					}
				});
			},

			goToTap() {
				toastService.showModal(null, '至少购买一件商品', null, null, false);
			}
		}
	};
</script>
<style>
	page {
		background-color: #f7f7f7;
		padding-top: 10rpx;
	}

	/* .weui-slidecells {
  margin-top: 10rpx;
} */

	.weui-slidecell {
		display: flex;
		justify-content: space-between;
		margin: 10rpx 20rpx 10rpx 20rpx;
		background-color: #fff;
		border-radius: 8px;
		padding: 20rpx 15rpx;
		line-height: 1.4;
		font-size: 17px;
		color: rgba(0, 0, 0, 0.9);
	}

	.is-first {
		margin-top: 20rpx;
	}

	.commdity-item {
		display: flex;
		align-items: center;
		justify-content: flex-start;
		width: 100%;
	}

	.commodity-icon {
		width: 25%;
		height: 108rpx;
		margin-right: 20rpx;
		border-radius: 10rpx;
	}

	.commdity-types {
		display: flex;
		flex-direction: column;
		justify-content: space-between;
		width: 72%;
	}

	.commodity-name-type {
		margin-bottom: 5rpx;
	}

	.commodity-image {
		width: 190rpx;
		height: auto;
		border-radius: 5rpx;
	}

	.checkbox-group-label {
		display: flex;
		align-items: center;
		width: 75%;
	}

	.commdity-name {
		font-size: 26rpx;
		font-weight: bold;
	}

	.types {
		font-size: 22rpx;
		color: #858585;
	}

	.commdity-money-add-subtract {
		width: 50%;
		display: flex;
		flex-direction: column;
	}

	.commdity-money {
		font-size: 26rpx;
		font-weight: bold;
		display: flex;
		align-items: center;
	}

	/*主容器*/
	.stepper {
		margin-left: 20rpx;
	}

	/*加号和减号*/
	.stepper text {
		width: 40rpx;
		height: 40rpx;
		line-height: 40rpx;
		font-size: 28rpx;
	}

	/*数值*/
	.stepper input {
		width: 30px;
	}

	/* 底部去支付样式 */
	.settlement-view {
		position: fixed;
		bottom: 0;
		width: 100%;
		display: flex;
		justify-content: space-between;
		z-index: 66;
		box-shadow: -2px 0px 5px 0.5px rgba(0, 0, 0, 0.1);
		background: white;
	}

	.total-payable-view {
		width: 65%;
		background: #fff;
		padding-left: 30rpx;
		display: flex;
		flex-direction: column;
	}

	.total-payable {
		display: flex;
		align-items: center;
		padding: 10rpx 0;
	}

	.baozhuangfei-class {
		font-size: 24rpx;
		display: flex;
		align-items: center;
		margin-bottom: 10rpx;
	}

	.total-payable-money-view {
		margin-left: 20rpx;
	}

	.to-pay-view {
		/* width: 25%; */
		font-size: 28rpx;
		color: white;
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 0 10rpx;
	}

	.title-text {
		font-size: 28rpx;
		color: #858585;
	}

	.sign-icon {
		font-size: 32rpx;
		font-weight: bold;
	}

	.total-payable-money {
		font-size: 34rpx;
		font-weight: bold;
	}

	/* 猜你喜欢样式 */

	.like-list-view {
		padding: 20rpx;
	}

	.title-like-view {
		display: flex;
		align-items: center;
		justify-content: center;
		margin-bottom: 20rpx;
	}

	.title-view {
		font-size: 30rpx;
		font-weight: bold;
	}

	.icon-font-view {
		display: flex;
		align-items: center;
		font-size: 28rpx;
	}

	.icon-change-class {
		width: 36rpx;
		height: auto;
		margin-right: 10rpx;
	}

	.icon-like-class {
		width: 100%;
		height: 100%;
		border-radius: 15rpx 15rpx 0 0;
	}

	.like-items {
		width: 100%;
		display: grid;
		grid-template-columns: 1fr 1fr 1fr;
		grid-row-gap: 10px;
		grid-column-gap: 10px;
	}

	.like-item {
		height: 25vh;
		display: flex;
		justify-content: center;
		align-items: center;
		flex-direction: column;
		background: white;
		border-radius: 15rpx;
		margin-bottom: 15rpx;
	}

	.item-two {
		margin: 0 3.5%;
	}

	.like-detail-view {
		width: 100%;
		height: 100%;
		display: flex;
		justify-content: center;
		align-items: center;
		flex-direction: column;
	}

	.fullname-class {
		margin-top: 11rpx;
		font-size: 26rpx;
		width: 90%;
	}

	.engname-class {
		font-size: 24rpx;
		color: #ccc;
		width: 90%;
	}

	.like-money-view {
		width: 90%;
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 15rpx 0 10rpx 0;
	}

	.like-money {
		font-size: 26rpx;
		font-weight: bold;
	}

	.plus-view {
		font-size: 28rpx;
		width: 38rpx;
		height: 38rpx;
		line-height: 38rpx;
		text-align: center;
		border-radius: 50%;
		color: white;
	}

	.out-store {
		left: 13.8%;
		font-size: 22rpx;
		height: 80rpx;
		line-height: 80rpx;
		width: 80rpx;
	}

	.full-reduction-view {
		font-size: 20rpx;
		font-weight: bold;
		padding: 0 10rpx;
		margin: 0 5rpx;
		border-radius: 10rpx;
	}

	.gotogg {
		text-align: center;
		margin: 0 35% 15% 35%;
		border-radius: 50rpx;
		padding: 10rpx 0;
		font-size: 30rpx;
	}

	.flvan-swipe-cell__right {
		color: red;
		width: 100%;
		height: 100rpx;
		line-height: 100rpx;
		font-size: 48rpx;
		width: 100rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		background-color: white;
		border-radius: 50%;
	}

	.van-swipe-cell__right {
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.settlement-view {
		bottom: calc(var(--window-bottom));
	}

	/* ==========================================================================
	   C-10 批次 6：购物车条目卡 351×88 + 3 列猜你喜欢 + 结算栏（画板 B-03）
	   追加覆盖式：原 style 段保留，仅覆盖必要类
	   ========================================================================== */
	.cart-list {
		padding: 20rpx 0 10rpx 0;
	}

	.cart-cell {
		margin: 0 24rpx 20rpx 24rpx;
		border-radius: 24rpx;
		overflow: hidden;
		background-color: #ffffff;
	}

	.cart-item {
		display: flex;
		align-items: center;
		height: 176rpx;
		padding: 0 16rpx;
		box-sizing: border-box;
		background-color: #ffffff;
	}

	.cart-item-disabled {
		background-color: #fafafa;
	}

	.cart-check {
		flex-shrink: 0;
		width: 48rpx;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.cart-check checkbox {
		transform: scale(0.6);
	}

	.cart-img {
		width: 144rpx;
		height: 144rpx;
		border-radius: 16rpx;
		flex-shrink: 0;
	}

	.cart-img-empty {
		background-color: #f2f2f2;
	}

	.cart-info {
		flex: 1;
		min-width: 0;
		padding-left: 20rpx;
	}

	.cart-name {
		font-size: 30rpx;
		color: #202020;
		line-height: 42rpx;
	}

	.cart-name-disabled {
		color: #999999;
	}

	.cart-spec {
		font-size: 26rpx;
		color: #999999;
		line-height: 36rpx;
		margin-top: 2rpx;
	}

	.cart-bottom {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-top: 6rpx;
	}

	.cart-price {
		display: flex;
		align-items: baseline;
	}

	.cart-price-disabled {
		color: #999999;
	}

	.cart-price .price-symbol {
		font-size: 22rpx;
		font-weight: bold;
	}

	.cart-price .price-value {
		font-size: 30rpx;
		font-weight: bold;
	}

	.cart-price-disabled .price-symbol,
	.cart-price-disabled .price-value {
		color: #999999;
	}

	.cart-sold-out {
		font-size: 20rpx;
		color: #999999;
		background-color: #efefef;
		border-radius: 8rpx;
		padding: 4rpx 12rpx;
	}

	.cart-stepper {
		display: flex;
		align-items: center;
	}

	.cart-stepper .step-btn {
		width: 52rpx;
		height: 52rpx;
		line-height: 52rpx;
		text-align: center;
		border: 1rpx solid #ededed;
		border-radius: 8rpx;
		font-size: 26rpx;
		color: #333333;
	}

	.cart-stepper .step-num {
		width: 72rpx;
		text-align: center;
		font-size: 28rpx;
		font-weight: 500;
		color: #202020;
	}

	/* 左滑删除区：65×88 红底 + 垃圾桶 + 「删除」 */
	.swipe-del {
		width: 130rpx;
		height: 176rpx;
		background-color: #ee0a24;
		flex-direction: column;
		color: #ffffff;
	}

	.swipe-del-icon {
		font-size: 40rpx;
		color: #ffffff;
	}

	.swipe-del-text {
		font-size: 22rpx;
		color: #ffffff;
		margin-top: 6rpx;
	}

	/* 猜你喜欢：3 列小卡（111.67×144） */
	.like-items {
		display: grid;
		grid-template-columns: 1fr 1fr 1fr;
		grid-column-gap: 12rpx;
		grid-row-gap: 16rpx;
		padding: 0 24rpx 20rpx 24rpx;
	}

	.like-card {
		background-color: #ffffff;
		border-radius: 24rpx;
		padding-bottom: 16rpx;
		overflow: hidden;
	}

	.like-card-img {
		width: 100%;
		height: 172rpx;
	}

	.like-card-img-empty {
		background-color: #f2f2f2;
	}

	.like-card-name {
		font-size: 28rpx;
		color: #202020;
		padding: 10rpx 16rpx 0 16rpx;
	}

	.like-card-sub {
		font-size: 24rpx;
		color: #999999;
		padding: 2rpx 16rpx 0 16rpx;
	}

	.like-card-bottom {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 8rpx 16rpx 0 16rpx;
	}

	.like-card-price {
		display: flex;
		align-items: baseline;
	}

	.like-card-price .price-symbol {
		font-size: 20rpx;
		font-weight: bold;
	}

	.like-card-price-value {
		font-size: 28rpx;
		font-weight: bold;
		color: #202020;
	}

	.like-card-add {
		width: 48rpx;
		height: 48rpx;
		border-radius: 50%;
		color: #ffffff;
		font-size: 30rpx;
		line-height: 48rpx;
		text-align: center;
	}

	/* 结算栏：全选 + 合计 + 去结算(件数) */
	.settlement-view {
		position: fixed;
		left: 0;
		right: 0;
		bottom: 0;
		width: 100%;
		height: 100rpx;
		padding: 0 24rpx;
		box-sizing: border-box;
		background-color: #ffffff;
		border-top: 1rpx solid #f7f7f7;
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.settle-left {
		display: flex;
		align-items: center;
	}

	.settle-check {
		width: 40rpx;
		height: 40rpx;
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.settle-check-plain {
		border: 1rpx solid #ededed;
		background-color: #ffffff;
	}

	.settle-check-icon {
		font-size: 24rpx;
		color: #ffffff;
	}

	.settle-all-text {
		font-size: 28rpx;
		color: #202020;
		margin-left: 12rpx;
	}

	.settle-right {
		display: flex;
		align-items: center;
	}

	.settle-total {
		display: flex;
		align-items: baseline;
		margin-right: 20rpx;
	}

	.settle-total-label {
		font-size: 26rpx;
		color: #666666;
		margin-right: 8rpx;
	}

	.settle-total-symbol {
		font-size: 22rpx;
		font-weight: bold;
		color: #202020;
	}

	.settle-total-money {
		font-size: 32rpx;
		font-weight: bold;
		color: #202020;
	}

	.to-pay-view {
		width: 214rpx;
		height: 80rpx;
		border-radius: 40rpx;
		color: #ffffff;
		font-size: 30rpx;
		line-height: 80rpx;
		text-align: center;
	}

</style>