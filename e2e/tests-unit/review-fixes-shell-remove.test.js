// Review fixes (TS-A): the Remove and Clear pages must only remove what their count and dialog name.
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { installDetailDisclosureTemplateRuntime } = require('./workbench-detail-disclosure-runtime.js');

const scripts = process.env.WORKBENCH_SCRIPT_DIR
    || path.resolve(__dirname, '../../tools/workbench/src/main/webapp/scripts');

/** The views with a timer queue the test runs by hand and the given browser location search. */
function loadViews(search) {
    const timers = [];
    const window = { location: { href: 'http://x.test/workbench/repositories/r/remove' + (search || ''),
        search: search || '' } };
    const workbench = {};
    installDetailDisclosureTemplateRuntime(workbench);
    const context = vm.createContext({
        console, URL, URLSearchParams, Promise, window, workbench, AbortController,
        setTimeout: (fn, ms) => { timers.push({ fn, ms }); return timers.length; },
        clearTimeout: (id) => { if (id && timers[id - 1]) { timers[id - 1].cancelled = true; } }
    });
    const quiet = console.error;
    console.error = () => {};
    try {
        for (const filename of ['workbenchViews.js', 'workbenchRoutes.js', 'queryStream.js', 'workbenchApp.js']) {
            vm.runInContext(fs.readFileSync(path.join(scripts, filename), 'utf8'), context, { filename });
        }
    } finally {
        console.error = quiet;
    }
    const wb = context.workbench;
    return { wb, timers, runTimers() {
        const due = timers.filter((timer) => !timer.cancelled && !timer.ran);
        due.forEach((timer) => { timer.ran = true; timer.fn(); });
        return due.length;
    } };
}

function runtime() {
    function html(strings, ...values) { return { strings: Array.from(strings), values }; }
    return { html, render() {} };
}

function flat(template) {
    if (Array.isArray(template)) { return template.map(flat).join(''); }
    if (template && Array.isArray(template.strings)) {
        return template.strings.map((part, index) => part
            + (index < template.values.length ? flat(template.values[index]) : '')).join('');
    }
    return template == null || typeof template === 'function' ? '' : String(template);
}

function handlers(template, out = []) {
    if (Array.isArray(template)) { template.forEach((item) => handlers(item, out)); return out; }
    if (template && Array.isArray(template.strings)) {
        template.values.forEach((value, index) => {
            if (typeof value === 'function') { out.push({ before: template.strings[index].slice(-40), fn: value }); }
            else { handlers(value, out); }
        });
    }
    return out;
}

const ctx = { basePath: '/workbench', repositoryId: 'r', workbench: {} };
const quietRender = (wb, model) => {
    const quiet = console.error;
    console.error = () => {};
    try { return wb.views.pageTemplate(model, ctx, runtime()); } finally { console.error = quiet; }
};

function removePage(views, model) {
    const template = quietRender(views.wb, model);
    const found = handlers(template);
    return {
        markup: flat(template),
        submit: found.find((entry) => /@submit=$/.test(entry.before)).fn,
        input: found.find((entry) => /@input=$/.test(entry.before) && entry.fn.toString().includes('recount')).fn
    };
}

function removeButton(markup) {
    const at = markup.indexOf('type="submit"');
    return markup.slice(at, markup.indexOf('</button>', at)).replace(/\s+/g, ' ');
}

function fakeForm(values) {
    const controls = {};
    Object.keys(values).forEach((name) => { controls[name] = { value: values[name] }; });
    return { controls, isConnected: true, closest: () => null,
        querySelector: (selector) => controls[/name="(\w+)"/.exec(selector)[1]] || null };
}

/** A count answer whose single row is released by the test. */
function countingApp(views) {
    const requests = [];
    views.wb.app = {
        loadModel(_fetcher, url) {
            const request = { url, release: null };
            requests.push(request);
            return Promise.resolve({ metadata: {}, rowCount: 1, rowStore: {
                read: () => new Promise((resolve) => {
                    request.release = (count) => resolve([[{ kind: 'literal', value: String(count) }]]);
                }),
                dispose() {}
            } });
        }
    };
    return requests;
}

const settle = () => new Promise(setImmediate);

test('A1: changing a Remove value disables Remove at once and the old count can never be confirmed', async () => {
    const views = loadViews();
    const requests = countingApp(views);
    const model = { viewId: 'remove', vars: ['context'], rows: [], rowCount: 0, metadata: {} };
    const form = fakeForm({ subj: '<urn:s>', pred: '<urn:p>', obj: '', context: '' });
    const first = removePage(views, model);
    first.input({ currentTarget: { form } });
    views.runTimers();
    await settle();
    requests[0].release(3);
    await settle();
    assert.equal(model.removeCount.state, 'counted');
    assert.match(removeButton(removePage(views, model).markup), /\?disabled=false/);
    assert.match(removeButton(removePage(views, model).markup), /Remove 3 statements/);

    // The user clears Subject: Remove would now remove every statement with that predicate.
    const page = removePage(views, model);
    form.controls.subj.value = '';
    page.input({ currentTarget: { form } });
    assert.notEqual(model.removeCount.state, 'counted', 'the count of the old values no longer applies');
    assert.match(removeButton(removePage(views, model).markup), /\?disabled=true/,
        'Remove waits for the count of the values now in the form');

    const dialogs = [];
    const posted = [];
    views.wb.confirmDialog = { open: (options) => { dialogs.push(options); return Promise.resolve(true); } };
    views.wb.router = { send: () => { posted.push(Object.assign({}, form.controls)); return new Promise(() => {}); } };
    // The handler captured while the old count was shown still checks the live state.
    page.submit({ preventDefault() {}, currentTarget: form });
    await settle();
    assert.equal(dialogs.length, 0, 'no dialog promises the old count');
    assert.equal(posted.length, 0);

    views.runTimers();
    await settle();
    requests[1].release(7);
    await settle();
    assert.equal(model.removeCount.state, 'counted');
    removePage(views, model).submit({ preventDefault() {}, currentTarget: form });
    await settle();
    assert.equal(dialogs.length, 1);
    assert.equal(dialogs[0].title, 'Remove 7 statements?');
    assert.match(dialogs[0].body, /every graph/, 'the dialog names the graph it removes from');
    assert.equal(posted.length, 1);
});

test('A1: values changed without an input event are counted again before Remove can be confirmed', async () => {
    const views = loadViews();
    const requests = countingApp(views);
    const model = { viewId: 'remove', vars: ['context'], rows: [], rowCount: 0, metadata: {} };
    const form = fakeForm({ subj: '<urn:s>', pred: '', obj: '', context: '<http://example/g>' });
    removePage(views, model).input({ currentTarget: { form } });
    views.runTimers();
    await settle();
    requests[0].release(2);
    await settle();
    const dialogs = [];
    views.wb.confirmDialog = { open: (options) => { dialogs.push(options); return Promise.resolve(false); } };
    const page = removePage(views, model);
    page.submit({ preventDefault() {}, currentTarget: form });
    await settle();
    assert.equal(dialogs.length, 1);
    assert.match(dialogs[0].body, /graph http:\/\/example\/g/, 'a named graph is named in the dialog');

    form.controls.context.value = '';
    page.submit({ preventDefault() {}, currentTarget: form });
    await settle();
    assert.equal(dialogs.length, 1, 'values that differ from the counted ones are not confirmed');
    assert.notEqual(model.removeCount.state, 'counted');
});

test('A3: a count read that finishes after the values changed does not overwrite the newer state', async () => {
    const views = loadViews();
    const requests = countingApp(views);
    const model = { viewId: 'remove', vars: ['context'], rows: [], rowCount: 0, metadata: {} };
    const form = fakeForm({ subj: '<urn:s>', pred: '', obj: '', context: '' });
    const page = removePage(views, model);
    page.input({ currentTarget: { form } });
    views.runTimers();
    await settle();
    assert.equal(model.removeCount.state, 'counting');
    form.controls.subj.value = '';
    page.input({ currentTarget: { form } });
    assert.equal(model.removeCount.state, 'empty');
    requests[0].release(3);
    await settle();
    assert.equal(model.removeCount.state, 'empty', 'the stale read is dropped');
    assert.match(removeButton(removePage(views, model).markup), /\?disabled=true/);
});

test('A3: a preview read that finishes after the values changed stays hidden', async () => {
    const views = loadViews();
    let release;
    views.wb.app = { loadModel: () => Promise.resolve({ metadata: {}, rowCount: 1, rowStore: {
        read: () => new Promise((resolve) => { release = resolve; }), dispose() {} } }) };
    const model = { viewId: 'remove', vars: ['context'], rows: [], rowCount: 0, metadata: {} };
    const form = fakeForm({ subj: '<urn:s>', pred: '', obj: '', context: '' });
    const template = quietRender(views.wb, model);
    const preview = handlers(template).find((entry) => /@click=$/.test(entry.before)
        && entry.fn.toString().includes('loadPreview')).fn;
    preview({ currentTarget: { form } });
    await settle();
    assert.equal(model.removePreview.state, 'loading');
    removePage(views, model).input({ currentTarget: { form } });
    assert.equal(model.removePreview.state, 'hidden');
    release([[{ kind: 'iri', value: 'urn:s' }, { kind: 'iri', value: 'urn:p' }, { kind: 'iri', value: 'urn:o' }, null]]);
    await settle();
    assert.equal(model.removePreview.state, 'hidden', 'the preview of the old values is not shown');
});

function selectOf(markup) {
    const start = markup.indexOf('<select id="context"');
    return markup.slice(start, markup.indexOf('</select>', start)).replace(/\s+/g, ' ');
}

function selectedOption(select) {
    const match = /<option value=(?:"([^"]*)"|([^ ]*)) (?:\?disabled=\w+ )?\?selected=true>/.exec(select);
    return match ? (typeof match[1] === 'string' ? match[1] : match[2]) : null;
}

const literal = (value) => ({ kind: 'literal', value });

test('A2: a rejected Remove keeps the posted graph selected instead of falling back to Any graph', () => {
    const views = loadViews();
    const markup = flat(quietRender(views.wb, { viewId: 'remove', vars: ['error-message', 'subj', 'pred', 'obj', 'context'],
        rows: [[literal('Undefined prefix'), literal('<http://s>'), literal('ex:p'), null, literal('<http://example/g>')]],
        rowCount: 1, metadata: {} }));
    assert.equal(selectedOption(selectOf(markup)), '<http://example/g>');
});

test('A2: a rejected Remove from the default graph keeps Default graph selected', () => {
    const views = loadViews();
    const markup = flat(quietRender(views.wb, { viewId: 'remove', vars: ['error-message', 'subj', 'pred', 'obj', 'context'],
        rows: [[literal('Undefined prefix'), literal('<http://s>'), null, null, literal('null')]], rowCount: 1, metadata: {} }));
    assert.equal(selectedOption(selectOf(markup)), 'null');
});

test('A2: opening Remove does not preselect the first graph of the listing', () => {
    const views = loadViews();
    const markup = flat(quietRender(views.wb, { viewId: 'remove', vars: ['context'],
        rows: [[{ kind: 'bnode', value: 'b0' }], [{ kind: 'iri', value: 'http://example/g' }]], rowCount: 2, metadata: {} }));
    assert.equal(selectedOption(selectOf(markup)), '', 'Any graph stays the choice');
});

test('A2: opening Remove with ?context= preselects that graph', () => {
    const views = loadViews('?context=' + encodeURIComponent('<http://example/g>'));
    const markup = flat(quietRender(views.wb, { viewId: 'remove', vars: ['context'],
        rows: [[{ kind: 'bnode', value: 'b0' }], [{ kind: 'iri', value: 'http://example/g' }]], rowCount: 2, metadata: {} }));
    assert.equal(selectedOption(selectOf(markup)), '<http://example/g>');
});

test('A2: opening Clear does not preselect the first graph of the listing', () => {
    const views = loadViews();
    const markup = flat(quietRender(views.wb, { viewId: 'clear', vars: ['context', 'statements'],
        rows: [[{ kind: 'bnode', value: 'genid-abc' }, null], [{ kind: 'iri', value: 'http://example/g' }, null], [null, null]],
        rowCount: 3, metadata: { 'context-discovery-complete': true } }));
    assert.equal(selectedOption(selectOf(markup)), '', 'Entire repository stays the choice');
});

test('A2: Clear opened from a graph row keeps that graph selected', () => {
    const views = loadViews('?context=' + encodeURIComponent('<http://example/g>'));
    const markup = flat(quietRender(views.wb, { viewId: 'clear', vars: ['context', 'statements'],
        rows: [[{ kind: 'bnode', value: 'genid-abc' }, null], [{ kind: 'iri', value: 'http://example/g' }, null], [null, null]],
        rowCount: 3, metadata: { 'context-discovery-complete': true } }));
    assert.equal(selectedOption(selectOf(markup)), '<http://example/g>');
});
