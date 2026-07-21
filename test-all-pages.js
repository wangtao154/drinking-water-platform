const { chromium } = require('playwright');
const fs = require('fs');

const BASE_URL = 'http://localhost:8088';
const GATEWAY_URL = 'http://localhost:8080';
const ADMIN_USER = 'admin';
const ADMIN_PASS = 'admin123';

const results = [];
let page, browser, context;

async function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }

async function login() {
  console.log('=== Step 1: Login ===');
  browser = await chromium.launch({ headless: true, args: ['--no-sandbox'] });
  context = await browser.newContext({ viewport: { width: 1920, height: 1080 } });
  page = await context.newPage();

  await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
  await sleep(2000);

  const accountInput = page.locator('input[placeholder*="账号"], input[placeholder*="用户"], input[type="text"]').first();
  const passInput = page.locator('input[type="password"]').first();
  await accountInput.fill(ADMIN_USER);
  await passInput.fill(ADMIN_PASS);
  await sleep(500);

  const loginBtn = page.locator('button:has-text("登录"), button:has-text("登 录"), button[type="submit"]').first();
  await loginBtn.click();
  await sleep(3000);

  const currentUrl = page.url();
  const loginSuccess = currentUrl.includes('/dashboard') || !currentUrl.includes('/login');
  console.log(`  Current URL: ${currentUrl} | ${loginSuccess ? 'PASS' : 'FAIL'}`);
  results.push({ module: 'Login', page: '登录', route: '/login', status: loginSuccess ? 'PASS' : 'FAIL', note: loginSuccess ? 'Redirected to dashboard' : `URL: ${currentUrl}` });
  return loginSuccess;
}

async function testPage(moduleName, pageName, route) {
  console.log(`\n--- ${moduleName} > ${pageName} ---`);
  try {
    const url = `${BASE_URL}${route}`;
    
    // Track API responses on this page
    const apiResults = [];
    const responseHandler = (response) => {
      const reqUrl = response.url();
      if (reqUrl.includes('/api/v1/')) {
        apiResults.push({ url: reqUrl.replace(GATEWAY_URL, ''), status: response.status() });
      }
    };
    page.on('response', responseHandler);

    await page.goto(url, { waitUntil: 'networkidle', timeout: 20000 });
    await sleep(3000);

    // Remove handler to avoid duplicate entries
    page.off('response', responseHandler);

    // Check page content using evaluate
    const pageInfo = await page.evaluate(() => {
      const getTableRows = () => document.querySelectorAll('.el-table__body-wrapper tr').length;
      const hasError = document.querySelector('.el-message--error') !== null;
      const errorText = hasError ? document.querySelector('.el-message--error')?.textContent : '';
      const hasTable = document.querySelector('.el-table') !== null;
      const hasForm = document.querySelector('.el-form') !== null;
      const hasCard = document.querySelector('.el-card') !== null;
      const hasEmpty = document.querySelector('.el-empty') !== null;
      const emptyText = hasEmpty ? document.querySelector('.el-empty')?.textContent?.trim() : '';
      const hasCharts = document.querySelectorAll('canvas, [_echarts_instance_]').length;
      const hasPagination = document.querySelector('.el-pagination') !== null;
      const pageTitle = document.querySelector('.el-page-header__title, .app-container h2, h2')?.textContent?.trim() || '';
      const is404 = document.body.textContent.includes('404') && document.body.textContent.includes('找不到');
      const statCards = document.querySelectorAll('.stat-card, .el-col .el-card').length;
      
      // Get table column headers
      const tableHeaders = Array.from(document.querySelectorAll('.el-table__header th .cell')).map(th => th.textContent?.trim()).filter(t => t);
      
      return { getTableRows, hasError, errorText, hasTable, hasForm, hasCard, hasEmpty, emptyText, 
               hasCharts, hasPagination, pageTitle, is404, statCards, tableHeaders };
    });

    // Take screenshot
    const screenshotName = route.replace(/\//g, '_').replace(/^_/, '') + '.png';
    await page.screenshot({ path: `test-screenshots/${screenshotName}`, fullPage: true });

    let status = 'PASS';
    let notes = [];

    if (pageInfo.is404) {
      status = 'FAIL';
      notes.push('404 page');
    } else if (pageInfo.hasError) {
      status = 'WARN';
      notes.push(`Error: ${pageInfo.errorText}`);
    }

    if (pageInfo.hasTable) notes.push(`${pageInfo.getTableRows} rows`);
    if (pageInfo.hasCharts > 0) notes.push(`${pageInfo.hasCharts} charts`);
    if (pageInfo.hasForm) notes.push('form');
    if (pageInfo.hasEmpty) notes.push(`empty: ${pageInfo.emptyText?.substring(0, 20)}`);
    if (pageInfo.hasPagination) notes.push('pagination');
    if (pageInfo.statCards > 0) notes.push(`${pageInfo.statCards} stat cards`);
    if (pageInfo.tableHeaders.length > 0) notes.push(`cols: ${pageInfo.tableHeaders.slice(0, 5).join('/')}`);

    // Check API results for this page
    const failedApis = apiResults.filter(a => a.status >= 400);
    if (failedApis.length > 0) {
      status = status === 'PASS' ? 'WARN' : status;
      notes.push(`API errors: ${failedApis.map(a => `${a.url.split('/api/v1/')[1]?.split('?')[0]}=${a.status}`).join(', ')}`);
    }
    const okApis = apiResults.filter(a => a.status === 200);

    const noteStr = notes.join(', ');
    console.log(`  ${status} | ${noteStr}`);
    if (apiResults.length > 0) {
      console.log(`  APIs: ${apiResults.length} total, ${okApis.length} OK, ${failedApis.length} failed`);
    }

    results.push({
      module: moduleName, page: pageName, route, status,
      note: noteStr,
      tableRows: pageInfo.getTableRows,
      hasCharts: pageInfo.hasCharts > 0,
      hasForm: pageInfo.hasForm,
      apiCount: apiResults.length,
      apiFailed: failedApis.length,
      apiFailedList: failedApis.map(a => a.url)
    });

    return { status, tableRows: pageInfo.getTableRows };
  } catch (e) {
    console.log(`  FAIL | ${e.message}`);
    results.push({ module: moduleName, page: pageName, route, status: 'FAIL', note: e.message });
    return { status: 'FAIL', tableRows: 0 };
  }
}

async function testApiDirectly() {
  console.log('\n=== Direct API Tests ===');
  
  const loginResp = await fetch(`${GATEWAY_URL}/api/v1/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ account: ADMIN_USER, password: ADMIN_PASS })
  });
  const loginData = await loginResp.json();
  const token = loginData.data?.accessToken || loginData.data?.token;
  
  if (!token) { console.log('  Failed to get token'); return; }
  console.log('  Got auth token');

  const apiTests = [
    // Report service (uses /api/v1/reports/**)
    { module: 'Dashboard', url: '/api/v1/reports/dashboard', desc: '仪表盘数据' },
    { module: 'Report', url: '/api/v1/reports/devices', desc: '设备报表' },
    { module: 'Report', url: '/api/v1/reports/orders', desc: '订单报表' },
    { module: 'Report', url: '/api/v1/reports/finance', desc: '财务报表' },
    { module: 'Report', url: '/api/v1/reports/workers', desc: '工单报表' },
    // Device service
    { module: 'Device', url: '/api/v1/devices?pageNum=1&pageSize=10', desc: '设备列表' },
    { module: 'DeviceModel', url: '/api/v1/device-models?pageNum=1&pageSize=10', desc: '设备型号' },
    // Filter service
    { module: 'Filter', url: '/api/v1/filters?pageNum=1&pageSize=10', desc: '滤芯列表' },
    { module: 'FilterModel', url: '/api/v1/filter-models?pageNum=1&pageSize=10', desc: '滤芯型号' },
    // User service
    { module: 'Customer', url: '/api/v1/customers?pageNum=1&pageSize=10', desc: '客户列表' },
    { module: 'Dealer', url: '/api/v1/dealers?pageNum=1&pageSize=10', desc: '经销商列表' },
    { module: 'Worker', url: '/api/v1/workers?pageNum=1&pageSize=10', desc: '运维人员列表' },
    // Worker-order service
    { module: 'WorkOrder', url: '/api/v1/work-orders?pageNum=1&pageSize=10', desc: '工单列表' },
    // Order service
    { module: 'Order', url: '/api/v1/orders?pageNum=1&pageSize=10', desc: '订单列表' },
    // Package service
    { module: 'Package', url: '/api/v1/packages?pageNum=1&pageSize=10', desc: '套餐列表' },
    // Inventory service
    { module: 'Inventory', url: '/api/v1/inventory/devices?pageNum=1&pageSize=10', desc: '设备库存' },
    { module: 'InventoryBatch', url: '/api/v1/inventory/batches?pageNum=1&pageSize=10', desc: '进货批次' },
    // Finance service
    { module: 'Finance', url: '/api/v1/finance/settlements?pageNum=1&pageSize=10', desc: '结算账单' },
    { module: 'Finance', url: '/api/v1/finance/commissions?pageNum=1&pageSize=10', desc: '分佣明细' },
    { module: 'Finance', url: '/api/v1/finance/invoices?pageNum=1&pageSize=10', desc: '发票管理' },
    // Payment service
    { module: 'Payment', url: '/api/v1/payments/refunds?pageNum=1&pageSize=10', desc: '退款管理' },
    // Monitor service
    { module: 'Monitor', url: '/api/v1/monitor/alerts?pageNum=1&pageSize=10', desc: '告警列表' },
    { module: 'Monitor', url: '/api/v1/monitor/thresholds?pageNum=1&pageSize=10', desc: '阈值管理' },
    // System service
    { module: 'System', url: '/api/v1/system/configs?pageNum=1&pageSize=10', desc: '系统配置' },
    { module: 'System', url: '/api/v1/system/audit-logs?pageNum=1&pageSize=10', desc: '审计日志' },
  ];

  for (const api of apiTests) {
    try {
      const resp = await fetch(`${GATEWAY_URL}${api.url}`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const status = resp.status;
      let records = 0;
      let detail = '';

      if (status === 200) {
        try {
          const data = await resp.json();
          if (data.data) {
            if (data.data.records) records = data.data.records.length;
            else if (Array.isArray(data.data)) records = data.data.length;
            else if (data.data.total !== undefined) records = data.data.total;
            else records = Object.keys(data.data).length;
          }
          detail = data.code === 200 ? `${records} records` : `code=${data.code} msg=${data.message || ''}`.substring(0, 80);
        } catch (e) {
          detail = 'Non-JSON';
        }
      } else {
        try {
          const errorBody = await resp.json();
          detail = errorBody.message || errorBody.error || '';
          detail = detail.substring(0, 80);
        } catch (e) {
          detail = resp.statusText;
        }
      }

      const pass = status === 200;
      const icon = pass ? '✅' : '❌';
      console.log(`  ${icon} ${status} | ${api.module.padEnd(15)} | ${api.desc.padEnd(12)} | ${detail}`);
      results.push({
        module: api.module + '(API)', page: api.desc, route: api.url,
        status: pass ? 'PASS' : 'FAIL', note: `HTTP ${status}, ${detail}`
      });
    } catch (e) {
      console.log(`  ❌ ERR  | ${api.module.padEnd(15)} | ${api.desc.padEnd(12)} | ${e.message.substring(0, 80)}`);
      results.push({ module: api.module + '(API)', page: api.desc, route: api.url, status: 'FAIL', note: e.message });
    }
  }
}

async function runTests() {
  if (!fs.existsSync('test-screenshots')) fs.mkdirSync('test-screenshots');

  const loginOk = await login();
  if (!loginOk) { console.log('Login failed!'); await browser.close(); return; }

  // Dashboard
  await testPage('仪表盘', '仪表盘', '/dashboard');

  // Device
  await testPage('设备管理', '设备列表', '/devices/list');
  await testPage('设备管理', '设备登记', '/devices/register');

  // Filter
  await testPage('滤芯管理', '滤芯列表', '/filters/list');
  await testPage('滤芯管理', '滤芯入库', '/filters/inbound');

  // Customer
  await testPage('客户管理', '客户列表', '/customers/list');

  // Dealer
  await testPage('经销商管理', '经销商列表', '/dealers/list');

  // Worker
  await testPage('运维人员', '运维人员列表', '/workers/list');

  // WorkOrder
  await testPage('工单管理', '工单列表', '/work-orders/list');

  // Order
  await testPage('订单管理', '订单列表', '/orders/list');
  await testPage('订单管理', '异常订单', '/orders/abnormal');

  // Package
  await testPage('套餐管理', '套餐列表', '/packages/list');

  // Inventory
  await testPage('库存管理', '设备库存', '/inventory/devices');
  await testPage('库存管理', '进货批次', '/inventory/batches');

  // Finance
  await testPage('财务管理', '结算账单', '/finance/settlements');
  await testPage('财务管理', '分佣明细', '/finance/commissions');
  await testPage('财务管理', '发票管理', '/finance/invoices');
  await testPage('财务管理', '退款管理', '/finance/refunds');

  // Report
  await testPage('报表中心', '水质报表', '/reports/water-quality');
  await testPage('报表中心', '营收报表', '/reports/revenue');
  await testPage('报表中心', '工单报表', '/reports/work-order');

  // Monitor
  await testPage('监控预警', '告警列表', '/monitor/alerts');
  await testPage('监控预警', '阈值管理', '/monitor/thresholds');

  // System
  await testPage('系统管理', '系统配置', '/system/configs');
  await testPage('系统管理', '审计日志', '/system/audit-logs');

  // API Tests
  await testApiDirectly();

  await browser.close();
  generateReport();
}

function generateReport() {
  const stats = { PASS: 0, FAIL: 0, WARN: 0 };
  for (const r of results) { if (stats[r.status] !== undefined) stats[r.status]++; }

  // Group by module
  const modules = {};
  for (const r of results) {
    if (!modules[r.module]) modules[r.module] = [];
    modules[r.module].push(r);
  }

  const reportHtml = generateHtmlReport(results, stats, modules);
  fs.writeFileSync('test-report.html', reportHtml);
  fs.writeFileSync('test-results.json', JSON.stringify({ timestamp: new Date().toISOString(), stats, results }, null, 2));

  console.log('\n\n========== TEST SUMMARY ==========');
  console.log(`Total: ${results.length} | PASS: ${stats.PASS} | WARN: ${stats.WARN} | FAIL: ${stats.FAIL}`);
  console.log(`Pass Rate: ${((stats.PASS / results.length) * 100).toFixed(1)}%`);
  console.log('Report saved to test-report.html and test-results.json\n');
}

function generateHtmlReport(results, stats, modules) {
  let html = `<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>直饮水平台 - 前端全量功能测试报告</title>
<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #f0f2f5; color: #303133; }
.header { background: linear-gradient(135deg, #409eff, #2d8cf0); color: white; padding: 30px 40px; }
.header h1 { font-size: 24px; margin-bottom: 8px; }
.header .info { font-size: 14px; opacity: 0.9; }
.summary { display: flex; gap: 20px; padding: 20px 40px; }
.stat-card { background: white; border-radius: 8px; padding: 20px 30px; box-shadow: 0 2px 8px rgba(0,0,0,0.08); flex: 1; text-align: center; }
.stat-card .num { font-size: 32px; font-weight: bold; }
.stat-card .label { font-size: 14px; color: #909399; margin-top: 4px; }
.stat-card.pass .num { color: #67c23a; }
.stat-card.fail .num { color: #f56c6c; }
.stat-card.warn .num { color: #e6a23c; }
.stat-card.total .num { color: #409eff; }
.content { padding: 0 40px 40px; }
.module { background: white; border-radius: 8px; margin-bottom: 16px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.module-header { padding: 14px 20px; font-size: 16px; font-weight: 600; border-bottom: 1px solid #ebeef5; display: flex; justify-content: space-between; align-items: center; }
.module-header .badge { font-size: 12px; padding: 2px 10px; border-radius: 10px; }
table { width: 100%; border-collapse: collapse; }
th { background: #f5f7fa; padding: 10px 16px; text-align: left; font-size: 13px; color: #909399; font-weight: 500; }
td { padding: 10px 16px; border-top: 1px solid #ebeef5; font-size: 13px; }
.status-pass { color: #67c23a; font-weight: 600; }
.status-fail { color: #f56c6c; font-weight: 600; }
.status-warn { color: #e6a23c; font-weight: 600; }
.note { color: #606266; max-width: 400px; }
.route { color: #909399; font-family: monospace; }
</style>
</head>
<body>
<div class="header">
<h1>直饮水平台 - 前端全量功能测试报告</h1>
<div class="info">测试时间: ${new Date().toLocaleString('zh-CN')} | 前端: ${BASE_URL} | 网关: ${GATEWAY_URL}</div>
</div>
<div class="summary">
<div class="stat-card total"><div class="num">${results.length}</div><div class="label">总测试数</div></div>
<div class="stat-card pass"><div class="num">${stats.PASS}</div><div class="label">通过 ✅</div></div>
<div class="stat-card warn"><div class="num">${stats.WARN}</div><div class="label">警告 ⚠️</div></div>
<div class="stat-card fail"><div class="num">${stats.FAIL}</div><div class="label">失败 ❌</div></div>
<div class="stat-card total"><div class="num">${((stats.PASS / results.length) * 100).toFixed(1)}%</div><div class="label">通过率</div></div>
</div>
<div class="content">
`;

  for (const [mod, items] of Object.entries(modules)) {
    const modPass = items.filter(i => i.status === 'PASS').length;
    const modFail = items.filter(i => i.status === 'FAIL').length;
    const badgeClass = modFail === 0 ? 'background:#f0f9eb;color:#67c23a' : 'background:#fef0f0;color:#f56c6c';
    html += `<div class="module">
<div class="module-header">${mod} <span class="badge" style="${badgeClass}">${modPass}/${items.length} 通过</span></div>
<table>
<thead><tr><th width="60">状态</th><th>页面</th><th width="120">路由</th><th>备注</th></tr></thead>
<tbody>`;
    for (const item of items) {
      const cls = item.status === 'PASS' ? 'status-pass' : item.status === 'WARN' ? 'status-warn' : 'status-fail';
      const icon = item.status === 'PASS' ? '✅' : item.status === 'WARN' ? '⚠️' : '❌';
      html += `<tr><td class="${cls}">${icon} ${item.status}</td><td>${item.page}</td><td class="route">${item.route}</td><td class="note">${item.note || ''}</td></tr>`;
    }
    html += `</tbody></table></div>`;
  }

  html += `</div></body></html>`;
  return html;
}

runTests().catch(e => { console.error('Error:', e); process.exit(1); });
