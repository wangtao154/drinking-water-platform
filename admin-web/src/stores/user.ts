import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, getCurrentUser, logout as logoutApi } from '@/api/auth'
import { getToken, setToken, removeToken, getRefreshToken, setRefreshToken } from '@/utils/auth'
import type { LoginDTO, SysUserVO } from '@/types/api'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(getToken() || '')
  const refreshToken = ref<string>(getRefreshToken() || '')
  const userInfo = ref<SysUserVO | null>(null)
  const permissions = ref<string[]>([])
  const roles = ref<string[]>([])

  const isLogin = computed(() => !!token.value)
  const nickname = computed(() => userInfo.value?.nickname || userInfo.value?.account || '')

  async function login(dto: LoginDTO) {
    const res = await loginApi(dto)
    token.value = res.data.accessToken
    refreshToken.value = res.data.refreshToken
    userInfo.value = res.data.userInfo
    // 后端将 permissions 放在 userInfo 内部，不是顶层
    const info = (res.data.userInfo || {}) as any
    permissions.value = info.permissions || (res.data as any).permissions || []
    roles.value = info.roles || (res.data as any).roles || (info.roleCode ? [info.roleCode] : [])
    setToken(token.value)
    setRefreshToken(refreshToken.value)
    return res
  }

  async function fetchUserInfo() {
    const res = await getCurrentUser()
    if (res.data) {
      userInfo.value = res.data
      const info = res.data as any
      permissions.value = info.permissions || permissions.value
      roles.value = info.roles || (info.roleCode ? [info.roleCode] : roles.value)
    }
    return res
  }

  async function logout() {
    try {
      await logoutApi()
    } catch (e) {
      // 忽略登出 API 错误
    }
    resetState()
  }

  function resetState() {
    token.value = ''
    refreshToken.value = ''
    userInfo.value = null
    permissions.value = []
    roles.value = []
    removeToken()
  }

  function hasPermission(perm: string): boolean {
    if (permissions.value.includes('*') || roles.value.includes('SUPER_ADMIN')) return true
    return permissions.value.includes(perm)
  }

  function hasRole(role: string): boolean {
    return roles.value.includes(role)
  }

  return {
    token,
    refreshToken,
    userInfo,
    permissions,
    roles,
    isLogin,
    nickname,
    login,
    fetchUserInfo,
    logout,
    resetState,
    hasPermission,
    hasRole
  }
})
