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

/** 商品列表（按分类过滤，不分页） */
export const fetchGoods = (menuId?: number) =>
  http.post<unknown, Page<GoodsRow>>('/rest/merchant/goods/list', {
    pageNo: -1,
    pageSize: 100,
    menuId,
    status: 2
  })
