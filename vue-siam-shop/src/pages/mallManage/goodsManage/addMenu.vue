<template>
  <section>
    <!-- 新增商城分类（A2 09-30：图标上传已下线；A1/A3 新增 排序号/状态） -->
    <el-form :model="editForm" label-width="150px" class="editForm" style="width: 80%;" :rules="editFormRules" ref="editForm">
				<el-form-item label="分类名称" prop="name">
					<el-input v-model="editForm.name"></el-input>
				</el-form-item>
				<!-- A1：排序号（数字越小越靠前；留空按 0 提交，避免后端跳过 null 导致改不动） -->
				<el-form-item label="排序号" prop="sortNumber">
					<el-input v-model="editForm.sortNumber" class="minInput" placeholder="数字越小越靠前，留空按 0 处理"></el-input>
				</el-form-item>
				<!-- A3：状态（禁用后顾客端商城分类页不再显示） -->
				<el-form-item label="状态" prop="isDisabled">
					<el-select v-model="editForm.isDisabled" placeholder="请选择">
						<el-option label="正常" :value="0"></el-option>
						<el-option label="禁用" :value="1"></el-option>
					</el-select>
				</el-form-item>
				<el-form-item label="描述" prop="description">
					<el-input type="textarea" :rows="5" v-model="editForm.description"></el-input>
				</el-form-item>
			</el-form>
			<div slot="footer" class="el-dialog__footer">
				<el-button @click="goBack">取消</el-button>
				<el-button type="primary" @click="saveGoodsMsg">保存</el-button>
			</div>
  </section>
</template>
<script>
export default {
  data() {
    return {
      menuList: [],
      editForm: {
				isDisabled: 0,      // 默认「正常」
				sortNumber: '',     // 留空按 0 提交
				name: '',
				description: '',
      },
      editFormRules: {
				name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
				description: [{ required: false, message: '请输入分类描述', trigger: 'blur' }],
				sortNumber: [{ validator: this.validateSortNumber, trigger: 'blur' }],
      }
    }
  },
  methods: {
    // 排序号：留空合法（按 0），填了必须是数字
    validateSortNumber(rule, value, callback) {
      if (value === '' || value === null || value === undefined) return callback()
      if (isNaN(Number(value))) return callback(new Error('排序号需为数字'))
      callback()
    },
    saveGoodsMsg() {
		this.$refs.editForm.validate((valid) => {
			if (valid) {
				this.editLoading = true;
				let vue = this

				let param = Object.assign({}, this.editForm);

				// A1：排序号归一为数字，空值按 0 提交（口径同商品页 addGoods）
				param.sortNumber = (param.sortNumber === '' || param.sortNumber === null || param.sortNumber === undefined) ? 0 : Number(param.sortNumber);
				// A3：状态兜底，避免提交 undefined
				param.isDisabled = (param.isDisabled === null || param.isDisabled === undefined) ? 0 : param.isDisabled;

				let url = '';
				if(param.id){
					url = '/rest/admin/pointsMall/menu/update';
					vue.$http.put(vue, url, param,
						(vue, data) => {
							this.editLoading = false;
							vue.$message({
								showClose: true,
								message: data.message,
								type: 'success'
							});

							vue.goBack()
							vue.editFormVisible = false;
						}, (error, data) => {
							vue.editFormVisible = false;
							vue.$message({
								showClose: true,
								message: data.message,
								type: 'error'
							});
						}
					)
				}else{
					url = '/rest/admin/pointsMall/menu/insert';
					vue.$http.post(vue, url, param,
						(vue, data) => {
							this.editLoading = false;
							vue.$message({
								showClose: true,
								message: data.message,
								type: 'success'
							});
							vue.goBack()
							vue.editFormVisible = false;
						}, (error, data) => {
							vue.editFormVisible = false;
							vue.$message({
								showClose: true,
								message: data.message,
								type: 'error'
							});
						}
					)
				}
			}
		});
    },
    goBack() {
      this.$refs['editForm'].resetFields();
      this.$router.push({path: 'pointsMall_menuList'})
    },
    getTypeList() { // 获取商品类别列表
				let vue = this
				let param = {
          pageNo: -1,
          pageSize: 10,
          typestatus: 0
        }
				vue.$http.post(vue, '/rest/admin/pointsMall/menu/list', param,
					(vue, data) => {
						vue.menuList = data.data.records
					},(error, data)=> {
						vue.$message({
							showClose: true,
							message: data.message,
							type: 'error'
						});
					}
				)
		},
  },
  created() {
    this.getTypeList();
    this.editForm.status = 1;
    //开启订单自动打印定时器
    this.$orderPrint.init();
  }
}
</script>
<style scoped>
.editForm .el-input {
  width: 420px;
}
.standForm .el-input {
  width: 200px;
}
.editForm .minInput {
  width: 240px;
}
</style>
