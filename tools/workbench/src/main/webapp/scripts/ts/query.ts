/// <reference path="template.ts" />
/// <reference path="jquery.d.ts" />
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
        var explainSpinnerVisibleSince = 0;
        var explainSpinnerTargetId = '';
        var explainSpinnerDelayTimeoutId: number = null;
        var explainSpinnerHideTimeoutId: number = null;
        var activeExplainRequestId = 0;
        var activeExplainJqXHR: JQueryXHR = null;
        var activeCompareRequestId = 0;
        var activeComparePendingRequests = 0;
        var activeCompareExplainJqXHRs: JQueryXHR[] = [];
        var compareModeEnabled = false;
        var compareSidebarOpen = false;
        var compareQuerySeeded = false;
        var diffNotReadyLabel = '';
        var lastDiffTriggerElement: HTMLElement = null;

        interface ExplanationPaneState {
            key: string;
            queryId: string;
            errorId: string;
            explanationRowId: string;
            explanationControlsRowId?: string;
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
            explanationId: 'query-explanation-compare',
            dotViewId: 'query-explanation-dot-view-compare',
            jsonViewId: 'query-explanation-json-view-compare',
            latestExplanation: '',
            latestExplanationFormat: 'text',
            dotPanZoomInstance: null,
            explainButtonViewportTopBeforeRequest: null,
            explainButtonIdBeforeRequest: ''
        };

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

        function hasPrimaryExplanation(): boolean {
            return primaryPaneState.latestExplanation.length > 0;
        }

        function updateDownloadButtonState() {
            $('#download-explanation').prop('disabled', !primaryPaneState.latestExplanation);
        }

        function syncPrimaryExplanationControls() {
            var primaryExplanationAvailable = hasPrimaryExplanation();
            var primaryControlsVisible = primaryExplanationAvailable || compareModeEnabled;
            $('#query-explanation-controls-row').toggle(primaryControlsVisible);
            $('#primary-explain-settings').toggle(primaryControlsVisible);
            $('#primary-explain-repeat-controls').toggle(primaryExplanationAvailable);
            $('#download-explanation').toggle(primaryControlsVisible);
            $('#compare-toggle').toggle(primaryExplanationAvailable || compareModeEnabled);
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

        function showExplainSpinner(buttonId: string) {
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

        function beginExplainRequest(buttonId: string): number {
            activeExplainRequestId += 1;
            var requestId = activeExplainRequestId;
            explainSpinnerTargetId = buttonId;
            clearExplainSpinnerDelayTimeout();
            clearExplainSpinnerHideTimeout();
            hideExplainSpinners();
            hideExplainCancelButtons();
            setExplainButtonsDisabled(true);
            explainSpinnerDelayTimeoutId = window.setTimeout(function() {
                if (requestId !== activeExplainRequestId) {
                    return;
                }
                explainSpinnerDelayTimeoutId = null;
                showExplainSpinner(buttonId);
            }, 1000);
            return requestId;
        }

        function finishExplainRequest(requestId: number) {
            if (requestId !== activeExplainRequestId) {
                return;
            }
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
                explainSpinnerHideTimeoutId = window.setTimeout(function() {
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
            $('#' + paneState.explanationRowId).show();
            if (paneState.explanationControlsRowId) {
                $('#' + paneState.explanationControlsRowId).show();
            }
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
                setExplanationDisplayMode(paneKey, 'dot');
                if (!explanationText) {
                    dotView.empty().show();
                    restoreExplainButtonViewportTopIfNeeded(paneKey);
                    return;
                }
                dotView.html('<div>Rendering DOT graph...</div>').show();
                if (typeof Viz === 'undefined') {
                    dotView.html('<div class="error">Graphviz visualizer script not loaded.</div>');
                    return;
                }
                if (!vizRenderer) {
                    vizRenderer = new Viz();
                }
                vizRenderer.renderSVGElement(explanationText).then(function(svgElement: SVGElement) {
                    $(svgElement).css({
                        width: '100%',
                        height: '100%',
                        maxWidth: '100%',
                        maxHeight: 'none',
                        display: 'block'
                    });
                    dotView.empty().append(svgElement).show();
                    applyDotPanZoom(paneKey, svgElement);
                    restoreExplainButtonViewportTopIfNeeded(paneKey);
                }).catch(function() {
                    vizRenderer = new Viz();
                    destroyDotPanZoom(paneKey);
                    dotView.html('<div class="error">Unable to render DOT graph.</div>');
                    restoreExplainButtonViewportTopIfNeeded(paneKey);
                });
                return;
            }
            destroyDotPanZoom(paneKey);
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

        function serializeExplainFormData(queryValue: string, level: string, format: string): string {
            var serializedForm: any[] = <any[]>$('form[action="query"]').serializeArray();
            var seenAction = false;
            var seenExplain = false;
            var seenFormat = false;
            var seenQuery = false;
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

        function applyExplainResponseToPane(paneKey: string, response: AjaxExplainResponse, fallbackFormat: string) {
            var responseFormat = response.format || fallbackFormat || 'text';
            if (response.error) {
                showExplainError(paneKey, response.error);
                return;
            }
            $('#' + getPaneState(paneKey).errorId).text('');
            renderExplanation(paneKey, response.content || '', responseFormat);
        }

        function ajaxExplain(level: string, requestId: number) {
            var selectedFormat = (<string>$('#explain-format').val() || 'text').toLowerCase();
            $('#' + primaryPaneState.errorId).text('');
            clearRenderedExplanation('primary', selectedFormat);
            activeExplainJqXHR = $.ajax({
                url: 'query',
                type: 'POST',
                dataType: 'json',
                data: serializeExplainFormData(getPaneRawQueryValue('primary'), level, selectedFormat),
                error: function(jqXHR: JQueryXHR, textStatus: string, errorThrown: string) {
                    if (textStatus !== 'abort') {
                        showExplainError('primary', getExplainErrorMessage(jqXHR, textStatus, errorThrown));
                    }
                },
                success: function(response: AjaxExplainResponse) {
                    if (requestId !== activeExplainRequestId) {
                        return;
                    }
                    applyExplainResponseToPane('primary', response, selectedFormat);
                },
                complete: function() {
                    activeExplainJqXHR = null;
                    finishExplainRequest(requestId);
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
                .attr('aria-busy', 'false');
            $('#explain-compare-trigger-icon')
                .removeClass('query-compare-action__icon--spinning');
            hideCompareExplainCancelButton();
        }

        function showCompareExplainSpinner() {
            $('#explain-compare-trigger')
                .attr('aria-busy', 'true');
            $('#explain-compare-trigger-icon')
                .addClass('query-compare-action__icon--spinning');
            showCompareExplainCancelButton();
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

        function setCompareExplainButtonsDisabled(disabled: boolean) {
            $('#explain-compare-trigger').prop('disabled', disabled);
            $('#explain-trigger').prop('disabled', disabled);
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

        function shouldAutoExplainComparePaneOnOpen(): boolean {
            if (!compareModeEnabled || !hasPrimaryExplanation()) {
                return false;
            }
            var selectedLevel = <string>$('#explain-level').val() || 'Optimized';
            return selectedLevel === 'Unoptimized' || selectedLevel === 'Optimized';
        }

        function syncCompareModeVisibility() {
            $('#query-compare-layout').toggleClass('query-compare-layout--active', compareModeEnabled);
            $('#query-compare-controls').toggle(compareModeEnabled);
            $('#explain-trigger').show();
            if (!compareModeEnabled) {
                hideCompareExplainSpinner();
            }
            syncPrimaryExplanationControls();
            syncCompareSidebarState();
            updateCompareActionState();
            refreshVisibleQueryEditors();
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

        function beginCompareExplainRequest(pendingRequests?: number): number {
            activeCompareRequestId += 1;
            activeComparePendingRequests = pendingRequests || 2;
            hideCompareExplainSpinner();
            setCompareExplainButtonsDisabled(true);
            $('#query-diff-trigger').prop('disabled', true);
            showCompareExplainSpinner();
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
            hideCompareExplainSpinner();
            setCompareExplainButtonsDisabled(false);
            updateCompareActionState();
        }

        function enqueueCompareExplanationRequest(paneKey: string, level: string, selectedFormat: string, requestId: number) {
            var compareRequest = $.ajax({
                url: 'query',
                type: 'POST',
                dataType: 'json',
                data: serializeExplainFormData(getPaneRawQueryValue(paneKey), level, selectedFormat),
                error: function(jqXHR: JQueryXHR, textStatus: string, errorThrown: string) {
                    if (textStatus !== 'abort' && requestId === activeCompareRequestId) {
                        showExplainError(paneKey, getExplainErrorMessage(jqXHR, textStatus, errorThrown));
                    }
                },
                success: function(response: AjaxExplainResponse) {
                    if (requestId !== activeCompareRequestId) {
                        return;
                    }
                    applyExplainResponseToPane(paneKey, response, selectedFormat);
                },
                complete: function() {
                    finishCompareExplainRequest(requestId);
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
            var effectiveLevel = level || <string>$('#explain-level').val() || 'Optimized';
            var explainButton = getExplainTriggerButtonElement(buttonId);
            if (explainButton && explainButton.disabled) {
                return;
            }
            $('#explain-level').val(effectiveLevel);
            captureExplainButtonViewportTop('primary', buttonId);
            savePaneQuery('primary');
            var requestId = beginExplainRequest(explainButton ? explainButton.id : 'explain-trigger');
            ajaxExplain(effectiveLevel, requestId);
        }

        export function cancelExplain() {
            if (activeExplainJqXHR) {
                activeExplainJqXHR.abort();
            }
        }

        export function runCompareExplain(buttonId?: string) {
            if (!compareModeEnabled) {
                return;
            }
            savePaneQuery('primary');
            savePaneQuery('compare');
            var effectiveLevel = <string>$('#explain-level').val() || 'Optimized';
            var selectedFormat = (<string>$('#explain-format').val() || 'text').toLowerCase();
            var triggerButtonId = buttonId || 'explain-compare-trigger';
            captureExplainButtonViewportTop('primary', triggerButtonId);
            captureExplainButtonViewportTop('compare', triggerButtonId);
            $('#' + primaryPaneState.errorId).text('');
            $('#' + comparePaneState.errorId).text('');
            clearRenderedExplanation('primary', selectedFormat);
            clearRenderedExplanation('compare', selectedFormat);
            var requestId = beginCompareExplainRequest();
            var paneKeys = ['primary', 'compare'];
            for (var i = 0; i < paneKeys.length; i++) {
                enqueueCompareExplanationRequest(paneKeys[i], effectiveLevel, selectedFormat, requestId);
            }
        }

        function requestComparePaneExplanation(level: string) {
            if (!compareModeEnabled) {
                return;
            }
            savePaneQuery('compare');
            var selectedFormat = (<string>$('#explain-format').val() || 'text').toLowerCase();
            $('#' + comparePaneState.errorId).text('');
            clearRenderedExplanation('compare', selectedFormat);
            var requestId = beginCompareExplainRequest(1);
            enqueueCompareExplanationRequest('compare', level, selectedFormat, requestId);
        }

        export function cancelCompareExplain() {
            activeCompareRequestId += 1;
            activeComparePendingRequests = 0;
            for (var i = 0; i < activeCompareExplainJqXHRs.length; i++) {
                activeCompareExplainJqXHRs[i].abort();
            }
            activeCompareExplainJqXHRs = [];
            hideCompareExplainSpinner();
            setCompareExplainButtonsDisabled(false);
            updateCompareActionState();
        }

        export function downloadExplanation() {
            if (!primaryPaneState.latestExplanation) {
                return;
            }
            var format = primaryPaneState.latestExplanationFormat || <string>$('#explain-format').val() || 'text';
            var extension = getExplanationDownloadExtension(format);
            var mimeType = getExplanationDownloadMimeType(format);
            var blob = new Blob([primaryPaneState.latestExplanation], { type: mimeType + ';charset=utf-8' });
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
            var initialFormat = <string>$('#query-explanation').attr('data-format')
                || <string>$('#explain-format').val() || 'text';
            if (initialExplanation) {
                renderExplanation('primary', initialExplanation, initialFormat);
            } else {
                primaryPaneState.latestExplanation = '';
                primaryPaneState.latestExplanationFormat = initialFormat.toLowerCase();
                updateDownloadButtonState();
                renderDotView('primary', '', primaryPaneState.latestExplanationFormat);
                renderJsonView('primary', '', primaryPaneState.latestExplanationFormat);
                $('#query-explanation-controls-row').hide();
            }
            comparePaneState.latestExplanation = '';
            comparePaneState.latestExplanationFormat = 'text';
            $('#query-explanation-row-compare').hide();
            renderDotView('compare', '', comparePaneState.latestExplanationFormat);
            renderJsonView('compare', '', comparePaneState.latestExplanationFormat);
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
                consumeShareLink: null//don't try to parse the url args. this is already done by the addLoad function below
            });
            $(paneEditor.getWrapperElement()).css({
                "fontSize": "14px",
                "width": "100%",
                "maxWidth": "100%",
                "boxSizing": "border-box"
            });
            if (clearFeedbackOnChange) {
                paneEditor.on('change', function() {
                    workbench.query.clearFeedback();
                    updateCompareActionState();
                });
            } else {
                paneEditor.on('change', updateCompareActionState);
            }
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
        }

        function closeYasqe() {
            if (yasqe) {
                yasqe.toTextArea();
                yasqe = null;
            }
        }

        export function toggleCompareMode() {
            compareModeEnabled = !compareModeEnabled;
            if (compareModeEnabled) {
                compareSidebarOpen = false;
                if (!compareQuerySeeded && !getPaneQueryValue('compare')) {
                    setPaneQueryValue('compare', getPaneRawQueryValue('primary'));
                    compareQuerySeeded = true;
                }
                if ($("#queryLn").val() == "SPARQL") {
                    ensureCompareYasqe();
                }
            } else {
                compareSidebarOpen = false;
                closeDiffModal();
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
            compareSidebarOpen = !compareSidebarOpen;
            syncCompareSidebarState();
        }

        export function openDiffModal() {
            if (!compareModeEnabled) {
                return;
            }
            lastDiffTriggerElement = <HTMLElement>document.getElementById('query-diff-trigger');
            renderDiffView('#query-diff-query', getPaneRawQueryValue('primary'), getPaneRawQueryValue('compare'));
            if (!primaryPaneState.latestExplanation || !comparePaneState.latestExplanation) {
                $('#query-diff-explanation').text(diffNotReadyLabel);
            } else {
                renderDiffView(
                    '#query-diff-explanation',
                    primaryPaneState.latestExplanation,
                    comparePaneState.latestExplanation,
                    diffNotReadyLabel
                );
            }
            $('#query-diff-modal')
                .addClass('query-diff-modal--open')
                .attr('aria-hidden', 'false');
            (<HTMLElement>document.getElementById('query-diff-close')).focus();
        }

        export function closeDiffModal() {
            $('#query-diff-modal')
                .removeClass('query-diff-modal--open')
                .attr('aria-hidden', 'true');
            if (lastDiffTriggerElement) {
                lastDiffTriggerElement.focus();
            }
        }

        export function initializeCompareUi() {
            compareSidebarOpen = false;
            $('#query-compare-controls').hide();
            $('#query-diff-modal').attr('aria-hidden', 'true');
            diffNotReadyLabel = $.trim($('#query-diff-explanation').text());
            syncCompareModeVisibility();
        }

        export function refreshCompareActionState() {
            updateCompareActionState();
        }

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
    $('#query').bind('keydown cut paste', function() {
        workbench.query.clearFeedback();
        workbench.query.refreshCompareActionState();
    });
    $('#query-compare').bind('keydown cut paste', function() {
        workbench.query.clearFeedback();
        workbench.query.refreshCompareActionState();
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
