import { get, post } from '@/utils/request'
import type { TelemetryVO, R } from '@/types/api'

const BASE = '/v1/iot'

export function getLatestTelemetry(sn: string) {
  return get<TelemetryVO>(`${BASE}/devices/${sn}/latest`)
}

export function sendCommand(sn: string, data: { command: string; params?: any }) {
  return post<null>(`${BASE}/devices/${sn}/command`, data)
}

/** 下发 set 配置指令（写入设备寄存器） */
export function sendSetCommand(sn: string, pointID: string, value: string) {
  return post<any>(`${BASE}/devices/${sn}/set`, { pointID, value })
}

export function getOnlineStatus(sn: string) {
  return get<boolean>(`${BASE}/devices/${sn}/online-status`)
}

/** 查询历史遥测数据（曲线图） */
export function getHistoryTelemetry(sn: string, params: { range: string; fields?: string; interval?: string }) {
  return get<any>(`${BASE}/devices/${sn}/history`, params)
}
