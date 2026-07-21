import { get, post } from '@/utils/request'
import type { PushStatsVO, AlertVO, PageQueryDTO, PageResult, R } from '@/types/api'

const BASE = '/v1/push'

export function pageUnpushed(params: PageQueryDTO) {
  return get<PageResult<AlertVO>>(`${BASE}/unpushed`, params)
}
export function pushAlert(id: number) { return post<null>(`${BASE}/alerts/${id}`) }
export function batchPush() { return post<null>(`${BASE}/batch`) }
export function getPushStats() { return get<PushStatsVO>(`${BASE}/stats`) }
