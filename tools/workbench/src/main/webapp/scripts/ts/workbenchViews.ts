/// <reference path="template.ts" />
/// <reference path="queryStream.ts" />
/// <reference lib="es2015.collection" />

// WARNING: Do not edit the generated workbenchViews.js file. Edit this source
// and run the Workbench TypeScript compiler instead.

// Ordinary-DOM Lit views for the Workbench shell and non-query routes. The
// existing route names, field names, and script-facing selectors stay stable.
module workbench {
    const actionIconCatalog: { [name: string]: string } = {
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

    function actionIconPath(name: string): string {
        return actionIconCatalog[name] || actionIconCatalog.add;
    }

    const calloutIcons: { [kind: string]: string } = {
        info: 'M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16Zm0 7v5m0-8h.01',
        warning: 'M12 4 2.8 20h18.4L12 4Zm0 6v4.5m0 2.5h.01',
        error: 'M12 3.5a8.5 8.5 0 1 0 0 17 8.5 8.5 0 0 0 0-17ZM12 8v5m0 3h.01'
    };

    function calloutIconPath(kind: string): string {
        return calloutIcons[kind] || calloutIcons.info;
    }

    /** DOM version of the callout component for code that does not render with Lit. */
    export function createCallout(document: any, kind: string, body: string, title?: string): any {
        const namespace = 'http://www.w3.org/2000/svg';
        const element = document.createElement('div');
        element.className = 'workbench-callout workbench-callout--' + (calloutIcons[kind] ? kind : 'info');
        element.setAttribute('role', kind === 'error' ? 'alert' : 'note');
        if (document.createElementNS) {
            const svg = document.createElementNS(namespace, 'svg');
            svg.setAttribute('class', 'workbench-callout__icon');
            svg.setAttribute('viewBox', '0 0 24 24');
            svg.setAttribute('width', '18');
            svg.setAttribute('height', '18');
            svg.setAttribute('focusable', 'false');
            svg.setAttribute('aria-hidden', 'true');
            const path = document.createElementNS(namespace, 'path');
            path.setAttribute('d', calloutIconPath(kind));
            svg.appendChild(path);
            element.appendChild(svg);
        }
        const content = document.createElement('div');
        content.className = 'workbench-callout__body';
        if (title) {
            const strong = document.createElement('strong');
            strong.className = 'workbench-callout__title';
            strong.textContent = title;
            content.appendChild(strong);
            content.appendChild(document.createTextNode(' '));
        }
        content.appendChild(document.createTextNode(body));
        element.appendChild(content);
        return element;
    }

    export module icons {
        export function decorateButton(button: any, name: string, accessibleName: string): void {
            if (!button || !button.ownerDocument || typeof button.setAttribute !== 'function') {
                return;
            }
            const document = button.ownerDocument;
            const namespace = 'http://www.w3.org/2000/svg';
            const knownName = actionIconCatalog[name] ? name : 'add';
            let svg = button.querySelector ? button.querySelector('svg[data-workbench-icon]') : null;
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
            const path = document.createElementNS(namespace, 'path');
            path.setAttribute('d', actionIconPath(knownName));
            svg.appendChild(path);
            button.setAttribute('data-workbench-icon', knownName);
            button.setAttribute('aria-label', accessibleName);
        }

        export function decorateDisclosureButton(button: any, accessibleName: string): void {
            if (!button || !button.ownerDocument || typeof button.appendChild !== 'function') {
                return;
            }
            decorateButton(button, 'chevron', accessibleName);
            const svg = button.querySelector ? button.querySelector('svg[data-workbench-icon]') : null;
            if (!svg) {
                return;
            }
            svg.setAttribute('class', 'workbench-action-icon workbench-action-icon--chevron workbench-disclosure-chevron');
            button.appendChild(svg);
        }
    }

    export module views {
        interface ViewContext {
            basePath: string;
            repositoryId: string;
            workbench: any;
            linked?: any;
            pageModel?: PageModel;
            runtime?: LitRuntime;
            executionFormId?: string;
            resultsMountId?: string;
            rowRegions?: RowRegions;
            /** Locale for number formatting; the browser locale when omitted. */
            locale?: string;
        }

        // The outer template owns each region's Node; a separate Lit root owns its contents.
        interface RowRegions {
            model: PageModel;
            document: Document;
            tableBody?: HTMLElement;
            renderTableRows?: () => void;
            savedList?: HTMLElement;
            savedCards: { [key: string]: HTMLElement };
            renderSavedRows?: () => void;
            groups: { [key: string]: { node: HTMLElement; render: () => void } };
        }

        const rowRegionsByMount = new WeakMap<Element, RowRegions>();

        /** Short page names for the document title; keep in sync with SHORT_TITLES in WorkbenchHtmlShell.java. */
        const shortTitles: { [key: string]: string } = {
            summary: 'Summary', information: 'Information', repositories: 'Repositories',
            create: 'Create repository', delete: 'Delete repository', namespaces: 'Namespaces', contexts: 'Graphs',
            types: 'Types', explore: 'Explore', query: 'Query', 'saved-queries': 'Saved queries', export: 'Export',
            add: 'Add RDF', remove: 'Remove', clear: 'Clear', update: 'SPARQL Update', server: 'Connection'
        };

        /** "Page · repository — RDF4J Workbench", matching the title the server writes into the shell. */
        export function documentTitle(viewId: string, repositoryId: string): string {
            const page = shortTitles[viewId];
            if (!page) {
                return 'RDF4J Workbench';
            }
            const hasRepository = !!repositoryId && repositoryId !== 'NONE';
            return page + (hasRepository ? ' · ' + repositoryId : '') + ' — RDF4J Workbench';
        }

        const titles: { [key: string]: string } = {
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

        function text(value: any): string {
            if (value === null || typeof value === 'undefined') {
                return '';
            }
            if (typeof value === 'object' && value.kind) {
                return termText(value);
            }
            return String(value);
        }

        function termText(term: any): string {
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
        function formatCount(value: any, context: ViewContext): string {
            const format = (workbench as any).format;
            const raw = text(value);
            return format && typeof format.count === 'function' ? format.count(raw, context.locale) : raw;
        }

        function field(record: any, name: string, fallback?: string): any {
            if (record && typeof record[name] !== 'undefined') {
                return record[name];
            }
            if (fallback && record && typeof record[fallback] !== 'undefined') {
                return record[fallback];
            }
            return undefined;
        }

        function records(model: PageModel): any[] {
            return recordsFromRows(model, model.rows || []);
        }

        function recordsFromRows(model: PageModel, rows: any[][]): any[] {
            return rows.map((row: any[]) => {
                const record: any = {};
                (model.vars || []).forEach((name: string, index: number) => {
                    record[name] = row[index];
                });
                return record;
            });
        }

        function rowCount(model: PageModel): number {
            return typeof model.rowCount === 'number' ? model.rowCount : (model.rows || []).length;
        }

        function rowStart(model: PageModel): number {
            return typeof model.rowStart === 'number' ? model.rowStart : 0;
        }

        function rowHeight(model: PageModel): number {
            return model.viewId === 'saved-queries' ? 240 : 44;
        }

        function meta(model: PageModel, name: string, fallback?: string): any {
            return field(model.metadata, name, fallback);
        }

        function pageValue(model: PageModel, name: string, fallback?: string): any {
            const fromMetadata = meta(model, name, fallback);
            return typeof fromMetadata === 'undefined' ? field(firstRecord(model), name, fallback) : fromMetadata;
        }

        function firstRecord(model: PageModel): any {
            const all = records(model);
            return all.length ? all[0] : {};
        }

        function routeTitle(model: PageModel): string {
            return text(meta(model, 'title')) || titles[model.viewId] || 'RDF4J Workbench';
        }

        function urlFor(context: ViewContext, route: string): string {
            const base = (context.basePath || '').replace(/\/+$/, '');
            const special = route === 'server' || route === 'repositories' || route === 'create'
                || route === 'delete' || route === 'information';
            const repositoryBase = base + '/repositories';
            if (special) {
                return repositoryBase + '/NONE/' + route;
            }
            if (context.repositoryId) {
                return repositoryBase + '/' + encodeURIComponent(context.repositoryId) + '/' + route;
            }
            return repositoryBase + '/NONE/' + route;
        }

        function iconPath(name: string): string {
            return actionIconPath(name);
        }

        function icon(runtime: LitRuntime, name: string, className?: string): any {
            const h = runtime.html;
            return h`<svg class=${'workbench-action-icon workbench-action-icon--' + name + (className ? ' ' + className : '')}
                    data-workbench-icon=${name} viewBox="0 0 24 24" width="16" height="16" focusable="false" aria-hidden="true">
                <path d=${iconPath(name)}></path>
            </svg>`;
        }

        function disclosureChevron(runtime: LitRuntime): any {
            return icon(runtime, 'chevron', 'workbench-disclosure-chevron');
        }

        function statusIcon(runtime: LitRuntime, status: string, label: string): any {
            const h = runtime.html;
            const path = status === 'readable'
                ? 'M3 12s3.5-5 9-5 9 5 9 5-3.5 5-9 5-9-5-9-5Zm9-2.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5Z'
                : status === 'writeable'
                    ? 'm4 20 4.5-1 10-10a2.1 2.1 0 0 0-3-3l-10 10L4 20Zm10-13 3 3'
                    : status === 'positive' ? 'm5 12 4 4L19 6' : 'm7 7 10 10m0-10L7 17';
            return h`<svg class=${'workbench-status-icon workbench-status-icon--' + status}
                    viewBox="0 0 24 24" width="16" height="16" role="img" aria-label=${label} focusable="false">
                <title>${label}</title><path d=${path}></path>
            </svg>`;
        }

        function normalizeWorkbench(info: any, linkedInfo?: PageModel): any {
            const source = info || {};
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
            const infoModel = linkedInfo;
            if (!infoModel) {
                return source;
            }
            const sourceRows = records(infoModel);
            const first = sourceRows.length ? sourceRows[0] : {};
            const valueOf = (row: any, name: string): any => {
                const value = row[name];
                if (value && value.kind) {
                    return value.value;
                }
                return value;
            };
            const result: any = {};
            ['id', 'description', 'location', 'server', 'readable', 'writeable'].forEach((key) => {
                result[key] = valueOf(first, key);
            });
            result.defaults = {};
            const firstValue = (key: string): any => {
                for (let index = 0; index < sourceRows.length; index++) {
                    const candidate = valueOf(sourceRows[index], key);
                    if (typeof candidate !== 'undefined' && candidate !== null) {
                        return candidate;
                    }
                }
                return undefined;
            };
            ['default-limit', 'default-queryLn', 'default-infer', 'default-query-timeout',
                'default-workbench-theme', 'default-Accept', 'default-export-format',
                'default-Content-Type', 'default-download-limit'].forEach((key) => {
                result.defaults[key] = firstValue(key);
            });
            result.uploadFormats = [];
            result.queryFormats = [];
            result.graphDownloadFormats = [];
            result.tupleDownloadFormats = [];
            result.booleanDownloadFormats = [];
            const menu: any[] = [];
            const featureMap: any = {};
            sourceRows.forEach((row) => {
                const groupId = valueOf(row, 'menu-group-id');
                const itemId = valueOf(row, 'menu-item-id');
                if (itemId) {
                    let group = menu.filter((candidate) => candidate.id === groupId)[0];
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
                const featureId = valueOf(row, 'query-feature-id');
                if (featureId) {
                    const raw = valueOf(row, 'query-feature-enabled');
                    featureMap[featureId] = raw === true || String(raw) === 'true';
                }
            });
            ['upload-format', 'query-format', 'graph-download-format', 'tuple-download-format',
                'boolean-download-format'].forEach((key) => {
                const target = key === 'upload-format' ? result.uploadFormats
                    : key === 'query-format' ? result.queryFormats
                        : key === 'graph-download-format' ? result.graphDownloadFormats
                            : key === 'tuple-download-format' ? result.tupleDownloadFormats
                                : result.booleanDownloadFormats;
                sourceRows.forEach((row) => {
                    const value = valueOf(row, key);
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
        export function linkedInfoMetadata(infoModel: PageModel): any {
            return normalizeWorkbench({ metadata: infoModel.metadata }, infoModel);
        }

        function menuEntries(context: ViewContext): any[] {
            const info = normalizeWorkbench(context.workbench,
                context.linked && context.linked.info);
            const menu = info.menu || info.menuGroups || info.menuItems;
            const hasMenu = Object.prototype.hasOwnProperty.call(info, 'menu')
                || Object.prototype.hasOwnProperty.call(info, 'menuGroups')
                || Object.prototype.hasOwnProperty.call(info, 'menuItems');
            if (hasMenu && Array.isArray(menu) && menu.length === 0) {
                return [];
            }
            if (Array.isArray(menu) && menu.length && menu[0].items) {
                return menu;
            }
            if (Array.isArray(menu) && menu.length && !menu[0].items) {
                const groups: any[] = [];
                menu.forEach((item: any) => {
                    const id = item.groupId || item.group || item['menu-group-id'] || 'Workbench';
                    let group = groups.filter((candidate) => candidate.id === id)[0];
                    if (!group) {
                        group = {
                            id,
                            label: item.groupLabel || item['menu-group-label'] || id,
                            icon: item.groupIcon || item['menu-group-icon'] || 'modify',
                            items: []
                        };
                        groups.push(group);
                    }
                    group.items.push(item);
                });
                return groups;
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

        function isDisabled(item: any, info: any): boolean {
            const id = item.id || item['menu-item-id'];
            const readable = info.readable !== false && String(info.readable) !== 'false';
            const writeable = info.writeable !== false && String(info.writeable) !== 'false';
            const readRoutes: { [key: string]: boolean } = {
                summary: true, namespaces: true, contexts: true, types: true, explore: true,
                query: true, 'saved-queries': true, export: true
            };
            const writeRoutes: { [key: string]: boolean } = {
                update: true, add: true, remove: true, clear: true
            };
            return !!item.disabled || (!!readRoutes[id] && !readable) || (!!writeRoutes[id] && !writeable);
        }

        /** Groups with more items than this keep a disclosure so long custom menus stay manageable. */
        const navigationDisclosureThreshold = 12;

        function navigationItem(runtime: LitRuntime, context: ViewContext, info: any, item: any,
                                groupLabel: string, active: string): any {
            const h = runtime.html;
            const id = text(item.id || item['menu-item-id']);
            const label = text(item.label || item['menu-item-label'] || id);
            const href = item.href || item['menu-item-href'] || urlFor(context, id);
            return h`<li class=${active === id ? 'current' : ''}>
                ${isDisabled(item, info)
                    ? h`<span class="disabled" title=${groupLabel}>${icon(runtime, item.icon || item['menu-item-icon'] || id)}${label}</span>`
                    : h`<a href=${href} data-workbench-nav-href=${href}
                        title=${groupLabel} aria-current=${active === id ? 'page' : 'false'}>
                        ${icon(runtime, item.icon || item['menu-item-icon'] || id)}${label}
                    </a>`}
            </li>`;
        }

        /** Every group renders the same way: a non-interactive label followed by its items (M2.4). */
        function navigation(context: ViewContext, active: string, runtime: LitRuntime): any {
            const h = runtime.html;
            const info = normalizeWorkbench(context.workbench, context.linked && context.linked.info);
            return menuEntries(context).filter((group: any) => (group.items || []).length > 0).map((group: any) => {
                const items = group.items || [];
                const groupId = text(group.id || group['menu-group-id'] || 'workbench');
                const groupLabel = text(group.label || group['menu-group-label'] || 'Workbench');
                const list = h`<ul id=${'workbench-nav-items-' + groupId} class="group" aria-labelledby=${'workbench-nav-label-' + groupId}>
                    ${items.map((item: any) => navigationItem(runtime, context, info, item, groupLabel, active))}
                </ul>`;
                if (items.length > navigationDisclosureThreshold) {
                    const containsActive = items.some((item: any) => text(item.id || item['menu-item-id']) === active);
                    return h`<li class="workbench-nav-group workbench-nav-group--long" data-workbench-menu-group=${groupId}
                            data-workbench-menu-label=${groupLabel}>
                        <details class="workbench-nav-group__disclosure" ?open=${containsActive}>
                            <summary id=${'workbench-nav-summary-' + groupId} aria-controls=${'workbench-nav-items-' + groupId}
                                    class="workbench-nav-group__summary">
                                <span id=${'workbench-nav-label-' + groupId} class="workbench-nav-group__label">${groupLabel}</span>
                                ${icon(runtime, 'chevron', 'workbench-nav-group__chevron workbench-disclosure-chevron')}
                            </summary>
                            ${list}
                        </details>
                    </li>`;
                }
                return h`<li class="workbench-nav-group" data-workbench-menu-group=${groupId}
                        data-workbench-menu-label=${groupLabel}>
                    <span id=${'workbench-nav-label-' + groupId} class="workbench-nav-group__label">${groupLabel}</span>
                    ${list}
                </li>`;
            });
        }

        /** Decodes the user name from the server-user-password cookie ("user:password" in base64). */
        function serverUser(): string {
            if (typeof document === 'undefined') {
                return '';
            }
            const encoded = currentCookieValue('server-user-password');
            if (!encoded) {
                return '';
            }
            let decoded = encoded;
            try {
                decoded = typeof window !== 'undefined' && typeof window.atob === 'function' ? window.atob(encoded) : encoded;
            } catch (error) {
                return '';
            }
            const user = decoded.indexOf(':') >= 0 ? decoded.substring(0, decoded.indexOf(':')) : decoded;
            return user === '""' ? '' : user;
        }

        function hostAndPort(server: string): string {
            try {
                return new URL(server).host || server;
            } catch (error) {
                return server;
            }
        }

        /** Shell state for the context bar: server, repository and user. */
        export function contextBarState(context: ViewContext): any {
            const info = normalizeWorkbench(context.workbench, context.linked && context.linked.info);
            const contextRepository = context.repositoryId && context.repositoryId !== 'NONE' ? context.repositoryId : '';
            const repositoryId = text(info.id || contextRepository);
            return {
                server: text(info.server || info.location || ''),
                repositoryId: repositoryId === 'NONE' ? '' : repositoryId,
                repositoryTitle: text(info.description || info.title),
                user: serverUser()
            };
        }

        function switcherChevron(runtime: LitRuntime): any {
            return icon(runtime, 'chevron', 'workbench-switcher__chevron');
        }

        /** The 56px context bar (mockups 01 and 04): brand, server, repository and user switchers. */
        function contextBar(context: ViewContext, active: string, runtime: LitRuntime): any {
            const h = runtime.html;
            const state = contextBarState(context);
            const server = state.server;
            return h`<header id="workbench-contextbar" class="workbench-contextbar">
                <a id="logo" class="workbench-brand" href=${urlFor(context, 'repositories')} aria-label="RDF4J Workbench home">
                    <img class="workbench-brand__light" src=${context.basePath + '/images/logo.png'} alt="rdf4j" />
                    <img class="product workbench-brand__light" src=${context.basePath + '/images/product.png'} alt="workbench" />
                    <img class="workbench-brand__dark" src=${context.basePath + '/images/logo-dark.png'} alt="rdf4j" />
                    <img class="product workbench-brand__dark" src=${context.basePath + '/images/product-dark.png'} alt="workbench" />
                </a>
                <div class="workbench-switcher" data-workbench-switcher="server">
                    <button id="workbench-server-switcher" class="workbench-switcher__button" type="button"
                            aria-haspopup="dialog" aria-expanded="false" aria-controls="workbench-server-popover"
                            title=${server || 'No server'}>
                        ${icon(runtime, 'server')}<span class="workbench-switcher__key">Server</span>
                        <span class="workbench-switcher__value">${server ? hostAndPort(server) : 'None'}</span>
                        ${switcherChevron(runtime)}
                    </button>
                    <div id="workbench-server-popover" class="workbench-popover" role="dialog" aria-label="Server" hidden>
                        <dl class="workbench-kv">
                            <div class="workbench-kv__row"><dt>Server</dt><dd class="workbench-kv__code">${server || 'None'}</dd></div>
                            <div class="workbench-kv__row"><dt>User</dt><dd>${state.user || 'Not signed in'}</dd></div>
                        </dl>
                        <div class="workbench-popover__footer">
                            <a href=${urlFor(context, 'server')}>${icon(runtime, 'settings')}Change server or user…</a>
                        </div>
                    </div>
                </div>
                <span class="workbench-contextbar__divider" aria-hidden="true"></span>
                <div class="workbench-switcher" data-workbench-switcher="repository">
                    <button id="workbench-repository-switcher" class="workbench-switcher__button" type="button"
                            aria-haspopup="dialog" aria-expanded="false" aria-controls="workbench-repository-popover">
                        ${icon(runtime, 'repository')}<span class="workbench-switcher__key">Repository</span>
                        ${state.repositoryId
                            ? h`<span class="workbench-switcher__value"><span class="workbench-switcher__id">${state.repositoryId}</span>${state.repositoryTitle
                                ? h`<span class="workbench-switcher__title">${state.repositoryTitle}</span>` : ''}</span>`
                            : h`<span class="workbench-switcher__value workbench-switcher__value--empty">No repository</span>`}
                        ${switcherChevron(runtime)}
                    </button>
                    <div id="workbench-repository-popover" class="workbench-popover workbench-popover--repositories"
                            role="dialog" aria-label="Choose repository" hidden
                            data-workbench-active-view=${active} data-workbench-repositories-url=${urlFor(context, 'repositories')}
                            data-workbench-base-path=${(context.basePath || '').replace(/\/+$/, '')}>
                        <div class="workbench-popover__search">${icon(runtime, 'search')}
                            <input id="workbench-repository-filter" type="search" placeholder="Find repository"
                                aria-label="Find repository" autocomplete="off" aria-controls="workbench-repository-options" />
                        </div>
                        <ul id="workbench-repository-options" class="workbench-popover__list" aria-label="Repositories"></ul>
                        <p class="workbench-popover__status" role="status" aria-live="polite"></p>
                        <div class="workbench-popover__footer">
                            <a href=${urlFor(context, 'repositories')}>${icon(runtime, 'repository')}All repositories</a>
                            <a href=${urlFor(context, 'create')}>${icon(runtime, 'create')}Create repository</a>
                        </div>
                    </div>
                </div>
                <span class="workbench-contextbar__spacer"></span>
                <div class="workbench-switcher" data-workbench-switcher="user">
                    <button id="workbench-user-switcher" class="workbench-switcher__button workbench-switcher__button--user"
                            type="button" aria-haspopup="dialog" aria-expanded="false" aria-controls="workbench-user-popover">
                        ${icon(runtime, 'user')}<span id="selected-user" class="workbench-switcher__value">${state.user || 'Not signed in'}</span>
                        ${switcherChevron(runtime)}
                    </button>
                    <div id="workbench-user-popover" class="workbench-popover workbench-popover--end" role="dialog" aria-label="User" hidden>
                        <p class="workbench-popover__text">${state.user ? h`Signed in as <strong>${state.user}</strong>` : 'Not signed in'}</p>
                        <div class="workbench-popover__footer">
                            <a href=${urlFor(context, 'server')}>${icon(runtime, 'settings')}Change server or user…</a>
                        </div>
                    </div>
                </div>
            </header>`;
        }

        /** The state that the persistent shell (header, menu, footer) depends on. */
        export interface ShellState {
            viewId: string;
            context: ViewContext;
        }

        /** Persistent shell around the outlet; `outlet` is the outlet node or, without a DOM, its template. */
        function shellTemplate(state: ShellState, runtime: LitRuntime, outlet: any): any {
            const h = runtime.html;
            const context = state.context;
            return h`${contextBar(context, state.viewId, runtime)}
            <details id="workbench-navigation-disclosure" class="workbench-navigation-disclosure" open>
                <summary id="workbench-navigation-summary">
                    <svg class="workbench-menu-icon" viewBox="0 0 24 24" width="18" height="18" focusable="false" aria-hidden="true">
                        <path d="M4 6h16M4 12h16M4 18h16"></path>
                    </svg><span>Menu</span>
                    <svg class="workbench-menu-chevron workbench-disclosure-chevron" viewBox="0 0 24 24" width="18" height="18" focusable="false" aria-hidden="true">
                        <path d="m6 9 6 6 6-6"></path>
                    </svg>
                </summary>
                <div id="navigation" class="workbench-nav"><ul class="maingroup">
                    ${navigation(context, state.viewId, runtime)}
                </ul></div>
            </details>
            <main id="content" class="workbench-main">${outlet}</main>
            <div id="footer" class="workbench-footer"><div>Copyright © Eclipse RDF4J contributors</div></div>`;
        }

        /** The page area that changes from route to route: title, noscript notice and page surface. */
        function outletContentTemplate(model: PageModel, runtime: LitRuntime, body: any): any {
            const h = runtime.html;
            return h`<h1 id="title_heading">${routeTitle(model)}</h1>
                <p id="noscript-message" class="ERROR">Scripting is not enabled. The RDF4J Workbench application requires scripting to be enabled.</p>
                <div id="workbench-page-surface" class="workbench-page-surface">${body}</div>`;
        }

        function shell(model: PageModel, context: ViewContext, runtime: LitRuntime, body: any): any {
            const h = runtime.html;
            return shellTemplate({ viewId: model.viewId, context }, runtime,
                h`<div id="workbench-outlet" class="workbench-outlet" tabindex="-1">${outletContentTemplate(model, runtime, body)}</div>`);
        }

        function workbenchData(context: ViewContext): any {
            return normalizeWorkbench(context.workbench, context.linked && context.linked.info);
        }

        /** People-facing column label for a page table; options.labels overrides the default. */
        function columnLabel(name: string, options?: any): string {
            const labels = options && options.labels;
            if (labels && typeof labels[name] === 'string') {
                return labels[name];
            }
            return name ? name.charAt(0).toUpperCase() + name.substring(1) : name;
        }

        function table(runtime: LitRuntime, model: PageModel, context: ViewContext,
                       options?: any): any {
            const h = runtime.html;
            const columns = model.vars || [];
            const allRows = records(model);
            const total = rowCount(model);
            const emptyText = options && options.emptyText ? options.emptyText : 'No results to display.';
            const regions = context.rowRegions;
            if (regions) {
                if (!regions.tableBody) { regions.tableBody = regions.document.createElement('tbody'); }
                regions.renderTableRows = () => runtime.render(tableRows(runtime, model, context, options,
                    records(model), rowStart(model), rowCount(model), emptyText), regions.tableBody);
            }
            return h`<table class="data" data-workbench-row-table=${model.rowStore && total ? 'true' : runtime.nothing}>
                ${columns.length ? h`<thead><tr>${columns.map((name: string) => h`<th scope="col">${columnLabel(name, options)}</th>`)}</tr></thead>` : ''}
                ${regions ? regions.tableBody
                    : h`<tbody>${tableRows(runtime, model, context, options, allRows, rowStart(model), total, emptyText)}</tbody>`}
            </table>`;
        }

        function tableRows(runtime: LitRuntime, model: PageModel, context: ViewContext,
                           options: any, allRows: any[], start: number, total: number, emptyText: string): any {
            const h = runtime.html;
            const columns = model.vars || [];
            const rowHeightValue = rowHeight(model);
            if (!total && !allRows.length) {
                return h`<tr class="workbench-empty-row"><td role="status" colspan=${Math.max(1, columns.length)}>${emptyText}</td></tr>`;
            }
            if (!allRows.length) {
                return h`<tr class="workbench-pending-row"><td role="status" colspan=${Math.max(1, columns.length)}>Loading rows...</td></tr>`;
            }
            const before = typeof model.rowTopSpacer === 'number'
                ? model.rowTopSpacer : Math.max(0, start) * rowHeightValue;
            const after = typeof model.rowBottomSpacer === 'number'
                ? model.rowBottomSpacer : Math.max(0, total - start - allRows.length) * rowHeightValue;
            return h`${before ? h`<tr class="workbench-virtual-spacer" aria-hidden="true"><td colspan=${Math.max(1, columns.length)}
                    style=${'height:' + before + 'px;padding:0;border:0'}></td></tr>` : ''}
                ${allRows.map((record: any, relativeIndex: number) => h`<tr data-workbench-row-index=${start + relativeIndex}>
                    ${columns.map((name: string) => {
                        const cell = record[name];
                        if (options && options.status && (name === 'readable' || name === 'writeable')) {
                            const status = name === 'readable' ? 'readable' : 'writeable';
                            return h`<td data-label=${name}>${statusIcon(runtime,
                                (cell === true || text(cell) === 'true') ? status : 'negative',
                                (cell === true || text(cell) === 'true') ? (name === 'readable' ? 'Readable' : 'Writeable') : 'No')}</td>`;
                        }
                        if (options && options.repository && name === 'id') {
                            const id = text(cell);
                            const href = '../' + encodeURIComponent(id) + '/summary';
                            return h`<td data-label=${name}><a href=${href}>${id}</a></td>`;
                        }
                        return h`<td data-label=${name}>${renderTerm(runtime, cell, context, !!(options && options.linkTerms))}</td>`;
                    })}
                </tr>`)}
                ${after ? h`<tr class="workbench-virtual-spacer" aria-hidden="true"><td colspan=${Math.max(1, columns.length)}
                    style=${'height:' + after + 'px;padding:0;border:0'}></td></tr>` : ''}`;
        }

        function renderTerm(runtime: LitRuntime, value: any, context: ViewContext, link: boolean): any {
            const h = runtime.html;
            if (!value || typeof value !== 'object' || !value.kind) {
                return text(value);
            }
            const display = termText(value);
            if (link && (value.kind === 'iri' || value.kind === 'literal' || value.kind === 'bnode')) {
                const resource = workbench.queryStream.exploreResource(value);
                return h`<a href=${'explore?resource=' + encodeURIComponent(resource)}>${display}</a>`;
            }
            if (value.kind === 'literal' && value.language) {
                return h`<span class="rdf-literal" lang=${value.language}>${display}<span class="rdf-language">@${value.language}</span></span>`;
            }
            return display;
        }

        /** Shared key/value list (mockup 11): label column and value column, stacked below 600px. */
        function keyValueList(runtime: LitRuntime, rows: [string, any][]): any {
            const h = runtime.html;
            return h`<dl class="workbench-kv">${rows.map((row) => h`<div class="workbench-kv__row"><dt>${row[0]}</dt><dd>${
                row[1] && typeof row[1] === 'object' && Array.isArray(row[1].strings) ? row[1] : text(row[1])}</dd></div>`)}</dl>`;
        }

        /** Callout (mockup 11): info and warning use role="note", errors use role="alert". */
        function callout(runtime: LitRuntime, kind: string, body: any, title?: string, id?: string): any {
            const h = runtime.html;
            const content = h`<svg class="workbench-callout__icon" viewBox="0 0 24 24" width="18" height="18"
                    focusable="false" aria-hidden="true"><path d=${calloutIconPath(kind)}></path></svg>
                <div class="workbench-callout__body">${title
                    ? h`<strong class="workbench-callout__title">${title}</strong> ` : ''}${body}</div>`;
            if (kind === 'error') {
                return id
                    ? h`<div id="${id}" class="workbench-callout workbench-callout--error" role="alert">${content}</div>`
                    : h`<div class="workbench-callout workbench-callout--error" role="alert">${content}</div>`;
            }
            if (kind === 'warning') {
                return id
                    ? h`<div id="${id}" class="workbench-callout workbench-callout--warning" role="note">${content}</div>`
                    : h`<div class="workbench-callout workbench-callout--warning" role="note">${content}</div>`;
            }
            return id
                ? h`<div id="${id}" class="workbench-callout workbench-callout--info" role="note">${content}</div>`
                : h`<div class="workbench-callout workbench-callout--info" role="note">${content}</div>`;
        }

        function errorCallout(runtime: LitRuntime, model: PageModel): any {
            const message = text(pageValue(model, 'error-message'));
            return message ? callout(runtime, 'error', message) : '';
        }

        function systemRepositoryCallout(runtime: LitRuntime, context: ViewContext): any {
            return context.repositoryId === 'SYSTEM'
                ? callout(runtime, 'warning', 'The SYSTEM repository is intended for system use.') : '';
        }

        function simpleSection(runtime: LitRuntime, title: string, rows: [string, any][], className?: string,
                               id?: string, island: boolean = true): any {
            const h = runtime.html;
            const content = h`<h2>${title}</h2>${keyValueList(runtime, rows)}`;
            const sectionClass = [island ? 'workbench-island' : '', className || '']
                .filter((part) => part.length > 0).join(' ');
            return id
                ? h`<section id=${id} class=${sectionClass}>${content}</section>`
                : h`<section class=${sectionClass}>${content}</section>`;
        }

        function summaryPage(runtime: LitRuntime, model: PageModel, context: ViewContext): any {
            const h = runtime.html;
            const row = firstRecord(model);
            const config = text(meta(model, 'config-model-turtle', 'config-model'));
            return h`${simpleSection(runtime, 'Repository Location', [
                    ['Repository ID', field(row, 'id')],
                    ['Repository title', field(row, 'description')],
                    ['Repository location', field(row, 'location')],
                    ['Server', field(row, 'server')]
                ], 'workbench-summary-location')}
                ${simpleSection(runtime, 'Repository Size', [
                    ['Repository size', formatCount(field(row, 'size'), context)],
                    ['Named contexts', field(row, 'contexts')]
                ], 'workbench-summary-size')}
                ${config ? h`<section class="workbench-island workbench-summary-config">
                    <details id="summary-config-model" class="workbench-options">
                        <summary>Config Model${icon(runtime, 'chevron', 'workbench-disclosure-chevron')}</summary>
                        <pre role="region">${config}</pre>
                    </details>
                </section>` : ''}`;
        }

        function informationPage(runtime: LitRuntime, model: PageModel): any {
            const row = firstRecord(model);
            return runtime.html`<div id="workbench-information" class="workbench-island">
                ${simpleSection(runtime, 'Application Information', [
                    ['Application name', 'RDF4J Workbench'], ['Application version', field(row, 'version')]
                ], 'workbench-information-section', 'information-application', false)}
                ${simpleSection(runtime, 'Runtime Information', [
                    ['Operating system', field(row, 'os')], ['Java runtime', field(row, 'jvm')], ['Process user', field(row, 'user')]
                ], 'workbench-information-section', 'information-runtime', false)}
                ${simpleSection(runtime, 'Memory', [
                    ['Memory used', field(row, 'memory-used')], ['Maximum memory', field(row, 'maximum-memory')]
                ], 'workbench-information-section', 'information-memory', false)}
            </div>`;
        }

        function repositoriesPage(runtime: LitRuntime, model: PageModel, context: ViewContext): any {
            const h = runtime.html;
            return h`<section id="repositories-results" class="workbench-island workbench-responsive-records">
                ${rowCount(model) ? table(runtime, model, context, { status: true, repository: true })
                    : h`<p class="workbench-empty" role="status">No repositories are available.</p>`}
            </section>`;
        }

        function createPage(runtime: LitRuntime, model: PageModel, context: ViewContext): any {
            const h = runtime.html;
            const rows = records(model);
            if (model.vars.indexOf('fieldId') >= 0) {
                return createTemplateForm(runtime, model, rows, context);
            }
            if (model.vars.indexOf('location') >= 0 && model.vars.indexOf('description') >= 0
                    && model.vars.indexOf('id') >= 0) {
                return federateForm(runtime, rows, context);
            }
            return h`<form id="create-type-form" action="create" method="get" class="workbench-island workbench-form-card">
                <div class="workbench-field"><label for="type">Repository type</label>
                    <select id="type" name="type">
                        ${rows.map((row: any) => h`<option value=${text(row.type)}>${text(row.label)}</option>`)}
                    </select>
                </div>
                <div class="workbench-form-actions">
                    <span class="workbench-action workbench-action--primary"><label class="workbench-action-hit-area">
                        ${icon(runtime, 'next')}<span class="workbench-action-label"><input type="submit" name="next" value="Next" /></span>
                    </label></span>
                </div>
            </form>`;
        }

        function federateForm(runtime: LitRuntime, rows: any[], context: ViewContext): any {
            const h = runtime.html;
            return h`<form action="create" method="post" class="workbench-form-card">
                <table class="dataentry" data-advanced-label="Advanced settings"><tbody>
                    <tr><th><label for="type">Repository type</label></th><td><select id="type" name="type"><option value="federate">Federation Store</option></select></td></tr>
                    <tr><th><label for="id">Repository ID</label></th><td><input id="id" name="Local repository ID" type="text" value="fed" data-field-role="repository-id" /></td><td><span id="recurse-message" class="error" hidden>Federation ID may not match an existing ID.</span></td></tr>
                    <tr><th><label for="title">Repository title</label></th><td><input id="title" name="Repository title" type="text" value="Federation" data-field-role="repository-title" /></td></tr>
                    <tr data-field-role="federation-member"><th>Federation members</th><td><div class="workbench-choice-list">
                        ${rows.filter((row: any) => text(row.id) !== 'SYSTEM').map((row: any) => h`<label class="workbench-choice">
                            <input type="checkbox" class="memberID" name="memberID" value=${text(row.id)} data-field-role="federation-member" />
                            <span>${text(row.id)}${text(row.description) ? ' — ' + text(row.description) : ''}</span>
                        </label>`)}
                    </div></td><td><span class="error" id="create-feedback">Select at least two federation members.</span></td></tr>
                </tbody></table>
                ${createActions(runtime, false, context)}
            </form>`;
        }

        function optionLabel(value: string): string {
            if (value === '__workbench_unset__') { return 'Repository default'; }
            if (value === 'true') { return 'True'; }
            if (value === 'false') { return 'False'; }
            return value;
        }

        function createTemplateForm(runtime: LitRuntime, model: PageModel, rows: any[], context: ViewContext): any {
            const h = runtime.html;
            const first = rows[0] || {};
            const groups: any[] = [];
            rows.forEach((row: any) => {
                const id = text(row.fieldId);
                let group = groups.filter((candidate) => candidate.id === id)[0];
                if (!group) {
                    group = { id, row, options: [] };
                    groups.push(group);
                }
                group.options.push(row);
            });
            const fieldRows = groups.map((group: any) => renderTemplateField(runtime, group));
            return h`<form action="create" method="post" class="workbench-form-card">
                <table class="dataentry" data-advanced-label="Advanced settings"><tbody>
                    <tr><th><label for="type">Repository type</label></th><td><select id="type" name="type">
                        <option value=${text(first.templateType)}>${text(first.templateLabel)}</option>
                    </select></td></tr>
                    ${fieldRows}
                </tbody></table>
                ${createActions(runtime, true, context)}
            </form>`;
        }

        function renderTemplateField(runtime: LitRuntime, group: any): any {
            const h = runtime.html;
            const row = group.row;
            const id = text(row.fieldId);
            const name = text(row.fieldName);
            const type = text(row.fieldType);
            const property = text(row.fieldProperty);
            const role = text(row.fieldRole);
            const selected = group.options.filter((option: any) => text(option.selected) === 'true')[0]
                || group.options[0];
            const value = selected ? text(selected.value) : '';
            const common: any = {};
            if (property) { common['data-config-property'] = property; }
            if (role) { common['data-field-role'] = role; }
            if (type === 'select') {
                return h`<tr><th><label for=${id}>${name}</label></th><td><select id=${id} name=${name}
                        data-config-property=${property || runtime.nothing} data-field-role=${role || runtime.nothing}>
                    ${group.options.map((option: any) => h`<option value=${text(option.value)} ?selected=${text(option.selected) === 'true'}>
                        ${optionLabel(text(option.value))}</option>`)}
                </select></td></tr>`;
            }
            if (type === 'radio') {
                const ordered = group.options.slice();
                if (ordered.length === 2 && ordered.some((option: any) => text(option.value) === 'true')
                        && ordered.some((option: any) => text(option.value) === 'false')) {
                    ordered.sort((left: any) => text(left.value) === 'true' ? -1 : 1);
                }
                return h`<tr><th>${name}</th><td>${ordered.map((option: any, index: number) => h`<label class="workbench-choice">
                    <input type="radio" id=${id + '-' + (index + 1)} name=${name} value=${text(option.value)}
                        data-config-property=${property || runtime.nothing} data-field-role=${role || runtime.nothing}
                        ?checked=${text(option.selected) === 'true'} />
                    <span>${optionLabel(text(option.value))}</span></label>`)}
                </td></tr>`;
            }
            if (type === 'textarea') {
                return h`<tr><th><label for=${id}>${name}</label></th><td><textarea id=${id} name=${name}
                    rows=${Number(text(row.rows)) || 6} cols=${Number(text(row.cols)) || 70}
                    placeholder=${text(row.placeholder) || runtime.nothing}
                    data-config-property=${property || runtime.nothing} data-field-role=${role || runtime.nothing}>${value}</textarea>
                </td></tr>`;
            }
            return h`<tr><th><label for=${id}>${name}</label></th><td><input type="text" id=${id} name=${name}
                size=${Number(text(row.size)) || 32} value=${value} placeholder=${text(row.placeholder) || runtime.nothing}
                data-config-property=${property || runtime.nothing} data-field-role=${role || runtime.nothing} /></td></tr>`;
        }

        function createActions(runtime: LitRuntime, confirmOverwrite: boolean, context: ViewContext): any {
            const h = runtime.html;
            const cancel = urlFor(context, 'repositories');
            const createType = confirmOverwrite ? 'button' : 'submit';
            const createHandler = confirmOverwrite ? (event: Event) => {
                event.preventDefault();
                const globalWindow: any = typeof window !== 'undefined' ? window : null;
                if (globalWindow && typeof globalWindow.checkOverwrite === 'function') {
                    globalWindow.checkOverwrite();
                } else {
                    const form = (event.currentTarget as HTMLElement).closest('form') as HTMLFormElement;
                    if (form) { form.requestSubmit(); }
                }
            } : undefined;
            return h`<div class="workbench-form-actions">
                <span class="workbench-action workbench-action--secondary" data-workbench-action="cancel">
                    <label class="workbench-action-hit-area">${icon(runtime, 'cancel')}
                        <span class="workbench-action-label"><input type="button" value="Cancel" data-href=${cancel}
                            @click=${() => { window.location.href = cancel; }} /></span>
                    </label>
                </span>
                <span class="workbench-action workbench-action--primary" data-workbench-action="create">
                    <label class="workbench-action-hit-area">${icon(runtime, 'create')}
                        <span class="workbench-action-label"><input id="create" type=${createType} value="Create" @click=${createHandler} /></span>
                    </label>
                </span>
            </div>`;
        }

        function pickerWindow(runtime: LitRuntime, model: PageModel, name: string, label: string): any {
            const h = runtime.html;
            const total = rowCount(model);
            const start = model.pickerStart || 0;
            const count = (model.pickerRows || []).length;
            const end = Math.min(total, start + count);
            if (total <= model.pickerPageSize) {
                return '';
            }
            return h`<div class="workbench-form-actions workbench-window-controls" role="group" aria-label=${label + ' pages'}>
                <button type="button" class="workbench-action workbench-action--secondary" data-workbench-window-picker=${name} data-workbench-window-action="previous"
                    ?disabled=${start === 0}>Previous</button>
                <span role="status">Showing ${start + 1}–${end} of ${total} ${label}</span>
                <button type="button" class="workbench-action workbench-action--secondary" data-workbench-window-picker=${name} data-workbench-window-action="next"
                    ?disabled=${end >= total}>Next</button>
            </div>`;
        }

        function deletePage(runtime: LitRuntime, model: PageModel): any {
            const h = runtime.html;
            const options = recordsFromRows(model, model.pickerRows || []).filter((row: any) => text(row.id) !== 'SYSTEM');
            const selectedId = text(model.metadata.selectedRepositoryId || pageValue(model, 'id'));
            const selectedRepository = model.metadata.selectedRepository;
            const selectedVisible = options.some((row: any) => text(row.id) === selectedId);
            return h`<form id="delete-form" class="workbench-island workbench-form-card" action="delete" method="post"
                    @submit=${(event: Event) => {
                        const globalWindow: any = typeof window !== 'undefined' ? window : null;
                        if (globalWindow && typeof globalWindow.checkIsSafeToDelete === 'function') {
                            globalWindow.checkIsSafeToDelete(event);
                        }
                    }}>
                <div class="workbench-field"><label for="id">Repository</label>
                    <select id="id" name="id" data-workbench-window-picker="repositories" @change=${(event: any) => {
                        const selected = options.filter((row: any) => text(row.id) === event.target.value)[0];
                        model.metadata.selectedRepositoryId = event.target.value;
                        model.metadata.selectedRepository = selected || null;
                    }}><option value="" ?selected=${!selectedId}></option>
                        ${selectedId && !selectedVisible && selectedRepository
                            ? h`<option value=${selectedId} selected>${text(selectedRepository.id)} — ${text(selectedRepository.description)}</option>` : ''}
                        ${options.map((row: any) => h`<option value=${text(row.id)} ?selected=${text(row.id) === selectedId}>
                            ${text(row.id)} — ${text(row.description)}</option>`)}
                    </select>
                    ${pickerWindow(runtime, model, 'repositories', 'repositories')}
                </div>
                <div id="delete-actions" class="workbench-form-actions"><span class="workbench-action workbench-action--danger-outline">
                    <label class="workbench-action-hit-area">${icon(runtime, 'delete')}
                        <span class="workbench-action-label"><input type="submit" value="Delete" /></span>
                    </label></span>
                </div><span id="delete-feedback" class="error" role="alert"></span>
            </form>`;
        }

        function namespacesPage(runtime: LitRuntime, model: PageModel, context: ViewContext): any {
            const h = runtime.html;
            const entries = recordsFromRows(model, model.pickerRows || []);
            const selectedNamespace = text(model.metadata.selectedNamespaceValue || pageValue(model, 'namespace'));
            const selectedEntry = model.metadata.selectedNamespace;
            const selectedVisible = entries.some((row: any) => text(row.namespace) === selectedNamespace);
            return h`${errorCallout(runtime, model)}
                <form id="namespaces-form" class="workbench-island" action="namespaces" method="post">
                    <div class="workbench-form-grid">
                        <div class="workbench-field"><label for="prefix">Prefix</label>
                            <div class="workbench-inline-controls">
                                <input type="text" id="prefix" name="prefix" size="8" value=${text(pageValue(model, 'prefix'))} />
                                <select id="prefix-select" data-workbench-window-picker="namespaces" @change=${(event: any) => {
                                    const selected = entries.filter((row: any) => text(row.namespace) === event.target.value)[0];
                                    model.metadata.selectedNamespaceValue = event.target.value;
                                    model.metadata.selectedNamespace = selected || null;
                                    const globalWorkbench: any = (window as any).workbench;
                                    if (globalWorkbench.namespaces && globalWorkbench.namespaces.updatePrefix) {
                                        globalWorkbench.namespaces.updatePrefix();
                                    }
                                }}>
                                    <option value="" ?selected=${!selectedNamespace}></option>
                                    ${selectedNamespace && !selectedVisible && selectedEntry
                                        ? h`<option value=${selectedNamespace} selected>${text(selectedEntry.prefix)}</option>` : ''}
                                    ${entries.map((row: any) => h`<option value=${text(row.namespace)}
                                        ?selected=${text(row.namespace) === selectedNamespace}>${text(row.prefix)}</option>`)}
                                </select>
                                ${pickerWindow(runtime, model, 'namespaces', 'namespaces')}
                            </div>
                        </div>
                        <div class="workbench-field"><label for="namespace">Namespace</label>
                            <input type="text" id="namespace" name="namespace" size="48" value=${text(pageValue(model, 'namespace'))} />
                        </div>
                    </div>
                    <div class="workbench-form-actions">
                        <span class="workbench-action workbench-action--primary"><label class="workbench-action-hit-area">${icon(runtime, 'update')}
                            <span class="workbench-action-label"><button type="submit" value="Update">Update</button></span></label></span>
                        <span class="workbench-action workbench-action--danger-outline"><label class="workbench-action-hit-area">${icon(runtime, 'delete')}
                            <span class="workbench-action-label"><button type="submit" value="Delete" @click=${() => {
                                const namespace = document.getElementById('namespace') as HTMLInputElement;
                                if (namespace) { namespace.value = ''; }
                            }}>Delete</button></span></label></span>
                    </div>
                </form>
                <section id="namespaces-results" class="workbench-island workbench-responsive-records">
                    ${table(runtime, model, context, { emptyText: 'No results to display.' })}
                </section>`;
        }

        function recordBrowsePage(runtime: LitRuntime, model: PageModel, context: ViewContext): any {
            const h = runtime.html;
            const route = model.viewId;
            return h`<section id=${route + '-results'} class="workbench-island workbench-responsive-records">
                ${table(runtime, model, context,
                    { emptyText: 'No results to display.', linkTerms: true })}
            </section>`;
        }

        const explorePageSize = 40;
        const rdfsNamespace = 'http://www.w3.org/2000/01/rdf-schema#';
        const exploreGroups: any[] = [
            { key: 'superClasses', title: 'Super Classes', predicate: rdfsNamespace + 'subClassOf', field: 'object' },
            { key: 'subClasses', title: 'Sub Classes', predicate: rdfsNamespace + 'subClassOf', field: 'subject' },
            { key: 'properties', title: 'Properties', predicate: rdfsNamespace + 'domain', field: 'subject' },
            { key: 'superProperties', title: 'Super Properties', predicate: rdfsNamespace + 'subPropertyOf', field: 'object' },
            { key: 'subProperties', title: 'Sub Properties', predicate: rdfsNamespace + 'subPropertyOf', field: 'subject' },
            { key: 'domain', title: 'Domain', predicate: rdfsNamespace + 'domain', field: 'object' },
            { key: 'range', title: 'Range', predicate: rdfsNamespace + 'range', field: 'object' }
        ];

        function exploreGroupStates(model: PageModel): any {
            const target: any = model;
            if (!target.exploreGroupStates) {
                target.exploreGroupStates = {};
                exploreGroups.forEach((group: any) => {
                    target.exploreGroupStates[group.key] = { cursor: null, history: [], start: 0 };
                });
            }
            return target.exploreGroupStates;
        }

        function exploreAccumulator(model: PageModel): any {
            const states = exploreGroupStates(model);
            const groups: any = {};
            exploreGroups.forEach((group: any) => {
                groups[group.key] = {
                    count: 0,
                    eligible: 0,
                    candidates: [],
                    cursor: states[group.key].cursor
                };
            });
            return {
                groups,
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

        function candidateBefore(left: any, right: any): boolean {
            return left.key < right.key || (left.key === right.key && left.rowIndex < right.rowIndex);
        }

        function addExploreCandidate(group: any, value: any, rowIndex: number, pageSize: number): void {
            const key = text(value);
            group.count++;
            const cursor = group.cursor;
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
            const candidate = { key, rowIndex, value };
            let index = 0;
            while (index < group.candidates.length && candidateBefore(group.candidates[index], candidate)) {
                index++;
            }
            group.candidates.splice(index, 0, candidate);
            if (group.candidates.length > pageSize) {
                group.candidates.pop();
            }
        }

        function consumeExploreRows(model: PageModel, rows: any[][], start: number, accumulator: any): void {
            const subjectIndex = model.vars.indexOf('subject');
            const predicateIndex = model.vars.indexOf('predicate');
            const objectIndex = model.vars.indexOf('object');
            rows.forEach((row: any[], offset: number) => {
                const subject = subjectIndex >= 0 ? row[subjectIndex] : null;
                const predicate = predicateIndex >= 0 ? text(row[predicateIndex]) : '';
                const object = objectIndex >= 0 ? row[objectIndex] : null;
                const rowIndex = start + offset;
                if (predicate === rdfsNamespace + 'label') {
                    accumulator.labelCount++;
                    accumulator.label = object;
                } else if (predicate === rdfsNamespace + 'comment') {
                    accumulator.commentCount++;
                    accumulator.comment = object;
                } else if (predicate === rdfsNamespace + 'subClassOf') {
                    accumulator.subclassCount++;
                } else if (predicate === rdfsNamespace + 'domain') {
                    accumulator.domainCount++;
                } else if (predicate === rdfsNamespace + 'subPropertyOf') {
                    accumulator.subPropertyCount++;
                } else if (predicate === rdfsNamespace + 'range') {
                    accumulator.rangeCount++;
                }
                exploreGroups.forEach((definition: any) => {
                    if (predicate === definition.predicate) {
                        const value = definition.field === 'subject' ? subject : object;
                        if (value !== null && typeof value !== 'undefined') {
                            addExploreCandidate(accumulator.groups[definition.key], value, rowIndex, explorePageSize);
                        }
                    }
                });
            });
        }

        function finishExploreSummary(model: PageModel, accumulator: any): any {
            const states = exploreGroupStates(model);
            const groups: any = {};
            exploreGroups.forEach((definition: any) => {
                const state = states[definition.key];
                const collected = accumulator.groups[definition.key];
                const candidates = collected.candidates;
                const last = candidates.length ? candidates[candidates.length - 1] : null;
                let nextCursor = null;
                if (last) {
                    const sameKeyInPage = candidates.filter((candidate: any) => candidate.key === last.key).length;
                    nextCursor = {
                        key: last.key,
                        consumed: state.cursor && state.cursor.key === last.key
                            ? state.cursor.consumed + sameKeyInPage : sameKeyInPage
                    };
                }
                groups[definition.key] = {
                    count: collected.count,
                    start: state.start,
                    items: candidates.map((candidate: any) => candidate.value),
                    hasPrevious: state.history.length > 0,
                    hasNext: collected.eligible > candidates.length,
                    nextCursor
                };
            });
            return {
                label: accumulator.labelCount === 1 ? accumulator.label : null,
                comment: accumulator.commentCount === 1 ? accumulator.comment : null,
                subclassCount: accumulator.subclassCount,
                domainCount: accumulator.domainCount,
                subPropertyCount: accumulator.subPropertyCount,
                rangeCount: accumulator.rangeCount,
                groups
            };
        }

        function summarizeVisibleExploreRows(model: PageModel): any {
            const accumulator = exploreAccumulator(model);
            consumeExploreRows(model, model.rows || [], rowStart(model), accumulator);
            return finishExploreSummary(model, accumulator);
        }

        function prepareExploreSummary(model: PageModel): Promise<any> {
            if (model.viewId !== 'explore' || !model.rowStore) {
                return Promise.resolve();
            }
            const accumulator = exploreAccumulator(model);
            const chunkSize = 256;
            const scan = (start: number): Promise<void> => {
                if (start >= model.rowCount) {
                    return Promise.resolve();
                }
                const count = Math.min(chunkSize, model.rowCount - start);
                return model.rowStore.read(start, count).then((rows: any[][]) => {
                    consumeExploreRows(model, rows, start, accumulator);
                    return rows.length ? scan(start + rows.length) : Promise.resolve();
                });
            };
            return scan(0).then(() => finishExploreSummary(model, accumulator));
        }

        function exploreGroupList(runtime: LitRuntime, model: PageModel, context: ViewContext,
                                  definition: any, page: any): any {
            const h = runtime.html;
            if (!page || !page.count) { return ''; }
            const regions = context.rowRegions;
            if (regions) {
                if (!regions.groups[definition.key]) {
                    const node = regions.document.createElement('section');
                    node.setAttribute('data-workbench-explore-group', definition.key);
                    regions.groups[definition.key] = {
                        node,
                        render: () => {
                            const summary = (model as any).exploreSummary || summarizeVisibleExploreRows(model);
                            runtime.render(exploreGroupContent(runtime, model, context, definition,
                                summary.groups[definition.key]), node);
                        }
                    };
                }
                return regions.groups[definition.key].node;
            }
            return h`<section data-workbench-explore-group=${definition.key}>
                ${exploreGroupContent(runtime, model, context, definition, page)}
            </section>`;
        }

        function exploreGroupContent(runtime: LitRuntime, model: PageModel, context: ViewContext,
                                     definition: any, page: any): any {
            const h = runtime.html;
            return h`<h3>${definition.title}</h3>
                <ul>${page.items.map((value: any) => h`<li>${renderTerm(runtime, value, context, true)}</li>`)}</ul>
                ${page.count > explorePageSize ? h`<div class="workbench-form-actions workbench-window-controls"
                    role="group" aria-label=${definition.title + ' pages'}>
                    <span role="status">Showing ${page.start + 1}–${page.start + page.items.length} of ${page.count}</span>
                    <button type="button" class="workbench-action workbench-action--secondary" data-workbench-explore-group=${definition.key}
                        data-workbench-explore-action="previous" ?disabled=${!page.hasPrevious}>Previous</button>
                    <button type="button" class="workbench-action workbench-action--secondary" data-workbench-explore-group=${definition.key}
                        data-workbench-explore-action="next" ?disabled=${!page.hasNext}>Next</button>
                </div>` : ''}`;
        }

        function explorePage(runtime: LitRuntime, model: PageModel, context: ViewContext): any {
            const h = runtime.html;
            const resource = text(pageValue(model, 'resource'));
            const total = rowCount(model);
            const summary = (model as any).exploreSummary || summarizeVisibleExploreRows(model);
            const info = workbenchData(context);
            const defaults = info.defaults || info;
            const resultLimit = text(pageValue(model, 'default-limit') || defaults['default-limit']) || '100';
            const resultLimited = !!resource && resultLimit !== '0' && total === Number(resultLimit);
            const groupPages = summary.groups || {};
            const classGroups = [exploreGroups[0], exploreGroups[1]]
                .map((definition: any) => exploreGroupList(runtime, model, context, definition, groupPages[definition.key]));
            const propertyGroups: any[] = [];
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
            const groupedResults = summary.subclassCount || summary.domainCount || summary.rangeCount
                ? h`<div class="workbench-explore-groups">
                    ${classGroups.some((group: any) => !!group) ? h`<div>${classGroups}</div>` : ''}
                    ${propertyGroups.some((group: any) => !!group) ? h`<div>${propertyGroups}</div>` : ''}
                </div>` : '';
            return h`${resultLimited ? h`<p id="result-limited">The results shown maybe truncated.</p>` : ''}
                ${errorCallout(runtime, model)}
                ${summary.label ? h`<h2>${text(summary.label)}</h2>` : ''}${summary.comment ? h`<p class="workbench-prose">${text(summary.comment)}</p>` : ''}
                <p id="explore-resource-summary" class="workbench-page-meta" ?hidden=${!resource}>
                    <span id="explore-resource-value">${resource}</span><span id="explore-result-count">${total}</span>
                </p>
                <form id="explore-form" class="workbench-island" action="explore">
                    <input id="workbench-total-result-count" type="hidden"
                        value=${text(pageValue(model, 'total-result-count'))} />
                    <div id="explore-controls"><div id="explore-resource-field" class="workbench-field">
                        <label for="resource">Resource</label><input id="resource" name="resource" size="48" type="text" value=${resource} />
                    </div>
                    ${workbench.detailDisclosure.render(h, {
                        id: 'explore-result-options', toggleId: 'explore-result-options-toggle',
                        panelId: 'explore-result-options-panel', label: 'Result options',
                        ownerClass: 'workbench-options workbench-form-subgroup'
                    }, h`<div class="workbench-field workbench-disclosure__field"><label for="limit_explore">Result limit</label>
                            ${limitSelect(runtime, 'limit_explore', context, text(pageValue(model, 'default-limit')) || '100')}
                        </div><label class="workbench-check" for="explore-show-datatypes">
                            <input id="explore-show-datatypes" type="checkbox" name="show-datatypes" value="show-dataypes" checked />
                            <span>Show datatypes</span></label>`)}
                    </div>
                </form>
                <section id="explore-results" class="workbench-island workbench-responsive-records">
                    ${total ? h`${groupedResults}${table(runtime, model, context, { linkTerms: true })}`
                        : h`<p class="workbench-empty" role="status">No results to display.</p>`}
                    <div id="explore-pagination" class="workbench-form-actions" ?hidden=${total === 0}>
                        <button id="previousX" class="workbench-action workbench-action--secondary" type="button" value=${'Previous ' + total}
                            @click=${() => invoke('workbench.paging.previousOffset', 'explore')}>Previous ${total}</button>
                        <button id="nextX" class="workbench-action workbench-action--secondary" type="button" value=${'Next ' + total}
                            @click=${() => invoke('workbench.paging.nextOffset', 'explore')}>Next ${total}</button>
                    </div>
                </section>`;
        }

        function limitSelect(runtime: LitRuntime, id: string, _context: ViewContext, selected: string): any {
            const h = runtime.html;
            const options = ['0', '10', '50', '100', '200'];
            if (selected && options.indexOf(selected) < 0) { options.push(selected); }
            return h`<select id=${id} name=${id}>${options.map((option: string) => h`<option value=${option} ?selected=${option === selected}>
                ${option === '0' ? 'All' : option}</option>`)}</select>`;
        }

        function invoke(path: string, ...arguments_: any[]): void {
            const pieces = path.split('.');
            let target: any = typeof window !== 'undefined' ? (window as any) : null;
            for (let i = 0; target && i < pieces.length - 1; i++) { target = target[pieces[i]]; }
            if (target && typeof target[pieces[pieces.length - 1]] === 'function') {
                target[pieces[pieces.length - 1]].apply(target, arguments_);
            }
        }

        function savedQueriesPage(runtime: LitRuntime, model: PageModel, context: ViewContext): any {
            const h = runtime.html;
            if (!rowCount(model)) {
                const queryAvailable = menuEntries(context).some((group: any) => (group.items || []).some((item: any) =>
                    text(item.id || item['menu-item-id']) === 'query' && !isDisabled(item, workbenchData(context))));
                return h`<div id="saved-queries" class="workbench-page-layout">
                    <div class="saved-queries-empty" role="status"><p>No saved queries yet.</p>
                        ${queryAvailable ? h`<a href=${urlFor(context, 'query')}>Open Query</a>` : ''}</div>
                </div>`;
            }
            const regions = context.rowRegions;
            if (regions) {
                if (!regions.savedList) {
                    regions.savedList = regions.document.createElement('div');
                    regions.savedList.id = 'saved-queries';
                    regions.savedList.className = 'workbench-page-layout';
                    regions.savedList.setAttribute('data-workbench-row-list', 'true');
                }
                regions.renderSavedRows = () => {
                    const cards: { [key: string]: HTMLElement } = {};
                    const rows = records(model).map((row: any, relativeIndex: number) => {
                        const index = rowStart(model) + relativeIndex;
                        const urn = text(row.query);
                        const key = index + ':' + urn;
                        let card = regions.savedCards[key];
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
            return h`<div id="saved-queries" class="workbench-page-layout" data-workbench-row-list="true">
                ${savedQueryListContent(runtime, model, records(model).map((row: any, relativeIndex: number) => {
                    const index = rowStart(model) + relativeIndex;
                    const urn = text(row.query);
                    return h`<article id=${urn + '-div'} class="saved-query-row workbench-island"
                            data-workbench-row-index=${index}>
                        ${savedQueryContent(runtime, row, index)}
                    </article>`;
                }))}
            </div>`;
        }

        function savedQueryListContent(runtime: LitRuntime, model: PageModel, rows: any[]): any {
            const h = runtime.html;
            return h`<div class="workbench-virtual-spacer" style=${model.rowTopSpacer
                    ? 'height:' + model.rowTopSpacer + 'px' : 'display:none'} aria-hidden="true"></div>
                ${rows}
                <div class="workbench-virtual-spacer" style=${model.rowBottomSpacer
                    ? 'height:' + model.rowBottomSpacer + 'px' : 'display:none'} aria-hidden="true"></div>`;
        }

        function savedQueryContent(runtime: LitRuntime, row: any, index: number): any {
            const h = runtime.html;
            const urn = text(row.query);
            const queryName = text(row.queryName);
            const owner = text(row.user);
            const query = text(row.queryText || row.query);
            const queryTimeout = text(row.queryTimeout).trim() || '0';
            const formId = 'saved-query-exec-' + index;
            return h`
                        <div class="saved-query-row__heading"><h2>${queryName}</h2><span>${owner}</span></div>
                        <div class="saved-query-actions">
                            <form method="post" action="query" id=${formId}
                                data-workbench-query-execution="true"
                                data-workbench-results-target=${'saved-query-results-' + index}>
                                <input type="hidden" name="action" value="exec" />
                                <input type="hidden" name="queryLn" value=${text(row.queryLn)} />
                                <input type="hidden" name="query" value=${queryName} />
                                <input type="hidden" name="ref" value="id" />
                                <input type="hidden" name="owner" value=${owner} />
                                <input type="hidden" name="infer" value=${text(row.infer)} />
                                <input type="hidden" name="query-timeout" value=${queryTimeout} />
                                <span class="workbench-action workbench-action--primary"><label class="workbench-action-hit-area">
                                    ${icon(runtime, 'execute')}<span class="workbench-action-label"><input type="submit" value="Execute" /></span>
                                </label></span>
                            </form>
                            <button type="button" class="saved-query-toggle workbench-action workbench-action--secondary" id=${urn + '-toggle'} data-query-urn=${urn}
                                value="Show">Show</button>
                            <form method="post" action="query"><input type="hidden" name="action" value="edit" />
                                <input type="hidden" name="queryLn" value=${text(row.queryLn)} /><input type="hidden" name="query" value=${queryName} />
                                <input type="hidden" name="ref" value="id" /><input type="hidden" name="owner" value=${owner} />
                                <input type="hidden" name="infer" value=${text(row.infer)} />
                                <input type="hidden" name="query-timeout" value=${queryTimeout} /><button class="workbench-action workbench-action--secondary" type="submit">Edit</button>
                            </form>
                            <form method="post" id=${urn} action=${'saved-queries?delete=' + encodeURIComponent(urn)}>
                                <button type="button" class="saved-query-delete workbench-action workbench-action--danger-outline" data-query-owner=${owner} data-query-name=${queryName}
                                    data-query-urn=${urn}>Delete…</button>
                            </form>
                        </div>
                        <div id=${'saved-query-results-' + index} class="query-results" aria-live="polite"></div>
                        <table class="data" id=${urn + '-metadata'} style="display: none"><tbody><tr>
                            <th>Query Language</th><td>${text(row.queryLn)}</td><th>Include Inferred Statements</th><td>${text(row.infer)}</td>
                            <th>Shared</th><td>${text(row.shared)}</td>
                        </tr></tbody></table>
                        <textarea id=${urn + '-text'} style="display: none">${query}</textarea>
                    `;
        }

        function exportPage(runtime: LitRuntime, model: PageModel, context: ViewContext): any {
            const h = runtime.html;
            const info = workbenchData(context);
            const defaults = info.defaults || info;
            const graphFormats = formatOptions(info.graphDownloadFormats || info['graph-download-format']);
            const locationObject: any = typeof window !== 'undefined' ? window.location : null;
            const urlFormat = locationObject && typeof locationObject.search === 'string'
                ? new URLSearchParams(locationObject.search).get('Accept') : '';
            const explicitFormat = text(urlFormat || meta(model, 'Accept') || pageValue(model, 'Accept'));
            const cookieFormat = currentCookieValue('Accept');
            const configuredFormat = text(meta(model, 'default-export-format') || defaults['default-export-format']);
            const genericFormat = text(meta(model, 'default-Accept') || defaults['default-Accept']);
            const available = (candidate: string) => graphFormats.some((format: any) => format.value === candidate);
            const defaultFormat = [explicitFormat, cookieFormat, configuredFormat, genericFormat]
                .filter((candidate: string) => candidate && available(candidate))[0]
                || (graphFormats.length ? graphFormats[0].value : 'application/n-quads');
            const timeout = text(meta(model, 'export-timeout')) || '43200';
            const requested = text(meta(model, 'statement-preview-requested')) === 'true';
            const previewLimit = text(meta(model, 'statement-preview-limit')) || '100';
            return h`<form id="export-form" class="workbench-island workbench-form-card" action="export">
                <div class="workbench-form-grid">
                    <div class="workbench-field"><label for="Accept">Download format</label>
                        <select id="Accept" name="Accept">${graphFormats.map((format: any) => h`<option value=${format.value} ?selected=${format.value === defaultFormat}>${format.label}</option>`)}</select>
                    </div>
                    <div class="workbench-field"><label for="compression">Compression</label><select id="compression" name="compression">
                        <option value="none">None</option><option value="gzip" selected>Gzip</option><option value="zip">Zip</option>
                    </select></div>
                    <div class="workbench-field"><label for="timeout">Export timeout (seconds)</label>
                        <input id="timeout" name="timeout" type="number" min="0" step="1" required value=${timeout} />
                        <span class="hint">Maximum time allowed for the export operation. Use 0 for no timeout.</span>
                    </div>
                </div>
                ${workbench.detailDisclosure.render(h, {
                    id: 'export-result-options', toggleId: 'export-result-options-toggle',
                    panelId: 'export-result-options-panel', label: 'Result options',
                    ownerClass: 'workbench-options workbench-form-subgroup'
                }, h`<div class="workbench-field workbench-disclosure__field"><label for="limit_export">Preview limit</label>
                        ${limitSelect(runtime, 'limit_export', context, previewLimit)}
                        <span id="result-limited">${requested && previewLimit !== '0' && rowCount(model) >= Number(previewLimit)
                            ? 'The preview is limited to the selected number of statements.' : ''}</span>
                        <span class="hint">Limit only applies to the preview, not to downloads.</span>
                    </div>`)}
                <div class="workbench-form-actions"><span class="workbench-action workbench-action--primary">
                    <span class="workbench-action-hit-area"><span class="workbench-action-label">
                        <button type="submit" name="action" value="download" aria-label="Download">
                            ${icon(runtime, 'download')}<span>Download</span>
                        </button>
                    </span></span>
                </span></div>
            </form>
            <section id="export-results" class="workbench-island workbench-responsive-records">
                <p class="workbench-page-meta">Statement preview</p>
                <div class="workbench-form-actions"><button class="workbench-action workbench-action--secondary" type="submit" form="export-form" name="action" value="preview">Retrieve statements</button></div>
                ${rowCount(model) ? table(runtime, model, context, { linkTerms: true })
                    : h`<p class="workbench-empty" role="status">${requested ? 'No results to display.' : 'Choose Retrieve statements to preview repository data.'}</p>`}
            </section>`;
        }

        function formatOptions(values: any): any[] {
            if (!Array.isArray(values)) { return []; }
            return values.map((entry: any) => {
                if (entry && typeof entry === 'object') {
                    return { value: text(entry.value || entry.mimeType || entry.name), label: text(entry.label || entry.name || entry.value) };
                }
                const value = text(entry);
                const separator = value.indexOf(' ');
                return separator < 0 ? { value, label: value }
                    : { value: value.substring(0, separator), label: value.substring(separator + 1) };
            });
        }

        function currentCookieValue(name: string): string {
            const namespace: any = workbench as any;
            if (namespace && typeof namespace.getCookie === 'function') {
                return text(namespace.getCookie(name));
            }
            if (typeof document === 'undefined' || typeof document.cookie !== 'string') {
                return '';
            }
            let cookie: string | undefined;
            const cookies = document.cookie.split(';');
            for (let index = 0; index < cookies.length; index++) {
                const candidate = cookies[index].trim();
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
            } catch (error) {
                return cookie.substring(cookie.indexOf('=') + 1);
            }
        }

        function addPage(runtime: LitRuntime, model: PageModel, context: ViewContext): any {
            const h = runtime.html;
            const rows = records(model);
            const info = workbenchData(context);
            const defaults = info.defaults || info;
            const formats = formatOptions(info.uploadFormats || info['upload-format']);
            const isolationOptions = rows.filter((row: any) => field(row, 'isolation-level-option'));
            const selectedIsolation = text(pageValue(model, 'transaction-setting__org.eclipse.rdf4j.common.transaction.IsolationLevel'));
            const error = text(pageValue(model, 'error-message'));
            return h`${error ? callout(runtime, 'error', error) : ''}
                ${systemRepositoryCallout(runtime, context)}
                <form method="post" action="add" enctype="multipart/form-data" class="workbench-form-card">
                    <fieldset id="add-source-tabs" class="workbench-source-tabs"><legend>Source</legend>
                        ${[['file', 'File'], ['url', 'URL'], ['text', 'Text']].map((entry: string[]) => h`<label for=${'source-' + entry[0]}>
                            ${icon(runtime, 'source-' + (entry[0] === 'text' ? 'text' : entry[0]))}
                            <input type="radio" id=${'source-' + entry[0]} name="source"
                                value=${entry[0] === 'text' ? 'contents' : entry[0]}
                                ?checked=${entry[0] === 'file'}
                                @change=${() => invoke('workbench.add.enabledInput', entry[0])} />
                            <span>${entry[1]}</span>
                        </label>`)}
                    </fieldset>
                    <div class="workbench-form-grid add-source-fields">
                        <div id="add-source-file-panel" class="workbench-field add-source-panel" data-source="file">
                            <label for="file">RDF file</label><input type="file" id="file" name="content"
                                @change=${() => invoke('workbench.add.enabledInput', 'file')} />
                            <div class="hint">Select an RDF document from your computer.</div>
                        </div>
                        <div id="add-source-url-panel" class="workbench-field add-source-panel" data-source="url" hidden>
                            <label for="url">RDF URL</label><input id="url" name="url" type="text" size="48" disabled
                                @change=${() => invoke('workbench.add.enabledInput', 'url')} />
                        </div>
                        <div id="add-source-text-panel" class="workbench-field add-source-panel" data-source="text" hidden>
                            <label for="text">RDF text</label><textarea id="text" name="content" rows="6" cols="70" disabled></textarea>
                        </div>
                        <div class="workbench-field add-source-format"><label for="Content-Type">Data format</label>
                            <div class="workbench-select-control"><select id="Content-Type" name="Content-Type">
                                <option id="autodetect" value="autodetect" selected>(autodetect)</option>
                                ${formats.map((format: any) => h`<option value=${format.value}>${format.label}</option>`)}
                            </select>${icon(runtime, 'chevron', 'workbench-select-chevron')}</div>
                        </div>
                    </div>
                    ${workbench.detailDisclosure.render(h, {
                        id: 'add-import-settings', toggleId: 'add-import-settings-toggle',
                        panelId: 'add-import-settings-panel', label: 'Advanced settings',
                        ownerClass: 'workbench-options workbench-form-subgroup'
                    }, h`<div class="workbench-form-grid">
                            <div class="workbench-field workbench-disclosure__field"><label for="baseURI">Base URI</label>
                                <input id="baseURI" name="baseURI" type="text" size="48" value=${text(pageValue(model, 'baseURI'))} />
                                <label class="workbench-check" for="overrideContext"><input type="checkbox" id="overrideContext" name="overrideContext"
                                    ?checked=${!!text(pageValue(model, 'context'))} @change=${() => invoke('workbench.add.handleContextOverride')} />
                                    <span>Override parsed contexts with this context</span></label>
                            </div>
                            <div class="workbench-field workbench-disclosure__field"><label for="context">Context</label>
                                <input id="context" name="context" type="text" size="48" aria-describedby="context-help"
                                    value=${text(pageValue(model, 'context'))} ?disabled=${!text(pageValue(model, 'context'))} />
                                <p id="context-help" class="workbench-help">RDF context may be an IRI, blank node, or the default graph. With override off, embedded contexts are preserved; contextless data uses the default graph. Base URI resolves relative RDF identifiers; it does not choose a graph context.</p>
                            </div>
                            <div class="workbench-field workbench-disclosure__field"><label for="transaction-setting__org.eclipse.rdf4j.common.transaction.IsolationLevel">Isolation level</label>
                                <select id="transaction-setting__org.eclipse.rdf4j.common.transaction.IsolationLevel"
                                    name="transaction-setting__org.eclipse.rdf4j.common.transaction.IsolationLevel">
                                    <option value="" ?selected=${!selectedIsolation}>Default</option>
                                    ${isolationOptions.map((row: any) => h`<option value=${text(row['isolation-level-option'])}
                                        ?selected=${text(row['isolation-level-option']) === selectedIsolation}>
                                        ${text(row['isolation-level-option-label']) || text(row['isolation-level-option'])}</option>`)}
                                </select>
                                <div class="hint">Choose the transaction isolation level used for this import.</div>
                            </div>
                        </div>`)}
                    <div id="add-upload-actions" class="workbench-form-actions"><span class="workbench-action workbench-action--primary">
                        <label class="workbench-action-hit-area">${icon(runtime, 'upload')}<span class="workbench-action-label"><input type="submit" value="Upload" /></span></label>
                    </span></div>
                </form>`;
        }

        function removePage(runtime: LitRuntime, model: PageModel, context: ViewContext): any {
            const h = runtime.html;
            return h`<form id="remove-form" class="workbench-island workbench-form-card" method="post" action="remove">
                ${systemRepositoryCallout(runtime, context)}
                ${callout(runtime, 'warning', 'Only statements matching the supplied values will be removed. An empty form is rejected.', 'Remove is permanent.', 'remove-warning')}
                <p>Values use RDF syntax: IRIs in angle brackets, blank nodes as _:nodeID, and literals in double quotes with optional language or datatype.</p>
                <details id="remove-examples" class="workbench-options"><summary>Examples${icon(runtime, 'chevron', 'workbench-disclosure-chevron')}</summary>
                    <ul><li>URI: <tt>&lt;http://foo.com/bar&gt;</tt></li><li>BNode: <tt>_:nodeID</tt></li>
                        <li>Literal: <tt>"Hello"</tt>, <tt>"Hello"@en</tt>, or <tt>"Hello"^^&lt;http://bar.com/foo&gt;</tt></li></ul>
                </details>
                ${errorCallout(runtime, model)}
                    <div class="workbench-field-stack">
                        ${[['subj', 'Subject', 'text'], ['pred', 'Predicate', 'text'], ['obj', 'Object', 'textarea'], ['context', 'Context', 'text']].map((entry: string[]) => h`<div class="workbench-field">
                            <label for=${entry[0]}>${entry[1]}</label>${entry[2] === 'textarea'
                                ? h`<textarea id="obj" name="obj" rows="3">${text(pageValue(model, 'obj'))}</textarea>`
                                : h`<input id=${entry[0]} name=${entry[0]} type="text" value=${text(pageValue(model, entry[0]))} />`}
                        </div>`)}
                    </div>
                    <div class="workbench-form-actions"><span class="workbench-action workbench-action--danger-outline"><label class="workbench-action-hit-area">
                        ${icon(runtime, 'remove')}<span class="workbench-action-label"><input type="submit" value="Remove" /></span>
                    </label></span></div>
                </form>`;
        }

        function clearPage(runtime: LitRuntime, model: PageModel, context: ViewContext): any {
            const h = runtime.html;
            return h`<form id="clear-form" class="workbench-island workbench-form-card" method="post" action="clear">
                ${systemRepositoryCallout(runtime, context)}
                ${callout(runtime, 'warning', 'Clearing without a context removes every statement in this repository.', 'Clear is permanent.', 'clear-warning')}
                ${errorCallout(runtime, model)}
                    <div class="workbench-field-stack">
                        <div class="workbench-field"><label for="context">Context</label>
                            <input id="context" name="context" type="text" value=${text(pageValue(model, 'context'))} /></div>
                    </div>
                    <div class="workbench-form-actions"><span class="workbench-action workbench-action--danger-outline"><label class="workbench-action-hit-area">
                        ${icon(runtime, 'clear')}<span class="workbench-action-label"><input type="submit" value="Clear context" /></span>
                    </label></span></div>
                </form>`;
        }

        function updatePage(runtime: LitRuntime, model: PageModel, context: ViewContext): any {
            const h = runtime.html;
            const error = text(pageValue(model, 'error-message'));
            const query = text(pageValue(model, 'update')) || '\n\t';
            const mappings = context.linked && context.linked.namespaces
                ? context.linked.namespaces.namespaceMap : model.namespaceMap;
            if (typeof window !== 'undefined') {
                (window as any).namespaces = mappings || {};
                (window as any).sparqlNamespaces = mappings || {};
            }
            return h`<form id="update-form" class="workbench-island" action="update" method="post"
                    @submit=${(event: Event) => {
                        const globalWorkbench: any = (window as any).workbench;
                        if (globalWorkbench.update && globalWorkbench.update.doSubmit) {
                            if (!globalWorkbench.update.doSubmit()) { event.preventDefault(); }
                        }
                    }}>
                <div id="update-editor" class="workbench-field"><label for="update">SPARQL Update</label>
                    <textarea id="update" name="update" rows="16" cols="80">${query}</textarea>
                    <span id="updateString.errors" class="error" role="alert">${error}</span>
                </div>
                <div id="update-actions" class="workbench-form-actions"><span class="workbench-action workbench-action--primary">
                    <label class="workbench-action-hit-area">${icon(runtime, 'execute')}<span class="workbench-action-label"><input type="submit" value="Execute" /></span></label>
                </span></div>
            </form>`;
        }

        function serverPage(runtime: LitRuntime, model: PageModel, context: ViewContext): any {
            const h = runtime.html;
            const row = firstRecord(model);
            const info = workbenchData(context);
            const server = text(field(row, 'server') || info.server || info.location);
            return h`<form id="server-form" class="workbench-form-card" action="server" method="post"
                    @submit=${(event: Event) => {
                        const globalWindow: any = typeof window !== 'undefined' ? window : null;
                        if (globalWindow && typeof globalWindow.changeServer === 'function') {
                            globalWindow.changeServer(event);
                        }
                    }}>
                <div class="workbench-form-grid server-connection-fields"><div class="workbench-field">
                    <label for="workbench-server">Server</label><input id="workbench-server" name="workbench-server" type="text" size="40" value=${server} />
                    <div class="hint">Enter the URL of an RDF4J Server.</div><span class="error" role="alert">${text(field(row, 'error-message'))}</span>
                </div></div>
                ${workbench.detailDisclosure.render(h, {
                    id: 'server-auth', toggleId: 'server-auth-toggle', panelId: 'server-auth-panel',
                    label: 'Advanced settings', ownerClass: 'workbench-options workbench-form-subgroup'
                }, h`<div class="workbench-form-grid">
                        <div class="workbench-field workbench-disclosure__field"><label for="server-user">User</label><input id="server-user" name="server-user"
                            type="text" size="32" value=${text(field(row, 'server-user'))} /></div>
                        <div class="workbench-field workbench-disclosure__field"><label for="server-password">Password</label><input id="server-password" name="server-password" type="password" size="32" value="" /></div>
                    </div>`)}
                <div id="server-change-actions" class="workbench-form-actions"><span class="workbench-action workbench-action--primary">
                    <label class="workbench-action-hit-area">${icon(runtime, 'update')}<span class="workbench-action-label"><input type="submit" value="Change" /></span></label>
                </span></div>
            </form>`;
        }

        function queryFeatureEnabled(context: ViewContext, id: string): boolean {
            const features = context.workbench && context.workbench.queryFeatures;
            return !features || typeof features[id] === 'undefined' || features[id] !== false;
        }

        function anyQueryFeatureEnabled(context: ViewContext, ids: string[]): boolean {
            const features = context.workbench && context.workbench.queryFeatures;
            const configured = !!features && ids.some((id) => Object.prototype.hasOwnProperty.call(features, id));
            return !configured || ids.some((id) => queryFeatureEnabled(context, id));
        }

        function effectiveQueryFeatureOption(context: ViewContext, requested: string, options: any[], preference: string[]): string {
            const requestedOption = options.filter((option: any) => option.value === requested
                && queryFeatureEnabled(context, option.feature))[0];
            if (requestedOption) {
                return requestedOption.value;
            }
            const preferred = preference.map((value) => options.filter((option: any) => option.value === value
                && queryFeatureEnabled(context, option.feature))[0]).filter((option: any) => !!option)[0];
            return preferred ? preferred.value : '';
        }

        function queryExplainEnabled(context: ViewContext): boolean {
            if (!queryFeatureEnabled(context, 'query-explain')) {
                return false;
            }
            const features = context.workbench && context.workbench.queryFeatures;
            const levels = ['unoptimized', 'optimized', 'executed', 'telemetry', 'timed']
                .map((level) => 'explain-level-' + level);
            const formats = ['text', 'dot', 'json'].map((format) => 'explain-format-' + format);
            const anyConfigured = (ids: string[]) => !!features
                && ids.some((id) => Object.prototype.hasOwnProperty.call(features, id));
            const anyEnabled = (ids: string[]) => ids.some((id) => queryFeatureEnabled(context, id));
            return (!anyConfigured(levels) || anyEnabled(levels))
                && (!anyConfigured(formats) || anyEnabled(formats));
        }

        function queryExplainSettingsEnabled(context: ViewContext): boolean {
            return anyQueryFeatureEnabled(context, [
                'explain-format-text', 'explain-format-dot', 'explain-format-json',
                'explain-level-unoptimized', 'explain-level-optimized', 'explain-level-executed',
                'explain-level-telemetry', 'explain-level-timed', 'explain-highlight-syntax',
                'explain-highlight-hotspot', 'explain-property-selection'
            ]);
        }

        function queryPane(runtime: LitRuntime, options: any, context: ViewContext): any {
            const h = runtime.html;
            const compare = !!options.compare;
            const queryId = compare ? 'query-compare' : 'query';
            const suffix = compare ? '-compare' : '';
            const explanation = compare ? '' : text(options.explanation);
            const paneId = compare ? 'query-compare-pane' : 'query-primary-pane';
            const explainFormats = [
                { value: 'text', label: 'Text', feature: 'explain-format-text' },
                { value: 'dot', label: 'DOT', feature: 'explain-format-dot' },
                { value: 'json', label: 'JSON', feature: 'explain-format-json' }
            ];
            const explainLevels = [
                { value: 'Unoptimized', feature: 'explain-level-unoptimized' },
                { value: 'Optimized', feature: 'explain-level-optimized' },
                { value: 'Executed', feature: 'explain-level-executed' },
                { value: 'Telemetry', feature: 'explain-level-telemetry' },
                { value: 'Timed', feature: 'explain-level-timed' }
            ];
            const selectedFormat = effectiveQueryFeatureOption(context, text(options.explanationFormat), explainFormats,
                ['text', 'dot', 'json']);
            const selectedLevel = effectiveQueryFeatureOption(context, text(options.explanationLevel), explainLevels,
                ['Optimized', 'Unoptimized', 'Executed', 'Telemetry', 'Timed']);
            const allExplainFormatsDisabled = !anyQueryFeatureEnabled(context,
                explainFormats.map((option: any) => option.feature));
            const allExplainLevelsDisabled = !anyQueryFeatureEnabled(context,
                explainLevels.map((option: any) => option.feature));
            const allHighlightingDisabled = !anyQueryFeatureEnabled(context,
                ['explain-highlight-syntax', 'explain-highlight-hotspot']);
            const propertySelectionEnabled = queryFeatureEnabled(context, 'explain-property-selection');
            const formatOption = (format: any): any => {
                const enabled = queryFeatureEnabled(context, format.feature);
                const selected = format.value === selectedFormat;
                if (format.value === 'text') {
                    return enabled
                        ? h`<option value=${format.value} ?selected=${selected}>Text</option>`
                        : h`<option value=${format.value} ?selected=${false} ?hidden=${true} ?disabled=${true}>Text</option>`;
                }
                if (format.value === 'dot') {
                    return enabled
                        ? h`<option value=${format.value} ?selected=${selected}>DOT</option>`
                        : h`<option value=${format.value} ?selected=${false} ?hidden=${true} ?disabled=${true}>DOT</option>`;
                }
                return enabled
                    ? h`<option value=${format.value} ?selected=${selected}>JSON</option>`
                    : h`<option value=${format.value} ?selected=${false} ?hidden=${true} ?disabled=${true}>JSON</option>`;
            };
            const levelOption = (level: any): any => {
                const enabled = queryFeatureEnabled(context, level.feature);
                const selected = level.value === selectedLevel;
                switch (level.value) {
                    case 'Unoptimized':
                        return enabled
                            ? h`<option value=${level.value} ?selected=${selected}>Unoptimized</option>`
                            : h`<option value=${level.value} ?hidden=${true} ?disabled=${true} ?selected=${false}>Unoptimized</option>`;
                    case 'Optimized':
                        return enabled
                            ? h`<option value=${level.value} ?selected=${selected}>Optimized</option>`
                            : h`<option value=${level.value} ?hidden=${true} ?disabled=${true} ?selected=${false}>Optimized</option>`;
                    case 'Executed':
                        return enabled
                            ? h`<option value=${level.value} ?selected=${selected}>Executed</option>`
                            : h`<option value=${level.value} ?hidden=${true} ?disabled=${true} ?selected=${false}>Executed</option>`;
                    case 'Telemetry':
                        return enabled
                            ? h`<option value=${level.value} ?selected=${selected}>Telemetry</option>`
                            : h`<option value=${level.value} ?hidden=${true} ?disabled=${true} ?selected=${false}>Telemetry</option>`;
                    default:
                        return enabled
                            ? h`<option value=${level.value} ?selected=${selected}>Timed</option>`
                            : h`<option value=${level.value} ?hidden=${true} ?disabled=${true} ?selected=${false}>Timed</option>`;
                }
            };
            return h`<section id=${paneId} class=${compare
                    ? 'query-compare-pane query-compare-pane--secondary'
                    : 'query-compare-pane query-compare-pane--primary'}>
                <div class="query-form__row query-form__row--stacked">
                    <div class="query-editor-header"><label class="query-form__label" for=${queryId}>${compare ? 'Compare query' : 'Query'}</label>
                        ${compare ? h`<button id="query-compare-close" class="query-compare-pane__close workbench-action workbench-action--ghost workbench-action--icon" type="button"
                            aria-label="Close comparison" title="Close comparison"
                            @click=${() => invoke('workbench.query.closeComparePane')}>${icon(runtime, 'close', 'query-compare-pane__close-icon')}</button>` : ''}</div>
                    <div class="query-form__field">${compare
                        ? h`<textarea id="query-compare" rows="16" cols="80" wrap="soft"></textarea>`
                        : h`<textarea id="query" name="query" rows="16" cols="80" wrap="soft">${text(options.query)}</textarea>`}</div>
                </div>
                <div class="query-form__row"><span class="query-form__label query-form__label--blank"></span>
                    <div class="query-form__field"><span id=${'queryString.errors' + suffix} class="error">${compare ? '' : text(options.error)}</span></div>
                </div>
                <div id=${'query-explanation-row' + suffix} class="query-form__row query-form__row--stacked"
                    ?hidden=${!queryFeatureEnabled(context, 'query-explain')}
                    style=${!compare && !explanation ? 'display:none;' : ''}>
                    <span class="query-form__label">Query explanation</span><div class="query-form__field">
                        <div id=${'query-explanation-status' + suffix} class="query-explanation-status" aria-live="polite"></div>
                        <div class="query-explanation-toolbar">
                            ${compare ? h`<button id="copy-explanation-compare" class="query-explanation-copy workbench-action workbench-action--secondary"
                                type="button" ?hidden=${!queryFeatureEnabled(context, 'explain-copy')}>Copy explanation</button>`
                                : h`<button id="copy-explanation" class="query-explanation-copy workbench-action workbench-action--secondary"
                                    type="button" ?hidden=${!queryFeatureEnabled(context, 'explain-copy')}>Copy explanation</button>`}
                            ${compare ? '' : h`<select id="explain-format" name="explain-format"
                                ?hidden=${allExplainFormatsDisabled}>
                                ${explainFormats.map(formatOption)}
                            </select><select id="explain-level" ?hidden=${allExplainLevelsDisabled}>
                                ${explainLevels.map(levelOption)}
                            </select>
                            ${workbench.detailDisclosure.render(h, {
                                id: 'explanation-settings', toggleId: 'explanation-settings-toggle',
                                panelId: 'explanation-settings-panel', label: 'Config', hidden: !queryExplainSettingsEnabled(context),
                                ownerClass: 'query-explanation-settings', toggleClass: 'query-explanation-settings__toggle workbench-action workbench-action--secondary',
                                panelClass: 'query-explanation-settings__panel',
                                panelRole: 'group'
                            }, h`
                                    <div class="query-explanation-settings__section" ?hidden=${allHighlightingDisabled}>
                                        <div class="query-explanation-settings__header"><strong>Highlighting</strong></div>
                                        <div class="query-explanation-settings__highlighting">
                                            <span id="explanation-highlight-mode" class="query-explanation-highlight-mode" role="radiogroup"
                                                aria-label="Text explanation highlighting" ?hidden=${allHighlightingDisabled}>
                                                <label class="workbench-choice" for="explanation-highlight-syntax"
                                                    ?hidden=${!queryFeatureEnabled(context, 'explain-highlight-syntax')}>
                                                    <input id="explanation-highlight-syntax" name="explanation-highlight-mode" type="radio" value="syntax" checked
                                                        ?hidden=${!queryFeatureEnabled(context, 'explain-highlight-syntax')} />
                                                    <span>Normal</span>
                                                </label>
                                                <label class="workbench-choice" for="explanation-highlight-hotspot"
                                                    ?hidden=${!queryFeatureEnabled(context, 'explain-highlight-hotspot')}>
                                                    <input id="explanation-highlight-hotspot" name="explanation-highlight-mode" type="radio" value="hotspot"
                                                        ?hidden=${!queryFeatureEnabled(context, 'explain-highlight-hotspot')} />
                                                    <span>Heatmap</span>
                                                </label>
                                            </span><span id="explanation-hotspot-legend" aria-live="polite"></span>
                                        </div>
                                    </div>
                                    <div id="explanation-property-config" class="query-explanation-settings__section query-explanation-property-config"
                                        ?hidden=${!propertySelectionEnabled}>
                                        <div class="query-explanation-property-config__header">
                                            <div class="query-explanation-property-config__title">
                                                <strong>Visible properties</strong><span id="explanation-property-count" aria-live="polite"></span>
                                            </div>
                                            <div class="query-explanation-property-config__actions">
                                                <button id="explanation-properties-all" class="workbench-action workbench-action--secondary" type="button" ?hidden=${!propertySelectionEnabled}>All</button>
                                                <button id="explanation-properties-none" class="workbench-action workbench-action--secondary" type="button" ?hidden=${!propertySelectionEnabled}>None</button>
                                            </div>
                                        </div>
                                        <div id="explanation-property-options" class="query-explanation-property-config__options"
                                            role="group" aria-label="Visible query plan properties"></div>
                                        <p class="query-explanation-property-config__hint">Plan structure always remains visible.</p>
                                    </div>
                            `)}
                            `}
                        </div>
                        <div class="query-explanation-surface"><div id=${'query-explanation-overlay' + suffix}
                            class="query-explanation-overlay" aria-hidden="true"></div>
                            ${compare ? h`<pre id="query-explanation-compare" data-format=${selectedFormat}
                                ?hidden=${!queryFeatureEnabled(context, 'explain-view-text')}>${explanation}</pre>`
                                : h`<pre id="query-explanation" data-format=${selectedFormat}
                                    ?hidden=${!queryFeatureEnabled(context, 'explain-view-text')}>${explanation}</pre>`}
                            ${compare ? h`<div id="query-explanation-dot-view-compare"
                                ?hidden=${!queryFeatureEnabled(context, 'explain-view-dot')}></div>`
                                : h`<div id="query-explanation-dot-view"
                                    ?hidden=${!queryFeatureEnabled(context, 'explain-view-dot')}></div>`}
                            ${compare ? h`<div id="query-explanation-json-view-compare"
                                ?hidden=${!queryFeatureEnabled(context, 'explain-view-json')}></div>`
                                : h`<div id="query-explanation-json-view"
                                    ?hidden=${!queryFeatureEnabled(context, 'explain-view-json')}></div>`}
                        </div>
                        ${compare ? '' : h`<div id="query-explanation-controls-row" class="query-explanation-controls-row-class"
                            ?hidden=${!queryFeatureEnabled(context, 'query-explain')}
                            style=${!explanation ? 'display:none;' : ''}>
                            <span id="primary-explain-settings" class="query-form__field--controls-group">
                                <span id="primary-explain-repeat-controls" class="query-form__field--controls-group">
                                    <button id="rerun-explanation" class="workbench-action workbench-action--secondary" type="button"
                                        ?hidden=${!queryFeatureEnabled(context, 'query-rerun')}
                                        data-query-rerun-enabled=${queryFeatureEnabled(context, 'query-rerun') ? 'true' : 'false'}
                                        @click=${() => invoke('workbench.query.runExplain', null, 'rerun-explanation')}>Explain again</button>
                                    <span id="rerun-explanation-spinner" class="query-explain-spinner" aria-hidden="true"></span>
                                    <button id="rerun-explanation-cancel" class="query-explain-cancel workbench-action workbench-action--secondary" type="button" disabled
                                        ?hidden=${!queryFeatureEnabled(context, 'explain-cancel')}
                                        @click=${() => invoke('workbench.query.cancelExplain')}>Cancel</button>
                                </span>
                                <span id="primary-explain-utility-controls" class="query-form__field--controls-group">
                                    <button id="download-explanation" class="workbench-action workbench-action--secondary" type="button" ?disabled=${!explanation}
                                        ?hidden=${!queryFeatureEnabled(context, 'explain-download')}>Download explanation</button>
                                    <button id="compare-toggle" class="workbench-action workbench-action--secondary" type="button" ?hidden=${!queryFeatureEnabled(context, 'query-compare')}
                                        @click=${() => invoke('workbench.query.toggleCompareMode')}>Compare</button>
                                </span>
                            </span>
                        </div>`}
                    </div>
                </div>
            </section>`;
        }

        function queryPage(runtime: LitRuntime, model: PageModel, context: ViewContext): any {
            const h = runtime.html;
            const info = workbenchData(context);
            const defaults = info.defaults || info;
            const queryFormats = formatOptions(info.queryFormats || info['query-format']);
            const defaultQueryLanguage = text(pageValue(model, 'queryLn') || defaults['default-queryLn']);
            const selectedQueryLanguage = defaultQueryLanguage || (queryFormats.length ? queryFormats[0].value : 'SPARQL');
            const hideQueryLanguageRow = queryFormats.length === 1 && queryFormats[0].value === 'SPARQL';
            const defaultTimeout = text(pageValue(model, 'query-timeout') || defaults['default-query-timeout']) || '60';
            const query = text(pageValue(model, 'query'));
            const explanation = text(pageValue(model, 'explanation'));
            const explanationFormat = text(pageValue(model, 'explanation-format')) || 'text';
            const explanationLevel = text(pageValue(model, 'explanation-level')) || 'Optimized';
            const saveDisclosure = workbench.detailDisclosure.renderSeparated(h, {
                id: 'save-query-disclosure', toggleId: 'save-query-toggle', panelId: 'save-query-panel',
                label: 'Save query', ownerClass: 'query-disclosure query-save-disclosure',
                toggleClass: 'query-disclosure__toggle workbench-action workbench-action--secondary',
                panelClass: 'query-disclosure__body query-disclosure__panel query-save-disclosure__body',
                contentClass: 'workbench-disclosure__fields',
                toggleHidden: !queryFeatureEnabled(context, 'query-save')
            }, h`<div class="workbench-disclosure__field query-save-disclosure__name-field">
                    <label class="query-form__label" for="query-name">Query name</label>
                    <input id="query-name" name="query-name" type="text" size="32" maxlength="32" value="" />
                </div>
                <label class="query-option query-save-disclosure__private"
                    ?hidden=${!queryFeatureEnabled(context, 'query-private-save')}>
                    <input id="save-private" name="save-private" type="checkbox" value="true"
                        ?hidden=${!queryFeatureEnabled(context, 'query-private-save')} />Private</label>
                <div class="workbench-disclosure__actions query-disclosure__actions">
                    <input id="save" type="submit" value="Save" disabled
                        ?hidden=${!queryFeatureEnabled(context, 'query-save')} /> <span id="save-feedback"></span>
                </div>`);
            const optionsDisclosure = workbench.detailDisclosure.renderSeparated(h, {
                id: 'query-options-disclosure', toggleId: 'query-options-toggle',
                panelId: 'query-options-panel', label: 'Options',
                ownerClass: 'query-disclosure query-options-disclosure',
                toggleClass: 'query-disclosure__toggle workbench-action workbench-action--secondary',
                panelClass: 'query-disclosure__body query-disclosure__panel',
                contentClass: 'workbench-disclosure__fields',
                toggleHidden: !queryFeatureEnabled(context, 'query-options')
            }, h`<div class="workbench-disclosure__field">
                    <label for="query-timeout">Query timeout</label>
                    <input id="query-timeout" name="query-timeout" type="number" min="0" step="1"
                        value=${defaultTimeout} ?hidden=${!queryFeatureEnabled(context, 'query-timeout')} />
                </div>
                <div class="workbench-disclosure__field">
                    <label class="query-option" for="infer"><input id="infer" name="infer" type="checkbox" value="true"
                        ?checked=${text(defaults['default-infer']) === 'true'}
                        ?hidden=${!queryFeatureEnabled(context, 'query-inferred-statements')} />
                        <span>Include inferred statements</span></label>
                </div>
                <div class="workbench-disclosure__actions query-disclosure__actions"><input id="query-reset-namespaces" type="button" value="Clear"
                    data-editor-namespaces-enabled=${queryFeatureEnabled(context, 'editor-namespaces') ? 'true' : 'false'}
                    ?hidden=${!queryFeatureEnabled(context, 'editor-namespaces')}
                    @click=${() => invoke('workbench.query.resetNamespaces')} /></div>`);
            return h`<div id="query-page" class="query-page"
                    data-editor-fullscreen-enabled=${queryFeatureEnabled(context, 'editor-fullscreen') ? 'true' : 'false'}>
                <form id="query-form" action="query" method="post"
                    data-workbench-query-execution="true" data-workbench-results-target="query-results">
                    <input type="hidden" name="action" id="action" />
                    <input type="hidden" name="explain" id="explain" />
                    <input type="hidden" name="ref" value="text" />
                    <input type="hidden" name="include-query-text" id="include-query-text" value="false" />
                    <input type="hidden" name="query-request-id" id="query-request-id" value="" />
                    <div class="query-form">
                        <div id="query-language-row" class="query-form__row"
                            ?hidden=${hideQueryLanguageRow || !queryFeatureEnabled(context, 'query-language')}>
                            <label class="query-form__label" for="queryLn">Query language</label>
                            <div class="query-form__field"><select id="queryLn" name="queryLn"
                                @change=${() => invoke('workbench.query.onQlChange')}>
                                ${queryFormats.length ? queryFormats.map((option: any) => h`<option value=${option.value}
                                    ?selected=${option.value === selectedQueryLanguage}>${option.label}</option>`)
                                    : h`<option value="${selectedQueryLanguage}" selected>${selectedQueryLanguage}</option>`}
                            </select></div>
                        </div>
                        <div id="query-compare-toolbar" class="query-compare-toolbar" ?hidden=${!queryFeatureEnabled(context, 'query-compare')}>
                            <span id="query-sidebar-toggle-action" class="workbench-action workbench-action--secondary"
                                data-workbench-action="menu"><span class="workbench-action-label">
                                <button id="query-sidebar-toggle" class="query-sidebar-toggle" type="button"
                                    aria-hidden="true" aria-expanded="false" aria-controls="navigation" tabindex="-1"
                                    data-show-label="Show navigation" data-hide-label="Hide navigation"
                                    ?hidden=${!queryFeatureEnabled(context, 'editor-sidebar')}
                                    @click=${() => invoke('workbench.query.toggleCompareSidebar')}>
                                    <span id="query-sidebar-toggle-icon" class="query-sidebar-toggle__icon" aria-hidden="true">
                                        ${icon(runtime, 'menu', 'query-sidebar-toggle__svg')}
                                    </span>
                                </button></span></span>
                            <button id="query-compare-copy" class="workbench-action workbench-action--secondary" type="button"
                                ?hidden=${!queryFeatureEnabled(context, 'explain-copy')}>Copy</button>
                            <button id="query-compare-swap" class="workbench-action workbench-action--secondary" type="button"
                                ?hidden=${!queryFeatureEnabled(context, 'query-swap')}>Swap</button>
                            <div id="query-compare-controls" class="query-compare-toolbar__actions">
                                <button id="explain-compare-trigger" class="query-compare-action workbench-action workbench-action--secondary" type="button"
                                    data-query-refresh-enabled=${queryFeatureEnabled(context, 'query-refresh') ? 'true' : 'false'}
                                    ?hidden=${!queryFeatureEnabled(context, 'query-refresh') || !queryExplainEnabled(context)}
                                    @click=${() => invoke('workbench.query.runCompareExplain')}>Refresh explanations</button>
                                <button id="explain-compare-cancel" class="query-compare-action query-explain-cancel workbench-action workbench-action--secondary" type="button" disabled
                                    ?hidden=${!queryFeatureEnabled(context, 'explain-cancel')}>
                                    <span id="explain-compare-cancel-icon" class="query-compare-action__svg--cancel" aria-hidden="true">×</span>Cancel</button>
                                <button id="query-diff-trigger" class="query-compare-action workbench-action workbench-action--secondary" type="button" disabled
                                    ?hidden=${!queryFeatureEnabled(context, 'query-diff')}
                                    @click=${() => invoke('workbench.query.openDiffModal')}>
                                    <span id="query-diff-trigger-icon" class="query-compare-action__icon" aria-hidden="true">⇄</span>Diff</button>
                            </div>
                        </div>
                        <div id="query-compare-layout" class="query-compare-layout"
                            ?hidden=${!queryFeatureEnabled(context, 'query-compare')}>
                            ${queryPane(runtime, { query, explanation, explanationFormat, explanationLevel,
                                error: pageValue(model, 'error-message') }, context)}
                            ${queryPane(runtime, { compare: true }, context)}
                        </div>
                        <div class="query-actions-toolbar workbench-action-toolbar"><div class="query-form__field query-actions-toolbar__primary workbench-action-toolbar__primary">
                            <button id="exec" class="query-action workbench-action workbench-action--primary" type="submit"
                                ?hidden=${!queryFeatureEnabled(context, 'query-execution')}>${icon(runtime, 'execute')}<span>Execute</span></button>
                            <input id="query-cancel" class="query-cancel workbench-action workbench-action--secondary" type="button" value="Cancel" aria-hidden="true" disabled
                                ?hidden=${!queryFeatureEnabled(context, 'query-cancel')}
                                @click=${() => invoke('workbench.query.cancelQuery')} />
                            <button id="explain-trigger" class="query-action workbench-action workbench-action--secondary" type="button"
                                ?hidden=${!queryExplainEnabled(context)}
                                @click=${() => invoke('workbench.query.runExplain', null, 'explain-trigger')}>${icon(runtime, 'explain')}<span>Explain</span></button>
                            <span id="explain-trigger-spinner" class="query-explain-spinner" aria-hidden="true"></span>
                            <span id="explain-trigger-cancel-action" class="workbench-action workbench-action--danger-outline query-explain-cancel"
                                ?hidden=${!queryFeatureEnabled(context, 'explain-cancel')}
                                data-workbench-action="cancel"><span id="explain-trigger-cancel-icon" aria-hidden="true"></span>
                                <input id="explain-trigger-cancel" type="button" value="Cancel" aria-hidden="true" disabled
                                    ?hidden=${!queryFeatureEnabled(context, 'explain-cancel')}
                                    @click=${() => invoke('workbench.query.cancelExplain')} /></span>
                        </div>
                        <div class="workbench-action-toolbar__actions">
                            <div class="workbench-action-toolbar__group">${saveDisclosure.owner}${optionsDisclosure.owner}</div>
                        </div>
                        <div class="workbench-action-toolbar__panels workbench-disclosure-track">
                            ${saveDisclosure.panel}${optionsDisclosure.panel}
                        </div>
                    </div>
                    </div>
                </form>
                <section id="query-results" class="query-results" aria-busy="false" hidden aria-labelledby="query-results-heading">
                    <div class="query-results__header workbench-action-toolbar">
                        <div class="workbench-action-toolbar__primary"><h2 id="query-results-heading">Query result</h2></div>
                        <div class="workbench-action-toolbar__actions">
                        <button id="query-results-fullscreen" class="query-results__fullscreen workbench-action workbench-action--secondary" type="button"
                            aria-label="Full screen" title="Full screen" hidden aria-pressed="false"
                            data-result-fullscreen-enabled=${queryFeatureEnabled(context, 'result-fullscreen') ? 'true' : 'false'}
                            @click=${() => invoke('workbench.query.toggleResultsFullscreen')}>
                            <svg class="query-results__fullscreen-icon" viewBox="0 0 24 24" focusable="false" aria-hidden="true">
                                <path d="M4 9V4h5M15 4h5v5M20 15v5h-5M9 20H4v-5"></path>
                            </svg><span class="query-results__fullscreen-label">Full screen</span>
                        </button>
                        </div>
                    </div>
                    <div id="query-results-loading" class="query-results__loading" hidden role="status" aria-live="polite">Loading query results...</div>
                    <div id="query-results-status" class="query-results__status" role="status" aria-live="polite"></div>
                    <iframe id="query-results-frame" name="query-results-frame" class="query-results__frame"
                        title="Query results" hidden></iframe>
                </section>
                <div id="query-diff-modal" class="query-diff-modal" aria-hidden="true">
                    <div class="query-diff-modal__dialog" role="dialog" aria-modal="true" aria-labelledby="query-diff-modal-title">
                        <div class="query-diff-modal__header"><div id="query-diff-modal-title" class="query-diff-modal__title">Query diff</div>
                            <button id="query-diff-close" class="workbench-action workbench-action--secondary" type="button" value="Close"
                                @click=${() => invoke('workbench.query.closeDiffModal')}>Close</button></div>
                        <div class="query-diff-modal__body">
                            <section class="query-diff-section query-diff-section--query"><div class="query-diff-section__title">Query</div><div id="query-diff-query" class="query-diff-view"></div></section>
                            <section class="query-diff-section query-diff-section--explanation"><div class="query-diff-section__title">Explanation</div><div id="query-diff-explanation" class="query-diff-view">Comparison is not ready.</div></section>
                        </div>
                    </div>
                </div>
            </div>`;
        }

        function routeBody(model: PageModel, context: ViewContext, runtime: LitRuntime): any {
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
                case 'query': return runtime.html`<div id="query-page-content">${queryPage(runtime, model, context)}</div>`;
                case 'saved-queries': return savedQueriesPage(runtime, model, context);
                case 'export': return exportPage(runtime, model, context);
                case 'add': return addPage(runtime, model, context);
                case 'remove': return removePage(runtime, model, context);
                case 'clear': return clearPage(runtime, model, context);
                case 'update': return updatePage(runtime, model, context);
                case 'server': return serverPage(runtime, model, context);
                default: return runtime.html`<p class="error" role="alert">Unsupported Workbench view: ${model.viewId}</p>`;
            }
        }

        /** Return a route template without mutating the DOM (also used by unit tests). */
        export function pageTemplate(model: PageModel, context: ViewContext, runtime: LitRuntime): any {
            return shell(model, context, runtime, routeBody(model, context, runtime));
        }

        const repositoryScopedViews: { [view: string]: boolean } = {
            summary: true, namespaces: true, contexts: true, types: true, explore: true, query: true,
            'saved-queries': true, export: true, update: true, add: true, remove: true, clear: true
        };

        /** Where choosing a repository in the switcher leads: the same view when it is repository-scoped. */
        export function repositorySwitchTarget(basePath: string, activeView: string, repositoryId: string): string {
            const view = repositoryScopedViews[activeView] ? activeView : 'summary';
            return (basePath || '').replace(/\/+$/, '') + '/repositories/' + encodeURIComponent(repositoryId) + '/' + view;
        }

        function repositoryOptions(panel: any): any[] {
            const list = panel.querySelector('#workbench-repository-options');
            const options: any[] = [];
            const links = list ? list.querySelectorAll('a.workbench-popover__option') : [];
            for (let index = 0; index < links.length; index++) {
                if (!links[index].parentElement.hidden) {
                    options.push(links[index]);
                }
            }
            return options;
        }

        function renderRepositoryOptions(panel: any, repositories: any[], currentId: string): void {
            const document = panel.ownerDocument;
            const list = panel.querySelector('#workbench-repository-options');
            const basePath = panel.getAttribute('data-workbench-base-path') || '';
            const activeView = panel.getAttribute('data-workbench-active-view') || '';
            while (list.firstChild) {
                list.removeChild(list.firstChild);
            }
            repositories.forEach((repository: any) => {
                const item = document.createElement('li');
                const link = document.createElement('a');
                link.className = 'workbench-popover__option';
                link.setAttribute('href', repositorySwitchTarget(basePath, activeView, repository.id));
                link.setAttribute('data-repository-id', repository.id);
                link.setAttribute('data-repository-search', (repository.id + ' ' + repository.title).toLowerCase());
                if (repository.id === currentId) {
                    link.setAttribute('aria-current', 'true');
                }
                const id = document.createElement('span');
                id.className = 'workbench-popover__option-id';
                id.textContent = repository.id;
                link.appendChild(id);
                if (repository.title) {
                    const title = document.createElement('span');
                    title.className = 'workbench-popover__option-title';
                    title.textContent = repository.title;
                    link.appendChild(title);
                }
                item.appendChild(link);
                list.appendChild(item);
            });
            filterRepositoryOptions(panel);
        }

        function filterRepositoryOptions(panel: any): void {
            const input = panel.querySelector('#workbench-repository-filter');
            const needle = input ? String(input.value || '').trim().toLowerCase() : '';
            const links = panel.querySelectorAll('a.workbench-popover__option');
            let visible = 0;
            for (let index = 0; index < links.length; index++) {
                const matches = !needle || links[index].getAttribute('data-repository-search').indexOf(needle) >= 0;
                links[index].parentElement.hidden = !matches;
                if (matches) { visible++; }
            }
            const status = panel.querySelector('.workbench-popover__status');
            if (status && panel.__rdf4jRepositoriesLoaded) {
                status.textContent = visible ? '' : (links.length ? 'No matching repositories.' : 'No repositories are available.');
            }
        }

        function loadRepositoryOptions(panel: any, currentId: string): Promise<void> {
            if (panel.__rdf4jRepositoriesLoading) {
                return panel.__rdf4jRepositoriesLoading;
            }
            const app: any = (workbench as any).app;
            const status = panel.querySelector('.workbench-popover__status');
            if (!app || typeof app.loadModel !== 'function' || typeof window === 'undefined') {
                return Promise.resolve();
            }
            if (status) { status.textContent = 'Loading repositories…'; }
            const url = new URL(panel.getAttribute('data-workbench-repositories-url'), window.location.href).toString();
            const loading = app.loadModel(window.fetch.bind(window), url).then((model: any) => {
                return model.rowStore.read(0, model.rowCount).then((rows: any[][]) => {
                    model.rowStore.dispose();
                    return recordsFromRows(model, rows).map((record: any) => ({
                        id: text(record.id), title: text(record.description)
                    })).filter((repository: any) => !!repository.id);
                }, (error: any) => {
                    model.rowStore.dispose();
                    throw error;
                });
            }).then((repositories: any[]) => {
                panel.__rdf4jRepositoriesLoaded = true;
                renderRepositoryOptions(panel, repositories, currentId);
            }, (error: any) => {
                panel.__rdf4jRepositoriesLoading = null;
                if (status) {
                    status.textContent = 'Unable to load repositories: ' + (error && error.message ? error.message : String(error));
                }
            });
            panel.__rdf4jRepositoriesLoading = loading;
            return loading;
        }

        function moveRepositoryFocus(panel: any, from: any, delta: number): void {
            const options = repositoryOptions(panel);
            const input = panel.querySelector('#workbench-repository-filter');
            const index = options.indexOf(from);
            const next = index + delta;
            if (next < 0) {
                if (input) { input.focus(); }
            } else if (next < options.length) {
                options[next].focus();
            }
        }

        /**
         * Bind the context bar switchers once per shell: popovers (workbench.popover from template.ts),
         * the lazily loaded repository list, its filter and arrow-key movement.
         */
        export function bindContextBar(appMount: any, context: ViewContext): () => void {
            const document = appMount && appMount.ownerDocument;
            const popover: any = (workbench as any).popover;
            if (!document || !document.getElementById || !popover || typeof popover.bind !== 'function') {
                return () => {};
            }
            const disposers: Array<() => void> = [];
            const currentId = contextBarState(context).repositoryId;
            ['server', 'repository', 'user'].forEach((name) => {
                const button = document.getElementById('workbench-' + name + '-switcher');
                const panel = document.getElementById('workbench-' + name + '-popover');
                disposers.push(popover.bind(button, panel, name === 'repository'
                    ? { onOpen: (opened: any) => { loadRepositoryOptions(opened, currentId); } } : {}));
            });
            const repositoryPanel = document.getElementById('workbench-repository-popover');
            const filter = document.getElementById('workbench-repository-filter');
            if (repositoryPanel && filter && !repositoryPanel.__rdf4jContextBarBound) {
                repositoryPanel.__rdf4jContextBarBound = true;
                const onInput = () => filterRepositoryOptions(repositoryPanel);
                const onKey = (event: any) => {
                    const target = event.target;
                    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
                        event.preventDefault();
                        if (target === filter) {
                            if (event.key === 'ArrowDown') {
                                const options = repositoryOptions(repositoryPanel);
                                if (options.length) { options[0].focus(); }
                            }
                        } else if (target && target.classList && target.classList.contains('workbench-popover__option')) {
                            moveRepositoryFocus(repositoryPanel, target, event.key === 'ArrowDown' ? 1 : -1);
                        }
                    } else if (event.key === 'Enter' && target === filter) {
                        const options = repositoryOptions(repositoryPanel);
                        if (options.length === 1) {
                            event.preventDefault();
                            options[0].click();
                        }
                    }
                };
                filter.addEventListener('input', onInput);
                repositoryPanel.addEventListener('keydown', onKey);
                disposers.push(() => {
                    filter.removeEventListener('input', onInput);
                    repositoryPanel.removeEventListener('keydown', onKey);
                    repositoryPanel.__rdf4jContextBarBound = false;
                });
            }
            return () => disposers.forEach((dispose) => dispose());
        }

        const outletsByMount = new WeakMap<Element, Element>();
        const outletElements = new WeakSet<Element>();

        function outletNodeFor(appMount: any): Element | null {
            const existing = outletsByMount.get(appMount);
            if (existing) {
                return existing;
            }
            const document = appMount && appMount.ownerDocument;
            if (!document || typeof document.createElement !== 'function') {
                return null;
            }
            const outlet = document.createElement('div');
            outlet.id = 'workbench-outlet';
            outlet.className = 'workbench-outlet';
            outlet.setAttribute('tabindex', '-1');
            outletsByMount.set(appMount, outlet);
            outletElements.add(outlet);
            return outlet;
        }

        /** The outlet element that renderShell placed inside an application mount, if any. */
        export function outletOf(appMount: any): Element | null {
            return appMount ? outletsByMount.get(appMount) || null : null;
        }

        function prepareRowRegions(target: any, model: PageModel): RowRegions {
            let regions = rowRegionsByMount.get(target);
            if (model.rowStore && target.ownerDocument && target.ownerDocument.createElement) {
                if (!regions || regions.model !== model) {
                    regions = { model, document: target.ownerDocument, savedCards: {}, groups: {} };
                    rowRegionsByMount.set(target, regions);
                }
                return regions;
            }
            rowRegionsByMount.delete(target);
            return null;
        }

        function renderRowRegions(regions: RowRegions): void {
            if (regions) {
                if (regions.renderTableRows) { regions.renderTableRows(); }
                if (regions.renderSavedRows) { regions.renderSavedRows(); }
                Object.keys(regions.groups).forEach((key) => regions.groups[key].render());
            }
        }

        /**
         * Render the persistent shell (header, menu, footer) into the application mount and return the
         * outlet element that renderOutlet fills. Returns null when the mount has no DOM document.
         */
        export function renderShell(appMount: Element, shellState: ShellState, runtime: LitRuntime): Element | null {
            const outlet = outletNodeFor(appMount);
            if (!outlet) {
                return null;
            }
            runtime.render(shellTemplate(shellState, runtime, outlet), appMount);
            const document: any = (appMount as any).ownerDocument;
            if (document && 'title' in document) {
                document.title = documentTitle(shellState.viewId, shellState.context.repositoryId);
            }
            return outlet;
        }

        /** Render a route's title and page surface into the outlet. */
        export function renderOutlet(outletMount: Element, model: PageModel, context: ViewContext,
                                     runtime: LitRuntime): Element {
            const regions = prepareRowRegions(outletMount, model);
            const renderedContext = regions ? { ...context, rowRegions: regions } : context;
            runtime.render(outletContentTemplate(model, runtime, routeBody(model, renderedContext, runtime)), outletMount);
            renderRowRegions(regions);
            return outletMount;
        }

        /** Render a complete route: the shell into the mount and the route into its outlet. */
        export function render(mount: Element, model: PageModel, context: ViewContext,
                               runtime: LitRuntime): Element {
            if (outletElements.has(mount)) {
                renderOutlet(mount, model, context, runtime);
                return mount;
            }
            const outlet = renderShell(mount, { viewId: model.viewId, context }, runtime);
            if (outlet) {
                renderOutlet(outlet, model, context, runtime);
                return mount;
            }
            // Without a DOM document (unit-test fakes) the complete page renders as one template.
            const regions = prepareRowRegions(mount, model);
            const renderedContext = regions ? { ...context, rowRegions: regions } : context;
            runtime.render(pageTemplate(model, renderedContext, runtime), mount);
            renderRowRegions(regions);
            return mount;
        }

        /** Keep streamed route rows in the worker store and render only viewport windows. */
        export function bindRowWindows(mount: any, model: PageModel, context: ViewContext,
                                       runtime: LitRuntime): Promise<() => void> {
            const targetWindow: any = typeof window !== 'undefined' ? window : null;
            const stream: any = (workbench as any).queryStream;
            const HeightIndex = stream && stream.MeasuredRowHeights;
            // Row regions belong to the outlet when the mount is an application mount with a shell.
            const regionKey = outletOf(mount) || mount;
            const windowSize = model.pickerPageSize || 50;
            const hasQuery = (selector: string): boolean => !!(mount && mount.querySelectorAll
                && mount.querySelectorAll(selector) && mount.querySelectorAll(selector).length);
            const hasRows = model.rowCount > 0 && model.rowStore
                && hasQuery('[data-workbench-row-table="true"], [data-workbench-row-list="true"]');
            const hasPicker = hasQuery('[data-workbench-window-picker]');
            if (!targetWindow || !model.rowStore || !hasRows && !hasPicker) {
                return Promise.resolve(() => {});
            }
            if (hasRows && typeof HeightIndex !== 'function') {
                return Promise.reject(new Error('Measured Workbench row-window geometry is unavailable'));
            }

            let disposed = false;
            let generation = 0;
            let groupGeneration = 0;
            let heights: any = hasRows
                ? new HeightIndex(model.rowCount, model.viewId === 'saved-queries' ? 240 : 44) : null;
            let executionDisposers: Array<() => void> = [];

            const elements = (selector: string): any[] => {
                if (!mount || !mount.querySelectorAll) { return []; }
                const list = mount.querySelectorAll(selector);
                const result: any[] = [];
                for (let index = 0; list && index < list.length; index++) { result.push(list[index]); }
                return result;
            };

            const disposeExecutionForms = () => {
                executionDisposers.forEach((dispose) => dispose());
                executionDisposers = [];
            };

            const bindExecutionForms = () => {
                if (model.viewId !== 'saved-queries' || !stream
                        || typeof stream.bindExecutionForms !== 'function') {
                    return;
                }
                const bound = stream.bindExecutionForms(mount, { workbench: context.workbench });
                executionDisposers = [];
                if (typeof bound === 'function') {
                    executionDisposers.push(bound);
                } else if (Array.isArray(bound)) {
                    bound.forEach((dispose: any) => {
                        if (typeof dispose === 'function') { executionDisposers.push(dispose); }
                    });
                }
            };

            function bindExploreControls(): void {
                if (model.viewId !== 'explore') { return; }
                elements('[data-workbench-explore-action]').forEach((button: any) => {
                    if (button.__rdf4jWorkbenchExploreBound || !button.addEventListener) { return; }
                    const groupKey = button.getAttribute('data-workbench-explore-group');
                    const action = button.getAttribute('data-workbench-explore-action');
                    button.addEventListener('click', (event: any) => {
                        if (event && event.preventDefault) { event.preventDefault(); }
                        const states = exploreGroupStates(model);
                        const state = states[groupKey];
                        const page = (model as any).exploreSummary
                            && (model as any).exploreSummary.groups[groupKey];
                        if (!state || !page || disposed) { return; }
                        if (action === 'previous') {
                            const previous = state.history.pop();
                            if (!previous) { return; }
                            state.cursor = previous.cursor;
                            state.start = previous.start;
                        } else {
                            if (!page.hasNext || !page.nextCursor || !page.items.length) { return; }
                            state.history.push({ cursor: state.cursor, start: state.start });
                            state.cursor = page.nextCursor;
                            state.start += page.items.length;
                        }
                        const activeGeneration = ++groupGeneration;
                        prepareExploreSummary(model).then((summary) => {
                            if (disposed || activeGeneration !== groupGeneration) { return; }
                            (model as any).exploreSummary = summary;
                            const regions = rowRegionsByMount.get(regionKey);
                            if (regions) {
                                Object.keys(regions.groups).forEach((key) => regions.groups[key].render());
                            }
                            bindExploreControls();
                        });
                    });
                    button.__rdf4jWorkbenchExploreBound = true;
                });
            }

            const renderCurrent = () => {
                render(mount, model, context, runtime);
                bindExecutionForms();
                bindPickerControls();
                bindExploreControls();
            };

            const renderCurrentRows = () => {
                const regions = rowRegionsByMount.get(regionKey);
                if (regions) {
                    if (regions.renderTableRows) { regions.renderTableRows(); }
                    if (regions.renderSavedRows) { regions.renderSavedRows(); }
                }
                bindExecutionForms();
            };

            const bindPickerControls = () => {
                elements('[data-workbench-window-action]').forEach((button: any) => {
                    if (button.__rdf4jWorkbenchWindowBound || !button.addEventListener) { return; }
                    const action = button.getAttribute('data-workbench-window-action');
                    const click = (event: any) => {
                        if (event && event.preventDefault) { event.preventDefault(); }
                        const delta = action === 'previous' ? -1 : 1;
                        const maximum = Math.max(0, Math.floor((model.rowCount - 1) / windowSize) * windowSize);
                        const start = Math.max(0, Math.min(maximum, (model.pickerStart || 0) + delta * windowSize));
                        if (start === model.pickerStart || disposed) { return; }
                        generation++;
                        model.rowStore.read(start, windowSize).then((rows: any[][]) => {
                            if (disposed) { return; }
                            model.pickerStart = start;
                            model.pickerRows = rows;
                            renderCurrent();
                        });
                    };
                    button.addEventListener('click', click);
                    button.__rdf4jWorkbenchWindowBound = true;
                });
            };

            const measureRows = (start: number, end: number) => {
                elements('[data-workbench-row-index]').forEach((row: any) => {
                    const index = Number(row.getAttribute('data-workbench-row-index'));
                    const rectangle = row.getBoundingClientRect ? row.getBoundingClientRect() : null;
                    if (rectangle && isFinite(index) && index >= start && index < end && rectangle.height > 0) {
                        heights.measure(index, rectangle.height);
                    }
                });
            };

            const refreshRows = (paint: boolean = true): Promise<void> => {
                if (!hasRows || disposed) { return Promise.resolve(); }
                const activeGeneration = ++generation;
                heights.resize(model.rowCount);
                const targets = elements('[data-workbench-row-table="true"], [data-workbench-row-list="true"]');
                const target = targets.length ? targets[0] : null;
                if (!target) { return Promise.resolve(); }
                const rectangle = target.getBoundingClientRect ? target.getBoundingClientRect() : { top: 0 };
                const scrollTop = Math.max(0, -Number(rectangle.top || 0));
                const viewportHeight = Math.max(1, Number(targetWindow.innerHeight) || 600);
                const range = heights.range(scrollTop, viewportHeight, 4, 80);
                return model.rowStore.read(range.start, range.end - range.start).then((rows: any[][]) => {
                    if (disposed || activeGeneration !== generation) { return; }
                    model.rows = rows;
                    model.rowStart = range.start;
                    model.rowTopSpacer = range.topSpacer;
                    model.rowBottomSpacer = range.bottomSpacer;
                    if (paint) { renderCurrentRows(); }
                    measureRows(range.start, range.end);
                    const measuredRange = heights.range(scrollTop, viewportHeight, 4, 80);
                    const topSpacer = heights.offsetOf(measuredRange.start);
                    const bottomSpacer = heights.offsetOf(model.rowCount) - heights.offsetOf(measuredRange.end);
                    const moved = measuredRange.start !== range.start || measuredRange.end !== range.end;
                    const spacerChanged = Math.abs(topSpacer - model.rowTopSpacer) > 0.5
                        || Math.abs(bottomSpacer - model.rowBottomSpacer) > 0.5;
                    if (moved) {
                        return refreshRows(paint);
                    }
                    if (spacerChanged && !disposed) {
                        model.rowTopSpacer = topSpacer;
                        model.rowBottomSpacer = bottomSpacer;
                        if (paint) { renderCurrentRows(); }
                    }
                });
            };

            const refresh = () => {
                if (!hasRows) { return Promise.resolve(); }
                return refreshRows();
            };

            const onScroll = () => refresh();
            const onResize = () => refresh();
            const initialPicker = hasPicker && model.rowCount > 0
                ? model.rowStore.read(0, windowSize).then((rows: any[][]) => {
                    if (!disposed) {
                        model.pickerRows = rows;
                        model.pickerStart = 0;
                    }
                })
                : Promise.resolve();
            return initialPicker.then(() => prepareExploreSummary(model)).then((summary: any) => {
                if (summary) { (model as any).exploreSummary = summary; }
                return refreshRows(false);
            }).then(() => {
                renderCurrent();
                return refreshRows();
            }).then(() => {
                bindPickerControls();
                bindExploreControls();
                if (hasRows && targetWindow.addEventListener) {
                    targetWindow.addEventListener('scroll', onScroll, { passive: true });
                    targetWindow.addEventListener('resize', onResize);
                }
                return () => {
                    disposed = true;
                    generation++;
                    groupGeneration++;
                    disposeExecutionForms();
                    const regions = rowRegionsByMount.get(regionKey);
                    if (regions && regions.model === model) { rowRegionsByMount.delete(regionKey); }
                    if (hasRows && targetWindow.removeEventListener) {
                        targetWindow.removeEventListener('scroll', onScroll);
                        targetWindow.removeEventListener('resize', onResize);
                    }
                };
            });
        }
    }
}
