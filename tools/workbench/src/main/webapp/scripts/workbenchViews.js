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
        menu: 'M4 6h16M4 12h16M4 18h16',
        user: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm-7 8a7 7 0 0 1 14 0',
        search: 'M10.5 4a6.5 6.5 0 1 0 0 13 6.5 6.5 0 0 0 0-13ZM16 16l5 5'
    };
    function actionIconPath(name) {
        return actionIconCatalog[name] || actionIconCatalog.add;
    }
    var calloutIcons = {
        info: 'M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16Zm0 7v5m0-8h.01',
        warning: 'M12 4 2.8 20h18.4L12 4Zm0 6v4.5m0 2.5h.01',
        error: 'M12 3.5a8.5 8.5 0 1 0 0 17 8.5 8.5 0 0 0 0-17ZM12 8v5m0 3h.01'
    };
    function calloutIconPath(kind) {
        return calloutIcons[kind] || calloutIcons.info;
    }
    /** DOM version of the callout component for code that does not render with Lit. */
    function createCallout(document, kind, body, title) {
        var namespace = 'http://www.w3.org/2000/svg';
        var element = document.createElement('div');
        element.className = 'workbench-callout workbench-callout--' + (calloutIcons[kind] ? kind : 'info');
        element.setAttribute('role', kind === 'error' ? 'alert' : 'note');
        if (document.createElementNS) {
            var svg = document.createElementNS(namespace, 'svg');
            svg.setAttribute('class', 'workbench-callout__icon');
            svg.setAttribute('viewBox', '0 0 24 24');
            svg.setAttribute('width', '18');
            svg.setAttribute('height', '18');
            svg.setAttribute('focusable', 'false');
            svg.setAttribute('aria-hidden', 'true');
            var path = document.createElementNS(namespace, 'path');
            path.setAttribute('d', calloutIconPath(kind));
            svg.appendChild(path);
            element.appendChild(svg);
        }
        var content = document.createElement('div');
        content.className = 'workbench-callout__body';
        if (title) {
            var strong = document.createElement('strong');
            strong.className = 'workbench-callout__title';
            strong.textContent = title;
            content.appendChild(strong);
            content.appendChild(document.createTextNode(' '));
        }
        content.appendChild(document.createTextNode(body));
        element.appendChild(content);
        return element;
    }
    workbench.createCallout = createCallout;
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
        /** Formats integer counts with workbench.format.count (template.ts) when it is loaded. */
        function formatCount(value, context) {
            var format = workbench.format;
            var raw = text(value);
            return format && typeof format.count === 'function' ? format.count(raw, context.locale) : raw;
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
            // The menu always comes from the policy-filtered Info model; never invent one.
            if (typeof console !== 'undefined' && console.error) {
                console.error('Workbench menu is unavailable');
            }
            return [];
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
        /** Groups with more items than this keep a disclosure so long custom menus stay manageable. */
        var navigationDisclosureThreshold = 12;
        function navigationItem(runtime, context, info, item, groupLabel, active) {
            var h = runtime.html;
            var id = text(item.id || item['menu-item-id']);
            var label = text(item.label || item['menu-item-label'] || id);
            var href = item.href || item['menu-item-href'] || urlFor(context, id);
            return h(__makeTemplateObject(["<li class=", ">\n                ", "\n            </li>"], ["<li class=", ">\n                ", "\n            </li>"]), active === id ? 'current' : '', isDisabled(item, info)
                ? h(__makeTemplateObject(["<span class=\"disabled\" title=", ">", "", "</span>"], ["<span class=\"disabled\" title=", ">", "", "</span>"]), groupLabel, icon(runtime, item.icon || item['menu-item-icon'] || id), label) : h(__makeTemplateObject(["<a href=", " data-workbench-nav-href=", "\n                        title=", " aria-current=", ">\n                        ", "", "\n                    </a>"], ["<a href=", " data-workbench-nav-href=", "\n                        title=", " aria-current=", ">\n                        ", "", "\n                    </a>"]), href, href, groupLabel, active === id ? 'page' : 'false', icon(runtime, item.icon || item['menu-item-icon'] || id), label));
        }
        /** Every group renders the same way: a non-interactive label followed by its items (M2.4). */
        function navigation(context, active, runtime) {
            var h = runtime.html;
            var info = normalizeWorkbench(context.workbench, context.linked && context.linked.info);
            return menuEntries(context).filter(function (group) { return (group.items || []).length > 0; }).map(function (group) {
                var items = group.items || [];
                var groupId = text(group.id || group['menu-group-id'] || 'workbench');
                var groupLabel = text(group.label || group['menu-group-label'] || 'Workbench');
                var list = h(__makeTemplateObject(["<ul id=", " class=\"group\" aria-labelledby=", ">\n                    ", "\n                </ul>"], ["<ul id=", " class=\"group\" aria-labelledby=", ">\n                    ", "\n                </ul>"]), 'workbench-nav-items-' + groupId, 'workbench-nav-label-' + groupId, items.map(function (item) { return navigationItem(runtime, context, info, item, groupLabel, active); }));
                if (items.length > navigationDisclosureThreshold) {
                    var containsActive = items.some(function (item) { return text(item.id || item['menu-item-id']) === active; });
                    return h(__makeTemplateObject(["<li class=\"workbench-nav-group workbench-nav-group--long\" data-workbench-menu-group=", "\n                            data-workbench-menu-label=", ">\n                        <details class=\"workbench-nav-group__disclosure\" ?open=", ">\n                            <summary id=", " aria-controls=", "\n                                    class=\"workbench-nav-group__summary\">\n                                <span id=", " class=\"workbench-nav-group__label\">", "</span>\n                                ", "\n                            </summary>\n                            ", "\n                        </details>\n                    </li>"], ["<li class=\"workbench-nav-group workbench-nav-group--long\" data-workbench-menu-group=", "\n                            data-workbench-menu-label=", ">\n                        <details class=\"workbench-nav-group__disclosure\" ?open=", ">\n                            <summary id=", " aria-controls=", "\n                                    class=\"workbench-nav-group__summary\">\n                                <span id=", " class=\"workbench-nav-group__label\">", "</span>\n                                ", "\n                            </summary>\n                            ", "\n                        </details>\n                    </li>"]), groupId, groupLabel, containsActive, 'workbench-nav-summary-' + groupId, 'workbench-nav-items-' + groupId, 'workbench-nav-label-' + groupId, groupLabel, icon(runtime, 'chevron', 'workbench-nav-group__chevron workbench-disclosure-chevron'), list);
                }
                return h(__makeTemplateObject(["<li class=\"workbench-nav-group\" data-workbench-menu-group=", "\n                        data-workbench-menu-label=", ">\n                    <span id=", " class=\"workbench-nav-group__label\">", "</span>\n                    ", "\n                </li>"], ["<li class=\"workbench-nav-group\" data-workbench-menu-group=", "\n                        data-workbench-menu-label=", ">\n                    <span id=", " class=\"workbench-nav-group__label\">", "</span>\n                    ", "\n                </li>"]), groupId, groupLabel, 'workbench-nav-label-' + groupId, groupLabel, list);
            });
        }
        /** Decodes the user name from the server-user-password cookie ("user:password" in base64). */
        function serverUser() {
            if (typeof document === 'undefined') {
                return '';
            }
            var encoded = currentCookieValue('server-user-password');
            if (!encoded) {
                return '';
            }
            var decoded = encoded;
            try {
                decoded = typeof window !== 'undefined' && typeof window.atob === 'function' ? window.atob(encoded) : encoded;
            }
            catch (error) {
                return '';
            }
            var user = decoded.indexOf(':') >= 0 ? decoded.substring(0, decoded.indexOf(':')) : decoded;
            return user === '""' ? '' : user;
        }
        function hostAndPort(server) {
            try {
                return new URL(server).host || server;
            }
            catch (error) {
                return server;
            }
        }
        /** Shell state for the context bar: server, repository and user. */
        function contextBarState(context) {
            var info = normalizeWorkbench(context.workbench, context.linked && context.linked.info);
            var contextRepository = context.repositoryId && context.repositoryId !== 'NONE' ? context.repositoryId : '';
            var repositoryId = text(info.id || contextRepository);
            return {
                server: text(info.server || info.location || ''),
                repositoryId: repositoryId === 'NONE' ? '' : repositoryId,
                repositoryTitle: text(info.description || info.title),
                user: serverUser()
            };
        }
        views.contextBarState = contextBarState;
        function switcherChevron(runtime) {
            return icon(runtime, 'chevron', 'workbench-switcher__chevron');
        }
        /** The 56px context bar (mockups 01 and 04): brand, server, repository and user switchers. */
        function contextBar(context, active, runtime) {
            var h = runtime.html;
            var state = contextBarState(context);
            var server = state.server;
            return h(__makeTemplateObject(["<header id=\"workbench-contextbar\" class=\"workbench-contextbar\">\n                <a id=\"logo\" class=\"workbench-brand\" href=", " aria-label=\"RDF4J Workbench home\">\n                    <img src=", " alt=\"rdf4j\" />\n                    <img class=\"product\" src=", " alt=\"workbench\" />\n                </a>\n                <div class=\"workbench-switcher\" data-workbench-switcher=\"server\">\n                    <button id=\"workbench-server-switcher\" class=\"workbench-switcher__button\" type=\"button\"\n                            aria-haspopup=\"dialog\" aria-expanded=\"false\" aria-controls=\"workbench-server-popover\"\n                            title=", ">\n                        ", "<span class=\"workbench-switcher__key\">Server</span>\n                        <span class=\"workbench-switcher__value\">", "</span>\n                        ", "\n                    </button>\n                    <div id=\"workbench-server-popover\" class=\"workbench-popover\" role=\"dialog\" aria-label=\"Server\" hidden>\n                        <dl class=\"workbench-kv\">\n                            <div class=\"workbench-kv__row\"><dt>Server</dt><dd class=\"workbench-kv__code\">", "</dd></div>\n                            <div class=\"workbench-kv__row\"><dt>User</dt><dd>", "</dd></div>\n                        </dl>\n                        <div class=\"workbench-popover__footer\">\n                            <a href=", ">", "Change server or user\u2026</a>\n                        </div>\n                    </div>\n                </div>\n                <span class=\"workbench-contextbar__divider\" aria-hidden=\"true\"></span>\n                <div class=\"workbench-switcher\" data-workbench-switcher=\"repository\">\n                    <button id=\"workbench-repository-switcher\" class=\"workbench-switcher__button\" type=\"button\"\n                            aria-haspopup=\"dialog\" aria-expanded=\"false\" aria-controls=\"workbench-repository-popover\">\n                        ", "<span class=\"workbench-switcher__key\">Repository</span>\n                        ", "\n                        ", "\n                    </button>\n                    <div id=\"workbench-repository-popover\" class=\"workbench-popover workbench-popover--repositories\"\n                            role=\"dialog\" aria-label=\"Choose repository\" hidden\n                            data-workbench-active-view=", " data-workbench-repositories-url=", "\n                            data-workbench-base-path=", ">\n                        <div class=\"workbench-popover__search\">", "\n                            <input id=\"workbench-repository-filter\" type=\"search\" placeholder=\"Find repository\"\n                                aria-label=\"Find repository\" autocomplete=\"off\" aria-controls=\"workbench-repository-options\" />\n                        </div>\n                        <ul id=\"workbench-repository-options\" class=\"workbench-popover__list\" aria-label=\"Repositories\"></ul>\n                        <p class=\"workbench-popover__status\" role=\"status\" aria-live=\"polite\"></p>\n                        <div class=\"workbench-popover__footer\">\n                            <a href=", ">", "All repositories</a>\n                            <a href=", ">", "Create repository</a>\n                        </div>\n                    </div>\n                </div>\n                <span class=\"workbench-contextbar__spacer\"></span>\n                <div class=\"workbench-switcher\" data-workbench-switcher=\"user\">\n                    <button id=\"workbench-user-switcher\" class=\"workbench-switcher__button workbench-switcher__button--user\"\n                            type=\"button\" aria-haspopup=\"dialog\" aria-expanded=\"false\" aria-controls=\"workbench-user-popover\">\n                        ", "<span id=\"selected-user\" class=\"workbench-switcher__value\">", "</span>\n                        ", "\n                    </button>\n                    <div id=\"workbench-user-popover\" class=\"workbench-popover workbench-popover--end\" role=\"dialog\" aria-label=\"User\" hidden>\n                        <p class=\"workbench-popover__text\">", "</p>\n                        <div class=\"workbench-popover__footer\">\n                            <a href=", ">", "Change server or user\u2026</a>\n                        </div>\n                    </div>\n                </div>\n            </header>"], ["<header id=\"workbench-contextbar\" class=\"workbench-contextbar\">\n                <a id=\"logo\" class=\"workbench-brand\" href=", " aria-label=\"RDF4J Workbench home\">\n                    <img src=", " alt=\"rdf4j\" />\n                    <img class=\"product\" src=", " alt=\"workbench\" />\n                </a>\n                <div class=\"workbench-switcher\" data-workbench-switcher=\"server\">\n                    <button id=\"workbench-server-switcher\" class=\"workbench-switcher__button\" type=\"button\"\n                            aria-haspopup=\"dialog\" aria-expanded=\"false\" aria-controls=\"workbench-server-popover\"\n                            title=", ">\n                        ", "<span class=\"workbench-switcher__key\">Server</span>\n                        <span class=\"workbench-switcher__value\">", "</span>\n                        ", "\n                    </button>\n                    <div id=\"workbench-server-popover\" class=\"workbench-popover\" role=\"dialog\" aria-label=\"Server\" hidden>\n                        <dl class=\"workbench-kv\">\n                            <div class=\"workbench-kv__row\"><dt>Server</dt><dd class=\"workbench-kv__code\">", "</dd></div>\n                            <div class=\"workbench-kv__row\"><dt>User</dt><dd>", "</dd></div>\n                        </dl>\n                        <div class=\"workbench-popover__footer\">\n                            <a href=", ">", "Change server or user\u2026</a>\n                        </div>\n                    </div>\n                </div>\n                <span class=\"workbench-contextbar__divider\" aria-hidden=\"true\"></span>\n                <div class=\"workbench-switcher\" data-workbench-switcher=\"repository\">\n                    <button id=\"workbench-repository-switcher\" class=\"workbench-switcher__button\" type=\"button\"\n                            aria-haspopup=\"dialog\" aria-expanded=\"false\" aria-controls=\"workbench-repository-popover\">\n                        ", "<span class=\"workbench-switcher__key\">Repository</span>\n                        ", "\n                        ", "\n                    </button>\n                    <div id=\"workbench-repository-popover\" class=\"workbench-popover workbench-popover--repositories\"\n                            role=\"dialog\" aria-label=\"Choose repository\" hidden\n                            data-workbench-active-view=", " data-workbench-repositories-url=", "\n                            data-workbench-base-path=", ">\n                        <div class=\"workbench-popover__search\">", "\n                            <input id=\"workbench-repository-filter\" type=\"search\" placeholder=\"Find repository\"\n                                aria-label=\"Find repository\" autocomplete=\"off\" aria-controls=\"workbench-repository-options\" />\n                        </div>\n                        <ul id=\"workbench-repository-options\" class=\"workbench-popover__list\" aria-label=\"Repositories\"></ul>\n                        <p class=\"workbench-popover__status\" role=\"status\" aria-live=\"polite\"></p>\n                        <div class=\"workbench-popover__footer\">\n                            <a href=", ">", "All repositories</a>\n                            <a href=", ">", "Create repository</a>\n                        </div>\n                    </div>\n                </div>\n                <span class=\"workbench-contextbar__spacer\"></span>\n                <div class=\"workbench-switcher\" data-workbench-switcher=\"user\">\n                    <button id=\"workbench-user-switcher\" class=\"workbench-switcher__button workbench-switcher__button--user\"\n                            type=\"button\" aria-haspopup=\"dialog\" aria-expanded=\"false\" aria-controls=\"workbench-user-popover\">\n                        ", "<span id=\"selected-user\" class=\"workbench-switcher__value\">", "</span>\n                        ", "\n                    </button>\n                    <div id=\"workbench-user-popover\" class=\"workbench-popover workbench-popover--end\" role=\"dialog\" aria-label=\"User\" hidden>\n                        <p class=\"workbench-popover__text\">", "</p>\n                        <div class=\"workbench-popover__footer\">\n                            <a href=", ">", "Change server or user\u2026</a>\n                        </div>\n                    </div>\n                </div>\n            </header>"]), urlFor(context, 'repositories'), context.basePath + '/images/logo.png', context.basePath + '/images/product.png', server || 'No server', icon(runtime, 'server'), server ? hostAndPort(server) : 'None', switcherChevron(runtime), server || 'None', state.user || 'Not signed in', urlFor(context, 'server'), icon(runtime, 'settings'), icon(runtime, 'repository'), state.repositoryId
                ? h(__makeTemplateObject(["<span class=\"workbench-switcher__value\"><span class=\"workbench-switcher__id\">", "</span>", "</span>"], ["<span class=\"workbench-switcher__value\"><span class=\"workbench-switcher__id\">", "</span>", "</span>"]), state.repositoryId, state.repositoryTitle
                    ? h(__makeTemplateObject(["<span class=\"workbench-switcher__title\">", "</span>"], ["<span class=\"workbench-switcher__title\">", "</span>"]), state.repositoryTitle) : '') : h(__makeTemplateObject(["<span class=\"workbench-switcher__value workbench-switcher__value--empty\">No repository</span>"], ["<span class=\"workbench-switcher__value workbench-switcher__value--empty\">No repository</span>"])), switcherChevron(runtime), active, urlFor(context, 'repositories'), (context.basePath || '').replace(/\/+$/, ''), icon(runtime, 'search'), urlFor(context, 'repositories'), icon(runtime, 'repository'), urlFor(context, 'create'), icon(runtime, 'create'), icon(runtime, 'user'), state.user || 'Not signed in', switcherChevron(runtime), state.user ? h(__makeTemplateObject(["Signed in as <strong>", "</strong>"], ["Signed in as <strong>", "</strong>"]), state.user) : 'Not signed in', urlFor(context, 'server'), icon(runtime, 'settings'));
        }
        /** Persistent shell around the outlet; `outlet` is the outlet node or, without a DOM, its template. */
        function shellTemplate(state, runtime, outlet) {
            var h = runtime.html;
            var context = state.context;
            return h(__makeTemplateObject(["", "\n            <details id=\"workbench-navigation-disclosure\" class=\"workbench-navigation-disclosure\" open>\n                <summary id=\"workbench-navigation-summary\">\n                    <svg class=\"workbench-menu-icon\" viewBox=\"0 0 24 24\" width=\"18\" height=\"18\" focusable=\"false\" aria-hidden=\"true\">\n                        <path d=\"M4 6h16M4 12h16M4 18h16\"></path>\n                    </svg><span>Menu</span>\n                    <svg class=\"workbench-menu-chevron workbench-disclosure-chevron\" viewBox=\"0 0 24 24\" width=\"18\" height=\"18\" focusable=\"false\" aria-hidden=\"true\">\n                        <path d=\"m6 9 6 6 6-6\"></path>\n                    </svg>\n                </summary>\n                <div id=\"navigation\" class=\"workbench-nav\"><ul class=\"maingroup\">\n                    ", "\n                </ul></div>\n            </details>\n            <main id=\"content\" class=\"workbench-main\">", "</main>\n            <div id=\"footer\" class=\"workbench-footer\"><div>Copyright \u00A9 Eclipse RDF4J contributors</div></div>"], ["", "\n            <details id=\"workbench-navigation-disclosure\" class=\"workbench-navigation-disclosure\" open>\n                <summary id=\"workbench-navigation-summary\">\n                    <svg class=\"workbench-menu-icon\" viewBox=\"0 0 24 24\" width=\"18\" height=\"18\" focusable=\"false\" aria-hidden=\"true\">\n                        <path d=\"M4 6h16M4 12h16M4 18h16\"></path>\n                    </svg><span>Menu</span>\n                    <svg class=\"workbench-menu-chevron workbench-disclosure-chevron\" viewBox=\"0 0 24 24\" width=\"18\" height=\"18\" focusable=\"false\" aria-hidden=\"true\">\n                        <path d=\"m6 9 6 6 6-6\"></path>\n                    </svg>\n                </summary>\n                <div id=\"navigation\" class=\"workbench-nav\"><ul class=\"maingroup\">\n                    ", "\n                </ul></div>\n            </details>\n            <main id=\"content\" class=\"workbench-main\">", "</main>\n            <div id=\"footer\" class=\"workbench-footer\"><div>Copyright \u00A9 Eclipse RDF4J contributors</div></div>"]), contextBar(context, state.viewId, runtime), navigation(context, state.viewId, runtime), outlet);
        }
        /** The page area that changes from route to route: title, noscript notice and page surface. */
        function outletContentTemplate(model, runtime, body) {
            var h = runtime.html;
            return h(__makeTemplateObject(["<h1 id=\"title_heading\">", "</h1>\n                <p id=\"noscript-message\" class=\"ERROR\">Scripting is not enabled. The RDF4J Workbench application requires scripting to be enabled.</p>\n                <div id=\"workbench-page-surface\" class=\"workbench-page-surface\">", "</div>"], ["<h1 id=\"title_heading\">", "</h1>\n                <p id=\"noscript-message\" class=\"ERROR\">Scripting is not enabled. The RDF4J Workbench application requires scripting to be enabled.</p>\n                <div id=\"workbench-page-surface\" class=\"workbench-page-surface\">", "</div>"]), routeTitle(model), body);
        }
        function shell(model, context, runtime, body) {
            var h = runtime.html;
            return shellTemplate({ viewId: model.viewId, context: context }, runtime, h(__makeTemplateObject(["<div id=\"workbench-outlet\" class=\"workbench-outlet\" tabindex=\"-1\">", "</div>"], ["<div id=\"workbench-outlet\" class=\"workbench-outlet\" tabindex=\"-1\">", "</div>"]), outletContentTemplate(model, runtime, body)));
        }
        function workbenchData(context) {
            return normalizeWorkbench(context.workbench, context.linked && context.linked.info);
        }
        /** People-facing column label for a page table; options.labels overrides the default. */
        function columnLabel(name, options) {
            var labels = options && options.labels;
            if (labels && typeof labels[name] === 'string') {
                return labels[name];
            }
            return name ? name.charAt(0).toUpperCase() + name.substring(1) : name;
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
            return h(__makeTemplateObject(["<table class=\"data\" data-workbench-row-table=", ">\n                ", "\n                ", "\n            </table>"], ["<table class=\"data\" data-workbench-row-table=", ">\n                ", "\n                ", "\n            </table>"]), model.rowStore && total ? 'true' : runtime.nothing, columns.length ? h(__makeTemplateObject(["<thead><tr>", "</tr></thead>"], ["<thead><tr>", "</tr></thead>"]), columns.map(function (name) { return h(__makeTemplateObject(["<th scope=\"col\">", "</th>"], ["<th scope=\"col\">", "</th>"]), columnLabel(name, options)); })) : '', regions ? regions.tableBody
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
        /** Shared key/value list (mockup 11): label column and value column, stacked below 600px. */
        function keyValueList(runtime, rows) {
            var h = runtime.html;
            return h(__makeTemplateObject(["<dl class=\"workbench-kv\">", "</dl>"], ["<dl class=\"workbench-kv\">", "</dl>"]), rows.map(function (row) { return h(__makeTemplateObject(["<div class=\"workbench-kv__row\"><dt>", "</dt><dd>", "</dd></div>"], ["<div class=\"workbench-kv__row\"><dt>", "</dt><dd>", "</dd></div>"]), row[0], row[1] && typeof row[1] === 'object' && Array.isArray(row[1].strings) ? row[1] : text(row[1])); }));
        }
        /** Callout (mockup 11): info and warning use role="note", errors use role="alert". */
        function callout(runtime, kind, body, title, id) {
            var h = runtime.html;
            var content = h(__makeTemplateObject(["<svg class=\"workbench-callout__icon\" viewBox=\"0 0 24 24\" width=\"18\" height=\"18\"\n                    focusable=\"false\" aria-hidden=\"true\"><path d=", "></path></svg>\n                <div class=\"workbench-callout__body\">", "", "</div>"], ["<svg class=\"workbench-callout__icon\" viewBox=\"0 0 24 24\" width=\"18\" height=\"18\"\n                    focusable=\"false\" aria-hidden=\"true\"><path d=", "></path></svg>\n                <div class=\"workbench-callout__body\">", "", "</div>"]), calloutIconPath(kind), title
                ? h(__makeTemplateObject(["<strong class=\"workbench-callout__title\">", "</strong> "], ["<strong class=\"workbench-callout__title\">", "</strong> "]), title) : '', body);
            if (kind === 'error') {
                return id
                    ? h(__makeTemplateObject(["<div id=\"", "\" class=\"workbench-callout workbench-callout--error\" role=\"alert\">", "</div>"], ["<div id=\"", "\" class=\"workbench-callout workbench-callout--error\" role=\"alert\">", "</div>"]), id, content) : h(__makeTemplateObject(["<div class=\"workbench-callout workbench-callout--error\" role=\"alert\">", "</div>"], ["<div class=\"workbench-callout workbench-callout--error\" role=\"alert\">", "</div>"]), content);
            }
            if (kind === 'warning') {
                return id
                    ? h(__makeTemplateObject(["<div id=\"", "\" class=\"workbench-callout workbench-callout--warning\" role=\"note\">", "</div>"], ["<div id=\"", "\" class=\"workbench-callout workbench-callout--warning\" role=\"note\">", "</div>"]), id, content) : h(__makeTemplateObject(["<div class=\"workbench-callout workbench-callout--warning\" role=\"note\">", "</div>"], ["<div class=\"workbench-callout workbench-callout--warning\" role=\"note\">", "</div>"]), content);
            }
            return id
                ? h(__makeTemplateObject(["<div id=\"", "\" class=\"workbench-callout workbench-callout--info\" role=\"note\">", "</div>"], ["<div id=\"", "\" class=\"workbench-callout workbench-callout--info\" role=\"note\">", "</div>"]), id, content) : h(__makeTemplateObject(["<div class=\"workbench-callout workbench-callout--info\" role=\"note\">", "</div>"], ["<div class=\"workbench-callout workbench-callout--info\" role=\"note\">", "</div>"]), content);
        }
        function errorCallout(runtime, model) {
            var message = text(pageValue(model, 'error-message'));
            return message ? callout(runtime, 'error', message) : '';
        }
        function systemRepositoryCallout(runtime, context) {
            return context.repositoryId === 'SYSTEM'
                ? callout(runtime, 'warning', 'The SYSTEM repository is intended for system use.') : '';
        }
        function simpleSection(runtime, title, rows, className, id, island) {
            if (island === void 0) { island = true; }
            var h = runtime.html;
            var content = h(__makeTemplateObject(["<h2>", "</h2>", ""], ["<h2>", "</h2>", ""]), title, keyValueList(runtime, rows));
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
                ['Repository size', formatCount(field(row, 'size'), context)],
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
            return h(__makeTemplateObject(["<form id=\"create-type-form\" action=\"create\" method=\"get\" class=\"workbench-island workbench-form-card\">\n                <div class=\"workbench-field\"><label for=\"type\">Repository type</label>\n                    <select id=\"type\" name=\"type\">\n                        ", "\n                    </select>\n                </div>\n                <div class=\"workbench-form-actions\">\n                    <span class=\"workbench-action workbench-action--primary\"><label class=\"workbench-action-hit-area\">\n                        ", "<span class=\"workbench-action-label\"><input type=\"submit\" name=\"next\" value=\"Next\" /></span>\n                    </label></span>\n                </div>\n            </form>"], ["<form id=\"create-type-form\" action=\"create\" method=\"get\" class=\"workbench-island workbench-form-card\">\n                <div class=\"workbench-field\"><label for=\"type\">Repository type</label>\n                    <select id=\"type\" name=\"type\">\n                        ", "\n                    </select>\n                </div>\n                <div class=\"workbench-form-actions\">\n                    <span class=\"workbench-action workbench-action--primary\"><label class=\"workbench-action-hit-area\">\n                        ", "<span class=\"workbench-action-label\"><input type=\"submit\" name=\"next\" value=\"Next\" /></span>\n                    </label></span>\n                </div>\n            </form>"]), rows.map(function (row) { return h(__makeTemplateObject(["<option value=", ">", "</option>"], ["<option value=", ">", "</option>"]), text(row.type), text(row.label)); }), icon(runtime, 'next'));
        }
        function federateForm(runtime, rows, context) {
            var h = runtime.html;
            return h(__makeTemplateObject(["<form action=\"create\" method=\"post\" class=\"workbench-form-card\">\n                <table class=\"dataentry\" data-advanced-label=\"Advanced settings\"><tbody>\n                    <tr><th><label for=\"type\">Repository type</label></th><td><select id=\"type\" name=\"type\"><option value=\"federate\">Federation Store</option></select></td></tr>\n                    <tr><th><label for=\"id\">Repository ID</label></th><td><input id=\"id\" name=\"Local repository ID\" type=\"text\" value=\"fed\" data-field-role=\"repository-id\" /></td><td><span id=\"recurse-message\" class=\"error\" hidden>Federation ID may not match an existing ID.</span></td></tr>\n                    <tr><th><label for=\"title\">Repository title</label></th><td><input id=\"title\" name=\"Repository title\" type=\"text\" value=\"Federation\" data-field-role=\"repository-title\" /></td></tr>\n                    <tr data-field-role=\"federation-member\"><th>Federation members</th><td><div class=\"workbench-choice-list\">\n                        ", "\n                    </div></td><td><span class=\"error\" id=\"create-feedback\">Select at least two federation members.</span></td></tr>\n                </tbody></table>\n                ", "\n            </form>"], ["<form action=\"create\" method=\"post\" class=\"workbench-form-card\">\n                <table class=\"dataentry\" data-advanced-label=\"Advanced settings\"><tbody>\n                    <tr><th><label for=\"type\">Repository type</label></th><td><select id=\"type\" name=\"type\"><option value=\"federate\">Federation Store</option></select></td></tr>\n                    <tr><th><label for=\"id\">Repository ID</label></th><td><input id=\"id\" name=\"Local repository ID\" type=\"text\" value=\"fed\" data-field-role=\"repository-id\" /></td><td><span id=\"recurse-message\" class=\"error\" hidden>Federation ID may not match an existing ID.</span></td></tr>\n                    <tr><th><label for=\"title\">Repository title</label></th><td><input id=\"title\" name=\"Repository title\" type=\"text\" value=\"Federation\" data-field-role=\"repository-title\" /></td></tr>\n                    <tr data-field-role=\"federation-member\"><th>Federation members</th><td><div class=\"workbench-choice-list\">\n                        ", "\n                    </div></td><td><span class=\"error\" id=\"create-feedback\">Select at least two federation members.</span></td></tr>\n                </tbody></table>\n                ", "\n            </form>"]), rows.filter(function (row) { return text(row.id) !== 'SYSTEM'; }).map(function (row) { return h(__makeTemplateObject(["<label class=\"workbench-choice\">\n                            <input type=\"checkbox\" class=\"memberID\" name=\"memberID\" value=", " data-field-role=\"federation-member\" />\n                            <span>", "", "</span>\n                        </label>"], ["<label class=\"workbench-choice\">\n                            <input type=\"checkbox\" class=\"memberID\" name=\"memberID\" value=", " data-field-role=\"federation-member\" />\n                            <span>", "", "</span>\n                        </label>"]), text(row.id), text(row.id), text(row.description) ? ' — ' + text(row.description) : ''); }), createActions(runtime, false, context));
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
            return h(__makeTemplateObject(["<form action=\"create\" method=\"post\" class=\"workbench-form-card\">\n                <table class=\"dataentry\" data-advanced-label=\"Advanced settings\"><tbody>\n                    <tr><th><label for=\"type\">Repository type</label></th><td><select id=\"type\" name=\"type\">\n                        <option value=", ">", "</option>\n                    </select></td></tr>\n                    ", "\n                </tbody></table>\n                ", "\n            </form>"], ["<form action=\"create\" method=\"post\" class=\"workbench-form-card\">\n                <table class=\"dataentry\" data-advanced-label=\"Advanced settings\"><tbody>\n                    <tr><th><label for=\"type\">Repository type</label></th><td><select id=\"type\" name=\"type\">\n                        <option value=", ">", "</option>\n                    </select></td></tr>\n                    ", "\n                </tbody></table>\n                ", "\n            </form>"]), text(first.templateType), text(first.templateLabel), fieldRows, createActions(runtime, true, context));
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
            return h(__makeTemplateObject(["<div class=\"workbench-form-actions workbench-window-controls\" role=\"group\" aria-label=", ">\n                <button type=\"button\" class=\"workbench-action workbench-action--secondary\" data-workbench-window-picker=", " data-workbench-window-action=\"previous\"\n                    ?disabled=", ">Previous</button>\n                <span role=\"status\">Showing ", "\u2013", " of ", " ", "</span>\n                <button type=\"button\" class=\"workbench-action workbench-action--secondary\" data-workbench-window-picker=", " data-workbench-window-action=\"next\"\n                    ?disabled=", ">Next</button>\n            </div>"], ["<div class=\"workbench-form-actions workbench-window-controls\" role=\"group\" aria-label=", ">\n                <button type=\"button\" class=\"workbench-action workbench-action--secondary\" data-workbench-window-picker=", " data-workbench-window-action=\"previous\"\n                    ?disabled=", ">Previous</button>\n                <span role=\"status\">Showing ", "\u2013", " of ", " ", "</span>\n                <button type=\"button\" class=\"workbench-action workbench-action--secondary\" data-workbench-window-picker=", " data-workbench-window-action=\"next\"\n                    ?disabled=", ">Next</button>\n            </div>"]), label + ' pages', name, start === 0, start + 1, end, total, label, name, end >= total);
        }
        function deletePage(runtime, model) {
            var h = runtime.html;
            var options = recordsFromRows(model, model.pickerRows || []).filter(function (row) { return text(row.id) !== 'SYSTEM'; });
            var selectedId = text(model.metadata.selectedRepositoryId || pageValue(model, 'id'));
            var selectedRepository = model.metadata.selectedRepository;
            var selectedVisible = options.some(function (row) { return text(row.id) === selectedId; });
            return h(__makeTemplateObject(["<form id=\"delete-form\" class=\"workbench-island workbench-form-card\" action=\"delete\" method=\"post\"\n                    @submit=", ">\n                <div class=\"workbench-field\"><label for=\"id\">Repository</label>\n                    <select id=\"id\" name=\"id\" data-workbench-window-picker=\"repositories\" @change=", "><option value=\"\" ?selected=", "></option>\n                        ", "\n                        ", "\n                    </select>\n                    ", "\n                </div>\n                <div id=\"delete-actions\" class=\"workbench-form-actions\"><span class=\"workbench-action workbench-action--danger-outline\">\n                    <label class=\"workbench-action-hit-area\">", "\n                        <span class=\"workbench-action-label\"><input type=\"submit\" value=\"Delete\" /></span>\n                    </label></span>\n                </div><span id=\"delete-feedback\" class=\"error\" role=\"alert\"></span>\n            </form>"], ["<form id=\"delete-form\" class=\"workbench-island workbench-form-card\" action=\"delete\" method=\"post\"\n                    @submit=", ">\n                <div class=\"workbench-field\"><label for=\"id\">Repository</label>\n                    <select id=\"id\" name=\"id\" data-workbench-window-picker=\"repositories\" @change=", "><option value=\"\" ?selected=", "></option>\n                        ", "\n                        ", "\n                    </select>\n                    ", "\n                </div>\n                <div id=\"delete-actions\" class=\"workbench-form-actions\"><span class=\"workbench-action workbench-action--danger-outline\">\n                    <label class=\"workbench-action-hit-area\">", "\n                        <span class=\"workbench-action-label\"><input type=\"submit\" value=\"Delete\" /></span>\n                    </label></span>\n                </div><span id=\"delete-feedback\" class=\"error\" role=\"alert\"></span>\n            </form>"]), function (event) {
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
            return h(__makeTemplateObject(["", "\n                <form id=\"namespaces-form\" class=\"workbench-island\" action=\"namespaces\" method=\"post\">\n                    <div class=\"workbench-form-grid\">\n                        <div class=\"workbench-field\"><label for=\"prefix\">Prefix</label>\n                            <div class=\"workbench-inline-controls\">\n                                <input type=\"text\" id=\"prefix\" name=\"prefix\" size=\"8\" value=", " />\n                                <select id=\"prefix-select\" data-workbench-window-picker=\"namespaces\" @change=", ">\n                                    <option value=\"\" ?selected=", "></option>\n                                    ", "\n                                    ", "\n                                </select>\n                                ", "\n                            </div>\n                        </div>\n                        <div class=\"workbench-field\"><label for=\"namespace\">Namespace</label>\n                            <input type=\"text\" id=\"namespace\" name=\"namespace\" size=\"48\" value=", " />\n                        </div>\n                    </div>\n                    <div class=\"workbench-form-actions\">\n                        <span class=\"workbench-action workbench-action--primary\"><label class=\"workbench-action-hit-area\">", "\n                            <span class=\"workbench-action-label\"><button type=\"submit\" value=\"Update\">Update</button></span></label></span>\n                        <span class=\"workbench-action workbench-action--danger-outline\"><label class=\"workbench-action-hit-area\">", "\n                            <span class=\"workbench-action-label\"><button type=\"submit\" value=\"Delete\" @click=", ">Delete</button></span></label></span>\n                    </div>\n                </form>\n                <section id=\"namespaces-results\" class=\"workbench-island workbench-responsive-records\">\n                    ", "\n                </section>"], ["", "\n                <form id=\"namespaces-form\" class=\"workbench-island\" action=\"namespaces\" method=\"post\">\n                    <div class=\"workbench-form-grid\">\n                        <div class=\"workbench-field\"><label for=\"prefix\">Prefix</label>\n                            <div class=\"workbench-inline-controls\">\n                                <input type=\"text\" id=\"prefix\" name=\"prefix\" size=\"8\" value=", " />\n                                <select id=\"prefix-select\" data-workbench-window-picker=\"namespaces\" @change=", ">\n                                    <option value=\"\" ?selected=", "></option>\n                                    ", "\n                                    ", "\n                                </select>\n                                ", "\n                            </div>\n                        </div>\n                        <div class=\"workbench-field\"><label for=\"namespace\">Namespace</label>\n                            <input type=\"text\" id=\"namespace\" name=\"namespace\" size=\"48\" value=", " />\n                        </div>\n                    </div>\n                    <div class=\"workbench-form-actions\">\n                        <span class=\"workbench-action workbench-action--primary\"><label class=\"workbench-action-hit-area\">", "\n                            <span class=\"workbench-action-label\"><button type=\"submit\" value=\"Update\">Update</button></span></label></span>\n                        <span class=\"workbench-action workbench-action--danger-outline\"><label class=\"workbench-action-hit-area\">", "\n                            <span class=\"workbench-action-label\"><button type=\"submit\" value=\"Delete\" @click=", ">Delete</button></span></label></span>\n                    </div>\n                </form>\n                <section id=\"namespaces-results\" class=\"workbench-island workbench-responsive-records\">\n                    ", "\n                </section>"]), errorCallout(runtime, model), text(pageValue(model, 'prefix')), function (event) {
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
            return h(__makeTemplateObject(["<h3>", "</h3>\n                <ul>", "</ul>\n                ", ""], ["<h3>", "</h3>\n                <ul>", "</ul>\n                ", ""]), definition.title, page.items.map(function (value) { return h(__makeTemplateObject(["<li>", "</li>"], ["<li>", "</li>"]), renderTerm(runtime, value, context, true)); }), page.count > explorePageSize ? h(__makeTemplateObject(["<div class=\"workbench-form-actions workbench-window-controls\"\n                    role=\"group\" aria-label=", ">\n                    <span role=\"status\">Showing ", "\u2013", " of ", "</span>\n                    <button type=\"button\" class=\"workbench-action workbench-action--secondary\" data-workbench-explore-group=", "\n                        data-workbench-explore-action=\"previous\" ?disabled=", ">Previous</button>\n                    <button type=\"button\" class=\"workbench-action workbench-action--secondary\" data-workbench-explore-group=", "\n                        data-workbench-explore-action=\"next\" ?disabled=", ">Next</button>\n                </div>"], ["<div class=\"workbench-form-actions workbench-window-controls\"\n                    role=\"group\" aria-label=", ">\n                    <span role=\"status\">Showing ", "\u2013", " of ", "</span>\n                    <button type=\"button\" class=\"workbench-action workbench-action--secondary\" data-workbench-explore-group=", "\n                        data-workbench-explore-action=\"previous\" ?disabled=", ">Previous</button>\n                    <button type=\"button\" class=\"workbench-action workbench-action--secondary\" data-workbench-explore-group=", "\n                        data-workbench-explore-action=\"next\" ?disabled=", ">Next</button>\n                </div>"]), definition.title + ' pages', page.start + 1, page.start + page.items.length, page.count, definition.key, !page.hasPrevious, definition.key, !page.hasNext) : '');
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
            return h(__makeTemplateObject(["", "\n                ", "\n                ", "", "\n                <p id=\"explore-resource-summary\" class=\"workbench-page-meta\" ?hidden=", ">\n                    <span id=\"explore-resource-value\">", "</span><span id=\"explore-result-count\">", "</span>\n                </p>\n                <form id=\"explore-form\" class=\"workbench-island\" action=\"explore\">\n                    <input id=\"workbench-total-result-count\" type=\"hidden\"\n                        value=", " />\n                    <div id=\"explore-controls\"><div id=\"explore-resource-field\" class=\"workbench-field\">\n                        <label for=\"resource\">Resource</label><input id=\"resource\" name=\"resource\" size=\"48\" type=\"text\" value=", " />\n                    </div>\n                    ", "\n                    </div>\n                </form>\n                <section id=\"explore-results\" class=\"workbench-island workbench-responsive-records\">\n                    ", "\n                    <div id=\"explore-pagination\" class=\"workbench-form-actions\" ?hidden=", ">\n                        <button id=\"previousX\" class=\"workbench-action workbench-action--secondary\" type=\"button\" value=", "\n                            @click=", ">Previous ", "</button>\n                        <button id=\"nextX\" class=\"workbench-action workbench-action--secondary\" type=\"button\" value=", "\n                            @click=", ">Next ", "</button>\n                    </div>\n                </section>"], ["", "\n                ", "\n                ", "", "\n                <p id=\"explore-resource-summary\" class=\"workbench-page-meta\" ?hidden=", ">\n                    <span id=\"explore-resource-value\">", "</span><span id=\"explore-result-count\">", "</span>\n                </p>\n                <form id=\"explore-form\" class=\"workbench-island\" action=\"explore\">\n                    <input id=\"workbench-total-result-count\" type=\"hidden\"\n                        value=", " />\n                    <div id=\"explore-controls\"><div id=\"explore-resource-field\" class=\"workbench-field\">\n                        <label for=\"resource\">Resource</label><input id=\"resource\" name=\"resource\" size=\"48\" type=\"text\" value=", " />\n                    </div>\n                    ", "\n                    </div>\n                </form>\n                <section id=\"explore-results\" class=\"workbench-island workbench-responsive-records\">\n                    ", "\n                    <div id=\"explore-pagination\" class=\"workbench-form-actions\" ?hidden=", ">\n                        <button id=\"previousX\" class=\"workbench-action workbench-action--secondary\" type=\"button\" value=", "\n                            @click=", ">Previous ", "</button>\n                        <button id=\"nextX\" class=\"workbench-action workbench-action--secondary\" type=\"button\" value=", "\n                            @click=", ">Next ", "</button>\n                    </div>\n                </section>"]), resultLimited ? h(__makeTemplateObject(["<p id=\"result-limited\">The results shown maybe truncated.</p>"], ["<p id=\"result-limited\">The results shown maybe truncated.</p>"])) : '', errorCallout(runtime, model), summary.label ? h(__makeTemplateObject(["<h2>", "</h2>"], ["<h2>", "</h2>"]), text(summary.label)) : '', summary.comment ? h(__makeTemplateObject(["<p class=\"workbench-prose\">", "</p>"], ["<p class=\"workbench-prose\">", "</p>"]), text(summary.comment)) : '', !resource, resource, total, text(pageValue(model, 'total-result-count')), resource, workbench.detailDisclosure.render(h, {
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
            return h(__makeTemplateObject(["\n                        <div class=\"saved-query-row__heading\"><h2>", "</h2><span>", "</span></div>\n                        <div class=\"saved-query-actions\">\n                            <form method=\"post\" action=\"query\" id=", "\n                                data-workbench-query-execution=\"true\"\n                                data-workbench-results-target=", ">\n                                <input type=\"hidden\" name=\"action\" value=\"exec\" />\n                                <input type=\"hidden\" name=\"queryLn\" value=", " />\n                                <input type=\"hidden\" name=\"query\" value=", " />\n                                <input type=\"hidden\" name=\"ref\" value=\"id\" />\n                                <input type=\"hidden\" name=\"owner\" value=", " />\n                                <input type=\"hidden\" name=\"infer\" value=", " />\n                                <input type=\"hidden\" name=\"query-timeout\" value=", " />\n                                <span class=\"workbench-action workbench-action--primary\"><label class=\"workbench-action-hit-area\">\n                                    ", "<span class=\"workbench-action-label\"><input type=\"submit\" value=\"Execute\" /></span>\n                                </label></span>\n                            </form>\n                            <button type=\"button\" class=\"saved-query-toggle workbench-action workbench-action--secondary\" id=", " data-query-urn=", "\n                                value=\"Show\">Show</button>\n                            <form method=\"post\" action=\"query\"><input type=\"hidden\" name=\"action\" value=\"edit\" />\n                                <input type=\"hidden\" name=\"queryLn\" value=", " /><input type=\"hidden\" name=\"query\" value=", " />\n                                <input type=\"hidden\" name=\"ref\" value=\"id\" /><input type=\"hidden\" name=\"owner\" value=", " />\n                                <input type=\"hidden\" name=\"infer\" value=", " />\n                                <input type=\"hidden\" name=\"query-timeout\" value=", " /><button class=\"workbench-action workbench-action--secondary\" type=\"submit\">Edit</button>\n                            </form>\n                            <form method=\"post\" id=", " action=", ">\n                                <button type=\"button\" class=\"saved-query-delete workbench-action workbench-action--danger-outline\" data-query-owner=", " data-query-name=", "\n                                    data-query-urn=", ">Delete\u2026</button>\n                            </form>\n                        </div>\n                        <div id=", " class=\"query-results\" aria-live=\"polite\"></div>\n                        <table class=\"data\" id=", " style=\"display: none\"><tbody><tr>\n                            <th>Query Language</th><td>", "</td><th>Include Inferred Statements</th><td>", "</td>\n                            <th>Shared</th><td>", "</td>\n                        </tr></tbody></table>\n                        <textarea id=", " style=\"display: none\">", "</textarea>\n                    "], ["\n                        <div class=\"saved-query-row__heading\"><h2>", "</h2><span>", "</span></div>\n                        <div class=\"saved-query-actions\">\n                            <form method=\"post\" action=\"query\" id=", "\n                                data-workbench-query-execution=\"true\"\n                                data-workbench-results-target=", ">\n                                <input type=\"hidden\" name=\"action\" value=\"exec\" />\n                                <input type=\"hidden\" name=\"queryLn\" value=", " />\n                                <input type=\"hidden\" name=\"query\" value=", " />\n                                <input type=\"hidden\" name=\"ref\" value=\"id\" />\n                                <input type=\"hidden\" name=\"owner\" value=", " />\n                                <input type=\"hidden\" name=\"infer\" value=", " />\n                                <input type=\"hidden\" name=\"query-timeout\" value=", " />\n                                <span class=\"workbench-action workbench-action--primary\"><label class=\"workbench-action-hit-area\">\n                                    ", "<span class=\"workbench-action-label\"><input type=\"submit\" value=\"Execute\" /></span>\n                                </label></span>\n                            </form>\n                            <button type=\"button\" class=\"saved-query-toggle workbench-action workbench-action--secondary\" id=", " data-query-urn=", "\n                                value=\"Show\">Show</button>\n                            <form method=\"post\" action=\"query\"><input type=\"hidden\" name=\"action\" value=\"edit\" />\n                                <input type=\"hidden\" name=\"queryLn\" value=", " /><input type=\"hidden\" name=\"query\" value=", " />\n                                <input type=\"hidden\" name=\"ref\" value=\"id\" /><input type=\"hidden\" name=\"owner\" value=", " />\n                                <input type=\"hidden\" name=\"infer\" value=", " />\n                                <input type=\"hidden\" name=\"query-timeout\" value=", " /><button class=\"workbench-action workbench-action--secondary\" type=\"submit\">Edit</button>\n                            </form>\n                            <form method=\"post\" id=", " action=", ">\n                                <button type=\"button\" class=\"saved-query-delete workbench-action workbench-action--danger-outline\" data-query-owner=", " data-query-name=", "\n                                    data-query-urn=", ">Delete\u2026</button>\n                            </form>\n                        </div>\n                        <div id=", " class=\"query-results\" aria-live=\"polite\"></div>\n                        <table class=\"data\" id=", " style=\"display: none\"><tbody><tr>\n                            <th>Query Language</th><td>", "</td><th>Include Inferred Statements</th><td>", "</td>\n                            <th>Shared</th><td>", "</td>\n                        </tr></tbody></table>\n                        <textarea id=", " style=\"display: none\">", "</textarea>\n                    "]), queryName, owner, formId, 'saved-query-results-' + index, text(row.queryLn), queryName, owner, text(row.infer), queryTimeout, icon(runtime, 'execute'), urn + '-toggle', urn, text(row.queryLn), queryName, owner, text(row.infer), queryTimeout, urn, 'saved-queries?delete=' + encodeURIComponent(urn), owner, queryName, urn, 'saved-query-results-' + index, urn + '-metadata', text(row.queryLn), text(row.infer), text(row.shared), urn + '-text', query);
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
            return h(__makeTemplateObject(["<form id=\"export-form\" class=\"workbench-island workbench-form-card\" action=\"export\">\n                <div class=\"workbench-form-grid\">\n                    <div class=\"workbench-field\"><label for=\"Accept\">Download format</label>\n                        <select id=\"Accept\" name=\"Accept\">", "</select>\n                    </div>\n                    <div class=\"workbench-field\"><label for=\"compression\">Compression</label><select id=\"compression\" name=\"compression\">\n                        <option value=\"none\">None</option><option value=\"gzip\" selected>Gzip</option><option value=\"zip\">Zip</option>\n                    </select></div>\n                    <div class=\"workbench-field\"><label for=\"timeout\">Export timeout (seconds)</label>\n                        <input id=\"timeout\" name=\"timeout\" type=\"number\" min=\"0\" step=\"1\" required value=", " />\n                        <span class=\"hint\">Maximum time allowed for the export operation. Use 0 for no timeout.</span>\n                    </div>\n                </div>\n                ", "\n                <div class=\"workbench-form-actions\"><span class=\"workbench-action workbench-action--primary\">\n                    <span class=\"workbench-action-hit-area\"><span class=\"workbench-action-label\">\n                        <button type=\"submit\" name=\"action\" value=\"download\" aria-label=\"Download\">\n                            ", "<span>Download</span>\n                        </button>\n                    </span></span>\n                </span></div>\n            </form>\n            <section id=\"export-results\" class=\"workbench-island workbench-responsive-records\">\n                <p class=\"workbench-page-meta\">Statement preview</p>\n                <div class=\"workbench-form-actions\"><button class=\"workbench-action workbench-action--secondary\" type=\"submit\" form=\"export-form\" name=\"action\" value=\"preview\">Retrieve statements</button></div>\n                ", "\n            </section>"], ["<form id=\"export-form\" class=\"workbench-island workbench-form-card\" action=\"export\">\n                <div class=\"workbench-form-grid\">\n                    <div class=\"workbench-field\"><label for=\"Accept\">Download format</label>\n                        <select id=\"Accept\" name=\"Accept\">", "</select>\n                    </div>\n                    <div class=\"workbench-field\"><label for=\"compression\">Compression</label><select id=\"compression\" name=\"compression\">\n                        <option value=\"none\">None</option><option value=\"gzip\" selected>Gzip</option><option value=\"zip\">Zip</option>\n                    </select></div>\n                    <div class=\"workbench-field\"><label for=\"timeout\">Export timeout (seconds)</label>\n                        <input id=\"timeout\" name=\"timeout\" type=\"number\" min=\"0\" step=\"1\" required value=", " />\n                        <span class=\"hint\">Maximum time allowed for the export operation. Use 0 for no timeout.</span>\n                    </div>\n                </div>\n                ", "\n                <div class=\"workbench-form-actions\"><span class=\"workbench-action workbench-action--primary\">\n                    <span class=\"workbench-action-hit-area\"><span class=\"workbench-action-label\">\n                        <button type=\"submit\" name=\"action\" value=\"download\" aria-label=\"Download\">\n                            ", "<span>Download</span>\n                        </button>\n                    </span></span>\n                </span></div>\n            </form>\n            <section id=\"export-results\" class=\"workbench-island workbench-responsive-records\">\n                <p class=\"workbench-page-meta\">Statement preview</p>\n                <div class=\"workbench-form-actions\"><button class=\"workbench-action workbench-action--secondary\" type=\"submit\" form=\"export-form\" name=\"action\" value=\"preview\">Retrieve statements</button></div>\n                ", "\n            </section>"]), graphFormats.map(function (format) { return h(__makeTemplateObject(["<option value=", " ?selected=", ">", "</option>"], ["<option value=", " ?selected=", ">", "</option>"]), format.value, format.value === defaultFormat, format.label); }), timeout, workbench.detailDisclosure.render(h, {
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
            return h(__makeTemplateObject(["", "\n                ", "\n                <form method=\"post\" action=\"add\" enctype=\"multipart/form-data\" class=\"workbench-form-card\">\n                    <fieldset id=\"add-source-tabs\" class=\"workbench-source-tabs\"><legend>Source</legend>\n                        ", "\n                    </fieldset>\n                    <div class=\"workbench-form-grid add-source-fields\">\n                        <div id=\"add-source-file-panel\" class=\"workbench-field add-source-panel\" data-source=\"file\">\n                            <label for=\"file\">RDF file</label><input type=\"file\" id=\"file\" name=\"content\"\n                                @change=", " />\n                            <div class=\"hint\">Select an RDF document from your computer.</div>\n                        </div>\n                        <div id=\"add-source-url-panel\" class=\"workbench-field add-source-panel\" data-source=\"url\" hidden>\n                            <label for=\"url\">RDF URL</label><input id=\"url\" name=\"url\" type=\"text\" size=\"48\" disabled\n                                @change=", " />\n                        </div>\n                        <div id=\"add-source-text-panel\" class=\"workbench-field add-source-panel\" data-source=\"text\" hidden>\n                            <label for=\"text\">RDF text</label><textarea id=\"text\" name=\"content\" rows=\"6\" cols=\"70\" disabled></textarea>\n                        </div>\n                        <div class=\"workbench-field add-source-format\"><label for=\"Content-Type\">Data format</label>\n                            <div class=\"workbench-select-control\"><select id=\"Content-Type\" name=\"Content-Type\">\n                                <option id=\"autodetect\" value=\"autodetect\" selected>(autodetect)</option>\n                                ", "\n                            </select>", "</div>\n                        </div>\n                    </div>\n                    ", "\n                    <div id=\"add-upload-actions\" class=\"workbench-form-actions\"><span class=\"workbench-action workbench-action--primary\">\n                        <label class=\"workbench-action-hit-area\">", "<span class=\"workbench-action-label\"><input type=\"submit\" value=\"Upload\" /></span></label>\n                    </span></div>\n                </form>"], ["", "\n                ", "\n                <form method=\"post\" action=\"add\" enctype=\"multipart/form-data\" class=\"workbench-form-card\">\n                    <fieldset id=\"add-source-tabs\" class=\"workbench-source-tabs\"><legend>Source</legend>\n                        ", "\n                    </fieldset>\n                    <div class=\"workbench-form-grid add-source-fields\">\n                        <div id=\"add-source-file-panel\" class=\"workbench-field add-source-panel\" data-source=\"file\">\n                            <label for=\"file\">RDF file</label><input type=\"file\" id=\"file\" name=\"content\"\n                                @change=", " />\n                            <div class=\"hint\">Select an RDF document from your computer.</div>\n                        </div>\n                        <div id=\"add-source-url-panel\" class=\"workbench-field add-source-panel\" data-source=\"url\" hidden>\n                            <label for=\"url\">RDF URL</label><input id=\"url\" name=\"url\" type=\"text\" size=\"48\" disabled\n                                @change=", " />\n                        </div>\n                        <div id=\"add-source-text-panel\" class=\"workbench-field add-source-panel\" data-source=\"text\" hidden>\n                            <label for=\"text\">RDF text</label><textarea id=\"text\" name=\"content\" rows=\"6\" cols=\"70\" disabled></textarea>\n                        </div>\n                        <div class=\"workbench-field add-source-format\"><label for=\"Content-Type\">Data format</label>\n                            <div class=\"workbench-select-control\"><select id=\"Content-Type\" name=\"Content-Type\">\n                                <option id=\"autodetect\" value=\"autodetect\" selected>(autodetect)</option>\n                                ", "\n                            </select>", "</div>\n                        </div>\n                    </div>\n                    ", "\n                    <div id=\"add-upload-actions\" class=\"workbench-form-actions\"><span class=\"workbench-action workbench-action--primary\">\n                        <label class=\"workbench-action-hit-area\">", "<span class=\"workbench-action-label\"><input type=\"submit\" value=\"Upload\" /></span></label>\n                    </span></div>\n                </form>"]), error ? callout(runtime, 'error', error) : '', systemRepositoryCallout(runtime, context), [['file', 'File'], ['url', 'URL'], ['text', 'Text']].map(function (entry) { return h(__makeTemplateObject(["<label for=", ">\n                            ", "\n                            <input type=\"radio\" id=", " name=\"source\"\n                                value=", "\n                                ?checked=", "\n                                @change=", " />\n                            <span>", "</span>\n                        </label>"], ["<label for=", ">\n                            ", "\n                            <input type=\"radio\" id=", " name=\"source\"\n                                value=", "\n                                ?checked=", "\n                                @change=", " />\n                            <span>", "</span>\n                        </label>"]), 'source-' + entry[0], icon(runtime, 'source-' + (entry[0] === 'text' ? 'text' : entry[0])), 'source-' + entry[0], entry[0] === 'text' ? 'contents' : entry[0], entry[0] === 'file', function () { return invoke('workbench.add.enabledInput', entry[0]); }, entry[1]); }), function () { return invoke('workbench.add.enabledInput', 'file'); }, function () { return invoke('workbench.add.enabledInput', 'url'); }, formats.map(function (format) { return h(__makeTemplateObject(["<option value=", ">", "</option>"], ["<option value=", ">", "</option>"]), format.value, format.label); }), icon(runtime, 'chevron', 'workbench-select-chevron'), workbench.detailDisclosure.render(h, {
                id: 'add-import-settings', toggleId: 'add-import-settings-toggle',
                panelId: 'add-import-settings-panel', label: 'Advanced settings',
                ownerClass: 'workbench-options workbench-form-subgroup'
            }, h(__makeTemplateObject(["<div class=\"workbench-form-grid\">\n                            <div class=\"workbench-field workbench-disclosure__field\"><label for=\"baseURI\">Base URI</label>\n                                <input id=\"baseURI\" name=\"baseURI\" type=\"text\" size=\"48\" value=", " />\n                                <label class=\"workbench-check\" for=\"overrideContext\"><input type=\"checkbox\" id=\"overrideContext\" name=\"overrideContext\"\n                                    ?checked=", " @change=", " />\n                                    <span>Override parsed contexts with this context</span></label>\n                            </div>\n                            <div class=\"workbench-field workbench-disclosure__field\"><label for=\"context\">Context</label>\n                                <input id=\"context\" name=\"context\" type=\"text\" size=\"48\" aria-describedby=\"context-help\"\n                                    value=", " ?disabled=", " />\n                                <p id=\"context-help\" class=\"workbench-help\">RDF context may be an IRI, blank node, or the default graph. With override off, embedded contexts are preserved; contextless data uses the default graph. Base URI resolves relative RDF identifiers; it does not choose a graph context.</p>\n                            </div>\n                            <div class=\"workbench-field workbench-disclosure__field\"><label for=\"transaction-setting__org.eclipse.rdf4j.common.transaction.IsolationLevel\">Isolation level</label>\n                                <select id=\"transaction-setting__org.eclipse.rdf4j.common.transaction.IsolationLevel\"\n                                    name=\"transaction-setting__org.eclipse.rdf4j.common.transaction.IsolationLevel\">\n                                    <option value=\"\" ?selected=", ">Default</option>\n                                    ", "\n                                </select>\n                                <div class=\"hint\">Choose the transaction isolation level used for this import.</div>\n                            </div>\n                        </div>"], ["<div class=\"workbench-form-grid\">\n                            <div class=\"workbench-field workbench-disclosure__field\"><label for=\"baseURI\">Base URI</label>\n                                <input id=\"baseURI\" name=\"baseURI\" type=\"text\" size=\"48\" value=", " />\n                                <label class=\"workbench-check\" for=\"overrideContext\"><input type=\"checkbox\" id=\"overrideContext\" name=\"overrideContext\"\n                                    ?checked=", " @change=", " />\n                                    <span>Override parsed contexts with this context</span></label>\n                            </div>\n                            <div class=\"workbench-field workbench-disclosure__field\"><label for=\"context\">Context</label>\n                                <input id=\"context\" name=\"context\" type=\"text\" size=\"48\" aria-describedby=\"context-help\"\n                                    value=", " ?disabled=", " />\n                                <p id=\"context-help\" class=\"workbench-help\">RDF context may be an IRI, blank node, or the default graph. With override off, embedded contexts are preserved; contextless data uses the default graph. Base URI resolves relative RDF identifiers; it does not choose a graph context.</p>\n                            </div>\n                            <div class=\"workbench-field workbench-disclosure__field\"><label for=\"transaction-setting__org.eclipse.rdf4j.common.transaction.IsolationLevel\">Isolation level</label>\n                                <select id=\"transaction-setting__org.eclipse.rdf4j.common.transaction.IsolationLevel\"\n                                    name=\"transaction-setting__org.eclipse.rdf4j.common.transaction.IsolationLevel\">\n                                    <option value=\"\" ?selected=", ">Default</option>\n                                    ", "\n                                </select>\n                                <div class=\"hint\">Choose the transaction isolation level used for this import.</div>\n                            </div>\n                        </div>"]), text(pageValue(model, 'baseURI')), !!text(pageValue(model, 'context')), function () { return invoke('workbench.add.handleContextOverride'); }, text(pageValue(model, 'context')), !text(pageValue(model, 'context')), !selectedIsolation, isolationOptions.map(function (row) { return h(__makeTemplateObject(["<option value=", "\n                                        ?selected=", ">\n                                        ", "</option>"], ["<option value=", "\n                                        ?selected=", ">\n                                        ", "</option>"]), text(row['isolation-level-option']), text(row['isolation-level-option']) === selectedIsolation, text(row['isolation-level-option-label']) || text(row['isolation-level-option'])); }))), icon(runtime, 'upload'));
        }
        function removePage(runtime, model, context) {
            var h = runtime.html;
            return h(__makeTemplateObject(["<form id=\"remove-form\" class=\"workbench-island workbench-form-card\" method=\"post\" action=\"remove\">\n                ", "\n                ", "\n                <p>Values use RDF syntax: IRIs in angle brackets, blank nodes as _:nodeID, and literals in double quotes with optional language or datatype.</p>\n                <details id=\"remove-examples\" class=\"workbench-options\"><summary>Examples", "</summary>\n                    <ul><li>URI: <tt>&lt;http://foo.com/bar&gt;</tt></li><li>BNode: <tt>_:nodeID</tt></li>\n                        <li>Literal: <tt>\"Hello\"</tt>, <tt>\"Hello\"@en</tt>, or <tt>\"Hello\"^^&lt;http://bar.com/foo&gt;</tt></li></ul>\n                </details>\n                ", "\n                    <div class=\"workbench-field-stack\">\n                        ", "\n                    </div>\n                    <div class=\"workbench-form-actions\"><span class=\"workbench-action workbench-action--danger-outline\"><label class=\"workbench-action-hit-area\">\n                        ", "<span class=\"workbench-action-label\"><input type=\"submit\" value=\"Remove\" /></span>\n                    </label></span></div>\n                </form>"], ["<form id=\"remove-form\" class=\"workbench-island workbench-form-card\" method=\"post\" action=\"remove\">\n                ", "\n                ", "\n                <p>Values use RDF syntax: IRIs in angle brackets, blank nodes as _:nodeID, and literals in double quotes with optional language or datatype.</p>\n                <details id=\"remove-examples\" class=\"workbench-options\"><summary>Examples", "</summary>\n                    <ul><li>URI: <tt>&lt;http://foo.com/bar&gt;</tt></li><li>BNode: <tt>_:nodeID</tt></li>\n                        <li>Literal: <tt>\"Hello\"</tt>, <tt>\"Hello\"@en</tt>, or <tt>\"Hello\"^^&lt;http://bar.com/foo&gt;</tt></li></ul>\n                </details>\n                ", "\n                    <div class=\"workbench-field-stack\">\n                        ", "\n                    </div>\n                    <div class=\"workbench-form-actions\"><span class=\"workbench-action workbench-action--danger-outline\"><label class=\"workbench-action-hit-area\">\n                        ", "<span class=\"workbench-action-label\"><input type=\"submit\" value=\"Remove\" /></span>\n                    </label></span></div>\n                </form>"]), systemRepositoryCallout(runtime, context), callout(runtime, 'warning', 'Only statements matching the supplied values will be removed. An empty form is rejected.', 'Remove is permanent.', 'remove-warning'), icon(runtime, 'chevron', 'workbench-disclosure-chevron'), errorCallout(runtime, model), [['subj', 'Subject', 'text'], ['pred', 'Predicate', 'text'], ['obj', 'Object', 'textarea'], ['context', 'Context', 'text']].map(function (entry) { return h(__makeTemplateObject(["<div class=\"workbench-field\">\n                            <label for=", ">", "</label>", "\n                        </div>"], ["<div class=\"workbench-field\">\n                            <label for=", ">", "</label>", "\n                        </div>"]), entry[0], entry[1], entry[2] === 'textarea'
                ? h(__makeTemplateObject(["<textarea id=\"obj\" name=\"obj\" rows=\"3\">", "</textarea>"], ["<textarea id=\"obj\" name=\"obj\" rows=\"3\">", "</textarea>"]), text(pageValue(model, 'obj'))) : h(__makeTemplateObject(["<input id=", " name=", " type=\"text\" value=", " />"], ["<input id=", " name=", " type=\"text\" value=", " />"]), entry[0], entry[0], text(pageValue(model, entry[0])))); }), icon(runtime, 'remove'));
        }
        function clearPage(runtime, model, context) {
            var h = runtime.html;
            return h(__makeTemplateObject(["<form id=\"clear-form\" class=\"workbench-island workbench-form-card\" method=\"post\" action=\"clear\">\n                ", "\n                ", "\n                ", "\n                    <div class=\"workbench-field-stack\">\n                        <div class=\"workbench-field\"><label for=\"context\">Context</label>\n                            <input id=\"context\" name=\"context\" type=\"text\" value=", " /></div>\n                    </div>\n                    <div class=\"workbench-form-actions\"><span class=\"workbench-action workbench-action--danger-outline\"><label class=\"workbench-action-hit-area\">\n                        ", "<span class=\"workbench-action-label\"><input type=\"submit\" value=\"Clear context\" /></span>\n                    </label></span></div>\n                </form>"], ["<form id=\"clear-form\" class=\"workbench-island workbench-form-card\" method=\"post\" action=\"clear\">\n                ", "\n                ", "\n                ", "\n                    <div class=\"workbench-field-stack\">\n                        <div class=\"workbench-field\"><label for=\"context\">Context</label>\n                            <input id=\"context\" name=\"context\" type=\"text\" value=", " /></div>\n                    </div>\n                    <div class=\"workbench-form-actions\"><span class=\"workbench-action workbench-action--danger-outline\"><label class=\"workbench-action-hit-area\">\n                        ", "<span class=\"workbench-action-label\"><input type=\"submit\" value=\"Clear context\" /></span>\n                    </label></span></div>\n                </form>"]), systemRepositoryCallout(runtime, context), callout(runtime, 'warning', 'Clearing without a context removes every statement in this repository.', 'Clear is permanent.', 'clear-warning'), errorCallout(runtime, model), text(pageValue(model, 'context')), icon(runtime, 'clear'));
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
            return h(__makeTemplateObject(["<form id=\"server-form\" class=\"workbench-form-card\" action=\"server\" method=\"post\"\n                    @submit=", ">\n                <div class=\"workbench-form-grid server-connection-fields\"><div class=\"workbench-field\">\n                    <label for=\"workbench-server\">Server</label><input id=\"workbench-server\" name=\"workbench-server\" type=\"text\" size=\"40\" value=", " />\n                    <div class=\"hint\">Enter the URL of an RDF4J Server.</div><span class=\"error\" role=\"alert\">", "</span>\n                </div></div>\n                ", "\n                <div id=\"server-change-actions\" class=\"workbench-form-actions\"><span class=\"workbench-action workbench-action--primary\">\n                    <label class=\"workbench-action-hit-area\">", "<span class=\"workbench-action-label\"><input type=\"submit\" value=\"Change\" /></span></label>\n                </span></div>\n            </form>"], ["<form id=\"server-form\" class=\"workbench-form-card\" action=\"server\" method=\"post\"\n                    @submit=", ">\n                <div class=\"workbench-form-grid server-connection-fields\"><div class=\"workbench-field\">\n                    <label for=\"workbench-server\">Server</label><input id=\"workbench-server\" name=\"workbench-server\" type=\"text\" size=\"40\" value=", " />\n                    <div class=\"hint\">Enter the URL of an RDF4J Server.</div><span class=\"error\" role=\"alert\">", "</span>\n                </div></div>\n                ", "\n                <div id=\"server-change-actions\" class=\"workbench-form-actions\"><span class=\"workbench-action workbench-action--primary\">\n                    <label class=\"workbench-action-hit-area\">", "<span class=\"workbench-action-label\"><input type=\"submit\" value=\"Change\" /></span></label>\n                </span></div>\n            </form>"]), function (event) {
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
                : 'query-compare-pane query-compare-pane--primary', queryId, compare ? 'Compare query' : 'Query', compare ? h(__makeTemplateObject(["<button id=\"query-compare-close\" class=\"query-compare-pane__close workbench-action workbench-action--ghost workbench-action--icon\" type=\"button\"\n                            aria-label=\"Close comparison\" title=\"Close comparison\"\n                            @click=", ">", "</button>"], ["<button id=\"query-compare-close\" class=\"query-compare-pane__close workbench-action workbench-action--ghost workbench-action--icon\" type=\"button\"\n                            aria-label=\"Close comparison\" title=\"Close comparison\"\n                            @click=", ">", "</button>"]), function () { return invoke('workbench.query.closeComparePane'); }, icon(runtime, 'close', 'query-compare-pane__close-icon')) : '', compare
                ? h(__makeTemplateObject(["<textarea id=\"query-compare\" rows=\"16\" cols=\"80\" wrap=\"soft\"></textarea>"], ["<textarea id=\"query-compare\" rows=\"16\" cols=\"80\" wrap=\"soft\"></textarea>"])) : h(__makeTemplateObject(["<textarea id=\"query\" name=\"query\" rows=\"16\" cols=\"80\" wrap=\"soft\">", "</textarea>"], ["<textarea id=\"query\" name=\"query\" rows=\"16\" cols=\"80\" wrap=\"soft\">", "</textarea>"]), text(options.query)), 'queryString.errors' + suffix, compare ? '' : text(options.error), 'query-explanation-row' + suffix, !queryFeatureEnabled(context, 'query-explain'), !compare && !explanation ? 'display:none;' : '', 'query-explanation-status' + suffix, compare ? h(__makeTemplateObject(["<button id=\"copy-explanation-compare\" class=\"query-explanation-copy workbench-action workbench-action--secondary\"\n                                type=\"button\" ?hidden=", ">Copy explanation</button>"], ["<button id=\"copy-explanation-compare\" class=\"query-explanation-copy workbench-action workbench-action--secondary\"\n                                type=\"button\" ?hidden=", ">Copy explanation</button>"]), !queryFeatureEnabled(context, 'explain-copy')) : h(__makeTemplateObject(["<button id=\"copy-explanation\" class=\"query-explanation-copy workbench-action workbench-action--secondary\"\n                                    type=\"button\" ?hidden=", ">Copy explanation</button>"], ["<button id=\"copy-explanation\" class=\"query-explanation-copy workbench-action workbench-action--secondary\"\n                                    type=\"button\" ?hidden=", ">Copy explanation</button>"]), !queryFeatureEnabled(context, 'explain-copy')), compare ? '' : h(__makeTemplateObject(["<select id=\"explain-format\" name=\"explain-format\"\n                                ?hidden=", ">\n                                ", "\n                            </select><select id=\"explain-level\" ?hidden=", ">\n                                ", "\n                            </select>\n                            ", "\n                            "], ["<select id=\"explain-format\" name=\"explain-format\"\n                                ?hidden=", ">\n                                ", "\n                            </select><select id=\"explain-level\" ?hidden=", ">\n                                ", "\n                            </select>\n                            ", "\n                            "]), allExplainFormatsDisabled, explainFormats.map(formatOption), allExplainLevelsDisabled, explainLevels.map(levelOption), workbench.detailDisclosure.render(h, {
                id: 'explanation-settings', toggleId: 'explanation-settings-toggle',
                panelId: 'explanation-settings-panel', label: 'Config', hidden: !queryExplainSettingsEnabled(context),
                ownerClass: 'query-explanation-settings', toggleClass: 'query-explanation-settings__toggle workbench-action workbench-action--secondary',
                panelClass: 'query-explanation-settings__panel',
                panelRole: 'group'
            }, h(__makeTemplateObject(["\n                                    <div class=\"query-explanation-settings__section\" ?hidden=", ">\n                                        <div class=\"query-explanation-settings__header\"><strong>Highlighting</strong></div>\n                                        <div class=\"query-explanation-settings__highlighting\">\n                                            <span id=\"explanation-highlight-mode\" class=\"query-explanation-highlight-mode\" role=\"radiogroup\"\n                                                aria-label=\"Text explanation highlighting\" ?hidden=", ">\n                                                <label class=\"workbench-choice\" for=\"explanation-highlight-syntax\"\n                                                    ?hidden=", ">\n                                                    <input id=\"explanation-highlight-syntax\" name=\"explanation-highlight-mode\" type=\"radio\" value=\"syntax\" checked\n                                                        ?hidden=", " />\n                                                    <span>Normal</span>\n                                                </label>\n                                                <label class=\"workbench-choice\" for=\"explanation-highlight-hotspot\"\n                                                    ?hidden=", ">\n                                                    <input id=\"explanation-highlight-hotspot\" name=\"explanation-highlight-mode\" type=\"radio\" value=\"hotspot\"\n                                                        ?hidden=", " />\n                                                    <span>Heatmap</span>\n                                                </label>\n                                            </span><span id=\"explanation-hotspot-legend\" aria-live=\"polite\"></span>\n                                        </div>\n                                    </div>\n                                    <div id=\"explanation-property-config\" class=\"query-explanation-settings__section query-explanation-property-config\"\n                                        ?hidden=", ">\n                                        <div class=\"query-explanation-property-config__header\">\n                                            <div class=\"query-explanation-property-config__title\">\n                                                <strong>Visible properties</strong><span id=\"explanation-property-count\" aria-live=\"polite\"></span>\n                                            </div>\n                                            <div class=\"query-explanation-property-config__actions\">\n                                                <button id=\"explanation-properties-all\" class=\"workbench-action workbench-action--secondary\" type=\"button\" ?hidden=", ">All</button>\n                                                <button id=\"explanation-properties-none\" class=\"workbench-action workbench-action--secondary\" type=\"button\" ?hidden=", ">None</button>\n                                            </div>\n                                        </div>\n                                        <div id=\"explanation-property-options\" class=\"query-explanation-property-config__options\"\n                                            role=\"group\" aria-label=\"Visible query plan properties\"></div>\n                                        <p class=\"query-explanation-property-config__hint\">Plan structure always remains visible.</p>\n                                    </div>\n                            "], ["\n                                    <div class=\"query-explanation-settings__section\" ?hidden=", ">\n                                        <div class=\"query-explanation-settings__header\"><strong>Highlighting</strong></div>\n                                        <div class=\"query-explanation-settings__highlighting\">\n                                            <span id=\"explanation-highlight-mode\" class=\"query-explanation-highlight-mode\" role=\"radiogroup\"\n                                                aria-label=\"Text explanation highlighting\" ?hidden=", ">\n                                                <label class=\"workbench-choice\" for=\"explanation-highlight-syntax\"\n                                                    ?hidden=", ">\n                                                    <input id=\"explanation-highlight-syntax\" name=\"explanation-highlight-mode\" type=\"radio\" value=\"syntax\" checked\n                                                        ?hidden=", " />\n                                                    <span>Normal</span>\n                                                </label>\n                                                <label class=\"workbench-choice\" for=\"explanation-highlight-hotspot\"\n                                                    ?hidden=", ">\n                                                    <input id=\"explanation-highlight-hotspot\" name=\"explanation-highlight-mode\" type=\"radio\" value=\"hotspot\"\n                                                        ?hidden=", " />\n                                                    <span>Heatmap</span>\n                                                </label>\n                                            </span><span id=\"explanation-hotspot-legend\" aria-live=\"polite\"></span>\n                                        </div>\n                                    </div>\n                                    <div id=\"explanation-property-config\" class=\"query-explanation-settings__section query-explanation-property-config\"\n                                        ?hidden=", ">\n                                        <div class=\"query-explanation-property-config__header\">\n                                            <div class=\"query-explanation-property-config__title\">\n                                                <strong>Visible properties</strong><span id=\"explanation-property-count\" aria-live=\"polite\"></span>\n                                            </div>\n                                            <div class=\"query-explanation-property-config__actions\">\n                                                <button id=\"explanation-properties-all\" class=\"workbench-action workbench-action--secondary\" type=\"button\" ?hidden=", ">All</button>\n                                                <button id=\"explanation-properties-none\" class=\"workbench-action workbench-action--secondary\" type=\"button\" ?hidden=", ">None</button>\n                                            </div>\n                                        </div>\n                                        <div id=\"explanation-property-options\" class=\"query-explanation-property-config__options\"\n                                            role=\"group\" aria-label=\"Visible query plan properties\"></div>\n                                        <p class=\"query-explanation-property-config__hint\">Plan structure always remains visible.</p>\n                                    </div>\n                            "]), allHighlightingDisabled, allHighlightingDisabled, !queryFeatureEnabled(context, 'explain-highlight-syntax'), !queryFeatureEnabled(context, 'explain-highlight-syntax'), !queryFeatureEnabled(context, 'explain-highlight-hotspot'), !queryFeatureEnabled(context, 'explain-highlight-hotspot'), !propertySelectionEnabled, !propertySelectionEnabled, !propertySelectionEnabled))), 'query-explanation-overlay' + suffix, compare ? h(__makeTemplateObject(["<pre id=\"query-explanation-compare\" data-format=", "\n                                ?hidden=", ">", "</pre>"], ["<pre id=\"query-explanation-compare\" data-format=", "\n                                ?hidden=", ">", "</pre>"]), selectedFormat, !queryFeatureEnabled(context, 'explain-view-text'), explanation) : h(__makeTemplateObject(["<pre id=\"query-explanation\" data-format=", "\n                                    ?hidden=", ">", "</pre>"], ["<pre id=\"query-explanation\" data-format=", "\n                                    ?hidden=", ">", "</pre>"]), selectedFormat, !queryFeatureEnabled(context, 'explain-view-text'), explanation), compare ? h(__makeTemplateObject(["<div id=\"query-explanation-dot-view-compare\"\n                                ?hidden=", "></div>"], ["<div id=\"query-explanation-dot-view-compare\"\n                                ?hidden=", "></div>"]), !queryFeatureEnabled(context, 'explain-view-dot')) : h(__makeTemplateObject(["<div id=\"query-explanation-dot-view\"\n                                    ?hidden=", "></div>"], ["<div id=\"query-explanation-dot-view\"\n                                    ?hidden=", "></div>"]), !queryFeatureEnabled(context, 'explain-view-dot')), compare ? h(__makeTemplateObject(["<div id=\"query-explanation-json-view-compare\"\n                                ?hidden=", "></div>"], ["<div id=\"query-explanation-json-view-compare\"\n                                ?hidden=", "></div>"]), !queryFeatureEnabled(context, 'explain-view-json')) : h(__makeTemplateObject(["<div id=\"query-explanation-json-view\"\n                                    ?hidden=", "></div>"], ["<div id=\"query-explanation-json-view\"\n                                    ?hidden=", "></div>"]), !queryFeatureEnabled(context, 'explain-view-json')), compare ? '' : h(__makeTemplateObject(["<div id=\"query-explanation-controls-row\" class=\"query-explanation-controls-row-class\"\n                            ?hidden=", "\n                            style=", ">\n                            <span id=\"primary-explain-settings\" class=\"query-form__field--controls-group\">\n                                <span id=\"primary-explain-repeat-controls\" class=\"query-form__field--controls-group\">\n                                    <button id=\"rerun-explanation\" class=\"workbench-action workbench-action--secondary\" type=\"button\"\n                                        ?hidden=", "\n                                        data-query-rerun-enabled=", "\n                                        @click=", ">Explain again</button>\n                                    <span id=\"rerun-explanation-spinner\" class=\"query-explain-spinner\" aria-hidden=\"true\"></span>\n                                    <button id=\"rerun-explanation-cancel\" class=\"query-explain-cancel workbench-action workbench-action--secondary\" type=\"button\" disabled\n                                        ?hidden=", "\n                                        @click=", ">Cancel</button>\n                                </span>\n                                <span id=\"primary-explain-utility-controls\" class=\"query-form__field--controls-group\">\n                                    <button id=\"download-explanation\" class=\"workbench-action workbench-action--secondary\" type=\"button\" ?disabled=", "\n                                        ?hidden=", ">Download explanation</button>\n                                    <button id=\"compare-toggle\" class=\"workbench-action workbench-action--secondary\" type=\"button\" ?hidden=", "\n                                        @click=", ">Compare</button>\n                                </span>\n                            </span>\n                        </div>"], ["<div id=\"query-explanation-controls-row\" class=\"query-explanation-controls-row-class\"\n                            ?hidden=", "\n                            style=", ">\n                            <span id=\"primary-explain-settings\" class=\"query-form__field--controls-group\">\n                                <span id=\"primary-explain-repeat-controls\" class=\"query-form__field--controls-group\">\n                                    <button id=\"rerun-explanation\" class=\"workbench-action workbench-action--secondary\" type=\"button\"\n                                        ?hidden=", "\n                                        data-query-rerun-enabled=", "\n                                        @click=", ">Explain again</button>\n                                    <span id=\"rerun-explanation-spinner\" class=\"query-explain-spinner\" aria-hidden=\"true\"></span>\n                                    <button id=\"rerun-explanation-cancel\" class=\"query-explain-cancel workbench-action workbench-action--secondary\" type=\"button\" disabled\n                                        ?hidden=", "\n                                        @click=", ">Cancel</button>\n                                </span>\n                                <span id=\"primary-explain-utility-controls\" class=\"query-form__field--controls-group\">\n                                    <button id=\"download-explanation\" class=\"workbench-action workbench-action--secondary\" type=\"button\" ?disabled=", "\n                                        ?hidden=", ">Download explanation</button>\n                                    <button id=\"compare-toggle\" class=\"workbench-action workbench-action--secondary\" type=\"button\" ?hidden=", "\n                                        @click=", ">Compare</button>\n                                </span>\n                            </span>\n                        </div>"]), !queryFeatureEnabled(context, 'query-explain'), !explanation ? 'display:none;' : '', !queryFeatureEnabled(context, 'query-rerun'), queryFeatureEnabled(context, 'query-rerun') ? 'true' : 'false', function () { return invoke('workbench.query.runExplain', null, 'rerun-explanation'); }, !queryFeatureEnabled(context, 'explain-cancel'), function () { return invoke('workbench.query.cancelExplain'); }, !explanation, !queryFeatureEnabled(context, 'explain-download'), !queryFeatureEnabled(context, 'query-compare'), function () { return invoke('workbench.query.toggleCompareMode'); }));
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
                toggleClass: 'query-disclosure__toggle workbench-action workbench-action--secondary',
                panelClass: 'query-disclosure__body query-disclosure__panel query-save-disclosure__body',
                contentClass: 'workbench-disclosure__fields',
                toggleHidden: !queryFeatureEnabled(context, 'query-save')
            }, h(__makeTemplateObject(["<div class=\"workbench-disclosure__field query-save-disclosure__name-field\">\n                    <label class=\"query-form__label\" for=\"query-name\">Query name</label>\n                    <input id=\"query-name\" name=\"query-name\" type=\"text\" size=\"32\" maxlength=\"32\" value=\"\" />\n                </div>\n                <label class=\"query-option query-save-disclosure__private\"\n                    ?hidden=", ">\n                    <input id=\"save-private\" name=\"save-private\" type=\"checkbox\" value=\"true\"\n                        ?hidden=", " />Private</label>\n                <div class=\"workbench-disclosure__actions query-disclosure__actions\">\n                    <input id=\"save\" type=\"submit\" value=\"Save\" disabled\n                        ?hidden=", " /> <span id=\"save-feedback\"></span>\n                </div>"], ["<div class=\"workbench-disclosure__field query-save-disclosure__name-field\">\n                    <label class=\"query-form__label\" for=\"query-name\">Query name</label>\n                    <input id=\"query-name\" name=\"query-name\" type=\"text\" size=\"32\" maxlength=\"32\" value=\"\" />\n                </div>\n                <label class=\"query-option query-save-disclosure__private\"\n                    ?hidden=", ">\n                    <input id=\"save-private\" name=\"save-private\" type=\"checkbox\" value=\"true\"\n                        ?hidden=", " />Private</label>\n                <div class=\"workbench-disclosure__actions query-disclosure__actions\">\n                    <input id=\"save\" type=\"submit\" value=\"Save\" disabled\n                        ?hidden=", " /> <span id=\"save-feedback\"></span>\n                </div>"]), !queryFeatureEnabled(context, 'query-private-save'), !queryFeatureEnabled(context, 'query-private-save'), !queryFeatureEnabled(context, 'query-save')));
            var optionsDisclosure = workbench.detailDisclosure.renderSeparated(h, {
                id: 'query-options-disclosure', toggleId: 'query-options-toggle',
                panelId: 'query-options-panel', label: 'Options',
                ownerClass: 'query-disclosure query-options-disclosure',
                toggleClass: 'query-disclosure__toggle workbench-action workbench-action--secondary',
                panelClass: 'query-disclosure__body query-disclosure__panel',
                contentClass: 'workbench-disclosure__fields',
                toggleHidden: !queryFeatureEnabled(context, 'query-options')
            }, h(__makeTemplateObject(["<div class=\"workbench-disclosure__field\">\n                    <label for=\"query-timeout\">Query timeout</label>\n                    <input id=\"query-timeout\" name=\"query-timeout\" type=\"number\" min=\"0\" step=\"1\"\n                        value=", " ?hidden=", " />\n                </div>\n                <div class=\"workbench-disclosure__field\">\n                    <label class=\"query-option\" for=\"infer\"><input id=\"infer\" name=\"infer\" type=\"checkbox\" value=\"true\"\n                        ?checked=", "\n                        ?hidden=", " />\n                        <span>Include inferred statements</span></label>\n                </div>\n                <div class=\"workbench-disclosure__actions query-disclosure__actions\"><input id=\"query-reset-namespaces\" type=\"button\" value=\"Clear\"\n                    data-editor-namespaces-enabled=", "\n                    ?hidden=", "\n                    @click=", " /></div>"], ["<div class=\"workbench-disclosure__field\">\n                    <label for=\"query-timeout\">Query timeout</label>\n                    <input id=\"query-timeout\" name=\"query-timeout\" type=\"number\" min=\"0\" step=\"1\"\n                        value=", " ?hidden=", " />\n                </div>\n                <div class=\"workbench-disclosure__field\">\n                    <label class=\"query-option\" for=\"infer\"><input id=\"infer\" name=\"infer\" type=\"checkbox\" value=\"true\"\n                        ?checked=", "\n                        ?hidden=", " />\n                        <span>Include inferred statements</span></label>\n                </div>\n                <div class=\"workbench-disclosure__actions query-disclosure__actions\"><input id=\"query-reset-namespaces\" type=\"button\" value=\"Clear\"\n                    data-editor-namespaces-enabled=", "\n                    ?hidden=", "\n                    @click=", " /></div>"]), defaultTimeout, !queryFeatureEnabled(context, 'query-timeout'), text(defaults['default-infer']) === 'true', !queryFeatureEnabled(context, 'query-inferred-statements'), queryFeatureEnabled(context, 'editor-namespaces') ? 'true' : 'false', !queryFeatureEnabled(context, 'editor-namespaces'), function () { return invoke('workbench.query.resetNamespaces'); }));
            return h(__makeTemplateObject(["<div id=\"query-page\" class=\"query-page\"\n                    data-editor-fullscreen-enabled=", ">\n                <form id=\"query-form\" action=\"query\" method=\"post\"\n                    data-workbench-query-execution=\"true\" data-workbench-results-target=\"query-results\">\n                    <input type=\"hidden\" name=\"action\" id=\"action\" />\n                    <input type=\"hidden\" name=\"explain\" id=\"explain\" />\n                    <input type=\"hidden\" name=\"ref\" value=\"text\" />\n                    <input type=\"hidden\" name=\"include-query-text\" id=\"include-query-text\" value=\"false\" />\n                    <input type=\"hidden\" name=\"query-request-id\" id=\"query-request-id\" value=\"\" />\n                    <div class=\"query-form\">\n                        <div id=\"query-language-row\" class=\"query-form__row\"\n                            ?hidden=", ">\n                            <label class=\"query-form__label\" for=\"queryLn\">Query language</label>\n                            <div class=\"query-form__field\"><select id=\"queryLn\" name=\"queryLn\"\n                                @change=", ">\n                                ", "\n                            </select></div>\n                        </div>\n                        <div id=\"query-compare-toolbar\" class=\"query-compare-toolbar\" ?hidden=", ">\n                            <span id=\"query-sidebar-toggle-action\" class=\"workbench-action workbench-action--secondary\"\n                                data-workbench-action=\"menu\"><span class=\"workbench-action-label\">\n                                <button id=\"query-sidebar-toggle\" class=\"query-sidebar-toggle\" type=\"button\"\n                                    aria-hidden=\"true\" aria-expanded=\"false\" aria-controls=\"navigation\" tabindex=\"-1\"\n                                    data-show-label=\"Show navigation\" data-hide-label=\"Hide navigation\"\n                                    ?hidden=", "\n                                    @click=", ">\n                                    <span id=\"query-sidebar-toggle-icon\" class=\"query-sidebar-toggle__icon\" aria-hidden=\"true\">\n                                        ", "\n                                    </span>\n                                </button></span></span>\n                            <button id=\"query-compare-copy\" class=\"workbench-action workbench-action--secondary\" type=\"button\"\n                                ?hidden=", ">Copy</button>\n                            <button id=\"query-compare-swap\" class=\"workbench-action workbench-action--secondary\" type=\"button\"\n                                ?hidden=", ">Swap</button>\n                            <div id=\"query-compare-controls\" class=\"query-compare-toolbar__actions\">\n                                <button id=\"explain-compare-trigger\" class=\"query-compare-action workbench-action workbench-action--secondary\" type=\"button\"\n                                    data-query-refresh-enabled=", "\n                                    ?hidden=", "\n                                    @click=", ">Refresh explanations</button>\n                                <button id=\"explain-compare-cancel\" class=\"query-compare-action query-explain-cancel workbench-action workbench-action--secondary\" type=\"button\" disabled\n                                    ?hidden=", ">\n                                    <span id=\"explain-compare-cancel-icon\" class=\"query-compare-action__svg--cancel\" aria-hidden=\"true\">\u00D7</span>Cancel</button>\n                                <button id=\"query-diff-trigger\" class=\"query-compare-action workbench-action workbench-action--secondary\" type=\"button\" disabled\n                                    ?hidden=", "\n                                    @click=", ">\n                                    <span id=\"query-diff-trigger-icon\" class=\"query-compare-action__icon\" aria-hidden=\"true\">\u21C4</span>Diff</button>\n                            </div>\n                        </div>\n                        <div id=\"query-compare-layout\" class=\"query-compare-layout\"\n                            ?hidden=", ">\n                            ", "\n                            ", "\n                        </div>\n                        <div class=\"query-actions-toolbar workbench-action-toolbar\"><div class=\"query-form__field query-actions-toolbar__primary workbench-action-toolbar__primary\">\n                            <button id=\"exec\" class=\"query-action workbench-action workbench-action--primary\" type=\"submit\"\n                                ?hidden=", ">", "<span>Execute</span></button>\n                            <input id=\"query-cancel\" class=\"query-cancel workbench-action workbench-action--secondary\" type=\"button\" value=\"Cancel\" aria-hidden=\"true\" disabled\n                                ?hidden=", "\n                                @click=", " />\n                            <button id=\"explain-trigger\" class=\"query-action workbench-action workbench-action--secondary\" type=\"button\"\n                                ?hidden=", "\n                                @click=", ">", "<span>Explain</span></button>\n                            <span id=\"explain-trigger-spinner\" class=\"query-explain-spinner\" aria-hidden=\"true\"></span>\n                            <span id=\"explain-trigger-cancel-action\" class=\"workbench-action workbench-action--danger-outline query-explain-cancel\"\n                                ?hidden=", "\n                                data-workbench-action=\"cancel\"><span id=\"explain-trigger-cancel-icon\" aria-hidden=\"true\"></span>\n                                <input id=\"explain-trigger-cancel\" type=\"button\" value=\"Cancel\" aria-hidden=\"true\" disabled\n                                    ?hidden=", "\n                                    @click=", " /></span>\n                        </div>\n                        <div class=\"workbench-action-toolbar__actions\">\n                            <div class=\"workbench-action-toolbar__group\">", "", "</div>\n                        </div>\n                        <div class=\"workbench-action-toolbar__panels workbench-disclosure-track\">\n                            ", "", "\n                        </div>\n                    </div>\n                    </div>\n                </form>\n                <section id=\"query-results\" class=\"query-results\" aria-busy=\"false\" hidden aria-labelledby=\"query-results-heading\">\n                    <div class=\"query-results__header workbench-action-toolbar\">\n                        <div class=\"workbench-action-toolbar__primary\"><h2 id=\"query-results-heading\">Query result</h2></div>\n                        <div class=\"workbench-action-toolbar__actions\">\n                        <button id=\"query-results-fullscreen\" class=\"query-results__fullscreen workbench-action workbench-action--secondary\" type=\"button\"\n                            aria-label=\"Full screen\" title=\"Full screen\" hidden aria-pressed=\"false\"\n                            data-result-fullscreen-enabled=", "\n                            @click=", ">\n                            <svg class=\"query-results__fullscreen-icon\" viewBox=\"0 0 24 24\" focusable=\"false\" aria-hidden=\"true\">\n                                <path d=\"M4 9V4h5M15 4h5v5M20 15v5h-5M9 20H4v-5\"></path>\n                            </svg><span class=\"query-results__fullscreen-label\">Full screen</span>\n                        </button>\n                        </div>\n                    </div>\n                    <div id=\"query-results-loading\" class=\"query-results__loading\" hidden role=\"status\" aria-live=\"polite\">Loading query results...</div>\n                    <div id=\"query-results-status\" class=\"query-results__status\" role=\"status\" aria-live=\"polite\"></div>\n                    <iframe id=\"query-results-frame\" name=\"query-results-frame\" class=\"query-results__frame\"\n                        title=\"Query results\" hidden></iframe>\n                </section>\n                <div id=\"query-diff-modal\" class=\"query-diff-modal\" aria-hidden=\"true\">\n                    <div class=\"query-diff-modal__dialog\" role=\"dialog\" aria-modal=\"true\" aria-labelledby=\"query-diff-modal-title\">\n                        <div class=\"query-diff-modal__header\"><div id=\"query-diff-modal-title\" class=\"query-diff-modal__title\">Query diff</div>\n                            <button id=\"query-diff-close\" class=\"workbench-action workbench-action--secondary\" type=\"button\" value=\"Close\"\n                                @click=", ">Close</button></div>\n                        <div class=\"query-diff-modal__body\">\n                            <section class=\"query-diff-section query-diff-section--query\"><div class=\"query-diff-section__title\">Query</div><div id=\"query-diff-query\" class=\"query-diff-view\"></div></section>\n                            <section class=\"query-diff-section query-diff-section--explanation\"><div class=\"query-diff-section__title\">Explanation</div><div id=\"query-diff-explanation\" class=\"query-diff-view\">Comparison is not ready.</div></section>\n                        </div>\n                    </div>\n                </div>\n            </div>"], ["<div id=\"query-page\" class=\"query-page\"\n                    data-editor-fullscreen-enabled=", ">\n                <form id=\"query-form\" action=\"query\" method=\"post\"\n                    data-workbench-query-execution=\"true\" data-workbench-results-target=\"query-results\">\n                    <input type=\"hidden\" name=\"action\" id=\"action\" />\n                    <input type=\"hidden\" name=\"explain\" id=\"explain\" />\n                    <input type=\"hidden\" name=\"ref\" value=\"text\" />\n                    <input type=\"hidden\" name=\"include-query-text\" id=\"include-query-text\" value=\"false\" />\n                    <input type=\"hidden\" name=\"query-request-id\" id=\"query-request-id\" value=\"\" />\n                    <div class=\"query-form\">\n                        <div id=\"query-language-row\" class=\"query-form__row\"\n                            ?hidden=", ">\n                            <label class=\"query-form__label\" for=\"queryLn\">Query language</label>\n                            <div class=\"query-form__field\"><select id=\"queryLn\" name=\"queryLn\"\n                                @change=", ">\n                                ", "\n                            </select></div>\n                        </div>\n                        <div id=\"query-compare-toolbar\" class=\"query-compare-toolbar\" ?hidden=", ">\n                            <span id=\"query-sidebar-toggle-action\" class=\"workbench-action workbench-action--secondary\"\n                                data-workbench-action=\"menu\"><span class=\"workbench-action-label\">\n                                <button id=\"query-sidebar-toggle\" class=\"query-sidebar-toggle\" type=\"button\"\n                                    aria-hidden=\"true\" aria-expanded=\"false\" aria-controls=\"navigation\" tabindex=\"-1\"\n                                    data-show-label=\"Show navigation\" data-hide-label=\"Hide navigation\"\n                                    ?hidden=", "\n                                    @click=", ">\n                                    <span id=\"query-sidebar-toggle-icon\" class=\"query-sidebar-toggle__icon\" aria-hidden=\"true\">\n                                        ", "\n                                    </span>\n                                </button></span></span>\n                            <button id=\"query-compare-copy\" class=\"workbench-action workbench-action--secondary\" type=\"button\"\n                                ?hidden=", ">Copy</button>\n                            <button id=\"query-compare-swap\" class=\"workbench-action workbench-action--secondary\" type=\"button\"\n                                ?hidden=", ">Swap</button>\n                            <div id=\"query-compare-controls\" class=\"query-compare-toolbar__actions\">\n                                <button id=\"explain-compare-trigger\" class=\"query-compare-action workbench-action workbench-action--secondary\" type=\"button\"\n                                    data-query-refresh-enabled=", "\n                                    ?hidden=", "\n                                    @click=", ">Refresh explanations</button>\n                                <button id=\"explain-compare-cancel\" class=\"query-compare-action query-explain-cancel workbench-action workbench-action--secondary\" type=\"button\" disabled\n                                    ?hidden=", ">\n                                    <span id=\"explain-compare-cancel-icon\" class=\"query-compare-action__svg--cancel\" aria-hidden=\"true\">\u00D7</span>Cancel</button>\n                                <button id=\"query-diff-trigger\" class=\"query-compare-action workbench-action workbench-action--secondary\" type=\"button\" disabled\n                                    ?hidden=", "\n                                    @click=", ">\n                                    <span id=\"query-diff-trigger-icon\" class=\"query-compare-action__icon\" aria-hidden=\"true\">\u21C4</span>Diff</button>\n                            </div>\n                        </div>\n                        <div id=\"query-compare-layout\" class=\"query-compare-layout\"\n                            ?hidden=", ">\n                            ", "\n                            ", "\n                        </div>\n                        <div class=\"query-actions-toolbar workbench-action-toolbar\"><div class=\"query-form__field query-actions-toolbar__primary workbench-action-toolbar__primary\">\n                            <button id=\"exec\" class=\"query-action workbench-action workbench-action--primary\" type=\"submit\"\n                                ?hidden=", ">", "<span>Execute</span></button>\n                            <input id=\"query-cancel\" class=\"query-cancel workbench-action workbench-action--secondary\" type=\"button\" value=\"Cancel\" aria-hidden=\"true\" disabled\n                                ?hidden=", "\n                                @click=", " />\n                            <button id=\"explain-trigger\" class=\"query-action workbench-action workbench-action--secondary\" type=\"button\"\n                                ?hidden=", "\n                                @click=", ">", "<span>Explain</span></button>\n                            <span id=\"explain-trigger-spinner\" class=\"query-explain-spinner\" aria-hidden=\"true\"></span>\n                            <span id=\"explain-trigger-cancel-action\" class=\"workbench-action workbench-action--danger-outline query-explain-cancel\"\n                                ?hidden=", "\n                                data-workbench-action=\"cancel\"><span id=\"explain-trigger-cancel-icon\" aria-hidden=\"true\"></span>\n                                <input id=\"explain-trigger-cancel\" type=\"button\" value=\"Cancel\" aria-hidden=\"true\" disabled\n                                    ?hidden=", "\n                                    @click=", " /></span>\n                        </div>\n                        <div class=\"workbench-action-toolbar__actions\">\n                            <div class=\"workbench-action-toolbar__group\">", "", "</div>\n                        </div>\n                        <div class=\"workbench-action-toolbar__panels workbench-disclosure-track\">\n                            ", "", "\n                        </div>\n                    </div>\n                    </div>\n                </form>\n                <section id=\"query-results\" class=\"query-results\" aria-busy=\"false\" hidden aria-labelledby=\"query-results-heading\">\n                    <div class=\"query-results__header workbench-action-toolbar\">\n                        <div class=\"workbench-action-toolbar__primary\"><h2 id=\"query-results-heading\">Query result</h2></div>\n                        <div class=\"workbench-action-toolbar__actions\">\n                        <button id=\"query-results-fullscreen\" class=\"query-results__fullscreen workbench-action workbench-action--secondary\" type=\"button\"\n                            aria-label=\"Full screen\" title=\"Full screen\" hidden aria-pressed=\"false\"\n                            data-result-fullscreen-enabled=", "\n                            @click=", ">\n                            <svg class=\"query-results__fullscreen-icon\" viewBox=\"0 0 24 24\" focusable=\"false\" aria-hidden=\"true\">\n                                <path d=\"M4 9V4h5M15 4h5v5M20 15v5h-5M9 20H4v-5\"></path>\n                            </svg><span class=\"query-results__fullscreen-label\">Full screen</span>\n                        </button>\n                        </div>\n                    </div>\n                    <div id=\"query-results-loading\" class=\"query-results__loading\" hidden role=\"status\" aria-live=\"polite\">Loading query results...</div>\n                    <div id=\"query-results-status\" class=\"query-results__status\" role=\"status\" aria-live=\"polite\"></div>\n                    <iframe id=\"query-results-frame\" name=\"query-results-frame\" class=\"query-results__frame\"\n                        title=\"Query results\" hidden></iframe>\n                </section>\n                <div id=\"query-diff-modal\" class=\"query-diff-modal\" aria-hidden=\"true\">\n                    <div class=\"query-diff-modal__dialog\" role=\"dialog\" aria-modal=\"true\" aria-labelledby=\"query-diff-modal-title\">\n                        <div class=\"query-diff-modal__header\"><div id=\"query-diff-modal-title\" class=\"query-diff-modal__title\">Query diff</div>\n                            <button id=\"query-diff-close\" class=\"workbench-action workbench-action--secondary\" type=\"button\" value=\"Close\"\n                                @click=", ">Close</button></div>\n                        <div class=\"query-diff-modal__body\">\n                            <section class=\"query-diff-section query-diff-section--query\"><div class=\"query-diff-section__title\">Query</div><div id=\"query-diff-query\" class=\"query-diff-view\"></div></section>\n                            <section class=\"query-diff-section query-diff-section--explanation\"><div class=\"query-diff-section__title\">Explanation</div><div id=\"query-diff-explanation\" class=\"query-diff-view\">Comparison is not ready.</div></section>\n                        </div>\n                    </div>\n                </div>\n            </div>"]), queryFeatureEnabled(context, 'editor-fullscreen') ? 'true' : 'false', hideQueryLanguageRow || !queryFeatureEnabled(context, 'query-language'), function () { return invoke('workbench.query.onQlChange'); }, queryFormats.length ? queryFormats.map(function (option) { return h(__makeTemplateObject(["<option value=", "\n                                    ?selected=", ">", "</option>"], ["<option value=", "\n                                    ?selected=", ">", "</option>"]), option.value, option.value === selectedQueryLanguage, option.label); })
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
        var repositoryScopedViews = {
            summary: true, namespaces: true, contexts: true, types: true, explore: true, query: true,
            'saved-queries': true, export: true, update: true, add: true, remove: true, clear: true
        };
        /** Where choosing a repository in the switcher leads: the same view when it is repository-scoped. */
        function repositorySwitchTarget(basePath, activeView, repositoryId) {
            var view = repositoryScopedViews[activeView] ? activeView : 'summary';
            return (basePath || '').replace(/\/+$/, '') + '/repositories/' + encodeURIComponent(repositoryId) + '/' + view;
        }
        views.repositorySwitchTarget = repositorySwitchTarget;
        function repositoryOptions(panel) {
            var list = panel.querySelector('#workbench-repository-options');
            var options = [];
            var links = list ? list.querySelectorAll('a.workbench-popover__option') : [];
            for (var index = 0; index < links.length; index++) {
                if (!links[index].parentElement.hidden) {
                    options.push(links[index]);
                }
            }
            return options;
        }
        function renderRepositoryOptions(panel, repositories, currentId) {
            var document = panel.ownerDocument;
            var list = panel.querySelector('#workbench-repository-options');
            var basePath = panel.getAttribute('data-workbench-base-path') || '';
            var activeView = panel.getAttribute('data-workbench-active-view') || '';
            while (list.firstChild) {
                list.removeChild(list.firstChild);
            }
            repositories.forEach(function (repository) {
                var item = document.createElement('li');
                var link = document.createElement('a');
                link.className = 'workbench-popover__option';
                link.setAttribute('href', repositorySwitchTarget(basePath, activeView, repository.id));
                link.setAttribute('data-repository-id', repository.id);
                link.setAttribute('data-repository-search', (repository.id + ' ' + repository.title).toLowerCase());
                if (repository.id === currentId) {
                    link.setAttribute('aria-current', 'true');
                }
                var id = document.createElement('span');
                id.className = 'workbench-popover__option-id';
                id.textContent = repository.id;
                link.appendChild(id);
                if (repository.title) {
                    var title = document.createElement('span');
                    title.className = 'workbench-popover__option-title';
                    title.textContent = repository.title;
                    link.appendChild(title);
                }
                item.appendChild(link);
                list.appendChild(item);
            });
            filterRepositoryOptions(panel);
        }
        function filterRepositoryOptions(panel) {
            var input = panel.querySelector('#workbench-repository-filter');
            var needle = input ? String(input.value || '').trim().toLowerCase() : '';
            var links = panel.querySelectorAll('a.workbench-popover__option');
            var visible = 0;
            for (var index = 0; index < links.length; index++) {
                var matches = !needle || links[index].getAttribute('data-repository-search').indexOf(needle) >= 0;
                links[index].parentElement.hidden = !matches;
                if (matches) {
                    visible++;
                }
            }
            var status = panel.querySelector('.workbench-popover__status');
            if (status && panel.__rdf4jRepositoriesLoaded) {
                status.textContent = visible ? '' : (links.length ? 'No matching repositories.' : 'No repositories are available.');
            }
        }
        function loadRepositoryOptions(panel, currentId) {
            if (panel.__rdf4jRepositoriesLoading) {
                return panel.__rdf4jRepositoriesLoading;
            }
            var app = workbench.app;
            var status = panel.querySelector('.workbench-popover__status');
            if (!app || typeof app.loadModel !== 'function' || typeof window === 'undefined') {
                return Promise.resolve();
            }
            if (status) {
                status.textContent = 'Loading repositories…';
            }
            var url = new URL(panel.getAttribute('data-workbench-repositories-url'), window.location.href).toString();
            var loading = app.loadModel(window.fetch.bind(window), url).then(function (model) {
                return model.rowStore.read(0, model.rowCount).then(function (rows) {
                    model.rowStore.dispose();
                    return recordsFromRows(model, rows).map(function (record) { return ({
                        id: text(record.id), title: text(record.description)
                    }); }).filter(function (repository) { return !!repository.id; });
                }, function (error) {
                    model.rowStore.dispose();
                    throw error;
                });
            }).then(function (repositories) {
                panel.__rdf4jRepositoriesLoaded = true;
                renderRepositoryOptions(panel, repositories, currentId);
            }, function (error) {
                panel.__rdf4jRepositoriesLoading = null;
                if (status) {
                    status.textContent = 'Unable to load repositories: ' + (error && error.message ? error.message : String(error));
                }
            });
            panel.__rdf4jRepositoriesLoading = loading;
            return loading;
        }
        function moveRepositoryFocus(panel, from, delta) {
            var options = repositoryOptions(panel);
            var input = panel.querySelector('#workbench-repository-filter');
            var index = options.indexOf(from);
            var next = index + delta;
            if (next < 0) {
                if (input) {
                    input.focus();
                }
            }
            else if (next < options.length) {
                options[next].focus();
            }
        }
        /**
         * Bind the context bar switchers once per shell: popovers (workbench.popover from template.ts),
         * the lazily loaded repository list, its filter and arrow-key movement.
         */
        function bindContextBar(appMount, context) {
            var document = appMount && appMount.ownerDocument;
            var popover = workbench.popover;
            if (!document || !document.getElementById || !popover || typeof popover.bind !== 'function') {
                return function () { };
            }
            var disposers = [];
            var currentId = contextBarState(context).repositoryId;
            ['server', 'repository', 'user'].forEach(function (name) {
                var button = document.getElementById('workbench-' + name + '-switcher');
                var panel = document.getElementById('workbench-' + name + '-popover');
                disposers.push(popover.bind(button, panel, name === 'repository'
                    ? { onOpen: function (opened) { loadRepositoryOptions(opened, currentId); } } : {}));
            });
            var repositoryPanel = document.getElementById('workbench-repository-popover');
            var filter = document.getElementById('workbench-repository-filter');
            if (repositoryPanel && filter && !repositoryPanel.__rdf4jContextBarBound) {
                repositoryPanel.__rdf4jContextBarBound = true;
                var onInput_1 = function () { return filterRepositoryOptions(repositoryPanel); };
                var onKey_1 = function (event) {
                    var target = event.target;
                    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
                        event.preventDefault();
                        if (target === filter) {
                            if (event.key === 'ArrowDown') {
                                var options = repositoryOptions(repositoryPanel);
                                if (options.length) {
                                    options[0].focus();
                                }
                            }
                        }
                        else if (target && target.classList && target.classList.contains('workbench-popover__option')) {
                            moveRepositoryFocus(repositoryPanel, target, event.key === 'ArrowDown' ? 1 : -1);
                        }
                    }
                    else if (event.key === 'Enter' && target === filter) {
                        var options = repositoryOptions(repositoryPanel);
                        if (options.length === 1) {
                            event.preventDefault();
                            options[0].click();
                        }
                    }
                };
                filter.addEventListener('input', onInput_1);
                repositoryPanel.addEventListener('keydown', onKey_1);
                disposers.push(function () {
                    filter.removeEventListener('input', onInput_1);
                    repositoryPanel.removeEventListener('keydown', onKey_1);
                    repositoryPanel.__rdf4jContextBarBound = false;
                });
            }
            return function () { return disposers.forEach(function (dispose) { return dispose(); }); };
        }
        views.bindContextBar = bindContextBar;
        var outletsByMount = new WeakMap();
        var outletElements = new WeakSet();
        function outletNodeFor(appMount) {
            var existing = outletsByMount.get(appMount);
            if (existing) {
                return existing;
            }
            var document = appMount && appMount.ownerDocument;
            if (!document || typeof document.createElement !== 'function') {
                return null;
            }
            var outlet = document.createElement('div');
            outlet.id = 'workbench-outlet';
            outlet.className = 'workbench-outlet';
            outlet.setAttribute('tabindex', '-1');
            outletsByMount.set(appMount, outlet);
            outletElements.add(outlet);
            return outlet;
        }
        /** The outlet element that renderShell placed inside an application mount, if any. */
        function outletOf(appMount) {
            return appMount ? outletsByMount.get(appMount) || null : null;
        }
        views.outletOf = outletOf;
        function prepareRowRegions(target, model) {
            var regions = rowRegionsByMount.get(target);
            if (model.rowStore && target.ownerDocument && target.ownerDocument.createElement) {
                if (!regions || regions.model !== model) {
                    regions = { model: model, document: target.ownerDocument, savedCards: {}, groups: {} };
                    rowRegionsByMount.set(target, regions);
                }
                return regions;
            }
            rowRegionsByMount.delete(target);
            return null;
        }
        function renderRowRegions(regions) {
            if (regions) {
                if (regions.renderTableRows) {
                    regions.renderTableRows();
                }
                if (regions.renderSavedRows) {
                    regions.renderSavedRows();
                }
                Object.keys(regions.groups).forEach(function (key) { return regions.groups[key].render(); });
            }
        }
        /**
         * Render the persistent shell (header, menu, footer) into the application mount and return the
         * outlet element that renderOutlet fills. Returns null when the mount has no DOM document.
         */
        function renderShell(appMount, shellState, runtime) {
            var outlet = outletNodeFor(appMount);
            if (!outlet) {
                return null;
            }
            runtime.render(shellTemplate(shellState, runtime, outlet), appMount);
            return outlet;
        }
        views.renderShell = renderShell;
        /** Render a route's title and page surface into the outlet. */
        function renderOutlet(outletMount, model, context, runtime) {
            var regions = prepareRowRegions(outletMount, model);
            var renderedContext = regions ? __assign(__assign({}, context), { rowRegions: regions }) : context;
            runtime.render(outletContentTemplate(model, runtime, routeBody(model, renderedContext, runtime)), outletMount);
            renderRowRegions(regions);
            return outletMount;
        }
        views.renderOutlet = renderOutlet;
        /** Render a complete route: the shell into the mount and the route into its outlet. */
        function render(mount, model, context, runtime) {
            if (outletElements.has(mount)) {
                renderOutlet(mount, model, context, runtime);
                return mount;
            }
            var outlet = renderShell(mount, { viewId: model.viewId, context: context }, runtime);
            if (outlet) {
                renderOutlet(outlet, model, context, runtime);
                return mount;
            }
            // Without a DOM document (unit-test fakes) the complete page renders as one template.
            var regions = prepareRowRegions(mount, model);
            var renderedContext = regions ? __assign(__assign({}, context), { rowRegions: regions }) : context;
            runtime.render(pageTemplate(model, renderedContext, runtime), mount);
            renderRowRegions(regions);
            return mount;
        }
        views.render = render;
        /** Keep streamed route rows in the worker store and render only viewport windows. */
        function bindRowWindows(mount, model, context, runtime) {
            var targetWindow = typeof window !== 'undefined' ? window : null;
            var stream = workbench.queryStream;
            var HeightIndex = stream && stream.MeasuredRowHeights;
            // Row regions belong to the outlet when the mount is an application mount with a shell.
            var regionKey = outletOf(mount) || mount;
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
                            var regions = rowRegionsByMount.get(regionKey);
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
                var regions = rowRegionsByMount.get(regionKey);
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
                    var regions = rowRegionsByMount.get(regionKey);
                    if (regions && regions.model === model) {
                        rowRegionsByMount.delete(regionKey);
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