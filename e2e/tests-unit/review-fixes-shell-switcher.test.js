// Review fixes (TS-A): the repository switcher lists the repositories the server has now.
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { installDetailDisclosureTemplateRuntime } = require('./workbench-detail-disclosure-runtime.js');

const scripts = process.env.WORKBENCH_SCRIPT_DIR
    || path.resolve(__dirname, '../../tools/workbench/src/main/webapp/scripts');

function node(tagName, attributes = {}) {
    return {
        tagName, className: '', hidden: false, textContent: '', parentElement: null, children: [],
        attributes: Object.assign({}, attributes),
        get firstChild() { return this.children[0] || null; },
        appendChild(child) { child.parentElement = this; this.children.push(child); return child; },
        removeChild(child) { this.children = this.children.filter((item) => item !== child); child.parentElement = null; },
        setAttribute(name, value) { this.attributes[name] = String(value); },
        getAttribute(name) { return Object.prototype.hasOwnProperty.call(this.attributes, name) ? this.attributes[name] : null; },
        addEventListener() {},
        removeEventListener() {},
        focus() {}
    };
}

function switcher() {
    const document = { createElement: (tagName) => node(tagName) };
    const list = node('ul', { id: 'workbench-repository-options' });
    const status = node('p');
    const filter = node('input', { id: 'workbench-repository-filter' });
    filter.value = '';
    const panel = node('div', { id: 'workbench-repository-popover', 'data-workbench-base-path': '/workbench',
        'data-workbench-active-view': 'summary', 'data-workbench-repositories-url': '/workbench/repositories/NONE/repositories' });
    panel.ownerDocument = document;
    panel.querySelector = (selector) => ({ '#workbench-repository-options': list, '.workbench-popover__status': status,
        '#workbench-repository-filter': filter })[selector] || null;
    panel.querySelectorAll = (selector) => selector === 'a.workbench-popover__option'
        ? list.children.map((item) => item.children[0]) : [];
    const elements = { 'workbench-repository-popover': panel, 'workbench-repository-filter': filter,
        'workbench-repository-switcher': node('button'), 'workbench-server-switcher': node('button'),
        'workbench-server-popover': node('div') };
    document.getElementById = (id) => elements[id] || null;
    return { document, panel, list, ids: () => list.children.map((item) => item.children[0].getAttribute('data-repository-id')) };
}

function load(serverRepositories) {
    const window = { location: { href: 'http://x.test/workbench/repositories/a/summary', search: '' }, fetch() {} };
    const workbench = {};
    installDetailDisclosureTemplateRuntime(workbench);
    const context = vm.createContext({ console, URL, URLSearchParams, Promise, window, workbench, setTimeout });
    vm.runInContext(fs.readFileSync(path.join(scripts, 'workbenchViews.js'), 'utf8'), context,
        { filename: 'workbenchViews.js' });
    const wb = context.workbench;
    const opens = [];
    wb.popover = { bind: (button, panel, options) => { if (options && options.onOpen) { opens.push(options.onOpen); } return () => {}; } };
    let requests = 0;
    wb.app = { loadModel: () => {
        requests++;
        const rows = serverRepositories.map((id) => [{ kind: 'literal', value: id }, { kind: 'literal', value: id + ' store' }]);
        return Promise.resolve({ vars: ['id', 'description'], rowCount: rows.length,
            rowStore: { read: () => Promise.resolve(rows), dispose() {} } });
    } };
    return { wb, opens, requests: () => requests };
}

const settle = () => new Promise(setImmediate);

test('A14: a repository created or deleted in the page is listed (or not) the next time the switcher opens', async () => {
    const server = ['a', 'b'];
    const loaded = load(server);
    const shell = switcher();
    loaded.wb.views.bindContextBar({ ownerDocument: shell.document }, { basePath: '/workbench', repositoryId: 'a', workbench: {} });
    assert.equal(loaded.opens.length, 1);
    loaded.opens[0](shell.panel);
    await settle();
    assert.deepEqual(shell.ids(), ['a', 'b']);

    server.push('c');
    server.splice(server.indexOf('b'), 1);
    loaded.opens[0](shell.panel);
    assert.deepEqual(shell.ids(), ['a', 'b'], 'the list known so far is shown at once');
    await settle();
    assert.deepEqual(shell.ids(), ['a', 'c'], 'then the current list replaces it');
});

test('A14: opening the switcher twice while its list loads asks the server once', async () => {
    const loaded = load(['a']);
    const shell = switcher();
    loaded.wb.views.bindContextBar({ ownerDocument: shell.document }, { basePath: '/workbench', repositoryId: 'a', workbench: {} });
    loaded.opens[0](shell.panel);
    loaded.opens[0](shell.panel);
    await settle();
    assert.equal(loaded.requests(), 1);
    assert.deepEqual(shell.ids(), ['a']);
});

function bindingAfter(template, pattern) {
    if (Array.isArray(template)) {
        for (const item of template) { const found = bindingAfter(item, pattern); if (found) { return found; } }
        return null;
    }
    if (template && Array.isArray(template.strings)) {
        for (let index = 0; index < template.values.length; index++) {
            if (pattern.test(template.strings[index])) { return { before: template.strings[index], value: template.values[index] }; }
            const found = bindingAfter(template.values[index], pattern);
            if (found) { return found; }
        }
    }
    return null;
}

test('A15: the Types/Graphs filter shows each page\'s own filter, but keeps what is typed while that page re-renders', () => {
    const loaded = load([]);
    const views = loaded.wb.views;
    const html = (strings, ...values) => ({ strings: Array.from(strings), values });
    const runtime = { nothing: '', html, render() {} };
    const ctx = { basePath: '/workbench', repositoryId: 'r', workbench: {} };
    const quiet = console.error;
    console.error = () => {};
    try {
        const filtered = { viewId: 'types', vars: ['type', 'instances'], rows: [], rowCount: 0, metadata: { filter: 'foo' } };
        const first = bindingAfter(views.pageTemplate(filtered, ctx, runtime), /name="filter"\s+\.?value=$/);
        assert.ok(first, 'the filter is bound');
        // lit sets a property binding on every render (an object value always counts as changed), which would clear
        // what is typed whenever the page renders again; the attribute only gives a field that was not typed into
        // its value.
        assert.match(first.before, /\svalue=$/, 'an attribute binding');
        assert.equal(first.value, 'foo');
        // A page opened in place may keep the previous page's field: mounting the page sets it to its own filter.
        const field = { value: 'typed on the page before' };
        const outlet = { querySelector: (selector) => selector === '#types-filter' ? field : null,
            querySelectorAll: () => [] };
        const unfiltered = { viewId: 'types', vars: ['type', 'instances'], rows: [], rowCount: 0, metadata: {},
            rowStore: { dispose() {} } };
        views.bindRowWindows(outlet, unfiltered, ctx, runtime);
        assert.equal(field.value, '', 'the page opened shows its own (empty) filter');
        views.bindRowWindows(outlet, filtered, ctx, runtime);
        assert.equal(field.value, 'foo');
    } finally {
        console.error = quiet;
    }
});

function fakeButton(action) {
    const button = { attributes: { 'data-workbench-window-action': action }, listeners: [],
        getAttribute(name) { return this.attributes[name] || null; },
        addEventListener(type, listener) { if (type === 'click') { this.listeners.push(listener); } },
        removeEventListener(type, listener) { this.listeners = this.listeners.filter((item) => item !== listener); },
        click() { this.listeners.slice().forEach((listener) => listener({ preventDefault() {} })); } };
    return button;
}

test('A16: the Delete picker pages again after another visit reuses its Previous/Next buttons', async () => {
    const loaded = load([]);
    const views = loaded.wb.views;
    const runtime = { nothing: '', html: (strings, ...values) => ({ strings: Array.from(strings), values }), render() {} };
    const ctx = { basePath: '/workbench', repositoryId: 'NONE', workbench: {} };
    const next = fakeButton('next');
    const previous = fakeButton('previous');
    const mount = { querySelectorAll(selector) {
        if (selector === '[data-workbench-window-action]') { return [previous, next]; }
        if (selector === '[data-workbench-window-picker]') { return [{}]; }
        return [];
    }, contains: () => true };
    const model = () => {
        const rows = Array.from({ length: 120 }, (_, index) => [{ kind: 'literal', value: 'repo-' + index }]);
        return { viewId: 'delete', vars: ['id'], rows: [], rowCount: rows.length, metadata: {}, pickerPageSize: 50,
            rowStore: { read: (start, count) => Promise.resolve(rows.slice(start, start + count)), dispose() {} } };
    };
    const quiet = console.error;
    console.error = () => {};
    try {
        const first = model();
        const disposeFirst = await views.bindRowWindows(mount, first, ctx, runtime);
        next.click();
        await settle();
        assert.equal(first.pickerStart, 50, 'Next pages the first visit');
        disposeFirst();
        assert.equal(next.listeners.length, 0, 'a disposed page leaves no listener on the buttons');

        const second = model();
        const disposeSecond = await views.bindRowWindows(mount, second, ctx, runtime);
        next.click();
        await settle();
        assert.equal(second.pickerStart, 50, 'Next pages the second visit through the same button');
        disposeSecond();
    } finally {
        console.error = quiet;
    }
});
