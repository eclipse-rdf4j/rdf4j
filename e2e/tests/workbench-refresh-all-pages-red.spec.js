// @ts-check
const { test, expect } = require('@playwright/test');

const WORKBENCH_BASE_URL = process.env.RDF4J_WORKBENCH_BASE_URL || 'http://127.0.0.1:8080/rdf4j-workbench';
const REPOSITORY_ID = 'query-refinement-baseline-20260923-2365';

test.describe('Workbench-wide refresh seed', () => {
    test('shared shell exposes a scoped refresh surface and responsive navigation', async ({ page }) => {
        await page.setViewportSize({ width: 390, height: 1000 });
        await page.goto(`${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/summary`, { waitUntil: 'domcontentloaded' });

        await expect(page.locator('link[href*="styles/workbench-refresh.css"]')).toHaveCount(1);
        await expect(page.locator('#workbench-navigation-disclosure')).toHaveCount(1);
        await expect(page.locator('#workbench-navigation-disclosure')).not.toHaveAttribute('open', '');
        await expect(page.locator('#workbench-navigation-summary')).toContainText('Menu');
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

        await expect(page.locator('details.workbench-advanced')).toHaveCount(1);
        await expect(page.locator('details.workbench-advanced')).not.toHaveAttribute('open', '');
        await expect(page.locator('#create')).toBeVisible();
        await expect(page.locator('#create').locator('xpath=ancestor::details')).toHaveCount(0);
        await expect(page.locator('details.workbench-advanced summary')).toContainText('Advanced settings');
    });
});
