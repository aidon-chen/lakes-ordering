<template>
	<section>
		<el-card class="box-card">
			<div slot="header">
				<span>打印与提醒设置</span>
			</div>
			<el-form label-width="140px" style="max-width: 560px;">
				<el-form-item label="订单语音提醒">
					<el-switch v-model="isOpenOrderAudio" active-color="#1890ff" active-text="开启" inactive-text="关闭"
						@change="changeSwitch('isOpenOrderAudio', $event)" />
					<div class="hint">新订单到达时，商家端中心是否语音播报。</div>
				</el-form-item>
				<el-form-item label="本地打印">
					<el-switch v-model="isOpenLocalPrint" active-color="#1890ff" active-text="开启" inactive-text="关闭"
						@change="changeSwitch('isOpenLocalPrint', $event)" />
					<div class="hint">新订单到达时，是否向前台本地打印机推送小票。</div>
				</el-form-item>
				<el-form-item label="云打印">
					<el-switch v-model="isOpenCloudPrint" active-color="#1890ff" active-text="开启" inactive-text="关闭"
						@change="changeSwitch('isOpenCloudPrint', $event)" />
					<div class="hint">新订单到达时，是否向云打印机（飞鹅云/芯烨云）推送小票。</div>
					<div class="warn">注：云打印开关当前仅保存配置，后端尚未启用门控，切换暂不直接影响实际打印（待后续后端接入）。</div>
				</el-form-item>
			</el-form>
		</el-card>
	</section>
</template>

<script>
export default {
	data() {
		return {
			shopId: null,                 // 当前店铺 id（来自 getLoginMerchantShopInfo）
			isOpenOrderAudio: false,     // 订单语音提醒
			isOpenLocalPrint: false,     // 本地打印
			isOpenCloudPrint: false,     // 云打印（占位，后端未门控）
			settingLoading: false
		}
	},
	methods: {
		// 拉取本店信息，回填店铺 id 与 3 个开关初值
		getShopSetting() {
			let vue = this;
			vue.$http.post(vue, '/rest/merchant/shop/getLoginMerchantShopInfo', {},
				(vue, data) => {
					let shop = data.data || {};
					vue.shopId = shop.id;
					vue.isOpenOrderAudio = !!shop.isOpenOrderAudio;
					vue.isOpenLocalPrint = !!shop.isOpenLocalPrint;
					vue.isOpenCloudPrint = !!shop.isOpenCloudPrint;
				}, (error, data) => {
					vue.$message({ showClose: true, message: data.message, type: 'error' });
				}
			);
		},
		// 切换开关：写回 tb_shop 对应字段，成功后重新拉取刷新
		changeSwitch(field, value) {
			let vue = this;
			if (!vue.shopId) {
				vue.$message({ showClose: true, message: '店铺信息未加载，请刷新重试', type: 'warning' });
				vue.getShopSetting(); // 复位 UI 到服务端真实值
				return;
			}
			vue.settingLoading = true;
			let param = { id: vue.shopId };
			param[field] = value; // 仅带变化的那个开关字段
			vue.$http.post(vue, '/rest/merchant/shop/update', param,
				(vue, data) => {
					vue.settingLoading = false;
					vue.$message({ showClose: true, message: data.message || '保存成功', type: 'success' });
					vue.getShopSetting(); // 重新拉取，确保开关状态与服务端一致
				}, (error, data) => {
					vue.settingLoading = false;
					vue.getShopSetting(); // 失败则回滚 UI 到服务端真实值
					vue.$message({ showClose: true, message: data.message, type: 'error' });
				}
			);
		}
	},
	mounted() {
		this.getShopSetting();
	}
}
</script>

<style scoped>
.box-card {
	max-width: 720px;
}
.hint {
	color: #999;
	font-size: 12px;
	margin-top: 4px;
	line-height: 1.5;
}
.warn {
	color: #e6a23c;
	font-size: 12px;
	margin-top: 4px;
	line-height: 1.5;
}
</style>
