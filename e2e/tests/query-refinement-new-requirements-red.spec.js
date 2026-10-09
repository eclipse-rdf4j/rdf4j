// @ts-check
const { test, expect } = require('@playwright/test');
const {
    createSeededRepository,
    deleteRepository,
    repositoryPageUrl,
    runQuery,
    serverBaseUrl,
    uniqueRepositoryId
} = require('./workbench-test-helpers.js');

// Migrated with the redesign (see .agent/execplans/workbench-stale-spec-migration-20261002.md): the result is read on
// the Query page instead of in the retired result iframe, and every settings pane opens on its own (app-shell plan
// M14.3), so the 390 px test opens the panes one after another.

const REPOSITORY_ID = uniqueRepositoryId('query-refinement');
const QUERY_URL = repositoryPageUrl(REPOSITORY_ID, 'query');

test.beforeAll(async ({ request }) => {
    await createSeededRepository(request, serverBaseUrl(), REPOSITORY_ID,
        { graphs: [], label: 'Query refinement test' });
});

test.afterAll(async ({ request }) => {
    await deleteRepository(request, serverBaseUrl(), REPOSITORY_ID);
});

/** Runs the query and returns the streamed result on the Query page once it has finished. */
async function execute(page, query) {
    await runQuery(page, query);
    const result = page.locator('#query-results [data-query-stream-root]');
    await expect(result).toHaveCount(1, { timeout: 30000 });
    await expect(page.locator('#query-request-id')).toHaveValue('', { timeout: 30000 });
    return result;
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
    const result = await execute(page, wideQuery());
    await result.locator('.query-result-options-toggle').press('Enter');
    const layout = result.getByLabel('Result layout', { exact: true });
    await expect(layout).toHaveCount(1);
    await expect(layout.locator('option')).toHaveText([/auto/i, /table/i, /records?/i]);
    await expect(result.getByRole('checkbox', { name: /wrap values/i })).toHaveCount(1);
});

test('automatic layout keeps every field accessible without page overflow', async ({ page }) => {
    const result = await execute(page, wideQuery());
    await result.locator('.query-result-options-toggle').press('Enter');
    const layout = result.getByLabel('Result layout', { exact: true });
    await expect(layout).toHaveCount(1);
    await layout.selectOption('auto');
    await expect(result.getByRole('checkbox', { name: /wrap values/i })).toBeChecked();
    const visibleRecords = result.locator('[id^="query-result-records-"]');
    await expect(visibleRecords).toBeVisible();
    await expect(visibleRecords.getByText('alpha', { exact: false }).first()).toBeVisible();
    await expect(visibleRecords.getByText('tail', { exact: false }).first()).toBeVisible();
    // Record labels name the variables as written in the query, with the ? (app-shell plan M4).
    for (const field of ['s', 'p', 'o', 'label', 'long', 'kind', 'extra', 'tail']) {
        await expect(visibleRecords.getByText(`?${field}`, { exact: true }).first()).toBeVisible();
    }
    const geometry = await result.evaluate(element => ({
        clientWidth: element.clientWidth,
        scrollWidth: element.scrollWidth
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
    await execute(page, wideQuery());
    const initialUrl = page.url();
    const fullScreen = page.locator('#query-results').getByRole('button', { name: /full.?screen/i });
    await expect(fullScreen).toHaveCount(1);
    await fullScreen.click();
    await expect.poll(async () => page.evaluate(() => {
        const host = document.querySelector('#query-results');
        const rect = host?.getBoundingClientRect();
        return Boolean(rect && rect.width >= window.innerWidth - 2 && rect.height >= window.innerHeight - 2
            && document.documentElement.scrollWidth <= window.innerWidth);
    })).toBe(true);
    expect(page.context().pages()).toHaveLength(1);
    expect(page.url()).toBe(initialUrl);
    expect(requests).toHaveLength(1);
    await page.keyboard.press('Escape');
    await expect.poll(async () => page.evaluate(() => {
        const host = document.querySelector('#query-results');
        const rect = host?.getBoundingClientRect();
        const fullState = host?.matches('[data-fullscreen="true"],.is-fullscreen,.query-results--fullscreen');
        return Boolean(rect && !fullState && rect.width < window.innerWidth - 2);
    })).toBe(true);
    const focusState = await page.evaluate(() => {
        const focused = document.activeElement;
        return {
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
    const result = await execute(page, 'SELECT ?s ?p ?o WHERE { VALUES (?s ?p ?o) { ("alpha" <urn:p> "first") ("beta" <urn:p> "second") } }');
    const panes = [
        { toggle: page.locator('#save-query-toggle'), control: page.locator('#save-private') },
        { toggle: page.locator('#query-options-toggle'), control: page.locator('#infer') },
        { toggle: result.locator('.query-result-download-toggle'), control: result.locator('select[name="Accept"]') },
        { toggle: result.locator('.query-result-options-toggle'), control: result.locator('input[name="show-datatypes"]') }
    ];
    for (const pane of panes) {
        // An open pane lies over the page below it, so each pane is closed with its toggle before the next one opens.
        await pane.toggle.click();
        await expect(pane.toggle).toHaveAttribute('aria-expanded', 'true');
        const panel = page.locator(`#${await pane.toggle.getAttribute('aria-controls')}`);
        await expect(panel).toBeVisible();
        await expect.poll(() => panel.evaluate(element => element.getAnimations({ subtree: true })
            .filter(animation => animation.playState === 'running').length)).toBe(0);
        const metrics = await panel.evaluate(element => {
            const panelRect = element.getBoundingClientRect();
            const controls = Array.from(element.querySelectorAll('input:not([type="hidden"]), select, button, label'))
                .map(control => control.getBoundingClientRect()).filter(rect => rect.width > 0);
            return {
                clientWidth: document.documentElement.clientWidth,
                scrollWidth: document.documentElement.scrollWidth,
                panel: { clientWidth: element.clientWidth, scrollWidth: element.scrollWidth },
                controls: controls.length,
                controlsLeft: Math.min(...controls.map(rect => rect.left)) - panelRect.left,
                controlsRight: panelRect.right - Math.max(...controls.map(rect => rect.right))
            };
        });
        expect(metrics.scrollWidth).toBeLessThanOrEqual(metrics.clientWidth);
        expect(metrics.panel.scrollWidth).toBeLessThanOrEqual(metrics.panel.clientWidth);
        expect(metrics.controls).toBeGreaterThan(0);
        expect(metrics.controlsLeft).toBeGreaterThanOrEqual(0);
        expect(metrics.controlsRight).toBeGreaterThanOrEqual(0);
        await expect(pane.control).toBeVisible();
        await pane.toggle.click();
        await expect(panel).toBeHidden();
    }
});

test('query shell uses outline icons and a responsive Menu disclosure', async ({ page }) => {
    // The menu and action icons are the shared outline icons, svg.workbench-action-icon (app-shell plan M1). The
    // default menu has 17 entries (app-shell plan, decision on the default menu groups), each with its icon.
    const desktopIcons = await page.evaluate(() => {
        const entries = Array.from(document.querySelectorAll('#navigation li > a, #navigation li > span.disabled'));
        return {
            entries: entries.length,
            entriesWithIcon: entries.filter(entry => entry.querySelector('svg.workbench-action-icon')).length,
            actions: document.querySelectorAll('.query-actions-toolbar svg.workbench-action-icon').length
        };
    });
    expect(desktopIcons.entries).toBeGreaterThanOrEqual(17);
    expect(desktopIcons.entriesWithIcon).toBe(desktopIcons.entries);
    expect(desktopIcons.actions).toBeGreaterThanOrEqual(4);

    await page.setViewportSize({ width: 390, height: 900 });
    await page.reload({ waitUntil: 'domcontentloaded' });
    await page.locator('.CodeMirror').first().waitFor({ state: 'visible' });
    const menu = page.locator('#workbench-menu-sheet');
    await expect(menu).toBeHidden();
    await expect(page.locator('#navigation a').first()).toBeHidden();
    await page.locator('#workbench-menu-button').press('Enter');
    await expect(menu).toBeVisible();
    await expect(menu.locator('a').first()).toBeVisible();
    await page.keyboard.press('Escape');
    // Classic scrollbars (Linux, Windows) keep a stable gutter, so the page may be narrower than the viewport;
    // closing the menu must leave no horizontal overflow.
    await expect.poll(() => page.evaluate(() => ({
        clientWidth: document.documentElement.clientWidth,
        overflows: document.documentElement.scrollWidth > document.documentElement.clientWidth
    }))).toEqual({ clientWidth: 390, overflows: false });

    await page.setViewportSize({ width: 1440, height: 900 });
    await page.reload({ waitUntil: 'domcontentloaded' });
    await page.locator('.CodeMirror').first().waitFor({ state: 'visible' });
    await expect(page.locator('#workbench-menu-button')).toBeHidden();
    await expect(page.locator('#navigation a').first()).toBeVisible();
});
