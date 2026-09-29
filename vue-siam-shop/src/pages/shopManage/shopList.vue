<template>
	<section>
		<!-- 单店「本店信息」页（09-29 方案A）：每分店一账号只管自己店；不再有多店列表/搜索/分页/启停 -->
		<el-col :span="24" class="page-tip">
			<i class="el-icon-info"></i>
			当前账号仅管理本店资料，保存后立即生效（顾客端/收银端同步读取）。图片如需更换请直接重新上传；清空图片不保存即保持原图。
		</el-col>

		<!--本店信息编辑表单（数据源＝登录账号绑定的门店；保存走 merchant 隔离接口）-->
		<el-form :model="editForm" label-width="170px" style="width: 680px; margin-top: 12px;" :rules="editFormRules" ref="editForm" v-loading="pageLoading">
			<el-form-item label="门店名称" prop="name">
				<el-input v-model="editForm.name" maxlength="30"></el-input>
			</el-form-item>

			<el-form-item prop="contactPhone" label="联系电话">
				<el-input type="text" v-model="editForm.contactPhone" auto-complete="off"></el-input>
			</el-form-item>

			<el-form-item prop="selectedOptions" label="所在城市">
				<el-cascader style="width:100%;" :options="options" separator="-" :props="props" clearable change-on-select
					v-model="editForm.selectedOptions">
				</el-cascader>
			</el-form-item>

			<el-form-item prop="street" label="门店地址">
				<el-input type="text" v-model="editForm.street" auto-complete="off"></el-input>
			</el-form-item>

			<el-form-item prop="houseNumber" label="门牌号">
				<el-input type="text" v-model="editForm.houseNumber" auto-complete="off" placeholder="如：A区101（顾客端与门店地址拼接展示）"></el-input>
			</el-form-item>

			<el-form-item prop="managePrimary" label="营业类目">
				<el-input type="text" v-model="editForm.managePrimary" auto-complete="off"></el-input>
			</el-form-item>

			<el-form-item label="门店头像（宽400px * 高400px）" prop="shopLogoImgFile" class="shopLogoImgFileItem">
				<el-upload
					class="avatar-uploader" accept=".image, .png, .jpg"
					action="" multiple name="shopLogoImgUpload"
					:on-remove="handleRemoveShopLogoImg"
					:file-list="editForm.shopLogoImgFile"
					:before-upload="beforeAvatarUpload" :http-request="upload"
					list-type="picture-card">
					<i class="el-icon-plus avatar-uploader-icon"></i>
				</el-upload>
			</el-form-item>

			<el-form-item label="店内照片（宽600px * 高355px）" prop="shopWithinImgFile" class="shopWithinImgFileItem">
				<el-upload
					ref="multipleUpload"
					:file-list="editForm.shopWithinImgFile"
					class="avatar-uploader" action="" accept=".image, .png, .jpg" multiple name="shopWithinImgUpload"
					:on-remove="handleRemoveShopWithinImg"
					:before-upload="beforeAvatarUpload" :http-request="upload"
					list-type="picture-card" >
					<i class="el-icon-plus avatar-uploader-icon"></i>
				</el-upload>
			</el-form-item>

			<el-form-item label="店铺是否营业" prop="isOperating">
				<!-- 直接绑布尔值（与后端 Boolean 对齐），提交无需字符串转换 -->
				<el-radio-group v-model="editForm.isOperating" size="medium">
					<el-radio-button :label="true">正常营业</el-radio-button>
					<el-radio-button :label="false">暂不营业</el-radio-button>
				</el-radio-group>
			</el-form-item>

			<el-form-item label="店铺营业开始时间" prop="startTime">
				<el-input v-model="editForm.startTime" placeholder="如：0:00"></el-input>
			</el-form-item>

			<el-form-item label="店铺营业结束时间" prop="endTime">
				<el-input v-model="editForm.endTime" placeholder="如：23:59"></el-input>
			</el-form-item>

			<el-form-item label="门店公告" prop="announcement">
				<el-input v-model="editForm.announcement"></el-input>
			</el-form-item>

			<el-form-item label="门店简介" prop="briefIntroduction">
				<el-input type="textarea" :rows="6" v-model="editForm.briefIntroduction"></el-input>
			</el-form-item>

			<!-- 打印机绑定已迁至「打印机管理→小票打印机列表」页（2026-09-29 用户拍板，绑定属打印机域配置） -->

			<el-form-item>
				<el-button type="primary" @click.native="saveShopMsg" :loading="saveLoading">保存</el-button>
			</el-form-item>
		</el-form>
	</section>
</template>

<script>
	import cityData from '../../../static/citys.json';
	export default {
		data() {
			return {
				options: cityData,
				props: {
					value: 'label',
					children: 'children'
				},

				pageLoading: false, // 进页取本店信息的 loading
				saveLoading: false,

				// 必填只拦强链接字段（顾客端/收银端直接消费），其余放宽
				editFormRules: {
					name: [{ required: true, message: '请输入门店名称', trigger: 'blur' }],
					street: [{ required: true, message: '请输入门店地址', trigger: 'blur' }],
					startTime: [{ required: true, message: '请输入店铺营业开始时间', trigger: 'blur' }],
					endTime: [{ required: true, message: '请输入店铺营业结束时间', trigger: 'blur' }],
				},

				// 本店信息表单（isOperating 直接用布尔，与后端 Boolean 对齐）
				editForm: {
					id: null,
					name: '',
					contactPhone: '',
					selectedOptions: [], // [province, city, area]
					street: '',
					houseNumber: '',
					managePrimary: '',
					shopLogoImgFile: [], // 门店头像
					shopWithinImgFile: [], // 店内照片
					isOperating: true,
					startTime: '',
					endTime: '',
					announcement: '',
					briefIntroduction: '',
				}
			}
		},
		methods: {
			// 图片列表 → 逗号拼接的相对路径串（空列表由调用方决定传 null 保持原值）
			getIdByArr(data) {
				let arr = data || []
				let url = arr.map((item) => {
					return item.oldUrl
				});
				return url.join(',')
			},
			// 后端相对路径串 → el-upload 文件列表（回显用）
			resetImg(row, imgType) {
				let data = row
				if (row[imgType]) {
					let arr = row[imgType].split(',');
					let newArr = []
					let index = 0
					arr.forEach(ele => {
						let obj = {
							url: this.$http.publicUrl(ele),
							oldUrl: ele,
							name: index + 1
						}
						newArr.push(obj)
						index++
					});
					return newArr
				} else {
					return []
				}
			},
			handleRemoveShopLogoImg(file, filelist) {
				let arr = this.editForm.shopLogoImgFile
				this.editForm.shopLogoImgFile = arr.filter(function(item) {
					return item.uid !== file.uid
				});
			},
			handleRemoveShopWithinImg(file, filelist) {
				let arr = this.editForm.shopWithinImgFile
				this.editForm.shopWithinImgFile = arr.filter(function(item) {
					return item.uid !== file.uid
				});
			},
			beforeAvatarUpload(file) {
				const isLt6M = file.size / 1024 / 1024 < 6;
				if (!isLt6M) {
					this.$message({
						showClose: true,
						message: '上传图片大小不能超过 6MB!',
						type: 'error'
					});
				}
				return isLt6M;
			},
			upload(option) { // 图片上传（merchant 口，与登录身份一致）
				let vue = this;
				let value = option.file;
				let formData = new FormData();
				formData.append('file', value);
				vue.$http.postupload(
					vue, '/rest/merchant/uploadSingleImage', formData,
					function (vue, data) {
						option.onSuccess();
						let obj = {
							oldUrl: data.data,
							url: vue.$http.publicUrl(data.data),
							uid: option.file.uid,
						}
						if (option.filename === 'shopWithinImgUpload') {
							vue.editForm.shopWithinImgFile.push(obj)
						} else if (option.filename === 'shopLogoImgUpload') {
							vue.editForm.shopLogoImgFile.push(obj)
						}
					},
					function (error) {
						option.onError();
					})
			},
			// 进页取本店信息：后端按登录账号的 shopId 直查本店（无需传 id）
			getDetail() {
				let vue = this;
				vue.pageLoading = true;
				vue.$http.post(vue, '/rest/merchant/shop/getLoginMerchantShopInfo', {},
					(vue, data) => {
						let shop = data.data || {};
						vue.editForm = {
							id: shop.id,
							name: shop.name,
							contactPhone: shop.contactPhone,
							// 省市区回填进级联选择器；清空后再保存＝传 null 保持原值
							selectedOptions: (shop.province && shop.city) ? [shop.province, shop.city, shop.area] : [],
							street: shop.street,
							houseNumber: shop.houseNumber,
							managePrimary: shop.managePrimary,
							shopLogoImgFile: vue.resetImg(shop, 'shopLogoImg'),
							shopWithinImgFile: vue.resetImg(shop, 'shopWithinImg'),
							isOperating: shop.isOperating === true,
							startTime: shop.startTime,
							endTime: shop.endTime,
							announcement: shop.announcement,
							briefIntroduction: shop.briefIntroduction,
						};
						vue.pageLoading = false;
					}, (error, data) => {
						vue.pageLoading = false;
						vue.$message({
							showClose: true,
							message: data.message,
							type: 'error'
						});
					}
				);
			},
			// 本店信息保存：白名单字段提交（merchant 口自带隔离守卫；updateById 忽略 null＝未提交字段不覆盖）
			saveShopMsg: function () {
				let vue = this;
				vue.$refs['editForm'].validate((valid) => {
					if (!valid) return;
					let f = vue.editForm;
					let selected = f.selectedOptions || [];
					let param = {
						id: f.id,
						name: f.name,
						contactPhone: f.contactPhone,
						province: selected[0] || null,
						city: selected[1] || null,
						area: selected[2] || null,
						street: f.street,
						houseNumber: f.houseNumber,
						managePrimary: f.managePrimary,
						// 图片：空传 null（后端忽略 null＝保持原图；不支持「清空」，换图即重传）
						shopLogoImg: f.shopLogoImgFile.length ? vue.getIdByArr(f.shopLogoImgFile) : null,
						shopWithinImg: f.shopWithinImgFile.length ? vue.getIdByArr(f.shopWithinImgFile) : null,
						isOperating: f.isOperating === true,
						startTime: f.startTime,
						endTime: f.endTime,
						announcement: f.announcement,
						briefIntroduction: f.briefIntroduction,
					};
					vue.saveLoading = true;
					// ⚠ merchant/update 是 @PostMapping，必须 POST（旧 admin 口才是 PUT）
					vue.$http.post(vue, '/rest/merchant/shop/update', param,
						(vue, data) => {
							vue.saveLoading = false;
							vue.$message({
								showClose: true,
								message: '本店信息已保存',
								type: 'success'
							});
							vue.getDetail(); // 保存后回读，展示落库后的真实值
						}, (error, data) => {
							vue.saveLoading = false;
							vue.$message({
								showClose: true,
								message: data.message,
								type: 'error'
							});
						}
					);
				});
			}
		},
		mounted() {
			this.getDetail();
			//开启订单自动打印定时器
			this.$orderPrint.init();
		}
	}

</script>

<style scoped>
	.page-tip {
		padding: 10px 12px;
		background: #f4f6f8;
		color: #606266;
		font-size: 13px;
		border-radius: 4px;
	}
	.avatar-uploader .el-upload {
		border: 1px dashed #d9d9d9;
		border-radius: 6px;
		cursor: pointer;
		position: relative;
		overflow: hidden;
	}
	.avatar-uploader-icon {
		font-size: 28px;
		color: #8c939d;
		width: 148px;
		height: 148px;
		line-height: 148px;
		text-align: center;
	}
</style>
