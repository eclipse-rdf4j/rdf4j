// @ts-check
const { test, expect } = require('@playwright/test');
const {
    createSeededRepository,
    deleteRepository,
    repositoryPageUrl,
    serverBaseUrl,
    uniqueRepositoryId
} = require('./workbench-test-helpers.js');

// Migrated with the redesign (see .agent/execplans/workbench-stale-spec-migration-20261002.md): a query runs as one
// streamed POST into the Query page (no GET for short queries and no result iframe), and a failed result shows the
// designed failure state, the status "Query failed." and an error callout titled "The query failed" (app-shell plan,
// decision M3.5), instead of the iframe's "Unable to load query results." line.

const REPOSITORY_ID = uniqueRepositoryId('query-result-lifecycle');
const QUERY_PAGE = repositoryPageUrl(REPOSITORY_ID, 'query');

test.describe('embedded query result load failures', () => {
    test.beforeAll(async ({ request }) => {
        await createSeededRepository(request, serverBaseUrl(), REPOSITORY_ID,
            { graphs: [], label: 'Query result lifecycle test' });
    });

    test.afterAll(async ({ request }) => {
        await deleteRepository(request, serverBaseUrl(), REPOSITORY_ID);
    });

    test.beforeEach(async ({ page }) => {
        await page.goto(QUERY_PAGE, { waitUntil: 'domcontentloaded' });
        await page.locator('.CodeMirror').waitFor({ state: 'visible' });
    });

    async function expectFailedResult(page) {
        const result = page.locator('#query-results [data-query-stream-root]');
        await expect(result.locator('.query-result-status')).toHaveText('Query failed.');
        await expect(result.locator('.query-result-error')).toBeVisible();
        await expect(result.locator('.query-result-error .workbench-callout__title')).toHaveText('The query failed');
        await expect(page.locator('#query-results')).toHaveAttribute('aria-busy', 'false');
        await expect(page.locator('#query-request-id')).toHaveValue('');
        await expect(page.locator('#query-cancel')).toBeDisabled();
        await expect(page.locator('#query-results-loading')).toBeHidden();
    }

    test('clears execution state when the result response is an HTTP failure', async ({ page }) => {
        await page.route(`**/rdf4j-workbench/repositories/${REPOSITORY_ID}/query**`, async route => {
            if (route.request().method() === 'POST' && (route.request().postData() || '').includes('action=exec')) {
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

        await expectFailedResult(page);
    });

    test('clears POST execution state when the result response is malformed', async ({ page }) => {
        await page.route(`**/rdf4j-workbench/repositories/${REPOSITORY_ID}/query**`, async route => {
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

        await expectFailedResult(page);
    });
});
