const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const vm = require('node:vm');
const { spawnSync } = require('node:child_process');

const appSource = path.resolve(__dirname,
    '../../tools/workbench/src/main/webapp/scripts/ts/workbenchApp.ts');

function loadWorkbenchSource() {
    const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'rdf4j-explore-window-summary-'));
    const output = path.join(directory, 'workbench.js');
    const compilation = spawnSync('tsc', [
        '--noImplicitAny',
        '--target', 'ES2017',
        '--lib', 'ES2017,DOM',
        '--skipLibCheck',
        '--outFile', output,
        appSource
    ], { encoding: 'utf8' });
    assert.equal(compilation.status, 0,
        `Workbench source must compile for Explore window contract:\n${compilation.stdout}${compilation.stderr}`);

    const workbench = {};
    const window = {};
    const context = vm.createContext({ console, URL, Promise, window, workbench, setTimeout });
    vm.runInContext(fs.readFileSync(output, 'utf8'), context, { filename: output });
    window.workbench = context.workbench;
    context.workbench.__testWindow = window;
    return context.workbench;
}

function term(kind, value) {
    return { kind, value };
}

function fakeRuntime() {
    return {
        html(strings, ...values) {
            return { strings: Array.from(strings), values };
        },
        render(template, mount) {
            mount.template = template;
        }
    };
}

function collectText(template, result = []) {
    if (!template || typeof template !== 'object') {
        return result;
    }
    if (Array.isArray(template.strings)) {
        result.push(template.strings.join(' '));
        for (const value of template.values || []) {
            if (Array.isArray(value)) {
                value.forEach((entry) => collectText(entry, result));
            } else if (value && typeof value === 'object') {
                collectText(value, result);
            } else if (typeof value === 'string' || typeof value === 'number') {
                result.push(String(value));
            }
        }
    }
    return result;
}

test('Explore summaries and result counts stay complete across first, middle, and last row windows', async () => {
    const workbench = loadWorkbenchSource();
    const rows = Array.from({ length: 300 }, (_unused, index) => [
        term('iri', `urn:subject:${index}`),
        term('iri', 'urn:ordinary'),
        term('literal', `value-${index}`)
    ]);
    const setRow = (index, predicate, object) => {
        rows[index] = [term('iri', 'urn:explored'), term('iri', predicate), object];
    };
    setRow(1, 'http://www.w3.org/2000/01/rdf-schema#label', term('literal', 'Example class'));
    setRow(151, 'http://www.w3.org/2000/01/rdf-schema#comment', term('literal', 'Comment from middle window'));
    setRow(4, 'http://www.w3.org/2000/01/rdf-schema#subClassOf', term('iri', 'urn:parent:first'));
    rows[4][0] = term('iri', 'urn:child:first');
    setRow(150, 'http://www.w3.org/2000/01/rdf-schema#subClassOf', term('iri', 'urn:parent:middle'));
    rows[150][0] = term('iri', 'urn:child:middle');
    setRow(295, 'http://www.w3.org/2000/01/rdf-schema#subClassOf', term('iri', 'urn:parent:last'));
    rows[295][0] = term('iri', 'urn:child:last');
    for (let index = 200; index < 250; index++) {
        setRow(index, 'http://www.w3.org/2000/01/rdf-schema#range', term('iri', `urn:range:${index}`));
    }

    const rowStore = {
        async read(start, count) { return rows.slice(start, start + count); },
        async count() { return rows.length; }
    };
    const window = workbench.__testWindow;
    const listeners = {};
    window.innerHeight = 220;
    window.scrollY = 0;
    window.addEventListener = (name, listener) => { listeners[name] = listener; };
    window.removeEventListener = () => {};
    workbench.queryStream = Object.assign(workbench.queryStream, {
        MeasuredRowHeights: class {
            constructor(count, estimate) { this.count = count; this.estimate = estimate; }
            resize(count) { this.count = count; }
            measure() {}
            offsetOf(index) { return index * this.estimate; }
            range(scrollTop, viewportHeight, overscan, maximum) {
                const first = Math.min(this.count, Math.floor(scrollTop / this.estimate));
                const after = Math.min(this.count, Math.ceil((scrollTop + viewportHeight) / this.estimate));
                let start = Math.max(0, first - overscan);
                let end = Math.min(this.count, Math.max(after + overscan, start + 1));
                if (end - start > maximum) {
                    start = Math.max(0, first - Math.floor(maximum / 2));
                    end = Math.min(this.count, start + maximum);
                }
                return {
                    start,
                    end,
                    topSpacer: start * this.estimate,
                    bottomSpacer: (this.count - end) * this.estimate
                };
            }
        }
    });

    const table = {
        getAttribute(name) { return name === 'data-workbench-row-table' ? 'true' : ''; },
        getBoundingClientRect() { return { top: -window.scrollY, height: rows.length * 44 }; }
    };
    const mount = {
        querySelectorAll(selector) {
            return selector.indexOf('data-workbench-row-table') >= 0 ? [table] : [];
        }
    };
    const model = {
        viewId: 'explore',
        vars: ['subject', 'predicate', 'object'],
        rows: [],
        rowStart: 0,
        rowCount: rows.length,
        rowStore,
        namespaceMap: {},
        links: [],
        metadata: { resource: 'urn:explored' }
    };
    const runtime = fakeRuntime();

    const dispose = await workbench.views.bindRowWindows(mount, model,
        { basePath: '/workbench', repositoryId: 'repo-1', workbench: {} }, runtime);
    assert.ok(model.rows.length > 0, 'row-window binding should populate the first visible window');
    const expectedSummary = [
        'Example class',
        'Comment from middle window',
        'urn:parent:first', 'urn:parent:middle', 'urn:parent:last',
        'urn:child:first', 'urn:child:middle', 'urn:child:last'
    ];
    const snapshots = [];
    const inspect = () => {
        assert.ok(model.rows.length > 0, 'each viewport should load a row window');
        assert.equal(JSON.stringify(model.exploreSummary.groups.superClasses.items.map((item) => item.value)),
            JSON.stringify(['urn:parent:first', 'urn:parent:last', 'urn:parent:middle']));
        assert.equal(model.exploreSummary.groups.range.count, 50);
        assert.equal(model.exploreSummary.groups.range.items.length, 40,
            'group summaries should keep only a bounded page of members in the main thread');
        assert.equal(model.exploreSummary.groups.range.hasNext, true);
        const output = collectText(mount.template).join(' ');
        assert.ok(output.includes('300'), 'result count should use the complete row store count');
        assert.ok(output.includes('data-workbench-explore-action') && output.includes('50'),
            'all group members should remain reachable through bounded summary paging');
        for (const value of expectedSummary) {
            assert.ok(output.includes(value),
                `Explore summary should include ${value} outside the visible row window`);
        }
        snapshots.push(output);
    };

    inspect();
    window.scrollY = 145 * 44;
    await listeners.scroll();
    inspect();
    window.scrollY = 290 * 44;
    await listeners.scroll();
    inspect();

    assert.equal(model.rowCount, 300);
    assert.ok(model.rows.length < rows.length, 'all rows must remain in worker storage, not in the page model');
    assert.equal(snapshots.length, 3);
    dispose();
});
