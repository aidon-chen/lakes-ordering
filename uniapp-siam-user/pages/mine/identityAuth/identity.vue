<template>
	<view class="c17b">
		<!-- 步骤条（画板 419:83）：① 已完成、② 当前 -->
		<view class="c17b-stepbar">
			<view class="c17b-step c17b-step-done">① 实名认证</view>
			<view class="c17b-step-line"></view>
			<view class="c17b-step c17b-step-active">② 身份认证</view>
		</view>

		<!-- 驳回理由（红）：驳回后重提时置顶展示，用户才知道上次为何没通过 -->
		<view class="c17b-block" v-if="rejectReason">
			<view class="c17b-reject">驳回理由：{{ rejectReason }}</view>
		</view>

		<!-- 类型卡（画板 419:90） -->
		<view class="c17b-block">
			<view class="c17b-card">
				<text class="c17b-card-title">选择身份类型</text>
				<view class="c17b-type-row">
					<view class="c17b-type" :class="identityType === 1 ? 'c17b-type-on' : 'c17b-type-off'"
						@tap="selectType(1)">学生</view>
					<view class="c17b-type" :class="identityType === 2 ? 'c17b-type-on' : 'c17b-type-off'"
						@tap="selectType(2)">教师</view>
				</view>
			</view>
		</view>

		<!-- 证件卡（画板 419:98） -->
		<view class="c17b-block">
			<view class="c17b-card">
				<text class="c17b-card-title">上传证件照片</text>
				<view class="c17b-card-desc">学生：上传学生证信息页与注册章页；教师：上传教师资格证</view>

				<!-- 学生：两图（信息页 + 注册章页） -->
				<view class="c17b-upload-row" v-if="identityType === 1">
					<view class="c17b-upload" @tap="chooseImage('studentFront')">
						<image v-if="studentCardImgUrl" class="c17b-upload-img" :src="studentCardImgUrl"
							mode="aspectFill"></image>
						<block v-else>
							<view class="c17b-camera">
								<view class="c17b-camera-lens"></view>
							</view>
							<text class="c17b-upload-text">学生证信息页</text>
						</block>
					</view>
					<view class="c17b-upload" @tap="chooseImage('studentReg')">
						<image v-if="studentCardRegImgUrl" class="c17b-upload-img" :src="studentCardRegImgUrl"
							mode="aspectFill"></image>
						<block v-else>
							<view class="c17b-camera">
								<view class="c17b-camera-lens"></view>
							</view>
							<text class="c17b-upload-text">注册章页</text>
						</block>
					</view>
				</view>

				<!-- 教师：单图（教师资格证） -->
				<view class="c17b-upload-row" v-else>
					<view class="c17b-upload c17b-upload-full" @tap="chooseImage('teacher')">
						<image v-if="teacherCertImgUrl" class="c17b-upload-img" :src="teacherCertImgUrl"
							mode="aspectFill"></image>
						<block v-else>
							<view class="c17b-camera">
								<view class="c17b-camera-lens"></view>
							</view>
							<text class="c17b-upload-text">教师资格证</text>
						</block>
					</view>
				</view>

				<text class="c17b-upload-note">照片需清晰可辨，姓名与院校信息完整</text>
			</view>
		</view>

		<!-- 提示条（画板 419:116） -->
		<view class="c17b-block">
			<view class="c17b-tip">提交后由人工审核，1-3 个工作日完成；驳回可重新提交</view>
		</view>

		<!-- 底部按钮 -->
		<view class="c17b-footer">
			<view class="c17b-submit theme-bg" @tap="submit">提交认证</view>
		</view>
	</view>
</template>

<script>
	import https from '../../../utils/http';
	import toastService from '../../../utils/toast.service';
	import authService from '../../../utils/auth';
	import GlobalConfig from '../../../utils/global-config';

	export default {
		data() {
			return {
				ossUrl: GlobalConfig.ossUrl,
				identityType: 1,
				// path = 提交表单入库用；Url = 展示用（私有桶下由后端下发短时效签名 URL，不能自己拼）
				studentCardImg: '',
				studentCardImgUrl: '',
				studentCardRegImg: '',
				studentCardRegImgUrl: '',
				teacherCertImg: '',
				teacherCertImgUrl: '',
				rejectReason: '',
				uploading: false
			};
		},
		onLoad() {
			// 登录守卫（09-24 未登录拦截修复）：未登录不进表单页，静默退回。
			// 原实现未登录时 detail 接口静默失败、页面停留可操作——正是拦截不彻底的缺口之一
			authService.checkIsLogin().then((result) => {
				if (!result) {
					uni.navigateBack({
						delta: 1,
						fail: () => {
							uni.reLaunch({
								url: '/pages/mine/index/index'
							});
						}
					});
					return;
				}
				this.loadDetail();
			});
		},
		methods: {
			// 回显：驳回重提时带出上次填写内容
			loadDetail() {
				https.request('/rest/member/identityAuth/detail', {}).then((result) => {
					if (result && result.success && result.data) {
						if (!result.data.stage1Passed) {
							toastService.showToast('请先完成实名认证');
							setTimeout(() => {
								uni.navigateBack(1);
							}, 1200);
							return;
						}
						const auth = result.data.identityAuth;
						if (auth) {
							this.identityType = Number(auth.identityType) || 1;
							this.studentCardImg = auth.studentCardImg || '';
							this.studentCardRegImg = auth.studentCardRegImg || '';
							this.teacherCertImg = auth.teacherCertImg || '';
							// 证件图在私有桶：用后端下发的签名 URL（本地联调是相对路径），不再自己拼 ossUrl
							this.studentCardImgUrl = this.absUrl(auth.studentCardImgUrl);
							this.studentCardRegImgUrl = this.absUrl(auth.studentCardRegImgUrl);
							this.teacherCertImgUrl = this.absUrl(auth.teacherCertImgUrl);
							// 仅「已驳回」（audit_status=3）时展示理由
							if (Number(auth.auditStatus) === 3) {
								this.rejectReason = auth.auditReason || '未填写驳回理由，请核对材料后重新提交';
							}
						}
					}
				});
			},

			selectType(type) {
				if (this.uploading) {
					return;
				}
				this.identityType = type;
			},

			chooseImage(which) {
				if (this.uploading) {
					return;
				}
				uni.showActionSheet({
					itemList: ['拍照', '从手机相册选择'],
					success: (res) => {
						uni.chooseImage({
							count: 1,
							sizeType: ['original', 'compressed'],
							sourceType: [res.tapIndex === 0 ? 'camera' : 'album'],
							success: (result) => {
								this.upload(result.tempFilePaths[0], which);
							}
						});
					}
				});
			},

			// 展示层：绝对地址（云端签名 URL）原样用；相对路径（本地联调）拼本机前缀
			absUrl(u) {
				if (!u) {
					return '';
				}
				return /^https?:\/\//i.test(u) ? u : (this.ossUrl + u);
			},

			upload(filePath, which) {
				this.uploading = true;
				toastService.showLoading('正在上传...', true);
				// 证件类走私有桶专用接口（返回 {path,url}）；不能用 /uploadSingleImage（默认桶是公共读）
				const url = GlobalConfig.baseUrl + '/rest/member/uploadIdentityImage';
				https.uploadImage({
					url: url,
					filePath: filePath,
					success: (data) => {
						toastService.hideLoading();
						this.uploading = false;
						if (data && data.success && data.data) {
							const path = data.data.path || '';
							const showUrl = this.absUrl(data.data.url);
							if (which === 'studentFront') {
								this.studentCardImg = path;
								this.studentCardImgUrl = showUrl;
							} else if (which === 'studentReg') {
								this.studentCardRegImg = path;
								this.studentCardRegImgUrl = showUrl;
							} else {
								this.teacherCertImg = path;
								this.teacherCertImgUrl = showUrl;
							}
							toastService.showToast('上传成功');
						} else {
							toastService.showToast((data && data.message) || '上传失败');
						}
					},
					fail: () => {
						toastService.hideLoading();
						this.uploading = false;
						toastService.showToast('上传失败，请重试');
					}
				});
			},

			submit() {
				// 登录守卫（双保险，09-24 修复）：正常流已被 onLoad 拦截，此处兜「停留期间 token 失效」
				authService.checkIsLogin().then((result) => {
					if (!result) {
						getApp().globalData.checkIsAuth('scope.userInfo');
						return;
					}
					this.doSubmit();
				});
			},
			doSubmit() {
				if (this.uploading) {
					toastService.showToast('图片上传中，请稍候');
					return;
				}
				if (this.identityType === 1) {
					if (!this.studentCardImg) {
						toastService.showToast('请上传学生证信息页');
						return;
					}
					if (!this.studentCardRegImg) {
						toastService.showToast('请上传学生证注册章页');
						return;
					}
				} else {
					if (!this.teacherCertImg) {
						toastService.showToast('请上传教师资格证');
						return;
					}
				}
				https.request('/rest/member/identityAuth/submitIdentity', {
					identityType: this.identityType,
					studentCardImg: this.studentCardImg,
					studentCardRegImg: this.studentCardRegImg,
					teacherCertImg: this.teacherCertImg
				}).then((result) => {
					if (result && result.success) {
						toastService.showToast('已提交，等待人工审核');
						setTimeout(() => {
							uni.navigateBack(1);
						}, 1200);
					} else {
						toastService.showToast((result && result.message) || '提交失败');
					}
				});
			}
		}
	};
</script>

<style>
	page {
		background: #f4f5f0;
	}

	.c17b {
		min-height: 100vh;
		display: flex;
		flex-direction: column;
		padding-bottom: 40rpx;
	}

	/* ========== 步骤条：① 已完成 = 浅绿底深绿字；② 当前 = 绿底白字 ========== */
	.c17b-stepbar {
		display: flex;
		align-items: center;
		padding: 8rpx 24rpx 0 24rpx;
	}

	.c17b-step {
		height: 52rpx;
		line-height: 52rpx;
		padding: 0 24rpx;
		border-radius: 26rpx;
		font-size: 22rpx;
	}

	.c17b-step-active {
		background: #5f7d22;
		color: #ffffff;
		font-weight: 500;
	}

	.c17b-step-done {
		background: #e8f0dc;
		color: #5f7d22;
	}

	.c17b-step-line {
		width: 32rpx;
		height: 3rpx;
		background: #d8ded9;
	}

	/* ========== 卡片 ========== */
	/* 区块间距：画板 C-17b itemSpacing 12pt = 24rpx（步骤区顶部另有 4pt，见 .c17b-stepbar） */
	.c17b-block {
		margin: 24rpx 24rpx 0 24rpx;
	}

	.c17b-card {
		background: #ffffff;
		border-radius: 24rpx;
		padding: 28rpx;
		display: flex;
		flex-direction: column;
	}

	.c17b-card-title {
		font-size: 28rpx;
		font-weight: 500;
		color: #26301b;
	}

	.c17b-card-desc {
		font-size: 22rpx;
		line-height: 32rpx;
		color: #8a917f;
		margin-top: 12rpx;
	}

	/* ========== 身份类型 ========== */
	.c17b-type-row {
		display: flex;
		margin-top: 20rpx;
	}

	.c17b-type {
		flex: 1;
		height: 80rpx;
		line-height: 80rpx;
		text-align: center;
		border-radius: 20rpx;
		font-size: 28rpx;
	}

	.c17b-type + .c17b-type {
		margin-left: 20rpx;
	}

	.c17b-type-on {
		background: #5f7d22;
		color: #ffffff;
		font-weight: 500;
	}

	.c17b-type-off {
		background: #ffffff;
		border: 1rpx solid #dfe5e1;
		color: #5a614e;
	}

	/* ========== 上传位 ========== */
	.c17b-upload-row {
		display: flex;
		margin-top: 20rpx;
	}

	.c17b-upload {
		flex: 1;
		height: 208rpx;
		background: #f7f8f7;
		border: 1rpx solid #d8ded9;
		border-radius: 20rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		overflow: hidden;
	}

	.c17b-upload:first-child:not(.c17b-upload-full) {
		margin-right: 20rpx;
	}

	.c17b-upload-img {
		width: 100%;
		height: 208rpx;
	}

	.c17b-upload-text {
		font-size: 22rpx;
		color: #8a917f;
		margin-top: 16rpx;
	}

	.c17b-upload-note {
		font-size: 22rpx;
		color: #b0b0b0;
		margin-top: 16rpx;
	}

	/* 相机图标（纯 CSS）——描边色对齐画板 419:103/419:109 的主题绿 */
	.c17b-camera {
		width: 52rpx;
		height: 40rpx;
		border: 3rpx solid #5f7d22;
		border-radius: 8rpx;
		position: relative;
	}

	.c17b-camera-lens {
		position: absolute;
		top: 7rpx;
		left: 13rpx;
		width: 18rpx;
		height: 18rpx;
		border: 3rpx solid #5f7d22;
		border-radius: 50%;
	}

	/* ========== 提示条 ========== */
	.c17b-tip {
		background: #fff7e6;
		border-radius: 16rpx;
		padding: 20rpx;
		font-size: 22rpx;
		line-height: 32rpx;
		color: #8a6d3b;
	}

	/* 驳回理由（红）：与提示条同形，只换色系 */
	.c17b-reject {
		background: #fdecec;
		border-radius: 16rpx;
		padding: 20rpx;
		font-size: 22rpx;
		line-height: 32rpx;
		color: #e25a2b;
		word-break: break-all;
	}

	/* ========== 底部按钮 ========== */
	.c17b-footer {
		margin-top: auto;
		padding: 24rpx;
	}

	.c17b-submit {
		height: 88rpx;
		line-height: 88rpx;
		border-radius: 44rpx;
		text-align: center;
		font-size: 28rpx;
		font-weight: 500;
	}
</style>
