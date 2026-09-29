const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const scripts = process.env.WORKBENCH_SCRIPT_DIR
    || path.resolve(__dirname, '../../tools/workbench/src/main/webapp/scripts');

function queryTemplates(metadata, queryFeatures) {
    const context = vm.createContext({
        URLSearchParams,
        window: { location: { search: '' } },
        workbench: {}
    });
    const source = fs.readFileSync(path.join(scripts, 'workbenchViews.js'), 'utf8');
    vm.runInContext(source, context, { filename: 'workbenchViews.js' });
    const runtime = {
        nothing: Symbol('nothing'),
        html(strings, ...values) { return { strings: Array.from(strings), values }; },
        render() {}
    };
    const result = context.workbench.views.pageTemplate({
        viewId: 'query', vars: [], rows: [], metadata
    }, {
        basePath: '/workbench', repositoryId: 'repo-1',
        workbench: { defaults: {}, queryFeatures, queryFormats: [] }
    }, runtime);
    return collect(result);
}

function collect(value, templates = []) {
    if (Array.isArray(value)) {
        value.forEach(entry => collect(entry, templates));
    } else if (value && Array.isArray(value.strings) && Array.isArray(value.values)) {
        templates.push(value);
        value.values.forEach(entry => collect(entry, templates));
    }
    return templates;
}

function option(templates, value) {
    for (const template of templates) {
        const strings = template.strings.join('');
        const dynamicValue = strings.includes('<option value=') && template.values[0] === value;
        const marker = dynamicValue ? '<option value=' : `<option value="${value}"`;
        const annotated = template.strings.map((part, index) => part
            + (index < template.values.length ? `@@value_${index}@@` : '')).join('');
        const start = annotated.indexOf(marker);
        if (start < 0) {
            continue;
        }
        const end = annotated.indexOf('>', start);
        const attributes = annotated.slice(start, end < 0 ? annotated.length : end);
        const valueFor = name => {
            const match = attributes.match(new RegExp(`\\?${name}=@@value_(\\d+)@@`));
            return match ? template.values[Number(match[1])] : undefined;
        };
        return [value, valueFor('selected'), valueFor('hidden'), valueFor('disabled')];
    }
    return null;
}

test('query explanation selectors hide unavailable options and select an available fallback', () => {
    const templates = queryTemplates({
        'explanation-format': 'dot',
        'explanation-level': 'Optimized',
        explanation: 'Existing explanation'
    }, {
        'explain-format-dot': false,
        'explain-level-optimized': false
    });

    const dot = option(templates, 'dot');
    const text = option(templates, 'text');
    const optimized = option(templates, 'Optimized');
    const unoptimized = option(templates, 'Unoptimized');
    assert.ok(dot && text && optimized && unoptimized, 'all explanation format and level options remain represented');
    assert.equal(dot[1], false, 'a disabled stored format must not stay selected');
    assert.equal(dot[2], true, 'a disabled format option must be hidden');
    assert.equal(dot[3], true, 'a disabled format option must be unavailable');
    assert.equal(text[1], true, 'the first supported output format becomes the fallback');
    assert.equal(optimized[1], false, 'a disabled stored level must not stay selected');
    assert.equal(optimized[2], true, 'a disabled explanation level must be hidden');
    assert.equal(optimized[3], true, 'a disabled explanation level must be unavailable');
    assert.equal(unoptimized[1], true, 'the legacy preferred supported level becomes the fallback');
});
