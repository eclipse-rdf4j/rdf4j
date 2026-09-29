/// <reference path="template.ts" />
/// <reference path="jquery.d.ts" />

// WARNING: Do not edit the *.js version of this file. Instead, always edit the
// corresponding *.ts source in the ts subfolder, and then invoke the
// compileTypescript.sh bash script to generate new *.js and *.js.map files.

module workbench {

    export module namespaces {

        /**
         * Invoked by the rendered #prefix-select element.
         */
        export function updatePrefix() {
            var select = $('#prefix-select');
            $('#prefix').val(select.find('option:selected').text());
            $('#namespace').val(select.val());
        }
    }
}
