import axios from 'axios'
import { ElMessage } from 'element-plus'
import { AUTH_TOKEN_KEY, AUTH_USER_KEY } from '../constants/app'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem(AUTH_TOKEN_KEY)
  if (token && !config.headers.Authorization) config.headers.Authorization = `Bearer ${token}`
  return config
})

request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const status = error.response?.status
    const payload = error.response?.data ?? error
    if (payload && typeof payload === 'object') payload.__httpStatus = status
    const skipErrorNotification = error.config?.skipErrorNotification === true
    let notified = false
    if (status === 401 && error.config?.preserveAuthOnUnauthorized !== true) {
      localStorage.removeItem(AUTH_TOKEN_KEY)
      localStorage.removeItem(AUTH_USER_KEY)
      sessionStorage.removeItem(AUTH_TOKEN_KEY)
      sessionStorage.removeItem(AUTH_USER_KEY)
      if (!['/login', '/register'].includes(window.location.pathname)) window.location.assign('/login')
    } else if (status === 403 && !skipErrorNotification) {
      ElMessage.error(payload?.message || '没有权限执行此操作')
      notified = true
    } else if (!error.response && !skipErrorNotification) {
      ElMessage.error('网络连接失败，请确认后端服务已启动')
      notified = true
    } else if (status !== 403 && status !== 401 && !skipErrorNotification) {
      ElMessage.error(payload?.message || (status >= 500 ? '服务器暂时不可用，请稍后重试' : '请求失败'))
      notified = true
    }
    if (notified && payload && typeof payload === 'object') payload.__notified = true
    return Promise.reject(payload)
  },
)

export default request
