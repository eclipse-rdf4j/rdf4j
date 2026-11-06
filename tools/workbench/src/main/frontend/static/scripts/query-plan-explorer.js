(function (root, factory) {
    const api = factory(root || {});
    if (typeof module !== 'undefined' && module.exports) {
        module.exports = api;
    }
    if (root) {
        root.workbench = root.workbench || {};
        root.workbench.queryPlanExplorer = api;
    }
}(typeof window !== 'undefined' ? window : globalThis, function (root) {
    'use strict';

    const TRACE_JSON_METRIC = 'optimizer.cascadesTraceJson';
    const EMPTY_TRACE = {
        formatVersion: '',
        ruleCatalog: {},
        ruleEvaluations: [],
        alternatives: [],
        winners: [],
        events: []
    };

    function isObject(value) {
        return value !== null && typeof value === 'object' && !Array.isArray(value);
    }

    function parseJson(value) {
        if (typeof value !== 'string') {
            return value;
        }
        const trimmed = value.trim();
        if (!trimmed) {
            return {};
        }
        return JSON.parse(trimmed);
    }

    function parseSnapshot(input) {
        try {
            const parsed = parseJson(input);
            return isObject(parsed) ? parsed : {};
        } catch (error) {
            return {
                parseError: error.message
            };
        }
    }

    function asArray(value) {
        return Array.isArray(value) ? value : [];
    }

    function readMetricContainer(container) {
        if (!isObject(container)) {
            return null;
        }
        const metrics = container.debugMetrics || container.metrics || container.telemetry;
        if (isObject(metrics) && metrics[TRACE_JSON_METRIC]) {
            return metrics[TRACE_JSON_METRIC];
        }
        if (container[TRACE_JSON_METRIC]) {
            return container[TRACE_JSON_METRIC];
        }
        return null;
    }

    function findTraceJson(value, seen) {
        if (value == null) {
            return null;
        }
        if (typeof value === 'string') {
            return null;
        }
        if (!isObject(value) && !Array.isArray(value)) {
            return null;
        }
        if (seen.has(value)) {
            return null;
        }
        seen.add(value);
        const direct = readMetricContainer(value);
        if (direct) {
            return direct;
        }
        const entries = Array.isArray(value) ? value : Object.values(value);
        for (const entry of entries) {
            const found = findTraceJson(entry, seen);
            if (found) {
                return found;
            }
        }
        return null;
    }

    function normalizeTrace(snapshotInput) {
        const snapshot = parseSnapshot(snapshotInput);
        if (snapshot.parseError) {
            return Object.assign({}, EMPTY_TRACE, {
                parseError: snapshot.parseError
            });
        }
        let trace = snapshot.optimizerTrace || snapshot.trace || snapshot.cascadesTrace;
        if (!trace) {
            trace = findTraceJson(snapshot.explanations || snapshot, new Set());
        }
        if (!trace) {
            return Object.assign({}, EMPTY_TRACE);
        }
        try {
            const parsedTrace = parseJson(trace);
            return Object.assign({}, EMPTY_TRACE, isObject(parsedTrace) ? parsedTrace : {});
        } catch (error) {
            return Object.assign({}, EMPTY_TRACE, {
                parseError: error.message
            });
        }
    }

    function text(value, fallback) {
        if (value === undefined || value === null || value === '') {
            return fallback || '';
        }
        return String(value);
    }

    function numberValue(value) {
        const number = Number(value);
        return Number.isFinite(number) ? number : null;
    }

    function costValue(cost, name) {
        if (!isObject(cost)) {
            return null;
        }
        return numberValue(cost[name]);
    }

	function eventIndex(row, fallback) {
		const parsed = numberValue(row.eventIndex);
		return parsed == null ? fallback : parsed;
	}

	function groupLabel(row) {
		return text(row.group, row.groupId === undefined || row.groupId === null ? '' : `g${row.groupId}`);
	}

	function expressionLabel(row, fallbackPrefix) {
		const explicit = text(row.expression || row.memoExpr);
		if (explicit) {
			return explicit;
		}
		const group = groupLabel(row);
		const operator = text(row.operator);
		if (row.expressionId !== undefined && row.expressionId !== null) {
			return `${group}:e${row.expressionId}${operator ? ` ${operator}` : ''}`;
		}
		if (group) {
			return `${group}:${fallbackPrefix}${eventIndex(row, 0)} ${text(row.rule || row.ruleId || row.phase || row.status, 'event')}`;
		}
		return '';
	}

	function rowCost(row) {
		return isObject(row.cost) ? row.cost : isObject(row.localCost) ? row.localCost : {};
	}

	function normalizeRule(row, index, catalog) {
		const ruleId = text(row.rule || row.ruleId, 'unknown-rule');
		const catalogEntry = catalog[ruleId] || {};
		return {
			eventIndex: eventIndex(row, index),
			expression: expressionLabel(row, 'r'),
			ruleId,
			kind: text(row.kind, text(catalogEntry.kind, 'unknown')),
			phase: text(row.phase, text(catalogEntry.phase, '')),
            status: text(row.status, 'unknown'),
            promise: numberValue(row.promise),
            reason: text(row.reason),
            goal: text(row.goal)
        };
	}

	function normalizeWinner(row, index) {
		const cost = rowCost(row);
		return {
			eventIndex: eventIndex(row, index),
			group: groupLabel(row),
			expression: expressionLabel(row, 'w'),
			ruleId: text(row.rule || row.ruleId),
			cost,
			rows: costValue(cost, 'rows'),
            workRows: costValue(cost, 'workRows'),
            qError: costValue(cost, 'qError')
        };
	}

	function normalizeAlternative(row, index, winnerKeys) {
		const cost = rowCost(row);
		const phase = text(row.phase || row.status, 'considered');
		const expression = expressionLabel(row, 'a');
		const group = groupLabel(row);
		const key = alternativeKey(group, expression);
		return {
			eventIndex: eventIndex(row, index),
            phase,
            group,
            expression,
            ruleId: text(row.rule || row.ruleId),
            reason: text(row.reason),
            cost,
            rows: costValue(cost, 'rows'),
            workRows: costValue(cost, 'workRows'),
            qError: costValue(cost, 'qError'),
            rejected: /discarded|rejected|dominated|pruned/.test(phase),
            winner: winnerKeys.has(key) || Boolean(row.winner)
        };
    }

    function alternativeKey(group, expression) {
        return `${group || ''}/${expression || ''}`;
    }

    function labelEvent(event) {
        if (event.type === 'ruleEvaluation') {
            return `${text(event.rule || event.ruleId, 'rule')} ${text(event.status, 'evaluated')}`;
        }
        if (event.type === 'alternative') {
            return `${text(event.expression, 'alternative')} ${text(event.phase, 'considered')}`;
        }
        if (event.type === 'winner') {
            return `${text(event.expression, 'winner')} selected`;
        }
        if (event.type === 'estimate') {
            return `${text(event.expression, 'expression')} estimated`;
        }
        return text(event.type, 'event');
    }

    function normalizeEvent(event, index) {
        return Object.assign({}, event, {
            eventIndex: eventIndex(event, index),
            label: labelEvent(event)
        });
    }

    function countByStatus(rows) {
        return rows.reduce((counts, row) => {
            counts[row.status] = (counts[row.status] || 0) + 1;
            return counts;
        }, {});
    }

    function createViewModel(input) {
        const snapshot = parseSnapshot(input);
        const trace = normalizeTrace(snapshot);
        const catalog = isObject(trace.ruleCatalog) ? trace.ruleCatalog : {};
        const winners = asArray(trace.winners).map(normalizeWinner);
        const winnerKeys = new Set(winners.map((winner) => alternativeKey(winner.group, winner.expression)));
        const ruleEvaluations = asArray(trace.ruleEvaluations)
            .map((row, index) => normalizeRule(row, index, catalog));
        const alternatives = asArray(trace.alternatives)
            .map((row, index) => normalizeAlternative(row, index, winnerKeys));
        const timeline = asArray(trace.events)
            .map(normalizeEvent)
            .sort((left, right) => left.eventIndex - right.eventIndex);
        const parseError = snapshot.parseError || trace.parseError || '';
        return {
            snapshot,
            trace,
            summary: {
                hasTrace: !parseError && (ruleEvaluations.length > 0 || alternatives.length > 0 || winners.length > 0),
                parseError,
                formatVersion: text(trace.formatVersion),
                eventCount: numberValue(trace.eventCount) || timeline.length,
                ruleEvaluations: ruleEvaluations.length,
                alternatives: alternatives.length,
                winners: winners.length,
                query: text(snapshot.query || snapshot.queryText)
            },
            statusCounts: countByStatus(ruleEvaluations),
            ruleEvaluations,
            alternatives,
            winners,
            timeline
        };
    }

    function filterRuleEvaluations(model, filters) {
        const status = text(filters && filters.status);
        const rule = text(filters && filters.rule).toLowerCase();
        const kind = text(filters && filters.kind);
        return model.ruleEvaluations.filter((row) => {
            if (status && status !== 'all' && row.status !== status) {
                return false;
            }
            if (kind && kind !== 'all' && row.kind !== kind) {
                return false;
            }
            return !rule || row.ruleId.toLowerCase().includes(rule) || row.expression.toLowerCase().includes(rule);
        });
    }

    function findAlternative(model, selector) {
        if (selector == null || selector === '') {
            return null;
        }
        const index = Number(selector);
        if (Number.isInteger(index) && model.alternatives[index]) {
            return model.alternatives[index];
        }
        return model.alternatives.find((row) => row.expression === selector || alternativeKey(row.group, row.expression) === selector) || null;
    }

    function delta(right, left) {
        if (right == null || left == null) {
            return null;
        }
        return right - left;
    }

	function compareAlternatives(model, leftSelector, rightSelector) {
		const left = findAlternative(model, leftSelector);
		const right = findAlternative(model, rightSelector);
		return {
            left,
            right,
            deltas: {
                rows: left && right ? delta(right.rows, left.rows) : null,
                workRows: left && right ? delta(right.workRows, left.workRows) : null,
                qError: left && right ? delta(right.qError, left.qError) : null
			}
		};
	}

	return {
		TRACE_JSON_METRIC,
		compareAlternatives,
		createViewModel,
		filterRuleEvaluations,
		normalizeTrace,
		parseSnapshot
	};
}));
