// WARNING: Do not edit the *.js version of this file. Instead, always edit the
// corresponding *.ts source in the ts subfolder, and then invoke the
// compileTypescript.sh bash script to generate new *.js and *.js.map files.
var workbench;
(function (workbench) {
    var requestIdCounter = 0;
    function createFallbackRequestId() {
        requestIdCounter += 1;
        var timestampPart = ('000000000000' + Date.now().toString(16)).slice(-12);
        var counterPart = ('00000000' + requestIdCounter.toString(16)).slice(-8);
        var randomPart = '';
        var cryptoObject = window.crypto || window.msCrypto;
        if (cryptoObject && cryptoObject.getRandomValues) {
            var buffer = new Uint16Array(4);
            cryptoObject.getRandomValues(buffer);
            for (var i = 0; i < buffer.length; i++) {
                randomPart += ('0000' + buffer[i].toString(16)).slice(-4);
            }
        }
        else {
            while (randomPart.length < 16) {
                randomPart += ('00000000' + Math.floor(Math.random() * 0xffffffff).toString(16)).slice(-8);
            }
            randomPart = randomPart.substring(0, 16);
        }
        return timestampPart + '-' + randomPart.substring(0, 4) + '-4'
            + randomPart.substring(4, 7) + '-a' + randomPart.substring(7, 10)
            + '-' + randomPart.substring(10, 16) + counterPart.substring(0, 6);
    }
    function generateRequestId() {
        var cryptoObject = window.crypto || window.msCrypto;
        if (cryptoObject && cryptoObject.randomUUID) {
            return cryptoObject.randomUUID();
        }
        return createFallbackRequestId();
    }
    workbench.generateRequestId = generateRequestId;
    // The following is to allow composed XSLT style sheets to each add
    // functions to the window.onload event.
    function chain(args) {
        return function () {
            for (var i = 0; i < args.length; i++) {
                args[i]();
            }
        };
    }
    // Note that the way this is currently constructed, functions added with
    // addLoad() will be executed in the order that they were added.
    //
    // @see
    // http://onwebdevelopment.blogspot.com/2008/07/chaining-functions-in-javascript.html
    // @param fn
    // function to add
    function addLoad(fn) {
        window.onload = typeof (window.onload) == 'function' ? chain([
            window.onload, fn
        ]) : fn;
    }
    workbench.addLoad = addLoad;
    /**
     * Retrieves the value of the cookie with the given name.
     *
     * @param {String} name The name of the cookie to retrieve.
     * @returns {String} The value of the given cookie, or an empty string if it
     *          doesn't exist.
     */
    function getCookie(name) {
        var cookies = document.cookie.split(';');
        var rval = '';
        for (var i = 0; i < cookies.length; i++) {
            var cookie = cookies[i];
            var eq = cookie.indexOf('=');
            if (name == cookie.substr(0, eq).replace(/^\s+|\s+$/g, '')) {
                rval = decodeURIComponent(cookie.substr(eq + 1).replace(/\+/g, '%20'));
                break;
            }
        }
        return rval;
    }
    workbench.getCookie = getCookie;
    /**
     * Parses workbench URL query strings into processable arrays.
     *
     * @returns an array of the 'name=value' substrings of the URL query string
     */
    function getQueryStringElements() {
        var href = document.location.href;
        return href.substring(href.indexOf('?') + 1).split(decodeURIComponent('%26'));
    }
    workbench.getQueryStringElements = getQueryStringElements;
    /**
     * Utility method for assembling the query string for a request URL.
     *
     * @param sb
     *            string buffer, actually an array of strings to be joined later
     * @param id
     *            name of parameter to add, also the id of the document element
     *            to get the value from
     */
    function addParam(sb, id) {
        sb[sb.length] = id + '=';
        var tag = document.getElementById(id);
        sb[sb.length] = tag.type == 'checkbox' ? String(tag.checked) :
            encodeURIComponent(tag.value);
        sb[sb.length] = '&';
    }
    workbench.addParam = addParam;
})(workbench || (workbench = {}));
/**
 * Code to run when the document loads: eliminate the 'noscript' warning
 * message, and display an unauthenticated user properly.
 */
workbench
    .addLoad(function () {
    var noScriptMessage = document.getElementById('noscript-message');
    if (noScriptMessage) {
        noScriptMessage.style.display = 'none';
    }
    var encoded = workbench.getCookie("server-user-password");
    var decoded = encoded && window.atob ? window.atob(encoded) : encoded;
    var user = decoded && decoded.substring(0, decoded.indexOf(':'));
    var selectedUser = document.getElementById('selected-user');
    if (!selectedUser) {
        return;
    }
    if (!user || user == '""') {
        selectedUser.textContent = '';
        var anonymousUser = document.createElement('span');
        anonymousUser.className = 'disabled';
        anonymousUser.textContent = 'None';
        selectedUser.appendChild(anonymousUser);
    }
    else {
        selectedUser.textContent = user;
    }
});
//# sourceMappingURL=template.js.map