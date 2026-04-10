import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { getToken, getRefreshToken, setToken, removeToken } from './auth'
import { message } from 'ant-design-vue'

export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  traceId: string
}

export interface PageResult<T = any> {
  items: T[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

const service: AxiosInstance = axios.create({
  baseURL: '/api/v1',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
})

// Request interceptor
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    if (!config.headers['X-Trace-Id']) {
      config.headers['X-Trace-Id'] = crypto.randomUUID()
    }
    return config
  },
  (error) => Promise.reject(error),
)

// Response interceptor
let isRefreshing = false
let refreshSubscribers: Array<(token: string) => void> = []

service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data
    if (res.code !== 200 && res.code !== 201) {
      message.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message))
    }
    return response
  },
  async (error) => {
    const { response, config } = error
    if (!response) {
      message.error('网络异常，请检查网络连接')
      return Promise.reject(error)
    }
    switch (response.status) {
      case 401: {
        if (!isRefreshing) {
          isRefreshing = true
          try {
            const rt = getRefreshToken()
            const res = await axios.post('/api/v1/auth/refresh', { refresh_token: rt })
            const newToken = res.data.data.access_token
            setToken(newToken, rt!, res.data.data.expires_in)
            config.headers.Authorization = `Bearer ${newToken}`
            refreshSubscribers.forEach(cb => cb(newToken))
            refreshSubscribers = []
            return service(config)
          } catch {
            removeToken()
            window.location.href = '/login'
            return Promise.reject(error)
          } finally { isRefreshing = false }
        } else {
          return new Promise((resolve) => {
            refreshSubscribers.push((token: string) => {
              config.headers.Authorization = `Bearer ${token}`
              resolve(service(config))
            })
          })
        }
      }
      case 403: message.error('无权限访问'); break
      case 404: message.error('请求的资源不存在'); break
      case 429: message.warning('请求过于频繁'); break
      case 500: message.error('服务器内部错误'); break
      default: message.error(response.data?.message || '请求失败')
    }
    return Promise.reject(error)
  },
)

export default service
