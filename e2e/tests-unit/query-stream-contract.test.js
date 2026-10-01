const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const vm = require('node:vm');
const { spawnSync } = require('node:child_process');
const { FakeDocument } = require('./browser-fakes.js');

const disclosureSource = path.resolve(__dirname,
	'../../tools/workbench/src/main/webapp/scripts/ts/template.ts');
const streamSource = path.resolve(__dirname,
    '../../tools/workbench/src/main/webapp/scripts/ts/queryStream.ts');

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
        this.listeners.set(type, (this.listeners.get(type) || []).filter(candidate => candidate !== listener));
    }

    postMessage(message) {
        this.messages.push(message);
        queueMicrotask(() => {
            const response = { requestId: message.requestId, ok: true };
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
            (this.listeners.get('message') || []).forEach(listener => listener({ data: response }));
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

function loadQueryStreamApi() {
    const outputDirectory = fs.mkdtempSync(path.join(os.tmpdir(), 'rdf4j-query-stream-contract-'));
    const outputPath = path.join(outputDirectory, 'queryStream.js');
    const loadCallbacks = [];
    const document = new FakeDocument();
    const compilation = spawnSync('tsc', [
        '--target', 'ES2017',
        '--lib', 'ES2017,DOM',
        '--skipLibCheck',
        '--outFile', outputPath,
        disclosureSource,
        streamSource
    ], { encoding: 'utf8' });

    assert.equal(compilation.status, 0,
        `queryStream.ts must compile for its in-repo parser contract:\n${compilation.stdout}${compilation.stderr}`);

    const sessionValues = new Map();
    const localValues = new Map();
    const resizeListeners = new Set();
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
                boxSizing: 'border-box', lineHeight: '1px', fontSize: '1px',
                paddingTop: '0px', paddingBottom: '0px',
                borderTopWidth: '0px', borderBottomWidth: '0px',
                marginBlockStart: '8px', marginBlockEnd: '0px', direction: 'ltr', opacity: '1'
            }, element && element.style || {});
        },
        matchMedia() { return { matches: true }; },
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
    const workbench = { addLoad(callback) { loadCallbacks.push(callback); } };
    const context = vm.createContext({
        workbench,
        window: testWindow,
        document,
        Event: class { constructor(type) { this.type = type; } },
        TextDecoder: require('node:util').TextDecoder,
        URLSearchParams
    });
    vm.runInContext(fs.readFileSync(outputPath, 'utf8'), context, { filename: outputPath });
    const api = context.workbench.queryStream;
    api.__testWorkbench = context.workbench;
    api.__testWindow = testWindow;
    api.__testSessionValues = sessionValues;
    api.__testLocalValues = localValues;
    api.__testResizeListeners = resizeListeners;
    api.__testLoadCallbacks = loadCallbacks;
    return api;
}

test('NDJSON parser emits complete typed records as chunks arrive', () => {
    const queryStream = loadQueryStreamApi();
    assert.equal(typeof queryStream.NdjsonResultParser, 'function');

    const actual = [];
    const parser = new queryStream.NdjsonResultParser(record => actual.push(record));
    const records = [
        { type: 'head', version: 1 },
        { type: 'view', id: 'tuple' },
        { type: 'vars', values: ['subject', 'value'] },
        { type: 'namespaces', values: [{ prefix: 'ex', name: 'http://example.com/' }] },
        { type: 'links', values: ['https://example.com/help'] },
        { type: 'metadata', values: { limit: 0, offset: 0 } },
        {
            type: 'rows',
            values: [[
                { kind: 'iri', value: 'http://example.com/s' },
                {
                    kind: 'triple',
                    subject: { kind: 'bnode', value: 'b1' },
                    predicate: { kind: 'iri', value: 'http://example.com/p' },
                    object: { kind: 'literal', value: 'line one\nline two', language: 'en' }
                }
            ], [null, { kind: 'literal', value: 'plain' }]]
        },
        { type: 'end', metadata: { count: 2, hasMore: false } }
    ];
    const wire = records.map(record => JSON.stringify(record) + '\r\n').join('');
    const firstRecordEnd = wire.indexOf('\n') + 1;

    parser.push(wire.slice(0, firstRecordEnd));
    assert.equal(actual.length, 1, 'a complete record should be delivered before the response ends');
    parser.push(wire.slice(firstRecordEnd, firstRecordEnd + 19));
    parser.push(wire.slice(firstRecordEnd + 19, Math.floor(wire.length / 2)));
    parser.push(wire.slice(Math.floor(wire.length / 2)));
    parser.finish();

    assert.equal(JSON.stringify(actual), JSON.stringify(records));
});

test('query stream sends exactly one typed request and exposes terminal metadata', async () => {
    const queryStream = loadQueryStreamApi();
    const requests = [];
    const received = [];
    const wire = [
        { type: 'head', version: 1 },
        { type: 'view', id: 'tuple' },
        { type: 'vars', values: ['value'] },
        { type: 'rows', values: [[{ kind: 'literal', value: 'answer' }]] },
        { type: 'end', metadata: { count: 1, hasMore: false } }
    ].map(record => JSON.stringify(record) + '\n').join('');
    const encoded = new TextEncoder().encode(wire);
    let delivered = false;

    const outcome = await queryStream.executeQueryStream('/repositories/test/query', [
        { name: 'action', value: 'exec' },
        { name: 'query', value: 'SELECT ?value WHERE { VALUES ?value { "answer" } }' }
    ], {
        fetcher: async (url, init) => {
            requests.push({ url, init });
            return {
                ok: true,
                status: 200,
                body: {
                    getReader() {
                        return {
                            async read() {
                                if (delivered) {
                                    return { done: true, value: undefined };
                                }
                                delivered = true;
                                return { done: false, value: encoded };
                            },
                            async cancel() {
                            }
                        };
                    }
                }
            };
        },
        onRecord: record => received.push(record)
    });

    assert.equal(requests.length, 1, 'an execution descriptor should produce one POST');
    assert.equal(requests[0].url, '/repositories/test/query');
    assert.equal(requests[0].init.method, 'POST');
    assert.equal(requests[0].init.headers.Accept,
        'application/vnd.rdf4j.workbench-query-v2+ndjson, application/vnd.rdf4j.workbench+ndjson;q=0.9');
    assert.equal(requests[0].init.body.some(parameter => parameter.name === 'Accept'), false);
    assert.equal(outcome.type, 'end');
    assert.equal(outcome.metadata.count, 1);
    assert.equal(received.filter(record => record.type === 'rows').length, 1);
});

test('stale query streams stop notifying the renderer and cancel their reader', async () => {
    const queryStream = loadQueryStreamApi();
    const records = [
        { type: 'head', version: 1 },
        { type: 'view', id: 'boolean' },
        { type: 'boolean', value: true },
        { type: 'end', metadata: { count: 1 } }
    ].map(record => JSON.stringify(record) + '\n');
    let current = true;
    let index = 0;
    let canceled = false;
    const received = [];

    const outcome = await queryStream.executeQueryStream('/query', [], {
        fetcher: async () => ({
            ok: true,
            status: 200,
            body: {
                getReader() {
                    return {
                        async read() {
                            if (index >= records.length) {
                                return { done: true, value: undefined };
                            }
                            const value = new TextEncoder().encode(records[index++]);
                            return { done: false, value };
                        },
                        async cancel() {
                            canceled = true;
                        }
                    };
                }
            }
        }),
        isCurrent: () => current,
        onRecord: record => {
            received.push(record);
            if (record.type === 'view') {
                current = false;
            }
        }
    });

    assert.equal(outcome.type, 'stale');
    assert.equal(received.some(record => record.type === 'boolean'), false);
    assert.equal(canceled, true);
});

test('virtual row window stays bounded and includes the visible range', () => {
    const queryStream = loadQueryStreamApi();
    const heights = new queryStream.MeasuredRowHeights(100000, 50);
    const range = heights.range(450000, 500, 5, 80);

    assert.equal(range.start <= 9000, true);
    assert.equal(range.end > 9009, true);
    assert.equal(range.end - range.start <= 80, true);
    assert.equal(range.topSpacer, heights.offsetOf(range.start));
    assert.equal(range.bottomSpacer, heights.offsetOf(100000) - heights.offsetOf(range.end));
    assert.equal(heights.offsetOf(100000), 5000000);
});

test('parser rejects an unsupported version and rows outside their variables', () => {
    const queryStream = loadQueryStreamApi();
    const parser = new queryStream.NdjsonResultParser(() => {});

    assert.throws(() => parser.push('{"type":"head","version":2}\n'), /version/i);

    const wrongWidth = new queryStream.QueryResultState();
    wrongWidth.accept({ type: 'view', id: 'tuple' });
    wrongWidth.accept({ type: 'vars', values: ['subject', 'object'] });
    assert.throws(() => wrongWidth.accept({ type: 'rows', values: [[null]] }), /row|variable/i);
});

test('graph results retain the four positional statement variables', () => {
    const queryStream = loadQueryStreamApi();
    const state = new queryStream.QueryResultState();
    state.accept({ type: 'view', id: 'graph' });
    assert.throws(() => state.accept({
        type: 'vars', values: ['subject', 'predicate', 'object']
    }), /graph|context/i);
});

test('legacy paging metadata helper remains isolated from the streamed query view', () => {
    const queryStream = loadQueryStreamApi();
    assert.equal(typeof queryStream.calculateResultPaging, 'function');

    const page = queryStream.calculateResultPaging(7, 0, 0, {
        'total-result-count': 37,
        'result-offset': 20,
        'result-limit': 10
    });
    assert.deepEqual(JSON.parse(JSON.stringify(page)), {
        offset: 20,
        limit: 10,
        rowCount: 7,
        totalCount: 37,
        firstRow: 21,
        lastRow: 27,
        hasPrevious: true,
        hasNext: true
    });

    const unlimited = queryStream.calculateResultPaging(100000, 0, 0, {
        'total-result-count': 100000,
        'result-offset': 0,
        'result-limit': 0
    });
    assert.equal(unlimited.lastRow, 100000);
    assert.equal(unlimited.hasNext, false, 'a zero legacy limit means there is no legacy next page');
});

test('execution form binding is explicit and leaves raw downloads native', () => {
    const queryStream = loadQueryStreamApi();
    assert.equal(typeof queryStream.bindExecutionForms, 'function');

    function form(id, targetId, accept) {
        const listeners = new Map();
        const attributes = new Map([
            ['id', id],
            ['data-workbench-query-execution', 'true'],
            ['data-workbench-results-target', targetId]
        ]);
        const controls = accept ? [{ name: 'Accept', value: accept }] : [{ name: 'action', value: 'exec' }];
        return {
            id,
            elements: controls,
            attributes,
            getAttribute(name) { return attributes.get(name); },
            addEventListener(type, callback) {
                const existing = listeners.get(type) || [];
                existing.push(callback);
                listeners.set(type, existing);
            },
            removeEventListener(type, callback) {
                listeners.set(type, (listeners.get(type) || []).filter(candidate => candidate !== callback));
            },
            listeners
        };
    }

    const execution = form('saved-query-exec', 'saved-query-result', '');
    const rawDownload = form('saved-query-download', 'saved-query-download-result', 'text/csv');
    const otherNativePost = form('save-query', 'save-query-result', '');
    otherNativePost.attributes.set('data-workbench-query-execution', 'false');
    const targets = new Map([
        ['saved-query-result', { ownerDocument: { createElement() { return {}; } } }],
        ['saved-query-download-result', {}],
        ['save-query-result', {}]
    ]);
    const root = {
        querySelectorAll(selector) {
            assert.equal(selector, 'form[data-workbench-query-execution="true"]');
            return [execution, rawDownload, otherNativePost];
        },
        ownerDocument: { getElementById(id) { return targets.get(id); } }
    };

    const dispose = queryStream.bindExecutionForms(root, {
        rowStoreOptions: { workerFactory: () => new InMemoryWorker() }
    });
    assert.equal(execution.listeners.get('submit').length, 1);
    assert.equal(rawDownload.listeners.get('submit'), undefined);
    assert.equal(otherNativePost.listeners.get('submit'), undefined);
    dispose();
    assert.equal(execution.listeners.get('submit').length, 0);
});

test('re-binding a saved-query window binds new forms and disposes detached forms', () => {
    const queryStream = loadQueryStreamApi();
    const document = {
        getElementById() { return {}; }
    };
    function executionForm(id) {
        const listeners = new Map();
        const attributes = new Map([
            ['id', id],
            ['method', 'post'],
            ['action', 'query'],
            ['data-workbench-query-execution', 'true'],
            ['data-workbench-results-target', id + '-results']
        ]);
        const controls = [{ name: 'action', value: 'exec' }];
        return {
            id,
            method: 'post',
            elements: controls,
            getAttribute(name) { return attributes.get(name) || null; },
            addEventListener(type, callback) {
                listeners.set(type, [...(listeners.get(type) || []), callback]);
            },
            removeEventListener(type, callback) {
                listeners.set(type, (listeners.get(type) || []).filter(candidate => candidate !== callback));
            },
            listeners
        };
    }
    const first = executionForm('saved-exec-first');
    const second = executionForm('saved-exec-second');
    let visibleForms = [first];
    const root = {
        ownerDocument: document,
        querySelectorAll() { return visibleForms; }
    };
    const dispose = queryStream.bindExecutionForms(root);
    assert.equal(first.listeners.get('submit').length, 1);

    visibleForms = [second];
    queryStream.bindExecutionForms(root);
    assert.equal(first.listeners.get('submit').length, 0);
    assert.equal(second.listeners.get('submit').length, 1);

    dispose();
    assert.equal(second.listeners.get('submit').length, 0);
});

test('a bound execution form requests the default million-row batch into its declared result mount', async () => {
    const queryStream = loadQueryStreamApi();
    const document = new FakeDocument();
    const form = document.createElement('form');
    form.setAttribute('id', 'saved-query-exec-0');
    form.setAttribute('method', 'post');
    form.setAttribute('action', '/repositories/test/query');
    form.setAttribute('data-workbench-query-execution', 'true');
    form.setAttribute('data-workbench-results-target', 'saved-query-results-0');
    const action = document.createElement('input');
    action.name = 'action';
    action.value = 'exec';
    const query = document.createElement('textarea');
    query.name = 'query';
    query.value = 'SELECT * WHERE {?s ?p ?o}';
    const oldPageLimit = document.createElement('input');
    oldPageLimit.name = 'limit_query';
    oldPageLimit.value = '17';
    form.appendChild(action);
    form.appendChild(query);
    form.appendChild(oldPageLimit);
    document.cookie = 'total_result_count=23; path=/';
    const target = document.createElement('section');
    target.setAttribute('id', 'saved-query-results-0');
    document.body.appendChild(form);
    document.body.appendChild(target);

    const requests = [];
    const wire = [
        { type: 'head', version: 1 },
        { type: 'view', id: 'tuple' },
        { type: 'vars', values: ['value'] },
        { type: 'rows', values: [[{ kind: 'literal', value: 'streamed' }]] },
        { type: 'end', metadata: {
            'result-offset': 0,
            'result-limit': 1000000,
            'result-batch-count': 1,
            'result-has-more': false,
            'result-next-offset': 1
        } }
    ].map(record => JSON.stringify(record) + '\n').join('');
    const bytes = new TextEncoder().encode(wire);
    queryStream.__testWindow.fetch = async (url, init) => {
        requests.push({ url, init });
        let delivered = false;
        return {
            ok: true,
            status: 200,
            body: {
                getReader() {
                    return {
                        async read() {
                            if (delivered) {
                                return { done: true, value: undefined };
                            }
                            delivered = true;
                            return { done: false, value: bytes };
                        },
                        async cancel() {
                        }
                    };
                }
            }
        };
    };

    const root = {
        ownerDocument: document,
        querySelectorAll() { return [form]; }
    };
    const dispose = queryStream.bindExecutionForms(root, {
        rowStoreOptions: { workerFactory: () => new InMemoryWorker() }
    });
    const submitEvent = form.trigger('submit');
    await new Promise(resolve => setImmediate(resolve));

    assert.equal(submitEvent.defaultPrevented, true);
    assert.equal(requests.length, 1);
    assert.equal(requests[0].url, '/repositories/test/query');
    assert.equal(requests[0].init.method, 'POST');
    assert.equal(requests[0].init.headers.Accept,
        'application/vnd.rdf4j.workbench-query-v2+ndjson, application/vnd.rdf4j.workbench+ndjson;q=0.9');
    assert.equal(requests[0].init.body.get('action'), 'exec');
    assert.equal(requests[0].init.body.get('query'), 'SELECT * WHERE {?s ?p ?o}');
    assert.ok(requests[0].init.body.get('query-request-id'));
    assert.equal(target.getAttribute('aria-busy'), 'false');
    assert.equal(target.querySelector('.query-result-status').textContent, '1 result.',
        target.querySelector('.ERROR').textContent);
    assert.equal(target.querySelector('td').textContent, '"streamed"');
    assert.equal(requests[0].init.body.get('batch-size'), '1000000');
    assert.equal(requests[0].init.body.get('batch-offset'), '0');
    assert.equal(requests[0].init.body.has('limit_query'), false);
    assert.equal(requests[0].init.body.has('know_total'), false);
    assert.equal(target.querySelectorAll('button').some(button => /Next|Previous/.test(button.textContent)), false);
    assert.equal(target.querySelectorAll('select').some(select => select.name === 'stream-result-limit'), false);

    form.trigger('submit');
    await new Promise(resolve => setImmediate(resolve));
    assert.equal(requests.length, 2, 'Execute starts a fresh query rather than navigating a result page');
    assert.equal(requests[1].init.body.get('batch-size'), '1000000');
    assert.equal(requests[1].init.body.get('batch-offset'), '0');
    assert.equal(requests[1].init.body.has('know_total'), false,
        'a previous response count is not carried into a later execution');
    dispose();
});

test('large batched results stay in worker storage and bound table DOM', async () => {
    const queryStream = loadQueryStreamApi();
    assert.equal(typeof queryStream.QueryResultRenderer, 'function');

    const document = new FakeDocument();
    const target = document.createElement('section');
    document.body.appendChild(target);
    const renderer = new queryStream.QueryResultRenderer(target, {
        batched: true, requestedLimit: 1000000, maxDomRows: 80, rowStore: inMemoryRowStore()
    });
    renderer.beginBatch(0);
    await renderer.accept({ type: 'head', version: 1 });
    await renderer.accept({ type: 'view', id: 'tuple' });
    await renderer.accept({ type: 'vars', values: ['value'] });
    await renderer.accept({ type: 'rows', values: Array.from({ length: 100000 }, () => [null]) });
    await renderer.accept({ type: 'end', metadata: {
        'result-offset': 0,
        'result-limit': 1000000,
        'result-batch-count': 100000,
        'result-has-more': false,
        'result-next-offset': 100000
    } });

    assert.equal(renderer.state.rowCount, 100000);
    assert.equal(Array.isArray(renderer.state.rows), false,
        'large result batches must not retain an all-rows array on the main thread');
    assert.equal(renderer.tableBody.children.length <= 82, true,
        'the live table must contain at most 80 rows plus two spacers');
    renderer.tableWrap.scrollTop = 450000;
    renderer.tableWrap.trigger('scroll');
    await new Promise(resolve => setImmediate(resolve));
    const renderedIndexes = renderer.tableBody.children
        .map(row => row.getAttribute('data-query-row-index'))
        .filter(value => value !== undefined)
        .map(Number);
    assert.equal(renderedIndexes.some(index => index >= 99900), true,
        'the scroll window can seek near the end of a large loaded batch');
    assert.equal(renderer.tableBody.children.length <= 82, true);
});

test('records layout scrolls through loaded rows with bounded DOM and no local page controls', async () => {
    const queryStream = loadQueryStreamApi();
    const document = new FakeDocument();
    const target = document.createElement('section');
    document.body.appendChild(target);
    const renderer = new queryStream.QueryResultRenderer(target, {
        batched: true, requestedLimit: 25, maxDomRows: 10, rowStore: inMemoryRowStore()
    });
    renderer.beginBatch(0);
    await renderer.accept({ type: 'head', version: 1 });
    await renderer.accept({ type: 'view', id: 'tuple' });
    await renderer.accept({ type: 'vars', values: ['a', 'b', 'c', 'd', 'e', 'f', 'g'] });
    await renderer.accept({ type: 'rows', values: Array.from({ length: 25 }, (_, index) => [
        { kind: 'literal', value: String(index) }, null, null, null, null, null, null
    ]) });
    await renderer.accept({ type: 'end', metadata: {
        'result-offset': 0,
        'result-limit': 25,
        'result-batch-count': 25,
        'result-has-more': false,
        'result-next-offset': 25
    } });

    renderer.layoutControl.value = 'records';
    renderer.layoutControl.trigger('change');
    await new Promise(resolve => setImmediate(resolve));
    assert.equal(renderer.records.children.length <= 12, true,
        'records layout should keep a bounded visible window with overscan');
    assert.equal(renderer.records.querySelectorAll('button').some(button => /Next|Previous/.test(button.textContent)), false,
        'local record paging controls are replaced by scrolling');
    assert.equal(renderer.loadMoreButton.hidden, true,
        'a short final batch has no continuation control');

    renderer.records.scrollTop = 20 * 20;
    renderer.records.trigger('scroll');
    await new Promise(resolve => setImmediate(resolve));
    assert.equal(renderer.records.children.length <= 12, true);
    assert.equal(renderer.records.children.some(record => record.getAttribute('data-query-record-index') === '20'), true,
        'scrolling should materialize a later record window from the loaded row store');
    renderer.records.scrollTop = 24 * 20;
    renderer.records.trigger('scroll');
    await new Promise(resolve => setImmediate(resolve));
    assert.equal(renderer.records.children.some(record => record.getAttribute('data-query-record-index') === '24'), true,
        'scrolling can reach a precise loaded record without a row locator');
});

test('record titles retain their global positions across appended batches', async () => {
    const queryStream = loadQueryStreamApi();
    const document = new FakeDocument();
    const target = document.createElement('section');
    document.body.appendChild(target);
    const renderer = new queryStream.QueryResultRenderer(target, {
        batched: true, requestedOffset: 0, requestedLimit: 2, rowStore: inMemoryRowStore()
    });
    renderer.beginBatch(0);
    await renderer.accept({ type: 'head', version: 1 });
    await renderer.accept({ type: 'view', id: 'tuple' });
    await renderer.accept({ type: 'vars', values: ['value'] });
    await renderer.accept({ type: 'rows', values: [
        [{ kind: 'literal', value: 'first row' }],
        [{ kind: 'literal', value: 'second row' }]
    ] });
    await renderer.accept({ type: 'end', metadata: {
        'result-offset': 0,
        'result-limit': 2,
        'result-batch-count': 2,
        'result-has-more': true,
        'result-next-offset': 2
    } });
    renderer.beginBatch(2);
    await renderer.accept({ type: 'head', version: 1 });
    await renderer.accept({ type: 'view', id: 'tuple' });
    await renderer.accept({ type: 'vars', values: ['value'] });
    await renderer.accept({ type: 'rows', values: [
        [{ kind: 'literal', value: 'third row' }],
        [{ kind: 'literal', value: 'fourth row' }]
    ] });
    await renderer.accept({ type: 'end', metadata: {
        'result-offset': 2,
        'result-limit': 2,
        'result-batch-count': 2,
        'result-has-more': false,
        'result-next-offset': 4
    } });

    renderer.layoutControl.value = 'records';
    renderer.layoutControl.trigger('change');
    await new Promise(resolve => setImmediate(resolve));

    assert.match(renderer.records.children[0].children[0].textContent, /Record 1/);
    assert.match(renderer.records.children[3].children[0].textContent, /Record 4/,
        'appended batches keep one continuous row index instead of restarting record numbering');
});

test('query page lifecycle hook is exported and owns one form submit listener', () => {
    const queryStream = loadQueryStreamApi();
    const workbench = queryStream.__testWorkbench;
    assert.equal(typeof workbench.queryPage.renderInto, 'function');

    const document = new FakeDocument();
    const form = document.createElement('form');
    form.setAttribute('id', 'query-form');
    const target = document.createElement('section');
    target.setAttribute('id', 'query-results');
    document.body.appendChild(form);
    document.body.appendChild(target);
    let submitCount = 0;
    workbench.query = { doSubmit() { submitCount += 1; } };

    const dispose = workbench.queryPage.renderInto(document.body, {}, {
        executionFormId: 'query-form',
        resultsMountId: 'query-results'
    });
    const event = form.trigger('submit');

    assert.equal(event.defaultPrevented, true);
    assert.equal(submitCount, 1);
    assert.equal(form.eventHandlers.get('submit').length, 1);
    dispose();
    assert.equal(form.eventHandlers.get('submit').length, 0);
});

test('query page keeps BFCache-owned results and releases rows on destructive pagehide', async () => {
    async function mountedResultPage() {
        const queryStream = loadQueryStreamApi();
        const workbench = queryStream.__testWorkbench;
        const window = queryStream.__testWindow;
        const listeners = new Map();
        window.addEventListener = (type, listener) => {
            const handlers = listeners.get(type) || [];
            handlers.push(listener);
            listeners.set(type, handlers);
        };
        window.removeEventListener = (type, listener) => {
            listeners.set(type, (listeners.get(type) || []).filter(handler => handler !== listener));
        };
        window.trigger = (type, event) => (listeners.get(type) || []).slice()
            .forEach(listener => listener(Object.assign({ type }, event || {})));

        const records = [
            { type: 'head', version: 1 },
            { type: 'view', id: 'tuple' },
            { type: 'vars', values: ['item'] },
            { type: 'rows', values: [[{ kind: 'literal', value: 'persisted row' }]] },
            { type: 'end', metadata: {
                'result-offset': 0,
                'result-limit': 1000000,
                'result-batch-count': 1,
                'result-has-more': false,
                'result-next-offset': 1
            } }
        ].map(record => JSON.stringify(record) + '\n').join('');
        const bytes = new TextEncoder().encode(records);
        let delivered = false;
        window.fetch = async () => ({
            body: {
                getReader: () => ({
                    read: async () => {
                        if (delivered) {
                            return { done: true };
                        }
                        delivered = true;
                        return { done: false, value: bytes };
                    },
                    cancel: async () => {}
                })
            }
        });

        const worker = new InMemoryWorker();
        const document = new FakeDocument();
        const form = document.createElement('form');
        form.setAttribute('id', 'query-form');
        form.setAttribute('action', 'query');
        const query = document.createElement('textarea');
        query.name = 'query';
        query.value = 'SELECT ?item WHERE { VALUES ?item { "persisted row" } }';
        form.appendChild(query);
        const action = document.createElement('input');
        action.name = 'action';
        action.value = 'exec';
        form.appendChild(action);
        const limit = document.createElement('input');
        limit.name = 'limit_query';
        limit.value = '0';
        form.appendChild(limit);
        const target = document.createElement('section');
        target.setAttribute('id', 'query-results');
        document.body.appendChild(form);
        document.body.appendChild(target);

        const dispose = workbench.queryPage.renderInto(document.body, {}, {
            executionFormId: 'query-form',
            resultsMountId: 'query-results',
            rowStoreOptions: { workerFactory: () => worker }
        });
        form.trigger('submit');
        for (let attempt = 0; attempt < 30
                && (worker.rows.length !== 1 || !/1 row · complete/.test(target.textContent)); attempt += 1) {
            await new Promise(resolve => setImmediate(resolve));
        }
        assert.equal(worker.rows.length, 1, 'the query should store its streamed row before pagehide');
        assert.match(target.textContent, /1 row · complete/, 'the query should reach its terminal view before pagehide');
        assert.equal(target.querySelectorAll('.query-result-layout').length, 1);
        return { queryStream, window, worker, target, dispose };
    }

    const bfcachePage = await mountedResultPage();
    bfcachePage.window.trigger('pagehide', { persisted: true });
    await new Promise(resolve => setImmediate(resolve));
    assert.equal(bfcachePage.queryStream.__testLocalValues.size, 0,
        'a BFCache-owned result must not be marked for destructive cleanup');
    assert.equal(bfcachePage.worker.terminated, false,
        'a BFCache-owned view must keep its worker available for back navigation');
    assert.equal(bfcachePage.worker.rows.length, 1);
    assert.equal(bfcachePage.target.querySelectorAll('.query-result-layout').length, 1);
    bfcachePage.dispose();
    await new Promise(resolve => setImmediate(resolve));

    const leavingPage = await mountedResultPage();
    leavingPage.window.trigger('pagehide', { persisted: false });
    await new Promise(resolve => setImmediate(resolve));
    const pendingCleanup = [...leavingPage.queryStream.__testLocalValues.keys()];
    assert.deepEqual(pendingCleanup, ['rdf4j.workbench.query-results.pending-disposal.v1:test-store'],
        'destructive pagehide must synchronously preserve the exact store ID for next-document recovery');
    assert.equal(leavingPage.worker.terminated, true,
        'a page leaving the session must complete the row-store dispose request');
    assert.deepEqual(leavingPage.worker.rows, []);
    assert.equal(leavingPage.target.querySelectorAll('.query-result-layout').length, 0);
});

// Task M4.3 of .agent/execplans/workbench-app-shell-and-critique-fixes-20260930.md: literals read as values.
test('formatRdfTerm shows literal values without quotes or datatype suffixes', () => {
    const queryStream = loadQueryStreamApi();
    const XSD = 'http://www.w3.org/2001/XMLSchema#';
    const namespaces = [{ prefix: 'rdf', name: 'http://www.w3.org/1999/02/22-rdf-syntax-ns#' }, { prefix: 'xsd', name: XSD }];
    const format = term => queryStream.formatRdfTerm(term, { namespaces });

    const plain = format({ kind: 'literal', value: 'absolvers pomades', datatype: XSD + 'string' });
    assert.equal(plain.label, 'absolvers pomades');
    assert.equal(plain.numeric, false);
    assert.equal(plain.ntriples, '"absolvers pomades"');
    assert.equal(plain.title, '"absolvers pomades"');

    const french = format({ kind: 'literal', value: 'bonjour', language: 'fr' });
    assert.equal(french.label, 'bonjour');
    assert.equal(french.language, 'fr');
    assert.equal(french.ntriples, '"bonjour"@fr');

    const integer = format({ kind: 'literal', value: '58', datatype: XSD + 'integer' });
    assert.equal(integer.label, '58');
    assert.equal(integer.numeric, true);
    assert.equal(integer.ntriples, '"58"^^<' + XSD + 'integer>');
    assert.equal(format({ kind: 'literal', value: '1.5', datatype: XSD + 'double' }).numeric, true);
    assert.equal(format({ kind: 'literal', value: '7', datatype: XSD + 'nonNegativeInteger' }).numeric, true);

    const date = format({ kind: 'literal', value: '2000-07-04', datatype: XSD + 'date' });
    assert.equal(date.label, '2000-07-04');
    assert.equal(date.datatype, XSD + 'date');
    assert.equal(date.numeric, false);

    const type = format({ kind: 'iri', value: 'http://www.w3.org/1999/02/22-rdf-syntax-ns#type' });
    assert.equal(type.label, 'rdf:type');
    assert.equal(type.numeric, false);
    assert.equal(type.ntriples, '<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>');
});

// Plan task M9.2: a running query is cancelled when its page is left.
test('a running query is cancelled with a keepalive request when the page is left, and with retries in the page', async () => {
    async function runningQueryPage() {
        const queryStream = loadQueryStreamApi();
        const workbench = queryStream.__testWorkbench;
        const window = queryStream.__testWindow;
        const listeners = new Map();
        window.addEventListener = (type, listener) => {
            listeners.set(type, (listeners.get(type) || []).concat([listener]));
        };
        window.removeEventListener = (type, listener) => {
            listeners.set(type, (listeners.get(type) || []).filter(handler => handler !== listener));
        };
        window.trigger = (type, event) => (listeners.get(type) || []).slice()
            .forEach(listener => listener(Object.assign({ type }, event || {})));
        const requests = [];
        window.fetch = (url, options) => {
            requests.push({ url: String(url), options: options || {} });
            // The query's stream never answers; the cancel request does.
            return options && options.keepalive ? Promise.resolve({ ok: true }) : new Promise(() => {});
        };
        const retryingCancels = [];
        workbench.query = { cancelServerQuery: (id) => retryingCancels.push(id) };
        const document = new FakeDocument();
        const form = document.createElement('form');
        form.setAttribute('id', 'query-form');
        form.setAttribute('action', 'query');
        for (const [name, value] of [['query', 'SELECT * WHERE { ?s ?p ?o }'], ['action', 'exec']]) {
            const control = document.createElement(name === 'query' ? 'textarea' : 'input');
            control.name = name;
            control.value = value;
            form.appendChild(control);
        }
        const target = document.createElement('section');
        target.setAttribute('id', 'query-results');
        document.body.appendChild(form);
        document.body.appendChild(target);
        const worker = new InMemoryWorker();
        workbench.queryPage.renderInto(document.body, {}, {
            executionFormId: 'query-form', resultsMountId: 'query-results',
            rowStoreOptions: { workerFactory: () => worker }
        });
        form.trigger('submit');
        for (let attempt = 0; attempt < 30 && !workbench.queryPage.hasActiveRequest(); attempt += 1) {
            await new Promise(resolve => setImmediate(resolve));
        }
        assert.equal(workbench.queryPage.hasActiveRequest(), true, 'the query is running');
        return { workbench, window, requests, retryingCancels };
    }

    const leaving = await runningQueryPage();
    leaving.window.trigger('pagehide', { persisted: false });
    const keepalive = leaving.requests.filter(request => request.options.keepalive);
    assert.equal(keepalive.length, 1, 'one cancel request that survives the page');
    assert.equal(keepalive[0].options.method, 'POST');
    assert.match(String(keepalive[0].options.body), /^action=cancel-query&query-request-id=\S+$/);
    assert.deepEqual(leaving.retryingCancels, [], 'the retrying jQuery request would not outlive the page');

    const navigating = await runningQueryPage();
    assert.equal(navigating.workbench.queryPage.cancelExecution(), true);
    assert.equal(navigating.retryingCancels.length, 1, 'in the page the retrying request is used');
    assert.equal(navigating.requests.filter(request => request.options.keepalive).length, 0);
    assert.equal(navigating.workbench.queryPage.cancelExecutionOnLeave(), false, 'nothing is left to cancel');
});
