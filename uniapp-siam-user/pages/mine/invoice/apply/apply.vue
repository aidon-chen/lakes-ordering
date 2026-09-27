<template>
	<view class="ia">
		<view class="ia-body">
		<!-- 订单信息卡：点击「选择订单」弹出选单弹层；入口带 orderId 时仅作预选 -->
		<view class="ia-card" @tap="openPicker">
			<view class="ia-card-title-row">
				<view class="ia-card-title">关联订单</view>
				<view class="ia-pick-link">选择订单 ›</view>
			</view>
			<block v-if="order.orderNo">
				<view class="ia-info-line">订单编号  {{ order.orderNo }}</view>
				<view class="ia-info-line">开票金额  ¥{{ order.actualPrice ? order.actualPrice : '0.00' }}</view>
			</block>
			<view class="ia-info-line" v-else>暂未选择，点「选择订单」挑选要开票的订单</view>
		</view>

			<!-- 抬头类型 -->
			<view class="ia-card">
				<view class="ia-card-title">抬头类型</view>
				<view class="ia-type-row">
					<view class="ia-type" :class="form.titleType === 2 ? 'theme-bg' : 'ia-type-off'"
						@tap="form.titleType = 2">单位</view>
					<view class="ia-type" :class="form.titleType === 1 ? 'theme-bg' : 'ia-type-off'"
						@tap="form.titleType = 1">个人</view>
				</view>
			</view>

			<!-- 开票表单 -->
			<view class="ia-card">
				<view class="ia-label">抬头名称</view>
				<input class="ia-input" v-model="form.title" placeholder="请输入发票抬头名称" placeholder-class="ia-ph" />
				<block v-if="form.titleType === 2">
					<view class="ia-label">纳税人识别号</view>
					<input class="ia-input" v-model="form.taxNo" placeholder="请输入纳税人识别号" placeholder-class="ia-ph" />
				</block>
				<view class="ia-label">接收邮箱</view>
				<input class="ia-input" v-model="form.email" placeholder="电子发票将发送至该邮箱" placeholder-class="ia-ph" />
			</view>

			<!-- 申请记录 -->
			<view class="ia-card" v-if="records.length">
				<view class="ia-card-title">申请记录</view>
				<view class="ia-rec-row" v-for="record in records" :key="record.id">
					<view class="ia-rec-left">
						<view class="ia-rec-title">{{ record.title }}</view>
						<view class="ia-rec-date">{{ fmtTime(record.applyTime) }}</view>
					</view>
					<view class="ia-tag" :class="tagClass(record.status)">{{ tagText(record.status) }}</view>
				</view>
			</view>
		</view>

		<!-- 选单弹层：列出「已完成且未开票」订单，点选回填；点遮罩关闭 -->
		<view class="ia-mask" v-if="showPicker" @tap="showPicker = false">
			<view class="ia-sheet" @tap.stop>
				<view class="ia-sheet-head">
					<view class="ia-card-title">选择开票订单</view>
					<view class="ia-sheet-close" @tap="showPicker = false">×</view>
				</view>
				<scroll-view scroll-y class="ia-sheet-list" v-if="orderList.length">
					<view class="ia-order-item" :class="String(it.id) === String(orderId) ? 'ia-order-item-on' : ''"
						v-for="it in orderList" :key="it.id" @tap="chooseOrder(it)">
						<view class="ia-oi-main">
							<view class="ia-oi-goods">{{ goodsNames(it) || '消费订单' }}</view>
							<view class="ia-oi-sub">{{ it.orderNo }} · 下单 {{ (it.createTime || '').slice(0, 16) }}</view>
						</view>
						<view class="ia-oi-price">¥{{ fmtMoney(it.actualPrice) }}</view>
					</view>
				</scroll-view>
				<view class="ia-sheet-empty" v-else>暂无可开票的已完成订单</view>
			</view>
		</view>

		<!-- 吸底主按钮 -->
		<view class="ia-bottom">
			<view class="ia-btn theme-bg" @tap="submit">提交申请</view>
		</view>
	</view>
</template>

<script>
	import https from '../../../../utils/http';
	import authService from '../../../../utils/auth';

	/** 订单状态：6 = 已完成（与后端 ORDER_STATUS_FINISHED 一致） */
	const ORDER_STATUS_FINISHED = 6;

	export default {
		data() {
			return {
				orderId: '',
				order: {
					orderNo: '',
					actualPrice: ''
				},
				form: {
					titleType: 2, // 1 个人 / 2 单位（与后端 TITLE_TYPE_* 一致）
					title: '',
					taxNo: '',
					email: ''
				},
				orderList: [],
				showPicker: false,
				records: [],
				submitting: false
			};
		},
		onLoad(options) {
			if (options && options.orderId) {
				this.orderId = options.orderId;
			}
			authService.checkIsLogin().then((result) => {
				if (result) {
					this.loadInvoiceableOrders();
					this.loadRecords();
					return;
				}
				getApp().globalData.checkIsAuth('scope.userInfo');
			});
		},
		methods: {
			/** 拉取可开票订单池（已完成+未开票）；入口带 orderId 仅作预选，不自动兜底选单，选单交给用户 */
			loadInvoiceableOrders() {
				https.request('/rest/member/order/list', {
					pageNo: 1,
					pageSize: 50,
					tabType: 'all',
					keyWords: ''
				}).then((result) => {
					if (!result || !result.success || !result.data) return;
					const records = result.data.records || [];
					this.orderList = records.filter((item) => Number(item.status) === ORDER_STATUS_FINISHED &&
						!item.isInvoice);
					if (this.orderId) {
						const matched = this.orderList.find((item) => String(item.id) === String(this.orderId));
						if (matched) this.order = matched;
					}
				});
			},
			/** 打开选单弹层；列表为空时兜底重拉一次（可能刚完成新订单） */
			openPicker() {
				this.showPicker = true;
				if (!this.orderList.length) this.loadInvoiceableOrders();
			},
			/** 点选订单：回填开票对象并收起弹层 */
			chooseOrder(item) {
				this.orderId = item.id;
				this.order = item;
				this.showPicker = false;
			},
			/** 商品名摘要：优先明细逐项名称（与订单列表页同口径），无明细兜底 description */
			goodsNames(item) {
				const list = item.orderDetailList;
				if (list && list.length > 0) {
					return list.map((d) => d.goodsName).filter(Boolean).join('  ');
				}
				return item.description || '';
			},
			/** 金额统一两位小数 */
			fmtMoney(value) {
				const n = Number(value);
				return isNaN(n) ? '0.00' : n.toFixed(2);
			},
			loadRecords() {
				https.request('/rest/member/invoice/list', {}).then((result) => {
					if (result && result.success && result.data) {
						this.records = result.data;
						return;
					}
					this.records = [];
				});
			},
			submit() {
				if (this.submitting) return;
				if (!this.orderId) {
					uni.showToast({
						title: '请先选择要开票的订单',
						icon: 'none'
					});
					return;
				}
				if (!this.form.title.trim()) {
					uni.showToast({
						title: '请输入发票抬头名称',
						icon: 'none'
					});
					return;
				}
				if (this.form.titleType === 2 && !this.form.taxNo.trim()) {
					uni.showToast({
						title: '请输入纳税人识别号',
						icon: 'none'
					});
					return;
				}
				if (!this.form.email.trim() || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.form.email.trim())) {
					uni.showToast({
						title: '请输入正确的接收邮箱',
						icon: 'none'
					});
					return;
				}

				this.submitting = true;
				https.request('/rest/member/invoice/apply', {
					orderId: this.orderId,
					titleType: this.form.titleType,
					title: this.form.title.trim(),
					taxNo: this.form.titleType === 2 ? this.form.taxNo.trim() : null,
					email: this.form.email.trim()
				}).then((result) => {
					this.submitting = false;
					if (!result || !result.success) return;
					uni.showToast({
						title: '申请已提交',
						icon: 'success'
					});
				this.form.title = '';
				this.form.taxNo = '';
				this.form.email = '';
				this.orderId = '';
				this.order = { orderNo: '', actualPrice: '' };
				this.loadRecords();
				this.loadInvoiceableOrders();
				}).catch(() => {
					this.submitting = false;
				});
			},
			tagText(status) {
				if (Number(status) === 2) return '已开具';
				if (Number(status) === 3) return '已驳回';
				return '开票中';
			},
			tagClass(status) {
				if (Number(status) === 2) return 'ia-tag-ok';
				if (Number(status) === 3) return 'ia-tag-off';
				return 'ia-tag-warn';
			},
			fmtTime(value) {
				if (!value) return '';
				if (typeof value === 'number') {
					const d = new Date(value);
					const pad = (n) => (n < 10 ? '0' + n : '' + n);
					return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + ' ' + pad(d
						.getHours()) + ':' + pad(d.getMinutes());
				}
				const str = String(value).replace('T', ' ');
				return str.length >= 16 ? str.substring(0, 16) : str;
			}
		}
	};
</script>

<style>
	page {
		background: #f5f5f5;
	}

	.ia {
		min-height: 100vh;
		background: #f5f5f5;
	}

	.ia-body {
		padding: 24rpx;
		/* 给吸底按钮区留位 */
		padding-bottom: 180rpx;
	}

	.ia-card {
		background: #ffffff;
		border-radius: 24rpx;
		padding: 24rpx;
		margin-bottom: 20rpx;
	}

	.ia-info-line {
		font-size: 24rpx;
		color: #999999;
		line-height: 40rpx;
	}

	.ia-card-title {
		font-size: 28rpx;
		font-weight: 500;
		color: #333333;
	}

	.ia-type-row {
		display: flex;
		align-items: center;
		margin-top: 20rpx;
	}

	.ia-type {
		width: 144rpx;
		height: 60rpx;
		line-height: 60rpx;
		text-align: center;
		font-size: 26rpx;
		border-radius: 30rpx;
		margin-right: 16rpx;
	}

	.ia-type-off {
		color: #666666;
		border: 1rpx solid #EDEDED;
		background: #ffffff;
	}

	.ia-type.theme-bg {
		color: #ffffff;
	}

	.ia-label {
		font-size: 26rpx;
		color: #333333;
		margin-bottom: 16rpx;
	}

	.ia-input {
		width: 100%;
		height: 80rpx;
		line-height: 80rpx;
		background: #f5f5f5;
		border-radius: 16rpx;
		padding: 0 20rpx;
		box-sizing: border-box;
		font-size: 26rpx;
		color: #333333;
		margin-bottom: 28rpx;
	}

	.ia-ph {
		color: #c3c3c3;
		font-size: 26rpx;
	}

	.ia-rec-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-top: 24rpx;
	}

	.ia-rec-left {
		display: flex;
		flex-direction: column;
		flex: 1;
		margin-right: 12rpx;
	}

	.ia-rec-title {
		font-size: 26rpx;
		color: #333333;
	}

	.ia-rec-date {
		font-size: 22rpx;
		color: #999999;
		margin-top: 4rpx;
	}

	.ia-tag {
		font-size: 20rpx;
		height: 40rpx;
		line-height: 40rpx;
		padding: 0 12rpx;
		border-radius: 8rpx;
		flex-shrink: 0;
	}

	.ia-tag-ok {
		background: #E8F5EE;
		color: #43A047;
	}

	.ia-tag-warn {
		background: #FFF7E6;
		color: #E6A23C;
	}

	.ia-tag-off {
		background: #F0F0F0;
		color: #B0B0B0;
	}

	.ia-bottom {
		position: fixed;
		left: 0;
		right: 0;
		bottom: 0;
		padding: 12rpx 32rpx 32rpx 32rpx;
		box-sizing: border-box;
		background: #f5f5f5;
	}

	.ia-btn {
		width: 100%;
		height: 88rpx;
		line-height: 88rpx;
		text-align: center;
		font-size: 30rpx;
		color: #ffffff;
		border-radius: 44rpx;
	}

	/* ===== 选单弹层 ===== */
	.ia-card-title-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-bottom: 12rpx;
	}

	.ia-pick-link {
		font-size: 24rpx;
		color: #5f7d22;
	}

	.ia-mask {
		position: fixed;
		left: 0;
		top: 0;
		right: 0;
		bottom: 0;
		background: rgba(0, 0, 0, 0.45);
		z-index: 999;
		display: flex;
		align-items: flex-end;
	}

	.ia-sheet {
		width: 100%;
		max-height: 70vh;
		background: #ffffff;
		border-radius: 24rpx 24rpx 0 0;
		padding: 24rpx 24rpx 48rpx 24rpx;
		box-sizing: border-box;
	}

	.ia-sheet-head {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding-bottom: 16rpx;
	}

	.ia-sheet-close {
		font-size: 40rpx;
		line-height: 40rpx;
		color: #999999;
		padding: 0 12rpx;
	}

	.ia-sheet-list {
		max-height: 56vh;
	}

	.ia-order-item {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 24rpx 16rpx;
		border-bottom: 1rpx solid #F0F0F0;
	}

	.ia-order-item-on {
		background: #F4F7ED;
		border-radius: 12rpx;
		border-bottom-color: transparent;
	}

	.ia-oi-main {
		flex: 1;
		margin-right: 16rpx;
		overflow: hidden;
	}

	.ia-oi-goods {
		font-size: 26rpx;
		color: #333333;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.ia-oi-sub {
		font-size: 22rpx;
		color: #999999;
		margin-top: 6rpx;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.ia-oi-price {
		font-size: 28rpx;
		font-weight: 500;
		color: #333333;
		flex-shrink: 0;
	}

	.ia-sheet-empty {
		padding: 80rpx 0;
		text-align: center;
		font-size: 26rpx;
		color: #999999;
	}
</style>
