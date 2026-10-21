// Review fixes (TS-A): loading route scripts.
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const scripts = process.env.WORKBENCH_SCRIPT_DIR
    || path.resolve(__dirname, '../../tools/workbench/src/main/webapp/scripts');

/** workbenchApp.js with a document whose script elements fail to load while `failing` says so. */
function loadApp() {
    const appended = [];
    const removed = [];
    const state = { failing: true };
    const head = {
        appendChild(script) {
            appended.push(script);
            script.parentNode = head;
            setImmediate(() => {
                if (state.failing) {
                    script.onerror(new Error('network'));
                } else {
                    script.setAttribute('data-workbench-loaded', 'true');
                    script.onload();
                }
            });
            return script;
        },
        removeChild(script) { removed.push(script); script.parentNode = null; return script; }
    };
    const document = {
        readyState: 'loading',
        addEventListener() {},
        getElementById() { return null; },
        querySelector(selector) {
            const source = /^script\[src="(.*)"\]$/.exec(selector);
            return source && appended.filter((script) => script.parentNode && script.src === source[1]).pop() || null;
        },
        createElement() {
            const attributes = {};
            return { src: '', async: true, onload: null, onerror: null, parentNode: null,
                getAttribute: (name) => attributes[name] || null, setAttribute: (name, value) => { attributes[name] = value; } };
        },
        head
    };
    const window = { document };
    const workbench = {};
    const context = vm.createContext({ console, URL, Promise, window, workbench, setTimeout });
    vm.runInContext(fs.readFileSync(path.join(scripts, 'workbenchApp.js'), 'utf8'), context, { filename: 'workbenchApp.js' });
    return { app: workbench.app, appended, removed, state };
}

test('A17: a route script that failed to load is loaded again the next time, and its failed element is removed', async () => {
    const loaded = loadApp();
    await assert.rejects(loaded.app.loadScripts(['viz/viz.js']), /Unable to load Workbench script/);
    assert.equal(loaded.appended.length, 1);
    assert.deepEqual(loaded.removed, loaded.appended, 'the failed script element does not stay in the document');
    loaded.state.failing = false;
    await loaded.app.loadScripts(['viz/viz.js']);
    assert.equal(loaded.appended.length, 2, 'a fresh request is made instead of reusing the failure');
});

test('A21: the shared runtime scripts are requested together (their order kept by async=false), not one by one', async () => {
    const appended = [];
    const document = {
        readyState: 'loading', addEventListener() {}, getElementById() { return null; },
        querySelector() { return null; },
        createElement() {
            const attributes = {};
            return { src: '', async: true, getAttribute: (name) => attributes[name] || null,
                setAttribute: (name, value) => { attributes[name] = value; } };
        },
        head: { appendChild(script) { appended.push(script); return script; } }
    };
    const workbench = {};
    const context = vm.createContext({ console, URL, Promise, window: { document }, workbench, setTimeout });
    vm.runInContext(fs.readFileSync(path.join(scripts, 'workbenchApp.js'), 'utf8'), context, { filename: 'workbenchApp.js' });
    workbench.views = { render() {} };
    workbench.routes = { get() {} };
    workbench.queryStream = { scheduleRowStoreMaintenance() {},
        consumeNdjsonResponse() {}, createRowStore() {} };
    const mount = { getAttribute: (name) => (name === 'data-workbench-base-path' ? '/wb' : ''), setAttribute() {} };
    const boot = workbench.app.bootstrap(mount);
    await new Promise(setImmediate);
    assert.deepEqual(appended.map((script) => script.src), ['workbenchViews.js', 'workbenchRoutes.js',
        'workbenchRouter.js', 'queryStream.js', 'workbench-theme.js'].map((name) => '/wb/scripts/' + name),
        'every shared script is requested before the first one has loaded');
    assert.ok(appended.every((script) => script.async === false), 'they still run in this order');
    appended.forEach((script) => { script.setAttribute('data-workbench-loaded', 'true'); script.onload(); });
    assert.equal((await boot).status, 'skipped');
});

test('A-opt2: restoring a scroll position keeps applying it while the page is still growing', async () => {
    const frames = [];
    let height = 1000;
    const window = {
        document: { readyState: 'loading', addEventListener() {}, getElementById() { return null; } },
        scrollY: 0,
        scrollTo(x, y) { window.scrollY = Math.min(y, height); },
        requestAnimationFrame(callback) { frames.push(callback); },
        addEventListener() {}, removeEventListener() {}
    };
    const workbench = {};
    const context = vm.createContext({ console, URL, Promise, window, workbench, setTimeout });
    vm.runInContext(fs.readFileSync(path.join(scripts, 'workbenchApp.js'), 'utf8'), context, { filename: 'workbenchApp.js' });
    assert.equal(typeof workbench.app.restoreScroll, 'function', 'one helper restores scroll for bootstrap and the router');
    workbench.app.restoreScroll(window, 4000);
    assert.equal(window.scrollY, 1000, 'clamped by the page as it is now');
    height = 2500;
    frames.shift()();
    height = 5000;
    frames.shift()();
    assert.equal(window.scrollY, 4000, 'reached once the rows have grown the page');
});
