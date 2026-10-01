<template>
	<loading-toast />
	<view class="nl">
		<view class="nl-card" v-for="(item, index) in list" :key="item.id" @tap="goDetail(item)">
			<view class="nl-title-row">
				<view class="nl-title">{{ item.title }}</view>
				<view class="nl-top theme-bg" v-if="item.isTop">置顶</view>
			</view>
			<view class="nl-date">{{ item.publishDate }}</view>
			<view class="nl-summary">{{ item.summary }}</view>
		</view>
		<view class="nl-empty" v-if="!list.length">{{ loading ? '加载中…' : '暂无公告' }}</view>
	</view>
</template>

<script>
	import https from '../../../utils/http';

	export default {
		data() {
			return {
				list: [],
				loading: true
			};
		},
		onLoad() {
			this.loadList();
		},
		onPullDownRefresh() {
			this.loadList();
		},
		methods: {
			// 公告列表为公开接口（/rest/notice/** 不走 member 鉴权），未登录也可浏览
			loadList() {
				https.request('/rest/notice/list', {}).then((result) => {
					this.loading = false;
					uni.stopPullDownRefresh();
					if (result && result.success && result.data) {
						this.list = result.data;
						return;
					}
					this.list = [];
				}).catch(() => {
					this.loading = false;
					uni.stopPullDownRefresh();
				});
			},
			goDetail(item) {
				uni.navigateTo({
					url: '/pages/notice/detail/detail?id=' + item.id
				});
			}
		}
	};
</script>

<style>
	page {
		background: #f5f5f5;
	}

	.nl {
		min-height: 100vh;
		padding: 24rpx;
		box-sizing: border-box;
	}

	.nl-card {
		background: #ffffff;
		border-radius: 24rpx;
		padding: 24rpx;
		margin-bottom: 20rpx;
		display: flex;
		flex-direction: column;
	}

	.nl-title-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.nl-title {
		font-size: 28rpx;
		font-weight: 500;
		color: #333333;
		flex: 1;
		margin-right: 12rpx;
	}

	.nl-top {
		width: 64rpx;
		height: 36rpx;
		line-height: 36rpx;
		text-align: center;
		font-size: 20rpx;
		color: #ffffff;
		border-radius: 8rpx;
		flex-shrink: 0;
	}

	.nl-date {
		font-size: 22rpx;
		color: #999999;
		margin-top: 12rpx;
	}

	.nl-summary {
		font-size: 24rpx;
		color: #666666;
		margin-top: 12rpx;
		line-height: 36rpx;
	}

	.nl-empty {
		text-align: center;
		font-size: 26rpx;
		color: #999999;
		padding-top: 120rpx;
	}
</style>
