/// <reference path="template.ts" />
/// <reference path="jquery.d.ts" />
/// <reference path="yasqe.d.ts" />
/// <reference path="yasqeHelper.ts" />
// WARNING: Do not edit the *.js version of this file. Instead, always edit the
// corresponding *.ts source in the ts subfolder, and then invoke the
// compileTypescript.sh bash script to generate new *.js and *.js.map files.
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
        var explainSpinnerVisibleSince = 0;
        var explainSpinnerTargetId = '';
        var explainSpinnerDelayTimeoutId = null;
        var explainSpinnerHideTimeoutId = null;
        var compareExplainSpinnerVisibleSince = 0;
        var compareExplainSpinnerDelayTimeoutId = null;
        var compareExplainSpinnerHideTimeoutId = null;
        var activeExplainRequestId = 0;
        var activeExplainJqXHR = null;
        var primaryExplanationPending = false;
        var activeCompareRequestId = 0;
        var activeComparePendingRequests = 0;
        var activeCompareExplainJqXHRs = [];
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
                pane: paneKey,
                source: source,
                queryHash: getPaneQueryHashFromInputs(paneKey, currentInputs),
                level: currentInputs.explainLevel,
                format: currentInputs.explainFormat,
                groupId: groupId
            };
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
            document.getElementById(primaryPaneState.queryId).value = queryEditor.getValue();
            if (YASQE.storeQuery) {
                YASQE.storeQuery(queryEditor);
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
        function persistPrimaryQueryValue() {
            setWorkbenchCookie('query', getPaneRawQueryValue('primary'));
            clearWorkbenchCookie('ref');
        }
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
            $('#explain-trigger').prop('disabled', disabled);
            $('#rerun-explanation').prop('disabled', disabled);
        }
        function hideExplainCancelButtons() {
            $('.query-explain-cancel')
                .removeClass('query-explain-cancel--visible')
                .attr('aria-hidden', 'true')
                .prop('disabled', true);
        }
        function showExplainCancelButton(buttonId) {
            var cancelButtonId = buttonId === 'rerun-explanation'
                ? '#rerun-explanation-cancel'
                : '#explain-trigger-cancel';
            hideExplainCancelButtons();
            $(cancelButtonId)
                .addClass('query-explain-cancel--visible')
                .attr('aria-hidden', 'false')
                .prop('disabled', false);
        }
        function hideExplainSpinners() {
            $('.query-explain-spinner')
                .removeClass('query-explain-spinner--visible')
                .attr('aria-hidden', 'true');
            explainSpinnerVisibleSince = 0;
        }
        function showExplainSpinner(buttonId) {
            var spinnerId = buttonId === 'rerun-explanation'
                ? '#rerun-explanation-spinner'
                : '#explain-trigger-spinner';
            hideExplainSpinners();
            explainSpinnerTargetId = buttonId;
            $(spinnerId)
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
        function beginExplainRequest(buttonId, signature) {
            activeExplainRequestId = signature.requestId;
            activePrimaryRequestSignature = signature;
            primaryExplanationPending = true;
            explainSpinnerTargetId = buttonId;
            clearExplainSpinnerDelayTimeout();
            clearExplainSpinnerHideTimeout();
            hideExplainSpinners();
            hideExplainCancelButtons();
            setExplainButtonsDisabled(true);
            explainSpinnerDelayTimeoutId = window.setTimeout(function () {
                if (!activePrimaryRequestSignature || !signaturesMatch(activePrimaryRequestSignature, signature)) {
                    return;
                }
                explainSpinnerDelayTimeoutId = null;
                dispatchQueryPageEvent({ type: 'SPINNER_DELAY_ELAPSED', signature: signature });
                showExplainSpinner(buttonId);
            }, 1000);
            return signature.requestId;
        }
        function finishExplainRequest(requestId) {
            if (requestId !== activeExplainRequestId) {
                return;
            }
            primaryExplanationPending = false;
            syncPrimaryExplanationControls();
            setExplainButtonsDisabled(false);
            hideExplainCancelButtons();
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
                explainSpinnerHideTimeoutId = window.setTimeout(function () {
                    if (requestId !== activeExplainRequestId || spinnerTargetId !== explainSpinnerTargetId) {
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
        function getExplainErrorMessage(jqXHR, textStatus, errorThrown) {
            var response = jqXHR.responseJSON;
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
        function serializeExplainFormData(queryValue, level, format) {
            var serializedForm = $('form[action="query"]').serializeArray();
            var seenAction = false;
            var seenExplain = false;
            var seenFormat = false;
            var seenQuery = false;
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
                else if (serializedForm[i].name === 'query') {
                    serializedForm[i].value = queryValue;
                    seenQuery = true;
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
            return $.param(serializedForm);
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
                data: serializeExplainFormData(getPaneRawQueryValue('primary'), signature.level, signature.format),
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
            $('#explain-compare-trigger')
                .attr('aria-busy', 'false');
            $('#explain-compare-trigger-icon')
                .removeClass('query-compare-action__icon--spinning');
            hideCompareExplainCancelButton();
            compareExplainSpinnerVisibleSince = 0;
        }
        function showCompareExplainSpinner() {
            $('#explain-compare-trigger')
                .attr('aria-busy', 'true');
            $('#explain-compare-trigger-icon')
                .addClass('query-compare-action__icon--spinning');
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
        function setCompareExplainButtonsDisabled(disabled) {
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
        function buildDiffRows(leftText, rightText) {
            var leftLines = splitDiffLines(leftText);
            var rightLines = splitDiffLines(rightText);
            var matrix = [];
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
                    }
                    else {
                        matrix[leftIndex][rightIndex] = Math.max(matrix[leftIndex + 1][rightIndex], matrix[leftIndex][rightIndex + 1]);
                    }
                }
            }
            var diffRows = [];
            var leftPointer = 0;
            var rightPointer = 0;
            while (leftPointer < leftLength && rightPointer < rightLength) {
                if (leftLines[leftPointer] === rightLines[rightPointer]) {
                    diffRows.push({ marker: ' ', text: leftLines[leftPointer], type: 'context' });
                    leftPointer += 1;
                    rightPointer += 1;
                }
                else if (matrix[leftPointer + 1][rightPointer] >= matrix[leftPointer][rightPointer + 1]) {
                    diffRows.push({ marker: '-', text: leftLines[leftPointer], type: 'removed' });
                    leftPointer += 1;
                }
                else {
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
                rowElement.append($('<span class="query-diff-row__marker"></span>').text(diffRow.marker));
                rowElement.append($('<span></span>').text(diffRow.text));
                target.append(rowElement);
            }
        }
        function beginCompareExplainRequest(requestSignatures) {
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
            setCompareExplainButtonsDisabled(true);
            $('#query-diff-trigger').prop('disabled', true);
            compareExplainSpinnerDelayTimeoutId = window.setTimeout(function () {
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
            setCompareExplainButtonsDisabled(false);
            clearCompareExplainSpinnerDelayTimeout();
            clearCompareExplainSpinnerHideTimeout();
            if (!compareExplainSpinnerVisibleSince) {
                hideCompareExplainSpinner();
                updateCompareActionState();
                return;
            }
            var remainingSpinnerTime = 1000 - (Date.now() - compareExplainSpinnerVisibleSince);
            if (remainingSpinnerTime > 0) {
                compareExplainSpinnerHideTimeoutId = window.setTimeout(function () {
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
        function enqueueCompareExplanationRequest(signature) {
            var compareRequest = $.ajax({
                url: 'query',
                type: 'POST',
                dataType: 'json',
                data: serializeExplainFormData(getPaneRawQueryValue(signature.pane), signature.level, signature.format),
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
            var cancelledSignature = activePrimaryRequestSignature;
            if (!cancelledSignature) {
                return;
            }
            dispatchQueryPageEvent({
                type: 'CANCEL_EXPLAIN',
                pane: 'primary',
                signature: cancelledSignature
            });
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
            beginCompareExplainRequest(requestSignatures);
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
            beginCompareExplainRequest([compareSignature]);
            enqueueCompareExplanationRequest(compareSignature);
        }
        function cancelCompareExplain() {
            var cancelledSignatures = [];
            for (var paneKey in activeCompareRequestSignatures) {
                if (activeCompareRequestSignatures.hasOwnProperty(paneKey) && activeCompareRequestSignatures[paneKey]) {
                    cancelledSignatures.push(activeCompareRequestSignatures[paneKey]);
                }
            }
            activeCompareRequestId += 1;
            activeComparePendingRequests = 0;
            for (var i = 0; i < activeCompareExplainJqXHRs.length; i++) {
                activeCompareExplainJqXHRs[i].abort();
            }
            activeCompareExplainJqXHRs = [];
            activeCompareRequestSignatures = {};
            clearCompareExplainSpinnerDelayTimeout();
            clearCompareExplainSpinnerHideTimeout();
            hideCompareExplainSpinner();
            setCompareExplainButtonsDisabled(false);
            for (var j = 0; j < cancelledSignatures.length; j++) {
                dispatchQueryPageEvent({
                    type: 'CANCEL_EXPLAIN',
                    pane: cancelledSignatures[j].pane,
                    signature: cancelledSignatures[j]
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
        function initializeExplanationView() {
            var initialExplanation = $('#query-explanation').text();
            var initialFormat = getNormalizedExplainFormat($('#query-explanation').attr('data-format') || $('#explain-format').val() || 'text');
            var hydratedExplanation = null;
            if (initialExplanation) {
                hydratedExplanation = createStableExplanationFromResponse({
                    requestId: 0,
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
    function getParameterFromUrlOrCookie(param) {
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
        }
        else {
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
    $('#download-explanation').click(workbench.query.downloadExplanation);
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