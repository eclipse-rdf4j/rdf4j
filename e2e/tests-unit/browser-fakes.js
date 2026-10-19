function splitClassNames(value) {
    return String(value || '')
        .trim()
        .split(/\s+/)
        .filter(Boolean);
}

function createLocation(initialHref) {
    const location = {};

    function apply(nextHref) {
        const parsed = new URL(nextHref, 'http://localhost:8080');
        location._href = parsed.href;
        location.pathname = parsed.pathname;
        location.search = parsed.search;
    }

    Object.defineProperty(location, 'href', {
        enumerable: true,
        get() {
            return location._href;
        },
        set(value) {
            apply(String(value));
        }
    });

    apply(initialHref || 'http://localhost:8080/rdf4j-workbench/repositories/test/query');
    return location;
}

function createTextNode(ownerDocument, text) {
    return {
        nodeType: 3,
        ownerDocument,
        parentNode: null,
        textContent: String(text)
    };
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
        this.type = options.type || '';
        this.checked = !!options.checked;
        this.selected = !!options.selected;
        this.disabled = !!options.disabled;
        this.readOnly = !!options.readOnly;
        this.visible = options.visible !== undefined ? options.visible : true;
        this.attributes = new Map();
        this.classes = new Set();
        this.classList = new FakeClassList(this);
        this.style = Object.assign({}, options.style);
        this.children = [];
        this.parentNode = null;
        this.eventHandlers = new Map();
        this.formControls = [];
        this.onclick = null;
        this.onchange = null;
        this.onsubmit = null;
        this.submitCount = 0;
        this._textContent = options.textContent || '';
        this._innerHTML = options.innerHTML || '';
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
        if (this.checked) {
            this.attributes.set('checked', 'checked');
        }
        if (this.selected) {
            this.attributes.set('selected', 'selected');
        }
        if (this.disabled) {
            this.attributes.set('disabled', 'disabled');
        }
        if (this.readOnly) {
            this.attributes.set('readonly', 'readonly');
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

    get innerHTML() {
        return this._innerHTML;
    }

    set innerHTML(value) {
        this._innerHTML = String(value);
        this._textContent = '';
        this.children = [];
    }

    get textContent() {
        const childText = this.children
            .map((child) => child.nodeType === 3 ? child.textContent : child.textContent)
            .join('');
        return this._textContent + childText;
    }

    set textContent(value) {
        this._textContent = String(value);
        this._innerHTML = '';
        this.children = [];
    }

    get innerText() {
        return this.textContent;
    }

    set innerText(value) {
        this.textContent = value;
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
        } else if (name === 'value') {
            this.value = normalizedValue;
        } else if (name === 'type') {
            this.type = normalizedValue;
        } else if (name === 'checked') {
            this.checked = normalizedValue !== 'false';
        } else if (name === 'selected') {
            this.selected = normalizedValue !== 'false';
        } else if (name === 'disabled') {
            this.disabled = normalizedValue !== 'false';
        } else if (name === 'readonly') {
            this.readOnly = normalizedValue !== 'false';
        }
    }

    getAttribute(name) {
        return this.attributes.has(name) ? this.attributes.get(name) : undefined;
    }

    removeAttribute(name) {
        this.attributes.delete(name);
        if (name === 'class') {
            this.classes.clear();
        } else if (name === 'checked') {
            this.checked = false;
        } else if (name === 'selected') {
            this.selected = false;
        } else if (name === 'disabled') {
            this.disabled = false;
        } else if (name === 'readonly') {
            this.readOnly = false;
        }
    }

    addEventListener(type, handler) {
        if (!this.eventHandlers.has(type)) {
            this.eventHandlers.set(type, []);
        }
        this.eventHandlers.get(type).push(handler);
    }

    removeEventListener(type, handler) {
        if (!this.eventHandlers.has(type)) {
            return;
        }
        if (!handler) {
            this.eventHandlers.delete(type);
            return;
        }
        this.eventHandlers.set(type, this.eventHandlers.get(type).filter((candidate) => candidate !== handler));
    }

    removeAllEventListeners() {
        this.eventHandlers.clear();
    }

    trigger(type, event = {}) {
        const normalizedEvent = Object.assign({
            currentTarget: this,
            target: this,
            type,
            defaultPrevented: false,
            preventDefault() {
                this.defaultPrevented = true;
            }
        }, event);
        const handlers = this.eventHandlers.get(type) || [];
        for (const handler of handlers) {
            const result = handler.call(this, normalizedEvent);
            if (result === false) {
                break;
            }
        }
        const propertyHandler = this['on' + type];
        if (typeof propertyHandler === 'function') {
            propertyHandler.call(this, normalizedEvent);
        }
        return normalizedEvent;
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
        const normalizedChild = typeof child === 'string'
            ? createTextNode(this.ownerDocument, child)
            : child;
        this.ownerDocument.track(normalizedChild);
        this.children.push(normalizedChild);
        normalizedChild.parentNode = this;
        if (this.tagName === 'FORM' && normalizedChild.nodeType !== 3
            && normalizedChild.name && !this.formControls.includes(normalizedChild)) {
            this.formControls.push(normalizedChild);
        }
        return normalizedChild;
    }

    removeChild(child) {
        this.children = this.children.filter((candidate) => candidate !== child);
        this.formControls = this.formControls.filter((candidate) => candidate !== child);
        child.parentNode = null;
        return child;
    }

    getBoundingClientRect() {
        return { top: 0, left: 0, width: 0, height: 0 };
    }

    getElementsByTagName(tagName) {
        const normalized = String(tagName).toUpperCase();
        const result = [];

        function visit(element) {
            element.children.forEach((child) => {
                if (child.nodeType === 3) {
                    return;
                }
                if (normalized === '*' || child.tagName === normalized) {
                    result.push(child);
                }
                visit(child);
            });
        }

        visit(this);
        return result;
    }

    serializeArray() {
        return this.formControls
            .filter((control) => control.name && !control.disabled)
            .filter((control) => !['checkbox', 'radio'].includes(control.type) || control.checked)
            .map((control) => ({
                name: control.name,
                value: control.value || ''
            }));
    }

    submit() {
        this.submitCount += 1;
        this.ownerDocument.lastSubmittedForm = this;
        if (typeof this.onsubmit === 'function') {
            this.onsubmit();
        }
    }
}

class FakeDocument {
    constructor(initialHref) {
        this.elements = [];
        this.elementsById = new Map();
        this.activeElement = null;
        this.cookies = new Map();
        this.all = false;
        this.eventHandlers = new Map();
        this.location = createLocation(initialHref);
        this.body = this.register(new FakeElement(this, 'body'));
        this.children = [this.body];
        this.forms = {
            namedItem: (name) => {
                return this.elements.find((element) => {
                    return element.tagName === 'FORM' && (element.name === name || element.id === name);
                }) || null;
            }
        };
        Object.defineProperty(this, 'cookie', {
            configurable: true,
            enumerable: true,
            get: () => Array.from(this.cookies.entries())
                .map(([name, value]) => `${name}=${value}`)
                .join('; '),
            set: (value) => this.setCookieString(value)
        });
    }

    setCookieString(rawValue) {
        const segments = String(rawValue || '')
            .split(';')
            .map((segment) => segment.trim())
            .filter(Boolean);
        if (!segments.length) {
            return;
        }

        const attributeNames = new Set(['domain', 'expires', 'httponly', 'max-age', 'path', 'samesite', 'secure']);
        const assignments = [];
        let currentAssignment = null;

        for (const segment of segments) {
            const separatorIndex = segment.indexOf('=');
            const name = (separatorIndex >= 0 ? segment.substring(0, separatorIndex) : segment).trim().toLowerCase();
            const isAttribute = currentAssignment && attributeNames.has(name);
            if (isAttribute) {
                currentAssignment.attributes.push(segment);
                continue;
            }
            if (currentAssignment) {
                assignments.push(currentAssignment);
            }
            currentAssignment = {
                cookie: segment,
                attributes: []
            };
        }

        if (currentAssignment) {
            assignments.push(currentAssignment);
        }

        assignments.forEach((assignment) => this.applyCookieAssignment(assignment.cookie, assignment.attributes));
    }

    applyCookieAssignment(cookiePair, attributes) {
        const separatorIndex = cookiePair.indexOf('=');
        if (separatorIndex < 0) {
            return;
        }

        const name = cookiePair.substring(0, separatorIndex).trim();
        const value = cookiePair.substring(separatorIndex + 1);
        const expiresAttribute = attributes.find((attribute) => attribute.toLowerCase().startsWith('expires='));
        const maxAgeAttribute = attributes.find((attribute) => attribute.toLowerCase().startsWith('max-age='));
        const expiresAt = expiresAttribute ? Date.parse(expiresAttribute.substring('expires='.length)) : NaN;
        const maxAge = maxAgeAttribute ? Number(maxAgeAttribute.substring('max-age='.length)) : NaN;
        const isExpired = (!Number.isNaN(expiresAt) && expiresAt <= Date.now())
            || (!Number.isNaN(maxAge) && maxAge <= 0);

        if (isExpired) {
            this.cookies.delete(name);
            return;
        }

        this.cookies.set(name, value);
    }

    track(node) {
        if (!node || node.nodeType === 3) {
            return node;
        }
        if (!this.elements.includes(node)) {
            this.elements.push(node);
        }
        if (node.id) {
            this.elementsById.set(node.id, node);
        }
        return node;
    }

    register(element) {
        return this.track(element);
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
        return this.track(new FakeElement(this, tagName));
    }

    createTextNode(text) {
        return createTextNode(this, text);
    }

    getElementById(id) {
        return this.elementsById.get(id) || null;
    }

    getElementsByTagName(tagName) {
        const normalized = String(tagName).toUpperCase();
        const result = normalized === 'BODY' ? [this.body] : [];
        return result.concat(this.body.getElementsByTagName(tagName));
    }

    querySelectorAll(selector) {
        return selectElements([this.body], selector, true);
    }

    addEventListener(type, handler) {
        if (!this.eventHandlers.has(type)) {
            this.eventHandlers.set(type, []);
        }
        this.eventHandlers.get(type).push(handler);
    }

    removeEventListener(type, handler) {
        if (!this.eventHandlers.has(type)) {
            return;
        }
        if (!handler) {
            this.eventHandlers.delete(type);
            return;
        }
        this.eventHandlers.set(type, this.eventHandlers.get(type).filter((candidate) => candidate !== handler));
    }

    removeAllEventListeners() {
        this.eventHandlers.clear();
    }

    trigger(type, event = {}) {
        const normalizedEvent = Object.assign({
            currentTarget: this,
            target: this,
            type
        }, event);
        const handlers = this.eventHandlers.get(type) || [];
        handlers.forEach((handler) => handler.call(this, normalizedEvent));
        return normalizedEvent;
    }
}

function parseSelectorToken(selector) {
    const token = String(selector || '').trim();
    const firstOnly = /:first$/.test(token);
    const selected = /:selected$/.test(token);
    const checked = /:checked$/.test(token);
    const stripped = token.replace(/:(first|selected|checked)$/, '');
    const tagMatch = stripped.match(/^[a-zA-Z0-9_-]+/);
    const idMatch = stripped.match(/#([a-zA-Z0-9_-]+)/);
    const classMatches = Array.from(stripped.matchAll(/\.([a-zA-Z0-9_-]+)/g)).map((match) => match[1]);
    const attributeMatches = Array.from(stripped.matchAll(/\[([^\]=]+)(?:=['"]?([^'"\]]*)['"]?)?\]/g)).map((match) => {
        return { name: match[1], value: match[2] };
    });

    return {
        tagName: tagMatch ? tagMatch[0].toUpperCase() : '',
        id: idMatch ? idMatch[1] : '',
        classNames: classMatches,
        attributes: attributeMatches,
        checked,
        firstOnly,
        selected
    };
}

function matchesSelectorToken(element, selector) {
    if (!element || element.nodeType === 3) {
        return false;
    }

    const token = parseSelectorToken(selector);
    if (token.tagName && element.tagName !== token.tagName) {
        return false;
    }
    if (token.id && element.id !== token.id) {
        return false;
    }
    if (!token.classNames.every((className) => element.classList.contains(className))) {
        return false;
    }
    if (!token.attributes.every((attribute) => {
        if (attribute.value === undefined) {
            return element.getAttribute(attribute.name) !== undefined;
        }
        return element.getAttribute(attribute.name) === attribute.value;
    })) {
        return false;
    }
    if (token.checked && !element.checked) {
        return false;
    }
    if (token.selected && !element.selected) {
        return false;
    }
    return true;
}

function collectDescendants(root, includeSelf) {
    const descendants = [];

    function visit(node, shouldInclude) {
        if (!node || node.nodeType === 3) {
            return;
        }
        if (shouldInclude) {
            descendants.push(node);
        }
        node.children.forEach((child) => visit(child, true));
    }

    visit(root, includeSelf);
    return descendants;
}

function selectElements(roots, selector, includeSelf) {
    return String(selector || '')
        .split(',')
        .flatMap((part) => {
            const tokens = part.trim().replace(/>/g, ' > ').split(/\s+/).filter(Boolean);
            if (!tokens.length) {
                return [];
            }
            let currentRoots = roots;
            let directOnly = false;
            tokens.forEach((token, index) => {
                if (token === '>') {
                    directOnly = true;
                    return;
                }
                const matches = currentRoots.flatMap((root) => {
                    const candidates = directOnly
                        ? (root.children || []).filter((child) => child.nodeType !== 3)
                        : collectDescendants(root, includeSelf && index === 0);
                    return candidates
                        .filter((element) => matchesSelectorToken(element, token));
                });
                currentRoots = parseSelectorToken(token).firstOnly ? matches.slice(0, 1) : matches;
                directOnly = false;
            });
            return currentRoots;
        })
        .filter((element, index, elements) => elements.indexOf(element) === index);
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
        for (let index = 0; index < this.elements.length; index += 1) {
            const element = this.elements[index];
            const result = callback.call(element, index, element);
            if (result === false) {
                break;
            }
        }
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
        });
    }

    html(value) {
        if (value === undefined) {
            return this.elements[0] ? this.elements[0].innerHTML : '';
        }
        return this.each((index, element) => {
            element.innerHTML = String(value);
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
            } else if (name === 'checked') {
                if (value) {
                    element.setAttribute('checked', 'checked');
                } else {
                    element.removeAttribute('checked');
                }
            } else if (name === 'selected') {
                if (value) {
                    element.setAttribute('selected', 'selected');
                } else {
                    element.removeAttribute('selected');
                }
            } else if (name === 'readOnly') {
                if (value) {
                    element.setAttribute('readonly', 'readonly');
                } else {
                    element.removeAttribute('readonly');
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
            element.style.display = element.visible ? '' : 'none';
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
                if (candidate) {
                    element.appendChild(candidate);
                }
            });
        });
    }

    empty() {
        return this.each((index, element) => {
            element.textContent = '';
            element.innerHTML = '';
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

    on(eventNames, handler) {
        return this.bind(eventNames, handler);
    }

    off(eventNames) {
        const eventTypes = String(eventNames || '').split(/\s+/).filter(Boolean);
        return this.each((index, element) => {
            if (!eventTypes.length) {
                element.removeAllEventListeners();
                return;
            }
            eventTypes.forEach((eventType) => {
                element.removeEventListener(eventType);
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

    serialize() {
        return this.serializeArray()
            .map((entry) => `${encodeURIComponent(entry.name)}=${encodeURIComponent(entry.value == null ? '' : entry.value)}`)
            .join('&');
    }

    outerHeight() {
        return this.elements[0] ? (this.elements[0].outerHeight || 0) : 0;
    }

    outerWidth() {
        return this.elements[0] ? (this.elements[0].outerWidth || 0) : 0;
    }

    find(selector) {
        return new JQueryCollection(this.elements.flatMap((element) => selectElements([element], selector, false)));
    }

    filter(selector) {
        if (typeof selector === 'function') {
            return new JQueryCollection(this.elements.filter((element, index) => selector.call(element, index, element)));
        }
        return new JQueryCollection(this.elements.filter((element) => matchesSelectorToken(element, selector)));
    }

    closest(selector) {
        const matches = [];
        this.elements.forEach((element) => {
            let current = element;
            while (current) {
                if (matchesSelectorToken(current, selector)) {
                    matches.push(current);
                    break;
                }
                current = current.parentNode;
            }
        });
        return new JQueryCollection(matches);
    }

    get(index) {
        return this.elements[index];
    }

    hasClass(className) {
        return this.elements.some((element) => element.classList.contains(className));
    }

    is(selector) {
        return this.elements.some((element) => matchesSelectorToken(element, selector));
    }

    submit() {
        return this.each((index, element) => {
            if (typeof element.submit === 'function') {
                element.submit();
            }
        });
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
        if (Array.isArray(selector)) {
            return new JQueryCollection(selector);
        }
        if (selector === document) {
            return new JQueryCollection([document]);
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
            .map((entry) => {
                return `${encodeURIComponent(entry.name)}=${encodeURIComponent(entry.value == null ? '' : entry.value)}`;
            })
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

module.exports = {
    FakeDocument,
    FakeElement,
    FakeTimerQueue,
    JQueryCollection,
    createJQuery,
    createLocation,
    createTextNode,
    matchesSelectorToken,
    selectElements,
    splitClassNames
};
