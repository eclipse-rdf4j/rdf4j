/// <reference path="template.ts" />
/// <reference path="jquery.d.ts" />

// WARNING: Do not edit the *.js version of this file. Instead, always edit the
// corresponding *.ts source in the ts subfolder, and then invoke the
// compileTypescript.sh bash script to generate new *.js and *.js.map files.

module workbench {

    export module create {
        /** The field with the given role (inside root when given), or the fallback selector's field. */
        export function findFieldByRole(role: string, fallbackSelector: string, root?: HTMLElement): JQuery {
            var find = function(selector: string) { return root ? $(root).find(selector) : $(selector); };
            var field = find('[data-field-role="' + role + '"]');
            return field.length ? field : find(fallbackSelector);
        }

        /** The repository id and title fields of the mounted form (set by mount). */
        export var id: JQuery;
        export var title: JQuery;

        /**
         * Route mount (plan task M7.2): group the advanced settings, fill id and title from the URL and keep Create
         * disabled while the id is empty. The returned function unbinds the id handler and the advanced disclosure.
         */
        export function mount(outlet: HTMLElement): () => void {
            /**
             * Keep required identity, endpoint, and rule query fields visible while moving the
             * lower-frequency repository tuning fields into one shared disclosure. The
             * rows are moved rather than cloned so every existing input name, value and
             * form submission remains unchanged.
             */
            function installAdvancedFields(): HTMLElement {
                var existing = <HTMLElement>outlet.querySelector(
                    '[data-workbench-detail-disclosure="true"].workbench-advanced');
                if (existing) {
                    return existing;
                }
                var table = <HTMLTableElement>outlet.querySelector("form[action='create'] table.dataentry");
                if (!table) {
                    return null;
                }
                var body = table.tBodies.length ? table.tBodies[0] : null;
                if (!body || body.rows.length < 3) {
                    return null;
                }

                var rowsToMove: HTMLTableRowElement[] = [];
                for (var i = 1; i < body.rows.length; i++) {
                    var row = body.rows[i];
                    var roleControl = <HTMLElement>row.querySelector('[data-field-role], [data-config-property]');
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

                var advancedItems: any[] = [];
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
                        } else if (heading.textContent && heading.textContent.trim()) {
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

                function firstField(node: any): HTMLElement | null {
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

                function appendPrefixBranch(parent: HTMLElement, node: any): void {
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

                    var actions: any[] = [];
                    for (var entryIndex = 0; entryIndex < groupNode.entries.length; entryIndex++) {
                        actions.push({ entry: groupNode.entries[entryIndex], order: groupNode.entries[entryIndex].order });
                    }
                    for (var groupChildIndex = 0; groupChildIndex < groupNode.children.length; groupChildIndex++) {
                        var child = groupNode.children[groupChildIndex];
                        actions.push({ node: child, order: child.firstOrder });
                    }
                    actions.sort(function (left: any, right: any) { return left.order - right.order; });

                    for (var actionIndex = 0; actionIndex < actions.length; actionIndex++) {
                        var action = actions[actionIndex];
                        if (action.entry) {
                            groupFields.appendChild(action.entry.field);
                        } else if (action.node.total > 1) {
                            appendPrefixBranch(groupFields, action.node);
                        } else {
                            var childField = firstField(action.node);
                            if (childField) {
                                groupFields.appendChild(childField);
                            }
                        }
                    }
                    parent.appendChild(group);
                }

                var prefixRoot: any = { entries: [], children: [] };
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
                        var next: any = null;
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

                var topLevelActions: any[] = [];
                for (var rootEntryIndex = 0; rootEntryIndex < prefixRoot.entries.length; rootEntryIndex++) {
                    topLevelActions.push({ entry: prefixRoot.entries[rootEntryIndex], order: prefixRoot.entries[rootEntryIndex].order });
                }
                for (var rootChildIndex = 0; rootChildIndex < prefixRoot.children.length; rootChildIndex++) {
                    var rootChild = prefixRoot.children[rootChildIndex];
                    topLevelActions.push({ node: rootChild, order: rootChild.firstOrder });
                }
                topLevelActions.sort(function (left: any, right: any) { return left.order - right.order; });
                for (var topLevelIndex = 0; topLevelIndex < topLevelActions.length; topLevelIndex++) {
                    var topLevelAction = topLevelActions[topLevelIndex];
                    if (topLevelAction.entry) {
                        advancedFields.appendChild(topLevelAction.entry.field);
                    } else if (topLevelAction.node.total > 1) {
                        appendPrefixBranch(advancedFields, topLevelAction.node);
                    } else {
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
            id = findFieldByRole('repository-id', '#id', outlet);
            title = findFieldByRole('repository-title', '#title', outlet);

            /**
             * Disables the create button if the id field doesn't have any text.
             */
            var createButton = $(outlet).find('input#create');
            function disableCreateIfEmptyId() {
                createButton.prop('disabled', !(/.+/.test(id.val())));
            }

            // Populate parameters
            var elements = workbench.getQueryStringElements();
            for (var i = 0; elements.length - i; i++) {
                var pair = elements[i].split('=');
                var value = decodeURIComponent(pair[1]).replace(/\+/g, ' ');
                if (pair[0] == 'id') {
                    id.val(value);
                }
                if (pair[0] == 'title') {
                    title.val(value);
                }
            }

            disableCreateIfEmptyId();

            // Calls another function with a delay of 0 msec. (Workaround for 
            // annoying browser behavior.)
            var fields = id;
            fields.on('keydown.wbRoute paste.wbRoute cut.wbRoute', function() {
                setTimeout(disableCreateIfEmptyId, 0);
            });
            return function() {
                fields.off('.wbRoute');
                releaseAdvanced();
            };
        }
    }
}

/**
 * Invoked by the "Create" button on the form for all but 
 * Create form views. Checks with the InfoServlet for the user-provided id
 * for the existence of the id already, giving a chance to back out if it
 * does. An unknown id answers 404 Not Found (500 on older servers).
 */
function checkOverwrite() {
    // 'exists' asks before replacing the configuration; 'absent' creates; anything else sends nothing.
    var found = '';
    var id = workbench.create.id.val();
    $.ajax({
        url: '../' + id + '/info',
        success: function () {
            found = 'exists';
        },
        statusCode: {
            // 404 answers an unknown repository; 500 is kept for servers from before the not-found page.
            404: function () {
                found = 'absent';
            },
            500: function () {
                found = 'absent';
            }
        },
		complete : function(xhr, status) {
            var overwrite: Promise<boolean> = found == 'exists' ? workbench.confirmDialog.open({
                title: 'Replace repository configuration?',
                body: 'A repository with the id "' + id + '" already exists. Creating it again replaces its configuration.',
                confirmLabel: 'Replace configuration',
                danger: true
            }) : Promise.resolve(found == 'absent');
            overwrite.then(function(submit: boolean) {
                if (submit && !id.match(/^[a-z0-9._-]+$/)) {
                    return workbench.confirmDialog.open({
                        title: 'Use this repository id?',
                        body: 'The id "' + id + '" contains characters other than lowercase letters, digits, '
                            + 'dots, underscores and hyphens, which some tools cannot handle.',
                        confirmLabel: 'Create repository',
                        danger: false
                    });
                }
                return submit;
            }).then(function(submit: boolean) {
                if (submit) {
                    $("form[action='create']").submit();
                }
            });
        }
    });
}
