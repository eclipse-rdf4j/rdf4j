/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
/// <reference path="template.ts" />
/// <reference path="jquery.d.ts" />
// WARNING: Do not edit the *.js version of this file. Instead, always edit the
// corresponding *.ts source in the ts subfolder, and then invoke the
// compileTypescript.sh bash script to generate new *.js and *.js.map files.
var workbench;
(function (workbench) {
    var add;
    (function (add) {
        function handleFormatSelection(selected) {
            if (selected == 'application/x-trig' || selected == 'application/trix'
                || selected == 'text/x-nquads') {
                $('#useForContext').prop('checked', false);
                $('#context').val('').prop('readonly', false);
            }
        }
        add.handleFormatSelection = handleFormatSelection;
        function setContextFromBaseURI() {
            var baseURI = $('#baseURI').val();
            $('#context').val(baseURI == '' ? '' : '<' + baseURI + '>');
        }
        function handleBaseURIUse() {
            if ($('#useForContext').prop('checked')) {
                setContextFromBaseURI();
            }
        }
        add.handleBaseURIUse = handleBaseURIUse;
        function enabledInput(selected) {
            var istext = (selected == 'text');
            $('#text').prop('disabled', !istext);
            var contentType = $('#Content-Type');
            var firstType = contentType.find('option:first');
            firstType.prop('disabled', true);
            $('#source-' + selected).prop('checked', true);
            var isfile = (selected == 'file');
            var file = $('#file');
            file.prop('disabled', !isfile);
            var isurl = (selected == 'url');
            var url = $('#url');
            url.prop('disabled', !isurl);
            if (istext) {
                var turtle = contentType.find("option[value='application/x-turtle']");
                if (turtle.length == 0) {
                    turtle = contentType.find("option[value='text/turtle']");
                }
                if (turtle.length > 0) {
                    turtle.prop('selected', true);
                }
            }
            else {
                firstType.prop('selected', true);
                var baseURI = $('#baseURI');
                var checked = $('#useForContext').prop('checked');
                if (isfile) {
                    baseURI.val(file.val() == '' ? '' : encodeURI('file://'
                        + file.val().replace(/\\/g, '/')));
                    if (checked) {
                        setContextFromBaseURI();
                    }
                }
                else if (isurl) {
                    baseURI.val(url.val());
                    if (checked) {
                        setContextFromBaseURI();
                    }
                }
            }
        }
        add.enabledInput = enabledInput;
    })(add = workbench.add || (workbench.add = {}));
})(workbench || (workbench = {}));
//# sourceMappingURL=add.js.map