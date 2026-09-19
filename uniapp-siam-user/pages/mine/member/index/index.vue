<template>
	<view class="c03">
		<!-- 等级卡（画板 298:97）—— 全宽绿带，非卡片 -->
		<view class="c03-level theme-bg">
			<view class="c03-level-row">
				<view class="c03-level-name">{{ levelText }}</view>
				<view class="c03-level-more" @tap="goGrowth">成长值明细 ›</view>
			</view>
			<!-- 成长值进度（画板 298:101/102/103）：等级与阈值统一走 utils/member-level.js，
			     growth 为 0/缺失时整块不渲染，避免造数 -->
			<block v-if="hasGrowth">
				<view class="c03-track">
					<view class="c03-track-bar" :style="'width:' + level.percent + '%'"></view>
				</view>
				<view class="c03-growth-text">成长值 {{ member.growth }}/{{ level.nextMin }} · 距{{ level.nextName }}还差 {{ level.gap }}</view>
			</block>
		</view>

		<!-- 权益卡（画板 298:104）—— 4 条权益为画板文案池，后端无权益表，暂静态配置 -->
		<view class="c03-card">
			<view class="c03-card-title">会员权益</view>
			<view class="c03-benefit-row" v-for="(row, rowIndex) in benefitRows" :key="rowIndex">
				<view class="c03-benefit-item" v-for="(item, itemIndex) in row" :key="itemIndex">· {{ item }}</view>
			</view>
		</view>

		<!-- 积分积点卡（画板 298:112） -->
		<view class="c03-card">
			<view class="c03-card-head">
				<view class="c03-card-title">我的积分 / 积点</view>
				<view class="c03-card-more" @tap="goIntegral">收支明细 ›</view>
			</view>
			<view class="c03-data-row">
				<view class="c03-data-item">
					<view class="c03-data-value theme-color">{{ member.points || 0 }}</view>
					<view class="c03-data-label">积分（可抵扣）</view>
				</view>
				<!-- 积点（等级成长）：后端无独立字段，沿用 C-01「我的」页同口径取 balance -->
				<view class="c03-data-item">
					<view class="c03-data-value theme-color">{{ member.balance || 0 }}</view>
					<view class="c03-data-label">积点（等级成长）</view>
				</view>
			</view>
		</view>

		<!-- 优惠券卡（画板 298:123） -->
		<view class="c03-card">
			<view class="c03-card-head">
				<view class="c03-card-title">我的优惠券</view>
				<view class="c03-card-more" @tap="goCoupons">{{ couponCount }} 张 ›</view>
			</view>
			<view class="c03-coupon-row" v-if="couponList.length">
				<view class="c03-coupon" v-for="(item, index) in couponList" :key="index" @tap="goCoupons">
					<view class="c03-coupon-value" :class="{ 'c03-coupon-value-sm': item.small }">{{ item.value }}</view>
					<view class="c03-coupon-cond">{{ item.cond }}</view>
				</view>
			</view>
			<view class="c03-coupon-empty" v-else>暂无可用优惠券</view>
		</view>

		<!-- 身份认证行（画板 298:137）已按用户要求移除：本页不再展示认证状态入口；
		     认证能力仍保留在「我的」页常用服务 · 身份认证 → pages/mine/security/index/index -->
	</view>
</template>

<script>
	import https from '../../../../utils/http';
	import authService from '../../../../utils/auth';
	import systemStatus from '../../../../utils/system-status';
	import memberLevel from '../../../../utils/member-level';

	let app = null;

	export default {
		data() {
			return {
				member: {
					id: '',
					username: '',
					headImg: '',
					points: 0,
					balance: 0,
					couponsNumber: 0,
					type: '',
					// 以下字段由后端补齐后自动生效：growth 已随 tb_member 加列上线（见 19-*.sql）；
					// levelName / nextGrowth / nextLevelName 仍无后端来源，等级改由 utils/member-level.js 判定
					levelName: '',
					growth: null,
					nextGrowth: null,
					nextLevelName: ''
				},
				benefitRows: [
					['专属折扣', '生日礼遇'],
					['优先取酒', '会员日积分翻倍']
				],
				couponList: []
			};
		},
		computed: {
			// 等级：有成长值就走等级表（utils/member-level.js），否则退回 type 文案
			// （后端 tb_member 已加 growth 列，见 sql/mysql/cloudrun/19-会员成长值-加列与种子数据.sql）
			hasGrowth() {
				return Number(this.member.growth) > 0;
			},
			level() {
				return memberLevel.levelOf(this.member.growth);
			},
			levelText() {
				if (this.hasGrowth) {
					return this.level.name;
				}
				return this.member.levelName ? this.member.levelName : systemStatus.typeVipText(this.member.type);
			},
			couponCount() {
				return this.member.couponsNumber || 0;
			}
		},
		onLoad() {
			app = getApp();
		},
		onShow() {
			authService.checkIsLogin().then((result) => {
				if (result) {
					this.getMemberInfo();
					this.getCouponList();
					return;
				}
				this.member = {
					id: ''
				};
				this.couponList = [];
				uni.showToast({
					title: '请先登录',
					icon: 'none'
				});
				setTimeout(() => {
					uni.navigateBack();
				}, 800);
			});
		},
		methods: {
			// 会员基础信息：未登录/接口异常时保持空态，不阻断页面渲染
			getMemberInfo() {
				https.request('/rest/member/getLoginMemberInfo', {}).then((result) => {
					if (result && result.success && result.data) {
						this.member = result.data;
					}
				});
			},
			// 券卡取前 3 张可用券（isUsed/isExpired/isValid 三个 0 与「我的优惠券」页同口径）
			getCouponList() {
				https.request('/rest/member/couponsMemberRelation/list', {
					pageNo: 1,
					pageSize: 3,
					isUsed: 0,
					isExpired: 0,
					isValid: 0
				}).then((result) => {
					if (result && result.success && result.data && result.data.records) {
						this.couponList = result.data.records.slice(0, 3).map((row) => this.toCouponChip(row));
					} else {
						this.couponList = [];
					}
				});
			},
			// 券面额/门槛换算：preferentialType 1=折扣（discountAmount 为比率，0.38 → 3.8 折）/ 2=满减
			toCouponChip(row) {
				const coupon = (row && row.couponsMemberRelationMap) || row || {};
				const type = Number(coupon.preferentialType);
				const name = coupon.couponsName || coupon.name || '';
				if (type === 2) {
					return {
						value: '¥' + (coupon.reducedPrice || 0),
						cond: '满 ' + (coupon.limitedPrice || 0) + ' 可用',
						small: false
					};
				}
				if (type === 1) {
					const discount = Number(coupon.discountAmount);
					const zhe = isFinite(discount) && discount > 0 ?
						Math.round((discount <= 1 ? discount * 10 : discount) * 10) / 10 :
						0;
					return {
						value: zhe ? zhe + ' 折' : '折扣券',
						cond: coupon.description || name || '指定酒款',
						small: false
					};
				}
				return {
					value: name || '优惠券',
					cond: coupon.description || '',
					small: true
				};
			},
			// C-08「我的成长值」（画板 21:660）已落地 → 直接跳转
			goGrowth() {
				uni.navigateTo({
					url: '/pages/mine/growth/index/index'
				});
			},
			goIntegral() {
				uni.navigateTo({
					url: '/pages/mine/integral/integral'
				});
			},
			goCoupons() {
				uni.navigateTo({
					url: '/pages/mine/coupons/coupons'
				});
			}
		}
	};
</script>

<style>
	page {
		background: #F0F3F2;
	}

	.c03 {
		min-height: 100vh;
		background: #F0F3F2;
		padding-bottom: 40rpx;
	}

	/* ========== 等级卡（画板 298:97）：全宽绿带 ========== */
	.c03-level {
		padding: 32rpx 32rpx 36rpx 32rpx;
	}

	.c03-level-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.c03-level-name {
		font-size: 36rpx;
		font-weight: 700;
		line-height: 52rpx;
		color: #ffffff;
	}

	.c03-level-more {
		font-size: 22rpx;
		color: #ffffff;
		opacity: 0.95;
	}

	.c03-track {
		height: 12rpx;
		border-radius: 6rpx;
		background: #ffffff;
		overflow: hidden;
		margin-top: 20rpx;
	}

	.c03-track-bar {
		height: 12rpx;
		border-radius: 6rpx;
		background: #F0DCAB;
	}

	.c03-growth-text {
		font-size: 22rpx;
		line-height: 32rpx;
		color: #ffffff;
		margin-top: 20rpx;
	}

	/* ========== 白卡通用：与 C-01「我的」页同口径（24rpx 页边距 / 24rpx 圆角 / 28rpx 内边距 / 20rpx 卡间距） ========== */
	.c03-card {
		margin: 20rpx 24rpx 0 24rpx;
		padding: 28rpx;
		background: #ffffff;
		border-radius: 24rpx;
	}

	.c03-card-head {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.c03-card-title {
		font-size: 28rpx;
		font-weight: 500;
		line-height: 40rpx;
		color: #333333;
	}

	.c03-card-more {
		font-size: 22rpx;
		color: #999999;
	}

	/* ========== 权益行（画板 298:106 / 298:109） ========== */
	.c03-benefit-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-top: 20rpx;
	}

	.c03-benefit-item {
		font-size: 24rpx;
		line-height: 34rpx;
		color: #666666;
	}

	/* ========== 积分 / 积点数据行（画板 298:116） ========== */
	.c03-data-row {
		display: flex;
		align-items: flex-start;
		justify-content: space-between;
		margin-top: 20rpx;
	}

	.c03-data-item {
		display: flex;
		flex-direction: column;
		align-items: center;
	}

	.c03-data-value {
		font-size: 40rpx;
		font-weight: 700;
		line-height: 56rpx;
	}

	.c03-data-label {
		font-size: 20rpx;
		line-height: 26rpx;
		color: #999999;
		margin-top: 4rpx;
	}

	/* ========== 优惠券行（画板 298:127） ========== */
	.c03-coupon-row {
		display: flex;
		margin-top: 20rpx;
	}

	.c03-coupon {
		flex: 1;
		height: 104rpx;
		border-radius: 16rpx;
		background: #F0DCAB;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		overflow: hidden;
	}

	.c03-coupon + .c03-coupon {
		margin-left: 16rpx;
	}

	.c03-coupon-value {
		font-size: 30rpx;
		font-weight: 700;
		line-height: 42rpx;
		color: #6B5320;
	}

	/* 文字券（如「小食券」）面额降一档字号，对应画板 298:135 的 13pt */
	.c03-coupon-value-sm {
		font-size: 26rpx;
	}

	.c03-coupon-cond {
		font-size: 18rpx;
		line-height: 26rpx;
		color: #8A7540;
		margin-top: 4rpx;
		max-width: 100%;
		overflow: hidden;
		white-space: nowrap;
		text-overflow: ellipsis;
	}

	.c03-coupon-empty {
		font-size: 22rpx;
		color: #B0B0B0;
		margin-top: 20rpx;
		padding: 24rpx 0;
		text-align: center;
	}
</style>
