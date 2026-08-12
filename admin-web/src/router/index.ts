import { createRouter, createWebHistory } from 'vue-router'
import NProgress from 'nprogress'
import { constantRoutes, asyncRoutes } from './routes'
import { useUserStore } from '@/stores/user'
import { getToken } from '@/utils/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes
})

NProgress.configure({ showSpinner: false })

const whiteList = ['/login', '/404', '/403']

let dynamicRoutesReady = false

function ensureDynamicRoutes() {
  if (dynamicRoutesReady) return

  asyncRoutes.forEach(route => {
    router.addRoute(route)
  })
  dynamicRoutesReady = true
}

router.beforeEach(async (to, _from, next) => {
  NProgress.start()
  document.title = `${to.meta.title || ''} - 直饮水平台管理后台`

  const token = getToken()

  if (!token) {
    if (whiteList.includes(to.path)) {
      next()
    } else {
      next(`/login?redirect=${to.path}`)
    }
    return
  }

  // 已登录
  if (to.path === '/login') {
    next('/')
    return
  }

  const userStore = useUserStore()

  try {
    // 用户信息和动态路由是两个独立状态，不能用 userInfo 代替路由就绪状态。
    if (!userStore.userInfo) {
      await userStore.fetchUserInfo()
    }

    if (!dynamicRoutesReady) {
      ensureDynamicRoutes()
      next({ ...to, replace: true })
      return
    }
  } catch (error) {
    console.error('route initialization failed', error)
    userStore.resetState()
    next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
    return
  }

  next()
})

router.afterEach((to) => {
  NProgress.done()
  sessionStorage.removeItem(`route-chunk-reload:${to.fullPath}`)
})

router.onError((error, to) => {
  NProgress.done()
  console.error('router navigation error', error)

  // 发布新版本后，旧页面可能仍引用已经失效的分包文件。仅自动刷新一次，避免死循环。
  const message = String(error?.message || error)
  const isChunkLoadError = /Failed to fetch dynamically imported module|Importing a module script failed|Loading chunk .* failed/i.test(message)
  if (!isChunkLoadError) return

  const reloadKey = `route-chunk-reload:${to.fullPath}`
  if (sessionStorage.getItem(reloadKey)) {
    sessionStorage.removeItem(reloadKey)
    return
  }

  sessionStorage.setItem(reloadKey, '1')
  window.location.replace(to.fullPath)
})

export default router
