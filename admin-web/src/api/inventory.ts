import { get, post } from '@/utils/request'
import type { PageQueryDTO, PageResult, DeviceStockVO, StockBatchVO, InventoryStatisticsVO, R } from '@/types/api'

const BASE = '/v1/inventory'

export function getInventoryStatistics() {
  return get<InventoryStatisticsVO>(`${BASE}/statistics`)
}
export function pageDeviceStocks(params: PageQueryDTO & { warehouseType?: string; status?: string }) {
  return get<PageResult<DeviceStockVO>>(`${BASE}/devices`, params)
}
export function inboundDevice(data: any) { return post<null>(`${BASE}/devices/inbound`, data) }
export function transferDevice(data: any) { return post<null>(`${BASE}/devices/transfer`, data) }

export function pageBatches(params: PageQueryDTO & { batchType?: string }) {
  return get<PageResult<StockBatchVO>>(`${BASE}/batches`, params)
}
export function createBatch(data: any) { return post<StockBatchVO>(`${BASE}/batches`, data) }
