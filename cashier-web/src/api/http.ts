import axios from 'axios'

/**
 * siam-server 接口层
 * - 本地开发（vite dev）：/rest 经 vite proxy 转发到 localhost:9200
 * - 生产构建（vite build）：直连云托管后端 bar-mvp 公网域名（静态托管无 proxy）
 * 响应信封：{ code: 0, msg, data }（BasicResult）
 */
const API_BASE = import.meta.env.PROD
  ? 'https://bar-mvp-315579-12-1488393615.sh.run.tcloudbase.com/siam-server'
  : '/'

export const http = axios.create({
  baseURL: API_BASE,
  timeout: 15000
})

// 请求拦截：注入商家 token（登录后写入 localStorage）
http.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers['token'] = token
  return config
})

// 响应拦截：BasicResult 解包；code!=0 抛业务错误
http.interceptors.response.use(
  resp => {
    const body = resp.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code !== 0) return Promise.reject(new Error(body.msg || '业务处理失败'))
      return body.data
    }
    return body
  },
  err => Promise.reject(err)
)
