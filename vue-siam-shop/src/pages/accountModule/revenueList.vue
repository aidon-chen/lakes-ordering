<template>
	<section>
		<!-- 维度切换 + 日期范围 -->
		<el-row>
			<el-col :span="24" class="toolbar" style="padding-bottom: 0px;">
				<el-form :inline="true" size="small">
					<el-form-item label="统计维度">
						<el-radio-group v-model="timeType" @change="handleTimeTypeChange">
							<el-radio-button label="DAY">日</el-radio-button>
							<el-radio-button label="WEEK">周</el-radio-button>
							<el-radio-button label="MONTH">月</el-radio-button>
						</el-radio-group>
					</el-form-item>
					<el-form-item label="">
						<el-date-picker
							v-model="dateRange"
							type="daterange"
							align="right"
							unlink-panels
							clearable
							range-separator="至"
							start-placeholder="开始日期"
							end-placeholder="结束日期"
							:picker-options="pickerOptions"
							@change="getData">
						</el-date-picker>
					</el-form-item>
					<el-form-item>
						<el-button type="primary" @click="getData">查询</el-button>
					</el-form-item>
					<el-form-item>
						<span style="color:#909399;font-size:12px;">统计区间：{{rangeText}}</span>
					</el-form-item>
				</el-form>
			</el-col>
		</el-row>

		<!-- 核心指标卡：营业额 / 订单数 / 客单价 / 退款额 -->
		<el-row :gutter="20" style="margin-top: 10px;">
			<el-col :span="6">
				<div class="metric-frame blue">
					<p class="metric-title">营业额(元)</p>
					<p class="metric-value">￥{{revenue.orderAmount || 0}}</p>
				</div>
			</el-col>
			<el-col :span="6">
				<div class="metric-frame green">
					<p class="metric-title">订单数</p>
					<p class="metric-value">{{revenue.orderCount || 0}}</p>
				</div>
			</el-col>
			<el-col :span="6">
				<div class="metric-frame yellow">
					<p class="metric-title">客单价(元)</p>
					<p class="metric-value">￥{{revenue.perCustomerTransaction || 0}}</p>
				</div>
			</el-col>
			<el-col :span="6">
				<div class="metric-frame orange">
					<p class="metric-title">退款额(元)</p>
					<p class="metric-value">￥{{revenue.refundAmount || 0}}</p>
				</div>
			</el-col>
		</el-row>

		<!-- 趋势折线：营业额/订单量双轴 -->
		<div class="chart-layout">
			<div class="layout-title">营收趋势</div>
			<div style="padding: 10px 20px 20px 20px;">
				<ve-line
					:data="trendChartData"
					:settings="trendChartSettings"
					:legend-visible="true"
					:loading="listLoading"
					height="320px">
				</ve-line>
			</div>
		</div>

		<!-- 支付方式分布 + 订单类型分布 -->
		<el-row :gutter="20" style="margin-top: 20px;">
			<el-col :span="12">
				<div class="chart-layout" style="margin-top: 0;">
					<div class="layout-title">支付方式分布</div>
					<div style="padding: 10px;">
						<ve-pie
							:data="paymentChartData"
							:settings="pieChartSettings"
							:loading="listLoading"
							height="300px">
						</ve-pie>
					</div>
				</div>
			</el-col>
			<el-col :span="12">
				<div class="chart-layout" style="margin-top: 0;">
					<div class="layout-title">订单类型分布</div>
					<div style="padding: 10px;">
						<ve-ring
							:data="sceneChartData"
							:settings="ringChartSettings"
							:loading="listLoading"
							height="300px">
						</ve-ring>
					</div>
				</div>
			</el-col>
		</el-row>

		<!-- 商品销售 TOP10 -->
		<div class="chart-layout">
			<div class="layout-title">商品销售 TOP10</div>
			<el-table :data="topGoodsList" v-loading="listLoading" style="width: 100%;" :cell-style="cellStyle" :header-cell-style="headerCellStyle">
				<el-table-column type="index" label="排名" width="80"></el-table-column>
				<el-table-column prop="goodsName" label="商品名称"></el-table-column>
				<el-table-column prop="goodsQuantity" label="销售数量"></el-table-column>
				<el-table-column prop="goodsAmount" label="销售金额(元)">
					<template scope="scope">
						<span>{{changeDecimal(scope.row.goodsAmount, 2)}}</span>
					</template>
				</el-table-column>
			</el-table>
		</div>
	</section>
</template>

<script>
	export default {
		data() {
			return {
				timeType: 'DAY', //统计维度：日/周/月
				dateRange: '', //日期范围，为空时后端按维度给缺省区间
				rangeText: '',
				listLoading: false,
				revenue: {}, //核心指标
				topGoodsList: [], //商品TOP10
				trendChartData: {
					columns: ['statDate', 'orderCount', 'orderAmount'],
					rows: []
				},
				//趋势双轴：左轴订单量、右轴营业额
				trendChartSettings: {
					axisSite: {right: ['orderAmount']},
					labelMap: {'orderCount': '订单量', 'orderAmount': '营业额(元)'},
					area: true
				},
				paymentChartData: {
					columns: ['name', 'value'],
					rows: []
				},
				pieChartSettings: {
					dimension: 'name',
					metrics: ['value']
				},
				sceneChartData: {
					columns: ['name', 'value'],
					rows: []
				},
				ringChartSettings: {
					dimension: 'name',
					metrics: ['value']
				},
				pickerOptions: {
					shortcuts: [{
						text: '最近一周',
						onClick(picker) {
							const end = new Date();
							let start = new Date();
							start.setTime(end.getTime() - 1000 * 60 * 60 * 24 * 7);
							picker.$emit('pick', [start, end]);
						}
					}, {
						text: '最近一月',
						onClick(picker) {
							const end = new Date();
							let start = new Date();
							start.setTime(end.getTime() - 1000 * 60 * 60 * 24 * 30);
							picker.$emit('pick', [start, end]);
						}
					}, {
						text: '最近三月',
						onClick(picker) {
							const end = new Date();
							let start = new Date();
							start.setTime(end.getTime() - 1000 * 60 * 60 * 24 * 90);
							picker.$emit('pick', [start, end]);
						}
					}, {
						text: '最近一年',
						onClick(picker) {
							const end = new Date();
							let start = new Date();
							start.setTime(end.getTime() - 1000 * 60 * 60 * 24 * 365);
							picker.$emit('pick', [start, end]);
						}
					}]
				}
			}
		},
		methods: {
			//切换维度：清空日期范围，由后端按维度给缺省区间并重新取数
			handleTimeTypeChange() {
				this.dateRange = '';
				this.getData();
			},
			changeDecimal(val, scale) {
				return Number(val || 0).toFixed(scale);
			},
			formatTime(date) {
				if (!date) {
					return '';
				}
				return this.$utils.formatDate(new Date(date), 'yyyy-MM-dd')
			},
			cellStyle() {
				return "text-align:center";
			},
			headerCellStyle() {
				return "text-align:center";
			},
			getData() {
				let vue = this;
				let param = {
					timeType: vue.timeType
				};
				//日期范围：为空时不传，后端按维度取缺省区间
				if (vue.dateRange && vue.dateRange.length == 2) {
					param.startCreateTime = vue.$utils.formatDate(new Date(vue.dateRange[0]), 'yyyy/MM/dd hh:mm:ss');
					param.endCreateTime = vue.$utils.formatDate(new Date(vue.dateRange[1]), 'yyyy/MM/dd hh:mm:ss');
				}

				vue.listLoading = true;
				vue.$http.post(vue, '/rest/admin/statistics/revenue', param,
					(vue, data) => {
						vue.listLoading = false;
						let result = data.data;

						//核心指标
						vue.revenue = {
							orderAmount: Number(result.orderAmount || 0).toFixed(2),
							orderCount: result.orderCount || 0,
							perCustomerTransaction: Number(result.perCustomerTransaction || 0).toFixed(2),
							refundAmount: Number(result.refundAmount || 0).toFixed(2)
						};
						vue.rangeText = vue.formatTime(result.startTime) + ' 至 ' + vue.formatTime(result.endTime);

						//趋势折线
						vue.trendChartData = {
							columns: ['statDate', 'orderCount', 'orderAmount'],
							rows: (result.trendList || []).map(item => {
								return {
									statDate: item.statDate,
									orderCount: item.orderCount,
									orderAmount: Number(item.orderAmount || 0)
								}
							})
						};

						//支付方式分布（饼图：按金额占比）
						vue.paymentChartData = {
							columns: ['name', 'value'],
							rows: (result.paymentModeList || []).map(item => {
								return {name: item.name, value: Number(item.orderAmount || 0)}
							})
						};

						//订单类型分布（环形图）
						vue.sceneChartData = {
							columns: ['name', 'value'],
							rows: (result.orderSceneList || []).map(item => {
								return {name: item.name, value: Number(item.orderAmount || 0)}
							})
						};

						//商品销售TOP10
						vue.topGoodsList = result.topGoodsList || [];
					}, (error, data) => {
						vue.listLoading = false;
						vue.$message({
							showClose: true,
							message: data.message,
							type: 'error'
						});
					}
				)
			}
		},
		mounted() {
			this.getData();
		}
	}
</script>

<style scoped>
	.metric-frame {
		color: #fff;
		padding: 20px 24px;
		border-radius: 4px;
	}
	.metric-title {
		font-size: 14px;
		font-weight: 700;
	}
	.metric-value {
		font-size: 30px;
		line-height: 36px;
		margin-top: 12px;
	}
	.blue {
		background-color: #05ACEC;
	}
	.green {
		background-color: #3FD7E4;
	}
	.yellow {
		background-color: #F8B548;
	}
	.orange {
		background-color: #F78A71;
	}
	.chart-layout {
		margin-top: 20px;
		border: 1px solid #DCDFE6;
	}
	.layout-title {
		color: #606266;
		padding: 15px 20px;
		background: #F2F6FC;
		font-weight: bold;
	}
</style>