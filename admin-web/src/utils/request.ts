import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import NProgress from 'nprogress'
import { getToken, removeToken } from './auth'
import { useUserStore } from '@/stores/user'
import router from '@/router'
import type { R, PageResult } from '@/types/api'

NProgress.configure({ showSpinner: false })

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' }
})

// 请求拦截
service.interceptors.request.use(
  (config) => {
    NProgress.start()
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    NProgress.done()
    return Promise.reject(error)
  }
)

// 响应拦截
service.interceptors.response.use(
  (response): any => {
    NProgress.done()
    const res = response.data as R

    // 如果是文件下载（blob），直接返回
    if (response.config.responseType === 'blob') {
      return response
    }

    if (res.code === 200) {
      // 后端 Long 类型通过 ToStringSerializer 序列化为字符串
      // PageResult 的 total/current/pages 等字段需要转为 number 供前端组件使用
      if (res.data && Array.isArray((res.data as any).records)) {
        const pageData = res.data as any
        if (pageData.total != null) pageData.total = Number(pageData.total) || 0
        if (pageData.current != null) pageData.current = Number(pageData.current) || 0
        if (pageData.pages != null) pageData.pages = Number(pageData.pages) || 0
        if (pageData.size != null) pageData.size = Number(pageData.size) || 0
      }
      return res
    }

    // Token 过期
    if (res.code === 40100) {
      ElMessageBox.confirm('登录状态已过期，请重新登录', '提示', {
        confirmButtonText: '重新登录',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        const userStore = useUserStore()
        userStore.resetState()
        removeToken()
        router.push('/login')
      })
      return Promise.reject(new Error('token expired'))
    }

    // 无权限
    if (res.code === 40300) {
      ElMessage.error('无权限访问')
      return Promise.reject(new Error('forbidden'))
    }

    // 其他业务错误
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || 'Error'))
  },
  (error) => {
    NProgress.done()
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        ElMessage.error('登录已过期，请重新登录')
        removeToken()
        router.push('/login')
      } else if (status === 403) {
        ElMessage.error('无权限访问')
      } else if (status === 404) {
        ElMessage.error('请求资源不存在')
      } else if (status >= 500) {
        ElMessage.error('服务器内部错误')
      } else {
        ElMessage.error(error.response.data?.message || '请求失败')
      }
    } else if (error.message?.includes('timeout')) {
      ElMessage.error('请求超时，请稍后重试')
    } else {
      ElMessage.error('网络异常，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

// 封装请求方法
export function request<T = any>(config: AxiosRequestConfig): Promise<R<T>> {
  return service(config) as unknown as Promise<R<T>>
}

export function get<T = any>(url: string, params?: any): Promise<R<T>> {
  return request<T>({ method: 'GET', url, params })
}

export function post<T = any>(url: string, data?: any): Promise<R<T>> {
  return request<T>({ method: 'POST', url, data })
}

export function put<T = any>(url: string, data?: any): Promise<R<T>> {
  return request<T>({ method: 'PUT', url, data })
}

export function del<T = any>(url: string, params?: any): Promise<R<T>> {
  return request<T>({ method: 'DELETE', url, params })
}

// 分页请求封装
export async function page<T = any>(url: string, params: any): Promise<R<PageResult<T>>> {
  const res = await get<PageResult<T>>(url, params)
  // 后端 Long 类型通过 ToStringSerializer 序列化为字符串，需转为 number 供前端使用
  if (res.data) {
    res.data.total = Number(res.data.total) || 0
    res.data.current = Number(res.data.current) || 0
    res.data.pages = Number(res.data.pages) || 0
    if (res.data.size != null) res.data.size = Number(res.data.size) || 0
  }
  return res
}

export default service
