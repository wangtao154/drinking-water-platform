/**
 * 全量 API 测试脚本 - 覆盖所有模块的增删改查
 */
const http = require('http')

const BASE = 'localhost'
const PORT = 8080

let token = ''
const results = []
let passCount = 0
let failCount = 0

function api(method, path, body) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null
    const headers = { 'Content-Type': 'application/json' }
    if (token) headers['Authorization'] = `Bearer ${token}`
    if (data) headers['Content-Length'] = Buffer.byteLength(data)

    const req = http.request({ hostname: BASE, port: PORT, path: `/api/v1${path}`, method, headers }, (res) => {
      let chunks = ''
      res.on('data', d => chunks += d)
      res.on('end', () => {
        try {
          resolve({ status: res.statusCode, data: JSON.parse(chunks) })
        } catch {
          resolve({ status: res.statusCode, data: chunks })
        }
      })
    })
    req.on('error', reject)
    if (data) req.write(data)
    req.end()
  })
}

function record(name, pass, detail = '') {
  const status = pass ? 'PASS' : 'FAIL'
  results.push({ name, status, detail })
  if (pass) passCount++
  else failCount++
  const icon = pass ? '[PASS]' : '[FAIL]'
  console.log(`${icon} ${name}${detail ? ' - ' + detail : ''}`)
}

function sleep(ms) { return new Promise(r => setTimeout(r, ms)) }

async function main() {
  console.log('========== 直饮水平台全量功能测试 ==========\n')

  // ============ 0. 登录 ============
  console.log('--- 0. 认证 ---')
  const loginRes = await api('POST', '/auth/login', { account: 'admin', password: 'admin123' })
  if (loginRes.data.code === 200 && loginRes.data.data?.accessToken) {
    token = loginRes.data.data.accessToken
    record('管理员登录', true, 'Token获取成功')
  } else {
    record('管理员登录', false, JSON.stringify(loginRes.data))
    console.log('登录失败，终止测试')
    return
  }

  // ============ 1. 系统管理 ============
  console.log('\n--- 1. 系统管理 ---')

  // 1.1 角色管理 CRUD
  const rolesList = await api('GET', '/system/roles?pageNum=1&pageSize=20')
  record('角色列表', rolesList.data.code === 200, `共 ${rolesList.data.data?.total || rolesList.data.data?.length || 0} 个角色`)

  const rolesAll = await api('GET', '/system/roles/list')
  record('角色全量列表', rolesAll.data.code === 200, `返回 ${rolesAll.data.data?.length || 0} 个角色`)

  const roleCreate = await api('POST', '/system/roles', {
    roleCode: 'TEST_ROLE_001',
    roleName: '测试角色',
    roleDesc: '全量测试创建的角色'
  })
  const roleId = roleCreate.data.data?.id
  record('角色创建', roleCreate.data.code === 200, `角色ID: ${roleId || 'N/A'}`)

  if (roleId) {
    const roleUpdate = await api('PUT', `/system/roles/${roleId}`, {
      roleCode: 'TEST_ROLE_001',
      roleName: '测试角色-已修改',
      roleDesc: '修改后的描述'
    })
    record('角色修改', roleUpdate.data.code === 200, `名称改为"测试角色-已修改"`)

    const roleDelete = await api('DELETE', `/system/roles/${roleId}`)
    record('角色删除', roleDelete.data.code === 200, '已删除')

    // 验证删除
    const roleVerify = await api('GET', '/system/roles?pageNum=1&pageSize=20')
    const roleStillExists = (roleVerify.data.data?.records || roleVerify.data.data || []).some(r => r.id === roleId || r.id === String(roleId))
    record('角色删除验证', !roleStillExists, '角色已不存在')
  }

  // 1.2 账户管理 CRUD
  const userList = await api('GET', '/system/users?pageNum=1&pageSize=20')
  record('账户列表', userList.data.code === 200, `共 ${userList.data.data?.total || userList.data.data?.length || 0} 个账户`)

  const userCreate = await api('POST', '/system/users', {
    employeeNo: 'EMP-TEST-001',
    username: 'testuser001',
    password: 'Test@1234',
    roleId: 1,
    name: '测试用户',
    phone: '13800000001',
    department: '测试部门'
  })
  const userId = userCreate.data.data?.id
  record('账户创建', userCreate.data.code === 200, `账户ID: ${userId || 'N/A'} - ${userCreate.data.msg || ''}`)

  if (userId) {
    const userUpdate = await api('PUT', `/system/users/${userId}`, {
      employeeNo: 'EMP-TEST-001',
      username: 'testuser001',
      name: '测试用户-已修改',
      phone: '13800000002',
      department: '测试部门-修改',
      roleId: 1
    })
    record('账户修改', userUpdate.data.code === 200, `名称改为"测试用户-已修改"`)

    // 重置密码
    const pwdReset = await api('PUT', `/system/users/${userId}/reset-password`, { newPassword: 'NewPass@123' })
    record('重置密码', pwdReset.data.code === 200, '密码已重置')

    const userDelete = await api('DELETE', `/system/users/${userId}`)
    record('账户删除', userDelete.data.code === 200, '已删除')
  }

  // 1.3 审计日志
  const auditLogs = await api('GET', '/system/audit-logs?pageNum=1&pageSize=10')
  record('审计日志列表', auditLogs.data.code === 200, `共 ${auditLogs.data.data?.total || 0} 条日志`)

  // 1.4 系统配置
  const configs = await api('GET', '/system/configs?pageNum=1&pageSize=10')
  record('系统配置列表', configs.data.code === 200, `共 ${configs.data.data?.total || 0} 条配置`)

  // ============ 2. 设备管理 ============
  console.log('\n--- 2. 设备管理 ---')

  const deviceList = await api('GET', '/devices?pageNum=1&pageSize=20')
  record('设备列表', deviceList.data.code === 200, `共 ${deviceList.data.data?.total || 0} 台设备`)

  const modelList = await api('GET', '/device-models')
  record('设备型号列表', modelList.data.code === 200, `共 ${modelList.data.data?.length || 0} 个型号`)

  // 设备型号 CRUD
  const modelCreate = await api('POST', '/device-models', {
    modelCode: 'TEST-MODEL-CRUD',
    modelName: '测试型号CRUD',
    category: '测试分类',
    status: 'ENABLED',
    description: '全量测试创建的型号'
  })
  const testModelId = modelCreate.data.data?.id
  record('设备型号创建', modelCreate.data.code === 200, `型号ID: ${testModelId || 'N/A'} - ${modelCreate.data.msg || ''}`)

  if (testModelId) {
    const modelUpdate = await api('PUT', `/device-models/${testModelId}`, {
      modelCode: 'TEST-MODEL-CRUD',
      modelName: '测试型号CRUD-已修改',
      category: '测试分类-修改',
      status: 'DISABLED',
      description: '修改后描述'
    })
    record('设备型号修改', modelUpdate.data.code === 200, `名称改为"测试型号CRUD-已修改"`)

    const modelDelete = await api('DELETE', `/device-models/${testModelId}`)
    record('设备型号删除', modelDelete.data.code === 200, '已删除')
  }

  // ============ 3. 滤芯管理 ============
  console.log('\n--- 3. 滤芯管理 ---')

  const filterList = await api('GET', '/filters?pageNum=1&pageSize=20')
  record('滤芯列表', filterList.data.code === 200, `共 ${filterList.data.data?.total || 0} 个滤芯`)

  // 滤芯 CRUD
  const filterCreate = await api('POST', '/filters', {
    filterName: '测试滤芯-PP棉',
    filterType: 'PP棉',
    spec: '10寸',
    lifespan: 6,
    manufacturer: '测试厂商',
    status: 'ENABLED'
  })
  const filterId = filterCreate.data.data?.id
  record('滤芯创建', filterCreate.data.code === 200, `滤芯ID: ${filterId || 'N/A'} - ${filterCreate.data.msg || ''}`)

  if (filterId) {
    const filterUpdate = await api('PUT', `/filters/${filterId}`, {
      filterName: '测试滤芯-PP棉-已修改',
      filterType: 'PP棉',
      spec: '10寸加强型',
      lifespan: 12,
      manufacturer: '测试厂商-修改',
      status: 'ENABLED'
    })
    record('滤芯修改', filterUpdate.data.code === 200, `名称改为"测试滤芯-PP棉-已修改"`)

    const filterDelete = await api('DELETE', `/filters/${filterId}`)
    record('滤芯删除', filterDelete.data.code === 200, '已删除')
  }

  // 滤芯更换记录
  const filterRecords = await api('GET', '/filters/records?pageNum=1&pageSize=10')
  record('滤芯更换记录', filterRecords.data.code === 200, `共 ${filterRecords.data.data?.total || 0} 条记录`)

  // ============ 4. 客户管理 ============
  console.log('\n--- 4. 客户管理 ---')

  const customerList = await api('GET', '/customers?pageNum=1&pageSize=20')
  record('客户列表', customerList.data.code === 200, `共 ${customerList.data.data?.total || 0} 个客户`)

  const customerCreate = await api('POST', '/customers', {
    customerName: '测试客户-张三',
    phone: '13900000001',
    address: '测试地址-贵阳市',
    type: 'INDIVIDUAL',
    status: 'ACTIVE'
  })
  const customerId = customerCreate.data.data?.id
  record('客户创建', customerCreate.data.code === 200, `客户ID: ${customerId || 'N/A'} - ${customerCreate.data.msg || ''}`)

  if (customerId) {
    const customerUpdate = await api('PUT', `/customers/${customerId}`, {
      customerName: '测试客户-张三-已修改',
      phone: '13900000002',
      address: '测试地址-修改',
      type: 'INDIVIDUAL',
      status: 'ACTIVE'
    })
    record('客户修改', customerUpdate.data.code === 200, `名称改为"测试客户-张三-已修改"`)

    const customerDelete = await api('DELETE', `/customers/${customerId}`)
    record('客户删除', customerDelete.data.code === 200, '已删除')
  }

  // ============ 5. 经销商管理 ============
  console.log('\n--- 5. 经销商管理 ---')

  const dealerList = await api('GET', '/dealers?pageNum=1&pageSize=20')
  record('经销商列表', dealerList.data.code === 200, `共 ${dealerList.data.data?.total || 0} 个经销商`)

  const dealerCreate = await api('POST', '/dealers', {
    dealerName: '测试经销商-贵阳分公司',
    contactPerson: '李四',
    phone: '13800001111',
    address: '贵阳市观山湖区',
    status: 'ACTIVE'
  })
  const dealerId = dealerCreate.data.data?.id
  record('经销商创建', dealerCreate.data.code === 200, `经销商ID: ${dealerId || 'N/A'} - ${dealerCreate.data.msg || ''}`)

  if (dealerId) {
    const dealerUpdate = await api('PUT', `/dealers/${dealerId}`, {
      dealerName: '测试经销商-贵阳分公司-已修改',
      contactPerson: '李四-改',
      phone: '13800002222',
      address: '贵阳市南明区',
      status: 'ACTIVE'
    })
    record('经销商修改', dealerUpdate.data.code === 200, `名称改为"测试经销商-贵阳分公司-已修改"`)

    const dealerDelete = await api('DELETE', `/dealers/${dealerId}`)
    record('经销商删除', dealerDelete.data.code === 200, '已删除')
  }

  // ============ 6. 运维人员 ============
  console.log('\n--- 6. 运维人员 ---')

  const workerList = await api('GET', '/workers?pageNum=1&pageSize=20')
  record('运维人员列表', workerList.data.code === 200, `共 ${workerList.data.data?.total || 0} 个运维人员`)

  const workerCreate = await api('POST', '/workers', {
    workerName: '测试运维-王五',
    phone: '13700000001',
    skillLevel: 'SENIOR',
    status: 'ACTIVE',
    region: '贵阳'
  })
  const workerId = workerCreate.data.data?.id
  record('运维人员创建', workerCreate.data.code === 200, `运维ID: ${workerId || 'N/A'} - ${workerCreate.data.msg || ''}`)

  if (workerId) {
    const workerUpdate = await api('PUT', `/workers/${workerId}`, {
      workerName: '测试运维-王五-已修改',
      phone: '13700000002',
      skillLevel: 'EXPERT',
      status: 'ACTIVE',
      region: '贵阳-观山湖'
    })
    record('运维人员修改', workerUpdate.data.code === 200, `名称改为"测试运维-王五-已修改"`)

    const workerDelete = await api('DELETE', `/workers/${workerId}`)
    record('运维人员删除', workerDelete.data.code === 200, '已删除')
  }

  // ============ 7. 工单管理 ============
  console.log('\n--- 7. 工单管理 ---')

  const workOrderList = await api('GET', '/work-orders?pageNum=1&pageSize=20')
  record('工单列表', workOrderList.data.code === 200, `共 ${workOrderList.data.data?.total || 0} 个工单`)

  const woCreate = await api('POST', '/work-orders', {
    deviceSn: 'TEST-WO-001',
    orderType: 'MAINTENANCE',
    description: '测试工单-设备维护',
    priority: 'NORMAL'
  })
  const woId = woCreate.data.data?.id
  record('工单创建', woCreate.data.code === 200, `工单ID: ${woId || 'N/A'} - ${woCreate.data.msg || ''}`)

  if (woId) {
    const woUpdate = await api('PUT', `/work-orders/${woId}`, {
      description: '测试工单-设备维护-已修改',
      priority: 'HIGH'
    })
    record('工单修改', woUpdate.data.code === 200, '描述已修改')

    const woDelete = await api('DELETE', `/work-orders/${woId}`)
    record('工单删除', woDelete.data.code === 200, '已删除')
  }

  // ============ 8. 套餐管理 ============
  console.log('\n--- 8. 套餐管理 ---')

  const pkgList = await api('GET', '/packages?pageNum=1&pageSize=20')
  record('套餐列表', pkgList.data.code === 200, `共 ${pkgList.data.data?.total || 0} 个套餐`)

  const pkgCreate = await api('POST', '/packages', {
    packageName: '测试套餐-月度畅饮',
    packageType: 'MONTHLY',
    price: 99.00,
    waterQuota: 200,
    description: '全量测试创建的月度套餐'
  })
  const pkgId = pkgCreate.data.data?.id
  record('套餐创建', pkgCreate.data.code === 200, `套餐ID: ${pkgId || 'N/A'} - ${pkgCreate.data.msg || ''}`)

  if (pkgId) {
    const pkgUpdate = await api('PUT', `/packages/${pkgId}`, {
      packageName: '测试套餐-月度畅饮-已修改',
      packageType: 'MONTHLY',
      price: 129.00,
      waterQuota: 250,
      description: '修改后的月度套餐'
    })
    record('套餐修改', pkgUpdate.data.code === 200, `价格改为129.00`)

    const pkgDelete = await api('DELETE', `/packages/${pkgId}`)
    record('套餐删除', pkgDelete.data.code === 200, '已删除')
  }

  // ============ 9. 订单管理 ============
  console.log('\n--- 9. 订单管理 ---')

  const orderList = await api('GET', '/orders?pageNum=1&pageSize=20')
  record('订单列表', orderList.data.code === 200, `共 ${orderList.data.data?.total || 0} 个订单`)

  const rechargeList = await api('GET', '/orders?pageNum=1&pageSize=20&orderType=RECHARGE')
  record('充值记录列表', rechargeList.data.code === 200, `共 ${rechargeList.data.data?.total || 0} 条充值记录`)

  // ============ 10. 财务管理 ============
  console.log('\n--- 10. 财务管理 ---')

  const invoiceList = await api('GET', '/finance/invoices?pageNum=1&pageSize=20')
  record('发票列表', invoiceList.data.code === 200, `共 ${invoiceList.data.data?.total || 0} 张发票`)

  const financeRecords = await api('GET', '/finance/records?pageNum=1&pageSize=20')
  record('财务记录列表', financeRecords.data.code === 200, `共 ${financeRecords.data.data?.total || 0} 条记录`)

  // 发票创建
  const invoiceCreate = await api('POST', '/finance/invoices', {
    title: '测试发票-公司名称',
    taxNo: '91520100TEST12345X',
    amount: 1000.00,
    type: 'GENERAL',
    email: 'test@test.com'
  })
  const invoiceId = invoiceCreate.data.data?.id
  record('发票创建', invoiceCreate.data.code === 200, `发票ID: ${invoiceId || 'N/A'} - ${invoiceCreate.data.msg || ''}`)

  if (invoiceId) {
    const invoiceDelete = await api('DELETE', `/finance/invoices/${invoiceId}`)
    record('发票删除', invoiceDelete.data.code === 200, '已删除')
  }

  // ============ 11. 库存管理 ============
  console.log('\n--- 11. 库存管理 ---')

  const stockList = await api('GET', '/inventory/stock?pageNum=1&pageSize=20')
  record('库存列表', stockList.data.code === 200, `共 ${stockList.data.data?.total || 0} 条库存`)

  const stockMovements = await api('GET', '/inventory/movements?pageNum=1&pageSize=20')
  record('库存流水', stockMovements.data.code === 200, `共 ${stockMovements.data.data?.total || 0} 条流水`)

  // ============ 12. 监控预警 ============
  console.log('\n--- 12. 监控预警 ---')

  const alertList = await api('GET', '/monitor/alerts?pageNum=1&pageSize=20')
  record('告警列表', alertList.data.code === 200, `共 ${alertList.data.data?.total || 0} 条告警`)

  // ============ 13. 报表中心 ============
  console.log('\n--- 13. 报表中心 ---')

  const dashboard = await api('GET', '/reports/dashboard')
  record('仪表盘数据', dashboard.data.code === 200, `设备总数: ${dashboard.data.data?.totalDevices || 'N/A'}`)

  const flowReport = await api('GET', '/reports/flow')
  record('流量报表数据', flowReport.data.code === 200, `总流量: ${flowReport.data.data?.totalWaterFlow || 'N/A'}`)

  const reportDevices = await api('GET', '/reports/devices?pageNum=1&pageSize=10')
  record('设备报表', reportDevices.data.code === 200, `数据: ${reportDevices.data.data?.total || 0} 条`)

  const reportOrders = await api('GET', '/reports/orders?pageNum=1&pageSize=10')
  record('订单报表', reportOrders.data.code === 200, `数据: ${reportOrders.data.data?.total || 0} 条`)

  const reportFinance = await api('GET', '/reports/finance?pageNum=1&pageSize=10')
  record('财务报表', reportFinance.data.code === 200, `数据: ${reportFinance.data.data?.total || 0} 条`)

  const reportWorkers = await api('GET', '/reports/workers?pageNum=1&pageSize=10')
  record('运维报表', reportWorkers.data.code === 200, `数据: ${reportWorkers.data.data?.total || 0} 条`)

  // ============ 14. 推送服务 ============
  console.log('\n--- 14. 推送服务 ---')

  const pushList = await api('GET', '/push/messages?pageNum=1&pageSize=10')
  record('推送消息列表', pushList.data.code === 200, `共 ${pushList.data.data?.total || 0} 条消息 - ${pushList.data.msg || ''}`)

  // ============ 15. IoT 服务 ============
  console.log('\n--- 15. IoT 服务 ---')

  const iotStatus = await api('GET', '/iot/status')
  record('IoT状态', iotStatus.data.code === 200, `${iotStatus.data.msg || 'OK'}`)

  // ============ 汇总 ============
  console.log('\n========== 测试汇总 ==========')
  const byModule = {}
  results.forEach(r => {
    const parts = r.name.split('-')
    const mod = parts[0].trim()
    if (!byModule[mod]) byModule[mod] = { pass: 0, fail: 0 }
    if (r.status === 'PASS') byModule[mod].pass++
    else byModule[mod].fail++
  })

  Object.entries(byModule).forEach(([mod, counts]) => {
    const total = counts.pass + counts.fail
    const rate = ((counts.pass / total) * 100).toFixed(0)
    console.log(`  ${mod}: ${counts.pass}/${total} PASS (${rate}%) ${counts.fail > 0 ? '⚠️ ' + counts.fail + ' FAILED' : ''}`)
  })

  console.log('\n--------------------------------')
  const totalRate = ((passCount / (passCount + failCount)) * 100).toFixed(1)
  console.log(`总计: ${passCount} PASS / ${failCount} FAIL (${totalRate}%)`)

  if (failCount > 0) {
    console.log('\n--- 失败项详情 ---')
    results.filter(r => r.status === 'FAIL').forEach(r => {
      console.log(`  [FAIL] ${r.name} - ${r.detail}`)
    })
  }

  console.log('================================')
}

main().catch(err => {
  console.error('测试执行错误:', err)
  process.exit(1)
})
