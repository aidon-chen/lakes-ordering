<template>
	<section>
		<!-- 状态 Tab：待自提 / 已完成 / 已取消 / 售后（无「待付款」，到店支付场景前端文案不做待支付主 TAB） -->
		<el-tabs v-model="activeTab" @tab-click="handleTabClick">
			<el-tab-pane label="待自提" name="4"></el-tab-pane>
			<el-tab-pane label="已完成" name="6"></el-tab-pane>
			<el-tab-pane label="已取消" name="10"></el-tab-pane>
			<el-tab-pane label="售后" name="-1"></el-tab-pane>
		</el-tabs>

		<!-- 工具条 -->
		<el-col :span="24" class="toolbar" style="padding-bottom: 0px;">
			<el-form :inline="true" :model="searchMsg">
				<el-form-item>
					<el-input v-model="searchMsg.orderNo" placeholder="请输入订单编号/自提码搜索" clearable></el-input>
				</el-form-item>
				<el-form-item>
					<el-date-picker
						v-model="searchMsg.createTime"
						type="daterange"
						align="right"
						unlink-panels
						range-separator="至"
						start-placeholder="开始日期"
						end-placeholder="结束日期">
					</el-date-picker>
				</el-form-item>
				<el-form-item>
					<el-button type="primary" @click="getList(1)">查询</el-button>
				</el-form-item>
			</el-form>
		</el-col>

		<!-- 列表 -->
		<el-table ref="table" :data="list" highlight-current-row v-loading="listLoading" style="width: 100%;">
			<!-- 商品明细（展开行） -->
			<el-table-column type="expand">
				<template slot-scope="props">
					<el-table :data="props.row.orderDetailList" style="width: 100%">
						<el-table-column label="商品主图" prop="mainImage">
							<template slot-scope="scope">
								<img :src="$http.publicUrl(scope.row.mainImage)" width="50" height="50">
							</template>
						</el-table-column>
						<el-table-column prop="goodsName" label="商品名称"></el-table-column>
						<el-table-column prop="specList" label="规格" :formatter="formatSpecList"></el-table-column>
						<el-table-column prop="price" label="单价" :formatter="addUnit"></el-table-column>
						<el-table-column prop="number" label="数量"></el-table-column>
						<el-table-column prop="subtotal" label="小计" :formatter="addUnit"></el-table-column>
					</el-table>
				</template>
			</el-table-column>

			<el-table-column prop="orderNo" label="订单编号" width="180"></el-table-column>
			<el-table-column prop="pickupCode" label="自提码" width="110"></el-table-column>
			<el-table-column prop="actualPrice" label="订单金额" :formatter="addUnit" width="100"></el-table-column>
			<el-table-column prop="goodsTotalQuantity" label="商品数量" width="90">
				<template scope="scope">
					<span>{{scope.row.goodsTotalQuantity}}件</span>
				</template>
			</el-table-column>
			<el-table-column prop="remark" label="买家备注"></el-table-column>
			<el-table-column prop="status" label="状态" width="100" :formatter="formatOrder"></el-table-column>
			<el-table-column prop="verifyTime" label="核销时间" :formatter="formatTime" width="160"></el-table-column>
			<el-table-column prop="createTime" label="下单时间" :formatter="formatTime" width="160"></el-table-column>
			<el-table-column label="操作" fixed="right" width="150">
				<template slot-scope="scope">
					<el-button type="primary" size="small" v-if="scope.row.status == 4" @click="handlePickup(scope.row)">核销</el-button>
					<el-button size="small" @click="handleDetail(scope.row)">详情</el-button>
				</template>
			</el-table-column>
		</el-table>

		<!-- 分页 -->
		<el-col :span="24" class="toolbar">
			<el-pagination
				@size-change="handleSizeChange"
				@current-change="handleCurrentChange"
				:current-page="searchMsg.pageNo"
				:page-sizes="[10, 20, 50]"
				:page-size="searchMsg.pageSize"
				layout="total, sizes, prev, pager, next, jumper"
				:total="total"
				style="float: right;">
			</el-pagination>
		</el-col>

		<!-- 订单详情弹层 -->
		<el-dialog title="商城订单详情" :visible.sync="detailVisible" width="760px" :close-on-click-modal="false">
			<!-- 注意：本工程 element-ui 为 2.12.0（无 el-descriptions，2.15+ 才有），用 el-form 栅格布局承载详情字段 -->
			<el-form label-width="90px" size="small" v-if="detail">
				<el-row>
					<el-col :span="12"><el-form-item label="订单编号">{{detail.orderNo}}</el-form-item></el-col>
					<el-col :span="12"><el-form-item label="自提码">{{detail.pickupCode || '-'}}</el-form-item></el-col>
				</el-row>
				<el-row>
					<el-col :span="12"><el-form-item label="状态">{{formatOrder(detail)}}</el-form-item></el-col>
					<el-col :span="12"><el-form-item label="订单金额">{{detail.actualPrice}}元</el-form-item></el-col>
				</el-row>
				<el-row>
					<el-col :span="12"><el-form-item label="下单时间">{{formatTime(detail, null, detail.createTime)}}</el-form-item></el-col>
					<el-col :span="12"><el-form-item label="取货截止">{{formatTime(detail, null, detail.pickupDeadline) || '-'}}</el-form-item></el-col>
				</el-row>
				<el-row>
					<el-col :span="12"><el-form-item label="核销时间">{{formatTime(detail, null, detail.verifyTime) || '-'}}</el-form-item></el-col>
					<el-col :span="12"><el-form-item label="核销人id">{{detail.verifierId || '（顾客自助核销）'}}</el-form-item></el-col>
				</el-row>
				<el-row>
					<el-col :span="12"><el-form-item label="联系人">{{detail.contactRealname || '-'}}</el-form-item></el-col>
					<el-col :span="12"><el-form-item label="联系电话">{{detail.contactPhone || '-'}}</el-form-item></el-col>
				</el-row>
				<el-row>
					<el-col :span="24"><el-form-item label="买家备注">{{detail.remark || '-'}}</el-form-item></el-col>
				</el-row>
			</el-form>
			<el-table :data="detail ? detail.orderDetailList : []" style="width: 100%; margin-top: 12px;">
				<el-table-column prop="goodsName" label="商品名称"></el-table-column>
				<el-table-column prop="specList" label="规格" :formatter="formatSpecList"></el-table-column>
				<el-table-column prop="price" label="单价" :formatter="addUnit"></el-table-column>
				<el-table-column prop="number" label="数量"></el-table-column>
				<el-table-column prop="subtotal" label="小计" :formatter="addUnit"></el-table-column>
			</el-table>
			<div slot="footer" class="dialog-footer">
				<el-button @click="detailVisible = false">关闭</el-button>
				<el-button type="primary" v-if="detail && detail.status == 4" @click="handlePickup(detail, true)">核销</el-button>
			</div>
		</el-dialog>
	</section>
</template>

<script>
	/**
	 * 商城订单（A-06-4）——外带自提酒水商城运营视图
	 * 语义口径（grill session 2026-09-19）：到店支付、无支付方式/实付款/物流/积分；
	 * 状态机与客户端 C-10 对齐：4=待自提、6=已完成、7/9=售后、10=已取消；
	 * 核销接口走 merchant 侧 /rest/merchant/pointsMall/order/confirmPickup（管理端经单店桥接会话调用，收银端后续复用）。
	 */
	export default {
		data() {
			return {
				activeTab: '4',
				searchMsg: {
					pageNo: 1,
					pageSize: 10,
					orderNo: '',
					createTime: null
				},
				list: [],
				total: 0,
				listLoading: false,
				detail: null,
				detailVisible: false
			};
		},
		methods: {
			handleTabClick() {
				this.getList(1);
			},
			getList(pageNo) {
				let vue = this;
				if (pageNo) {
					vue.searchMsg.pageNo = pageNo;
				}
				let param = Object.assign({}, vue.searchMsg);
				//处理开始日期、结束日期
				if (vue.searchMsg.createTime) {
					param.startCreateTime = vue.$utils.formatDate(new Date(vue.searchMsg.createTime[0]), 'yyyy/MM/dd hh:mm:ss');
					param.endCreateTime = vue.$utils.formatDate(new Date(vue.searchMsg.createTime[1]), 'yyyy/MM/dd hh:mm:ss');
				}
				delete param.createTime;
				//Tab 即状态筛选（-1 = 售后集合：7 售后处理中 / 9 售后完成 / 11）
				param.status = Number(vue.activeTab);

				vue.listLoading = true;
				vue.$http.post(vue, '/rest/admin/pointsMall/order/listWithDetail', param,
					(vue, data) => {
						vue.list = data.data.records || [];
						vue.total = data.data.total || 0;
						vue.listLoading = false;
					}, (error, data) => {
						vue.listLoading = false;
						vue.$message({ showClose: true, message: data.message, type: 'error' });
					}
				);
			},
			// 核销：待自提 → 已完成（幂等）；核销后刷新列表并关闭详情弹层
			handlePickup(row, fromDetail) {
				let vue = this;
				this.$confirm('确认核销该订单？核销后订单状态将变更为「已完成」，该操作不可撤销。', '核销确认', {
					type: 'warning'
				}).then(() => {
					vue.$http.post(vue, '/rest/merchant/pointsMall/order/confirmPickup', { id: row.id },
						(vue, data) => {
							vue.$message({ showClose: true, message: '核销成功', type: 'success' });
							if (fromDetail) {
								vue.detailVisible = false;
							}
							vue.getList();
						}, (error, data) => {
							vue.$message({ showClose: true, message: data.message, type: 'error' });
						}
					);
				}).catch(() => {});
			},
			handleDetail(row) {
				this.detail = row;
				this.detailVisible = true;
			},
			handleSizeChange(val) {
				this.searchMsg.pageSize = val;
				this.getList(1);
			},
			handleCurrentChange(val) {
				this.searchMsg.pageNo = val;
				this.getList();
			},
			addUnit(row, column, cellValue) {
				return cellValue + '元';
			},
			//口径：1=待支付 4=待自提 5=已发货 6=已完成 7=售后处理中 8=已退款 9=售后完成 10=已取消 11=售后相关
			formatOrder(row) {
				let status = row.status;
				if (status == 1) return '待支付';
				if (status == 4) return '待自提';
				if (status == 5) return '已发货';
				if (status == 6) return '已完成';
				if (status == 7) return '售后处理中';
				if (status == 8) return '已退款';
				if (status == 9) return '售后完成';
				if (status == 10) return '已取消';
				if (status == 11) return '售后相关';
				return '-';
			},
			formatTime(row, column, cellValue) {
				if (!cellValue) return '';
				return this.$utils.formatDate(new Date(cellValue), 'yyyy-MM-dd hh:mm:ss');
			},
			formatSpecList(row, column, cellValue) {
				if (!cellValue) return '';
				let str = '';
				for (let key in cellValue) {
					str = str === '' ? cellValue[key] : str + '/' + cellValue[key];
				}
				return str;
			}
		},
		mounted() {
			this.getList(1);
		}
	};
</script>