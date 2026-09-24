/**
 * 会员权益配置（C-03 会员中心 · 权益卡 3×3 网格）
 *
 * 【拍板记录 2026-09-23】§4.1 权益数据源缺失 → 采用方案 C 混合：
 *   - 权益文案/角标：当前为**前端占位文案池**（正式 9 项清单待产品拍板，类目未定先占位）；
 *   - 解锁态：**由前端按等级现算**（levelOf(growth).level >= unlockLevel），不落库，避免状态不一致；
 *   - 后端 tb_member_benefit 配置表就绪后，本数组字段即表契约（title/desc/unlockLevel/badge/sort），
 *     届时把 BENEFITS 换成接口下发即可，解锁态逻辑不动。
 *
 * ⚠️ 以下 9 项均为**占位文案**：命名避开了与「纯堂食+到店自提，不做外卖/配送」定位冲突的
 *    配送类权益（设计稿初稿里的「减4元配送费」「新品免4元配送费」已剔除）。
 *    正式清单确定后整表替换，勿在此表上增量打补丁。
 */
const BENEFITS = [{
		key: 'discount',
		title: '专属折扣',
		desc: '指定酒款会员价',
		unlockLevel: 1,
		badge: '',
		sort: 1,
		// 白色 glyph（绿圆底）/ 置灰 glyph（浅灰圆底）；空串=占位阶段无图标（画布缓存无对应源）
		icon: '/static/assets/icons/benefit-discount.png',
		iconOff: '/static/assets/icons/benefit-discount-off.png'
	},
	{
		key: 'winePriority',
		title: '优先取酒',
		desc: '存酒优先取出',
		unlockLevel: 1,
		badge: '',
		sort: 2,
		icon: '/static/assets/icons/benefit-wine.png',
		iconOff: '/static/assets/icons/benefit-wine-off.png'
	},
	{
		key: 'newTaste',
		title: '新品尝鲜',
		desc: '新品首发尝鲜价',
		unlockLevel: 2,
		badge: '',
		sort: 3,
		icon: '/static/assets/icons/benefit-newtaste.png',
		iconOff: '/static/assets/icons/benefit-newtaste-off.png'
	},
	{
		key: 'luckyDraw',
		title: '幸运抽奖',
		desc: '会员日抽奖机会',
		unlockLevel: 2,
		badge: '限时',
		sort: 4,
		icon: '/static/assets/icons/benefit-draw.png',
		iconOff: '/static/assets/icons/benefit-draw-off.png'
	},
	{
		key: 'birthday',
		title: '生日赠饮',
		desc: '生日月赠饮一杯',
		unlockLevel: 3,
		badge: '',
		sort: 5,
		icon: '/static/assets/icons/benefit-birthday.png',
		iconOff: '/static/assets/icons/benefit-birthday-off.png'
	},
	{
		key: 'doubleGrowth',
		title: '成长值翻倍',
		desc: '会员日成长值翻倍',
		unlockLevel: 3,
		badge: '限时',
		sort: 6,
		icon: '/static/assets/icons/benefit-growth.png',
		iconOff: '/static/assets/icons/benefit-growth-off.png'
	},
	{
		key: 'shareCoupon',
		title: '好友分享券',
		desc: '好友同行享赠券',
		unlockLevel: 4,
		badge: '',
		sort: 7,
		icon: '/static/assets/icons/benefit-share.png',
		iconOff: '/static/assets/icons/benefit-share-off.png'
	},
	{
		key: 'dineDiscount',
		title: '堂食 85 折',
		desc: '堂食消费 85 折',
		unlockLevel: 4,
		badge: '',
		sort: 8,
		icon: '',
		iconOff: ''
	},
	{
		key: 'butler',
		title: '专属客服',
		desc: '专属通道优先接待',
		unlockLevel: 5,
		badge: '',
		sort: 9,
		icon: '',
		iconOff: ''
	}
];

/**
 * 按当前等级计算解锁态
 * @param {Number} level 当前等级（member-level.js levelOf(growth).level，1~5）
 * @returns {Array} BENEFITS 按 sort 排序，每项附加 unlocked: Boolean
 */
const withUnlockState = (level) => {
	const current = Number(level) || 1;
	return BENEFITS
		.slice()
		.sort((a, b) => a.sort - b.sort)
		.map((item) => Object.assign({}, item, {
			unlocked: current >= item.unlockLevel
		}));
};

export default {
	BENEFITS,
	withUnlockState
};
