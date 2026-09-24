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
            fullscreenEnabled = data.enabled === true;
            setFullscreenButtonState(fullscreenEnabled);
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

    function readableColumnBudget(table: HTMLElement): number {
        var header = table.querySelector('thead tr');
        if (!header || !header.children.length) {
            return 0;
        }
        var tableStyle = getComputedStyle(table);
        var fontSize = parseFloat(tableStyle.fontSize) || 14;
        var characterWidth = fontSize * 0.55;
        var canvas = document.createElement('canvas');
        var context = canvas.getContext && canvas.getContext('2d');
        if (context) {
            context.font = tableStyle.font;
            characterWidth = context.measureText('0').width || characterWidth;
        }
        var budget = 0;
        for (var i = 0; i < header.children.length; i++) {
            var cell = <HTMLElement>header.children[i];
            var cellStyle = getComputedStyle(cell);
            var horizontalPadding = (parseFloat(cellStyle.paddingLeft) || 0)
                + (parseFloat(cellStyle.paddingRight) || 0);
            // Sixteen characters is the minimum readable width for a value
            // column. Auto switches to records when all columns cannot meet
            // that budget in the available frame width.
            budget += characterWidth * 16 + horizontalPadding + 2;
        }
        return budget;
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
            var readableWidth = readableColumnBudget(<HTMLElement>table);
            effectiveLayout = readableWidth > tableWrap.clientWidth ? 'records' : 'table';
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
            wrap: !wrapControl || wrapControl.checked
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
            var tableWrap = document.getElementById('query-result-table-wrap');
            if (tableWrap) {
                var observer = new (<any>window).ResizeObserver(function() {
                    var selectedLayout = layoutControl ? layoutControl.value : 'auto';
                    if (selectedLayout === 'auto') {
                        applyPresentation(selectedLayout, !wrapControl || wrapControl.checked);
                    }
                });
                observer.observe(tableWrap);
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
            applyPresentation(data.layout || 'auto', data.wrap !== false);
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
