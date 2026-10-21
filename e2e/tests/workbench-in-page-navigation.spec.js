// @ts-check
const { test, expect } = require('@playwright/test');
const {
    createSeededRepository,
    deleteRepository,
    repositoryPageUrl,
    serverBaseUrl,
    uniqueRepositoryId,
    waitForRoute
} = require('./workbench-test-helpers');

const repositoryId = uniqueRepositoryId('workbench-in-page-navigation');

test.beforeAll(async ({ request }) => {
    await createSeededRepository(request, serverBaseUrl(), repositoryId);
});

test.afterAll(async ({ request }) => {
    await deleteRepository(request, serverBaseUrl(), repositoryId);
});

/** Opens a page through its menu link, as a person does, and waits until the router shows it. */
async function openFromMenu(page, view) {
    await page.locator(`#navigation a[href="${view}"]`).click();
    await waitForRoute(page, view, { url: (url) => url.pathname.endsWith('/' + view) });
}

test('the menu keeps opening pages after the Query page was shown', async ({ page }) => {
    const errors = [];
    page.on('pageerror', (error) => errors.push(error.message));
    await page.goto(repositoryPageUrl(repositoryId, 'summary'));
    await waitForRoute(page, 'summary');

    for (const view of ['query', 'namespaces', 'types', 'query', 'contexts', 'summary']) {
        await openFromMenu(page, view);
        await expect(page.locator('#workbench-outlet')).toHaveAttribute('data-workbench-route', view);
    }

    await expect(page.locator('[aria-busy="true"]#workbench-outlet')).toHaveCount(0);
    await expect(page.locator('[data-workbench-route-loading="true"]')).toHaveCount(0);
    expect(errors).toEqual([]);
});

// What is typed must survive the page rendering again while it stays shown: workbench-browsing.spec.js
// ('Counts that arrive later update the Types page in place') holds the counts back to check that.
test('the Types filter field shows the filter of the page opened, not what was typed on the page before', async ({ page }) => {
    await page.goto(repositoryPageUrl(repositoryId, 'types') + '?filter=offer');
    await waitForRoute(page, 'types');
    const filter = page.locator('#types-filter');
    await expect(filter).toHaveValue('offer');

    await filter.fill('typed but not sent');
    await openFromMenu(page, 'types');
    await expect(filter).toHaveValue('');
});

test('the page heading focused after an in-page navigation shows no focus ring', async ({ page }) => {
    await page.goto(repositoryPageUrl(repositoryId, 'summary'));
    await waitForRoute(page, 'summary');

    for (const view of ['namespaces', 'types', 'query', 'summary']) {
        await openFromMenu(page, view);
        const heading = page.locator('#workbench-outlet h1');
        // Focus moves to the heading so screen readers start reading the new page there; it is not a control.
        await expect(heading).toBeFocused();
        expect(await heading.evaluate((element) => getComputedStyle(element).outlineStyle), view).toBe('none');
    }
});
