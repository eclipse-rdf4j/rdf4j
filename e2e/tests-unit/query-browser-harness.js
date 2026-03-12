const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

function splitClassNames(value) {
    return String(value || '')
        .trim()
        .split(/\s+/)
        .filter(Boolean);
}

class FakeClassList {
    constructor(element) {
        this.element = element;
    }

    add() {
        Array.from(arguments).flatMap(splitClassNames).forEach((className) => {
            this.element.classes.add(className);
        });
    }

    remove() {
        if (arguments.length === 0) {
            this.element.classes.clear();
            return;
        }
        Array.from(arguments).flatMap(splitClassNames).forEach((className) => {
            this.element.classes.delete(className);
        });
    }

    toggle(className, force) {
        if (force === true) {
            this.add(className);
            return true;
        }
        if (force === false) {
            this.remove(className);
            return false;
        }
        if (this.contains(className)) {
            this.remove(className);
            return false;
        }
        this.add(className);
        return true;
    }

    contains(className) {
        return this.element.classes.has(className);
    }
}

class FakeElement {
    constructor(ownerDocument, tagName, options = {}) {
        this.ownerDocument = ownerDocument;
        this.tagName = String(tagName || 'div').toUpperCase();
        this.id = options.id || '';
        this.name = options.name || '';
        this.value = options.value || '';
        this.textContent = options.textContent || '';
        this.innerHTML = '';
        this.visible = options.visible !== undefined ? options.visible : true;
        this.disabled = !!options.disabled;
        this.type = options.type || '';
        this.attributes = new Map();
        this.classes = new Set();
        this.classList = new FakeClassList(this);
        this.style = {};
        this.children = [];
        this.parentNode = null;
        this.eventHandlers = new Map();
        this.formControls = [];
        this.onclick = null;
        this.onchange = null;
        if (options.className) {
            this.className = options.className;
        }
        if (this.id) {
            this.attributes.set('id', this.id);
        }
        if (this.name) {
            this.attributes.set('name', this.name);
        }
        if (this.type) {
            this.attributes.set('type', this.type);
        }
        if (this.disabled) {
            this.attributes.set('disabled', 'disabled');
        }
        Object.entries(options.attributes || {}).forEach(([name, value]) => {
            this.setAttribute(name, value);
        });
    }

    get className() {
        return Array.from(this.classes).join(' ');
    }

    set className(value) {
        this.classes = new Set(splitClassNames(value));
        this.classList = new FakeClassList(this);
        if (this.classes.size) {
            this.attributes.set('class', Array.from(this.classes).join(' '));
        } else {
            this.attributes.delete('class');
        }
    }

    setAttribute(name, value) {
        const normalizedValue = String(value);
        this.attributes.set(name, normalizedValue);
        if (name === 'id') {
            this.id = normalizedValue;
            this.ownerDocument.reindex(this);
        } else if (name === 'name') {
            this.name = normalizedValue;
        } else if (name === 'class') {
            this.className = normalizedValue;
        } else if (name === 'disabled') {
            this.disabled = normalizedValue !== 'false';
        }
    }

    getAttribute(name) {
        return this.attributes.has(name) ? this.attributes.get(name) : undefined;
    }

    removeAttribute(name) {
        this.attributes.delete(name);
        if (name === 'class') {
            this.classes.clear();
        } else if (name === 'disabled') {
            this.disabled = false;
        }
    }

    addEventListener(type, handler) {
        if (!this.eventHandlers.has(type)) {
            this.eventHandlers.set(type, []);
        }
        this.eventHandlers.get(type).push(handler);
    }

    trigger(type, event = {}) {
        const normalizedEvent = Object.assign({ currentTarget: this, target: this, type }, event);
        const handlers = this.eventHandlers.get(type) || [];
        handlers.forEach((handler) => handler.call(this, normalizedEvent));
        const propertyHandler = this['on' + type];
        if (typeof propertyHandler === 'function') {
            propertyHandler.call(this, normalizedEvent);
        }
    }

    click() {
        if (this.disabled) {
            return;
        }
        this.focus();
        this.trigger('click');
    }

    focus() {
        this.ownerDocument.activeElement = this;
    }

    appendChild(child) {
        this.children.push(child);
        child.parentNode = this;
        return child;
    }

    removeChild(child) {
        this.children = this.children.filter((candidate) => candidate !== child);
        child.parentNode = null;
        return child;
    }

    getBoundingClientRect() {
        return { top: 0, left: 0, width: 0, height: 0 };
    }

    serializeArray() {
        return this.formControls.map((control) => ({
            name: control.name,
            value: control.value || ''
        }));
    }
}

class FakeDocument {
    constructor() {
        this.elements = [];
        this.elementsById = new Map();
        this.activeElement = null;
        this.cookie = '';
        this.all = false;
        this.location = {
            href: 'http://localhost:8080/rdf4j-workbench/repositories/test/query',
            pathname: '/rdf4j-workbench/repositories/test/query',
            search: ''
        };
        this.body = this.register(new FakeElement(this, 'body'));
    }

    register(element) {
        this.elements.push(element);
        if (element.id) {
            this.elementsById.set(element.id, element);
        }
        return element;
    }

    reindex(element) {
        for (const [id, indexedElement] of this.elementsById.entries()) {
            if (indexedElement === element && id !== element.id) {
                this.elementsById.delete(id);
            }
        }
        if (element.id) {
            this.elementsById.set(element.id, element);
        }
    }

    createElement(tagName) {
        return new FakeElement(this, tagName);
    }

    getElementById(id) {
        return this.elementsById.get(id) || null;
    }

    querySelectorAll(selector) {
        return selector.split(',').flatMap((part) => {
            const normalizedSelector = part.trim();
            if (!normalizedSelector) {
                return [];
            }
            if (normalizedSelector === 'body') {
                return [this.body];
            }
            if (normalizedSelector.startsWith('#')) {
                const element = this.getElementById(normalizedSelector.slice(1));
                return element ? [element] : [];
            }
            if (normalizedSelector.startsWith('.')) {
                const className = normalizedSelector.slice(1);
                return this.elements.filter((element) => element.classList.contains(className));
            }
            const attributeSelectorMatch = normalizedSelector.match(/^([a-zA-Z0-9_-]+)\[([^=]+)="([^"]*)"\]$/);
            if (attributeSelectorMatch) {
                const [, tagName, attributeName, attributeValue] = attributeSelectorMatch;
                return this.elements.filter((element) => {
                    return element.tagName === tagName.toUpperCase()
                        && element.getAttribute(attributeName) === attributeValue;
                });
            }
            return this.elements.filter((element) => element.tagName === normalizedSelector.toUpperCase());
        }).filter((element, index, elements) => elements.indexOf(element) === index);
    }
}

class FakeTimerQueue {
    constructor() {
        this.now = 0;
        this.nextId = 1;
        this.tasks = new Map();
    }

    setTimeout(callback, delay) {
        const taskId = this.nextId++;
        this.tasks.set(taskId, {
            callback,
            scheduledFor: this.now + (delay || 0)
        });
        return taskId;
    }

    clearTimeout(taskId) {
        this.tasks.delete(taskId);
    }

    advance(ms) {
        const targetTime = this.now + ms;
        while (true) {
            let nextTaskId = null;
            let nextTaskTime = Number.POSITIVE_INFINITY;
            for (const [taskId, task] of this.tasks.entries()) {
                if (task.scheduledFor < nextTaskTime) {
                    nextTaskId = taskId;
                    nextTaskTime = task.scheduledFor;
                }
            }
            if (nextTaskId === null || nextTaskTime > targetTime) {
                break;
            }
            this.now = nextTaskTime;
            const nextTask = this.tasks.get(nextTaskId);
            this.tasks.delete(nextTaskId);
            nextTask.callback();
        }
        this.now = targetTime;
    }
}

class JQueryCollection {
    constructor(elements) {
        this.elements = elements.filter(Boolean);
        this.length = this.elements.length;
    }

    each(callback) {
        this.elements.forEach((element, index) => callback.call(element, index, element));
        return this;
    }

    val(value) {
        if (value === undefined) {
            return this.elements[0] ? this.elements[0].value : undefined;
        }
        return this.each((index, element) => {
            element.value = value == null ? '' : String(value);
        });
    }

    text(value) {
        if (value === undefined) {
            return this.elements[0] ? this.elements[0].textContent : '';
        }
        return this.each((index, element) => {
            element.textContent = String(value);
            element.innerHTML = '';
            element.children = [];
        });
    }

    html(value) {
        if (value === undefined) {
            return this.elements[0] ? this.elements[0].innerHTML : '';
        }
        return this.each((index, element) => {
            element.innerHTML = String(value);
            element.textContent = '';
            element.children = [];
        });
    }

    attr(name, value) {
        if (value === undefined) {
            return this.elements[0] ? this.elements[0].getAttribute(name) : undefined;
        }
        return this.each((index, element) => {
            element.setAttribute(name, value);
        });
    }

    prop(name, value) {
        if (value === undefined) {
            return this.elements[0] ? this.elements[0][name] : undefined;
        }
        return this.each((index, element) => {
            element[name] = value;
            if (name === 'disabled') {
                if (value) {
                    element.setAttribute('disabled', 'disabled');
                } else {
                    element.removeAttribute('disabled');
                }
            }
        });
    }

    removeAttr(name) {
        return this.each((index, element) => {
            element.removeAttribute(name);
        });
    }

    show() {
        return this.toggle(true);
    }

    hide() {
        return this.toggle(false);
    }

    toggle(force) {
        return this.each((index, element) => {
            element.visible = force === undefined ? !element.visible : !!force;
        });
    }

    addClass(className) {
        return this.each((index, element) => {
            element.classList.add(className);
            if (element.className) {
                element.attributes.set('class', element.className);
            }
        });
    }

    removeClass(className) {
        return this.each((index, element) => {
            if (className === undefined) {
                element.classList.remove();
            } else {
                element.classList.remove(className);
            }
            if (element.className) {
                element.attributes.set('class', element.className);
            } else {
                element.attributes.delete('class');
            }
        });
    }

    toggleClass(className, force) {
        return this.each((index, element) => {
            splitClassNames(className).forEach((singleClassName) => {
                element.classList.toggle(singleClassName, force);
            });
            if (element.className) {
                element.attributes.set('class', element.className);
            } else {
                element.attributes.delete('class');
            }
        });
    }

    css(name, value) {
        if (typeof name === 'string' && value === undefined) {
            return this.elements[0] ? this.elements[0].style[name] : undefined;
        }
        return this.each((index, element) => {
            if (typeof name === 'string') {
                element.style[name] = value;
                return;
            }
            Object.assign(element.style, name);
        });
    }

    append(child) {
        const normalizedChildren = child instanceof JQueryCollection
            ? child.elements
            : Array.isArray(child)
                ? child
                : [child];
        return this.each((index, element) => {
            normalizedChildren.forEach((candidate) => {
                if (candidate instanceof FakeElement) {
                    element.appendChild(candidate);
                }
            });
        });
    }

    empty() {
        return this.each((index, element) => {
            element.textContent = '';
            element.innerHTML = '';
            element.children = [];
        });
    }

    click(handler) {
        if (typeof handler === 'function') {
            return this.bind('click', handler);
        }
        return this.each((index, element) => {
            element.click();
        });
    }

    change(handler) {
        if (typeof handler === 'function') {
            return this.bind('change', handler);
        }
        return this.each((index, element) => {
            element.trigger('change');
        });
    }

    keydown(handler) {
        if (typeof handler === 'function') {
            return this.bind('keydown', handler);
        }
        return this.each((index, element) => {
            element.trigger('keydown');
        });
    }

    bind(eventNames, handler) {
        const eventTypes = String(eventNames || '').split(/\s+/).filter(Boolean);
        return this.each((index, element) => {
            eventTypes.forEach((eventType) => {
                element.addEventListener(eventType, handler);
            });
        });
    }

    focus() {
        return this.each((index, element) => {
            element.focus();
        });
    }

    serializeArray() {
        if (!this.elements[0] || typeof this.elements[0].serializeArray !== 'function') {
            return [];
        }
        return this.elements[0].serializeArray();
    }

    outerHeight() {
        return 0;
    }

    outerWidth() {
        return 0;
    }
}

function createFragment(document, selector) {
    const fragmentMatch = String(selector).trim().match(/^<([a-zA-Z0-9_-]+)([^>]*)><\/\1>$/);
    if (!fragmentMatch) {
        return [];
    }
    const [, tagName, attributeText] = fragmentMatch;
    const element = document.createElement(tagName);
    const classMatch = attributeText.match(/class="([^"]*)"/);
    if (classMatch) {
        element.className = classMatch[1];
    }
    return [element];
}

function createJQuery(document, ajaxHandler) {
    const $ = (selector) => {
        if (selector instanceof FakeElement) {
            return new JQueryCollection([selector]);
        }
        if (selector instanceof JQueryCollection) {
            return new JQueryCollection(selector.elements);
        }
        if (selector === document) {
            return new JQueryCollection([]);
        }
        if (typeof selector === 'string' && selector.trim().startsWith('<')) {
            return new JQueryCollection(createFragment(document, selector));
        }
        if (typeof selector === 'string') {
            return new JQueryCollection(document.querySelectorAll(selector));
        }
        return new JQueryCollection([]);
    };

    $.trim = (value) => String(value == null ? '' : value).trim();
    $.param = (entries) => {
        return (entries || [])
            .map((entry) => `${encodeURIComponent(entry.name)}=${encodeURIComponent(entry.value == null ? '' : entry.value)}`)
            .join('&');
    };
    $.ajax = ajaxHandler;
    $.getJSON = (url, data, callback) => {
        if (typeof callback === 'function') {
            callback({});
        }
    };
    return $;
}

function parseRequestData(data) {
    return new URLSearchParams(typeof data === 'string' ? data : '');
}

function createQueryBrowserHarness(options = {}) {
    const timers = new FakeTimerQueue();
    const document = new FakeDocument();
    const loadCallbacks = [];
    const ajaxRequests = [];
    const pendingExplainRequests = [];
    const generatedRequestIds = Array.from(options.serverRequestIds || []);
    let generatedRequestIndex = 0;

    function registerElement(tagName, elementOptions) {
        return document.register(new FakeElement(document, tagName, elementOptions));
    }

    const navigation = registerElement('div', { id: 'navigation' });
    const titleHeading = registerElement('div', { id: 'title_heading' });
    const noScriptMessage = registerElement('div', { id: 'noscript-message' });
    const queryFormContainer = registerElement('div', { className: 'query-form' });
    document.body.appendChild(navigation);
    document.body.appendChild(titleHeading);
    document.body.appendChild(noScriptMessage);
    document.body.appendChild(queryFormContainer);

    const form = registerElement('form', {
        attributes: {
            action: 'query'
        }
    });
    const actionInput = registerElement('input', { id: 'action', name: 'action', value: '' });
    const explainInput = registerElement('input', { id: 'explain', name: 'explain', value: '' });
    const explainLevel = registerElement('select', { id: 'explain-level', value: 'Optimized' });
    const explainFormat = registerElement('select', { id: 'explain-format', name: 'explain-format', value: 'text' });
    const explainRequestIdInput = registerElement('input', { id: 'explain-request-id', name: 'explain-request-id', value: '' });
    const queryInput = registerElement('textarea', { id: 'query', name: 'query', value: options.query || 'SELECT * WHERE {?s ?p ?o}' });
    const compareQueryInput = registerElement('textarea', { id: 'query-compare', value: '' });
    form.formControls = [actionInput, explainInput, explainFormat, explainRequestIdInput, queryInput];

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
    const queryExplanation = registerElement('pre', { id: 'query-explanation' });
    const queryExplanationDotView = registerElement('div', { id: 'query-explanation-dot-view' });
    const queryExplanationJsonView = registerElement('div', { id: 'query-explanation-json-view' });
    const queryErrors = registerElement('div', { id: 'queryString.errors' });
    const downloadExplanation = registerElement('button', { id: 'download-explanation' });
    const primaryExplainSettings = registerElement('div', { id: 'primary-explain-settings' });
    const primaryExplainRepeatControls = registerElement('div', { id: 'primary-explain-repeat-controls' });
    const compareToggle = registerElement('button', { id: 'compare-toggle' });
    const queryDiffTrigger = registerElement('button', { id: 'query-diff-trigger' });
    const queryCompareLayout = registerElement('div', { id: 'query-compare-layout' });
    const queryCompareControls = registerElement('div', { id: 'query-compare-controls' });
    const queryDiffModal = registerElement('div', {
        id: 'query-diff-modal',
        attributes: { 'aria-hidden': 'true' }
    });
    const querySidebarToggle = registerElement('button', {
        id: 'query-sidebar-toggle',
        attributes: {
            'data-hide-label': 'Hide compare sidebar',
            'data-show-label': 'Show compare sidebar'
        }
    });
    const queryDiffExplanation = registerElement('div', { id: 'query-diff-explanation' });

    document.body.appendChild(form);
    [
        actionInput,
        explainInput,
        explainLevel,
        explainFormat,
        explainRequestIdInput,
        queryInput,
        compareQueryInput,
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
        downloadExplanation,
        primaryExplainSettings,
        primaryExplainRepeatControls,
        compareToggle,
        queryDiffTrigger,
        queryCompareLayout,
        queryCompareControls,
        queryDiffModal,
        querySidebarToggle,
        queryDiffExplanation
    ].forEach((element) => {
        document.body.appendChild(element);
    });

    function handleAjax(options) {
        const requestParams = parseRequestData(options.data);
        const request = {
            data: typeof options.data === 'string' ? options.data : '',
            options,
            params: requestParams,
            action: requestParams.get('action'),
            aborted: false,
            completed: false
        };
        const jqXHR = {
            abort() {
                if (request.completed || request.aborted) {
                    return;
                }
                request.aborted = true;
                if (typeof options.error === 'function') {
                    options.error(jqXHR, 'abort');
                }
                if (typeof options.complete === 'function') {
                    options.complete(jqXHR, 'abort');
                }
            }
        };
        request.jqXHR = jqXHR;
        request.resolve = (response) => {
            if (request.completed || request.aborted) {
                return;
            }
            request.completed = true;
            if (typeof options.success === 'function') {
                options.success(response, 'success', jqXHR);
            }
            if (typeof options.complete === 'function') {
                options.complete(jqXHR, 'success');
            }
        };
        ajaxRequests.push(request);
        if (request.action === 'explain') {
            pendingExplainRequests.push(request);
            return jqXHR;
        }
        request.completed = true;
        if (typeof options.complete === 'function') {
            options.complete(jqXHR, 'success');
        }
        return jqXHR;
    }

    const $ = createJQuery(document, handleAjax);
    const workbench = {
        addLoad(callback) {
            loadCallbacks.push(callback);
        },
        getCookie() {
            return '';
        },
        queryCancelPolicy: undefined,
        yasqeHelper: {
            setupCompleters() {
            }
        }
    };
    const window = {
        clearTimeout: (id) => timers.clearTimeout(id),
        crypto: {
            randomUUID() {
                if (generatedRequestIndex < generatedRequestIds.length) {
                    const nextRequestId = generatedRequestIds[generatedRequestIndex];
                    generatedRequestIndex += 1;
                    return nextRequestId;
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
        sessionStorage: {
            removeItem() {
            }
        },
        setTimeout: (callback, delay) => timers.setTimeout(callback, delay)
    };
    window.window = window;
    const context = vm.createContext({
        $,
        Blob: function Blob() {
        },
        Date: { now: () => timers.now },
        URL: {
            createObjectURL() {
                return 'blob:query';
            },
            revokeObjectURL() {
            }
        },
        Viz: undefined,
        YASQE: {
            storeQuery() {
            }
        },
        console,
        document,
        jQuery: $,
        setTimeout: window.setTimeout,
        clearTimeout: window.clearTimeout,
        window,
        workbench
    });
    context.globalThis = context;
    context.self = window;
    context.window.URL = context.URL;

    function runScript(relativePath) {
        const absolutePath = path.resolve(__dirname, '..', '..', relativePath);
        const source = fs.readFileSync(absolutePath, 'utf8');
        vm.runInContext(source, context, { filename: absolutePath });
    }

    runScript('tools/workbench/src/main/webapp/scripts/queryCancelPolicy.js');
    runScript('tools/workbench/src/main/webapp/scripts/query.js');

    explainTrigger.onclick = () => context.workbench.query.runExplain(null, 'explain-trigger');
    explainTriggerCancel.onclick = () => context.workbench.query.cancelExplain();
    rerunExplanation.onclick = () => context.workbench.query.runExplain(null, 'rerun-explanation');
    rerunExplanationCancel.onclick = () => context.workbench.query.cancelExplain();
    explainCompareTrigger.onclick = () => context.workbench.query.runCompareExplain('explain-compare-trigger');
    explainCompareCancel.onclick = () => context.workbench.query.cancelCompareExplain();
    explainLevel.onchange = () => context.workbench.query.notifyQueryPageInputChange('EXPLAIN_LEVEL_CHANGED');
    explainFormat.onchange = () => context.workbench.query.notifyQueryPageInputChange('EXPLAIN_FORMAT_CHANGED');

    return {
        ajaxRequests,
        advanceTimers(ms) {
            timers.advance(ms);
        },
        click(id) {
            const element = document.getElementById(id);
            if (!element) {
                throw new Error(`Unknown element: ${id}`);
            }
            element.click();
        },
        context,
        getAttribute(id, name) {
            const element = document.getElementById(id);
            return element ? element.getAttribute(name) : undefined;
        },
        getProperty(id, name) {
            const element = document.getElementById(id);
            return element ? element[name] : undefined;
        },
        hasClass(id, className) {
            const element = document.getElementById(id);
            return !!element && element.classList.contains(className);
        },
        pendingExplainRequests,
        requestsByAction(action) {
            return ajaxRequests.filter((request) => request.action === action);
        },
        setValue(id, value) {
            const element = document.getElementById(id);
            if (!element) {
                throw new Error(`Unknown element: ${id}`);
            }
            element.value = value;
            element.trigger('change');
        }
    };
}

module.exports = {
    createQueryBrowserHarness
};
