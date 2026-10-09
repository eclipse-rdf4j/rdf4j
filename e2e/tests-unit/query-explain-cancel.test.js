const test = require('node:test');
const assert = require('node:assert/strict');

const { createQueryBrowserHarness } = require('./query-browser-harness.js');

test('does not start an Explain rerun when that UI feature is disabled', () => {
    const harness = createQueryBrowserHarness();

    harness.runPageLoad();
    harness.context.workbench.query.setQueryValue('ASK {}');
    harness.document.getElementById('rerun-explanation').setAttribute('data-query-rerun-enabled', 'false');

    harness.click('rerun-explanation');

    assert.equal(harness.requestsByAction('explain').length, 0);
});

test('the Explanation panel is busy while a plan loads so it shows the local progress bar', () => {
    const harness = createQueryBrowserHarness({
        serverRequestIds: ['request-1', 'request-2', 'request-3', 'request-4']
    });

    harness.runPageLoad();
    harness.context.workbench.query.setQueryValue('ASK {}');
    assert.notEqual(harness.getAttribute('query-explanation-panel', 'aria-busy'), 'true');

    harness.click('explain-trigger');
    assert.equal(harness.getAttribute('query-explanation-panel', 'aria-busy'), 'true');
    harness.pendingExplainRequests[0].resolve({ content: '{"type":"Projection"}', format: 'json', error: '' });
    assert.equal(harness.getAttribute('query-explanation-panel', 'aria-busy'), 'false');

    harness.click('rerun-explanation');
    harness.advanceTimers(1000);
    assert.equal(harness.getAttribute('query-explanation-panel', 'aria-busy'), 'true');
    harness.click('rerun-explanation-cancel');
    assert.equal(harness.getAttribute('query-explanation-panel', 'aria-busy'), 'false');

    harness.context.workbench.query.toggleCompareMode();
    harness.advanceTimers(1000);
    assert.equal(harness.getAttribute('query-explanation-panel', 'aria-busy'), 'true');
    harness.pendingExplainRequests.slice(2).forEach(request => request.resolve({
        content: '{"type":"Projection"}', format: 'json', error: ''
    }));
    assert.equal(harness.getAttribute('query-explanation-panel', 'aria-busy'), 'false');
});

test('Explain transport does not impose a fixed client timeout', () => {
    const harness = createQueryBrowserHarness();

    harness.click('explain-trigger');

    const request = harness.pendingExplainRequests[0];
    assert.ok(request, 'Explain should start its asynchronous request');
    assert.equal(Object.prototype.hasOwnProperty.call(request.options, 'timeout'), false,
        'long-running query explanations remain governed by the configured server timeout');
});

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

    assert.equal(harness.getAttribute('query-explanation-panel', 'aria-busy'), 'true');
    assert.equal(harness.hasClass('explain-trigger-cancel', 'query-explain-cancel--visible'), true);
    assert.equal(harness.getAttribute('explain-trigger-cancel', 'aria-hidden'), 'false');
    assert.equal(harness.getProperty('explain-trigger-cancel', 'disabled'), false);

    harness.click('explain-trigger-cancel');

    const cancelRequests = harness.requestsByAction('cancel-explain');
    assert.equal(cancelRequests.length, 2);
    assert.equal(cancelRequests[1].params.get('explain-request-id'), 'request-2');

    harness.advanceTimers(1000);

    assert.equal(harness.getAttribute('query-explanation-panel', 'aria-busy'), 'false');
    assert.equal(harness.hasClass('explain-trigger-cancel', 'query-explain-cancel--visible'), false);
    assert.equal(harness.getAttribute('explain-trigger-cancel', 'aria-hidden'), 'true');
    assert.equal(harness.getProperty('explain-trigger-cancel', 'disabled'), true);
});

test('cancels the in-flight primary explain before compare refresh starts', () => {
    const harness = createQueryBrowserHarness({
        serverRequestIds: ['request-1', 'request-2', 'request-3']
    });

    harness.runPageLoad();
    harness.context.workbench.query.runExplain('Optimized', 'explain-trigger');

    const primaryExplainRequest = harness.requestsByAction('explain')[0];
    assert.ok(primaryExplainRequest);
    assert.equal(primaryExplainRequest.params.get('explain-request-id'), 'request-1');

    harness.context.workbench.query.toggleCompareMode();
    harness.context.workbench.query.runCompareExplain('explain-trigger');

    const cancelRequests = harness.requestsByAction('cancel-explain');
    assert.equal(cancelRequests.length, 1);
    assert.equal(cancelRequests[0].params.get('explain-request-id'), 'request-1');
    assert.equal(primaryExplainRequest.aborted, true);
    assert.equal(harness.requestsByAction('explain').length, 3);
});

test('the Cancel beside Config shows while any explanation runs and cancels it', () => {
    const harness = createQueryBrowserHarness({
        serverRequestIds: ['request-1', 'request-2', 'request-3', 'request-4', 'request-5', 'request-6']
    });
    const plan = { content: '{"type":"Projection"}', format: 'json', error: '' };
    const cancelShown = () => harness.hasClass('explanation-cancel', 'query-explain-cancel--visible')
        && harness.getAttribute('explanation-cancel', 'aria-hidden') === 'false'
        && harness.getProperty('explanation-cancel', 'disabled') === false;

    harness.runPageLoad();
    harness.context.workbench.query.setQueryValue('ASK {}');
    harness.click('explain-trigger');
    assert.equal(cancelShown(), false, 'like the other Cancel buttons it waits a second before it shows');
    harness.advanceTimers(1000);
    assert.equal(cancelShown(), true, 'a running explanation shows the Cancel beside Config');
    assert.equal(harness.hasClass('explain-trigger-cancel', 'query-explain-cancel--visible'), true,
        'the Cancel beside the button that started the explanation still shows');

    harness.click('explanation-cancel');
    const primaryCancels = harness.requestsByAction('cancel-explain');
    assert.equal(primaryCancels.length, 1);
    assert.equal(primaryCancels[0].params.get('explain-request-id'), 'request-1');
    harness.advanceTimers(1000);
    assert.equal(cancelShown(), false);
    assert.equal(harness.getAttribute('explanation-cancel', 'aria-hidden'), 'true');

    harness.click('explain-trigger');
    harness.pendingExplainRequests[1].resolve(plan);
    harness.context.workbench.query.toggleCompareMode();
    harness.advanceTimers(1000);
    assert.equal(cancelShown(), true, 'explaining the compare query as compare mode opens shows it too');
    harness.pendingExplainRequests[2].resolve(plan);
    harness.advanceTimers(1000);
    assert.equal(cancelShown(), false);

    harness.click('explain-trigger');
    assert.equal(harness.pendingExplainRequests.length, 5, 'Explain explains both queries in compare mode');
    harness.advanceTimers(1000);
    assert.equal(cancelShown(), true, 'so does explaining both queries');
    harness.click('explanation-cancel');
    assert.deepEqual(harness.requestsByAction('cancel-explain').slice(1)
        .map(request => request.params.get('explain-request-id')), ['request-4', 'request-5'],
    'it cancels both compare requests');
    harness.advanceTimers(1000);
    assert.equal(cancelShown(), false);
});
