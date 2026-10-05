// Review fixes (TS-A): the Query page form must not send controls the Workbench policy turned off.
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { installDetailDisclosureTemplateRuntime } = require('./workbench-detail-disclosure-runtime.js');

const scripts = process.env.WORKBENCH_SCRIPT_DIR
    || path.resolve(__dirname, '../../tools/workbench/src/main/webapp/scripts');

/** The Query page markup for these policy features, with an optional cookie jar for the signed-in user. */
function queryMarkup(queryFeatures, options = {}) {
    const workbench = {};
    installDetailDisclosureTemplateRuntime(workbench);
    const sandbox = {
        URL,
        URLSearchParams,
        window: { location: { search: '' }, atob: (value) => Buffer.from(value, 'base64').toString('binary') },
        workbench
    };
    if (typeof options.cookie === 'string') {
        sandbox.document = { cookie: options.cookie };
    }
    const context = vm.createContext(sandbox);
    vm.runInContext(fs.readFileSync(path.join(scripts, 'workbenchViews.js'), 'utf8'), context,
        { filename: 'workbenchViews.js' });
    const runtime = {
        nothing: '',
        html(strings, ...values) { return { strings: Array.from(strings), values }; },
        render() {}
    };
    const template = context.workbench.views.pageTemplate({ viewId: 'query', vars: [], rows: [], metadata: {} }, {
        basePath: '/workbench', repositoryId: 'repo-1',
        workbench: { defaults: { 'default-infer': 'true' }, queryFeatures,
            queryFormats: [{ value: 'SPARQL', label: 'SPARQL' }, { value: 'SERQL', label: 'SeRQL' }] }
    }, runtime);
    return flat(template);
}

function flat(template) {
    if (Array.isArray(template)) { return template.map(flat).join(''); }
    if (template && Array.isArray(template.strings)) {
        return template.strings.map((part, index) => part
            + (index < template.values.length ? flat(template.values[index]) : '')).join('');
    }
    return template == null || typeof template === 'function' ? '' : String(template);
}

/** The start tag of the element with this id. */
function tagOf(markup, id) {
    const at = markup.indexOf('id="' + id + '"');
    assert.ok(at >= 0, id + ' is rendered');
    return markup.slice(markup.lastIndexOf('<', at), markup.indexOf('>', at) + 1).replace(/\s+/g, ' ');
}

test('A19: policy-disabled query language, timeout and inference controls are disabled so the form omits them', () => {
    const markup = queryMarkup({ 'query-language': false, 'query-timeout': false, 'query-inferred-statements': false });
    assert.match(tagOf(markup, 'queryLn'), /\?disabled=true/, 'queryLn is not submitted');
    assert.match(tagOf(markup, 'query-timeout'), /\?disabled=true/, 'query-timeout is not submitted');
    assert.match(tagOf(markup, 'infer'), /\?disabled=true/, 'infer is not submitted');
});

test('A19: enabled query options stay submittable', () => {
    const markup = queryMarkup({});
    assert.match(tagOf(markup, 'queryLn'), /\?disabled=false/);
    assert.match(tagOf(markup, 'query-timeout'), /\?disabled=false/);
    assert.match(tagOf(markup, 'infer'), /\?disabled=false/);
});

test('A13: Private cannot be chosen while nobody is signed in', () => {
    const markup = queryMarkup({});
    const input = tagOf(markup, 'save-private');
    assert.match(input, /\?disabled=true/, 'an anonymous user cannot save privately');
    assert.match(input, /\?checked=false/);
    assert.match(markup, /id="save-private-help"[^>]*>[^<]*[Ss]ign in/, 'the page says why');
});

test('A13: a signed-in user can save privately', () => {
    const cookie = 'server-user-password=' + Buffer.from('alice:secret').toString('base64');
    const markup = queryMarkup({}, { cookie });
    assert.match(tagOf(markup, 'save-private'), /\?disabled=false/);
    assert.doesNotMatch(markup, /id="save-private-help"[^>]*>[^<]*[Ss]ign in/);
});

test('A13: Private stays off when the policy disables private saves, even when signed in', () => {
    const cookie = 'server-user-password=' + Buffer.from('alice:secret').toString('base64');
    const markup = queryMarkup({ 'query-private-save': false }, { cookie });
    assert.match(tagOf(markup, 'save-private'), /\?disabled=true/);
});
