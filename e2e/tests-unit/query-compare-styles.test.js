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

test('query editor matching tags use the Workbench selected surface', () => {
    const css = fs.readFileSync(
        path.resolve(__dirname, '..', '..', 'tools/workbench/src/main/webapp/styles/workbench-refresh.css'),
        'utf8'
    );

    const matchingTagRule = css.match(/body\.workbench-body \.yasqe \.CodeMirror-matchingtag\s*\{([^}]*)\}/);
    assert.ok(matchingTagRule, 'Expected a Workbench override for the vendor matching-tag highlight');
    assert.match(matchingTagRule[1], /background:\s*var\(--workbench-selected\)\s*!important/);
});

/** Reads a background-position x value written as a percentage or as calc(<percentage> / <number>). */
function percentageValue(value) {
    const plain = value.match(/^(-?[\d.]+)%$/);
    if (plain) {
        return Number(plain[1]);
    }
    const divided = value.match(/^calc\((-?[\d.]+)%\s*\/\s*([\d.]+)\)$/);
    assert.ok(divided, `Expected a percentage or calc(<percentage> / <number>), got ${value}`);
    return Number(divided[1]) / Number(divided[2]);
}

test('the loading bar loops without a jump: it moves linearly and starts and ends outside its box', () => {
    const css = fs.readFileSync(
        path.resolve(__dirname, '..', '..', 'tools/workbench/src/main/webapp/styles/workbench-refresh.css'),
        'utf8'
    );
    const keyframes = css.match(/@keyframes workbench-route-progress\s*\{\s*from\s*\{\s*background-position:\s*(\S+(?: \/ \S+\))?)\s+0;\s*\}\s*to\s*\{\s*background-position:\s*(\S+(?: \/ \S+\))?)\s+0;\s*\}\s*\}/);
    assert.ok(keyframes, 'Expected from/to background-position keyframes for workbench-route-progress');
    const from = percentageValue(keyframes[1]);
    const to = percentageValue(keyframes[2]);

    const rules = [...css.matchAll(/([^{}]+)\{([^{}]*animation:\s*workbench-route-progress[^{}]*)\}/g)];
    assert.equal(rules.length, 2, 'the page bar and the panel bar both run the loop');
    for (const [, selector, body] of rules) {
        assert.match(body, /animation:\s*workbench-route-progress\s+[\d.]+m?s\s+linear\s+infinite/,
            `${selector.trim()} moves at a constant speed, so the loop has no pause or jump`);
        const size = body.match(/background-size:\s*(-?[\d.]+)%\s+2px;/);
        assert.ok(size, `${selector.trim()} sizes its bar as a percentage of the box`);
        const barWidth = Number(size[1]);
        // A background-position percentage is a fraction of (box width - bar width).
        const leftEdge = position => (100 - barWidth) * position / 100;
        assert.ok(leftEdge(from) <= -barWidth + 1e-9,
            `${selector.trim()} starts with the bar wholly left of the box (left edge ${leftEdge(from)}%)`);
        assert.ok(leftEdge(to) >= 100 - 1e-9,
            `${selector.trim()} ends with the bar wholly right of the box (left edge ${leftEdge(to)}%)`);
        const resting = body.match(/background-position:\s*(\S+(?: \/ \S+\))?)\s+0;/);
        assert.ok(resting && percentageValue(resting[1]) === from,
            `${selector.trim()} rests at the first frame, so the bar does not flash before the animation starts`);
    }
});

test('the explanation times sit in the panel top padding, above the toolbar', () => {
    const css = fs.readFileSync(
        path.resolve(__dirname, '..', '..', 'tools/workbench/src/main/webapp/styles/query-explanation.css'),
        'utf8'
    );
    const rule = css.match(/\n\.query-explanation-timings\s*\{([^}]*)\}/);
    assert.ok(rule, 'Expected a .query-explanation-timings rule');
    assert.match(rule[1], /margin:\s*-14px 0 var\(--workbench-control-gap, 8px\);/,
        'the row moves 14px up into the padding under the tabs, so the panel has less empty space');
});
