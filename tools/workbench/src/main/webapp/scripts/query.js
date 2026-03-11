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
        var latestExplanation = '';
        var latestExplanationFormat = 'text';
        var vizRenderer = null;
        var dotPanZoomInstance = null;
        var explainButtonViewportTopBeforeRequest = null;
        var explainButtonIdBeforeRequest = '';
        var explainSpinnerVisibleSince = 0;
        var explainSpinnerTargetId = '';
        var explainSpinnerDelayTimeoutId = null;
        var explainSpinnerHideTimeoutId = null;
        var activeExplainRequestId = 0;
        var activeExplainJqXHR = null;
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
        function clearExplainSelection() {
            $('#explain').val('');
            $('#explain-level').val('Optimized');
        }
        function updateDownloadButtonState() {
            $('#download-explanation').prop('disabled', !latestExplanation);
        }
        function lockExplanationDimensions() {
            var explanation = $('#query-explanation');
            var dotView = $('#query-explanation-dot-view');
            var jsonView = $('#query-explanation-json-view');
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
        function clearExplanationDimensionLock() {
            var explanation = $('#query-explanation');
            var dotView = $('#query-explanation-dot-view');
            var jsonView = $('#query-explanation-json-view');
            explanation.css('min-height', '');
            explanation.css('min-width', '');
            dotView.css('min-height', '');
            dotView.css('min-width', '');
            jsonView.css('min-height', '');
            jsonView.css('min-width', '');
        }
        function destroyDotPanZoom() {
            if (dotPanZoomInstance && typeof dotPanZoomInstance.destroy === 'function') {
                dotPanZoomInstance.destroy();
            }
            dotPanZoomInstance = null;
        }
        function clearExplainButtonViewportRestoreState() {
            explainButtonViewportTopBeforeRequest = null;
            explainButtonIdBeforeRequest = '';
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
        function beginExplainRequest(buttonId) {
            activeExplainRequestId += 1;
            var requestId = activeExplainRequestId;
            explainSpinnerTargetId = buttonId;
            clearExplainSpinnerDelayTimeout();
            clearExplainSpinnerHideTimeout();
            hideExplainSpinners();
            hideExplainCancelButtons();
            setExplainButtonsDisabled(true);
            explainSpinnerDelayTimeoutId = window.setTimeout(function () {
                if (requestId !== activeExplainRequestId) {
                    return;
                }
                explainSpinnerDelayTimeoutId = null;
                showExplainSpinner(buttonId);
            }, 1000);
            return requestId;
        }
        function finishExplainRequest(requestId) {
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
        function captureExplainButtonViewportTop(buttonId) {
            var explainButton = getExplainTriggerButtonElement(buttonId);
            if (!explainButton) {
                clearExplainButtonViewportRestoreState();
                return;
            }
            explainButtonViewportTopBeforeRequest = explainButton.getBoundingClientRect().top;
            explainButtonIdBeforeRequest = explainButton.id;
        }
        function restoreExplainButtonViewportTopIfNeeded() {
            if (explainButtonViewportTopBeforeRequest === null) {
                return;
            }
            var explainButton = explainButtonIdBeforeRequest
                ? document.getElementById(explainButtonIdBeforeRequest)
                : getExplainTriggerButtonElement();
            if (!explainButton) {
                clearExplainButtonViewportRestoreState();
                return;
            }
            window.requestAnimationFrame(function () {
                var currentButtonTop = explainButton.getBoundingClientRect().top;
                var scrollDelta = currentButtonTop - explainButtonViewportTopBeforeRequest;
                if (Math.abs(scrollDelta) > 1) {
                    window.scrollBy(0, scrollDelta);
                }
                clearExplainButtonViewportRestoreState();
            });
        }
        function setExplanationDisplayMode(format) {
            if (format === 'dot') {
                $('#query-explanation').hide();
                $('#query-explanation-json-view').hide();
                $('#query-explanation-dot-view').show();
                return;
            }
            if (format === 'json') {
                $('#query-explanation').hide();
                $('#query-explanation-dot-view').hide();
                $('#query-explanation-json-view').show();
                destroyDotPanZoom();
            }
            else {
                $('#query-explanation').show();
                $('#query-explanation-dot-view').hide();
                $('#query-explanation-json-view').hide();
                destroyDotPanZoom();
            }
        }
        function applyDotPanZoom(svgElement) {
            destroyDotPanZoom();
            if (typeof svgPanZoom === 'undefined') {
                return;
            }
            dotPanZoomInstance = svgPanZoom(svgElement, {
                zoomEnabled: true,
                controlIconsEnabled: true,
                fit: true,
                center: true,
                minZoom: 0.2,
                maxZoom: 20
            });
        }
        function clearRenderedExplanation() {
            $('#query-explanation-row').show();
            $('#query-explanation-controls-row').show();
            lockExplanationDimensions();
            $('#query-explanation').text('');
            var pendingFormat = ($('#explain-format').val() || 'text').toLowerCase();
            $('#query-explanation').attr('data-format', pendingFormat);
            latestExplanation = '';
            latestExplanationFormat = pendingFormat;
            updateDownloadButtonState();
            destroyDotPanZoom();
            $('#query-explanation-dot-view').empty();
            $('#query-explanation-json-view').empty();
            setExplanationDisplayMode(pendingFormat);
        }
        function showExplainError(message) {
            var errorMessage = message || 'Explain request failed.';
            clearRenderedExplanation();
            $('#queryString.errors').text(errorMessage);
            $('#query-explanation').show().text(errorMessage).attr('data-format', 'text');
            $('#query-explanation-dot-view').hide().empty();
            $('#query-explanation-json-view').hide().empty();
            destroyDotPanZoom();
            restoreExplainButtonViewportTopIfNeeded();
            clearExplanationDimensionLock();
        }
        function renderDotView(explanationText, format) {
            var dotView = $('#query-explanation-dot-view');
            if (format === 'dot') {
                setExplanationDisplayMode('dot');
                if (!explanationText) {
                    dotView.empty().show();
                    restoreExplainButtonViewportTopIfNeeded();
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
                vizRenderer.renderSVGElement(explanationText).then(function (svgElement) {
                    $(svgElement).css({
                        width: '100%',
                        height: '100%',
                        maxWidth: '100%',
                        maxHeight: 'none',
                        display: 'block'
                    });
                    dotView.empty().append(svgElement).show();
                    applyDotPanZoom(svgElement);
                    restoreExplainButtonViewportTopIfNeeded();
                }).catch(function () {
                    vizRenderer = new Viz();
                    destroyDotPanZoom();
                    dotView.html('<div class="error">Unable to render DOT graph.</div>');
                    restoreExplainButtonViewportTopIfNeeded();
                });
                return;
            }
            destroyDotPanZoom();
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
        function renderJsonView(explanationText, format) {
            var jsonView = $('#query-explanation-json-view');
            if (format !== 'json') {
                jsonView.hide().empty();
                return;
            }
            setExplanationDisplayMode('json');
            jsonView.empty().show();
            if (!explanationText) {
                restoreExplainButtonViewportTopIfNeeded();
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
            restoreExplainButtonViewportTopIfNeeded();
        }
        function renderExplanation(explanationText, format) {
            var normalizedFormat = (format || 'text').toLowerCase();
            $('#query-explanation-row').show();
            $('#query-explanation-controls-row').show();
            if (normalizedFormat === 'dot' || normalizedFormat === 'json') {
                $('#query-explanation').text('').attr('data-format', normalizedFormat);
            }
            else {
                $('#query-explanation').text(explanationText).attr('data-format', normalizedFormat);
            }
            setExplanationDisplayMode(normalizedFormat);
            latestExplanation = explanationText;
            latestExplanationFormat = normalizedFormat;
            updateDownloadButtonState();
            renderDotView(explanationText, normalizedFormat);
            renderJsonView(explanationText, normalizedFormat);
            if (normalizedFormat === 'text') {
                restoreExplainButtonViewportTopIfNeeded();
            }
            clearExplanationDimensionLock();
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
        function ajaxExplain(level, requestId) {
            var form = $('form[action="query"]');
            var previousAction = $('#action').val();
            var previousExplain = $('#explain').val();
            $('#action').val('explain');
            $('#explain').val(level);
            var serializedForm = form.serialize();
            $('#action').val(previousAction);
            $('#explain').val(previousExplain);
            $('#queryString.errors').text('');
            clearRenderedExplanation();
            activeExplainJqXHR = $.ajax({
                url: 'query',
                type: 'POST',
                dataType: 'json',
                data: serializedForm,
                error: function (jqXHR, textStatus, errorThrown) {
                    if (textStatus !== 'abort') {
                        showExplainError(getExplainErrorMessage(jqXHR, textStatus, errorThrown));
                    }
                },
                success: function (response) {
                    if (response.error) {
                        showExplainError(response.error);
                        return;
                    }
                    $('#queryString.errors').text('');
                    renderExplanation(response.content || '', response.format || $('#explain-format').val());
                },
                complete: function () {
                    activeExplainJqXHR = null;
                    finishExplainRequest(requestId);
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
            var effectiveLevel = level || $('#explain-level').val() || 'Optimized';
            var explainButton = getExplainTriggerButtonElement(buttonId);
            if (explainButton && explainButton.disabled) {
                return;
            }
            $('#explain-level').val(effectiveLevel);
            captureExplainButtonViewportTop(buttonId);
            if (yasqe)
                yasqe.save();
            var requestId = beginExplainRequest(explainButton ? explainButton.id : 'explain-trigger');
            ajaxExplain(effectiveLevel, requestId);
        }
        query_1.runExplain = runExplain;
        function cancelExplain() {
            if (activeExplainJqXHR) {
                activeExplainJqXHR.abort();
            }
        }
        query_1.cancelExplain = cancelExplain;
        function downloadExplanation() {
            if (!latestExplanation) {
                return;
            }
            var format = latestExplanationFormat || $('#explain-format').val() || 'text';
            var extension = getExplanationDownloadExtension(format);
            var mimeType = getExplanationDownloadMimeType(format);
            var blob = new Blob([latestExplanation], { type: mimeType + ';charset=utf-8' });
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
            var initialFormat = $('#query-explanation').attr('data-format')
                || $('#explain-format').val() || 'text';
            if (initialExplanation) {
                renderExplanation(initialExplanation, initialFormat);
            }
            else {
                latestExplanation = '';
                latestExplanationFormat = initialFormat.toLowerCase();
                updateDownloadButtonState();
                renderDotView('', latestExplanationFormat);
                renderJsonView('', latestExplanationFormat);
                $('#query-explanation-controls-row').hide();
            }
        }
        query_1.initializeExplanationView = initializeExplanationView;
        function setQueryValue(queryString) {
            yasqe.setValue(queryString.trim());
        }
        query_1.setQueryValue = setQueryValue;
        function getQueryValue() {
            return yasqe.getValue().trim();
        }
        query_1.getQueryValue = getQueryValue;
        function getYasqe() {
            return yasqe;
        }
        query_1.getYasqe = getYasqe;
        function updateYasqe() {
            if ($("#queryLn").val() == "SPARQL") {
                initYasqe();
            }
            else {
                closeYasqe();
            }
        }
        query_1.updateYasqe = updateYasqe;
        function initYasqe() {
            workbench.yasqeHelper.setupCompleters(sparqlNamespaces);
            yasqe = YASQE.fromTextArea(document.getElementById('query'), {
                consumeShareLink: null //don't try to parse the url args. this is already done by the addLoad function below
            });
            //some styling conflicts. Could add my own css file, but not a lot of things need changing, so just do this programmatically
            //first, set the font size (otherwise font is as small as menu, which is too small)
            //second, keep editor width constrained to its table column
            $(yasqe.getWrapperElement()).css({
                "fontSize": "14px",
                "width": "100%",
                "maxWidth": "100%",
                "boxSizing": "border-box"
            });
            //we made a change to the css wrapper element (and did so after initialization). So, force a manual update of the yasqe instance
            yasqe.refresh();
        }
        function closeYasqe() {
            if (yasqe) {
                //store yasqe value in text area (not sure whether this is desired, but it mimics current behavior)
                //it closes the yasqe instance as well
                yasqe.toTextArea();
                yasqe = null;
            }
        }
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
    $('#query').bind('keydown cut paste', workbench.query.clearFeedback);
    if (workbench.query.getYasqe()) {
        workbench.query.getYasqe().on('change', workbench.query.clearFeedback);
    }
    // Detect if there is no current authenticated user, and if so, disable
    // the 'save privately' option.
    if ($('#selected-user>span').is('.disabled')) {
        $('#save-private').prop('checked', false).prop('disabled', true);
    }
});
//# sourceMappingURL=query.js.map