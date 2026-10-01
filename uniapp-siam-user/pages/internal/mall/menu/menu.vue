<template>
	<loading-toast />
	<view class="mall-menu-page">
		<!-- 左分类栏：92rpx 宽（画板 B-02）｜「全部」为前端虚拟项，其余为后端分类 -->
		<scroll-view scroll-y class="cate-bar">
			<view v-for="(item, index) in cateList" :key="index"
				:class="'cate-item ' + (activeCateIndex === index ? 'cate-item-active' : '')"
				hover-class="hover-class-public" @tap="switchCate(index)">
				<view class="cate-line theme-bg" v-if="activeCateIndex === index"></view>
				<text :class="'cate-text ' + (activeCateIndex === index ? 'theme-color cate-text-active' : '')">{{ item.name }}</text>
			</view>
		</scroll-view>

		<!-- 右商品区：283rpx 宽，组标题 + 96rpx 商品行（步进器 / 加购钮 / 售罄 三态） -->
		<scroll-view scroll-y class="goods-panel">
			<block v-for="(group, groupIndex) in visibleGroups" :key="groupIndex">
				<view class="group-title">{{ group.name }}</view>

				<view class="goods-row" v-for="(chil, goodsIndex) in group.goodsList" :key="goodsIndex">
					<view class="goods-row-main" hover-class="hover-class-public"
						@tap="parseEventDynamicCode($event, chil.goodsStatus == 4 ? '' : 'commodityDetailTap')"
						:data-id="chil.goodsId">
						<image v-if="chil.mainImage" :src="chil.mainImage" mode="aspectFill" class="goods-row-img"></image>
						<view v-else class="goods-row-img goods-row-img-empty"></view>

						<view class="goods-row-info">
							<text :class="'goods-row-name out_of_range one_row ' + (chil.goodsStatus == 4 ? 'goods-row-name-disabled' : '')">{{ chil.goodsName }}</text>
							<view class="goods-row-price" v-if="chil.goodsStatus != 4">
								<text class="price-symbol">￥</text>
								<text class="price-value">{{ chil.goodsPrice }}</text>
							</view>
							<view class="sell-out-tag" v-if="chil.goodsStatus == 4">已售罄</view>
						</view>
					</view>

					<view class="goods-row-action">
						<view class="stepper" v-if="chil.goodsStatus != 4 && chil.number > 0">
							<text class="step-btn" @tap.stop.prevent="changeNumber(chil, -1)">－</text>
							<text class="step-num">{{ chil.number }}</text>
							<text class="step-btn" @tap.stop.prevent="changeNumber(chil, 1)">＋</text>
						</view>
						<view v-else-if="chil.goodsStatus != 4" class="add-btn theme-bg"
							@tap.stop.prevent="openSpecifications" :data-goods-id="chil.goodsId">＋</view>
					</view>
				</view>
			</block>

			<view class="goods-empty" v-if="visibleGroups.length === 0">该分类暂无商品</view>
		</scroll-view>

		<!-- 底部购物车悬浮栏：画板 B-02 未画入口，但"点单 → 加购 → 结算"必须可达（工程补充，已在计划登记） -->
		<view class="cart-float" v-if="cartCount > 0" hover-class="hover-class-public" @tap="goToCart">
			<view class="cart-float-icon theme-bg">
				<van-icon name="shopping-cart-o" class="cart-float-icon-inner" />
				<view class="cart-float-badge">{{ cartCount }}</view>
			</view>
			<text class="cart-float-text">已选 {{ cartCount }} 件</text>
			<view class="cart-float-btn theme-bg">去结算</view>
		</view>

		<!-- SKU 选择弹层（沿用原结构，仅去掉重复的商品名行） -->
		<van-action-sheet :show="specificationsDialog" title="选择规格" @close="close" @cancel="close">
			<view class="content_box">
				<view class="goods-info-view">
					<image v-if="goodsInfo.mainImage" :src="goodsInfo.mainImage" mode="aspectFill" class="commodity-image"></image>
					<view v-else class="commodity-image commodity-image-empty"></view>
					<view>
						<view class="goods-info-name out_of_range one_row">{{ goodsInfo.name }}</view>
						<view class="goods-info-specListString">已选：{{ specListString }}</view>
						<view class="goods-info-price">￥{{ priceAfter }}</view>
					</view>
				</view>
				<scroll-view scroll-y style="height: 50vh">
					<view class="commdity-name-type-view">
						<view class="commdity-type-item" v-for="(item, key) in specList" :key="key">
							<view class="commdity-type-name">{{ key }}</view>

							<radio-group class="radio-group" @change="radioChange" :data-firstIndex="key">
								<label :class="
                                        'group-label theme-border ' +
                                        (!item.stock ? 'disabled-group-label' : '') +
                                        ' ' +
                                        (item.checked ? 'active theme-bg' : 'theme-color-border') +
                                        ' out_of_range one_row'
                                    " v-for="(item, index) in item" :key="index">
									<radio :value="index" :checked="item.checked" :disabled="!item.stock"
										class="radio" />

									{{ item.name }}
								</label>
							</radio-group>
						</view>
						<van-empty v-if="specList.length <= 0" description="暂无规格"></van-empty>
					</view>
				</scroll-view>
				<view slot="footer">
					<view class="good-choice-btn theme-bg" @tap="insertShoppingCart">我选好了</view>
				</view>
			</view>
		</van-action-sheet>
	</view>
</template>

<script>
	import GlobalConfig from '../../../../utils/global-config';
	import https from '../../../../utils/http';
	import authService from '../../../../utils/auth';
	import toastService from '../../../../utils/toast.service';
	let app = null;
	export default {
		data() {
			return {
				// 后端返回的分类（12 个实体分类，每个含 goodsList）
				menuList: [],
				// 左栏列表：「全部」+ 各实体分类（画板 B-02 共 13 项）
				cateList: [],
				// 0 = 全部，1..n = menuList[activeCateIndex - 1]
				activeCateIndex: 0,
				specificationsDialog: false,
				goodsInfo: {
					id: '',
					mainImage: '',
					name: '',
					price: ''
				},
				priceAfter: '',
				specListString: '',
				specList: [],
				cartCount: 0
			};
		},
		computed: {
			// 右栏分组：全部 → 所有分类各自成组（组标题＝分类名）；选中分类 → 仅该组
			visibleGroups() {
				if (this.activeCateIndex === 0) {
					return this.menuList;
				}
				const group = this.menuList[this.activeCateIndex - 1];
				return group ? [group] : [];
			}
		},
		/**
		 * 生命周期函数--监听页面加载
		 */
		onLoad: function(options) {
			app = getApp();
			this.getMenuList();
		},
		onReady: function() {},
		/**
		 * 生命周期函数--监听页面显示：回显购物车数量（原页面缺失此步 → 进页面看不到已加购数量）
		 */
		onShow: function() {
			if (this.menuList.length > 0) {
				this.refreshCart();
			}
		},
		onHide: function() {},
		onUnload: function() {},
		onPullDownRefresh: function() {},
		onReachBottom: function() {},
		onShareAppMessage: function() {},
		methods: {
			/** 加载分类 + 商品（C-10 批次6：原 van-tree-select 改为自绘左右栏） */
			getMenuList() {
				toastService.showLoading();
				https.request('/rest/pointsMall/menu/listWithGoods', {}).then((result) => {
					toastService.hideLoading();
					if (result.success && result.data) {
						const list = result.data;
						list.forEach((menu) => {
							menu.title = menu.name;
							(menu.goodsList || []).forEach((g) => {
								g.number = 0;
								g.cartId = '';
								// 种子商品 main_image 可能为空 → 交给模板渲染灰底占位
								if (g.mainImage) {
									g.mainImage = GlobalConfig.ossUrl + g.mainImage;
								}
							});
						});
						this.menuList = list;
						this.cateList = [{
							name: '全部'
						}].concat(list.map((m) => ({
							name: m.name
						})));
						this.refreshCart();
					}
				}).catch(() => {
					toastService.hideLoading();
					toastService.showToast('加载商品失败，请下拉重试');
				});
			},

			/** 切换左栏分类 */
			switchCate(index) {
				this.activeCateIndex = index;
			},

			/**
			 * 回显购物车：把每件商品的已加购数量与 cartId 写回 menuList，并刷新 tabBar 角标。
			 * 加购 / 加减 / 进页面 后都调用 —— 这是 B-02 主链路的关键（原实现完全没有回显）。
			 */
			refreshCart() {
				return authService.checkIsLogin().then((logged) => {
					if (!logged) {
						return;
					}
					return https.request('/rest/member/pointsMall/shoppingCart/list', {
						pageNo: 1,
						pageSize: 200
					}, authService.getToken()).then((result) => {
						if (!result.success) {
							return;
						}
						const records = (result.data && result.data.records) || [];
						const byGoods = {};
						records.forEach((r) => {
							const gid = r.goodsId;
							if (!byGoods[gid]) {
								byGoods[gid] = {
									number: 0,
									cartId: r.id
								};
							}
							byGoods[gid].number += (r.number || 0);
						});
						let total = 0;
						this.menuList.forEach((menu) => {
							(menu.goodsList || []).forEach((g) => {
								const hit = byGoods[g.goodsId];
								g.number = hit ? hit.number : 0;
								g.cartId = hit ? hit.cartId : '';
								total += g.number;
							});
						});
						this.menuList = this.menuList.slice();
						this.cartCount = total;
						if (app && app.globalData && app.globalData.setTabBarBadge) {
							app.globalData.setTabBarBadge(total);
						}
					}).catch(() => {});
				});
			},

			/** 点击 ＋ 打开 SKU 弹层 */
			openSpecifications(e) {
				const goodsId = e.currentTarget.dataset.goodsId;
				if (!goodsId) {
					return;
				}
				toastService.showLoading();
				this.specificationsDialog = true;
				this.goodsInfo.id = goodsId;
				this.getCommodityDetails(goodsId);
			},

			getCommodityDetails(id) {
				https.request('/rest/pointsMall/goods/selectById', {
					id: id,
					pageNo: -1,
					pageSize: 20
				}).then((result) => {
					toastService.hideLoading();
					if (result.success && result.data) {
						const detail = result.data;
						if (detail.mainImage) {
							detail.mainImage = GlobalConfig.ossUrl + detail.mainImage;
						}
						this.goodsInfo = detail;
						this.priceAfter = detail.price;
						this.selectByGoodsId(id);
					}
				}).catch(() => {
					toastService.hideLoading();
				});
			},

			commodityDetailTap(e) {
				uni.navigateTo({
					url: '../detail/detail?id=' + e.currentTarget.dataset.id
				});
			},

			selectByGoodsId(goodsId) {
				https.request('/rest/pointsMall/goodsSpecificationOption/selectByGoodsId', {
					goodsId: goodsId
				}).then((result) => {
					if (result.success && result.data) {
						const specList = result.data;
						let price = 0;
						let specListString = '';
						for (let key in specList) {
							let isChecked = true;
							for (let keyof in specList[key]) {
								specList[key][keyof].checked = false;
								// 每个规格默认选中第一个有库存的选项
								if (specList[key][keyof].stock == 1 && isChecked) {
									specList[key][keyof].checked = true;
									price = price + specList[key][keyof].price;
									specListString = (specListString ? specListString + '/' : specListString) +
										specList[key][keyof].name;
									isChecked = false;
								}
							}
						}
						this.specListString = specListString ? specListString : '暂无规格';
						this.specList = JSON.stringify(specList) == '{}' ? [] : specList;
					}
				}).catch(() => {
					toastService.hideLoading();
				});
			},

			/** 步进器：加/减（C-10 批次6 重写，原 bindMinus/bindPlus 引用了本页不存在的 shoppingCartList，必报错） */
			changeNumber(goods, delta) {
				if (goods.goodsStatus == 4) {
					return;
				}
				if (!goods.cartId) {
					return;
				}
				if (delta < 0 && goods.number <= 1) {
					// 减到 0 由后端自动删除（updateNumber 内 actualNumber==0 时删除）
				}
				toastService.showLoading();
				https.request('/rest/member/pointsMall/shoppingCart/updateNumber', {
					id: goods.cartId,
					type: delta > 0 ? 1 : 0,
					number: Math.abs(delta)
				}, authService.getToken()).then((result) => {
					toastService.hideLoading();
					if (result.success) {
						this.refreshCart();
					} else {
						toastService.showToast(result.message || '操作失败');
					}
				}).catch(() => {
					toastService.hideLoading();
					toastService.showToast('操作失败，请重试');
				});
			},

			radioChange(e) {
				const checkValue = e.detail.value;
				const firstIndex = e.currentTarget.dataset.firstindex;
				const specList = this.specList;
				for (let j in specList[firstIndex]) {
					specList[firstIndex][j].checked = false;
				}
				specList[firstIndex][checkValue].checked = true;

				let price = this.goodsInfo.price;
				let specListString = '';
				for (let key in specList) {
					for (let keyof in specList[key]) {
						if (specList[key][keyof].checked) {
							price = price + specList[key][keyof].price;
							specListString = (specListString ? specListString + '/' : specListString) + specList[key][
								keyof
							].name;
						}
					}
				}
				this.specList = specList;
				this.specListString = specListString;
				this.priceAfter = price;
			},

			/** 加入购物车（C-10 批次6：补 token + 成功后回显数量与角标） */
			insertShoppingCart(e) {
				authService.checkIsLogin().then((result) => {
					if (!result) {
						app.globalData.checkIsAuth('scope.userInfo');
						return;
					}
					toastService.showLoading();
					const goodsSpecs = {};
					const specList = this.specList;
					for (let key in specList) {
						for (let keyof in specList[key]) {
							// 拼接查询规格等的json数据格式，查询商品规格等对应的价格
							if (specList[key][keyof].checked) {
								goodsSpecs[key] = specList[key][keyof].name;
							}
						}
					}
					https.request('/rest/member/pointsMall/shoppingCart/insert', {
						goodsId: this.goodsInfo.id,
						specList: JSON.stringify(goodsSpecs)
					}, authService.getToken()).then((res) => {
						toastService.hideLoading();
						if (res.success) {
							toastService.showToast('加入购物车成功');
							this.specificationsDialog = false;
							this.refreshCart();
						} else {
							toastService.showToast(res.message || '加入购物车失败');
						}
					}).catch(() => {
						toastService.hideLoading();
						toastService.showToast('加入购物车失败，请重试');
					});
				});
			},

			close() {
				this.specificationsDialog = false;
			},

			/** 去购物车（底部悬浮栏入口） */
			goToCart() {
				if (this._navigating) {
					return;
				}
				this._navigating = true;
				uni.navigateTo({
					url: '../car/car',
					complete: () => {
						this._navigating = false;
					}
				});
			}
		}
	};
</script>
<style>
	page {
		background-color: #fff;
	}

	.mp-vtabs-view {
		background-color: #fff;
	}

	.content-class {
		/* margin-top: 6%; */
		width: 100%;
		height: 100%;
	}

	/* 地址定位 */
	.store-full-name-view {
		box-shadow: -2px 0.5px 5px 0.5px rgba(0, 0, 0, 0.1);
		padding: 15rpx 20rpx;
		margin: 15rpx 20rpx;
		border-radius: 10rpx;
		/* display: flex;
   justify-content: space-between;
   align-items: center; */
		background: #fff;
	}

	.banner-view {
		margin: 10rpx 20rpx;
		border-radius: 10rpx;
	}

	.place-image {
		width: 10%;
		height: auto;
		padding-right: 20rpx;
	}

	.business-text {
		font-size: 24rpx;
		font-weight: bold;
	}

	.right-class {
		width: 90%;
	}

	.store-full-name-place-view {
		font-size: 26rpx;
		font-weight: bold;
	}

	.distance-phone-username-veiw {
		color: #6b6b6b;
		font-size: 24rpx;
		line-height: 42rpx;
	}

	.tips-view {
		font-size: 24rpx;
		display: flex;
		align-items: center;
	}

	.mbp-view {
		color: #a1a1a1;
	}

	.bar- {
		font-size: 28rpx;
		margin: 0 10rpx;
	}

	/* 单选框样式--自取配送 */
	.radio-group-view {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 25%;
	}

	.radio-group {
		width: 100%;
		display: flex;
		justify-content: center;
		align-items: center;
		flex-wrap: wrap;
		padding: 5rpx;
		border-radius: 50rpx;
		/* height: 66rpx; */
	}

	.radio-group-label-radio {
		display: none;
	}

	.radio-group-label {
		width: 46%;
		padding: 2%;
		font-size: 26rpx;
		border-radius: 50rpx;
		display: flex;
		justify-content: center;
		align-items: center;
		/* height: 60rpx; */
	}

	.not-active {
		color: white;
	}

	/* 轮播图样式 */
	swiper {
		width: 100%;
		height: 244rpx;
	}

	.slide-image {
		width: 100%;
		height: 244rpx;
		border-radius: 10rpx;
	}

	.business-view {
		/* position: absolute;
   top: 6%; */
		text-align: center;
		width: 100%;
		margin-top: -20%;
	}

	.business-image {
		width: 160rpx;
		height: 160rpx;
		border: 2rpx solid #f5f5f5;
	}

	.business-info {
		/* margin-top: 40rpx; */
		text-align: center;
		margin-left: 20px;
		margin-right: 20px;
	}

	.business-info-name {
		font-size: 36rpx;
		font-weight: bold;
	}

	.notice-view {
		font-size: 24rpx;
		color: #9b9b9b;
	}

	.business-info-other {
		font-size: 22rpx;
	}

	.swiper-tabs-choice {
		width: 100%;
		text-align: center;
		height: 88rpx;
		line-height: 88rpx;
		display: flex;
		flex-flow: row;
		justify-content: space-between;
		background: #fff;
		z-index: 1;
		border-bottom: 6rpx solid #f5f5f5;
	}

	.swiper-tabs-choice-item {
		width: 50%;
	}

	.swiper-items {
		height: 100%;
	}

	/* 菜单品类样式 */
	.swiper-tab {
		/* width: 24%; */
		text-align: center;
		flex-flow: row;
		justify-content: space-between;
		z-index: -1;
		font-size: 34rpx;
	}

	.swiper-tab-item {
		/* width: 100%; */
		/* color:#969696; */
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 10rpx 15rpx;
		font-size: 28rpx;
		height: 80rpx;
		font-weight: bold;
	}

	.swiper_table_item_view {
		display: flex;
		align-items: center;
		justify-content: center;
		/* border-bottom: 4rpx solid #fff; */
	}

	.swiper-box {
		display: block;
		width: 100%;
		height: 100%;
		overflow: hidden;
	}

	.swiper-active {
		background: white;
		transition: 0.5s;
	}

	.swiper-items {
		height: 100%;
	}

	.selectMenuTap {
		width: 40%;
	}

	.scroll-views {
		height: 100%;
		background: #f5f5f5;
		border-radius: 10rpx;
	}

	.full-reduction-view {
		display: flex;
		align-items: center;
		background: white;
		padding: 10rpx 0rpx;
		height: 34rpx;
	}

	.full-reduction-list {
		font-size: 24rpx;
		font-weight: bold;
		padding: 0 10rpx;
		margin: 0 5rpx;
		border-radius: 10rpx;
	}

	.full-reduction-text {
		color: white;
		margin-left: 20rpx;
		font-size: 28rpx;
		width: 10%;
		text-align: center;
		border-radius: 15rpx;
	}

	.business-discount-list {
		padding: 1rpx 6rpx;
		font-size: 20rpx;
		font-weight: bold;
		border-radius: 10rpx;
		margin-right: 5rpx;
	}

	.other-promotionList {
		width: 27%;
		display: flex;
		align-items: center;
	}

	.business-discount {
		width: 85%;
		padding-bottom: 1rpx;
		margin-bottom: 10rpx;
		display: flex;
		align-items: center;
	}

	.business-info-flex {
		display: flex;
		align-items: center;
		justify-content: space-between;
		font-size: 24rpx;
		color: #717171;
		margin: 10rpx 10rpx 10rpx 0;
	}

	.commodity-menu-view {
		box-shadow: -2px 0.5px 5px 0.5px rgba(0, 0, 0, 0.1);
		height: 100%;
		display: flex;
		justify-content: space-between;
		border-radius: 10rpx;
		padding-bottom: 73px;
	}

	.commodity-detail-view {
		width: 100%;
		/* background: white; */
		border-radius: 10rpx;
	}

	/* 商品信息 */
	.commodity-item-view {
		display: flex;
		flex-direction: column;
		/* margin-bottom: 10rpx; */
		background: white;
		border-radius: 10rpx;
		padding-top: 20rpx;
	}

	.commodity-type {
		/* line-height: 64rpx; */
		padding: 10rpx 20rpx;
		/* margin-top: 25rpx; */
	}

	.categoryName-view {
		/* width: 35%; */
		font-size: 28rpx;
	}

	.font-white {
		color: white;
	}

	.commodity-item {
		display: flex;
		padding: 10rpx 20rpx 10rpx 20rpx;
		align-items: flex-end;
		border-radius: 5rpx;
		background-color: white;
	}

	.commodity-image {
		width: 170rpx;
		height: 166rpx;
		border-radius: 8rpx;
		margin-right: 10rpx;
	}

	.commodity-name-english-view {
		width: 62%;
	}

	.commodity-name-view {
		width: 100%;
		display: flex;
		align-items: center;
	}

	.line-view {
		background: #b0b0b0;
		width: 100%;
		height: 2rpx;
	}

	.commodity-name {
		font-size: 28rpx;
		color: #969696;
		font-weight: bold;
	}

	.commodity-english {
		font-size: 24rpx;
		color: #b0b0b0;
	}

	.money-view {
		font-size: 30rpx;
		font-weight: bold;
		margin-top: 30rpx;
	}

	.insert-view {
		border-radius: 50%;
		width: 45rpx;
		height: 45rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		color: white;
		line-height: 45rpx;
	}

	.shangjiatuijianjia {
		margin-right: 35rpx;
	}

	.money-insert-view {
		position: relative;
		margin-top: -10%;
		bottom: 5%;
		display: flex;
		align-items: center;
		justify-content: flex-end;
	}

	#space-view {
		height: 20rpx;
		background: white;
	}

	.now-order-image {
		width: 100%;
		height: 100%;
	}

	/* shopcart 样式 */
	.content-fullReductionRuleName {
		width: 100%;
		background: #edf3df;
		font-size: 24rpx;
		text-align: center;
		position: sticky;
		top: 0;
		z-index: 0;
		opacity: 0.7;
		visibility: visible;
	}

	.shopping-cart-detail {
		position: fixed;
		bottom: 0;
		z-index: 9999;
		width: 100%;
		background: white;
	}

	.highlight {
		position: relative;
		top: -10px;
		width: 80rpx;
		line-height: 80rpx;
		text-align: center;
		height: 80rpx;
		margin-left: 20rpx;
		margin-right: 20rpx;
		border-radius: 50%;
		border: 10rpx solid #444444;
	}

	.shopping-cart-content {
		width: 100%;
		display: flex;
		align-items: center;
		background: #535257;
		z-index: 9999;
		height: 54px;
	}

	.shopping-cart-left {
		width: 70%;
		height: 100%;
		display: flex;
		align-items: center;
		background: #505052;
		z-index: 9999;
	}

	.shopping-cart-right {
		width: 30%;
		height: 100%;
		text-align: center;
		color: white;
		z-index: 9999;
		font-size: 30rpx;
		font-weight: bold;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.shopping-cart-bg {
		background: #535257;
	}

	.shopping-cart-desc {
		font-size: 24rpx;
		color: #8c8c8e;
	}

	.shopping-cart-totalPrice {
		display: flex;
		align-items: center;
	}

	.fullPriceReductionClass {
		color: gainsboro;
		text-decoration: line-through;
		margin-right: 20rpx;
	}

	.totalPrice {
		color: white;
		font-weight: bold;
	}

	.full-price-reduction {
		color: white;
		font-weight: bold;
	}

	.not-full-price-reduction {
		font-size: 28rpx;
		color: #8c8c8e;
	}

	.fullPriceReductionIsHidden {
		font-size: 28rpx;
	}

	.content-manjian {
		position: relative;
		top: 0;
		background: #edf3df;
		font-size: 24rpx;
		font-weight: bold;
		text-align: center;
		height: 73px;
		z-index: 9999;
	}

	.num {
		position: absolute;
		top: -6px;
		right: -10px;
		width: 24px;
		height: 24px;
		line-height: 24px;
		text-align: center;
		border-radius: 16px;
		font-size: 9px;
		font-weight: 700;
		color: #fff;
		background: rgb(240, 20, 20);
		box-shadow: 0 4px 8px 0 rgba(0, 0, 0, 0.4);
	}

	.business-recommend-scroll-view {
		height: 269rpx;
		white-space: nowrap;
	}

	.icon-business-recommend-class {
		width: 255rpx;
		height: 200rpx;
		border-radius: 15rpx 15rpx 0 0;
	}

	.business-recommend-title {
		font-size: 34rpx;
		font-weight: bold;
		margin: 20rpx 20rpx 0 20rpx;
	}

	.business-recommend-scroll-view {
		margin: 20rpx 0rpx;
		background: #fff;
		border-radius: 50rpx;
		height: 100%;
		display: flex;
		align-items: center;
	}

	.business-recommend-scroll-view scroll-view {
		display: block;
		width: 100%;
	}

	.business-recommend-items {
		width: 100%;
		display: flex;
		/* justify-content: space-between; */
	}

	.business-recommend-item {
		width: 255rpx;
		height: auto;
		background: #f5f5f5;
		border-radius: 15rpx;
		margin: 0 10rpx;
	}

	.item-two {
		margin: 0 3.5%;
	}

	.recommend-sell-out {
		position: absolute;
		top: 10%;
		margin-left: 10%;
		height: 100rpx;
		line-height: 100rpx;
		width: 100rpx;
		text-align: center;
		opacity: 0.7;
		border-radius: 50%;
		font-size: 30rpx;
		background: #434343;
		color: white;
	}

	.business-recommend-detail-view {
		width: 255rpx;
		height: auto;
	}

	.fullname-class {
		margin-top: 11rpx;
		font-size: 26rpx;
	}

	.engname-class {
		font-size: 24rpx;
		color: #ccc;
		width: 90%;
	}

	.business-recommend-money-view {
		width: 100%;
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 0rpx 15rpx;
	}

	.fullname-stepper {
		margin-bottom: 10rpx;
	}

	.business-recommend-money-view .stepper {
		margin-right: 30rpx;
	}

	.business-recommend-money {
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

	.settlement-view {
		position: fixed;
		z-index: 999;
		background: white;
		top: 0;
		border-bottom: 6rpx solid #f5f5f5;
	}

	.manjiantop {
		position: absolute;
		top: 0;
	}

	.closeImages {
		position: relative;
		left: 91%;
		top: 4%;
		z-index: 999;
	}

	.close-image-class {
		width: 50rpx;
		height: auto;
	}

	.goods-info-view {
		display: flex;
		padding-bottom: 20rpx;
		padding-left: 20rpx;
	}

	.goods-info-name {
		font-size: 30rpx;
		font-weight: bold;
	}

	.goods-info-specListString {
		color: #6b6b6b;
		font-size: 24rpx;
	}

	.goods-info-price {
		font-weight: bold;
		height: 88rpx;
		line-height: 88rpx;
		font-size: 32rpx;
		color: #e0583b;
	}

	.specifications-scroll-view {
		height: 274px;
	}

	.clearNull {
		font-weight: 700;
		font-size: 15px;
		color: #80858a;
	}

	.specifications-dialog {
		margin-bottom: 0rpx;
	}

	.commdity-name-type-view {
		padding: 20rpx;
		background: #fff;
	}

	.commdity-name {
		font-size: 32rpx;
		font-weight: bold;
		line-height: 50rpx;
	}

	.commdity-engname {
		font-size: 28rpx;
		line-height: 60rpx;
	}

	.commdity-type-item {
		width: 100%;
		height: 100%;
		display: flex;
		justify-content: left;
		align-items: center;
		flex-wrap: wrap;
		padding-bottom: 10rpx;
	}

	.commdity-type-name {
		font-size: 28rpx;
		margin-right: 30rpx;
	}

	.radio-group {
		width: 80%;
		display: flex;
		justify-content: flex-start;
		align-items: center;
		flex-wrap: wrap;
		padding: 10rpx 10rpx 10rpx 0;
		background: #fff;
		border-radius: 50rpx;
	}

	.group-label {
		width: 28%;
		padding: 1%;
		margin: 1%;
		font-size: 26rpx;
		border-radius: 18rpx;
		text-align: center;
	}

	.disabled-group-label {
		background: #f5f5f5;
		color: #808080;
		border: none;
	}

	.radio {
		display: none;
	}

	.good-choice-view {
		padding: 20rpx;
		border-top: 1prx #808080 solid;
	}

	.good-choice-btn {
		width: 100%;
		text-align: center;
		padding: 20rpx 0;
		border-radius: 15rpx;
		font-size: 28rpx;
		font-weight: bold;
	}

	.shoppingCart-screen-dialog {
		height: 55%;
	}

	.shoppingCart-scroll-view {
		height: 350px;
	}

	.shoppingCart-item {
		display: flex;
		align-items: center;
		justify-content: space-between;
		/* width: 100%; */
		padding: 20rpx 20rpx;
		border-bottom: 1rpx solid #f5f5f5;
	}

	.goodsName-restructure-view {
		width: 50%;
	}

	.goodsName-packingCharges {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 20rpx;
	}

	.goodsName {
		font-size: 28rpx;
	}

	.restructure {
		font-size: 22rpx;
		color: #808080;
	}

	.goodsPrice-number-view {
		width: 50%;
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.goodsPrice {
		font-size: 30rpx;
		font-weight: bold;
		color: #f01414;
	}

	/*主容器*/

	.stepper {
		margin-left: 20rpx;
		display: flex;
		align-items: center;
	}

	/*加号和减号*/

	.stepper text {
		width: 45rpx;
		height: 45rpx;
		line-height: 45rpx;
		font-size: 28rpx;
	}

	/*数值*/

	.stepper input {
		width: 24px;
	}

	/* 商家栏样式 */
	.swiper-bussiness {
		text-align: left;
	}

	.swiper-tab-bussiness {
		text-align: left;
	}

	.swiper-bussiness-item {
		background: white;
		margin-bottom: 20rpx;
		padding: 20rpx;
	}

	.swiper-bussiness-info {
		font-size: 28rpx;
		color: #717171;
	}

	.swiper-bussiness-title {
		font-size: 30rpx;
		font-weight: bold;
	}

	.swiper-bussiness-row {
		font-size: 26rpx;
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 15rpx 20rpx;
		background: white;
	}

	.swiper-bussiness-row-left {
		width: 20%;
		font-size: 26rpx;
		color: black;
		font-weight: bold;
	}

	.contact-bussiness-text {
		color: #516b1d;
	}

	.carousel-swiper-item {
		border-radius: 15rpx;
		height: 200rpx;
	}

	.carousel-image {
		width: 100%;
		height: 100%;
	}

	.vtabs-content-item {
		height: 100%;
	}

	.mp-vtabs-class scroll-view {
		height: 100vh;
	}

	.is-end-item {
		padding-bottom: 50px;
		background-color: white;
	}

	.theme-other-bg {
		background: #353535;
		color: #5f5e63;
	}

	.weui-vtabs-bar__item .weui-vtabs-bar__title {
		white-space: normal;
		text-align: center;
	}

	.evaluate-business-info {
		display: flex;
		justify-content: space-between;
		align-items: center;
	}

	.evaluate-info-left {
		width: 80%;
		margin: 20rpx 0rpx;
		padding: 0 50rpx;
		border-right: 1rpx solid #f5f5f5;
	}

	.evaluate-info-right {
		width: 20%;
		padding: 20rpx;
		text-align: center;
	}

	.business-evaluate {
		display: flex;
		justify-content: space-between;
	}

	.evaluate-total-score {
		color: #f56427;
		font-size: 40rpx;
		margin-right: 25rpx;
	}

	.evaluate-total-star {
		color: #4f4f4f;
		font-size: 24rpx;
	}

	.evaluate-total-num {
		color: #4f4f4f;
		font-size: 35rpx;
	}

	.view-line {
		height: 20rpx;
		background: #f3f3f3;
	}

	.evaluate-items {
		padding: 20rpx;
		margin-bottom: 80rpx;
	}

	.evaluate-item {
		display: flex;
		justify-content: space-between;
		padding-bottom: 30rpx;
		padding-top: 20rpx;
		border-bottom: 1rpx solid #f5f5f5;
	}

	.appraise-reply-items {
		background: #f7f7f7;
		border-radius: 10rpx;
		padding: 10rpx 15rpx;
	}

	.reply-item {
		font-size: 24rpx;
		color: #4f4f4f;
	}

	.evaluate-item-detail {
		width: 90%;
	}

	.evaluate-user-image {
		width: 8%;
		height: 8%;
		border-radius: 50%;
	}

	.evaluate-itemu-username-time {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.username-detail {
		width: 60%;
		font-size: 28rpx;
	}

	.images-url {
		width: 100rpx;
		height: 100rpx;
		margin-right: 10rpx;
	}

	.datetime-detail {
		font-size: 24rpx;
		color: #7d7d7d;
	}

	.pl-dz-class {
		display: flex;
		align-items: center;
		justify-content: space-between;
		text-align: end;
	}

	.appraise-class-pl-dz {
		margin-left: 10rpx;
	}

	.pl-dz-view {
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 26rpx;
		color: #9b9b9b;
	}

	.pl-dz-display {
		display: flex;
		align-items: center;
	}

	.input-pinglun {
		width: 100%;
		padding: 20rpx;
	}

	.userinfo-text {
		font-size: 28rpx;
		text-align: end;
		font-weight: bold;
	}

	.input-appraise {
		width: 92%;
		border: 1rpx solid #8e8e8e;
		padding: 10rpx;
		border-radius: 15rpx;
		margin: 20rpx;
		text-align: start;
		height: 150rpx;
	}

	.pinglun-bottom {
		position: fixed;
		width: 100%;
		bottom: 0;
		background: white;
		border-top: 1rpx solid #f5f5f5;
	}

	.reply-button-view {
		text-align: end;
		padding: 0 20rpx 20rpx 20rpx;
	}

	.extClassShopDetail {
		z-index: 9999;
	}

	.shop-detail-dialog {
		margin-bottom: 20rpx;
	}

	.reduced-delivery-price {
		font-size: 28rpx;
		margin-top: 20rpx;
	}

	.dialog-title {
		font-size: 30rpx;
	}

	.edit-address-class {
		text-align: center;
		height: 50px;
	}

	.edit-address-btn {
		margin: 20rpx 0;
	}

	.icongouwuche1 {
		font-size: 40rpx;
	}

	.iconweibiaoti35-copy {
		font-size: 26rpx;
	}

	.main-active-class {
		background-color: var(--sidebar-selected-border-color, #5f7d22);
		background-color: var(--van-tree-select-nav-background, #5f7d22);
	}

	.van-sidebar-item--selected {
		background-color: var(--sidebar-selected-border-color, #5f7d22);
	}

	/* ==========================================================================
	   C-10 批次 6：自绘左右栏 + 96rpx 商品行（画板 B-02）
	   追加覆盖式：原 style 段保留不动，仅以更高特异性覆盖必要类
	   ========================================================================== */
	page {
		height: 100%;
	}

	.mall-menu-page {
		display: flex;
		width: 100%;
		height: 100%;
		overflow: hidden;
		background-color: #ffffff;
	}

	/* ---- 左分类栏 92px = 184rpx ---- */
	.cate-bar {
		width: 184rpx;
		height: 100%;
		background-color: #f5f5f5;
		flex-shrink: 0;
	}

	.cate-item {
		position: relative;
		width: 184rpx;
		height: 96rpx;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.cate-item-active {
		background-color: #ffffff;
	}

	.cate-line {
		position: absolute;
		left: 0;
		top: 0;
		width: 8rpx;
		height: 96rpx;
	}

	.cate-text {
		font-size: 28rpx;
		color: #666666;
	}

	.cate-text-active {
		font-weight: 500;
	}

	/* ---- 右商品区 283px ---- */
	.goods-panel {
		flex: 1;
		height: 100%;
		background-color: #ffffff;
	}

	.group-title {
		padding: 24rpx 24rpx 12rpx 24rpx;
		font-size: 28rpx;
		font-weight: bold;
		color: #333333;
	}

	.goods-row {
		position: relative;
		height: 192rpx;
		padding: 16rpx 24rpx;
		box-sizing: border-box;
		display: flex;
		align-items: center;
	}

	.goods-row-main {
		flex: 1;
		min-width: 0;
		display: flex;
		align-items: center;
	}

	.goods-row-img {
		width: 160rpx;
		height: 160rpx;
		border-radius: 16rpx;
		flex-shrink: 0;
	}

	.goods-row-img-empty {
		background-color: #f2f2f2;
	}

	.goods-row-info {
		flex: 1;
		min-width: 0;
		padding-left: 24rpx;
	}

	.goods-row-name {
		font-size: 30rpx;
		color: #202020;
		line-height: 42rpx;
	}

	.goods-row-name-disabled {
		color: #999999;
	}

	.goods-row-price {
		margin-top: 12rpx;
		display: flex;
		align-items: baseline;
	}

	.goods-row .price-symbol {
		font-size: 22rpx;
		font-weight: bold;
		color: #333333;
	}

	.goods-row .price-value {
		font-size: 36rpx;
		font-weight: bold;
		color: #333333;
	}

	.sell-out-tag {
		margin-top: 12rpx;
		width: 88rpx;
		height: 40rpx;
		line-height: 40rpx;
		text-align: center;
		background-color: #efefef;
		color: #999999;
		font-size: 20rpx;
		border-radius: 8rpx;
	}

	/* ---- 右侧操作区：加购钮 / 步进器（覆盖旧 .stepper text 的方形样式）---- */
	.goods-row-action {
		flex-shrink: 0;
		margin-left: 16rpx;
	}

	.add-btn {
		width: 56rpx;
		height: 56rpx;
		border-radius: 50%;
		color: #ffffff;
		font-size: 34rpx;
		line-height: 56rpx;
		text-align: center;
	}

	.goods-row .stepper {
		margin-left: 0;
		display: flex;
		align-items: center;
	}

	.goods-row .stepper text {
		width: auto;
		height: auto;
		line-height: 1;
		font-size: 28rpx;
		color: #333333;
	}

	.goods-row .step-btn {
		width: 52rpx;
		height: 52rpx;
		line-height: 52rpx;
		text-align: center;
		border: 1rpx solid #ededed;
		border-radius: 8rpx;
		font-size: 28rpx;
	}

	.goods-row .step-num {
		width: 72rpx;
		text-align: center;
		font-size: 28rpx;
		font-weight: 500;
	}

	.goods-empty {
		padding: 80rpx 0;
		text-align: center;
		font-size: 26rpx;
		color: #999999;
	}

	.commodity-image-empty {
		background-color: #f2f2f2;
	}


	/* 底部购物车悬浮栏（工程补充入口） */
	.mall-menu-page {
		padding-bottom: 0;
	}

	.cart-float {
		position: fixed;
		left: 24rpx;
		right: 24rpx;
		bottom: 24rpx;
		height: 104rpx;
		background-color: #ffffff;
		border-radius: 52rpx;
		box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.12);
		display: flex;
		align-items: center;
		padding: 0 12rpx 0 20rpx;
		box-sizing: border-box;
		z-index: 20;
	}

	.cart-float-icon {
		position: relative;
		width: 72rpx;
		height: 72rpx;
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
	}

	.cart-float-icon-inner {
		font-size: 40rpx;
		color: #ffffff;
	}

	.cart-float-badge {
		position: absolute;
		top: -8rpx;
		right: -8rpx;
		min-width: 32rpx;
		height: 32rpx;
		line-height: 32rpx;
		text-align: center;
		padding: 0 6rpx;
		border-radius: 16rpx;
		background-color: #ee0a24;
		color: #ffffff;
		font-size: 20rpx;
	}

	.cart-float-text {
		flex: 1;
		margin-left: 20rpx;
		font-size: 28rpx;
		color: #202020;
	}

	.cart-float-btn {
		width: 168rpx;
		height: 80rpx;
		border-radius: 40rpx;
		color: #ffffff;
		font-size: 28rpx;
		line-height: 80rpx;
		text-align: center;
		flex-shrink: 0;
	}

	/* 悬浮栏出现时，给右商品区留出底部空间，避免最后一条商品被遮挡 */
	.goods-panel {
		padding-bottom: 140rpx;
	}

</style>