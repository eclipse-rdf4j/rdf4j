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

test('Text explanation highlighting preserves server plaintext and toggles without refetching', async ({ page }) => {
    await createLmdbRepo(page);
    await insertChainData(page);

    const explainRequests = [];
    const consoleErrors = [];
    page.on('console', message => {
        if (message.type() === 'error') {
            consoleErrors.push(message.text());
        }
    });
    page.on('request', request => {
        if (request.method() !== 'POST' || !request.url().endsWith('/query')) {
            return;
        }
        const params = new URLSearchParams(request.postData() || '');
        if (params.get('action') === 'explain') {
            explainRequests.push(params.get('explain-format'));
        }
    });

    await page.goto(QUERY_URL);
    await page.waitForSelector('.CodeMirror');
    await setPrimaryQuery(page, JOIN_QUERY);
    await page.locator('#explain-trigger').click();
    await page.locator('#query-explanation .query-explanation-token--node-type').first().waitFor();

    await expect.poll(() => explainRequests.length).toBe(1);
    expect(explainRequests[0]).toBe('json');
    const highlightedText = await page.locator('#query-explanation').textContent();
    await expect(page.locator('#query-explanation .query-explanation-token--node-type').first()).toBeVisible();

    const plainResponse = await page.evaluate(async query => {
        const response = await fetch('query', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                'X-Requested-With': 'XMLHttpRequest'
            },
            body: new URLSearchParams({
                action: 'explain',
                explain: 'Optimized',
                'explain-format': 'text',
                'explain-request-id': `plaintext-contract-${Date.now()}`,
                infer: 'false',
                queryLn: 'SPARQL',
                ref: 'text',
                query
            }).toString()
        });
        return response.json();
    }, JOIN_QUERY);
    expect(highlightedText).toBe(plainResponse.content);

    const requestCountBeforeToggle = explainRequests.length;
    await page.locator('#explanation-highlight-hotspot').click();
    await expect(page.locator('#explanation-highlight-hotspot')).toHaveAttribute('aria-pressed', 'true');
    await expect(page.locator('#explanation-hotspot-legend')).toContainText('Cost estimate');
    await expect(page.locator('#query-explanation .query-explanation-line--hotspot').first()).toBeVisible();
    expect(await page.locator('#query-explanation').textContent()).toBe(highlightedText);
    expect(explainRequests.length).toBe(requestCountBeforeToggle);

    await page.locator('#compare-toggle').click();
    await page.locator('#query-explanation-compare .query-explanation-line--hotspot').first().waitFor();
    const primaryHeat = await page.locator('#query-explanation .query-explanation-line--hotspot')
        .first().getAttribute('data-heat');
    const compareHeat = await page.locator('#query-explanation-compare .query-explanation-line--hotspot')
        .first().getAttribute('data-heat');
    expect(primaryHeat).toBe(compareHeat);
    await expect(page.locator('.query-explanation-overlay--visible')).toHaveCount(0);
    await page.screenshot({
        path: '/tmp/rdf4j-query-explanation-desktop.png',
        fullPage: true
    });

    await page.locator('#compare-toggle').click();
    await expect(page.locator('#query-explanation-row-compare')).toBeHidden();
    await page.setViewportSize({ width: 700, height: 900 });
    await page.locator('#explanation-highlight-syntax').click();
    await page.locator('#explanation-highlight-syntax').focus();
    await page.locator('#explanation-highlight-syntax').press('ArrowRight');
    await expect(page.locator('#explanation-highlight-hotspot')).toHaveAttribute('aria-pressed', 'true');
    expect(await page.evaluate(() => document.activeElement.id)).toBe('explanation-highlight-hotspot');
    expect(await page.locator('#explanation-highlight-hotspot').evaluate(element =>
        getComputedStyle(element).outlineStyle)).not.toBe('none');
    const narrowControlBounds = await page.locator('#explanation-highlight-mode').evaluate(element => {
        const bounds = element.getBoundingClientRect();
        return { left: bounds.left, right: bounds.right, viewportWidth: window.innerWidth };
    });
    expect(narrowControlBounds.left).toBeGreaterThanOrEqual(0);
    expect(narrowControlBounds.right).toBeLessThanOrEqual(narrowControlBounds.viewportWidth);
    await page.screenshot({
        path: '/tmp/rdf4j-query-explanation-narrow.png',
        fullPage: true
    });
    expect(consoleErrors).toEqual([]);
});
