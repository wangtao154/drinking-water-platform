const { chromium } = require('playwright')

const BASE_URL = 'http://localhost:8088'

async function main() {
  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage({ viewport: { width: 1400, height: 900 } })

  const results = []
  let passCount = 0
  let failCount = 0

  // 1. Login
  await page.goto(BASE_URL + '/login')
  await page.waitForTimeout(2000)
  await page.fill('input[placeholder*="账号"]', 'admin')
  await page.fill('input[placeholder*="密码"]', 'admin123')
  await page.click('button.el-button--primary')
  await page.waitForURL('**/dashboard', { timeout: 15000 })
  results.push({ test: 'Login -> dashboard', status: 'PASS', detail: page.url() })
  passCount++

  // 2. Click dashboard menu from dashboard page
  await page.click('text=仪表盘')
  await page.waitForTimeout(1000)
  const url2 = page.url()
  if (url2.includes('/dashboard') && !url2.includes('/404')) {
    results.push({ test: 'From dashboard -> click dashboard menu', status: 'PASS', detail: url2 })
    passCount++
  } else {
    results.push({ test: 'From dashboard -> click dashboard menu', status: 'FAIL', detail: url2 })
    failCount++
  }

  // 3. Navigate to device list, then click dashboard menu
  await page.goto(BASE_URL + '/devices/list')
  await page.waitForTimeout(1000)
  await page.click('text=仪表盘')
  await page.waitForTimeout(1500)
  const url3 = page.url()
  if (url3.includes('/dashboard') && !url3.includes('/404')) {
    results.push({ test: 'From /devices/list -> click dashboard menu', status: 'PASS', detail: url3 })
    passCount++
  } else {
    results.push({ test: 'From /devices/list -> click dashboard menu', status: 'FAIL', detail: url3 })
    failCount++
  }

  // 4. Navigate to system/roles, then click dashboard menu
  await page.goto(BASE_URL + '/system/roles')
  await page.waitForTimeout(1000)
  await page.click('text=仪表盘')
  await page.waitForTimeout(1500)
  const url4 = page.url()
  if (url4.includes('/dashboard') && !url4.includes('/404')) {
    results.push({ test: 'From /system/roles -> click dashboard menu', status: 'PASS', detail: url4 })
    passCount++
  } else {
    results.push({ test: 'From /system/roles -> click dashboard menu', status: 'FAIL', detail: url4 })
    failCount++
  }

  // 5. Navigate to finance/records, then click dashboard menu
  await page.goto(BASE_URL + '/finance/records')
  await page.waitForTimeout(1000)
  await page.click('text=仪表盘')
  await page.waitForTimeout(1500)
  const url5 = page.url()
  if (url5.includes('/dashboard') && !url5.includes('/404')) {
    results.push({ test: 'From /finance/records -> click dashboard menu', status: 'PASS', detail: url5 })
    passCount++
  } else {
    results.push({ test: 'From /finance/records -> click dashboard menu', status: 'FAIL', detail: url5 })
    failCount++
  }

  // 6. Navigate to reports/flow, then click dashboard menu
  await page.goto(BASE_URL + '/reports/flow')
  await page.waitForTimeout(1000)
  await page.click('text=仪表盘')
  await page.waitForTimeout(1500)
  const url6 = page.url()
  if (url6.includes('/dashboard') && !url6.includes('/404')) {
    results.push({ test: 'From /reports/flow -> click dashboard menu', status: 'PASS', detail: url6 })
    passCount++
  } else {
    results.push({ test: 'From /reports/flow -> click dashboard menu', status: 'FAIL', detail: url6 })
    failCount++
  }

  // 7. Navigate to monitor/alerts, then click dashboard menu
  await page.goto(BASE_URL + '/monitor/alerts')
  await page.waitForTimeout(1000)
  await page.click('text=仪表盘')
  await page.waitForTimeout(1500)
  const url7 = page.url()
  if (url7.includes('/dashboard') && !url7.includes('/404')) {
    results.push({ test: 'From /monitor/alerts -> click dashboard menu', status: 'PASS', detail: url7 })
    passCount++
  } else {
    results.push({ test: 'From /monitor/alerts -> click dashboard menu', status: 'FAIL', detail: url7 })
    failCount++
  }

  // Output
  console.log('\n========== Dashboard Navigation Test ==========')
  results.forEach(r => {
    const icon = r.status === 'PASS' ? '[PASS]' : '[FAIL]'
    console.log(`${icon} ${r.test}`)
    console.log(`       URL: ${r.detail}`)
  })
  console.log('==============================================')
  console.log(`Total: ${passCount} PASS / ${failCount} FAIL`)
  console.log(`Pass Rate: ${((passCount / (passCount + failCount)) * 100).toFixed(1)}%`)

  await browser.close()
}

main().catch(err => {
  console.error('Test error:', err)
  process.exit(1)
})
