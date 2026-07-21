import { get, post, put, del } from '@/utils/request'
import type { PageQueryDTO, PageResult, DeviceModelVO, DeviceVO, DeviceRegisterDTO, R } from '@/types/api'

const BASE = '/v1'

// ===== 设备型号 =====
export function pageDeviceModels(params: PageQueryDTO) {
  return get<PageResult<DeviceModelVO>>(`${BASE}/device-models`, params)
}
export function listDeviceModels() { return get<DeviceModelVO[]>(`${BASE}/device-models`) }
export function getDeviceModel(id: number) { return get<DeviceModelVO>(`${BASE}/device-models/${id}`) }
export function createDeviceModel(data: any) { return post<DeviceModelVO>(`${BASE}/device-models`, data) }
export function updateDeviceModel(id: number, data: any) { return put<null>(`${BASE}/device-models/${id}`, data) }
export function deleteDeviceModel(id: number) { return del<null>(`${BASE}/device-models/${id}`) }

// ===== 设备实例 =====
export function pageDevices(params: PageQueryDTO & { onlineStatus?: number; lifecycleStatus?: string; keyword?: string }) {
  return get<PageResult<DeviceVO>>(`${BASE}/devices`, params)
}
export function getDevice(deviceId: string) { return get<DeviceVO>(`${BASE}/devices/${deviceId}`) }
export function registerDevice(data: DeviceRegisterDTO) { return post<DeviceVO>(`${BASE}/devices`, data) }
export function updateDevice(deviceId: string, data: any) { return put<null>(`${BASE}/devices/${deviceId}`, data) }
export function deleteDevice(deviceId: string) { return del<null>(`${BASE}/devices/${deviceId}`) }
export function unbindDevice(deviceId: string) { return post<null>(`${BASE}/devices/unbind/${deviceId}`) }
