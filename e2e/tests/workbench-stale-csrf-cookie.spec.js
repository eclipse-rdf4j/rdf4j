// @ts-check
const { test, expect } = require('@playwright/test');
const {
    createSeededRepository,
    deleteRepository,
    repositoryPageUrl,
    serverBaseUrl,
    workbenchBaseUrl,
    uniqueRepositoryId
} = require('./workbench-test-helpers.js');

const WORKBENCH_BASE_URL = workbenchBaseUrl();
const WORKBENCH = new URL(WORKBENCH_BASE_URL);
const REPOSITORY_ID = uniqueRepositoryId('stale-csrf-cookie');
const QUERY_URL = repositoryPageUrl(REPOSITORY_ID, 'query');
const CSRF_COOKIE = 'rdf4j-workbench-csrf';
const STALE_TOKEN = 'a'.repeat(64);
const QUERY = 'SELECT ?s WHERE { VALUES ?s { <urn:cookie:query> } }';
const FORBIDDEN_RETRY_GUIDANCE = 'Refresh this page and try again. If the error persists, clear the cookies for this '
    + 'Workbench site in your browser settings, then refresh the page and try again.';

test.setTimeout(60000);

test.beforeAll(async ({ request }) => {
    await createSeededRepository(request, serverBaseUrl(), REPOSITORY_ID, {
        graphs: [], label: 'Workbench stale CSRF cookie test'
    });
});

test.afterAll(async ({ request }) => {
    await deleteRepository(request, serverBaseUrl(), REPOSITORY_ID);
});

test.beforeEach(async ({ page }) => {
    await page.goto(QUERY_URL, { waitUntil: 'domcontentloaded' });
    await page.locator('.CodeMirror').first().waitFor({ state: 'visible' });
    await page.locator('.CodeMirror').first().evaluate((element, query) => {
        element.CodeMirror.setValue(query);
    }, QUERY);
});

function normalizeCookiePath(value) {
    const path = String(value || '').replace(/\/+$/, '');
    return path || '/';
}

async function seedM2CookieAndPreference(page) {
    const paths = await page.locator('#workbench-app').evaluate(mount => ({
        contextPath: mount.getAttribute('data-workbench-context-path'),
        basePath: mount.getAttribute('data-workbench-base-path')
    }));
    paths.contextPath = normalizeCookiePath(paths.contextPath);
    paths.basePath = normalizeCookiePath(paths.basePath);
    test.skip(paths.basePath === paths.contextPath,
        'the Workbench mount cookie is canonical in this WAR deployment');

    const initialCookies = await page.context().cookies(WORKBENCH_BASE_URL);
    const currentToken = initialCookies.find(cookie => cookie.name === CSRF_COOKIE
        && cookie.path === paths.contextPath);
    expect(currentToken, 'the current token should match the servlet context path').toBeTruthy();
    expect(currentToken.value).toMatch(/^[0-9a-f]{64}$/);

    await page.context().addCookies([
        {
            name: CSRF_COOKIE,
            value: STALE_TOKEN,
            domain: WORKBENCH.hostname,
            path: paths.basePath,
            sameSite: 'Strict'
        },
        {
            name: 'workbench-preference',
            value: 'keep',
            domain: WORKBENCH.hostname,
            path: paths.basePath
        }
    ]);

    const visible = await page.evaluate(() => document.cookie.split(';').map(value => value.trim()));
    expect(visible.find(value => value.startsWith(CSRF_COOKIE + '='))).toBe(CSRF_COOKIE + '=' + STALE_TOKEN,
        'the old, more-specific M2 cookie shadows the current token on the Workbench path');
    expect(visible).toContain('workbench-preference=keep');
    return { currentToken: currentToken.value, ...paths };
}

function isQueryResponse(response, action) {
    const request = response.request();
    return request.method() === 'POST'
        && request.url().startsWith(QUERY_URL)
        && new URLSearchParams(request.postData() || '').get('action') === action;
}

async function expectShadowRemovedAndCurrentStateKept(page, state) {
    await expect.poll(async () => {
        return (await page.context().cookies(WORKBENCH_BASE_URL)).some(cookie =>
            cookie.name === CSRF_COOKIE && cookie.path === state.basePath);
    }).toBe(false);
    const cookies = await page.context().cookies(WORKBENCH_BASE_URL);
    expect(cookies.find(cookie => cookie.name === CSRF_COOKIE && cookie.path === state.contextPath)?.value)
        .toBe(state.currentToken);
    expect(cookies.find(cookie => cookie.name === 'workbench-preference'
        && cookie.path === state.basePath)?.value).toBe('keep');
}

test('Execute shows the first 403, clears only the shadow cookie, and succeeds on user retry', async ({ page }) => {
    const state = await seedM2CookieAndPreference(page);
    const requests = [];
    page.on('request', request => {
        if (request.method() === 'POST' && request.url().startsWith(QUERY_URL)
                && new URLSearchParams(request.postData() || '').get('action') === 'exec') {
            requests.push(request);
        }
    });

    const firstResponsePromise = page.waitForResponse(response => isQueryResponse(response, 'exec'));
    await page.locator('#exec').click();
    const firstResponse = await firstResponsePromise;
    expect(firstResponse.status()).toBe(403);
    expect((await firstResponse.request().allHeaders())['x-rdf4j-csrf-token']).toBe(STALE_TOKEN);
    await expect(page.locator('#query-results')).toHaveAttribute('aria-busy', 'false');
    const errorMessage = page.locator('#query-results [role="alert"] .query-result-error__message');
    await expect(errorMessage).toContainText('Workbench could not execute the query (HTTP 403 Forbidden).');
    await expect(errorMessage).toContainText(FORBIDDEN_RETRY_GUIDANCE);
    expect(requests).toHaveLength(1, 'the UI leaves the first forbidden response visible for the user');
    await expectShadowRemovedAndCurrentStateKept(page, state);

    const retryResponsePromise = page.waitForResponse(response => isQueryResponse(response, 'exec'));
    await page.locator('#exec').click();
    const retryResponse = await retryResponsePromise;
    expect(retryResponse.status()).toBe(200);
    expect((await retryResponse.request().allHeaders())['x-rdf4j-csrf-token']).toBe(state.currentToken);
    await expect(page.locator('#query-results')).toHaveAttribute('aria-busy', 'false');
    await expect(page.locator('#query-results .query-result-status')).toContainText('complete');
    await expect(page.locator('#query-results')).toContainText('urn:cookie:query');
    expect(requests).toHaveLength(2);
});

test('Explain shows the first 403, clears only the shadow cookie, and succeeds on user retry', async ({ page }) => {
    const state = await seedM2CookieAndPreference(page);
    const requests = [];
    page.on('request', request => {
        if (request.method() === 'POST' && request.url().startsWith(QUERY_URL)
                && new URLSearchParams(request.postData() || '').get('action') === 'explain') {
            requests.push(request);
        }
    });

    const firstResponsePromise = page.waitForResponse(response => isQueryResponse(response, 'explain'));
    await page.locator('#explain-trigger').click();
    const firstResponse = await firstResponsePromise;
    expect(firstResponse.status()).toBe(403);
    expect((await firstResponse.request().allHeaders())['x-rdf4j-csrf-token']).toBe(STALE_TOKEN);
    const errorMessage = page.locator('#query-explanation-status');
    await expect(errorMessage).toContainText('Workbench could not generate the explanation (HTTP 403 Forbidden).');
    await expect(errorMessage).toContainText(FORBIDDEN_RETRY_GUIDANCE);
    expect(requests).toHaveLength(1, 'the UI leaves the first forbidden response visible for the user');
    await expectShadowRemovedAndCurrentStateKept(page, state);

    const retryResponsePromise = page.waitForResponse(response => isQueryResponse(response, 'explain'));
    await page.locator('#explain-trigger').click();
    const retryResponse = await retryResponsePromise;
    expect(retryResponse.status()).toBe(200);
    expect((await retryResponse.request().allHeaders())['x-rdf4j-csrf-token']).toBe(state.currentToken);
    await expect(page.locator('#query-explanation')).toContainText(/BindingSetAssignment|Projection/i);
    expect(requests).toHaveLength(2);
});

