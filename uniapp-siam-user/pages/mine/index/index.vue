<template>
	<view class="c01">
		<!-- Hero 渐变绿背景（画板 3:2）：状态栏占位（本页 navigationStyle: custom）+ Lakes 字标 -->
		<view class="c01-hero">
			<view class="c01-status" :style="'height:' + statusBarHeight + 'rpx;'"></view>
			<view class="c01-logo">Lakes</view>
		</view>

		<!-- 会员卡（画板 3:15）：白卡叠入 hero；头像徽标 + 昵称/等级徽章 + 权益入口 + 分隔线 + 升级提示 -->
		<view class="c01-vip-card" v-if="member.id" @tap="goMemberCenter">
			<view class="c01-vip-top">
				<view class="c01-vip-avatar">
					<image v-if="member.headImg" class="c01-vip-avatar-img" :src="member.headImg" mode="aspectFill"></image>
					<view v-else class="c01-vip-avatar-brand">L</view>
				</view>
				<!-- C-02 个人资料入口：点昵称列进编辑页（原顶部用户行已按画板移入会员卡） -->
				<view class="c01-vip-info" @tap.stop="goUserInfo">
					<view class="c01-vip-nickname">{{ member.username || '未设置昵称' }}</view>
					<!-- 徽章行（画板 3:28 红圈「学生」）：等级徽章 + 身份标识并排；
					     身份标识仅在认证中心有已通过结果时显示（loadIdentityBadge） -->
					<view class="c01-vip-badges">
						<view class="c01-vip-badge">{{ vipBadgeText }}</view>
						<view class="c01-vip-badge c01-vip-badge-identity" v-if="identityBadgeText">{{ identityBadgeText }}</view>
					</view>
				</view>
			</view>
			<view class="c01-vip-entry">
				<view class="c01-vip-entry-text">点击查看会员权益</view>
				<view class="c01-vip-entry-arrow">›</view>
			</view>
			<!-- 升级提示行（画板 3:35）：口径=积分仅用于等级成长（2026-09-23 拍板），
			     数值取 growth（工程字段语义为成长值，与 C-08 同源，不造数） -->
			<block v-if="hasGrowth">
				<view class="c01-vip-divider"></view>
				<view class="c01-vip-hint">
					<view class="c01-vip-hint-text" v-if="level.nextLevel">再获得 {{ level.gap }} 成长值可升级为{{ level.nextName }}</view>
					<view class="c01-vip-hint-text" v-else>已是最高等级</view>
					<view class="c01-vip-hint-num" v-if="level.nextLevel">{{ member.growth }}/{{ level.nextMin }}</view>
				</view>
			</block>
		</view>

		<!-- 未登录占位卡：保持与会员卡同位同形，引导登录 -->
		<view class="c01-vip-card" v-else @tap="goUserInfo">
			<view class="c01-vip-top">
				<view class="c01-vip-avatar">
					<view class="c01-vip-avatar-brand">L</view>
				</view>
				<view class="c01-vip-info">
					<view class="c01-vip-nickname">请登录/注册</view>
					<view class="c01-vip-badge">Lakes 会员</view>
				</view>
			</view>
			<view class="c01-vip-entry">
				<view class="c01-vip-entry-text">登录后享会员权益</view>
				<view class="c01-vip-entry-arrow">›</view>
			</view>
		</view>

		<!-- 数据二宫格（画板 3:38）：绩点 + 优惠券（口径 2026-09-23 拍板：绩点=金额支付，沿用 balance） -->
		<view class="c01-data-grid" v-if="member.id">
			<view class="c01-data-card" @tap="goMemberCenter">
				<image class="c01-data-icon" src="/static/assets/icons/data-credit.png" mode="aspectFit"></image>
				<view class="c01-data-value">{{ member.balance || 0 }}</view>
				<view class="c01-data-label">绩点</view>
			</view>
			<view class="c01-data-card" @tap="goCoupons">
				<image class="c01-data-icon" src="/static/assets/icons/data-coupon.png" mode="aspectFit"></image>
				<view class="c01-data-value">{{ member.couponsNumber || 0 }}</view>
				<view class="c01-data-label">优惠券</view>
			</view>
		</view>

		<!-- 已解锁权益横滑（画板 3:62）：数据源=member-benefit.js 解锁项（方案 C 混合，前端现算） -->
		<view class="c01-strip" v-if="member.id && unlockedBenefits.length">
			<view class="c01-strip-head">
				<view class="c01-strip-title">已解锁{{ unlockedBenefits.length }}项权益</view>
				<view class="c01-strip-more" @tap="goMemberCenter">全部权益 ›</view>
			</view>
			<scroll-view class="c01-strip-scroll" scroll-x="true">
				<view class="c01-strip-item" v-for="(item, index) in unlockedBenefits" :key="item.key">
					<view class="c01-strip-iconwrap">
						<image v-if="item.icon" class="c01-strip-glyph" :src="item.icon" mode="aspectFit"></image>
						<view class="c01-strip-badge" v-if="item.badge">{{ item.badge }}</view>
					</view>
					<view class="c01-strip-name">{{ item.title }}</view>
					<view class="c01-strip-sub">{{ item.desc }}</view>
				</view>
			</scroll-view>
		</view>

		<!-- 「我的订单」入口卡已按用户要求移除（2026-09-23）：订单中心在底部 tabBar 有常驻入口，页面内再放一份冗余 -->

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

		<!-- 我的功能（画板 3:128）：身份认证 / 公告 两项（画板同宽 4 格，后 2 格为空占位） -->
		<view class="c01-block">
			<view class="c01-card c01-fn-card">
				<view class="c01-fn-title">我的功能</view>
				<view class="c01-fn-row">
					<view class="c01-fn-item" v-for="(item, index) in services" :key="index" @tap="goService(item)">
						<image class="c01-fn-icon" :src="item.icon" mode="aspectFit"></image>
						<view class="c01-fn-label">{{ item.name }}</view>
					</view>
					<view class="c01-fn-item"></view>
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
	import memberBenefit from '../../../utils/member-benefit';
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
				// 「我的订单」入口卡已移除（tabBar 有订单中心常驻入口），orderEntries/goOrderCenter 一并清理
				// 我的功能（画板 3:128）：2 项；优惠券入口移至数据二宫格的券卡
				services: [{
						name: '身份认证',
						icon: '/static/assets/icons/mine-identity.png',
						url: '/pages/mine/identityAuth/index'
					},
					{
						name: '公告',
						icon: '/static/assets/icons/mine-notice.png',
						url: '/pages/notice/list/list'
					},
					{
						// 领券中心（画板 C-18）：用户 2026-09-24 红圈指定第三格
						// ⚠️ 正本改动：customer-cli/src 由本文件 sync 生成，勿直改 src
						name: '领券中心',
						icon: '/static/assets/icons/mine-coupon.png',
						url: '/pages/mine/coupons/receive'
					}
				],
				wineList: [],
				// 身份标识（画板 3:28 红圈「学生」）：'' = 不显示；
				// 阶段二通过 → 学生/教师（identityType），仅阶段一通过 → 已实名
				identityBadgeText: ''
			};
		},
		computed: {
			level() {
				return memberLevel.levelOf(this.member.growth);
			},
			hasGrowth() {
				return Number(this.member.growth) > 0;
			},
			// 等级徽章（画板 3:28「V1普通会员」）：有成长值走等级表（工程口径 青铜~钻石），
			// 否则退回 type 文案（1 普通用户 / 2 VIP会员），不带 V 前缀
			vipBadgeText() {
				if (this.hasGrowth) {
					return 'V' + this.level.level + this.level.name;
				}
				return this.member.levelName ? this.member.levelName : systemStatus.typeVipText(this.member.type);
			},
			// 已解锁权益（画板 3:62 横滑）：member-benefit.js 按等级现算后取解锁项
			unlockedBenefits() {
				return memberBenefit.withUnlockState(this.level.level).filter((item) => item.unlocked);
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
					// 三个接口互不依赖，并行发起：任一个失败都不影响另一个。
					// 原先存酒取数嵌在「会员信息成功」分支里，会员接口一次抖动
					// 就会连带整块存酒区不渲染（表现为「板块没实现」）
					this.getMemberInfo();
					this.loadWineList();
					this.loadIdentityBadge();
					return;
				}
				this.member = {
					id: ''
				};
				this.wineList = [];
				this.identityBadgeText = '';
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
			// 身份标识（画板 3:28 红圈「学生」）：复用认证中心的 detail 接口，与会员信息/存酒并行、
			// 任一失败互不影响。auditStatus：0=未提交 1=待审核 2=通过 3=驳回。
			// 阶段二（学生/教师）通过 → 按 identityType 显示「学生/教师」；
			// 仅阶段一（实名）通过 → 「已实名」；都没通过/接口异常 → ''（不渲染徽标）
			loadIdentityBadge() {
				https.request('/rest/member/identityAuth/detail', {}).then((result) => {
					let text = '';
					if (result && result.success && result.data) {
						const data = result.data;
						const stage1Pass = Number(data.stage1Status) === 2;
						const stage2Pass = Number(data.stage2Status) === 2;
						if (stage2Pass && data.identityAuth) {
							// authType=2 时 identityType 才有值：1=学生 2=教师
							text = Number(data.identityAuth.identityType) === 2 ? '教师' : '学生';
						} else if (stage1Pass) {
							text = '已实名';
						}
					}
					this.identityBadgeText = text;
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
			// C-02 个人资料（画板 21:191）：点会员卡昵称列进入编辑页。
			// 未登录时沿用全站统一引导（App.vue checkIsAuth），不直接 push 空白页
			goUserInfo() {
				if (!this.member.id) {
					app.globalData.checkIsAuth('scope.userInfo');
					return;
				}
				uni.navigateTo({
					url: '/pages/mine/userinfo/userinfo'
				});
			},
			goMemberCenter() {
				uni.navigateTo({
					url: '/pages/mine/member/index/index'
				});
			},
			goCoupons() {
				uni.navigateTo({
					url: '/pages/mine/coupons/coupons'
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
			}
		}
	};
</script>

<style>
	page {
		background: #F4F5F0;
	}

	.c01 {
		min-height: 100vh;
		background: #F4F5F0;
		/* 底部留出 tabBar 高度 */
		padding-bottom: calc(140rpx + constant(safe-area-inset-bottom));
		padding-bottom: calc(140rpx + env(safe-area-inset-bottom));
	}

	/* ========== Hero 渐变绿背景（画板 3:2 原始 stops 偏浅，2026-09-23 用户反馈「太浅」已加深）：
	     #7DA138（画板最深端）→ #5F7D22（主题主色）→ #516B1D（主题深阶）
	     （渐变是唯一无法走主题类的例外，色值来源见此） ========== */
	.c01-hero {
		box-sizing: border-box;
		height: 452rpx;
		background: linear-gradient(135deg, #7DA138 0%, #5F7D22 55%, #516B1D 100%);
	}

	.c01-status {
		width: 100%;
	}

	/* Lakes 字标（画板 3:13）：48pt→100rpx Bold；hero 加深后由画布深绿翻白保证对比度 */
	.c01-logo {
		margin-top: 54rpx;
		text-align: center;
		font-size: 100rpx;
		line-height: 120rpx;
		font-weight: 700;
		color: #ffffff;
	}

	/* ========== 会员卡（画板 3:15）：344pt 宽白卡，上叠 hero 43pt ========== */
	.c01-vip-card {
		margin: -90rpx 16rpx 0 16rpx;
		padding: 32rpx;
		background: #ffffff;
		border-radius: 32rpx;
		/* 画板 3:15 投影 0 2 10 rgba(140,128,102,0.10) */
		box-shadow: 0 4rpx 20rpx rgba(140, 128, 102, 0.10);
		position: relative;
		z-index: 2;
	}

	.c01-vip-top {
		display: flex;
		align-items: center;
	}

	/* 头像徽标（画板 3:16）：60pt→124rpx 圆，品牌红 #C8102E 有意保留；有真实头像显示头像 */
	.c01-vip-avatar {
		width: 124rpx;
		height: 124rpx;
		border-radius: 50%;
		background: #C8102E;
		overflow: hidden;
		flex-shrink: 0;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.c01-vip-avatar-img {
		width: 124rpx;
		height: 124rpx;
	}

	.c01-vip-avatar-brand {
		font-size: 56rpx;
		font-weight: 700;
		color: #ffffff;
	}

	.c01-vip-info {
		margin-left: 20rpx;
		display: flex;
		flex-direction: column;
		align-items: flex-start;
	}

	.c01-vip-nickname {
		font-size: 26rpx;
		font-weight: 600;
		color: #26301B;
	}

	/* 等级徽章（画板 3:27/3:28）：主题绿底白字 10pt→20rpx */
	.c01-vip-badge {
		margin-top: 14rpx;
		height: 44rpx;
		line-height: 44rpx;
		padding: 0 20rpx;
		border-radius: 8rpx;
		background: #5F7D22;
		font-size: 20rpx;
		color: #ffffff;
	}

	/* 徽章行（画板 3:28）：等级徽章 + 身份标识并排；徽章自带上边距，行本身不加 */
	.c01-vip-badges {
		display: flex;
		flex-direction: row;
		align-items: center;
	}

	/* 身份标识（画板 3:28 红圈「学生」）：白底绿字绿描边，与主题绿等级徽章并排不抢视觉 */
	.c01-vip-badge-identity {
		margin-left: 12rpx;
		background: #ffffff;
		color: #5F7D22;
		border: 2rpx solid #5F7D22;
	}

	/* 权益入口行（画板 3:30）：11pt→22rpx */
	.c01-vip-entry {
		display: flex;
		align-items: center;
		margin-top: 26rpx;
	}

	.c01-vip-entry-text {
		font-size: 22rpx;
		color: #5A614E;
	}

	.c01-vip-entry-arrow {
		font-size: 22rpx;
		color: #8A917F;
		margin-left: 6rpx;
	}

	/* 分隔线（画板 3:34）+ 升级提示行（画板 3:35） */
	.c01-vip-divider {
		height: 2rpx;
		background: #E3E6DA;
		margin: 20rpx 0;
	}

	.c01-vip-hint {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.c01-vip-hint-text {
		font-size: 22rpx;
		color: #8A917F;
	}

	.c01-vip-hint-num {
		font-size: 22rpx;
		color: #8A917F;
	}

	/* ========== 数据二宫格（画板 3:38）：白卡 r12pt，icon 26pt + 数值 16pt + 标签 9pt ========== */
	.c01-data-grid {
		display: flex;
		margin: 24rpx 16rpx 0 16rpx;
	}

	.c01-data-card {
		flex: 1;
		background: #ffffff;
		border-radius: 24rpx;
		padding: 24rpx;
		display: flex;
		flex-direction: column;
		align-items: flex-start;
	}

	.c01-data-card + .c01-data-card {
		margin-left: 20rpx;
	}

	.c01-data-icon {
		width: 52rpx;
		height: 52rpx;
	}

	.c01-data-value {
		font-size: 32rpx;
		font-weight: 600;
		color: #26301B;
		margin-top: 8rpx;
	}

	.c01-data-label {
		font-size: 18rpx;
		color: #5A614E;
		margin-top: 6rpx;
	}

	/* ========== 已解锁权益横滑（画板 3:62）：标题 13pt Bold + 项（icon 圆 34pt + 名 8pt + 副标 7pt） ========== */
	.c01-strip {
		margin: 24rpx 16rpx 0 16rpx;
		padding: 32rpx 32rpx 28rpx 32rpx;
		background: #ffffff;
		border-radius: 32rpx;
	}

	.c01-strip-head {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.c01-strip-title {
		font-size: 26rpx;
		font-weight: 700;
		color: #26301B;
	}

	.c01-strip-more {
		font-size: 18rpx;
		color: #8A917F;
	}

	.c01-strip-scroll {
		margin-top: 24rpx;
		white-space: nowrap;
		width: 100%;
	}

	.c01-strip-item {
		display: inline-block;
		width: 130rpx;
		vertical-align: top;
		text-align: center;
	}

	.c01-strip-iconwrap {
		position: relative;
		width: 70rpx;
		height: 70rpx;
		border-radius: 50%;
		background: #5F7D22;
		margin: 0 auto;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.c01-strip-glyph {
		width: 42rpx;
		height: 42rpx;
	}

	/* 限时角标（画板 badge-每周三 30×12pt，红 #E23A3A 有意保留） */
	.c01-strip-badge {
		position: absolute;
		top: -8rpx;
		right: -12rpx;
		height: 25rpx;
		line-height: 25rpx;
		padding: 0 8rpx;
		border-radius: 13rpx;
		background: #E23A3A;
		font-size: 14rpx;
		color: #ffffff;
		white-space: nowrap;
	}

	.c01-strip-name {
		font-size: 16rpx;
		line-height: 20rpx;
		color: #26301B;
		margin-top: 12rpx;
		overflow: hidden;
		white-space: nowrap;
		text-overflow: ellipsis;
	}

	.c01-strip-sub {
		font-size: 14rpx;
		line-height: 18rpx;
		color: #A8AFA0;
		margin-top: 4rpx;
		overflow: hidden;
		white-space: nowrap;
		text-overflow: ellipsis;
	}

	/* ========== 卡片通用 ========== */
	.c01-block {
		margin: 24rpx 16rpx 0 16rpx;
	}

	.c01-card {
		background: #ffffff;
		border-radius: 32rpx;
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

	/* ========== 我的功能（画板 3:128）：标题 13pt Bold + 4 列格（icon 26pt + 名 10pt） ========== */
	.c01-fn-card {
		padding: 32rpx;
	}

	.c01-fn-title {
		font-size: 26rpx;
		font-weight: 700;
		color: #26301B;
	}

	.c01-fn-row {
		display: flex;
		margin-top: 32rpx;
	}

	.c01-fn-item {
		width: 25%;
		display: flex;
		flex-direction: column;
		align-items: center;
		min-height: 100rpx;
	}

	.c01-fn-icon {
		width: 52rpx;
		height: 52rpx;
	}

	.c01-fn-label {
		font-size: 20rpx;
		line-height: 28rpx;
		color: #5A614E;
		margin-top: 16rpx;
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
