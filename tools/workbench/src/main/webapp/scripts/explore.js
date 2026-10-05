/// <reference path="template.ts" />
/// <reference path="jquery.d.ts" />
/// <reference path="paging.ts" />
// WARNING: Do not edit the *.js version of this file. Instead, always edit the
// corresponding *.ts source in the ts subfolder, and then invoke the
// compileTypescript.sh bash script to generate new *.js and *.js.map files.
var workbench;
(function (workbench) {
    var explore;
    (function (explore_1) {
        /**
         * Route mount (plan task M7.2): fill the resource summary and the row count and bind the datatype toggle, all
         * inside the outlet. The view leaves out repeated list items (M12.1). The returned function unbinds the toggle.
         */
        function mount(outlet) {
            function syncExplorePaginationVisibility() {
                var pagination = outlet.querySelector('#explore-pagination');
                if (!pagination) {
                    return;
                }
                var resultRows = outlet.querySelectorAll('#explore-results table.data tbody tr');
                var resultTable = outlet.querySelector('#explore-results table.data');
                var emptyResult = outlet.querySelector('#explore-results .workbench-empty');
                var emptyPage = Boolean(emptyResult) || Boolean(resultTable && resultRows.length === 0);
                if (resultTable) {
                    resultTable.hidden = resultRows.length === 0;
                }
                var offset = workbench.paging.getOffset();
                pagination.hidden = emptyPage && offset <= 0;
            }
            var page = $(outlet);
            // Populate parameters
            var elements = workbench.getQueryStringElements();
            var resource = page.find('#resource');
            var suffix = '_explore';
            var limit_param = workbench.paging.LIMIT + suffix;
            var limit_id = workbench.paging.LIM_ID + suffix;
            var limit_param_found = false;
            for (var i = 0; elements.length - i; i++) {
                // A '+' in the query string is a space; an encoded one (%2B, as in <…/C++>) stays a '+'.
                var separator = elements[i].indexOf('=');
                var pair = separator < 0 ? [elements[i], ''] : [elements[i].substring(0, separator),
                    elements[i].substring(separator + 1)];
                var value = decodeURIComponent(pair[1].replace(/\+/g, ' '));
                if ('resource' == pair[0]) {
                    resource.val(value);
                }
                else if (limit_param == pair[0]) {
                    page.find(limit_id).val(value);
                    limit_param_found = true;
                }
            }
            if (!limit_param_found) {
                var limit_cookie = workbench.getCookie(limit_param);
                if (limit_cookie) {
                    page.find(limit_id).val(limit_cookie);
                }
            }
            var explore = 'explore';
            workbench.paging.correctButtons(explore);
            var rvalue = resource.val();
            var summary = outlet.querySelector('#explore-resource-summary');
            var resourceValue = outlet.querySelector('#explore-resource-value');
            var resultCount = outlet.querySelector('#explore-result-count');
            // The view leaves these spans empty for this script, even when it renders a new page in place.
            if (resourceValue) {
                resourceValue.textContent = rvalue || '';
            }
            if (resultCount) {
                resultCount.textContent = '';
            }
            if (rvalue) {
                if (summary && resourceValue && resultCount) {
                    summary.removeAttribute('hidden');
                }
                var limit = workbench.paging.getLimit(explore);
                // Modify title to reflect total_result_count cookie
                var total_result_count = workbench.paging.getTotalResultCount();
                var have_total_count = (total_result_count > 0);
                var offset = limit == 0 ? 0 : workbench.paging.getOffset();
                var first = offset + 1;
                var last = limit == 0 ? total_result_count : offset + limit;
                var result_rows = outlet.querySelectorAll('#explore-results table.data tbody tr');
                var result_table = outlet.querySelector('#explore-results table.data');
                var empty_result = outlet.querySelector('#explore-results .workbench-empty');
                var empty_page = Boolean(empty_result) || Boolean(result_table && result_rows.length === 0);
                // Truncate range if close to end.
                last = have_total_count ? Math.min(total_result_count, last) : last;
                var range = 'Rows ' + first + '–' + last;
                if (empty_page) {
                    range = have_total_count ? 'Rows 0 of ' + total_result_count : 'No rows';
                }
                else if (have_total_count) {
                    range = range + ' of ' + total_result_count;
                }
                if (resultCount) {
                    resultCount.textContent = range;
                }
            }
            workbench.paging.setShowDataTypesCheckboxAndSetChangeEvent(outlet);
            syncExplorePaginationVisibility();
            return function () {
                page.find("input[name='show-datatypes']").off('.wbRoute');
            };
        }
        explore_1.mount = mount;
    })(explore = workbench.explore || (workbench.explore = {}));
})(workbench || (workbench = {}));
//# sourceMappingURL=explore.js.map