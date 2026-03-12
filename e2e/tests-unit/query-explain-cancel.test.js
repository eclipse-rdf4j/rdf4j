const test = require('node:test');
const assert = require('node:assert/strict');

const { createQueryBrowserHarness } = require('./query-browser-harness.js');

test('cancels the active slow explain request after level change and explicit cancel', () => {
    const harness = createQueryBrowserHarness({
        serverRequestIds: ['request-1', 'request-2']
    });

    harness.click('explain-trigger');

    const firstExplainRequest = harness.requestsByAction('explain')[0];
    assert.ok(firstExplainRequest);
    assert.equal(firstExplainRequest.params.get('explain'), 'Optimized');
    assert.equal(firstExplainRequest.params.get('explain-request-id'), 'request-1');

    harness.setValue('explain-level', 'Timed');

    const firstCancelRequest = harness.requestsByAction('cancel-explain')[0];
    assert.ok(firstCancelRequest);
    assert.equal(firstCancelRequest.params.get('explain-request-id'), 'request-1');

    harness.click('explain-trigger');

    const explainRequests = harness.requestsByAction('explain');
    assert.equal(explainRequests.length, 2);
    assert.equal(explainRequests[1].params.get('explain'), 'Timed');
    assert.equal(explainRequests[1].params.get('explain-request-id'), 'request-2');

    harness.advanceTimers(1000);

    assert.equal(harness.hasClass('explain-trigger-spinner', 'query-explain-spinner--visible'), true);
    assert.equal(harness.getAttribute('explain-trigger-spinner', 'aria-hidden'), 'false');
    assert.equal(harness.hasClass('explain-trigger-cancel', 'query-explain-cancel--visible'), true);
    assert.equal(harness.getAttribute('explain-trigger-cancel', 'aria-hidden'), 'false');
    assert.equal(harness.getProperty('explain-trigger-cancel', 'disabled'), false);

    harness.click('explain-trigger-cancel');

    const cancelRequests = harness.requestsByAction('cancel-explain');
    assert.equal(cancelRequests.length, 2);
    assert.equal(cancelRequests[1].params.get('explain-request-id'), 'request-2');

    harness.advanceTimers(1000);

    assert.equal(harness.hasClass('explain-trigger-spinner', 'query-explain-spinner--visible'), false);
    assert.equal(harness.getAttribute('explain-trigger-spinner', 'aria-hidden'), 'true');
    assert.equal(harness.hasClass('explain-trigger-cancel', 'query-explain-cancel--visible'), false);
    assert.equal(harness.getAttribute('explain-trigger-cancel', 'aria-hidden'), 'true');
    assert.equal(harness.getProperty('explain-trigger-cancel', 'disabled'), true);
});
