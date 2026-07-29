/** 后端统一响应结构 */
export interface R<T = any> {
  code: number
  message: string
  data: T
  timestamp?: number
}

/** 分页查询基础参数 */
export interface PageQueryDTO {
  pageNum?: number
  pageSize?: number
  keyword?: string
}

/** 分页结果 */
export interface PageResult<T = any> {
  total: number
  size: number
  current: number
  pages?: number
  records: T[]
}

// ===== Auth =====
export interface LoginDTO {
  account: string
  password: string
}

export interface LoginVO {
  accessToken: string
  refreshToken: string
  userInfo: SysUserVO
  permissions: string[]
  roles: string[]
}

// ===== User =====
export interface SysUserVO {
  id: number
  account: string
  nickname: string
  phone: string
  email: string
  status: number
  roles?: string[]
  createdAt: string
}

export interface CustomerVO {
  id: number
  customerName: string
  customerType: string
  phone: string
  idCard?: string
  enterpriseName?: string
  address: string
  dealerId?: number
  dealerName?: string
  status?: string
  registeredAt?: string
  createdAt: string
  updatedAt?: string
}

export interface DealerVO {
  id: number
  dealerName: string
  dealerCode: string
  contactPerson: string
  contactPhone: string
  level: number
  parentId?: number
  address: string
  status: number
  createdAt: string
}

export interface WorkerVO {
  id: number
  name: string
  phone: string
  province?: string
  city?: string
  district?: string
  address?: string
  dealerId?: number
  status: string
  serviceCount: number
  rating: number
  registeredAt?: string
  createdAt: string
}

// ===== Device =====
export interface DeviceModelVO {
  id: number
  modelCode: string
  modelName: string
  category: string
  description: string
  status: string
  filterConfig?: string
  createdAt: string
}

export interface DeviceVO {
  id: number
  deviceId: string
  sn: string
  modelId: number
  modelName?: string
  iccid?: string
  imei?: string
  productionDate?: string
  productionBatch?: string
  qrCodeUrl?: string
  onlineStatus: number
  lifecycleStatus: string
  dealerId?: number
  customerName?: string
  dealerName?: string
  customerId?: number
  activatedAt?: string
  createdAt: string
  updatedAt?: string
}

export interface DeviceRegisterDTO {
  sn: string
  modelId: number
  iccid?: string
  imei?: string
  productionDate?: string
  productionBatch?: string
}

// ===== IoT =====
export interface TelemetryVO {
  deviceSn: string
  timestamp: string
  points: Record<string, number | string>
  online: boolean
}

// ===== Filter =====
export interface FilterModelVO {
  id: number
  modelCode: string
  modelName: string
  filterLevel: number
  ratedLife: number
  unit: string
  status: string
  createdAt: string
}

export interface FilterInstanceVO {
  id: number
  filterId: string
  modelId: number
  modelName?: string
  deviceId?: string
  currentDeviceId?: string
  status: string
  lifecycleStatus?: string
  remainPercentage?: number
  installedAt?: string
  scrappedAt?: string
  replacedAt?: string
  qrCodeUrl?: string
  createdAt: string
}

// ===== WorkOrder =====
export interface WorkOrderLogVO {
  id: number
  workOrderId: number
  fromStatus?: string
  toStatus?: string
  fromStatusDesc?: string
  toStatusDesc?: string
  operatorType?: string
  operatorName?: string
  remark?: string
  createdAt: string
}

export interface WorkOrderVO {
  id: number
  orderNo: string
  orderType: string
  orderStatus: string
  deviceId?: string
  deviceSn?: string
  customerId?: number
  customerName?: string
  customerPhone?: string
  workerId?: number
  workerName?: string
  workerPhone?: string
  description: string
  customerImages?: string[]
  photoUrls?: string[]
  remark?: string
  reviewContent?: string
  rating?: number
  address?: string
  province?: string
  city?: string
  district?: string
  contactPerson?: string
  contactPhone?: string
  acceptedAt?: string
  completedAt?: string
  createdAt: string
  updatedAt?: string
  statusLogs?: WorkOrderLogVO[]
}

export interface WorkOrderStatisticsVO {
  total: number
  pending: number
  dispatched: number
  accepted: number
  inProgress: number
  completed: number
  cancelled: number
}

// ===== Package =====
export interface PackageVO {
  id: number
  packageCode: string
  packageName: string
  packageType: string
  chargeMode: string
  price: number
  originalPrice?: number
  description: string
  status: string
  createdAt: string
}

// ===== Order =====
export interface OrderVO {
  id: number
  orderNo: string
  customerId?: number
  customerName?: string
  deviceId?: string
  packageId?: number
  packageName?: string
  orderStatus: string
  payAmount: number
  payTime?: string
  payMethod?: string
  remark?: string
  createdAt: string
}

export interface OrderStatisticsVO {
  total: number
  pending: number
  paid: number
  cancelled: number
  refunding: number
  refunded: number
  totalAmount: number
  refundAmount: number
}

export interface ScanOrderVO {
  id: number
  orderNo: string
  customerId?: number
  deviceId?: string
  sn?: string
  targetMl: number
  payAmount: number
  payStatus: string
  dispenseStatus: string
  commandStatus: string
  q74Payload?: string
  transactionId?: string
  paymentProvider?: string
  mockPayment?: boolean
  paidAt?: string
  commandSentAt?: string
  completedAt?: string
  remark?: string
  createdAt: string
  updatedAt?: string
}

export interface ScanOrderStatsVO {
  total: number
  paid: number
  pending: number
  dispatched: number
  sent: number
  totalAmount: number
}

// ===== Payment =====
export interface PaymentVO {
  id: number
  txId: string
  orderNo: string
  payStatus: string
  payMethod: string
  amount: number
  paidAt?: string
  createdAt: string
}

export interface RefundVO {
  id: number
  refundNo: string
  orderNo: string
  refundStatus: string
  refundAmount: number
  reason: string
  approvedBy?: string
  approvedAt?: string
  createdAt: string
}

// ===== Finance =====
export interface FinanceSummaryVO {
  totalCommission: number
  settledCommission: number
  pendingCommission: number
  totalConsumption: number
  totalRevenue: number
  pendingSettlements: number
}

export interface SettlementVO {
  id: number
  billNo: string
  dealerId?: number
  dealerName?: string
  billStartDate: string
  billEndDate: string
  totalAmount: number
  commissionAmount: number
  withdrawAmount: number
  status: string
  createdAt: string
  updatedAt: string
}

export interface InvoiceVO {
  id: number
  invoiceNo: string
  customerId: number
  amount: number
  title: string
  taxNo?: string
  status: string
  issuedAt?: string
  mailedAt?: string
  mailingAddress?: string
  createdAt: string
  updatedAt: string
}

export interface CommissionVO {
  id: number
  commissionNo: string
  orderNo: string
  dealerId: number
  dealerName?: string
  amount: number
  status: string
  settledAt?: string
  createdAt: string
}

// ===== Inventory =====
export interface DeviceStockVO {
  id: number
  deviceId: string
  sn: string
  modelName?: string
  warehouseType: string
  warehouseId?: number
  quantity: number
  status: string
  createdAt: string
}

export interface StockBatchVO {
  id: number
  batchNo: string
  batchType: string
  totalQuantity: number
  remainingQuantity: number
  warehouseType: string
  remark?: string
  createdAt: string
}

export interface InventoryStatisticsVO {
  totalDevices: number
  inStockDevices: number
  allocatedDevices: number
  totalFilters: number
  inStockFilters: number
  lowStockFilters: number
}

// ===== Monitor =====
export interface AlertVO {
  id: number
  deviceId?: string
  sn: string
  alertType: string
  alertLevel: string
  alertMessage: string
  pushStatus?: string
  handledStatus: string
  autoWorkOrderId?: number
  relatedFilterId?: string
  triggeredAt?: string
  pushedAt?: string
  handledAt?: string
  createdAt: string
}

export interface AlertStatisticsVO {
  total: number
  unhandled: number
  handled: number
  critical: number
  warning: number
  info: number
}

export interface ThresholdVO {
  id: number
  pointCode: string
  pointName: string
  thresholdValue: number
  operator: string
  alertLevel: string
  enabled: number
}

// ===== Report =====
export interface DashboardVO {
  totalDevices: number
  onlineDevices: number
  offlineDevices: number
  totalRevenue: number
  totalOrders: number
  activeAlerts: number
  totalCustomers: number
  totalWorkers: number
}

export interface ReportDeviceVO {
  totalDevices: number
  onlineCount: number
  offlineCount: number
  faultCount: number
  registeredToday: number
  activatedToday: number
}

export interface ReportOrderVO {
  totalOrders: number
  pendingOrders: number
  paidOrders: number
  cancelledOrders: number
  refundedOrders: number
  totalRevenue: number
  todayOrders: number
  todayRevenue: number
}

export interface ReportFinanceVO {
  totalCommission: number
  settledCommission: number
  pendingCommission: number
  totalConsumption: number
  totalRevenue: number
  pendingSettlements: number
}

export interface ReportWorkerVO {
  totalWorkers: number
  activeWorkers: number
  totalWorkOrders: number
  pendingWorkOrders: number
  completedWorkOrders: number
  avgRating: number
}

// ===== System =====
export interface SysConfigVO {
  id: number
  configKey: string
  configValue: string
  configDesc: string
  configType: string
  createdAt: string
  updatedAt: string
}

export interface AuditLogVO {
  id: number
  userId: number
  username: string
  operation: string
  method: string
  params?: string
  ip: string
 耗时?: number
  createdAt: string
}

// ===== Push =====
export interface PushStatsVO {
  totalAlerts: number
  pushedAlerts: number
  unpushedAlerts: number
  pushSuccess: number
  pushFail: number
}

// ===== 充值记录 (复用 OrderVO, 过滤 orderType=RECHARGE) =====

// ===== 流量报表 =====
export interface FlowReportVO {
  totalDevices: number
  onlineDevices: number
  offlineDevices: number
  todayReportDevices: number
  totalWaterFlow: number
  todayWaterFlow: number
  totalPureFlow: number
  todayPureFlow: number
  dailyFlowTrend: { date: string; value: number }[]
  modelDistribution: { name: string; value: number }[]
  onlineRate: number
}

// ===== 系统角色 =====
export interface SysRoleVO {
  id: number
  roleCode: string
  roleName: string
  roleDesc?: string
  status: string
  createdAt: string
  updatedAt: string
}

export interface SysPermissionVO {
  id: number
  permissionCode: string
  permissionName: string
  permissionType: 'MENU' | 'BUTTON' | 'API' | string
  parentId?: number
  path?: string
  icon?: string
  sortOrder?: number
  status: string
  createdAt?: string
  updatedAt?: string
  children?: SysPermissionVO[]
}

// ===== 系统账户 =====
export interface SysAccountVO {
  id: number
  employeeNo: string
  username: string
  roleId?: number
  roleName?: string
  name?: string
  phone?: string
  email?: string
  wechat?: string
  department?: string
  lastLoginIp?: string
  lastLoginAt?: string
  status: string
  createdAt: string
  updatedAt: string
}
