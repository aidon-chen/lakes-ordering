<template>
	<view>
		<view class="top-carousel-swiper" v-if="carouselUrls.length > 0">
			<swiper :indicator-dots="indicatorDots" class="carousel-swiper" :autoplay="autoplay" :interval="interval"
				:duration="duration" :indicator-active-color="afterColor">
				<block v-for="(item, index) in carouselUrls" :key="index">
					<swiper-item class="carousel-swiper-item">
						<image :src="item.imagePath" class="carousel-image" mode="aspectFill"
							:data-imageLinkUrl="item.imageLinkUrl" @tap="carouseCommodityDetailTap" />
					</swiper-item>
				</block>
			</swiper>
		</view>
		<!-- 商城首页增强 v3（2026-09-23）：猜你喜欢 = 一行三个（3 列小卡）；推荐好物 = 一行两个 -->
		<view class="like-section" v-if="likeList.length > 0">
			<view class="section-header">
				<text class="section-title">猜你喜欢</text>
				<text class="section-more theme-color" @tap="bindToMenu">查看全部</text>
			</view>
			<view class="like-grid">
				<view class="like-card" v-for="(item, index) in likeList" :key="index" hover-class="hover-class-public"
					@tap="commodityDetailTap" :data-id="item.id" :data-shopid="item.shopId">
					<image v-if="item.mainImage" :src="item.mainImage" mode="aspectFill" class="like-card-img"></image>
					<view v-else class="like-card-img like-card-img-empty"></view>
					<view class="like-card-name out_of_range one_row">{{ item.name }}</view>
					<view class="like-card-bottom">
						<view class="like-card-price">
							<text class="price-symbol">￥</text>
							<text class="like-card-price-value">{{ item.price }}</text>
						</view>
					</view>
				</view>
			</view>
		</view>

		<!-- C-10 批次6：分类宫格已按画板 B-01 移除（分类入口只在商城点单页左栏），改为「推荐好物」分区 -->
		<!-- 商城首页增强 v2（2026-09-23）：去掉「查看全部」入口；改为触底继续加载（不再固定 4 卡） -->
		<view class="section-header">
			<text class="section-title">推荐好物</text>
		</view>

		<view class="like-items">
			<view class="like-item" hover-class="hover-class-public" @tap="commodityDetailTap"
				:data-shopid="item.shopId" :data-id="item.id" v-for="(item, index) in recommendGoodsList" :key="index">
				<image v-if="item.mainImage" :src="item.mainImage" mode="scaleToFill" class="icon-like-class"></image>
				<view v-else class="icon-like-class icon-like-placeholder"></view>

				<view class="fullname-class">
					<text class="name-text out_of_range two_row">{{ item.name }}</text>
				</view>

				<view class="like-money-view">
					<view class="like-money">
						<text class="price-symbol">￥</text>
						<text class="price-value">{{ item.price }}</text>
					</view>
				</view>

				<view class="like-money-view">
					<text class="latelyMonthlySales out_of_range one_row">已售：{{ item.latelyMonthlySales || 0 }}</text>
					<view class="insert-car" @tap.stop.prevent="openSpecifications" :data-index="index">
						<van-icon name="plus" class="icongouwuche1 theme-bg" />
					</view>
				</view>
			</view>
		</view>
		<view class="list-loading" v-if="loadingMore">正在加载...</view>
		<van-divider contentPosition="center" style="padding-bottom: 20px;"
			v-if="!hasMore && !loadingMore && recommendGoodsList.length > 0">没有更多啦</van-divider>

		<!-- 底部占位：避免末行内容被固定悬浮购物车栏遮挡
		     （原「服务保障条」正品保障/到店自提/营业时间 已按用户 2026-09-23 要求删除） -->
		<view class="bottom-space"></view>

		<!-- 底部购物车悬浮栏：画板 B-01 未画入口，但首页加购后需可达购物车（工程补充，已在计划登记） -->
		<view class="cart-float" v-if="cartCount > 0" hover-class="hover-class-public" @tap="goToCart">
			<view class="cart-float-icon theme-bg">
				<van-icon name="shopping-cart-o" class="cart-float-icon-inner" />
				<view class="cart-float-badge">{{ cartCount }}</view>
			</view>
			<text class="cart-float-text">已选 {{ cartCount }} 件</text>
			<view class="cart-float-btn theme-bg">去结算</view>
		</view>

		<van-action-sheet :show="specificationsDialog" @close="close" @cancel="close" title="选择规格">
			<view class="content_box">
				<view class="goods-info-view">
					<image v-if="goods.mainImage" :src="goods.mainImage" mode="aspectFill" class="commodity-image"></image>
					<view v-else class="commodity-image commodity-image-placeholder"></view>
					<view>
						<view class="goods-info-name out_of_range one_row">{{ goods.name }}</view>
						<view class="goods-info-specListString">已选：{{ specListString }}</view>
						<view class="goods-info-price">￥{{ priceAfter }}</view>
					</view>
				</view>
				<scroll-view scroll-y style="height: 50vh">
					<view class="commdity-name-type-view">
						<!-- C-10 批次6：拆开 v-if/v-for（同元素会让 Vue3 语义失效，且触发小程序渲染层 framework inner error） -->
						<block v-if="specList && specList.length > 0">
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
						</block>
						<van-empty v-else description="暂无规格"></van-empty>
					</view>
				</scroll-view>

				<view slot="footer">
					<view class="good-choice-btn theme-bg" @tap="insertShoppingCart" :data-goodsid="goods.id">我选好了
					</view>
				</view>
			</view>
		</van-action-sheet>
	</view>
</template>

<script>
	import GlobalConfig from '../../../../utils/global-config';
	import https from '../../../../utils/http';
	import toastService from '../../../../utils/toast.service';
	import authService from '../../../../utils/auth';
	let app = null;
	var pageNo = 1;
	// 商城首页增强 v2（2026-09-23）：「推荐好物」改为可分页追加（2 列 × 5 行），不再固定 4 卡
	var pageSize = 10;
	export default {
		data() {
			return {
				indicatorDots: true,
				autoplay: true,
				interval: 5000,
				duration: 1000,
				//beforeColor: "white",//指示点颜色,
				afterColor: '#f1a142',
				//当前选中的指示点颜色
				opacity: 0.4,
				recommendGoodsList: [],
				likeList: [],
				specList: [],
				// 商城首页增强 v2：hasMore=是否还有下一页（触底继续加载）；loadingMore=追加中
				hasMore: true,
				loadingMore: false,
				carouselUrls: '',
				priceAfter: '',
				specListString: '',
				specificationsDialog: false,
				cartCount: 0,
				goods: {
					mainImage: '',
					name: '',
					id: ''
				}
			};
		},
		/**
		 * 生命周期函数--监听页面加载
		 */
		onLoad: function(options) {
			app = getApp();
			pageNo = 1;
			this.getSystemUsageRecord();
			this.getRecommendGoodsList();
			this.getGuessLikeList();
		},
		/**
		 * 生命周期函数--监听页面初次渲染完成
		 */
		onReady: function() {},
		/**
		 * 生命周期函数--监听页面显示
		 */
		onShow: function() {
			//pageNo=1;
			app.globalData.getShoppingCarNumber();
			this.refreshCartCount();
			// C-10 批次6：分类宫格已移除（分类入口在商城点单页左栏），不再拉取分类
			//this.getClassifyList();
			//this.getRecommendGoodsList();
			this.getCarouselList();
		},
		/**
		 * 生命周期函数--监听页面隐藏
		 */
		onHide: function() {},
		/**
		 * 生命周期函数--监听页面卸载
		 */
		onUnload: function() {},
		/**
		 * 页面相关事件处理函数--监听用户下拉动作
		 */
		onPullDownRefresh: function() {
			pageNo = 1;
			this.hasMore = true;
			this.getRecommendGoodsList();
			this.getGuessLikeList();
		},
		/**
		 * 页面上拉触底事件的处理函数
		 */
		onReachBottom: function() {
			// 商城首页增强 v2（2026-09-23）：「推荐好物」触底继续加载（不再固定 4 卡）
			if (!this.hasMore || this.loadingMore) {
				return;
			}
			pageNo++;
			this.getRecommendGoodsList();
		},
		/**
		 * 用户点击右上角分享
		 */
		onShareAppMessage: function() {},
		methods: {
			carouseCommodityDetailTap(e) {
				let _this = this;
				authService.checkIsLogin().then((result) => {
					console.log(result);
					if (result) {
						_this.getUserInfo(e);
						return;
					}
					app.globalData.checkIsAuth('scope.userInfo');
				});
			},
			getUserInfo: function(e) {
				https.request('/rest/member/getLoginMemberInfo', {}).then((result) => {
					if (result.success) {
						uni.navigateTo({
							url: e.currentTarget.dataset.imagelinkurl
						});
					} else {
						app.globalData.checkIsAuth('scope.userInfo');
					}
				});
			},
			getCarouselList() {
				https.request('/rest/advertisement/list', {
					type: 3,
					pageNo: -1,
					pageSize: 20
				}).then((result) => {
					if (result.success) {
						result.data.records.forEach(function(item, index) {
							if (item.imagePath) {
								item.imagePath = GlobalConfig.ossUrl + item.imagePath;
							}
						});
						// C-10 批次6：vue3 运行时未挂载小程序数据刷新方法（调用即抛错并中断后续语句），改响应式赋值
						this.carouselUrls = result.data.records;
					}
				});
			},
			// C-10 批次6：分类宫格已移除（画板 B-01 无宫格），getClassifyList 随之删除；
			// 分类入口由「查看全部」→ 商城点单页左栏承载。

			getRecommendGoodsList() {
				// 商城首页增强 v2（2026-09-23）：「推荐好物」改为分页追加（触底继续加载），不再固定 4 卡
				const isFirstPage = pageNo === 1;
				if (isFirstPage) {
					toastService.showLoading();
				} else {
					this.loadingMore = true;
				}
				https.request('/rest/pointsMall/goods/recommendGoodsList', {
					pageNo: pageNo,
					pageSize: pageSize
				}).then((result) => {
					toastService.hideLoading();
					uni.stopPullDownRefresh();
					this.loadingMore = false;
					if (result.success) {
						const records = (result.data && result.data.records) || [];
						records.forEach(function(item) {
							// 种子商品的 main_image 可能为空 → 交由模板渲染灰底占位（画板 B-01 图位即灰块）
							if (item.mainImage) {
								item.mainImage = GlobalConfig.ossUrl + item.mainImage;
							}
						});
						// vue3 运行时未挂载小程序数据刷新方法，改响应式赋值（勿改回 setData 式写法）
						this.recommendGoodsList = isFirstPage ?
							records :
							this.recommendGoodsList.concat(records);
						// MyBatis-Plus Page 元信息：current=当前页、pages=总页数
						const meta = result.data || {};
						const current = meta.current || pageNo;
						const pages = meta.pages || 0;
						this.hasMore = current < pages;
					}
				});
			},

			/** 商城首页增强（2026-09-23）：猜你喜欢（近30天热销 Top6），复用 car.vue 已用接口 */
			getGuessLikeList() {
				https.request('/rest/pointsMall/goods/guessLikeGoodsList', {}).then((result) => {
					if (result.success) {
						const list = (result.data || []).slice(0, 6);
						list.forEach(function(item) {
							if (item.mainImage) {
								item.mainImage = GlobalConfig.ossUrl + item.mainImage;
							}
						});
						// vue3 运行时限制：响应式赋值（勿改回 setData 式写法）
						this.likeList = list;
					}
				});
			},

			commodityDetailTap(e) {
				console.log(e.currentTarget.dataset.id);
				uni.navigateTo({
					url: '../detail/detail?id=' + e.currentTarget.dataset.id
				});
			},

			bindToMenu(e) {
				uni.navigateTo({
					url: '../menu/menu'
				});
			},

			getSystemUsageRecord() {
				https.request('/rest/systemUsageRecord/insert', {
					type: 'intoPointsMall'
				}).then((result) => {
					if (result.success) {}
				});
			},

			openSpecifications(e) {
				this.selectByGoodsId(e.currentTarget.dataset.index);
			},

			radioChange(e) {
				//获取复选框选中的下标值
				var checkValue = e.detail.value;
				//获取第一级分类的下标值
				let firstIndex = e.currentTarget.dataset.firstindex;
				//获取所有分类信息
				let specList = this.specList;
				//console.log(specList)
				//遍历分类信息给第一级分类为false，提交的时候对应各级分类
				for (var j in specList[firstIndex]) {
					specList[firstIndex][j].checked = false;
				}
				//给选中的第二级分类的checked设置为true
				// for (var i in checkValue) {
				// 	specList[firstIndex][checkValue[i]].checked = true;
				// 	//console.log(specList[firstIndex][checkValue[i]])
				// }
				specList[firstIndex][checkValue].checked = true;

				let price = this.goods.price;
				let specListString = '';
				for (let key in specList) {
					for (let keyof in specList[key]) {
						//console.log(specList[key][keyof].price)
						if (specList[key][keyof].checked) {
							price = price + specList[key][keyof].price;
							specListString = (specListString ? specListString + '/' : specListString) + specList[key][
								keyof].name;
						}
					}
				}
				console.log(specListString);
				// C-10 批次6：vue3 运行时未挂载小程序数据刷新方法，改响应式赋值（勿改回）
				this.specList = specList;
				this.priceAfter = price;
				this.specListString = specListString;
			},

			insertShoppingCart(e) {
				authService.checkIsLogin().then((result) => {
					toastService.showLoading();
					if (result) {
						let goodsSpecs = {};
						let specList = this.specList;
						for (let key in specList) {
							for (let keyof in specList[key]) {
								//拼接查询规格等的json数据格式，查询商品规格等对应的价格
								if (specList[key][keyof].checked) {
									goodsSpecs[key] = specList[key][keyof].name;
								}
							}
						}
						//console.log(goodsSpecs)
						let data = {
							goodsId: e.currentTarget.dataset.goodsid,
							specList: JSON.stringify(goodsSpecs)
						};
						toastService.hideLoading();
						https.request('/rest/member/pointsMall/shoppingCart/insert', data, authService.getToken())
							.then((result) => {
								if (result.success) {
									toastService.showToast('加入购物车成功');
									// C-10 批次6：vue3 运行时未挂载小程序数据刷新方法，改响应式赋值（勿改回）
									this.specificationsDialog = false;
									app.globalData.getShoppingCarNumber();
									this.refreshCartCount();
								}
							});
						return;
					}
					toastService.hideLoading();
					app.globalData.checkIsAuth('scope.userInfo');
				});
			},
			setTabBarBadge(num) {
				app.globalData.setTabBarBadge(num);
			},
			selectByGoodsId(index) {
				let goods = this.recommendGoodsList[index];
				https.request('/rest/pointsMall/goodsSpecificationOption/selectByGoodsId', {
					goodsId: goods.id
				}).then((result) => {
					if (result.success) {
						//重新设置商品的规格等数据的格式
						//let goodsSpecs = {};
						let specList = result.data;
						let price = goods.price;
						let specListString = '';
						for (let key in specList) {
							let isChecked = true;
							for (let keyof in specList[key]) {
								//拼接查询规格等的json数据格式，查询商品规格等对应的价格
								specList[key][keyof].checked = false;
								//设置每个规格的第一个选项为选中，当库存为0时则选中下一个
								if (specList[key][keyof].stock == 1 && isChecked) {
									specList[key][keyof].checked = true;
									//选中的规格价钱在商品价钱的基础上累加
									price = price + specList[key][keyof].price;
									specListString = (specListString ? specListString + '/' : specListString) +
										specList[key][keyof].name;
									isChecked = false;
								}
							}
						}
						console.log(goods);
						console.log(specList);
						// C-10 批次6：vue3 运行时未挂载小程序数据刷新方法，改响应式赋值（勿改回）
						this.specListString = specListString ? specListString : '暂无规格';
						this.specList = JSON.stringify(specList) == '{}' ? [] : specList;
						this.goods = goods;
						this.specificationsDialog = true;
						this.priceAfter = price;
					}
				});
			},
			/** 购物车件数（底部悬浮栏 + 角标联动） */
			refreshCartCount() {
				return authService.checkIsLogin().then((logged) => {
					if (!logged) {
						this.cartCount = 0;
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
						this.cartCount = records.reduce((sum, r) => sum + (r.number || 0), 0);
					});
				});
			},

			/** 去购物车（底部悬浮栏入口） */
			goToCart() {
				uni.navigateTo({
					url: '../car/car'
				});
			},

			close() {
				// C-10 批次6：vue3 运行时未挂载小程序数据刷新方法，改响应式赋值（勿改回）
				this.specificationsDialog = false;
			}
		}
	};
</script>
<style>
	page {
		width: 100%;
		background-color: #f5f5f5;
	}

	.top-carousel-swiper {
		width: 100%;
		border-radius: 10rpx;
	}

	.carousel-swiper {
		margin: 20rpx 20rpx 0 20rpx;
		height: 358rpx;
	}

	.carousel-swiper-item {
		border-radius: 15rpx;
		height: 100%;
	}

	.carousel-image {
		width: 100%;
		height: 100%;
	}

	/* C-10 批次6：分类宫格样式随宫格移除；新增「推荐好物」分区头（画板 B-01） */
	.section-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 20rpx 20rpx 10rpx 20rpx;
		background-color: #f5f5f5;
	}

	.section-title {
		font-size: 32rpx;
		font-weight: bold;
		color: #333333;
	}

	.section-more {
		font-size: 26rpx;
	}

	/* 商品无主图时的灰底占位（画板 B-01 图位即灰块，种子数据 main_image 可为空） */
	.icon-like-placeholder {
		background-color: #f2f2f2;
	}

	/* 商品推荐 */
	.recommend-business-title {
		margin-bottom: 20rpx;
		font-weight: bold;
		font-size: 30rpx;
	}

	.like-items {
		display: grid;
		grid-template-columns: 1fr 1fr;
		grid-column-gap: 10px;
		padding: 0 20rpx 10rpx 20rpx;
		padding: 0 20rpx;
		background-color: #f5f5f5;
	}

	.like-item {
		display: flex;
		/* justify-content: center; */
		align-items: center;
		flex-direction: column;
		background: white;
		border-radius: 15rpx;
		margin-bottom: 20rpx;
		padding-bottom: 10rpx;
	}

	.icon-like-class {
		width: 100%;
		height: 315rpx;
		border-radius: 15rpx 15rpx 0 0;
	}

	.item-two {
		margin: 0 3.5%;
	}

	.like-detail-view {
		width: 100%;
		height: auto;
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

	.latelyMonthlySales {
		width: 75%;
		font-size: 26rpx;
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.go-to-shop {
		padding: 0 20rpx;
		border-radius: 10rpx;
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
		padding: 5rpx 0;
	}

	.like-money {
		font-size: 26rpx;
		font-weight: bold;
	}

	/* C-10 批次6：价格排版（画板 B-01 为 ￥11px + 数字 18px bold） */
	.price-symbol {
		font-size: 22rpx;
		font-weight: bold;
	}

	.price-value {
		font-size: 36rpx;
		font-weight: bold;
		color: #333333;
	}

	/* SKU 弹层商品图占位（无主图时灰底，避免 src=null 的空白） */
	.commodity-image-placeholder {
		background-color: #f2f2f2;
	}

	.insert-car {
		position: absolute;
		margin-top: -0.5%;
		margin-left: 36%;
	}

	.icongouwuche1 {
		width: 40rpx;
		height: 40rpx;
		line-height: 40rpx;
		text-align: center;
		font-size: 24rpx;
		border-radius: 50%;
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

	.radio-group {
		width: 80%;
		display: flex;
		justify-content: flex-start;
		align-items: center;
		flex-wrap: wrap;
		padding: 10rpx;
		background: #fff;
		border-radius: 50rpx;
	}

	.group-label {
		width: 28%;
		padding: 12rpx 1rpx;
		margin: 5rpx 5rpx;
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

	.commodity-image {
		width: 170rpx;
		height: 152rpx;
		border-radius: 8rpx;
		margin-right: 10rpx;
	}

	.good-choice-btn {
		width: 100%;
		text-align: center;
		padding: 20rpx 0;
		border-radius: 15rpx;
		font-size: 28rpx;
		font-weight: bold;
	}

	.commdity-name-type-view {
		padding: 20rpx;
		background: #fff;
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

	.content_box {
		padding-top: 0;
	}

	/* 底部购物车悬浮栏（工程补充入口，与 B-02 同款） */
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

	/* 商城首页增强 v3（2026-09-23）：猜你喜欢 = 一行三个（3 列小卡）；推荐好物 = 一行两个（.like-items/.like-item） */
	.like-section {
		margin-top: 10rpx;
	}

	.like-grid {
		display: grid;
		grid-template-columns: 1fr 1fr 1fr;
		grid-column-gap: 12rpx;
		grid-row-gap: 16rpx;
		padding: 0 24rpx 20rpx 24rpx;
		background-color: #f5f5f5;
	}

	.like-card {
		background-color: #ffffff;
		border-radius: 15rpx;
		padding-bottom: 10rpx;
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
		font-size: 26rpx;
		color: #202020;
		padding: 10rpx 12rpx 0 12rpx;
	}

	.like-card-bottom {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 6rpx 12rpx 0 12rpx;
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

	/* 触底追加时的加载提示 */
	.list-loading {
		text-align: center;
		font-size: 26rpx;
		color: #999999;
		padding-bottom: 20px;
	}

	/* 底部占位高度：与固定悬浮购物车栏（距底 24rpx + 高 104rpx）留出余量
	   原「服务保障条」样式（.service-bar/.service-item/.service-icon/.service-text）已随组件一并删除 */
	.bottom-space {
		height: 140rpx;
	}

</style>