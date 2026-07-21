import dayjs from 'dayjs'

/** 金额：分转元 */
export function fenToYuan(fen: number | string | undefined | null): string {
  if (fen === undefined || fen === null || fen === '') return '0.00'
  const num = Number(fen)
  if (isNaN(num)) return '0.00'
  return (num / 100).toFixed(2)
}

/** 金额：元转分 */
export function yuanToFen(yuan: number | string): number {
  const num = Number(yuan)
  if (isNaN(num)) return 0
  return Math.round(num * 100)
}

/** 格式化日期时间 */
export function formatDateTime(date: string | number | Date | undefined, fmt = 'YYYY-MM-DD HH:mm:ss'): string {
  if (!date) return '-'
  return dayjs(date).format(fmt)
}

/** 格式化日期 */
export function formatDate(date: string | number | Date | undefined): string {
  return formatDateTime(date, 'YYYY-MM-DD')
}

/** 相对时间 */
export function formatRelative(date: string | number | Date | undefined): string {
  if (!date) return '-'
  const diff = Date.now() - new Date(date).getTime()
  const min = Math.floor(diff / 60000)
  const hour = Math.floor(diff / 3600000)
  const day = Math.floor(diff / 86400000)
  if (min < 1) return '刚刚'
  if (min < 60) return `${min}分钟前`
  if (hour < 24) return `${hour}小时前`
  if (day < 30) return `${day}天前`
  return formatDate(date)
}

/** 状态标签颜色映射 */
export function statusTagType(status: string): 'success' | 'warning' | 'danger' | 'info' | 'primary' {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info' | 'primary'> = {
    ONLINE: 'success',
    ACTIVE: 'success',
    ENABLED: 'success',
    PAID: 'success',
    COMPLETED: 'success',
    VERIFIED: 'success',
    APPROVED: 'success',
    ISSUED: 'success',
    SETTLED: 'success',
    IN_STOCK: 'primary',
    IN_USE: 'success',
    OFFLINE: 'info',
    DISABLED: 'info',
    CANCELLED: 'info',
    DELETED: 'info',
    PENDING: 'warning',
    PENDING_INSTALL: 'warning',
    DISPATCHED: 'warning',
    ACCEPTED: 'warning',
    IN_PROGRESS: 'warning',
    REFUNDING: 'warning',
    FAULT: 'danger',
    REJECTED: 'danger',
    SCRAPPED: 'danger',
    REGISTERED: 'primary',
    ALLOCATED: 'primary',
    ACTIVATED_ONLINE: 'success',
    ACTIVATED_OFFLINE: 'success',
    UNHANDLED: 'danger',
    HANDLED: 'success',
    CRITICAL: 'danger',
    WARNING: 'warning'
  }
  return map[status] || 'info'
}

/** 状态中文标签 */
export function statusLabel(status: string): string {
  const map: Record<string, string> = {
    ONLINE: '在线',
    OFFLINE: '离线',
    REGISTERED: '已登记',
    ALLOCATED: '已分配',
    IN_STOCK: '入库',
    PENDING_INSTALL: '待安装',
    IN_USE: '使用中',
    ACTIVATED_ONLINE: '已激活(在线)',
    ACTIVATED_OFFLINE: '已激活(离线)',
    RETURNED: '已退货',
    SCRAPPED: '已报废',
    DELETED: '已删除',
    FAULT: '故障',
    ENABLED: '启用',
    DISABLED: '禁用',
    ACTIVE: '生效',
    PENDING: '待支付',
    PAID: '已支付',
    CANCELLED: '已取消',
    REFUNDING: '退款中',
    REFUNDED: '已退款',
    SUCCESS: '成功',
    FAIL: '失败',
    DISPATCHED: '已派单',
    ASSIGNED: '已派单',
    ACCEPTED: '已接单',
    IN_PROGRESS: '进行中',
    COMPLETED: '待核验',
    VERIFIED: '已完成',
    APPROVED: '已批准',
    REJECTED: '已拒绝',
    SETTLED: '已结算',
    UNSETTLED: '未结算',
    ISSUED: '已开票',
    UNHANDLED: '未处理',
    HANDLED: '已处理',
    CRITICAL: '严重',
    WARNING: '警告',
    INFO: '提示',
    FLOW_BASED: '流量计费',
    MONTHLY_RENT: '月租',
    PACKAGE_RECHARGE: '套餐充值',
    SHARED: '共享',
    QR_SCAN: '扫码',
    RENTAL: '租赁',
    WALLET: '钱包',
    INSTALL: '安装',
    INSTALL_APPOINTMENT: '安装预约',
    REPAIR: '维修',
    REMOVE: '退机',
    RELOCATE: '移机',
    FILTER_REPLACE: '滤芯更换',
    INDIVIDUAL: '个人',
    ENTERPRISE: '企业'
  }
  return map[status] || status
}

/** 设备生命周期状态中文 */
export function deviceStatusLabel(status: string): string {
  return statusLabel(status)
}

/** 下载文件 */
export function downloadFile(blob: Blob, filename: string): void {
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  window.URL.revokeObjectURL(url)
}
