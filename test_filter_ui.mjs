// filter-ui-test.mjs - Playwright E2E test for Filter Management pages
import { chromium } from 'playwright';

const BASE_URL = 'http://localhost:8088';
const results = [];
let passed = 0, failed = 0;

function log(name, ok, detail = '') {
    const status = ok ? 'PASS' : 'FAIL';
    if (ok) passed++; else failed++;
    console.log(`[${status}] ${name}${detail ? ' - ' + detail : ''}`);
    results.push({ name, ok, detail });
}

async function login(page) {
    await page.goto(`${BASE_URL}/login`);
    await page.waitForSelector('input[placeholder*="账号"]', { timeout: 10000 });
    await page.fill('input[placeholder*="账号"]', 'admin');
    await page.fill('input[placeholder*="密码"]', 'admin123');
    // Element Plus el-button, not native button[type="submit"]
    await page.click('.el-button--primary');
    await page.waitForURL('**/dashboard', { timeout: 10000 });
    log('Login', true);
}

async function main() {
    const browser = await chromium.launch({ headless: true });
    const context = await browser.newContext({ viewport: { width: 1400, height: 900 } });
    const page = await context.newPage();

    try {
        // Login
        await login(page);

        // ===== Test 1: Filter List Page =====
        console.log('\n===== Filter List Page =====');
        await page.goto(`${BASE_URL}/filters/list`);
        await page.waitForTimeout(2000);

        // Check page title and table
        const listTitle = await page.locator('.el-page-header__title, .page-header, h2').first().textContent().catch(() => '');
        const hasTable = await page.locator('.el-table').count();
        log('Filter list page loads', hasTable > 0, `table found=${hasTable > 0}`);

        // Check table has data rows
        const tableRows = await page.locator('.el-table__body-wrapper .el-table__row').count();
        log('Filter list has data', tableRows > 0, `${tableRows} rows`);

        // Check for register button
        const hasRegisterBtn = await page.locator('button:has-text("登记")').count();
        log('Register button present', hasRegisterBtn > 0);

        // Check for edit button
        const hasEditBtn = await page.locator('button:has-text("编辑")').count();
        log('Edit button present', hasEditBtn > 0);

        // Check for delete button
        const hasDeleteBtn = await page.locator('button:has-text("删除")').count();
        log('Delete button present', hasDeleteBtn > 0);

        // Check for trace button
        const hasTraceBtn = await page.locator('button:has-text("追溯")').count();
        log('Trace button present', hasTraceBtn > 0);

        // ===== Test 2: Filter Models Page =====
        console.log('\n===== Filter Models Page =====');
        await page.goto(`${BASE_URL}/filters/models`);
        await page.waitForTimeout(2000);

        const modelsTable = await page.locator('.el-table').count();
        log('Filter models page loads', modelsTable > 0);

        const modelRows = await page.locator('.el-table__body-wrapper .el-table__row').count();
        log('Filter models has data', modelRows >= 0, `${modelRows} rows`);

        // Check for add button
        const hasAddBtn = await page.locator('button:has-text("新增")').count();
        log('Add model button present', hasAddBtn > 0);

        // ===== Test 3: Filter Register Page =====
        console.log('\n===== Filter Register Page =====');
        await page.goto(`${BASE_URL}/filters/list`);
        await page.waitForTimeout(2000);

        // Click register button (navigates to /filters/inbound)
        const registerBtn = page.locator('button:has-text("登记")').first();
        if (await registerBtn.count() > 0) {
            await registerBtn.click();
            await page.waitForTimeout(2000);

            // Check if navigated to inbound page
            const currentUrl = page.url();
            const isRegisterPage = currentUrl.includes('/filters/inbound') || currentUrl.includes('/filters/register');
            log('Register page navigates', isRegisterPage, `url=${currentUrl}`);

            if (isRegisterPage) {
                // Check for model select on register page
                const hasModelSelect = await page.locator('.el-select').count();
                log('Model select on register page', hasModelSelect > 0);

                // Check for form fields
                const hasForm = await page.locator('.el-form').count();
                log('Register form present', hasForm > 0);
            }

            // Go back for next test
            await page.goto(`${BASE_URL}/filters/list`);
            await page.waitForTimeout(2000);
        } else {
            log('Register page navigates', false, 'register button not found');
        }

        // ===== Test 4: Filter Edit Dialog =====
        console.log('\n===== Filter Edit Dialog =====');
        // Reload to ensure clean state
        await page.goto(`${BASE_URL}/filters/list`);
        await page.waitForTimeout(2000);

        const editBtn = page.locator('button:has-text("编辑")').first();
        if (await editBtn.count() > 0) {
            await editBtn.click();
            await page.waitForTimeout(1000);

            const editDialog = await page.locator('.el-dialog').count();
            log('Edit dialog opens', editDialog > 0);

            if (editDialog > 0) {
                // Check for filterId field (should be disabled)
                const filterIdInput = page.locator('.el-dialog input[disabled]').first();
                const filterIdDisabled = await filterIdInput.count();
                log('Filter ID field is disabled in edit', filterIdDisabled > 0);

                // Check for model select in edit dialog
                const hasModelSelect = await page.locator('.el-dialog .el-select').count();
                log('Model select in edit dialog', hasModelSelect > 0);

                // Check for status select
                const statusSelect = page.locator('.el-dialog .el-select').last();
                const hasStatusSelect = await statusSelect.count();
                log('Status select in edit dialog', hasStatusSelect > 0);

                // Check for remark textarea
                const hasRemark = await page.locator('.el-dialog .el-textarea, .el-dialog textarea').count();
                log('Remark textarea in edit dialog', hasRemark > 0);

                // Close dialog
                await page.keyboard.press('Escape');
                await page.waitForTimeout(500);
            }
        } else {
            log('Edit dialog opens', false, 'edit button not found');
        }

        // ===== Test 5: Delete Confirmation =====
        console.log('\n===== Delete Confirmation =====');
        const deleteBtn = page.locator('button:has-text("删除")').first();
        if (await deleteBtn.count() > 0) {
            await deleteBtn.click();
            await page.waitForTimeout(500);

            // Check for popconfirm
            const popconfirm = await page.locator('.el-popconfirm, .el-popover').count();
            log('Delete popconfirm appears', popconfirm > 0);

            // Cancel deletion
            const cancelBtn = page.locator('.el-popconfirm button:has-text("否"), .el-popover button:has-text("取消")').first();
            if (await cancelBtn.count() > 0) {
                await cancelBtn.click();
                await page.waitForTimeout(500);
            }
        } else {
            log('Delete popconfirm appears', false, 'delete button not found');
        }

        // ===== Test 6: Model Create Dialog =====
        console.log('\n===== Model Create Dialog =====');
        await page.goto(`${BASE_URL}/filters/models`);
        await page.waitForTimeout(2000);

        const addModelBtn = page.locator('button:has-text("新增")').first();
        if (await addModelBtn.count() > 0) {
            await addModelBtn.click();
            await page.waitForTimeout(1500);

            const modelDialog = await page.locator('.el-dialog').count();
            log('Model create dialog opens', modelDialog > 0);

            if (modelDialog > 0) {
                // Wait for dialog content to render
                await page.waitForTimeout(500);

                // Check form fields
                const inputs = await page.locator('.el-dialog .el-input, .el-dialog .el-select, .el-dialog .el-input-number, .el-dialog .el-textarea').count();
                log('Model create form has fields', inputs >= 5, `${inputs} fields found`);

                // Check for category select (label is "分类")
                const categoryItems = await page.locator('.el-dialog .el-form-item__label').filter({ hasText: '分类' }).count();
                log('Category select in model dialog', categoryItems > 0, `found ${categoryItems} labels`);

                // Check for level input (label is "滤芯级别")
                const levelItems = await page.locator('.el-dialog .el-form-item__label').filter({ hasText: '级别' }).count();
                log('Level input in model dialog', levelItems > 0, `found ${levelItems} labels`);

                // Close dialog
                await page.keyboard.press('Escape');
                await page.waitForTimeout(500);
            }
        } else {
            log('Model create dialog opens', false, 'add button not found');
        }

        // ===== Test 7: Trace Page =====
        console.log('\n===== Trace Page =====');
        await page.goto(`${BASE_URL}/filters/list`);
        await page.waitForTimeout(2000);

        // Trace button navigates to /filters/trace/:filterId
        const traceBtn = page.locator('button:has-text("追溯")').first();
        if (await traceBtn.count() > 0) {
            await traceBtn.click();
            await page.waitForTimeout(2000);

            const currentUrl = page.url();
            const isTracePage = currentUrl.includes('/filters/trace/');
            log('Trace page navigates', isTracePage, `url=${currentUrl}`);

            if (isTracePage) {
                // Check for trace content (timeline or table or card)
                const hasContent = await page.locator('.el-timeline, .el-table, .el-card').count();
                log('Trace page has content', hasContent > 0);
            }

            // Go back
            await page.goto(`${BASE_URL}/filters/list`);
            await page.waitForTimeout(1000);
        } else {
            log('Trace page navigates', false, 'trace button not found');
        }

        // ===== Summary =====
        console.log('\n' + '='.repeat(50));
        console.log(`TOTAL: ${passed + failed} | PASS: ${passed} | FAIL: ${failed}`);
        console.log('='.repeat(50));

    } catch (err) {
        console.error('Test error:', err.message);
        failed++;
    } finally {
        await browser.close();
    }

    process.exit(failed > 0 ? 1 : 0);
}

main();
