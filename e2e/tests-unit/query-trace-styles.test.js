const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

test('trace result highlight paints binding hints green', () => {
    const css = fs.readFileSync(
        path.resolve(__dirname, '..', '..', 'tools/workbench/src/main/webapp/styles/query-explanation.css'),
        'utf8'
    );

    const resultBindingRuleMatch = css.match(/\.query-trace-query--result \.query-trace-query__binding\s*\{([\s\S]*?)\n\}/);
    assert.ok(resultBindingRuleMatch, 'Expected a result-state binding hint rule');
    assert.match(resultBindingRuleMatch[1], /background:\s*rgba\(\s*46,\s*125,\s*50,\s*0\.[0-9]+\s*\);/);
    assert.match(resultBindingRuleMatch[1], /color:\s*#f[0-9a-f]{5};/i);

    const resultArrowRuleMatch = css.match(/\.query-trace-query--result \.query-trace-query__binding::after\s*\{([\s\S]*?)\n\}/);
    assert.ok(resultArrowRuleMatch, 'Expected a result-state binding hint arrow rule');
    assert.match(resultArrowRuleMatch[1], /border-color:\s*rgba\(\s*46,\s*125,\s*50,\s*0\.[0-9]+\s*\)\s+transparent\s+transparent;/);
});
