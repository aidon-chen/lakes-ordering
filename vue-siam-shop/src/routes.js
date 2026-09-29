// const Login = () => import( './pages/Login.vue')
const LoginMain = () => import('./pages/LoginMain.vue')
const NotFound = () => import( './pages/404.vue')
const Home = () => import( './pages/Home.vue')
const Main = () => import( './pages/Main.vue')

const login = () => import('./pages/login/login.vue')
const register = () => import('./pages/login/register.vue')
const userAgreement = () => import('./pages/login/userAgreement.vue')
const QuickLogin = () => import('./pages/login/QuickLogin.vue')
const setPassword = () => import('./pages/login/setPassword.vue')
const fillInformation = () => import('./pages/login/fillInformation.vue')

//空页面
const emptyList = () => import( './pages/basicModule/emptyList.vue')
//const CoreImage = () => import( './pages/basicModule/CoreImage.vue')


//商品管理
const menuList = () => import( './pages/goodsManage/menuList.vue')
const goodsList = () => import( './pages/goodsManage/goodsList.vue')
const addGoods = () => import( './pages/goodsManage/addGoods.vue')
const editGoods = () => import( './pages/goodsManage/editGoods.vue')
const goodsAccessoriesList = () => import( './pages/goodsManage/goodsAccessoriesList.vue')
const rawmaterialList = () => import( './pages/goodsManage/rawmaterialList.vue')
const goodsRawmaterialRelationList = () => import( './pages/goodsManage/goodsRawmaterialRelationList.vue')
const specificationList = () => import( './pages/goodsManage/specificationList.vue')
const stockList = () => import( './pages/goodsManage/stockList.vue')

//用户管理（会员管理）
const memberList = () => import( './pages/memberManage/memberList.vue')
const memberDetail = () => import( './pages/memberManage/memberDetail.vue')

//订单管理
const waitHandleOrderList = () => import( './pages/orderManage/waitHandleOrderList.vue')
const forHereTabsChild1 = () => import( './pages/orderManage/forHereTabsChild1.vue')
const takeOutTabsChild1 = () => import( './pages/orderManage/takeOutTabsChild1.vue')
const takeOutTabsChild2 = () => import( './pages/orderManage/takeOutTabsChild2.vue')
const forHereTabsChild2 = () => import( './pages/orderManage/forHereTabsChild2.vue')
const refundOrderList = () => import( './pages/orderManage/refundOrderList.vue')
//C-15 发票申请审批（商家完结：已开具/已驳回）
const invoiceApprovalList = () => import( './pages/orderManage/invoiceApprovalList.vue')
const forHereTabsChild3 = () => import( './pages/orderManage/forHereTabsChild3.vue')
const orderDetail = () => import( './pages/orderManage/orderDetail.vue')
//const unpaidOrderList = () => import( './pages/MallManage/unpaidOrderList.vue')
const forHereOrderList = () => import( './pages/orderManage/forHereOrderList.vue')
const takeOutOrderList = () => import( './pages/orderManage/takeOutOrderList.vue')
const todayOrderList = () => import( './pages/orderManage/todayOrderList.vue')
const appraiseList = () => import( './pages/orderManage/appraiseList.vue')

//促销管理
const couponsList = () => import( './pages/promotionManage/couponsList.vue')

//营销中心（M-01~M-04 / A-10 首页海报）
const activityPopupList = () => import( './pages/marketing/activityPopupList.vue')
const homePosterList = () => import( './pages/marketing/homePosterList.vue')

//门店管理
const shopList = () => import( './pages/shopManage/shopList.vue')   // A-01 门店信息（含启停）
const shopInfo = () => import( './pages/shopManage/shopInfo.vue')
const shopInfoImportant = () => import( './pages/shopManage/shopInfoImportant.vue')
const shopChangeRecord = () => import( './pages/shopManage/shopChangeRecord.vue')
const courierList = () => import( './pages/shopManage/internal/courierList.vue')
const ticketPrinterList = () => import( './pages/shopManage/internal/ticketPrinterList.vue')
const labelPrinterList = () => import( './pages/shopManage/internal/labelPrinterList.vue')

//系统设置（A-01 门店信息 / A-02 基础参数与业务字典 / A-08 日志审计）
const settingList = () => import( './pages/systemSetting/settingList.vue')
const dictList = () => import( './pages/systemSetting/dictList.vue')
const operateLogList = () => import( './pages/systemSetting/operateLogList.vue')
const operateLogDetail = () => import( './pages/systemSetting/operateLogDetail.vue')
const loginLogList = () => import( './pages/systemSetting/loginLogList.vue')

//轮播图管理
const advertisementList = () => import( './pages/shopDecoration/advertisementList.vue')

//台位管理（A-09）
const tableList = () => import( './pages/dining/tableList.vue')

//商城管理（A-06，迁自 vue-siam-admin 积分商城 + 自提语义改造）
const mallMenuList = () => import( './pages/mallManage/goodsManage/menuList.vue')
const mallAddMenu = () => import( './pages/mallManage/goodsManage/addMenu.vue')
const mallEditMenu = () => import( './pages/mallManage/goodsManage/editMenu.vue')
const mallGoodsList = () => import( './pages/mallManage/goodsManage/goodsList.vue')
const mallAddGoods = () => import( './pages/mallManage/goodsManage/addGoods.vue')
const mallEditGoods = () => import( './pages/mallManage/goodsManage/editGoods.vue')
const mallStockList = () => import( './pages/mallManage/goodsManage/stockList.vue')
const mallOrderList = () => import( './pages/mallManage/orderManage/orderList.vue')
const mallFullReductionRuleList = () => import( './pages/mallManage/promotionManage/fullReductionRuleList.vue')
//商城轮播（2026-09-25）：tb_advertisement type=3，顾客端商城首页顶部轮播
const mallCarouselList = () => import( './pages/mallManage/advertisementManage/mallCarouselList.vue')

//数据中心（2026-09-22：菜单项与路由已下线，页面文件 pages/basicModule/statisticGraph.vue 保留未删）
const statisticGraph = () => import( './pages/basicModule/statisticGraph.vue')

//财务报表
const merchantBillingRecord = () => import( './pages/accountModule/merchantBillingRecord.vue')
//A-07 报表与营收统计
const revenueList = () => import( './pages/accountModule/revenueList.vue')
const vipRechargeRecord = () => import( './pages/accountModule/vipRechargeRecord.vue')
//（2026-09-28 奖励金/分销体系整体下线：memberWithdrawRecord 页面与菜单已删）
//C-17 会员身份认证审核（身份证与身份材料双图同屏）
const memberIdentityAuth = () => import( './pages/accountModule/memberIdentityAuth.vue')


let routes = [
    // {
    //     path: '/login',
    //     component: Login,
    //     name: '',
    //     hidden: true
    // },
    {
      path: '/login',
      component: LoginMain,
      name: '',
      hidden: true,
      children: [
          { path: '/', component: login, pagename: '登录', icon: 'el-icon-search', hidden: true },
          { path: '/register', component: register, pagename: '注册', icon: 'el-icon-search', hidden: true },
          { path: '/QuickLogin', component: QuickLogin, pagename: '手机验证登录', icon: 'el-icon-search', hidden: true },
          { path: '/setPassword', component: setPassword, pagename: '忘记密码', icon: 'el-icon-search', hidden: true },
          
      ]
    },    
    {
      path: '/userAgreement',
      component: userAgreement,
      name: '',
      hidden: true
    },    
    {
      path: '/fillInformation',
      component: fillInformation,
      name: '',
      hidden: true
    },            
    {
        path: '/404',
        component: NotFound,
        name: '',
        hidden: true
    },
    {
      // 原「数据中心」位置：2026-09-22 起改由「营收统计」占用（数据中心菜单已下线）
      path: '/',
      component: Home,
      name: '营收统计',
      iconCls: 'el-icon-user',
      leaf: true,//只有一个节点
      children: [
          { path: '/revenueList', component: revenueList, name: '营收统计' },
      ]
    },
    {
      path: '/',
      component: Home,
      name: '用户管理',
      iconCls: 'el-icon-user',
      children: [
          { path: '/memberList', component: memberList, name: '会员列表' },
          { path: '/memberDetail', component: memberDetail, name: '会员详情', hidden: true },
          { path: '/memberIdentityAuth', component: memberIdentityAuth, name: '身份认证审核' },//C-17（2026-09-27 自财务报表迁入）
      ]
    },
    {
      path: '/',
      component: Home,
      name: '订单管理',
      iconCls: 'el-icon-s-order',
      children: [
          { path: '/todayOrderList', component: todayOrderList, name: '订单查询' },
          { path: '/forHereOrderList', component: forHereOrderList, name: '自取订单' },
          // 需求外（单店堂食+自提场景）：隐藏菜单，保留页面与路由
          { path: '/takeOutOrderList', component: takeOutOrderList, name: '外卖订单', hidden: true },          
          // { path: '/unpaidOrderList', component: unpaidOrderList, name: '未付款' },
          // { path: '/waitHandleOrderList', component: waitHandleOrderList, name: '待处理订单' },
          // { path: '/forHereTabsChild1', component: forHereTabsChild1, name: '待自取订单' },
          // { path: '/takeOutTabsChild1', component: takeOutTabsChild1, name: '待配送订单' },
          // { path: '/takeOutTabsChild2', component: takeOutTabsChild2, name: '已配送订单' },
          // { path: '/forHereTabsChild2', component: forHereTabsChild2, name: '已完成订单' },       
          { path: '/refundOrderList', component: refundOrderList, name: '售后订单' },
          { path: '/invoiceApprovalList', component: invoiceApprovalList, name: '发票审批' },
          { path: '/appraiseList', component: appraiseList, name: '评价管理' },
          // { path: '/forHereTabsChild3', component: forHereTabsChild3, name: '已取消订单' },          
          { path: '/orderDetail', component: orderDetail, name: '查看订单详情', hidden: true },   
         // { path: '/CoreImage', component: CoreImage, name: '裁剪图片' },        
      ]
    }, 
    {
      path: '/',
      component: Home,
      name: '商品管理',
      iconCls: 'el-icon-milk-tea',
      children: [
          { path: '/menuList', component: menuList, name: '分类管理' },
          { path: '/goodsList', component: goodsList, name: '商品列表' },
          { path: '/addGoods', component: addGoods, name: '新增商品', hidden: true },
          { path: '/editGoods', component: editGoods, name: '编辑商品', hidden: true },
          // { path: '/goodsAccessoriesList', component: goodsAccessoriesList, name: '商品辅料' },          
          // { path: '/rawmaterialList', component: rawmaterialList, name: '商品原料' },  
          // { path: '/goodsRawmaterialRelationList', component: goodsRawmaterialRelationList, name: '原料配比' },  
          { path: '/specificationList', component: specificationList, name: '商品规格' },
          { path: '/stockList', component: stockList, name: '商品库存' },          
      ]
    },        
    {
      path: '/',
      component: Home,
      name: '商城管理',
      iconCls: 'el-icon-shopping-bag-1',
      children: [
          { path: '/pointsMall_menuList', component: mallMenuList, name: '商城分类' },
          { path: '/pointsMall_goodsList', component: mallGoodsList, name: '商城商品' },
          { path: '/pointsMall_stockList', component: mallStockList, name: '商城库存' },
          { path: '/pointsMall_orderList', component: mallOrderList, name: '商城订单' },
          // 满减规则菜单已下线（2026-09-29 A 批）：满减统一走券体系（PointsMallCoupons.preferentialType=2），页面文件保留未删（先例同「数据中心」）
          // { path: '/pointsMall_fullReductionRuleList', component: mallFullReductionRuleList, name: '满减规则' },
          { path: '/pointsMall_carouselList', component: mallCarouselList, name: '商城轮播' },//商城首页顶部轮播（type=3）
          // 隐藏路由（弹层外置页）：路径名沿用 admin 源页内的跳转目标，保证页面内导航零改动
          { path: '/pointsMall_addMenu', component: mallAddMenu, name: '新增分类', hidden: true },
          { path: '/pointsMall_editMenu', component: mallEditMenu, name: '编辑分类', hidden: true },
          { path: '/pointsMall_addGoods', component: mallAddGoods, name: '新增商品', hidden: true },
          { path: '/pointsMall_editGoods', component: mallEditGoods, name: '编辑商品', hidden: true },
      ]
    },        
    {
      path: '/',
      component: Home,
      name: '营销中心',
      iconCls: 'el-icon-s-order',
      children: [
          { path: '/activityPopupList', component: activityPopupList, name: '首页活动弹窗' },//M-01/M-02
          { path: '/homePosterList', component: homePosterList, name: '首页海报' },//A-10 首页海报（type=1，R3 迁移单一入口）
          { path: '/couponsList', component: couponsList, name: '优惠券管理' },
      ]
    },     
    {
      path: '/',
      component: Home,
      name: '台位管理',
      iconCls: 'el-icon-s-grid',
      children: [
          { path: '/tableList', component: tableList, name: '台位与桌码' },
      ]
    },
    {
      path: '/',
      component: Home,
      name: '打印机管理',
      iconCls: 'el-icon-user',
      children: [
          { path: '/ticketPrinterList', component: ticketPrinterList, name: '小票打印机列表' },
          { path: '/labelPrinterList', component: labelPrinterList, name: '标签打印机列表' },
      ]
    },
    {
      path: '/',
      component: Home,
      name: '财务报表',
      iconCls: 'el-icon-user',
      children: [
          // 营收统计已提升为一级菜单（2026-09-22）
          { path: '/merchantBillingRecord', component: merchantBillingRecord, name: '账单记录' },//交易明细          
          { path: '/vipRechargeRecord', component: vipRechargeRecord, name: '会员充值记录' },
          //（2026-09-28 删「奖励金提现记录」memberWithdrawRecord——分销/奖励金体系已整体下线）
          // 身份认证审核已迁至「用户管理」分组（2026-09-27）
      ]
    },        
    {
      path: '/',
      component: Home,
      name: '门店管理',
      iconCls: 'el-icon-house',
      children: [
          { path: '/shopInfo', component: shopInfo, name: '门店基本信息' },
          { path: '/shopInfoImportant', component: shopInfoImportant, name: '门店重要信息' },
          { path: '/shopChangeRecord', component: shopChangeRecord, name: '门店信息变更记录' },          
          { path: '/advertisementList', component: advertisementList, name: '门店装修' },
          { path: '/courierList', component: courierList, name: '骑手信息', hidden: true },
      ]
    },
    {
      path: '/',
      component: Home,
      name: '系统设置',
      iconCls: 'el-icon-setting',
      children: [
          { path: '/settingList', component: settingList, name: '基础参数配置' },
          { path: '/dictList', component: dictList, name: '业务字典' },
          { path: '/shopList', component: shopList, name: '门店信息' },
          { path: '/operateLogList', component: operateLogList, name: '操作日志' },
          { path: '/operateLogDetail', component: operateLogDetail, name: '操作日志详情', hidden: true },
          { path: '/loginLogList', component: loginLogList, name: '登录日志' },
      ]
    },
    {
        path: '*',
        hidden: true,
        redirect: { path: '/404' }
    }
];

export default routes;