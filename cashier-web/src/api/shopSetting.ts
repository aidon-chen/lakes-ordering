import { http } from './http'

/**
 * 门店营业设置（商家侧 /rest/merchant/shopSetting/**，批次 G · W-23）
 *
 * 服务端存**键值对**（tb_shop_setting），值统一是**字符串**：
 * 布尔用 "true"/"false"、数字用 "70"，由前端负责解析与回填默认值。
 * ⚠️ 门店维度由后端用登录会话的 shopId 决定，前端传了也没用。
 */

/** 已知设置键（与后端 ShopSetting 的常量一一对应） */
export const SHOP_SETTING_KEYS = [
  'businessOpen',
  'businessOpenChangedAt',
  'orderVoiceAlert',
  'voiceVolume',
  'autoAccept',
  'productionTimeoutMin',
  'cloudPrint',
  'localPrint',
  'printMode',
  'defaultPrinterId'
] as const

export type ShopSettingKey = (typeof SHOP_SETTING_KEYS)[number]

/** 读出门店全部设置（缺失的键不会出现在返回里） */
export const fetchShopSettings = () =>
  http.post<unknown, Record<string, string>>('/rest/merchant/shopSetting/list', {})

/** 批量保存（upsert）；只写传入的键，未传的键保持不变 */
export const saveShopSettings = (settings: Record<string, string>) =>
  http.post<unknown, Record<string, string>>('/rest/merchant/shopSetting/save', settings)
