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
        search: 'M10.5 4a6.5 6.5 0 1 0 0 13 6.5 6.5 0 0 0 0-13ZM16 16l5 5',
        sliders: 'M4 7h9m4 0h3M17 7a2 2 0 1 1-4 0 2 2 0 0 1 4 0M4 17h3m4 0h9M11 17a2 2 0 1 1-4 0 2 2 0 0 1 4 0',
        more: 'M6 12h.01M12 12h.01M18 12h.01',
        check: 'm5 12 5 5 9-10',
        'warning-sign': 'M12 4 2.8 20h18.4L12 4Zm0 6v4.5m0 2.5h.01'
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
        /** Short page names for the document title; keep in sync with SHORT_TITLES in WorkbenchHtmlShell.java. */
        var shortTitles = {
            summary: 'Summary', information: 'Information', repositories: 'Repositories',
            create: 'Create repository', delete: 'Delete repository', namespaces: 'Namespaces', contexts: 'Graphs',
            types: 'Types', explore: 'Explore', query: 'Query', 'saved-queries': 'Saved queries', export: 'Export',
            add: 'Add RDF', remove: 'Remove', clear: 'Clear', update: 'SPARQL Update', server: 'Connection'
        };
        /** "Page · repository — RDF4J Workbench", matching the title the server writes into the shell. */
        function documentTitle(viewId, repositoryId) {
            var page = shortTitles[viewId];
            if (!page) {
                return 'RDF4J Workbench';
            }
            var hasRepository = !!repositoryId && repositoryId !== 'NONE';
            return page + (hasRepository ? ' · ' + repositoryId : '') + ' — RDF4J Workbench';
        }
        views.documentTitle = documentTitle;
        var titles = {
            summary: 'Summary',
            information: 'System Information',
            repositories: 'List of Repositories',
            create: 'New Repository',
            delete: 'Delete Repository',
            namespaces: 'Namespaces In Repository',
            contexts: 'Graphs',
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
        function isRepositoryNotFound(model) {
            return !!model.error && model.error.code === 'repository-not-found';
        }
        function routeTitle(model) {
            if (isRepositoryNotFound(model)) {
                return 'Repository not found';
            }
            return text(meta(model, 'title')) || titles[model.viewId] || 'RDF4J Workbench';
        }
        /** In-shell page for an unknown repository (mockup 13). */
        function repositoryNotFoundPage(runtime, context) {
            var h = runtime.html;
            var server = contextBarState(context).server;
            var id = context.missingRepositoryId || '';
            return h(__makeTemplateObject(["<section id=\"repository-not-found\" class=\"workbench-island workbench-not-found\" aria-labelledby=\"repository-not-found-title\">\n                ", "\n                <h2 id=\"repository-not-found-title\">Repository not found</h2>\n                <p>The server", " has no repository with id\n                    <code>", "</code>. It may have been deleted or renamed.</p>\n                <div class=\"workbench-form-actions workbench-not-found__actions\">\n                    <a class=\"workbench-action workbench-action--primary\" href=", ">Go to repositories</a>\n                    <a class=\"workbench-action workbench-action--secondary\" href=", ">Change server</a>\n                </div>\n            </section>"], ["<section id=\"repository-not-found\" class=\"workbench-island workbench-not-found\" aria-labelledby=\"repository-not-found-title\">\n                ", "\n                <h2 id=\"repository-not-found-title\">Repository not found</h2>\n                <p>The server", " has no repository with id\n                    <code>", "</code>. It may have been deleted or renamed.</p>\n                <div class=\"workbench-form-actions workbench-not-found__actions\">\n                    <a class=\"workbench-action workbench-action--primary\" href=", ">Go to repositories</a>\n                    <a class=\"workbench-action workbench-action--secondary\" href=", ">Change server</a>\n                </div>\n            </section>"]), icon(runtime, 'repository', 'workbench-not-found__icon'), server ? h(__makeTemplateObject([" at <code>", "</code>"], [" at <code>", "</code>"]), hostAndPort(server)) : '', id, urlFor(context, 'repositories'), urlFor(context, 'server'));
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
        /**
         * Every group renders the same way: a non-interactive label followed by its items (M2.4). The sidebar
         * uses the default id prefix; the mobile menu sheet renders the same menu with its own prefix.
         */
        function navigation(context, active, runtime, idPrefix, groups) {
            if (idPrefix === void 0) { idPrefix = 'workbench-nav'; }
            var h = runtime.html;
            var info = normalizeWorkbench(context.workbench, context.linked && context.linked.info);
            return (groups || menuEntries(context)).filter(function (group) { return (group.items || []).length > 0; }).map(function (group) {
                var items = group.items || [];
                var groupId = text(group.id || group['menu-group-id'] || 'workbench');
                var groupLabel = text(group.label || group['menu-group-label'] || 'Workbench');
                var list = h(__makeTemplateObject(["<ul id=", " class=\"group\" aria-labelledby=", ">\n                    ", "\n                </ul>"], ["<ul id=", " class=\"group\" aria-labelledby=", ">\n                    ", "\n                </ul>"]), idPrefix + '-items-' + groupId, idPrefix + '-label-' + groupId, items.map(function (item) { return navigationItem(runtime, context, info, item, groupLabel, active); }));
                if (items.length > navigationDisclosureThreshold) {
                    var containsActive = items.some(function (item) { return text(item.id || item['menu-item-id']) === active; });
                    return h(__makeTemplateObject(["<li class=\"workbench-nav-group workbench-nav-group--long\" data-workbench-menu-group=", "\n                            data-workbench-menu-label=", ">\n                        <details class=\"workbench-nav-group__disclosure\" ?open=", ">\n                            <summary id=", " aria-controls=", "\n                                    class=\"workbench-nav-group__summary\">\n                                <span id=", " class=\"workbench-nav-group__label\">", "</span>\n                                ", "\n                            </summary>\n                            ", "\n                        </details>\n                    </li>"], ["<li class=\"workbench-nav-group workbench-nav-group--long\" data-workbench-menu-group=", "\n                            data-workbench-menu-label=", ">\n                        <details class=\"workbench-nav-group__disclosure\" ?open=", ">\n                            <summary id=", " aria-controls=", "\n                                    class=\"workbench-nav-group__summary\">\n                                <span id=", " class=\"workbench-nav-group__label\">", "</span>\n                                ", "\n                            </summary>\n                            ", "\n                        </details>\n                    </li>"]), groupId, groupLabel, containsActive, idPrefix + '-summary-' + groupId, idPrefix + '-items-' + groupId, idPrefix + '-label-' + groupId, groupLabel, icon(runtime, 'chevron', 'workbench-nav-group__chevron workbench-disclosure-chevron'), list);
                }
                return h(__makeTemplateObject(["<li class=\"workbench-nav-group\" data-workbench-menu-group=", "\n                        data-workbench-menu-label=", ">\n                    <span id=", " class=\"workbench-nav-group__label\">", "</span>\n                    ", "\n                </li>"], ["<li class=\"workbench-nav-group\" data-workbench-menu-group=", "\n                        data-workbench-menu-label=", ">\n                    <span id=", " class=\"workbench-nav-group__label\">", "</span>\n                    ", "\n                </li>"]), groupId, groupLabel, idPrefix + '-label-' + groupId, groupLabel, list);
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
            return h(__makeTemplateObject(["<header id=\"workbench-contextbar\" class=\"workbench-contextbar\">\n                <a id=\"logo\" class=\"workbench-brand\" href=", " aria-label=\"RDF4J Workbench home\">\n                    <img class=\"workbench-brand__light\" src=", " alt=\"rdf4j\" />\n                    <img class=\"product workbench-brand__light\" src=", " alt=\"workbench\" />\n                    <img class=\"workbench-brand__dark\" src=", " alt=\"rdf4j\" />\n                    <img class=\"product workbench-brand__dark\" src=", " alt=\"workbench\" />\n                </a>\n                <div class=\"workbench-switcher\" data-workbench-switcher=\"server\">\n                    <button id=\"workbench-server-switcher\" class=\"workbench-switcher__button\" type=\"button\"\n                            aria-haspopup=\"dialog\" aria-expanded=\"false\" aria-controls=\"workbench-server-popover\"\n                            title=", ">\n                        ", "<span class=\"workbench-switcher__key\">Server</span>\n                        <span class=\"workbench-switcher__value\">", "</span>\n                        ", "\n                    </button>\n                    <div id=\"workbench-server-popover\" class=\"workbench-popover\" role=\"dialog\" aria-label=\"Server\" hidden>\n                        <dl class=\"workbench-kv\">\n                            <div class=\"workbench-kv__row\"><dt>Server</dt><dd class=\"workbench-kv__code\">", "</dd></div>\n                            <div class=\"workbench-kv__row\"><dt>User</dt><dd>", "</dd></div>\n                        </dl>\n                        <div class=\"workbench-popover__footer\">\n                            <a href=", ">", "Change server or user\u2026</a>\n                        </div>\n                    </div>\n                </div>\n                <span class=\"workbench-contextbar__divider\" aria-hidden=\"true\"></span>\n                <div class=\"workbench-switcher\" data-workbench-switcher=\"repository\">\n                    <button id=\"workbench-repository-switcher\" class=\"workbench-switcher__button\" type=\"button\"\n                            aria-haspopup=\"dialog\" aria-expanded=\"false\" aria-controls=\"workbench-repository-popover\">\n                        ", "<span class=\"workbench-switcher__key\">Repository</span>\n                        ", "\n                        ", "\n                    </button>\n                    <div id=\"workbench-repository-popover\" class=\"workbench-popover workbench-popover--repositories\"\n                            role=\"dialog\" aria-label=\"Choose repository\" hidden\n                            data-workbench-active-view=", " data-workbench-repositories-url=", "\n                            data-workbench-base-path=", ">\n                        <div class=\"workbench-popover__search\">", "\n                            <input id=\"workbench-repository-filter\" type=\"search\" placeholder=\"Find repository\"\n                                aria-label=\"Find repository\" autocomplete=\"off\" aria-controls=\"workbench-repository-options\" />\n                        </div>\n                        <ul id=\"workbench-repository-options\" class=\"workbench-popover__list\" aria-label=\"Repositories\"></ul>\n                        <p class=\"workbench-popover__status\" role=\"status\" aria-live=\"polite\"></p>\n                        <div class=\"workbench-popover__footer\">\n                            <a href=", ">", "All repositories</a>\n                            <a href=", ">", "Create repository</a>\n                        </div>\n                    </div>\n                </div>\n                <span class=\"workbench-contextbar__spacer\"></span>\n                <div class=\"workbench-switcher\" data-workbench-switcher=\"user\">\n                    <button id=\"workbench-user-switcher\" class=\"workbench-switcher__button workbench-switcher__button--user\"\n                            type=\"button\" aria-haspopup=\"dialog\" aria-expanded=\"false\" aria-controls=\"workbench-user-popover\">\n                        ", "<span id=\"selected-user\" class=\"workbench-switcher__value\">", "</span>\n                        ", "\n                    </button>\n                    <div id=\"workbench-user-popover\" class=\"workbench-popover workbench-popover--end\" role=\"dialog\" aria-label=\"User\" hidden>\n                        <p class=\"workbench-popover__text\">", "</p>\n                        <div class=\"workbench-popover__footer\">\n                            <a href=", ">", "Change server or user\u2026</a>\n                        </div>\n                    </div>\n                </div>\n                <button id=\"workbench-menu-button\" class=\"workbench-action workbench-action--ghost workbench-action--icon workbench-menu-button\"\n                        type=\"button\" aria-haspopup=\"dialog\" aria-controls=\"workbench-menu-sheet\"\n                        aria-label=\"Menu\" title=\"Menu\">", "</button>\n            </header>"], ["<header id=\"workbench-contextbar\" class=\"workbench-contextbar\">\n                <a id=\"logo\" class=\"workbench-brand\" href=", " aria-label=\"RDF4J Workbench home\">\n                    <img class=\"workbench-brand__light\" src=", " alt=\"rdf4j\" />\n                    <img class=\"product workbench-brand__light\" src=", " alt=\"workbench\" />\n                    <img class=\"workbench-brand__dark\" src=", " alt=\"rdf4j\" />\n                    <img class=\"product workbench-brand__dark\" src=", " alt=\"workbench\" />\n                </a>\n                <div class=\"workbench-switcher\" data-workbench-switcher=\"server\">\n                    <button id=\"workbench-server-switcher\" class=\"workbench-switcher__button\" type=\"button\"\n                            aria-haspopup=\"dialog\" aria-expanded=\"false\" aria-controls=\"workbench-server-popover\"\n                            title=", ">\n                        ", "<span class=\"workbench-switcher__key\">Server</span>\n                        <span class=\"workbench-switcher__value\">", "</span>\n                        ", "\n                    </button>\n                    <div id=\"workbench-server-popover\" class=\"workbench-popover\" role=\"dialog\" aria-label=\"Server\" hidden>\n                        <dl class=\"workbench-kv\">\n                            <div class=\"workbench-kv__row\"><dt>Server</dt><dd class=\"workbench-kv__code\">", "</dd></div>\n                            <div class=\"workbench-kv__row\"><dt>User</dt><dd>", "</dd></div>\n                        </dl>\n                        <div class=\"workbench-popover__footer\">\n                            <a href=", ">", "Change server or user\u2026</a>\n                        </div>\n                    </div>\n                </div>\n                <span class=\"workbench-contextbar__divider\" aria-hidden=\"true\"></span>\n                <div class=\"workbench-switcher\" data-workbench-switcher=\"repository\">\n                    <button id=\"workbench-repository-switcher\" class=\"workbench-switcher__button\" type=\"button\"\n                            aria-haspopup=\"dialog\" aria-expanded=\"false\" aria-controls=\"workbench-repository-popover\">\n                        ", "<span class=\"workbench-switcher__key\">Repository</span>\n                        ", "\n                        ", "\n                    </button>\n                    <div id=\"workbench-repository-popover\" class=\"workbench-popover workbench-popover--repositories\"\n                            role=\"dialog\" aria-label=\"Choose repository\" hidden\n                            data-workbench-active-view=", " data-workbench-repositories-url=", "\n                            data-workbench-base-path=", ">\n                        <div class=\"workbench-popover__search\">", "\n                            <input id=\"workbench-repository-filter\" type=\"search\" placeholder=\"Find repository\"\n                                aria-label=\"Find repository\" autocomplete=\"off\" aria-controls=\"workbench-repository-options\" />\n                        </div>\n                        <ul id=\"workbench-repository-options\" class=\"workbench-popover__list\" aria-label=\"Repositories\"></ul>\n                        <p class=\"workbench-popover__status\" role=\"status\" aria-live=\"polite\"></p>\n                        <div class=\"workbench-popover__footer\">\n                            <a href=", ">", "All repositories</a>\n                            <a href=", ">", "Create repository</a>\n                        </div>\n                    </div>\n                </div>\n                <span class=\"workbench-contextbar__spacer\"></span>\n                <div class=\"workbench-switcher\" data-workbench-switcher=\"user\">\n                    <button id=\"workbench-user-switcher\" class=\"workbench-switcher__button workbench-switcher__button--user\"\n                            type=\"button\" aria-haspopup=\"dialog\" aria-expanded=\"false\" aria-controls=\"workbench-user-popover\">\n                        ", "<span id=\"selected-user\" class=\"workbench-switcher__value\">", "</span>\n                        ", "\n                    </button>\n                    <div id=\"workbench-user-popover\" class=\"workbench-popover workbench-popover--end\" role=\"dialog\" aria-label=\"User\" hidden>\n                        <p class=\"workbench-popover__text\">", "</p>\n                        <div class=\"workbench-popover__footer\">\n                            <a href=", ">", "Change server or user\u2026</a>\n                        </div>\n                    </div>\n                </div>\n                <button id=\"workbench-menu-button\" class=\"workbench-action workbench-action--ghost workbench-action--icon workbench-menu-button\"\n                        type=\"button\" aria-haspopup=\"dialog\" aria-controls=\"workbench-menu-sheet\"\n                        aria-label=\"Menu\" title=\"Menu\">", "</button>\n            </header>"]), urlFor(context, 'repositories'), context.basePath + '/images/logo.png', context.basePath + '/images/product.png', context.basePath + '/images/logo-dark.png', context.basePath + '/images/product-dark.png', server || 'No server', icon(runtime, 'server'), server ? hostAndPort(server) : 'None', switcherChevron(runtime), server || 'None', state.user || 'Not signed in', urlFor(context, 'server'), icon(runtime, 'settings'), icon(runtime, 'repository'), state.repositoryId
                ? h(__makeTemplateObject(["<span class=\"workbench-switcher__value\"><span class=\"workbench-switcher__id\">", "</span>", "</span>"], ["<span class=\"workbench-switcher__value\"><span class=\"workbench-switcher__id\">", "</span>", "</span>"]), state.repositoryId, state.repositoryTitle
                    ? h(__makeTemplateObject(["<span class=\"workbench-switcher__title\">", "</span>"], ["<span class=\"workbench-switcher__title\">", "</span>"]), state.repositoryTitle) : '') : h(__makeTemplateObject(["<span class=\"workbench-switcher__value workbench-switcher__value--empty\">No repository</span>"], ["<span class=\"workbench-switcher__value workbench-switcher__value--empty\">No repository</span>"])), switcherChevron(runtime), active, urlFor(context, 'repositories'), (context.basePath || '').replace(/\/+$/, ''), icon(runtime, 'search'), urlFor(context, 'repositories'), icon(runtime, 'repository'), urlFor(context, 'create'), icon(runtime, 'create'), icon(runtime, 'user'), state.user || 'Not signed in', switcherChevron(runtime), state.user ? h(__makeTemplateObject(["Signed in as <strong>", "</strong>"], ["Signed in as <strong>", "</strong>"]), state.user) : 'Not signed in', urlFor(context, 'server'), icon(runtime, 'settings'), icon(runtime, 'menu'));
        }
        /** Full-height menu sheet for narrow screens (M2.8, mockup 14); a native modal dialog. */
        function menuSheet(context, active, runtime, groups) {
            var h = runtime.html;
            var state = contextBarState(context);
            return h(__makeTemplateObject(["<dialog id=\"workbench-menu-sheet\" class=\"workbench-menu-sheet\" aria-label=\"Menu\">\n                <div class=\"workbench-menu-sheet__header\">\n                    <span class=\"workbench-menu-sheet__brand workbench-brand\">\n                        <img class=\"workbench-brand__light\" src=", " alt=\"rdf4j\" />\n                        <img class=\"workbench-brand__dark\" src=", " alt=\"rdf4j\" />\n                    </span>\n                    <span class=\"workbench-menu-sheet__repository\">", "</span>\n                    <button id=\"workbench-menu-close\" class=\"workbench-action workbench-action--secondary workbench-action--icon\"\n                            type=\"button\" aria-label=\"Close menu\" title=\"Close menu\">", "</button>\n                </div>\n                <p class=\"workbench-menu-sheet__context\">Server ", " \u00B7 ", "</p>\n                <nav id=\"workbench-menu-sheet-nav\" class=\"workbench-nav workbench-menu-sheet__nav\" aria-label=\"Workbench menu\">\n                    <ul class=\"maingroup\">", "</ul>\n                </nav>\n            </dialog>"], ["<dialog id=\"workbench-menu-sheet\" class=\"workbench-menu-sheet\" aria-label=\"Menu\">\n                <div class=\"workbench-menu-sheet__header\">\n                    <span class=\"workbench-menu-sheet__brand workbench-brand\">\n                        <img class=\"workbench-brand__light\" src=", " alt=\"rdf4j\" />\n                        <img class=\"workbench-brand__dark\" src=", " alt=\"rdf4j\" />\n                    </span>\n                    <span class=\"workbench-menu-sheet__repository\">", "</span>\n                    <button id=\"workbench-menu-close\" class=\"workbench-action workbench-action--secondary workbench-action--icon\"\n                            type=\"button\" aria-label=\"Close menu\" title=\"Close menu\">", "</button>\n                </div>\n                <p class=\"workbench-menu-sheet__context\">Server ", " \u00B7 ", "</p>\n                <nav id=\"workbench-menu-sheet-nav\" class=\"workbench-nav workbench-menu-sheet__nav\" aria-label=\"Workbench menu\">\n                    <ul class=\"maingroup\">", "</ul>\n                </nav>\n            </dialog>"]), context.basePath + '/images/logo.png', context.basePath + '/images/logo-dark.png', state.repositoryId || 'No repository', icon(runtime, 'close'), state.server ? hostAndPort(state.server) : 'None', state.user || 'Not signed in', navigation(context, active, runtime, 'workbench-sheet-nav', groups));
        }
        /** Persistent shell around the outlet; `outlet` is the outlet node or, without a DOM, its template. */
        function shellTemplate(state, runtime, outlet) {
            var h = runtime.html;
            var context = state.context;
            var groups = menuEntries(context);
            return h(__makeTemplateObject(["", "\n            <nav id=\"workbench-navigation-disclosure\" class=\"workbench-navigation-disclosure\" aria-label=\"Workbench menu\">\n                <div id=\"navigation\" class=\"workbench-nav\"><ul class=\"maingroup\">\n                    ", "\n                </ul></div>\n            </nav>\n            ", "\n            <main id=\"content\" class=\"workbench-main\">", "</main>\n            <div id=\"footer\" class=\"workbench-footer\"><div>Copyright \u00A9 Eclipse RDF4J contributors</div></div>"], ["", "\n            <nav id=\"workbench-navigation-disclosure\" class=\"workbench-navigation-disclosure\" aria-label=\"Workbench menu\">\n                <div id=\"navigation\" class=\"workbench-nav\"><ul class=\"maingroup\">\n                    ", "\n                </ul></div>\n            </nav>\n            ", "\n            <main id=\"content\" class=\"workbench-main\">", "</main>\n            <div id=\"footer\" class=\"workbench-footer\"><div>Copyright \u00A9 Eclipse RDF4J contributors</div></div>"]), contextBar(context, state.viewId, runtime), navigation(context, state.viewId, runtime, 'workbench-nav', groups), menuSheet(context, state.viewId, runtime, groups), outlet);
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
            var columns = options && options.columns || model.vars || [];
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
            return h(__makeTemplateObject(["<table class=\"data\" data-workbench-row-table=", ">\n                ", "\n                ", "\n            </table>"], ["<table class=\"data\" data-workbench-row-table=", ">\n                ", "\n                ", "\n            </table>"]), model.rowStore && total ? 'true' : runtime.nothing, columns.length ? h(__makeTemplateObject(["<thead><tr>", "</tr></thead>"], ["<thead><tr>", "</tr></thead>"]), columns.map(function (name) { return options && options.header
                ? options.header(name) : h(__makeTemplateObject(["<th scope=\"col\">", "</th>"], ["<th scope=\"col\">", "</th>"]), columnLabel(name, options)); })) : '', regions ? regions.tableBody
                : h(__makeTemplateObject(["<tbody>", "</tbody>"], ["<tbody>", "</tbody>"]), tableRows(runtime, model, context, options, allRows, rowStart(model), total, emptyText)));
        }
        function tableRows(runtime, model, context, options, allRows, start, total, emptyText) {
            var h = runtime.html;
            var columns = options && options.columns || model.vars || [];
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
            return h(__makeTemplateObject(["", "\n                ", "\n                ", "\n                ", ""], ["", "\n                ", "\n                ", "\n                ", ""]), before ? h(__makeTemplateObject(["<tr class=\"workbench-virtual-spacer\" aria-hidden=\"true\"><td colspan=", "\n                    style=", "></td></tr>"], ["<tr class=\"workbench-virtual-spacer\" aria-hidden=\"true\"><td colspan=", "\n                    style=", "></td></tr>"]), Math.max(1, columns.length), 'height:' + before + 'px;padding:0;border:0') : '', allRows.map(function (record, relativeIndex) { return h(__makeTemplateObject(["<tr data-workbench-row-index=", ">\n                    ", "\n                </tr>"], ["<tr data-workbench-row-index=", ">\n                    ", "\n                </tr>"]), start + relativeIndex, options && options.cells ? options.cells(record, start + relativeIndex) : columns.map(function (name) {
                var cell = record[name];
                if (options && options.status && (name === 'readable' || name === 'writeable')) {
                    var status_1 = name === 'readable' ? 'readable' : 'writeable';
                    return h(__makeTemplateObject(["<td data-label=", ">", "</td>"], ["<td data-label=", ">", "</td>"]), columnLabel(name, options), statusIcon(runtime, (cell === true || text(cell) === 'true') ? status_1 : 'negative', (cell === true || text(cell) === 'true') ? (name === 'readable' ? 'Readable' : 'Writeable') : 'No'));
                }
                if (options && options.repository && name === 'id') {
                    var id = text(cell);
                    var href = '../' + encodeURIComponent(id) + '/summary';
                    return h(__makeTemplateObject(["<td data-label=", "><a href=", ">", "</a></td>"], ["<td data-label=", "><a href=", ">", "</a></td>"]), columnLabel(name, options), href, id);
                }
                return h(__makeTemplateObject(["<td data-label=", ">", "</td>"], ["<td data-label=", ">", "</td>"]), columnLabel(name, options), renderTerm(runtime, cell, context, !!(options && options.linkTerms)));
            })); }), after ? h(__makeTemplateObject(["<tr class=\"workbench-virtual-spacer\" aria-hidden=\"true\"><td colspan=", "\n                    style=", "></td></tr>"], ["<tr class=\"workbench-virtual-spacer\" aria-hidden=\"true\"><td colspan=", "\n                    style=", "></td></tr>"]), Math.max(1, columns.length), 'height:' + after + 'px;padding:0;border:0') : '', options && options.trailing ? options.trailing() : '');
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
        /** Summary (M5.4, mockup 12): one card with Repository and Size sections and the configuration disclosure. */
        function summaryPage(runtime, model, context) {
            var h = runtime.html;
            var row = firstRecord(model);
            var config = text(meta(model, 'config-model-turtle', 'config-model'));
            var code = function (value) {
                var content = text(value);
                return content ? h(__makeTemplateObject(["<code>", "</code>"], ["<code>", "</code>"]), content) : '';
            };
            return h(__makeTemplateObject(["<section id=\"workbench-summary\" class=\"workbench-island workbench-summary\">\n                <h2>Repository</h2>\n                ", "\n                <h2>Size</h2>\n                ", "\n                ", "\n            </section>"], ["<section id=\"workbench-summary\" class=\"workbench-island workbench-summary\">\n                <h2>Repository</h2>\n                ", "\n                <h2>Size</h2>\n                ", "\n                ", "\n            </section>"]), keyValueList(runtime, [
                ['ID', code(field(row, 'id'))],
                ['Title', field(row, 'description')],
                ['Location', code(field(row, 'location'))],
                ['Server', code(field(row, 'server'))]
            ]), keyValueList(runtime, [
                ['Statements', formatCount(field(row, 'size'), context)],
                ['Named graphs', formatCount(field(row, 'contexts'), context)]
            ]), config ? h(__makeTemplateObject(["<details id=\"summary-config-model\" class=\"workbench-options workbench-summary-config\">\n                    <summary>Configuration (Turtle)", "</summary>\n                    <pre role=\"region\">", "</pre>\n                </details>"], ["<details id=\"summary-config-model\" class=\"workbench-options workbench-summary-config\">\n                    <summary>Configuration (Turtle)", "</summary>\n                    <pre role=\"region\">", "</pre>\n                </details>"]), icon(runtime, 'chevron', 'workbench-disclosure-chevron'), config) : '');
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
        function repositoryUrl(context, id, route) {
            return (context.basePath || '').replace(/\/+$/, '') + '/repositories/' + encodeURIComponent(id) + '/' + route;
        }
        /** One repository row (M5.3, mockup 12): Id link, Title, Access badges and an actions menu. */
        function repositoryCells(runtime, context, record, index) {
            var h = runtime.html;
            var id = text(record.id);
            var access = [
                record.readable === true || text(record.readable) === 'true' ? 'Read' : '',
                record.writeable === true || text(record.writeable) === 'true' ? 'Write' : ''
            ].filter(function (label) { return !!label; });
            var menuId = 'repository-actions-' + index;
            var item = function (route, iconName, label) { return h(__makeTemplateObject(["<li><a href=\"", "\">", "", "</a></li>"], ["<li><a href=\"", "\">", "", "</a></li>"]), repositoryUrl(context, id, route), icon(runtime, iconName), label); };
            return h(__makeTemplateObject(["<td data-label=\"Id\"><a class=\"workbench-repository-link\" href=\"", "\">", "</a></td>\n                <td data-label=\"Title\" title=\"", "\">", "</td>\n                <td data-label=\"Access\"><span class=\"workbench-badges\">", "</span></td>\n                <td class=\"workbench-row-actions\" data-label=\"Actions\"><div class=\"workbench-row-menu\">\n                    <button type=\"button\" class=\"workbench-action workbench-action--ghost workbench-action--icon\"\n                        aria-label=\"", "\" title=\"Actions\" aria-expanded=\"false\" aria-controls=\"", "\"\n                        data-workbench-row-menu=\"true\">", "</button>\n                    <div id=\"", "\" class=\"workbench-popover workbench-popover--end workbench-row-menu__panel\" hidden>\n                        <ul class=\"workbench-popover__menu\">\n                            ", "", "", "\n                            <li><a class=\"workbench-popover__danger\" href=\"", "\">", "Delete\u2026</a></li>\n                        </ul>\n                    </div>\n                </div></td>"], ["<td data-label=\"Id\"><a class=\"workbench-repository-link\" href=\"", "\">", "</a></td>\n                <td data-label=\"Title\" title=\"", "\">", "</td>\n                <td data-label=\"Access\"><span class=\"workbench-badges\">", "</span></td>\n                <td class=\"workbench-row-actions\" data-label=\"Actions\"><div class=\"workbench-row-menu\">\n                    <button type=\"button\" class=\"workbench-action workbench-action--ghost workbench-action--icon\"\n                        aria-label=\"", "\" title=\"Actions\" aria-expanded=\"false\" aria-controls=\"", "\"\n                        data-workbench-row-menu=\"true\">", "</button>\n                    <div id=\"", "\" class=\"workbench-popover workbench-popover--end workbench-row-menu__panel\" hidden>\n                        <ul class=\"workbench-popover__menu\">\n                            ", "", "", "\n                            <li><a class=\"workbench-popover__danger\" href=\"", "\">", "Delete\u2026</a></li>\n                        </ul>\n                    </div>\n                </div></td>"]), repositoryUrl(context, id, 'summary'), id, text(record.location), text(record.description), access.length
                ? access.map(function (label, position) { return h(__makeTemplateObject(["", "<span class=\"workbench-badge\">", "</span>"], ["", "<span class=\"workbench-badge\">", "</span>"]), position ? ' ' : '', label); })
                : h(__makeTemplateObject(["<span class=\"workbench-access-none\">None</span>"], ["<span class=\"workbench-access-none\">None</span>"])), 'Actions for ' + id, menuId, icon(runtime, 'more'), menuId, item('query', 'query', 'Query'), item('explore', 'explore', 'Explore'), item('summary', 'summary', 'Summary'), urlFor(context, 'delete') + '?id=' + encodeURIComponent(id), icon(runtime, 'delete'));
        }
        function repositoriesPage(runtime, model, context) {
            var h = runtime.html;
            var header = function (name) { return name === 'actions'
                ? h(__makeTemplateObject(["<th scope=\"col\"><span class=\"workbench-visually-hidden\">Actions</span></th>"], ["<th scope=\"col\"><span class=\"workbench-visually-hidden\">Actions</span></th>"])) : h(__makeTemplateObject(["<th scope=\"col\">", "</th>"], ["<th scope=\"col\">", "</th>"]), { id: 'Id', title: 'Title', access: 'Access' }[name]); };
            return h(__makeTemplateObject(["<section id=\"repositories-results\" class=\"workbench-island workbench-responsive-records workbench-browse-card\">\n                <div class=\"workbench-browse-card__header\">\n                    <h2>Repositories</h2><span class=\"workbench-browse-card__count\">", "</span>\n                    <a class=\"workbench-action workbench-action--primary workbench-browse-card__action\" href=\"", "\">", "<span>Create</span></a>\n                </div>\n                ", "\n            </section>"], ["<section id=\"repositories-results\" class=\"workbench-island workbench-responsive-records workbench-browse-card\">\n                <div class=\"workbench-browse-card__header\">\n                    <h2>Repositories</h2><span class=\"workbench-browse-card__count\">", "</span>\n                    <a class=\"workbench-action workbench-action--primary workbench-browse-card__action\" href=\"", "\">", "<span>Create</span></a>\n                </div>\n                ", "\n            </section>"]), formatCount(String(rowCount(model)), context), urlFor(context, 'create'), icon(runtime, 'create'), rowCount(model) ? table(runtime, model, context, { columns: ['id', 'title', 'access', 'actions'], header: header, cells: function (record, index) { return repositoryCells(runtime, context, record, index); } })
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
        /**
         * Delete repository (M6.6, mockup 09): nothing is chosen until the user picks a repository or arrives with
         * `?id=<id>`, the button stays disabled until then, and delete.ts confirms with the typed id.
         */
        function deletePage(runtime, model, context) {
            var h = runtime.html;
            var options = recordsFromRows(model, model.pickerRows || []).filter(function (row) { return text(row.id) !== 'SYSTEM'; });
            if (typeof model.metadata.selectedRepositoryId !== 'string') {
                model.metadata.selectedRepositoryId = locationParameter('id');
            }
            var selectedId = text(model.metadata.selectedRepositoryId);
            var selectedRepository = model.metadata.selectedRepository;
            var selectedVisible = options.some(function (row) { return text(row.id) === selectedId; });
            return h(__makeTemplateObject(["<form id=\"delete-form\" class=\"workbench-island workbench-form-card\" action=\"delete\" method=\"post\"\n                    @submit=", ">\n                <div class=\"workbench-field\"><label for=\"id\">Repository</label>\n                    <select id=\"id\" name=\"id\" data-workbench-window-picker=\"repositories\" @change=", "><option value=\"\" disabled ?selected=", ">Choose a repository</option>\n                        ", "\n                        ", "\n                    </select>\n                    ", "\n                </div>\n                <div id=\"delete-actions\" class=\"workbench-form-actions\"><button type=\"submit\"\n                        class=\"workbench-action workbench-action--danger-outline\" ?disabled=", ">", "<span>Delete repository\u2026</span></button>\n                </div><span id=\"delete-feedback\" class=\"error\" role=\"alert\"></span>\n            </form>"], ["<form id=\"delete-form\" class=\"workbench-island workbench-form-card\" action=\"delete\" method=\"post\"\n                    @submit=", ">\n                <div class=\"workbench-field\"><label for=\"id\">Repository</label>\n                    <select id=\"id\" name=\"id\" data-workbench-window-picker=\"repositories\" @change=", "><option value=\"\" disabled ?selected=", ">Choose a repository</option>\n                        ", "\n                        ", "\n                    </select>\n                    ", "\n                </div>\n                <div id=\"delete-actions\" class=\"workbench-form-actions\"><button type=\"submit\"\n                        class=\"workbench-action workbench-action--danger-outline\" ?disabled=", ">", "<span>Delete repository\u2026</span></button>\n                </div><span id=\"delete-feedback\" class=\"error\" role=\"alert\"></span>\n            </form>"]), function (event) {
                var globalWindow = typeof window !== 'undefined' ? window : null;
                if (globalWindow && typeof globalWindow.checkIsSafeToDelete === 'function') {
                    globalWindow.checkIsSafeToDelete(event);
                }
            }, function (event) {
                var selected = options.filter(function (row) { return text(row.id) === event.target.value; })[0];
                model.metadata.selectedRepositoryId = event.target.value;
                model.metadata.selectedRepository = selected || null;
                var outlet = event.target.closest('.workbench-outlet');
                if (outlet) {
                    render(outlet, model, context, runtime);
                }
            }, !selectedId, selectedId && !selectedVisible
                ? h(__makeTemplateObject(["<option value=", " selected>", "</option>"], ["<option value=", " selected>", "</option>"]), selectedId, selectedRepository
                    ? text(selectedRepository.id) + ' — ' + text(selectedRepository.description) : selectedId) : '', options.map(function (row) { return h(__makeTemplateObject(["<option value=", " ?selected=", ">\n                            ", " \u2014 ", "</option>"], ["<option value=", " ?selected=", ">\n                            ", " \u2014 ", "</option>"]), text(row.id), text(row.id) === selectedId, text(row.id), text(row.description)); }), pickerWindow(runtime, model, 'repositories', 'repositories'), !selectedId, icon(runtime, 'delete'));
        }
        function namespaceEditor(model) {
            var holder = model;
            if (!holder.namespaceEditor) {
                holder.namespaceEditor = { editing: null, adding: false, filter: '' };
            }
            return holder.namespaceEditor;
        }
        /** Posts a namespace change; the server answers with a redirect to the listing or the listing and an error. */
        function postNamespaces(document, fields) {
            var form = document.createElement('form');
            form.method = 'post';
            form.action = 'namespaces';
            form.hidden = true;
            Object.keys(fields).forEach(function (name) {
                var input = document.createElement('input');
                input.type = 'hidden';
                input.name = name;
                input.value = fields[name];
                form.appendChild(input);
            });
            document.body.appendChild(form);
            form.submit();
        }
        /** Namespaces (M6.3, mockup 07): rows in prefix order, edited in place; no field is prefilled from a row. */
        function namespacesPage(runtime, model, context) {
            var h = runtime.html;
            var state = namespaceEditor(model);
            var rows = records(model).map(function (record) { return ({ prefix: text(record.prefix), namespace: text(record.namespace) }); });
            var needle = state.filter.trim().toLowerCase();
            var visible = rows.filter(function (row) { return !needle || row.prefix.toLowerCase().indexOf(needle) >= 0
                || row.namespace.toLowerCase().indexOf(needle) >= 0; });
            var rerender = function (event, focusEditor) {
                var outlet = event.currentTarget.closest('.workbench-outlet');
                if (outlet) {
                    render(outlet, model, context, runtime);
                    var first = focusEditor ? outlet.querySelector('.workbench-namespace-edit input') : null;
                    if (first) {
                        first.focus();
                    }
                }
            };
            var label = function (prefix) { return prefix || '(default)'; };
            var save = function (event, row) {
                var inputs = event.currentTarget.closest('tr').querySelectorAll('input');
                var fields = { action: 'save', prefix: inputs[0].value.trim(), namespace: inputs[1].value.trim() };
                if (row) {
                    fields.previousPrefix = row.prefix;
                }
                postNamespaces(event.currentTarget.ownerDocument, fields);
            };
            var cancel = function (event) {
                state.editing = null;
                state.adding = false;
                rerender(event);
            };
            var editKeys = function (event, row) {
                if (event.key === 'Enter') {
                    event.preventDefault();
                    save(event, row);
                }
                else if (event.key === 'Escape') {
                    event.preventDefault();
                    cancel(event);
                }
            };
            var editRow = function (row) { return h(__makeTemplateObject(["<tr class=\"workbench-namespace-edit\">\n                <td data-label=\"Prefix\"><input type=\"text\" aria-label=\"Prefix\" value=", "\n                    autocomplete=\"off\" spellcheck=\"false\" @keydown=", " /></td>\n                <td data-label=\"Namespace\"><input type=\"text\" aria-label=\"Namespace\" value=", "\n                    autocomplete=\"off\" spellcheck=\"false\" @keydown=", " /></td>\n                <td class=\"workbench-row-actions\" data-label=\"Actions\"><button type=\"button\"\n                        class=\"workbench-action workbench-action--primary workbench-action--icon\" aria-label=\"Save\" title=\"Save\"\n                        @click=", ">", "</button><button type=\"button\"\n                        class=\"workbench-action workbench-action--secondary workbench-action--icon\" aria-label=\"Cancel\" title=\"Cancel\"\n                        @click=", ">", "</button></td>\n            </tr>"], ["<tr class=\"workbench-namespace-edit\">\n                <td data-label=\"Prefix\"><input type=\"text\" aria-label=\"Prefix\" value=", "\n                    autocomplete=\"off\" spellcheck=\"false\" @keydown=", " /></td>\n                <td data-label=\"Namespace\"><input type=\"text\" aria-label=\"Namespace\" value=", "\n                    autocomplete=\"off\" spellcheck=\"false\" @keydown=", " /></td>\n                <td class=\"workbench-row-actions\" data-label=\"Actions\"><button type=\"button\"\n                        class=\"workbench-action workbench-action--primary workbench-action--icon\" aria-label=\"Save\" title=\"Save\"\n                        @click=", ">", "</button><button type=\"button\"\n                        class=\"workbench-action workbench-action--secondary workbench-action--icon\" aria-label=\"Cancel\" title=\"Cancel\"\n                        @click=", ">", "</button></td>\n            </tr>"]), row ? row.prefix : '', function (event) { return editKeys(event, row); }, row ? row.namespace : '', function (event) { return editKeys(event, row); }, function (event) { return save(event, row); }, icon(runtime, 'check'), cancel, icon(runtime, 'close')); };
            var viewRow = function (row) { return h(__makeTemplateObject(["<tr>\n                <td data-label=\"Prefix\"><code>", "</code></td>\n                <td data-label=\"Namespace\"><code>", "</code></td>\n                <td class=\"workbench-row-actions\" data-label=\"Actions\"><button type=\"button\"\n                        class=\"workbench-action workbench-action--ghost workbench-action--icon\" aria-label=", "\n                        title=\"Edit\" @click=", ">", "</button><button type=\"button\"\n                        class=\"workbench-action workbench-action--ghost workbench-action--icon workbench-namespace-delete\"\n                        aria-label=", " title=\"Delete\" @click=", ">", "</button></td>\n            </tr>"], ["<tr>\n                <td data-label=\"Prefix\"><code>", "</code></td>\n                <td data-label=\"Namespace\"><code>", "</code></td>\n                <td class=\"workbench-row-actions\" data-label=\"Actions\"><button type=\"button\"\n                        class=\"workbench-action workbench-action--ghost workbench-action--icon\" aria-label=", "\n                        title=\"Edit\" @click=", ">", "</button><button type=\"button\"\n                        class=\"workbench-action workbench-action--ghost workbench-action--icon workbench-namespace-delete\"\n                        aria-label=", " title=\"Delete\" @click=", ">", "</button></td>\n            </tr>"]), row.prefix, row.namespace, 'Edit ' + label(row.prefix), function (event) {
                state.editing = row.prefix;
                state.adding = false;
                rerender(event, true);
            }, icon(runtime, 'edit'), 'Delete ' + label(row.prefix), function (event) {
                var document = event.currentTarget.ownerDocument;
                var dialog = workbench.confirmDialog;
                dialog.open({ title: 'Delete prefix ' + label(row.prefix) + '?', body: row.namespace,
                    confirmLabel: 'Delete prefix', danger: true }).then(function (confirmed) {
                    if (confirmed) {
                        postNamespaces(document, { action: 'delete', prefix: row.prefix });
                    }
                });
            }, icon(runtime, 'delete')); };
            return h(__makeTemplateObject(["", "<section id=\"namespaces-results\"\n                    class=\"workbench-island workbench-responsive-records workbench-browse-card\">\n                <div class=\"workbench-browse-card__header\">\n                    <h2>Namespaces</h2><span class=\"workbench-browse-card__count\">", "</span>\n                    <form class=\"workbench-browse-card__filter\" role=\"search\" @submit=", ">\n                        <label class=\"workbench-visually-hidden\" for=\"namespaces-filter\">Filter prefixes or IRIs</label>\n                        <div class=\"workbench-search-field\">", "<input\n                            type=\"text\" id=\"namespaces-filter\" placeholder=\"Filter prefixes or IRIs\" autocomplete=\"off\"\n                            spellcheck=\"false\" @input=", " /></div>\n                    </form>\n                    <button type=\"button\" class=\"workbench-action workbench-action--primary workbench-browse-card__action\"\n                        @click=", ">", "<span>Add namespace</span></button>\n                </div>\n                <table class=\"data workbench-namespaces-table\">\n                    <thead><tr><th scope=\"col\">Prefix</th><th scope=\"col\">Namespace</th>\n                        <th scope=\"col\"><span class=\"workbench-visually-hidden\">Actions</span></th></tr></thead>\n                    <tbody>\n                        ", "\n                        ", "\n                        ", "\n                    </tbody>\n                </table>\n            </section>"], ["", "<section id=\"namespaces-results\"\n                    class=\"workbench-island workbench-responsive-records workbench-browse-card\">\n                <div class=\"workbench-browse-card__header\">\n                    <h2>Namespaces</h2><span class=\"workbench-browse-card__count\">", "</span>\n                    <form class=\"workbench-browse-card__filter\" role=\"search\" @submit=", ">\n                        <label class=\"workbench-visually-hidden\" for=\"namespaces-filter\">Filter prefixes or IRIs</label>\n                        <div class=\"workbench-search-field\">", "<input\n                            type=\"text\" id=\"namespaces-filter\" placeholder=\"Filter prefixes or IRIs\" autocomplete=\"off\"\n                            spellcheck=\"false\" @input=", " /></div>\n                    </form>\n                    <button type=\"button\" class=\"workbench-action workbench-action--primary workbench-browse-card__action\"\n                        @click=", ">", "<span>Add namespace</span></button>\n                </div>\n                <table class=\"data workbench-namespaces-table\">\n                    <thead><tr><th scope=\"col\">Prefix</th><th scope=\"col\">Namespace</th>\n                        <th scope=\"col\"><span class=\"workbench-visually-hidden\">Actions</span></th></tr></thead>\n                    <tbody>\n                        ", "\n                        ", "\n                        ", "\n                    </tbody>\n                </table>\n            </section>"]), errorCallout(runtime, model), formatCount(String(rows.length), context), function (event) { return event.preventDefault(); }, icon(runtime, 'search', 'workbench-search-field__icon'), function (event) {
                state.filter = event.currentTarget.value;
                rerender(event);
            }, function (event) {
                state.adding = true;
                state.editing = null;
                rerender(event, true);
            }, icon(runtime, 'add'), state.adding ? editRow(null) : '', visible.map(function (row) { return state.editing === row.prefix ? editRow(row) : viewRow(row); }), !visible.length && !state.adding ? h(__makeTemplateObject(["<tr class=\"workbench-empty-row\"><td role=\"status\" colspan=\"3\">", "</td></tr>"], ["<tr class=\"workbench-empty-row\"><td role=\"status\" colspan=\"3\">", "</td></tr>"]), rows.length ? 'No namespaces match this filter.' : 'No namespaces.') : '');
        }
        /** Types and Graphs (M5.2, mockup 06): what each list shows and the column its counts fill in. */
        var browseLists = {
            types: { title: 'Types', noun: 'types', column: 'type', label: 'Type',
                count: 'instances', countLabel: 'Instances', resort: true },
            contexts: { title: 'Graphs', noun: 'graphs', column: 'context', label: 'Graph',
                count: 'statements', countLabel: 'Statements', actions: true }
        };
        function browseCounts(model) {
            var holder = model;
            if (!holder.browseCounts) {
                holder.browseCounts = { state: model.rowCount > 0 ? 'pending' : 'none', listed: model.rowCount, values: {} };
            }
            return holder.browseCounts;
        }
        function browseNameCell(runtime, model, list, term) {
            var h = runtime.html;
            if (!term) {
                return h(__makeTemplateObject(["<td data-label=", ">Default graph</td>"], ["<td data-label=", ">Default graph</td>"]), list.label);
            }
            if (typeof term !== 'object' || !term.kind) {
                return h(__makeTemplateObject(["<td data-label=", ">", "</td>"], ["<td data-label=", ">", "</td>"]), list.label, text(term));
            }
            var stream = workbench.queryStream;
            var full = termText(term);
            var prefixed = list.column === 'type' && term.kind === 'iri' ? stream.abbreviateIri(term.value, exploreNamespaces(model)) : '';
            var label = prefixed && prefixed.charAt(0) !== '<' ? prefixed : full;
            return h(__makeTemplateObject(["<td data-label=", "><a href=\"", "\"\n                title=", ">", "</a></td>"], ["<td data-label=", "><a href=\"", "\"\n                title=", ">", "</a></td>"]), list.label, 'explore?resource=' + encodeURIComponent(stream.exploreResource(term)), full, label);
        }
        function browseCountCell(runtime, list, counts, value, context) {
            var h = runtime.html;
            if (value !== null && typeof value !== 'undefined') {
                return h(__makeTemplateObject(["<td class=\"workbench-count-cell\" data-label=", ">", "</td>"], ["<td class=\"workbench-count-cell\" data-label=", ">", "</td>"]), list.countLabel, formatCount(value, context));
            }
            if (counts.state === 'pending') {
                return h(__makeTemplateObject(["<td class=\"workbench-count-cell\" data-label=", " aria-busy=\"true\"><span\n                    title=\"Counting\u2026\">\u2026</span></td>"], ["<td class=\"workbench-count-cell\" data-label=", " aria-busy=\"true\"><span\n                    title=\"Counting\u2026\">\u2026</span></td>"]), list.countLabel);
            }
            var reason = counts.state === 'timed-out' ? 'Counting took longer than two seconds' : 'No count is available';
            return h(__makeTemplateObject(["<td class=\"workbench-count-cell\" data-label=", "><span title=", ">\u2014</span></td>"], ["<td class=\"workbench-count-cell\" data-label=", "><span title=", ">\u2014</span></td>"]), list.countLabel, reason);
        }
        function browseActionsCell(runtime, term) {
            var h = runtime.html;
            if (!term || typeof term !== 'object' || !term.kind) {
                return h(__makeTemplateObject(["<td class=\"workbench-row-actions\" data-label=\"Actions\"></td>"], ["<td class=\"workbench-row-actions\" data-label=\"Actions\"></td>"]));
            }
            var key = encodeURIComponent(ntriples(term));
            var name = termText(term);
            return h(__makeTemplateObject(["<td class=\"workbench-row-actions\" data-label=\"Actions\"><a class=\"workbench-action workbench-action--ghost workbench-action--icon\"\n                    href=\"", "\" aria-label=\"", "\" title=\"Explore\">", "</a><a\n                    class=\"workbench-action workbench-action--ghost workbench-action--icon\" href=\"", "\"\n                    aria-label=\"", "\" title=\"Clear graph\u2026\">", "</a></td>"], ["<td class=\"workbench-row-actions\" data-label=\"Actions\"><a class=\"workbench-action workbench-action--ghost workbench-action--icon\"\n                    href=\"", "\" aria-label=\"", "\" title=\"Explore\">", "</a><a\n                    class=\"workbench-action workbench-action--ghost workbench-action--icon\" href=\"", "\"\n                    aria-label=\"", "\" title=\"Clear graph\u2026\">", "</a></td>"]), 'explore?resource=' + key, 'Explore ' + name, icon(runtime, 'explore'), 'clear?context=' + key, 'Clear graph ' + name + '…', icon(runtime, 'clear'));
        }
        function browseListPage(runtime, model, context) {
            var h = runtime.html;
            var route = model.viewId;
            var list = browseLists[route];
            var counts = browseCounts(model);
            var filter = text(pageValue(model, 'filter'));
            var byCount = list.resort && counts.state === 'done';
            var columns = [list.column, list.count].concat(list.actions ? ['actions'] : []);
            var header = function (name) {
                if (name === 'actions') {
                    return h(__makeTemplateObject(["<th scope=\"col\"><span class=\"workbench-visually-hidden\">Actions</span></th>"], ["<th scope=\"col\"><span class=\"workbench-visually-hidden\">Actions</span></th>"]));
                }
                var sort = name === list.column && !byCount ? 'ascending'
                    : name === list.count && byCount ? 'descending' : runtime.nothing;
                return name === list.count
                    ? h(__makeTemplateObject(["<th scope=\"col\" class=\"workbench-count-column\" aria-sort=\"", "\">", "</th>"], ["<th scope=\"col\" class=\"workbench-count-column\" aria-sort=\"", "\">", "</th>"]), sort, list.countLabel) : h(__makeTemplateObject(["<th scope=\"col\" aria-sort=\"", "\">", "</th>"], ["<th scope=\"col\" aria-sort=\"", "\">", "</th>"]), sort, list.label);
            };
            // The server names the listed column (type or context); the counts add a second one.
            var nameColumn = model.vars && model.vars.length ? model.vars[0] : list.column;
            var cells = function (record) {
                var term = record[nameColumn];
                var value = list.resort ? record[list.count] : counts.values[term ? ntriples(term) : ''];
                return h(__makeTemplateObject(["", "", "", ""], ["", "", "", ""]), browseNameCell(runtime, model, list, term), browseCountCell(runtime, list, counts, value, context), list.actions ? browseActionsCell(runtime, term) : '');
            };
            // The default graph has no name, so it is listed only without a filter (the server omits it too).
            var trailing = list.actions && !filter ? function () { return h(__makeTemplateObject(["<tr class=\"workbench-browse-default-row\">", "", "", "</tr>"], ["<tr class=\"workbench-browse-default-row\">", "", "", "</tr>"]), browseNameCell(runtime, model, list, null), browseCountCell(runtime, list, counts, counts.values[''], context), browseActionsCell(runtime, null)); } : null;
            var inputId = route + '-filter';
            return h(__makeTemplateObject(["", "<section id=", "\n                    class=\"workbench-island workbench-responsive-records workbench-browse-card\">\n                <div class=\"workbench-browse-card__header\">\n                    <h2>", "</h2><span class=\"workbench-browse-card__count\">", "</span>\n                    <form class=\"workbench-browse-card__filter\" action=", " method=\"get\" role=\"search\">\n                        <label class=\"workbench-visually-hidden\" for=\"", "\">", "</label>\n                        <div class=\"workbench-search-field\">", "<input\n                            type=\"text\" id=\"", "\" name=\"filter\" value=", " placeholder=", "\n                            autocomplete=\"off\" spellcheck=\"false\" /></div>\n                    </form>\n                </div>\n                ", "\n            </section>"], ["", "<section id=", "\n                    class=\"workbench-island workbench-responsive-records workbench-browse-card\">\n                <div class=\"workbench-browse-card__header\">\n                    <h2>", "</h2><span class=\"workbench-browse-card__count\">", "</span>\n                    <form class=\"workbench-browse-card__filter\" action=", " method=\"get\" role=\"search\">\n                        <label class=\"workbench-visually-hidden\" for=\"", "\">", "</label>\n                        <div class=\"workbench-search-field\">", "<input\n                            type=\"text\" id=\"", "\" name=\"filter\" value=", " placeholder=", "\n                            autocomplete=\"off\" spellcheck=\"false\" /></div>\n                    </form>\n                </div>\n                ", "\n            </section>"]), errorCallout(runtime, model), route + '-results', list.title, formatCount(String(counts.listed), context), route, inputId, 'Filter ' + list.noun, icon(runtime, 'search', 'workbench-search-field__icon'), inputId, filter, 'Filter ' + list.noun, table(runtime, model, context, { columns: columns, header: header, cells: cells, trailing: trailing, emptyText: filter ? 'No ' + list.noun + ' match this filter.'
                    : route === 'contexts' ? 'No named graphs.' : 'No types.' }));
        }
        var explorePageSize = 40;
        var rdfsNamespace = 'http://www.w3.org/2000/01/rdf-schema#';
        var rdfNamespace = 'http://www.w3.org/1999/02/22-rdf-syntax-ns#';
        /** Pages that fit in one row window are grouped by role; larger pages keep the single windowed table. */
        var exploreGroupedRowLimit = 80;
        var exploreRoles = [
            { key: 'outgoing', title: 'Outgoing', columns: ['predicate', 'object'] },
            { key: 'incoming', title: 'Incoming', columns: ['subject', 'predicate'] },
            { key: 'predicate', title: 'Used as predicate', columns: ['subject', 'object'] },
            { key: 'graph', title: 'Graph contents', columns: ['subject', 'predicate', 'object'] }
        ];
        function ntriples(term) {
            var stream = workbench.queryStream;
            return stream && typeof stream.ntriplesTerm === 'function' ? stream.ntriplesTerm(term) : text(term);
        }
        /** The explored resource in N-Triples form: the server's resolved value, or the typed text when it already
         *  is N-Triples. */
        function exploreResourceKey(model) {
            var resolved = text(pageValue(model, 'explore-resource'));
            if (resolved) {
                return resolved;
            }
            var typed = text(pageValue(model, 'resource')).trim();
            return /^(?:<|_:|"|<<)/.test(typed) ? typed : '';
        }
        function exploreNamespaces(model) {
            var map = model.namespaceMap || {};
            return Object.keys(map).map(function (prefix) { return ({
                prefix: prefix.charAt(prefix.length - 1) === ':' ? prefix.slice(0, -1) : prefix,
                name: map[prefix]
            }); });
        }
        /** One Explore cell, formatted like a query result cell (M4.3): prefixed names, values, tags. */
        function exploreTerm(runtime, term, namespaces, alwaysLink) {
            var h = runtime.html;
            var stream = workbench.queryStream;
            if (!term || typeof term !== 'object' || !term.kind || !stream || typeof stream.formatRdfTerm !== 'function') {
                return text(term);
            }
            var display = stream.formatRdfTerm(term, { namespaces: namespaces });
            var tags = display.kind !== 'literal' ? ''
                : display.language ? h(__makeTemplateObject(["<span class=\"rdf-language\">@", "", "</span>"], ["<span class=\"rdf-language\">@", "", "</span>"]), display.language, term.direction ? '--' + term.direction : '') : stream.showsDatatypeTag(display)
                    ? h(__makeTemplateObject(["<span class=\"rdf-datatype\">", "</span>"], ["<span class=\"rdf-datatype\">", "</span>"]), stream.abbreviateIri(display.datatype, namespaces)) : '';
            var href = display.exploreHref
                || (alwaysLink ? 'explore?resource=' + encodeURIComponent(stream.exploreResource(term)) : '');
            var value = href
                ? h(__makeTemplateObject(["<a href=", " title=", ">", "</a>"], ["<a href=", " title=", ">", "</a>"]), href, display.title, display.label) : h(__makeTemplateObject(["<span title=", ">", "</span>"], ["<span title=", ">", "</span>"]), display.title, display.label);
            return h(__makeTemplateObject(["<div class=\"resource\">", "", "</div>"], ["<div class=\"resource\">", "", "</div>"]), value, tags);
        }
        function exploreGraphLabel(term, namespaces) {
            if (!term) {
                return 'Default graph';
            }
            var stream = workbench.queryStream;
            return stream && typeof stream.formatRdfTerm === 'function'
                ? stream.formatRdfTerm(term, { namespaces: namespaces }).label : text(term);
        }
        /** The four role groups: Outgoing, Incoming, Used as predicate and Graph contents (mockup 05). */
        function exploreRoleGroups(runtime, model, roles) {
            var h = runtime.html;
            var namespaces = exploreNamespaces(model);
            var index = function (name) { return model.vars.indexOf(name); };
            return exploreRoles.filter(function (role) { return roles[role.key].length; }).map(function (role) {
                var entries = roles[role.key];
                var rows = entries.map(function (entry) { return entry.values; });
                var graphs = rows.map(function (row) { return index('context') >= 0 ? row[index('context')] : null; });
                var graphKeys = graphs.map(function (graph) { return graph ? ntriples(graph) : ''; });
                var oneGraph = role.key === 'graph' || graphKeys.every(function (key) { return key === graphKeys[0]; });
                var columns = role.columns.concat(oneGraph ? [] : ['context']);
                var headingId = 'explore-group-' + role.key;
                return h(__makeTemplateObject(["<section class=\"explore-group\" data-explore-role=", " aria-labelledby=", ">\n                    <h3 id=", " class=\"explore-group__title\">", "\n                        <span class=\"explore-group__count\">", "</span>\n                        ", "</h3>\n                    <table class=\"data\">\n                        <thead><tr>", "</tr></thead>\n                        <tbody>", "</tbody>\n                    </table>\n                </section>"], ["<section class=\"explore-group\" data-explore-role=", " aria-labelledby=", ">\n                    <h3 id=", " class=\"explore-group__title\">", "\n                        <span class=\"explore-group__count\">", "</span>\n                        ", "</h3>\n                    <table class=\"data\">\n                        <thead><tr>", "</tr></thead>\n                        <tbody>", "</tbody>\n                    </table>\n                </section>"]), role.key, headingId, headingId, role.title, rows.length, oneGraph && role.key !== 'graph'
                    ? h(__makeTemplateObject(["<span class=\"explore-group__graph\">Graph: ", "</span>"], ["<span class=\"explore-group__graph\">Graph: ", "</span>"]), exploreGraphLabel(graphs[0], namespaces)) : '', columns.map(function (name) { return h(__makeTemplateObject(["<th scope=\"col\">", "</th>"], ["<th scope=\"col\">", "</th>"]), name === 'context' ? 'Graph' : columnLabel(name)); }), entries.map(function (entry) { return entry.values; }).map(function (row, position) { return h(__makeTemplateObject(["<tr\n                            data-workbench-row-index=", ">", "</tr>"], ["<tr\n                            data-workbench-row-index=", ">", "</tr>"]), entries[position].index, columns.map(function (name) { return h(__makeTemplateObject(["<td\n                            data-label=", ">", "</td>"], ["<td\n                            data-label=", ">", "</td>"]), name === 'context' ? 'Graph' : columnLabel(name), name === 'context' && !row[index(name)]
                    ? 'Default graph' : exploreTerm(runtime, row[index(name)], namespaces)); })); }));
            });
        }
        /** The explored resource: label, IRI with Copy, types as chips, comment and "Query this resource". */
        function exploreResourceCard(runtime, model, summary, resource) {
            var h = runtime.html;
            var namespaces = exploreNamespaces(model);
            var key = exploreResourceKey(model) || resource;
            var isIri = /^<[^<]/.test(key);
            var comment = summary.comment ? text(summary.comment) : '';
            var types = summary.types || [];
            var copy = function () {
                var clipboard = typeof navigator !== 'undefined' && navigator.clipboard;
                if (clipboard && typeof clipboard.writeText === 'function') {
                    clipboard.writeText(isIri ? key.slice(1, -1) : key);
                }
            };
            var toggleComment = function (event) {
                var button = event.currentTarget;
                var card = button && button.closest ? button.closest('.explore-resource-card') : null;
                var paragraph = card ? card.querySelector('.explore-resource-card__comment') : null;
                if (paragraph) {
                    var expanded = paragraph.classList.toggle('is-expanded');
                    button.setAttribute('aria-expanded', expanded ? 'true' : 'false');
                    button.textContent = expanded ? 'Show less' : 'Show more';
                }
            };
            return h(__makeTemplateObject(["<section id=\"explore-resource-card\" class=\"workbench-island explore-resource-card\" aria-label=\"Explored resource\">\n                ", "\n                <div class=\"explore-resource-card__iri\"><code id=\"explore-resource-iri\">", "</code>\n                    <button class=\"workbench-action workbench-action--ghost workbench-action--icon\" type=\"button\"\n                        aria-label=\"Copy resource\" title=\"Copy resource\" @click=", ">", "</button></div>\n                ", "\n                ", "\n                ", "\n            </section>"], ["<section id=\"explore-resource-card\" class=\"workbench-island explore-resource-card\" aria-label=\"Explored resource\">\n                ", "\n                <div class=\"explore-resource-card__iri\"><code id=\"explore-resource-iri\">", "</code>\n                    <button class=\"workbench-action workbench-action--ghost workbench-action--icon\" type=\"button\"\n                        aria-label=\"Copy resource\" title=\"Copy resource\" @click=", ">", "</button></div>\n                ", "\n                ", "\n                ", "\n            </section>"]), summary.label ? h(__makeTemplateObject(["<h2>", "</h2>"], ["<h2>", "</h2>"]), text(summary.label)) : '', isIri ? key.slice(1, -1) : key, copy, icon(runtime, 'copy'), types.length ? h(__makeTemplateObject(["<ul class=\"explore-resource-card__types\" aria-label=\"Types\">", "</ul>"], ["<ul class=\"explore-resource-card__types\" aria-label=\"Types\">", "</ul>"]), types.map(function (type) { return h(__makeTemplateObject(["<li>\n                    <a class=\"explore-chip\" href=", ">", "</a></li>"], ["<li>\n                    <a class=\"explore-chip\" href=", ">", "</a></li>"]), 'explore?resource=' + encodeURIComponent(ntriples(type)), exploreGraphLabel(type, namespaces)); })) : '', comment ? h(__makeTemplateObject(["<p class=\"explore-resource-card__comment workbench-prose\">", "</p>\n                    ", ""], ["<p class=\"explore-resource-card__comment workbench-prose\">", "</p>\n                    ", ""]), comment, comment.length > 240 ? h(__makeTemplateObject(["<button class=\"workbench-action workbench-action--ghost explore-resource-card__more\" type=\"button\"\n                        aria-expanded=\"false\" @click=", ">Show more</button>"], ["<button class=\"workbench-action workbench-action--ghost explore-resource-card__more\" type=\"button\"\n                        aria-expanded=\"false\" @click=", ">Show more</button>"]), toggleComment) : '') : '', isIri ? h(__makeTemplateObject(["<a class=\"workbench-action workbench-action--secondary explore-resource-card__query\"\n                    href=", ">", "<span>Query this resource</span></a>"], ["<a class=\"workbench-action workbench-action--secondary explore-resource-card__query\"\n                    href=", ">", "<span>Query this resource</span></a>"]), 'query?query=' + encodeURIComponent('SELECT * WHERE { ' + key + ' ?p ?o }'), icon(runtime, 'query')) : '');
        }
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
            var key = exploreResourceKey(model);
            return {
                groups: groups,
                resourceKey: key,
                // Rows grouped by the role the explored resource plays, for pages small enough to list (M5.1).
                roles: key && rowCount(model) <= exploreGroupedRowLimit
                    ? { outgoing: [], predicate: [], incoming: [], graph: [] } : null,
                types: [],
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
            var contextIndex = model.vars.indexOf('context');
            var key = accumulator.resourceKey;
            var matches = function (term) { return !!key && !!term && ntriples(term) === key; };
            rows.forEach(function (row, offset) {
                var subject = subjectIndex >= 0 ? row[subjectIndex] : null;
                var predicate = predicateIndex >= 0 ? text(row[predicateIndex]) : '';
                var object = objectIndex >= 0 ? row[objectIndex] : null;
                var rowIndex = start + offset;
                // Another resource's label must not become the heading: only rows about the explored resource count.
                var aboutResource = !key || matches(subject);
                if (accumulator.roles) {
                    var role = matches(subject) ? 'outgoing'
                        : predicateIndex >= 0 && matches(row[predicateIndex]) ? 'predicate'
                            : matches(object) ? 'incoming'
                                : contextIndex >= 0 && matches(row[contextIndex]) ? 'graph' : null;
                    if (role) {
                        accumulator.roles[role].push({ values: row, index: rowIndex });
                    }
                }
                if (aboutResource && predicate === rdfNamespace + 'type' && object) {
                    accumulator.types.push(object);
                }
                if (predicate === rdfsNamespace + 'label' && aboutResource) {
                    accumulator.labelCount++;
                    accumulator.label = object;
                }
                else if (predicate === rdfsNamespace + 'comment' && aboutResource) {
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
                roles: accumulator.roles,
                types: accumulator.types,
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
            // Group only when every row of the page has a role; anything unexpected keeps the plain table.
            // Display keeps its toggle in the form row and opens its panel in a track below the row.
            var exploreDisplay = workbench.detailDisclosure.renderSeparated(h, {
                id: 'explore-result-options', toggleId: 'explore-result-options-toggle',
                panelId: 'explore-result-options-panel', label: 'Display', accessibleName: 'Result display options',
                ownerClass: 'workbench-options workbench-form-subgroup'
            }, h(__makeTemplateObject(["<div class=\"workbench-field workbench-disclosure__field\"><label for=\"limit_explore\">Result limit</label>\n                    ", "\n                </div><label class=\"workbench-check\" for=\"explore-show-datatypes\">\n                    <input id=\"explore-show-datatypes\" type=\"checkbox\" name=\"show-datatypes\" value=\"show-dataypes\" checked />\n                    <span>Show datatypes</span></label>"], ["<div class=\"workbench-field workbench-disclosure__field\"><label for=\"limit_explore\">Result limit</label>\n                    ", "\n                </div><label class=\"workbench-check\" for=\"explore-show-datatypes\">\n                    <input id=\"explore-show-datatypes\" type=\"checkbox\" name=\"show-datatypes\" value=\"show-dataypes\" checked />\n                    <span>Show datatypes</span></label>"]), limitSelect(runtime, 'limit_explore', context, text(pageValue(model, 'default-limit')) || '100')));
            var roles = summary.roles;
            var groupedRows = roles ? exploreRoles.reduce(function (sum, role) { return sum + roles[role.key].length; }, 0) : 0;
            var grouped = !!roles && !!total && groupedRows === total;
            return h(__makeTemplateObject(["<form id=\"explore-form\" class=\"workbench-island explore-form\" action=\"explore\">\n                    <input id=\"workbench-total-result-count\" type=\"hidden\"\n                        value=", " />\n                    <div id=\"explore-controls\"><div id=\"explore-resource-field\" class=\"workbench-field\">\n                        <label for=\"resource\">Resource</label>\n                        <div class=\"workbench-search-field\">", "<input id=\"resource\"\n                            name=\"resource\" size=\"48\" type=\"text\" value=", " spellcheck=\"false\"\n                            placeholder=\"<http://\u2026>, prefix:name, _:node or &quot;literal&quot;\" /></div>\n                    </div>\n                    <button id=\"explore-submit\" class=\"workbench-action workbench-action--primary\" type=\"submit\">Explore</button>\n                    ", "\n                    </div>\n                    <div class=\"workbench-action-toolbar__panels workbench-disclosure-track\">", "</div>\n                </form>\n                ", "\n                ", "\n                ", "\n                <p id=\"explore-resource-summary\" class=\"workbench-page-meta\" ?hidden=", ">\n                    <span id=\"explore-resource-value\" hidden>", "</span><span id=\"explore-result-count\">", "</span>\n                </p>\n                <section id=\"explore-results\" class=\"workbench-island workbench-responsive-records\">\n                    ", "\n                    <div id=\"explore-pagination\" class=\"workbench-form-actions\" ?hidden=", ">\n                        <button id=\"previousX\" class=\"workbench-action workbench-action--secondary\" type=\"button\" value=", "\n                            @click=", ">Previous ", "</button>\n                        <button id=\"nextX\" class=\"workbench-action workbench-action--secondary\" type=\"button\" value=", "\n                            @click=", ">Next ", "</button>\n                    </div>\n                </section>"], ["<form id=\"explore-form\" class=\"workbench-island explore-form\" action=\"explore\">\n                    <input id=\"workbench-total-result-count\" type=\"hidden\"\n                        value=", " />\n                    <div id=\"explore-controls\"><div id=\"explore-resource-field\" class=\"workbench-field\">\n                        <label for=\"resource\">Resource</label>\n                        <div class=\"workbench-search-field\">", "<input id=\"resource\"\n                            name=\"resource\" size=\"48\" type=\"text\" value=", " spellcheck=\"false\"\n                            placeholder=\"<http://\u2026>, prefix:name, _:node or &quot;literal&quot;\" /></div>\n                    </div>\n                    <button id=\"explore-submit\" class=\"workbench-action workbench-action--primary\" type=\"submit\">Explore</button>\n                    ", "\n                    </div>\n                    <div class=\"workbench-action-toolbar__panels workbench-disclosure-track\">", "</div>\n                </form>\n                ", "\n                ", "\n                ", "\n                <p id=\"explore-resource-summary\" class=\"workbench-page-meta\" ?hidden=", ">\n                    <span id=\"explore-resource-value\" hidden>", "</span><span id=\"explore-result-count\">", "</span>\n                </p>\n                <section id=\"explore-results\" class=\"workbench-island workbench-responsive-records\">\n                    ", "\n                    <div id=\"explore-pagination\" class=\"workbench-form-actions\" ?hidden=", ">\n                        <button id=\"previousX\" class=\"workbench-action workbench-action--secondary\" type=\"button\" value=", "\n                            @click=", ">Previous ", "</button>\n                        <button id=\"nextX\" class=\"workbench-action workbench-action--secondary\" type=\"button\" value=", "\n                            @click=", ">Next ", "</button>\n                    </div>\n                </section>"]), text(pageValue(model, 'total-result-count')), icon(runtime, 'search', 'workbench-search-field__icon'), resource, exploreDisplay.owner, exploreDisplay.panel, resultLimited ? h(__makeTemplateObject(["<p id=\"result-limited\">The results shown maybe truncated.</p>"], ["<p id=\"result-limited\">The results shown maybe truncated.</p>"])) : '', errorCallout(runtime, model), resource || exploreResourceKey(model)
                ? exploreResourceCard(runtime, model, summary, resource || exploreResourceKey(model)) : '', !resource, resource, total, total ? h(__makeTemplateObject(["", "", ""], ["", "", ""]), groupedResults, grouped ? exploreRoleGroups(runtime, model, roles)
                : table(runtime, model, context, { linkTerms: true })) : h(__makeTemplateObject(["<p class=\"workbench-empty\" role=\"status\">No results to display.</p>"], ["<p class=\"workbench-empty\" role=\"status\">No results to display.</p>"])), total === 0, 'Previous ' + total, function () { return invoke('workbench.paging.previousOffset', 'explore'); }, total, 'Next ' + total, function () { return invoke('workbench.paging.nextOffset', 'explore'); }, total);
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
        /** Saved query details (M5.6): Yes/No values; the query language only when it is not SPARQL. */
        function savedQueryDetails(row) {
            var yesNo = function (value) { return text(value) === 'true' ? 'Yes' : 'No'; };
            var language = text(row.queryLn);
            var details = language && language.toUpperCase() !== 'SPARQL' ? [['Query language', language]] : [];
            return details.concat([['Include inferred statements', yesNo(row.infer)], ['Shared', yesNo(row.shared)]]);
        }
        function savedQueryContent(runtime, row, index) {
            var h = runtime.html;
            var urn = text(row.query);
            var queryName = text(row.queryName);
            var owner = text(row.user);
            var query = text(row.queryText || row.query);
            var queryTimeout = text(row.queryTimeout).trim() || '0';
            var formId = 'saved-query-exec-' + index;
            return h(__makeTemplateObject(["\n                        <div class=\"saved-query-row__heading\"><h2>", "</h2><span>", "</span></div>\n                        <div class=\"saved-query-actions\">\n                            <form method=\"post\" action=\"query\" id=", "\n                                data-workbench-query-execution=\"true\"\n                                data-workbench-results-target=", ">\n                                <input type=\"hidden\" name=\"action\" value=\"exec\" />\n                                <input type=\"hidden\" name=\"queryLn\" value=", " />\n                                <input type=\"hidden\" name=\"query\" value=", " />\n                                <input type=\"hidden\" name=\"ref\" value=\"id\" />\n                                <input type=\"hidden\" name=\"owner\" value=", " />\n                                <input type=\"hidden\" name=\"infer\" value=", " />\n                                <input type=\"hidden\" name=\"query-timeout\" value=", " />\n                                <span class=\"workbench-action workbench-action--primary\"><label class=\"workbench-action-hit-area\">\n                                    ", "<span class=\"workbench-action-label\"><input type=\"submit\" value=\"Execute\" /></span>\n                                </label></span>\n                            </form>\n                            <button type=\"button\" class=\"saved-query-toggle workbench-action workbench-action--secondary\" id=", " data-query-urn=", "\n                                aria-expanded=\"false\" aria-controls=", ">Show details</button>\n                            <form method=\"post\" action=\"query\"><input type=\"hidden\" name=\"action\" value=\"edit\" />\n                                <input type=\"hidden\" name=\"queryLn\" value=", " /><input type=\"hidden\" name=\"query\" value=", " />\n                                <input type=\"hidden\" name=\"ref\" value=\"id\" /><input type=\"hidden\" name=\"owner\" value=", " />\n                                <input type=\"hidden\" name=\"infer\" value=", " />\n                                <input type=\"hidden\" name=\"query-timeout\" value=", " /><button class=\"workbench-action workbench-action--secondary\" type=\"submit\">Edit</button>\n                            </form>\n                            <form method=\"post\" id=", " action=", ">\n                                <button type=\"button\" class=\"saved-query-delete workbench-action workbench-action--danger-outline\" data-query-owner=", " data-query-name=", "\n                                    data-query-urn=", ">Delete\u2026</button>\n                            </form>\n                        </div>\n                        <div id=", " class=\"query-results\" aria-live=\"polite\"></div>\n                        <div class=\"saved-query-metadata\" id=", " style=\"display: none\">", "</div>\n                        <textarea id=", " style=\"display: none\">", "</textarea>\n                    "], ["\n                        <div class=\"saved-query-row__heading\"><h2>", "</h2><span>", "</span></div>\n                        <div class=\"saved-query-actions\">\n                            <form method=\"post\" action=\"query\" id=", "\n                                data-workbench-query-execution=\"true\"\n                                data-workbench-results-target=", ">\n                                <input type=\"hidden\" name=\"action\" value=\"exec\" />\n                                <input type=\"hidden\" name=\"queryLn\" value=", " />\n                                <input type=\"hidden\" name=\"query\" value=", " />\n                                <input type=\"hidden\" name=\"ref\" value=\"id\" />\n                                <input type=\"hidden\" name=\"owner\" value=", " />\n                                <input type=\"hidden\" name=\"infer\" value=", " />\n                                <input type=\"hidden\" name=\"query-timeout\" value=", " />\n                                <span class=\"workbench-action workbench-action--primary\"><label class=\"workbench-action-hit-area\">\n                                    ", "<span class=\"workbench-action-label\"><input type=\"submit\" value=\"Execute\" /></span>\n                                </label></span>\n                            </form>\n                            <button type=\"button\" class=\"saved-query-toggle workbench-action workbench-action--secondary\" id=", " data-query-urn=", "\n                                aria-expanded=\"false\" aria-controls=", ">Show details</button>\n                            <form method=\"post\" action=\"query\"><input type=\"hidden\" name=\"action\" value=\"edit\" />\n                                <input type=\"hidden\" name=\"queryLn\" value=", " /><input type=\"hidden\" name=\"query\" value=", " />\n                                <input type=\"hidden\" name=\"ref\" value=\"id\" /><input type=\"hidden\" name=\"owner\" value=", " />\n                                <input type=\"hidden\" name=\"infer\" value=", " />\n                                <input type=\"hidden\" name=\"query-timeout\" value=", " /><button class=\"workbench-action workbench-action--secondary\" type=\"submit\">Edit</button>\n                            </form>\n                            <form method=\"post\" id=", " action=", ">\n                                <button type=\"button\" class=\"saved-query-delete workbench-action workbench-action--danger-outline\" data-query-owner=", " data-query-name=", "\n                                    data-query-urn=", ">Delete\u2026</button>\n                            </form>\n                        </div>\n                        <div id=", " class=\"query-results\" aria-live=\"polite\"></div>\n                        <div class=\"saved-query-metadata\" id=", " style=\"display: none\">", "</div>\n                        <textarea id=", " style=\"display: none\">", "</textarea>\n                    "]), queryName, owner, formId, 'saved-query-results-' + index, text(row.queryLn), queryName, owner, text(row.infer), queryTimeout, icon(runtime, 'execute'), urn + '-toggle', urn, urn + '-metadata', text(row.queryLn), queryName, owner, text(row.infer), queryTimeout, urn, 'saved-queries?delete=' + encodeURIComponent(urn), owner, queryName, urn, 'saved-query-results-' + index, urn + '-metadata', keyValueList(runtime, savedQueryDetails(row)), urn + '-text', query);
        }
        /** The formats Export can write; the server's `export-formats` adds file extensions and named-graph support. */
        function exportFormats(model, info) {
            var described = meta(model, 'export-formats');
            if (Array.isArray(described) && described.length) {
                return described.map(function (format) { return ({ value: text(format.value), label: text(format.label),
                    extension: text(format.extension), graphs: format.graphs === true || format.graphs === 'true' }); });
            }
            return formatOptions(info.graphDownloadFormats || info['graph-download-format'])
                .map(function (format) { return ({ value: format.value, label: format.label, extension: '', graphs: null }); });
        }
        /** What happens to the named graphs in the chosen format, so a single-graph format is never a surprise. */
        function exportFormatNote(format, graphCount) {
            if (format.graphs === true) {
                return { text: 'Keeps the named graphs.', warning: false };
            }
            if (format.graphs !== false) {
                return { text: '', warning: false };
            }
            if (graphCount === 0) {
                return { text: format.label + ' does not record graphs; this repository has no named graphs to lose.', warning: false };
            }
            var which = isFinite(graphCount)
                ? (graphCount === 1 ? 'the named graph' : 'all ' + graphCount + ' named graphs') : 'every graph';
            return { text: format.label + ' cannot hold named graphs: the statements of ' + which
                    + ' are written as one graph. Choose N-Quads or TriG to keep them.', warning: true };
        }
        /**
         * Export (user-requested redesign): one card downloads a file and names it on its button, with a note on what
         * the format does with named graphs and the timeout under Advanced settings; a second card previews statements
         * with its own form, so its limit is never mistaken for a download setting. The preview form carries the chosen
         * format and compression along, so previewing never resets them.
         */
        function exportPage(runtime, model, context) {
            var h = runtime.html;
            var info = workbenchData(context);
            var defaults = info.defaults || info;
            var formats = exportFormats(model, info);
            var locationObject = typeof window !== 'undefined' ? window.location : null;
            var urlFormat = locationObject && typeof locationObject.search === 'string'
                ? new URLSearchParams(locationObject.search).get('Accept') : '';
            var explicitFormat = text(urlFormat || meta(model, 'Accept') || pageValue(model, 'Accept'));
            var cookieFormat = currentCookieValue('Accept');
            var configuredFormat = text(meta(model, 'default-export-format') || defaults['default-export-format']);
            var genericFormat = text(meta(model, 'default-Accept') || defaults['default-Accept']);
            var available = function (candidate) { return formats.some(function (format) { return format.value === candidate; }); };
            var holder = model;
            if (!holder.exportChoice) {
                var urlCompression = locationObject && typeof locationObject.search === 'string'
                    ? new URLSearchParams(locationObject.search).get('compression') : '';
                holder.exportChoice = {
                    format: [explicitFormat, cookieFormat, configuredFormat, genericFormat]
                        .filter(function (candidate) { return candidate && available(candidate); })[0]
                        || (formats.length ? formats[0].value : 'application/n-quads'),
                    compression: ['none', 'gzip', 'zip'].indexOf(urlCompression) >= 0 ? urlCompression : 'gzip'
                };
            }
            var choice = holder.exportChoice;
            var format = formats.filter(function (candidate) { return candidate.value === choice.format; })[0]
                || { value: choice.format, label: choice.format, extension: '', graphs: null };
            var extension = format.extension || 'rdf';
            var fileName = choice.compression === 'zip' ? 'export.zip'
                : 'export.' + extension + (choice.compression === 'gzip' ? '.gz' : '');
            var graphCountValue = meta(model, 'named-graph-count');
            var graphCount = graphCountValue === undefined || graphCountValue === null || text(graphCountValue) === ''
                ? NaN : Number(text(graphCountValue));
            var note = exportFormatNote(format, graphCount);
            var compressionHelp = choice.compression === 'zip' ? 'A .zip archive with export.' + extension + ' inside.'
                : choice.compression === 'gzip' ? 'The smallest download. Add RDF and most RDF tools read .gz files directly.'
                    : 'The file is as large as the data.';
            var timeout = text(meta(model, 'export-timeout')) || '43200';
            var safari = isSafari();
            var requested = text(meta(model, 'statement-preview-requested')) === 'true';
            var previewLimit = text(meta(model, 'statement-preview-limit')) || '100';
            var repositoryName = context.repositoryId || 'this repository';
            var rerender = function (event) {
                var outlet = event.currentTarget.closest('.workbench-outlet');
                if (outlet) {
                    render(outlet, model, context, runtime);
                }
            };
            var option = function (candidate) { return h(__makeTemplateObject(["<option value=", " ?selected=", ">", "</option>"], ["<option value=", " ?selected=", ">", "</option>"]), candidate.value, candidate.value === format.value, candidate.label); };
            var keeping = formats.filter(function (candidate) { return candidate.graphs === true; });
            var merging = formats.filter(function (candidate) { return candidate.graphs !== true; });
            return h(__makeTemplateObject(["<form id=\"export-form\" class=\"workbench-island workbench-form-card export-card\" action=\"export\">\n                <div class=\"export-card__header\"><h2>Download a file</h2>\n                    <p class=\"workbench-page-meta\">Writes every statement in ", " to one file.</p></div>\n                <div class=\"workbench-form-grid export-download__fields\">\n                    <div class=\"workbench-field\"><label for=\"Accept\">Format</label>\n                        <div class=\"workbench-select-control\"><select id=\"Accept\" name=\"Accept\" aria-describedby=\"export-format-note\"\n                                @change=", ">", "</select>", "</div>\n                        <p id=\"export-format-note\" class=", "\n                            ?hidden=", ">", "", "</p>\n                    </div>\n                    <fieldset class=\"workbench-segmented export-compression\" aria-describedby=\"export-compression-help\">\n                        <legend>Compression</legend>\n                        ", "\n                        <p id=\"export-compression-help\" class=\"workbench-field__help workbench-segmented__help\">", "</p>\n                        ", "\n                    </fieldset>\n                </div>\n                ", "\n                <div class=\"workbench-form-actions\"><button type=\"submit\" name=\"action\" value=\"download\"\n                        class=\"workbench-action workbench-action--primary\">", "<span>Download <code\n                        class=\"export-file-name\">", "</code></span></button></div>\n            </form>\n            <section id=\"export-results\" class=\"workbench-island workbench-responsive-records export-card\">\n                <div class=\"export-card__header\"><h2>Preview statements</h2>\n                    <p class=\"workbench-page-meta\">Shows the first statements of ", " here. It does not change the\n                        downloaded file.</p></div>\n                <form id=\"export-preview-form\" class=\"export-preview__controls\" action=\"export\">\n                    <input type=\"hidden\" name=\"action\" value=\"preview\" />\n                    <input type=\"hidden\" name=\"Accept\" value=", " />\n                    <input type=\"hidden\" name=\"compression\" value=", " />\n                    <label for=\"limit_export\">Show</label><div class=\"workbench-select-control\">", "", "</div><span\n                        class=\"export-preview__unit\">statements</span>\n                    <button class=\"workbench-action workbench-action--secondary\" type=\"submit\">Show preview</button>\n                </form>\n                <p id=\"result-limited\" class=\"workbench-field__help\" ?hidden=", ">", "</p>\n                ", "\n            </section>"], ["<form id=\"export-form\" class=\"workbench-island workbench-form-card export-card\" action=\"export\">\n                <div class=\"export-card__header\"><h2>Download a file</h2>\n                    <p class=\"workbench-page-meta\">Writes every statement in ", " to one file.</p></div>\n                <div class=\"workbench-form-grid export-download__fields\">\n                    <div class=\"workbench-field\"><label for=\"Accept\">Format</label>\n                        <div class=\"workbench-select-control\"><select id=\"Accept\" name=\"Accept\" aria-describedby=\"export-format-note\"\n                                @change=", ">", "</select>", "</div>\n                        <p id=\"export-format-note\" class=", "\n                            ?hidden=", ">", "", "</p>\n                    </div>\n                    <fieldset class=\"workbench-segmented export-compression\" aria-describedby=\"export-compression-help\">\n                        <legend>Compression</legend>\n                        ", "\n                        <p id=\"export-compression-help\" class=\"workbench-field__help workbench-segmented__help\">", "</p>\n                        ", "\n                    </fieldset>\n                </div>\n                ", "\n                <div class=\"workbench-form-actions\"><button type=\"submit\" name=\"action\" value=\"download\"\n                        class=\"workbench-action workbench-action--primary\">", "<span>Download <code\n                        class=\"export-file-name\">", "</code></span></button></div>\n            </form>\n            <section id=\"export-results\" class=\"workbench-island workbench-responsive-records export-card\">\n                <div class=\"export-card__header\"><h2>Preview statements</h2>\n                    <p class=\"workbench-page-meta\">Shows the first statements of ", " here. It does not change the\n                        downloaded file.</p></div>\n                <form id=\"export-preview-form\" class=\"export-preview__controls\" action=\"export\">\n                    <input type=\"hidden\" name=\"action\" value=\"preview\" />\n                    <input type=\"hidden\" name=\"Accept\" value=", " />\n                    <input type=\"hidden\" name=\"compression\" value=", " />\n                    <label for=\"limit_export\">Show</label><div class=\"workbench-select-control\">", "", "</div><span\n                        class=\"export-preview__unit\">statements</span>\n                    <button class=\"workbench-action workbench-action--secondary\" type=\"submit\">Show preview</button>\n                </form>\n                <p id=\"result-limited\" class=\"workbench-field__help\" ?hidden=", ">", "</p>\n                ", "\n            </section>"]), repositoryName, function (event) { choice.format = event.currentTarget.value; rerender(event); }, keeping.length && merging.length
                ? h(__makeTemplateObject(["<optgroup label=\"Keep named graphs\">", "</optgroup>\n                                    <optgroup label=\"Without named graphs\">", "</optgroup>"], ["<optgroup label=\"Keep named graphs\">", "</optgroup>\n                                    <optgroup label=\"Without named graphs\">", "</optgroup>"]), keeping.map(option), merging.map(option)) : formats.map(option), icon(runtime, 'chevron', 'workbench-select-chevron'), 'workbench-field__help' + (note.warning ? ' workbench-field__help--warning' : ''), !note.text, note.warning ? icon(runtime, 'warning-sign', 'workbench-field__help-icon') : '', note.text, [['none', 'None'], ['gzip', 'Gzip'], ['zip', 'Zip']].map(function (entry) { return h(__makeTemplateObject(["<label for=", ">\n                            <input type=\"radio\" id=", " name=\"compression\" value=", "\n                                ?checked=", "\n                                @change=", " /><span>", "</span>\n                        </label>"], ["<label for=", ">\n                            <input type=\"radio\" id=", " name=\"compression\" value=", "\n                                ?checked=", "\n                                @change=", " /><span>", "</span>\n                        </label>"]), 'compression-' + entry[0], 'compression-' + entry[0], entry[0], choice.compression === entry[0], function (event) { choice.compression = entry[0]; rerender(event); }, entry[1]); }), compressionHelp, safari && choice.compression !== 'none' ? h(__makeTemplateObject(["<p id=\"export-safari-note\"\n                            class=\"workbench-field__help workbench-segmented__help\">Safari expands .gz and .zip downloads after saving\n                            them. To keep the compressed file, turn off <em>Open \u201Csafe\u201D files after downloading</em> in Safari\n                            Settings \u203A General.</p>"], ["<p id=\"export-safari-note\"\n                            class=\"workbench-field__help workbench-segmented__help\">Safari expands .gz and .zip downloads after saving\n                            them. To keep the compressed file, turn off <em>Open \u201Csafe\u201D files after downloading</em> in Safari\n                            Settings \u203A General.</p>"])) : '', workbench.detailDisclosure.render(h, {
                id: 'export-advanced', toggleId: 'export-advanced-toggle',
                panelId: 'export-advanced-panel', label: 'Advanced settings',
                ownerClass: 'workbench-options workbench-form-subgroup'
            }, h(__makeTemplateObject(["<div class=\"workbench-field workbench-disclosure__field\"><label for=\"timeout\">Timeout</label>\n                        <div class=\"workbench-input-unit\"><input id=\"timeout\" name=\"timeout\" type=\"number\" min=\"0\" step=\"1\" required\n                            value=", " aria-describedby=\"export-timeout-unit export-timeout-help\"\n                            @input=", " /><span id=\"export-timeout-unit\" class=\"workbench-input-unit__suffix\">seconds</span></div>\n                        <p class=\"workbench-field__help\"><span id=\"export-timeout-help\" aria-live=\"polite\">", "</span>\n                            \u00B7 the export stops when it takes longer.</p>\n                    </div>"], ["<div class=\"workbench-field workbench-disclosure__field\"><label for=\"timeout\">Timeout</label>\n                        <div class=\"workbench-input-unit\"><input id=\"timeout\" name=\"timeout\" type=\"number\" min=\"0\" step=\"1\" required\n                            value=", " aria-describedby=\"export-timeout-unit export-timeout-help\"\n                            @input=", " /><span id=\"export-timeout-unit\" class=\"workbench-input-unit__suffix\">seconds</span></div>\n                        <p class=\"workbench-field__help\"><span id=\"export-timeout-help\" aria-live=\"polite\">", "</span>\n                            \u00B7 the export stops when it takes longer.</p>\n                    </div>"]), timeout, function (event) {
                var help = event.target.ownerDocument.getElementById('export-timeout-help');
                if (help) {
                    help.textContent = durationLabel(event.target.value);
                }
            }, durationLabel(timeout))), icon(runtime, 'download'), fileName, repositoryName, format.value, choice.compression, limitSelect(runtime, 'limit_export', context, previewLimit), icon(runtime, 'chevron', 'workbench-select-chevron'), !(requested && previewLimit !== '0'
                && rowCount(model) >= Number(previewLimit)), 'Showing the first ' + previewLimit + ' statements.', rowCount(model) ? table(runtime, model, context, {
                labels: { context: 'Graph' },
                // Prefixed names and value tags, as on Explore, keep four columns readable beside the download card.
                cells: function (record) { return (model.vars || []).map(function (name) { return h(__makeTemplateObject(["<td data-label=", ">", "</td>"], ["<td data-label=", ">", "</td>"]), columnLabel(name, { labels: { context: 'Graph' } }), exploreTerm(runtime, record[name], exploreNamespaces(model), true)); }); }
            }) : h(__makeTemplateObject(["<p class=\"workbench-empty\" role=\"status\">", "</p>"], ["<p class=\"workbench-empty\" role=\"status\">", "</p>"]), requested ? 'No statements to show.'
                : 'Choose Show preview to see the first statements.'));
        }
        /** Safari (not another browser built on WebKit's user agent string), which expands downloaded archives. */
        function isSafari() {
            var agent = typeof navigator !== 'undefined' ? String(navigator.userAgent || '') : '';
            return /Safari\//.test(agent) && !/Chrome|Chromium|CriOS|FxiOS|EdgiOS|Edg\/|OPR\/|Android/.test(agent);
        }
        /** A time limit in seconds as people say it: "12 hours", "1 minute 30 seconds", "No limit" for 0. */
        function durationLabel(value) {
            var seconds = Math.floor(Number(text(value)));
            if (!isFinite(seconds) || seconds < 0 || text(value).trim() === '') {
                return '';
            }
            if (seconds === 0) {
                return 'No limit';
            }
            var parts = [];
            [[86400, 'day'], [3600, 'hour'], [60, 'minute'], [1, 'second']].reduce(function (rest, unit) {
                var count = Math.floor(rest / unit[0]);
                if (count) {
                    parts.push(count + ' ' + unit[1] + (count === 1 ? '' : 's'));
                }
                return rest - count * unit[0];
            }, seconds);
            return parts.join(' ');
        }
        views.durationLabel = durationLabel;
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
        /** The Add RDF file field (M5.5, mockup 10): a dropped file becomes the file input's selection. */
        function addDropZone(runtime) {
            var h = runtime.html;
            var active = 'add-drop-zone--active';
            var fileInput = function (zone) { return zone.querySelector('#file'); };
            return h(__makeTemplateObject(["<div id=\"add-drop-zone\" class=\"add-drop-zone\"\n                    @dragover=", "\n                    @dragleave=", "\n                    @drop=", "\n                    @click=", ">\n                <span class=\"add-drop-zone__prompt\">", "<span>Drop an RDF file here or <label for=\"file\"\n                    class=\"add-drop-zone__choose\">choose a file</label></span></span>\n                <span id=\"add-drop-zone-formats\" class=\"add-drop-zone__formats\">Turtle, TriG, N-Triples, N-Quads, RDF/XML, JSON-LD and more; gzip, zip and tar accepted</span>\n                <span class=\"add-drop-zone__file\" aria-live=\"polite\"></span>\n                <input type=\"file\" id=\"file\" name=\"content\" class=\"workbench-visually-hidden\" aria-describedby=\"add-drop-zone-formats\"\n                    @change=", " />\n            </div>"], ["<div id=\"add-drop-zone\" class=\"add-drop-zone\"\n                    @dragover=", "\n                    @dragleave=", "\n                    @drop=", "\n                    @click=", ">\n                <span class=\"add-drop-zone__prompt\">", "<span>Drop an RDF file here or <label for=\"file\"\n                    class=\"add-drop-zone__choose\">choose a file</label></span></span>\n                <span id=\"add-drop-zone-formats\" class=\"add-drop-zone__formats\">Turtle, TriG, N-Triples, N-Quads, RDF/XML, JSON-LD and more; gzip, zip and tar accepted</span>\n                <span class=\"add-drop-zone__file\" aria-live=\"polite\"></span>\n                <input type=\"file\" id=\"file\" name=\"content\" class=\"workbench-visually-hidden\" aria-describedby=\"add-drop-zone-formats\"\n                    @change=", " />\n            </div>"]), function (event) {
                event.preventDefault();
                event.currentTarget.classList.add(active);
            }, function (event) {
                if (!event.currentTarget.contains(event.relatedTarget)) {
                    event.currentTarget.classList.remove(active);
                }
            }, function (event) {
                event.preventDefault();
                var zone = event.currentTarget;
                zone.classList.remove(active);
                var input = fileInput(zone);
                var files = event.dataTransfer && event.dataTransfer.files;
                if (input && !input.disabled && files && files.length) {
                    input.files = files;
                    input.dispatchEvent(new Event('change', { bubbles: true }));
                }
            }, function (event) {
                var input = fileInput(event.currentTarget);
                if (input && !input.disabled && !event.target.closest('label, input')) {
                    input.click();
                }
            }, icon(runtime, 'upload'), function (event) {
                var input = event.target;
                var name = input.closest('.add-drop-zone').querySelector('.add-drop-zone__file');
                name.textContent = input.files && input.files.length ? input.files[0].name : '';
                invoke('workbench.add.enabledInput', 'file');
            });
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
            return h(__makeTemplateObject(["", "\n                ", "\n                <form method=\"post\" action=\"add\" enctype=\"multipart/form-data\" class=\"workbench-form-card\">\n                    <fieldset id=\"add-source-tabs\" class=\"workbench-source-tabs\"><legend>Source</legend>\n                        ", "\n                    </fieldset>\n                    <div class=\"workbench-field-stack add-source-fields\">\n                        <div id=\"add-source-file-panel\" class=\"workbench-field add-source-panel\" data-source=\"file\">\n                            <label for=\"file\">RDF file</label>", "\n                        </div>\n                        <div id=\"add-source-url-panel\" class=\"workbench-field add-source-panel\" data-source=\"url\" hidden>\n                            <label for=\"url\">RDF URL</label><input id=\"url\" name=\"url\" type=\"text\" size=\"48\" disabled\n                                @change=", " />\n                        </div>\n                        <div id=\"add-source-text-panel\" class=\"workbench-field add-source-panel\" data-source=\"text\" hidden>\n                            <label for=\"text\">RDF text</label><textarea id=\"text\" name=\"content\" rows=\"6\" cols=\"70\" disabled></textarea>\n                        </div>\n                    </div>\n                    <div class=\"workbench-form-grid add-target-fields\">\n                        <div class=\"workbench-field add-source-format\"><label for=\"Content-Type\">Data format</label>\n                            <div class=\"workbench-select-control\"><select id=\"Content-Type\" name=\"Content-Type\">\n                                <option id=\"autodetect\" value=\"autodetect\" selected>Detect from file name</option>\n                                ", "\n                            </select>", "</div>\n                        </div>\n                        <div class=\"workbench-field add-target-graph\"><label for=\"context\">Target graph</label>\n                            <input id=\"context\" name=\"context\" type=\"text\" size=\"48\" placeholder=\"Default graph\"\n                                aria-describedby=\"context-help\" value=", " />\n                            <p id=\"context-help\" class=\"workbench-field__help\">Leave empty to keep the graphs named in the data and put the rest in the default graph; a graph IRI puts every statement in that graph.</p>\n                        </div>\n                    </div>\n                    ", "\n                    <div id=\"add-upload-actions\" class=\"workbench-form-actions\"><span class=\"workbench-action workbench-action--primary\">\n                        <label class=\"workbench-action-hit-area\">", "<span class=\"workbench-action-label\"><input type=\"submit\" value=\"Upload\" /></span></label>\n                    </span></div>\n                </form>"], ["", "\n                ", "\n                <form method=\"post\" action=\"add\" enctype=\"multipart/form-data\" class=\"workbench-form-card\">\n                    <fieldset id=\"add-source-tabs\" class=\"workbench-source-tabs\"><legend>Source</legend>\n                        ", "\n                    </fieldset>\n                    <div class=\"workbench-field-stack add-source-fields\">\n                        <div id=\"add-source-file-panel\" class=\"workbench-field add-source-panel\" data-source=\"file\">\n                            <label for=\"file\">RDF file</label>", "\n                        </div>\n                        <div id=\"add-source-url-panel\" class=\"workbench-field add-source-panel\" data-source=\"url\" hidden>\n                            <label for=\"url\">RDF URL</label><input id=\"url\" name=\"url\" type=\"text\" size=\"48\" disabled\n                                @change=", " />\n                        </div>\n                        <div id=\"add-source-text-panel\" class=\"workbench-field add-source-panel\" data-source=\"text\" hidden>\n                            <label for=\"text\">RDF text</label><textarea id=\"text\" name=\"content\" rows=\"6\" cols=\"70\" disabled></textarea>\n                        </div>\n                    </div>\n                    <div class=\"workbench-form-grid add-target-fields\">\n                        <div class=\"workbench-field add-source-format\"><label for=\"Content-Type\">Data format</label>\n                            <div class=\"workbench-select-control\"><select id=\"Content-Type\" name=\"Content-Type\">\n                                <option id=\"autodetect\" value=\"autodetect\" selected>Detect from file name</option>\n                                ", "\n                            </select>", "</div>\n                        </div>\n                        <div class=\"workbench-field add-target-graph\"><label for=\"context\">Target graph</label>\n                            <input id=\"context\" name=\"context\" type=\"text\" size=\"48\" placeholder=\"Default graph\"\n                                aria-describedby=\"context-help\" value=", " />\n                            <p id=\"context-help\" class=\"workbench-field__help\">Leave empty to keep the graphs named in the data and put the rest in the default graph; a graph IRI puts every statement in that graph.</p>\n                        </div>\n                    </div>\n                    ", "\n                    <div id=\"add-upload-actions\" class=\"workbench-form-actions\"><span class=\"workbench-action workbench-action--primary\">\n                        <label class=\"workbench-action-hit-area\">", "<span class=\"workbench-action-label\"><input type=\"submit\" value=\"Upload\" /></span></label>\n                    </span></div>\n                </form>"]), error ? callout(runtime, 'error', error) : '', systemRepositoryCallout(runtime, context), [['file', 'File'], ['url', 'URL'], ['text', 'Text']].map(function (entry) { return h(__makeTemplateObject(["<label for=", ">\n                            ", "\n                            <input type=\"radio\" id=", " name=\"source\"\n                                value=", "\n                                ?checked=", "\n                                @change=", " />\n                            <span>", "</span>\n                        </label>"], ["<label for=", ">\n                            ", "\n                            <input type=\"radio\" id=", " name=\"source\"\n                                value=", "\n                                ?checked=", "\n                                @change=", " />\n                            <span>", "</span>\n                        </label>"]), 'source-' + entry[0], icon(runtime, 'source-' + (entry[0] === 'text' ? 'text' : entry[0])), 'source-' + entry[0], entry[0] === 'text' ? 'contents' : entry[0], entry[0] === 'file', function () { return invoke('workbench.add.enabledInput', entry[0]); }, entry[1]); }), addDropZone(runtime), function () { return invoke('workbench.add.enabledInput', 'url'); }, formats.map(function (format) { return h(__makeTemplateObject(["<option value=", ">", "</option>"], ["<option value=", ">", "</option>"]), format.value, format.label); }), icon(runtime, 'chevron', 'workbench-select-chevron'), text(pageValue(model, 'context')), workbench.detailDisclosure.render(h, {
                id: 'add-import-settings', toggleId: 'add-import-settings-toggle',
                panelId: 'add-import-settings-panel', label: 'Advanced settings',
                ownerClass: 'workbench-options workbench-form-subgroup'
            }, h(__makeTemplateObject(["<div class=\"workbench-form-grid\">\n                            <div class=\"workbench-field workbench-disclosure__field\"><label for=\"baseURI\">Base URI</label>\n                                <input id=\"baseURI\" name=\"baseURI\" type=\"text\" size=\"48\" aria-describedby=\"baseURI-help\"\n                                    value=", " />\n                                <p id=\"baseURI-help\" class=\"workbench-field__help\">Resolves relative IRIs in the data; it does not choose a graph.</p>\n                            </div>\n                            <div class=\"workbench-field workbench-disclosure__field\"><label for=\"transaction-setting__org.eclipse.rdf4j.common.transaction.IsolationLevel\">Isolation level</label>\n                                <select id=\"transaction-setting__org.eclipse.rdf4j.common.transaction.IsolationLevel\"\n                                    name=\"transaction-setting__org.eclipse.rdf4j.common.transaction.IsolationLevel\">\n                                    <option value=\"\" ?selected=", ">Default</option>\n                                    ", "\n                                </select>\n                                <div class=\"hint\">Choose the transaction isolation level used for this import.</div>\n                            </div>\n                        </div>"], ["<div class=\"workbench-form-grid\">\n                            <div class=\"workbench-field workbench-disclosure__field\"><label for=\"baseURI\">Base URI</label>\n                                <input id=\"baseURI\" name=\"baseURI\" type=\"text\" size=\"48\" aria-describedby=\"baseURI-help\"\n                                    value=", " />\n                                <p id=\"baseURI-help\" class=\"workbench-field__help\">Resolves relative IRIs in the data; it does not choose a graph.</p>\n                            </div>\n                            <div class=\"workbench-field workbench-disclosure__field\"><label for=\"transaction-setting__org.eclipse.rdf4j.common.transaction.IsolationLevel\">Isolation level</label>\n                                <select id=\"transaction-setting__org.eclipse.rdf4j.common.transaction.IsolationLevel\"\n                                    name=\"transaction-setting__org.eclipse.rdf4j.common.transaction.IsolationLevel\">\n                                    <option value=\"\" ?selected=", ">Default</option>\n                                    ", "\n                                </select>\n                                <div class=\"hint\">Choose the transaction isolation level used for this import.</div>\n                            </div>\n                        </div>"]), text(pageValue(model, 'baseURI')), !selectedIsolation, isolationOptions.map(function (row) { return h(__makeTemplateObject(["<option value=", "\n                                        ?selected=", ">\n                                        ", "</option>"], ["<option value=", "\n                                        ?selected=", ">\n                                        ", "</option>"]), text(row['isolation-level-option']), text(row['isolation-level-option']) === selectedIsolation, text(row['isolation-level-option-label']) || text(row['isolation-level-option'])); }))), icon(runtime, 'upload'));
        }
        /** Cancel what a page still has pending when its route is disposed: Remove's scheduled or running count. */
        function releasePage(model) {
            var state = model.removeCount;
            if (!state) {
                return;
            }
            clearTimeout(state.timer);
            if (state.controller) {
                state.controller.abort();
            }
            state.timer = null;
            state.controller = null;
        }
        views.releasePage = releasePage;
        var removeFields = [['subj', 'Subject', 'Any subject'], ['pred', 'Predicate', 'Any predicate'],
            ['obj', 'Object', 'Any object']];
        /** "N statements match" for a live Remove count; above the server's limit it says "more than". */
        function removeMatchLabel(state, context) {
            if (state.state !== 'counted') {
                return state.state === 'counting' ? 'Counting…' : state.state === 'timed-out' ? '—' : '';
            }
            if (state.count > 1000000) {
                return 'More than 1,000,000 statements match';
            }
            if (state.count === 0) {
                return 'No statements match';
            }
            return state.count === 1 ? '1 statement matches' : formatCount(String(state.count), context) + ' statements match';
        }
        /**
         * Remove (M6.5, mockup 09): the page counts the explicit statements that match as values are typed (400 ms
         * after the last change, cancelling the previous request), names that number on the button and confirms it in a
         * dialog before the existing POST. The button is disabled while nothing is chosen and when nothing matches.
         */
        function removePage(runtime, model, context) {
            var h = runtime.html;
            var graphs = (model.vars || []).indexOf('error-message') < 0
                ? records(model).filter(function (record) { return !!record.context; }) : [];
            var holder = model;
            var state = holder.removeCount
                || (holder.removeCount = { state: 'empty', count: 0, field: '', message: '', timer: null, controller: null });
            var refresh = function (form) {
                var outlet = form.closest('.workbench-outlet');
                if (outlet) {
                    render(outlet, model, context, runtime);
                }
            };
            var values = function (form) {
                var fields = {};
                ['subj', 'pred', 'obj', 'context'].forEach(function (name) {
                    var control = form.querySelector('[name="' + name + '"]');
                    fields[name] = control ? String(control.value || '').trim() : '';
                });
                return fields;
            };
            var recount = function (form) {
                clearTimeout(state.timer);
                if (state.controller) {
                    state.controller.abort();
                }
                var fields = values(form);
                if (!fields.subj && !fields.pred && !fields.obj && !fields.context) {
                    state.state = 'empty';
                    state.field = '';
                    refresh(form);
                    return;
                }
                state.timer = setTimeout(function () {
                    var app = workbench.app;
                    var controller = typeof AbortController === 'function' ? new AbortController() : null;
                    state.controller = controller;
                    state.state = 'counting';
                    state.field = '';
                    refresh(form);
                    var query = new URLSearchParams({ count: 'true' });
                    Object.keys(fields).forEach(function (name) { if (fields[name]) {
                        query.set(name, fields[name]);
                    } });
                    var fetcher = function (url, options) { return window.fetch(url, controller
                        ? Object.assign({}, options, { signal: controller.signal }) : options); };
                    app.loadModel(fetcher, new URL('remove?' + query.toString(), window.location.href).toString())
                        .then(function (answer) {
                        if (state.controller !== controller) {
                            return answer.rowStore.dispose();
                        }
                        if (answer.error) {
                            state.state = 'invalid';
                            state.field = answer.error.code;
                            state.message = answer.error.message;
                            answer.rowStore.dispose();
                            return refresh(form);
                        }
                        return answer.rowStore.read(0, 1).then(function (rows) {
                            answer.rowStore.dispose();
                            var count = rows.length && rows[0][0] ? Number(text(rows[0][0])) : NaN;
                            state.state = isFinite(count) ? 'counted' : 'timed-out';
                            state.count = isFinite(count) ? count : 0;
                            refresh(form);
                        });
                    }, function (error) {
                        if (state.controller !== controller || (error && error.name === 'AbortError')) {
                            return;
                        }
                        state.state = 'failed';
                        refresh(form);
                    });
                }, 400);
            };
            var counted = state.state === 'counted';
            var disabled = state.state === 'empty' || state.state === 'invalid' || state.state === 'counting'
                || counted && state.count === 0;
            var amount = counted ? (state.count > 1000000 ? 'more than 1,000,000 statements'
                : state.count === 1 ? '1 statement' : formatCount(String(state.count), context) + ' statements') : 'statements';
            var confirmAndSubmit = function (event) {
                event.preventDefault();
                var form = event.currentTarget;
                if (disabled) {
                    return;
                }
                workbench.confirmDialog.open({
                    title: counted ? 'Remove ' + amount + '?' : 'Remove the matching statements?',
                    body: 'This permanently removes ' + (counted ? amount : 'every explicit statement') + ' that match these values.',
                    confirmLabel: 'Remove statements', danger: true
                }).then(function (confirmed) { if (confirmed) {
                    form.submit();
                } });
            };
            var selectedGraph = text(pageValue(model, 'context'));
            return h(__makeTemplateObject(["<form id=\"remove-form\" class=\"workbench-island workbench-form-card\" method=\"post\" action=\"remove\"\n                    @submit=", ">\n                ", "\n                ", "\n                <p>Values use RDF syntax: IRIs in angle brackets, blank nodes as _:nodeID, and literals in double quotes with optional language or datatype.</p>\n                <details id=\"remove-examples\" class=\"workbench-options\"><summary>Examples", "</summary>\n                    <ul><li>URI: <tt>&lt;http://foo.com/bar&gt;</tt></li><li>BNode: <tt>_:nodeID</tt></li>\n                        <li>Literal: <tt>\"Hello\"</tt>, <tt>\"Hello\"@en</tt>, or <tt>\"Hello\"^^&lt;http://bar.com/foo&gt;</tt></li></ul>\n                </details>\n                ", "\n                <div class=\"workbench-field-stack\">\n                    ", "\n                    <div class=\"workbench-field\"><label for=\"context\">Graph</label>\n                        <div class=\"workbench-select-control\"><select id=\"context\" name=\"context\"\n                                @change=", ">\n                            <option value=\"\" ?selected=", ">Any graph</option>\n                            <option value=\"null\" ?selected=", ">Default graph</option>\n                            ", "\n                        </select>", "</div>\n                    </div>\n                </div>\n                <div class=\"workbench-form-actions remove-actions\">\n                    <button type=\"submit\" class=\"workbench-action workbench-action--danger-outline\" ?disabled=", ">", "<span>", "</span></button>\n                    <span id=\"remove-count\" class=\"remove-actions__count\" role=\"status\"\n                        title=", ">", "</span>\n                </div>\n            </form>"], ["<form id=\"remove-form\" class=\"workbench-island workbench-form-card\" method=\"post\" action=\"remove\"\n                    @submit=", ">\n                ", "\n                ", "\n                <p>Values use RDF syntax: IRIs in angle brackets, blank nodes as _:nodeID, and literals in double quotes with optional language or datatype.</p>\n                <details id=\"remove-examples\" class=\"workbench-options\"><summary>Examples", "</summary>\n                    <ul><li>URI: <tt>&lt;http://foo.com/bar&gt;</tt></li><li>BNode: <tt>_:nodeID</tt></li>\n                        <li>Literal: <tt>\"Hello\"</tt>, <tt>\"Hello\"@en</tt>, or <tt>\"Hello\"^^&lt;http://bar.com/foo&gt;</tt></li></ul>\n                </details>\n                ", "\n                <div class=\"workbench-field-stack\">\n                    ", "\n                    <div class=\"workbench-field\"><label for=\"context\">Graph</label>\n                        <div class=\"workbench-select-control\"><select id=\"context\" name=\"context\"\n                                @change=", ">\n                            <option value=\"\" ?selected=", ">Any graph</option>\n                            <option value=\"null\" ?selected=", ">Default graph</option>\n                            ", "\n                        </select>", "</div>\n                    </div>\n                </div>\n                <div class=\"workbench-form-actions remove-actions\">\n                    <button type=\"submit\" class=\"workbench-action workbench-action--danger-outline\" ?disabled=", ">", "<span>", "</span></button>\n                    <span id=\"remove-count\" class=\"remove-actions__count\" role=\"status\"\n                        title=", ">", "</span>\n                </div>\n            </form>"]), confirmAndSubmit, systemRepositoryCallout(runtime, context), callout(runtime, 'warning', 'Every explicit statement that matches the values below is removed; empty fields match anything.', 'Remove is permanent.', 'remove-warning'), icon(runtime, 'chevron', 'workbench-disclosure-chevron'), errorCallout(runtime, model), removeFields.map(function (entry) {
                var invalid = state.state === 'invalid' && state.field === entry[0];
                var onInput = function (event) { return recount(event.currentTarget.form); };
                return h(__makeTemplateObject(["<div class=\"workbench-field\"><label for=", ">", "</label>", "\n                            <p id=", " class=\"workbench-field__error\" ?hidden=", ">", "</p>\n                        </div>"], ["<div class=\"workbench-field\"><label for=", ">", "</label>", "\n                            <p id=", " class=\"workbench-field__error\" ?hidden=", ">", "</p>\n                        </div>"]), entry[0], entry[1], entry[0] === 'obj'
                    ? h(__makeTemplateObject(["<textarea id=\"obj\" name=\"obj\" rows=\"3\" placeholder=", " aria-invalid=", "\n                                aria-describedby=\"obj-error\" @input=", ">", "</textarea>"], ["<textarea id=\"obj\" name=\"obj\" rows=\"3\" placeholder=", " aria-invalid=", "\n                                aria-describedby=\"obj-error\" @input=", ">", "</textarea>"]), entry[2], invalid ? 'true' : 'false', onInput, text(pageValue(model, 'obj'))) : h(__makeTemplateObject(["<input id=", " name=", " type=\"text\" placeholder=", " autocomplete=\"off\"\n                                spellcheck=\"false\" aria-invalid=", " aria-describedby=", "\n                                value=", " @input=", " />"], ["<input id=", " name=", " type=\"text\" placeholder=", " autocomplete=\"off\"\n                                spellcheck=\"false\" aria-invalid=", " aria-describedby=", "\n                                value=", " @input=", " />"]), entry[0], entry[0], entry[2], invalid ? 'true' : 'false', entry[0] + '-error', text(pageValue(model, entry[0])), onInput), entry[0] + '-error', !invalid, invalid ? state.message : '');
            }), function (event) { return recount(event.currentTarget.form); }, !selectedGraph, selectedGraph === 'null', graphs.map(function (record) { return h(__makeTemplateObject(["<option value=", "\n                                ?selected=", ">", "</option>"], ["<option value=", "\n                                ?selected=", ">", "</option>"]), ntriples(record.context), ntriples(record.context) === selectedGraph, termText(record.context)); }), icon(runtime, 'chevron', 'workbench-select-chevron'), disabled, icon(runtime, 'remove'), 'Remove ' + amount + '…', state.state === 'timed-out' ? 'Counting took longer than 2 seconds' : '', removeMatchLabel(state, context));
        }
        /** "N statements", or "—" when the server could not count within its budget. */
        function statementsLabel(count, context) {
            var raw = text(count);
            return raw ? formatCount(raw, context) + (raw === '1' ? ' statement' : ' statements') : '—';
        }
        /** A query parameter of the current page, or '' without a browser location. */
        function locationParameter(name) {
            var location = typeof window !== 'undefined' ? window.location : null;
            return location && typeof location.search === 'string'
                ? new URLSearchParams(location.search).get(name) || '' : '';
        }
        /**
         * Clear (M6.4, mockup 08): what to clear is chosen from the repository, the default graph and each graph, with
         * their sizes; the button names the choice and a dialog confirms it (typing the repository id for everything).
         * The form still posts `context`: empty for the entire repository, "null" for the default graph.
         */
        function clearPage(runtime, model, context) {
            var h = runtime.html;
            var listing = (model.vars || []).indexOf('statements') >= 0 ? records(model) : [];
            var targets = [{ value: '', label: 'Entire repository', count: meta(model, 'repository-size') }]
                .concat(listing.filter(function (record) { return !record.context; })
                .map(function (record) { return ({ value: 'null', label: 'Default graph', count: record.statements }); }))
                .concat(listing.filter(function (record) { return !!record.context; })
                .map(function (record) { return ({ value: ntriples(record.context), label: termText(record.context), count: record.statements }); }));
            var holder = model;
            if (typeof holder.clearTarget !== 'string') {
                var requested_1 = locationParameter('context') || text(pageValue(model, 'context'));
                holder.clearTarget = targets.some(function (target) { return target.value === requested_1; }) ? requested_1 : '';
            }
            var selected = targets.filter(function (target) { return target.value === holder.clearTarget; })[0] || targets[0];
            var everything = selected.value === '';
            var repositoryId = context.repositoryId || '';
            var confirmAndSubmit = function (event) {
                event.preventDefault();
                var form = event.currentTarget;
                var dialog = workbench.confirmDialog;
                var size = text(selected.count);
                var removes = size ? 'This permanently removes ' + statementsLabel(size, context) : 'This permanently removes every statement';
                dialog.open(everything
                    ? { title: 'Clear entire repository?', body: removes + ' from ' + repositoryId + '.',
                        confirmLabel: 'Clear repository', danger: true, requireText: repositoryId,
                        requireLabel: 'Type ' + repositoryId + ' to confirm' }
                    : { title: 'Clear graph?', body: removes + ' from ' + (selected.value === 'null' ? 'the default graph' : selected.label) + '.',
                        confirmLabel: 'Clear graph', danger: true }).then(function (confirmed) {
                    if (confirmed) {
                        form.submit();
                    }
                });
            };
            return h(__makeTemplateObject(["<form id=\"clear-form\" class=\"workbench-island workbench-form-card\" method=\"post\" action=\"clear\"\n                    @submit=", ">\n                ", "\n                ", "\n                ", "\n                <div class=\"workbench-field-stack\">\n                    <div class=\"workbench-field\"><label for=\"context\">What to clear</label>\n                        <div class=\"workbench-select-control\"><select id=\"context\" name=\"context\" @change=", ">", "</select>", "</div>\n                    </div>\n                </div>\n                <div class=\"workbench-form-actions\"><button type=\"submit\" class=\"workbench-action workbench-action--danger\">", "<span>", "</span></button></div>\n            </form>"], ["<form id=\"clear-form\" class=\"workbench-island workbench-form-card\" method=\"post\" action=\"clear\"\n                    @submit=", ">\n                ", "\n                ", "\n                ", "\n                <div class=\"workbench-field-stack\">\n                    <div class=\"workbench-field\"><label for=\"context\">What to clear</label>\n                        <div class=\"workbench-select-control\"><select id=\"context\" name=\"context\" @change=", ">", "</select>", "</div>\n                    </div>\n                </div>\n                <div class=\"workbench-form-actions\"><button type=\"submit\" class=\"workbench-action workbench-action--danger\">", "<span>", "</span></button></div>\n            </form>"]), confirmAndSubmit, systemRepositoryCallout(runtime, context), callout(runtime, 'warning', 'Choose one graph, or the entire repository. There is no undo.', 'This permanently deletes statements.', 'clear-warning'), errorCallout(runtime, model), function (event) {
                holder.clearTarget = event.currentTarget.value;
                var outlet = event.currentTarget.closest('.workbench-outlet');
                if (outlet) {
                    render(outlet, model, context, runtime);
                }
            }, targets.map(function (target) { return h(__makeTemplateObject(["<option value=", " ?selected=", ">", "</option>"], ["<option value=", " ?selected=", ">", "</option>"]), target.value, target === selected, target.label + ' — ' + statementsLabel(target.count, context)); }), icon(runtime, 'chevron', 'workbench-select-chevron'), icon(runtime, 'clear'), everything ? 'Clear entire repository…' : 'Clear graph…');
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
            return h(__makeTemplateObject(["<form id=\"update-form\" class=\"workbench-island\" action=\"update\" method=\"post\"\n                    @submit=", ">\n                ", "\n                <div id=\"update-editor\" class=\"workbench-field\"><label for=\"update\">SPARQL Update</label>\n                    <textarea id=\"update\" name=\"update\" rows=\"16\" cols=\"80\">", "</textarea>\n                    <div id=\"update-editor-resize\" class=\"query-editor-resize\" role=\"separator\"\n                        aria-orientation=\"horizontal\" aria-label=\"Resize editor\" tabindex=\"0\"></div>\n                </div>\n                <div id=\"update-actions\" class=\"workbench-form-actions\"><span class=\"workbench-action workbench-action--primary\">\n                    <label class=\"workbench-action-hit-area\">", "<span class=\"workbench-action-label\"><input type=\"submit\" value=\"Execute\" /></span></label>\n                </span></div>\n            </form>"], ["<form id=\"update-form\" class=\"workbench-island\" action=\"update\" method=\"post\"\n                    @submit=", ">\n                ", "\n                <div id=\"update-editor\" class=\"workbench-field\"><label for=\"update\">SPARQL Update</label>\n                    <textarea id=\"update\" name=\"update\" rows=\"16\" cols=\"80\">", "</textarea>\n                    <div id=\"update-editor-resize\" class=\"query-editor-resize\" role=\"separator\"\n                        aria-orientation=\"horizontal\" aria-label=\"Resize editor\" tabindex=\"0\"></div>\n                </div>\n                <div id=\"update-actions\" class=\"workbench-form-actions\"><span class=\"workbench-action workbench-action--primary\">\n                    <label class=\"workbench-action-hit-area\">", "<span class=\"workbench-action-label\"><input type=\"submit\" value=\"Execute\" /></span></label>\n                </span></div>\n            </form>"]), function (event) {
                var globalWorkbench = window.workbench;
                if (globalWorkbench.update && globalWorkbench.update.doSubmit) {
                    if (!globalWorkbench.update.doSubmit()) {
                        event.preventDefault();
                    }
                }
            }, error ? callout(runtime, 'error', error, undefined, 'updateString.errors')
                : h(__makeTemplateObject(["<span id=\"updateString.errors\" class=\"error\" role=\"alert\"></span>"], ["<span id=\"updateString.errors\" class=\"error\" role=\"alert\"></span>"])), query, icon(runtime, 'execute'));
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
        /** True on Apple platforms, where the Execute shortcut is Cmd+Enter instead of Ctrl+Enter. */
        function isMacPlatform() {
            var navigatorObject = typeof navigator !== 'undefined' ? navigator : null;
            if (!navigatorObject) {
                return false;
            }
            var platform = String((navigatorObject.userAgentData && navigatorObject.userAgentData.platform)
                || navigatorObject.platform || '');
            return platform.indexOf('Mac') >= 0;
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
        /** Menu toggle shown at the start of the primary editor while compare mode hides the navigation. */
        function compareSidebarToggle(runtime, context) {
            var h = runtime.html;
            return h(__makeTemplateObject(["<span id=\"query-sidebar-toggle-action\" class=\"query-sidebar-toggle-action workbench-action workbench-action--secondary\"\n                    data-workbench-action=\"menu\"><span class=\"workbench-action-label\">\n                    <button id=\"query-sidebar-toggle\" class=\"query-sidebar-toggle\" type=\"button\"\n                        aria-hidden=\"true\" aria-expanded=\"false\" aria-controls=\"navigation\" tabindex=\"-1\"\n                        data-show-label=\"Show navigation\" data-hide-label=\"Hide navigation\"\n                        ?hidden=", "\n                        @click=", ">\n                        <span id=\"query-sidebar-toggle-icon\" class=\"query-sidebar-toggle__icon\" aria-hidden=\"true\">\n                            ", "\n                        </span>\n                    </button></span></span>"], ["<span id=\"query-sidebar-toggle-action\" class=\"query-sidebar-toggle-action workbench-action workbench-action--secondary\"\n                    data-workbench-action=\"menu\"><span class=\"workbench-action-label\">\n                    <button id=\"query-sidebar-toggle\" class=\"query-sidebar-toggle\" type=\"button\"\n                        aria-hidden=\"true\" aria-expanded=\"false\" aria-controls=\"navigation\" tabindex=\"-1\"\n                        data-show-label=\"Show navigation\" data-hide-label=\"Hide navigation\"\n                        ?hidden=", "\n                        @click=", ">\n                        <span id=\"query-sidebar-toggle-icon\" class=\"query-sidebar-toggle__icon\" aria-hidden=\"true\">\n                            ", "\n                        </span>\n                    </button></span></span>"]), !queryFeatureEnabled(context, 'editor-sidebar'), function () { return invoke('workbench.query.toggleCompareSidebar'); }, icon(runtime, 'menu', 'query-sidebar-toggle__svg'));
        }
        /** Editor column of the query form; the explanation lives in the output card below the action row. */
        function queryPane(runtime, options, context) {
            var h = runtime.html;
            var compare = !!options.compare;
            var queryId = compare ? 'query-compare' : 'query';
            var suffix = compare ? '-compare' : '';
            var paneId = compare ? 'query-compare-pane' : 'query-primary-pane';
            return h(__makeTemplateObject(["<section id=", " class=", ">\n                <div class=\"query-form__row query-form__row--stacked\">\n                    <div class=\"query-editor-header\">", "<label\n                        class=\"query-form__label\" for=", ">", "</label>\n                        ", "</div>\n                    <div class=\"query-form__field\">", "\n                        <div id=", " class=\"query-editor-resize\"\n                            role=\"separator\" aria-orientation=\"horizontal\" aria-label=\"Resize editor\" tabindex=\"0\"></div></div>\n                </div>\n                <div class=\"query-form__row\"><span class=\"query-form__label query-form__label--blank\"></span>\n                    <div class=\"query-form__field\"><span id=", " class=\"error\">", "</span></div>\n                </div>\n            </section>"], ["<section id=", " class=", ">\n                <div class=\"query-form__row query-form__row--stacked\">\n                    <div class=\"query-editor-header\">", "<label\n                        class=\"query-form__label\" for=", ">", "</label>\n                        ", "</div>\n                    <div class=\"query-form__field\">", "\n                        <div id=", " class=\"query-editor-resize\"\n                            role=\"separator\" aria-orientation=\"horizontal\" aria-label=\"Resize editor\" tabindex=\"0\"></div></div>\n                </div>\n                <div class=\"query-form__row\"><span class=\"query-form__label query-form__label--blank\"></span>\n                    <div class=\"query-form__field\"><span id=", " class=\"error\">", "</span></div>\n                </div>\n            </section>"]), paneId, compare
                ? 'query-compare-pane query-compare-pane--secondary'
                : 'query-compare-pane query-compare-pane--primary', compare ? '' : compareSidebarToggle(runtime, context), queryId, compare ? 'Compare query' : 'Query', compare ? h(__makeTemplateObject(["<button id=\"query-compare-close\" class=\"query-compare-pane__close workbench-action workbench-action--ghost workbench-action--icon\" type=\"button\"\n                            aria-label=\"Close comparison\" title=\"Close comparison\"\n                            @click=", ">", "</button>"], ["<button id=\"query-compare-close\" class=\"query-compare-pane__close workbench-action workbench-action--ghost workbench-action--icon\" type=\"button\"\n                            aria-label=\"Close comparison\" title=\"Close comparison\"\n                            @click=", ">", "</button>"]), function () { return invoke('workbench.query.closeComparePane'); }, icon(runtime, 'close', 'query-compare-pane__close-icon')) : '', compare
                ? h(__makeTemplateObject(["<textarea id=\"query-compare\" rows=\"16\" cols=\"80\" wrap=\"soft\"></textarea>"], ["<textarea id=\"query-compare\" rows=\"16\" cols=\"80\" wrap=\"soft\"></textarea>"])) : h(__makeTemplateObject(["<textarea id=\"query\" name=\"query\" rows=\"16\" cols=\"80\" wrap=\"soft\">", "</textarea>"], ["<textarea id=\"query\" name=\"query\" rows=\"16\" cols=\"80\" wrap=\"soft\">", "</textarea>"]), text(options.query)), compare ? 'query-compare-editor-resize' : 'query-editor-resize', 'queryString.errors' + suffix, compare ? '' : text(options.error));
        }
        /** One explanation column: its status line and the text, DOT and JSON views. */
        function explanationColumn(runtime, compare, explanation, selectedFormat, context) {
            var h = runtime.html;
            var suffix = compare ? '-compare' : '';
            return h(__makeTemplateObject(["<div id=", " class=\"query-explanation-column\"\n                    ?hidden=", "\n                    style=", ">\n                <div class=\"query-explanation-column__header\">\n                    <span class=\"query-explanation-column__label\">", "</span>\n                    ", "\n                </div>\n                <div id=", " class=\"query-explanation-status\" aria-live=\"polite\"></div>\n                <div class=\"query-explanation-surface\"><div id=", "\n                    class=\"query-explanation-overlay\" aria-hidden=\"true\"></div>\n                    ", "\n                    ", "\n                    ", "\n                </div>\n            </div>"], ["<div id=", " class=\"query-explanation-column\"\n                    ?hidden=", "\n                    style=", ">\n                <div class=\"query-explanation-column__header\">\n                    <span class=\"query-explanation-column__label\">", "</span>\n                    ", "\n                </div>\n                <div id=", " class=\"query-explanation-status\" aria-live=\"polite\"></div>\n                <div class=\"query-explanation-surface\"><div id=", "\n                    class=\"query-explanation-overlay\" aria-hidden=\"true\"></div>\n                    ", "\n                    ", "\n                    ", "\n                </div>\n            </div>"]), 'query-explanation-row' + suffix, !queryFeatureEnabled(context, 'query-explain'), compare || !explanation ? 'display:none;' : '', compare ? 'Compare query' : 'Query', compare
                ? h(__makeTemplateObject(["<button id=\"copy-explanation-compare\" class=\"workbench-action workbench-action--ghost workbench-action--icon\" type=\"button\"\n                            aria-label=\"Copy compare explanation\" title=\"Copy compare explanation\"\n                            ?hidden=", ">", "<span\n                            class=\"workbench-visually-hidden\">Copy compare explanation</span></button>"], ["<button id=\"copy-explanation-compare\" class=\"workbench-action workbench-action--ghost workbench-action--icon\" type=\"button\"\n                            aria-label=\"Copy compare explanation\" title=\"Copy compare explanation\"\n                            ?hidden=", ">", "<span\n                            class=\"workbench-visually-hidden\">Copy compare explanation</span></button>"]), !queryFeatureEnabled(context, 'explain-copy'), icon(runtime, 'copy')) : h(__makeTemplateObject(["<button id=\"query-compare-copy\" class=\"workbench-action workbench-action--ghost workbench-action--icon\" type=\"button\"\n                            aria-label=\"Copy query explanation\" title=\"Copy query explanation\"\n                            ?hidden=", ">", "<span\n                            class=\"workbench-visually-hidden\">Copy query explanation</span></button>"], ["<button id=\"query-compare-copy\" class=\"workbench-action workbench-action--ghost workbench-action--icon\" type=\"button\"\n                            aria-label=\"Copy query explanation\" title=\"Copy query explanation\"\n                            ?hidden=", ">", "<span\n                            class=\"workbench-visually-hidden\">Copy query explanation</span></button>"]), !queryFeatureEnabled(context, 'explain-copy'), icon(runtime, 'copy')), 'query-explanation-status' + suffix, 'query-explanation-overlay' + suffix, compare ? h(__makeTemplateObject(["<pre id=\"query-explanation-compare\" data-format=", "\n                        ?hidden=", ">", "</pre>"], ["<pre id=\"query-explanation-compare\" data-format=", "\n                        ?hidden=", ">", "</pre>"]), selectedFormat, !queryFeatureEnabled(context, 'explain-view-text'), explanation) : h(__makeTemplateObject(["<pre id=\"query-explanation\" data-format=", "\n                            ?hidden=", ">", "</pre>"], ["<pre id=\"query-explanation\" data-format=", "\n                            ?hidden=", ">", "</pre>"]), selectedFormat, !queryFeatureEnabled(context, 'explain-view-text'), explanation), compare ? h(__makeTemplateObject(["<div id=\"query-explanation-dot-view-compare\"\n                        ?hidden=", "></div>"], ["<div id=\"query-explanation-dot-view-compare\"\n                        ?hidden=", "></div>"]), !queryFeatureEnabled(context, 'explain-view-dot')) : h(__makeTemplateObject(["<div id=\"query-explanation-dot-view\"\n                            ?hidden=", "></div>"], ["<div id=\"query-explanation-dot-view\"\n                            ?hidden=", "></div>"]), !queryFeatureEnabled(context, 'explain-view-dot')), compare ? h(__makeTemplateObject(["<div id=\"query-explanation-json-view-compare\"\n                        ?hidden=", "></div>"], ["<div id=\"query-explanation-json-view-compare\"\n                        ?hidden=", "></div>"]), !queryFeatureEnabled(context, 'explain-view-json')) : h(__makeTemplateObject(["<div id=\"query-explanation-json-view\"\n                            ?hidden=", "></div>"], ["<div id=\"query-explanation-json-view\"\n                            ?hidden=", "></div>"]), !queryFeatureEnabled(context, 'explain-view-json')));
        }
        /** Explanation tab panel: one toolbar for the plan settings and actions, then one or two plan columns. */
        function explanationPanel(runtime, options, context) {
            var h = runtime.html;
            var explanation = text(options.explanation);
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
            // Config keeps its toggle in the toolbar and opens its panel in a track below the toolbar row,
            // so an open panel never moves the other toolbar controls.
            var explanationSettings = workbench.detailDisclosure.renderSeparated(h, {
                id: 'explanation-settings', toggleId: 'explanation-settings-toggle',
                panelId: 'explanation-settings-panel', label: 'Config', hidden: !queryExplainSettingsEnabled(context),
                ownerClass: 'query-explanation-settings', toggleClass: 'query-explanation-settings__toggle workbench-action workbench-action--secondary',
                panelClass: 'query-explanation-settings__panel',
                panelRole: 'group'
            }, h(__makeTemplateObject(["\n                    <div class=\"query-explanation-settings__section\" ?hidden=", ">\n                        <div class=\"query-explanation-settings__header\"><strong>Highlighting</strong></div>\n                        <div class=\"query-explanation-settings__highlighting\">\n                            <span id=\"explanation-highlight-mode\" class=\"query-explanation-highlight-mode\" role=\"radiogroup\"\n                                aria-label=\"Text explanation highlighting\" ?hidden=", ">\n                                <label class=\"workbench-choice\" for=\"explanation-highlight-syntax\"\n                                    ?hidden=", ">\n                                    <input id=\"explanation-highlight-syntax\" name=\"explanation-highlight-mode\" type=\"radio\" value=\"syntax\" checked\n                                        ?hidden=", " />\n                                    <span>Normal</span>\n                                </label>\n                                <label class=\"workbench-choice\" for=\"explanation-highlight-hotspot\"\n                                    ?hidden=", ">\n                                    <input id=\"explanation-highlight-hotspot\" name=\"explanation-highlight-mode\" type=\"radio\" value=\"hotspot\"\n                                        ?hidden=", " />\n                                    <span>Heatmap</span>\n                                </label>\n                            </span><span id=\"explanation-hotspot-legend\" aria-live=\"polite\"></span>\n                        </div>\n                    </div>\n                    <div id=\"explanation-property-config\" class=\"query-explanation-settings__section query-explanation-property-config\"\n                        ?hidden=", ">\n                        <div class=\"query-explanation-property-config__header\">\n                            <div class=\"query-explanation-property-config__title\">\n                                <strong>Visible properties</strong><span id=\"explanation-property-count\" aria-live=\"polite\"></span>\n                            </div>\n                            <div class=\"query-explanation-property-config__actions\">\n                                <button id=\"explanation-properties-all\" class=\"workbench-action workbench-action--secondary\" type=\"button\" ?hidden=", ">All</button>\n                                <button id=\"explanation-properties-none\" class=\"workbench-action workbench-action--secondary\" type=\"button\" ?hidden=", ">None</button>\n                            </div>\n                        </div>\n                        <div id=\"explanation-property-options\" class=\"query-explanation-property-config__options\"\n                            role=\"group\" aria-label=\"Visible query plan properties\"></div>\n                        <p class=\"query-explanation-property-config__hint\">Plan structure always remains visible.</p>\n                    </div>\n                "], ["\n                    <div class=\"query-explanation-settings__section\" ?hidden=", ">\n                        <div class=\"query-explanation-settings__header\"><strong>Highlighting</strong></div>\n                        <div class=\"query-explanation-settings__highlighting\">\n                            <span id=\"explanation-highlight-mode\" class=\"query-explanation-highlight-mode\" role=\"radiogroup\"\n                                aria-label=\"Text explanation highlighting\" ?hidden=", ">\n                                <label class=\"workbench-choice\" for=\"explanation-highlight-syntax\"\n                                    ?hidden=", ">\n                                    <input id=\"explanation-highlight-syntax\" name=\"explanation-highlight-mode\" type=\"radio\" value=\"syntax\" checked\n                                        ?hidden=", " />\n                                    <span>Normal</span>\n                                </label>\n                                <label class=\"workbench-choice\" for=\"explanation-highlight-hotspot\"\n                                    ?hidden=", ">\n                                    <input id=\"explanation-highlight-hotspot\" name=\"explanation-highlight-mode\" type=\"radio\" value=\"hotspot\"\n                                        ?hidden=", " />\n                                    <span>Heatmap</span>\n                                </label>\n                            </span><span id=\"explanation-hotspot-legend\" aria-live=\"polite\"></span>\n                        </div>\n                    </div>\n                    <div id=\"explanation-property-config\" class=\"query-explanation-settings__section query-explanation-property-config\"\n                        ?hidden=", ">\n                        <div class=\"query-explanation-property-config__header\">\n                            <div class=\"query-explanation-property-config__title\">\n                                <strong>Visible properties</strong><span id=\"explanation-property-count\" aria-live=\"polite\"></span>\n                            </div>\n                            <div class=\"query-explanation-property-config__actions\">\n                                <button id=\"explanation-properties-all\" class=\"workbench-action workbench-action--secondary\" type=\"button\" ?hidden=", ">All</button>\n                                <button id=\"explanation-properties-none\" class=\"workbench-action workbench-action--secondary\" type=\"button\" ?hidden=", ">None</button>\n                            </div>\n                        </div>\n                        <div id=\"explanation-property-options\" class=\"query-explanation-property-config__options\"\n                            role=\"group\" aria-label=\"Visible query plan properties\"></div>\n                        <p class=\"query-explanation-property-config__hint\">Plan structure always remains visible.</p>\n                    </div>\n                "]), allHighlightingDisabled, allHighlightingDisabled, !queryFeatureEnabled(context, 'explain-highlight-syntax'), !queryFeatureEnabled(context, 'explain-highlight-syntax'), !queryFeatureEnabled(context, 'explain-highlight-hotspot'), !queryFeatureEnabled(context, 'explain-highlight-hotspot'), !propertySelectionEnabled, !propertySelectionEnabled, !propertySelectionEnabled));
            return h(__makeTemplateObject(["<div id=\"query-explanation-panel\" class=\"query-output__panel query-explanation-panel\" role=\"tabpanel\"\n                    aria-labelledby=\"query-output-tab-explanation\" tabindex=\"0\" ?hidden=", ">\n                <div class=\"query-explanation-toolbar workbench-action-toolbar\">\n                    <div class=\"query-explanation-toolbar__settings\">\n                        <select id=\"explain-level\" aria-label=\"Plan level\" ?hidden=", ">\n                            ", "\n                        </select><select id=\"explain-format\" name=\"explain-format\" aria-label=\"Plan format\"\n                            ?hidden=", ">\n                            ", "\n                        </select>\n                        ", "\n                    </div>\n                    <div id=\"query-explanation-controls-row\" class=\"query-explanation-controls-row-class query-explanation-toolbar__actions\"\n                        ?hidden=", "\n                        style=", ">\n                        <span id=\"primary-explain-settings\" class=\"query-form__field--controls-group\">\n                            <span id=\"primary-explain-utility-controls\" class=\"query-form__field--controls-group\">\n                                <button id=\"copy-explanation\" class=\"workbench-action workbench-action--ghost workbench-action--icon\" type=\"button\"\n                                    aria-label=\"Copy explanation\" title=\"Copy explanation\"\n                                    ?hidden=", ">", "<span\n                                    class=\"workbench-visually-hidden\">Copy explanation</span></button>\n                                <button id=\"download-explanation\" class=\"workbench-action workbench-action--ghost workbench-action--icon\" type=\"button\"\n                                    aria-label=\"Download explanation\" title=\"Download explanation\" ?disabled=", "\n                                    ?hidden=", ">", "<span\n                                    class=\"workbench-visually-hidden\">Download explanation</span></button>\n                                <button id=\"compare-toggle\" class=\"workbench-action workbench-action--secondary\" type=\"button\" ?hidden=", "\n                                    @click=", ">", "<span>Compare</span></button>\n                            </span>\n                            <span id=\"primary-explain-repeat-controls\" class=\"query-form__field--controls-group\">\n                                <button id=\"rerun-explanation\" class=\"workbench-action workbench-action--secondary\" type=\"button\"\n                                    ?hidden=", "\n                                    data-query-rerun-enabled=", "\n                                    @click=", ">Explain again</button>\n                                <span id=\"rerun-explanation-spinner\" class=\"query-explain-spinner\" aria-hidden=\"true\"></span>\n                                <button id=\"rerun-explanation-cancel\" class=\"query-explain-cancel workbench-action workbench-action--secondary\" type=\"button\" disabled\n                                    ?hidden=", "\n                                    @click=", ">Cancel</button>\n                            </span>\n                        </span>\n                    </div>\n                    <div id=\"query-compare-toolbar\" class=\"query-compare-toolbar\" hidden>\n                        <button id=\"query-compare-swap\" class=\"workbench-action workbench-action--secondary\" type=\"button\"\n                            ?hidden=", ">", "<span>Swap</span></button>\n                        <div id=\"query-compare-controls\" class=\"query-compare-toolbar__actions\">\n                            <button id=\"query-diff-trigger\" class=\"query-compare-action workbench-action workbench-action--secondary\" type=\"button\" disabled\n                                ?hidden=", "\n                                @click=", ">\n                                <span id=\"query-diff-trigger-icon\" class=\"query-compare-action__icon\" aria-hidden=\"true\">\u21C4</span>Diff</button>\n                            <button id=\"explain-compare-trigger\" class=\"query-compare-action workbench-action workbench-action--secondary\" type=\"button\"\n                                data-query-refresh-enabled=", "\n                                ?hidden=", "\n                                @click=", ">Refresh explanations</button>\n                            <button id=\"explain-compare-cancel\" class=\"query-compare-action query-explain-cancel workbench-action workbench-action--secondary\" type=\"button\" disabled\n                                ?hidden=", ">\n                                <span id=\"explain-compare-cancel-icon\" class=\"query-compare-action__svg--cancel\" aria-hidden=\"true\">\u00D7</span>Cancel</button>\n                        </div>\n                    </div>\n                </div>\n                <div class=\"query-explanation-settings-track\">", "</div>\n                <div class=\"query-explanation-columns\">\n                    ", "\n                    ", "\n                </div>\n                <p class=\"query-output__empty query-explanation-panel__empty\" ?hidden=", ">Choose Explain to see how the repository plans this query.</p>\n            </div>"], ["<div id=\"query-explanation-panel\" class=\"query-output__panel query-explanation-panel\" role=\"tabpanel\"\n                    aria-labelledby=\"query-output-tab-explanation\" tabindex=\"0\" ?hidden=", ">\n                <div class=\"query-explanation-toolbar workbench-action-toolbar\">\n                    <div class=\"query-explanation-toolbar__settings\">\n                        <select id=\"explain-level\" aria-label=\"Plan level\" ?hidden=", ">\n                            ", "\n                        </select><select id=\"explain-format\" name=\"explain-format\" aria-label=\"Plan format\"\n                            ?hidden=", ">\n                            ", "\n                        </select>\n                        ", "\n                    </div>\n                    <div id=\"query-explanation-controls-row\" class=\"query-explanation-controls-row-class query-explanation-toolbar__actions\"\n                        ?hidden=", "\n                        style=", ">\n                        <span id=\"primary-explain-settings\" class=\"query-form__field--controls-group\">\n                            <span id=\"primary-explain-utility-controls\" class=\"query-form__field--controls-group\">\n                                <button id=\"copy-explanation\" class=\"workbench-action workbench-action--ghost workbench-action--icon\" type=\"button\"\n                                    aria-label=\"Copy explanation\" title=\"Copy explanation\"\n                                    ?hidden=", ">", "<span\n                                    class=\"workbench-visually-hidden\">Copy explanation</span></button>\n                                <button id=\"download-explanation\" class=\"workbench-action workbench-action--ghost workbench-action--icon\" type=\"button\"\n                                    aria-label=\"Download explanation\" title=\"Download explanation\" ?disabled=", "\n                                    ?hidden=", ">", "<span\n                                    class=\"workbench-visually-hidden\">Download explanation</span></button>\n                                <button id=\"compare-toggle\" class=\"workbench-action workbench-action--secondary\" type=\"button\" ?hidden=", "\n                                    @click=", ">", "<span>Compare</span></button>\n                            </span>\n                            <span id=\"primary-explain-repeat-controls\" class=\"query-form__field--controls-group\">\n                                <button id=\"rerun-explanation\" class=\"workbench-action workbench-action--secondary\" type=\"button\"\n                                    ?hidden=", "\n                                    data-query-rerun-enabled=", "\n                                    @click=", ">Explain again</button>\n                                <span id=\"rerun-explanation-spinner\" class=\"query-explain-spinner\" aria-hidden=\"true\"></span>\n                                <button id=\"rerun-explanation-cancel\" class=\"query-explain-cancel workbench-action workbench-action--secondary\" type=\"button\" disabled\n                                    ?hidden=", "\n                                    @click=", ">Cancel</button>\n                            </span>\n                        </span>\n                    </div>\n                    <div id=\"query-compare-toolbar\" class=\"query-compare-toolbar\" hidden>\n                        <button id=\"query-compare-swap\" class=\"workbench-action workbench-action--secondary\" type=\"button\"\n                            ?hidden=", ">", "<span>Swap</span></button>\n                        <div id=\"query-compare-controls\" class=\"query-compare-toolbar__actions\">\n                            <button id=\"query-diff-trigger\" class=\"query-compare-action workbench-action workbench-action--secondary\" type=\"button\" disabled\n                                ?hidden=", "\n                                @click=", ">\n                                <span id=\"query-diff-trigger-icon\" class=\"query-compare-action__icon\" aria-hidden=\"true\">\u21C4</span>Diff</button>\n                            <button id=\"explain-compare-trigger\" class=\"query-compare-action workbench-action workbench-action--secondary\" type=\"button\"\n                                data-query-refresh-enabled=", "\n                                ?hidden=", "\n                                @click=", ">Refresh explanations</button>\n                            <button id=\"explain-compare-cancel\" class=\"query-compare-action query-explain-cancel workbench-action workbench-action--secondary\" type=\"button\" disabled\n                                ?hidden=", ">\n                                <span id=\"explain-compare-cancel-icon\" class=\"query-compare-action__svg--cancel\" aria-hidden=\"true\">\u00D7</span>Cancel</button>\n                        </div>\n                    </div>\n                </div>\n                <div class=\"query-explanation-settings-track\">", "</div>\n                <div class=\"query-explanation-columns\">\n                    ", "\n                    ", "\n                </div>\n                <p class=\"query-output__empty query-explanation-panel__empty\" ?hidden=", ">Choose Explain to see how the repository plans this query.</p>\n            </div>"]), !explanation, allExplainLevelsDisabled, explainLevels.map(levelOption), allExplainFormatsDisabled, explainFormats.map(formatOption), explanationSettings.owner, !queryFeatureEnabled(context, 'query-explain'), !explanation ? 'display:none;' : '', !queryFeatureEnabled(context, 'explain-copy'), icon(runtime, 'copy'), !explanation, !queryFeatureEnabled(context, 'explain-download'), icon(runtime, 'download'), !queryFeatureEnabled(context, 'query-compare'), function () { return invoke('workbench.query.toggleCompareMode'); }, icon(runtime, 'compare'), !queryFeatureEnabled(context, 'query-rerun'), queryFeatureEnabled(context, 'query-rerun') ? 'true' : 'false', function () { return invoke('workbench.query.runExplain', null, 'rerun-explanation'); }, !queryFeatureEnabled(context, 'explain-cancel'), function () { return invoke('workbench.query.cancelExplain'); }, !queryFeatureEnabled(context, 'query-swap'), icon(runtime, 'swap'), !queryFeatureEnabled(context, 'query-diff'), function () { return invoke('workbench.query.openDiffModal'); }, queryFeatureEnabled(context, 'query-refresh') ? 'true' : 'false', !queryFeatureEnabled(context, 'query-refresh') || !queryExplainEnabled(context), function () { return invoke('workbench.query.runCompareExplain'); }, !queryFeatureEnabled(context, 'explain-cancel'), explanationSettings.panel, explanationColumn(runtime, false, explanation, selectedFormat, context), explanationColumn(runtime, true, '', selectedFormat, context), !!explanation);
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
                panelId: 'query-options-panel', label: 'Query settings', icon: icon(runtime, 'sliders'),
                ownerClass: 'query-disclosure query-options-disclosure',
                toggleClass: 'query-disclosure__toggle workbench-action workbench-action--secondary',
                panelClass: 'query-disclosure__body query-disclosure__panel',
                contentClass: 'workbench-disclosure__fields',
                toggleHidden: !queryFeatureEnabled(context, 'query-options')
            }, h(__makeTemplateObject(["<div id=\"query-timeout-field\" class=\"workbench-disclosure__field query-timeout-field\"\n                    ?hidden=", ">\n                    <label for=\"query-timeout\">Timeout</label>\n                    <div class=\"workbench-input-unit\"><input id=\"query-timeout\" name=\"query-timeout\" type=\"number\" min=\"0\" step=\"1\"\n                        value=", " ?hidden=", "\n                        aria-describedby=\"query-timeout-unit query-timeout-help\" /><span id=\"query-timeout-unit\"\n                        class=\"workbench-input-unit__suffix\">seconds</span></div>\n                    <p id=\"query-timeout-help\" class=\"workbench-field__help\">0 means no limit</p>\n                </div>\n                <div class=\"workbench-disclosure__field\">\n                    <label class=\"query-option\" for=\"infer\"><input id=\"infer\" name=\"infer\" type=\"checkbox\" value=\"true\"\n                        ?checked=", "\n                        ?hidden=", " />\n                        <span>Include inferred statements</span></label>\n                </div>\n                <div class=\"workbench-disclosure__actions query-disclosure__actions\"><button id=\"query-insert-prefixes\"\n                    class=\"workbench-action workbench-action--secondary\" type=\"button\"\n                    title=\"Add a PREFIX line for every repository namespace the query does not declare yet\"\n                    data-editor-namespaces-enabled=", "\n                    ?hidden=", "\n                    @click=", ">Insert prefixes</button></div>"], ["<div id=\"query-timeout-field\" class=\"workbench-disclosure__field query-timeout-field\"\n                    ?hidden=", ">\n                    <label for=\"query-timeout\">Timeout</label>\n                    <div class=\"workbench-input-unit\"><input id=\"query-timeout\" name=\"query-timeout\" type=\"number\" min=\"0\" step=\"1\"\n                        value=", " ?hidden=", "\n                        aria-describedby=\"query-timeout-unit query-timeout-help\" /><span id=\"query-timeout-unit\"\n                        class=\"workbench-input-unit__suffix\">seconds</span></div>\n                    <p id=\"query-timeout-help\" class=\"workbench-field__help\">0 means no limit</p>\n                </div>\n                <div class=\"workbench-disclosure__field\">\n                    <label class=\"query-option\" for=\"infer\"><input id=\"infer\" name=\"infer\" type=\"checkbox\" value=\"true\"\n                        ?checked=", "\n                        ?hidden=", " />\n                        <span>Include inferred statements</span></label>\n                </div>\n                <div class=\"workbench-disclosure__actions query-disclosure__actions\"><button id=\"query-insert-prefixes\"\n                    class=\"workbench-action workbench-action--secondary\" type=\"button\"\n                    title=\"Add a PREFIX line for every repository namespace the query does not declare yet\"\n                    data-editor-namespaces-enabled=", "\n                    ?hidden=", "\n                    @click=", ">Insert prefixes</button></div>"]), !queryFeatureEnabled(context, 'query-timeout'), defaultTimeout, !queryFeatureEnabled(context, 'query-timeout'), text(defaults['default-infer']) === 'true', !queryFeatureEnabled(context, 'query-inferred-statements'), queryFeatureEnabled(context, 'editor-namespaces') ? 'true' : 'false', !queryFeatureEnabled(context, 'editor-namespaces'), function () { return invoke('workbench.query.insertPrefixes'); }));
            return h(__makeTemplateObject(["<div id=\"query-page\" class=\"query-page\"\n                    data-editor-fullscreen-enabled=", ">\n                <form id=\"query-form\" action=\"query\" method=\"post\"\n                    data-workbench-query-execution=\"true\" data-workbench-results-target=\"query-results\">\n                    <input type=\"hidden\" name=\"action\" id=\"action\" />\n                    <input type=\"hidden\" name=\"explain\" id=\"explain\" />\n                    <input type=\"hidden\" name=\"ref\" value=\"text\" />\n                    <input type=\"hidden\" name=\"include-query-text\" id=\"include-query-text\" value=\"false\" />\n                    <input type=\"hidden\" name=\"query-request-id\" id=\"query-request-id\" value=\"\" />\n                    <div class=\"query-form\">\n                        <div id=\"query-language-row\" class=\"query-form__row\"\n                            ?hidden=", ">\n                            <label class=\"query-form__label\" for=\"queryLn\">Query language</label>\n                            <div class=\"query-form__field\"><select id=\"queryLn\" name=\"queryLn\"\n                                @change=", ">\n                                ", "\n                            </select></div>\n                        </div>\n                        <div id=\"query-compare-layout\" class=\"query-compare-layout\"\n                            ?hidden=", ">\n                            ", "\n                            ", "\n                        </div>\n                        <div class=\"query-actions-toolbar workbench-action-toolbar\"><div class=\"query-form__field query-actions-toolbar__primary workbench-action-toolbar__primary\">\n                            <button id=\"exec\" class=\"query-action workbench-action workbench-action--primary\" type=\"submit\"\n                                ?hidden=", "\n                                title=", ">", "<span>Execute</span><kbd\n                                class=\"workbench-shortcut-hint\" aria-hidden=\"true\">", "</kbd></button>\n                            <input id=\"query-cancel\" class=\"query-cancel workbench-action workbench-action--secondary\" type=\"button\" value=\"Cancel\" aria-hidden=\"true\" disabled\n                                ?hidden=", "\n                                @click=", " />\n                            <button id=\"explain-trigger\" class=\"query-action workbench-action workbench-action--secondary\" type=\"button\"\n                                ?hidden=", "\n                                @click=", ">", "<span>Explain</span></button>\n                            <span id=\"explain-trigger-spinner\" class=\"query-explain-spinner\" aria-hidden=\"true\"></span>\n                            <span id=\"explain-trigger-cancel-action\" class=\"workbench-action workbench-action--danger-outline query-explain-cancel\"\n                                ?hidden=", "\n                                data-workbench-action=\"cancel\"><span id=\"explain-trigger-cancel-icon\" aria-hidden=\"true\"></span>\n                                <input id=\"explain-trigger-cancel\" type=\"button\" value=\"Cancel\" aria-hidden=\"true\" disabled\n                                    ?hidden=", "\n                                    @click=", " /></span>\n                        </div>\n                        <div class=\"workbench-action-toolbar__actions\">\n                            <div class=\"workbench-action-toolbar__group\">", "", "</div>\n                        </div>\n                        <div class=\"workbench-action-toolbar__panels workbench-disclosure-track\">\n                            ", "", "\n                        </div>\n                    </div>\n                    </div>\n                </form>\n                <section id=\"query-output\" class=\"query-output workbench-island\" aria-label=\"Query output\" ?hidden=", ">\n                    <div class=\"query-output__tabs\" role=\"tablist\" aria-label=\"Query output\">\n                        <button id=\"query-output-tab-results\" class=\"query-output__tab\" type=\"button\" role=\"tab\"\n                            aria-selected=", " aria-controls=\"query-results-panel\"\n                            tabindex=", "><span>Results</span><span\n                            id=\"query-results-count\" class=\"query-output__badge\" hidden></span></button>\n                        <button id=\"query-output-tab-explanation\" class=\"query-output__tab\" type=\"button\" role=\"tab\"\n                            aria-selected=", " aria-controls=\"query-explanation-panel\"\n                            tabindex=", "\n                            ?hidden=", ">Explanation</button>\n                    </div>\n                    <div id=\"query-results-panel\" class=\"query-output__panel query-results-panel\" role=\"tabpanel\"\n                        aria-labelledby=\"query-output-tab-results\" ?hidden=", ">\n                        <section id=\"query-results\" class=\"query-results\" aria-busy=\"false\" hidden aria-labelledby=\"query-results-heading\">\n                            <div class=\"query-results__header workbench-action-toolbar\">\n                                <div class=\"workbench-action-toolbar__primary\"><h2 id=\"query-results-heading\">Query result</h2></div>\n                                <div class=\"workbench-action-toolbar__actions\">\n                                <button id=\"query-results-fullscreen\" class=\"query-results__fullscreen workbench-action workbench-action--secondary\" type=\"button\"\n                                    aria-label=\"Full screen\" title=\"Full screen\" hidden aria-pressed=\"false\"\n                                    data-result-fullscreen-enabled=", "\n                                    @click=", ">\n                                    <svg class=\"query-results__fullscreen-icon\" viewBox=\"0 0 24 24\" focusable=\"false\" aria-hidden=\"true\">\n                                        <path d=\"M4 9V4h5M15 4h5v5M20 15v5h-5M9 20H4v-5\"></path>\n                                    </svg><span class=\"query-results__fullscreen-label\">Full screen</span>\n                                </button>\n                                </div>\n                            </div>\n                            <div id=\"query-results-loading\" class=\"query-results__loading\" hidden role=\"status\" aria-live=\"polite\">Loading query results...</div>\n                            <div id=\"query-results-status\" class=\"query-results__status\" role=\"status\" aria-live=\"polite\"></div>\n                            <iframe id=\"query-results-frame\" name=\"query-results-frame\" class=\"query-results__frame\"\n                                title=\"Query results\" hidden></iframe>\n                        </section>\n                        <p class=\"query-output__empty query-results-panel__empty\">Choose Execute to see the results here.</p>\n                    </div>\n                    ", "\n                </section>\n                <div id=\"query-diff-modal\" class=\"query-diff-modal\" aria-hidden=\"true\">\n                    <div class=\"query-diff-modal__dialog\" role=\"dialog\" aria-modal=\"true\" aria-labelledby=\"query-diff-modal-title\">\n                        <div class=\"query-diff-modal__header\"><div id=\"query-diff-modal-title\" class=\"query-diff-modal__title\">Query diff</div>\n                            <button id=\"query-diff-close\" class=\"workbench-action workbench-action--secondary\" type=\"button\" value=\"Close\"\n                                @click=", ">Close</button></div>\n                        <div class=\"query-diff-modal__body\">\n                            <section class=\"query-diff-section query-diff-section--query\"><div class=\"query-diff-section__title\">Query</div><div id=\"query-diff-query\" class=\"query-diff-view\"></div></section>\n                            <section class=\"query-diff-section query-diff-section--explanation\"><div class=\"query-diff-section__title\">Explanation</div><div id=\"query-diff-explanation\" class=\"query-diff-view\">Comparison is not ready.</div></section>\n                        </div>\n                    </div>\n                </div>\n            </div>"], ["<div id=\"query-page\" class=\"query-page\"\n                    data-editor-fullscreen-enabled=", ">\n                <form id=\"query-form\" action=\"query\" method=\"post\"\n                    data-workbench-query-execution=\"true\" data-workbench-results-target=\"query-results\">\n                    <input type=\"hidden\" name=\"action\" id=\"action\" />\n                    <input type=\"hidden\" name=\"explain\" id=\"explain\" />\n                    <input type=\"hidden\" name=\"ref\" value=\"text\" />\n                    <input type=\"hidden\" name=\"include-query-text\" id=\"include-query-text\" value=\"false\" />\n                    <input type=\"hidden\" name=\"query-request-id\" id=\"query-request-id\" value=\"\" />\n                    <div class=\"query-form\">\n                        <div id=\"query-language-row\" class=\"query-form__row\"\n                            ?hidden=", ">\n                            <label class=\"query-form__label\" for=\"queryLn\">Query language</label>\n                            <div class=\"query-form__field\"><select id=\"queryLn\" name=\"queryLn\"\n                                @change=", ">\n                                ", "\n                            </select></div>\n                        </div>\n                        <div id=\"query-compare-layout\" class=\"query-compare-layout\"\n                            ?hidden=", ">\n                            ", "\n                            ", "\n                        </div>\n                        <div class=\"query-actions-toolbar workbench-action-toolbar\"><div class=\"query-form__field query-actions-toolbar__primary workbench-action-toolbar__primary\">\n                            <button id=\"exec\" class=\"query-action workbench-action workbench-action--primary\" type=\"submit\"\n                                ?hidden=", "\n                                title=", ">", "<span>Execute</span><kbd\n                                class=\"workbench-shortcut-hint\" aria-hidden=\"true\">", "</kbd></button>\n                            <input id=\"query-cancel\" class=\"query-cancel workbench-action workbench-action--secondary\" type=\"button\" value=\"Cancel\" aria-hidden=\"true\" disabled\n                                ?hidden=", "\n                                @click=", " />\n                            <button id=\"explain-trigger\" class=\"query-action workbench-action workbench-action--secondary\" type=\"button\"\n                                ?hidden=", "\n                                @click=", ">", "<span>Explain</span></button>\n                            <span id=\"explain-trigger-spinner\" class=\"query-explain-spinner\" aria-hidden=\"true\"></span>\n                            <span id=\"explain-trigger-cancel-action\" class=\"workbench-action workbench-action--danger-outline query-explain-cancel\"\n                                ?hidden=", "\n                                data-workbench-action=\"cancel\"><span id=\"explain-trigger-cancel-icon\" aria-hidden=\"true\"></span>\n                                <input id=\"explain-trigger-cancel\" type=\"button\" value=\"Cancel\" aria-hidden=\"true\" disabled\n                                    ?hidden=", "\n                                    @click=", " /></span>\n                        </div>\n                        <div class=\"workbench-action-toolbar__actions\">\n                            <div class=\"workbench-action-toolbar__group\">", "", "</div>\n                        </div>\n                        <div class=\"workbench-action-toolbar__panels workbench-disclosure-track\">\n                            ", "", "\n                        </div>\n                    </div>\n                    </div>\n                </form>\n                <section id=\"query-output\" class=\"query-output workbench-island\" aria-label=\"Query output\" ?hidden=", ">\n                    <div class=\"query-output__tabs\" role=\"tablist\" aria-label=\"Query output\">\n                        <button id=\"query-output-tab-results\" class=\"query-output__tab\" type=\"button\" role=\"tab\"\n                            aria-selected=", " aria-controls=\"query-results-panel\"\n                            tabindex=", "><span>Results</span><span\n                            id=\"query-results-count\" class=\"query-output__badge\" hidden></span></button>\n                        <button id=\"query-output-tab-explanation\" class=\"query-output__tab\" type=\"button\" role=\"tab\"\n                            aria-selected=", " aria-controls=\"query-explanation-panel\"\n                            tabindex=", "\n                            ?hidden=", ">Explanation</button>\n                    </div>\n                    <div id=\"query-results-panel\" class=\"query-output__panel query-results-panel\" role=\"tabpanel\"\n                        aria-labelledby=\"query-output-tab-results\" ?hidden=", ">\n                        <section id=\"query-results\" class=\"query-results\" aria-busy=\"false\" hidden aria-labelledby=\"query-results-heading\">\n                            <div class=\"query-results__header workbench-action-toolbar\">\n                                <div class=\"workbench-action-toolbar__primary\"><h2 id=\"query-results-heading\">Query result</h2></div>\n                                <div class=\"workbench-action-toolbar__actions\">\n                                <button id=\"query-results-fullscreen\" class=\"query-results__fullscreen workbench-action workbench-action--secondary\" type=\"button\"\n                                    aria-label=\"Full screen\" title=\"Full screen\" hidden aria-pressed=\"false\"\n                                    data-result-fullscreen-enabled=", "\n                                    @click=", ">\n                                    <svg class=\"query-results__fullscreen-icon\" viewBox=\"0 0 24 24\" focusable=\"false\" aria-hidden=\"true\">\n                                        <path d=\"M4 9V4h5M15 4h5v5M20 15v5h-5M9 20H4v-5\"></path>\n                                    </svg><span class=\"query-results__fullscreen-label\">Full screen</span>\n                                </button>\n                                </div>\n                            </div>\n                            <div id=\"query-results-loading\" class=\"query-results__loading\" hidden role=\"status\" aria-live=\"polite\">Loading query results...</div>\n                            <div id=\"query-results-status\" class=\"query-results__status\" role=\"status\" aria-live=\"polite\"></div>\n                            <iframe id=\"query-results-frame\" name=\"query-results-frame\" class=\"query-results__frame\"\n                                title=\"Query results\" hidden></iframe>\n                        </section>\n                        <p class=\"query-output__empty query-results-panel__empty\">Choose Execute to see the results here.</p>\n                    </div>\n                    ", "\n                </section>\n                <div id=\"query-diff-modal\" class=\"query-diff-modal\" aria-hidden=\"true\">\n                    <div class=\"query-diff-modal__dialog\" role=\"dialog\" aria-modal=\"true\" aria-labelledby=\"query-diff-modal-title\">\n                        <div class=\"query-diff-modal__header\"><div id=\"query-diff-modal-title\" class=\"query-diff-modal__title\">Query diff</div>\n                            <button id=\"query-diff-close\" class=\"workbench-action workbench-action--secondary\" type=\"button\" value=\"Close\"\n                                @click=", ">Close</button></div>\n                        <div class=\"query-diff-modal__body\">\n                            <section class=\"query-diff-section query-diff-section--query\"><div class=\"query-diff-section__title\">Query</div><div id=\"query-diff-query\" class=\"query-diff-view\"></div></section>\n                            <section class=\"query-diff-section query-diff-section--explanation\"><div class=\"query-diff-section__title\">Explanation</div><div id=\"query-diff-explanation\" class=\"query-diff-view\">Comparison is not ready.</div></section>\n                        </div>\n                    </div>\n                </div>\n            </div>"]), queryFeatureEnabled(context, 'editor-fullscreen') ? 'true' : 'false', hideQueryLanguageRow || !queryFeatureEnabled(context, 'query-language'), function () { return invoke('workbench.query.onQlChange'); }, queryFormats.length ? queryFormats.map(function (option) { return h(__makeTemplateObject(["<option value=", "\n                                    ?selected=", ">", "</option>"], ["<option value=", "\n                                    ?selected=", ">", "</option>"]), option.value, option.value === selectedQueryLanguage, option.label); })
                : h(__makeTemplateObject(["<option value=\"", "\" selected>", "</option>"], ["<option value=\"", "\" selected>", "</option>"]), selectedQueryLanguage, selectedQueryLanguage), !queryFeatureEnabled(context, 'query-compare'), queryPane(runtime, { query: query, error: pageValue(model, 'error-message') }, context), queryPane(runtime, { compare: true }, context), !queryFeatureEnabled(context, 'query-execution'), 'Execute (' + (isMacPlatform() ? 'Cmd' : 'Ctrl') + '+Enter)', icon(runtime, 'execute'), isMacPlatform() ? '⌘↵' : 'Ctrl+↵', !queryFeatureEnabled(context, 'query-cancel'), function () { return invoke('workbench.query.cancelQuery'); }, !queryExplainEnabled(context), function () { return invoke('workbench.query.runExplain', null, 'explain-trigger'); }, icon(runtime, 'explain'), !queryFeatureEnabled(context, 'explain-cancel'), !queryFeatureEnabled(context, 'explain-cancel'), function () { return invoke('workbench.query.cancelExplain'); }, saveDisclosure.owner, optionsDisclosure.owner, saveDisclosure.panel, optionsDisclosure.panel, !explanation, explanation ? 'false' : 'true', explanation ? '-1' : '0', explanation ? 'true' : 'false', explanation ? '0' : '-1', !queryExplainEnabled(context), !!explanation, queryFeatureEnabled(context, 'result-fullscreen') ? 'true' : 'false', function () { return invoke('workbench.query.toggleResultsFullscreen'); }, explanationPanel(runtime, { explanation: explanation, explanationFormat: explanationFormat, explanationLevel: explanationLevel }, context), function () { return invoke('workbench.query.closeDiffModal'); });
        }
        function routeBody(model, context, runtime) {
            if (isRepositoryNotFound(model)) {
                return repositoryNotFoundPage(runtime, context);
            }
            switch (model.viewId) {
                case 'summary': return summaryPage(runtime, model, context);
                case 'information': return informationPage(runtime, model);
                case 'repositories': return repositoriesPage(runtime, model, context);
                case 'create': return createPage(runtime, model, context);
                case 'delete': return deletePage(runtime, model, context);
                case 'namespaces': return namespacesPage(runtime, model, context);
                case 'contexts':
                case 'types': return browseListPage(runtime, model, context);
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
        /** Open the menu sheet from the menu button; close it from its close button or when an item is chosen. */
        function bindMenuSheet(document) {
            var button = document.getElementById('workbench-menu-button');
            var sheet = document.getElementById('workbench-menu-sheet');
            var close = document.getElementById('workbench-menu-close');
            if (!button || !sheet || typeof sheet.showModal !== 'function' || sheet.__rdf4jMenuSheetBound) {
                return function () { };
            }
            sheet.__rdf4jMenuSheetBound = true;
            var onOpen = function () {
                if (!sheet.open) {
                    sheet.showModal();
                    button.setAttribute('aria-expanded', 'true');
                }
            };
            var onCloseButton = function () { return sheet.close(); };
            var onClosed = function () { return button.setAttribute('aria-expanded', 'false'); };
            var onSheetClick = function (event) {
                var target = event.target;
                if (target === sheet || (target && target.closest && target.closest('a[href]'))) {
                    sheet.close();
                }
            };
            button.setAttribute('aria-expanded', 'false');
            button.addEventListener('click', onOpen);
            if (close) {
                close.addEventListener('click', onCloseButton);
            }
            sheet.addEventListener('close', onClosed);
            sheet.addEventListener('click', onSheetClick);
            return function () {
                button.removeEventListener('click', onOpen);
                if (close) {
                    close.removeEventListener('click', onCloseButton);
                }
                sheet.removeEventListener('close', onClosed);
                sheet.removeEventListener('click', onSheetClick);
                sheet.__rdf4jMenuSheetBound = false;
            };
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
            disposers.push(bindMenuSheet(document));
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
            var document = appMount.ownerDocument;
            if (document && 'title' in document) {
                document.title = shellState.title || documentTitle(shellState.viewId, shellState.context.repositoryId);
            }
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
            var outlet = renderShell(mount, { viewId: model.viewId, context: context, title: isRepositoryNotFound(model) ? 'Repository not found — RDF4J Workbench' : undefined }, runtime);
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
            var rowMenuDisposers = [];
            /** Row action menus (repository list): popovers bound once per rendered row element. */
            var bindRowMenus = function () {
                var popover = workbench.popover;
                if (!popover || typeof popover.bind !== 'function') {
                    return;
                }
                elements('[data-workbench-row-menu]').forEach(function (button) {
                    var panel = button.nextElementSibling;
                    if (panel && button.getAttribute('data-workbench-popover-bound') !== 'true') {
                        rowMenuDisposers.push(popover.bind(button, panel));
                    }
                });
            };
            var renderCurrent = function () {
                render(mount, model, context, runtime);
                bindExecutionForms();
                bindPickerControls();
                bindExploreControls();
                bindRowMenus();
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
                bindRowMenus();
            };
            /** A click anywhere on a repository row that is not on a control opens it through its Id link. */
            var onRepositoryRowClick = function (event) {
                var target = event.target;
                if (!target || !target.closest || target.closest('a, button, input, select, textarea, label, .workbench-popover')) {
                    return;
                }
                var selection = targetWindow.getSelection ? targetWindow.getSelection() : null;
                if (selection && String(selection).length) {
                    return;
                }
                var row = target.closest('tr');
                var link = row ? row.querySelector('a.workbench-repository-link') : null;
                if (link) {
                    link.click();
                }
            };
            var repositoryRows = model.viewId === 'repositories' ? elements('#repositories-results')[0] : null;
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
            /** Types and Graphs: request the counts once the list is on screen (M5.2). */
            var loadBrowseCounts = function () {
                var list = browseLists[model.viewId];
                var app = workbench.app;
                var counts = list ? browseCounts(model) : null;
                var href = targetWindow.location && targetWindow.location.href;
                if (!counts || counts.state !== 'pending' || !href || !app || typeof app.loadModel !== 'function') {
                    return;
                }
                var url = new URL(String(href));
                url.searchParams.set('counts', 'true');
                app.loadModel(targetWindow.fetch.bind(targetWindow), url.toString()).then(function (answer) {
                    var timedOut = answer.metadata && text(answer.metadata['counts-timed-out']) === 'true';
                    if (disposed || timedOut) {
                        answer.rowStore.dispose();
                        if (!disposed) {
                            counts.state = 'timed-out';
                            renderCurrent();
                        }
                        return undefined;
                    }
                    if (list.resort) {
                        // The answer is already in count order: show its rows in place of the name-ordered ones.
                        var previous = model.rowStore;
                        model.vars = answer.vars;
                        model.rowStore = answer.rowStore;
                        model.rowCount = answer.rowCount;
                        counts.state = 'done';
                        previous.dispose();
                        return refreshRows(false).then(function () { if (!disposed) {
                            renderCurrent();
                        } });
                    }
                    return answer.rowStore.read(0, answer.rowCount).then(function (rows) {
                        answer.rowStore.dispose();
                        rows.forEach(function (row) {
                            counts.values[row[0] ? ntriples(row[0]) : ''] = row[1];
                        });
                        counts.state = 'done';
                        if (!disposed) {
                            renderCurrent();
                        }
                    });
                }).then(null, function () {
                    counts.state = 'failed';
                    if (!disposed) {
                        renderCurrent();
                    }
                });
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
                loadBrowseCounts();
                if (repositoryRows && repositoryRows.addEventListener) {
                    repositoryRows.addEventListener('click', onRepositoryRowClick);
                }
                if (hasRows && targetWindow.addEventListener) {
                    targetWindow.addEventListener('scroll', onScroll, { passive: true });
                    targetWindow.addEventListener('resize', onResize);
                }
                return function () {
                    disposed = true;
                    generation++;
                    groupGeneration++;
                    disposeExecutionForms();
                    rowMenuDisposers.forEach(function (dispose) { return dispose(); });
                    rowMenuDisposers = [];
                    if (repositoryRows && repositoryRows.removeEventListener) {
                        repositoryRows.removeEventListener('click', onRepositoryRowClick);
                    }
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