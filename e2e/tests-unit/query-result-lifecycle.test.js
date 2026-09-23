const test = require('node:test');
const assert = require('node:assert/strict');

const { createQueryBrowserHarness } = require('./query-browser-harness.js');
const { createScriptHarness } = require('./script-harness.js');

function submitQuery(options = {}) {
    const requestId = (options.serverRequestIds || ['query-1'])[0];
    const harness = createQueryBrowserHarness(Object.assign({
        serverRequestIds: ['query-1'],
        query: 'SELECT * WHERE {?s ?p ?o}'
    }, options));
    harness.runPageLoad();
    harness.setValue('action', 'exec');
    const submittedByPost = harness.context.workbench.query.doSubmit();
    assert.equal(harness.getProperty('query-request-id', 'value'), requestId);
    assert.equal(harness.getProperty('query-cancel', 'disabled'), false);
    assert.equal(submittedByPost, !!options.longQuery);
    return { harness, requestId, submittedByPost };
}

test('short query GET navigates the current document without opening a popup', () => {
    const { harness } = submitQuery();

    assert.equal(harness.openedWindows.length, 0);
    assert.equal(harness.document.location.pathname, '/query');
    assert.match(harness.document.location.search, /action=exec/);
    assert.match(harness.document.location.search, /query-request-id=query-1/);
});

test('long query POST stays on the current form without a named target', () => {
    const { harness } = submitQuery({
        query: 'x'.repeat(3000),
        longQuery: true
    });
    const form = harness.document.querySelectorAll('form[action="query"]')[0];

    assert.equal(harness.openedWindows.length, 0);
    assert.equal(harness.getProperty('include-query-text', 'value'), 'true');
    assert.equal(form.getAttribute('target'), undefined);
    form.submit();
    assert.equal(harness.document.lastSubmittedForm, form);
    assert.equal(form.submitCount, 1);
});

test('repeated execution cancels the prior request and tracks a fresh request id', () => {
    const { harness } = submitQuery({
        serverRequestIds: ['query-1', 'query-2']
    });

    harness.context.workbench.query.doSubmit();

    const cancelRequests = harness.requestsByAction('cancel-query');
    assert.equal(cancelRequests.length, 1);
    assert.equal(cancelRequests[0].params.get('query-request-id'), 'query-1');
    assert.equal(harness.getProperty('query-request-id', 'value'), 'query-2');
    assert.equal(harness.getProperty('query-cancel', 'disabled'), false);
    assert.equal(harness.openedWindows.length, 0);
});

test('cancellation stops the current navigation before posting backend cancellation', () => {
    const events = [];
    const observedRequests = [];
    let stopInProgress = false;
    let stopCalls = 0;
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
                events.push('stop');
                stopCalls += 1;
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
    harness.click('query-cancel');

    const cancelRequests = observedRequests.filter((request) => request.action === 'cancel-query');
    assert.equal(stopCalls, 1);
    assert.equal(events[0], 'stop');
    assert.equal(cancelRequests.length, 1);
    assert.equal(cancelRequests[0].params.get('query-request-id'), 'query-1');
    assert.equal(cancelRequests[0].aborted, false);
    assert.equal(harness.getProperty('query-request-id', 'value'), '');
    assert.equal(harness.getProperty('query-cancel', 'disabled'), true);
});

test('page lifecycle events clear stale query cancellation state', () => {
    const { harness } = submitQuery({
        serverRequestIds: ['query-1', 'query-2']
    });

    harness.window.trigger('pagehide');
    assert.equal(harness.getProperty('query-request-id', 'value'), '');
    assert.equal(harness.getProperty('query-cancel', 'disabled'), true);
    assert.equal(harness.hasClass('query-cancel', 'query-cancel--visible'), false);

    harness.setValue('action', 'exec');
    harness.context.workbench.query.doSubmit();
    assert.equal(harness.getProperty('query-request-id', 'value'), 'query-2');

    harness.window.trigger('pageshow');
    assert.equal(harness.getProperty('query-request-id', 'value'), '');
    assert.equal(harness.getProperty('query-cancel', 'disabled'), true);
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

    resultHarness.runScript('tools/workbench/src/main/webapp/scripts/queryResult.js');
    resultHarness.window.trigger('load');

    assert.equal(messages.length, 1);
    assert.equal(messages[0].origin, 'http://localhost:8080');
    assert.equal(messages[0].message.type, 'rdf4j-query-result');
    assert.equal(messages[0].message.queryRequestId, 'query-1');
    assert.equal(messages[0].message.status, 'error');
});
