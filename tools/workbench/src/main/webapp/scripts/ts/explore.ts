/// <reference path="template.ts" />
/// <reference path="jquery.d.ts" />
/// <reference path="paging.ts" />

// WARNING: Do not edit the *.js version of this file. Instead, always edit the
// corresponding *.ts source in the ts subfolder, and then invoke the
// compileTypescript.sh bash script to generate new *.js and *.js.map files.

module workbench {

    export module explore {

        /**
         * Route mount (plan task M7.2): fill the resource summary and the row count and bind the datatype toggle, all
         * inside the outlet. The view leaves out repeated list items (M12.1). The returned function unbinds the toggle.
         */
        export function mount(outlet: HTMLElement): () => void {
            // The view says when a page has no rows (.workbench-empty); the result tables lay their rows out later.
            function syncExplorePaginationVisibility() {
                var pagination = outlet.querySelector('#explore-pagination') as HTMLElement;
                if (!pagination) {
                    return;
                }
                var emptyPage = Boolean(outlet.querySelector('#explore-results .workbench-empty'));
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
                }
            }
            // Without limit_explore the server uses 100 (ExploreServlet ignores the cookie), as the view's select shows.
            // A new Result limit applies at once, like Show datatypes (C19).
            page.find(limit_id).on('change.wbRoute', function() {
                var chosen = parseInt(<string>$(this).val(), 10);
                if (!isNaN(chosen)) {
                    workbench.paging.addPagingParam(limit_param, chosen);
                }
            });
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
                var empty_page = Boolean(outlet.querySelector('#explore-results .workbench-empty'));

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
            return function() {
                page.find("input[name='show-datatypes']").off('.wbRoute');
                page.find(limit_id).off('.wbRoute');
            };
        }
    }
}
