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
    const resultWindow = harness.openedWindows[0];
    assert.ok(resultWindow);
    assert.equal(harness.getProperty('query-request-id', 'value'), requestId);
    assert.equal(harness.getProperty('query-cancel', 'disabled'), false);
    assert.equal(submittedByPost, !!options.longQuery);
    return { harness, resultWindow };
}

test('result document completion clears the active GET query and cancel control', () => {
    const { harness, resultWindow } = submitQuery();

    harness.emitResultMessage(resultWindow, {
        type: 'rdf4j-query-result',
        queryRequestId: 'query-1',
        status: 'completed'
    });

    assert.equal(harness.getProperty('query-request-id', 'value'), '');
    assert.equal(harness.getProperty('query-cancel', 'disabled'), true);
    assert.equal(harness.hasClass('query-cancel', 'query-cancel--visible'), false);
});

test('result document completion clears a long-query POST target', () => {
    const { harness, resultWindow } = submitQuery({
        query: 'x'.repeat(3000),
        longQuery: true
    });

    assert.equal(harness.getProperty('query-request-id', 'value'), 'query-1');
    assert.equal(harness.getProperty('query-cancel', 'disabled'), false);
    assert.equal(harness.getAttribute('query-cancel', 'aria-hidden'), 'false');

    harness.emitResultMessage(resultWindow, {
        type: 'rdf4j-query-result',
        queryRequestId: 'query-1',
        status: 'completed'
    });

    assert.equal(harness.getProperty('query-request-id', 'value'), '');
    assert.equal(harness.getProperty('query-cancel', 'disabled'), true);
});

test('result notifications require the active window, origin, and request id', () => {
    const { harness, resultWindow } = submitQuery();
    const unrelatedWindow = harness.openedWindows[0] === resultWindow
        ? { closed: false }
        : harness.openedWindows[0];
    const message = {
        type: 'rdf4j-query-result',
        queryRequestId: 'query-1',
        status: 'completed'
    };

    harness.emitResultMessage(unrelatedWindow, message);
    harness.emitResultMessage(resultWindow, Object.assign({}, message, { queryRequestId: 'stale-query' }));
    harness.emitResultMessage(resultWindow, message, 'http://attacker.invalid');

    assert.equal(harness.getProperty('query-request-id', 'value'), 'query-1');
    assert.equal(harness.getProperty('query-cancel', 'disabled'), false);

    harness.emitResultMessage(resultWindow, message);
    assert.equal(harness.getProperty('query-request-id', 'value'), '');
});

test('closing the result window clears the active query after the lifecycle check', () => {
    const { harness, resultWindow } = submitQuery();

    resultWindow.close();
    harness.advanceTimers(250);

    assert.equal(harness.requestsByAction('cancel-query').length, 1);
    assert.equal(
        harness.requestsByAction('cancel-query')[0].params.get('query-request-id'),
        'query-1'
    );
    assert.equal(harness.getProperty('query-request-id', 'value'), '');
    assert.equal(harness.getProperty('query-cancel', 'disabled'), true);
});

test('a completed same-origin XML error document clears a tracked query without cancellation', () => {
    const { harness, resultWindow } = submitQuery();

    resultWindow.location.href = 'http://localhost:8080/rdf4j-workbench/repositories/test/query?error=true';
    resultWindow.document = {
        readyState: 'complete',
        location: resultWindow.location,
        contentType: 'application/xml'
    };
    harness.advanceTimers(250);

    assert.equal(harness.requestsByAction('cancel-query').length, 0);
    assert.equal(harness.getProperty('query-request-id', 'value'), '');
    assert.equal(harness.getProperty('query-cancel', 'disabled'), true);
});

test('a completed attachment document keeps cancellation available until the user closes it', () => {
    const { harness, resultWindow } = submitQuery();

    resultWindow.location.href = 'http://localhost:8080/rdf4j-workbench/repositories/test/query?download=true';
    resultWindow.document = {
        readyState: 'complete',
        location: resultWindow.location,
        contentType: 'text/csv'
    };
    harness.advanceTimers(250);

    assert.equal(harness.requestsByAction('cancel-query').length, 0);
    assert.equal(harness.getProperty('query-request-id', 'value'), 'query-1');
    resultWindow.close();
    harness.advanceTimers(250);
    assert.equal(harness.requestsByAction('cancel-query').length, 1);
});

test('error and download result notifications both finish the active query', () => {
    for (const status of ['error', 'download']) {
        const { harness, resultWindow } = submitQuery({ serverRequestIds: [`query-${status}`] });
        harness.emitResultMessage(resultWindow, {
            type: 'rdf4j-query-result',
            queryRequestId: `query-${status}`,
            status
        });
        assert.equal(harness.getProperty('query-request-id', 'value'), '', status);
        assert.equal(harness.getProperty('query-cancel', 'disabled'), true, status);
    }
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
