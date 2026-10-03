/// <reference path="template.ts" />
/// <reference path="jquery.d.ts" />
/// <reference path="yasqeHelper.ts" />
// WARNING: Do not edit the *.js version of this file. Instead, always edit the
// corresponding *.ts source in the ts subfolder, and then invoke the
// compileTypescript.sh bash script to generate new *.js and *.js.map files.
var workbench;
(function (workbench) {
    var update;
    (function (update) {
        var yasqe = null;
        var releaseSizing = null;
        /** Cmd/Ctrl+Enter submits the update form through its submit handler (which saves the editor text). */
        function submitUpdateForm() {
            var form = document.getElementById('update-form');
            if (form && typeof form.requestSubmit === 'function') {
                form.requestSubmit();
            }
        }
        function initYasqe(root) {
            workbench.yasqeHelper.setupCompleters(namespaces);
            yasqe = YASQE.fromTextArea(root.querySelector('#update'), {
                createShareLink: function () {
                    return { update: yasqe.getValue() };
                },
                consumeShareLink: function (yasqe, args) {
                    if (args.update)
                        yasqe.setValue(args.update);
                },
                // This way, we don't conflict with the YASQE editor of the
                // regular query interface, and we show the most recent
                // -update- query.
                persistent: "update",
                // Updates run through the Workbench form; YASQE must never send them to its default endpoint.
                sparql: { endpoint: '', showQueryButton: false },
                extraKeys: {
                    'Ctrl-Enter': submitUpdateForm,
                    'Cmd-Enter': submitUpdateForm
                }
            });
            // The editor frame grows with its content like the Query editor (styles/query.css) and can be
            // resized with the handle under it.
            var sizing = workbench.editorSizing;
            var handle = root.querySelector('#update-editor-resize');
            if (sizing && typeof sizing.install === 'function' && handle) {
                releaseSizing = sizing.install(yasqe, handle, 'rdf4j.workbench.update-editor-height.v1');
            }
            yasqe.refresh();
            // If the text area we instantiated YASQE on has no query val,
            // then show a regular default update query.
            if (yasqe.getValue().trim().length == 0) {
                yasqe.setValue('INSERT DATA {\n\t<http://exampleSub> ' +
                    '<http://examplePred> <http://exampleObj> .\n}');
            }
        }
        /** Route mount (plan task M9.1): open the editor; the returned function closes it again. */
        function mount(outlet) {
            initYasqe(outlet);
            return function () {
                if (typeof releaseSizing === 'function') {
                    releaseSizing();
                }
                releaseSizing = null;
                if (yasqe) {
                    yasqe.toTextArea();
                    yasqe = null;
                }
            };
        }
        update.mount = mount;
        /**
         * Invoked upon form submission.
         *
         * @returns {boolean} true, always
         */
        function doSubmit() {
            // Save yasqe content to text area.
            if (yasqe) {
                yasqe.save();
            }
            return true;
        }
        update.doSubmit = doSubmit;
    })(update = workbench.update || (workbench.update = {}));
})(workbench || (workbench = {}));
//# sourceMappingURL=update.js.map