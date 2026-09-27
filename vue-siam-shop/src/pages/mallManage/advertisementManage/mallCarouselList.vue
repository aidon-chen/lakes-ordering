<template>
	<section>
		<!--工具条-->
		<el-col :span="24" class="toolbar" style="padding-bottom: 0px;">
			<el-form :inline="true" :model="searchMsg" size="small">
				<el-form-item>
					<el-input v-model="searchMsg.imageName" placeholder="请输入轮播名称搜索" clearable></el-input>
				</el-form-item>
				<el-form-item>
					<el-button type="primary" @click="getList(1)">查询</el-button>
					<el-button type="primary" @click="handleAdd">新增轮播</el-button>
				</el-form-item>
				<el-form-item>
					<span class="spec-tip">规格：750×380（横版，约 2:1），客户端按此比例裁切铺满</span>
				</el-form-item>
			</el-form>
		</el-col>

		<!--列表-->
		<el-table :data="list" highlight-current-row v-loading="listLoading" style="width: 100%;"
			:cell-style="cellStyle" :header-cell-style="headerCellStyle">
			<el-table-column label="轮播预览图" width="140">
				<template slot-scope="scope">
					<img v-if="scope.row.imagePath" :src="$http.publicUrl(scope.row.imagePath)" width="112" height="57">
					<span v-else>-</span>
				</template>
			</el-table-column>
			<el-table-column prop="imageName" label="轮播名称">
				<template slot-scope="scope">
					<span>{{ scope.row.imageName || '-' }}</span>
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

		<!--分页-->
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
				<el-form-item label="轮播名称" prop="imageName">
					<el-input v-model="editForm.imageName" placeholder="仅后台备注用，如：开业活动轮播（客户端不展示文字）"></el-input>
				</el-form-item>
				<el-form-item label="轮播图片(750×380)" prop="imageFile">
					<el-upload
						class="avatar-uploader"
						action=""
						name="mallCarouselUpload"
						:limit="1"
						:file-list="editForm.imageFile"
						:before-upload="beforeCarouselImageUpload"
						:http-request="upload"
						:on-remove="handleRemoveImage"
						list-type="picture-card">
						<i class="el-icon-plus avatar-uploader-icon"></i>
					</el-upload>
					<div class="field-tip">建议 750×380（横版），允许 ±3px 误差；比例不符会被客户端裁切</div>
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
					<el-input v-model="editForm.imageLinkUrl" placeholder="点击轮播跳转的小程序页面路径（可空，如 /pages/mine/share/index/index）"></el-input>
				</el-form-item>
				<el-form-item label="排序" prop="sortNumber">
					<el-input v-model="editForm.sortNumber" placeholder="数字越小越靠前，留空按 0 处理"></el-input>
				</el-form-item>
			</el-form>
			<div slot="footer" class="dialog-footer">
				<el-button @click="editFormVisible = false">取消</el-button>
				<el-button type="primary" @click="editSubmit" :loading="editLoading">提交</el-button>
			</div>
		</el-dialog>

		<!--预览弹层（模拟商城首页轮播效果）-->
		<el-dialog title="轮播预览（模拟商城首页效果）" :visible.sync="previewVisible" :close-on-click-modal="false" width="420px">
			<div class="preview-phone">
				<div class="preview-carousel">
					<img v-if="previewForm.imagePath" :src="$http.publicUrl(previewForm.imagePath)" class="preview-img">
					<div v-else class="preview-empty">未上传轮播图</div>
					<div class="preview-dots">
						<i class="dot is-active"></i><i class="dot"></i><i class="dot"></i>
					</div>
				</div>
				<p class="preview-tip">排序：{{ previewForm.sortNumber == null ? '-' : previewForm.sortNumber }}｜跳转：{{ previewForm.imageLinkUrl || '无' }}</p>
				<p class="preview-tip">客户端商城首页顶部轮播；多张按排序号从左到右依次播放</p>
			</div>
			<div slot="footer" class="dialog-footer">
				<el-button @click.native="previewVisible = false">关闭</el-button>
			</div>
		</el-dialog>
	</section>
</template>

<script>
	/**
	 * 商城管理 · 商城轮播（tb_advertisement type=3）管理
	 *
	 * 底座取自营销中心同目录 homePosterList.vue（type=1 首页海报），按轮播语义做了三处适配：
	 *   1) type 由 1 改为 3 —— 顾客端「商城首页」顶部轮播读的就是 type=3
	 *      （uniapp-siam-user/pages/internal/mall/index/index.vue → getCarouselList）
	 *   2) 尺寸由 750×1140（竖幅）改为 750×380（横版约 2:1）—— 对应客户端 .carousel-swiper 的 710rpx × 358rpx
	 *   3) 去掉「副标题」字段并改预览样式 —— 客户端轮播只渲染图片，不叠加任何文字
	 *
	 * 接口：/rest/admin/advertisement/{list,insert,update,delete}（与首页海报/活动弹窗同一套，后端零改动）
	 * 上传：/rest/admin/uploadSingleImage
	 * 字段语义：imageName = 轮播名称（仅后台备注）；description 本页不使用
	 */
	const CAROUSEL_WIDTH = 750;
	const CAROUSEL_HEIGHT = 380;
	const CAROUSEL_SIZE_TOLERANCE = 3; //允许 ±3px，避免等比导图后的 1px 误差挡住上传

	export default {
		data() {
			return {
				searchMsg: {
					pageNo: 1,
					pageSize: 10,
					type: 3 //仅管理商城轮播（type=3）
				},
				list: [],
				total: 0,
				listLoading: false,
				editFormVisible: false,
				editLoading: false,
				dialogTitle: '新增轮播',
				validTime: [], //生效时间区间
				editForm: {
					type: 3,
					sortNumber: 0,
					imageFile: []
				},
				editFormRules: {
					imageFile: [{ type: 'array', required: true, message: '请上传轮播图片', trigger: 'change' }]
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
			//状态：1=生效中 2=未开始 3=已过期（无独立启停字段，按生效时间判定；空 start/end 视为长期）
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
				this.dialogTitle = '新增轮播';
				this.validTime = [];
				this.editForm = {
					type: 3,
					sortNumber: 0,
					imageFile: []
				};
				this.editFormVisible = true;
			},
			handleEdit(row) {
				this.dialogTitle = '编辑轮播';
				let form = Object.assign({}, row);
				form.imageFile = row.imagePath ? [{
					url: this.$http.publicUrl(row.imagePath),
					upUrl: row.imagePath,
					uid: 'mallCarousel'
				}] : [];
				this.editForm = form;
				this.validTime = (row.startTime && row.endTime) ? [row.startTime, row.endTime] : [];
				this.editFormVisible = true;
			},
			//同步校验：大小上限 6MB
			beforeCarouselImageUpload(file) {
				const isLt6M = file.size / 1024 / 1024 < 6;
				if (!isLt6M) {
					this.$message({ showClose: true, message: '上传图片大小不能超过 6MB!', type: 'error' });
					return false;
				}
				return true;
			},
			//异步校验图片尺寸（750×380 ± 容差），不合规取消上传
			validateImageSize(file) {
				let vue = this;
				return new Promise((resolve) => {
					if (!/\.(png|jpe?g|webp)$/i.test(file.name)) {
						resolve(true); //非图片扩展名交给后端判定
						return;
					}
					let reader = new FileReader();
					reader.onload = (e) => {
						let img = new Image();
						img.onload = () => {
							const okW = Math.abs(img.width - CAROUSEL_WIDTH) <= CAROUSEL_SIZE_TOLERANCE;
							const okH = Math.abs(img.height - CAROUSEL_HEIGHT) <= CAROUSEL_SIZE_TOLERANCE;
							if (!okW || !okH) {
								vue.$message({
									showClose: true,
									message: '商城轮播尺寸需为 ' + CAROUSEL_WIDTH + '×' + CAROUSEL_HEIGHT +
										'（横版约 2:1），当前为 ' + img.width + '×' + img.height + '，请调整后重新上传',
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
					//排序号归一为数字；留空按 0 提交（后端 updateById 会跳过 null，留空将改不动原值）
					let sortNumber = vue.editForm.sortNumber;
					sortNumber = (sortNumber === '' || sortNumber === null || sortNumber === undefined) ? 0 : Number(sortNumber);
					if (isNaN(sortNumber)) {
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
					param.type = 3;

					vue.editLoading = true;
					let isEdit = !!param.id;
					let url = isEdit ? '/rest/admin/advertisement/update' : '/rest/admin/advertisement/insert';
					//后端 update 是 @PutMapping，insert 是 @PostMapping
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
				vue.$confirm('确认删除该轮播图吗？删除后商城首页不再展示。', '提示', { type: 'warning' }).then(() => {
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
		max-width: 220px;
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
		width: 112px;
		height: 57px;
		line-height: 57px;
		text-align: center;
	}
	/* 预览：模拟商城首页顶部轮播（750×380 → 280×142） */
	.preview-phone {
		padding: 10px 20px;
		text-align: center;
	}
	.preview-carousel {
		position: relative;
		display: inline-block;
		width: 280px;
		height: 142px;
		border-radius: 8px;
		overflow: hidden;
		background: #F1EFE8;
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
		top: 45%;
		width: 100%;
		text-align: center;
		color: #909399;
		font-size: 12px;
	}
	.preview-dots {
		position: absolute;
		left: 0;
		right: 0;
		bottom: 8px;
		text-align: center;
		line-height: 0;
	}
	.preview-dots .dot {
		display: inline-block;
		width: 6px;
		height: 6px;
		margin: 0 3px;
		border-radius: 3px;
		background: rgba(255, 255, 255, 0.6);
	}
	.preview-dots .dot.is-active {
		width: 16px;
		background: #FFFFFF;
	}
	.preview-tip {
		margin-top: 12px;
		color: #909399;
		font-size: 12px;
	}
</style>
