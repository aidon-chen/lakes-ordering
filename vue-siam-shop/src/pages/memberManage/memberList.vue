<template>
	<section>
		<!--工具条-->
		<el-col :span="24" class="toolbar" style="padding-bottom: 0px;">
			<el-form :inline="true" :model="searchMsg">
				<el-form-item label="手机号">
					<el-input v-model="searchMsg.mobile" clearable placeholder="请输入手机号" style="width:160px;"></el-input>
				</el-form-item>
				<el-form-item label="昵称">
					<el-input v-model="searchMsg.nickname" clearable placeholder="请输入昵称" style="width:160px;"></el-input>
				</el-form-item>
				<el-form-item>
					<el-button type="primary" @click="getList(1)">查询</el-button>
					<el-button @click="resetSearch">重置</el-button>
				</el-form-item>
			</el-form>
		</el-col>

		<!--列表-->
		<el-table :data="list" highlight-current-row v-loading="listLoading" style="width: 100%;"
			:cell-style="cellStyle" :header-cell-style="headerCellStyle">
			<el-table-column label="会员" min-width="190">
				<template slot-scope="scope">
					<div class="member-cell">
						<img v-if="scope.row.headImg" class="member-avatar" :src="$http.publicUrl(scope.row.headImg)" />
						<div v-else class="member-avatar member-avatar-empty">无头像</div>
						<div class="member-info">
							<div class="member-name">{{ scope.row.nickname || '（未设置昵称）' }}</div>
							<div class="member-sub">ID {{ scope.row.id }}</div>
						</div>
					</div>
				</template>
			</el-table-column>
			<el-table-column prop="mobile" label="手机号" width="130"></el-table-column>
			<el-table-column label="会员等级" width="160">
				<template slot-scope="scope">
					<span class="level-tag">{{ levelTag(scope.row.growth) }}</span>
					<span class="level-name">{{ levelName(scope.row.growth) }}</span>
				</template>
			</el-table-column>
			<el-table-column prop="growth" label="成长值" width="90"></el-table-column>
			<el-table-column label="余额" width="100">
				<template slot-scope="scope">
					<span>{{ money(scope.row.balance) }}</span>
				</template>
			</el-table-column>
			<el-table-column label="积分" width="90">
				<template slot-scope="scope">
					<span>{{ num(scope.row.points) }}</span>
				</template>
			</el-table-column>
			<el-table-column label="实名" width="100">
				<template slot-scope="scope">
					<span v-if="scope.row.realName">{{ scope.row.realName }}</span>
					<span v-else class="muted">未实名</span>
				</template>
			</el-table-column>
			<el-table-column label="状态" width="90">
				<template slot-scope="scope">
					<span v-if="scope.row.isDisabled" style="color:#E25A2B">已禁用</span>
					<span v-else style="color:#43A047">正常</span>
				</template>
			</el-table-column>
			<el-table-column prop="createTime" label="注册时间" :formatter="formatTime" width="160"></el-table-column>
			<el-table-column prop="lastLoginTime" label="最近登录" :formatter="formatTime" width="160"></el-table-column>
			<el-table-column label="操作" fixed="right" width="90">
				<template slot-scope="scope">
					<el-button size="small" type="primary" @click="goDetail(scope.row)">详情</el-button>
				</template>
			</el-table-column>
		</el-table>

		<!--分页-->
		<el-col :span="24" class="toolbar">
			<el-pagination @size-change="handleSizeChange" @current-change="handleCurrentChange"
				:page-sizes="[10, 20, 50, 100]" :page-size="searchMsg.pageSize"
				layout="total, sizes, prev, pager, next, jumper" :total="total" style="float:right;">
			</el-pagination>
		</el-col>
	</section>
</template>

<script>
	// 会员等级规则：与顾客端 utils/member-level.js 保持同一口径。
	// 后端没有等级表（tb_member 无 level 列），等级完全由累计成长值 growth 判定，
	// 阈值目前是前端配置 —— 产品给出正式档位后需同时改这边与顾客端。
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
				searchMsg: {
					pageNo: 1,
					pageSize: 20,
					mobile: '',
					nickname: ''
				},
				list: [],
				total: 0,
				listLoading: false
			};
		},
		mounted() {
			this.getList(1);
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
				const val = row[column.property];
				if (!val) {
					return '-';
				}
				let date = new Date(val);
				return this.$utils.formatDate(date, 'yyyy-MM-dd hh:mm:ss');
			},
			// 金额：后端 balance 是 decimal(10,2)，统一两位小数
			money(val) {
				const n = Number(val);
				if (!isFinite(n)) {
					return '¥0.00';
				}
				return '¥' + n.toFixed(2);
			},
			// 积分：整数值（避免显示 0.00）
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

			getList(pageNo) {
				let vue = this;
				vue.listLoading = true;
				if (pageNo) {
					vue.searchMsg.pageNo = pageNo;
				}
				const param = {
					pageNo: vue.searchMsg.pageNo,
					pageSize: vue.searchMsg.pageSize,
					mobile: vue.searchMsg.mobile,
					nickname: vue.searchMsg.nickname,
					// 后端不传 isDeleted 会把已删除会员一并列出，这里固定只看未删除
					isDeleted: false
				};
				vue.$http.post(vue, '/rest/admin/member/list', param,
					(vue, data) => {
						vue.list = data.data.records;
						vue.total = data.data.total;
						vue.listLoading = false;
					}, (error, data) => {
						vue.listLoading = false;
						vue.$message({
							showClose: true,
							message: data && data.message ? data.message : '查询失败',
							type: 'error'
						});
					});
			},

			goDetail(row) {
				this.$router.push({
					path: '/memberDetail',
					query: {
						id: row.id
					}
				});
			},

			resetSearch() {
				this.searchMsg.mobile = '';
				this.searchMsg.nickname = '';
				this.getList(1);
			},

			handleSizeChange(val) {
				this.searchMsg.pageSize = val;
				this.getList(1);
			},
			handleCurrentChange(val) {
				this.searchMsg.pageNo = val;
				this.getList();
			}
		}
	};
</script>

<style scoped>
	.member-cell {
		display: flex;
		align-items: center;
		text-align: left;
	}

	.member-avatar {
		width: 36px;
		height: 36px;
		border-radius: 50%;
		object-fit: cover;
		border: 1px solid #e3e6da;
		flex-shrink: 0;
	}

	.member-avatar-empty {
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 10px;
		color: #a8b0a0;
		background: #f7f8f7;
	}

	.member-info {
		margin-left: 10px;
		overflow: hidden;
	}

	.member-name {
		font-size: 14px;
		color: #3c4233;
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.member-sub {
		font-size: 12px;
		color: #8a917f;
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

	.level-name {
		font-size: 13px;
		color: #5a614e;
	}

	.muted {
		color: #b0b0b0;
	}
</style>
