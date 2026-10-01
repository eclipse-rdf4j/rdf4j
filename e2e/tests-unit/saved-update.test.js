const test = require('node:test');
const assert = require('node:assert/strict');

const { createFormBrowserHarness } = require('./form-browser-harness.js');

function createYasqeStub(harness) {
    const state = {
        appendPrefixCalls: [],
        registeredCompleters: [],
        wrapper: harness.registerElement('div', { className: 'yasqe-wrapper' })
    };
    const codeMirror = harness.registerElement('div', { className: 'CodeMirror' });
    const scroll = harness.registerElement('div', { className: 'CodeMirror-scroll' });
    state.wrapper.appendChild(codeMirror);
    state.wrapper.appendChild(scroll);

    return {
        state,
        api: {
            defaults: {},
            Autocompleters: {
                prefixes: {
                    appendPrefixIfNeeded(yasqe, name) {
                        state.appendPrefixCalls.push({ yasqe, name });
                    },
                    isValidCompletionPosition(yasqe) {
                        return yasqe.valid;
                    },
                    preprocessPrefixTokenForCompletion(yasqe, token) {
                        return `${yasqe.label}:${token}`;
                    }
                }
            },
            fromTextArea(textarea, options) {
                let value = textarea.value || '';
                const instance = {
                    label: 'stub',
                    valid: true,
                    getValue() {
                        return value;
                    },
                    getWrapperElement() {
                        return state.wrapper;
                    },
                    on(eventName, handler) {
                        instance.changeHandler = handler;
                    },
                    refresh() {
                        instance.refreshCount = (instance.refreshCount || 0) + 1;
                    },
                    save() {
                        instance.saveCount = (instance.saveCount || 0) + 1;
                    },
                    setValue(nextValue) {
                        value = nextValue;
                        textarea.value = nextValue;
                    },
                    toTextArea() {
                        instance.closed = true;
                    }
                };
                instance.options = options;
                state.instance = instance;
                return instance;
            },
            registerAutocompleter(name, factory) {
                state.registeredCompleters.push({ name, factory });
            }
        }
    };
}

test('saved queries delete permissions and toggle behavior cover both branches', async () => {
    const harness = createFormBrowserHarness({
        confirmResponses: [true]
    });
    const form = harness.registerElement('form', { id: 'urn:query', name: 'urn:query' });
    const metadata = harness.registerElement('div', {
        id: 'urn:query-metadata',
        style: { display: 'none' }
    });
    const toggle = harness.registerElement('button', {
        id: 'urn:query-toggle',
        attributes: { 'aria-expanded': 'false' }
    });
    const textarea = harness.registerElement('textarea', {
        id: 'urn:query-text',
        value: '  SELECT * WHERE {?s ?p ?o}  '
    });
    const pre = harness.registerElement('pre', { innerHTML: '  ASK {}  ' });
    const queryForm = harness.registerElement('form', {
        attributes: { name: 'edit-query' }
    });
    const queryInput = harness.registerElement('input', {
        name: 'query',
        attributes: { value: '  DESCRIBE ?s  ' }
    });
    queryForm.appendChild(queryInput);
    harness.document.body.appendChild(form);
    harness.document.body.appendChild(metadata);
    harness.document.body.appendChild(toggle);
    harness.document.body.appendChild(textarea);
    harness.document.body.appendChild(pre);
    harness.document.body.appendChild(queryForm);

    const yasqe = createYasqeStub(harness);
    harness.context.YASQE = yasqe.api;
    harness.document.cookie = 'server-user-password=' + encodeURIComponent(Buffer.from('alice:secret').toString('base64'));

    harness.loadScripts(['saved-queries.js']);
    harness.runLoadHandlers();

    assert.equal(pre.innerHTML, 'ASK {}');
    assert.equal(queryInput.getAttribute('value'), 'DESCRIBE ?s');

    harness.context.workbench.savedQueries.deleteQuery('alice', 'Query 1', 'urn:query');
    await new Promise((resolve) => setImmediate(resolve));
    assert.equal(form.submitCount, 1);

    harness.context.workbench.savedQueries.deleteQuery('bob', 'Query 2', 'urn:query');
    assert.match(harness.alerts[0], /User 'alice' is not allowed do delete it/);

    harness.context.workbench.savedQueries.toggle('urn:query');
    assert.equal(metadata.style.display, '');
    assert.equal(textarea.value, 'SELECT * WHERE {?s ?p ?o}');
    assert.equal(yasqe.state.instance.refreshCount, 1);
    assert.equal(toggle.getAttribute('aria-expanded'), 'true');
    assert.equal(toggle.textContent, 'Hide details');

    harness.context.workbench.savedQueries.toggle('urn:query');
    assert.equal(textarea.style.display, 'none');
    assert.equal(yasqe.state.instance.closed, true);
    assert.equal(toggle.getAttribute('aria-expanded'), 'false');
    assert.equal(toggle.textContent, 'Show details');
});

test('saved query controls bind inert data attributes to static handlers', async () => {
    const harness = createFormBrowserHarness({
        confirmResponses: [true]
    });
    const owner = "owner');globalThis.rdf4jXss=true;//";
    const queryName = "query');globalThis.rdf4jXss=true;//";
    const form = harness.registerElement('form', { id: 'urn:query', name: 'urn:query' });
    const metadata = harness.registerElement('div', {
        id: 'urn:query-metadata',
        style: { display: 'none' }
    });
    const toggle = harness.registerElement('button', {
        id: 'urn:query-toggle',
        className: 'saved-query-toggle',
        attributes: {
            'data-query-urn': 'urn:query',
            'aria-expanded': 'false'
        }
    });
    const textarea = harness.registerElement('textarea', {
        id: 'urn:query-text',
        value: 'ASK {}'
    });
    const deleteButton = harness.registerElement('input', {
        id: 'urn:query-delete',
        className: 'saved-query-delete',
        attributes: {
            'data-query-name': queryName,
            'data-query-owner': owner,
            'data-query-urn': 'urn:query'
        }
    });
    [form, metadata, toggle, textarea, deleteButton]
        .forEach((element) => harness.document.body.appendChild(element));

    const yasqe = createYasqeStub(harness);
    harness.context.YASQE = yasqe.api;
    harness.document.cookie = 'server-user-password=' + encodeURIComponent(Buffer.from(owner + ':secret').toString('base64'));

    harness.loadScripts(['saved-queries.js']);
    harness.runLoadHandlers();

    deleteButton.click();
    await new Promise((resolve) => setImmediate(resolve));
    assert.equal(form.submitCount, 1);
    assert.match(harness.confirms[0], /globalThis\.rdf4jXss=true/);
    assert.equal(harness.context.rdf4jXss, undefined);

    toggle.click();
    assert.equal(metadata.style.display, '');
    assert.equal(toggle.textContent, 'Hide details');
    assert.equal(yasqe.state.instance.refreshCount, 1);
});

test('update page initializes yasqe, applies defaults, and submits safely without init', () => {
    const harness = createFormBrowserHarness({
        globals: {
            namespaces: {
                ex: 'http://example.com/'
            }
        }
    });
    const update = harness.registerElement('textarea', {
        id: 'update',
        value: ''
    });
    harness.document.body.appendChild(update);
    const resizeHandle = harness.registerElement('div', { id: 'update-editor-resize' });
    harness.document.body.appendChild(resizeHandle);

    const yasqe = createYasqeStub(harness);
    let setupCompletersArg = null;
    harness.context.YASQE = yasqe.api;

    harness.loadScripts(['yasqeHelper.js', 'update.js']);
    harness.context.workbench.yasqeHelper.setupCompleters = (namespaces) => {
        setupCompletersArg = namespaces;
    };
    const sizingInstalls = [];
    harness.context.workbench.editorSizing.install = (cm, handle, key) => sizingInstalls.push({ handle, key });

    assert.equal(harness.context.workbench.update.doSubmit(), true);

    harness.runLoadHandlers();

    const instance = yasqe.state.instance;
    assert.deepEqual(setupCompletersArg, { ex: 'http://example.com/' });
    assert.match(instance.getValue(), /INSERT DATA/);
    // Size comes from the shared editor CSS (M3.6): no inline width or height on the editor.
    assert.equal(yasqe.state.wrapper.style.width || '', '');
    assert.equal(yasqe.state.wrapper.getElementsByTagName('div')[0].style.height || '', '');
    assert.equal(instance.refreshCount, 1);
    assert.equal(sizingInstalls.length, 1);
    assert.equal(sizingInstalls[0].handle, resizeHandle);
    assert.equal(sizingInstalls[0].key, 'rdf4j.workbench.update-editor-height.v1');
    assert.deepEqual(JSON.parse(JSON.stringify(instance.options.createShareLink())), { update: instance.getValue() });

    instance.options.consumeShareLink(instance, { update: 'DELETE WHERE {}' });
    assert.equal(instance.getValue(), 'DELETE WHERE {}');
    assert.equal(harness.context.workbench.update.doSubmit(), true);
    assert.equal(instance.saveCount, 1);

    // Cmd/Ctrl+Enter submits through the form's submit handler; YASQE never posts to its own endpoint.
    assert.deepEqual(JSON.parse(JSON.stringify(instance.options.sparql)), { endpoint: '', showQueryButton: false });
    instance.options.extraKeys['Ctrl-Enter']();
    const form = harness.registerElement('form', { id: 'update-form' });
    let requested = 0;
    form.requestSubmit = () => { requested++; };
    instance.options.extraKeys['Ctrl-Enter']();
    instance.options.extraKeys['Cmd-Enter']();
    assert.equal(requested, 2);
});

test('yasqe helper registers namespace completer and delegates prefix helpers', () => {
    const harness = createFormBrowserHarness();
    const yasqe = createYasqeStub(harness);
    harness.context.YASQE = yasqe.api;

    harness.loadScripts(['yasqeHelper.js']);
    harness.context.workbench.yasqeHelper.setupCompleters({
        ex: 'http://example.com/',
        foaf: 'http://xmlns.com/foaf/0.1/'
    });

    assert.deepEqual(Array.from(harness.context.YASQE.defaults.autocompleters), ['customPrefixCompleter', 'variables']);
    assert.equal(yasqe.state.registeredCompleters.length, 1);
    assert.equal(yasqe.state.registeredCompleters[0].name, 'customPrefixCompleter');

    const fakeEditor = {
        label: 'editor',
        on(eventName, handler) {
            this.eventName = eventName;
            this.handler = handler;
        },
        valid: false
    };
    const completer = yasqe.state.registeredCompleters[0].factory(fakeEditor, 'customPrefixCompleter');
    fakeEditor.handler();

    assert.deepEqual(Array.from(completer.get()), [
        'ex <http://example.com/>',
        'foaf <http://xmlns.com/foaf/0.1/>'
    ]);
    assert.equal(completer.bulk, true);
    assert.equal(completer.async, false);
    assert.equal(completer.autoShow, true);
    assert.equal(completer.isValidCompletionPosition(), false);
    assert.equal(completer.preProcessToken('tok'), 'editor:tok');
    assert.equal(yasqe.state.appendPrefixCalls.length, 1);
});
