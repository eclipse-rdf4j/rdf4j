// @ts-check
const { test, expect } = require('@playwright/test');
const {
    deleteRepository,
    memoryRepositoryConfiguration,
    openQueryPage,
    serverBaseUrl,
    uniqueRepositoryId
} = require('./workbench-test-helpers.js');

// Retired with the redesign (see .agent/execplans/workbench-stale-spec-migration-20261002.md): "keeps query disclosure
// triggers fixed while panels open" opened both query panes at once and expected toolbar-wide stacked panes; panes now
// open one at a time over the page, which workbench-settings-panes.spec.js checks, including that the toggles do not
// move. "uses a stable result header grid and readable narrow title" measured the retired result iframe; the narrow
// result toolbar is checked by workbench-dropdown-detail-parity.spec.js ("dynamic result details ...").

const REPOSITORY_ID = uniqueRepositoryId('query-structure');

test.beforeAll(async ({ request }) => {
    const created = await request.put(`${serverBaseUrl()}/repositories/${REPOSITORY_ID}`, {
        headers: { 'Content-Type': 'text/turtle' },
        data: memoryRepositoryConfiguration(REPOSITORY_ID, 'Query structure test')
    });
    expect([200, 201, 204]).toContain(created.status());
});

test.afterAll(async ({ request }) => {
    await deleteRepository(request, serverBaseUrl(), REPOSITORY_ID);
});

test('hides empty query errors and keeps Explain secondary', async ({ page }) => {
    await openQueryPage(page, REPOSITORY_ID);
    await expect(page.locator('#queryString\\.errors')).toBeHidden();
    const colors = await page.evaluate(() => {
        const probe = document.createElement('span');
        probe.style.color = 'var(--workbench-primary)';
        document.querySelector('#query-form').append(probe);
        const primary = getComputedStyle(probe).color;
        probe.remove();
        return {
            execute: getComputedStyle(document.querySelector('#exec')).backgroundColor,
            explain: getComputedStyle(document.querySelector('#explain-trigger')).backgroundColor,
            resultOptionCheck: getComputedStyle(document.querySelector('#infer')).accentColor,
            primary
        };
    });
    expect(colors.explain).not.toBe(colors.execute);
    expect(colors.resultOptionCheck, 'query option checkboxes use the Workbench primary color').toBe(colors.primary);
});
