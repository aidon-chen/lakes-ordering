<template>
	<section>
		<el-row :gutter="20">
			<!-- 左侧：字典类型 -->
			<el-col :span="9">
				<div class="panel-layout">
					<div class="layout-title">字典类型</div>
					<div class="panel-toolbar">
						<el-form :inline="true" size="small">
							<el-form-item>
								<el-input v-model="typeSearchMsg.name" placeholder="类型名称/编码" clearable></el-input>
							</el-form-item>
							<el-form-item>
								<el-button type="primary" @click="getTypeList(1)">查询</el-button>
								<el-button @click="openTypeDialog()">新增类型</el-button>
							</el-form-item>
						</el-form>
					</div>
					<el-table :data="typeList" highlight-current-row v-loading="typeLoading" style="width: 100%;"
						:cell-style="cellStyle" :header-cell-style="headerCellStyle" @current-change="handleTypeChange">
						<el-table-column prop="code" label="类型编码" width="130"></el-table-column>
						<el-table-column prop="name" label="类型名称"></el-table-column>
						<el-table-column prop="remark" label="备注" show-overflow-tooltip></el-table-column>
						<el-table-column label="操作" width="140">
							<template slot-scope="scope">
								<el-button size="small" @click.stop="openTypeDialog(scope.row)">编辑</el-button>
								<el-button size="small" type="danger" @click.stop="handleTypeDel(scope.row)">删除</el-button>
							</template>
						</el-table-column>
					</el-table>
					<el-col :span="24" class="toolbar">
						<el-pagination
							@size-change="handleTypeSizeChange"
							@current-change="handleTypeCurrentChange"
							:page-sizes="[10, 20, 50]"
							:page-size="typeSearchMsg.pageSize"
							layout="total, prev, pager, next"
							:total="typeTotal"
							style="float:right;">
						</el-pagination>
					</el-col>
				</div>
			</el-col>

			<!-- 右侧：字典项 -->
			<el-col :span="15">
				<div class="panel-layout">
					<div class="layout-title">
						字典项
						<span v-if="currentType" class="current-type-tip">当前类型：{{currentType.name}}（{{currentType.code}}）</span>
						<span v-else class="current-type-tip">请在左侧选择字典类型</span>
					</div>
					<div class="panel-toolbar">
						<el-form :inline="true" size="small">
							<el-form-item>
								<el-input v-model="itemSearchMsg.label" placeholder="字典项名称" clearable></el-input>
							</el-form-item>
							<el-form-item>
								<el-button type="primary" @click="getItemList(1)">查询</el-button>
								<el-button :disabled="!currentType" @click="openItemDialog()">新增字典项</el-button>
							</el-form-item>
						</el-form>
					</div>
					<el-table :data="itemList" v-loading="itemLoading" style="width: 100%;"
						:cell-style="cellStyle" :header-cell-style="headerCellStyle">
						<el-table-column prop="label" label="字典项名称"></el-table-column>
						<el-table-column prop="value" label="键值"></el-table-column>
						<el-table-column prop="sort" label="排序" width="80"></el-table-column>
						<el-table-column prop="status" label="状态" width="90">
							<template slot-scope="scope">
								<el-tag v-if="scope.row.status == 1" type="success" size="small">启用</el-tag>
								<el-tag v-else type="info" size="small">停用</el-tag>
							</template>
						</el-table-column>
						<el-table-column prop="remark" label="备注" show-overflow-tooltip></el-table-column>
						<el-table-column label="操作" width="150">
							<template slot-scope="scope">
								<el-button size="small" @click="openItemDialog(scope.row)">编辑</el-button>
								<el-button size="small" type="danger" @click="handleItemDel(scope.row)">删除</el-button>
							</template>
						</el-table-column>
					</el-table>
					<el-col :span="24" class="toolbar">
						<el-pagination
							@size-change="handleItemSizeChange"
							@current-change="handleItemCurrentChange"
							:page-sizes="[10, 20, 50]"
							:page-size="itemSearchMsg.pageSize"
							layout="total, prev, pager, next"
							:total="itemTotal"
							style="float:right;">
						</el-pagination>
					</el-col>
				</div>
			</el-col>
		</el-row>

		<!--类型编辑弹层-->
		<el-dialog :title="typeDialogTitle" :visible.sync="typeDialogVisible" :close-on-click-modal="false" width="480px">
			<el-form :model="typeForm" label-width="100px" :rules="typeRules" ref="typeForm">
				<el-form-item label="类型编码" prop="code">
					<el-input v-model="typeForm.code" placeholder="如 cup_size"></el-input>
				</el-form-item>
				<el-form-item label="类型名称" prop="name">
					<el-input v-model="typeForm.name" placeholder="如 杯型"></el-input>
				</el-form-item>
				<el-form-item label="备注" prop="remark">
					<el-input type="textarea" :rows="3" v-model="typeForm.remark"></el-input>
				</el-form-item>
			</el-form>
			<div slot="footer" class="dialog-footer">
				<el-button @click="typeDialogVisible = false">取消</el-button>
				<el-button type="primary" @click="typeSubmit" :loading="typeSubmitLoading">提交</el-button>
			</div>
		</el-dialog>

		<!--字典项编辑弹层-->
		<el-dialog :title="itemDialogTitle" :visible.sync="itemDialogVisible" :close-on-click-modal="false" width="480px">
			<el-form :model="itemForm" label-width="100px" :rules="itemRules" ref="itemForm">
				<el-form-item label="所属类型">
					<span>{{itemForm.typeCode}}</span>
				</el-form-item>
				<el-form-item label="字典项名称" prop="label">
					<el-input v-model="itemForm.label" placeholder="如 中杯"></el-input>
				</el-form-item>
				<el-form-item label="键值" prop="value">
					<el-input v-model="itemForm.value" placeholder="如 middle"></el-input>
				</el-form-item>
				<el-form-item label="排序" prop="sort">
					<el-input v-model="itemForm.sort"></el-input>
				</el-form-item>
				<el-form-item label="状态" prop="status">
					<el-radio-group v-model="itemForm.status">
						<el-radio :label="1">启用</el-radio>
						<el-radio :label="2">停用</el-radio>
					</el-radio-group>
				</el-form-item>
				<el-form-item label="备注" prop="remark">
					<el-input type="textarea" :rows="3" v-model="itemForm.remark"></el-input>
				</el-form-item>
			</el-form>
			<div slot="footer" class="dialog-footer">
				<el-button @click="itemDialogVisible = false">取消</el-button>
				<el-button type="primary" @click="itemSubmit" :loading="itemSubmitLoading">提交</el-button>
			</div>
		</el-dialog>
	</section>
</template>

<script>
	export default {
		data() {
			return {
				//字典类型
				typeSearchMsg: {
					pageNo: 1,
					pageSize: 10,
					name: ''
				},
				typeList: [],
				typeTotal: 0,
				typeLoading: false,
				currentType: null, //当前选中的字典类型
				typeDialogVisible: false,
				typeDialogTitle: '新增类型',
				typeSubmitLoading: false,
				typeForm: {},
				typeRules: {
					code: [{ required: true, message: '请输入类型编码', trigger: 'blur' }],
					name: [{ required: true, message: '请输入类型名称', trigger: 'blur' }]
				},

				//字典项
				itemSearchMsg: {
					pageNo: 1,
					pageSize: 10,
					typeCode: '',
					label: ''
				},
				itemList: [],
				itemTotal: 0,
				itemLoading: false,
				itemDialogVisible: false,
				itemDialogTitle: '新增字典项',
				itemSubmitLoading: false,
				itemForm: {
					status: 1,
					sort: 0
				},
				itemRules: {
					label: [{ required: true, message: '请输入字典项名称', trigger: 'blur' }],
					value: [{ required: true, message: '请输入键值', trigger: 'blur' }]
				}
			}
		},
		methods: {
			cellStyle() {
				return "text-align:center";
			},
			headerCellStyle() {
				return "text-align:center";
			},

			// ============ 字典类型 ============
			getTypeList(pageNoParam) {
				if (pageNoParam) {
					this.typeSearchMsg.pageNo = pageNoParam;
				}
				let vue = this;
				let param = Object.assign({}, vue.typeSearchMsg);
				vue.typeLoading = true;
				vue.$http.post(vue, '/rest/admin/dict/type/list', param,
					(vue, data) => {
						vue.typeList = data.data.records;
						vue.typeTotal = data.data.total;
						vue.typeLoading = false;
						//默认选中第一行，便于右侧直接操作
						if (!vue.currentType && vue.typeList.length > 0) {
							vue.handleTypeChange(vue.typeList[0]);
						}
					}, (error, data) => {
						vue.typeLoading = false;
						vue.$message({ showClose: true, message: data.message, type: 'error' });
					}
				)
			},
			handleTypeChange(row) {
				if (!row) {
					return;
				}
				this.currentType = row;
				this.itemSearchMsg.typeCode = row.code;
				this.itemSearchMsg.pageNo = 1;
				this.getItemList();
			},
			openTypeDialog(row) {
				if (row) {
					this.typeDialogTitle = '编辑类型';
					this.typeForm = Object.assign({}, row);
				} else {
					this.typeDialogTitle = '新增类型';
					this.typeForm = {};
				}
				this.typeDialogVisible = true;
			},
			typeSubmit() {
				let vue = this;
				vue.$refs['typeForm'].validate((valid) => {
					if (!valid) {
						return false;
					}
					vue.typeSubmitLoading = true;
					let isEdit = !!vue.typeForm.id;
					let url = isEdit ? '/rest/admin/dict/type/update' : '/rest/admin/dict/type/insert';
					let request = isEdit
						? (v, u, p, s, f) => v.$http.put(v, u, p, s, f)
						: (v, u, p, s, f) => v.$http.post(v, u, p, s, f);

					request(vue, url, vue.typeForm,
						(vue, data) => {
							vue.typeSubmitLoading = false;
							vue.typeDialogVisible = false;
							vue.$message({ showClose: true, message: data.message, type: 'success' });
							vue.getTypeList(isEdit ? undefined : 1);
						}, (error, data) => {
							vue.typeSubmitLoading = false;
							vue.$message({ showClose: true, message: data.message, type: 'error' });
						}
					)
				});
			},
			handleTypeDel(row) {
				let vue = this;
				vue.$confirm('确认删除该字典类型吗?', '提示', { type: 'warning' }).then(() => {
					vue.$http.delete(vue, '/rest/admin/dict/type/delete', { id: row.id },
						(vue, data) => {
							vue.$message({ showClose: true, message: data.message, type: 'success' });
							//删除的是当前选中类型时，清空右侧
							if (vue.currentType && vue.currentType.id === row.id) {
								vue.currentType = null;
								vue.itemList = [];
								vue.itemTotal = 0;
								vue.itemSearchMsg.typeCode = '';
							}
							vue.getTypeList();
						}, (error, data) => {
							vue.$message({ showClose: true, message: data.message, type: 'error' });
						}
					)
				}).catch(() => {});
			},
			handleTypeSizeChange(val) {
				this.typeSearchMsg.pageSize = val;
				this.getTypeList();
			},
			handleTypeCurrentChange(val) {
				this.typeSearchMsg.pageNo = val;
				this.getTypeList();
			},

			// ============ 字典项 ============
			getItemList(pageNoParam) {
				if (pageNoParam) {
					this.itemSearchMsg.pageNo = pageNoParam;
				}
				if (!this.itemSearchMsg.typeCode) {
					this.itemList = [];
					this.itemTotal = 0;
					return false;
				}
				let vue = this;
				let param = Object.assign({}, vue.itemSearchMsg);
				vue.itemLoading = true;
				vue.$http.post(vue, '/rest/admin/dict/item/list', param,
					(vue, data) => {
						vue.itemList = data.data.records;
						vue.itemTotal = data.data.total;
						vue.itemLoading = false;
					}, (error, data) => {
						vue.itemLoading = false;
						vue.$message({ showClose: true, message: data.message, type: 'error' });
					}
				)
			},
			openItemDialog(row) {
				if (row) {
					this.itemDialogTitle = '编辑字典项';
					this.itemForm = Object.assign({}, row);
				} else {
					this.itemDialogTitle = '新增字典项';
					this.itemForm = {
						typeCode: this.itemSearchMsg.typeCode,
						status: 1,
						sort: 0
					};
				}
				this.itemDialogVisible = true;
			},
			itemSubmit() {
				let vue = this;
				vue.$refs['itemForm'].validate((valid) => {
					if (!valid) {
						return false;
					}
					vue.itemSubmitLoading = true;
					let isEdit = !!vue.itemForm.id;
					let url = isEdit ? '/rest/admin/dict/item/update' : '/rest/admin/dict/item/insert';
					let request = isEdit
						? (v, u, p, s, f) => v.$http.put(v, u, p, s, f)
						: (v, u, p, s, f) => v.$http.post(v, u, p, s, f);

					request(vue, url, vue.itemForm,
						(vue, data) => {
							vue.itemSubmitLoading = false;
							vue.itemDialogVisible = false;
							vue.$message({ showClose: true, message: data.message, type: 'success' });
							vue.getItemList(isEdit ? undefined : 1);
						}, (error, data) => {
							vue.itemSubmitLoading = false;
							vue.$message({ showClose: true, message: data.message, type: 'error' });
						}
					)
				});
			},
			handleItemDel(row) {
				let vue = this;
				vue.$confirm('确认删除该字典项吗?', '提示', { type: 'warning' }).then(() => {
					vue.$http.delete(vue, '/rest/admin/dict/item/delete', { id: row.id },
						(vue, data) => {
							vue.$message({ showClose: true, message: data.message, type: 'success' });
							vue.getItemList();
						}, (error, data) => {
							vue.$message({ showClose: true, message: data.message, type: 'error' });
						}
					)
				}).catch(() => {});
			},
			handleItemSizeChange(val) {
				this.itemSearchMsg.pageSize = val;
				this.getItemList();
			},
			handleItemCurrentChange(val) {
				this.itemSearchMsg.pageNo = val;
				this.getItemList();
			}
		},
		mounted() {
			this.getTypeList();
		}
	}
</script>

<style scoped>
	.panel-layout {
		border: 1px solid #DCDFE6;
	}
	.layout-title {
		color: #606266;
		padding: 15px 20px;
		background: #F2F6FC;
		font-weight: bold;
	}
	.panel-toolbar {
		padding: 10px 20px 0 20px;
	}
	.current-type-tip {
		margin-left: 15px;
		font-weight: normal;
		font-size: 13px;
		color: #909399;
	}
</style>