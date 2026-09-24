// @ts-check
const { test, expect } = require('@playwright/test');

const SERVER_BASE_URL = process.env.RDF4J_SERVER_BASE_URL || 'http://127.0.0.1:8080/rdf4j-server';
const WORKBENCH_BASE_URL = process.env.RDF4J_WORKBENCH_BASE_URL || 'http://127.0.0.1:8080/rdf4j-workbench';
const REPOSITORY_ID = 'query-refresh-layout';
const MEMBER_REPOSITORY_ID = 'query-refresh-layout-member';

test.beforeEach(async ({ request }) => {
    for (const repositoryId of [REPOSITORY_ID, MEMBER_REPOSITORY_ID]) {
        await request.delete(`${SERVER_BASE_URL}/repositories/${repositoryId}`);
        const response = await request.put(`${SERVER_BASE_URL}/repositories/${repositoryId}`, {
            headers: { 'Content-Type': 'text/turtle' },
            data: nativeRepositoryConfig(repositoryId)
        });
        expect([200, 201, 204]).toContain(response.status());
    }
});

async function viewportMetrics(page) {
    return page.evaluate(() => ({
        viewport: document.documentElement ? document.documentElement.clientWidth : window.innerWidth,
        scrollWidth: document.documentElement ? document.documentElement.scrollWidth : window.innerWidth,
        bodyScrollWidth: document.body ? document.body.scrollWidth : window.innerWidth
    }));
}

function nativeRepositoryConfig(repositoryId) {
    return `@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix config: <tag:rdf4j.org,2023:config/>.

[] a config:Repository ;
   config:rep.id "${repositoryId}" ;
   rdfs:label "Workbench refresh corrections test" ;
   config:rep.impl [
      config:rep.type "openrdf:SailRepository" ;
      config:sail.impl [
         config:sail.type "openrdf:MemoryStore"
      ]
   ].
`;
}

test('mobile workbench forms keep editors and selected source fields inside the viewport', async ({ page }) => {
    for (const width of [320, 390, 768]) {
        await page.setViewportSize({ width, height: 900 });
        await page.goto(`${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/update`, { waitUntil: 'domcontentloaded' });
        await page.locator('#update-form').waitFor({ state: 'attached' });
        const updateMetrics = await viewportMetrics(page);
        expect(updateMetrics.scrollWidth, `SPARQL Update overflows at ${width}px`).toBeLessThanOrEqual(width);

        await page.goto(`${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/add`, { waitUntil: 'domcontentloaded' });
        await page.locator('#add-source-tabs').waitFor({ state: 'attached' });
        const addMetrics = await viewportMetrics(page);
        expect(addMetrics.scrollWidth, `Add RDF overflows at ${width}px`).toBeLessThanOrEqual(width);
        await expect(page.locator('#source-file')).toBeChecked();
        await expect(page.locator('.add-source-panel:visible')).toHaveCount(1);
        await expect(page.locator('#add-source-file-panel')).toBeVisible();
        await expect(page.locator('#Content-Type')).toBeVisible();
    }
});

test('federation keeps required identity and member controls visible', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 900 });
    await page.goto(`${WORKBENCH_BASE_URL}/repositories/NONE/create?type=federate`, { waitUntil: 'domcontentloaded' });
    await expect(page.locator('#id')).toBeVisible();
    await expect(page.locator('#title')).toBeVisible();
    await expect(page.locator('.memberID')).toHaveCount(2);
    for (const control of ['#id', '#title', '.memberID']) {
        await expect(page.locator(control).first()).toBeVisible();
    }
    const metrics = await viewportMetrics(page);
    expect(metrics.scrollWidth).toBeLessThanOrEqual(390);
    await expect(page.locator('#create')).toBeVisible();
});

test('mobile repository data uses complete labelled records without page overflow', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 900 });
    await page.goto(`${WORKBENCH_BASE_URL}/repositories/NONE/repositories`, { waitUntil: 'domcontentloaded' });
    await expect(page.locator('#repositories-results table.data')).toHaveCount(1);
    await expect(page.locator('#repositories-results table.data tbody tr')).not.toHaveCount(0);
    await expect(page.locator('#repositories-results table.data td[data-label]')).not.toHaveCount(0);
    const metrics = await viewportMetrics(page);
    expect(metrics.scrollWidth).toBeLessThanOrEqual(390);
    const surface = await page.locator('#workbench-page-surface').evaluate(element => {
        const style = getComputedStyle(element);
        return { background: style.backgroundColor, shadow: style.boxShadow };
    });
    expect(surface.shadow).toBe('none');
});

test('creation actions use compact labels-above-fields geometry', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 1000 });
    await page.goto(`${WORKBENCH_BASE_URL}/repositories/NONE/create?type=memory-rdfs-dt`, { waitUntil: 'domcontentloaded' });
    const form = page.locator('form[action="create"]');
    await expect(form.locator('input#create')).toBeVisible();
    const geometry = await form.evaluate(element => {
        const rect = element.getBoundingClientRect();
        const action = element.querySelector('input#create');
        const actionRect = action.getBoundingClientRect();
        const style = getComputedStyle(element);
        return { width: rect.width, actionWidth: actionRect.width, maxWidth: style.maxWidth };
    });
    expect(geometry.width).toBeLessThanOrEqual(760);
    expect(geometry.actionWidth).toBeLessThan(240);
    expect(geometry.maxWidth).toContain('720');
});
