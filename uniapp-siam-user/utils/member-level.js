/**
 * 会员等级规则（成长值口径）
 *
 * 依据《点餐消费系统-开发方案》§七「积分体系改会员成长值（不消费/不兑换，只判等级）」
 * 与画板 C-08（`21:660`）「距 V4 还 156」：
 *   当前成长值 844 → 落在 V3（≥600），距 V4（≥1000）还 156。
 *
 * ⚠️ 阈值目前是**本文件里的前端配置**，后端没有等级表。
 *    产品给出正式档位/名称后，只需改这里（或改成后端下发），C-01 / C-03 / C-08 三处会自动跟随。
 */
const LEVELS = [{
		level: 1,
		name: '青铜会员',
		min: 0
	},
	{
		level: 2,
		name: '白银会员',
		min: 300
	},
	{
		level: 3,
		name: '黄金会员',
		min: 600
	},
	{
		level: 4,
		name: '铂金会员',
		min: 1000
	},
	{
		level: 5,
		name: '钻石会员',
		min: 2000
	}
];

const toNumber = (v) => {
	const n = Number(v);
	return isFinite(n) ? n : 0;
};

/**
 * 按成长值算出等级信息
 * @param {Number} growth 累计成长值
 * @returns {Object} { level, name, min, nextLevel, nextName, nextMin, gap, percent }
 *   percent = 当前等级区间内的进度(0~100)；已是最高等级时 percent = 100
 */
const levelOf = (growth) => {
	const value = Math.max(0, toNumber(growth));
	let currentIndex = 0;
	for (let i = 0; i < LEVELS.length; i++) {
		if (value >= LEVELS[i].min) {
			currentIndex = i;
		}
	}
	const current = LEVELS[currentIndex];
	const next = LEVELS[currentIndex + 1] || null;

	let percent = 100;
	let gap = 0;
	if (next) {
		const span = next.min - current.min;
		percent = span > 0 ? Math.round(((value - current.min) / span) * 100) : 100;
		percent = Math.max(0, Math.min(100, percent));
		gap = Math.max(0, next.min - value);
	}

	return {
		level: current.level,
		name: current.name,
		min: current.min,
		nextLevel: next ? next.level : 0,
		nextName: next ? next.name : '',
		nextMin: next ? next.min : 0,
		gap: gap,
		percent: percent
	};
};

/** 等级短标：V1 ~ V5（画板胶囊用的是「V3」这种短标） */
const levelTag = (growth) => 'V' + levelOf(growth).level;

export default {
	LEVELS,
	levelOf,
	levelTag
};
