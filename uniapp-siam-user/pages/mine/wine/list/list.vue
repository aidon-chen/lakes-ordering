<template>
	<view class="wl">
		<!-- 有效期提示条 -->
		<view class="wl-notice-wrap">
			<view class="wl-notice">存酒有效期 90 天，临期会在此提醒</view>
		</view>

		<!-- 临期提醒：有「将过期」记录时动态提示 -->
		<view class="wl-warn" v-if="expiringCount > 0">
			您有 {{ expiringCount }} 笔存酒即将到期，请尽快到店取用
		</view>

		<!-- 待核销取酒码（取第一条待核销申请；无则不显示） -->
		<view class="wl-code-card" v-if="topPending">
			<view class="wl-code-label">待核销取酒码</view>
			<view class="wl-code theme-color">{{ topPending.takeCode }}</view>
			<view class="wl-code-tip">到店出示即可取酒 · 24 小时内有效</view>
			<view class="wl-code-tip">有效期至 {{ topPending.expireTime }}</view>
			<view class="wl-cancel theme-color" @tap="cancelApply(topPending)">取消申请</view>
		</view>

		<!-- 存酒卡列表 -->
		<view class="wl-card" v-for="wine in list" :key="wine.id">
			<view class="wl-head-row">
				<view class="wl-name">{{ wine.goodsName }}</view>
				<view class="wl-tag" :class="tagClass(wine.statusCode)">{{ wine.statusText }}</view>
			</view>
			<view class="wl-detail">{{ remainText(wine) }}</view>
			<view class="wl-date">存入 {{ wine.storageDate }} · 有效期至 {{ wine.expireDate }}</view>
			<view class="wl-act-row">
				<view
					class="wl-apply-btn"
					:class="{ 'wl-apply-off': !canApply(wine) }"
					@tap="openApply(wine)"
				>{{ applyBtnText(wine) }}</view>
			</view>
		</view>

		<view class="wl-empty" v-if="!list.length">{{ loading ? '加载中…' : '暂无存酒记录' }}</view>

		<!-- 我的取酒申请 -->
		<view class="wl-sec" v-if="applies.length">
			<view class="wl-sec-title">我的取酒申请</view>
			<view class="wl-apply-item" v-for="a in applies" :key="a.id">
				<view class="wl-apply-main">
					<view class="wl-apply-name">{{ a.goodsName }}</view>
					<view class="wl-apply-sub">{{ a.takeCount }} 瓶 · {{ a.applyTime }}</view>
				</view>
				<view class="wl-apply-status" :class="applyTagClass(a.statusCode)">{{ a.statusText }}</view>
				<view class="wl-apply-cancel theme-color" v-if="Number(a.statusCode) === 1" @tap="cancelApply(a)">取消</view>
			</view>
		</view>

		<!-- 申请取酒弹窗 -->
		<view class="wl-mask" v-if="applyTarget" @tap="closeApply">
			<view class="wl-dialog" @tap.stop>
				<view class="wl-dialog-title">申请取酒</view>
				<view class="wl-dialog-sub">{{ applyTarget.goodsName }} · 当前剩余 {{ applyTarget.remainCount }} 瓶</view>
				<view class="wl-dialog-row">
					<text class="wl-dialog-label">取出瓶数</text>
					<input class="wl-dialog-input" type="number" v-model="applyCount" />
					<text class="wl-dialog-hint">瓶（最多 {{ applyTarget.remainCount }}）</text>
				</view>
				<view class="wl-dialog-actions">
					<view class="wl-btn wl-btn-ghost" @tap="closeApply">取消</view>
					<view class="wl-btn wl-btn-primary" :class="{ 'wl-btn-off': !canSubmitApply }" @tap="doApply">确认申请</view>
				</view>
			</view>
		</view>

		<!-- 出码弹窗 -->
		<view class="wl-mask" v-if="createdApply" @tap="closeCreated">
			<view class="wl-dialog" @tap.stop>
				<view class="wl-dialog-title">取酒码</view>
				<view class="wl-code-big theme-color">{{ createdApply.takeCode }}</view>
				<view class="wl-dialog-sub">到店出示即可取酒 · 24 小时内有效</view>
				<view class="wl-dialog-sub">有效期至 {{ createdApply.expireTime }}</view>
				<view class="wl-dialog-actions">
					<view class="wl-btn wl-btn-primary" @tap="closeCreated">知道了</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	import https from '../../../../utils/http';
	import authService from '../../../../utils/auth';

	export default {
		data() {
			return {
				list: [],
				applies: [],
				loading: true,
				applyTarget: null,
				applyCount: 1,
				createdApply: null,
				busy: false
			};
		},
		computed: {
			/** 待核销申请 */
			pendingList() {
				return this.applies.filter((a) => Number(a.statusCode) === 1);
			},
			/** 顶部卡展示第一条待核销申请 */
			topPending() {
				return this.pendingList.length ? this.pendingList[0] : null;
			},
			/** 临期提醒条数（将过期） */
			expiringCount() {
				return this.list.filter((w) => Number(w.statusCode) === 4).length;
			},
			canSubmitApply() {
				const t = this.applyTarget;
				if (!t) return false;
				const n = Number(this.applyCount);
				return Number.isInteger(n) && n >= 1 && n <= Number(t.remainCount || 0);
			}
		},
		onLoad() {
			authService.checkIsLogin().then((result) => {
				if (result) {
					this.loadList();
					this.loadApplies();
					this.checkExpiring(); // 批次F · W-19：临期/过期弹窗提醒
					return;
				}
				this.loading = false;
				getApp().globalData.checkIsAuth('scope.userInfo');
			});
		},
		methods: {
			/**
			 * 批次F · W-19 存酒临期提醒（弹窗口径，用户拍板；不做短信）
			 * - 数据源：/rest/member/wineStorage/expiringList（剩余天数≤门店设置的提前天数）
			 * - 同一存酒同一天只弹一次（本地记忆；后端 remindAck 亦按天幂等）
			 * - 弹窗确认后回报 remindAck → 收银端 W-19「通知状态」列显示「已弹窗 MM-dd」
			 */
			checkExpiring() {
				https.request('/rest/member/wineStorage/expiringList', {}).then((result) => {
					const rows = (result && result.success && Array.isArray(result.data)) ? result.data : [];
					if (!rows.length) return;
					const d = new Date();
					const key = 'wineRemindShown_' + d.getFullYear() + '-' + (d.getMonth() + 1) + '-' + d.getDate();
					const shown = uni.getStorageSync(key) || [];
					const fresh = rows.filter((r) => shown.indexOf(r.storageId) === -1);
					if (!fresh.length) return;
					const lines = fresh.slice(0, 3).map((r) => {
						const n = Number(r.daysRemaining);
						const when = n < 0 ? ('已过期 ' + Math.abs(n) + ' 天') : (n === 0 ? '今天到期' : ('剩 ' + n + ' 天'));
						return (r.goodsName || '存酒') + ' · ' + when;
					});
					const more = fresh.length > 3 ? ('\n…等共 ' + fresh.length + ' 条') : '';
					uni.showModal({
						title: '存酒临期提醒',
						content: lines.join('\n') + more,
						showCancel: false,
						confirmText: '知道了',
						success: () => {
							uni.setStorageSync(key, shown.concat(fresh.map((r) => r.storageId)));
							https.request('/rest/member/wineStorage/remindAck', {
								storageIds: fresh.map((r) => r.storageId)
							}).then(() => {}).catch(() => {});
						}
					});
				}).catch(() => {});
			},
			loadList() {
				https.request('/rest/member/wineStorage/list', {}).then((result) => {
					this.loading = false;
					if (result && result.success && result.data) {
						this.list = result.data;
						return;
					}
					this.list = [];
				}).catch(() => {
					this.loading = false;
				});
			},
			loadApplies() {
				https.request('/rest/member/wineStorage/applyList', {}).then((result) => {
					if (result && result.success && result.data) {
						this.applies = result.data;
						return;
					}
					this.applies = [];
				}).catch(() => {});
			},
			remainText(wine) {
				const count = wine.remainCount === null || wine.remainCount === undefined ? wine.bottleCount : wine
					.remainCount;
				return count + ' 瓶' + (wine.shopName ? ' · ' + wine.shopName : '');
			},
			// statusCode：1 存续中 / 2 已取完 / 3 已过期 / 4 将过期
			tagClass(statusCode) {
				if (Number(statusCode) === 4) return 'wl-tag-warn';
				if (Number(statusCode) === 3 || Number(statusCode) === 2) return 'wl-tag-off';
				return 'wl-tag-ok';
			},
			// 申请状态：1 待核销 / 2 已核销 / 3 已取消 / 4 已过期
			applyTagClass(statusCode) {
				const s = Number(statusCode);
				if (s === 1) return 'wl-tag-ok';
				if (s === 4) return 'wl-tag-warn';
				return 'wl-tag-off';
			},
			canApply(wine) {
				const s = Number(wine.statusCode);
				return (s === 1 || s === 4) && !wine.pendingApply;
			},
			applyBtnText(wine) {
				if (wine.pendingApply) return '已有待核销申请';
				const s = Number(wine.statusCode);
				if (s === 2) return '已取完';
				if (s === 3) return '已过期';
				return '申请取酒';
			},
			openApply(wine) {
				if (!this.canApply(wine)) {
					if (wine.pendingApply) {
						uni.showToast({ title: '该存酒已有待核销申请', icon: 'none' });
					}
					return;
				}
				this.applyTarget = wine;
				this.applyCount = 1;
			},
			closeApply() {
				this.applyTarget = null;
			},
			doApply() {
				if (!this.canSubmitApply || this.busy) return;
				this.busy = true;
				https.request('/rest/member/wineStorage/applyTake', {
					storageId: this.applyTarget.id,
					takeCount: Number(this.applyCount)
				}).then((result) => {
					this.busy = false;
					if (result && result.success && result.data) {
						this.applyTarget = null;
						this.createdApply = result.data;
						this.loadList();
						this.loadApplies();
						return;
					}
					uni.showToast({ title: (result && result.message) || '申请失败', icon: 'none' });
				}).catch((e) => {
					this.busy = false;
					uni.showToast({ title: (e && e.message) || '申请失败', icon: 'none' });
				});
			},
			closeCreated() {
				this.createdApply = null;
			},
			cancelApply(a) {
				if (Number(a.statusCode) !== 1) return;
				uni.showModal({
					title: '取消申请',
					content: '确定取消这笔取酒申请吗？',
					success: (r) => {
						if (!r.confirm) return;
						https.request('/rest/member/wineStorage/cancelApply', { applyId: a.id }).then((res) => {
							if (res && res.success) {
								this.loadApplies();
								this.loadList();
								return;
							}
							uni.showToast({ title: (res && res.message) || '取消失败', icon: 'none' });
						}).catch((e) => {
							uni.showToast({ title: (e && e.message) || '取消失败', icon: 'none' });
						});
					}
				});
			}
		}
	};
</script>

<style>
	page {
		background: #f5f5f5;
	}

	.wl {
		min-height: 100vh;
		padding: 24rpx;
		box-sizing: border-box;
		background: #f5f5f5;
	}

	/* 提示条：底 #FFF7E6 / 字 #8A6D3B（批次5 语义色） */
	.wl-notice-wrap {
		margin-bottom: 20rpx;
	}

	.wl-notice {
		background: #FFF7E6;
		color: #8A6D3B;
		font-size: 24rpx;
		line-height: 36rpx;
		padding: 20rpx;
		border-radius: 16rpx;
	}

	/* 临期提醒条：警示橙 */
	.wl-warn {
		background: #FCF5ED;
		color: #B8720E;
		font-size: 24rpx;
		line-height: 36rpx;
		padding: 20rpx;
		border-radius: 16rpx;
		margin-bottom: 20rpx;
	}

	.wl-code-card {
		background: #ffffff;
		border-radius: 24rpx;
		padding: 32rpx;
		margin-bottom: 20rpx;
	}

	.wl-code-label {
		font-size: 24rpx;
		color: #999999;
	}

	.wl-code {
		font-size: 72rpx;
		font-weight: 700;
		line-height: 96rpx;
		margin-top: 12rpx;
		letter-spacing: 8rpx;
	}

	.wl-code-tip {
		font-size: 22rpx;
		color: #999999;
		margin-top: 4rpx;
	}

	.wl-cancel {
		font-size: 24rpx;
		margin-top: 16rpx;
	}

	.wl-card {
		background: #ffffff;
		border-radius: 24rpx;
		padding: 24rpx;
		margin-bottom: 20rpx;
	}

	.wl-head-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.wl-name {
		font-size: 28rpx;
		font-weight: 600;
		color: #333333;
		flex: 1;
		margin-right: 12rpx;
	}

	.wl-tag {
		font-size: 20rpx;
		height: 40rpx;
		line-height: 40rpx;
		padding: 0 12rpx;
		border-radius: 8rpx;
		flex-shrink: 0;
	}

	.wl-tag-ok {
		background: #E8F5EE;
		color: #43A047;
	}

	.wl-tag-warn {
		background: #FFF7E6;
		color: #E6A23C;
	}

	.wl-tag-off {
		background: #F0F0F0;
		color: #B0B0B0;
	}

	.wl-detail {
		font-size: 24rpx;
		color: #666666;
		margin-top: 16rpx;
	}

	.wl-date {
		font-size: 22rpx;
		color: #999999;
		margin-top: 8rpx;
	}

	.wl-act-row {
		display: flex;
		justify-content: flex-end;
		margin-top: 20rpx;
	}

	.wl-apply-btn {
		font-size: 24rpx;
		color: #ffffff;
		background: #5f7d22;
		border-radius: 32rpx;
		padding: 10rpx 32rpx;
	}

	.wl-apply-off {
		background: #EDEDED;
		color: #AAAAAA;
	}

	.wl-empty {
		text-align: center;
		font-size: 26rpx;
		color: #999999;
		padding-top: 120rpx;
	}

	.wl-sec {
		margin-top: 10rpx;
	}

	.wl-sec-title {
		font-size: 26rpx;
		font-weight: 600;
		color: #333333;
		margin: 12rpx 0;
	}

	.wl-apply-item {
		display: flex;
		align-items: center;
		background: #ffffff;
		border-radius: 16rpx;
		padding: 20rpx 24rpx;
		margin-bottom: 12rpx;
	}

	.wl-apply-main {
		flex: 1;
		min-width: 0;
	}

	.wl-apply-name {
		font-size: 26rpx;
		color: #333333;
	}

	.wl-apply-sub {
		font-size: 22rpx;
		color: #999999;
		margin-top: 4rpx;
	}

	.wl-apply-status {
		font-size: 22rpx;
		padding: 4rpx 12rpx;
		border-radius: 8rpx;
		margin-left: 12rpx;
	}

	.wl-apply-cancel {
		font-size: 24rpx;
		margin-left: 16rpx;
	}

	/* 弹窗 */
	.wl-mask {
		position: fixed;
		left: 0;
		top: 0;
		right: 0;
		bottom: 0;
		background: rgba(0, 0, 0, 0.45);
		display: flex;
		align-items: center;
		justify-content: center;
		z-index: 99;
	}

	.wl-dialog {
		width: 600rpx;
		background: #ffffff;
		border-radius: 24rpx;
		padding: 40rpx 32rpx 24rpx;
		box-sizing: border-box;
	}

	.wl-dialog-title {
		font-size: 32rpx;
		font-weight: 600;
		color: #333333;
		text-align: center;
	}

	.wl-dialog-sub {
		font-size: 24rpx;
		color: #999999;
		text-align: center;
		margin-top: 12rpx;
	}

	.wl-dialog-row {
		display: flex;
		align-items: center;
		margin-top: 32rpx;
	}

	.wl-dialog-label {
		font-size: 26rpx;
		color: #333333;
	}

	.wl-dialog-input {
		flex: 1;
		height: 72rpx;
		border: 1rpx solid #EDEDED;
		border-radius: 12rpx;
		margin: 0 16rpx;
		text-align: center;
		font-size: 28rpx;
		color: #333333;
	}

	.wl-dialog-hint {
		font-size: 22rpx;
		color: #999999;
	}

	.wl-code-big {
		font-size: 88rpx;
		font-weight: 700;
		text-align: center;
		letter-spacing: 12rpx;
		margin: 24rpx 0 8rpx;
	}

	.wl-dialog-actions {
		display: flex;
		margin-top: 40rpx;
	}

	.wl-btn {
		flex: 1;
		height: 80rpx;
		line-height: 80rpx;
		text-align: center;
		border-radius: 40rpx;
		font-size: 28rpx;
	}

	.wl-btn-ghost {
		background: #F5F5F5;
		color: #666666;
		margin-right: 20rpx;
	}

	.wl-btn-primary {
		background: #5f7d22;
		color: #ffffff;
	}

	.wl-btn-off {
		background: #C8D3B0;
		color: #ffffff;
	}
</style>
