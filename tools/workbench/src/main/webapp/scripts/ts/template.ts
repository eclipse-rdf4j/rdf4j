// WARNING: Do not edit the *.js version of this file. Instead, always edit the
// corresponding *.ts source in the ts subfolder, and then invoke the
// compileTypescript.sh bash script to generate new *.js and *.js.map files.

module workbench {

    export interface DetailDisclosureOptions {
        id?: string;
        toggleId: string;
        panelId: string;
        label: string;
        accessibleName?: string;
        ownerClass?: string;
        toggleClass?: string;
        panelClass?: string;
        contentClass?: string;
        panelRole?: string;
        expanded?: boolean;
        toggleHidden?: boolean;
        hidden?: boolean;
    }

    export interface DetailDisclosureElements {
        owner: HTMLElement;
        toggle: HTMLButtonElement;
        panel: HTMLElement;
        content: HTMLElement;
    }

    /** Shared settings/action disclosure markup and controller entry point. */
    export module detailDisclosure {
        var chevronPath = 'm6 9 6 6 6-6';

        function classes(base: string, extra: string): string {
            return extra ? base + ' ' + extra : base;
        }

        function renderToggle(html: any, options: DetailDisclosureOptions): any {
            var expanded = !!options.expanded;
            return html`<button id=${options.toggleId} type="button"
                    class=${classes('workbench-disclosure__toggle', options.toggleClass || '')}
                    aria-controls=${options.panelId} aria-expanded=${expanded ? 'true' : 'false'}
                    aria-label=${options.accessibleName || options.label} ?hidden=${!!options.toggleHidden}>
                    <span class="workbench-disclosure__toggle-label">${options.label}</span>
                    <svg class="workbench-action-icon workbench-action-icon--chevron workbench-disclosure-chevron"
                        data-workbench-icon="chevron" viewBox="0 0 24 24" width="16" height="16"
                        focusable="false" aria-hidden="true"><path d=${chevronPath}></path></svg>
                </button>`;
        }

        function renderPanel(html: any, options: DetailDisclosureOptions, content: any): any {
            return html`<div id=${options.panelId}
                    class=${classes('workbench-disclosure__panel', options.panelClass || '')}
                    role=${options.panelRole || 'region'} aria-labelledby=${options.toggleId}
                    ?hidden=${!options.expanded}>
                    <div class=${classes('workbench-disclosure__content', options.contentClass || '')}>${content}</div>
                </div>`;
        }

        function renderOwner(html: any, options: DetailDisclosureOptions, content: any): any {
            return html`<div id=${options.id || ''}
                    class=${classes('workbench-disclosure', options.ownerClass || '')}
                    data-workbench-detail-disclosure="true" ?hidden=${!!options.hidden}>${content}</div>`;
        }

        export function render(html: any, options: DetailDisclosureOptions, content: any): any {
            return renderOwner(html, options,
                html`${renderToggle(html, options)}${renderPanel(html, options, content)}`);
        }

        /** Keep triggers in their action group and associated panels in a shared full-width track. */
        export function renderSeparated(html: any, options: DetailDisclosureOptions,
                                        content: any): { owner: any; panel: any } {
            return {
                owner: renderOwner(html, options, renderToggle(html, options)),
                panel: renderPanel(html, options, content)
            };
        }

        export function create(doc: any, options: DetailDisclosureOptions): DetailDisclosureElements {
            var owner: HTMLElement = doc.createElement('div');
            owner.className = classes('workbench-disclosure', options.ownerClass || '');
            owner.setAttribute('data-workbench-detail-disclosure', 'true');
            owner.hidden = !!options.hidden;
            if (options.id) {
                owner.id = options.id;
            }

            var toggle: HTMLButtonElement = doc.createElement('button');
            toggle.type = 'button';
            toggle.id = options.toggleId;
            toggle.className = classes('workbench-disclosure__toggle', options.toggleClass || '');
            toggle.hidden = !!options.toggleHidden;
            toggle.setAttribute('aria-controls', options.panelId);
            toggle.setAttribute('aria-expanded', options.expanded ? 'true' : 'false');
            toggle.setAttribute('aria-label', options.accessibleName || options.label);
            var label: HTMLElement = doc.createElement('span');
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

            var panel: HTMLElement = doc.createElement('div');
            panel.id = options.panelId;
            panel.className = classes('workbench-disclosure__panel', options.panelClass || '');
            panel.setAttribute('role', options.panelRole || 'region');
            panel.setAttribute('aria-labelledby', options.toggleId);
            panel.hidden = !options.expanded;
            owner.appendChild(panel);

            var panelContent: HTMLElement = doc.createElement('div');
            panelContent.className = classes('workbench-disclosure__content', options.contentClass || '');
            panel.appendChild(panelContent);
            return { owner: owner, toggle: toggle, panel: panel, content: panelContent };
        }

        export function bind(toggle: HTMLButtonElement, panel: HTMLElement,
                            owner?: HTMLElement): () => void {
            if (!toggle || !panel || toggle.getAttribute('data-workbench-bound') === 'true') {
                return function() {};
            }
            toggle.setAttribute('data-workbench-bound', 'true');
            var disclosureOwner = owner || toggle.parentElement;
            var initiallyExpanded = toggle.getAttribute('aria-expanded') === 'true';
            workbench.setDisclosureExpanded(toggle, panel, disclosureOwner, initiallyExpanded, false);
            var onClick = function() {
                workbench.setDisclosureExpanded(toggle, panel, disclosureOwner,
                    toggle.getAttribute('aria-expanded') !== 'true', true);
            };
            toggle.addEventListener('click', onClick, false);
            var disposed = false;
            return function() {
                if (disposed) {
                    return;
                }
                disposed = true;
                toggle.removeEventListener('click', onClick, false);
                toggle.removeAttribute('data-workbench-bound');
                workbench.releaseDisclosure(toggle, panel, disclosureOwner);
            };
        }

        export function bindOwner(owner: HTMLElement): void {
            if (!owner) {
                return;
            }
            var toggle = <HTMLButtonElement>owner.querySelector('.workbench-disclosure__toggle');
            var panelId = toggle ? toggle.getAttribute('aria-controls') : null;
            var panel = panelId && owner.ownerDocument ? owner.ownerDocument.getElementById(panelId) : null;
            bind(toggle, panel, owner);
        }

        export function bindAll(root?: any): void {
            var scope = root || document;
            if (!scope || !scope.querySelectorAll) {
                return;
            }
            var owners = scope.querySelectorAll('[data-workbench-detail-disclosure="true"]');
            for (var i = 0; i < owners.length; i++) {
                bindOwner(<HTMLElement>owners[i]);
            }
        }
    }

    var requestIdCounter = 0;

    var motionDisclosureDuration = 180;
    var motionLayoutDuration = 220;
    var motionEasing = 'cubic-bezier(0.2, 0.7, 0.2, 1)';

    interface MotionStyleSnapshot {
        height: string;
        heightPriority: string;
        overflow: string;
        overflowPriority: string;
        opacity: string;
        opacityPriority: string;
    }

    interface DisclosureBoxStyle {
        boxSizing: string;
        paddingTop: string;
        paddingBottom: string;
        borderTopWidth: string;
        borderBottomWidth: string;
        marginBlockStart: string;
        marginBlockEnd: string;
    }

    interface OwnedMotion {
        element: HTMLElement;
        animation: Animation;
        styles: MotionStyleSnapshot;
        complete: () => void;
    }

    interface NativeDisclosureContentState {
        element: HTMLElement;
        inert: boolean;
        ariaHidden: string;
    }

    interface NativeDisclosureState {
        details: HTMLDetailsElement;
        summary: HTMLElement;
        content: NativeDisclosureContentState[];
        requestedOpen: boolean;
    }

    interface PanelDisclosureState {
        button: HTMLButtonElement;
        panel: HTMLElement;
        owner: HTMLElement;
        inert: boolean;
        ariaHidden: string;
        expanded: boolean;
    }

    interface DisclosureAnchor {
        button: HTMLButtonElement;
        panel: HTMLElement;
        track: HTMLElement;
    }

    var ownedMotions: OwnedMotion[] = [];
    var nativeDisclosureStates: NativeDisclosureState[] = [];
    var panelDisclosureStates: PanelDisclosureState[] = [];
    var disclosureAnchors: DisclosureAnchor[] = [];
    var disclosureAnchorObserver: any = null;
    var disclosureAnchorResizeListenerInstalled = false;
    var disclosureAnchorRefreshPending = false;

    function inlineStyleValue(element: HTMLElement, property: string): string {
        var style: any = element.style;
        return style && style.getPropertyValue ? style.getPropertyValue(property) : style ? style[property] || '' : '';
    }

    function inlineStylePriority(element: HTMLElement, property: string): string {
        var style: any = element.style;
        return style && style.getPropertyPriority ? style.getPropertyPriority(property) : '';
    }

    function captureMotionStyles(element: HTMLElement): MotionStyleSnapshot {
        return {
            height: inlineStyleValue(element, 'height'),
            heightPriority: inlineStylePriority(element, 'height'),
            overflow: inlineStyleValue(element, 'overflow'),
            overflowPriority: inlineStylePriority(element, 'overflow'),
            opacity: inlineStyleValue(element, 'opacity'),
            opacityPriority: inlineStylePriority(element, 'opacity')
        };
    }

    function restoreMotionStyles(element: HTMLElement, styles: MotionStyleSnapshot): void {
        var style: any = element.style;
        if (!style) {
            return;
        }
        if (styles.height) {
            if (style.setProperty) {
                style.setProperty('height', styles.height, styles.heightPriority);
            } else {
                style.height = styles.height;
            }
        } else {
            if (style.removeProperty) {
                style.removeProperty('height');
            } else {
                style.height = '';
            }
        }
        if (styles.overflow) {
            if (style.setProperty) {
                style.setProperty('overflow', styles.overflow, styles.overflowPriority);
            } else {
                style.overflow = styles.overflow;
            }
        } else {
            if (style.removeProperty) {
                style.removeProperty('overflow');
            } else {
                style.overflow = '';
            }
        }
        if (styles.opacity) {
            if (style.setProperty) {
                style.setProperty('opacity', styles.opacity, styles.opacityPriority);
            } else {
                style.opacity = styles.opacity;
            }
        } else {
            if (style.removeProperty) {
                style.removeProperty('opacity');
            } else {
                style.opacity = '';
            }
        }
    }

    function elementHeight(element: HTMLElement): number {
        return element && element.getBoundingClientRect ? element.getBoundingClientRect().height : 0;
    }

    function cssHeightFromBorderBox(element: HTMLElement, borderBoxHeight: number,
                                    style?: DisclosureBoxStyle): number {
        var computedStyle = style || window.getComputedStyle(element);
        if (computedStyle.boxSizing === 'border-box') {
            return borderBoxHeight;
        }
        var verticalInsets = parseFloat(computedStyle.paddingTop) + parseFloat(computedStyle.paddingBottom)
            + parseFloat(computedStyle.borderTopWidth) + parseFloat(computedStyle.borderBottomWidth);
        return Math.max(0, borderBoxHeight - verticalInsets);
    }

    function snapshotDisclosureBoxStyle(style: CSSStyleDeclaration): DisclosureBoxStyle {
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

    function disclosureBoxKeyframe(element: HTMLElement, borderBoxHeight: number,
                                    style: DisclosureBoxStyle): any {
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

    function collapsedDisclosureKeyframe(): any {
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

    function dispatchWorkbenchResize(): void {
        if (window.dispatchEvent && typeof Event !== 'undefined') {
            window.dispatchEvent(new Event('resize'));
        }
    }

    function motionFor(element: HTMLElement): OwnedMotion {
        for (var i = 0; i < ownedMotions.length; i++) {
            if (ownedMotions[i].element === element) {
                return ownedMotions[i];
            }
        }
        return null;
    }

    function removeOwnedMotion(motion: OwnedMotion): void {
        var index = ownedMotions.indexOf(motion);
        if (index >= 0) {
            ownedMotions.splice(index, 1);
        }
    }

    function cancelOwnedMotion(element: HTMLElement): OwnedMotion {
        var motion = motionFor(element);
        if (!motion) {
            return null;
        }
        motion.animation.onfinish = null;
        motion.animation.cancel();
        removeOwnedMotion(motion);
        return motion;
    }

    function reducedMotionRequested(): boolean {
        return !!(window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches);
    }

    function startOwnedMotion(element: HTMLElement, keyframes: any[], duration: number,
                              styles: MotionStyleSnapshot, complete: () => void): void {
        if (reducedMotionRequested() || !(<any>element).animate) {
            complete();
            restoreMotionStyles(element, styles);
            return;
        }
        var animation = element.animate(keyframes, {
            duration: duration,
            easing: motionEasing,
            fill: 'forwards'
        });
        var motion: OwnedMotion = {
            element: element,
            animation: animation,
            styles: styles,
            complete: complete
        };
        ownedMotions.push(motion);
        animation.onfinish = function() {
            if (motionFor(element) !== motion) {
                return;
            }
            animation.onfinish = null;
            removeOwnedMotion(motion);
            try {
                complete();
            } finally {
                animation.cancel();
                restoreMotionStyles(element, styles);
            }
        };
    }

    function settleOwnedMotions(): void {
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

    function installReducedMotionListener(): void {
        if (!window.matchMedia) {
            return;
        }
        var preference = window.matchMedia('(prefers-reduced-motion: reduce)');
        var settleWhenReduced = function(event: MediaQueryListEvent) {
            if (event.matches) {
                settleOwnedMotions();
            }
        };
        if (preference.addEventListener) {
            preference.addEventListener('change', settleWhenReduced);
        } else if ((<any>preference).addListener) {
            (<any>preference).addListener(settleWhenReduced);
        }
    }

    function nativeDisclosureState(details: HTMLDetailsElement): NativeDisclosureState {
        for (var i = 0; i < nativeDisclosureStates.length; i++) {
            if (nativeDisclosureStates[i].details === details) {
                return nativeDisclosureStates[i];
            }
        }
        return null;
    }

    function applyNativeDisclosureContent(state: NativeDisclosureState, expanded: boolean): void {
        for (var i = 0; i < state.content.length; i++) {
            var content = state.content[i];
            (<any>content.element).inert = expanded ? content.inert : true;
            if (expanded && content.ariaHidden === null) {
                content.element.setAttribute('aria-hidden', 'false');
            } else {
                content.element.setAttribute('aria-hidden', expanded ? content.ariaHidden : 'true');
            }
        }
    }

    function collapsedDisclosureHeight(state: NativeDisclosureState): number {
        var detailsStyle = window.getComputedStyle(state.details);
        var summaryStyle = window.getComputedStyle(state.summary);
        var summaryMargins = parseFloat(summaryStyle.marginTop) + parseFloat(summaryStyle.marginBottom);
        var collapsedBorderBoxHeight = elementHeight(state.summary) + summaryMargins
            + parseFloat(detailsStyle.paddingTop) + parseFloat(detailsStyle.paddingBottom)
            + parseFloat(detailsStyle.borderTopWidth) + parseFloat(detailsStyle.borderBottomWidth);
        return cssHeightFromBorderBox(state.details, collapsedBorderBoxHeight, detailsStyle);
    }

    export function setNativeDisclosureOpen(details: HTMLDetailsElement, expanded: boolean, animate?: boolean): void {
        var state = nativeDisclosureState(details);
        if (!state) {
            installNativeDisclosure(details);
            state = nativeDisclosureState(details);
        }
        if (!state || details.hidden || (<any>details).inert) {
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
        } else {
            applyNativeDisclosureContent(state, false);
        }

        if (details.classList.contains('workbench-nav-group__disclosure') && expanded) {
            var siblingGroups = document.querySelectorAll('#navigation .workbench-nav-group__disclosure');
            for (var i = 0; i < siblingGroups.length; i++) {
                var sibling = <HTMLDetailsElement>siblingGroups[i];
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
        ], motionDisclosureDuration, styles, function() {
            details.open = expanded;
            state.summary.setAttribute('aria-expanded', expanded ? 'true' : 'false');
            applyNativeDisclosureContent(state, expanded);
        });
    }

    export function installNativeDisclosure(details: HTMLDetailsElement): void {
        if (!details || nativeDisclosureState(details)) {
            return;
        }
        var summary: HTMLElement = null;
        for (var i = 0; i < details.children.length; i++) {
            if (details.children[i].tagName.toLowerCase() === 'summary') {
                summary = <HTMLElement>details.children[i];
                break;
            }
        }
        if (!summary) {
            return;
        }
        var content: NativeDisclosureContentState[] = [];
        for (var j = 0; j < details.children.length; j++) {
            var child = <HTMLElement>details.children[j];
            if (child === summary) {
                continue;
            }
            content.push({
                element: child,
                inert: !!(<any>child).inert,
                ariaHidden: child.getAttribute('aria-hidden')
            });
        }
        var state: NativeDisclosureState = {
            details: details,
            summary: summary,
            content: content,
            requestedOpen: details.open
        };
        nativeDisclosureStates.push(state);
        details.setAttribute('data-workbench-motion-ready', 'true');
        summary.setAttribute('aria-expanded', details.open ? 'true' : 'false');
        applyNativeDisclosureContent(state, details.open);
        summary.addEventListener('click', function(event: Event) {
            event.preventDefault();
            setNativeDisclosureOpen(details, !state.requestedOpen, true);
        }, true);
    }

    function panelDisclosureState(button: HTMLButtonElement, panel: HTMLElement,
                                  owner: HTMLElement): PanelDisclosureState {
        for (var i = 0; i < panelDisclosureStates.length; i++) {
            if (panelDisclosureStates[i].panel === panel) {
                return panelDisclosureStates[i];
            }
        }
        var initialAriaHidden = panel.getAttribute('aria-hidden');
        var state: PanelDisclosureState = {
            button: button,
            panel: panel,
            owner: owner,
            inert: !!(<any>panel).inert,
            ariaHidden: typeof initialAriaHidden === 'undefined' ? null : initialAriaHidden,
            expanded: button.getAttribute('aria-expanded') === 'true'
        };
        panelDisclosureStates.push(state);
        return state;
    }

    function disclosureAnchorTrack(panel: HTMLElement): HTMLElement {
        if (!panel || !panel.closest) {
            return null;
        }
        return <HTMLElement>panel.closest('.workbench-disclosure-track, .query-actions-toolbar, .query-result-disclosure-panels')
            || <HTMLElement>panel.closest('.workbench-disclosure');
    }

    function refreshDisclosureAnchor(button: HTMLButtonElement, panel: HTMLElement,
                                     track: HTMLElement): void {
        if (!button || !panel || !track || panel.hidden || button.hidden || button.disabled
                || button.getClientRects().length === 0) {
            return;
        }

        panel.style.setProperty('--workbench-disclosure-panel-start', '0px');
        var trackRect = track.getBoundingClientRect();
        var panelRect = panel.getBoundingClientRect();
        var buttonRect = button.getBoundingClientRect();
        if (trackRect.width <= 0 || panelRect.width <= 0) {
            return;
        }

        var direction = window.getComputedStyle(track).direction;
        var availableInlineStart = Math.max(0, trackRect.width - panelRect.width);
        var desiredInlineStart = direction === 'rtl'
            ? trackRect.right - buttonRect.left - panelRect.width
            : buttonRect.right - trackRect.left - panelRect.width;
        var inlineStart = Math.max(0, Math.min(availableInlineStart, desiredInlineStart));
        var panelLeft = direction === 'rtl'
            ? trackRect.right - inlineStart - panelRect.width
            : trackRect.left + inlineStart;
        var buttonCenter = (buttonRect.left + buttonRect.right) / 2;
        var anchorOffset = Math.max(4, Math.min(panelRect.width - 4, buttonCenter - panelLeft));
        panel.style.setProperty('--workbench-disclosure-panel-start', inlineStart + 'px');
        panel.style.setProperty('--workbench-disclosure-anchor-x', anchorOffset + 'px');
    }

    function refreshDisclosureAnchors(): void {
        disclosureAnchorRefreshPending = false;
        for (var i = 0; i < disclosureAnchors.length; i++) {
            var anchor = disclosureAnchors[i];
            refreshDisclosureAnchor(anchor.button, anchor.panel, anchor.track);
        }
    }

    function scheduleDisclosureAnchorRefresh(): void {
        if (disclosureAnchorRefreshPending) {
            return;
        }
        disclosureAnchorRefreshPending = true;
        if (window.requestAnimationFrame) {
            window.requestAnimationFrame(refreshDisclosureAnchors);
        } else {
            refreshDisclosureAnchors();
        }
    }

    function registerDisclosureAnchor(button: HTMLButtonElement, panel: HTMLElement): void {
        var track = disclosureAnchorTrack(panel);
        if (!track) {
            return;
        }
        for (var i = 0; i < disclosureAnchors.length; i++) {
            if (disclosureAnchors[i].panel === panel) {
                return;
            }
        }

        disclosureAnchors.push({ button: button, panel: panel, track: track });
        if (!disclosureAnchorResizeListenerInstalled) {
            window.addEventListener('resize', scheduleDisclosureAnchorRefresh);
            disclosureAnchorResizeListenerInstalled = true;
        }
        if (!disclosureAnchorObserver && (<any>window).ResizeObserver) {
            var ResizeObserverConstructor = (<any>window).ResizeObserver;
            disclosureAnchorObserver = new ResizeObserverConstructor(scheduleDisclosureAnchorRefresh);
        }
        if (disclosureAnchorObserver) {
            disclosureAnchorObserver.observe(button);
            disclosureAnchorObserver.observe(panel);
            disclosureAnchorObserver.observe(track);
        }
        refreshDisclosureAnchor(button, panel, track);
    }

    export function releaseDisclosure(button: HTMLButtonElement, panel: HTMLElement,
                                      owner?: HTMLElement): void {
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

        var releasedTrack: HTMLElement = null;
        for (var anchorIndex = disclosureAnchors.length - 1; anchorIndex >= 0; anchorIndex--) {
            var anchor = disclosureAnchors[anchorIndex];
            if (anchor.panel === panel || button && anchor.button === button) {
                releasedTrack = anchor.track;
                if (disclosureAnchorObserver && disclosureAnchorObserver.unobserve) {
                    disclosureAnchorObserver.unobserve(anchor.button);
                    disclosureAnchorObserver.unobserve(anchor.panel);
                }
                disclosureAnchors.splice(anchorIndex, 1);
            }
        }
        var trackStillObserved = false;
        if (releasedTrack) {
            for (var remainingIndex = 0; remainingIndex < disclosureAnchors.length; remainingIndex++) {
                if (disclosureAnchors[remainingIndex].track === releasedTrack) {
                    trackStillObserved = true;
                    break;
                }
            }
        }
        if (!trackStillObserved && releasedTrack && disclosureAnchorObserver && disclosureAnchorObserver.unobserve) {
            disclosureAnchorObserver.unobserve(releasedTrack);
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
    }

    export function setDisclosureExpanded(button: HTMLButtonElement, panel: HTMLElement, owner: HTMLElement,
                                         expanded: boolean, animate?: boolean): void {
        if (!button || !panel) {
            return;
        }
        registerDisclosureAnchor(button, panel);
        var state = panelDisclosureState(button, panel, owner);
        if (button.hidden || button.disabled) {
            state.expanded = false;
            button.setAttribute('aria-expanded', 'false');
            panel.hidden = true;
            (<any>panel).inert = true;
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
                    setDisclosureExpanded(siblingDisclosure.button, siblingDisclosure.panel,
                        siblingDisclosure.owner, false, false);
                }
            }
        }
        var existingMotion = motionFor(panel);
        if (state.expanded === expanded && !existingMotion) {
            button.setAttribute('aria-expanded', expanded ? 'true' : 'false');
            panel.hidden = !expanded;
            (<any>panel).inert = expanded ? state.inert : true;
            if (expanded && state.ariaHidden === null) {
                panel.setAttribute('aria-hidden', 'false');
            } else {
                panel.setAttribute('aria-hidden', expanded ? state.ariaHidden : 'true');
            }
            if (owner) {
                owner.classList.toggle('is-open', expanded);
            }
            if (expanded && anchorTrack) {
                refreshDisclosureAnchor(button, panel, anchorTrack);
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
        if (expanded && anchorTrack) {
            refreshDisclosureAnchor(button, panel, anchorTrack);
        }
        (<any>panel).inert = expanded ? state.inert : true;
        if (expanded && state.ariaHidden === null) {
            panel.setAttribute('aria-hidden', 'false');
        } else {
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
        var keyframes: any[] = [startBox, endBox];
        startOwnedMotion(panel, keyframes, motionDisclosureDuration, styles, function() {
            panel.hidden = !expanded;
            (<any>panel).inert = expanded ? state.inert : true;
            if (expanded && state.ariaHidden === null) {
                panel.setAttribute('aria-hidden', 'false');
            } else {
                panel.setAttribute('aria-hidden', expanded ? state.ariaHidden : 'true');
            }
        });
    }

    export function setElementExpanded(element: HTMLElement, expanded: boolean, animate?: boolean): void {
        if (!element || element.hidden) {
            return;
        }
        var existingMotion = motionFor(element);
        var wasExpanded = window.getComputedStyle(element).display !== 'none';
        if (wasExpanded === expanded && !existingMotion) {
            (<any>element).inert = !expanded;
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
        (<any>element).inert = !expanded;
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
        startOwnedMotion(element, [startBox, endBox], motionDisclosureDuration, styles, function() {
            element.style.display = '';
        });
    }

    export function animateElementOpacity(element: HTMLElement, visible: boolean, complete?: () => void): void {
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
        ], motionLayoutDuration, styles, function() {
            if (complete) {
                complete();
            }
        });
    }

    addLoad(installReducedMotionListener);

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

    var allDisclosures = document.querySelectorAll('details');
    for (var i = 0; i < allDisclosures.length; i++) {
        workbench.installNativeDisclosure(<HTMLDetailsElement>allDisclosures[i]);
    }

    var mediaQuery = window.matchMedia ? window.matchMedia('(max-width: 900px)') : null;
    var firstSync = true;
    var syncDisclosure = function() {
        var isMobile = mediaQuery ? mediaQuery.matches : window.innerWidth <= 900;
        workbench.setNativeDisclosureOpen(disclosure, !isMobile, !firstSync);
        firstSync = false;
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
                var group = item.parentElement;
                while (group && !group.classList.contains('workbench-nav-group')) {
                    group = group.parentElement;
                }
                if (group) {
                    var groupDisclosure = <HTMLDetailsElement>group.querySelector('.workbench-nav-group__disclosure');
                    if (groupDisclosure) {
                        workbench.setNativeDisclosureOpen(groupDisclosure, true, false);
                    }
                }
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
    workbench.detailDisclosure.bindAll(document);
});

/**
 * Keep native select controls keyboard and screen-reader accessible while
 * giving every Workbench page and embedded result one shared chevron style.
 */
workbench.addLoad(function installSharedSelectControls() {
    var selects = document.querySelectorAll('select');
    var iconNamespace = 'http://www.w3.org/2000/svg';
    for (var i = 0; i < selects.length; i++) {
        var select = <HTMLSelectElement>selects[i];
        if (select.parentElement && select.parentElement.classList.contains('workbench-select-control')) {
            continue;
        }

        var parent = select.parentNode;
        if (!parent) {
            continue;
        }

        var control = document.createElement('span');
        control.className = 'workbench-select-control';
        parent.insertBefore(control, select);
        control.appendChild(select);

        var chevron = document.createElementNS(iconNamespace, 'svg');
        chevron.setAttribute('class', 'workbench-action-icon workbench-select-chevron');
        chevron.setAttribute('viewBox', '0 0 24 24');
        chevron.setAttribute('width', '16');
        chevron.setAttribute('height', '16');
        chevron.setAttribute('focusable', 'false');
        chevron.setAttribute('aria-hidden', 'true');
        var chevronPath = document.createElementNS(iconNamespace, 'path');
        chevronPath.setAttribute('d', 'm6 9 6 6 6-6');
        chevron.appendChild(chevronPath);
        control.appendChild(chevron);
    }
});
