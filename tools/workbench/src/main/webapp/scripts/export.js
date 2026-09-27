/// <reference path="template.ts" />
/// <reference path="jquery.d.ts" />
/// <reference path="paging.ts" />
// WARNING: Do not edit the *.js version of this file. Instead, always edit
// the corresponding *.ts source in the ts subfolder, and then invoke the
// compileTypescript.sh bash script to generate new *.js and *.js.map files.
workbench.addLoad(function () {
    var suffix = '_export';
    var limitParam = workbench.paging.LIMIT + suffix;
    var limitElement = $(workbench.paging.LIM_ID + suffix);
    function setElement(num) {
        var limit = parseInt(num, 10);
        limitElement.val(String(isNaN(limit) || limit < 0 ? 100 : limit));
    }
    var limit = workbench.paging.hasQueryParameter(limitParam) ?
        workbench.paging.getQueryParameter(limitParam) :
        workbench.getCookie(limitParam);
    setElement(limit === null || typeof limit === 'undefined' || limit === '' ? '100' : limit);
});
//# sourceMappingURL=export.js.map