import { get, post, put, del } from '@/utils/request'
import type { PageQueryDTO, PageResult, WorkOrderVO, WorkOrderStatisticsVO, R } from '@/types/api'

const BASE = '/v1/work-orders'

export function pageWorkOrders(params: PageQueryDTO & { orderStatus?: string; orderType?: string; workerId?: number }) {
  return get<PageResult<WorkOrderVO>>(`${BASE}`, params)
}
export function getWorkOrder(id: number) { return get<WorkOrderVO>(`${BASE}/${id}`) }
export function getWorkOrderDetail(id: number) { return get<WorkOrderVO>(`${BASE}/${id}`) }
export function createWorkOrder(data: any) { return post<WorkOrderVO>(`${BASE}`, data) }
export function dispatchWorkOrder(id: number | string, workerId: number | string) {
  return put<null>(`${BASE}/${id}/dispatch`, { workerId })
}
export function updateWorkOrderStatus(id: number, status: string) {
  return put<null>(`${BASE}/${id}/status`, { orderStatus: status })
}
export function getWorkOrderStatistics() {
  return get<WorkOrderStatisticsVO>(`${BASE}/statistics`)
}
export function deleteWorkOrder(id: number) { return del<null>(`${BASE}/${id}`) }
