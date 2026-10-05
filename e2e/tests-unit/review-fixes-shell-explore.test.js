// Review fixes (TS-A): the Explore page.
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { installDetailDisclosureTemplateRuntime } = require('./workbench-detail-disclosure-runtime.js');
const { createExploreBrowserHarness } = require('./explore-browser-harness.js');
const { createFormBrowserHarness } = require('./form-browser-harness.js');

const scripts = process.env.WORKBENCH_SCRIPT_DIR
    || path.resolve(__dirname, '../../tools/workbench/src/main/webapp/scripts');

function loadViews(search = '') {
    const window = { location: { href: 'http://x.test/workbench/repositories/r/explore' + search, search } };
    const workbench = {};
    installDetailDisclosureTemplateRuntime(workbench);
    const context = vm.createContext({ console, URL, URLSearchParams, Promise, window, workbench, setTimeout });
    for (const filename of ['workbenchViews.js', 'workbenchRoutes.js', 'queryStream.js', 'workbenchApp.js']) {
        vm.runInContext(fs.readFileSync(path.join(scripts, filename), 'utf8'), context, { filename });
    }
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
const vars = ['subject', 'predicate', 'object', 'context'];
const iri = (value) => ({ kind: 'iri', value });

test('A10: a "+" in an explored resource stays a "+"', () => {
    const resource = '<http://example.org/C++>';
    const harness = createExploreBrowserHarness({
        href: 'http://localhost:8080/rdf4j-workbench/repositories/test/explore?resource=' + encodeURIComponent(resource)
    });
    harness.loadExploreScript();
    harness.workbench.explore.mount(harness.document.body);
    assert.equal(harness.document.getElementById('resource').value, resource);
});

test('A10: a "+" written as a space in the query string is still a space; "=" in a value is kept', () => {
    const harness = createExploreBrowserHarness({
        href: 'http://localhost:8080/rdf4j-workbench/repositories/test/explore?resource=%22a+b=c%22'
    });
    harness.loadExploreScript();
    harness.workbench.explore.mount(harness.document.body);
    assert.equal(harness.document.getElementById('resource').value, '"a b=c"');
});

test('A10: Create keeps a "+" in the id and title from its URL', () => {
    const harness = createFormBrowserHarness({
        href: 'http://localhost:8080/rdf4j-workbench/create?id=a%2Bb&title=C%2B%2B+store'
    });
    const form = harness.registerElement('form', { attributes: { action: 'create' } });
    const id = harness.registerElement('input', { id: 'id', attributes: { 'data-field-role': 'repository-id' } });
    const title = harness.registerElement('input', { id: 'title', attributes: { 'data-field-role': 'repository-title' } });
    const button = harness.registerElement('input', { id: 'create', type: 'submit' });
    form.appendChild(id);
    form.appendChild(title);
    form.appendChild(button);
    harness.document.body.appendChild(form);
    harness.loadScripts(['create.js']);
    harness.workbench.create.mount(harness.document.body);
    assert.equal(id.value, 'a+b');
    assert.equal(title.value, 'C++ store');
});

test('A11: a large Explore page shows its terms like the grouped view (IRIs, literals, datatypes, default graph)', () => {
    const wb = loadViews();
    const rows = [];
    for (let index = 0; index < 90; index++) {
        rows.push([iri('urn:s' + index), iri('urn:p'),
            { kind: 'literal', value: '2026-10-' + String(index % 28 + 1).padStart(2, '0'),
                datatype: 'http://www.w3.org/2001/XMLSchema#date' },
            index % 2 ? iri('urn:g') : null]);
    }
    const model = { viewId: 'explore', vars, rows, rowCount: rows.length, metadata: { 'explore-resource': '<urn:p>' } };
    const markup = flat(quiet(() => wb.views.pageTemplate(model, ctx, runtime())));
    const table = markup.slice(markup.indexOf('<table'), markup.indexOf('</table>'));
    assert.match(table, /class="rdf-datatype"/, 'datatypes are tagged so "Show datatypes" applies');
    assert.match(table, /Default graph/, 'a statement in the default graph says so');
    assert.match(table, /class="resource"/, 'cells are formatted like query results');
});

test('A12: Explore says a page may be truncated when it is as long as the limit in use', () => {
    const wb = loadViews();
    const rows = Array.from({ length: 100 }, (_, index) => [iri('urn:s'), iri('urn:p' + index), iri('urn:o'), null]);
    const model = { viewId: 'explore', vars, rows, rowCount: 100,
        metadata: { 'explore-resource': '<urn:s>', 'total-result-count': 250 } };
    assert.match(flat(quiet(() => wb.views.pageTemplate(model, ctx, runtime()))), /id="result-limited"/);
});

test('A12: an Explore page shorter than the limit in use is not called truncated', () => {
    const wb = loadViews('?resource=%3Curn%3As%3E&limit_explore=200');
    const rows = Array.from({ length: 100 }, (_, index) => [iri('urn:s'), iri('urn:p' + index), iri('urn:o'), null]);
    const model = { viewId: 'explore', vars, rows, rowCount: 100,
        metadata: { 'explore-resource': '<urn:s>', 'total-result-count': 100 } };
    assert.doesNotMatch(flat(quiet(() => wb.views.pageTemplate(model, ctx, runtime()))), /id="result-limited"/);
});

test('A27: a rejected Explore shows its error and the resource field, without a result table or paging', () => {
    const wb = loadViews('?resource=foo%3Abar');
    const model = { viewId: 'explore', vars: ['error-message'],
        rows: [[{ kind: 'literal', value: 'Undefined prefix: foo' }]], rowCount: 1, metadata: {} };
    const markup = flat(quiet(() => wb.views.pageTemplate(model, ctx, runtime())));
    assert.match(markup, /workbench-callout--error/);
    assert.match(markup, /Undefined prefix: foo/);
    assert.match(markup, /id="explore-form"/, 'the resource can be corrected');
    assert.doesNotMatch(markup, /<table/, 'the error row is not a result');
    assert.doesNotMatch(markup, /id="explore-pagination"[^>]*\?hidden=false/, 'no paging for it');
});
