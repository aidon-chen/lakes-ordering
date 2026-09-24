<template>
	<view class="c17">
		<!-- 状态卡（画板 419:5）：绿底 + 盾牌 + 阶段进度 -->
		<view class="c17-status-card">
			<view class="c17-status-row">
				<view class="c17-shield">
					<view class="c17-shield-tick"></view>
				</view>
				<view class="c17-status-col">
					<view class="c17-status-title">{{ statusTitle }}</view>
					<view class="c17-status-desc">{{ statusDesc }}</view>
				</view>
			</view>

			<view class="c17-progress">
				<view class="c17-step" :class="stage1Passed ? 'c17-step-done' : 'c17-step-active'">① 实名认证</view>
				<view class="c17-step-line"></view>
				<view class="c17-step" :class="stage2Passed ? 'c17-step-done' : 'c17-step-idle'">② 身份认证</view>
			</view>
		</view>

		<!-- 阶段一卡（画板 419:20） -->
		<view class="c17-block">
			<view class="c17-card">
				<view class="c17-card-head">
					<text class="c17-card-title">① 实名认证</text>
					<text class="c17-tag" :class="stage1TagClass">{{ stage1TagText }}</text>
				</view>
				<view class="c17-card-desc">提交姓名与身份证正反面照片，人工审核 1-3 个工作日</view>
				<view class="c17-card-desc">所需材料：姓名 · 身份证人像面 · 身份证国徽面</view>
				<!-- 驳回理由（红）：计划文档 §5 状态码 3 的展示口径 -->
				<view class="c17-reject-reason" v-if="stage1RejectReason">驳回理由：{{ stage1RejectReason }}</view>
				<view class="c17-btn" :class="stage1BtnClass" @tap="goRealName">{{ stage1BtnText }}</view>
			</view>
		</view>

		<!-- 阶段二卡（画板 419:30） -->
		<view class="c17-block">
			<view class="c17-card">
				<view class="c17-card-head">
					<text class="c17-card-title">② 身份认证</text>
					<text class="c17-tag" :class="stage2TagClass">{{ stage2TagText }}</text>
				</view>
				<view class="c17-card-desc">选择学生或教师身份，上传对应证件照片</view>
				<view class="c17-card-desc">所需材料：学生证 或 教师资格证</view>
				<!-- 驳回理由（红）：计划文档 §5 状态码 3 的展示口径 -->
				<view class="c17-reject-reason" v-if="stage2RejectReason">驳回理由：{{ stage2RejectReason }}</view>
				<view class="c17-btn" :class="stage2BtnClass" @tap="goIdentity">{{ stage2BtnText }}</view>
			</view>
		</view>

		<!-- 隐私说明（画板 419:39） -->
		<view class="c17-privacy">资料仅用于身份核验，全部人工审核，不予外传</view>

		<!-- 权益卡（画板 419:121） -->
		<view class="c17-block">
			<view class="c17-card">
				<text class="c17-card-title">认证后解锁</text>
				<view class="c17-benefit" v-for="(item, index) in benefits" :key="index">
					<view class="c17-tick">
						<view class="c17-tick-mark"></view>
					</view>
					<text class="c17-benefit-text">{{ item }}</text>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	import https from '../../../utils/http';
	import authService from '../../../utils/auth';

	// 认证状态码：0=未提交 1=待审核 2=通过 3=驳回
	const STATUS_NONE = 0;
	const STATUS_WAIT = 1;
	const STATUS_PASS = 2;
	const STATUS_REJECT = 3;

	export default {
		data() {
			return {
				stage1Passed: false,
				stage2Passed: false,
				stage1Status: STATUS_NONE,
				stage2Status: STATUS_NONE,
				// 驳回理由（后端 auditReason，仅在 audit_status=3 时有值）
				stage1Reason: '',
				stage2Reason: '',
				benefits: [
					'学生/教师专属折扣与会员日活动',
					'生日礼遇与优先取酒通道',
					'成长值加速累积（1.5 倍）'
				]
			};
		},
		computed: {
			statusTitle() {
				if (this.stage1Passed && this.stage2Passed) {
					return '已认证';
				}
				if (this.stage1Status === STATUS_REJECT || this.stage2Status === STATUS_REJECT) {
					return '认证未通过';
				}
				if (this.stage1Status === STATUS_WAIT || this.stage2Status === STATUS_WAIT) {
					return '审核中';
				}
				if (this.stage1Passed) {
					return '待完善身份';
				}
				return '未认证';
			},
			statusDesc() {
				if (this.stage1Passed && this.stage2Passed) {
					return '已完成两阶段认证，会员专属权益已解锁';
				}
				if (this.stage1Status === STATUS_WAIT) {
					return '实名认证审核中，请耐心等待';
				}
				if (this.stage2Status === STATUS_WAIT) {
					return '身份认证审核中，请耐心等待';
				}
				if (this.stage2Status === STATUS_REJECT) {
					return '身份认证未通过，请按驳回理由修改后重新提交';
				}
				if (this.stage1Status === STATUS_REJECT) {
					return '实名认证未通过，请按驳回理由修改后重新提交';
				}
				return '完成两阶段认证，解锁会员专属权益';
			},
			// 驳回理由：仅「已驳回」时返回文案；后端异常导致理由为空时给兜底提示，避免只看到「已驳回」不知道原因
			stage1RejectReason() {
				if (this.stage1Status !== STATUS_REJECT) {
					return '';
				}
				return this.stage1Reason || '未填写驳回理由，请修改后重新提交';
			},
			stage2RejectReason() {
				if (this.stage2Status !== STATUS_REJECT) {
					return '';
				}
				return this.stage2Reason || '未填写驳回理由，请修改后重新提交';
			},
			stage1TagText() {
				return this.tagText(this.stage1Status);
			},
			stage1TagClass() {
				return this.tagClass(this.stage1Status);
			},
			stage2TagText() {
				if (!this.stage1Passed && this.stage2Status === STATUS_NONE) {
					return '需先完成实名';
				}
				return this.tagText(this.stage2Status);
			},
			stage2TagClass() {
				if (!this.stage1Passed && this.stage2Status === STATUS_NONE) {
					return 'c17-tag-idle';
				}
				return this.tagClass(this.stage2Status);
			},
			// 阶段一按钮：未提交/驳回 → 可提交；待审核 → 禁用；已通过 → 不可再提交
			stage1BtnText() {
				if (this.stage1Status === STATUS_PASS) {
					return '已完成实名认证';
				}
				if (this.stage1Status === STATUS_WAIT) {
					return '审核中';
				}
				return this.stage1Status === STATUS_REJECT ? '重新提交实名认证' : '去实名认证';
			},
			stage1BtnClass() {
				return this.stage1Status === STATUS_WAIT || this.stage1Status === STATUS_PASS ?
					'c17-btn-disabled' : 'theme-bg';
			},
			// 阶段二按钮：阶段一未通过一律置灰且写明原因
			stage2BtnText() {
				if (!this.stage1Passed) {
					return '去身份认证';
				}
				if (this.stage2Status === STATUS_PASS) {
					return '已完成身份认证';
				}
				if (this.stage2Status === STATUS_WAIT) {
					return '审核中';
				}
				return this.stage2Status === STATUS_REJECT ? '重新提交身份认证' : '去身份认证';
			},
			stage2BtnClass() {
				return (this.stage1Passed && (this.stage2Status === STATUS_NONE || this.stage2Status ===
					STATUS_REJECT)) ? 'theme-bg' : 'c17-btn-disabled';
			}
		},
		onShow() {
			authService.checkIsLogin().then((result) => {
				if (result) {
					this.loadDetail();
					return;
				}
				getApp().globalData.checkIsAuth('scope.userInfo');
			});
		},
		methods: {
			loadDetail() {
				https.request('/rest/member/identityAuth/detail', {}).then((result) => {
					if (result && result.success && result.data) {
						const data = result.data;
						this.stage1Passed = !!data.stage1Passed;
						this.stage2Passed = Number(data.stage2Status) === STATUS_PASS;
						this.stage1Status = Number(data.stage1Status) || STATUS_NONE;
						this.stage2Status = Number(data.stage2Status) || STATUS_NONE;
						// 驳回理由：从两阶段各自的记录里取（后端已返回 auditReason，之前前端没读）
						this.stage1Reason = (data.realNameAuth && data.realNameAuth.auditReason) || '';
						this.stage2Reason = (data.identityAuth && data.identityAuth.auditReason) || '';
					}
				});
			},
			tagText(status) {
				if (status === STATUS_PASS) {
					return '已认证';
				}
				if (status === STATUS_WAIT) {
					return '审核中';
				}
				if (status === STATUS_REJECT) {
					return '已驳回';
				}
				return '未完成';
			},
			tagClass(status) {
				if (status === STATUS_PASS) {
					return 'c17-tag-ok';
				}
				if (status === STATUS_REJECT) {
					return 'c17-tag-reject';
				}
				if (status === STATUS_WAIT) {
					return 'c17-tag-wait';
				}
				return 'c17-tag-warn';
			},
			goRealName() {
				if (this.stage1Status === STATUS_WAIT || this.stage1Status === STATUS_PASS) {
					return;
				}
				uni.navigateTo({
					url: '/pages/mine/identityAuth/realname'
				});
			},
			goIdentity() {
				if (!this.stage1Passed) {
					uni.showToast({
						title: '需先完成实名认证',
						icon: 'none'
					});
					return;
				}
				if (this.stage2Status === STATUS_WAIT || this.stage2Status === STATUS_PASS) {
					return;
				}
				uni.navigateTo({
					url: '/pages/mine/identityAuth/identity'
				});
			}
		}
	};
</script>

<style>
	page {
		background: #f4f5f0;
	}

	.c17 {
		min-height: 100vh;
		padding-bottom: 40rpx;
	}

	/* ========== 状态卡（绿底，对齐画板 419:5 的 20pt 内边距） ========== */
	.c17-status-card {
		background: #5f7d22;
		padding: 40rpx 40rpx 44rpx 40rpx;
		display: flex;
		flex-direction: column;
	}

	.c17-status-row {
		display: flex;
		align-items: center;
	}

	.c17-shield {
		width: 68rpx;
		height: 68rpx;
		border-radius: 50%;
		background: rgba(255, 255, 255, 0.22);
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
	}

	.c17-shield-tick {
		width: 18rpx;
		height: 32rpx;
		border-right: 4rpx solid #ffffff;
		border-bottom: 4rpx solid #ffffff;
		transform: rotate(45deg);
		margin-top: -8rpx;
	}

	.c17-status-col {
		margin-left: 24rpx;
		display: flex;
		flex-direction: column;
	}

	.c17-status-title {
		font-size: 38rpx;
		font-weight: bold;
		color: #ffffff;
	}

	.c17-status-desc {
		font-size: 24rpx;
		color: #ffffff;
		opacity: 0.92;
		margin-top: 8rpx;
	}

	.c17-progress {
		display: flex;
		align-items: center;
		margin-top: 24rpx;
	}

	.c17-step {
		height: 52rpx;
		line-height: 52rpx;
		padding: 0 24rpx;
		border-radius: 26rpx;
		font-size: 22rpx;
	}

	/* 当前待办：白底 + 主题深绿字 */
	.c17-step-active {
		background: #ffffff;
		color: #3f5416;
		font-weight: 500;
	}

	/* 已完成：半透明白底 + 白字 */
	.c17-step-done {
		background: rgba(255, 255, 255, 0.26);
		color: #ffffff;
	}

	/* 未解锁：半透明白底 + 白字（弱化） */
	.c17-step-idle {
		background: rgba(255, 255, 255, 0.16);
		color: #ffffff;
		opacity: 0.85;
	}

	.c17-step-line {
		width: 32rpx;
		height: 3rpx;
		background: rgba(255, 255, 255, 0.6);
	}

	/* ========== 卡片通用 ========== */
	.c17-block {
		margin: 20rpx 24rpx 0 24rpx;
	}

	.c17-card {
		background: #ffffff;
		border-radius: 24rpx;
		padding: 28rpx;
		display: flex;
		flex-direction: column;
	}

	.c17-card-head {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.c17-card-title {
		font-size: 28rpx;
		font-weight: 500;
		color: #26301b;
	}

	.c17-card-desc {
		font-size: 22rpx;
		line-height: 32rpx;
		color: #8a917f;
		margin-top: 12rpx;
	}

	/* 驳回理由（红）：与 .c17-tag-reject 同色系，一眼可见 */
	.c17-reject-reason {
		margin-top: 16rpx;
		padding: 16rpx 20rpx;
		background: #fdecec;
		border-radius: 12rpx;
		font-size: 22rpx;
		line-height: 32rpx;
		color: #e25a2b;
		word-break: break-all;
	}

	/* 状态标签 */
	.c17-tag {
		font-size: 20rpx;
		height: 40rpx;
		line-height: 40rpx;
		padding: 0 12rpx;
		border-radius: 8rpx;
	}

	.c17-tag-warn {
		background: #fff7e6;
		color: #e6a23c;
	}

	.c17-tag-wait {
		background: #fff7e6;
		color: #e6a23c;
	}

	.c17-tag-ok {
		background: #e8f5ee;
		color: #43a047;
	}

	.c17-tag-reject {
		background: #fdecec;
		color: #e25a2b;
	}

	.c17-tag-idle {
		background: #f0f0f0;
		color: #b0b0b0;
	}

	/* 按钮：高 72rpx / 圆角 36rpx（画板 36pt / radius 18pt） */
	.c17-btn {
		height: 72rpx;
		line-height: 72rpx;
		border-radius: 36rpx;
		text-align: center;
		font-size: 26rpx;
		font-weight: 500;
		color: #ffffff;
		margin-top: 24rpx;
	}

	.c17-btn-disabled {
		background: #f0f0f0;
		color: #b0b0b0;
	}

	/* 隐私说明 */
	.c17-privacy {
		font-size: 20rpx;
		color: #8a917f;
		text-align: center;
		margin-top: 24rpx;
	}

	/* 权益 */
	.c17-benefit {
		display: flex;
		align-items: center;
		margin-top: 20rpx;
	}

	.c17-tick {
		width: 32rpx;
		height: 32rpx;
		border-radius: 50%;
		border: 2rpx solid #5f7d22;
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
	}

	.c17-tick-mark {
		width: 8rpx;
		height: 14rpx;
		border-right: 2rpx solid #5f7d22;
		border-bottom: 2rpx solid #5f7d22;
		transform: rotate(45deg);
		margin-top: -4rpx;
	}

	.c17-benefit-text {
		font-size: 24rpx;
		color: #5a614e;
		margin-left: 16rpx;
	}
</style>
