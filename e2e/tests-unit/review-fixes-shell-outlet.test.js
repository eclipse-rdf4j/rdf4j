// Review fixes (TS-A): what every routed page's outlet shows.
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { installDetailDisclosureTemplateRuntime } = require('./workbench-detail-disclosure-runtime.js');

const scripts = process.env.WORKBENCH_SCRIPT_DIR
    || path.resolve(__dirname, '../../tools/workbench/src/main/webapp/scripts');

function loadViews() {
    const window = { location: { href: 'http://x.test/workbench/repositories/r/summary', search: '' } };
    const workbench = {};
    installDetailDisclosureTemplateRuntime(workbench);
    const context = vm.createContext({ console, URL, URLSearchParams, Promise, window, workbench, setTimeout });
    vm.runInContext(fs.readFileSync(path.join(scripts, 'workbenchViews.js'), 'utf8'), context,
        { filename: 'workbenchViews.js' });
    return context.workbench;
}

function runtime() {
    return { nothing: '', html(strings, ...values) { return { strings: Array.from(strings), values }; }, render() {} };
}

function flat(template) {
    if (Array.isArray(template)) { return template.map(flat).join(''); }
    if (template && Array.isArray(template.strings)) {
        return template.strings.map((part, index) => part
            + (index < template.values.length ? flat(template.values[index]) : '')).join('');
    }
    return template == null || typeof template === 'function' ? '' : String(template);
}

function quiet(fn) {
    const error = console.error;
    console.error = () => {};
    try { return fn(); } finally { console.error = error; }
}

const ctx = { basePath: '/workbench', repositoryId: 'r', workbench: {} };

test('A6: no routed page carries a "Scripting is not enabled" notice that only bootstrap hides', () => {
    const wb = loadViews();
    for (const viewId of ['summary', 'types', 'remove', 'query', 'repositories']) {
        const markup = flat(quiet(() => wb.views.pageTemplate({ viewId, vars: [], rows: [], rowCount: 0, metadata: {} },
            ctx, runtime())));
        assert.doesNotMatch(markup, /noscript-message|Scripting is not enabled/, viewId);
    }
});

const unauthorized = 'The entered credentials entered either failed to authenticate to the RDF4J server, or were unauthorized for the requested operation.';

test('A26: an answer that only carries an error message is shown as that error on every page', () => {
    const wb = loadViews();
    for (const viewId of ['repositories', 'delete', 'create', 'saved-queries', 'information', 'summary', 'namespaces',
        'contexts', 'types', 'export']) {
        const model = { viewId, vars: ['error-message'], rows: [[{ kind: 'literal', value: unauthorized }]], rowCount: 1,
            metadata: {}, pickerRows: [], pickerPageSize: 50 };
        const markup = flat(quiet(() => wb.views.pageTemplate(model, ctx, runtime())));
        assert.match(markup, /workbench-callout--error/, viewId + ' shows an error callout');
        assert.match(markup, /failed to authenticate/, viewId + ' names the error');
        assert.doesNotMatch(markup, /<table|data-workbench-row-table|id="delete-form"|id="create-type-form"/,
            viewId + ' renders no data or form for it');
    }
});

test('A26: the rows of an error-only answer are read before it is shown', async () => {
    const workbench = {};
    installDetailDisclosureTemplateRuntime(workbench);
    const context = vm.createContext({ console, URL, URLSearchParams, Promise, window: {}, workbench, setTimeout });
    quiet(() => {
        for (const filename of ['workbenchViews.js', 'workbenchRoutes.js', 'queryStream.js', 'workbenchApp.js']) {
            vm.runInContext(fs.readFileSync(path.join(scripts, filename), 'utf8'), context, { filename });
        }
    });
    const row = [{ kind: 'literal', value: unauthorized }];
    const model = { viewId: 'repositories', vars: ['error-message'], rows: [], rowCount: 1, metadata: {},
        rowStore: { read: (start, count) => Promise.resolve([row].slice(start, start + count)) } };
    await context.workbench.app.prepareInitialRows(model);
    assert.equal(model.rows.length, 1);
    assert.equal(model.rows[0][0].value, unauthorized);
});

test('A24: a saved query\'s inline result is not one live region; only its status line announces', () => {
    const wb = loadViews();
    const literal = (value) => ({ kind: 'literal', value });
    const model = { viewId: 'saved-queries', vars: ['query', 'user', 'queryName', 'queryLn', 'queryText', 'infer', 'rowsPerPage'],
        rows: [[{ kind: 'iri', value: 'urn:q' }, literal('alice'), literal('Q'), literal('SPARQL'),
            literal('SELECT * WHERE { ?s ?p ?o }'), literal('true'), literal('10')]], rowCount: 1, metadata: {} };
    const markup = flat(quiet(() => wb.views.pageTemplate(model, ctx, runtime())));
    const at = markup.indexOf('id=saved-query-results-');
    assert.ok(at > 0, 'the result container is rendered');
    const tag = markup.slice(markup.lastIndexOf('<', at), markup.indexOf('>', at) + 1);
    assert.doesNotMatch(tag, /aria-live/, 'a streamed table is not read out row by row');
});
