const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await context.newPage();

  // Login
  await page.goto('http://localhost:8088/login');
  await page.getByPlaceholder('请输入账号').fill('admin');
  await page.getByPlaceholder('请输入密码').fill('admin123');
  await page.getByRole('button', { name: '登 录' }).click();

  // Wait for navigation to dashboard
  await page.waitForURL('**/dashboard', { timeout: 10000 });

  // Navigate to work order list via menu
  await page.click('text=工单管理');
  await page.waitForTimeout(500);
  await page.click('text=工单列表');
  await page.waitForTimeout(2000);
  await page.waitForTimeout(3000);

  // Take full page screenshot regardless
  await page.screenshot({ path: 'workorder_list_full.png', fullPage: true });

  // Take full page screenshot
  await page.screenshot({ path: 'workorder_list_full.png', fullPage: true });

  // Check if pagination exists
  const paginationCount = await page.locator('.el-pagination').count();
  console.log('Pagination count:', paginationCount);

  const paginationVisible = await page.locator('.el-pagination').isVisible().catch(() => false);
  console.log('Pagination visible:', paginationVisible);

  const paginationHTML = await page.locator('.el-pagination').innerHTML().catch(() => null);
  console.log('Pagination HTML length:', paginationHTML ? paginationHTML.length : 0);

  await browser.close();
})();
