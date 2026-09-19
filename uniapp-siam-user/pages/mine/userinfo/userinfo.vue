<template>
	<view class="c02">
		<!-- 行序对齐画板 21:191：头像 / 用户名 / 绑定手机 / 性别 / 邮箱 / 绑定微信 / 认证身份 -->
		<view class="userinfo-item">
			<text class="title-text">头像</text>
			<image class="userinfo-avatar"
				:src="data.headImg ? data.headImg : '/static/assets/images/user-head.png'" mode="cover"></image>
		</view>
		<view class="userinfo-item">
			<text class="title-text">用户名</text>
			<text class="userinfo-text">{{ data.username }}</text>
		</view>
		<view class="userinfo-item">
			<text class="title-text">绑定手机</text>
			<text class="userinfo-text">{{ data.mobile }}</text>
		</view>
		<view class="userinfo-item" @tap="openConfirm">
			<text class="title-text">性别</text>
			<view class="input-image">
				<text class="userinfo-text">{{ data.sex == 1 ? '男' : data.sex == 2 ? '女' : '保密' }}</text>
				<van-icon name="arrow" />
			</view>
		</view>
		<view class="userinfo-item" @tap="openEmail">
			<text class="title-text">邮箱</text>
			<view class="input-image">
				<text
					:class="data.email ? 'userinfo-text' : 'placeholder-text'">{{ data.email ? data.email : '请输入邮箱地址' }}</text>
				<van-icon name="arrow" />
			</view>
		</view>
		<view class="userinfo-item" @tap="parseEventDynamicCode($event, data.isBindWx ? 'bindWx' : '')"
			:data-mobile="data.mobile" :data-isBindWx="data.isBindWx">
			<text class="title-text">绑定微信</text>
			<view class="bind-wx">
				<text class="userinfo-text">{{ data.isBindWx ? '已绑定' : '未绑定' }}</text>
				<van-icon name="arrow" />
			</view>
		</view>
		<!-- 认证身份行：画板 21:191 于本日新增（节点 298:158~167），取代原来的「真实姓名」行。
		     ⚠️ 画板此行只有「标签 + 箭头」，本页右侧额外给出 已认证/未认证（与 C-03 画板同概念行的写法一致）。
		     点击进入实名认证弹窗 —— 保留 realName 写入口（/rest/member/update 与提现前置校验仍在用）。 -->
		<view class="userinfo-item" @tap="openRealName">
			<text class="title-text">认证身份</text>
			<view class="input-image">
				<text
					:class="data.realName ? 'c02-verified' : 'c02-unverified'">{{ data.realName ? '已认证' : '未认证' }}</text>
				<van-icon name="arrow" />
			</view>
		</view>

		<view class="out-login-btn theme-bg" hover-class="hover-class-public" @tap="bindOutLogTap">退出登录</view>

		<van-action-sheet :show="dialogShow" @close="close" @cancel="close" title="选择性别">
			<view class="content_box">
				<scroll-view scroll-y style="height: 52vh">
					<radio-group @change="radioChange" class="sex-radio-group">
						<label :data-radioIndex="index" class="radio-group-label" v-for="(item, index) in modeList"
							:key="index">
							{{ item.name }}
							<radio :value="item.value" class="radio-group-label-radio" :checked="item.checked" />
						</label>
					</radio-group>
				</scroll-view>
				<view class="flex_between">
					<view class="close-botton good-choice-btn" @tap="close">取消</view>
					<view class="good-choice-btn theme-bg" @tap="tapdialogButton">确定修改</view>
				</view>
			</view>
		</van-action-sheet>

		<van-action-sheet :show="dialogShowEmail" @close="close" @cancel="close" title="编辑邮箱">
			<view class="content_box">
				<scroll-view scroll-y style="height: 52vh">
					<view class="content_box">
						<input class="userinfo-text input-email" placeholder="请输入邮箱地址" :value="data.email"
							@input="bindInputEmail" />
					</view>
				</scroll-view>
				<view class="flex_between">
					<view class="close-botton good-choice-btn" @tap="close">取消</view>
					<view class="good-choice-btn theme-bg" @tap="tapdialogEmailButton">确定修改</view>
				</view>
			</view>
		</van-action-sheet>

		<van-action-sheet :show="dialogShowRealName" @close="close" @cancel="close" title="身份认证">
			<view class="content_box">
				<scroll-view scroll-y style="height: 52vh">
					<view class="title-tip">请于身份证上保持一致，信息有误影响奖励提现</view>
					<input class="userinfo-text input-email" placeholder="请输入真实姓名" :value="data.realName"
						@input="bindInputRealName" />
				</scroll-view>
				<view class="flex_between">
					<view class="close-botton good-choice-btn" @tap="close">取消</view>
					<view class="theme-bg good-choice-btn" @tap="tapdialogRealNameButton">确定修改</view>
				</view>
			</view>
		</van-action-sheet>
	</view>
</template>

<script>
	import https from '../../../utils/http';
	import authService from '../../../utils/auth';
	import toastService from '../../../utils/toast.service';
	import utilHelper from '../../../utils/util';

	let app = null;

	export default {
		data() {
			return {
				data: {
					isBindWx: false,
					sex: '',
					email: '',
					realName: '',
					headImg: false,
					username: '',
					mobile: ''
				},
				dialogShow: false,
				dialogShowEmail: false,
				dialogShowRealName: false,
				email: '',
				realName: '',
				sex: '',
				modeList: []
			};
		},
		onLoad: function(options) {
			app = getApp();
		},
		onReady: function() {},
		onShow: function() {
			this.getUserInfo();
		},
		onHide: function() {},
		onUnload: function() {},
		onPullDownRefresh: function() {},
		onReachBottom: function() {},
		onShareAppMessage: function() {},
		methods: {
			getUserInfo() {
				https.request('/rest/member/getLoginMemberInfo', {}).then((result) => {
					if (result && result.success && result.data) {
						const member = result.data;
						member.email = member.email ? member.email : '';
						member.realName = member.realName ? member.realName : '';
						this.data = member;
					}
				});
			},

			// 仅「已绑定」时才会被 parseEventDynamicCode 派发到这里（解绑）
			bindWx(e) {
				toastService.showModal(null, '确定解除与微信账号的关联关系吗？', () => {
					https.request('/rest/member/removeBindingWx', {
						mobile: e.currentTarget.dataset.mobile,
						isbindwx: e.currentTarget.dataset.isbindwx
					}).then((result) => {
						if (result && result.success) {
							toastService.showToast(result.message);
							this.data.isBindWx = false;
						}
					});
				});
			},

			openEmail() {
				this.dialogShowEmail = true;
				this.email = this.data.email;
			},

			// 原实现这里写的是 this.data.email（拷贝粘贴笔误）→ 打开弹窗时输入框会带出邮箱
			openRealName() {
				this.dialogShowRealName = true;
				this.realName = this.data.realName;
			},

			openConfirm() {
				this.dialogShow = true;
				this.modeList = [{
						name: '男',
						value: 1,
						checked: this.data.sex == 1
					},
					{
						name: '女',
						value: 2,
						checked: this.data.sex == 2
					}
				];
			},

			radioChange(e) {
				this.sex = e.detail.value;
			},

			bindOutLogTap() {
				toastService.showModal(null, '确定退出登录吗？', () => {
					https.request('/rest/member/logout', {}).then((result) => {
						if (result && result.success) {
							authService.deleteToken();
							authService.deleteOpenId();
							uni.navigateBack(1);
						}
					});
				});
			},

			tapdialogButton(e) {
				https.request('/rest/member/update', {
					sex: this.sex
				}).then((result) => {
					if (result && result.success) {
						toastService.showToast(result.message);
						this.data.sex = this.sex;
						this.dialogShow = false;
					}
				});
			},

			tapdialogEmailButton(e) {
				const email = this.email;
				if (email == this.data.email) {
					toastService.showToast('请输入不同的邮箱号码');
					return;
				}
				// 原实现写的是 utils.verifyEmail —— utils 未导入，点「确定修改」必抛 ReferenceError
				if (!utilHelper.verifyEmail(email)) {
					toastService.showToast('邮箱号码格式错误');
					return;
				}
				https.request('/rest/member/update', {
					email: email
				}).then((result) => {
					if (result && result.success) {
						toastService.showToast(result.message);
						this.data.email = email;
						this.dialogShowEmail = false;
					}
				});
			},

			tapdialogRealNameButton(e) {
				const realName = this.realName;
				if (!realName) {
					toastService.showToast('请输入真实姓名');
					return;
				}
				https.request('/rest/member/update', {
					realName: realName
				}).then((result) => {
					if (result && result.success) {
						toastService.showToast(result.message);
						this.data.realName = realName;
						this.dialogShowRealName = false;
					}
				});
			},

			bindInputEmail(e) {
				this.email = e.detail.value;
			},

			bindInputRealName(e) {
				this.realName = e.detail.value;
			},

			close() {
				this.dialogShow = false;
				this.dialogShowRealName = false;
				this.dialogShowEmail = false;
			}
		}
	};
</script>

<style>
	page {
		background: #ffffff;
	}

	.c02 {
		min-height: 100vh;
		background: #ffffff;
	}

	/* 行高 54pt = 108rpx（画板 21:191 相邻分割线间距实测 54pt） */
	.userinfo-item {
		line-height: 68rpx;
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 20rpx 30rpx;
		border-bottom: 1rpx solid #f5f5f5;
	}

	.userinfo-avatar {
		width: 108rpx;
		height: 108rpx;
		border-radius: 50%;
		box-shadow: -2px 0px 5px 0.5px rgba(0, 0, 0, 0), 0px -2px 5px 1px rgba(0, 0, 0, 0.1), 2px 0px 5px 1px rgba(0, 0, 0, 0), 0px 2px 5px 1px rgba(0, 0, 0, 0.1);
	}

	.title-text {
		font-size: 28rpx;
		color: #999999;
	}

	.userinfo-text {
		font-size: 28rpx;
		text-align: end;
		font-weight: bold;
	}

	.input-email {
		width: 100%;
		text-align: center;
		border: 1rpx solid #8e8e8e;
		padding: 10rpx 0;
		border-radius: 15rpx;
		margin: 20rpx 0;
		color: black;
	}

	.input-image {
		display: flex;
		align-items: center;
		width: 80%;
		justify-content: flex-end;
	}

	.out-login-btn {
		margin: 100rpx 30rpx 0 30rpx;
		text-align: center;
		line-height: 100rpx;
		color: white;
		border-radius: 10rpx;
		font-size: 30rpx;
	}

	.bind-wx {
		display: flex;
		align-items: center;
		justify-content: flex-end;
	}

	/* 认证身份状态：已认证=成功绿、未认证=警示橙（沿用 C-01/C-03 的既有令牌） */
	.c02-verified {
		font-size: 28rpx;
		font-weight: bold;
		color: #43A047;
	}

	.c02-unverified {
		font-size: 28rpx;
		font-weight: bold;
		color: #E6A23C;
	}

	.radio-group-label {
		display: flex;
		align-items: center;
		justify-content: space-between;
		height: 88rpx;
		padding: 0 20rpx;
		font-weight: bold;
	}

	.placeholder-email {
		text-align: end;
	}

	.placeholder-text {
		text-align: end;
		color: #808090;
		font-size: 30rpx;
	}

	.title-tip {
		font-size: 22rpx;
		color: red;
	}

	.close-botton {
		background-color: #ededed;
	}

	.good-choice-btn {
		width: 100%;
		text-align: center;
		padding: 20rpx 0;
		border-radius: 15rpx;
		font-size: 28rpx;
		font-weight: bold;
		margin: 20rpx;
	}
</style>
