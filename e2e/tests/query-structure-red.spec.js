// @ts-check
const { test, expect } = require('@playwright/test');

const SERVER_BASE_URL = process.env.RDF4J_SERVER_BASE_URL || 'http://127.0.0.1:8080/rdf4j-server';
const WORKBENCH_BASE_URL = process.env.RDF4J_WORKBENCH_BASE_URL || 'http://127.0.0.1:8080/rdf4j-workbench';
const REPOSITORY_ID = 'query-refresh-layout';
const QUERY_URL = `${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/query`;

test.beforeEach(async ({ page, request }) => {
    await request.delete(`${SERVER_BASE_URL}/repositories/${REPOSITORY_ID}`);
    const createResponse = await request.put(`${SERVER_BASE_URL}/repositories/${REPOSITORY_ID}`, {
        headers: { 'Content-Type': 'text/turtle' },
        data: nativeRepositoryConfig(REPOSITORY_ID)
    });
    expect([200, 201, 204]).toContain(createResponse.status());
    await page.goto(QUERY_URL);
    await page.locator('.CodeMirror').waitFor({ state: 'visible' });
});

test('keeps query disclosure triggers fixed while panels open', async ({ page }) => {
    const saveToggle = page.locator('#save-query-toggle');
    const optionsToggle = page.locator('#query-options-toggle');
    await expect(saveToggle).toHaveCount(1);
    await expect(optionsToggle).toHaveCount(1);

    const before = await page.evaluate(() => {
        const rect = selector => document.querySelector(selector).getBoundingClientRect();
        return {
            save: rect('#save-query-toggle'),
            options: rect('#query-options-toggle'),
            toolbar: rect('.query-actions-toolbar')
        };
    });

    await saveToggle.press('Enter');
    await optionsToggle.press('Enter');
    await expect(saveToggle).toHaveAttribute('aria-expanded', 'true');
    await expect(optionsToggle).toHaveAttribute('aria-expanded', 'true');
    await expect(page.locator('#save-query-panel')).toBeVisible();
    await expect(page.locator('#query-options-panel')).toBeVisible();

    const after = await page.evaluate(() => {
        const rect = selector => document.querySelector(selector).getBoundingClientRect();
        return {
            save: rect('#save-query-toggle'),
            options: rect('#query-options-toggle'),
            toolbar: rect('.query-actions-toolbar'),
            savePanel: rect('#save-query-panel'),
            optionsPanel: rect('#query-options-panel')
        };
    });

    expect(Math.abs(after.save.left - before.save.left)).toBeLessThanOrEqual(1);
    expect(Math.abs(after.save.top - before.save.top)).toBeLessThanOrEqual(1);
    expect(Math.abs(after.options.left - before.options.left)).toBeLessThanOrEqual(1);
    expect(Math.abs(after.options.top - before.options.top)).toBeLessThanOrEqual(1);
    expect(after.savePanel.width).toBeGreaterThanOrEqual(after.toolbar.width * 0.8);
    expect(after.optionsPanel.width).toBeGreaterThanOrEqual(after.toolbar.width * 0.8);
    expect(after.optionsPanel.top).toBeGreaterThanOrEqual(after.savePanel.bottom - 1);
});

test('uses a stable result header grid and readable narrow title', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 900 });
    await page.reload();
    await page.locator('.CodeMirror').waitFor({ state: 'visible' });
    await page.locator('.CodeMirror').evaluate(element => {
        element.CodeMirror.setValue(`SELECT ?s ?p ?o WHERE {
            VALUES (?s ?p ?o) {
                ("short" <http://example.org/p> <http://example.org/resource/with-a-long-name>)
                ("literal" <http://example.org/predicate> "another value")
            }
        }`);
    });
    await page.locator('#exec').click();
    const frame = page.frameLocator('#query-results-frame');
    await expect(frame.locator('table.data tbody tr')).toHaveCount(2);
    await expect(frame.locator('#query-result-download-toggle')).toHaveCount(1);
    await expect(frame.locator('#query-result-options-toggle')).toHaveCount(1);
    await expect(frame.locator('#query-result-embedded-header h2')).toHaveText(/Query Result/);

    const metrics = await page.locator('#query-results-frame').evaluate(frameElement => {
        const documentElement = frameElement.contentDocument.documentElement;
        const rect = selector => frameElement.contentDocument.querySelector(selector).getBoundingClientRect();
        const title = rect('#query-result-embedded-header h2');
        const fullscreen = rect('#query-result-fullscreen');
        const download = rect('#query-result-download-toggle');
        const options = rect('#query-result-options-toggle');
        return {
            title,
            fullscreen,
            download,
            options,
            bodyOverflow: documentElement.scrollWidth - documentElement.clientWidth
        };
    });

    expect(metrics.title.width).toBeGreaterThanOrEqual(90);
    expect(Math.abs(metrics.title.top - metrics.fullscreen.top)).toBeLessThanOrEqual(4);
    expect(metrics.download.top).toBeGreaterThanOrEqual(Math.max(metrics.title.bottom, metrics.fullscreen.bottom) - 1);
    expect(metrics.options.top).toBeGreaterThanOrEqual(metrics.download.top - 1);
    expect(metrics.bodyOverflow).toBeLessThanOrEqual(1);

    await frame.locator('#query-result-download-toggle').press('Enter');
    await frame.locator('#query-result-options-toggle').press('Enter');
    await expect(frame.locator('#query-result-download-panel')).toBeVisible();
    await expect(frame.locator('#query-result-options-panel')).toBeVisible();
});

test('hides empty query errors and keeps Explain secondary', async ({ page }) => {
    await expect(page.locator('#queryString\\.errors')).toBeHidden();
    const colors = await page.evaluate(() => ({
        execute: getComputedStyle(document.querySelector('#exec')).backgroundColor,
        explain: getComputedStyle(document.querySelector('#explain-trigger')).backgroundColor,
        resultOptionCheck: getComputedStyle(document.querySelector('#infer')).accentColor
    }));
    expect(colors.explain).not.toBe(colors.execute);
    expect(colors.resultOptionCheck).toBe('rgb(15, 118, 110)');
});

function nativeRepositoryConfig(repositoryId) {
    return `@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix config: <tag:rdf4j.org,2023:config/>.

[] a config:Repository ;
   config:rep.id "${repositoryId}" ;
   rdfs:label "Query structure test" ;
   config:rep.impl [
      config:rep.type "openrdf:SailRepository" ;
      config:sail.impl [
         config:sail.type "openrdf:MemoryStore"
      ]
   ].`;
}
