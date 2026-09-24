// @ts-check
const { test, expect } = require('@playwright/test');

const WORKBENCH_BASE_URL = process.env.RDF4J_WORKBENCH_BASE_URL || 'http://127.0.0.1:8080/rdf4j-workbench';
const REPOSITORY_ID = 'query-refinement-baseline-20260923-2365';
const QUERY_URL = `${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/query`;

async function setQuery(page, query) {
    await page.locator('.CodeMirror').first().evaluate((element, value) => element.CodeMirror.setValue(value), query);
}

async function execute(page, query) {
    await setQuery(page, query);
    await page.locator('#exec').click();
    const frame = page.frameLocator('#query-results-frame');
    await expect(frame.locator('#rdf4j-query-result')).toHaveCount(1, { timeout: 30000 });
    await expect(page.locator('#query-request-id')).toHaveValue('', { timeout: 30000 });
    return frame;
}

function wideQuery() {
    return `SELECT ?s ?p ?o ?label ?long ?kind ?extra ?tail WHERE { VALUES (?s ?p ?o ?label ?long ?kind ?extra ?tail) {
        ("alpha" <urn:example:p> "first" "label alpha" "A long value that should wrap instead of forcing a page-wide horizontal scrollbar" "kind" "extra" "tail")
        ("beta" <urn:example:p> "second" "label beta" "Another long value that must remain fully accessible in the automatic records layout" "kind" "extra" "tail")
    } }`;
}

test.beforeEach(async ({ page }) => {
    await page.goto(QUERY_URL, { waitUntil: 'domcontentloaded' });
    await page.locator('.CodeMirror').first().waitFor({ state: 'visible' });
});

test('result options expose semantic layout and wrapping controls', async ({ page }) => {
    const frame = await execute(page, wideQuery());
    await frame.locator('#query-result-options-toggle').press('Enter');
    const layout = frame.getByLabel('Layout', { exact: true });
    await expect(layout).toHaveCount(1);
    await expect(layout.locator('option')).toHaveText([/auto/i, /table/i, /records?/i]);
    await expect(frame.getByRole('checkbox', { name: /wrap values/i })).toHaveCount(1);
});

test('automatic layout keeps every field accessible without page overflow', async ({ page }) => {
    const frame = await execute(page, wideQuery());
    await frame.locator('#query-result-options-toggle').press('Enter');
    const layout = frame.getByLabel('Layout', { exact: true });
    await expect(layout).toHaveCount(1);
    await layout.selectOption('auto');
    await expect(frame.getByRole('checkbox', { name: /wrap values/i })).toBeChecked();
    const visibleRecords = frame.locator('#query-result-records');
    await expect(visibleRecords).toBeVisible();
    await expect(visibleRecords.getByText('alpha', { exact: false }).first()).toBeVisible();
    await expect(visibleRecords.getByText('tail', { exact: false }).first()).toBeVisible();
    for (const field of ['s', 'p', 'o', 'label', 'long', 'kind', 'extra', 'tail']) {
        await expect(visibleRecords.getByText(field, { exact: true }).first()).toBeVisible();
    }
    const geometry = await frame.locator('body').evaluate(body => ({
        clientWidth: body.clientWidth,
        scrollWidth: body.scrollWidth
    }));
    expect(geometry.scrollWidth).toBeLessThanOrEqual(geometry.clientWidth + 1);
    const pageGeometry = await page.evaluate(() => ({
        clientWidth: document.documentElement.clientWidth,
        scrollWidth: document.documentElement.scrollWidth
    }));
    expect(pageGeometry.scrollWidth).toBeLessThanOrEqual(pageGeometry.clientWidth);
});

test('full-screen results toggles in place and exits with Escape', async ({ page }) => {
    const requests = [];
    page.on('request', request => {
        const params = new URLSearchParams(request.method() === 'POST'
            ? request.postData() || ''
            : new URL(request.url()).search);
        if (params.get('action') === 'exec') {
            requests.push(request);
        }
    });
    const frame = await execute(page, wideQuery());
    const initialUrl = page.url();
    const frameFullScreen = frame.getByRole('button', { name: /full.?screen/i });
    const parentFullScreen = page.locator('#query-results').getByRole('button', { name: /full.?screen/i });
    const frameCount = await frameFullScreen.count();
    const parentCount = await parentFullScreen.count();
    expect(frameCount + parentCount).toBeGreaterThanOrEqual(1);
    const fullScreen = frameCount > 0 ? frameFullScreen.first() : parentFullScreen.first();
    await fullScreen.click();
    await expect.poll(async () => page.evaluate(() => {
        const host = document.querySelector('#query-results');
        const frame = document.querySelector('#query-results-frame');
        const rect = (host || frame)?.getBoundingClientRect();
        return Boolean(rect && rect.width >= window.innerWidth - 2 && rect.height >= window.innerHeight - 2
            && document.documentElement.scrollWidth <= window.innerWidth);
    })).toBe(true);
    expect(page.context().pages()).toHaveLength(1);
    expect(page.url()).toBe(initialUrl);
    expect(requests).toHaveLength(1);
    await page.keyboard.press('Escape');
    await expect.poll(async () => page.evaluate(() => {
        const host = document.querySelector('#query-results');
        const frame = document.querySelector('#query-results-frame');
        const rect = (host || frame)?.getBoundingClientRect();
        const fullState = host?.matches('[data-fullscreen="true"],.is-fullscreen,.query-results--fullscreen')
            || frame?.matches('[data-fullscreen="true"],.is-fullscreen,.query-results--fullscreen');
        return Boolean(rect && !fullState && rect.width < window.innerWidth - 2);
    })).toBe(true);
    const focusState = await page.evaluate(() => {
        const active = document.activeElement;
        const frame = document.querySelector('#query-results-frame');
        const frameActive = active === frame && frame.contentDocument && frame.contentDocument.activeElement;
        const focused = frameActive || active;
        return {
            owner: frameActive ? 'result-frame' : 'parent',
            label: focused ? focused.getAttribute('aria-label') || focused.textContent || '' : ''
        };
    });
    expect(focusState.label).toMatch(/full.?screen/i);
    expect(page.context().pages()).toHaveLength(1);
    expect(page.url()).toBe(initialUrl);
});

test('390px opened settings keep controls inside their panels', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 1000 });
    await page.reload({ waitUntil: 'domcontentloaded' });
    await page.locator('.CodeMirror').first().waitFor({ state: 'visible' });
    const frame = await execute(page, 'SELECT ?s ?p ?o WHERE { VALUES (?s ?p ?o) { ("alpha" <urn:p> "first") ("beta" <urn:p> "second") } }');
    await page.getByText(/^Save query$/i).first().click();
    await page.getByText(/^Options$/i).first().click();
    await frame.getByText(/^Download$/i).first().click();
    await frame.getByText(/^Result options$/i).first().click();
    const metrics = await page.evaluate(() => ({
        clientWidth: document.documentElement.clientWidth,
        scrollWidth: document.documentElement.scrollWidth,
        panels: [...document.querySelectorAll('#save-query-disclosure, #query-options-disclosure')].map(panel => ({
            clientWidth: panel.clientWidth,
            scrollWidth: panel.scrollWidth
        }))
    }));
    expect(metrics.scrollWidth).toBeLessThanOrEqual(metrics.clientWidth);
    for (const panel of metrics.panels) {
        expect(panel.scrollWidth).toBeLessThanOrEqual(panel.clientWidth);
    }
    await expect(page.getByText(/Save privately/i)).toBeVisible();
    await expect(frame.getByText(/Show data types/i)).toBeVisible();
});

test('query shell uses outline icons and a responsive Menu disclosure', async ({ page }) => {
    const desktopIcons = await page.evaluate(() => ({
        navigation: document.querySelectorAll('#navigation svg.query-nav-icon').length,
        actions: document.querySelectorAll('.query-actions-toolbar svg.query-action-icon').length
    }));
    expect(desktopIcons.navigation).toBeGreaterThanOrEqual(18);
    expect(desktopIcons.actions).toBeGreaterThanOrEqual(4);

    await page.setViewportSize({ width: 390, height: 900 });
    await page.reload({ waitUntil: 'domcontentloaded' });
    await page.locator('.CodeMirror').first().waitFor({ state: 'visible' });
    const menu = page.locator('#workbench-navigation-disclosure');
    await expect(menu).toHaveCount(1);
    await expect(menu.locator('summary')).toHaveText(/Menu/i);
    await expect(menu).not.toHaveAttribute('open', '');
    await expect(page.locator('#navigation a').first()).toBeHidden();
    await menu.locator('summary').press('Enter');
    await expect(menu).toHaveAttribute('open', '');
    await expect(page.locator('#navigation a').first()).toBeVisible();
    await expect.poll(() => page.evaluate(() => ({
        clientWidth: document.documentElement.clientWidth,
        scrollWidth: document.documentElement.scrollWidth
    }))).toEqual({ clientWidth: 390, scrollWidth: 390 });

    await page.setViewportSize({ width: 1440, height: 900 });
    await page.reload({ waitUntil: 'domcontentloaded' });
    await page.locator('.CodeMirror').first().waitFor({ state: 'visible' });
    await expect(menu).toHaveAttribute('open', '');
    await expect(page.locator('#navigation a').first()).toBeVisible();
});
