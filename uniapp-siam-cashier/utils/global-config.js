export default class GlabalConfig {
	// #ifdef H5
	//生产：云托管 bar-mvp 公网域名（2026-09-18 部署接入）；静态托管无 proxy，必须用完整域名
	static baseUrl = 'https://bar-mvp-315579-12-1488393615.sh.run.tcloudbase.com/siam-server';
	// #endif
	// #ifdef APP-PLUS||MP-WEIXIN||MP-ALIPAY
	static baseUrl = 'https://api.show.siamit.cn/siam-server';
	// #endif
    
    // static baseUrl = 'http://localhost:9020';
    static ossUrl = 'https://siam-hangzhou.oss-cn-hangzhou.aliyuncs.com/';
}
