/// <reference path="template.ts" />
/// <reference path="jquery.d.ts" />
/// <reference path="queryCancelPolicy.ts" />
/// <reference path="query-trace-player.ts" />
/// <reference path="yasqe.d.ts" />
/// <reference path="yasqeHelper.ts" />
var workbench;
(function (workbench) {
    var query;
    (function (query_1) {
        /**
         * Holds the current selected query language.
         */
        var currentQueryLn = '';
        var yasqe = null;
        var compareYasqe = null;
        var vizRenderer = null;
        var queryPageState = null;
        var lastRenderedExplanationKeys = {};
        var pendingDotRenderKeys = {};
        var activePrimaryRequestSignature = null;
        var activeCompareRequestSignatures = {};
        var explainServerRequestIdCounter = 0;
        var activeExplainRequestId = 0;
        var activeExplainJqXHR = null;
        var primaryExplanationPending = false;
        var activeCompareRequestId = 0;
        var activeComparePendingRequests = 0;
        var activeCompareExplainJqXHRs = [];
        var traceServerRequestIdCounter = 0;
        var activeTraceRequestId = 0;
        var activeTraceJqXHR = null;
        var activeTraceServerRequestId = '';
        var activeTracePlaybackTimer = null;
        var activeTraceBridgeTimer = null;
        var currentTrace = null;
        var currentTraceState = null;
        var previousTraceFrameIndex = null;
        var previousTraceActivePatternIndex = null;
        var previousTraceMarkerOffset = null;
        var pendingTraceMarkerAnimationFrame = null;
        var compareModeEnabled = false;
        var compareSidebarOpen = false;
        var compareQuerySeeded = false;
        var diffNotReadyLabel = '';
        var lastDiffTriggerElement = null;
        var primaryPaneState = {
            key: 'primary',
            queryId: 'query',
            errorId: 'queryString.errors',
            explanationRowId: 'query-explanation-row',
            explanationControlsRowId: 'query-explanation-controls-row',
            copyButtonId: 'copy-explanation',
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
        var comparePaneState = {
            key: 'compare',
            queryId: 'query-compare',
            errorId: 'queryString.errors-compare',
            explanationRowId: 'query-explanation-row-compare',
            copyButtonId: 'copy-explanation-compare',
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
        var explainRequestSpinnerDelayMs = 1000;
        var explainRequestSpinnerMinVisibleMs = 1000;
        function createExplainRequestUiState() {
            return {
                spinnerVisibleSince: 0,
                spinnerTargetId: '',
                spinnerDelayTimeoutId: null,
                spinnerHideTimeoutId: null
            };
        }
        var explainRequestUiStates = {
            primary: createExplainRequestUiState(),
            compare: createExplainRequestUiState()
        };
        function normalizePrimaryExplainButtonId(buttonId) {
            return workbench.queryCancelPolicy.getExplainControlIds(buttonId).buttonId;
        }
        function hidePrimaryExplainCancelButtons() {
            $('.query-explain-cancel')
                .removeClass('query-explain-cancel--visible')
                .attr('aria-hidden', 'true')
                .prop('disabled', true);
        }
        function showPrimaryExplainCancelButton(buttonId) {
            var controlIds = workbench.queryCancelPolicy.getExplainControlIds(buttonId);
            hidePrimaryExplainCancelButtons();
            if (!controlIds.cancelId) {
                return false;
            }
            $('#' + controlIds.cancelId)
                .addClass('query-explain-cancel--visible')
                .attr('aria-hidden', 'false')
                .prop('disabled', false);
            return true;
        }
        function hidePrimaryExplainSpinner() {
            $('.query-explain-spinner')
                .removeClass('query-explain-spinner--visible')
                .attr('aria-hidden', 'true');
            hidePrimaryExplainCancelButtons();
        }
        function showPrimaryExplainSpinner(buttonId) {
            var controlIds = workbench.queryCancelPolicy.getExplainControlIds(buttonId);
            if (!controlIds.spinnerId) {
                return false;
            }
            $('#' + controlIds.spinnerId)
                .addClass('query-explain-spinner--visible')
                .attr('aria-hidden', 'false');
            showPrimaryExplainCancelButton(controlIds.buttonId);
            return true;
        }
        function setPrimaryExplainButtonsDisabled(disabled) {
            $('#explain-trigger').prop('disabled', disabled);
            $('#rerun-explanation').prop('disabled', disabled);
        }
        function normalizeCompareExplainButtonId() {
            return 'explain-compare-trigger';
        }
        function hideCompareExplainCancelButtonInternal() {
            $('#explain-compare-cancel')
                .removeClass('query-explain-cancel--visible')
                .attr('aria-hidden', 'true')
                .prop('disabled', true);
        }
        function showCompareExplainCancelButtonInternal() {
            $('#explain-compare-cancel')
                .addClass('query-explain-cancel--visible')
                .attr('aria-hidden', 'false')
                .prop('disabled', false);
            return true;
        }
        function hideCompareExplainSpinnerInternal() {
            $('#explain-compare-trigger')
                .attr('aria-busy', 'false')
                .removeClass('query-compare-action--spinning');
            hideCompareExplainCancelButtonInternal();
        }
        function showCompareExplainSpinnerInternal() {
            $('#explain-compare-trigger')
                .attr('aria-busy', 'true')
                .addClass('query-compare-action--spinning');
            showCompareExplainCancelButtonInternal();
            return true;
        }
        function setCompareExplainButtonsDisabledInternal(disabled) {
            $('#explain-compare-trigger').prop('disabled', disabled);
            $('#explain-trigger').prop('disabled', disabled);
            $('#rerun-explanation').prop('disabled', disabled);
        }
        var explainRequestUiControllers = {
            primary: {
                key: 'primary',
                normalizeButtonId: normalizePrimaryExplainButtonId,
                hideCancelButtons: hidePrimaryExplainCancelButtons,
                showCancelButton: showPrimaryExplainCancelButton,
                hideSpinner: hidePrimaryExplainSpinner,
                showSpinner: showPrimaryExplainSpinner,
                setButtonsDisabled: setPrimaryExplainButtonsDisabled
            },
            compare: {
                key: 'compare',
                normalizeButtonId: normalizeCompareExplainButtonId,
                hideCancelButtons: hideCompareExplainCancelButtonInternal,
                showCancelButton: showCompareExplainCancelButtonInternal,
                hideSpinner: hideCompareExplainSpinnerInternal,
                showSpinner: showCompareExplainSpinnerInternal,
                setButtonsDisabled: setCompareExplainButtonsDisabledInternal
            }
        };
        function getExplainRequestUiState(controllerKey) {
            return explainRequestUiStates[controllerKey];
        }
        function getExplainRequestUiController(controllerKey) {
            return explainRequestUiControllers[controllerKey];
        }
        function hideExplainRequestCancelButtons(controllerKey) {
            getExplainRequestUiController(controllerKey).hideCancelButtons();
        }
        function showExplainRequestCancelButton(controllerKey, buttonId) {
            getExplainRequestUiController(controllerKey).showCancelButton(buttonId);
        }
        function hideExplainRequestSpinner(controllerKey) {
            var uiState = getExplainRequestUiState(controllerKey);
            getExplainRequestUiController(controllerKey).hideSpinner();
            uiState.spinnerVisibleSince = 0;
        }
        function showExplainRequestSpinner(controllerKey, buttonId) {
            var uiState = getExplainRequestUiState(controllerKey);
            var uiController = getExplainRequestUiController(controllerKey);
            var targetButtonId = uiController.normalizeButtonId(buttonId);
            hideExplainRequestSpinner(controllerKey);
            if (!targetButtonId || !uiController.showSpinner(targetButtonId)) {
                uiState.spinnerTargetId = '';
                return;
            }
            uiState.spinnerTargetId = targetButtonId;
            uiState.spinnerVisibleSince = Date.now();
        }
        function clearExplainRequestSpinnerDelayTimeout(controllerKey) {
            var uiState = getExplainRequestUiState(controllerKey);
            if (uiState.spinnerDelayTimeoutId !== null) {
                window.clearTimeout(uiState.spinnerDelayTimeoutId);
                uiState.spinnerDelayTimeoutId = null;
            }
        }
        function clearExplainRequestSpinnerHideTimeout(controllerKey) {
            var uiState = getExplainRequestUiState(controllerKey);
            if (uiState.spinnerHideTimeoutId !== null) {
                window.clearTimeout(uiState.spinnerHideTimeoutId);
                uiState.spinnerHideTimeoutId = null;
            }
        }
        function setExplainRequestButtonsDisabled(controllerKey, disabled) {
            getExplainRequestUiController(controllerKey).setButtonsDisabled(disabled);
        }
        function beginExplainRequestUiWaitState(controllerKey, requestCounter, buttonId, shouldShowSpinner, options) {
            var uiState = getExplainRequestUiState(controllerKey);
            var uiController = getExplainRequestUiController(controllerKey);
            var targetButtonId = uiController.normalizeButtonId(buttonId);
            clearExplainRequestSpinnerDelayTimeout(controllerKey);
            clearExplainRequestSpinnerHideTimeout(controllerKey);
            hideExplainRequestSpinner(controllerKey);
            uiState.spinnerTargetId = targetButtonId;
            if (options && options.disableButtons) {
                uiController.setButtonsDisabled(true);
            }
            if (!targetButtonId) {
                return;
            }
            uiState.spinnerDelayTimeoutId = window.setTimeout(function () {
                if (!shouldShowSpinner(requestCounter, targetButtonId)) {
                    return;
                }
                uiState.spinnerDelayTimeoutId = null;
                if (options && options.onDelayElapsed) {
                    options.onDelayElapsed();
                }
                showExplainRequestSpinner(controllerKey, targetButtonId);
            }, explainRequestSpinnerDelayMs);
        }
        function finishExplainRequestUiWaitState(controllerKey, requestCounter, shouldHideSpinner, options) {
            var uiState = getExplainRequestUiState(controllerKey);
            var uiController = getExplainRequestUiController(controllerKey);
            if (options && options.disableButtons) {
                uiController.setButtonsDisabled(false);
            }
            clearExplainRequestSpinnerDelayTimeout(controllerKey);
            clearExplainRequestSpinnerHideTimeout(controllerKey);
            if (!uiState.spinnerVisibleSince) {
                hideExplainRequestSpinner(controllerKey);
                uiState.spinnerTargetId = '';
                if (options && options.onAfterHide) {
                    options.onAfterHide();
                }
                return;
            }
            var spinnerTargetId = uiState.spinnerTargetId;
            var remainingSpinnerTime = explainRequestSpinnerMinVisibleMs - (Date.now() - uiState.spinnerVisibleSince);
            if (remainingSpinnerTime > 0) {
                uiState.spinnerHideTimeoutId = window.setTimeout(function () {
                    if (!shouldHideSpinner(requestCounter, spinnerTargetId)) {
                        return;
                    }
                    uiState.spinnerHideTimeoutId = null;
                    hideExplainRequestSpinner(controllerKey);
                    uiState.spinnerTargetId = '';
                    if (options && options.onAfterHide) {
                        options.onAfterHide();
                    }
                }, remainingSpinnerTime);
                return;
            }
            hideExplainRequestSpinner(controllerKey);
            uiState.spinnerTargetId = '';
            if (options && options.onAfterHide) {
                options.onAfterHide();
            }
        }
        function resetExplainRequestUiState(controllerKey) {
            var uiState = getExplainRequestUiState(controllerKey);
            uiState.spinnerVisibleSince = 0;
            uiState.spinnerTargetId = '';
            uiState.spinnerDelayTimeoutId = null;
            uiState.spinnerHideTimeoutId = null;
        }
        function getNormalizedExplainLevel(level) {
            switch (level) {
                case 'Unoptimized':
                case 'Executed':
                case 'Telemetry':
                case 'Timed':
                    return level;
                case 'Optimized':
                default:
                    return 'Optimized';
            }
        }
        function getNormalizedExplainFormat(format) {
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
        function buildQueryHash(queryValue) {
            return queryValue || '';
        }
        function createEmptyQueryPageInputs() {
            return {
                primaryQueryHash: '',
                compareQueryHash: '',
                explainLevel: 'Optimized',
                explainFormat: 'text'
            };
        }
        function collectCurrentInputs() {
            return {
                primaryQueryHash: buildQueryHash(getPaneRawQueryValue('primary')),
                compareQueryHash: buildQueryHash(getPaneRawQueryValue('compare')),
                explainLevel: getNormalizedExplainLevel($('#explain-level').val()),
                explainFormat: getNormalizedExplainFormat($('#explain-format').val())
            };
        }
        function createRequestSignature(paneKey, source, requestId, groupId) {
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
        function createFallbackExplainServerRequestId() {
            explainServerRequestIdCounter += 1;
            var timestampPart = ('000000000000' + Date.now().toString(16)).slice(-12);
            var counterPart = ('00000000' + explainServerRequestIdCounter.toString(16)).slice(-8);
            var randomPart = '';
            var cryptoObject = window.crypto || window.msCrypto;
            if (cryptoObject && cryptoObject.getRandomValues) {
                var buffer = new Uint16Array(4);
                cryptoObject.getRandomValues(buffer);
                for (var i = 0; i < buffer.length; i++) {
                    randomPart += ('0000' + buffer[i].toString(16)).slice(-4);
                }
            }
            else {
                while (randomPart.length < 16) {
                    randomPart += ('00000000' + Math.floor(Math.random() * 0xffffffff).toString(16)).slice(-8);
                }
                randomPart = randomPart.substring(0, 16);
            }
            return timestampPart + '-' + randomPart.substring(0, 4) + '-4'
                + randomPart.substring(4, 7) + '-a' + randomPart.substring(7, 10)
                + '-' + randomPart.substring(10, 16) + counterPart.substring(0, 6);
        }
        function generateExplainServerRequestId() {
            var cryptoObject = window.crypto || window.msCrypto;
            if (cryptoObject && cryptoObject.randomUUID) {
                return cryptoObject.randomUUID();
            }
            return createFallbackExplainServerRequestId();
        }
        function createInitialQueryPageState() {
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
        function isCompareLayout(layout) {
            return layout.mode === 'compare';
        }
        function getPaneMachineState(paneKey) {
            return paneKey === 'compare' ? queryPageState.comparePane : queryPageState.primaryPane;
        }
        function setPaneMachineState(paneKey, paneState) {
            if (paneKey === 'compare') {
                queryPageState.comparePane = paneState;
                return;
            }
            queryPageState.primaryPane = paneState;
        }
        function cloneStableExplanation(explanation) {
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
        function getStableExplanationKey(explanation) {
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
        function getStableExplanationContentKey(explanation) {
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
        function getPaneSnapshot(paneState) {
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
        function getPaneQueryHashFromInputs(paneKey, inputs) {
            return paneKey === 'compare' ? inputs.compareQueryHash : inputs.primaryQueryHash;
        }
        function getStaleReasons(explanation, paneKey, inputs) {
            var staleReasons = [];
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
        function createReadyPaneState(explanation, paneKey, inputs) {
            var staleReasons = getStaleReasons(explanation, paneKey, inputs);
            return {
                kind: 'ready',
                freshness: staleReasons.length ? 'stale' : 'current',
                staleReasons: staleReasons,
                explanation: cloneStableExplanation(explanation)
            };
        }
        function createErrorPaneState(message, mode, previous, paneKey, inputs) {
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
        function cloneExplanationWithQueryHash(explanation, queryHash) {
            var nextExplanation = cloneStableExplanation(explanation);
            if (nextExplanation) {
                nextExplanation.queryHash = queryHash;
            }
            return nextExplanation;
        }
        function restorePaneStateFromPrevious(paneState, paneKey, inputs, layout) {
            var previousExplanation = getPaneSnapshot(paneState);
            if (previousExplanation) {
                return createReadyPaneState(previousExplanation, paneKey, inputs);
            }
            if (paneKey === 'compare' && !isCompareLayout(layout)) {
                return { kind: 'inactive' };
            }
            return { kind: 'empty' };
        }
        function isPaneReadyCurrent(paneState) {
            return paneState.kind === 'ready' && paneState.freshness === 'current';
        }
        function signaturesMatch(left, right) {
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
        function getEventSignatureForPane(event, paneKey) {
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
        function createDiffModalState(kind, primaryPane, comparePane) {
            if (kind === 'closed') {
                return { kind: 'closed' };
            }
            return {
                kind: 'open',
                explanation: isPaneReadyCurrent(primaryPane) && isPaneReadyCurrent(comparePane) ? 'ready' : 'placeholder'
            };
        }
        function reducePaneState(paneState, paneKey, event, inputs, layout) {
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
                    return createErrorPaneState(event.message, paneState.previous ? 'refresh' : 'initial', paneState.previous, paneKey, inputs);
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
        function reduceDiffModalState(diffModalState, event, primaryPane, comparePane, layout, inputs) {
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
        function syncLegacyExplanationCache(paneKey) {
            var paneState = getPaneState(paneKey);
            var paneSnapshot = getPaneSnapshot(getPaneMachineState(paneKey));
            paneState.latestExplanation = paneSnapshot ? paneSnapshot.rawContent : '';
            paneState.latestExplanationFormat = paneSnapshot ? paneSnapshot.responseFormat : 'text';
        }
        function dispatchQueryPageEvent(event) {
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
                        : { kind: 'empty' },
                    comparePane: { kind: 'inactive' },
                    diffModal: { kind: 'closed' },
                    inputs: currentInputs,
                    compareQuerySeeded: false
                };
            }
            else if (event.type === 'TOGGLE_COMPARE') {
                var compareEnabled = isCompareLayout(queryPageState.layout);
                var nextLayout = compareEnabled ? { mode: 'single' } : { mode: 'compare', sidebar: 'closed' };
                var nextComparePane = compareEnabled ? { kind: 'inactive' } : queryPageState.comparePane;
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
            }
            else if (event.type === 'TOGGLE_SIDEBAR') {
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
                    diffModal: reduceDiffModalState(queryPageState.diffModal, event, queryPageState.primaryPane, queryPageState.comparePane, queryPageState.layout, currentInputs),
                    inputs: currentInputs,
                    compareQuerySeeded: queryPageState.compareQuerySeeded
                };
            }
            else {
                var nextPrimaryPane = reducePaneState(queryPageState.primaryPane, 'primary', event, currentInputs, queryPageState.layout);
                var nextComparePaneForEvent = reducePaneState(queryPageState.comparePane, 'compare', event, currentInputs, queryPageState.layout);
                queryPageState = {
                    lifecycle: 'ready',
                    layout: queryPageState.layout,
                    primaryPane: nextPrimaryPane,
                    comparePane: nextComparePaneForEvent,
                    diffModal: reduceDiffModalState(queryPageState.diffModal, event, nextPrimaryPane, nextComparePaneForEvent, queryPageState.layout, currentInputs),
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
        function loadNamespaces() {
            function toggleNamespaces() {
                workbench.query.setQueryValue(namespaces.text());
                currentQueryLn = queryLn;
            }
            var query = workbench.query.getQueryValue();
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
        query_1.loadNamespaces = loadNamespaces;
        /**
         *Fires when the query language is changed
         */
        function onQlChange() {
            workbench.query.loadNamespaces();
            workbench.query.updateYasqe();
        }
        query_1.onQlChange = onQlChange;
        /**
         * Invoked by the "clear" button. After confirming with the user,
         * clears the query text and loads the current repository and query
         * language name space declarations.
         */
        function resetNamespaces() {
            if (confirm('Click OK to clear the current query text and replace' +
                'it with the ' + $('#queryLn').val() +
                ' namespace declarations.')) {
                workbench.query.setQueryValue('');
                workbench.query.loadNamespaces();
            }
        }
        query_1.resetNamespaces = resetNamespaces;
        /**
         * Clear any contents of the save feedback field.
         */
        function clearFeedback() {
            $('#save-feedback').removeClass().text('');
        }
        query_1.clearFeedback = clearFeedback;
        /**
         * Clear the save feedback field, and look at the contents of the query name
         * field. Disables the save button if the field doesn't satisfy a given regular
         * expression. With a delay of 200 msec, to give enough time after
         * the event for the document to have changed. (Workaround for annoying browser
         * behavior.)
         */
        function handleNameChange() {
            setTimeout(function disableSaveIfNotValidName() {
                $('#save').prop('disabled', !/^[- \w]{1,32}$/.test($('#query-name').val()));
                workbench.query.clearFeedback();
            }, 0);
        }
        query_1.handleNameChange = handleNameChange;
        function getPaneState(paneKey) {
            return paneKey === 'compare' ? comparePaneState : primaryPaneState;
        }
        function getPaneQueryEditor(paneKey) {
            return paneKey === 'compare' ? compareYasqe : yasqe;
        }
        function setPaneQueryEditor(paneKey, editor) {
            if (paneKey === 'compare') {
                compareYasqe = editor;
                return;
            }
            yasqe = editor;
        }
        function getPaneQuerySelector(paneKey) {
            return '#' + getPaneState(paneKey).queryId;
        }
        function getPanePersistedQueryStorageKey(paneKey) {
            return paneKey === 'compare'
                ? 'yasqe_query-compare-pane_queryVal'
                : 'yasqe_query-primary-pane_queryVal';
        }
        function clearPanePersistedQuery(paneKey) {
            var storageKey = getPanePersistedQueryStorageKey(paneKey);
            try {
                window.localStorage.removeItem(storageKey);
            }
            catch (e) {
                // Ignore browsers where storage access is unavailable.
            }
            try {
                window.sessionStorage.removeItem(storageKey);
            }
            catch (e) {
                // Ignore browsers where storage access is unavailable.
            }
        }
        function persistPrimaryQueryEditorValue(queryEditor) {
            if (!queryEditor) {
                return;
            }
            queryEditor.save();
            var queryValue = queryEditor.getValue();
            document.getElementById(primaryPaneState.queryId).value = queryValue;
            setPrimaryQueryDraftSessionValue(queryValue);
        }
        function getPrimaryQueryDraftSessionStorageKey() {
            return 'workbench:query-draft:' + window.location.pathname;
        }
        function getPrimaryQueryDraftSessionValue() {
            var storageKey = getPrimaryQueryDraftSessionStorageKey();
            try {
                return window.sessionStorage.getItem(storageKey) || '';
            }
            catch (e) {
                // Ignore browsers where storage access is unavailable.
                return '';
            }
        }
        query_1.getPrimaryQueryDraftSessionValue = getPrimaryQueryDraftSessionValue;
        function setPrimaryQueryDraftSessionValue(queryValue) {
            var storageKey = getPrimaryQueryDraftSessionStorageKey();
            try {
                window.sessionStorage.setItem(storageKey, queryValue);
            }
            catch (e) {
                // Ignore browsers where storage access is unavailable.
            }
        }
        function getWorkbenchCookiePath() {
            var pathSegments = window.location.pathname.split('/');
            return pathSegments.length > 1 && pathSegments[1]
                ? '/' + pathSegments[1]
                : '/';
        }
        function setWorkbenchCookie(name, value) {
            document.cookie = name + '=' + encodeURIComponent(value || '') + '; path=' + getWorkbenchCookiePath();
        }
        function clearWorkbenchCookie(name) {
            document.cookie = name + '=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=' + getWorkbenchCookiePath();
        }
        function shouldPersistPrimaryQueryCookieValue(queryValue) {
            return !queryValue || queryValue.length <= 2048;
        }
        function persistPrimaryQueryValue() {
            var queryValue = getPaneRawQueryValue('primary');
            setPrimaryQueryDraftSessionValue(queryValue);
            if (!shouldPersistPrimaryQueryCookieValue(queryValue)) {
                return;
            }
            setWorkbenchCookie('query', queryValue);
            clearWorkbenchCookie('ref');
        }
        query_1.persistPrimaryQueryValue = persistPrimaryQueryValue;
        function getPaneRawQueryValue(paneKey) {
            var queryEditor = getPaneQueryEditor(paneKey);
            if (queryEditor) {
                return queryEditor.getValue();
            }
            return ($(getPaneQuerySelector(paneKey)).val() || '');
        }
        function getPaneQueryValue(paneKey) {
            return $.trim(getPaneRawQueryValue(paneKey));
        }
        function setPaneQueryValue(paneKey, queryString) {
            var normalizedQuery = queryString || '';
            var queryEditor = getPaneQueryEditor(paneKey);
            if (queryEditor) {
                queryEditor.setValue(normalizedQuery);
                return;
            }
            $(getPaneQuerySelector(paneKey)).val(normalizedQuery);
        }
        function savePaneQuery(paneKey) {
            var queryEditor = getPaneQueryEditor(paneKey);
            if (queryEditor) {
                queryEditor.save();
            }
        }
        function clearExplainSelection() {
            $('#explain').val('');
            $('#explain-level').val('Optimized');
        }
        function getPaneDisplayExplanation(paneState) {
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
        function getPaneStatusMessage(paneState) {
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
        function getPaneStatusClassName(paneState) {
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
        function getPaneOverlayMessage(paneState) {
            if (paneState && paneState.kind === 'loading' && paneState.mode === 'refresh') {
                return 'Refreshing explanation...';
            }
            return '';
        }
        function hasPrimaryExplanation() {
            return !!getPaneDisplayExplanation(queryPageState.primaryPane);
        }
        function refreshPaneStateWithInputs(paneState, paneKey, inputs) {
            if (paneState.kind === 'ready') {
                return createReadyPaneState(paneState.explanation, paneKey, inputs);
            }
            if (paneState.kind === 'error' && paneState.previous) {
                return createErrorPaneState(paneState.message, paneState.mode, paneState.previous, paneKey, inputs);
            }
            return paneState;
        }
        function syncStateAfterAsyncPrimaryQueryLoad() {
            if (!queryPageState || queryPageState.lifecycle !== 'ready') {
                return;
            }
            var currentInputs = collectCurrentInputs();
            var nextPrimaryPane = queryPageState.primaryPane;
            var primarySnapshot = getPaneSnapshot(queryPageState.primaryPane);
            if (primarySnapshot) {
                var reboundPrimaryExplanation = cloneExplanationWithQueryHash(primarySnapshot, currentInputs.primaryQueryHash);
                if (queryPageState.primaryPane.kind === 'error') {
                    nextPrimaryPane = createErrorPaneState(queryPageState.primaryPane.message, queryPageState.primaryPane.mode, reboundPrimaryExplanation, 'primary', currentInputs);
                }
                else if (queryPageState.primaryPane.kind === 'loading') {
                    nextPrimaryPane = {
                        kind: 'loading',
                        phase: queryPageState.primaryPane.phase,
                        mode: queryPageState.primaryPane.mode,
                        request: queryPageState.primaryPane.request,
                        previous: reboundPrimaryExplanation || undefined
                    };
                }
                else {
                    nextPrimaryPane = createReadyPaneState(reboundPrimaryExplanation, 'primary', currentInputs);
                }
            }
            var nextComparePane = refreshPaneStateWithInputs(queryPageState.comparePane, 'compare', currentInputs);
            queryPageState = {
                lifecycle: 'ready',
                layout: queryPageState.layout,
                primaryPane: nextPrimaryPane,
                comparePane: nextComparePane,
                diffModal: queryPageState.diffModal.kind === 'open'
                    && isCompareLayout(queryPageState.layout)
                    && currentInputs.primaryQueryHash.length
                    && currentInputs.compareQueryHash.length
                    ? createDiffModalState('open', nextPrimaryPane, nextComparePane)
                    : { kind: 'closed' },
                inputs: currentInputs,
                compareQuerySeeded: queryPageState.compareQuerySeeded
            };
            syncLegacyMachineFlags();
            syncLegacyExplanationCache('primary');
            syncLegacyExplanationCache('compare');
            renderQueryPageState();
        }
        function updateDownloadButtonState() {
            $('#download-explanation').prop('disabled', !(queryPageState.primaryPane.kind === 'ready' && queryPageState.primaryPane.freshness === 'current'));
        }
        function syncPrimaryExplanationControls() {
            var primaryPaneMachineState = queryPageState.primaryPane;
            var primaryControlsVisible = compareModeEnabled || primaryPaneMachineState.kind !== 'empty';
            var primaryActionsDisabled = primaryPaneMachineState.kind === 'loading' || activeComparePendingRequests > 0;
            $('#query-explanation-controls-row').toggle(primaryControlsVisible);
            $('#primary-explain-settings').toggle(primaryControlsVisible);
            $('#primary-explain-repeat-controls').toggle(primaryPaneMachineState.kind !== 'empty');
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
                ? sidebarToggle.attr('data-hide-label')
                : sidebarToggle.attr('data-show-label');
            sidebarToggle
                .show()
                .toggleClass('query-sidebar-toggle--nav-open', compareSidebarOpen)
                .attr('aria-hidden', 'false')
                .attr('aria-label', label)
                .attr('title', label)
                .removeAttr('tabindex');
        }
        function lockExplanationDimensions(paneKey) {
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
        function clearExplanationDimensionLock(paneKey) {
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
        function destroyDotPanZoom(paneKey) {
            var paneState = getPaneState(paneKey);
            if (paneState.dotPanZoomInstance && typeof paneState.dotPanZoomInstance.destroy === 'function') {
                paneState.dotPanZoomInstance.destroy();
            }
            paneState.dotPanZoomInstance = null;
        }
        function clearExplainButtonViewportRestoreState(paneKey) {
            var paneState = getPaneState(paneKey);
            paneState.explainButtonViewportTopBeforeRequest = null;
            paneState.explainButtonIdBeforeRequest = '';
        }
        function setExplainButtonsDisabled(disabled) {
            setExplainRequestButtonsDisabled('primary', disabled);
        }
        function hideExplainCancelButtons() {
            hideExplainRequestCancelButtons('primary');
        }
        function showExplainCancelButton(buttonId) {
            showExplainRequestCancelButton('primary', buttonId);
        }
        function hideExplainSpinners() {
            hideExplainRequestSpinner('primary');
        }
        function showExplainSpinner(buttonId) {
            showExplainRequestSpinner('primary', buttonId);
        }
        function clearExplainSpinnerDelayTimeout() {
            clearExplainRequestSpinnerDelayTimeout('primary');
        }
        function clearExplainSpinnerHideTimeout() {
            clearExplainRequestSpinnerHideTimeout('primary');
        }
        function beginComparePrimaryExplainWaitState(buttonId) {
            var targetButtonId = buttonId || '';
            beginExplainRequestUiWaitState('primary', activeCompareRequestId, targetButtonId, function (requestCounter, spinnerTargetId) {
                return requestCounter === activeCompareRequestId
                    && workbench.queryCancelPolicy.shouldShowComparePrimaryWaitState(targetButtonId, activeComparePendingRequests, !!activeCompareRequestId, spinnerTargetId);
            });
        }
        function finishComparePrimaryExplainWaitState(requestId) {
            finishExplainRequestUiWaitState('primary', requestId, function (requestCounter, spinnerTargetId) {
                return requestCounter === activeCompareRequestId
                    && spinnerTargetId === getExplainRequestUiState('primary').spinnerTargetId;
            });
        }
        function beginExplainRequest(buttonId, signature) {
            activeExplainRequestId = signature.requestId;
            activePrimaryRequestSignature = signature;
            primaryExplanationPending = true;
            beginExplainRequestUiWaitState('primary', signature.requestId, buttonId, function (requestCounter) {
                return requestCounter === activeExplainRequestId
                    && !!activePrimaryRequestSignature
                    && signaturesMatch(activePrimaryRequestSignature, signature);
            }, {
                disableButtons: true,
                onDelayElapsed: function () {
                    dispatchQueryPageEvent({ type: 'SPINNER_DELAY_ELAPSED', signature: signature });
                }
            });
            return signature.requestId;
        }
        function finishExplainRequest(requestId) {
            if (requestId !== activeExplainRequestId) {
                return;
            }
            primaryExplanationPending = false;
            syncPrimaryExplanationControls();
            finishExplainRequestUiWaitState('primary', requestId, function (requestCounter, spinnerTargetId) {
                return (!activePrimaryRequestSignature || requestCounter === activeExplainRequestId)
                    && spinnerTargetId === getExplainRequestUiState('primary').spinnerTargetId;
            }, {
                disableButtons: true
            });
        }
        function getExplainTriggerButtonElement(buttonId) {
            if (buttonId) {
                return document.getElementById(buttonId);
            }
            var activeElement = document.activeElement;
            if (activeElement
                && (activeElement.id === 'explain-trigger' || activeElement.id === 'rerun-explanation')) {
                return activeElement;
            }
            return document.getElementById('explain-trigger');
        }
        function captureExplainButtonViewportTop(paneKey, buttonId) {
            var explainButton = document.getElementById(buttonId)
                || getExplainTriggerButtonElement(buttonId);
            if (!explainButton) {
                clearExplainButtonViewportRestoreState(paneKey);
                return;
            }
            var paneState = getPaneState(paneKey);
            paneState.explainButtonViewportTopBeforeRequest = explainButton.getBoundingClientRect().top;
            paneState.explainButtonIdBeforeRequest = explainButton.id;
        }
        function restoreExplainButtonViewportTopIfNeeded(paneKey) {
            var paneState = getPaneState(paneKey);
            if (paneState.explainButtonViewportTopBeforeRequest === null) {
                return;
            }
            var explainButton = paneState.explainButtonIdBeforeRequest
                ? document.getElementById(paneState.explainButtonIdBeforeRequest)
                : getExplainTriggerButtonElement();
            if (!explainButton) {
                clearExplainButtonViewportRestoreState(paneKey);
                return;
            }
            window.requestAnimationFrame(function () {
                var currentButtonTop = explainButton.getBoundingClientRect().top;
                var scrollDelta = currentButtonTop - paneState.explainButtonViewportTopBeforeRequest;
                if (Math.abs(scrollDelta) > 1) {
                    window.scrollBy(0, scrollDelta);
                }
                clearExplainButtonViewportRestoreState(paneKey);
            });
        }
        function setExplanationDisplayMode(paneKey, format) {
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
            }
            else {
                explanation.show();
                dotView.hide();
                jsonView.hide();
                destroyDotPanZoom(paneKey);
            }
        }
        function applyDotPanZoom(paneKey, svgElement) {
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
        function clearRenderedExplanation(paneKey, pendingFormat) {
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
        function showExplainError(paneKey, message) {
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
        function renderDotView(paneKey, explanationText, format) {
            var paneState = getPaneState(paneKey);
            var dotView = $('#' + paneState.dotViewId);
            if (format === 'dot') {
                var paneMachineState = getPaneMachineState(paneKey);
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
                            pane: paneKey,
                            explanationKey: explanationKey
                        });
                    }
                    return;
                }
                if (!vizRenderer) {
                    vizRenderer = new Viz();
                }
                vizRenderer.renderSVGElement(explanationText).then(function (svgElement) {
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
                            pane: paneKey,
                            explanationKey: explanationKey
                        });
                    }
                    restoreExplainButtonViewportTopIfNeeded(paneKey);
                }).catch(function () {
                    if (pendingDotRenderKeys[paneKey] !== explanationContentKey) {
                        return;
                    }
                    vizRenderer = new Viz();
                    destroyDotPanZoom(paneKey);
                    dotView.html('<div class="error">Unable to render DOT graph.</div>');
                    if (displayExplanation) {
                        dispatchQueryPageEvent({
                            type: 'DOT_RENDER_FAIL',
                            pane: paneKey,
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
        function isJsonExpandable(value) {
            return value !== null && typeof value === 'object';
        }
        function getJsonSummary(value) {
            if (Array.isArray(value)) {
                return '[ ' + value.length + ' ]';
            }
            var keyCount = Object.keys(value).length;
            return '{ ' + keyCount + ' }';
        }
        function formatJsonKey(key) {
            if (key && key.charAt(0) === '[') {
                return key;
            }
            return '"' + key + '"';
        }
        function formatJsonArrayEntryKey(index, arrayEntry) {
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
        function parseNumericJsonValue(value) {
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
        function computePlanEntryPercentages(plans) {
            var planTimes = [];
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
            var percentages = [];
            for (var j = 0; j < planTimes.length; j++) {
                var currentPlanTime = planTimes[j];
                if (typeof currentPlanTime === 'number' && currentPlanTime >= 0) {
                    percentages.push((currentPlanTime / totalTimeActual) * 100);
                }
                else {
                    percentages.push(null);
                }
            }
            return percentages;
        }
        function formatPercentage(percentage) {
            return percentage.toFixed(1) + '%';
        }
        function createJsonScalarElement(value) {
            var valueElement = document.createElement('span');
            valueElement.className = 'query-json-node__value';
            if (value === null) {
                valueElement.className += ' query-json-node__value--null';
                valueElement.textContent = 'null';
            }
            else if (typeof value === 'string') {
                valueElement.className += ' query-json-node__value--string';
                valueElement.textContent = '"' + value + '"';
            }
            else if (typeof value === 'number') {
                valueElement.className += ' query-json-node__value--number';
                valueElement.textContent = value.toString();
            }
            else if (typeof value === 'boolean') {
                valueElement.className += ' query-json-node__value--boolean';
                valueElement.textContent = value ? 'true' : 'false';
            }
            else {
                valueElement.className += ' query-json-node__value--other';
                valueElement.textContent = String(value);
            }
            return valueElement;
        }
        function createJsonTreeNode(value, key, depth, percentageOfPlansTotal) {
            var node = document.createElement('div');
            node.className = 'query-json-node';
            var line = document.createElement('div');
            line.className = 'query-json-node__line';
            node.appendChild(line);
            var expandable = isJsonExpandable(value);
            var toggle = null;
            if (expandable) {
                toggle = document.createElement('button');
                toggle.type = 'button';
                toggle.className = 'query-json-node__toggle';
                line.appendChild(toggle);
            }
            else {
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
                    var arrayEntryPercentages = [];
                    if (key === 'plans') {
                        arrayEntryPercentages = computePlanEntryPercentages(value);
                    }
                    for (var index = 0; index < value.length; index++) {
                        childrenElement.appendChild(createJsonTreeNode(value[index], formatJsonArrayEntryKey(index, value[index]), depth + 1, arrayEntryPercentages[index]));
                    }
                }
                else {
                    var objectKeys = Object.keys(value);
                    for (var i = 0; i < objectKeys.length; i++) {
                        var childKey = objectKeys[i];
                        childrenElement.appendChild(createJsonTreeNode(value[childKey], childKey, depth + 1));
                    }
                }
                toggle.addEventListener('click', function () {
                    var isCollapsed = node.classList.toggle('query-json-node--collapsed');
                    var expanded = !isCollapsed;
                    toggle.setAttribute('aria-expanded', expanded ? 'true' : 'false');
                    toggle.textContent = expanded ? '▾' : '▸';
                });
            }
            else {
                line.appendChild(createJsonScalarElement(value));
            }
            return node;
        }
        function renderJsonExplanationTree(explanationJson) {
            var treeElement = document.createElement('div');
            treeElement.className = 'query-json-tree';
            treeElement.appendChild(createJsonTreeNode(explanationJson, '', 0));
            return treeElement;
        }
        function renderJsonView(paneKey, explanationText, format) {
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
            }
            catch (parseError) {
                jsonView.append($('<div class="error">Unable to parse JSON explanation. Showing raw content.</div>'));
                jsonView.append($('<pre class="query-json-view__raw"></pre>').text(explanationText));
            }
            restoreExplainButtonViewportTopIfNeeded(paneKey);
        }
        function renderExplanation(paneKey, explanationText, format) {
            var paneState = getPaneState(paneKey);
            var normalizedFormat = (format || 'text').toLowerCase();
            $('#' + paneState.explanationRowId).show();
            if (paneState.explanationControlsRowId) {
                $('#' + paneState.explanationControlsRowId).show();
            }
            if (normalizedFormat === 'dot' || normalizedFormat === 'json') {
                $('#' + paneState.explanationId).text('').attr('data-format', normalizedFormat);
            }
            else {
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
        function renderPanePresentation(paneKey) {
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
            $('#' + paneState.copyButtonId).prop('disabled', !paneDisplayExplanation);
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
                    renderDiffView('#query-diff-explanation', queryPageState.primaryPane.explanation.rawContent, queryPageState.comparePane.explanation.rawContent, diffNotReadyLabel);
                }
                else {
                    $('#query-diff-explanation').text(diffNotReadyLabel);
                }
            }
        }
        function getErrorMessage(error) {
            if (typeof error === 'string') {
                return error;
            }
            if (error && typeof error.message === 'string') {
                return error.message;
            }
            if (error && typeof error === 'object') {
                try {
                    return JSON.stringify(error);
                }
                catch (e) {
                    // fall through and stringify the value directly
                }
            }
            return String(error);
        }
        function getExplainErrorMessage(jqXHR, textStatus, errorThrown) {
            var response = jqXHR.responseJSON;
            if (response && response.error) {
                return getErrorMessage(response.error);
            }
            var responseText = jqXHR.responseText;
            if (responseText) {
                try {
                    var parsedResponse = JSON.parse(responseText);
                    if (parsedResponse && parsedResponse.error) {
                        return getErrorMessage(parsedResponse.error);
                    }
                }
                catch (e) {
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
        function serializeExplainFormData(queryValue, level, format, serverRequestId) {
            var serializedForm = $('form[action="query"]').serializeArray();
            var seenAction = false;
            var seenExplain = false;
            var seenFormat = false;
            var seenInfer = false;
            var seenPreserveQueryOrder = false;
            var seenQuery = false;
            var seenExplainRequestId = false;
            for (var i = 0; i < serializedForm.length; i++) {
                if (serializedForm[i].name === 'action') {
                    serializedForm[i].value = 'explain';
                    seenAction = true;
                }
                else if (serializedForm[i].name === 'explain') {
                    serializedForm[i].value = level;
                    seenExplain = true;
                }
                else if (serializedForm[i].name === 'explain-format') {
                    serializedForm[i].value = format;
                    seenFormat = true;
                }
                else if (serializedForm[i].name === 'infer') {
                    seenInfer = true;
                }
                else if (serializedForm[i].name === 'preserve-query-order') {
                    seenPreserveQueryOrder = true;
                }
                else if (serializedForm[i].name === 'query') {
                    serializedForm[i].value = queryValue;
                    seenQuery = true;
                }
                else if (serializedForm[i].name === 'explain-request-id') {
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
            if (!seenInfer) {
                serializedForm.push({ name: 'infer', value: 'false' });
            }
            if (!seenPreserveQueryOrder) {
                serializedForm.push({ name: 'preserve-query-order', value: 'false' });
            }
            if (!seenQuery) {
                serializedForm.push({ name: 'query', value: queryValue });
            }
            if (!seenExplainRequestId) {
                serializedForm.push({ name: 'explain-request-id', value: serverRequestId });
            }
            return $.param(serializedForm);
        }
        function serializeCancelExplainFormData(serverRequestId) {
            return $.param([
                { name: 'action', value: 'cancel-explain' },
                { name: 'explain-request-id', value: serverRequestId }
            ]);
        }
        function serializeTraceFormData(queryValue, serverRequestId) {
            var serializedForm = $('form[action="query"]').serializeArray();
            var seenAction = false;
            var seenTrace = false;
            var seenInfer = false;
            var seenPreserveQueryOrder = false;
            var seenQuery = false;
            var seenTraceRequestId = false;
            for (var i = 0; i < serializedForm.length; i++) {
                if (serializedForm[i].name === 'action') {
                    serializedForm[i].value = 'trace';
                    seenAction = true;
                }
                else if (serializedForm[i].name === 'trace') {
                    serializedForm[i].value = 'true';
                    seenTrace = true;
                }
                else if (serializedForm[i].name === 'infer') {
                    seenInfer = true;
                }
                else if (serializedForm[i].name === 'preserve-query-order') {
                    seenPreserveQueryOrder = true;
                }
                else if (serializedForm[i].name === 'query') {
                    serializedForm[i].value = queryValue;
                    seenQuery = true;
                }
                else if (serializedForm[i].name === 'trace-request-id') {
                    serializedForm[i].value = serverRequestId;
                    seenTraceRequestId = true;
                }
            }
            if (!seenAction) {
                serializedForm.push({ name: 'action', value: 'trace' });
            }
            if (!seenTrace) {
                serializedForm.push({ name: 'trace', value: 'true' });
            }
            if (!seenInfer) {
                serializedForm.push({ name: 'infer', value: 'false' });
            }
            if (!seenPreserveQueryOrder) {
                serializedForm.push({ name: 'preserve-query-order', value: 'false' });
            }
            if (!seenQuery) {
                serializedForm.push({ name: 'query', value: queryValue });
            }
            if (!seenTraceRequestId) {
                serializedForm.push({ name: 'trace-request-id', value: serverRequestId });
            }
            return $.param(serializedForm);
        }
        function serializeCancelTraceFormData(serverRequestId) {
            return $.param([
                { name: 'action', value: 'cancel-trace' },
                { name: 'trace-request-id', value: serverRequestId }
            ]);
        }
        function postCancelExplain(serverRequestId) {
            if (!serverRequestId) {
                return;
            }
            $.ajax({
                url: 'query',
                type: 'POST',
                data: serializeCancelExplainFormData(serverRequestId)
            });
        }
        function postCancelTrace(serverRequestId) {
            if (!serverRequestId) {
                return;
            }
            $.ajax({
                url: 'query',
                type: 'POST',
                data: serializeCancelTraceFormData(serverRequestId)
            });
        }
        function createStableExplanationFromResponse(signature, response, fallbackFormat) {
            var responseFormat = getNormalizedExplainFormat(response.format || fallbackFormat || 'text');
            var explanationText = response.content || '';
            var explanationView = 'text';
            if (responseFormat === 'json') {
                explanationView = 'jsonTree';
                if (explanationText) {
                    try {
                        JSON.parse(explanationText);
                    }
                    catch (parseError) {
                        explanationView = 'jsonRawFallback';
                    }
                }
            }
            else if (responseFormat === 'dot') {
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
        function applyExplainResponseToPane(paneKey, signature, response, fallbackFormat) {
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
        function ajaxExplain(signature) {
            activeExplainJqXHR = $.ajax({
                url: 'query',
                type: 'POST',
                dataType: 'json',
                data: serializeExplainFormData(getPaneRawQueryValue('primary'), signature.level, signature.format, signature.serverRequestId),
                error: function (jqXHR, textStatus, errorThrown) {
                    if (textStatus !== 'abort' && activePrimaryRequestSignature && signaturesMatch(activePrimaryRequestSignature, signature)) {
                        dispatchQueryPageEvent({
                            type: 'EXPLAIN_ERROR',
                            signature: signature,
                            message: getExplainErrorMessage(jqXHR, textStatus, errorThrown)
                        });
                    }
                },
                success: function (response) {
                    if (!activePrimaryRequestSignature || !signaturesMatch(activePrimaryRequestSignature, signature)) {
                        return;
                    }
                    applyExplainResponseToPane('primary', signature, response, signature.format);
                },
                complete: function () {
                    if (!activePrimaryRequestSignature || !signaturesMatch(activePrimaryRequestSignature, signature)) {
                        return;
                    }
                    activePrimaryRequestSignature = null;
                    activeExplainJqXHR = null;
                    finishExplainRequest(signature.requestId);
                }
            });
        }
        function getExplanationDownloadMimeType(format) {
            if (format === 'json') {
                return 'application/json';
            }
            if (format === 'dot') {
                return 'text/vnd.graphviz';
            }
            return 'text/plain';
        }
        function getExplanationDownloadExtension(format) {
            if (format === 'json') {
                return 'json';
            }
            if (format === 'dot') {
                return 'dot';
            }
            return 'txt';
        }
        function hideCompareExplainSpinner() {
            hideExplainRequestSpinner('compare');
        }
        function showCompareExplainSpinner() {
            showExplainRequestSpinner('compare', 'explain-compare-trigger');
        }
        function hideCompareExplainCancelButton() {
            hideExplainRequestCancelButtons('compare');
        }
        function showCompareExplainCancelButton() {
            showExplainRequestCancelButton('compare', 'explain-compare-trigger');
        }
        function clearCompareExplainSpinnerDelayTimeout() {
            clearExplainRequestSpinnerDelayTimeout('compare');
        }
        function clearCompareExplainSpinnerHideTimeout() {
            clearExplainRequestSpinnerHideTimeout('compare');
        }
        function setCompareExplainButtonsDisabled(disabled) {
            setExplainRequestButtonsDisabled('compare', disabled);
        }
        function updateCompareActionState() {
            var bothQueriesAvailable = compareModeEnabled
                && getPaneQueryValue('primary').length > 0
                && getPaneQueryValue('compare').length > 0;
            $('#query-diff-trigger').prop('disabled', !bothQueriesAvailable || activeComparePendingRequests > 0);
        }
        function refreshVisibleQueryEditors() {
            window.requestAnimationFrame(function () {
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
        function shouldAutoExplainComparePaneOnOpen() {
            if (!compareModeEnabled || !isPaneReadyCurrent(queryPageState.primaryPane)) {
                return false;
            }
            var selectedLevel = getNormalizedExplainLevel($('#explain-level').val() || 'Optimized');
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
        function handleQueryPageInputChange(eventType) {
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
            clearTraceState();
            dispatchQueryPageEvent({ type: eventType });
        }
        function notifyQueryPageInputChange(eventType) {
            if (eventType === 'PRIMARY_QUERY_CHANGED'
                || eventType === 'COMPARE_QUERY_CHANGED'
                || eventType === 'EXPLAIN_LEVEL_CHANGED'
                || eventType === 'EXPLAIN_FORMAT_CHANGED') {
                handleQueryPageInputChange(eventType);
            }
        }
        query_1.notifyQueryPageInputChange = notifyQueryPageInputChange;
        function splitDiffLines(text) {
            if (!text) {
                return [];
            }
            return text.replace(/\r\n/g, '\n').split('\n');
        }
        function splitDiffChunkLines(text) {
            if (!text) {
                return [];
            }
            var normalizedText = text.replace(/\r\n/g, '\n');
            var lines = normalizedText.split('\n');
            if (normalizedText.charAt(normalizedText.length - 1) === '\n') {
                lines.pop();
            }
            return lines;
        }
        function createDiffSegments(leftText, rightText, rowType) {
            var rowText = rowType === 'added' ? rightText : leftText;
            if (!Diff || typeof Diff.diffWordsWithSpace !== 'function') {
                return rowText ? [{ text: rowText, changed: false }] : [];
            }
            var wordChunks = Diff.diffWordsWithSpace(leftText || '', rightText || '');
            var diffSegments = [];
            for (var i = 0; i < wordChunks.length; i++) {
                var wordChunk = wordChunks[i];
                var includeChunk = rowType === 'added' ? !wordChunk.removed : !wordChunk.added;
                if (!includeChunk || !wordChunk.value) {
                    continue;
                }
                diffSegments.push({
                    text: wordChunk.value,
                    changed: rowType === 'added' ? !!wordChunk.added : !!wordChunk.removed
                });
            }
            if (!diffSegments.length && rowText) {
                diffSegments.push({ text: rowText, changed: true });
            }
            return diffSegments;
        }
        function createDiffRow(marker, text, rowType, segments) {
            var diffRow = {
                marker: marker,
                text: text,
                type: rowType
            };
            if (segments && segments.length) {
                diffRow.segments = segments;
            }
            return diffRow;
        }
        function appendPairedDiffRows(diffRows, removedLines, addedLines) {
            var pairCount = Math.max(removedLines.length, addedLines.length);
            for (var lineIndex = 0; lineIndex < pairCount; lineIndex++) {
                var removedLine = removedLines[lineIndex];
                var addedLine = addedLines[lineIndex];
                if (typeof removedLine === 'string' && typeof addedLine === 'string') {
                    diffRows.push(createDiffRow('-', removedLine, 'removed', createDiffSegments(removedLine, addedLine, 'removed')));
                    diffRows.push(createDiffRow('+', addedLine, 'added', createDiffSegments(removedLine, addedLine, 'added')));
                }
                else if (typeof removedLine === 'string') {
                    diffRows.push(createDiffRow('-', removedLine, 'removed', [{ text: removedLine, changed: true }]));
                }
                else if (typeof addedLine === 'string') {
                    diffRows.push(createDiffRow('+', addedLine, 'added', [{ text: addedLine, changed: true }]));
                }
            }
        }
        function buildDiffRows(leftText, rightText) {
            if (!Diff || typeof Diff.diffLines !== 'function') {
                return [];
            }
            var diffRows = [];
            var diffChunks = Diff.diffLines(leftText || '', rightText || '');
            for (var i = 0; i < diffChunks.length; i++) {
                var diffChunk = diffChunks[i];
                var nextDiffChunk = diffChunks[i + 1];
                if (diffChunk.removed && nextDiffChunk && nextDiffChunk.added) {
                    appendPairedDiffRows(diffRows, splitDiffChunkLines(diffChunk.value || ''), splitDiffChunkLines(nextDiffChunk.value || ''));
                    i += 1;
                    continue;
                }
                if (diffChunk.added && nextDiffChunk && nextDiffChunk.removed) {
                    appendPairedDiffRows(diffRows, splitDiffChunkLines(nextDiffChunk.value || ''), splitDiffChunkLines(diffChunk.value || ''));
                    i += 1;
                    continue;
                }
                var type = diffChunk.added ? 'added' : (diffChunk.removed ? 'removed' : 'context');
                var marker = diffChunk.added ? '+' : (diffChunk.removed ? '-' : ' ');
                var lines = splitDiffChunkLines(diffChunk.value || '');
                for (var lineIndex = 0; lineIndex < lines.length; lineIndex++) {
                    diffRows.push(createDiffRow(marker, lines[lineIndex], type));
                }
            }
            return diffRows;
        }
        function renderDiffView(targetSelector, leftText, rightText, placeholder) {
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
                var contentElement = $('<span class="query-diff-row__content"></span>');
                rowElement.append($('<span class="query-diff-row__marker"></span>').text(diffRow.marker));
                if (diffRow.segments && diffRow.segments.length) {
                    for (var segmentIndex = 0; segmentIndex < diffRow.segments.length; segmentIndex++) {
                        var diffSegment = diffRow.segments[segmentIndex];
                        var segmentElement = $('<span class="query-diff-row__segment"></span>').text(diffSegment.text);
                        if (diffSegment.changed) {
                            segmentElement.addClass('query-diff-row__segment--changed');
                        }
                        contentElement.append(segmentElement);
                    }
                }
                else {
                    contentElement.text(diffRow.text);
                }
                rowElement.append(contentElement);
                target.append(rowElement);
            }
        }
        function beginCompareExplainRequest(requestSignatures, triggerButtonId) {
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
            beginComparePrimaryExplainWaitState(triggerButtonId);
            $('#query-diff-trigger').prop('disabled', true);
            beginExplainRequestUiWaitState('compare', activeCompareRequestId, triggerButtonId || 'explain-compare-trigger', function (requestCounter) {
                return requestCounter === activeCompareRequestId
                    && activeComparePendingRequests > 0
                    && !!activeCompareRequestId;
            }, {
                disableButtons: true,
                onDelayElapsed: function () {
                    for (var signatureKey in activeCompareRequestSignatures) {
                        if (activeCompareRequestSignatures.hasOwnProperty(signatureKey)) {
                            dispatchQueryPageEvent({
                                type: 'SPINNER_DELAY_ELAPSED',
                                signature: activeCompareRequestSignatures[signatureKey]
                            });
                        }
                    }
                }
            });
            return activeCompareRequestId;
        }
        function finishCompareExplainRequest(requestId) {
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
            finishComparePrimaryExplainWaitState(requestId);
            finishExplainRequestUiWaitState('compare', requestId, function (requestCounter) {
                return requestCounter === activeCompareRequestId;
            }, {
                disableButtons: true,
                onAfterHide: function () {
                    updateCompareActionState();
                }
            });
        }
        function enqueueCompareExplanationRequest(signature) {
            var compareRequest = $.ajax({
                url: 'query',
                type: 'POST',
                dataType: 'json',
                data: serializeExplainFormData(getPaneRawQueryValue(signature.pane), signature.level, signature.format, signature.serverRequestId),
                error: function (jqXHR, textStatus, errorThrown) {
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
                success: function (response) {
                    if (!activeCompareRequestSignatures[signature.pane]
                        || !signaturesMatch(activeCompareRequestSignatures[signature.pane], signature)) {
                        return;
                    }
                    applyExplainResponseToPane(signature.pane, signature, response, signature.format);
                },
                complete: function () {
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
        function ajaxSave(overwrite) {
            var feedback = $('#save-feedback');
            var url = [];
            url[url.length] = 'query';
            if (overwrite) {
                url[url.length] = document.all ? ';' : '?';
                url[url.length] = 'overwrite=true&';
            }
            var href = url.join('');
            var form = $('form[action="query"]');
            $.ajax({
                url: href,
                type: 'POST',
                dataType: 'json',
                data: form.serialize(),
                timeout: 5000,
                error: function (jqXHR, textStatus, errorThrown) {
                    feedback.removeClass().addClass('error');
                    if (textStatus == 'timeout') {
                        feedback.text('Timed out waiting for response. Uncertain if save occured.');
                    }
                    else {
                        feedback.text('Save Request Failed: Error Type = ' +
                            textStatus + ', HTTP Status Text = "' + errorThrown + '"');
                    }
                },
                success: function (response) {
                    if (response.accessible) {
                        if (response.written) {
                            feedback.removeClass().addClass('success');
                            feedback.text('Query saved.');
                        }
                        else {
                            if (response.existed) {
                                if (confirm('Query name exists. Click OK to overwrite.')) {
                                    ajaxSave(true);
                                }
                                else {
                                    feedback.removeClass().addClass('error');
                                    feedback.text('Cancelled overwriting existing query.');
                                }
                            }
                        }
                    }
                    else {
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
        function doSubmit() {
            //if yasqe is instantiated, make sure we save the value to the textarea
            if (yasqe)
                yasqe.save();
            $('#include-query-text').val('false');
            var allowPageToSubmitForm = false;
            var save = ($('#action').val() == 'save');
            if (save) {
                clearExplainSelection();
                ajaxSave(false);
            }
            else {
                var url = [];
                url[url.length] = 'query';
                if (document.all) {
                    url[url.length] = ';';
                }
                else {
                    url[url.length] = '?';
                }
                workbench.addParam(url, 'action');
                workbench.addParam(url, 'queryLn');
                workbench.addParam(url, 'query');
                workbench.addParam(url, 'limit_query');
                workbench.addParam(url, 'query-timeout');
                workbench.addParam(url, 'infer');
                workbench.addParam(url, 'preserve-query-order');
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
                }
                else {
                    // GET using the constructed URL, method exits here
                    document.location.href = href;
                }
            }
            // Value returned to form submit event. If not true, prevents normal form
            // submission.
            return allowPageToSubmitForm;
        }
        query_1.doSubmit = doSubmit;
        function runExplain(level, buttonId) {
            if (compareModeEnabled) {
                runCompareExplain(buttonId || 'explain-trigger');
                return;
            }
            var effectiveLevel = getNormalizedExplainLevel(level || $('#explain-level').val() || 'Optimized');
            var explainButton = getExplainTriggerButtonElement(buttonId);
            if (explainButton && explainButton.disabled) {
                return;
            }
            $('#explain-level').val(effectiveLevel);
            captureExplainButtonViewportTop('primary', buttonId);
            savePaneQuery('primary');
            activeExplainRequestId += 1;
            var signature = createRequestSignature('primary', explainButton && explainButton.id === 'rerun-explanation' ? 'primary-rerun' : 'primary-explain', activeExplainRequestId);
            dispatchQueryPageEvent({ type: 'REQUEST_EXPLAIN', signature: signature });
            beginExplainRequest(explainButton ? explainButton.id : 'explain-trigger', signature);
            ajaxExplain(signature);
        }
        query_1.runExplain = runExplain;
        function cancelExplain() {
            var cancelAction = workbench.queryCancelPolicy.getExplainCancelAction(activeComparePendingRequests, !!activePrimaryRequestSignature);
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
        query_1.cancelExplain = cancelExplain;
        function runCompareExplain(buttonId) {
            if (!compareModeEnabled) {
                return;
            }
            if (activePrimaryRequestSignature) {
                cancelExplain();
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
        query_1.runCompareExplain = runCompareExplain;
        function requestComparePaneExplanation(level) {
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
        function cancelCompareExplain() {
            var cancelledSignatures = [];
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
            getExplainRequestUiState('primary').spinnerTargetId = '';
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
        query_1.cancelCompareExplain = cancelCompareExplain;
        function downloadExplanation() {
            if (queryPageState.primaryPane.kind !== 'ready' || queryPageState.primaryPane.freshness !== 'current') {
                return;
            }
            var primaryExplanation = queryPageState.primaryPane.explanation;
            var format = primaryExplanation.responseFormat || $('#explain-format').val() || 'text';
            var extension = getExplanationDownloadExtension(format);
            var mimeType = getExplanationDownloadMimeType(format);
            var blob = new Blob([primaryExplanation.rawContent], { type: mimeType + ';charset=utf-8' });
            var link = document.createElement('a');
            var selectedLevel = $('#explain-level').val() || 'query';
            link.download = 'query-explanation-' + selectedLevel.toLowerCase() + '.' + extension;
            link.href = window.URL.createObjectURL(blob);
            document.body.appendChild(link);
            link.click();
            window.URL.revokeObjectURL(link.href);
            document.body.removeChild(link);
        }
        query_1.downloadExplanation = downloadExplanation;
        function copyExplanation(paneKey) {
            var normalizedPaneKey = paneKey === 'compare' ? 'compare' : 'primary';
            var paneDisplayExplanation = getPaneDisplayExplanation(getPaneMachineState(normalizedPaneKey));
            if (!paneDisplayExplanation || !paneDisplayExplanation.rawContent) {
                return false;
            }
            if (!window.navigator
                || !window.navigator.clipboard
                || typeof window.navigator.clipboard.writeText !== 'function') {
                return false;
            }
            return window.navigator.clipboard.writeText(paneDisplayExplanation.rawContent);
        }
        query_1.copyExplanation = copyExplanation;
        function generateTraceServerRequestId() {
            traceServerRequestIdCounter += 1;
            return 'workbench-trace-' + traceServerRequestIdCounter + '-' + new Date().getTime();
        }
        function stopTracePlayback() {
            if (activeTracePlaybackTimer !== null) {
                window.clearInterval(activeTracePlaybackTimer);
                activeTracePlaybackTimer = null;
            }
            if (currentTraceState) {
                currentTraceState = workbench.queryTracePlayer.setPlaying(currentTraceState, false);
            }
        }
        function setTraceRequestUi(active) {
            $('#trace-trigger').prop('disabled', active);
            $('#trace-trigger-spinner').toggleClass('query-explain-spinner--visible', active);
            $('#trace-trigger-cancel')
                .toggleClass('query-explain-cancel--visible', active)
                .prop('disabled', !active);
        }
        function setTraceStatus(message, isError) {
            var status = $('#query-trace-status');
            status.text(message || '');
            status
                .toggleClass('query-explanation-status--visible', !!message)
                .toggleClass('query-explanation-status--loading', !!message && !isError)
                .toggleClass('query-explanation-status--error', !!message && !!isError);
        }
        function createTraceMetaItem(text) {
            return $('<span></span>')
                .addClass('query-trace-meta-item')
                .text(text);
        }
        function renderTraceMeta(frame, patternCount, frameIndex, frameCount) {
            var summary = $('#query-trace-summary');
            var frameLabel = $('#query-trace-frame-label');
            summary.empty();
            frameLabel.empty();
            if (patternCount > 0) {
                summary.append(createTraceMetaItem(patternCount + ' patterns'));
            }
            if (frameCount > 0) {
                summary.append(createTraceMetaItem(frameCount + ' frames'));
            }
            if (frameCount <= 0 || !frame) {
                return;
            }
            frameLabel
                .append(createTraceMetaItem('Frame ' + (frameIndex + 1) + ' of ' + frameCount))
                .append(createTraceMetaItem('Event ' + frame.event));
            if (frame.patternIndex >= 0) {
                frameLabel.append(createTraceMetaItem('Line ' + (frame.patternIndex + 1)));
            }
        }
        function renderTraceStepLabel(frame, frameIndex, frameCount) {
            var stepLabel = $('#query-trace-step-label');
            stepLabel.text('');
            if (frameCount <= 0 || !frame) {
                return;
            }
            var label = 'Step ' + (frameIndex + 1) + ' / ' + frameCount + '  ·  '
                + frame.event.charAt(0).toUpperCase() + frame.event.substring(1);
            if (frame.patternIndex >= 0) {
                label += '  ·  Line ' + (frame.patternIndex + 1);
            }
            stepLabel.text(label);
        }
        function renderTraceEmptyState(message) {
            $('#query-trace-patterns')
                .empty()
                .append($('<div class="query-trace-empty"></div>')
                .text(message));
        }
        function clearTraceMotionState() {
            if (activeTraceBridgeTimer !== null) {
                window.clearTimeout(activeTraceBridgeTimer);
                activeTraceBridgeTimer = null;
            }
            previousTraceFrameIndex = null;
            previousTraceActivePatternIndex = null;
            previousTraceMarkerOffset = null;
            if (pendingTraceMarkerAnimationFrame !== null) {
                window.cancelAnimationFrame(pendingTraceMarkerAnimationFrame);
                pendingTraceMarkerAnimationFrame = null;
            }
        }
        function getSnapshotActivePatternIndex(snapshot) {
            return snapshot && snapshot.frame && typeof snapshot.frame.patternIndex === 'number'
                && snapshot.frame.patternIndex >= 0
                ? snapshot.frame.patternIndex
                : null;
        }
        function getRollbackWaveDelayMs(queryLine, activePatternIndex, rollbackFromPatternIndex) {
            if (!queryLine || !queryLine.pattern || activePatternIndex === null || rollbackFromPatternIndex === null) {
                return -1;
            }
            if (activePatternIndex >= rollbackFromPatternIndex) {
                return -1;
            }
            if (queryLine.pattern.index < activePatternIndex || queryLine.pattern.index > rollbackFromPatternIndex) {
                return -1;
            }
            return (rollbackFromPatternIndex - queryLine.pattern.index) * 48;
        }
        function computeTraceActiveMarkerOffset(activePatternIndex) {
            return ((activePatternIndex + 1) * 4) + 'em';
        }
        function renderTraceActiveMarker(query, activePatternIndex) {
            if (pendingTraceMarkerAnimationFrame !== null) {
                window.cancelAnimationFrame(pendingTraceMarkerAnimationFrame);
                pendingTraceMarkerAnimationFrame = null;
            }
            if (activePatternIndex === null) {
                previousTraceMarkerOffset = null;
                return;
            }
            var marker = $('<span class="query-trace-query__active-marker" aria-hidden="true"></span>');
            query.append(marker);
            var targetOffset = computeTraceActiveMarkerOffset(activePatternIndex);
            var startOffset = previousTraceMarkerOffset !== null ? previousTraceMarkerOffset : targetOffset;
            marker.css('transform', 'translate3d(0, ' + startOffset + ', 0)');
            previousTraceMarkerOffset = targetOffset;
            pendingTraceMarkerAnimationFrame = window.requestAnimationFrame(function () {
                pendingTraceMarkerAnimationFrame = null;
                marker.css('transform', 'translate3d(0, ' + targetOffset + ', 0)');
            });
        }
        function createTraceQueryShellLine(text, modifierClass) {
            var line = $('<div class="query-trace-query__line query-trace-query__line--shell"></div>');
            if (modifierClass) {
                line.addClass(modifierClass);
            }
            line.append($('<span class="query-trace-query__gutter"></span>')
                .append($('<span class="query-trace-query__gutter-label"></span>')));
            line.append($('<span class="query-trace-query__line-content"></span>')
                .text(text));
            return line;
        }
        function appendTraceQueryToken(content, token) {
            if (!token.variableName) {
                content.append(document.createTextNode(token.text));
                return;
            }
            var variable = $('<span class="query-trace-query__variable"></span>');
            if (token.bindingValue) {
                var binding = $('<span class="query-trace-query__binding"></span>')
                    .text(token.bindingValue);
                if (token.bindingState) {
                    binding.addClass('query-trace-query__binding--' + token.bindingState);
                }
                variable
                    .addClass('query-trace-query__variable--bound')
                    .append(binding);
            }
            variable.append($('<span class="query-trace-query__variable-text"></span>')
                .text(token.text));
            content.append(variable);
        }
        function createTraceQueryPatternLine(queryLine, activePatternIndex, rollbackFromPatternIndex) {
            var kindClass = 'query-trace-query__line--pattern';
            if (queryLine.kind === 'filter') {
                kindClass = 'query-trace-query__line--filter';
            }
            else if (queryLine.kind === 'optionalStart') {
                kindClass = 'query-trace-query__line--optional-start';
            }
            else if (queryLine.kind === 'optionalEnd') {
                kindClass = 'query-trace-query__line--optional-end';
            }
            var line = $('<div class="query-trace-query__line"></div>')
                .addClass(kindClass)
                .toggleClass('query-trace-query__line--active', queryLine.active)
                .toggleClass('query-trace-query__line--pending', queryLine.pending);
            var rollbackWaveDelayMs = getRollbackWaveDelayMs(queryLine, activePatternIndex, rollbackFromPatternIndex);
            if (rollbackWaveDelayMs >= 0) {
                line
                    .addClass('query-trace-query__line--rollback')
                    .css('--query-trace-rollback-delay', rollbackWaveDelayMs + 'ms');
            }
            line.append($('<span class="query-trace-query__gutter"></span>')
                .append($('<span class="query-trace-query__gutter-label"></span>')
                .text(queryLine.gutterLabel || '')));
            var content = $('<span class="query-trace-query__line-content"></span>');
            for (var i = 0; i < queryLine.tokens.length; i++) {
                appendTraceQueryToken(content, queryLine.tokens[i]);
            }
            line.append(content);
            return line;
        }
        function renderTraceQuerySnapshot(snapshot) {
            var patternContainer = $('#query-trace-patterns');
            patternContainer.empty();
            var query = $('<div class="query-trace-query"></div>');
            if (snapshot.direction === 'rollback') {
                query.addClass('query-trace-query--rollback');
            }
            var activePatternIndex = snapshot && typeof snapshot.activePatternIndex === 'number'
                && snapshot.activePatternIndex >= 0
                ? snapshot.activePatternIndex
                : null;
            var rollbackFromPatternIndex = snapshot.direction === 'rollback'
                && previousTraceActivePatternIndex !== null
                ? previousTraceActivePatternIndex
                : null;
            query.append(createTraceQueryShellLine(snapshot.queryHead, 'query-trace-query__line--head'));
            for (var i = 0; i < snapshot.queryLines.length; i++) {
                query.append(createTraceQueryPatternLine(snapshot.queryLines[i], activePatternIndex, rollbackFromPatternIndex));
            }
            query.append(createTraceQueryShellLine(snapshot.queryTail, 'query-trace-query__line--tail'));
            patternContainer.append(query);
            renderTraceActiveMarker(query, activePatternIndex);
            previousTraceActivePatternIndex = activePatternIndex;
        }
        function continueTraceRollbackBridge(bridgeSnapshots, finalSnapshot, finalFrameIndex, bridgeIndex) {
            if (bridgeIndex >= bridgeSnapshots.length) {
                activeTraceBridgeTimer = null;
                renderTraceQuerySnapshot(finalSnapshot);
                previousTraceFrameIndex = finalFrameIndex;
                return;
            }
            renderTraceQuerySnapshot(bridgeSnapshots[bridgeIndex]);
            activeTraceBridgeTimer = window.setTimeout(function () {
                continueTraceRollbackBridge(bridgeSnapshots, finalSnapshot, finalFrameIndex, bridgeIndex + 1);
            }, 180);
        }
        function renderTraceQuery(snapshot) {
            if (activeTraceBridgeTimer !== null) {
                window.clearTimeout(activeTraceBridgeTimer);
                activeTraceBridgeTimer = null;
            }
            var currentFrameIndex = currentTraceState ? currentTraceState.frameIndex : null;
            var bridgeSnapshots = [];
            if (currentTrace && currentTraceState && previousTraceFrameIndex !== null
                && previousTraceFrameIndex !== currentFrameIndex) {
                var previousState = workbench.queryTracePlayer.seek(workbench.queryTracePlayer.createState(currentTrace), previousTraceFrameIndex);
                bridgeSnapshots = workbench.queryTracePlayer.createRollbackBridgeSnapshots(previousState, currentTraceState);
            }
            if (!bridgeSnapshots.length) {
                renderTraceQuerySnapshot(snapshot);
                previousTraceFrameIndex = currentFrameIndex;
                return;
            }
            continueTraceRollbackBridge(bridgeSnapshots, snapshot, currentFrameIndex, 0);
        }
        function clearTraceSurface() {
            clearTraceMotionState();
            renderTraceEmptyState('Run Trace to step through the optimized query.');
            $('#query-trace-frame-label').empty();
            $('#query-trace-summary').empty();
            $('#query-trace-step-label').text('');
            $('#query-trace-scrubber').val('0').prop('max', 0).prop('disabled', true);
            $('#trace-playback-toggle').prop('disabled', true).val('Play');
            $('#trace-previous').prop('disabled', true);
            $('#trace-next').prop('disabled', true);
            $('#trace-reset').prop('disabled', true);
            $('#download-trace').prop('disabled', true);
        }
        function renderTracePanel() {
            var traceRow = $('#query-trace-row');
            traceRow.show();
            if (!currentTrace) {
                clearTraceSurface();
                return;
            }
            if (currentTrace.error) {
                clearTraceSurface();
                setTraceStatus(currentTrace.error.message || 'Trace request failed.', true);
                return;
            }
            if (!currentTraceState) {
                currentTraceState = workbench.queryTracePlayer.createState(currentTrace);
            }
            var snapshot = workbench.queryTracePlayer.snapshot(currentTraceState);
            var frameCount = workbench.queryTracePlayer.getFrameCount(currentTraceState);
            $('#query-trace-scrubber')
                .prop('disabled', frameCount <= 1)
                .prop('max', Math.max(0, frameCount - 1))
                .val(String(currentTraceState.frameIndex));
            $('#trace-playback-toggle')
                .prop('disabled', frameCount <= 1)
                .val(currentTraceState.playing ? 'Pause' : 'Play');
            $('#trace-previous').prop('disabled', !workbench.queryTracePlayer.canStepBackward(currentTraceState));
            $('#trace-next').prop('disabled', !workbench.queryTracePlayer.canStepForward(currentTraceState));
            $('#trace-reset').prop('disabled', frameCount <= 0);
            $('#download-trace').prop('disabled', false);
            var frame = snapshot.frame;
            renderTraceMeta(frame, currentTrace.patterns.length, currentTraceState.frameIndex, frameCount);
            renderTraceStepLabel(frame, currentTraceState.frameIndex, frameCount);
            if (!frame) {
                renderTraceEmptyState('No trace frames were returned.');
                return;
            }
            renderTraceQuery(snapshot);
            setTraceStatus('');
        }
        function applyTraceResponse(response, requestId) {
            if (requestId !== activeTraceRequestId) {
                return;
            }
            stopTracePlayback();
            currentTrace = workbench.queryTracePlayer.normalizeTrace(response);
            currentTraceState = currentTrace.error ? null : workbench.queryTracePlayer.createState(currentTrace);
            renderTracePanel();
        }
        function clearTraceState() {
            stopTracePlayback();
            currentTrace = null;
            currentTraceState = null;
            setTraceStatus('');
            clearTraceSurface();
        }
        function runTrace() {
            savePaneQuery('primary');
            if (activeTraceJqXHR) {
                cancelTrace();
            }
            clearTraceState();
            activeTraceRequestId += 1;
            var requestId = activeTraceRequestId;
            activeTraceServerRequestId = generateTraceServerRequestId();
            $('#query-trace-row').show();
            setTraceRequestUi(true);
            setTraceStatus('Tracing query...');
            activeTraceJqXHR = $.ajax({
                url: 'query',
                type: 'POST',
                dataType: 'json',
                data: serializeTraceFormData(getPaneRawQueryValue('primary'), activeTraceServerRequestId),
                error: function (jqXHR, textStatus, errorThrown) {
                    if (textStatus === 'abort' || requestId !== activeTraceRequestId) {
                        return;
                    }
                    stopTracePlayback();
                    currentTrace = {
                        patterns: [],
                        frames: [],
                        error: {
                            code: 'traceRequestFailed',
                            message: getExplainErrorMessage(jqXHR, textStatus, errorThrown)
                        }
                    };
                    currentTraceState = null;
                    renderTracePanel();
                },
                success: function (response) {
                    applyTraceResponse(response, requestId);
                },
                complete: function () {
                    if (requestId !== activeTraceRequestId) {
                        return;
                    }
                    activeTraceJqXHR = null;
                    activeTraceServerRequestId = '';
                    setTraceRequestUi(false);
                }
            });
        }
        query_1.runTrace = runTrace;
        function cancelTrace() {
            if (!activeTraceJqXHR && !activeTraceServerRequestId) {
                return;
            }
            postCancelTrace(activeTraceServerRequestId);
            if (activeTraceJqXHR) {
                activeTraceJqXHR.abort();
                activeTraceJqXHR = null;
            }
            activeTraceServerRequestId = '';
            stopTracePlayback();
            setTraceRequestUi(false);
            setTraceStatus('Trace cancelled.', true);
        }
        query_1.cancelTrace = cancelTrace;
        function toggleTracePlayback() {
            if (!currentTraceState || workbench.queryTracePlayer.getFrameCount(currentTraceState) <= 1) {
                return;
            }
            if (currentTraceState.playing) {
                stopTracePlayback();
                renderTracePanel();
                return;
            }
            currentTraceState = workbench.queryTracePlayer.setPlaying(currentTraceState, true);
            renderTracePanel();
            activeTracePlaybackTimer = window.setInterval(function () {
                if (!currentTraceState || !workbench.queryTracePlayer.canStepForward(currentTraceState)) {
                    stopTracePlayback();
                    renderTracePanel();
                    return;
                }
                currentTraceState = workbench.queryTracePlayer.next(currentTraceState);
                currentTraceState = workbench.queryTracePlayer.setPlaying(currentTraceState, true);
                renderTracePanel();
            }, 900);
        }
        query_1.toggleTracePlayback = toggleTracePlayback;
        function stepTrace(delta) {
            if (!currentTraceState) {
                return;
            }
            stopTracePlayback();
            currentTraceState = delta < 0
                ? workbench.queryTracePlayer.previous(currentTraceState)
                : workbench.queryTracePlayer.next(currentTraceState);
            renderTracePanel();
        }
        query_1.stepTrace = stepTrace;
        function resetTracePlayback() {
            if (!currentTraceState) {
                return;
            }
            stopTracePlayback();
            currentTraceState = workbench.queryTracePlayer.reset(currentTraceState);
            renderTracePanel();
        }
        query_1.resetTracePlayback = resetTracePlayback;
        function seekTrace(frameIndexValue) {
            if (!currentTraceState) {
                return;
            }
            var nextFrameIndex = parseInt(frameIndexValue, 10);
            if (isNaN(nextFrameIndex)) {
                return;
            }
            stopTracePlayback();
            currentTraceState = workbench.queryTracePlayer.seek(currentTraceState, nextFrameIndex);
            renderTracePanel();
        }
        query_1.seekTrace = seekTrace;
        function downloadTrace() {
            if (!currentTrace || currentTrace.error) {
                return;
            }
            var blob = new Blob([JSON.stringify(currentTrace, null, 2)], { type: 'application/json;charset=utf-8' });
            var link = document.createElement('a');
            link.download = 'query-trace.json';
            link.href = window.URL.createObjectURL(blob);
            document.body.appendChild(link);
            link.click();
            window.URL.revokeObjectURL(link.href);
            document.body.removeChild(link);
        }
        query_1.downloadTrace = downloadTrace;
        function initializeExplanationView() {
            var initialExplanation = $('#query-explanation').text();
            var initialFormat = getNormalizedExplainFormat($('#query-explanation').attr('data-format') || $('#explain-format').val() || 'text');
            var hydratedExplanation = null;
            if (initialExplanation) {
                hydratedExplanation = createStableExplanationFromResponse({
                    requestId: 0,
                    serverRequestId: 'initial-primary-explanation',
                    pane: 'primary',
                    source: 'primary-explain',
                    queryHash: buildQueryHash(getPaneRawQueryValue('primary')),
                    level: getNormalizedExplainLevel($('#explain-level').val()),
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
        query_1.initializeExplanationView = initializeExplanationView;
        function setQueryValue(queryString) {
            setPaneQueryValue('primary', $.trim(queryString));
        }
        query_1.setQueryValue = setQueryValue;
        function applyLoadedPrimaryQuery(queryString) {
            var normalizedQuery = $.trim(queryString || '');
            var previousPrimaryQuery = getPaneRawQueryValue('primary');
            if (activeComparePendingRequests > 0) {
                cancelCompareExplain();
            }
            if (activePrimaryRequestSignature) {
                cancelExplain();
            }
            setPaneQueryValue('primary', normalizedQuery);
            if (compareModeEnabled
                && queryPageState
                && queryPageState.compareQuerySeeded
                && getPaneRawQueryValue('compare') === previousPrimaryQuery) {
                setPaneQueryValue('compare', normalizedQuery);
            }
            persistPrimaryQueryValue();
            syncStateAfterAsyncPrimaryQueryLoad();
        }
        query_1.applyLoadedPrimaryQuery = applyLoadedPrimaryQuery;
        function getQueryValue() {
            return getPaneQueryValue('primary');
        }
        query_1.getQueryValue = getQueryValue;
        function getYasqe() {
            return yasqe;
        }
        query_1.getYasqe = getYasqe;
        function updateYasqe() {
            if ($("#queryLn").val() == "SPARQL") {
                initYasqe();
                if (compareModeEnabled || compareYasqe) {
                    ensureCompareYasqe();
                }
            }
            else {
                closeYasqe();
                closeCompareYasqe();
            }
            updateCompareActionState();
        }
        query_1.updateYasqe = updateYasqe;
        function initPaneYasqe(paneKey, clearFeedbackOnChange) {
            workbench.yasqeHelper.setupCompleters(sparqlNamespaces);
            var paneEditor = YASQE.fromTextArea(document.getElementById(getPaneState(paneKey).queryId), {
                consumeShareLink: null, //don't try to parse the url args. this is already done by the addLoad function below
                persistent: null
            });
            clearPanePersistedQuery(paneKey);
            $(paneEditor.getWrapperElement()).css({
                "fontSize": "14px",
                "width": "100%",
                "maxWidth": "100%",
                "boxSizing": "border-box"
            });
            paneEditor.on('change', function () {
                if (paneKey === 'compare') {
                    clearPanePersistedQuery('compare');
                }
                else {
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
        function toggleCompareMode() {
            if (!compareModeEnabled) {
                if (!queryPageState.compareQuerySeeded && !getPaneQueryValue('compare')) {
                    setPaneQueryValue('compare', getPaneRawQueryValue('primary'));
                    queryPageState.compareQuerySeeded = true;
                }
                dispatchQueryPageEvent({ type: 'TOGGLE_COMPARE' });
                if ($("#queryLn").val() == "SPARQL") {
                    ensureCompareYasqe();
                }
            }
            else {
                closeDiffModal();
                resetComparePaneState();
                dispatchQueryPageEvent({ type: 'TOGGLE_COMPARE' });
            }
            syncCompareModeVisibility();
            if (compareModeEnabled && shouldAutoExplainComparePaneOnOpen()) {
                var selectedExplainLevel = $('#explain-level').val() || 'Optimized';
                requestComparePaneExplanation(selectedExplainLevel);
            }
        }
        query_1.toggleCompareMode = toggleCompareMode;
        function toggleCompareSidebar() {
            if (!compareModeEnabled) {
                return;
            }
            dispatchQueryPageEvent({ type: 'TOGGLE_SIDEBAR' });
        }
        query_1.toggleCompareSidebar = toggleCompareSidebar;
        function openDiffModal() {
            if (!compareModeEnabled) {
                return;
            }
            lastDiffTriggerElement = document.getElementById('query-diff-trigger');
            dispatchQueryPageEvent({ type: 'OPEN_DIFF' });
            document.getElementById('query-diff-close').focus();
        }
        query_1.openDiffModal = openDiffModal;
        function closeDiffModal() {
            dispatchQueryPageEvent({ type: 'CLOSE_DIFF' });
            if (lastDiffTriggerElement) {
                lastDiffTriggerElement.focus();
            }
        }
        query_1.closeDiffModal = closeDiffModal;
        function initializeCompareUi() {
            clearPanePersistedQuery('compare');
            diffNotReadyLabel = $.trim($('#query-diff-explanation').text());
            resetComparePaneState();
            syncCompareModeVisibility();
        }
        query_1.initializeCompareUi = initializeCompareUi;
        function refreshCompareActionState() {
            updateCompareActionState();
        }
        query_1.refreshCompareActionState = refreshCompareActionState;
        query_1.testing = {
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
            getPrimaryQueryDraftSessionValue: getPrimaryQueryDraftSessionValue,
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
            getInternalState: function () {
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
            resetInternalState: function () {
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
                resetExplainRequestUiState('primary');
                resetExplainRequestUiState('compare');
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
            setInternalState: function (state) {
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
    })(query = workbench.query || (workbench.query = {}));
})(workbench || (workbench = {}));
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
    function getParameterFromUrl(param) {
        var href = document.location.href;
        var elements = href.substring(href.indexOf('?') + 1).substring(href.indexOf(';') + 1).split(decodeURIComponent('%26'));
        var result = '';
        for (var i = 0; elements.length - i; i++) {
            var pair = elements[i].split('=');
            var value = decodeURIComponent(pair[1]).replace(/\+/g, ' ');
            if (pair[0] == param) {
                result = value;
            }
        }
        return result;
    }
    function getParameterFromUrlOrCookie(param) {
        var result = getParameterFromUrl(param);
        if (!result) {
            result = workbench.getCookie(param);
        }
        return result;
    }
    function getQueryTextFromServer(queryParam, refParam) {
        $.getJSON('query', {
            action: "get",
            query: queryParam,
            ref: refParam
        }, function (response) {
            if (response.queryText) {
                workbench.query.applyLoadedPrimaryQuery(response.queryText);
            }
        });
    }
    workbench.queryTracePlayer.setNamespaces(traceNamespaces);
    //Start with initializing our YASQE instance, given that 'SPARQL' is the selected query language
    //(all the following 'set' and 'get' SPARQL query functions require an instantiated yasqe instance
    workbench.query.updateYasqe();
    // Populate the query text area with the value of the URL query parameter,
    // only if it is present. If it is not present in the URL query, then
    // looks for the 'query' cookie, and sets it from that. (The cookie
    // enables re-populating the text field with the previous query when the
    // user returns via the browser back button.)
    var query = getParameterFromUrl('query');
    if (query) {
        var ref = getParameterFromUrl('ref');
        if (ref == 'id' || ref == 'hash') {
            getQueryTextFromServer(query, ref);
        }
        else {
            workbench.query.setQueryValue(query);
            workbench.query.persistPrimaryQueryValue();
        }
    }
    else {
        var initialQueryValue = workbench.query.getQueryValue();
        if (initialQueryValue) {
            workbench.query.persistPrimaryQueryValue();
        }
        else {
            var sessionDraft = workbench.query.getPrimaryQueryDraftSessionValue();
            if (sessionDraft) {
                workbench.query.setQueryValue(sessionDraft);
            }
            else {
                query = getParameterFromUrlOrCookie('query');
                if (query) {
                    var fallbackRef = getParameterFromUrlOrCookie('ref');
                    if (fallbackRef == 'id' || fallbackRef == 'hash') {
                        getQueryTextFromServer(query, fallbackRef);
                    }
                    else {
                        workbench.query.setQueryValue(query);
                        workbench.query.persistPrimaryQueryValue();
                    }
                }
            }
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
    var addHandler = function (id, callback) {
        $('#' + id).click(function setAction() {
            $('#action').val(id);
            if (callback) {
                callback();
            }
        });
    };
    addHandler('exec', function () {
        $('#explain').val('');
        $('#explain-level').val('');
    });
    addHandler('save', function () {
        $('#explain').val('');
        $('#explain-level').val('');
    });
    $('#copy-explanation').click(function () {
        workbench.query.copyExplanation('primary');
    });
    $('#copy-explanation-compare').click(function () {
        workbench.query.copyExplanation('compare');
    });
    $('#download-explanation').click(workbench.query.downloadExplanation);
    $('#download-trace').click(workbench.query.downloadTrace);
    $('#query-trace-scrubber').on('input change', function () {
        workbench.query.seekTrace($(this).val());
    });
    $('#trace-playback-toggle').click(function () {
        workbench.query.toggleTracePlayback();
    });
    $('#trace-previous').click(function () {
        workbench.query.stepTrace(-1);
    });
    $('#trace-next').click(function () {
        workbench.query.stepTrace(1);
    });
    $('#trace-reset').click(function () {
        workbench.query.resetTracePlayback();
    });
    $('#query-trace-row').hide();
    // Add event handlers to the save name field to react to changes in it.
    $('#query-name').bind('keydown cut paste', workbench.query.handleNameChange);
    // Add event handlers to the query text area to react to changes in it.
    function deferInputChange(handler) {
        return function () {
            window.setTimeout(handler, 0);
        };
    }
    $('#query').bind('keydown cut paste change', deferInputChange(function () {
        workbench.query.clearFeedback();
        workbench.query.notifyQueryPageInputChange('PRIMARY_QUERY_CHANGED');
    }));
    $('#query-compare').bind('keydown cut paste change', deferInputChange(function () {
        workbench.query.clearFeedback();
        workbench.query.notifyQueryPageInputChange('COMPARE_QUERY_CHANGED');
    }));
    $('#explain-level').change(function () {
        workbench.query.notifyQueryPageInputChange('EXPLAIN_LEVEL_CHANGED');
    });
    $('#explain-format').change(function () {
        workbench.query.notifyQueryPageInputChange('EXPLAIN_FORMAT_CHANGED');
    });
    $('#query-diff-modal').click(function (event) {
        if (event.target && event.target.id === 'query-diff-modal') {
            workbench.query.closeDiffModal();
        }
    });
    $(document).keydown(function (event) {
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
//# sourceMappingURL=query.js.map