<template>
	<view class="c01">
		<!-- 状态栏占位（本页 navigationStyle: custom） -->
		<view class="c01-status" :style="'height:' + statusBarHeight + 'rpx;'"></view>

		<!-- 绿色头部：用户行 + 会员卡 -->
		<view class="c01-head theme-bg">
			<view class="c01-user-row">
				<view class="c01-avatar">
					<image v-if="member.id && member.headImg" class="c01-avatar-img" :src="member.headImg"
						mode="aspectFill"></image>
					<image v-else class="c01-avatar-person" src="/static/assets/icons/mine-avatar-person.png"
						mode="widthFix"></image>
				</view>
				<view class="c01-user-col">
					<view class="c01-nickname">{{ member.id ? member.username : '请登录/注册' }}</view>
					<view class="c01-user-line" v-if="member.id && phoneLine">{{ phoneLine }}</view>
					<view class="c01-user-line" v-if="member.id && profileLine">{{ profileLine }}</view>
				</view>
			</view>

			<view class="c01-vip-card" v-if="member.id" @tap="goMemberCenter">
				<view class="c01-vip-level-row">
					<view class="c01-vip-level-left">
						<image class="c01-crown" src="/static/assets/icons/mine-crown.png" mode="widthFix"></image>
						<view class="c01-vip-level-text">{{ vipLevelText }}</view>
					</view>
					<view class="c01-vip-growth">{{ growthText }}</view>
				</view>
				<view class="c01-vip-divider"></view>
				<view class="c01-vip-data-row">
					<view class="c01-vip-data-item">
						<view class="c01-vip-value">{{ member.points || 0 }}</view>
						<view class="c01-vip-label">积分</view>
					</view>
					<view class="c01-vip-data-item">
						<view class="c01-vip-value">{{ member.balance || 0 }}</view>
						<view class="c01-vip-label">积点</view>
					</view>
					<view class="c01-vip-data-item">
						<view class="c01-vip-value">{{ member.couponsNumber || 0 }}</view>
						<view class="c01-vip-label">优惠券</view>
					</view>
				</view>
			</view>
		</view>

		<!-- 订单入口卡（上叠绿色头部，叠入量 = 设计稿头部底与卡片顶之差 36pt） -->
		<view class="c01-order-card">
			<view class="c01-title-row">
				<view class="c01-title">我的订单</view>
				<view class="c01-more" @tap="bindOrderInfo">全部订单 ›</view>
			</view>
			<view class="c01-entry-row">
				<view class="c01-entry" v-for="(item, index) in orderEntries" :key="index" @tap="goOrderCenter(item)">
					<image class="c01-entry-icon" :src="item.icon" mode="widthFix"></image>
					<view class="c01-entry-label">{{ item.name }}</view>
				</view>
			</view>
		</view>

		<!-- 存酒区（C-01 原型 298:53「存酒区」/ 298:54「存酒卡」）：常驻板块，
		     无数据时给空态而非整卡隐藏 —— 否则接口一次抖动该板块会凭空消失 -->
		<view class="c01-block">
			<view class="c01-card">
				<view class="c01-title-row">
					<view class="c01-title">我的存酒</view>
					<view class="c01-more" @tap="goWineList">全部 ›</view>
				</view>
				<block v-for="(wine, index) in wineList" :key="wine.id">
					<view class="c01-wine-row">
						<view class="c01-wine-left">
							<view class="c01-wine-name">{{ wineName(wine) }}</view>
							<view class="c01-wine-meta">{{ wine.summaryLine }}</view>
						</view>
						<view class="c01-wine-tag" :class="wineTagClass(wine.statusCode)">{{ wine.statusText }}</view>
					</view>
					<view class="c01-wine-expire" v-if="index === 0 && wine.expireDate">有效期至 {{ wine.expireDate }} ·
						将过期将短信提醒</view>
				</block>
				<view class="c01-wine-empty" v-if="!wineList.length">暂无存酒记录，到店存酒后可在此查看</view>
				<view class="c01-wine-note">已过期存酒请联系门店处理，将过期将于到期前 3 天短信提醒</view>
			</view>
		</view>

		<!-- 常用服务 -->
		<view class="c01-block">
			<view class="c01-card c01-svc-card">
				<view class="c01-title">常用服务</view>
				<view class="c01-entry-row c01-svc-row">
					<view class="c01-entry" v-for="(item, index) in services" :key="index" @tap="goService(item)">
						<image class="c01-entry-icon" :src="item.icon" mode="widthFix"></image>
						<view class="c01-entry-label">{{ item.name }}</view>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	import https from '../../../utils/http';
	import authService from '../../../utils/auth';
	import systemStatus from '../../../utils/system-status';
	import memberLevel from '../../../utils/member-level';
	//获取应用实例
	let app = null;
	export default {
		data() {
			return {
				statusBarHeight: 40,
				member: {
					id: '',
					headImg: '',
					username: '',
					phone: '',
					birthday: '',
					sex: '',
					points: 0,
					balance: 0,
					couponsNumber: 0,
					type: '',
					levelName: '',
					growth: null,
					nextGrowth: null
				},
				// 订单入口 4 项：订单中心现有筛选为 all/waitPayment/waitPickedUp/afterSales，
				// 「待评价」「再来一单」后端暂无对应筛选/动作接口 → 本批落「全部」列表，待后续批次补
				orderEntries: [{
						name: '待自提',
						icon: '/static/assets/icons/mine-pickup.png',
						modeType: 'waitPickedUp',
						modeId: 2
					},
					{
						name: '待评价',
						icon: '/static/assets/icons/mine-review.png',
						modeType: 'all',
						modeId: 0
					},
					{
						name: '退款/售后',
						icon: '/static/assets/icons/mine-refund.png',
						modeType: 'afterSales',
						modeId: 3
					},
					{
						name: '再来一单',
						icon: '/static/assets/icons/mine-reorder.png',
						modeType: 'all',
						modeId: 0
					}
				],
				services: [{
						name: '身份认证',
						icon: '/static/assets/icons/mine-identity.png',
						url: '/pages/mine/security/index/index'
					},
					{
						name: '发票申请',
						icon: '/static/assets/icons/mine-invoice.png',
						url: '/pages/mine/invoice/apply/apply'
					},
					{
						name: '公告',
						icon: '/static/assets/icons/mine-notice.png',
						url: '/pages/notice/list/list'
					},
					{
						name: '优惠券',
						icon: '/static/assets/icons/mine-coupon.png',
						url: '/pages/mine/coupons/coupons'
					}
				],
				wineList: []
			};
		},
		computed: {
			phoneLine() {
				const phone = this.member.phone;
				if (!phone) {
					return '微信获取';
				}
				const str = String(phone);
				if (str.length === 11) {
					return str.substring(0, 3) + '****' + str.substring(7) + ' · 微信获取';
				}
				return str + ' · 微信获取';
			},
			profileLine() {
				const parts = [];
				if (this.member.birthday) {
					const d = String(this.member.birthday);
					parts.push('生日 ' + (d.length >= 10 ? d.substring(5, 10) : d));
				}
				if (this.member.sex) {
					parts.push('性别 ' + this.member.sex);
				}
				return parts.join(' · ');
			},
			vipLevelText() {
				// 有成长值就用等级表（utils/member-level.js）；否则退回 type 文案（1 普通用户 / 2 VIP会员）
				if (Number(this.member.growth) > 0) {
					return this.level.name;
				}
				return this.member.levelName ? this.member.levelName : systemStatus.typeVipText(this.member.type);
			},
			level() {
				return memberLevel.levelOf(this.member.growth);
			},
			growthText() {
				// growth 已随 tb_member 加列上线；有值显示「成长值 x/下一档阈值 ›」，否则退化为会员中心入口
				if (Number(this.member.growth) > 0) {
					return '成长值 ' + this.member.growth + '/' + this.level.nextMin + ' ›';
				}
				return '会员权益 ›';
			}
		},
		onLoad() {
			app = getApp();
			if (app.globalData.systemInfoSync && app.globalData.systemInfoSync.statusBarHeight) {
				this.statusBarHeight = app.globalData.systemInfoSync.statusBarHeight * 2;
			}
		},
		onShow() {
			authService.checkIsLogin().then((result) => {
				if (result) {
					// 两个接口互不依赖，并行发起：任一个失败都不影响另一个。
					// 原先存酒取数嵌在「会员信息成功」分支里，会员接口一次抖动
					// 就会连带整块存酒区不渲染（表现为「板块没实现」）
					this.getMemberInfo();
					this.loadWineList();
					return;
				}
				this.member = {
					id: ''
				};
				this.wineList = [];
				app.globalData.checkIsAuth('scope.userInfo');
			});
		},
		methods: {
			// 会员信息（未登录/接口异常时保持空态，不阻断页面渲染）
			getMemberInfo() {
				https.request('/rest/member/getLoginMemberInfo', {}).then((result) => {
					if (result && result.success && result.data) {
						this.member = result.data;
					}
				});
			},
			// 存酒区（C-01 原型 298:53）：取前 3 条；失败或无数据时置空 → 板块走空态而非消失
			loadWineList() {
				https.request('/rest/member/wineStorage/list', {}).then((result) => {
					if (result && result.success && result.data) {
						this.wineList = result.data.slice(0, 3);
					} else {
						this.wineList = [];
					}
				});
			},
			wineName(wine) {
				const count = wine.remainCount === null || wine.remainCount === undefined ? wine.bottleCount : wine
					.remainCount;
				return wine.goodsName + ' × ' + count + ' 瓶';
			},
			// statusCode：1 存续中 / 2 已取完 / 3 已过期 / 4 将过期
			wineTagClass(statusCode) {
				if (statusCode === 4) {
					return 'c01-tag-warn';
				}
				if (statusCode === 3 || statusCode === 2) {
					return 'c01-tag-off';
				}
				return 'c01-tag-ok';
			},
			goMemberCenter() {
				uni.navigateTo({
					url: '/pages/mine/member/index/index'
				});
			},
			goWineList() {
				uni.navigateTo({
					url: '/pages/mine/wine/list/list'
				});
			},
			goService(item) {
				uni.navigateTo({
					url: item.url
				});
			},
			bindOrderInfo() {
				this.goOrderCenter({
					modeType: 'all',
					modeId: 0
				});
			},
			// 订单中心是 tabBar 页：switchTab 不能带参，经 globalData 传递入口状态（onShow 消费）
			goOrderCenter(item) {
				app.globalData.orderCenterEntry = {
					currentTab: 0,
					modeType: item.modeType,
					currentOrderTab: item.modeId
				};
				uni.switchTab({
					url: '/pages/order/index/index'
				});
			}
		}
	};
</script>

<style>
	page {
		background: #f5f5f5;
	}

	.c01 {
		min-height: 100vh;
		background: #f5f5f5;
		/* 底部留出 tabBar 高度 */
		padding-bottom: calc(140rpx + constant(safe-area-inset-bottom));
		padding-bottom: calc(140rpx + env(safe-area-inset-bottom));
	}

	/* ========== 绿色头部 ========== */
	.c01-head {
		height: 480rpx;
		box-sizing: border-box;
		padding: 28rpx 40rpx 40rpx 40rpx;
	}

	.c01-user-row {
		display: flex;
		align-items: center;
	}

	.c01-avatar {
		width: 112rpx;
		height: 112rpx;
		border-radius: 50%;
		background: #ffffff;
		display: flex;
		align-items: center;
		justify-content: center;
		overflow: hidden;
		flex-shrink: 0;
	}

	.c01-avatar-img {
		width: 112rpx;
		height: 112rpx;
	}

	.c01-avatar-person {
		width: 60rpx;
	}

	.c01-user-col {
		margin-left: 28rpx;
		display: flex;
		flex-direction: column;
	}

	.c01-nickname {
		font-size: 34rpx;
		font-weight: 500;
		color: #ffffff;
	}

	.c01-user-line {
		font-size: 24rpx;
		color: #ffffff;
		opacity: 0.92;
		margin-top: 8rpx;
	}

	/* 会员卡：深绿渐变（批次5 决策 D-E，画板深青绿映射到主色深阶） */
	.c01-vip-card {
		margin-top: 28rpx;
		padding: 28rpx;
		border-radius: 24rpx;
		background: linear-gradient(135deg, #485f1a, #3a4d15);
		border: 1rpx solid rgba(240, 220, 171, 0.65);
	}

	.c01-vip-level-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.c01-vip-level-left {
		display: flex;
		align-items: center;
	}

	.c01-crown {
		width: 32rpx;
		margin-right: 12rpx;
	}

	.c01-vip-level-text {
		font-size: 30rpx;
		font-weight: 600;
		color: #F0DCAB;
	}

	.c01-vip-growth {
		font-size: 22rpx;
		color: #ffffff;
		opacity: 0.95;
	}

	.c01-vip-divider {
		height: 2rpx;
		background: #F0DCAB;
		opacity: 0.85;
		margin: 20rpx 0;
	}

	.c01-vip-data-row {
		display: flex;
		align-items: flex-start;
	}

	.c01-vip-data-item {
		flex: 1;
		display: flex;
		flex-direction: column;
	}

	.c01-vip-value {
		font-size: 36rpx;
		font-weight: 700;
		color: #F0DCAB;
	}

	.c01-vip-label {
		font-size: 20rpx;
		color: #ffffff;
		margin-top: 4rpx;
	}

	/* ========== 卡片通用 ========== */
	.c01-order-card {
		/* 上叠绿色头部：设计稿头部底 y=284pt、卡片顶 y=248pt → 叠入 36pt = 72rpx */
		margin: -72rpx 24rpx 0 24rpx;
		background: #ffffff;
		border-radius: 24rpx;
		padding: 28rpx;
		position: relative;
		z-index: 2;
	}

	.c01-block {
		margin: 20rpx 24rpx 0 24rpx;
	}

	.c01-card {
		background: #ffffff;
		border-radius: 24rpx;
		padding: 24rpx;
	}

	.c01-svc-card {
		padding: 28rpx;
	}

	.c01-title-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.c01-title {
		font-size: 28rpx;
		font-weight: 500;
		color: #333333;
	}

	.c01-more {
		font-size: 22rpx;
		color: #999999;
	}

	/* ========== 4 宫格入口 ========== */
	.c01-entry-row {
		display: flex;
		align-items: flex-start;
		justify-content: space-between;
		margin-top: 20rpx;
	}

	.c01-svc-row {
		margin-top: 24rpx;
	}

	.c01-entry {
		width: 25%;
		display: flex;
		flex-direction: column;
		align-items: center;
	}

	.c01-entry-icon {
		width: 44rpx;
		height: 44rpx;
	}

	.c01-entry-label {
		font-size: 22rpx;
		color: #666666;
		margin-top: 10rpx;
	}

	/* ========== 存酒区 ========== */
	.c01-wine-row {
		display: flex;
		align-items: flex-start;
		justify-content: space-between;
		margin-top: 24rpx;
	}

	.c01-wine-left {
		display: flex;
		flex-direction: column;
		flex: 1;
	}

	.c01-wine-name {
		font-size: 26rpx;
		color: #333333;
	}

	.c01-wine-meta {
		font-size: 22rpx;
		color: #999999;
		margin-top: 6rpx;
	}

	.c01-wine-tag {
		font-size: 20rpx;
		height: 40rpx;
		line-height: 40rpx;
		padding: 0 12rpx;
		border-radius: 8rpx;
		flex-shrink: 0;
		margin-left: 12rpx;
	}

	.c01-tag-ok {
		background: #E8F5EE;
		color: #43A047;
	}

	.c01-tag-warn {
		background: #FFF7E6;
		color: #E6A23C;
	}

	.c01-tag-off {
		background: #F0F0F0;
		color: #B0B0B0;
	}

	.c01-wine-expire {
		font-size: 22rpx;
		color: #999999;
		margin-top: 12rpx;
	}

	/* 空态：与 .c01-wine-row 同 top 间距，保证有数据/无数据两种形态板块高度不跳 */
	.c01-wine-empty {
		font-size: 24rpx;
		color: #B0B0B0;
		margin-top: 24rpx;
		padding: 16rpx 0;
		text-align: center;
	}

	.c01-wine-note {
		font-size: 20rpx;
		color: #B0B0B0;
		margin-top: 20rpx;
	}
</style>
