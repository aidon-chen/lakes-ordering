<template>
	<view class="wl">
		<!-- 有效期提示条 -->
		<view class="wl-notice-wrap">
			<view class="wl-notice">存酒有效期 90 天，临期将通过短信提醒</view>
		</view>

		<!-- 我的存酒码（取第一条存续中记录的存酒码；无则不显示） -->
		<view class="wl-code-card" v-if="mainCode">
			<view class="wl-code-label">我的存酒码</view>
			<view class="wl-code theme-color">{{ mainCode }}</view>
			<view class="wl-code-tip">到店出示即可取酒</view>
		</view>

		<!-- 存酒卡列表 -->
		<view class="wl-card" v-for="wine in list" :key="wine.id">
			<view class="wl-head-row">
				<view class="wl-name">{{ wine.goodsName }}</view>
				<view class="wl-tag" :class="tagClass(wine.statusCode)">{{ wine.statusText }}</view>
			</view>
			<view class="wl-detail">{{ remainText(wine) }}</view>
			<view class="wl-date">存入 {{ wine.storageDate }} · 有效期至 {{ wine.expireDate }}</view>
			<view class="wl-code-row">
				<view class="wl-code-inline theme-color">存酒码 {{ wine.storageCode }}</view>
				<view class="wl-code-inline theme-color">出示即可取酒 ›</view>
			</view>
		</view>

		<view class="wl-empty" v-if="!list.length">{{ loading ? '加载中…' : '暂无存酒记录' }}</view>
	</view>
</template>

<script>
	import https from '../../../../utils/http';
	import authService from '../../../../utils/auth';

	export default {
		data() {
			return {
				list: [],
				loading: true
			};
		},
		computed: {
			/** 存酒码卡展示第一条「存续中」记录的码（批次5 决策：状态措辞统一用 C-01 版） */
			mainCode() {
				const active = this.list.find((item) => Number(item.statusCode) === 1);
				return active ? active.storageCode : '';
			}
		},
		onLoad() {
			authService.checkIsLogin().then((result) => {
				if (result) {
					this.loadList();
					return;
				}
				this.loading = false;
				getApp().globalData.checkIsAuth('scope.userInfo');
			});
		},
		methods: {
			loadList() {
				https.request('/rest/member/wineStorage/list', {}).then((result) => {
					this.loading = false;
					if (result && result.success && result.data) {
						this.list = result.data;
						return;
					}
					this.list = [];
				}).catch(() => {
					this.loading = false;
				});
			},
			remainText(wine) {
				const count = wine.remainCount === null || wine.remainCount === undefined ? wine.bottleCount : wine
					.remainCount;
				return count + ' 瓶' + (wine.shopName ? ' · ' + wine.shopName : '');
			},
			// statusCode：1 存续中 / 2 已取完 / 3 已过期 / 4 将过期
			tagClass(statusCode) {
				if (Number(statusCode) === 4) return 'wl-tag-warn';
				if (Number(statusCode) === 3 || Number(statusCode) === 2) return 'wl-tag-off';
				return 'wl-tag-ok';
			}
		}
	};
</script>

<style>
	page {
		background: #f5f5f5;
	}

	.wl {
		min-height: 100vh;
		padding: 24rpx;
		box-sizing: border-box;
		background: #f5f5f5;
	}

	/* 提示条：底 #FFF7E6 / 字 #8A6D3B（批次5 语义色） */
	.wl-notice-wrap {
		margin-bottom: 20rpx;
	}

	.wl-notice {
		background: #FFF7E6;
		color: #8A6D3B;
		font-size: 24rpx;
		line-height: 36rpx;
		padding: 20rpx;
		border-radius: 16rpx;
	}

	.wl-code-card {
		background: #ffffff;
		border-radius: 24rpx;
		padding: 32rpx;
		margin-bottom: 20rpx;
	}

	.wl-code-label {
		font-size: 24rpx;
		color: #999999;
	}

	.wl-code {
		font-size: 72rpx;
		font-weight: 700;
		line-height: 96rpx;
		margin-top: 12rpx;
	}

	.wl-code-tip {
		font-size: 22rpx;
		color: #999999;
		margin-top: 8rpx;
	}

	.wl-card {
		background: #ffffff;
		border-radius: 24rpx;
		padding: 24rpx;
		margin-bottom: 20rpx;
	}

	.wl-head-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.wl-name {
		font-size: 28rpx;
		font-weight: 600;
		color: #333333;
		flex: 1;
		margin-right: 12rpx;
	}

	.wl-tag {
		font-size: 20rpx;
		height: 40rpx;
		line-height: 40rpx;
		padding: 0 12rpx;
		border-radius: 8rpx;
		flex-shrink: 0;
	}

	.wl-tag-ok {
		background: #E8F5EE;
		color: #43A047;
	}

	.wl-tag-warn {
		background: #FFF7E6;
		color: #E6A23C;
	}

	.wl-tag-off {
		background: #F0F0F0;
		color: #B0B0B0;
	}

	.wl-detail {
		font-size: 24rpx;
		color: #666666;
		margin-top: 16rpx;
	}

	.wl-date {
		font-size: 22rpx;
		color: #999999;
		margin-top: 8rpx;
	}

	.wl-code-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-top: 20rpx;
	}

	.wl-code-inline {
		font-size: 24rpx;
	}

	.wl-empty {
		text-align: center;
		font-size: 26rpx;
		color: #999999;
		padding-top: 120rpx;
	}
</style>
