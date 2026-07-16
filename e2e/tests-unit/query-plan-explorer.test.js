const test = require('node:test');
const assert = require('node:assert/strict');

const explorer = require('../../tools/workbench/src/main/webapp/scripts/query-plan-explorer.js');

function sampleTrace() {
    return {
        formatVersion: '1',
        ruleCatalog: {
            'lmdb-bound-lookup': {
                id: 'lmdb-bound-lookup',
                kind: 'implementation',
                phase: 'explore'
            },
            'join-commute': {
                id: 'join-commute',
                kind: 'transformation',
                phase: 'explore'
            }
        },
        ruleEvaluations: [
            {
                eventIndex: 1,
                expression: 'g0:e0',
                rule: 'lmdb-bound-lookup',
                kind: 'implementation',
                status: 'matched',
                promise: 80
            },
            {
                eventIndex: 2,
                expression: 'g0:e0',
                rule: 'join-commute',
                kind: 'transformation',
                status: 'not_matched',
                promise: 0
            }
        ],
        alternatives: [
            {
                eventIndex: 3,
                phase: 'accepted',
                group: 'g0',
                expression: 'g0:e1',
                rule: 'lmdb-bound-lookup',
                cost: {
                    rows: 3,
                    workRows: 9,
                    qError: 1
                }
            },
            {
                eventIndex: 4,
                phase: 'discarded',
                group: 'g0',
                expression: 'g0:e2',
                rule: 'join-commute',
                reason: 'dominated',
                cost: {
                    rows: 30,
                    workRows: 90,
                    qError: 4
                }
            }
        ],
        winners: [
            {
                eventIndex: 5,
                group: 'g0',
                expression: 'g0:e1',
                rule: 'lmdb-bound-lookup',
                cost: {
                    rows: 3,
                    workRows: 9,
                    qError: 1
                }
            }
        ],
        events: [
            {
                eventIndex: 1,
                type: 'ruleEvaluation',
                status: 'matched',
                rule: 'lmdb-bound-lookup'
            },
            {
                eventIndex: 3,
                type: 'alternative',
                phase: 'accepted',
                expression: 'g0:e1'
            },
            {
                eventIndex: 5,
                type: 'winner',
                expression: 'g0:e1'
            }
        ]
    };
}

test('query plan explorer normalizes optimizer trace snapshots', () => {
    const model = explorer.createViewModel({
        query: 'SELECT * WHERE { ?s ?p ?o }',
        optimizerTrace: sampleTrace()
    });

    assert.equal(model.summary.formatVersion, '1');
    assert.equal(model.summary.ruleEvaluations, 2);
    assert.equal(model.summary.alternatives, 2);
    assert.equal(model.summary.winners, 1);
    assert.equal(model.statusCounts.matched, 1);
    assert.equal(model.statusCounts.not_matched, 1);
    assert.equal(model.ruleEvaluations[0].ruleId, 'lmdb-bound-lookup');
    assert.equal(model.ruleEvaluations[0].kind, 'implementation');
    assert.equal(model.alternatives[0].winner, true);
    assert.equal(model.alternatives[1].rejected, true);
    assert.equal(model.timeline[0].label, 'lmdb-bound-lookup matched');
});

test('query plan explorer extracts optimizer trace from explanation debug metrics', () => {
    const snapshot = {
        explanations: {
            optimized: {
                debugMetrics: {
                    'optimizer.cascadesTraceJson': JSON.stringify(sampleTrace())
                }
            }
        }
    };

    const model = explorer.createViewModel(JSON.stringify(snapshot));

    assert.equal(model.summary.hasTrace, true);
    assert.equal(model.ruleEvaluations.length, 2);
    assert.equal(model.winners[0].expression, 'g0:e1');
});

test('query plan explorer filters rules and compares alternatives', () => {
	const model = explorer.createViewModel({ optimizerTrace: sampleTrace() });
	const onlySkipped = explorer.filterRuleEvaluations(model, { status: 'not_matched' });
	const comparison = explorer.compareAlternatives(model, 'g0:e1', 'g0:e2');

    assert.deepEqual(onlySkipped.map((row) => row.ruleId), ['join-commute']);
    assert.equal(comparison.left.expression, 'g0:e1');
    assert.equal(comparison.right.expression, 'g0:e2');
	assert.equal(comparison.deltas.workRows, 81);
	assert.equal(comparison.deltas.rows, 27);
});

test('query plan explorer labels actual telemetry events without expression strings', () => {
	const model = explorer.createViewModel({
		optimizerTrace: {
			formatVersion: '1',
			ruleEvaluations: [
				{
					eventIndex: 10,
					groupId: 4,
					expressionId: 4,
					operator: 'Extension',
					ruleId: 'distinct-enforcer',
					kind: 'ENFORCER',
					status: 'not_matched',
					promise: 0
				}
			],
			alternatives: [
				{
					eventIndex: 11,
					groupId: 4,
					ruleId: 'generic-physical-implementation',
					status: 'accepted',
					localCost: { rows: 2, workRows: 5, qError: 1 },
					plan: 'Extension Join StatementPattern'
				},
				{
					eventIndex: 12,
					groupId: 4,
					ruleId: 'join-commute',
					status: 'discarded',
					reason: 'dominated',
					cost: { rows: 7, workRows: 21, qError: 4 },
					plan: 'Join StatementPattern StatementPattern'
				}
			],
			winners: [
				{
					eventIndex: 13,
					groupId: 4,
					expressionId: 28,
					operator: 'Join',
					ruleId: 'lmdb-cascades-connected-hypergraph-join',
					cost: { rows: 2, workRows: 5, qError: 1 }
				}
			],
			events: []
		}
	});
	const comparison = explorer.compareAlternatives(model, model.alternatives[0].expression, model.alternatives[1].expression);

	assert.equal(model.ruleEvaluations[0].expression, 'g4:e4 Extension');
	assert.equal(model.alternatives[0].expression, 'g4:a11 generic-physical-implementation');
	assert.equal(model.alternatives[0].phase, 'accepted');
	assert.equal(model.alternatives[1].rejected, true);
	assert.equal(model.winners[0].expression, 'g4:e28 Join');
	assert.equal(comparison.deltas.workRows, 16);
});
