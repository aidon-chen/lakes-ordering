<template>
	<section>
		<!--顶部：返回 + 会员摘要-->
		<el-col :span="24" class="toolbar">
			<el-button size="small" @click="goBack">返回列表</el-button>
			<span class="detail-title" v-if="member">
				{{ member.nickname || '（未设置昵称）' }}
				<span class="detail-sub">{{ member.mobile || '-' }}</span>
			</span>
		</el-col>

		<!--基本信息-->
		<el-card shadow="never" class="block" v-loading="memberLoading">
			<div slot="header" class="block-title">基本信息</div>
			<el-row v-if="member">
				<el-col :span="8">
					<div class="kv"><span class="k">会员ID</span><span class="v">{{ member.id }}</span></div>
				</el-col>
				<el-col :span="8">
					<div class="kv"><span class="k">昵称</span><span class="v">{{ member.nickname || '-' }}</span></div>
				</el-col>
				<el-col :span="8">
					<div class="kv"><span class="k">手机号</span><span class="v">{{ member.mobile || '-' }}</span></div>
				</el-col>
				<el-col :span="8">
					<div class="kv"><span class="k">用户名</span><span class="v">{{ member.username || '-' }}</span></div>
				</el-col>
				<el-col :span="8">
					<div class="kv"><span class="k">会员等级</span>
						<span class="v">
							<span class="level-tag">{{ levelTag(member.growth) }}</span>{{ levelName(member.growth) }}
						</span>
					</div>
				</el-col>
				<el-col :span="8">
					<div class="kv"><span class="k">成长值</span><span class="v">{{ num(member.growth) }}</span></div>
				</el-col>
				<el-col :span="8">
					<div class="kv"><span class="k">余额</span><span class="v">{{ money(member.balance) }}</span></div>
				</el-col>
				<el-col :span="8">
					<div class="kv"><span class="k">积分</span><span class="v">{{ num(member.points) }}</span></div>
				</el-col>
				<el-col :span="8">
					<div class="kv"><span class="k">实名姓名</span><span class="v">{{ member.realName || '未实名' }}</span></div>
				</el-col>
				<el-col :span="8">
					<div class="kv"><span class="k">账号状态</span>
						<span class="v" v-if="member.isDisabled" style="color:#E25A2B">已禁用</span>
						<span class="v" v-else style="color:#43A047">正常</span>
					</div>
				</el-col>
				<el-col :span="8">
					<div class="kv"><span class="k">注册时间</span><span class="v">{{ formatTimeValue(member.createTime) }}</span></div>
				</el-col>
				<el-col :span="8">
					<div class="kv"><span class="k">最近登录</span><span class="v">{{ formatTimeValue(member.lastLoginTime) }}</span></div>
				</el-col>
			</el-row>
			<div v-else class="empty">未获取到会员信息（会员ID：{{ memberId }}）</div>
		</el-card>

		<!--切换栏卡片：订单记录 / 优惠券 / 存酒 / 余额（Ardot 原型 84:1）-->
		<el-card shadow="never" class="block">
			<el-tabs v-model="activeTab" @tab-click="handleTabClick">
				<!--Tab1 订单记录-->
				<el-tab-pane label="订单记录" name="order">
					<el-table :data="orders" v-loading="orderLoading" style="width: 100%;"
						:cell-style="cellStyle" :header-cell-style="headerCellStyle">
						<el-table-column prop="orderNo" label="订单号" width="190"></el-table-column>
						<el-table-column label="类型" width="80">
							<template slot-scope="scope">
								<span>{{ scope.row.orderScene == 2 ? '自提' : '堂食' }}</span>
							</template>
						</el-table-column>
						<el-table-column label="取餐号 / 桌号" width="130">
							<template slot-scope="scope">
								<span>{{ scope.row.orderScene == 2 ? (scope.row.queueNo || '-') : (scope.row.tableNo || '-') }}</span>
							</template>
						</el-table-column>
						<el-table-column label="实付款" width="110">
							<template slot-scope="scope">
								<span>{{ money(scope.row.actualPrice) }}</span>
							</template>
						</el-table-column>
						<el-table-column label="支付方式" width="110">
							<template slot-scope="scope">
								<span v-if="scope.row.paymentMode == 1">微信支付</span>
								<span v-else-if="scope.row.paymentMode == 2">支付宝支付</span>
								<span v-else-if="scope.row.paymentMode == 4">到店支付</span>
								<span v-else-if="scope.row.paymentMode == 3 || scope.row.paymentMode == 0">平台余额</span>
								<span v-else>-</span>
							</template>
						</el-table-column>
						<el-table-column label="订单状态" width="120">
							<template slot-scope="scope">
								<span>{{ orderStatusText(scope.row.status) }}</span>
							</template>
						</el-table-column>
						<el-table-column prop="createTime" label="下单时间" :formatter="formatTime" width="160"></el-table-column>
					</el-table>
					<el-col :span="24" class="toolbar">
						<el-pagination @size-change="handleOrderSizeChange" @current-change="handleOrderCurrentChange"
							:page-sizes="[10, 20, 50]" :page-size="orderQuery.pageSize"
							layout="total, sizes, prev, pager, next" :total="orderTotal" style="float:right;">
						</el-pagination>
					</el-col>
				</el-tab-pane>

				<!--Tab2 优惠券-->
				<el-tab-pane label="优惠券" name="coupon">
					<el-table :data="coupons" v-loading="couponLoading" style="width: 100%;"
						:cell-style="cellStyle" :header-cell-style="headerCellStyle">
						<el-table-column label="优惠券名称" min-width="160">
							<template slot-scope="scope">
								<span>{{ scope.row.couponsName || '-' }}</span>
							</template>
						</el-table-column>
						<el-table-column label="类型" width="90">
							<template slot-scope="scope">
								<span>{{ scope.row.preferentialType == 1 ? '折扣' : '满减' }}</span>
							</template>
						</el-table-column>
						<el-table-column label="优惠力度" width="200">
							<template slot-scope="scope">
								<span>{{ couponBenefit(scope.row) }}</span>
							</template>
						</el-table-column>
						<el-table-column label="有效期" width="300">
							<template slot-scope="scope">
								<span>{{ formatTimeValue(scope.row.startTime) }} ~ {{ formatTimeValue(scope.row.endTime) }}</span>
							</template>
						</el-table-column>
						<el-table-column label="状态" width="100">
							<template slot-scope="scope">
								<span :class="couponStatusClass(scope.row)">{{ couponStatusText(scope.row) }}</span>
							</template>
						</el-table-column>
					</el-table>
					<el-col :span="24" class="toolbar">
						<el-pagination @size-change="handleCouponSizeChange" @current-change="handleCouponCurrentChange"
							:page-sizes="[10, 20, 50]" :page-size="couponQuery.pageSize"
							layout="total, sizes, prev, pager, next" :total="couponTotal" style="float:right;">
						</el-pagination>
					</el-col>
				</el-tab-pane>

				<!--Tab3 存酒-->
				<el-tab-pane label="存酒" name="wine">
					<el-table :data="wines" v-loading="wineLoading" style="width: 100%;"
						:cell-style="cellStyle" :header-cell-style="headerCellStyle">
						<el-table-column label="品名" min-width="150">
							<template slot-scope="scope">
								<span>{{ scope.row.goodsName || '-' }}</span>
							</template>
						</el-table-column>
						<el-table-column label="存入瓶数" width="100">
							<template slot-scope="scope">
								<span>{{ num(scope.row.bottleCount) }}</span>
							</template>
						</el-table-column>
						<el-table-column label="剩余瓶数" width="100">
							<template slot-scope="scope">
								<span>{{ num(scope.row.remainCount) }}</span>
							</template>
						</el-table-column>
						<el-table-column label="存放柜位" width="140">
							<template slot-scope="scope">
								<span>{{ scope.row.location || '-' }}</span>
							</template>
						</el-table-column>
						<el-table-column label="存入日期" width="130">
							<template slot-scope="scope">
								<span>{{ formatDay(scope.row.storageDate) }}</span>
							</template>
						</el-table-column>
						<el-table-column label="有效期至" width="130">
							<template slot-scope="scope">
								<span>{{ formatDay(scope.row.expireDate) }}</span>
							</template>
						</el-table-column>
						<el-table-column label="状态" width="100">
							<template slot-scope="scope">
								<span :class="wineStatusClass(scope.row)">{{ wineStatusText(scope.row) }}</span>
							</template>
						</el-table-column>
					</el-table>
				</el-tab-pane>

				<!--Tab4 余额-->
				<el-tab-pane label="余额" name="balance">
					<div class="bal-box" v-if="member">
						<div class="bal-main">
							<div class="bl">当前余额</div>
							<div class="bv">{{ money(member.balance) }}</div>
						</div>
						<div class="bal-metrics">
							<div class="metric">
								<div class="ml">累计消费</div>
								<div class="mv">{{ money(member.totalConsumeBalance) }}</div>
							</div>
							<div class="metric">
								<div class="ml">奖励金余额</div>
								<div class="mv">{{ money(rewardBalance) }}</div>
							</div>
							<div class="metric">
								<div class="ml">待领取奖励金</div>
								<div class="mv">{{ money(member.unreceivedInviteRewardAmount) }}</div>
							</div>
						</div>
					</div>
					<div v-else class="empty">未获取到会员信息</div>
				</el-tab-pane>
			</el-tabs>
		</el-card>
	</section>
</template>

<script>
	// 会员等级规则：与顾客端 utils/member-level.js 保持同一口径（后端无等级表）
	const MEMBER_LEVELS = [{
			level: 1,
			name: '青铜会员',
			min: 0
		},
		{
			level: 2,
			name: '白银会员',
			min: 300
		},
		{
			level: 3,
			name: '黄金会员',
			min: 600
		},
		{
			level: 4,
			name: '铂金会员',
			min: 1000
		},
		{
			level: 5,
			name: '钻石会员',
			min: 2000
		}
	];

	export default {
		data() {
			return {
				memberId: '',
				member: null,
				memberLoading: false,
				activeTab: 'order',
				// 订单
				orders: [],
				orderTotal: 0,
				orderLoading: false,
				orderQuery: {
					pageNo: 1,
					pageSize: 10
				},
				// 优惠券
				coupons: [],
				couponTotal: 0,
				couponLoading: false,
				couponQuery: {
					pageNo: 1,
					pageSize: 10
				},
				// 存酒（listByMember 返回全量，无分页）
				wines: [],
				wineLoading: false
			};
		},
		computed: {
			// 奖励金余额 = 累计邀请奖励 − 累积已提（两字段均由 member/list 下发）
			rewardBalance() {
				if (!this.member) {
					return 0;
				}
				const got = Number(this.member.inviteRewardAmount) || 0;
				const withdrew = Number(this.member.totalWithdrawInviteRewardAmount) || 0;
				return got - withdrew;
			}
		},
		mounted() {
			this.memberId = this.$route.query.id;
			this.getMember();
			this.loadTab(this.activeTab);
		},
		methods: {
			cellStyle({
				row,
				column,
				rowIndex,
				columnIndex
			}) {
				return "text-align:center";
			},
			headerCellStyle({
				row,
				rowIndex
			}) {
				return "text-align:center";
			},
			formatTime(row, column) {
				return this.formatTimeValue(row[column.property]);
			},
			formatTimeValue(val) {
				if (!val) {
					return '-';
				}
				let date = new Date(val);
				if (isNaN(date.getTime())) {
					return '-';
				}
				return this.$utils.formatDate(date, 'yyyy-MM-dd hh:mm:ss');
			},
			// 存酒的日期字段是 date 类型，只到天
			formatDay(val) {
				if (!val) {
					return '-';
				}
				const s = this.formatTimeValue(val);
				return s === '-' ? '-' : s.substring(0, 10);
			},
			money(val) {
				const n = Number(val);
				if (!isFinite(n)) {
					return '¥0.00';
				}
				return '¥' + n.toFixed(2);
			},
			num(val) {
				const n = Number(val);
				if (!isFinite(n)) {
					return '0';
				}
				return String(n);
			},
			levelOf(growth) {
				const value = Math.max(0, Number(growth) || 0);
				let current = MEMBER_LEVELS[0];
				for (let i = 0; i < MEMBER_LEVELS.length; i++) {
					if (value >= MEMBER_LEVELS[i].min) {
						current = MEMBER_LEVELS[i];
					}
				}
				return current;
			},
			levelTag(growth) {
				return 'V' + this.levelOf(growth).level;
			},
			levelName(growth) {
				return this.levelOf(growth).name;
			},
			// 状态文案与订单管理页保持一致（tb_order.status 注释口径）
			orderStatusText(status) {
				const map = {
					1: '未付款',
					2: '待处理',
					3: '待自取',
					4: '待配送',
					5: '已配送',
					6: '已完成',
					7: '售后处理中',
					8: '已退款',
					9: '售后处理完成',
					10: '已取消(未支付)',
					11: '已取消(已支付)'
				};
				return map[status] || '-';
			},
			// 券优惠力度：preferentialType 1=折扣（discountAmount 为比率，0.38 → 3.8折）2=满减
			couponBenefit(row) {
				if (row.preferentialType == 1) {
					const ratio = Number(row.discountAmount);
					if (!isFinite(ratio) || ratio <= 0) {
						return '-';
					}
					return ratio * 10 + ' 折';
				}
				if (row.preferentialType == 2) {
					return '满' + this.money(row.limitedPrice) + ' 减' + this.money(row.reducedPrice);
				}
				return '-';
			},
			// 券状态：is_used 优先；未使用再按 endTime 判过期（定时任务不跑，is_expired 不可信，与 C-07 口径一致）
			couponStatusText(row) {
				if (row.isUsed) {
					return '已使用';
				}
				const end = new Date(row.endTime);
				if (!isNaN(end.getTime()) && end.getTime() < Date.now()) {
					return '已过期';
				}
				return '未使用';
			},
			couponStatusClass(row) {
				const text = this.couponStatusText(row);
				if (text === '未使用') {
					return 'status-green';
				}
				if (text === '已过期') {
					return 'status-red';
				}
				return 'status-gray';
			},
			// 存酒状态：1=在存 2=已取完 3=已过期
			wineStatusCode(row) {
				return Number(row.statusCode !== undefined ? row.statusCode : row.status);
			},
			wineStatusText(row) {
				const map = {
					1: '在存',
					2: '已取完',
					3: '已过期'
				};
				return map[this.wineStatusCode(row)] || '-';
			},
			wineStatusClass(row) {
				const code = this.wineStatusCode(row);
				if (code == 1) {
					return 'status-green';
				}
				if (code == 3) {
					return 'status-red';
				}
				return 'status-gray';
			},

			// Tab 懒加载：首次切到才请求；切走再切回重新请求（数据量小，免缓存一致性）
			handleTabClick(tab) {
				this.loadTab(tab.name);
			},
			loadTab(name) {
				if (name === 'order') {
					this.getOrders(1);
				} else if (name === 'coupon') {
					this.getCoupons(1);
				} else if (name === 'wine') {
					this.getWines();
				}
				// balance 直接读 member，无需请求
			},

			getMember() {
				let vue = this;
				if (!vue.memberId) {
					vue.member = null;
					return;
				}
				vue.memberLoading = true;
				// 复用会员列表接口，按 id 精确取单条（该接口 SQL 支持 member.id 精确匹配）
				vue.$http.post(vue, '/rest/admin/member/list', {
						pageNo: 1,
						pageSize: 1,
						id: vue.memberId
					},
					(vue, data) => {
						const records = (data.data && data.data.records) || [];
						vue.member = records.length ? records[0] : null;
						vue.memberLoading = false;
					}, (error, data) => {
						vue.memberLoading = false;
						vue.$message({
							showClose: true,
							message: data && data.message ? data.message : '获取会员信息失败',
							type: 'error'
						});
					});
			},

			getOrders(pageNo) {
				let vue = this;
				if (!vue.memberId) {
					return;
				}
				vue.orderLoading = true;
				if (pageNo) {
					vue.orderQuery.pageNo = pageNo;
				}
				vue.$http.post(vue, '/rest/admin/order/list', {
						pageNo: vue.orderQuery.pageNo,
						pageSize: vue.orderQuery.pageSize,
						memberId: Number(vue.memberId),
						isDeleted: false
					},
					(vue, data) => {
						vue.orders = (data.data && data.data.records) || [];
						vue.orderTotal = (data.data && data.data.total) || 0;
						vue.orderLoading = false;
					}, (error, data) => {
						vue.orderLoading = false;
						vue.$message({
							showClose: true,
							message: data && data.message ? data.message : '获取订单失败',
							type: 'error'
						});
					});
			},

			getCoupons(pageNo) {
				let vue = this;
				if (!vue.memberId) {
					return;
				}
				vue.couponLoading = true;
				if (pageNo) {
					vue.couponQuery.pageNo = pageNo;
				}
				vue.$http.post(vue, '/rest/admin/couponsMemberRelation/list', {
						pageNo: vue.couponQuery.pageNo,
						pageSize: vue.couponQuery.pageSize,
						memberId: Number(vue.memberId)
					},
					(vue, data) => {
						// 返回结构为 { couponsMemberRelationMap, goodsList, shopList }，表格只取 relation 部分
						const records = (data.data && data.data.records) || [];
						vue.coupons = records.map(r => Object.assign({}, r.couponsMemberRelationMap || {}));
						vue.couponTotal = (data.data && data.data.total) || 0;
						vue.couponLoading = false;
					}, (error, data) => {
						vue.couponLoading = false;
						vue.$message({
							showClose: true,
							message: data && data.message ? data.message : '获取优惠券失败',
							type: 'error'
						});
					});
			},

			getWines() {
				let vue = this;
				if (!vue.memberId) {
					return;
				}
				vue.wineLoading = true;
				vue.$http.post(vue, '/rest/admin/wineStorage/listByMember', {
						memberId: Number(vue.memberId)
					},
					(vue, data) => {
						vue.wines = data.data || [];
						vue.wineLoading = false;
					}, (error, data) => {
						vue.wineLoading = false;
						vue.$message({
							showClose: true,
							message: data && data.message ? data.message : '获取存酒失败',
							type: 'error'
						});
					});
			},

			goBack() {
				this.$router.push({
					path: '/memberList'
				});
			},

			handleOrderSizeChange(val) {
				this.orderQuery.pageSize = val;
				this.getOrders(1);
			},
			handleOrderCurrentChange(val) {
				this.orderQuery.pageNo = val;
				this.getOrders();
			},
			handleCouponSizeChange(val) {
				this.couponQuery.pageSize = val;
				this.getCoupons(1);
			},
			handleCouponCurrentChange(val) {
				this.couponQuery.pageNo = val;
				this.getCoupons();
			}
		}
	};
</script>

<style scoped>
	.detail-title {
		margin-left: 14px;
		font-size: 15px;
		color: #3c4233;
	}

	.detail-sub {
		margin-left: 10px;
		font-size: 13px;
		color: #8a917f;
	}

	.block {
		margin-bottom: 16px;
		border: 1px solid #e3e6da;
	}

	.block-title {
		font-size: 14px;
		color: #3c4233;
	}

	.kv {
		display: flex;
		padding: 7px 0;
		font-size: 13px;
		line-height: 20px;
	}

	.k {
		width: 110px;
		flex-shrink: 0;
		color: #8a917f;
		text-align: right;
		margin-right: 12px;
	}

	.v {
		color: #3c4233;
		word-break: break-all;
	}

	.level-tag {
		display: inline-block;
		min-width: 30px;
		padding: 1px 6px;
		margin-right: 6px;
		font-size: 12px;
		color: #5f7d22;
		background: #eef3e2;
		border-radius: 9px;
	}

	.empty {
		padding: 30px 0;
		text-align: center;
		color: #b0b0b0;
		font-size: 13px;
	}

	/* 切换栏（对齐 Ardot 原型 84:1）*/
	.status-green {
		color: #5f7d22;
		background: #edf2de;
		display: inline-block;
		padding: 2px 10px;
		border-radius: 10px;
		font-size: 12px;
	}

	.status-gray {
		color: #8a917f;
		background: #f1efe8;
		display: inline-block;
		padding: 2px 10px;
		border-radius: 10px;
		font-size: 12px;
	}

	.status-red {
		color: #e25a2b;
		background: #fce3e3;
		display: inline-block;
		padding: 2px 10px;
		border-radius: 10px;
		font-size: 12px;
	}

	.bal-box {
		display: flex;
		align-items: center;
		padding: 12px 0;
	}

	.bal-main {
		width: 300px;
	}

	.bal-main .bl {
		font-size: 13px;
		color: #8a917f;
		margin-bottom: 8px;
	}

	.bal-main .bv {
		font-size: 30px;
		font-weight: 700;
		color: #404a21;
	}

	.bal-metrics {
		display: flex;
		flex: 1;
	}

	.metric {
		flex: 1;
		padding-left: 32px;
		border-left: 1px solid #e3e6da;
	}

	.metric .ml {
		font-size: 13px;
		color: #8a917f;
		margin-bottom: 8px;
	}

	.metric .mv {
		font-size: 18px;
		font-weight: 700;
		color: #3c4233;
	}
</style>
