(function (root, factory) {
	const api = factory(root || {}, root && root.workbench && root.workbench.queryPlanExplorer);
	if (typeof module !== 'undefined' && module.exports) {
		module.exports = api;
	}
	if (root && root.workbench && root.workbench.queryPlanExplorer) {
		root.workbench.queryPlanExplorer.init = api.init;
		root.workbench.queryPlanExplorerPage = api;
	}
}(typeof window !== 'undefined' ? window : globalThis, function (root, modelApi) {
	'use strict';

	function text(value) {
		if (value === undefined || value === null || value === '') {
			return '';
		}
		return String(value);
	}

	function escapeHtml(value) {
		return text(value)
			.replace(/&/g, '&amp;')
			.replace(/</g, '&lt;')
			.replace(/>/g, '&gt;')
			.replace(/"/g, '&quot;')
			.replace(/'/g, '&#39;');
	}

	function formatNumber(value) {
		if (value == null) {
			return '-';
		}
		return Number(value).toLocaleString('en-US', {
			maximumFractionDigits: 3
		});
	}

	function statusOptions(model) {
		return ['all'].concat(Object.keys(model.statusCounts).sort());
	}

	function metricCard(label, value) {
		return `<article class="qpe-metric"><span>${escapeHtml(label)}</span><strong>${escapeHtml(value)}</strong></article>`;
	}

	function renderSummary(model) {
		const summary = model.summary;
		if (summary.parseError) {
			return `<div class="qpe-empty qpe-empty--error">Unable to parse snapshot: ${escapeHtml(summary.parseError)}</div>`;
		}
		if (!summary.hasTrace) {
			return '<div class="qpe-empty">No Cascades optimizer trace found. Run with rdf4j.optimizer.lmdb.cascades.trace=true and capture JSON.</div>';
		}
		return [
			metricCard('Format', summary.formatVersion || 'unknown'),
			metricCard('Rules', summary.ruleEvaluations),
			metricCard('Alternatives', summary.alternatives),
			metricCard('Winners', summary.winners),
			metricCard('Events', summary.eventCount)
		].join('');
	}

	function renderRules(model, filters) {
		const rows = modelApi.filterRuleEvaluations(model, filters);
		if (!rows.length) {
			return '<div class="qpe-empty">No rules match the current filters.</div>';
		}
		return `<table class="qpe-table">
			<thead><tr><th>#</th><th>Rule</th><th>Status</th><th>Kind</th><th>Promise</th><th>Expression</th><th>Reason</th></tr></thead>
			<tbody>${rows.map((row) => `<tr>
				<td>${escapeHtml(row.eventIndex)}</td>
				<td>${escapeHtml(row.ruleId)}</td>
				<td><span class="qpe-pill qpe-pill--${escapeHtml(row.status)}">${escapeHtml(row.status)}</span></td>
				<td>${escapeHtml(row.kind)}</td>
				<td>${formatNumber(row.promise)}</td>
				<td>${escapeHtml(row.expression)}</td>
				<td>${escapeHtml(row.reason)}</td>
			</tr>`).join('')}</tbody>
		</table>`;
	}

	function renderAlternatives(model) {
		if (!model.alternatives.length) {
			return '<div class="qpe-empty">No alternatives recorded.</div>';
		}
		return model.alternatives.map((row, index) => `<article class="qpe-alt ${row.winner ? 'qpe-alt--winner' : ''} ${row.rejected ? 'qpe-alt--rejected' : ''}">
			<div>
				<strong>${escapeHtml(row.expression || `alternative-${index}`)}</strong>
				<span>${escapeHtml(row.ruleId || 'unknown rule')}</span>
			</div>
			<dl>
				<div><dt>Rows</dt><dd>${formatNumber(row.rows)}</dd></div>
				<div><dt>Work</dt><dd>${formatNumber(row.workRows)}</dd></div>
				<div><dt>QErr</dt><dd>${formatNumber(row.qError)}</dd></div>
			</dl>
			<footer>
				<span>${escapeHtml(row.phase)}${row.reason ? `: ${escapeHtml(row.reason)}` : ''}</span>
				<button type="button" data-compare-left="${escapeHtml(row.expression)}">Left</button>
				<button type="button" data-compare-right="${escapeHtml(row.expression)}">Right</button>
			</footer>
		</article>`).join('');
	}

	function renderWinners(model) {
		if (!model.winners.length) {
			return '<div class="qpe-empty">No winners recorded.</div>';
		}
		return model.winners.map((winner) => `<article class="qpe-winner">
			<span>${escapeHtml(winner.group || 'root')}</span>
			<strong>${escapeHtml(winner.expression || 'winner')}</strong>
			<em>${escapeHtml(winner.ruleId || 'unknown rule')}</em>
			<dl>
				<div><dt>Rows</dt><dd>${formatNumber(winner.rows)}</dd></div>
				<div><dt>Work</dt><dd>${formatNumber(winner.workRows)}</dd></div>
				<div><dt>QErr</dt><dd>${formatNumber(winner.qError)}</dd></div>
			</dl>
		</article>`).join('');
	}

	function renderTimeline(model) {
		if (!model.timeline.length) {
			return '<div class="qpe-empty">No event timeline recorded.</div>';
		}
		return model.timeline.map((event) => `<li>
			<span>${escapeHtml(event.eventIndex)}</span>
			<strong>${escapeHtml(event.label)}</strong>
			<code>${escapeHtml(event.type)}</code>
		</li>`).join('');
	}

	function renderComparePane(label, row) {
		return `<article class="qpe-compare-pane">
			<span>${escapeHtml(label)}</span>
			<strong>${escapeHtml(row.expression)}</strong>
			<em>${escapeHtml(row.ruleId || 'unknown rule')}</em>
			<pre>${escapeHtml(JSON.stringify(row.cost || {}, null, 2))}</pre>
		</article>`;
	}

	function renderComparison(model, left, right) {
		const comparison = modelApi.compareAlternatives(model, left, right);
		if (!comparison.left || !comparison.right) {
			return '<div class="qpe-empty">Choose two alternatives to compare.</div>';
		}
		return `<div class="qpe-compare-grid">
			${renderComparePane('Left', comparison.left)}
			${renderComparePane('Right', comparison.right)}
			<article class="qpe-delta">
				<span>Delta, right minus left</span>
				<strong>Rows ${formatNumber(comparison.deltas.rows)}</strong>
				<strong>Work ${formatNumber(comparison.deltas.workRows)}</strong>
				<strong>QErr ${formatNumber(comparison.deltas.qError)}</strong>
			</article>
		</div>`;
	}

	function renderStatusSelect(select, model, selected) {
		select.innerHTML = statusOptions(model)
			.map((status) => `<option value="${escapeHtml(status)}"${status === selected ? ' selected' : ''}>${escapeHtml(status)}</option>`)
			.join('');
	}

	function loadDemo() {
		return {
			query: 'SELECT * WHERE { ?s ?p ?o }',
			optimizerTrace: {
				formatVersion: '1',
				ruleEvaluations: [
					{ eventIndex: 1, expression: 'g0:e0', rule: 'lmdb-bound-lookup', kind: 'implementation', status: 'matched', promise: 80 },
					{ eventIndex: 2, expression: 'g0:e0', rule: 'join-commute', kind: 'transformation', status: 'not_matched', promise: 0 }
				],
				alternatives: [
					{ eventIndex: 3, phase: 'accepted', group: 'g0', expression: 'g0:e1', rule: 'lmdb-bound-lookup', cost: { rows: 3, workRows: 9, qError: 1 } },
					{ eventIndex: 4, phase: 'discarded', group: 'g0', expression: 'g0:e2', rule: 'join-commute', reason: 'dominated', cost: { rows: 30, workRows: 90, qError: 4 } }
				],
				winners: [
					{ eventIndex: 5, group: 'g0', expression: 'g0:e1', rule: 'lmdb-bound-lookup', cost: { rows: 3, workRows: 9, qError: 1 } }
				],
				events: [
					{ eventIndex: 1, type: 'ruleEvaluation', status: 'matched', rule: 'lmdb-bound-lookup' },
					{ eventIndex: 3, type: 'alternative', phase: 'accepted', expression: 'g0:e1' },
					{ eventIndex: 5, type: 'winner', expression: 'g0:e1' }
				]
			}
		};
	}

	function init(options) {
		const doc = (options && options.document) || root.document;
		if (!doc || !modelApi) {
			return null;
		}
		const state = {
			model: modelApi.createViewModel({}),
			filters: { status: 'all', rule: '' },
			left: '',
			right: ''
		};
		const nodes = {
			input: doc.getElementById('qpe-json'),
			file: doc.getElementById('qpe-file'),
			load: doc.getElementById('qpe-load'),
			demo: doc.getElementById('qpe-demo'),
			clear: doc.getElementById('qpe-clear'),
			status: doc.getElementById('qpe-status-filter'),
			rule: doc.getElementById('qpe-rule-filter'),
			summary: doc.getElementById('qpe-summary'),
			rules: doc.getElementById('qpe-rules'),
			alternatives: doc.getElementById('qpe-alternatives'),
			winners: doc.getElementById('qpe-winners'),
			timeline: doc.getElementById('qpe-timeline'),
			comparison: doc.getElementById('qpe-comparison')
		};

		function render() {
			nodes.summary.innerHTML = renderSummary(state.model);
			renderStatusSelect(nodes.status, state.model, state.filters.status);
			nodes.rules.innerHTML = renderRules(state.model, state.filters);
			nodes.alternatives.innerHTML = renderAlternatives(state.model);
			nodes.winners.innerHTML = renderWinners(state.model);
			nodes.timeline.innerHTML = renderTimeline(state.model);
			nodes.comparison.innerHTML = renderComparison(state.model, state.left, state.right);
		}

		function load(value) {
			state.model = modelApi.createViewModel(value);
			state.left = state.model.alternatives[0] ? state.model.alternatives[0].expression : '';
			state.right = state.model.alternatives[1] ? state.model.alternatives[1].expression : '';
			render();
		}

		nodes.load.addEventListener('click', () => load(nodes.input.value));
		nodes.demo.addEventListener('click', () => {
			nodes.input.value = JSON.stringify(loadDemo(), null, 2);
			load(nodes.input.value);
		});
		nodes.clear.addEventListener('click', () => {
			nodes.input.value = '';
			load({});
		});
		nodes.status.addEventListener('change', () => {
			state.filters.status = nodes.status.value;
			render();
		});
		nodes.rule.addEventListener('input', () => {
			state.filters.rule = nodes.rule.value;
			render();
		});
		nodes.alternatives.addEventListener('click', (event) => {
			const target = event.target;
			if (target && target.getAttribute('data-compare-left')) {
				state.left = target.getAttribute('data-compare-left');
				render();
			} else if (target && target.getAttribute('data-compare-right')) {
				state.right = target.getAttribute('data-compare-right');
				render();
			}
		});
		nodes.file.addEventListener('change', () => {
			const file = nodes.file.files && nodes.file.files[0];
			if (!file || !root.FileReader) {
				return;
			}
			const reader = new root.FileReader();
			reader.onload = () => {
				nodes.input.value = String(reader.result || '');
				load(nodes.input.value);
			};
			reader.readAsText(file);
		});
		render();
		return { load, render, state };
	}

	return {
		escapeHtml,
		formatNumber,
		init,
		loadDemo,
		renderSummary
	};
}));
