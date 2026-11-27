const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const { createQueryBrowserHarness } = require('./query-browser-harness.js');
const { createScriptHarness } = require('./script-harness.js');

function createHighlighterHarness() {
    const harness = createScriptHarness();
    harness.runScript('scripts/queryExplanationHighlighter.js');
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

test('preserves native-plan metric ordering and strategy-decision text', () => {
    const { highlighter } = createHighlighterHarness();
    const physicalPlan = 'NativeRows(arg=MultiJoin(order=[Pattern(s=?s)]))';
    const plan = {
        type: 'Projection',
        physicalPlanPrelude: 'LMDB native physical plan (planned)\n  kind: row\n  plan:\n    ' + physicalPlan,
        stringMetricsPlanned: {
            nativePhysicalPlan: physicalPlan,
            plannedExecutionKind: 'row',
            plannedExecutionEngine: 'lmdb-native'
        },
        strategyDecisions: [{
            decisionPoint: 'Decision 1: row/join dispatch',
            capturedAtMillis: 1234,
            mode: 'runtime dependent',
            wouldSelect: 'irKernelParallel',
            fallback: 'genericFallback',
            reason: 'Execution-time binding and admission must still succeed',
            candidates: [{
                strategy: 'irKernelParallel',
                priority: 1,
                canAttempt: true,
                decision: 'Would select now',
                declineReason: 'Execution-time binding and admission must still succeed'
            }]
        }]
    };
    const result = highlighter.format(plan);

    assert.equal(
        result.text,
        'LMDB native physical plan (planned)\n'
            + '  kind: row\n'
            + '  plan:\n'
            + '    ' + physicalPlan + '\n'
            + '\n'
            + 'Query explanation\n'
            + 'Projection (plannedExecutionEngine=lmdb-native, plannedExecutionKind=row, nativePhysicalPlan='
            + physicalPlan + ')\n'
            + 'Strategies — Decision 1: row/join dispatch\n'
            + 'Would select now: irKernelParallel (runtime dependent); fallback: genericFallback\n'
            + 'Execution-time binding and admission must still succeed\n'
            + 'Priority | Strategy | Can attempt? | Decision | Reason\n'
            + '1 | irKernelParallel | Yes | Would select now | Execution-time binding and admission must still succeed\n'
    );
    assert.equal(
        highlighter.format(plan, 'Optimized', '\r\n').text,
        result.text.replace(/\n/g, '\r\n')
    );
});

test('renders nested strategy decisions as accessible tables with consistent roles', () => {
    const { harness, highlighter } = createHighlighterHarness();
    const plan = { type: 'Join', plans: [{ type: 'Slice (limit=100)', plans: [{ type: 'Projection' }],
        strategyDecisions: [{
            decisionPoint: 'Decision 1: row/join dispatch', mode: 'normal', wouldSelect: 'nestedLoop',
            fallback: 'packedFtree(serial)', reason: 'Lowest expected price (134414 ns)',
            candidates: [
                { strategy: 'nestedLoop', priority: 1, canAttempt: true, decision: 'Would select now' },
                { strategy: 'packedFtree(serial)', priority: 2, canAttempt: true, decision: 'Eligible',
                    condition: 'Priced 707..1372044 ns (expected 158323) | sample(range)\nsecond line' },
                { strategy: 'irKernel', decision: 'Runtime dependent' },
                { strategy: 'factorizedRows', canAttempt: false, decision: 'Declined',
                    condition: 'unused', declineReason: 'not applicable' }
            ]
        }, {
            decisionPoint: 'Decision 2: custom dispatch', mode: 'no dispatch', reason: 'No input',
            candidates: []
        }]
    }, { type: 'SingletonSet' }] };
    const target = harness.document.createElement('div');
    const result = highlighter.render(plan, { level: 'Optimized', mode: 'syntax' });
    target.appendChild(result.fragment);
    const tables = target.querySelectorAll('table');
    assert.equal(tables.length, 2);
    const tokens = kind => Array.from(target.querySelectorAll('.query-explanation-token--' + kind),
        token => token.textContent);
    assert.deepEqual(tokens('strategy'),
        ['nestedLoop', 'packedFtree(serial)', 'nestedLoop', 'packedFtree(serial)', 'irKernel', 'factorizedRows']);
    assert.ok(tokens('node-type').includes('Strategies — Decision 1: row/join dispatch'));
    assert.ok(tokens('node-type').includes('Strategies — Decision 2: custom dispatch'));
    for (const column of ['Priority', 'Strategy', 'Can attempt?', 'Decision', 'Reason']) {
        assert.ok(tokens('metric-name').includes(column), column);
    }
    assert.deepEqual(tokens('status-positive'), ['Yes', 'Would select now', 'Yes', 'Eligible']);
    assert.deepEqual(tokens('status-negative'), ['No', 'Declined']);
    assert.ok(tokens('status-neutral').includes('Runtime dependent'));
    assert.ok(tokens('status-neutral').includes('No strategy required'));
    assert.ok(tokens('metric-value').includes('134414'));
    assert.ok(tokens('metric-value').includes('707'));
    assert.ok(tokens('metric-value').includes('1372044'));
    assert.deepEqual(tokens('metric-unit'), ['ns', 'ns']);
    assert.ok(!tokens('node-type').some(type => /^(price|ns|sample)$/.test(type)), tokens('node-type'));

    assert.equal(tables[0].getAttribute('aria-label'), 'Strategies — Decision 1: row/join dispatch');
    assert.deepEqual(Array.from(tables[0].querySelector('thead').querySelectorAll('th'), header =>
        [header.textContent, header.getAttribute('scope')]),
    ['Priority', 'Strategy', 'Can attempt?', 'Decision', 'Reason'].map(label => [label, 'col']));
    assert.deepEqual(Array.from(tables[0].querySelector('tbody').querySelectorAll('tr'), row =>
        Array.from(row.querySelectorAll('td'), cell => cell.textContent)), [
        ['1', 'nestedLoop', 'Yes', 'Would select now', ''],
        ['2', 'packedFtree(serial)', 'Yes', 'Eligible', 'Priced 707..1372044 ns (expected 158323) | sample(range)\nsecond line'],
        ['—', 'irKernel', 'Runtime dependent', 'Runtime dependent', ''],
        ['—', 'factorizedRows', 'No', 'Declined', 'not applicable']
    ]);
    assert.equal(tables[1].querySelector('tbody').querySelectorAll('tr').length, 0);
    assert.ok(target.textContent.indexOf('Lowest expected price') < target.textContent.indexOf('Priority'));
    assert.ok(target.textContent.indexOf('not applicable') < target.textContent.indexOf('Strategies — Decision 2'));
    assert.ok(target.textContent.indexOf('No input') < target.textContent.indexOf('Projection'));
    assert.ok(result.text.includes('║  2 | packedFtree(serial) | Yes | Eligible | '
        + 'Priced 707..1372044 ns (expected 158323) | sample(range)\n║  second line\n'));
    assert.equal(result.text, highlighter.format(plan, 'Optimized').text);
    assert.ok(!target.textContent.includes('Priority | Strategy'));
});

test('uses the same roles for logical fields, native fields, projection bindings, and profile values', () => {
    const { harness, highlighter } = createHighlighterHarness();
    const physical = 'NativeRows(arg=Pattern(s=?s, p=const(<https://example.test/p> (id=5))))';
    const plan = {
        type: 'Slice (limit=100)', logicalPath: '0', totalTimeActual: 12,
        physicalPlanPrelude: 'LMDB native physical plan (executed)\n  kind: row\n  strategy: nestedLoop\n'
            + '  status: COMPLETED\n  reason: Lowest expected price (134414 ns)\n  plan: ' + physical,
        stringMetricsPlanned: { plannedExecutionKind: 'row', nativePhysicalPlan: physical },
        executionProfile: { status: 'COMPLETED', elapsedNanos: 12000000,
            diagnostics: [{ severity: 'WARN', message: 'No sample(range) (12 ns)' }],
            stages: [{ id: '0', operator: 'Scan', logicalPath: '0', strategy: 'nestedLoop', status: 'COMPLETED',
                metrics: { cpu: { count: 12, unit: 'ns', origin: 'measured', state: 'complete', scope: 'self' } } }],
            attempts: [{ stageId: '0', strategy: 'nestedLoop', status: 'COMPLETED', reason: 'Used price (12 ns)' }] },
        plans: [{ type: 'ProjectionElem "dataset" AS "resource"' }, { type: 'Var (name=dataset)' }]
    };
    const target = harness.document.createElement('pre');
    const result = highlighter.render(plan, {
        level: 'Telemetry', mode: 'syntax', namespaces: { ex: 'https://example.test/' }
    });
    target.appendChild(result.fragment);
    const tokens = kind => Array.from(target.querySelectorAll('.query-explanation-token--' + kind),
        token => token.textContent.trim());
    assert.deepEqual(tokens('strategy'), ['nestedLoop', 'nestedLoop', 'nestedLoop']);
    assert.equal(tokens('status-positive').filter(text => text === 'COMPLETED').length, 4);
    assert.ok(tokens('metric-name').includes('limit'));
    assert.ok(tokens('metric-value').includes('100'));
    assert.equal(tokens('metric-value').filter(text => text === 'row').length, 2);
    assert.equal(tokens('metric-unit').filter(text => text === 'ns').length, 4);
    assert.equal(tokens('metric-unit').filter(text => text === 'ms').length, 2);
    assert.ok(!tokens('node-type').some(type => /^(price|ns|sample)$/.test(type)), tokens('node-type'));
    assert.ok(tokens('node-type').includes('const'));
    assert.ok(tokens('node-type').includes('NativeRows'));
    assert.deepEqual(tokens('variable').filter(text => !text.startsWith('?')), ['dataset', 'resource', 'dataset']);
    assert.ok(tokens('value-iri').includes('ex:p'));
    assert.equal(target.querySelector('.query-explanation-token--value-iri').getAttribute('title'),
        '<https://example.test/p>');
    assert.equal(target.textContent, result.text.replaceAll('<https://example.test/p>', 'ex:p'));
});

test('renders wire-format operator and RDF term structure with legacy text retained for export', () => {
    const { harness, highlighter } = createHighlighterHarness();
    const namespace = 'https://example.test/';
    const iri = { kind: 'iri', value: namespace + 'dataset' };
    const literal = { kind: 'literal', value: 'Read <https://example.test/inside> "as written"', language: 'en' };
    const pattern = { kind: 'operator', name: 'Pattern', arguments: {
        s: { kind: 'slot', slot: 0, name: 'dataset' },
        p: { kind: 'constant', id: '9007199254740993', term: iri },
        o: { kind: 'constant', slot: 1, name: 'entryValue', id: '42', term: iri },
        c: { kind: 'wildcard' }
    } };
    const native = { name: 'NativeRows', arguments: { arg: { name: 'MultiJoin', arguments: { order: [pattern] } } } };
    const bound = { bound: true, id: '9007199254740993', term: iri };
    const plan = {
        type: 'Slice (limit=999)', operator: 'Slice', arguments: { limit: 10, offset: 2 }, logicalPath: '0',
        physicalPlanPrelude: 'Legacy physical prelude\n  ignoredForDisplay: 999',
        physicalPlans: [
            { engine: 'lmdb-native', state: 'planned', kind: 'row', plan: native },
            { engine: 'lmdb-native', state: 'executed', omittedInvocations: 99, invocations: [{
                index: 0, status: 'COMPLETED', strategy: 'nestedLoop', opens: 1, actualOrder: [pattern],
                entryPlan: { kind: 'operator', name: 'Empty' },
                initialBindings: { dataset: bound },
                adjacencyAccess: { used: true, attempts: [{
                    source: 'ADJACENCY', outcome: 'AVAILABLE', where: pattern,
                    bindings: { subject: bound, predicate: { bound: false } }
                }] }
            }] }
        ],
        plannedPhysicalPlan: { engine: 'lmdb-native', state: 'planned', kind: 'row', plan: native },
        stringMetricsPlanned: { nativePhysicalPlan: 'LegacyNative()' },
        stringMetricsActual: { nativeDecisionOutcome: 'Legacy outcome' },
        actualAttributes: { nativeDecisionOutcome: { winner: 'nestedLoop', reason: 'Measured 11 ns' } },
        executionProfile: {
            status: 'COMPLETED', elapsedNanos: 12, properties: {
                inputAlgebra: 'Legacy input algebra', consumptionMode: 'values'
            },
            inputPlan: { type: 'LegacyInput', operator: 'Projection',
                plans: [{ type: 'ProjectionElem "wrong"', operator: 'ProjectionElem',
                    arguments: { name: 'dataset', alias: 'resource' } }] },
            attributes: { includeInferred: true, bindings: { dataset: iri }, labels: { name: 'x' } },
            stages: [{ id: 'scan', operator: 'Statement access', logicalPath: '0',
                status: 'COMPLETE', strategy: 'nestedLoop', properties: { subject: 'Legacy subject', route: 'index' },
                attributes: { subject: bound, actualOrder: [pattern] } }]
        },
        plans: [{ type: 'ValueConstant (value="legacy")', operator: 'ValueConstant', arguments: { value: literal } }]
    };
    const target = harness.document.createElement('div');
    const result = highlighter.render(plan, { level: 'Telemetry', mode: 'syntax', namespaces: { ex: namespace } });
    target.appendChild(result.fragment);
    const tokens = kind => Array.from(target.querySelectorAll('.query-explanation-token--' + kind),
        token => token.textContent.trim());

    assert.ok(tokens('node-type').includes('LMDB native physical plan (planned)'));
    assert.ok(tokens('node-type').includes('LMDB native physical plan (executed)'));
    assert.equal(tokens('node-type').filter(text => text === 'NativeRows').length, 2);
    assert.ok(tokens('node-type').filter(text => text === 'Pattern').length >= 4);
    assert.ok(tokens('node-type').includes('Empty'));
    assert.ok(target.textContent.includes('name: x'));
    assert.doesNotMatch(target.textContent, /x\(\)/);
    assert.ok(tokens('metric-name').includes('limit'));
    assert.ok(tokens('metric-name').includes('offset'));
    assert.ok(tokens('metric-value').includes('10'));
    assert.ok(tokens('metric-value').includes('2'));
    assert.ok(tokens('metric-value').includes('9007199254740993'));
    assert.ok(tokens('metric-name').includes('omittedInvocations'));
    assert.ok(tokens('metric-value').includes('99'));
    assert.ok(tokens('metric-name').includes('initialBindings'));
    assert.ok(tokens('metric-name').includes('adjacencyAccess'));
    assert.ok(tokens('metric-name').includes('route'));
    assert.ok(tokens('metric-name').includes('nativeDecisionOutcome'));
    assert.ok(tokens('metric-name').includes('winner'));
    assert.ok(tokens('metric-value').includes('11'));
    assert.ok(tokens('metric-name').includes('includeInferred'));
    assert.ok(tokens('metric-name').includes('bindings'));
    assert.ok(tokens('strategy').includes('nestedLoop'));
    assert.ok(tokens('binding-bound').includes('BOUND'));
    assert.ok(tokens('binding-unbound').includes('UNBOUND'));
    assert.ok(tokens('variable').includes('?dataset#0'));
    assert.ok(tokens('variable').includes('?entryValue#1'));
    assert.match(target.textContent, /o=\?entryValue#1=const\(ex:dataset \(id=42\)\)/);
    assert.ok(tokens('variable').includes('resource'));
    assert.ok(tokens('value-iri').includes('ex:dataset'));
    assert.ok(tokens('value-literal').includes('"Read <https://example.test/inside> \\"as written\\""@en'));
    assert.doesNotMatch(target.textContent, /Legacy|999|wrong|ex:inside/);
    assert.match(result.text, /Legacy physical prelude/);
    assert.match(result.text, /Slice \(limit=999\)/);
    assert.match(result.text, /LegacyNative\(\)/);
    assert.match(result.text, /Legacy outcome/);
    assert.match(result.text, /Legacy input algebra/);
    assert.match(result.text, /Legacy subject/);
    assert.equal(result.text, highlighter.format(plan, 'Telemetry').text);
});

test('parses exact long values in nested typed explanation fields', () => {
    const parserHarness = createQueryBrowserHarness();
    const parsePlanJson = parserHarness.context.workbench.query.testing.parsePlanJson;
    const parsed = parsePlanJson(`{
        "type": "Slice (limit=9007199254740993)", "operator": "Slice",
        "arguments": {"limit": 9007199254740993, "offset": 2},
        "actualAttributes": {"nativeDecisionOutcome": {"expectedNanos": 9007199254740995}},
        "plannedPhysicalPlan": {"name": "NativeRows", "arguments": {"cost": 0.25, "id": "7"}},
        "physicalPlans": [{"invocations": [{"count": 9223372036854775807}]}],
        "executionProfile": {
            "inputPlan": {"operator": "Slice", "arguments": {"limit": 9223372036854775807}},
            "stages": [{"attributes": {"measurement": {"count": 9007199254740994, "rowFraction": 0.75}}}]
        }
    }`);
    assert.equal(parsed.arguments.limit, '9007199254740993');
    assert.equal(parsed.arguments.offset, 2);
    assert.equal(parsed.actualAttributes.nativeDecisionOutcome.expectedNanos, '9007199254740995');
    assert.equal(parsed.plannedPhysicalPlan.arguments.cost, 0.25);
    assert.equal(parsed.plannedPhysicalPlan.arguments.id, '7');
    assert.equal(parsed.physicalPlans[0].invocations[0].count, '9223372036854775807');
    assert.equal(parsed.executionProfile.inputPlan.arguments.limit, '9223372036854775807');
    assert.equal(parsed.executionProfile.stages[0].attributes.measurement.count, '9007199254740994');
    assert.equal(parsed.executionProfile.stages[0].attributes.measurement.rowFraction, 0.75);
});

test('displays typed execution summary fields at Executed and Timed without changing legacy export', () => {
    const { harness, highlighter } = createHighlighterHarness();
    const plan = {
        type: 'Slice (limit=10)', operator: 'Slice', arguments: { limit: 10 },
        actualAttributes: { nativeExecutionPath: { paths: ['nestedLoop'], truncated: false } },
        stringMetricsActual: { nativeExecutionPath: 'legacy path' }
    };
    for (const level of ['Unoptimized', 'Optimized', 'Executed', 'Timed', 'Telemetry']) {
        const target = harness.document.createElement('div');
        const result = highlighter.render(plan, { level, mode: 'syntax' });
        target.appendChild(result.fragment);
        const displaysSummary = ['Executed', 'Timed', 'Telemetry'].includes(level);
        assert.equal(target.textContent.includes('nativeExecutionPath={'), displaysSummary, level);
        assert.equal(target.textContent.includes('nestedLoop'), displaysSummary, level);
        assert.equal(result.text.includes('nativeExecutionPath=legacy path'), level === 'Telemetry', level);
        assert.equal(highlighter.getProperties(plan, level).includes('nativeExecutionPath'), displaysSummary, level);
    }
});

test('puts the timeout warning before the physical plan', () => {
    const { highlighter } = createHighlighterHarness();
    const warning = 'Timed out while retrieving explanation! Explanation may be incomplete!';
    const result = highlighter.format({
        timedOut: true,
        type: 'Projection',
        physicalPlanPrelude: 'Native plan'
    }, 'Timed');
    assert.equal(result.text, warning + '\n'
        + 'You can change the timeout by setting .setMaxExecutionTime(...) on your query.\n\n'
        + 'Native plan\n\nQuery explanation\nProjection\n');
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

test('matches Java saturated rounding for very large doubles', () => {
    const { highlighter } = createHighlighterHarness();
    const result = highlighter.format({
        type: 'Join',
        costEstimate: 1e30,
        totalTimeActual: 1e30
    });

    assert.equal(
        result.text,
        'Join (costEstimate=9.223372036854776E17M, totalTimeActual=9.223372036854776E17s)\n'
    );
});

test('splits multiline plan details before applying ordinary and binary prefixes', () => {
    const { highlighter } = createHighlighterHarness();
    const lineSeparator = '\r\n';
    const plan = {
        type: 'Join',
        algorithm: `hash${lineSeparator}join`,
        costEstimate: 12.5,
        plans: [
            {
                type: 'StatementPattern',
                stringMetricsActual: {
                    indexName: `spoc${lineSeparator}posc`
                },
                plans: [
                    { type: `Var (name=s, value=first${lineSeparator}second)` }
                ]
            },
            {
                type: `Filter${lineSeparator}${lineSeparator}continued${lineSeparator}`,
                plans: [
                    { type: 'StatementPattern', resultSizeEstimate: 2000 }
                ]
            }
        ]
    };

    assert.equal(
        highlighter.format(plan, 'Optimized', lineSeparator).text,
        `Join (hash${lineSeparator}join) (costEstimate=13)${lineSeparator}`
            + `╠══ StatementPattern (indexName=spoc [left]${lineSeparator}`
            + `║  posc)${lineSeparator}`
            + `║     s: Var (name=s, value=first${lineSeparator}`
            + `║     s: second)${lineSeparator}`
            + `╚══ Filter [right]${lineSeparator}`
            + `   ${lineSeparator}`
            + `   continued${lineSeparator}`
            + `   ${lineSeparator}`
            + `      StatementPattern (resultSizeEstimate=2.0K)${lineSeparator}`
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

test('retains value semantics while marking rendered IRIs and literal forms', () => {
    const { harness, highlighter } = createHighlighterHarness();
    const namespaces = {
        ex: 'http://example.org/',
        xsd: 'http://www.w3.org/2001/XMLSchema#'
    };

    const renderValue = (value) => {
        const rendered = highlighter.render({
            type: `Var (name=o, value=${value})`
        }, {
            level: 'Optimized',
            mode: 'syntax',
            namespaces
        });
        const target = harness.document.createElement('pre');
        target.appendChild(rendered.fragment);
        return target.getElementsByTagName('span').find((element) =>
            element.classList.contains('query-explanation-token--value'));
    };

    for (const iri of ['<http://example.org/Thing>', 'http://example.org/Thing']) {
        const token = renderValue(iri);
        assert.equal(token.textContent, 'ex:Thing');
        assert.equal(token.getAttribute('title'), iri);
        assert.equal(token.classList.contains('query-explanation-token--value-iri'), true);
    }

    for (const literal of [
        '"Example"',
        '"5"^^<http://www.w3.org/2001/XMLSchema#integer>',
        '"bonjour"@fr'
    ]) {
        const token = renderValue(literal);
        assert.equal(token.textContent, literal);
        assert.equal(token.classList.contains('query-explanation-token--value-literal'), true);
    }
});

test('compacts namespaces whose prefixes collide with object properties', () => {
    const { harness, highlighter } = createHighlighterHarness();
    const namespace = 'http://example.org/';

    for (const [prefix, expectedPrefix] of [
        ['hasOwnProperty', 'hasOwnProperty'],
        ['hasOwnProperty:', 'hasOwnProperty'],
        ['__proto__', '__proto__'],
        ['__proto__:', '__proto__'],
        ['constructor', 'constructor'],
        ['constructor:', 'constructor']
    ]) {
        const namespaces = JSON.parse(`{"${prefix}":"${namespace}"}`);
        const result = highlighter.render({
            type: 'Var (name=o, value=http://example.org/Thing)'
        }, {
            level: 'Optimized',
            mode: 'syntax',
            namespaces
        });
        const target = harness.document.createElement('pre');
        target.appendChild(result.fragment);

        assert.equal(
            target.textContent,
            `Var (name=o, value=${expectedPrefix}:Thing)\n`,
            prefix
        );
    }
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

test('hotspot highlighting uses the blue palette at its minimum and maximum intensities', () => {
    const { highlighter } = createHighlighterHarness();
    const result = highlighter.render({
        type: 'Join',
        costEstimate: 0,
        plans: [{ type: 'StatementPattern', costEstimate: 1 }]
    }, {
        level: 'Optimized',
        mode: 'hotspot'
    });
    const hotspots = result.fragment.children.filter(element => element.tagName === 'SPAN'
        && element.classList.contains('query-explanation-line--hotspot'));

    assert.equal(hotspots[0].style.backgroundColor, 'rgb(222, 243, 251)');
    assert.equal(hotspots[1].style.backgroundColor, 'rgb(115, 196, 226)');
});

test('keeps large actual-row values exact for hotspot maxima and shared scales', () => {
    const { highlighter } = createHighlighterHarness();
    const plan = {
        type: 'Join',
        resultSizeActual: '9007199254799999',
        plans: [{ type: 'StatementPattern', resultSizeActual: '9007199254799998' }]
    };

    assert.deepEqual(
        JSON.parse(JSON.stringify(highlighter.getHotspot(plan, 'Telemetry'))),
        { metric: 'resultSizeActual', label: 'Actual rows', maximum: '9007199254799999' }
    );

    const result = highlighter.render(plan, {
        level: 'Telemetry',
        mode: 'hotspot',
        sharedMaximum: '9007199254800001'
    });
    assert.equal(result.maximum, '9007199254799999');
    assert.equal(result.sharedMaximum, '9007199254800001');
});

test('matches Java long overflow and missing counter defaults in telemetry', () => {
    const { highlighter } = createHighlighterHarness();
    const overflowingService = highlighter.format({
        type: 'Service',
        longMetricsActual: {
            remoteRequestCountActual: '9223372036854775807',
            remoteAskRequestCountActual: '9223372036854775807',
            remoteSelectRequestCountActual: 1
        }
    }, 'Telemetry');
    assert.equal(
        overflowingService.text,
        'Service (remoteRequestCountActual=9.2233720368547E12M, '
            + 'remoteAskRequestCountActual=9.2233720368547E12M, '
            + 'remoteSelectRequestCountActual=1)\n'
    );

    const missingTypedCounter = highlighter.format({
        type: 'Service',
        longMetricsActual: { remoteRequestCountActual: 5 }
    }, 'Telemetry');
    assert.equal(missingTypedCounter.text,
        'Service (remoteRequestCountActual=5, remoteRetryCountActual=5)\n');
});

test('wraps a negative LongMin overflow before formatting it', () => {
    const { highlighter } = createHighlighterHarness();
    const result = highlighter.format({
        type: 'Join',
        resultSizeActual: '-9223372036854775809'
    }, 'Telemetry');

    assert.equal(result.text, 'Join (resultSizeActual=9.2233720368547E12M)\n');
});

test('renders the physical plan prelude ahead of the query explanation', () => {
    const { highlighter } = createHighlighterHarness();
    const plan = {
        physicalPlanPrelude: 'LMDB native physical plan (executed)\n  invocation[0]:\n    status: COMPLETED',
        type: 'StatementPattern',
        plans: [{ type: 'Var (name=s)' }]
    };

    assert.equal(
        highlighter.format(plan).text,
        'LMDB native physical plan (executed)\n'
            + '  invocation[0]:\n'
            + '    status: COMPLETED\n'
            + '\n'
            + 'Query explanation\n'
            + 'StatementPattern\n'
            + '   s: Var (name=s)\n'
    );

    const lineSeparator = '\r\n';
    assert.equal(
        highlighter.format(plan, 'Optimized', lineSeparator).text,
        `LMDB native physical plan (executed)${lineSeparator}`
            + `  invocation[0]:${lineSeparator}`
            + `    status: COMPLETED${lineSeparator}`
            + `${lineSeparator}`
            + `Query explanation${lineSeparator}`
            + `StatementPattern${lineSeparator}`
            + `   s: Var (name=s)${lineSeparator}`
    );
});

test('highlights entire explanations for every level with or without native and adjacency details', () => {
    const { harness, highlighter } = createHighlighterHarness();
    for (const level of ['Unoptimized', 'Optimized', 'Executed', 'Telemetry', 'Timed']) {
        for (const native of [false, true]) {
            for (const adjacency of [false, true]) {
                const context = `${level}, native=${native}, adjacency=${adjacency}`;
                const plan = { type: 'StatementPattern', plans: [{ type: 'Var (name=s)' }] };
                const fields = ['runtime: ' + (native ? 'NATIVE' : 'GENERIC')];
                if (native) {
                    fields.push('actualOrder:',
                        '  0: Pattern(s=?s#0, p=const(<https://example.test/p> (id=5)), o="id=7 <https://example.test/literal>")');
                }
                if (adjacency) {
                    fields.push('adjacencyAccess:', '  source: IN_MEMORY_ADJACENCY', '  rowsScannedActual: 17',
                        '  binding: BOUND[0x0F]', '  other: UNBOUND');
                } else {
                    fields.push('lookup: LMDB', '  rowsScannedActual: 23');
                }
                plan.physicalPlanPrelude = 'Physical plan\n' + fields.join('\n');
                plan.strategyDecisions = [{ decisionPoint: 'row dispatch', mode: 'runtime dependent',
                    reason: 'Choose at runtime', candidates: [{ priority: 1, strategy: 'scan',
                        canAttempt: true, decision: 'CANDIDATE', condition: 'rows > 0' }] }];
                if (level === 'Telemetry') {
                    plan.executionProfile = {
                        status: 'TIMED_OUT', elapsedNanos: 4000000, detail: 'Stopped', droppedEvents: 2,
                        properties: { consumptionMode: 'values', inputAlgebra: 'Projection\n'
                            + '   StatementPattern\n      p: Var (name=p, value=https://example.test/algebra?at=27)' },
                        diagnostics: [{ severity: 'WARN', message: 'Slow', evidence: ['sample: 17'] }],
                        phases: [{ name: 'execution', elapsedNanos: 3000000, scope: 'wall' }],
                        stages: [{ id: 'scan/0', operator: 'Scan', engine: native ? 'lmdb-native' : 'generic',
                            status: 'PARTIAL', properties: { route: adjacency ? 'adjacency' : 'index' },
                            metrics: { scanned: { count: 17, unit: 'rows', origin: 'measured', state: 'complete', scope: 'self' } } }],
                        attempts: [{ stageId: 'scan/0', strategy: 'scan', status: 'PARTIAL', estimatedMillis: 2 }],
                        workers: [{ stageId: 'scan/0', ordinal: 1, rows: 17, complete: false }]
                    };
                }
                const target = harness.document.createElement('pre');
                const rendered = highlighter.render(plan, { mode: 'syntax', level, namespaces: { ex: 'https://example.test/' } });
                target.appendChild(rendered.fragment);
                const textFor = kind => Array.from(target.querySelectorAll('.query-explanation-token--' + kind),
                    token => token.textContent.trim());
                for (const heading of ['Physical plan', 'Query explanation', 'StatementPattern']) {
                    assert.ok(textFor('node-type').includes(heading), `${context}: ${heading}`);
                }
                for (const field of ['runtime', 'rowsScannedActual', 'Would select now']) {
                    assert.ok(textFor('metric-name').includes(field), `${context}: ${field}`);
                }
                assert.ok(textFor('metric-value').includes(adjacency ? '17' : '23'), context);
                if (adjacency) {
                    assert.ok(textFor('binding-bound').includes('BOUND'), context);
                    assert.ok(textFor('binding-unbound').includes('UNBOUND'), context);
                    assert.ok(textFor('metric-value').includes('0x0F'), context);
                }
                if (native) {
                    assert.ok(textFor('variable').includes('?s#0'), context);
                    assert.ok(textFor('value-iri').includes('ex:p'), context);
                    assert.deepEqual(textFor('value-literal'), ['"id=7 <https://example.test/literal>"'], context);
                    assert.equal(target.querySelector('.query-explanation-token--value-iri').getAttribute('title'),
                        '<https://example.test/p>', context);
                }
                if (level === 'Telemetry') {
                    assert.ok(textFor('node-type').includes('Execution profile'), context);
                    assert.ok(textFor('node-type').includes('Scan'), context);
                    assert.ok(textFor('annotation').includes('[measured, complete, self]'), context);
                    assert.ok(textFor('node-type').includes('Projection'), context);
                    assert.ok(target.textContent.includes('value=https://example.test/algebra?at=27'), context);
                    for (const field of ['consumptionMode', 'WARN', 'evidence', 'phase execution', 'stage scan/0',
                        'route', 'scanned', 'attempt scan/0', 'worker scan/0/1', 'omitted events']) {
                        assert.ok(textFor('metric-name').includes(field), `${context}: ${field}`);
                    }
                    assert.deepEqual(textFor('strategy'), ['scan', 'scan'], context);
                }
                assert.equal(rendered.text, highlighter.format(plan, level).text, context);
            }
        }
    }
});

test('keeps multiline literals opaque in preludes and execution profile properties', () => {
    const { harness, highlighter } = createHighlighterHarness();
    const embedded = '<https://example.test/literal> (id=19)';
    const details = `Var (name=o, value="first line\n${embedded}\nlast line")\n`
        + 'object: BOUND[<https://example.test/resource> (id=23)]';
    const plan = { type: 'StatementPattern', physicalPlanPrelude: 'Physical plan\n' + details,
        executionProfile: { status: 'COMPLETED', properties: { inputAlgebra: details } } };
    const target = harness.document.createElement('pre');
    target.appendChild(highlighter.render(plan, {
        mode: 'syntax', level: 'Telemetry', namespaces: { ex: 'https://example.test/' }
    }).fragment);
    assert.equal(Array.from(target.querySelectorAll('.query-explanation-token--value-literal'))
        .filter(token => token.textContent === embedded).length, 2);
    assert.deepEqual(Array.from(target.querySelectorAll('.query-explanation-token--value-iri'), token => token.textContent),
        ['ex:resource', 'ex:resource']);
});

test('matches the Java formatter for every shared plan fixture', () => {
    const { highlighter } = createHighlighterHarness();
    const parserHarness = createQueryBrowserHarness();
    const parsePlanJson = parserHarness.context.workbench.query.testing.parsePlanJson;
    const fixtureDirectory = path.resolve(
        __dirname,
        '../../../test/resources/query-explanation-highlighter'
    );
    const fixtureFiles = fs.readdirSync(fixtureDirectory)
        .filter((fileName) => fileName.endsWith('.json'))
        .sort();

    for (const fixtureFile of fixtureFiles) {
        const fixtureName = fixtureFile.replace(/\.json$/, '');
        const level = fixtureName.substring(0, fixtureName.indexOf('-'));
        const plan = parsePlanJson(fs.readFileSync(path.join(fixtureDirectory, fixtureFile), 'utf8'));
        const expected = fs.readFileSync(path.join(fixtureDirectory, fixtureName + '.txt'), 'utf8');

        assert.equal(highlighter.format(plan, level).text, expected, fixtureName);
    }
});
