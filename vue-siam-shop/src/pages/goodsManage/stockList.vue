<template>
	<section>
		<!--工具条-->
		<el-col :span="24" class="toolbar" style="padding-bottom: 0px;">
			<el-form :inline="true" :model="searchMsg">
				<el-form-item>
					<el-input v-model="searchMsg.name" placeholder="请输入商品名称搜索" clearable></el-input>
				</el-form-item>
				<el-form-item>
					<el-button type="primary" @click="getList(1)">查询</el-button>
				</el-form-item>
				<el-form-item>
					<el-button type="primary" :disabled="sels.length === 0" @click="openBatchDialog">批量改库存</el-button>
				</el-form-item>
			</el-form>
		</el-col>
		<!--列表-->
		<el-table :data="list" highlight-current-row v-loading="listLoading" @selection-change="handleSelectionChange" style="width: 100%;" :cell-style="cellStyle" :header-cell-style="headerCellStyle">
			<el-table-column type="selection" width="55"></el-table-column>
			<el-table-column label="商品主图" prop="mainImage" width="90">
				<template slot-scope="scope">
					<img :src="$http.publicUrl(scope.row.mainImage)" width="50" height="50">
				</template>
			</el-table-column>
			<el-table-column prop="name" label="商品名称"></el-table-column>
			<el-table-column prop="bottleStock" label="按瓶库存" width="100">
				<template slot-scope="scope">
					<span>{{ scope.row.bottleStock === null || scope.row.bottleStock === undefined ? '-' : scope.row.bottleStock }}</span>
				</template>
			</el-table-column>
			<el-table-column prop="cupStock" label="按杯库存" width="100">
				<template slot-scope="scope">
					<span>{{ scope.row.cupStock === null || scope.row.cupStock === undefined ? '-' : scope.row.cupStock }}</span>
				</template>
			</el-table-column>
			<el-table-column prop="stockWarningThreshold" label="预警阈值" width="100">
				<template slot-scope="scope">
					<span>{{ thresholdOf(scope.row) }}</span>
				</template>
			</el-table-column>
			<el-table-column label="库存状态" width="100">
				<template slot-scope="scope">
					<span :style="stockStateStyle(scope.row)">{{ stockStateText(scope.row) }}</span>
				</template>
			</el-table-column>
			<el-table-column label="操作" fixed="right" width="110">
				<template slot-scope="scope">
					<el-button size="small" @click="handleEdit(scope.$index, scope.row)">改库存</el-button>
				</template>
			</el-table-column>
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

		<!--编辑界面-->
		<el-dialog :title="batchMode ? ('批量改库存（已选 ' + sels.length + ' 件商品）') : ('改库存 · ' + (editForm.name || ''))" :visible.sync="editFormVisible" @close="editFormClose" :close-on-click-modal="false">
			<el-form size="small" :model="editForm" class="editForm" label-width="110px" style="width: 85%;">
				<el-form-item label="按瓶库存">
					<el-input v-model="editForm.bottleStock" placeholder="按瓶库存（留空则不修改）"></el-input>
				</el-form-item>
				<el-form-item label="按杯库存">
					<el-input v-model="editForm.cupStock" placeholder="按杯库存（留空则不修改）"></el-input>
				</el-form-item>
				<el-form-item label="预警阈值">
					<el-input v-model="editForm.stockWarningThreshold" placeholder="低于等于阈值标「预警」（留空则不修改）"></el-input>
				</el-form-item>
			</el-form>
			<div slot="footer" class="dialog-footer">
				<el-button @click.native="editFormVisible = false">取消</el-button>
				<el-button type="primary" @click.native="editSubmit" :loading="editLoading">提交</el-button>
			</div>
		</el-dialog>
	</section>
</template>

<script>
	/**
	 * 商品库存（A-05-5）
	 * 口径（grill session 2026-09-19）：按瓶/按杯两列均为**人工维护**，无自动扣减、不做一瓶=N杯换算；
	 * 预警阈值仅用于展示标色（充足/预警/售罄），不参与任何校验与拦截。
	 * 状态判定值：优先「按杯库存」，为空时退回「按瓶库存」；阈值按行维护，缺省 10。
	 */
	export default {
		data() {
			return {
				searchMsg: {
					pageNo: 1,
					pageSize: 20,
					name: ''
				},
				list: [],
				total: 0,
				listLoading: false,
				sels: [],//列表选中列
				batchMode: false,
				editFormVisible: false,//编辑界面是否显示
				editLoading: false,
				//编辑界面数据（字符串承载，便于区分「留空=不修改」）
				editForm: {
					id: null,
					name: '',
					bottleStock: '',
					cupStock: '',
					stockWarningThreshold: ''
				}
			}
		},
		methods: {
			cellStyle({row, column, rowIndex, columnIndex}){
				return "text-align:center";
			},
			headerCellStyle({row, rowIndex}){
				return "text-align:center";
			},
			// 状态判定值：优先按杯，退回按瓶
			primaryStock(row) {
				let cup = row.cupStock;
				if (cup !== null && cup !== undefined && cup !== '') {
					return Number(cup);
				}
				let bottle = row.bottleStock;
				if (bottle !== null && bottle !== undefined && bottle !== '') {
					return Number(bottle);
				}
				return 0;
			},
			thresholdOf(row) {
				let t = row.stockWarningThreshold;
				if (t === null || t === undefined || t === '') {
					return 10;
				}
				return Number(t);
			},
			stockStateOf(row) {
				let stock = this.primaryStock(row);
				if (stock <= 0) return 'soldout';
				if (stock <= this.thresholdOf(row)) return 'warning';
				return 'enough';
			},
			stockStateText(row) {
				let state = this.stockStateOf(row);
				if (state === 'soldout') return '售罄';
				if (state === 'warning') return '预警';
				return '充足';
			},
			stockStateStyle(row) {
				let state = this.stockStateOf(row);
				if (state === 'soldout') return 'color:red;font-weight:bold;';
				if (state === 'warning') return 'color:#E6A23C;font-weight:bold;';
				return 'color:green;font-weight:bold;';
			},
			handleSelectionChange(val) {
				this.sels = val;
			},
			editFormClose() {
				this.batchMode = false;
				this.editForm = {
					id: null,
					name: '',
					bottleStock: '',
					cupStock: '',
					stockWarningThreshold: ''
				};
			},
			// 组装提交参数：留空字段不提交（避免误覆盖）
			buildPayload(id) {
				let payload = { id: id };
				let fields = ['bottleStock', 'cupStock', 'stockWarningThreshold'];
				for (let i = 0; i < fields.length; i++) {
					let key = fields[i];
					let val = this.editForm[key];
					if (val === '' || val === null || val === undefined) {
						continue;
					}
					if (!/^\d+$/.test(String(val).trim())) {
						this.$message({
							showClose: true,
							message: '「' + (key === 'bottleStock' ? '按瓶库存' : key === 'cupStock' ? '按杯库存' : '预警阈值') + '」只能填写非负整数',
							type: 'error'
						});
						return null;
					}
					payload[key] = Number(String(val).trim());
				}
				if (Object.keys(payload).length <= 1) {
					this.$message({
						showClose: true,
						message: '请至少填写一项要修改的库存字段',
						type: 'error'
					});
					return null;
				}
				return payload;
			},
			editSubmit: function() {
				let vue = this;
				let updateOne = function (id) {
					let payload = vue.buildPayload(id);
					if (!payload) return Promise.reject('invalid');
					return new Promise(function (resolve) {
						vue.$http.post(vue, '/rest/merchant/goods/update', payload,
							(vue, data) => resolve(),
							(error, data) => {
								vue.$message({
									showClose: true,
									message: data.message,
									type: 'error'
								});
								resolve();
							}
						);
					});
				};
				let rows = vue.batchMode ? vue.sels : [{ id: vue.editForm.id }];
				vue.editLoading = true;
				let seq = Promise.resolve();
				rows.forEach(function (row) {
					seq = seq.then(function () {
						return updateOne(row.id).catch(function () { });
					});
				});
				seq.then(function () {
					vue.editLoading = false;
					vue.editFormVisible = false;
					vue.$message({
						showClose: true,
						message: '库存已更新（人工维护，前端不做自动扣减）',
						type: 'success'
					});
					vue.getList();
				});
			},
			handleSizeChange(val) {
				this.searchMsg.pageSize = val;
				this.getList();
			},
			handleCurrentChange(val) {
				this.searchMsg.pageNo = val;
				this.getList();
			},
			getList(pageNoParam) { // 获取列表
				if(pageNoParam){
					this.searchMsg.pageNo = pageNoParam;
				}
				let vue = this;
				let param = Object.assign({}, vue.searchMsg);
				vue.listLoading = true;
				vue.$http.post(vue, '/rest/merchant/goods/list', param,
					(vue, data) => {
						vue.list = data.data.records;
						vue.total = data.data.total;
						vue.listLoading = false;
					},(error, data)=> {
						vue.listLoading = false;
						vue.$message({
							showClose: true,
							message: data.message,
							type: 'error'
						});
					}
				)
			},
			handleEdit: function (index, row) { // 显示单个编辑界面
				this.batchMode = false;
				this.editFormVisible = true;
				this.editForm = {
					id: row.id,
					name: row.name,
					bottleStock: row.bottleStock === null || row.bottleStock === undefined ? '' : String(row.bottleStock),
					cupStock: row.cupStock === null || row.cupStock === undefined ? '' : String(row.cupStock),
					stockWarningThreshold: row.stockWarningThreshold === null || row.stockWarningThreshold === undefined ? '' : String(row.stockWarningThreshold)
				};
			},
			openBatchDialog: function () { // 打开批量编辑界面（仅填要改的字段）
				this.batchMode = true;
				this.editFormVisible = true;
				this.editForm = {
					id: null,
					name: '',
					bottleStock: '',
					cupStock: '',
					stockWarningThreshold: ''
				};
			}
		},
		mounted() {
			this.getList();
			//开启订单自动打印定时器
			this.$orderPrint.init();
		}
	}

</script>

<style scoped>
.el-button+.el-button {
  margin-top: 5px;
  margin-left: 0;
}
.editForm .el-input {
  width: 300px;
}
</style>