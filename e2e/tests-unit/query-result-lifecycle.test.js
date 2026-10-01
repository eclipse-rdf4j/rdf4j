const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { spawnSync } = require('node:child_process');

const { createQueryBrowserHarness } = require('./query-browser-harness.js');

function compileQuerySource() {
    const outputDirectory = fs.mkdtempSync(path.join(os.tmpdir(), 'rdf4j-query-source-contract-'));
    const outputPath = path.join(outputDirectory, 'query.js');
    const sharedTypePath = path.join(outputDirectory, 'query-stream-fullscreen.d.ts');
    const sourcePath = path.resolve(__dirname,
        '../../tools/workbench/src/main/webapp/scripts/ts/query.ts');
    fs.writeFileSync(sharedTypePath, `declare namespace workbench {
    module resultFullscreen {
        function currentTarget(): HTMLElement;
        function isFullscreen(target?: HTMLElement): boolean;
        function set(target: HTMLElement, control: HTMLButtonElement, enabled: boolean,
                restoreFocus?: boolean, callbacks?: any): void;
        function toggle(target: HTMLElement, control: HTMLButtonElement): void;
    }
}`);
    const compilation = spawnSync('tsc', [
        '--target', 'ES2017',
        '--lib', 'ES2017,DOM',
        '--skipLibCheck',
        '--outFile', outputPath,
        sharedTypePath,
        sourcePath
    ], { encoding: 'utf8' });
    assert.equal(compilation.status, 0,
        `query.ts must compile for its in-repo lifecycle contract:\n${compilation.stdout}${compilation.stderr}`);
    return outputPath;
}

test('mounted query stream controller owns execution', () => {
    let submitCalls = 0;
    const harness = createQueryBrowserHarness({
        query: 'SELECT * WHERE {?s ?p ?o}',
        queryScriptPath: compileQuerySource(),
        workbench: {
            queryPage: {
                ownsForm() { return true; },
                submitExecution() {
                    submitCalls += 1;
                    return false;
                }
            }
        }
    });

    const result = harness.context.workbench.query.doSubmit();

    assert.equal(result, false);
    assert.equal(submitCalls, 1);
    assert.equal(harness.document.getElementById('query-results-frame'), null, 'there is no result frame (M9.1)');
});

test('query cancellation delegates to the mounted streaming controller', () => {
    let cancelCalls = 0;
    const harness = createQueryBrowserHarness({
        queryScriptPath: compileQuerySource(),
        workbench: {
            queryPage: {
                ownsForm() { return true; },
                hasActiveRequest() { return true; },
                cancelExecution() {
                    cancelCalls += 1;
                    return true;
                }
            }
        }
    });
    harness.context.workbench.query.cancelQuery();

    assert.equal(cancelCalls, 1);
});


test('without the streamed renderer a submit says that results are unavailable', () => {
    const harness = createQueryBrowserHarness({ query: 'SELECT * WHERE {?s ?p ?o}' });
    harness.setValue('action', 'exec');

    assert.equal(harness.context.workbench.query.doSubmit(), false);
    assert.equal(harness.getText('query-results-status'), 'Query results are unavailable on this page.');
    assert.equal(harness.document.lastSubmittedForm, undefined, 'nothing is posted');
});

test('the result area starts afresh when the page is hidden or shown again', () => {
    for (const type of ['pagehide', 'pageshow']) {
        const harness = createQueryBrowserHarness({ queryScriptPath: compileQuerySource() });
        harness.runPageLoad();
        const results = harness.document.getElementById('query-results');
        results.hidden = false;
        harness.setValue('query-request-id', 'query-1');
        harness.document.getElementById('query-results-status').textContent = 'Query cancelled.';

        harness.window.trigger(type);

        assert.equal(results.hidden, true, type);
        assert.equal(harness.getProperty('query-request-id', 'value'), '', type);
        assert.equal(harness.getText('query-results-status'), '', type);
        assert.equal(harness.getProperty('query-cancel', 'disabled'), true, type);
    }
});

test('a server-side query cancellation is retried until the server accepts it', () => {
    const harness = createQueryBrowserHarness({
        onAjaxRequest(request) {
            if (request.action === 'cancel-query' && harness.requestsByAction('cancel-query').length < 3) {
                request.reject('error', 'Service Unavailable');
            }
        }
    });

    harness.context.workbench.query.cancelServerQuery('query-1');
    harness.context.workbench.query.cancelServerQuery('');

    const cancels = harness.requestsByAction('cancel-query');
    assert.equal(cancels.length, 3, 'two failed attempts are retried');
    assert.deepEqual(cancels.map((request) => request.params.get('query-request-id')),
        ['query-1', 'query-1', 'query-1']);
});
