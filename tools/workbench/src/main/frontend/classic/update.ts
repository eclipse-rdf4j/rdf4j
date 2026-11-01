/// <reference path="template.ts" />
/// <reference path="jquery.d.ts" />
/// <reference path="yasqeHelper.ts" />

// WARNING: Do not edit the *.js version of this file. Instead, always edit the
// corresponding *.ts source in the ts subfolder, and then invoke the
// compileTypescript.sh bash script to generate new *.js and *.js.map files.

namespace workbench {

    export namespace update {

        // Need to declare YASQE library for typescript compilation.
        declare var YASQE:any;
        declare var namespaces:{string:string};
        var yasqe:any = null;
        var releaseSizing: () => void = null;

        /** Cmd/Ctrl+Enter submits the update form through its submit handler (which saves the editor text). */
        function submitUpdateForm() {
            var form = <HTMLFormElement>document.getElementById('update-form');
            if (form && typeof form.requestSubmit === 'function') {
                form.requestSubmit();
            }
        }

        function initYasqe(root: HTMLElement) {
            workbench.yasqeHelper.setupCompleters(namespaces);
            yasqe = YASQE.fromTextArea(root.querySelector('#update'), {
                createShareLink: function () {
                    return {update: yasqe.getValue()};
                },
                consumeShareLink: function (yasqe:any, args:any) {
                    if (args && args.update) {
                        yasqe.setValue(args.update);
                    }
                },

                // This way, we don't conflict with the YASQE editor of the
                // regular query interface, and we show the most recent
                // -update- query.
                persistent: "update",
                // Updates run through the Workbench form; YASQE must never send them to its default endpoint.
                sparql: { endpoint: '', showQueryButton: false },
                screenReaderLabel: 'SPARQL update',
                extraKeys: {
                    'Ctrl-Enter': submitUpdateForm,
                    'Cmd-Enter': submitUpdateForm
                }
            });

            workbench.yasqeHelper.enableToolbarKeyboard(yasqe);
            yasqe.on('change', clearErrorLocation);

            // The editor frame grows with its content like the Query editor (styles/query.css) and can be
            // resized with the handle under it.
            var sizing: any = (<any>workbench).editorSizing;
            var handle = root.querySelector('#update-editor-resize');
            if (sizing && typeof sizing.install === 'function' && handle) {
                releaseSizing = sizing.install(yasqe, handle, 'rdf4j.workbench.update-editor-height.v1');
            }
            yasqe.refresh();

            // If the text area we instantiated YASQE on has no query val,
            // then show a regular default update query.
            if (yasqe.getValue().trim().length == 0) {
                yasqe.setValue('INSERT DATA {\n\t<http://exampleSub> '+
                    '<http://examplePred> <http://exampleObj> .\n}');
            }
        }

        /** The editor line (0-based) marked as where the server's parser stopped, or null. */
        var errorLine: number = null;

        /**
         * Mark the line (1-based) where the server's parser stopped, as the Query page does; with reveal, also put the
         * cursor at the column and scroll it into view.
         */
        export function showErrorLocation(line: number, column: number, reveal: boolean): void {
            if (!yasqe || !(line >= 1)) {
                return;
            }
            clearErrorLocation();
            var doc = yasqe.getDoc();
            var index = Math.min(line - 1, Math.max(0, doc.lineCount() - 1));
            var marker = document.createElement('span');
            marker.className = 'query-editor-error-marker';
            marker.title = 'The server reported a syntax error on this line';
            yasqe.setGutterMarker(index, 'gutterErrorBar', marker);
            yasqe.addLineClass(index, 'background', 'query-editor-error-line');
            errorLine = index;
            if (reveal) {
                yasqe.focus();
                doc.setCursor({ line: index, ch: Math.max(0, column - 1) });
                yasqe.scrollIntoView(null, 80);
            }
        }

        function clearErrorLocation(): void {
            if (errorLine === null) {
                return;
            }
            if (yasqe) {
                yasqe.setGutterMarker(errorLine, 'gutterErrorBar', null);
                yasqe.removeLineClass(errorLine, 'background', 'query-editor-error-line');
            }
            errorLine = null;
        }

        /** Route mount (plan task M9.1): open the editor; the returned function closes it again. */
        export function mount(outlet: HTMLElement): () => void {
            initYasqe(outlet);
            return function() {
                if (typeof releaseSizing === 'function') {
                    releaseSizing();
                }
                releaseSizing = null;
                if (yasqe) {
                    // Full screen hides the document's scrollbars (CodeMirror's add-on); closing the editor alone
                    // would leave the next page unable to scroll.
                    if (typeof yasqe.getOption === 'function' && yasqe.getOption('fullScreen')) {
                        yasqe.setOption('fullScreen', false);
                    }
                    yasqe.toTextArea();
                    yasqe = null;
                    errorLine = null;
                }
            };
        }

        /**
         * Invoked upon form submission.
         *
         * @returns {boolean} true, always
         */
        export function doSubmit() {
            // Save yasqe content to text area.
            if (yasqe) {
                yasqe.save();
            }
            return true;
        }
    }
}
