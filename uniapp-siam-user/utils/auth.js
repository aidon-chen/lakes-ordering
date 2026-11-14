import storage from './storage';
import http from './http';
const TOKEN_KEY = 'security.siam.token';
const OPENID_KEY = 'security.siam.openid';
const PHONE_KEY = 'security.siam.phone';
export default class AuthService {
    static getToken() {
        return storage.getStorage(TOKEN_KEY);
    }
    static setToken(token) {
        return storage.setStorage(TOKEN_KEY, token);
    }
    static deleteToken() {
        return storage.removeStorage(TOKEN_KEY);
    }
    static setOpenId(openId) {
        return storage.setStorage(OPENID_KEY, openId);
    }
    static getOpenId() {
        // [2026-10-02 支付断路修复] 'security.siam.openid' 全仓无任何 setOpenId 写入方 →
        // uni.getStorage 必 fail → 原实现 reject 且 7 个调用点全无 catch →
        // weChatPay/mockPay 永不执行（云库实证：8 笔测试单全部 is_payment=0 且后端从未收到 mockPay）。
        // 语义修正：openId 本就是「可能不存在的缓存」，缺失时 resolve(null) 放行流程——
        // mockPay 不依赖 openId（utils/pay.js 头注），余额支付走支付密码层也不依赖；
        // 将来接真实微信支付时，由支付分支自行判断 null 并补 wx.login 链路。
        return storage.getStorage(OPENID_KEY).catch(() => null);
    }
    static deleteOpenId() {
        return storage.removeStorage(OPENID_KEY);
    }
    static setPhoneNumber(phone) {
        return storage.setStorage(PHONE_KEY, phone);
    }
    static getPhoneNumber() {
        return storage.getStorage(PHONE_KEY);
    }
    static deletePhoneNumber() {
        return storage.removeStorage(PHONE_KEY);
    }
    static setWxStorage(storageText, storageValue) {
        return storage.setStorage('security.' + storageText, storageValue);
    }
    static getWxStorage(storageText) {
        console.log(storageText);
        return storage.getStorage('security.' + storageText);
    }
    static deleteOpenId(storageText) {
        return storage.removeStorage('security.' + storageText);
    }
    static checkIsLogin() {
        return new Promise((fulfil, reject) => {
            this.getToken().then(
                (token) => {
                    if (token) {
                        fulfil(true);
                    } else {
                        fulfil(false);
                    }
                },
                (err) => {
                    fulfil(false);
                }
            );
        });
    }
}
