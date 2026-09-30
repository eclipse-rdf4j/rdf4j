const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const vm = require('node:vm');
const { spawnSync } = require('node:child_process');
const { FakeDocument } = require('./browser-fakes.js');

const streamSource = path.resolve(__dirname,
    '../../tools/workbench/src/main/webapp/scripts/ts/queryStream.ts');
const disclosureSource = path.resolve(__dirname,
    '../../tools/workbench/src/main/webapp/scripts/ts/template.ts');

function loadQueryStreamApi(workbench = {}) {
    const outputDirectory = fs.mkdtempSync(path.join(os.tmpdir(), 'rdf4j-query-stream-refinements-'));
    const outputPath = path.join(outputDirectory, 'queryStream.js');
    const compilation = spawnSync('tsc', [
        '--target', 'ES2017',
        '--lib', 'ES2017,DOM',
        '--skipLibCheck',
        '--outFile', outputPath,
        disclosureSource,
        streamSource
    ], { encoding: 'utf8' });

    assert.equal(compilation.status, 0,
        `queryStream.ts must compile for its in-repo streaming contracts:\n${compilation.stdout}${compilation.stderr}`);

    const sessionValues = new Map();
    const localValues = new Map();
    const resizeListeners = new Set();
    const disclosureObservers = [];
    const testWindow = {
        addEventListener(type, listener) {
            if (type === 'resize') resizeListeners.add(listener);
        },
        removeEventListener(type, listener) {
            if (type === 'resize') resizeListeners.delete(listener);
        },
        dispatchEvent() { return true; },
        requestAnimationFrame(callback) { callback(); },
        getComputedStyle(element) {
            return Object.assign({
                boxSizing: 'border-box', paddingTop: '12px', paddingBottom: '12px',
                borderTopWidth: '1px', borderBottomWidth: '1px',
                marginBlockStart: '8px', marginBlockEnd: '0px', direction: 'ltr'
            }, element && element.style || {});
        },
        matchMedia() { return { matches: true }; },
        ResizeObserver: class {
            constructor(callback) {
                this.callback = callback;
                this.observed = new Set();
                this.disconnected = false;
                disclosureObservers.push(this);
            }
            observe(element) { this.observed.add(element); }
            unobserve(element) { this.observed.delete(element); }
            disconnect() { this.disconnected = true; this.observed.clear(); }
        },
        sessionStorage: {
            getItem(key) { return sessionValues.has(key) ? sessionValues.get(key) : null; },
            setItem(key, value) { sessionValues.set(key, String(value)); },
            removeItem(key) { sessionValues.delete(key); }
        },
        localStorage: {
            get length() { return localValues.size; },
            key(index) { return [...localValues.keys()][index] || null; },
            getItem(key) { return localValues.has(key) ? localValues.get(key) : null; },
            setItem(key, value) { localValues.set(key, String(value)); },
            removeItem(key) { localValues.delete(key); }
        }
    };
    const context = vm.createContext({
        workbench,
        window: testWindow,
        document: { activeElement: null, cookie: '' },
        Event: class { constructor(type) { this.type = type; } },
        TextDecoder: require('node:util').TextDecoder,
        URLSearchParams
    });
    vm.runInContext(fs.readFileSync(outputPath, 'utf8'), context, { filename: outputPath });
    context.workbench.queryStream.__testWindow = testWindow;
    context.workbench.queryStream.__testSessionValues = sessionValues;
    context.workbench.queryStream.__testLocalValues = localValues;
    context.workbench.queryStream.__testResizeListeners = resizeListeners;
    context.workbench.queryStream.__testDisclosureObservers = disclosureObservers;
    return context.workbench.queryStream;
}

class InMemoryWorker {
    constructor() {
        this.rows = [];
        this.messages = [];
        this.storeId = 'test-store';
        this.listeners = new Map();
        this.terminated = false;
    }

    addEventListener(type, listener) {
        const listeners = this.listeners.get(type) || [];
        listeners.push(listener);
        this.listeners.set(type, listeners);
    }

    removeEventListener(type, listener) {
        this.listeners.set(type, (this.listeners.get(type) || []).filter(value => value !== listener));
    }

    postMessage(message) {
        this.messages.push(message);
        queueMicrotask(() => {
            let response = { requestId: message.requestId, ok: true };
            if (message.op === 'create') {
                response.storeId = this.storeId;
            } else if (message.op === 'append') {
                this.rows.push(...message.rows);
                response.count = this.rows.length;
            } else if (message.op === 'read') {
                response.rows = this.rows.slice(message.start, message.start + message.count);
                response.count = this.rows.length;
            } else if (message.op === 'count') {
                response.count = this.rows.length;
            } else if (message.op === 'dispose') {
                this.rows = [];
                response.disposed = true;
            }
            for (const listener of this.listeners.get('message') || []) {
                listener({ data: response });
            }
        });
    }

    terminate() {
        this.terminated = true;
    }
}

function inMemoryRowStore() {
    let rows = [];
    return {
        async append(batch) { rows.push(...batch); return rows.length; },
        async read(start, count) { return rows.slice(start, start + count); },
        async count() { return rows.length; },
        async dispose() { rows = []; }
    };
}

test('generic NDJSON reader awaits each route-data callback before reading further', async () => {
    const queryStream = loadQueryStreamApi();
    assert.equal(typeof queryStream.consumeNdjsonResponse, 'function',
        'page and query streaming must share one generic framing reader');

    const records = [
        { type: 'head', version: 1 },
        { type: 'view', id: 'saved-queries' },
        { type: 'vars', values: ['name', 'query'] },
        { type: 'rows', values: [['alpha', 'SELECT * {}']] },
        { type: 'end', metadata: { 'total-result-count': 1 } }
    ];
    let readCount = 0;
    let unblockRows;
    let rowCallbackEntered;
    const entered = new Promise(resolve => { rowCallbackEntered = resolve; });
    const gate = new Promise(resolve => { unblockRows = resolve; });
    const response = {
        ok: true,
        body: {
            getReader() {
                return {
                    async read() {
                        if (readCount >= records.length) {
                            return { done: true };
                        }
                        const line = JSON.stringify(records[readCount++]) + '\n';
                        return { done: false, value: new TextEncoder().encode(line) };
                    },
                    async cancel() {}
                };
            }
        }
    };

    const consumed = queryStream.consumeNdjsonResponse(response, {
        async onRecord(record) {
            if (record.type === 'rows') {
                rowCallbackEntered();
                await gate;
            }
        }
    });
    await entered;
    assert.equal(readCount, 4, 'the reader must pause while the row-store append is pending');
    unblockRows();
    const outcome = await consumed;
    assert.equal(outcome.type, 'end');
    assert.equal(readCount, records.length);
});

test('worker row store appends in order, reads bounded windows, disposes, and reports quota errors', async () => {
    const queryStream = loadQueryStreamApi();
    assert.equal(typeof queryStream.createRowStore, 'function',
        'unlimited row data must live behind the worker-owned store API');

    const worker = new InMemoryWorker();
    const store = await queryStream.createRowStore({
        workerUrl: '/scripts/queryStreamWorker.js',
        workerFactory: () => worker
    });
    const batch = Array.from({ length: 10000 }, (_, index) => [
        { kind: 'literal', value: String(index) }
    ]);
    assert.equal(await store.append(batch), 10000);
    assert.deepEqual(JSON.parse(JSON.stringify(await store.read(4375, 4))), [
        [{ kind: 'literal', value: '4375' }],
        [{ kind: 'literal', value: '4376' }],
        [{ kind: 'literal', value: '4377' }],
        [{ kind: 'literal', value: '4378' }]
    ]);
    assert.equal(await store.count(), 10000);
    await store.dispose();
    assert.equal(worker.terminated, true);
    await assert.rejects(store.append([[null]]), /disposed/i);

    const failingWorker = new InMemoryWorker();
    failingWorker.postMessage = function(message) {
        queueMicrotask(() => {
            for (const listener of this.listeners.get('message') || []) {
                listener({ data: { requestId: message.requestId, ok: false, error: 'QuotaExceededError' } });
            }
        });
    };
    await assert.rejects(
        queryStream.createRowStore({ workerFactory: () => failingWorker }), /quota|QuotaExceededError/i);
});

test('new same-tab row stores reclaim only stores marked by a destructive pagehide', async () => {
    const queryStream = loadQueryStreamApi();
    assert.equal(typeof queryStream.markCurrentRowStoresForRecovery, 'function',
        'destructive navigation must persist a recovery marker before the worker can be interrupted');

    const oldPageWorker = new InMemoryWorker();
    oldPageWorker.storeId = 'old-page-store';
    const oldPageStore = await queryStream.createRowStore({ workerFactory: () => oldPageWorker });
    await oldPageStore.append([[{ kind: 'literal', value: 'old page row' }]]);
    queryStream.markCurrentRowStoresForRecovery({ persisted: true });
    assert.equal(queryStream.__testLocalValues.size, 0,
        'a BFCache pagehide must not write any durable deletion marker');
    queryStream.markCurrentRowStoresForRecovery({ persisted: false });

    const pending = [...queryStream.__testLocalValues.keys()];
    assert.deepEqual(pending, ['rdf4j.workbench.query-results.pending-disposal.v1:old-page-store']);

    const newPageWorker = new InMemoryWorker();
    newPageWorker.storeId = 'new-page-store';
    const newPageStore = await queryStream.createRowStore({ workerFactory: () => newPageWorker });
    assert.ok(newPageWorker.messages.some(message => message.op === 'dispose'
        && message.storeId === 'old-page-store'), 'recovery must delete the old store by its exact ID');
    assert.equal(newPageStore.id, 'new-page-store');
    assert.equal(queryStream.__testLocalValues.size, 0,
        'successfully reclaimed IDs must not be deleted again by later documents');
});

test('bootstrap reclaims persisted row stores before any route rows are loaded', async () => {
    const queryStream = loadQueryStreamApi();
    const prefix = 'rdf4j.workbench.query-results.pending-disposal.v1:';
    queryStream.__testWindow.localStorage.setItem(`${prefix}closed-tab-store`, '1');
    queryStream.__testWindow.localStorage.setItem(`${prefix}other-tab-store`, '2');
    const workers = [];

    await queryStream.recoverPendingRowStores({ workerFactory: () => {
        const worker = new InMemoryWorker();
        worker.storeId = 'temporary-bootstrap-store';
        workers.push(worker);
        return worker;
    } });

    assert.equal(workers.length, 1, 'bootstrap should use one worker only when a recovery marker exists');
    assert.deepEqual(workers[0].messages.map(message => [message.op, message.storeId]), [
        ['dispose', 'closed-tab-store'],
        ['dispose', 'other-tab-store']
    ], 'boot recovery should dispose exact stale IDs without creating a throwaway store');
    assert.equal(queryStream.__testLocalValues.size, 0);

    let workerCreated = false;
    await queryStream.recoverPendingRowStores({ workerFactory: () => {
        workerCreated = true;
        return new InMemoryWorker();
    } });
    assert.equal(workerCreated, false, 'ordinary boot with no markers should not create an extra worker');
});

test('failed boot recovery preserves the marker for a later same-origin boot', async () => {
    const queryStream = loadQueryStreamApi();
    const key = 'rdf4j.workbench.query-results.pending-disposal.v1:store-after-quota-error';
    queryStream.__testWindow.localStorage.setItem(key, 'pending');
    const worker = new InMemoryWorker();
    worker.postMessage = function(message) {
        this.messages.push(message);
        queueMicrotask(() => {
            for (const listener of this.listeners.get('message') || []) {
                listener({ data: { requestId: message.requestId, ok: false, error: 'QuotaExceededError' } });
            }
        });
    };

    await assert.rejects(queryStream.recoverPendingRowStores({ workerFactory: () => worker }),
        /quota|QuotaExceededError/i);
    assert.equal(queryStream.__testWindow.localStorage.getItem(key), 'pending',
        'recovery markers are removed only after the worker confirms disposal');
    assert.equal(worker.terminated, true);
});

test('variable-height virtualization uses measured row prefixes and stays bounded at scale', () => {
    const queryStream = loadQueryStreamApi();
    assert.equal(typeof queryStream.MeasuredRowHeights, 'function',
        'virtual scrolling must index measured variable row heights rather than a fixed rowHeight');

    const heights = new queryStream.MeasuredRowHeights(100000, 24);
    heights.measure(0, 20);
    heights.measure(1, 65);
    heights.measure(2, 31);
    const range = heights.range(50, 80, 2, 32);
    assert.equal(range.start, 0);
    assert.equal(range.end > range.start, true);
    assert.equal(range.end - range.start <= 32, true);
    assert.equal(range.topSpacer, 0);
    assert.equal(range.bottomSpacer > 0, true);
    assert.equal(heights.offsetOf(3), 116, 'measured wrapped rows must contribute their actual heights');
    assert.equal(heights.measurementCount, 3,
        'height metadata must stay sparse instead of allocating one entry per unlimited row');
});

test('measured row samples refine the estimate used for unvisited rows', () => {
    const queryStream = loadQueryStreamApi();
    const heights = new queryStream.MeasuredRowHeights(20000, 1);
    heights.measure(0, 34);
    heights.measure(1, 38);

    assert.equal(heights.setEstimate(38), true,
        'the initial visible rows should calibrate the estimate used for the remaining result');
    assert.equal(heights.offsetOf(20000), 759996,
        'calibration must preserve measured heights while rebasing unvisited rows');
    assert.equal(heights.measurementCount, 2,
        'calibrating a shared estimate must keep only explicitly measured row overrides');
    assert.equal(heights.setEstimate(38), false, 'an unchanged estimate should not rebuild the index');

    const nearEnd = heights.range(38 * 19800, 100, 4, 80);
    assert.equal(nearEnd.start >= 19700, true,
        'a calibrated large result should remain navigable near its final rows');
});

test('automatic layout chooses by measured readable width instead of variable count', () => {
    const queryStream = loadQueryStreamApi();
    assert.equal(typeof queryStream.chooseAutoLayout, 'function');

    assert.equal(queryStream.chooseAutoLayout(340, [92, 118, 105]), 'table');
    assert.equal(queryStream.chooseAutoLayout(300, [92, 118, 105]), 'records');
    assert.equal(queryStream.chooseAutoLayout(340, [92, 118, 105], 'records'), 'records',
        'explicit layout selection must take precedence over automatic measurement');
});

test('typed terms retain explore links, namespace abbreviations, nested triples, and datatype toggle', () => {
    const queryStream = loadQueryStreamApi();
    assert.equal(typeof queryStream.formatRdfTerm, 'function',
        'typed terms must use one formatter for table cells and record cards');

    const iri = queryStream.formatRdfTerm({ kind: 'iri', value: 'http://example.test/ns/item' }, {
        namespaces: [{ prefix: 'ex', name: 'http://example.test/ns/' }]
    });
    assert.equal(iri.label, 'ex:item');
    assert.equal(iri.exploreHref, 'explore?resource=%3Chttp%3A%2F%2Fexample.test%2Fns%2Fitem%3E');
    assert.equal(iri.externalHref, 'http://example.test/ns/item');
    assert.equal(queryStream.formatRdfTerm({ kind: 'iri', value: 'javascript:alert(1)' }).externalHref, undefined,
        'untrusted RDF IRIs must not become executable external links');

    const literal = { kind: 'literal', value: '7', datatype: 'http://www.w3.org/2001/XMLSchema#integer' };
    assert.equal(queryStream.formatRdfTerm(literal, { showDatatypes: true }).label, '7',
        'built-in numeric datatypes retain their lexical display');
    assert.equal(queryStream.formatRdfTerm(literal, { showDatatypes: false }).label, '7');
    assert.equal(queryStream.formatRdfTerm(literal).exploreHref,
        'explore?resource=%227%22%5E%5E%3Chttp%3A%2F%2Fwww.w3.org%2F2001%2FXMLSchema%23integer%3E',
        'typed literal values remain linked to Workbench Explore');
    assert.match(queryStream.formatRdfTerm({
        kind: 'triple',
        subject: { kind: 'iri', value: 'http://example.test/ns/item' },
        predicate: { kind: 'iri', value: 'http://example.test/ns/knows' },
        object: literal
    }, { namespaces: [{ prefix: 'ex', name: 'http://example.test/ns/' }] }).label, /<< ex:item ex:knows/);
});

test('RDF term output matches Workbench Explore, namespace, and datatype presentation', () => {
    const queryStream = loadQueryStreamApi();
    const namespaces = [
        { prefix: 'ex', name: 'http://example.test/ns/' },
        { prefix: 'kind', name: 'http://example.test/types/' }
    ];
    function present(term, options = {}) {
        const display = queryStream.formatRdfTerm(term, Object.assign({ namespaces }, options));
        const resourceParameter = display.exploreHref
            ? decodeURIComponent(display.exploreHref.substring(display.exploreHref.indexOf('=') + 1)) : null;
        return {
            label: display.label,
            exploreResource: resourceParameter,
            externalHref: display.externalHref || null,
            preformatted: display.preformatted === true
        };
    }

    const typed = { kind: 'literal', value: 'widget', datatype: 'http://example.test/types/Code' };
    const xmlLiteral = {
        kind: 'literal', value: '<item/>',
        datatype: 'http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral'
    };
    const actual = [
        present({ kind: 'iri', value: 'urn:unmapped:value' }),
        present({ kind: 'iri', value: 'http://example.test/ns/item' }),
        present({ kind: 'literal', value: 'plain' }),
        present({ kind: 'literal', value: 'bonjour', language: 'fr' }),
        present({ kind: 'literal', value: 'bonjour', language: 'fr' }, { showDatatypes: false }),
        present(typed),
        present(typed, { showDatatypes: false }),
        present(xmlLiteral),
        present({ kind: 'literal', value: 'first line\nsecond line' })
    ];
    assert.deepEqual(actual, [
        {
            label: '<urn:unmapped:value>', exploreResource: '<urn:unmapped:value>',
            externalHref: null, preformatted: false
        },
        {
            label: 'ex:item', exploreResource: '<http://example.test/ns/item>',
            externalHref: 'http://example.test/ns/item', preformatted: false
        },
        { label: '"plain"', exploreResource: '"plain"', externalHref: null, preformatted: false },
        {
            label: '"bonjour"@fr', exploreResource: '"bonjour"@fr', externalHref: null, preformatted: false
        },
        { label: '"bonjour"', exploreResource: '"bonjour"@fr', externalHref: null, preformatted: false },
        {
            label: '"widget"^^kind:Code', exploreResource: '"widget"^^<http://example.test/types/Code>',
            externalHref: null, preformatted: false
        },
        {
            label: '"widget"', exploreResource: '"widget"^^<http://example.test/types/Code>',
            externalHref: null, preformatted: false
        },
        { label: '<item/>', exploreResource: null, externalHref: null, preformatted: true },
        { label: 'first line\nsecond line', exploreResource: null, externalHref: null, preformatted: true }
    ]);
});

test('XML and multiline literals render as preformatted text in table cells', async () => {
    const queryStream = loadQueryStreamApi();
    const document = new FakeDocument();
    const target = document.createElement('section');
    document.body.appendChild(target);
    const renderer = new queryStream.QueryResultRenderer(target, { rowStore: inMemoryRowStore() });
    await renderer.accept({ type: 'view', id: 'tuple' });
    await renderer.accept({ type: 'vars', values: ['xml', 'multiline'] });
    await renderer.accept({ type: 'rows', values: [[
        { kind: 'literal', value: '<item/>', datatype: 'http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral' },
        { kind: 'literal', value: 'first line\nsecond line' }
    ]] });

    const row = renderer.tableBody.children.find(child => child.getAttribute('data-query-row-index') === '0');
    assert.equal(row.children[0].querySelector('pre').textContent, '<item/>');
    assert.equal(row.children[1].querySelector('pre').textContent, 'first line\nsecond line');
    renderer.dispose();
});

test('typed Explore links and datatype visibility stay consistent across table and record layouts', async () => {
    const queryStream = loadQueryStreamApi();
    const document = new FakeDocument();
    const target = document.createElement('section');
    document.body.appendChild(target);
    const renderer = new queryStream.QueryResultRenderer(target, { rowStore: inMemoryRowStore() });
    await renderer.accept({ type: 'view', id: 'tuple' });
    await renderer.accept({ type: 'vars', values: ['code'] });
    await renderer.accept({ type: 'namespaces', values: [{ prefix: 'kind', name: 'http://example.test/types/' }] });
    await renderer.accept({ type: 'rows', values: [[{
        kind: 'literal', value: 'widget', datatype: 'http://example.test/types/Code'
    }]] });
    await renderer.accept({ type: 'end', metadata: { 'total-result-count': 1 } });

    const tableLink = renderer.tableBody.children.find(
        child => child.getAttribute('data-query-row-index') === '0').querySelector('a');
    assert.equal(tableLink.textContent, '"widget"^^kind:Code');
    assert.equal(tableLink.getAttribute('href'),
        'explore?resource=%22widget%22%5E%5E%3Chttp%3A%2F%2Fexample.test%2Ftypes%2FCode%3E');

    renderer.datatypeControl.checked = false;
    renderer.datatypeControl.trigger('change');
    await new Promise(resolve => setImmediate(resolve));
    assert.equal(renderer.tableBody.children.find(
        child => child.getAttribute('data-query-row-index') === '0').querySelector('a').textContent, '"widget"');

    renderer.layoutControl.value = 'records';
    renderer.layoutControl.trigger('change');
    await new Promise(resolve => setImmediate(resolve));
    const recordLink = renderer.records.children[0].querySelector('dd').querySelector('a');
    assert.equal(recordLink.textContent, '"widget"');
    assert.equal(recordLink.getAttribute('href'), tableLink.getAttribute('href'));
    renderer.dispose();
});

test('typed result text remains inert and inline without duplicating the Workbench shell', async () => {
    const queryStream = loadQueryStreamApi();
    const document = new FakeDocument();
    const target = document.createElement('section');
    target.setAttribute('id', 'query-results');
    document.body.appendChild(target);
    const renderer = new queryStream.QueryResultRenderer(target, { rowStore: inMemoryRowStore() });
    const hostileLiteral = '<img src=x onerror=alert(1)>';

    assert.match(renderer.root.className, /query-result-embedded/,
        'streamed results should mount as an inline result surface');
    await renderer.accept({ type: 'view', id: 'tuple' });
    await renderer.accept({ type: 'vars', values: ['value'] });
    await renderer.accept({ type: 'rows', values: [[{ kind: 'literal', value: hostileLiteral }]] });
    await renderer.accept({ type: 'end', metadata: { 'total-result-count': 1 } });

    const cell = renderer.tableBody.children.find(row => row.getAttribute('data-query-row-index') === '0').children[0];
    assert.equal(cell.textContent, '"<img src=x onerror=alert(1)>"');
    assert.equal(cell.querySelector('img'), null,
        'RDF literal markup must be rendered as text, never parsed as HTML');
    assert.equal(target.querySelector('#header'), null);
    assert.equal(target.querySelector('#navigation'), null);
    renderer.dispose();
});

test('tuple, graph, and both boolean results keep Workbench metadata out of visible text', async () => {
    const queryStream = loadQueryStreamApi();
    const document = new FakeDocument();
    const views = [
        { view: 'tuple', variables: ['value'] },
        { view: 'graph', variables: ['subject', 'predicate', 'object', 'context'] },
        { view: 'boolean', booleanValue: true },
        { view: 'boolean', booleanValue: false }
    ];
    const sentinels = [
        'query-request-id-sentinel', 'query-result-status-sentinel', 'query-language-sentinel',
        'query-infer-sentinel', 'query-timeout-sentinel', 'query-count-sentinel'
    ];

    for (const resultCase of views) {
        const target = document.createElement('section');
        document.body.appendChild(target);
        const renderer = new queryStream.QueryResultRenderer(target, { rowStore: inMemoryRowStore() });
        await renderer.accept({ type: 'view', id: resultCase.view });
        if (resultCase.variables) {
            await renderer.accept({ type: 'vars', values: resultCase.variables });
        }
        await renderer.accept({ type: 'metadata', values: {
            'query-request-id': sentinels[0],
            'query-result-status': sentinels[1],
            'query-language': sentinels[2],
            'query-infer': sentinels[3],
            'query-timeout': sentinels[4],
            'total-result-count': 0
        } });
        if (typeof resultCase.booleanValue === 'boolean') {
            await renderer.accept({ type: 'boolean', value: resultCase.booleanValue });
        }
        await renderer.accept({ type: 'end', metadata: { 'total-result-count': 0 } });

        const visibleText = target.textContent;
        sentinels.forEach(sentinel => assert.ok(!visibleText.includes(sentinel),
            `${resultCase.view} metadata ${sentinel} must remain non-visible`));
        assert.match(renderer.root.className, /query-result-embedded/);
        assert.equal(target.querySelector('#header'), null);
        assert.equal(target.querySelector('#navigation'), null);
        assert.equal(target.querySelector('#footer'), null);
        renderer.dispose();
    }
});

test('download limit defaults to All independently of the query batch size', async () => {
    const queryStream = loadQueryStreamApi();
    const document = new FakeDocument();
    const target = document.createElement('section');
    document.body.appendChild(target);
    const renderer = new queryStream.QueryResultRenderer(target, {
        requestedLimit: 1000000,
        rowStore: inMemoryRowStore(),
        workbench: { defaults: { 'default-download-limit': '0' } }
    });
    await renderer.accept({ type: 'view', id: 'tuple' });
    await renderer.accept({ type: 'vars', values: ['value'] });
    await renderer.accept({ type: 'end', metadata: {
        'total-result-count': 11,
        'result-offset': 0,
        'result-limit': 10
    } });

    assert.equal(renderer.downloadLimitControl.value, '0');
    assert.equal(renderer.downloadLimitControl.children.find(option => option.value === '0').textContent, 'All');
    assert.equal(renderer.root.querySelectorAll('select').some(select => select.name === 'stream-result-limit'), false,
        'the download limit is independent of a removed result page-size control');
    assert.equal(renderer.root.querySelectorAll('button').some(button => /Next|Previous/.test(button.textContent)), false);
    renderer.dispose();
});

test('tuple and graph downloads preserve view-specific formats and policy', async () => {
    const queryStream = loadQueryStreamApi();
    const document = new FakeDocument();
    const cases = [
        {
            view: 'tuple',
            variables: ['value'],
            formats: ['text/csv CSV', 'application/sparql-results+json SPARQL JSON'],
            defaultFormat: 'text/csv',
            feature: 'result-download-format-tuple'
        },
        {
            view: 'graph',
            variables: ['subject', 'predicate', 'object', 'context'],
            formats: ['application/n-quads N-Quads', 'text/turtle Turtle'],
            defaultFormat: 'text/turtle',
            feature: 'result-download-format-graph'
        }
    ];

    for (const resultCase of cases) {
        const target = document.createElement('section');
        document.body.appendChild(target);
        const renderer = new queryStream.QueryResultRenderer(target, {
            rowStore: inMemoryRowStore(),
            workbench: {
                tupleDownloadFormats: cases[0].formats,
                graphDownloadFormats: cases[1].formats,
                defaults: { 'default-Accept': resultCase.defaultFormat }
            }
        });
        await renderer.accept({ type: 'view', id: resultCase.view });
        await renderer.accept({ type: 'vars', values: resultCase.variables });
        await renderer.accept({ type: 'end', metadata: { 'total-result-count': 0 } });

        assert.deepEqual(renderer.downloadFormatControl.children.map(option => option.value),
            resultCase.formats.map(format => format.slice(0, format.indexOf(' '))));
        assert.equal(renderer.downloadFormatControl.value, resultCase.defaultFormat);
        assert.equal(renderer.downloadFormatControl.hidden, false);
        renderer.dispose();

        const policyTarget = document.createElement('section');
        document.body.appendChild(policyTarget);
        const policyRenderer = new queryStream.QueryResultRenderer(policyTarget, {
            rowStore: inMemoryRowStore(),
            workbench: {
                tupleDownloadFormats: cases[0].formats,
                graphDownloadFormats: cases[1].formats,
                defaults: { 'default-Accept': resultCase.defaultFormat }
            },
            features: { [resultCase.feature]: false }
        });
        await policyRenderer.accept({ type: 'view', id: resultCase.view });
        await policyRenderer.accept({ type: 'vars', values: resultCase.variables });
        await policyRenderer.accept({ type: 'end', metadata: { 'total-result-count': 0 } });
        assert.equal(policyRenderer.downloadFormatControl.hidden, true,
            `${resultCase.view} format controls should follow their view-specific feature policy`);
        policyRenderer.dispose();
    }
});

test('streamed renderer restores download/options/fullscreen controls and avoids a main-thread row copy', async () => {
    const queryStream = loadQueryStreamApi();
    const document = new FakeDocument();
    const target = document.createElement('section');
    document.body.appendChild(target);
    const renderer = new queryStream.QueryResultRenderer(target, {
        requestedLimit: 1000000,
        batched: true,
        maxDomRows: 20,
        rowStore: inMemoryRowStore()
    });
    assert.ok(renderer.downloadToggle);
    assert.ok(renderer.optionsToggle);
    assert.ok(renderer.fullscreenButton);
    assert.ok(renderer.downloadFormatControl);
    assert.ok(renderer.downloadLimitControl);
    assert.ok(renderer.datatypeControl);

    renderer.beginBatch(0);
    await renderer.accept({ type: 'head', version: 1 });
    await renderer.accept({ type: 'view', id: 'tuple' });
    await renderer.accept({ type: 'vars', values: ['value'] });
    await renderer.accept({ type: 'rows', values: Array.from({ length: 100000 }, (_, index) => [
        { kind: 'literal', value: String(index) }
    ]) });
    await renderer.accept({ type: 'end', metadata: {
        'result-offset': 0,
        'result-limit': 1000000,
        'result-batch-count': 100000,
        'result-has-more': false,
        'result-next-offset': 100000
    } });
    assert.equal(renderer.state.rowCount, 100000);
    assert.equal(Array.isArray(renderer.state.rows), false,
        'the renderer must not keep a duplicate all-results array on the main thread');
    assert.equal(renderer.tableBody.children.length <= 22, true);

    renderer.dispose();
});

test('result toolbar preserves typed native downloads, panels, and fullscreen handoff', async () => {
    const queryStream = loadQueryStreamApi();
    const document = new FakeDocument();
    const target = document.createElement('section');
    target.setAttribute('id', 'query-results');
    const fullscreen = document.createElement('button');
    fullscreen.setAttribute('id', 'query-results-fullscreen');
    fullscreen.setAttribute('data-result-fullscreen-enabled', 'true');
    document.body.appendChild(target);
    document.body.appendChild(fullscreen);
    const executionForm = document.createElement('form');
    executionForm.setAttribute('action', 'query');
    executionForm.setAttribute('method', 'post');
    for (const control of [
        { name: 'action', value: 'exec' },
        { name: 'query', value: 'SELECT ?value WHERE {?s ?p ?value}' }
    ]) {
        const input = document.createElement('input');
        input.name = control.name;
        input.value = control.value;
        executionForm.appendChild(input);
    }
    document.body.appendChild(executionForm);
    const resultFrame = document.createElement('iframe');
    resultFrame.setAttribute('id', 'query-results-frame');
    resultFrame.setAttribute('name', 'query-results-frame');
    document.body.appendChild(resultFrame);
    const renderer = new queryStream.QueryResultRenderer(target, {
        executionForm,
        rowStore: inMemoryRowStore(),
        workbench: {
            tupleDownloadFormats: ['text/csv CSV', 'application/sparql-results+json SPARQL JSON'],
            defaults: { 'default-Accept': 'text/csv', 'default-download-limit': '17' }
        }
    });

    await renderer.accept({ type: 'view', id: 'tuple' });
    await renderer.accept({ type: 'vars', values: ['value'] });
    await renderer.accept({ type: 'rows', values: [[{ kind: 'literal', value: 'typed' }]] });
    await renderer.accept({ type: 'end', metadata: {
        'total-result-count': 1, 'result-offset': 0, 'result-limit': 0
    } });

    assert.equal(renderer.fullscreenButton, fullscreen);
    assert.equal(fullscreen.hidden, false, 'the result view enables the shared shell fullscreen control');
    assert.equal(renderer.downloadFormatControl.value, 'text/csv');
    assert.equal(renderer.downloadLimitControl.value, '17');
	renderer.downloadToggle.trigger('click');
	renderer.optionsToggle.trigger('click');
	assert.equal(renderer.downloadToggle.getAttribute('aria-expanded'), 'false',
		'opening a sibling result panel should close the previously open panel');
	assert.equal(renderer.optionsToggle.getAttribute('aria-expanded'), 'true');
	renderer.downloadToggle.trigger('click');
	target.querySelector('.query-result-download-button').click();

    const download = document.lastSubmittedForm;
    const parameters = new Map(download.formControls.map(input => [input.name, input.value]));
    assert.equal(download.method, 'POST');
    assert.equal(download.action, 'query');
    assert.equal(download.target, 'query-results-frame');
    assert.equal(parameters.get('action'), 'exec');
    assert.equal(parameters.get('Accept'), 'text/csv');
    assert.equal(parameters.get('download_limit'), '17');
    assert.equal(parameters.get('query'), 'SELECT ?value WHERE {?s ?p ?value}');
    assert.equal(Object.prototype.hasOwnProperty.call(download, 'headers'), false,
        'raw result downloads remain browser-native and do not use the stream Accept header');
    renderer.dispose();
});

test('result disclosure panels measure their trigger and stay inside the result mount', async () => {
    const queryStream = loadQueryStreamApi();
    const document = new FakeDocument();
    const target = document.createElement('section');
    document.body.appendChild(target);
	const renderer = new queryStream.QueryResultRenderer(target, { rowStore: inMemoryRowStore() });
	await renderer.accept({ type: 'view', id: 'tuple' });
	await renderer.accept({ type: 'vars', values: ['value'] });
	await renderer.accept({ type: 'rows', values: [[{ kind: 'literal', value: 'visible' }]] });
	await renderer.accept({ type: 'end', metadata: { 'total-result-count': 1 } });
	const panel = document.getElementById(renderer.optionsToggle.getAttribute('aria-controls'));
	const panelTrack = panel.parentNode;
	const observer = queryStream.__testDisclosureObservers[0];
	assert.equal(panelTrack.classList.contains('workbench-disclosure-track'), true);
	assert.equal(observer.observed.has(panelTrack), true,
		'the shared anchor observer should watch the final result panel track');
	assert.equal(observer.observed.has(renderer.optionsToggle.parentNode), false,
		'the observer should not retain the detached trigger owner as the options panel track');
	const rect = (left, top, width, height) => ({
        left, top, width, height, right: left + width, bottom: top + height
    });
    panel.parentNode.getBoundingClientRect = () => rect(100, 10, 800, 600);
    renderer.optionsToggle.getBoundingClientRect = () => rect(840, 10, 80, 36);
    panel.getBoundingClientRect = () => rect(100, 50, 320, 200);

    renderer.optionsToggle.trigger('click');

    assert.equal(panel.style.getPropertyValue('--workbench-disclosure-panel-start'), '480px',
        'the panel should shift left when its trigger is near the result mount edge');
	assert.equal(panel.style.getPropertyValue('--workbench-disclosure-anchor-x'), '300px',
		'the panel arrow should remain centered under its trigger after clamping');
	assert.equal(renderer.optionsToggle.getAttribute('aria-expanded'), 'true');
	renderer.optionsToggle.getBoundingClientRect = () => rect(400, 10, 80, 36);
	observer.callback();
	assert.equal(panel.style.getPropertyValue('--workbench-disclosure-panel-start'), '60px',
		'resizing should refresh the panel position against its final shared track');
	assert.equal(panel.style.getPropertyValue('--workbench-disclosure-anchor-x'), '280px');
	renderer.dispose();
});

test('query result toolbar toggles expose stable semantic classes', () => {
    const queryStream = loadQueryStreamApi();
    const document = new FakeDocument();
    const target = document.createElement('section');
    document.body.appendChild(target);
    const renderer = new queryStream.QueryResultRenderer(target, { rowStore: inMemoryRowStore() });

    assert.equal(renderer.downloadToggle.classList.contains('query-result-download-toggle'), true,
        'Download styling must target a semantic class emitted by the renderer');
    assert.equal(renderer.optionsToggle.classList.contains('query-result-options-toggle'), true,
        'Options styling must target a semantic class emitted by the renderer');
    renderer.dispose();
});

test('result summaries report exact totals and follow explicit batch continuation', async () => {
    const queryStream = loadQueryStreamApi();
    const document = new FakeDocument();

    for (const batch of [
        { rowCount: 2, hasMore: true, total: 4, elapsed: 3500,
            expectedLabel: '2 loaded rows of 4', expectedStatus: '2 loaded rows of 4 results. Complete query: 3500 ms.' },
        { rowCount: 1, hasMore: false, total: 1, elapsed: 2100,
            expectedLabel: '1 loaded rows of 1', expectedStatus: '1 loaded rows of 1 results. Complete query: 2100 ms.' }
    ]) {
        const target = document.createElement('section');
        document.body.appendChild(target);
        const renderer = new queryStream.QueryResultRenderer(target, {
            requestedOffset: 0,
            requestedLimit: 2,
            batched: true,
            rowStore: inMemoryRowStore(),
            onLoadMore() {}
        });
        renderer.beginBatch(0);
        await renderer.accept({ type: 'head', version: 1 });
        await renderer.accept({ type: 'view', id: 'tuple' });
        await renderer.accept({ type: 'vars', values: ['value'] });
        await renderer.accept({ type: 'rows', values: Array.from({ length: batch.rowCount }, (_, index) => [
            { kind: 'literal', value: String(index + 1) }
        ]) });
        await renderer.accept({ type: 'end', metadata: {
            'total-result-count': batch.total,
            'result-offset': 0,
            'result-limit': 2,
            'result-batch-count': batch.rowCount,
            'result-has-more': batch.hasMore,
            'result-next-offset': batch.rowCount,
            'query-elapsed-ms': batch.elapsed,
            'query-result-status': 'completed'
        } });

        assert.equal(renderer.countLabel.textContent, batch.expectedLabel,
            'the summary describes rows loaded in the local result store');
        assert.equal(renderer.status.textContent, batch.expectedStatus,
            'the completed summary includes the exact evaluated total and elapsed time');
        assert.equal(renderer.loadMoreButton.hidden, !batch.hasMore,
            'continuation visibility comes from explicit batch metadata');
        assert.equal(renderer.root.querySelectorAll('button').some(button => /Next|Previous/.test(button.textContent)), false);
        assert.equal(renderer.root.querySelectorAll('select').some(select => select.name === 'stream-result-limit'), false);
        renderer.dispose();
    }
});

test('boolean results preserve their fullscreen-only legacy toolbar', async () => {
    const queryStream = loadQueryStreamApi();
    const document = new FakeDocument();
    const target = document.createElement('section');
    document.body.appendChild(target);
    const renderer = new queryStream.QueryResultRenderer(target, { rowStore: inMemoryRowStore() });
    await renderer.accept({ type: 'view', id: 'boolean' });
    assert.equal(renderer.booleanResult.hidden, true,
        'a Boolean view must not show a provisional false value before its data record arrives');
    assert.equal(renderer.booleanResult.textContent, '');
    await renderer.accept({ type: 'boolean', value: true });
    await renderer.accept({ type: 'end' });

    assert.equal(renderer.booleanResult.tagName, 'DIV');
    assert.equal(renderer.booleanResult.querySelector('svg').getAttribute('class'),
        'workbench-status-icon workbench-status-icon--positive');
    assert.equal(renderer.booleanResult.querySelector('svg').getAttribute('aria-label'), 'Yes');
    assert.equal(renderer.booleanResult.children[1].textContent, 'Yes');
    assert.equal(renderer.fullscreenButton.hidden, false);
    assert.equal(renderer.downloadToggle.hidden, true);
    assert.equal(renderer.optionsToggle.hidden, true);
    renderer.dispose();

    const localizedTarget = document.createElement('section');
    document.body.appendChild(localizedTarget);
    const localized = new queryStream.QueryResultRenderer(localizedTarget, {
        rowStore: inMemoryRowStore(),
        workbench: { messages: { 'true.label': 'Oui', 'false.label': 'Non' } }
    });
    await localized.accept({ type: 'view', id: 'boolean' });
    await localized.accept({ type: 'boolean', value: false });
    await localized.accept({ type: 'end' });
    assert.equal(localized.booleanResult.querySelector('svg').getAttribute('class'),
        'workbench-status-icon workbench-status-icon--negative');
    assert.equal(localized.booleanResult.children[1].textContent, 'Non',
        'labels supplied by a typed route model must take precedence over default messages');
    localized.dispose();
});

test('boolean results accept common namespaces and links before metadata and their value', () => {
    const queryStream = loadQueryStreamApi();
    const state = new queryStream.QueryResultState();
    state.accept({ type: 'view', id: 'boolean' });
    state.accept({ type: 'namespaces', values: [{ prefix: 'ex', name: 'urn:example:' }] });
    state.accept({ type: 'links', values: ['info'] });
    state.accept({ type: 'metadata', values: { 'total-result-count': 1 } });
    state.accept({ type: 'boolean', value: true });
    state.accept({ type: 'end' });

    assert.deepEqual(JSON.parse(JSON.stringify(state.namespaces)), [
        { prefix: 'ex', name: 'urn:example:' }
    ]);
    assert.deepEqual(JSON.parse(JSON.stringify(state.links)), ['info']);
    assert.equal(state.complete, true);
    assert.equal(state.booleanValue, true);
    assert.throws(() => state.accept({ type: 'namespaces', values: [] }), /namespaces must be/,
        'boolean views still allow a single namespace declaration only');
    assert.throws(() => state.accept({ type: 'links', values: [] }), /links must be/,
        'boolean views still reject links after their boolean value');

    const metadataFirst = new queryStream.QueryResultState();
    metadataFirst.accept({ type: 'view', id: 'boolean' });
    metadataFirst.accept({ type: 'metadata', values: {} });
    assert.throws(() => metadataFirst.accept({ type: 'links', values: ['info'] }), /links must be/,
        'common links remain ordered before metadata');
});

test('empty tuple and streamed error results retain distinct accessible states', async () => {
    const queryStream = loadQueryStreamApi();
    const document = new FakeDocument();
    const emptyTarget = document.createElement('section');
    document.body.appendChild(emptyTarget);
    const emptyRenderer = new queryStream.QueryResultRenderer(emptyTarget, {
        rowStore: inMemoryRowStore()
    });
    await emptyRenderer.accept({ type: 'view', id: 'query-result-tuple' });
    await emptyRenderer.accept({ type: 'vars', values: ['value'] });
    await emptyRenderer.accept({ type: 'end', metadata: { 'total-result-count': 0 } });

    assert.equal(emptyRenderer.status.getAttribute('role'), 'status');
    assert.equal(emptyRenderer.status.textContent, 'No results.');
    assert.equal(emptyTarget.querySelectorAll('[data-query-row-index]').length, 0);
    emptyRenderer.dispose();

    const errorTarget = document.createElement('section');
    document.body.appendChild(errorTarget);
    const errorRenderer = new queryStream.QueryResultRenderer(errorTarget, {
        rowStore: inMemoryRowStore()
    });
    await errorRenderer.accept({ type: 'view', id: 'query-result-error' });
    await errorRenderer.accept({ type: 'error', status: 400, message: 'Malformed query' });

    const alert = errorTarget.querySelector('[role="alert"]');
    assert.ok(alert);
    assert.equal(alert.textContent, 'Malformed query');
    assert.equal(errorRenderer.status.textContent, 'Query failed.');
    errorRenderer.dispose();
});

test('streamed timeout after rows is marked partial without completion-only result controls', async () => {
    const queryStream = loadQueryStreamApi();
    const timeoutMessage = 'Query timed out after 1 second. Increase the Query timeout in Options and run again.';
    const eofMessage = 'org.eclipse.rdf4j.query.QueryEvaluationException: java.io.EOFException';
    const cancelledMessage = 'Query cancelled.';

    async function renderError(errorRecord) {
        const document = new FakeDocument();
        const target = document.createElement('section');
        const loading = document.createElement('div');
        loading.setAttribute('id', 'query-results-loading');
        loading.hidden = true;
        target.appendChild(loading);
        document.body.appendChild(target);
        const renderer = new queryStream.QueryResultRenderer(target, {
            requestedLimit: 1000000,
            rowStore: inMemoryRowStore()
        });

        renderer.setBusy(true);
        await renderer.accept({ type: 'view', id: 'query-result-tuple' });
        await renderer.accept({ type: 'vars', values: ['value'] });
        await renderer.accept({ type: 'rows', values: [
            [{ kind: 'literal', value: 'partial one' }],
            [{ kind: 'literal', value: 'partial two' }]
        ] });
        if (errorRecord.type === 'transport-error') {
            renderer.fail(errorRecord.message);
        } else {
            await renderer.accept(errorRecord);
        }
        renderer.setBusy(false);

        const alert = target.querySelector('[role="alert"]');
        const downloadButton = target.querySelector('.query-result-download-button');
        const stateError = renderer.state.error;
        const observation = {
            statusText: renderer.status.textContent,
            partialStatus: /partial|incomplete/i.test(renderer.status.textContent),
            noCompletionCount: renderer.countLabel.hidden
                && !/(?:total rows|Page \d+ of \d+)/i.test(renderer.countLabel.textContent),
            noDownloadToggle: renderer.downloadToggle.hidden,
            noDownloadAction: downloadButton.hidden,
            busyCleared: target.getAttribute('aria-busy') === 'false' && loading.hidden,
            cancelCleared: renderer.cancelButton.hidden,
            complete: renderer.state.complete,
            rows: renderer.state.rowCount,
            errorCode: stateError.code,
            errorMessagePreserved: stateError.message === errorRecord.message,
            visibleError: alert.textContent
        };
        renderer.dispose();
        return observation;
    }

    const timeout = await renderError({
        type: 'error', code: 'timeout', message: timeoutMessage
    });
    const genericEof = await renderError({
        type: 'transport-error', message: eofMessage
    });
    const cancelled = await renderError({
        type: 'error', code: 'cancelled', message: cancelledMessage
    });

    const checks = {
        timeoutPartialStatus: timeout.partialStatus,
        timeoutGuidanceVisible: timeout.errorMessagePreserved && timeout.visibleError === timeoutMessage,
        noCompletionCount: timeout.noCompletionCount,
        noDownloadToggle: timeout.noDownloadToggle,
        noDownloadAction: timeout.noDownloadAction,
        busyCleared: timeout.busyCleared,
        cancelCleared: timeout.cancelCleared,
        timeoutCodePreserved: timeout.errorCode === 'timeout',
        partialRowsRetained: timeout.rows === 2 && genericEof.rows === 2,
        incompleteEofIsSanitized: genericEof.errorMessagePreserved && genericEof.errorCode !== 'timeout'
            && /visible results are incomplete/i.test(genericEof.visibleError)
            && /increase query timeout/i.test(genericEof.visibleError)
            && !/java\.io|eofexception|queryevaluationexception/i.test(genericEof.visibleError)
            && !/^query timed out\b/i.test(genericEof.statusText + ' ' + genericEof.visibleError),
        errorsAreNotSuccessfulCompletion: !timeout.complete && !genericEof.complete && !cancelled.complete,
        cancelCodeHasDistinctWording: cancelled.errorCode === 'cancelled'
            && cancelled.errorMessagePreserved && cancelled.visibleError === cancelledMessage
            && /cancelled/i.test(cancelled.statusText)
            && !/timed out|increase query timeout/i.test(cancelled.statusText + ' ' + cancelled.visibleError)
    };
    assert.deepEqual(checks, {
        timeoutPartialStatus: true,
        timeoutGuidanceVisible: true,
        noCompletionCount: true,
        noDownloadToggle: true,
        noDownloadAction: true,
        busyCleared: true,
        cancelCleared: true,
        timeoutCodePreserved: true,
        partialRowsRetained: true,
        incompleteEofIsSanitized: true,
        errorsAreNotSuccessfulCompletion: true,
        cancelCodeHasDistinctWording: true
    }, `timeout observation=${JSON.stringify(timeout)}; generic EOF observation=${JSON.stringify(genericEof)}; `
        + `cancelled observation=${JSON.stringify(cancelled)}`);
});

test('partial Records stay scrollable without paging and keep Load more hidden', async () => {
    const queryStream = loadQueryStreamApi();
    const document = new FakeDocument();
    const target = document.createElement('section');
    document.body.appendChild(target);
    const renderer = new queryStream.QueryResultRenderer(target, {
        requestedLimit: 1000000,
        initialLayout: 'records',
        maxDomRows: 2,
        rowStore: inMemoryRowStore()
    });

    await renderer.accept({ type: 'view', id: 'query-result-tuple' });
    await renderer.accept({ type: 'vars', values: ['value'] });
    await renderer.accept({ type: 'rows', values: [
        [{ kind: 'literal', value: 'partial one' }],
        [{ kind: 'literal', value: 'partial two' }],
        [{ kind: 'literal', value: 'partial three' }],
        [{ kind: 'literal', value: 'partial four' }],
        [{ kind: 'literal', value: 'partial five' }]
    ] });
    await renderer.accept({
        type: 'error', code: 'incomplete',
        message: 'The result stream ended before the query completed. The visible results are incomplete.'
    });
    renderer.setBusy(false);

    assert.match(renderer.status.textContent, /Incomplete results: 5 rows/);
    assert.equal(renderer.records.children.length <= 4, true,
        'partial record markup stays bounded to a small visible window with overscan');
    assert.equal(renderer.rowPositionControl.hidden, false,
        'the precise row locator remains available for retained partial results');
    assert.equal(renderer.loadMoreButton.hidden, true,
        'an incomplete stream without valid terminal continuation cannot expose Load more');
    assert.equal(renderer.root.querySelectorAll('button').some(button => /Next|Previous/.test(button.textContent)), false,
        'partial results do not expose result or record page controls');
    renderer.rowPositionControl.value = '5';
    renderer.rowPositionControl.trigger('change');
    await new Promise(resolve => setImmediate(resolve));

    assert.equal(renderer.records.children.some(record => record.getAttribute('data-query-record-index') === '4'), true,
        'the row locator can inspect retained partial rows without page navigation');
    renderer.dispose();
});

test('partial circuit-breaker results preserve a safe server reason without timeout guidance', async () => {
    const queryStream = loadQueryStreamApi();
    const document = new FakeDocument();
    const target = document.createElement('section');
    document.body.appendChild(target);
    const renderer = new queryStream.QueryResultRenderer(target, { rowStore: inMemoryRowStore() });
    const breakerMessage = 'The server stopped this query because of memory pressure. Try again later.';

    await renderer.accept({ type: 'view', id: 'query-result-tuple' });
    await renderer.accept({ type: 'vars', values: ['value'] });
    await renderer.accept({ type: 'rows', values: [[{ kind: 'literal', value: 'retained row' }]] });
    await renderer.accept({ type: 'error', code: 'circuit-breaker', message: breakerMessage });

    assert.equal(target.querySelector('[role="alert"]').textContent, breakerMessage,
        'a safe classified server reason must not be replaced by generic EOF wording');
    assert.match(renderer.status.textContent, /partial|incomplete/i,
        'the retained row must still be identified as incomplete');
    assert.doesNotMatch(renderer.status.textContent + ' ' + breakerMessage, /increase query timeout/i,
        'memory pressure is not primarily a query-timeout configuration problem');
    assert.equal(renderer.state.complete, false);
    assert.equal(renderer.downloadToggle.hidden, true);
    renderer.dispose();
});

test('result feature policy hides controls without changing loaded-row storage', async () => {
    const queryStream = loadQueryStreamApi();
    const document = new FakeDocument();
    const target = document.createElement('section');
    document.body.appendChild(target);
    const renderer = new queryStream.QueryResultRenderer(target, {
        requestedLimit: 120,
        batched: true,
        onLoadMore() {},
        rowStore: inMemoryRowStore(),
        features: {
            'result-layout': false,
            'result-wrap': false,
            'result-paging': false,
            'result-show-datatypes': false,
            'result-download': false,
            'result-download-format': false,
            'result-download-format-tuple': false,
            'result-download-limit': false,
            'result-fullscreen': false
        }
    });
    renderer.beginBatch(0);
    await renderer.accept({ type: 'head', version: 1 });
    await renderer.accept({ type: 'view', id: 'tuple' });
    await renderer.accept({ type: 'vars', values: ['value'] });
    await renderer.accept({ type: 'rows', values: Array.from({ length: 120 }, (_, index) => [
        { kind: 'literal', value: String(index) }
    ]) });
    await renderer.accept({ type: 'end', metadata: {
        'result-offset': 0,
        'result-limit': 120,
        'result-batch-count': 120,
        'result-has-more': true,
        'result-next-offset': 120
    } });

    assert.equal(renderer.state.rowCount, 120);
    assert.equal(renderer.layoutControl.hidden, true);
    assert.equal(renderer.wrapControl.hidden, true);
    assert.equal(renderer.loadMoreButton.hidden, true,
        'the result-paging policy hides only the continuation control');
    assert.equal(renderer.datatypeControl.hidden, true);
    assert.equal(renderer.downloadToggle.hidden, true);
    assert.equal(renderer.fullscreenButton.hidden, true);
    assert.equal(renderer.tableBody.children.length <= 82, true);
    assert.equal(renderer.root.querySelectorAll('button').some(button => /Next|Previous/.test(button.textContent)), false);
    assert.equal(renderer.root.querySelectorAll('select').some(select => select.name === 'stream-result-limit'), false);
    renderer.dispose();
});

test('loaded-row summary and Load more honor independent feature policies', async () => {
    const queryStream = loadQueryStreamApi();
    const document = new FakeDocument();
    const cases = [
        {
            feature: 'result-totals',
            totalsHidden: true,
            loadMoreHidden: false
        },
        {
            feature: 'result-paging',
            totalsHidden: false,
            loadMoreHidden: true
        }
    ];

    for (const resultCase of cases) {
        const target = document.createElement('section');
        document.body.appendChild(target);
        const renderer = new queryStream.QueryResultRenderer(target, {
            requestedOffset: 0,
            requestedLimit: 10,
            batched: true,
            rowStore: inMemoryRowStore(),
            onLoadMore() {},
            features: { [resultCase.feature]: false }
        });
        renderer.beginBatch(0);
        await renderer.accept({ type: 'head', version: 1 });
        await renderer.accept({ type: 'view', id: 'tuple' });
        await renderer.accept({ type: 'vars', values: ['value'] });
        await renderer.accept({ type: 'rows', values: Array.from({ length: 10 }, (_, index) => [
            { kind: 'literal', value: String(index) }
        ]) });
        await renderer.accept({ type: 'end', metadata: {
            'result-offset': 0,
            'result-limit': 10,
            'result-batch-count': 10,
            'result-has-more': true,
            'result-next-offset': 10
        } });

        assert.equal(renderer.countLabel.hidden, resultCase.totalsHidden,
            `${resultCase.feature} should independently control the loaded-row label`);
        assert.equal(renderer.loadMoreButton.hidden, resultCase.loadMoreHidden,
            `${resultCase.feature} should independently control Load more`);
        assert.equal(renderer.root.querySelectorAll('button').some(button => /Next|Previous/.test(button.textContent)), false);
        assert.equal(renderer.root.querySelectorAll('select').some(select => select.name === 'stream-result-limit'), false);
        renderer.dispose();
    }
});

test('result layout, wrapping, size, datatype, fullscreen, and download-limit policies stay independent', async () => {
    const queryStream = loadQueryStreamApi();
    const document = new FakeDocument();
    const cases = [
        { feature: 'result-layout', control: 'layoutControl', peers: ['wrapControl'] },
        { feature: 'result-wrap', control: 'wrapControl', peers: ['layoutControl'] },
        { feature: 'result-show-datatypes', control: 'datatypeControl', peers: ['layoutControl', 'wrapControl'] },
        { feature: 'result-fullscreen', control: 'fullscreenButton', peers: ['downloadLimitControl'] },
        { feature: 'result-download-limit', control: 'downloadLimitControl', peers: ['layoutControl', 'fullscreenButton'] }
    ];

    for (const resultCase of cases) {
        const target = document.createElement('section');
        document.body.appendChild(target);
        const renderer = new queryStream.QueryResultRenderer(target, {
            rowStore: inMemoryRowStore(),
            features: { [resultCase.feature]: false }
        });
        await renderer.accept({ type: 'view', id: 'tuple' });
        await renderer.accept({ type: 'vars', values: ['value'] });
        await renderer.accept({ type: 'rows', values: [[{ kind: 'literal', value: 'visible' }]] });
        await renderer.accept({ type: 'end', metadata: {
            'total-result-count': 30,
            'result-offset': 10,
            'result-limit': 10
        } });

        assert.equal(renderer[resultCase.control].hidden, true,
            `${resultCase.feature} should hide only ${resultCase.control}`);
        for (const peer of resultCase.peers) {
            assert.equal(renderer[peer].hidden, false,
                `${resultCase.feature} should not hide the independent ${peer} control`);
        }
        assert.equal(renderer.root.querySelectorAll('button').some(button => /Next|Previous/.test(button.textContent)), false);
        assert.equal(renderer.root.querySelectorAll('select').some(select => select.name === 'stream-result-limit'), false);
        renderer.dispose();
    }
});

test('download disclosure remains available for format or limit controls when download is disabled', async () => {
    const queryStream = loadQueryStreamApi();
    const document = new FakeDocument();
    const target = document.createElement('section');
    document.body.appendChild(target);
    const renderer = new queryStream.QueryResultRenderer(target, {
        rowStore: inMemoryRowStore(),
        features: { 'result-download': false }
    });
    await renderer.accept({ type: 'view', id: 'tuple' });
    await renderer.accept({ type: 'vars', values: ['value'] });
    await renderer.accept({ type: 'end', metadata: { 'total-result-count': 0 } });

    assert.equal(renderer.downloadToggle.hidden, false,
        'legacy download controls remain reachable while format or limit settings are enabled');
    assert.equal(renderer.downloadFormatControl.hidden, false);
    assert.equal(renderer.downloadLimitControl.hidden, false);
    assert.equal(target.querySelector('.query-result-download-button').hidden, true,
        'the actual download action stays hidden when downloads are disabled');
    renderer.dispose();
});

test('repeated result mount and dispose releases shared disclosure listeners and observers', async () => {
	const queryStream = loadQueryStreamApi();
	const document = new FakeDocument();
	const target = document.createElement('section');
	document.body.appendChild(target);

	for (let index = 0; index < 3; index++) {
		const renderer = new queryStream.QueryResultRenderer(target, { rowStore: inMemoryRowStore() });
		await renderer.accept({ type: 'view', id: 'tuple' });
		await renderer.accept({ type: 'vars', values: ['value'] });
		await renderer.accept({ type: 'rows', values: [[{ kind: 'literal', value: 'visible' }]] });
		await renderer.accept({ type: 'end', metadata: { 'total-result-count': 1 } });
		const observers = queryStream.__testDisclosureObservers;
		const observer = observers[observers.length - 1];
		const panel = document.getElementById(renderer.downloadToggle.getAttribute('aria-controls'));
		assert.equal(queryStream.__testResizeListeners.size, 1,
			'the shared controller should install one resize listener for the mounted result');
		assert.equal(observer.disconnected, false);
		assert.equal(renderer.downloadToggle.getAttribute('aria-expanded'), 'false');

		renderer.downloadToggle.click();
		assert.equal(panel.getAttribute('aria-hidden'), 'false');
		assert.equal(panel.inert, false,
			'opening dynamic controls should restore keyboard access');

		renderer.dispose();
		assert.equal(queryStream.__testResizeListeners.size, 0,
			'disposing the result should release the shared resize listener');
		assert.equal(observer.disconnected, true,
			'disposing the result should release the shared resize observer');
		assert.equal(target.querySelectorAll('[data-query-stream-root]').length, 0);
	}
});

test('query result download and options controls use the shared icon adapter', () => {
	const disclosureCalls = [];
	const actionCalls = [];
    const queryStream = loadQueryStreamApi({
        icons: {
            decorateButton(button, name, accessibleName) {
                actionCalls.push({ text: button.textContent, name, accessibleName });
            },
            decorateDisclosureButton(button, accessibleName) {
                disclosureCalls.push({ text: button.textContent, accessibleName });
            }
        }
    });
    const document = new FakeDocument();
	const target = document.createElement('section');
	document.body.appendChild(target);
	const renderer = new queryStream.QueryResultRenderer(target, { rowStore: inMemoryRowStore() });

	assert.deepEqual(disclosureCalls, [], 'the reusable panel component should own disclosure chevrons');
	for (const button of [renderer.downloadToggle, renderer.optionsToggle]) {
		const chevron = button.querySelector('svg[data-workbench-icon="chevron"]');
		assert.ok(chevron.classList.contains('workbench-disclosure-chevron'),
			'the reusable panel component should provide the rotating chevron');
		assert.equal(chevron.getAttribute('aria-hidden'), 'true');
	}
	assert.deepEqual(actionCalls, [
		{ text: 'Download', name: 'download', accessibleName: 'Download' }
	], 'the Download action should keep its download icon');
    renderer.dispose();
});
