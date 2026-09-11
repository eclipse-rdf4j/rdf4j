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

test('discovers and filters individual plan properties without changing the tree', () => {
    const { harness, highlighter } = createHighlighterHarness();
    const plan = {
        type: 'Join',
        newScope: true,
        algorithm: 'hash join',
        costEstimate: 10,
        resultSizeEstimate: 4,
        stringMetricsActual: {
            bindingState: 'bound'
        }
    };

    assert.deepEqual(
        Array.from(highlighter.getProperties(plan, 'Optimized')),
        ['newScope', 'algorithm', 'costEstimate', 'resultSizeEstimate', 'bindingState']
    );

    const rendered = highlighter.render(plan, {
        level: 'Optimized',
        mode: 'syntax',
        hiddenProperties: ['algorithm', 'costEstimate', 'bindingState']
    });
    const target = harness.document.createElement('pre');
    target.appendChild(rendered.fragment);

    assert.equal(rendered.text, 'Join (new scope) (resultSizeEstimate=4.00)\n');
    assert.equal(target.textContent, rendered.text);
});

test('matches Java decimal rounding and scientific notation', () => {
    const { highlighter } = createHighlighterHarness();
    const result = highlighter.format({
        type: 'Join',
        costEstimate: 2.675,
        plans: [
            { type: 'StatementPattern', costEstimate: 10000000000000 }
        ]
    });

    assert.equal(
        result.text,
        'Join (costEstimate=2.68)\n'
            + '   StatementPattern (costEstimate=1.0E7M)\n'
    );
});

test('uses the server line separator for formatted and rendered text', () => {
    const { harness, highlighter } = createHighlighterHarness();
    const plan = {
        type: 'StatementPattern',
        plans: [{ type: 'Var (name=s)' }]
    };

    assert.equal(
        highlighter.format(plan, 'Optimized', '\r\n').text,
        'StatementPattern\r\n   s: Var (name=s)\r\n'
    );

    const rendered = highlighter.render(plan, {
        level: 'Optimized',
        mode: 'syntax',
        lineSeparator: '\r\n'
    });
    const target = harness.document.createElement('pre');
    target.appendChild(rendered.fragment);

    assert.equal(rendered.text, 'StatementPattern\r\n   s: Var (name=s)\r\n');
    assert.equal(target.textContent, rendered.text);
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

test('compacts default and repository IRIs without changing the legacy plaintext', () => {
    const { harness, highlighter } = createHighlighterHarness();
    const rdfType = 'http://www.w3.org/1999/02/22-rdf-syntax-ns#type';
    const exampleType = 'http://example#Type';
    const result = highlighter.render({
        type: 'StatementPattern',
        plans: [
            { type: 'Var (name=a)' },
            { type: `Var (name=p, value=${rdfType}, anonymous)` },
            { type: `Var (name=o, value=${exampleType}, anonymous)` }
        ]
    }, {
        level: 'Optimized',
        mode: 'syntax',
        namespaces: { ex: 'http://example#' }
    });
    const target = harness.document.createElement('pre');
    target.appendChild(result.fragment);

    assert.equal(
        target.textContent,
        'StatementPattern\n'
            + '   s: Var (name=a)\n'
            + '   p: Var (name=p, value=rdf:type, anonymous)\n'
            + '   o: Var (name=o, value=ex:Type, anonymous)\n'
    );
    assert.equal(
        result.text,
        'StatementPattern\n'
            + '   s: Var (name=a)\n'
            + `   p: Var (name=p, value=${rdfType}, anonymous)\n`
            + `   o: Var (name=o, value=${exampleType}, anonymous)\n`
    );
    assert.deepEqual(
        target.getElementsByTagName('span')
            .filter((element) => element.classList.contains('query-explanation-token--value'))
            .map((element) => [element.textContent, element.getAttribute('title')]),
        [
            ['rdf:type', rdfType],
            ['ex:Type', exampleType]
        ]
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
