<template>
	<view class="c08">
		<!-- 顶部：导航栏与资料头同属一个主题色块（画板 21:660 蓝块 → 工程绿 .theme-bg） -->
		<view class="c08-head theme-bg">
			<view class="c08-status" :style="'height:' + statusBarHeight + 'rpx;'"></view>
			<view class="c08-nav">
				<view class="c08-nav-back" @tap="goBack">
					<text class="c08-nav-arrow"></text>
				</view>
				<view class="c08-nav-title">我的成长值</view>
				<view class="c08-nav-back"></view>
			</view>

			<view class="c08-hero">
				<view class="c08-hero-label">当前成长值</view>
				<view class="c08-hero-row">
					<view class="c08-hero-value">{{ growth }}</view>
					<view class="c08-level-tag" :style="levelTagStyle">{{ levelTag }}</view>
				</view>
				<view class="c08-track">
					<view class="c08-track-bar" :style="'width:' + level.percent + '%'"></view>
				</view>
				<view class="c08-hero-tip" v-if="level.nextLevel">距 {{ 'V' + level.nextLevel }} 还 {{ level.gap }}</view>
				<view class="c08-hero-tip" v-else>已是最高等级</view>
			</view>

			<view class="c08-summary">
				<view class="c08-summary-item">
				<view class="c08-summary-value">{{ summaryTotal }}</view>
				<view class="c08-summary-label">累计成长值</view>
				</view>
				<view class="c08-summary-item">
					<view class="c08-summary-value">{{ summary.consume }}</view>
					<view class="c08-summary-label">消费获得</view>
				</view>
				<view class="c08-summary-item">
					<view class="c08-summary-value">{{ summary.today }}</view>
					<view class="c08-summary-label">今日获得</view>
				</view>
			</view>
		</view>

		<!-- 明细列表 -->
		<view class="c08-list">
			<view class="c08-card" v-for="(item, index) in list" :key="index">
				<view class="c08-card-left">
					<view class="c08-card-title">{{ item.title }}</view>
					<view class="c08-card-time">{{ item.timeText }}</view>
				</view>
				<view :class="'c08-card-value ' + (item.isAdd ? 'theme-color' : 'c08-minus')">{{ item.valueText }}</view>
			</view>
			<view class="c08-empty" v-if="!list.length">暂无成长值记录</view>
		</view>
	</view>
</template>

<script>
	import https from '../../../../utils/http';
	import authService from '../../../../utils/auth';
	import memberLevel from '../../../../utils/member-level';

	let app = null;

	export default {
		data() {
			return {
				statusBarHeight: 40,
				growth: 0,
				// 累计成长值 = 积分流水净额（myPoints.totalPoints，真累计，不受明细分页窗口限制）
				summaryTotal: 0,
				summary: {
					consume: 0,
					today: 0
				},
				list: []
			};
		},
		computed: {
			level() {
				return memberLevel.levelOf(this.growth);
			},
			levelTag() {
				return 'V' + this.level.level;
			},
			// 等级胶囊配色（2026-09-25 等级差异化）：随等级换浅底深字，
			// 替换原固定金色（#F0DCAB 底 / #6E4E1F 字），与 C-01 徽章、C-03 等级卡同一套色源。
			levelTagStyle() {
				const b = this.level && this.level.badge;
				return b ? 'background:' + b.bg + ';color:' + b.text + ';' : '';
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
					this.getMemberInfo();
					this.getGrowthList();
					return;
				}
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
			// 当前成长值：改读积分流水账本（2026-09-27 换源）。
			// 旧源 getLoginMemberInfo.growth 是 19 号 SQL 的种子死数（后端无写入点）；
			// 真实入账在 tb_member_points_log（支付/充值/退款红冲，package_points 模块），
			// myPoints.totalPoints = SUM(change_points)，与等级阈值表同源。
			getMemberInfo() {
				https.request('/rest/member/points/myPoints', {}).then((result) => {
					if (result && result.success && result.data) {
						this.growth = Number(result.data.totalPoints) || 0;
						// 累计成长值直接用流水净额（真累计）；明细接口只负责两个派生格
						this.summaryTotal = this.growth;
					}
				});
			},
			// 成长值明细：改读积分流水分页（2026-09-27 换源）。
			// 旧源 billingRecord/list?coinType=6 只有种子；biz_type 1=消费 2=活动 3=充值 4=退款红冲 5=后台调整
			// ⚠️ https.request 封装是 POST+JSON body（参数不拼 query），而 myLog 是 @RequestParam
			//    读 query —— 分页参数必须拼在 URL 上，放 body 会被吞掉走默认 20 条；
			//    pageSize=-1 = MyBatis-Plus 不分页全量（与旧 billing 调用 pageNo:-1 同口径）
			getGrowthList() {
				https.request('/rest/member/points/myLog?pageNo=1&pageSize=-1', {}).then((result) => {
					if (!result || !result.success || !result.data || !result.data.records) {
						this.list = [];
						this.summary = {
							consume: 0,
							today: 0
						};
						return;
					}
					const todayKey = this.todayKey();
					// total 格不用这里统计——累计成长值=流水净额，走 myPoints（真累计）
					const summary = {
						consume: 0,
						today: 0
					};
					this.list = result.data.records.map((raw) => {
						// MyBatis-Plus 驼峰为主，退回下划线兜底（与旧 normalize 同策略）
						const rec = this.normalize(raw);
						const num = Math.abs(Number(rec.changePoints) || 0);
						const isAdd = Number(rec.changePoints) > 0;
						if (isAdd) {
							if (Number(rec.bizType) === 1) {
								// biz_type 1=消费（对应旧 coinType=6 流水的 type 23）
								summary.consume += num;
							}
							if (this.dateKey(rec.createTime) === todayKey) {
								summary.today += num;
							}
						}
						return {
							title: rec.remark || this.bizTitle(rec.bizType),
							timeText: this.formatTime(rec.createTime),
							valueText: (isAdd ? '+' : '-') + num,
							isAdd: isAdd
						};
					});
					this.summary = summary;
				});
			},
			normalize(raw) {
				const pick = (camel, snake) => (raw[camel] !== undefined ? raw[camel] : raw[snake]);
				return {
					bizType: pick('bizType', 'biz_type'),
					changePoints: pick('changePoints', 'change_points'),
					remark: pick('remark', 'remark'),
					createTime: pick('createTime', 'create_time')
				};
			},
			// 流水标题：后端 earnByAmount 的 remark 为空（商品名未带），按 biz_type 出通用文案，
			// 与 19 号种子文案口径一致（1消费/2活动/3充值/4退款红冲/5后台调整）
			bizTitle(bizType) {
				const map = {
					1: '消费获得成长值',
					2: '活动赠送成长值',
					3: '充值获得成长值',
					4: '退款扣回成长值',
					5: '积分调整'
				};
				return map[Number(bizType)] || '成长值变动';
			},
			todayKey() {
				return this.dateKey(new Date());
			},
			pad2(n) {
				const s = String(n);
				return s.length < 2 ? '0' + s : s;
			},
			// 后端 Date 序列化成 "2026/09/05 22:31:06"（斜杠、月日不补零），
			// 直接 substring(0,10) 与 "YYYY-MM-DD" 比会永远不相等 → 这里统一归一化
			formatTime(value) {
				if (!value) {
					return '';
				}
				let s = String(value).trim().replace('T', ' ');
				s = s.replace(/\//g, '-');
				s = s.replace(/^(\d{4})-(\d{1,2})-(\d{1,2})/, (m, y, mo, d) => y + '-' + this.pad2(mo) + '-' + this.pad2(d));
				return s.substring(0, 19);
			},
			dateKey(value) {
				if (value instanceof Date) {
					return value.getFullYear() + '-' + this.pad2(value.getMonth() + 1) + '-' + this.pad2(value.getDate());
				}
				return this.formatTime(value).substring(0, 10);
			},
			goBack() {
				uni.navigateBack();
			}
		}
	};
</script>

<style>
	page {
		background: #F5F5F5;
	}

	.c08 {
		min-height: 100vh;
		background: #F5F5F5;
		padding-bottom: 40rpx;
	}

	/* ========== 顶部色块（导航 + 资料头一体） ========== */
	.c08-head {
		padding-bottom: 44rpx;
	}

	.c08-nav {
		height: 88rpx;
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 0 24rpx;
	}

	.c08-nav-back {
		width: 60rpx;
		height: 60rpx;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.c08-nav-arrow {
		width: 20rpx;
		height: 20rpx;
		border-left: 4rpx solid #ffffff;
		border-bottom: 4rpx solid #ffffff;
		transform: rotate(45deg);
	}

	.c08-nav-title {
		font-size: 34rpx;
		color: #ffffff;
		font-weight: 500;
	}

	.c08-hero {
		padding: 8rpx 48rpx 0 48rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
	}

	.c08-hero-label {
		font-size: 24rpx;
		color: #ffffff;
		opacity: 0.9;
	}

	.c08-hero-row {
		display: flex;
		align-items: center;
		margin-top: 12rpx;
	}

	.c08-hero-value {
		font-size: 72rpx;
		font-weight: 700;
		color: #ffffff;
		line-height: 88rpx;
	}

	/* 等级胶囊：底 #F0DCAB / 字 #6E4E1F（画板 21:660 的金色 V3 标） */
	.c08-level-tag {
		margin-left: 12rpx;
		padding: 4rpx 14rpx;
		border-radius: 8rpx;
		background: #F0DCAB;
		color: #6E4E1F;
		font-size: 22rpx;
		font-weight: 700;
	}

	.c08-track {
		width: 100%;
		height: 12rpx;
		border-radius: 6rpx;
		background: rgba(255, 255, 255, 0.3);
		overflow: hidden;
		margin-top: 20rpx;
	}

	.c08-track-bar {
		height: 12rpx;
		border-radius: 6rpx;
		background: #F0DCAB;
	}

	.c08-hero-tip {
		font-size: 22rpx;
		color: #ffffff;
		opacity: 0.9;
		margin-top: 16rpx;
	}

	.c08-summary {
		display: flex;
		margin-top: 32rpx;
	}

	.c08-summary-item {
		flex: 1;
		display: flex;
		flex-direction: column;
		align-items: center;
	}

	.c08-summary-value {
		font-size: 44rpx;
		font-weight: 700;
		color: #ffffff;
		line-height: 56rpx;
	}

	.c08-summary-label {
		font-size: 18rpx;
		color: #ffffff;
		opacity: 0.85;
		margin-top: 6rpx;
	}

	/* ========== 明细列表 ========== */
	.c08-list {
		padding: 20rpx 0 0 0;
	}

	.c08-card {
		margin: 0 20rpx 20rpx 20rpx;
		padding: 28rpx;
		background: #ffffff;
		border-radius: 16rpx;
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.c08-card-left {
		flex: 1;
		display: flex;
		flex-direction: column;
	}

	.c08-card-title {
		font-size: 32rpx;
		color: #333333;
	}

	.c08-card-time {
		font-size: 24rpx;
		color: #BCBCBC;
		margin-top: 10rpx;
	}

	.c08-card-value {
		font-size: 30rpx;
		font-weight: 500;
		margin-left: 16rpx;
		flex-shrink: 0;
	}

	/* 获得走主题色（画板 +20 是蓝色 → 全局 .theme-color）；扣回为灰（画板 -15） */
	.c08-minus {
		color: #999999;
	}

	.c08-empty {
		font-size: 26rpx;
		color: #B0B0B0;
		text-align: center;
		padding: 80rpx 0;
	}
</style>
