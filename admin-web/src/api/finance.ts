import { get, post, put } from '@/utils/request'
import type { PageQueryDTO, PageResult, FinanceSummaryVO, SettlementVO, InvoiceVO, CommissionVO, OrderVO, R } from '@/types/api'
export type { FinanceSummaryVO, SettlementVO, InvoiceVO, CommissionVO } from '@/types/api'

const BASE = '/v1/finance'
const ORDER_BASE = '/v1/orders'

// ===== 财务汇总 =====
export function getFinanceSummary() {
  return get<FinanceSummaryVO>(`${BASE}/summary`)
}

// ===== 充值记录 (调用 order-service, 过滤 orderType=RECHARGE) =====
export function pageRechargeRecords(params: PageQueryDTO & { orderStatus?: string; keyword?: string }) {
  return get<PageResult<OrderVO>>(`${ORDER_BASE}`, { ...params, orderType: 'RECHARGE' })
}

// ===== 分润记录 =====
export function pageCommissions(params: PageQueryDTO & { status?: string }) {
  return get<PageResult<CommissionVO>>(`${BASE}/commissions`, params)
}

// ===== 结算账单 =====
export function pageSettlements(params: PageQueryDTO & { status?: string }) {
  return get<PageResult<SettlementVO>>(`${BASE}/settlements`, params)
}
export function createSettlement(data: any) { return post<SettlementVO>(`${BASE}/settlements`, data) }
export function settleSettlement(billNo: string) { return put<null>(`${BASE}/settlements/${billNo}/settle`) }

// ===== 发票 =====
export function pageInvoices(params: PageQueryDTO & { status?: string }) {
  return get<PageResult<InvoiceVO>>(`${BASE}/invoices`, params)
}
export function getInvoice(invoiceNo: string) { return get<InvoiceVO>(`${BASE}/invoices/${invoiceNo}`) }
export function createInvoice(data: any) { return post<InvoiceVO>(`${BASE}/invoices`, data) }
export function issueInvoice(invoiceNo: string) { return put<null>(`${BASE}/invoices/${invoiceNo}/issue`) }
