// Review fixes (TS-A): starting the Workbench page.
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { installDetailDisclosureTemplateRuntime } = require('./workbench-detail-disclosure-runtime.js');

const scripts = process.env.WORKBENCH_SCRIPT_DIR
    || path.resolve(__dirname, '../../tools/workbench/src/main/webapp/scripts');

function loadWorkbench(sandboxExtras = {}) {
    const window = {};
    const workbench = {};
    installDetailDisclosureTemplateRuntime(workbench);
    const context = vm.createContext(Object.assign({ console, URL, Promise, window, workbench, setTimeout },
        sandboxExtras));
    for (const filename of ['workbenchViews.js', 'workbenchRoutes.js', 'queryStream.js', 'workbenchApp.js']) {
        vm.runInContext(fs.readFileSync(path.join(scripts, filename), 'utf8'), context, { filename });
    }
    window.workbench = context.workbench;
    context.workbench.__testWindow = window;
    return context.workbench;
}

/** A row store and NDJSON reader in memory; cleanup of stored results is what each test makes of it. */
function installStream(workbench, maintain) {
    workbench.queryStream = {
        scheduleRowStoreMaintenance: maintain,
        markCurrentRowStoresForRecovery() {},
        async createRowStore() {
            const rows = [];
            return { async append(batch) { rows.push(...batch); return rows.length; },
                async read(start, count) { return rows.slice(start, start + count); },
                async count() { return rows.length; }, async dispose() {} };
        },
        async consumeNdjsonResponse(response, options) {
            for (const record of response.records) { await options.onRecord(record); }
            return { type: 'end' };
        }
    };
}

function runtime() {
    return { html(strings, ...values) { return { strings: Array.from(strings), values }; }, render() {} };
}

function page(workbench, view) {
    const document = {
        location: { href: 'https://example.test/workbench/repositories/repo-1/' + view },
        body: { classList: { add() {} } },
        documentElement: { setAttribute() {} },
        readyState: 'complete',
        getElementById() { return null; }
    };
    workbench.__testWindow.document = document;
    workbench.__testWindow.localStorage = { getItem() { return null; }, setItem() {} };
    workbench.__testWindow.matchMedia = () => ({ matches: false });
    return {
        ownerDocument: document,
        getAttribute(name) {
            return { 'data-workbench-fetch-page-model': 'true', 'data-workbench-view': view,
                'data-workbench-base-path': '/workbench', 'data-workbench-repository-id': 'repo-1' }[name] || null;
        }
    };
}

const contexts = { ok: true, records: [
    { type: 'head', version: 1 }, { type: 'view', id: 'contexts' }, { type: 'vars', values: ['context'] },
    { type: 'rows', values: [[{ kind: 'iri', value: 'urn:g' }]] }, { type: 'end' }
] };

test('A23: the page starts when stored query results cannot be cleaned up (for example without IndexedDB)', async () => {
    const workbench = loadWorkbench();
    installStream(workbench, () => { throw new Error('IndexedDB is unavailable'); });
    const warnings = [];
    const quiet = { error: console.error, warn: console.warn };
    console.error = (...args) => warnings.push(args);
    console.warn = (...args) => warnings.push(args);
    let result;
    try {
        result = await workbench.app.bootstrap(page(workbench, 'contexts'), {
            fetch: () => Promise.resolve(contexts), runtime: runtime(), skipScripts: true
        });
    } finally {
        console.error = quiet.error;
        console.warn = quiet.warn;
    }
    assert.equal(result.model.viewId, 'contexts', 'the page is shown');
    assert.ok(warnings.some((args) => args.some((arg) => String(arg && arg.message || arg).includes('IndexedDB'))),
        'the console says why nothing was cleaned up');
});

test('A43: reloading a page (F5) brings it back to where it was scrolled, as Back does', async () => {
    const sessionValues = new Map();
    const scrollCalls = [];
    async function openPage(navigationType) {
        const workbench = loadWorkbench();
        installStream(workbench, () => Promise.resolve());
        const pagehide = [];
        const window = workbench.__testWindow;
        window.scrollY = 0;
        window.pageYOffset = 0;
        window.navigation = { currentEntry: { key: 'contexts-entry' } };
        window.performance = { getEntriesByType: (type) => (type === 'navigation' ? [{ type: navigationType }] : []) };
        window.sessionStorage = {
            getItem: (key) => (sessionValues.has(key) ? sessionValues.get(key) : null),
            setItem: (key, value) => sessionValues.set(key, String(value)),
            removeItem: (key) => sessionValues.delete(key)
        };
        window.scrollTo = (x, y) => { scrollCalls.push(y); window.scrollY = y; window.pageYOffset = y; };
        window.addEventListener = (type, listener) => { if (type === 'pagehide') { pagehide.push(listener); } };
        window.removeEventListener = () => {};
        workbench.views.render = () => ({ status: 'rendered' });
        workbench.views.bindRowWindows = () => Promise.resolve(() => {});
        const mount = page(workbench, 'contexts');
        await workbench.app.bootstrap(mount, { fetch: () => Promise.resolve(contexts), runtime: runtime(), skipScripts: true });
        return { window, leave: (y) => { window.scrollY = y; window.pageYOffset = y; pagehide.forEach((listener) => listener({ persisted: false })); } };
    }
    const first = await openPage('navigate');
    assert.deepEqual(scrollCalls, []);
    first.leave(4200);
    await openPage('reload');
    assert.deepEqual(scrollCalls, [4200], 'the reloaded page is where it was left');
});

/** The markup of a template captured from the runtime, values in place. */
function flat(template) {
    if (Array.isArray(template)) { return template.map(flat).join(''); }
    if (template && Array.isArray(template.strings)) {
        return template.strings.map((part, index) => part
            + (index < template.values.length ? flat(template.values[index]) : '')).join('');
    }
    return template == null || typeof template === 'function' ? '' : String(template);
}

/** A runtime that keeps what it renders. */
function capturingRuntime() {
    const rendered = [];
    return { rendered, nothing: '', html(strings, ...values) { return { strings: Array.from(strings), values }; },
        render(template) { rendered.push(flat(template)); } };
}

function pageWithInfo(view, vars, row) {
    return { ok: true, records: [
        { type: 'head', version: 1 }, { type: 'view', id: view }, { type: 'links', values: ['info'] },
        { type: 'vars', values: vars }, { type: 'rows', values: [row] }, { type: 'end' }
    ] };
}

/** Answers the page itself, and fails the Info request the way a server error (500) does. */
function failingInfo(pageAnswer) {
    return (url) => (/\/info(\?|$)/.test(url) ? Promise.resolve({ ok: false, status: 500 }) : Promise.resolve(pageAnswer));
}

// An Info answer that fails for another reason than a refused user (a server error, a dropped connection) leaves the
// page inside the shell, whose menu still offers Connection, instead of a bare error.
test('a page whose Info fails is an error inside the shell, whose menu offers Connection', async () => {
    const workbench = loadWorkbench();
    installStream(workbench, () => Promise.resolve());
    const lit = capturingRuntime();
    const result = await workbench.app.bootstrap(page(workbench, 'summary'), {
        fetch: failingInfo(pageWithInfo('summary', ['id'], [{ kind: 'literal', value: 'repo-1' }])),
        runtime: lit, skipScripts: true
    });
    assert.equal(result.status, 'rendered');
    assert.equal(result.model.error.code, 'page-incomplete');
    const markup = lit.rendered.join('');
    assert.match(markup, /Unable to load this Workbench page\./);
    assert.match(markup, /Unable to load Workbench page data \(500\)/);
    assert.match(markup, /id="navigation"[\s\S]*href=\/workbench\/repositories\/NONE\/server data-workbench-nav-href=/);
});

test('the Connection page is shown in full when its Info fails', async () => {
    const workbench = loadWorkbench();
    installStream(workbench, () => Promise.resolve());
    const lit = capturingRuntime();
    const result = await workbench.app.bootstrap(page(workbench, 'server'), {
        fetch: failingInfo(pageWithInfo('server', ['server'], [{ kind: 'literal', value: 'https://example.test/rdf4j-server' }])),
        runtime: lit, skipScripts: true
    });
    assert.equal(result.status, 'rendered');
    assert.equal(result.model.error, undefined);
    const markup = lit.rendered.join('');
    assert.match(markup, /id="server-form"/);
    assert.match(markup, /value=https:\/\/example\.test\/rdf4j-server/);
    assert.match(markup, /id="navigation"[\s\S]*href=\/workbench\/repositories\/NONE\/server data-workbench-nav-href=/);
});
