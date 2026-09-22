/**
 * 收银端侧栏导航与路由的单一事实源
 *
 * 结构（2026-09-22 按业务方要求重排）：
 *   **5 个「直属功能」直接挂一级**（高频作业动作，一点即达）：
 *     台位图 / 代客点单 / 出品进度 / 点单品沽清 / 自提核销
 *   **4 个「分组」**（组内为同类或低频项）：
 *     存酒管理 / 订单管理 / 打印管理 / 门店设置
 * ⇒ 一级 9 项（5 叶子 + 4 分组）/ 组内二级 13 项，合计 18 个功能页。
 *
 * 重排理由：高频动作（开台、点单、看出品、沽清、核销）原本埋在二级里，
 * 每次都要「展开一级 + 点二级」；提到一级后**一点即达**。
 */

export interface MenuLeaf {
  /** 菜单名（＝面包屑末级、页面标题） */
  label: string
  /** 路由路径（kebab-case 英文） */
  path: string
  /** 路由 name */
  name: string
  /** 对应画板编号（保留供追溯） */
  board: string
}

export interface MenuGroup {
  label: string
  children: MenuLeaf[]
  /** 打印管理一级带告警角标位（对位 C-侧栏 `2:99` 侧-打印角标） */
  badged?: boolean
}

/** 一级导航项：要么是「直属叶子」，要么是「分组」 */
export type MenuEntry = MenuLeaf | MenuGroup

export function isMenuGroup(e: MenuEntry): e is MenuGroup {
  return (e as MenuGroup).children !== undefined
}

export function isMenuLeaf(e: MenuEntry): e is MenuLeaf {
  return (e as MenuLeaf).path !== undefined
}

export const MENU: MenuEntry[] = [
  /* ===== 直属功能（一级，高频作业动作）===== */
  { label: '台位图', path: '/table/floor', name: 'table-floor', board: 'W-07' },
  { label: '代客点单', path: '/order/cashier', name: 'order-cashier', board: 'W-04' },
  { label: '出品进度', path: '/orders/production', name: 'orders-production', board: 'W-14' },
  { label: '点单品沽清', path: '/order/goods-status', name: 'order-goods-status', board: 'W-26' },
  { label: '自提核销', path: '/pickup/verify', name: 'pickup-verify', board: 'W-15' },

  /* ===== 分组 ===== */
  {
    label: '存酒管理',
    children: [
      { label: '存酒登记', path: '/wine/create', name: 'wine-create', board: 'W-17' },
      { label: '存酒列表', path: '/wine/list', name: 'wine-list', board: 'W-18' },
      { label: '临期提醒', path: '/wine/expiring', name: 'wine-expiring', board: 'W-19' }
    ]
  },
  {
    label: '订单管理',
    children: [
      { label: '今日订单', path: '/orders/today', name: 'orders-today', board: 'W-10' },
      { label: '订单处理', path: '/orders/handle', name: 'orders-handle', board: 'W-11' },
      // 核销记录本质是「自提订单的核销流水」→ 归订单管理（业务方给的单子里未指定，此为判断项，一行可改）
      { label: '核销记录', path: '/pickup/records', name: 'pickup-records', board: 'W-16' },
      { label: '售后与退单', path: '/orders/aftersales', name: 'orders-aftersales', board: 'W-13' }
    ]
  },
  {
    label: '打印管理',
    badged: true,
    children: [
      { label: '小票打印机', path: '/print/printers', name: 'print-printers', board: 'W-20' },
      { label: '打印队列与告警', path: '/print/queue', name: 'print-queue', board: 'W-25' },
      { label: '打印模板', path: '/print/templates', name: 'print-templates', board: 'W-21' },
      { label: '分单规则', path: '/print/routing', name: 'print-routing', board: 'W-22' }
    ]
  },
  {
    label: '门店设置',
    children: [
      { label: '营业设置', path: '/shop/settings', name: 'shop-settings', board: 'W-23' },
      { label: '账号与密码', path: '/shop/account', name: 'shop-account', board: 'W-24' }
    ]
  }
]

/**
 * 全部功能页扁平化（路由注册用）。
 * 带上 groupLabel：直属叶子的 groupLabel = 自身 label（面包屑/高亮用，避免出现空值）。
 */
export const MENU_LEAVES: Array<MenuLeaf & { groupLabel: string }> = MENU.flatMap(entry =>
  isMenuGroup(entry)
    ? entry.children.map(child => ({ ...child, groupLabel: entry.label }))
    : [{ ...entry, groupLabel: entry.label }]
)

/** 默认落地页＝台位图（收银台第一作业面） */
export const DEFAULT_PATH = '/table/floor'

/**
 * 按 path 反查菜单项。
 * `group` 为 null 表示该页是**直属一级叶子**（不属于任何分组）。
 */
export function findMenuByPath(path: string): { group: MenuGroup | null; leaf: MenuLeaf } | null {
  for (const entry of MENU) {
    if (isMenuGroup(entry)) {
      const leaf = entry.children.find(l => l.path === path)
      if (leaf) return { group: entry, leaf }
    } else if (entry.path === path) {
      return { group: null, leaf: entry }
    }
  }
  return null
}

/** 某 path 所属分组的 label（直属叶子返回 null；侧栏默认展开项用） */
export function groupLabelOfPath(path: string): string | null {
  return findMenuByPath(path)?.group?.label ?? null
}

/**
 * 画板 → 落地批次（对齐执行计划 §5）
 * A 外壳与地基已随本批次交付；B~H 为待落地批次，占位页据此提示。
 */
export const BATCH_OF_BOARD: Record<string, string> = {
  'W-04': 'B 点单主链',
  'W-26': 'B 点单主链',
  'W-07': 'C 台位',
  'W-08': 'C 台位',
  'W-10': 'D 订单与出品',
  'W-11': 'D 订单与出品',
  'W-12': 'D 订单与出品',
  'W-13': 'D 订单与出品',
  'W-14': 'D 订单与出品',
  'W-15': 'E 自提核销',
  'W-16': 'E 自提核销',
  'W-17': 'F 存酒',
  'W-18': 'F 存酒',
  'W-19': 'F 存酒',
  'W-20': 'G 打印',
  'W-21': 'G 打印',
  'W-22': 'G 打印',
  'W-25': 'G 打印',
  'W-23': 'G 打印与设置',
  'W-24': 'A 外壳与地基（已交付）'
}
