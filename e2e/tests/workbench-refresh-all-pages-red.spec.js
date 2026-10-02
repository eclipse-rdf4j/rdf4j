// @ts-check
const { test, expect } = require('@playwright/test');
const {
    deleteRepository,
    memoryRepositoryConfiguration,
    serverBaseUrl,
    uniqueRepositoryId,
    workbenchBaseUrl
} = require('./workbench-test-helpers');

const WORKBENCH_BASE_URL = workbenchBaseUrl();
const REPOSITORY_ID = uniqueRepositoryId('workbench-refresh-seed');

test.beforeAll(async ({ request }) => {
    const response = await request.put(`${serverBaseUrl()}/repositories/${REPOSITORY_ID}`, {
        headers: { 'Content-Type': 'text/turtle' },
        data: memoryRepositoryConfiguration(REPOSITORY_ID, 'Workbench refresh seed')
    });
    expect([200, 201, 204]).toContain(response.status());
});

test.afterAll(async ({ request }) => {
    await deleteRepository(request, serverBaseUrl(), REPOSITORY_ID);
});

test.describe('Workbench-wide refresh seed', () => {
    test('shared shell exposes a scoped refresh surface and responsive navigation', async ({ page }) => {
        await page.setViewportSize({ width: 390, height: 1000 });
        await page.goto(`${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/summary`, { waitUntil: 'domcontentloaded' });

        await expect(page.locator('#workbench-summary')).toBeVisible();
        await expect(page.locator('link[href*="styles/workbench-refresh.css"]')).toHaveCount(1);
        await expect(page.locator('#workbench-navigation-disclosure')).toBeHidden();
        await expect(page.locator('#workbench-menu-button')).toHaveAttribute('aria-label', 'Menu');
        await expect(page.locator('#workbench-menu-sheet')).toBeHidden();
        await expect(page.locator('#navigation')).toHaveClass(/workbench-nav/);
        await expect(page.locator('#content')).toHaveClass(/workbench-main/);
        await expect(page.locator('#workbench-page-surface')).toHaveClass(/workbench-page-surface/);

        const pageMetrics = await page.evaluate(() => ({
            bodyOverflow: document.documentElement.scrollWidth - document.documentElement.clientWidth,
            backgroundImage: getComputedStyle(document.body).backgroundImage
        }));
        expect(pageMetrics.bodyOverflow).toBe(0);
        expect(pageMetrics.backgroundImage).toBe('none');
    });

    test('repository creation keeps advanced fields discoverable and actions outside disclosure', async ({ page }) => {
        await page.goto(`${WORKBENCH_BASE_URL}/repositories/NONE/create?type=memory-rdfs-dt`, { waitUntil: 'domcontentloaded' });

        // Advanced settings are the shared disclosure component (a toggle button and the pane it controls) instead of
        // a native details element; they open over the form (plan task M14.3).
        const advanced = page.locator('.workbench-advanced');
        const toggle = advanced.locator(':scope > .workbench-disclosure__toggle');
        await expect(advanced).toHaveCount(1);
        await expect(toggle).toHaveAttribute('aria-expanded', 'false');
        await expect(page.locator(`#${await toggle.getAttribute('aria-controls')}`)).toBeHidden();
        await expect(page.locator('#create')).toBeVisible();
        await expect(advanced.locator('#create')).toHaveCount(0);
        await expect(toggle).toContainText('Advanced settings');
    });
});
