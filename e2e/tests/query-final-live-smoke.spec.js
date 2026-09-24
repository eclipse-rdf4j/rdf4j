// @ts-check
const { test, expect } = require('@playwright/test');

const SERVER_BASE_URL = process.env.RDF4J_SERVER_BASE_URL || 'http://127.0.0.1:8080/rdf4j-server';
const WORKBENCH_BASE_URL = process.env.RDF4J_WORKBENCH_BASE_URL || 'http://127.0.0.1:8080/rdf4j-workbench';
const REPOSITORY_ID = 'query-final-live';
const QUERY_URL = `${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/query`;
const QUERY_ENDPOINT = `${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/query`;

test.setTimeout(120000);

test.beforeEach(async ({ page, request }) => {
    await request.delete(`${SERVER_BASE_URL}/repositories/${REPOSITORY_ID}`);
    const response = await request.put(`${SERVER_BASE_URL}/repositories/${REPOSITORY_ID}`, {
        headers: { 'Content-Type': 'text/turtle' },
        data: nativeRepositoryConfig(REPOSITORY_ID)
    });
    expect([200, 201, 204]).toContain(response.status());
    await page.goto(QUERY_URL, { waitUntil: 'domcontentloaded' });
    await page.locator('.CodeMirror').waitFor({ state: 'visible' });
});

test.afterEach(async ({ request }) => {
    await request.delete(`${SERVER_BASE_URL}/repositories/${REPOSITORY_ID}`).catch(() => {});
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
        'GET');
    await expect(select.frame.locator('table.data tbody tr')).toHaveCount(2);
    expect(page.context().pages()).toHaveLength(1);
    expect(page.url()).toBe(initialUrl);
    log(`SELECT method=${select.request.method()} rows=${await select.frame.locator('table.data tbody tr').count()} tabs=${page.context().pages().length}`);

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

    await page.locator('#compare-toggle').click();
    await expect(page.locator('.CodeMirror')).toHaveCount(2);
    await page.locator('.CodeMirror').nth(1).evaluate((element, query) => element.CodeMirror.setValue(query),
        'ASK { VALUES ?s { "one" } }');
    await page.locator('#explain-compare-trigger').click();
    await expect.poll(() => page.locator('#query-explanation-compare').textContent())
        .not.toContain('Loading explanation...');
    log(`COMPARE panes=${await page.locator('.CodeMirror').count()} text-length=${(await page.locator('#query-explanation-compare').textContent()).trim().length}`);
    await page.goto(QUERY_URL, { waitUntil: 'domcontentloaded' });
    await page.locator('.CodeMirror').waitFor({ state: 'visible' });
    await expect(page.locator('.CodeMirror')).toHaveCount(1);

	const ask = await execute(page, 'ASK { VALUES ?s { <urn:subject:one> } }', 'GET');
	await expect(ask.frame.locator('.queryResult')).toContainText(/Yes|true/i);
	expect((await ask.frame.locator('.queryResult').innerText()).trim()).toBe('Yes');
	log(`ASK method=${ask.request.method()} result="${(await ask.frame.locator('.queryResult').textContent()).trim()}"`);

	const askFalse = await execute(page, 'ASK { FILTER(false) }', 'GET');
	await expect(askFalse.frame.locator('.queryResult')).toContainText(/No|false/i);
	expect((await askFalse.frame.locator('.queryResult').innerText()).trim()).toBe('No');
	log(`ASK_FALSE method=${askFalse.request.method()} result="${(await askFalse.frame.locator('.queryResult').textContent()).trim()}"`);

	const graph = await execute(page,
		'CONSTRUCT { ?s ?p ?o } WHERE { VALUES (?s ?p ?o) { (<urn:subject:one> <urn:p> "two") (<urn:subject:three> <urn:p> "four") } }',
		'GET');
    await expect(graph.frame.locator('table.data')).toHaveCount(1);
    const graphRows = await graph.frame.locator('table.data tr').count();
    expect(graphRows).toBeGreaterThan(0);
    await graph.frame.locator('#query-result-options-toggle').press('Enter');
    const graphLimitRequest = page.waitForRequest(request => request.url().startsWith(QUERY_ENDPOINT)
        && request.method() === 'POST'
        && new URLSearchParams(request.postData() || '').get('action') === 'exec');
    await graph.frame.locator('#limit_query').selectOption('10');
    const graphLimit = await graphLimitRequest;
    const graphLimitParameters = new URLSearchParams(graphLimit.postData() || '');
    expect(graphLimitParameters.get('embedded')).toBe('true');
    expect(graphLimitParameters.get('limit_query')).toBe('10');
    const graphLimited = await waitForResult(page);
    await expect(graphLimited.locator('table.data')).toHaveCount(1);
    log(`GRAPH method=${graph.request.method()} rows=${graphRows} limit-method=${graphLimit.method()} embedded=${graphLimitParameters.get('embedded')}`);

    const longQuery = `SELECT ?s WHERE { VALUES ?s { ${values(380)} } }`;
    const long = await execute(page, longQuery, 'POST');
    expect((long.request.postData() || '').length).toBeGreaterThan(2048);
    await expect(long.frame.locator('table.data tbody tr')).toHaveCount(100);
    log(`LONG method=${long.request.method()} query-bytes=${longQuery.length} body-bytes=${(long.request.postData() || '').length} rows=100`);

    const paged = await execute(page, `SELECT ?s WHERE { VALUES ?s { ${values(32)} } }`, 'GET');
    await paged.frame.locator('#query-result-options-toggle').press('Enter');
    const pageLimitRequest = page.waitForRequest(request => request.url().startsWith(QUERY_ENDPOINT)
        && request.method() === 'POST'
        && new URLSearchParams(request.postData() || '').get('action') === 'exec');
    await paged.frame.locator('#limit_query').selectOption('10');
    const pageLimit = await pageLimitRequest;
    const pageLimitParameters = new URLSearchParams(pageLimit.postData() || '');
    expect(pageLimitParameters.get('embedded')).toBe('true');
    expect(pageLimitParameters.get('limit_query')).toBe('10');
    const limited = await waitForResult(page);
    await expect(limited.locator('#nextX')).toBeEnabled();
    const nextRequest = page.waitForRequest(request => request.url().startsWith(QUERY_ENDPOINT)
        && request.method() === 'POST'
        && new URLSearchParams(request.postData() || '').get('offset'));
    await limited.locator('#nextX').click();
    const next = await nextRequest;
    const nextParameters = new URLSearchParams(next.postData() || '');
    expect(nextParameters.get('embedded')).toBe('true');
    expect(nextParameters.get('query-request-id')).toBeTruthy();
    const nextFrame = await waitForResult(page);
    await expect(nextFrame.locator('table.data tbody tr')).toHaveCount(10);
    log(`PAGING limit=${pageLimitParameters.get('limit_query')} next-offset=${nextParameters.get('offset')} fresh-id=${Boolean(nextParameters.get('query-request-id'))}`);

    const downloadResult = await execute(page, 'SELECT * WHERE { VALUES ?s { "download-me" } }', 'GET');
    await downloadResult.frame.locator('#query-result-download-toggle').press('Enter');
    const downloadPromise = page.waitForEvent('download');
    await downloadResult.frame.locator('input[type="submit"][value="Download"]').click();
    const download = await downloadPromise;
    expect(download.suggestedFilename()).toBeTruthy();
    await expect(page.locator('#query-results-loading')).toBeHidden();
    log(`DOWNLOAD filename=${download.suggestedFilename()} parent-url-unchanged=${page.url() === initialUrl}`);

    const invalid = await execute(page, 'SELECT WHERE {', 'GET');
    await expect(invalid.frame.locator('.query-result-error')).toHaveCount(1);
    log(`INVALID error=${await invalid.frame.locator('.query-result-error').count()} editor-visible=${await page.locator('.CodeMirror').count() === 1}`);

    let cancelStatus;
    let delayedExecution = false;
    const routePattern = `**/rdf4j-workbench/repositories/${REPOSITORY_ID}/query**`;
    await page.route(routePattern, async route => {
        const request = route.request();
        const parameters = new URLSearchParams(request.method() === 'POST'
            ? request.postData() || ''
            : new URL(request.url()).search);
        const action = parameters.get('action');
        if (action === 'exec' && !delayedExecution) {
            delayedExecution = true;
            const response = await route.fetch();
            await new Promise(resolve => setTimeout(resolve, 3000));
            await route.fulfill({ response });
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
    await expect(page.locator('#query-results-status')).toHaveText('Query cancelled.');
    await expect.poll(() => cancelStatus).toBeGreaterThanOrEqual(200);
    expect([200, 204, 404]).toContain(cancelStatus);
    await page.unroute(routePattern);
    log(`CANCEL request-id=${Boolean(cancelledRequestId)} endpoint-status=${cancelStatus} status="${(await page.locator('#query-results-status').textContent()).trim()}" backend-handle=${cancelStatus < 300 ? 'accepted' : 'already-completed-local-fixture'}`);

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
    const frame = await waitForResult(page);
    return { frame: page.frameLocator('#query-results-frame'), request };
}

async function waitForResult(page) {
    const frame = page.frameLocator('#query-results-frame');
    await expect(frame.locator('#rdf4j-query-result')).toHaveCount(1, { timeout: 30000 });
    await expect(page.locator('#query-request-id')).toHaveValue('', { timeout: 30000 });
    return frame;
}

async function setQuery(page, query) {
    await page.locator('.CodeMirror').first().evaluate((element, value) => {
        element.CodeMirror.setValue(value);
    }, query);
}

function values(count) {
    return Array.from({ length: count }, (_, index) => `"row-${index}"`).join(' ');
}

function nativeRepositoryConfig(repositoryId) {
    return `@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix config: <tag:rdf4j.org,2023:config/>.

[] a config:Repository ;
   config:rep.id "${repositoryId}" ;
   rdfs:label "Query final live smoke test" ;
   config:rep.impl [
      config:rep.type "openrdf:SailRepository" ;
      config:sail.impl [
         config:sail.type "openrdf:MemoryStore"
      ]
   ].
`;
}
