import { get, post, put } from '@/utils/request'
import type { PageQueryDTO, PageResult, PaymentVO, RefundVO, R } from '@/types/api'
export type { PaymentVO, RefundVO } from '@/types/api'

const BASE = '/v1/payments'

// ===== 支付记录 =====
export function pagePayments(params: PageQueryDTO & { orderNo?: string; payStatus?: string }) {
  return get<PageResult<PaymentVO>>(`${BASE}`, params)
}
export function getPayment(orderNo: string) { return get<PaymentVO>(`${BASE}/${orderNo}`) }
export function createPayment(data: any) { return post<PaymentVO>(`${BASE}`, data) }

// ===== 退款 =====
export function pageRefunds(params: PageQueryDTO & { orderNo?: string; refundStatus?: string }) {
  return get<PageResult<RefundVO>>(`${BASE}/refunds`, params)
}
export function createRefund(data: any) { return post<RefundVO>(`${BASE}/refunds`, data) }
export function approveRefund(refundNo: string) { return put<null>(`${BASE}/refunds/${refundNo}/approve`) }
export function rejectRefund(refundNo: string, reason: string) {
  return put<null>(`${BASE}/refunds/${refundNo}/reject`, { reason })
}
