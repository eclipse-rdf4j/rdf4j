// @ts-check
const { test, expect } = require('@playwright/test');
const {
    deleteRepository,
    repositoryPageUrl,
    serverBaseUrl,
    uniqueRepositoryId
} = require('./workbench-test-helpers');

// Each test used to delete and re-create a repository named testrepo1 through the Delete and Create pages of a
// server on localhost:8080, then insert the chain data through the Update page. The spec now creates its own LMDB
// repository with the same index order (spoc,posc,ospc, the Create page's default) and data once, through the
// server's REST API.
const REPOSITORY_ID = uniqueRepositoryId('workbench-explain');
const QUERY_URL = repositoryPageUrl(REPOSITORY_ID, 'query');
const JOIN_QUERY = `select ?b where {
  ?a ?b ?c.
  ?c ?d ?f.
}`;

test.beforeAll(async ({ request }) => {
    const repositoryUrl = `${serverBaseUrl()}/repositories/${encodeURIComponent(REPOSITORY_ID)}`;
    const created = await request.put(repositoryUrl, {
        headers: { 'Content-Type': 'text/turtle' },
        data: `@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix config: <tag:rdf4j.org,2023:config/>.
@prefix lmdb: <http://rdf4j.org/config/sail/lmdb#>.
[] a config:Repository ; config:rep.id "${REPOSITORY_ID}" ; rdfs:label "Workbench explain LMDB fixture" ;
   config:rep.impl [ config:rep.type "openrdf:SailRepository" ;
      config:sail.impl [ config:sail.type "rdf4j:LmdbStore" ; lmdb:tripleIndexes "spoc,posc,ospc" ] ].`
    });
    expect([200, 201, 204]).toContain(created.status());
    const loaded = await request.post(`${repositoryUrl}/statements`, {
        headers: { 'Content-Type': 'application/n-triples' },
        data: `<urn:a1> <urn:b1> <urn:c1> .
<urn:c1> <urn:d1> <urn:f1> .
<urn:a2> <urn:b2> <urn:c2> .
<urn:c2> <urn:d2> <urn:f2> .
<urn:a3> <urn:b3> <urn:c3> .
<urn:c3> <urn:d3> <urn:f3> .
`
    });
    expect([200, 204]).toContain(loaded.status());
});

test.afterAll(async ({ request }) => {
    await deleteRepository(request, serverBaseUrl(), REPOSITORY_ID);
});

test.beforeEach(async ({ page }) => {
    page.on('dialog', dialog => {
        dialog.dismiss();
    });
});

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
    const settingsPanel = page.locator('#explanation-settings-panel');
    const settingsToggle = page.locator('#explanation-settings-toggle');
    await settingsToggle.click();
    await expect(settingsPanel).toBeVisible();
    await expect(page.locator('#explanation-highlight-syntax')).toBeChecked();
    await expect(page.locator('#explanation-highlight-hotspot')).not.toBeChecked();
    await page.locator('#explanation-highlight-hotspot').click();
    await expect(page.locator('#explanation-highlight-hotspot')).toBeChecked();
    await expect(page.locator('#explanation-hotspot-legend')).toContainText('Cost estimate');
    await expect(page.locator('#query-explanation .query-explanation-line--hotspot').first()).toBeVisible();
    expect(await page.locator('#query-explanation').textContent()).toBe(highlightedText);
    expect(explainRequests.length).toBe(requestCountBeforeToggle);

    // Click an established explanation control outside Config so document-level
    // dismissal is exercised without the open panel intercepting the click.
    await page.locator('#explain-format').click();
    await expect(settingsPanel).toBeHidden();
    await settingsToggle.click();
    await expect(settingsPanel).toBeVisible();
    await expect(page.locator('#explanation-highlight-hotspot')).toBeChecked();

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
    await settingsToggle.click();
    await expect(settingsPanel).toBeVisible();
    await page.locator('#explanation-highlight-syntax').click();
    await page.locator('#explanation-highlight-syntax').focus();
    await page.locator('#explanation-highlight-syntax').press('ArrowRight');
    await expect(page.locator('#explanation-highlight-hotspot')).toBeChecked();
    await expect(page.locator('#explanation-highlight-syntax')).not.toBeChecked();
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

test('Closing comparison cancels stale work and preserves the primary result', async ({ page }) => {
    await page.goto(QUERY_URL);
    await page.waitForSelector('.CodeMirror');
    const primaryQuery = 'SELECT ?s WHERE { ?s ?p ?o }';
    const compareQuery = 'ASK { ?s ?p ?o }';
    await setPrimaryQuery(page, primaryQuery);
    await page.locator('#exec').click();
    // The result streams into the page (no iframe since the redesign); marking its root shows below that closing the
    // comparison keeps this very result instead of rendering or executing it again.
    const result = page.locator('#query-results [data-query-stream-root]');
    await expect(page.locator('#query-results')).toHaveAttribute('aria-busy', 'false');
    await expect(result.locator('tbody')).toContainText('urn:a1');
    const primaryResultText = await result.innerText();
    await result.evaluate(element => element.setAttribute('data-test-primary-result', 'true'));

    await page.locator('#explain-trigger').click();
    await page.waitForFunction(() => {
        const explanation = document.getElementById('query-explanation');
        const trigger = document.getElementById('explain-trigger');
        return explanation && explanation.textContent.trim().length > 0
            && trigger && trigger.getAttribute('aria-busy') !== 'true';
    });
    await page.locator('#explain-level').evaluate(element => {
        element.value = 'Executed';
    });
    await expect(page.locator('#compare-toggle')).toBeVisible();
    await page.locator('#compare-toggle').click();
    await expect(page.locator('.CodeMirror')).toHaveCount(2);
    await expect.poll(() => page.locator('#query-compare-layout').evaluate(element =>
        element.getAnimations().some(animation => animation.playState === 'running'))).toBe(false);
    await expect(page.locator('#query-compare-close')).toHaveAccessibleName('Close comparison');
    const desktopCloseBounds = await page.locator('#query-compare-close').boundingBox();
    expect.soft(desktopCloseBounds.height).toBeGreaterThanOrEqual(36);
    const desktopEditorTops = await page.evaluate(() =>
        Array.from(document.querySelectorAll('.query-compare-pane .CodeMirror'))
            .map(editor => editor.getBoundingClientRect().top));
    expect.soft(desktopEditorTops).toHaveLength(2);
    expect.soft(Math.abs(desktopEditorTops[0] - desktopEditorTops[1])).toBeLessThanOrEqual(1);
    const desktopCompareGeometry = await page.evaluate(() => {
        const layout = document.getElementById('query-compare-layout').getBoundingClientRect();
        const panes = Array.from(document.querySelectorAll('.query-compare-pane'));
        const paneBounds = panes.map(pane => pane.getBoundingClientRect());
        const editorBounds = panes.map(pane => pane.querySelector('.CodeMirror').getBoundingClientRect());
        return {
            layoutRight: layout.right,
            paneWidths: paneBounds.map(bounds => bounds.width),
            paneRight: paneBounds[1].right,
            editorWidths: editorBounds.map(bounds => bounds.width)
        };
    });
    expect.soft(Math.abs(desktopCompareGeometry.paneWidths[0] - desktopCompareGeometry.paneWidths[1]))
        .toBeLessThanOrEqual(1);
    expect.soft(Math.abs(desktopCompareGeometry.editorWidths[0] - desktopCompareGeometry.editorWidths[1]))
        .toBeLessThanOrEqual(1);
    expect.soft(desktopCompareGeometry.paneRight).toBeCloseTo(desktopCompareGeometry.layoutRight, 0);
    await page.evaluate(() => window.scrollTo(0, 0));
    await page.screenshot({ path: '/private/tmp/rdf4j-compare-close-desktop.png' });

    await page.setViewportSize({ width: 320, height: 900 });
    await page.locator('#query-compare-close').scrollIntoViewIfNeeded();
    const mobileCloseBounds = await page.locator('#query-compare-close').evaluate(button => {
        const bounds = button.getBoundingClientRect();
        const pane = document.querySelector('.query-compare-pane--secondary').getBoundingClientRect();
        return {
            height: bounds.height,
            left: bounds.left,
            right: bounds.right,
            viewportWidth: window.innerWidth,
            paneLeft: pane.left,
            paneRight: pane.right
        };
    });
    expect.soft(mobileCloseBounds.height).toBeGreaterThanOrEqual(44);
    expect.soft(mobileCloseBounds.left).toBeGreaterThanOrEqual(mobileCloseBounds.paneLeft);
    expect.soft(mobileCloseBounds.right).toBeLessThanOrEqual(mobileCloseBounds.paneRight + 1);
    expect.soft(mobileCloseBounds.right).toBeLessThanOrEqual(mobileCloseBounds.viewportWidth);
    await page.locator('.query-compare-pane--secondary').screenshot({ path: '/private/tmp/rdf4j-compare-close-mobile-320.png' });
    await page.setViewportSize({ width: 1280, height: 900 });

    await page.evaluate(query => {
        document.querySelectorAll('.CodeMirror')[1].CodeMirror.setValue(query);
    }, compareQuery);

    let releaseCompareResponse;
    let resolveCompareStarted;
    let resolveCompareResponseFinished;
    const compareRequestStarted = new Promise(resolve => {
        resolveCompareStarted = resolve;
    });
    const responseGate = new Promise(resolve => {
        releaseCompareResponse = resolve;
    });
    const compareResponseFinished = new Promise(resolve => {
        resolveCompareResponseFinished = resolve;
    });
    const cancelledRequestIds = [];
    let compareRequestId;

    await page.route(QUERY_URL, async route => {
        const request = route.request();
        const parameters = new URLSearchParams(request.postData() || '');
        const action = parameters.get('action');

        if (request.method() === 'POST' && action === 'explain' && parameters.get('query') === compareQuery) {
            compareRequestId = parameters.get('explain-request-id');
            resolveCompareStarted();
            await responseGate;
            try {
                await route.fulfill({
                    status: 200,
                    contentType: 'application/json',
                    body: JSON.stringify({ content: 'stale comparison explanation' })
                });
            } catch (_) {
                // Closing the pane aborts this intentionally held browser request.
            } finally {
                resolveCompareResponseFinished();
            }
            return;
        }

        if (request.method() === 'POST' && action === 'cancel-explain') {
            cancelledRequestIds.push(parameters.get('explain-request-id'));
        }
        await route.continue();
    });

    try {
        await page.locator('#explain-compare-trigger').click();
        await compareRequestStarted;
        await page.locator('#query-compare-close').click();

        await expect(page.locator('#query-compare-layout')).not.toHaveClass(/query-compare-layout--active/);
        await expect(page.locator('#query-compare-close')).toBeHidden();
        await expect(page.locator('#compare-toggle')).toBeFocused();
        // The explanation and the result are tabs of one output card (app-shell plan M3.3): the result is behind
        // the Results tab while the explanation is shown.
        await page.locator('#query-output-tab-results').click();
        await expect(result).toBeVisible();
        await expect(result).toHaveAttribute('data-test-primary-result', 'true');
        await expect(result.locator('tbody')).toContainText('urn:a1');
        await expect.poll(() => result.innerText()).toBe(primaryResultText);
        await page.locator('#query-output-tab-explanation').click();
        expect(await page.evaluate(() => document.querySelector('.CodeMirror').CodeMirror.getValue()))
            .toBe(primaryQuery);
        await expect.poll(() => cancelledRequestIds).toContain(compareRequestId);

        releaseCompareResponse();
        await compareResponseFinished;
        await expect(page.locator('#query-explanation-compare')).toBeEmpty();

        await page.locator('#compare-toggle').click();
        await expect(page.locator('.CodeMirror')).toHaveCount(2);
        await expect.poll(() => page.evaluate(() =>
            document.querySelectorAll('.CodeMirror')[1].CodeMirror.getValue())).toBe(primaryQuery);
        await expect(page.locator('#query-explanation-compare')).toBeEmpty();

        await page.locator('#explain-compare-trigger').click();
        await page.waitForFunction(() => {
            const primary = document.getElementById('query-explanation');
            const comparison = document.getElementById('query-explanation-compare');
            return primary && comparison && primary.textContent.trim() && comparison.textContent.trim();
        });
        await page.locator('#query-diff-trigger').click();
        await expect(page.locator('#query-diff-modal')).toHaveClass(/query-diff-modal--open/);
        await page.locator('#query-compare-close').evaluate(button => button.click());
        await expect(page.locator('#query-diff-modal')).not.toHaveClass(/query-diff-modal--open/);
        await expect(page.locator('#compare-toggle')).toBeFocused();
    } finally {
        releaseCompareResponse();
        await page.unroute(QUERY_URL);
    }
});

test('Query editor and Explain match develop light theme and preserve dark colors on live switches', async ({ page }) => {
    await page.goto(QUERY_URL);
    await page.waitForSelector('.CodeMirror');
    await setPrimaryQuery(page, `SELECT ?subject WHERE {
  ?subject <urn:name> "Example" .
  FILTER(42 > 0)
}`);

    const editorColors = async () => page.evaluate(() => {
        const editor = document.querySelector('.query-page .CodeMirror');
        const computed = selector => {
            const token = editor.querySelector(selector);
            return token ? getComputedStyle(token).color : null;
        };
        const codeMirror = editor.CodeMirror;
        codeMirror.focus();
        codeMirror.setCursor({ line: 0, ch: 0 });
        const cursor = editor.querySelector('.CodeMirror-cursor');
        const cursorColor = cursor ? getComputedStyle(cursor).borderLeftColor : null;
        codeMirror.setSelection({ line: 0, ch: 0 }, { line: 0, ch: 6 });
        const focusedSelection = editor.querySelector('.CodeMirror-selected');
        const focusedSelectionColor = focusedSelection
            ? getComputedStyle(focusedSelection).backgroundColor : null;
        codeMirror.getInputField().blur();
        const unfocusedSelection = editor.querySelector('.CodeMirror-selected');
        return {
            background: getComputedStyle(editor).backgroundColor,
            foreground: getComputedStyle(editor).color,
            gutter: getComputedStyle(editor.querySelector('.CodeMirror-gutters')).backgroundColor,
            gutterBorder: getComputedStyle(editor.querySelector('.CodeMirror-gutters')).borderRightColor,
            lineNumber: getComputedStyle(editor.querySelector('.CodeMirror-linenumber')).color,
            cursor: cursorColor,
            focusedSelection: focusedSelectionColor,
            unfocusedSelection: unfocusedSelection
                ? getComputedStyle(unfocusedSelection).backgroundColor : null,
            keyword: computed('.cm-keyword'),
            atom: computed('.cm-atom'),
            inspectedTokens: Array.from(editor.querySelectorAll('.CodeMirror-code span'))
                .map(token => ({
                    text: token.textContent,
                    className: token.className,
                    color: getComputedStyle(token).color
                }))
                .filter(token => /subject|urn:name|Example/.test(token.text)),
            variables: Array.from(editor.querySelectorAll('.cm-variable, .cm-variable-2, .cm-variable-3'))
                .map(token => ({
                    className: token.className,
                    color: getComputedStyle(token).color
                })),
            string: computed('.cm-string'),
            number: computed('.cm-number')
        };
    });

    const explanationColors = async () => page.evaluate(() => {
        const explanation = document.getElementById('query-explanation');
        const computed = selector => {
            const token = explanation.querySelector(selector);
            return token ? getComputedStyle(token).color : null;
        };
        return {
            background: getComputedStyle(explanation).backgroundColor,
            foreground: getComputedStyle(explanation).color,
            nodeType: computed('.query-explanation-token--node-type'),
            variableLabel: computed('.query-explanation-token--variable-label'),
            variable: computed('.query-explanation-token--variable'),
            value: computed('.query-explanation-token--value'),
            valueIri: computed('.query-explanation-token--value-iri'),
            valueLiteral: computed('.query-explanation-token--value-literal'),
            inspectedTokens: Array.from(explanation.querySelectorAll('.query-explanation-token'))
                .map(token => ({
                    text: token.textContent,
                    className: token.className,
                    color: getComputedStyle(token).color
                }))
                .filter(token => /subject|urn:name|Example/.test(token.text)),
            metric: computed('.query-explanation-token--metric-value')
        };
    });

    const structuredExplanationColors = async () => page.evaluate(() => {
        const view = document.getElementById('query-explanation-json-view');
        const computed = selector => {
            const token = view.querySelector(selector);
            return token ? getComputedStyle(token).color : null;
        };
        return {
            background: getComputedStyle(view).backgroundColor,
            foreground: getComputedStyle(view.querySelector('.query-json-tree')).color,
            key: computed('.query-json-node__key'),
            string: computed('.query-json-node__value--string'),
            number: computed('.query-json-node__value--number')
        };
    });

    await page.evaluate(() => window.RDF4JWorkbenchTheme.setPreference('light'));
    await expect.poll(() => page.locator('html').getAttribute('data-theme')).toBe('light');
    await page.locator('#explain-trigger').click();
    await page.locator('#query-explanation .query-explanation-token--node-type').first().waitFor();
    const lightEditor = await editorColors();
    const lightTextExplanation = await explanationColors();

    await page.locator('#explain-format').selectOption('json');
    await expect(page.locator('#explain-format')).toHaveValue('json');
    const lightJsonExplainResponse = page.waitForResponse(response => {
        const request = response.request();
        const parameters = new URLSearchParams(request.postData() || '');
        return request.method() === 'POST' && request.url().endsWith('/query')
            && parameters.get('action') === 'explain' && parameters.get('explain-format') === 'json';
    });
    await page.locator('#rerun-explanation').click();
    expect((await lightJsonExplainResponse).ok()).toBe(true);
    await page.locator('#query-explanation-json-view .query-json-tree').waitFor();
    const lightJsonExplanation = await structuredExplanationColors();

    await page.evaluate(() => window.RDF4JWorkbenchTheme.setPreference('dark'));
    await expect.poll(() => page.locator('html').getAttribute('data-theme')).toBe('dark');
    const darkEditor = await editorColors();
    const darkJsonExplanation = await structuredExplanationColors();

    await page.locator('#explain-format').selectOption('text');
    await expect(page.locator('#explain-format')).toHaveValue('text');
    const darkTextExplainResponse = page.waitForResponse(response => {
        const request = response.request();
        const parameters = new URLSearchParams(request.postData() || '');
        // The Text view is rendered from the JSON explain response.
        return request.method() === 'POST' && request.url().endsWith('/query')
            && parameters.get('action') === 'explain' && parameters.get('explain-format') === 'json';
    });
    await page.locator('#rerun-explanation').click();
    await page.locator('#query-explanation .query-explanation-token--node-type').first().waitFor();
    expect((await darkTextExplainResponse).ok()).toBe(true);
    const darkTextExplanation = await explanationColors();
    await page.evaluate(() => window.RDF4JWorkbenchTheme.setPreference('light'));
    await expect.poll(() => page.locator('html').getAttribute('data-theme')).toBe('light');
    const switchedBackEditor = await editorColors();

    console.log('QUERY_THEME_COLORS', JSON.stringify({
        lightEditor,
        lightJsonExplanation,
        lightTextExplanation,
        darkEditor,
        darkTextExplanation,
        darkJsonExplanation,
        switchedBackEditor
    }));

    expect.soft(lightEditor.background).toBe('rgb(255, 255, 255)');
    expect.soft(lightEditor.foreground).toBe('rgb(0, 0, 0)');
    expect.soft(lightEditor.gutter).toBe('rgb(247, 247, 247)');
    expect.soft(lightEditor.gutterBorder).toBe('rgb(221, 221, 221)');
    expect.soft(lightEditor.lineNumber).toBe('rgb(153, 153, 153)');
    expect.soft(lightEditor.cursor).toBe('rgb(0, 0, 0)');
    expect.soft(lightEditor.focusedSelection).toBe('rgb(215, 212, 240)');
    expect.soft(lightEditor.unfocusedSelection).toBe('rgb(217, 217, 217)');
    expect.soft(lightEditor.keyword).toBe('rgb(119, 0, 136)');
    expect.soft(lightEditor.atom).toBe('rgb(34, 17, 153)');
    expect.soft(lightEditor.variables.length).toBeGreaterThan(0);
    for (const token of lightEditor.variables) {
        const classes = token.className.split(/\s+/);
        const developColor = classes.includes('cm-variable-2')
            ? 'rgb(0, 85, 170)' : classes.includes('cm-variable-3')
                ? 'rgb(0, 136, 85)' : 'rgb(0, 0, 0)';
        expect.soft(token.color).toBe(developColor);
    }
    const tokenColor = (tokens, text, tokenClass) => tokens.find(token => token.text === text
        && token.className.split(/\s+/).includes(tokenClass))?.color;
    const querySubjectColor = tokenColor(lightEditor.inspectedTokens, '?subject', 'cm-atom');
    const queryIriColor = tokenColor(lightEditor.inspectedTokens, '<urn:name>', 'cm-variable-3');
    const queryLiteralColor = tokenColor(lightEditor.inspectedTokens, '"Example"', 'cm-string');
    const explainSubjectColor = tokenColor(lightTextExplanation.inspectedTokens,
        'subject', 'query-explanation-token--variable');
    const explainIriColor = tokenColor(lightTextExplanation.inspectedTokens,
        'urn:name', 'query-explanation-token--value');
    const explainLiteralColor = tokenColor(lightTextExplanation.inspectedTokens,
        '"Example"', 'query-explanation-token--value');
    expect.soft(querySubjectColor).toBeDefined();
    expect.soft(queryIriColor).toBeDefined();
    expect.soft(queryLiteralColor).toBeDefined();
    expect.soft(explainSubjectColor).toBeDefined();
    expect.soft(explainIriColor).toBeDefined();
    expect.soft(explainLiteralColor).toBeDefined();
    expect.soft(explainSubjectColor).toBe(querySubjectColor);
    expect.soft(explainIriColor).toBe(queryIriColor);
    expect.soft(explainLiteralColor).toBe(queryLiteralColor);
    const subjectVariableColor = querySubjectColor;
    expect.soft(lightEditor.string).toBe('rgb(170, 17, 17)');
    expect.soft(lightEditor.number).toBe('rgb(17, 102, 68)');
    for (const explanation of [lightTextExplanation]) {
        expect.soft(explanation.background).toBe('rgb(255, 255, 255)');
        expect.soft(explanation.foreground).toBe('rgb(0, 0, 0)');
        expect.soft(explanation.nodeType).toBe('rgb(119, 0, 136)');
        expect.soft(explanation.variableLabel).toBe(subjectVariableColor);
        expect.soft(explanation.variable).toBe(subjectVariableColor);
        expect.soft(explanation.value).not.toBeNull();
        expect.soft(explanation.valueIri).toBe('rgb(0, 136, 85)');
        expect.soft(explanation.valueLiteral).toBe('rgb(170, 17, 17)');
        expect.soft(explanation.metric).not.toBeNull();
        expect.soft(explanation.metric).toBe('rgb(17, 102, 68)');
    }
    expect.soft(lightJsonExplanation.background).toBe('rgb(255, 255, 255)');
    expect.soft(lightJsonExplanation.foreground).toBe('rgb(0, 0, 0)');
    expect.soft(lightJsonExplanation.key).toBe('rgb(85, 85, 85)');
    expect.soft(lightJsonExplanation.string).not.toBeNull();
    expect.soft(lightJsonExplanation.string).toBe('rgb(170, 17, 17)');
    expect.soft(lightJsonExplanation.number).not.toBeNull();
    expect.soft(lightJsonExplanation.number).toBe('rgb(17, 102, 68)');
    expect.soft(darkEditor.background).toBe('rgb(17, 26, 29)');
    expect.soft(darkEditor.foreground).toBe('rgb(206, 218, 223)');
    expect.soft(darkEditor.gutter).toBe('rgb(24, 36, 40)');
    expect.soft(darkEditor.cursor).toBe('rgb(0, 0, 0)');
    expect.soft(darkEditor.keyword).toBe('rgb(115, 196, 226)');
    for (const token of darkEditor.variables) {
        expect.soft(token.color).toBe('rgb(193, 179, 255)');
    }
    expect.soft(darkEditor.string).toBe('rgb(148, 214, 162)');
    expect.soft(darkEditor.number).toBe('rgb(169, 184, 255)');
    expect.soft(darkTextExplanation.background).toBe('rgb(17, 26, 29)');
    expect.soft(darkTextExplanation.foreground).toBe('rgb(232, 240, 243)');
    expect.soft(darkTextExplanation.nodeType).toBe('rgb(115, 196, 226)');
    expect.soft(darkTextExplanation.variableLabel).toBe('rgb(115, 196, 226)');
    expect.soft(darkTextExplanation.variable).toBe('rgb(148, 214, 239)');
    expect.soft(darkTextExplanation.valueIri).toBe('rgb(206, 218, 223)');
    expect.soft(darkTextExplanation.valueLiteral).toBe('rgb(206, 218, 223)');
    expect.soft(darkJsonExplanation.background).toBe('rgb(17, 26, 29)');
    expect.soft(darkJsonExplanation.foreground).toBe('rgb(232, 240, 243)');
    expect.soft(darkJsonExplanation.key).toBe('rgb(206, 218, 223)');
    expect.soft(darkJsonExplanation.string).toBe('rgb(232, 240, 243)');
    expect.soft(switchedBackEditor).toEqual(lightEditor);
});
