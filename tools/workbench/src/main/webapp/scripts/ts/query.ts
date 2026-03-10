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
        var latestExplanation = '';
        var latestExplanationFormat = 'text';
        var vizRenderer: any = null;
        var dotPanZoomInstance: any = null;
        var explainButtonViewportTopBeforeRequest: number = null;
        var explainButtonIdBeforeRequest = '';

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
            var dotHeight = dotView.outerHeight();
            var dotWidth = dotView.outerWidth();
            var jsonHeight = jsonView.outerHeight();
            var jsonWidth = jsonView.outerWidth();
            if (dotHeight && (!currentHeight || dotHeight > currentHeight)) {
                currentHeight = dotHeight;
            }
            if (dotWidth && (!currentWidth || dotWidth > currentWidth)) {
                currentWidth = dotWidth;
            }
            if (jsonHeight && (!currentHeight || jsonHeight > currentHeight)) {
                currentHeight = jsonHeight;
            }
            if (jsonWidth && (!currentWidth || jsonWidth > currentWidth)) {
                currentWidth = jsonWidth;
            }
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

        function getExplainTriggerButtonElement(): HTMLElement {
            var activeElement = <HTMLElement>document.activeElement;
            if (activeElement
                    && (activeElement.id === 'explain-trigger' || activeElement.id === 'rerun-explanation')) {
                return activeElement;
            }
            return <HTMLElement>document.getElementById('explain-trigger');
        }

        function captureExplainButtonViewportTop() {
            var explainButton = getExplainTriggerButtonElement();
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
                    ? <HTMLElement>document.getElementById(explainButtonIdBeforeRequest)
                    : getExplainTriggerButtonElement();
            if (!explainButton) {
                clearExplainButtonViewportRestoreState();
                return;
            }
            window.requestAnimationFrame(function() {
                var currentButtonTop = explainButton.getBoundingClientRect().top;
                var scrollDelta = currentButtonTop - explainButtonViewportTopBeforeRequest;
                if (Math.abs(scrollDelta) > 1) {
                    window.scrollBy(0, scrollDelta);
                }
                clearExplainButtonViewportRestoreState();
            });
        }

        function setExplanationDisplayMode(format: string) {
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
            } else {
                $('#query-explanation').show();
                $('#query-explanation-dot-view').hide();
                $('#query-explanation-json-view').hide();
                destroyDotPanZoom();
            }
        }

        function applyDotPanZoom(svgElement: SVGElement) {
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
            var pendingFormat = (<string>$('#explain-format').val() || 'text').toLowerCase();
            $('#query-explanation').attr('data-format', pendingFormat);
            latestExplanation = '';
            latestExplanationFormat = pendingFormat;
            updateDownloadButtonState();
            destroyDotPanZoom();
            $('#query-explanation-dot-view').empty();
            $('#query-explanation-json-view').empty();
            setExplanationDisplayMode(pendingFormat);
        }

        function showExplainError(message: string) {
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

        function renderDotView(explanationText: string, format: string) {
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
                vizRenderer.renderSVGElement(explanationText).then(function(svgElement: SVGElement) {
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
                }).catch(function() {
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

        function renderJsonView(explanationText: string, format: string) {
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
            } catch (parseError) {
                jsonView.append($('<div class="error">Unable to parse JSON explanation. Showing raw content.</div>'));
                jsonView.append($('<pre class="query-json-view__raw"></pre>').text(explanationText));
            }

            restoreExplainButtonViewportTopIfNeeded();
        }

        function renderExplanation(explanationText: string, format: string) {
            var normalizedFormat = (format || 'text').toLowerCase();
            $('#query-explanation-row').show();
            $('#query-explanation-controls-row').show();
            if (normalizedFormat === 'dot' || normalizedFormat === 'json') {
                $('#query-explanation').text('').attr('data-format', normalizedFormat);
            } else {
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

        function ajaxExplain() {
            var form = $('form[action="query"]');
            $('#queryString.errors').text('');
            clearRenderedExplanation();
            $.ajax({
                url: 'query',
                type: 'POST',
                dataType: 'json',
                data: form.serialize(),
                error: function(jqXHR: JQueryXHR, textStatus: string, errorThrown: string) {
                    showExplainError(getExplainErrorMessage(jqXHR, textStatus, errorThrown));
                },
                success: function(response: AjaxExplainResponse) {
                    if (response.error) {
                        showExplainError(response.error);
                        return;
                    }
                    $('#queryString.errors').text('');
                    renderExplanation(response.content || '', response.format || <string>$('#explain-format').val());
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

        export function runExplain(level?: string) {
            var effectiveLevel = level || <string>$('#explain-level').val() || 'Optimized';
            $('#explain-level').val(effectiveLevel);
            captureExplainButtonViewportTop();
            if (yasqe) yasqe.save();
            $('#action').val('explain');
            $('#explain').val(effectiveLevel);
            ajaxExplain();
        }

        export function downloadExplanation() {
            if (!latestExplanation) {
                return;
            }
            var format = latestExplanationFormat || <string>$('#explain-format').val() || 'text';
            var extension = getExplanationDownloadExtension(format);
            var mimeType = getExplanationDownloadMimeType(format);
            var blob = new Blob([latestExplanation], { type: mimeType + ';charset=utf-8' });
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
                renderExplanation(initialExplanation, initialFormat);
            } else {
                latestExplanation = '';
                latestExplanationFormat = initialFormat.toLowerCase();
                updateDownloadButtonState();
                renderDotView('', latestExplanationFormat);
                renderJsonView('', latestExplanationFormat);
                $('#query-explanation-controls-row').hide();
            }
        }

        export function setQueryValue(queryString: string): void {
            yasqe.setValue(queryString.trim());
        }

        export function getQueryValue(): string {
            return yasqe.getValue().trim();
        }

        export function getYasqe(): YASQE_Instance {
            return yasqe;
        }

        export function updateYasqe() {
            if ($("#queryLn").val() == "SPARQL") {
                initYasqe();
            } else {
                closeYasqe();
            }
        }

        function initYasqe() {
            workbench.yasqeHelper.setupCompleters(sparqlNamespaces);

            yasqe = YASQE.fromTextArea(<HTMLTextAreaElement>document.getElementById('query'), {
                consumeShareLink: null//don't try to parse the url args. this is already done by the addLoad function below
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
    $('#query').bind('keydown cut paste', workbench.query.clearFeedback);
    if (workbench.query.getYasqe()) {
        workbench.query.getYasqe().on('change',
            workbench.query.clearFeedback);
    }

    // Detect if there is no current authenticated user, and if so, disable
    // the 'save privately' option.
    if ($('#selected-user>span').is('.disabled')) {
        $('#save-private').prop('checked', false).prop('disabled', true);
    }
});
