<template>
	<section>
		<!--工具条-->
		<el-col :span="24" class="toolbar" style="padding-bottom: 0px;">
			<el-form :inline="true" :model="searchMsg" size="small">
				<el-form-item label="账号">
					<el-input v-model="searchMsg.account" clearable placeholder="请输入登录账号"></el-input>
				</el-form-item>
				<el-form-item label="登录结果">
					<el-select v-model="searchMsg.result" clearable placeholder="全部">
						<el-option label="成功" :value="1"></el-option>
						<el-option label="失败" :value="2"></el-option>
					</el-select>
				</el-form-item>
				<el-form-item label="">
					<el-date-picker
						v-model="searchMsg.createTime"
						type="daterange"
						align="right"
						unlink-panels
						range-separator="至"
						start-placeholder="开始日期"
						end-placeholder="结束日期"
						:picker-options="pickerOptions">
					</el-date-picker>
				</el-form-item>
				<el-form-item>
					<el-button type="primary" @click="getList(1)">查询</el-button>
					<el-button @click="handleExport">导出</el-button>
				</el-form-item>
			</el-form>
		</el-col>

		<!--列表-->
		<el-table :data="list" highlight-current-row v-loading="listLoading" style="width: 100%;"
			:cell-style="cellStyle" :header-cell-style="headerCellStyle"
			:row-class-name="rowClassName">
			<el-table-column prop="createTime" label="登录时间" :formatter="formatTime" width="170"></el-table-column>
			<el-table-column prop="account" label="账号"></el-table-column>
			<el-table-column prop="accountType" label="账号类型" :formatter="formatAccountType" width="100"></el-table-column>
			<el-table-column prop="ip" label="登录IP" width="140"></el-table-column>
			<el-table-column prop="region" label="归属地" width="140">
				<template slot-scope="scope">
					<span>{{scope.row.region || '-'}}</span>
				</template>
			</el-table-column>
			<el-table-column prop="browser" label="浏览器" width="110">
				<template slot-scope="scope">
					<span>{{scope.row.browser || '-'}}</span>
				</template>
			</el-table-column>
			<el-table-column prop="os" label="操作系统" width="110">
				<template slot-scope="scope">
					<span>{{scope.row.os || '-'}}</span>
				</template>
			</el-table-column>
			<el-table-column prop="result" label="登录结果" width="100">
				<template slot-scope="scope">
					<el-tag v-if="scope.row.result == 1" type="success" size="small">成功</el-tag>
					<el-tag v-else type="danger" size="small">失败</el-tag>
				</template>
			</el-table-column>
			<el-table-column prop="message" label="提示信息" show-overflow-tooltip></el-table-column>
		</el-table>

		<!--工具条-->
		<el-col :span="24" class="toolbar">
			<el-pagination
				@size-change="handleSizeChange"
				@current-change="handleCurrentChange"
				:page-sizes="[10, 20, 50, 100]"
				:page-size="searchMsg.pageSize"
				layout="total, sizes, prev, pager, next, jumper"
				:total="total"
				style="float:right;">
			</el-pagination>
		</el-col>
	</section>
</template>

<script>
	export default {
		data() {
			return {
				pickerOptions: {
					shortcuts: [{
						text: '最近一周',
						onClick(picker) {
							const end = new Date();
							let start = new Date();
							start.setTime(end.getTime() - 1000 * 60 * 60 * 24 * 7);
							picker.$emit('pick', [start, end]);
						}
					}, {
						text: '最近一月',
						onClick(picker) {
							const end = new Date();
							let start = new Date();
							start.setTime(end.getTime() - 1000 * 60 * 60 * 24 * 30);
							picker.$emit('pick', [start, end]);
						}
					}]
				},
				searchMsg: {
					pageNo: 1,
					pageSize: 20
				},
				list: [],
				total: 0,
				listLoading: false
			}
		},
		methods: {
			cellStyle() {
				return "text-align:center";
			},
			headerCellStyle() {
				return "text-align:center";
			},
			//失败记录整行标红
			rowClassName({row}) {
				return row.result == 2 ? 'fail-row' : '';
			},
			formatTime(row, column) {
				if (!row[column.property]) {
					return '-';
				}
				return this.$utils.formatDate(new Date(row[column.property]), 'yyyy-MM-dd hh:mm:ss')
			},
			formatAccountType(row) {
				switch (row.accountType) {
					case 1:
						return '管理员';
					case 2:
						return '商家';
					case 3:
						return '用户';
					default:
						return '-';
				}
			},
			getList(pageNoParam) {
				if (pageNoParam) {
					this.searchMsg.pageNo = pageNoParam;
				}
				let vue = this;
				let param = vue.buildParam();

				vue.listLoading = true;
				vue.$http.post(vue, '/rest/admin/loginLog/list', param,
					(vue, data) => {
						vue.list = data.data.records;
						vue.total = data.data.total;
						vue.listLoading = false;
					}, (error, data) => {
						vue.listLoading = false;
						vue.$message({ showClose: true, message: data.message, type: 'error' });
					}
				)
			},
			buildParam() {
				let param = Object.assign({}, this.searchMsg);
				if (this.searchMsg.createTime && this.searchMsg.createTime.length == 2) {
					param.startCreateTime = this.$utils.formatDate(new Date(this.searchMsg.createTime[0]), 'yyyy/MM/dd hh:mm:ss');
					param.endCreateTime = this.$utils.formatDate(new Date(this.searchMsg.createTime[1]), 'yyyy/MM/dd hh:mm:ss');
				}
				delete param.createTime;
				return param;
			},
			//CSV 单元格转义：含逗号/引号/换行时整体用双引号包裹，内部双引号翻倍
			csvCell(value) {
				if (value === null || value === undefined) {
					return '';
				}
				let text = String(value);
				if (/[",\n\r]/.test(text)) {
					return '"' + text.replace(/"/g, '""') + '"';
				}
				return text;
			},
			//导出当前筛选条件下的全部登录日志（客户端导出 CSV）
			handleExport() {
				let vue = this;
				let param = this.buildParam();
				param.pageNo = -1;
				param.pageSize = 100000;
				vue.$http.post(vue, '/rest/admin/loginLog/export', param,
					(vue, data) => {
						let rows = data.data || [];
						if (rows.length === 0) {
							vue.$message({ showClose: true, message: '当前条件下没有可导出的日志', type: 'warning' });
							return;
						}
						let header = ['登录时间', '账号', '账号类型', '登录IP', '归属地', '浏览器', '操作系统', '登录结果', '提示信息'];
						let lines = [header.map(item => vue.csvCell(item)).join(',')];
						rows.forEach(item => {
							lines.push([
								vue.formatTime(item, {property: 'createTime'}),
								item.account || '',
								vue.formatAccountType(item),
								item.ip || '',
								item.region || '',
								item.browser || '',
								item.os || '',
								item.result == 1 ? '成功' : '失败',
								item.message || ''
							].map(item => vue.csvCell(item)).join(','));
						});
						let blob = new Blob(['\ufeff' + lines.join('\n')], {type: 'text/csv;charset=utf-8'});
						let link = document.createElement('a');
						link.href = URL.createObjectURL(blob);
						link.download = 'login-log-' + new Date().getTime() + '.csv';
						link.click();
						URL.revokeObjectURL(link.href);
					}, (error, data) => {
						vue.$message({ showClose: true, message: data.message, type: 'error' });
					}
				)
			},
			handleSizeChange(val) {
				this.searchMsg.pageSize = val;
				this.getList();
			},
			handleCurrentChange(val) {
				this.searchMsg.pageNo = val;
				this.getList();
			}
		},
		mounted() {
			this.getList();
		}
	}
</script>

<style scoped>
	/deep/ .fail-row {
		color: #f56c6c;
	}
</style>