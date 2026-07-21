import type { RouteRecordRaw } from 'vue-router'

const AdminLayout = () => import('@/layouts/AdminLayout.vue')

// 静态路由（无需权限）
export const constantRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { hidden: true }
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { hidden: true }
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/403.vue'),
    meta: { hidden: true }
  },
  {
    path: '/',
    component: AdminLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '仪表盘', icon: 'Odometer', affix: true }
      }
    ]
  }
]

// 动态路由（需权限过滤）
export const asyncRoutes: RouteRecordRaw[] = [
  // 设备管理
  {
    path: '/devices',
    component: AdminLayout,
    meta: { title: '设备管理', icon: 'Box' },
    children: [
      { path: 'list', name: 'DeviceList', component: () => import('@/views/device/list.vue'), meta: { title: '设备列表', icon: 'List' } },
      { path: 'register', name: 'DeviceRegister', component: () => import('@/views/device/register.vue'), meta: { title: '设备登记', icon: 'CirclePlus' } },
      { path: 'models', name: 'DeviceModels', component: () => import('@/views/device/models.vue'), meta: { title: '设备型号', icon: 'Operation' } },
      { path: 'detail/:deviceId', name: 'DeviceDetail', component: () => import('@/views/device/detail.vue'), meta: { title: '设备详情', hidden: true, activeMenu: '/devices/list' } }
    ]
  },
  // 滤芯管理
  {
    path: '/filters',
    component: AdminLayout,
    meta: { title: '滤芯管理', icon: 'Filter' },
    children: [
      { path: 'list', name: 'FilterList', component: () => import('@/views/filter/list.vue'), meta: { title: '滤芯列表', icon: 'List' } },
      { path: 'inbound', name: 'FilterInbound', component: () => import('@/views/filter/inbound.vue'), meta: { title: '滤芯入库', icon: 'Box' } },
      { path: 'models', name: 'FilterModels', component: () => import('@/views/filter/models.vue'), meta: { title: '滤芯型号', icon: 'Operation' } },
      { path: 'trace/:filterId', name: 'FilterTrace', component: () => import('@/views/filter/trace.vue'), meta: { title: '滤芯追溯', hidden: true, activeMenu: '/filters/list' } }
    ]
  },
  // 客户管理
  {
    path: '/customers',
    component: AdminLayout,
    meta: { title: '客户管理', icon: 'User' },
    children: [
      { path: 'list', name: 'CustomerList', component: () => import('@/views/customer/list.vue'), meta: { title: '客户列表', icon: 'List' } },
      { path: 'detail/:id', name: 'CustomerDetail', component: () => import('@/views/customer/detail.vue'), meta: { title: '客户详情', hidden: true, activeMenu: '/customers/list' } }
    ]
  },
  // 身份审批
  {
    path: '/applications',
    component: AdminLayout,
    meta: { title: '身份审批', icon: 'Stamp' },
    children: [
      { path: 'list', name: 'ApplicationList', component: () => import('@/views/application/list.vue'), meta: { title: '申请列表', icon: 'List' } }
    ]
  },
  // 经销商管理
  {
    path: '/dealers',
    component: AdminLayout,
    meta: { title: '经销商管理', icon: 'OfficeBuilding' },
    children: [
      { path: 'list', name: 'DealerList', component: () => import('@/views/dealer/list.vue'), meta: { title: '经销商列表', icon: 'List' } }
    ]
  },
  // 运维人员
  {
    path: '/workers',
    component: AdminLayout,
    meta: { title: '运维人员', icon: 'UserFilled' },
    children: [
      { path: 'list', name: 'WorkerList', component: () => import('@/views/worker/list.vue'), meta: { title: '运维人员列表', icon: 'List' } }
    ]
  },
  // 工单管理
  {
    path: '/work-orders',
    component: AdminLayout,
    meta: { title: '工单管理', icon: 'Document' },
    children: [
      { path: 'list', name: 'WorkOrderList', component: () => import('@/views/workOrder/list.vue'), meta: { title: '工单列表', icon: 'List' } },
      { path: 'detail/:id', name: 'WorkOrderDetail', component: () => import('@/views/workOrder/detail.vue'), meta: { title: '工单详情', hidden: true, activeMenu: '/work-orders/list' } }
    ]
  },
  // 订单管理
  {
    path: '/orders',
    component: AdminLayout,
    meta: { title: '订单管理', icon: 'ShoppingCart' },
    children: [
      { path: 'list', name: 'OrderList', component: () => import('@/views/order/list.vue'), meta: { title: '订单列表', icon: 'List' } },
      { path: 'abnormal', name: 'AbnormalOrder', component: () => import('@/views/order/abnormal.vue'), meta: { title: '异常订单', icon: 'Warning' } }
    ]
  },
  // 套餐管理
  {
    path: '/packages',
    component: AdminLayout,
    meta: { title: '套餐管理', icon: 'Goods' },
    children: [
      { path: 'list', name: 'PackageList', component: () => import('@/views/package/list.vue'), meta: { title: '套餐列表', icon: 'List' } }
    ]
  },
  // 库存管理
  {
    path: '/inventory',
    component: AdminLayout,
    meta: { title: '库存管理', icon: 'Box' },
    children: [
      { path: 'devices', name: 'InventoryDevices', component: () => import('@/views/inventory/devices.vue'), meta: { title: '设备库存', icon: 'Monitor' } },
      { path: 'batches', name: 'InventoryBatches', component: () => import('@/views/inventory/batches.vue'), meta: { title: '进货批次', icon: 'Tickets' } }
    ]
  },
  // 财务管理
  {
    path: '/finance',
    component: AdminLayout,
    meta: { title: '财务管理', icon: 'Money' },
    children: [
      { path: 'records', name: 'FinanceRecords', component: () => import('@/views/finance/records.vue'), meta: { title: '充值记录', icon: 'Wallet' } },
      { path: 'settlements', name: 'FinanceSettlements', component: () => import('@/views/finance/settlements.vue'), meta: { title: '结算账单', icon: 'Document' } },
      { path: 'commissions', name: 'FinanceCommissions', component: () => import('@/views/finance/commissions.vue'), meta: { title: '分佣明细', icon: 'Coin' } },
      { path: 'invoices', name: 'FinanceInvoices', component: () => import('@/views/finance/invoices.vue'), meta: { title: '发票管理', icon: 'Tickets' } },
      { path: 'refunds', name: 'FinanceRefunds', component: () => import('@/views/finance/refunds.vue'), meta: { title: '退款管理', icon: 'RefreshLeft' } }
    ]
  },
  // 报表中心
  {
    path: '/reports',
    component: AdminLayout,
    meta: { title: '报表中心', icon: 'DataAnalysis' },
    children: [
      { path: 'water-quality', name: 'ReportWaterQuality', component: () => import('@/views/report/waterQuality.vue'), meta: { title: '水质报表', icon: 'LineChart' } },
      { path: 'flow', name: 'ReportFlow', component: () => import('@/views/report/flow.vue'), meta: { title: '流量报表', icon: 'DataLine' } },
      { path: 'revenue', name: 'ReportRevenue', component: () => import('@/views/report/revenue.vue'), meta: { title: '营收报表', icon: 'BarChart' } },
      { path: 'work-order', name: 'ReportWorkOrder', component: () => import('@/views/report/workOrder.vue'), meta: { title: '工单报表', icon: 'PieChart' } }
    ]
  },
  // 监控预警
  {
    path: '/monitor',
    component: AdminLayout,
    meta: { title: '监控预警', icon: 'Monitor' },
    children: [
      { path: 'alerts', name: 'MonitorAlerts', component: () => import('@/views/monitor/alerts.vue'), meta: { title: '告警列表', icon: 'Warning' } },
      { path: 'thresholds', name: 'MonitorThresholds', component: () => import('@/views/monitor/thresholds.vue'), meta: { title: '阈值管理', icon: 'Setting' } }
    ]
  },
  // 系统管理
  {
    path: '/system',
    component: AdminLayout,
    meta: { title: '系统管理', icon: 'Setting' },
    children: [
      { path: 'roles', name: 'SystemRoles', component: () => import('@/views/system/roles.vue'), meta: { title: '角色管理', icon: 'UserFilled' } },
      { path: 'users', name: 'SystemUsers', component: () => import('@/views/system/users.vue'), meta: { title: '账户管理', icon: 'Avatar' } },
      { path: 'configs', name: 'SystemConfigs', component: () => import('@/views/system/configs.vue'), meta: { title: '系统配置', icon: 'Tools' } },
      { path: 'mqtt', name: 'SystemMqtt', component: () => import('@/views/system/mqtt.vue'), meta: { title: 'MQTT连接', icon: 'Connection' } },
      { path: 'audit-logs', name: 'SystemAuditLogs', component: () => import('@/views/system/auditLogs.vue'), meta: { title: '审计日志', icon: 'Document' } }
    ]
  },
  // 可视化大屏（全屏独立页面，新窗口打开）
  {
    path: '/bigscreen',
    name: 'BigScreen',
    component: () => import('@/views/dashboard/bigscreen.vue'),
    meta: { title: '可视化大屏', icon: 'DataLine', external: true }
  },
  // 通配兜底
  { path: '/:pathMatch(.*)*', redirect: '/404', meta: { hidden: true } }
]
