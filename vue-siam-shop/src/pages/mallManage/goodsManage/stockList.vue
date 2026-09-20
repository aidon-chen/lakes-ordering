<template>
	<section>
		<!-- 工具条：商品名称 + 库存状态 + 预警阈值（前端口径，阈值仅用于标色，不改库存数据） -->
		<el-col :span="24" class="toolbar" style="padding-bottom: 0px;">
			<el-form :inline="true">
				<el-form-item>
					<el-input v-model="keyword" placeholder="请输入商品名称搜索" clearable @keyup.enter.native="filteredList"></el-input>
				</el-form-item>
				<el-form-item>
					<el-select v-model="stockState" placeholder="库存状态" clearable>
						<el-option label="充足" value="enough"></el-option>
						<el-option label="预警" value="warning"></el-option>
						<el-option label="售罄" value="soldout"></el-option>
					</el-select>
				</el-form-item>
				<el-form-item label="预警阈值">
					<el-input-number v-model="warningThreshold" :min="0" :max="9999" size="small" controls-position="right"></el-input-number>
					<span class="note-margin">件（库存 ≤ 阈值标为「预警」，=0 标为「售罄」）</span>
				</el-form-item>
				<el-form-item>
					<el-button type="primary" @click="filteredList">查询</el-button>
				</el-form-item>
				<el-form-item>
					<el-button type="primary" :disabled="sels.length === 0" @click="openBatchDialog">批量改库存</el-button>
				</el-form-item>
			</el-form>
		</el-col>

		<!-- 列表 -->
		<el-table :data="pageList" @selection-change="handleSelectionChange" highlight-current-row v-loading="listLoading" style="width: 100%;">
			<el-table-column type="selection" width="55"></el-table-column>
			<el-table-column prop="id" label="商品编号" width="90"></el-table-column>
			<el-table-column label="商品主图" prop="mainImage" width="90">
				<template slot-scope="scope">
					<img :src="$http.publicUrl(scope.row.mainImage)" width="50" height="50">
				</template>
			</el-table-column>
			<el-table-column prop="name" label="商品名称"></el-table-column>
			<el-table-column prop="menuName" label="商品类别"></el-table-column>
			<el-table-column prop="price" label="一口价" :formatter="addUnit" width="100"></el-table-column>
			<el-table-column prop="stock" label="可售库存" width="100"></el-table-column>
			<el-table-column label="库存状态" width="100">
				<template slot-scope="scope">
					<span :style="stockStateStyle(scope.row.stock)">{{ stockStateText(scope.row.stock) }}</span>
				</template>
			</el-table-column>
			<el-table-column label="操作" fixed="right" width="120">
				<template slot-scope="scope">
					<el-button size="small" @click="openEditDialog(scope.row)">改库存</el-button>
				</template>
			</el-table-column>
		</el-table>

		<!-- 分页（本地分页：商城商品量小，一次取回后本地筛选/分页） -->
		<el-col :span="24" class="toolbar">
			<el-pagination
				@size-change="handleSizeChange"
				@current-change="handleCurrentChange"
				:current-page="pageNo"
				:page-sizes="[10, 20, 50]"
				:page-size="pageSize"
				layout="total, sizes, prev, pager, next, jumper"
				:total="filtered.length"
				style="float: right;">
			</el-pagination>
		</el-col>

		<!-- 单个/批量改库存弹层 -->
		<el-dialog :title="batchMode ? ('批量改库存（已选 ' + sels.length + ' 件商品）') : '修改库存'" :visible.sync="dialogVisible" width="420px" :close-on-click-modal="false">
			<el-form label-width="90px">
				<el-form-item label="商品">
					<span v-if="batchMode">{{ sels.length }} 件商品</span>
					<span v-else>{{ editRow ? editRow.name : '' }}</span>
				</el-form-item>
				<el-form-item label="可售库存">
					<el-input-number v-model="editStock" :min="0" :max="99999" controls-position="right"></el-input-number>
				</el-form-item>
			</el-form>
			<div slot="footer" class="dialog-footer">
				<el-button @click="dialogVisible = false">取消</el-button>
				<el-button type="primary" :loading="saving" @click="saveStock">保存</el-button>
			</div>
		</el-dialog>
	</section>
</template>

<script>
	/**
	 * 商城库存管理（A-06-6）
	 * 口径（grill session 2026-09-19）：商城沿用 PointsMallGoods.stock 单列，纯人工维护、无自动扣减；
	 * 「预警」为前端展示口径（库存 ≤ 阈值标色），阈值仅本页前端使用，不改库结构。
	 * 商品量小（单店）：一次取回后在本地做筛选与分页，避免服务端聚合改造。
	 */
	export default {
		data() {
			return {
				keyword: '',
				stockState: '',
				warningThreshold: 10,
				list: [],
				filtered: [],
				pageList: [],
				pageNo: 1,
				pageSize: 10,
				listLoading: false,
				sels: [],
				dialogVisible: false,
				batchMode: false,
				editRow: null,
				editStock: 0,
				saving: false
			};
		},
		methods: {
			getList() {
				let vue = this;
				vue.listLoading = true;
				//商城商品量小：一次取回（500 上限），本地筛选/分页
				vue.$http.post(vue, '/rest/admin/pointsMall/goods/list', { pageNo: 1, pageSize: 500 },
					(vue, data) => {
						vue.list = (data.data && data.data.records) ? data.data.records : [];
						vue.listLoading = false;
						vue.filteredList();
					}, (error, data) => {
						vue.listLoading = false;
						vue.$message({ showClose: true, message: data.message, type: 'error' });
					}
				);
			},
			filteredList() {
				let vue = this;
				vue.filtered = vue.list.filter(item => {
					if (vue.keyword && (item.name || '').indexOf(vue.keyword) === -1) return false;
					if (vue.stockState && vue.stockStateOf(item.stock) !== vue.stockState) return false;
					return true;
				});
				vue.pageNo = 1;
				vue.slicePage();
			},
			slicePage() {
				let start = (this.pageNo - 1) * this.pageSize;
				this.pageList = this.filtered.slice(start, start + this.pageSize);
			},
			handleSizeChange(val) {
				this.pageSize = val;
				this.slicePage();
			},
			handleCurrentChange(val) {
				this.pageNo = val;
				this.slicePage();
			},
			handleSelectionChange(val) {
				this.sels = val;
			},
			stockStateOf(stock) {
				let s = stock === null || stock === undefined ? 0 : Number(stock);
				if (s <= 0) return 'soldout';
				if (s <= this.warningThreshold) return 'warning';
				return 'enough';
			},
			stockStateText(stock) {
				let state = this.stockStateOf(stock);
				if (state === 'soldout') return '售罄';
				if (state === 'warning') return '预警';
				return '充足';
			},
			stockStateStyle(stock) {
				let state = this.stockStateOf(stock);
				if (state === 'soldout') return 'color:red;font-weight:bold;';
				if (state === 'warning') return 'color:#E6A23C;font-weight:bold;';
				return 'color:green;font-weight:bold;';
			},
			openEditDialog(row) {
				this.batchMode = false;
				this.editRow = row;
				this.editStock = row.stock === null || row.stock === undefined ? 0 : row.stock;
				this.dialogVisible = true;
			},
			openBatchDialog() {
				this.batchMode = true;
				this.editRow = null;
				this.editStock = 0;
				this.dialogVisible = true;
			},
			saveStock() {
				let vue = this;
				let rows = vue.batchMode ? vue.sels : [vue.editRow];
				if (!rows || rows.length === 0) return;
				vue.saving = true;
				//逐个提交（复用现有：/rest/admin/pointsMall/goods/update 承载 stock 字段）
				let seq = Promise.resolve();
				rows.forEach(row => {
					seq = seq.then(() => new Promise(resolve => {
						vue.$http.post(vue, '/rest/admin/pointsMall/goods/update', { id: row.id, stock: vue.editStock },
							() => resolve(), (error, data) => {
								vue.$message({ showClose: true, message: (data && data.message) || '保存失败', type: 'error' });
								resolve();
							}
						);
					}));
				});
				seq.then(() => {
					vue.saving = false;
					vue.dialogVisible = false;
					vue.$message({ showClose: true, message: '库存已更新', type: 'success' });
					vue.getList();
				});
			},
			addUnit(row, column, cellValue) {
				return cellValue + '元';
			}
		},
		mounted() {
			this.getList();
		}
	};
</script>