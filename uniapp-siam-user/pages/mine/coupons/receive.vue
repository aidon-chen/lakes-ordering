<template>
	<view class="rcc">
		<!-- 领券中心（画板 C-18 455:2）：券池列表 + 领取三态。
		     数据源：券池=POST /rest/member/coupons/list（issueType=2 自行领取）；
		     已领集合=/rest/member/couponsMemberRelation/list 的 couponsId 去重。
		     后端 insert 已按券+会员查重并从 session 取 memberId（2026-09-24 B1 加固），
		     前端 memberId 仅兜底传参；「已领取即灰」仍是第一道防连点。
		     ⚠️ 本文件属正本 uniapp-siam-user：改动请落正本再 sync，直接改 customer-cli/src 会被 sync 覆盖（09-24 踩坑） -->
		<view class="rcc-tip" v-if="isLogin">
			以下优惠券由门店发放，领取后可在「我的券包」查看使用
		</view>
		<!-- 未登录：停留本页 + 引导登录（修复范式，禁止静默跳回） -->
		<view class="rcc-login-card" v-else>
			<view class="rcc-login-text">登录后即可领取优惠券</view>
			<view class="rcc-login-btn" @tap="goLogin">去登录</view>
		</view>

		<view class="rcc-list" v-if="couponsList.length > 0">
			<view class="rcc-card" v-for="(item, index) in couponsList" :key="item.id">
				<view class="rcc-face">
					<view class="rcc-face-amount">{{ amountText(item) }}</view>
					<view class="rcc-face-cond">{{ condText(item) }}</view>
				</view>
				<view class="rcc-info">
					<view class="rcc-name">{{ item.name }}</view>
					<view class="rcc-valid">{{ validText(item) }}</view>
					<view class="rcc-rule" v-if="item.description" @tap="toggleRule(item)">
						使用规则
						<text v-if="item.ruleShow"> ▴</text>
						<text v-else> ▾</text>
					</view>
				</view>
				<!-- 已领取：灰按钮 + 去使用（对应画板 C-18 第三态） -->
				<block v-if="isReceived(item.id)">
					<view class="rcc-btn rcc-btn-received">已领取</view>
					<view class="rcc-use" @tap="goUse">去使用 ›</view>
				</block>
				<!-- 可领：主题绿按钮（防连点 submitting 锁） -->
				<view class="rcc-btn" v-else-if="!submitting" @tap="onReceive(item)">立即领取</view>
				<view class="rcc-btn rcc-btn-received" v-else>领取中…</view>
			</view>
			<!-- 使用规则展开区（折叠在对应卡片下方） -->
			<view class="rcc-rule-panel" v-for="item in couponsList" :key="'rule-' + item.id"
				v-if="item.ruleShow && item.description">{{ item.description }}</view>
		</view>
		<!-- 空态/错误态：自绘（van-empty 未在 usingComponents 注册会渲染为空白节点，券包页同款隐患，这里不依赖组件库） -->
		<view class="rcc-empty" v-else-if="isLogin && !loading && !loadError">
			<view class="rcc-empty-icon">✂</view>
			<view class="rcc-empty-text">暂无可领取的优惠券</view>
			<view class="rcc-empty-sub">门店发放后此处即可领取</view>
		</view>
		<view class="rcc-empty" v-else-if="isLogin && loadError">
			<view class="rcc-empty-icon">⚠</view>
			<view class="rcc-empty-text">加载失败：{{ loadErrorMsg }}</view>
			<view class="rcc-empty-sub">开发者工具请勾选「不校验合法域名」后重进；下拉可重试</view>
			<view class="rcc-login-btn" @tap="reload">重试</view>
		</view>

		<!-- 领取成功弹窗（画板 C-18a 455:46）：遮罩 + 对勾 + 券票 + 双操作 -->
		<view class="rcc-mask" v-if="showSuccess" @tap="closeSuccess">
			<view class="rcc-dialog" @tap.stop>
				<view class="rcc-dialog-check">✓</view>
				<view class="rcc-dialog-title">领取成功</view>
				<view class="rcc-dialog-ticket" v-if="successCoupon">
					<view class="rcc-dialog-amount">{{ amountText(successCoupon) }}</view>
					<view class="rcc-dialog-info">
						<view class="rcc-dialog-name">{{ successCoupon.name }}</view>
						<view class="rcc-dialog-valid">{{ validText(successCoupon) }}</view>
					</view>
				</view>
				<view class="rcc-dialog-sub">已存入我的券包 · 待使用</view>
				<view class="rcc-dialog-btn" @tap="goCoupons">查看我的券包</view>
				<view class="rcc-dialog-continue" @tap="closeSuccess">继续领券</view>
			</view>
		</view>
	</view>
</template>

<script>
	import https from '../../../utils/http';
	import toastService from '../../../utils/toast.service';
	import authService from '../../../utils/auth';
	import dateHelper from '../../../utils/date-helper';
	let app = null;
	export default {
		data() {
			return {
				isLogin: false,
				loading: true,
				loadError: false,
				loadErrorMsg: '',
				memberId: null,
				couponsList: [],
				receivedIds: [],
				submitting: false,
				showSuccess: false,
				successCoupon: null
			};
		},
		onShow() {
			app = getApp();
			// 登录守卫：未登录停留本页引导登录（与「我的」页范式一致），已登录并行拉券池+已领集合
			authService.checkIsLogin().then((result) => {
				this.isLogin = !!result;
				if (result) {
					this.getMemberInfo();
					this.getCouponsPool();
					this.getReceivedIds();
				} else {
					this.loading = false;
				}
			});
		},
		onPullDownRefresh() {
			if (!this.isLogin) {
				uni.stopPullDownRefresh();
				return;
			}
			Promise.all([this.getCouponsPool(), this.getReceivedIds()]).finally(() => {
				uni.stopPullDownRefresh();
			});
		},
		methods: {
			// 失败可见化：之前静默置空列表 + van-empty 未注册 → 页面纯白看不出原因（09-24 排障教训）
			reload() {
				this.loading = true;
				this.loadError = false;
				this.getCouponsPool();
				this.getReceivedIds();
			},
			// memberId 后端已改从 session 取（B1 加固），此处仅兜底传参保持向后兼容
			getMemberInfo() {
				https.request('/rest/member/getLoginMemberInfo', {}).then((result) => {
					if (result && result.success && result.data) {
						this.memberId = result.data.id;
					}
				});
			},
			// 券池：issueType=2（自行领取）且未删除，由后端过滤
			getCouponsPool() {
				return https.request('/rest/member/coupons/list', {}).then((result) => {
					if (result && result.success && result.data) {
						const list = result.data.couponsList || [];
						list.forEach((item) => {
							item.ruleShow = false;
						});
						this.couponsList = list;
						this.loadError = false;
					} else {
						this.couponsList = [];
						this.loadError = true;
						this.loadErrorMsg = (result && result.message) || '接口返回异常';
					}
					this.loading = false;
				}).catch((err) => {
					this.couponsList = [];
					this.loading = false;
					this.loadError = true;
					this.loadErrorMsg = (err && err.message) ? err.message : '网络请求失败';
				});
			},
			// 已领集合：全量拉自己的券关系，取 couponsId 去重（后端查重已上，此处做已领态渲染）
			getReceivedIds() {
				return https.request('/rest/member/couponsMemberRelation/list', {
					pageNo: -1,
					pageSize: 999
				}).then((result) => {
					if (result && result.success && result.data) {
						const ids = [];
						(result.data.records || []).forEach((record) => {
							const info = record.couponsMemberRelationMap || {};
							if (info.couponsId !== undefined && info.couponsId !== null) {
								ids.push(info.couponsId);
							}
						});
						this.receivedIds = ids;
					}
				});
			},
			isReceived(couponsId) {
				return this.receivedIds.indexOf(couponsId) !== -1;
			},
			// 面额：1=折扣（discountAmount 为比率，0.38→3.8 折）/ 2=满减（对齐券包页口径）
			amountText(item) {
				if (Number(item.preferentialType) === 1) {
					const zhe = Number(item.discountAmount);
					if (!isFinite(zhe) || zhe <= 0) {
						return '折扣券';
					}
					return (zhe <= 1 ? zhe * 10 : zhe) + ' 折';
				}
				return '¥' + (item.reducedPrice || 0);
			},
			condText(item) {
				if (Number(item.preferentialType) === 1) {
					return item.description ? String(item.description).substring(0, 7) : '指定商品可用';
				}
				return '满 ' + (item.limitedPrice || 0) + ' 可用';
			},
			// 有效期：validType=2 领后 N 天（endTime 后端算好，前端只做文案）；=1 固定时间段
			validText(item) {
				if (Number(item.validType) === 2) {
					return '领取后 ' + (item.validDays || 0) + ' 天内有效';
				}
				const start = item.validStartTime ? dateHelper.formatISODate(item.validStartTime, 'YYYY-MM-DD') : '';
				const end = item.validEndTime ? dateHelper.formatISODate(item.validEndTime, 'YYYY-MM-DD') : '';
				if (start && end) {
					return start + ' 至 ' + end;
				}
				return '';
			},
			toggleRule(item) {
				item.ruleShow = !item.ruleShow;
			},
			// 领取：POST+JSON（铁律）；后端已有 session memberId + 按券查重，前端 submitting 锁防连点
			onReceive(item) {
				if (this.submitting || this.isReceived(item.id)) {
					return;
				}
				this.submitting = true;
				toastService.showLoading();
				https.request('/rest/member/couponsMemberRelation/insert', {
					couponsId: item.id,
					memberId: this.memberId
				}).then((result) => {
					toastService.hideLoading();
					this.submitting = false;
					if (result && result.success) {
						this.receivedIds.push(item.id);
						this.successCoupon = item;
						this.showSuccess = true;
					} else {
						toastService.showToast((result && result.message) || '领取失败，请稍后再试');
						// 后端查重拒绝（如他端已领）→ 刷新已领集合纠正按钮态
						if (result && result.message && result.message.indexOf('已领取') !== -1) {
							this.getReceivedIds();
						}
					}
				}).catch(() => {
					toastService.hideLoading();
					this.submitting = false;
					toastService.showToast('领取失败，请稍后再试');
				});
			},
			closeSuccess() {
				this.showSuccess = false;
				this.successCoupon = null;
			},
			goCoupons() {
				this.showSuccess = false;
				uni.navigateTo({
					url: '/pages/mine/coupons/coupons'
				});
			},
			// 领的是商家券 → 去点单（tabBar 页必须 switchTab）
			goUse() {
				uni.switchTab({
					url: '/pages/index/index'
				});
			},
			goLogin() {
				app.globalData.checkIsAuth('scope.userInfo');
			}
		}
	};
</script>
<style>
	page {
		background: #f5f5f5;
	}

	.rcc {
		min-height: 100vh;
		background: #f5f5f5;
		padding-bottom: 40rpx;
	}

	.rcc-tip {
		padding: 24rpx 30rpx 8rpx;
		font-size: 22rpx;
		color: #999999;
	}

	.rcc-login-card {
		margin: 24rpx 30rpx;
		padding: 40rpx 30rpx;
		background: white;
		border-radius: 24rpx;
		text-align: center;
	}

	.rcc-login-text {
		font-size: 26rpx;
		color: #666666;
		margin-bottom: 24rpx;
	}

	.rcc-login-btn {
		display: inline-block;
		padding: 16rpx 60rpx;
		background: #5F7D22;
		color: white;
		font-size: 26rpx;
		border-radius: 40rpx;
	}

	.rcc-list {
		padding: 20rpx 24rpx 0;
	}

	.rcc-card {
		display: flex;
		flex-direction: row;
		align-items: center;
		background: white;
		border-radius: 24rpx;
		padding: 24rpx;
		margin-bottom: 20rpx;
	}

	.rcc-face {
		width: 168rpx;
		height: 144rpx;
		background: #F0DDAB;
		border-radius: 16rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
	}

	.rcc-face-amount {
		font-size: 36rpx;
		font-weight: bold;
		color: #6B5320;
	}

	.rcc-face-cond {
		font-size: 20rpx;
		color: #8A7540;
		margin-top: 4rpx;
	}

	.rcc-info {
		flex: 1;
		margin: 0 16rpx;
		overflow: hidden;
	}

	.rcc-name {
		font-size: 28rpx;
		font-weight: 500;
		color: #333333;
	}

	.rcc-valid {
		font-size: 22rpx;
		color: #999999;
		margin-top: 8rpx;
	}

	.rcc-rule {
		font-size: 22rpx;
		color: #999999;
		margin-top: 8rpx;
	}

	.rcc-rule-panel {
		background: white;
		border-radius: 16rpx;
		padding: 20rpx 24rpx;
		margin: -12rpx 0 20rpx;
		font-size: 22rpx;
		color: #999999;
		line-height: 1.6;
	}

	.rcc-btn {
		padding: 16rpx 28rpx;
		background: #5F7D22;
		color: white;
		font-size: 24rpx;
		border-radius: 28rpx;
		flex-shrink: 0;
	}

	.rcc-btn-received {
		background: #CCCCCC;
	}

	.rcc-use {
		font-size: 24rpx;
		color: #5F7D22;
		margin-left: 12rpx;
		flex-shrink: 0;
	}

	.rcc-empty {
		padding: 120rpx 40rpx;
		text-align: center;
	}

	.rcc-empty-icon {
		font-size: 80rpx;
		color: #CCCCCC;
	}

	.rcc-empty-text {
		font-size: 28rpx;
		color: #666666;
		margin-top: 24rpx;
	}

	.rcc-empty-sub {
		font-size: 22rpx;
		color: #BBBBBB;
		margin-top: 12rpx;
		margin-bottom: 32rpx;
	}

	.rcc-mask {
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

	.rcc-dialog {
		width: 600rpx;
		background: white;
		border-radius: 32rpx;
		padding: 48rpx 40rpx 40rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
	}

	.rcc-dialog-check {
		width: 96rpx;
		height: 96rpx;
		line-height: 96rpx;
		text-align: center;
		border-radius: 50%;
		background: #5F7D22;
		color: white;
		font-size: 56rpx;
	}

	.rcc-dialog-title {
		font-size: 34rpx;
		font-weight: bold;
		color: #333333;
		margin-top: 24rpx;
	}

	.rcc-dialog-ticket {
		width: 100%;
		box-sizing: border-box;
		background: #F0DDAB;
		border-radius: 16rpx;
		padding: 24rpx;
		margin-top: 32rpx;
		display: flex;
		flex-direction: row;
		align-items: center;
	}

	.rcc-dialog-amount {
		font-size: 40rpx;
		font-weight: bold;
		color: #6B5320;
	}

	.rcc-dialog-info {
		flex: 1;
		margin-left: 20rpx;
	}

	.rcc-dialog-name {
		font-size: 26rpx;
		font-weight: 500;
		color: #6B5320;
	}

	.rcc-dialog-valid {
		font-size: 20rpx;
		color: #8A7540;
		margin-top: 4rpx;
	}

	.rcc-dialog-sub {
		font-size: 24rpx;
		color: #999999;
		margin-top: 24rpx;
	}

	.rcc-dialog-btn {
		width: 100%;
		box-sizing: border-box;
		background: #5F7D22;
		color: white;
		font-size: 28rpx;
		text-align: center;
		padding: 24rpx 0;
		border-radius: 40rpx;
		margin-top: 32rpx;
	}

	.rcc-dialog-continue {
		font-size: 24rpx;
		color: #999999;
		margin-top: 24rpx;
		padding: 8rpx;
	}
</style>
