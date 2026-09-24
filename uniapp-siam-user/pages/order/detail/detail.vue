<template>
	<view class="page">
		<!-- 状态头（C-11 163:106 蓝底） -->
		<view class="status-header">
			<text class="status-title">{{ statusTitle }}</text>
			<text class="status-desc">{{ statusDesc }}</text>
		</view>

		<!-- 门店行卡（163:109） -->
		<view class="shop-row-card" hover-class="row-hover" @tap="contactShop">
			<text class="shop-row-name">{{ order.shopName }}</text>
			<text class="shop-row-contact">联系门店</text>
		</view>

		<!-- 取餐码卡（163:112）：仅外带自提单展示；堂食单扫码已带桌号，出品按桌送 -->
		<view class="queue-card" v-if="showPickupCode">
			<text class="queue-label">取餐码</text>
			<text class="queue-code">{{ pickupCode }}</text>
			<text class="queue-tip">向店员出示此码，核销后取货</text>
		</view>

		<!-- 出品进度卡（2026-09-23 新增）：订单进行中时让顾客看到「做到哪了」。
		     数据源＝后端已返回的 order.productionStatus（1待制作/2出品中/3已出品），无需新接口 -->
		<view class="production-card" v-if="showProduction">
			<text class="production-label">出品进度</text>
			<view class="production-steps">
				<view class="prod-step" v-for="(s, i) in productionSteps" :key="i">
					<view :class="'prod-dot' + (i <= productionIndex ? ' on' : '')"></view>
					<text :class="'prod-text' + (i <= productionIndex ? ' on' : '')">{{ s }}</text>
				</view>
			</view>
			<text class="production-tip">{{ productionTip }}</text>
		</view>

		<!-- 退款进度 -->
		<view class="refund-card" v-if="order.isRefundOrder" hover-class="row-hover" @tap="bindRefundProcess">
			<text class="refund-label">退款进度</text>
			<view class="refund-right">
				<text class="refund-status">{{ order.refundStatusText }}</text>
				<van-icon name="arrow" color="#5f7d22" size="14px" />
			</view>
		</view>

		<!-- 明细卡（163:116） -->
		<view class="detail-card">
			<text class="card-title">商品明细</text>
			<view class="goods-row" v-for="(item, index) in orderDetailList" :key="index">
				<view class="goods-name-box">
					<text class="goods-name">{{ item.goodsName }}</text>
					<text class="goods-spec" v-if="item.specListAnalysis">（{{ item.specListAnalysis }}）</text>
					<text class="goods-num">×{{ item.number }}</text>
				</view>
				<text class="goods-subtotal">¥{{ item.lineSubtotal }}</text>
			</view>
			<view class="fee-row" v-if="Number(order.packingCharges) > 0">
				<text>包装费</text>
				<text>¥{{ order.packingCharges }}</text>
			</view>
			<view class="fee-row" v-if="Number(order.deliveryFee) > 0">
				<text>配送费</text>
				<text>¥{{ order.deliveryFee }}</text>
			</view>
			<view class="fee-row" v-if="order.fullReductionRuleId">
				<text>满减优惠</text>
				<text>{{ order.reducedPrice ? '-¥' + order.reducedPrice : order.fullReductionRuleDescription }}</text>
			</view>
			<view class="fee-row" v-if="order.couponsId">
				<text>优惠券</text>
				<text>{{ order.couponsDescription }}</text>
			</view>
			<view class="divider"></view>
			<view class="sum-row">
				<text class="sum-label">合计</text>
				<text class="sum-value">¥{{ order.actualPrice }}</text>
			</view>
		</view>

		<!-- 订单信息卡（163:128） -->
		<view class="info-card">
			<text class="info-row">订单编号  {{ order.orderNo }}</text>
			<text class="info-row">下单时间  {{ order.createTime }}</text>
			<text class="info-row" v-if="order.tableName">台位  {{ order.tableName }}</text>
			<text class="info-row" v-if="Number(order.diningNum) > 0">就餐人数  {{ order.diningNum }} 人</text>
			<text class="info-row" v-if="order.paymentModeText && Number(order.status) != 1">支付方式  {{ order.paymentModeText }}</text>
			<text class="info-row"
				v-if="order.shoppingWay == 2 && order.contactRealname">收货信息  {{ order.contactProvince }}{{ order.contactCity }}{{ order.contactArea }}{{ order.contactStreet }} {{ order.contactRealname }} {{ order.contactPhone }}</text>
		</view>

		<!-- 底部按钮占位 -->
		<view class="bottom-space" v-if="bottomButtons.length > 0"></view>

		<!-- 底部按钮栏（163:133）：more=左下角小字灰色「更多」（收纳取消订单） -->
		<view class="bottom-bar" v-if="bottomButtons.length > 0">
			<view v-for="btn in bottomButtons" :key="btn.key"
				:class="'bar-btn ' + (btn.type == 'primary' ? 'btn-primary' : (btn.type == 'more' ? 'bar-more' : 'btn-stroke'))"
				hover-class="btn-hover" @tap="onBarButton(btn.key)">{{ btn.text }}</view>
		</view>

		<!-- 取消原因（无理由取消） -->
		<van-action-sheet :show="choiceReasonDialog" title="选择取消原因" @close="closeSheet" @cancel="closeSheet"
			z-index="10">
			<view class="content_box">
				<scroll-view style="height: 55vh" scroll-y>
					<radio-group class="choiceReason-radio-group" @change="radioChange">
						<label
							:class="'choiceReason-lable ' + (refundReasonRapidly.length - 1 != index ? 'choiceReason-border' : '')"
							v-for="(item, index) in refundReasonRapidly" :key="index">
							{{ item.name }}
							<radio :value="item.value"></radio>
						</label>
					</radio-group>
				</scroll-view>
				<view class="content_box_footer">
					<view class="good-choice-btn theme-bg" @tap="cancelOrderNoReasonFun">确定</view>
				</view>
			</view>
		</van-action-sheet>

		<!-- 支付方式选择 -->
		<van-action-sheet :show="dialogShow" :actions="paymentModes" @select="goToPay" cancel-text="取消" :title="title"
			@cancel="closeDialog" @close="closeDialog"></van-action-sheet>

		<!-- 余额支付密码 -->
		<van-dialog use-slot :show="showPayPwdInput" :showConfirmButton="false" :showCancelButton="false" z-index="12">
			<view class="flex_between content_box">
				<view></view>
				<view>输入支付密码</view>
				<van-icon name="cross" @tap="balancePayFail" />
			</view>
			// #ifdef APP-PLUS||H5
			<view class="content_box" style="padding: 0 0;">
				<view class="password_dialog_tip" style="padding: 0 16px;"><text>使用会员卡余额支付需要验证身份，验证通过后才可进行支付。</text></view>
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
		<van-number-keyboard :show="payFocus" @blur="payFocus = false" @input="inputPwd" @delete="deletePwd" />
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
	import dateHelper from '../../../utils/date-helper';
	import systemStatus from '../../../utils/system-status';

	var orderId = '';
	let app = null;
	export default {
		data() {
			return {
				refundReasonRapidly: [{
						value: 1,
						name: '信息填写错误'
					},
					{
						value: 2,
						name: '送达时间选错了'
					},
					{
						value: 3,
						name: '买错了/买少了'
					},
					{
						value: 4,
						name: '商家缺货'
					},
					{
						value: 5,
						name: '商家联系我取消'
					},
					{
						value: 6,
						name: '配送太慢'
					},
					{
						value: 7,
						name: '骑手联系我取消'
					},
					{
						value: 8,
						name: '我不想要了'
					}
				],
				refundStatusTexts: [{
						value: 1,
						name: '退款申请已提交'
					},
					{
						value: 2,
						name: '等待商家处理'
					},
					{
						value: 3,
						name: '商家拒绝退款'
					},
					{
						value: 4,
						name: '等待平台处理'
					},
					{
						value: 5,
						name: '平台拒绝退款，退款已关闭'
					},
					{
						value: 6,
						name: '退款已关闭'
					},
					{
						value: 7,
						name: '退款成功'
					}
				],
				choiceReasonDialog: false,
				showPayPwdInput: false,
				pwdVal: '',
				payFocus: false,
				adjustPosition: false,
				isForgetThePassword: false,
				userInfo: '',
				dialogShow: false,
				maskClosable: false,
				title: '',
				paymentModes: [],
				paymentModeIndex: '',
				balanceId: '',
				balanceOpenId: '',
				cancelOrderNoReason: '',
				order: {
					status: 0,
					queueNo: '',
					statusText: '',
					isAllowCancelNoReason: '',
					isAllowApplyRefund: '',
					isAllowAppraise: '',
					id: '',
					shopId: '',
					isRefundOrder: '',
					refundStatusText: '',
					shoppingWay: 0,
					orderNo: '',
					createTime: '',
					tableName: '',
					deliveryFee: '',
					packingCharges: '',
					fullReductionRuleId: '',
					fullReductionRuleDescription: '',
					couponsId: '',
					couponsDescription: '',
					actualPrice: '',
					paymentModeText: '',
					shopName: '',
					shopContactPhone: '',
					contactProvince: '',
					contactCity: '',
					contactArea: '',
					contactStreet: '',
					contactRealname: '',
					contactSex: 0,
					contactPhone: ''
				},
				orderDetailList: []
			};
		},
		computed: {
			statusTitle() {
				return this.order.statusText || '-';
			},
			/**
			 * 出品进度（2026-09-23 新增）。
			 * 读后端已返回的 productionStatus（1待制作 / 2出品中 / 3已出品；批次 D 新增列），
			 * 与「订单状态」是两套东西 —— 原先顾客只能看到「商家正在备餐」且永不变化。
			 */
			productionIndex() {
				const p = Number(this.order.productionStatus);
				return p >= 1 && p <= 3 ? p - 1 : -1;
			},
			/** 仅「已支付且未到终态」时展示：status 2=待处理 3=待自取/已处理 4=待配送 */
			showProduction() {
				return this.productionIndex >= 0 && [2, 3, 4].indexOf(Number(this.order.status)) !== -1;
			},
			productionSteps() {
				return ['已接单', '制作中', '已出品'];
			},
			/** 自提单在「已出品」时给出可取的明确信号 —— 这是本次优化的核心收益点 */
			productionTip() {
				const p = Number(this.order.productionStatus);
				if (p === 3) {
					return this.isDineIn
						? '已出品，即将送至您的桌位'
						: ('已出餐，凭取餐码 ' + (this.pickupCode || '') + ' 到前台取');
				}
				if (p === 2) return '正在制作中，请稍候';
				return this.isDineIn ? '后厨已接单' : '已接单，正在排单';
			},
			statusDesc() {
				const s = Number(this.order.status);
				if (s === 1) return '订单已提交，请在5分钟内完成支付';
				if (s === 2) return '支付成功，商家正在备餐';
				if (s === 3) return this.isDineIn ? '下单成功，餐品将送至您的桌位' :
					'下单成功，到店出示取餐码即可';
				if (s === 4) return '商家正在处理您的订单，请留意商家通知';
				if (s === 5 || s === 6) return '订单已完成，感谢惠顾';
				if (s === 7) return '售后处理中，请耐心等待';
				if (s === 8) return '退款已原路退回，请注意查收';
				if (s === 9) return '售后处理完成，期待您再次光临';
				if (s === 10 || s === 11) return '订单已取消，期待您再次光临';
				return '';
			},
			// 堂食单判定：扫码进桌的订单带桌号快照（tableName），出品按桌送、不出取餐码
			// TODO 后端 Order 实体补 orderScene 映射后，可改用 orderScene === 2 精确判定自提单
			isDineIn() {
				return !!this.order.tableName;
			},
			// 自提单取餐码：整数流水号格式化为设计稿的「A-038」形态
			pickupCode() {
				const no = Number(this.order.queueNo);
				if (!no) return '';
				const digit = no < 10 ? '00' + no : (no < 100 ? '0' + no : String(no));
				return 'A-' + digit;
			},
			// 取餐码卡：仅自提单、且已支付未完成期间展示
			showPickupCode() {
				return !this.isDineIn && !!this.order.queueNo &&
					[2, 3].indexOf(Number(this.order.status)) != -1;
			},
			// 底部按钮栏（C-11 163:133）；2026-09-24 拍板：取消订单收进左下角「更多」（小字灰色），
			// 栏内只留主按钮（立即支付/再来一单/评价）
			bottomButtons() {
				const s = Number(this.order.status);
				const btns = [];
				if (s === 1) {
					btns.push({
						key: 'more',
						text: '更多',
						type: 'more'
					});
					btns.push({
						key: 'pay',
						text: '立即支付',
						type: 'primary'
					});
					return btns;
				}
			if (s >= 2 && s <= 5) {
				// 「申请退款」入口已下线（2026-09-23 13:5x 用户拍板）；已申请的退款进度仍走上方「退款进度」卡
				if (s <= 3 && this.order.isAllowCancelNoReason) {
						btns.push({
							key: 'more',
							text: '更多',
							type: 'more'
						});
					}
				}
				if (s === 6 && this.order.isAllowAppraise) {
					btns.push({
						key: 'evaluate',
						text: '评价',
						type: 'stroke'
					});
				}
				btns.push({
					key: 'reorder',
					text: '再来一单',
					type: 'primary'
				});
				return btns;
			}
		},
		onLoad: function(options) {
			app = getApp();
			orderId = options.id;
			this.getOrderDetail(orderId);
		},
		onShow: function() {
			if (this.isForgetThePassword || this.showPayPwdInput) {
				if (this.paymentModes[this.paymentModeIndex] && this.paymentModes[this.paymentModeIndex]
					.value == 2) {
					this.showPwdLayer();
					this.isForgetThePassword = false;
				}
			}
			this.getLoginMemberInfo();
		},
		onPullDownRefresh: function() {},
		onReachBottom: function() {},
		onShareAppMessage: function() {},
		methods: {
			getLoginMemberInfo: function() {
				https.request('/rest/member/getLoginMemberInfo', {}).then((result) => {
					if (result.success) {
						this.userInfo = result.data;
					}
				});
			},

			getOrderDetail(id) {
				https.request('/rest/member/order/selectById', {
					id: id
				}).then((result) => {
					if (result.success) {
						const data = result.data;
						data.order.statusText = systemStatus.statusText(data.order.status);
						data.order.createTime = dateHelper.fmtDate(data.order.createTime);
						data.order.paymentModeText = systemStatus.paymentModeText(data.order
							.paymentMode);
						//解析订单商品的规格
						(data.orderDetailList || []).forEach((item) => {
							let specListAnalysis = '';
							try {
								const specMap = JSON.parse(item.specList || '{}');
								for (let key in specMap) {
									specListAnalysis = (specListAnalysis ? specListAnalysis + '/' :
										specListAnalysis) + specMap[key];
								}
							} catch (e) {
								specListAnalysis = '';
							}
							item.specListAnalysis = specListAnalysis;
							item.lineSubtotal = item.subtotal != null ? item.subtotal : (item
								.price * item.number);
						});
						for (let i in this.refundStatusTexts) {
							if (data.order.refundStatus == this.refundStatusTexts[i].value) {
								data.order.refundStatusText = this.refundStatusTexts[i].name;
							}
						}
						this.order = data.order;
						this.orderDetailList = data.orderDetailList || [];
					}
				});
			},

			contactShop() {
				if (this.order.shopContactPhone) {
					uni.makePhoneCall({
						phoneNumber: this.order.shopContactPhone
					});
				} else {
					toastService.showToast('暂无门店联系电话');
				}
			},

			reorder() {
				uni.switchTab({
					url: '/pages/menu/index/index'
				});
			},

			onBarButton(key) {
				if (key === 'more') this.showMoreSheet();
				if (key === 'cancel') this.cancelOrderTap();
				if (key === 'pay') this.openConfirm();
				if (key === 'evaluate') this.evaluateTip();
				if (key === 'reorder') this.reorder();
			},

			// 「更多」（2026-09-24 拍板）：收纳取消订单入口，小字灰色样式；后续更多次级动作往这里加
			showMoreSheet() {
				uni.showActionSheet({
					itemList: ['取消订单'],
					itemColor: '#333333',
					success: (res) => {
						if (res.tapIndex === 0) {
							this.cancelOrderTap();
						}
					}
				});
			},

			cancelOrderTap() {
				if (Number(this.order.status) === 1) {
					// 未付款：直接取消
					const that = this;
					toastService.showModal(null, '确定取消这个订单吗?', function() {
						https.request('/rest/member/order/cancelOrder', {
							id: orderId
						}).then((result) => {
							if (result.success) {
								toastService.showSuccess('取消成功', true);
								const timeout = setTimeout(() => {
									that.getOrderDetail(orderId);
									clearTimeout(timeout);
								}, 1500);
							}
						});
					}, null);
				} else if (this.order.isAllowCancelNoReason) {
					// 已付款：无理由取消（选择原因）
					this.choiceReasonDialog = true;
				}
			},

			closeSheet() {
				this.choiceReasonDialog = false;
			},

			radioChange(e) {
				this.cancelOrderNoReason = e.detail.value;
			},

			cancelOrderNoReasonFun() {
				toastService.showLoading();
				this.choiceReasonDialog = false;
				https.request('/rest/member/order/cancelOrderNoReason', {
					id: this.order.id,
					orderRefund: {
						refundReason: this.cancelOrderNoReason
					}
				}).then((result) => {
					toastService.hideLoading();
					if (result.success) {
						toastService.showSuccess('取消成功');
						this.getOrderDetail(this.order.id);
					}
				});
			},

			applyRefund() {
				uni.navigateTo({
					url: '../refund/apply/apply?orderId=' + this.order.id
				});
			},

			bindRefundProcess() {
				uni.navigateTo({
					url: '../refund/progress/progress?orderId=' + this.order.id
				});
			},

			evaluateTip() {
				uni.navigateTo({
					url: '../evaluate/evaluate?orderId=' + this.order.id + '&shopId=' + this.order
						.shopId
				});
			},

			/* ===== 支付流程（待付款订单） ===== */
			openConfirm() {
				const paymentModes = [{
						value: 1,
						name: '微信支付',
						icon: 'wechat-pay',
						show: true
					},
					{
						value: 2,
						name: '平台余额',
						icon: 'balance-pay',
						show: true
					}
				];
				if (Number(this.order.actualPrice) > Number(this.userInfo.balance || 0)) {
					paymentModes[1].subname = '余额不足';
					paymentModes[1].isBindTap = true;
				}
				if (!this.userInfo.paymentPassword) {
					paymentModes[1].subname = '未设置支付密码,去设置';
					paymentModes[1].isBindTap = false;
				}
				this.dialogShow = true;
				this.title = '请选择支付方式';
				this.paymentModes = paymentModes;
			},

			closeDialog: function() {
				this.dialogShow = false;
			},

			goToPay(e) {
				const that = this;
				authService.getOpenId().then((openId) => {
					that.closeDialog();
					let paymentModeIndex = 1;
					// #ifdef APP-PLUS||H5
					paymentModeIndex = e.value;
					// #endif
					// #ifdef MP-WEIXIN||MP-ALIPAY
					paymentModeIndex = e.detail.value;
					// #endif
					this.paymentModeIndex = paymentModeIndex;
					if (paymentModeIndex == 1) {
						//微信支付：MVP 演示环境无商户号，由 utils/pay.js 依 cloud.mockPay 自动改走模拟支付
						//（原为「暂不支持微信支付」硬拦，导致本页支付入口不可达；主支付页 menu/pay 一直是微信支付）
						toastService.showLoading('正在加载...', true);
						that.weChatPay(orderId, openId);
						return;
					}
					if (paymentModeIndex == 2) {
						if (!that.userInfo.paymentPassword) {
							uni.navigateTo({
								url: '../../mine/security/index/index'
							});
							return;
						}
						that.balanceId = orderId;
						that.balanceOpenId = openId;
						that.$nextTick(() => {
							that.showPayPwdInput = true;
							that.getFocus();
						});
					}
				}, null);
			},

			weChatPay(id, openId) {
				const that = this;
				//MVP 演示：无微信支付商户号 → 走模拟支付（后端与真实回调同一套订单流转）
				if (payService.isMock()) {
					payService.mockPay({
						type: 1,
						out_trade_no: this.order.orderNo,
						total_fee: this.order.actualPrice
					}).then(() => {
						toastService.hideLoading();
						toastService.showSuccess('支付成功', true);
						const timeout = setTimeout(() => {
							that.getOrderDetail(id);
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
					out_trade_no: this.order.orderNo,
					total_fee: this.order.actualPrice
				}).then((result) => {
					if (result.success) {
						toastService.hideLoading();
						uni.requestPayment({
							appId: result.data.appid,
							timeStamp: result.data.timeStamp,
							nonceStr: result.data.nonceStr,
							package: result.data.package,
							signType: 'MD5',
							paySign: result.data.paySign,
							success() {
								toastService.showSuccess('支付成功', true);
								const timeout = setTimeout(() => {
									that.getOrderDetail(id);
									clearTimeout(timeout);
								}, 1000);
							},
							fail() {
								toastService.showError('支付失败', true);
								const timeout = setTimeout(() => {
									that.getOrderDetail(id);
									clearTimeout(timeout);
								}, 1000);
							}
						});
					}
				});
			},

			deletePwd() {
				if (this.pwdVal.length === 0) {
					return;
				}
				this.pwdVal = this.pwdVal.substring(0, this.pwdVal.length - 1);
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

			getFocus: function() {
				this.payFocus = false;
				this.$nextTick(() => {
					// #ifdef APP-PLUS||H5
					this.payFocus = true;
					// #endif
					// #ifdef MP-WEIXIN||MP-ALIPAY
					setTimeout(() => {
						this.payFocus = true;
					}, 100);
					// #endif
				});
			},

			balancePay() {
				const password = Base64.encode(this.pwdVal);
				this.pwdVal = '';
				this.payFocus = true;
				https.request('/rest/member/platformPay/byBalance', {
					openid: this.balanceOpenId,
					type: 1,
					paymentMode: 2,
					paymentPassword: password,
					out_trade_no: this.order.orderNo,
					total_fee: this.order.actualPrice
				}).then((result) => {
					toastService.hideLoading();
					if (result.success) {
						toastService.showSuccess('支付成功', true);
						this.hidePwdLayer();
						const timeout = setTimeout(() => {
							this.getOrderDetail(orderId);
							clearTimeout(timeout);
						}, 1000);
					}
				});
			},

			balancePayFail() {
				this.hidePwdLayer();
				toastService.showError('支付失败', true);
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

			forgetThePassword: function() {
				this.isForgetThePassword = true;
				uni.navigateTo({
					url: '../../mine/security/index/index'
				});
			}
		}
	};
</script>
<style>
	page {
		background: #f5f5f5;
		padding-bottom: 0;
	}

	.page {
		min-height: 100vh;
		background: #f5f5f5;
	}

	/* ===== 状态头（C-11 163:106） ===== */
	.status-header {
		background: #5f7d22;
		padding: 32rpx 32rpx 40rpx;
		display: flex;
		flex-direction: column;
	}

	.status-title {
		font-size: 36rpx;
		font-weight: 700;
		color: #ffffff;
	}

	.status-desc {
		font-size: 24rpx;
		color: #ffffff;
		margin-top: 8rpx;
	}

	/* ===== 门店行卡（163:109） ===== */
	.shop-row-card {
		background: #ffffff;
		border-radius: 24rpx;
		height: 104rpx;
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 0 24rpx;
		margin-top: 20rpx;
	}

	.row-hover {
		opacity: 0.85;
	}

	.shop-row-name {
		font-size: 28rpx;
		font-weight: 600;
		color: #333333;
		flex: 1;
		margin-right: 16rpx;
		overflow: hidden;
		white-space: nowrap;
		text-overflow: ellipsis;
	}

	.shop-row-contact {
		font-size: 24rpx;
		color: #5f7d22;
		flex-shrink: 0;
	}

	/* ===== 取餐码卡（163:112） ===== */
	.queue-card {
		background: #ffffff;
		border-radius: 24rpx;
		padding: 32rpx;
		margin-top: 20rpx;
		display: flex;
		flex-direction: column;
	}

	.queue-label {
		font-size: 24rpx;
		color: #999999;
	}

	.queue-code {
		font-size: 80rpx;
		font-weight: 700;
		color: #333333;
		margin-top: 12rpx;
		letter-spacing: 4rpx;
	}

	.queue-tip {
		font-size: 22rpx;
		color: #999999;
		margin-top: 12rpx;
	}

	/* ===== 出品进度（2026-09-23）===== */
	.production-card {
		background: #ffffff;
		border-radius: 24rpx;
		padding: 32rpx;
		margin-top: 20rpx;
		display: flex;
		flex-direction: column;
	}

	.production-label {
		font-size: 24rpx;
		color: #999999;
	}

	.production-steps {
		display: flex;
		justify-content: space-between;
		margin-top: 24rpx;
	}

	.prod-step {
		display: flex;
		flex-direction: column;
		align-items: center;
	}

	.prod-dot {
		width: 20rpx;
		height: 20rpx;
		border-radius: 50%;
		background: #e0e0e0;
	}

	.prod-dot.on {
		background: #5f7d22;
	}

	.prod-text {
		font-size: 24rpx;
		color: #999999;
		margin-top: 12rpx;
	}

	.prod-text.on {
		color: #5f7d22;
		font-weight: 600;
	}

	.production-tip {
		font-size: 22rpx;
		color: #666666;
		margin-top: 20rpx;
	}

	/* ===== 退款进度 ===== */
	.refund-card {
		background: #ffffff;
		border-radius: 24rpx;
		height: 104rpx;
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 0 24rpx;
		margin-top: 20rpx;
	}

	.refund-label {
		font-size: 28rpx;
		font-weight: 600;
		color: #333333;
	}

	.refund-right {
		display: flex;
		align-items: center;
	}

	.refund-status {
		font-size: 24rpx;
		color: #5f7d22;
		margin-right: 8rpx;
	}

	/* ===== 明细卡（163:116） ===== */
	.detail-card {
		background: #ffffff;
		border-radius: 24rpx;
		padding: 24rpx;
		margin-top: 20rpx;
	}

	.card-title {
		font-size: 28rpx;
		font-weight: 500;
		color: #333333;
	}

	.goods-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-top: 16rpx;
	}

	.goods-name-box {
		flex: 1;
		margin-right: 16rpx;
		overflow: hidden;
		white-space: nowrap;
		text-overflow: ellipsis;
		font-size: 26rpx;
		color: #333333;
	}

	.goods-name {
		font-size: 26rpx;
		color: #333333;
	}

	.goods-spec {
		font-size: 22rpx;
		color: #999999;
	}

	.goods-num {
		font-size: 26rpx;
		color: #333333;
		margin-left: 8rpx;
	}

	.goods-subtotal {
		font-size: 26rpx;
		color: #333333;
		flex-shrink: 0;
	}

	.fee-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		font-size: 24rpx;
		color: #999999;
		margin-top: 12rpx;
	}

	.divider {
		height: 2rpx;
		background: #f5f5f5;
		margin-top: 20rpx;
	}

	.sum-row {
		display: flex;
		align-items: baseline;
		justify-content: space-between;
		margin-top: 20rpx;
	}

	.sum-label {
		font-size: 26rpx;
		color: #999999;
	}

	.sum-value {
		font-size: 30rpx;
		font-weight: 700;
		color: #f01414;
	}

	/* ===== 订单信息卡（163:128） ===== */
	.info-card {
		background: #ffffff;
		border-radius: 24rpx;
		padding: 24rpx;
		margin-top: 20rpx;
		display: flex;
		flex-direction: column;
	}

	.info-row {
		font-size: 24rpx;
		color: #999999;
		line-height: 44rpx;
		overflow: hidden;
		white-space: nowrap;
		text-overflow: ellipsis;
	}

	/* ===== 底部按钮栏（163:133） ===== */
	.bottom-space {
		height: 160rpx;
	}

	.bottom-bar {
		position: fixed;
		left: 0;
		right: 0;
		bottom: 0;
		z-index: 20;
		background: #ffffff;
		display: flex;
		justify-content: flex-end;
		padding: 20rpx 24rpx;
		padding-bottom: calc(20rpx + constant(safe-area-inset-bottom));
		padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
	}

	.bar-btn {
		/* 设计 163:133/163:134：按钮固定 110px（=220rpx），整排右对齐 */
		flex: none;
		width: 220rpx;
		height: 80rpx;
		border-radius: 40rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 28rpx;
	}

	.bar-btn + .bar-btn {
		margin-left: 20rpx;
	}

	.btn-hover {
		opacity: 0.85;
	}

	.btn-stroke {
		background: #ffffff;
		border: 1rpx solid #e0e0e0;
		color: #666666;
	}

	.btn-primary {
		background: #5f7d22;
		color: #ffffff;
		font-weight: 500;
	}

	/* 「更多」（09-24 拍板）：小字灰色文本钮，贴左；margin-right:auto 把主按钮推到右侧 */
	.bar-more {
		flex: none;
		width: auto;
		height: 80rpx;
		display: flex;
		align-items: center;
		margin-right: auto;
		padding: 0 12rpx;
		font-size: 26rpx;
		color: #999999;
	}

	/* ===== 弹层 ===== */
	.content_box {
		padding: 20rpx 0;
	}

	.flex_between {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.choiceReason-radio-group {
		display: flex;
		justify-content: center;
		flex-direction: column;
	}

	.choiceReason-lable {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 20rpx 32rpx;
		font-size: 30rpx;
	}

	.choiceReason-border {
		border-bottom: 1rpx solid #f5f5f5;
	}

	.good-choice-btn {
		width: 100%;
		text-align: center;
		padding: 20rpx 0;
		border-radius: 15rpx;
		font-size: 28rpx;
		font-weight: bold;
	}

	.password_dialog_tip {
		color: #979797;
		font-size: 26rpx;
		line-height: 40rpx;
	}

	.password_dialog_row {
		display: flex;
		align-items: center;
		justify-content: center;
		margin: 30rpx 0;
	}

	.password_dialog_item_input {
		width: 80rpx;
		height: 80rpx;
		border: 1rpx solid #e0e0e0;
		border-radius: 8rpx;
		margin: 0 8rpx;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.password_dialog_forget_pwd {
		text-align: center;
		font-size: 26rpx;
		padding-bottom: 30rpx;
	}

	.password_dialog_input_control {
		position: fixed;
		left: -9999rpx;
		width: 10rpx;
		height: 10rpx;
		opacity: 0;
	}

	::-webkit-scrollbar {
		width: 0;
		height: 0;
		color: transparent;
	}
</style>
