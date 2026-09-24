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
				<el-col :span="8">
					<div class="kv"><span class="k">最近使用小程序</span><span class="v">{{ formatTimeValue(member.lastUseTime) }}</span></div>
				</el-col>
				<el-col :span="8">
					<div class="kv"><span class="k">邀请码</span><span class="v">{{ member.inviteCode || '-' }}</span></div>
				</el-col>
			</el-row>
			<div v-else class="empty">未获取到会员信息（会员ID：{{ memberId }}）</div>
		</el-card>

		<!--订单记录-->
		<el-card shadow="never" class="block">
			<div slot="header" class="block-title">
				订单记录
				<span class="block-tip">共 {{ orderTotal }} 笔</span>
			</div>
			<el-table :data="orders" v-loading="orderLoading" style="width: 100%;"
				:cell-style="cellStyle" :header-cell-style="headerCellStyle">
				<el-table-column prop="orderNo" label="订单号" width="190"></el-table-column>
				<el-table-column label="类型" width="80">
					<template slot-scope="scope">
						<span>{{ scope.row.orderScene == 2 ? '自提' : '堂食' }}</span>
					</template>
				</el-table-column>
				<el-table-column label="取餐号 / 桌号" width="120">
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
		</el-card>

		<!--暂未开放的数据块-->
		<el-card shadow="never" class="block">
			<div slot="header" class="block-title">优惠券 / 存酒 / 余额流水</div>
			<div class="pending">
				<div class="pending-lead">以下三项后端尚未提供按会员查询的接口，因此暂不展示（不放置点了没反应的死按钮）：</div>
				<ul class="pending-list">
					<li><b>优惠券</b>：<code>/rest/admin/couponsMemberRelation</code> 只有发券接口（giveCoupons / giveSingleCoupons），没有按会员查券的列表接口。</li>
					<li><b>存酒记录</b>：目前只有收银端接口 <code>/rest/merchant/wineStorage</code>，后台无对应查询接口。</li>
					<li><b>余额流水</b>：<code>AdminMemberBillingRecordController</code> 整个类处于注释状态，接口未启用。</li>
				</ul>
				<div class="pending-foot">
					数据库侧三张表都已存在（tb_coupons_member_relation、tb_wine_storage、tb_member_billing_record），补齐查询接口后可直接点亮。
				</div>
			</div>
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
				orders: [],
				orderTotal: 0,
				orderLoading: false,
				orderQuery: {
					pageNo: 1,
					pageSize: 10
				}
			};
		},
		mounted() {
			this.memberId = this.$route.query.id;
			this.getMember();
			this.getOrders(1);
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
				return this.$utils.formatDate(date, 'yyyy-MM-dd hh:mm:ss');
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

	.block-tip {
		margin-left: 10px;
		font-size: 12px;
		color: #8a917f;
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

	.pending {
		font-size: 13px;
		color: #5a614e;
		line-height: 1.8;
	}

	.pending-lead {
		margin-bottom: 6px;
	}

	.pending-list {
		margin: 0;
		padding-left: 20px;
	}

	.pending-list b {
		color: #3c4233;
		font-weight: 500;
	}

	.pending-list code {
		padding: 1px 5px;
		font-size: 12px;
		color: #5f7d22;
		background: #f2f5ec;
		border-radius: 3px;
	}

	.pending-foot {
		margin-top: 8px;
		color: #8a917f;
		font-size: 12px;
	}
</style>
