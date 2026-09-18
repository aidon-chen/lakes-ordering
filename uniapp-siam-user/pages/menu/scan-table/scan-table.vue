<template>
	<view class="page">
		<!-- 成功区 -->
		<view class="success-area">
			<view class="success-icon">
				<view class="check-mark"></view>
			</view>
			<text class="success-title">扫码成功</text>
			<text class="success-desc">已自动识别桌台，确认人数后即可点餐</text>
		</view>

		<!-- 台位卡 -->
		<view class="card">
			<text class="card-title">桌台信息</text>
			<view class="card-row">
				<text class="row-label">桌号</text>
				<text class="row-value">{{ tableLabel }}</text>
			</view>
		</view>

		<!-- 人数卡 -->
		<view class="card">
			<text class="card-title">就餐人数</text>
			<view class="stepper-row">
				<view class="stepper">
					<view class="stepper-btn" @tap="decrease">－</view>
					<text class="stepper-num">{{ personCount }}</text>
					<view class="stepper-btn" @tap="increase">＋</view>
				</view>
			</view>
			<text class="range-tip">限 {{ minPerson }} - {{ maxPerson }} 人</text>
		</view>

		<!-- 底部按钮区 -->
		<view class="btn-area">
			<view class="main-btn" @tap="startOrder">开始点餐</view>
		</view>
	</view>
</template>

<script>
	let app = null;
	export default {
		data() {
			return {
				tableId: '',
				area: 'A',
				tableType: '散台',
				personCount: 4,
				minPerson: 2,
				maxPerson: 12
			};
		},
		computed: {
			tableLabel() {
				let label = '';
				if (this.area) {
					label += this.area + ' 区 · ';
				}
				label += this.tableType + ' ';
				label += this.tableId || '-';
				return label;
			}
		},
		onLoad(options) {
			app = getApp();
			// 桌台二维码携带参数：优先 scene（小程序码），格式如 tid=05&area=A&min=800
			let scene = '';
			if (options && options.scene) {
				try {
					scene = decodeURIComponent(options.scene);
				} catch (e) {
					scene = options.scene;
				}
			}
			const params = {};
			if (scene) {
				scene.split('&').forEach((kv) => {
					const idx = kv.indexOf('=');
					if (idx > 0) {
						const k = kv.substring(0, idx).trim();
						const v = kv.substring(idx + 1).trim();
						try {
							params[k] = decodeURIComponent(v);
						} catch (e) {
							params[k] = v;
						}
					}
				});
			}
			this.tableId = params.tid || params.tableId || params.tableNo || (options && options.tableNo) || '';
			this.area = params.area || (options && options.area) || '';
			this.tableType = params.type || (options && options.type) || '散台';
			// TODO: 后端桌台接口就绪后，按 tableId 拉取桌台信息（可容纳人数等）
		},
		methods: {
			decrease() {
				if (this.personCount > this.minPerson) {
					this.personCount--;
				}
			},
			increase() {
				if (this.personCount < this.maxPerson) {
					this.personCount++;
				}
			},
			startOrder() {
				if (!this.tableId) {
					uni.showToast({
						title: '未识别到桌号，请重新扫码',
						icon: 'none'
					});
					return;
				}
				const dineInfo = {
					tableId: this.tableId,
					area: this.area,
					tableNo: (this.area ? this.area + '区' : '') + this.tableId + '桌',
					tableLabel: this.tableLabel,
					personCount: this.personCount
				};
				// 通过 globalData 传递桌号（menu/index 为 tabBar 页，switchTab 不能带参）
				app.globalData.deliveryAndSelfTaking.dineInfo = dineInfo;
				uni.switchTab({
					url: '/pages/menu/index/index'
				});
			}
		}
	};
</script>

<style>
	.page {
		background: #ffffff;
		min-height: 100vh;
		display: flex;
		flex-direction: column;
	}

	/* 成功区 */
	.success-area {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 16rpx;
		padding: 32rpx 0 8rpx 0;
	}

	.success-icon {
		width: 112rpx;
		height: 112rpx;
		border-radius: 56rpx;
		background: #004ca0;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.check-mark {
		width: 52rpx;
		height: 26rpx;
		border-left: 6rpx solid #ffffff;
		border-bottom: 6rpx solid #ffffff;
		transform: rotate(-45deg);
		margin-top: -10rpx;
	}

	.success-title {
		font-size: 40rpx;
		font-weight: 500;
		color: #333333;
		margin-top: 4rpx;
	}

	.success-desc {
		font-size: 26rpx;
		color: #999999;
	}

	/* 卡片 */
	.card {
		background: #ffffff;
		border: 1rpx solid #f0f0f0;
		border-radius: 24rpx;
		padding: 32rpx;
		margin: 24rpx 0 0 0;
		display: flex;
		flex-direction: column;
		gap: 24rpx;
	}

	.card-title {
		font-size: 30rpx;
		font-weight: 500;
		color: #333333;
	}

	.card-row {
		display: flex;
		align-items: center;
		gap: 24rpx;
	}

	.row-label,
	.row-value {
		font-size: 28rpx;
		color: #333333;
	}

	.row-label {
		color: #999999;
	}

	/* 步进器 */
	.stepper-row {
		display: flex;
		align-items: center;
	}

	.stepper {
		display: flex;
		align-items: center;
	}

	.stepper-btn {
		width: 52rpx;
		height: 52rpx;
		border: 1rpx solid #ededed;
		border-radius: 8rpx;
		background: #ffffff;
		color: #333333;
		font-size: 28rpx;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.stepper-num {
		width: 104rpx;
		text-align: center;
		font-size: 28rpx;
		font-weight: 500;
		color: #333333;
	}

	.range-tip {
		font-size: 22rpx;
		color: #b0b0b0;
	}

	/* 底部按钮 */
	.btn-area {
		padding: 32rpx;
	}

	.main-btn {
		height: 88rpx;
		background: #004ca0;
		color: #ffffff;
		font-size: 30rpx;
		font-weight: 500;
		display: flex;
		align-items: center;
		justify-content: center;
	}
</style>
