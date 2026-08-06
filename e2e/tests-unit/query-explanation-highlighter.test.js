const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const { createScriptHarness } = require('./script-harness.js');

function createHighlighterHarness() {
    const harness = createScriptHarness();
    harness.runScript('tools/workbench/src/main/webapp/scripts/queryExplanationHighlighter.js');
    return {
        harness,
        highlighter: harness.context.workbench.queryExplanationHighlighter
    };
}

test('formats plan JSON as the legacy text tree', () => {
    const { highlighter } = createHighlighterHarness();
    const result = highlighter.format({
        type: 'StatementPattern',
        costEstimate: 1250,
        resultSizeEstimate: 2.5,
        plans: [
            { type: 'Var (name=s)' },
            { type: 'Var (name=p)' },
            { type: 'Var (name=o)' }
        ]
    });

    assert.equal(
        result.text,
        'StatementPattern (costEstimate=1.3K, resultSizeEstimate=2.50)\n'
            + '   s: Var (name=s)\n'
            + '   p: Var (name=p)\n'
            + '   o: Var (name=o)\n'
    );
});

test('renders HTML-like values as inert highlighted text', () => {
    const { harness, highlighter } = createHighlighterHarness();
    const result = highlighter.render({
        type: 'Var (name=s, value=<script>alert(1)</script>)'
    }, {
        level: 'Optimized',
        mode: 'syntax'
    });
    const target = harness.document.createElement('pre');
    target.appendChild(result.fragment);

    assert.equal(target.textContent, 'Var (name=s, value=<script>alert(1)</script>)\n');
    assert.equal(target.getElementsByTagName('script').length, 0);
    assert.equal(target.getElementsByTagName('span').length > 0, true);
    assert.equal(
        target.getElementsByTagName('span').some((element) =>
            element.classList.contains('query-explanation-token--value')),
        true
    );
});

test('selects adaptive hotspot metrics and accepts a shared maximum', () => {
    const { highlighter } = createHighlighterHarness();
    const plan = {
        type: 'Join',
        costEstimate: 4,
        resultSizeActual: 8,
        selfTimeActual: 2,
        plans: [
            { type: 'StatementPattern', costEstimate: 10, resultSizeActual: 20, selfTimeActual: 5 }
        ]
    };

    assert.deepEqual(
        JSON.parse(JSON.stringify(highlighter.getHotspot(plan, 'Optimized'))),
        { metric: 'costEstimate', label: 'Cost estimate', maximum: 10 }
    );
    assert.deepEqual(
        JSON.parse(JSON.stringify(highlighter.getHotspot(plan, 'Telemetry'))),
        { metric: 'resultSizeActual', label: 'Actual rows', maximum: 20 }
    );
    assert.deepEqual(
        JSON.parse(JSON.stringify(highlighter.getHotspot(plan, 'Timed'))),
        { metric: 'selfTimeActual', label: 'Self time', maximum: 5 }
    );
    assert.equal(highlighter.getHotspot(plan, 'Unoptimized'), null);

    const result = highlighter.render(plan, {
        level: 'Optimized',
        mode: 'hotspot',
        sharedMaximum: 20
    });
    assert.equal(result.maximum, 10);
    assert.equal(result.sharedMaximum, 20);
});

test('matches the Java formatter for every shared plan fixture', () => {
    const { highlighter } = createHighlighterHarness();
    const fixtureDirectory = path.resolve(
        __dirname,
        '../../tools/workbench/src/test/resources/query-explanation-highlighter'
    );
    const fixtureFiles = fs.readdirSync(fixtureDirectory)
        .filter((fileName) => fileName.endsWith('.json'))
        .sort();

    for (const fixtureFile of fixtureFiles) {
        const fixtureName = fixtureFile.replace(/\.json$/, '');
        const level = fixtureName.substring(0, fixtureName.indexOf('-'));
        const plan = JSON.parse(fs.readFileSync(path.join(fixtureDirectory, fixtureFile), 'utf8'));
        const expected = fs.readFileSync(path.join(fixtureDirectory, fixtureName + '.txt'), 'utf8');

        assert.equal(highlighter.format(plan, level).text, expected, fixtureName);
    }
});
