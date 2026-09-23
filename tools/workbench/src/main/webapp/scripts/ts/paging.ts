/// <reference path="template.ts" />
/// <reference path="jquery.d.ts" />

// WARNING: Do not edit the *.js version of this file. Instead, always edit the
// corresponding *.ts source in the ts subfolder, and then invoke the
// compileTypescript.sh bash script to generate new *.js and *.js.map files.

module workbench {

    export module paging {

        var KT = 'know_total';

        var RESULT_TOTAL_COUNT_ID = 'workbench-total-result-count';

        var OFFSET = 'offset';

        export var LIMIT = 'limit';

        export var LIM_ID = '#' + LIMIT;

        var AMP = decodeURIComponent('%26');

        function addCookieToUrlQueryIfPresent(url: string, name: string){
            var value = workbench.getCookie(name);
            if (value) {
                url = url + AMP + name + '=' + value;
            }
            return url;
        }

        function createHiddenInput(name: string, value: string): HTMLInputElement {
            var input = document.createElement('input');
            input.type = 'hidden';
            input.name = name;
            input.value = value;
            return input;
        }

        function addCookieToFormIfPresent(form: HTMLFormElement, name: string) {
            var value = workbench.getCookie(name);
            if (value) {
                form.appendChild(createHiddenInput(name, value));
            }
        }

        function addElementValueToFormIfPresent(form: HTMLFormElement, name: string) {
            var element = <HTMLInputElement | HTMLSelectElement>document.getElementById(name);
            if (element && element.value) {
                form.appendChild(createHiddenInput(name, element.value));
            }
        }

        function appendParamToUrl(url: string, name: string, value: string) {
            if (url.indexOf('?') + 1 || url.indexOf(';') + 1) {
                return url + AMP + name + '=' + value;
            }
            return url + ';' + name + '=' + value;
        }

        function addElementValueToUrlIfPresent(url: string, name: string) {
            var element = <HTMLInputElement | HTMLSelectElement>document.getElementById(name);
            if (element && element.value) {
                return appendParamToUrl(url, name, encodeURIComponent(element.value));
            }
            return url;
        }

        function getEmbeddedQueryText(): string {
            var queryText = <HTMLTextAreaElement>document.getElementById('wb-query-text');
            if (queryText && queryText.value) {
                return queryText.value;
            }
            return '';
        }

        function addQueryReferenceToForm(form: HTMLFormElement) {
            var queryText = getEmbeddedQueryText();
            if (queryText) {
                form.appendChild(createHiddenInput('query', queryText));
                form.appendChild(createHiddenInput('ref', 'text'));
                return;
            }
            if (isEmbeddedResultPage()) {
                return;
            }
            addCookieToFormIfPresent(form, 'query');
            addCookieToFormIfPresent(form, 'ref');
        }

        function isEmbeddedResultPage(): boolean {
            var marker = document.getElementById('rdf4j-query-result');
            return !!marker && marker.getAttribute('data-query-embedded') === 'true';
        }

        function getEmbeddedResultMetadata(name: string): string {
            var marker = document.getElementById('rdf4j-query-result');
            if (!marker) {
                return '';
            }
            return marker.getAttribute('data-query-' + name) || '';
        }

        function addEmbeddedQueryOptionsToForm(form: HTMLFormElement, limitOverride?: string) {
            var queryLanguage = getEmbeddedResultMetadata('language');
            var infer = getEmbeddedResultMetadata('infer');
            var queryTimeout = getEmbeddedResultMetadata('timeout');
            if (queryLanguage) {
                form.appendChild(createHiddenInput('queryLn', queryLanguage));
            }
            if (infer) {
                form.appendChild(createHiddenInput('infer', infer));
            }
            if (queryTimeout) {
                form.appendChild(createHiddenInput('query-timeout', queryTimeout));
            }
            if (typeof limitOverride === 'string') {
                form.appendChild(createHiddenInput('limit_query', limitOverride));
            } else {
                addElementValueToFormIfPresent(form, 'limit_query');
            }
        }

        function addQueryOptionsToForm(form: HTMLFormElement, limitOverride?: string) {
            if (isEmbeddedResultPage()) {
                addEmbeddedQueryOptionsToForm(form, limitOverride);
                return;
            }
            addCookieToFormIfPresent(form, 'owner');
            addCookieToFormIfPresent(form, 'queryLn');
            addCookieToFormIfPresent(form, 'infer');
            addCookieToFormIfPresent(form, 'limit_query');
            addCookieToFormIfPresent(form, 'query-timeout');
        }

        function notifyParent(message: any) {
            if (!isEmbeddedResultPage() || !window.parent || window.parent === window
                    || typeof window.parent.postMessage !== 'function') {
                return;
            }
            var targetOrigin = window.location.origin;
            if (!targetOrigin || targetOrigin === 'null') {
                targetOrigin = window.location.protocol + '//' + window.location.host;
            }
            window.parent.postMessage(message, targetOrigin);
        }

        function addEmbeddedRequestToForm(form: HTMLFormElement, name: string): boolean {
            if (!isEmbeddedResultPage() || name === 'Accept') {
                return false;
            }
            var queryRequestId = workbench.generateRequestId();
            form.appendChild(createHiddenInput('query-request-id', queryRequestId));
            form.appendChild(createHiddenInput('embedded', 'true'));
            notifyParent({
                type: 'rdf4j-query-start',
                queryRequestId: queryRequestId
            });
            return true;
        }

        function submitGraphParamRequest(name: string, value: string) {
            var form = document.createElement('form');
            form.method = 'POST';
            form.action = 'query';
            form.style.display = 'none';

            form.appendChild(createHiddenInput('action', 'exec'));
            addQueryReferenceToForm(form);
            var limitOverride = isEmbeddedResultPage() && name === 'limit_query';
            addQueryOptionsToForm(form, limitOverride ? value : undefined);
            addEmbeddedRequestToForm(form, name);
            if (name == 'Accept') {
                addElementValueToFormIfPresent(form, 'download_limit');
            }
            if (!limitOverride) {
                form.appendChild(createHiddenInput(name, value));
            }

            document.body.appendChild(form);
            form.submit();
            document.body.removeChild(form);
        }

        function submitPagingParamRequest(name: string, value: string) {
            var form = document.createElement('form');
            form.method = 'POST';
            form.action = 'query';
            form.style.display = 'none';

            form.appendChild(createHiddenInput('action', 'exec'));
            addQueryReferenceToForm(form);
            var limitOverride = isEmbeddedResultPage() && name === 'limit_query';
            addQueryOptionsToForm(form, limitOverride ? value : undefined);
            addEmbeddedRequestToForm(form, name);
            if (!hasQueryParameter(KT) || 'false' == getQueryParameter(KT)) {
                form.appendChild(createHiddenInput(KT, String(getTotalResultCount())));
            }
            if (!limitOverride) {
                form.appendChild(createHiddenInput(name, value));
            }

            document.body.appendChild(form);
            form.submit();
            document.body.removeChild(form);
        }

        /**
         * Invoked in graph.xsl and tuple.xsl for download functionality. Takes a
         * document element by name, and creates a request with it as a parameter.
         */
        export function addGraphParam(name: string) {
            var value = <string>$('#' + name).val();
            var url = document.location.href;
            if (isEmbeddedResultPage() && name !== 'Accept') {
                submitGraphParamRequest(name, value);
                return;
            }
            if (url.match(/query$/)) { // looking at POST query results?
                submitGraphParamRequest(name, value);
                return;
            }
            if (name == 'Accept') {
                url = addElementValueToUrlIfPresent(url, 'download_limit');
            }
            document.location.href = appendParamToUrl(url, name, encodeURIComponent(value));
        }
        
        class StringMap {
            [key: string]: string;
        }

        /**
         * Scans the given URI for duplicate query parameter names, and removes
         * all but the last occurrence for any duplicate case.
         * 
         * @param {String} href The URI to simplify.
         * @returns {String} The URI with only the last occurrence of any
         *          given parameter name remaining.
         */
        function simplifyParameters(href: string) {
            var params:StringMap = {};
            var rval = '';
            var queryString = getQueryString(href);
            var start = href.substring(0, href.indexOf(queryString));
            var elements = queryString.split(decodeURIComponent('%26'));
            for (var i = 0; elements.length - i; i++) {
                var pair = elements[i].split('=');
                params[pair[0]] = pair[1];
                // Keep looping. We are interested in the last value.
            }
            for (var name in params) {
                // use hasOwnProperty to filter out keys from the
                // Object.prototype
                if (params.hasOwnProperty(name)) {
                    rval += name + '=' + params[name] + AMP;
                }
            }
            rval = start + rval.substring(0, rval.length - 1);
            return rval;
        }


        /**
         * First, adds the given parameter to the URL query string. Second,
         * adds a 'know_total' parameter if its current value is 'false' or
         * non-existent. Third, simplifies the URL. Fourth, sends the browser
         * to the modified URL.
         * 
         * @param {String} name The name of the query parameter.
         * @param {number} value The value of the query parameter.
         */
        export function addPagingParam(name: string, value: number) {
            if (isEmbeddedResultPage() || document.location.pathname.match(/\/query$/)) {
                submitPagingParamRequest(name, String(value));
                return;
            }
            var url = document.location.href;
            var hasParams = (url.indexOf('?') + 1 || url.indexOf(';') + 1);
            var sep = hasParams ? AMP : ';';
            url = url + sep + name + '=' + value;
            if (!hasQueryParameter(KT) || 'false' == getQueryParameter(KT)) {
                url += AMP + KT + '=' + getTotalResultCount();
            }
            if (!hasQueryParameter('query')) {
                url += AMP + 'query=' + encodeURIComponent(workbench.getCookie('query'));
                url += AMP + 'ref=' + encodeURIComponent(workbench.getCookie('ref'));
            }
            document.location.href = simplifyParameters(url);
        }

        /**
         * Invoked in tuple.xsl and explore.xsl. Changes the limit query
         * parameter and navigates to the new URL.
         */
        export function addLimit(page: string) {
            var suffix = '_' + page;
            addPagingParam(LIMIT + suffix, $(LIM_ID + suffix).val());
        }

        /**
         * Invoked in tuple.xsl and explore.xsl. Increments the offset query
         * parameter, and navigates to the new URL.
         */
        export function nextOffset(page: string) {
            addPagingParam(OFFSET, getOffset() + getLimit(page));
        }

        /**
         * Invoked in tuple.xsl and explore.xsl. Decrements the offset query
         * parameter and navigates to the new URL.
         */
        export function previousOffset(page: string) {
            addPagingParam(OFFSET, Math.max(0, getOffset() - getLimit(page)));
        }

        /**
         * @returns {number} The value of the offset query parameter.
         */
        export function getOffset() {
            var offset = getQueryParameter(OFFSET);
            return ('' == offset) ? 0 : parseInt(offset, 10);
        }

        /**
         * @returns {number} The value of the limit query parameter.
         */
        export function getLimit(page: string): number {
            return parseInt($(LIM_ID + '_' + page).val(), 10);
        }

        /**
         * Retrieves the URL query parameter with the given name.
         * 
         * @param {String} name The name of the parameter to retrieve.
         * @returns {String} The value of the given parameter, or an empty
         *          string if it doesn't exist.
         */
        export function getQueryParameter(name: string): string {
            var rval = '';
            var elements = getQueryString(document.location.href).split(decodeURIComponent('%26'));
            for (var i = 0; elements.length - i; i++) {
                var pair = elements[i].split('=');
                if (name != pair[0]) {
                    continue;
                }
                rval = pair[1];
                // Keep looping. We are interested in the last value.
            }
            return rval;
        }

        /**
         * Gets whether a URL query parameter with the given name is present.
         * 
         * @param {String} name The name of the parameter to retrieve.
         * @returns {Boolean} True, if a parameter with the given name is in
         *                    the URL. Otherwise, false.
         */
        export function hasQueryParameter(name: string) {
            var rval = false;
            var elements = getQueryString(document.location.href).split(decodeURIComponent('%26'));
            for (var i = 0; elements.length - i; i++) {
                var pair = elements[i].split('=');
                if (name == pair[0]) {
                    rval = true;
                    break;
                }
            }
            return rval;
        }

        /**
         * Convenience function for returning the tail of a string after a
         * given character.
         *   
         * @param {String} value The string to get the tail of.
         * @param split
         *            character to give tail after
         * @returns The substring after the 'split' character, or the original
         *          string if 'split' is not found.
         */
        function tailAfter(value: string, split: string): string {
            return value.substring(value.indexOf(split) + 1);
        }

        export function getQueryString(href: string) {
            return tailAfter(tailAfter(href, '?'), ';');
        }

        /**
         * Using the value of the 'limit' query parameter, correct the text of the
         * Next and Previous buttons. Makes use of RegExp to preserve any
         * localization.
         */
        export function correctButtons(page: string) {
            var buttonWordPattern = /^[A-z]+\s+/;
            var nextButton = $('#nextX');
            var oldNext = nextButton.val();
            var count = parseInt(/\d+$/.exec(oldNext)[0], 10);
            var limit = workbench.paging.getLimit(page);
            nextButton.val(buttonWordPattern.exec(oldNext)[0] + limit);
            var previousButton = $('#previousX');
            previousButton
                .val(buttonWordPattern.exec(previousButton.val())[0] + limit);
            var offset = workbench.paging.getOffset();
            previousButton.prop('disabled', (offset <= 0 || limit <= 0));
            nextButton.prop('disabled',
                (count < limit || limit <= 0 || (offset + count) >= getTotalResultCount()));
        }

        /**
         * Gets the total result count, preferably from the 'know_total' query
         * parameter. If the parameter doesn't exist, get it from the
         * 'total_result_count' cookie.
         * 
         * @returns {Number} The given total result count, or zero if it isn't
         *          given.
         */
        export function getTotalResultCount() {
            var total_result_count = 0;
            var resultMetadata = document.getElementById(RESULT_TOTAL_COUNT_ID) as HTMLInputElement;
            if (resultMetadata && resultMetadata.value) {
                var resultMetadataCount = parseInt(resultMetadata.value, 10);
                if (!isNaN(resultMetadataCount)) {
                    return resultMetadataCount;
                }
            }
            var s_trc = workbench.paging.getQueryParameter(KT);
            if (s_trc.length == 0) {
                s_trc = workbench.getCookie('total_result_count');
            }

            if (s_trc.length > 0) {
                total_result_count = parseInt(s_trc, 10);
            }

            return total_result_count;
        }

        module DataTypeVisibility {
            function setCookie(c_name: string, value: boolean, exdays: number) {
                var exdate = new Date();
                exdate.setDate(exdate.getDate() + exdays);
                document.cookie = c_name + "=" + value + 
                    ((exdays == null) ? "" : 
                    "; expires=" + exdate.toUTCString());
            }

            export function setShow(show: boolean) {
                setCookie('show-datatypes', show, 365);
                var data = show ? 'data-longform' : 'data-shortform';
                $('div.resource[' + data + ']').each(function() {
                    var me = $(this);
                    me.find('a:first').text(decodeURIComponent(me.attr(data)));
                });
            }
        }

        export function setShowDataTypesCheckboxAndSetChangeEvent() {
            var hideDataTypes = (workbench.getCookie('show-datatypes') == 'false');
            var showDTcb = $("input[name='show-datatypes']");
            if (hideDataTypes) {
                showDTcb.prop('checked', false);
                DataTypeVisibility.setShow(false);
            }
            showDTcb.on('change', function() {
                DataTypeVisibility.setShow(showDTcb.prop('checked'));
            });
        }
    }
}
