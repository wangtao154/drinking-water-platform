// login-debug.mjs - Debug login error
import { chromium } from 'playwright';

const BASE_URL = 'http://localhost:8088';

async function main() {
    const browser = await chromium.launch({ headless: false });
    const context = await browser.newContext({ viewport: { width: 1400, height: 900 } });
    const page = await context.newPage();

    // Capture all network requests
    const requests = [];
    const responses = [];

    page.on('request', req => {
        if (req.url().includes('localhost')) {
            requests.push({ method: req.method(), url: req.url() });
        }
    });

    page.on('response', async res => {
        if (res.url().includes('localhost') && !res.url().includes('favicon')) {
            let body = '';
            try {
                if (res.status() >= 400) {
                    body = await res.text();
                } else {
                    body = await res.text();
                    if (body.length > 500) body = body.substring(0, 500) + '...';
                }
            } catch (e) {
                body = '(unreadable)';
            }
            responses.push({ status: res.status(), url: res.url(), body });
        }
    });

    page.on('console', msg => {
        if (msg.type() === 'error') {
            console.log('CONSOLE ERROR:', msg.text());
        }
    });

    // Login
    await page.goto(`${BASE_URL}/login`);
    await page.waitForSelector('input[placeholder*="账号"]', { timeout: 10000 });
    await page.fill('input[placeholder*="账号"]', 'admin');
    await page.fill('input[placeholder*="密码"]', 'admin123');
    await page.click('.el-button--primary');

    await page.waitForTimeout(5000);

    // Print all failed requests
    console.log('\n===== FAILED REQUESTS =====');
    for (const r of responses.filter(r => r.status >= 400)) {
        console.log(`\n[${r.status}] ${r.url}`);
        console.log(`Body: ${r.body}`);
    }

    console.log('\n===== ALL API CALLS =====');
    for (const r of responses.filter(r => r.url.includes('/api/'))) {
        console.log(`[${r.status}] ${r.url}`);
    }

    // Take screenshot
    await page.screenshot({ path: 'login_error.png' });
    console.log('\nScreenshot saved: login_error.png');

    await browser.close();
}

main().catch(console.error);
