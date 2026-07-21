const { chromium } = require('playwright')

const BASE_URL = 'http://localhost:8088'

async function main() {
  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage({ viewport: { width: 1400, height: 900 } })

  const results = []
  let passCount = 0
  let failCount = 0

  function record(name, pass, detail) {
    results.push({ test: name, status: pass ? 'PASS' : 'FAIL', detail })
    if (pass) passCount++; else failCount++
  }

  // 1. Login
  await page.goto(BASE_URL + '/login')
  await page.waitForTimeout(2000)
  await page.fill('input[placeholder*="账号"]', 'admin')
  await page.fill('input[placeholder*="密码"]', 'admin123')
  await page.click('button.el-button--primary')
  await page.waitForURL('**/dashboard', { timeout: 15000 })
  record('Login', true, page.url())

  // 2. Navigate to device register page
  await page.goto(BASE_URL + '/devices/register')
  await page.waitForTimeout(2500)
  record('Navigate to device register page', page.url().includes('/devices/register'), page.url())

  // 3. Screenshot before interaction
  await page.screenshot({ path: 'test-register-1-before.png' })

  // 4. Click the device model select
  // Find the el-select in the form item with label "设备型号"
  const selectWrapper = page.locator('.el-form-item:has-text("设备型号") .el-select .el-input__wrapper, .el-form-item:has-text("设备型号") .el-select .el-select__wrapper')
  await selectWrapper.first().click()
  await page.waitForTimeout(1500)
  await page.screenshot({ path: 'test-register-2-dropdown.png' })

  // 5. Get dropdown options
  const options = await page.evaluate(() => {
    const items = document.querySelectorAll('.el-select-dropdown__item')
    return Array.from(items).map(item => item.textContent?.trim() || '').filter(t => t.length > 0)
  })
  record('Device model dropdown has options', options.length > 0, `Found ${options.length} options: ${options.join(', ')}`)

  // 6. Click the first option in dropdown
  await page.locator('.el-select-dropdown__item').first().click()
  await page.waitForTimeout(1000)
  await page.screenshot({ path: 'test-register-3-selected.png' })

  // 7. Check selected value via DOM
  const selectedValue = await page.evaluate(() => {
    // Check if there's a tag/pill showing the selection
    const tags = document.querySelectorAll('.el-select .el-select__selected-item, .el-select .el-select__placeholder.is-trans')
    const placeholderEl = document.querySelector('.el-select .el-select__placeholder')
    return {
      placeholderText: placeholderEl?.textContent || '',
      hasTag: document.querySelectorAll('.el-select .el-select__selected-item').length > 0,
    }
  })
  record('Select device model', selectedValue.hasTag || !selectedValue.placeholderText.includes('请选择'), JSON.stringify(selectedValue))

  // 8. Fill deviceId and SN
  await page.fill('.el-form-item:has-text("设备ID") input', 'DEV-TEST-001')
  await page.waitForTimeout(300)
  await page.fill('.el-form-item:has-text("SN") input', 'SN-TEST-001')
  await page.waitForTimeout(300)

  // 9. Submit
  await page.locator('.el-form .el-button--primary').click()
  await page.waitForTimeout(3000)
  await page.screenshot({ path: 'test-register-4-submit.png' })

  const finalUrl = page.url()
  record('Submit device registration', finalUrl.includes('/devices/list'), `URL: ${finalUrl}`)

  // 10. Check device list
  if (finalUrl.includes('/devices/list')) {
    await page.waitForTimeout(2000)
    const hasDevice = await page.evaluate(() => {
      return document.body.textContent?.includes('DEV-TEST-001')
    })
    record('Device appears in list', hasDevice, hasDevice ? 'DEV-TEST-001 found' : 'DEV-TEST-001 not found in list')
  }

  // Output results
  console.log('\n========== Device Register Test ==========')
  results.forEach(r => {
    const icon = r.status === 'PASS' ? '[PASS]' : '[FAIL]'
    console.log(`${icon} ${r.test}`)
    console.log(`       ${r.detail}`)
  })
  console.log('=========================================')
  console.log(`Total: ${passCount} PASS / ${failCount} FAIL`)
  console.log(`Pass Rate: ${((passCount / (passCount + failCount)) * 100).toFixed(1)}%`)

  await browser.close()
}

main().catch(err => {
  console.error('Test error:', err)
  process.exit(1)
})
