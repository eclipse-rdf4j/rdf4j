// Review fixes (TS-A): write forms whose page script has not loaded yet must never submit natively.
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { installDetailDisclosureTemplateRuntime } = require('./workbench-detail-disclosure-runtime.js');

const scripts = process.env.WORKBENCH_SCRIPT_DIR
    || path.resolve(__dirname, '../../tools/workbench/src/main/webapp/scripts');

function loadViews(search) {
    const window = { location: { href: 'http://x.test/workbench/repositories/NONE/delete' + (search || ''),
        search: search || '' } };
    const workbench = {};
    installDetailDisclosureTemplateRuntime(workbench);
    const context = vm.createContext({ console, URL, URLSearchParams, Promise, window, workbench, setTimeout });
    vm.runInContext(fs.readFileSync(path.join(scripts, 'workbenchViews.js'), 'utf8'), context,
        { filename: 'workbenchViews.js' });
    return { wb: context.workbench, window };
}

function runtime() {
    return { nothing: '', html(strings, ...values) { return { strings: Array.from(strings), values }; }, render() {} };
}

function handlers(template, out = []) {
    if (Array.isArray(template)) { template.forEach((item) => handlers(item, out)); return out; }
    if (template && Array.isArray(template.strings)) {
        template.values.forEach((value, index) => {
            if (typeof value === 'function') {
                out.push({ before: template.strings.slice(0, index + 1).join('').slice(-200), fn: value });
            } else { handlers(value, out); }
        });
    }
    return out;
}

function quiet(fn) {
    const error = console.error;
    console.error = () => {};
    try { return fn(); } finally { console.error = error; }
}

function submitEvent(form) {
    return { defaultPrevented: false, target: form, currentTarget: form,
        preventDefault() { this.defaultPrevented = true; } };
}

const settle = () => new Promise(setImmediate);
const ctx = { basePath: '/workbench', repositoryId: 'NONE', workbench: {} };

function deleteSubmit(views) {
    const model = { viewId: 'delete', vars: ['id', 'description'], rows: [], rowCount: 0, metadata: {},
        pickerRows: [['prod', 'Production']], pickerPageSize: 50 };
    const template = quiet(() => views.wb.views.pageTemplate(model, ctx, runtime()));
    return handlers(template).find((entry) => /id="delete-form"[\s\S]*@submit=$/.test(entry.before)).fn;
}

test('A41: Delete never posts natively before delete.js is loaded; it confirms once the script arrives', async () => {
    const views = loadViews('?id=prod');
    const submit = deleteSubmit(views);
    const checks = [];
    let release;
    views.wb.app = { loadScripts: (names) => new Promise((resolve) => {
        release = () => {
            views.window.checkIsSafeToDelete = (event) => checks.push(event);
            resolve();
        };
        assert.deepEqual(Array.from(names), ['delete.js']);
    }) };
    const form = { isConnected: true, closest: () => null };
    const event = submitEvent(form);
    submit(event);
    assert.equal(event.defaultPrevented, true, 'the browser does not delete the repository without a confirmation');
    assert.equal(checks.length, 0);
    release();
    await settle();
    assert.equal(checks.length, 1, 'the typed-id confirmation runs once delete.js is there');
});

test('A41: Delete sends nothing when delete.js cannot be loaded', async () => {
    const views = loadViews('?id=prod');
    const submit = deleteSubmit(views);
    views.wb.app = { loadScripts: () => Promise.reject(new Error('offline')) };
    const event = submitEvent({ isConnected: true, closest: () => null });
    quiet(() => submit(event));
    await settle();
    assert.equal(event.defaultPrevented, true);
});

test('A41: the create configuration form does not submit without its overwrite check', () => {
    const views = loadViews();
    const model = { viewId: 'create', vars: ['fieldId', 'templateType', 'templateLabel'],
        rows: [['id', 'memory', 'Memory Store']], rowCount: 1, metadata: {} };
    const template = quiet(() => views.wb.views.pageTemplate(model, ctx, runtime()));
    const click = handlers(template).find((entry) => /id="create"[^>]*@click=$/.test(entry.before)).fn;
    let submitted = 0;
    const form = { requestSubmit() { submitted++; }, submit() { submitted++; } };
    const event = submitEvent(null);
    event.currentTarget = { closest: () => form };
    click(event);
    assert.equal(event.defaultPrevented, true);
    assert.equal(submitted, 0, 'without create.js nobody checked whether the id replaces an existing repository');
});

test('A41: the server form never posts the raw password before server.js is loaded', async () => {
    const views = loadViews();
    const model = { viewId: 'server', vars: ['server'], rows: [], rowCount: 0, metadata: {} };
    const template = quiet(() => views.wb.views.pageTemplate(model, ctx, runtime()));
    const submit = handlers(template).find((entry) => /id="server-form"[\s\S]*@submit=$/.test(entry.before)).fn;
    const changes = [];
    views.wb.app = { loadScripts: () => {
        views.window.changeServer = (event) => changes.push(event);
        return Promise.resolve();
    } };
    const event = submitEvent({ isConnected: true, closest: () => null });
    submit(event);
    assert.equal(event.defaultPrevented, true, 'the password is not posted as server-password');
    await settle();
    assert.equal(changes.length, 1, 'server.js encodes the credentials once it is loaded');
});

test('A42: an in-place write the server rejects says why instead of suggesting it may have happened', async () => {
    const views = loadViews();
    views.window.workbench = views.wb;
    const model = { viewId: 'update', vars: [], rows: [], rowCount: 0, metadata: {} };
    const template = quiet(() => views.wb.views.pageTemplate(model, ctx, runtime()));
    const submit = handlers(template).find((entry) => /id="update-form"[\s\S]*@submit=$/.test(entry.before)).fn;
    views.wb.router = { send: (form, submitter, failed) => {
        failed({ status: 409, message: 'The server answered 409 Conflict: Failed SHACL validation' });
        return Promise.resolve('failed');
    } };
    const form = { isConnected: true, closest: () => null };
    submit({ preventDefault() {}, currentTarget: form, submitter: null });
    await settle();
    assert.equal(model.submission.state, 'failed');
    assert.match(model.submission.message, /409 Conflict: Failed SHACL validation/);
    assert.doesNotMatch(model.submission.message, /did not confirm/);
});

test('A42: an in-place write without any answer still asks to check the repository', async () => {
    const views = loadViews();
    views.window.workbench = views.wb;
    const model = { viewId: 'update', vars: [], rows: [], rowCount: 0, metadata: {} };
    const template = quiet(() => views.wb.views.pageTemplate(model, ctx, runtime()));
    const submit = handlers(template).find((entry) => /id="update-form"[\s\S]*@submit=$/.test(entry.before)).fn;
    views.wb.router = { send: (form, submitter, failed) => {
        failed({ status: 0, message: '' });
        return Promise.resolve('failed');
    } };
    submit({ preventDefault() {}, currentTarget: { isConnected: true, closest: () => null }, submitter: null });
    await settle();
    assert.match(model.submission.message, /did not confirm/);
});

function flat(template) {
    if (Array.isArray(template)) { return template.map(flat).join(''); }
    if (template && Array.isArray(template.strings)) {
        return template.strings.map((part, index) => part
            + (index < template.values.length ? flat(template.values[index]) : '')).join('');
    }
    return template == null || typeof template === 'function' ? '' : String(template);
}

test('A9: the page that sent a refused form shows why above its own content', () => {
    const views = loadViews();
    const model = { viewId: 'delete', vars: ['id', 'description'], rows: [], rowCount: 0, metadata: {},
        pickerRows: [], pickerPageSize: 50,
        sendFailure: { status: 500, message: 'The server answered 500: Repository prod is in use' } };
    const markup = flat(quiet(() => views.wb.views.pageTemplate(model, ctx, runtime())));
    const failure = markup.indexOf('id="workbench-send-failure"');
    assert.ok(failure > 0, 'a failure callout is shown');
    assert.ok(failure < markup.indexOf('id="delete-form"'), 'above the form, which keeps its input');
    assert.match(markup, /Repository prod is in use/);
    assert.match(markup.slice(failure, failure + 400), /role="alert"/);
});

test('protected Delete renders a blank masked field and keeps the password out of model state', () => {
    const views = loadViews('?id=prod');
    const model = { viewId: 'delete', vars: ['id', 'description', 'error-message'],
        rows: [['prod', 'Production', 'Repository deletion requires a valid deletion password.']], rowCount: 1,
        pickerRows: [['prod', 'Production', 'Repository deletion requires a valid deletion password.']], pickerPageSize: 50,
        metadata: { 'repository-deletion-password-required': true, selectedRepositoryId: 'prod' } };
    const template = quiet(() => views.wb.views.pageTemplate(model, ctx, runtime()));
    const markup = flat(template);
    assert.match(markup, /id="delete-password" name="delete-password" type="password" autocomplete="off" required/);
    assert.match(markup, /valid deletion password/);
    const input = handlers(template).find((entry) => /@input=$/.test(entry.before)
        && /delete-password-help/.test(entry.before)).fn;
    const action = { disabled: true };
    const selected = { value: 'prod' };
    const password = { form: null, value: '' };
    const form = { querySelector: (selector) => selector === '#id' ? selected : selector === '#delete-password' ? password : action };
    password.form = form;
    input({ target: password });
    assert.equal(action.disabled, true);
    password.value = '  ';
    input({ target: password });
    assert.equal(action.disabled, false, 'whitespace is not trimmed');
    selected.value = '';
    input({ target: password });
    assert.equal(action.disabled, true);
    assert.equal(JSON.stringify(model).includes('  '), false, 'typing stores no password in the page model');
});

test('windowed Delete retains its error outside the picker rows so paging cannot hide the retry message', () => {
    const views = loadViews('?id=prod');
    const model = { viewId: 'delete', vars: ['id', 'description', 'error-message'], rows: [], rowCount: 1,
        pickerRows: [['prod', 'Production', 'Repository deletion requires a valid deletion password.']], pickerPageSize: 50,
        metadata: { 'repository-deletion-password-required': true, selectedRepositoryId: 'prod' } };
    assert.match(flat(quiet(() => views.wb.views.pageTemplate(model, ctx, runtime()))), /valid deletion password/);
    model.pickerRows = [['other', 'Another repository', null]];
    assert.match(flat(quiet(() => views.wb.views.pageTemplate(model, ctx, runtime()))), /valid deletion password/);
});

test('rerendering Delete derives its enabled state from the live secret without storing that secret in the model', () => {
    const views = loadViews('?id=prod');
    const model = { viewId: 'delete', vars: ['id', 'description'], rows: [], rowCount: 1,
        pickerRows: [['prod', 'Production']], pickerPageSize: 50,
        metadata: { 'repository-deletion-password-required': true, selectedRepositoryId: 'prod' } };
    const selected = { value: 'prod' };
    const password = { value: 'live secret' };
    const action = { disabled: true };
    const form = { querySelector: (selector) => selector === '#id' ? selected : selector === '#delete-password' ? password : action };
    const outlet = { querySelector: (selector) => selector === '#delete-form' ? form : null };
    const lit = runtime();
    lit.render = () => { action.disabled = true; };
    quiet(() => views.wb.views.renderOutlet(outlet, model, ctx, lit));
    assert.equal(action.disabled, false);
    assert.equal(JSON.stringify(model).includes('live secret'), false);
    password.value = '';
    quiet(() => views.wb.views.renderOutlet(outlet, model, ctx, lit));
    assert.equal(action.disabled, true);
    password.value = 'live secret';
    model.metadata['repository-deletion-policy-error'] = true;
    quiet(() => views.wb.views.renderOutlet(outlet, model, ctx, lit));
    assert.equal(action.disabled, true, 'a discovery failure blocks deletion despite a nonempty field');
    model.metadata['repository-deletion-policy-error'] = false;
    form.querySelector = (selector) => selector === '#id' ? selected : selector === '#delete-password' ? null : action;
    quiet(() => views.wb.views.renderOutlet(outlet, model, ctx, lit));
    assert.equal(action.disabled, true, 'required protection fails closed when the field is absent');
});

test('unprotected Delete has no password field and discovery failure visibly disables deletion', () => {
    const views = loadViews('?id=prod');
    const model = { viewId: 'delete', vars: ['id', 'description'], rows: [], rowCount: 0,
        pickerRows: [['prod', 'Production']], pickerPageSize: 50,
        metadata: { 'repository-deletion-password-required': false } };
    assert.doesNotMatch(flat(quiet(() => views.wb.views.pageTemplate(model, ctx, runtime()))), /type="password"/);
    model.metadata['repository-deletion-policy-error'] = true;
    const markup = flat(quiet(() => views.wb.views.pageTemplate(model, ctx, runtime())));
    assert.match(markup, /Unable to determine whether a deletion password is required/);
    assert.match(markup, /workbench-action--danger-outline" \?disabled=true/);
    model.metadata['error-message'] = 'Unable to determine whether a deletion password is required. Reload this page to try again.';
    const failed = flat(quiet(() => views.wb.views.pageTemplate(model, ctx, runtime())));
    assert.equal((failed.match(/Unable to determine whether a deletion password is required/g) || []).length, 1);
});

test('choosing another repository clears a live deletion password and retains only the selection', () => {
    const views = loadViews('?id=prod');
    const model = { viewId: 'delete', vars: ['id', 'description'], rows: [], rowCount: 0,
        pickerRows: [['prod', 'Production'], ['test', 'Test']], pickerPageSize: 50,
        metadata: { 'repository-deletion-password-required': true } };
    const template = quiet(() => views.wb.views.pageTemplate(model, ctx, runtime()));
    const change = handlers(template).find((entry) => /data-workbench-window-picker="repositories" @change=$/.test(entry.before)).fn;
    const password = { value: 'live secret' };
    const action = { disabled: false };
    change({ target: { value: 'test', form: { querySelector: (selector) => selector === '#delete-password' ? password : action },
        closest: () => null } });
    assert.equal(password.value, '');
    assert.equal(action.disabled, true, 'clearing the password disables Delete before any Lit rerender');
    assert.equal(model.metadata.selectedRepositoryId, 'test');
    assert.equal(JSON.stringify(model).includes('live secret'), false);
});

test('A20: the Add page says a target graph may be written with or without angle brackets', () => {
    const views = loadViews();
    const model = { viewId: 'add', vars: [], rows: [], rowCount: 0, metadata: {} };
    const markup = flat(quiet(() => views.wb.views.pageTemplate(model, ctx, runtime())));
    const help = markup.slice(markup.indexOf('id="context-help"'), markup.indexOf('</p>', markup.indexOf('id="context-help"')));
    assert.match(help, /&lt;|<http/, 'the <IRI> form is named');
    assert.match(help, /http:\/\/example\.org\/graph(?!>)/, 'a bare IRI is named too');
});
