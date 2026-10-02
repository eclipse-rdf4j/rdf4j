// WARNING: Do not edit the *.js version of this file. Instead, always edit the
// corresponding *.ts source in the ts subfolder, and then invoke the
// compileTypescript.sh bash script to generate new *.js and *.js.map files.
var __makeTemplateObject = (this && this.__makeTemplateObject) || function (cooked, raw) {
    if (Object.defineProperty) { Object.defineProperty(cooked, "raw", { value: raw }); } else { cooked.raw = raw; }
    return cooked;
};
var workbench;
(function (workbench) {
    /** Shared settings/action disclosure markup and controller entry point. */
    var detailDisclosure;
    (function (detailDisclosure) {
        var chevronPath = 'm6 9 6 6 6-6';
        function classes(base, extra) {
            return extra ? base + ' ' + extra : base;
        }
        function renderToggle(html, options) {
            var expanded = !!options.expanded;
            return html(__makeTemplateObject(["<button id=", " type=\"button\"\n                    class=", "\n                    aria-controls=", " aria-expanded=", "\n                    aria-label=", " ?hidden=", ">\n                    ", "<span class=\"workbench-disclosure__toggle-label\">", "</span>\n                    <svg class=\"workbench-action-icon workbench-action-icon--chevron workbench-disclosure-chevron\"\n                        data-workbench-icon=\"chevron\" viewBox=\"0 0 24 24\" width=\"16\" height=\"16\"\n                        focusable=\"false\" aria-hidden=\"true\"><path d=", "></path></svg>\n                </button>"], ["<button id=", " type=\"button\"\n                    class=", "\n                    aria-controls=", " aria-expanded=", "\n                    aria-label=", " ?hidden=", ">\n                    ", "<span class=\"workbench-disclosure__toggle-label\">", "</span>\n                    <svg class=\"workbench-action-icon workbench-action-icon--chevron workbench-disclosure-chevron\"\n                        data-workbench-icon=\"chevron\" viewBox=\"0 0 24 24\" width=\"16\" height=\"16\"\n                        focusable=\"false\" aria-hidden=\"true\"><path d=", "></path></svg>\n                </button>"]), options.toggleId, classes('workbench-disclosure__toggle', options.toggleClass || ''), options.panelId, expanded ? 'true' : 'false', options.accessibleName || options.label, !!options.toggleHidden, options.icon || '', options.label, chevronPath);
        }
        function renderPanel(html, options, content) {
            return html(__makeTemplateObject(["<div id=", "\n                    class=", "\n                    role=", " aria-labelledby=", "\n                    ?hidden=", ">\n                    <div class=", ">", "</div>\n                </div>"], ["<div id=", "\n                    class=", "\n                    role=", " aria-labelledby=", "\n                    ?hidden=", ">\n                    <div class=", ">", "</div>\n                </div>"]), options.panelId, classes('workbench-disclosure__panel', options.panelClass || ''), options.panelRole || 'region', options.toggleId, !options.expanded, classes('workbench-disclosure__content', options.contentClass || ''), content);
        }
        function renderOwner(html, options, content) {
            return html(__makeTemplateObject(["<div id=", "\n                    class=", "\n                    data-workbench-detail-disclosure=\"true\" ?hidden=", ">", "</div>"], ["<div id=", "\n                    class=", "\n                    data-workbench-detail-disclosure=\"true\" ?hidden=", ">", "</div>"]), options.id || '', classes('workbench-disclosure', options.ownerClass || ''), !!options.hidden, content);
        }
        function render(html, options, content) {
            return renderOwner(html, options, html(__makeTemplateObject(["", "", ""], ["", "", ""]), renderToggle(html, options), renderPanel(html, options, content)));
        }
        detailDisclosure.render = render;
        /** Keep triggers in their action group and associated panels in a shared full-width track. */
        function renderSeparated(html, options, content) {
            return {
                owner: renderOwner(html, options, renderToggle(html, options)),
                panel: renderPanel(html, options, content)
            };
        }
        detailDisclosure.renderSeparated = renderSeparated;
        function create(doc, options) {
            var owner = doc.createElement('div');
            owner.className = classes('workbench-disclosure', options.ownerClass || '');
            owner.setAttribute('data-workbench-detail-disclosure', 'true');
            owner.hidden = !!options.hidden;
            if (options.id) {
                owner.id = options.id;
            }
            var toggle = doc.createElement('button');
            toggle.type = 'button';
            toggle.id = options.toggleId;
            toggle.className = classes('workbench-disclosure__toggle', options.toggleClass || '');
            toggle.hidden = !!options.toggleHidden;
            toggle.setAttribute('aria-controls', options.panelId);
            toggle.setAttribute('aria-expanded', options.expanded ? 'true' : 'false');
            toggle.setAttribute('aria-label', options.accessibleName || options.label);
            var label = doc.createElement('span');
            label.className = 'workbench-disclosure__toggle-label';
            label.textContent = options.label;
            toggle.appendChild(label);
            var namespace = 'http://www.w3.org/2000/svg';
            var chevron = doc.createElementNS(namespace, 'svg');
            chevron.setAttribute('class', 'workbench-action-icon workbench-action-icon--chevron workbench-disclosure-chevron');
            chevron.setAttribute('data-workbench-icon', 'chevron');
            chevron.setAttribute('viewBox', '0 0 24 24');
            chevron.setAttribute('width', '16');
            chevron.setAttribute('height', '16');
            chevron.setAttribute('focusable', 'false');
            chevron.setAttribute('aria-hidden', 'true');
            var path = doc.createElementNS(namespace, 'path');
            path.setAttribute('d', chevronPath);
            chevron.appendChild(path);
            toggle.appendChild(chevron);
            owner.appendChild(toggle);
            var panel = doc.createElement('div');
            panel.id = options.panelId;
            panel.className = classes('workbench-disclosure__panel', options.panelClass || '');
            panel.setAttribute('role', options.panelRole || 'region');
            panel.setAttribute('aria-labelledby', options.toggleId);
            panel.hidden = !options.expanded;
            owner.appendChild(panel);
            var panelContent = doc.createElement('div');
            panelContent.className = classes('workbench-disclosure__content', options.contentClass || '');
            panel.appendChild(panelContent);
            return { owner: owner, toggle: toggle, panel: panel, content: panelContent };
        }
        detailDisclosure.create = create;
        function bind(toggle, panel, owner) {
            if (!toggle || !panel || toggle.getAttribute('data-workbench-bound') === 'true') {
                return function () { };
            }
            toggle.setAttribute('data-workbench-bound', 'true');
            var disclosureOwner = owner || toggle.parentElement;
            var initiallyExpanded = toggle.getAttribute('aria-expanded') === 'true';
            workbench.setDisclosureExpanded(toggle, panel, disclosureOwner, initiallyExpanded, false);
            var onClick = function () {
                workbench.setDisclosureExpanded(toggle, panel, disclosureOwner, toggle.getAttribute('aria-expanded') !== 'true', true);
            };
            toggle.addEventListener('click', onClick, false);
            var disposed = false;
            return function () {
                if (disposed) {
                    return;
                }
                disposed = true;
                toggle.removeEventListener('click', onClick, false);
                toggle.removeAttribute('data-workbench-bound');
                workbench.releaseDisclosure(toggle, panel, disclosureOwner);
            };
        }
        detailDisclosure.bind = bind;
        function bindOwner(owner) {
            if (!owner) {
                return function () { };
            }
            var toggle = owner.querySelector('.workbench-disclosure__toggle');
            var panelId = toggle ? toggle.getAttribute('aria-controls') : null;
            var panel = panelId && owner.ownerDocument ? owner.ownerDocument.getElementById(panelId) : null;
            return bind(toggle, panel, owner);
        }
        detailDisclosure.bindOwner = bindOwner;
        /** Bind every disclosure under root; the returned function releases the ones this call bound. */
        function bindAll(root) {
            var scope = root || document;
            if (!scope || !scope.querySelectorAll) {
                return function () { };
            }
            var owners = scope.querySelectorAll('[data-workbench-detail-disclosure="true"]');
            var disposers = [];
            for (var i = 0; i < owners.length; i++) {
                disposers.push(bindOwner(owners[i]));
            }
            return function () {
                for (var j = 0; j < disposers.length; j++) {
                    disposers[j]();
                }
            };
        }
        detailDisclosure.bindAll = bindAll;
    })(detailDisclosure = workbench.detailDisclosure || (workbench.detailDisclosure = {}));
    /** Number formatting shared by the Workbench pages. */
    var format;
    (function (format) {
        /**
         * Formats an integer (number or digit string) with Intl.NumberFormat for the given locale
         * (the browser locale when omitted); any other value is returned unchanged as text.
         */
        function count(value, locale) {
            if (value === null || typeof value === 'undefined') {
                return '';
            }
            var text = String(value);
            var trimmed = text.trim();
            if (!/^-?\d+$/.test(trimmed)) {
                return text;
            }
            var number = Number(trimmed);
            if (!isFinite(number) || Math.abs(number) > 9007199254740991) {
                return text;
            }
            return new Intl.NumberFormat(locale).format(number);
        }
        format.count = count;
    })(format = workbench.format || (workbench.format = {}));
    /**
     * Anchored popover panels opened by a button (the context bar switchers). Only one popover is
     * open at a time; Escape or a click outside closes it and Escape returns focus to its button.
     */
    /** Accessible tabs: one selected tab in the Tab order, arrow keys move between tabs, panels follow. */
    var tabs;
    (function (tabs) {
        function tabsOf(tablist) {
            return Array.prototype.slice.call(tablist.querySelectorAll('[role="tab"]'))
                .filter(function (tab) {
                return !tab.hidden;
            });
        }
        /** Select a tab, show its panel and hide the panels of the other tabs in the same list. */
        function select(tab, focus) {
            var tablist = tab && tab.closest ? tab.closest('[role="tablist"]') : null;
            if (!tablist) {
                return;
            }
            var doc = tab.ownerDocument;
            Array.prototype.slice.call(tablist.querySelectorAll('[role="tab"]')).forEach(function (other) {
                var selected = other === tab;
                other.setAttribute('aria-selected', selected ? 'true' : 'false');
                other.tabIndex = selected ? 0 : -1;
                var panel = doc.getElementById(other.getAttribute('aria-controls') || '');
                if (panel) {
                    panel.hidden = !selected;
                }
            });
            if (focus) {
                tab.focus();
            }
        }
        tabs.select = select;
        function bind(tablist) {
            if (!tablist || tablist.getAttribute('data-workbench-tabs') === 'bound') {
                return;
            }
            tablist.setAttribute('data-workbench-tabs', 'bound');
            tablist.addEventListener('click', function (event) {
                var tab = event.target.closest('[role="tab"]');
                if (tab && tablist.contains(tab)) {
                    select(tab);
                }
            });
            tablist.addEventListener('keydown', function (event) {
                var available = tabsOf(tablist);
                var current = available.indexOf(event.target.closest('[role="tab"]'));
                if (current < 0) {
                    return;
                }
                var next = -1;
                if (event.key === 'ArrowRight') {
                    next = (current + 1) % available.length;
                }
                else if (event.key === 'ArrowLeft') {
                    next = (current - 1 + available.length) % available.length;
                }
                else if (event.key === 'Home') {
                    next = 0;
                }
                else if (event.key === 'End') {
                    next = available.length - 1;
                }
                if (next < 0) {
                    return;
                }
                event.preventDefault();
                select(available[next], true);
            });
        }
        tabs.bind = bind;
    })(tabs = workbench.tabs || (workbench.tabs = {}));
    /**
     * A modal confirmation (M6.2): a native dialog with a title, a body, an optional typed confirmation,
     * Cancel (focused first) and the confirm button. Escape and Cancel resolve false; only confirming resolves true.
     */
    var confirmDialog;
    (function (confirmDialog) {
        var counter = 0;
        function renderBody(container, body) {
            var lit = window.RDF4JLitHTML;
            if (body && typeof body === 'object' && lit && typeof lit.render === 'function') {
                lit.render(body, container);
            }
            else {
                var paragraph = container.ownerDocument.createElement('p');
                paragraph.textContent = body === null || body === undefined ? '' : String(body);
                container.appendChild(paragraph);
            }
        }
        function button(doc, className, label) {
            var element = doc.createElement('button');
            element.type = 'button';
            element.className = className;
            element.textContent = label;
            return element;
        }
        function open(options) {
            var doc = document;
            var id = 'workbench-confirm-' + (++counter);
            var dialog = doc.createElement('dialog');
            dialog.className = 'workbench-dialog';
            dialog.setAttribute('aria-labelledby', id + '-title');
            dialog.setAttribute('aria-describedby', id + '-body');
            var title = doc.createElement('h2');
            title.id = id + '-title';
            title.className = 'workbench-dialog__title';
            title.textContent = options.title;
            var body = doc.createElement('div');
            body.id = id + '-body';
            body.className = 'workbench-dialog__body';
            dialog.appendChild(title);
            dialog.appendChild(body);
            renderBody(body, options.body);
            var input = null;
            if (options.requireText) {
                var field = doc.createElement('div');
                field.className = 'workbench-field workbench-dialog__field';
                var label = doc.createElement('label');
                label.setAttribute('for', id + '-input');
                label.textContent = options.requireLabel || 'Type ' + options.requireText + ' to confirm';
                input = doc.createElement('input');
                input.id = id + '-input';
                input.type = 'text';
                input.setAttribute('autocomplete', 'off');
                input.setAttribute('spellcheck', 'false');
                input.setAttribute('data-workbench-confirm-text', options.requireText);
                field.appendChild(label);
                field.appendChild(input);
                dialog.appendChild(field);
            }
            var actions = doc.createElement('div');
            actions.className = 'workbench-dialog__actions';
            var cancel = button(doc, 'workbench-action workbench-action--secondary', 'Cancel');
            var confirm = button(doc, 'workbench-action ' + (options.danger ? 'workbench-action--danger' : 'workbench-action--primary'), options.confirmLabel);
            actions.appendChild(cancel);
            actions.appendChild(confirm);
            dialog.appendChild(actions);
            var sync = function () {
                confirm.disabled = !!input && input.value !== options.requireText;
            };
            sync();
            return new Promise(function (resolve) {
                var confirmed = false;
                if (input) {
                    input.addEventListener('input', sync);
                    input.addEventListener('keydown', function (event) {
                        if (event.key === 'Enter') {
                            event.preventDefault();
                            confirm.click();
                        }
                    });
                }
                cancel.addEventListener('click', function () {
                    dialog.close();
                });
                confirm.addEventListener('click', function () {
                    if (!confirm.disabled) {
                        confirmed = true;
                        dialog.close();
                    }
                });
                dialog.addEventListener('close', function () {
                    if (dialog.parentNode) {
                        dialog.parentNode.removeChild(dialog);
                    }
                    resolve(confirmed);
                });
                doc.body.appendChild(dialog);
                dialog.showModal();
                cancel.focus();
            });
        }
        confirmDialog.open = open;
    })(confirmDialog = workbench.confirmDialog || (workbench.confirmDialog = {}));
    var popover;
    (function (popover) {
        var closeOpenPopover = null;
        function focusableIn(panel) {
            return panel.querySelector('input:not([disabled]):not([hidden]), a[href], button:not([disabled]):not([hidden]), [tabindex="0"]');
        }
        function closeAll() {
            if (closeOpenPopover) {
                closeOpenPopover(false);
            }
        }
        popover.closeAll = closeAll;
        function bind(button, panel, options) {
            if (!button || !panel || button.getAttribute('data-workbench-popover-bound') === 'true') {
                return function () { };
            }
            button.setAttribute('data-workbench-popover-bound', 'true');
            var document = button.ownerDocument;
            var settings = options || {};
            var isOpen = function () {
                return button.getAttribute('aria-expanded') === 'true';
            };
            var onDocumentPointer = function (event) {
                var target = event.target;
                if (!panel.contains(target) && !button.contains(target)) {
                    close(false);
                }
            };
            var onDocumentKey = function (event) {
                if (event.key === 'Escape' && isOpen()) {
                    event.preventDefault();
                    close(true);
                }
            };
            var close = function (returnFocus) {
                if (!isOpen()) {
                    return;
                }
                button.setAttribute('aria-expanded', 'false');
                panel.hidden = true;
                document.removeEventListener('pointerdown', onDocumentPointer, true);
                document.removeEventListener('keydown', onDocumentKey, true);
                if (closeOpenPopover === close) {
                    closeOpenPopover = null;
                }
                if (returnFocus) {
                    button.focus();
                }
            };
            var open = function () {
                if (isOpen()) {
                    return;
                }
                closeAll();
                button.setAttribute('aria-expanded', 'true');
                panel.hidden = false;
                closeOpenPopover = close;
                document.addEventListener('pointerdown', onDocumentPointer, true);
                document.addEventListener('keydown', onDocumentKey, true);
                if (settings.onOpen) {
                    settings.onOpen(panel);
                }
                var first = focusableIn(panel);
                if (first) {
                    first.focus();
                }
            };
            var onClick = function (event) {
                event.preventDefault();
                if (isOpen()) {
                    close(false);
                }
                else {
                    open();
                }
            };
            var onButtonKey = function (event) {
                if (event.key === 'ArrowDown' && !isOpen()) {
                    event.preventDefault();
                    open();
                }
            };
            button.addEventListener('click', onClick, false);
            button.addEventListener('keydown', onButtonKey, false);
            return function () {
                close(false);
                button.removeEventListener('click', onClick, false);
                button.removeEventListener('keydown', onButtonKey, false);
                button.removeAttribute('data-workbench-popover-bound');
            };
        }
        popover.bind = bind;
    })(popover = workbench.popover || (workbench.popover = {}));
    var requestIdCounter = 0;
    var motionDisclosureDuration = 180;
    var motionLayoutDuration = 220;
    var motionEasing = 'cubic-bezier(0.2, 0.7, 0.2, 1)';
    var ownedMotions = [];
    var nativeDisclosureStates = [];
    var panelDisclosureStates = [];
    var disclosureAnchors = [];
    var disclosureAnchorObserver = null;
    var disclosureAnchorResizeListenerInstalled = false;
    var disclosureAnchorRefreshPending = false;
    var disclosureDismissInstalled = false;
    /** The cards a pane that opens over the page stays inside (M14.3). */
    var disclosureBoundsSelector = '.workbench-island, .workbench-form-card, .query-output, .workbench-page-surface';
    function inlineStyleValue(element, property) {
        var style = element.style;
        return style && style.getPropertyValue ? style.getPropertyValue(property) : style ? style[property] || '' : '';
    }
    function inlineStylePriority(element, property) {
        var style = element.style;
        return style && style.getPropertyPriority ? style.getPropertyPriority(property) : '';
    }
    function captureMotionStyles(element) {
        return {
            height: inlineStyleValue(element, 'height'),
            heightPriority: inlineStylePriority(element, 'height'),
            overflow: inlineStyleValue(element, 'overflow'),
            overflowPriority: inlineStylePriority(element, 'overflow'),
            opacity: inlineStyleValue(element, 'opacity'),
            opacityPriority: inlineStylePriority(element, 'opacity')
        };
    }
    function restoreMotionStyles(element, styles) {
        var style = element.style;
        if (!style) {
            return;
        }
        if (styles.height) {
            if (style.setProperty) {
                style.setProperty('height', styles.height, styles.heightPriority);
            }
            else {
                style.height = styles.height;
            }
        }
        else {
            if (style.removeProperty) {
                style.removeProperty('height');
            }
            else {
                style.height = '';
            }
        }
        if (styles.overflow) {
            if (style.setProperty) {
                style.setProperty('overflow', styles.overflow, styles.overflowPriority);
            }
            else {
                style.overflow = styles.overflow;
            }
        }
        else {
            if (style.removeProperty) {
                style.removeProperty('overflow');
            }
            else {
                style.overflow = '';
            }
        }
        if (styles.opacity) {
            if (style.setProperty) {
                style.setProperty('opacity', styles.opacity, styles.opacityPriority);
            }
            else {
                style.opacity = styles.opacity;
            }
        }
        else {
            if (style.removeProperty) {
                style.removeProperty('opacity');
            }
            else {
                style.opacity = '';
            }
        }
    }
    function elementHeight(element) {
        return element && element.getBoundingClientRect ? element.getBoundingClientRect().height : 0;
    }
    function cssHeightFromBorderBox(element, borderBoxHeight, style) {
        var computedStyle = style || window.getComputedStyle(element);
        if (computedStyle.boxSizing === 'border-box') {
            return borderBoxHeight;
        }
        var verticalInsets = parseFloat(computedStyle.paddingTop) + parseFloat(computedStyle.paddingBottom)
            + parseFloat(computedStyle.borderTopWidth) + parseFloat(computedStyle.borderBottomWidth);
        return Math.max(0, borderBoxHeight - verticalInsets);
    }
    function snapshotDisclosureBoxStyle(style) {
        return {
            boxSizing: style.boxSizing,
            paddingTop: style.paddingTop,
            paddingBottom: style.paddingBottom,
            borderTopWidth: style.borderTopWidth,
            borderBottomWidth: style.borderBottomWidth,
            marginBlockStart: style.marginBlockStart,
            marginBlockEnd: style.marginBlockEnd
        };
    }
    function disclosureBoxKeyframe(element, borderBoxHeight, style) {
        return {
            height: cssHeightFromBorderBox(element, borderBoxHeight, style) + 'px',
            paddingTop: style.paddingTop,
            paddingBottom: style.paddingBottom,
            borderTopWidth: style.borderTopWidth,
            borderBottomWidth: style.borderBottomWidth,
            marginBlockStart: style.marginBlockStart,
            marginBlockEnd: style.marginBlockEnd
        };
    }
    function collapsedDisclosureKeyframe() {
        return {
            height: '0px',
            paddingTop: '0px',
            paddingBottom: '0px',
            borderTopWidth: '0px',
            borderBottomWidth: '0px',
            marginBlockStart: '0px',
            marginBlockEnd: '0px'
        };
    }
    // The share of a disclosure's motion, at its collapsed end, in which its top and bottom borders are 0. Chromium
    // paints a border wider than 0 and narrower than 1 px as one device pixel, so borders that shrink over the whole
    // collapse would leave its last frames two borders tall, and the box would jump from there to hidden.
    var disclosureBorderlessProgress = 0.1;
    /**
     * Keyframes from startBox to endBox in which the top and bottom borders change only while the box is more than
     * disclosureBorderlessProgress away from collapsed: a collapse reaches endBox's borders there and an expansion
     * keeps startBox's borders until there, so an expansion mirrors a collapse. Height, padding and margin change
     * over the whole motion.
     */
    function disclosureKeyframes(startBox, endBox, expanding) {
        var borderBox = expanding ? startBox : endBox;
        var borders = {
            offset: expanding ? disclosureBorderlessProgress : 1 - disclosureBorderlessProgress,
            borderTopWidth: borderBox.borderTopWidth,
            borderBottomWidth: borderBox.borderBottomWidth
        };
        return [startBox, borders, endBox];
    }
    function dispatchWorkbenchResize() {
        if (window.dispatchEvent && typeof Event !== 'undefined') {
            window.dispatchEvent(new Event('resize'));
        }
    }
    function motionFor(element) {
        for (var i = 0; i < ownedMotions.length; i++) {
            if (ownedMotions[i].element === element) {
                return ownedMotions[i];
            }
        }
        return null;
    }
    function removeOwnedMotion(motion) {
        var index = ownedMotions.indexOf(motion);
        if (index >= 0) {
            ownedMotions.splice(index, 1);
        }
    }
    function cancelOwnedMotion(element) {
        var motion = motionFor(element);
        if (!motion) {
            return null;
        }
        motion.animation.onfinish = null;
        motion.animation.cancel();
        removeOwnedMotion(motion);
        return motion;
    }
    function reducedMotionRequested() {
        return !!(window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches);
    }
    function startOwnedMotion(element, keyframes, duration, styles, complete) {
        if (reducedMotionRequested() || !element.animate) {
            complete();
            restoreMotionStyles(element, styles);
            return;
        }
        var animation = element.animate(keyframes, {
            duration: duration,
            easing: motionEasing,
            fill: 'forwards'
        });
        var motion = {
            element: element,
            animation: animation,
            styles: styles,
            complete: complete
        };
        ownedMotions.push(motion);
        animation.onfinish = function () {
            if (motionFor(element) !== motion) {
                return;
            }
            animation.onfinish = null;
            removeOwnedMotion(motion);
            try {
                complete();
            }
            finally {
                animation.cancel();
                restoreMotionStyles(element, styles);
            }
        };
    }
    function settleOwnedMotions() {
        var pending = ownedMotions.slice(0);
        for (var i = 0; i < pending.length; i++) {
            var motion = pending[i];
            if (motionFor(motion.element) !== motion) {
                continue;
            }
            motion.animation.onfinish = null;
            motion.animation.cancel();
            removeOwnedMotion(motion);
            motion.complete();
            restoreMotionStyles(motion.element, motion.styles);
        }
    }
    function installReducedMotionListener() {
        if (!window.matchMedia) {
            return;
        }
        var preference = window.matchMedia('(prefers-reduced-motion: reduce)');
        var settleWhenReduced = function (event) {
            if (event.matches) {
                settleOwnedMotions();
            }
        };
        if (preference.addEventListener) {
            preference.addEventListener('change', settleWhenReduced);
        }
        else if (preference.addListener) {
            preference.addListener(settleWhenReduced);
        }
    }
    function nativeDisclosureState(details) {
        for (var i = 0; i < nativeDisclosureStates.length; i++) {
            if (nativeDisclosureStates[i].details === details) {
                return nativeDisclosureStates[i];
            }
        }
        return null;
    }
    function applyNativeDisclosureContent(state, expanded) {
        for (var i = 0; i < state.content.length; i++) {
            var content = state.content[i];
            content.element.inert = expanded ? content.inert : true;
            if (expanded && content.ariaHidden === null) {
                content.element.setAttribute('aria-hidden', 'false');
            }
            else {
                content.element.setAttribute('aria-hidden', expanded ? content.ariaHidden : 'true');
            }
        }
    }
    function collapsedDisclosureHeight(state) {
        var detailsStyle = window.getComputedStyle(state.details);
        var summaryStyle = window.getComputedStyle(state.summary);
        var summaryMargins = parseFloat(summaryStyle.marginTop) + parseFloat(summaryStyle.marginBottom);
        var collapsedBorderBoxHeight = elementHeight(state.summary) + summaryMargins
            + parseFloat(detailsStyle.paddingTop) + parseFloat(detailsStyle.paddingBottom)
            + parseFloat(detailsStyle.borderTopWidth) + parseFloat(detailsStyle.borderBottomWidth);
        return cssHeightFromBorderBox(state.details, collapsedBorderBoxHeight, detailsStyle);
    }
    function setNativeDisclosureOpen(details, expanded, animate) {
        var state = nativeDisclosureState(details);
        if (!state) {
            installNativeDisclosure(details);
            state = nativeDisclosureState(details);
        }
        if (!state || details.hidden || details.inert) {
            return;
        }
        var shouldAnimate = animate !== false;
        var existingMotion = motionFor(details);
        if (state.requestedOpen === expanded && !existingMotion) {
            details.open = expanded;
            state.summary.setAttribute('aria-expanded', expanded ? 'true' : 'false');
            applyNativeDisclosureContent(state, expanded);
            return;
        }
        var detailsStyle = window.getComputedStyle(details);
        var startHeight = cssHeightFromBorderBox(details, elementHeight(details), detailsStyle);
        var motion = cancelOwnedMotion(details);
        var styles = motion ? motion.styles : captureMotionStyles(details);
        state.requestedOpen = expanded;
        state.summary.setAttribute('aria-expanded', expanded ? 'true' : 'false');
        if (expanded) {
            details.open = true;
            applyNativeDisclosureContent(state, true);
        }
        else {
            applyNativeDisclosureContent(state, false);
        }
        if (details.classList.contains('workbench-nav-group__disclosure') && expanded) {
            var siblingGroups = document.querySelectorAll('#navigation .workbench-nav-group__disclosure');
            for (var i = 0; i < siblingGroups.length; i++) {
                var sibling = siblingGroups[i];
                var siblingState = nativeDisclosureState(sibling);
                if (sibling !== details && siblingState && siblingState.requestedOpen) {
                    setNativeDisclosureOpen(sibling, false, shouldAnimate);
                }
            }
        }
        if (!shouldAnimate) {
            details.open = expanded;
            restoreMotionStyles(details, styles);
            return;
        }
        var endHeight = expanded
            ? cssHeightFromBorderBox(details, elementHeight(details), window.getComputedStyle(details))
            : collapsedDisclosureHeight(state);
        if (!details.open && !expanded) {
            restoreMotionStyles(details, styles);
            return;
        }
        details.style.overflow = 'hidden';
        startOwnedMotion(details, [
            { height: startHeight + 'px' },
            { height: endHeight + 'px' }
        ], motionDisclosureDuration, styles, function () {
            details.open = expanded;
            state.summary.setAttribute('aria-expanded', expanded ? 'true' : 'false');
            applyNativeDisclosureContent(state, expanded);
        });
    }
    workbench.setNativeDisclosureOpen = setNativeDisclosureOpen;
    function installNativeDisclosure(details) {
        if (!details || nativeDisclosureState(details)) {
            return;
        }
        var summary = null;
        for (var i = 0; i < details.children.length; i++) {
            if (details.children[i].tagName.toLowerCase() === 'summary') {
                summary = details.children[i];
                break;
            }
        }
        if (!summary) {
            return;
        }
        var content = [];
        for (var j = 0; j < details.children.length; j++) {
            var child = details.children[j];
            if (child === summary) {
                continue;
            }
            content.push({
                element: child,
                inert: !!child.inert,
                ariaHidden: child.getAttribute('aria-hidden')
            });
        }
        var state = {
            details: details,
            summary: summary,
            content: content,
            requestedOpen: details.open
        };
        nativeDisclosureStates.push(state);
        details.setAttribute('data-workbench-motion-ready', 'true');
        summary.setAttribute('aria-expanded', details.open ? 'true' : 'false');
        applyNativeDisclosureContent(state, details.open);
        state.onClick = function (event) {
            event.preventDefault();
            setNativeDisclosureOpen(details, !state.requestedOpen, true);
        };
        summary.addEventListener('click', state.onClick, true);
    }
    workbench.installNativeDisclosure = installNativeDisclosure;
    /** Forget an installed native disclosure and remove its summary listener (a route that is left). */
    function releaseNativeDisclosure(details) {
        var state = nativeDisclosureState(details);
        if (!state) {
            return;
        }
        nativeDisclosureStates.splice(nativeDisclosureStates.indexOf(state), 1);
        state.summary.removeEventListener('click', state.onClick, true);
        details.removeAttribute('data-workbench-motion-ready');
    }
    workbench.releaseNativeDisclosure = releaseNativeDisclosure;
    function panelDisclosureState(button, panel, owner) {
        for (var i = 0; i < panelDisclosureStates.length; i++) {
            if (panelDisclosureStates[i].panel === panel) {
                return panelDisclosureStates[i];
            }
        }
        var initialAriaHidden = panel.getAttribute('aria-hidden');
        var state = {
            button: button,
            panel: panel,
            owner: owner,
            inert: !!panel.inert,
            ariaHidden: typeof initialAriaHidden === 'undefined' ? null : initialAriaHidden,
            expanded: button.getAttribute('aria-expanded') === 'true'
        };
        panelDisclosureStates.push(state);
        return state;
    }
    function disclosureAnchorTrack(panel) {
        if (!panel || !panel.closest) {
            return null;
        }
        return panel.closest('.workbench-disclosure-track, .query-actions-toolbar, .query-result-toolbar__disclosures')
            || panel.closest('.workbench-disclosure');
    }
    function disclosureBounds(panel) {
        return panel.closest(disclosureBoundsSelector);
    }
    /**
     * The horizontal room a pane has: the viewport less 8 px on either side, its card's content box, and the padding
     * box of every element between them that clips what overflows it sideways (the result area does).
     */
    function disclosureRoom(panel, bounds) {
        var room = { left: 8, right: document.documentElement.clientWidth - 8 };
        for (var box = panel.parentNode; box && box.getBoundingClientRect; box = box === bounds ? null : box.parentNode) {
            var style = window.getComputedStyle(box);
            var card = box === bounds;
            if (card || /^(clip|hidden|auto|scroll)$/.test(style.overflowX)) {
                var rect = box.getBoundingClientRect();
                room.left = Math.max(room.left, rect.left + parseFloat(style.borderLeftWidth)
                    + (card ? parseFloat(style.paddingLeft) : 0));
                room.right = Math.min(room.right, rect.right - parseFloat(style.borderRightWidth)
                    - (card ? parseFloat(style.paddingRight) : 0));
            }
        }
        return room;
    }
    /**
     * Place an open pane over the page (M14.3): just below its button, or below the whole toolbar the button is in
     * so that it never covers the toolbar's other buttons, with its end at the button's end (its start in
     * right-to-left text), moved inward to stay inside its card, and no wider than the card's content. The
     * stylesheet positions the panel absolutely from these values, so opening it moves nothing.
     */
    function refreshDisclosureAnchor(button, panel, bounds) {
        var frame = panel.offsetParent;
        if (!frame || panel.hidden || button.hidden || button.disabled || button.getClientRects().length === 0) {
            return;
        }
        var room = disclosureRoom(panel, bounds);
        panel.style.setProperty('--workbench-disclosure-max-width', Math.max(0, room.right - room.left) + 'px');
        var buttonRect = button.getBoundingClientRect();
        var width = panel.getBoundingClientRect().width;
        var start = window.getComputedStyle(panel).direction === 'rtl' ? buttonRect.left : buttonRect.right - width;
        var left = Math.max(room.left, Math.min(room.right - width, start));
        var toolbar = button.closest('.workbench-action-toolbar');
        var bottom = toolbar ? toolbar.getBoundingClientRect().bottom : buttonRect.bottom;
        var frameRect = frame.getBoundingClientRect();
        panel.style.setProperty('--workbench-disclosure-left', (left - frameRect.left - frame.clientLeft + frame.scrollLeft) + 'px');
        panel.style.setProperty('--workbench-disclosure-top', (bottom - frameRect.top - frame.clientTop + frame.scrollTop) + 'px');
        var anchorOffset = Math.max(4, Math.min(width - 4, (buttonRect.left + buttonRect.right) / 2 - left));
        panel.style.setProperty('--workbench-disclosure-anchor-x', anchorOffset + 'px');
    }
    function refreshDisclosureAnchors() {
        disclosureAnchorRefreshPending = false;
        for (var i = 0; i < disclosureAnchors.length; i++) {
            placeDisclosure(disclosureAnchors[i]);
        }
    }
    /** Stop watching an anchor's card unless another pane is in it too. */
    function unwatchDisclosureBounds(anchor) {
        var bounds = anchor.bounds;
        anchor.bounds = null;
        for (var i = 0; i < disclosureAnchors.length; i++) {
            if (disclosureAnchors[i].bounds === bounds) {
                return;
            }
        }
        if (bounds && disclosureAnchorObserver) {
            disclosureAnchorObserver.unobserve(bounds);
        }
    }
    /**
     * Place a pane, and watch the card it is in now: a card that grows (a taller editor, a new callout) moves the
     * button the pane hangs from, and a pane may be bound before it is placed in its card.
     */
    function placeDisclosure(anchor) {
        var bounds = disclosureBounds(anchor.panel);
        if (bounds !== anchor.bounds) {
            unwatchDisclosureBounds(anchor);
            anchor.bounds = bounds;
            if (bounds && disclosureAnchorObserver) {
                disclosureAnchorObserver.observe(bounds);
            }
        }
        refreshDisclosureAnchor(anchor.button, anchor.panel, bounds);
    }
    function scheduleDisclosureAnchorRefresh() {
        if (disclosureAnchorRefreshPending) {
            return;
        }
        disclosureAnchorRefreshPending = true;
        if (window.requestAnimationFrame) {
            window.requestAnimationFrame(refreshDisclosureAnchors);
        }
        else {
            refreshDisclosureAnchors();
        }
    }
    function registerDisclosureAnchor(button, panel) {
        for (var i = 0; i < disclosureAnchors.length; i++) {
            if (disclosureAnchors[i].panel === panel) {
                return disclosureAnchors[i];
            }
        }
        var anchor = { button: button, panel: panel, bounds: null };
        disclosureAnchors.push(anchor);
        if (!disclosureAnchorResizeListenerInstalled) {
            window.addEventListener('resize', scheduleDisclosureAnchorRefresh);
            disclosureAnchorResizeListenerInstalled = true;
        }
        if (!disclosureAnchorObserver && window.ResizeObserver) {
            var ResizeObserverConstructor = window.ResizeObserver;
            disclosureAnchorObserver = new ResizeObserverConstructor(scheduleDisclosureAnchorRefresh);
        }
        if (disclosureAnchorObserver) {
            disclosureAnchorObserver.observe(button);
            disclosureAnchorObserver.observe(panel);
        }
        placeDisclosure(anchor);
        return anchor;
    }
    function expandedDisclosures() {
        return panelDisclosureStates.filter(function (state) {
            return state.expanded;
        });
    }
    /** A press outside an open pane and its toggle closes the pane (M14.3). */
    function dismissDisclosuresOutside(event) {
        var target = event.target;
        expandedDisclosures().forEach(function (state) {
            if (!state.panel.contains(target) && !state.button.contains(target)) {
                setDisclosureExpanded(state.button, state.panel, state.owner, false, true);
            }
        });
    }
    /**
     * Escape in an open pane, or on its toggle, closes the pane; focus returns to the toggle (M14.3). Safari does not
     * focus a button that is clicked, so focus on an element around the toggle (the page, the outlet) counts as well.
     */
    function dismissDisclosureOnEscape(event) {
        if (event.key !== 'Escape' || event.defaultPrevented) {
            return;
        }
        var focused = document.activeElement;
        expandedDisclosures().forEach(function (state) {
            if (state.panel.contains(focused) || !!focused && focused.contains(state.button)) {
                event.preventDefault();
                setDisclosureExpanded(state.button, state.panel, state.owner, false, true);
                state.button.focus();
            }
        });
    }
    /** Listen for presses and Escape only while a pane is open. */
    function syncDisclosureDismiss() {
        var listen = expandedDisclosures().length > 0;
        if (listen === disclosureDismissInstalled) {
            return;
        }
        disclosureDismissInstalled = listen;
        if (listen) {
            document.addEventListener('pointerdown', dismissDisclosuresOutside, true);
            document.addEventListener('keydown', dismissDisclosureOnEscape, false);
        }
        else {
            document.removeEventListener('pointerdown', dismissDisclosuresOutside, true);
            document.removeEventListener('keydown', dismissDisclosureOnEscape, false);
        }
    }
    function releaseDisclosure(button, panel, owner) {
        if (!panel) {
            return;
        }
        var motion = cancelOwnedMotion(panel);
        if (motion) {
            restoreMotionStyles(panel, motion.styles);
        }
        for (var stateIndex = panelDisclosureStates.length - 1; stateIndex >= 0; stateIndex--) {
            if (panelDisclosureStates[stateIndex].panel === panel) {
                panelDisclosureStates.splice(stateIndex, 1);
            }
        }
        for (var anchorIndex = disclosureAnchors.length - 1; anchorIndex >= 0; anchorIndex--) {
            var anchor = disclosureAnchors[anchorIndex];
            if (anchor.panel === panel || button && anchor.button === button) {
                if (disclosureAnchorObserver) {
                    disclosureAnchorObserver.unobserve(anchor.button);
                    disclosureAnchorObserver.unobserve(anchor.panel);
                }
                disclosureAnchors.splice(anchorIndex, 1);
                unwatchDisclosureBounds(anchor);
            }
        }
        if (disclosureAnchors.length === 0) {
            if (disclosureAnchorResizeListenerInstalled) {
                window.removeEventListener('resize', scheduleDisclosureAnchorRefresh);
                disclosureAnchorResizeListenerInstalled = false;
            }
            if (disclosureAnchorObserver) {
                disclosureAnchorObserver.disconnect();
                disclosureAnchorObserver = null;
            }
        }
        if (owner) {
            owner.classList.remove('is-open');
        }
        syncDisclosureDismiss();
    }
    workbench.releaseDisclosure = releaseDisclosure;
    /** Open or close a pane; an open pane listens for a press outside it and for Escape (M14.3). */
    function setDisclosureExpanded(button, panel, owner, expanded, animate) {
        applyDisclosureExpanded(button, panel, owner, expanded, animate);
        syncDisclosureDismiss();
    }
    workbench.setDisclosureExpanded = setDisclosureExpanded;
    function applyDisclosureExpanded(button, panel, owner, expanded, animate) {
        if (!button || !panel) {
            return;
        }
        var anchor = registerDisclosureAnchor(button, panel);
        var state = panelDisclosureState(button, panel, owner);
        if (button.hidden || button.disabled) {
            state.expanded = false;
            button.setAttribute('aria-expanded', 'false');
            panel.hidden = true;
            panel.inert = true;
            panel.setAttribute('aria-hidden', 'true');
            if (owner) {
                owner.classList.remove('is-open');
            }
            return;
        }
        var anchorTrack = disclosureAnchorTrack(panel);
        if (expanded && anchorTrack) {
            for (var disclosureIndex = 0; disclosureIndex < panelDisclosureStates.length; disclosureIndex++) {
                var siblingDisclosure = panelDisclosureStates[disclosureIndex];
                if (siblingDisclosure.panel !== panel
                    && disclosureAnchorTrack(siblingDisclosure.panel) === anchorTrack
                    && !siblingDisclosure.panel.hidden) {
                    // Remove the previous row synchronously so the new panel's
                    // connector never points across a closing disclosure.
                    setDisclosureExpanded(siblingDisclosure.button, siblingDisclosure.panel, siblingDisclosure.owner, false, false);
                }
            }
        }
        var existingMotion = motionFor(panel);
        if (state.expanded === expanded && !existingMotion) {
            button.setAttribute('aria-expanded', expanded ? 'true' : 'false');
            panel.hidden = !expanded;
            panel.inert = expanded ? state.inert : true;
            if (expanded && state.ariaHidden === null) {
                panel.setAttribute('aria-hidden', 'false');
            }
            else {
                panel.setAttribute('aria-hidden', expanded ? state.ariaHidden : 'true');
            }
            if (owner) {
                owner.classList.toggle('is-open', expanded);
            }
            if (expanded) {
                placeDisclosure(anchor);
            }
            return;
        }
        var startHeight = existingMotion ? elementHeight(panel) : (panel.hidden ? 0 : elementHeight(panel));
        // Copy the animated box values before canceling: getComputedStyle returns a live declaration, so
        // reading it after cancelOwnedMotion would use the unanimated styles instead of the current frame.
        var computedPanelStyle = snapshotDisclosureBoxStyle(window.getComputedStyle(panel));
        var motion = cancelOwnedMotion(panel);
        var styles = motion ? motion.styles : captureMotionStyles(panel);
        state.expanded = expanded;
        button.setAttribute('aria-expanded', expanded ? 'true' : 'false');
        if (owner) {
            owner.classList.toggle('is-open', expanded);
        }
        if (!expanded && panel.contains && panel.contains(document.activeElement)) {
            button.focus();
        }
        panel.hidden = false;
        if (expanded) {
            placeDisclosure(anchor);
        }
        panel.inert = expanded ? state.inert : true;
        if (expanded && state.ariaHidden === null) {
            panel.setAttribute('aria-hidden', 'false');
        }
        else {
            panel.setAttribute('aria-hidden', expanded ? state.ariaHidden : 'true');
        }
        var endHeight = expanded ? elementHeight(panel) : 0;
        var endPanelStyle = snapshotDisclosureBoxStyle(window.getComputedStyle(panel));
        dispatchWorkbenchResize();
        if (animate === false) {
            panel.hidden = !expanded;
            restoreMotionStyles(panel, styles);
            return;
        }
        panel.style.overflow = 'hidden';
        var startBox = expanded && !existingMotion
            ? collapsedDisclosureKeyframe()
            : disclosureBoxKeyframe(panel, startHeight, computedPanelStyle);
        var endBox = expanded
            ? disclosureBoxKeyframe(panel, endHeight, endPanelStyle)
            : collapsedDisclosureKeyframe();
        var keyframes = disclosureKeyframes(startBox, endBox, expanded);
        startOwnedMotion(panel, keyframes, motionDisclosureDuration, styles, function () {
            panel.hidden = !expanded;
            panel.inert = expanded ? state.inert : true;
            if (expanded && state.ariaHidden === null) {
                panel.setAttribute('aria-hidden', 'false');
            }
            else {
                panel.setAttribute('aria-hidden', expanded ? state.ariaHidden : 'true');
            }
        });
    }
    function setElementExpanded(element, expanded, animate) {
        if (!element || element.hidden) {
            return;
        }
        var existingMotion = motionFor(element);
        var wasExpanded = window.getComputedStyle(element).display !== 'none';
        if (wasExpanded === expanded && !existingMotion) {
            element.inert = !expanded;
            element.setAttribute('aria-hidden', expanded ? 'false' : 'true');
            return;
        }
        var startHeight = existingMotion ? elementHeight(element) : (wasExpanded ? elementHeight(element) : 0);
        var computedStartStyle = window.getComputedStyle(element);
        var startStyle = snapshotDisclosureBoxStyle(computedStartStyle);
        var startOpacity = existingMotion ? computedStartStyle.opacity : '0';
        var motion = cancelOwnedMotion(element);
        var styles = motion ? motion.styles : captureMotionStyles(element);
        element.style.display = '';
        element.inert = !expanded;
        element.setAttribute('aria-hidden', expanded ? 'false' : 'true');
        var endHeight = expanded ? elementHeight(element) : 0;
        var endStyle = snapshotDisclosureBoxStyle(window.getComputedStyle(element));
        if (animate === false || !expanded) {
            element.style.display = expanded ? '' : 'none';
            restoreMotionStyles(element, styles);
            return;
        }
        element.style.overflow = 'hidden';
        var startBox = existingMotion
            ? disclosureBoxKeyframe(element, startHeight, startStyle) : collapsedDisclosureKeyframe();
        startBox.opacity = startOpacity;
        var endBox = disclosureBoxKeyframe(element, endHeight, endStyle);
        endBox.opacity = window.getComputedStyle(element).opacity;
        var keyframes = disclosureKeyframes(startBox, endBox, true);
        startOwnedMotion(element, keyframes, motionDisclosureDuration, styles, function () {
            element.style.display = '';
        });
    }
    workbench.setElementExpanded = setElementExpanded;
    function animateElementOpacity(element, visible, complete) {
        if (!element) {
            if (complete) {
                complete();
            }
            return;
        }
        var existingMotion = motionFor(element);
        var computedStyle = window.getComputedStyle ? window.getComputedStyle(element) : null;
        var computedOpacity = computedStyle ? parseFloat(computedStyle.opacity) : 1;
        var startOpacity = existingMotion ? computedOpacity : (visible ? 0 : computedOpacity);
        var motion = cancelOwnedMotion(element);
        var styles = motion ? motion.styles : captureMotionStyles(element);
        startOwnedMotion(element, [
            { opacity: String(startOpacity) },
            { opacity: visible ? '1' : '0' }
        ], motionLayoutDuration, styles, function () {
            if (complete) {
                complete();
            }
        });
    }
    workbench.animateElementOpacity = animateElementOpacity;
    addLoad(installReducedMotionListener);
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
    /**
     * Give every select under root the shared chevron while keeping the native control. Exported for content built
     * after the page was decorated (the query result's panes).
     */
    function wrapSelects(root) {
        var doc = root.ownerDocument || root;
        var selects = root.querySelectorAll('select');
        var iconNamespace = 'http://www.w3.org/2000/svg';
        for (var i = 0; i < selects.length; i++) {
            var select = selects[i];
            if (select.parentElement && select.parentElement.classList.contains('workbench-select-control')) {
                continue;
            }
            var parent = select.parentNode;
            if (!parent) {
                continue;
            }
            var control = doc.createElement('span');
            control.className = 'workbench-select-control';
            parent.insertBefore(control, select);
            control.appendChild(select);
            var chevron = doc.createElementNS(iconNamespace, 'svg');
            chevron.setAttribute('class', 'workbench-action-icon workbench-select-chevron');
            chevron.setAttribute('viewBox', '0 0 24 24');
            chevron.setAttribute('width', '16');
            chevron.setAttribute('height', '16');
            chevron.setAttribute('focusable', 'false');
            chevron.setAttribute('aria-hidden', 'true');
            var chevronPath = doc.createElementNS(iconNamespace, 'path');
            chevronPath.setAttribute('d', 'm6 9 6 6 6-6');
            chevron.appendChild(chevronPath);
            control.appendChild(chevron);
        }
    }
    workbench.wrapSelects = wrapSelects;
    /**
     * The decorations every page gets: animated native disclosures (details elements), button/panel disclosures
     * whose panels open below their toolbar, and the shared select chevron. Run for the document once it loads and
     * for the outlet on every route mount; the returned function releases the disclosures this call installed.
     */
    function decoratePage(root) {
        var allDisclosures = root.querySelectorAll('details');
        var installed = [];
        for (var i = 0; i < allDisclosures.length; i++) {
            var details = allDisclosures[i];
            if (!nativeDisclosureState(details)) {
                installNativeDisclosure(details);
                installed.push(details);
            }
        }
        var releaseDisclosures = detailDisclosure.bindAll(root);
        wrapSelects(root);
        return function () {
            releaseDisclosures();
            for (var j = 0; j < installed.length; j++) {
                releaseNativeDisclosure(installed[j]);
            }
        };
    }
    workbench.decoratePage = decoratePage;
    /**
     * Submit a form from a script, for example after a confirmation dialog: through the in-page router when it
     * runs and can show the answer (M10.1), natively otherwise.
     */
    function submitForm(form) {
        var router = workbench.router;
        if (router && typeof router.submit === 'function') {
            router.submit(form);
        }
        else {
            form.submit();
        }
    }
    workbench.submitForm = submitForm;
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
 * Code to run when the document loads: eliminate the 'noscript' warning message.
 */
workbench
    .addLoad(function () {
    var noScriptMessage = document.getElementById('noscript-message');
    if (noScriptMessage) {
        noScriptMessage.style.display = 'none';
    }
    // The server user is part of the rendered shell state (workbench.views.contextBarState).
});
/**
 * Decorate the whole document once it has loaded: the shell, and the page of a route that is not mounted through
 * the route registry. Router-ready routes decorate their outlet from their mount (workbench.decoratePage).
 */
workbench.addLoad(function decorateDocument() {
    workbench.decoratePage(document);
});
//# sourceMappingURL=template.js.map