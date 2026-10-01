<template>
	<el-row class="container">
		<el-col :span="24" class="header">
			<el-col :span="10" class="logo" :class="collapsed ? 'logo-collapse-width' : 'logo-width'">
				{{ collapsed ? '' : sysName }}
			</el-col>
			<el-col :span="10">
				<div>
					<span class="tools">
						<i class="el-icon-menu" @click.prevent="collapse"></i>
					</span>
				</div>
			</el-col>
			<el-col :span="4" class="userinfo">
				<el-dropdown trigger="hover">
					<span class="el-dropdown-link userinfo-inner">
						<img :src="this.sysUserAvatar" />
						{{ sysUserName }}
					</span>
					<el-dropdown-menu slot="dropdown">
						<!-- <el-dropdown-item>我的消息</el-dropdown-item>
						-->
						<el-dropdown-item @click.native="showDialog">修改密码</el-dropdown-item>
						<el-dropdown-item divided @click.native="logout">退出登录</el-dropdown-item>
					</el-dropdown-menu>
				</el-dropdown>
			</el-col>
			<el-col :span="4" class="userinfo">
				<el-button type="primary" round @click="toCashier" style="margin-right:20px;">收银台点餐</el-button>
			</el-col>
		</el-col>
		<el-col :span="24" class="main">
			<aside :class="collapsed ? 'menu-collapsed' : 'menu-expanded'">
				<!--导航菜单-->
				<el-menu :default-active="$route.path" @open="handleopen" @close="handleclose" @select="handleselect"
					unique-opened router v-if="!collapsed">
					<template v-for="(item, index) in routerArr" v-if="!item.hidden">
						<el-submenu :key="index" :index="index + ''" v-if="!item.leaf">
							<template slot="title"><i :class="item.iconCls"></i>{{ item.name }}</template>
							<el-menu-item v-for="child in item.children" :index="child.path" :key="child.path"
								v-if="!child.hidden">{{ child.name
								}}</el-menu-item>
						</el-submenu>
						<el-menu-item :key="index" v-if="item.leaf && item.children.length > 0"
							:index="item.children[0].path"><i :class="item.iconCls"></i>{{ item.children[0].name
							}}</el-menu-item>
					</template>
				</el-menu>
				<!--导航菜单-折叠后-->
				<ul class="el-menu collapsed" v-else ref="menuCollapsed">
					<li v-for="(item, index) in routerArr" :key="index" v-if="!item.hidden" class="el-submenu item">
						<template v-if="!item.leaf">
							<div class="el-submenu__title" style="padding-left: 20px;"
								@mouseover="showMenu(index, true)" @mouseout="showMenu(index, false)"><i
									:class="item.iconCls"></i></div>
							<ul class="el-menu submenu" :class="'submenu-hook-' + index"
								@mouseover="showMenu(index, true)" @mouseout="showMenu(index, false)">
								<li v-for="child in item.children" v-if="!child.hidden" :key="child.path"
									class="el-menu-item" style="padding-left: 40px;"
									:class="$route.path == child.path ? 'is-active' : ''"
									@click="$router.push(child.path)">
									{{ child.name }}</li>
							</ul>
						</template>
						<template v-else>
					<li class="el-submenu">
						<div class="el-submenu__title el-menu-item"
							style="padding-left: 20px;height: 56px;line-height: 56px;padding: 0 20px;"
							:class="$route.path == item.children[0].path ? 'is-active' : ''"
							@click="$router.push(item.children[0].path)"><i :class="item.iconCls"></i></div>
					</li>
</template>
</li>
</ul>
</aside>
<section class="content-container">
	<div class="grid-content bg-purple-light">
		<el-col :span="24" class="breadcrumb-container">
			<strong class="title">{{ $route.name }}</strong>
			<el-breadcrumb separator="/" class="breadcrumb-inner">
				<el-breadcrumb-item v-for="item in $route.matched" :key="item.path">
					{{ item.name }}
				</el-breadcrumb-item>
			</el-breadcrumb>
		</el-col>
		<el-col :span="24" class="content-wrapper">
			<transition name="fade" mode="out-in">
				<router-view></router-view>
			</transition>
		</el-col>
	</div>
</section>
</el-col>
<el-dialog title="修改密码" class="changePassword" :center="true" :visible.sync="changePasswordForm"
	@close="closeDialog('passwordForm')" :close-on-click-modal="false">
	<el-form :model="passwordForm" class="changePassform" label-width="150px" :rules="editFormRules" ref="passwordForm">
		<el-form-item label="旧密码：" prop="oldPassword">
			<el-input type="password" v-model="passwordForm.oldPassword" autocomplete="off"
				placeholder="密码只包含数字、字母组成。"></el-input>
		</el-form-item>
		<el-form-item label="新密码：" prop="newPassword">
			<el-input type="password" v-model="passwordForm.newPassword" autocomplete="off"
				placeholder="密码只包含数字、字母组成。"></el-input>
		</el-form-item>
	</el-form>
	<div slot="footer" class="dialog-footer">
		<el-button @click.native="changePasswordForm = false">取消</el-button>
		<el-button type="primary" @click.native="passwordSubmit" :loading="logining">提交</el-button>
	</div>
</el-dialog>
<!-- <el-dialog title="收银台点餐" :visible.sync="cashierDialog" width="55%" top="0" :before-close="handleClose"
	custom-class="cashier_custom_class">
	<iframe id="huoduan_frame" frameborder="0" scrolling="auto" name="main" :src="item" style="
              height: 88vh;
              visibility: inherit;
              width: 100%;
              z-index: 1;
              overflow: visible;
            ">
	</iframe>
	<span slot="footer" class="dialog-footer">
		<el-button @click="dialogVisible = false">取 消</el-button>
		<el-button type="primary" @click="dialogVisible = false">确 定</el-button>
	</span>
</el-dialog> -->
<el-drawer :visible.sync="cashierDialog" size="90%" direction="btt" :withHeader="false" :before-close="handleClose"
	custom-class="cashier_drawer_custom_class">
	<!-- allow="camera"（2026-09-24）：收银台与商家端是跨域（8001 → 5270）。
	     Permissions Policy 默认不给跨域 iframe 摄像头权限，收银台「扫码收款」调
	     navigator.mediaDevices 会被直接拒掉 → 必须在此显式放行。
	     另注：父页自身也得是 https:// 或 localhost，否则整条链仍不满足安全上下文。 -->
	<iframe id="huoduan_frame" frameborder="0" scrolling="auto" name="main" allow="camera" :src="item" style="
              height: 90vh;
              visibility: inherit;
              width: 60%;
              z-index: 1;
              overflow: visible;
            ">
	</iframe>
</el-drawer>
</el-row>

</template>

<script>
import config from '../../config/index.js';
export default {
	data() {
		var checkPassword = (rule, value, callback) => {
			let reg = /^[a-z0-9]+$/i
			if (!reg.test(value)) {
				if (rule.fullField === 'password') {
					callback(new Error('密码只包含数字、字母组成。'));
				} else {
					callback(new Error('用户名只包含数字、字母组成。'));
				}
			} else {
				callback();
			}
		};
		return {
			sysName: '湖里Lakes-商家中心',
			collapsed: false,
			sysUserName: '',
			sysUserAvatar: 'https://lakes-public.oss-cn-chengdu.aliyuncs.com/data/images/system/logo.png',
			form: {
				name: '',
				region: '',
				date1: '',
				date2: '',
				delivery: false,
				type: [],
				resource: '',
				desc: ''
			},
			changePasswordForm: false,
			passwordForm: {
				oldPassword: '',
				newPassword: ''
			},
			editFormRules: {
				oldPassword: [{ required: true, validator: checkPassword, trigger: 'blur' }],
				newPassword: [{ required: true, validator: checkPassword, trigger: 'blur' }],
			},
			logining: false,
			routerArr: [],
			cashierDialog: false,
			item: '',
			cashierHeight: '80%'
		}
	},
	// computed: {
	// 	routerArr() {
	// 		let arr = this.$router.options.routes
	// 		return arr.find(function(ele){
	// 			return ele.menuRole === 'seller'
	// 		})
	// 	}
	// },
	methods: {
		refreshUserInfo() {
			let vue = this
			var token = sessionStorage.getItem('token');
			vue.$http.post(vue, '/rest/merchant/getLoginMerchantInfo', { "token": token },
				(vue, data) => {
					let user = data.data
					sessionStorage.setItem('user', JSON.stringify(user));
				},
				(error, data) => {

				})
		},
		showDialog() {
			this.changePasswordForm = true
		},
		passwordSubmit() {
			let vue = this
			this.$refs.passwordForm.validate((valid) => {
				if (valid) {
					if (vue.passwordForm.newPassword.length < 6) {
						vue.$message({
							showClose: true,
							message: "密码长度最少6位",
							type: 'error'
						});
						return false;
					}
					vue.logining = true;
					let param = {
						oldPassword: vue.$utils.Base64(vue.passwordForm.oldPassword),
						newPassword: vue.$utils.Base64(vue.passwordForm.newPassword)
					}
					// 管理端登录体系：修改的是管理员账号（tb_admin）的密码
					vue.$http.post(vue, '/rest/admin/updatePassword', param,
						(vue, data) => {
							vue.logining = false;
							vue.$message({
								showClose: true,
								message: data.message,
								type: 'success'
							});
							vue.changePasswordForm = false
							vue.$router.push({ path: '/login' })
						},
						(error, data) => {
							vue.logining = false;
							vue.$message({
								showClose: true,
								message: data.message,
								type: 'error'
							});
						})
				} else {
					console.log('error submit!!');
					return false;
				}
			})
		},
		closeDialog(formName) {
			this.$refs[formName].resetFields();
		},
		handleopen() {
			//console.log('handleopen');
		},
		handleclose() {
			//console.log('handleclose');
		},
		handleselect: function (a, b) {
		},
		//退出登录
		logout: function () {
			var vue = this;
			this.$confirm('确认退出吗?', '提示', {
				//type: 'warning'
			}).then(() => {
				// 管理端登出（后端同时清理管理员与桥接的商家会话）
				vue.$http.post(vue, '/rest/admin/logout', {},
					function (vue, data) {
						sessionStorage.removeItem('user');
						vue.$router.push('/login');
					}, function (error, data) {
						vue.$message({
							showClose: true,
							message: data.message,
							type: 'error'
						});
					}
				)
			}).catch(() => {
			});
		},
		//折叠导航栏
		collapse: function () {
			this.collapsed = !this.collapsed;
		},
		showMenu(i, status) {
			this.$refs.menuCollapsed.getElementsByClassName('submenu-hook-' + i)[0].style.display = status ? 'block' : 'none';
		},
		formatRouter() {
			let arr = this.$router.options.routes
			return arr.filter(function (ele) {
				return ele.menuRole == 'seller'
			})
		},
		handleClose(done) {
			this.$confirm('确认关闭收银台点餐吗？')
				.then(_ => {
					this.item = '';
					done();
				})
				.catch(_ => { });
		},
		toCashier() {
			this.cashierDialog = true;
			var token = sessionStorage.getItem('token');
			var user = sessionStorage.getItem('user');
			user = JSON.parse(user);
			if (!user.shopId || !token) {
				this.$confirm('请登录后操作')
					.then(_ => {
						done();
					})
					.catch(_ => { });
				return
			}
			// 设置收银台点餐跳转地址及携带参数
			var params = JSON.stringify({ shopId: user.shopId, token: token });
			console.log('params', params);
			var p = this.$utils.Base64(params);
			this.item = config.appConfig.http.cashierBaseUrl + `/#/?p=${p}`;
			console.log(this.item);
		}
	},
	mounted() {
		// Pad 端适配（Pad端适配-实施计划-v1.1 §3.3）：窄屏（pad 竖屏/手机横屏 ≤900px）
		// 默认折叠侧栏为 60px 图标栏，点顶部图标可展开；仅进入时判定一次。
		if (window.innerWidth <= 900) {
			this.collapsed = true;
		}
		this.routerArr = this.$router.options.routes
		var user = sessionStorage.getItem('user');
		if (user) {
			user = JSON.parse(user);
			// 顶部店名不从 sessionStorage.user 取：该对象是商家账号（Merchant 无 shopName 字段），
			// 取到的恒为空。真实店名统一在下方的 getLoginMerchantShopInfo 回调里赋值。
			// 顶部头像同理：user(Merchant) 上没有 shopLogoImg 字段，直接拼接会得到 ".../undefined"（404）。
			// 这里先给默认图（走 publicUrl：本地联调=后端图片目录，线上=OSS），
			// 真实头像在下方 getLoginMerchantShopInfo 回调里按 Shop.shopLogoImg 覆盖。
			this.sysUserAvatar = this.$http.publicUrl('data/images/system/logo.png');
			if (user.issaler) {
				this.routerArr = this.formatRouter()
			}
			// this.isOperating = user.isOperating;


		}
		//获取开关设置
		var vue = this;
		vue.$http.post(vue, '/rest/merchant/shop/getLoginMerchantShopInfo', {},
			(vue, data) => {
				let shop = data.data
				this.sysUserName = shop.name || '';
				// 顶部头像：Shop 实体才有 shopLogoImg（Merchant 没有，见 mounted 注释）；
				// 空值兜底保留 mounted 里的默认图，避免拼出 ".../undefined"。
				if (shop.shopLogoImg) {
					this.sysUserAvatar = this.$http.publicUrl(shop.shopLogoImg);
				}
			}, (error, data) => {
				vue.$message({
					showClose: true,
					message: data.message,
					type: 'error'
				});
			}
		)
	}
}

</script>

<style scoped lang="scss">
.container {
	position: absolute;
	top: 0px;
	bottom: 0px;
	width: 100%;

	.header {
		height: 48px;
		line-height: 48px;
		background: white;
		color: black;

		.userinfo {
			text-align: right;
			padding-right: 35px;
			float: right;

			.userinfo-inner {
				cursor: pointer;
				color: #fff;

				img {
					width: 40px;
					height: 40px;
					border-radius: 20px;
					margin: 10px 0px 10px 10px;
					float: right;
				}
			}
		}

		.logo {
			//width:230px;
			height: 48px;
			// 标题「湖里Lakes-商家中心」在 230px 宽度内放不下 22px 字号，降到 18px 并收窄内边距，防止末字被截断
			font-size: 18px;
			padding-left: 12px;
			padding-right: 12px;
			white-space: nowrap;
			border-color: rgba(238, 241, 146, 0.3);
			border-right-width: 1px;
			border-right-style: solid;

			img {
				width: 40px;
				float: left;
				margin: 10px 10px 10px 18px;
			}

			.txt {
				color: #fff;
			}
		}

		.logo-width {
			width: 230px;
		}

		.logo-collapse-width {
			width: 60px
		}

		//头部正方体图标
		.tools {
			padding: 0px 23px;
			width: 14px;
			height: 48px;
			line-height: 48px;
			cursor: pointer;
		}
	}

	.main {
		display: flex;
		// background: #324057;
		position: absolute;
		top: 48px; //决定头部的高度
		bottom: 0px;
		overflow: hidden;

		aside {
			flex: 0 0 230px;
			width: 230px;

			// position: absolute;
			// top: 0px;
			// bottom: 0px;
			.el-menu {
				height: 100%;
				overflow: auto;
			}

			.collapsed {
				width: 60px;

				.item {
					position: relative;
				}

				.submenu {
					position: absolute;
					top: 0px;
					left: 60px;
					z-index: 99999;
					height: auto;
					display: none;
				}

			}
		}

		.menu-collapsed {
			flex: 0 0 60px;
			width: 60px;
		}

		.menu-expanded {
			flex: 0 0 230px;
			width: 230px;
		}

		.content-container {
			// background: #f1f2f7;
			flex: 1;
			// position: absolute;
			// right: 0px;
			// top: 0px;
			// bottom: 0px;
			// left: 230px;
			overflow-y: scroll;
			padding: 20px;

			.breadcrumb-container {

				//margin-bottom: 15px;
				.title {
					width: 200px;
					float: left;
					color: #475669;
				}

				.breadcrumb-inner {
					float: right;
				}
			}

			.content-wrapper {
				background-color: #fff;
				box-sizing: border-box;
			}
		}
	}

	.changePassword {
		.changePassform {
			margin: 0 auto;
			max-width: 600px;

			.el-input {
				max-width: 400px;
			}
		}
	}
}

//弹出层的高度
::v-deep .el-drawer.cashier_drawer_custom_class {

	.el-drawer__body {
		display: flex;
		justify-content: center;
	}
}

.cashier_drawer_custom_class .el-drawer__body {
	display: flex;
	justify-content: center;
	flex-direction: column;
}

//弹出层的高度
::v-deep .el-dialog.cashier_custom_class {
	height: 100vh;
	max-height: 100vh;
	overflow-y: auto;
}

//弹出层里内容的高度
::v-deep .el-dialog__body {
	max-height: 100vh !important;
}
</style>