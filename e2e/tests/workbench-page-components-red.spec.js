// @ts-check
const { test, expect } = require('@playwright/test');

const SERVER_BASE_URL = process.env.RDF4J_SERVER_BASE_URL || 'http://127.0.0.1:8080/rdf4j-server';
const WORKBENCH_BASE_URL = process.env.RDF4J_WORKBENCH_BASE_URL || 'http://127.0.0.1:8080/rdf4j-workbench';
const REPOSITORY_ID = 'query-refresh-layout';

test.beforeEach(async ({ request }) => {
    await request.delete(`${SERVER_BASE_URL}/repositories/${REPOSITORY_ID}`);
    const response = await request.put(`${SERVER_BASE_URL}/repositories/${REPOSITORY_ID}`, {
        headers: { 'Content-Type': 'text/turtle' },
        data: repositoryConfig(REPOSITORY_ID)
    });
    expect([200, 201, 204]).toContain(response.status());
});

test('Add RDF exposes source choices and a collapsed import settings panel', async ({ page }) => {
    await page.goto(`${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/add`, { waitUntil: 'domcontentloaded' });
    await expect(page.locator('#add-source-tabs')).toHaveCount(1);
    await expect(page.locator('#add-source-tabs input[type="radio"]')).toHaveCount(3);
    await expect(page.locator('#add-import-settings')).toHaveCount(1);
    await expect(page.locator('#add-import-settings')).not.toHaveAttribute('open', '');
    await expect(page.locator('#add-upload-actions')).toHaveCount(1);
    await expect(page.locator('#add-upload-actions').locator('xpath=ancestor::details')).toHaveCount(0);
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
    await expect(page.locator('#server-auth')).not.toHaveAttribute('open', '');
    await expect(page.locator('#server-change-actions')).toHaveCount(1);
    await expect(page.locator('#server-change-actions').locator('xpath=ancestor::details')).toHaveCount(0);
    await expect(page.locator('#server-form .error:empty')).toBeHidden();
});

test('data and administration pages expose stable islands and action regions', async ({ page }) => {
    await page.goto(`${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/export`, { waitUntil: 'domcontentloaded' });
    await expect(page.locator('#export-download-form')).toHaveCount(1);
    await expect(page.locator('#export-download-form #Accept')).toBeVisible();
    await expect(page.locator('#export-result-options')).toHaveCount(1);
    await expect(page.locator('#export-result-options')).not.toHaveAttribute('open', '');
    await expect(page.locator('#export-results')).toHaveCount(1);

    await page.goto(`${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/update`, { waitUntil: 'domcontentloaded' });
    await expect(page.locator('#update-form')).toHaveCount(1);
    await expect(page.locator('#update-editor')).toHaveCount(1);
    await expect(page.locator('#update-actions')).toHaveCount(1);

    await page.goto(`${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/namespaces`, { waitUntil: 'domcontentloaded' });
    await expect(page.locator('#namespaces-form')).toHaveCount(1);
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

test('narrow embedded result disclosures stay within one readable column', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 1000 });
    await page.goto(`${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/query`, { waitUntil: 'domcontentloaded' });
    const editor = page.locator('.CodeMirror').first();
    await editor.waitFor({ state: 'visible' });
    await editor.evaluate((element, query) => element.CodeMirror.setValue(query),
        'SELECT ?s ?p ?o WHERE { VALUES (?s ?p ?o) { ("alpha" <urn:p> "first") ("beta" <urn:p> "second") } }');
    await page.locator('#exec').click();
    const frame = page.frameLocator('#query-results-frame');
    await frame.locator('#rdf4j-query-result').waitFor({ state: 'attached' });
    await frame.locator('#query-result-download-toggle').press('Enter');
    await frame.locator('#query-result-options-toggle').press('Enter');
    const metrics = await frame.locator('.query-result-toolbar__disclosures').evaluate(element => {
        const boxes = Array.from(element.querySelectorAll('.query-result-disclosure')).map(disclosure => {
            const rect = disclosure.getBoundingClientRect();
            return { left: rect.left, right: rect.right, width: rect.width };
        });
        return { boxes, scrollWidth: element.scrollWidth, clientWidth: element.clientWidth };
    });
    expect(metrics.boxes).toHaveLength(2);
    expect(metrics.boxes[0].width).toBeGreaterThanOrEqual(metrics.clientWidth - 1);
    expect(metrics.boxes[1].left).toBeGreaterThanOrEqual(metrics.boxes[0].left - 1);
    expect(metrics.boxes[1].right).toBeLessThanOrEqual(metrics.boxes[0].right + 1);
    expect(metrics.scrollWidth).toBeLessThanOrEqual(metrics.clientWidth + 1);
});

function repositoryConfig(repositoryId) {
    return `@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix config: <tag:rdf4j.org,2023:config/>.

[] a config:Repository ;
   config:rep.id "${repositoryId}" ;
   rdfs:label "Workbench page component test" ;
   config:rep.impl [
      config:rep.type "openrdf:SailRepository" ;
      config:sail.impl [
         config:sail.type "openrdf:MemoryStore"
      ]
   ].`;
}
