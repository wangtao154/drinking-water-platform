import { get, post, put } from '@/utils/request'
import type { PageQueryDTO, PageResult, OrderVO, OrderStatisticsVO, R } from '@/types/api'

const BASE = '/v1/orders'

export function pageOrders(params: PageQueryDTO & { orderStatus?: string; keyword?: string }) {
  return get<PageResult<OrderVO>>(`${BASE}`, params)
}
export function getOrder(orderNo: string) { return get<OrderVO>(`${BASE}/${orderNo}`) }
export function createOrder(data: any) { return post<OrderVO>(`${BASE}`, data) }
export function payOrder(orderNo: string) { return put<null>(`${BASE}/${orderNo}/pay`) }
export function cancelOrder(orderNo: string) { return put<null>(`${BASE}/${orderNo}/cancel`) }
export function getOrderStatistics() {
  return get<OrderStatisticsVO>(`${BASE}/statistics`)
}
