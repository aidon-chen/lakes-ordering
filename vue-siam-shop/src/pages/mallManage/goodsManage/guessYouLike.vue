<template>
	<section>
		<!--工具条-->
		<el-col :span="24" class="toolbar" style="padding-bottom: 0px;">
			<el-form :inline="true">
				<el-form-item>
					<el-button type="primary" icon="el-icon-plus" @click="openAddDialog">添加商品</el-button>
				</el-form-item>
				<el-form-item>
					<el-button type="success" @click="saveSort">保存排序</el-button>
				</el-form-item>
				<el-form-item>
					<span style="color:#999;font-size:12px;">顾客端「猜你喜欢」按排序号升序展示（未配置时回退近30天热销）；移除仅解除关联，不影响商品本体</span>
				</el-form-item>
			</el-form>
		</el-col>
		<!--列表-->
		<el-table :data="list" highlight-current-row v-loading="listLoading" style="width: 100%;">
			<el-table-column type="index" label="序号" width="50"></el-table-column>
			<el-table-column label="商品主图" width="90">
				<template scope="scope">
					<img v-if="scope.row.mainImage" :src="$http.publicUrl(scope.row.mainImage)" width="50" height="50">
					<span v-else style="color:#999;">无图</span>
				</template>
			</el-table-column>
			<el-table-column prop="name" label="商品名称"></el-table-column>
			<el-table-column prop="price" label="一口价" width="90">
				<template scope="scope">
					<span>￥{{ scope.row.price || 0 }}</span>
				</template>
			</el-table-column>
			<el-table-column label="状态" width="90">
				<template scope="scope">
					<span v-if="scope.row.status == 2" style="color:green;font-weight:bold;">已上架</span>
					<span v-else-if="scope.row.status == 4" style="color:red;font-weight:bold;">售罄</span>
					<span v-else style="color:grey;font-weight:bold;">未上架</span>
				</template>
			</el-table-column>
			<el-table-column label="真实销量" width="90">
				<template scope="scope">
					<span>{{ scope.row.totalSales || 0 }}份</span>
				</template>
			</el-table-column>
			<el-table-column label="展示销量" width="90">
				<template scope="scope">
					<span>{{ (scope.row.totalSales || 0) + (scope.row.salesOffset || 0) }}份</span>
				</template>
			</el-table-column>
			<el-table-column label="排序号" width="130">
				<template scope="scope">
					<el-input-number v-model="scope.row.sortNumber" :min="1" size="mini" style="width:110px;"></el-input-number>
				</template>
			</el-table-column>
			<el-table-column label="操作" width="220">
				<template scope="scope">
					<el-button size="mini" :disabled="scope.$index === 0" @click="moveUp(scope.$index)">上移</el-button>
					<el-button size="mini" :disabled="scope.$index === list.length - 1" @click="moveDown(scope.$index)">下移</el-button>
					<el-button type="danger" size="mini" @click="handleRemove(scope.row)">移除</el-button>
				</template>
			</el-table-column>
		</el-table>

		<!--添加商品弹窗：搜索勾选已上架商品，追加到列表末尾-->
		<el-dialog title="添加商品到猜你喜欢" :visible.sync="addVisible" :close-on-click-modal="false" width="640px">
			<el-form :inline="true" size="small">
				<el-form-item>
					<el-input v-model="addSearch.name" placeholder="商品名称" clearable></el-input>
				</el-form-item>
				<el-form-item>
					<el-button type="primary" @click="searchAddGoods(1)">搜索</el-button>
				</el-form-item>
			</el-form>
			<el-table :data="addGoodsList" v-loading="addLoading" size="small" @selection-change="handleAddSelection" max-height="360">
				<el-table-column type="selection" width="45"></el-table-column>
				<el-table-column prop="id" label="编号" width="70"></el-table-column>
				<el-table-column prop="name" label="商品名称"></el-table-column>
				<el-table-column prop="price" label="一口价" width="90"></el-table-column>
				<el-table-column label="是否已添加" width="100">
					<template scope="scope">
						<span v-if="addedGoodsIds.indexOf(scope.row.id) >= 0" style="color:#999;">已添加</span>
						<span v-else style="color:#67c23a;">未添加</span>
					</template>
				</el-table-column>
			</el-table>
			<el-pagination
				@current-change="searchAddGoods"
				:current-page="addSearch.pageNo"
				:page-size="addSearch.pageSize"
				layout="prev, pager, next, total"
				:total="addTotal"
				style="float:right;margin-top:10px;">
			</el-pagination>
			<div slot="footer" class="dialog-footer">
				<el-button size="small" @click="addVisible = false">取消</el-button>
				<el-button type="primary" size="small" :disabled="addSelection.length === 0" @click="confirmAdd">添加所选（{{ addSelection.length }}）</el-button>
			</div>
		</el-dialog>
	</section>
</template>

<script>
	export default {
		data() {
			return {
				list: [],
				listLoading: false,
				//添加商品弹窗
				addVisible: false,
				addLoading: false,
				addSearch: { pageNo: 1, pageSize: 10, name: '', status: 2 },
				addGoodsList: [],
				addTotal: 0,
				addSelection: []
			}
		},
		computed: {
			//已添加商品的 goodsId 集合（用于弹窗内标识）
			addedGoodsIds() {
				return this.list.map(item => item.goodsId);
			}
		},
		methods: {
			getList() {
				let vue = this;
				vue.listLoading = true;
				vue.$http.post(vue, '/rest/admin/pointsMall/guessYouLike/list', {},
					(vue, data) => {
						vue.list = data.data || [];
						vue.listLoading = false;
					}, (error, data) => {
						vue.listLoading = false;
						vue.$message({ showClose: true, message: data.message, type: 'error' });
					}
				);
			},
			openAddDialog() {
				this.addVisible = true;
				this.addSelection = [];
				this.searchAddGoods(1);
			},
			searchAddGoods(pageNo) {
				let vue = this;
				if (pageNo) vue.addSearch.pageNo = pageNo;
				vue.addLoading = true;
				vue.$http.post(vue, '/rest/admin/pointsMall/goods/list', vue.addSearch,
					(vue, data) => {
						vue.addGoodsList = data.data.records || [];
						vue.addTotal = data.data.total || 0;
						vue.addLoading = false;
					}, (error, data) => {
						vue.addLoading = false;
						vue.$message({ showClose: true, message: data.message, type: 'error' });
					}
				);
			},
			handleAddSelection(val) {
				this.addSelection = val;
			},
			confirmAdd() {
				let vue = this;
				let goodsIds = vue.addSelection.map(item => item.id);
				vue.$http.post(vue, '/rest/admin/pointsMall/guessYouLike/add', { goodsIds: goodsIds },
					function(vue, data) {
						vue.$message({ showClose: true, message: '添加成功（新增 ' + data.data + ' 条，已存在的自动跳过）', type: 'success' });
						vue.addVisible = false;
						vue.getList();
					}, function(error, data) {
						vue.$message({ showClose: true, message: data.message, type: 'error' });
					}
				);
			},
			handleRemove(row) {
				let vue = this;
				vue.$confirm('确认将 ‘ ' + (row.name || '该商品') + ' ’ 移出猜你喜欢？（仅解除关联，不影响商品）', '提示', {
					type: 'warning'
				}).then(() => {
					vue.$http.post(vue, '/rest/admin/pointsMall/guessYouLike/remove', { ids: [row.id] },
						function(vue, data) {
							vue.$message({ showClose: true, message: '移除成功', type: 'success' });
							vue.getList();
						}, function(error, data) {
							vue.$message({ showClose: true, message: data.message, type: 'error' });
						}
					);
				}).catch(() => {});
			},
			moveUp(index) {
				let list = this.list;
				let temp = list[index];
				this.$set(list, index, list[index - 1]);
				this.$set(list, index - 1, temp);
				this.renumber();
			},
			moveDown(index) {
				let list = this.list;
				let temp = list[index];
				this.$set(list, index, list[index + 1]);
				this.$set(list, index + 1, temp);
				this.renumber();
			},
			//上移/下移后按当前展示顺序重排排序号（1,2,3...）
			renumber() {
				this.list.forEach((item, index) => {
					item.sortNumber = index + 1;
				});
			},
			saveSort() {
				let vue = this;
				if (!vue.list.length) {
					vue.$message({ showClose: true, message: '暂无可排序的商品', type: 'error' });
					return;
				}
				let items = vue.list.map((item, index) => ({ id: item.id, sortNumber: item.sortNumber || index + 1 }));
				vue.$http.post(vue, '/rest/admin/pointsMall/guessYouLike/updateSort', { items: items },
					function(vue, data) {
						vue.$message({ showClose: true, message: '排序保存成功', type: 'success' });
						vue.getList();
					}, function(error, data) {
						vue.$message({ showClose: true, message: data.message, type: 'error' });
					}
				);
			}
		},
		mounted() {
			this.getList();
		}
	}
</script>

<style scoped>
	.toolbar {
		background: #f2f2f2;
		padding: 10px;
	}
</style>
