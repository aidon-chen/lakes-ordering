import { http } from './http'

/** 商品分类（菜单） */
export interface MenuRow {
  id: number
  name: string
  sortNumber: number
  isDisabled: number
}

/** 商品行（/rest/merchant/goods/list 返回） */
export interface GoodsRow {
  id: number
  name: string
  mainImage: string
  price: number
  salePrice: number
  monthlySales: number
  stock: number
  status: number // 2=已上架 3=下架 4=售罄
  menuId: number
  menuName: string
  specList: string // 商品规格 JSON
  description: string
}

/** 分页包装（MyBatis-Plus Page） */
export interface Page<T> {
  records: T[]
  total: number
  size: number
  current: number
}

/** 商品分类列表（不分页） */
export const fetchMenus = () =>
  http.post<unknown, Page<MenuRow>>('/rest/merchant/menu/list', { pageNo: -1, pageSize: 100 })

/**
 * 商品列表（按分类过滤，不分页）
 * goodsStatusIn2And4=true → 后端 GoodsMapper 过滤 g.status in (2,4)：
 * 只取「可出餐(2) + 已沽清(4)」，收银端据此显示并可一键恢复，待上架(1)/已下架(3) 归管理端 A-05。
 */
export const fetchGoods = (menuId?: number) =>
  http.post<unknown, Page<GoodsRow>>('/rest/merchant/goods/list', {
    pageNo: -1,
    pageSize: 100,
    menuId,
    goodsStatusIn2And4: true
  })

/**
 * 沽清 / 恢复出餐（M-07，D8 裁决：上下架＝当前能否出餐，归商家端）
 * - status=4 沽清（顾客端不可下单，后端 insertByMerchant 会拦截）
 * - status=2 恢复出餐
 * 后端 MerchantGoodsController.update 走 MP updateById，仅非空字段落库 → 只传 {id,status} 不会改动商品资料。
 */
export const updateGoodsStatus = (id: number, status: 2 | 4) =>
  http.post<unknown, null>('/rest/merchant/goods/update', { id, status })

/** 规格选项（/rest/goodsSpecificationOption/selectByGoodsId 返回 LinkedHashMap<规格名, 选项列表>） */
export interface SpecOption {
  id: number
  name: string
  /** 加价金额 */
  price: number
  /** 1=有货 2=无货 */
  stock: number
}

export type SpecGroupMap = Record<string, SpecOption[]>

/** 查询商品规格组（无规格时返回空对象） */
export const fetchSpecs = (goodsId: number) =>
  http.post<unknown, SpecGroupMap>('/rest/goodsSpecificationOption/selectByGoodsId', { goodsId })
