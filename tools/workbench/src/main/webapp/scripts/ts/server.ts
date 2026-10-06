/// <reference path="template.ts" />
/// <reference path="jquery.d.ts" />

// WARNING: Do not edit the *.js version of this file. Instead, always edit the
// corresponding *.ts source in the ts subfolder, and then invoke the
// compileTypescript.sh bash script to generate new *.js and *.js.map files.

module workbench {

    export module server {
        /**
         * Base64 of the UTF-8 bytes of a text. window.btoa only takes characters up to U+00FF, so a password such as
         * 'pass✓' is encoded to UTF-8 first; the server decodes the credentials as UTF-8.
         */
        export function encodeUtf8Base64(text: string): string {
            var binary = '';
            var TextEncoderType = (<any>window).TextEncoder;
            if (typeof TextEncoderType === 'function') {
                var bytes: Uint8Array = new TextEncoderType().encode(text);
                for (var i = 0; i < bytes.length; i++) {
                    binary += String.fromCharCode(bytes[i]);
                }
            } else {
                binary = unescape(encodeURIComponent(text));
            }
            return window.btoa(binary);
        }
    }
}

/**
 * Invoked by the rendered change server form.
 */
function changeServer(event: JQueryEventObject) {
    event.preventDefault();
    var form = <HTMLFormElement>$(event.target).closest('form').get(0);
    var user = $('#server-user').prop('value');
    var password = $('#server-password').prop('value');
    if (user && password) {
        var decoded = user + ':' + password;
        var encoded = window.btoa ? workbench.server.encodeUtf8Base64(decoded) : decoded;
        $('#server-password').attr('name', 'server-user-password').prop('value', encoded);
    }
    if (form) {
        form.submit();
    }
}
