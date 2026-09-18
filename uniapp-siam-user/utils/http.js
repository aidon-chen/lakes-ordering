import GlobalConfig from './global-config';
import authService from './auth';
const initHeaders = function (headers) {
    return new Promise((fulfill, reject) => {
        let headersObj = {};
        headersObj['lang'] = 'zh-CN';
        // headersObj["content-type"] = "application/x-www-form-urlencoded";
        headersObj['content-type'] = 'application/json';
        if (headers) {
            for (var propName in headers) {
                headersObj[propName] = headers[propName];
            }
        }
        authService.getToken().then(
            (token) => {
                if (token) {
                    headersObj['Authorization'] = token;
                }
                fulfill(headersObj);
            },
            () => {
                fulfill(headersObj);
            }
        );
    });
};
const handleError = function (err) {
    let body = {};
    try {
        if (err && err.success) {
            body = err;
        } else {
            body = err;
        }
    } catch (cerr) {
        console.log('CatchClause', cerr);
        console.log('CatchClause', cerr);
        body = {
            errorCode: err.code,
            errorMsg: err.message
        };
    }
    err = body;
    const errorCode = err && err ? err.code + '' : 0;
    switch (errorCode) {
        case '404':
            uni.showToast({
                title: '错误代码(404): 接口不存在或者已经不可用。',
                icon: 'none',
                duration: 3000
            });
            break;
        case '401':
            authService.deleteToken();
            uni.showToast({
                title: '用户已过期或者未登录，请登录后再使用！',
                icon: 'none',
                duration: 3000
            });
            break;
        case '403':
            uni.showToast({
                title: '您没有权限访问该功能！',
                icon: 'none',
                duration: 3000
            });
            break;
        case '500':
            uni.showToast({
                title: '内部服务器错误！',
                icon: 'none',
                duration: 3000
            });
            break;
        case '400':
            uni.showToast({
                title: '错误的请求！',
                icon: 'none',
                duration: 3000
            });
            break;
        case '502':
            uni.showToast({
                title: '服务器开小差了，稍后再试吧！',
                icon: 'none',
                duration: 3000
            });
            break;
        default:
            // message 兜底为字符串，避免 showToast title=undefined 报错
            uni.showToast({
                title: (err && err.message) || '网络异常，请稍后重试',
                icon: 'none',
                duration: 3000
            });
            break;
    }
};
export default class WXHttp {
    static request(url, postData, headers = null) {
        //fulfill代表异步操作成功,reject:代表异步操作失败
        return new Promise((fulfill, reject) => {
            initHeaders(headers).then((headersObj) => {
                var errObj = {};
                //统一响应处理：callContainer 与 uni.request 的 success/fail 结构一致（res.data=resBody）
                const onSuccess = (res) => {
                    const body = res.data;
                    try {
                        if (body && !body.success) {
                            errObj.code = res.statusCode ? res.statusCode : 101;
                            errObj.message = res.data.message;
                            if (res.data.code == 2) {
                                authService.deleteToken();
                                authService.deleteOpenId();
                                authService.deletePhoneNumber();
                            }
                            if (res.data.code != 2) {
                                handleError(errObj);
                                reject(errObj);
                                fulfill(body);
                            }
                        }
                        fulfill(body); //返回数据，在调用函数后面加.then
                    } catch (ex) {
                        console.log('CatchClause', ex);
                        console.log('CatchClause', ex);
                        errObj.code = res.statusCode ? res.statusCode : 101;
                        errObj.message = res.errMsg;
                        handleError(errObj);
                        reject(errObj);
                    } finally {
                        fulfill(body); //返回数据，在调用函数后面加.then
                    }
                };
                const onFail = (err) => {
                    console.log(err);
                    errObj.code = err.statusCode ? err.statusCode : 101;
                    // errMsg 形如 "request:fail xxx"；无 statusCode 即网络层失败，给可读提示
                    const rawMsg = err.errMsg || '';
                    const detail = rawMsg.split(' ')[1] || '';
                    errObj.message = err.statusCode ? (detail || rawMsg) : '网络异常，请稍后重试';
                    handleError(errObj);
                    reject(errObj);
                };
                //云托管通道：微信小程序且已配置云环境时走 wx.cloud.callContainer（免备案）
                //见 04-deploy-cost/mvp-migration/05；回滚开关=global-config.js 清空 cloud.env
                // #ifdef MP-WEIXIN
                if (GlobalConfig.cloud && GlobalConfig.cloud.env) {
                    wx.cloud.callContainer({
                        config: { env: GlobalConfig.cloud.env },
                        path: GlobalConfig.cloud.basePath + url,
                        data: JSON.stringify(postData),
                        method: 'POST',
                        dataType: 'json',
                        header: Object.assign({}, headersObj, { 'X-WX-SERVICE': GlobalConfig.cloud.service }),
                        success: onSuccess,
                        fail: onFail
                    });
                    return;
                }
                // #endif
                uni.request({
                    url: GlobalConfig.baseUrl + url,
                    data: JSON.stringify(postData),
                    method: 'POST',
                    dataType: 'json',
                    header: headersObj,
                    success: onSuccess,
                    fail: onFail
                });
            });
        });
    }

    /**
     * 单个本地资源上传
     * options
     * sourceType:'camera':照相机，'album':从相册
     * url:服务器资源地址
     * formData:额外formData
     * headers 中不能设置 Referer
     * success:上传成功
     * fail:上传失败
     * complete:上传成功、失败都会执行
     */
    static uploadFile(options) {
        return new Promise((fulfill, reject) => {
            //云托管通道下 callContainer 不支持 multipart 上传（评估§5.3 A级：MVP演示不做上传）
            // #ifdef MP-WEIXIN
            if (GlobalConfig.cloud && GlobalConfig.cloud.env) {
                uni.showToast({ title: '演示版暂不支持上传图片', icon: 'none', duration: 2500 });
                reject({ code: 601, message: '演示版暂不支持上传' });
                return;
            }
            // #endif
            initHeaders(options.headers).then((headersObj) => {
                uni.showActionSheet({
                    //获取底部菜单
                    itemList: ['拍照', '从手机相册选择'],
                    //底部菜单选项
                    success: (res) => {
                        uni.chooseImage({
                            count: 1,
                            sizeType: ['original', 'compressed'],
                            // 可以指定是原图还是压缩图，默认二者都有
                            sourceType: [res.tapIndex == 0 ? 'camera' : 'album'],
                            success: (res) => {
                                var errObj = {};
                                let filePath = res.tempFilePaths[0]; //获取选中的图片或者拍照的图片
                                let url = GlobalConfig.baseUrl + options.url; //上传接口路径
                                uni.uploadFile({
                                    //上传方法
                                    url: url,
                                    filePath: filePath,
                                    name: 'file',
                                    header: headersObj,
                                    formData: options.formData,
                                    success: (res) => {
                                        try {
                                            var result = JSON.parse(res.data);
                                            if (result && !result.success) {
                                                errObj.status = res.statusCode ? res.statusCode : 101;
                                                errObj.message = res.errMsg;
                                                handleError(errObj);
                                                reject(errObj);
                                                return;
                                            }
                                            fulfill(result); //返回数据，在调用函数后面加.then
                                        } catch (ex) {
                                            console.log('CatchClause', ex);
                                            console.log('CatchClause', ex);
                                            errObj.status = res.statusCode ? res.statusCode : 101;
                                            errObj.message = res.errMsg;
                                            handleError(errObj);
                                            reject(errObj);
                                        }
                                    },
                                    fail: (error) => {
                                        //console.log(error)
                                        errObj.status = error.statusCode ? error.statusCode : 101;
                                        errObj.message = error.errMsg.split(' ')[1];
                                        handleError(errObj);
                                        reject(errObj);
                                    }
                                });
                            }
                        });
                    }
                });
            });
        });
    }
    static uploadImage(options) {
        return new Promise((fulfill, reject) => {
            initHeaders(options.headers).then((headersObj) => {
                let url = options.url;
                const uploadTask = uni.uploadFile({
                    url: url,
                    filePath: options.filePath,
                    name: 'file',
                    header: headersObj,
                    formData: options.formData,
                    success: (res) => {
                        var result = JSON.parse(res.data);
                        if (options.success) {
                            options.success(result);
                        }
                    },
                    fail: (error) => {
                        if (options.fail) {
                            options.fail(error);
                        }
                    },
                    complete: () => {
                        if (options.complete) {
                            options.complete();
                        }
                    }
                });
                fulfill(uploadTask);
            });
        });
    }
}
