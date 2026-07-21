/**
 * 全量 API 测试脚本 V2 - 修正了所有字段名和 API 路径
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

// Extract ID from API response (handles both string and number)
function getId(data) {
  if (!data) return null
  return data.id || null
}

async function main() {
  console.log('========== 直饮水平台全量功能测试 V2 ==========\n')

  // ============ 0. 登录 ============
  console.log('--- 0. 认证 ---')
  const loginRes = await api('POST', '/auth/login', { account: 'admin', password: 'admin123' })
  if (loginRes.data.code === 200 && loginRes.data.data?.accessToken) {
    token = loginRes.data.data.accessToken
    record('管理员登录', true, 'Token获取成功')
  } else {
    record('管理员登录', false, JSON.stringify(loginRes.data))
    return
  }

  // ============ 1. 系统管理 ============
  console.log('\n--- 1. 系统管理 ---')

  // 1.1 角色 CRUD
  const rolesList = await api('GET', '/system/roles?pageNum=1&pageSize=20')
  record('角色列表', rolesList.data.code === 200, `共 ${rolesList.data.data?.total || rolesList.data.data?.length || 0} 个角色`)

  const rolesAll = await api('GET', '/system/roles/list')
  record('角色全量列表', rolesAll.data.code === 200, `返回 ${rolesAll.data.data?.length || 0} 个角色`)

  // Verify Long→String for role IDs
  const roleIdType = rolesAll.data.data?.[0]?.id
  record('角色ID序列化为String', typeof roleIdType === 'string', `id=${roleIdType}, type=${typeof roleIdType}`)

  const roleCreate = await api('POST', '/system/roles', {
    roleCode: 'TEST_ROLE_' + Date.now(),
    roleName: 'TestRoleCRUD',
    roleDesc: 'test role for CRUD'
  })
  const roleId = getId(roleCreate.data.data)
  record('角色创建', roleCreate.data.code === 200, `角色ID: ${roleId || 'N/A'}`)

  if (roleId) {
    const roleUpdate = await api('PUT', `/system/roles/${roleId}`, {
      roleCode: 'TEST_ROLE_CRUD',
      roleName: '测试角色CRUD-已修改',
      roleDesc: '修改后的描述'
    })
    record('角色修改', roleUpdate.data.code === 200, `${roleUpdate.data.msg || 'OK'}`)

    const roleDelete = await api('DELETE', `/system/roles/${roleId}`)
    record('角色删除', roleDelete.data.code === 200, `${roleDelete.data.msg || 'OK'}`)
  }

  // 1.2 账户 CRUD
  const userList = await api('GET', '/system/users?pageNum=1&pageSize=20')
  record('账户列表', userList.data.code === 200, `共 ${userList.data.data?.total || userList.data.data?.length || 0} 个账户`)

  const ts = String(Date.now()).slice(-8)
  const userCreate = await api('POST', '/system/users', {
    employeeNo: 'EMP' + ts,
    username: 'test' + ts,
    password: 'Test@1234',
    roleId: 1,
    name: 'TestUserCRUD',
    phone: '13800000001',
    department: 'TestDept'
  })
  const userId = getId(userCreate.data.data)
  record('账户创建', userCreate.data.code === 200, `账户ID: ${userId || 'N/A'} - ${userCreate.data.message || ''}`)
  if (userCreate.data.code !== 200) console.log('  DEBUG:', JSON.stringify(userCreate.data))

  if (userId) {
    const userUpdate = await api('PUT', `/system/users/${userId}`, {
      employeeNo: 'EMP-TEST-CRUD',
      username: 'testuser_crud',
      name: '测试用户CRUD-已修改',
      phone: '13800000002',
      department: '测试部门-修改',
      roleId: 1
    })
    record('账户修改', userUpdate.data.code === 200, `${userUpdate.data.msg || 'OK'}`)

    const pwdReset = await api('PUT', `/system/users/${userId}/reset-password`, { password: 'NewPass@123' })
    record('重置密码', pwdReset.data.code === 200, `${pwdReset.data.msg || 'OK'}`)

    const userDelete = await api('DELETE', `/system/users/${userId}`)
    record('账户删除', userDelete.data.code === 200, `${userDelete.data.msg || 'OK'}`)
  }

  // 1.3 审计日志 + 系统配置
  const auditLogs = await api('GET', '/system/audit-logs?pageNum=1&pageSize=10')
  record('审计日志列表', auditLogs.data.code === 200, `共 ${auditLogs.data.data?.total || 0} 条日志`)

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
    modelCode: 'TEST_MODEL_' + Date.now(),
    modelName: 'TestModelCRUD',
    category: 'TestCategory',
    status: 'ENABLED',
    description: 'test model'
  })
  const testModelId = getId(modelCreate.data.data)
  record('设备型号创建', modelCreate.data.code === 200, `型号ID: ${testModelId || 'N/A'}`)

  if (testModelId) {
    const modelUpdate = await api('PUT', `/device-models/${testModelId}`, {
      modelCode: 'TEST_MODEL_CRUD2',
      modelName: '测试型号CRUD2-已修改',
      category: '测试分类-修改',
      status: 'DISABLED',
      description: '修改后描述'
    })
    record('设备型号修改', modelUpdate.data.code === 200, `${modelUpdate.data.msg || 'OK'}`)

    const modelDelete = await api('DELETE', `/device-models/${testModelId}`)
    record('设备型号删除', modelDelete.data.code === 200, `${modelDelete.data.msg || 'OK'}`)
  }

  // ============ 3. 滤芯管理 ============
  console.log('\n--- 3. 滤芯管理 ---')

  const filterList = await api('GET', '/filters?pageNum=1&pageSize=20')
  record('滤芯列表', filterList.data.code === 200, `共 ${filterList.data.data?.total || 0} 个滤芯`)

  // 滤芯登记：POST /filters/register，需要 filterModelId
  // 先获取一个滤芯型号ID（从列表中取第一个）
  const firstFilter = filterList.data.data?.records?.[0] || filterList.data.data?.[0]
  const filterModelId = firstFilter?.filterModelId || firstFilter?.modelId || 1

  const filterCreate = await api('POST', '/filters/register', {
    filterModelId: filterModelId,
    productionDate: '2026-07-09',
    productionBatch: 'BATCH-TEST-001'
  })
  const filterId = getId(filterCreate.data.data)
  record('滤芯登记', filterCreate.data.code === 200, `滤芯ID: ${filterId || 'N/A'} - ${filterCreate.data.msg || ''}`)

  // ============ 4. 客户管理 ============
  console.log('\n--- 4. 客户管理 ---')

  const customerList = await api('GET', '/customers?pageNum=1&pageSize=20')
  record('客户列表', customerList.data.code === 200, `共 ${customerList.data.data?.total || 0} 个客户`)

  const customerCreate = await api('POST', '/customers', {
    name: 'TestCustomerCRUD',
    phone: '13900000001',
    address: 'test address',
    customerType: 'RESIDENTIAL'
  })
  const customerId = getId(customerCreate.data.data)
  record('客户创建', customerCreate.data.code === 200, `客户ID: ${customerId || 'N/A'} - ${customerCreate.data.msg || ''}`)

  if (customerId) {
    const customerDelete = await api('DELETE', `/customers/${customerId}`)
    record('客户删除', customerDelete.data.code === 200, `${customerDelete.data.msg || 'OK'}`)
  }

  // ============ 5. 经销商管理 ============
  console.log('\n--- 5. 经销商管理 ---')

  const dealerList = await api('GET', '/dealers?pageNum=1&pageSize=20')
  record('经销商列表', dealerList.data.code === 200, `共 ${dealerList.data.data?.total || 0} 个经销商`)

  // 获取已有经销商ID供运维人员使用
  const firstDealer = dealerList.data.data?.records?.[0] || dealerList.data.data?.[0]
  const existingDealerId = firstDealer?.id

  const dealerCreate = await api('POST', '/dealers', {
    dealerCode: 'D' + ts,
    dealerName: 'TestDealerCRUD',
    contactPhone: '13900139001',
    phone: '13900139001',
    address: 'test address',
    dealerLevel: 'L1',
    password: 'Dealer@123'
  })
  const dealerId = getId(dealerCreate.data.data)
  record('经销商创建', dealerCreate.data.code === 200, `经销商ID: ${dealerId || 'N/A'} - ${dealerCreate.data.msg || ''}`)

  if (dealerId) {
    const dealerDelete = await api('DELETE', `/dealers/${dealerId}`)
    record('经销商删除', dealerDelete.data.code === 200, `${dealerDelete.data.msg || 'OK'}`)
  }

  // ============ 6. 运维人员 ============
  console.log('\n--- 6. 运维人员 ---')

  const workerList = await api('GET', '/workers?pageNum=1&pageSize=20')
  record('运维人员列表', workerList.data.code === 200, `共 ${workerList.data.data?.total || 0} 个运维人员`)

  // 使用已有经销商ID创建运维人员
  const workerDealerId = dealerId || existingDealerId || 1
  const workerCreate = await api('POST', '/workers', {
    name: 'TestWorkerCRUD',
    phone: '13700000001',
    dealerId: workerDealerId,
    workType: 'INSTALL',
    password: 'Worker@123'
  })
  const workerId = getId(workerCreate.data.data)
  record('运维人员创建', workerCreate.data.code === 200, `运维ID: ${workerId || 'N/A'} - ${workerCreate.data.msg || ''}`)

  if (workerId) {
    const workerDelete = await api('DELETE', `/workers/${workerId}`)
    record('运维人员删除', workerDelete.data.code === 200, `${workerDelete.data.msg || 'OK'}`)
  }

  // ============ 7. 工单管理 ============
  console.log('\n--- 7. 工单管理 ---')

  const workOrderList = await api('GET', '/work-orders?pageNum=1&pageSize=20')
  record('工单列表', workOrderList.data.code === 200, `共 ${workOrderList.data.data?.total || 0} 个工单`)

  // 工单创建（需要 dealerId, deviceId, customerId）
  const firstDev = deviceList.data.data?.records?.[0] || deviceList.data.data?.[0]
  const woDeviceSn = firstDev?.sn || firstDev?.deviceId || '25'
  const woDeviceId = firstDev?.id || firstDev?.deviceId || '1'
  const woDealerId = dealerList.data.data?.records?.[0]?.id || dealerList.data.data?.[0]?.id || '1'
  const woCustomerId = customerList.data.data?.records?.[0]?.id || customerList.data.data?.[0]?.id || '1'
  const woCreate = await api('POST', '/work-orders', {
    deviceSn: woDeviceSn,
    deviceId: woDeviceId,
    dealerId: woDealerId,
    customerId: woCustomerId,
    orderType: 'MAINTENANCE',
    description: 'test work order',
    priority: 1
  })
  const woId = getId(woCreate.data.data)
  record('工单创建', woCreate.data.code === 200, `工单ID: ${woId || 'N/A'} - ${woCreate.data.message || ''}`)

  if (woId) {
    const woDelete = await api('DELETE', `/work-orders/${woId}`)
    record('工单删除', woDelete.data.code === 200, `${woDelete.data.message || 'OK'}`)
  }

  // ============ 8. 套餐管理 ============
  console.log('\n--- 8. 套餐管理 ---')

  const pkgList = await api('GET', '/packages?pageNum=1&pageSize=20')
  record('套餐列表', pkgList.data.code === 200, `共 ${pkgList.data.data?.total || 0} 个套餐`)

  // 检查套餐ID是否为String
  const pkgIdType = pkgList.data.data?.records?.[0]?.id || pkgList.data.data?.[0]?.id
  record('套餐ID序列化为String', typeof pkgIdType === 'string', `id=${pkgIdType}, type=${typeof pkgIdType}`)

  const pkgCreate = await api('POST', '/packages', {
    packageName: 'test-pkg-crud-' + Date.now(),
    packageType: 'MONTHLY',
    price: 99.00,
    waterQuota: 200,
    description: 'test'
  })
  const pkgId = getId(pkgCreate.data.data)
  record('套餐创建', pkgCreate.data.code === 200, `套餐ID: ${pkgId || 'N/A'} - ${pkgCreate.data.msg || ''}`)

  if (pkgId) {
    const pkgUpdate = await api('PUT', `/packages/${pkgId}`, {
      packageName: '测试套餐CRUD2-已修改',
      packageType: 'MONTHLY',
      price: 129.00,
      waterQuota: 250,
      description: '修改后的月度套餐'
    })
    record('套餐修改', pkgUpdate.data.code === 200, `${pkgUpdate.data.msg || 'OK'}`)

    const pkgDelete = await api('DELETE', `/packages/${pkgId}`)
    record('套餐删除', pkgDelete.data.code === 200, `${pkgDelete.data.msg || 'OK'}`)
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

  const commissionList = await api('GET', '/finance/commissions?pageNum=1&pageSize=20')
  record('分润记录列表', commissionList.data.code === 200, `共 ${commissionList.data.data?.total || 0} 条分润`)

  const settlementList = await api('GET', '/finance/settlements?pageNum=1&pageSize=20')
  record('结算账单列表', settlementList.data.code === 200, `共 ${settlementList.data.data?.total || 0} 条结算`)

  const financeSummary = await api('GET', '/finance/summary')
  record('财务汇总', financeSummary.data.code === 200, `${financeSummary.data.msg || 'OK'}`)

  // 发票创建
  const invoiceCreate = await api('POST', '/finance/invoices', {
    customerId: 1,
    amount: 100000,
    title: '测试发票-公司名称',
    taxNo: '91520100TEST12345X',
    mailingAddress: '贵阳市观山湖区'
  })
  const invoiceId = getId(invoiceCreate.data.data)
  record('发票创建', invoiceCreate.data.code === 200, `发票: ${invoiceId || 'N/A'} - ${invoiceCreate.data.msg || ''}`)

  // ============ 11. 库存管理 ============
  console.log('\n--- 11. 库存管理 ---')

  const stockDevices = await api('GET', '/inventory/devices?pageNum=1&pageSize=20')
  record('设备库存列表', stockDevices.data.code === 200, `共 ${stockDevices.data.data?.total || 0} 条库存 - ${stockDevices.data.msg || ''}`)

  const stockStats = await api('GET', '/inventory/statistics')
  record('库存统计', stockStats.data.code === 200, `${stockStats.data.msg || 'OK'}`)

  const batchList = await api('GET', '/inventory/batches?pageNum=1&pageSize=20')
  record('批次列表', batchList.data.code === 200, `共 ${batchList.data.data?.total || 0} 个批次 - ${batchList.data.msg || ''}`)

  // ============ 12. 监控预警 ============
  console.log('\n--- 12. 监控预警 ---')

  const alertList = await api('GET', '/monitor/alerts?pageNum=1&pageSize=20')
  record('告警列表', alertList.data.code === 200, `共 ${alertList.data.data?.total || 0} 条告警`)

  // ============ 13. 报表中心 ============
  console.log('\n--- 13. 报表中心 ---')

  const dashboard = await api('GET', '/reports/dashboard')
  record('仪表盘数据', dashboard.data.code === 200, `设备总数: ${dashboard.data.data?.totalDevices || 'N/A'}`)

  const flowReport = await api('GET', '/reports/flow')
  record('流量报表数据', flowReport.data.code === 200, `在线率: ${flowReport.data.data?.onlineRate || 'N/A'}`)

  const reportDevices = await api('GET', '/reports/devices?pageNum=1&pageSize=10')
  record('设备报表', reportDevices.data.code === 200, `${reportDevices.data.msg || 'OK'}`)

  const reportOrders = await api('GET', '/reports/orders?pageNum=1&pageSize=10')
  record('订单报表', reportOrders.data.code === 200, `${reportOrders.data.msg || 'OK'}`)

  const reportFinance = await api('GET', '/reports/finance?pageNum=1&pageSize=10')
  record('财务报表', reportFinance.data.code === 200, `${reportFinance.data.msg || 'OK'}`)

  const reportWorkers = await api('GET', '/reports/workers?pageNum=1&pageSize=10')
  record('运维报表', reportWorkers.data.code === 200, `${reportWorkers.data.msg || 'OK'}`)

  // ============ 14. 推送服务 ============
  console.log('\n--- 14. 推送服务 ---')

  const pushUnpushed = await api('GET', '/push/unpushed?pageNum=1&pageSize=10')
  record('未推送告警列表', pushUnpushed.data.code === 200, `共 ${pushUnpushed.data.data?.total || 0} 条 - ${pushUnpushed.data.msg || ''}`)

  const pushStats = await api('GET', '/push/stats')
  record('推送统计', pushStats.data.code === 200, `${pushStats.data.msg || 'OK'}`)

  // ============ 15. IoT 服务 ============
  console.log('\n--- 15. IoT 服务 ---')

  // 获取一个设备SN用于测试
  const firstDevice = deviceList.data.data?.records?.[0] || deviceList.data.data?.[0]
  const deviceSn = firstDevice?.sn || firstDevice?.deviceId || 'TEST-001'

  const iotLatest = await api('GET', `/iot/devices/${deviceSn}/latest`)
  record('设备最新遥测', iotLatest.status === 200, `${iotLatest.data.message || 'OK'}`)

  const iotOnline = await api('GET', `/iot/devices/${deviceSn}/online-status`)
  record('设备在线状态', iotOnline.status === 200, `online=${iotOnline.data.online}`)

  // ============ 汇总 ============
  console.log('\n========== 测试汇总 ==========')

  // Group by module
  const modules = {}
  const moduleNames = {
    '0': '认证', '1': '系统管理', '2': '设备管理', '3': '滤芯管理',
    '4': '客户管理', '5': '经销商管理', '6': '运维人员', '7': '工单管理',
    '8': '套餐管理', '9': '订单管理', '10': '财务管理', '11': '库存管理',
    '12': '监控预警', '13': '报表中心', '14': '推送服务', '15': 'IoT服务'
  }

  let currentModule = ''
  results.forEach((r, i) => {
    // Determine module from test name (first number in section)
    if (r.name.startsWith('0') || r.name.includes('登录')) currentModule = '0'
    else if (r.name.startsWith('1') || r.name.includes('角色') || r.name.includes('账户') || r.name.includes('审计') || r.name.includes('配置') || r.name.includes('ID序列')) currentModule = '1'
    else if (r.name.startsWith('2') || r.name.includes('设备') && !r.name.includes('库存') && !r.name.includes('遥测') && !r.name.includes('在线')) currentModule = '2'
    else if (r.name.startsWith('3') || r.name.includes('滤芯')) currentModule = '3'
    else if (r.name.startsWith('4') || r.name.includes('客户')) currentModule = '4'
    else if (r.name.startsWith('5') || r.name.includes('经销商')) currentModule = '5'
    else if (r.name.startsWith('6') || r.name.includes('运维')) currentModule = '6'
    else if (r.name.startsWith('7') || r.name.includes('工单')) currentModule = '7'
    else if (r.name.startsWith('8') || r.name.includes('套餐')) currentModule = '8'
    else if (r.name.startsWith('9') || r.name.includes('订单') || r.name.includes('充值')) currentModule = '9'
    else if (r.name.startsWith('1 0') || r.name.includes('发票') || r.name.includes('分润') || r.name.includes('结算') || r.name.includes('财务汇总') || r.name.includes('财务')) currentModule = '10'
    else if (r.name.includes('库存') || r.name.includes('批次')) currentModule = '11'
    else if (r.name.includes('告警') || r.name.includes('监控')) currentModule = '12'
    else if (r.name.includes('报表') || r.name.includes('仪表') || r.name.includes('流量')) currentModule = '13'
    else if (r.name.includes('推送')) currentModule = '14'
    else if (r.name.includes('遥测') || r.name.includes('在线状态') || r.name.includes('IoT') || r.name.includes('iot')) currentModule = '15'

    if (!modules[currentModule]) modules[currentModule] = { pass: 0, fail: 0 }
    if (r.status === 'PASS') modules[currentModule].pass++
    else modules[currentModule].fail++
  })

  Object.keys(modules).sort().forEach(mod => {
    const counts = modules[mod]
    const total = counts.pass + counts.fail
    const rate = ((counts.pass / total) * 100).toFixed(0)
    const name = moduleNames[mod] || mod
    const warning = counts.fail > 0 ? `  ⚠️ ${counts.fail} FAILED` : ''
    console.log(`  ${name}: ${counts.pass}/${total} (${rate}%)${warning}`)
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
