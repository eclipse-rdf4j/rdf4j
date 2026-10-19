// @ts-check
const { test, expect } = require('@playwright/test');

const WORKBENCH_URL = 'http://localhost:8080/rdf4j-workbench/';
const QUERY_URL = 'http://localhost:8080/rdf4j-workbench/repositories/testrepo1/query';
const JOIN_QUERY = `select ?b where {
  ?a ?b ?c.
  ?c ?d ?f.
}`;

test.beforeEach(async ({ page }) => {
    page.on('dialog', dialog => {
        dialog.dismiss();
    });

    await page.goto(WORKBENCH_URL);
    await page.getByText('Delete repository').click();
    await page.waitForSelector('#id');

    if (await page.locator('#id option[value="testrepo1"]').count() > 0) {
        await page.locator('#id').selectOption('testrepo1');
        await page.getByRole('button', { name: 'Delete' }).click();
        await page.getByText('List of Repositories').click();
    }
});

async function createLmdbRepo(page) {
    await page.getByText('New repository').click();
    await page.waitForSelector('#type');
    await page.locator('#type').selectOption('lmdb');
    await page.locator('#id').fill('testrepo1');
    await page.getByText('Next').click();
    await page.waitForSelector('#create');
    await page.locator('#create').click();
    await expect(page.getByText('Repository Location')).toHaveText('Repository Location');
}

async function insertChainData(page) {
    await page.getByText('SPARQL Update').click();
    await page.waitForSelector('.CodeMirror');
    await page.evaluate(() => {
        document.getElementsByClassName('CodeMirror')[0].CodeMirror.setValue(`INSERT DATA {
  <urn:a1> <urn:b1> <urn:c1> .
  <urn:c1> <urn:d1> <urn:f1> .
  <urn:a2> <urn:b2> <urn:c2> .
  <urn:c2> <urn:d2> <urn:f2> .
  <urn:a3> <urn:b3> <urn:c3> .
  <urn:c3> <urn:d3> <urn:f3> .
}`);
    });
    await page.getByRole('button', { name: 'Execute' }).click();
    await page.waitForLoadState('networkidle');
}

async function waitForExplanation(page) {
    await page.waitForFunction(() => {
        const explanation = document.getElementById('query-explanation');
        return explanation && explanation.textContent.trim().length > 0;
    });
}

async function setPrimaryQuery(page, query) {
    await page.evaluate(nextQuery => {
        document.getElementsByClassName('CodeMirror')[0].CodeMirror.setValue(nextQuery);
    }, query);
}

test('Executed explanation hides telemetry stability stats for LMDB queries', async ({ page }) => {
    await createLmdbRepo(page);
    await insertChainData(page);

    await page.goto(QUERY_URL);
    await page.waitForSelector('.CodeMirror');
    await setPrimaryQuery(page, JOIN_QUERY);

    await page.locator('#explain-trigger').click();
    await waitForExplanation(page);
    const initialExplanation = await page.locator('#query-explanation').textContent();

    await page.locator('#explain-level').selectOption('Executed');
    await page.locator('#explain-trigger').click();
    await page.waitForFunction(previousExplanation => {
        const explanation = document.getElementById('query-explanation');
        const text = explanation && explanation.textContent.trim();
        return text && text.length > 0 && text !== previousExplanation;
    }, initialExplanation && initialExplanation.trim());

    const explanation = await page.locator('#query-explanation').textContent();

    await expect(explanation).toContain('StatementPattern [index: spoc]');
    await expect(explanation).not.toContain('sampleCountActual=');
    await expect(explanation).not.toContain('varianceActual=');
    await expect(explanation).not.toContain('stddevActual=');
    await expect(explanation).not.toContain('confidenceScoreActual=');
});
