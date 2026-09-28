<template>
	<section>
		<!--工具条：状态筛选-->
		<el-col :span="24" class="toolbar" style="padding-bottom: 0px;">
			<el-form :inline="true" :model="searchMsg">
				<el-form-item label="开票状态">
					<el-select v-model="searchMsg.status" clearable placeholder="全部" @change="getList(1)">
						<el-option label="全部" :value="-1"></el-option>
						<el-option label="待处理" :value="1"></el-option>
						<el-option label="已开具" :value="2"></el-option>
						<el-option label="已驳回" :value="3"></el-option>
					</el-select>
				</el-form-item>
				<el-form-item label="订单号">
					<el-input v-model="searchMsg.orderNoLike" clearable placeholder="按订单号过滤" @clear="getList(1)"></el-input>
				</el-form-item>
				<el-form-item>
					<el-button type="primary" @click="getList(1)">查询</el-button>
				</el-form-item>
			</el-form>
		</el-col>

		<!--列表-->
		<el-table :data="filteredList" highlight-current-row v-loading="listLoading" style="width: 100%;"
			:cell-style="cellStyle" :header-cell-style="headerCellStyle">
			<el-table-column prop="orderNo" label="订单号" width="190"></el-table-column>
			<el-table-column prop="amount" label="开票金额" width="100">
				<template scope="scope">¥{{ scope.row.amount }}</template>
			</el-table-column>
			<el-table-column label="抬头类型" width="90">
				<template scope="scope">
					<span v-if="scope.row.titleType == 1">个人</span>
					<span v-else-if="scope.row.titleType == 2">单位</span>
				</template>
			</el-table-column>
			<el-table-column prop="title" label="发票抬头" min-width="150"></el-table-column>
			<el-table-column label="税号" width="160">
				<template scope="scope">{{ scope.row.titleType == 2 ? (scope.row.taxNo || '-') : '-' }}</template>
			</el-table-column>
			<el-table-column prop="email" label="接收邮箱" width="170"></el-table-column>
			<el-table-column label="状态" width="90">
				<template scope="scope">
					<span v-if="scope.row.status == 1" style="color:#E6A23C">待处理</span>
					<span v-else-if="scope.row.status == 2" style="color:#43A047">已开具</span>
					<span v-else-if="scope.row.status == 3" style="color:#E25A2B">已驳回</span>
				</template>
			</el-table-column>
			<el-table-column prop="remark" label="驳回原因" width="150">
				<template scope="scope">{{ scope.row.remark || '-' }}</template>
			</el-table-column>
			<el-table-column prop="applyTime" label="申请时间" :formatter="formatTime" width="160"></el-table-column>
			<el-table-column prop="finishTime" label="处理时间" :formatter="formatTime" width="160"></el-table-column>
			<el-table-column label="操作" fixed="right" width="100">
				<template slot-scope="scope">
					<el-button v-if="scope.row.status == 1" size="small" type="primary"
						@click="handleAudit(scope.row)">审批</el-button>
					<el-button v-else size="small" @click="handleAudit(scope.row)">查看</el-button>
				</template>
			</el-table-column>
		</el-table>

		<!--接口返回全量列表（非分页），量级=真实开票申请数，前端不再加分页条-->

		<!--审批弹窗：申请详情 + 完结操作-->
		<el-dialog title="发票申请审批" :visible.sync="auditVisible" width="640px" :close-on-click-modal="false">
			<div class="inv-head">
				<span class="inv-head-item">订单号：{{ auditRow.orderNo || '-' }}</span>
				<span class="inv-head-item">金额：¥{{ auditRow.amount || '-' }}</span>
				<span class="inv-head-tip">线下开具电子发票发送至顾客邮箱后，方可标记「已开具」</span>
			</div>

			<div class="inv-meta">抬头类型：{{ auditRow.titleType == 2 ? '单位' : '个人' }}</div>
			<div class="inv-meta">发票抬头：{{ auditRow.title || '-' }}</div>
			<div class="inv-meta" v-if="auditRow.titleType == 2">纳税人识别号：{{ auditRow.taxNo || '-' }}</div>
			<div class="inv-meta">接收邮箱：{{ auditRow.email || '-' }}</div>
			<div class="inv-meta">申请时间：{{ formatTimeValue(auditRow.applyTime) }}</div>

			<div class="inv-conclusion" v-if="auditRow.status == 1">
				<el-form label-width="90px">
					<el-form-item label="驳回原因">
						<el-input type="textarea" :rows="2" v-model="remark"
							placeholder="驳回时必填（开具时可留空）"></el-input>
					</el-form-item>
				</el-form>
			</div>
			<div class="inv-conclusion" v-else>
				<div class="inv-meta">本单已于 {{ formatTimeValue(auditRow.finishTime) }} 处理：{{ statusText(auditRow.status) }}</div>
				<div class="inv-meta" v-if="auditRow.remark">驳回原因：{{ auditRow.remark }}</div>
			</div>

			<div slot="footer" class="dialog-footer">
				<el-button @click.native="auditVisible = false">取消</el-button>
				<el-button v-if="auditRow.status == 1" type="danger" :loading="auditLoading"
					@click="submitFinish(3)">驳回</el-button>
				<el-button v-if="auditRow.status == 1" type="success" :loading="auditLoading"
					@click="submitFinish(2)">已开具</el-button>
			</div>
		</el-dialog>
	</section>
</template>

<script>
	export default {
		data() {
			return {
				searchMsg: {
					status: 1,
					orderNoLike: ''
				},
				list: [],
				listLoading: false,
				auditVisible: false,
				auditLoading: false,
				auditRow: {},
				remark: ''
			};
		},
		computed: {
			// 订单号为纯前端过滤（接口未暴露 orderNo 条件，单店申请量小，全量拉取后本地筛）
			filteredList() {
				const kw = (this.searchMsg.orderNoLike || '').trim();
				if (!kw) {
					return this.list;
				}
				return this.list.filter((row) => String(row.orderNo || '').indexOf(kw) !== -1);
			}
		},
		mounted() {
			this.getList(1);
		},
		methods: {
			cellStyle() {
				return "text-align:center";
			},
			headerCellStyle() {
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
			statusText(status) {
				if (status == 1) {
					return '待处理';
				}
				if (status == 2) {
					return '已开具';
				}
				if (status == 3) {
					return '已驳回';
				}
				return '-';
			},

			getList(pageNo) {
				let vue = this;
				vue.listLoading = true;
				const param = {
					// 全部=-1 时传 null（后端 null=不过滤）；接口返回全量 List，非分页
					status: vue.searchMsg.status === '' || vue.searchMsg.status === null || vue.searchMsg.status === -1 ?
						null : vue.searchMsg.status
				};
				vue.$http.post(vue, '/rest/merchant/invoice/list', param,
					(vue, data) => {
						vue.list = data.data || [];
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

			handleAudit(row) {
				this.auditRow = row;
				this.remark = '';
				this.auditVisible = true;
			},

			// status：2=已开具 3=已驳回（与后端枚举一致，别搞反）
			submitFinish(status) {
				let vue = this;
				if (status == 3 && (!vue.remark || !vue.remark.trim())) {
					vue.$message({
						showClose: true,
						message: '驳回时，驳回原因不能为空',
						type: 'warning'
					});
					return;
				}
				vue.auditLoading = true;
				vue.$http.post(vue, '/rest/merchant/invoice/finish', {
						id: vue.auditRow.id,
						status: status,
						remark: vue.remark
					},
					(vue, data) => {
						vue.auditLoading = false;
						if (data.success) {
							vue.$message({
								showClose: true,
								message: status == 2 ? '已标记开具' : '已驳回',
								type: 'success'
							});
							vue.auditVisible = false;
							vue.getList();
						} else {
							vue.$message({
								showClose: true,
								message: data.message,
								type: 'error'
							});
						}
					}, (error, data) => {
						vue.auditLoading = false;
						vue.$message({
							showClose: true,
							message: data && data.message ? data.message : '操作失败',
							type: 'error'
						});
					});
			}
		}
	};
</script>

<style scoped>
	.inv-head {
		margin-bottom: 16px;
		font-size: 14px;
	}

	.inv-head-item {
		margin-right: 20px;
		font-weight: bold;
	}

	.inv-head-tip {
		color: #E6A23C;
		font-size: 13px;
	}

	.inv-meta {
		font-size: 13px;
		color: #5a614e;
		margin-top: 6px;
	}

	.inv-conclusion {
		margin-top: 20px;
		border-top: 1px solid #e3e6da;
		padding-top: 16px;
	}
</style>
