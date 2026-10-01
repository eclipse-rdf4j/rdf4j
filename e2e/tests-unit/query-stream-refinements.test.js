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
    context.workbench.queryStream.__testResultFullscreen = context.workbench.resultFullscreen;
    context.workbench.queryStream.__testSessionValues = sessionValues;
    context.workbench.queryStream.__testLocalValues = localValues;
    context.workbench.queryStream.__testResizeListeners = resizeListeners;
    context.workbench.queryStream.__testDisclosureObservers = disclosureObservers;
    return context.workbench.queryStream;
}

function createFullscreenFixture(doc = new FakeDocument()) {
    doc.contains = element => element === doc.body || doc.elements.includes(element);
    const target = doc.createElement('section');
    const control = doc.createElement('button');
    control.setAttribute('data-result-fullscreen-enabled', 'true');
    for (const element of [target, control]) {
        element.hasAttribute = name => element.attributes.has(name);
        element.closest = () => null;
    }
    doc.body.appendChild(target);
    doc.body.appendChild(control);
    return { doc, target, control };
}

test('result fullscreen restores the parent control after entry from a focused result iframe', () => {
    const queryStream = loadQueryStreamApi();
    const manager = queryStream.__testResultFullscreen;
    const { doc, target, control } = createFullscreenFixture();
    const frame = doc.createElement('iframe');
    doc.body.appendChild(frame);
    frame.hasAttribute = name => frame.attributes.has(name);
    frame.closest = () => null;

    frame.focus();
    assert.equal(doc.activeElement, frame);
    manager.set(target, control, true, true, { previousFocus: control });
    assert.equal(doc.activeElement, control, 'entering fullscreen moves focus to its parent control');

    manager.set(target, control, false);

    assert.equal(doc.activeElement, control,
        'exiting fullscreen restores focus to the parent control instead of the previously focused iframe');
    assert.equal(doc.body.classList.contains('query-results-fullscreen-active'), false,
        'fullscreen exit releases the shared body scroll lock');
});

test('shared fullscreen ownership switches targets, enforces policy, and unlocks on renderer disposal', () => {
    const queryStream = loadQueryStreamApi();
    const manager = queryStream.__testResultFullscreen;
    const first = createFullscreenFixture();
    const second = createFullscreenFixture(first.doc);
    const disabled = createFullscreenFixture(first.doc);
    disabled.control.setAttribute('data-result-fullscreen-enabled', 'false');

    manager.set(disabled.target, disabled.control, true);
    assert.equal(manager.isFullscreen(disabled.target), false,
        'a disabled fullscreen policy cannot activate the target');
    assert.equal(manager.currentTarget(), null);

    const firstRenderer = new queryStream.QueryResultRenderer(first.target, {
        initialWrap: true, rowStore: inMemoryRowStore()
    });
    const secondRenderer = new queryStream.QueryResultRenderer(second.target, {
        initialWrap: true, rowStore: inMemoryRowStore()
    });

    manager.set(first.target, firstRenderer.fullscreenButton, true);
    assert.equal(firstRenderer.root.getAttribute('data-wrap'), 'false',
        'the first owner uses fullscreen presentation while active');
    assert.equal(first.doc.body.classList.contains('query-results-fullscreen-active'), true);
    manager.set(second.target, secondRenderer.fullscreenButton, true);
    assert.equal(manager.isFullscreen(first.target), false, 'only the latest result owns fullscreen');
    assert.equal(manager.isFullscreen(second.target), true);
    assert.equal(manager.currentTarget(), second.target);
    assert.equal(firstRenderer.root.getAttribute('data-wrap'), 'true',
        'handing fullscreen to another result restores the previous owner preference');
    assert.equal(secondRenderer.root.getAttribute('data-wrap'), 'false',
        'the new fullscreen owner receives the no-wrap presentation');
    assert.equal(first.doc.body.classList.contains('query-results-fullscreen-active'), true,
        'switching result owners preserves the shared scroll lock');
    manager.set(second.target, secondRenderer.fullscreenButton, false, false);
    assert.equal(secondRenderer.root.getAttribute('data-wrap'), 'true',
        'leaving fullscreen restores the second owner preference');
    assert.equal(first.doc.body.classList.contains('query-results-fullscreen-active'), false,
        'exiting the active owner releases the scroll lock');
    assert.equal(manager.currentTarget(), null);
    firstRenderer.dispose();
    secondRenderer.dispose();

    const disposal = createFullscreenFixture();
    const renderer = new queryStream.QueryResultRenderer(disposal.target, {
        initialWrap: true, rowStore: inMemoryRowStore()
    });
    manager.set(disposal.target, renderer.fullscreenButton, true);
    assert.equal(renderer.root.getAttribute('data-wrap'), 'false');
    renderer.dispose();
    assert.equal(manager.isFullscreen(disposal.target), false,
        'disposing a streamed result exits fullscreen on its owned target');
    assert.equal(renderer.root.getAttribute('data-wrap'), 'true',
        'disposing a renderer restores its pre-fullscreen presentation before teardown');
    assert.equal(disposal.doc.body.classList.contains('query-results-fullscreen-active'), false,
        'renderer disposal releases its body scroll lock');
    assert.equal(manager.currentTarget(), null);
    const replacement = new queryStream.QueryResultRenderer(disposal.target, {
        initialWrap: true, rowStore: inMemoryRowStore()
    });
    assert.equal(replacement.root.getAttribute('data-wrap'), 'true',
        'a replacement renderer starts from normal presentation after disposal');
    replacement.dispose();
});

test('fullscreen starts no-wrap and restores normal wrapping without saving temporary toggles', async () => {
    for (const normalWrap of [true, false]) {
        const preferenceChanges = [];
        const workbench = {
            query: {
                getResultPresentationState: () => ({ layout: 'auto', wrap: normalWrap }),
                applyResultPresentationState: (layout, wrap) => preferenceChanges.push({ layout, wrap })
            }
        };
        const queryStream = loadQueryStreamApi(workbench);
        const document = new FakeDocument();
        document.contains = element => element === document.body || document.elements.includes(element);
        const createElement = document.createElement.bind(document);
        document.createElement = tagName => {
            const element = createElement(tagName);
            element.hasAttribute = name => element.attributes.has(name);
            element.closest = () => null;
            return element;
        };
        const keyHandlers = new Map();
        document.defaultView = {
            addEventListener(type, listener) {
                if (type === 'keydown') keyHandlers.set(type, listener);
            },
            removeEventListener(type, listener) {
                if (keyHandlers.get(type) === listener) keyHandlers.delete(type);
            }
        };
        const target = document.createElement('section');
        document.body.appendChild(target);
        const renderer = new queryStream.QueryResultRenderer(target, {
            initialWrap: normalWrap,
            rowStore: inMemoryRowStore()
        });
        await renderer.accept({ type: 'view', id: 'tuple' });
        await renderer.accept({ type: 'vars', values: ['value'] });
        await renderer.accept({ type: 'rows', values: [[{ kind: 'literal', value: 'long result value' }]] });
        await renderer.accept({ type: 'end', metadata: { 'total-result-count': 1 } });

        const manager = queryStream.__testResultFullscreen;
        manager.set(target, renderer.fullscreenButton, true, false);
        assert.equal(renderer.root.getAttribute('data-wrap'), 'false',
            'fullscreen should begin with result values unwrapped');
        assert.equal(renderer.wrapControl.checked, false,
            'the Wrap values checkbox should match the no-wrap fullscreen presentation');

        renderer.wrapControl.checked = true;
        renderer.wrapControl.trigger('change');
        assert.equal(renderer.root.getAttribute('data-wrap'), 'true',
            'the user can enable wrapping while fullscreen');
        assert.equal(renderer.wrapControl.checked, true);
        assert.deepEqual(preferenceChanges, [],
            'fullscreen-only wrap changes must not overwrite the normal-view preference');

        renderer.layoutControl.value = 'records';
        renderer.layoutControl.trigger('change');
        assert.deepEqual(preferenceChanges, [{ layout: 'records', wrap: normalWrap }],
            'a fullscreen layout change must preserve the normal-view wrap preference');

        if (normalWrap) {
            const escape = {
                key: 'Escape',
                defaultPrevented: false,
                prevented: false,
                preventDefault() { this.prevented = true; }
            };
            keyHandlers.get('keydown')(escape);
            assert.equal(escape.prevented, true, 'Escape should exit fullscreen through the shared owner');
        } else {
            manager.set(target, renderer.fullscreenButton, false, false);
        }
        assert.equal(renderer.root.getAttribute('data-wrap'), normalWrap ? 'true' : 'false',
            'fullscreen exit should restore the previous normal wrapping preference');
        assert.equal(renderer.wrapControl.checked, normalWrap,
            'the restored checkbox should match the previous normal wrapping preference');
        assert.deepEqual(preferenceChanges, [{ layout: 'records', wrap: normalWrap }],
            'restoring normal presentation must not write the temporary fullscreen state');

        renderer.wrapControl.checked = !normalWrap;
        renderer.wrapControl.trigger('change');
        assert.deepEqual(preferenceChanges, [
            { layout: 'records', wrap: normalWrap },
            { layout: 'records', wrap: !normalWrap }
        ],
            'normal-view wrap changes should still update the normal preference');
        renderer.dispose();
    }
});

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

test('virtualized Table and Records views omit user row-navigation controls', async () => {
    const queryStream = loadQueryStreamApi();
    const observed = [];
    for (const targetId of ['query-results', 'saved-query-results-0']) {
        for (const initialLayout of ['table', 'records']) {
            const document = new FakeDocument();
            const target = document.createElement('section');
            target.setAttribute('id', targetId);
            document.body.appendChild(target);
            const renderer = new queryStream.QueryResultRenderer(target, {
                initialLayout,
                maxDomRows: 20,
                rowStore: inMemoryRowStore()
            });
            try {
                renderer.beginBatch(0);
                await renderer.accept({ type: 'head', version: 1 });
                await renderer.accept({ type: 'view', id: 'tuple' });
                await renderer.accept({ type: 'vars', values: ['value'] });
                await renderer.accept({ type: 'rows', values: Array.from({ length: 81 }, (_, index) => [
                    { kind: 'literal', value: String(index + 1) }
                ]) });
                await renderer.accept({ type: 'end', metadata: {
                    'result-offset': 0,
                    'result-limit': 81,
                    'result-batch-count': 81,
                    'result-has-more': false,
                    'result-next-offset': 81,
                    'total-result-count': 81
                } });
                const controls = renderer.root.querySelectorAll('.query-result-row-position');
                const labels = renderer.root.querySelectorAll('label').filter(label =>
                    label.textContent.trim() === 'Go to row');
                const namedInputs = renderer.root.querySelectorAll('input').filter(input =>
                    input.getAttribute('aria-label') === 'Go to loaded row');
                observed.push({ targetId, layout: initialLayout, rowCount: renderer.state.rowCount,
                    controls: controls.length, labels: labels.length, namedInputs: namedInputs.length });
            } finally {
                renderer.dispose();
            }
        }
    }

    assert.deepEqual(observed, [
        { targetId: 'query-results', layout: 'table', rowCount: 81, controls: 0, labels: 0, namedInputs: 0 },
        { targetId: 'query-results', layout: 'records', rowCount: 81, controls: 0, labels: 0, namedInputs: 0 },
        { targetId: 'saved-query-results-0', layout: 'table', rowCount: 81, controls: 0, labels: 0, namedInputs: 0 },
        { targetId: 'saved-query-results-0', layout: 'records', rowCount: 81, controls: 0, labels: 0, namedInputs: 0 }
    ], 'main and saved virtualized result renderers should not expose Go to row UI or names');
});

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
    assert.equal(queryStream.chooseAutoLayout(315, [92, 118, 105], 'auto', true), 'table',
        'the exact readable minimum boundary should remain a table');
    assert.equal(queryStream.chooseAutoLayout(314.99, [92, 118, 105], 'auto', true), 'records',
        'wrapped Auto should use records only below the total minimum');
    assert.equal(queryStream.chooseAutoLayout(0, [92, 118, 105], 'auto', true), 'records',
        'a result with no visible width cannot satisfy wrapped per-column minimums');
    assert.equal(queryStream.chooseAutoLayout(1, [92, 118, 105], 'auto', false), 'table',
        'no-wrap Auto should retain a horizontally scrollable table below the minimum');
    assert.equal(queryStream.chooseAutoLayout(1, [92, 118, 105], 'table', true), 'table',
        'an explicit Table selection should remain selected below the minimum');
    assert.equal(queryStream.chooseAutoLayout(340, [92, 118, 105], 'records'), 'records',
        'explicit layout selection must take precedence over automatic measurement');
});

test('table column allocation honors every minimum and distributes wrapped width by sample demand', () => {
    const queryStream = loadQueryStreamApi();
    assert.equal(typeof queryStream.allocateTableColumnWidths, 'function');

    const asymmetric = Array.from(queryStream.allocateTableColumnWidths([32, 1038, 64], [150, 150, 150], 1134, true));
    assert.deepEqual(asymmetric, [150, 834, 150],
        'a short identifier and another short column must leave useful width for the sampled long value');
    assert.equal(asymmetric.reduce((sum, width) => sum + width, 0), 1134,
        'a wrapped table should use the available width when all minima fit');
    assert.equal(asymmetric.every(width => width >= 150), true,
        'each wrapped column should meet its minimum');

    const tight = Array.from(queryStream.allocateTableColumnWidths([10, 500], [100, 100], 200, true));
    assert.deepEqual(tight, [100, 100], 'the minimum equality boundary should allocate exactly the minimums');
    const explicitOverflow = Array.from(queryStream.allocateTableColumnWidths([10, 500], [100, 100], 150, true));
    assert.deepEqual(explicitOverflow, [100, 100],
        'an explicitly selected narrow table should keep readable minima and scroll instead of shrinking');
    const noWrap = Array.from(queryStream.allocateTableColumnWidths([25, 60], [100, 100], 150, false));
    assert.deepEqual(noWrap, [100, 100], 'no-wrap columns should preserve minima when the viewport is narrow');
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
        // Literal labels are their lexical values; language and datatype become tags in the cell (task M4.3).
        { label: 'plain', exploreResource: '"plain"', externalHref: null, preformatted: false },
        { label: 'bonjour', exploreResource: '"bonjour"@fr', externalHref: null, preformatted: false },
        { label: 'bonjour', exploreResource: '"bonjour"@fr', externalHref: null, preformatted: false },
        {
            label: 'widget', exploreResource: '"widget"^^<http://example.test/types/Code>',
            externalHref: null, preformatted: false
        },
        {
            label: 'widget', exploreResource: '"widget"^^<http://example.test/types/Code>',
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
    const renderer = new queryStream.QueryResultRenderer(target, {
        initialLayout: 'table', rowStore: inMemoryRowStore()
    });
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
    const renderer = new queryStream.QueryResultRenderer(target, {
        initialLayout: 'table', rowStore: inMemoryRowStore()
    });
    await renderer.accept({ type: 'view', id: 'tuple' });
    await renderer.accept({ type: 'vars', values: ['code'] });
    await renderer.accept({ type: 'namespaces', values: [{ prefix: 'kind', name: 'http://example.test/types/' }] });
    await renderer.accept({ type: 'rows', values: [[{
        kind: 'literal', value: 'widget', datatype: 'http://example.test/types/Code'
    }]] });
    await renderer.accept({ type: 'end', metadata: { 'total-result-count': 1 } });

    const tableLink = renderer.tableBody.children.find(
        child => child.getAttribute('data-query-row-index') === '0').querySelector('a');
    assert.equal(tableLink.textContent, 'widget');
    assert.equal(tableLink.parentNode.querySelector('.rdf-datatype').textContent, 'kind:Code');
    assert.equal(tableLink.getAttribute('href'),
        'explore?resource=%22widget%22%5E%5E%3Chttp%3A%2F%2Fexample.test%2Ftypes%2FCode%3E');

    renderer.datatypeControl.checked = false;
    renderer.datatypeControl.trigger('change');
    await new Promise(resolve => setImmediate(resolve));
    const hiddenTagRow = renderer.tableBody.children.find(child => child.getAttribute('data-query-row-index') === '0');
    assert.equal(hiddenTagRow.querySelector('a').textContent, 'widget');
    assert.equal(hiddenTagRow.querySelector('.rdf-datatype'), null);

    renderer.layoutControl.value = 'records';
    renderer.layoutControl.trigger('change');
    await new Promise(resolve => setImmediate(resolve));
    const recordLink = renderer.records.children[0].querySelector('dd').querySelector('a');
    assert.equal(recordLink.textContent, 'widget');
    assert.equal(recordLink.getAttribute('href'), tableLink.getAttribute('href'));
    renderer.dispose();
});

test('typed result text remains inert and inline without duplicating the Workbench shell', async () => {
    const queryStream = loadQueryStreamApi();
    const document = new FakeDocument();
    const target = document.createElement('section');
    target.setAttribute('id', 'query-results');
    document.body.appendChild(target);
    const renderer = new queryStream.QueryResultRenderer(target, {
        initialLayout: 'table', rowStore: inMemoryRowStore()
    });
    const hostileLiteral = '<img src=x onerror=alert(1)>';

    assert.match(renderer.root.className, /query-result-embedded/,
        'streamed results should mount as an inline result surface');
    await renderer.accept({ type: 'view', id: 'tuple' });
    await renderer.accept({ type: 'vars', values: ['value'] });
    await renderer.accept({ type: 'rows', values: [[{ kind: 'literal', value: hostileLiteral }]] });
    await renderer.accept({ type: 'end', metadata: { 'total-result-count': 1 } });

    const cell = renderer.tableBody.children.find(row => row.getAttribute('data-query-row-index') === '0').children[0];
    assert.equal(cell.textContent, '<img src=x onerror=alert(1)>');
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
    target.setAttribute('aria-labelledby', 'query-results-heading');
    const legacyHeader = document.createElement('div');
    legacyHeader.className = 'query-results__header';
    const legacyHeading = document.createElement('h2');
    legacyHeading.setAttribute('id', 'query-results-heading');
    legacyHeading.textContent = 'Query result';
    legacyHeader.appendChild(legacyHeading);
    const fullscreen = document.createElement('button');
    fullscreen.setAttribute('id', 'query-results-fullscreen');
    fullscreen.setAttribute('data-result-fullscreen-enabled', 'true');
    fullscreen.hidden = true;
    legacyHeader.appendChild(fullscreen);
    target.appendChild(legacyHeader);
    document.body.appendChild(target);
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
    let fullscreenToggles = 0;
    const renderer = new queryStream.QueryResultRenderer(target, {
        executionForm,
        rowStore: inMemoryRowStore(),
        onToggleFullscreen: () => { fullscreenToggles++; },
        workbench: {
            tupleDownloadFormats: ['text/csv CSV', 'application/sparql-results+json SPARQL JSON'],
            defaults: { 'default-Accept': 'text/csv', 'default-download-limit': '17' }
        }
    });

    assert.equal(legacyHeader.hidden, true, 'the streamed title owns the initial result header');
    const initialHeading = renderer.root.querySelector('h2');
    assert.ok(initialHeading && initialHeading.getAttribute('id'));
    assert.equal(target.getAttribute('aria-labelledby'), initialHeading.getAttribute('id'));
    renderer.setBusy(true);
    assert.equal(target.getAttribute('aria-busy'), 'true', 'the result region exposes its loading state');
    renderer.setBusy(false);
    assert.equal(target.getAttribute('aria-busy'), 'false', 'the result region exposes its settled state');

    await renderer.accept({ type: 'view', id: 'tuple' });
    await renderer.accept({ type: 'vars', values: ['value'] });
    await renderer.accept({ type: 'rows', values: [[{ kind: 'literal', value: 'typed' }]] });
    await renderer.accept({ type: 'end', metadata: {
        'total-result-count': 1, 'result-offset': 0, 'result-limit': 0
    } });
    assert.equal(renderer.state.complete, true, 'the toolbar remains owned through completed rendering');

    assert.notEqual(renderer.fullscreenButton, fullscreen,
        'the streamed result owns its fullscreen trigger without moving the Lit-owned shell button');
    assert.equal(legacyHeader.hidden, true, 'the legacy result header is hidden while the renderer owns its toolbar');
    assert.equal(fullscreen.hidden, true, 'the hidden legacy toolbar cannot expose a second fullscreen trigger');
    const streamedHeading = renderer.root.querySelector('h2');
    assert.ok(streamedHeading && streamedHeading.getAttribute('id'),
        'the renderer supplies an accessible title for its result view');
    assert.equal(target.getAttribute('aria-labelledby'), streamedHeading.getAttribute('id'),
        'the result region names the visible streamed title');
    const visibleHeadings = target.querySelectorAll('h2').filter(heading => {
        let ancestor = heading;
        while (ancestor && ancestor !== target.parentNode) {
            if (ancestor.hidden || ancestor.inert) return false;
            ancestor = ancestor.parentNode;
        }
        return true;
    });
    assert.equal(visibleHeadings.length, 1, 'exactly one result title is visible during streaming');
    assert.equal(visibleHeadings[0], streamedHeading);
    assert.equal(renderer.fullscreenButton.hidden, false);
    renderer.fullscreenButton.click();
    assert.equal(fullscreenToggles, 1, 'the renderer-owned trigger keeps the fullscreen callback');
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
    assert.equal(legacyHeader.hidden, false, 'disposing the renderer restores the legacy result header');
    assert.equal(target.getAttribute('aria-labelledby'), legacyHeading.getAttribute('id'),
        'disposing the renderer restores the legacy accessible title');
    const replacement = new queryStream.QueryResultRenderer(target, { rowStore: inMemoryRowStore() });
    assert.equal(legacyHeader.hidden, true, 'a replacement stream takes ownership after a result reset');
    assert.equal(target.getAttribute('aria-labelledby'), replacement.root.querySelector('h2').getAttribute('id'));
    replacement.dispose();
    assert.equal(legacyHeader.hidden, false, 'replacement disposal restores the legacy result header');
    assert.equal(target.getAttribute('aria-labelledby'), legacyHeading.getAttribute('id'));
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
    assert.equal(renderer.optionsToggle.textContent.trim(), 'Display',
        'the result disclosure is labeled Display, not the Options of the query settings');
    assert.equal(renderer.optionsToggle.getAttribute('aria-label'), 'Result display options');
    renderer.dispose();
});

test('result summaries report exact totals and follow explicit batch continuation', async () => {
    const queryStream = loadQueryStreamApi();
    const document = new FakeDocument();

    for (const batch of [
        { rowCount: 2, hasMore: true, total: 4, elapsed: 3500,
            expectedLabel: '2 of 4 loaded', expectedStatus: '4 rows · complete in 3,500 ms' },
        { rowCount: 1, hasMore: false, total: 1, elapsed: 2100,
            expectedLabel: '', expectedStatus: '1 row · complete in 2,100 ms' }
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

test('boolean results render their answer without result toolbar actions', async () => {
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
    assert.equal(renderer.booleanResult.querySelector('.query-result-boolean__value').textContent, 'Yes');
    assert.equal(renderer.fullscreenButton.hidden, true, 'a Yes/No answer has nothing to show full screen');
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
    assert.equal(localized.booleanResult.querySelector('.query-result-boolean__value').textContent, 'Non',
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
    assert.equal(emptyRenderer.status.textContent, '0 rows · complete');
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
    assert.equal(alert.querySelector('.workbench-callout__title').textContent, 'The query failed');
    assert.equal(alert.querySelector('.query-result-error__message').textContent, 'Malformed query');
    assert.equal(errorRenderer.status.textContent, 'Query failed.');
    errorRenderer.dispose();
});

test('streamed timeout after rows is marked partial without completion-only result controls', async () => {
    const queryStream = loadQueryStreamApi();
    const timeoutMessage = 'Query timed out. Increase the timeout in Query settings and run it again.';
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
            && /increase the timeout in query settings/i.test(genericEof.visibleError)
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
    assert.equal(renderer.root.querySelectorAll('.query-result-row-position').length, 0,
        'retained partial results do not expose a row locator');
    assert.equal(renderer.loadMoreButton.hidden, true,
        'an incomplete stream without valid terminal continuation cannot expose Load more');
    assert.equal(renderer.root.querySelectorAll('button').some(button => /Next|Previous/.test(button.textContent)), false,
        'partial results do not expose result or record page controls');
    renderer.records.scrollTop = 80;
    renderer.records.trigger('scroll');
    await new Promise(resolve => setImmediate(resolve));

    assert.equal(renderer.records.children.some(record => record.getAttribute('data-query-record-index') === '4'), true,
        'scrolling can inspect retained partial rows without page navigation');
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
    // One row: an empty result has no download controls at all (task M3.5).
    await renderer.accept({ type: 'rows', values: [[{ kind: 'literal', value: 'row' }]] });
    await renderer.accept({ type: 'end', metadata: { 'total-result-count': 1 } });

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

// Task M3.5 of .agent/execplans/workbench-app-shell-and-critique-fixes-20260930.md: one status line and
// designed result states.
async function renderTuple(queryStream, document, rowCount, metadata, options = {}) {
    const target = document.createElement('section');
    document.body.appendChild(target);
    const renderer = new queryStream.QueryResultRenderer(target, Object.assign({ rowStore: inMemoryRowStore() }, options));
    await renderer.accept({ type: 'view', id: 'query-result-tuple' });
    await renderer.accept({ type: 'vars', values: ['value'] });
    if (rowCount) {
        await renderer.accept({ type: 'rows', values: Array.from({ length: rowCount }, (_unused, index) => [
            { kind: 'literal', value: 'v' + index }
        ]) });
    }
    if (metadata) {
        await renderer.accept({ type: 'end', metadata: Object.assign({ 'query-result-status': 'completed' }, metadata) });
    }
    return { target, renderer };
}

test('the status line reads the row count and completion time', async () => {
    const queryStream = loadQueryStreamApi();
    const document = new FakeDocument();
    const streaming = await renderTuple(queryStream, document, 12, null);
    assert.equal(streaming.renderer.status.textContent, 'Receiving… 12 rows');
    assert.equal(streaming.renderer.countLabel.hidden, true, 'no loaded-of label while the stream runs');
    streaming.renderer.dispose();

    const complete = await renderTuple(queryStream, document, 400,
        { 'total-result-count': 400, 'query-elapsed-ms': 31 });
    assert.equal(complete.renderer.status.textContent, '400 rows · complete in 31 ms');
    assert.equal(complete.renderer.countLabel.hidden, true, 'every row is loaded, so there is no loaded-of label');
    complete.renderer.dispose();

    const one = await renderTuple(queryStream, document, 1, { 'total-result-count': 1, 'query-elapsed-ms': 2 });
    assert.equal(one.renderer.status.textContent, '1 row · complete in 2 ms');
    one.renderer.dispose();
});

test('an empty result shows a designed empty state under the header row', async () => {
    const queryStream = loadQueryStreamApi();
    const document = new FakeDocument();
    const { target, renderer } = await renderTuple(queryStream, document, 0,
        { 'total-result-count': 0, 'query-elapsed-ms': 4 });
    const empty = target.querySelector('.query-result-empty');
    assert.ok(empty, 'an empty-state element exists');
    assert.equal(empty.hidden, false);
    assert.equal(empty.querySelector('.query-result-empty__title').textContent, 'No results');
    assert.equal(empty.querySelector('.query-result-empty__text').textContent,
        'The query matched no solutions. Check the prefixes and the graph, or remove filters.');
    assert.equal(renderer.status.textContent, '0 rows · complete in 4 ms');
    assert.deepEqual([renderer.fullscreenButton.hidden, renderer.optionsToggle.hidden, renderer.downloadToggle.hidden],
        [true, true, true], 'an empty result has nothing to show full screen, display or download');
    renderer.dispose();

    const filled = await renderTuple(queryStream, document, 2, { 'total-result-count': 2, 'query-elapsed-ms': 4 });
    assert.equal(filled.target.querySelector('.query-result-empty').hidden, true);
    filled.renderer.dispose();
});

test('a boolean answer is a large state without Full screen, Display or Download', async () => {
    const queryStream = loadQueryStreamApi();
    const document = new FakeDocument();
    const target = document.createElement('section');
    document.body.appendChild(target);
    const renderer = new queryStream.QueryResultRenderer(target, { rowStore: inMemoryRowStore() });
    await renderer.accept({ type: 'view', id: 'boolean' });
    await renderer.accept({ type: 'boolean', value: true });
    await renderer.accept({ type: 'end', metadata: { 'query-elapsed-ms': 5, 'query-result-status': 'completed' } });
    assert.equal(renderer.status.textContent, 'Answer · complete in 5 ms');
    assert.equal(renderer.fullscreenButton.hidden, true);
    assert.equal(renderer.optionsToggle.hidden, true);
    assert.equal(renderer.downloadToggle.hidden, true);
    assert.equal(renderer.booleanResult.querySelector('.query-result-boolean__value').textContent, 'Yes');
    assert.equal(renderer.booleanResult.querySelector('.query-result-boolean__text').textContent,
        'At least one solution matches this pattern.');
    renderer.dispose();

    const noTarget = document.createElement('section');
    document.body.appendChild(noTarget);
    const no = new queryStream.QueryResultRenderer(noTarget, { rowStore: inMemoryRowStore() });
    await no.accept({ type: 'view', id: 'boolean' });
    await no.accept({ type: 'boolean', value: false });
    await no.accept({ type: 'end' });
    assert.equal(no.booleanResult.querySelector('.query-result-boolean__text').textContent,
        'No solution matches this pattern.');
    no.dispose();
});

test('a failure without rows hides Full screen and Display', async () => {
    const queryStream = loadQueryStreamApi();
    const document = new FakeDocument();
    const { renderer } = await renderTuple(queryStream, document, 0, null);
    const summaries = [];
    renderer.target.addEventListener('workbench:query-result-summary', event => summaries.push(event.detail));
    await renderer.accept({ type: 'error', status: 500, message: 'Repository unavailable' });
    assert.equal(summaries[summaries.length - 1].error, true, 'the Results badge learns about the failure');
    assert.equal(renderer.fullscreenButton.hidden, true);
    assert.equal(renderer.optionsToggle.hidden, true);
    assert.equal(renderer.downloadToggle.hidden, true);
    renderer.dispose();
});

test('a syntax error becomes a callout that points at the line and keeps the parser details', async () => {
    const queryStream = loadQueryStreamApi();
    const document = new FakeDocument();
    const target = document.createElement('section');
    document.body.appendChild(target);
    const events = [];
    target.addEventListener('workbench:query-error-location', event => events.push(event.detail));
    const renderer = new queryStream.QueryResultRenderer(target, { rowStore: inMemoryRowStore() });
    const message = 'Encountered "<EOF>" at line 1, column 27.\nWas expecting one of:\n    "{" ...';
    await renderer.accept({ type: 'view', id: 'query-result-error' });
    await renderer.accept({ type: 'error', status: 400, message });

    const alert = target.querySelector('[role="alert"]');
    assert.equal(alert.querySelector('.workbench-callout__title').textContent, 'Syntax error on line 1, column 27');
    assert.equal(alert.querySelector('.query-result-error__message').textContent,
        'Encountered "<EOF>" at line 1, column 27.');
    assert.equal(alert.querySelector('.query-result-error__details pre').textContent, message);
    const goTo = alert.querySelector('.query-result-error__goto');
    assert.equal(goTo.textContent, 'Go to line 1');
    goTo.click();
    assert.deepEqual(events.map(detail => [detail.line, detail.column, detail.reveal]), [[1, 27, false], [1, 27, true]],
        'the renderer reports the location when it shows the error and again when Go to line is pressed');
    renderer.dispose();

    const genericTarget = document.createElement('section');
    document.body.appendChild(genericTarget);
    const generic = new queryStream.QueryResultRenderer(genericTarget, { rowStore: inMemoryRowStore() });
    await generic.accept({ type: 'view', id: 'query-result-error' });
    await generic.accept({ type: 'error', status: 500, message: 'Repository unavailable' });
    const genericAlert = genericTarget.querySelector('[role="alert"]');
    assert.equal(genericAlert.querySelector('.workbench-callout__title').textContent, 'The query failed');
    assert.equal(genericAlert.querySelector('.query-result-error__goto'), null);
    generic.dispose();
});

// Task M4.1: rows scroll with the page unless the result is taller than the browser can scroll.
function fakePageView() {
    return {
        innerHeight: 900,
        scrollX: 0,
        scrollY: 0,
        listeners: {},
        addEventListener(type, listener) { (this.listeners[type] = this.listeners[type] || []).push(listener); },
        removeEventListener() {},
        scrollBy() {},
        scrollTo() {}
    };
}

async function renderedScrollSource(capacity) {
    const queryStream = loadQueryStreamApi();
    const document = new FakeDocument();
    document.defaultView = fakePageView();
    const target = document.createElement('section');
    document.body.appendChild(target);
    const renderer = new queryStream.QueryResultRenderer(target, { rowStore: inMemoryRowStore(), initialLayout: 'table' });
    renderer.tableScrollCapacity = capacity;
    await renderer.accept({ type: 'view', id: 'query-result-tuple' });
    await renderer.accept({ type: 'vars', values: ['value'] });
    await renderer.accept({ type: 'rows', values: Array.from({ length: 200 }, (_unused, index) => [
        { kind: 'literal', value: 'v' + index }
    ]) });
    await renderer.accept({ type: 'end', metadata: {} });
    const source = renderer.root.getAttribute('data-scroll-source');
    const compressed = renderer.rowCoordinates.compressed;
    renderer.dispose();
    return { source, compressed };
}

test('rows scroll with the page, and a result taller than the scroll capacity keeps its inner scroll element', async () => {
    assert.deepEqual(await renderedScrollSource(Number.MAX_SAFE_INTEGER), { source: 'page', compressed: false });
    assert.deepEqual(await renderedScrollSource(50), { source: 'element', compressed: true });
});

// Task M4.3: datatype and language tags, numeric cells and ?variable headings.
test('cells show language badges, datatype tags and right-aligned numbers, and headers show ?variables', async () => {
    const queryStream = loadQueryStreamApi();
    const document = new FakeDocument();
    const target = document.createElement('section');
    document.body.appendChild(target);
    const XSD = 'http://www.w3.org/2001/XMLSchema#';
    const renderer = new queryStream.QueryResultRenderer(target, { initialLayout: 'table', rowStore: inMemoryRowStore() });
    await renderer.accept({ type: 'view', id: 'tuple' });
    await renderer.accept({ type: 'vars', values: ['label', 'num', 'day', 'thing'] });
    await renderer.accept({ type: 'namespaces', values: [{ prefix: 'xsd', name: XSD }] });
    await renderer.accept({ type: 'rows', values: [[
        { kind: 'literal', value: 'bonjour', language: 'fr' },
        { kind: 'literal', value: '58', datatype: XSD + 'integer' },
        { kind: 'literal', value: '2000-07-04', datatype: XSD + 'date' },
        { kind: 'literal', value: 'plain', datatype: XSD + 'string' }
    ]] });
    await renderer.accept({ type: 'end', metadata: { 'total-result-count': 1 } });

    const headers = renderer.root.querySelectorAll('thead th').map(cell => cell.textContent);
    assert.deepEqual(headers, ['?label', '?num', '?day', '?thing']);
    const cells = renderer.tableBody.children.find(child => child.getAttribute('data-query-row-index') === '0').children;
    assert.equal(cells[0].querySelector('a').textContent, 'bonjour');
    assert.equal(cells[0].querySelector('.rdf-language').textContent, '@fr');
    assert.equal(cells[1].querySelector('a').textContent, '58');
    assert.equal(cells[1].classList.contains('rdf-numeric'), true);
    assert.equal(cells[1].querySelector('.rdf-datatype'), null, 'numbers carry no datatype tag');
    assert.equal(cells[2].querySelector('.rdf-datatype').textContent, 'xsd:date');
    assert.equal(cells[3].querySelector('a').textContent, 'plain');
    assert.equal(cells[3].querySelector('.rdf-datatype'), null, 'xsd:string never shows a tag');
    assert.equal(cells[3].classList.contains('rdf-literal'), true);

    renderer.datatypeControl.checked = false;
    renderer.datatypeControl.trigger('change');
    await new Promise(resolve => setImmediate(resolve));
    const hidden = renderer.tableBody.children.find(child => child.getAttribute('data-query-row-index') === '0').children;
    assert.equal(hidden[2].querySelector('.rdf-datatype'), null, 'Show datatypes off hides the tags');
    renderer.dispose();
});
