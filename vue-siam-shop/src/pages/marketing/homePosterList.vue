<template>
	<section>
		<!--工具条-->
		<el-col :span="24" class="toolbar" style="padding-bottom: 0px;">
			<el-form :inline="true" :model="searchMsg" size="small">
				<el-form-item>
					<el-input v-model="searchMsg.imageName" placeholder="请输入海报主标题搜索" clearable></el-input>
				</el-form-item>
				<el-form-item>
					<el-button type="primary" @click="getList(1)">查询</el-button>
					<el-button type="primary" @click="handleAdd">新增海报</el-button>
				</el-form-item>
				<el-form-item>
					<span class="spec-tip">规格：750×1140（竖幅），超出会按此比例裁切</span>
				</el-form-item>
			</el-form>
		</el-col>

		<!--列表-->
		<el-table :data="list" highlight-current-row v-loading="listLoading" style="width: 100%;"
			:cell-style="cellStyle" :header-cell-style="headerCellStyle">
			<el-table-column label="海报预览图" width="110">
				<template slot-scope="scope">
					<img v-if="scope.row.imagePath" :src="$http.publicUrl(scope.row.imagePath)" width="56" height="85">
					<span v-else>-</span>
				</template>
			</el-table-column>
			<el-table-column prop="imageName" label="主标题">
				<template slot-scope="scope">
					<span>{{ scope.row.imageName || '-' }}</span>
				</template>
			</el-table-column>
			<el-table-column prop="description" label="副标题">
				<template slot-scope="scope">
					<span class="sub-text">{{ scope.row.description || '-' }}</span>
				</template>
			</el-table-column>
			<el-table-column prop="imageLinkUrl" label="点击跳转链接">
				<template slot-scope="scope">
					<span class="sub-text">{{ scope.row.imageLinkUrl || '-' }}</span>
				</template>
			</el-table-column>
			<el-table-column prop="sortNumber" label="排序" width="70"></el-table-column>
			<el-table-column prop="startTime" label="生效开始时间" :formatter="formatTime" width="170"></el-table-column>
			<el-table-column prop="endTime" label="生效结束时间" :formatter="formatTime" width="170"></el-table-column>
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
			<el-form :model="editForm" label-width="150px" :rules="editFormRules" ref="editForm">
				<el-form-item label="海报主标题" prop="imageName">
					<el-input v-model="editForm.imageName" placeholder="首页海报上方的白色主标题，如：中秋主题夜"></el-input>
				</el-form-item>
				<el-form-item label="海报副标题" prop="description">
					<el-input v-model="editForm.description" placeholder="主标题下方的副标题，如：9/25 乐队现场 · 双款特调首发"></el-input>
				</el-form-item>
				<el-form-item label="海报图片(750×1140)" prop="imageFile">
					<el-upload
						class="avatar-uploader"
						action=""
						name="homePosterUpload"
						:limit="1"
						:file-list="editForm.imageFile"
						:before-upload="beforePosterImageUpload"
						:http-request="upload"
						:on-remove="handleRemoveImage"
						list-type="picture-card">
						<i class="el-icon-plus avatar-uploader-icon"></i>
					</el-upload>
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
					<div class="field-tip">留空表示长期展示；到期后客户端自动不再下发</div>
				</el-form-item>
				<el-form-item label="点击跳转链接" prop="imageLinkUrl">
					<el-input v-model="editForm.imageLinkUrl" placeholder="点击海报跳转的小程序页面路径（可空）"></el-input>
				</el-form-item>
				<el-form-item label="排序" prop="sortNumber">
					<el-input v-model="editForm.sortNumber" placeholder="数字越小越靠前"></el-input>
				</el-form-item>
			</el-form>
			<div slot="footer" class="dialog-footer">
				<el-button @click="editFormVisible = false">取消</el-button>
				<el-button type="primary" @click="editSubmit" :loading="editLoading">提交</el-button>
			</div>
		</el-dialog>

		<!--预览弹层（模拟客户端首页海报区效果）-->
		<el-dialog title="海报预览（模拟客户端首页效果）" :visible.sync="previewVisible" :close-on-click-modal="false" width="420px">
			<div class="preview-phone">
				<div class="preview-hero">
					<img v-if="previewForm.imagePath" :src="$http.publicUrl(previewForm.imagePath)" class="preview-img">
					<div v-else class="preview-empty">未上传海报图</div>
					<div class="preview-mask"></div>
					<div class="preview-title">{{ previewForm.imageName || '（无主标题）' }}</div>
					<div class="preview-subtitle">{{ previewForm.description || '' }}</div>
					<div class="preview-dots">
						<i class="dot is-active"></i><i class="dot"></i><i class="dot"></i>
					</div>
				</div>
				<p class="preview-tip">排序：{{ previewForm.sortNumber == null ? '-' : previewForm.sortNumber }}｜跳转：{{ previewForm.imageLinkUrl || '无' }}</p>
			</div>
			<div slot="footer" class="dialog-footer">
				<el-button @click.native="previewVisible = false">关闭</el-button>
			</div>
		</el-dialog>
	</section>
</template>

<script>
	/**
	 * A-10 营销管理 · 首页海报（type=1）管理
	 * 底座迁移自调度后台 vue-siam-admin/pages/shopDecoration/advertisementList.vue（R3 单一入口），
	 * 增强能力取自同目录 activityPopupList.vue：生效时间 / 状态标签 / 图片尺寸校验 / 预览层。
	 * 接口：/rest/admin/advertisement（与活动弹窗同一套）；上传：/rest/admin/uploadSingleImage。
	 * 字段语义：imageName = 海报主标题；description = 海报副标题（R7，不加表字段）。
	 * 规格：750×1140（R1，与客户端首页 hero 一致）。
	 */
	const POSTER_WIDTH = 750;
	const POSTER_HEIGHT = 1140;

	export default {
		data() {
			return {
				searchMsg: {
					pageNo: 1,
					pageSize: 10,
					type: 1 //A-10：仅管理首页海报（type=1），与活动弹窗页 type=2 对称
				},
				list: [],
				total: 0,
				listLoading: false,
				editFormVisible: false,
				editLoading: false,
				dialogTitle: '新增海报',
				validTime: [], //生效时间区间
				editForm: {
					type: 1,
					sortNumber: 0,
					imageFile: []
				},
				editFormRules: {
					imageName: [{ required: true, message: '请输入海报主标题', trigger: 'blur' }],
					imageFile: [{ type: 'array', required: true, message: '请上传海报图片', trigger: 'change' }]
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
					return 1; // 海报无生效时间 = 长期展示（与活动弹窗口径不同：海报默认长期）
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
				this.dialogTitle = '新增海报';
				this.validTime = [];
				this.editForm = {
					type: 1,
					sortNumber: 0,
					imageFile: []
				};
				this.editFormVisible = true;
			},
			handleEdit(row) {
				this.dialogTitle = '编辑海报';
				let form = Object.assign({}, row);
				form.imageFile = row.imagePath ? [{
					url: this.$http.publicUrl(row.imagePath),
					upUrl: row.imagePath,
					uid: 'homePoster'
				}] : [];
				this.editForm = form;
				this.validTime = (row.startTime && row.endTime) ? [row.startTime, row.endTime] : [];
				this.editFormVisible = true;
			},
			//同步校验：大小上限 6MB
			beforePosterImageUpload(file) {
				const isLt6M = file.size / 1024 / 1024 < 6;
				if (!isLt6M) {
					this.$message({ showClose: true, message: '上传图片大小不能超过 6MB!', type: 'error' });
					return false;
				}
				return true;
			},
			//异步校验图片尺寸（750×1140，R1），不合规则取消上传
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
							if (img.width !== POSTER_WIDTH || img.height !== POSTER_HEIGHT) {
								vue.$message({
									showClose: true,
									message: '首页海报尺寸需为 ' + POSTER_WIDTH + '×' + POSTER_HEIGHT + '，当前为 ' + img.width + '×' + img.height + '，请调整后重新上传',
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
					//排序号归一为数字（后端 sort_number 为 INT）
					let sortNumber = vue.editForm.sortNumber;
					sortNumber = (sortNumber === '' || sortNumber === null || sortNumber === undefined) ? null : Number(sortNumber);
					if (sortNumber !== null && isNaN(sortNumber)) {
						vue.$message({ showClose: true, message: '排序号需为数字', type: 'error' });
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
					param.sortNumber = sortNumber;

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
				vue.$confirm('确认删除该海报吗？删除后客户端首页不再展示。', '提示', { type: 'warning' }).then(() => {
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
	.spec-tip {
		color: #909399;
		font-size: 12px;
	}
	.field-tip {
		color: #909399;
		font-size: 12px;
		line-height: 1.6;
	}
	.sub-text {
		display: inline-block;
		max-width: 160px;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
		vertical-align: middle;
	}
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
		height: 138px;
		line-height: 138px;
		text-align: center;
	}
	/* 预览：模拟客户端首页海报区（750×1140 → 240×365） */
	.preview-phone {
		padding: 10px 20px;
		text-align: center;
	}
	.preview-hero {
		position: relative;
		display: inline-block;
		width: 240px;
		height: 365px;
		border-radius: 8px;
		overflow: hidden;
		background: linear-gradient(180deg, #253730 0%, #4F6B5E 55%, #739B8D 100%);
		text-align: left;
	}
	.preview-img {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		object-fit: cover;
		display: block;
	}
	.preview-empty {
		position: absolute;
		left: 0;
		top: 40%;
		width: 100%;
		text-align: center;
		color: rgba(255, 255, 255, 0.7);
		font-size: 12px;
	}
	.preview-mask {
		position: absolute;
		left: 0;
		right: 0;
		bottom: 0;
		height: 40%;
		background: linear-gradient(180deg, rgba(0, 0, 0, 0) 0%, rgba(0, 0, 0, 0.55) 100%);
	}
	.preview-title {
		position: absolute;
		left: 13px;
		bottom: 40px;
		color: #FFFFFF;
		font-size: 17px;
		font-weight: 700;
		line-height: 1.3;
	}
	.preview-subtitle {
		position: absolute;
		left: 13px;
		bottom: 24px;
		color: #FFFFFF;
		font-size: 11px;
		line-height: 1.3;
	}
	.preview-dots {
		position: absolute;
		left: 0;
		right: 0;
		bottom: 10px;
		text-align: center;
		line-height: 0;
	}
	.preview-dots .dot {
		display: inline-block;
		width: 6px;
		height: 6px;
		margin: 0 3px;
		border-radius: 3px;
		background: rgba(255, 255, 255, 0.44);
	}
	.preview-dots .dot.is-active {
		width: 16px;
		background: #FFFFFF;
	}
	.preview-tip {
		margin-top: 15px;
		color: #909399;
		font-size: 12px;
	}
</style>
