import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import NProgress from 'nprogress'
import { getToken } from './auth'
import { useUserStore } from '@/stores/user'
import router from '@/router'
import type { R, PageResult } from '@/types/api'

NProgress.configure({ showSpinner: false })

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' }
})

function getRequestToken(config: any): string | null {
  const headers = config?.headers
  const authorization =
    headers?.Authorization ||
    headers?.authorization ||
    (typeof headers?.get === 'function' ? headers.get('Authorization') : null)

  if (typeof authorization !== 'string') return null
  const prefix = 'Bearer '
  return authorization.startsWith(prefix) ? authorization.slice(prefix.length) : null
}

function shouldHandleAuthFailure(config: any): boolean {
  const requestToken = getRequestToken(config)
  const currentToken = getToken()
  return !requestToken || !currentToken || requestToken === currentToken
}

function redirectToLogin(): void {
  const userStore = useUserStore()
  userStore.resetState()

  const currentRoute = router.currentRoute.value
  if (currentRoute.path !== '/login') {
    router.replace(`/login?redirect=${encodeURIComponent(currentRoute.fullPath)}`)
  }
}

async function resolveResponseErrorMessage(data: unknown): Promise<string | undefined> {
  if (data instanceof Blob) {
    try {
      const body = JSON.parse(await data.text())
      return typeof body?.message === 'string' ? body.message : undefined
    } catch {
      return undefined
    }
  }
  return typeof (data as any)?.message === 'string' ? (data as any).message : undefined
}

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

    // 文件下载（blob）直接返回原始响应
    if (response.config.responseType === 'blob') {
      return response
    }

    const res = response.data as R
    if (res.code === 200) {
      // 后端 Long 类型会序列化为字符串，分页字段需要转换为 number 供组件使用。
      if (res.data && Array.isArray((res.data as any).records)) {
        const pageData = res.data as any
        if (pageData.total != null) pageData.total = Number(pageData.total) || 0
        if (pageData.current != null) pageData.current = Number(pageData.current) || 0
        if (pageData.pages != null) pageData.pages = Number(pageData.pages) || 0
        if (pageData.size != null) pageData.size = Number(pageData.size) || 0
      }
      return res
    }

    // Token 过期。若这是旧 token 的滞后响应，不清理当前新登录状态。
    if (res.code === 40100) {
      if (!shouldHandleAuthFailure(response.config)) {
        return Promise.reject(new Error('stale token expired'))
      }

      ElMessageBox.confirm('登录状态已过期，请重新登录', '提示', {
        confirmButtonText: '重新登录',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        redirectToLogin()
      })
      return Promise.reject(new Error('token expired'))
    }

    if (res.code === 40300) {
      ElMessage.error('无权限访问')
      return Promise.reject(new Error('forbidden'))
    }

    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || 'Error'))
  },
  async (error) => {
    NProgress.done()
    if (error.response) {
      const status = error.response.status
      const serverMessage = await resolveResponseErrorMessage(error.response.data)
      if (status === 401) {
        // 旧 token 的并发请求可能晚于新登录返回，不能让它清掉刚写入的新 token。
        if (!shouldHandleAuthFailure(error.config)) {
          return Promise.reject(error)
        }

        ElMessage.error('登录已过期，请重新登录')
        redirectToLogin()
      } else if (status === 403) {
        ElMessage.error('无权限访问')
      } else if (status === 404) {
        ElMessage.error('请求资源不存在')
      } else if (serverMessage) {
        ElMessage.error(serverMessage)
      } else if (status >= 500) {
        ElMessage.error('服务器内部错误')
      } else {
        ElMessage.error('请求失败')
      }
    } else if (error.message?.includes('timeout')) {
      ElMessage.error('请求超时，请稍后重试')
    } else {
      ElMessage.error('网络异常，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

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

export async function page<T = any>(url: string, params: any): Promise<R<PageResult<T>>> {
  const res = await get<PageResult<T>>(url, params)
  if (res.data) {
    res.data.total = Number(res.data.total) || 0
    res.data.current = Number(res.data.current) || 0
    res.data.pages = Number(res.data.pages) || 0
    if (res.data.size != null) res.data.size = Number(res.data.size) || 0
  }
  return res
}

export default service
