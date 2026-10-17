// Repository deletion protection: the password stays in its live input only, and confirmation belongs to one form.
const test = require('node:test');
const assert = require('node:assert/strict');
const { createFormBrowserHarness } = require('./form-browser-harness.js');

const settle = () => new Promise(setImmediate);

function fixture(options = {}) {
    const harness = createFormBrowserHarness(options);
    const form = harness.registerElement('form', { id: 'delete-form' });
    const id = harness.registerElement('select', { id: 'id', name: 'id', value: 'repo-1' });
    const password = options.protected === false ? null
        : harness.registerElement('input', { id: 'delete-password', name: 'delete-password', type: 'password', value: ' secret 🔑 ' });
    const action = harness.registerElement('button', { type: 'submit' });
    const feedback = harness.registerElement('div', { id: 'delete-feedback' });
    [id, password, action, feedback].filter(Boolean).forEach((element) => form.appendChild(element));
    harness.document.body.appendChild(form);
    harness.loadScripts(['delete.js']);
    const event = { target: form, preventDefault() {} };
    return { ...harness, form, id, password, action, feedback,
        submit: () => harness.context.checkIsSafeToDelete(event) };
}

test('a protected form sends nothing until its password is nonempty; whitespace remains meaningful', () => {
    const page = fixture();
    page.password.value = '';
    page.submit();
    assert.equal(page.ajaxRequests.length, 0);
    page.password.value = '  ';
    page.submit();
    assert.equal(page.ajaxRequests.length, 1);
    assert.equal(page.ajaxRequests[0].params.has('delete-password'), false, 'the safety GET carries no secret');
});

test('the safety request keeps the dialog opener focusable so keyboard cancellation can restore focus', () => {
    const page = fixture();
    page.action.focus();
    page.submit();
    assert.equal(page.action.disabled, false, 'disabling a focused opener loses native browser focus before the modal opens');
});

test('repeated submits create one safety request, one typed-ID dialog, and one POST', async () => {
    const page = fixture({ confirmResponses: [true] });
    let complete;
    let posts = 0;
    let sentPassword;
    page.workbench.submitForm = () => {
        posts++;
        sentPassword = page.password.value;
        return new Promise((resolve) => { complete = resolve; });
    };
    page.submit();
    page.submit();
    assert.equal(page.ajaxRequests.length, 1);
    page.ajaxRequests[0].resolve({ safe: true });
    page.submit();
    await settle();
    page.submit();
    assert.equal(posts, 1);
    assert.equal(page.confirms.length, 1);
    assert.match(page.confirms[0], /Type repo-1 to confirm/);
    assert.equal(sentPassword, ' secret 🔑 ');
    assert.equal(page.password.value, '', 'the live secret is cleared immediately after serialization');
    assert.equal(page.action.disabled, true);
    complete('failed');
    await settle();
    page.password.value = 'retry';
    page.submit();
    assert.equal(page.ajaxRequests.length, 2, 'a failed POST releases the lock for an explicit retry');
});

test('a safety failure releases the lock and a cancelled dialog restores a usable action', async () => {
    const page = fixture({ confirmResponses: [false] });
    page.submit();
    page.ajaxRequests[0].reject('timeout', 'timeout');
    assert.equal(page.action.disabled, false);
    page.submit();
    page.ajaxRequests[1].resolve({ safe: false });
    await settle();
    assert.equal(page.form.submitCount, 0);
    assert.equal(page.action.disabled, false);
    assert.match(page.confirms[0], /proxies this one/);
    page.submit();
    assert.equal(page.ajaxRequests.length, 3);
});

for (const stage of ['safety', 'dialog']) {
    test(`a changed repository invalidates the ${stage} confirmation`, async () => {
        const page = fixture();
        let answer;
        page.workbench.confirmDialog.open = () => new Promise((resolve) => { answer = resolve; });
        page.submit();
        if (stage === 'dialog') { page.ajaxRequests[0].resolve({ safe: true }); }
        page.id.value = 'repo-2';
        if (stage === 'dialog') { answer(true); } else { page.ajaxRequests[0].resolve({ safe: true }); }
        await settle();
        assert.equal(page.form.submitCount, 0);
        page.submit();
        assert.equal(page.ajaxRequests.length, 2, 'the next repository can start its own confirmation');
    });
}

test('a confirmation from a detached form cannot submit a replacement form with the same repository', async () => {
    const page = fixture();
    let answer;
    let current = true;
    page.workbench.captureFormOwner = () => ({ url: null, isCurrent: () => current });
    page.workbench.confirmDialog.open = () => new Promise((resolve) => { answer = resolve; });
    page.submit();
    page.ajaxRequests[0].resolve({ safe: true });
    current = false;
    answer(true);
    await settle();
    assert.equal(page.form.submitCount, 0);
});

test('unprotected forms retain their typed-ID workflow and synchronous submission errors allow retry', async () => {
    const page = fixture({ protected: false, confirmResponses: [true] });
    page.workbench.submitForm = () => { throw new Error('offline'); };
    page.submit();
    page.ajaxRequests[0].resolve({ safe: true });
    await settle();
    assert.match(page.feedback.textContent, /could not be sent/);
    assert.equal(page.action.disabled, false);
    page.submit();
    assert.equal(page.ajaxRequests.length, 2);
});

test('an unprotected form reused for a new model cannot retain an old POST disabled state', async () => {
    const page = fixture({ protected: false, confirmResponses: [true] });
    let finish;
    let current = true;
    page.workbench.captureFormOwner = () => ({ url: null, isCurrent: () => current });
    page.workbench.submitForm = () => new Promise((resolve) => { finish = resolve; });
    page.submit();
    page.ajaxRequests[0].resolve({ safe: true });
    await settle();
    current = false;
    finish('committed');
    await settle();
    assert.equal(page.action.disabled, false, 'Lit may reuse this form for the error model and its unchanged binding');
});
