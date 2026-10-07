/// <reference path="template.ts" />
/// <reference path="jquery.d.ts" />
// WARNING: Do not edit the *.js version of this file. Instead, always edit the
// corresponding *.ts source in the ts subfolder, and then invoke the
// compileTypescript.sh bash script to generate new *.js and *.js.map files.
var workbench;
(function (workbench) {
    var create;
    (function (create) {
        /** The field with the given role (inside root when given), or the fallback selector's field. */
        function findFieldByRole(role, fallbackSelector, root) {
            var find = function (selector) { return root ? $(root).find(selector) : $(selector); };
            var field = find('[data-field-role="' + role + '"]');
            return field.length ? field : find(fallbackSelector);
        }
        create.findFieldByRole = findFieldByRole;
        /** The id of the message that explains an id the server cannot use. */
        var ID_ERROR = 'repository-id-error';
        /**
         * The repository ids the server accepts (CreateServlet answers any other with 400 and the same message): ASCII
         * letters, digits, '-', '_', '.' and '@' (the Remote template's default id is SYSTEM@localhost), and not only
         * dots. Such an id works as a URL path segment and as a directory name.
         */
        var VALID_ID = /^(?!\.+$)[A-Za-z0-9._@-]+$/;
        /**
         * Why a repository id cannot be used, or '' when it can. An empty id is explained only when asked for (once
         * the field was edited): a form that opens without an id keeps Create disabled without an error.
         */
        function idProblem(value, explainEmpty) {
            if (!value) {
                return explainEmpty ? 'Enter a repository ID.' : '';
            }
            return VALID_ID.test(value) ? '' : "Repository ID '" + value + "' is not valid: use only the letters a-z "
                + "and A-Z, digits, '-', '_', '.' and '@', and not only dots.";
        }
        create.idProblem = idProblem;
        /** The message under the id field, created on first use. */
        function idError(field) {
            var doc = field.ownerDocument;
            var error = doc.getElementById(ID_ERROR);
            if (!error) {
                error = doc.createElement('span');
                error.id = ID_ERROR;
                error.className = 'error workbench-field-error';
                error.setAttribute('role', 'alert');
                error.hidden = true;
                if (field.parentNode) {
                    field.parentNode.insertBefore(error, field.nextSibling);
                }
            }
            return error;
        }
        /**
         * Shows or clears the explanation of an unusable id at the id field (of an empty one too with explainEmpty).
         * With focus, the field takes focus so the id can be corrected. Returns whether the id can be used.
         */
        function checkId(focus, explainEmpty) {
            var field = create.id && create.id.get(0);
            if (!field) {
                return true;
            }
            var problem = idProblem(field.value, explainEmpty);
            var error = idError(field);
            error.textContent = problem;
            error.hidden = !problem;
            if (problem) {
                field.setAttribute('aria-invalid', 'true');
                field.setAttribute('aria-describedby', ID_ERROR);
                if (focus) {
                    field.focus();
                }
            }
            else {
                field.removeAttribute('aria-invalid');
                if (field.getAttribute('aria-describedby') === ID_ERROR) {
                    field.removeAttribute('aria-describedby');
                }
            }
            return !problem && !!field.value;
        }
        create.checkId = checkId;
        /**
         * Whether a repository with this id exists: 'exists', 'absent', or '' when the server could not tell. It reads
         * the repository list, which answers without an error either way (an unknown id's information page answers
         * 404 Not Found, which browsers log as an error). When the list cannot be read (a policy can hide it), the
         * id's information page answers instead.
         */
        function existence(value, ownerUrl, isCurrent) {
            var resolve = function (path) {
                return ownerUrl ? new URL(path, ownerUrl).href : path;
            };
            return new Promise(function (done) {
                var listed = null;
                $.ajax({
                    url: resolve('../NONE/repositories'),
                    dataType: 'json',
                    headers: { Accept: 'application/sparql-results+json' },
                    success: function (data) {
                        var bindings = data && data.results && data.results.bindings;
                        if (bindings && typeof bindings.length === 'number') {
                            listed = 'absent';
                            for (var i = 0; i < bindings.length; i++) {
                                if (bindings[i] && bindings[i].id && bindings[i].id.value === value) {
                                    listed = 'exists';
                                }
                            }
                        }
                    },
                    complete: function () {
                        // A form that is no longer shown needs no second answer.
                        if (listed || (isCurrent && !isCurrent())) {
                            done(listed || '');
                            return;
                        }
                        var found = '';
                        $.ajax({
                            url: resolve('../' + encodeURIComponent(value) + '/info'),
                            success: function () {
                                found = 'exists';
                            },
                            statusCode: {
                                // 404 answers an unknown repository; 500 is kept for servers from before the
                                // not-found page.
                                404: function () {
                                    found = 'absent';
                                },
                                500: function () {
                                    found = 'absent';
                                }
                            },
                            complete: function () {
                                done(found);
                            }
                        });
                    }
                });
            });
        }
        create.existence = existence;
        /**
         * Route mount (plan task M7.2): group the advanced settings, fill id and title from the URL and keep Create
         * disabled while the id is empty. The returned function unbinds the id handler and the advanced disclosure.
         */
        function mount(outlet) {
            /**
             * Keep required identity, endpoint, and rule query fields visible while moving the
             * lower-frequency repository tuning fields into one shared disclosure. The
             * rows are moved rather than cloned so every existing input name, value and
             * form submission remains unchanged.
             */
            function installAdvancedFields() {
                var existing = outlet.querySelector('[data-workbench-detail-disclosure="true"].workbench-advanced');
                if (existing) {
                    return existing;
                }
                var table = outlet.querySelector("form[action='create'] table.dataentry");
                if (!table) {
                    return null;
                }
                var body = table.tBodies.length ? table.tBodies[0] : null;
                if (!body || body.rows.length < 3) {
                    return null;
                }
                var rowsToMove = [];
                for (var i = 1; i < body.rows.length; i++) {
                    var row = body.rows[i];
                    var roleControl = row.querySelector('[data-field-role], [data-config-property]');
                    var role = (roleControl && roleControl.getAttribute('data-field-role'))
                        || row.getAttribute('data-field-role') || '';
                    var fieldId = roleControl ? roleControl.id || '' : '';
                    var property = roleControl ? roleControl.getAttribute('data-config-property') || '' : '';
                    var required = fieldId === 'sp_text'
                        || role === 'repository-id' || role === 'repository-title'
                        || role === 'federation-member'
                        || property === 'config:http.url'
                        || property === 'config:sparql.queryEndpoint'
                        || property === 'config:sparql.updateEndpoint'
                        || property === 'config:cgqi.queryLanguage';
                    if (!required) {
                        rowsToMove.push(row);
                    }
                }
                if (!rowsToMove.length) {
                    return null;
                }
                var disclosure = workbench.detailDisclosure.create(document, {
                    id: 'create-advanced-disclosure', toggleId: 'create-advanced-toggle',
                    panelId: 'create-advanced-panel',
                    label: table.getAttribute('data-advanced-label') || 'Advanced settings',
                    ownerClass: 'workbench-advanced workbench-form-subgroup'
                });
                var advancedFields = document.createElement('div');
                advancedFields.className = 'workbench-disclosure__fields workbench-advanced-fields';
                disclosure.content.appendChild(advancedFields);
                var advancedItems = [];
                for (var j = 0; j < rowsToMove.length; j++) {
                    var row = rowsToMove[j];
                    var field = document.createElement('div');
                    field.className = 'workbench-disclosure__field workbench-advanced__field';
                    var fieldLabel = '';
                    var heading = row.querySelector('th');
                    if (heading) {
                        var label = heading.querySelector('label');
                        if (label) {
                            fieldLabel = label.textContent ? label.textContent.trim() : '';
                            field.appendChild(label);
                        }
                        else if (heading.textContent && heading.textContent.trim()) {
                            fieldLabel = heading.textContent.trim();
                            var fieldHeading = document.createElement('span');
                            fieldHeading.textContent = fieldLabel;
                            field.appendChild(fieldHeading);
                        }
                    }
                    var controls = document.createElement('div');
                    controls.className = 'workbench-advanced__control';
                    var cells = row.querySelectorAll('td');
                    for (var cellIndex = 0; cellIndex < cells.length; cellIndex++) {
                        while (cells[cellIndex].firstChild) {
                            controls.appendChild(cells[cellIndex].firstChild);
                        }
                    }
                    if (controls.childNodes.length) {
                        field.appendChild(controls);
                    }
                    advancedItems.push({ field: field, label: fieldLabel, order: j });
                    if (row.parentNode) {
                        row.parentNode.removeChild(row);
                    }
                }
                function firstField(node) {
                    if (node.entries.length) {
                        return node.entries[0].field;
                    }
                    for (var childIndex = 0; childIndex < node.children.length; childIndex++) {
                        var field = firstField(node.children[childIndex]);
                        if (field) {
                            return field;
                        }
                    }
                    return null;
                }
                function appendPrefixBranch(parent, node) {
                    var groupNode = node;
                    while (groupNode.entries.length === 0 && groupNode.children.length === 1
                        && groupNode.children[0].total === groupNode.total) {
                        groupNode = groupNode.children[0];
                    }
                    if (groupNode.total < 2) {
                        var onlyField = firstField(groupNode);
                        if (onlyField) {
                            parent.appendChild(onlyField);
                        }
                        return;
                    }
                    var group = document.createElement('fieldset');
                    group.className = 'workbench-advanced__group';
                    group.setAttribute('data-workbench-config-group', groupNode.label);
                    var legend = document.createElement('legend');
                    legend.className = 'workbench-advanced__group-title';
                    legend.textContent = groupNode.label;
                    group.appendChild(legend);
                    var groupFields = document.createElement('div');
                    groupFields.className = 'workbench-advanced__group-fields';
                    group.appendChild(groupFields);
                    var actions = [];
                    for (var entryIndex = 0; entryIndex < groupNode.entries.length; entryIndex++) {
                        actions.push({ entry: groupNode.entries[entryIndex], order: groupNode.entries[entryIndex].order });
                    }
                    for (var groupChildIndex = 0; groupChildIndex < groupNode.children.length; groupChildIndex++) {
                        var child = groupNode.children[groupChildIndex];
                        actions.push({ node: child, order: child.firstOrder });
                    }
                    actions.sort(function (left, right) { return left.order - right.order; });
                    for (var actionIndex = 0; actionIndex < actions.length; actionIndex++) {
                        var action = actions[actionIndex];
                        if (action.entry) {
                            groupFields.appendChild(action.entry.field);
                        }
                        else if (action.node.total > 1) {
                            appendPrefixBranch(groupFields, action.node);
                        }
                        else {
                            var childField = firstField(action.node);
                            if (childField) {
                                groupFields.appendChild(childField);
                            }
                        }
                    }
                    parent.appendChild(group);
                }
                var prefixRoot = { entries: [], children: [] };
                for (var itemIndex = 0; itemIndex < advancedItems.length; itemIndex++) {
                    var item = advancedItems[itemIndex];
                    var words = item.label ? item.label.trim().split(/\s+/) : [];
                    var node = prefixRoot;
                    if (!words.length) {
                        node.entries.push(item);
                        continue;
                    }
                    for (var wordIndex = 0; wordIndex < words.length; wordIndex++) {
                        var normalizedWord = words[wordIndex].toLowerCase();
                        var next = null;
                        for (var candidateIndex = 0; candidateIndex < node.children.length; candidateIndex++) {
                            if (node.children[candidateIndex].word === normalizedWord) {
                                next = node.children[candidateIndex];
                                break;
                            }
                        }
                        if (!next) {
                            next = {
                                word: normalizedWord,
                                label: words.slice(0, wordIndex + 1).join(' '),
                                entries: [], children: [], total: 0, firstOrder: item.order
                            };
                            node.children.push(next);
                        }
                        next.total++;
                        next.firstOrder = Math.min(next.firstOrder, item.order);
                        node = next;
                    }
                    node.entries.push(item);
                }
                var topLevelActions = [];
                for (var rootEntryIndex = 0; rootEntryIndex < prefixRoot.entries.length; rootEntryIndex++) {
                    topLevelActions.push({ entry: prefixRoot.entries[rootEntryIndex], order: prefixRoot.entries[rootEntryIndex].order });
                }
                for (var rootChildIndex = 0; rootChildIndex < prefixRoot.children.length; rootChildIndex++) {
                    var rootChild = prefixRoot.children[rootChildIndex];
                    topLevelActions.push({ node: rootChild, order: rootChild.firstOrder });
                }
                topLevelActions.sort(function (left, right) { return left.order - right.order; });
                for (var topLevelIndex = 0; topLevelIndex < topLevelActions.length; topLevelIndex++) {
                    var topLevelAction = topLevelActions[topLevelIndex];
                    if (topLevelAction.entry) {
                        advancedFields.appendChild(topLevelAction.entry.field);
                    }
                    else if (topLevelAction.node.total > 1) {
                        appendPrefixBranch(advancedFields, topLevelAction.node);
                    }
                    else {
                        var topLevelField = firstField(topLevelAction.node);
                        if (topLevelField) {
                            advancedFields.appendChild(topLevelField);
                        }
                    }
                }
                table.parentNode.insertBefore(disclosure.owner, table.nextSibling);
                return disclosure.owner;
            }
            var releaseAdvanced = workbench.detailDisclosure.bindOwner(installAdvancedFields());
            create.id = findFieldByRole('repository-id', '#id', outlet);
            create.title = findFieldByRole('repository-title', '#title', outlet);
            /**
             * Disables the create button while the id is empty or one the server does not accept.
             */
            var createButton = $(outlet).find('input#create');
            function disableCreateUnlessValidId() {
                createButton.prop('disabled', !VALID_ID.test(create.id.val() || ''));
            }
            // Populate parameters
            var elements = workbench.getQueryStringElements();
            for (var i = 0; elements.length - i; i++) {
                // A '+' in the query string is a space; an encoded one (%2B) stays a '+'.
                var separator = elements[i].indexOf('=');
                var pair = separator < 0 ? [elements[i], ''] : [elements[i].substring(0, separator),
                    elements[i].substring(separator + 1)];
                var value = decodeURIComponent(pair[1].replace(/\+/g, ' '));
                if (pair[0] == 'id') {
                    create.id.val(value);
                }
                if (pair[0] == 'title') {
                    create.title.val(value);
                }
            }
            disableCreateUnlessValidId();
            checkId(false);
            // Calls another function with a delay of 0 msec. (Workaround for
            // annoying browser behavior.)
            var fields = create.id;
            fields.on('keydown.wbRoute paste.wbRoute cut.wbRoute', function () {
                setTimeout(disableCreateUnlessValidId, 0);
            });
            // Explains an id the server cannot use (or an emptied one) as it is typed, and clears that once it is fixed.
            fields.on('input.wbRoute', function () {
                disableCreateUnlessValidId();
                checkId(false, true);
            });
            return function () {
                fields.off('.wbRoute');
                releaseAdvanced();
            };
        }
        create.mount = mount;
    })(create = workbench.create || (workbench.create = {}));
})(workbench || (workbench = {}));
/**
 * Invoked by the "Create" button on the form for all but Create form views. An id the server cannot use is explained
 * at the id field and not sent. Otherwise the repository list tells whether the id exists already, giving a chance to
 * back out before its configuration is replaced.
 */
function checkOverwrite() {
    var id = workbench.create.id.val();
    var form = $(workbench.create.id.get(0)).closest('form').get(0);
    var owner = workbench.captureFormOwner(form);
    if (!owner.isCurrent()) {
        return;
    }
    if (!workbench.create.checkId(true, true)) {
        return;
    }
    // 'exists' asks before replacing the configuration; 'absent' creates; anything else sends nothing.
    workbench.create.existence(id, owner.url, owner.isCurrent).then(function (found) {
        if (!owner.isCurrent()) {
            return false;
        }
        var overwrite = found == 'exists' ? workbench.confirmDialog.open({
            title: 'Replace repository configuration?',
            body: 'A repository with the id "' + id + '" already exists. Creating it again replaces its configuration.',
            confirmLabel: 'Replace configuration',
            danger: true
        }) : Promise.resolve(found == 'absent');
        return overwrite.then(function (submit) {
            if (!owner.isCurrent()) {
                return false;
            }
            if (submit && !id.match(/^[a-z0-9._-]+$/)) {
                return workbench.confirmDialog.open({
                    title: 'Use this repository id?',
                    body: 'The id "' + id + '" contains characters other than lowercase letters a-z, digits, '
                        + 'dots, underscores and hyphens, which some tools cannot handle.',
                    confirmLabel: 'Create repository',
                    danger: false
                });
            }
            return submit;
        });
    }).then(function (submit) {
        if (submit && owner.isCurrent()) {
            workbench.submitForm(form);
        }
    });
}
//# sourceMappingURL=create.js.map