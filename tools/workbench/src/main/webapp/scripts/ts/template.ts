// WARNING: Do not edit the *.js version of this file. Instead, always edit the
// corresponding *.ts source in the ts subfolder, and then invoke the
// compileTypescript.sh bash script to generate new *.js and *.js.map files.

module workbench {

    var requestIdCounter = 0;

    function createFallbackRequestId(): string {
        requestIdCounter += 1;
        var timestampPart = ('000000000000' + Date.now().toString(16)).slice(-12);
        var counterPart = ('00000000' + requestIdCounter.toString(16)).slice(-8);
        var randomPart = '';
        var cryptoObject: any = (<any>window).crypto || (<any>window).msCrypto;

        if (cryptoObject && cryptoObject.getRandomValues) {
            var buffer = new Uint16Array(4);
            cryptoObject.getRandomValues(buffer);
            for (var i = 0; i < buffer.length; i++) {
                randomPart += ('0000' + buffer[i].toString(16)).slice(-4);
            }
        } else {
            while (randomPart.length < 16) {
                randomPart += ('00000000' + Math.floor(Math.random() * 0xffffffff).toString(16)).slice(-8);
            }
            randomPart = randomPart.substring(0, 16);
        }

        return timestampPart + '-' + randomPart.substring(0, 4) + '-4'
            + randomPart.substring(4, 7) + '-a' + randomPart.substring(7, 10)
            + '-' + randomPart.substring(10, 16) + counterPart.substring(0, 6);
    }

    export function generateRequestId(): string {
        var cryptoObject: any = (<any>window).crypto || (<any>window).msCrypto;
        if (cryptoObject && cryptoObject.randomUUID) {
            return cryptoObject.randomUUID();
        }
        return createFallbackRequestId();
    }

    export interface LoadRoutine {
        (ev?: Event): void;
    }

    // The following is to allow composed XSLT style sheets to each add
    // functions to the window.onload event.
    function chain(args: LoadRoutine[]): LoadRoutine {
            return function() {
            for (var i = 0; i < args.length; i++) {
                args[i]();
            }
        }
    }

    // Note that the way this is currently constructed, functions added with
    // addLoad() will be executed in the order that they were added.
    //
    // @see
    // http://onwebdevelopment.blogspot.com/2008/07/chaining-functions-in-javascript.html
    // @param fn
    // function to add
    export function addLoad(fn: LoadRoutine) {
        window.onload = typeof (window.onload) == 'function' ? chain([
            window.onload, fn]) : fn;
    }

    /**
     * Retrieves the value of the cookie with the given name.
     * 
     * @param {String} name The name of the cookie to retrieve.
     * @returns {String} The value of the given cookie, or an empty string if it
     *          doesn't exist.
     */
    export function getCookie(name: string) {
        var cookies = document.cookie.split(';');
        var rval = '';
        for (var i = 0; i < cookies.length; i++) {
            var cookie = cookies[i];
            var eq = cookie.indexOf('=');
            if (name == cookie.substr(0, eq).replace(/^\s+|\s+$/g, '')) {
                rval = decodeURIComponent(cookie.substr(eq + 1).replace(/\+/g,
                    '%20'));
                break;
            }
        }
        return rval;
    }

    /**
     * Parses workbench URL query strings into processable arrays.
     * 
     * @returns an array of the 'name=value' substrings of the URL query string
     */
    export function getQueryStringElements() {
        var href = document.location.href;
        return href.substring(href.indexOf('?') + 1).split(
            decodeURIComponent('%26'));
    }

    /**
     * Utility method for assembling the query string for a request URL.
     * 
     * @param sb
     *            string buffer, actually an array of strings to be joined later
     * @param id
     *            name of parameter to add, also the id of the document element
     *            to get the value from
     */
    export function addParam(sb: string[], id: string) {
        sb[sb.length] = id + '=';
        var tag = <HTMLInputElement>document.getElementById(id);
        sb[sb.length] = tag.type == 'checkbox' ? String(tag.checked) :
        encodeURIComponent(tag.value);
        sb[sb.length] = '&';
    }
}

/**
 * Code to run when the document loads: eliminate the 'noscript' warning
 * message, and display an unauthenticated user properly.
 */
workbench
    .addLoad(function() {
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
        } else {
            selectedUser.textContent = user;
        }
    });

/**
 * Keep the shared navigation usable at every Workbench route.  The XSL
 * template renders the complete list for desktop and this small controller
 * only changes its disclosure state at the narrow breakpoint.  It also marks
 * the route currently displayed by the browser so the same navigation works
 * for pages that do not load query.ts.
 */
workbench.addLoad(function installWorkbenchNavigation() {
    var disclosure = <HTMLDetailsElement>document.getElementById('workbench-navigation-disclosure');
    if (!disclosure) {
        return;
    }

    var mediaQuery = window.matchMedia ? window.matchMedia('(max-width: 900px)') : null;
    var syncDisclosure = function() {
        var isMobile = mediaQuery ? mediaQuery.matches : window.innerWidth <= 900;
        disclosure.open = !isMobile;
    };
    syncDisclosure();
    if (mediaQuery) {
        if (mediaQuery.addEventListener) {
            mediaQuery.addEventListener('change', syncDisclosure);
        } else if ((<any>mediaQuery).addListener) {
            (<any>mediaQuery).addListener(syncDisclosure);
        }
    }

    var currentPath = window.location.pathname.replace(/\/+$/, '');
    var currentSegment = currentPath.substring(currentPath.lastIndexOf('/') + 1);
    var entries = document.querySelectorAll('#navigation a[data-workbench-nav-href]');
    for (var i = 0; i < entries.length; i++) {
        var entry = <HTMLAnchorElement>entries[i];
        var target = entry.getAttribute('data-workbench-nav-href');
        if (!target) {
            continue;
        }
        var targetUrl = document.createElement('a');
        targetUrl.href = entry.href;
        var targetPath = targetUrl.pathname.replace(/\/+$/, '');
        var targetSegment = targetPath.substring(targetPath.lastIndexOf('/') + 1);
        if (targetSegment === currentSegment || (currentSegment === '' && targetSegment === 'repositories')) {
            var item = entry.parentElement;
            if (item) {
                item.className += ' current';
            }
            entry.setAttribute('aria-current', 'page');
        }
    }
});

/**
 * Keep disclosure triggers in the toolbar while their panels open below the
 * controls. Native details summaries move with their content in a wrapping
 * flex row, so these button/panel pairs provide a stable keyboard and pointer
 * interaction for both the query page and embedded result documents.
 */
workbench.addLoad(function installDisclosureToggles() {
    var toggles = document.querySelectorAll('.query-disclosure__toggle');
    for (var i = 0; i < toggles.length; i++) {
        var toggle = <HTMLButtonElement>toggles[i];
        var panelId = toggle.getAttribute('aria-controls');
        var panel = panelId ? document.getElementById(panelId) : null;
        if (!panel) {
            continue;
        }
        var container = toggle.parentElement;
        var setExpanded = function(button: HTMLButtonElement, target: HTMLElement, owner: HTMLElement,
                                  expanded: boolean) {
            button.setAttribute('aria-expanded', expanded ? 'true' : 'false');
            target.hidden = !expanded;
            if (owner) {
                owner.classList.toggle('is-open', expanded);
            }
            window.dispatchEvent(new Event('resize'));
        };
        var initiallyExpanded = toggle.getAttribute('aria-expanded') === 'true';
        setExpanded(toggle, panel, container, initiallyExpanded);
        toggle.addEventListener('click', (function(button: HTMLButtonElement, target: HTMLElement, owner: HTMLElement) {
            return function() {
                setExpanded(button, target, owner, button.getAttribute('aria-expanded') !== 'true');
            };
        })(toggle, panel, container), false);
    }
});
