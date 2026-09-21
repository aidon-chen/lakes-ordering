import https from './http';
import GlobalConfig from './global-config';

/**
 * 模拟支付通道（仅 MVP 演示）
 *
 * 背景：演示环境没有微信支付商户号（后端 wxpay.appId/mchId/mchKey 为空），
 * 真实微信支付必然失败 → 下单后订单永远停在「待付款」，商家端看不到新单。
 *
 * 开关：utils/global-config.js 的 cloud.mockPay
 *   true  → 走 /rest/member/pay/mockPay（后端 @Profile("mvp")，生产环境该接口不存在=404）
 *   false → 调用方继续走真实微信支付，本模块不参与
 *
 * 后端 mockPay 按 type 分派到与微信支付回调（WxPayController.wxNotify）完全相同的业务方法：
 *   1=订单付款 → orderService.paymentNotify
 *   2=会员充值 → vipRechargeRecordService.updateByPayNotify
 *   3=自取订单改为配送 → orderService.paymentNotifyOfChangeToDelivery
 *   4=积分商城订单付款 → pointsMallOrderService.paymentNotify
 */
export default {
	/** 是否启用模拟支付 */
	isMock() {
		return !!(GlobalConfig.cloud && GlobalConfig.cloud.mockPay);
	},

	/**
	 * 发起模拟支付
	 * @param params 与 /rest/member/wxPay/toPay4Applet 的入参对齐：
	 *        { type, out_trade_no, total_fee, vipRechargeDenominationId, deliveryAddressId }
	 *        （openid 不需要——模拟支付不调微信，也就不需要 wx.login）
	 * @returns Promise  resolve=支付成功（含后端返回的 message/data）；reject={ message }
	 */
	mockPay(params) {
		return new Promise((resolve, reject) => {
			https.request('/rest/member/pay/mockPay', {
				type: params.type,
				//⚠️ 字段名与 toPay4Applet 的 out_trade_no 不同：mockPay 后端读的是驼峰 outTradeNo
				//（见 MockPayController.mockPay 的 params.get("outTradeNo")）。
				//调用方沿用真实支付的 out_trade_no 契约，在这里做一次显式映射，避免改 6 个调用点。
				outTradeNo: params.out_trade_no,
				total_fee: params.total_fee,
				vipRechargeDenominationId: params.vipRechargeDenominationId,
				deliveryAddressId: params.deliveryAddressId
			}).then((result) => {
				if (result.success) {
					resolve(result);
				} else {
					reject({
						message: result.message || '支付失败'
					});
				}
			}).catch((err) => {
				reject({
					message: (err && err.message) || '支付失败'
				});
			});
		});
	}
};