const test = require('node:test');
const assert = require('node:assert/strict');

const { createQueryBrowserHarness } = require('./query-browser-harness.js');

test('query page load initializes editors, fetches saved query text, and hydrates explanation state', () => {
    const harness = createQueryBrowserHarness({
        href: 'http://localhost:8080/rdf4j-workbench/repositories/test/query?query=saved-query&ref=id',
        query: '',
        getJSONResponses: [{ queryText: ' ASK {} ' }],
        initialExplanation: 'Initial plan',
        initialExplanationFormat: 'text',
        noAuthenticatedUser: true
    });

    harness.runPageLoad();

    assert.equal(harness.getJSONRequests.length, 1);
    assert.equal(harness.context.workbench.query.getQueryValue(), 'ASK {}');
    assert.ok(harness.yasqeState.instances.query);
    assert.equal(harness.getProperty('save-private', 'disabled'), true);
    assert.equal(harness.getProperty('save-private', 'checked'), false);
    assert.equal(harness.getText('query-explanation'), 'Initial plan');
    assert.equal(harness.getProperty('download-explanation', 'disabled'), false);
});

test('query utilities cover namespace reset, name validation, query language switch, save, and submit branches', () => {
    const harness = createQueryBrowserHarness({
        confirmResponses: [false, true]
    });

    harness.runPageLoad();

    harness.setValue('query-name', 'bad!');
    harness.context.workbench.query.handleNameChange();
    harness.advanceTimers(0);
    assert.equal(harness.getProperty('save', 'disabled'), true);

    harness.setValue('query-name', 'valid_name');
    harness.context.workbench.query.handleNameChange();
    harness.advanceTimers(0);
    assert.equal(harness.getProperty('save', 'disabled'), false);

    harness.context.workbench.query.setQueryValue('SELECT * WHERE {?s ?p ?o}');
    harness.context.workbench.query.resetNamespaces();
    assert.equal(harness.context.workbench.query.getQueryValue(), 'SELECT * WHERE {?s ?p ?o}');

    harness.context.workbench.query.resetNamespaces();
    assert.match(harness.context.workbench.query.getQueryValue(), /PREFIX ex:/);

    const primaryEditor = harness.yasqeState.instances.query;
    harness.setValue('queryLn', 'SERQL');
    harness.context.workbench.query.onQlChange();
    assert.equal(primaryEditor.closed, true);

    harness.setValue('queryLn', 'SPARQL');
    harness.context.workbench.query.onQlChange();
    assert.ok(harness.yasqeState.instances.query.refreshCount >= 1);

    harness.setValue('action', 'save');
    assert.equal(harness.context.workbench.query.doSubmit(), false);
    harness.ajaxRequests[harness.ajaxRequests.length - 1].resolve({ accessible: true, written: true });
    assert.equal(harness.getText('save-feedback'), 'Query saved.');
    assert.equal(harness.hasClass('save-feedback', 'success'), true);

    harness.setValue('action', 'exec');
    harness.context.workbench.query.setQueryValue('SELECT * WHERE {?s ?p ?o}');
    assert.equal(harness.context.workbench.query.doSubmit(), false);
    assert.match(harness.document.location.href, /action=exec/);
    assert.equal(harness.getProperty('include-query-text', 'value'), 'false');

    harness.context.workbench.query.setQueryValue('x'.repeat(3000));
    assert.equal(harness.context.workbench.query.doSubmit(), true);
    assert.equal(harness.getProperty('include-query-text', 'value'), 'true');
    assert.match(harness.alerts[harness.alerts.length - 1], /Due to its length/);
});

test('query explain flow covers success, error, download, and legacy change notifications', () => {
    const harness = createQueryBrowserHarness({
        serverRequestIds: ['request-1', 'request-2']
    });

    harness.runPageLoad();
    harness.context.workbench.query.runExplain('json', 'explain-trigger');
    const explainRequest = harness.pendingExplainRequests[0];
    explainRequest.resolve({
        content: '{"plans":[{"selfTimeActual":2,"totalTimeActual":4}]}',
        format: 'json',
        error: ''
    });

    assert.equal(harness.getProperty('download-explanation', 'disabled'), false);
    assert.equal(harness.document.getElementById('query-explanation-json-view').children.length > 0, true);

    harness.context.workbench.query.downloadExplanation();

    harness.context.workbench.query.notifyQueryPageInputChange('PRIMARY_QUERY_CHANGED');
    assert.match(harness.document.cookie, /ref=;/);

    harness.context.workbench.query.runExplain('Optimized', 'rerun-explanation');
    const failingRequest = harness.pendingExplainRequests[1];
    failingRequest.reject('timeout');
    assert.equal(
        harness.getText('query-explanation-status'),
        'Timed out waiting for explanation response.'
    );
});

test('query compare flow covers auto-explain, compare refresh, diff modal, and compare cancellation', () => {
    const harness = createQueryBrowserHarness({
        initialExplanation: 'Primary explanation',
        initialExplanationFormat: 'text',
        query: 'SELECT * WHERE {?s ?p ?o}',
        compareQuery: ''
    });

    harness.runPageLoad();

    harness.context.workbench.query.toggleCompareMode();
    assert.equal(harness.pendingExplainRequests.length, 1);
    harness.pendingExplainRequests[0].resolve({
        content: 'Compare explanation',
        format: 'text',
        error: ''
    });

    assert.ok(harness.yasqeState.instances['query-compare']);
    assert.equal(harness.context.workbench.query.getQueryValue().length > 0, true);

    harness.setValue('query-compare', 'SELECT * WHERE {?o ?p ?s}');
    harness.context.workbench.query.runCompareExplain('explain-compare-trigger');
    assert.equal(harness.pendingExplainRequests.length, 3);
    harness.pendingExplainRequests[1].resolve({
        content: 'Primary refreshed',
        format: 'text',
        error: ''
    });
    harness.pendingExplainRequests[2].resolve({
        content: 'Compare refreshed',
        format: 'text',
        error: ''
    });

    harness.context.workbench.query.openDiffModal();
    assert.equal(harness.hasClass('query-diff-modal', 'query-diff-modal--open'), true);
    assert.equal(harness.document.getElementById('query-diff-query').children.length > 0, true);

    harness.document.trigger('keydown', { key: 'Escape' });
    assert.equal(harness.hasClass('query-diff-modal', 'query-diff-modal--open'), false);

    harness.context.workbench.query.runCompareExplain('explain-compare-trigger');
    harness.context.workbench.query.cancelCompareExplain();
    assert.equal(harness.requestsByAction('cancel-explain').length >= 2, true);
});

test('saved query load keeps initial explanation current after async fetch', () => {
    const harness = createQueryBrowserHarness({
        href: 'http://localhost:8080/rdf4j-workbench/repositories/test/query?query=saved-query&ref=id',
        query: '',
        getJSONResponses: [{ queryText: ' ASK {} ' }],
        initialExplanation: 'Initial plan',
        initialExplanationFormat: 'text',
        deferGetJSON: true
    });

    harness.runPageLoad();
    harness.resolveNextGetJSON();

    harness.context.workbench.query.toggleCompareMode();

    assert.equal(harness.context.workbench.query.getQueryValue(), 'ASK {}');
    assert.equal(harness.getProperty('query-compare', 'value'), 'ASK {}');
    assert.equal(harness.pendingExplainRequests.length, 1);
});

test('saved query load backfills compare seed when compare opens before fetch returns', () => {
    const harness = createQueryBrowserHarness({
        href: 'http://localhost:8080/rdf4j-workbench/repositories/test/query?query=saved-query&ref=id',
        query: '',
        getJSONResponses: [{ queryText: ' ASK {} ' }],
        initialExplanation: 'Initial plan',
        initialExplanationFormat: 'text',
        deferGetJSON: true
    });

    harness.runPageLoad();
    harness.context.workbench.query.toggleCompareMode();

    harness.resolveNextGetJSON();

    assert.equal(harness.context.workbench.query.getQueryValue(), 'ASK {}');
    assert.equal(harness.getProperty('query-compare', 'value'), 'ASK {}');
});
