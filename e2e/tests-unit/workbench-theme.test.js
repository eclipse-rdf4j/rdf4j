const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const scriptPath = path.resolve(__dirname,
    '../../tools/workbench/src/main/webapp/scripts/workbench-theme.js');

function loadTheme(defaultTheme, storedTheme) {
    const source = fs.readFileSync(scriptPath, 'utf8');
    const attributes = {};
    const values = new Map();
    if (storedTheme !== undefined && storedTheme !== null) {
        values.set('rdf4j-workbench-theme', storedTheme);
    }
    const root = {
        setAttribute(name, value) { attributes[name] = value; }
    };
    const mediaListeners = [];
    const windowListeners = new Map();
    const media = {
        matches: true,
        addEventListener(_type, listener) { mediaListeners.push(listener); }
    };
    const controls = [];
    const document = {
        documentElement: root,
        readyState: 'complete',
        querySelector(selector) {
            if (selector !== 'meta[name="rdf4j-workbench-theme-default"]') { return null; }
            return { getAttribute: () => defaultTheme };
        },
        getElementById() { return controls.length ? controls[controls.length - 1] : null; }
    };
    const window = {
        localStorage: {
            getItem(key) { return values.has(key) ? values.get(key) : null; },
            setItem(key, value) { values.set(key, value); }
        },
        matchMedia() { return media; },
        addEventListener(type, listener) {
            if (!windowListeners.has(type)) { windowListeners.set(type, []); }
            windowListeners.get(type).push(listener);
        }
    };
    vm.runInNewContext(source, { document, window }, { filename: scriptPath });

    return {
        attributes,
        controls,
        media,
        mediaListeners,
        values,
        window,
        addControl() {
            const listeners = new Map();
            const control = {
                value: '',
                addEventListener(type, listener) {
                    if (!listeners.has(type)) { listeners.set(type, []); }
                    listeners.get(type).push(listener);
                },
                dispatch(type) {
                    (listeners.get(type) || []).forEach((listener) => listener({ target: control }));
                }
            };
            controls.push(control);
            return control;
        },
        dispatchStorage(event) {
            (windowListeners.get('storage') || []).forEach((listener) => listener(event));
        }
    };
}

test('theme lifecycle binds late controls and keeps explicit System separate from configured default', () => {
    const state = loadTheme('dark', null);
    assert.equal(state.attributes['data-theme'], 'dark');

    const control = state.addControl();
    assert.equal(typeof state.window.RDF4JWorkbenchTheme.connectControl, 'function');
    state.window.RDF4JWorkbenchTheme.connectControl();
    assert.equal(control.value, 'dark');

    control.value = 'system';
    control.dispatch('change');
    assert.equal(state.values.get('rdf4j-workbench-theme'), 'system');
    assert.equal(state.attributes['data-theme'], 'dark');

    state.media.matches = false;
    state.mediaListeners.forEach((listener) => listener({ matches: false }));
    assert.equal(state.attributes['data-theme'], 'light');
    assert.equal(control.value, 'system');

    state.dispatchStorage({ key: 'rdf4j-workbench-theme', newValue: 'dark' });
    assert.equal(control.value, 'dark');
    assert.equal(state.attributes['data-theme'], 'dark');
});

test('theme lifecycle updates a late-bound control on storage changes and resets to configured default', () => {
    const state = loadTheme('dark', 'light');
    const control = state.addControl();
    assert.equal(typeof state.window.RDF4JWorkbenchTheme.connectControl, 'function');
    state.window.RDF4JWorkbenchTheme.connectControl();
    assert.equal(control.value, 'light');

    state.dispatchStorage({ key: 'other-key', newValue: 'dark' });
    assert.equal(control.value, 'light');
    state.dispatchStorage({ key: 'rdf4j-workbench-theme', newValue: null });
    assert.equal(control.value, 'dark');
    assert.equal(state.attributes['data-theme'], 'dark');
});
