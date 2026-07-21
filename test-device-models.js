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

  // 2. Navigate to device models page
  await page.goto(BASE_URL + '/devices/models')
  await page.waitForTimeout(2500)
  record('Navigate to device models page', page.url().includes('/devices/models'), page.url())
  await page.screenshot({ path: 'test-models-1-list.png' })

  // 3. Check table has data
  const rowCount = await page.evaluate(() => {
    const rows = document.querySelectorAll('.el-table__body-wrapper .el-table__row')
    return rows.length
  })
  record('Table has device model data', rowCount > 0, `Found ${rowCount} rows`)

  // 4. Verify Chinese characters display correctly
  const firstRowText = await page.evaluate(() => {
    const firstRow = document.querySelector('.el-table__body-wrapper .el-table__row')
    return firstRow ? firstRow.textContent : ''
  })
  const hasCorrectChinese = firstRowText.includes('智饮') || firstRowText.includes('直饮水机')
  record('Chinese characters display correctly', hasCorrectChinese, firstRowText.substring(0, 80))

  // 5. Test creating a new device model
  // Intercept API response
  const createResponse = await page.evaluate(async () => {
    const token = localStorage.getItem('dw_access_token')
    const res = await fetch('/api/v1/device-models', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        modelCode: 'DW-TEST-001',
        modelName: 'Test-Model-RO100',
        category: 'TestCat',
        status: 'ENABLED',
        description: 'Test model description'
      })
    })
    const data = await res.json()
    return { status: res.status, code: data.code, message: data.message }
  })
  console.log('  Create API response:', JSON.stringify(createResponse))
  record('Create device model (API)', createResponse.code === 200, JSON.stringify(createResponse))

  // Reload page to see the new model
  await page.reload()
  await page.waitForTimeout(2000)

  // Check if new model appears
  const hasNewModel = await page.evaluate(() => {
    return document.body.textContent?.includes('DW-TEST-001')
  })
  record('Create device model (UI)', hasNewModel, hasNewModel ? 'DW-TEST-001 found in table' : 'DW-TEST-001 NOT found')

  // 6. Test editing via API
  if (hasNewModel) {
    const updateResponse = await page.evaluate(async () => {
      const token = localStorage.getItem('dw_access_token')
      // First get the model ID
      const listRes = await fetch('/api/v1/device-models', {
        headers: { 'Authorization': `Bearer ${token}` }
      })
      const listData = await listRes.json()
      const model = listData.data?.find(m => m.modelCode === 'DW-TEST-001')
      if (!model) return { error: 'Model not found' }

      const res = await fetch(`/api/v1/device-models/${model.id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          modelCode: 'DW-TEST-001',
          modelName: 'Test-Model-Updated',
          category: 'TestCat',
          status: 'ENABLED',
          description: 'Updated description'
        })
      })
      const data = await res.json()
      return { status: res.status, code: data.code, message: data.message, modelName: data.data?.modelName }
    })
    console.log('  Update API response:', JSON.stringify(updateResponse))
    record('Update device model (API)', updateResponse.code === 200, JSON.stringify(updateResponse))

    // Reload and verify
    await page.reload()
    await page.waitForTimeout(2000)
    const hasUpdated = await page.evaluate(() => {
      return document.body.textContent?.includes('Test-Model-Updated')
    })
    record('Update device model (UI)', hasUpdated, hasUpdated ? 'Name updated' : 'Name NOT updated')

    // 7. Test deleting via API
    const deleteResponse = await page.evaluate(async () => {
      const token = localStorage.getItem('dw_access_token')
      const listRes = await fetch('/api/v1/device-models', {
        headers: { 'Authorization': `Bearer ${token}` }
      })
      const listData = await listRes.json()
      const model = listData.data?.find(m => m.modelCode === 'DW-TEST-001')
      if (!model) return { error: 'Model not found' }

      const res = await fetch(`/api/v1/device-models/${model.id}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${token}` }
      })
      const data = await res.json()
      return { status: res.status, code: data.code, message: data.message }
    })
    console.log('  Delete API response:', JSON.stringify(deleteResponse))
    record('Delete device model (API)', deleteResponse.code === 200, JSON.stringify(deleteResponse))

    // Reload and verify
    await page.reload()
    await page.waitForTimeout(2000)
    const isDeleted = await page.evaluate(() => {
      return !document.body.textContent?.includes('DW-TEST-001')
    })
    record('Delete device model (UI)', isDeleted, isDeleted ? 'DW-TEST-001 removed' : 'DW-TEST-001 still exists')
  }

  // 8. Verify device register page still shows models
  await page.goto(BASE_URL + '/devices/register')
  await page.waitForTimeout(2000)
  await page.click('.el-form-item:has-text("设备型号") .el-select__wrapper, .el-form-item:has-text("设备型号") .el-input__wrapper')
  await page.waitForTimeout(1000)
  const options = await page.evaluate(() => {
    const items = document.querySelectorAll('.el-select-dropdown__item')
    return Array.from(items).map(item => item.textContent?.trim() || '').filter(t => t.length > 0)
  })
  record('Device register page shows models', options.length > 0, `${options.length} options available`)

  // Output
  console.log('\n========== Device Model CRUD Test ==========')
  results.forEach(r => {
    const icon = r.status === 'PASS' ? '[PASS]' : '[FAIL]'
    console.log(`${icon} ${r.test}`)
    console.log(`       ${r.detail}`)
  })
  console.log('============================================')
  console.log(`Total: ${passCount} PASS / ${failCount} FAIL`)
  console.log(`Pass Rate: ${((passCount / (passCount + failCount)) * 100).toFixed(1)}%`)

  await browser.close()
}

main().catch(err => {
  console.error('Test error:', err)
  process.exit(1)
})
