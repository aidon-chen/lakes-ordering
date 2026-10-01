<template>
	<view>
		<!-- C-10 批次6（口径变更）：不做配送 → 原「收货地址卡」改为「自提门店卡」（画板 B-05） -->
		<view class="pickup-shop-card margin-border-radius">
			<view class="pickup-shop-icon theme-color">
				<van-icon name="location-o" />
			</view>
			<view class="pickup-shop-main">
				<view class="pickup-shop-name out_of_range one_row">{{ pickupShop.name || '门店信息加载中…' }}</view>
				<view class="pickup-shop-addr out_of_range one_row" v-if="pickupShop.address">{{ pickupShop.address }}</view>
				<view class="pickup-shop-tip">到店自提 · 请到店出示自提码</view>
			</view>
		</view>
		<view class="ask-for-delivery-detail margin-border-radius" v-if="false">
			<view class="ask-for-delivery-title">收货地址</view>
			<view class="ask-for-delivery" @tap="openAddressDialog">
				<view class="ask-for-delivery-house-view" v-if="deliveryAndSelfTaking.deliveryAddress"
					data-radioIndex="0">
					<view class="ask-for-delivery-view">
						<view class="ask-for-delivery-house">
							<van-icon name="location-o" />
							<view class="right-class out_of_range two_row">
								{{ deliveryAndSelfTaking.deliveryAddress.province }}{{ deliveryAndSelfTaking.deliveryAddress.city }}
								{{ deliveryAndSelfTaking.deliveryAddress.area
                                }}{{ deliveryAndSelfTaking.deliveryAddress.street }}
							</view>
						</view>
						<view class="ask-for-delivery-address-detail">
							<van-icon name="phone-o" />
							<view class="right-class">
								<text>{{ deliveryAndSelfTaking.deliveryAddress.phone }}</text>
								<text :decode="true">
									&nbsp;&nbsp;&nbsp;{{ deliveryAndSelfTaking.deliveryAddress.realname }}{{ deliveryAndSelfTaking.deliveryAddress.sex == 0 ? '先生' : '女士' }}
								</text>
							</view>
						</view>
					</view>
					<van-icon name="arrow" />
				</view>
				<view class="please-address" v-else data-radioIndex="0">
					<text>请选择地址</text>
					<van-icon name="arrow" />
				</view>
			</view>
		</view>
		<view class="shopping-commodity-details margin-border-radius">
			<view class="commodity-name-price-detail" v-for="(item, index) in data.orderDetailList" :key="index">
				<view class="commodity-name-types">
					<view class="commodity-name">{{ item.goodsName }}</view>
					<view class="commodity-types">{{ item.restructure }}</view>
				</view>

				<view class="commodity-totalnum-price">
					<view class="commodity-totalnum">x{{ item.number }}</view>
					<view class="commodity-price">￥{{ item.price }}</view>
				</view>
			</view>
			<view class="view-line"></view>
			<view class="total-money-view">
				<text class="money-icon commodity-price">总计：</text>
				<view :class="data.fullPriceReductionIsHidden || data.couponsIsHidden ? 'fullPriceReductionClass' : ''">
					<text
						:class="'commodity-price ' + (data.fullPriceReductionIsHidden || data.couponsIsHidden ? 'strike_through' : '')">￥{{ data.actualPrice }}</text>
				</view>
				<text class="commodity-price"
					v-if="data.fullPriceReductionIsHidden || data.couponsIsHidden">￥{{ data.fullPriceReduction }}</text>
			</view>
		</view>
		<view class="pay-mode-view margin-border-radius">
			<view class="pay-mode-title">优惠券</view>
			<!-- C-10 批次6：无可用券时也要能进选券页（原条件会让点击无反应） -->
			<view class="choose-pay-mode" @tap="onCoupon">
				<view class="theme-color after-discount flex_end" v-if="afterDiscount.couponsName">
					<view class="flex_end">
						<text class="coupons-name-text" :decode="true">
							{{ afterDiscount.couponsName ? afterDiscount.couponsName : availableCouponSize > 0 ? '未使用优惠券&nbsp;&nbsp;可用' + availableCouponSize + '张' : '' }}
						</text>
						<text
							:decode="true">{{ afterDiscount.couponsName ? '&nbsp;&nbsp;优惠' + afterDiscount.price + '元' : '' }}</text>
					</view>
				</view>
				<view class="theme-color after-discount" v-else>
					<text class="out_of_range one_row">无可用优惠券</text>
				</view>
				<van-icon name="arrow" />
			</view>
		</view>
		<view class="pay-mode-view margin-border-radius">
			<view>支付方式</view>
			<view class="choose-pay-mode">
				<radio-group class="radio-group-address" @change="radioChangePayment">
					<block v-for="(item, index) in paymentModes" :key="index">
						<label v-if="item.show"
							:class="'radio-label-payment flex_center ' + (item.checked ? 'payment-checked' : 'payment-not-checked')">
							<radio :value="index" :checked="item.checked" class="pay_radio" />
							<van-icon :name="item.icon" v-if="item.icon" />
							<text :decode="true">&nbsp;{{ item.text }}</text>
							<text :decode="true" class="actionItem__desc" v-if="item.desc">&nbsp;{{ item.desc }}</text>
						</label>
					</block>
				</radio-group>
			</view>
		</view>
		<view class="ask-for-remarks-view margin-border-radius">
			<view class="view-line" v-if="radioIndex == 0"></view>
			<view class="remarks-view">
				<view class="remarks-title-input-num">
					<view class="remarks-title">特殊备注要求</view>
					<view class="remarks-input-num">{{ inputLength }}/30</view>
				</view>
				<textarea class="textarea-remarks" @input="remarksInput" maxlength="30" placeholder="输入其他特殊备注要求（可不填）"
					placeholder-class="textarea-placeholder" :auto-focus="false" :focus="false" :hold-keyboard="false"
					:adjust-position="false"></textarea>
			</view>
		</view>
		<view class="safe-area go-pay-view">
			<view class="go-pay-money">
				<view class="more-pay">
					<view>还需支付</view>
				</view>
				<view class="total-money" v-if="!data.fullPriceReductionIsHidden && !data.couponsIsHidden">
					￥{{ data.actualPrice }}</view>
				<view class="total-money" v-else>￥{{ data.fullPriceReduction }}</view>
			</view>
			<view class="go-pay theme-bg" hover-class="hover-class-public" @tap="weChatPayTap">去支付</view>
		</view>
		<van-action-sheet @actiontap="goToPay" :show="dialogShow" :actions="paymentModes" :title="title"
			z-index='1'></van-action-sheet>
		<van-action-sheet :show="addressDialog" title="选择地址" @close="closeDialog" @cancel="closeDialog" z-index='1'>
			<view class="content_box">
				<scroll-view scroll-y style="height: 56vh">
					<radio-group class="radio-group-address" @change="radioChangeAddress">
						<label class="radio-label-address" v-for="(item, index) in addressList" :key="index">
							<block v-if="deliveryAndSelfTaking.deliveryAddress">
								<radio :value="index" :checked="item.id == deliveryAndSelfTaking.deliveryAddress.id"
									class="radio" />
							</block>
							<block v-else>
								<radio :value="index" class="radio" />
							</block>

							<view class="flex_between choose-address-dialog">
								<view class="">
									<view class="address-detail">
										{{ item.province }}{{ item.city }}{{ item.area }}{{ item.street }}
									</view>
									<view>
										<text class="address-name-phone">{{ item.realname }}</text>
										<text class="address-name-phone">{{ item.phone }}</text>
									</view>
								</view>
								<view>
									<van-icon name="edit" @tap="editAddress" :data-data="item" :data-key="index" />
								</view>
							</view>
						</label>
					</radio-group>
				</scroll-view>
				<view slot="footer" class="footer-btns">
					<view class="good-choice-btn theme-color-border" @tap="insertAddress">新增</view>
					<view class="good-choice-btn theme-bg" @tap="confirmChooseAddress">确认选择</view>
				</view>
			</view>
		</van-action-sheet>
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
				<view class="password_dialog_row" @tap.stop.prevent="getFocus">
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
		<!-- C-10 批次6（口径变更）：会员引导弹窗（加入会员/获好友返利）已删除 -->
		// #ifdef APP-PLUS||H5
		<van-number-keyboard :show="payFocus" @blur="payFocus = false" @input="inputPwd" @delete="deletePwd" />
		// #endif
	</view>
</template>

<script>
	import https from '../../../../utils/http';
	import authService from '../../../../utils/auth';
	import payService from '../../../../utils/pay';
	import {
		Base64
	} from 'js-base64';
	import toastService from '../../../../utils/toast.service';
	import systemStatus from '../../../../utils/system-status';
	import dateHelper from '../../../../utils/date-helper';
	import utilHelper from '../../../../utils/util';
	let app = null;
	export default {
		data() {
			return {
				orderToken: '',
				time: '10:00',
				isChoose: false,
				//是否选择派送方式
				isFirstShop: false,
				//是否选择的是门店
				isFirstAddress: false,
				inputLength: 0,
				deliveryData: {},
				title: '请选择支付方式',
				// C-10 批次6（口径变更）：支付方式保留「到店支付」（默认），并打开已预留的「微信支付」项
				paymentModes: [
				{
					// C-10 批次6（口径变更）：积分支付全面取消 → 保留「到店支付」（4=到店支付，后端约定）
					checked: true,
					value: 4,
					text: '到店支付',
					show: true,
					icon: ''
				},
				{
					// 微信支付（1）：批次6 §0.1-①「同批次内接上微信支付」的接入位，本次打开。
					// MVP 演示环境无微信支付商户号 → 由 utils/pay.js 依 cloud.mockPay 改走模拟支付
					// （后端 mockPay type=4 → pointsMallOrderService.paymentNotify）
					value: 1,
					text: '微信支付',
					show: true,
					icon: 'wechat-pay'
				},
				{
					// 余额支付（2）：2026-09-25 恢复（主计划决策点 3 拍板）→
					// /rest/member/pointsMall/platformPay/byBalance（type=6，含 S1/S2/S3 安全加固）
					value: 2,
					text: '余额支付',
					show: true,
					icon: 'gold-coin-o'
				}
			],
			paymentModeIndex: 0,
				// C-10 批次6：自提门店（结算页展示；来源 /rest/shop/listForPickup，失败则留空）
				pickupShop: {
					id: '',
					name: '',
					address: ''
				},
				dialogShow: false,
				addressDialog: false,
				remarks: '',
				isVipDialogShow: false,
				deliveryAndSelfTaking: {
					deliveryAddress: {
						province: '',
						city: '',
						area: '',
						street: '',
						phone: '',
						realname: '',
						sex: 0,
						id: ''
					}
				},
				payType: '',
				initData: '',
				timestamp: '',
				addressList: '',
				addrIndex: '',
				refreshKey: '',
				userInfo: '',
				afterDiscount: {
					couponsName: false,
					price: ''
				},
				availableCouponSize: '',
				data: {
					fullPriceReduction: '',
					couponsIsHidden: false,
					orderDetailList: [],
					fullPriceReductionIsHidden: '',
					actualPrice: ''
				},
				tipReward: '',
				payFocus: false,
				pwdVal: '',
				showPayPwdInput: false,
				isPayJson: '',
				balanceId: '',
				balanceOrderNo: '',
				balanceActualPrice: '',
				balanceOpenId: '',
				radioIndex: 0,
				feeData: '',
				maskClosable: '',
				i: '',
				holdKeyboard: '',
				adjustPosition: '',
				buttons: '',
				extClass: '',
				shopInfo: {
					shop: {
						name: ''
					}
				}
			};
		},
		/**
		 * 生命周期函数--监听页面加载
		 */
		onLoad: function(options) {
			app = getApp();
			console.log(app.globalData.deliveryAndSelfTaking);
			this.deliveryAndSelfTaking = app.globalData.deliveryAndSelfTaking;
			var prePageData = JSON.parse(options.orderDetail);
			// ⚠️ 上一页传来的 actualPrice 是字符串（如 "108.00"），"+0" 会变成字符串拼接出三位小数 "108.000"；
			// 统一转数字并固定保留两位小数（确认订单页「总计」划线价展示口径）
			prePageData.actualPrice = (Number(prePageData.actualPrice) || 0).toFixed(2);
			let time = dateHelper.formatTime('hms');
			//this.getFullReductionRule(data);
			this.getCouponsMemberRelation(prePageData);
			this.payType = options.payType;
			this.time = time;
			this.initData = prePageData;

			// this.getShopAddressList();
			// setTimeout(out=>{
			//   this.getCouponsMemberRelation();
			// },1000)
		},
		/**
		 * 生命周期函数--监听页面初次渲染完成
		 */
		onReady: function() {},
		/**
		 * 生命周期函数--监听页面显示
		 */
		onShow: function() {
			app = getApp();
			this.deliveryAndSelfTaking = app.globalData.deliveryAndSelfTaking;
			this.getPickupShop();
			this.getLoginMemberInfo();
			this.createOrderToken();
			setTimeout((time) => {
				this.selectCommissionReward();
				clearTimeout(time);
			}, 1500);
			this.getTimestamp();
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
			getTimestamp() {
				var timestamp = dateHelper.getTimestamp();
				console.log(timestamp);
				this.timestamp = timestamp;
			},

			/**
			 * C-10 批次6：加载自提门店（画板 B-05 门店卡）。
			 * 选店优先级（选门店恢复⑦）：用户手选 currentShopId > 定位最近一家。
			 * 手选走 detailForPickup（返回 data.shop）；定位失败用门店所在地坐标兜底；接口失败不阻塞下单。
			 */
			getPickupShop() {
				const that = this;
				// 手选门店优先
				const savedShopId = uni.getStorageSync('currentShopId');
				if (savedShopId) {
					https.request('/rest/shop/detailForPickup', {
						id: savedShopId
					}).then((result) => {
						if (result.success && result.data && result.data.shop) {
							const shop = result.data.shop;
							that.pickupShop = {
								id: shop.id,
								name: shop.name || '',
								address: [shop.street, shop.houseNumber].filter(Boolean).join('') ||
									((shop.province || '') + (shop.city || '') + (shop.area || ''))
							};
						}
					}).catch(() => {});
					return;
				}
				const loadByPosition = (lng, lat) => {
					https.request('/rest/shop/listForPickup', {
						position: lng + ',' + lat,
						pageNo: 1,
						pageSize: 1
					}).then((result) => {
						if (result.success) {
							const records = (result.data && result.data.records) || [];
							if (records.length > 0) {
								const shop = records[0];
								that.pickupShop = {
									id: shop.id,
									name: shop.name || '',
									address: shop.street || ((shop.province || '') + (shop.city || '') + (shop.area || ''))
								};
							}
						}
					}).catch(() => {});
				};
				uni.getLocation({
					type: 'gcj02',
					success: (res) => {
						loadByPosition(res.longitude, res.latitude);
					},
					fail: () => {
						// 兜底：门店所在坐标（演示环境）
						loadByPosition(112.883120, 28.236586);
					}
				});
			},

			openAddressDialog(e) {
				this.getDeliveryAddressList();
				this.addressDialog = true;
			},

			getDeliveryAddressList() {
				https.request('/rest/member/deliveryAddress/list', {
					pageNo: -1,
					pageSize: 10
				}).then((result) => {
					toastService.hideLoading();
					if (result.success) {
						this.addressList = result.data.records;
					}
				});
			},

			radioChangeAddress(e) {
				console.log(e);
				this.addrIndex = e.detail.value;
			},

			choosePayModeTap() {
				// C-10 批次6（口径变更）：积分/余额支付已取消，支付方式只剩「到店支付」一项。
				// 原实现会给 paymentModes[1]（平台积分）写 subname/isBindTap —— 该项已删除，
				// 访问 [1] 会抛 "Cannot set properties of undefined (setting 'subname')" 并中断后续逻辑。
				toastService.hideLoading();
			},

			close() {
				var _this = this;
				this.isVipDialogShow = false;
				https.request('/rest/member/pointsMall/order/insert', _this.isPayJson).then((result) => {
					if (result.success) {
						this.$nextTick(() => {
							_this.toPay4Applet(result.data.id, result.data.orderNo, result.data
								.actualPrice);
						});
					}
				});
			},

			goToRecharge(e) {
				this.isVipDialogShow = false;
				uni.navigateTo({
					url: '/pages/mine/member/recharge/recharge'
				});
			},

			closeDialog: function() {
				this.dialogShow = false;
				this.addressDialog = false;
			},

			radioChangePayment(e) {
				// e.detail.value 是 radio-group 的**下标**；paymentModes[i].value 才是支付方式码
				// （4=到店支付 / 1=微信支付，见 data.paymentModes 注释）。
				// 原实现按下标 0/1 硬编码「0=微信支付被拦、1=余额/积分需密码」——积分与余额支付取消后
				// 该映射已失效（下标 1 现在是微信支付，会被误判为需支付密码），故改为按下标通用处理。
				var _this = this;
				authService.getOpenId().then((openId) => {
					_this.closeDialog();
					let paymentModes = _this.paymentModes;
					const index = Number(e.detail.value);
					if (!paymentModes[index]) {
						return;
					}
					for (let key in paymentModes) {
						paymentModes[key].checked = false;
					}
					paymentModes[index].checked = true;
					_this.paymentModes = paymentModes;
					_this.paymentModeIndex = index;
				});
			},

			confirmChooseAddress(e) {
				console.log(e);
				console.log(this.addressList[this.addrIndex]);
				if (!this.addressList[this.addrIndex]) {
					// C-10 批次6（口径变更）：不做配送，地址选择弹层已禁用
					return;
				}
				app.globalData.deliveryAndSelfTaking.deliveryAddress = this.addressList[this.addrIndex];
				this.addressDialog = false;
				this.deliveryAndSelfTaking.deliveryAddress = this.addressList[this.addrIndex];
			},

			insertAddress() {
				uni.navigateTo({
					url: '/pages/address/insert/insert'
				});
			},

			editAddress(e) {
				var index = e.currentTarget.dataset.key;
				var address = this.addressList[index];
				this.refreshKey = index;
				uni.navigateTo({
					url: '/pages/address/edit/edit?detail=' + JSON.stringify(address)
				});
			},

			remarksInput(e) {
				this.inputLength = e.detail.value.length;
				this.remarks = e.detail.value;
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
						setTimeout((time) => {
							this.choosePayModeTap();
							clearTimeout(time);
						}, 1000);
					}
				});
			},

			createOrderToken: function(e) {
				toastService.showLoading();
				https.request('/rest/member/pointsMall/order/createOrderToken', {}).then((result) => {
					if (result.success) {
						this.orderToken = result.data;
					}
				});
			},

			//this.getFullReductionRule(data);
			getCouponsMemberRelation(prePageData) {
				console.log(prePageData);
				toastService.showLoading();
				https.request('/rest/member/pointsMall/couponsMemberRelation/list', {
					pageNo: -1,
					pageSize: 20,
					isUsed: 0,
					isExpired: 0,
					isValid: 0
				}).then((result) => {
					toastService.hideLoading();
					if (result.success) {
						let afterDiscounts = [];
						var availableCouponSize = 0;
						result.data.records.forEach((res) => {
							//判断优惠券是否过期和是否已经使用
							//if (!res.couponsMemberRelationMap.isExpired && !res.couponsMemberRelationMap.isUsed) {
							//console.log(this.data)
							//判断是否已经满减，如果已经满减就取满减之后的字段值，反之取总额
							//if (this.data.data.actualPrice && !this.data.data.fullPriceReduction) {

							//判断当前优惠券是折扣还是满减券,1等于折扣,2等于满减
							if (res.couponsMemberRelationMap.preferentialType == 1) {
								availableCouponSize++;
								//遍历当前订单的商品
								prePageData.orderDetailList.forEach((order) => {
									console.log(order);
									console.log(order.price);
									//遍历当前优惠券绑定的优惠商品
									res.goodsList.forEach((goods) => {
										console.log(order.price);
										//console.log(goods)
										//console.log(order)
										//判断当前订单的商品是否等于优惠券绑定的优惠商品
										//等于则进行优惠
										if (order.goodsId == goods.id) {
											// console.log(order.price)
											// console.log(order.number)
											// console.log(res.couponsMemberRelationMap.discountAmount != 0 ? utilHelper.toFixed((order.price / order.number) - ((order.price / order.number) * res.couponsMemberRelationMap.discountAmount), 2) : (order.price / order.number))
											afterDiscounts.push({
												id: res.couponsMemberRelationMap
													.id,
												price: res.couponsMemberRelationMap
													.discountAmount != 0 ?
													utilHelper.toFixed(order
														.price - order.price * res
														.couponsMemberRelationMap
														.discountAmount, 2) : order
													.price,
												goodsId: order.goodsId,
												couponsId: res
													.couponsMemberRelationMap
													.couponsId,
												couponsName: res
													.couponsMemberRelationMap
													.couponsName,
												preferentialType: systemStatus
													.preferentialTypeText(res
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
										}
									});
								});
								return;
							}
							//判断如果是优惠券满减的话这就进行优惠券的总价满减
							//console.log(res)
							if (res.couponsMemberRelationMap.preferentialType == 2) {
								if (prePageData.fullPriceReduction) {
									if (prePageData.fullPriceReduction >= res.couponsMemberRelationMap
										.limitedPrice) {
										//console.log(res.couponsMemberRelationMap.limitedPrice)
										afterDiscounts.push({
											id: res.couponsMemberRelationMap.id,
											couponsId: res.couponsMemberRelationMap.couponsId,
											price: utilHelper.toFixed(prePageData
													.fullPriceReduction, 2) - res
												.couponsMemberRelationMap.reducedPrice,
											couponsName: res.couponsMemberRelationMap.couponsName,
											preferentialType: systemStatus.preferentialTypeText(res
												.couponsMemberRelationMap.preferentialType),
											isExpired: res.couponsMemberRelationMap.isExpired,
											isUsed: res.couponsMemberRelationMap.isUsed,
											isValid: res.couponsMemberRelationMap.isValid
										});
									}
									return;
								}
								if (prePageData.actualPrice >= res.couponsMemberRelationMap.limitedPrice) {
									afterDiscounts.push({
										id: res.couponsMemberRelationMap.id,
										couponsId: res.couponsMemberRelationMap.couponsId,
										price: prePageData.actualPrice - res
											.couponsMemberRelationMap.reducedPrice,
										couponsName: res.couponsMemberRelationMap.couponsName,
										preferentialType: systemStatus.preferentialTypeText(res
											.couponsMemberRelationMap.preferentialType),
										isExpired: res.couponsMemberRelationMap.isExpired,
										isUsed: res.couponsMemberRelationMap.isUsed,
										isValid: res.couponsMemberRelationMap.isValid
									});
								}
							}
							//}
							//}
						});
						//console.log(afterDiscounts)
						//如果优惠券的使用张数大于0张
						prePageData.couponsIsHidden = false;
						prePageData.actualFullPriceReduction = prePageData.actualPrice;
						console.log(prePageData.actualPrice);
						if (afterDiscounts.length > 0) {
							var afterDiscountPrice = 0;
							var price = 0;
							var afterDiscountList; //遍历所有可以使用优惠券的商品，并计算出最大的优惠券
							afterDiscounts.forEach((afterDiscount) => {
								price = Number(afterDiscount.price);
								if (Number(price) >= afterDiscountPrice) {
									afterDiscountPrice = price;
									afterDiscountList = afterDiscount;
								}
							});
							//console.log(this.data.data.fullPriceReduction)
							//console.log(afterDiscountPrice)
							//let fullPriceReduction = prePageData.fullPriceReduction ? (prePageData.fullPriceReduction - afterDiscountPrice) : (prePageData.actualPrice - afterDiscountPrice)
							//console.log(prePageData.packingCharges)
							//获取总金额（加配送费）减去优惠券的最后优惠价格
							//⚠️ 原式 `actualPrice + packingCharges - afterDiscountPrice` 是**符号错误**：
							//afterDiscountPrice 是「券后应付价」（上面按 `actualPrice - reducedPrice` 算出），
							//不是「优惠金额」，相减得到的恰好是优惠额本身。
							//实测：商品合计 297、券「满50减8」→ afterDiscountPrice=289，原式算出 8
							//→ 下单被后端「订单实付款计算错误，请刷新页面重试」拒绝（商城整个下不了单）。
							//正确口径 = 券后应付价 + 包装费。
							let fullPriceReduction = Number(afterDiscountPrice) + Number(prePageData
								.packingCharges);
							prePageData.couponsIsHidden = true; //设置使用优惠券
							prePageData.fullPriceReduction = utilHelper.toFixed(fullPriceReduction, 2);
							//设置使用优惠券后价格属性，满减会使用到
							prePageData.actualFullPriceReduction = prePageData.fullPriceReduction;
							// prePageData.actualPrice=fullPriceReduction;
							console.log(prePageData.actualFullPriceReduction);

							this.afterDiscount = afterDiscountList;
							this.availableCouponSize = availableCouponSize;
							this.data.fullPriceReduction = utilHelper.toFixed(fullPriceReduction <= 0 ? 0 :
								fullPriceReduction, 2);
							this.data.couponsIsHidden = true;
						}
						// 自动满减已下线（2026-09-29 A 批）：不再「先券后满减」叠加，优惠仅来自所选优惠券
					}
				});
			},

			onCoupon() {
				let afterDiscount = this.afterDiscount;
				afterDiscount.fullPriceReduction = this.data.fullPriceReduction;
				afterDiscount.actualPrice = this.data.actualPrice;
				afterDiscount.fullPriceReductionIsHidden = this.data.fullPriceReductionIsHidden;
				afterDiscount.orderDetailList = this.data.orderDetailList;
				afterDiscount.fullPriceReductionAfter = this.data.fullPriceReductionAfter;
				afterDiscount.limitedPrice = this.data.limitedPrice;
				// 自动满减已下线（2026-09-29 A 批）：不再向选券页透传 fullReductionRuleId
				// afterDiscount.fullReductionRuleId = this.data.fullReductionRuleId;
				afterDiscount.fullPriceReductionIsHidden = this.data.fullPriceReductionIsHidden;
				// afterDiscount.packingCharges = this.data.data.packingCharges;
				afterDiscount.type = 2;
				//console.log(afterDiscount)
				uni.navigateTo({
					url: '../../../mine/coupons/coupons?prevData=' + JSON.stringify(afterDiscount)
				});
			},

			selectCommissionReward() {
				https.request('/rest/member/pointsMall/order/selectCommissionReward', {
					actualPrice: !this.data.fullPriceReductionIsHidden && !this.data.couponsIsHidden ? this.data
						.actualPrice : this.data.fullPriceReduction
				}).then((result) => {
					if (result.success) {
						// C-10 批次6（口径变更）：返现/返利全面取消 → 后端已不返回文案，此处自然为空
						this.tipReward = result.data || '';
					}
				});
			},

			/**
			 * 获取焦点
			 */
			getFocus: function() {
				var _this = this;
				this.payFocus = false;
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
					// 2026-09-25 恢复余额支付：按选中支付方式分流（2=余额）；
					// 积分支付按口径（积分仅用于等级成长）维持取消，原 pointsPay 分支与死代码已清。
					if (this.paymentModes[this.paymentModeIndex] && this.paymentModes[this.paymentModeIndex].value == 2) {
						this.balancePay();
					}
				}
			},

			balancePay() {
				var password = Base64.encode(this.pwdVal);
				this.pwdVal = '';
				this.payFocus = true;
				toastService.showLoading("支付中...");
				https.request('/rest/member/pointsMall/platformPay/byBalance', {
					openid: this.balanceOpenId,
					type: 6,
					paymentMode: 2,
					paymentPassword: password,
					out_trade_no: this.balanceOrderNo,
					total_fee: this.data.fullPriceReductionIsHidden ? this.data.fullPriceReduction : this
						.balanceActualPrice
				}).then((result) => {
					toastService.hideLoading();
					if (result.success) {
						toastService.showSuccess('支付成功', true);
						this.hidePwdLayer();
						let timeout = setTimeout(() => {
							uni.redirectTo({
								url: '../../mall/order/detail/detail?id=' + this.balanceId
							});
							clearTimeout(timeout);
						}, 1000);
						return;
					}
					// 2026-09-25 恢复余额支付：失败分支（后端安全加固 S2/S3 文案驱动引导）
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

			balancePayFail() {
				this.hidePwdLayer();
				toastService.showError('支付失败', true);
				let timeout = setTimeout(() => {
					uni.redirectTo({
						url: '../../mall/order/detail/detail?id=' + this.balanceId
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
			},

			/**
			 * 隐藏支付密码输入层
			 */
			forgetThePassword: function() {
				uni.navigateTo({
					url: '../../../mine/security/index/index'
				});
			},

			weChatPayTap() {
				//判断店铺是否打烊
				var data = {};
				// 支付方式随支付方式卡的选择提交：后端 insert 按 paymentMode 分流
				//   1（微信支付）→ status=1 未付款 + 5 分钟支付截止（走在线支付）
				//   4（到店支付）→ status=4 待自提（无需在线支付）
				// ⚠️ 不传时后端默认按「到店支付」处理，微信支付分支就走不到了。
				data.paymentMode = this.paymentModes[this.paymentModeIndex]
					? this.paymentModes[this.paymentModeIndex].value : 4;
				// C-10 批次6（口径变更）：商城只做门店自提 → 不再校验/提交配送地址与配送费，改传自提门店 id
				// ⚠️ 门店信息可能因定位失败而为空：此时**不要传 shopId**。
				// 传空字符串会让后端 Integer 反序列化抛 NumberFormatException → 前端只看到「系统异常，请稍后重试」。
				if (this.pickupShop && this.pickupShop.id) {
					data.shopId = this.pickupShop.id;
				}
				var list = this.data.orderDetailList;
				var orderDetailList = [];
				data.shoppingCartIdList = [];
				for (var key in list) {
					orderDetailList.push({
						goodsId: list[key].goodsId,
						goodsName: list[key].goodsName,
						specList: list[key].specList,
						number: list[key].number
					});
					if (this.payType == 'car') {
						data.shoppingCartIdList.push(list[key].id);
					}

				}
				//console.log(this.data)
				data.orderDetailList = orderDetailList;
				data.actualPrice = this.data.fullPriceReductionIsHidden || this.data.couponsIsHidden ? this.data
					.fullPriceReduction : this.data.actualPrice;
				// data.packingCharges = this.data.data.packingCharges;
				data.remark = this.remarks;
				// C-10 批次6（口径变更）：商城只做门店自提 → 固定 1（后端亦强制为 1）
				data.shoppingWay = 1;
				//console.log(data)
				if (this.data.couponsIsHidden) {
					data.couponsId = this.afterDiscount.couponsId;
					data.couponsMemberRelationId = this.afterDiscount.id;
					data.couponsDescription = this.afterDiscount.couponsName;
				}
				//console.log(this.data)
				// 自动满减已下线（2026-09-29 A 批）：不再向订单写入 fullReductionRuleId（后端同步停写，列保留兼容历史单）
				var _this = this;
				// toastService.showModal("积分商城订单", "订单确认后将无法更改!\r\n确认下单吗?", function comfirm() {

				//防重令牌
				data.orderToken = this.orderToken;
				// C-10 批次6（口径变更）：返现/会员引导已取消 → 不再按会员身份弹窗，直接下单
				toastService.showLoading();
				https.request('/rest/member/pointsMall/order/insert', data).then((result) => {
					toastService.hideLoading();
					if (result.success) {
						_this.toPay4Applet(result.data.id, result.data.orderNo, result.data.actualPrice);
					} else {
						toastService.showToast(result.message || '下单失败');
					}
				}).catch(() => {
					toastService.hideLoading();
					toastService.showToast('下单失败，请重试');
				});
				// }, null)
			},
			toPay4Applet(id, orderNo, actualPrice) {
				var _this = this;
				authService.getOpenId().then((openId) => {
					//console.log(openId)
					// if (!openId) {
					//   toastService.showToast("登录用户错误，请重新登录");
					//   return
					// }
					if (this.paymentModes[this.paymentModeIndex].value == 1) {
						this.weChatPay(id, orderNo, actualPrice, openId);
						return;
					}
					if (this.paymentModes[this.paymentModeIndex].value == 4) {
						// 到店支付：后端下单时已置 status=4（待自提），无需在线支付 → 直接进订单详情。
						// （原实现无该分支，选到店支付点「去支付」后下单成功却毫无反应）
						uni.redirectTo({
							url: '../../mall/order/detail/detail?id=' + id
						});
						return;
					}
					if (this.paymentModes[this.paymentModeIndex].value == 2 || this.paymentModes[this
							.paymentModeIndex].value == 3) {
						this.balanceId = id;
						this.balanceOrderNo = orderNo;
						this.balanceActualPrice = actualPrice;
						this.balanceOpenId = openId;

						_this.$nextTick(() => {
							_this.showPayPwdInput = true;
							//_this.payFocus = true;
							_this.getFocus();
						});
					}
				});
			},

			weChatPay(id, orderNo, actualPrice, openId) {
				const totalFee = this.data.fullPriceReductionIsHidden ? this.data.fullPriceReduction : actualPrice;
				//MVP 演示：无微信支付商户号 → 走模拟支付（后端与真实回调同一套订单流转）
				if (payService.isMock()) {
					payService.mockPay({
						type: 4,
						out_trade_no: orderNo,
						total_fee: totalFee
					}).then(() => {
						toastService.hideLoading();
						toastService.showSuccess('支付成功', true);
						let timeout = setTimeout(() => {
							uni.redirectTo({
								url: '../../mall/order/detail/detail?id=' + id
							});
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
					type: 4,
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
							success(res) {
								toastService.showSuccess('支付成功', true);
								let timeout = setTimeout(() => {
									uni.redirectTo({
										url: '../../mall/order/detail/detail?id=' + id
									});
									clearTimeout(timeout);
								}, 1000);
							},
							fail(res) {
								toastService.showError('支付失败', true);
								let timeout = setTimeout(() => {
									uni.redirectTo({
										url: '../../mall/order/detail/detail?id=' + id
									});
									clearTimeout(timeout);
								}, 1000);
							}
						});
					}
				});
			},

			// toPay4Applet(id, orderNo, actualPrice) {
			//   toastService.showLoading("正在加载...", true);
			//   authService.getOpenId().then(openId => {
			//     //console.log(openId)
			//     if (openId) {
			//       https.request('/rest/member/order/cancelOrder', {
			//         openid: openId,
			//         type: 1,
			//         out_trade_no: orderNo,
			//         total_fee: this.data.data.fullPriceReductionIsHidden ? this.data.data.fullPriceReduction : actualPrice
			//       }).then(result => {
			//         toastService.hideLoading();
			//         if (result.success) {
			//           //console.log(result)
			//           wx.requestPayment({
			//             appId: result.data.appid,
			//             timeStamp: result.data.timeStamp,
			//             nonceStr: result.data.nonceStr,
			//             package: result.data.package,
			//             signType: 'MD5',
			//             paySign: result.data.paySign,
			//             success(res) {
			//               toastService.showSuccess("支付成功", true);
			//               let timeout = setTimeout(() => {
			//                 wx.redirectTo({
			//                   url: '../order-details/order-details?id=' + id,
			//                 });
			//                 clearTimeout(timeout);
			//               }, 1000)
			//             },
			//             fail(res) {
			//               toastService.showError("支付失败", true);
			//               let timeout = setTimeout(() => {
			//                 wx.redirectTo({
			//                   url: '../order-details/order-details?id=' + id,
			//                 });
			//                 clearTimeout(timeout);
			//               }, 1000)
			//             }
			//           })
			//         }
			//       })
			//       return
			//     }
			//     toastService.showToast("登录用户错误，请重新登录");
			//   });
			// },
			// 自动满减已下线（2026-09-29 A 批）：原 getFullReductionRule 方法删除，商城优惠统一走满减券
			customerServiceWechat() {
				console.log('占位：函数 customerServiceWechat 未声明');
			},

			goToPay() {
				console.log('占位：函数 goToPay 未声明');
			},

			trueFun() {
				console.log('占位：函数 true 未声明');
			}
		}
	};
</script>
<style>
	page {
		background: #f5f5f5;
		padding-bottom: 99rpx;
	}

	.please-address {
		font-size: 32rpx;
		font-weight: bold;
		color: gainsboro;
	}

	.margin-border-radius {
		margin: 20rpx;
		border-radius: 15rpx;
	}

	.ask-for-delivery-house-view {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-bottom: 20rpx;
		width: 100%;
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

	.ask-for-delivery-detail {
		margin-top: 20rpx;
		padding: 20rpx;
		background: white;
	}

	.ask-for-delivery-title {
		font-size: 32rpx;
		font-weight: bold;
		line-height: 60rpx;
	}

	.ask-for-delivery {
		display: flex;
		align-items: center;
	}

	.ask-for-delivery-address {
		width: 75%;
	}

	.ask-for-delivery-house {
		font-size: 28rpx;
		display: flex;
		align-items: center;
	}

	.right-class {
		width: 100%;
		margin-left: 10rpx;
	}

	.shopping-commodity-details {
		margin-top: 10rpx;
		padding: 20rpx;
		background: white;
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
		font-size: 30rpx;
		font-weight: bold;
	}

	.commodity-types {
		font-size: 30rpx;
		color: #7f7f7f;
	}

	.commodity-totalnum {
		font-size: 30rpx;
	}

	.commodity-price {
		font-size: 28rpx;
		font-weight: bold;
		margin-left: 20rpx;
	}

	.commodity-totalnum-price {
		width: 25%;
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
		margin-top: 20rpx;
		padding: 20rpx;
		font-size: 30rpx;
	}

	.pay-mode-title {
		width: 20%;
	}

	.choose-pay-mode {
		display: flex;
		align-items: center;
	}

	.ask-for-remarks-view {
		background: white;
		margin-top: 20rpx;
		padding: 0 20rpx;
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

	.remarks-view {
		padding-bottom: 20rpx;
	}

	.textarea-remarks {
		width: 94%;
		padding: 20rpx;
		background: #f5f5f5;
		border-radius: 10rpx;
		height: 120rpx;
		font-size: 28rpx;
	}

	.go-pay-view {
		position: fixed;
		bottom: 0;
		width: 100%;
		line-height: 100rpx;
		display: flex;
		align-items: center;
		background: white;
		box-shadow: -2px 0px 5px 0.5px rgba(0, 0, 0, 0.2);
		font-size: 36rpx;
		z-index: 0;
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

	.full-reduction-view {
		font-size: 24rpx;
		font-weight: bold;
		padding: 0 10rpx;
		margin: 0 5rpx;
		border-radius: 10rpx;
	}

	.fullPriceReductionText {
		position: relative;
		left: -47%;
		font-size: 30rpx;
		font-weight: bold;
		top: 0;
	}

	.fullPriceReductionClass {
		color: gainsboro;
	}

	.after-discount {
		font-size: 28rpx;
		font-weight: bold;
	}

	.iconwechat_pay {
		color: #09bb07;
		font-size: 40rpx;
	}

	.iconjifen {
		color: #f0dcab;
		font-size: 35rpx;
	}

	.good-choice-btn {
		width: 100%;
		text-align: center;
		padding: 20rpx 0;
		border-radius: 15rpx;
		font-size: 28rpx;
		font-weight: bold;
	}

	.radio-group-address {
		display: flex;
		justify-items: center;
		justify-content: center;
		flex-direction: column;
	}

	.radio-label-address {
		display: flex;
		align-items: center;
	}

	.choose-address-dialog {
		width: 100%;
		border-bottom: 0.5rpx solid #ededed;
		padding: 20rpx 20rpx 20rpx 0;
	}

	.address-detail {
		font-size: 28rpx;
	}

	.address-name-phone {
		font-size: 26rpx;
		color: #7f7f7f;
		margin-right: 20rpx;
	}

	.footer-btns {
		display: flex;
		align-items: center;
		justify-content: space-between;
		border-top: 1rpx solid #ededed;
		padding-top: 20rpx;
	}

	.footer-btns view {
		width: 48%;
	}

	.iconbianji-copy {
		font-size: 50rpx;
	}

	::-webkit-scrollbar {
		width: 0;
		height: 0;
		color: transparent;
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

	.radio-label-payment.flex_center.payment-checked .iconjifen {
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

	.weui-dialog.extClassIsVip {
		background-color: rgba(255, 255, 255, 0);
	}

	.weui-dialog.extClassIsVip .weui-dialog__bd {
		padding: 0;
	}

	.close-view-class .iconfont.icon55 {
		font-size: 62rpx;
		color: gainsboro;
	}

	/* C-10 批次6：自提门店卡（替代原收货地址卡，画板 B-05） */
	.pickup-shop-card {
		display: flex;
		align-items: center;
		padding: 24rpx 20rpx;
		background-color: #ffffff;
	}

	.pickup-shop-icon {
		font-size: 40rpx;
		margin-right: 18rpx;
		line-height: 44rpx;
		flex-shrink: 0;
	}

	.pickup-shop-main {
		flex: 1;
		min-width: 0;
	}

	.pickup-shop-name {
		font-size: 30rpx;
		font-weight: 600;
		color: #202020;
		line-height: 42rpx;
	}

	.pickup-shop-addr {
		font-size: 26rpx;
		color: #909090;
		line-height: 36rpx;
		margin-top: 4rpx;
	}

	.pickup-shop-tip {
		font-size: 24rpx;
		color: #999999;
		margin-top: 4rpx;
	}

</style>