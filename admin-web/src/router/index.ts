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

  // 首次加载，获取用户信息并注入动态路由
  if (!userStore.userInfo) {
    try {
      await userStore.fetchUserInfo()
      // 注入动态路由
      asyncRoutes.forEach(route => {
        if (!router.hasRoute(route.name!)) {
          router.addRoute(route)
        }
      })
      next({ ...to, replace: true })
      return
    } catch (error) {
      userStore.resetState()
      next(`/login?redirect=${to.path}`)
      return
    }
  }

  next()
})

router.afterEach(() => {
  NProgress.done()
})

export default router
