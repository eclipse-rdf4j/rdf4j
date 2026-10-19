const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

test('query diff rows size to the full horizontal diff width', () => {
    const css = fs.readFileSync(
        path.resolve(__dirname, '..', '..', 'tools/workbench/src/main/webapp/styles/query-compare.css'),
        'utf8'
    );

    const diffRowRuleMatch = css.match(/\.query-diff-row\s*\{([\s\S]*?)\n\}/);
    assert.ok(diffRowRuleMatch, 'Expected a .query-diff-row CSS rule');
    assert.match(diffRowRuleMatch[1], /width:\s*max-content;/);
    assert.match(diffRowRuleMatch[1], /min-width:\s*100%;/);
});
