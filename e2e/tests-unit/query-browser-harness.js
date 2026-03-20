const { createAjaxRequest, createScriptHarness, parseRequestData } = require('./script-harness.js');

function appendOption(registerElement, select, value, text, selected) {
    const option = registerElement('option', {
        value,
        textContent: text || value,
        selected: !!selected,
        attributes: { value }
    });
    select.appendChild(option);
    if (selected) {
        select.value = value;
    }
    return option;
}

function createYasqeStub(registerElement) {
    const state = {
        instances: {},
        storeQueryCalls: []
    };

    return {
        state,
        api: {
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
            fromTextArea(textarea, options) {
                const wrapper = registerElement('div', { className: 'yasqe-wrapper' });
                wrapper.appendChild(registerElement('div', { className: 'CodeMirror' }));
                wrapper.appendChild(registerElement('div', { className: 'CodeMirror-scroll' }));
                const instance = {
                    options,
                    textarea,
                    changeHandler: null,
                    closed: false,
                    refreshCount: 0,
                    saveCount: 0,
                    getValue() {
                        return textarea.value || '';
                    },
                    getWrapperElement() {
                        return wrapper;
                    },
                    on(eventName, handler) {
                        if (eventName === 'change') {
                            instance.changeHandler = handler;
                        }
                    },
                    refresh() {
                        instance.refreshCount += 1;
                    },
                    save() {
                        instance.saveCount += 1;
                    },
                    setValue(value) {
                        textarea.value = value;
                    },
                    toTextArea() {
                        instance.closed = true;
                    },
                    triggerChange() {
                        if (instance.changeHandler) {
                            instance.changeHandler();
                        }
                    }
                };
                state.instances[textarea.id] = instance;
                return instance;
            },
            registerAutocompleter() {
            },
            storeQuery(editor) {
                state.storeQueryCalls.push(editor);
            }
        }
    };
}

function createQueryBrowserHarness(options = {}) {
    const getJSONRequests = [];
    const pendingGetJSONRequests = [];
    const pendingExplainRequests = [];
    const getJSONResponses = Array.from(options.getJSONResponses || []);
    const harness = createScriptHarness(Object.assign({}, options, {
        ajaxHandler(ajaxOptions, helpers) {
            const request = createAjaxRequest(ajaxOptions);
            request.data = typeof ajaxOptions.data === 'string' ? ajaxOptions.data : '';
            request.params = parseRequestData(ajaxOptions.data);
            request.action = request.params.get('action');
            helpers.ajaxRequests.push(request);
            if (request.action === 'explain') {
                pendingExplainRequests.push(request);
                return request.jqXHR;
            }
            if (request.action === 'cancel-explain') {
                request.completed = true;
                if (typeof ajaxOptions.complete === 'function') {
                    ajaxOptions.complete(request.jqXHR, 'success');
                }
            }
            return request.jqXHR;
        }
    }));
    const { $, context, document, registerElement } = harness;
    const yasqe = createYasqeStub(registerElement);
    context.YASQE = yasqe.api;
    context.sparqlNamespaces = Object.assign({
        ex: 'http://example.com/'
    }, options.sparqlNamespaces || {});
    context.Blob = function Blob(parts, options) {
        this.parts = parts;
        this.options = options;
    };
    context.URL = {
        createObjectURL() {
            return 'blob:query-explanation';
        },
        revokeObjectURL() {
        }
    };
    context.window.URL = context.URL;

    $.getJSON = (url, data, callback) => {
        const request = {
            url,
            data,
            response: getJSONResponses.length ? getJSONResponses.shift() : {},
            callback
        };
        getJSONRequests.push(request);
        if (options.deferGetJSON) {
            pendingGetJSONRequests.push(request);
            return;
        }
        if (typeof callback === 'function') {
            callback(request.response);
        }
    };

    const navigation = registerElement('div', { id: 'navigation' });
    const titleHeading = registerElement('div', { id: 'title_heading', textContent: 'Query' });
    const noScriptMessage = registerElement('div', { id: 'noscript-message' });
    const selectedUser = registerElement('div', { id: 'selected-user' });
    const selectedUserSpan = registerElement('span', {
        className: options.noAuthenticatedUser ? 'disabled' : '',
        textContent: options.selectedUserName || 'alice'
    });
    selectedUser.appendChild(selectedUserSpan);
    const queryFormContainer = registerElement('div', { className: 'query-form' });
    document.body.appendChild(navigation);
    document.body.appendChild(titleHeading);
    document.body.appendChild(noScriptMessage);
    document.body.appendChild(selectedUser);
    document.body.appendChild(queryFormContainer);

    const form = registerElement('form', {
        attributes: {
            action: 'query'
        }
    });
    const actionInput = registerElement('input', { id: 'action', name: 'action', value: '' });
    const explainInput = registerElement('input', { id: 'explain', name: 'explain', value: '' });
    const explainLevel = registerElement('select', { id: 'explain-level', name: 'explain-level', value: 'Optimized' });
    appendOption(registerElement, explainLevel, 'Optimized', 'Optimized', true);
    appendOption(registerElement, explainLevel, 'Unoptimized');
    appendOption(registerElement, explainLevel, 'Executed');
    appendOption(registerElement, explainLevel, 'Timed');
    appendOption(registerElement, explainLevel, 'Telemetry');
    const explainFormat = registerElement('select', { id: 'explain-format', name: 'explain-format', value: 'text' });
    appendOption(registerElement, explainFormat, 'text', 'text', true);
    appendOption(registerElement, explainFormat, 'dot');
    appendOption(registerElement, explainFormat, 'json');
    const explainRequestIdInput = registerElement('input', { id: 'explain-request-id', name: 'explain-request-id', value: '' });
    const queryInput = registerElement('textarea', {
        id: 'query',
        name: 'query',
        value: Object.prototype.hasOwnProperty.call(options, 'query')
            ? options.query
            : 'SELECT * WHERE {?s ?p ?o}'
    });
    const compareQueryInput = registerElement('textarea', {
        id: 'query-compare',
        value: Object.prototype.hasOwnProperty.call(options, 'compareQuery') ? options.compareQuery : ''
    });
    const queryLn = registerElement('select', { id: 'queryLn', name: 'queryLn', value: options.queryLn || 'SPARQL' });
    appendOption(registerElement, queryLn, 'SPARQL', 'SPARQL', (options.queryLn || 'SPARQL') === 'SPARQL');
    appendOption(registerElement, queryLn, 'SERQL', 'SERQL', options.queryLn === 'SERQL');
    const limitQuery = registerElement('input', { id: 'limit_query', name: 'limit_query', value: options.limitQuery || '25' });
    const queryTimeout = registerElement('input', { id: 'query-timeout', name: 'query-timeout', value: options.queryTimeout || '0' });
    const infer = registerElement('input', { id: 'infer', name: 'infer', type: 'checkbox', checked: !!options.infer });
    const includeQueryText = registerElement('input', {
        id: 'include-query-text',
        name: 'include-query-text',
        value: 'false'
    });
    const queryName = registerElement('input', { id: 'query-name', name: 'query-name', value: options.queryName || '' });
    const savePrivate = registerElement('input', { id: 'save-private', name: 'save-private', type: 'checkbox', checked: true });
    form.formControls = [
        actionInput,
        explainInput,
        explainFormat,
        explainRequestIdInput,
        queryInput,
        queryLn,
        limitQuery,
        queryTimeout,
        infer,
        includeQueryText,
        queryName,
        savePrivate
    ];

    const sparqlNamespaces = registerElement('pre', {
        id: 'SPARQL-namespaces',
        textContent: options.sparqlNamespacesText || 'PREFIX ex: <http://example.com/>'
    });
    const serqlNamespaces = registerElement('pre', { id: 'SERQL-namespaces', textContent: 'using namespace ex = <http://example.com/>' });

    const exec = registerElement('button', { id: 'exec' });
    const save = registerElement('button', { id: 'save' });
    const saveFeedback = registerElement('div', { id: 'save-feedback' });

    const explainTrigger = registerElement('input', { id: 'explain-trigger', type: 'button' });
    const explainTriggerSpinner = registerElement('span', {
        id: 'explain-trigger-spinner',
        className: 'query-explain-spinner',
        attributes: { 'aria-hidden': 'true' }
    });
    const explainTriggerCancel = registerElement('input', {
        id: 'explain-trigger-cancel',
        type: 'button',
        className: 'query-explain-cancel',
        disabled: true,
        attributes: { 'aria-hidden': 'true' }
    });
    const rerunExplanation = registerElement('input', { id: 'rerun-explanation', type: 'button' });
    const rerunExplanationSpinner = registerElement('span', {
        id: 'rerun-explanation-spinner',
        className: 'query-explain-spinner',
        attributes: { 'aria-hidden': 'true' }
    });
    const rerunExplanationCancel = registerElement('input', {
        id: 'rerun-explanation-cancel',
        type: 'button',
        className: 'query-explain-cancel',
        disabled: true,
        attributes: { 'aria-hidden': 'true' }
    });
    const explainCompareTrigger = registerElement('button', { id: 'explain-compare-trigger' });
    const explainCompareCancel = registerElement('button', {
        id: 'explain-compare-cancel',
        className: 'query-explain-cancel',
        disabled: true,
        attributes: { 'aria-hidden': 'true' }
    });

    const queryExplanationRow = registerElement('div', { id: 'query-explanation-row' });
    const queryExplanationControlsRow = registerElement('div', { id: 'query-explanation-controls-row' });
    const queryExplanationStatus = registerElement('div', { id: 'query-explanation-status' });
    const queryExplanationOverlay = registerElement('div', { id: 'query-explanation-overlay' });
    const queryExplanation = registerElement('pre', {
        id: 'query-explanation',
        textContent: options.initialExplanation || '',
        attributes: { 'data-format': options.initialExplanationFormat || 'text' }
    });
    const queryExplanationDotView = registerElement('div', { id: 'query-explanation-dot-view' });
    const queryExplanationJsonView = registerElement('div', { id: 'query-explanation-json-view' });
    const queryErrors = registerElement('div', { id: 'queryString.errors' });
    const queryErrorsCompare = registerElement('div', { id: 'queryString.errors-compare' });
    const downloadExplanation = registerElement('button', { id: 'download-explanation' });
    const primaryExplainSettings = registerElement('div', { id: 'primary-explain-settings' });
    const primaryExplainRepeatControls = registerElement('div', { id: 'primary-explain-repeat-controls' });
    const compareToggle = registerElement('button', { id: 'compare-toggle' });
    const queryDiffTrigger = registerElement('button', { id: 'query-diff-trigger' });
    const queryCompareLayout = registerElement('div', { id: 'query-compare-layout' });
    const queryCompareControls = registerElement('div', { id: 'query-compare-controls' });
    const queryExplanationRowCompare = registerElement('div', { id: 'query-explanation-row-compare' });
    const queryExplanationStatusCompare = registerElement('div', { id: 'query-explanation-status-compare' });
    const queryExplanationOverlayCompare = registerElement('div', { id: 'query-explanation-overlay-compare' });
    const queryExplanationCompare = registerElement('pre', { id: 'query-explanation-compare' });
    const queryExplanationDotViewCompare = registerElement('div', { id: 'query-explanation-dot-view-compare' });
    const queryExplanationJsonViewCompare = registerElement('div', { id: 'query-explanation-json-view-compare' });
    const queryDiffModal = registerElement('div', {
        id: 'query-diff-modal',
        attributes: { 'aria-hidden': 'true' }
    });
    const queryDiffClose = registerElement('button', { id: 'query-diff-close' });
    const querySidebarToggle = registerElement('button', {
        id: 'query-sidebar-toggle',
        attributes: {
            'data-hide-label': 'Hide compare sidebar',
            'data-show-label': 'Show compare sidebar'
        }
    });
    const queryDiffQuery = registerElement('div', { id: 'query-diff-query' });
    const queryDiffExplanation = registerElement('div', {
        id: 'query-diff-explanation',
        textContent: options.diffNotReadyLabel || 'Run explanations for both queries.'
    });

    document.body.appendChild(form);
    [
        actionInput,
        explainInput,
        explainLevel,
        explainFormat,
        explainRequestIdInput,
        queryInput,
        compareQueryInput,
        queryLn,
        limitQuery,
        queryTimeout,
        infer,
        includeQueryText,
        queryName,
        savePrivate,
        sparqlNamespaces,
        serqlNamespaces,
        exec,
        save,
        saveFeedback,
        explainTrigger,
        explainTriggerSpinner,
        explainTriggerCancel,
        rerunExplanation,
        rerunExplanationSpinner,
        rerunExplanationCancel,
        explainCompareTrigger,
        explainCompareCancel,
        queryExplanationRow,
        queryExplanationControlsRow,
        queryExplanationStatus,
        queryExplanationOverlay,
        queryExplanation,
        queryExplanationDotView,
        queryExplanationJsonView,
        queryErrors,
        queryErrorsCompare,
        downloadExplanation,
        primaryExplainSettings,
        primaryExplainRepeatControls,
        compareToggle,
        queryDiffTrigger,
        queryCompareLayout,
        queryCompareControls,
        queryExplanationRowCompare,
        queryExplanationStatusCompare,
        queryExplanationOverlayCompare,
        queryExplanationCompare,
        queryExplanationDotViewCompare,
        queryExplanationJsonViewCompare,
        queryDiffModal,
        queryDiffClose,
        querySidebarToggle,
        queryDiffQuery,
        queryDiffExplanation
    ].forEach((element) => {
        document.body.appendChild(element);
    });

    if (!context.Diff) {
        harness.runScript('tools/workbench/src/main/webapp/scripts/diff.min.js');
    }
    harness.runScript('tools/workbench/src/main/webapp/scripts/queryCancelPolicy.js');
    harness.runScript('tools/workbench/src/main/webapp/scripts/query.js');

    explainTrigger.onclick = () => context.workbench.query.runExplain(null, 'explain-trigger');
    explainTriggerCancel.onclick = () => context.workbench.query.cancelExplain();
    rerunExplanation.onclick = () => context.workbench.query.runExplain(null, 'rerun-explanation');
    rerunExplanationCancel.onclick = () => context.workbench.query.cancelExplain();
    explainCompareTrigger.onclick = () => context.workbench.query.runCompareExplain('explain-compare-trigger');
    explainCompareCancel.onclick = () => context.workbench.query.cancelCompareExplain();
    explainLevel.onchange = () => context.workbench.query.notifyQueryPageInputChange('EXPLAIN_LEVEL_CHANGED');
    explainFormat.onchange = () => context.workbench.query.notifyQueryPageInputChange('EXPLAIN_FORMAT_CHANGED');

    return Object.assign({}, harness, {
        getJSONRequests,
        pendingGetJSONRequests,
        pendingExplainRequests,
        yasqeState: yasqe.state,
        requestsByAction(action) {
            return harness.ajaxRequests.filter((request) => request.action === action);
        },
        resolveNextGetJSON(response) {
            const request = pendingGetJSONRequests.shift();
            if (!request) {
                throw new Error('No pending getJSON request to resolve');
            }
            if (typeof response !== 'undefined') {
                request.response = response;
            }
            if (typeof request.callback === 'function') {
                request.callback(request.response);
            }
        },
        runPageLoad() {
            harness.runLoadHandlers();
        }
    });
}

module.exports = {
    createQueryBrowserHarness
};
