<template>
	<view class="c17a">
		<!-- 步骤条（画板 419:45） -->
		<view class="c17a-stepbar">
			<view class="c17a-step c17a-step-active">① 实名认证</view>
			<view class="c17a-step-line"></view>
			<view class="c17a-step c17a-step-idle">② 身份认证</view>
		</view>

		<!-- 驳回理由（红）：驳回后重提时置顶展示，用户才知道上次为何没通过 -->
		<view class="c17a-block" v-if="rejectReason">
			<view class="c17a-reject">驳回理由：{{ rejectReason }}</view>
		</view>

		<!-- 姓名卡（画板 419:52） -->
		<view class="c17a-block">
			<view class="c17a-card">
				<text class="c17a-card-title">真实姓名</text>
				<input class="c17a-input" type="text" placeholder="请输入与身份证一致的姓名"
					placeholder-class="c17a-placeholder" :value="realName" @input="onRealNameInput" maxlength="20" />
			</view>
		</view>

		<!-- 单独同意（敏感个人信息）：独立勾选框，**不得**并入《用户协议》《隐私政策》的一揽子同意
		     依据《个人信息保护法》第 28~30 条 + 实施计划 §8 要求 1；放在上传区之前，保证「先同意后收集」 -->
		<view class="c17a-block">
			<view class="c17a-consent">
				<view class="c17a-check-hit" @tap="toggleAgree">
					<view class="c17a-check" :class="agreed ? 'c17a-check-on theme-bg' : 'c17a-check-off'">
						<view class="c17a-tick" v-if="agreed"></view>
					</view>
				</view>
				<view class="c17a-consent-text">
					<text class="c17a-consent-normal" @tap="toggleAgree">我已阅读并同意</text>
					<text class="c17a-consent-link theme-color" @tap.stop="openConsent">《实名认证个人信息处理告知书》</text>
				</view>
			</view>
		</view>

		<!-- 证件卡（画板 419:57） -->
		<view class="c17a-block">
			<view class="c17a-card">
				<text class="c17a-card-title">身份证照片</text>
				<view class="c17a-card-desc">请确保证件四角完整、无遮挡与反光，仅用于人工核验</view>
				<view class="c17a-upload-row">
					<view class="c17a-upload" @tap="chooseImage('front')">
						<image v-if="idCardFrontUrl" class="c17a-upload-img" :src="idCardFrontUrl" mode="aspectFill"></image>
						<block v-else>
							<view class="c17a-camera">
								<view class="c17a-camera-lens"></view>
							</view>
							<text class="c17a-upload-text">点击上传人像面</text>
						</block>
					</view>
					<view class="c17a-upload" @tap="chooseImage('back')">
						<image v-if="idCardBackUrl" class="c17a-upload-img" :src="idCardBackUrl" mode="aspectFill"></image>
						<block v-else>
							<view class="c17a-camera">
								<view class="c17a-camera-lens"></view>
							</view>
							<text class="c17a-upload-text">点击上传国徽面</text>
						</block>
					</view>
				</view>
			</view>
		</view>

		<!-- 提示条（画板 419:74） -->
		<view class="c17a-block">
			<view class="c17a-tip">提交后由人工审核，1-3 个工作日完成；驳回可重新提交</view>
		</view>

		<!-- 底部按钮 -->
		<view class="c17a-footer">
			<view class="c17a-submit theme-bg" @tap="submit">提交认证</view>
		</view>

		<!-- 独立告知弹窗（PIPL 第 30 条：处理目的 + 方式范围 + 必要性/影响 + 保存期限 + 撤回方式）
		     与勾选框配套，二者共同构成「单独同意」，不与其他协议共用同一个勾选动作 -->
		<view class="c17a-mask" v-if="showConsent" @tap="closeConsent" @touchmove.stop.prevent="noop">
			<view class="c17a-dialog" @tap.stop>
				<view class="c17a-dialog-title">实名认证个人信息处理告知书</view>
				<scroll-view class="c17a-dialog-body" scroll-y>
					<view class="c17a-dialog-p">为完成您的真实身份核验，我们需要处理您的敏感个人信息。依据《个人信息保护法》第二十八条至第三十条，我们向您单独告知如下：</view>

					<view class="c17a-dialog-p">
						<text class="c17a-dialog-h">一、处理目的</text>
						核验您本人与所提交身份证件的一致性、证件的真实性，确认您的实名身份。
					</view>

					<view class="c17a-dialog-p">
						<text class="c17a-dialog-h">二、处理方式与范围</text>
						收集您的真实姓名、身份证件正反面照片。上述照片由审核人员<text class="c17a-dialog-strong">人工比对</text>，我们不使用人脸识别算法对其进行自动化处理，也不会将其用于广告、营销、用户画像或任何与身份核验无关的目的。
					</view>

					<view class="c17a-dialog-p">
						<text class="c17a-dialog-h">三、必要性及对您个人权益的影响</text>
						真实身份核验只能通过核对证件实现，故收集证件照片具有充分必要性。若您不同意，我们将无法为您完成实名认证，但这<text class="c17a-dialog-strong">不影响</text>您浏览商品、查看门店与到店消费等基础功能。证件照片一旦泄露可能对您的人身、财产安全造成不利影响，我们已采取传输加密、访问权限最小化、审核留痕与限时删除等措施降低风险。
					</view>

					<view class="c17a-dialog-p">
						<text class="c17a-dialog-h">四、保存期限</text>
						自审核结论作出之日起保存 {{ keepDays }} 日，期满后我们即予删除，认证记录中仅保留审核结论等必要痕迹。保存期间您也可以要求我们提前删除。
					</view>

					<view class="c17a-dialog-p">
						<text class="c17a-dialog-h">五、撤回同意的方式</text>
						您可以随时通过小程序内的「我的 — 联系客服」渠道，或《{{ brandLabel }}隐私政策》第八条所列的联系方式提出撤回。撤回后我们将停止处理并删除相应材料，但不影响撤回前基于该同意已进行的处理活动。
					</view>

					<view class="c17a-dialog-p">
						<text class="c17a-dialog-h">六、您的其他权利</text>
						您有权查阅、复制、更正、删除您的个人信息，并可随时撤回同意。详细内容见《{{ brandLabel }}隐私政策》。
					</view>
				</scroll-view>
				<view class="c17a-dialog-foot">
					<view class="c17a-dialog-btn c17a-dialog-cancel" @tap="closeConsent">取消</view>
					<view class="c17a-dialog-btn c17a-dialog-ok theme-bg" @tap="confirmConsent">同意并继续</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	import https from '../../../utils/http';
	import toastService from '../../../utils/toast.service';
	import GlobalConfig from '../../../utils/global-config';
	// 合规数值单一源（保存期限等），与隐私政策 4.5 条共用，避免两处文案漂移
	import memberConsent from '../../../utils/member-consent';

	export default {
		data() {
			return {
				ossUrl: GlobalConfig.ossUrl,
				realName: '',
				// path = 提交表单入库用；Url = 展示用（私有桶下由后端下发短时效签名 URL，不能自己拼）
				idCardFront: '',
				idCardFrontUrl: '',
				idCardBack: '',
				idCardBackUrl: '',
				rejectReason: '',
				uploading: false,
				// 单独同意状态：默认**未勾选**，必须由用户主动勾选（敏感个人信息不得默认同意）
				agreed: false,
				// 告知书弹窗
				showConsent: false,
				keepDays: memberConsent.IDENTITY_KEEP_DAYS,
				brandLabel: '湖里Lakes'
			};
		},
		onLoad() {
			this.loadDetail();
		},
		methods: {
			// 回显：驳回重提时带出上次填写内容，避免重新输入
			loadDetail() {
				https.request('/rest/member/identityAuth/detail', {}).then((result) => {
					if (result && result.success && result.data && result.data.realNameAuth) {
						const auth = result.data.realNameAuth;
						this.realName = auth.realName || '';
						this.idCardFront = auth.idCardFront || '';
						this.idCardBack = auth.idCardBack || '';
						// 证件图在私有桶：用后端下发的签名 URL（本地联调是相对路径），不再自己拼 ossUrl
						this.idCardFrontUrl = this.absUrl(auth.idCardFrontUrl);
						this.idCardBackUrl = this.absUrl(auth.idCardBackUrl);
						// 仅「已驳回」（audit_status=3）时展示理由
						if (Number(auth.auditStatus) === 3) {
							this.rejectReason = auth.auditReason || '未填写驳回理由，请核对材料后重新提交';
						}
					}
				});
			},

			onRealNameInput(e) {
				this.realName = e.detail.value;
			},

			/* ===== 单独同意：勾选框 + 独立告知弹窗 ===== */
			toggleAgree() {
				this.agreed = !this.agreed;
			},

			openConsent() {
				this.showConsent = true;
			},

			closeConsent() {
				this.showConsent = false;
			},

			// 弹窗内「同意并继续」视为一次明确的意思表示，等价于勾选
			confirmConsent() {
				this.agreed = true;
				this.showConsent = false;
			},

			// 遮罩层阻止背景滚动用（scroll-view 内部仍可滚动）
			noop() {},

			chooseImage(which) {
				if (this.uploading) {
					return;
				}
				// 先同意后收集：未单独同意前不得上传证件（PIPL 第 29 条的「处理」含收集）
				// 此处直接弹出告知书，比单纯 toast 拦截更易理解
				if (!this.agreed) {
					this.showConsent = true;
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

			// 拖到展示层：绝对地址（云端签名 URL）原样用；相对路径（本地联调）拼本机前缀
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
							if (which === 'front') {
								this.idCardFront = path;
								this.idCardFrontUrl = showUrl;
							} else {
								this.idCardBack = path;
								this.idCardBackUrl = showUrl;
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
				// 单独同意是提交的前置条件（双重拦截：chooseImage 已拦一次，此处再兜一次）
				if (!this.agreed) {
					toastService.showToast('请先阅读并同意《实名认证个人信息处理告知书》');
					this.showConsent = true;
					return;
				}
				if (this.uploading) {
					toastService.showToast('图片上传中，请稍候');
					return;
				}
				if (!this.realName || !this.realName.trim()) {
					toastService.showToast('请输入与身份证一致的姓名');
					return;
				}
				if (!this.idCardFront) {
					toastService.showToast('请上传身份证人像面');
					return;
				}
				if (!this.idCardBack) {
					toastService.showToast('请上传身份证国徽面');
					return;
				}
				https.request('/rest/member/identityAuth/submitRealName', {
					realName: this.realName.trim(),
					idCardFront: this.idCardFront,
					idCardBack: this.idCardBack
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

	.c17a {
		min-height: 100vh;
		display: flex;
		flex-direction: column;
		padding-bottom: 40rpx;
	}

	/* ========== 步骤条 ========== */
	.c17a-stepbar {
		display: flex;
		align-items: center;
		padding: 8rpx 24rpx 0 24rpx;
	}

	.c17a-step {
		height: 52rpx;
		line-height: 52rpx;
		padding: 0 24rpx;
		border-radius: 26rpx;
		font-size: 22rpx;
	}

	.c17a-step-active {
		background: #5f7d22;
		color: #ffffff;
		font-weight: 500;
	}

	.c17a-step-idle {
		background: #ffffff;
		color: #b0b0b0;
	}

	.c17a-step-line {
		width: 32rpx;
		height: 3rpx;
		background: #d8ded9;
	}

	/* ========== 卡片 ========== */
	/* 区块间距：画板 C-17a itemSpacing 12pt = 24rpx（步骤区顶部另有 4pt，见 .c17a-stepbar） */
	.c17a-block {
		margin: 24rpx 24rpx 0 24rpx;
	}

	.c17a-card {
		background: #ffffff;
		border-radius: 24rpx;
		padding: 28rpx;
		display: flex;
		flex-direction: column;
	}

	.c17a-card-title {
		font-size: 28rpx;
		font-weight: 500;
		color: #26301b;
	}

	.c17a-card-desc {
		font-size: 22rpx;
		line-height: 32rpx;
		color: #8a917f;
		margin-top: 12rpx;
	}

	/* 输入框：高 88rpx（画板 44pt）、浅底、圆角 8pt */
	.c17a-input {
		height: 88rpx;
		background: #f7f8f7;
		border-radius: 16rpx;
		padding: 0 24rpx;
		font-size: 26rpx;
		color: #26301b;
		margin-top: 20rpx;
	}

	.c17a-placeholder {
		color: #c3c3c3;
		font-size: 26rpx;
	}

	/* ========== 上传位 ========== */
	.c17a-upload-row {
		display: flex;
		margin-top: 20rpx;
	}

	.c17a-upload {
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

	.c17a-upload:first-child {
		margin-right: 20rpx;
	}

	.c17a-upload-img {
		width: 100%;
		height: 208rpx;
	}

	.c17a-upload-text {
		font-size: 22rpx;
		color: #8a917f;
		margin-top: 16rpx;
	}

	/* 相机图标（纯 CSS，避免额外图片资源）——描边色对齐画板 419:62/419:68 的主题绿 */
	.c17a-camera {
		width: 52rpx;
		height: 40rpx;
		border: 3rpx solid #5f7d22;
		border-radius: 8rpx;
		position: relative;
	}

	.c17a-camera-lens {
		position: absolute;
		top: 7rpx;
		left: 13rpx;
		width: 18rpx;
		height: 18rpx;
		border: 3rpx solid #5f7d22;
		border-radius: 50%;
	}

	/* ========== 提示条 ========== */
	.c17a-tip {
		background: #fff7e6;
		border-radius: 16rpx;
		padding: 20rpx;
		font-size: 22rpx;
		line-height: 32rpx;
		color: #8a6d3b;
	}

	/* 驳回理由（红）：与提示条同形，只换色系 */
	.c17a-reject {
		background: #fdecec;
		border-radius: 16rpx;
		padding: 20rpx;
		font-size: 22rpx;
		line-height: 32rpx;
		color: #e25a2b;
		word-break: break-all;
	}

	/* ========== 单独同意勾选（独立于《用户协议》的一揽子勾选） ========== */
	.c17a-consent {
		display: flex;
		align-items: flex-start;
		padding: 0 4rpx;
	}

	/* 加大点击热区但不位移视觉盒（与 components/login-agreement 同一手法） */
	.c17a-check-hit {
		margin: -10rpx 0;
		padding: 10rpx 12rpx 10rpx 0;
	}

	.c17a-check {
		width: 32rpx;
		height: 32rpx;
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		box-sizing: border-box;
	}

	.c17a-check-off {
		background: transparent;
		border: 2rpx solid #c3c3c3;
	}

	.c17a-check-on {
		border: none;
	}

	/* 纯 CSS 对勾（与登录页协议行同规格） */
	.c17a-tick {
		width: 12rpx;
		height: 5rpx;
		border-left: 3rpx solid #ffffff;
		border-bottom: 3rpx solid #ffffff;
		transform: rotate(-45deg);
	}

	.c17a-consent-text {
		flex: 1;
		font-size: 22rpx;
		line-height: 34rpx;
	}

	.c17a-consent-normal {
		color: #8a917f;
	}

	/* 链接色由全局 .theme-color 提供 */

	/* ========== 告知书弹窗 ========== */
	.c17a-mask {
		position: fixed;
		left: 0;
		top: 0;
		right: 0;
		bottom: 0;
		background: rgba(0, 0, 0, 0.45);
		display: flex;
		align-items: center;
		justify-content: center;
		z-index: 999;
	}

	.c17a-dialog {
		width: 620rpx;
		background: #ffffff;
		border-radius: 24rpx;
		display: flex;
		flex-direction: column;
		overflow: hidden;
	}

	.c17a-dialog-title {
		font-size: 30rpx;
		font-weight: 600;
		color: #26301b;
		text-align: center;
		padding: 32rpx 28rpx 20rpx 28rpx;
	}

	/* 固定高度而非 max-height：scroll-view 需确定高度才能滚动，短内容时仅底部略留白 */
	.c17a-dialog-body {
		height: 50vh;
		padding: 0 28rpx;
		box-sizing: border-box;
	}

	.c17a-dialog-p {
		font-size: 24rpx;
		line-height: 38rpx;
		color: #666666;
		margin-bottom: 16rpx;
	}

	.c17a-dialog-h {
		display: block;
		font-weight: 600;
		color: #26301b;
		margin-bottom: 4rpx;
	}

	.c17a-dialog-strong {
		font-weight: 600;
		color: #26301b;
	}

	.c17a-dialog-foot {
		display: flex;
		border-top: 1rpx solid #ededed;
	}

	.c17a-dialog-btn {
		flex: 1;
		height: 88rpx;
		line-height: 88rpx;
		text-align: center;
		font-size: 28rpx;
	}

	.c17a-dialog-cancel {
		color: #8a917f;
	}

	.c17a-dialog-ok {
		font-weight: 500;
	}

	/* ========== 底部按钮 ========== */
	.c17a-footer {
		margin-top: auto;
		padding: 24rpx;
	}

	.c17a-submit {
		height: 88rpx;
		line-height: 88rpx;
		border-radius: 44rpx;
		text-align: center;
		font-size: 28rpx;
		font-weight: 500;
	}
</style>
