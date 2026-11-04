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
        confirmResponses: [true, true]
    });
    const form = harness.registerElement('form', { id: 'urn:query', name: 'urn:query' });
    const metadata = harness.registerElement('div', {
        id: 'urn:query-metadata',
        style: { display: 'none' }
    });
    const toggle = harness.registerElement('button', {
        id: 'urn:query-toggle',
        className: 'saved-query-toggle',
        attributes: { 'aria-expanded': 'false', 'data-query-urn': 'urn:query' }
    });
    const remove = harness.registerElement('button', {
        className: 'saved-query-delete',
        attributes: { 'data-query-urn': 'urn:query', 'data-query-owner': 'alice', 'data-query-name': 'Query 1' }
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
    harness.document.body.appendChild(remove);
    harness.document.body.appendChild(textarea);
    harness.document.body.appendChild(pre);
    harness.document.body.appendChild(queryForm);

    const yasqe = createYasqeStub(harness);
    harness.context.YASQE = yasqe.api;
    harness.document.cookie = 'server-user-password=' + encodeURIComponent(Buffer.from('alice:secret').toString('base64'));

    harness.loadScripts(['saved-queries.js']);
    harness.context.workbench.savedQueries.mount(harness.document.body);

    assert.equal(pre.innerHTML, 'ASK {}');
    assert.equal(queryInput.getAttribute('value'), 'DESCRIBE ?s');

    remove.dispatchEvent({ type: 'click', bubbles: true });
    await new Promise((resolve) => setImmediate(resolve));
    assert.equal(form.submitCount, 1);

    remove.setAttribute('data-query-owner', 'bob');
    remove.setAttribute('data-query-name', 'Query 2');
    remove.dispatchEvent({ type: 'click', bubbles: true });
    await new Promise((resolve) => setImmediate(resolve));
    assert.equal(form.submitCount, 2, 'the server makes the authorization decision');
    assert.deepEqual(harness.alerts, []);

    toggle.dispatchEvent({ type: 'click', bubbles: true });
    assert.equal(metadata.style.display, '');
    assert.equal(textarea.value, 'SELECT * WHERE {?s ?p ?o}');
    assert.equal(yasqe.state.instance.refreshCount, 1);
    // A read-only query needs no completion; YASQE's default completers fetch prefix.cc and vocabularies, and
    // YASQE deep-merges an empty list into its default one, so only null turns them off.
    assert.equal(yasqe.state.instance.options.autocompleters, null);
    assert.equal(toggle.getAttribute('aria-expanded'), 'true');
    assert.equal(toggle.textContent, 'Hide details');

    toggle.dispatchEvent({ type: 'click', bubbles: true });
    assert.equal(textarea.style.display, 'none');
    assert.equal(yasqe.state.instance.closed, true);
    assert.equal(toggle.getAttribute('aria-expanded'), 'false');
    assert.equal(toggle.textContent, 'Show details');
});

function countWrites(element, property) {
    const prototype = Object.getPrototypeOf(element);
    const descriptor = Object.getOwnPropertyDescriptor(prototype, property);
    const writes = [];
    Object.defineProperty(element, property, {
        configurable: true,
        get() {
            return descriptor.get.call(this);
        },
        set(value) {
            writes.push(value);
            descriptor.set.call(this, value);
        }
    });
    return writes;
}

test('saved query refresh leaves unchanged card content in place', () => {
    // The virtual row renderer refreshes the cards on every scroll; replacing a toggle's text while it is pressed
    // makes WebKit drop the click.
    const harness = createFormBrowserHarness();
    const metadata = harness.registerElement('div', { id: 'urn:query-metadata', style: { display: 'none' } });
    const toggle = harness.registerElement('button', {
        id: 'urn:query-toggle',
        className: 'saved-query-toggle',
        attributes: { 'aria-expanded': 'false', 'data-query-urn': 'urn:query' },
        textContent: 'Show details'
    });
    const textarea = harness.registerElement('textarea', { id: 'urn:query-text', value: 'ASK {}' });
    const pre = harness.registerElement('pre', { innerHTML: 'ASK {}' });
    [metadata, toggle, textarea, pre].forEach((element) => harness.document.body.appendChild(element));
    harness.context.YASQE = createYasqeStub(harness).api;
    harness.loadScripts(['saved-queries.js']);
    harness.context.workbench.savedQueries.mount(harness.document.body);

    const toggleTextWrites = countWrites(toggle, 'textContent');
    const queryWrites = countWrites(pre, 'innerHTML');
    harness.context.workbench.savedQueries.refresh(harness.document.body);

    assert.deepEqual(toggleTextWrites, []);
    assert.deepEqual(queryWrites, []);

    toggle.dispatchEvent({ type: 'click', bubbles: true });
    assert.equal(toggle.textContent, 'Hide details');
    toggleTextWrites.length = 0;
    harness.context.workbench.savedQueries.refresh(harness.document.body);

    assert.deepEqual(toggleTextWrites, []);
    assert.equal(toggle.getAttribute('aria-expanded'), 'true');
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
    harness.context.workbench.savedQueries.mount(harness.document.body);

    deleteButton.dispatchEvent({ type: 'click', bubbles: true });
    await new Promise((resolve) => setImmediate(resolve));
    assert.equal(form.submitCount, 1);
    assert.match(harness.confirms[0], /globalThis\.rdf4jXss=true/);
    assert.equal(harness.context.rdf4jXss, undefined);

    toggle.dispatchEvent({ type: 'click', bubbles: true });
    assert.equal(metadata.style.display, '');
    assert.equal(toggle.textContent, 'Hide details');
    assert.equal(yasqe.state.instance.refreshCount, 1);
});

// Round 2 (R2 client): the owner of a saved query is checked against the user of the raw credentials cookie; base64
// '+' must not be read as a space (which would name another user and refuse the owner's own delete).
test('the owner whose credentials encode with "+" may delete their saved query', async () => {
    const harness = createFormBrowserHarness({ confirmResponses: [true] });
    const owner = 'n>>';
    const form = harness.registerElement('form', { id: 'urn:query', name: 'urn:query' });
    const deleteButton = harness.registerElement('input', {
        id: 'urn:query-delete',
        className: 'saved-query-delete',
        attributes: { 'data-query-name': 'mine', 'data-query-owner': owner, 'data-query-urn': 'urn:query' }
    });
    [form, deleteButton].forEach((element) => harness.document.body.appendChild(element));
    harness.context.YASQE = createYasqeStub(harness).api;
    const encoded = Buffer.from(owner + ':pw').toString('base64');
    assert.ok(encoded.includes('+'));
    harness.document.cookie = 'server-user-password=' + encoded;
    const alerts = [];
    harness.context.alert = (message) => alerts.push(message);
    harness.loadScripts(['saved-queries.js']);
    harness.context.workbench.savedQueries.mount(harness.document.body);

    deleteButton.dispatchEvent({ type: 'click', bubbles: true });
    await new Promise((resolve) => setImmediate(resolve));
    assert.deepEqual(alerts, [], 'the owner is not told someone else saved it');
    assert.equal(form.submitCount, 1);
});

test('a malformed legacy credentials cookie has no effect on deletion confirmation', async () => {
    const harness = createFormBrowserHarness({ confirmResponses: [true] });
    const form = harness.registerElement('form', { id: 'urn:query', name: 'urn:query' });
    const deleteButton = harness.registerElement('input', {
        id: 'urn:query-delete',
        className: 'saved-query-delete',
        attributes: { 'data-query-name': 'theirs', 'data-query-owner': 'alice', 'data-query-urn': 'urn:query' }
    });
    [form, deleteButton].forEach((element) => harness.document.body.appendChild(element));
    harness.context.YASQE = createYasqeStub(harness).api;
    harness.document.cookie = 'server-user-password=%%%';
    const alerts = [];
    harness.context.alert = (message) => alerts.push(message);
    harness.loadScripts(['saved-queries.js']);
    harness.context.workbench.savedQueries.mount(harness.document.body);
    assert.doesNotThrow(() => deleteButton.dispatchEvent({ type: 'click', bubbles: true }));
    await new Promise((resolve) => setImmediate(resolve));
    assert.equal(alerts.length, 0);
    assert.equal(form.submitCount, 1, 'the server checks whether deletion is authorized');
});

for (const [label, parameters, expected] of [
    ['no URL parameters', null, 'INSERT DATA {}'],
    ['undefined URL parameters', undefined, 'INSERT DATA {}'],
    ['empty URL parameters', {}, 'INSERT DATA {}'],
    ['unrelated query parameters', { query: 'ASK {}' }, 'INSERT DATA {}'],
    ['a shared update', { update: 'DELETE WHERE {}' }, 'DELETE WHERE {}']
]) {
    test('Update mounts when YASQE consumes ' + label + ' during construction', () => {
        const harness = createFormBrowserHarness({ globals: { namespaces: {} } });
        const textarea = harness.registerElement('textarea', { id: 'update', value: 'INSERT DATA {}' });
        harness.document.body.appendChild(textarea);
        const yasqe = createYasqeStub(harness);
        const createEditor = yasqe.api.fromTextArea;
        yasqe.api.fromTextArea = (element, options) => {
            const editor = createEditor(element, options);
            // The bundled YASQE invokes this callback synchronously; getUrlParams() returns null for plain URLs.
            options.consumeShareLink(editor, parameters);
            return editor;
        };
        harness.context.YASQE = yasqe.api;
        harness.loadScripts(['yasqeHelper.js', 'update.js']);

        const dispose = harness.context.workbench.update.mount(harness.document.body);

        assert.equal(yasqe.state.instance.getValue(), expected);
        dispose();
        assert.equal(yasqe.state.instance.closed, true);
    });
}

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

    harness.context.workbench.update.mount(harness.document.body);

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
    assert.equal(instance.options.screenReaderLabel, 'SPARQL update', 'the editor names what it edits for screen readers');
    instance.options.extraKeys['Ctrl-Enter']();
    const form = harness.registerElement('form', { id: 'update-form' });
    let requested = 0;
    form.requestSubmit = () => { requested++; };
    instance.options.extraKeys['Ctrl-Enter']();
    instance.options.extraKeys['Cmd-Enter']();
    assert.equal(requested, 2);
});

// YASQE draws Share and Full screen as plain divs: they become buttons a keyboard can reach and press.
test('yasqe helper makes the editor toolbar controls keyboard buttons with names', () => {
    const harness = createFormBrowserHarness();
    harness.context.YASQE = createYasqeStub(harness).api;
    harness.loadScripts(['yasqeHelper.js']);
    const wrapper = harness.registerElement('div', { className: 'CodeMirror' });
    const share = harness.registerElement('div', { className: 'svgImg yasqe_share' });
    const fullscreen = harness.registerElement('div', { className: 'svgImg yasqe_fullscreenBtn' });
    wrapper.appendChild(share);
    wrapper.appendChild(fullscreen);
    const editor = { getWrapperElement: () => wrapper };
    harness.context.workbench.yasqeHelper.enableToolbarKeyboard(editor);
    harness.context.workbench.yasqeHelper.enableToolbarKeyboard(editor);

    assert.deepEqual([share, fullscreen].map(control => [control.getAttribute('role'), control.getAttribute('tabindex'),
        control.getAttribute('aria-label')]), [['button', '0', 'Share query'], ['button', '0', 'Full screen']]);
    let clicks = 0;
    fullscreen.addEventListener('click', () => clicks++);
    const enter = fullscreen.trigger('keydown', { key: 'Enter' });
    fullscreen.trigger('keydown', { key: ' ' });
    fullscreen.trigger('keydown', { key: 'a' });
    assert.equal(clicks, 2, 'Enter and Space press the control once each, however often the editor was prepared');
    assert.equal(enter.defaultPrevented, true, 'the key does not also reach the page');
});

// An empty editor says what goes in it: a quiet line where the text starts, hidden from screen readers (the editor's
// field already has a name) and gone as soon as there is text.
test('yasqe helper shows a hint in an empty editor until it has text', () => {
    const harness = createFormBrowserHarness();
    harness.context.YASQE = createYasqeStub(harness).api;
    harness.loadScripts(['yasqeHelper.js']);
    const wrapper = harness.registerElement('div', { className: 'CodeMirror' });
    const gutters = harness.registerElement('div', { className: 'CodeMirror-gutters' });
    gutters.offsetWidth = 31;
    let value = '';
    const handlers = {};
    const editor = {
        getWrapperElement: () => wrapper,
        getGutterElement: () => gutters,
        getValue: () => value,
        on(name, handler) { (handlers[name] = handlers[name] || []).push(handler); }
    };
    harness.context.workbench.yasqeHelper.showEmptyHint(editor, 'Write a SPARQL query');
    const hint = wrapper.querySelector('.workbench-editor-hint');
    assert.ok(hint, 'the hint is in the editor');
    assert.equal(hint.textContent, 'Write a SPARQL query');
    assert.equal(hint.getAttribute('aria-hidden'), 'true');
    assert.equal(hint.hidden, false);
    assert.equal(hint.style.left, '35px', 'the hint starts where the first line\'s text starts, after the gutters');
    value = 'SELECT';
    handlers.change.forEach(handler => handler());
    assert.equal(hint.hidden, true, 'text hides the hint');
    value = '';
    handlers.change.forEach(handler => handler());
    assert.equal(hint.hidden, false, 'an editor emptied again shows it again');
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

    // The completer hands YASQE the given namespaces at once; the fallback namespaces follow when they arrive
    // (workbench-fallback-namespaces.test.js).
    const lists = [];
    completer.get(null, (list) => lists.push(Array.from(list)));
    assert.deepEqual(lists[0], [
        'ex <http://example.com/>',
        'foaf <http://xmlns.com/foaf/0.1/>'
    ]);
    assert.equal(completer.bulk, true);
    assert.equal(completer.async, true);
    assert.equal(completer.autoShow, true);
    assert.equal(completer.isValidCompletionPosition(), false);
    assert.equal(completer.preProcessToken('tok'), 'editor:tok');
    assert.equal(yasqe.state.appendPrefixCalls.length, 1);
});

// Plan task M9.1: Update and Saved queries mount, dispose and mount again.
test('the Update route closes its editor on dispose and opens one again on the next mount', () => {
    const harness = createFormBrowserHarness({ globals: { namespaces: {} } });
    const page = harness.registerElement('div', { id: 'page' });
    const update = harness.registerElement('textarea', { id: 'update', value: 'DELETE WHERE {}' });
    page.appendChild(update);
    page.appendChild(harness.registerElement('div', { id: 'update-editor-resize' }));
    harness.document.body.appendChild(page);
    const yasqe = createYasqeStub(harness);
    harness.context.YASQE = yasqe.api;
    harness.loadScripts(['yasqeHelper.js', 'update.js']);
    const released = [];
    harness.context.workbench.editorSizing.install = () => () => released.push('sizing');

    const first = harness.context.workbench.update.mount(page);
    const firstEditor = yasqe.state.instance;
    first();
    assert.equal(firstEditor.closed, true);
    assert.deepEqual(released, ['sizing']);
    assert.equal(harness.context.workbench.update.doSubmit(), true, 'a submit after dispose is harmless');
    const second = harness.context.workbench.update.mount(page);

    assert.notEqual(yasqe.state.instance, firstEditor);
    assert.equal(yasqe.state.instance.closed, undefined);
    second();
});

test('the Saved queries route unbinds its buttons and closes opened editors on dispose', () => {
    const harness = createFormBrowserHarness();
    const page = harness.registerElement('div', { id: 'page' });
    const toggle = harness.registerElement('button', { id: 'urn-1-toggle', className: 'saved-query-toggle',
        attributes: { 'data-query-urn': 'urn-1', 'aria-expanded': 'false' } });
    const metadata = harness.registerElement('div', { id: 'urn-1-metadata' });
    metadata.style.display = 'none';
    const text = harness.registerElement('textarea', { id: 'urn-1-text', value: ' SELECT * {} ' });
    text.style.display = 'none';
    const remove = harness.registerElement('button', { className: 'saved-query-delete',
        attributes: { 'data-query-owner': 'alice', 'data-query-name': 'q', 'data-query-urn': 'urn-1' } });
    [toggle, metadata, text, remove].forEach((element) => page.appendChild(element));
    harness.document.body.appendChild(page);
    const yasqe = createYasqeStub(harness);
    harness.context.YASQE = yasqe.api;
    harness.loadScripts(['saved-queries.js']);

    const first = harness.context.workbench.savedQueries.mount(page);
    assert.equal(page.listenerCount('click'), 1, 'one stable outlet listener serves every card');
    toggle.dispatchEvent({ type: 'click', bubbles: true });
    const opened = yasqe.state.instance;
    first();
    assert.equal(opened.closed, true, 'an opened editor is closed');
    assert.equal(page.listenerCount('click'), 0);
    assert.equal(toggle.listenerCount('click'), 0);
    assert.equal(remove.listenerCount('click'), 0);
    const second = harness.context.workbench.savedQueries.mount(page);
    assert.equal(page.listenerCount('click'), 1, 'one stable outlet listener serves every card');
    second();
});
