/// <reference path="template.ts" />
/// <reference path="queryStream.ts" />
/// <reference lib="es2015.collection" />
var __makeTemplateObject = (this && this.__makeTemplateObject) || function (cooked, raw) {
    if (Object.defineProperty) { Object.defineProperty(cooked, "raw", { value: raw }); } else { cooked.raw = raw; }
    return cooked;
};
var __assign = (this && this.__assign) || function () {
    __assign = Object.assign || function(t) {
        for (var s, i = 1, n = arguments.length; i < n; i++) {
            s = arguments[i];
            for (var p in s) if (Object.prototype.hasOwnProperty.call(s, p))
                t[p] = s[p];
        }
        return t;
    };
    return __assign.apply(this, arguments);
};
// WARNING: Do not edit the generated workbenchViews.js file. Edit this source
// and run the Workbench TypeScript compiler instead.
// Ordinary-DOM Lit views for the Workbench shell and non-query routes. The
// existing route names, field names, and script-facing selectors stay stable.
var workbench;
(function (workbench) {
    var actionIconCatalog = {
        server: 'M4 4h16v6H4zM4 14h16v6H4zM7 7h.01M7 17h.01',
        repository: 'M5 6c0-1.7 3.1-3 7-3s7 1.3 7 3-3.1 3-7 3-7-1.3-7-3Zm0 0v12c0 1.7 3.1 3 7 3s7-1.3 7-3V6',
        create: 'M12 5v14M5 12h14',
        delete: 'M5 7h14M9 7V4h6v3M7 7l1 13h8l1-13M10 11v5M14 11v5',
        summary: 'M5 19V9M12 19V5M19 19v-7',
        namespaces: 'M8 4H6a2 2 0 0 0-2 2v3a3 3 0 0 1-3 3 3 3 0 0 1 3 3v3a2 2 0 0 0 2 2h2M16 4h2a2 2 0 0 1 2 2v3a3 3 0 0 0 3 3 3 3 0 0 0-3 3v3a2 2 0 0 1-2 2h-2',
        contexts: 'M5 5h14v14H5zM2 2h14v14H2z',
        types: 'm20 13-7 7-9-9V4h7l9 9Z',
        explore: 'M10.5 4a6.5 6.5 0 1 0 0 13 6.5 6.5 0 0 0 0-13ZM16 16l5 5',
        query: 'm5 7 5 5-5 5M13 17h6',
        saved: 'M6 4h12v16l-6-3-6 3V4Z',
        export: 'M12 4v12m-5-5 5 5 5-5M5 20h14',
        update: 'm4 20 4.5-1 10-10a2.1 2.1 0 0 0-3-3l-10 10L4 20Zm10-13 3 3',
        add: 'M12 5v14M5 12h14',
        remove: 'M5 12h14',
        clear: 'M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16Zm-3 5 6 6m0-6-6 6',
        information: 'M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16Zm0 7v5m0-8h.01',
        modify: 'm4 17 4-4 3 3 7-8m0 0h-5m5 0v5',
        upload: 'M12 16V4m0 0L7 9m5-5 5 5M5 20h14',
        download: 'M12 4v12m0 0 5-5m-5 5-5-5M5 20h14',
        execute: 'M8 5 19 12 8 19V5Z',
        explain: 'M8.5 12a2.5 2.5 0 1 1-5 0 2.5 2.5 0 1 1 5 0M20.5 6a2.5 2.5 0 1 1-5 0 2.5 2.5 0 1 1 5 0M20.5 18a2.5 2.5 0 1 1-5 0 2.5 2.5 0 1 1 5 0M8.2 10.8l7.5-3.6m-7.5 5.6 7.5 3.6',
        eye: 'M3 12s3.5-5 9-5 9 5 9 5-3.5 5-9 5-9-5-9-5Zm9-2.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5Z',
        edit: 'm4 20 4.5-1 10-10a2.1 2.1 0 0 0-3-3l-10 10L4 20Zm10-13 3 3',
        chevron: 'm6 9 6 6 6-6',
        cancel: 'M19 12H5m6-6-6 6 6 6',
        close: 'm6 6 12 12M18 6 6 18',
        previous: 'm14 5-7 7 7 7',
        next: 'm10 5 7 7-7 7',
        'source-file': 'M5 4h9l5 5v11H5ZM14 4v5h5M8 13h8M8 17h6',
        'source-url': 'M10 14 8.5 15.5a3.5 3.5 0 0 1-5-5L6 8a3.5 3.5 0 0 1 5 0m3 2 1.5-1.5a3.5 3.5 0 0 1 5 5L18 16a3.5 3.5 0 0 1-5 0m-5 0 8-8',
        'source-text': 'M5 4h14v16H5Zm3 4h8m-8 4h8m-8 4h5',
        settings: 'M12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6Zm7 3a7 7 0 0 0-.2-1.6l2-1.2-2-3.4-2.2 1a7 7 0 0 0-2.7-1.6L14 3h-4l-.3 2.2A7 7 0 0 0 7 6.8l-2.2-1-2 3.4 2 1.2A7 7 0 0 0 4.6 12c0 .6.1 1.1.2 1.6l-2 1.2 2 3.4 2.2-1a7 7 0 0 0 2.7 1.6L10 21h4l.3-2.2a7 7 0 0 0 2.7-1.6l2.2 1 2-3.4-2-1.2c.1-.5.2-1 .2-1.6Z',
        compare: 'M5 7h6v6H5zm8 4h6v6h-6zm-2-1 2 2',
        copy: 'M8 8h11v12H8zM6 16H5a1.5 1.5 0 0 1-1.5-1.5V5A1.5 1.5 0 0 1 5 3.5h8.5A1.5 1.5 0 0 1 15 5v1',
        link: 'M9.5 14.5 8 16a3.5 3.5 0 0 1-5-5l2.5-2.5a3.5 3.5 0 0 1 5 0m4-1 1.5-1.5a3.5 3.5 0 0 1 5 5L18.5 15a3.5 3.5 0 0 1-5 0M8 16l8-8',
        swap: 'M4 7h14m-4-4 4 4-4 4M20 17H6m4 4-4-4 4-4',
        menu: 'M4 6h16M4 12h16M4 18h16'
    };
    function actionIconPath(name) {
        return actionIconCatalog[name] || actionIconCatalog.add;
    }
    var icons;
    (function (icons) {
        function decorateButton(button, name, accessibleName) {
            if (!button || !button.ownerDocument || typeof button.setAttribute !== 'function') {
                return;
            }
            var document = button.ownerDocument;
            var namespace = 'http://www.w3.org/2000/svg';
            var knownName = actionIconCatalog[name] ? name : 'add';
            var svg = button.querySelector ? button.querySelector('svg[data-workbench-icon]') : null;
            if (!svg) {
                svg = document.createElementNS(namespace, 'svg');
                button.insertBefore(svg, button.firstChild || null);
            }
            svg.setAttribute('class', 'workbench-action-icon workbench-action-icon--' + knownName);
            svg.setAttribute('data-workbench-icon', knownName);
            svg.setAttribute('viewBox', '0 0 24 24');
            svg.setAttribute('width', '16');
            svg.setAttribute('height', '16');
            svg.setAttribute('focusable', 'false');
            svg.setAttribute('aria-hidden', 'true');
            while (svg.firstChild) {
                svg.removeChild(svg.firstChild);
            }
            var path = document.createElementNS(namespace, 'path');
            path.setAttribute('d', actionIconPath(knownName));
            svg.appendChild(path);
            button.setAttribute('data-workbench-icon', knownName);
            button.setAttribute('aria-label', accessibleName);
        }
        icons.decorateButton = decorateButton;
        function decorateDisclosureButton(button, accessibleName) {
            if (!button || !button.ownerDocument || typeof button.appendChild !== 'function') {
                return;
            }
            decorateButton(button, 'chevron', accessibleName);
            var svg = button.querySelector ? button.querySelector('svg[data-workbench-icon]') : null;
            if (!svg) {
                return;
            }
            svg.setAttribute('class', 'workbench-action-icon workbench-action-icon--chevron workbench-disclosure-chevron');
            button.appendChild(svg);
        }
        icons.decorateDisclosureButton = decorateDisclosureButton;
    })(icons = workbench.icons || (workbench.icons = {}));
    var views;
    (function (views) {
        var rowRegionsByMount = new WeakMap();
        var titles = {
            summary: 'Summary',
            information: 'System Information',
            repositories: 'List of Repositories',
            create: 'New Repository',
            delete: 'Delete Repository',
            namespaces: 'Namespaces In Repository',
            contexts: 'Contexts In Repository',
            types: 'Types In Repository',
            explore: 'Explore',
            query: 'Query Repository',
            'saved-queries': 'Saved Queries',
            export: 'Export Repository',
            add: 'Add RDF',
            remove: 'Remove Statements',
            clear: 'Clear Repository',
            update: 'Execute SPARQL Update on Repository',
            server: 'Connect to RDF4J Server'
        };
        var defaultMenu = [
            { id: 'server', label: 'Server', icon: 'server', group: 'Server' },
            { id: 'repositories', label: 'Repositories', icon: 'repository', group: 'Repositories' },
            { id: 'create', label: 'Create', icon: 'create', group: 'Repositories' },
            { id: 'delete', label: 'Delete', icon: 'delete', group: 'Repositories' },
            { id: 'summary', label: 'Summary', icon: 'summary', group: 'Browse' },
            { id: 'namespaces', label: 'Namespaces', icon: 'namespaces', group: 'Browse' },
            { id: 'contexts', label: 'Contexts', icon: 'contexts', group: 'Browse' },
            { id: 'types', label: 'Types', icon: 'types', group: 'Browse' },
            { id: 'explore', label: 'Explore', icon: 'explore', group: 'Browse' },
            { id: 'query', label: 'Query', icon: 'query', group: 'Query' },
            { id: 'saved-queries', label: 'Saved Queries', icon: 'saved', group: 'Query' },
            { id: 'export', label: 'Export', icon: 'export', group: 'Query' },
            { id: 'update', label: 'SPARQL Update', icon: 'update', group: 'Modify' },
            { id: 'add', label: 'Add', icon: 'add', group: 'Modify' },
            { id: 'remove', label: 'Remove', icon: 'remove', group: 'Modify' },
            { id: 'clear', label: 'Clear', icon: 'clear', group: 'Modify' },
            { id: 'information', label: 'Information', icon: 'information', group: 'Information' }
        ];
        function text(value) {
            if (value === null || typeof value === 'undefined') {
                return '';
            }
            if (typeof value === 'object' && value.kind) {
                return termText(value);
            }
            return String(value);
        }
        function termText(term) {
            if (!term) {
                return '';
            }
            if (term.kind === 'bnode') {
                return String(term.value || '').indexOf('_:') === 0 ? String(term.value) : '_:' + String(term.value || '');
            }
            if (term.kind === 'triple') {
                return '<< ' + termText(term.subject) + ' ' + termText(term.predicate) + ' '
                    + termText(term.object) + ' >>';
            }
            return typeof term.value === 'undefined' ? '' : String(term.value);
        }
        function field(record, name, fallback) {
            if (record && typeof record[name] !== 'undefined') {
                return record[name];
            }
            if (fallback && record && typeof record[fallback] !== 'undefined') {
                return record[fallback];
            }
            return undefined;
        }
        function records(model) {
            return recordsFromRows(model, model.rows || []);
        }
        function recordsFromRows(model, rows) {
            return rows.map(function (row) {
                var record = {};
                (model.vars || []).forEach(function (name, index) {
                    record[name] = row[index];
                });
                return record;
            });
        }
        function rowCount(model) {
            return typeof model.rowCount === 'number' ? model.rowCount : (model.rows || []).length;
        }
        function rowStart(model) {
            return typeof model.rowStart === 'number' ? model.rowStart : 0;
        }
        function rowHeight(model) {
            return model.viewId === 'saved-queries' ? 240 : 44;
        }
        function meta(model, name, fallback) {
            return field(model.metadata, name, fallback);
        }
        function pageValue(model, name, fallback) {
            var fromMetadata = meta(model, name, fallback);
            return typeof fromMetadata === 'undefined' ? field(firstRecord(model), name, fallback) : fromMetadata;
        }
        function firstRecord(model) {
            var all = records(model);
            return all.length ? all[0] : {};
        }
        function routeTitle(model) {
            return text(meta(model, 'title')) || titles[model.viewId] || 'RDF4J Workbench';
        }
        function urlFor(context, route) {
            var base = (context.basePath || '').replace(/\/+$/, '');
            var special = route === 'server' || route === 'repositories' || route === 'create'
                || route === 'delete' || route === 'information';
            var repositoryBase = base + '/repositories';
            if (special) {
                return repositoryBase + '/NONE/' + route;
            }
            if (context.repositoryId) {
                return repositoryBase + '/' + encodeURIComponent(context.repositoryId) + '/' + route;
            }
            return repositoryBase + '/NONE/' + route;
        }
        function iconPath(name) {
            return actionIconPath(name);
        }
        function icon(runtime, name, className) {
            var h = runtime.html;
            return h(__makeTemplateObject(["<svg class=", "\n                    data-workbench-icon=", " viewBox=\"0 0 24 24\" width=\"16\" height=\"16\" focusable=\"false\" aria-hidden=\"true\">\n                <path d=", "></path>\n            </svg>"], ["<svg class=", "\n                    data-workbench-icon=", " viewBox=\"0 0 24 24\" width=\"16\" height=\"16\" focusable=\"false\" aria-hidden=\"true\">\n                <path d=", "></path>\n            </svg>"]), 'workbench-action-icon workbench-action-icon--' + name + (className ? ' ' + className : ''), name, iconPath(name));
        }
        function disclosureChevron(runtime) {
            return icon(runtime, 'chevron', 'workbench-disclosure-chevron');
        }
        function statusIcon(runtime, status, label) {
            var h = runtime.html;
            var path = status === 'readable'
                ? 'M3 12s3.5-5 9-5 9 5 9 5-3.5 5-9 5-9-5-9-5Zm9-2.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5Z'
                : status === 'writeable'
                    ? 'm4 20 4.5-1 10-10a2.1 2.1 0 0 0-3-3l-10 10L4 20Zm10-13 3 3'
                    : status === 'positive' ? 'm5 12 4 4L19 6' : 'm7 7 10 10m0-10L7 17';
            return h(__makeTemplateObject(["<svg class=", "\n                    viewBox=\"0 0 24 24\" width=\"16\" height=\"16\" role=\"img\" aria-label=", " focusable=\"false\">\n                <title>", "</title><path d=", "></path>\n            </svg>"], ["<svg class=", "\n                    viewBox=\"0 0 24 24\" width=\"16\" height=\"16\" role=\"img\" aria-label=", " focusable=\"false\">\n                <title>", "</title><path d=", "></path>\n            </svg>"]), 'workbench-status-icon workbench-status-icon--' + status, label, label, path);
        }
        function normalizeWorkbench(info, linkedInfo) {
            var source = info || {};
            if (source.workbench && typeof source.workbench === 'object') {
                return source.workbench;
            }
            if (source.metadata && source.metadata.workbench) {
                return source.metadata.workbench;
            }
            // Bootstrap carries both the reduced Info object and a lightweight
            // linked-info wrapper. Keep that ready-to-render summary instead of
            // trying to interpret the wrapper as the original row model.
            if (Array.isArray(source.menu) || Array.isArray(source.menuGroups)
                || Array.isArray(source.menuItems)) {
                return source;
            }
            var infoModel = linkedInfo;
            if (!infoModel) {
                return source;
            }
            var sourceRows = records(infoModel);
            var first = sourceRows.length ? sourceRows[0] : {};
            var valueOf = function (row, name) {
                var value = row[name];
                if (value && value.kind) {
                    return value.value;
                }
                return value;
            };
            var result = {};
            ['id', 'description', 'location', 'server', 'readable', 'writeable'].forEach(function (key) {
                result[key] = valueOf(first, key);
            });
            result.defaults = {};
            var firstValue = function (key) {
                for (var index = 0; index < sourceRows.length; index++) {
                    var candidate = valueOf(sourceRows[index], key);
                    if (typeof candidate !== 'undefined' && candidate !== null) {
                        return candidate;
                    }
                }
                return undefined;
            };
            ['default-limit', 'default-queryLn', 'default-infer', 'default-query-timeout',
                'default-workbench-theme', 'default-Accept', 'default-export-format',
                'default-Content-Type', 'default-download-limit'].forEach(function (key) {
                result.defaults[key] = firstValue(key);
            });
            result.uploadFormats = [];
            result.queryFormats = [];
            result.graphDownloadFormats = [];
            result.tupleDownloadFormats = [];
            result.booleanDownloadFormats = [];
            var menu = [];
            var featureMap = {};
            sourceRows.forEach(function (row) {
                var groupId = valueOf(row, 'menu-group-id');
                var itemId = valueOf(row, 'menu-item-id');
                if (itemId) {
                    var group = menu.filter(function (candidate) { return candidate.id === groupId; })[0];
                    if (!group) {
                        group = {
                            id: groupId,
                            label: valueOf(row, 'menu-group-label'),
                            icon: valueOf(row, 'menu-group-icon'),
                            order: Number(valueOf(row, 'menu-group-order') || 0),
                            items: []
                        };
                        menu.push(group);
                    }
                    group.items.push({
                        id: itemId,
                        label: valueOf(row, 'menu-item-label'),
                        icon: valueOf(row, 'menu-item-icon'),
                        order: Number(valueOf(row, 'menu-item-order') || 0),
                        href: valueOf(row, 'menu-item-href')
                    });
                }
                var featureId = valueOf(row, 'query-feature-id');
                if (featureId) {
                    var raw = valueOf(row, 'query-feature-enabled');
                    featureMap[featureId] = raw === true || String(raw) === 'true';
                }
            });
            ['upload-format', 'query-format', 'graph-download-format', 'tuple-download-format',
                'boolean-download-format'].forEach(function (key) {
                var target = key === 'upload-format' ? result.uploadFormats
                    : key === 'query-format' ? result.queryFormats
                        : key === 'graph-download-format' ? result.graphDownloadFormats
                            : key === 'tuple-download-format' ? result.tupleDownloadFormats
                                : result.booleanDownloadFormats;
                sourceRows.forEach(function (row) {
                    var value = valueOf(row, key);
                    if (value) {
                        target.push(value);
                    }
                });
            });
            result.menu = menu;
            result.queryFeatures = featureMap;
            return result;
        }
        /** Reduce the linked, fixed-size server Info model to shared shell metadata. */
        function linkedInfoMetadata(infoModel) {
            return normalizeWorkbench({ metadata: infoModel.metadata }, infoModel);
        }
        views.linkedInfoMetadata = linkedInfoMetadata;
        function menuEntries(context) {
            var info = normalizeWorkbench(context.workbench, context.linked && context.linked.info);
            var menu = info.menu || info.menuGroups || info.menuItems;
            var hasMenu = Object.prototype.hasOwnProperty.call(info, 'menu')
                || Object.prototype.hasOwnProperty.call(info, 'menuGroups')
                || Object.prototype.hasOwnProperty.call(info, 'menuItems');
            if (hasMenu && Array.isArray(menu) && menu.length === 0) {
                return [];
            }
            if (Array.isArray(menu) && menu.length && menu[0].items) {
                return menu;
            }
            if (Array.isArray(menu) && menu.length && !menu[0].items) {
                var groups_1 = [];
                menu.forEach(function (item) {
                    var id = item.groupId || item.group || item['menu-group-id'] || 'Workbench';
                    var group = groups_1.filter(function (candidate) { return candidate.id === id; })[0];
                    if (!group) {
                        group = {
                            id: id,
                            label: item.groupLabel || item['menu-group-label'] || id,
                            icon: item.groupIcon || item['menu-group-icon'] || 'modify',
                            items: []
                        };
                        groups_1.push(group);
                    }
                    group.items.push(item);
                });
                return groups_1;
            }
            if (Array.isArray(menu) && menu.length) {
                return menu;
            }
            var groups = [];
            defaultMenu.forEach(function (item) {
                var group = groups.filter(function (candidate) { return candidate.id === item.group; })[0];
                if (!group) {
                    group = { id: item.group, label: item.group, icon: item.icon, items: [] };
                    groups.push(group);
                }
                group.items.push(item);
            });
            return groups;
        }
        function isDisabled(item, info) {
            var id = item.id || item['menu-item-id'];
            var readable = info.readable !== false && String(info.readable) !== 'false';
            var writeable = info.writeable !== false && String(info.writeable) !== 'false';
            var readRoutes = {
                summary: true, namespaces: true, contexts: true, types: true, explore: true,
                query: true, 'saved-queries': true, export: true
            };
            var writeRoutes = {
                update: true, add: true, remove: true, clear: true
            };
            return !!item.disabled || (!!readRoutes[id] && !readable) || (!!writeRoutes[id] && !writeable);
        }
        function navigation(context, active, runtime) {
            var h = runtime.html;
            var info = normalizeWorkbench(context.workbench, context.linked && context.linked.info);
            return menuEntries(context).map(function (group) {
                var items = group.items || [];
                var groupId = text(group.id || group['menu-group-id'] || 'workbench');
                var groupLabel = text(group.label || group['menu-group-label'] || 'Workbench');
                if (items.length === 1) {
                    var item = items[0];
                    var id = text(item.id || item['menu-item-id']);
                    var label = text(item.label || item['menu-item-label'] || id);
                    var disabled = isDisabled(item, info);
                    return h(__makeTemplateObject(["<li class=\"workbench-nav-group workbench-nav-group--single\"\n                            data-workbench-menu-group=", " data-workbench-menu-label=", ">\n                        ", "\n                    </li>"], ["<li class=\"workbench-nav-group workbench-nav-group--single\"\n                            data-workbench-menu-group=", " data-workbench-menu-label=", ">\n                        ", "\n                    </li>"]), groupId, groupLabel, disabled
                        ? h(__makeTemplateObject(["<span class=\"disabled\" title=", ">", "", "</span>"], ["<span class=\"disabled\" title=", ">", "", "</span>"]), groupLabel, icon(runtime, item.icon || item['menu-item-icon'] || id), label) : h(__makeTemplateObject(["<a href=", "\n                                    data-workbench-nav-href=", "\n                                    title=", " aria-current=", ">\n                                ", "", "\n                            </a>"], ["<a href=", "\n                                    data-workbench-nav-href=", "\n                                    title=", " aria-current=", ">\n                                ", "", "\n                            </a>"]), item.href || item['menu-item-href'] || urlFor(context, id), item.href || item['menu-item-href'] || urlFor(context, id), groupLabel, active === id ? 'page' : 'false', icon(runtime, item.icon || item['menu-item-icon'] || id), label));
                }
                return h(__makeTemplateObject(["<li class=\"workbench-nav-group\" data-workbench-menu-group=", "\n                        data-workbench-menu-label=", ">\n                    <details class=\"workbench-nav-group__disclosure\">\n                        <summary id=", "\n                                aria-controls=", "\n                                class=\"query-nav-group-label workbench-nav-group__summary\">\n                            ", "\n                            <span class=\"workbench-nav-group__label\">", "</span>\n                            ", "\n                        </summary>\n                        <ul id=", " class=\"group\" aria-label=", ">\n                            ", "\n                        </ul>\n                    </details>\n                </li>"], ["<li class=\"workbench-nav-group\" data-workbench-menu-group=", "\n                        data-workbench-menu-label=", ">\n                    <details class=\"workbench-nav-group__disclosure\">\n                        <summary id=", "\n                                aria-controls=", "\n                                class=\"query-nav-group-label workbench-nav-group__summary\">\n                            ", "\n                            <span class=\"workbench-nav-group__label\">", "</span>\n                            ", "\n                        </summary>\n                        <ul id=", " class=\"group\" aria-label=", ">\n                            ", "\n                        </ul>\n                    </details>\n                </li>"]), groupId, groupLabel, 'workbench-nav-summary-' + groupId, 'workbench-nav-items-' + groupId, icon(runtime, group.icon || group['menu-group-icon'] || 'modify'), groupLabel, icon(runtime, 'chevron', 'workbench-nav-group__chevron workbench-disclosure-chevron'), 'workbench-nav-items-' + groupId, groupLabel, items.map(function (item) {
                    var id = text(item.id || item['menu-item-id']);
                    var label = text(item.label || item['menu-item-label'] || id);
                    var href = item.href || item['menu-item-href'] || urlFor(context, id);
                    return h(__makeTemplateObject(["<li class=", ">\n                                    ", "\n                                </li>"], ["<li class=", ">\n                                    ", "\n                                </li>"]), active === id ? 'current' : '', isDisabled(item, info)
                        ? h(__makeTemplateObject(["<span class=\"disabled\" title=", ">", "", "</span>"], ["<span class=\"disabled\" title=", ">", "", "</span>"]), groupLabel, icon(runtime, item.icon || item['menu-item-icon'] || id), label) : h(__makeTemplateObject(["<a href=", " data-workbench-nav-href=", "\n                                            title=", " aria-current=", ">\n                                            ", "", "\n                                        </a>"], ["<a href=", " data-workbench-nav-href=", "\n                                            title=", " aria-current=", ">\n                                            ", "", "\n                                        </a>"]), href, href, groupLabel, active === id ? 'page' : 'false', icon(runtime, item.icon || item['menu-item-icon'] || id), label));
                }));
            });
        }
        function contextTable(context, active, runtime) {
            var h = runtime.html;
            var info = normalizeWorkbench(context.workbench, context.linked && context.linked.info);
            var repositoryId = text(info.id || context.repositoryId);
            var description = text(info.description || info.title);
            var server = text(info.server || info.location || 'None');
            var repositoryLabel = repositoryId
                ? (description ? description + ' (' + repositoryId + ')' : repositoryId)
                : 'None';
            return h(__makeTemplateObject(["<div id=\"contentheader\" class=\"workbench-context\">\n                <table><tbody>\n                    <tr><th>Server</th><td>", "</td><td class=\"change\"><a href=", ">Change</a></td></tr>\n                    <tr><th>Repository</th><td>", "</td><td class=\"change\"><a href=", ">Change</a></td></tr>\n                    <tr><th>Server user</th><td id=\"selected-user\"></td><td class=\"change\"><a href=", ">Change</a></td></tr>\n                </tbody></table>\n            </div>"], ["<div id=\"contentheader\" class=\"workbench-context\">\n                <table><tbody>\n                    <tr><th>Server</th><td>", "</td><td class=\"change\"><a href=", ">Change</a></td></tr>\n                    <tr><th>Repository</th><td>", "</td><td class=\"change\"><a href=", ">Change</a></td></tr>\n                    <tr><th>Server user</th><td id=\"selected-user\"></td><td class=\"change\"><a href=", ">Change</a></td></tr>\n                </tbody></table>\n            </div>"]), server || 'None', urlFor(context, 'server'), repositoryLabel, urlFor(context, 'repositories'), urlFor(context, 'server'));
        }
        function shell(model, context, runtime, body) {
            var h = runtime.html;
            var title = routeTitle(model);
            return h(__makeTemplateObject(["<div id=\"header\" class=\"workbench-header\">\n                ", "\n                <div id=\"logo\" class=\"workbench-brand\">\n                    <img src=", " alt=\"rdf4j\" />\n                    <img class=\"product\" src=", " alt=\"workbench\" />\n                </div>\n            </div>\n            <details id=\"workbench-navigation-disclosure\" class=\"workbench-navigation-disclosure\" open>\n                <summary id=\"workbench-navigation-summary\">\n                    <svg class=\"workbench-menu-icon\" viewBox=\"0 0 24 24\" width=\"18\" height=\"18\" focusable=\"false\" aria-hidden=\"true\">\n                        <path d=\"M4 6h16M4 12h16M4 18h16\"></path>\n                    </svg><span>Menu</span>\n                    <svg class=\"workbench-menu-chevron workbench-disclosure-chevron\" viewBox=\"0 0 24 24\" width=\"18\" height=\"18\" focusable=\"false\" aria-hidden=\"true\">\n                        <path d=\"m6 9 6 6 6-6\"></path>\n                    </svg>\n                </summary>\n                <div id=\"navigation\" class=\"workbench-nav\"><ul class=\"maingroup\">\n                    ", "\n                </ul></div>\n            </details>\n            <main id=\"content\" class=\"workbench-main\">\n                <h1 id=\"title_heading\">", "</h1>\n                <p id=\"noscript-message\" class=\"ERROR\">Scripting is not enabled. The RDF4J Workbench application requires scripting to be enabled.</p>\n                <div id=\"workbench-page-surface\" class=\"workbench-page-surface\">", "</div>\n            </main>\n            <div id=\"footer\" class=\"workbench-footer\"><div>Copyright \u00A9 Eclipse RDF4J contributors</div></div>"], ["<div id=\"header\" class=\"workbench-header\">\n                ", "\n                <div id=\"logo\" class=\"workbench-brand\">\n                    <img src=", " alt=\"rdf4j\" />\n                    <img class=\"product\" src=", " alt=\"workbench\" />\n                </div>\n            </div>\n            <details id=\"workbench-navigation-disclosure\" class=\"workbench-navigation-disclosure\" open>\n                <summary id=\"workbench-navigation-summary\">\n                    <svg class=\"workbench-menu-icon\" viewBox=\"0 0 24 24\" width=\"18\" height=\"18\" focusable=\"false\" aria-hidden=\"true\">\n                        <path d=\"M4 6h16M4 12h16M4 18h16\"></path>\n                    </svg><span>Menu</span>\n                    <svg class=\"workbench-menu-chevron workbench-disclosure-chevron\" viewBox=\"0 0 24 24\" width=\"18\" height=\"18\" focusable=\"false\" aria-hidden=\"true\">\n                        <path d=\"m6 9 6 6 6-6\"></path>\n                    </svg>\n                </summary>\n                <div id=\"navigation\" class=\"workbench-nav\"><ul class=\"maingroup\">\n                    ", "\n                </ul></div>\n            </details>\n            <main id=\"content\" class=\"workbench-main\">\n                <h1 id=\"title_heading\">", "</h1>\n                <p id=\"noscript-message\" class=\"ERROR\">Scripting is not enabled. The RDF4J Workbench application requires scripting to be enabled.</p>\n                <div id=\"workbench-page-surface\" class=\"workbench-page-surface\">", "</div>\n            </main>\n            <div id=\"footer\" class=\"workbench-footer\"><div>Copyright \u00A9 Eclipse RDF4J contributors</div></div>"]), contextTable(context, model.viewId, runtime), context.basePath + '/images/logo.png', context.basePath + '/images/product.png', navigation(context, model.viewId, runtime), title, body);
        }
        function workbenchData(context) {
            return normalizeWorkbench(context.workbench, context.linked && context.linked.info);
        }
        function table(runtime, model, context, options) {
            var h = runtime.html;
            var columns = model.vars || [];
            var allRows = records(model);
            var total = rowCount(model);
            var emptyText = options && options.emptyText ? options.emptyText : 'No results to display.';
            var regions = context.rowRegions;
            if (regions) {
                if (!regions.tableBody) {
                    regions.tableBody = regions.document.createElement('tbody');
                }
                regions.renderTableRows = function () { return runtime.render(tableRows(runtime, model, context, options, records(model), rowStart(model), rowCount(model), emptyText), regions.tableBody); };
            }
            return h(__makeTemplateObject(["<table class=\"data\" data-workbench-row-table=", ">\n                ", "\n                ", "\n            </table>"], ["<table class=\"data\" data-workbench-row-table=", ">\n                ", "\n                ", "\n            </table>"]), model.rowStore && total ? 'true' : runtime.nothing, columns.length ? h(__makeTemplateObject(["<thead><tr>", "</tr></thead>"], ["<thead><tr>", "</tr></thead>"]), columns.map(function (name) { return h(__makeTemplateObject(["<th scope=\"col\">", "</th>"], ["<th scope=\"col\">", "</th>"]), name); })) : '', regions ? regions.tableBody
                : h(__makeTemplateObject(["<tbody>", "</tbody>"], ["<tbody>", "</tbody>"]), tableRows(runtime, model, context, options, allRows, rowStart(model), total, emptyText)));
        }
        function tableRows(runtime, model, context, options, allRows, start, total, emptyText) {
            var h = runtime.html;
            var columns = model.vars || [];
            var rowHeightValue = rowHeight(model);
            if (!total && !allRows.length) {
                return h(__makeTemplateObject(["<tr class=\"workbench-empty-row\"><td role=\"status\" colspan=", ">", "</td></tr>"], ["<tr class=\"workbench-empty-row\"><td role=\"status\" colspan=", ">", "</td></tr>"]), Math.max(1, columns.length), emptyText);
            }
            if (!allRows.length) {
                return h(__makeTemplateObject(["<tr class=\"workbench-pending-row\"><td role=\"status\" colspan=", ">Loading rows...</td></tr>"], ["<tr class=\"workbench-pending-row\"><td role=\"status\" colspan=", ">Loading rows...</td></tr>"]), Math.max(1, columns.length));
            }
            var before = typeof model.rowTopSpacer === 'number'
                ? model.rowTopSpacer : Math.max(0, start) * rowHeightValue;
            var after = typeof model.rowBottomSpacer === 'number'
                ? model.rowBottomSpacer : Math.max(0, total - start - allRows.length) * rowHeightValue;
            return h(__makeTemplateObject(["", "\n                ", "\n                ", ""], ["", "\n                ", "\n                ", ""]), before ? h(__makeTemplateObject(["<tr class=\"workbench-virtual-spacer\" aria-hidden=\"true\"><td colspan=", "\n                    style=", "></td></tr>"], ["<tr class=\"workbench-virtual-spacer\" aria-hidden=\"true\"><td colspan=", "\n                    style=", "></td></tr>"]), Math.max(1, columns.length), 'height:' + before + 'px;padding:0;border:0') : '', allRows.map(function (record, relativeIndex) { return h(__makeTemplateObject(["<tr data-workbench-row-index=", ">\n                    ", "\n                </tr>"], ["<tr data-workbench-row-index=", ">\n                    ", "\n                </tr>"]), start + relativeIndex, columns.map(function (name) {
                var cell = record[name];
                if (options && options.status && (name === 'readable' || name === 'writeable')) {
                    var status_1 = name === 'readable' ? 'readable' : 'writeable';
                    return h(__makeTemplateObject(["<td data-label=", ">", "</td>"], ["<td data-label=", ">", "</td>"]), name, statusIcon(runtime, (cell === true || text(cell) === 'true') ? status_1 : 'negative', (cell === true || text(cell) === 'true') ? (name === 'readable' ? 'Readable' : 'Writeable') : 'No'));
                }
                if (options && options.repository && name === 'id') {
                    var id = text(cell);
                    var href = '../' + encodeURIComponent(id) + '/summary';
                    return h(__makeTemplateObject(["<td data-label=", "><a href=", ">", "</a></td>"], ["<td data-label=", "><a href=", ">", "</a></td>"]), name, href, id);
                }
                return h(__makeTemplateObject(["<td data-label=", ">", "</td>"], ["<td data-label=", ">", "</td>"]), name, renderTerm(runtime, cell, context, !!(options && options.linkTerms)));
            })); }), after ? h(__makeTemplateObject(["<tr class=\"workbench-virtual-spacer\" aria-hidden=\"true\"><td colspan=", "\n                    style=", "></td></tr>"], ["<tr class=\"workbench-virtual-spacer\" aria-hidden=\"true\"><td colspan=", "\n                    style=", "></td></tr>"]), Math.max(1, columns.length), 'height:' + after + 'px;padding:0;border:0') : '');
        }
        function renderTerm(runtime, value, context, link) {
            var h = runtime.html;
            if (!value || typeof value !== 'object' || !value.kind) {
                return text(value);
            }
            var display = termText(value);
            if (link && (value.kind === 'iri' || value.kind === 'literal' || value.kind === 'bnode')) {
                var resource = workbench.queryStream.exploreResource(value);
                return h(__makeTemplateObject(["<a href=", ">", "</a>"], ["<a href=", ">", "</a>"]), 'explore?resource=' + encodeURIComponent(resource), display);
            }
            if (value.kind === 'literal' && value.language) {
                return h(__makeTemplateObject(["<span class=\"rdf-literal\" lang=", ">", "<span class=\"rdf-language\">@", "</span></span>"], ["<span class=\"rdf-literal\" lang=", ">", "<span class=\"rdf-language\">@", "</span></span>"]), value.language, display, value.language);
            }
            return display;
        }
        function simpleSection(runtime, title, rows, className, id, island) {
            if (island === void 0) { island = true; }
            var h = runtime.html;
            var content = h(__makeTemplateObject(["<h2>", "</h2><table class=\"simple\"><tbody>\n                    ", "\n                </tbody></table>"], ["<h2>", "</h2><table class=\"simple\"><tbody>\n                    ", "\n                </tbody></table>"]), title, rows.map(function (row) { return h(__makeTemplateObject(["<tr><th>", "</th><td>", "</td></tr>"], ["<tr><th>", "</th><td>", "</td></tr>"]), row[0], text(row[1])); }));
            var sectionClass = [island ? 'workbench-island' : '', className || '']
                .filter(function (part) { return part.length > 0; }).join(' ');
            return id
                ? h(__makeTemplateObject(["<section id=", " class=", ">", "</section>"], ["<section id=", " class=", ">", "</section>"]), id, sectionClass, content) : h(__makeTemplateObject(["<section class=", ">", "</section>"], ["<section class=", ">", "</section>"]), sectionClass, content);
        }
        function summaryPage(runtime, model, context) {
            var h = runtime.html;
            var row = firstRecord(model);
            var config = text(meta(model, 'config-model-turtle', 'config-model'));
            return h(__makeTemplateObject(["", "\n                ", "\n                ", ""], ["", "\n                ", "\n                ", ""]), simpleSection(runtime, 'Repository Location', [
                ['Repository ID', field(row, 'id')],
                ['Repository title', field(row, 'description')],
                ['Repository location', field(row, 'location')],
                ['Server', field(row, 'server')]
            ], 'workbench-summary-location'), simpleSection(runtime, 'Repository Size', [
                ['Repository size', field(row, 'size')],
                ['Named contexts', field(row, 'contexts')]
            ], 'workbench-summary-size'), config ? h(__makeTemplateObject(["<section class=\"workbench-island workbench-summary-config\">\n                    <details id=\"summary-config-model\" class=\"workbench-options\">\n                        <summary>Config Model", "</summary>\n                        <pre role=\"region\">", "</pre>\n                    </details>\n                </section>"], ["<section class=\"workbench-island workbench-summary-config\">\n                    <details id=\"summary-config-model\" class=\"workbench-options\">\n                        <summary>Config Model", "</summary>\n                        <pre role=\"region\">", "</pre>\n                    </details>\n                </section>"]), icon(runtime, 'chevron', 'workbench-disclosure-chevron'), config) : '');
        }
        function informationPage(runtime, model) {
            var row = firstRecord(model);
            return runtime.html(__makeTemplateObject(["<div id=\"workbench-information\" class=\"workbench-island\">\n                ", "\n                ", "\n                ", "\n            </div>"], ["<div id=\"workbench-information\" class=\"workbench-island\">\n                ", "\n                ", "\n                ", "\n            </div>"]), simpleSection(runtime, 'Application Information', [
                ['Application name', 'RDF4J Workbench'], ['Application version', field(row, 'version')]
            ], 'workbench-information-section', 'information-application', false), simpleSection(runtime, 'Runtime Information', [
                ['Operating system', field(row, 'os')], ['Java runtime', field(row, 'jvm')], ['Process user', field(row, 'user')]
            ], 'workbench-information-section', 'information-runtime', false), simpleSection(runtime, 'Memory', [
                ['Memory used', field(row, 'memory-used')], ['Maximum memory', field(row, 'maximum-memory')]
            ], 'workbench-information-section', 'information-memory', false));
        }
        function repositoriesPage(runtime, model, context) {
            var h = runtime.html;
            return h(__makeTemplateObject(["<section id=\"repositories-results\" class=\"workbench-island workbench-responsive-records\">\n                ", "\n            </section>"], ["<section id=\"repositories-results\" class=\"workbench-island workbench-responsive-records\">\n                ", "\n            </section>"]), rowCount(model) ? table(runtime, model, context, { status: true, repository: true })
                : h(__makeTemplateObject(["<p class=\"workbench-empty\" role=\"status\">No repositories are available.</p>"], ["<p class=\"workbench-empty\" role=\"status\">No repositories are available.</p>"])));
        }
        function createPage(runtime, model, context) {
            var h = runtime.html;
            var rows = records(model);
            if (model.vars.indexOf('fieldId') >= 0) {
                return createTemplateForm(runtime, model, rows, context);
            }
            if (model.vars.indexOf('location') >= 0 && model.vars.indexOf('description') >= 0
                && model.vars.indexOf('id') >= 0) {
                return federateForm(runtime, rows, context);
            }
            return h(__makeTemplateObject(["<form id=\"create-type-form\" action=\"create\" method=\"get\" class=\"workbench-island\">\n                <div class=\"workbench-field\"><label for=\"type\">Repository type</label>\n                    <select id=\"type\" name=\"type\">\n                        ", "\n                    </select>\n                </div>\n                <div class=\"workbench-form-actions\">\n                    <span class=\"workbench-action workbench-action--primary\"><label class=\"workbench-action-hit-area\">\n                        ", "<span class=\"workbench-action-label\"><input type=\"submit\" name=\"next\" value=\"Next\" /></span>\n                    </label></span>\n                </div>\n            </form>"], ["<form id=\"create-type-form\" action=\"create\" method=\"get\" class=\"workbench-island\">\n                <div class=\"workbench-field\"><label for=\"type\">Repository type</label>\n                    <select id=\"type\" name=\"type\">\n                        ", "\n                    </select>\n                </div>\n                <div class=\"workbench-form-actions\">\n                    <span class=\"workbench-action workbench-action--primary\"><label class=\"workbench-action-hit-area\">\n                        ", "<span class=\"workbench-action-label\"><input type=\"submit\" name=\"next\" value=\"Next\" /></span>\n                    </label></span>\n                </div>\n            </form>"]), rows.map(function (row) { return h(__makeTemplateObject(["<option value=", ">", "</option>"], ["<option value=", ">", "</option>"]), text(row.type), text(row.label)); }), icon(runtime, 'next'));
        }
        function federateForm(runtime, rows, context) {
            var h = runtime.html;
            return h(__makeTemplateObject(["<form action=\"create\" method=\"post\">\n                <table class=\"dataentry\" data-advanced-label=\"Advanced settings\"><tbody>\n                    <tr><th><label for=\"type\">Repository type</label></th><td><select id=\"type\" name=\"type\"><option value=\"federate\">Federation Store</option></select></td></tr>\n                    <tr><th><label for=\"id\">Repository ID</label></th><td><input id=\"id\" name=\"Local repository ID\" type=\"text\" value=\"fed\" data-field-role=\"repository-id\" /></td><td><span id=\"recurse-message\" class=\"error\" hidden>Federation ID may not match an existing ID.</span></td></tr>\n                    <tr><th><label for=\"title\">Repository title</label></th><td><input id=\"title\" name=\"Repository title\" type=\"text\" value=\"Federation\" data-field-role=\"repository-title\" /></td></tr>\n                    <tr data-field-role=\"federation-member\"><th>Federation members</th><td><div class=\"workbench-choice-list\">\n                        ", "\n                    </div></td><td><span class=\"error\" id=\"create-feedback\">Select at least two federation members.</span></td></tr>\n                </tbody></table>\n                ", "\n            </form>"], ["<form action=\"create\" method=\"post\">\n                <table class=\"dataentry\" data-advanced-label=\"Advanced settings\"><tbody>\n                    <tr><th><label for=\"type\">Repository type</label></th><td><select id=\"type\" name=\"type\"><option value=\"federate\">Federation Store</option></select></td></tr>\n                    <tr><th><label for=\"id\">Repository ID</label></th><td><input id=\"id\" name=\"Local repository ID\" type=\"text\" value=\"fed\" data-field-role=\"repository-id\" /></td><td><span id=\"recurse-message\" class=\"error\" hidden>Federation ID may not match an existing ID.</span></td></tr>\n                    <tr><th><label for=\"title\">Repository title</label></th><td><input id=\"title\" name=\"Repository title\" type=\"text\" value=\"Federation\" data-field-role=\"repository-title\" /></td></tr>\n                    <tr data-field-role=\"federation-member\"><th>Federation members</th><td><div class=\"workbench-choice-list\">\n                        ", "\n                    </div></td><td><span class=\"error\" id=\"create-feedback\">Select at least two federation members.</span></td></tr>\n                </tbody></table>\n                ", "\n            </form>"]), rows.filter(function (row) { return text(row.id) !== 'SYSTEM'; }).map(function (row) { return h(__makeTemplateObject(["<label class=\"workbench-choice\">\n                            <input type=\"checkbox\" class=\"memberID\" name=\"memberID\" value=", " data-field-role=\"federation-member\" />\n                            <span>", "", "</span>\n                        </label>"], ["<label class=\"workbench-choice\">\n                            <input type=\"checkbox\" class=\"memberID\" name=\"memberID\" value=", " data-field-role=\"federation-member\" />\n                            <span>", "", "</span>\n                        </label>"]), text(row.id), text(row.id), text(row.description) ? ' — ' + text(row.description) : ''); }), createActions(runtime, false, context));
        }
        function optionLabel(value) {
            if (value === '__workbench_unset__') {
                return 'Repository default';
            }
            if (value === 'true') {
                return 'True';
            }
            if (value === 'false') {
                return 'False';
            }
            return value;
        }
        function createTemplateForm(runtime, model, rows, context) {
            var h = runtime.html;
            var first = rows[0] || {};
            var groups = [];
            rows.forEach(function (row) {
                var id = text(row.fieldId);
                var group = groups.filter(function (candidate) { return candidate.id === id; })[0];
                if (!group) {
                    group = { id: id, row: row, options: [] };
                    groups.push(group);
                }
                group.options.push(row);
            });
            var fieldRows = groups.map(function (group) { return renderTemplateField(runtime, group); });
            return h(__makeTemplateObject(["<form action=\"create\" method=\"post\">\n                <table class=\"dataentry\" data-advanced-label=\"Advanced settings\"><tbody>\n                    <tr><th><label for=\"type\">Repository type</label></th><td><select id=\"type\" name=\"type\">\n                        <option value=", ">", "</option>\n                    </select></td></tr>\n                    ", "\n                </tbody></table>\n                ", "\n            </form>"], ["<form action=\"create\" method=\"post\">\n                <table class=\"dataentry\" data-advanced-label=\"Advanced settings\"><tbody>\n                    <tr><th><label for=\"type\">Repository type</label></th><td><select id=\"type\" name=\"type\">\n                        <option value=", ">", "</option>\n                    </select></td></tr>\n                    ", "\n                </tbody></table>\n                ", "\n            </form>"]), text(first.templateType), text(first.templateLabel), fieldRows, createActions(runtime, true, context));
        }
        function renderTemplateField(runtime, group) {
            var h = runtime.html;
            var row = group.row;
            var id = text(row.fieldId);
            var name = text(row.fieldName);
            var type = text(row.fieldType);
            var property = text(row.fieldProperty);
            var role = text(row.fieldRole);
            var selected = group.options.filter(function (option) { return text(option.selected) === 'true'; })[0]
                || group.options[0];
            var value = selected ? text(selected.value) : '';
            var common = {};
            if (property) {
                common['data-config-property'] = property;
            }
            if (role) {
                common['data-field-role'] = role;
            }
            if (type === 'select') {
                return h(__makeTemplateObject(["<tr><th><label for=", ">", "</label></th><td><select id=", " name=", "\n                        data-config-property=", " data-field-role=", ">\n                    ", "\n                </select></td></tr>"], ["<tr><th><label for=", ">", "</label></th><td><select id=", " name=", "\n                        data-config-property=", " data-field-role=", ">\n                    ", "\n                </select></td></tr>"]), id, name, id, name, property || runtime.nothing, role || runtime.nothing, group.options.map(function (option) { return h(__makeTemplateObject(["<option value=", " ?selected=", ">\n                        ", "</option>"], ["<option value=", " ?selected=", ">\n                        ", "</option>"]), text(option.value), text(option.selected) === 'true', optionLabel(text(option.value))); }));
            }
            if (type === 'radio') {
                var ordered = group.options.slice();
                if (ordered.length === 2 && ordered.some(function (option) { return text(option.value) === 'true'; })
                    && ordered.some(function (option) { return text(option.value) === 'false'; })) {
                    ordered.sort(function (left) { return text(left.value) === 'true' ? -1 : 1; });
                }
                return h(__makeTemplateObject(["<tr><th>", "</th><td>", "\n                </td></tr>"], ["<tr><th>", "</th><td>", "\n                </td></tr>"]), name, ordered.map(function (option, index) { return h(__makeTemplateObject(["<label class=\"workbench-choice\">\n                    <input type=\"radio\" id=", " name=", " value=", "\n                        data-config-property=", " data-field-role=", "\n                        ?checked=", " />\n                    <span>", "</span></label>"], ["<label class=\"workbench-choice\">\n                    <input type=\"radio\" id=", " name=", " value=", "\n                        data-config-property=", " data-field-role=", "\n                        ?checked=", " />\n                    <span>", "</span></label>"]), id + '-' + (index + 1), name, text(option.value), property || runtime.nothing, role || runtime.nothing, text(option.selected) === 'true', optionLabel(text(option.value))); }));
            }
            if (type === 'textarea') {
                return h(__makeTemplateObject(["<tr><th><label for=", ">", "</label></th><td><textarea id=", " name=", "\n                    rows=", " cols=", "\n                    placeholder=", "\n                    data-config-property=", " data-field-role=", ">", "</textarea>\n                </td></tr>"], ["<tr><th><label for=", ">", "</label></th><td><textarea id=", " name=", "\n                    rows=", " cols=", "\n                    placeholder=", "\n                    data-config-property=", " data-field-role=", ">", "</textarea>\n                </td></tr>"]), id, name, id, name, Number(text(row.rows)) || 6, Number(text(row.cols)) || 70, text(row.placeholder) || runtime.nothing, property || runtime.nothing, role || runtime.nothing, value);
            }
            return h(__makeTemplateObject(["<tr><th><label for=", ">", "</label></th><td><input type=\"text\" id=", " name=", "\n                size=", " value=", " placeholder=", "\n                data-config-property=", " data-field-role=", " /></td></tr>"], ["<tr><th><label for=", ">", "</label></th><td><input type=\"text\" id=", " name=", "\n                size=", " value=", " placeholder=", "\n                data-config-property=", " data-field-role=", " /></td></tr>"]), id, name, id, name, Number(text(row.size)) || 32, value, text(row.placeholder) || runtime.nothing, property || runtime.nothing, role || runtime.nothing);
        }
        function createActions(runtime, confirmOverwrite, context) {
            var h = runtime.html;
            var cancel = urlFor(context, 'repositories');
            var createType = confirmOverwrite ? 'button' : 'submit';
            var createHandler = confirmOverwrite ? function (event) {
                event.preventDefault();
                var globalWindow = typeof window !== 'undefined' ? window : null;
                if (globalWindow && typeof globalWindow.checkOverwrite === 'function') {
                    globalWindow.checkOverwrite();
                }
                else {
                    var form = event.currentTarget.closest('form');
                    if (form) {
                        form.requestSubmit();
                    }
                }
            } : undefined;
            return h(__makeTemplateObject(["<div class=\"workbench-form-actions\">\n                <span class=\"workbench-action workbench-action--secondary\" data-workbench-action=\"cancel\">\n                    <label class=\"workbench-action-hit-area\">", "\n                        <span class=\"workbench-action-label\"><input type=\"button\" value=\"Cancel\" data-href=", "\n                            @click=", " /></span>\n                    </label>\n                </span>\n                <span class=\"workbench-action workbench-action--primary\" data-workbench-action=\"create\">\n                    <label class=\"workbench-action-hit-area\">", "\n                        <span class=\"workbench-action-label\"><input id=\"create\" type=", " value=\"Create\" @click=", " /></span>\n                    </label>\n                </span>\n            </div>"], ["<div class=\"workbench-form-actions\">\n                <span class=\"workbench-action workbench-action--secondary\" data-workbench-action=\"cancel\">\n                    <label class=\"workbench-action-hit-area\">", "\n                        <span class=\"workbench-action-label\"><input type=\"button\" value=\"Cancel\" data-href=", "\n                            @click=", " /></span>\n                    </label>\n                </span>\n                <span class=\"workbench-action workbench-action--primary\" data-workbench-action=\"create\">\n                    <label class=\"workbench-action-hit-area\">", "\n                        <span class=\"workbench-action-label\"><input id=\"create\" type=", " value=\"Create\" @click=", " /></span>\n                    </label>\n                </span>\n            </div>"]), icon(runtime, 'cancel'), cancel, function () { window.location.href = cancel; }, icon(runtime, 'create'), createType, createHandler);
        }
        function pickerWindow(runtime, model, name, label) {
            var h = runtime.html;
            var total = rowCount(model);
            var start = model.pickerStart || 0;
            var count = (model.pickerRows || []).length;
            var end = Math.min(total, start + count);
            if (total <= model.pickerPageSize) {
                return '';
            }
            return h(__makeTemplateObject(["<div class=\"workbench-form-actions workbench-window-controls\" role=\"group\" aria-label=", ">\n                <button type=\"button\" data-workbench-window-picker=", " data-workbench-window-action=\"previous\"\n                    ?disabled=", ">Previous</button>\n                <span role=\"status\">Showing ", "\u2013", " of ", " ", "</span>\n                <button type=\"button\" data-workbench-window-picker=", " data-workbench-window-action=\"next\"\n                    ?disabled=", ">Next</button>\n            </div>"], ["<div class=\"workbench-form-actions workbench-window-controls\" role=\"group\" aria-label=", ">\n                <button type=\"button\" data-workbench-window-picker=", " data-workbench-window-action=\"previous\"\n                    ?disabled=", ">Previous</button>\n                <span role=\"status\">Showing ", "\u2013", " of ", " ", "</span>\n                <button type=\"button\" data-workbench-window-picker=", " data-workbench-window-action=\"next\"\n                    ?disabled=", ">Next</button>\n            </div>"]), label + ' pages', name, start === 0, start + 1, end, total, label, name, end >= total);
        }
        function deletePage(runtime, model) {
            var h = runtime.html;
            var options = recordsFromRows(model, model.pickerRows || []).filter(function (row) { return text(row.id) !== 'SYSTEM'; });
            var selectedId = text(model.metadata.selectedRepositoryId || pageValue(model, 'id'));
            var selectedRepository = model.metadata.selectedRepository;
            var selectedVisible = options.some(function (row) { return text(row.id) === selectedId; });
            return h(__makeTemplateObject(["<form id=\"delete-form\" class=\"workbench-island\" action=\"delete\" method=\"post\"\n                    @submit=", ">\n                <div class=\"workbench-field\"><label for=\"id\">Repository</label>\n                    <select id=\"id\" name=\"id\" data-workbench-window-picker=\"repositories\" @change=", "><option value=\"\" ?selected=", "></option>\n                        ", "\n                        ", "\n                    </select>\n                    ", "\n                </div>\n                <div id=\"delete-actions\" class=\"workbench-form-actions\"><span class=\"workbench-action workbench-action--danger\">\n                    <label class=\"workbench-action-hit-area\">", "\n                        <span class=\"workbench-action-label\"><input type=\"submit\" value=\"Delete\" /></span>\n                    </label></span>\n                </div><span id=\"delete-feedback\" class=\"error\" role=\"alert\"></span>\n            </form>"], ["<form id=\"delete-form\" class=\"workbench-island\" action=\"delete\" method=\"post\"\n                    @submit=", ">\n                <div class=\"workbench-field\"><label for=\"id\">Repository</label>\n                    <select id=\"id\" name=\"id\" data-workbench-window-picker=\"repositories\" @change=", "><option value=\"\" ?selected=", "></option>\n                        ", "\n                        ", "\n                    </select>\n                    ", "\n                </div>\n                <div id=\"delete-actions\" class=\"workbench-form-actions\"><span class=\"workbench-action workbench-action--danger\">\n                    <label class=\"workbench-action-hit-area\">", "\n                        <span class=\"workbench-action-label\"><input type=\"submit\" value=\"Delete\" /></span>\n                    </label></span>\n                </div><span id=\"delete-feedback\" class=\"error\" role=\"alert\"></span>\n            </form>"]), function (event) {
                var globalWindow = typeof window !== 'undefined' ? window : null;
                if (globalWindow && typeof globalWindow.checkIsSafeToDelete === 'function') {
                    globalWindow.checkIsSafeToDelete(event);
                }
            }, function (event) {
                var selected = options.filter(function (row) { return text(row.id) === event.target.value; })[0];
                model.metadata.selectedRepositoryId = event.target.value;
                model.metadata.selectedRepository = selected || null;
            }, !selectedId, selectedId && !selectedVisible && selectedRepository
                ? h(__makeTemplateObject(["<option value=", " selected>", " \u2014 ", "</option>"], ["<option value=", " selected>", " \u2014 ", "</option>"]), selectedId, text(selectedRepository.id), text(selectedRepository.description)) : '', options.map(function (row) { return h(__makeTemplateObject(["<option value=", " ?selected=", ">\n                            ", " \u2014 ", "</option>"], ["<option value=", " ?selected=", ">\n                            ", " \u2014 ", "</option>"]), text(row.id), text(row.id) === selectedId, text(row.id), text(row.description)); }), pickerWindow(runtime, model, 'repositories', 'repositories'), icon(runtime, 'delete'));
        }
        function namespacesPage(runtime, model, context) {
            var h = runtime.html;
            var entries = recordsFromRows(model, model.pickerRows || []);
            var selectedNamespace = text(model.metadata.selectedNamespaceValue || pageValue(model, 'namespace'));
            var selectedEntry = model.metadata.selectedNamespace;
            var selectedVisible = entries.some(function (row) { return text(row.namespace) === selectedNamespace; });
            return h(__makeTemplateObject(["", "\n                <form id=\"namespaces-form\" class=\"workbench-island\" action=\"namespaces\" method=\"post\">\n                    <div class=\"workbench-form-grid\">\n                        <div class=\"workbench-field\"><label for=\"prefix\">Prefix</label>\n                            <div class=\"workbench-inline-controls\">\n                                <input type=\"text\" id=\"prefix\" name=\"prefix\" size=\"8\" value=", " />\n                                <select id=\"prefix-select\" data-workbench-window-picker=\"namespaces\" @change=", ">\n                                    <option value=\"\" ?selected=", "></option>\n                                    ", "\n                                    ", "\n                                </select>\n                                ", "\n                            </div>\n                        </div>\n                        <div class=\"workbench-field\"><label for=\"namespace\">Namespace</label>\n                            <input type=\"text\" id=\"namespace\" name=\"namespace\" size=\"48\" value=", " />\n                        </div>\n                    </div>\n                    <div class=\"workbench-form-actions\">\n                        <span class=\"workbench-action workbench-action--primary\"><label class=\"workbench-action-hit-area\">", "\n                            <span class=\"workbench-action-label\"><button type=\"submit\" value=\"Update\">Update</button></span></label></span>\n                        <span class=\"workbench-action workbench-action--danger\"><label class=\"workbench-action-hit-area\">", "\n                            <span class=\"workbench-action-label\"><button type=\"submit\" value=\"Delete\" @click=", ">Delete</button></span></label></span>\n                    </div>\n                </form>\n                <section id=\"namespaces-results\" class=\"workbench-island workbench-responsive-records\">\n                    ", "\n                </section>"], ["", "\n                <form id=\"namespaces-form\" class=\"workbench-island\" action=\"namespaces\" method=\"post\">\n                    <div class=\"workbench-form-grid\">\n                        <div class=\"workbench-field\"><label for=\"prefix\">Prefix</label>\n                            <div class=\"workbench-inline-controls\">\n                                <input type=\"text\" id=\"prefix\" name=\"prefix\" size=\"8\" value=", " />\n                                <select id=\"prefix-select\" data-workbench-window-picker=\"namespaces\" @change=", ">\n                                    <option value=\"\" ?selected=", "></option>\n                                    ", "\n                                    ", "\n                                </select>\n                                ", "\n                            </div>\n                        </div>\n                        <div class=\"workbench-field\"><label for=\"namespace\">Namespace</label>\n                            <input type=\"text\" id=\"namespace\" name=\"namespace\" size=\"48\" value=", " />\n                        </div>\n                    </div>\n                    <div class=\"workbench-form-actions\">\n                        <span class=\"workbench-action workbench-action--primary\"><label class=\"workbench-action-hit-area\">", "\n                            <span class=\"workbench-action-label\"><button type=\"submit\" value=\"Update\">Update</button></span></label></span>\n                        <span class=\"workbench-action workbench-action--danger\"><label class=\"workbench-action-hit-area\">", "\n                            <span class=\"workbench-action-label\"><button type=\"submit\" value=\"Delete\" @click=", ">Delete</button></span></label></span>\n                    </div>\n                </form>\n                <section id=\"namespaces-results\" class=\"workbench-island workbench-responsive-records\">\n                    ", "\n                </section>"]), pageValue(model, 'error-message') ? h(__makeTemplateObject(["<p class=\"error\" role=\"alert\">", "</p>"], ["<p class=\"error\" role=\"alert\">", "</p>"]), text(pageValue(model, 'error-message'))) : '', text(pageValue(model, 'prefix')), function (event) {
                var selected = entries.filter(function (row) { return text(row.namespace) === event.target.value; })[0];
                model.metadata.selectedNamespaceValue = event.target.value;
                model.metadata.selectedNamespace = selected || null;
                var globalWorkbench = window.workbench;
                if (globalWorkbench.namespaces && globalWorkbench.namespaces.updatePrefix) {
                    globalWorkbench.namespaces.updatePrefix();
                }
            }, !selectedNamespace, selectedNamespace && !selectedVisible && selectedEntry
                ? h(__makeTemplateObject(["<option value=", " selected>", "</option>"], ["<option value=", " selected>", "</option>"]), selectedNamespace, text(selectedEntry.prefix)) : '', entries.map(function (row) { return h(__makeTemplateObject(["<option value=", "\n                                        ?selected=", ">", "</option>"], ["<option value=", "\n                                        ?selected=", ">", "</option>"]), text(row.namespace), text(row.namespace) === selectedNamespace, text(row.prefix)); }), pickerWindow(runtime, model, 'namespaces', 'namespaces'), text(pageValue(model, 'namespace')), icon(runtime, 'update'), icon(runtime, 'delete'), function () {
                var namespace = document.getElementById('namespace');
                if (namespace) {
                    namespace.value = '';
                }
            }, table(runtime, model, context, { emptyText: 'No results to display.' }));
        }
        function recordBrowsePage(runtime, model, context) {
            var h = runtime.html;
            var route = model.viewId;
            return h(__makeTemplateObject(["<section id=", " class=\"workbench-island workbench-responsive-records\">\n                ", "\n            </section>"], ["<section id=", " class=\"workbench-island workbench-responsive-records\">\n                ", "\n            </section>"]), route + '-results', table(runtime, model, context, { emptyText: 'No results to display.', linkTerms: true }));
        }
        var explorePageSize = 40;
        var rdfsNamespace = 'http://www.w3.org/2000/01/rdf-schema#';
        var exploreGroups = [
            { key: 'superClasses', title: 'Super Classes', predicate: rdfsNamespace + 'subClassOf', field: 'object' },
            { key: 'subClasses', title: 'Sub Classes', predicate: rdfsNamespace + 'subClassOf', field: 'subject' },
            { key: 'properties', title: 'Properties', predicate: rdfsNamespace + 'domain', field: 'subject' },
            { key: 'superProperties', title: 'Super Properties', predicate: rdfsNamespace + 'subPropertyOf', field: 'object' },
            { key: 'subProperties', title: 'Sub Properties', predicate: rdfsNamespace + 'subPropertyOf', field: 'subject' },
            { key: 'domain', title: 'Domain', predicate: rdfsNamespace + 'domain', field: 'object' },
            { key: 'range', title: 'Range', predicate: rdfsNamespace + 'range', field: 'object' }
        ];
        function exploreGroupStates(model) {
            var target = model;
            if (!target.exploreGroupStates) {
                target.exploreGroupStates = {};
                exploreGroups.forEach(function (group) {
                    target.exploreGroupStates[group.key] = { cursor: null, history: [], start: 0 };
                });
            }
            return target.exploreGroupStates;
        }
        function exploreAccumulator(model) {
            var states = exploreGroupStates(model);
            var groups = {};
            exploreGroups.forEach(function (group) {
                groups[group.key] = {
                    count: 0,
                    eligible: 0,
                    candidates: [],
                    cursor: states[group.key].cursor
                };
            });
            return {
                groups: groups,
                labelCount: 0,
                label: null,
                commentCount: 0,
                comment: null,
                subclassCount: 0,
                domainCount: 0,
                subPropertyCount: 0,
                rangeCount: 0
            };
        }
        function candidateBefore(left, right) {
            return left.key < right.key || (left.key === right.key && left.rowIndex < right.rowIndex);
        }
        function addExploreCandidate(group, value, rowIndex, pageSize) {
            var key = text(value);
            group.count++;
            var cursor = group.cursor;
            if (cursor && key < cursor.key) {
                return;
            }
            if (cursor && key === cursor.key) {
                group.cursorValuesSeen = (group.cursorValuesSeen || 0) + 1;
                if (group.cursorValuesSeen <= cursor.consumed) {
                    return;
                }
            }
            group.eligible++;
            var candidate = { key: key, rowIndex: rowIndex, value: value };
            var index = 0;
            while (index < group.candidates.length && candidateBefore(group.candidates[index], candidate)) {
                index++;
            }
            group.candidates.splice(index, 0, candidate);
            if (group.candidates.length > pageSize) {
                group.candidates.pop();
            }
        }
        function consumeExploreRows(model, rows, start, accumulator) {
            var subjectIndex = model.vars.indexOf('subject');
            var predicateIndex = model.vars.indexOf('predicate');
            var objectIndex = model.vars.indexOf('object');
            rows.forEach(function (row, offset) {
                var subject = subjectIndex >= 0 ? row[subjectIndex] : null;
                var predicate = predicateIndex >= 0 ? text(row[predicateIndex]) : '';
                var object = objectIndex >= 0 ? row[objectIndex] : null;
                var rowIndex = start + offset;
                if (predicate === rdfsNamespace + 'label') {
                    accumulator.labelCount++;
                    accumulator.label = object;
                }
                else if (predicate === rdfsNamespace + 'comment') {
                    accumulator.commentCount++;
                    accumulator.comment = object;
                }
                else if (predicate === rdfsNamespace + 'subClassOf') {
                    accumulator.subclassCount++;
                }
                else if (predicate === rdfsNamespace + 'domain') {
                    accumulator.domainCount++;
                }
                else if (predicate === rdfsNamespace + 'subPropertyOf') {
                    accumulator.subPropertyCount++;
                }
                else if (predicate === rdfsNamespace + 'range') {
                    accumulator.rangeCount++;
                }
                exploreGroups.forEach(function (definition) {
                    if (predicate === definition.predicate) {
                        var value = definition.field === 'subject' ? subject : object;
                        if (value !== null && typeof value !== 'undefined') {
                            addExploreCandidate(accumulator.groups[definition.key], value, rowIndex, explorePageSize);
                        }
                    }
                });
            });
        }
        function finishExploreSummary(model, accumulator) {
            var states = exploreGroupStates(model);
            var groups = {};
            exploreGroups.forEach(function (definition) {
                var state = states[definition.key];
                var collected = accumulator.groups[definition.key];
                var candidates = collected.candidates;
                var last = candidates.length ? candidates[candidates.length - 1] : null;
                var nextCursor = null;
                if (last) {
                    var sameKeyInPage = candidates.filter(function (candidate) { return candidate.key === last.key; }).length;
                    nextCursor = {
                        key: last.key,
                        consumed: state.cursor && state.cursor.key === last.key
                            ? state.cursor.consumed + sameKeyInPage : sameKeyInPage
                    };
                }
                groups[definition.key] = {
                    count: collected.count,
                    start: state.start,
                    items: candidates.map(function (candidate) { return candidate.value; }),
                    hasPrevious: state.history.length > 0,
                    hasNext: collected.eligible > candidates.length,
                    nextCursor: nextCursor
                };
            });
            return {
                label: accumulator.labelCount === 1 ? accumulator.label : null,
                comment: accumulator.commentCount === 1 ? accumulator.comment : null,
                subclassCount: accumulator.subclassCount,
                domainCount: accumulator.domainCount,
                subPropertyCount: accumulator.subPropertyCount,
                rangeCount: accumulator.rangeCount,
                groups: groups
            };
        }
        function summarizeVisibleExploreRows(model) {
            var accumulator = exploreAccumulator(model);
            consumeExploreRows(model, model.rows || [], rowStart(model), accumulator);
            return finishExploreSummary(model, accumulator);
        }
        function prepareExploreSummary(model) {
            if (model.viewId !== 'explore' || !model.rowStore) {
                return Promise.resolve();
            }
            var accumulator = exploreAccumulator(model);
            var chunkSize = 256;
            var scan = function (start) {
                if (start >= model.rowCount) {
                    return Promise.resolve();
                }
                var count = Math.min(chunkSize, model.rowCount - start);
                return model.rowStore.read(start, count).then(function (rows) {
                    consumeExploreRows(model, rows, start, accumulator);
                    return rows.length ? scan(start + rows.length) : Promise.resolve();
                });
            };
            return scan(0).then(function () { return finishExploreSummary(model, accumulator); });
        }
        function exploreGroupList(runtime, model, context, definition, page) {
            var h = runtime.html;
            if (!page || !page.count) {
                return '';
            }
            var regions = context.rowRegions;
            if (regions) {
                if (!regions.groups[definition.key]) {
                    var node_1 = regions.document.createElement('section');
                    node_1.setAttribute('data-workbench-explore-group', definition.key);
                    regions.groups[definition.key] = {
                        node: node_1,
                        render: function () {
                            var summary = model.exploreSummary || summarizeVisibleExploreRows(model);
                            runtime.render(exploreGroupContent(runtime, model, context, definition, summary.groups[definition.key]), node_1);
                        }
                    };
                }
                return regions.groups[definition.key].node;
            }
            return h(__makeTemplateObject(["<section data-workbench-explore-group=", ">\n                ", "\n            </section>"], ["<section data-workbench-explore-group=", ">\n                ", "\n            </section>"]), definition.key, exploreGroupContent(runtime, model, context, definition, page));
        }
        function exploreGroupContent(runtime, model, context, definition, page) {
            var h = runtime.html;
            return h(__makeTemplateObject(["<h3>", "</h3>\n                <ul>", "</ul>\n                ", ""], ["<h3>", "</h3>\n                <ul>", "</ul>\n                ", ""]), definition.title, page.items.map(function (value) { return h(__makeTemplateObject(["<li>", "</li>"], ["<li>", "</li>"]), renderTerm(runtime, value, context, true)); }), page.count > explorePageSize ? h(__makeTemplateObject(["<div class=\"workbench-form-actions workbench-window-controls\"\n                    role=\"group\" aria-label=", ">\n                    <span role=\"status\">Showing ", "\u2013", " of ", "</span>\n                    <button type=\"button\" data-workbench-explore-group=", "\n                        data-workbench-explore-action=\"previous\" ?disabled=", ">Previous</button>\n                    <button type=\"button\" data-workbench-explore-group=", "\n                        data-workbench-explore-action=\"next\" ?disabled=", ">Next</button>\n                </div>"], ["<div class=\"workbench-form-actions workbench-window-controls\"\n                    role=\"group\" aria-label=", ">\n                    <span role=\"status\">Showing ", "\u2013", " of ", "</span>\n                    <button type=\"button\" data-workbench-explore-group=", "\n                        data-workbench-explore-action=\"previous\" ?disabled=", ">Previous</button>\n                    <button type=\"button\" data-workbench-explore-group=", "\n                        data-workbench-explore-action=\"next\" ?disabled=", ">Next</button>\n                </div>"]), definition.title + ' pages', page.start + 1, page.start + page.items.length, page.count, definition.key, !page.hasPrevious, definition.key, !page.hasNext) : '');
        }
        function explorePage(runtime, model, context) {
            var h = runtime.html;
            var resource = text(pageValue(model, 'resource'));
            var total = rowCount(model);
            var summary = model.exploreSummary || summarizeVisibleExploreRows(model);
            var info = workbenchData(context);
            var defaults = info.defaults || info;
            var resultLimit = text(pageValue(model, 'default-limit') || defaults['default-limit']) || '100';
            var resultLimited = !!resource && resultLimit !== '0' && total === Number(resultLimit);
            var groupPages = summary.groups || {};
            var classGroups = [exploreGroups[0], exploreGroups[1]]
                .map(function (definition) { return exploreGroupList(runtime, model, context, definition, groupPages[definition.key]); });
            var propertyGroups = [];
            if (summary.domainCount) {
                propertyGroups.push(exploreGroupList(runtime, model, context, exploreGroups[2], groupPages.properties));
                if (summary.subPropertyCount) {
                    propertyGroups.push(exploreGroupList(runtime, model, context, exploreGroups[3], groupPages.superProperties));
                    propertyGroups.push(exploreGroupList(runtime, model, context, exploreGroups[4], groupPages.subProperties));
                }
                propertyGroups.push(exploreGroupList(runtime, model, context, exploreGroups[5], groupPages.domain));
            }
            if (summary.rangeCount) {
                propertyGroups.push(exploreGroupList(runtime, model, context, exploreGroups[6], groupPages.range));
            }
            var groupedResults = summary.subclassCount || summary.domainCount || summary.rangeCount
                ? h(__makeTemplateObject(["<div class=\"workbench-explore-groups\">\n                    ", "\n                    ", "\n                </div>"], ["<div class=\"workbench-explore-groups\">\n                    ", "\n                    ", "\n                </div>"]), classGroups.some(function (group) { return !!group; }) ? h(__makeTemplateObject(["<div>", "</div>"], ["<div>", "</div>"]), classGroups) : '', propertyGroups.some(function (group) { return !!group; }) ? h(__makeTemplateObject(["<div>", "</div>"], ["<div>", "</div>"]), propertyGroups) : '') : '';
            return h(__makeTemplateObject(["", "\n                ", "\n                ", "", "\n                <p id=\"explore-resource-summary\" class=\"workbench-page-meta\" ?hidden=", ">\n                    <span id=\"explore-resource-value\">", "</span><span id=\"explore-result-count\">", "</span>\n                </p>\n                <form id=\"explore-form\" class=\"workbench-island\" action=\"explore\">\n                    <input id=\"workbench-total-result-count\" type=\"hidden\"\n                        value=", " />\n                    <div id=\"explore-controls\"><div id=\"explore-resource-field\" class=\"workbench-field\">\n                        <label for=\"resource\">Resource</label><input id=\"resource\" name=\"resource\" size=\"48\" type=\"text\" value=", " />\n                    </div>\n                    ", "\n                    </div>\n                </form>\n                <section id=\"explore-results\" class=\"workbench-island workbench-responsive-records\">\n                    ", "\n                    <div id=\"explore-pagination\" class=\"workbench-form-actions\" ?hidden=", ">\n                        <button id=\"previousX\" type=\"button\" value=", "\n                            @click=", ">Previous ", "</button>\n                        <button id=\"nextX\" type=\"button\" value=", "\n                            @click=", ">Next ", "</button>\n                    </div>\n                </section>"], ["", "\n                ", "\n                ", "", "\n                <p id=\"explore-resource-summary\" class=\"workbench-page-meta\" ?hidden=", ">\n                    <span id=\"explore-resource-value\">", "</span><span id=\"explore-result-count\">", "</span>\n                </p>\n                <form id=\"explore-form\" class=\"workbench-island\" action=\"explore\">\n                    <input id=\"workbench-total-result-count\" type=\"hidden\"\n                        value=", " />\n                    <div id=\"explore-controls\"><div id=\"explore-resource-field\" class=\"workbench-field\">\n                        <label for=\"resource\">Resource</label><input id=\"resource\" name=\"resource\" size=\"48\" type=\"text\" value=", " />\n                    </div>\n                    ", "\n                    </div>\n                </form>\n                <section id=\"explore-results\" class=\"workbench-island workbench-responsive-records\">\n                    ", "\n                    <div id=\"explore-pagination\" class=\"workbench-form-actions\" ?hidden=", ">\n                        <button id=\"previousX\" type=\"button\" value=", "\n                            @click=", ">Previous ", "</button>\n                        <button id=\"nextX\" type=\"button\" value=", "\n                            @click=", ">Next ", "</button>\n                    </div>\n                </section>"]), resultLimited ? h(__makeTemplateObject(["<p id=\"result-limited\">The results shown maybe truncated.</p>"], ["<p id=\"result-limited\">The results shown maybe truncated.</p>"])) : '', pageValue(model, 'error-message') ? h(__makeTemplateObject(["<p class=\"error\" role=\"alert\">", "</p>"], ["<p class=\"error\" role=\"alert\">", "</p>"]), text(pageValue(model, 'error-message'))) : '', summary.label ? h(__makeTemplateObject(["<h2>", "</h2>"], ["<h2>", "</h2>"]), text(summary.label)) : '', summary.comment ? h(__makeTemplateObject(["<p>", "</p>"], ["<p>", "</p>"]), text(summary.comment)) : '', !resource, resource, total, text(pageValue(model, 'total-result-count')), resource, workbench.detailDisclosure.render(h, {
                id: 'explore-result-options', toggleId: 'explore-result-options-toggle',
                panelId: 'explore-result-options-panel', label: 'Result options',
                ownerClass: 'workbench-options workbench-form-subgroup'
            }, h(__makeTemplateObject(["<div class=\"workbench-field workbench-disclosure__field\"><label for=\"limit_explore\">Result limit</label>\n                            ", "\n                        </div><label class=\"workbench-check\" for=\"explore-show-datatypes\">\n                            <input id=\"explore-show-datatypes\" type=\"checkbox\" name=\"show-datatypes\" value=\"show-dataypes\" checked />\n                            <span>Show datatypes</span></label>"], ["<div class=\"workbench-field workbench-disclosure__field\"><label for=\"limit_explore\">Result limit</label>\n                            ", "\n                        </div><label class=\"workbench-check\" for=\"explore-show-datatypes\">\n                            <input id=\"explore-show-datatypes\" type=\"checkbox\" name=\"show-datatypes\" value=\"show-dataypes\" checked />\n                            <span>Show datatypes</span></label>"]), limitSelect(runtime, 'limit_explore', context, text(pageValue(model, 'default-limit')) || '100'))), total ? h(__makeTemplateObject(["", "", ""], ["", "", ""]), groupedResults, table(runtime, model, context, { linkTerms: true })) : h(__makeTemplateObject(["<p class=\"workbench-empty\" role=\"status\">No results to display.</p>"], ["<p class=\"workbench-empty\" role=\"status\">No results to display.</p>"])), total === 0, 'Previous ' + total, function () { return invoke('workbench.paging.previousOffset', 'explore'); }, total, 'Next ' + total, function () { return invoke('workbench.paging.nextOffset', 'explore'); }, total);
        }
        function limitSelect(runtime, id, _context, selected) {
            var h = runtime.html;
            var options = ['0', '10', '50', '100', '200'];
            if (selected && options.indexOf(selected) < 0) {
                options.push(selected);
            }
            return h(__makeTemplateObject(["<select id=", " name=", ">", "</select>"], ["<select id=", " name=", ">", "</select>"]), id, id, options.map(function (option) { return h(__makeTemplateObject(["<option value=", " ?selected=", ">\n                ", "</option>"], ["<option value=", " ?selected=", ">\n                ", "</option>"]), option, option === selected, option === '0' ? 'All' : option); }));
        }
        function invoke(path) {
            var arguments_ = [];
            for (var _i = 1; _i < arguments.length; _i++) {
                arguments_[_i - 1] = arguments[_i];
            }
            var pieces = path.split('.');
            var target = typeof window !== 'undefined' ? window : null;
            for (var i = 0; target && i < pieces.length - 1; i++) {
                target = target[pieces[i]];
            }
            if (target && typeof target[pieces[pieces.length - 1]] === 'function') {
                target[pieces[pieces.length - 1]].apply(target, arguments_);
            }
        }
        function savedQueriesPage(runtime, model, context) {
            var h = runtime.html;
            if (!rowCount(model)) {
                var queryAvailable = menuEntries(context).some(function (group) { return (group.items || []).some(function (item) {
                    return text(item.id || item['menu-item-id']) === 'query' && !isDisabled(item, workbenchData(context));
                }); });
                return h(__makeTemplateObject(["<div id=\"saved-queries\" class=\"workbench-page-layout\">\n                    <div class=\"saved-queries-empty\" role=\"status\"><p>No saved queries yet.</p>\n                        ", "</div>\n                </div>"], ["<div id=\"saved-queries\" class=\"workbench-page-layout\">\n                    <div class=\"saved-queries-empty\" role=\"status\"><p>No saved queries yet.</p>\n                        ", "</div>\n                </div>"]), queryAvailable ? h(__makeTemplateObject(["<a href=", ">Open Query</a>"], ["<a href=", ">Open Query</a>"]), urlFor(context, 'query')) : '');
            }
            var regions = context.rowRegions;
            if (regions) {
                if (!regions.savedList) {
                    regions.savedList = regions.document.createElement('div');
                    regions.savedList.id = 'saved-queries';
                    regions.savedList.className = 'workbench-page-layout';
                    regions.savedList.setAttribute('data-workbench-row-list', 'true');
                }
                regions.renderSavedRows = function () {
                    var cards = {};
                    var rows = records(model).map(function (row, relativeIndex) {
                        var index = rowStart(model) + relativeIndex;
                        var urn = text(row.query);
                        var key = index + ':' + urn;
                        var card = regions.savedCards[key];
                        if (!card) {
                            card = regions.document.createElement('article');
                            card.id = urn + '-div';
                            card.className = 'saved-query-row workbench-island';
                            card.setAttribute('data-workbench-row-index', String(index));
                            runtime.render(savedQueryContent(runtime, row, index), card);
                        }
                        cards[key] = card;
                        return card;
                    });
                    runtime.render(savedQueryListContent(runtime, model, rows), regions.savedList);
                    regions.savedCards = cards;
                };
                return regions.savedList;
            }
            return h(__makeTemplateObject(["<div id=\"saved-queries\" class=\"workbench-page-layout\" data-workbench-row-list=\"true\">\n                ", "\n            </div>"], ["<div id=\"saved-queries\" class=\"workbench-page-layout\" data-workbench-row-list=\"true\">\n                ", "\n            </div>"]), savedQueryListContent(runtime, model, records(model).map(function (row, relativeIndex) {
                var index = rowStart(model) + relativeIndex;
                var urn = text(row.query);
                return h(__makeTemplateObject(["<article id=", " class=\"saved-query-row workbench-island\"\n                            data-workbench-row-index=", ">\n                        ", "\n                    </article>"], ["<article id=", " class=\"saved-query-row workbench-island\"\n                            data-workbench-row-index=", ">\n                        ", "\n                    </article>"]), urn + '-div', index, savedQueryContent(runtime, row, index));
            })));
        }
        function savedQueryListContent(runtime, model, rows) {
            var h = runtime.html;
            return h(__makeTemplateObject(["<div class=\"workbench-virtual-spacer\" style=", " aria-hidden=\"true\"></div>\n                ", "\n                <div class=\"workbench-virtual-spacer\" style=", " aria-hidden=\"true\"></div>"], ["<div class=\"workbench-virtual-spacer\" style=", " aria-hidden=\"true\"></div>\n                ", "\n                <div class=\"workbench-virtual-spacer\" style=", " aria-hidden=\"true\"></div>"]), model.rowTopSpacer
                ? 'height:' + model.rowTopSpacer + 'px' : 'display:none', rows, model.rowBottomSpacer
                ? 'height:' + model.rowBottomSpacer + 'px' : 'display:none');
        }
        function savedQueryContent(runtime, row, index) {
            var h = runtime.html;
            var urn = text(row.query);
            var queryName = text(row.queryName);
            var owner = text(row.user);
            var query = text(row.queryText || row.query);
            var queryTimeout = text(row.queryTimeout).trim() || '0';
            var formId = 'saved-query-exec-' + index;
            return h(__makeTemplateObject(["\n                        <div class=\"saved-query-row__heading\"><h2>", "</h2><span>", "</span></div>\n                        <div class=\"saved-query-actions\">\n                            <form method=\"post\" action=\"query\" id=", "\n                                data-workbench-query-execution=\"true\"\n                                data-workbench-results-target=", ">\n                                <input type=\"hidden\" name=\"action\" value=\"exec\" />\n                                <input type=\"hidden\" name=\"queryLn\" value=", " />\n                                <input type=\"hidden\" name=\"query\" value=", " />\n                                <input type=\"hidden\" name=\"ref\" value=\"id\" />\n                                <input type=\"hidden\" name=\"owner\" value=", " />\n                                <input type=\"hidden\" name=\"infer\" value=", " />\n                                <input type=\"hidden\" name=\"query-timeout\" value=", " />\n                                <span class=\"workbench-action workbench-action--primary\"><label class=\"workbench-action-hit-area\">\n                                    ", "<span class=\"workbench-action-label\"><input type=\"submit\" value=\"Execute\" /></span>\n                                </label></span>\n                            </form>\n                            <button type=\"button\" class=\"saved-query-toggle\" id=", " data-query-urn=", "\n                                value=\"Show\">Show</button>\n                            <form method=\"post\" action=\"query\"><input type=\"hidden\" name=\"action\" value=\"edit\" />\n                                <input type=\"hidden\" name=\"queryLn\" value=", " /><input type=\"hidden\" name=\"query\" value=", " />\n                                <input type=\"hidden\" name=\"ref\" value=\"id\" /><input type=\"hidden\" name=\"owner\" value=", " />\n                                <input type=\"hidden\" name=\"infer\" value=", " />\n                                <input type=\"hidden\" name=\"query-timeout\" value=", " /><button type=\"submit\">Edit</button>\n                            </form>\n                            <form method=\"post\" id=", " action=", ">\n                                <button type=\"button\" class=\"saved-query-delete\" data-query-owner=", " data-query-name=", "\n                                    data-query-urn=", ">Delete\u2026</button>\n                            </form>\n                        </div>\n                        <div id=", " class=\"query-results\" aria-live=\"polite\"></div>\n                        <table class=\"data\" id=", " style=\"display: none\"><tbody><tr>\n                            <th>Query Language</th><td>", "</td><th>Include Inferred Statements</th><td>", "</td>\n                            <th>Shared</th><td>", "</td>\n                        </tr></tbody></table>\n                        <textarea id=", " style=\"display: none\">", "</textarea>\n                    "], ["\n                        <div class=\"saved-query-row__heading\"><h2>", "</h2><span>", "</span></div>\n                        <div class=\"saved-query-actions\">\n                            <form method=\"post\" action=\"query\" id=", "\n                                data-workbench-query-execution=\"true\"\n                                data-workbench-results-target=", ">\n                                <input type=\"hidden\" name=\"action\" value=\"exec\" />\n                                <input type=\"hidden\" name=\"queryLn\" value=", " />\n                                <input type=\"hidden\" name=\"query\" value=", " />\n                                <input type=\"hidden\" name=\"ref\" value=\"id\" />\n                                <input type=\"hidden\" name=\"owner\" value=", " />\n                                <input type=\"hidden\" name=\"infer\" value=", " />\n                                <input type=\"hidden\" name=\"query-timeout\" value=", " />\n                                <span class=\"workbench-action workbench-action--primary\"><label class=\"workbench-action-hit-area\">\n                                    ", "<span class=\"workbench-action-label\"><input type=\"submit\" value=\"Execute\" /></span>\n                                </label></span>\n                            </form>\n                            <button type=\"button\" class=\"saved-query-toggle\" id=", " data-query-urn=", "\n                                value=\"Show\">Show</button>\n                            <form method=\"post\" action=\"query\"><input type=\"hidden\" name=\"action\" value=\"edit\" />\n                                <input type=\"hidden\" name=\"queryLn\" value=", " /><input type=\"hidden\" name=\"query\" value=", " />\n                                <input type=\"hidden\" name=\"ref\" value=\"id\" /><input type=\"hidden\" name=\"owner\" value=", " />\n                                <input type=\"hidden\" name=\"infer\" value=", " />\n                                <input type=\"hidden\" name=\"query-timeout\" value=", " /><button type=\"submit\">Edit</button>\n                            </form>\n                            <form method=\"post\" id=", " action=", ">\n                                <button type=\"button\" class=\"saved-query-delete\" data-query-owner=", " data-query-name=", "\n                                    data-query-urn=", ">Delete\u2026</button>\n                            </form>\n                        </div>\n                        <div id=", " class=\"query-results\" aria-live=\"polite\"></div>\n                        <table class=\"data\" id=", " style=\"display: none\"><tbody><tr>\n                            <th>Query Language</th><td>", "</td><th>Include Inferred Statements</th><td>", "</td>\n                            <th>Shared</th><td>", "</td>\n                        </tr></tbody></table>\n                        <textarea id=", " style=\"display: none\">", "</textarea>\n                    "]), queryName, owner, formId, 'saved-query-results-' + index, text(row.queryLn), queryName, owner, text(row.infer), queryTimeout, icon(runtime, 'execute'), urn + '-toggle', urn, text(row.queryLn), queryName, owner, text(row.infer), queryTimeout, urn, 'saved-queries?delete=' + encodeURIComponent(urn), owner, queryName, urn, 'saved-query-results-' + index, urn + '-metadata', text(row.queryLn), text(row.infer), text(row.shared), urn + '-text', query);
        }
        function exportPage(runtime, model, context) {
            var h = runtime.html;
            var info = workbenchData(context);
            var defaults = info.defaults || info;
            var graphFormats = formatOptions(info.graphDownloadFormats || info['graph-download-format']);
            var locationObject = typeof window !== 'undefined' ? window.location : null;
            var urlFormat = locationObject && typeof locationObject.search === 'string'
                ? new URLSearchParams(locationObject.search).get('Accept') : '';
            var explicitFormat = text(urlFormat || meta(model, 'Accept') || pageValue(model, 'Accept'));
            var cookieFormat = currentCookieValue('Accept');
            var configuredFormat = text(meta(model, 'default-export-format') || defaults['default-export-format']);
            var genericFormat = text(meta(model, 'default-Accept') || defaults['default-Accept']);
            var available = function (candidate) { return graphFormats.some(function (format) { return format.value === candidate; }); };
            var defaultFormat = [explicitFormat, cookieFormat, configuredFormat, genericFormat]
                .filter(function (candidate) { return candidate && available(candidate); })[0]
                || (graphFormats.length ? graphFormats[0].value : 'application/n-quads');
            var timeout = text(meta(model, 'export-timeout')) || '43200';
            var requested = text(meta(model, 'statement-preview-requested')) === 'true';
            var previewLimit = text(meta(model, 'statement-preview-limit')) || '100';
            return h(__makeTemplateObject(["<form id=\"export-form\" class=\"workbench-island\" action=\"export\">\n                <div class=\"workbench-form-grid\">\n                    <div class=\"workbench-field\"><label for=\"Accept\">Download format</label>\n                        <select id=\"Accept\" name=\"Accept\">", "</select>\n                    </div>\n                    <div class=\"workbench-field\"><label for=\"compression\">Compression</label><select id=\"compression\" name=\"compression\">\n                        <option value=\"none\">None</option><option value=\"gzip\" selected>Gzip</option><option value=\"zip\">Zip</option>\n                    </select></div>\n                    <div class=\"workbench-field\"><label for=\"timeout\">Export timeout (seconds)</label>\n                        <input id=\"timeout\" name=\"timeout\" type=\"number\" min=\"0\" step=\"1\" required value=", " />\n                        <span class=\"hint\">Maximum time allowed for the export operation. Use 0 for no timeout.</span>\n                    </div>\n                </div>\n                ", "\n                <div class=\"workbench-form-actions\"><span class=\"workbench-action workbench-action--primary\">\n                    <span class=\"workbench-action-hit-area\"><span class=\"workbench-action-label\">\n                        <button type=\"submit\" name=\"action\" value=\"download\" aria-label=\"Download\">\n                            ", "<span>Download</span>\n                        </button>\n                    </span></span>\n                </span></div>\n            </form>\n            <section id=\"export-results\" class=\"workbench-island workbench-responsive-records\">\n                <p class=\"workbench-page-meta\">Statement preview</p>\n                <div class=\"workbench-form-actions\"><button type=\"submit\" form=\"export-form\" name=\"action\" value=\"preview\">Retrieve statements</button></div>\n                ", "\n            </section>"], ["<form id=\"export-form\" class=\"workbench-island\" action=\"export\">\n                <div class=\"workbench-form-grid\">\n                    <div class=\"workbench-field\"><label for=\"Accept\">Download format</label>\n                        <select id=\"Accept\" name=\"Accept\">", "</select>\n                    </div>\n                    <div class=\"workbench-field\"><label for=\"compression\">Compression</label><select id=\"compression\" name=\"compression\">\n                        <option value=\"none\">None</option><option value=\"gzip\" selected>Gzip</option><option value=\"zip\">Zip</option>\n                    </select></div>\n                    <div class=\"workbench-field\"><label for=\"timeout\">Export timeout (seconds)</label>\n                        <input id=\"timeout\" name=\"timeout\" type=\"number\" min=\"0\" step=\"1\" required value=", " />\n                        <span class=\"hint\">Maximum time allowed for the export operation. Use 0 for no timeout.</span>\n                    </div>\n                </div>\n                ", "\n                <div class=\"workbench-form-actions\"><span class=\"workbench-action workbench-action--primary\">\n                    <span class=\"workbench-action-hit-area\"><span class=\"workbench-action-label\">\n                        <button type=\"submit\" name=\"action\" value=\"download\" aria-label=\"Download\">\n                            ", "<span>Download</span>\n                        </button>\n                    </span></span>\n                </span></div>\n            </form>\n            <section id=\"export-results\" class=\"workbench-island workbench-responsive-records\">\n                <p class=\"workbench-page-meta\">Statement preview</p>\n                <div class=\"workbench-form-actions\"><button type=\"submit\" form=\"export-form\" name=\"action\" value=\"preview\">Retrieve statements</button></div>\n                ", "\n            </section>"]), graphFormats.map(function (format) { return h(__makeTemplateObject(["<option value=", " ?selected=", ">", "</option>"], ["<option value=", " ?selected=", ">", "</option>"]), format.value, format.value === defaultFormat, format.label); }), timeout, workbench.detailDisclosure.render(h, {
                id: 'export-result-options', toggleId: 'export-result-options-toggle',
                panelId: 'export-result-options-panel', label: 'Result options',
                ownerClass: 'workbench-options workbench-form-subgroup'
            }, h(__makeTemplateObject(["<div class=\"workbench-field workbench-disclosure__field\"><label for=\"limit_export\">Preview limit</label>\n                        ", "\n                        <span id=\"result-limited\">", "</span>\n                        <span class=\"hint\">Limit only applies to the preview, not to downloads.</span>\n                    </div>"], ["<div class=\"workbench-field workbench-disclosure__field\"><label for=\"limit_export\">Preview limit</label>\n                        ", "\n                        <span id=\"result-limited\">", "</span>\n                        <span class=\"hint\">Limit only applies to the preview, not to downloads.</span>\n                    </div>"]), limitSelect(runtime, 'limit_export', context, previewLimit), requested && previewLimit !== '0' && rowCount(model) >= Number(previewLimit)
                ? 'The preview is limited to the selected number of statements.' : '')), icon(runtime, 'download'), rowCount(model) ? table(runtime, model, context, { linkTerms: true })
                : h(__makeTemplateObject(["<p class=\"workbench-empty\" role=\"status\">", "</p>"], ["<p class=\"workbench-empty\" role=\"status\">", "</p>"]), requested ? 'No results to display.' : 'Choose Retrieve statements to preview repository data.'));
        }
        function formatOptions(values) {
            if (!Array.isArray(values)) {
                return [];
            }
            return values.map(function (entry) {
                if (entry && typeof entry === 'object') {
                    return { value: text(entry.value || entry.mimeType || entry.name), label: text(entry.label || entry.name || entry.value) };
                }
                var value = text(entry);
                var separator = value.indexOf(' ');
                return separator < 0 ? { value: value, label: value }
                    : { value: value.substring(0, separator), label: value.substring(separator + 1) };
            });
        }
        function currentCookieValue(name) {
            var namespace = workbench;
            if (namespace && typeof namespace.getCookie === 'function') {
                return text(namespace.getCookie(name));
            }
            if (typeof document === 'undefined' || typeof document.cookie !== 'string') {
                return '';
            }
            var cookie;
            var cookies = document.cookie.split(';');
            for (var index = 0; index < cookies.length; index++) {
                var candidate = cookies[index].trim();
                if (candidate.substring(0, candidate.indexOf('=')) === name) {
                    cookie = candidate;
                    break;
                }
            }
            if (!cookie) {
                return '';
            }
            try {
                return decodeURIComponent(cookie.substring(cookie.indexOf('=') + 1).replace(/\+/g, '%20'));
            }
            catch (error) {
                return cookie.substring(cookie.indexOf('=') + 1);
            }
        }
        function addPage(runtime, model, context) {
            var h = runtime.html;
            var rows = records(model);
            var info = workbenchData(context);
            var defaults = info.defaults || info;
            var formats = formatOptions(info.uploadFormats || info['upload-format']);
            var isolationOptions = rows.filter(function (row) { return field(row, 'isolation-level-option'); });
            var selectedIsolation = text(pageValue(model, 'transaction-setting__org.eclipse.rdf4j.common.transaction.IsolationLevel'));
            var error = text(pageValue(model, 'error-message'));
            return h(__makeTemplateObject(["", "\n                ", "\n                <form method=\"post\" action=\"add\" enctype=\"multipart/form-data\">\n                    <fieldset id=\"add-source-tabs\" class=\"workbench-source-tabs\"><legend>Source</legend>\n                        ", "\n                    </fieldset>\n                    <div class=\"workbench-form-grid add-source-fields\">\n                        <div id=\"add-source-file-panel\" class=\"workbench-field add-source-panel\" data-source=\"file\">\n                            <label for=\"file\">RDF file</label><input type=\"file\" id=\"file\" name=\"content\"\n                                @change=", " />\n                            <div class=\"hint\">Select an RDF document from your computer.</div>\n                        </div>\n                        <div id=\"add-source-url-panel\" class=\"workbench-field add-source-panel\" data-source=\"url\" hidden>\n                            <label for=\"url\">RDF URL</label><input id=\"url\" name=\"url\" type=\"text\" size=\"48\" disabled\n                                @change=", " />\n                        </div>\n                        <div id=\"add-source-text-panel\" class=\"workbench-field add-source-panel\" data-source=\"text\" hidden>\n                            <label for=\"text\">RDF text</label><textarea id=\"text\" name=\"content\" rows=\"6\" cols=\"70\" disabled></textarea>\n                        </div>\n                        <div class=\"workbench-field add-source-format\"><label for=\"Content-Type\">Data format</label>\n                            <div class=\"workbench-select-control\"><select id=\"Content-Type\" name=\"Content-Type\">\n                                <option id=\"autodetect\" value=\"autodetect\" selected>(autodetect)</option>\n                                ", "\n                            </select>", "</div>\n                        </div>\n                    </div>\n                    ", "\n                    <div id=\"add-upload-actions\" class=\"workbench-form-actions\"><span class=\"workbench-action workbench-action--primary\">\n                        <label class=\"workbench-action-hit-area\">", "<span class=\"workbench-action-label\"><input type=\"submit\" value=\"Upload\" /></span></label>\n                    </span></div>\n                </form>"], ["", "\n                ", "\n                <form method=\"post\" action=\"add\" enctype=\"multipart/form-data\">\n                    <fieldset id=\"add-source-tabs\" class=\"workbench-source-tabs\"><legend>Source</legend>\n                        ", "\n                    </fieldset>\n                    <div class=\"workbench-form-grid add-source-fields\">\n                        <div id=\"add-source-file-panel\" class=\"workbench-field add-source-panel\" data-source=\"file\">\n                            <label for=\"file\">RDF file</label><input type=\"file\" id=\"file\" name=\"content\"\n                                @change=", " />\n                            <div class=\"hint\">Select an RDF document from your computer.</div>\n                        </div>\n                        <div id=\"add-source-url-panel\" class=\"workbench-field add-source-panel\" data-source=\"url\" hidden>\n                            <label for=\"url\">RDF URL</label><input id=\"url\" name=\"url\" type=\"text\" size=\"48\" disabled\n                                @change=", " />\n                        </div>\n                        <div id=\"add-source-text-panel\" class=\"workbench-field add-source-panel\" data-source=\"text\" hidden>\n                            <label for=\"text\">RDF text</label><textarea id=\"text\" name=\"content\" rows=\"6\" cols=\"70\" disabled></textarea>\n                        </div>\n                        <div class=\"workbench-field add-source-format\"><label for=\"Content-Type\">Data format</label>\n                            <div class=\"workbench-select-control\"><select id=\"Content-Type\" name=\"Content-Type\">\n                                <option id=\"autodetect\" value=\"autodetect\" selected>(autodetect)</option>\n                                ", "\n                            </select>", "</div>\n                        </div>\n                    </div>\n                    ", "\n                    <div id=\"add-upload-actions\" class=\"workbench-form-actions\"><span class=\"workbench-action workbench-action--primary\">\n                        <label class=\"workbench-action-hit-area\">", "<span class=\"workbench-action-label\"><input type=\"submit\" value=\"Upload\" /></span></label>\n                    </span></div>\n                </form>"]), error ? h(__makeTemplateObject(["<p class=\"error\" role=\"alert\">", "</p>"], ["<p class=\"error\" role=\"alert\">", "</p>"]), error) : '', context.repositoryId === 'SYSTEM' ? h(__makeTemplateObject(["<p class=\"WARN\">The SYSTEM repository is intended for system use.</p>"], ["<p class=\"WARN\">The SYSTEM repository is intended for system use.</p>"])) : '', [['file', 'File'], ['url', 'URL'], ['text', 'Text']].map(function (entry) { return h(__makeTemplateObject(["<label for=", ">\n                            ", "\n                            <input type=\"radio\" id=", " name=\"source\"\n                                value=", "\n                                ?checked=", "\n                                @change=", " />\n                            <span>", "</span>\n                        </label>"], ["<label for=", ">\n                            ", "\n                            <input type=\"radio\" id=", " name=\"source\"\n                                value=", "\n                                ?checked=", "\n                                @change=", " />\n                            <span>", "</span>\n                        </label>"]), 'source-' + entry[0], icon(runtime, 'source-' + (entry[0] === 'text' ? 'text' : entry[0])), 'source-' + entry[0], entry[0] === 'text' ? 'contents' : entry[0], entry[0] === 'file', function () { return invoke('workbench.add.enabledInput', entry[0]); }, entry[1]); }), function () { return invoke('workbench.add.enabledInput', 'file'); }, function () { return invoke('workbench.add.enabledInput', 'url'); }, formats.map(function (format) { return h(__makeTemplateObject(["<option value=", ">", "</option>"], ["<option value=", ">", "</option>"]), format.value, format.label); }), icon(runtime, 'chevron', 'workbench-select-chevron'), workbench.detailDisclosure.render(h, {
                id: 'add-import-settings', toggleId: 'add-import-settings-toggle',
                panelId: 'add-import-settings-panel', label: 'Advanced settings',
                ownerClass: 'workbench-options workbench-form-subgroup'
            }, h(__makeTemplateObject(["<div class=\"workbench-form-grid\">\n                            <div class=\"workbench-field workbench-disclosure__field\"><label for=\"baseURI\">Base URI</label>\n                                <input id=\"baseURI\" name=\"baseURI\" type=\"text\" size=\"48\" value=", " />\n                                <label class=\"workbench-check\" for=\"overrideContext\"><input type=\"checkbox\" id=\"overrideContext\" name=\"overrideContext\"\n                                    ?checked=", " @change=", " />\n                                    <span>Override parsed contexts with this context</span></label>\n                            </div>\n                            <div class=\"workbench-field workbench-disclosure__field\"><label for=\"context\">Context</label>\n                                <input id=\"context\" name=\"context\" type=\"text\" size=\"48\" aria-describedby=\"context-help\"\n                                    value=", " ?disabled=", " />\n                                <p id=\"context-help\" class=\"workbench-help\">RDF context may be an IRI, blank node, or the default graph. With override off, embedded contexts are preserved; contextless data uses the default graph. Base URI resolves relative RDF identifiers; it does not choose a graph context.</p>\n                            </div>\n                            <div class=\"workbench-field workbench-disclosure__field\"><label for=\"transaction-setting__org.eclipse.rdf4j.common.transaction.IsolationLevel\">Isolation level</label>\n                                <select id=\"transaction-setting__org.eclipse.rdf4j.common.transaction.IsolationLevel\"\n                                    name=\"transaction-setting__org.eclipse.rdf4j.common.transaction.IsolationLevel\">\n                                    <option value=\"\" ?selected=", ">Default</option>\n                                    ", "\n                                </select>\n                                <div class=\"hint\">Choose the transaction isolation level used for this import.</div>\n                            </div>\n                        </div>"], ["<div class=\"workbench-form-grid\">\n                            <div class=\"workbench-field workbench-disclosure__field\"><label for=\"baseURI\">Base URI</label>\n                                <input id=\"baseURI\" name=\"baseURI\" type=\"text\" size=\"48\" value=", " />\n                                <label class=\"workbench-check\" for=\"overrideContext\"><input type=\"checkbox\" id=\"overrideContext\" name=\"overrideContext\"\n                                    ?checked=", " @change=", " />\n                                    <span>Override parsed contexts with this context</span></label>\n                            </div>\n                            <div class=\"workbench-field workbench-disclosure__field\"><label for=\"context\">Context</label>\n                                <input id=\"context\" name=\"context\" type=\"text\" size=\"48\" aria-describedby=\"context-help\"\n                                    value=", " ?disabled=", " />\n                                <p id=\"context-help\" class=\"workbench-help\">RDF context may be an IRI, blank node, or the default graph. With override off, embedded contexts are preserved; contextless data uses the default graph. Base URI resolves relative RDF identifiers; it does not choose a graph context.</p>\n                            </div>\n                            <div class=\"workbench-field workbench-disclosure__field\"><label for=\"transaction-setting__org.eclipse.rdf4j.common.transaction.IsolationLevel\">Isolation level</label>\n                                <select id=\"transaction-setting__org.eclipse.rdf4j.common.transaction.IsolationLevel\"\n                                    name=\"transaction-setting__org.eclipse.rdf4j.common.transaction.IsolationLevel\">\n                                    <option value=\"\" ?selected=", ">Default</option>\n                                    ", "\n                                </select>\n                                <div class=\"hint\">Choose the transaction isolation level used for this import.</div>\n                            </div>\n                        </div>"]), text(pageValue(model, 'baseURI')), !!text(pageValue(model, 'context')), function () { return invoke('workbench.add.handleContextOverride'); }, text(pageValue(model, 'context')), !text(pageValue(model, 'context')), !selectedIsolation, isolationOptions.map(function (row) { return h(__makeTemplateObject(["<option value=", "\n                                        ?selected=", ">\n                                        ", "</option>"], ["<option value=", "\n                                        ?selected=", ">\n                                        ", "</option>"]), text(row['isolation-level-option']), text(row['isolation-level-option']) === selectedIsolation, text(row['isolation-level-option-label']) || text(row['isolation-level-option'])); }))), icon(runtime, 'upload'));
        }
        function removePage(runtime, model, context) {
            var h = runtime.html;
            return h(__makeTemplateObject(["", "\n                <p id=\"remove-warning\" class=\"WARN\" role=\"alert\">Only statements matching the supplied values will be removed. An empty form is rejected.</p>\n                <p>Values use RDF syntax: IRIs in angle brackets, blank nodes as _:nodeID, and literals in double quotes with optional language or datatype.</p>\n                <details id=\"remove-examples\" class=\"workbench-options\"><summary>Examples", "</summary>\n                    <ul><li>URI: <tt>&lt;http://foo.com/bar&gt;</tt></li><li>BNode: <tt>_:nodeID</tt></li>\n                        <li>Literal: <tt>\"Hello\"</tt>, <tt>\"Hello\"@en</tt>, or <tt>\"Hello\"^^&lt;http://bar.com/foo&gt;</tt></li></ul>\n                </details>\n                ", "\n                <form id=\"remove-form\" class=\"workbench-island\" method=\"post\" action=\"remove\">\n                    <table class=\"dataentry\"><tbody>\n                        ", "\n                        <tr><td></td><td><span class=\"workbench-action workbench-action--danger\"><label class=\"workbench-action-hit-area\">\n                            ", "<span class=\"workbench-action-label\"><input type=\"submit\" value=\"Remove\" /></span>\n                        </label></span></td><td></td></tr>\n                    </tbody></table>\n                </form>"], ["", "\n                <p id=\"remove-warning\" class=\"WARN\" role=\"alert\">Only statements matching the supplied values will be removed. An empty form is rejected.</p>\n                <p>Values use RDF syntax: IRIs in angle brackets, blank nodes as _:nodeID, and literals in double quotes with optional language or datatype.</p>\n                <details id=\"remove-examples\" class=\"workbench-options\"><summary>Examples", "</summary>\n                    <ul><li>URI: <tt>&lt;http://foo.com/bar&gt;</tt></li><li>BNode: <tt>_:nodeID</tt></li>\n                        <li>Literal: <tt>\"Hello\"</tt>, <tt>\"Hello\"@en</tt>, or <tt>\"Hello\"^^&lt;http://bar.com/foo&gt;</tt></li></ul>\n                </details>\n                ", "\n                <form id=\"remove-form\" class=\"workbench-island\" method=\"post\" action=\"remove\">\n                    <table class=\"dataentry\"><tbody>\n                        ", "\n                        <tr><td></td><td><span class=\"workbench-action workbench-action--danger\"><label class=\"workbench-action-hit-area\">\n                            ", "<span class=\"workbench-action-label\"><input type=\"submit\" value=\"Remove\" /></span>\n                        </label></span></td><td></td></tr>\n                    </tbody></table>\n                </form>"]), context.repositoryId === 'SYSTEM' ? h(__makeTemplateObject(["<p class=\"WARN\">The SYSTEM repository is intended for system use.</p>"], ["<p class=\"WARN\">The SYSTEM repository is intended for system use.</p>"])) : '', icon(runtime, 'chevron', 'workbench-disclosure-chevron'), pageValue(model, 'error-message') ? h(__makeTemplateObject(["<p class=\"error\" role=\"alert\">", "</p>"], ["<p class=\"error\" role=\"alert\">", "</p>"]), text(pageValue(model, 'error-message'))) : '', [['subj', 'Subject', 'text'], ['pred', 'Predicate', 'text'], ['obj', 'Object', 'textarea'], ['context', 'Context', 'text']].map(function (entry) { return h(__makeTemplateObject(["<tr>\n                            <th><label for=", ">", "</label></th><td>", "</td><td></td>\n                        </tr>"], ["<tr>\n                            <th><label for=", ">", "</label></th><td>", "</td><td></td>\n                        </tr>"]), entry[0], entry[1], entry[2] === 'textarea'
                ? h(__makeTemplateObject(["<textarea id=\"obj\" name=\"obj\" cols=\"70\">", "</textarea>"], ["<textarea id=\"obj\" name=\"obj\" cols=\"70\">", "</textarea>"]), text(pageValue(model, 'obj'))) : h(__makeTemplateObject(["<input id=", " name=", " type=\"text\" size=\"48\" value=", " />"], ["<input id=", " name=", " type=\"text\" size=\"48\" value=", " />"]), entry[0], entry[0], text(pageValue(model, entry[0])))); }), icon(runtime, 'remove'));
        }
        function clearPage(runtime, model, context) {
            var h = runtime.html;
            return h(__makeTemplateObject(["", "\n                <p id=\"clear-warning\" class=\"WARN\" role=\"alert\">Clearing without a context removes every statement in this repository.</p>\n                ", "\n                <form id=\"clear-form\" class=\"workbench-island\" method=\"post\" action=\"clear\">\n                    <table class=\"dataentry\"><tbody>\n                        <tr><th><label for=\"context\">Context</label></th><td><input id=\"context\" name=\"context\" size=\"48\" type=\"text\" value=", " /></td><td></td></tr>\n                        <tr><td></td><td><span class=\"workbench-action workbench-action--danger\"><label class=\"workbench-action-hit-area\">\n                            ", "<span class=\"workbench-action-label\"><input type=\"submit\" value=\"Clear context\" /></span>\n                        </label></span></td></tr>\n                    </tbody></table>\n                </form>"], ["", "\n                <p id=\"clear-warning\" class=\"WARN\" role=\"alert\">Clearing without a context removes every statement in this repository.</p>\n                ", "\n                <form id=\"clear-form\" class=\"workbench-island\" method=\"post\" action=\"clear\">\n                    <table class=\"dataentry\"><tbody>\n                        <tr><th><label for=\"context\">Context</label></th><td><input id=\"context\" name=\"context\" size=\"48\" type=\"text\" value=", " /></td><td></td></tr>\n                        <tr><td></td><td><span class=\"workbench-action workbench-action--danger\"><label class=\"workbench-action-hit-area\">\n                            ", "<span class=\"workbench-action-label\"><input type=\"submit\" value=\"Clear context\" /></span>\n                        </label></span></td></tr>\n                    </tbody></table>\n                </form>"]), context.repositoryId === 'SYSTEM' ? h(__makeTemplateObject(["<p class=\"WARN\">The SYSTEM repository is intended for system use.</p>"], ["<p class=\"WARN\">The SYSTEM repository is intended for system use.</p>"])) : '', pageValue(model, 'error-message') ? h(__makeTemplateObject(["<p class=\"error\" role=\"alert\">", "</p>"], ["<p class=\"error\" role=\"alert\">", "</p>"]), text(pageValue(model, 'error-message'))) : '', text(pageValue(model, 'context')), icon(runtime, 'clear'));
        }
        function updatePage(runtime, model, context) {
            var h = runtime.html;
            var error = text(pageValue(model, 'error-message'));
            var query = text(pageValue(model, 'update')) || '\n\t';
            var mappings = context.linked && context.linked.namespaces
                ? context.linked.namespaces.namespaceMap : model.namespaceMap;
            if (typeof window !== 'undefined') {
                window.namespaces = mappings || {};
                window.sparqlNamespaces = mappings || {};
            }
            return h(__makeTemplateObject(["<form id=\"update-form\" class=\"workbench-island\" action=\"update\" method=\"post\"\n                    @submit=", ">\n                <div id=\"update-editor\" class=\"workbench-field\"><label for=\"update\">SPARQL Update</label>\n                    <textarea id=\"update\" name=\"update\" rows=\"16\" cols=\"80\">", "</textarea>\n                    <span id=\"updateString.errors\" class=\"error\" role=\"alert\">", "</span>\n                </div>\n                <div id=\"update-actions\" class=\"workbench-form-actions\"><span class=\"workbench-action workbench-action--primary\">\n                    <label class=\"workbench-action-hit-area\">", "<span class=\"workbench-action-label\"><input type=\"submit\" value=\"Execute\" /></span></label>\n                </span></div>\n            </form>"], ["<form id=\"update-form\" class=\"workbench-island\" action=\"update\" method=\"post\"\n                    @submit=", ">\n                <div id=\"update-editor\" class=\"workbench-field\"><label for=\"update\">SPARQL Update</label>\n                    <textarea id=\"update\" name=\"update\" rows=\"16\" cols=\"80\">", "</textarea>\n                    <span id=\"updateString.errors\" class=\"error\" role=\"alert\">", "</span>\n                </div>\n                <div id=\"update-actions\" class=\"workbench-form-actions\"><span class=\"workbench-action workbench-action--primary\">\n                    <label class=\"workbench-action-hit-area\">", "<span class=\"workbench-action-label\"><input type=\"submit\" value=\"Execute\" /></span></label>\n                </span></div>\n            </form>"]), function (event) {
                var globalWorkbench = window.workbench;
                if (globalWorkbench.update && globalWorkbench.update.doSubmit) {
                    if (!globalWorkbench.update.doSubmit()) {
                        event.preventDefault();
                    }
                }
            }, query, error, icon(runtime, 'execute'));
        }
        function serverPage(runtime, model, context) {
            var h = runtime.html;
            var row = firstRecord(model);
            var info = workbenchData(context);
            var server = text(field(row, 'server') || info.server || info.location);
            return h(__makeTemplateObject(["<form id=\"server-form\" action=\"server\" method=\"post\"\n                    @submit=", ">\n                <div class=\"workbench-form-grid server-connection-fields\"><div class=\"workbench-field\">\n                    <label for=\"workbench-server\">Server</label><input id=\"workbench-server\" name=\"workbench-server\" type=\"text\" size=\"40\" value=", " />\n                    <div class=\"hint\">Enter the URL of an RDF4J Server.</div><span class=\"error\" role=\"alert\">", "</span>\n                </div></div>\n                ", "\n                <div id=\"server-change-actions\" class=\"workbench-form-actions\"><span class=\"workbench-action workbench-action--primary\">\n                    <label class=\"workbench-action-hit-area\">", "<span class=\"workbench-action-label\"><input type=\"submit\" value=\"Change\" /></span></label>\n                </span></div>\n            </form>"], ["<form id=\"server-form\" action=\"server\" method=\"post\"\n                    @submit=", ">\n                <div class=\"workbench-form-grid server-connection-fields\"><div class=\"workbench-field\">\n                    <label for=\"workbench-server\">Server</label><input id=\"workbench-server\" name=\"workbench-server\" type=\"text\" size=\"40\" value=", " />\n                    <div class=\"hint\">Enter the URL of an RDF4J Server.</div><span class=\"error\" role=\"alert\">", "</span>\n                </div></div>\n                ", "\n                <div id=\"server-change-actions\" class=\"workbench-form-actions\"><span class=\"workbench-action workbench-action--primary\">\n                    <label class=\"workbench-action-hit-area\">", "<span class=\"workbench-action-label\"><input type=\"submit\" value=\"Change\" /></span></label>\n                </span></div>\n            </form>"]), function (event) {
                var globalWindow = typeof window !== 'undefined' ? window : null;
                if (globalWindow && typeof globalWindow.changeServer === 'function') {
                    globalWindow.changeServer(event);
                }
            }, server, text(field(row, 'error-message')), workbench.detailDisclosure.render(h, {
                id: 'server-auth', toggleId: 'server-auth-toggle', panelId: 'server-auth-panel',
                label: 'Advanced settings', ownerClass: 'workbench-options workbench-form-subgroup'
            }, h(__makeTemplateObject(["<div class=\"workbench-form-grid\">\n                        <div class=\"workbench-field workbench-disclosure__field\"><label for=\"server-user\">User</label><input id=\"server-user\" name=\"server-user\"\n                            type=\"text\" size=\"32\" value=", " /></div>\n                        <div class=\"workbench-field workbench-disclosure__field\"><label for=\"server-password\">Password</label><input id=\"server-password\" name=\"server-password\" type=\"password\" size=\"32\" value=\"\" /></div>\n                    </div>"], ["<div class=\"workbench-form-grid\">\n                        <div class=\"workbench-field workbench-disclosure__field\"><label for=\"server-user\">User</label><input id=\"server-user\" name=\"server-user\"\n                            type=\"text\" size=\"32\" value=", " /></div>\n                        <div class=\"workbench-field workbench-disclosure__field\"><label for=\"server-password\">Password</label><input id=\"server-password\" name=\"server-password\" type=\"password\" size=\"32\" value=\"\" /></div>\n                    </div>"]), text(field(row, 'server-user')))), icon(runtime, 'update'));
        }
        function queryFeatureEnabled(context, id) {
            var features = context.workbench && context.workbench.queryFeatures;
            return !features || typeof features[id] === 'undefined' || features[id] !== false;
        }
        function anyQueryFeatureEnabled(context, ids) {
            var features = context.workbench && context.workbench.queryFeatures;
            var configured = !!features && ids.some(function (id) { return Object.prototype.hasOwnProperty.call(features, id); });
            return !configured || ids.some(function (id) { return queryFeatureEnabled(context, id); });
        }
        function effectiveQueryFeatureOption(context, requested, options, preference) {
            var requestedOption = options.filter(function (option) { return option.value === requested
                && queryFeatureEnabled(context, option.feature); })[0];
            if (requestedOption) {
                return requestedOption.value;
            }
            var preferred = preference.map(function (value) { return options.filter(function (option) { return option.value === value
                && queryFeatureEnabled(context, option.feature); })[0]; }).filter(function (option) { return !!option; })[0];
            return preferred ? preferred.value : '';
        }
        function queryExplainEnabled(context) {
            if (!queryFeatureEnabled(context, 'query-explain')) {
                return false;
            }
            var features = context.workbench && context.workbench.queryFeatures;
            var levels = ['unoptimized', 'optimized', 'executed', 'telemetry', 'timed']
                .map(function (level) { return 'explain-level-' + level; });
            var formats = ['text', 'dot', 'json'].map(function (format) { return 'explain-format-' + format; });
            var anyConfigured = function (ids) { return !!features
                && ids.some(function (id) { return Object.prototype.hasOwnProperty.call(features, id); }); };
            var anyEnabled = function (ids) { return ids.some(function (id) { return queryFeatureEnabled(context, id); }); };
            return (!anyConfigured(levels) || anyEnabled(levels))
                && (!anyConfigured(formats) || anyEnabled(formats));
        }
        function queryExplainSettingsEnabled(context) {
            return anyQueryFeatureEnabled(context, [
                'explain-format-text', 'explain-format-dot', 'explain-format-json',
                'explain-level-unoptimized', 'explain-level-optimized', 'explain-level-executed',
                'explain-level-telemetry', 'explain-level-timed', 'explain-highlight-syntax',
                'explain-highlight-hotspot', 'explain-property-selection'
            ]);
        }
        function queryPane(runtime, options, context) {
            var h = runtime.html;
            var compare = !!options.compare;
            var queryId = compare ? 'query-compare' : 'query';
            var suffix = compare ? '-compare' : '';
            var explanation = compare ? '' : text(options.explanation);
            var paneId = compare ? 'query-compare-pane' : 'query-primary-pane';
            var explainFormats = [
                { value: 'text', label: 'Text', feature: 'explain-format-text' },
                { value: 'dot', label: 'DOT', feature: 'explain-format-dot' },
                { value: 'json', label: 'JSON', feature: 'explain-format-json' }
            ];
            var explainLevels = [
                { value: 'Unoptimized', feature: 'explain-level-unoptimized' },
                { value: 'Optimized', feature: 'explain-level-optimized' },
                { value: 'Executed', feature: 'explain-level-executed' },
                { value: 'Telemetry', feature: 'explain-level-telemetry' },
                { value: 'Timed', feature: 'explain-level-timed' }
            ];
            var selectedFormat = effectiveQueryFeatureOption(context, text(options.explanationFormat), explainFormats, ['text', 'dot', 'json']);
            var selectedLevel = effectiveQueryFeatureOption(context, text(options.explanationLevel), explainLevels, ['Optimized', 'Unoptimized', 'Executed', 'Telemetry', 'Timed']);
            var allExplainFormatsDisabled = !anyQueryFeatureEnabled(context, explainFormats.map(function (option) { return option.feature; }));
            var allExplainLevelsDisabled = !anyQueryFeatureEnabled(context, explainLevels.map(function (option) { return option.feature; }));
            var allHighlightingDisabled = !anyQueryFeatureEnabled(context, ['explain-highlight-syntax', 'explain-highlight-hotspot']);
            var propertySelectionEnabled = queryFeatureEnabled(context, 'explain-property-selection');
            var formatOption = function (format) {
                var enabled = queryFeatureEnabled(context, format.feature);
                var selected = format.value === selectedFormat;
                if (format.value === 'text') {
                    return enabled
                        ? h(__makeTemplateObject(["<option value=", " ?selected=", ">Text</option>"], ["<option value=", " ?selected=", ">Text</option>"]), format.value, selected) : h(__makeTemplateObject(["<option value=", " ?selected=", " ?hidden=", " ?disabled=", ">Text</option>"], ["<option value=", " ?selected=", " ?hidden=", " ?disabled=", ">Text</option>"]), format.value, false, true, true);
                }
                if (format.value === 'dot') {
                    return enabled
                        ? h(__makeTemplateObject(["<option value=", " ?selected=", ">DOT</option>"], ["<option value=", " ?selected=", ">DOT</option>"]), format.value, selected) : h(__makeTemplateObject(["<option value=", " ?selected=", " ?hidden=", " ?disabled=", ">DOT</option>"], ["<option value=", " ?selected=", " ?hidden=", " ?disabled=", ">DOT</option>"]), format.value, false, true, true);
                }
                return enabled
                    ? h(__makeTemplateObject(["<option value=", " ?selected=", ">JSON</option>"], ["<option value=", " ?selected=", ">JSON</option>"]), format.value, selected) : h(__makeTemplateObject(["<option value=", " ?selected=", " ?hidden=", " ?disabled=", ">JSON</option>"], ["<option value=", " ?selected=", " ?hidden=", " ?disabled=", ">JSON</option>"]), format.value, false, true, true);
            };
            var levelOption = function (level) {
                var enabled = queryFeatureEnabled(context, level.feature);
                var selected = level.value === selectedLevel;
                switch (level.value) {
                    case 'Unoptimized':
                        return enabled
                            ? h(__makeTemplateObject(["<option value=", " ?selected=", ">Unoptimized</option>"], ["<option value=", " ?selected=", ">Unoptimized</option>"]), level.value, selected) : h(__makeTemplateObject(["<option value=", " ?hidden=", " ?disabled=", " ?selected=", ">Unoptimized</option>"], ["<option value=", " ?hidden=", " ?disabled=", " ?selected=", ">Unoptimized</option>"]), level.value, true, true, false);
                    case 'Optimized':
                        return enabled
                            ? h(__makeTemplateObject(["<option value=", " ?selected=", ">Optimized</option>"], ["<option value=", " ?selected=", ">Optimized</option>"]), level.value, selected) : h(__makeTemplateObject(["<option value=", " ?hidden=", " ?disabled=", " ?selected=", ">Optimized</option>"], ["<option value=", " ?hidden=", " ?disabled=", " ?selected=", ">Optimized</option>"]), level.value, true, true, false);
                    case 'Executed':
                        return enabled
                            ? h(__makeTemplateObject(["<option value=", " ?selected=", ">Executed</option>"], ["<option value=", " ?selected=", ">Executed</option>"]), level.value, selected) : h(__makeTemplateObject(["<option value=", " ?hidden=", " ?disabled=", " ?selected=", ">Executed</option>"], ["<option value=", " ?hidden=", " ?disabled=", " ?selected=", ">Executed</option>"]), level.value, true, true, false);
                    case 'Telemetry':
                        return enabled
                            ? h(__makeTemplateObject(["<option value=", " ?selected=", ">Telemetry</option>"], ["<option value=", " ?selected=", ">Telemetry</option>"]), level.value, selected) : h(__makeTemplateObject(["<option value=", " ?hidden=", " ?disabled=", " ?selected=", ">Telemetry</option>"], ["<option value=", " ?hidden=", " ?disabled=", " ?selected=", ">Telemetry</option>"]), level.value, true, true, false);
                    default:
                        return enabled
                            ? h(__makeTemplateObject(["<option value=", " ?selected=", ">Timed</option>"], ["<option value=", " ?selected=", ">Timed</option>"]), level.value, selected) : h(__makeTemplateObject(["<option value=", " ?hidden=", " ?disabled=", " ?selected=", ">Timed</option>"], ["<option value=", " ?hidden=", " ?disabled=", " ?selected=", ">Timed</option>"]), level.value, true, true, false);
                }
            };
            return h(__makeTemplateObject(["<section id=", " class=", ">\n                <div class=\"query-form__row query-form__row--stacked\">\n                    <div class=\"query-editor-header\"><label class=\"query-form__label\" for=", ">", "</label>\n                        ", "</div>\n                    <div class=\"query-form__field\">", "</div>\n                </div>\n                <div class=\"query-form__row\"><span class=\"query-form__label query-form__label--blank\"></span>\n                    <div class=\"query-form__field\"><span id=", " class=\"error\">", "</span></div>\n                </div>\n                <div id=", " class=\"query-form__row query-form__row--stacked\"\n                    ?hidden=", "\n                    style=", ">\n                    <span class=\"query-form__label\">Query explanation</span><div class=\"query-form__field\">\n                        <div id=", " class=\"query-explanation-status\" aria-live=\"polite\"></div>\n                        <div class=\"query-explanation-toolbar\">\n                            ", "\n                            ", "\n                        </div>\n                        <div class=\"query-explanation-surface\"><div id=", "\n                            class=\"query-explanation-overlay\" aria-hidden=\"true\"></div>\n                            ", "\n                            ", "\n                            ", "\n                        </div>\n                        ", "\n                    </div>\n                </div>\n            </section>"], ["<section id=", " class=", ">\n                <div class=\"query-form__row query-form__row--stacked\">\n                    <div class=\"query-editor-header\"><label class=\"query-form__label\" for=", ">", "</label>\n                        ", "</div>\n                    <div class=\"query-form__field\">", "</div>\n                </div>\n                <div class=\"query-form__row\"><span class=\"query-form__label query-form__label--blank\"></span>\n                    <div class=\"query-form__field\"><span id=", " class=\"error\">", "</span></div>\n                </div>\n                <div id=", " class=\"query-form__row query-form__row--stacked\"\n                    ?hidden=", "\n                    style=", ">\n                    <span class=\"query-form__label\">Query explanation</span><div class=\"query-form__field\">\n                        <div id=", " class=\"query-explanation-status\" aria-live=\"polite\"></div>\n                        <div class=\"query-explanation-toolbar\">\n                            ", "\n                            ", "\n                        </div>\n                        <div class=\"query-explanation-surface\"><div id=", "\n                            class=\"query-explanation-overlay\" aria-hidden=\"true\"></div>\n                            ", "\n                            ", "\n                            ", "\n                        </div>\n                        ", "\n                    </div>\n                </div>\n            </section>"]), paneId, compare
                ? 'query-compare-pane query-compare-pane--secondary'
                : 'query-compare-pane query-compare-pane--primary', queryId, compare ? 'Compare query' : 'Query', compare ? h(__makeTemplateObject(["<button id=\"query-compare-close\" class=\"query-compare-pane__close\" type=\"button\"\n                            aria-label=\"Close comparison\" title=\"Close comparison\"\n                            @click=", ">", "</button>"], ["<button id=\"query-compare-close\" class=\"query-compare-pane__close\" type=\"button\"\n                            aria-label=\"Close comparison\" title=\"Close comparison\"\n                            @click=", ">", "</button>"]), function () { return invoke('workbench.query.closeComparePane'); }, icon(runtime, 'close', 'query-compare-pane__close-icon')) : '', compare
                ? h(__makeTemplateObject(["<textarea id=\"query-compare\" rows=\"16\" cols=\"80\" wrap=\"soft\"></textarea>"], ["<textarea id=\"query-compare\" rows=\"16\" cols=\"80\" wrap=\"soft\"></textarea>"])) : h(__makeTemplateObject(["<textarea id=\"query\" name=\"query\" rows=\"16\" cols=\"80\" wrap=\"soft\">", "</textarea>"], ["<textarea id=\"query\" name=\"query\" rows=\"16\" cols=\"80\" wrap=\"soft\">", "</textarea>"]), text(options.query)), 'queryString.errors' + suffix, compare ? '' : text(options.error), 'query-explanation-row' + suffix, !queryFeatureEnabled(context, 'query-explain'), !compare && !explanation ? 'display:none;' : '', 'query-explanation-status' + suffix, compare ? h(__makeTemplateObject(["<button id=\"copy-explanation-compare\" class=\"query-explanation-copy\"\n                                type=\"button\" ?hidden=", ">Copy explanation</button>"], ["<button id=\"copy-explanation-compare\" class=\"query-explanation-copy\"\n                                type=\"button\" ?hidden=", ">Copy explanation</button>"]), !queryFeatureEnabled(context, 'explain-copy')) : h(__makeTemplateObject(["<button id=\"copy-explanation\" class=\"query-explanation-copy\"\n                                    type=\"button\" ?hidden=", ">Copy explanation</button>"], ["<button id=\"copy-explanation\" class=\"query-explanation-copy\"\n                                    type=\"button\" ?hidden=", ">Copy explanation</button>"]), !queryFeatureEnabled(context, 'explain-copy')), compare ? '' : h(__makeTemplateObject(["<select id=\"explain-format\" name=\"explain-format\"\n                                ?hidden=", ">\n                                ", "\n                            </select><select id=\"explain-level\" ?hidden=", ">\n                                ", "\n                            </select>\n                            ", "\n                            "], ["<select id=\"explain-format\" name=\"explain-format\"\n                                ?hidden=", ">\n                                ", "\n                            </select><select id=\"explain-level\" ?hidden=", ">\n                                ", "\n                            </select>\n                            ", "\n                            "]), allExplainFormatsDisabled, explainFormats.map(formatOption), allExplainLevelsDisabled, explainLevels.map(levelOption), workbench.detailDisclosure.render(h, {
                id: 'explanation-settings', toggleId: 'explanation-settings-toggle',
                panelId: 'explanation-settings-panel', label: 'Config', hidden: !queryExplainSettingsEnabled(context),
                ownerClass: 'query-explanation-settings', toggleClass: 'query-explanation-settings__toggle',
                panelClass: 'query-explanation-settings__panel',
                panelRole: 'group'
            }, h(__makeTemplateObject(["\n                                    <div class=\"query-explanation-settings__section\" ?hidden=", ">\n                                        <div class=\"query-explanation-settings__header\"><strong>Highlighting</strong></div>\n                                        <div class=\"query-explanation-settings__highlighting\">\n                                            <span id=\"explanation-highlight-mode\" class=\"query-explanation-highlight-mode\" role=\"radiogroup\"\n                                                aria-label=\"Text explanation highlighting\" ?hidden=", ">\n                                                <label class=\"workbench-choice\" for=\"explanation-highlight-syntax\"\n                                                    ?hidden=", ">\n                                                    <input id=\"explanation-highlight-syntax\" name=\"explanation-highlight-mode\" type=\"radio\" value=\"syntax\" checked\n                                                        ?hidden=", " />\n                                                    <span>Normal</span>\n                                                </label>\n                                                <label class=\"workbench-choice\" for=\"explanation-highlight-hotspot\"\n                                                    ?hidden=", ">\n                                                    <input id=\"explanation-highlight-hotspot\" name=\"explanation-highlight-mode\" type=\"radio\" value=\"hotspot\"\n                                                        ?hidden=", " />\n                                                    <span>Heatmap</span>\n                                                </label>\n                                            </span><span id=\"explanation-hotspot-legend\" aria-live=\"polite\"></span>\n                                        </div>\n                                    </div>\n                                    <div id=\"explanation-property-config\" class=\"query-explanation-settings__section query-explanation-property-config\"\n                                        ?hidden=", ">\n                                        <div class=\"query-explanation-property-config__header\">\n                                            <div class=\"query-explanation-property-config__title\">\n                                                <strong>Visible properties</strong><span id=\"explanation-property-count\" aria-live=\"polite\"></span>\n                                            </div>\n                                            <div class=\"query-explanation-property-config__actions\">\n                                                <button id=\"explanation-properties-all\" type=\"button\" ?hidden=", ">All</button>\n                                                <button id=\"explanation-properties-none\" type=\"button\" ?hidden=", ">None</button>\n                                            </div>\n                                        </div>\n                                        <div id=\"explanation-property-options\" class=\"query-explanation-property-config__options\"\n                                            role=\"group\" aria-label=\"Visible query plan properties\"></div>\n                                        <p class=\"query-explanation-property-config__hint\">Plan structure always remains visible.</p>\n                                    </div>\n                            "], ["\n                                    <div class=\"query-explanation-settings__section\" ?hidden=", ">\n                                        <div class=\"query-explanation-settings__header\"><strong>Highlighting</strong></div>\n                                        <div class=\"query-explanation-settings__highlighting\">\n                                            <span id=\"explanation-highlight-mode\" class=\"query-explanation-highlight-mode\" role=\"radiogroup\"\n                                                aria-label=\"Text explanation highlighting\" ?hidden=", ">\n                                                <label class=\"workbench-choice\" for=\"explanation-highlight-syntax\"\n                                                    ?hidden=", ">\n                                                    <input id=\"explanation-highlight-syntax\" name=\"explanation-highlight-mode\" type=\"radio\" value=\"syntax\" checked\n                                                        ?hidden=", " />\n                                                    <span>Normal</span>\n                                                </label>\n                                                <label class=\"workbench-choice\" for=\"explanation-highlight-hotspot\"\n                                                    ?hidden=", ">\n                                                    <input id=\"explanation-highlight-hotspot\" name=\"explanation-highlight-mode\" type=\"radio\" value=\"hotspot\"\n                                                        ?hidden=", " />\n                                                    <span>Heatmap</span>\n                                                </label>\n                                            </span><span id=\"explanation-hotspot-legend\" aria-live=\"polite\"></span>\n                                        </div>\n                                    </div>\n                                    <div id=\"explanation-property-config\" class=\"query-explanation-settings__section query-explanation-property-config\"\n                                        ?hidden=", ">\n                                        <div class=\"query-explanation-property-config__header\">\n                                            <div class=\"query-explanation-property-config__title\">\n                                                <strong>Visible properties</strong><span id=\"explanation-property-count\" aria-live=\"polite\"></span>\n                                            </div>\n                                            <div class=\"query-explanation-property-config__actions\">\n                                                <button id=\"explanation-properties-all\" type=\"button\" ?hidden=", ">All</button>\n                                                <button id=\"explanation-properties-none\" type=\"button\" ?hidden=", ">None</button>\n                                            </div>\n                                        </div>\n                                        <div id=\"explanation-property-options\" class=\"query-explanation-property-config__options\"\n                                            role=\"group\" aria-label=\"Visible query plan properties\"></div>\n                                        <p class=\"query-explanation-property-config__hint\">Plan structure always remains visible.</p>\n                                    </div>\n                            "]), allHighlightingDisabled, allHighlightingDisabled, !queryFeatureEnabled(context, 'explain-highlight-syntax'), !queryFeatureEnabled(context, 'explain-highlight-syntax'), !queryFeatureEnabled(context, 'explain-highlight-hotspot'), !queryFeatureEnabled(context, 'explain-highlight-hotspot'), !propertySelectionEnabled, !propertySelectionEnabled, !propertySelectionEnabled))), 'query-explanation-overlay' + suffix, compare ? h(__makeTemplateObject(["<pre id=\"query-explanation-compare\" data-format=", "\n                                ?hidden=", ">", "</pre>"], ["<pre id=\"query-explanation-compare\" data-format=", "\n                                ?hidden=", ">", "</pre>"]), selectedFormat, !queryFeatureEnabled(context, 'explain-view-text'), explanation) : h(__makeTemplateObject(["<pre id=\"query-explanation\" data-format=", "\n                                    ?hidden=", ">", "</pre>"], ["<pre id=\"query-explanation\" data-format=", "\n                                    ?hidden=", ">", "</pre>"]), selectedFormat, !queryFeatureEnabled(context, 'explain-view-text'), explanation), compare ? h(__makeTemplateObject(["<div id=\"query-explanation-dot-view-compare\"\n                                ?hidden=", "></div>"], ["<div id=\"query-explanation-dot-view-compare\"\n                                ?hidden=", "></div>"]), !queryFeatureEnabled(context, 'explain-view-dot')) : h(__makeTemplateObject(["<div id=\"query-explanation-dot-view\"\n                                    ?hidden=", "></div>"], ["<div id=\"query-explanation-dot-view\"\n                                    ?hidden=", "></div>"]), !queryFeatureEnabled(context, 'explain-view-dot')), compare ? h(__makeTemplateObject(["<div id=\"query-explanation-json-view-compare\"\n                                ?hidden=", "></div>"], ["<div id=\"query-explanation-json-view-compare\"\n                                ?hidden=", "></div>"]), !queryFeatureEnabled(context, 'explain-view-json')) : h(__makeTemplateObject(["<div id=\"query-explanation-json-view\"\n                                    ?hidden=", "></div>"], ["<div id=\"query-explanation-json-view\"\n                                    ?hidden=", "></div>"]), !queryFeatureEnabled(context, 'explain-view-json')), compare ? '' : h(__makeTemplateObject(["<div id=\"query-explanation-controls-row\" class=\"query-explanation-controls-row-class\"\n                            ?hidden=", "\n                            style=", ">\n                            <span id=\"primary-explain-settings\" class=\"query-form__field--controls-group\">\n                                <span id=\"primary-explain-repeat-controls\" class=\"query-form__field--controls-group\">\n                                    <button id=\"rerun-explanation\" type=\"button\"\n                                        ?hidden=", "\n                                        data-query-rerun-enabled=", "\n                                        @click=", ">Explain again</button>\n                                    <span id=\"rerun-explanation-spinner\" class=\"query-explain-spinner\" aria-hidden=\"true\"></span>\n                                    <button id=\"rerun-explanation-cancel\" class=\"query-explain-cancel\" type=\"button\" disabled\n                                        ?hidden=", "\n                                        @click=", ">Cancel</button>\n                                </span>\n                                <span id=\"primary-explain-utility-controls\" class=\"query-form__field--controls-group\">\n                                    <button id=\"download-explanation\" type=\"button\" ?disabled=", "\n                                        ?hidden=", ">Download explanation</button>\n                                    <button id=\"compare-toggle\" type=\"button\" ?hidden=", "\n                                        @click=", ">Compare</button>\n                                </span>\n                            </span>\n                        </div>"], ["<div id=\"query-explanation-controls-row\" class=\"query-explanation-controls-row-class\"\n                            ?hidden=", "\n                            style=", ">\n                            <span id=\"primary-explain-settings\" class=\"query-form__field--controls-group\">\n                                <span id=\"primary-explain-repeat-controls\" class=\"query-form__field--controls-group\">\n                                    <button id=\"rerun-explanation\" type=\"button\"\n                                        ?hidden=", "\n                                        data-query-rerun-enabled=", "\n                                        @click=", ">Explain again</button>\n                                    <span id=\"rerun-explanation-spinner\" class=\"query-explain-spinner\" aria-hidden=\"true\"></span>\n                                    <button id=\"rerun-explanation-cancel\" class=\"query-explain-cancel\" type=\"button\" disabled\n                                        ?hidden=", "\n                                        @click=", ">Cancel</button>\n                                </span>\n                                <span id=\"primary-explain-utility-controls\" class=\"query-form__field--controls-group\">\n                                    <button id=\"download-explanation\" type=\"button\" ?disabled=", "\n                                        ?hidden=", ">Download explanation</button>\n                                    <button id=\"compare-toggle\" type=\"button\" ?hidden=", "\n                                        @click=", ">Compare</button>\n                                </span>\n                            </span>\n                        </div>"]), !queryFeatureEnabled(context, 'query-explain'), !explanation ? 'display:none;' : '', !queryFeatureEnabled(context, 'query-rerun'), queryFeatureEnabled(context, 'query-rerun') ? 'true' : 'false', function () { return invoke('workbench.query.runExplain', null, 'rerun-explanation'); }, !queryFeatureEnabled(context, 'explain-cancel'), function () { return invoke('workbench.query.cancelExplain'); }, !explanation, !queryFeatureEnabled(context, 'explain-download'), !queryFeatureEnabled(context, 'query-compare'), function () { return invoke('workbench.query.toggleCompareMode'); }));
        }
        function queryPage(runtime, model, context) {
            var h = runtime.html;
            var info = workbenchData(context);
            var defaults = info.defaults || info;
            var queryFormats = formatOptions(info.queryFormats || info['query-format']);
            var defaultQueryLanguage = text(pageValue(model, 'queryLn') || defaults['default-queryLn']);
            var selectedQueryLanguage = defaultQueryLanguage || (queryFormats.length ? queryFormats[0].value : 'SPARQL');
            var hideQueryLanguageRow = queryFormats.length === 1 && queryFormats[0].value === 'SPARQL';
            var defaultTimeout = text(pageValue(model, 'query-timeout') || defaults['default-query-timeout']) || '60';
            var query = text(pageValue(model, 'query'));
            var explanation = text(pageValue(model, 'explanation'));
            var explanationFormat = text(pageValue(model, 'explanation-format')) || 'text';
            var explanationLevel = text(pageValue(model, 'explanation-level')) || 'Optimized';
            var saveDisclosure = workbench.detailDisclosure.renderSeparated(h, {
                id: 'save-query-disclosure', toggleId: 'save-query-toggle', panelId: 'save-query-panel',
                label: 'Save query', ownerClass: 'query-disclosure query-save-disclosure',
                toggleClass: 'query-disclosure__toggle',
                panelClass: 'query-disclosure__body query-disclosure__panel query-save-disclosure__body',
                contentClass: 'workbench-disclosure__fields',
                toggleHidden: !queryFeatureEnabled(context, 'query-save')
            }, h(__makeTemplateObject(["<div class=\"workbench-disclosure__field query-save-disclosure__name-field\">\n                    <label class=\"query-form__label\" for=\"query-name\">Query name</label>\n                    <input id=\"query-name\" name=\"query-name\" type=\"text\" size=\"32\" maxlength=\"32\" value=\"\" />\n                </div>\n                <label class=\"query-option query-save-disclosure__private\"\n                    ?hidden=", ">\n                    <input id=\"save-private\" name=\"save-private\" type=\"checkbox\" value=\"true\"\n                        ?hidden=", " />Private</label>\n                <div class=\"workbench-disclosure__actions query-disclosure__actions\">\n                    <input id=\"save\" type=\"submit\" value=\"Save\" disabled\n                        ?hidden=", " /> <span id=\"save-feedback\"></span>\n                </div>"], ["<div class=\"workbench-disclosure__field query-save-disclosure__name-field\">\n                    <label class=\"query-form__label\" for=\"query-name\">Query name</label>\n                    <input id=\"query-name\" name=\"query-name\" type=\"text\" size=\"32\" maxlength=\"32\" value=\"\" />\n                </div>\n                <label class=\"query-option query-save-disclosure__private\"\n                    ?hidden=", ">\n                    <input id=\"save-private\" name=\"save-private\" type=\"checkbox\" value=\"true\"\n                        ?hidden=", " />Private</label>\n                <div class=\"workbench-disclosure__actions query-disclosure__actions\">\n                    <input id=\"save\" type=\"submit\" value=\"Save\" disabled\n                        ?hidden=", " /> <span id=\"save-feedback\"></span>\n                </div>"]), !queryFeatureEnabled(context, 'query-private-save'), !queryFeatureEnabled(context, 'query-private-save'), !queryFeatureEnabled(context, 'query-save')));
            var optionsDisclosure = workbench.detailDisclosure.renderSeparated(h, {
                id: 'query-options-disclosure', toggleId: 'query-options-toggle',
                panelId: 'query-options-panel', label: 'Options',
                ownerClass: 'query-disclosure query-options-disclosure',
                toggleClass: 'query-disclosure__toggle',
                panelClass: 'query-disclosure__body query-disclosure__panel',
                contentClass: 'workbench-disclosure__fields',
                toggleHidden: !queryFeatureEnabled(context, 'query-options')
            }, h(__makeTemplateObject(["<div class=\"workbench-disclosure__field\">\n                    <label for=\"query-timeout\">Query timeout</label>\n                    <input id=\"query-timeout\" name=\"query-timeout\" type=\"number\" min=\"0\" step=\"1\"\n                        value=", " ?hidden=", " />\n                </div>\n                <div class=\"workbench-disclosure__field\">\n                    <label class=\"query-option\" for=\"infer\"><input id=\"infer\" name=\"infer\" type=\"checkbox\" value=\"true\"\n                        ?checked=", "\n                        ?hidden=", " />\n                        <span>Include inferred statements</span></label>\n                </div>\n                <div class=\"workbench-disclosure__actions query-disclosure__actions\"><input id=\"query-reset-namespaces\" type=\"button\" value=\"Clear\"\n                    data-editor-namespaces-enabled=", "\n                    ?hidden=", "\n                    @click=", " /></div>"], ["<div class=\"workbench-disclosure__field\">\n                    <label for=\"query-timeout\">Query timeout</label>\n                    <input id=\"query-timeout\" name=\"query-timeout\" type=\"number\" min=\"0\" step=\"1\"\n                        value=", " ?hidden=", " />\n                </div>\n                <div class=\"workbench-disclosure__field\">\n                    <label class=\"query-option\" for=\"infer\"><input id=\"infer\" name=\"infer\" type=\"checkbox\" value=\"true\"\n                        ?checked=", "\n                        ?hidden=", " />\n                        <span>Include inferred statements</span></label>\n                </div>\n                <div class=\"workbench-disclosure__actions query-disclosure__actions\"><input id=\"query-reset-namespaces\" type=\"button\" value=\"Clear\"\n                    data-editor-namespaces-enabled=", "\n                    ?hidden=", "\n                    @click=", " /></div>"]), defaultTimeout, !queryFeatureEnabled(context, 'query-timeout'), text(defaults['default-infer']) === 'true', !queryFeatureEnabled(context, 'query-inferred-statements'), queryFeatureEnabled(context, 'editor-namespaces') ? 'true' : 'false', !queryFeatureEnabled(context, 'editor-namespaces'), function () { return invoke('workbench.query.resetNamespaces'); }));
            return h(__makeTemplateObject(["<div id=\"query-page\" class=\"query-page\"\n                    data-editor-fullscreen-enabled=", ">\n                <form id=\"query-form\" action=\"query\" method=\"post\"\n                    data-workbench-query-execution=\"true\" data-workbench-results-target=\"query-results\">\n                    <input type=\"hidden\" name=\"action\" id=\"action\" />\n                    <input type=\"hidden\" name=\"explain\" id=\"explain\" />\n                    <input type=\"hidden\" name=\"ref\" value=\"text\" />\n                    <input type=\"hidden\" name=\"include-query-text\" id=\"include-query-text\" value=\"false\" />\n                    <input type=\"hidden\" name=\"query-request-id\" id=\"query-request-id\" value=\"\" />\n                    <div class=\"query-form\">\n                        <div id=\"query-language-row\" class=\"query-form__row\"\n                            ?hidden=", ">\n                            <label class=\"query-form__label\" for=\"queryLn\">Query language</label>\n                            <div class=\"query-form__field\"><select id=\"queryLn\" name=\"queryLn\"\n                                @change=", ">\n                                ", "\n                            </select></div>\n                        </div>\n                        <div id=\"query-compare-toolbar\" class=\"query-compare-toolbar\" ?hidden=", ">\n                            <span id=\"query-sidebar-toggle-action\" class=\"workbench-action workbench-action--secondary\"\n                                data-workbench-action=\"menu\"><span class=\"workbench-action-label\">\n                                <button id=\"query-sidebar-toggle\" class=\"query-sidebar-toggle\" type=\"button\"\n                                    aria-hidden=\"true\" aria-expanded=\"false\" aria-controls=\"navigation\" tabindex=\"-1\"\n                                    data-show-label=\"Show navigation\" data-hide-label=\"Hide navigation\"\n                                    ?hidden=", "\n                                    @click=", ">\n                                    <span id=\"query-sidebar-toggle-icon\" class=\"query-sidebar-toggle__icon\" aria-hidden=\"true\">\n                                        ", "\n                                    </span>\n                                </button></span></span>\n                            <button id=\"query-compare-copy\" type=\"button\"\n                                ?hidden=", ">Copy</button>\n                            <button id=\"query-compare-swap\" type=\"button\"\n                                ?hidden=", ">Swap</button>\n                            <div id=\"query-compare-controls\" class=\"query-compare-toolbar__actions\">\n                                <button id=\"explain-compare-trigger\" class=\"query-compare-action\" type=\"button\"\n                                    data-query-refresh-enabled=", "\n                                    ?hidden=", "\n                                    @click=", ">Refresh explanations</button>\n                                <button id=\"explain-compare-cancel\" class=\"query-compare-action query-explain-cancel\" type=\"button\" disabled\n                                    ?hidden=", ">\n                                    <span id=\"explain-compare-cancel-icon\" class=\"query-compare-action__svg--cancel\" aria-hidden=\"true\">\u00D7</span>Cancel</button>\n                                <button id=\"query-diff-trigger\" class=\"query-compare-action\" type=\"button\" disabled\n                                    ?hidden=", "\n                                    @click=", ">\n                                    <span id=\"query-diff-trigger-icon\" class=\"query-compare-action__icon\" aria-hidden=\"true\">\u21C4</span>Diff</button>\n                            </div>\n                        </div>\n                        <div id=\"query-compare-layout\" class=\"query-compare-layout\"\n                            ?hidden=", ">\n                            ", "\n                            ", "\n                        </div>\n                        <div class=\"query-actions-toolbar workbench-action-toolbar\"><div class=\"query-form__field query-actions-toolbar__primary workbench-action-toolbar__primary\">\n                            <button id=\"exec\" class=\"query-action query-action--primary\" type=\"submit\"\n                                ?hidden=", ">", "<span>Execute</span></button>\n                            <input id=\"query-cancel\" class=\"query-cancel\" type=\"button\" value=\"Cancel\" aria-hidden=\"true\" disabled\n                                ?hidden=", "\n                                @click=", " />\n                            <button id=\"explain-trigger\" class=\"query-action\" type=\"button\"\n                                ?hidden=", "\n                                @click=", ">", "<span>Explain</span></button>\n                            <span id=\"explain-trigger-spinner\" class=\"query-explain-spinner\" aria-hidden=\"true\"></span>\n                            <span id=\"explain-trigger-cancel-action\" class=\"workbench-action workbench-action--danger query-explain-cancel\"\n                                ?hidden=", "\n                                data-workbench-action=\"cancel\"><span id=\"explain-trigger-cancel-icon\" aria-hidden=\"true\"></span>\n                                <input id=\"explain-trigger-cancel\" type=\"button\" value=\"Cancel\" aria-hidden=\"true\" disabled\n                                    ?hidden=", "\n                                    @click=", " /></span>\n                        </div>\n                        <div class=\"workbench-action-toolbar__actions\">\n                            <div class=\"workbench-action-toolbar__group\">", "", "</div>\n                        </div>\n                        <div class=\"workbench-action-toolbar__panels workbench-disclosure-track\">\n                            ", "", "\n                        </div>\n                    </div>\n                    </div>\n                </form>\n                <section id=\"query-results\" class=\"query-results\" aria-busy=\"false\" hidden aria-labelledby=\"query-results-heading\">\n                    <div class=\"query-results__header workbench-action-toolbar\">\n                        <div class=\"workbench-action-toolbar__primary\"><h2 id=\"query-results-heading\">Query result</h2></div>\n                        <div class=\"workbench-action-toolbar__actions\">\n                        <button id=\"query-results-fullscreen\" class=\"query-results__fullscreen\" type=\"button\"\n                            aria-label=\"Full screen\" title=\"Full screen\" hidden aria-pressed=\"false\"\n                            data-result-fullscreen-enabled=", "\n                            @click=", ">\n                            <svg class=\"query-results__fullscreen-icon\" viewBox=\"0 0 24 24\" focusable=\"false\" aria-hidden=\"true\">\n                                <path d=\"M4 9V4h5M15 4h5v5M20 15v5h-5M9 20H4v-5\"></path>\n                            </svg><span class=\"query-results__fullscreen-label\">Full screen</span>\n                        </button>\n                        </div>\n                    </div>\n                    <div id=\"query-results-loading\" class=\"query-results__loading\" hidden role=\"status\" aria-live=\"polite\">Loading query results...</div>\n                    <div id=\"query-results-status\" class=\"query-results__status\" role=\"status\" aria-live=\"polite\"></div>\n                    <iframe id=\"query-results-frame\" name=\"query-results-frame\" class=\"query-results__frame\"\n                        title=\"Query results\" hidden></iframe>\n                </section>\n                <div id=\"query-diff-modal\" class=\"query-diff-modal\" aria-hidden=\"true\">\n                    <div class=\"query-diff-modal__dialog\" role=\"dialog\" aria-modal=\"true\" aria-labelledby=\"query-diff-modal-title\">\n                        <div class=\"query-diff-modal__header\"><div id=\"query-diff-modal-title\" class=\"query-diff-modal__title\">Query diff</div>\n                            <button id=\"query-diff-close\" type=\"button\" value=\"Close\"\n                                @click=", ">Close</button></div>\n                        <div class=\"query-diff-modal__body\">\n                            <section class=\"query-diff-section query-diff-section--query\"><div class=\"query-diff-section__title\">Query</div><div id=\"query-diff-query\" class=\"query-diff-view\"></div></section>\n                            <section class=\"query-diff-section query-diff-section--explanation\"><div class=\"query-diff-section__title\">Explanation</div><div id=\"query-diff-explanation\" class=\"query-diff-view\">Comparison is not ready.</div></section>\n                        </div>\n                    </div>\n                </div>\n            </div>"], ["<div id=\"query-page\" class=\"query-page\"\n                    data-editor-fullscreen-enabled=", ">\n                <form id=\"query-form\" action=\"query\" method=\"post\"\n                    data-workbench-query-execution=\"true\" data-workbench-results-target=\"query-results\">\n                    <input type=\"hidden\" name=\"action\" id=\"action\" />\n                    <input type=\"hidden\" name=\"explain\" id=\"explain\" />\n                    <input type=\"hidden\" name=\"ref\" value=\"text\" />\n                    <input type=\"hidden\" name=\"include-query-text\" id=\"include-query-text\" value=\"false\" />\n                    <input type=\"hidden\" name=\"query-request-id\" id=\"query-request-id\" value=\"\" />\n                    <div class=\"query-form\">\n                        <div id=\"query-language-row\" class=\"query-form__row\"\n                            ?hidden=", ">\n                            <label class=\"query-form__label\" for=\"queryLn\">Query language</label>\n                            <div class=\"query-form__field\"><select id=\"queryLn\" name=\"queryLn\"\n                                @change=", ">\n                                ", "\n                            </select></div>\n                        </div>\n                        <div id=\"query-compare-toolbar\" class=\"query-compare-toolbar\" ?hidden=", ">\n                            <span id=\"query-sidebar-toggle-action\" class=\"workbench-action workbench-action--secondary\"\n                                data-workbench-action=\"menu\"><span class=\"workbench-action-label\">\n                                <button id=\"query-sidebar-toggle\" class=\"query-sidebar-toggle\" type=\"button\"\n                                    aria-hidden=\"true\" aria-expanded=\"false\" aria-controls=\"navigation\" tabindex=\"-1\"\n                                    data-show-label=\"Show navigation\" data-hide-label=\"Hide navigation\"\n                                    ?hidden=", "\n                                    @click=", ">\n                                    <span id=\"query-sidebar-toggle-icon\" class=\"query-sidebar-toggle__icon\" aria-hidden=\"true\">\n                                        ", "\n                                    </span>\n                                </button></span></span>\n                            <button id=\"query-compare-copy\" type=\"button\"\n                                ?hidden=", ">Copy</button>\n                            <button id=\"query-compare-swap\" type=\"button\"\n                                ?hidden=", ">Swap</button>\n                            <div id=\"query-compare-controls\" class=\"query-compare-toolbar__actions\">\n                                <button id=\"explain-compare-trigger\" class=\"query-compare-action\" type=\"button\"\n                                    data-query-refresh-enabled=", "\n                                    ?hidden=", "\n                                    @click=", ">Refresh explanations</button>\n                                <button id=\"explain-compare-cancel\" class=\"query-compare-action query-explain-cancel\" type=\"button\" disabled\n                                    ?hidden=", ">\n                                    <span id=\"explain-compare-cancel-icon\" class=\"query-compare-action__svg--cancel\" aria-hidden=\"true\">\u00D7</span>Cancel</button>\n                                <button id=\"query-diff-trigger\" class=\"query-compare-action\" type=\"button\" disabled\n                                    ?hidden=", "\n                                    @click=", ">\n                                    <span id=\"query-diff-trigger-icon\" class=\"query-compare-action__icon\" aria-hidden=\"true\">\u21C4</span>Diff</button>\n                            </div>\n                        </div>\n                        <div id=\"query-compare-layout\" class=\"query-compare-layout\"\n                            ?hidden=", ">\n                            ", "\n                            ", "\n                        </div>\n                        <div class=\"query-actions-toolbar workbench-action-toolbar\"><div class=\"query-form__field query-actions-toolbar__primary workbench-action-toolbar__primary\">\n                            <button id=\"exec\" class=\"query-action query-action--primary\" type=\"submit\"\n                                ?hidden=", ">", "<span>Execute</span></button>\n                            <input id=\"query-cancel\" class=\"query-cancel\" type=\"button\" value=\"Cancel\" aria-hidden=\"true\" disabled\n                                ?hidden=", "\n                                @click=", " />\n                            <button id=\"explain-trigger\" class=\"query-action\" type=\"button\"\n                                ?hidden=", "\n                                @click=", ">", "<span>Explain</span></button>\n                            <span id=\"explain-trigger-spinner\" class=\"query-explain-spinner\" aria-hidden=\"true\"></span>\n                            <span id=\"explain-trigger-cancel-action\" class=\"workbench-action workbench-action--danger query-explain-cancel\"\n                                ?hidden=", "\n                                data-workbench-action=\"cancel\"><span id=\"explain-trigger-cancel-icon\" aria-hidden=\"true\"></span>\n                                <input id=\"explain-trigger-cancel\" type=\"button\" value=\"Cancel\" aria-hidden=\"true\" disabled\n                                    ?hidden=", "\n                                    @click=", " /></span>\n                        </div>\n                        <div class=\"workbench-action-toolbar__actions\">\n                            <div class=\"workbench-action-toolbar__group\">", "", "</div>\n                        </div>\n                        <div class=\"workbench-action-toolbar__panels workbench-disclosure-track\">\n                            ", "", "\n                        </div>\n                    </div>\n                    </div>\n                </form>\n                <section id=\"query-results\" class=\"query-results\" aria-busy=\"false\" hidden aria-labelledby=\"query-results-heading\">\n                    <div class=\"query-results__header workbench-action-toolbar\">\n                        <div class=\"workbench-action-toolbar__primary\"><h2 id=\"query-results-heading\">Query result</h2></div>\n                        <div class=\"workbench-action-toolbar__actions\">\n                        <button id=\"query-results-fullscreen\" class=\"query-results__fullscreen\" type=\"button\"\n                            aria-label=\"Full screen\" title=\"Full screen\" hidden aria-pressed=\"false\"\n                            data-result-fullscreen-enabled=", "\n                            @click=", ">\n                            <svg class=\"query-results__fullscreen-icon\" viewBox=\"0 0 24 24\" focusable=\"false\" aria-hidden=\"true\">\n                                <path d=\"M4 9V4h5M15 4h5v5M20 15v5h-5M9 20H4v-5\"></path>\n                            </svg><span class=\"query-results__fullscreen-label\">Full screen</span>\n                        </button>\n                        </div>\n                    </div>\n                    <div id=\"query-results-loading\" class=\"query-results__loading\" hidden role=\"status\" aria-live=\"polite\">Loading query results...</div>\n                    <div id=\"query-results-status\" class=\"query-results__status\" role=\"status\" aria-live=\"polite\"></div>\n                    <iframe id=\"query-results-frame\" name=\"query-results-frame\" class=\"query-results__frame\"\n                        title=\"Query results\" hidden></iframe>\n                </section>\n                <div id=\"query-diff-modal\" class=\"query-diff-modal\" aria-hidden=\"true\">\n                    <div class=\"query-diff-modal__dialog\" role=\"dialog\" aria-modal=\"true\" aria-labelledby=\"query-diff-modal-title\">\n                        <div class=\"query-diff-modal__header\"><div id=\"query-diff-modal-title\" class=\"query-diff-modal__title\">Query diff</div>\n                            <button id=\"query-diff-close\" type=\"button\" value=\"Close\"\n                                @click=", ">Close</button></div>\n                        <div class=\"query-diff-modal__body\">\n                            <section class=\"query-diff-section query-diff-section--query\"><div class=\"query-diff-section__title\">Query</div><div id=\"query-diff-query\" class=\"query-diff-view\"></div></section>\n                            <section class=\"query-diff-section query-diff-section--explanation\"><div class=\"query-diff-section__title\">Explanation</div><div id=\"query-diff-explanation\" class=\"query-diff-view\">Comparison is not ready.</div></section>\n                        </div>\n                    </div>\n                </div>\n            </div>"]), queryFeatureEnabled(context, 'editor-fullscreen') ? 'true' : 'false', hideQueryLanguageRow || !queryFeatureEnabled(context, 'query-language'), function () { return invoke('workbench.query.onQlChange'); }, queryFormats.length ? queryFormats.map(function (option) { return h(__makeTemplateObject(["<option value=", "\n                                    ?selected=", ">", "</option>"], ["<option value=", "\n                                    ?selected=", ">", "</option>"]), option.value, option.value === selectedQueryLanguage, option.label); })
                : h(__makeTemplateObject(["<option value=\"", "\" selected>", "</option>"], ["<option value=\"", "\" selected>", "</option>"]), selectedQueryLanguage, selectedQueryLanguage), !queryFeatureEnabled(context, 'query-compare'), !queryFeatureEnabled(context, 'editor-sidebar'), function () { return invoke('workbench.query.toggleCompareSidebar'); }, icon(runtime, 'menu', 'query-sidebar-toggle__svg'), !queryFeatureEnabled(context, 'explain-copy'), !queryFeatureEnabled(context, 'query-swap'), queryFeatureEnabled(context, 'query-refresh') ? 'true' : 'false', !queryFeatureEnabled(context, 'query-refresh') || !queryExplainEnabled(context), function () { return invoke('workbench.query.runCompareExplain'); }, !queryFeatureEnabled(context, 'explain-cancel'), !queryFeatureEnabled(context, 'query-diff'), function () { return invoke('workbench.query.openDiffModal'); }, !queryFeatureEnabled(context, 'query-compare'), queryPane(runtime, { query: query, explanation: explanation, explanationFormat: explanationFormat, explanationLevel: explanationLevel, error: pageValue(model, 'error-message') }, context), queryPane(runtime, { compare: true }, context), !queryFeatureEnabled(context, 'query-execution'), icon(runtime, 'execute'), !queryFeatureEnabled(context, 'query-cancel'), function () { return invoke('workbench.query.cancelQuery'); }, !queryExplainEnabled(context), function () { return invoke('workbench.query.runExplain', null, 'explain-trigger'); }, icon(runtime, 'explain'), !queryFeatureEnabled(context, 'explain-cancel'), !queryFeatureEnabled(context, 'explain-cancel'), function () { return invoke('workbench.query.cancelExplain'); }, saveDisclosure.owner, optionsDisclosure.owner, saveDisclosure.panel, optionsDisclosure.panel, queryFeatureEnabled(context, 'result-fullscreen') ? 'true' : 'false', function () { return invoke('workbench.query.toggleResultsFullscreen'); }, function () { return invoke('workbench.query.closeDiffModal'); });
        }
        function routeBody(model, context, runtime) {
            switch (model.viewId) {
                case 'summary': return summaryPage(runtime, model, context);
                case 'information': return informationPage(runtime, model);
                case 'repositories': return repositoriesPage(runtime, model, context);
                case 'create': return createPage(runtime, model, context);
                case 'delete': return deletePage(runtime, model);
                case 'namespaces': return namespacesPage(runtime, model, context);
                case 'contexts':
                case 'types': return recordBrowsePage(runtime, model, context);
                case 'explore': return explorePage(runtime, model, context);
                case 'query': return runtime.html(__makeTemplateObject(["<div id=\"query-page-content\">", "</div>"], ["<div id=\"query-page-content\">", "</div>"]), queryPage(runtime, model, context));
                case 'saved-queries': return savedQueriesPage(runtime, model, context);
                case 'export': return exportPage(runtime, model, context);
                case 'add': return addPage(runtime, model, context);
                case 'remove': return removePage(runtime, model, context);
                case 'clear': return clearPage(runtime, model, context);
                case 'update': return updatePage(runtime, model, context);
                case 'server': return serverPage(runtime, model, context);
                default: return runtime.html(__makeTemplateObject(["<p class=\"error\" role=\"alert\">Unsupported Workbench view: ", "</p>"], ["<p class=\"error\" role=\"alert\">Unsupported Workbench view: ", "</p>"]), model.viewId);
            }
        }
        /** Return a route template without mutating the DOM (also used by unit tests). */
        function pageTemplate(model, context, runtime) {
            return shell(model, context, runtime, routeBody(model, context, runtime));
        }
        views.pageTemplate = pageTemplate;
        /** Render a complete route into the Workbench mount. */
        function render(mount, model, context, runtime) {
            var regions = rowRegionsByMount.get(mount);
            if (model.rowStore && mount.ownerDocument && mount.ownerDocument.createElement) {
                if (!regions || regions.model !== model) {
                    regions = { model: model, document: mount.ownerDocument, savedCards: {}, groups: {} };
                    rowRegionsByMount.set(mount, regions);
                }
            }
            else {
                rowRegionsByMount.delete(mount);
                regions = null;
            }
            var renderedContext = regions ? __assign(__assign({}, context), { rowRegions: regions }) : context;
            runtime.render(pageTemplate(model, renderedContext, runtime), mount);
            if (regions) {
                if (regions.renderTableRows) {
                    regions.renderTableRows();
                }
                if (regions.renderSavedRows) {
                    regions.renderSavedRows();
                }
                Object.keys(regions.groups).forEach(function (key) { return regions.groups[key].render(); });
            }
            return mount;
        }
        views.render = render;
        /** Keep streamed route rows in the worker store and render only viewport windows. */
        function bindRowWindows(mount, model, context, runtime) {
            var targetWindow = typeof window !== 'undefined' ? window : null;
            var stream = workbench.queryStream;
            var HeightIndex = stream && stream.MeasuredRowHeights;
            var windowSize = model.pickerPageSize || 50;
            var hasQuery = function (selector) { return !!(mount && mount.querySelectorAll
                && mount.querySelectorAll(selector) && mount.querySelectorAll(selector).length); };
            var hasRows = model.rowCount > 0 && model.rowStore
                && hasQuery('[data-workbench-row-table="true"], [data-workbench-row-list="true"]');
            var hasPicker = hasQuery('[data-workbench-window-picker]');
            if (!targetWindow || !model.rowStore || !hasRows && !hasPicker) {
                return Promise.resolve(function () { });
            }
            if (hasRows && typeof HeightIndex !== 'function') {
                return Promise.reject(new Error('Measured Workbench row-window geometry is unavailable'));
            }
            var disposed = false;
            var generation = 0;
            var groupGeneration = 0;
            var heights = hasRows
                ? new HeightIndex(model.rowCount, model.viewId === 'saved-queries' ? 240 : 44) : null;
            var executionDisposers = [];
            var elements = function (selector) {
                if (!mount || !mount.querySelectorAll) {
                    return [];
                }
                var list = mount.querySelectorAll(selector);
                var result = [];
                for (var index = 0; list && index < list.length; index++) {
                    result.push(list[index]);
                }
                return result;
            };
            var disposeExecutionForms = function () {
                executionDisposers.forEach(function (dispose) { return dispose(); });
                executionDisposers = [];
            };
            var bindExecutionForms = function () {
                if (model.viewId !== 'saved-queries' || !stream
                    || typeof stream.bindExecutionForms !== 'function') {
                    return;
                }
                var bound = stream.bindExecutionForms(mount, { workbench: context.workbench });
                executionDisposers = [];
                if (typeof bound === 'function') {
                    executionDisposers.push(bound);
                }
                else if (Array.isArray(bound)) {
                    bound.forEach(function (dispose) {
                        if (typeof dispose === 'function') {
                            executionDisposers.push(dispose);
                        }
                    });
                }
            };
            function bindExploreControls() {
                if (model.viewId !== 'explore') {
                    return;
                }
                elements('[data-workbench-explore-action]').forEach(function (button) {
                    if (button.__rdf4jWorkbenchExploreBound || !button.addEventListener) {
                        return;
                    }
                    var groupKey = button.getAttribute('data-workbench-explore-group');
                    var action = button.getAttribute('data-workbench-explore-action');
                    button.addEventListener('click', function (event) {
                        if (event && event.preventDefault) {
                            event.preventDefault();
                        }
                        var states = exploreGroupStates(model);
                        var state = states[groupKey];
                        var page = model.exploreSummary
                            && model.exploreSummary.groups[groupKey];
                        if (!state || !page || disposed) {
                            return;
                        }
                        if (action === 'previous') {
                            var previous = state.history.pop();
                            if (!previous) {
                                return;
                            }
                            state.cursor = previous.cursor;
                            state.start = previous.start;
                        }
                        else {
                            if (!page.hasNext || !page.nextCursor || !page.items.length) {
                                return;
                            }
                            state.history.push({ cursor: state.cursor, start: state.start });
                            state.cursor = page.nextCursor;
                            state.start += page.items.length;
                        }
                        var activeGeneration = ++groupGeneration;
                        prepareExploreSummary(model).then(function (summary) {
                            if (disposed || activeGeneration !== groupGeneration) {
                                return;
                            }
                            model.exploreSummary = summary;
                            var regions = rowRegionsByMount.get(mount);
                            if (regions) {
                                Object.keys(regions.groups).forEach(function (key) { return regions.groups[key].render(); });
                            }
                            bindExploreControls();
                        });
                    });
                    button.__rdf4jWorkbenchExploreBound = true;
                });
            }
            var renderCurrent = function () {
                render(mount, model, context, runtime);
                bindExecutionForms();
                bindPickerControls();
                bindExploreControls();
            };
            var renderCurrentRows = function () {
                var regions = rowRegionsByMount.get(mount);
                if (regions) {
                    if (regions.renderTableRows) {
                        regions.renderTableRows();
                    }
                    if (regions.renderSavedRows) {
                        regions.renderSavedRows();
                    }
                }
                bindExecutionForms();
            };
            var bindPickerControls = function () {
                elements('[data-workbench-window-action]').forEach(function (button) {
                    if (button.__rdf4jWorkbenchWindowBound || !button.addEventListener) {
                        return;
                    }
                    var action = button.getAttribute('data-workbench-window-action');
                    var click = function (event) {
                        if (event && event.preventDefault) {
                            event.preventDefault();
                        }
                        var delta = action === 'previous' ? -1 : 1;
                        var maximum = Math.max(0, Math.floor((model.rowCount - 1) / windowSize) * windowSize);
                        var start = Math.max(0, Math.min(maximum, (model.pickerStart || 0) + delta * windowSize));
                        if (start === model.pickerStart || disposed) {
                            return;
                        }
                        generation++;
                        model.rowStore.read(start, windowSize).then(function (rows) {
                            if (disposed) {
                                return;
                            }
                            model.pickerStart = start;
                            model.pickerRows = rows;
                            renderCurrent();
                        });
                    };
                    button.addEventListener('click', click);
                    button.__rdf4jWorkbenchWindowBound = true;
                });
            };
            var measureRows = function (start, end) {
                elements('[data-workbench-row-index]').forEach(function (row) {
                    var index = Number(row.getAttribute('data-workbench-row-index'));
                    var rectangle = row.getBoundingClientRect ? row.getBoundingClientRect() : null;
                    if (rectangle && isFinite(index) && index >= start && index < end && rectangle.height > 0) {
                        heights.measure(index, rectangle.height);
                    }
                });
            };
            var refreshRows = function (paint) {
                if (paint === void 0) { paint = true; }
                if (!hasRows || disposed) {
                    return Promise.resolve();
                }
                var activeGeneration = ++generation;
                heights.resize(model.rowCount);
                var targets = elements('[data-workbench-row-table="true"], [data-workbench-row-list="true"]');
                var target = targets.length ? targets[0] : null;
                if (!target) {
                    return Promise.resolve();
                }
                var rectangle = target.getBoundingClientRect ? target.getBoundingClientRect() : { top: 0 };
                var scrollTop = Math.max(0, -Number(rectangle.top || 0));
                var viewportHeight = Math.max(1, Number(targetWindow.innerHeight) || 600);
                var range = heights.range(scrollTop, viewportHeight, 4, 80);
                return model.rowStore.read(range.start, range.end - range.start).then(function (rows) {
                    if (disposed || activeGeneration !== generation) {
                        return;
                    }
                    model.rows = rows;
                    model.rowStart = range.start;
                    model.rowTopSpacer = range.topSpacer;
                    model.rowBottomSpacer = range.bottomSpacer;
                    if (paint) {
                        renderCurrentRows();
                    }
                    measureRows(range.start, range.end);
                    var measuredRange = heights.range(scrollTop, viewportHeight, 4, 80);
                    var topSpacer = heights.offsetOf(measuredRange.start);
                    var bottomSpacer = heights.offsetOf(model.rowCount) - heights.offsetOf(measuredRange.end);
                    var moved = measuredRange.start !== range.start || measuredRange.end !== range.end;
                    var spacerChanged = Math.abs(topSpacer - model.rowTopSpacer) > 0.5
                        || Math.abs(bottomSpacer - model.rowBottomSpacer) > 0.5;
                    if (moved) {
                        return refreshRows(paint);
                    }
                    if (spacerChanged && !disposed) {
                        model.rowTopSpacer = topSpacer;
                        model.rowBottomSpacer = bottomSpacer;
                        if (paint) {
                            renderCurrentRows();
                        }
                    }
                });
            };
            var refresh = function () {
                if (!hasRows) {
                    return Promise.resolve();
                }
                return refreshRows();
            };
            var onScroll = function () { return refresh(); };
            var onResize = function () { return refresh(); };
            var initialPicker = hasPicker && model.rowCount > 0
                ? model.rowStore.read(0, windowSize).then(function (rows) {
                    if (!disposed) {
                        model.pickerRows = rows;
                        model.pickerStart = 0;
                    }
                })
                : Promise.resolve();
            return initialPicker.then(function () { return prepareExploreSummary(model); }).then(function (summary) {
                if (summary) {
                    model.exploreSummary = summary;
                }
                return refreshRows(false);
            }).then(function () {
                renderCurrent();
                return refreshRows();
            }).then(function () {
                bindPickerControls();
                bindExploreControls();
                if (hasRows && targetWindow.addEventListener) {
                    targetWindow.addEventListener('scroll', onScroll, { passive: true });
                    targetWindow.addEventListener('resize', onResize);
                }
                return function () {
                    disposed = true;
                    generation++;
                    groupGeneration++;
                    disposeExecutionForms();
                    var regions = rowRegionsByMount.get(mount);
                    if (regions && regions.model === model) {
                        rowRegionsByMount.delete(mount);
                    }
                    if (hasRows && targetWindow.removeEventListener) {
                        targetWindow.removeEventListener('scroll', onScroll);
                        targetWindow.removeEventListener('resize', onResize);
                    }
                };
            });
        }
        views.bindRowWindows = bindRowWindows;
    })(views = workbench.views || (workbench.views = {}));
})(workbench || (workbench = {}));
//# sourceMappingURL=workbenchViews.js.map