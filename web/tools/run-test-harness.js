#!/usr/bin/env node
'use strict';

const path = require('path');
const puppeteer = require('puppeteer-core');

const CHROME_PATH = process.env.CHROME_PATH || '/snap/bin/chromium';
const HARNESS_URL = 'file://' + path.join(__dirname, '..', 'test', 'index.html');
const TIMEOUT_MS = 60_000;

async function main() {
    const browser = await puppeteer.launch({
        executablePath: CHROME_PATH,
        headless: true,
        args: ['--no-sandbox'],
    });

    try {
        const page = await browser.newPage();

        const failures = [];
        await page.evaluateOnNewDocument(() => {
            window.__qunitResult = null;
            window.__qunitFailures = [];
            const timer = setInterval(() => {
                if (!window.QUnit) return;
                clearInterval(timer);
                window.QUnit.log((details) => {
                    if (!details.result) {
                        window.__qunitFailures.push({
                            module: details.module,
                            name: details.name,
                            message: details.message,
                            expected: details.expected,
                            actual: details.actual,
                        });
                    }
                });
                window.QUnit.done((details) => {
                    window.__qunitResult = details;
                });
            }, 20);
        });

        page.on('pageerror', (err) => failures.push('pageerror: ' + err.message));
        page.on('console', (msg) => {
            if (msg.type() === 'error') failures.push('console.error: ' + msg.text());
        });

        await page.goto(HARNESS_URL, { waitUntil: 'load' });
        await page.waitForFunction(() => window.__qunitResult !== null, { timeout: TIMEOUT_MS });

        const result = await page.evaluate(() => window.__qunitResult);
        const qunitFailures = await page.evaluate(() => window.__qunitFailures);

        console.log(`QUnit: ${result.passed}/${result.total} passed, ${result.failed} failed (${result.runtime}ms)`);

        if (result.failed > 0) {
            console.error('\nFalhas:');
            for (const f of qunitFailures) {
                console.error(`  [${f.module}] ${f.name}: ${f.message}`);
                if (f.expected !== undefined) {
                    console.error(`    esperado: ${JSON.stringify(f.expected)} | obtido: ${JSON.stringify(f.actual)}`);
                }
            }
            process.exitCode = 1;
            return;
        }

        if (failures.length > 0) {
            console.error('\nErros de página/console durante execução:');
            failures.forEach((f) => console.error('  ' + f));
            process.exitCode = 1;
            return;
        }

        process.exitCode = 0;
    } finally {
        await browser.close();
    }
}

main().catch((err) => {
    console.error('Falha ao rodar test harness:', err);
    process.exitCode = 1;
});
