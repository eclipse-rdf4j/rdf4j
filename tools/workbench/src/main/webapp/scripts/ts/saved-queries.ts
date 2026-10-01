/// <reference path="template.ts" />
/// <reference path="jquery.d.ts" />

// WARNING: Do not edit the *.js version of this file. Instead, always edit the
// corresponding *.ts source in the ts subfolder, and then invoke the
// compileTypescript.sh bash script to generate new *.js and *.js.map files.

module workbench {

    export module savedQueries {
        //need to declar YASQE library for typescript compilation
        declare var YASQE: any;
        
        export function deleteQuery(savedBy: string, name: string, urn: string) {
            var encoded = workbench.getCookie("server-user-password");
            var decoded = encoded && window.atob ? window.atob(encoded) : encoded;
            var currentUser = decoded && decoded.substring(0, decoded.indexOf(':'));
            if ((!savedBy || currentUser == savedBy)) {
                workbench.confirmDialog.open({
                    title: 'Delete saved query?',
                    body: "'" + name + "' will no longer be accessible, even using your browser's history.",
                    confirmLabel: 'Delete',
                    danger: true
                }).then(function(confirmed: boolean) {
                    if (confirmed) {
                        (<HTMLFormElement>document.forms.namedItem(urn)).submit();
                    }
                });
            } else {
                alert("'" + name + "' was saved by user '" + savedBy + "'.\nUser '"
                    + currentUser + "' is not allowed do delete it.");
            }
        }

        function toggleElement(urn: string, suffix: string) {
           var htmlElement = document.getElementById(urn + suffix);
	       htmlElement.style.display = (htmlElement.style.display == 'none') ? '' : 'none';
        }
        var yasqeInstances: { [index:string]: any } = {};
        function toggleYasqe(urn: string) {
            if (yasqeInstances[urn]) {
                //hide it
                if (yasqeInstances[urn]) {
                    yasqeInstances[urn].toTextArea();//simple way to close instances
                    yasqeInstances[urn] = null;
                }
                //now we only have the text-area. Hide that element as well
                document.getElementById(urn + '-text').style.display = 'none';
            } else {
                //show it
                var el = <HTMLInputElement>document.getElementById(urn + '-text');
                //but: somehow the xsl adds lots of spaces before/after the saved query. Couldnt figure out why, so just trim the string before initialization
                el.value = el.value.trim();
                yasqeInstances[urn] = YASQE.fromTextArea(el, {readOnly: 'nocursor', createShareLink: null});//initialize as read-only
                $(yasqeInstances[urn].getWrapperElement()).css({"fontSize": "14px", "height": "auto"});//set height to auto, i.e. resize to fit content
                //we made a change to the css wrapper element (and did so after initialization). So, force a manual update of the yasqe instance
                yasqeInstances[urn].refresh();
            }
        }
        
        
        /** Show or hide a saved query's details; the button says which one it will do next. */
        export function toggle(urn: string) {
            toggleElement(urn, '-metadata');
            toggleYasqe(urn);

            var toggle = document.getElementById(urn + '-toggle');
            var expanded = toggle.getAttribute('aria-expanded') != 'true';
            toggle.setAttribute('aria-expanded', expanded ? 'true' : 'false');
            toggle.textContent = expanded ? 'Hide details' : 'Show details';
        }

        /**
         * Route mount (plan task M9.1): tidy the rendered query texts and bind the Delete and details buttons in the
         * .wbRoute namespace. The returned function unbinds them and closes every opened editor.
         */
        export function mount(outlet: HTMLElement): () => void {
            var page = $(outlet);
            // not using jQuery.html(...) for this since it doesn't do the whitespace correctly
            var queries = outlet.getElementsByTagName('pre');
            for (var i = 0; i < queries.length; i++) {
                queries[i].innerHTML = queries[i].innerHTML.trim();
            }

            page.find('[name="edit-query"]').find('[name="query"]').each(function() {
                $(this).attr('value', $(this).attr('value').trim());
            });

            var deletes = page.find('.saved-query-delete');
            deletes.each(function() {
                var button = $(this);
                button.on('click.wbRoute', function() {
                    deleteQuery(button.attr('data-query-owner'), button.attr('data-query-name'),
                        button.attr('data-query-urn'));
                });
            });

            var toggles = page.find('.saved-query-toggle');
            toggles.each(function() {
                var button = $(this);
                button.on('click.wbRoute', function() {
                    toggle(button.attr('data-query-urn'));
                });
            });

            return function() {
                deletes.off('.wbRoute');
                toggles.off('.wbRoute');
                Object.keys(yasqeInstances).forEach(function(urn) {
                    if (yasqeInstances[urn]) {
                        yasqeInstances[urn].toTextArea();
                    }
                    delete yasqeInstances[urn];
                });
            };
        }
    }
}
