// @ts-check
const { test, expect } = require('@playwright/test');

const QUERY_PAGE = 'http://127.0.0.1:8080/rdf4j-workbench/repositories/embeddedrepo/query';

test.describe('embedded query result load failures', () => {
    test.beforeEach(async ({ page }) => {
        await page.goto(QUERY_PAGE, { waitUntil: 'domcontentloaded' });
        await page.locator('.CodeMirror').waitFor({ state: 'visible' });
    });

    test('clears GET execution state when the result response is an HTTP failure', async ({ page }) => {
        await page.route('**/rdf4j-workbench/repositories/embeddedrepo/query**', async route => {
            if (route.request().method() === 'GET' && route.request().url().includes('action=exec')) {
                await route.fulfill({ status: 500, contentType: 'text/plain', body: 'query backend failed' });
                return;
            }
            await route.continue();
        });

        await page.evaluate(() => {
            document.querySelector('.CodeMirror').CodeMirror.setValue('SELECT * WHERE { VALUES ?s { "one" } }');
        });
        const response = page.waitForResponse(result => result.status() === 500);
        await page.locator('#exec').click();
        await expect(await response).toBeTruthy();
        await page.waitForTimeout(250);

        await expect(page.locator('#query-request-id')).toHaveValue('');
        await expect(page.locator('#query-cancel')).toBeDisabled();
        await expect(page.locator('#query-results-loading')).toBeHidden();
        await expect(page.locator('#query-results-status')).toHaveText('Unable to load query results.');
    });

    test('clears POST execution state when the result response is malformed', async ({ page }) => {
        await page.route('**/rdf4j-workbench/repositories/embeddedrepo/query**', async route => {
            if (route.request().method() === 'POST' && (route.request().postData() || '').includes('action=exec')) {
                await route.fulfill({ status: 200, contentType: 'application/xml', body: '<malformed' });
                return;
            }
            await route.continue();
        });

        await page.evaluate(() => {
            document.querySelector('.CodeMirror').CodeMirror.setValue('SELECT * WHERE { VALUES ?s { ' + '"x" '.repeat(900) + '} }');
        });
        const response = page.waitForResponse(result => result.status() === 200
            && result.request().method() === 'POST'
            && (result.request().postData() || '').includes('action=exec'));
        await page.locator('#exec').click();
        await expect(await response).toBeTruthy();
        await page.waitForTimeout(250);

        await expect(page.locator('#query-request-id')).toHaveValue('');
        await expect(page.locator('#query-cancel')).toBeDisabled();
        await expect(page.locator('#query-results-loading')).toBeHidden();
        await expect(page.locator('#query-results-status')).toHaveText('Unable to load query results.');
    });
});
