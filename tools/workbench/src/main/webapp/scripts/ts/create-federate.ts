/// <reference path="template.ts" />
/// <reference path="jquery.d.ts" />

// WARNING: Do not edit the *.js version of this file. Instead, always edit the
// corresponding *.ts source in the ts subfolder, and then invoke the
// compileTypescript.sh bash script to generate new *.js and *.js.map files.

module workbench {

    export module createFederate {

        /**
         * Route mount (plan task M7.2): Create is enabled only for a new id and at least two members. The id
         * check replaces create.js's plain one (both use the .wbRoute namespace).
         */
        export function mount(outlet: HTMLElement): () => void {
            var page = $(outlet);
            var memberID = page.find('input.memberID');
            var typeChoice = page.find("input[name='type']");
            var id = page.find('#id');

            function respondToFormState() {
                var enoughMembers = memberID.filter(':checked').length >= 2;
                if (enoughMembers) {
                    page.find('#create-feedback').hide();
                } else {
                    page.find('#create-feedback').show();
                }
                var fedID = id.val();
                var validID = /.+/.test(fedID);
                var disable = !(validID && enoughMembers);
                var matchExisting = false;

                // test that fedID not equal any existing id
                memberID.each(function() {
                    if (fedID == $(this).attr('value')) {
                        disable = true;
                        matchExisting = true;
                        return false;
                    }
                });
                var recurseMessage = page.find('#recurse-message');
                if (matchExisting) {
                    recurseMessage.show();
                } else {
                    recurseMessage.hide();
                }
                page.find('input#create').prop('disabled', disable);
            }

            /**
             * Calls another function with a delay of 0 msec. (Workaround for annoying
             * browser behavior.)
             */
            function timeoutRespond() {
                setTimeout(respondToFormState, 0);
            }

            respondToFormState();
            memberID.on('change.wbRoute', respondToFormState);
            typeChoice.on('change.wbRoute', respondToFormState);
            id.off('.wbRoute').on('keydown.wbRoute paste.wbRoute cut.wbRoute', timeoutRespond);
            return function() {
                memberID.off('.wbRoute');
                typeChoice.off('.wbRoute');
                id.off('.wbRoute');
            };
        }
    }
}
