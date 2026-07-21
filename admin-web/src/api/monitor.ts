import { get, put } from '@/utils/request'
import type { PageQueryDTO, PageResult, AlertVO, AlertStatisticsVO, ThresholdVO, R } from '@/types/api'
export type { AlertVO, AlertStatisticsVO, ThresholdVO } from '@/types/api'

const BASE = '/v1/monitor'

export function pageAlerts(params: PageQueryDTO & { alertLevel?: string; handledStatus?: string }) {
  return get<PageResult<AlertVO>>(`${BASE}/alerts`, params)
}
export function getAlert(id: number) { return get<AlertVO>(`${BASE}/alerts/${id}`) }
export function handleAlert(id: number, data: { handleRemark: string }) {
  return put<null>(`${BASE}/alerts/${id}/handle`, data)
}
export function getAlertStatistics() {
  return get<AlertStatisticsVO>(`${BASE}/alerts/statistics`)
}
export function pageThresholds(params: PageQueryDTO) {
  return get<PageResult<ThresholdVO>>(`${BASE}/thresholds`, params)
}
export function updateThreshold(id: number, thresholdValue: number) {
  return put<null>(`${BASE}/thresholds/${id}`, { thresholdValue })
}
