const test = require('node:test');
const assert = require('node:assert/strict');

const { createQueryBrowserHarness } = require('./query-browser-harness.js');
const { createScriptHarness } = require('./script-harness.js');
const { FakeWindow } = require('./browser-fakes.js');

function submitQuery(options = {}) {
    const requestId = (options.serverRequestIds || ['query-1'])[0];
    const harness = createQueryBrowserHarness(Object.assign({
        serverRequestIds: ['query-1'],
        query: 'SELECT * WHERE {?s ?p ?o}'
    }, options));
    harness.runPageLoad();
    const previousFrame = harness.getResultFrame();
    harness.setValue('action', 'exec');
    const submittedByPost = harness.context.workbench.query.doSubmit();
    assert.equal(harness.getProperty('query-request-id', 'value'), requestId);
    assert.equal(harness.getProperty('query-cancel', 'disabled'), false);
    return { harness, requestId, previousFrame, submittedByPost };
}

test('short query GET targets a fresh embedded result frame without opening a popup', () => {
    const { harness, previousFrame } = submitQuery({ serverRequestIds: ['query-1', 'query-2'] });
    const parentHref = harness.document.location.href;
    const resultFrame = harness.getResultFrame();
    const frameUrl = new URL(resultFrame.src, harness.document.location.href);

    assert.equal(harness.openedWindows.length, 0);
    assert.equal(harness.document.location.href, parentHref);
    assert.notEqual(resultFrame, previousFrame);
    assert.equal(resultFrame.hidden, false);
    assert.equal(harness.getProperty('query-results-loading', 'hidden'), false);
    assert.equal(frameUrl.searchParams.get('action'), 'exec');
    assert.equal(frameUrl.searchParams.get('query-request-id'), 'query-1');
    assert.equal(frameUrl.searchParams.get('embedded'), 'true');
});

test('long query POST uses a temporary form targeted at the embedded result frame', () => {
    const { harness } = submitQuery({
        query: 'x'.repeat(3000),
        longQuery: true
    });
    const editorForm = harness.document.querySelectorAll('form[action="query"]')[0];
    const submittedForm = harness.document.lastSubmittedForm;
    const submittedParams = new URLSearchParams(submittedForm.serializeArray()
        .map((entry) => `${entry.name}=${encodeURIComponent(entry.value)}`).join('&'));

    assert.equal(harness.openedWindows.length, 0);
    assert.equal(harness.getProperty('include-query-text', 'value'), 'true');
    assert.equal(harness.document.location.pathname, '/rdf4j-workbench/repositories/test/query');
    assert.equal(editorForm.getAttribute('target'), undefined);
    assert.notEqual(submittedForm, editorForm);
    assert.equal(submittedForm.getAttribute('target'), 'query-results-frame');
    assert.equal(submittedForm.submitCount, 1);
    assert.equal(submittedParams.get('embedded'), 'true');
    assert.equal(submittedParams.get('query-request-id'), 'query-1');
});

test('repeated execution cancels the prior request and tracks a fresh request id', () => {
    const { harness } = submitQuery({
        serverRequestIds: ['query-1', 'query-2']
    });
    const firstFrame = harness.getResultFrame();

    harness.context.workbench.query.doSubmit();
    const secondFrame = harness.getResultFrame();

    const cancelRequests = harness.requestsByAction('cancel-query');
    assert.equal(cancelRequests.length, 1);
    assert.equal(cancelRequests[0].params.get('query-request-id'), 'query-1');
    assert.equal(harness.getProperty('query-request-id', 'value'), 'query-2');
    assert.equal(harness.getProperty('query-cancel', 'disabled'), false);
    assert.notEqual(secondFrame, firstFrame);
    assert.equal(firstFrame.contentWindow.stopCount, 1);
    assert.equal(harness.openedWindows.length, 0);
});

test('result frame ignores blank, stale, and wrong-source messages until current completion', () => {
    const { harness } = submitQuery({ serverRequestIds: ['query-1', 'query-2'] });
    const firstFrame = harness.getResultFrame();

    harness.context.workbench.query.doSubmit();
    const currentFrame = harness.getResultFrame();
    assert.equal(harness.getProperty('query-request-id', 'value'), 'query-2');

    harness.window.trigger('message', {
        origin: harness.window.location.origin,
        source: firstFrame.contentWindow,
        data: { type: 'rdf4j-query-result', queryRequestId: 'query-1', status: 'completed' }
    });
    harness.window.trigger('message', {
        origin: harness.window.location.origin,
        source: currentFrame.contentWindow,
        data: { type: 'rdf4j-query-result', queryRequestId: ' ', status: 'completed' }
    });
    harness.window.trigger('message', {
        origin: harness.window.location.origin,
        source: new FakeWindow('', 'about:blank'),
        data: { type: 'rdf4j-query-result', queryRequestId: 'query-2', status: 'completed' }
    });

    assert.equal(harness.getProperty('query-request-id', 'value'), 'query-2');
    harness.window.trigger('message', {
        origin: harness.window.location.origin,
        source: currentFrame.contentWindow,
        data: { type: 'rdf4j-query-result', queryRequestId: 'query-2', status: 'completed' }
    });
    assert.equal(harness.getProperty('query-request-id', 'value'), '');
    assert.equal(harness.getProperty('query-cancel', 'disabled'), true);
});

test('terminal embedded documents with blank request ids complete GET and POST renders', () => {
    for (const { query, requestId } of [
        { query: 'SELECT * WHERE {?s ?p ?o}', requestId: 'get-1' },
        { query: 'x'.repeat(3000), requestId: 'post-1' }
    ]) {
        const { harness } = submitQuery({ query, serverRequestIds: [requestId] });
        const frame = harness.getResultFrame();
        frame.contentWindow.location.href = 'http://localhost:8080/rdf4j-workbench/repositories/test/query';
        frame.contentDocument = {
            getElementById(id) {
                if (id !== 'rdf4j-query-result') {
                    return null;
                }
                return {
                    getAttribute(name) {
                        return name === 'data-query-request-id' ? ' ' : 'completed';
                    }
                };
            }
        };
        frame.trigger('load');

        assert.equal(harness.getProperty('query-request-id', 'value'), '');
        assert.equal(harness.getProperty('query-cancel', 'disabled'), true);
        assert.equal(harness.getProperty('query-results-loading', 'hidden'), true);
    }
});

test('nonempty stale embedded document ids remain ignored', () => {
    const { harness } = submitQuery({ serverRequestIds: ['current-1'] });
    const frame = harness.getResultFrame();
    frame.contentWindow.location.href = 'http://localhost:8080/rdf4j-workbench/repositories/test/query';
    frame.contentDocument = {
        getElementById() {
            return {
                getAttribute(name) {
                    return name === 'data-query-request-id' ? 'stale-1' : 'completed';
                }
            };
        }
    };
    frame.trigger('load');

    assert.equal(harness.getProperty('query-request-id', 'value'), 'current-1');
    assert.equal(harness.getProperty('query-cancel', 'disabled'), false);
    assert.equal(harness.getProperty('query-results-loading', 'hidden'), false);
});

test('blank result-frame load is ignored and failed load clears pending state', () => {
    const { harness } = submitQuery({ serverRequestIds: ['query-1'] });
    const resultFrame = harness.getResultFrame();

    resultFrame.src = 'about:blank';
    resultFrame.trigger('load');
    assert.equal(harness.getProperty('query-results-loading', 'hidden'), false);
    resultFrame.trigger('error');

    assert.equal(harness.getProperty('query-results-loading', 'hidden'), true);
    assert.equal(harness.getText('query-results-status'), 'Unable to load query results.');
    assert.equal(harness.getProperty('query-request-id', 'value'), '');
    assert.equal(harness.getProperty('query-cancel', 'disabled'), true);
});

test('intermediate XML load stays pending until the deferred stylesheet document arrives', () => {
    const { harness } = submitQuery({ serverRequestIds: ['query-1'] });
    const resultFrame = harness.getResultFrame();

    resultFrame.src = 'http://localhost:8080/rdf4j-workbench/repositories/test/query?action=exec';
    resultFrame.contentWindow.location.href = resultFrame.src;
    resultFrame.contentDocument = null;
    resultFrame.trigger('load');
    harness.advanceTimers(300);

    assert.equal(harness.getProperty('query-request-id', 'value'), 'query-1');
    assert.equal(harness.getProperty('query-results-loading', 'hidden'), false);
    assert.equal(harness.getText('query-results-status'), '');

    resultFrame.contentDocument = {
        getElementById(id) {
            if (id !== 'rdf4j-query-result') {
                return null;
            }
            return {
                getAttribute(name) {
                    return name === 'data-query-request-id' ? 'query-1' : 'completed';
                }
            };
        }
    };
    resultFrame.trigger('load');

    assert.equal(harness.getProperty('query-request-id', 'value'), '');
    assert.equal(harness.getProperty('query-results-loading', 'hidden'), true);
});

test('empty query clears the previous result and reports an actionable status', () => {
    const harness = createQueryBrowserHarness({ query: '' });
    harness.runPageLoad();
    harness.context.workbench.query.setQueryValue('');
    harness.setValue('action', 'exec');

    assert.equal(harness.context.workbench.query.doSubmit(), false);
    assert.equal(harness.getResultFrame().hidden, true);
    assert.equal(harness.getProperty('query-results-loading', 'hidden'), true);
    assert.equal(harness.getText('query-results-status'), 'Enter a query to see results.');
    assert.equal(harness.requestsByAction('cancel-query').length, 0);
});

test('empty rerun cancels the preceding result before clearing the frame', () => {
    const { harness } = submitQuery({ serverRequestIds: ['query-1'] });
    harness.context.workbench.query.setQueryValue('');
    harness.setValue('action', 'exec');

    assert.equal(harness.context.workbench.query.doSubmit(), false);
    assert.equal(harness.requestsByAction('cancel-query').length, 1);
    assert.equal(harness.requestsByAction('cancel-query')[0].params.get('query-request-id'), 'query-1');
    assert.equal(harness.getResultFrame().hidden, true);
    assert.equal(harness.getProperty('query-request-id', 'value'), '');
    assert.equal(harness.getText('query-results-status'), 'Enter a query to see results.');
});

test('cancellation stops only the result frame before posting backend cancellation', () => {
    const events = [];
    const observedRequests = [];
    let stopInProgress = false;
    let parentStopCalls = 0;
    const harness = createQueryBrowserHarness({
        serverRequestIds: ['query-1'],
        onAjaxRequest(request) {
            observedRequests.push(request);
            if (request.action === 'cancel-query') {
                events.push('cancel-request');
                if (stopInProgress) {
                    request.jqXHR.abort();
                }
            }
        },
        window: {
            stop() {
                parentStopCalls += 1;
                stopInProgress = true;
                observedRequests
                    .filter((request) => request.action === 'cancel-query' && !request.aborted && !request.completed)
                    .forEach((request) => request.jqXHR.abort());
                stopInProgress = false;
            }
        }
    });

    harness.runPageLoad();
    harness.setValue('action', 'exec');
    assert.equal(harness.context.workbench.query.doSubmit(), false);
    const resultFrame = harness.getResultFrame();
    resultFrame.contentWindow.stop = () => {
        events.push('frame-stop');
        stopInProgress = true;
        observedRequests
            .filter((request) => request.action === 'cancel-query' && !request.aborted && !request.completed)
            .forEach((request) => request.jqXHR.abort());
        stopInProgress = false;
    };
    harness.click('query-cancel');

    const cancelRequests = observedRequests.filter((request) => request.action === 'cancel-query');
    assert.equal(parentStopCalls, 0);
    assert.equal(resultFrame.contentWindow.stopCount, 0);
    assert.equal(events[0], 'frame-stop');
    assert.equal(cancelRequests.length, 1);
    assert.equal(cancelRequests[0].params.get('query-request-id'), 'query-1');
    assert.equal(cancelRequests[0].aborted, false);
    assert.equal(harness.getProperty('query-request-id', 'value'), '');
    assert.equal(harness.getProperty('query-cancel', 'disabled'), true);
    resultFrame.trigger('error');
    assert.equal(harness.getText('query-results-status'), 'Query cancelled.');
});

test('page lifecycle events clear stale query cancellation state', () => {
    const { harness } = submitQuery({
        serverRequestIds: ['query-1', 'query-2']
    });
    const pendingFrame = harness.getResultFrame();

    harness.window.trigger('pagehide');
    assert.equal(pendingFrame.contentWindow.stopCount, 1);
    assert.equal(harness.getProperty('query-results-loading', 'hidden'), true);
    assert.equal(harness.getProperty('query-request-id', 'value'), '');
    assert.equal(harness.getProperty('query-cancel', 'disabled'), true);
    assert.equal(harness.hasClass('query-cancel', 'query-cancel--visible'), false);

    harness.setValue('action', 'exec');
    harness.context.workbench.query.doSubmit();
    assert.equal(harness.getProperty('query-request-id', 'value'), 'query-2');

    harness.window.trigger('pageshow');
    assert.equal(harness.getProperty('query-results-loading', 'hidden'), true);
    assert.equal(harness.getProperty('query-request-id', 'value'), '');
    assert.equal(harness.getProperty('query-cancel', 'disabled'), true);
});

test('embedded graph limits submit a fresh request from both GET and POST result origins', () => {
    for (const href of [
        'http://localhost:8080/rdf4j-workbench/repositories/test/query?action=exec&embedded=true',
        'http://localhost:8080/rdf4j-workbench/repositories/test/query'
    ]) {
        const resultHarness = createScriptHarness({
            href,
            serverRequestIds: ['limit-1'],
            window: {
                parent: {
                    postMessage() {
                    }
                }
            }
        });
        const queryText = resultHarness.registerElement('textarea', {
            id: 'wb-query-text',
            value: 'CONSTRUCT {?s ?p ?o} WHERE {?s ?p ?o}'
        });
        const limit = resultHarness.registerElement('select', { id: 'limit_query', value: '25' });
        const marker = resultHarness.registerElement('meta', {
            id: 'rdf4j-query-result',
            attributes: { 'data-query-embedded': 'true' }
        });
        [queryText, limit, marker].forEach((element) => resultHarness.document.body.appendChild(element));

        resultHarness.runScript('tools/workbench/src/main/webapp/scripts/template.js');
        resultHarness.runScript('tools/workbench/src/main/webapp/scripts/paging.js');
        resultHarness.context.workbench.paging.addGraphParam('limit_query');

        const form = resultHarness.document.lastSubmittedForm;
        const params = new URLSearchParams(form.serializeArray()
            .map((entry) => `${entry.name}=${encodeURIComponent(entry.value)}`).join('&'));
        assert.equal(params.get('embedded'), 'true');
        assert.equal(params.get('query-request-id'), 'limit-1');
        assert.equal(params.get('limit_query'), '25');
    }
});

test('embedded paging uses rendered query options and overrides one result limit', () => {
    const resultHarness = createScriptHarness({
        href: 'http://localhost:8080/rdf4j-workbench/repositories/test/query',
        serverRequestIds: ['options-1', 'options-2', 'options-3'],
        window: {
            parent: {
                postMessage() {
                }
            }
        }
    });
    resultHarness.document.cookie = 'query=stale-cookie-query; ref=stale-cookie-ref; queryLn=SERQL; infer=false; query-timeout=99; limit_query=99';
    const queryText = resultHarness.registerElement('textarea', {
        id: 'wb-query-text',
        value: 'SELECT * WHERE {?s ?p ?o}'
    });
    const limit = resultHarness.registerElement('select', { id: 'limit_query', value: '25' });
    const marker = resultHarness.registerElement('meta', {
        id: 'rdf4j-query-result',
        attributes: {
            'data-query-embedded': 'true',
            'data-query-language': 'SPARQL',
            'data-query-infer': 'true',
            'data-query-timeout': '12'
        }
    });
    [queryText, limit, marker].forEach((element) => resultHarness.document.body.appendChild(element));

    resultHarness.runScript('tools/workbench/src/main/webapp/scripts/template.js');
    resultHarness.runScript('tools/workbench/src/main/webapp/scripts/paging.js');
    resultHarness.context.workbench.paging.addGraphParam('limit_query');
    const limitParams = new URLSearchParams(resultHarness.document.lastSubmittedForm.serializeArray()
        .map((entry) => `${entry.name}=${encodeURIComponent(entry.value)}`).join('&'));
    assert.deepEqual(limitParams.getAll('queryLn'), ['SPARQL']);
    assert.deepEqual(limitParams.getAll('infer'), ['true']);
    assert.deepEqual(limitParams.getAll('query-timeout'), ['12']);
    assert.deepEqual(limitParams.getAll('limit_query'), ['25']);
    assert.equal(limitParams.get('query'), 'SELECT * WHERE {?s ?p ?o}');
    assert.equal(limitParams.get('ref'), 'text');

    resultHarness.context.workbench.paging.addPagingParam('offset', 25);
    const pagingParams = new URLSearchParams(resultHarness.document.lastSubmittedForm.serializeArray()
        .map((entry) => `${entry.name}=${encodeURIComponent(entry.value)}`).join('&'));
    assert.deepEqual(pagingParams.getAll('queryLn'), ['SPARQL']);
    assert.deepEqual(pagingParams.getAll('infer'), ['true']);
    assert.deepEqual(pagingParams.getAll('query-timeout'), ['12']);
    assert.deepEqual(pagingParams.getAll('limit_query'), ['25']);

    resultHarness.context.workbench.paging.addPagingParam('limit_query', 50);
    const overrideParams = new URLSearchParams(resultHarness.document.lastSubmittedForm.serializeArray()
        .map((entry) => `${entry.name}=${encodeURIComponent(entry.value)}`).join('&'));
    assert.deepEqual(overrideParams.getAll('limit_query'), ['50']);
    assert.deepEqual(overrideParams.getAll('queryLn'), ['SPARQL']);
    assert.deepEqual(overrideParams.getAll('infer'), ['true']);
    assert.deepEqual(overrideParams.getAll('query-timeout'), ['12']);
});

test('result page script posts a terminal message from the result document to its opener', () => {
    const messages = [];
    const resultHarness = createScriptHarness({
        href: 'http://localhost:8080/rdf4j-workbench/repositories/test/tuple',
        window: {
            opener: {
                closed: false,
                postMessage(message, origin) {
                    messages.push({ message, origin });
                }
            }
        }
    });
    const marker = resultHarness.registerElement('meta', {
        id: 'rdf4j-query-result',
        attributes: {
            'data-query-request-id': 'query-1',
            'data-query-result-status': 'error'
        }
    });
    resultHarness.document.body.appendChild(marker);

    resultHarness.runScript('tools/workbench/src/main/webapp/scripts/template.js');
    resultHarness.runScript('tools/workbench/src/main/webapp/scripts/queryResult.js');
    resultHarness.window.trigger('load');

    assert.equal(messages.length, 1);
    assert.equal(messages[0].origin, 'http://localhost:8080');
    assert.equal(messages[0].message.type, 'rdf4j-query-result');
    assert.equal(messages[0].message.queryRequestId, 'query-1');
    assert.equal(messages[0].message.status, 'error');
});

test('embedded result page script posts completion to its parent window', () => {
    const messages = [];
    const parentWindow = {
        closed: false,
        postMessage(message, origin) {
            messages.push({ message, origin });
        }
    };
    const resultHarness = createScriptHarness({
        href: 'http://localhost:8080/rdf4j-workbench/repositories/test/query',
        window: { parent: parentWindow }
    });
    const marker = resultHarness.registerElement('meta', {
        id: 'rdf4j-query-result',
        attributes: {
            'data-query-request-id': 'query-embedded-1',
            'data-query-result-status': 'completed'
        }
    });
    resultHarness.document.body.appendChild(marker);

    resultHarness.runScript('tools/workbench/src/main/webapp/scripts/template.js');
    resultHarness.runScript('tools/workbench/src/main/webapp/scripts/queryResult.js');
    resultHarness.window.trigger('load');

    assert.equal(messages.length, 1);
    assert.equal(messages[0].origin, 'http://localhost:8080');
    assert.equal(messages[0].message.queryRequestId, 'query-embedded-1');
});

test('embedded paging announces a fresh result request and preserves the result mode', () => {
    const messages = [];
    const resultHarness = createScriptHarness({
        href: 'http://localhost:8080/rdf4j-workbench/repositories/test/query',
        serverRequestIds: ['page-1'],
        window: {
            parent: {
                postMessage(message, origin) {
                    messages.push({ message, origin });
                }
            }
        }
    });
    const queryText = resultHarness.registerElement('textarea', {
        id: 'wb-query-text',
        value: 'SELECT * WHERE {?s ?p ?o}'
    });
    const limit = resultHarness.registerElement('select', { id: 'limit_query', value: '25' });
    const total = resultHarness.registerElement('input', {
        id: 'workbench-total-result-count',
        value: '100'
    });
    const marker = resultHarness.registerElement('meta', {
        id: 'rdf4j-query-result',
        attributes: { 'data-query-embedded': 'true' }
    });
    [queryText, limit, total, marker].forEach((element) => resultHarness.document.body.appendChild(element));

    resultHarness.runScript('tools/workbench/src/main/webapp/scripts/template.js');
    resultHarness.runScript('tools/workbench/src/main/webapp/scripts/paging.js');
    resultHarness.context.workbench.paging.addPagingParam('offset', '25');

    const form = resultHarness.document.lastSubmittedForm;
    const params = new URLSearchParams(form.serializeArray()
        .map((entry) => `${entry.name}=${encodeURIComponent(entry.value)}`).join('&'));
    assert.equal(params.get('action'), 'exec');
    assert.equal(params.get('query'), 'SELECT * WHERE {?s ?p ?o}');
    assert.equal(params.get('embedded'), 'true');
    assert.equal(params.get('query-request-id'), 'page-1');
    assert.equal(params.get('offset'), '25');
    assert.equal(messages.length, 1);
    assert.equal(messages[0].message.type, 'rdf4j-query-start');
    assert.equal(messages[0].message.queryRequestId, 'page-1');
});

test('embedded downloads keep attachment parameters without entering result lifecycle', () => {
    const messages = [];
    const resultHarness = createScriptHarness({
        href: 'http://localhost:8080/rdf4j-workbench/repositories/test/query',
        serverRequestIds: ['download-1'],
        window: {
            parent: {
                postMessage(message) {
                    messages.push(message);
                }
            }
        }
    });
    const queryText = resultHarness.registerElement('textarea', {
        id: 'wb-query-text',
        value: 'CONSTRUCT {?s ?p ?o} WHERE {?s ?p ?o}'
    });
    resultHarness.document.cookie = 'queryLn=SERQL; infer=true; query-timeout=99; limit_query=99';
    const accept = resultHarness.registerElement('select', { id: 'Accept', value: 'text/turtle' });
    const downloadLimit = resultHarness.registerElement('select', { id: 'download_limit', value: '100' });
    const limit = resultHarness.registerElement('select', { id: 'limit_query', value: '25' });
    const marker = resultHarness.registerElement('meta', {
        id: 'rdf4j-query-result',
        attributes: {
            'data-query-embedded': 'true',
            'data-query-language': 'SPARQL',
            'data-query-infer': 'false',
            'data-query-timeout': '12'
        }
    });
    [queryText, accept, downloadLimit, limit, marker]
        .forEach((element) => resultHarness.document.body.appendChild(element));

    resultHarness.runScript('tools/workbench/src/main/webapp/scripts/template.js');
    resultHarness.runScript('tools/workbench/src/main/webapp/scripts/paging.js');
    resultHarness.context.workbench.paging.addGraphParam('Accept');

    const form = resultHarness.document.lastSubmittedForm;
    const params = new URLSearchParams(form.serializeArray()
        .map((entry) => `${entry.name}=${encodeURIComponent(entry.value)}`).join('&'));
    assert.equal(params.get('Accept'), 'text/turtle');
    assert.equal(params.get('download_limit'), '100');
    assert.deepEqual(params.getAll('queryLn'), ['SPARQL']);
    assert.deepEqual(params.getAll('infer'), ['false']);
    assert.deepEqual(params.getAll('query-timeout'), ['12']);
    assert.deepEqual(params.getAll('limit_query'), ['25']);
    assert.equal(params.get('embedded'), null);
    assert.equal(params.get('query-request-id'), null);
    assert.equal(messages.length, 0);
});
