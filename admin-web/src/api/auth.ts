import { get, post } from '@/utils/request'
import type { LoginDTO, LoginVO, SysUserVO, R } from '@/types/api'

const BASE = '/v1/auth'

export function login(data: LoginDTO) {
  return post<LoginVO>(`${BASE}/login`, data)
}

export function getCurrentUser() {
  return get<SysUserVO>(`${BASE}/current-user`)
}

export function logout() {
  return post<null>(`${BASE}/logout`)
}

export function changePassword(data: { oldPassword: string; newPassword: string }) {
  return post<null>(`${BASE}/change-password`, data)
}

export function refreshToken(refreshToken: string) {
  return post<LoginVO>(`${BASE}/refresh`, { refreshToken })
}
