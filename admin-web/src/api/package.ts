import { get, post, put, del } from '@/utils/request'
import type { PageQueryDTO, PageResult, PackageVO, R } from '@/types/api'

const BASE = '/v1/packages'

export function pagePackages(params: PageQueryDTO & { packageType?: string; status?: string }) {
  return get<PageResult<PackageVO>>(`${BASE}`, params)
}
export function getPackage(id: number) { return get<PackageVO>(`${BASE}/${id}`) }
export function createPackage(data: any) { return post<PackageVO>(`${BASE}`, data) }
export function updatePackage(id: number, data: any) { return put<null>(`${BASE}/${id}`, data) }
export function listActivePackages() { return get<PackageVO[]>(`${BASE}/active`) }
export function onlinePackage(id: number) { return put<null>(`${BASE}/${id}/online`) }
export function offlinePackage(id: number) { return put<null>(`${BASE}/${id}/offline`) }
export function deletePackage(id: number) { return del<null>(`${BASE}/${id}`) }
