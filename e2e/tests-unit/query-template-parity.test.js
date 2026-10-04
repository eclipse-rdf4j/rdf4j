const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { installDetailDisclosureTemplateRuntime } = require('./workbench-detail-disclosure-runtime.js');

const scripts = process.env.WORKBENCH_SCRIPT_DIR
    || path.resolve(__dirname, '../../tools/workbench/src/main/webapp/scripts');

function loadViews() {
    const workbench = {};
    installDetailDisclosureTemplateRuntime(workbench);
    const context = vm.createContext({
        URLSearchParams,
        window: { location: { search: '' } },
        workbench
    });
    const source = fs.readFileSync(path.join(scripts, 'workbenchViews.js'), 'utf8');
    vm.runInContext(source, context, { filename: 'workbenchViews.js' });
    return context.workbench.views;
}

function fakeRuntime() {
    return {
        nothing: Symbol('nothing'),
        html(strings, ...values) {
            return { strings: Array.from(strings), values };
        },
        render() {}
    };
}

function visitTemplates(value, templates = [], bindings = []) {
    if (Array.isArray(value)) {
        value.forEach((entry) => visitTemplates(entry, templates, bindings));
    } else if (value && Array.isArray(value.strings) && Array.isArray(value.values)) {
        templates.push(value);
        value.values.forEach((entry, index) => {
            bindings.push({ before: value.strings[index], value: entry, after: value.strings[index + 1] });
            visitTemplates(entry, templates, bindings);
        });
    }
    return { templates, bindings };
}

function queryTemplate({ metadata = {}, defaults = {}, queryFeatures = {}, queryFormats = [] } = {}) {
    const views = loadViews();
    const context = {
        basePath: '/workbench',
        repositoryId: 'repo-1',
        workbench: { defaults, queryFeatures, queryFormats }
    };
    return visitTemplates(views.pageTemplate({ viewId: 'query', vars: [], rows: [], metadata }, context, fakeRuntime()));
}

function savedQueryTemplate(queryTimeout, overrides = {}) {
    const views = loadViews();
    const row = Object.assign({
        query: 'urn:saved:1',
        queryName: 'Example query',
        queryText: 'SELECT * WHERE {}',
        user: 'alice',
        queryLn: 'SPARQL',
        infer: 'true',
        rowsPerPage: '20',
        shared: 'false'
    }, overrides);
    if (typeof queryTimeout !== 'undefined') {
        row.queryTimeout = queryTimeout;
    }
    const model = { viewId: 'saved-queries', vars: Object.keys(row), rows: [Object.values(row)], metadata: {} };
    const context = {
        basePath: '/workbench',
        repositoryId: 'repo-1',
        workbench: { defaults: { 'default-query-timeout': '23' } }
    };
    return visitTemplates(views.pageTemplate(model, context, fakeRuntime()));
}

function savedQueriesEmptyTemplate(menu) {
    const views = loadViews();
    return visitTemplates(views.pageTemplate({ viewId: 'saved-queries', vars: [], rows: [], metadata: {} }, {
        basePath: '/workbench',
        repositoryId: 'repo-1',
        workbench: { menu }
    }, fakeRuntime()));
}

function bindingFor(bindings, predicate, description) {
    const found = bindings.find(predicate);
    assert.ok(found, `query page should expose a ${description} template binding`);
    return found;
}

function dynamicAttributeValue(page, selector, attributeName) {
    for (const template of page.templates) {
        const annotated = template.strings.map((value, index) => value
            + (index < template.values.length ? `@@value_${index}@@` : '')).join('');
        const selectorIndex = annotated.indexOf(selector);
        if (selectorIndex < 0) {
            continue;
        }
        const tagEnd = annotated.indexOf('>', selectorIndex);
        const attributes = annotated.slice(selectorIndex, tagEnd < 0 ? annotated.length : tagEnd);
        const match = attributes.match(new RegExp(`${attributeName}=@@value_(\\d+)@@`));
        if (match) {
            return template.values[Number(match[1])];
        }
    }
    return undefined;
}

test('query page preserves configured, fallback, and explicit timeout values', () => {
    const configured = queryTemplate({ defaults: { 'default-query-timeout': '23' } });
    const configuredTimeout = bindingFor(configured.bindings,
        binding => binding.before.includes('id="query-timeout"') && binding.before.includes('value='),
        'configured timeout');
    assert.equal(configuredTimeout.value, '23');

    const fallback = queryTemplate({ defaults: { 'default-query-timeout': '' } });
    const fallbackTimeout = bindingFor(fallback.bindings,
        binding => binding.before.includes('id="query-timeout"') && binding.before.includes('value='),
        'fallback timeout');
    assert.equal(fallbackTimeout.value, '60');

    for (const explicit of ['17', '0']) {
        const page = queryTemplate({
            metadata: { 'query-timeout': explicit },
            defaults: { 'default-query-timeout': '23' }
        });
        const timeout = bindingFor(page.bindings,
            binding => binding.before.includes('id="query-timeout"') && binding.before.includes('value='),
            'explicit timeout');
        assert.equal(timeout.value, explicit,
            `an explicit ${explicit} second timeout should override the configured default`);
    }
});

test('query-page feature policy hides configured controls without rendering result page size', () => {
    const page = queryTemplate({
        defaults: { 'default-limit': '0', 'default-queryLn': 'SPARQL', 'default-infer': 'true' },
        queryFormats: ['SPARQL SPARQL 1.1', 'SERQL SeRQL'],
        queryFeatures: {
            'query-language': false,
            'query-execution': false,
            'query-explain': false,
            'query-options': false,
            'query-save': false,
            'query-timeout': false,
            'query-inferred-statements': false,
            'editor-namespaces': false,
            'result-fullscreen': false
        }
    });

    const hiddenBindings = [
        ['id="query-language-row"', 'query language'],
        ['id="exec"', 'query execution'],
        ['id="explain-trigger"', 'query explanation'],
        ['id="query-options-toggle"', 'query options'],
        ['id="save-query-toggle"', 'query saving'],
        ['id="query-timeout"', 'query timeout'],
        ['id="infer"', 'inferred statements'],
        ['id="query-insert-prefixes"', 'prefix insertion']
    ];
    hiddenBindings.forEach(([selector, description]) => {
        const hiddenValue = dynamicAttributeValue(page, selector, '\\?hidden');
        assert.notEqual(hiddenValue, undefined, `query page should include a hidden binding for ${description}`);
        assert.equal(hiddenValue, true,
            `${description} should follow the disabled feature policy`);
    });

    const fullscreen = bindingFor(page.bindings,
        binding => binding.before.includes('data-result-fullscreen-enabled='), 'result fullscreen policy');
    assert.equal(fullscreen.value, 'false');

    const staticMarkup = page.templates.map(template => template.strings.join('')).join('');
    assert.doesNotMatch(staticMarkup, /(?:id|name)="limit_query"|Rows Per Page/i,
        'result collection no longer renders a query page-size control');

    const languageOptions = page.templates.filter(template => template.strings.join('').includes('<option value=')
        && template.values.some(value => value === 'SPARQL' || value === 'SERQL'))
        .map(template => ({ value: template.values[0], selected: template.values[1] }));
    assert.deepEqual(languageOptions, [
        { value: 'SPARQL', selected: true },
        { value: 'SERQL', selected: false }
    ],
        'the query language choices should retain the configured format values');

    const inferredSelection = bindingFor(page.bindings,
        binding => binding.before.includes('?checked='), 'default inferred-statements choice');
    assert.equal(inferredSelection.value, true, 'the configured inferred-statements default should remain selected');
});

test('query language selector is omitted when SPARQL is the only available query format', () => {
    const page = queryTemplate({ queryFormats: ['SPARQL SPARQL 1.1'] });
    assert.equal(dynamicAttributeValue(page, 'id="query-language-row"', 'hidden'), true,
        'a single SPARQL format should not show a redundant language selector');
});

test('linked Info feature rows feed the query page visibility policy', () => {
    const views = loadViews();
    const info = views.linkedInfoMetadata({
        viewId: 'information',
        vars: ['query-feature-id', 'query-feature-enabled'],
        rows: [
            [{ kind: 'literal', value: 'query-language' }, { kind: 'literal', value: 'false' }],
            [{ kind: 'literal', value: 'query-execution' }, { kind: 'literal', value: 'false' }],
            [{ kind: 'literal', value: 'query-explain' }, { kind: 'literal', value: 'false' }],
            [{ kind: 'literal', value: 'query-options' }, { kind: 'literal', value: 'false' }]
        ]
    });
    const page = visitTemplates(views.pageTemplate({ viewId: 'query', vars: [], rows: [], metadata: {} }, {
        basePath: '/workbench',
        repositoryId: 'repo-1',
        workbench: info
    }, fakeRuntime()));

    assert.deepEqual(JSON.parse(JSON.stringify(info.queryFeatures)), {
        'query-language': false,
        'query-execution': false,
        'query-explain': false,
        'query-options': false
    });
	for (const selector of [
		'id="query-language-row"', 'id="exec"', 'id="explain-trigger"', 'id="query-options-toggle"'
	]) {
		const hidden = dynamicAttributeValue(page, selector, '\\?hidden');
		assert.notEqual(hidden, undefined, `the ${selector} route control should expose a policy binding`);
		assert.equal(hidden, true);
	}
});

test('query Explain is hidden when every explain level and format is disabled', () => {
    const queryFeatures = {};
    ['unoptimized', 'optimized', 'executed', 'telemetry', 'timed'].forEach(level => {
        queryFeatures['explain-level-' + level] = false;
    });
    ['text', 'dot', 'json'].forEach(format => {
        queryFeatures['explain-format-' + format] = false;
    });
    const page = queryTemplate({ queryFeatures });
    const explain = bindingFor(page.bindings,
        binding => binding.before.includes('<button id="explain-trigger"')
            && binding.before.includes('?hidden='), 'Explain availability');

    assert.equal(explain.value, true,
        'Explain should not be offered if the server disables every level and output format');
});

test('query page selects the level and format attached to the rendered explanation', () => {
    const page = queryTemplate({ metadata: { 'explanation-level': 'Timed', 'explanation-format': 'dot' } });
    const timedLevel = page.templates.find(template => template.values[0] === 'Timed'
        && template.strings.join('').includes('?selected='));
    const dotFormat = page.templates.find(template => template.values[0] === 'dot'
        && template.strings.join('').includes('<option value='));
    const formatTemplate = dotFormat
        ? null
        : page.templates.find(template => template.strings.join('').includes('id="explain-format"'));
    const dotBindingIndex = formatTemplate
        ? formatTemplate.strings.findIndex(value => value.includes('value="dot"') && value.includes('?selected='))
        : -1;

    assert.ok(timedLevel, 'the selected explanation level should remain available');
    assert.ok(dotFormat || (formatTemplate && dotBindingIndex >= 0), 'the explanation format selector should expose DOT');
    assert.equal(timedLevel.values[1], true,
        'the explanation level should remain selected while its explanation is displayed');
    assert.equal(dotFormat ? dotFormat.values[1] : formatTemplate.values[dotBindingIndex], true,
        'the explanation format should remain selected while its explanation is displayed');
});

test('comparison editor exposes an accessible close action', () => {
    const page = queryTemplate();
    const closeButton = page.templates.find(template => template.strings.join('')
        .includes('<button id="query-compare-close"'));

    assert.ok(closeButton, 'the comparison pane should provide a close control');
    assert.match(closeButton.strings.join(''), /aria-label="Close comparison"/);
    assert.ok(closeButton.values.some(value => typeof value === 'function'),
        'the accessible close control should be wired to the comparison-pane action');
});

test('query rerun policy hides only Explain again while keeping Execute available', () => {
    const page = queryTemplate({
        metadata: { explanation: 'Existing explanation' },
        queryFeatures: { 'query-rerun': false }
    });
    const rerun = bindingFor(page.bindings,
        binding => binding.before.includes('<button id="rerun-explanation"')
            && binding.before.includes('?hidden='), 'Explain again availability');
    assert.equal(rerun.value, true,
        'the disabled rerun policy should hide the Explain again action after an explanation exists');

    const execute = bindingFor(page.bindings,
        binding => binding.before.includes('<button id="exec"') && binding.before.includes('?hidden='),
        'query execution availability');
    assert.equal(execute.value, false,
        'disabling explanation reruns must not hide the independent Execute action');
});

test('running queries and explanations are cancelled with warning actions and show no spinners', () => {
    const page = queryTemplate({ metadata: { explanation: 'Existing explanation' } });
    const staticMarkup = page.templates.map(template => template.strings.join('')).join('');

    for (const id of ['query-cancel', 'explain-trigger-cancel', 'rerun-explanation-cancel', 'explain-compare-cancel']) {
        assert.match(staticMarkup, new RegExp(`<button id="${id}" class="[^"]*\\bworkbench-action--warning\\b`),
            `${id} is a warning button`);
    }
    const stopIcons = page.bindings.filter(binding => binding.before.endsWith('data-workbench-icon=')
        && binding.value === 'stop');
    assert.equal(stopIcons.length, 4, 'each Cancel button carries the stop icon');
    assert.doesNotMatch(staticMarkup, /query-explain-spinner|explain-trigger-cancel-action/,
        'the explanation shows its progress as the panel loading bar, not as a spinner beside the button');
});

test('individual query feature policies hide their matching controls and explanation views', () => {
    const queryFeatures = {};
    [
        'query-save', 'query-options', 'query-explain', 'query-private-save', 'query-timeout',
        'query-inferred-statements', 'query-cancel', 'query-compare',
        'query-diff', 'query-swap', 'query-rerun', 'query-refresh', 'result-page-size', 'result-fullscreen',
        'editor-sidebar', 'editor-fullscreen', 'editor-namespaces', 'explain-format-text', 'explain-format-dot',
        'explain-format-json', 'explain-level-unoptimized', 'explain-level-optimized', 'explain-level-executed',
        'explain-level-telemetry', 'explain-level-timed', 'explain-view-text', 'explain-view-dot',
        'explain-view-json', 'explain-download', 'explain-copy', 'explain-cancel', 'explain-highlight-syntax',
        'explain-highlight-hotspot', 'explain-property-selection'
    ].forEach(feature => { queryFeatures[feature] = false; });
    const page = queryTemplate({ metadata: { explanation: 'Existing explanation' }, queryFeatures });
    const controls = [
        ['id="save-query-toggle"', 'query-save'],
        ['id="query-options-toggle"', 'query-options'],
        ['id="explain-trigger"', 'query-explain'],
        ['id="save-private"', 'query-private-save'],
        ['id="query-timeout"', 'query-timeout'],
        ['id="infer"', 'query-inferred-statements'],
        ['id="query-cancel"', 'query-cancel'],
        ['id="compare-toggle"', 'query-compare'],
        ['id="query-diff-trigger"', 'query-diff'],
        ['id="query-compare-swap"', 'query-swap'],
        ['id="rerun-explanation"', 'query-rerun'],
        ['id="query-sidebar-toggle"', 'editor-sidebar'],
        ['id="query-insert-prefixes"', 'editor-namespaces'],
        ['id="explain-format"', 'explain-format-text'],
        ['id="explain-level"', 'explain-level-optimized'],
        ['id="copy-explanation"', 'explain-copy'],
        ['id="explanation-settings"', 'explain-highlight-syntax'],
        ['id="download-explanation"', 'explain-download'],
        ['id="explanation-highlight-syntax"', 'explain-highlight-syntax'],
        ['id="explanation-highlight-hotspot"', 'explain-highlight-hotspot'],
        ['id="explanation-properties-all"', 'explain-property-selection'],
        ['id="query-explanation"', 'explain-view-text'],
        ['id="query-explanation-dot-view"', 'explain-view-dot'],
        ['id="query-explanation-json-view"', 'explain-view-json'],
        ['id="rerun-explanation-cancel"', 'explain-cancel'],
        ['id="explain-compare-trigger"', 'query-refresh'],
        ['id="explain-compare-cancel"', 'explain-cancel']
    ];
    const missingPolicies = controls.filter(([selector]) => {
        return dynamicAttributeValue(page, selector, '\\?hidden') !== true;
    }).map(([selector, feature]) => `${selector} (${feature})`);

    assert.deepEqual(missingPolicies, [], 'disabled features must hide their matching query control');
    const staticMarkup = page.templates.map(template => template.strings.join('')).join('');
    assert.doesNotMatch(staticMarkup, /(?:id|name)="limit_query"|Rows Per Page/i,
        'legacy result-page-size policy cannot add a removed page-size field');
    assert.equal(dynamicAttributeValue(page, 'id="query-page"', 'data-editor-fullscreen-enabled'), 'false',
        'editor fullscreen policy is applied to both query editors');
    assert.equal(dynamicAttributeValue(page, 'id="query-results-fullscreen"', 'data-result-fullscreen-enabled'), 'false',
        'result fullscreen remains disabled by its independent policy');
});

test('saved query execute and edit preserve stored timeout and default legacy rows to unlimited time', () => {
    for (const [queryTimeout, expected] of [['17', '17'], [undefined, '0']]) {
        const page = savedQueryTemplate(queryTimeout);
        const staticMarkup = page.templates.map(template => template.strings.join('')).join('');
        const timeoutValues = page.bindings
            .filter(binding => binding.before.includes('name="query-timeout"'))
            .map(binding => binding.value);
        assert.deepEqual(timeoutValues, [expected, expected],
            `both Execute and Edit should preserve the ${expected}-second saved-query timeout`);
        assert.doesNotMatch(staticMarkup, /(?:id|name)="limit_query"|Rows Per Page/i,
            'saved query actions no longer render the stored result page size');
    }
});

test('saved query values remain dynamic text and attribute bindings', () => {
    const payload = `</textarea><img src=x onerror="globalThis.rdf4jXss=true">`;
    const page = savedQueryTemplate('17', {
        queryName: payload,
        queryText: payload,
        user: payload
    });
    const staticMarkup = page.templates.flatMap(template => template.strings).join('');
    const dynamicValues = page.bindings.map(binding => binding.value);

    assert.ok(!staticMarkup.includes(payload), 'saved query data must not become literal template markup');
    assert.ok(dynamicValues.filter(value => value === payload).length >= 3,
        'query name, owner, and query source should remain dynamic Lit bindings');
});

test('saved-query empty state links to Query only when that route is available', () => {
    const visible = savedQueriesEmptyTemplate([{ id: 'operations', items: [{ id: 'query', label: 'Query' }] }]);
    const hidden = savedQueriesEmptyTemplate([]);
    const isOpenQueryLink = binding => binding.before.includes('<a href=') && binding.after.includes('Open Query');

    assert.ok(visible.bindings.some(isOpenQueryLink), 'the empty state should link to an available Query route');
    assert.ok(!hidden.bindings.some(isOpenQueryLink),
        'the empty state should not link to Query when the route is absent from navigation');
});

test('query text stays a Lit text binding instead of becoming executable markup', () => {
    const dangerousQuery = '</textarea><img src=x onerror=alert(1)>';
    const page = queryTemplate({ metadata: { query: dangerousQuery } });
    const queryBinding = bindingFor(page.bindings,
        binding => binding.before.includes('<textarea id="query"') && binding.after.includes('</textarea>'),
        'query editor text');

    assert.equal(queryBinding.value, dangerousQuery);
    assert.ok(!queryBinding.before.includes(dangerousQuery));
    assert.ok(!queryBinding.after.includes(dangerousQuery));
});

function templateText(page) {
    return page.templates.map(template => template.strings.map((value, index) => value
        + (index < template.values.length && ['string', 'number'].includes(typeof template.values[index])
            ? String(template.values[index]) : '')).join('')).join('');
}

test('query settings, timeout and prefix insertion use clear names and units', () => {
    const page = queryTemplate();
    const values = page.bindings.map(binding => binding.value).filter(value => typeof value === 'string');
    assert.equal(values.includes('Query settings'), true, 'the query disclosure is labeled Query settings');
    assert.equal(values.includes('Options'), false, 'no disclosure on the query page is labeled Options');
    const text = templateText(page);
    assert.match(text, /<label[^>]*for="query-timeout"[^>]*>Timeout<\/label>/);
    assert.match(text, /seconds/);
    assert.match(text, /0 means no limit/);
    assert.doesNotMatch(text, /value="Clear"/);
    assert.match(text, /Insert prefixes/);

    const disabled = queryTemplate({ queryFeatures: { 'query-timeout': false } });
    assert.equal(dynamicAttributeValue(disabled, 'id="query-timeout-field"', '\\?hidden'), true,
        'a disabled timeout hides its whole field, label included');
});
