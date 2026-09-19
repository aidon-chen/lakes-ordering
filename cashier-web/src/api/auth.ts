import { http } from './http'

/** 商家登录（密码 Base64 编码，对齐后端 MerchantServiceImpl.login） */
export const login = (username: string, password: string) =>
  http.post<unknown, { token: string }>('/rest/merchant/login', {
    username,
    password: btoa(unescape(encodeURIComponent(password)))
  })
