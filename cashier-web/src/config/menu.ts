/**
 * 收银端侧栏导航与路由的单一事实源
 * 对齐 Ardot 商家端 file/726208852398101 · C-侧栏 `2:84`：**7 个一级 / 19 个二级**
 * 一级顺序即画板顺序：台位管理 / 点单管理 / 订单管理 / 自提核销 / 存酒管理 / 打印管理 / 门店设置
 */

export interface MenuLeaf {
  /** 二级菜单名（＝面包屑末级、页面标题） */
  label: string
  /** 路由路径（kebab-case 英文） */
  path: string
  /** 路由 name */
  name: string
  /** 对应画板编号（未落地页面在占位页展示，落地后保留供追溯） */
  board: string
}

export interface MenuGroup {
  label: string
  children: MenuLeaf[]
  /** 打印管理一级带告警角标位（对位 C-侧栏 `2:99` 侧-打印角标） */
  badged?: boolean
}

export const MENU: MenuGroup[] = [
  {
    label: '台位管理',
    children: [
      // W-08 台位类型与低消监控：需求书 V7 变更一取消低消与达标监控，
      // 台位类型配置按 M-05 划归管理端 A-09 → 收银端**整项移除**（不再挂死链菜单项）
      { label: '台位图', path: '/table/floor', name: 'table-floor', board: 'W-07' }
    ]
  },
  {
    label: '点单管理',
    children: [
      { label: '代客点单', path: '/order/cashier', name: 'order-cashier', board: 'W-04' },
      { label: '点单品沽清', path: '/order/goods-status', name: 'order-goods-status', board: 'W-26' }
    ]
  },
  {
    label: '订单管理',
    children: [
      { label: '今日订单', path: '/orders/today', name: 'orders-today', board: 'W-10' },
      { label: '订单处理', path: '/orders/handle', name: 'orders-handle', board: 'W-11' },
      { label: '出品进度', path: '/orders/production', name: 'orders-production', board: 'W-14' },
      { label: '售后与退单', path: '/orders/aftersales', name: 'orders-aftersales', board: 'W-13' }
    ]
  },
  {
    label: '自提核销',
    children: [
      { label: '自提核销', path: '/pickup/verify', name: 'pickup-verify', board: 'W-15' },
      { label: '核销记录', path: '/pickup/records', name: 'pickup-records', board: 'W-16' }
    ]
  },
  {
    label: '存酒管理',
    children: [
      { label: '存酒登记', path: '/wine/create', name: 'wine-create', board: 'W-17' },
      { label: '存酒列表', path: '/wine/list', name: 'wine-list', board: 'W-18' },
      { label: '临期提醒', path: '/wine/expiring', name: 'wine-expiring', board: 'W-19' }
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

/** 全部二级项扁平化（路由注册用） */
export const MENU_LEAVES: MenuLeaf[] = MENU.flatMap(g => g.children)

/** 默认落地页＝台位图（收银台第一作业面） */
export const DEFAULT_PATH = '/table/floor'

/** 按 path 反查一/二级（面包屑、侧栏高亮、占位页共用） */
export function findMenuByPath(path: string): { group: MenuGroup; leaf: MenuLeaf } | null {
  for (const group of MENU) {
    const leaf = group.children.find(l => l.path === path)
    if (leaf) return { group, leaf }
  }
  return null
}

/** 某 path 所属一级导航的 label（侧栏默认展开项用） */
export function groupLabelOfPath(path: string): string | null {
  return findMenuByPath(path)?.group.label ?? null
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
