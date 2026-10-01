const test = require('node:test');
const assert = require('node:assert/strict');

const { createFormBrowserHarness } = require('./form-browser-harness.js');

function appendOptions(harness, select, values) {
    values.forEach((value) => {
        const option = harness.registerElement('option', {
            value: value.value,
            textContent: value.text || value.value,
            selected: !!value.selected,
            attributes: {
                value: value.value
            }
        });
        select.appendChild(option);
        if (value.selected) {
            select.value = value.value;
        }
    });
}

test('template helpers chain loads, parse cookies, and update selected user', () => {
    const harness = createFormBrowserHarness({
        href: 'http://localhost:8080/rdf4j-workbench/create?id=repo-1&title=My+Repo'
    });
    harness.document.cookie = 'server-user-password=' + encodeURIComponent(Buffer.from('alice:secret').toString('base64'));
    const field = harness.registerElement('input', {
        id: 'flag',
        type: 'checkbox'
    });
    field.checked = true;
    harness.document.body.appendChild(field);

    harness.loadScripts([]);
    const calls = [];
    harness.context.workbench.addLoad(() => calls.push('first'));
    harness.context.workbench.addLoad(() => calls.push('second'));
    harness.runLoadHandlers();

    const params = [];
    harness.context.workbench.addParam(params, 'flag');

    assert.deepEqual(calls, ['first', 'second']);
    assert.equal(harness.context.workbench.getCookie('server-user-password'), Buffer.from('alice:secret').toString('base64'));
    assert.deepEqual(Array.from(harness.context.workbench.getQueryStringElements()), ['id=repo-1', 'title=My+Repo']);
    assert.deepEqual(params, ['flag=', 'true', '&']);
    assert.equal(harness.document.getElementById('noscript-message').style.display, 'none');
    // The server user is rendered by the shell (workbench.views.contextBarState), not patched into the DOM.
    assert.equal(harness.document.getElementById('selected-user').textContent, '');
});

test('add page handles context and source selection branches', () => {
    const harness = createFormBrowserHarness();
    const text = harness.registerElement('textarea', { id: 'text' });
    const file = harness.registerElement('input', { id: 'file', value: '/tmp/data.ttl' });
    const url = harness.registerElement('input', { id: 'url', value: 'https://example.test/data' });
    const baseURI = harness.registerElement('input', { id: 'baseURI', value: 'https://example.test/base' });
    const context = harness.registerElement('input', { id: 'context', value: '' });
    const sourceText = harness.registerElement('input', { id: 'source-text', type: 'radio' });
    const sourceFile = harness.registerElement('input', { id: 'source-file', type: 'radio' });
    const sourceUrl = harness.registerElement('input', { id: 'source-url', type: 'radio' });
    const contentType = harness.registerElement('select', { id: 'Content-Type' });

    appendOptions(harness, contentType, [
        { value: 'autodetect', text: 'Auto', selected: true },
        { value: 'application/x-turtle', text: 'Turtle' },
        { value: 'text/turtle', text: 'Legacy Turtle' }
    ]);

    [
        text,
        file,
        url,
        baseURI,
        context,
        sourceText,
        sourceFile,
        sourceUrl,
        contentType
    ].forEach((element) => harness.document.body.appendChild(element));

    harness.loadScripts(['add.js']);

    // The target graph is always editable (plan task M5.5): an empty value keeps the data's own graphs.
    assert.equal(harness.context.workbench.add.handleContextOverride, undefined);
    assert.equal(context.disabled, false);

    harness.context.workbench.add.enabledInput('text');
    assert.equal(text.disabled, false);
    assert.equal(file.disabled, true);
    assert.equal(url.disabled, true);
    assert.equal(sourceText.checked, true);
    assert.equal(contentType.getElementsByTagName('option')[0].disabled, true);
    assert.equal(contentType.getElementsByTagName('option')[1].selected, true);

    context.value = '';
    harness.context.workbench.add.enabledInput('file');
    assert.equal(file.disabled, false);
    assert.equal(baseURI.value, 'file:///tmp/data.ttl');
    assert.equal(context.value, '');
    assert.equal(context.disabled, false);

    harness.context.workbench.add.enabledInput('url');
    assert.equal(url.disabled, false);
    assert.equal(baseURI.value, 'https://example.test/data');
    assert.equal(context.value, '');
    assert.equal(context.disabled, false);
  });

/** Lets promise chains (confirmation dialogs, M6.2) finish. */
const settle = () => new Promise((resolve) => setImmediate(resolve));

test('create page resolves field roles, overwrite checks, and delayed enablement', async () => {
    const harness = createFormBrowserHarness({
        confirmResponses: [true, true],
        href: 'http://localhost:8080/rdf4j-workbench/create?id=repo-1&title=My+Repo'
    });
    const createForm = harness.registerElement('form', {
        attributes: { action: 'create' }
    });
    const id = harness.registerElement('input', {
        id: 'id',
        attributes: { 'data-field-role': 'repository-id' }
    });
    const title = harness.registerElement('input', {
        id: 'title',
        attributes: { 'data-field-role': 'repository-title' }
    });
    const createButton = harness.registerElement('input', {
        id: 'create',
        type: 'submit'
    });
    createForm.appendChild(id);
    createForm.appendChild(title);
    createForm.appendChild(createButton);
    harness.document.body.appendChild(createForm);

    harness.loadScripts(['create.js']);
    harness.workbench.create.mount(harness.document.body);

    assert.equal(id.value, 'repo-1');
    assert.equal(title.value, 'My Repo');
    assert.equal(createButton.disabled, false);
    assert.equal(harness.context.workbench.create.findFieldByRole('missing-role', '#title').get(0), title);

    id.value = '';
    id.trigger('keydown');
    harness.advanceTimers(0);
    assert.equal(createButton.disabled, true);

    id.value = 'invalid id';
    harness.context.checkOverwrite();
    const infoRequest = harness.ajaxRequests[0];
    infoRequest.resolve({});
    await settle();
    assert.equal(createForm.submitCount, 1);
    assert.equal(harness.confirms.length, 2);
    assert.match(harness.confirms[0], /Replace repository configuration\?/);
    assert.match(harness.confirms[1], /Use this repository id\?/);

    createForm.submitCount = 0;
    id.value = 'new-id';
    harness.context.checkOverwrite();
    harness.ajaxRequests[1].status(500);
    await settle();
    assert.equal(createForm.submitCount, 1);

    // Unknown repositories answer 404 since the in-shell not-found page; 500 stays accepted for older servers.
    createForm.submitCount = 0;
    harness.context.checkOverwrite();
    harness.ajaxRequests[2].status(404);
    await settle();
    assert.equal(createForm.submitCount, 1);
});

test('create federate page enables create only for valid member selection', () => {
    const harness = createFormBrowserHarness();
    const id = harness.registerElement('input', { id: 'id', value: 'fed' });
    const createButton = harness.registerElement('input', { id: 'create', type: 'submit' });
    const feedback = harness.registerElement('div', { id: 'create-feedback' });
    const recurseMessage = harness.registerElement('div', { id: 'recurse-message' });
    const memberA = harness.registerElement('input', {
        id: 'member-a',
        className: 'memberID',
        type: 'checkbox',
        checked: true,
        attributes: { value: 'repo-a' }
    });
    const memberB = harness.registerElement('input', {
        id: 'member-b',
        className: 'memberID',
        type: 'checkbox',
        checked: true,
        attributes: { value: 'repo-b' }
    });
    const typeRadio = harness.registerElement('input', {
        id: 'type-native',
        name: 'type',
        type: 'radio'
    });
    [
        id,
        createButton,
        feedback,
        recurseMessage,
        memberA,
        memberB,
        typeRadio
    ].forEach((element) => harness.document.body.appendChild(element));

    harness.loadScripts(['create-federate.js']);
    harness.workbench.createFederate.mount(harness.document.body);

    assert.equal(createButton.disabled, false);
    assert.equal(feedback.style.display, 'none');

    id.value = 'repo-a';
    memberB.checked = false;
    memberA.trigger('change');
    assert.equal(createButton.disabled, true);
    assert.equal(recurseMessage.style.display, '');
    assert.equal(feedback.style.display, '');

    memberB.checked = true;
    memberB.trigger('change');
    typeRadio.trigger('change');
    id.trigger('keydown');
    harness.advanceTimers(0);
    assert.equal(recurseMessage.style.display, '');
    assert.equal(createButton.disabled, true);
});

test('delete page reports timeout and successful unsafe-delete confirmation', async () => {
    const harness = createFormBrowserHarness({
        confirmResponses: [true]
    });
    const form = harness.registerElement('form', { id: 'delete-form' });
    const button = harness.registerElement('button', { id: 'delete-button' });
    const id = harness.registerElement('input', { id: 'id', value: 'repo-1' });
    const feedback = harness.registerElement('div', { id: 'delete-feedback' });
    form.appendChild(button);
    harness.document.body.appendChild(form);
    harness.document.body.appendChild(id);
    harness.document.body.appendChild(feedback);

    harness.loadScripts(['delete.js']);

    harness.context.checkIsSafeToDelete({
        target: button,
        preventDefault() {
        }
    });
    harness.ajaxRequests[0].reject('timeout', 'Timeout');
    assert.equal(feedback.textContent, 'The server seems unresponsive. Delete request not sent.');

    harness.context.checkIsSafeToDelete({
        target: button,
        preventDefault() {
        }
    });
    harness.ajaxRequests[1].resolve({ safe: false });
    await settle();
    assert.equal(form.submitCount, 1);
    // Every deletion asks for the typed id (plan task M6.6); a proxied repository adds a warning.
    assert.match(harness.confirms[0], /Delete repository repo-1\?/);
    assert.match(harness.confirms[0], /proxies this one/);
    assert.equal(feedback.textContent, '');
});

test('delete page asks for the typed id for every repository and keeps a cancelled one (M6.6)', async () => {
    const harness = createFormBrowserHarness({
        confirmResponses: [true, false]
    });
    const form = harness.registerElement('form', { id: 'delete-form' });
    const button = harness.registerElement('button', { id: 'delete-button' });
    const id = harness.registerElement('input', { id: 'id', value: 'repo-1' });
    const feedback = harness.registerElement('div', { id: 'delete-feedback' });
    form.appendChild(button);
    harness.document.body.appendChild(form);
    harness.document.body.appendChild(id);
    harness.document.body.appendChild(feedback);
    harness.loadScripts(['delete.js']);
    const event = { target: button, preventDefault() {} };

    harness.context.checkIsSafeToDelete(event);
    harness.ajaxRequests[0].resolve({ safe: true });
    await settle();
    assert.equal(form.submitCount, 1, 'typing the id and confirming deletes the repository');
    assert.match(harness.confirms[0], /Type repo-1 to confirm/);
    assert.doesNotMatch(harness.confirms[0], /proxies this one/, 'a repository nothing proxies gets no proxy warning');

    harness.context.checkIsSafeToDelete(event);
    harness.ajaxRequests[1].resolve({ safe: false });
    await settle();
    assert.equal(harness.confirms.length, 2);
    assert.equal(form.submitCount, 1, 'Cancel keeps the repository');
});

test('delete page reports generic server errors', () => {
    const harness = createFormBrowserHarness();
    const form = harness.registerElement('form', { id: 'delete-form' });
    const button = harness.registerElement('button', { id: 'delete-button' });
    const id = harness.registerElement('input', { id: 'id', value: 'repo-1' });
    const feedback = harness.registerElement('div', { id: 'delete-feedback' });
    form.appendChild(button);
    harness.document.body.appendChild(form);
    harness.document.body.appendChild(id);
    harness.document.body.appendChild(feedback);

    harness.loadScripts(['delete.js']);
    harness.context.checkIsSafeToDelete({
        target: button,
        preventDefault() {
        }
    });
    harness.ajaxRequests[0].reject('error', 'Forbidden');

    assert.match(feedback.textContent, /HTTP Status Text = "Forbidden"/);
});

test('server page rewrites password field when credentials are present', () => {
    const harness = createFormBrowserHarness();
    const form = harness.registerElement('form', { id: 'server-form' });
    const user = harness.registerElement('input', { id: 'server-user', value: 'alice' });
    const password = harness.registerElement('input', { id: 'server-password', value: 'secret' });
    form.appendChild(user);
    form.appendChild(password);
    harness.document.body.appendChild(form);

    harness.loadScripts(['server.js']);

    harness.context.changeServer({
        target: password,
        preventDefault() {
        }
    });
    assert.equal(password.name, 'server-user-password');
    assert.equal(password.value, Buffer.from('alice:secret').toString('base64'));
    assert.equal(form.submitCount, 1);

    password.name = '';
    password.value = '';
    harness.context.changeServer({
        target: password,
        preventDefault() {
        }
    });
    assert.equal(password.name, '');
    assert.equal(form.submitCount, 2);
});

test('confirmation dialog waits for the typed text and resolves false on Cancel (M6.2)', async () => {
    const harness = createFormBrowserHarness();
    harness.loadScripts([]);
    const component = harness.context.workbench.confirmDialog;
    const pending = component.open({ title: 'Delete repository?', body: 'This deletes bsbm and its data.',
        confirmLabel: 'Delete repository', danger: true, requireText: 'bsbm' });
    const dialog = harness.document.querySelectorAll('dialog')[0];
    assert.equal(dialog.open, true);
    assert.equal(dialog.querySelectorAll('h2')[0].textContent, 'Delete repository?');
    const [cancel, confirm] = dialog.querySelectorAll('button');
    assert.equal(cancel.textContent, 'Cancel');
    assert.equal(harness.document.activeElement, cancel, 'Cancel has the focus first');
    assert.equal(confirm.textContent, 'Delete repository');
    assert.ok(confirm.classList.contains('workbench-action--danger'));
    assert.equal(confirm.disabled, true);
    const input = dialog.querySelectorAll('input')[0];
    input.value = 'bsb';
    input.trigger('input');
    assert.equal(confirm.disabled, true);
    input.value = 'bsbm';
    input.trigger('input');
    assert.equal(confirm.disabled, false);
    cancel.click();
    assert.equal(await pending, false);
    assert.equal(dialog.parentNode, null, 'the dialog is removed once it closes');

    const confirmed = component.open({ title: 'Clear graph?', body: 'Clears 3 statements.', confirmLabel: 'Clear graph',
        danger: true });
    const second = harness.document.querySelectorAll('dialog')[0];
    const confirmButton = second.querySelectorAll('button')[1];
    assert.equal(confirmButton.disabled, false);
    confirmButton.click();
    assert.equal(await confirmed, true);
});

// Plan task M10.1: scripts submit forms through the router when it is there.
test('submitForm hands a form to the router when there is one, and submits it natively otherwise', () => {
    const harness = createFormBrowserHarness();
    harness.loadScripts([]);
    const form = harness.registerElement('form', { attributes: { action: 'clear', method: 'post' } });
    harness.document.body.appendChild(form);

    harness.context.workbench.submitForm(form);
    assert.equal(form.submitCount, 1, 'no router: a native submit');

    const routed = [];
    harness.context.workbench.router = { submit: (submitted) => routed.push(submitted) };
    harness.context.workbench.submitForm(form);
    assert.deepEqual(routed, [form]);
    assert.equal(form.submitCount, 1);
});
