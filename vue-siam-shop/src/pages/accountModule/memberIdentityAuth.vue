<template>
	<section>
		<!--工具条-->
		<el-col :span="24" class="toolbar" style="padding-bottom: 0px;">
			<el-form :inline="true" :model="searchMsg">
				<el-form-item label="审核状态">
					<el-select v-model="searchMsg.auditStatus" clearable placeholder="全部">
						<el-option label="全部" :value="-1"></el-option>
						<el-option label="审核中" :value="1"></el-option>
						<el-option label="已通过" :value="2"></el-option>
						<el-option label="已驳回" :value="3"></el-option>
					</el-select>
				</el-form-item>
				<el-form-item label="认证类型">
					<el-select v-model="searchMsg.authType" clearable placeholder="全部">
						<el-option label="实名认证" :value="1"></el-option>
						<el-option label="身份认证" :value="2"></el-option>
					</el-select>
				</el-form-item>
				<el-form-item label="手机号">
					<el-input v-model="searchMsg.mobileLike" clearable placeholder="请输入手机号"></el-input>
				</el-form-item>
				<el-form-item label="姓名">
					<el-input v-model="searchMsg.realName" clearable placeholder="请输入姓名"></el-input>
				</el-form-item>
				<el-form-item>
					<el-button type="primary" @click="getList(1)">查询</el-button>
				</el-form-item>
			</el-form>
		</el-col>

		<!--列表-->
		<el-table :data="list" highlight-current-row v-loading="listLoading" style="width: 100%;"
			:cell-style="cellStyle" :header-cell-style="headerCellStyle">
			<el-table-column prop="mobile" label="手机号" width="130"></el-table-column>
			<el-table-column prop="realName" label="姓名" width="110"></el-table-column>
			<el-table-column label="认证类型" width="110">
				<template scope="scope">
					<span v-if="scope.row.authType == 1">实名认证</span>
					<span v-else-if="scope.row.authType == 2">身份认证</span>
				</template>
			</el-table-column>
			<el-table-column label="身份类型" width="100">
				<template scope="scope">
					<span v-if="scope.row.identityType == 1">学生</span>
					<span v-else-if="scope.row.identityType == 2">教师</span>
					<span v-else>-</span>
				</template>
			</el-table-column>
			<el-table-column label="审核状态" width="110">
				<template scope="scope">
					<span v-if="scope.row.auditStatus == 1" style="color:#E6A23C">审核中</span>
					<span v-else-if="scope.row.auditStatus == 2" style="color:#43A047">已通过</span>
					<span v-else-if="scope.row.auditStatus == 3" style="color:#E25A2B">已驳回</span>
				</template>
			</el-table-column>
			<el-table-column prop="auditReason" label="驳回理由" width="180"></el-table-column>
			<el-table-column prop="createTime" label="提交时间" :formatter="formatTime" width="160"></el-table-column>
			<el-table-column prop="auditTime" label="审核时间" :formatter="formatTime" width="160"></el-table-column>
			<el-table-column label="操作" fixed="right" width="110">
				<template slot-scope="scope">
					<el-button v-if="scope.row.auditStatus == 1" size="small" type="primary"
						@click="handleAudit(scope.row)">审核</el-button>
					<el-button v-else size="small" @click="handleAudit(scope.row)">查看</el-button>
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

		<!--审核弹窗：身份证与身份材料双图同屏（实施计划 §7.1 的关键交互）-->
		<el-dialog title="身份认证审核" :visible.sync="auditVisible" width="900px" :close-on-click-modal="false">
			<div class="auth-head">
				<span class="auth-head-item">会员：{{ auditRow.mobile || '-' }}</span>
				<span class="auth-head-item">姓名：{{ auditRow.realName || '-' }}</span>
				<span class="auth-head-tip">请在下方同屏核对两阶段材料，确认姓名一致后再给结论</span>
			</div>

			<el-row :gutter="20">
				<el-col :span="12">
					<div class="auth-col-title">① 实名认证 · 身份证</div>
					<template v-if="stage1">
						<div class="auth-img-row" v-if="stage1.idCardFrontUrl || stage1.idCardBackUrl">
							<el-image class="auth-img" :src="$http.publicUrl(stage1.idCardFrontUrl)"
								:preview-src-list="[$http.publicUrl(stage1.idCardFrontUrl)]"></el-image>
							<el-image class="auth-img" :src="$http.publicUrl(stage1.idCardBackUrl)"
								:preview-src-list="[$http.publicUrl(stage1.idCardBackUrl)]"></el-image>
						</div>
						<div class="auth-img-label-row" v-if="stage1.idCardFrontUrl || stage1.idCardBackUrl">
							<span class="auth-img-label">人像面</span>
							<span class="auth-img-label">国徽面</span>
						</div>
						<!-- 证件图已迁私有桶，且出结论满保留期限后会被删除 → 后端置 materialPurged -->
						<div class="auth-purged" v-else>
							{{ stage1.materialPurged ? '材料已按保留期限自动删除，仅保留审核痕迹' : '无图片' }}
						</div>
						<div class="auth-meta">姓名：{{ stage1.realName || '-' }}</div>
						<div class="auth-meta">状态：{{ auditStatusText(stage1.auditStatus) }}</div>
					</template>
					<div v-else class="auth-empty">未提交</div>
				</el-col>

				<el-col :span="12">
					<div class="auth-col-title">
						② 身份认证 · {{ stage2 && stage2.identityType == 2 ? '教师资格证' : '学生证' }}
					</div>
					<template v-if="stage2">
						<div class="auth-img-row" v-if="hasStage2Img"
							:class="{ 'auth-img-row-single': stage2.identityType == 2 }">
							<el-image v-if="stage2.identityType == 1 && stage2.studentCardImgUrl" class="auth-img"
								:src="$http.publicUrl(stage2.studentCardImgUrl)"
								:preview-src-list="[$http.publicUrl(stage2.studentCardImgUrl)]"></el-image>
							<el-image v-if="stage2.identityType == 1 && stage2.studentCardRegImgUrl" class="auth-img"
								:src="$http.publicUrl(stage2.studentCardRegImgUrl)"
								:preview-src-list="[$http.publicUrl(stage2.studentCardRegImgUrl)]"></el-image>
							<el-image v-if="stage2.identityType == 2 && stage2.teacherCertImgUrl"
								class="auth-img auth-img-wide"
								:src="$http.publicUrl(stage2.teacherCertImgUrl)"
								:preview-src-list="[$http.publicUrl(stage2.teacherCertImgUrl)]"></el-image>
						</div>
						<div class="auth-purged" v-else>
							{{ stage2.materialPurged ? '材料已按保留期限自动删除，仅保留审核痕迹' : '无图片' }}
						</div>
						<div class="auth-meta">姓名：{{ stage2.realName || '-' }}（人工核对与阶段一是否一致）</div>
						<div class="auth-meta">学校：{{ stage2.schoolName || '-' }}</div>
						<div class="auth-meta">学号：{{ stage2.studentNo || '-' }}</div>
						<div class="auth-meta">证书编号：{{ stage2.certNo || '-' }}</div>
					</template>
					<div v-else class="auth-empty">未提交</div>
				</el-col>
			</el-row>

			<div class="auth-conclusion" v-if="auditRow.auditStatus == 1">
				<el-form label-width="90px">
					<el-form-item label="驳回理由">
						<el-input type="textarea" :rows="2" v-model="opinion"
							placeholder="审核不通过时必填（通过时可留空）"></el-input>
					</el-form-item>
				</el-form>
			</div>
			<div class="auth-conclusion" v-else>
				<div class="auth-meta">本单已于 {{ formatTimeValue(auditRow.auditTime) }} 审核：{{ auditStatusText(auditRow.auditStatus) }}</div>
				<div class="auth-meta" v-if="auditRow.auditReason">驳回理由：{{ auditRow.auditReason }}</div>
			</div>

			<div slot="footer" class="dialog-footer">
				<el-button @click.native="auditVisible = false">取消</el-button>
				<el-button v-if="auditRow.auditStatus == 1" type="primary" :loading="auditLoading"
					@click="submitAudit(2)">驳回</el-button>
				<el-button v-if="auditRow.auditStatus == 1" type="success" :loading="auditLoading"
					@click="submitAudit(1)">通过</el-button>
			</div>
		</el-dialog>
	</section>
</template>

<script>
	export default {
		data() {
			return {
				searchMsg: {
					pageNo: 1,
					pageSize: 20,
					auditStatus: 1,
					authType: '',
					mobileLike: '',
					realName: ''
				},
				list: [],
				total: 0,
				listLoading: false,
				auditVisible: false,
				auditLoading: false,
				auditRow: {},
				stage1: null,
				stage2: null,
				opinion: ''
			};
		},
		mounted() {
			this.getList(1);
		},
		computed: {
			// 阶段二是否有可展示的图（证件图已迁私有桶，URL 由后端下发；出结论满保留期限后会被删空）
			hasStage2Img() {
				const s = this.stage2;
				if (!s) {
					return false;
				}
				return !!(s.studentCardImgUrl || s.studentCardRegImgUrl || s.teacherCertImgUrl);
			}
		},
		methods: {
			cellStyle({row, column, rowIndex, columnIndex}) {
				return "text-align:center";
			},
			headerCellStyle({row, rowIndex}) {
				return "text-align:center";
			},
			formatTime(row, column) {
				const val = row[column.property];
				if (!val) {
					return '-';
				}
				return this.formatTimeValue(val);
			},
			formatTimeValue(val) {
				if (!val) {
					return '-';
				}
				let date = new Date(val);
				return this.$utils.formatDate(date, 'yyyy-MM-dd hh:mm:ss');
			},
			auditStatusText(status) {
				if (status == 1) {
					return '审核中';
				}
				if (status == 2) {
					return '已通过';
				}
				if (status == 3) {
					return '已驳回';
				}
				return '未提交';
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
					auditStatus: vue.searchMsg.auditStatus === '' || vue.searchMsg.auditStatus === null ?
						null : vue.searchMsg.auditStatus,
					authType: vue.searchMsg.authType === '' ? null : vue.searchMsg.authType,
					mobileLike: vue.searchMsg.mobileLike,
					realName: vue.searchMsg.realName
				};
				vue.$http.post(vue, '/rest/admin/memberIdentityAuth/list', param,
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

			// 审核弹窗：按会员id取该会员两阶段全部记录，实现「双图同屏」
			handleAudit(row) {
				let vue = this;
				vue.auditRow = row;
				vue.opinion = '';
				vue.stage1 = null;
				vue.stage2 = null;
				vue.$http.post(vue, '/rest/admin/memberIdentityAuth/detail', {
						queryMemberId: row.memberId
					},
					(vue, data) => {
						const records = data.data || [];
						records.forEach((item) => {
							if (item.authType == 1) {
								vue.stage1 = item;
							} else if (item.authType == 2) {
								vue.stage2 = item;
							}
						});
					}, (error, data) => {
						vue.$message({
							showClose: true,
							message: data && data.message ? data.message : '获取详情失败',
							type: 'error'
						});
					});
				vue.auditVisible = true;
			},

			// status：1=通过 2=不通过（与后端一致，别搞反）
			submitAudit(status) {
				let vue = this;
				if (status == 2 && (!vue.opinion || !vue.opinion.trim())) {
					vue.$message({
						showClose: true,
						message: '审核不通过时，审核意见不能为空',
						type: 'warning'
					});
					return;
				}
				vue.auditLoading = true;
				vue.$http.post(vue, '/rest/admin/memberIdentityAuth/audit', {
						id: vue.auditRow.id,
						status: status,
						opinion: vue.opinion
					},
					(vue, data) => {
						vue.auditLoading = false;
						if (data.success) {
							vue.$message({
								showClose: true,
								message: status == 1 ? '已通过' : '已驳回',
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
							message: data && data.message ? data.message : '审核失败',
							type: 'error'
						});
					});
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
	.auth-head {
		margin-bottom: 16px;
		font-size: 14px;
	}

	.auth-head-item {
		margin-right: 20px;
		font-weight: bold;
	}

	.auth-head-tip {
		color: #E6A23C;
	}

	.auth-col-title {
		font-weight: bold;
		margin-bottom: 10px;
	}

	.auth-img-row {
		display: flex;
	}

	.auth-img {
		width: 48%;
		height: 130px;
		border: 1px solid #e3e6da;
		border-radius: 6px;
		margin-right: 4%;
	}

	.auth-img-row-single .auth-img-wide {
		width: 100%;
		margin-right: 0;
	}

	.auth-img-label-row {
		display: flex;
		margin-top: 6px;
	}

	.auth-img-label {
		width: 48%;
		margin-right: 4%;
		text-align: center;
		font-size: 12px;
		color: #8a917f;
	}

	.auth-meta {
		font-size: 13px;
		color: #5a614e;
		margin-top: 6px;
	}

	.auth-empty {
		height: 130px;
		line-height: 130px;
		text-align: center;
		color: #b0b0b0;
		background: #f7f8f7;
		border: 1px dashed #d8ded9;
		border-radius: 6px;
	}

	/* 材料已按保留期限删除（plan §8「审核即弃」）——比坏掉的图片框更易理解 */
	.auth-purged {
		height: 130px;
		line-height: 130px;
		text-align: center;
		color: #8a6d3b;
		background: #fff7e6;
		border: 1px dashed #f0e0c0;
		border-radius: 6px;
		font-size: 13px;
	}

	.auth-conclusion {
		margin-top: 20px;
		border-top: 1px solid #e3e6da;
		padding-top: 16px;
	}
</style>
