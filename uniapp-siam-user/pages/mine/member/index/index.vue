<template>
	<view class="c03">
		<!-- 未登录空态：原实现是「toast + 800ms 后 navigateBack」，用户只看到页面
		     闪一下被弹回 → 误判为「跳转失败」（2026-09-22 实测复现）。
		     改为：停留本页 + 明确引导登录。-->
		<view class="c03-guest" v-if="!loggedIn">
			<view class="c03-guest-title">登录后查看会员权益</view>
			<view class="c03-guest-desc">会员等级、成长值、积分与优惠券将在这里展示</view>
			<view class="c03-guest-btn theme-bg" @tap="goLogin">去登录</view>
		</view>

		<block v-else>
		<!-- 顶部渐变带（画板 5:3 #EDF2DE→白）：头像昵称行 + 等级卡 -->
		<view class="c03-hero">
			<!-- 头部行（画板 5:17/5:26）：头像 56pt + 昵称 15pt；
			     有真实头像显示头像，无头像时用默认头像图（湖里Lakes logo，2026-09-24 用户指定） -->
			<view class="c03-headrow">
				<view class="c03-avatar">
					<image v-if="member.headImg" class="c03-avatar-img" :src="member.headImg" mode="aspectFill"></image>
					<image v-else class="c03-avatar-img" src="/static/assets/images/default-avatar.jpg" mode="aspectFill"></image>
				</view>
				<view class="c03-nickname">{{ member.username || '未登录' }}</view>
			</view>

			<!-- 等级卡（画板 5:27）：横渐变圆角卡 + 右侧竖排等级名 21pt。
			     到期胶囊不渲染：vip_end_time 全库无读写（技术说明 §8.9），无字段不造数 -->
			<view class="c03-level-card" :style="levelCardStyle">
				<view class="c03-level-vertical">
					<view class="c03-level-char" :style="levelCardTextStyle" v-for="(ch, index) in levelChars" :key="index">{{ ch }}</view>
				</view>
				<!-- 工程既成入口：C-08 成长值明细（画板无此链接，功能保留） -->
				<view class="c03-level-growth-link" :style="levelCardTextStyle" @tap="goGrowth">成长值明细 ›</view>
			</view>

			<!-- 等级说明（画板 5:33）：等级表后端无配置（§4.3），先置灰提示 -->
			<view class="c03-level-note" @tap="goLevelNote">等级说明 ›</view>
		</view>

		<!-- 五级刻度轴（画板 5:36~5:57）：阈值/等级名统一走 utils/member-level.js（工程口径
		     0/300/600/1000/2000，画板 0/20/100/300/1000 是参考图残留，拍板 2.2 以工程为准）。
		     growth 为 0/缺失时整块不渲染，避免造数 -->
		<view class="c03-axis" v-if="hasGrowth">
			<view class="c03-axis-track"></view>
			<view class="c03-axis-row">
				<view class="c03-axis-col" v-for="(lv, index) in levels" :key="lv.level">
					<view class="c03-axis-val">{{ lv.min }}</view>
					<view class="c03-axis-dot" :class="{ 'c03-axis-dot-on': lv.level <= level.level }" :style="axisDotStyle(lv)"></view>
					<view class="c03-axis-name">V{{ lv.level }}{{ lv.name }}</view>
				</view>
			</view>
			<view class="c03-axis-tip" v-if="level.nextLevel">
				当前成长值 <text class="c03-axis-tip-num">{{ member.growth }}</text>，距离{{ level.nextName }}只差
				<text class="c03-axis-tip-num">{{ level.gap }}</text>
			</view>
			<view class="c03-axis-tip" v-else>已达最高等级，感谢相伴</view>
		</view>

		<!-- 权益 3×3（画板 5:58/5:89/5:115）：icon 圆 44pt + 限时角标 + 名称/说明两行居中。
		     解锁=主题绿圆白 glyph / 未解锁=浅灰圆灰 glyph（前端按等级现算，方案 C 混合）。
		     文案为 utils/member-benefit.js 占位文案池（正式清单待产品）；
		     堂食85折/专属客服两项暂无图标源，先渲染空圆 -->
		<view class="c03-benefit-grid">
			<view class="c03-benefit-cell" v-for="(item, index) in benefits" :key="item.key">
				<view class="c03-benefit-iconwrap" :class="item.unlocked ? 'c03-benefit-icon-on' : 'c03-benefit-icon-off'">
					<image v-if="item.unlocked && item.icon" class="c03-benefit-glyph" :src="item.icon" mode="aspectFit"></image>
					<image v-else-if="!item.unlocked && item.iconOff" class="c03-benefit-glyph" :src="item.iconOff" mode="aspectFit"></image>
					<view class="c03-benefit-badge" v-if="item.unlocked && item.badge">{{ item.badge }}</view>
				</view>
				<view class="c03-benefit-name" :class="{ 'c03-benefit-name-off': !item.unlocked }">{{ item.title }}</view>
				<view class="c03-benefit-desc">{{ item.unlocked ? item.desc : 'V' + item.unlockLevel + ' 解锁' }}</view>
			</view>
		</view>

		<!-- ===== 以下为工程既成功能区块（画板页2未画出，保留不回退）===== -->
		<view class="c03-legacy">
			<!-- 积分 / 余额卡（画板 298:112）—— 口径 2026-09-24 修正：
			     积分=仅用于会员等级成长；余额=可用于金额支付。
			     后端无独立「积点」字段，此格承载的本来就是 member.balance（账户余额）；
			     此前标签写「绩点」，导致用户以为「前端看不到余额」，故正名并可直接点击进余额页。 -->
			<view class="c03-card">
				<view class="c03-card-head">
					<view class="c03-card-title">我的积分 / 余额</view>
					<view class="c03-card-more" @tap="goIntegral">积分明细 ›</view>
				</view>
				<view class="c03-data-row">
					<view class="c03-data-item">
						<view class="c03-data-value theme-color">{{ member.points || 0 }}</view>
						<view class="c03-data-label">积分（等级成长）</view>
					</view>
					<!-- 余额：点击进余额页（该页此前全工程仅充值页一处入口，实际不可达） -->
					<view class="c03-data-item" @tap="goBalance">
						<view class="c03-data-value theme-color">{{ member.balance || 0 }}</view>
						<view class="c03-data-label">余额（可支付）›</view>
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
		</view>

		</block>

		<!-- 身份认证行（画板 298:137）已按用户要求移除：本页不再展示认证状态入口；
		     认证能力仍保留在「我的」页常用服务 · 身份认证 → pages/mine/security/index/index -->
	</view>
</template>

<script>
	import https from '../../../../utils/http';
	import authService from '../../../../utils/auth';
	import systemStatus from '../../../../utils/system-status';
	import memberLevel from '../../../../utils/member-level';
	import memberBenefit from '../../../../utils/member-benefit';

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
				couponList: [],
				// 登录态（onShow 由 checkIsLogin 确定）。默认 true 避免首屏闪出未登录空态
				loggedIn: true
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
			// 五级刻度轴数据源（正本=member-level.js，验收#1 三页同源）
			levels() {
				return memberLevel.LEVELS;
			},
			// 等级卡配色（2026-09-25 等级差异化）：横渐变底（浅→略深）随等级换，
			// 色值来自 member-level.js 的 card 字段，替换原固定浅绿渐变 #E8F0D9→#C7D9A1。
			levelCardStyle() {
				const c = this.level && this.level.card;
				if (!c) {
					return '';
				}
				return 'background:linear-gradient(90deg, ' + c.from + ' 0%, ' + c.to + ' 100%);';
			},
			levelCardTextStyle() {
				const c = this.level && this.level.card;
				return c ? 'color:' + c.text + ';' : '';
			},
			// 竖排等级名：拆字符逐行渲染（小程序对 writing-mode 支持不稳）
			levelChars() {
				return String(this.levelText).split('');
			},
			levelText() {
				if (this.hasGrowth) {
					return this.level.name;
				}
				return this.member.levelName ? this.member.levelName : systemStatus.typeVipText(this.member.type);
			},
			// 权益 3×3：文案/图标来自 member-benefit.js（占位文案池），
			// 解锁态按当前等级现算（growth=0 → level=1 → 仅 V1 权益亮起，不造数）
			benefits() {
				return memberBenefit.withUnlockState(this.level.level);
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
				this.loggedIn = !!result;
				if (result) {
					this.getMemberInfo();
					this.getCouponList();
					return;
				}
				// 未登录：保留本页并展示未登录空态（模板 v-if="!loggedIn"）。
				// ⚠️ 原实现是 toast 后 800ms 自动 navigateBack —— 用户只看到「闪一下被弹回」，
				//    会误判成「跳转失败」。实测（2026-09-22）：navigateTo 返回 ok，随后
				//    showToast「请先登录」→ navigateBack，路由本身并无问题。
				this.member = {
					id: ''
				};
				this.couponList = [];
			});
		},
		methods: {
			// 会员基础信息：未登录/接口异常时保持空态，不阻断页面渲染
			// 五级刻度轴：已达等级的圆点点亮为「该等级自己」的深色（badge.text），未达保持灰。
			// ⚠️ 必须放 methods 而非 computed —— 它需要按列传参，vue2 的 computed 不支持带参调用。
			axisDotStyle(lv) {
				if (!lv || lv.level > this.level.level) {
					return '';
				}
				return lv.badge ? 'background:' + lv.badge.text + ';' : '';
			},
			getMemberInfo() {
				https.request('/rest/member/getLoginMemberInfo', {}).then((result) => {
					if (result && result.success && result.data) {
						this.member = result.data;
						// 积分/成长值换源（2026-09-27）：growth/points 是 19 号 SQL 种子死数，
						// 真实账在 tb_member_points_log，改用 myPoints.totalPoints 回填
						//（等级判定 levelOf 走 utils/member-level.js，阈值与后端 level_setting 对齐，管线不动）
						this.getMyPoints();
					}
				});
			},
			// 当前积分（=等级成长值）：SUM(积分流水)，与 C-08「我的成长值」同源
			getMyPoints() {
				https.request('/rest/member/points/myPoints', {}).then((result) => {
					if (result && result.success && result.data) {
						const total = Number(result.data.totalPoints) || 0;
						this.member.growth = total;
						this.member.points = total;
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
			// 未登录空态的「去登录」：跳登录方式选择页（E-08b）
			goLogin() {
				uni.navigateTo({
					url: '/pages/internal/login/choose/choose'
				});
			},
			// C-08「我的成长值」（画板 21:660）已落地 → 直接跳转
			goGrowth() {
				uni.navigateTo({
					url: '/pages/mine/growth/index/index'
				});
			},
			// 等级说明（画板 5:33）：等级阈值/规则仍为前端配置（§4.3），无说明页可落 → 提示待定档
			goLevelNote() {
				uni.showToast({
					title: '等级规则即将上线',
					icon: 'none'
				});
			},
			goIntegral() {
				uni.navigateTo({
					url: '/pages/mine/integral/integral'
				});
			},
			// 余额页：本页已由 onShow 保证登录态（未登录渲染空态），故直接跳
			goBalance() {
				uni.navigateTo({
					url: '/pages/mine/balance/index/index'
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
		background: #ffffff;
	}

	.c03 {
		min-height: 100vh;
		background: #ffffff;
		padding-bottom: 40rpx;
	}

	/* ========== 未登录空态（与 C-01 白卡同口径：24rpx 边距 / 24rpx 圆角 / 28rpx 内边距） ========== */
	.c03-guest {
		margin: 80rpx 24rpx 0 24rpx;
		padding: 80rpx 40rpx;
		background: #ffffff;
		border-radius: 24rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
	}

	.c03-guest-title {
		font-size: 32rpx;
		font-weight: 600;
		line-height: 46rpx;
		color: #333333;
	}

	.c03-guest-desc {
		font-size: 24rpx;
		line-height: 36rpx;
		color: #999999;
		margin-top: 12rpx;
		text-align: center;
	}

	.c03-guest-btn {
		margin-top: 48rpx;
		width: 320rpx;
		height: 80rpx;
		line-height: 80rpx;
		text-align: center;
		border-radius: 40rpx;
		font-size: 28rpx;
		color: #ffffff;
	}

	/* ========== 顶部渐变带（画板 5:3）：#EDF2DE → 透明（渐变是唯一无法走主题类的例外） ========== */
	.c03-hero {
		background: linear-gradient(180deg, #EDF2DE 0%, #EDF2DE 55%, rgba(237, 242, 222, 0) 100%);
		padding-bottom: 8rpx;
	}

	/* 头部行（画板 5:17/5:26）：头像 56pt→116rpx + 昵称 15pt→30rpx */
	.c03-headrow {
		display: flex;
		align-items: center;
		padding: 10rpx 20rpx 0 20rpx;
	}

	.c03-avatar {
		width: 116rpx;
		height: 116rpx;
		border-radius: 50%;
		overflow: hidden;
		flex-shrink: 0;
		/* 默认头像图底色（原品牌红圆已由 Lakes logo 默认头像替代，2026-09-24） */
		background: #F5F5F5;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.c03-avatar-img {
		width: 116rpx;
		height: 116rpx;
	}

	.c03-nickname {
		font-size: 30rpx;
		font-weight: 500;
		color: #26301B;
		margin-left: 20rpx;
	}

	/* 等级卡（画板 5:27）：300×124pt → 624×258rpx，横渐变 #E8F0D9→#C7D9A1 */
	.c03-level-card {
		position: relative;
		margin: 34rpx 62rpx 0 62rpx;
		height: 258rpx;
		border-radius: 28rpx;
		/* 画板 5:27 横向渐变（渐变是唯一无法走主题类的例外，色值取自画板 stops） */
		background: linear-gradient(90deg, #E8F0D9 0%, #C7D9A1 100%);
	}

	/* 竖排等级名（画板 5:32）：21pt→44rpx，右侧一字一行 */
	.c03-level-vertical {
		position: absolute;
		top: 38rpx;
		right: 60rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
	}

	.c03-level-char {
		font-size: 44rpx;
		line-height: 46rpx;
		font-weight: 500;
		color: #3F5518;
	}

	/* 工程既成入口：C-08 成长值明细（画板无，功能保留） */
	.c03-level-growth-link {
		position: absolute;
		left: 40rpx;
		bottom: 32rpx;
		font-size: 22rpx;
		color: #516B1D;
	}

	/* 等级说明（画板 5:33）：11pt→22rpx 右对齐 */
	.c03-level-note {
		margin: 20rpx 62rpx 0 62rpx;
		text-align: right;
		font-size: 22rpx;
		color: #8A917F;
	}

	/* ========== 五级刻度轴（画板 5:36~5:57） ========== */
	.c03-axis {
		position: relative;
		margin: 44rpx 62rpx 0 62rpx;
	}

	/* 轴线：2pt→4rpx，两端缩进半列宽让线穿过首尾刻度点中心 */
	.c03-axis-track {
		position: absolute;
		left: 10%;
		right: 10%;
		top: 74rpx;
		height: 4rpx;
		border-radius: 2rpx;
		background: #DFE3D5;
	}

	.c03-axis-row {
		position: relative;
		display: flex;
	}

	.c03-axis-col {
		width: 20%;
		display: flex;
		flex-direction: column;
		align-items: center;
	}

	.c03-axis-val {
		font-size: 22rpx;
		line-height: 30rpx;
		color: #8A917F;
	}

	/* 刻度点：当前 10pt→20rpx 主题绿 / 其余 9pt→18rpx 浅灰（到达过的档位全亮） */
	.c03-axis-dot {
		width: 18rpx;
		height: 18rpx;
		border-radius: 50%;
		background: #E6EADD;
		margin-top: 16rpx;
	}

	.c03-axis-dot-on {
		width: 20rpx;
		height: 20rpx;
		background: #5F7D22;
	}

	.c03-axis-name {
		font-size: 20rpx;
		line-height: 28rpx;
		color: #5A614E;
		margin-top: 14rpx;
		white-space: nowrap;
	}

	.c03-axis-tip {
		margin: 40rpx 20rpx 0 20rpx;
		font-size: 20rpx;
		line-height: 28rpx;
		color: #5A614E;
	}

	.c03-axis-tip-num {
		color: #5F7D22;
		font-weight: 600;
	}

	/* ========== 权益 3×3（画板 5:58/5:89/5:115）：开放列布局，无卡片底 ========== */
	.c03-benefit-grid {
		display: flex;
		flex-wrap: wrap;
		margin: 56rpx 20rpx 0 20rpx;
		row-gap: 48rpx;
	}

	.c03-benefit-cell {
		width: 33.33%;
		display: flex;
		flex-direction: column;
		align-items: center;
	}

	/* icon 圆 44pt→92rpx：解锁=主题绿 / 未解锁=浅灰（画板 icon-wrap / icon-wrap-未解锁） */
	.c03-benefit-iconwrap {
		position: relative;
		width: 92rpx;
		height: 92rpx;
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.c03-benefit-icon-on {
		background: #5F7D22;
	}

	.c03-benefit-icon-off {
		background: #E6EADD;
	}

	.c03-benefit-glyph {
		width: 50rpx;
		height: 50rpx;
	}

	/* 限时角标（画板 badge-每周三 29×13pt→60×27rpx，红 #E23A3A 有意保留） */
	.c03-benefit-badge {
		position: absolute;
		top: -8rpx;
		right: -14rpx;
		height: 27rpx;
		line-height: 27rpx;
		padding: 0 10rpx;
		border-radius: 14rpx;
		background: #E23A3A;
		font-size: 14rpx;
		color: #ffffff;
		white-space: nowrap;
	}

	.c03-benefit-name {
		font-size: 20rpx;
		line-height: 28rpx;
		color: #26301B;
		margin-top: 16rpx;
		text-align: center;
	}

	.c03-benefit-name-off {
		color: #5A614E;
	}

	.c03-benefit-desc {
		font-size: 14rpx;
		line-height: 20rpx;
		color: #9AA38C;
		margin-top: 6rpx;
		text-align: center;
		max-width: 100%;
		overflow: hidden;
		white-space: nowrap;
		text-overflow: ellipsis;
	}

	/* ========== 工程既成区块容器（画板页2未画出，浅灰底与画板区隔） ========== */
	.c03-legacy {
		margin-top: 48rpx;
		padding: 4rpx 0 36rpx 0;
		background: #F4F5F0;
	}

	/* ========== 白卡通用：与 C-01「我的」页同口径（24rpx 边距 / 24rpx 圆角 / 28rpx 内边距 / 20rpx 卡间距） ========== */
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

	/* ========== 积分 / 绩点数据行（画板 298:116） ========== */
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
