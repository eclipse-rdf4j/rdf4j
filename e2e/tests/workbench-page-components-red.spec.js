// @ts-check
const { test, expect } = require('@playwright/test');
const {
    deleteRepository,
    memoryRepositoryConfiguration,
    serverBaseUrl,
    uniqueRepositoryId,
    workbenchBaseUrl
} = require('./workbench-test-helpers');

// Retired with the redesign (see .agent/execplans/workbench-stale-spec-migration-20261002.md): "narrow embedded result
// disclosures stay within one readable column" opened the result's Download and Options panes at once inside the
// retired result iframe and expected them stacked across the toolbar; panes now open one at a time over the page (plan
// task M14.3). workbench-dropdown-detail-parity.spec.js ("dynamic result details keep distinct triggers and responsive
// Format-style fields") checks at 390 px that each pane keeps its fields in one column without overflowing the page,
// and workbench-settings-panes.spec.js ("390 px › result Download/Display opens over the page ...") that each stays
// inside the viewport.

const WORKBENCH_BASE_URL = workbenchBaseUrl();
const REPOSITORY_ID = uniqueRepositoryId('workbench-page-components');

test.beforeAll(async ({ request }) => {
    const response = await request.put(`${serverBaseUrl()}/repositories/${REPOSITORY_ID}`, {
        headers: { 'Content-Type': 'text/turtle' },
        data: memoryRepositoryConfiguration(REPOSITORY_ID, 'Workbench page component test')
    });
    expect([200, 201, 204]).toContain(response.status());
});

test.afterAll(async ({ request }) => {
    await deleteRepository(request, serverBaseUrl(), REPOSITORY_ID);
});

/**
 * Settings disclosures are the shared component (a toggle button and the pane it controls) instead of native
 * details elements; checks that the one with this id is closed and that the given actions are outside it.
 */
async function expectClosedDisclosure(page, disclosureId, actionsId) {
    const toggle = page.locator(`#${disclosureId} > .workbench-disclosure__toggle`);
    await expect(toggle).toHaveAttribute('aria-expanded', 'false');
    await expect(page.locator(`#${await toggle.getAttribute('aria-controls')}`)).toBeHidden();
    if (actionsId) {
        await expect(page.locator(`#${actionsId}`)).toHaveCount(1);
        await expect(page.locator(`#${disclosureId} #${actionsId}`)).toHaveCount(0);
    }
}

test('Add RDF exposes source choices and a collapsed import settings panel', async ({ page }) => {
    await page.goto(`${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/add`, { waitUntil: 'domcontentloaded' });
    await expect(page.locator('#add-source-tabs')).toHaveCount(1);
    await expect(page.locator('#add-source-tabs input[type="radio"]')).toHaveCount(3);
    await expect(page.locator('#add-import-settings')).toHaveCount(1);
    await expectClosedDisclosure(page, 'add-import-settings', 'add-upload-actions');
});

test('Summary and destructive pages expose labelled disclosure and warning regions', async ({ page }) => {
    await page.goto(`${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/summary`, { waitUntil: 'domcontentloaded' });
    await expect(page.locator('#summary-config-model')).toHaveCount(1);
    await expect(page.locator('#summary-config-model')).not.toHaveAttribute('open', '');
    await expect(page.locator('#summary-config-model pre')).toBeHidden();

    await page.goto(`${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/remove`, { waitUntil: 'domcontentloaded' });
    await expect(page.locator('#remove-examples')).toHaveCount(1);
    await expect(page.locator('#remove-examples')).not.toHaveAttribute('open', '');
    await expect(page.locator('#remove-form')).toHaveCount(1);
    await expect(page.locator('#remove-warning')).toBeVisible();

    await page.goto(`${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/clear`, { waitUntil: 'domcontentloaded' });
    await expect(page.locator('#clear-form')).toHaveCount(1);
    await expect(page.locator('#clear-warning')).toBeVisible();
});

test('server connection keeps optional authentication behind a disclosure', async ({ page }) => {
    await page.goto(`${WORKBENCH_BASE_URL}/repositories/NONE/server`, { waitUntil: 'domcontentloaded' });
    await expect(page.locator('#server-form')).toHaveCount(1);
    await expect(page.locator('#server-auth')).toHaveCount(1);
    await expectClosedDisclosure(page, 'server-auth', 'server-change-actions');
    await expect(page.locator('#server-form .error:empty')).toBeHidden();
});

test('data and administration pages expose stable islands and action regions', async ({ page }) => {
    await page.goto(`${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/export`, { waitUntil: 'domcontentloaded' });
    // The Export redesign (plan task M6.6) makes the download form the "Download a file" card, #export-form.
    await expect(page.locator('#export-form')).toHaveCount(1);
    await expect(page.locator('#export-form #Accept')).toBeVisible();
    // Export's disclosure holds the timeout since the Export redesign.
    await expect(page.locator('#export-advanced')).toHaveCount(1);
    await expectClosedDisclosure(page, 'export-advanced');
    await expect(page.locator('#export-results')).toHaveCount(1);

    await page.goto(`${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/update`, { waitUntil: 'domcontentloaded' });
    await expect(page.locator('#update-form')).toHaveCount(1);
    await expect(page.locator('#update-editor')).toHaveCount(1);
    await expect(page.locator('#update-actions')).toHaveCount(1);

    await page.goto(`${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/namespaces`, { waitUntil: 'domcontentloaded' });
    // Plan task M6.3: no form above the namespace table; a filter sits in the card header.
    await expect(page.locator('#namespaces-filter')).toHaveCount(1);
    await expect(page.locator('#namespaces-results')).toHaveCount(1);

    await page.goto(`${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/explore`, { waitUntil: 'domcontentloaded' });
    await expect(page.locator('#explore-form')).toHaveCount(1);
    await expect(page.locator('#explore-controls')).toHaveCount(1);
    await expect(page.locator('#explore-results')).toHaveCount(1);

    await page.goto(`${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/saved-queries`, { waitUntil: 'domcontentloaded' });
    await expect(page.locator('#saved-queries')).toHaveCount(1);

    await page.goto(`${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/information`, { waitUntil: 'domcontentloaded' });
    await expect(page.locator('#information-application')).toHaveCount(1);
    await expect(page.locator('#information-runtime')).toHaveCount(1);
    await expect(page.locator('#information-memory')).toHaveCount(1);

    await page.goto(`${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/contexts`, { waitUntil: 'domcontentloaded' });
    await expect(page.locator('#contexts-results')).toHaveCount(1);
    await page.goto(`${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/types`, { waitUntil: 'domcontentloaded' });
    await expect(page.locator('#types-results')).toHaveCount(1);

    await page.goto(`${WORKBENCH_BASE_URL}/repositories/NONE/repositories`, { waitUntil: 'domcontentloaded' });
    await expect(page.locator('#repositories-results')).toHaveCount(1);
    await page.goto(`${WORKBENCH_BASE_URL}/repositories/NONE/delete`, { waitUntil: 'domcontentloaded' });
    await expect(page.locator('#delete-form')).toHaveCount(1);
    await expect(page.locator('#delete-actions')).toHaveCount(1);
});
