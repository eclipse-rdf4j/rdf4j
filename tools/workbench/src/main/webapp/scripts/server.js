/// <reference path="template.ts" />
/// <reference path="jquery.d.ts" />
// WARNING: Do not edit the *.js version of this file. Instead, always edit the
// corresponding *.ts source in the ts subfolder, and then invoke the
// compileTypescript.sh bash script to generate new *.js and *.js.map files.
var workbench;
(function (workbench) {
    var server;
    (function (server) {
        /**
         * Base64 of the UTF-8 bytes of a text. window.btoa only takes characters up to U+00FF, so a password such as
         * 'pass✓' is encoded to UTF-8 first; the server decodes the credentials as UTF-8.
         */
        function encodeUtf8Base64(text) {
            var binary = '';
            var TextEncoderType = window.TextEncoder;
            if (typeof TextEncoderType === 'function') {
                var bytes = new TextEncoderType().encode(text);
                for (var i = 0; i < bytes.length; i++) {
                    binary += String.fromCharCode(bytes[i]);
                }
            }
            else {
                binary = unescape(encodeURIComponent(text));
            }
            return window.btoa(binary);
        }
        server.encodeUtf8Base64 = encodeUtf8Base64;
    })(server = workbench.server || (workbench.server = {}));
})(workbench || (workbench = {}));
/**
 * Invoked by the rendered change server form.
 */
function changeServer(event) {
    event.preventDefault();
    var form = $(event.target).closest('form').get(0);
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
//# sourceMappingURL=server.js.map