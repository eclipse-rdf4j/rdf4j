/// <reference path="template.ts" />
/// <reference path="jquery.d.ts" />
/// <reference path="queryCancelPolicy.ts" />
/// <reference path="yasqe.d.ts" />
/// <reference path="yasqeHelper.ts" />

// WARNING: Do not edit the *.js version of this file. Instead, always edit the
// corresponding *.ts source in the ts subfolder, and then invoke the
// compileTypescript.sh bash script to generate new *.js and *.js.map files.

module workbench {

    export module query {

        /**
         * JSON value provided by script element in document (see query.xsl).
         */
        declare var sparqlNamespaces: any;
        declare var Viz: any;
        declare var svgPanZoom: any;

        /**
         * Holds the current selected query language.
         */
        var currentQueryLn = '';
        var yasqe: YASQE_Instance = null;
        var compareYasqe: YASQE_Instance = null;
        var vizRenderer: any = null;
        var queryPageState: QueryPageState = null;
        var lastRenderedExplanationKeys: { [key: string]: string } = {};
        var pendingDotRenderKeys: { [key: string]: string } = {};
        var activePrimaryRequestSignature: RequestSignature = null;
        var activeCompareRequestSignatures: { [key: string]: RequestSignature } = {};
        var explainServerRequestIdCounter = 0;
        var explainSpinnerVisibleSince = 0;
        var explainSpinnerTargetId = '';
        var explainSpinnerDelayTimeoutId: number = null;
        var explainSpinnerHideTimeoutId: number = null;
        var compareExplainSpinnerVisibleSince = 0;
        var compareExplainSpinnerDelayTimeoutId: number = null;
        var compareExplainSpinnerHideTimeoutId: number = null;
        var activeExplainRequestId = 0;
        var activeExplainJqXHR: JQueryXHR = null;
        var primaryExplanationPending = false;
        var activeCompareRequestId = 0;
        var activeComparePendingRequests = 0;
        var activeCompareExplainJqXHRs: JQueryXHR[] = [];
        var compareModeEnabled = false;
        var compareSidebarOpen = false;
        var compareQuerySeeded = false;
        var diffNotReadyLabel = '';
        var lastDiffTriggerElement: HTMLElement = null;

        type ExplainLevel = 'Unoptimized' | 'Optimized' | 'Executed' | 'Telemetry' | 'Timed';
        type ExplainFormat = 'text' | 'dot' | 'json';
        type StaleReason = 'query' | 'level' | 'format';
        type ExplanationView = 'text' | 'jsonTree' | 'jsonRawFallback' | 'dotRendering' | 'dotReady' | 'dotRenderError';
        type PaneKey = 'primary' | 'compare';

        type LayoutState =
            | { mode: 'single' }
            | { mode: 'compare'; sidebar: 'closed' | 'open' };

        interface StableExplanation {
            queryHash: string;
            level: ExplainLevel;
            requestedFormat: ExplainFormat;
            responseFormat: 'text' | 'json' | 'dot';
            view: ExplanationView;
            rawContent: string;
        }

        interface RequestSignature {
            requestId: number;
            serverRequestId: string;
            pane: PaneKey;
            source: 'primary-explain' | 'primary-rerun' | 'compare-auto' | 'compare-refresh-both' | 'compare-refresh-right';
            queryHash: string;
            level: ExplainLevel;
            format: ExplainFormat;
            groupId?: number;
        }

        type PaneState =
            | { kind: 'inactive' }
            | { kind: 'empty' }
            | {
                kind: 'loading';
                phase: 'delay' | 'spinner';
                mode: 'initial' | 'refresh';
                request: RequestSignature;
                previous?: StableExplanation;
            }
            | {
                kind: 'ready';
                freshness: 'current' | 'stale';
                staleReasons: StaleReason[];
                explanation: StableExplanation;
            }
            | {
                kind: 'error';
                mode: 'initial' | 'refresh';
                message: string;
                previous?: StableExplanation;
                freshness: 'current' | 'stale';
                staleReasons: StaleReason[];
            };

        type DiffModalState =
            | { kind: 'closed' }
            | { kind: 'open'; explanation: 'placeholder' | 'ready' };

        interface QueryPageInputs {
            primaryQueryHash: string;
            compareQueryHash: string;
            explainLevel: ExplainLevel;
            explainFormat: ExplainFormat;
        }

        interface QueryPageState {
            lifecycle: 'bootstrapping' | 'ready';
            layout: LayoutState;
            primaryPane: PaneState;
            comparePane: PaneState;
            diffModal: DiffModalState;
            inputs: QueryPageInputs;
            compareQuerySeeded: boolean;
        }

        type QueryPageEvent =
            | { type: 'HYDRATE'; primaryExplanation?: StableExplanation }
            | { type: 'PRIMARY_QUERY_CHANGED' }
            | { type: 'COMPARE_QUERY_CHANGED' }
            | { type: 'EXPLAIN_LEVEL_CHANGED' }
            | { type: 'EXPLAIN_FORMAT_CHANGED' }
            | { type: 'REQUEST_EXPLAIN'; signature: RequestSignature }
            | { type: 'SPINNER_DELAY_ELAPSED'; signature: RequestSignature }
            | { type: 'EXPLAIN_SUCCESS'; signature: RequestSignature; explanation: StableExplanation }
            | { type: 'EXPLAIN_ERROR'; signature: RequestSignature; message: string }
            | { type: 'DOT_RENDER_OK'; pane: PaneKey; explanationKey: string }
            | { type: 'DOT_RENDER_FAIL'; pane: PaneKey; explanationKey: string }
            | { type: 'CANCEL_EXPLAIN'; pane: PaneKey; signature?: RequestSignature }
            | { type: 'CLEAR_PANE'; pane: PaneKey; next: 'inactive' | 'empty' }
            | { type: 'TOGGLE_COMPARE' }
            | { type: 'TOGGLE_SIDEBAR' }
            | { type: 'OPEN_DIFF' }
            | { type: 'CLOSE_DIFF' };

        interface ExplanationPaneState {
            key: string;
            queryId: string;
            errorId: string;
            explanationRowId: string;
            explanationControlsRowId?: string;
            statusId: string;
            overlayId: string;
            explanationId: string;
            dotViewId: string;
            jsonViewId: string;
            latestExplanation: string;
            latestExplanationFormat: string;
            dotPanZoomInstance: any;
            explainButtonViewportTopBeforeRequest: number;
            explainButtonIdBeforeRequest: string;
        }

        var primaryPaneState: ExplanationPaneState = {
            key: 'primary',
            queryId: 'query',
            errorId: 'queryString.errors',
            explanationRowId: 'query-explanation-row',
            explanationControlsRowId: 'query-explanation-controls-row',
            statusId: 'query-explanation-status',
            overlayId: 'query-explanation-overlay',
            explanationId: 'query-explanation',
            dotViewId: 'query-explanation-dot-view',
            jsonViewId: 'query-explanation-json-view',
            latestExplanation: '',
            latestExplanationFormat: 'text',
            dotPanZoomInstance: null,
            explainButtonViewportTopBeforeRequest: null,
            explainButtonIdBeforeRequest: ''
        };
        var comparePaneState: ExplanationPaneState = {
            key: 'compare',
            queryId: 'query-compare',
            errorId: 'queryString.errors-compare',
            explanationRowId: 'query-explanation-row-compare',
            statusId: 'query-explanation-status-compare',
            overlayId: 'query-explanation-overlay-compare',
            explanationId: 'query-explanation-compare',
            dotViewId: 'query-explanation-dot-view-compare',
            jsonViewId: 'query-explanation-json-view-compare',
            latestExplanation: '',
            latestExplanationFormat: 'text',
            dotPanZoomInstance: null,
            explainButtonViewportTopBeforeRequest: null,
            explainButtonIdBeforeRequest: ''
        };

        function getNormalizedExplainLevel(level?: string): ExplainLevel {
            switch (level) {
                case 'Unoptimized':
                case 'Executed':
                case 'Telemetry':
                case 'Timed':
                    return <ExplainLevel>level;
                case 'Optimized':
                default:
                    return 'Optimized';
            }
        }

        function getNormalizedExplainFormat(format?: string): ExplainFormat {
            switch ((format || '').toLowerCase()) {
                case 'dot':
                    return 'dot';
                case 'json':
                    return 'json';
                case 'text':
                default:
                    return 'text';
            }
        }

        function buildQueryHash(queryValue: string): string {
            return queryValue || '';
        }

        function createEmptyQueryPageInputs(): QueryPageInputs {
            return {
                primaryQueryHash: '',
                compareQueryHash: '',
                explainLevel: 'Optimized',
                explainFormat: 'text'
            };
        }

        function collectCurrentInputs(): QueryPageInputs {
            return {
                primaryQueryHash: buildQueryHash(getPaneRawQueryValue('primary')),
                compareQueryHash: buildQueryHash(getPaneRawQueryValue('compare')),
                explainLevel: getNormalizedExplainLevel(<string>$('#explain-level').val()),
                explainFormat: getNormalizedExplainFormat(<string>$('#explain-format').val())
            };
        }

        function createRequestSignature(
            paneKey: PaneKey,
            source: 'primary-explain' | 'primary-rerun' | 'compare-auto' | 'compare-refresh-both' | 'compare-refresh-right',
            requestId: number,
            groupId?: number
        ): RequestSignature {
            var currentInputs = collectCurrentInputs();
            return {
                requestId: requestId,
                serverRequestId: generateExplainServerRequestId(),
                pane: paneKey,
                source: source,
                queryHash: getPaneQueryHashFromInputs(paneKey, currentInputs),
                level: currentInputs.explainLevel,
                format: currentInputs.explainFormat,
                groupId: groupId
            };
        }

        function createFallbackExplainServerRequestId(): string {
            explainServerRequestIdCounter += 1;

            var timestampPart = ('000000000000' + Date.now().toString(16)).slice(-12);
            var counterPart = ('00000000' + explainServerRequestIdCounter.toString(16)).slice(-8);
            var randomPart = '';
            var cryptoObject: any = (<any>window).crypto || (<any>window).msCrypto;

            if (cryptoObject && cryptoObject.getRandomValues) {
                var buffer = new Uint16Array(4);
                cryptoObject.getRandomValues(buffer);
                for (var i = 0; i < buffer.length; i++) {
                    randomPart += ('0000' + buffer[i].toString(16)).slice(-4);
                }
            } else {
                while (randomPart.length < 16) {
                    randomPart += ('00000000' + Math.floor(Math.random() * 0xffffffff).toString(16)).slice(-8);
                }
                randomPart = randomPart.substring(0, 16);
            }

            return timestampPart + '-' + randomPart.substring(0, 4) + '-4'
                + randomPart.substring(4, 7) + '-a' + randomPart.substring(7, 10)
                + '-' + randomPart.substring(10, 16) + counterPart.substring(0, 6);
        }

        function generateExplainServerRequestId(): string {
            var cryptoObject: any = (<any>window).crypto || (<any>window).msCrypto;
            if (cryptoObject && cryptoObject.randomUUID) {
                return cryptoObject.randomUUID();
            }
            return createFallbackExplainServerRequestId();
        }

        function createInitialQueryPageState(): QueryPageState {
            return {
                lifecycle: 'bootstrapping',
                layout: { mode: 'single' },
                primaryPane: { kind: 'empty' },
                comparePane: { kind: 'inactive' },
                diffModal: { kind: 'closed' },
                inputs: createEmptyQueryPageInputs(),
                compareQuerySeeded: false
            };
        }

        queryPageState = createInitialQueryPageState();

        function isCompareLayout(layout: LayoutState): boolean {
            return layout.mode === 'compare';
        }

        function getPaneMachineState(paneKey: PaneKey): PaneState {
            return paneKey === 'compare' ? queryPageState.comparePane : queryPageState.primaryPane;
        }

        function setPaneMachineState(paneKey: PaneKey, paneState: PaneState) {
            if (paneKey === 'compare') {
                queryPageState.comparePane = paneState;
                return;
            }
            queryPageState.primaryPane = paneState;
        }

        function cloneStableExplanation(explanation: StableExplanation): StableExplanation {
            if (!explanation) {
                return null;
            }
            return {
                queryHash: explanation.queryHash,
                level: explanation.level,
                requestedFormat: explanation.requestedFormat,
                responseFormat: explanation.responseFormat,
                view: explanation.view,
                rawContent: explanation.rawContent
            };
        }

        function getStableExplanationKey(explanation: StableExplanation): string {
            if (!explanation) {
                return '';
            }
            return [
                explanation.queryHash,
                explanation.level,
                explanation.requestedFormat,
                explanation.responseFormat,
                explanation.view,
                explanation.rawContent
            ].join('||');
        }

        function getStableExplanationContentKey(explanation: StableExplanation): string {
            if (!explanation) {
                return '';
            }
            return [
                explanation.queryHash,
                explanation.level,
                explanation.requestedFormat,
                explanation.responseFormat,
                explanation.rawContent
            ].join('||');
        }

        function getPaneSnapshot(paneState: PaneState): StableExplanation {
            if (!paneState) {
                return null;
            }
            if (paneState.kind === 'ready') {
                return cloneStableExplanation(paneState.explanation);
            }
            if (paneState.kind === 'loading' || paneState.kind === 'error') {
                return cloneStableExplanation(paneState.previous);
            }
            return null;
        }

        function getPaneQueryHashFromInputs(paneKey: PaneKey, inputs: QueryPageInputs): string {
            return paneKey === 'compare' ? inputs.compareQueryHash : inputs.primaryQueryHash;
        }

        function getStaleReasons(explanation: StableExplanation, paneKey: PaneKey, inputs: QueryPageInputs): StaleReason[] {
            var staleReasons: StaleReason[] = [];
            if (!explanation) {
                return staleReasons;
            }
            if (explanation.queryHash !== getPaneQueryHashFromInputs(paneKey, inputs)) {
                staleReasons.push('query');
            }
            if (explanation.level !== inputs.explainLevel) {
                staleReasons.push('level');
            }
            if (explanation.requestedFormat !== inputs.explainFormat) {
                staleReasons.push('format');
            }
            return staleReasons;
        }

        function createReadyPaneState(explanation: StableExplanation, paneKey: PaneKey, inputs: QueryPageInputs): PaneState {
            var staleReasons = getStaleReasons(explanation, paneKey, inputs);
            return {
                kind: 'ready',
                freshness: staleReasons.length ? 'stale' : 'current',
                staleReasons: staleReasons,
                explanation: cloneStableExplanation(explanation)
            };
        }

        function createErrorPaneState(
            message: string,
            mode: 'initial' | 'refresh',
            previous: StableExplanation,
            paneKey: PaneKey,
            inputs: QueryPageInputs
        ): PaneState {
            var staleReasons = previous ? getStaleReasons(previous, paneKey, inputs) : [];
            return {
                kind: 'error',
                mode: mode,
                message: message,
                previous: previous ? cloneStableExplanation(previous) : undefined,
                freshness: staleReasons.length ? 'stale' : 'current',
                staleReasons: staleReasons
            };
        }

        function restorePaneStateFromPrevious(
            paneState: PaneState,
            paneKey: PaneKey,
            inputs: QueryPageInputs,
            layout: LayoutState
        ): PaneState {
            var previousExplanation = getPaneSnapshot(paneState);
            if (previousExplanation) {
                return createReadyPaneState(previousExplanation, paneKey, inputs);
            }
            if (paneKey === 'compare' && !isCompareLayout(layout)) {
                return { kind: 'inactive' };
            }
            return { kind: 'empty' };
        }

        function isPaneReadyCurrent(paneState: PaneState): boolean {
            return paneState.kind === 'ready' && paneState.freshness === 'current';
        }

        function signaturesMatch(left: RequestSignature, right: RequestSignature): boolean {
            return !!left
                && !!right
                && left.requestId === right.requestId
                && left.serverRequestId === right.serverRequestId
                && left.pane === right.pane
                && left.source === right.source
                && left.queryHash === right.queryHash
                && left.level === right.level
                && left.format === right.format
                && left.groupId === right.groupId;
        }

        function getEventSignatureForPane(event: QueryPageEvent, paneKey: PaneKey): RequestSignature {
            switch (event.type) {
                case 'REQUEST_EXPLAIN':
                case 'SPINNER_DELAY_ELAPSED':
                case 'EXPLAIN_SUCCESS':
                case 'EXPLAIN_ERROR':
                    return event.signature.pane === paneKey ? event.signature : null;
                case 'CANCEL_EXPLAIN':
                    return event.pane === paneKey ? event.signature : null;
                default:
                    return null;
            }
        }

        function createDiffModalState(kind: 'closed' | 'open', primaryPane: PaneState, comparePane: PaneState): DiffModalState {
            if (kind === 'closed') {
                return { kind: 'closed' };
            }
            return {
                kind: 'open',
                explanation: isPaneReadyCurrent(primaryPane) && isPaneReadyCurrent(comparePane) ? 'ready' : 'placeholder'
            };
        }

        function reducePaneState(
            paneState: PaneState,
            paneKey: PaneKey,
            event: QueryPageEvent,
            inputs: QueryPageInputs,
            layout: LayoutState
        ): PaneState {
            var eventSignature = getEventSignatureForPane(event, paneKey);
            var paneSnapshot = getPaneSnapshot(paneState);
            switch (event.type) {
                case 'REQUEST_EXPLAIN':
                    if (!eventSignature) {
                        return paneState;
                    }
                    return {
                        kind: 'loading',
                        phase: 'delay',
                        mode: paneSnapshot ? 'refresh' : 'initial',
                        request: eventSignature,
                        previous: paneSnapshot || undefined
                    };
                case 'SPINNER_DELAY_ELAPSED':
                    if (paneState.kind !== 'loading' || !eventSignature || !signaturesMatch(paneState.request, eventSignature)) {
                        return paneState;
                    }
                    return {
                        kind: 'loading',
                        phase: 'spinner',
                        mode: paneState.mode,
                        request: paneState.request,
                        previous: paneState.previous
                    };
                case 'EXPLAIN_SUCCESS':
                    if (paneState.kind !== 'loading' || !eventSignature || !signaturesMatch(paneState.request, eventSignature)) {
                        return paneState;
                    }
                    return createReadyPaneState(event.explanation, paneKey, inputs);
                case 'EXPLAIN_ERROR':
                    if (paneState.kind !== 'loading' || !eventSignature || !signaturesMatch(paneState.request, eventSignature)) {
                        return paneState;
                    }
                    return createErrorPaneState(
                        event.message,
                        paneState.previous ? 'refresh' : 'initial',
                        paneState.previous,
                        paneKey,
                        inputs
                    );
                case 'CANCEL_EXPLAIN':
                    if (event.pane !== paneKey) {
                        return paneState;
                    }
                    return restorePaneStateFromPrevious(paneState, paneKey, inputs, layout);
                case 'PRIMARY_QUERY_CHANGED':
                    if (paneKey !== 'primary') {
                        return paneState.kind === 'loading'
                            ? restorePaneStateFromPrevious(paneState, paneKey, inputs, layout)
                            : paneState;
                    }
                    return restorePaneStateFromPrevious(paneState, paneKey, inputs, layout);
                case 'COMPARE_QUERY_CHANGED':
                    if (paneKey !== 'compare') {
                        return paneState.kind === 'loading'
                            ? restorePaneStateFromPrevious(paneState, paneKey, inputs, layout)
                            : paneState;
                    }
                    return restorePaneStateFromPrevious(paneState, paneKey, inputs, layout);
                case 'EXPLAIN_LEVEL_CHANGED':
                case 'EXPLAIN_FORMAT_CHANGED':
                    return restorePaneStateFromPrevious(paneState, paneKey, inputs, layout);
                case 'DOT_RENDER_OK':
                    if (paneState.kind !== 'ready'
                            || paneState.explanation.view !== 'dotRendering'
                            || event.pane !== paneKey
                            || getStableExplanationKey(paneState.explanation) !== event.explanationKey) {
                        return paneState;
                    }
                    paneState.explanation.view = 'dotReady';
                    return createReadyPaneState(paneState.explanation, paneKey, inputs);
                case 'DOT_RENDER_FAIL':
                    if (paneState.kind !== 'ready'
                            || paneState.explanation.view !== 'dotRendering'
                            || event.pane !== paneKey
                            || getStableExplanationKey(paneState.explanation) !== event.explanationKey) {
                        return paneState;
                    }
                    paneState.explanation.view = 'dotRenderError';
                    return createReadyPaneState(paneState.explanation, paneKey, inputs);
                case 'CLEAR_PANE':
                    if (event.pane !== paneKey) {
                        return paneState;
                    }
                    return event.next === 'inactive' ? { kind: 'inactive' } : { kind: 'empty' };
                default:
                    if (paneState.kind === 'ready') {
                        return createReadyPaneState(paneState.explanation, paneKey, inputs);
                    }
                    if (paneState.kind === 'error' && paneState.previous) {
                        return createErrorPaneState(paneState.message, paneState.mode, paneState.previous, paneKey, inputs);
                    }
                    return paneState;
            }
        }

        function reduceDiffModalState(
            diffModalState: DiffModalState,
            event: QueryPageEvent,
            primaryPane: PaneState,
            comparePane: PaneState,
            layout: LayoutState,
            inputs: QueryPageInputs
        ): DiffModalState {
            switch (event.type) {
                case 'OPEN_DIFF':
                    if (!isCompareLayout(layout) || !inputs.primaryQueryHash.length || !inputs.compareQueryHash.length) {
                        return { kind: 'closed' };
                    }
                    return createDiffModalState('open', primaryPane, comparePane);
                case 'CLOSE_DIFF':
                case 'TOGGLE_COMPARE':
                    if (!isCompareLayout(layout)) {
                        return { kind: 'closed' };
                    }
                    return event.type === 'CLOSE_DIFF'
                        ? { kind: 'closed' }
                        : diffModalState.kind === 'open'
                            ? createDiffModalState('open', primaryPane, comparePane)
                            : { kind: 'closed' };
                default:
                    if (diffModalState.kind !== 'open') {
                        return diffModalState;
                    }
                    return createDiffModalState('open', primaryPane, comparePane);
            }
        }

        function syncLegacyMachineFlags() {
            compareModeEnabled = isCompareLayout(queryPageState.layout);
            compareSidebarOpen = compareModeEnabled
                && queryPageState.layout.mode === 'compare'
                && queryPageState.layout.sidebar === 'open';
            compareQuerySeeded = queryPageState.compareQuerySeeded;
            primaryExplanationPending = queryPageState.primaryPane.kind === 'loading';
        }

        function syncLegacyExplanationCache(paneKey: PaneKey) {
            var paneState = getPaneState(paneKey);
            var paneSnapshot = getPaneSnapshot(getPaneMachineState(paneKey));
            paneState.latestExplanation = paneSnapshot ? paneSnapshot.rawContent : '';
            paneState.latestExplanationFormat = paneSnapshot ? paneSnapshot.responseFormat : 'text';
        }

        function dispatchQueryPageEvent(event: QueryPageEvent) {
            var currentInputs = collectCurrentInputs();
            if (!queryPageState) {
                queryPageState = createInitialQueryPageState();
            }
            if (event.type === 'HYDRATE') {
                queryPageState = {
                    lifecycle: 'ready',
                    layout: { mode: 'single' },
                    primaryPane: event.primaryExplanation
                        ? createReadyPaneState(event.primaryExplanation, 'primary', currentInputs)
                        : <PaneState>{ kind: 'empty' },
                    comparePane: { kind: 'inactive' },
                    diffModal: { kind: 'closed' },
                    inputs: currentInputs,
                    compareQuerySeeded: false
                };
            } else if (event.type === 'TOGGLE_COMPARE') {
                var compareEnabled = isCompareLayout(queryPageState.layout);
                var nextLayout: LayoutState = compareEnabled ? { mode: 'single' } : { mode: 'compare', sidebar: 'closed' };
                var nextComparePane: PaneState = compareEnabled ? { kind: 'inactive' } : queryPageState.comparePane;
                if (!compareEnabled && nextComparePane.kind === 'inactive') {
                    nextComparePane = { kind: 'empty' };
                }
                queryPageState = {
                    lifecycle: 'ready',
                    layout: nextLayout,
                    primaryPane: reducePaneState(queryPageState.primaryPane, 'primary', event, currentInputs, nextLayout),
                    comparePane: nextComparePane,
                    diffModal: compareEnabled ? { kind: 'closed' } : queryPageState.diffModal,
                    inputs: currentInputs,
                    compareQuerySeeded: compareEnabled ? false : queryPageState.compareQuerySeeded
                };
            } else if (event.type === 'TOGGLE_SIDEBAR') {
                queryPageState = {
                    lifecycle: 'ready',
                    layout: queryPageState.layout.mode === 'compare'
                        ? {
                            mode: 'compare',
                            sidebar: queryPageState.layout.sidebar === 'open' ? 'closed' : 'open'
                        }
                        : queryPageState.layout,
                    primaryPane: reducePaneState(queryPageState.primaryPane, 'primary', event, currentInputs, queryPageState.layout),
                    comparePane: reducePaneState(queryPageState.comparePane, 'compare', event, currentInputs, queryPageState.layout),
                    diffModal: reduceDiffModalState(
                        queryPageState.diffModal,
                        event,
                        queryPageState.primaryPane,
                        queryPageState.comparePane,
                        queryPageState.layout,
                        currentInputs
                    ),
                    inputs: currentInputs,
                    compareQuerySeeded: queryPageState.compareQuerySeeded
                };
            } else {
                var nextPrimaryPane = reducePaneState(queryPageState.primaryPane, 'primary', event, currentInputs, queryPageState.layout);
                var nextComparePaneForEvent = reducePaneState(queryPageState.comparePane, 'compare', event, currentInputs, queryPageState.layout);
                queryPageState = {
                    lifecycle: 'ready',
                    layout: queryPageState.layout,
                    primaryPane: nextPrimaryPane,
                    comparePane: nextComparePaneForEvent,
                    diffModal: reduceDiffModalState(
                        queryPageState.diffModal,
                        event,
                        nextPrimaryPane,
                        nextComparePaneForEvent,
                        queryPageState.layout,
                        currentInputs
                    ),
                    inputs: currentInputs,
                    compareQuerySeeded: queryPageState.compareQuerySeeded
                };
            }
            syncLegacyMachineFlags();
            syncLegacyExplanationCache('primary');
            syncLegacyExplanationCache('compare');
            renderQueryPageState();
        }

        /**
         * Populate reasonable default name space declarations into the query text area.
         * The server has provided the declaration text in hidden elements.
         */
        export function loadNamespaces() {
            function toggleNamespaces() {
                workbench.query.setQueryValue(namespaces.text());
                currentQueryLn = queryLn;
            }

            var query: string = workbench.query.getQueryValue();
            var queryLn = $('#queryLn').val();
            var namespaces = $('#' + queryLn + '-namespaces');
            var last = $('#' + currentQueryLn + '-namespaces');
            if (namespaces.length) {
                if (!query || query.trim().length == 0) {
                    toggleNamespaces();
                }
                if (last.length && (query == last.text())) {
                    toggleNamespaces();
                }
            }
        }

        /**
         *Fires when the query language is changed
         */
        export function onQlChange() {
            workbench.query.loadNamespaces();
            workbench.query.updateYasqe();
        }
        /**
         * Invoked by the "clear" button. After confirming with the user,
         * clears the query text and loads the current repository and query
         * language name space declarations.
         */
        export function resetNamespaces() {
            if (confirm('Click OK to clear the current query text and replace' +
                'it with the ' + $('#queryLn').val() +
                ' namespace declarations.')) {
                workbench.query.setQueryValue('');
                workbench.query.loadNamespaces();
            }
        }

        /**
         * Clear any contents of the save feedback field.
         */
        export function clearFeedback() {
            $('#save-feedback').removeClass().text('');
        }

        /**
         * Clear the save feedback field, and look at the contents of the query name
         * field. Disables the save button if the field doesn't satisfy a given regular
         * expression. With a delay of 200 msec, to give enough time after
         * the event for the document to have changed. (Workaround for annoying browser
         * behavior.)
         */
        export function handleNameChange() {
            setTimeout(function disableSaveIfNotValidName() {
                $('#save').prop('disabled',
                    !/^[- \w]{1,32}$/.test($('#query-name').val()));
                workbench.query.clearFeedback();
            }, 0);
        }

        interface AjaxSaveResponse {
            accessible: boolean;
            existed: boolean;
            written: boolean;
        }

        interface AjaxExplainResponse {
            content: string;
            format: string;
            error: string;
        }

        interface DiffRow {
            marker: string;
            text: string;
            type: string;
        }

        function getPaneState(paneKey?: string): ExplanationPaneState {
            return paneKey === 'compare' ? comparePaneState : primaryPaneState;
        }

        function getPaneQueryEditor(paneKey?: string): YASQE_Instance {
            return paneKey === 'compare' ? compareYasqe : yasqe;
        }

        function setPaneQueryEditor(paneKey: string, editor: YASQE_Instance) {
            if (paneKey === 'compare') {
                compareYasqe = editor;
                return;
            }
            yasqe = editor;
        }

        function getPaneQuerySelector(paneKey?: string): string {
            return '#' + getPaneState(paneKey).queryId;
        }

        function getPanePersistedQueryStorageKey(paneKey: string): string {
            return paneKey === 'compare'
                ? 'yasqe_query-compare-pane_queryVal'
                : 'yasqe_query-primary-pane_queryVal';
        }

        function clearPanePersistedQuery(paneKey: string): void {
            var storageKey = getPanePersistedQueryStorageKey(paneKey);
            try {
                window.localStorage.removeItem(storageKey);
            } catch (e) {
                // Ignore browsers where storage access is unavailable.
            }
            try {
                window.sessionStorage.removeItem(storageKey);
            } catch (e) {
                // Ignore browsers where storage access is unavailable.
            }
        }

        function persistPrimaryQueryEditorValue(queryEditor: YASQE_Instance): void {
            if (!queryEditor) {
                return;
            }
            queryEditor.save();
            (<HTMLTextAreaElement>document.getElementById(primaryPaneState.queryId)).value = queryEditor.getValue();
            if (YASQE.storeQuery) {
                YASQE.storeQuery(queryEditor);
            }
        }

        function getWorkbenchCookiePath(): string {
            var pathSegments = window.location.pathname.split('/');
            return pathSegments.length > 1 && pathSegments[1]
                ? '/' + pathSegments[1]
                : '/';
        }

        function setWorkbenchCookie(name: string, value: string): void {
            document.cookie = name + '=' + encodeURIComponent(value || '') + '; path=' + getWorkbenchCookiePath();
        }

        function clearWorkbenchCookie(name: string): void {
            document.cookie = name + '=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=' + getWorkbenchCookiePath();
        }

        function persistPrimaryQueryValue(): void {
            setWorkbenchCookie('query', getPaneRawQueryValue('primary'));
            clearWorkbenchCookie('ref');
        }

        function getPaneRawQueryValue(paneKey?: string): string {
            var queryEditor = getPaneQueryEditor(paneKey);
            if (queryEditor) {
                return queryEditor.getValue();
            }
            return <string>($(getPaneQuerySelector(paneKey)).val() || '');
        }

        function getPaneQueryValue(paneKey?: string): string {
            return $.trim(getPaneRawQueryValue(paneKey));
        }

        function setPaneQueryValue(paneKey: string, queryString: string): void {
            var normalizedQuery = queryString || '';
            var queryEditor = getPaneQueryEditor(paneKey);
            if (queryEditor) {
                queryEditor.setValue(normalizedQuery);
                return;
            }
            $(getPaneQuerySelector(paneKey)).val(normalizedQuery);
        }

        function savePaneQuery(paneKey: string) {
            var queryEditor = getPaneQueryEditor(paneKey);
            if (queryEditor) {
                queryEditor.save();
            }
        }

        function clearExplainSelection() {
            $('#explain').val('');
            $('#explain-level').val('Optimized');
        }

        function getPaneDisplayExplanation(paneState: PaneState): StableExplanation {
            if (!paneState) {
                return null;
            }
            if (paneState.kind === 'ready') {
                return paneState.explanation;
            }
            if (paneState.kind === 'loading' || paneState.kind === 'error') {
                return paneState.previous || null;
            }
            return null;
        }

        function getPaneStatusMessage(paneState: PaneState): string {
            if (!paneState) {
                return '';
            }
            if (paneState.kind === 'loading' && paneState.mode === 'initial') {
                return 'Loading explanation...';
            }
            if (paneState.kind === 'ready' && paneState.freshness === 'stale') {
                return 'Explanation is stale. Re-run to refresh.';
            }
            if (paneState.kind === 'error') {
                return paneState.message;
            }
            return '';
        }

        function getPaneStatusClassName(paneState: PaneState): string {
            if (!paneState) {
                return '';
            }
            if (paneState.kind === 'loading' && paneState.mode === 'initial') {
                return 'query-explanation-status--loading';
            }
            if (paneState.kind === 'ready' && paneState.freshness === 'stale') {
                return 'query-explanation-status--stale';
            }
            if (paneState.kind === 'error') {
                return 'query-explanation-status--error';
            }
            return '';
        }

        function getPaneOverlayMessage(paneState: PaneState): string {
            if (paneState && paneState.kind === 'loading' && paneState.mode === 'refresh') {
                return 'Refreshing explanation...';
            }
            return '';
        }

        function hasPrimaryExplanation(): boolean {
            return !!getPaneDisplayExplanation(queryPageState.primaryPane);
        }

        function updateDownloadButtonState() {
            $('#download-explanation').prop(
                'disabled',
                !(queryPageState.primaryPane.kind === 'ready' && queryPageState.primaryPane.freshness === 'current')
            );
        }

        function syncPrimaryExplanationControls() {
            var primaryPaneMachineState = queryPageState.primaryPane;
            var primaryControlsVisible = compareModeEnabled || primaryPaneMachineState.kind !== 'empty';
            var primaryActionsDisabled = primaryPaneMachineState.kind === 'loading' || activeComparePendingRequests > 0;
            $('#query-explanation-controls-row').toggle(primaryControlsVisible);
            $('#primary-explain-settings').toggle(primaryControlsVisible);
            $('#primary-explain-repeat-controls').toggle(
                primaryPaneMachineState.kind !== 'empty'
            );
            $('#download-explanation').toggle(primaryControlsVisible);
            $('#compare-toggle').toggle(compareModeEnabled || primaryPaneMachineState.kind !== 'empty');
            $('#rerun-explanation').prop('disabled', primaryActionsDisabled);
            $('#explain-trigger').prop('disabled', primaryActionsDisabled);
        }

        function syncCompareSidebarState() {
            $('body').toggleClass('query-compare-mode', compareModeEnabled);
            $('body').toggleClass('query-compare-nav-open', compareModeEnabled && compareSidebarOpen);

            var sidebarToggle = $('#query-sidebar-toggle');
            var navigationTransform = '';
            var queryWorkspaceTransform = '';
            var sidebarToggleTransform = '';
            if (!compareModeEnabled) {
                $('#navigation').css('transform', navigationTransform);
                $('#title_heading, #noscript-message, .query-form').css('transform', queryWorkspaceTransform);
                sidebarToggle.css('transform', sidebarToggleTransform);
                sidebarToggle
                    .hide()
                    .removeClass('query-sidebar-toggle--nav-open')
                    .attr('aria-hidden', 'true')
                    .attr('tabindex', '-1');
                return;
            }

            navigationTransform = compareSidebarOpen ? 'translateX(0)' : 'translateX(-220px)';
            queryWorkspaceTransform = compareSidebarOpen ? 'translateX(184px)' : 'translateX(0)';
            sidebarToggleTransform = compareSidebarOpen ? 'translateX(192px)' : 'translateX(0)';
            $('#navigation').css('transform', navigationTransform);
            $('#title_heading, #noscript-message, .query-form').css('transform', queryWorkspaceTransform);
            sidebarToggle.css('transform', sidebarToggleTransform);

            var label = compareSidebarOpen
                ? <string>sidebarToggle.attr('data-hide-label')
                : <string>sidebarToggle.attr('data-show-label');
            sidebarToggle
                .show()
                .toggleClass('query-sidebar-toggle--nav-open', compareSidebarOpen)
                .attr('aria-hidden', 'false')
                .attr('aria-label', label)
                .attr('title', label)
                .removeAttr('tabindex');
        }

        function lockExplanationDimensions(paneKey?: string) {
            var paneState = getPaneState(paneKey);
            var explanation = $('#' + paneState.explanationId);
            var dotView = $('#' + paneState.dotViewId);
            var jsonView = $('#' + paneState.jsonViewId);
            var currentHeight = explanation.outerHeight();
            var currentWidth = explanation.outerWidth();
            if (currentHeight) {
                explanation.css('min-height', currentHeight + 'px');
                dotView.css('min-height', currentHeight + 'px');
                jsonView.css('min-height', currentHeight + 'px');
            }
            if (currentWidth) {
                explanation.css('min-width', currentWidth + 'px');
                dotView.css('min-width', currentWidth + 'px');
                jsonView.css('min-width', currentWidth + 'px');
            }
        }

        function clearExplanationDimensionLock(paneKey?: string) {
            var paneState = getPaneState(paneKey);
            var explanation = $('#' + paneState.explanationId);
            var dotView = $('#' + paneState.dotViewId);
            var jsonView = $('#' + paneState.jsonViewId);
            explanation.css('min-height', '');
            explanation.css('min-width', '');
            dotView.css('min-height', '');
            dotView.css('min-width', '');
            jsonView.css('min-height', '');
            jsonView.css('min-width', '');
        }

        function destroyDotPanZoom(paneKey?: string) {
            var paneState = getPaneState(paneKey);
            if (paneState.dotPanZoomInstance && typeof paneState.dotPanZoomInstance.destroy === 'function') {
                paneState.dotPanZoomInstance.destroy();
            }
            paneState.dotPanZoomInstance = null;
        }

        function clearExplainButtonViewportRestoreState(paneKey?: string) {
            var paneState = getPaneState(paneKey);
            paneState.explainButtonViewportTopBeforeRequest = null;
            paneState.explainButtonIdBeforeRequest = '';
        }

        function setExplainButtonsDisabled(disabled: boolean) {
            $('#explain-trigger').prop('disabled', disabled);
            $('#rerun-explanation').prop('disabled', disabled);
        }

        function hideExplainCancelButtons() {
            $('.query-explain-cancel')
                .removeClass('query-explain-cancel--visible')
                .attr('aria-hidden', 'true')
                .prop('disabled', true);
        }

        function showExplainCancelButton(buttonId: string) {
            var controlIds = workbench.queryCancelPolicy.getExplainControlIds(buttonId);
            hideExplainCancelButtons();
            if (!controlIds.cancelId) {
                return;
            }
            $('#' + controlIds.cancelId)
                .addClass('query-explain-cancel--visible')
                .attr('aria-hidden', 'false')
                .prop('disabled', false);
        }

        function hideExplainSpinners() {
            $('.query-explain-spinner')
                .removeClass('query-explain-spinner--visible')
                .attr('aria-hidden', 'true');
            hideExplainCancelButtons();
            explainSpinnerVisibleSince = 0;
        }

        function showExplainSpinner(buttonId: string) {
            var controlIds = workbench.queryCancelPolicy.getExplainControlIds(buttonId);
            hideExplainSpinners();
            if (!controlIds.spinnerId) {
                explainSpinnerTargetId = '';
                return;
            }
            explainSpinnerTargetId = buttonId;
            $('#' + controlIds.spinnerId)
                .addClass('query-explain-spinner--visible')
                .attr('aria-hidden', 'false');
            showExplainCancelButton(buttonId);
            explainSpinnerVisibleSince = Date.now();
        }

        function clearExplainSpinnerDelayTimeout() {
            if (explainSpinnerDelayTimeoutId !== null) {
                window.clearTimeout(explainSpinnerDelayTimeoutId);
                explainSpinnerDelayTimeoutId = null;
            }
        }

        function clearExplainSpinnerHideTimeout() {
            if (explainSpinnerHideTimeoutId !== null) {
                window.clearTimeout(explainSpinnerHideTimeoutId);
                explainSpinnerHideTimeoutId = null;
            }
        }

        function beginComparePrimaryExplainWaitState(buttonId?: string) {
            var targetButtonId = buttonId || '';
            clearExplainSpinnerDelayTimeout();
            clearExplainSpinnerHideTimeout();
            hideExplainSpinners();
            hideExplainCancelButtons();
            explainSpinnerTargetId = '';
            if (!workbench.queryCancelPolicy.getExplainControlIds(targetButtonId).buttonId) {
                return;
            }
            explainSpinnerTargetId = targetButtonId;
            explainSpinnerDelayTimeoutId = window.setTimeout(function() {
                if (!workbench.queryCancelPolicy.shouldShowComparePrimaryWaitState(
                        targetButtonId,
                        activeComparePendingRequests,
                        !!activeCompareRequestId,
                        explainSpinnerTargetId
                )) {
                    return;
                }
                explainSpinnerDelayTimeoutId = null;
                showExplainSpinner(targetButtonId);
            }, 1000);
        }

        function finishComparePrimaryExplainWaitState(requestId: number) {
            clearExplainSpinnerDelayTimeout();
            clearExplainSpinnerHideTimeout();
            if (!explainSpinnerVisibleSince) {
                hideExplainSpinners();
                explainSpinnerTargetId = '';
                return;
            }
            var spinnerTargetId = explainSpinnerTargetId;
            var remainingSpinnerTime = 1000 - (Date.now() - explainSpinnerVisibleSince);
            if (remainingSpinnerTime > 0) {
                explainSpinnerHideTimeoutId = window.setTimeout(function() {
                    if (requestId !== activeCompareRequestId || spinnerTargetId !== explainSpinnerTargetId) {
                        return;
                    }
                    explainSpinnerHideTimeoutId = null;
                    hideExplainSpinners();
                    explainSpinnerTargetId = '';
                }, remainingSpinnerTime);
                return;
            }
            hideExplainSpinners();
            explainSpinnerTargetId = '';
        }

        function beginExplainRequest(buttonId: string, signature: RequestSignature): number {
            activeExplainRequestId = signature.requestId;
            activePrimaryRequestSignature = signature;
            primaryExplanationPending = true;
            explainSpinnerTargetId = buttonId;
            clearExplainSpinnerDelayTimeout();
            clearExplainSpinnerHideTimeout();
            hideExplainSpinners();
            hideExplainCancelButtons();
            setExplainButtonsDisabled(true);
            explainSpinnerDelayTimeoutId = window.setTimeout(function() {
                if (!activePrimaryRequestSignature || !signaturesMatch(activePrimaryRequestSignature, signature)) {
                    return;
                }
                explainSpinnerDelayTimeoutId = null;
                dispatchQueryPageEvent({ type: 'SPINNER_DELAY_ELAPSED', signature: signature });
                showExplainSpinner(buttonId);
            }, 1000);
            return signature.requestId;
        }

        function finishExplainRequest(requestId: number) {
            if (requestId !== activeExplainRequestId) {
                return;
            }
            primaryExplanationPending = false;
            syncPrimaryExplanationControls();
            setExplainButtonsDisabled(false);
            clearExplainSpinnerDelayTimeout();
            clearExplainSpinnerHideTimeout();
            if (!explainSpinnerVisibleSince) {
                hideExplainSpinners();
                explainSpinnerTargetId = '';
                return;
            }
            var spinnerTargetId = explainSpinnerTargetId;
            var remainingSpinnerTime = 1000 - (Date.now() - explainSpinnerVisibleSince);
            if (remainingSpinnerTime > 0) {
                explainSpinnerHideTimeoutId = window.setTimeout(function() {
                    if ((activePrimaryRequestSignature && requestId !== activeExplainRequestId)
                            || spinnerTargetId !== explainSpinnerTargetId) {
                        return;
                    }
                    explainSpinnerHideTimeoutId = null;
                    hideExplainSpinners();
                    explainSpinnerTargetId = '';
                }, remainingSpinnerTime);
                return;
            }
            hideExplainSpinners();
            explainSpinnerTargetId = '';
        }

        function getExplainTriggerButtonElement(buttonId?: string): HTMLInputElement {
            if (buttonId) {
                return <HTMLInputElement>document.getElementById(buttonId);
            }
            var activeElement = <HTMLInputElement>document.activeElement;
            if (activeElement
                    && (activeElement.id === 'explain-trigger' || activeElement.id === 'rerun-explanation')) {
                return activeElement;
            }
            return <HTMLInputElement>document.getElementById('explain-trigger');
        }

        function captureExplainButtonViewportTop(paneKey?: string, buttonId?: string) {
            var explainButton = <HTMLElement>document.getElementById(buttonId)
                || getExplainTriggerButtonElement(buttonId);
            if (!explainButton) {
                clearExplainButtonViewportRestoreState(paneKey);
                return;
            }
            var paneState = getPaneState(paneKey);
            paneState.explainButtonViewportTopBeforeRequest = explainButton.getBoundingClientRect().top;
            paneState.explainButtonIdBeforeRequest = explainButton.id;
        }

        function restoreExplainButtonViewportTopIfNeeded(paneKey?: string) {
            var paneState = getPaneState(paneKey);
            if (paneState.explainButtonViewportTopBeforeRequest === null) {
                return;
            }
            var explainButton = paneState.explainButtonIdBeforeRequest
                    ? <HTMLElement>document.getElementById(paneState.explainButtonIdBeforeRequest)
                    : getExplainTriggerButtonElement();
            if (!explainButton) {
                clearExplainButtonViewportRestoreState(paneKey);
                return;
            }
            window.requestAnimationFrame(function() {
                var currentButtonTop = explainButton.getBoundingClientRect().top;
                var scrollDelta = currentButtonTop - paneState.explainButtonViewportTopBeforeRequest;
                if (Math.abs(scrollDelta) > 1) {
                    window.scrollBy(0, scrollDelta);
                }
                clearExplainButtonViewportRestoreState(paneKey);
            });
        }

        function setExplanationDisplayMode(paneKey: string, format: string) {
            var paneState = getPaneState(paneKey);
            var explanation = $('#' + paneState.explanationId);
            var dotView = $('#' + paneState.dotViewId);
            var jsonView = $('#' + paneState.jsonViewId);
            if (format === 'dot') {
                explanation.hide();
                jsonView.hide();
                dotView.show();
                return;
            }
            if (format === 'json') {
                explanation.hide();
                dotView.hide();
                jsonView.show();
                destroyDotPanZoom(paneKey);
            } else {
                explanation.show();
                dotView.hide();
                jsonView.hide();
                destroyDotPanZoom(paneKey);
            }
        }

        function applyDotPanZoom(paneKey: string, svgElement: SVGElement) {
            destroyDotPanZoom(paneKey);
            if (typeof svgPanZoom === 'undefined') {
                return;
            }
            getPaneState(paneKey).dotPanZoomInstance = svgPanZoom(svgElement, {
                zoomEnabled: true,
                controlIconsEnabled: true,
                fit: true,
                center: true,
                minZoom: 0.2,
                maxZoom: 20
            });
        }

        function clearRenderedExplanation(paneKey: string, pendingFormat?: string) {
            var paneState = getPaneState(paneKey);
            lockExplanationDimensions(paneKey);
            var explanation = $('#' + paneState.explanationId);
            explanation.text('');
            var normalizedFormat = (pendingFormat || paneState.latestExplanationFormat || 'text').toLowerCase();
            explanation.attr('data-format', normalizedFormat);
            paneState.latestExplanation = '';
            paneState.latestExplanationFormat = normalizedFormat;
            if (paneKey !== 'compare') {
                updateDownloadButtonState();
                syncPrimaryExplanationControls();
            }
            destroyDotPanZoom(paneKey);
            $('#' + paneState.dotViewId).empty();
            $('#' + paneState.jsonViewId).empty();
            setExplanationDisplayMode(paneKey, normalizedFormat);
        }

        function showExplainError(paneKey: string, message: string) {
            var paneState = getPaneState(paneKey);
            var errorMessage = message || 'Explain request failed.';
            clearRenderedExplanation(paneKey, 'text');
            $('#' + paneState.errorId).text(errorMessage);
            $('#' + paneState.explanationId).show().text(errorMessage).attr('data-format', 'text');
            $('#' + paneState.dotViewId).hide().empty();
            $('#' + paneState.jsonViewId).hide().empty();
            destroyDotPanZoom(paneKey);
            if (paneKey !== 'compare') {
                syncPrimaryExplanationControls();
            }
            restoreExplainButtonViewportTopIfNeeded(paneKey);
            clearExplanationDimensionLock(paneKey);
        }

        function renderDotView(paneKey: string, explanationText: string, format: string) {
            var paneState = getPaneState(paneKey);
            var dotView = $('#' + paneState.dotViewId);
            if (format === 'dot') {
                var paneMachineState = getPaneMachineState(<PaneKey>paneKey);
                var displayExplanation = getPaneDisplayExplanation(paneMachineState);
                var explanationContentKey = getStableExplanationContentKey(displayExplanation);
                var explanationKey = getStableExplanationKey(displayExplanation);
                setExplanationDisplayMode(paneKey, 'dot');
                if (!explanationText) {
                    dotView.empty().show();
                    pendingDotRenderKeys[paneKey] = '';
                    restoreExplainButtonViewportTopIfNeeded(paneKey);
                    return;
                }
                if (explanationContentKey && pendingDotRenderKeys[paneKey] === explanationContentKey) {
                    dotView.show();
                    return;
                }
                pendingDotRenderKeys[paneKey] = explanationContentKey;
                dotView.html('<div>Rendering DOT graph...</div>').show();
                if (typeof Viz === 'undefined') {
                    dotView.html('<div class="error">Graphviz visualizer script not loaded.</div>');
                    if (displayExplanation) {
                        dispatchQueryPageEvent({
                            type: 'DOT_RENDER_FAIL',
                            pane: <PaneKey>paneKey,
                            explanationKey: explanationKey
                        });
                    }
                    return;
                }
                if (!vizRenderer) {
                    vizRenderer = new Viz();
                }
                vizRenderer.renderSVGElement(explanationText).then(function(svgElement: SVGElement) {
                    if (pendingDotRenderKeys[paneKey] !== explanationContentKey) {
                        return;
                    }
                    $(svgElement).css({
                        width: '100%',
                        height: '100%',
                        maxWidth: '100%',
                        maxHeight: 'none',
                        display: 'block'
                    });
                    dotView.empty().append(svgElement).show();
                    applyDotPanZoom(paneKey, svgElement);
                    if (displayExplanation) {
                        dispatchQueryPageEvent({
                            type: 'DOT_RENDER_OK',
                            pane: <PaneKey>paneKey,
                            explanationKey: explanationKey
                        });
                    }
                    restoreExplainButtonViewportTopIfNeeded(paneKey);
                }).catch(function() {
                    if (pendingDotRenderKeys[paneKey] !== explanationContentKey) {
                        return;
                    }
                    vizRenderer = new Viz();
                    destroyDotPanZoom(paneKey);
                    dotView.html('<div class="error">Unable to render DOT graph.</div>');
                    if (displayExplanation) {
                        dispatchQueryPageEvent({
                            type: 'DOT_RENDER_FAIL',
                            pane: <PaneKey>paneKey,
                            explanationKey: explanationKey
                        });
                    }
                    restoreExplainButtonViewportTopIfNeeded(paneKey);
                });
                return;
            }
            destroyDotPanZoom(paneKey);
            pendingDotRenderKeys[paneKey] = '';
            dotView.hide().empty();
        }

        function isJsonExpandable(value: any): boolean {
            return value !== null && typeof value === 'object';
        }

        function getJsonSummary(value: any): string {
            if (Array.isArray(value)) {
                return '[ ' + value.length + ' ]';
            }
            var keyCount = Object.keys(value).length;
            return '{ ' + keyCount + ' }';
        }

        function formatJsonKey(key: string): string {
            if (key && key.charAt(0) === '[') {
                return key;
            }
            return '"' + key + '"';
        }

        function formatJsonArrayEntryKey(index: number, arrayEntry: any): string {
            var indexLabel = '[' + index + ']';
            if (!arrayEntry || typeof arrayEntry !== 'object') {
                return indexLabel;
            }
            var entryType = arrayEntry.type;
            if (typeof entryType !== 'string' || entryType.length === 0) {
                return indexLabel;
            }
            return indexLabel + ' ' + entryType;
        }

        function parseNumericJsonValue(value: any): number {
            if (typeof value === 'number' && !isNaN(value) && isFinite(value)) {
                return value;
            }
            if (typeof value !== 'string') {
                return null;
            }
            var parsedValue = Number(value);
            if (isNaN(parsedValue) || !isFinite(parsedValue)) {
                return null;
            }
            return parsedValue;
        }

        function computePlanEntryPercentages(plans: any[]): any[] {
            var planTimes: any[] = [];
            var totalTimeActual = 0;
            for (var i = 0; i < plans.length; i++) {
                var planEntry = plans[i];
                var planTime = null;
                if (planEntry && typeof planEntry === 'object') {
                    planTime = parseNumericJsonValue(planEntry.totalTimeActual);
                }
                planTimes.push(planTime);
                if (typeof planTime === 'number' && planTime > 0) {
                    totalTimeActual += planTime;
                }
            }

            if (totalTimeActual <= 0) {
                return [];
            }

            var percentages: any[] = [];
            for (var j = 0; j < planTimes.length; j++) {
                var currentPlanTime = planTimes[j];
                if (typeof currentPlanTime === 'number' && currentPlanTime >= 0) {
                    percentages.push((currentPlanTime / totalTimeActual) * 100);
                } else {
                    percentages.push(null);
                }
            }
            return percentages;
        }

        function formatPercentage(percentage: number): string {
            return percentage.toFixed(1) + '%';
        }

        function createJsonScalarElement(value: any): HTMLElement {
            var valueElement = document.createElement('span');
            valueElement.className = 'query-json-node__value';

            if (value === null) {
                valueElement.className += ' query-json-node__value--null';
                valueElement.textContent = 'null';
            } else if (typeof value === 'string') {
                valueElement.className += ' query-json-node__value--string';
                valueElement.textContent = '"' + value + '"';
            } else if (typeof value === 'number') {
                valueElement.className += ' query-json-node__value--number';
                valueElement.textContent = value.toString();
            } else if (typeof value === 'boolean') {
                valueElement.className += ' query-json-node__value--boolean';
                valueElement.textContent = value ? 'true' : 'false';
            } else {
                valueElement.className += ' query-json-node__value--other';
                valueElement.textContent = String(value);
            }

            return valueElement;
        }

        function createJsonTreeNode(value: any, key: string, depth: number, percentageOfPlansTotal?: number): HTMLElement {
            var node = document.createElement('div');
            node.className = 'query-json-node';

            var line = document.createElement('div');
            line.className = 'query-json-node__line';
            node.appendChild(line);

            var expandable = isJsonExpandable(value);
            var toggle: HTMLButtonElement = null;

            if (expandable) {
                toggle = document.createElement('button');
                toggle.type = 'button';
                toggle.className = 'query-json-node__toggle';
                line.appendChild(toggle);
            } else {
                var spacer = document.createElement('span');
                spacer.className = 'query-json-node__toggle query-json-node__toggle--spacer';
                spacer.setAttribute('aria-hidden', 'true');
                line.appendChild(spacer);
            }

            if (key) {
                var keyElement = document.createElement('span');
                keyElement.className = 'query-json-node__key';
                keyElement.textContent = formatJsonKey(key);
                line.appendChild(keyElement);

                var separatorElement = document.createElement('span');
                separatorElement.className = 'query-json-node__label';
                separatorElement.textContent = ':';
                line.appendChild(separatorElement);
            }

            if (typeof percentageOfPlansTotal === 'number') {
                var percentageElement = document.createElement('span');
                percentageElement.className = 'query-json-node__percentage';
                percentageElement.textContent = '(' + formatPercentage(percentageOfPlansTotal) + ')';
                line.appendChild(percentageElement);
            }

            if (expandable) {
                var summaryElement = document.createElement('span');
                summaryElement.className = 'query-json-node__summary';
                summaryElement.textContent = getJsonSummary(value);
                line.appendChild(summaryElement);

                var childrenElement = document.createElement('div');
                childrenElement.className = 'query-json-node__children';
                node.appendChild(childrenElement);

                var expandedByDefault = depth <= 1;
                if (!expandedByDefault) {
                    node.classList.add('query-json-node--collapsed');
                }
                toggle.setAttribute('aria-expanded', expandedByDefault ? 'true' : 'false');
                toggle.textContent = expandedByDefault ? '▾' : '▸';

                if (Array.isArray(value)) {
                    var arrayEntryPercentages: any[] = [];
                    if (key === 'plans') {
                        arrayEntryPercentages = computePlanEntryPercentages(value);
                    }
                    for (var index = 0; index < value.length; index++) {
                        childrenElement.appendChild(createJsonTreeNode(
                            value[index],
                            formatJsonArrayEntryKey(index, value[index]),
                            depth + 1,
                            arrayEntryPercentages[index]
                        ));
                    }
                } else {
                    var objectKeys = Object.keys(value);
                    for (var i = 0; i < objectKeys.length; i++) {
                        var childKey = objectKeys[i];
                        childrenElement.appendChild(createJsonTreeNode(value[childKey], childKey, depth + 1));
                    }
                }

                toggle.addEventListener('click', function() {
                    var isCollapsed = node.classList.toggle('query-json-node--collapsed');
                    var expanded = !isCollapsed;
                    toggle.setAttribute('aria-expanded', expanded ? 'true' : 'false');
                    toggle.textContent = expanded ? '▾' : '▸';
                });
            } else {
                line.appendChild(createJsonScalarElement(value));
            }

            return node;
        }

        function renderJsonExplanationTree(explanationJson: any): HTMLElement {
            var treeElement = document.createElement('div');
            treeElement.className = 'query-json-tree';
            treeElement.appendChild(createJsonTreeNode(explanationJson, '', 0));
            return treeElement;
        }

        function renderJsonView(paneKey: string, explanationText: string, format: string) {
            var paneState = getPaneState(paneKey);
            var jsonView = $('#' + paneState.jsonViewId);
            if (format !== 'json') {
                jsonView.hide().empty();
                return;
            }

            setExplanationDisplayMode(paneKey, 'json');
            jsonView.empty().show();

            if (!explanationText) {
                restoreExplainButtonViewportTopIfNeeded(paneKey);
                return;
            }

            try {
                var explanationJson = JSON.parse(explanationText);
                jsonView.append(renderJsonExplanationTree(explanationJson));
            } catch (parseError) {
                jsonView.append($('<div class="error">Unable to parse JSON explanation. Showing raw content.</div>'));
                jsonView.append($('<pre class="query-json-view__raw"></pre>').text(explanationText));
            }

            restoreExplainButtonViewportTopIfNeeded(paneKey);
        }

        function renderExplanation(paneKey: string, explanationText: string, format: string) {
            var paneState = getPaneState(paneKey);
            var normalizedFormat = (format || 'text').toLowerCase();
            $('#' + paneState.explanationRowId).show();
            if (paneState.explanationControlsRowId) {
                $('#' + paneState.explanationControlsRowId).show();
            }
            if (normalizedFormat === 'dot' || normalizedFormat === 'json') {
                $('#' + paneState.explanationId).text('').attr('data-format', normalizedFormat);
            } else {
                $('#' + paneState.explanationId).text(explanationText).attr('data-format', normalizedFormat);
            }
            setExplanationDisplayMode(paneKey, normalizedFormat);
            paneState.latestExplanation = explanationText;
            paneState.latestExplanationFormat = normalizedFormat;
            if (paneKey !== 'compare') {
                updateDownloadButtonState();
                syncPrimaryExplanationControls();
            }
            renderDotView(paneKey, explanationText, normalizedFormat);
            renderJsonView(paneKey, explanationText, normalizedFormat);
            if (normalizedFormat === 'text') {
                restoreExplainButtonViewportTopIfNeeded(paneKey);
            }
            clearExplanationDimensionLock(paneKey);
        }

        function renderPanePresentation(paneKey: PaneKey) {
            var paneMachineState = getPaneMachineState(paneKey);
            var paneState = getPaneState(paneKey);
            var paneStatus = $('#' + paneState.statusId);
            var paneOverlay = $('#' + paneState.overlayId);
            var paneDisplayExplanation = getPaneDisplayExplanation(paneMachineState);
            var paneStatusMessage = getPaneStatusMessage(paneMachineState);
            var paneStatusClassName = getPaneStatusClassName(paneMachineState);
            var paneOverlayMessage = getPaneOverlayMessage(paneMachineState);
            var rowVisible = paneMachineState.kind !== 'inactive' && paneMachineState.kind !== 'empty';
            var renderContentKey = getStableExplanationContentKey(paneDisplayExplanation);

            $('#' + paneState.explanationRowId).toggle(rowVisible);
            if (!rowVisible) {
                paneStatus
                    .removeClass('query-explanation-status--visible query-explanation-status--loading query-explanation-status--stale query-explanation-status--error')
                    .text('');
                paneOverlay
                    .removeClass('query-explanation-overlay--visible')
                    .attr('aria-hidden', 'true')
                    .text('');
                $('#' + paneState.errorId).text('');
                clearRenderedExplanation(paneKey, queryPageState.inputs.explainFormat);
                lastRenderedExplanationKeys[paneKey] = '';
                clearExplanationDimensionLock(paneKey);
                return;
            }

            paneStatus
                .removeClass('query-explanation-status--visible query-explanation-status--loading query-explanation-status--stale query-explanation-status--error')
                .text('');
            if (paneStatusMessage) {
                paneStatus
                    .addClass('query-explanation-status--visible')
                    .addClass(paneStatusClassName)
                    .text(paneStatusMessage);
            }

            paneOverlay
                .toggleClass('query-explanation-overlay--visible', !!paneOverlayMessage)
                .attr('aria-hidden', paneOverlayMessage ? 'false' : 'true')
                .text(paneOverlayMessage);

            $('#' + paneState.errorId).text(paneMachineState.kind === 'error' ? paneMachineState.message : '');

            if (paneMachineState.kind === 'loading' && paneMachineState.mode === 'initial') {
                lastRenderedExplanationKeys[paneKey] = '';
                clearRenderedExplanation(paneKey, queryPageState.inputs.explainFormat);
                $('#' + paneState.explanationId)
                    .show()
                    .text('Loading explanation...')
                    .attr('data-format', 'text');
                setExplanationDisplayMode(paneKey, 'text');
                clearExplanationDimensionLock(paneKey);
                return;
            }

            if (paneMachineState.kind === 'error' && !paneDisplayExplanation) {
                lastRenderedExplanationKeys[paneKey] = '';
                clearRenderedExplanation(paneKey, 'text');
                $('#' + paneState.explanationId)
                    .show()
                    .text(paneMachineState.message)
                    .attr('data-format', 'text');
                setExplanationDisplayMode(paneKey, 'text');
                clearExplanationDimensionLock(paneKey);
                return;
            }

            if (!paneDisplayExplanation) {
                lastRenderedExplanationKeys[paneKey] = '';
                clearRenderedExplanation(paneKey, queryPageState.inputs.explainFormat);
                clearExplanationDimensionLock(paneKey);
                return;
            }

            if (lastRenderedExplanationKeys[paneKey] !== renderContentKey) {
                lastRenderedExplanationKeys[paneKey] = renderContentKey;
                renderExplanation(paneKey, paneDisplayExplanation.rawContent, paneDisplayExplanation.responseFormat);
            }
        }

        function renderQueryPageState() {
            if (!queryPageState) {
                return;
            }

            syncLegacyMachineFlags();

            $('#query-compare-layout').toggleClass('query-compare-layout--active', compareModeEnabled);
            $('#query-compare-controls').toggle(compareModeEnabled);
            $('#query-diff-modal')
                .toggleClass('query-diff-modal--open', queryPageState.diffModal.kind === 'open')
                .attr('aria-hidden', queryPageState.diffModal.kind === 'open' ? 'false' : 'true');

            renderPanePresentation('primary');
            renderPanePresentation('compare');

            updateDownloadButtonState();
            syncPrimaryExplanationControls();
            syncCompareSidebarState();
            updateCompareActionState();

            if (queryPageState.diffModal.kind === 'open') {
                renderDiffView('#query-diff-query', getPaneRawQueryValue('primary'), getPaneRawQueryValue('compare'));
                if (queryPageState.diffModal.explanation === 'ready'
                        && queryPageState.primaryPane.kind === 'ready'
                        && queryPageState.comparePane.kind === 'ready') {
                    renderDiffView(
                        '#query-diff-explanation',
                        queryPageState.primaryPane.explanation.rawContent,
                        queryPageState.comparePane.explanation.rawContent,
                        diffNotReadyLabel
                    );
                } else {
                    $('#query-diff-explanation').text(diffNotReadyLabel);
                }
            }
        }

        function getExplainErrorMessage(jqXHR: JQueryXHR, textStatus: string, errorThrown: string): string {
            var response: any = (<any>jqXHR).responseJSON;
            if (response && response.error) {
                return response.error;
            }
            var responseText = jqXHR.responseText;
            if (responseText) {
                try {
                    var parsedResponse = JSON.parse(responseText);
                    if (parsedResponse && parsedResponse.error) {
                        return parsedResponse.error;
                    }
                } catch (e) {
                    // fall through and return plain response text
                }
                return responseText;
            }
            if (textStatus == 'timeout') {
                return 'Timed out waiting for explanation response.';
            }
            if (errorThrown) {
                return 'Explain request failed: ' + errorThrown;
            }
            return 'Explain request failed.';
        }

        function serializeExplainFormData(queryValue: string, level: string, format: string, serverRequestId: string): string {
            var serializedForm: any[] = <any[]>$('form[action="query"]').serializeArray();
            var seenAction = false;
            var seenExplain = false;
            var seenFormat = false;
            var seenQuery = false;
            var seenExplainRequestId = false;
            for (var i = 0; i < serializedForm.length; i++) {
                if (serializedForm[i].name === 'action') {
                    serializedForm[i].value = 'explain';
                    seenAction = true;
                } else if (serializedForm[i].name === 'explain') {
                    serializedForm[i].value = level;
                    seenExplain = true;
                } else if (serializedForm[i].name === 'explain-format') {
                    serializedForm[i].value = format;
                    seenFormat = true;
                } else if (serializedForm[i].name === 'query') {
                    serializedForm[i].value = queryValue;
                    seenQuery = true;
                } else if (serializedForm[i].name === 'explain-request-id') {
                    serializedForm[i].value = serverRequestId;
                    seenExplainRequestId = true;
                }
            }
            if (!seenAction) {
                serializedForm.push({ name: 'action', value: 'explain' });
            }
            if (!seenExplain) {
                serializedForm.push({ name: 'explain', value: level });
            }
            if (!seenFormat) {
                serializedForm.push({ name: 'explain-format', value: format });
            }
            if (!seenQuery) {
                serializedForm.push({ name: 'query', value: queryValue });
            }
            if (!seenExplainRequestId) {
                serializedForm.push({ name: 'explain-request-id', value: serverRequestId });
            }
            return $.param(serializedForm);
        }

        function serializeCancelExplainFormData(serverRequestId: string): string {
            return $.param([
                { name: 'action', value: 'cancel-explain' },
                { name: 'explain-request-id', value: serverRequestId }
            ]);
        }

        function postCancelExplain(serverRequestId: string) {
            if (!serverRequestId) {
                return;
            }
            $.ajax({
                url: 'query',
                type: 'POST',
                data: serializeCancelExplainFormData(serverRequestId)
            });
        }

        function createStableExplanationFromResponse(
            signature: RequestSignature,
            response: AjaxExplainResponse,
            fallbackFormat: string
        ): StableExplanation {
            var responseFormat = getNormalizedExplainFormat(response.format || fallbackFormat || 'text');
            var explanationText = response.content || '';
            var explanationView: ExplanationView = 'text';
            if (responseFormat === 'json') {
                explanationView = 'jsonTree';
                if (explanationText) {
                    try {
                        JSON.parse(explanationText);
                    } catch (parseError) {
                        explanationView = 'jsonRawFallback';
                    }
                }
            } else if (responseFormat === 'dot') {
                explanationView = 'dotRendering';
            }
            return {
                queryHash: signature.queryHash,
                level: signature.level,
                requestedFormat: signature.format,
                responseFormat: responseFormat,
                view: explanationView,
                rawContent: explanationText
            };
        }

        function applyExplainResponseToPane(paneKey: string, signature: RequestSignature, response: AjaxExplainResponse, fallbackFormat: string) {
            if (response.error) {
                dispatchQueryPageEvent({
                    type: 'EXPLAIN_ERROR',
                    signature: signature,
                    message: response.error
                });
                return;
            }
            dispatchQueryPageEvent({
                type: 'EXPLAIN_SUCCESS',
                signature: signature,
                explanation: createStableExplanationFromResponse(signature, response, fallbackFormat)
            });
        }

        function ajaxExplain(signature: RequestSignature) {
            activeExplainJqXHR = $.ajax({
                url: 'query',
                type: 'POST',
                dataType: 'json',
                data: serializeExplainFormData(
                    getPaneRawQueryValue('primary'),
                    signature.level,
                    signature.format,
                    signature.serverRequestId
                ),
                error: function(jqXHR: JQueryXHR, textStatus: string, errorThrown: string) {
                    if (textStatus !== 'abort' && activePrimaryRequestSignature && signaturesMatch(activePrimaryRequestSignature, signature)) {
                        dispatchQueryPageEvent({
                            type: 'EXPLAIN_ERROR',
                            signature: signature,
                            message: getExplainErrorMessage(jqXHR, textStatus, errorThrown)
                        });
                    }
                },
                success: function(response: AjaxExplainResponse) {
                    if (!activePrimaryRequestSignature || !signaturesMatch(activePrimaryRequestSignature, signature)) {
                        return;
                    }
                    applyExplainResponseToPane('primary', signature, response, signature.format);
                },
                complete: function() {
                    if (!activePrimaryRequestSignature || !signaturesMatch(activePrimaryRequestSignature, signature)) {
                        return;
                    }
                    activePrimaryRequestSignature = null;
                    activeExplainJqXHR = null;
                    finishExplainRequest(signature.requestId);
                }
            });
        }

        function getExplanationDownloadMimeType(format: string): string {
            if (format === 'json') {
                return 'application/json';
            }
            if (format === 'dot') {
                return 'text/vnd.graphviz';
            }
            return 'text/plain';
        }

        function getExplanationDownloadExtension(format: string): string {
            if (format === 'json') {
                return 'json';
            }
            if (format === 'dot') {
                return 'dot';
            }
            return 'txt';
        }

        function hideCompareExplainSpinner() {
            $('#explain-compare-trigger')
                .attr('aria-busy', 'false')
                .removeClass('query-compare-action--spinning');
            hideCompareExplainCancelButton();
            compareExplainSpinnerVisibleSince = 0;
        }

        function showCompareExplainSpinner() {
            $('#explain-compare-trigger')
                .attr('aria-busy', 'true')
                .addClass('query-compare-action--spinning');
            showCompareExplainCancelButton();
            compareExplainSpinnerVisibleSince = Date.now();
        }

        function hideCompareExplainCancelButton() {
            $('#explain-compare-cancel')
                .removeClass('query-explain-cancel--visible')
                .attr('aria-hidden', 'true')
                .prop('disabled', true);
        }

        function showCompareExplainCancelButton() {
            $('#explain-compare-cancel')
                .addClass('query-explain-cancel--visible')
                .attr('aria-hidden', 'false')
                .prop('disabled', false);
        }

        function clearCompareExplainSpinnerDelayTimeout() {
            if (compareExplainSpinnerDelayTimeoutId !== null) {
                window.clearTimeout(compareExplainSpinnerDelayTimeoutId);
                compareExplainSpinnerDelayTimeoutId = null;
            }
        }

        function clearCompareExplainSpinnerHideTimeout() {
            if (compareExplainSpinnerHideTimeoutId !== null) {
                window.clearTimeout(compareExplainSpinnerHideTimeoutId);
                compareExplainSpinnerHideTimeoutId = null;
            }
        }

        function setCompareExplainButtonsDisabled(disabled: boolean) {
            $('#explain-compare-trigger').prop('disabled', disabled);
            $('#explain-trigger').prop('disabled', disabled);
            $('#rerun-explanation').prop('disabled', disabled);
        }

        function updateCompareActionState() {
            var bothQueriesAvailable = compareModeEnabled
                && getPaneQueryValue('primary').length > 0
                && getPaneQueryValue('compare').length > 0;
            $('#query-diff-trigger').prop('disabled', !bothQueriesAvailable || activeComparePendingRequests > 0);
        }

        function refreshVisibleQueryEditors() {
            window.requestAnimationFrame(function() {
                if (yasqe) {
                    yasqe.refresh();
                }
                if (compareModeEnabled && compareYasqe) {
                    compareYasqe.refresh();
                }
            });
        }

        function resetComparePaneState() {
            cancelCompareExplain();
            if (queryPageState) {
                queryPageState.compareQuerySeeded = false;
            }
            setPaneQueryValue('compare', '');
            dispatchQueryPageEvent({
                type: 'CLEAR_PANE',
                pane: 'compare',
                next: compareModeEnabled ? 'empty' : 'inactive'
            });
        }

        function shouldAutoExplainComparePaneOnOpen(): boolean {
            if (!compareModeEnabled || !isPaneReadyCurrent(queryPageState.primaryPane)) {
                return false;
            }
            var selectedLevel = getNormalizedExplainLevel(<string>$('#explain-level').val() || 'Optimized');
            return selectedLevel === 'Unoptimized' || selectedLevel === 'Optimized';
        }

        function syncCompareModeVisibility() {
            $('#explain-trigger').show();
            if (!compareModeEnabled) {
                hideCompareExplainSpinner();
            }
            renderQueryPageState();
            refreshVisibleQueryEditors();
        }

        function handleQueryPageInputChange(
            eventType: 'PRIMARY_QUERY_CHANGED' | 'COMPARE_QUERY_CHANGED' | 'EXPLAIN_LEVEL_CHANGED' | 'EXPLAIN_FORMAT_CHANGED'
        ) {
            if (activeComparePendingRequests > 0) {
                cancelCompareExplain();
            }
            if (activePrimaryRequestSignature) {
                cancelExplain();
            }
            if (eventType === 'COMPARE_QUERY_CHANGED' && queryPageState) {
                queryPageState.compareQuerySeeded = false;
            }
            if (eventType === 'PRIMARY_QUERY_CHANGED') {
                persistPrimaryQueryValue();
            }
            dispatchQueryPageEvent({ type: eventType });
        }

        export function notifyQueryPageInputChange(eventType: string) {
            if (eventType === 'PRIMARY_QUERY_CHANGED'
                    || eventType === 'COMPARE_QUERY_CHANGED'
                    || eventType === 'EXPLAIN_LEVEL_CHANGED'
                    || eventType === 'EXPLAIN_FORMAT_CHANGED') {
                handleQueryPageInputChange(<any>eventType);
            }
        }

        function splitDiffLines(text: string): string[] {
            if (!text) {
                return [];
            }
            return text.replace(/\r\n/g, '\n').split('\n');
        }

        function buildDiffRows(leftText: string, rightText: string): DiffRow[] {
            var leftLines = splitDiffLines(leftText);
            var rightLines = splitDiffLines(rightText);
            var matrix: number[][] = [];
            var leftLength = leftLines.length;
            var rightLength = rightLines.length;
            for (var rowIndex = 0; rowIndex <= leftLength; rowIndex++) {
                matrix[rowIndex] = [];
                for (var columnIndex = 0; columnIndex <= rightLength; columnIndex++) {
                    matrix[rowIndex][columnIndex] = 0;
                }
            }
            for (var leftIndex = leftLength - 1; leftIndex >= 0; leftIndex--) {
                for (var rightIndex = rightLength - 1; rightIndex >= 0; rightIndex--) {
                    if (leftLines[leftIndex] === rightLines[rightIndex]) {
                        matrix[leftIndex][rightIndex] = matrix[leftIndex + 1][rightIndex + 1] + 1;
                    } else {
                        matrix[leftIndex][rightIndex] = Math.max(
                            matrix[leftIndex + 1][rightIndex],
                            matrix[leftIndex][rightIndex + 1]
                        );
                    }
                }
            }

            var diffRows: DiffRow[] = [];
            var leftPointer = 0;
            var rightPointer = 0;
            while (leftPointer < leftLength && rightPointer < rightLength) {
                if (leftLines[leftPointer] === rightLines[rightPointer]) {
                    diffRows.push({ marker: ' ', text: leftLines[leftPointer], type: 'context' });
                    leftPointer += 1;
                    rightPointer += 1;
                } else if (matrix[leftPointer + 1][rightPointer] >= matrix[leftPointer][rightPointer + 1]) {
                    diffRows.push({ marker: '-', text: leftLines[leftPointer], type: 'removed' });
                    leftPointer += 1;
                } else {
                    diffRows.push({ marker: '+', text: rightLines[rightPointer], type: 'added' });
                    rightPointer += 1;
                }
            }
            while (leftPointer < leftLength) {
                diffRows.push({ marker: '-', text: leftLines[leftPointer], type: 'removed' });
                leftPointer += 1;
            }
            while (rightPointer < rightLength) {
                diffRows.push({ marker: '+', text: rightLines[rightPointer], type: 'added' });
                rightPointer += 1;
            }
            return diffRows;
        }

        function renderDiffView(targetSelector: string, leftText: string, rightText: string, placeholder?: string) {
            var target = $(targetSelector);
            target.empty();
            if (!leftText && !rightText) {
                target.text(placeholder || diffNotReadyLabel);
                return;
            }
            var diffRows = buildDiffRows(leftText || '', rightText || '');
            if (!diffRows.length) {
                target.text(placeholder || diffNotReadyLabel);
                return;
            }
            for (var i = 0; i < diffRows.length; i++) {
                var diffRow = diffRows[i];
                var rowElement = $('<div class="query-diff-row"></div>')
                    .addClass('query-diff-row--' + diffRow.type);
                rowElement.append($('<span class="query-diff-row__marker"></span>').text(diffRow.marker));
                rowElement.append($('<span></span>').text(diffRow.text));
                target.append(rowElement);
            }
        }

        function beginCompareExplainRequest(requestSignatures: RequestSignature[], triggerButtonId?: string): number {
            activeCompareRequestId += 1;
            activeComparePendingRequests = requestSignatures.length;
            activeCompareRequestSignatures = {};
            for (var i = 0; i < requestSignatures.length; i++) {
                activeCompareRequestSignatures[requestSignatures[i].pane] = requestSignatures[i];
                dispatchQueryPageEvent({
                    type: 'REQUEST_EXPLAIN',
                    signature: requestSignatures[i]
                });
            }
            hideCompareExplainSpinner();
            clearCompareExplainSpinnerDelayTimeout();
            clearCompareExplainSpinnerHideTimeout();
            beginComparePrimaryExplainWaitState(triggerButtonId);
            setCompareExplainButtonsDisabled(true);
            $('#query-diff-trigger').prop('disabled', true);
            compareExplainSpinnerDelayTimeoutId = window.setTimeout(function() {
                if (activeComparePendingRequests <= 0 || !activeCompareRequestId) {
                    return;
                }
                compareExplainSpinnerDelayTimeoutId = null;
                for (var signatureKey in activeCompareRequestSignatures) {
                    if (activeCompareRequestSignatures.hasOwnProperty(signatureKey)) {
                        dispatchQueryPageEvent({
                            type: 'SPINNER_DELAY_ELAPSED',
                            signature: activeCompareRequestSignatures[signatureKey]
                        });
                    }
                }
                showCompareExplainSpinner();
            }, 1000);
            return activeCompareRequestId;
        }

        function finishCompareExplainRequest(requestId: number) {
            if (requestId !== activeCompareRequestId) {
                return;
            }
            activeComparePendingRequests -= 1;
            if (activeComparePendingRequests > 0) {
                return;
            }
            activeComparePendingRequests = 0;
            activeCompareExplainJqXHRs = [];
            activeCompareRequestSignatures = {};
            setCompareExplainButtonsDisabled(false);
            clearCompareExplainSpinnerDelayTimeout();
            clearCompareExplainSpinnerHideTimeout();
            finishComparePrimaryExplainWaitState(requestId);
            if (!compareExplainSpinnerVisibleSince) {
                hideCompareExplainSpinner();
                updateCompareActionState();
                return;
            }
            var remainingSpinnerTime = 1000 - (Date.now() - compareExplainSpinnerVisibleSince);
            if (remainingSpinnerTime > 0) {
                compareExplainSpinnerHideTimeoutId = window.setTimeout(function() {
                    if (requestId !== activeCompareRequestId) {
                        return;
                    }
                    compareExplainSpinnerHideTimeoutId = null;
                    hideCompareExplainSpinner();
                    updateCompareActionState();
                }, remainingSpinnerTime);
                return;
            }
            hideCompareExplainSpinner();
            updateCompareActionState();
        }

        function enqueueCompareExplanationRequest(signature: RequestSignature) {
            var compareRequest = $.ajax({
                url: 'query',
                type: 'POST',
                dataType: 'json',
                data: serializeExplainFormData(
                    getPaneRawQueryValue(signature.pane),
                    signature.level,
                    signature.format,
                    signature.serverRequestId
                ),
                error: function(jqXHR: JQueryXHR, textStatus: string, errorThrown: string) {
                    if (textStatus !== 'abort'
                            && activeCompareRequestSignatures[signature.pane]
                            && signaturesMatch(activeCompareRequestSignatures[signature.pane], signature)) {
                        dispatchQueryPageEvent({
                            type: 'EXPLAIN_ERROR',
                            signature: signature,
                            message: getExplainErrorMessage(jqXHR, textStatus, errorThrown)
                        });
                    }
                },
                success: function(response: AjaxExplainResponse) {
                    if (!activeCompareRequestSignatures[signature.pane]
                            || !signaturesMatch(activeCompareRequestSignatures[signature.pane], signature)) {
                        return;
                    }
                    applyExplainResponseToPane(signature.pane, signature, response, signature.format);
                },
                complete: function() {
                    if (activeCompareRequestSignatures[signature.pane]
                            && signaturesMatch(activeCompareRequestSignatures[signature.pane], signature)) {
                        delete activeCompareRequestSignatures[signature.pane];
                    }
                    finishCompareExplainRequest(signature.groupId || signature.requestId);
                }
            });
            activeCompareExplainJqXHRs.push(compareRequest);
        }

        /**
         * Send a background HTTP request to save the query, and handle the
         * response asynchronously.
         *
         * @param overwrite
         *            if true, add a URL parameter that tells the server we wish
         *            to overwrite any already saved query
         */
        function ajaxSave(overwrite: boolean) {
            var feedback = $('#save-feedback');
            var url: string[] = [];
            url[url.length] = 'query';
            if (overwrite) {
                url[url.length] = document.all ? ';' : '?';
                url[url.length] = 'overwrite=true&'
	        }
            var href = url.join('');
            var form = $('form[action="query"]');
            $.ajax({
                url: href,
                type: 'POST',
                dataType: 'json',
                data: form.serialize(),
                timeout: 5000,
                error: function(jqXHR: JQueryXHR, textStatus: string, errorThrown: string) {
                    feedback.removeClass().addClass('error');
                    if (textStatus == 'timeout') {
                        feedback.text('Timed out waiting for response. Uncertain if save occured.');
                    } else {
                        feedback.text('Save Request Failed: Error Type = ' +
                            textStatus + ', HTTP Status Text = "' + errorThrown + '"');
                    }
                },
                success: function(response: AjaxSaveResponse) {
                    if (response.accessible) {
                        if (response.written) {
                            feedback.removeClass().addClass('success');
                            feedback.text('Query saved.');
                        } else {
                            if (response.existed) {
                                if (confirm('Query name exists. Click OK to overwrite.')) {
                                    ajaxSave(true);
                                } else {
                                    feedback.removeClass().addClass('error');
                                    feedback.text('Cancelled overwriting existing query.');
                                }
                            }
                        }
                    } else {
                        feedback.removeClass().addClass('error');
                        feedback.text('Repository was not accessible (check your permissions).');
                    }
                }
            });
        }

        /**
         * Invoked by form submission.
         *
         * @returns {boolean} true if a form POST is performed, false if
         *          a GET is instead performed
         */
        export function doSubmit() {
            //if yasqe is instantiated, make sure we save the value to the textarea
            if (yasqe) yasqe.save();
            $('#include-query-text').val('false');
            var allowPageToSubmitForm = false;
            var save = ($('#action').val() == 'save');
            if (save) {
                clearExplainSelection();
                ajaxSave(false);
            } else {
                var url: string[] = [];
                url[url.length] = 'query';
                if (document.all) {
                    url[url.length] = ';';
                } else {
                    url[url.length] = '?';
                }
                workbench.addParam(url, 'action');
                workbench.addParam(url, 'queryLn');
                workbench.addParam(url, 'query');
                workbench.addParam(url, 'limit_query');
                workbench.addParam(url, 'query-timeout');
                workbench.addParam(url, 'infer');
                workbench.addParam(url, 'explain');
                workbench.addParam(url, 'explain-format');
                var href = url.join('');
                var loc = document.location;
                var currentBaseLength = loc.href.length - loc.pathname.length
                    - loc.search.length;
                var pathLength = href.length;
                var urlLength = pathLength + currentBaseLength;

                // Published Internet Explorer restrictions on URL length, which are the
                // most restrictive of the major browsers.
                if (pathLength > 2048 || urlLength > 2083) {
                    alert("Due to its length, your query will be posted in the request body. "
                        + "It won't be possible to use a bookmark for the results page.");
                    $('#include-query-text').val('true');
                    allowPageToSubmitForm = true;
                } else {
                    // GET using the constructed URL, method exits here
                    document.location.href = href;
                }
            }

            // Value returned to form submit event. If not true, prevents normal form
            // submission.
            return allowPageToSubmitForm;
        }

        export function runExplain(level?: string, buttonId?: string) {
            if (compareModeEnabled) {
                runCompareExplain(buttonId || 'explain-trigger');
                return;
            }
            var effectiveLevel = getNormalizedExplainLevel(level || <string>$('#explain-level').val() || 'Optimized');
            var explainButton = getExplainTriggerButtonElement(buttonId);
            if (explainButton && explainButton.disabled) {
                return;
            }
            $('#explain-level').val(effectiveLevel);
            captureExplainButtonViewportTop('primary', buttonId);
            savePaneQuery('primary');
            activeExplainRequestId += 1;
            var signature = createRequestSignature(
                'primary',
                explainButton && explainButton.id === 'rerun-explanation' ? 'primary-rerun' : 'primary-explain',
                activeExplainRequestId
            );
            dispatchQueryPageEvent({ type: 'REQUEST_EXPLAIN', signature: signature });
            beginExplainRequest(explainButton ? explainButton.id : 'explain-trigger', signature);
            ajaxExplain(signature);
        }

        export function cancelExplain() {
            var cancelAction = workbench.queryCancelPolicy.getExplainCancelAction(
                activeComparePendingRequests,
                !!activePrimaryRequestSignature
            );
            if (cancelAction === 'compare') {
                cancelCompareExplain();
                return;
            }
            var cancelledSignature = activePrimaryRequestSignature;
            if (!cancelledSignature) {
                return;
            }
            dispatchQueryPageEvent({
                type: 'CANCEL_EXPLAIN',
                pane: 'primary',
                signature: cancelledSignature
            });
            postCancelExplain(cancelledSignature.serverRequestId);
            activePrimaryRequestSignature = null;
            if (activeExplainJqXHR) {
                activeExplainJqXHR.abort();
            }
            activeExplainJqXHR = null;
            finishExplainRequest(cancelledSignature.requestId);
            activeExplainRequestId += 1;
        }

        export function runCompareExplain(buttonId?: string) {
            if (!compareModeEnabled) {
                return;
            }
            savePaneQuery('primary');
            savePaneQuery('compare');
            var triggerButtonId = buttonId || 'explain-compare-trigger';
            captureExplainButtonViewportTop('primary', triggerButtonId);
            captureExplainButtonViewportTop('compare', triggerButtonId);
            var nextGroupId = activeCompareRequestId + 1;
            var requestSignatures = [
                createRequestSignature('primary', 'compare-refresh-both', nextGroupId, nextGroupId),
                createRequestSignature('compare', 'compare-refresh-both', nextGroupId, nextGroupId)
            ];
            beginCompareExplainRequest(requestSignatures, triggerButtonId);
            for (var i = 0; i < requestSignatures.length; i++) {
                enqueueCompareExplanationRequest(requestSignatures[i]);
            }
        }

        function requestComparePaneExplanation(level: string) {
            if (!compareModeEnabled) {
                return;
            }
            savePaneQuery('compare');
            $('#explain-level').val(getNormalizedExplainLevel(level));
            var nextGroupId = activeCompareRequestId + 1;
            var compareSignature = createRequestSignature('compare', 'compare-auto', nextGroupId, nextGroupId);
            beginCompareExplainRequest([compareSignature], 'explain-compare-trigger');
            enqueueCompareExplanationRequest(compareSignature);
        }

        export function cancelCompareExplain() {
            var cancelledSignatures: RequestSignature[] = [];
            for (var paneKey in activeCompareRequestSignatures) {
                if (activeCompareRequestSignatures.hasOwnProperty(paneKey) && activeCompareRequestSignatures[paneKey]) {
                    cancelledSignatures.push(activeCompareRequestSignatures[paneKey]);
                }
            }
            for (var i = 0; i < cancelledSignatures.length; i++) {
                postCancelExplain(cancelledSignatures[i].serverRequestId);
            }
            activeCompareRequestId += 1;
            activeComparePendingRequests = 0;
            for (var j = 0; j < activeCompareExplainJqXHRs.length; j++) {
                activeCompareExplainJqXHRs[j].abort();
            }
            activeCompareExplainJqXHRs = [];
            activeCompareRequestSignatures = {};
            clearCompareExplainSpinnerDelayTimeout();
            clearCompareExplainSpinnerHideTimeout();
            clearExplainSpinnerDelayTimeout();
            clearExplainSpinnerHideTimeout();
            hideExplainSpinners();
            explainSpinnerTargetId = '';
            hideCompareExplainSpinner();
            setCompareExplainButtonsDisabled(false);
            for (var k = 0; k < cancelledSignatures.length; k++) {
                dispatchQueryPageEvent({
                    type: 'CANCEL_EXPLAIN',
                    pane: cancelledSignatures[k].pane,
                    signature: cancelledSignatures[k]
                });
            }
            updateCompareActionState();
        }

        export function downloadExplanation() {
            if (queryPageState.primaryPane.kind !== 'ready' || queryPageState.primaryPane.freshness !== 'current') {
                return;
            }
            var primaryExplanation = queryPageState.primaryPane.explanation;
            var format = primaryExplanation.responseFormat || <string>$('#explain-format').val() || 'text';
            var extension = getExplanationDownloadExtension(format);
            var mimeType = getExplanationDownloadMimeType(format);
            var blob = new Blob([primaryExplanation.rawContent], { type: mimeType + ';charset=utf-8' });
            var link = document.createElement('a');
            var selectedLevel = <string>$('#explain-level').val() || 'query';
            link.download = 'query-explanation-' + selectedLevel.toLowerCase() + '.' + extension;
            link.href = window.URL.createObjectURL(blob);
            document.body.appendChild(link);
            link.click();
            window.URL.revokeObjectURL(link.href);
            document.body.removeChild(link);
        }

        export function initializeExplanationView() {
            var initialExplanation = $('#query-explanation').text();
            var initialFormat = getNormalizedExplainFormat(
                <string>$('#query-explanation').attr('data-format') || <string>$('#explain-format').val() || 'text'
            );
            var hydratedExplanation: StableExplanation = null;
            if (initialExplanation) {
                hydratedExplanation = createStableExplanationFromResponse({
                    requestId: 0,
                    serverRequestId: 'initial-primary-explanation',
                    pane: 'primary',
                    source: 'primary-explain',
                    queryHash: buildQueryHash(getPaneRawQueryValue('primary')),
                    level: getNormalizedExplainLevel(<string>$('#explain-level').val()),
                    format: initialFormat
                }, {
                    content: initialExplanation,
                    format: initialFormat,
                    error: ''
                }, initialFormat);
            }
            dispatchQueryPageEvent({
                type: 'HYDRATE',
                primaryExplanation: hydratedExplanation
            });
        }

        export function setQueryValue(queryString: string): void {
            setPaneQueryValue('primary', $.trim(queryString));
        }

        export function getQueryValue(): string {
            return getPaneQueryValue('primary');
        }

        export function getYasqe(): YASQE_Instance {
            return yasqe;
        }

        export function updateYasqe() {
            if ($("#queryLn").val() == "SPARQL") {
                initYasqe();
                if (compareModeEnabled || compareYasqe) {
                    ensureCompareYasqe();
                }
            } else {
                closeYasqe();
                closeCompareYasqe();
            }
            updateCompareActionState();
        }

        function initPaneYasqe(paneKey: string, clearFeedbackOnChange?: boolean): YASQE_Instance {
            workbench.yasqeHelper.setupCompleters(sparqlNamespaces);
            var paneEditor = YASQE.fromTextArea(<HTMLTextAreaElement>document.getElementById(getPaneState(paneKey).queryId), {
                consumeShareLink: null,//don't try to parse the url args. this is already done by the addLoad function below
                persistent: paneKey === 'compare' ? null : getPanePersistedQueryStorageKey(paneKey)
            });
            if (paneKey === 'compare') {
                clearPanePersistedQuery('compare');
            }
            $(paneEditor.getWrapperElement()).css({
                "fontSize": "14px",
                "width": "100%",
                "maxWidth": "100%",
                "boxSizing": "border-box"
            });
            paneEditor.on('change', function() {
                if (paneKey === 'compare') {
                    clearPanePersistedQuery('compare');
                } else {
                    persistPrimaryQueryEditorValue(paneEditor);
                }
                workbench.query.clearFeedback();
                handleQueryPageInputChange(paneKey === 'compare' ? 'COMPARE_QUERY_CHANGED' : 'PRIMARY_QUERY_CHANGED');
            });
            paneEditor.refresh();
            setPaneQueryEditor(paneKey, paneEditor);
            return paneEditor;
        }

        function initYasqe() {
            if (yasqe) {
                yasqe.refresh();
                return;
            }
            initPaneYasqe('primary', true);
        }

        function ensureCompareYasqe() {
            if ($("#queryLn").val() != "SPARQL" || compareYasqe) {
                return;
            }
            initPaneYasqe('compare');
        }

        function closeCompareYasqe() {
            if (compareYasqe) {
                compareYasqe.toTextArea();
                compareYasqe = null;
            }
            clearPanePersistedQuery('compare');
        }

        function closeYasqe() {
            if (yasqe) {
                yasqe.toTextArea();
                yasqe = null;
            }
        }

        export function toggleCompareMode() {
            if (!compareModeEnabled) {
                if (!queryPageState.compareQuerySeeded && !getPaneQueryValue('compare')) {
                    setPaneQueryValue('compare', getPaneRawQueryValue('primary'));
                    queryPageState.compareQuerySeeded = true;
                }
                dispatchQueryPageEvent({ type: 'TOGGLE_COMPARE' });
                if ($("#queryLn").val() == "SPARQL") {
                    ensureCompareYasqe();
                }
            } else {
                closeDiffModal();
                resetComparePaneState();
                dispatchQueryPageEvent({ type: 'TOGGLE_COMPARE' });
            }
            syncCompareModeVisibility();
            if (compareModeEnabled && shouldAutoExplainComparePaneOnOpen()) {
                var selectedExplainLevel = <string>$('#explain-level').val() || 'Optimized';
                requestComparePaneExplanation(selectedExplainLevel);
            }
        }

        export function toggleCompareSidebar() {
            if (!compareModeEnabled) {
                return;
            }
            dispatchQueryPageEvent({ type: 'TOGGLE_SIDEBAR' });
        }

        export function openDiffModal() {
            if (!compareModeEnabled) {
                return;
            }
            lastDiffTriggerElement = <HTMLElement>document.getElementById('query-diff-trigger');
            dispatchQueryPageEvent({ type: 'OPEN_DIFF' });
            (<HTMLElement>document.getElementById('query-diff-close')).focus();
        }

        export function closeDiffModal() {
            dispatchQueryPageEvent({ type: 'CLOSE_DIFF' });
            if (lastDiffTriggerElement) {
                lastDiffTriggerElement.focus();
            }
        }

        export function initializeCompareUi() {
            clearPanePersistedQuery('compare');
            diffNotReadyLabel = $.trim($('#query-diff-explanation').text());
            resetComparePaneState();
            syncCompareModeVisibility();
        }

        export function refreshCompareActionState() {
            updateCompareActionState();
        }

        export var testing = {
            applyDotPanZoom: applyDotPanZoom,
            ajaxSave: ajaxSave,
            applyExplainResponseToPane: applyExplainResponseToPane,
            buildDiffRows: buildDiffRows,
            buildQueryHash: buildQueryHash,
            beginComparePrimaryExplainWaitState: beginComparePrimaryExplainWaitState,
            beginExplainRequest: beginExplainRequest,
            cancelCompareExplain: cancelCompareExplain,
            captureExplainButtonViewportTop: captureExplainButtonViewportTop,
            clearExplainButtonViewportRestoreState: clearExplainButtonViewportRestoreState,
            clearExplainSelection: clearExplainSelection,
            clearExplainSpinnerDelayTimeout: clearExplainSpinnerDelayTimeout,
            clearExplainSpinnerHideTimeout: clearExplainSpinnerHideTimeout,
            clearExplanationDimensionLock: clearExplanationDimensionLock,
            clearRenderedExplanation: clearRenderedExplanation,
            clearWorkbenchCookie: clearWorkbenchCookie,
            clearPanePersistedQuery: clearPanePersistedQuery,
            cloneStableExplanation: cloneStableExplanation,
            collectCurrentInputs: collectCurrentInputs,
            computePlanEntryPercentages: computePlanEntryPercentages,
            createDiffModalState: createDiffModalState,
            createEmptyQueryPageInputs: createEmptyQueryPageInputs,
            createErrorPaneState: createErrorPaneState,
            createFallbackExplainServerRequestId: createFallbackExplainServerRequestId,
            createInitialQueryPageState: createInitialQueryPageState,
            createJsonScalarElement: createJsonScalarElement,
            createJsonTreeNode: createJsonTreeNode,
            createReadyPaneState: createReadyPaneState,
            createRequestSignature: createRequestSignature,
            createStableExplanationFromResponse: createStableExplanationFromResponse,
            dispatchQueryPageEvent: dispatchQueryPageEvent,
            destroyDotPanZoom: destroyDotPanZoom,
            formatJsonArrayEntryKey: formatJsonArrayEntryKey,
            formatJsonKey: formatJsonKey,
            formatPercentage: formatPercentage,
            finishCompareExplainRequest: finishCompareExplainRequest,
            finishComparePrimaryExplainWaitState: finishComparePrimaryExplainWaitState,
            finishExplainRequest: finishExplainRequest,
            generateExplainServerRequestId: generateExplainServerRequestId,
            getEventSignatureForPane: getEventSignatureForPane,
            getExplainErrorMessage: getExplainErrorMessage,
            getExplainTriggerButtonElement: getExplainTriggerButtonElement,
            getExplanationDownloadExtension: getExplanationDownloadExtension,
            getExplanationDownloadMimeType: getExplanationDownloadMimeType,
            getJsonSummary: getJsonSummary,
            getNormalizedExplainFormat: getNormalizedExplainFormat,
            getNormalizedExplainLevel: getNormalizedExplainLevel,
            getPaneQueryValue: getPaneQueryValue,
            getPaneRawQueryValue: getPaneRawQueryValue,
            getPanePersistedQueryStorageKey: getPanePersistedQueryStorageKey,
            getPaneQuerySelector: getPaneQuerySelector,
            getPaneState: getPaneState,
            getPaneDisplayExplanation: getPaneDisplayExplanation,
            getPaneMachineState: getPaneMachineState,
            getPaneQueryHashFromInputs: getPaneQueryHashFromInputs,
            getPaneSnapshot: getPaneSnapshot,
            getPaneStatusClassName: getPaneStatusClassName,
            getPaneStatusMessage: getPaneStatusMessage,
            getPaneOverlayMessage: getPaneOverlayMessage,
            getStableExplanationContentKey: getStableExplanationContentKey,
            getStableExplanationKey: getStableExplanationKey,
            getStaleReasons: getStaleReasons,
            getWorkbenchCookiePath: getWorkbenchCookiePath,
            handleQueryPageInputChange: handleQueryPageInputChange,
            hideCompareExplainCancelButton: hideCompareExplainCancelButton,
            hideCompareExplainSpinner: hideCompareExplainSpinner,
            hideExplainCancelButtons: hideExplainCancelButtons,
            hideExplainSpinners: hideExplainSpinners,
            lockExplanationDimensions: lockExplanationDimensions,
            isJsonExpandable: isJsonExpandable,
            isPaneReadyCurrent: isPaneReadyCurrent,
            parseNumericJsonValue: parseNumericJsonValue,
            persistPrimaryQueryEditorValue: persistPrimaryQueryEditorValue,
            persistPrimaryQueryValue: persistPrimaryQueryValue,
            postCancelExplain: postCancelExplain,
            refreshVisibleQueryEditors: refreshVisibleQueryEditors,
            reduceDiffModalState: reduceDiffModalState,
            reducePaneState: reducePaneState,
            renderDotView: renderDotView,
            renderDiffView: renderDiffView,
            renderExplanation: renderExplanation,
            renderJsonExplanationTree: renderJsonExplanationTree,
            renderJsonView: renderJsonView,
            renderQueryPageState: renderQueryPageState,
            resetComparePaneState: resetComparePaneState,
            restorePaneStateFromPrevious: restorePaneStateFromPrevious,
            restoreExplainButtonViewportTopIfNeeded: restoreExplainButtonViewportTopIfNeeded,
            savePaneQuery: savePaneQuery,
            serializeCancelExplainFormData: serializeCancelExplainFormData,
            serializeExplainFormData: serializeExplainFormData,
            setCompareExplainButtonsDisabled: setCompareExplainButtonsDisabled,
            setExplainButtonsDisabled: setExplainButtonsDisabled,
            setExplanationDisplayMode: setExplanationDisplayMode,
            setPaneQueryEditor: setPaneQueryEditor,
            setPaneQueryValue: setPaneQueryValue,
            setPaneMachineState: setPaneMachineState,
            setWorkbenchCookie: setWorkbenchCookie,
            showCompareExplainCancelButton: showCompareExplainCancelButton,
            showCompareExplainSpinner: showCompareExplainSpinner,
            showExplainCancelButton: showExplainCancelButton,
            showExplainError: showExplainError,
            showExplainSpinner: showExplainSpinner,
            shouldAutoExplainComparePaneOnOpen: shouldAutoExplainComparePaneOnOpen,
            signaturesMatch: signaturesMatch,
            splitDiffLines: splitDiffLines,
            syncCompareSidebarState: syncCompareSidebarState,
            syncLegacyExplanationCache: syncLegacyExplanationCache,
            syncLegacyMachineFlags: syncLegacyMachineFlags,
            syncPrimaryExplanationControls: syncPrimaryExplanationControls,
            updateCompareActionState: updateCompareActionState,
            updateDownloadButtonState: updateDownloadButtonState,
            getInternalState: function() {
                return {
                    activeComparePendingRequests: activeComparePendingRequests,
                    activeCompareRequestId: activeCompareRequestId,
                    activeCompareRequestSignatures: activeCompareRequestSignatures,
                    activePrimaryRequestSignature: activePrimaryRequestSignature,
                    compareModeEnabled: compareModeEnabled,
                    comparePaneState: comparePaneState,
                    compareQuerySeeded: compareQuerySeeded,
                    compareSidebarOpen: compareSidebarOpen,
                    currentQueryLn: currentQueryLn,
                    diffNotReadyLabel: diffNotReadyLabel,
                    explainServerRequestIdCounter: explainServerRequestIdCounter,
                    lastRenderedExplanationKeys: lastRenderedExplanationKeys,
                    pendingDotRenderKeys: pendingDotRenderKeys,
                    primaryPaneState: primaryPaneState,
                    queryPageState: queryPageState
                };
            },
            resetInternalState: function() {
                currentQueryLn = '';
                yasqe = null;
                compareYasqe = null;
                vizRenderer = null;
                queryPageState = createInitialQueryPageState();
                lastRenderedExplanationKeys = {};
                pendingDotRenderKeys = {};
                activePrimaryRequestSignature = null;
                activeCompareRequestSignatures = {};
                explainServerRequestIdCounter = 0;
                explainSpinnerVisibleSince = 0;
                explainSpinnerTargetId = '';
                explainSpinnerDelayTimeoutId = null;
                explainSpinnerHideTimeoutId = null;
                compareExplainSpinnerVisibleSince = 0;
                compareExplainSpinnerDelayTimeoutId = null;
                compareExplainSpinnerHideTimeoutId = null;
                activeExplainRequestId = 0;
                activeExplainJqXHR = null;
                primaryExplanationPending = false;
                activeCompareRequestId = 0;
                activeComparePendingRequests = 0;
                activeCompareExplainJqXHRs = [];
                compareModeEnabled = false;
                compareSidebarOpen = false;
                compareQuerySeeded = false;
                diffNotReadyLabel = '';
                lastDiffTriggerElement = null;
                primaryPaneState.latestExplanation = '';
                primaryPaneState.latestExplanationFormat = 'text';
                primaryPaneState.dotPanZoomInstance = null;
                primaryPaneState.explainButtonViewportTopBeforeRequest = null;
                primaryPaneState.explainButtonIdBeforeRequest = '';
                comparePaneState.latestExplanation = '';
                comparePaneState.latestExplanationFormat = 'text';
                comparePaneState.dotPanZoomInstance = null;
                comparePaneState.explainButtonViewportTopBeforeRequest = null;
                comparePaneState.explainButtonIdBeforeRequest = '';
            },
            setInternalState: function(state: any) {
                if ('activeComparePendingRequests' in state) {
                    activeComparePendingRequests = state.activeComparePendingRequests;
                }
                if ('activeCompareRequestId' in state) {
                    activeCompareRequestId = state.activeCompareRequestId;
                }
                if ('activeCompareRequestSignatures' in state) {
                    activeCompareRequestSignatures = state.activeCompareRequestSignatures;
                }
                if ('activePrimaryRequestSignature' in state) {
                    activePrimaryRequestSignature = state.activePrimaryRequestSignature;
                }
                if ('compareModeEnabled' in state) {
                    compareModeEnabled = state.compareModeEnabled;
                }
                if ('compareQuerySeeded' in state) {
                    compareQuerySeeded = state.compareQuerySeeded;
                }
                if ('compareSidebarOpen' in state) {
                    compareSidebarOpen = state.compareSidebarOpen;
                }
                if ('currentQueryLn' in state) {
                    currentQueryLn = state.currentQueryLn;
                }
                if ('diffNotReadyLabel' in state) {
                    diffNotReadyLabel = state.diffNotReadyLabel;
                }
                if ('explainServerRequestIdCounter' in state) {
                    explainServerRequestIdCounter = state.explainServerRequestIdCounter;
                }
                if ('lastRenderedExplanationKeys' in state) {
                    lastRenderedExplanationKeys = state.lastRenderedExplanationKeys;
                }
                if ('pendingDotRenderKeys' in state) {
                    pendingDotRenderKeys = state.pendingDotRenderKeys;
                }
                if ('primaryPaneState' in state) {
                    primaryPaneState = state.primaryPaneState;
                }
                if ('comparePaneState' in state) {
                    comparePaneState = state.comparePaneState;
                }
                if ('queryPageState' in state) {
                    queryPageState = state.queryPageState;
                }
            }
        };

    }
}

interface QueryTextResponse {
    queryText: string;
}

workbench.addLoad(function queryPageLoaded() {
    /**
     * Gets a parameter from the URL or the cookies, preferentially in that
     * order.
     *
     * @param param
     *            the name of the parameter
     * @returns the value of the given parameter, or something that evaluates
                  as false, if the parameter was not found
     */
    function getParameterFromUrlOrCookie(param: string) {
        var href = document.location.href;
        var elements = href.substring(href.indexOf('?') + 1).substring(
            href.indexOf(';') + 1).split(decodeURIComponent('%26'));
        var result = '';
        for (var i = 0; elements.length - i; i++) {
            var pair = elements[i].split('=');
            var value = decodeURIComponent(pair[1]).replace(/\+/g, ' ');
            if (pair[0] == param) {
                result = value;
            }
        }
        if (!result) {
            result = workbench.getCookie(param);
        }
        return result;
    }

    function getQueryTextFromServer(queryParam: string, refParam: string) {
        $.getJSON('query', {
            action: "get",
            query: queryParam,
            ref: refParam
        }, function(response: QueryTextResponse) {
                if (response.queryText) {
                    workbench.query.setQueryValue(response.queryText);
                }
            });
    }

    //Start with initializing our YASQE instance, given that 'SPARQL' is the selected query language
    //(all the following 'set' and 'get' SPARQL query functions require an instantiated yasqe instance
    workbench.query.updateYasqe();

    // Populate the query text area with the value of the URL query parameter,
    // only if it is present. If it is not present in the URL query, then
    // looks for the 'query' cookie, and sets it from that. (The cookie
    // enables re-populating the text field with the previous query when the
    // user returns via the browser back button.)
    var query = getParameterFromUrlOrCookie('query');
    if (query) {
        var ref = getParameterFromUrlOrCookie('ref');
        if (ref == 'id' || ref == 'hash') {
            getQueryTextFromServer(query, ref);
        } else {
            workbench.query.setQueryValue(query);
        }
    }
    workbench.query.loadNamespaces();

    // Trim the query text area contents of any leading and/or trailing
    // whitespace.
    workbench.query.setQueryValue($.trim(workbench.query.getQueryValue()));
    workbench.query.initializeExplanationView();
    workbench.query.initializeCompareUi();

    // Add click handlers identifying the clicked element in a hidden 'action'
    // form field.
    var addHandler = function(id: string, callback?: () => void) {
        $('#' + id).click(function setAction() {
            $('#action').val(id);
            if (callback) {
                callback();
            }
        });
    };
    addHandler('exec', function() {
        $('#explain').val('');
        $('#explain-level').val('');
    });
    addHandler('save', function() {
        $('#explain').val('');
        $('#explain-level').val('');
    });
    $('#download-explanation').click(workbench.query.downloadExplanation);
    // Add event handlers to the save name field to react to changes in it.
    $('#query-name').bind('keydown cut paste', workbench.query.handleNameChange);

    // Add event handlers to the query text area to react to changes in it.
    function deferInputChange(handler: () => void) {
        return function() {
            window.setTimeout(handler, 0);
        };
    }
    $('#query').bind('keydown cut paste change', deferInputChange(function() {
        workbench.query.clearFeedback();
        workbench.query.notifyQueryPageInputChange('PRIMARY_QUERY_CHANGED');
    }));
    $('#query-compare').bind('keydown cut paste change', deferInputChange(function() {
        workbench.query.clearFeedback();
        workbench.query.notifyQueryPageInputChange('COMPARE_QUERY_CHANGED');
    }));
    $('#explain-level').change(function() {
        workbench.query.notifyQueryPageInputChange('EXPLAIN_LEVEL_CHANGED');
    });
    $('#explain-format').change(function() {
        workbench.query.notifyQueryPageInputChange('EXPLAIN_FORMAT_CHANGED');
    });
    $('#query-diff-modal').click(function(event) {
        if (event.target && (<HTMLElement>event.target).id === 'query-diff-modal') {
            workbench.query.closeDiffModal();
        }
    });
    $(document).keydown(function(event) {
        if (event.key === 'Escape' && $('#query-diff-modal').hasClass('query-diff-modal--open')) {
            workbench.query.closeDiffModal();
        }
    });

    // Detect if there is no current authenticated user, and if so, disable
    // the 'save privately' option.
    if ($('#selected-user>span').is('.disabled')) {
        $('#save-private').prop('checked', false).prop('disabled', true);
    }
});
