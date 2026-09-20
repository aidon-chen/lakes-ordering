<template>
	<section>
		<!-- 分区标题 + 表单：基础参数配置（A-02-1 三段重列） -->
		<div class="section-layout">
			<div class="layout-title">会员与积分</div>
			<div class="section-content">
				<el-form :model="editForm" label-width="220px" style="width: 70%;" ref="editForm">
					<el-form-item label="新用户注册赠送" prop="registrationRewardPoints">
						<el-input v-model="editForm.registrationRewardPoints" class="input-width">
							<template slot="append">个</template>
						</el-input>
						<span class="note-margin">积分</span>
					</el-form-item>
					<el-form-item label="新用户注册赠送" prop="registrationRewardInviteRewardAmount">
						<el-input v-model="editForm.registrationRewardInviteRewardAmount" class="input-width">
							<template slot="append">个</template>
						</el-input>
						<span class="note-margin">奖励金</span>
					</el-form-item>
					<el-form-item label="购买1杯赠送" prop="purchaseRewardPoints">
						<el-input v-model="editForm.purchaseRewardPoints" class="input-width">
							<template slot="append">个</template>
						</el-input>
						<span class="note-margin">积分</span>
					</el-form-item>
					<el-form-item label="奖励金累计到(≥)" prop="memberWithdrawMeetAmount">
						<el-input v-model="editForm.memberWithdrawMeetAmount" class="input-width">
							<template slot="append">元</template>
						</el-input>
						<span class="note-margin">可以提现</span>
					</el-form-item>
					<el-form-item label="用户提现手续费比例" prop="memberWithdrawFee">
						<el-input v-model="editForm.memberWithdrawFee" class="input-width">
							<template slot="append">%</template>
						</el-input>
					</el-form-item>
					<el-form-item label="用户提现金额达到(≥)" prop="memberWithdrawAuditThreshold">
						<el-input v-model="editForm.memberWithdrawAuditThreshold" class="input-width">
							<template slot="append">元</template>
						</el-input>
						<span class="note-margin">需要人工审核</span>
					</el-form-item>
				</el-form>
			</div>
		</div>

		<div class="section-layout">
			<div class="layout-title">经营与收款</div>
			<div class="section-content">
				<el-form :model="editForm" label-width="220px" style="width: 70%;" ref="editForm2">
					<el-form-item label="平台抽佣比例" prop="orderSystemExtractionRatio">
						<el-input v-model="editForm.orderSystemExtractionRatio" class="input-width">
							<template slot="append">%</template>
						</el-input>
						<span class="note-margin">用户下单系统抽佣</span>
					</el-form-item>
					<el-form-item label="客服电话" prop="customerServicePhone">
						<el-input v-model="editForm.customerServicePhone" class="input-width"></el-input>
					</el-form-item>
					<el-form-item label="客服微信" prop="customerServiceWechat">
						<el-input v-model="editForm.customerServiceWechat" class="input-width"></el-input>
					</el-form-item>
					<el-form-item label="客服微信二维码" prop="customerServiceWechatQrcodeFile">
						<el-upload
							class="avatar-uploader"
							action=""
							name="customerServiceWechatQrcodeUpload"
							:limit="1"
							:file-list="editForm.customerServiceWechatQrcodeFile"
							:before-upload="beforeAvatarUpload"
							:http-request="upload"
							:on-remove="handleRemoveImage('customerServiceWechatQrcodeFile')"
							list-type="picture-card">
							<i class="el-icon-plus avatar-uploader-icon"></i>
						</el-upload>
					</el-form-item>
					<el-form-item label="营业时间说明" prop="businessHoursDesc">
						<el-input v-model="editForm.businessHoursDesc" class="input-width" placeholder="如：周一至周日 18:00-02:00"></el-input>
					</el-form-item>
				</el-form>
			</div>
		</div>

		<div class="section-layout">
			<div class="layout-title">协议与图片</div>
			<div class="section-content">
				<el-form :model="editForm" label-width="220px" style="width: 70%;" ref="editForm3">
					<el-form-item label="用户服务协议" prop="userServiceAgreement">
						<el-input type="textarea" :rows="6" v-model="editForm.userServiceAgreement" placeholder="请输入用户服务协议内容"></el-input>
					</el-form-item>
					<el-form-item label="隐私政策" prop="privacyPolicy">
						<el-input type="textarea" :rows="6" v-model="editForm.privacyPolicy" placeholder="请输入隐私政策内容"></el-input>
					</el-form-item>
					<el-form-item label="平台logo（宽200px * 高200px）" prop="platformLogoFile">
						<el-upload
							class="avatar-uploader"
							action=""
							name="platformLogoUpload"
							:limit="1"
							:file-list="editForm.platformLogoFile"
							:before-upload="beforeAvatarUpload"
							:http-request="upload"
							:on-remove="handleRemoveImage('platformLogoFile')"
							list-type="picture-card">
							<i class="el-icon-plus avatar-uploader-icon"></i>
						</el-upload>
					</el-form-item>
					<el-form-item label="首页海报位（宽750px * 高400px）" prop="homePosterFile">
						<el-upload
							class="avatar-uploader"
							action=""
							name="homePosterUpload"
							:limit="1"
							:file-list="editForm.homePosterFile"
							:before-upload="beforeAvatarUpload"
							:http-request="upload"
							:on-remove="handleRemoveImage('homePosterFile')"
							list-type="picture-card">
							<i class="el-icon-plus avatar-uploader-icon"></i>
						</el-upload>
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
				//编辑界面数据（含图片上传用的 file-list）
				editForm: {
					customerServiceWechatQrcodeFile: [],
					platformLogoFile: [],
					homePosterFile: []
				}
			}
		},
		methods: {
			//单图转 file-list（回显）
			singleFileList(path, name) {
				if (!path) {
					return []
				}
				return [{
					url: this.$http.publicUrl(path),
					upUrl: path,
					uid: name
				}]
			},
			beforeAvatarUpload(file) {
				const isLt2M = file.size / 1024 / 1024 < 6;
				if (!isLt2M) {
					this.$message({
						showClose: true,
						message: '上传图片大小不能超过 6MB!',
						type: 'error'
					});
				}
				return isLt2M;
			},
			//图片上传（统一入口，按 name 区分落到哪个字段）
			upload(option) {
				let vue = this;
				let formData = new FormData();
				formData.append('file', option.file);

				vue.$http.postupload(vue, '/rest/admin/uploadSingleImage', formData,
					function (vue, data) {
						option.onSuccess();
						let target = '';
						if (option.filename === 'customerServiceWechatQrcodeUpload') {
							target = 'customerServiceWechatQrcodeFile';
						} else if (option.filename === 'platformLogoUpload') {
							target = 'platformLogoFile';
						} else {
							target = 'homePosterFile';
						}
						//单图：替换原图
						vue.editForm[target] = [{
							url: vue.$http.publicUrl(data.data),
							uid: option.file.uid,
							upUrl: data.data
						}];
					},
					function (error) {
						option.onError();
					})
			},
			//删除图片（返回一个处理器，避免在模板里写内联箭头函数）
			handleRemoveImage(field) {
				let vue = this;
				return function () {
					vue.editForm[field] = [];
				}
			},
			//取单图存储路径
			getSingleUpUrl(fileList) {
				if (fileList && fileList.length > 0) {
					return fileList[0].upUrl;
				}
				return null;
			},
			//获取当前参数配置
			getDetail() {
				let vue = this;
				vue.$http.post(vue, '/rest/admin/setting/selectCurrent', {},
					(vue, data) => {
						let setting = data.data || {};
						setting.customerServiceWechatQrcodeFile = vue.singleFileList(setting.customerServiceWechatQrcode, 'qrcode');
						setting.platformLogoFile = vue.singleFileList(setting.platformLogo, 'logo');
						setting.homePosterFile = vue.singleFileList(setting.homePoster, 'poster');
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

				let param = {
					id: vue.editForm.id,
					registrationRewardPoints: vue.editForm.registrationRewardPoints,
					registrationRewardInviteRewardAmount: vue.editForm.registrationRewardInviteRewardAmount,
					purchaseRewardPoints: vue.editForm.purchaseRewardPoints,
					memberWithdrawMeetAmount: vue.editForm.memberWithdrawMeetAmount,
					memberWithdrawFee: vue.editForm.memberWithdrawFee,
					memberWithdrawAuditThreshold: vue.editForm.memberWithdrawAuditThreshold,
					orderSystemExtractionRatio: vue.editForm.orderSystemExtractionRatio,
					customerServicePhone: vue.editForm.customerServicePhone,
					customerServiceWechat: vue.editForm.customerServiceWechat,
					businessHoursDesc: vue.editForm.businessHoursDesc,
					userServiceAgreement: vue.editForm.userServiceAgreement,
					privacyPolicy: vue.editForm.privacyPolicy,
					customerServiceWechatQrcode: vue.getSingleUpUrl(vue.editForm.customerServiceWechatQrcodeFile),
					platformLogo: vue.getSingleUpUrl(vue.editForm.platformLogoFile),
					homePoster: vue.getSingleUpUrl(vue.editForm.homePosterFile)
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