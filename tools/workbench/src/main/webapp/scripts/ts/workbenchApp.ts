/// <reference path="workbenchViews.ts" />
/// <reference path="workbenchRoutes.ts" />
/// <reference path="workbenchRouter.ts" />
/// <reference lib="es2015.promise" />

// WARNING: Do not edit the generated workbenchApp.js file. Edit this source
// and run the Workbench TypeScript compiler instead.

// Browser bootstrap for structured Workbench page models. Route-changing POST
// forms remain browser-owned; only eligible GET shells fetch page data here.
module workbench {
    export interface PageModel {
        viewId: string;
        vars: string[];
        /** The current bounded window only; complete rows remain in rowStore. */
        rows: any[][];
        rowStart: number;
        rowCount: number;
        rowTopSpacer: number;
        rowBottomSpacer: number;
        pickerRows: any[][];
        pickerStart: number;
        pickerPageSize: number;
        rowStore: any;
        namespaceMap: { [prefix: string]: string };
        links: string[];
        metadata: { [key: string]: any };
        boolean?: boolean;
        linked?: { [key: string]: any };
        workbench?: any;
        /** Set when the page model ends in an error record after its view, for example repository-not-found. */
        error?: { status: number; code: string; message: string };
        /** The URL the page model was answered from, after redirects (M8.1). */
        finalUrl?: string;
    }

    export interface LitRuntime {
        html: (strings: TemplateStringsArray, ...values: any[]) => any;
        render: (template: any, root: Element) => void;
        nothing?: any;
    }

    export module app {
        export const ACCEPT = 'application/vnd.rdf4j.workbench+ndjson';
        const scriptPromiseKey = '__rdf4jWorkbenchScriptPromises';
        const litPromiseKey = '__rdf4jWorkbenchLitPromise';
        const loadRoutineKey = '__rdf4jWorkbenchLoadRoutineRun';

        function invalid(message: string): Error {
            return new Error('Invalid Workbench page model: ' + message);
        }

        function isObject(value: any): boolean {
            return value !== null && typeof value === 'object' && !Array.isArray(value);
        }

        function mergeValues(target: { [key: string]: any }, source: any): void {
            if (!isObject(source)) {
                throw invalid('metadata values must be an object');
            }
            Object.keys(source).forEach((key) => {
                target[key] = source[key];
            });
        }

        function namespaceMap(value: any): { [prefix: string]: string } {
            const result: { [prefix: string]: string } = {};
            if (Array.isArray(value)) {
                value.forEach((entry: any) => {
                    if (isObject(entry) && typeof entry.prefix === 'string' && typeof entry.name === 'string') {
                        result[entry.prefix + ':'] = entry.name;
                    }
                });
            } else if (isObject(value)) {
                Object.keys(value).forEach((prefix) => {
                    result[prefix.charAt(prefix.length - 1) === ':' ? prefix : prefix + ':'] = String(value[prefix]);
                });
            }
            return result;
        }

        function termValue(value: any): string {
            if (typeof value === 'string') {
                return value;
            }
            return isObject(value) && typeof value.value === 'string' ? value.value : '';
        }

        function attribute(mount: any, name: string): string {
            if (mount && typeof mount.getAttribute === 'function') {
                return mount.getAttribute(name) || '';
            }
            if (mount && mount.dataset) {
                const dataName = name.replace(/^data-/, '').replace(/-([a-z])/g,
                    (_match: string, letter: string) => letter.toUpperCase());
                return mount.dataset[dataName] || '';
            }
            return '';
        }

        function decodeBase64UrlUtf8(encoded: string, label: string): string {
            if (!/^[A-Za-z0-9_-]+$/.test(encoded) || encoded.length % 4 === 1) {
                throw invalid(label + ' is not base64url');
            }
            const targetWindow: any = typeof window !== 'undefined' ? window : null;
            const decode = targetWindow && typeof targetWindow.atob === 'function'
                ? targetWindow.atob.bind(targetWindow) : null;
            if (!decode) {
                throw invalid(label + ' cannot be decoded in this browser');
            }
            let base64 = encoded.replace(/-/g, '+').replace(/_/g, '/');
            while (base64.length % 4) { base64 += '='; }
            let decodedText: string;
            try {
                const binary = decode(base64);
                let escaped = '';
                for (let index = 0; index < binary.length; index++) {
                    const hex = binary.charCodeAt(index).toString(16);
                    escaped += '%' + (hex.length === 1 ? '0' : '') + hex;
                }
                decodedText = decodeURIComponent(escaped);
            } catch (error) {
                throw invalid(label + ' is not valid UTF-8 base64url data');
            }
            return decodedText;
        }

        function decodeInitialPost(encoded: string): any {
            const json = decodeBase64UrlUtf8(encoded, 'initial POST descriptor');
            let parameters: any;
            try {
                parameters = JSON.parse(json);
            } catch (error) {
                throw invalid('initial POST descriptor is not valid JSON');
            }
            if (!isObject(parameters)) {
                throw invalid('initial POST descriptor must be a parameter object');
            }
            const names = Object.keys(parameters);
            for (let index = 0; index < names.length; index++) {
                const name = names[index];
                const value = parameters[name];
                if (!name || (typeof value !== 'string' && (!Array.isArray(value)
                        || value.some((item: any) => typeof item !== 'string')))) {
                    throw invalid('initial POST parameters must contain string values');
                }
            }
            const actions = Array.isArray(parameters.action) ? parameters.action : [parameters.action];
            if (actions.length !== 1 || actions[0] !== 'exec') {
                throw invalid('initial POST action must be exec');
            }
            const queries = Array.isArray(parameters.query) ? parameters.query : [parameters.query];
            if (!queries.length || typeof queries[0] !== 'string' || !queries[0].trim()) {
                throw invalid('initial query execution must contain a query');
            }
            return parameters;
        }

        function decodeInitialModel(encoded: string): any {
            const payload = decodeBase64UrlUtf8(encoded, 'initial page model');
            const targetWindow: any = typeof window !== 'undefined' ? window : null;
            const ResponseConstructor = targetWindow && targetWindow.Response;
            if (typeof ResponseConstructor !== 'function') {
                throw invalid('initial page model requires the Fetch Response API');
            }
            return new ResponseConstructor(payload, {
                status: 200,
                headers: { 'Content-Type': ACCEPT + '; charset=UTF-8' }
            });
        }

        function consumeInitialPost(mount: any, viewId: string): any {
            const encoded = attribute(mount, 'data-workbench-initial-post');
            if (!encoded) {
                return null;
            }
            if (mount && typeof mount.removeAttribute === 'function') {
                mount.removeAttribute('data-workbench-initial-post');
            } else if (mount && typeof mount.setAttribute === 'function') {
                mount.setAttribute('data-workbench-initial-post', '');
                mount.setAttribute('data-workbench-initial-post-consumed', 'true');
            }
            if (viewId !== 'query') {
                throw invalid('initial POST descriptor is only valid for the query view');
            }
            return decodeInitialPost(encoded);
        }

        function consumeInitialModel(mount: any): string | null {
            const encoded = attribute(mount, 'data-workbench-initial-model');
            if (!encoded) {
                return null;
            }
            // Remove the POST response payload before decoding so repeated
            // bootstrap calls cannot replay its page model or request.
            if (mount && typeof mount.removeAttribute === 'function') {
                mount.removeAttribute('data-workbench-initial-model');
            } else if (mount && typeof mount.setAttribute === 'function') {
                mount.setAttribute('data-workbench-initial-model', '');
            }
            if (mount && typeof mount.setAttribute === 'function') {
                mount.setAttribute('data-workbench-initial-model-consumed', 'true');
            }
            return encoded;
        }

        function formElements(form: any): any[] {
            const elements = form && form.elements;
            const result: any[] = [];
            for (let index = 0; elements && index < elements.length; index++) {
                result.push(elements[index]);
            }
            return result;
        }

        function appendHiddenParameter(form: any, document: any, name: string, value: string): void {
            if (!document || !document.createElement || !form || !form.appendChild) {
                throw invalid('query form cannot preserve initial POST parameters');
            }
            const input = document.createElement('input');
            input.type = 'hidden';
            input.name = name;
            input.value = value;
            form.appendChild(input);
        }

        /** Stage the original form parameter map for one controller-owned submit. */
        export function stageInitialQueryParameters(form: any, document: any, parameters: any): () => void {
            if (!form) {
                throw invalid('initial query execution has no query form');
            }
            const controls = formElements(form);
            const saved: any[] = controls.map((control: any) => ({
                control,
                value: control.value,
                checked: control.checked,
                disabled: control.disabled
            }));
            const names = Object.keys(parameters);
            controls.forEach((control: any) => {
                if (!control || !control.name || control.name === 'query-request-id'
                        || Object.prototype.hasOwnProperty.call(parameters, control.name)) {
                    return;
                }
                if (control.type === 'checkbox' || control.type === 'radio') {
                    control.checked = false;
                }
                control.disabled = true;
            });
            names.forEach((name) => {
                const rawValues = parameters[name];
                const values: string[] = Array.isArray(rawValues) ? rawValues : [rawValues];
                const matches = controls.filter((control: any) => control && control.name === name);
                if (!matches.length) {
                    values.forEach((value) => appendHiddenParameter(form, document, name, value));
                    return;
                }
                const type = String(matches[0].type || '').toLowerCase();
                if (type === 'checkbox' || type === 'radio') {
                    matches.forEach((control: any) => {
                        control.disabled = false;
                        control.checked = values.indexOf(String(control.value || '')) >= 0;
                    });
                    values.forEach((value) => {
                        if (!matches.some((control: any) => String(control.value || '') === value)) {
                            appendHiddenParameter(form, document, name, value);
                        }
                    });
                    return;
                }
                if (String(matches[0].tagName || '').toLowerCase() === 'select' && matches[0].multiple) {
                    const select = matches[0];
                    select.disabled = false;
                    const options = select.options || [];
                    const selectedValues: string[] = [];
                    for (let optionIndex = 0; optionIndex < options.length; optionIndex++) {
                        const option = options[optionIndex];
                        option.selected = values.indexOf(String(option.value)) >= 0;
                        if (option.selected) { selectedValues.push(String(option.value)); }
                    }
                    values.forEach((value) => {
                        if (selectedValues.indexOf(value) < 0) {
                            appendHiddenParameter(form, document, name, value);
                        }
                    });
                    return;
                }
                values.forEach((value, valueIndex) => {
                    if (valueIndex < matches.length) {
                        matches[valueIndex].value = value;
                        matches[valueIndex].disabled = false;
                    } else {
                        appendHiddenParameter(form, document, name, value);
                    }
                });
                for (let matchIndex = values.length; matchIndex < matches.length; matchIndex++) {
                    matches[matchIndex].disabled = true;
                }
            });
            return () => {
                saved.forEach((entry: any) => {
                    entry.control.disabled = entry.disabled;
                    if (entry.control.type === 'checkbox' || entry.control.type === 'radio') {
                        entry.control.checked = entry.checked;
                    }
                });
            };
        }

        function basePathFor(mount: any): string {
            return attribute(mount, 'data-workbench-base-path').replace(/\/+$/, '');
        }

        function currentUrlFor(mount: any, dependencies: any): string {
            if (dependencies && dependencies.currentUrl) {
                return String(dependencies.currentUrl);
            }
            const document = mount && mount.ownerDocument;
            const location = document && document.location ? document.location
                : (typeof window !== 'undefined' ? window.location : null);
            if (!location || !location.href) {
                throw new Error('Workbench page shell has no current URL');
            }
            return String(location.href);
        }

        function fetchFunction(dependencies: any): (url: string, options: any) => Promise<any> {
            if (dependencies && dependencies.fetch) {
                return dependencies.fetch;
            }
            if (typeof window !== 'undefined' && window.fetch) {
                return window.fetch.bind(window);
            }
            throw new Error('Fetch is unavailable in this browser');
        }

        function parseLinkedPath(value: string): string {
            try {
                return decodeURIComponent(value);
            } catch (error) {
                throw invalid('link path is not correctly encoded');
            }
        }

        function linkedUrl(currentUrl: string, path: string): string {
            const url = new URL(parseLinkedPath(path), currentUrl);
            if (url.origin !== new URL(currentUrl).origin) {
                throw invalid('linked model must remain on the current origin');
            }
            return url.toString();
        }

        function queryStream(): any {
            const stream = (workbench as any).queryStream;
            if (!stream || typeof stream.consumeNdjsonResponse !== 'function'
                    || typeof stream.createRowStore !== 'function') {
                throw new Error('The shared Workbench stream runtime is unavailable');
            }
            return stream;
        }

        function newPageModel(rowStore: any): PageModel {
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
                rowStore,
                namespaceMap: {},
                links: [],
                metadata: {},
                linked: {}
            };
        }

        function acceptPageRecord(model: PageModel, state: any, record: any): Promise<void> {
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
                            || record.values.some((value: any) => typeof value !== 'string')) {
                        throw invalid('vars must contain one string array');
                    }
                    model.vars = record.values.slice();
                    state.haveVars = true;
                    return Promise.resolve();
                case 'namespaces':
                    if (!Array.isArray(record.values)
                            || record.values.some((namespace: any) => !isObject(namespace)
                                || typeof namespace.prefix !== 'string' || typeof namespace.name !== 'string')) {
                        throw invalid('namespaces must contain prefix/name pairs');
                    }
                    record.values.forEach((namespace: any) => {
                        model.namespaceMap[namespace.prefix + ':'] = namespace.name;
                    });
                    model.metadata.namespaceMap = model.namespaceMap;
                    return Promise.resolve();
                case 'links':
                    if (!Array.isArray(record.values)
                            || record.values.some((value: any) => typeof value !== 'string')) {
                        throw invalid('links must contain a string array');
                    }
                    record.values.forEach((value: string) => model.links.push(value));
                    return Promise.resolve();
                case 'rows':
                    if (!Array.isArray(record.values)
                            || record.values.some((row: any) => !Array.isArray(row)
                                || (state.haveVars && row.length !== model.vars.length))) {
                        throw invalid('rows must contain positional arrays matching vars');
                    }
                    state.rowsSeen = true;
                    if (model.viewId === '_internal/namespaces' || model.viewId === 'namespace-metadata') {
                        const prefixIndex = model.vars.indexOf('prefix');
                        const nameIndex = model.vars.indexOf('namespace') >= 0
                            ? model.vars.indexOf('namespace') : model.vars.indexOf('name');
                        if (prefixIndex >= 0 && nameIndex >= 0) {
                            record.values.forEach((row: any[]) => {
                                const prefixTerm = row[prefixIndex];
                                const prefix = termValue(prefixTerm);
                                const name = termValue(row[nameIndex]);
                                if (prefixTerm !== null && typeof prefixTerm !== 'undefined' && name) {
                                    model.namespaceMap[prefix.charAt(prefix.length - 1) === ':'
                                        ? prefix : prefix + ':'] = name;
                                }
                            });
                            model.metadata.namespaceMap = model.namespaceMap;
                        }
                        return Promise.resolve();
                    }
                    return model.rowStore.append(record.values).then(() => {});
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
                    if (state.haveView) {
                        model.error = {
                            status: typeof record.status === 'number' ? record.status : 0,
                            code: typeof record.code === 'string' ? record.code : '',
                            message: state.error.message
                        };
                    }
                    return Promise.resolve();
                default:
                    throw invalid('unsupported event type ' + record.type);
            }
        }

        /** True when an error response still carries a page model (NDJSON page protocol body). */
        function isPageProtocolResponse(response: any): boolean {
            const headers = response && response.headers;
            const type = headers && typeof headers.get === 'function' ? String(headers.get('content-type') || '') : '';
            return type.toLowerCase().indexOf(ACCEPT) === 0;
        }

        /** The error a navigation that was given up (an aborted signal) rejects with. */
        function abortError(): Error {
            const error = new Error('The Workbench page request was abandoned.');
            error.name = 'AbortError';
            return error;
        }

        /**
         * Load the page model at url (NDJSON page protocol) into a new worker-backed row store. With a signal
         * the request can be abandoned (it then rejects with an AbortError); model.finalUrl is where the answer
         * came from after redirects.
         */
        export function loadModel(fetcher: (url: string, options: any) => Promise<any>, url: string,
                                  signal?: AbortSignal): Promise<PageModel> {
            const request: any = { headers: { Accept: ACCEPT }, credentials: 'same-origin' };
            if (signal) {
                request.signal = signal;
            }
            return fetcher(url, request).then((response: any) => loadModelFromResponse(response, signal, url));
        }

        /**
         * Read a page model from a response that is already there, for example the answer to a form POST the
         * router sent (M10.1). requestedUrl stands in for response.url when a fake response has none.
         */
        export function loadModelFromResponse(response: any, signal?: AbortSignal,
                                              requestedUrl?: string): Promise<PageModel> {
            if (!response || response.ok === false && !isPageProtocolResponse(response)) {
                const status = response && response.status ? ' (' + response.status + ')' : '';
                return Promise.reject(new Error('Unable to load Workbench page data' + status));
            }
            const stream = queryStream();
            return stream.createRowStore().then((rowStore: any) => {
                const model = newPageModel(rowStore);
                const state: { haveView: boolean; haveVars: boolean; haveTerminal: boolean;
                    rowsSeen: boolean; error: Error | null } = {
                    haveView: false, haveVars: false, haveTerminal: false, rowsSeen: false, error: null
                };
                model.finalUrl = response.url || requestedUrl;
                return stream.consumeNdjsonResponse(response, {
                    signal,
                    // A page model with an error status still describes the page to show (M10.1).
                    allowErrorStatus: true,
                    onRecord: (record: any) => acceptPageRecord(model, state, record)
                }).then((outcome: any) => {
                    if (outcome && outcome.type === 'stale') {
                        throw abortError();
                    }
                    if (model.error) {
                        // An error after the view is part of the page (for example an unknown repository).
                        return rowStore.count().then((rowCount: number) => {
                            model.rowCount = rowCount;
                            model.workbench = model.metadata.workbench || {};
                            return model;
                        });
                    }
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
                    return rowStore.count().then((rowCount: number) => {
                        model.rowCount = rowCount;
                        model.workbench = model.metadata.workbench || {};
                        return model;
                    });
                }).then(null, (error: any) => rowStore.dispose().then(() => { throw error; }));
            });
        }

        function requestedLinkedModels(model: PageModel): string[] {
            const allowed: { [key: string]: boolean } = {
                'info': true,
                '_internal/namespaces': true
            };
            const selected: string[] = [];
            model.links.forEach((link) => {
                const path = parseLinkedPath(link);
                if (allowed[path] && selected.indexOf(path) < 0) {
                    selected.push(path);
                }
            });
            return selected;
        }

        /** A URL beside which "info" resolves to the server-level Info model (repositories/NONE/info). */
        function notFoundInfoUrl(basePath: string, currentUrl: string): string {
            return new URL(basePath + '/repositories/NONE/', currentUrl).toString();
        }

        /** Load the linked models named by the model (or the given paths) relative to currentUrl. */
        export function linkedModels(fetcher: (url: string, options: any) => Promise<any>, currentUrl: string,
                                     model: PageModel, explicitPaths?: string[], signal?: AbortSignal): Promise<void> {
            const paths = explicitPaths || requestedLinkedModels(model);
            return Promise.all(paths.map((path) => loadModel(fetcher, linkedUrl(currentUrl, path), signal)
                .then((linked) => {
                    if (linked.error) {
                        return linked.rowStore.dispose().then(() => { throw new Error(linked.error.message); });
                    }
                    if (path === '_internal/namespaces') {
                        model.linked.namespaces = { namespaceMap: linked.namespaceMap };
                        return linked.rowStore.dispose();
                    }
                    return prepareInitialRows(linked).then(() => {
                        const workbenchInfo = linked.metadata.workbench
                            || (workbench.views && typeof workbench.views.linkedInfoMetadata === 'function'
                                ? workbench.views.linkedInfoMetadata(linked) : {});
                        model.workbench = workbenchInfo;
                        model.linked.info = { metadata: linked.metadata, workbench: workbenchInfo };
                        return linked.rowStore.dispose();
                    });
                }))).then(() => {
                const info = model.linked.info;
                if (info) {
                    model.workbench = info.workbench || info.metadata.workbench || {};
                }
            });
        }

        /**
         * Load what a page needs before it is shown: its linked models and the rows it renders directly. A
         * repository that does not exist takes its shell from the server-level Info model; any other error page
         * is returned as it is.
         */
        export function completeModel(fetcher: (url: string, options: any) => Promise<any>, url: string,
                                      model: PageModel, basePath: string, signal?: AbortSignal): Promise<PageModel> {
            if (model.error) {
                if (model.error.code === 'repository-not-found') {
                    return linkedModels(fetcher, notFoundInfoUrl(basePath, url), model, ['info'], signal).then(() => model);
                }
                // The shell around an error still shows the menu, from the repository's information (M13.6); without
                // it the error is shown all the same.
                return linkedModels(fetcher, url, model, ['info'], signal).then(() => model, () => model);
            }
            return linkedModels(fetcher, url, model, undefined, signal)
                .then(() => prepareInitialRows(model))
                .then(() => model);
        }

        function scriptUrl(basePath: string, name: string): string {
            return basePath + '/scripts/' + name;
        }

        function loadClassicScript(url: string, dependencies: any): Promise<void> {
            if (dependencies && dependencies.skipScripts) {
                return Promise.resolve();
            }
            const targetWindow: any = typeof window !== 'undefined' ? window : null;
            const document = targetWindow && targetWindow.document;
            if (!document || !document.createElement) {
                return Promise.resolve();
            }
            const existing = document.querySelector('script[src="' + url.replace(/"/g, '%22') + '"]');
            if (existing && existing.getAttribute('data-workbench-loaded') === 'true') {
                return Promise.resolve();
            }
            if (!targetWindow[scriptPromiseKey]) {
                targetWindow[scriptPromiseKey] = {};
            }
            const promises = targetWindow[scriptPromiseKey];
            if (promises[url]) {
                return promises[url];
            }
            promises[url] = new Promise<void>((resolve, reject) => {
                // An unmarked node may have already emitted an error before deferred bootstrap starts.
                // Only a successful marker or our tracked promise proves that its load is reusable.
                const script = document.createElement('script');
                script.src = url;
                script.async = false;
                script.onload = () => {
                    script.setAttribute('data-workbench-loaded', 'true');
                    resolve();
                };
                script.onerror = () => reject(new Error('Unable to load Workbench script ' + url));
                (document.head || document.body).appendChild(script);
            });
            return promises[url];
        }

        function loadSharedRuntime(basePath: string, dependencies: any): Promise<void> {
            let sequence: Promise<void> = Promise.resolve();
            ['workbenchViews.js', 'workbenchRoutes.js', 'workbenchRouter.js', 'queryStream.js', 'workbench-theme.js']
                .forEach((name) => {
                sequence = sequence.then(() => loadClassicScript(scriptUrl(basePath, name), dependencies));
            });
            return sequence.then(() => {
                if (!workbench.views || typeof workbench.views.render !== 'function') {
                    throw new Error('The Workbench route renderer is unavailable');
                }
                if (!workbench.routes || typeof workbench.routes.get !== 'function') {
                    throw new Error('The Workbench route registry is unavailable');
                }
                queryStream();
            });
        }

        /** Read the rows a view renders directly (form fields, summary rows, Info rows) into model.rows. */
        export function prepareInitialRows(model: PageModel): Promise<void> {
            let count = 0;
            if (model.viewId === 'create' || model.viewId === 'add') {
                // These rows describe form fields and select choices, not a data result table.
                count = model.rowCount;
            } else if (model.viewId === 'namespaces') {
                // The namespace list is small and filtered in the page (M6.3).
                count = model.rowCount;
            } else if (model.viewId === 'clear' || model.viewId === 'remove' || model.viewId === 'update') {
                // A rejected request answers with an error-message row, and Clear and Remove list graphs (M6.1).
                count = model.rowCount;
            } else if (model.viewId === 'summary' || model.viewId === 'information' || model.viewId === 'server'
                    || model.viewId === 'query') {
                // The Query page has a row only when it opens a saved query for editing (M10.2).
                count = Math.min(1, model.rowCount);
            } else if (model.viewId === 'info' && !model.metadata.workbench) {
                // The linked Info endpoint's small fixed rows describe shell settings,
                // policy-visible navigation, and format capabilities rather than a data table.
                count = model.rowCount;
            }
            if (!count) {
                return Promise.resolve();
            }
            return model.rowStore.read(0, count).then((rows: any[][]) => {
                model.rows = rows;
                model.rowStart = 0;
            });
        }

        function configureTheme(mount: any, workbenchInfo: any): void {
            const targetWindow: any = typeof window !== 'undefined' ? window : null;
            const theme = targetWindow && targetWindow.RDF4JWorkbenchTheme;
            if (!theme) {
                return;
            }
            const defaults = workbenchInfo.defaults || workbenchInfo;
            const configuredDefault = defaults['default-workbench-theme'] || defaults.defaultTheme || 'system';
            const defaultValue = configuredDefault && configuredDefault.kind
                ? configuredDefault.value : configuredDefault;
            if (typeof theme.configure === 'function') {
                theme.configure(String(defaultValue));
            }
            const document = mount && mount.ownerDocument;
            const control = document && document.getElementById
                ? document.getElementById('workbench-theme') : null;
            if (typeof theme.connectControl === 'function') {
                theme.connectControl(control);
            }
        }

        export function configureNamespaces(model: PageModel): void {
            const targetWindow: any = typeof window !== 'undefined' ? window : null;
            if (!targetWindow) {
                return;
            }
            const linked = model.linked && model.linked.namespaces;
            const source = linked && linked.namespaceMap ? linked.namespaceMap : model.namespaceMap;
            const mappings = namespaceMap(source || {});
            targetWindow.sparqlNamespaces = mappings;
            targetWindow.namespaces = mappings;
        }

        const scrollPositionStoragePrefix = 'rdf4j.workbench.scroll-position.v1:';

        function scrollPositionStorage(targetWindow: any): any {
            try {
                return targetWindow && targetWindow.sessionStorage;
            } catch (error) {
                return null;
            }
        }

        function scrollPositionKey(targetWindow: any): string | null {
            const navigation = targetWindow && targetWindow.navigation;
            const entry = navigation && navigation.currentEntry;
            const entryKey = entry && entry.key;
            if (typeof entryKey === 'string' && entryKey) {
                return scrollPositionStoragePrefix + 'entry:' + encodeURIComponent(entryKey);
            }
            const document = targetWindow && targetWindow.document;
            const location = document && document.location ? document.location : targetWindow && targetWindow.location;
            return location && location.href
                ? scrollPositionStoragePrefix + 'url:' + encodeURIComponent(String(location.href)) : null;
        }

        function isHistoryTraversal(targetWindow: any): boolean {
            const performanceObject = targetWindow && targetWindow.performance;
            if (!performanceObject) {
                return false;
            }
            if (typeof performanceObject.getEntriesByType === 'function') {
                try {
                    const entries = performanceObject.getEntriesByType('navigation');
                    if (entries && entries.length > 0 && entries[0] && typeof entries[0].type === 'string') {
                        return entries[0].type === 'back_forward';
                    }
                } catch (error) {
                    // Use the legacy navigation timing entry when the modern API is unavailable.
                }
            }
            return !!(performanceObject.navigation && performanceObject.navigation.type === 2);
        }

        function savedScrollPosition(targetWindow: any): { key: string; y: number } | null {
            if (!isHistoryTraversal(targetWindow)) {
                return null;
            }
            const storage = scrollPositionStorage(targetWindow);
            const key = scrollPositionKey(targetWindow);
            if (!storage || typeof storage.getItem !== 'function' || !key) {
                return null;
            }
            try {
                const value = storage.getItem(key);
                if (value === null) {
                    return null;
                }
                const y = Number(value);
                return isFinite(y) && y >= 0 ? { key, y } : null;
            } catch (error) {
                return null;
            }
        }

        function saveScrollPosition(targetWindow: any, event: any): void {
            if (!event || event.persisted !== false) {
                return;
            }
            const storage = scrollPositionStorage(targetWindow);
            const key = scrollPositionKey(targetWindow);
            if (!storage || typeof storage.setItem !== 'function' || !key) {
                return;
            }
            const scrollY = Number(typeof targetWindow.scrollY === 'number'
                ? targetWindow.scrollY : targetWindow.pageYOffset);
            if (!isFinite(scrollY) || scrollY < 0) {
                return;
            }
            try {
                storage.setItem(key, String(scrollY));
            } catch (error) {
                // Native history restoration remains available if session storage is disabled.
            }
        }

        function restoreScrollPosition(targetWindow: any, saved: { key: string; y: number } | null): void {
            if (!saved) {
                return;
            }
            const storage = scrollPositionStorage(targetWindow);
            if (typeof targetWindow.scrollTo !== 'function') {
                return;
            }
            targetWindow.scrollTo(0, saved.y);
            // The page can still grow for a frame or two after its rows are bound, which clamps the first scroll short
            // of a position near its end; apply it again once the page has its final height, as the router does,
            // unless the page was scrolled in the meantime.
            const clamped = targetWindow.scrollY;
            if (clamped < saved.y && typeof targetWindow.requestAnimationFrame === 'function') {
                targetWindow.requestAnimationFrame(() => targetWindow.requestAnimationFrame(() => {
                    if (targetWindow.scrollY === clamped) {
                        targetWindow.scrollTo(0, saved.y);
                    }
                }));
            }
            if (storage && typeof storage.removeItem === 'function') {
                try {
                    storage.removeItem(saved.key);
                } catch (error) {
                    // The restored value is harmless and will be overwritten on the next pagehide.
                }
            }
        }

        /** Dispose the mounted route when the page is left, but not when the browser keeps it in its back/forward cache. */
        function disposeOnPagehide(instance: routes.RouteInstance): void {
            const targetWindow: any = typeof window !== 'undefined' ? window : null;
            if (!targetWindow || !targetWindow.addEventListener) {
                return;
            }
            let released = false;
            const release = (event: any) => {
                if (event && event.persisted === true) {
                    return;
                }
                if (released) {
                    return;
                }
                released = true;
                saveScrollPosition(targetWindow, event);
                queryStream().markCurrentRowStoresForRecovery(event);
                if (targetWindow.removeEventListener) {
                    targetWindow.removeEventListener('pagehide', release, false);
                }
                instance.dispose('pagehide');
            };
            targetWindow.addEventListener('pagehide', release, false);
        }

        function runtimeFor(mount: any, dependencies: any): Promise<LitRuntime> {
            if (dependencies && dependencies.runtime) {
                return Promise.resolve(dependencies.runtime);
            }
            const targetWindow: any = typeof window !== 'undefined' ? window : null;
            if (!targetWindow) {
                return Promise.reject(new Error('Workbench Lit runtime is unavailable'));
            }
            if (targetWindow.RDF4JLitHTML) {
                return Promise.resolve(targetWindow.RDF4JLitHTML);
            }
            if (targetWindow[litPromiseKey]) {
                return targetWindow[litPromiseKey];
            }
            const document = mount && mount.ownerDocument ? mount.ownerDocument : targetWindow.document;
            const bridge = scriptUrl(basePathFor(mount), 'workbench-lit-html.mjs');
            targetWindow[litPromiseKey] = new Promise<LitRuntime>((resolve, reject) => {
                const script = document.createElement('script');
                script.type = 'module';
                script.src = bridge;
                script.onload = () => targetWindow.RDF4JLitHTML
                    ? resolve(targetWindow.RDF4JLitHTML)
                    : reject(new Error('Local Lit bridge loaded without exposing its runtime'));
                script.onerror = () => reject(new Error('Unable to load local Lit runtime'));
                (document.head || document.body).appendChild(script);
            });
            return targetWindow[litPromiseKey];
        }

        function installLegacyHelpers(basePath: string, dependencies: any): Promise<void> {
            const windowObject: any = typeof window !== 'undefined' ? window : null;
            const promises: Promise<void>[] = [];
            if (windowObject && typeof windowObject.workbench !== 'undefined'
                    && typeof windowObject.workbench.addLoad !== 'function') {
                // The global namespace may exist before its compatibility helpers
                // when a static page shell loaded only the new renderer bundle.
                promises.push(loadClassicScript(scriptUrl(basePath, 'template.js'), dependencies));
            } else if (windowObject && !windowObject.workbench) {
                promises.push(loadClassicScript(scriptUrl(basePath, 'template.js'), dependencies));
            }
            if (windowObject && !windowObject.jQuery) {
                promises.push(loadClassicScript(scriptUrl(basePath, 'jquery-1.11.0.min.js'), dependencies));
            }
            return Promise.all(promises).then((): void => {});
        }

        /** Load, in order, the scripts a view needs for its model; the lists live in the route definitions (M7.1). */
        function installRouteRuntime(basePath: string, viewId: string, model: PageModel,
                                     dependencies: any): Promise<void> {
            const definition = routes.get(viewId);
            let sequence = Promise.resolve();
            (definition ? definition.scripts(model) : []).forEach((name) => {
                sequence = sequence.then(() => loadClassicScript(scriptUrl(basePath, name), dependencies));
            });
            return sequence;
        }

        function prepareLegacyLoadBarrier(mount: any): () => void {
            const windowObject: any = typeof window !== 'undefined' ? window : null;
            if (!windowObject || windowObject[loadRoutineKey]) {
                return () => {};
            }
            const document = mount && mount.ownerDocument ? mount.ownerDocument : windowObject.document;
            const original = windowObject.onload;
            let ready = false;
            let loadSeen = !!(document && document.readyState === 'complete');
            let invoked = false;
            const invokeOriginal = (event?: any) => {
                if (invoked) {
                    return;
                }
                invoked = true;
                windowObject[loadRoutineKey] = true;
                if (typeof original === 'function') {
                    original.call(windowObject, event);
                }
            };
            windowObject.onload = (event?: any) => {
                loadSeen = true;
                if (ready) {
                    invokeOriginal(event);
                }
            };
            return () => {
                ready = true;
                if (document && document.readyState === 'complete') {
                    loadSeen = true;
                }
                if (loadSeen) {
                    invokeOriginal();
                }
            };
        }

        /** The repository id segment of a Workbench URL (/repositories/<id>/<view>). */
        function repositoryIdFromUrl(url: string): string {
            const match = /\/repositories\/([^\/?#]+)/.exec(new URL(url).pathname);
            return match ? decodeURIComponent(match[1]) : '';
        }

        /** The view context of a page: where the Workbench lives, which repository it shows and its models. */
        export function viewContext(mount: any, model: PageModel, url: string, runtime: LitRuntime,
                                    repositoryId?: string): any {
            const notFound = !!model.error && model.error.code === 'repository-not-found';
            return {
                basePath: basePathFor(mount),
                repositoryId: notFound ? '' : (repositoryId === undefined ? repositoryIdFromUrl(url) : repositoryId),
                missingRepositoryId: notFound ? repositoryIdFromUrl(url) : undefined,
                workbench: model.workbench || {},
                linked: model.linked,
                pageModel: model,
                runtime,
                executionFormId: 'query-form',
                resultsMountId: 'query-results'
            };
        }

        /** Where bootstrap found the Workbench scripts, so routes can load theirs later (loadScripts). */
        let scriptSource: { basePath: string; dependencies: any } = { basePath: '', dependencies: null };

        /** Load Workbench scripts in order; scripts already loaded are not loaded again. */
        export function loadScripts(names: string[]): Promise<void> {
            let sequence = Promise.resolve();
            names.forEach((name) => {
                sequence = sequence.then(() => loadClassicScript(scriptUrl(scriptSource.basePath, name),
                    scriptSource.dependencies));
            });
            return sequence;
        }

        /** Show a failure as an error callout in the outlet when the shell exists, otherwise in the mount. */
        export function renderFailure(mount: any, error: any): void {
            const views: any = workbench.views;
            const outlet = views && typeof views.outletOf === 'function' ? views.outletOf(mount) : null;
            if (outlet) {
                mount = outlet;
            }
            const document = mount && mount.ownerDocument;
            if (!document || !document.createElement) {
                return;
            }
            const text = error && error.message ? error.message : String(error);
            const createCallout = (workbench as any).createCallout;
            let message: any;
            if (typeof createCallout === 'function') {
                message = createCallout(document, 'error', text, 'Unable to load this Workbench page.');
            } else {
                message = document.createElement('p');
                message.className = 'error';
                message.setAttribute('role', 'alert');
                message.textContent = 'Unable to load this Workbench page: ' + text;
            }
            while (mount.firstChild) {
                mount.removeChild(mount.firstChild);
            }
            mount.appendChild(message);
        }

        /** Fetch, combine, and render an eligible HTML shell exactly once. */
        function bootstrapAfterRecovery(mount: any, dependencies: any, basePath: string): Promise<any> {
            if (attribute(mount, 'data-workbench-fetch-page-model') !== 'true') {
                return Promise.resolve({ status: 'skipped' });
            }
            if (attribute(mount, 'data-workbench-initial-model-consumed') === 'true') {
                return Promise.resolve({ status: 'skipped' });
            }
            const targetWindow: any = typeof window !== 'undefined' ? window : null;
            const scrollToRestore = savedScrollPosition(targetWindow);
            const viewId = attribute(mount, 'data-workbench-view');
            if (!viewId) {
                return Promise.reject(new Error('Workbench page shell is missing its view id'));
            }
            let initialPost: any = null;
            let initialModel: any = null;
            try {
                initialPost = consumeInitialPost(mount, viewId);
                const encodedInitialModel = consumeInitialModel(mount);
                if (initialPost && encodedInitialModel) {
                    throw invalid('query execution and initial page model descriptors cannot be combined');
                }
                initialModel = encodedInitialModel ? decodeInitialModel(encodedInitialModel) : null;
            } catch (error) {
                renderFailure(mount, error);
                return Promise.reject(error);
            }
            const currentUrl = currentUrlFor(mount, dependencies);
            const fetcher = fetchFunction(dependencies);
            return (initialModel
                ? loadModel(() => Promise.resolve(initialModel), currentUrl)
                : loadModel(fetcher, currentUrl))
                .then((model) => {
                    if (model.viewId !== viewId) {
                        throw invalid('shell view ' + viewId + ' does not match data view ' + model.viewId);
                    }
                    // An error answer is shown inside the shell, as the router shows it (M8.1, M13.6).
                    return completeModel(fetcher, currentUrl, model, basePath).then(() => {
                        if (!model.error) {
                            configureNamespaces(model);
                        }
                        return model;
                    });
                }).then((model) => Promise.all([
                    runtimeFor(mount, dependencies),
                    installLegacyHelpers(basePath, dependencies)
                ]).then((loaded) => ({ model, runtime: loaded[0] as LitRuntime }))).then((state) => {
                const document = mount && mount.ownerDocument;
                if (document && document.body && document.body.classList) {
                    document.body.classList.add('workbench-body');
                }
                const context = viewContext(mount, state.model, currentUrl, state.runtime,
                    attribute(mount, 'data-workbench-repository-id'));
                const rendered = workbench.views.render(mount, state.model, context, state.runtime);
                const contextBar = workbench.views.bindContextBar
                    ? workbench.views.bindContextBar(mount, context) : undefined;
                configureTheme(mount, context.workbench);
                if (document && document.getElementById && document.getElementById('noscript-message')) {
                    document.getElementById('noscript-message').style.display = 'none';
                }
                const outlet = workbench.views.outletOf ? workbench.views.outletOf(mount) || mount : mount;
                const definition = routes.get(viewId);
                const routeContext: routes.RouteMountContext = {
                    outlet,
                    model: state.model,
                    context,
                    runtime: state.runtime,
                    url: new URL(currentUrl),
                    // A query posted to the Query page runs once the route is mounted (M9.1).
                    state: { rendered: true, initialPost }
                };
                // A router-ready route mounts its scripts itself, so they load first; the other routes start
                // their scripts in the legacy load handlers, after their rows are bound (M7.2). An error answer has
                // no page to mount: like the router's, its instance only releases the row store.
                const failed = !!state.model.error;
                const scriptsFirst = !failed && !!definition && definition.routerReady;
                const loadRouteScripts = () => failed ? Promise.resolve()
                    : installRouteRuntime(basePath, viewId, state.model, dependencies);
                let mounted: routes.RouteInstance = null;
                return (scriptsFirst ? loadRouteScripts() : Promise.resolve())
                    .then((): routes.RouteInstance | Promise<routes.RouteInstance> => failed
                        ? { dispose: () => { state.model.rowStore.dispose(); } }
                        : definition ? definition.mount(routeContext) : routes.defaultMount(routeContext))
                    .then((instance) => Promise.resolve(instance.ready).then(() => {
                        mounted = instance;
                        disposeOnPagehide(instance);
                        return scriptsFirst ? undefined : loadRouteScripts();
                    }))
                    .then(() => {
                        const runLegacyLoadHandlers = prepareLegacyLoadBarrier(mount);
                        runLegacyLoadHandlers();
                        restoreScrollPosition(targetWindow, scrollToRestore);
                        // ?router=off starts the Workbench without in-page navigation (debugging, M8.1).
                        const router: any = (workbench as any).router;
                        if (router && new URL(currentUrl).searchParams.get('router') !== 'off') {
                            router.start({ mount, runtime: state.runtime, basePath, fetcher, url: currentUrl,
                                model: state.model, instance: mounted, contextBar });
                        }
                        return { status: 'rendered', model: state.model, rendered };
                    });
            }).catch((error) => {
                renderFailure(mount, error);
                throw error;
            });
        }

        export function bootstrap(mount: any, dependencies?: any): Promise<any> {
            const basePath = basePathFor(mount);
            scriptSource = { basePath, dependencies };
            return loadSharedRuntime(basePath, dependencies)
                .then(() => queryStream().recoverPendingRowStores())
                .then(() => bootstrapAfterRecovery(mount, dependencies, basePath), (error: any) => {
                    if (attribute(mount, 'data-workbench-fetch-page-model') === 'true') {
                        renderFailure(mount, error);
                    }
                    throw error;
                });
        }

        function startFromDocument(): void {
            const document = typeof window !== 'undefined' ? window.document : null;
            const mount = document && document.getElementById('workbench-app');
            if (!mount || attribute(mount, 'data-workbench-bootstrap-started') === 'true') {
                return;
            }
            mount.setAttribute('data-workbench-bootstrap-started', 'true');
            bootstrap(mount).catch((error) => {
                if (typeof console !== 'undefined' && console.error) {
                    console.error(error);
                }
            });
        }

        if (typeof window !== 'undefined' && window.document) {
            if (window.document.readyState === 'loading') {
                window.document.addEventListener('DOMContentLoaded', startFromDocument, { once: true });
            } else {
                startFromDocument();
            }
        }
    }
}
