<template>
	<view class="nd">
		<block v-if="notice.id">
			<view class="nd-title">{{ notice.title }}</view>
			<view class="nd-date">{{ publishLine }}</view>
			<!-- 富文本正文：含标签走 rich-text，纯文本直接渲染 -->
			<rich-text class="nd-content" v-if="isRichText" :nodes="notice.content"></rich-text>
			<view class="nd-content" v-else>{{ notice.content }}</view>
		</block>
		<view class="nd-empty" v-if="!notice.id">{{ loading ? '加载中…' : '公告不存在或已下架' }}</view>
	</view>
</template>

<script>
	import https from '../../../utils/http';

	export default {
		data() {
			return {
				id: '',
				loading: true,
				notice: {
					id: '',
					title: '',
					content: '',
					publishDate: '',
					shopName: ''
				}
			};
		},
		computed: {
			isRichText() {
				return !!(this.notice.content && this.notice.content.indexOf('<') >= 0);
			},
			publishLine() {
				// 门店公告带门店名，平台公告只有日期（与设计稿「发布于 2026-09-17 · 门店名」一致）
				return '发布于 ' + (this.notice.publishDate || '')
					+ (this.notice.shopName ? ' · ' + this.notice.shopName : '');
			}
		},
		onLoad(options) {
			this.id = options && options.id ? options.id : '';
			if (!this.id) {
				this.loading = false;
				return;
			}
			this.loadDetail();
		},
		methods: {
			loadDetail() {
				https.request('/rest/notice/selectById', {
					id: this.id
				}).then((result) => {
					this.loading = false;
					if (result && result.success && result.data) {
						this.notice = result.data;
						return;
					}
					this.notice = {
						id: ''
					};
				}).catch(() => {
					this.loading = false;
				});
			}
		}
	};
</script>

<style>
	page {
		background: #ffffff;
	}

	.nd {
		min-height: 100vh;
		padding: 24rpx;
		box-sizing: border-box;
		background: #ffffff;
	}

	.nd-title {
		font-size: 36rpx;
		font-weight: 700;
		color: #333333;
		line-height: 52rpx;
	}

	.nd-date {
		font-size: 22rpx;
		color: #999999;
		margin-top: 20rpx;
	}

	.nd-content {
		font-size: 26rpx;
		color: #666666;
		line-height: 40rpx;
		margin-top: 40rpx;
		display: block;
	}

	.nd-empty {
		text-align: center;
		font-size: 26rpx;
		color: #999999;
		padding-top: 120rpx;
	}
</style>
