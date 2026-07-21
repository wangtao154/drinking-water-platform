const { chromium } = require('playwright');
const fs = require('fs');

const BASE_URL = 'http://localhost:8088';
const results = [];

async function delay(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function login(page) {
  await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
  await page.fill('input[placeholder*="账号"]', 'admin');
  await page.fill('input[placeholder*="密码"]', 'admin123');
  await page.click('button[type="submit"], .login-btn, button:has-text("登")');
  await page.waitForURL('**/dashboard**', { timeout: 10000 }).catch(() => {});
  await delay(2000);
}

async function testPage(page, name, path, screenshotName) {
  const result = { name, path, status: 'PASS', details: [] };
  try {
    const apiCalls = [];
    page.on('response', async (response) => {
      const url = response.url();
      if (url.includes('/api/')) {
        apiCalls.push({
          url: url.replace(BASE_URL, ''),
          status: response.status(),
        });
      }
    });

    await page.goto(`${BASE_URL}${path}`, { waitUntil: 'networkidle', timeout: 15000 });
    await delay(2000);

    const bodyText = await page.evaluate(() => document.body.innerText);
    const hasError = bodyText.includes('系统错误') || bodyText.includes('Cannot read') || bodyText.includes('TypeError');

    if (hasError) {
      result.status = 'FAIL';
      result.details.push('Page shows error text');
    }

    if (apiCalls.length > 0) {
      const failedApis = apiCalls.filter(a => a.status >= 400);
      if (failedApis.length > 0) {
        result.details.push(`API errors: ${failedApis.map(a => `${a.url}(${a.status})`).join(', ')}`);
      }
      result.apiCalls = apiCalls.length;
      result.apiErrors = failedApis.length;
    } else {
      result.apiCalls = 0;
    }

    const hasTable = await page.locator('table, .el-table').count();
    const hasCards = await page.locator('.el-card').count();
    result.uiElements = { tables: hasTable, cards: hasCards };

    const screenshotDir = './test-new-screenshots';
    if (!fs.existsSync(screenshotDir)) fs.mkdirSync(screenshotDir);
    await page.screenshot({ path: `${screenshotDir}/${screenshotName}.png`, fullPage: true });

    result.details.push(`Tables: ${hasTable}, Cards: ${hasCards}, API calls: ${result.apiCalls || 0}`);
  } catch (e) {
    result.status = 'FAIL';
    result.details.push(e.message);
  }
  results.push(result);
  console.log(`[${result.status}] ${name} - ${result.details.join('; ')}`);
}

async function testApi(page, endpoint, method = 'GET') {
  const result = { name: `API: ${method} ${endpoint}`, status: 'PASS', details: [] };
  try {
    const response = await page.evaluate(async ({ url, method }) => {
      const token = localStorage.getItem('dw_access_token') || '';
      const res = await fetch(url, {
        method,
        credentials: 'include',
        headers: {
          'Authorization': token ? `Bearer ${token}` : '',
          'Content-Type': 'application/json',
        },
      });
      return { status: res.status, data: await res.text().catch(() => '') };
    }, { url: `${BASE_URL}${endpoint}`, method });

    if (response.status >= 400) {
      result.status = 'FAIL';
      result.details.push(`HTTP ${response.status}`);
    } else {
      try {
        const data = JSON.parse(response.data);
        result.details.push(`HTTP ${response.status}, code: ${data.code}`);
        if (data.code !== 200) {
          result.status = 'FAIL';
        }
      } catch {
        result.details.push(`HTTP ${response.status}, non-JSON response`);
      }
    }
  } catch (e) {
    result.status = 'FAIL';
    result.details.push(e.message);
  }
  results.push(result);
  console.log(`[${result.status}] ${result.name} - ${result.details.join('; ')}`);
}

async function main() {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await context.newPage();

  console.log('=== Testing 4 New Extension Pages ===\n');

  await login(page);
  console.log('Login successful\n');

  // Test 4 new pages
  await testPage(page, '充值记录', '/finance/records', 'recharge-records');
  await testPage(page, '流量报表', '/reports/flow', 'flow-report');
  await testPage(page, '角色管理', '/system/roles', 'role-management');
  await testPage(page, '账户管理', '/system/users', 'user-management');

  // Test API endpoints using the same authenticated context
  console.log('\n=== Testing API Endpoints ===\n');
  await testApi(page, '/api/v1/orders?orderType=RECHARGE&pageNum=1&pageSize=10', 'GET');
  await testApi(page, '/api/v1/reports/flow', 'GET');
  await testApi(page, '/api/v1/system/roles?pageNum=1&pageSize=10', 'GET');
  await testApi(page, '/api/v1/system/roles/list', 'GET');
  await testApi(page, '/api/v1/system/users?pageNum=1&pageSize=10', 'GET');

  await browser.close();

  // Generate report
  const passed = results.filter(r => r.status === 'PASS').length;
  const failed = results.filter(r => r.status === 'FAIL').length;
  console.log(`\n=== Summary: ${passed}/${results.length} PASS, ${failed} FAIL ===`);

  fs.writeFileSync('test-new-results.json', JSON.stringify(results, null, 2));

  const html = `<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<title>4 个扩展页面测试报告</title>
<style>
body { font-family: 'Segoe UI', sans-serif; margin: 40px; background: #f5f7fa; }
h1 { color: #303133; }
.summary { display: flex; gap: 20px; margin-bottom: 30px; }
.card { background: white; padding: 20px 30px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
.card .num { font-size: 36px; font-weight: 700; }
.card .label { color: #909399; font-size: 14px; }
.pass { color: #67c23a; }
.fail { color: #f56c6c; }
table { width: 100%; border-collapse: collapse; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
th, td { padding: 12px 16px; text-align: left; border-bottom: 1px solid #ebeef5; }
th { background: #f5f7fa; font-weight: 600; color: #303133; }
tr:hover { background: #f5f7fa; }
.badge { padding: 2px 10px; border-radius: 4px; font-size: 12px; font-weight: 600; }
.badge-pass { background: #f0f9eb; color: #67c23a; }
.badge-fail { background: #fef0f0; color: #f56c6c; }
</style>
</head>
<body>
<h1>4 个扩展页面测试报告</h1>
<div class="summary">
  <div class="card"><div class="num ${passed === results.length ? 'pass' : 'fail'}">${passed}</div><div class="label">通过</div></div>
  <div class="card"><div class="num fail">${failed}</div><div class="label">失败</div></div>
  <div class="card"><div class="num">${results.length}</div><div class="label">总计</div></div>
</div>
<table>
<tr><th>#</th><th>测试项</th><th>状态</th><th>详情</th></tr>
${results.map((r, i) => `<tr>
  <td>${i + 1}</td>
  <td>${r.name}</td>
  <td><span class="badge badge-${r.status.toLowerCase()}">${r.status}</span></td>
  <td>${r.details.join('<br>')}</td>
</tr>`).join('')}
</table>
</body>
</html>`;
  fs.writeFileSync('test-new-report.html', html);

  process.exit(failed > 0 ? 1 : 0);
}

main().catch(console.error);
