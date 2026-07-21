import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { RouteRecordRaw } from 'vue-router'

export const usePermissionStore = defineStore('permission', () => {
  const routes = ref<RouteRecordRaw[]>([])
  const sidebarRoutes = ref<RouteRecordRaw[]>([])

  function setRoutes(dynamicRoutes: RouteRecordRaw[]) {
    routes.value = dynamicRoutes
    sidebarRoutes.value = dynamicRoutes
  }

  function hasPermission(roles: string[], route: RouteRecordRaw): boolean {
    if (route.meta && route.meta.roles) {
      return roles.some(role => (route.meta!.roles as string[]).includes(role))
    }
    return true
  }

  function filterRoutes(asyncRoutes: RouteRecordRaw[], roles: string[]): RouteRecordRaw[] {
    return asyncRoutes.filter(route => {
      if (hasPermission(roles, route)) {
        if (route.children) {
          route.children = filterRoutes(route.children, roles)
        }
        return true
      }
      return false
    })
  }

  function reset() {
    routes.value = []
    sidebarRoutes.value = []
  }

  return { routes, sidebarRoutes, setRoutes, filterRoutes, reset }
})
