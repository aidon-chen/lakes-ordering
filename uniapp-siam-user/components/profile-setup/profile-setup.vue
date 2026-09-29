<!--
	E-「完善个人资料」弹窗（2026-09-28 新增）
	触发：登录成功且本次为自动注册（新账号）→ 落到「我的」Tab 时弹一次，可跳过。
	微信能力：
	  · 头像 = button open-type="chooseAvatar"，回调 e.detail.avatarUrl 是**本地临时文件**，
	    必须经 /rest/member/uploadHeadImg 上传后才能长期保存（微信临时路径会失效）。
	  · 昵称 = input type="nickname"，聚焦后微信键盘上方自动出现「使用微信昵称」。
	    ⚠️ 不存在「一键读取微信昵称」的 API —— wx.getUserInfo/getUserProfile 自 2022 起
	    只返回灰色默认头像与「微信用户」，所以昵称只能由用户在键盘上点选填入。
	落库：头像 → POST /rest/member/uploadHeadImg（multipart，字段名 file，后端直接写 tb_member.head_img）
	      昵称/性别 → POST /rest/member/update（后端 selective 更新，一次请求可带两个字段）
-->
<template>
	<view class="ps-mask" @tap.stop="onMaskTap">
		<view class="ps-card" @tap.stop>
			<view class="ps-title">完善个人资料</view>
			<view class="ps-sub">头像与昵称将展示在点单、存酒与会员卡</view>

			<!-- 头像区：点击弹菜单（微信头像 / 拍照 / 相册） -->
			<view class="ps-avatar-wrap">
				<view class="ps-avatar" @tap="openAvatarSource">
					<image class="ps-avatar-img" :src="avatarSrc" mode="aspectFill"></image>
					<view class="ps-avatar-badge">更换</view>
				</view>
				<!-- 「使用微信头像」必须挂在真正触发的 button 上（微信要求用户手势落在 button 上），
				     故不用 actionSheet 里的「微信头像」，而单独给一个 open-type 按钮 -->
				<!-- #ifdef MP-WEIXIN -->
				<button class="ps-wx-btn" open-type="chooseAvatar" @chooseavatar="onChooseAvatar">使用微信头像</button>
				<!-- #endif -->
				<view class="ps-avatar-hint">也可拍照或从相册选择</view>
			</view>

			<!-- 昵称：type=nickname 时键盘上方出现「使用微信昵称」 -->
			<view class="ps-label">昵称</view>
			<input
				class="ps-input"
				type="nickname"
				placeholder="请输入昵称"
				placeholder-class="ps-input-ph"
				maxlength="20"
				:value="nickname"
				@input="onNicknameInput" />
			<view class="ps-hint">聚焦后可在键盘上方选择「使用微信昵称」</view>

			<!-- 性别：可选项，不强制 -->
			<view class="ps-label">性别（可选）</view>
			<view class="ps-sex-row">
				<view
					class="ps-sex-chip"
					:class="{ 'ps-sex-chip-on': sex === item.value }"
					v-for="(item, index) in sexOptions"
					:key="index"
					@tap="onPickSex(item.value)">{{ item.name }}</view>
			</view>

			<view class="ps-save theme-bg" :class="{ 'ps-save-off': !canSave }" @tap="submit">保存</view>
			<view class="ps-later" @tap="close">稍后再说</view>
		</view>
	</view>
</template>

<script>
	import https from '../../utils/http';
	import GlobalConfig from '../../utils/global-config';
	import toastService from '../../utils/toast.service';

	export default {
		name: 'profile-setup',
		props: {
			// 当前会员信息（/rest/member/getLoginMemberInfo 的 data）
			member: {
				type: Object,
				default: () => ({})
			}
		},
		data() {
			return {
				nickname: '',
				sex: '',
				// 待上传的头像本地临时路径（微信头像 / 拍照 / 相册，三者都先落到本地再统一上传）
				avatarTemp: '',
				uploading: false,
				saving: false,
				// 用户是否已手动改动过：为 true 时不再被 member 回填覆盖
				// （弹窗常在 getLoginMemberInfo 返回前渲染，接口回来会二次触发 watch）
				nameEdited: false,
				sexEdited: false,
				sexOptions: [{
						name: '男',
						value: 1
					},
					{
						name: '女',
						value: 2
					},
					{
						name: '保密',
						value: 0
					}
				]
			};
		},
		computed: {
			// 头像显示源：本地临时预览 > 已保存的相对路径（拼 OSS 前缀）> 默认灰剪影
			avatarSrc() {
				if (this.avatarTemp) {
					return this.avatarTemp;
				}
				const img = this.member && this.member.headImg;
				if (!img) {
					return '/static/assets/images/user-head.png';
				}
				// 兼容老数据：模板期遗留的是完整 URL，uploadHeadImg 之后存的是 OSS 相对路径
				return img.indexOf('http') === 0 ? img : GlobalConfig.ossUrl + img;
			},
			canSave() {
				return !this.saving && !this.uploading && (this.nickname || '').trim().length > 0;
			}
		},
		watch: {
			// 父组件拿到会员信息后回填表单（弹窗通常在 getLoginMemberInfo 之后渲染）
			member: {
				immediate: true,
				handler(value) {
					const m = value || {};
					if (!this.nameEdited) {
						this.nickname = m.username || '';
					}
					if (!this.sexEdited) {
						this.sex = m.sex === undefined || m.sex === null ? '' : m.sex;
					}
				}
			}
		},
		methods: {
			onNicknameInput(e) {
				this.nickname = e.detail.value;
				this.nameEdited = true;
			},

			onPickSex(value) {
				this.sex = value;
				this.sexEdited = true;
			},

			// 微信头像填写能力：返回的是临时文件路径，不能直接入库
			onChooseAvatar(e) {
				const url = e && e.detail && e.detail.avatarUrl;
				if (!url) {
					toastService.showToast('未获取到微信头像');
					return;
				}
				this.avatarTemp = url;
			},

			// 拍照 / 相册（非微信环境下的唯一头像来源）
			openAvatarSource() {
				uni.showActionSheet({
					itemList: ['拍照', '从手机相册选择'],
					success: (res) => {
						uni.chooseImage({
							count: 1,
							sizeType: ['compressed'],
							sourceType: [res.tapIndex === 0 ? 'camera' : 'album'],
							success: (result) => {
								this.avatarTemp = result.tempFilePaths[0];
							}
						});
					},
					fail: () => {}
				});
			},

			// 头像上传：multipart，字段名 file；成功即已写库，无需再调 update
			uploadAvatar(filePath) {
				return new Promise((fulfil, reject) => {
					this.uploading = true;
					https.uploadImage({
						url: GlobalConfig.baseUrl + '/rest/member/uploadHeadImg',
						filePath: filePath,
						success: (data) => {
							this.uploading = false;
							if (data && data.success) {
								fulfil(data);
								return;
							}
							reject(new Error((data && data.message) || '头像上传失败'));
						},
						fail: () => {
							this.uploading = false;
							reject(new Error('头像上传失败，请重试'));
						}
					});
				});
			},

			// 昵称 + 性别：一次 selective 更新；未修改的字段不带，避免把已有值改空
			updateProfile() {
				const payload = {};
				const nickname = (this.nickname || '').trim();
				const originName = (this.member && this.member.username) || '';
				if (nickname && nickname !== originName) {
					payload.username = nickname;
				}
				const originSex = this.member && this.member.sex;
				if (this.sex !== '' && this.sex !== originSex) {
					payload.sex = this.sex;
				}
				if (!Object.keys(payload).length) {
					return Promise.resolve(null);
				}
				return https.request('/rest/member/update', payload).then((result) => {
					if (!result || !result.success) {
						throw new Error((result && result.message) || '保存失败');
					}
					return result;
				});
			},

			submit() {
				if (this.saving || this.uploading) {
					return;
				}
				const nickname = (this.nickname || '').trim();
				if (!nickname) {
					toastService.showToast('请填写昵称');
					return;
				}
				if (nickname.length > 20) {
					toastService.showToast('昵称不能超过 20 个字');
					return;
				}
				this.saving = true;
				toastService.showLoading('正在保存...', true);
				// 头像与文本分开提交：头像走 multipart、文本走 JSON，顺序执行避免并发写同一行
				const headTask = this.avatarTemp ? this.uploadAvatar(this.avatarTemp) : Promise.resolve(null);
				headTask.then(() => this.updateProfile()).then(() => {
					toastService.hideLoading();
					this.saving = false;
					this.$emit('saved');
				}).catch((err) => {
					toastService.hideLoading();
					this.saving = false;
					toastService.showToast((err && err.message) || '保存失败，请重试');
				});
			},

			onMaskTap() {
				// 遮罩点击等同「稍后再说」：本弹窗允许跳过（2026-09-28 拍板）
				this.close();
			},

			close() {
				this.$emit('close');
			}
		}
	};
</script>

<style>
	.ps-mask {
		position: fixed;
		left: 0;
		right: 0;
		top: 0;
		bottom: 0;
		background: rgba(0, 0, 0, 0.45);
		display: flex;
		align-items: center;
		justify-content: center;
		z-index: 999;
	}

	.ps-card {
		width: 600rpx;
		background: #ffffff;
		border-radius: 24rpx;
		padding: 40rpx 36rpx 32rpx;
		box-sizing: border-box;
	}

	.ps-title {
		font-size: 34rpx;
		font-weight: 600;
		color: #222222;
		text-align: center;
	}

	.ps-sub {
		margin-top: 12rpx;
		font-size: 22rpx;
		color: #999999;
		text-align: center;
		line-height: 32rpx;
	}

	/* ── 头像区 ───────────────────────────── */
	.ps-avatar-wrap {
		margin-top: 36rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
	}

	.ps-avatar {
		position: relative;
		width: 160rpx;
		height: 160rpx;
	}

	.ps-avatar-img {
		width: 160rpx;
		height: 160rpx;
		border-radius: 50%;
		background: #f5f5f5;
	}

	.ps-avatar-badge {
		position: absolute;
		left: 50%;
		bottom: -8rpx;
		transform: translateX(-50%);
		padding: 4rpx 16rpx;
		border-radius: 20rpx;
		background: rgba(0, 0, 0, 0.55);
		color: #ffffff;
		font-size: 20rpx;
	}

	/* 微信头像按钮：描边胶囊，与主题一致 */
	.ps-wx-btn {
		margin-top: 24rpx;
		padding: 0 28rpx;
		height: 56rpx;
		line-height: 54rpx;
		font-size: 24rpx;
		border-radius: 28rpx;
		background: #ffffff;
		color: #5f7d22;
		border: 2rpx solid #5f7d22;
	}

	.ps-wx-btn::after {
		border: none;
	}

	.ps-avatar-hint {
		margin-top: 12rpx;
		font-size: 20rpx;
		color: #b0b0b0;
	}

	/* ── 表单 ─────────────────────────────── */
	.ps-label {
		margin-top: 32rpx;
		font-size: 24rpx;
		color: #888888;
	}

	.ps-input {
		margin-top: 12rpx;
		height: 88rpx;
		line-height: 88rpx;
		padding: 0 20rpx;
		box-sizing: border-box;
		border: 2rpx solid #ededed;
		border-radius: 12rpx;
		font-size: 28rpx;
		color: #333333;
	}

	.ps-input-ph {
		color: #c0c0c0;
	}

	.ps-hint {
		margin-top: 8rpx;
		font-size: 20rpx;
		color: #b0b0b0;
	}

	.ps-sex-row {
		margin-top: 16rpx;
		display: flex;
		flex-direction: row;
	}

	.ps-sex-chip {
		width: 140rpx;
		height: 56rpx;
		line-height: 54rpx;
		margin-right: 16rpx;
		text-align: center;
		font-size: 24rpx;
		border-radius: 28rpx;
		border: 2rpx solid #ededed;
		color: #888888;
		background: #ffffff;
	}

	.ps-sex-chip-on {
		background: #eef3e4;
		border-color: #5f7d22;
		color: #5f7d22;
	}

	/* ── 操作 ─────────────────────────────── */
	.ps-save {
		margin-top: 40rpx;
		height: 88rpx;
		line-height: 88rpx;
		text-align: center;
		font-size: 30rpx;
		border-radius: 12rpx;
	}

	.ps-save-off {
		opacity: 0.5;
	}

	.ps-later {
		margin-top: 20rpx;
		text-align: center;
		font-size: 26rpx;
		color: #999999;
	}
</style>
