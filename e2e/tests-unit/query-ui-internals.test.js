const test = require('node:test');
const assert = require('node:assert/strict');

const { createQueryBrowserHarness } = require('./query-browser-harness.js');

function readyState(explanation) {
    return {
        kind: 'ready',
        freshness: 'current',
        staleReasons: [],
        explanation
    };
}

function explanation(overrides = {}) {
    return Object.assign({
        queryHash: 'SELECT *',
        level: 'Optimized',
        requestedFormat: 'text',
        responseFormat: 'text',
        view: 'text',
        rawContent: 'plan'
    }, overrides);
}

function pageState(primaryPane, comparePane, layout = { mode: 'single' }) {
    return {
        lifecycle: 'ready',
        layout,
        primaryPane,
        comparePane,
        diffModal: { kind: 'closed' },
        inputs: {
            primaryQueryHash: 'SELECT *',
            compareQueryHash: 'DESCRIBE *',
            explainLevel: 'Optimized',
            explainFormat: 'text'
        },
        compareQuerySeeded: false
    };
}

test('query ui helpers cover editors, cookies, buttons, wait states, and dot rendering', async () => {
    const harness = createQueryBrowserHarness();
    const testing = harness.context.workbench.query.testing;

    const primaryEditor = {
        saveCount: 0,
        refreshCount: 0,
        value: 'SELECT * WHERE {?s ?p ?o}',
        getValue() {
            return this.value;
        },
        save() {
            this.saveCount += 1;
        },
        setValue(value) {
            this.value = value;
        },
        refresh() {
            this.refreshCount += 1;
        },
        toTextArea() {
            this.closed = true;
        }
    };
    const compareEditor = {
        saveCount: 0,
        refreshCount: 0,
        value: 'DESCRIBE *',
        getValue() {
            return this.value;
        },
        save() {
            this.saveCount += 1;
        },
        setValue(value) {
            this.value = value;
        },
        refresh() {
            this.refreshCount += 1;
        },
        toTextArea() {
            this.closed = true;
        }
    };

    testing.setPaneQueryEditor('primary', primaryEditor);
    testing.setPaneQueryEditor('compare', compareEditor);
    assert.equal(testing.getPaneQuerySelector('compare'), '#query-compare');
    assert.equal(testing.getPanePersistedQueryStorageKey('compare'), 'yasqe_query-compare-pane_queryVal');
    assert.equal(testing.getPaneRawQueryValue('primary'), 'SELECT * WHERE {?s ?p ?o}');
    testing.setPaneQueryValue('primary', 'ASK {}');
    assert.equal(testing.getPaneQueryValue('primary'), 'ASK {}');
    testing.persistPrimaryQueryEditorValue(primaryEditor);
    assert.equal(primaryEditor.saveCount, 1);
    testing.savePaneQuery('primary');
    assert.equal(primaryEditor.saveCount, 2);
    testing.clearPanePersistedQuery('compare');

    assert.equal(testing.getWorkbenchCookiePath(), '/rdf4j-workbench');
    testing.setWorkbenchCookie('query', 'ASK {}');
    assert.equal(harness.context.workbench.getCookie('query'), 'ASK {}');
    testing.clearWorkbenchCookie('query');
    assert.equal(harness.context.workbench.getCookie('query'), '');
    testing.setWorkbenchCookie('ref', 'hash');
    testing.persistPrimaryQueryValue();
    assert.equal(harness.context.workbench.getCookie('query'), 'ASK {}');
    assert.equal(harness.context.workbench.getCookie('ref'), '');

    testing.clearExplainSelection();
    assert.equal(harness.getProperty('explain', 'value'), '');
    assert.equal(harness.getProperty('explain-level', 'value'), 'Optimized');

    testing.setInternalState({
        compareModeEnabled: false,
        queryPageState: pageState(readyState(explanation()), { kind: 'empty' })
    });
    testing.updateDownloadButtonState();
    testing.syncPrimaryExplanationControls();
    testing.syncCompareSidebarState();
    assert.equal(harness.getProperty('download-explanation', 'disabled'), false);
    assert.equal(harness.getAttribute('query-sidebar-toggle', 'aria-hidden'), 'true');

    testing.setInternalState({
        compareModeEnabled: true,
        compareSidebarOpen: true,
        queryPageState: pageState(readyState(explanation()), readyState(explanation({ rawContent: 'compare' })), {
            mode: 'compare',
            sidebar: 'open'
        })
    });
    testing.syncCompareSidebarState();
    assert.equal(harness.hasClass('query-sidebar-toggle', 'query-sidebar-toggle--nav-open'), true);

    const explanationElement = harness.document.getElementById('query-explanation');
    explanationElement.outerHeight = 40;
    explanationElement.outerWidth = 80;
    testing.lockExplanationDimensions('primary');
    assert.equal(explanationElement.style['min-height'], '40px');
    testing.clearExplanationDimensionLock('primary');
    assert.equal(explanationElement.style['min-height'], '');

    testing.setExplainButtonsDisabled(true);
    assert.equal(harness.getProperty('explain-trigger', 'disabled'), true);
    testing.showExplainCancelButton('rerun-explanation');
    assert.equal(harness.hasClass('rerun-explanation-cancel', 'query-explain-cancel--visible'), true);
    testing.showExplainSpinner('explain-trigger');
    assert.equal(harness.hasClass('explain-trigger-cancel', 'query-explain-cancel--visible'), true);
    testing.hideExplainSpinners();
    assert.equal(harness.hasClass('explain-trigger-cancel', 'query-explain-cancel--visible'), false);

    const signature = {
        requestId: 7,
        serverRequestId: 'request-7',
        pane: 'primary',
        source: 'primary-explain',
        queryHash: 'ASK {}',
        level: 'Optimized',
        format: 'text'
    };
    testing.beginExplainRequest('explain-trigger', signature);
    harness.advanceTimers(1000);
    assert.equal(harness.hasClass('explain-trigger-cancel', 'query-explain-cancel--visible'), true);
    testing.finishExplainRequest(7);
    harness.advanceTimers(1000);
    assert.equal(harness.hasClass('explain-trigger-cancel', 'query-explain-cancel--visible'), false);

    testing.setInternalState({ activeComparePendingRequests: 1, activeCompareRequestId: 4 });
    testing.beginComparePrimaryExplainWaitState('explain-trigger');
    harness.advanceTimers(1000);
    assert.equal(harness.hasClass('explain-trigger-cancel', 'query-explain-cancel--visible'), true);
    testing.finishComparePrimaryExplainWaitState(4);
    harness.advanceTimers(1000);
    assert.equal(harness.hasClass('explain-trigger-cancel', 'query-explain-cancel--visible'), false);

    testing.showCompareExplainSpinner();
    assert.equal(harness.hasClass('explanation-cancel', 'query-explain-cancel--visible'), true,
        'a compare explanation shows the Cancel beside Config');
    testing.hideCompareExplainSpinner();
    assert.equal(harness.hasClass('explanation-cancel', 'query-explain-cancel--visible'), false);
    testing.showCompareExplainCancelButton();
    assert.equal(harness.hasClass('explanation-cancel', 'query-explain-cancel--visible'), true);
    testing.hideCompareExplainCancelButton();
    assert.equal(harness.getAttribute('explanation-cancel', 'aria-hidden'), 'true');
    testing.setCompareExplainButtonsDisabled(true);
    assert.equal(harness.getProperty('explain-trigger', 'disabled'), true);

    const explainButton = harness.document.getElementById('explain-trigger');
    explainButton.getBoundingClientRect = () => ({ top: 10 });
    let scrolledBy = null;
    harness.window.scrollBy = (x, y) => {
        scrolledBy = [x, y];
    };
    testing.captureExplainButtonViewportTop('primary', 'explain-trigger');
    explainButton.getBoundingClientRect = () => ({ top: 20 });
    testing.restoreExplainButtonViewportTopIfNeeded('primary');
    assert.deepEqual(scrolledBy, [0, 10]);
    testing.clearExplainButtonViewportRestoreState('primary');

    testing.setExplanationDisplayMode('primary', 'dot');
    assert.equal(harness.document.getElementById('query-explanation-dot-view').style.display, '');
    testing.clearRenderedExplanation('primary', 'json');
    assert.equal(harness.getAttribute('query-explanation', 'data-format'), 'json');
    testing.showExplainError('primary', 'boom');
    assert.equal(harness.getText('query-explanation'), 'boom');

    testing.setInternalState({
        queryPageState: pageState(
            readyState(explanation({ responseFormat: 'dot', requestedFormat: 'dot', view: 'dotRendering', rawContent: 'digraph{}' })),
            { kind: 'empty' }
        )
    });
    testing.renderDotView('primary', '', 'dot');
    assert.equal(harness.document.getElementById('query-explanation-dot-view').children.length, 0);
    testing.renderDotView('primary', 'digraph{}', 'dot');
    assert.match(harness.getHtml('query-explanation-dot-view'), /Graphviz visualizer script not loaded/);

    const svg = harness.registerElement('svg', {});
    harness.context.svgPanZoom = () => ({ destroy() { svg.destroyed = true; } });
    harness.context.Viz = function Viz() {
        this.renderSVGElement = () => Promise.resolve(svg);
    };
    testing.renderDotView('primary', 'digraph{}', 'dot');
    await Promise.resolve();
    await new Promise((resolve) => setImmediate(resolve));
    testing.applyDotPanZoom('primary', svg);
    testing.destroyDotPanZoom('primary');
    assert.equal(svg.destroyed, true);

    harness.context.Viz = function Viz() {
        this.renderSVGElement = () => Promise.reject(new Error('nope'));
    };
    testing.renderDotView('primary', 'digraph{}', 'dot');
    await Promise.resolve();
    await Promise.resolve();
    assert.match(harness.getHtml('query-explanation-dot-view'), /(Unable to render DOT graph|Graphviz visualizer script not loaded)/);

    testing.postCancelExplain('request-cancel');
    assert.equal(harness.requestsByAction('cancel-explain').length > 0, true);
    testing.setInternalState({ compareModeEnabled: true });
    testing.refreshVisibleQueryEditors();
    assert.equal(primaryEditor.refreshCount > 0, true);
    assert.equal(compareEditor.refreshCount > 0, true);
});

// Plan task M11.1: the graph renderer is loaded on the first DOT explanation, not with the Query page.
test('a DOT explanation loads the graph renderer first and then draws the graph', async () => {
    const harness = createQueryBrowserHarness();
    harness.runPageLoad();
    const testing = harness.context.workbench.query.testing;
    const svg = harness.registerElement('svg', {});
    const loads = [];
    let finishLoad;
    harness.context.workbench.app = {
        loadScripts(names) {
            loads.push(names);
            return new Promise((resolve) => { finishLoad = resolve; });
        }
    };
    testing.setInternalState({
        queryPageState: pageState(
            readyState(explanation({ responseFormat: 'dot', requestedFormat: 'dot', view: 'dotRendering', rawContent: 'digraph{}' })),
            { kind: 'empty' }
        )
    });

    testing.renderDotView('primary', 'digraph{}', 'dot');
    assert.deepEqual(JSON.parse(JSON.stringify(loads)), [['viz/viz.js', 'viz/full.render.js', 'svg-pan-zoom.min.js']]);
    assert.match(harness.getHtml('query-explanation-dot-view'), /Loading graph renderer…/);
    testing.renderDotView('primary', 'digraph{}', 'dot');
    assert.equal(loads.length, 1, 'a second request for the same graph waits for the same load');
    harness.context.svgPanZoom = () => ({ destroy() {} });
    harness.context.Viz = function Viz() {
        this.renderSVGElement = () => Promise.resolve(svg);
    };
    finishLoad();
    await new Promise((resolve) => setImmediate(resolve));
    await new Promise((resolve) => setImmediate(resolve));

    assert.equal(harness.document.getElementById('query-explanation-dot-view').children[0], svg);
});

test('a graph renderer that cannot be loaded is reported in the explanation', async () => {
    const harness = createQueryBrowserHarness();
    harness.runPageLoad();
    const testing = harness.context.workbench.query.testing;
    harness.context.workbench.app = { loadScripts: () => Promise.reject(new Error('offline')) };
    testing.setInternalState({
        queryPageState: pageState(
            readyState(explanation({ responseFormat: 'dot', requestedFormat: 'dot', view: 'dotRendering', rawContent: 'digraph{}' })),
            { kind: 'empty' }
        )
    });

    testing.renderDotView('primary', 'digraph{}', 'dot');
    await new Promise((resolve) => setImmediate(resolve));

    assert.match(harness.getHtml('query-explanation-dot-view'), /Graphviz visualizer script not loaded/);
});
