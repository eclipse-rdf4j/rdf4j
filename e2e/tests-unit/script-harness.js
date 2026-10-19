const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const { FakeDocument, FakeElement, FakeTimerQueue, createJQuery } = require('./browser-fakes.js');

function parseRequestData(data) {
    if (typeof data === 'string') {
        return new URLSearchParams(data);
    }
    if (data instanceof URLSearchParams) {
        return new URLSearchParams(data);
    }
    const params = new URLSearchParams();
    Object.entries(data || {}).forEach(([name, value]) => {
        params.set(name, value == null ? '' : String(value));
    });
    return params;
}

function createAjaxRequest(options) {
    const params = parseRequestData(options.data);
    const request = {
        aborted: false,
        completed: false,
        data: options.data,
        options,
        params
    };

    request.jqXHR = {
        abort() {
            if (request.completed || request.aborted) {
                return;
            }
            request.aborted = true;
            if (typeof options.error === 'function') {
                options.error(request.jqXHR, 'abort');
            }
            if (typeof options.complete === 'function') {
                options.complete(request.jqXHR, 'abort');
            }
        }
    };

    request.resolve = (payload) => {
        if (request.completed || request.aborted) {
            return;
        }
        request.completed = true;
        if (typeof options.success === 'function') {
            options.success(payload, 'success', request.jqXHR);
        }
        if (typeof options.complete === 'function') {
            options.complete(request.jqXHR, 'success');
        }
    };

    request.reject = (textStatus, errorThrown) => {
        if (request.completed || request.aborted) {
            return;
        }
        request.completed = true;
        if (typeof options.error === 'function') {
            options.error(request.jqXHR, textStatus, errorThrown);
        }
        if (typeof options.complete === 'function') {
            options.complete(request.jqXHR, textStatus);
        }
    };

    request.status = (code) => {
        if (request.completed || request.aborted) {
            return;
        }
        const handler = options.statusCode && options.statusCode[code];
        if (typeof handler === 'function') {
            handler();
        }
        request.completed = true;
        if (typeof options.complete === 'function') {
            options.complete(request.jqXHR, 'error');
        }
    };

    return request;
}

function createScriptHarness(options = {}) {
    const timers = new FakeTimerQueue();
    const document = new FakeDocument(options.href);
    const loadCallbacks = [];
    const ajaxRequests = [];
    const alerts = [];
    const confirms = [];
    const confirmResponses = Array.from(options.confirmResponses || []);
    const generatedRequestIds = Array.from(options.serverRequestIds || []);
    let generatedRequestIndex = 0;

    function registerElement(tagName, elementOptions) {
        return document.register(new FakeElement(document, tagName, elementOptions));
    }

    function getNextConfirmResponse() {
        if (!confirmResponses.length) {
            return true;
        }
        return confirmResponses.shift();
    }

    function defaultAjaxHandler(ajaxOptions) {
        const request = createAjaxRequest(ajaxOptions);
        ajaxRequests.push(request);
        return request.jqXHR;
    }

    const ajaxHandler = options.ajaxHandler || defaultAjaxHandler;
    const $ = createJQuery(document, (ajaxOptions) => ajaxHandler(ajaxOptions, { ajaxRequests, createAjaxRequest }));

    const workbench = Object.assign({
        addLoad(callback) {
            loadCallbacks.push(callback);
        },
        addParam(sb, id) {
            sb[sb.length] = id + '=';
            const tag = document.getElementById(id);
            sb[sb.length] = tag.type === 'checkbox' ? String(tag.checked) : encodeURIComponent(tag.value);
            sb[sb.length] = '&';
        },
        getCookie(name) {
            const cookies = document.cookie.split(';');
            for (const cookie of cookies) {
                const eq = cookie.indexOf('=');
                if (name === cookie.substring(0, eq).trim()) {
                    return decodeURIComponent(cookie.substring(eq + 1).replace(/\+/g, '%20'));
                }
            }
            return '';
        },
        getQueryStringElements() {
            const href = document.location.href;
            const query = href.includes('?') ? href.substring(href.indexOf('?') + 1) : '';
            return query ? query.split('&') : [''];
        },
        paging: {},
        queryCancelPolicy: undefined,
        yasqeHelper: {
            setupCompleters() {
            }
        }
    }, options.workbench || {});

    const window = Object.assign({
        clearTimeout: (id) => timers.clearTimeout(id),
        confirm(message) {
            confirms.push(message);
            return getNextConfirmResponse();
        },
        atob(value) {
            return Buffer.from(String(value), 'base64').toString('utf8');
        },
        btoa(value) {
            return Buffer.from(String(value), 'utf8').toString('base64');
        },
        crypto: {
            randomUUID() {
                if (generatedRequestIndex < generatedRequestIds.length) {
                    const requestId = generatedRequestIds[generatedRequestIndex];
                    generatedRequestIndex += 1;
                    return requestId;
                }
                generatedRequestIndex += 1;
                return `request-${generatedRequestIndex}`;
            }
        },
        document,
        location: document.location,
        localStorage: {
            removeItem() {
            }
        },
        msCrypto: undefined,
        requestAnimationFrame(callback) {
            callback();
        },
        scrollBy() {
        },
        sessionStorage: {
            removeItem() {
            }
        },
        setTimeout: (callback, delay) => timers.setTimeout(callback, delay)
    }, options.window || {});
    window.window = window;

    const context = vm.createContext(Object.assign({
        $,
        Blob: function Blob() {
        },
        Date: class extends Date {
            static now() {
                return timers.now;
            }
        },
        URL,
        Viz: undefined,
        YASQE: {
            defaults: {},
            Autocompleters: {
                prefixes: {
                    appendPrefixIfNeeded() {
                    },
                    isValidCompletionPosition() {
                        return true;
                    },
                    preprocessPrefixTokenForCompletion(yasqe, token) {
                        return token;
                    }
                }
            },
            fromTextArea() {
                return null;
            },
            registerAutocompleter() {
            },
            storeQuery() {
            }
        },
        alert(message) {
            alerts.push(message);
        },
        confirm(message) {
            return window.confirm(message);
        },
        console,
        document,
        jQuery: $,
        setTimeout: window.setTimeout,
        clearTimeout: window.clearTimeout,
        window,
        workbench
    }, options.globals || {}));
    context.globalThis = context;
    context.self = window;
    context.window.URL = context.URL;
    context.window.atob = context.window.atob || context.atob;
    context.window.btoa = context.window.btoa || context.btoa;

    function runScript(relativePath) {
        const absolutePath = path.resolve(__dirname, '..', '..', relativePath);
        const source = fs.readFileSync(absolutePath, 'utf8');
        vm.runInContext(source, context, { filename: absolutePath });
    }

    function runLoadHandlers() {
        loadCallbacks.splice(0).forEach((callback) => callback());
        if (typeof window.onload === 'function') {
            window.onload();
        }
    }

    function requireElement(id) {
        const element = document.getElementById(id);
        if (!element) {
            throw new Error(`Unknown element: ${id}`);
        }
        return element;
    }

    return {
        $, ajaxRequests, alerts, confirms, context, document, registerElement, runScript, runLoadHandlers, timers, window, workbench,
        advanceTimers(ms) {
            timers.advance(ms);
        },
        click(id) {
            requireElement(id).click();
        },
        getAttribute(id, name) {
            return requireElement(id).getAttribute(name);
        },
        getHtml(id) {
            return requireElement(id).innerHTML;
        },
        getProperty(id, name) {
            return requireElement(id)[name];
        },
        getText(id) {
            return requireElement(id).textContent;
        },
        hasClass(id, className) {
            return requireElement(id).classList.contains(className);
        },
        setChecked(id, value) {
            const element = requireElement(id);
            element.checked = !!value;
            element.trigger('change');
        },
        setValue(id, value) {
            const element = requireElement(id);
            element.value = value;
            element.trigger('change');
        }
    };
}

module.exports = {
    createAjaxRequest,
    createScriptHarness,
    parseRequestData
};
