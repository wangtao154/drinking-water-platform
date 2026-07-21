import { get } from '@/utils/request'
import type { DashboardVO, ReportDeviceVO, ReportOrderVO, ReportFinanceVO, ReportWorkerVO, FlowReportVO, R } from '@/types/api'

export type { DashboardVO, ReportDeviceVO, ReportOrderVO, ReportFinanceVO, ReportWorkerVO, FlowReportVO, R }

const BASE = '/v1/reports'

export function getDashboard() {
  return get<DashboardVO>(`${BASE}/dashboard`)
}
export function getDeviceReport() {
  return get<ReportDeviceVO>(`${BASE}/devices`)
}
export function getOrderReport() {
  return get<ReportOrderVO>(`${BASE}/orders`)
}
export function getFinanceReport() {
  return get<ReportFinanceVO>(`${BASE}/finance`)
}
export function getWorkerReport() {
  return get<ReportWorkerVO>(`${BASE}/workers`)
}
export function getFlowReport() {
  return get<FlowReportVO>(`${BASE}/flow`)
}
