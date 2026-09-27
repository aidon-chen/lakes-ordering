<template>
	<view class="c07">
		<!-- tab 条：画板 21:593 只有单个居中「商城券」+ 主题色下划线；
		     本页按用户口径改为 3 个状态 tab（待使用 / 已使用 / 已过期），
		     选中态沿用 App.vue「活动tab的公共样式」里的全局 .active -->
		<view class="swiper-content position-sticky">
			<view class="swiper-tab self-adaption">
				<view class="swiper-tab-item" :data-current="index" @tap="clickTab" hover-class="hover-click-class"
					v-for="(item, index) in tabList" :key="item.key">
					<view :class="'swiper_table_item_view ' + (currentTab === index ? 'active' : '')"
						:data-current="index" @tap="clickTab">
						{{ item.name }}
					</view>
				</view>
			</view>
		</view>

		<scroll-view :style="'height:' + winHeight + 'px;'" scroll-y class="scroll-views">
			<view class="coupins-items" v-if="activeList.length > 0">
				<view :class="'coupins-item ' + (itemDisabled(item) ? 'isRelation' : '')"
					v-for="(item, index) in activeList" :key="item.key">
					<view class="item-top">
						<view>
							<view class="couponsName">
								{{ item.record.couponsMemberRelationMap.couponsName }}-{{ item.record.couponsMemberRelationMap.preferentialTypeText }}
							</view>
							<view class="endTime">有效期至{{ item.record.couponsMemberRelationMap.endTime }}</view>
						</view>
						<view class="theme-color">
							<view class="couponsNum">{{ amountText(item.record.couponsMemberRelationMap) }}</view>
							<view class="afterDiscountPrice" v-if="discountText(item.record.couponsMemberRelationMap)">
								{{ discountText(item.record.couponsMemberRelationMap) }}
							</view>
						</view>
					</view>

					<view class="view-line"></view>

					<view class="bottom-view">
						<view class="usage-rule" @tap="toggleRule(item)">
							使用规则
							<text class="iconfont iconweibiaoti35"
								v-if="item.record.couponsMemberRelationMap.isShow"></text>
							<text class="iconfont iconhtbArrowright02" v-else></text>
						</view>
						<!-- 待使用可操作；已使用 / 已过期 只读展示状态 -->
						<block v-if="item.status === 'wait'">
							<view v-if="!item.record.couponsMemberRelationMap.checked"
								:class="'immediate-use ' + (canUse(item) ? 'theme-color-border' : 'out-of-commission')"
								@tap="onUse(item)">
								立即使用
							</view>
							<label class="checkbox-group-label" v-else>
								<checkbox :value="index" :checked="item.record.couponsMemberRelationMap.checked"
									class="theme-color theme-border-color" @tap="onCancelUse(item)" />
							</label>
						</block>
						<view v-else class="immediate-use out-of-commission">{{ statusText(item.status) }}</view>
					</view>

					<view v-if="item.record.couponsMemberRelationMap.isShow" class="usage-rule">
						{{ item.record.couponsMemberRelationMap.description }}
					</view>
				</view>
			</view>
			<van-empty v-else :description="'暂无' + tabList[currentTab].name" />
		</scroll-view>
	</view>
</template>

<script>
	import https from '../../../utils/http';
	import toastService from '../../../utils/toast.service';
	import utilHelper from '../../../utils/util';
	import dateHelper from '../../../utils/date-helper';
	import systemStatus from '../../../utils/system-status';
	let app = null;
	var pageNo = -1;
	var pageSize = 20;
	export default {
		data() {
			return {
				// 两条券来源各自保留原始数组：选券回填与满减计算都按「数组下标」索引，不能合并成一个
				couponsList: [],
				couponsPointsList: [],
				tabList: [{
						key: 'wait',
						name: '待使用'
					},
					{
						key: 'used',
						name: '已使用'
					},
					{
						key: 'expired',
						name: '已过期'
					}
				],
				currentTab: 0,
				winHeight: '',
				// 选券模式（支付页带 prevData 进来）才有值；纯浏览时为 null。
				// ⚠️ 原实现初始化为对象 → 恒为真 → 浏览模式也走「订单上下文」分支：
				//    该分支在无 shopId 时一条都不命中，且不会给 isRelation 赋值，
				//    结果整列表被 .isRelation 置灰、按钮显示为不可用（看着像没做）。
				prevData: null,
				afterDiscount: '',
				data: {
					couponsIsHidden: false
				},
				feeData: '',
				modeName: ''
			};
		},
		computed: {
			todayKey() {
				const d = new Date();
				const pad = (n) => (n < 10 ? '0' + n : '' + n);
				return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate());
			},
			// 两条来源按状态分桶；每项带 source + 在各自原始数组中的下标，供选券回填复用既有方法
			buckets() {
				const buckets = {
					wait: [],
					used: [],
					expired: []
				};
				const collect = (list, source) => {
					(list || []).forEach((record, index) => {
						const info = record.couponsMemberRelationMap || {};
						const status = this.resolveStatus(info);
						buckets[status].push({
							key: source + '-' + (info.id !== undefined && info.id !== null ? info.id :
								index),
							source: source,
							index: index,
							record: record,
							status: status
						});
					});
				};
				collect(this.couponsList, 'merchant');
				collect(this.couponsPointsList, 'mall');
				return buckets;
			},
			activeList() {
				return this.buckets[this.tabList[this.currentTab].key] || [];
			}
		},
		/**
		 * 生命周期函数--监听页面加载
		 */
		onLoad: function(options) {
			app = getApp();
			// 选券模式：支付页带 prevData 进来，选完回填上一页；否则是纯浏览
			var prevData = options && options.prevData;
			if (prevData) {
				prevData = JSON.parse(prevData);
				prevData.checked = true;
				this.prevData = prevData;
			}
			this.getCouponsMemberRelation();
			this.getCouponsMemberPointsMallRelation();
		},
		/**
		 * 生命周期函数--监听页面初次渲染完成
		 */
		onReady: function() {
			this.getHeight();
		},
		/**
		 * 生命周期函数--监听页面显示
		 */
		onShow: function() {},
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
		onPullDownRefresh: function() {},
		/**
		 * 页面上拉触底事件的处理函数
		 */
		onReachBottom: function() {},
		/**
		 * 用户点击右上角分享
		 */
		onShareAppMessage: function() {},
		methods: {
			// 列表高度 = 窗口高 − tab 条高。
			// ⚠️ 原实现用 this.prevData 判空，而它恒为对象 → 永远不减 tab 高，列表会被 tab 压住
			getHeight() {
				uni.getSystemInfo({
					success: (res) => {
						const query = uni.createSelectorQuery();
						query.select('.swiper-content').boundingClientRect();
						query.selectViewport().scrollOffset();
						query.exec((rects) => {
							const tabHeight = rects && rects[0] ? rects[0].height : 44;
							this.winHeight = res.windowHeight - tabHeight;
						});
					}
				});
			},

			// 状态判定：已使用优先；过期以 endTime 与今天做字符串比较
			// （endTime 已在上面的取数里被格式化成 YYYY-MM-DD，定时任务关闭时 is_expired 不可信）
			resolveStatus(info) {
				if (info.isUsed) {
					return 'used';
				}
				const end = info.endTime ? String(info.endTime) : '';
				if (info.isExpired) {
					return 'expired';
				}
				if (end && end.length >= 10 && end.substring(0, 10) < this.todayKey) {
					return 'expired';
				}
				return 'wait';
			},

			statusText(status) {
				if (status === 'used') {
					return '已使用';
				}
				if (status === 'expired') {
					return '已过期';
				}
				return '';
			},

			// 券面额：1=折扣（discountAmount 是比率，0.38 → 3.8 折）/ 2=满减
			// ⚠️ 浮点防御（09-25）：0.68*10=6.800000000000001 会撑爆券面，toFixed(2) 后剥尾零
			amountText(info) {
				if (!info) {
					return '';
				}
				if (Number(info.preferentialType) === 1) {
					const zhe = Number(info.discountAmount);
					if (!isFinite(zhe) || zhe <= 0) {
						return '折扣券';
					}
					const display = zhe <= 1 ? zhe * 10 : zhe;
					return parseFloat(display.toFixed(2)) + '折';
				}
				return '满' + (info.limitedPrice || 0) + '减' + (info.reducedPrice || 0);
			},

			// 优惠金额：选券模式用订单算出的真实值；浏览模式无订单金额，
			// 满减券直接展示券面额（对应画板 21:593 卡片的「优惠10元」）
			discountText(info) {
				if (!info || !info.isRelation) {
					return '';
				}
				const after = info.afterDiscountPrice;
				if (after !== undefined && after !== null && Number(after) >= 0) {
					return '优惠' + after + '元';
				}
				if (!this.prevData && Number(info.preferentialType) === 2) {
					return '优惠' + (info.reducedPrice || 0) + '元';
				}
				return '';
			},

			canUse(item) {
				return item.status === 'wait' && !!item.record.couponsMemberRelationMap.isRelation;
			},

			itemDisabled(item) {
				return item.status !== 'wait' || !item.record.couponsMemberRelationMap.isRelation;
			},

			// 点击切换
			clickTab(e) {
				const index = Number(e.currentTarget.dataset.current);
				if (this.currentTab === index) {
					return false;
				}
				this.currentTab = index;
			},

			toggleRule(item) {
				const info = item.record.couponsMemberRelationMap;
				info.isShow = !info.isShow;
			},

			// 把点击派回既有方法：它们按「原始数组下标」索引，故这里还原 legacy 的 event 形状
			onUse(item) {
				if (!this.canUse(item)) {
					return;
				}
				const e = {
					currentTarget: {
						dataset: {
							index: item.index
						}
					}
				};
				if (item.source === 'merchant') {
					this.onImmediateUse(e);
				} else {
					this.onImmediatePointsUse(e);
				}
			},

			onCancelUse(item) {
				const e = {
					currentTarget: {
						dataset: {
							index: item.index
						}
					}
				};
				if (item.source === 'merchant') {
					this.onRadioChange(e);
				} else {
					this.onPointsRadioChange(e);
				}
			},

			getCouponsMemberRelation() {
				toastService.showLoading();
				// 不再带 isUsed/isExpired 过滤：三个状态 tab 由客户端分桶（后端 SQL 另有 is_valid=1 硬编码）
https.request('/rest/member/couponsMemberRelation/list', {
					pageNo: pageNo,
					pageSize: pageSize
				}).then((result) => {
					toastService.hideLoading();
					if (result.success) {
						result.data.records.forEach((res) => {
							res.couponsMemberRelationMap.preferentialTypeText = systemStatus
								.preferentialTypeText(res.couponsMemberRelationMap.preferentialType);
							res.couponsMemberRelationMap.startTime = dateHelper.formatISODate(res
								.couponsMemberRelationMap.startTime, 'YYYY-MM-DD');
							//res.couponsMemberRelationMap.isRelation = true;
							res.couponsMemberRelationMap.endTime = dateHelper.formatISODate(res
								.couponsMemberRelationMap.endTime, 'YYYY-MM-DD');
							res.couponsMemberRelationMap.isShow = false; //设置每个优惠券的使用规则为隐藏
							//console.log(this.data)
							console.log(this.prevData);
							if (this.prevData) {
								if (res.couponsMemberRelationMap.id == this.prevData.id) {
									res.couponsMemberRelationMap.checked = true;
								}
								//判断优惠券是否过期和是否已经使用
								//if (!res.couponsMemberRelationMap.isExpired && !res.couponsMemberRelationMap.isUsed) {
								//console.log(this.data)
								//判断是否已经满减，如果已经满减就取满减之后的字段值，反之取总额
								//if (this.data.data.actualPrice && !this.data.data.fullPriceReduction) {
								//遍历当前订单的商品
								let afterDiscountPrice = 0;
								res.couponsMemberRelationMap.afterDiscountPrice = 0;
								//判断当前优惠券是折扣还是满减券,1等于折扣,2等于满减
								if (res.couponsMemberRelationMap.preferentialType == 1) {
									res.couponsMemberRelationMap.isRelation = false;
									if (res.shopList.length <= 0) {
										return;
									}
									res.shopList.forEach((shop, shopIndex) => {
										if (shop.id == this.prevData.shopId) {
											this.prevData.orderDetailList.forEach((order) => {
												//source  优惠券发放来源 1=商家中心 2=调度中心
												//如果是商家中心发放的优惠券，则需要判断关联商品
												//调度中心发放的优惠券，则无需判断关联商品，所有商品皆可使用
												if (res.couponsMemberRelationMap.source ==
													1) {
													//遍历当前优惠券绑定的优惠商品
													res.goodsList.forEach((goods) => {
														//console.log(goods)
														if (order.goodsId == goods
															.id) {
															//console.log(res.couponsMemberRelationMap)
															res.couponsMemberRelationMap
																.isRelation = true;
														}
													});
												}
												let orderPrice = order.price;
												let orderNumber = order.number;
												let unitPrice = utilHelper.toFixed(
													orderPrice - orderPrice * res
													.couponsMemberRelationMap
													.discountAmount, 2);
												//判断当前订单的商品是否等于优惠券绑定的优惠商品
												//等于则进行优惠
												//判断优惠券优惠的最大金额
												if (orderPrice >= afterDiscountPrice) {
													//console.log(afterDiscountPrice)
													afterDiscountPrice = orderPrice;
													//获取当前的商品折扣后的优惠价格
													res.couponsMemberRelationMap
														.afterDiscountPrice =
														res.couponsMemberRelationMap
														.discountAmount > 0 ? utilHelper
														.toFixed(unitPrice, 2) : unitPrice;
												}
												res.couponsMemberRelationMap.isRelation =
													true;
											});
											return;
										}
									});
								}
								//判断如果是优惠券满减的话这就进行优惠券的总价满减
								//console.log(res)
								if (res.couponsMemberRelationMap.preferentialType == 2) {
									if (this.prevData.fullPriceReductionAfter) {
										if (this.prevData.fullPriceReductionAfter >= res
											.couponsMemberRelationMap.limitedPrice) {
											//console.log(res.couponsMemberRelationMap.limitedPrice)
											res.couponsMemberRelationMap.afterDiscountPrice = this.prevData
												.fullPriceReductionAfter - res.couponsMemberRelationMap
												.reducedPrice;
										}
										return;
									}
									if (this.prevData.actualPrice >= res.couponsMemberRelationMap
										.limitedPrice) {
										res.couponsMemberRelationMap.afterDiscountPrice = this.prevData
											.actualPrice - res.couponsMemberRelationMap.reducedPrice;
									}
								}
								return;
							}
							console.log('从我的进入...');
							res.couponsMemberRelationMap.isRelation = true;
							//}
						});

												// 安全网：取数分支已给浏览模式置 isRelation=true，这里兜住任何漏网的记录，
						// 否则模板会按「不可用」把它置灰
						const records = result.data.records || [];
						records.forEach((row) => {
							const info = row.couponsMemberRelationMap || {};
							if (info.isRelation === undefined) {
								info.isRelation = true;
							}
						});
						this.couponsList = records;
					}
				});
			},

			getCouponsMemberPointsMallRelation() {
				toastService.showLoading();
				// 同商家券：取全量，状态分桶在客户端做
https.request('/rest/member/pointsMall/couponsMemberRelation/list', {
					pageNo: pageNo,
					pageSize: pageSize
				}).then((result) => {
					toastService.hideLoading();
					if (result.success) {
						result.data.records.forEach((res) => {
							res.couponsMemberRelationMap.preferentialTypeText = systemStatus
								.preferentialTypeText(res.couponsMemberRelationMap.preferentialType);
							res.couponsMemberRelationMap.startTime = dateHelper.formatISODate(res
								.couponsMemberRelationMap.startTime, 'YYYY-MM-DD');
							//res.couponsMemberRelationMap.isRelation = true;
							res.couponsMemberRelationMap.endTime = dateHelper.formatISODate(res
								.couponsMemberRelationMap.endTime, 'YYYY-MM-DD');
							res.couponsMemberRelationMap.isShow = false; //设置每个优惠券的使用规则为隐藏
							//console.log(this.data)
							console.log(this.prevData);
							if (this.prevData) {
								if (res.couponsMemberRelationMap.id == this.prevData.id) {
									res.couponsMemberRelationMap.checked = true;
								}
								//判断优惠券是否过期和是否已经使用
								//if (!res.couponsMemberRelationMap.isExpired && !res.couponsMemberRelationMap.isUsed) {
								//console.log(this.data)
								//判断是否已经满减，如果已经满减就取满减之后的字段值，反之取总额
								//if (this.data.data.actualPrice && !this.data.data.fullPriceReduction) {
								//遍历当前订单的商品
								let afterDiscountPrice = 0;
								res.couponsMemberRelationMap.afterDiscountPrice = 0;
								//判断当前优惠券是折扣还是满减券,1等于折扣,2等于满减
								if (res.couponsMemberRelationMap.preferentialType == 1) {
									res.couponsMemberRelationMap.isRelation = false;
									if (this.prevData) {
										this.prevData.orderDetailList.forEach((order) => {
											//source  优惠券发放来源 1=商家中心 2=调度中心
											//如果是商家中心发放的优惠券，则需要判断关联商品
											//调度中心发放的优惠券，则无需判断关联商品，所有商品皆可使用
											if (res.couponsMemberRelationMap.source == 1) {
												//遍历当前优惠券绑定的优惠商品
												res.goodsList.forEach((goods) => {
													//console.log(goods)
													if (order.goodsId == goods.id) {
														//console.log(res.couponsMemberRelationMap)
														res.couponsMemberRelationMap
															.isRelation = true;
													}
												});
											}
											let orderPrice = order.price;
											let orderNumber = order.number;
											let unitPrice = utilHelper.toFixed(orderPrice -
												orderPrice * res.couponsMemberRelationMap
												.discountAmount, 2);
											//判断当前订单的商品是否等于优惠券绑定的优惠商品
											//等于则进行优惠
											//判断优惠券优惠的最大金额
											if (orderPrice >= afterDiscountPrice) {
												//console.log(afterDiscountPrice)
												afterDiscountPrice = orderPrice;
												//获取当前的商品折扣后的优惠价格
												res.couponsMemberRelationMap.afterDiscountPrice =
													res.couponsMemberRelationMap.discountAmount >
													0 ? utilHelper.toFixed(unitPrice, 2) :
													unitPrice;
											}
											res.couponsMemberRelationMap.isRelation = true;
										});
									}
								}
								//判断如果是优惠券满减的话这就进行优惠券的总价满减
								//console.log(res)
								if (res.couponsMemberRelationMap.preferentialType == 2) {
									if (this.prevData.fullPriceReductionAfter) {
										if (this.prevData.fullPriceReductionAfter >= res
											.couponsMemberRelationMap.limitedPrice) {
											//console.log(res.couponsMemberRelationMap.limitedPrice)
											res.couponsMemberRelationMap.afterDiscountPrice = this.prevData
												.fullPriceReductionAfter - res.couponsMemberRelationMap
												.reducedPrice;
										}
										return;
									}
									if (this.prevData.actualPrice >= res.couponsMemberRelationMap
										.limitedPrice) {
										res.couponsMemberRelationMap.afterDiscountPrice = this.prevData
											.actualPrice - res.couponsMemberRelationMap.reducedPrice;
									}
								}
								return;
							}
							console.log('从我的进入...');
							res.couponsMemberRelationMap.isRelation = true;
							//}
						});

												// 安全网：同上（浏览模式整列表被置灰的兜底）
						const records = result.data.records || [];
						records.forEach((row) => {
							const info = row.couponsMemberRelationMap || {};
							if (info.isRelation === undefined) {
								info.isRelation = true;
							}
						});
						this.couponsPointsList = records;
					}
				});
			},

			onImmediateUse(e) {
				if (this.prevData) {
					//直接调用上一个页面的 setData() 方法，把数据存到上一个页面中去
					//console.log(this.data)
					let afterDiscounts = this.prevData;
					//console.log(afterDiscounts)
					//console.log(afterDiscounts.fullPriceReductionAfter - afterDiscounts.price)
					afterDiscounts.id = this.couponsList[e.currentTarget.dataset.index].couponsMemberRelationMap.id;
					//console.log(this.data.couponsList[e.currentTarget.dataset.index].couponsMemberRelationMap.afterDiscountPrice)
					afterDiscounts.price = this.couponsList[e.currentTarget.dataset.index].couponsMemberRelationMap
						.afterDiscountPrice;
					afterDiscounts.couponsName = this.couponsList[e.currentTarget.dataset.index].couponsMemberRelationMap
						.couponsName;
					afterDiscounts.preferentialTypeText = systemStatus.preferentialTypeText(this.couponsList[e
						.currentTarget.dataset.index].preferentialType);
					console.log(this.prevData);
					// prevPage.setData({
					//   afterDiscount: afterDiscounts,
					//   "data.fullPriceReduction": utilHelper.toFixed((this.data.prevData.actualPrice - afterDiscounts.price), 2),
					//   "data.couponsIsHidden": true
					// })
					this.afterDiscount = afterDiscounts;
					this.prevData.fullPriceReduction = utilHelper.toFixed(this.prevData.actualPrice - afterDiscounts.price, 2);
					this.prevData.couponsIsHidden = true;
					this.getFullReductionRule();
					//console.log(prevPage.data)
					//wx.navigateBack(1);
					return;
				}
				let couponIndex = e.currentTarget.dataset.index;
				console.log(this.couponsList[couponIndex]);
				if (this.couponsList[couponIndex].shopList.length == 1) {
					uni.navigateTo({
						url: '../../menu/index/index?id=' + this.couponsList[couponIndex].shopList[0].id
					});
				} else if (this.couponsList[couponIndex].shopList.length > 1) {
					uni.switchTab({
						url: '/pages/index/index'
					});
				} else {
					uni.switchTab({
						url: '/pages/index/index'
					});
				}
			},

			onImmediatePointsUse(e) {
				if (this.prevData) {
					//直接调用上一个页面的 setData() 方法，把数据存到上一个页面中去
					//console.log(this.data)
					let afterDiscounts = this.prevData;
					//console.log(afterDiscounts)
					//console.log(afterDiscounts.fullPriceReductionAfter - afterDiscounts.price)
					afterDiscounts.id = this.couponsPointsList[e.currentTarget.dataset.index].couponsMemberRelationMap.id;
					//console.log(this.data.couponsList[e.currentTarget.dataset.index].couponsMemberRelationMap.afterDiscountPrice)
					afterDiscounts.price = this.couponsPointsList[e.currentTarget.dataset.index].couponsMemberRelationMap
						.afterDiscountPrice;
					afterDiscounts.couponsName = this.couponsPointsList[e.currentTarget.dataset.index]
						.couponsMemberRelationMap.couponsName;
					afterDiscounts.preferentialTypeText = systemStatus.preferentialTypeText(this.couponsPointsList[e
						.currentTarget.dataset.index].preferentialType);
					console.log(this.prevData);
					// prevPage.setData({
					//   afterDiscount: afterDiscounts,
					//   "data.fullPriceReduction": utilHelper.toFixed((this.data.prevData.actualPrice - afterDiscounts.price), 2),
					//   "data.couponsIsHidden": true
					// })
					this.afterDiscount = afterDiscounts;
					this.prevData.fullPriceReduction = utilHelper.toFixed(this.prevData.actualPrice - afterDiscounts.price, 2);
					this.prevData.couponsIsHidden = true;
					this.getPointsFullReductionRule();
					//console.log(prevPage.data)
					//wx.navigateBack(1);
					return;
				}
				let couponIndex = e.currentTarget.dataset.index;
				console.log(this.couponsPointsList[couponIndex]);
				uni.switchTab({
					// switchTab 必须用 tabBar 里的绝对路径；商城 tab 是 pages/internal/mall/index/index
					url: '/pages/internal/mall/index/index'
				});
			},

			//获取满减规则
			getFullReductionRule() {
				toastService.showLoading();
				https.request('/rest/fullReductionRule/list', {
					pageNo: -1,
					pageSize: 1,
					shopId: this.prevData.shopId
				}).then((result) => {
					toastService.hideLoading();
					if (result.success) {
						var pages = getCurrentPages();
						var prevPage = pages[pages.length - 2]; //上一个页面
						//获取配送费，配送费不作为满减条件
						let feeData = app.globalData.deliveryAndSelfTaking.reducedDeliveryTotalPrice ? app
							.globalData.deliveryAndSelfTaking.reducedDeliveryTotalPrice : 0;
						let actualPrice = this.prevData.actualPrice;
						let fullPriceReductionBefore = actualPrice - feeData;
						let reducedDeliveryPrice = 0;
						console.log('总商品价格===》' + this.prevData.actualPrice);
						this.prevData.actualPrice = this.prevData.actualPrice ? utilHelper.toFixed(Number(this
							.prevData.actualPrice), 2) : 0;
						this.prevData.fullPriceReductionAfter = this.prevData.actualPrice;
						this.prevData.discountPrice = 0;
						this.prevData.fullPriceReductionBefore = fullPriceReductionBefore;
						this.prevData.fullPriceReductionIsHidden = this.prevData.fullPriceReductionIsHidden;
						this.prevData.limitedPrice = 0;
						if (prevPage.data.reducedDeliveryPrice >= feeData) {
							reducedDeliveryPrice = 0;
						} else {
							reducedDeliveryPrice = feeData - prevPage.data.reducedDeliveryPrice;
						}
						console.log('fullPriceReductionBefore===>' + fullPriceReductionBefore);
						console.log('获取商家配送费====' + prevPage.data.reducedDeliveryPrice);
						console.log('获取用户地址配送费====' + feeData);
						console.log('获取商家支付后的配送费====' + reducedDeliveryPrice);
						console.log('获取价格====' + this.prevData.fullPriceReductionBefore);
						console.log('获取优惠券信息===>');
						console.log(this.afterDiscount);
						fullPriceReductionBefore -= this.afterDiscount.price;
						this.prevData.fullPriceReduction = fullPriceReductionBefore;
						console.log('获取优惠券后的金额===》' + fullPriceReductionBefore);
						for (let i = 0; i < result.data.records.length; i++) {
							//总价格减去配送费大于满减金额则进行满减操作
							if (Number(fullPriceReductionBefore) >= result.data.records[i].limitedPrice) {
								//判断当前满减价格limitedPrice和上一个满减价格对比，如果大于就进行认证
								if (result.data.records[i].limitedPrice > this.prevData.limitedPrice) {
									this.prevData.limitedPrice = result.data.records[i].limitedPrice;
									console.log(result.data.records[i].reducedPrice);
									this.prevData.fullPriceReduction = utilHelper.toFixed(
										fullPriceReductionBefore - result.data.records[i].reducedPrice, 2);
									this.prevData.fullReductionRuleName = result.data.records[i].name;
									this.prevData.fullReductionRuleId = result.data.records[i].id;
									this.prevData.fullPriceReductionIsHidden = true;
									this.prevData.fullPriceReductionAfter = utilHelper.toFixed(this.prevData
										.fullPriceReduction - result.data.records[i].reducedPrice, 2);
								}
							}
						}
						console.log('获取满减后的金额===》' + this.prevData.fullPriceReduction);
						if (app.globalData.deliveryAndSelfTaking.currentTab == 0) {
							this.prevData.fullPriceReduction = utilHelper.toFixed(this.prevData
								.fullPriceReduction + reducedDeliveryPrice, 2);
						}
						app.globalData.deliveryAndSelfTaking.isThereADiscount = false;
						if (feeData != reducedDeliveryPrice) {
							app.globalData.deliveryAndSelfTaking.isThereADiscount = true;
						}
						console.log('获取价格====' + this.prevData.fullPriceReductionBefore);
						// vue3 下 prevPage 也没有 setData → 直接赋值（this.prevData 是 data 字段，整体替换）
						prevPage.data = this.prevData;
						prevPage.afterDiscount = this.afterDiscount;
						uni.navigateBack(1);
					}
				});
			},

			//获取满减规则
			getPointsFullReductionRule() {
				toastService.showLoading();
				https.request('/rest/pointsMall/fullReductionRule/list', {
					pageNo: -1,
					pageSize: 1
				}).then((result) => {
					toastService.hideLoading();
					if (result.success) {
						var pages = getCurrentPages();
						var prevPage = pages[pages.length - 2]; //上一个页面

						let actualPrice = this.prevData.actualPrice;
						let fullPriceReductionBefore = actualPrice;
						console.log('总商品价格===》' + this.prevData.actualPrice);
						this.prevData.actualPrice = this.prevData.actualPrice ? utilHelper.toFixed(Number(this
							.prevData.actualPrice), 2) : 0;
						this.prevData.fullPriceReductionAfter = this.prevData.actualPrice;
						this.prevData.discountPrice = 0;
						this.prevData.fullPriceReductionBefore = fullPriceReductionBefore;
						this.prevData.fullPriceReductionIsHidden = this.prevData.fullPriceReductionIsHidden;
						this.prevData.limitedPrice = 0;
						fullPriceReductionBefore -= this.afterDiscount.price;
						this.prevData.fullPriceReduction = fullPriceReductionBefore;
						console.log('获取优惠券后的金额===》' + fullPriceReductionBefore);
						for (let i = 0; i < result.data.records.length; i++) {
							//总价格减去配送费大于满减金额则进行满减操作
							if (Number(fullPriceReductionBefore) >= result.data.records[i].limitedPrice) {
								//判断当前满减价格limitedPrice和上一个满减价格对比，如果大于就进行认证
								if (result.data.records[i].limitedPrice > this.prevData.limitedPrice) {
									this.prevData.limitedPrice = result.data.records[i].limitedPrice;
									console.log(result.data.records[i].reducedPrice);
									this.prevData.fullPriceReduction = utilHelper.toFixed(
										fullPriceReductionBefore - result.data.records[i].reducedPrice, 2);
									this.prevData.fullReductionRuleName = result.data.records[i].name;
									this.prevData.fullReductionRuleId = result.data.records[i].id;
									this.prevData.fullPriceReductionIsHidden = true;
									this.prevData.fullPriceReductionAfter = utilHelper.toFixed(this.prevData
										.fullPriceReduction - result.data.records[i].reducedPrice, 2);
								}
							}
						}
						console.log('获取价格====' + this.prevData.fullPriceReductionBefore);
						// vue3 下 prevPage 也没有 setData → 直接赋值（this.prevData 是 data 字段，整体替换）
						prevPage.data = this.prevData;
						prevPage.afterDiscount = this.afterDiscount;
						uni.navigateBack(1);
					}
				});
			},

			getFullReductionRule_() {
				toastService.showLoading();
				https.request('/rest/fullReductionRule/list', {
					pageNo: -1,
					pageSize: 1,
					shopId: this.prevData.shopId
				}).then((result) => {
					toastService.hideLoading();
					if (result.success) {
						var pages = getCurrentPages();
						var prevPage = pages[pages.length - 2]; //上一个页面
						//获取配送费，配送费不作为满减条件
						let feeData = app.globalData.deliveryAndSelfTaking.reducedDeliveryTotalPrice ? app
							.globalData.deliveryAndSelfTaking.reducedDeliveryTotalPrice : 0;
						let reducedDeliveryPrice = 0;
						console.log('商品总价----》' + this.prevData.actualPrice);
						if (prevPage.data.reducedDeliveryPrice >= feeData) {
							reducedDeliveryPrice = 0;
							console.log('用户需支付的配送费为====>' + reducedDeliveryPrice);
						} else {
							reducedDeliveryPrice = feeData - prevPage.data.reducedDeliveryPrice;
							console.log('用户需支付的配送费为====>' + reducedDeliveryPrice);
						}
						this.prevData.actualPrice = this.prevData.actualPrice ? utilHelper.toFixed(Number(this
							.prevData.actualPrice), 2) : 0;
						this.prevData.fullPriceReductionAfter = this.prevData.actualPrice;
						this.prevData.discountPrice = 0;
						this.prevData.fullPriceReductionBefore = this.prevData.actualPrice > 0 ? this.prevData
							.actualPrice - feeData : 0;
						this.prevData.fullPriceReductionIsHidden = this.prevData.fullPriceReductionIsHidden;
						this.prevData.limitedPrice = 0;
						console.log('获取商家配送费====' + prevPage.data.reducedDeliveryPrice);
						console.log('获取用户地址配送费====' + feeData);
						console.log('获取商家支付后的配送费====' + reducedDeliveryPrice);
						console.log('获取价格====' + this.prevData.fullPriceReductionBefore);
						this.prevData.fullPriceReduction = this.prevData.fullPriceReductionBefore;
						for (let i = 0; i < result.data.records.length; i++) {
							//总价格减去配送费大于满减金额则进行满减操作
							if (Number(this.prevData.fullPriceReductionBefore) >= result.data.records[i]
								.limitedPrice) {
								//判断当前满减价格limitedPrice和上一个满减价格对比，如果大于就进行认证
								if (result.data.records[i].limitedPrice > this.prevData.limitedPrice) {
									this.prevData.limitedPrice = result.data.records[i].limitedPrice;
									this.prevData.fullPriceReduction = utilHelper.toFixed(this.prevData
										.fullPriceReductionBefore - result.data.records[i].reducedPrice, 2);
									this.prevData.fullReductionRuleName = result.data.records[i].name;
									this.prevData.fullReductionRuleId = result.data.records[i].id;
									this.prevData.fullPriceReductionIsHidden = true;
									this.prevData.fullPriceReductionAfter = utilHelper.toFixed(this.prevData
										.fullPriceReductionBefore - result.data.records[i].reducedPrice, 2);
									//console.log(this.data.prevData.fullPriceReduction)
								}
							}
						}

						console.log('获取最终配送金额=====' + reducedDeliveryPrice);
						console.log('获取最终优惠金额=====' + this.prevData.fullPriceReduction);
						this.prevData.fullPriceReduction = this.prevData.fullPriceReduction;
						if (app.globalData.deliveryAndSelfTaking.currentTab == 0) {
							this.prevData.fullPriceReduction = this.prevData.fullPriceReduction +
								reducedDeliveryPrice;
						}
						app.globalData.deliveryAndSelfTaking.isThereADiscount = false;
						if (reducedDeliveryPrice != feeData) {
							app.globalData.deliveryAndSelfTaking.isThereADiscount = true;
						}
						console.log('获取最终价格====' + this.prevData.fullPriceReduction);
						prevPage.feeData = reducedDeliveryPrice;
						prevPage.data = this.prevData;
						prevPage.data.couponsIsHidden = false;
					}
				});
			},

			getPonintsFullReductionRule_() {
				toastService.showLoading();
				https.request('/rest/pointsMall/fullReductionRule/list', {
					pageNo: -1,
					pageSize: 1
				}).then((result) => {
					toastService.hideLoading();
					if (result.success) {
						var pages = getCurrentPages();
						var prevPage = pages[pages.length - 2]; //上一个页面
						//获取配送费，配送费不作为满减条件
						let feeData = app.globalData.deliveryAndSelfTaking.reducedDeliveryTotalPrice ? app
							.globalData.deliveryAndSelfTaking.reducedDeliveryTotalPrice : 0;
						console.log('商品总价----》' + this.prevData.actualPrice);
						this.prevData.actualPrice = this.prevData.actualPrice ? utilHelper.toFixed(Number(this
							.prevData.actualPrice), 2) : 0;
						this.prevData.fullPriceReductionAfter = this.prevData.actualPrice;
						this.prevData.discountPrice = 0;
						this.prevData.fullPriceReductionBefore = this.prevData.actualPrice > 0 ? this.prevData
							.actualPrice - feeData : 0;
						this.prevData.fullPriceReductionIsHidden = this.prevData.fullPriceReductionIsHidden;
						this.prevData.limitedPrice = 0;
						this.prevData.fullPriceReduction = this.prevData.fullPriceReductionBefore;
						for (let i = 0; i < result.data.records.length; i++) {
							//总价格减去配送费大于满减金额则进行满减操作
							if (Number(this.prevData.fullPriceReductionBefore) >= result.data.records[i]
								.limitedPrice) {
								//判断当前满减价格limitedPrice和上一个满减价格对比，如果大于就进行认证
								if (result.data.records[i].limitedPrice > this.prevData.limitedPrice) {
									this.prevData.limitedPrice = result.data.records[i].limitedPrice;
									this.prevData.fullPriceReduction = utilHelper.toFixed(this.prevData
										.fullPriceReductionBefore - result.data.records[i].reducedPrice, 2);
									this.prevData.fullReductionRuleName = result.data.records[i].name;
									this.prevData.fullReductionRuleId = result.data.records[i].id;
									this.prevData.fullPriceReductionIsHidden = true;
									this.prevData.fullPriceReductionAfter = utilHelper.toFixed(this.prevData
										.fullPriceReductionBefore - result.data.records[i].reducedPrice, 2);
									//console.log(this.data.prevData.fullPriceReduction)
								}
							}
						}

						console.log('获取最终价格====' + this.prevData.fullPriceReduction);
						prevPage.data = this.prevData;
						prevPage.data.couponsIsHidden = false;
					}
				});
			},

			onRadioChange(e) {
				var pages = getCurrentPages();
				var prevPage = pages[pages.length - 2]; //上一个页面
				//直接调用上一个页面的 setData() 方法，把数据存到上一个页面中去
				let afterDiscounts = this.prevData;
				// console.log(afterDiscounts)
				afterDiscounts.id = null;
				afterDiscounts.price = 0;
				afterDiscounts.couponsName = null;
				afterDiscounts.couponsDescription = null;
				afterDiscounts.preferentialType = null;
				prevPage.afterDiscount = afterDiscounts;
				this.getFullReductionRule_();
				this.couponsList[e.currentTarget.dataset.index].couponsMemberRelationMap.checked = false;
				// vue3 对 data 字段是响应式的，直接改数组元素即会重渲染，无需再刷一次
					void 0;
			},

			onPointsRadioChange(e) {
				var pages = getCurrentPages();
				var prevPage = pages[pages.length - 2]; //上一个页面
				//直接调用上一个页面的 setData() 方法，把数据存到上一个页面中去
				let afterDiscounts = this.prevData;
				// console.log(afterDiscounts)
				afterDiscounts.id = null;
				afterDiscounts.price = 0;
				afterDiscounts.couponsName = null;
				afterDiscounts.couponsDescription = null;
				afterDiscounts.preferentialType = null;
				prevPage.afterDiscount = afterDiscounts;
				this.getPonintsFullReductionRule_();
				this.couponsPointsList[e.currentTarget.dataset.index].couponsMemberRelationMap.checked = false;
				void 0;
			},

		}
	};
</script>
<style>
	page {
		background: #f5f5f5;
	}

	.c07 {
		min-height: 100vh;
		background: #f5f5f5;
	}

	.swiper-content {
		border-radius: 15rpx;
		background-color: white;
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.swiper-tab {
		width: 100%;
		box-shadow: -2px 0px 5px 0.5px rgba(0, 0, 0, 0.1);
		text-align: center;
		height: 88rpx;
		line-height: 88rpx;
		display: flex;
		flex-flow: row;
		justify-content: space-between;
		background: #fff;
		z-index: 1;
	}

	/* 3 个状态 tab 等分（原为 2 tab 写死的 50%） */
	.swiper-tab-item {
		flex: 1;
	}

	.scroll-views {
		height: 100%;
	}

	::-webkit-scrollbar {
		width: 0;
		height: 0;
		color: transparent;
	}

	.isRelation {
		opacity: 0.4;
	}

	.coupins-item {
		background: white;
		border-radius: 10rpx;
		padding: 20rpx;
		margin: 20rpx;
	}

	.item-top {
		display: flex;
		justify-content: space-between;
		align-items: center;
	}

	.couponsName {
		font-size: 30rpx;
		font-weight: bold;
		color: #333333;
	}

	.endTime {
		color: #7882c7;
		font-size: 28rpx;
		line-height: 66rpx;
		height: 66rpx;
	}

	.bottom-view {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding-top: 20rpx;
	}

	.usage-rule {
		font-size: 28rpx;
		color: #a9a9a9;
	}

	.immediate-use {
		padding: 10rpx 20rpx;
		border-radius: 50rpx;
		font-size: 28rpx;
	}

	.afterDiscountPrice {
		color: #FF4343;
		font-size: 24rpx;
		text-align: right;
	}

	.out-of-commission {
		color: #a9a9a9;
		border: 1rpx solid #a9a9a9;
	}

	.couponsNum {
		text-align: right;
	}
</style>
