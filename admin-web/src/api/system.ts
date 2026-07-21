import { get, post, put, del } from '@/utils/request'
import type { PageQueryDTO, PageResult, SysRoleVO, SysAccountVO, R } from '@/types/api'

const BASE = '/v1/system'

// ===== 角色管理 =====
export function pageRoles(params: PageQueryDTO & { keyword?: string }) {
  return get<PageResult<SysRoleVO>>(`${BASE}/roles`, params)
}
export function listAllRoles() {
  return get<SysRoleVO[]>(`${BASE}/roles/list`)
}
export function createRole(data: { roleCode: string; roleName: string; roleDesc?: string }) {
  return post<SysRoleVO>(`${BASE}/roles`, data)
}
export function updateRole(id: number, data: { roleCode: string; roleName: string; roleDesc?: string }) {
  return put<SysRoleVO>(`${BASE}/roles/${id}`, data)
}
export function deleteRole(id: number) {
  return del<null>(`${BASE}/roles/${id}`)
}

// ===== 账户管理 =====
export function pageUsers(params: PageQueryDTO & { keyword?: string }) {
  return get<PageResult<SysAccountVO>>(`${BASE}/users`, params)
}
export function createUser(data: {
  employeeNo: string
  username: string
  password: string
  roleId?: number
  name?: string
  phone?: string
  email?: string
  department?: string
}) {
  return post<SysAccountVO>(`${BASE}/users`, data)
}
export function updateUser(id: number, data: {
  employeeNo?: string
  username?: string
  password?: string
  roleId?: number
  name?: string
  phone?: string
  email?: string
  department?: string
}) {
  return put<SysAccountVO>(`${BASE}/users/${id}`, data)
}
export function deleteUser(id: number) {
  return del<null>(`${BASE}/users/${id}`)
}
export function resetPassword(id: number, password: string) {
  return put<null>(`${BASE}/users/${id}/reset-password`, { password })
}

// ===== 审计日志 =====
export interface AuditLogVO {
  id?: number
  username?: string
  operation?: string
  method?: string
  params?: string
  ip?: string
  duration?: number
  createdAt?: string
}

export function pageAuditLogs(params: PageQueryDTO) {
  return get<PageResult<AuditLogVO>>(`${BASE}/audit-logs`, params)
}

// ===== 系统配置 =====
export interface SysConfigVO {
  id?: number
  configKey?: string
  configValue?: string
  description?: string
  type?: string
  updatedAt?: string
}

export function pageConfigs(params: PageQueryDTO) {
  return get<PageResult<SysConfigVO>>(`${BASE}/configs`, params)
}

export function updateConfig(id: number, data: { configValue: string; description?: string }) {
  return put<SysConfigVO>(`${BASE}/configs/${id}`, data)
}

// ===== MQTT 配置管理 =====
export interface MqttConfigVO {
  id?: number
  name?: string
  broker?: string
  clientId?: string
  username?: string
  password?: string
  keepAliveInterval?: number
  cleanSession?: boolean
  enabled?: boolean
  createdAt?: string
  updatedAt?: string
  runningStatus?: string  // 运行/停止/未知
  linkStatus?: string     // 连接/断开/未知
}

export function getMqttConfig() {
  return get<MqttConfigVO>(`${BASE}/mqtt-config`)
}

export function updateMqttConfig(data: MqttConfigVO) {
  return put<MqttConfigVO>(`${BASE}/mqtt-config`, data)
}

export function testMqttConnection(data: MqttConfigVO) {
  return post<{ success: boolean; message: string }>(`${BASE}/mqtt-config/test`, data)
}
