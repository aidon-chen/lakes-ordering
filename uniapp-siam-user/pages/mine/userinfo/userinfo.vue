<template>
	<view class="c02">
		<!-- 行序对齐画板 21:191：头像 / 用户名 / 绑定手机 / 性别 / 邮箱 / 绑定微信 / 认证身份 -->
		<!-- 头像 / 用户名：2026-09-25 放开编辑（原先两行只读，用户报「没法编辑我的头像和用户名」）。
		     头像走 /rest/member/uploadHeadImg（multipart，字段名 file）；用户名走 /rest/member/update。 -->
		<!-- 头像行（2026-09-28）：点头像 = 使用微信头像（button open-type="chooseAvatar"，
		     微信要求用户手势落在 button 上）；拍照/相册保留为右侧文字入口。
		     微信返回的 avatarUrl 是本地临时文件，统一走 uploadHeadImg 上传后才入库。 -->
		<view class="userinfo-item">
			<text class="title-text">头像</text>
			<view class="input-image">
				<!-- #ifdef MP-WEIXIN -->
				<button class="ua-avatar-btn" open-type="chooseAvatar" @chooseavatar="onChooseAvatar">
					<image class="userinfo-avatar" :src="headImgUrl" mode="cover"></image>
				</button>
				<!-- #endif -->
				<!-- #ifndef MP-WEIXIN -->
				<image class="userinfo-avatar" :src="headImgUrl" mode="cover" @tap="chooseHeadImg"></image>
				<!-- #endif -->
				<text class="ua-album-link" @tap="chooseHeadImg">拍照/相册</text>
				<van-icon name="arrow" />
			</view>
		</view>
		<view class="userinfo-item" @tap="openUsername">
			<text class="title-text">用户名</text>
			<view class="input-image">
				<text class="userinfo-text">{{ data.username }}</text>
				<van-icon name="arrow" />
			</view>
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
		<!-- 绑定微信：已绑定 → 原解绑流程；未绑定 → 2026-09-27 新增绑定流程
		     （原先未绑定时点击是空操作，用户永远无法变成「已绑定」） -->
		<view class="userinfo-item" @tap="tapBindWx"
			:data-mobile="data.mobile" :data-isBindWx="data.isBindWx">
			<text class="title-text">绑定微信</text>
			<view class="bind-wx">
				<text class="userinfo-text">{{ data.isBindWx ? '已绑定' : '未绑定' }}</text>
				<van-icon name="arrow" />
			</view>
		</view>
		<!-- 认证身份行（画板 21:191 / 节点 298:158~167）：点击进入 C-17 身份认证中心。
		     ⚠️ 画板此行只有「标签 + 箭头」，本页右侧额外给出认证状态。
		     状态口径改为读 /rest/member/identityAuth/detail 的两阶段真实状态 ——
		     原先用 realName 判断是错的：realName 只是提现前置校验用的姓名（/rest/member/update），
		     与本功能「实名认证 + 身份认证」两阶段无关。 -->
		<view class="userinfo-item" @tap="goIdentityAuth">
			<text class="title-text">认证身份</text>
			<view class="input-image">
				<text :class="authStatusClass">{{ authStatusText }}</text>
				<van-icon name="arrow" />
			</view>
		</view>

		<!-- 发票申请：由「我的」页「常用服务」移入本页（2026-09-23 用户要求）。
		     目标页未变，仍是 pages/mine/invoice/apply/apply -->
		<view class="userinfo-item" @tap="goInvoiceApply">
			<text class="title-text">发票申请</text>
			<view class="input-image">
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

		<van-action-sheet :show="dialogShowUsername" @close="close" @cancel="close" title="修改用户名">
			<view class="content_box">
				<scroll-view scroll-y style="height: 52vh">
					<view class="content_box">
						<!-- type="nickname"（2026-09-28）：聚焦后微信键盘上方出现「使用微信昵称」，
						     点了即自动填入。这是目前唯一合法的微信昵称获取途径
						     （getUserInfo/getUserProfile 只返回「微信用户」）。 -->
						<input class="userinfo-text input-email" type="nickname" placeholder="请输入用户名（20 字以内）"
							:value="usernameInput" maxlength="20" @input="bindInputUsername" />
						<view class="ua-nickname-hint">聚焦输入框，可在键盘上方选择「使用微信昵称」</view>
					</view>
				</scroll-view>
				<view class="flex_between">
					<view class="close-botton good-choice-btn" @tap="close">取消</view>
					<view class="good-choice-btn theme-bg" @tap="tapdialogUsernameButton">确定修改</view>
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
	import GlobalConfig from '../../../utils/global-config';

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
				dialogShowUsername: false,
				usernameInput: '',
				headUploading: false,
				authStatusText: '未认证',
				authPassed: false,
				email: '',
				realName: '',
				sex: '',
				modeList: []
			};
		},
		computed: {
			// 头像地址兼容两种存量格式：老数据存的是完整 URL（如 https://picture.moguit.cn/...，模板期遗留），
			// 而 /rest/member/uploadHeadImg → ossUtils.uploadImage 返回的是 OSS 相对路径 → 需拼 ossUrl。
			headImgUrl() {
				const img = this.data.headImg;
				if (!img) {
					return '/static/assets/images/default-avatar.jpg';
				}
				return img.indexOf('http') === 0 ? img : GlobalConfig.ossUrl + img;
			},
			// 认证状态色：已认证=绿，未认证=橙，审核中/已驳回=警示橙
			authStatusClass() {
				if (this.authPassed) {
					return 'c02-verified';
				}
				return this.authStatusText === '未认证' ? 'c02-unverified' : 'c02-auditing';
			}
		},
		onLoad: function(options) {
			app = getApp();
		},
		onReady: function() {},
		onShow: function() {
			this.getUserInfo();
			this.loadAuthStatus();
		},
		onHide: function() {},
		onUnload: function() {},
		onPullDownRefresh: function() {},
		onReachBottom: function() {},
		onShareAppMessage: function() {},
		methods: {
			// C-17 身份认证状态：stage1Status/stage2Status 0=未提交 1=待审核 2=通过 3=驳回
			loadAuthStatus() {
				https.request('/rest/member/identityAuth/detail', {}).then((result) => {
					if (result && result.success && result.data) {
						const d = result.data;
						const s1 = Number(d.stage1Status) || 0;
						const s2 = Number(d.stage2Status) || 0;
						this.authPassed = s2 === 2;
						if (this.authPassed) {
							this.authStatusText = '已认证';
						} else if (s1 === 3 || s2 === 3) {
							this.authStatusText = '已驳回';
						} else if (s1 === 1 || s2 === 1) {
							this.authStatusText = '审核中';
						} else {
							this.authStatusText = '未认证';
						}
					}
				});
			},

			goIdentityAuth() {
				// 登录守卫（09-24 未登录拦截修复）：认证中心涉及证件收集，未登录不进入
				authService.checkIsLogin().then((result) => {
					if (result) {
						uni.navigateTo({
							url: '/pages/mine/identityAuth/index'
						});
						return;
					}
					app.globalData.checkIsAuth('scope.userInfo');
				});
			},

			// 发票申请（由「我的」页常用服务移入本页）
			goInvoiceApply() {
				uni.navigateTo({
					url: '/pages/mine/invoice/apply/apply'
				});
			},

			// ===== 头像 =====
			// 微信头像填写能力：e.detail.avatarUrl 是本地临时文件，直接入库会失效 → 走统一上传
			onChooseAvatar(e) {
				const url = e && e.detail && e.detail.avatarUrl;
				if (!url) {
					toastService.showToast('未获取到微信头像');
					return;
				}
				this.uploadHeadImg(url);
			},

			chooseHeadImg() {
				if (this.headUploading) {
					return;
				}
				uni.showActionSheet({
					itemList: ['拍照', '从手机相册选择'],
					success: (res) => {
						uni.chooseImage({
							count: 1,
							sizeType: ['original', 'compressed'],
							sourceType: [res.tapIndex === 0 ? 'camera' : 'album'],
							success: (result) => {
								this.uploadHeadImg(result.tempFilePaths[0]);
							}
						});
					}
				});
			},

			// 头像上传走 multipart（字段名 file），与 /rest/member/uploadSingleImage 同款；
			// 接口成功即已写库，故成功后重新拉一次会员信息回显
			uploadHeadImg(filePath) {
				this.headUploading = true;
				toastService.showLoading('正在上传...', true);
				const url = GlobalConfig.baseUrl + '/rest/member/uploadHeadImg';
				https.uploadImage({
					url: url,
					filePath: filePath,
					success: (data) => {
						toastService.hideLoading();
						this.headUploading = false;
						if (data && data.success) {
							toastService.showToast('头像已更新');
							this.getUserInfo();
						} else {
							toastService.showToast((data && data.message) || '上传失败');
						}
					},
					fail: () => {
						toastService.hideLoading();
						this.headUploading = false;
						toastService.showToast('上传失败，请重试');
					}
				});
			},

			// ===== 用户名 =====
			openUsername() {
				this.dialogShowUsername = true;
				this.usernameInput = this.data.username;
			},

			bindInputUsername(e) {
				this.usernameInput = e.detail.value;
			},

			tapdialogUsernameButton() {
				const username = (this.usernameInput || '').trim();
				if (!username) {
					toastService.showToast('请输入用户名');
					return;
				}
				if (username === this.data.username) {
					toastService.showToast('请输入不同的用户名');
					return;
				}
				if (username.length > 20) {
					toastService.showToast('用户名不能超过 20 个字');
					return;
				}
				https.request('/rest/member/update', {
					username: username
				}).then((result) => {
					if (result && result.success) {
						toastService.showToast(result.message || '用户名已更新');
						this.data.username = username;
						this.dialogShowUsername = false;
					} else {
						toastService.showToast((result && result.message) || '修改失败');
					}
				});
			},

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
			// 绑定微信行点击分流：已绑定 → 原解绑流程；未绑定 → 新绑定流程
			tapBindWx(e) {
				if (this.data.isBindWx) {
					this.bindWx(e);
				} else {
					this.bindWxNow();
				}
			},

			// 绑定微信：uni.login 拿 code → 后端 jscode2session 换 openid → 绑到当前登录会员。
			// ⚠ 不复用 /weChat/silence/login 做绑定：那接口按 openid 找不到会员时会**自动注册新号并切登录态**，语义是登录不是绑定。
			bindWxNow() {
				toastService.showModal(null, '确定绑定当前微信账号吗？', () => {
					uni.login({
						provider: 'weixin',
						success: (loginRes) => {
							if (!loginRes.code) {
								toastService.showToast('获取微信凭证失败，请重试');
								return;
							}
							https.request('/rest/member/bindWx', {
								code: loginRes.code
							}).then((result) => {
								if (result && result.success) {
									toastService.showToast('绑定成功');
									this.data.isBindWx = true;
									this.getUserInfo();
								} else {
									toastService.showToast((result && result.message) || '绑定失败');
								}
							});
						},
						fail: () => {
							toastService.showToast('获取微信凭证失败，请重试');
						}
					});
				});
			},

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
				this.dialogShowUsername = false;
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

	/* 微信「头像填写能力」必须挂在 button 上：抹掉按钮默认样式，只留圆形头像本身 */
	.ua-avatar-btn {
		padding: 0;
		margin: 0;
		background: transparent;
		line-height: 1;
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.ua-avatar-btn::after {
		border: none;
	}

	/* 拍照/相册次级入口：点头像本体已经是「微信头像」，这里保留原有两条路径 */
	.ua-album-link {
		margin-right: 12rpx;
		font-size: 24rpx;
		color: #5f7d22;
	}

	.ua-nickname-hint {
		margin-top: 12rpx;
		font-size: 22rpx;
		color: #999999;
		text-align: center;
	}

	.userinfo-avatar {
		width: 108rpx;
		height: 108rpx;
		border-radius: 50%;
		/* 头像行现为可点行：头像与右侧箭头之间留出间距 */
		margin-right: 12rpx;
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

	/* 审核中 / 已驳回：警示橙（与未认证区分靠文案） */
	.c02-auditing {
		font-size: 28rpx;
		font-weight: bold;
		color: #E25A2B;
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
