/// <reference path="workbenchViews.ts" />
/// <reference lib="es2015.promise" />
// WARNING: Do not edit the generated workbenchApp.js file. Edit this source
// and run the Workbench TypeScript compiler instead.
// Browser bootstrap for structured Workbench page models. Route-changing POST
// forms remain browser-owned; only eligible GET shells fetch page data here.
var workbench;
(function (workbench) {
    var app;
    (function (app) {
        app.ACCEPT = 'application/vnd.rdf4j.workbench+ndjson';
        var scriptPromiseKey = '__rdf4jWorkbenchScriptPromises';
        var litPromiseKey = '__rdf4jWorkbenchLitPromise';
        var loadRoutineKey = '__rdf4jWorkbenchLoadRoutineRun';
        function invalid(message) {
            return new Error('Invalid Workbench page model: ' + message);
        }
        function isObject(value) {
            return value !== null && typeof value === 'object' && !Array.isArray(value);
        }
        function mergeValues(target, source) {
            if (!isObject(source)) {
                throw invalid('metadata values must be an object');
            }
            Object.keys(source).forEach(function (key) {
                target[key] = source[key];
            });
        }
        function namespaceMap(value) {
            var result = {};
            if (Array.isArray(value)) {
                value.forEach(function (entry) {
                    if (isObject(entry) && typeof entry.prefix === 'string' && typeof entry.name === 'string') {
                        result[entry.prefix + ':'] = entry.name;
                    }
                });
            }
            else if (isObject(value)) {
                Object.keys(value).forEach(function (prefix) {
                    result[prefix.charAt(prefix.length - 1) === ':' ? prefix : prefix + ':'] = String(value[prefix]);
                });
            }
            return result;
        }
        function termValue(value) {
            if (typeof value === 'string') {
                return value;
            }
            return isObject(value) && typeof value.value === 'string' ? value.value : '';
        }
        function attribute(mount, name) {
            if (mount && typeof mount.getAttribute === 'function') {
                return mount.getAttribute(name) || '';
            }
            if (mount && mount.dataset) {
                var dataName = name.replace(/^data-/, '').replace(/-([a-z])/g, function (_match, letter) { return letter.toUpperCase(); });
                return mount.dataset[dataName] || '';
            }
            return '';
        }
        function decodeBase64UrlUtf8(encoded, label) {
            if (!/^[A-Za-z0-9_-]+$/.test(encoded) || encoded.length % 4 === 1) {
                throw invalid(label + ' is not base64url');
            }
            var targetWindow = typeof window !== 'undefined' ? window : null;
            var decode = targetWindow && typeof targetWindow.atob === 'function'
                ? targetWindow.atob.bind(targetWindow) : null;
            if (!decode) {
                throw invalid(label + ' cannot be decoded in this browser');
            }
            var base64 = encoded.replace(/-/g, '+').replace(/_/g, '/');
            while (base64.length % 4) {
                base64 += '=';
            }
            var decodedText;
            try {
                var binary = decode(base64);
                var escaped = '';
                for (var index = 0; index < binary.length; index++) {
                    var hex = binary.charCodeAt(index).toString(16);
                    escaped += '%' + (hex.length === 1 ? '0' : '') + hex;
                }
                decodedText = decodeURIComponent(escaped);
            }
            catch (error) {
                throw invalid(label + ' is not valid UTF-8 base64url data');
            }
            return decodedText;
        }
        function decodeInitialPost(encoded) {
            var json = decodeBase64UrlUtf8(encoded, 'initial POST descriptor');
            var parameters;
            try {
                parameters = JSON.parse(json);
            }
            catch (error) {
                throw invalid('initial POST descriptor is not valid JSON');
            }
            if (!isObject(parameters)) {
                throw invalid('initial POST descriptor must be a parameter object');
            }
            var names = Object.keys(parameters);
            for (var index = 0; index < names.length; index++) {
                var name_1 = names[index];
                var value = parameters[name_1];
                if (!name_1 || (typeof value !== 'string' && (!Array.isArray(value)
                    || value.some(function (item) { return typeof item !== 'string'; })))) {
                    throw invalid('initial POST parameters must contain string values');
                }
            }
            var actions = Array.isArray(parameters.action) ? parameters.action : [parameters.action];
            if (actions.length !== 1 || actions[0] !== 'exec') {
                throw invalid('initial POST action must be exec');
            }
            var queries = Array.isArray(parameters.query) ? parameters.query : [parameters.query];
            if (!queries.length || typeof queries[0] !== 'string' || !queries[0].trim()) {
                throw invalid('initial query execution must contain a query');
            }
            return parameters;
        }
        function decodeInitialModel(encoded) {
            var payload = decodeBase64UrlUtf8(encoded, 'initial page model');
            var targetWindow = typeof window !== 'undefined' ? window : null;
            var ResponseConstructor = targetWindow && targetWindow.Response;
            if (typeof ResponseConstructor !== 'function') {
                throw invalid('initial page model requires the Fetch Response API');
            }
            return new ResponseConstructor(payload, {
                status: 200,
                headers: { 'Content-Type': app.ACCEPT + '; charset=UTF-8' }
            });
        }
        function consumeInitialPost(mount, viewId) {
            var encoded = attribute(mount, 'data-workbench-initial-post');
            if (!encoded) {
                return null;
            }
            if (mount && typeof mount.removeAttribute === 'function') {
                mount.removeAttribute('data-workbench-initial-post');
            }
            else if (mount && typeof mount.setAttribute === 'function') {
                mount.setAttribute('data-workbench-initial-post', '');
                mount.setAttribute('data-workbench-initial-post-consumed', 'true');
            }
            if (viewId !== 'query') {
                throw invalid('initial POST descriptor is only valid for the query view');
            }
            return decodeInitialPost(encoded);
        }
        function consumeInitialModel(mount) {
            var encoded = attribute(mount, 'data-workbench-initial-model');
            if (!encoded) {
                return null;
            }
            // Remove the POST response payload before decoding so repeated
            // bootstrap calls cannot replay its page model or request.
            if (mount && typeof mount.removeAttribute === 'function') {
                mount.removeAttribute('data-workbench-initial-model');
            }
            else if (mount && typeof mount.setAttribute === 'function') {
                mount.setAttribute('data-workbench-initial-model', '');
            }
            if (mount && typeof mount.setAttribute === 'function') {
                mount.setAttribute('data-workbench-initial-model-consumed', 'true');
            }
            return encoded;
        }
        function formElements(form) {
            var elements = form && form.elements;
            var result = [];
            for (var index = 0; elements && index < elements.length; index++) {
                result.push(elements[index]);
            }
            return result;
        }
        function appendHiddenParameter(form, document, name, value) {
            if (!document || !document.createElement || !form || !form.appendChild) {
                throw invalid('query form cannot preserve initial POST parameters');
            }
            var input = document.createElement('input');
            input.type = 'hidden';
            input.name = name;
            input.value = value;
            form.appendChild(input);
        }
        /** Stage the original form parameter map for one controller-owned submit. */
        function stageInitialQueryParameters(form, document, parameters) {
            if (!form) {
                throw invalid('initial query execution has no query form');
            }
            var controls = formElements(form);
            var saved = controls.map(function (control) { return ({
                control: control,
                value: control.value,
                checked: control.checked,
                disabled: control.disabled
            }); });
            var names = Object.keys(parameters);
            controls.forEach(function (control) {
                if (!control || !control.name || control.name === 'query-request-id'
                    || Object.prototype.hasOwnProperty.call(parameters, control.name)) {
                    return;
                }
                if (control.type === 'checkbox' || control.type === 'radio') {
                    control.checked = false;
                }
                control.disabled = true;
            });
            names.forEach(function (name) {
                var rawValues = parameters[name];
                var values = Array.isArray(rawValues) ? rawValues : [rawValues];
                var matches = controls.filter(function (control) { return control && control.name === name; });
                if (!matches.length) {
                    values.forEach(function (value) { return appendHiddenParameter(form, document, name, value); });
                    return;
                }
                var type = String(matches[0].type || '').toLowerCase();
                if (type === 'checkbox' || type === 'radio') {
                    matches.forEach(function (control) {
                        control.disabled = false;
                        control.checked = values.indexOf(String(control.value || '')) >= 0;
                    });
                    values.forEach(function (value) {
                        if (!matches.some(function (control) { return String(control.value || '') === value; })) {
                            appendHiddenParameter(form, document, name, value);
                        }
                    });
                    return;
                }
                if (String(matches[0].tagName || '').toLowerCase() === 'select' && matches[0].multiple) {
                    var select = matches[0];
                    select.disabled = false;
                    var options = select.options || [];
                    var selectedValues_1 = [];
                    for (var optionIndex = 0; optionIndex < options.length; optionIndex++) {
                        var option = options[optionIndex];
                        option.selected = values.indexOf(String(option.value)) >= 0;
                        if (option.selected) {
                            selectedValues_1.push(String(option.value));
                        }
                    }
                    values.forEach(function (value) {
                        if (selectedValues_1.indexOf(value) < 0) {
                            appendHiddenParameter(form, document, name, value);
                        }
                    });
                    return;
                }
                values.forEach(function (value, valueIndex) {
                    if (valueIndex < matches.length) {
                        matches[valueIndex].value = value;
                        matches[valueIndex].disabled = false;
                    }
                    else {
                        appendHiddenParameter(form, document, name, value);
                    }
                });
                for (var matchIndex = values.length; matchIndex < matches.length; matchIndex++) {
                    matches[matchIndex].disabled = true;
                }
            });
            return function () {
                saved.forEach(function (entry) {
                    entry.control.disabled = entry.disabled;
                    if (entry.control.type === 'checkbox' || entry.control.type === 'radio') {
                        entry.control.checked = entry.checked;
                    }
                });
            };
        }
        function basePathFor(mount) {
            return attribute(mount, 'data-workbench-base-path').replace(/\/+$/, '');
        }
        function currentUrlFor(mount, dependencies) {
            if (dependencies && dependencies.currentUrl) {
                return String(dependencies.currentUrl);
            }
            var document = mount && mount.ownerDocument;
            var location = document && document.location ? document.location
                : (typeof window !== 'undefined' ? window.location : null);
            if (!location || !location.href) {
                throw new Error('Workbench page shell has no current URL');
            }
            return String(location.href);
        }
        function fetchFunction(dependencies) {
            if (dependencies && dependencies.fetch) {
                return dependencies.fetch;
            }
            if (typeof window !== 'undefined' && window.fetch) {
                return window.fetch.bind(window);
            }
            throw new Error('Fetch is unavailable in this browser');
        }
        function parseLinkedPath(value) {
            try {
                return decodeURIComponent(value);
            }
            catch (error) {
                throw invalid('link path is not correctly encoded');
            }
        }
        function linkedUrl(currentUrl, path) {
            var url = new URL(parseLinkedPath(path), currentUrl);
            if (url.origin !== new URL(currentUrl).origin) {
                throw invalid('linked model must remain on the current origin');
            }
            return url.toString();
        }
        function queryStream() {
            var stream = workbench.queryStream;
            if (!stream || typeof stream.consumeNdjsonResponse !== 'function'
                || typeof stream.createRowStore !== 'function') {
                throw new Error('The shared Workbench stream runtime is unavailable');
            }
            return stream;
        }
        function newPageModel(rowStore) {
            return {
                viewId: '',
                vars: [],
                rows: [],
                rowStart: 0,
                rowCount: 0,
                rowTopSpacer: 0,
                rowBottomSpacer: 0,
                pickerRows: [],
                pickerStart: 0,
                pickerPageSize: 50,
                rowStore: rowStore,
                namespaceMap: {},
                links: [],
                metadata: {},
                linked: {}
            };
        }
        function acceptPageRecord(model, state, record) {
            if (!isObject(record) || typeof record.type !== 'string') {
                throw invalid('each record must have a type');
            }
            switch (record.type) {
                case 'head':
                    return Promise.resolve();
                case 'view':
                    if (state.haveView || typeof record.id !== 'string' || !record.id) {
                        throw invalid('view must contain one stable id');
                    }
                    model.viewId = record.id;
                    state.haveView = true;
                    return Promise.resolve();
                case 'metadata':
                    mergeValues(model.metadata, record.values);
                    if (Object.prototype.hasOwnProperty.call(record.values, 'namespaceMap')) {
                        model.namespaceMap = namespaceMap(record.values.namespaceMap);
                        model.metadata.namespaceMap = model.namespaceMap;
                    }
                    return Promise.resolve();
                case 'vars':
                    if (state.haveVars || !Array.isArray(record.values)
                        || record.values.some(function (value) { return typeof value !== 'string'; })) {
                        throw invalid('vars must contain one string array');
                    }
                    model.vars = record.values.slice();
                    state.haveVars = true;
                    return Promise.resolve();
                case 'namespaces':
                    if (!Array.isArray(record.values)
                        || record.values.some(function (namespace) { return !isObject(namespace)
                            || typeof namespace.prefix !== 'string' || typeof namespace.name !== 'string'; })) {
                        throw invalid('namespaces must contain prefix/name pairs');
                    }
                    record.values.forEach(function (namespace) {
                        model.namespaceMap[namespace.prefix + ':'] = namespace.name;
                    });
                    model.metadata.namespaceMap = model.namespaceMap;
                    return Promise.resolve();
                case 'links':
                    if (!Array.isArray(record.values)
                        || record.values.some(function (value) { return typeof value !== 'string'; })) {
                        throw invalid('links must contain a string array');
                    }
                    record.values.forEach(function (value) { return model.links.push(value); });
                    return Promise.resolve();
                case 'rows':
                    if (!Array.isArray(record.values)
                        || record.values.some(function (row) { return !Array.isArray(row)
                            || (state.haveVars && row.length !== model.vars.length); })) {
                        throw invalid('rows must contain positional arrays matching vars');
                    }
                    state.rowsSeen = true;
                    if (model.viewId === '_internal/namespaces' || model.viewId === 'namespace-metadata') {
                        var prefixIndex_1 = model.vars.indexOf('prefix');
                        var nameIndex_1 = model.vars.indexOf('namespace') >= 0
                            ? model.vars.indexOf('namespace') : model.vars.indexOf('name');
                        if (prefixIndex_1 >= 0 && nameIndex_1 >= 0) {
                            record.values.forEach(function (row) {
                                var prefixTerm = row[prefixIndex_1];
                                var prefix = termValue(prefixTerm);
                                var name = termValue(row[nameIndex_1]);
                                if (prefixTerm !== null && typeof prefixTerm !== 'undefined' && name) {
                                    model.namespaceMap[prefix.charAt(prefix.length - 1) === ':'
                                        ? prefix : prefix + ':'] = name;
                                }
                            });
                            model.metadata.namespaceMap = model.namespaceMap;
                        }
                        return Promise.resolve();
                    }
                    return model.rowStore.append(record.values).then(function () { });
                case 'boolean':
                    if (typeof record.value !== 'boolean') {
                        throw invalid('boolean must contain a boolean value');
                    }
                    model.boolean = record.value;
                    return Promise.resolve();
                case 'end':
                    if (record.metadata !== undefined) {
                        mergeValues(model.metadata, record.metadata);
                    }
                    state.haveTerminal = true;
                    return Promise.resolve();
                case 'error':
                    state.error = new Error(record.message || ('Workbench page request failed'
                        + (record.status ? ' (' + record.status + ')' : '')));
                    return Promise.resolve();
                default:
                    throw invalid('unsupported event type ' + record.type);
            }
        }
        function loadModel(fetcher, url) {
            var stream = queryStream();
            return stream.createRowStore().then(function (rowStore) {
                var model = newPageModel(rowStore);
                var state = {
                    haveView: false, haveVars: false, haveTerminal: false, rowsSeen: false, error: null
                };
                return fetcher(url, {
                    headers: { Accept: app.ACCEPT },
                    credentials: 'same-origin'
                }).then(function (response) {
                    if (!response || response.ok === false) {
                        var status_1 = response && response.status ? ' (' + response.status + ')' : '';
                        throw new Error('Unable to load Workbench page data' + status_1);
                    }
                    return stream.consumeNdjsonResponse(response, {
                        onRecord: function (record) { return acceptPageRecord(model, state, record); }
                    });
                }).then(function (outcome) {
                    if (state.error) {
                        throw state.error;
                    }
                    if (outcome && outcome.type === 'error') {
                        throw new Error(outcome.message || ('Workbench page request failed'
                            + (outcome.status ? ' (' + outcome.status + ')' : '')));
                    }
                    if (!state.haveView || !state.haveTerminal) {
                        throw invalid('view and end are required');
                    }
                    return rowStore.count().then(function (rowCount) {
                        model.rowCount = rowCount;
                        model.workbench = model.metadata.workbench || {};
                        return model;
                    });
                }).then(null, function (error) { return rowStore.dispose().then(function () { throw error; }); });
            });
        }
        function requestedLinkedModels(model) {
            var allowed = {
                'info': true,
                '_internal/namespaces': true
            };
            var selected = [];
            model.links.forEach(function (link) {
                var path = parseLinkedPath(link);
                if (allowed[path] && selected.indexOf(path) < 0) {
                    selected.push(path);
                }
            });
            return selected;
        }
        function linkedModels(fetcher, currentUrl, model) {
            var paths = requestedLinkedModels(model);
            return Promise.all(paths.map(function (path) { return loadModel(fetcher, linkedUrl(currentUrl, path))
                .then(function (linked) {
                if (path === '_internal/namespaces') {
                    model.linked.namespaces = { namespaceMap: linked.namespaceMap };
                    return linked.rowStore.dispose();
                }
                return prepareInitialRows(linked).then(function () {
                    var workbenchInfo = linked.metadata.workbench
                        || (workbench.views && typeof workbench.views.linkedInfoMetadata === 'function'
                            ? workbench.views.linkedInfoMetadata(linked) : {});
                    model.workbench = workbenchInfo;
                    model.linked.info = { metadata: linked.metadata, workbench: workbenchInfo };
                    return linked.rowStore.dispose();
                });
            }); })).then(function () {
                var info = model.linked.info;
                if (info) {
                    model.workbench = info.workbench || info.metadata.workbench || {};
                }
            });
        }
        function scriptUrl(basePath, name) {
            return basePath + '/scripts/' + name;
        }
        function loadClassicScript(url, dependencies) {
            if (dependencies && dependencies.skipScripts) {
                return Promise.resolve();
            }
            var targetWindow = typeof window !== 'undefined' ? window : null;
            var document = targetWindow && targetWindow.document;
            if (!document || !document.createElement) {
                return Promise.resolve();
            }
            var existing = document.querySelector('script[src="' + url.replace(/"/g, '%22') + '"]');
            if (existing && existing.getAttribute('data-workbench-loaded') === 'true') {
                return Promise.resolve();
            }
            if (!targetWindow[scriptPromiseKey]) {
                targetWindow[scriptPromiseKey] = {};
            }
            var promises = targetWindow[scriptPromiseKey];
            if (promises[url]) {
                return promises[url];
            }
            promises[url] = new Promise(function (resolve, reject) {
                // An unmarked node may have already emitted an error before deferred bootstrap starts.
                // Only a successful marker or our tracked promise proves that its load is reusable.
                var script = document.createElement('script');
                script.src = url;
                script.async = false;
                script.onload = function () {
                    script.setAttribute('data-workbench-loaded', 'true');
                    resolve();
                };
                script.onerror = function () { return reject(new Error('Unable to load Workbench script ' + url)); };
                (document.head || document.body).appendChild(script);
            });
            return promises[url];
        }
        function loadSharedRuntime(basePath, dependencies) {
            var sequence = Promise.resolve();
            ['workbenchViews.js', 'queryStream.js', 'workbench-theme.js'].forEach(function (name) {
                sequence = sequence.then(function () { return loadClassicScript(scriptUrl(basePath, name), dependencies); });
            });
            return sequence.then(function () {
                if (!workbench.views || typeof workbench.views.render !== 'function') {
                    throw new Error('The Workbench route renderer is unavailable');
                }
                queryStream();
            });
        }
        function prepareInitialRows(model) {
            var count = 0;
            if (model.viewId === 'create' || model.viewId === 'add') {
                // These rows describe form fields and select choices, not a data result table.
                count = model.rowCount;
            }
            else if (model.viewId === 'summary' || model.viewId === 'information' || model.viewId === 'server') {
                count = Math.min(1, model.rowCount);
            }
            else if (model.viewId === 'info' && !model.metadata.workbench) {
                // The linked Info endpoint's small fixed rows describe shell settings,
                // policy-visible navigation, and format capabilities rather than a data table.
                count = model.rowCount;
            }
            if (!count) {
                return Promise.resolve();
            }
            return model.rowStore.read(0, count).then(function (rows) {
                model.rows = rows;
                model.rowStart = 0;
            });
        }
        function configureTheme(mount, workbenchInfo) {
            var targetWindow = typeof window !== 'undefined' ? window : null;
            var theme = targetWindow && targetWindow.RDF4JWorkbenchTheme;
            if (!theme) {
                return;
            }
            var defaults = workbenchInfo.defaults || workbenchInfo;
            var configuredDefault = defaults['default-workbench-theme'] || defaults.defaultTheme || 'system';
            var defaultValue = configuredDefault && configuredDefault.kind
                ? configuredDefault.value : configuredDefault;
            if (typeof theme.configure === 'function') {
                theme.configure(String(defaultValue));
            }
            var document = mount && mount.ownerDocument;
            var control = document && document.getElementById
                ? document.getElementById('workbench-theme') : null;
            if (typeof theme.connectControl === 'function') {
                theme.connectControl(control);
            }
        }
        function configureNamespaces(model) {
            var targetWindow = typeof window !== 'undefined' ? window : null;
            if (!targetWindow) {
                return;
            }
            var linked = model.linked && model.linked.namespaces;
            var source = linked && linked.namespaceMap ? linked.namespaceMap : model.namespaceMap;
            var mappings = namespaceMap(source || {});
            targetWindow.sparqlNamespaces = mappings;
            targetWindow.namespaces = mappings;
        }
        var scrollPositionStoragePrefix = 'rdf4j.workbench.scroll-position.v1:';
        function scrollPositionStorage(targetWindow) {
            try {
                return targetWindow && targetWindow.sessionStorage;
            }
            catch (error) {
                return null;
            }
        }
        function scrollPositionKey(targetWindow) {
            var navigation = targetWindow && targetWindow.navigation;
            var entry = navigation && navigation.currentEntry;
            var entryKey = entry && entry.key;
            if (typeof entryKey === 'string' && entryKey) {
                return scrollPositionStoragePrefix + 'entry:' + encodeURIComponent(entryKey);
            }
            var document = targetWindow && targetWindow.document;
            var location = document && document.location ? document.location : targetWindow && targetWindow.location;
            return location && location.href
                ? scrollPositionStoragePrefix + 'url:' + encodeURIComponent(String(location.href)) : null;
        }
        function isHistoryTraversal(targetWindow) {
            var performanceObject = targetWindow && targetWindow.performance;
            if (!performanceObject) {
                return false;
            }
            if (typeof performanceObject.getEntriesByType === 'function') {
                try {
                    var entries = performanceObject.getEntriesByType('navigation');
                    if (entries && entries.length > 0 && entries[0] && typeof entries[0].type === 'string') {
                        return entries[0].type === 'back_forward';
                    }
                }
                catch (error) {
                    // Use the legacy navigation timing entry when the modern API is unavailable.
                }
            }
            return !!(performanceObject.navigation && performanceObject.navigation.type === 2);
        }
        function savedScrollPosition(targetWindow) {
            if (!isHistoryTraversal(targetWindow)) {
                return null;
            }
            var storage = scrollPositionStorage(targetWindow);
            var key = scrollPositionKey(targetWindow);
            if (!storage || typeof storage.getItem !== 'function' || !key) {
                return null;
            }
            try {
                var value = storage.getItem(key);
                if (value === null) {
                    return null;
                }
                var y = Number(value);
                return isFinite(y) && y >= 0 ? { key: key, y: y } : null;
            }
            catch (error) {
                return null;
            }
        }
        function saveScrollPosition(targetWindow, event) {
            if (!event || event.persisted !== false) {
                return;
            }
            var storage = scrollPositionStorage(targetWindow);
            var key = scrollPositionKey(targetWindow);
            if (!storage || typeof storage.setItem !== 'function' || !key) {
                return;
            }
            var scrollY = Number(typeof targetWindow.scrollY === 'number'
                ? targetWindow.scrollY : targetWindow.pageYOffset);
            if (!isFinite(scrollY) || scrollY < 0) {
                return;
            }
            try {
                storage.setItem(key, String(scrollY));
            }
            catch (error) {
                // Native history restoration remains available if session storage is disabled.
            }
        }
        function restoreScrollPosition(targetWindow, saved) {
            if (!saved) {
                return;
            }
            var storage = scrollPositionStorage(targetWindow);
            if (typeof targetWindow.scrollTo !== 'function') {
                return;
            }
            targetWindow.scrollTo(0, saved.y);
            if (storage && typeof storage.removeItem === 'function') {
                try {
                    storage.removeItem(saved.key);
                }
                catch (error) {
                    // The restored value is harmless and will be overwritten on the next pagehide.
                }
            }
        }
        function releaseRowStore(model, disposer) {
            var targetWindow = typeof window !== 'undefined' ? window : null;
            if (!targetWindow || !targetWindow.addEventListener) {
                return;
            }
            var released = false;
            var release = function (event) {
                if (event && event.persisted === true) {
                    return;
                }
                if (released) {
                    return;
                }
                released = true;
                saveScrollPosition(targetWindow, event);
                queryStream().markCurrentRowStoresForRecovery(event);
                if (disposer) {
                    disposer();
                }
                if (model.rowStore) {
                    model.rowStore.dispose();
                }
                if (targetWindow.removeEventListener) {
                    targetWindow.removeEventListener('pagehide', release, false);
                }
            };
            targetWindow.addEventListener('pagehide', release, false);
        }
        function runtimeFor(mount, dependencies) {
            if (dependencies && dependencies.runtime) {
                return Promise.resolve(dependencies.runtime);
            }
            var targetWindow = typeof window !== 'undefined' ? window : null;
            if (!targetWindow) {
                return Promise.reject(new Error('Workbench Lit runtime is unavailable'));
            }
            if (targetWindow.RDF4JLitHTML) {
                return Promise.resolve(targetWindow.RDF4JLitHTML);
            }
            if (targetWindow[litPromiseKey]) {
                return targetWindow[litPromiseKey];
            }
            var document = mount && mount.ownerDocument ? mount.ownerDocument : targetWindow.document;
            var bridge = scriptUrl(basePathFor(mount), 'workbench-lit-html.mjs');
            targetWindow[litPromiseKey] = new Promise(function (resolve, reject) {
                var script = document.createElement('script');
                script.type = 'module';
                script.src = bridge;
                script.onload = function () { return targetWindow.RDF4JLitHTML
                    ? resolve(targetWindow.RDF4JLitHTML)
                    : reject(new Error('Local Lit bridge loaded without exposing its runtime')); };
                script.onerror = function () { return reject(new Error('Unable to load local Lit runtime')); };
                (document.head || document.body).appendChild(script);
            });
            return targetWindow[litPromiseKey];
        }
        function routeScripts(viewId, model) {
            switch (viewId) {
                case 'server': return ['server.js'];
                case 'create':
                    if (model.vars.indexOf('fieldId') >= 0) {
                        return ['create.js'];
                    }
                    if (model.vars.indexOf('location') >= 0
                        && model.vars.indexOf('description') >= 0
                        && model.vars.indexOf('id') >= 0) {
                        return ['create.js', 'create-federate.js'];
                    }
                    return [];
                case 'delete': return ['delete.js'];
                case 'namespaces': return ['namespaces.js'];
                case 'explore': return ['paging.js', 'explore.js'];
                case 'saved-queries':
                    return ['queryStream.js', 'codemirror.4.5.0.min.js', 'yasqe.min.js', 'saved-queries.js'];
                case 'export': return ['paging.js', 'export.js'];
                case 'add': return ['add.js'];
                case 'update':
                    return ['codemirror.4.5.0.min.js', 'yasqe.min.js', 'yasqeHelper.js', 'update.js'];
                case 'query':
                    return ['queryStream.js', 'codemirror.4.5.0.min.js', 'yasqe.min.js',
                        'yasqeHelper.js', 'queryCancelPolicy.js', 'diff.min.js', 'viz/viz.js',
                        'viz/full.render.js', 'svg-pan-zoom.min.js', 'queryExplanationHighlighter.js',
                        'paging.js', 'query.js'];
                default: return [];
            }
        }
        function installLegacyHelpers(basePath, dependencies) {
            var windowObject = typeof window !== 'undefined' ? window : null;
            var promises = [];
            if (windowObject && typeof windowObject.workbench !== 'undefined'
                && typeof windowObject.workbench.addLoad !== 'function') {
                // The global namespace may exist before its compatibility helpers
                // when a static page shell loaded only the new renderer bundle.
                promises.push(loadClassicScript(scriptUrl(basePath, 'template.js'), dependencies));
            }
            else if (windowObject && !windowObject.workbench) {
                promises.push(loadClassicScript(scriptUrl(basePath, 'template.js'), dependencies));
            }
            if (windowObject && !windowObject.jQuery) {
                promises.push(loadClassicScript(scriptUrl(basePath, 'jquery-1.11.0.min.js'), dependencies));
            }
            return Promise.all(promises).then(function () { });
        }
        function installRouteRuntime(basePath, viewId, model, dependencies) {
            var route = routeScripts(viewId, model);
            var sequence = Promise.resolve();
            route.forEach(function (name) {
                sequence = sequence.then(function () { return loadClassicScript(scriptUrl(basePath, name), dependencies); });
            });
            return sequence;
        }
        function prepareLegacyLoadBarrier(mount) {
            var windowObject = typeof window !== 'undefined' ? window : null;
            if (!windowObject || windowObject[loadRoutineKey]) {
                return function () { };
            }
            var document = mount && mount.ownerDocument ? mount.ownerDocument : windowObject.document;
            var original = windowObject.onload;
            var ready = false;
            var loadSeen = !!(document && document.readyState === 'complete');
            var invoked = false;
            var invokeOriginal = function (event) {
                if (invoked) {
                    return;
                }
                invoked = true;
                windowObject[loadRoutineKey] = true;
                if (typeof original === 'function') {
                    original.call(windowObject, event);
                }
            };
            windowObject.onload = function (event) {
                loadSeen = true;
                if (ready) {
                    invokeOriginal(event);
                }
            };
            return function () {
                ready = true;
                if (document && document.readyState === 'complete') {
                    loadSeen = true;
                }
                if (loadSeen) {
                    invokeOriginal();
                }
            };
        }
        function renderFailure(mount, error) {
            var document = mount && mount.ownerDocument;
            if (!document || !document.createElement) {
                return;
            }
            var message = document.createElement('p');
            message.className = 'error';
            message.setAttribute('role', 'alert');
            message.textContent = 'Unable to load this Workbench page: '
                + (error && error.message ? error.message : String(error));
            while (mount.firstChild) {
                mount.removeChild(mount.firstChild);
            }
            mount.appendChild(message);
        }
        /** Fetch, combine, and render an eligible HTML shell exactly once. */
        function bootstrapAfterRecovery(mount, dependencies, basePath) {
            if (attribute(mount, 'data-workbench-fetch-page-model') !== 'true') {
                return Promise.resolve({ status: 'skipped' });
            }
            if (attribute(mount, 'data-workbench-initial-model-consumed') === 'true') {
                return Promise.resolve({ status: 'skipped' });
            }
            var targetWindow = typeof window !== 'undefined' ? window : null;
            var scrollToRestore = savedScrollPosition(targetWindow);
            var viewId = attribute(mount, 'data-workbench-view');
            if (!viewId) {
                return Promise.reject(new Error('Workbench page shell is missing its view id'));
            }
            var initialPost = null;
            var initialModel = null;
            try {
                initialPost = consumeInitialPost(mount, viewId);
                var encodedInitialModel = consumeInitialModel(mount);
                if (initialPost && encodedInitialModel) {
                    throw invalid('query execution and initial page model descriptors cannot be combined');
                }
                initialModel = encodedInitialModel ? decodeInitialModel(encodedInitialModel) : null;
            }
            catch (error) {
                renderFailure(mount, error);
                return Promise.reject(error);
            }
            var currentUrl = currentUrlFor(mount, dependencies);
            var fetcher = fetchFunction(dependencies);
            return (initialModel
                ? loadModel(function () { return Promise.resolve(initialModel); }, currentUrl)
                : loadModel(fetcher, currentUrl))
                .then(function (model) {
                if (model.viewId !== viewId) {
                    throw invalid('shell view ' + viewId + ' does not match data view ' + model.viewId);
                }
                return linkedModels(fetcher, currentUrl, model).then(function () { return prepareInitialRows(model); }).then(function () {
                    configureNamespaces(model);
                    return model;
                });
            }).then(function (model) { return Promise.all([
                runtimeFor(mount, dependencies),
                installLegacyHelpers(basePath, dependencies)
            ]).then(function (loaded) { return ({ model: model, runtime: loaded[0] }); }); }).then(function (state) {
                var document = mount && mount.ownerDocument;
                if (document && document.body && document.body.classList) {
                    document.body.classList.add('workbench-body');
                }
                var context = {
                    basePath: basePathFor(mount),
                    repositoryId: attribute(mount, 'data-workbench-repository-id'),
                    workbench: state.model.workbench || {},
                    linked: state.model.linked,
                    pageModel: state.model,
                    runtime: state.runtime,
                    executionFormId: 'query-form',
                    resultsMountId: 'query-results'
                };
                var rendered = workbench.views.render(mount, state.model, context, state.runtime);
                configureTheme(mount, context.workbench);
                if (document && document.getElementById && document.getElementById('noscript-message')) {
                    document.getElementById('noscript-message').style.display = 'none';
                }
                var rowWindows = workbench.views.bindRowWindows
                    ? workbench.views.bindRowWindows(mount, state.model, context, state.runtime)
                    : Promise.resolve(null);
                return Promise.resolve(rowWindows).then(function (disposeRows) {
                    releaseRowStore(state.model, disposeRows);
                    return installRouteRuntime(basePath, viewId, state.model, dependencies).then(function () {
                        var runLegacyLoadHandlers = prepareLegacyLoadBarrier(mount);
                        var restoreInitialPost = null;
                        if (viewId === 'query') {
                            var target = mount.querySelector
                                ? mount.querySelector('#query-page-content') : mount;
                            var queryPage_1 = workbench.queryPage;
                            if (queryPage_1 && typeof queryPage_1.renderInto === 'function') {
                                queryPage_1.renderInto(target, state.model, context);
                                if (initialPost) {
                                    var form = document && document.getElementById
                                        ? document.getElementById(context.executionFormId) : null;
                                    restoreInitialPost = stageInitialQueryParameters(form, document, initialPost);
                                }
                            }
                            else if (target && document) {
                                var warning = document.createElement('p');
                                warning.className = 'error';
                                warning.setAttribute('role', 'alert');
                                warning.textContent = 'The query page renderer is unavailable.';
                                target.appendChild(warning);
                            }
                        }
                        runLegacyLoadHandlers();
                        if (initialPost) {
                            var queryPage_2 = workbench.queryPage;
                            if (!queryPage_2 || typeof queryPage_2.submitExecution !== 'function') {
                                if (restoreInitialPost) {
                                    restoreInitialPost();
                                }
                                throw new Error('The initial query execution controller is unavailable');
                            }
                            try {
                                queryPage_2.submitExecution();
                            }
                            finally {
                                if (restoreInitialPost) {
                                    restoreInitialPost();
                                }
                            }
                        }
                        restoreScrollPosition(targetWindow, scrollToRestore);
                        return { status: 'rendered', model: state.model, rendered: rendered };
                    });
                });
            }).catch(function (error) {
                renderFailure(mount, error);
                throw error;
            });
        }
        function bootstrap(mount, dependencies) {
            var basePath = basePathFor(mount);
            return loadSharedRuntime(basePath, dependencies)
                .then(function () { return queryStream().recoverPendingRowStores(); })
                .then(function () { return bootstrapAfterRecovery(mount, dependencies, basePath); }, function (error) {
                if (attribute(mount, 'data-workbench-fetch-page-model') === 'true') {
                    renderFailure(mount, error);
                }
                throw error;
            });
        }
        app.bootstrap = bootstrap;
        function startFromDocument() {
            var document = typeof window !== 'undefined' ? window.document : null;
            var mount = document && document.getElementById('workbench-app');
            if (!mount || attribute(mount, 'data-workbench-bootstrap-started') === 'true') {
                return;
            }
            mount.setAttribute('data-workbench-bootstrap-started', 'true');
            bootstrap(mount).catch(function (error) {
                if (typeof console !== 'undefined' && console.error) {
                    console.error(error);
                }
            });
        }
        if (typeof window !== 'undefined' && window.document) {
            if (window.document.readyState === 'loading') {
                window.document.addEventListener('DOMContentLoaded', startFromDocument, { once: true });
            }
            else {
                startFromDocument();
            }
        }
    })(app = workbench.app || (workbench.app = {}));
})(workbench || (workbench = {}));
//# sourceMappingURL=workbenchApp.js.map