/// <reference path="template.ts" />
/// <reference path="jquery.d.ts" />
// WARNING: Do not edit the *.js version of this file. Instead, always edit the
// corresponding *.ts source in the ts subfolder, and then invoke the
// compileTypescript.sh bash script to generate new *.js and *.js.map files.
var workbench;
(function (workbench) {
    var add;
    (function (add) {
        function enabledInput(selected) {
            var istext = (selected == 'text');
            $('#add-source-file-panel').prop('hidden', selected != 'file');
            $('#add-source-url-panel').prop('hidden', selected != 'url');
            $('#add-source-text-panel').prop('hidden', !istext);
            $('#text').prop('disabled', !istext);
            var contentType = $('#Content-Type');
            $('#source-' + selected).prop('checked', true);
            var isfile = (selected == 'file');
            var file = $('#file');
            file.prop('disabled', !isfile);
            var isurl = (selected == 'url');
            var url = $('#url');
            url.prop('disabled', !isurl);
            var autodetect = contentType.find("option[value='autodetect']");
            if (istext) {
                autodetect.prop('disabled', true);
                var turtle = contentType.find("option[value='application/x-turtle']");
                if (turtle.length == 0) {
                    turtle = contentType.find("option[value='text/turtle']");
                }
                if (turtle.length > 0) {
                    turtle.prop('selected', true);
                }
            }
            else {
                autodetect.prop('disabled', false);
                autodetect.prop('selected', true);
                if (isfile) {
                    $('#baseURI').val(file.val() == '' ? '' : encodeURI('file://'
                        + file.val().replace(/\\/g, '/')));
                }
                else if (isurl) {
                    $('#baseURI').val(url.val());
                }
            }
        }
        add.enabledInput = enabledInput;
    })(add = workbench.add || (workbench.add = {}));
})(workbench || (workbench = {}));
workbench.addLoad(function addPageLoaded() {
    var selected = $("input[name='source']:checked").val() || 'file';
    workbench.add.enabledInput(selected == 'contents' ? 'text' : selected);
});
//# sourceMappingURL=add.js.map