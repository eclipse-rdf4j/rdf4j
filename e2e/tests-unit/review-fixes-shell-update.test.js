// Review fixes (TS-A): leaving SPARQL Update with its editor in full screen.
const test = require('node:test');
const assert = require('node:assert/strict');
const { createFormBrowserHarness } = require('./form-browser-harness.js');

test('A30: disposing Update leaves full screen first, so the next page can scroll', () => {
    const harness = createFormBrowserHarness({ globals: { namespaces: {} } });
    const page = harness.registerElement('div', { id: 'page' });
    page.appendChild(harness.registerElement('textarea', { id: 'update', value: 'DELETE WHERE {}' }));
    harness.document.body.appendChild(page);
    const calls = [];
    const root = { overflow: '' };
    let editor = null;
    harness.context.YASQE = {
        defaults: {},
        Autocompleters: { prefixes: {} },
        registerAutocompleter() {},
        fromTextArea(textarea) {
            let value = textarea.value || '';
            const options = {};
            editor = {
                getValue: () => value,
                setValue: (next) => { value = next; },
                refresh() {},
                save() {},
                getWrapperElement: () => harness.registerElement('div'),
                on() {},
                getOption: (name) => options[name],
                // CodeMirror's fullscreen add-on hides the document's scrollbars while it is on.
                setOption(name, on) {
                    calls.push(name + '=' + on);
                    options[name] = on;
                    if (name === 'fullScreen') { root.overflow = on ? 'hidden' : ''; }
                },
                toTextArea() { calls.push('toTextArea'); }
            };
            return editor;
        }
    };
    harness.loadScripts(['yasqeHelper.js', 'update.js']);
    const dispose = harness.context.workbench.update.mount(page);
    editor.setOption('fullScreen', true);
    dispose();
    assert.equal(root.overflow, '', 'the document scrolls again');
    assert.deepEqual(calls, ['fullScreen=true', 'fullScreen=false', 'toTextArea'], 'full screen ends before the editor closes');
});
