<template>
	<section>
		<!--工具条：搜索 + 新增-->
		<el-col :span="24" class="toolbar" style="padding-bottom: 0px;">
			<el-form :inline="true" :model="searchMsg">
				<el-form-item label="分类名称" prop="name">
					<el-input v-model="searchMsg.name" clearable placeholder="请输入分类名称"></el-input>
				</el-form-item>
				<!-- A3(09-30)：恢复状态筛选（原整段被注释；配合 tb_points_mall_menu.is_disabled 列） -->
				<el-form-item label="分类状态" prop="isDisabled">
					<el-select v-model="searchMsg.isDisabled" clearable placeholder="全部">
						<el-option label="正常" :value="0"></el-option>
						<el-option label="禁用" :value="1"></el-option>
					</el-select>
				</el-form-item>
				<el-form-item>
					<el-button type="primary" @click="getList(1)">查询</el-button>
				</el-form-item>
				<el-form-item>
					<el-button type="primary" @click="gotoAdd('add')">新增</el-button>
				</el-form-item>
			</el-form>
		</el-col>
		<!--列表（A2 09-30：分类图标列已下线；新增 排序号(行内编辑)/状态/商品数 三列）-->
		<el-table :data="list" highlight-current-row v-loading="listLoading" style="width: 100%;" :cell-style="cellStyle" :header-cell-style="headerCellStyle">
			<el-table-column prop="name" label="分类名称"></el-table-column>
			<!-- A1：排序号行内编辑，失焦/步进即保存（复用 menu/update，只传 id+sortNumber） -->
			<el-table-column prop="sortNumber" label="排序号" width="130">
				<template slot-scope="scope">
					<el-input-number v-model="scope.row.sortNumber" :min="0" :max="9999" :step="1" size="mini" controls-position="right" @change="handleSortChange(scope.row)"></el-input-number>
				</template>
			</el-table-column>
			<!-- A3：状态列（0=正常 1=禁用）；禁用后顾客端商城分类页不再显示 -->
			<el-table-column prop="isDisabled" label="状态" width="90" :formatter="formatType"></el-table-column>
			<!-- A4：商品数（后端子查询出参，也是「删不掉」的原因提示） -->
			<el-table-column prop="goodsCount" label="商品数" width="80"></el-table-column>
			<el-table-column prop="description" label="描述"></el-table-column>
			<el-table-column prop="createTime" label="创建时间" width="150" :formatter="formatTime"></el-table-column>
			<el-table-column prop="updateTime" label="修改时间" width="150" :formatter="formatTime"></el-table-column>
			<el-table-column label="操作" fixed="right" width="170">
				<template slot-scope="scope">
					<el-button type="text" size="small" @click="handleToggleDisabled(scope.row)">{{ scope.row.isDisabled ? '启用' : '禁用' }}</el-button>
					<el-button size="small" @click="gotoAdd('edit', scope.row)">编辑</el-button>
					<!-- A7：删除降级为文字按钮，避免红色 danger 抢占整行视觉焦点 -->
					<el-button type="text" size="small" style="color:#F56C6C;" @click="handleDel(scope.row.id)">删除</el-button>
				</template>
			</el-table-column>
		</el-table>

		<!--工具条：分页（A7：总数不满一页时隐藏分页条）-->
		<el-col :span="24" class="toolbar" v-if="total > searchMsg.pageSize">
			<el-pagination
				@size-change="handleSizeChange"
				@current-change="handleCurrentChange"
				:current-page="searchMsg.pageNo"
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
				searchMsg: {
					pageNo: 1,
					pageSize: 20,
					name: '',
					isDisabled: ''   // ''=全部；提交前剔除，避免空串传给后端 Integer
				},
				list: [],
				total: 0,
				listLoading: false
			}
		},
		methods: {
			cellStyle(){ return "text-align:center" },
			headerCellStyle(){ return "text-align:center" },
			formatTime(row, column) { // 时间列：NULL/0 显示 "-"（D-2：不补历史数据）
				let date = new Date(row[column.property]);
				let str = this.$utils.formatDate(date, 'yyyy-MM-dd hh:mm:ss');
				if(row[column.property]==undefined || str=="1970-01-01 08:00:00"){
					str = "-";
				}
				return str;
			},
			formatType (row, column) { // 0=正常/启用  1=禁用
				return row.isDisabled ? '禁用' : '正常';
			},
			handleSizeChange(val) {
				this.searchMsg.pageSize = val;
				this.getList();
			},
			handleCurrentChange(val) {
				this.searchMsg.pageNo = val;
				this.getList();
			},
			getList(pageNoParam) { // 获取列表（后端已按 sort_number 升序、COALESCE 兜底 NULL）
				if(pageNoParam){
					this.searchMsg.pageNo = pageNoParam;
				}
				let vue = this
				// 空串条件不下发：后端 isDisabled 为 Integer，空串虽会被 Jackson 转 null，这里显式剔除更稳
				let param = Object.assign({}, vue.searchMsg);
				if (param.isDisabled === '' || param.isDisabled === null || param.isDisabled === undefined) {
					delete param.isDisabled
				}
				vue.listLoading = true;
				vue.$http.post(vue, '/rest/admin/pointsMall/menu/list', param,
					(vue, data) => {
						vue.list = data.data.records
						vue.total = data.data.total
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
			handleSortChange(row) { // A1：行内改排序号，只提交 id + sortNumber
				let vue = this
				let n = (row.sortNumber === null || row.sortNumber === undefined || row.sortNumber === '') ? 0 : Number(row.sortNumber)
				if (isNaN(n)) {
					vue.$message({ showClose: true, message: '排序号需为数字', type: 'error' })
					vue.getList()
					return
				}
				vue.$http.put(vue, '/rest/admin/pointsMall/menu/update', { id: row.id, sortNumber: n },
					(vue) => {
						vue.$message({ showClose: true, message: '排序已更新', type: 'success' })
						vue.getList()   // 重取列表 → 立即按新排序号重排
					}, (error, data) => {
						vue.$message({ showClose: true, message: data.message, type: 'error' })
						vue.getList()
					})
			},
			handleToggleDisabled(row) { // A3：禁用/启用切换（带确认，防误触）
				let vue = this
				let target = row.isDisabled ? 0 : 1
				let action = target === 1 ? '禁用' : '启用'
				vue.$confirm(`确认${action}分类「${row.name}」吗？`, '提示', { type: 'warning' }).then(() => {
					vue.$http.put(vue, '/rest/admin/pointsMall/menu/update', { id: row.id, isDisabled: target },
						(vue) => {
							vue.$message({ showClose: true, message: `${action}成功`, type: 'success' })
							vue.getList()
						}, (error, data) => {
							vue.$message({ showClose: true, message: data.message, type: 'error' })
						})
				}).catch(() => {})
			},
			handleDel (id) { // 删除（后端拦截：分类下有商品则拒绝并提示数量）
				this.$confirm('确认删除该记录吗?', '提示', {
					type: 'warning'
				}).then(() => {
					let vue = this;
					vue.$http.delete(vue, '/rest/admin/pointsMall/menu/delete', {"ids" : [id]},
						function(vue, data) {
							vue.$message({ showClose: true, message: data.message, type: 'success' });
							vue.getList()
						}, function(error, data) {
							vue.$message({ showClose: true, message: data.message, type: 'error' });
						}
					)
				}).catch(() => {});
			},
			gotoAdd(type, row) { // 新增/编辑均走独立页面（原页内弹层为不可达死代码，已于 A6 清理）
				if(type === 'add') {
					this.$router.push({path: 'pointsMall_addMenu'})
				} else {
					this.$router.push({path:'pointsMall_editMenu',query:{id: row.id}})
				}
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

</style>
