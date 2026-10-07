// Review fixes (TS-A): pages hidden by the Workbench policy are not linked, and read-only repositories offer no writes.
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { installDetailDisclosureTemplateRuntime } = require('./workbench-detail-disclosure-runtime.js');

const scripts = process.env.WORKBENCH_SCRIPT_DIR
    || path.resolve(__dirname, '../../tools/workbench/src/main/webapp/scripts');

function loadViews() {
    const window = { location: { href: 'http://x.test/workbench/repositories/r/summary', search: '' }, fetch() {} };
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

const allPages = ['summary', 'query', 'saved-queries', 'explore', 'namespaces', 'contexts', 'types', 'add', 'export',
    'update', 'remove', 'clear', 'repositories', 'create', 'delete', 'server', 'information'];

/** A shell context whose policy-filtered menu has every page except the hidden ones. */
function context(hidden, extra = {}) {
    const items = allPages.filter((id) => hidden.indexOf(id) < 0).map((id) => ({ id, label: id, href: id }));
    return { basePath: '/workbench', repositoryId: extra.repositoryId || 'r',
        workbench: Object.assign({ menu: [{ id: 'all', label: 'All', items }] }, extra.workbench || {}) };
}

function page(wb, model, ctx) {
    return flat(wb.views.pageTemplate(Object.assign({ vars: [], rows: [], rowCount: 0, metadata: {} }, model), ctx, runtime()));
}

const literal = (value) => ({ kind: 'literal', value });
const repositories = { viewId: 'repositories', vars: ['id', 'description', 'location', 'readable', 'writeable'],
    rows: [[literal('r1'), literal('One'), literal('http://x/r1'), literal('true'), literal('true')]], rowCount: 1 };

test('A25: the repository list links no page the policy hides', () => {
    const wb = loadViews();
    const markup = page(wb, repositories, context(['query', 'explore', 'create', 'delete', 'summary'], { repositoryId: 'NONE' }));
    assert.doesNotMatch(markup, /\/r1\/query"/, 'no Query item');
    assert.doesNotMatch(markup, /\/r1\/explore"/, 'no Explore item');
    assert.doesNotMatch(markup, /\/r1\/summary"/, 'neither the Summary item nor the id link lead to a hidden Summary');
    assert.doesNotMatch(markup, /NONE\/delete\?id=r1/, 'no Delete… item');
    assert.doesNotMatch(markup, /Create repository/, 'no Create repository action');
    assert.match(markup, /\/r1\/namespaces"/, 'the id leads to the first page the repository has');
});

test('A25: with every page available the repository list links them all', () => {
    const wb = loadViews();
    const markup = page(wb, repositories, context([], { repositoryId: 'NONE' }));
    for (const route of ['query', 'explore', 'summary']) {
        assert.match(markup, new RegExp('/r1/' + route + '"'), route);
    }
    assert.match(markup, /NONE\/delete\?id=r1/);
    assert.match(markup, /Create repository/);
});

test('A25: the context bar does not offer Create repository when the policy hides it', () => {
    const wb = loadViews();
    const markup = page(wb, { viewId: 'summary' }, context(['create']));
    assert.doesNotMatch(markup, /NONE\/create[">]/);
    const visible = page(wb, { viewId: 'summary' }, context([]));
    assert.match(visible, /NONE\/create[">]/);
});

/** The repository switcher's panel, from its opening tag to the menu button that follows the switcher. */
function repositoryPopover(markup) {
    const start = markup.indexOf('id="workbench-repository-popover"');
    const end = markup.indexOf('id="workbench-menu-button"', start);
    assert.ok(start >= 0 && end > start, 'the context bar renders the repository switcher panel');
    return markup.slice(start, end);
}

test('the repository switcher panel names no server or user and links no server page', () => {
    const wb = loadViews();
    const popover = repositoryPopover(page(wb, { viewId: 'summary' }, context([])));
    assert.doesNotMatch(popover, /workbench-popover__server/);
    assert.doesNotMatch(popover, /Not signed in/);
    assert.doesNotMatch(popover, /Change server or user/);
    assert.doesNotMatch(popover, /NONE\/server/);
    assert.match(popover, /All repositories/);
    assert.match(popover, /Create repository/);
});

test('A25: Graphs offers Clear graph… only when the Clear page is available', () => {
    const wb = loadViews();
    const graphs = { viewId: 'contexts', vars: ['context', 'statements'],
        rows: [[{ kind: 'iri', value: 'urn:g' }, null]], rowCount: 1 };
    assert.doesNotMatch(page(wb, graphs, context(['clear'])), /clear\?context=/);
    assert.match(page(wb, graphs, context([])), /clear\?context=/);
});

test('A25: Explore offers "Query this resource" only when the Query page is available', () => {
    const wb = loadViews();
    const explore = { viewId: 'explore', vars: ['subject', 'predicate', 'object', 'context'], rows: [],
        metadata: { 'explore-resource': '<urn:s>' } };
    assert.doesNotMatch(page(wb, explore, context(['query'])), /Query this resource/);
    assert.match(page(wb, explore, context([])), /Query this resource/);
});

test('A25: the repository-not-found page offers no hidden Change server page', () => {
    const wb = loadViews();
    const missing = { viewId: 'summary', error: { status: 404, code: 'repository-not-found', message: 'missing' } };
    assert.doesNotMatch(page(wb, missing, context(['server'])), />Change server</);
    assert.match(page(wb, missing, context([])), />Change server</);
});

test('A25: switching repository from a page outside the repository leads to its first available page', () => {
    const wb = loadViews();
    const markup = page(wb, { viewId: 'information' }, context(['summary']));
    assert.match(markup, /data-workbench-switch-fallback=?"?query/, 'Summary is hidden, so Query is the landing page');
});

test('A28: Graphs lists the default graph, and asks for its count, when there are no named graphs', async () => {
    const wb = loadViews();
    const model = { viewId: 'contexts', vars: ['context'], rows: [], rowCount: 0, metadata: {},
        rowStore: { read: () => Promise.resolve([]), dispose() {} } };
    const markup = page(wb, model, context([]));
    assert.match(markup, /workbench-browse-default-row/, 'the default graph row is shown');
    assert.match(markup, /No named graphs/);

    const requests = [];
    wb.app = { loadModel: (fetcher, url) => {
        requests.push(url);
        return Promise.resolve({ vars: ['context', 'statements'], rowCount: 1, metadata: {},
            rowStore: { read: () => Promise.resolve([[null, { kind: 'literal', value: '12' }]]), dispose() {} } });
    } };
    const mount = { querySelectorAll: () => [], contains: () => true };
    const quiet = console.error;
    console.error = () => {};
    try {
        const dispose = await wb.views.bindRowWindows(mount, model, context([]), runtime());
        await new Promise(setImmediate);
        assert.equal(requests.length, 1, 'the counts are asked for');
        assert.match(requests[0], /counts=true/);
        assert.equal(model.browseCounts.state, 'done');
        assert.equal(model.browseCounts.values[''].value, '12', 'the default graph gets its count');
        if (typeof dispose === 'function') { dispose(); }
    } finally {
        console.error = quiet;
    }
});

const namespacesModel = () => ({ viewId: 'namespaces', vars: ['prefix', 'namespace'],
    rows: [[literal('ex'), literal('http://example.org/')], [literal('foaf'), literal('http://xmlns.com/foaf/0.1/')]],
    rowCount: 2, metadata: {} });

test('A29: a read-only repository offers no namespace changes and no Clear graph…', () => {
    const wb = loadViews();
    const readOnly = context([], { workbench: { writeable: false } });
    const namespaces = page(wb, namespacesModel(), readOnly);
    assert.doesNotMatch(namespaces, /Add namespace/);
    assert.doesNotMatch(namespaces, /aria-label=Edit |title="Edit"/);
    assert.doesNotMatch(namespaces, /workbench-namespace-delete/);
    const graphs = { viewId: 'contexts', vars: ['context', 'statements'], rows: [[{ kind: 'iri', value: 'urn:g' }, null]], rowCount: 1 };
    assert.doesNotMatch(page(wb, graphs, readOnly), /clear\?context=/);

    const writeable = context([], { workbench: { writeable: true } });
    assert.match(page(wb, namespacesModel(), writeable), /Add namespace/);
    assert.match(page(wb, namespacesModel(), writeable), /workbench-namespace-delete/);
    assert.match(page(wb, graphs, writeable), /clear\?context=/);
});

function handlersOf(template, out = []) {
    if (Array.isArray(template)) { template.forEach((item) => handlersOf(item, out)); return out; }
    if (template && Array.isArray(template.strings)) {
        template.values.forEach((value, index) => {
            if (typeof value === 'function') {
                out.push({ before: template.strings.slice(0, index + 1).join('').slice(-160), fn: value });
            } else { handlersOf(value, out); }
        });
    }
    return out;
}

test('A31: renaming a prefix onto one that exists is refused in the editor, which says why', () => {
    const wb = loadViews();
    const submitted = [];
    wb.submitForm = (form) => submitted.push(form);
    const model = namespacesModel();
    const ctx = context([], { workbench: { writeable: true } });
    const edit = handlersOf(wb.views.pageTemplate(model, ctx, runtime()))
        .filter((entry) => /title="Edit" @click=$/.test(entry.before))[0].fn;
    const button = { closest: () => null };
    edit({ currentTarget: button });
    const save = handlersOf(wb.views.pageTemplate(model, ctx, runtime()))
        .filter((entry) => /aria-label="Save" title="Save"\s*@click=$/.test(entry.before))[0].fn;
    const inputs = [{ value: 'foaf' }, { value: 'http://example.org/' }];
    const document = { createElement: () => ({ appendChild() {} }), body: { appendChild() {} } };
    save({ currentTarget: { ownerDocument: document, closest: (selector) => (selector === 'tr' ? { querySelectorAll: () => inputs } : null) } });
    assert.equal(submitted.length, 0, 'nothing replaces the existing foaf binding');
    const markup = page(wb, model, ctx);
    assert.match(markup, /Prefix 'foaf' is already defined/);
    assert.match(markup, /workbench-namespace-edit/, 'the editor stays open with the typed values');
});

// Round 2 (R7): an Info answer whose menu is empty (a user the server does not authorize, a failed Info) says nothing
// about hidden pages: the shell keeps the links that lead to the other repositories, and a missing repository's page
// still offers Change server.
test('an empty Info menu hides no recovery link', () => {
    const wb = loadViews();
    const empty = { basePath: '/workbench', repositoryId: 'r', workbench: { menu: [] } };
    const markup = page(wb, { viewId: 'summary' }, empty);
    assert.match(markup, /All repositories/);
    assert.match(markup, /Create repository/);
    const missing = { viewId: 'summary', error: { status: 404, code: 'repository-not-found', message: 'missing' } };
    assert.match(page(wb, missing, empty), />Change server</, 'the not-found page offers Change server');
});

/** The markup between an element's opening id and the next marker, to look at one region of the shell. */
function region(markup, startMarker, endMarker) {
    const start = markup.indexOf(startMarker);
    const end = markup.indexOf(endMarker, start);
    assert.ok(start >= 0 && end > start, startMarker + ' is rendered');
    return markup.slice(start, end);
}

// Connection is how a user the server refuses signs in: without a menu from the Info answer (a refused user, a failed
// Info) the sidebar and the phone menu still offer it.
for (const [name, info] of [['an empty Info menu', { menu: [] }], ['no Info answer', {}]]) {
    test(`with ${name} the sidebar and the phone menu still offer Connection`, () => {
        const wb = loadViews();
        for (const ctx of [{ basePath: '/workbench', repositoryId: 'r', workbench: info },
            { basePath: '/workbench', repositoryId: 'NONE', workbench: info }]) {
            const markup = page(wb, { viewId: 'summary' }, ctx);
            for (const nav of [region(markup, 'id="navigation"', '</nav>'),
                region(markup, 'id="workbench-menu-sheet-nav"', '</nav>')]) {
                assert.match(nav, /href=\/workbench\/repositories\/NONE\/server data-workbench-nav-href=/, ctx.repositoryId);
                assert.match(nav, /Connection\s*<\/a>/, ctx.repositoryId);
            }
        }
    });
}

test('a menu from the Info answer is shown as it is, without an added Connection', () => {
    const wb = loadViews();
    const markup = page(wb, { viewId: 'summary' }, context(['server']));
    assert.doesNotMatch(region(markup, 'id="navigation"', '</nav>'), /NONE\/server/);
});

// A Workbench whose server is fixed by configuration has no Connection page: nothing links to one, not even the menu
// a shell falls back to without an Info answer.
test('a Workbench whose server is fixed offers Connection nowhere', () => {
    const wb = loadViews();
    const fixed = (info) => ({ basePath: '/workbench', repositoryId: 'r', serverFixed: true, workbench: info });
    const listed = context([]).workbench;
    for (const info of [listed, { menu: [] }, {}]) {
        const markup = page(wb, { viewId: 'summary' }, fixed(info));
        assert.doesNotMatch(markup, /NONE\/server/, JSON.stringify(Object.keys(info)));
    }
    const missing = { viewId: 'summary', error: { status: 404, code: 'repository-not-found', message: 'missing' } };
    assert.doesNotMatch(page(wb, missing, fixed({ menu: [] })), />Change server</);
});
