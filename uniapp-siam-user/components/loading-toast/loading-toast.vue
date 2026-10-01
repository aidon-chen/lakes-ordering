<template>
	<view v-if="loadingState.visible" class="lt-wrap" :class="{ 'lt-mask': loadingState.mask }">
		<view class="lt-card">
			<view class="lt-notes">
				<view class="lt-note lt-n1"></view>
				<view class="lt-note lt-n2"></view>
				<view class="lt-note lt-n3"></view>
			</view>
			<text v-if="loadingState.text" class="lt-text">{{ loadingState.text }}</text>
		</view>
	</view>
</template>

<script>
	import { loadingState } from '../../utils/loading.js';

	export default {
		data() {
			return {
				loadingState
			};
		}
	};
</script>

<style>
	/* 容器默认不拦截点击（pointer-events:none），mask=true 时才阻断 —— 与原生 uni.showLoading 的 mask 语义一致 */
	.lt-wrap {
		position: fixed;
		left: 0;
		top: 0;
		right: 0;
		bottom: 0;
		display: flex;
		align-items: center;
		justify-content: center;
		z-index: 9999;
		pointer-events: none;
	}

	.lt-wrap.lt-mask {
		pointer-events: auto;
	}

	.lt-card {
		background: #ffffff;
		border: 1rpx solid rgba(0, 0, 0, 0.06);
		box-shadow: 0 4rpx 24rpx rgba(0, 0, 0, 0.12);
		border-radius: 16rpx;
		padding: 30rpx 44rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
	}

	.lt-notes {
		display: flex;
		align-items: flex-end;
		gap: 14rpx;
		height: 76rpx;
	}

	/* 音符：小程序 WXML 不支持 <svg>，故用 base64 走 background-image（fill=#5f7d22 已编码在内） */
	.lt-note {
		background-image: url("data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0iIzVmN2QyMiI+PHBhdGggZD0iTTEyIDN2MTAuNTVjLS41OS0uMzQtMS4yNy0uNTUtMi0uNTUtMi4yMSAwLTQgMS43OS00IDRzMS43OSA0IDQgNCA0LTEuNzkgNC00VjdoNFYzaC02eiIvPjwvc3ZnPg==");
		background-repeat: no-repeat;
		background-position: center bottom;
		background-size: 100% 100%;
	}

	/* 三个音符错拍跳动，大小差模拟音高起伏 */
	.lt-n1 {
		width: 34rpx;
		height: 34rpx;
		animation: lt-bounce 1.2s infinite ease-in-out;
	}

	.lt-n2 {
		width: 28rpx;
		height: 28rpx;
		animation: lt-bounce 1.2s infinite ease-in-out 0.2s;
	}

	.lt-n3 {
		width: 38rpx;
		height: 38rpx;
		animation: lt-bounce 1.2s infinite ease-in-out 0.4s;
	}

	@keyframes lt-bounce {
		0%, 80%, 100% { transform: translateY(0); opacity: 0.45; }
		40% { transform: translateY(-20rpx); opacity: 1; }
	}

	.lt-text {
		margin-top: 12rpx;
		font-size: 26rpx;
		color: #5f7d22;
	}
</style>
