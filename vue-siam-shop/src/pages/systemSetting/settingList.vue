<template>
	<section>
		<!-- 基础参数配置：现仅保留「平台抽佣」一项。
		     2026-10-01 清理下线：
		     〔1〕「会员与积分」整块（新用户注册赠送积分/奖励金、购买1杯赠送积分、奖励金提现门槛、
		          提现手续费比例、提现人工审核阈值）——奖励金/提现已整链删除，后端无消费方；
		          其中「注册送积分」后端 MemberServiceImpl 仍读 tb_setting，但运营无需配置故一并移除入口。
		     〔2〕「经营与收款」的「营业时间说明」businessHoursDesc——全端 0 消费。
		     〔3〕「协议与图片」整块（用户服务协议/隐私政策/平台logo/首页海报位）——协议在顾客端是
		          硬编码页、logo 与海报全端 0 消费。
		     2026-10-02 客服联系方式迁出：
		     〔4〕客服电话/客服微信/客服微信二维码——顾客端真实消费（订单详情拨号、退款页弹层），
		          按用户拍板迁到「门店管理→本店信息」页（数据源仍为 tb_setting，未改后端）。
		     ⚠ 后端 tb_setting 对应列与数据不动，留待 DDL 清理批次统一处理。 -->

		<div class="section-layout">
			<div class="layout-title">平台抽佣</div>
			<div class="section-content">
				<el-form :model="editForm" label-width="220px" style="width: 70%;" ref="editForm2">
					<el-form-item label="平台抽佣比例" prop="orderSystemExtractionRatio">
						<el-input v-model="editForm.orderSystemExtractionRatio" class="input-width">
							<template slot="append">%</template>
						</el-input>
						<span class="note-margin">用户下单系统抽佣</span>
					</el-form-item>
				</el-form>
			</div>
		</div>

		<div class="submit-bar">
			<el-button type="primary" @click="editSubmit" :loading="editLoading">提交</el-button>
		</div>
	</section>
</template>

<script>
	export default {
		data() {
			return {
				editLoading: false,
				//编辑界面数据（客服联系方式已于 2026-10-02 迁至「门店管理→本店信息」，此处只剩抽佣比例）
				editForm: {
					id: null,
					orderSystemExtractionRatio: ''
				}
			}
		},
		methods: {
			//获取当前参数配置
			getDetail() {
				let vue = this;
				vue.$http.post(vue, '/rest/admin/setting/selectCurrent', {},
					(vue, data) => {
						let setting = data.data || {};
						//整对象赋值以触发视图更新
						vue.editForm = setting;
					}, (error, data) => {
						vue.$message({
							showClose: true,
							message: data.message,
							type: 'error'
						});
					}
				)
			},
			//保存
			editSubmit() {
				let vue = this;
				vue.editLoading = true;

				// 白名单提交（updateByPrimaryKeySelective：未提交字段保持原值，不影响客服三列）
				let param = {
					id: vue.editForm.id,
					orderSystemExtractionRatio: vue.editForm.orderSystemExtractionRatio
				};

				vue.$http.put(vue, '/rest/admin/setting/update', param,
					(vue, data) => {
						vue.editLoading = false;
						vue.$message({
							showClose: true,
							message: data.message,
							type: 'success'
						});
						vue.getDetail();
					}, (error, data) => {
						vue.editLoading = false;
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
			this.getDetail();
		}
	}
</script>

<style scoped>
	.section-layout {
		margin-top: 20px;
		border: 1px solid #DCDFE6;
	}
	.layout-title {
		color: #606266;
		padding: 15px 20px;
		background: #F2F6FC;
		font-weight: bold;
	}
	.section-content {
		padding: 20px 40px 0px 40px;
	}
	.input-width {
		width: 260px;
	}
	.note-margin {
		margin-left: 10px;
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
		width: 148px;
		height: 148px;
		line-height: 148px;
		text-align: center;
	}
	.submit-bar {
		margin: 20px 0 40px 0;
		text-align: center;
	}
</style>