import { get, post, put, del } from '@/utils/request'
import type { PageQueryDTO, PageResult, CustomerVO, DealerVO, WorkerVO, SysUserVO, R } from '@/types/api'

const BASE = '/v1'

// ===== 客户管理 =====
export function pageCustomers(params: PageQueryDTO & { customerType?: string; status?: string; phone?: string; keyword?: string }) {
  return get<PageResult<CustomerVO>>(`${BASE}/customers`, params)
}
export function getCustomer(id: number) { return get<CustomerVO>(`${BASE}/customers/${id}`) }
export function createCustomer(data: any) { return post<CustomerVO>(`${BASE}/customers`, data) }
export function updateCustomer(id: number, data: any) { return put<null>(`${BASE}/customers/${id}`, data) }
export function deleteCustomer(id: number) { return del<null>(`${BASE}/customers/${id}`) }
export function resetCustomerPassword(id: number) { return put<null>(`${BASE}/customers/${id}/reset-password`) }

// ===== 经销商管理 =====
export function pageDealers(params: PageQueryDTO) {
  return get<PageResult<DealerVO>>(`${BASE}/dealers`, params)
}
export function getDealer(id: number) { return get<DealerVO>(`${BASE}/dealers/${id}`) }
export function createDealer(data: any) { return post<DealerVO>(`${BASE}/dealers`, data) }
export function updateDealer(id: number, data: any) { return put<null>(`${BASE}/dealers/${id}`, data) }
export function deleteDealer(id: number) { return del<null>(`${BASE}/dealers/${id}`) }

// ===== 运维人员 =====
export function pageWorkers(params: PageQueryDTO) {
  return get<PageResult<WorkerVO>>(`${BASE}/workers`, params)
}
export function getWorker(id: number) { return get<WorkerVO>(`${BASE}/workers/${id}`) }
export function createWorker(data: any) { return post<WorkerVO>(`${BASE}/workers`, data) }
export function updateWorker(id: number, data: any) { return put<null>(`${BASE}/workers/${id}`, data) }
export function deleteWorker(id: number) { return del<null>(`${BASE}/workers/${id}`) }
export function resetWorkerPassword(id: number) { return put<null>(`${BASE}/workers/${id}/reset-password`) }

// ===== 系统用户 =====
export function pageSysUsers(params: PageQueryDTO) {
  return get<PageResult<SysUserVO>>(`${BASE}/sys-users`, params)
}
export function createSysUser(data: any) { return post<SysUserVO>(`${BASE}/sys-users`, data) }
export function updateSysUser(id: number, data: any) { return put<null>(`${BASE}/sys-users/${id}`, data) }
export function deleteSysUser(id: number) { return del<null>(`${BASE}/sys-users/${id}`) }
export function resetPassword(id: number) { return put<null>(`${BASE}/sys-users/${id}/reset-password`) }

// ===== 身份申请 =====
export function pageApplications(params: PageQueryDTO & { applyType?: string; status?: string; keyword?: string }) {
  return get<PageResult<any>>(`${BASE}/applications`, params)
}
export function getApplication(id: number) { return get<any>(`${BASE}/applications/${id}`) }
export function reviewApplication(id: number, data: { approved: boolean; reviewComment?: string }) {
  return put<any>(`${BASE}/applications/${id}/review`, data)
}
export function deleteApplication(id: number) { return del<null>(`${BASE}/applications/${id}`) }
