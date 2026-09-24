export default class GlabalConfig {
    // static baseUrl = 'https://api.test.siamit.cn/siam-server';
	// #ifdef H5
	static baseUrl = 'siam-server';
	// #endif
	// #ifdef APP-PLUS||MP-WEIXIN||MP-ALIPAY
	static baseUrl = 'https://api.show.siamit.cn/siam-server';
	// #endif

    // static baseUrl = 'http://localhost:9020';
    static ossUrl = 'https://lakes-public.oss-cn-chengdu.aliyuncs.com/';
    // ★ 本地联调时的图片前缀（指向本地后端存储端点，落盘优先 + 本地无图自动回源 OSS）：
    //   走 customer-cli 编译时无需手改——sync-src.sh 检测到接口是 localhost 会自动改写 src 副本；
    //   仅在 HBuilderX 里直接跑本正本调试时，才把上面那行换成下面这行。
    // static ossUrl = 'http://localhost:9411/siam-server/upload/';

	/**
	 * 微信云托管通道（04-deploy-cost/mvp-migration/05）
	 * env 填入云托管环境ID后，小程序接口即切换为 wx.cloud.callContainer 通道（免备案）；
	 * 留空则维持 uni.request 直连 baseUrl 的原通道（回滚开关=清空 env）。
	 * basePath = 后端 context-path；service = 云托管服务名
	 * mockPay = true 时支付走模拟支付接口（仅MVP演示，见04计划；生产必须置 false）
	 */
	static cloud = {
		env: 'prod-d6gd6a2twe27f592a',
		service: 'bar-mvp',
		basePath: '/siam-server',
		mockPay: true
	};
}
