import { get, post } from '@/utils/request'
import type { PageQueryDTO, PageResult, ScanOrderStatsVO, ScanOrderVO } from '@/types/api'

const BASE = '/v1/water/scan-orders'

export function pageScanOrders(params: PageQueryDTO & {
  orderNo?: string
  sn?: string
  payStatus?: string
  dispenseStatus?: string
  commandStatus?: string
  refundStatus?: string
  paidStartTime?: string
  paidEndTime?: string
}) {
  return get<PageResult<ScanOrderVO>>(BASE, params)
}

export function getScanOrderStatistics(params?: {
  orderNo?: string
  sn?: string
  keyword?: string
  payStatus?: string
  dispenseStatus?: string
  commandStatus?: string
  refundStatus?: string
  paidStartTime?: string
  paidEndTime?: string
}) {
  return get<ScanOrderStatsVO>(`${BASE}/statistics`, params)
}

export function retryScanOrderRefund(orderNo: string) {
  return post<ScanOrderVO>(`${BASE}/${orderNo}/refund/retry`)
}
