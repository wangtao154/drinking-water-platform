import { get, post, put, del } from '@/utils/request'
import type { PageQueryDTO, PageResult, SysPermissionVO, SysRoleVO, SysAccountVO, R } from '@/types/api'

const BASE = '/v1/system'

export interface AiAssistantConsentStatusVO {
  policyVersion: string
  policyTitle: string
  accepted: boolean
  acceptedAt?: string
  lastConfirmedAt?: string
}

export function getAiAssistantConsentStatus() {
  return get<AiAssistantConsentStatusVO>(`${BASE}/ai-consents/me`)
}

export function acceptAiAssistantConsent(data: { policyVersion: string; source: 'LOGIN' | 'ASSISTANT_PANEL' }) {
  return post<AiAssistantConsentStatusVO>(`${BASE}/ai-consents/accept`, data)
}

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
export function permissionTree() {
  return get<SysPermissionVO[]>(`${BASE}/permissions/tree`)
}
export function getRolePermissions(id: number) {
  return get<number[]>(`${BASE}/roles/${id}/permissions`)
}
export function updateRolePermissions(id: number, permissionIds: number[]) {
  return put<null>(`${BASE}/roles/${id}/permissions`, { permissionIds })
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
export function cancelCurrentUserAccount(data: { password: string; confirmText: string }) {
  return post<null>(`${BASE}/users/me/cancellation`, data)
}
export function resetPassword(id: number, password: string) {
  return put<null>(`${BASE}/users/${id}/reset-password`, { password })
}
export function updateUserIdentityVerification(id: number, verified: boolean) {
  return put<SysAccountVO>(`${BASE}/users/${id}/identity-verification`, { verified })
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

export function pageAuditLogs(params: PageQueryDTO & {
  operatorId?: number
  operation?: string
  startTime?: string
  endTime?: string
}) {
  return get<PageResult<AuditLogVO>>(`${BASE}/audit-logs`, params)
}

export interface AiAssistantAuditLogVO {
  id: number
  requestId: string
  operatorId: number
  operatorNameMasked?: string
  modelName?: string
  resultStatus: 'SUCCESS' | 'FALLBACK' | 'REJECTED' | 'ERROR'
  fallback: boolean
  toolNames?: string
  knowledgeDocumentIds?: string
  questionSummaryMasked?: string
  answerSummaryMasked?: string
  errorCode?: string
  errorSummaryMasked?: string
  accountCancelledAt?: string
  retentionUntil?: string
  createdAt?: string
}

export interface AiAssistantAuditIntegrityVO {
  status: 'VERIFIED' | 'LEGACY_UNSEALED' | 'FAILED'
  message: string
  totalRecords: number
  checkedRecords: number
  legacyUnsealedRecords: number
  firstProblemRecordId?: number
  firstProblemCreatedAt?: string
  verifiedAt?: string
}

export function pageAiAssistantAuditLogs(params: PageQueryDTO & {
  operatorId?: number
  modelName?: string
  resultStatus?: string
  startTime?: string
  endTime?: string
}) {
  return get<PageResult<AiAssistantAuditLogVO>>(`${BASE}/audit-logs/ai`, params)
}

export function getAiAssistantAuditIntegrity() {
  return get<AiAssistantAuditIntegrityVO>(`${BASE}/audit-logs/ai/integrity`)
}

export type AiAssistantComplaintCategory = 'CONTENT_QUALITY' | 'DATA_ISSUE' | 'SECURITY_PRIVACY' | 'MISUSE_REPORT'
export type AiAssistantComplaintStatus = 'PENDING' | 'PROCESSING' | 'RESOLVED' | 'REJECTED'

export interface AiAssistantComplaintVO {
  id: number
  complaintNo: string
  reporterId: number
  reporterName: string
  category: AiAssistantComplaintCategory
  content: string
  requestId?: string
  status: AiAssistantComplaintStatus
  handleReply?: string
  handlerId?: number
  handlerName?: string
  replyDueAt?: string
  handledAt?: string
  createdAt?: string
  retentionUntil?: string
}

export function createAiAssistantComplaint(data: {
  category: AiAssistantComplaintCategory
  content: string
  requestId?: string
}) {
  return post<AiAssistantComplaintVO>(`${BASE}/ai-complaints`, data)
}

export function pageAiAssistantComplaints(params: PageQueryDTO & {
  category?: AiAssistantComplaintCategory
  status?: AiAssistantComplaintStatus
  keyword?: string
  startTime?: string
  endTime?: string
}) {
  return get<PageResult<AiAssistantComplaintVO>>(`${BASE}/audit-logs/ai-complaints`, params)
}

export function handleAiAssistantComplaint(id: number, data: {
  status: AiAssistantComplaintStatus
  handleReply?: string
}) {
  return put<AiAssistantComplaintVO>(`${BASE}/audit-logs/ai-complaints/${id}`, data)
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
