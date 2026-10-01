/// <reference path="template.ts" />
/// <reference path="jquery.d.ts" />
/// <reference path="paging.ts" />
// WARNING: Do not edit the *.js version of this file. Instead, always edit
// the corresponding *.ts source in the ts subfolder, and then invoke the
// compileTypescript.sh bash script to generate new *.js and *.js.map files.
var workbench;
(function (workbench) {
    var exportPage;
    (function (exportPage) {
        /** Route mount (plan task M7.2): restore the preview limit from the URL or its cookie. */
        function mount(outlet) {
            var suffix = '_export';
            var limitParam = workbench.paging.LIMIT + suffix;
            var limitElement = $(outlet).find(workbench.paging.LIM_ID + suffix);
            function setElement(num) {
                var limit = parseInt(num, 10);
                limitElement.val(String(isNaN(limit) || limit < 0 ? 100 : limit));
            }
            var limit = workbench.paging.hasQueryParameter(limitParam) ?
                workbench.paging.getQueryParameter(limitParam) :
                workbench.getCookie(limitParam);
            setElement(limit === null || typeof limit === 'undefined' || limit === '' ? '100' : limit);
            return function () { };
        }
        exportPage.mount = mount;
    })(exportPage = workbench.exportPage || (workbench.exportPage = {}));
})(workbench || (workbench = {}));
//# sourceMappingURL=export.js.map