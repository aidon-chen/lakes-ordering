<template>
	<section>
		<!--工具条-->
		<el-col :span="24" class="toolbar" style="padding-bottom: 0px;">
			<el-form :inline="true" :model="searchMsg" size="small">
				<el-form-item>
					<el-input v-model="searchMsg.imageName" placeholder="请输入弹窗名称搜索" clearable></el-input>
				</el-form-item>
				<el-form-item>
					<el-button type="primary" @click="getList(1)">查询</el-button>
					<el-button type="primary" @click="handleAdd">新增弹窗</el-button>
				</el-form-item>
			</el-form>
		</el-col>

		<!--列表-->
		<el-table :data="list" highlight-current-row v-loading="listLoading" style="width: 100%;"
			:cell-style="cellStyle" :header-cell-style="headerCellStyle">
			<el-table-column label="弹窗预览图" width="120">
				<template slot-scope="scope">
					<img v-if="scope.row.imagePath" :src="$http.publicUrl(scope.row.imagePath)" width="60" height="80">
					<span v-else>-</span>
				</template>
			</el-table-column>
			<el-table-column prop="imageName" label="弹窗名称"></el-table-column>
			<el-table-column prop="popupFrequency" label="展示频次" :formatter="formatFrequency"></el-table-column>
			<el-table-column prop="startTime" label="生效开始时间" :formatter="formatTime" width="180"></el-table-column>
			<el-table-column prop="endTime" label="生效结束时间" :formatter="formatTime" width="180"></el-table-column>
			<el-table-column prop="imageLinkUrl" label="超链接">
				<template slot-scope="scope">
					<span>{{scope.row.imageLinkUrl || '-'}}</span>
				</template>
			</el-table-column>
			<el-table-column prop="sortNumber" label="排序" width="70"></el-table-column>
			<el-table-column label="状态" width="90">
				<template slot-scope="scope">
					<el-tag v-if="getStatus(scope.row) == 1" type="success" size="small">生效中</el-tag>
					<el-tag v-else-if="getStatus(scope.row) == 2" type="warning" size="small">未开始</el-tag>
					<el-tag v-else type="info" size="small">已过期</el-tag>
				</template>
			</el-table-column>
			<el-table-column label="操作" fixed="right" width="200">
				<template slot-scope="scope">
					<el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
					<el-button size="small" type="primary" plain @click="handlePreview(scope.row)">预览</el-button>
					<el-button size="small" type="danger" @click="handleDel(scope.row.id)">删除</el-button>
				</template>
			</el-table-column>
		</el-table>

		<!--工具条-->
		<el-col :span="24" class="toolbar">
			<el-pagination
				@size-change="handleSizeChange"
				@current-change="handleCurrentChange"
				:page-sizes="[10, 20, 50]"
				:page-size="searchMsg.pageSize"
				layout="total, sizes, prev, pager, next, jumper"
				:total="total"
				style="float:right;">
			</el-pagination>
		</el-col>

		<!--新增/编辑弹层-->
		<el-dialog :title="dialogTitle" :visible.sync="editFormVisible" :close-on-click-modal="false" width="560px">
			<el-form :model="editForm" label-width="140px" :rules="editFormRules" ref="editForm">
				<el-form-item label="弹窗名称" prop="imageName">
					<el-input v-model="editForm.imageName"></el-input>
				</el-form-item>
				<el-form-item label="弹窗图片(750×1000)" prop="imageFile">
					<el-upload
						class="avatar-uploader"
						action=""
						name="popupImageUpload"
						:limit="1"
						:file-list="editForm.imageFile"
						:before-upload="beforePopupImageUpload"
						:http-request="upload"
						:on-remove="handleRemoveImage"
						list-type="picture-card">
						<i class="el-icon-plus avatar-uploader-icon"></i>
					</el-upload>
				</el-form-item>
				<el-form-item label="展示频次" prop="popupFrequency">
					<el-radio-group v-model="editForm.popupFrequency">
						<el-radio :label="1">每次进入</el-radio>
						<el-radio :label="2">每天一次</el-radio>
						<el-radio :label="3">仅一次</el-radio>
					</el-radio-group>
				</el-form-item>
				<el-form-item label="生效时间" prop="validTime">
					<el-date-picker
						v-model="validTime"
						type="datetimerange"
						range-separator="至"
						start-placeholder="开始时间"
						end-placeholder="结束时间"
						value-format="yyyy/MM/dd HH:mm:ss">
					</el-date-picker>
				</el-form-item>
				<el-form-item label="超链接" prop="imageLinkUrl">
					<el-input v-model="editForm.imageLinkUrl" placeholder="点击弹窗跳转地址（可空）"></el-input>
				</el-form-item>
				<el-form-item label="排序" prop="sortNumber">
					<el-input v-model="editForm.sortNumber"></el-input>
				</el-form-item>
				<el-form-item label="说明" prop="description">
					<el-input type="textarea" :rows="3" v-model="editForm.description"></el-input>
				</el-form-item>
			</el-form>
			<div slot="footer" class="dialog-footer">
				<el-button @click="editFormVisible = false">取消</el-button>
				<el-button type="primary" @click="editSubmit" :loading="editLoading">提交</el-button>
			</div>
		</el-dialog>

		<!--预览弹层（模拟客户端首页弹窗效果）-->
		<el-dialog title="弹窗预览（模拟客户端效果）" :visible.sync="previewVisible" :close-on-click-modal="false" width="420px">
			<div class="preview-phone">
				<div class="preview-dialog">
					<img v-if="previewForm.imagePath" :src="$http.publicUrl(previewForm.imagePath)" class="preview-img">
					<div v-else class="preview-empty">未配置图片</div>
					<i class="el-icon-close preview-close"></i>
				</div>
				<p class="preview-tip">展示频次：{{formatFrequency(previewForm)}}｜超链接：{{previewForm.imageLinkUrl || '无'}}</p>
			</div>
			<div slot="footer" class="dialog-footer">
				<el-button @click.native="previewVisible = false">关闭</el-button>
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
					pageSize: 10,
					type: 2 //M-01：仅管理首页活动弹窗（type=2）
				},
				list: [],
				total: 0,
				listLoading: false,
				editFormVisible: false,
				editLoading: false,
				dialogTitle: '新增弹窗',
				validTime: [], //生效时间区间
				editForm: {
					type: 2,
					popupFrequency: 1,
					sortNumber: 0,
					imageFile: []
				},
				editFormRules: {
					imageName: [{ required: true, message: '请输入弹窗名称', trigger: 'blur' }],
					imageFile: [{ type: 'array', required: true, message: '请上传弹窗图片', trigger: 'change' }]
				},
				previewVisible: false,
				previewForm: {}
			}
		},
		methods: {
			cellStyle() {
				return "text-align:center";
			},
			headerCellStyle() {
				return "text-align:center";
			},
			formatTime(row, column) {
				if (!row[column.property]) {
					return '-';
				}
				return this.$utils.formatDate(new Date(row[column.property]), 'yyyy-MM-dd hh:mm')
			},
			//展示频次：1=每次进入 2=每天一次 3=仅一次
			formatFrequency(row) {
				switch (row.popupFrequency) {
					case 1:
						return '每次进入';
					case 2:
						return '每天一次';
					case 3:
						return '仅一次';
					default:
						return '-';
				}
			},
			//状态：1=生效中 2=未开始 3=已过期（无独立启停字段，按生效时间判定）
			getStatus(row) {
				let now = new Date().getTime();
				let start = row.startTime ? new Date(row.startTime).getTime() : null;
				let end = row.endTime ? new Date(row.endTime).getTime() : null;
				if (start && now < start) {
					return 2;
				}
				if (end && now > end) {
					return 3;
				}
				if (!start && !end) {
					return 3;
				}
				return 1;
			},
			getList(pageNoParam) {
				if (pageNoParam) {
					this.searchMsg.pageNo = pageNoParam;
				}
				let vue = this;
				let param = Object.assign({}, vue.searchMsg);
				vue.listLoading = true;
				vue.$http.post(vue, '/rest/admin/advertisement/list', param,
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
			handleAdd() {
				this.dialogTitle = '新增弹窗';
				this.validTime = [];
				this.editForm = {
					type: 2,
					popupFrequency: 1,
					sortNumber: 0,
					imageFile: []
				};
				this.editFormVisible = true;
			},
			handleEdit(row) {
				this.dialogTitle = '编辑弹窗';
				let form = Object.assign({}, row);
				form.imageFile = row.imagePath ? [{
					url: this.$http.publicUrl(row.imagePath),
					upUrl: row.imagePath,
					uid: 'popup'
				}] : [];
				this.editForm = form;
				this.validTime = (row.startTime && row.endTime) ? [row.startTime, row.endTime] : [];
				this.editFormVisible = true;
			},
			//弹窗图片尺寸校验：必须 750×1000
			beforePopupImageUpload(file) {
				const isLt6M = file.size / 1024 / 1024 < 6;
				if (!isLt6M) {
					this.$message({ showClose: true, message: '上传图片大小不能超过 6MB!', type: 'error' });
					return false;
				}
				//同步校验静态图片宽高（非图片格式直接放行由服务端判断）
				return true;
			},
			//异步校验图片尺寸（750×1000），不合规则取消上传
			validateImageSize(file) {
				let vue = this;
				return new Promise((resolve) => {
					if (!/\.(png|jpe?g|webp)$/i.test(file.name)) {
						resolve(true);
						return;
					}
					let reader = new FileReader();
					reader.onload = (e) => {
						let img = new Image();
						img.onload = () => {
							if (img.width !== 750 || img.height !== 1000) {
								vue.$message({
									showClose: true,
									message: '弹窗图片尺寸需为 750×1000，当前为 ' + img.width + '×' + img.height,
									type: 'error'
								});
								resolve(false);
							} else {
								resolve(true);
							}
						};
						img.onerror = () => resolve(true);
						img.src = e.target.result;
					};
					reader.onerror = () => resolve(true);
					reader.readAsDataURL(file);
				});
			},
			upload(option) {
				let vue = this;
				vue.validateImageSize(option.file).then((pass) => {
					if (!pass) {
						option.onError();
						return;
					}
					let formData = new FormData();
					formData.append('file', option.file);
					vue.$http.postupload(vue, '/rest/admin/uploadSingleImage', formData,
						function (vue, data) {
							option.onSuccess();
							vue.editForm.imageFile = [{
								url: vue.$http.publicUrl(data.data),
								uid: option.file.uid,
								upUrl: data.data
							}];
						},
						function (error) {
							option.onError();
						})
				});
			},
			handleRemoveImage() {
				this.editForm.imageFile = [];
			},
			handlePreview(row) {
				this.previewForm = row;
				this.previewVisible = true;
			},
			editSubmit() {
				let vue = this;
				vue.$refs['editForm'].validate((valid) => {
					if (!valid) {
						return false;
					}
					if (vue.validTime && vue.validTime.length === 2) {
						vue.editForm.startTime = vue.validTime[0];
						vue.editForm.endTime = vue.validTime[1];
					} else {
						vue.editForm.startTime = null;
						vue.editForm.endTime = null;
					}
					let param = Object.assign({}, vue.editForm);
					param.imagePath = vue.editForm.imageFile.length > 0 ? vue.editForm.imageFile[0].upUrl : null;
					delete param.imageFile;

					vue.editLoading = true;
					let isEdit = !!param.id;
					let url = isEdit ? '/rest/admin/advertisement/update' : '/rest/admin/advertisement/insert';
					let request = isEdit
						? (v, u, p, s, f) => v.$http.put(v, u, p, s, f)
						: (v, u, p, s, f) => v.$http.post(v, u, p, s, f);

					request(vue, url, param,
						(vue, data) => {
							vue.editLoading = false;
							vue.editFormVisible = false;
							vue.$message({ showClose: true, message: data.message, type: 'success' });
							vue.getList(isEdit ? undefined : 1);
						}, (error, data) => {
							vue.editLoading = false;
							vue.$message({ showClose: true, message: data.message, type: 'error' });
						}
					)
				});
			},
			handleDel(id) {
				let vue = this;
				vue.$confirm('确认删除该弹窗吗?', '提示', { type: 'warning' }).then(() => {
					vue.$http.delete(vue, '/rest/admin/advertisement/delete', { id: id },
						(vue, data) => {
							vue.$message({ showClose: true, message: data.message, type: 'success' });
							vue.getList();
						}, (error, data) => {
							vue.$message({ showClose: true, message: data.message, type: 'error' });
						}
					)
				}).catch(() => {});
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
	.avatar-uploader .el-upload {
		border: 1px dashed #d9d9d9;
		border-radius: 6px;
		cursor: pointer;
		position: relative;
		overflow: hidden;
	}
	.avatar-uploader .el-upload:hover {
		border-color: #409EFF;
	}
	.avatar-uploader-icon {
		font-size: 28px;
		color: #8c939d;
		width: 100px;
		height: 134px;
		line-height: 134px;
		text-align: center;
	}
	/* 预览：模拟客户端弹窗 */
	.preview-phone {
		padding: 10px 20px;
		text-align: center;
	}
	.preview-dialog {
		position: relative;
		display: inline-block;
		width: 240px;
		border-radius: 8px;
		overflow: hidden;
		background: #f5f7fa;
	}
	.preview-img {
		width: 240px;
		display: block;
	}
	.preview-empty {
		height: 320px;
		line-height: 320px;
		color: #909399;
	}
	.preview-close {
		position: absolute;
		right: 8px;
		top: 8px;
		width: 20px;
		height: 20px;
		line-height: 20px;
		border-radius: 50%;
		background: rgba(0, 0, 0, 0.4);
		color: #fff;
		font-size: 12px;
	}
	.preview-tip {
		margin-top: 15px;
		color: #909399;
		font-size: 12px;
	}
</style>