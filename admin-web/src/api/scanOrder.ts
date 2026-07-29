import { get } from '@/utils/request'
import type { PageQueryDTO, PageResult, ScanOrderStatsVO, ScanOrderVO } from '@/types/api'

const BASE = '/v1/water/scan-orders'

export function pageScanOrders(params: PageQueryDTO & {
  orderNo?: string
  sn?: string
  payStatus?: string
  dispenseStatus?: string
  commandStatus?: string
}) {
  return get<PageResult<ScanOrderVO>>(BASE, params)
}

export function getScanOrderStatistics() {
  return get<ScanOrderStatsVO>(`${BASE}/statistics`)
}
