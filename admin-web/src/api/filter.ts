import { get, post, put, del } from '@/utils/request'
import type { PageQueryDTO, PageResult, FilterModelVO, FilterInstanceVO, R } from '@/types/api'

const BASE = '/v1'

// ===== 滤芯型号 =====
export function pageFilterModels(params: PageQueryDTO) {
  return get<PageResult<FilterModelVO>>(`${BASE}/filter-models`, params)
}
export function getFilterModel(id: number) { return get<FilterModelVO>(`${BASE}/filter-models/${id}`) }
export function createFilterModel(data: any) { return post<FilterModelVO>(`${BASE}/filter-models`, data) }
export function updateFilterModel(id: number, data: any) { return put<FilterModelVO>(`${BASE}/filter-models/${id}`, data) }
export function deleteFilterModel(id: number) { return del<null>(`${BASE}/filter-models/${id}`) }
export function listAllEnabledFilterModels() { return get<FilterModelVO[]>(`${BASE}/filter-models/enabled`) }

// ===== 滤芯实例 =====
export function pageFilters(params: PageQueryDTO & {
  status?: string
  modelId?: number | string
  lifecycleStatus?: string
  currentDeviceId?: string
  createdAtStart?: string
  createdAtEnd?: string
  installedAtStart?: string
  installedAtEnd?: string
}) {
  return get<PageResult<FilterInstanceVO>>(`${BASE}/filters`, params)
}
export function getFilter(filterId: string) { return get<FilterInstanceVO>(`${BASE}/filters/${filterId}`) }
export function registerFilter(data: any) {
  return post<FilterInstanceVO>(`${BASE}/filters/register`, data)
}
export function updateFilter(filterId: string, data: any) {
  return put<FilterInstanceVO>(`${BASE}/filters/${filterId}`, data)
}
export function deleteFilter(filterId: string) {
  return del<null>(`${BASE}/filters/${filterId}`)
}
export function traceFilter(filterId: string) {
  return get<any>(`${BASE}/filters/${filterId}/trace`)
}
