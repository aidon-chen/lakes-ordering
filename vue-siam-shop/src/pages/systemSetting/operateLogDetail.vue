<template>
	<section>
		<!--工具条-->
		<el-col :span="24" class="toolbar" style="padding-bottom: 0px;">
			<el-form :inline="true" size="small">
				<el-form-item>
					<el-button @click="backHistory">返回</el-button>
				</el-form-item>
			</el-form>
		</el-col>

		<!--基础信息卡-->
		<div class="section-layout">
			<div class="layout-title">基础信息</div>
			<div class="section-content">
				<el-row :gutter="20">
					<el-col :span="8" class="info-item"><span class="info-label">操作时间：</span>{{formatTime('createTime')}}</el-col>
					<el-col :span="8" class="info-item"><span class="info-label">操作人：</span>{{detail.operator || '-'}}</el-col>
					<el-col :span="8" class="info-item"><span class="info-label">账号类型：</span>{{formatAccountType()}}</el-col>
				</el-row>
				<el-row :gutter="20">
					<el-col :span="8" class="info-item"><span class="info-label">操作模块：</span>{{detail.moduleName || detail.module || '-'}}</el-col>
					<el-col :span="8" class="info-item"><span class="info-label">操作动作：</span>{{detail.action || '-'}}</el-col>
					<el-col :span="8" class="info-item">
						<span class="info-label">执行结果：</span>
						<el-tag v-if="detail.result == 1" type="success" size="small">成功</el-tag>
						<el-tag v-else-if="detail.result == 2" type="danger" size="small">失败</el-tag>
						<span v-else>-</span>
					</el-col>
				</el-row>
				<el-row :gutter="20">
					<el-col :span="8" class="info-item"><span class="info-label">请求方式：</span>{{detail.requestMethod || '-'}}</el-col>
					<el-col :span="16" class="info-item"><span class="info-label">请求地址：</span>{{detail.requestUrl || '-'}}</el-col>
				</el-row>
				<el-row :gutter="20">
					<el-col :span="8" class="info-item"><span class="info-label">IP：</span>{{detail.ip || '-'}}</el-col>
					<el-col :span="8" class="info-item"><span class="info-label">耗时：</span>{{detail.costMs == null ? '-' : detail.costMs + ' ms'}}</el-col>
					<el-col :span="8" class="info-item"><span class="info-label">业务ID：</span>{{detail.businessId || '-'}}</el-col>
				</el-row>
				<el-row :gutter="20">
					<el-col :span="24" class="info-item"><span class="info-label">User-Agent：</span>{{detail.ua || '-'}}</el-col>
				</el-row>
				<el-row :gutter="20" v-if="detail.result == 2 && detail.errorMsg">
					<el-col :span="24" class="info-item error-text"><span class="info-label">错误信息：</span>{{detail.errorMsg}}</el-col>
				</el-row>
			</div>
		</div>

		<!--变更前后值 Diff：字段名对齐、删除行红底、新增行绿底-->
		<div class="section-layout">
			<div class="layout-title">变更内容（左=变更前，右=变更后）</div>
			<div class="section-content">
				<el-row :gutter="20" v-if="diffRows.length > 0">
					<el-col :span="12" class="diff-side diff-before">
							<table class="diff-table">
								<thead>
									<tr><th class="diff-th">字段</th><th class="diff-th">变更前</th></tr>
								</thead>
								<tbody>
									<tr v-for="(row, index) in diffRows" :key="'b' + index" :class="row.rowClass">
										<td class="diff-td diff-field">{{row.field}}</td>
										<td class="diff-td">{{row.before}}</td>
									</tr>
								</tbody>
							</table>
						</el-col>
						<el-col :span="12" class="diff-side diff-after">
						<table class="diff-table">
							<thead>
								<tr><th class="diff-th">字段</th><th class="diff-th">变更后</th></tr>
							</thead>
							<tbody>
								<tr v-for="(row, index) in diffRows" :key="'a' + index" :class="row.rowClass">
									<td class="diff-td diff-field">{{row.field}}</td>
									<td class="diff-td">{{row.after}}</td>
								</tr>
							</tbody>
						</table>
					</el-col>
				</el-row>
				<div v-else class="empty-tip">该操作未记录变更前后值</div>

				<!--原始 JSON（折叠）-->
				<el-collapse style="margin-top: 15px;" v-if="detail.beforeValue || detail.afterValue">
					<el-collapse-item title="查看原始 JSON">
						<div class="json-block">
							<p class="json-title">变更前：</p>
							<pre>{{prettyJson(detail.beforeValue)}}</pre>
							<p class="json-title">变更后：</p>
							<pre>{{prettyJson(detail.afterValue)}}</pre>
						</div>
					</el-collapse-item>
				</el-collapse>
			</div>
		</div>
	</section>
</template>

<script>
	export default {
		data() {
			return {
				detail: {}
			}
		},
		computed: {
			//把 before/after JSON 展开成「字段对齐」的 Diff 行
			diffRows() {
				let before = this.parseJson(this.detail.beforeValue);
				let after = this.parseJson(this.detail.afterValue);
				if (!before && !after) {
					return [];
				}
				let fields = [];
				let pushField = (obj) => {
					if (!obj) {
						return;
					}
					Object.keys(obj).forEach((key) => {
						if (fields.indexOf(key) === -1) {
							fields.push(key);
						}
					});
				};
				pushField(before);
				pushField(after);

				return fields.map((field) => {
					let beforeText = before ? this.formatValue(before[field]) : '-';
					let afterText = after ? this.formatValue(after[field]) : '-';
					let rowClass = '';
					if (before && after && beforeText !== afterText) {
						rowClass = 'changed-row';
					} else if (!before && after) {
						rowClass = 'added-row';
					} else if (before && !after) {
						rowClass = 'removed-row';
					}
					return {
						field: field,
						before: beforeText,
						after: afterText,
						rowClass: rowClass
					};
				});
			}
		},
		methods: {
			parseJson(text) {
				if (!text) {
					return null;
				}
				try {
					let parsed = JSON.parse(text);
					return (typeof parsed === 'object' && parsed !== null) ? parsed : null;
				} catch (e) {
					return null;
				}
			},
			formatValue(value) {
				if (value === undefined || value === null) {
					return '-';
				}
				if (typeof value === 'object') {
					return JSON.stringify(value);
				}
				return String(value);
			},
			prettyJson(text) {
				let parsed = this.parseJson(text);
				if (parsed) {
					return JSON.stringify(parsed, null, 2);
				}
				return text || '-';
			},
			formatTime(field) {
				if (!this.detail[field]) {
					return '-';
				}
				return this.$utils.formatDate(new Date(this.detail[field]), 'yyyy-MM-dd hh:mm:ss')
			},
			formatAccountType() {
				switch (this.detail.accountType) {
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
			backHistory() {
				this.$router.go(-1);
			},
			getDetail() {
				let vue = this;
				let id = vue.$route.query.id;
				if (!id) {
					vue.$message({ showClose: true, message: '缺少日志ID', type: 'error' });
					return;
				}
				vue.$http.post(vue, '/rest/admin/operateLog/selectById', {id: id},
					(vue, data) => {
						vue.detail = data.data || {};
					}, (error, data) => {
						vue.$message({ showClose: true, message: data.message, type: 'error' });
					}
				)
			}
		},
		mounted() {
			this.getDetail();
		}
	}
</script>

<style scoped>
	.section-layout {
		margin-top: 20px;
		border: 1px solid #DCDFE6;
	}
	.layout-title {
		color: #606266;
		padding: 15px 20px;
		background: #F2F6FC;
		font-weight: bold;
	}
	.section-content {
		padding: 20px;
	}
	.info-item {
		padding: 8px 0;
		font-size: 13px;
		color: #606266;
	}
	.info-label {
		color: #909399;
	}
	.error-text {
		color: #f56c6c;
	}
	.diff-table {
		width: 100%;
		border-collapse: collapse;
		font-size: 13px;
	}
	.diff-th {
		background: #F2F6FC;
		color: #606266;
		padding: 8px;
		border: 1px solid #EBEEF5;
		text-align: left;
	}
	.diff-td {
		padding: 8px;
		border: 1px solid #EBEEF5;
		word-break: break-all;
	}
	.diff-field {
		width: 40%;
		color: #909399;
	}
	/* Diff 双栏：变更前侧红底、变更后侧绿底（更新行），新增仅右侧绿底、删除仅左侧红底 */
	.diff-before .changed-row .diff-td,
	.diff-before .removed-row .diff-td {
		background: #fef0f0;
		color: #f56c6c;
	}
	.diff-after .changed-row .diff-td,
	.diff-after .added-row .diff-td {
		background: #f0f9eb;
		color: #67c23a;
	}
	.diff-before .added-row .diff-td,
	.diff-after .removed-row .diff-td {
		color: #909399;
	}
	.empty-tip {
		color: #909399;
		font-size: 13px;
		padding: 10px 0;
	}
	.json-block pre {
		background: #f5f7fa;
		padding: 10px;
		border-radius: 4px;
		font-size: 12px;
		overflow: auto;
	}
	.json-title {
		color: #909399;
		margin: 8px 0 4px 0;
	}
</style>