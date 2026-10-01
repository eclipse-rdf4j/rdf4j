const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { installDetailDisclosureTemplateRuntime } = require('./workbench-detail-disclosure-runtime.js');

const scripts = process.env.WORKBENCH_SCRIPT_DIR
    || path.resolve(__dirname, '../../tools/workbench/src/main/webapp/scripts');

function loadWorkbench() {
    const window = {};
    const workbench = {};
    installDetailDisclosureTemplateRuntime(workbench);
    const context = vm.createContext({
        console,
        URL,
        Promise,
        window,
        workbench,
        setTimeout
    });
    for (const filename of ['workbenchViews.js', 'queryStream.js', 'workbenchApp.js']) {
        const source = fs.readFileSync(path.join(scripts, filename), 'utf8');
        vm.runInContext(source, context, { filename });
    }
    window.workbench = context.workbench;
    context.workbench.__testWindow = window;
    return context.workbench;
}

function encodeEvents(events) {
    return events.map((event) => JSON.stringify(event)).join('\n') + '\n';
}

function pageResponse(events) {
    return { ok: true, records: events };
}

function decodeEvents(source) {
    return source.split(/\r?\n/).map((line) => line.trim()).filter(Boolean).map((line) => JSON.parse(line));
}

function installTestStreamRuntime(workbench) {
    const stores = [];
    workbench.queryStream = {
        async recoverPendingRowStores() {},
        markCurrentRowStoresForRecovery() {},
        async createRowStore() {
            const rows = [];
            const store = {
                rows,
                disposed: false,
                async append(batch) { rows.push(...batch); return rows.length; },
                async read(start, count) { return rows.slice(start, start + count); },
                async count() { return rows.length; },
                async dispose() { store.disposed = true; }
            };
            stores.push(store);
            return store;
        },
        async consumeNdjsonResponse(response, options) {
            if (!response || !Array.isArray(response.records)) {
                throw new Error('The test response must contain protocol records.');
            }
            for (const record of response.records) {
                await options.onRecord(record);
            }
            return { type: 'end' };
        },
        MeasuredRowHeights: class {
            constructor(count, estimate) {
                this.count = count;
                this.estimate = estimate;
            }
            resize(count) { this.count = count; }
            measure() {}
            offsetOf(index) { return Math.max(0, Math.min(this.count, index)) * this.estimate; }
            range(scrollTop, viewportHeight, overscanRows, maxDomRows) {
                const first = Math.min(this.count, Math.floor(scrollTop / this.estimate));
                const after = Math.min(this.count, Math.ceil((scrollTop + viewportHeight) / this.estimate));
                let start = Math.max(0, first - overscanRows);
                let end = Math.min(this.count, Math.max(after + overscanRows, start + 1));
                if (end - start > maxDomRows) {
                    start = Math.max(0, first - Math.floor(maxDomRows / 2));
                    end = Math.min(this.count, start + maxDomRows);
                }
                return {
                    start,
                    end,
                    topSpacer: start * this.estimate,
                    bottomSpacer: Math.max(0, this.count - end) * this.estimate
                };
            }
        }
    };
    return stores;
}

function fakeRuntime() {
    function html(strings, ...values) {
        return { strings: Array.from(strings), values };
    }
    return {
        html,
        render(template, root) {
            root.template = template;
        }
    };
}

function collectTemplateText(template, output = []) {
    if (!template || typeof template !== 'object') {
        return output;
    }
    if (Array.isArray(template.strings)) {
            output.push(template.strings.join(' '));
            template.values.forEach((value) => {
                if (Array.isArray(value)) {
                    value.forEach((entry) => collectTemplateText(entry, output));
                } else {
                    collectTemplateText(value, output);
                    if (typeof value === 'string' || typeof value === 'number') {
                        output.push(String(value));
                    }
                }
            });
    }
    return output;
}

function flattenTemplateMarkup(template) {
    if (Array.isArray(template)) {
        return template.map(flattenTemplateMarkup).join('');
    }
    if (template && Array.isArray(template.strings)) {
        let markup = '';
        for (let index = 0; index < template.strings.length; index++) {
            markup += template.strings[index];
            if (index < template.values.length) {
                markup += flattenTemplateMarkup(template.values[index]);
            }
        }
        return markup;
    }
    return template === null || typeof template === 'undefined' ? '' : String(template);
}

test('page bootstrap reduces typed route records into a row-store-backed model', async () => {
    const workbench = loadWorkbench();
    const stores = installTestStreamRuntime(workbench);
    const records = [
        { type: 'head', version: 1 },
        { type: 'view', id: 'contexts' },
        { type: 'metadata', values: { title: 'Contexts' } },
        { type: 'vars', values: ['context'] },
        { type: 'rows', values: [[{ kind: 'iri', value: 'urn:graph' }], [null]] },
        { type: 'links', values: ['custom'] },
        { type: 'end', metadata: { count: 2 } }
    ];
    const document = {
        location: { href: 'https://example.test/workbench/repositories/repo-1/contexts' },
        body: { classList: { add() {} } },
        documentElement: { setAttribute() {} },
        readyState: 'complete',
        getElementById() { return null; }
    };
    workbench.__testWindow.document = document;
    workbench.__testWindow.localStorage = { getItem() { return null; }, setItem() {} };
    workbench.__testWindow.matchMedia = () => ({ matches: false });
    const mount = {
        ownerDocument: document,
        getAttribute(name) {
            return {
                'data-workbench-fetch-page-model': 'true',
                'data-workbench-view': 'contexts',
                'data-workbench-base-path': '/workbench',
                'data-workbench-repository-id': 'repo-1'
            }[name] || null;
        }
    };

    const result = await workbench.app.bootstrap(mount, {
        fetch: () => Promise.resolve(pageResponse(records)),
        runtime: fakeRuntime(),
        skipScripts: true
    });
    const model = result.model;

    assert.equal(model.viewId, 'contexts');
    assert.equal(model.metadata.title, 'Contexts');
    assert.equal(model.metadata.count, 2);
    assert.deepEqual(Array.from(model.vars), ['context']);
    assert.equal(model.rows.length, 0, 'large route rows are not copied into the main-thread model');
    assert.equal(model.rowCount, 2);
    assert.equal(await model.rowStore.count(), 2);
    const storedRows = await model.rowStore.read(0, 2);
    assert.equal(storedRows[0][0].kind, 'iri');
    assert.equal(storedRows[1][0], null);
    assert.deepEqual(Array.from(model.links), ['custom']);
    assert.equal(stores.length, 1);
});

test('page bootstrap rejects malformed route fields and terminal errors', async () => {
    const workbench = loadWorkbench();
    installTestStreamRuntime(workbench);
    const document = {
        location: { href: 'https://example.test/workbench/repositories/repo-1/contexts' },
        body: { classList: { add() {} } },
        documentElement: { setAttribute() {} },
        readyState: 'complete',
        getElementById() { return null; }
    };
    workbench.__testWindow.document = document;
    workbench.__testWindow.localStorage = { getItem() { return null; }, setItem() {} };
    workbench.__testWindow.matchMedia = () => ({ matches: false });
    const mount = {
        ownerDocument: document,
        getAttribute(name) {
            return {
                'data-workbench-fetch-page-model': 'true',
                'data-workbench-view': 'contexts',
                'data-workbench-base-path': '/workbench'
            }[name] || null;
        }
    };
    await assert.rejects(workbench.app.bootstrap(mount, {
        fetch: () => Promise.resolve(pageResponse([
            { type: 'head', version: 1 }, { type: 'view', id: 'contexts' },
            { type: 'metadata', values: [] }, { type: 'end' }
        ])),
        runtime: fakeRuntime(),
        skipScripts: true
    }), /metadata values must be an object/);

    await assert.rejects(workbench.app.bootstrap(mount, {
        fetch: () => Promise.resolve(pageResponse([
            { type: 'head', version: 1 }, { type: 'view', id: 'contexts' },
            { type: 'error', status: 500, message: 'unavailable' }
        ])),
        runtime: fakeRuntime(),
        skipScripts: true
    }), /unavailable/);
});

test('page bootstrap never fetches or replays a non-GET shell', async () => {
    const workbench = loadWorkbench();
    let calls = 0;
    const result = await workbench.app.bootstrap({
        getAttribute(name) {
            return name === 'data-workbench-fetch-page-model' ? 'false' : null;
        }
    }, {
        fetch() {
            calls++;
        },
        runtime: fakeRuntime()
    });

    assert.equal(result.status, 'skipped');
    assert.equal(calls, 0);
});

test('page bootstrap fetches current GET once and follows explicit linked models', async () => {
    const workbench = loadWorkbench();
    const stores = installTestStreamRuntime(workbench);
    const calls = [];
    const pageUrl = 'https://example.test/workbench/repo/contexts?limit=20';
    const current = encodeEvents([
        { type: 'head', version: 1 },
        { type: 'view', id: 'contexts' },
        { type: 'vars', values: ['context'] },
        { type: 'rows', values: [[{ kind: 'iri', value: 'urn:graph' }]] },
        { type: 'links', values: ['info', '_internal/namespaces'] },
        { type: 'end' }
    ]);
    const info = encodeEvents([
        { type: 'head', version: 1 },
        { type: 'view', id: 'info' },
        { type: 'metadata', values: { workbench: { repository: 'repo-1' } } },
        { type: 'end' }
    ]);
    const namespaces = encodeEvents([
        { type: 'head', version: 1 },
        { type: 'view', id: '_internal/namespaces' },
        { type: 'vars', values: ['prefix', 'namespace'] },
        { type: 'rows', values: [[{ kind: 'literal', value: 'ex' }, { kind: 'literal', value: 'urn:example:' }]] },
        { type: 'end' }
    ]);
    const responses = [pageResponse(decodeEvents(current)), pageResponse(decodeEvents(info)),
        pageResponse(decodeEvents(namespaces))];
    const mount = {
        dataset: { workbenchView: 'contexts', workbenchBasePath: '/workbench' },
        getAttribute(name) {
            return {
                'data-workbench-fetch-page-model': 'true',
                'data-workbench-view': 'contexts',
                'data-workbench-base-path': '/workbench',
                'data-workbench-repository-id': 'repo-1'
            }[name] || null;
        },
        ownerDocument: { location: { href: pageUrl } }
    };

    const result = await workbench.app.bootstrap(mount, {
        fetch(url, options) {
            calls.push({ url: String(url), options });
            return Promise.resolve(responses.shift());
        },
        runtime: fakeRuntime()
    });

    assert.equal(result.status, 'rendered');
    assert.equal(result.model.viewId, 'contexts');
    assert.equal(result.model.workbench.repository, 'repo-1');
    assert.equal(Object.keys(result.model.linked.namespaces.namespaceMap).length, 1);
    assert.equal(result.model.linked.namespaces.namespaceMap['ex:'], 'urn:example:');
    assert.equal(Object.prototype.hasOwnProperty.call(result.model.linked.namespaces, 'rows'), false,
        'linked namespace rows must not be retained alongside the compact editor map');
    assert.deepEqual(stores[2].rows, [], 'linked namespace table rows should not be copied into the main thread store');
    assert.equal(stores[2].disposed, true, 'the linked namespace row store should be released after reducing its map');
    assert.equal(calls.filter((call) => call.url === pageUrl).length, 1);
    assert.deepEqual(calls.map((call) => new URL(call.url).pathname), [
        '/workbench/repo/contexts',
        '/workbench/repo/info',
        '/workbench/repo/_internal/namespaces'
    ]);
    assert.ok(calls.every((call) => call.options.headers.Accept === 'application/vnd.rdf4j.workbench+ndjson'));
});

test('reduces namespace-metadata endpoint rows including the empty prefix into the editor map', async () => {
    const workbench = loadWorkbench();
    const stores = installTestStreamRuntime(workbench);
    const pageUrl = 'https://example.test/workbench/repo/contexts';
    const page = pageResponse([
        { type: 'head', version: 1 },
        { type: 'view', id: 'contexts' },
        { type: 'vars', values: ['context'] },
        { type: 'rows', values: [[{ kind: 'iri', value: 'urn:graph' }]] },
        { type: 'links', values: ['info', '_internal/namespaces'] },
        { type: 'end' }
    ]);
    const info = pageResponse([
        { type: 'head', version: 1 },
        { type: 'view', id: 'info' },
        { type: 'metadata', values: { workbench: { repository: 'repo-1' } } },
        { type: 'end' }
    ]);
    const namespaceMetadata = pageResponse([
        { type: 'head', version: 1 },
        { type: 'view', id: 'namespace-metadata' },
        { type: 'vars', values: ['prefix', 'namespace'] },
        { type: 'rows', values: [
            [{ kind: 'literal', value: '' }, { kind: 'literal', value: 'urn:default:' }],
            [{ kind: 'literal', value: 'ex' }, { kind: 'literal', value: 'urn:example:' }]
        ] },
        { type: 'end' }
    ]);
    const responses = [page, info, namespaceMetadata];
    const mount = {
        getAttribute(name) {
            return {
                'data-workbench-fetch-page-model': 'true',
                'data-workbench-view': 'contexts',
                'data-workbench-base-path': '/workbench',
                'data-workbench-repository-id': 'repo-1'
            }[name] || null;
        },
        ownerDocument: { location: { href: pageUrl } }
    };

    const result = await workbench.app.bootstrap(mount, {
        fetch() { return Promise.resolve(responses.shift()); },
        runtime: fakeRuntime(),
        skipScripts: true
    });

    assert.equal(result.status, 'rendered');
    assert.deepEqual(JSON.parse(JSON.stringify(result.model.linked.namespaces.namespaceMap)), {
        ':': 'urn:default:',
        'ex:': 'urn:example:'
    });
    assert.deepEqual(JSON.parse(JSON.stringify(workbench.__testWindow.sparqlNamespaces)), {
        ':': 'urn:default:',
        'ex:': 'urn:example:'
    });
    assert.equal(Object.prototype.hasOwnProperty.call(result.model.linked.namespaces, 'rows'), false);
    assert.deepEqual(stores[2].rows, [], 'linked namespace rows must be reduced before row-store retention');
    assert.equal(stores[2].disposed, true);
});

test('query route loads the complete legacy Explain, Compare, and Diff dependency graph', async () => {
    const workbench = loadWorkbench();
    installTestStreamRuntime(workbench);
    const scripts = [];
    const document = {
        location: { href: 'https://example.test/workbench/repositories/repo-1/query' },
        body: { classList: { add() {} } },
        documentElement: { setAttribute() {} },
        readyState: 'complete',
        head: {
            appendChild(script) {
                scripts.push(script.src);
                script.onload();
            }
        },
        querySelector() { return null; },
        getElementById() { return null; },
        createElement() {
            const attributes = {};
            return {
                setAttribute(name, value) { attributes[name] = String(value); },
                getAttribute(name) { return attributes[name] || null; }
            };
        }
    };
    workbench.__testWindow.document = document;
    workbench.__testWindow.localStorage = { getItem() { return null; }, setItem() {} };
    workbench.__testWindow.matchMedia = () => ({ matches: false });
    workbench.__testWindow.jQuery = {};
    workbench.addLoad = () => {};
    workbench.views.render = () => ({});
    workbench.views.bindRowWindows = undefined;
    workbench.queryPage = { renderInto() {} };
    const mount = {
        ownerDocument: document,
        querySelector() { return this; },
        getAttribute(name) {
            return {
                'data-workbench-fetch-page-model': 'true',
                'data-workbench-view': 'query',
                'data-workbench-base-path': '/workbench',
                'data-workbench-repository-id': 'repo-1'
            }[name] || null;
        }
    };

    await workbench.app.bootstrap(mount, {
        currentUrl: document.location.href,
        fetch: () => Promise.resolve(pageResponse([
            { type: 'head', version: 1 }, { type: 'view', id: 'query' }, { type: 'end' }
        ])),
        runtime: fakeRuntime()
    });

    const routeScripts = scripts.map((source) => source.slice('/workbench/scripts/'.length));
    const required = [
        'queryCancelPolicy.js', 'diff.min.js', 'viz/viz.js', 'viz/full.render.js',
        'svg-pan-zoom.min.js', 'queryExplanationHighlighter.js', 'query.js'
    ];
    for (const name of required) {
        assert.ok(routeScripts.includes(name), `query route must load ${name}`);
    }
    assert.ok(routeScripts.indexOf('queryCancelPolicy.js') < routeScripts.indexOf('query.js'));
    assert.ok(routeScripts.indexOf('queryExplanationHighlighter.js') < routeScripts.indexOf('query.js'));
});

test('linked Info rows hydrate the shared server identity and policy-visible navigation model', async () => {
    const workbench = loadWorkbench();
    const stores = installTestStreamRuntime(workbench);
    const pageUrl = 'https://example.test/workbench/repositories/NONE/repositories';
    const term = (value) => value === null ? null : { kind: 'literal', value: String(value) };
    const page = pageResponse([
        { type: 'head', version: 1 },
        { type: 'view', id: 'repositories' },
        { type: 'links', values: ['info'] },
        { type: 'end' }
    ]);
    const info = pageResponse([
        { type: 'head', version: 1 },
        { type: 'view', id: 'info' },
        { type: 'vars', values: [
            'server', 'readable', 'writeable', 'menu-group-id', 'menu-group-label', 'menu-group-icon',
            'menu-group-order', 'menu-item-id', 'menu-item-label', 'menu-item-icon', 'menu-item-order',
            'menu-item-href'
        ] },
        { type: 'rows', values: [[
            term('http://example.test/rdf4j-server'), term('true'), term('true'), term('repositories'),
            term('Repositories'), term('repository'), term('1'), term('create'), term('Create'),
            term('create'), term('1'), term('/workbench/repositories/NONE/create')
        ]] },
        { type: 'end' }
    ]);
    const responses = [page, info];
    const mount = {
        ownerDocument: { location: { href: pageUrl } },
        getAttribute(name) {
            return {
                'data-workbench-fetch-page-model': 'true',
                'data-workbench-view': 'repositories',
                'data-workbench-base-path': '/workbench'
            }[name] || null;
        }
    };

    const result = await workbench.app.bootstrap(mount, {
        fetch: () => Promise.resolve(responses.shift()),
        runtime: fakeRuntime()
    });

    assert.equal(result.model.workbench.server, 'http://example.test/rdf4j-server');
    assert.equal(result.model.workbench.menu[0].items[0].id, 'create');
    assert.equal(result.model.workbench.menu[0].items[0].href, '/workbench/repositories/NONE/create');
    assert.equal(stores[1].disposed, true, 'linked Info row storage should be released after aggregation');
});

test('linked Info named-result rows preserve the configured export format', () => {
    const workbench = loadWorkbench();
    const infoModel = {
        vars: ['id', 'default-export-format', 'graph-download-format'],
        rows: [
            ['repo-1', null, null],
            [null, 'application/n-quads', null],
            [null, null, 'application/n-triples N-Triples'],
            [null, null, 'application/n-quads N-Quads']
        ],
        metadata: {}
    };

    const info = workbench.views.linkedInfoMetadata(infoModel);
    assert.equal(info.defaults['default-export-format'], 'application/n-quads',
        'named Info settings may be emitted on separate rows after the identity row');
    assert.equal(info.graphDownloadFormats.length, 2);
    assert.equal(info.graphDownloadFormats[0], 'application/n-triples N-Triples');
    assert.equal(info.graphDownloadFormats[1], 'application/n-quads N-Quads');
});

test('Export Download button contains its accessible label and shared icon', () => {
    const workbench = loadWorkbench();
    const template = workbench.views.pageTemplate({ viewId: 'export', vars: [], rows: [], metadata: {} },
        { basePath: '/workbench', repositoryId: 'repo-1', workbench: {} }, fakeRuntime());
    const markup = flattenTemplateMarkup(template);
    const buttonStart = markup.indexOf('<button type="submit" name="action" value="download"');
    const buttonEnd = markup.indexOf('</button>', buttonStart);

    assert.ok(buttonStart >= 0, 'Export should keep a native Download submit button');
    assert.ok(buttonEnd > buttonStart, 'the Download button should have a complete template boundary');
    const buttonMarkup = markup.substring(buttonStart, buttonEnd);
    assert.match(buttonMarkup, /aria-label="Download"/, 'the Download button should expose an accessible name');
    assert.match(buttonMarkup, /data-workbench-icon=?download/,
        'the Download button should contain the shared icon');
});

test('shell rendering preserves a hydrated Info summary beside its linked-model wrapper', () => {
    const workbench = loadWorkbench();
    const runtime = fakeRuntime();
    const linkedInfo = {
        server: 'http://example.test/rdf4j-server',
        readable: 'true',
        writeable: 'true',
        menu: [{
            id: 'repositories', label: 'Repositories', icon: 'repository', order: 1,
            items: [{
                id: 'create', label: 'Create repository', icon: 'create', order: 0,
                href: '/workbench/repositories/NONE/create'
            }]
        }],
        queryFeatures: {}
    };
    const context = {
        basePath: '/workbench', repositoryId: 'NONE', workbench: linkedInfo,
        linked: { info: { metadata: {}, workbench: linkedInfo } }
    };
    const output = collectTemplateText(workbench.views.pageTemplate({
        viewId: 'repositories', vars: [], rows: [], rowCount: 0, metadata: {}
    }, context, runtime)).join(' ');

    assert.ok(output.includes('http://example.test/rdf4j-server'),
        'the shared context should retain the server from the hydrated Info summary');
    assert.ok(output.includes('data-workbench-menu-group=') && output.includes('Repositories'),
        'the shared navigation should render the policy-provided Info group');
    assert.ok(output.includes('data-workbench-nav-href=')
        && output.includes('/workbench/repositories/NONE/create'),
        'the Create route should keep its server-provided URL');
});

test('representative non-query templates keep Workbench form and table hooks', () => {
    const workbench = loadWorkbench();
    const runtime = fakeRuntime();
    const context = { basePath: '/workbench', repositoryId: 'repo-1', workbench: {} };
    const cases = [
        {
            viewId: 'namespaces',
            vars: ['prefix', 'namespace'],
            rows: [['ex', 'urn:example:']],
            expected: ['id="namespaces-form"', 'name="prefix"', 'id="prefix-select"', 'id="namespaces-results"']
        },
        {
            viewId: 'contexts',
            vars: ['context'],
            rows: [[{ kind: 'iri', value: 'urn:graph' }]],
            expected: ['contexts-results', 'urn:graph']
        },
        {
            viewId: 'create',
            vars: ['type', 'label'],
            rows: [['memory', 'Memory Store']],
            expected: ['form', 'name="type"', 'memory', 'Memory Store']
        }
    ];

    for (const testCase of cases) {
        const template = workbench.views.pageTemplate({ ...testCase, metadata: {} }, context, runtime);
        const text = collectTemplateText(template).join(' ');
        for (const expected of testCase.expected) {
            assert.ok(text.includes(expected), `${testCase.viewId} template should include ${expected}`);
        }
    }
});

test('Explore preserves its persisted datatype option and reports a result-limited page', () => {
    const workbench = loadWorkbench();
    const runtime = fakeRuntime();
    const context = { basePath: '/workbench', repositoryId: 'repo-1', workbench: {} };
    const model = {
        viewId: 'explore',
        vars: ['resource'],
        rows: [['urn:example:resource']],
        rowCount: 100,
        metadata: { resource: 'urn:example:resource', 'default-limit': '100' }
    };
    const output = collectTemplateText(workbench.views.pageTemplate(model, context, runtime)).join(' ');

    assert.ok(output.includes('id="explore-show-datatypes"'), 'Explore should keep the existing datatype control id');
    assert.ok(output.includes('name="show-datatypes" value="show-dataypes"'),
        'Explore should preserve the request value consumed by the existing endpoint');
    assert.ok(output.includes('id="result-limited"'), 'a full default-limit page should identify truncated results');

    const unlimited = collectTemplateText(workbench.views.pageTemplate({
        ...model,
        rowCount: 100,
        metadata: { ...model.metadata, 'default-limit': '0' }
    }, context, runtime)).join(' ');
    assert.ok(!unlimited.includes('id="result-limited"'), 'unlimited Explore results should not show the truncation notice');
});

test('Add template preserves isolation options and the disabled context-override default', () => {
    const workbench = loadWorkbench();
    const runtime = fakeRuntime();
    const context = { basePath: '/workbench', repositoryId: 'repo-1', workbench: {} };
    const model = {
        viewId: 'add',
        vars: ['isolation-level-option', 'isolation-level-option-label',
            'transaction-setting__org.eclipse.rdf4j.common.transaction.IsolationLevel', 'context'],
        rows: [
            ['READ_COMMITTED', 'Read committed', null, ''],
            ['SNAPSHOT', 'Snapshot', null, '']
        ],
        rowCount: 2,
        rowStart: 0,
        metadata: {}
    };
    const markup = flattenTemplateMarkup(workbench.views.pageTemplate(model, context, runtime));

    assert.ok(markup.includes('Isolation level'), 'Add should expose the isolation-level label');
    assert.ok(markup.includes('name="transaction-setting__org.eclipse.rdf4j.common.transaction.IsolationLevel"'),
        'Add should submit the isolation setting under the servlet parameter name');
    assert.ok(markup.includes('value=READ_COMMITTED') && markup.includes('Read committed'));
    assert.ok(markup.includes('value=SNAPSHOT') && markup.includes('Snapshot'));
    assert.ok(markup.includes('Override parsed contexts with this context'));
    assert.ok(markup.includes('id="context-help"') && markup.includes('embedded contexts are preserved'));
    assert.ok(markup.includes('id="overrideContext"') && markup.includes('?checked=false'),
        'context override should be off when no context is supplied');
    assert.ok(markup.includes('id="context"') && markup.includes('?disabled=true'),
        'the context input should start disabled when context override is off');
    assert.ok(markup.includes('RDF context may be an IRI, blank node, or the default graph.'));
    assert.ok(markup.includes('With override off, embedded contexts are preserved; contextless data uses the default graph.'));
    assert.ok(markup.includes('Base URI resolves relative RDF identifiers; it does not choose a graph context.'));

    const availableIsolationOptions = flattenTemplateMarkup(workbench.views.pageTemplate({
        ...model,
        vars: [...model.vars, 'baseURI'],
        rows: [
            ['NONE', 'None', null, '', 'https://example.org/base'],
            ['READ_COMMITTED', 'Read Committed', null, '', 'https://example.org/base']
        ],
        rowCount: 2
    }, context, runtime));
    assert.ok(availableIsolationOptions.includes('value=NONE'), 'the None option value should be preserved');
    assert.ok(availableIsolationOptions.includes('None'), 'the None option label should be preserved');
    assert.ok(availableIsolationOptions.includes('value=READ_COMMITTED'),
        'the Read Committed option value should be preserved');
    assert.ok(availableIsolationOptions.includes('Read Committed'),
        'the Read Committed option label should be preserved');
    assert.ok(!availableIsolationOptions.includes('value=SNAPSHOT'),
        'Add should render only isolation options supplied by the repository');
    assert.ok(availableIsolationOptions.includes('id="baseURI"')
        && availableIsolationOptions.includes('value=https://example.org/base'));
    assert.ok(availableIsolationOptions.includes('id="context"')
        && availableIsolationOptions.includes('?disabled=true')
        && !availableIsolationOptions.includes('id="context" name="context" type="text" size=48 aria-describedby="context-help" value=https://example.org/base'),
    'the base URI must not prefill or enable the context override');
});

test('Summary template renders the effective config model in a closed disclosure', () => {
    const workbench = loadWorkbench();
    const runtime = fakeRuntime();
    const config = '@prefix config: <tag:rdf4j.org,2023:config/> .\n'
        + '[] config:rep.id "memory" ; config:rep.impl [ config:rep.type "openrdf:SailRepository" ] .';
    const output = flattenTemplateMarkup(workbench.views.pageTemplate({
        viewId: 'summary',
        vars: ['id', 'description', 'location', 'server', 'size', 'contexts'],
        rows: [['memory', 'Memory repo', 'http://example.test/repositories/memory',
            'http://example.test/rdf4j-server/', '7', '1']],
        metadata: { 'config-model-turtle': config }
    }, { basePath: '/workbench', repositoryId: 'memory', workbench: {} }, runtime));

    assert.ok(output.includes('id="summary-config-model"'), 'Summary should expose its config-model disclosure');
    assert.ok(output.includes('Config Model'), 'Summary should retain the disclosure label');
    assert.ok(output.includes('<pre role="region">'), 'configuration text should render in the disclosure body');
    assert.ok(output.includes('@prefix config:') && output.includes('config:rep.type "openrdf:SailRepository"'),
        'the effective Turtle model should remain available as rendered text');
});

test('settings and action dropdowns share one component while information stays native', () => {
    const workbench = loadWorkbench();
    const runtime = fakeRuntime();
    const context = { basePath: '/workbench', repositoryId: 'repo-1', workbench: {} };
    const routes = [
        ['explore', { vars: ['subject', 'predicate', 'object'], rows: [['urn:s', 'urn:p', 'urn:o']] },
            [['explore-result-options', 'explore-result-options-toggle', 'explore-result-options-panel']]],
        ['export', { vars: [], rows: [] },
            [['export-result-options', 'export-result-options-toggle', 'export-result-options-panel']]],
        ['add', { vars: [], rows: [] },
            [['add-import-settings', 'add-import-settings-toggle', 'add-import-settings-panel']]],
        ['server', { vars: ['server'], rows: [['http://example.test']] },
            [['server-auth', 'server-auth-toggle', 'server-auth-panel']]],
        ['query', { vars: [], rows: [] }, [
            ['explanation-settings', 'explanation-settings-toggle', 'explanation-settings-panel'],
            ['save-query-disclosure', 'save-query-toggle', 'save-query-panel'],
            ['query-options-disclosure', 'query-options-toggle', 'query-options-panel']
        ]]
    ];

    for (const [viewId, model, disclosures] of routes) {
        const output = flattenTemplateMarkup(workbench.views.pageTemplate({
            viewId, metadata: {}, ...model
        }, context, runtime));
        for (const [owner, toggle, panel] of disclosures) {
            assert.ok(output.includes(`data-workbench-detail-disclosure="true"`),
                `${viewId} should use the shared detail component`);
            assert.ok(output.includes(`id="${owner}"`), `${viewId} should retain ${owner}`);
            assert.ok(output.includes(`id="${toggle}"`)
                && output.includes('class="workbench-disclosure__toggle'), `${viewId} should use the shared trigger`);
            assert.ok(output.includes(`id="${panel}"`)
                && output.includes('class="workbench-disclosure__panel'), `${viewId} should use the shared panel`);
        }
    }

    const summary = flattenTemplateMarkup(workbench.views.pageTemplate({
        viewId: 'summary', vars: ['id'], rows: [['repo-1']],
        metadata: { 'config-model-turtle': '@prefix config: <tag:rdf4j.org,2023:config/> .' }
    }, context, runtime));
    const remove = flattenTemplateMarkup(workbench.views.pageTemplate({
        viewId: 'remove', vars: [], rows: [], metadata: {}
    }, context, runtime));
    assert.ok(summary.includes('<details id="summary-config-model"'),
        'Summary Config Model remains an information-only native disclosure');
    assert.ok(remove.includes('<details id="remove-examples"'),
        'Remove Examples remains an information-only native disclosure');
});

test('every navigable built-in route has a registered ordinary-DOM template', () => {
    const workbench = loadWorkbench();
    const runtime = fakeRuntime();
    const context = { basePath: '/workbench', repositoryId: 'repo-1', workbench: {} };
    const routes = [
        ['summary', { vars: ['id', 'description', 'location', 'server', 'size', 'contexts'],
            rows: [['repo-1', 'Example repository', 'memory', 'http://example.test', '12', '2']],
            expected: ['Repository Location', 'Example repository', 'Repository Size'] }],
        ['information', { vars: ['version', 'os', 'jvm', 'user', 'memory-used', 'maximum-memory'],
            rows: [['5.0', 'Linux', 'OpenJDK', 'rdf4j', '128 MB', '1 GB']],
            expected: ['workbench-information', 'information-application', 'information-runtime',
                'information-memory', 'OpenJDK', '128 MB'] }],
        ['repositories', { vars: ['id', 'description'], rows: [['repo-1', 'Example repository']],
            expected: ['repositories-results', 'Example repository'] }],
        ['create', { vars: ['type', 'label'], rows: [['memory', 'Memory Store']],
            expected: ['create-type-form', 'memory', 'Memory Store'] }],
        ['delete', { vars: ['id', 'description'], rows: [['repo-1', 'Example repository']],
            pickerRows: [['repo-1', 'Example repository']], rowCount: 1,
            expected: ['delete-form', 'name="id"', 'repo-1'] }],
        ['namespaces', { vars: ['prefix', 'namespace'], rows: [['ex', 'urn:example:']],
            pickerRows: [['ex', 'urn:example:']], rowCount: 1,
            expected: ['namespaces-form', 'prefix-select', 'urn:example:'] }],
        ['contexts', { vars: ['context'], rows: [['urn:graph']], expected: ['contexts-results', 'urn:graph'] }],
        ['types', { vars: ['type', 'count'], rows: [['urn:Type', '3']], expected: ['types-results', 'urn:Type'] }],
        ['explore', { vars: ['subject', 'predicate', 'object'], rows: [['urn:s', 'urn:p', 'urn:o']],
            expected: ['explore-form', 'explore-results', 'urn:s'] }],
        ['query', { vars: [], rows: [], expected: ['query-form', 'query-results'] }],
        ['saved-queries', { vars: ['query', 'queryName', 'queryText'],
            rows: [['urn:saved:1', 'Example query', 'SELECT * WHERE {}']], expected: ['saved-queries', 'urn:saved:1'] }],
        ['export', { vars: [], rows: [], expected: ['export-form', 'export-results', 'name="Accept"'] }],
        ['add', { vars: [], rows: [], expected: ['add-source-tabs', 'add-import-settings', 'name="Content-Type"'] }],
        ['remove', { vars: [], rows: [], expected: ['remove-warning', 'remove-form', 'name=', 'subj'] }],
        ['clear', { vars: [], rows: [], expected: ['clear-warning', 'clear-form', 'name="context"'] }],
        ['update', { vars: [], rows: [], expected: ['update-form', 'updateString.errors', 'name="update"'] }],
        ['server', { vars: ['server'], rows: [['http://example.test']], expected: ['server-form', 'workbench-server'] }]
    ];

    assert.equal(routes.length, 17, 'the built-in route inventory should cover every navigable view');
    for (const [viewId, page] of routes) {
        const expected = page.expected;
        const { expected: _expected, ...pageModel } = page;
        const output = collectTemplateText(workbench.views.pageTemplate({
            viewId, metadata: {}, ...pageModel
        }, context, runtime)).join(' ');
        for (const marker of expected) {
            assert.ok(output.includes(marker), `${viewId} route should render ${marker}`);
        }
        assert.ok(!output.includes('Unsupported Workbench view'), `${viewId} must resolve in the route renderer`);
    }
});

test('shared page shell does not render a visible theme selector', () => {
    const workbench = loadWorkbench();
    const output = flattenTemplateMarkup(workbench.views.pageTemplate({
        viewId: 'repositories', vars: [], rows: [], metadata: {}
    }, { basePath: '/workbench', repositoryId: 'repo-1', workbench: {} }, fakeRuntime()));

    assert.ok(!output.includes('workbench-theme-control'), 'the shared header should not render the theme selector');
    assert.ok(!output.includes('id="workbench-theme"'), 'the theme selector should not appear in the page markup');
});

test('Server form restores the username while keeping the password blank', () => {
    const workbench = loadWorkbench();
    const runtime = fakeRuntime();
    const row = {
        server: 'http://example.test/rdf4j-server',
        'server-user': 'service-account',
        'server-password': 'must-not-be-rendered'
    };
    const output = collectTemplateText(workbench.views.pageTemplate({
        viewId: 'server', vars: Object.keys(row), rows: [Object.values(row)], metadata: {}
    }, { basePath: '/workbench', repositoryId: '', workbench: {} }, runtime)).join(' ');

    assert.ok(output.includes('server-user'), 'the server username field should remain present');
    assert.ok(output.includes('service-account'), 'the existing server username should be restored');
    assert.ok(!output.includes('must-not-be-rendered'), 'the server password must remain blank on render');
});

test('all repository creation variants preserve their page-model fields and actions', () => {
    const workbench = loadWorkbench();
    const runtime = fakeRuntime();
    const context = { basePath: '/workbench', repositoryId: 'repo-1', workbench: {} };
    const variants = [
        'federate', 'lmdb', 'memory', 'memory-customrule', 'memory-lucene', 'memory-rdfs',
        'memory-rdfs-dt', 'memory-rdfs-lucene', 'memory-shacl', 'native', 'native-customrule',
        'native-lucene', 'native-rdfs', 'native-rdfs-dt', 'native-rdfs-lucene', 'native-shacl',
        'remote', 'sparql'
    ];

    assert.equal(variants.length, 18, 'the creation contract inventory should cover all 18 repository variants');
    for (const variant of variants) {
        let model;
        let expected;
        if (variant === 'federate') {
            model = {
                viewId: 'create', vars: ['id', 'description', 'location'],
                rows: [['member-a', 'Member A', 'http://example.test/repositories/member-a']], metadata: {}
            };
            expected = ['value="federate"', 'Local repository ID', 'memberID', 'member-a', 'create-feedback'];
        } else {
            const fields = ['templateType', 'templateLabel', 'fieldId', 'fieldProperty', 'fieldRole',
                'fieldName', 'fieldType', 'value', 'selected', 'size', 'rows', 'cols', 'placeholder'];
            model = {
                viewId: 'create', vars: fields,
                rows: [[variant, 'Variant ' + variant, 'repository-id', 'urn:field:repository-id',
                    'repository-id', 'Local repository ID', 'text', 'repo-' + variant, 'true', '40',
                    '4', '60', 'Repository identifier']], metadata: {}
            };
            expected = [variant, 'Variant ' + variant, 'Local repository ID',
                'name=', 'repo-' + variant, 'data-config-property=', 'id="create"'];
        }
        const output = collectTemplateText(workbench.views.pageTemplate(model, context, runtime)).join(' ');
        for (const marker of expected) {
            assert.ok(output.includes(marker), `${variant} creation form should preserve ${marker}`);
        }
    }
});

test('repository creation templates render each configured field control without special cases', () => {
    const workbench = loadWorkbench();
    const runtime = fakeRuntime();
    const context = { basePath: '/workbench', repositoryId: 'repo-1', workbench: {} };
    const fields = [
        ['memory-id', 'Repository ID', 'text', 'repository-id', 'memory-1'],
        ['sail-type', 'Sail type', 'select', 'sail-type', 'memory'],
        ['infer', 'Include inferred statements', 'radio', 'include-inferred', 'true'],
        ['config', 'Configuration', 'textarea', 'config', '<rdf/>']
    ];
    const rows = fields.flatMap(([fieldId, fieldName, fieldType, fieldRole, value]) => {
        const base = {
            fieldId, fieldName, fieldType, fieldRole,
            fieldProperty: 'http://example.test/config#' + fieldId,
            templateType: 'memory', templateLabel: 'Memory Store'
        };
        if (fieldType === 'select' || fieldType === 'radio') {
            return [
                { ...base, value, selected: 'true' },
                ...(fieldType === 'radio' ? [{ ...base, value: 'false', selected: 'false' }] : [])
            ];
        }
        return [{ ...base, value }];
    });
    const model = {
        viewId: 'create', vars: Object.keys(rows[0]), rows: rows.map((row) => Object.values(row)), metadata: {}
    };
    const template = workbench.views.pageTemplate(model, context, runtime);
    const output = collectTemplateText(template).join(' ');

    for (const [fieldId, fieldName, , fieldRole, value] of fields) {
        assert.ok(output.includes(fieldId), `configured field ${fieldId} should be rendered`);
        assert.ok(output.includes(fieldName), `configured label ${fieldName} should be rendered`);
        assert.ok(output.includes(fieldRole), `configured role ${fieldRole} should be preserved`);
        assert.ok(output.includes(value), `configured value for ${fieldId} should be preserved`);
    }
    assert.ok(output.includes('data-config-property='));
    assert.ok(output.includes('Advanced settings'));
    assert.ok(output.includes('Cancel'));
});

test('saved-query execution has an explicit streamed result mount and native edit/delete forms', () => {
    const workbench = loadWorkbench();
    const runtime = fakeRuntime();
    const context = { basePath: '/workbench', repositoryId: 'repo-1', workbench: {} };
    const row = {
        query: 'urn:saved:1', queryName: 'Example query', queryText: 'SELECT * WHERE {}',
        user: 'alice', queryLn: 'SPARQL', infer: 'true', rowsPerPage: '20', queryTimeout: '0', shared: 'false'
    };
    const template = workbench.views.pageTemplate({
        viewId: 'saved-queries', vars: Object.keys(row), rows: [Object.values(row)], metadata: {}
    }, context, runtime);
    const output = collectTemplateText(template).join(' ');

    assert.ok(output.includes('data-workbench-query-execution='));
    assert.ok(output.includes('data-workbench-results-target='));
    assert.ok(output.includes('saved-query-results-0'));
    assert.ok(output.includes('value="exec"'));
    assert.ok(output.includes('value="edit"'));
    assert.match(output, /class="saved-query-delete[ "]/);
});

test('modification and browse views preserve their action form identifiers', () => {
    const workbench = loadWorkbench();
    const runtime = fakeRuntime();
    const context = { basePath: '/workbench', repositoryId: 'repo-1', workbench: {} };
    const cases = [
        ['add', ['id="add-source-tabs"', 'id="add-import-settings"', 'name="content"', 'name="baseURI"']],
        ['remove', ['id="remove-warning"', 'id="remove-form"', 'subj', 'obj']],
        ['clear', ['id="clear-warning"', 'id="clear-form"', 'name="context"']],
        ['update', ['id="update-form"', 'id="update"', 'name="update"']],
        ['export', ['id="export-form"', 'id="export-results"', 'name="Accept"', 'name="action"']],
        ['delete', ['id="delete-form"', 'id="delete-feedback"', 'name="id"']],
        ['types', ['types-results']],
        ['contexts', ['contexts-results']]
    ];

    for (const [viewId, expected] of cases) {
        const model = { viewId, vars: [], rows: [], metadata: {} };
        const template = workbench.views.pageTemplate(model, context, runtime);
        const output = collectTemplateText(template).join(' ');
        const markup = flattenTemplateMarkup(template);
        for (const text of expected) {
            assert.ok(output.includes(text) || markup.includes(text), `${viewId} route should include ${text}`);
        }
    }
});

test('shared navigation keeps app assets and Workbench servlet routes on their correct roots', () => {
    const workbench = loadWorkbench();
    const runtime = fakeRuntime();
    const menu = [
        { id: 'explore', label: 'Repository', items: [{ id: 'summary', label: 'Summary' }, { id: 'query', label: 'Query' }] },
        { id: 'repositories', label: 'Server', items: [{ id: 'repositories', label: 'Repositories' }, { id: 'server', label: 'Connection' }] },
        { id: 'system', label: 'System', items: [{ id: 'information', label: 'Information' }] }
    ];
    const template = workbench.views.pageTemplate({
        viewId: 'summary', vars: [], rows: [], metadata: {}
    }, { basePath: '/workbench', repositoryId: 'repo-1', workbench: { menu } }, runtime);
    const output = collectTemplateText(template).join(' ');

    assert.ok(output.includes('/workbench/images/logo.png'));
    assert.ok(output.includes('/workbench/repositories/repo-1/summary'));
    assert.ok(output.includes('/workbench/repositories/NONE/server'));
    assert.ok(output.includes('/workbench/repositories/NONE/repositories'));
    assert.ok(output.includes('/workbench/repositories/NONE/information'));
});

test('an explicitly empty policy-filtered menu does not fall back to visible routes', () => {
    const workbench = loadWorkbench();
    const runtime = fakeRuntime();
    const template = workbench.views.pageTemplate({
        viewId: 'summary', vars: [], rows: [], metadata: {}
    }, { basePath: '/workbench', repositoryId: 'repo-1', workbench: { menu: [] } }, runtime);
    const output = collectTemplateText(template).join(' ');

    assert.equal((output.match(/data-workbench-nav-href/g) || []).length, 0);
});

test('query route renders the existing streaming form and result targets', () => {
    const workbench = loadWorkbench();
    const runtime = fakeRuntime();
    const template = workbench.views.pageTemplate({
        viewId: 'query', vars: [], rows: [], metadata: {}
    }, { basePath: '/workbench', repositoryId: 'repo-1', workbench: { defaults: { 'default-limit': '0' } } }, runtime);
    const output = collectTemplateText(template).join(' ');
    const markup = flattenTemplateMarkup(template);

    for (const expected of [
        'id="query-page"',
        'id="query-page-content"',
        'id="query-form"',
        'data-workbench-query-execution=',
        'data-workbench-results-target=',
        'name="action"',
        'id="explain"',
        'name="ref"',
        'id="include-query-text"',
        'id="query-request-id"',
        'id="query-language-row"',
        'name="queryLn"',
        'id="query"',
        'name="query"',
        'id="query-compare-layout"',
        'id="query-options-toggle"',
        'id="save-query-toggle"',
        'id="query-timeout"',
        'id="infer"',
        'id="query-results"',
        'id="query-results-loading"',
        'id="query-results-status"',
        'id="query-results-fullscreen"'
    ]) {
        assert.ok(output.includes(expected) || markup.includes(expected), `query route should include ${expected}`);
    }
    assert.ok(!output.includes('onsubmit='), 'query streaming controller must own form submission');
    assert.doesNotMatch(output, /(?:id|name)="limit_query"|Rows Per Page/i,
        'the query form no longer exposes result page-size controls');

    const disclosureToggle = (id) => {
        const marker = markup.indexOf(`id="${id}"`);
        const start = markup.lastIndexOf('<button', marker);
        const end = markup.indexOf('</button>', start) + '</button>'.length;
        assert.ok(marker >= 0 && start >= 0 && end >= '</button>'.length,
            `${id} should have a complete disclosure button`);
        return markup.substring(start, end);
    };
    for (const [id, label] of [['save-query-toggle', 'Save query'], ['query-options-toggle', 'Options']]) {
        const button = disclosureToggle(id);
        const labelPosition = button.indexOf(
            `class="workbench-disclosure__toggle-label">${label}</span>`);
        const chevronPosition = button.indexOf('workbench-disclosure-chevron');
        assert.ok(labelPosition >= 0, `${id} should show ${label}`);
        assert.ok(button.includes('workbench-action-icon--chevron'), `${id} should use the shared chevron`);
        assert.ok(chevronPosition > labelPosition, `${id} should place the chevron after its label`);
		assert.match(button, /aria-expanded=(?:"false"|false)/, `${id} should expose its collapsed state`);
    }
});

test('query page renderer receives the Lit runtime through its shell context', async () => {
    const workbench = loadWorkbench();
    installTestStreamRuntime(workbench);
    const runtime = fakeRuntime();
    let received;
    let submissions = 0;
    workbench.queryPage = {
        renderInto(_target, _model, context) {
            received = context;
        },
        submitExecution() {
            submissions++;
            return true;
        }
    };
    const source = encodeEvents([
        { type: 'head', version: 1 },
        { type: 'view', id: 'query' },
        { type: 'vars', values: [] },
        { type: 'end' }
    ]);
    const document = {
        location: { href: 'https://example.test/workbench/repositories/repo-1/query' },
        body: { classList: { add() {} } },
        documentElement: { setAttribute() {} },
        readyState: 'loading',
        getElementById() { return null; }
    };
    const mount = {
        ownerDocument: document,
        getAttribute(name) {
            return {
                'data-workbench-fetch-page-model': 'true',
                'data-workbench-view': 'query',
                'data-workbench-base-path': '/workbench',
                'data-workbench-repository-id': 'repo-1'
            }[name] || null;
        },
        querySelector() { return {}; }
    };

    await workbench.app.bootstrap(mount, {
        fetch: () => Promise.resolve(pageResponse(decodeEvents(source))),
        runtime,
        skipScripts: true
    });

    assert.equal(received.runtime, runtime);
    assert.equal(received.executionFormId, 'query-form');
    assert.equal(received.resultsMountId, 'query-results');
    assert.equal(submissions, 0, 'a regular GET query page must not execute a query on load');
    assert.ok(collectTemplateText(mount.template).join(' ').includes('id="query-page-content"'));
});

test('bootstrap replays a native query execution descriptor exactly once after mounting', async () => {
    const workbench = loadWorkbench();
    installTestStreamRuntime(workbench);
    workbench.__testWindow.atob = (value) => Buffer.from(value, 'base64').toString('binary');
    const params = {
        action: ['exec'],
        query: ['SELECT * WHERE { ?s ?p "Håvard 🚀" }'],
        queryLn: ['SPARQL'],
        ref: ['text'],
        infer: ['true'],
        'query-timeout': ['60'],
        opaque: ['first', 'second'],
        repeated: ['left', 'right']
    };
    const initialPost = Buffer.from(JSON.stringify(params), 'utf8').toString('base64url');
    const attrs = {
        'data-workbench-fetch-page-model': 'true',
        'data-workbench-view': 'query',
        'data-workbench-base-path': '/workbench',
        'data-workbench-repository-id': 'repo-1',
        'data-workbench-initial-post': initialPost
    };
    const controls = [
        { name: 'action', type: 'hidden', value: '' },
        { name: 'query', type: 'textarea', value: '' },
        { name: 'queryLn', type: 'select-one', value: '' },
        { name: 'ref', type: 'hidden', value: '' },
        { name: 'infer', type: 'checkbox', value: 'true', checked: false },
        { name: 'query-timeout', type: 'number', value: '' },
        { name: 'save-private', type: 'checkbox', value: 'true', checked: true },
        { name: 'repeated', type: 'hidden', value: 'old-one' },
        { name: 'repeated', type: 'hidden', value: 'old-two' }
    ];
    const form = {
        elements: controls,
        addEventListener() {},
        removeEventListener() {},
        appendChild(control) { controls.push(control); }
    };
    const resultTarget = {};
    const calls = [];
    const document = {
        location: { href: 'https://example.test/workbench/repositories/repo-1/query' },
        body: { classList: { add() {} } },
        documentElement: { setAttribute() {} },
        readyState: 'complete',
        getElementById(id) {
            if (id === 'query-form') { return form; }
            if (id === 'query-results') { return resultTarget; }
            return null;
        },
        createElement() { return { setAttribute() {} }; }
    };
    workbench.__testWindow.document = document;
    workbench.__testWindow.localStorage = { getItem() { return null; }, setItem() {} };
    workbench.__testWindow.matchMedia = () => ({ matches: false });
    workbench.queryPage = {
        renderInto() { calls.push('renderInto'); },
        submitExecution() {
            calls.push('submitExecution');
            assert.equal(controls.find((control) => control.name === 'query').value, params.query[0]);
            assert.equal(controls.find((control) => control.name === 'queryLn').value, 'SPARQL');
            assert.equal(controls.find((control) => control.name === 'action').value, 'exec');
            assert.equal(controls.find((control) => control.name === 'infer').checked, true);
            assert.equal(controls.find((control) => control.name === 'save-private').checked, false,
                'unchecked/omitted controls must not leak UI defaults into the original POST');
            assert.equal(controls.find((control) => control.name === 'save-private').disabled, true,
                'controls absent from the native POST must be excluded during replay');
            assert.deepEqual(controls.filter((control) => control.name === 'opaque').map((control) => control.value),
                ['first', 'second']);
            assert.deepEqual(controls.filter((control) => control.name === 'repeated').map((control) => control.value),
                ['left', 'right'], 'repeated values should update the existing controls without duplication');
            return true;
        }
    };
    const mount = {
        ownerDocument: document,
        querySelector(selector) { return selector === '#query-page-content' ? {} : null; },
        querySelectorAll() { return []; },
        getAttribute(name) { return attrs[name] || null; },
        removeAttribute(name) { delete attrs[name]; },
        setAttribute(name, value) { attrs[name] = value; }
    };
    const page = pageResponse([
        { type: 'head', version: 1 }, { type: 'view', id: 'query' },
        { type: 'vars', values: [] }, { type: 'end' }
    ]);
    let getCount = 0;
    const dependencies = {
        skipScripts: true,
        runtime: fakeRuntime(),
        fetch() { getCount++; calls.push('page-get'); return Promise.resolve(page); }
    };

    await workbench.app.bootstrap(mount, dependencies);
    await workbench.app.bootstrap(mount, dependencies);

    assert.equal(getCount, 2, 'each explicit bootstrap call still makes its one page-model GET');
    assert.equal(calls.filter((call) => call === 'submitExecution').length, 1,
        'the consumed initial descriptor must execute once even if bootstrap is called again');
    assert.deepEqual(calls.slice(0, 3), ['page-get', 'renderInto', 'submitExecution']);
    assert.equal(controls.find((control) => control.name === 'save-private').checked, true,
        'temporary omissions must not change the rendered query form after submission');
    assert.equal(controls.find((control) => control.name === 'save-private').disabled, undefined);
    assert.equal(attrs['data-workbench-initial-post'], undefined, 'the descriptor should be removed before replay');
});

test('bootstrap consumes but never replays a non-exec initial POST descriptor', async () => {
    const workbench = loadWorkbench();
    installTestStreamRuntime(workbench);
    workbench.__testWindow.atob = (value) => Buffer.from(value, 'base64').toString('binary');
    let fetches = 0;
    const attrs = {
        'data-workbench-fetch-page-model': 'true',
        'data-workbench-view': 'query',
        'data-workbench-base-path': '/workbench',
        'data-workbench-initial-post': Buffer.from(JSON.stringify({ action: ['delete'] }), 'utf8').toString('base64url')
    };
    const events = [
        { type: 'head', version: 1 }, { type: 'view', id: 'query' },
        { type: 'vars', values: [] }, { type: 'end' }
    ];
    const document = {
        location: { href: 'https://example.test/workbench/repositories/repo-1/query' },
        body: { classList: { add() {} } },
        documentElement: { setAttribute() {} },
        readyState: 'complete',
        getElementById() { return null; },
        createElement() { return { setAttribute() {}, appendChild() {} }; },
        createTextNode(value) { return { nodeValue: value }; }
    };
    workbench.__testWindow.document = document;
    workbench.__testWindow.localStorage = { getItem() { return null; }, setItem() {} };
    workbench.__testWindow.matchMedia = () => ({ matches: false });
    workbench.queryPage = {
        renderInto() {},
        submitExecution() { throw new Error('invalid descriptor must not submit'); }
    };
    const mount = {
        ownerDocument: document,
        getAttribute(name) { return attrs[name] || null; },
        removeAttribute(name) { delete attrs[name]; },
        setAttribute(name, value) { attrs[name] = value; },
        querySelector() { return {}; },
        querySelectorAll() { return []; },
        appendChild() {}
    };

    await assert.rejects(workbench.app.bootstrap(mount, {
        fetch() { fetches++; return Promise.resolve(pageResponse(events)); },
        runtime: fakeRuntime(),
        skipScripts: true
    }), /action must be exec/);
    assert.equal(fetches, 0, 'a non-query-execution descriptor must be rejected before fetching');
    assert.equal(attrs['data-workbench-initial-post'], undefined, 'invalid descriptors are consumed once');
});

test('bootstrap renders an inline initial page model once without fetching its route', async () => {
    const workbench = loadWorkbench();
    installTestStreamRuntime(workbench);
    workbench.__testWindow.atob = (value) => Buffer.from(value, 'base64').toString('binary');
    let modelAttributeRemovedBeforeDecode = false;
    workbench.__testWindow.Response = class {
        constructor(body) {
            modelAttributeRemovedBeforeDecode = attrs['data-workbench-initial-model'] === undefined;
            this.ok = true;
            this.records = decodeEvents(body);
        }
    };
    const events = [
        { type: 'head', version: 1 },
        { type: 'view', id: 'add' },
        { type: 'vars', values: ['error-message', 'context', 'overrideContext'] },
        { type: 'rows', values: [[
            { kind: 'literal', value: 'The submitted RDF URL is invalid.' },
            { kind: 'literal', value: '' }, null
        ]] },
        { type: 'end' }
    ];
    const attrs = {
        'data-workbench-fetch-page-model': 'true',
        'data-workbench-view': 'add',
        'data-workbench-base-path': '/workbench',
        'data-workbench-repository-id': 'repo-1',
        'data-workbench-initial-model': Buffer.from(encodeEvents(events), 'utf8').toString('base64url')
    };
    const document = {
        location: { href: 'https://example.test/workbench/repositories/repo-1/add' },
        body: { classList: { add() {} } },
        documentElement: { setAttribute() {} },
        readyState: 'complete',
        getElementById() { return null; },
        createElement() { return { setAttribute() {}, appendChild() {} }; },
        createTextNode(value) { return { nodeValue: value }; }
    };
    workbench.__testWindow.document = document;
    workbench.__testWindow.localStorage = { getItem() { return null; }, setItem() {} };
    workbench.__testWindow.matchMedia = () => ({ matches: false });
    let renderedView = null;
    let renderedError = null;
    let renderCount = 0;
    workbench.views.render = (_mount, model) => {
        renderCount++;
        renderedView = model.viewId;
        renderedError = model.rows[0]?.[0]?.value;
        return { status: 'rendered' };
    };
    workbench.views.bindRowWindows = () => Promise.resolve(() => {});
    const mount = {
        ownerDocument: document,
        getAttribute(name) { return attrs[name] || null; },
        removeAttribute(name) { delete attrs[name]; },
        setAttribute(name, value) { attrs[name] = value; },
        querySelector() { return null; },
        querySelectorAll() { return []; },
        appendChild() {}
    };
    let fetchCount = 0;
    const dependencies = {
        skipScripts: true,
        runtime: fakeRuntime(),
        fetch() {
            fetchCount++;
            return Promise.reject(new Error('the inline page model must avoid route fetching'));
        }
    };

    const first = await workbench.app.bootstrap(mount, dependencies);
    const second = await workbench.app.bootstrap(mount, dependencies);

    assert.equal(first.status, 'rendered');
    assert.equal(renderedView, 'add', 'the inline model should render the shell’s Add route');
    assert.equal(renderedError, 'The submitted RDF URL is invalid.', 'validation data should reach the route renderer');
    assert.equal(modelAttributeRemovedBeforeDecode, true, 'consume the inline payload before parsing it');
    assert.equal(fetchCount, 0, 'the initial model replaces the route GET');
    assert.equal(renderCount, 1, 'a consumed initial model must not render or fetch twice');
    assert.equal(second.status, 'skipped');
    assert.equal(attrs['data-workbench-initial-model'], undefined, 'the descriptor is removed before parsing');
    assert.equal(attrs['data-workbench-initial-model-consumed'], 'true');
});

test('page bootstrap loads shared view and stream runtimes before requesting the page model', async () => {
    const workbench = loadWorkbench();
    installTestStreamRuntime(workbench);
    const window = workbench.__testWindow;
    const order = [];
    const scripts = [];
    const events = [
        { type: 'head', version: 1 },
        { type: 'view', id: 'contexts' },
        { type: 'vars', values: ['context'] },
        { type: 'end' }
    ];
    const document = {
        location: { href: 'https://example.test/workbench/repositories/repo-1/contexts' },
        body: { classList: { add() {} } },
        documentElement: { setAttribute() {} },
        readyState: 'complete',
        querySelector() { return null; },
        getElementById() { return null; },
        createElement(tagName) {
            return {
                tagName,
                attributes: {},
                setAttribute(name, value) { this.attributes[name] = value; },
                getAttribute(name) { return this.attributes[name] || null; }
            };
        },
        head: {
            appendChild(script) {
                scripts.push(script);
                const name = String(script.src).split('/').pop();
                order.push(name);
                if (name === 'workbenchViews.js') {
                    workbench.views = workbench.views || {};
                }
                if (name === 'queryStream.js') {
                    workbench.queryStream = workbench.queryStream || {};
                }
                if (name === 'workbench-theme.js') {
                    window.RDF4JWorkbenchTheme = window.RDF4JWorkbenchTheme || {
                        configure() {},
                        connectControl() {}
                    };
                }
                script.onload();
            }
        }
    };
    window.document = document;
    window.workbench = workbench;
    window.workbench.addLoad = () => {};
    window.jQuery = {};
    window.localStorage = { getItem() { return null; }, setItem() {} };
    window.matchMedia = () => ({ matches: false });

    const mount = {
        ownerDocument: document,
        getAttribute(name) {
            return {
                'data-workbench-fetch-page-model': 'true',
                'data-workbench-view': 'contexts',
                'data-workbench-base-path': '/workbench',
                'data-workbench-repository-id': 'repo-1'
            }[name] || null;
        }
    };
    const result = await workbench.app.bootstrap(mount, {
        fetch() {
            order.push('page-request');
            return Promise.resolve(pageResponse(events));
        },
        runtime: fakeRuntime()
    });

    assert.equal(result.status, 'rendered');
    assert.deepEqual(order.slice(0, 3), ['workbenchViews.js', 'queryStream.js', 'workbench-theme.js']);
    assert.equal(order[3], 'page-request');
    assert.equal(scripts.length, 3);
});

test('page bootstrap consumes the shared incremental reader and keeps streamed rows in the row store', async () => {
    const workbench = loadWorkbench();
    const calls = [];
    const storedRows = [];
    const rows = Array.from({ length: 300 }, (_unused, index) => [{ kind: 'literal', value: String(index) }]);
    const events = [
        { type: 'head', version: 1 },
        { type: 'view', id: 'contexts' },
        { type: 'vars', values: ['context'] },
        { type: 'rows', values: rows },
        { type: 'end', metadata: { count: rows.length } }
    ];
    workbench.queryStream = {
        async recoverPendingRowStores() {},
        markCurrentRowStoresForRecovery() {},
        createRowStore: async () => ({
            async append(batch) { storedRows.push(...batch); return storedRows.length; },
            async read(start, count) { return storedRows.slice(start, start + count); },
            async count() { return storedRows.length; },
            async dispose() {}
        }),
        async consumeNdjsonResponse(response, options) {
            calls.push('reader');
            for (const record of events) {
                await options.onRecord(record);
            }
            return { type: 'end' };
        }
    };
    const document = {
        location: { href: 'https://example.test/workbench/repositories/repo-1/contexts' },
        body: { classList: { add() {} } },
        documentElement: { setAttribute() {} },
        readyState: 'complete',
        getElementById() { return null; }
    };
    workbench.__testWindow.document = document;
    workbench.__testWindow.localStorage = { getItem() { return null; }, setItem() {} };
    workbench.__testWindow.matchMedia = () => ({ matches: false });
    const mount = {
        ownerDocument: document,
        getAttribute(name) {
            return {
                'data-workbench-fetch-page-model': 'true',
                'data-workbench-view': 'contexts',
                'data-workbench-base-path': '/workbench',
                'data-workbench-repository-id': 'repo-1'
            }[name] || null;
        }
    };

    const result = await workbench.app.bootstrap(mount, {
        fetch(_url, options) {
            calls.push(options.headers.Accept);
            return Promise.resolve({ ok: true, records: events });
        },
        runtime: fakeRuntime(),
        skipScripts: true
    });

    assert.equal(result.status, 'rendered');
    assert.deepEqual(calls, ['application/vnd.rdf4j.workbench+ndjson', 'reader']);
    assert.equal(await result.model.rowStore.count(), 300);
    assert.equal(result.model.rows.length, 0, 'route result rows must not be retained in the main-thread model');
    const firstRow = await result.model.rowStore.read(0, 1);
    assert.equal(firstRow[0][0].value, '0', 'all streamed rows remain available from the shared row store');
});

test('page bootstrap preserves row-store resources through BFCache and releases them on destructive pagehide', async () => {
    const workbench = loadWorkbench();
    const stores = installTestStreamRuntime(workbench);
    const lifecycle = [];
    const pagehideListeners = [];
    const window = workbench.__testWindow;
    window.addEventListener = (type, listener, options) => {
        if (type === 'pagehide') {
            pagehideListeners.push({ listener, once: !!(options && options.once) });
        }
    };
    window.removeEventListener = (type, listener) => {
        if (type === 'pagehide') {
            const index = pagehideListeners.findIndex((entry) => entry.listener === listener);
            if (index >= 0) {
                pagehideListeners.splice(index, 1);
            }
        }
    };
    window.dispatchPagehide = (event) => {
        const current = pagehideListeners.slice();
        for (const entry of current) {
            entry.listener(event);
            if (entry.once) {
                const index = pagehideListeners.indexOf(entry);
                if (index >= 0) {
                    pagehideListeners.splice(index, 1);
                }
            }
        }
    };
    let markedEvent = null;
    workbench.queryStream.markCurrentRowStoresForRecovery = (event) => {
        markedEvent = event;
        lifecycle.push('mark-for-recovery');
    };
    workbench.views.render = () => ({ status: 'rendered' });
    workbench.views.bindRowWindows = () => Promise.resolve(() => lifecycle.push('row-window-dispose'));

    const events = [
        { type: 'head', version: 1 },
        { type: 'view', id: 'contexts' },
        { type: 'vars', values: ['context'] },
        { type: 'rows', values: [[{ kind: 'iri', value: 'urn:kept' }]] },
        { type: 'end' }
    ];
    const document = {
        location: { href: 'https://example.test/workbench/repositories/repo-1/contexts' },
        body: { classList: { add() {} } },
        documentElement: { setAttribute() {} },
        readyState: 'complete',
        getElementById() { return null; }
    };
    window.document = document;
    window.localStorage = { getItem() { return null; }, setItem() {} };
    window.matchMedia = () => ({ matches: false });
    const mount = {
        ownerDocument: document,
        getAttribute(name) {
            return {
                'data-workbench-fetch-page-model': 'true',
                'data-workbench-view': 'contexts',
                'data-workbench-base-path': '/workbench',
                'data-workbench-repository-id': 'repo-1'
            }[name] || null;
        }
    };

    const result = await workbench.app.bootstrap(mount, {
        fetch: () => Promise.resolve(pageResponse(events)),
        runtime: fakeRuntime(),
        skipScripts: true
    });
    const store = stores[0];
    const dispose = store.dispose.bind(store);
    store.dispose = async () => {
        lifecycle.push('row-store-dispose');
        return dispose();
    };

    assert.equal(result.status, 'rendered');
    assert.equal(pagehideListeners.length, 1, 'the page should retain one lifecycle listener');
    window.dispatchPagehide({ persisted: true });
    assert.equal(store.disposed, false, 'BFCache pagehide must preserve the row store');
    assert.deepEqual(lifecycle, [], 'BFCache pagehide must preserve row-window listeners and avoid recovery marking');
    assert.equal(pagehideListeners.length, 1, 'the lifecycle listener must remain armed after a BFCache pagehide');

    const destructiveEvent = { persisted: false };
    window.dispatchPagehide(destructiveEvent);
    await Promise.resolve();
    assert.deepEqual(lifecycle, ['mark-for-recovery', 'row-window-dispose', 'row-store-dispose'],
        'destructive pagehide marks stores before disposing listeners and row data');
    assert.equal(markedEvent, destructiveEvent, 'recovery receives the actual destructive pagehide event');
    assert.equal(store.disposed, true, 'ordinary pagehide must retain the existing cleanup behavior');
    assert.equal(pagehideListeners.length, 0, 'the released page no longer needs its lifecycle listener');
});

test('page bootstrap recovers pending row stores before routing without creating a row store', async () => {
    const workbench = loadWorkbench();
    const stores = installTestStreamRuntime(workbench);
    const order = [];
    let fetchCount = 0;
    workbench.queryStream.recoverPendingRowStores = async () => { order.push('recover'); };
    const mount = {
        getAttribute(name) {
            return {
                'data-workbench-fetch-page-model': 'false',
                'data-workbench-base-path': '/workbench'
            }[name] || null;
        }
    };

    const result = await workbench.app.bootstrap(mount, {
        fetch() { fetchCount++; return Promise.reject(new Error('non-GET shells do not fetch route data')); },
        runtime: fakeRuntime(),
        skipScripts: true
    });

    assert.equal(result.status, 'skipped');
    assert.deepEqual(order, ['recover'], 'startup recovery runs before route eligibility is considered');
    assert.equal(stores.length, 0, 'recovery works on a route that has not created a row store');
    assert.equal(fetchCount, 0, 'recovery must not replay or fetch a non-GET route');
});

test('page bootstrap restores same-entry scroll after streamed row-window binding', async () => {
    const sessionValues = new Map();
    const pageUrl = 'https://example.test/workbench/repositories/repo-1/contexts';
    const scrollCalls = [];
    const events = [
        { type: 'head', version: 1 },
        { type: 'view', id: 'contexts' },
        { type: 'vars', values: ['context'] },
        { type: 'rows', values: Array.from({ length: 320 }, (_unused, index) => [
            { kind: 'iri', value: 'urn:context:' + index }
        ]) },
        { type: 'end' }
    ];

    async function openPage(navigationType, startingScroll) {
        const workbench = loadWorkbench();
        const stores = installTestStreamRuntime(workbench);
        const order = [];
        const pagehideListeners = [];
        const window = workbench.__testWindow;
        window.scrollY = startingScroll;
        window.pageYOffset = startingScroll;
        window.navigation = { currentEntry: { key: 'contexts-history-entry-1' } };
        window.performance = {
            getEntriesByType(type) {
                return type === 'navigation' ? [{ type: navigationType }] : [];
            }
        };
        window.sessionStorage = {
            getItem(key) { return sessionValues.has(key) ? sessionValues.get(key) : null; },
            setItem(key, value) { sessionValues.set(key, String(value)); },
            removeItem(key) { sessionValues.delete(key); }
        };
        window.scrollTo = (x, y) => {
            order.push('scrollTo');
            scrollCalls.push([x, y]);
            window.scrollY = y;
            window.pageYOffset = y;
        };
        window.addEventListener = (type, listener) => {
            if (type === 'pagehide') { pagehideListeners.push(listener); }
        };
        window.removeEventListener = (type, listener) => {
            if (type === 'pagehide') {
                const index = pagehideListeners.indexOf(listener);
                if (index >= 0) { pagehideListeners.splice(index, 1); }
            }
        };
        window.dispatchPagehide = (event) => pagehideListeners.slice().forEach((listener) => listener(event));
        const document = {
            location: { href: pageUrl },
            body: { classList: { add() {} } },
            documentElement: { setAttribute() {} },
            readyState: 'complete',
            getElementById() { return null; }
        };
        window.document = document;
        window.localStorage = { getItem() { return null; }, setItem() {} };
        window.matchMedia = () => ({ matches: false });
        workbench.views.render = () => { order.push('render'); return { status: 'rendered' }; };
        workbench.views.bindRowWindows = () => {
            order.push('row-window-bound');
            return Promise.resolve(() => order.push('row-window-dispose'));
        };
        const mount = {
            ownerDocument: document,
            getAttribute(name) {
                return {
                    'data-workbench-fetch-page-model': 'true',
                    'data-workbench-view': 'contexts',
                    'data-workbench-base-path': '/workbench',
                    'data-workbench-repository-id': 'repo-1'
                }[name] || null;
            }
        };
        const result = await workbench.app.bootstrap(mount, {
            fetch: () => Promise.resolve(pageResponse(events)),
            runtime: fakeRuntime(),
            skipScripts: true
        });
        return { result, stores, order, window };
    }

    const initialPage = await openPage('navigate', 0);
    assert.equal(initialPage.result.status, 'rendered');
    assert.deepEqual(scrollCalls, [], 'ordinary navigation keeps the browser default scroll behavior');
    assert.equal(await initialPage.result.model.rowStore.count(), 320);
    initialPage.window.scrollY = 13432;
    initialPage.window.pageYOffset = 13432;
    initialPage.window.dispatchPagehide({ persisted: false });
    assert.equal(initialPage.stores[0].disposed, true, 'the non-BFCache page still releases its old worker store');

    const returnedPage = await openPage('back_forward', 0);

    assert.equal(returnedPage.result.status, 'rendered');
    assert.equal(await returnedPage.result.model.rowStore.count(), 320,
        'the returned page must rebuild its streamed rows before restoring scroll');
    assert.deepEqual(scrollCalls, [[0, 13432]], 'history traversal restores the saved scroll position');
    assert.ok(returnedPage.order.indexOf('scrollTo') > returnedPage.order.indexOf('row-window-bound'),
        'scroll restoration runs after the streamed row window establishes document height');
    assert.equal(sessionValues.size, 0, 'the consumed history-entry position is removed from session storage');
});

test('legacy onload handlers run exactly once when bootstrap finishes before native load', async () => {
    const workbench = loadWorkbench();
    installTestStreamRuntime(workbench);
    const window = workbench.__testWindow;
    const listeners = [];
    let calls = 0;
    window.onload = () => { calls++; };
    window.addEventListener = (type, handler) => {
        if (type === 'load') { listeners.push(handler); }
    };
    window.document = {
        location: { href: 'https://example.test/workbench/repositories/repo-1/contexts' },
        body: { classList: { add() {} } },
        documentElement: { setAttribute() {} },
        readyState: 'loading',
        getElementById() { return null; }
    };
    window.localStorage = { getItem() { return null; }, setItem() {} };
    window.matchMedia = () => ({ matches: false });
    const source = encodeEvents([
        { type: 'head', version: 1 }, { type: 'view', id: 'contexts' }, { type: 'end' }
    ]);
    const mount = {
        ownerDocument: window.document,
        getAttribute(name) {
            return {
                'data-workbench-fetch-page-model': 'true',
                'data-workbench-view': 'contexts',
                'data-workbench-base-path': '/workbench'
            }[name] || null;
        }
    };
    await workbench.app.bootstrap(mount, {
        fetch: () => Promise.resolve(pageResponse(decodeEvents(source))),
        runtime: fakeRuntime(),
        skipScripts: true
    });

    const nativeLoad = () => {
        if (typeof window.onload === 'function') { window.onload(); }
        listeners.slice().forEach((listener) => listener());
    };
    nativeLoad();

    assert.equal(calls, 1);
});

test('legacy onload handlers run exactly once when bootstrap finishes after native load', async () => {
    const workbench = loadWorkbench();
    installTestStreamRuntime(workbench);
    const window = workbench.__testWindow;
    let calls = 0;
    window.onload = () => { calls++; };
    window.document = {
        location: { href: 'https://example.test/workbench/repositories/repo-1/contexts' },
        body: { classList: { add() {} } },
        documentElement: { setAttribute() {} },
        readyState: 'complete',
        getElementById() { return null; }
    };
    window.localStorage = { getItem() { return null; }, setItem() {} };
    window.matchMedia = () => ({ matches: false });
    const source = encodeEvents([
        { type: 'head', version: 1 }, { type: 'view', id: 'contexts' }, { type: 'end' }
    ]);
    const mount = {
        ownerDocument: window.document,
        getAttribute(name) {
            return {
                'data-workbench-fetch-page-model': 'true',
                'data-workbench-view': 'contexts',
                'data-workbench-base-path': '/workbench'
            }[name] || null;
        }
    };
    await workbench.app.bootstrap(mount, {
        fetch: () => Promise.resolve(pageResponse(decodeEvents(source))),
        runtime: fakeRuntime(),
        skipScripts: true
    });

    assert.equal(calls, 1);
});

test('route table windowing reads only visible rows and advances through the complete row store', async () => {
    const workbench = loadWorkbench();
    const reads = [];
    const rows = Array.from({ length: 500 }, (_unused, index) => [{ kind: 'literal', value: String(index) }]);
    const rowStore = {
        async read(start, count) {
            reads.push({ start, count });
            return rows.slice(start, start + count);
        },
        async count() { return rows.length; }
    };
    const tbody = { template: null };
    const table = {
        getAttribute(name) {
            if (name === 'data-workbench-row-table') { return 'true'; }
            return '';
        },
        querySelector(selector) { return selector === 'tbody' ? tbody : null; },
        getBoundingClientRect() { return { top: -window.scrollY, height: rows.length * 44 }; }
    };
    const mount = {
        querySelectorAll(selector) {
            return selector.indexOf('data-workbench-row-table') >= 0 ? [table] : [];
        }
    };
    const window = workbench.__testWindow;
    const listeners = {};
    window.innerHeight = 180;
    window.scrollY = 0;
    window.addEventListener = (type, listener) => { listeners[type] = listener; };
    window.removeEventListener = () => {};
    const model = {
        viewId: 'contexts', vars: ['context'], rows: [], rowStart: 0,
        rowCount: rows.length, rowStore, namespaceMap: {}, links: [], metadata: {}
    };
    const runtime = fakeRuntime();

    assert.equal(typeof workbench.views.bindRowWindows, 'function');
    const dispose = await workbench.views.bindRowWindows(mount, model,
        { basePath: '/workbench', repositoryId: 'repo-1', workbench: {} }, runtime);
    assert.ok(reads.length > 0);
    assert.equal(reads[0].start, 0);
    assert.ok(reads[0].count < rows.length);
    assert.equal(model.rows[0][0].value, '0');
    assert.ok(mount.template, 'the current result window should render into the route mount');

    window.scrollY = 4000;
    await listeners.scroll();
    assert.ok(reads.some((read) => read.start > 0), 'scrolling should request a later row window');
    assert.ok(model.rows.every((row) => row[0].kind === 'literal'));
    assert.ok(model.rows.length < rows.length);
    dispose();
});

test('delete repository choices expose bounded row-window navigation', () => {
    const workbench = loadWorkbench();
    const runtime = fakeRuntime();
    const rows = Array.from({ length: 250 }, (_unused, index) => [
        'repo-' + index, 'Repository ' + index
    ]);
    const output = collectTemplateText(workbench.views.pageTemplate({
        viewId: 'delete', vars: ['id', 'description'], rows: [], rowStart: 0,
        rowCount: rows.length, pickerRows: rows.slice(0, 50), pickerStart: 0, pickerPageSize: 50,
        metadata: { selectedRepositoryId: '' }
    }, { basePath: '/workbench', repositoryId: '', workbench: {} }, runtime)).join(' ');

    assert.ok(output.includes('data-workbench-window-picker=') && output.includes('repositories'),
        'the repository chooser should declare its bounded picker contract');
    assert.ok(output.includes('data-workbench-window-action="next"'),
        'repository choices beyond the first page should remain reachable');
    assert.ok(output.includes('Showing ') && output.includes(' of ') && output.includes('repositories')
        && output.includes('250'),
        'the current repository window should be announced');
    assert.ok(output.includes('repo-49'));
    assert.ok(!output.includes('repo-50'), 'later repository options must not be materialized in the DOM');
});

test('page tables label their columns for people instead of showing raw variable names', () => {
    const workbench = loadWorkbench();
    const runtime = fakeRuntime();
    const context = { basePath: '/workbench', repositoryId: 'repo-1', workbench: {} };
    const headings = (template) => {
        const markup = flattenTemplateMarkup(template);
        return Array.from(markup.matchAll(/<th scope="col"[^>]*>([^<]*)<\/th>/g)).map((match) => match[1].trim());
    };
    const repositories = workbench.views.pageTemplate({
        viewId: 'repositories', vars: ['readable', 'writeable', 'id', 'description', 'location'],
        rows: [[true, true, 'repo-1', 'Repository one', 'http://example.test/repositories/repo-1']],
        rowCount: 1, metadata: {}
    }, context, runtime);
    assert.deepEqual(headings(repositories), ['Readable', 'Writeable', 'Id', 'Description', 'Location']);
    const explore = workbench.views.pageTemplate({
        viewId: 'explore', vars: ['subject', 'predicate', 'object', 'context'],
        rows: [[{ kind: 'iri', value: 'urn:s' }, { kind: 'iri', value: 'urn:p' }, { kind: 'iri', value: 'urn:o' }, null]],
        rowCount: 1, metadata: { resource: '<urn:s>' }
    }, context, runtime);
    assert.deepEqual(headings(explore), ['Subject', 'Predicate', 'Object', 'Context']);
});

test('summary and information render key/value lists instead of simple tables', () => {
    const workbench = loadWorkbench();
    const runtime = fakeRuntime();
    const context = { basePath: '/workbench', repositoryId: 'repo-1', workbench: {} };
    const summary = flattenTemplateMarkup(workbench.views.pageTemplate({
        viewId: 'summary', vars: ['id', 'description', 'location', 'server', 'size', 'contexts'],
        rows: [['repo-1', 'Repository one', 'http://example.test/repositories/repo-1', 'http://example.test', '42', '2']],
        rowCount: 1, metadata: {}
    }, context, runtime));
    assert.match(summary, /<dl class="workbench-kv"/);
    assert.match(summary, /<div class="workbench-kv__row"><dt>Statements<\/dt>|<dt>/);
    assert.doesNotMatch(summary, /table class="simple"/);
    const information = flattenTemplateMarkup(workbench.views.pageTemplate({
        viewId: 'information', vars: ['version', 'os', 'jvm', 'user', 'memory-used', 'maximum-memory'],
        rows: [['6.2.0', 'macOS', 'Java 25', 'tester', '10 MB', '20 MB']], rowCount: 1, metadata: {}
    }, context, runtime));
    assert.match(information, /<dl class="workbench-kv"/);
    assert.doesNotMatch(information, /table class="simple"/);
    assert.doesNotMatch(information, /<th>[^<]*:<\/th>/);
});

test('Remove and Clear warn with a warning callout', () => {
    const workbench = loadWorkbench();
    const runtime = fakeRuntime();
    const context = { basePath: '/workbench', repositoryId: 'repo-1', workbench: {} };
    for (const viewId of ['remove', 'clear']) {
        const markup = flattenTemplateMarkup(workbench.views.pageTemplate({
            viewId, vars: [], rows: [], rowCount: 0, metadata: {}
        }, context, runtime));
        assert.match(markup, /class="workbench-callout workbench-callout--warning"/, viewId);
        assert.match(markup, /role="note"/, viewId);
        assert.doesNotMatch(markup, /class="WARN"/, viewId);
    }
});

test('a page error renders an error callout inside the page surface', () => {
    const workbench = loadWorkbench();
    const runtime = fakeRuntime();
    const context = { basePath: '/workbench', repositoryId: 'repo-1', workbench: {} };
    const markup = flattenTemplateMarkup(workbench.views.pageTemplate({
        viewId: 'remove', vars: [], rows: [], rowCount: 0, metadata: { 'error-message': 'No values' }
    }, context, runtime));
    assert.match(markup, /class="workbench-callout workbench-callout--error" role="alert"/);
    assert.match(markup, /No values/);
});

test('a model without a menu renders an empty menu instead of a built-in fallback', () => {
    const workbench = loadWorkbench();
    const runtime = fakeRuntime();
    const errors = [];
    const originalError = console.error;
    console.error = (message) => errors.push(String(message));
    try {
        const template = workbench.views.pageTemplate({
            viewId: 'server', vars: [], rows: [], metadata: {}
        }, { basePath: '/workbench', repositoryId: 'NONE', workbench: {} }, runtime);
        const output = collectTemplateText(template).join(' ');
        assert.equal((output.match(/data-workbench-nav-href/g) || []).length, 0);
        assert.ok(!output.includes('SPARQL Update'), 'the hard-coded fallback menu must not render');
    } finally {
        console.error = originalError;
    }
    assert.deepEqual(errors, ['Workbench menu is unavailable']);
});
