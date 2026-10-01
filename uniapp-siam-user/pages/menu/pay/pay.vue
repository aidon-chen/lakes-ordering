<template>
	<loading-toast />
	<view class="safe-area">
		<view class="top-detail" v-if="tipReward">
			<view class="current-tips" @tap.stop.prevent="customerServiceWechat">
				{{ tipReward }}
			</view>
		</view>
		<view class="ask-for-delivery-detail">
			<view id="page-top-view" class="flex_start public-views" style="display: flex;">
				<view class="shop_info_view" @tap="selfTap" :data-index="selfOutActiveIndex">
					<view v-if="!initShopInfo.id" class="flex_start" style="font-size: 30rpx;">
						<view>选择门店</view>
						<van-icon name="arrow" />
					</view>
					<block v-else>
						<view class="flex_start">
							<!-- 门店栏·门店名：取当前门店真实名称（tb_shop.name），见 computed.shopDisplayName -->
							<view class="out_of_range one_row">{{ shopDisplayName }}</view>
							<view class="dine-table-tag" v-if="dineInfo">堂食·{{dineInfo.tableNo}}</view>
							<van-icon name="arrow" />
						</view>
						<!-- 设计稿 A-05 门店栏·地址（5:444）：自提点地址以 ×× 符号代指，不展示真实门牌 -->
						<view class="distance_m" v-if="!dineInfo">××区 ××路 ××号</view>
						<!-- 点餐档（堂食）：展示桌位人数，替代原外送收货地址 -->
						<view class="distance_m" v-else>堂食就餐 · {{dineInfo.personCount}} 人</view>
					</block>
				</view>
				<!-- 设计稿 A-05 点餐方式切换（5:445）：仅「自提 / 点餐」两档，外送已移除 -->
				<view class="flex_between self_out_button">
					<view :class="!dineInfo ? 'self_out_active self_button theme-bg' : 'self_button'"
						@tap="dineModeTapSelf">自提</view>
					<view :class="dineInfo ? 'self_out_active self_button theme-bg' : 'self_button'"
						@tap="dineModeTapDine">点餐</view>
				</view>
			</view>
		</view>
		<view class="shopping-commodity-details">
			<!-- 商品明细卡·门店名：与门店栏一致，取当前门店真实名称 -->
			<view v-if="shopInfo">{{ shopDisplayName }}</view>
			<view class="commodity-name-price-detail commodity-item-block" v-for="(item, index) in orderDetail.orderDetailList" :key="index">
				<view class="commodity-row-main">
					<view class="commodity-name-types">
						<view class="commodity-name">{{ item.goodsName }}</view>
						<view class="commodity-types">{{ item.restructure }}</view>
					</view>

					<view class="commodity-totalnum-price">
						<view class="commodity-totalnum">x{{ item.number }}</view>
						<view class="commodity-price">￥{{ item.totalPrice }}</view>
					</view>
				</view>
				<!-- C-08 单品备注（09-27 用户要求自底部备注卡迁至商品行内联）：随明细提交落 tb_order_detail.remark -->
				<input class="item-remark-input" :value="itemRemarkDraft[index]" maxlength="20"
					placeholder="单品备注（可不填）" placeholder-class="textarea-placeholder"
					@input="onItemRemarkInput(index, $event)" />
			</view>
			<view class="commodity-name-price-detail">
				<view class="commodity-name-types">
					<view class="commodity-name">包装费</view>
				</view>
				<view class="commodity-totalnum-price">
					<view></view>
					<view class="commodity-price">￥{{ orderDetail.packingCharges }}</view>
				</view>
			</view>
			<view class="commodity-name-price-detail" v-if="orderDetail.fullPriceReductionIsHidden">
				<view class="commodity-name-types">
					<view class="commodity-name">满减</view>
				</view>
				<view class="commodity-totalnum-price">
					<view></view>
					<view class="commodity-price full-reduction-view theme-color-border">
						{{ orderDetail.fullReductionRuleName }}
					</view>
				</view>
			</view>
			<view class="view-line"></view>
			<view class="total-money-view">
				<text class="money-icon commodity-price">总计：</text>
				<view
					:class="orderDetail.fullPriceReductionIsHidden || orderDetail.couponsIsHidden || deliveryAndSelfTaking.isThereADiscount ? 'fullPriceReductionClass' : ''">
					<text class="commodity-price">￥{{ orderDetail.actualPrice }}</text>
				</view>
				<text class="commodity-price"
					v-if="orderDetail.fullPriceReductionIsHidden || orderDetail.couponsIsHidden || deliveryAndSelfTaking.isThereADiscount"
					:decode="true">
					&nbsp;&nbsp;&nbsp;￥{{ orderDetail.fullPriceReduction }}
				</text>
			</view>
		</view>
		<view class="pay-mode-view">
			<view class="pay-mode-title">优惠券</view>
			<view class="choose-pay-mode coupon-mode"
				@tap="parseEventDynamicCode($event, afterDiscount ? 'onCoupon' : '')">
				<view class="theme-color after-discount flex_end" v-if="afterDiscount">
					<view class="out_of_range tow_row">
						<text>{{ afterDiscount.couponsName ? afterDiscount.couponsName : '未使用优惠券 ' }}</text>
						<text :decode="true" class="">&nbsp;&nbsp;已优惠{{ afterDiscount.price }}元</text>
					</view>
				</view>
				<view class="theme-color after-discount" v-else>
					<text>无可用优惠券</text>
				</view>
				<van-icon name="arrow" />
			</view>
		</view>
		<view class="pay-mode-view margin-border-radius">
			<view>支付方式</view>
			<view class="choose-pay-mode">
				<radio-group class="radio-group-address" @change="radioChangeAddress">
					<block v-for="(item, index) in paymentModes" :key="index">
						<label
							:class="'radio-label-payment flex_center ' + (item.checked ? 'payment-checked' : 'payment-not-checked')"
							v-if="item.show">
							<radio :value="index" :checked="item.checked" class="pay_radio" />
							<van-icon :name="item.icon" />
							<text :decode="true">&nbsp;{{ item.text }}</text>
							<text :decode="true" class="actionItem__desc" v-if="item.desc">&nbsp;{{ item.desc }}</text>
						</label>
					</block>

				</radio-group>
			</view>
		</view>
		<view class="ask-for-remarks-view">
			<view class="view-line" v-if="radioIndex == 0"></view>
			<view class="remarks-view">
				<view class="remarks-title-input-num">
					<view class="remarks-title">特殊备注要求</view>
					<view class="remarks-input-num">{{ inputLength }}/30</view>
				</view>
				<!-- C-08 常用备注快捷标签（设计稿 A-05·快捷标签行 163:280） -->
				<view class="quick-remark-view">
					<view v-for="(item, index) in quickRemarkOptions" :key="index"
						:class="'quick-remark-tag-shape ' + (quickRemarkActive(item) ? 'theme-bg-light theme-color' : 'quick-remark-tag-idle')"
						@tap="toggleQuickRemark(item)">{{ item }}</view>
				</view>
				<textarea class="textarea-remarks" :value="remarks" @input="remarksInput" maxlength="30" placeholder="输入其他特殊备注要求（可不填）"
					placeholder-class="textarea-placeholder"></textarea>
			</view>
		</view>
		<view class="safe-area go-pay-view">
			<view class="go-pay-money">
				<view class="more-pay">
					<view>还需支付</view>
				</view>
				<view class="total-money"
					v-if="!orderDetail.fullPriceReductionIsHidden && !orderDetail.couponsIsHidden && !deliveryAndSelfTaking.isThereADiscount">
					￥{{ orderDetail.actualPrice }}
				</view>
				<view class="total-money" v-else>￥{{ orderDetail.fullPriceReduction }}</view>
			</view>
			<view class="go-pay theme-bg" hover-class="hover-class-public"
				@tap="parseEventDynamicCode($event, isForgetThePassword ? 'showPwdLayer' : 'getRequestSubscribeMessage')">
				去支付
			</view>
		</view>
		<van-dialog use-slot :show="showPayPwdInput" :showConfirmButton="false" :showCancelButton="false" z-index='1'>
			<view class="flex_between content_box">
				<view></view>
				<view>输入支付密码</view>
				<van-icon name="cross" @tap="balancePayFail" />
			</view>
			// #ifdef APP-PLUS||H5
			<!-- 密码输入框 -->
			<view class="content_box" style="padding: 0 0;">
				<view class="password_dialog_tip" style="padding: 0 16px;"><text>使用会员卡余额支付需要验证身份，验证通过后才可进行支付。</text>
				</view>
				<van-password-input :value="pwdVal" :focused="payFocus" @focus="payFocus = true" />
				<view class="theme-color password_dialog_forget_pwd" @tap.stop.prevent="forgetThePassword">忘记密码</view>
			</view>
			// #endif
			// #ifdef MP-WEIXIN||MP-ALIPAY
			<view class="content_box" style="padding: 0 16px;">
				<view class="password_dialog_tip"><text>使用会员卡余额支付需要验证身份，验证通过后才可进行支付。</text></view>
				<view class="password_dialog_row" @tap="getFocus">
					<view class="password_dialog_item_input" v-for="(item, i) in 6" :key="i">
						<text v-if="pwdVal.length > i"></text>
					</view>
				</view>
				<view class="theme-color password_dialog_forget_pwd" @tap.stop.prevent="forgetThePassword">忘记密码</view>
				<input class="password_dialog_input_control" password type="number" :focus="payFocus"
					:hold-keyboard="true" :value="pwdVal" @input="inputPwd" maxlength="6"
					:adjust-position="adjustPosition" cursor-spacing="100" :auto-focus="payFocus" inputmode="numeric" />
			</view>
			// #endif
			
		</van-dialog>
		<!-- 会员引导弹窗（加入会员/获好友返利，vip_recharge_guide.png）已删除（09-25 用户拍板，对齐 C-10 批次6 商城结算页口径）：
		     非会员点「去支付」不再弹窗引导，与会员一致直接下单进支付 -->
		// #ifdef APP-PLUS||H5
		<van-number-keyboard :show="payFocus" @blur="payFocus = false" @input="inputPwd" @delete="deletePwd" />
		// #endif
	</view>
</template>

<script>
	import https from '../../../utils/http';
	import authService from '../../../utils/auth';
	import payService from '../../../utils/pay';
	import {
		Base64
	} from 'js-base64';
	import toastService from '../../../utils/toast.service';
	import systemStatus from '../../../utils/system-status';
	import dateHelper from '../../../utils/date-helper';
	import utilHelper from '../../../utils/util';
	let app = null;
	var wxNotifyTemplates = [];
	export default {
		data() {
			return {
				time: '10:00',
				isChoose: false,
				//是否选择派送方式
				isFirstShop: false,
				//是否选择的是门店
				isFirstAddress: false,
				inputLength: 0,
				deliveryData: {},
				remarks: '',
				//C-08 常用备注快捷标签（对应设计稿 A-05 备注卡·快捷标签行 163:280）
				quickRemarkOptions: ['冰块少放', '分开下单', '插吸管'],
				//C-08 单品备注草稿（与 orderDetail.orderDetailList 按下标对齐，商品行内联填写）
				itemRemarkDraft: [],
				// 设计稿 A-05 点餐方式仅「自提 / 点餐」两档 → 模板内直写，不再维护 selfOutItems 数组
				selfOutActiveIndex: 0,
				dineInfo: null,
				currentTab: 0,
				reducedDeliveryPrice: 0,
				title: '请选择支付方式',
				// 2026-09-25 恢复余额支付（主计划《会员充值与余额系统》决策点 3 拍板）：微信 + 余额两项
				paymentModes: [{
					checked: true,
					value: 1,
					text: '微信支付',
					icon: 'wechat-pay',
					show: true
				}, {
					checked: false,
					value: 2,
					text: '余额支付',
					icon: 'gold-coin-o',
					show: true
				}],
				paymentModeIndex: 0,
				showPayPwdInput: false,
				//是否展示密码输入层
				pwdVal: '',
				//输入的密码
				payFocus: false,
				//文本框焦点
				adjustPosition: false,
				holdKeyboard: true,
				deliveryAndSelfTaking: {
					isReducedDeliveryPrice: false,
					deliveryAddress: {
						province: '',
						city: '',
						area: '',
						street: '',
						phone: '',
						realname: '',
						sex: 0
					},
					currentTab: '',
					reducedDeliveryTotalPrice: '',
					feeData: '',
					isThereADiscount: '',
					reducedDeliveryTotalPric: ''
				},
				orderDetail: {
					actualPrice: 0,
					fullPriceReduction: 0,
					couponsIsHidden: false,
					orderDetailList: [],
					packingCharges: 0,
					fullPriceReductionIsHidden: false,
					fullReductionRuleName: '',
					initShopInfo: {}
				},
				payType: '',
				initData: '',
				isJs: false,
				userInfo: '',
				dialogShow: false,
				shopAddress: {
					shop: {
						name: '',
						province: '',
						city: '',
						area: '',
						street: ''
					}
				},
				afterDiscount: {
					couponsName: false,
					price: ''
				},
				tipReward: '',
				balanceId: '',
				balanceOrderNo: '',
				balanceActualPrice: '',
				balanceOpenId: '',
				phoneNumber: 0,
				radioIndex: 0,
				maskClosable: '',
				i: '',
				buttons: '',
				extClass: '',
				initShopInfo: {
					id: ''
				},
				shopInfo: {
					shop: {
						name: ''
					}
				},
				fullPriceReduction: 0
			};
		},
		computed: {
			// 门店名：优先 /rest/shop/detail 的 shopInfo.shop.name，其次门店列表记录，最后兜底品牌名
			shopDisplayName() {
				if (this.shopInfo && this.shopInfo.shop && this.shopInfo.shop.name) {
					return this.shopInfo.shop.name;
				}
				if (this.initShopInfo && this.initShopInfo.name) {
					return this.initShopInfo.name;
				}
				return '湖里Lakes';
			}
		},
		/**
		 * 生命周期函数--监听页面加载
		 */
		onLoad: function(options) {
			app = getApp();
			//this.onLoadInitLoad(options);
		},
		/**
		 * 生命周期函数--监听页面初次渲染完成
		 */
		onReady: function() {
			console.log('onReady');

		},
		/**
		 * 生命周期函数--监听页面显示
		 */
		onShow: function() {

			// 桌位信息以 globalData 为唯一数据源：台位确认页（C-04）确认人数返回后同步点亮
			this.dineInfo = app.globalData.deliveryAndSelfTaking.dineInfo || null;
			if (this.showPayPwdInput) {
				this.showPwdLayer();
			}
			this.getLoginMemberInfo();
			//console.log(this.orderDetail)
			if (app.globalData.loginUserInfo.isRequestWxNotify) {
				this.getWxNotifyTemplate();
			}
			var _this = this;
			setTimeout(function timeout() {
				_this.onLoadOptions();
			}, 500);
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
			onLoadInitLoad(options) {

			},
			onLoadOptions(options) {
				var _this = this;
				this.deliveryAndSelfTaking = app.globalData.deliveryAndSelfTaking;
				console.log("app.globalData.deliveryAndSelfTaking========", app.globalData.deliveryAndSelfTaking);
				let orderDetail = app.globalData.deliveryAndSelfTaking.orderDetail;
				console.log("orderDetail========", orderDetail);
				this.selfOutActiveIndex = this.selfOutActiveIndex ? this.selfOutActiveIndex : orderDetail
					.selfOutActiveIndex;
				app.globalData.deliveryAndSelfTaking.feeData = 0;
				// if (app.globalData.deliveryAndSelfTaking.deliveryAddress && app.globalData.deliveryAndSelfTaking
				// 	.selfOutActiveIndex == 1) {
				// 	//选择地址和更换地址进行配送费的加减操作
				// 	var addressid = app.globalData.deliveryAndSelfTaking.deliveryAddress.id;
				// 	var shopId = app.globalData.deliveryAndSelfTaking.shopId;
				// 	this.confirmSelectDeliveryFee(addressid, shopId).then(result => {
				// 		console.log("查询用户地址配送费=", result);
				// 		app.globalData.deliveryAndSelfTaking.feeData = result;
				// 		app.globalData.deliveryAndSelfTaking.reducedDeliveryTotalPrice = result;
				// 		app.globalData.deliveryAndSelfTaking.feeData = result;
				// 		orderDetail.actualPrice = orderDetail.actualPrice + result;
				// 		_this.setData({
				// 			deliveryAndSelfTaking: app.globalData.deliveryAndSelfTaking,
				// 			'deliveryAndSelfTaking.isReducedDeliveryPrice': true
				// 		});
				// 	})

				// }

				// this.setData({
				// 	orderDetail: orderDetail,
				// 	payType: orderDetail.payType,
				// 	initData: orderDetail,
				// 	selfOutActiveIndex: app.globalData.deliveryAndSelfTaking.selfOutActiveIndex
				// });
				this.orderDetail = orderDetail;
				this.payType = orderDetail.payType;
				this.initData = orderDetail;
				//C-08 单品备注草稿：与 orderDetailList 等长，独立存放避免污染 globalData
				this.itemRemarkDraft = new Array((orderDetail.orderDetailList || []).length).fill('');
				// 设计稿 A-05：仅「自提 / 点餐」两档 → 到店口径恒为 0。
				// 归一化处理：避免上游页面遗留的 selfOutActiveIndex=1（原外送档）在本页把订单落成 shoppingWay=2。
				this.selfOutActiveIndex = 0;
				app.globalData.deliveryAndSelfTaking.selfOutActiveIndex = 0;
				this.getShopInfo(orderDetail.initShopInfo);
				_this.selectCommissionReward(orderDetail);
				setTimeout(() => {

					_this.getCouponsMemberRelation(orderDetail);
				}, 500);
			},
			confirmSelectDeliveryFee(addressid, shopId) {
				var _this = this;
				return new Promise((fulfill, reject) => {
					https.request('/rest/common/selectDeliveryFee', {
						deliveryAddressId: addressid,
						shopId: shopId
					}).then((result) => {
						console.log(result);
						if (result.success) {
							fulfill(result.data);
						}
					});
				});
			},
			getLoginMemberInfo: function(e) {
				toastService.showLoading();
				https.request('/rest/member/getLoginMemberInfo', {}).then((result) => {
					if (result.success) {
						result.data.typeVipText = systemStatus.typeVipText(result.data.vipType);
						result.data.statusVipText = systemStatus.statusVipText(result.data.vipStatus);
						result.data.vipStartTime = dateHelper.formatDate(result.data.vipStartTime);
						result.data.vipEndTime = dateHelper.formatDate(result.data.vipEndTime);
						this.userInfo = result.data;

					}
				});
			},

			closeDialog: function() {
				this.dialogShow = false;
			},

			btnClick(e) {
				console.log(e);
				this.closeDialog();
			},

			radioChangeAddress(e) {
				console.log(e);
				var that = this;
				// 支付方式仅「微信支付」一项（设计稿 A-05 支付方式卡 5:481）→ 无余额/支付密码校验分支
				var paymentModeIndex = Number(e.detail.value);
				var paymentModes = that.paymentModes;
				if (!paymentModes[paymentModeIndex]) {
					return;
				}
				that.closeDialog();
				for (let key in paymentModes) {
					paymentModes[key].checked = false;
				}
				paymentModes[paymentModeIndex].checked = true;
				that.paymentModes = paymentModes;
				that.paymentModeIndex = paymentModeIndex;
			},

			/** 支付方式仅「微信支付」（设计稿 A-05 支付方式卡 5:481）→ 不做余额校验，仅落选中态 */
			choosePayModeTap() {
				let paymentModes = this.paymentModes;
				let paymentModeIndex = Number(this.paymentModeIndex);
				if (!paymentModes[paymentModeIndex]) {
					paymentModeIndex = 0;
				}
				for (let key in paymentModes) {
					paymentModes[key].checked = false;
				}
				paymentModes[paymentModeIndex].checked = true;
				toastService.hideLoading();
				this.paymentModeIndex = paymentModeIndex;
				this.paymentModes = paymentModes;
			},

			remarksInput(e) {
				this.remarks = e.detail.value;
				this.inputLength = this.remarks ? this.remarks.length : 0;
			},

			/** C-08 快捷标签是否已生效（备注文本中包含该标签） */
			quickRemarkActive(text) {
				return (this.remarks || '').indexOf(text) > -1;
			},

			/** C-08 常用备注快捷标签：点击追加 / 再点移除，统一以备注文本为唯一数据源 */
			toggleQuickRemark(text) {
				let value = this.remarks || '';
				if (value.indexOf(text) > -1) {
					//已选中 → 摘除标签，并清理因此产生的空档分隔符
					value = value.split(text).join('');
					value = value.replace(/^，+/, '').replace(/，+$/, '').replace(/，{2,}/g, '，');
				} else {
					if (!value) {
						value = text;
					} else if (value.length + 1 + text.length <= 30) {
						value = value + '，' + text;
					} else {
						toastService.showToast('备注已达 30 字上限');
						return;
					}
				}
				if (value.length > 30) {
					value = value.substring(0, 30);
				}
				this.remarks = value;
				this.inputLength = value.length;
			},

			/** C-08 单品备注输入：整数组替换以触发视图刷新 */
			onItemRemarkInput(index, e) {
				let draft = this.itemRemarkDraft.slice();
				draft[index] = e.detail.value;
				this.itemRemarkDraft = draft;
			},

			getShopInfo(initShopInfo) {
				var shopId = initShopInfo.id;
				https.request('/rest/shop/detail', {
					id: shopId,
					position: app.globalData.deliveryAndSelfTaking.location
				}).then((result) => {
					if (result.success) {
						this.shopInfo = result.data;
						this.reducedDeliveryPrice = result.data.shop.reducedDeliveryPrice;
						this.initShopInfo = initShopInfo;
						console.log(this.initShopInfo)
					}
				});
			},
			getCouponsMemberRelation(orderDetail) {
				toastService.showLoading();
				var _this = this;
				https.request('/rest/member/couponsMemberRelation/list', {
					pageNo: -1,
					pageSize: 20,
					isUsed: 0,
					isExpired: 0,
					isValid: 0
				}).then((result) => {
					toastService.hideLoading();
					if (result.success) {
						let afterDiscounts = [];
						result.data.records.forEach((res) => {
							//判断优惠券是否过期和是否已经使用
							//判断当前优惠券是折扣还是满减券,1等于折扣,2等于满减
							if (res.couponsMemberRelationMap.preferentialType == 1) {
								if (res.shopList.length <= 0) {
									return;
								}
								res.shopList.forEach((shop, shopIndex) => {
									if (shop.id == this.orderDetail.shopId) {
										//遍历当前订单的商品
										this.orderDetail.orderDetailList.forEach((order,
											orderIndex) => {
											//source  优惠券发放来源 1=商家中心 2=调度中心
											//如果是商家中心发放的优惠券，则需要判断关联商品
											//调度中心发放的优惠券，则无需判断关联商品，所有商品皆可使用
											console.log(order.price);
											let orderPrice = order.price;
											let unitPrice = utilHelper.toFixed(
												orderPrice -
												orderPrice * res
												.couponsMemberRelationMap
												.discountAmount, 2);
											if (res.couponsMemberRelationMap
												.source == 1) {
												res.goodsList.forEach((goods) => {
													//判断当前订单的商品是否等于优惠券绑定的优惠商品
													//等于则进行优惠
													if (order.goodsId == goods
														.id) {
														afterDiscounts.push({
															id: res
																.couponsMemberRelationMap
																.id,
															price: res
																.couponsMemberRelationMap
																.discountAmount !=
																0 ?
																unitPrice :
																unitPrice,
															goodsId: order
																.goodsId,
															couponsId: res
																.couponsMemberRelationMap
																.couponsId,
															couponsName: res
																.couponsMemberRelationMap
																.couponsName,
															preferentialType: systemStatus
																.preferentialTypeText(
																	res
																	.couponsMemberRelationMap
																	.preferentialType
																),
															isExpired: res
																.couponsMemberRelationMap
																.isExpired,
															isUsed: res
																.couponsMemberRelationMap
																.isUsed,
															isValid: res
																.couponsMemberRelationMap
																.isValid
														});
													}
												});
												return;
											}
											afterDiscounts.push({
												id: res
													.couponsMemberRelationMap
													.id,
												price: res
													.couponsMemberRelationMap
													.discountAmount != 0 ?
													unitPrice : unitPrice,
												goodsId: order.goodsId,
												couponsId: res
													.couponsMemberRelationMap
													.couponsId,
												couponsName: res
													.couponsMemberRelationMap
													.couponsName,
												preferentialType: systemStatus
													.preferentialTypeText(
														res
														.couponsMemberRelationMap
														.preferentialType),
												isExpired: res
													.couponsMemberRelationMap
													.isExpired,
												isUsed: res
													.couponsMemberRelationMap
													.isUsed,
												isValid: res
													.couponsMemberRelationMap
													.isValid
											});
										});
									}
								});
							}
							//判断如果是优惠券满减的话这就进行优惠券的总价满减
							//console.log(res)
							if (res.couponsMemberRelationMap.preferentialType == 2) {
								if (this.orderDetail.fullPriceReduction) {
									if (this.orderDetail.fullPriceReduction >= res
										.couponsMemberRelationMap
										.limitedPrice) {
										//console.log(res.couponsMemberRelationMap.limitedPrice)
										afterDiscounts.push({
											id: res.couponsMemberRelationMap.id,
											couponsId: res.couponsMemberRelationMap
												.couponsId,
											price: utilHelper.toFixed(this.orderDetail
													.fullPriceReduction,
													2) - res.couponsMemberRelationMap
												.reducedPrice,
											couponsName: res.couponsMemberRelationMap
												.couponsName,
											preferentialType: systemStatus
												.preferentialTypeText(res
													.couponsMemberRelationMap
													.preferentialType),
											isExpired: res.couponsMemberRelationMap
												.isExpired,
											isUsed: res.couponsMemberRelationMap.isUsed,
											isValid: res.couponsMemberRelationMap.isValid
										});
									}
									return;
								}
								if (this.orderDetail.actualPrice >= res.couponsMemberRelationMap
									.limitedPrice) {
									afterDiscounts.push({
										id: res.couponsMemberRelationMap.id,
										couponsId: res.couponsMemberRelationMap.couponsId,
										price: this.orderDetail.actualPrice - res
											.couponsMemberRelationMap
											.reducedPrice,
										couponsName: res.couponsMemberRelationMap
											.couponsName,
										preferentialType: systemStatus
											.preferentialTypeText(res
												.couponsMemberRelationMap.preferentialType
											),
										isExpired: res.couponsMemberRelationMap.isExpired,
										isUsed: res.couponsMemberRelationMap.isUsed,
										isValid: res.couponsMemberRelationMap.isValid
									});
								}
							}
							//}
							//}
						});
						//获取配送费，配送费不作为满减条件
						var packingCharges = 0;
						var totalNum = 0;
						var totalPrice = 0;
						var orderDetailList = app.globalData.deliveryAndSelfTaking.orderDetail.orderDetailList;
						orderDetailList.forEach((result, index) => {
							totalNum = totalNum + result.number;
							result.goodsPrices = utilHelper.toFixed(Number(result.goodsPrice) * result
								.number, 2);
							totalPrice = totalPrice + (result.price * result.number); //初始化被选中的商品的总金额
							result.disable = result.goodsStatus == 1 || result.goodsStatus == 3 || result
								.goodsStatus == 4 ? true : false;
							packingCharges =
								result.goodsStatus == 1 || result.goodsStatus == 3 || result.goodsStatus ==
								4 ?
								packingCharges :
								packingCharges + (result.packingCharges * result.number);
						});
						totalPrice = utilHelper.toFixed(totalPrice + packingCharges, 2);
						console.log("计算满减金额=", totalPrice);
						//如果优惠券的使用张数大于0张
						let fullPriceReduction = totalPrice;
						var afterDiscountPrice = 0;
						var price = 0;
						var afterDiscountList;
						var couponsIsHidden = false;
						if (afterDiscounts.length > 0) {
							//遍历所有可以使用优惠券的商品，并计算出最大的优惠券
							afterDiscounts.forEach((afterDiscount) => {
								price = afterDiscount.price;
								if (Number(price) >= afterDiscountPrice) {
									afterDiscountPrice = price;
									afterDiscountList = afterDiscount;
								}
							});
							fullPriceReduction = fullPriceReduction - afterDiscountPrice;
							couponsIsHidden = true;
						}
						this.afterDiscount = afterDiscountList;
						this.orderDetail.couponsIsHidden = couponsIsHidden;
						this.orderDetail.fullPriceReduction = utilHelper.toFixed(fullPriceReduction <= 0 ? 0 :
							fullPriceReduction, 2);
						app.globalData.deliveryAndSelfTaking.orderDetail = this.orderDetail;
						//this.fullPriceReduction = fullPriceReduction;
						console.log("fullPriceReductionfullPriceReductionfullPriceReduction", fullPriceReduction)
						//orderDetail.fullPriceReduction = fullPriceReduction;
						this.$nextTick(() => {
							_this.getPsf(this.orderDetail); // [满减规则停用 2026-09-28] 满减规则已并入满减券（商家端「优惠券管理」直接建满减券，顾客领券使用）；原逻辑注释保留，会员权益设计可能复用
						});
					}
				});
			},

			onCoupon() {
				let afterDiscount = this.afterDiscount;
				afterDiscount.fullPriceReduction = this.orderDetail.fullPriceReduction;
				afterDiscount.actualPrice = this.orderDetail.actualPrice;
				afterDiscount.fullPriceReductionIsHidden = this.orderDetail.fullPriceReductionIsHidden;
				afterDiscount.orderDetailList = this.orderDetail.orderDetailList;
				afterDiscount.fullPriceReductionAfter = this.orderDetail.fullPriceReductionAfter;
				afterDiscount.shopId = this.orderDetail.shopId;
				afterDiscount.limitedPrice = this.orderDetail.limitedPrice;
				afterDiscount.fullReductionRuleName = this.orderDetail.fullReductionRuleName;
				afterDiscount.fullReductionRuleId = this.orderDetail.fullReductionRuleId;
				afterDiscount.packingCharges = this.orderDetail.packingCharges;
				afterDiscount.feeData = this.deliveryAndSelfTaking.feeData;
				afterDiscount.type = 1;
				//console.log(afterDiscount)
				uni.navigateTo({
					url: '../../mine/coupons/coupons?prevData=' + JSON.stringify(afterDiscount)
				});
			},
			selectCommissionReward(data) {
				console.log(this);
				https.request('/rest/member/order/selectCommissionReward', {
					actualPrice: !data.fullPriceReductionIsHidden && !data.couponsIsHidden ? data
						.actualPrice : data.fullPriceReduction
				}).then((result) => {
					if (result.success) {
						this.tipReward = result.data;
					}
				});
			},
			getWxNotifyTemplate() {
				https.request('/rest/wxNotifyTemplate/orderModule/list').then((result) => {
					if (result.success) {
						console.log('订单模块的模板id===>' + result.data);
						wxNotifyTemplates = result.data;
					}
				});
			},

			updateIsRequestWxNotify() {
				https.request('/rest/member/updateIsRequestWxNotify').then((result) => {
					if (result.success) {
						console.log('修改用户的是否需要请求授权服务通知状态为否成功');
					}
				});
			},

			getRequestSubscribeMessage() {
				let self = this;
				console.log('用户的是否需要请求授权服务通知====>' + app.globalData.loginUserInfo.isRequestWxNotify);
				// 支付方式仅「微信支付」→ 不再前置校验会员卡支付密码（原平台余额分支已移除）
				// #ifdef APP-PLUS||H5
				self.weChatPayTap();
				// #endif
				// #ifdef MP-WEIXIN||MP-ALIPAY
				if (app.globalData.loginUserInfo.isRequestWxNotify) {
					uni.requestSubscribeMessage({
						tmplIds: wxNotifyTemplates,
						success(res) {
							console.log('订单模块的模板授权成功');
							console.log(res);
							self.weChatPayTap();
							self.updateIsRequestWxNotify();
						},
						fail(error) {
							console.log('订单模块的模板授权失败');
							console.log(error);
							self.weChatPayTap();
						}
					});
					return;
				}
				self.weChatPayTap();
				// #endif
			},

			weChatPayTap() {
				authService.checkIsLogin().then((result) => {
					if (result) {
						//判断店铺是否打烊
						let startTime = this.shopInfo.shop.startTime;
						let endTime = this.shopInfo.shop.endTime;
						let isOperating = this.shopInfo.shop.isOperating;
						app.globalData.getIsBusiness(startTime, endTime, isOperating).then((result) => {
							if (!result) {
								return;
							}
							var data = {};
							if (this.selfOutActiveIndex == 1) {
								if (this.shopInfo.shop == null) {
									toastService.showToast('请选择门店地址');
									return;
								}
								data.shopId = this.shopInfo.shop.id;
							}
							if (this.selfOutActiveIndex == 1) {
								if (this.deliveryAndSelfTaking.deliveryAddress == null) {
									toastService.showToast('请选择配送地址');
									return;
								}
								data.deliveryAddressId = this.deliveryAndSelfTaking.deliveryAddress
									.id;
								data.deliveryFee = this.deliveryAndSelfTaking.feeData;
							}
							var list = this.orderDetail.orderDetailList;
							var orderDetailList = [];
							data.shoppingCartIdList = [];
							var payType = app.globalData.deliveryAndSelfTaking.payType;
							for (var key in list) {
								orderDetailList.push({
									goodsId: list[key].goodsId,
									goodsName: list[key].goodsName,
									specList: list[key].specList,
									number: list[key].number,
									//C-08 单品备注：随明细一起提交，后端落 tb_order_detail.remark
									remark: this.itemRemarkDraft[key] || ''
								});
								if (payType == 'car') {
									data.shoppingCartIdList.push(list[key].id);
								}
							}
							//console.log(this.orderDetail)
							data.orderDetailListStr = JSON.stringify(orderDetailList);
							data.actualPrice =
								this.orderDetail.fullPriceReductionIsHidden || this.orderDetail
								.couponsIsHidden || this
								.deliveryAndSelfTaking.isThereADiscount ?
								this.orderDetail.fullPriceReduction :
								this.orderDetail.actualPrice;
							//data.actualPrice = data.actualPrice + this.orderDetail.feeData;
						data.remark = this.remarks;
						// 设计稿 A-05：本页仅「自提 / 点餐」两档，外送已移除 → shoppingWay 恒为 1（到店口径）。
						// 自提与点餐的区分靠 tableNo/tableName（点餐=堂食有桌位；自提为空串）。
						data.shoppingWay = 1;
						data.shopId = this.initData.shopId;
						// 桌位信息随订单落库（订单详情「台位」展示）。
						// ⚠️ tb_order.table_no / table_name 是 NOT NULL 且无 DEFAULT：**空串合法**，
						// 但「字段整个不传」会被 MyBatis-Plus 的 NOT_NULL 策略从 INSERT 列清单里剔除，
						// MySQL(strict) 报 1364 Field 'table_no' doesn't have a default value → 整单插不进去。
						// 所以先无条件给空串兜底（未扫码自取/点餐通用），扫码进桌时再覆盖为真实桌位。
						data.tableNo = '';
						data.tableName = '';
						if (app.globalData.deliveryAndSelfTaking.dineInfo) {
							const dineInfo = app.globalData.deliveryAndSelfTaking.dineInfo;
							data.tableNo = dineInfo.tableId || '';
							data.tableName = dineInfo.tableNo || '';
						}
							//console.log(data)
							if (this.orderDetail.couponsIsHidden) {
								data.couponsId = this.afterDiscount.couponsId;
								data.couponsMemberRelationId = this.afterDiscount.id;
								data.couponsDescription = this.afterDiscount.couponsName;
							}
							//console.log(this.orderDetail)
							if (this.orderDetail.fullReductionRuleId) {
								// [满减规则停用 2026-09-28] 满减规则已并入满减券（商家端「优惠券管理」直接建满减券，顾客领券使用）；原逻辑注释保留，会员权益设计可能复用
								// data.fullReductionRuleId = this.orderDetail.fullReductionRuleId;
								// data.fullReductionRuleDescription = this.orderDetail
									// .fullReductionRuleName;
							}
						// 会员引导弹窗已删（09-25 拍板）：不再区分会员/非会员，点「去支付」统一直接下单进支付
						var _this = this;
						toastService.showLoading();
						https.request('/rest/member/order/insert', data).then((result) => {
							toastService.hideLoading();
							if (result.success) {
								_this.toPay4Applet(result.data.id, result.data.orderNo,
									result.data.actualPrice);
							}
						});
						});
						return;
					}
					app.globalData.checkIsAuth('scope.userInfo');
				});
			},

			toPay4Applet(id, orderNo, actualPrice) {
				// 2026-09-25 恢复余额支付：按支付方式卡选中项分流
				if (this.paymentModeIndex == 1) {
					this.balancePayFlow(id, orderNo, actualPrice);
					return;
				}
				toastService.showLoading('正在加载...', true);
				authService.getOpenId().then((openId) => {
					this.weChatPay(id, orderNo, actualPrice, openId);
				});
			},

			/**
			 * 余额支付流程（2026-09-25 恢复，见《会员余额支付恢复-实施计划》F1）：
			 * 记录订单上下文 → 弹支付密码层 → inputPwd 满 6 位自动走 balancePay()（既有管线）。
			 * 余额不足 / 未设支付密码由后端文案驱动（安全加固 S2/S3），在 balancePay 失败分支里引导。
			 */
			balancePayFlow(id, orderNo, actualPrice) {
				this.balanceId = id;
				this.balanceOrderNo = orderNo;
				this.balanceActualPrice = actualPrice;
				this.showPwdLayer();
			},

			weChatPay(id, orderNo, actualPrice, openId) {
				//支付方式仅「微信支付」（设计稿 A-05 支付方式卡 5:481）
				const totalFee = this.orderDetail.fullPriceReductionIsHidden ? this.orderDetail
					.fullPriceReduction : actualPrice;
				//MVP 演示：无微信支付商户号 → 走模拟支付（后端与真实回调同一套订单流转）
				if (payService.isMock()) {
					payService.mockPay({
						type: 1,
						out_trade_no: orderNo,
						total_fee: totalFee
					}).then(() => {
						toastService.hideLoading();
						toastService.showSuccess('支付成功', true);
						let timeout = setTimeout(() => {
							this.goPayResult(id, actualPrice, '微信支付');
							clearTimeout(timeout);
						}, 1000);
					}).catch((err) => {
						toastService.hideLoading();
						toastService.showError(err.message || '支付失败', true);
					});
					return;
				}
				https.request('/rest/member/wxPay/toPay4Applet', {
					openid: openId,
					type: 1,
					paymentMode: 1,
					out_trade_no: orderNo,
					total_fee: totalFee
				}).then((result) => {
					toastService.hideLoading();
					if (result.success) {
						//console.log(result)
						uni.requestPayment({
							appId: result.data.appid,
							timeStamp: result.data.timeStamp,
							nonceStr: result.data.nonceStr,
							package: result.data.package,
							signType: 'MD5',
							paySign: result.data.paySign,
							success: (res) => {
								toastService.showSuccess('支付成功', true);
								let timeout = setTimeout(() => {
									this.goPayResult(id, actualPrice, '微信支付');
									clearTimeout(timeout);
								}, 1000);
							},
							fail(res) {
								toastService.showError('支付失败', true);
								let timeout = setTimeout(() => {
									uni.redirectTo({
										url: '../../order/detail/detail?id=' + id
									});
									clearTimeout(timeout);
								}, 1000);
							}
						});
					}
				});
			},
			deletePwd(e) {
				if (this.pwdVal.length === 0) {
					return;
				}
				const lastIndex = this.pwdVal.length - 1;
				this.pwdVal = this.pwdVal.substring(0, lastIndex) + "";
			},
			inputPwd: function(e) {
				// #ifdef APP-PLUS||H5
				this.pwdVal = this.pwdVal + e;
				// #endif
				// #ifdef MP-WEIXIN||MP-ALIPAY
				this.pwdVal = e.detail.value;
				// #endif
				if (this.pwdVal.length >= 6) {
					toastService.showLoading('正在加载...');
					this.balancePay();
				}
			},

			/**
			 * 获取焦点
			 */
			getFocus: function() {
				var _this = this;
				this.payFocus = false;
				//this.showPayPwdInput = true;
				this.$nextTick(() => {
					console.log("点击获取焦点");
					// #ifdef APP-PLUS||H5
					_this.payFocus = true;
					// #endif
					// #ifdef MP-WEIXIN||MP-ALIPAY
					setTimeout(() => {
						_this.payFocus = true;
					}, 100);
					// #endif
				})
			},

			balancePay() {
				var password = Base64.encode(this.pwdVal);
				this.pwdVal = '';
				this.payFocus = true;
				https.request('/rest/member/platformPay/byBalance', {
					openid: this.balanceOpenId,
					type: 1,
					paymentMode: 2,
					paymentPassword: password,
					out_trade_no: this.balanceOrderNo,
					total_fee: this.orderDetail.fullPriceReductionIsHidden ? this.orderDetail
						.fullPriceReduction : this
						.balanceActualPrice
				}).then((result) => {
					toastService.hideLoading();
					if (result.success) {
						toastService.showSuccess('支付成功', true);
						this.hidePwdLayer();
						let timeout = setTimeout(() => {
							this.goPayResult(this.balanceId, this.orderDetail
								.fullPriceReductionIsHidden ? this.orderDetail.fullPriceReduction : this
								.balanceActualPrice, '余额支付');
							clearTimeout(timeout);
						}, 1000);
						return;
					}
					// 2026-09-25 恢复余额支付：失败分支（后端安全加固 S2/S3 的文案驱动引导）
					this.hidePwdLayer();
					var msg = result.message || '支付失败';
					if (msg.indexOf('余额不足') !== -1) {
						uni.showModal({
							title: '余额不足',
							content: msg + '，是否前往充值？',
							success: (res) => {
								if (res.confirm) {
									uni.navigateTo({
										url: '/pages/mine/member/recharge/recharge'
									});
								}
							}
						});
					} else if (msg.indexOf('请先设置支付密码') !== -1) {
						uni.showModal({
							title: '尚未设置支付密码',
							content: '使用余额支付需先设置 6 位支付密码，是否前往设置？',
							success: (res) => {
								if (res.confirm) {
									uni.navigateTo({
										url: '/pages/mine/security/index/index'
									});
								}
							}
						});
					} else {
						toastService.showError(msg, true);
					}
				});
			},

			bindtouchend(e) {
				console.log('触摸结束');
				this.showPwdLayer();
			},

			/**
			 * 跳转支付结果页（设计稿 C-09）
			 */
			goPayResult(orderId, amount, channel) {
				const d = getApp().globalData.deliveryAndSelfTaking || {};
				const dine = d.dineInfo;
				// 设计稿 A-05 仅「自提 / 点餐」两档：有桌位信息即堂食点餐，否则自提（外送已移除）
				const mode = dine ? '堂食' : '自提';
				let shopName = '';
				if (this.shopInfo && this.shopInfo.shop && this.shopInfo.shop.name) {
					shopName = this.shopInfo.shop.name;
				}
				let url = '/pages/menu/pay-result/pay-result?id=' + (orderId || '') +
					'&amount=' + (amount || 0) +
					'&channel=' + encodeURIComponent(channel || '微信支付') +
					'&mode=' + encodeURIComponent(mode);
				if (shopName) {
					url += '&shop=' + encodeURIComponent(shopName);
				}
				uni.redirectTo({
					url: url
				});
			},

			/**
			 * 结算页切到自提：清除堂食信息（设计稿 A-05 点餐方式切换 5:445）
			 */
			dineModeTapSelf() {
				if (!this.dineInfo && this.selfOutActiveIndex == 0) {
					return;
				}
				getApp().globalData.deliveryAndSelfTaking.dineInfo = null;
				this.dineInfo = null;
				this.selfOutActiveIndex = 0;
				toastService.showToast('已切换为自提');
			},

			/**
			 * 结算页切到点餐（堂食）：无桌位信息时扫桌台码补录
			 * 与 A-02 点餐台 dineModeTap('dine') 同一套解析口径（设计稿 A-05 点餐方式切换 5:445）
			 */
			dineModeTapDine() {
				if (this.dineInfo) {
					return;
				}
				// 原生扫码界面会把小程序压后台再回前台 → App.onShow 误触发活动弹窗标记；
				// 扫码期间置抑制位，complete 后 500ms 复位（09-24 修复）
				getApp().globalData.scanInProgress = true;
				uni.scanCode({
					success: (res) => {
						const text = res.result || '';
						// 桌台码内容支持：tid=05&area=A 或含这些参数的 URL
						const match = text.match(/(?:\?|&)?(tid|tableId|tableNo)=(\w+)/i);
						if (!match) {
							toastService.showToast('桌台码无效，请扫描桌台二维码');
							return;
						}
						// 进台位确认页（C-04）确认人数后返回本页继续结算（scene=查询串，URL 取 ? 后部分）
						const qIdx = text.indexOf('?');
						const scene = qIdx > -1 ? text.substring(qIdx + 1) : text;
						uni.navigateTo({
							url: '/pages/menu/scan-table/scan-table?scene=' + encodeURIComponent(scene) + '&from=pay'
						});
					},
					fail: () => {},
					complete: () => {
						// 500ms 宽限：兜住「App.onShow 晚于 complete 回调」的时序抖动
						setTimeout(() => {
							getApp().globalData.scanInProgress = false;
						}, 500);
					}
				});
			},

			balancePayFail() {
				this.hidePwdLayer();
				toastService.showError('支付失败', true);
				let timeout = setTimeout(() => {
					uni.redirectTo({
						url: '../../order/detail/detail?id=' + this.balanceId
					});
					clearTimeout(timeout);
				}, 1000);
			},

			hidePwdLayer() {
				this.showPayPwdInput = false;
				this.payFocus = false;
				this.pwdVal = '';

			},

			showPwdLayer() {
				this.showPayPwdInput = true;
				this.payFocus = true;
				this.pwdVal = '';


				// this.setData({
				// 	showPayPwdInput: true,
				// 	payFocus: true,
				// 	pwdVal: ''
				// });
			},

			/**
			 * 隐藏支付密码输入层
			 */
			forgetThePassword: function() {
				uni.navigateTo({
					url: '../../mine/security/index/index'
				});
			},
			//获取满减规则
			getFullReductionRule(orderDetail) {
				var _this = this;
				toastService.showLoading();
				https.request('/rest/fullReductionRule/list', {
					pageNo: -1,
					pageSize: 1,
					shopId: orderDetail.shopId
				}).then((result) => {
					toastService.hideLoading();
					if (result.success) {
						console.log('getFullReductionRule', app.globalData.deliveryAndSelfTaking.orderDetail);
						var orderDetailList = orderDetail.orderDetailList;
						var fullPriceReduction = orderDetail.fullPriceReduction;
						//获取配送费，配送费不作为满减条件
						// var packingCharges = 0;
						// var totalNum = 0;
						// var totalPrice = 0;
						// orderDetailList.forEach((result, index) => {
						// 	totalNum = totalNum + result.number;
						// 	result.goodsPrices = utilHelper.toFixed(Number(result.goodsPrice) * result
						// 		.number, 2);
						// 	totalPrice = totalPrice + (result.price * result.number); //初始化被选中的商品的总金额
						// 	result.disable = result.goodsStatus == 1 || result.goodsStatus == 3 || result
						// 		.goodsStatus == 4 ? true : false;
						// 	packingCharges =
						// 		result.goodsStatus == 1 || result.goodsStatus == 3 || result.goodsStatus ==
						// 		4 ?
						// 		packingCharges :
						// 		packingCharges + result.packingCharges * result.number;
						// });
						// totalPrice = utilHelper.toFixed(totalPrice + packingCharges, 2);
						var fullReductionRulePrice = 0;
						// [满减规则停用 2026-09-28] 满减规则已并入满减券（商家端「优惠券管理」直接建满减券，顾客领券使用）；原逻辑注释保留，会员权益设计可能复用
						// for (let i = 0; i < result.data.records.length; i++) {
							// //总价格减去配送费大于满减金额则进行满减操作
							// if (fullPriceReduction >= result.data.records[i].limitedPrice) {
								// orderDetail.fullPriceReduction = utilHelper.toFixed(fullPriceReduction - result
									// .data.records[i]
									// .reducedPrice, 2);
								// orderDetail.fullReductionRuleName = result.data.records[i].name;
								// orderDetail.fullReductionRuleId = result.data.records[i].id;
								// orderDetail.fullPriceReductionIsHidden = true;
								// orderDetail.reducedPrice = result.data.records[i].reducedPrice;
							// }
						// }
						//console.log('计算满减金额', totalPrice);
						this.$nextTick(() => {
							_this.getPsf(orderDetail);
						});

					}
				});
			},
			getPsf(orderDetail) {
				app.globalData.deliveryAndSelfTaking.feeData = 0;
				app.globalData.deliveryAndSelfTaking.reducedDeliveryTotalPrice = 0;
				app.globalData.deliveryAndSelfTaking.isReducedDeliveryPrice = false;
				var orderDetailList = orderDetail.orderDetailList;
				var packingCharges = 0;
				var totalNum = 0;
				var totalPrice = 0;
				var fullPriceReduction = orderDetail.fullPriceReduction;

				orderDetailList.forEach((result, index) => {
					totalNum = totalNum + result.number;
					result.goodsPrices = utilHelper.toFixed(Number(result.goodsPrice) * result
						.number, 2);
					totalPrice = totalPrice + (result.price * result.number); //初始化被选中的商品的总金额
					result.disable = result.goodsStatus == 1 || result.goodsStatus == 3 || result
						.goodsStatus == 4 ? true : false;
					packingCharges =
						result.goodsStatus == 1 || result.goodsStatus == 3 || result.goodsStatus ==
						4 ?
						packingCharges :
						packingCharges + result.packingCharges * result.number;
				});
				totalPrice = utilHelper.toFixed(totalPrice, 2);
				console.log('计算满减金额', totalPrice);
				var isStartDeliveryPrice = false;
				var priceDifference = 0;
				var startDeliveryPrice = app.globalData.deliveryAndSelfTaking.orderDetail.initShopInfo.startDeliveryPrice;
				if (totalPrice + packingCharges >= startDeliveryPrice) {
					isStartDeliveryPrice = true;
				}
				var actualPrice = utilHelper.toFixed(totalPrice + packingCharges, 2);
				if (app.globalData.deliveryAndSelfTaking.selfOutActiveIndex == 0) {
					app.globalData.deliveryAndSelfTaking.orderDetail.fullPriceReduction = fullPriceReduction;
					app.globalData.deliveryAndSelfTaking.orderDetail.actualPrice = actualPrice;
					this.deliveryAndSelfTaking = app.globalData.deliveryAndSelfTaking;
					this.orderDetail = app.globalData.deliveryAndSelfTaking.orderDetail;
					console.log('this.orderDetail', this.orderDetail);
				}
				if (app.globalData.deliveryAndSelfTaking.selfOutActiveIndex == 1) {
					var addressid = app.globalData.deliveryAndSelfTaking.deliveryAddress.id;
					var shopId = app.globalData.deliveryAndSelfTaking.orderDetail.shopId;
					this.confirmSelectDeliveryFee(addressid, shopId).then(result => {
						console.log("查询用户地址配送费=", result);
						let isReducedDeliveryPrice = false;
						let feeData = result;
						let reducedDeliveryTotalPrice = result;
						console.log('用户地址配送费' + reducedDeliveryTotalPrice);
						if (this.reducedDeliveryPrice >= reducedDeliveryTotalPrice) {
							feeData = 0;
						} else {
							feeData = feeData - this.reducedDeliveryPrice;
						}
						console.log('用户最终支付的配送费' + feeData);
						if (reducedDeliveryTotalPrice != feeData) {
							isReducedDeliveryPrice = true;
						}
						app.globalData.deliveryAndSelfTaking.feeData = feeData;
						app.globalData.deliveryAndSelfTaking.reducedDeliveryTotalPrice = result;
						app.globalData.deliveryAndSelfTaking.isReducedDeliveryPrice = isReducedDeliveryPrice;

						var isStartDeliveryPrice = false;
						var priceDifference = 0;
						if (totalPrice + packingCharges >= startDeliveryPrice) {
							isStartDeliveryPrice = true;
						}
						var actualPrice = utilHelper.toFixed(totalPrice + packingCharges, 2) + feeData;
						fullPriceReduction = utilHelper.toFixed(fullPriceReduction, 2) + feeData;
						app.globalData.deliveryAndSelfTaking.orderDetail.actualPrice = actualPrice;
						app.globalData.deliveryAndSelfTaking.orderDetail.fullPriceReduction = utilHelper.toFixed(
							fullPriceReduction, 2);
						this.deliveryAndSelfTaking = app.globalData.deliveryAndSelfTaking;
						this.orderDetail = app.globalData.deliveryAndSelfTaking.orderDetail;

					})
				}
				setTimeout((time) => {
					this.choosePayModeTap();
					clearTimeout(time);
				}, 1000);
			},

			customerServiceWechat() {
				console.log('占位：函数 customerServiceWechat 未声明');
			},

			goToPay() {
				console.log('占位：函数 goToPay 未声明');
			},

			trueFun() {
				console.log('占位：函数 true 未声明');
			},
			selfTap(e) {
				console.log(e);
				var index = e.currentTarget.dataset.index;
				var selfOutActiveIndex = this.selfOutActiveIndex;
				console.log("选择地址======", this.deliveryAndSelfTaking);
				var chooseId = this.deliveryAndSelfTaking.deliveryAddress ? this.deliveryAndSelfTaking
					.deliveryAddress.id :
					'';
				var chooseId = selfOutActiveIndex == 1 ? chooseId : '';
				var shopId = selfOutActiveIndex == 0 ? this.shopInfo.shop.id : app.globalData.deliveryAndSelfTaking
					.orderDetail.shopId;
				app.globalData.deliveryAndSelfTaking.chooseId = chooseId;
				app.globalData.deliveryAndSelfTaking.selfOutActiveIndex = selfOutActiveIndex;
				app.globalData.deliveryAndSelfTaking.shopId = shopId;
				app.globalData.deliveryAndSelfTaking.pageType = 'pay';
				app.globalData.deliveryAndSelfTaking.chooseIndex = index;
				app.globalData.deliveryAndSelfTaking.ifChooseBack = false;
				app.globalData.deliveryAndSelfTaking.ifChoosePayBack = false;
				uni.navigateTo({
					url: `/pages/address/choose/choose`
				})
			},
			getShoppingCartList(shopId) {

			},
		}
	};
</script>
<style>
	page {
		background: #f5f5f5;
		padding-bottom: 120rpx;
	}

	.top-tips-view {
		background: white;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 32rpx;
		font-weight: bold;
		line-height: 100rpx;
	}

	.probably-time-view {
		margin-left: 20rpx;
	}

	/* 单选框样式--自取配送 */

	.radio-group-view {
		display: flex;
		align-items: center;
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

	.radio-group-label {
		width: 46%;
		padding: 2%;
		font-size: 28rpx;
		border-radius: 50rpx;
		display: flex;
		justify-content: center;
		align-items: center;
		/* height: 60rpx; */
	}

	.not-active {
		color: white;
	}

	.ask-for-delivery-detail {
		margin: 20rpx;
		padding: 20rpx;
		background: white;
		border-radius: 15rpx;
	}

	.ask-for-delivery-title {
		font-size: 24rpx;
		font-weight: bold;
		line-height: 60rpx;
	}

	.ask-for-delivery {
		display: flex;
	}

	.ask-for-delivery-address {
		width: 75%;
	}

	.ask-for-delivery-house {
		width: 100%;
		font-size: 28rpx;
		display: flex;
		align-items: center;
	}

	.dizhi-phone-icon {
		width: 5%;
		height: auto;
	}

	.right-class {
		width: 90%;
	}

	.ask-for-delivery-address-detail {
		font-size: 28rpx;
		height: 40rpx;
		line-height: 40rpx;
		color: #7f7f7f;
		display: flex;
		align-items: center;
	}

	.ask-for-delivery-view {
		width: 90%;
	}

	.shopping-commodity-details {
		margin: 20rpx;
		padding: 20rpx;
		background: white;
		border-radius: 15rpx;
	}

	.commodity-name-price-detail {
		padding: 10rpx 0;
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.commodity-name-types {
		width: 70%;
	}

	.commodity-name {
		font-size: 28rpx;
		font-weight: bold;
	}

	.commodity-types {
		font-size: 24rpx;
	}

	.commodity-totalnum {
		font-size: 30rpx;
	}

	.commodity-price {
		font-size: 28rpx;
		font-weight: bold;
	}

	.commodity-totalnum-price {
		width: 30%;
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.total-money-view {
		width: 100%;
		line-height: 40rpx;
		margin-top: 20rpx;
		display: flex;
		align-items: center;
		justify-content: flex-end;
	}

	.total-title {
		font-size: 28rpx;
	}

	.total-money {
		font-size: 32rpx;
		font-weight: bold;
		margin-left: 20rpx;
		width: 40%;
	}

	.icon-wechat-pay {
		width: 50rpx;
		height: auto;
		margin-right: 20rpx;
	}

	.pay-mode-view {
		display: flex;
		align-items: center;
		justify-content: space-between;
		background: white;
		margin: 20rpx;
		padding: 20rpx;
		font-size: 30rpx;
		border-radius: 15rpx;
	}

	.choose-pay-mode {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.coupon-mode {
		width: 80%;
	}

	.ask-for-remarks-view {
		background: white;
		margin: 20rpx;
		padding: 20rpx;
		border-radius: 15rpx;
	}

	.choose-ask-for {
		display: flex;
		align-items: center;
		justify-content: space-between;
		line-height: 100rpx;
		font-size: 30rpx;
	}

	.remarks-title-input-num {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.remarks-title {
		font-size: 30rpx;
		line-height: 100rpx;
	}

	.remarks-input-num {
		font-size: 28rpx;
	}

	/* .remarks-view {
  padding-bottom: 120rpx;
} */

	.textarea-remarks {
		width: 94%;
		padding: 20rpx;
		background: #f5f5f5;
		border-radius: 10rpx;
		height: 120rpx;
		font-size: 28rpx;
		z-index: 0;
	}

	/* C-08 常用备注快捷标签（设计稿 A-05 备注卡·快捷标签行 163:280：高 26／圆角 13／左右内距 12／间距 8）
	   ⚠️ 几何与配色分两个类：配色只由「未选中」类承担，选中态交给主题类（.theme-bg-light/.theme-color），
	   避免页内样式（后加载）覆盖 app.wxss 的主题色导致选中态仍是灰字 */
	.quick-remark-view {
		display: flex;
		flex-direction: row;
		flex-wrap: wrap;
		margin-bottom: 16rpx;
	}

	.quick-remark-tag-shape {
		height: 52rpx;
		line-height: 52rpx;
		padding: 0 24rpx;
		margin-right: 16rpx;
		border-radius: 26rpx;
		font-size: 24rpx;
	}

	.quick-remark-tag-idle {
		background: #f5f5f5;
		color: #666666;
	}

	/* C-08 单品备注（09-27 自底部备注卡迁至商品行内联）：商品行改纵向块，
	   上行＝「名称规格 ↔ 数量价格」原布局，下行＝单品备注输入框 */
	.commodity-item-block {
		display: block;
	}

	.commodity-row-main {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.item-remark-input {
		margin-top: 8rpx;
		height: 60rpx;
		padding: 0 16rpx;
		background: #f5f5f5;
		border-radius: 8rpx;
		font-size: 24rpx;
	}

	.iconwechat_pay {
		color: #09bb07;
		font-size: 40rpx;
	}

	.iconyue {
		color: #f0dcab;
		font-size: 40rpx;
	}

	.go-pay-view {
		position: fixed;
		bottom: 0;
		width: 100%;
		line-height: 100rpx;
		display: flex;
		align-items: center;
		background: white;
		box-shadow: -2px 0px 5px 0.5px rgba(0, 0, 0, 0.1);
		font-size: 36rpx;
		z-index: 1;
		border-top: 0.5rpx solid #f5f5f5;
	}

	.go-pay-money {
		padding: 0 20rpx;
		width: 70%;
		background: white;
		display: flex;
	}

	.more-pay {
		font-size: 32rpx;
	}

	.go-pay {
		width: 30%;
		color: white;
		text-align: center;
		font-size: 32rpx;
	}

	cover-view {
		line-height: 100rpx;
	}

	/*radio 选项框大小  */

	radio .wx-radio-input {
		width: 35rpx;
		height: 35rpx;
		border-radius: 50%;
		border-color: #ededed;
	}

	.ask-for-label {
		display: flex;
		align-items: center;
	}

	.ask-for-radio-group {
		width: 60%;
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.ra-group-label {
		display: flex;
		align-items: center;
		padding: 10rpx;
	}

	.full-reduction-view {
		font-size: 24rpx;
		font-weight: bold;
		padding: 0 10rpx;
		margin: 0 5rpx;
		border-radius: 10rpx;
	}

	.fullPriceReductionClass {
		color: gainsboro;
		text-decoration: line-through;
	}

	.pay-mode-title {
		width: 20%;
		font-size: 28rpx;
	}

	.after-discount {
		font-size: 28rpx;
		font-weight: bold;
		text-align: end;
		width: 100%;
		font-size: 28rpx;
	}

	.swiper-tab {
		width: 100%;
		text-align: center;
		height: 88rpx;
		line-height: 88rpx;
		display: flex;
		flex-flow: row;
		justify-content: space-between;
		background: #fff;
		z-index: 1;
		border-bottom: 1rpx solid #ededed;
	}

	.swiper-tab-item {
		width: 50%;
	}

	.swiper-box {
		display: block;
		width: 100%;
		height: 100%;
		overflow: hidden;
	}

	.swiper-items {
		height: 100%;
	}

	.scroll-views {
		height: 100%;
		background: #fff;
	}

	.bindSlideChange {
		padding-top: 20rpx;
	}

	.space-between-class {
		width: 100%;
	}

	.bindSlideChange-class {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.currentTab1-view {
		margin-bottom: 20rpx;
	}

	.currentTab1-title {
		font-size: 26rpx;
		color: #7f7f7f;
	}

	.time-phone-view {
		border-top: 1rpx solid #f5f5f5;
		padding: 20rpx 0;
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.time-phone-item {
		font-size: 24rpx;
	}

	.time-phone-content {
		width: 100%;
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.time-phone-title {
		color: #7f7f7f;
	}

	.deliveryAddress-info {
		margin-bottom: 20rpx;
	}

	.dayue-time {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-top: 20rpx;
		font-size: 26rpx;
	}

	.dayue-time-songda-view {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.ask-for-delivery-house-view {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-bottom: 20rpx;
	}

	.payment-checked {
		background: linear-gradient(to right, #f2e1b5, #ebd198);
	}

	.payment-not-checked {
		background: #f0f0f0;
	}

	.radio-label-payment {
		padding: 8rpx 10rpx;
		font-size: 26rpx;
		border-radius: 18rpx;
		margin: 5rpx 0;
	}

	.pay_radio {
		display: none;
	}

	.radio-label-payment.flex_center.payment-checked .iconyue {
		color: black;
	}

	.actionItem__desc {
		font-size: 22rpx;
		color: red;
	}

	.current-tips {
		padding: 20rpx;
		font-size: 38rpx;
		border-radius: 10rpx;
		font-weight: bold;
		color: red;
	}

	.top-detail {
		margin: 20rpx;
		background-color: white;
		border-radius: 10rpx;
	}

	.close-view-class .iconfont.icon55 {
		font-size: 62rpx;
		color: gainsboro;
	}

	.self_out_button {
		background: #ededed;
		border-radius: 50rpx;
		height: 2rem;
		line-height: 2rem;
		width: 30%;
		padding: 5rpx;
	}

	.self_button {
		font-size: 24rpx;
		width: 50%;
		text-align: center;
		padding: 0 10rpx;
	}

	.self_out_active {
		color: white;
		padding: 0 10rpx;
		border-radius: 50rpx;
	}

	.shop_info_view {
		font-size: 32rpx;
		width: 70%;
	}

	.distance_m {
		font-size: 28rpx;
		color: var(--tab-text-color, #646566);
		margin-top: 10rpx;
	}

	/* 堂食桌号标签（设计稿 A-05） */
	.shop-name-row {
		gap: 8rpx;
		align-items: center;
	}

	.dine-table-tag {
		flex-shrink: 0;
		background: #edf3df;
		color: #5f7d22;
		font-size: 22rpx;
		line-height: 1;
		padding: 6rpx 16rpx;
		border-radius: 18rpx;
	}
</style>