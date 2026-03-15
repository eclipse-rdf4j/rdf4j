const test = require('node:test');
const assert = require('node:assert/strict');

const { createQueryBrowserHarness } = require('./query-browser-harness.js');

function createSignature(overrides = {}) {
    return Object.assign({
        requestId: 1,
        serverRequestId: 'request-1',
        pane: 'primary',
        source: 'primary-explain',
        queryHash: 'SELECT *',
        level: 'Optimized',
        format: 'text',
        groupId: 1
    }, overrides);
}

function createExplanation(overrides = {}) {
    return Object.assign({
        queryHash: 'SELECT *',
        level: 'Optimized',
        requestedFormat: 'text',
        responseFormat: 'text',
        view: 'text',
        rawContent: 'plan'
    }, overrides);
}

test('query testing helpers cover reducer and state utility branches', () => {
    const harness = createQueryBrowserHarness();
    const testing = harness.context.workbench.query.testing;

    testing.resetInternalState();
    assert.equal(testing.getNormalizedExplainLevel('Timed'), 'Timed');
    assert.equal(testing.getNormalizedExplainLevel('bogus'), 'Optimized');
    assert.equal(testing.getNormalizedExplainFormat('JSON'), 'json');
    assert.equal(testing.getNormalizedExplainFormat('bogus'), 'text');
    assert.equal(testing.buildQueryHash('abc'), 'abc');

    harness.window.crypto = {
        randomUUID() {
            return 'uuid-1';
        }
    };
    assert.equal(testing.generateExplainServerRequestId(), 'uuid-1');

    delete harness.window.crypto.randomUUID;
    harness.window.crypto.getRandomValues = (buffer) => {
        buffer[0] = 1;
        buffer[1] = 2;
        buffer[2] = 3;
        buffer[3] = 4;
    };
    assert.match(testing.createFallbackExplainServerRequestId(), /^[0-9a-f-]+$/);

    const inputs = {
        primaryQueryHash: 'SELECT *',
        compareQueryHash: 'DESCRIBE *',
        explainLevel: 'Optimized',
        explainFormat: 'text'
    };
    const explanation = createExplanation();
    assert.deepEqual(JSON.parse(JSON.stringify(testing.cloneStableExplanation(explanation))), explanation);
    assert.match(testing.getStableExplanationKey(explanation), /SELECT \*/);
    assert.match(testing.getStableExplanationContentKey(explanation), /plan/);
    assert.deepEqual(JSON.parse(JSON.stringify(testing.getPaneSnapshot({ kind: 'ready', explanation }))), explanation);
    assert.deepEqual(Array.from(testing.getStaleReasons(explanation, 'primary', inputs)), []);
    assert.deepEqual(Array.from(testing.getStaleReasons(explanation, 'compare', inputs)), ['query']);
    assert.equal(testing.getPaneQueryHashFromInputs('compare', inputs), 'DESCRIBE *');
    assert.equal(testing.isPaneReadyCurrent(testing.createReadyPaneState(explanation, 'primary', inputs)), true);
    assert.equal(testing.restorePaneStateFromPrevious({ kind: 'empty' }, 'compare', inputs, { mode: 'single' }).kind, 'inactive');

    const signature = createSignature();
    const loadingState = testing.reducePaneState(
        { kind: 'empty' },
        'primary',
        { type: 'REQUEST_EXPLAIN', signature },
        inputs,
        { mode: 'single' }
    );
    assert.equal(loadingState.kind, 'loading');
    const spinnerState = testing.reducePaneState(
        loadingState,
        'primary',
        { type: 'SPINNER_DELAY_ELAPSED', signature },
        inputs,
        { mode: 'single' }
    );
    assert.equal(spinnerState.phase, 'spinner');
    const readyState = testing.reducePaneState(
        spinnerState,
        'primary',
        { type: 'EXPLAIN_SUCCESS', signature, explanation },
        inputs,
        { mode: 'single' }
    );
    assert.equal(readyState.kind, 'ready');
    const dotFailState = testing.reducePaneState(
        { kind: 'ready', freshness: 'current', staleReasons: [], explanation: createExplanation({ view: 'dotRendering' }) },
        'primary',
        { type: 'DOT_RENDER_FAIL', pane: 'primary', explanationKey: testing.getStableExplanationKey(createExplanation({ view: 'dotRendering' })) },
        inputs,
        { mode: 'single' }
    );
    assert.equal(dotFailState.explanation.view, 'dotRenderError');
    const errorState = testing.reducePaneState(
        loadingState,
        'primary',
        { type: 'EXPLAIN_ERROR', signature, message: 'boom' },
        inputs,
        { mode: 'single' }
    );
    assert.equal(errorState.kind, 'error');
    assert.equal(testing.getPaneDisplayExplanation(errorState), null);
    assert.equal(testing.getPaneStatusMessage(errorState), 'boom');
    assert.equal(testing.getPaneStatusClassName(errorState), 'query-explanation-status--error');
    assert.equal(testing.getPaneOverlayMessage({ kind: 'loading', mode: 'refresh', phase: 'spinner', request: signature }), 'Refreshing explanation...');
    assert.equal(testing.signaturesMatch(signature, Object.assign({}, signature)), true);
    assert.equal(testing.getEventSignatureForPane({ type: 'CANCEL_EXPLAIN', pane: 'primary', signature }, 'primary').serverRequestId, 'request-1');

    const diffState = testing.reduceDiffModalState(
        { kind: 'closed' },
        { type: 'OPEN_DIFF' },
        { kind: 'ready', freshness: 'current', staleReasons: [], explanation },
        { kind: 'ready', freshness: 'current', staleReasons: [], explanation },
        { mode: 'compare', sidebar: 'closed' },
        { primaryQueryHash: 'a', compareQueryHash: 'b', explainLevel: 'Optimized', explainFormat: 'text' }
    );
    assert.equal(diffState.kind, 'open');
    assert.equal(diffState.explanation, 'ready');
});

test('query testing helpers cover serialization, explanation parsing, diff rendering, and compare actions', () => {
    const harness = createQueryBrowserHarness();
    const testing = harness.context.workbench.query.testing;
    const form = harness.document.querySelectorAll('form[action="query"]')[0];

    form.formControls = [];
    assert.match(testing.serializeExplainFormData('ASK {}', 'Optimized', 'json', 'server-1'), /action=explain/);
    assert.equal(testing.serializeCancelExplainFormData('server-1'), 'action=cancel-explain&explain-request-id=server-1');

    assert.equal(testing.getExplanationDownloadMimeType('dot'), 'text/vnd.graphviz');
    assert.equal(testing.getExplanationDownloadExtension('json'), 'json');
    assert.equal(testing.getExplainErrorMessage({ responseJSON: { error: 'json error' } }, '', ''), 'json error');
    assert.equal(testing.getExplainErrorMessage({ responseText: '{"error":"text json"}' }, '', ''), 'text json');
    assert.equal(testing.getExplainErrorMessage({ responseText: 'plain text' }, '', ''), 'plain text');
    assert.equal(testing.getExplainErrorMessage({}, 'timeout', ''), 'Timed out waiting for explanation response.');
    assert.equal(testing.getExplainErrorMessage({}, '', 'Broken'), 'Explain request failed: Broken');
    assert.equal(testing.getExplainErrorMessage({}, '', ''), 'Explain request failed.');

    assert.equal(testing.createStableExplanationFromResponse(createSignature(), { content: '{bad', format: 'json', error: '' }, 'json').view, 'jsonRawFallback');
    assert.equal(testing.createStableExplanationFromResponse(createSignature(), { content: 'digraph{}', format: 'dot', error: '' }, 'dot').view, 'dotRendering');

    assert.deepEqual(Array.from(testing.splitDiffLines('a\r\nb')), ['a', 'b']);
    assert.deepEqual(
        JSON.parse(JSON.stringify(testing.buildDiffRows('a\nb', 'a\nc'))),
        [
            { marker: ' ', text: 'a', type: 'context' },
            {
                marker: '-',
                text: 'b',
                type: 'removed',
                segments: [{ text: 'b', changed: true }]
            },
            {
                marker: '+',
                text: 'c',
                type: 'added',
                segments: [{ text: 'c', changed: true }]
            }
        ]
    );

    testing.renderDiffView('#query-diff-query', '', '', 'placeholder');
    assert.equal(harness.getText('query-diff-query'), 'placeholder');
    testing.renderDiffView('#query-diff-query', 'a\nb', 'a\nc');
    assert.equal(harness.document.getElementById('query-diff-query').children.length > 0, true);

    assert.equal(testing.isJsonExpandable({}), true);
    assert.equal(testing.isJsonExpandable('x'), false);
    assert.equal(testing.getJsonSummary([1, 2]), '[ 2 ]');
    assert.equal(testing.getJsonSummary({ a: 1, b: 2 }), '{ 2 }');
    assert.equal(testing.formatJsonKey('name'), '"name"');
    assert.equal(testing.formatJsonArrayEntryKey(2, { type: 'Plan' }), '[2] Plan');
    assert.equal(testing.parseNumericJsonValue('4.5'), 4.5);
    assert.equal(testing.parseNumericJsonValue('abc'), null);
    assert.deepEqual(
        JSON.parse(JSON.stringify(testing.computePlanEntryPercentages([
            { totalTimeActual: 4 },
            { totalTimeActual: 6 },
            {}
        ]))),
        [40, 60, null]
    );
    assert.equal(testing.formatPercentage(12.345), '12.3%');
    assert.equal(testing.createJsonScalarElement('value').textContent, '"value"');
    assert.equal(testing.createJsonTreeNode({ type: 'Plan', totalTimeActual: 5 }, 'plan', 0, 50).children.length > 0, true);
    assert.equal(testing.renderJsonExplanationTree({ plans: [{ totalTimeActual: 10 }] }).children.length > 0, true);
    testing.renderJsonView('primary', '{"plans":[{"totalTimeActual":10}]}', 'json');
    assert.equal(harness.document.getElementById('query-explanation-json-view').children.length > 0, true);

    testing.setInternalState({
        compareModeEnabled: true,
        queryPageState: {
            layout: { mode: 'compare', sidebar: 'closed' },
            primaryPane: { kind: 'ready', freshness: 'current', staleReasons: [], explanation: createExplanation() },
            comparePane: { kind: 'empty' },
            diffModal: { kind: 'closed' },
            inputs: { primaryQueryHash: 'a', compareQueryHash: 'b', explainLevel: 'Optimized', explainFormat: 'text' },
            lifecycle: 'ready',
            compareQuerySeeded: false
        }
    });
    testing.updateCompareActionState();
    assert.equal(testing.shouldAutoExplainComparePaneOnOpen(), true);
    harness.setValue('query', 'SELECT * WHERE {?s ?p ?o}');
    harness.setValue('query-compare', 'SELECT * WHERE {?o ?p ?s}');
    testing.renderQueryPageState();
    assert.equal(harness.getProperty('query-diff-trigger', 'disabled'), false);
});

test('query diff rendering delegates line diffing to the shared diff library', () => {
    const diffCalls = [];
    const harness = createQueryBrowserHarness({
        globals: {
            Diff: {
                diffLines(leftText, rightText, options) {
                    diffCalls.push({ leftText, rightText, options });
                    return [
                        { value: 'a\n' },
                        { removed: true, value: 'b\n' },
                        { added: true, value: 'c\n' }
                    ];
                }
            }
        }
    });
    const testing = harness.context.workbench.query.testing;

    assert.deepEqual(
        JSON.parse(JSON.stringify(testing.buildDiffRows('a\nb', 'a\nc'))),
        [
            { marker: ' ', text: 'a', type: 'context' },
            {
                marker: '-',
                text: 'b',
                type: 'removed',
                segments: [{ text: 'b', changed: false }]
            },
            {
                marker: '+',
                text: 'c',
                type: 'added',
                segments: [{ text: 'c', changed: false }]
            }
        ]
    );
    assert.equal(diffCalls.length, 1);
    assert.equal(diffCalls[0].leftText, 'a\nb');
    assert.equal(diffCalls[0].rightText, 'a\nc');
});

test('query diff rows retain intraline word changes for replaced lines', () => {
    const harness = createQueryBrowserHarness();
    const testing = harness.context.workbench.query.testing;

    assert.deepEqual(
        JSON.parse(JSON.stringify(testing.buildDiffRows('cat sat', 'cat slept'))),
        [
            {
                marker: '-',
                text: 'cat sat',
                type: 'removed',
                segments: [
                    { text: 'cat ', changed: false },
                    { text: 'sat', changed: true }
                ]
            },
            {
                marker: '+',
                text: 'cat slept',
                type: 'added',
                segments: [
                    { text: 'cat ', changed: false },
                    { text: 'slept', changed: true }
                ]
            }
        ]
    );
});

test('query diff view renders nested spans for changed words', () => {
    const harness = createQueryBrowserHarness();
    const testing = harness.context.workbench.query.testing;

    testing.renderDiffView('#query-diff-query', 'cat sat', 'cat slept');

    const diffRows = harness.document.getElementById('query-diff-query').children;
    assert.equal(diffRows.length, 2);
    assert.equal(diffRows[0].children[1].children[1].textContent, 'sat');
    assert.equal(diffRows[0].children[1].children[1].classList.contains('query-diff-row__segment--changed'), true);
    assert.equal(diffRows[1].children[1].children[1].textContent, 'slept');
    assert.equal(diffRows[1].children[1].children[1].classList.contains('query-diff-row__segment--changed'), true);
});

test('query testing helpers cover save error and overwrite branches', () => {
    const harness = createQueryBrowserHarness({
        confirmResponses: [true, false]
    });
    const testing = harness.context.workbench.query.testing;

    testing.ajaxSave(false);
    harness.ajaxRequests[harness.ajaxRequests.length - 1].reject('timeout', 'Timeout');
    assert.equal(harness.getText('save-feedback'), 'Timed out waiting for response. Uncertain if save occured.');

    testing.ajaxSave(false);
    harness.ajaxRequests[harness.ajaxRequests.length - 1].reject('error', 'Forbidden');
    assert.match(harness.getText('save-feedback'), /Save Request Failed/);

    testing.ajaxSave(false);
    harness.ajaxRequests[harness.ajaxRequests.length - 1].resolve({ accessible: false });
    assert.equal(harness.getText('save-feedback'), 'Repository was not accessible (check your permissions).');

    testing.ajaxSave(false);
    harness.ajaxRequests[harness.ajaxRequests.length - 1].resolve({ accessible: true, written: false, existed: true });
    assert.equal(harness.ajaxRequests.length >= 4, true);

    testing.ajaxSave(false);
    harness.ajaxRequests[harness.ajaxRequests.length - 1].resolve({ accessible: true, written: false, existed: true });
    assert.equal(harness.getText('save-feedback'), 'Cancelled overwriting existing query.');
});
