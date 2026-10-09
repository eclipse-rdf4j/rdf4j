// @ts-check
const { test, expect } = require('@playwright/test');
const {
    createSeededRepository,
    deleteRepository,
    repositoryPageUrl,
    serverBaseUrl,
    uniqueRepositoryId
} = require('./workbench-test-helpers.js');

// Migrated with the redesign (see .agent/execplans/workbench-stale-spec-migration-20261002.md): the result is read on
// the Query page instead of in the retired result iframe, and every execution is one streamed POST (short queries are no
// longer sent with GET). Two steps were retired because their controls were replaced by Load more: changing the
// result's "results per page" and paging with Next; workbench-query-load-more.spec.js covers both ("Execute requests
// one million rows by default and renders no result page controls" and "main query Load more appends batches, freezes
// the query, and keeps the virtual window bounded"). The long query therefore loads all of its 380 rows.

const REPOSITORY_ID = uniqueRepositoryId('query-final-live');
const QUERY_URL = repositoryPageUrl(REPOSITORY_ID, 'query');
const QUERY_ENDPOINT = QUERY_URL;

test.setTimeout(120000);

test.beforeAll(async ({ request }) => {
    await createSeededRepository(request, serverBaseUrl(), REPOSITORY_ID,
        { graphs: [], label: 'Query final live smoke test' });
});

test.afterAll(async ({ request }) => {
    await deleteRepository(request, serverBaseUrl(), REPOSITORY_ID);
});

test.beforeEach(async ({ page }) => {
    await page.goto(QUERY_URL, { waitUntil: 'domcontentloaded' });
    await page.locator('.CodeMirror').waitFor({ state: 'visible' });
});

test('covers the current embedded query workflow against a live repository', async ({ page }) => {
    const lines = [];
    const log = value => {
        lines.push(value);
        console.log(value);
    };
    const initialUrl = page.url();

    const select = await execute(page,
        'SELECT ?s ?p ?o WHERE { VALUES (?s ?p ?o) { ("one" <urn:p> "two") ("three" <urn:p> "four") } }',
        'POST');
    await expect(select.result.locator('table.data tbody tr')).toHaveCount(2);
    expect(page.context().pages()).toHaveLength(1);
    expect(page.url()).toBe(initialUrl);
    log(`SELECT method=${select.request.method()} rows=${await select.result.locator('table.data tbody tr').count()} tabs=${page.context().pages().length}`);

    await page.locator('#explain-trigger').click();
    await expect.poll(() => page.locator('#query-explanation').textContent())
        .not.toContain('Loading explanation...');
    log(`EXPLAIN text-length=${(await page.locator('#query-explanation').textContent()).trim().length}`);

    await page.locator('#save-query-toggle').press('Enter');
    await page.locator('#query-name').fill(`live-${Date.now()}`.slice(0, 32));
    const privateControlEnabled = await page.locator('#save-private').isEnabled();
    if (privateControlEnabled) {
        await page.locator('#save-private').check();
    }
    await page.evaluate(() => workbench.query.handleNameChange());
    await expect(page.locator('#save')).toBeEnabled();
    await page.locator('#save').click();
    await expect(page.locator('#save-feedback')).toHaveText('Query saved.');
    log(`SAVE feedback="${(await page.locator('#save-feedback').textContent()).trim()}" private-enabled=${privateControlEnabled} private=${await page.locator('#save-private').isChecked()}`);
    // The Save query pane lies over the page (app-shell plan M14.3); close it before using the controls below it.
    await page.locator('#save-query-toggle').click();
    await expect(page.locator('#save-query-panel')).toBeHidden();

    await page.locator('#compare-toggle').click();
    await expect(page.locator('.CodeMirror')).toHaveCount(2);
    await page.locator('.CodeMirror').nth(1).evaluate((element, query) => element.CodeMirror.setValue(query),
        'ASK { VALUES ?s { "one" } }');
    await page.locator('#explain-trigger').click();
    await expect.poll(() => page.locator('#query-explanation-compare').textContent())
        .not.toContain('Loading explanation...');
    log(`COMPARE panes=${await page.locator('.CodeMirror').count()} text-length=${(await page.locator('#query-explanation-compare').textContent()).trim().length}`);
    await page.goto(QUERY_URL, { waitUntil: 'domcontentloaded' });
    await page.locator('.CodeMirror').waitFor({ state: 'visible' });
    await expect(page.locator('.CodeMirror')).toHaveCount(1);

    // A Yes/No answer is a designed state with the answer and an explanation (app-shell plan M3.5).
    const ask = await execute(page, 'ASK { VALUES ?s { <urn:subject:one> } }', 'POST');
    await expect(ask.result.locator('.queryResult')).toContainText(/Yes|true/i);
    expect((await ask.result.locator('.query-result-boolean__value').innerText()).trim()).toBe('Yes');
    log(`ASK method=${ask.request.method()} result="${(await ask.result.locator('.query-result-boolean__value').textContent()).trim()}"`);

    const askFalse = await execute(page, 'ASK { FILTER(false) }', 'POST');
    await expect(askFalse.result.locator('.queryResult')).toContainText(/No|false/i);
    expect((await askFalse.result.locator('.query-result-boolean__value').innerText()).trim()).toBe('No');
    log(`ASK_FALSE method=${askFalse.request.method()} result="${(await askFalse.result.locator('.query-result-boolean__value').textContent()).trim()}"`);

    const graph = await execute(page,
        'CONSTRUCT { ?s ?p ?o } WHERE { VALUES (?s ?p ?o) { (<urn:subject:one> <urn:p> "two") (<urn:subject:three> <urn:p> "four") } }',
        'POST');
    const graphTable = graph.result.locator('[id^="query-result-table-wrap-"] table.data');
    await expect(graphTable).toHaveCount(1);
    const graphRows = await graphTable.locator('tr').count();
    expect(graphRows).toBeGreaterThan(0);
    log(`GRAPH method=${graph.request.method()} rows=${graphRows}`);

    const longQuery = `SELECT ?s WHERE { VALUES ?s { ${values(380)} } }`;
    const long = await execute(page, longQuery, 'POST');
    expect((long.request.postData() || '').length).toBeGreaterThan(2048);
    await expect(long.result.locator('.query-result-status')).toHaveText(/^380 rows · complete/);
    await expect(long.result.locator('table.data tbody tr[data-query-row-index]').first()).toBeVisible();
    log(`LONG method=${long.request.method()} query-bytes=${longQuery.length} body-bytes=${(long.request.postData() || '').length} rows=380`);

    const downloadResult = await execute(page, 'SELECT * WHERE { VALUES ?s { "download-me" } }', 'POST');
    await downloadResult.result.locator('.query-result-download-toggle').press('Enter');
    const downloadPromise = page.waitForEvent('download');
    await downloadResult.result.locator('.query-result-download-button').click();
    const download = await downloadPromise;
    expect(download.suggestedFilename()).toBeTruthy();
    await expect(page.locator('#query-results-loading')).toBeHidden();
    log(`DOWNLOAD filename=${download.suggestedFilename()} parent-url-unchanged=${page.url() === initialUrl}`);

    const invalid = await execute(page, 'SELECT WHERE {', 'POST');
    await expect(invalid.result.locator('.query-result-error')).toHaveCount(1);
    await expect(invalid.result.locator('.query-result-error')).toBeVisible();
    log(`INVALID error=${await invalid.result.locator('.query-result-error:visible').count()} editor-visible=${await page.locator('.CodeMirror').count() === 1}`);

    let cancelStatus;
    let delayedExecution = false;
    let delayedAnswer = Promise.resolve();
    const routePattern = `**/rdf4j-workbench/repositories/${REPOSITORY_ID}/query**`;
    await page.route(routePattern, async route => {
        const request = route.request();
        const parameters = new URLSearchParams(request.method() === 'POST'
            ? request.postData() || ''
            : new URL(request.url()).search);
        const action = parameters.get('action');
        if (action === 'exec' && !delayedExecution) {
            delayedExecution = true;
            delayedAnswer = (async () => {
                const response = await route.fetch();
                await new Promise(resolve => setTimeout(resolve, 3000));
                // The page aborts this request when the query is cancelled, so the late answer may have nowhere to go.
                await route.fulfill({ response }).catch(() => {});
            })();
            await delayedAnswer;
            return;
        }
        if (action === 'cancel-query') {
            const response = await route.fetch();
            cancelStatus = response.status();
            await route.fulfill({ response });
            return;
        }
        await route.continue();
    });
    await setQuery(page, 'SELECT * WHERE { VALUES ?s { "cancel-me" } }');
    await page.locator('#exec').click();
    await expect(page.locator('#query-cancel')).toBeEnabled();
    const cancelledRequestId = await page.locator('#query-request-id').inputValue();
    await page.locator('#query-cancel').click();
    await expect(page.locator('#query-request-id')).toHaveValue('');
    const cancelledStatus = page.locator('#query-results [data-query-stream-root] .query-result-status');
    await expect(cancelledStatus).toHaveText('Query cancelled.');
    await expect.poll(() => cancelStatus).toBeGreaterThanOrEqual(200);
    expect([200, 204, 404]).toContain(cancelStatus);
    // Let the held answer of the cancelled request run out before the route goes away.
    await delayedAnswer;
    await page.unroute(routePattern);
    log(`CANCEL request-id=${Boolean(cancelledRequestId)} endpoint-status=${cancelStatus} status="${(await cancelledStatus.textContent()).trim()}" backend-handle=${cancelStatus < 300 ? 'accepted' : 'already-completed-local-fixture'}`);

    console.log(`LIVE_SMOKE initial-url=${initialUrl} final-url=${page.url()} tabs=${page.context().pages().length}`);
});

async function execute(page, query, expectedMethod) {
    await setQuery(page, query);
    const requestPromise = page.waitForRequest(request => {
        if (!request.url().startsWith(QUERY_ENDPOINT)) {
            return false;
        }
        if (request.method() === 'GET') {
            return new URL(request.url()).searchParams.get('action') === 'exec';
        }
        return request.method() === 'POST'
            && new URLSearchParams(request.postData() || '').get('action') === 'exec';
    });
    await page.locator('#exec').click();
    const request = await requestPromise;
    expect(request.method()).toBe(expectedMethod);
    const result = await waitForResult(page);
    return { result, request };
}

/** Waits until the streamed result on the Query page has finished and returns it. */
async function waitForResult(page) {
    const result = page.locator('#query-results [data-query-stream-root]');
    await expect(result).toHaveCount(1, { timeout: 30000 });
    await expect(page.locator('#query-results')).toHaveAttribute('aria-busy', 'false', { timeout: 30000 });
    await expect(page.locator('#query-request-id')).toHaveValue('', { timeout: 30000 });
    return result;
}

async function setQuery(page, query) {
    await page.locator('.CodeMirror').first().evaluate((element, value) => {
        element.CodeMirror.setValue(value);
    }, query);
}

function values(count) {
    return Array.from({ length: count }, (_, index) => `"row-${index}"`).join(' ');
}
