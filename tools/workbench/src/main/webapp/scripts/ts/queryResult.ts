//*******************************************************************************
// Copyright (c) 2026 Eclipse RDF4J contributors, Aduna, and others.
// All rights reserved. This program and the accompanying materials
// are made available under the terms of the Eclipse Distribution License v1.0
// which accompanies this distribution, and is available at
// http://www.eclipse.org/org/documents/edl-v10.php.
//*******************************************************************************

// WARNING: Do not edit the *.js version of this file. Instead, always edit the
// corresponding *.ts source in the ts subfolder, and then invoke the
// compileTypescript.sh bash script to generate new *.js and *.js.map files.

(function() {
    var marker: HTMLElement = document.getElementById('rdf4j-query-result');
    var embedded = window.parent && window.parent !== window;
    var targetWindow: Window = embedded ? window.parent : window.opener;
    var queryRequestId: string = marker && marker.getAttribute('data-query-request-id');
    var fullscreenEnabled = false;
    var normalWrapBeforeFullscreen: boolean = null;

    function targetOrigin(): string {
        var origin = window.location.origin;
        return origin && origin !== 'null' ? origin : window.location.protocol + '//' + window.location.host;
    }

    function postToParent(message: any) {
        if (!embedded || !window.parent || typeof window.parent.postMessage !== 'function') {
            return;
        }
        try {
            window.parent.postMessage(message, targetOrigin());
        } catch (error) {
            // The parent may have navigated away while this result was loading.
        }
    }

    function setFullscreenButtonState(enabled: boolean) {
        var button = <HTMLButtonElement>document.getElementById('query-result-fullscreen');
        if (!button) {
            return;
        }
        var label = enabled ? 'Exit full screen' : 'Full screen';
        button.setAttribute('aria-label', label);
        button.setAttribute('title', label);
        button.setAttribute('aria-pressed', enabled ? 'true' : 'false');
        var labelElement = button.querySelector('.query-results__fullscreen-label');
        if (labelElement) {
            labelElement.textContent = label;
        }
    }

    // The parent owns the in-page fullscreen surface. Keep the button inside
    // the result component so title, downloads, options and layout controls
    // stay together without allowing the result document to create a popup.
    (<any>window).rdf4jQueryResultToggleFullscreen = function() {
        postToParent({
            type: 'rdf4j-query-toggle-fullscreen',
            queryRequestId: queryRequestId || ''
        });
    };

    if (embedded) {
        window.addEventListener('message', function(event: MessageEvent) {
            if (event.origin !== targetOrigin() || event.source !== window.parent) {
                return;
            }
            var data = event.data || {};
            if (data.type !== 'rdf4j-query-fullscreen-state') {
                return;
            }
            if (data.queryRequestId && queryRequestId && data.queryRequestId !== queryRequestId) {
                return;
            }
            var enabled = data.enabled === true;
            if (enabled && !fullscreenEnabled) {
                var wrapControl = <HTMLInputElement>document.getElementById('result-wrap-values');
                normalWrapBeforeFullscreen = !wrapControl || wrapControl.checked;
                fullscreenEnabled = true;
                var layoutControl = <HTMLSelectElement>document.getElementById('result-layout');
                applyPresentation(layoutControl ? layoutControl.value : 'auto', false);
            } else if (!enabled && fullscreenEnabled) {
                fullscreenEnabled = false;
                var restoreWrap = normalWrapBeforeFullscreen !== null ? normalWrapBeforeFullscreen : true;
                normalWrapBeforeFullscreen = null;
                var currentLayout = <HTMLSelectElement>document.getElementById('result-layout');
                applyPresentation(currentLayout ? currentLayout.value : 'auto', restoreWrap);
            }
            setFullscreenButtonState(enabled);
        }, false);
        window.addEventListener('keydown', function(event: KeyboardEvent) {
            if (event.key === 'Escape' && fullscreenEnabled) {
                postToParent({
                    type: 'rdf4j-query-toggle-fullscreen',
                    queryRequestId: queryRequestId || ''
                });
                event.preventDefault();
            }
        }, false);
    }

    function getPresentationRoot(): HTMLElement {
        return document.getElementById('query-result-layout');
    }

    function measureRenderedHeaderText(text: string, style: CSSStyleDeclaration,
            context: CanvasRenderingContext2D): number {
        var container = getPresentationRoot() || document.body || document.documentElement;
        if (container && document.createElement && container.appendChild && container.removeChild) {
            var probe = document.createElement('span');
            var probeStyle = <any>probe.style;
            var computedStyle = <any>style;
            var textProperties = ['font', 'fontKerning', 'fontFeatureSettings', 'fontVariationSettings',
                'fontOpticalSizing', 'fontStretch', 'fontVariant', 'fontVariantCaps', 'fontSynthesis',
                'fontSizeAdjust', 'fontLanguageOverride', 'letterSpacing', 'wordSpacing', 'textTransform',
                'textRendering', 'direction'];
            textProperties.forEach(property => {
                if (computedStyle && computedStyle[property]) {
                    probeStyle[property] = computedStyle[property];
                }
            });
            probeStyle.position = 'fixed';
            probeStyle.left = '-10000px';
            probeStyle.top = '-10000px';
            probeStyle.visibility = 'hidden';
            probeStyle.display = 'inline-block';
            probeStyle.whiteSpace = 'pre';
            probeStyle.width = 'max-content';
            probeStyle.minWidth = '0';
            probeStyle.maxWidth = 'none';
            probe.textContent = text;
            container.appendChild(probe);
            try {
                var renderedWidth = probe.getBoundingClientRect().width;
                if (isFinite(renderedWidth) && renderedWidth > 0) {
                    return renderedWidth;
                }
            } finally {
                container.removeChild(probe);
            }
        }
        if (context) {
            var fallbackWidth = context.measureText(text).width;
            var letterSpacing = parseFloat(style.letterSpacing);
            if (isFinite(letterSpacing) && text.length > 1) {
                fallbackWidth += letterSpacing * (text.length - 1);
            }
            return fallbackWidth;
        }
        return 0;
    }

    function readableColumnWidths(table: HTMLElement): number[] {
        var header = table.querySelector('thead tr');
        if (!header || !header.children.length) {
            return [];
        }
        var tableStyle = getComputedStyle(table);
        var fontSize = parseFloat(tableStyle.fontSize) || 14;
        var characterWidth = fontSize * 0.55;
        var root = getPresentationRoot();
        var rootStyle = root ? getComputedStyle(root) : null;
        var characterBudget = root
            && rootStyle && typeof rootStyle.getPropertyValue === 'function'
            ? parseFloat(rootStyle.getPropertyValue('--query-result-readable-column-characters')) : NaN;
        if (!isFinite(characterBudget) || characterBudget <= 0) {
            characterBudget = 16;
        }
        var canvas = document.createElement('canvas');
        var context = canvas.getContext && canvas.getContext('2d');
        if (context) {
            context.font = tableStyle.font;
            characterWidth = context.measureText('0').width || characterWidth;
        }
        var widths = [];
        for (var i = 0; i < header.children.length; i++) {
            var cell = <HTMLElement>header.children[i];
            var cellStyle = getComputedStyle(cell);
            var horizontalPadding = (parseFloat(cellStyle.paddingLeft) || 0)
                + (parseFloat(cellStyle.paddingRight) || 0);
            var minimumContentWidth = characterWidth * characterBudget;
            var headerText = cell.textContent || '';
            if (context && headerText) {
                context.font = cellStyle.font || tableStyle.font;
                var headerTextWidth = measureRenderedHeaderText(headerText, cellStyle, context);
                minimumContentWidth = Math.max(minimumContentWidth, headerTextWidth);
            }
            widths.push(minimumContentWidth + horizontalPadding + 2);
        }
        return widths;
    }

    function applyPresentation(layout: string, wrap: boolean) {
        var root = getPresentationRoot();
        if (!root) {
            return;
        }
        var layoutControl = <HTMLSelectElement>document.getElementById('result-layout');
        var wrapControl = <HTMLInputElement>document.getElementById('result-wrap-values');
        if (layout !== 'auto' && layout !== 'table' && layout !== 'records') {
            layout = 'auto';
        }
        if (layoutControl) {
            layoutControl.value = layout;
        }
        if (wrapControl) {
            wrapControl.checked = wrap !== false;
        }
        root.setAttribute('data-layout', layout);
        root.setAttribute('data-wrap', wrap === false ? 'false' : 'true');

        var tableWrap = document.getElementById('query-result-table-wrap');
        var table = tableWrap && tableWrap.querySelector('table.data');
        var records = document.getElementById('query-result-records');
        var effectiveLayout = layout;
        if (layout === 'auto' && table && tableWrap) {
            var minimumWidths = readableColumnWidths(<HTMLElement>table);
            var readableWidth = minimumWidths.reduce((total, width) => total + width, 0);
            var availableWidth = root.clientWidth || root.getBoundingClientRect().width || 0;
            effectiveLayout = wrap === false || readableWidth <= availableWidth ? 'table' : 'records';
        }
        if (table) {
            var header = table.querySelector('thead tr');
            var minimumWidths = readableColumnWidths(<HTMLElement>table);
            if (header) {
                for (var index = 0; index < header.children.length; index++) {
                    (<HTMLElement>header.children[index]).style.minWidth = minimumWidths[index] + 'px';
                }
            }
        }
        root.setAttribute('data-effective-layout', effectiveLayout);
        if (tableWrap) {
            tableWrap.hidden = effectiveLayout === 'records';
        }
        if (records) {
            records.hidden = effectiveLayout !== 'records';
        }
    }

    function notifyPresentation() {
        var layoutControl = <HTMLSelectElement>document.getElementById('result-layout');
        var wrapControl = <HTMLInputElement>document.getElementById('result-wrap-values');
        postToParent({
            type: 'rdf4j-query-display-state',
            queryRequestId: queryRequestId || '',
            layout: layoutControl ? layoutControl.value : 'auto',
            wrap: !wrapControl || wrapControl.checked,
            fullscreen: fullscreenEnabled
        });
    }

    function installPresentationControls() {
        if (!embedded) {
            return;
        }
        var layoutControl = <HTMLSelectElement>document.getElementById('result-layout');
        var wrapControl = <HTMLInputElement>document.getElementById('result-wrap-values');
        if (layoutControl) {
            layoutControl.addEventListener('change', function() {
                applyPresentation(layoutControl.value, !wrapControl || wrapControl.checked);
                notifyPresentation();
            }, false);
        }
        if (wrapControl) {
            wrapControl.addEventListener('change', function() {
                applyPresentation(layoutControl ? layoutControl.value : 'auto', wrapControl.checked);
                notifyPresentation();
            }, false);
        }
        applyPresentation(layoutControl ? layoutControl.value : 'auto', !wrapControl || wrapControl.checked);
        if ((<any>window).ResizeObserver) {
            var presentationRoot = getPresentationRoot();
            if (presentationRoot) {
                var observer = new (<any>window).ResizeObserver(function() {
                    var selectedLayout = layoutControl ? layoutControl.value : 'auto';
                    if (selectedLayout === 'auto') {
                        applyPresentation(selectedLayout, !wrapControl || wrapControl.checked);
                    }
                });
                observer.observe(presentationRoot);
            }
        }
        window.addEventListener('resize', function() {
            var selectedLayout = layoutControl ? layoutControl.value : 'auto';
            if (selectedLayout === 'auto') {
                applyPresentation(selectedLayout, !wrapControl || wrapControl.checked);
            }
        }, false);
        window.addEventListener('message', function(event: MessageEvent) {
            if (event.origin !== targetOrigin() || event.source !== window.parent) {
                return;
            }
            var data = event.data || {};
            if (data.type !== 'rdf4j-query-display-state') {
                return;
            }
            if (data.queryRequestId && queryRequestId && data.queryRequestId !== queryRequestId) {
                return;
            }
            var wrapControl = <HTMLInputElement>document.getElementById('result-wrap-values');
            var fullscreenDisplay = fullscreenEnabled || data.fullscreen === true;
            applyPresentation(data.layout || 'auto', fullscreenDisplay && wrapControl
                ? wrapControl.checked : data.wrap !== false);
        }, false);
    }

    installPresentationControls();

    if (!queryRequestId || !targetWindow || typeof targetWindow.postMessage !== 'function') {
        return;
    }

    function notifyResult() {
        var origin = targetOrigin();
        try {
            if (targetWindow.closed) {
                return;
            }
            targetWindow.postMessage({
                type: 'rdf4j-query-result',
                queryRequestId: queryRequestId,
                status: marker.getAttribute('data-query-result-status') || 'completed'
            }, origin);
        } catch (error) {
            // An opener may have navigated to another origin or closed while the result loaded.
        }
    }

    if (document.readyState === 'complete') {
        notifyResult();
    } else {
        window.addEventListener('load', notifyResult, false);
    }
})();
