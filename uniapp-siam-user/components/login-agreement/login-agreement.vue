<template>
	<!--
		登录协议勾选行（公共组件）
		设计稿规格：E-07b 节点 2:402 / E-08b 节点 2:427
		  · 容器 249×16，水平排列，元素间距 6px(=12rpx)
		  · 勾选框 16×16 圆形（=32rpx），选中态填主色 + 白色对勾 6.8×4.5（=13.6×9rpx）
		  · 文案 11px(=22rpx)：已阅读并同意（#808080）《用户协议》（主色）和（#808080）《隐私政策》（主色）
		配色遵循全站主色口径：设计稿蓝 #004CA0 → 主题绿 #5f7d22（走 .theme-* 全局类，不硬编码）

		用法：
		  <login-agreement v-model="agreed" />
		  <login-agreement :modelValue="agreed" @update:modelValue="agreed = $event" />
		  <login-agreement :modelValue="agreed" @change="agreed = $event" />
	-->
	<view class="login-agreement">
		<view class="agr-hit" @tap="toggle">
			<view class="agr-check" :class="modelValue ? 'agr-check-on theme-bg' : 'agr-check-off'">
				<view class="agr-tick" v-if="modelValue"></view>
			</view>
		</view>
		<view class="agr-text">
			<text class="agr-normal" @tap="toggle">已阅读并同意</text>
			<text class="agr-link theme-color" @tap.stop="openDoc('user')">《用户协议》</text>
			<text class="agr-normal" @tap="toggle">和</text>
			<text class="agr-link theme-color" @tap.stop="openDoc('privacy')">《隐私政策》</text>
		</view>
	</view>
</template>

<script>
	export default {
		name: 'login-agreement',
		props: {
			// 默认勾选（设计稿为已勾选态）
			modelValue: {
				type: Boolean,
				default: true
			},
			// 未勾选时点登录要给的提示文案
			tip: {
				type: String,
				default: '请先阅读并同意《用户协议》和《隐私政策》'
			}
		},
		emits: ['update:modelValue', 'change'],
		methods: {
			toggle() {
				const next = !this.modelValue;
				this.$emit('update:modelValue', next);
				this.$emit('change', next);
			},
			openDoc(type) {
				const url = type === 'privacy'
					? '/pages/internal/agreement/privacy'
					: '/pages/internal/agreement/user';
				uni.navigateTo({
					url,
					fail() {
						uni.showToast({ title: '页面打开失败', icon: 'none' });
					}
				});
			}
		}
	};
</script>

<style>
	.login-agreement {
		display: flex;
		flex-direction: row;
		align-items: center;
		justify-content: center;
	}

	/* 加大点击热区，但**不能位移视觉盒**：设计稿里勾选框顶端 = 协议行顶端
	   （5:760 / 5:788 行高 16px 由勾选框决定），若用上下 padding 撑热区，
	   圆圈会被下推 6px，整行比设计稿低。故用负外边距把 padding 抵消掉。 */
	.agr-hit {
		margin: -12rpx 0;
		padding: 12rpx 12rpx 12rpx 0;
	}

	.agr-check {
		width: 32rpx;
		height: 32rpx;
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		box-sizing: border-box;
	}

	.agr-check-off {
		background: transparent;
		border: 2rpx solid #c3c3c3;
	}

	.agr-check-on {
		border: none;
	}

	/* 纯 CSS 对勾：设计稿实测 8.40×6.10px（两臂 6.51 / 3.25px，描边 1.5px）
	   本写法 Wt=width+border⇒长臂、Ht=height+border⇒短臂，故 6pt+1.5pt / 2.5pt+1.5pt */
	.agr-tick {
		width: 12rpx;
		height: 5rpx;
		border-left: 3rpx solid #ffffff;
		border-bottom: 3rpx solid #ffffff;
		transform: rotate(-45deg);
	}

	.agr-text {
		display: flex;
		flex-direction: row;
		align-items: center;
	}

	/* 设计稿协议行 itemSpacing = 6px，文案之间必须留 12rpx 间距（整行才是 249px 宽） */
	.agr-normal,
	.agr-link {
		font-size: 22rpx;
		line-height: 32rpx;
		margin-left: 12rpx;
	}

	.agr-normal:first-child {
		margin-left: 0;
	}

	.agr-normal {
		color: #808080;
	}

	.agr-link {
		/* 颜色由全局 theme-color 提供 */
	}
</style>
