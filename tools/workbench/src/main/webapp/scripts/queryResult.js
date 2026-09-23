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
(function () {
    var marker = document.getElementById('rdf4j-query-result');
    var queryRequestId = marker && marker.getAttribute('data-query-request-id');
    var embedded = window.parent && window.parent !== window;
    var targetWindow = embedded ? window.parent : window.opener;
    if (!queryRequestId || !targetWindow || typeof targetWindow.postMessage !== 'function') {
        return;
    }
    function notifyOpener() {
        var targetOrigin = window.location.origin;
        if (!targetOrigin || targetOrigin === 'null') {
            targetOrigin = window.location.protocol + '//' + window.location.host;
        }
        try {
            if (targetWindow.closed) {
                return;
            }
            targetWindow.postMessage({
                type: 'rdf4j-query-result',
                queryRequestId: queryRequestId,
                status: marker.getAttribute('data-query-result-status') || 'completed'
            }, targetOrigin);
        }
        catch (error) {
            // An opener may have navigated to another origin or closed while the result loaded.
        }
    }
    if (document.readyState === 'complete') {
        notifyOpener();
    }
    else {
        window.addEventListener('load', notifyOpener, false);
    }
})();
//# sourceMappingURL=queryResult.js.map