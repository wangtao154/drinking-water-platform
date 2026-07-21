// filter-qr-test.mjs - Playwright test for QR code display
import { chromium } from 'playwright';

const BASE_URL = 'http://localhost:8088';
let passed = 0, failed = 0;

function log(name, ok, detail = '') {
    const status = ok ? 'PASS' : 'FAIL';
    if (ok) passed++; else failed++;
    console.log(`[${status}] ${name}${detail ? ' - ' + detail : ''}`);
}

async function main() {
    const browser = await chromium.launch({ headless: true });
    const context = await browser.newContext({ viewport: { width: 1400, height: 900 } });
    const page = await context.newPage();

    try {
        // Login
        await page.goto(`${BASE_URL}/login`);
        await page.waitForSelector('input[placeholder*="账号"]', { timeout: 10000 });
        await page.fill('input[placeholder*="账号"]', 'admin');
        await page.fill('input[placeholder*="密码"]', 'admin123');
        await page.click('.el-button--primary');
        await page.waitForURL('**/dashboard', { timeout: 10000 });
        log('Login', true);

        // ===== Test: Register filter and verify QR code =====
        console.log('\n===== QR Code Display Test =====');

        // Go to register page
        await page.goto(`${BASE_URL}/filters/inbound`);
        await page.waitForTimeout(2000);

        // Select first model from dropdown
        const modelSelect = page.locator('.el-select').first();
        await modelSelect.click();
        await page.waitForTimeout(500);

        const firstOption = page.locator('.el-select-dropdown__item').first();
        if (await firstOption.count() > 0) {
            await firstOption.click();
            await page.waitForTimeout(500);
            log('Model selected', true);

            // Submit registration
            const submitBtn = page.locator('button:has-text("提交登记")');
            await submitBtn.click();
            await page.waitForTimeout(4000);

            // Check for success result card
            const resultCard = await page.locator('.result-card').count();
            log('Result card appears after registration', resultCard > 0);

            if (resultCard > 0) {
                // Check for filter ID display
                const filterIdTag = await page.locator('.result-card .el-tag').first().textContent();
                log('Filter ID displayed', filterIdTag && filterIdTag.startsWith('FL'), `id=${filterIdTag}`);

                // Check for QR code image
                const qrImg = await page.locator('.result-card .qr-img').count();
                log('QR code image displayed', qrImg > 0);

                if (qrImg > 0) {
                    const qrSrc = await page.locator('.result-card .qr-img').getAttribute('src');
                    const isBase64 = qrSrc && qrSrc.startsWith('data:image');
                    log('QR code is valid Base64 image', isBase64);
                }

                // Check for download button
                const downloadBtn = await page.locator('.result-card button:has-text("下载")').count();
                log('Download QR button present', downloadBtn > 0);

                // Check for continue button
                const continueBtn = await page.locator('.result-card button:has-text("继续登记")').count();
                log('Continue button present', continueBtn > 0);
            }
        } else {
            log('Model selected', false, 'no models in dropdown');
        }

        // ===== Test: Trace page QR code =====
        console.log('\n===== Trace Page QR Code Test =====');

        // Go to filter list to find an existing filter
        await page.goto(`${BASE_URL}/filters/list`);
        await page.waitForTimeout(2000);

        const traceBtn = page.locator('button:has-text("追溯")').first();
        if (await traceBtn.count() > 0) {
            await traceBtn.click();
            await page.waitForTimeout(2000);

            const currentUrl = page.url();
            log('Trace page loads', currentUrl.includes('/filters/trace/'));

            // Check for QR code on trace page
            const traceQr = await page.locator('.qr-img').count();
            log('QR code on trace page', traceQr > 0);

            if (traceQr > 0) {
                const qrSrc = await page.locator('.qr-img').getAttribute('src');
                const isBase64 = qrSrc && qrSrc.startsWith('data:image');
                log('Trace page QR is valid Base64', isBase64);
            }

            // Check for info section with filter ID
            const filterIdText = await page.locator('.el-descriptions').first().textContent();
            log('Trace page shows filter info', filterIdText && filterIdText.includes('FL'));
        } else {
            log('Trace page loads', false, 'no trace button');
        }

        // Summary
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
