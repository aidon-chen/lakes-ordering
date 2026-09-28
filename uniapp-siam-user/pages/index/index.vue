<template>
	<view class="page" :style="{ '--status-bar-height': statusBarHeight + 'px' }">

		<!-- C-02 首页活动弹窗 -->
		<view class="activity-mask" v-if="activityDialogShow" @tap="closeActivityDialog">
			<view class="activity-dialog" @tap.stop>
				<image v-if="activityAd.imagePath" :src="activityAd.imagePath" class="activity-image" mode="widthFix"
					@tap="activityImageTap" />
				<view class="activity-close" @tap="closeActivityDialog">
					<text class="activity-close-icon">✕</text>
				</view>
			</view>
		</view>

		<!-- A-01 海报区（全幅无圆角）：后台 type=1 海报优先，无数据时回落设计稿占位渐变 -->
		<view class="hero">
			<swiper class="hero-swiper" :autoplay="heroSlides.length > 1" :interval="5000" :duration="500"
				:circular="heroSlides.length > 1" @change="swiperChange">
				<swiper-item v-for="(slide, index) in heroSlides" :key="index">
					<image v-if="!slide.isPlaceholder" class="hero-image" :src="slide.imagePath" mode="aspectFill"
						:data-link="slide.imageLinkUrl" @tap="posterTap" />
					<view v-else class="hero-slide"></view>
				</swiper-item>
			</swiper>

			<!-- 音符水印（设计稿不透明度 12%，见 a01-opt 画板像素反解） -->
			<image class="hero-watermark" src="/static/assets/icons/login-note-white.png" mode="widthFix" />

			<!-- 品牌行：门店 logo + 门店名（选门店恢复⑥：点击进门店列表） -->
			<view class="hero-brand" @tap="shopTap">
				<image class="hero-brand-logo" :src="shopLogo" mode="aspectFill" />
				<view class="hero-brand-name">{{ shopName }}</view>
			</view>
			<view class="hero-shop-info" @tap="shopTap">{{ shopInfoLine }}</view>

			<!-- 底部渐变遮罩 -->
			<view class="hero-mask"></view>

			<!-- 海报文案（随当前张切换）+ 轮播指示点 -->
			<view class="hero-title" v-if="posterTitle">{{ posterTitle }}</view>
			<view class="hero-subtitle" v-if="posterSubtitle">{{ posterSubtitle }}</view>
			<view class="hero-dots">
				<view class="hero-dot" :class="{ 'is-active': swiperCurrent === index }"
					v-for="(item, index) in dotCount" :key="index"></view>
			</view>
		</view>

		<!-- A-01 主功能区 -->
		<view class="main-area">
			<view class="member-row" hover-class="hover-class-public" @tap="memberCenterTap">
				<view class="member-row-left">
					<image class="member-crown" src="/static/assets/icons/home-crown.png" mode="widthFix" />
					<view class="member-title">会员中心</view>
					<!-- 余额段用 hasMember 包住：接口返回前 / 未登录时不渲染，
					     避免出现「余额 ¥0.00」这种会被误读成"钱没了"的假 0（金额场景不做假占位） -->
					<view class="member-desc"><text v-if="hasMember">余额 ¥{{ balanceValue }} · </text>成长值 {{ growthValue }} · 优惠券 {{ couponsNumber }} 张</view>
				</view>
				<van-icon name="arrow" class="member-arrow" />
			</view>

			<view class="entry-row">
				<view class="entry-card" hover-class="hover-class-public" @tap="dineScanTap">
					<view class="entry-icon">
						<van-icon name="scan" class="entry-icon-glyph" />
					</view>
					<view class="entry-text">
						<view class="entry-name">点餐</view>
						<view class="entry-sub">扫桌角码 · 立即开台</view>
					</view>
				</view>
				<view class="entry-card" hover-class="hover-class-public" @tap="selfPickupTap">
					<view class="entry-icon">
						<van-icon name="bag-o" class="entry-icon-glyph" />
					</view>
					<view class="entry-text">
						<view class="entry-name">外带自提</view>
						<view class="entry-sub">商城好物 · 到店取货</view>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	import GlobalConfig from '../../utils/global-config';
	import https from '../../utils/http';
	import authService from '../../utils/auth';
	//获取应用实例
	let app = null;
	export default {
		data() {
			return {
				statusBarHeight: 0,
				swiperCurrent: 0,
				// 海报区：后台配置的首页海报（type=1）。imageName=主标题，description=副标题（R7）
				// 上传入口：管理端 / 营销中心 / 首页海报
				posterList: [],
				// 门店信息：优先取用户在「门店列表」选过的 currentShopId，其次按定位取最近可自提门店；
				// 取不到时保留下面的兜底文案，不阻塞首页渲染。取数见 loadShopInfo()。
				shopName: '湖里Lakes',
				shopLogo: '/static/assets/icons/home-brand-mark.png',
				shopInfoLine: '***市 **区 **路 **号 · 营业中至 02:00',
				// 会员信息：/rest/member/getLoginMemberInfo（未登录时为空对象）
				member: {},
				// C-02 活动弹窗
				activityDialogShow: false,
				activityAd: {
					imagePath: '',
					imageLinkUrl: ''
				}
			};
		},
		computed: {
			// 海报区数据源：后台 type=1 海报优先；无数据时回落设计稿占位渐变（带默认文案，保证视觉不空）
			heroSlides() {
				if (this.posterList.length > 0) {
					return this.posterList.map(function(item) {
						return {
							isPlaceholder: false,
							imagePath: item.imagePath,
							imageLinkUrl: item.imageLinkUrl,
							imageName: item.imageName,
							description: item.description
						};
					});
				}
				return [{
					isPlaceholder: true,
					imageName: '中秋主题夜',
					description: '9/25 乐队现场 · 双款特调首发'
				}, {
					isPlaceholder: true,
					imageName: '中秋主题夜',
					description: '9/25 乐队现场 · 双款特调首发'
				}];
			},
			// 指示点数量跟随海报张数
			dotCount() {
				return this.heroSlides.length;
			},
			// 当前张的主标题 / 副标题（R7：imageName=主标题，description=副标题）
			posterTitle() {
				const slide = this.heroSlides[this.swiperCurrent];
				return (slide && slide.imageName) || '';
			},
			posterSubtitle() {
				const slide = this.heroSlides[this.swiperCurrent];
				return (slide && slide.description) || '';
			},
			// 是否已拿到会员数据：决定「余额」段是否渲染（避免未加载时显示 ¥0.00 造成误读）
			hasMember() {
				return !!(this.member && this.member.id);
			},
			// 账户余额（后端 getLoginMemberInfo 的 balance，decimal(10,2) → 固定两位小数）。
			// 会员条此前只显示"成长值 + 优惠券"，用户反馈「小程序里看不到余额」故补上。
			balanceValue() {
				const member = this.member || {};
				const value = Number(member.balance);
				return isFinite(value) ? value.toFixed(2) : '0.00';
			},
			// 成长值：tb_member.growth（2026-09-19 已加列）；老数据无该字段时退化取积分，保证展示真实非造数
			growthValue() {
				const member = this.member || {};
				const value = member.growth !== null && member.growth !== undefined ? member.growth : member.points;
				return value === null || value === undefined ? 0 : value;
			},
			couponsNumber() {
				const member = this.member || {};
				return member.couponsNumber === null || member.couponsNumber === undefined ? 0 : member.couponsNumber;
			}
		},
		onLoad: function() {
			app = getApp();
			this.statusBarHeight = app.globalData.systemInfoSync.statusBarHeight;
			this.getMemberInfo();
			this.getPosterList();
		},
		onShow: function() {
			if (!app) {
				app = getApp();
			}
			// C-02 首页活动弹窗：每次都弹 —— 由 App.vue 的 onShow 置位（= 每次打开小程序），此处消费。
			// 放在 onShow 而非 onLoad：冷启动/后台恢复都走 onShow；切 tab 回来时 App.onShow 未触发、
			// 标记已被消费，故不会重复弹。
			if (app.globalData.needActivityPopup) {
				app.globalData.needActivityPopup = false;
				this.getActivityDialog();
			}
			const addressInfo = app.globalData.deliveryAndSelfTaking.initRegeoInfo;
			if (addressInfo) {
				app.globalData.deliveryAndSelfTaking.regeoInfo = addressInfo;
				app.globalData.deliveryAndSelfTaking.location = addressInfo.location;
			}
			// 门店名/地址：切 tab 回来或换过门店都要刷新
			this.loadShopInfo();
		},
		onPullDownRefresh() {
			this.getMemberInfo();
			this.getPosterList();
			setTimeout(() => {
				uni.stopPullDownRefresh();
			}, 1000);
		},
		onHide() {
			this.activityDialogShow = false;
		},
		methods: {
			swiperChange(e) {
				this.swiperCurrent = e.detail.current;
			},

			// 选门店入口（选门店恢复⑥）：品牌行/门店信息行 → 门店列表页
			shopTap() {
				uni.navigateTo({ url: '/pages/shop/shop-list/shop-list' });
			},

			// 门店信息：① 用户选过的 currentShopId → /rest/shop/detail；
			// ② 否则按定位 → /rest/shop/listForPickup 取最近一家 → 再用 detail 补全；
			// ③ 全失败则保留 data 里的兜底文案。任何一步失败都静默降级，不阻塞首页。
			loadShopInfo() {
				const applyShop = (shop) => {
					if (!shop) return;
					if (shop.name) {
						this.shopName = shop.name;
					}
					const address = [shop.street, shop.houseNumber].filter(Boolean).join('');
					const operating = shop.isOperating ? ('营业中至 ' + (shop.endTime || '')) : '暂停营业';
					const line = [address, operating].filter(Boolean).join(' · ');
					if (line) {
						this.shopInfoLine = line;
					}
				};
				const loadById = (shopId) => https.request('/rest/shop/detail', {
					id: shopId
				}).then((result) => {
					if (result && result.success && result.data && result.data.shop) {
						applyShop(result.data.shop);
					}
				});
				const currentShopId = uni.getStorageSync('currentShopId');
				if (currentShopId) {
					loadById(currentShopId).catch(() => {});
					return;
				}
				const globalData = (app && app.globalData && app.globalData.deliveryAndSelfTaking) || {};
				const location = globalData.location;
				if (!location) {
					return;
				}
				https.request('/rest/shop/listForPickup', {
					pageNo: -1,
					pageSize: 1,
					position: location
				}).then((result) => {
					const records = result && result.data && result.data.records;
					if (records && records.length > 0) {
						applyShop(records[0]);
						return loadById(records[0].id);
					}
				}).catch(() => {});
			},

			// A-01 会员中心行：登录态才返回数据，未登录保持 0 值空态
			getMemberInfo() {
				https.request('/rest/member/getLoginMemberInfo', {}).then((result) => {
					if (result && result.success && result.data) {
						this.member = result.data;
						// 成长值换源（2026-09-27）：growth 是种子死数，改用积分流水合计回填
						//（会员条 growthValue computed 自动跟随；与 C-01/C-03/C-08 同源）
						https.request('/rest/member/points/myPoints', {}).then((res) => {
							if (res && res.success && res.data) {
								this.member.growth = Number(res.data.totalPoints) || 0;
							}
						}).catch(() => {});
					}
				}).catch(() => {});
			},

			// A-01 海报区：后台配置的首页海报（type=1）。
			// onlyValid=true 由后端按生效时间窗过滤（start/end 为空视为不限）；
			// 无数据时 computed.heroSlides 回落设计稿占位渐变。
			getPosterList() {
				https.request('/rest/advertisement/list', {
					type: 1,
					pageNo: -1,
					pageSize: 20,
					onlyValid: true
				}).then((result) => {
					if (result && result.success && result.data && result.data.records) {
						this.posterList = result.data.records.map(function(item) {
							return {
								imagePath: GlobalConfig.ossUrl + item.imagePath,
								imageLinkUrl: item.imageLinkUrl || '',
								imageName: item.imageName || '',
								description: item.description || ''
							};
						}).filter(function(item) {
							return !!item.imagePath;
						});
						this.swiperCurrent = 0;
					}
				}).catch(() => {});
			},

			// 点击海报跳转（沿用原轮播图的登录校验口径）
			posterTap(e) {
				const url = e.currentTarget.dataset.link;
				if (!url) return;
				let _this = this;
				authService.checkIsLogin().then((result) => {
					if (result) {
						_this.navigateToAd(url);
						return;
					}
					app.globalData.checkIsAuth('scope.userInfo');
				});
			},

			// C-02 首页活动弹窗：取弹窗广告（type=2），无数据则不展示。
			// onlyValid=true：由后端按生效时间窗过滤（此前客户端不判时间，过期弹窗照常弹出）
			getActivityDialog() {
				https.request('/rest/advertisement/list', {
					type: 2,
					pageNo: -1,
					pageSize: 1,
					onlyValid: true
				}).then((result) => {
					if (result.success && result.data && result.data.records && result.data.records
						.length > 0) {
						const ad = result.data.records[0];
						this.activityAd = {
							imagePath: GlobalConfig.ossUrl + ad.imagePath,
							imageLinkUrl: ad.imageLinkUrl || ''
						};
						this.activityDialogShow = true;
					}
				});
			},
			closeActivityDialog() {
				this.activityDialogShow = false;
			},
			activityImageTap() {
				if (!this.activityAd.imageLinkUrl) return;
				this.activityDialogShow = false;
				let _this = this;
				authService.checkIsLogin().then((result) => {
					if (result) {
						_this.navigateToAd(_this.activityAd.imageLinkUrl);
						return;
					}
					app.globalData.checkIsAuth('scope.userInfo');
				});
			},
			navigateToAd(url) {
				https.request('/rest/member/getLoginMemberInfo', {}).then((result) => {
					if (result.success) {
						uni.navigateTo({
							url: url
						});
					} else {
						app.globalData.checkIsAuth('scope.userInfo');
					}
				});
			},

			// A-01 会员入口条：先判登录态。未登录直接引导登录 ——
			// 否则会被会员中心页的守卫弹回（实测 navigateTo:ok → 800ms 后 navigateBack），
			// 用户看到「闪一下回来」会误判为「跳转失败」（2026-09-22 复现）
			memberCenterTap() {
				authService.checkIsLogin().then((isLogin) => {
					if (isLogin) {
						uni.navigateTo({
							url: '/pages/mine/member/index/index'
						});
						return;
					}
					uni.navigateTo({
						url: '/pages/internal/login/choose/choose'
					});
				});
			},

			// A-01 点餐：扫桌台码 → 台位确认页（C-04：校验台位＋选人数）→ 开始点餐进点单台
			dineScanTap() {
				// 原生扫码界面会把小程序压后台再回前台 → App.onShow 误触发活动弹窗标记；
				// 扫码期间置抑制位，complete 后 500ms 复位（09-24 修复）
				app.globalData.scanInProgress = true;
				uni.scanCode({
					success: (res) => {
						const text = res.result || '';
						// 桌台码内容支持：tid=05&area=A 或含这些参数的 URL
						const match = text.match(/(?:\?|&)?(tid|tableId|tableNo)=(\w+)/i);
						if (!match) {
							uni.showToast({
								title: '桌台码无效，请扫描桌台二维码',
								icon: 'none'
							});
							return;
						}
						// 取查询串作为 scene 传台位确认页（URL 取 ? 后部分，裸参数原样）
						const qIdx = text.indexOf('?');
						const scene = qIdx > -1 ? text.substring(qIdx + 1) : text;
						uni.navigateTo({
							url: '/pages/menu/scan-table/scan-table?scene=' + encodeURIComponent(scene)
						});
					},
					fail: () => {},
					complete: () => {
						// 500ms 宽限：兜住「App.onShow 晚于 complete 回调」的时序抖动
						setTimeout(() => {
							app.globalData.scanInProgress = false;
						}, 500);
					}
				});
			},

			// A-01 外带自提：切自提模式进点餐台
			selfPickupTap() {
				app.globalData.deliveryAndSelfTaking.selfOutActiveIndex = 0;
				app.globalData.deliveryAndSelfTaking.dineInfo = null;
				app.globalData.deliveryAndSelfTaking.ifIndexSwitchTab = true;
				app.globalData.deliveryAndSelfTaking.ifChooseBack = false;
				app.globalData.deliveryAndSelfTaking.ifChoosePayBack = false;
				uni.switchTab({
					url: '/pages/menu/index/index'
				});
			}
		}
	};
</script>
<style>
	page {
		width: 100%;
		background: #F0F3F2;
	}

	.page {
		min-height: 100vh;
	}

	/* ===== A-01 海报区 =====
	   渐变与不透明度口径来自画板「A-01 首页·优化版」334:90 的栅格像素反解：
	   linear 180°，#253730 0% → #4F6B5E 55% → #739B8D 100%；
	   音符水印为不透明白叠加 12%。
	   注：本页强调色按设计稿取 #739B8D，与全站主题绿 #5f7d22 存在色差（本次经确认采纳）。 */
	.hero {
		position: relative;
		width: 100%;
		/* 沉浸式海报（09-28 用户拍板）：页面不再预留 statusBarHeight 空隙，渐变直顶状态栏；
		   高度 = 100vh（custom 导航下为扣掉原生 tabBar 的窗口高）- 功能区 296rpx
		   → 首页恰好一屏，不再出现上下滚动 */
		height: calc(100vh - 296rpx);
		background: linear-gradient(180deg, #253730 0%, #4F6B5E 55%, #739B8D 100%);
		overflow: hidden;
	}

	.hero-swiper,
	.hero-slide,
	.hero-image {
		width: 100%;
		height: 100%;
	}

	/* 真实海报图（后台配置，规格 750×1140，aspectFill 填满 hero） */
	.hero-image {
		display: block;
	}

	/* 占位渐变（无后台数据时的设计稿兜底，同 .hero 底色） */
	.hero-slide {
		background: linear-gradient(180deg, #253730 0%, #4F6B5E 55%, #739B8D 100%);
	}

	/* 音符水印：画板 334:93，左 169.4pt 上 161.4pt 宽 171.5pt，不透明度 0.12
	   pointer-events:none —— 纯装饰层不截触摸，保证 swiper 手势可滑动（09-28） */
	.hero-watermark {
		position: absolute;
		left: 338rpx;
		/* 沉浸后 hero 顶=状态栏顶，设计稿坐标基于状态栏下方内容区 → 统一下移状态栏高度 */
		top: calc(322rpx + var(--status-bar-height, 0px));
		width: 344rpx;
		height: 344rpx;
		opacity: 0.12;
		pointer-events: none;
	}

	/* 品牌行：画板 334:97，left 16pt / top 14pt，logo 24pt + gap 8pt（纯展示，触摸穿透） */
	.hero-brand {
		position: absolute;
		left: 32rpx;
		top: calc(28rpx + var(--status-bar-height, 0px));
		display: flex;
		align-items: center;
		/* 选门店恢复⑥：品牌行可点跳门店列表 —— 须 auto 接管点击，
		   与 1.1.9 触摸穿透修复的取舍：仅这两小块区域内海报不可滑动手势 */
		pointer-events: auto;
	}

	.hero-brand-logo {
		width: 48rpx;
		height: 48rpx;
		margin-right: 16rpx;
		border-radius: 24rpx;
	}

	.hero-brand-name {
		font-size: 30rpx;
		font-weight: 500;
		color: #FFFFFF;
	}

	/* 门店信息：画板 334:104，left 16pt / top 44pt，11pt（纯展示，触摸穿透） */
	.hero-shop-info {
		position: absolute;
		left: 32rpx;
		top: calc(88rpx + var(--status-bar-height, 0px));
		font-size: 22rpx;
		font-weight: 400;
		color: #FFFFFF;
		/* 同 .hero-brand：选门店入口行，恢复可点 */
		pointer-events: auto;
	}

	/* 底部渐变遮罩：画板 334:109，高 200pt，透明 → 黑 55%（纯展示，触摸穿透） */
	.hero-mask {
		position: absolute;
		left: 0;
		right: 0;
		bottom: 0;
		height: 400rpx;
		background: linear-gradient(180deg, rgba(0, 0, 0, 0) 0%, rgba(0, 0, 0, 0.55) 100%);
		pointer-events: none;
	}

	/* 海报主/副标题：画板 334:110/334:111，left 20pt，距底 80pt / 59pt（纯展示，触摸穿透） */
	.hero-title {
		position: absolute;
		left: 40rpx;
		bottom: 160rpx;
		font-size: 52rpx;
		line-height: 76rpx;
		font-weight: 700;
		color: #FFFFFF;
		pointer-events: none;
	}

	.hero-subtitle {
		position: absolute;
		left: 40rpx;
		bottom: 118rpx;
		font-size: 26rpx;
		line-height: 38rpx;
		font-weight: 400;
		color: #FFFFFF;
		pointer-events: none;
	}

	/* 轮播指示点：画板 343:29，整体居中，距底 30pt；未选中白 44%，选中白色 16pt 长胶囊（纯展示，触摸穿透） */
	.hero-dots {
		position: absolute;
		left: 0;
		right: 0;
		bottom: 60rpx;
		height: 12rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		pointer-events: none;
	}

	.hero-dot {
		width: 12rpx;
		height: 12rpx;
		margin: 0 6rpx;
		border-radius: 6rpx;
		background: rgba(255, 255, 255, 0.44);
	}

	.hero-dot.is-active {
		width: 32rpx;
		background: #FFFFFF;
	}

	/* ===== A-01 主功能区（画板 334:114，padding 10pt / gap 8pt） ===== */
	.main-area {
		padding: 20rpx;
		display: flex;
		flex-direction: column;
	}

	/* 会员中心跳转行：画板 343:1，高 32pt；左组 gap 6pt */
	.member-row {
		height: 64rpx;
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.member-row-left {
		display: flex;
		align-items: center;
		/* 允许内部文字收缩并可省略：不加这两行，flex 子项不肯收缩，长文本会顶破固定高的容器 */
		flex: 1;
		min-width: 0;
	}

	.member-crown {
		width: 28rpx;
		height: 28rpx;
		margin-right: 12rpx;
	}

	.member-title {
		font-size: 26rpx;
		font-weight: 500;
		color: #333333;
	}

	.member-desc {
		margin-left: 12rpx;
		font-size: 22rpx;
		font-weight: 400;
		color: #999999;
		/* 会员条是固定高 64rpx 的单行：内容超长时省略号截断。
		   加入「余额 ¥xxxx.xx」后文本明显变长，更易触发，故补此保护。 */
		overflow: hidden;
		white-space: nowrap;
		text-overflow: ellipsis;
		min-width: 0;
	}

	/* 箭头：画板 343:9，6.6pt × 16.4pt，描边 #739B8D */
	.member-arrow {
		font-size: 56rpx;
		color: #739B8D;
	}

	/* 双主功能：画板 334:116，gap 10pt；卡片 172.5pt × 88pt / 圆角 12pt / 内边距 12pt */
	.entry-row {
		display: flex;
		margin-top: 16rpx;
	}

	.entry-card {
		flex: 1;
		height: 176rpx;
		padding: 24rpx;
		box-sizing: border-box;
		background: #FFFFFF;
		border-radius: 24rpx;
		display: flex;
		align-items: center;
		overflow: hidden;
	}

	.entry-card:last-child {
		margin-left: 20rpx;
	}

	/* 图标底：画板 343:11，36pt 方形 / 圆角 10pt / #E8F0EC */
	.entry-icon {
		width: 72rpx;
		height: 72rpx;
		margin-right: 20rpx;
		border-radius: 20rpx;
		background: #E8F0EC;
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
	}

	.entry-icon-glyph {
		font-size: 40rpx;
		color: #739B8D;
	}

	.entry-text {
		flex: 1;
		display: flex;
		flex-direction: column;
		overflow: hidden;
	}

	.entry-name {
		font-size: 30rpx;
		line-height: 42rpx;
		font-weight: 500;
		color: #333333;
	}

	.entry-sub {
		margin-top: 4rpx;
		font-size: 20rpx;
		line-height: 28rpx;
		font-weight: 400;
		color: #999999;
		white-space: nowrap;
	}

	/* ===== C-02 活动弹窗 ===== */
	.activity-mask {
		position: fixed;
		left: 0;
		top: 0;
		right: 0;
		bottom: 0;
		z-index: 9999;
		display: flex;
		align-items: center;
		justify-content: center;
		background: rgba(0, 0, 0, 0.55);
	}

	.activity-dialog {
		width: 560rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
	}

	.activity-image {
		width: 560rpx;
		border-radius: 24rpx;
		background: #FFFFFF;
	}

	.activity-close {
		margin-top: 32rpx;
		width: 64rpx;
		height: 64rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		border: 2rpx solid rgba(255, 255, 255, 0.8);
		border-radius: 50%;
	}

	.activity-close-icon {
		font-size: 32rpx;
		color: #FFFFFF;
		line-height: 1;
	}
</style>
