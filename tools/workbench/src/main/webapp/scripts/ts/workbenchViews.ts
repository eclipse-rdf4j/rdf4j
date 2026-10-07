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
        // A circled stop square, filled by the strokes of a square, a smaller square and a dot that overlap.
        stop: 'M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16ZM10 10h4v4h-4ZM11 11h2v2h-2ZM12 12h.01',
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
        // A failed write's status (C12): the error callout's circled exclamation mark.
        error: 'M12 3.5a8.5 8.5 0 1 0 0 17 8.5 8.5 0 0 0 0-17ZM12 8v5m0 3h.01',
        'warning-sign': 'M12 4 2.8 20h18.4L12 4Zm0 6v4.5m0 2.5h.01'
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
            /** The repository id of a URL whose repository does not exist (repository-not-found). */
            missingRepositoryId?: string;
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
            /** The result tables built for this mount of the page, by key (resultTable). */
            tableEntries?: { [key: string]: PageResultTable };
            /** The result tables the current render of the page uses, by key. */
            resultTables?: { [key: string]: PageResultTable };
        }

        /** A page's table of RDF terms: its host element and the shared result table in it. */
        interface PageResultTable {
            host: any;
            table: any;
            signature: string;
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

        /** One line under each title that says what the page is for (the layout preview's copy). */
        const descriptions: { [key: string]: string } = {
            summary: 'See where this repository is stored, how much data it holds and how it is configured.',
            information: 'Check the Workbench version and the Java runtime, operating system and memory it runs on.',
            repositories: 'Every repository on this server. Open one to work with its data.',
            create: 'Set up a new repository on this server.',
            delete: 'Permanently delete a repository and all of its data from the server.',
            namespaces: 'Manage the prefixes this repository uses to shorten IRIs in queries and results.',
            contexts: 'See the named graphs in this repository and how many statements each holds.',
            types: 'See the classes used in this repository and how many resources belong to each.',
            explore: 'Look up a resource and see every statement it appears in.',
            query: 'Run SPARQL queries against this repository.',
            'saved-queries': 'Run the queries saved for this repository again.',
            export: 'Download all the data in this repository as an RDF file.',
            add: 'Load RDF data into this repository.',
            remove: 'Remove the statements that match a subject, predicate, object or graph.',
            clear: 'Remove all statements from this repository, or from one of its graphs.',
            update: 'Change the data in this repository with SPARQL Update.',
            server: 'Choose which RDF4J Server this Workbench connects to.'
        };

        /** The sidebar icon of a page when the policy names none; keep in sync with the page definitions in WorkbenchPolicy.java. */
        const defaultPageIcons: { [key: string]: string } = { 'saved-queries': 'saved' };

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

        function isRepositoryNotFound(model: PageModel): boolean {
            return !!model.error && model.error.code === 'repository-not-found';
        }

        function routeTitle(model: PageModel): string {
            if (isRepositoryNotFound(model)) {
                return 'Repository not found';
            }
            return text(meta(model, 'title')) || titles[model.viewId] || 'RDF4J Workbench';
        }

        /** The page's one-line description; Repositories names the connected server. */
        function routeDescription(model: PageModel, context: ViewContext): string {
            const description = descriptions[model.viewId] || '';
            if (model.viewId === 'repositories') {
                const server = contextBarState(context).server;
                return server ? description.replace('this server', hostAndPort(server)) : description;
            }
            return description;
        }

        /** The icon the sidebar shows for a page: the policy's menu item icon, else the page's default icon. */
        function pageIcon(context: ViewContext, viewId: string): string {
            const groups = menuEntries(context, true);
            for (let g = 0; g < groups.length; g++) {
                const items = groups[g].items || [];
                for (let i = 0; i < items.length; i++) {
                    const item = items[i];
                    if ((item.id || item['menu-item-id']) === viewId) {
                        return item.icon || item['menu-item-icon'] || defaultPageIcons[viewId] || viewId;
                    }
                }
            }
            return defaultPageIcons[viewId] || viewId;
        }

        /** In-shell page for an unknown repository (mockup 13). */
        function repositoryNotFoundPage(runtime: LitRuntime, context: ViewContext): any {
            const h = runtime.html;
            const server = contextBarState(context).server;
            const id = context.missingRepositoryId || '';
            return h`<section id="repository-not-found" class="workbench-island workbench-not-found" aria-labelledby="repository-not-found-title">
                ${icon(runtime, 'repository', 'workbench-not-found__icon')}
                <h2 id="repository-not-found-title">Repository not found</h2>
                <p>The server${server ? h` at <code>${hostAndPort(server)}</code>` : ''} has no repository with id
                    <code>${id}</code>. It may have been deleted or renamed.</p>
                <div class="workbench-form-actions workbench-not-found__actions">
                    ${pageEnabled(context, 'repositories') ? h`<a class="workbench-action workbench-action--primary"
                        href=${urlFor(context, 'repositories')}>Go to repositories</a>` : ''}
                    ${pageEnabled(context, 'server') ? h`<a class="workbench-action workbench-action--secondary"
                        href=${urlFor(context, 'server')}>Change server</a>` : ''}
                </div>
            </section>`;
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

        /**
         * False when the Workbench policy hides a page: the policy-filtered menu of the Info model lists items and none
         * for this page. Without menu items nothing is known to be hidden: an Info answer with an empty menu (a user the
         * server does not authorize, a failed Info) must still offer the pages to sign in or pick another repository.
         */
        function pageEnabled(context: ViewContext, pageId: string): boolean {
            const info = normalizeWorkbench(context.workbench, context.linked && context.linked.info);
            const known = ['menu', 'menuGroups', 'menuItems'].some((key: string) =>
                Object.prototype.hasOwnProperty.call(info, key));
            if (!known) {
                return true;
            }
            const items = menuEntries(context).reduce((all: any[], group: any) => all.concat(group.items || []), []);
            return !items.length || items.some((item: any) => text(item.id || item['menu-item-id']) === pageId);
        }

        /** The pages a repository can open with, in the order the server picks its landing page. */
        const repositoryLandingPages = ['summary', 'query', 'explore', 'namespaces', 'contexts', 'types', 'saved-queries',
            'export', 'update', 'add', 'remove', 'clear'];

        /** The first page of a repository the policy shows (Summary unless it is hidden). */
        function repositoryLanding(context: ViewContext): string {
            return repositoryLandingPages.filter((pageId: string) => pageEnabled(context, pageId))[0] || 'summary';
        }

        /** The menu groups of the policy-filtered Info model; `quiet` skips the missing-menu warning the shell already logs. */
        function menuEntries(context: ViewContext, quiet?: boolean): any[] {
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
            if (!quiet && typeof console !== 'undefined' && console.error) {
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

        /** A query of this repository running or finished while its Query page is not shown (M13.5). */
        function queryActivity(context: ViewContext): string {
            const page: any = (workbench as any).queryPage;
            return page && typeof page.activity === 'function' ? page.activity(context.repositoryId || '') : '';
        }

        function navigationItem(runtime: LitRuntime, context: ViewContext, info: any, item: any,
                                groupLabel: string, active: string, idPrefix: string): any {
            const h = runtime.html;
            const id = text(item.id || item['menu-item-id']);
            const label = text(item.label || item['menu-item-label'] || id);
            const href = item.href || item['menu-item-href'] || urlFor(context, id);
            // The Query item says when a query runs on, or has finished, out of sight; its name stays "Query".
            const activity = id === 'query' && active !== 'query' ? queryActivity(context) : '';
            const activityId = idPrefix + '-query-activity';
            return h`<li class=${active === id ? 'current' : ''}>
                ${isDisabled(item, info)
                    ? h`<span class="disabled" title=${groupLabel}>${icon(runtime, item.icon || item['menu-item-icon'] || id)}${label}</span>`
                    : h`<a href=${href} data-workbench-nav-href=${href}
                        title=${groupLabel} aria-current=${active === id ? 'page' : 'false'}
                        aria-describedby=${activity ? activityId : runtime.nothing}>
                        ${icon(runtime, item.icon || item['menu-item-icon'] || id)}${label}${activity
                            ? h`<span class="workbench-nav-activity" data-activity=${activity} aria-hidden="true"></span>`
                            : ''}
                    </a>${activity ? h`<span id=${activityId} class="workbench-visually-hidden">${
                        activity === 'running' ? 'Query running' : 'Results ready'}</span>` : ''}`}
            </li>`;
        }

        /**
         * Every group renders the same way: a non-interactive label followed by its items (M2.4). The sidebar
         * uses the default id prefix; the mobile menu sheet renders the same menu with its own prefix.
         */
        function navigation(context: ViewContext, active: string, runtime: LitRuntime,
                            idPrefix: string = 'workbench-nav', groups?: any[]): any {
            const h = runtime.html;
            const info = normalizeWorkbench(context.workbench, context.linked && context.linked.info);
            return (groups || menuEntries(context)).filter((group: any) => (group.items || []).length > 0).map((group: any) => {
                const items = group.items || [];
                const groupId = text(group.id || group['menu-group-id'] || 'workbench');
                const groupLabel = text(group.label || group['menu-group-label'] || 'Workbench');
                const list = h`<ul id=${idPrefix + '-items-' + groupId} class="group" aria-labelledby=${idPrefix + '-label-' + groupId}>
                    ${items.map((item: any) => navigationItem(runtime, context, info, item, groupLabel, active, idPrefix))}
                </ul>`;
                if (items.length > navigationDisclosureThreshold) {
                    const containsActive = items.some((item: any) => text(item.id || item['menu-item-id']) === active);
                    return h`<li class="workbench-nav-group workbench-nav-group--long" data-workbench-menu-group=${groupId}
                            data-workbench-menu-label=${groupLabel}>
                        <details class="workbench-nav-group__disclosure" ?open=${containsActive}>
                            <summary id=${idPrefix + '-summary-' + groupId} aria-controls=${idPrefix + '-items-' + groupId}
                                    class="workbench-nav-group__summary">
                                <span id=${idPrefix + '-label-' + groupId} class="workbench-nav-group__label">${groupLabel}</span>
                                ${icon(runtime, 'chevron', 'workbench-nav-group__chevron workbench-disclosure-chevron')}
                            </summary>
                            ${list}
                        </details>
                    </li>`;
                }
                return h`<li class="workbench-nav-group" data-workbench-menu-group=${groupId}
                        data-workbench-menu-label=${groupLabel}>
                    <span id=${idPrefix + '-label-' + groupId} class="workbench-nav-group__label">${groupLabel}</span>
                    ${list}
                </li>`;
            });
        }

        /** Decodes the user name from the server-user-password cookie ("user:password" in base64). */
        function serverUser(): string {
            if (typeof document === 'undefined') {
                return '';
            }
            // Base64, where '+' is a digit: the raw value (form decoding would make it a space).
            const encoded = currentCookieValue('server-user-password', true);
            if (!encoded) {
                return '';
            }
            const decoded = decodeCredentials(encoded);
            if (!decoded) {
                return '';
            }
            const user = decoded.indexOf(':') >= 0 ? decoded.substring(0, decoded.indexOf(':')) : decoded;
            return user === '""' ? '' : user;
        }

        /**
         * The text of the base64 server-user-password credentials: UTF-8 "user:password" (C15), or, for credentials
         * written one byte per character (an older Connection page), the bytes as they are. '' when it is no base64.
         */
        function decodeCredentials(encoded: string): string {
            const view: any = typeof window !== 'undefined' ? window : null;
            if (!view || typeof view.atob !== 'function') {
                return encoded;
            }
            let binary: string;
            try {
                binary = view.atob(encoded);
            } catch (error) {
                return '';
            }
            try {
                const bytes = new Uint8Array(binary.length);
                for (let index = 0; index < binary.length; index++) {
                    bytes[index] = binary.charCodeAt(index);
                }
                return new TextDecoder('utf-8', { fatal: true }).decode(bytes);
            } catch (error) {
                return binary;
            }
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

        /**
         * The context bar: brand and the repository switcher. On a desktop it tops the sidebar; below 900px it is the
         * compact row that opens the menu sheet. The repository panel's footer names the server and the user, with the
         * link to change them: users rarely change the server, so it has no switcher of its own.
         */
        function contextBar(context: ViewContext, active: string, runtime: LitRuntime): any {
            const h = runtime.html;
            const state = contextBarState(context);
            return h`<header id="workbench-contextbar" class="workbench-contextbar">
                <a id="logo" class="workbench-brand" href=${urlFor(context, 'repositories')} aria-label="RDF4J Workbench home">
                    <img class="workbench-brand__light" src=${context.basePath + '/images/logo.png'} alt="rdf4j" />
                    <img class="product workbench-brand__light" src=${context.basePath + '/images/product.png'} alt="workbench" />
                    <img class="workbench-brand__dark" src=${context.basePath + '/images/logo-dark.png'} alt="rdf4j" />
                    <img class="product workbench-brand__dark" src=${context.basePath + '/images/product-dark.png'} alt="workbench" />
                </a>
                <div class="workbench-switcher" data-workbench-switcher="repository">
                    <button id="workbench-repository-switcher" class="workbench-switcher__button" type="button"
                            aria-haspopup="dialog" aria-expanded="false" aria-controls="workbench-repository-popover"
                            title=${state.repositoryId ? (state.repositoryTitle
                                ? state.repositoryId + ' — ' + state.repositoryTitle : state.repositoryId) : 'No repository'}>
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
                            data-workbench-switch-fallback=${repositoryLanding(context)}
                            data-workbench-base-path=${(context.basePath || '').replace(/\/+$/, '')}>
                        <div class="workbench-popover__search">${icon(runtime, 'search')}
                            <input id="workbench-repository-filter" type="search" placeholder="Find repository"
                                aria-label="Find repository" autocomplete="off" aria-controls="workbench-repository-options" />
                        </div>
                        <ul id="workbench-repository-options" class="workbench-popover__list" aria-label="Repositories"></ul>
                        <p class="workbench-popover__status" role="status" aria-live="polite"></p>
                        <div class="workbench-popover__footer">
                            ${pageEnabled(context, 'repositories') ? h`<a href=${urlFor(context, 'repositories')}>${
                                icon(runtime, 'repository')}All repositories</a>` : ''}
                            ${pageEnabled(context, 'create') ? h`<a href=${urlFor(context, 'create')}>${
                                icon(runtime, 'create')}Create repository</a>` : ''}
                        </div>
                    </div>
                </div>
                <span class="workbench-contextbar__spacer"></span>
                <button id="workbench-menu-button" class="workbench-action workbench-action--ghost workbench-action--icon workbench-menu-button"
                        type="button" aria-haspopup="dialog" aria-controls="workbench-menu-sheet"
                        aria-label="Menu" title="Menu">${icon(runtime, 'menu')}</button>
            </header>`;
        }

        /** Full-height menu sheet for narrow screens (M2.8, mockup 14); a native modal dialog. */
        function menuSheet(context: ViewContext, active: string, runtime: LitRuntime, groups: any[]): any {
            const h = runtime.html;
            const state = contextBarState(context);
            return h`<dialog id="workbench-menu-sheet" class="workbench-menu-sheet" aria-label="Menu">
                <div class="workbench-menu-sheet__header">
                    <span class="workbench-menu-sheet__brand workbench-brand">
                        <img class="workbench-brand__light" src=${context.basePath + '/images/logo.png'} alt="rdf4j" />
                        <img class="workbench-brand__dark" src=${context.basePath + '/images/logo-dark.png'} alt="rdf4j" />
                    </span>
                    <span class="workbench-menu-sheet__repository">${state.repositoryId || 'No repository'}</span>
                    <button id="workbench-menu-close" class="workbench-action workbench-action--secondary workbench-action--icon"
                            type="button" aria-label="Close menu" title="Close menu">${icon(runtime, 'close')}</button>
                </div>
                <p class="workbench-menu-sheet__context">Server ${state.server ? hostAndPort(state.server) : 'None'} · ${state.user || 'Not signed in'}</p>
                <nav id="workbench-menu-sheet-nav" class="workbench-nav workbench-menu-sheet__nav" aria-label="Workbench menu">
                    <ul class="maingroup">${navigation(context, active, runtime, 'workbench-sheet-nav', groups)}</ul>
                </nav>
            </dialog>`;
        }

        /** The state that the persistent shell (header, menu, footer) depends on. */
        export interface ShellState {
            viewId: string;
            context: ViewContext;
            /** Overrides the document title (for example on the repository-not-found page). */
            title?: string;
        }

        /**
         * The menu the sidebar and the phone menu show. Without a menu from the Info answer (a user the server refuses,
         * a failed Info) they still offer Connection, where the user signs in or picks another server.
         */
        function shellMenuGroups(context: ViewContext): any[] {
            const groups = menuEntries(context);
            if (groups.some((group: any) => (group.items || []).length > 0)) {
                return groups;
            }
            return [{ id: 'repositories', label: 'Server', icon: 'repository',
                items: [{ id: 'server', label: shortTitles.server, icon: 'server', href: urlFor(context, 'server') }] }];
        }

        /** Persistent shell around the outlet; `outlet` is the outlet node or, without a DOM, its template. */
        function shellTemplate(state: ShellState, runtime: LitRuntime, outlet: any): any {
            const h = runtime.html;
            const context = state.context;
            const groups = shellMenuGroups(context);
            return h`${contextBar(context, state.viewId, runtime)}
            <nav id="workbench-navigation-disclosure" class="workbench-navigation-disclosure" aria-label="Workbench menu">
                <div id="navigation" class="workbench-nav"><ul class="maingroup">${
                    navigation(context, state.viewId, runtime, 'workbench-nav', groups)}</ul></div>
            </nav>
            ${menuSheet(context, state.viewId, runtime, groups)}
            <main id="content" class="workbench-main">${outlet}</main>
            <p id="workbench-route-status" class="workbench-visually-hidden" role="status" aria-live="polite"></p>
            <div id="workbench-kept-alive" hidden inert></div>
            <div id="footer" class="workbench-footer"><div>Copyright © Eclipse RDF4J contributors</div></div>`;
        }

        /**
         * The page area that changes from route to route: title, why the server did not take a form the router sent
         * from this page (the router sets model.sendFailure) and page surface.
         */
        function outletContentTemplate(model: PageModel, context: ViewContext, runtime: LitRuntime, body: any): any {
            const h = runtime.html;
            const failure: any = (model as any).sendFailure;
            const notFound = isRepositoryNotFound(model);
            const description = notFound ? '' : routeDescription(model, context);
            return h`<header class="workbench-page-header">
                ${notFound ? '' : h`<span class="workbench-page-header__icon" aria-hidden="true">${
                    icon(runtime, pageIcon(context, model.viewId), 'workbench-page-header__icon-glyph')}</span>`}
                <div class="workbench-page-header__text">
                    <h1 id="title_heading" tabindex="-1">${routeTitle(model)}</h1>
                    ${description ? h`<p class="workbench-page-header__description">${description}</p>` : ''}
                </div>
            </header>
                ${failure ? callout(runtime, 'error', text(failure.message), failure.status
                    ? 'The server did not accept this.' : 'The request failed.', 'workbench-send-failure') : ''}
                <div id="workbench-page-surface" class="workbench-page-surface">${body}</div>`;
        }

        function shell(model: PageModel, context: ViewContext, runtime: LitRuntime, body: any): any {
            const h = runtime.html;
            return shellTemplate({ viewId: model.viewId, context }, runtime,
                h`<div id="workbench-outlet" class="workbench-outlet" tabindex="-1">${outletContentTemplate(model, context, runtime, body)}</div>`);
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
            const columns = options && options.columns || model.vars || [];
            const allRows = records(model);
            const total = rowCount(model);
            const emptyText = options && options.emptyText ? options.emptyText : 'No results to display.';
            const regions = context.rowRegions;
            if (regions) {
                if (!regions.tableBody) { regions.tableBody = regions.document.createElement('tbody'); }
                regions.renderTableRows = () => runtime.render(tableRows(runtime, model, context, options,
                    records(model), rowStart(model), rowCount(model), emptyText), regions.tableBody);
            }
            return tableScroll(runtime, h`<table class="data" data-workbench-row-table=${model.rowStore && total ? 'true' : runtime.nothing}>
                ${columns.length ? h`<thead><tr>${columns.map((name: string) => options && options.header
                    ? options.header(name) : h`<th scope="col">${columnLabel(name, options)}</th>`)}</tr></thead>` : ''}
                ${regions ? regions.tableBody
                    : h`<tbody>${tableRows(runtime, model, context, options, allRows, rowStart(model), total, emptyText)}</tbody>`}
            </table>`);
        }

        /**
         * How a kind of page presents its tables (Display: Layout and Wrap values). The choice is kept in memory while
         * the Workbench document lives, as the Query page keeps its own, so the next Explore resource or the next
         * preview starts with it.
         */
        interface TablePresentation {
            /** 'auto', 'table', 'records' or 'nquads' (tables of statements only). */
            layout: string;
            /** Wrap long values in their columns, or keep each on one line and scroll the table sideways. */
            wrap: boolean;
        }

        const tablePresentations: { [kind: string]: TablePresentation } = Object.create(null);

        function tablePresentation(kind: string): TablePresentation {
            return tablePresentations[kind] || (tablePresentations[kind] = { layout: 'auto', wrap: true });
        }

        /** A list table's box: it scrolls the table sideways when the card does not wrap its values. */
        function tableScroll(runtime: LitRuntime, content: any): any {
            return runtime.html`<div class="workbench-table-scroll">${content}</div>`;
        }

        const tableLayouts: string[][] = [['auto', 'Auto'], ['table', 'Table'], ['records', 'Records'], ['nquads', 'N-Quads']];

        /**
         * The Display fields of a page's tables: Layout (tables of statements: Auto, Table, Records or N-Quads) and Wrap
         * values. The controls have no name, so a form around them does not send them.
         */
        function tableDisplayFields(runtime: LitRuntime, model: PageModel, ids: { layout?: string; wrap: string },
                                    onWrap: (wrap: boolean, event: any) => void): any {
            const h = runtime.html;
            const presentation = tablePresentation(model.viewId);
            return h`${ids.layout ? h`<div class="workbench-field workbench-disclosure__field"><label for=${ids.layout}>Layout</label>
                    <select id=${ids.layout} @change=${(event: any) => setPageResultPresentation(model,
                        { layout: String(event.currentTarget.value) })}>${tableLayouts.map((choice: string[]) =>
                        h`<option value=${choice[0]} ?selected=${presentation.layout === choice[0]}>${choice[1]}</option>`)}</select>
                </div>` : ''}<label class="workbench-check" for=${ids.wrap}>
                    <input id=${ids.wrap} type="checkbox" .checked=${presentation.wrap}
                        @change=${(event: any) => onWrap(!!event.currentTarget.checked, event)} />
                    <span>Wrap values</span></label>`;
        }

        /**
         * A Display disclosure (toggle and pane) for a card of tables, with tableDisplayFields; it is in the card's first
         * render, so the route's decoratePage binds it, and hidden while the card shows no table.
         */
        function tableDisplay(runtime: LitRuntime, model: PageModel, prefix: string, ids: { layout?: string; wrap: string },
                              onWrap: (wrap: boolean, event: any) => void, hidden?: boolean): any {
            return workbench.detailDisclosure.render(runtime.html, {
                id: prefix + '-options', toggleId: prefix + '-options-toggle', panelId: prefix + '-options-panel',
                label: 'Display', accessibleName: 'Table display options', hidden: !!hidden,
                ownerClass: 'workbench-table-display',
                toggleClass: 'workbench-action workbench-action--secondary'
            }, runtime.html`<div class="workbench-disclosure__fields">${tableDisplayFields(runtime, model, ids, onWrap)}</div>`);
        }

        /**
         * A row menu in a list table that scrolls sideways (Wrap values off) would be cut off by the table's box, so it
         * opens fixed under its button and follows the button while the page or the box scrolls; otherwise it keeps
         * its place in the row.
         */
        function placeRowMenu(button: any, panel: any): void {
            const card = button.closest ? button.closest('[data-table-wrap]') : null;
            const scrolling = !!card && card.getAttribute('data-table-wrap') === 'false'
                && !!button.closest('.workbench-table-scroll');
            const view = button.ownerDocument && button.ownerDocument.defaultView;
            if (!scrolling || !view) {
                panel.style.position = '';
                panel.style.top = '';
                panel.style.right = '';
                panel.style.left = '';
                return;
            }
            const place = () => {
                if (panel.hidden) {
                    view.removeEventListener('scroll', place, true);
                    view.removeEventListener('resize', place, false);
                    return;
                }
                const box = button.getBoundingClientRect();
                const width = button.ownerDocument.documentElement.clientWidth || view.innerWidth;
                panel.style.position = 'fixed';
                panel.style.top = (box.bottom + 6) + 'px';
                panel.style.right = Math.max(12, width - box.right) + 'px';
                panel.style.left = 'auto';
            };
            place();
            view.addEventListener('scroll', place, true);
            view.addEventListener('resize', place, false);
        }

        /**
         * A list card's Display (Types, Graphs, Namespaces; the Repositories list always wraps and has none): Wrap
         * values sets the card's data-table-wrap, which the CSS reads (scroll: one line per value, the table scrolls
         * sideways in .workbench-table-scroll and stays a table on phones).
         */
        function listTableDisplay(runtime: LitRuntime, model: PageModel): any {
            const kind = model.viewId;
            return tableDisplay(runtime, model, kind + '-display', { wrap: kind + '-wrap-values' },
                (wrap: boolean, event: any) => {
                    tablePresentation(kind).wrap = wrap;
                    const card = event.currentTarget.closest('[data-table-wrap]');
                    if (card) {
                        card.setAttribute('data-table-wrap', wrap ? 'true' : 'false');
                    }
                });
        }

        function listTableWrap(model: PageModel): string {
            return tablePresentation(model.viewId).wrap ? 'true' : 'false';
        }

        function tableRows(runtime: LitRuntime, model: PageModel, context: ViewContext,
                           options: any, allRows: any[], start: number, total: number, emptyText: string): any {
            const h = runtime.html;
            const columns = options && options.columns || model.vars || [];
            const rowHeightValue = rowHeight(model);
            if (!total && !allRows.length) {
                // A trailing row (Graphs' default graph) is there with or without other rows.
                return h`<tr class="workbench-empty-row"><td class="workbench-empty-table-message" role="status" colspan=${Math.max(1, columns.length)}>${emptyText}</td></tr>${
                    options && options.trailing ? options.trailing() : ''}`;
            }
            if (!allRows.length) {
                return h`<tr class="workbench-pending-row"><td class="workbench-empty-table-message" role="status" colspan=${Math.max(1, columns.length)}>Loading rows...</td></tr>`;
            }
            const before = typeof model.rowTopSpacer === 'number'
                ? model.rowTopSpacer : Math.max(0, start) * rowHeightValue;
            const after = typeof model.rowBottomSpacer === 'number'
                ? model.rowBottomSpacer : Math.max(0, total - start - allRows.length) * rowHeightValue;
            return h`${before ? h`<tr class="workbench-virtual-spacer" aria-hidden="true"><td colspan=${Math.max(1, columns.length)}
                    style=${'height:' + before + 'px;padding:0;border:0'}></td></tr>` : ''}
                ${allRows.map((record: any, relativeIndex: number) => h`<tr data-workbench-row-index=${start + relativeIndex}>
                    ${options && options.cells ? options.cells(record, start + relativeIndex) : columns.map((name: string) => {
                        const cell = record[name];
                        if (options && options.status && (name === 'readable' || name === 'writeable')) {
                            const status = name === 'readable' ? 'readable' : 'writeable';
                            return h`<td data-label=${columnLabel(name, options)}>${statusIcon(runtime,
                                (cell === true || text(cell) === 'true') ? status : 'negative',
                                (cell === true || text(cell) === 'true') ? (name === 'readable' ? 'Readable' : 'Writeable') : 'No')}</td>`;
                        }
                        if (options && options.repository && name === 'id') {
                            const id = text(cell);
                            const href = '../' + encodeURIComponent(id) + '/summary';
                            return h`<td data-label=${columnLabel(name, options)}><a href=${href}>${id}</a></td>`;
                        }
                        return h`<td data-label=${columnLabel(name, options)}>${renderTerm(runtime, cell, context, !!(options && options.linkTerms))}</td>`;
                    })}
                </tr>`)}
                ${after ? h`<tr class="workbench-virtual-spacer" aria-hidden="true"><td colspan=${Math.max(1, columns.length)}
                    style=${'height:' + after + 'px;padding:0;border:0'}></td></tr>` : ''}
                ${options && options.trailing ? options.trailing() : ''}`;
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

        /**
         * What a page shows in one table of RDF statements. Its rows are either held by the page (rows) or read from
         * the page model's row store as the table scrolls (store and count).
         */
        interface ResultTableSpec {
            /** Names the table on its page, for example 'explore-outgoing'. */
            key: string;
            /** The table's accessible name. */
            label: string;
            /** The page model's variables the table shows, in order. */
            columns: string[];
            rows?: any[][];
            store?: { read(start: number, count: number): Promise<any[][]> };
            count?: number;
            /** The page row of a table row (data-workbench-row-index); the table row itself when absent. */
            rowIndex?: (index: number) => number;
            /**
             * A table row's whole statement [subject, predicate, object, graph] for the N-Quads layout, when the table
             * shows only some of its terms (Explore's role groups); else its subject, predicate, object and context.
             */
            statement?: (index: number) => any[];
            namespaces: { prefix: string; name: string }[];
            /** Changes whenever the rows change; a table with another signature is built again. */
            signature: string;
        }

        /** The terms of a statement in the N-Quads layout, by the names of the page's columns. */
        const statementColumns = ['subject', 'predicate', 'object', 'context'];

        /** Statement columns are named as people say them; the graph column is the Graph. */
        const statementColumnLabels: { [name: string]: string } = {
            subject: 'Subject', predicate: 'Predicate', object: 'Object', context: 'Graph'
        };

        function statementColumnLabel(name: string): string {
            return statementColumnLabels[name] || columnLabel(name);
        }

        /** Every result table built for a page model, on any mount; releasePage disposes them. */
        const pageResultTables = new WeakMap<PageModel, PageResultTable[]>();

        /**
         * A table of RDF statements on a page: the Query page's result table (workbench.queryStream.ResultTable), so
         * its cells, column widths, wrapping, datatype tags, sideways scrolling and row windows are the same. The
         * template gets the table's host element, kept per mount of the page and key so the page can render again
         * around it; renderPageResultTables lays it out once the host is in the document. Without a DOM document
         * (unit-test fakes) the host is an empty placeholder.
         */
        function resultTable(runtime: LitRuntime, model: PageModel, context: ViewContext, spec: ResultTableSpec): any {
            const h = runtime.html;
            const regions = context.rowRegions;
            const stream: any = (workbench as any).queryStream;
            if (!regions || !regions.document || !stream || typeof stream.ResultTable !== 'function') {
                return h`<div class="workbench-result-table" data-workbench-result-table=${spec.key}
                    aria-label=${spec.label}></div>`;
            }
            // Tables belong to one mount of the page (its row regions), so two mounts never share their elements.
            const tables = regions.tableEntries || (regions.tableEntries = {});
            let entry = tables[spec.key];
            if (entry && entry.signature !== spec.signature) {
                disposePageResultTable(model, entry);
                entry = null;
            }
            if (!entry) {
                const host: any = regions.document.createElement('div');
                host.className = 'query-result-layout query-result-embedded workbench-result-table';
                host.setAttribute('data-workbench-result-table', spec.key);
                const rows = spec.rows;
                const source: any = {
                    rowCount: () => rows ? rows.length : spec.count || 0,
                    variables: () => spec.columns,
                    read: (start: number, count: number) => rows
                        ? Promise.resolve(rows.slice(start, start + count)) : spec.store.read(start, count),
                    namespaces: () => spec.namespaces,
                    statements: () => true
                };
                if (spec.statement) {
                    source.statement = (_values: any[], index: number) => spec.statement(index);
                }
                // The page's Display choices so far, so the next Explore resource or preview starts with them.
                const presentation = tablePresentation(model.viewId);
                const table = new stream.ResultTable(host, source, {
                    layout: presentation.layout,
                    wrap: presentation.wrap,
                    label: spec.label,
                    renderAllRows: true,
                    columnLabel: statementColumnLabel,
                    cellLabel: statementColumnLabel,
                    unboundLabel: (name: string) => name === 'context' ? 'Default graph' : '',
                    rowAttributes: (index: number) => ({
                        'data-workbench-row-index': String(spec.rowIndex ? spec.rowIndex(index) : index)
                    })
                });
                entry = { host, table, signature: spec.signature };
                tables[spec.key] = entry;
                const built = pageResultTables.get(model) || [];
                built.push(entry);
                pageResultTables.set(model, built);
            }
            if (!regions.resultTables) {
                regions.resultTables = {};
            }
            regions.resultTables[spec.key] = entry;
            return entry.host;
        }

        function disposePageResultTable(model: PageModel, entry: PageResultTable): void {
            entry.table.dispose();
            const built = pageResultTables.get(model) || [];
            const index = built.indexOf(entry);
            if (index >= 0) {
                built.splice(index, 1);
            }
        }

        /** Lay out the result tables a render of the page used, and dispose the ones it no longer uses. */
        function renderPageResultTables(model: PageModel, regions: RowRegions): void {
            const tables = regions && regions.tableEntries;
            if (!tables) {
                return;
            }
            const used = regions.resultTables || {};
            Object.keys(tables).forEach((key) => {
                const entry = tables[key];
                if (used[key] !== entry) {
                    disposePageResultTable(model, entry);
                    delete tables[key];
                    return;
                }
                if (entry.host.isConnected !== false) {
                    entry.table.render().then(null, (error: any) => {
                        if (typeof console !== 'undefined') {
                            console.error('Unable to lay out a result table.', error);
                        }
                    });
                }
            });
        }

        /** Show or hide the datatype tags in every result table of a page (Explore's Display pane). */
        function showPageResultDatatypes(model: PageModel, show: boolean): void {
            (pageResultTables.get(model) || []).forEach((entry: PageResultTable) => {
                entry.table.setShowDatatypes(show);
                if (entry.host.isConnected !== false) {
                    entry.table.render().then(null, () => {});
                }
            });
        }

        /** Change the Layout or wrapping of every result table of a page (its Display pane) and of the ones it builds next. */
        function setPageResultPresentation(model: PageModel, change: { layout?: string; wrap?: boolean }): void {
            const presentation = tablePresentation(model.viewId);
            if (typeof change.layout === 'string') {
                presentation.layout = change.layout;
            }
            if (typeof change.wrap === 'boolean') {
                presentation.wrap = change.wrap;
            }
            (pageResultTables.get(model) || []).forEach((entry: PageResultTable) => {
                if (typeof change.layout === 'string') {
                    entry.table.setLayout(presentation.layout);
                }
                if (typeof change.wrap === 'boolean') {
                    entry.table.setWrap(presentation.wrap);
                }
                if (entry.host.isConnected !== false) {
                    entry.table.render().then(null, () => {});
                }
            });
        }

        /** Dispose a page's result tables when its route is left. */
        function releasePageResultTables(model: PageModel): void {
            (pageResultTables.get(model) || []).forEach((entry: PageResultTable) => entry.table.dispose());
            pageResultTables.delete(model);
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
            const message = pageErrorMessage(model);
            return message ? callout(runtime, 'error', message) : '';
        }

        /**
         * The error-message of a page answered with a refusal, until its form is sent again in place: the next write's
         * own status replaces it (C17).
         */
        function pageErrorMessage(model: PageModel): string {
            return (model as any).errorSuperseded ? '' : text(pageValue(model, 'error-message'));
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

        /** Summary (M5.4, mockup 12): one card with Repository and Size sections and the configuration disclosure. */
        function summaryPage(runtime: LitRuntime, model: PageModel, context: ViewContext): any {
            const h = runtime.html;
            const row = firstRecord(model);
            const config = text(meta(model, 'config-model-turtle', 'config-model'));
            const code = (value: any) => {
                const content = text(value);
                return content ? h`<code>${content}</code>` : '';
            };
            // The counts arrive after the page (M13.3); a page model that already has them shows them at once.
            const counts = pageCounts(model);
            const count = (name: string) => {
                const value = name in counts.values ? counts.values[name] : field(row, name);
                if (text(value)) { return formatCount(value, context); }
                return counts.state === 'counting' ? 'Counting…'
                    : h`<span title=${counts.state === 'timed-out' ? countedPages.summary.timedOut
                        : counts.reason ? 'Counts are not available: ' + counts.reason : 'No count is available'}>—</span>`;
            };
            return h`<section id="workbench-summary" class="workbench-island workbench-summary">
                <h2>Repository</h2>
                ${keyValueList(runtime, [
                    ['ID', code(field(row, 'id'))],
                    ['Title', field(row, 'description')],
                    ['Location', code(field(row, 'location'))],
                    ['Server', code(field(row, 'server'))]
                ])}
                <h2>Size</h2>
                ${keyValueList(runtime, [
                    ['Statements', count('size')],
                    ['Named graphs', count('contexts')]
                ])}
                ${counts.state === 'failed' ? h`<p class="workbench-page-meta workbench-summary__counts-note" role="note">Counts are not available: ${
                    counts.reason || 'the server could not count this repository.'}</p>` : ''}
                ${config ? h`<details id="summary-config-model" class="workbench-options workbench-summary-config">
                    <summary>Configuration (Turtle)${icon(runtime, 'chevron', 'workbench-disclosure-chevron')}</summary>
                    <pre role="region">${config}</pre>
                </details>` : ''}
            </section>`;
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

        function repositoryUrl(context: ViewContext, id: string, route: string): string {
            return (context.basePath || '').replace(/\/+$/, '') + '/repositories/' + encodeURIComponent(id) + '/' + route;
        }

        function repositoryAccess(record: any): string[] {
            return [
                record.readable === true || text(record.readable) === 'true' ? 'Read' : '',
                record.writeable === true || text(record.writeable) === 'true' ? 'Write' : ''
            ].filter((label) => !!label);
        }

        function sortedRepositoryRows(model: PageModel, rows: any[][], column: string, direction: string): any[][] {
            const options: any = { sensitivity: 'base' };
            if (column === 'id') { options.numeric = true; }
            return rows.map((row: any[], index: number) => {
                const record = recordsFromRows(model, [row])[0];
                const key = column === 'title' ? text(record.description)
                    : column === 'access' ? repositoryAccess(record).join(' ')
                        : text(record.id);
                return { row, index, key };
            }).sort((left: any, right: any) => {
                const compared = (left.key as any).localeCompare(right.key, undefined, options);
                return compared ? (direction === 'descending' ? -compared : compared) : left.index - right.index;
            }).map((item: any) => item.row);
        }

        /** One repository row: repository icon, ID link, Title, Access badges and an actions menu. */
        function repositoryCells(runtime: LitRuntime, context: ViewContext, record: any, index: number): any {
            const h = runtime.html;
            const id = text(record.id);
            const access = repositoryAccess(record);
            const menuId = 'repository-actions-' + index;
            // Pages the policy hides are not offered (the server would refuse them).
            const items = [['query', 'query', 'Query'], ['explore', 'explore', 'Explore'], ['summary', 'summary', 'Summary']]
                .filter((entry: string[]) => pageEnabled(context, entry[0]))
                .map((entry: string[]) => h`<li><a href="${repositoryUrl(context, id, entry[0])}">${
                    icon(runtime, entry[1])}${entry[2]}</a></li>`);
            const deletable = pageEnabled(context, 'delete');
            return h`<td data-label="Repository">${icon(runtime, 'repository')}</td>
                <td data-label="ID"><a class="workbench-repository-link" href="${repositoryUrl(context, id, repositoryLanding(context))}">${id}</a></td>
                <td data-label="Title" title="${text(record.location)}">${text(record.description)}</td>
                <td data-label="Access"><span class="workbench-badges">${access.length
                    ? access.map((label) => h`<span class="workbench-badge">${icon(runtime, label === 'Read' ? 'eye' : 'edit')}${label}</span>`)
                    : h`<span class="workbench-access-none">None</span>`}</span></td>
                <td class="workbench-row-actions" data-label="Actions">${items.length || deletable ? h`<div class="workbench-row-menu">
                    <button type="button" class="workbench-action workbench-action--ghost workbench-action--icon"
                        aria-label="${'Actions for ' + id}" title="Actions" aria-expanded="false" aria-controls="${menuId}"
                        data-workbench-row-menu="true">${icon(runtime, 'more')}</button>
                    <div id="${menuId}" class="workbench-popover workbench-popover--end workbench-row-menu__panel" hidden>
                        <ul class="workbench-popover__menu">
                            ${items}
                            ${deletable ? h`<li><a class="workbench-popover__danger" href="${urlFor(context, 'delete') + '?id=' + encodeURIComponent(id)}">${
                                icon(runtime, 'delete')}Delete…</a></li>` : ''}
                        </ul>
                    </div>
                </div>` : ''}</td>`;
        }

        function repositoriesPage(runtime: LitRuntime, model: PageModel, context: ViewContext): any {
            const h = runtime.html;
            // The list is in ID order until another column is chosen (C32a); the server lists in no particular order.
            const sorting = (model as any).repositorySort || { column: 'id', direction: 'ascending' };
            const labels: any = { id: 'ID', title: 'Title', access: 'Access' };
            const header = (name: string) => {
                if (name === 'repository' || name === 'actions') {
                    return h`<th scope="col" data-repository-column=${name}>${name === 'repository'
                        ? h`<span class="workbench-visually-hidden">Repository</span>` : 'Actions'}</th>`;
                }
                const label = labels[name];
                const selected = sorting && sorting.column === name;
                const direction = selected ? sorting.direction : 'none';
                const nextDirection = direction === 'ascending' ? 'descending' : 'ascending';
                const indicator = direction === 'ascending' ? '↑' : direction === 'descending' ? '↓' : '↕';
                return h`<th scope="col" data-repository-column=${name} aria-sort=${direction}>
                    <button type="button" class="workbench-repository-sort" data-workbench-sort=${name}
                        aria-label=${'Sort by ' + label + ', ' + nextDirection}>
                        <span>${label}</span><span class="workbench-repository-sort__indicator" aria-hidden="true">${indicator}</span>
                    </button>
                </th>`;
            };
            // The Repositories list has no Display control: it always wraps its values (data-table-wrap="true").
            return h`<section id="repositories-results" class="workbench-island workbench-responsive-records workbench-browse-card"
                    data-table-wrap="true">
                <div class="workbench-browse-card__header">
                    <h2>Repositories</h2><span class="workbench-browse-card__count">${formatCount(String(rowCount(model)), context)}</span>
                    ${pageEnabled(context, 'create') ? h`<a class="workbench-action workbench-action--primary workbench-browse-card__action workbench-repository-create"
                        href="${urlFor(context, 'create')}">${icon(runtime, 'create')}<span>Create repository</span></a>` : ''}
                </div>
                ${rowCount(model) ? table(runtime, model, context,
                    { columns: ['repository', 'id', 'title', 'access', 'actions'], header,
                    cells: (record: any, index: number) => repositoryCells(runtime, context, record, index) })
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
                    <tr><th><label for="id">Repository ID</label></th><td><input id="id" name="Local repository ID" type="text" size="16" value="fed" data-field-role="repository-id" /></td><td><span id="recurse-message" class="error" hidden>Federation ID may not match an existing ID.</span></td></tr>
                    <tr><th><label for="title">Repository title</label></th><td><input id="title" name="Repository title" type="text" size="48" value="Federation" data-field-role="repository-title" /></td></tr>
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

        /**
         * A write form whose check lives in a page script (delete.js, server.js): the page can be shown before that
         * script has loaded, and the browser's own submit would then skip the check. The form never submits natively:
         * the check runs at once, or once its script has loaded while the page is still shown, or not at all.
         */
        function submitThroughPageScript(event: any, script: string, helper: string, owns: () => boolean,
                                         run: (check: any) => void): void {
            event.preventDefault();
            const globalWindow: any = typeof window !== 'undefined' ? window : null;
            const available = () => !!globalWindow && typeof globalWindow[helper] === 'function';
            if (available()) {
                run(globalWindow[helper]);
                return;
            }
            const app: any = (workbench as any).app;
            if (!app || typeof app.loadScripts !== 'function') {
                return;
            }
            app.loadScripts([script]).then(() => {
                if (available() && owns()) { run(globalWindow[helper]); }
            }, (error: any) => {
                if (typeof console !== 'undefined' && console.error) { console.error(error); }
            });
        }

        function createActions(runtime: LitRuntime, confirmOverwrite: boolean, context: ViewContext): any {
            const h = runtime.html;
            const cancel = urlFor(context, 'repositories');
            const createType = confirmOverwrite ? 'button' : 'submit';
            // Without create.js (still loading) nothing checks whether the id replaces a repository: nothing is sent.
            const createHandler = confirmOverwrite ? (event: Event) => {
                event.preventDefault();
                const globalWindow: any = typeof window !== 'undefined' ? window : null;
                if (globalWindow && typeof globalWindow.checkOverwrite === 'function') {
                    globalWindow.checkOverwrite();
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

        /**
         * Delete repository (M6.6, mockup 09): nothing is chosen until the user picks a repository or arrives with
         * `?id=<id>`, the button stays disabled until then, and delete.ts confirms with the typed id.
         */
        function deletePage(runtime: LitRuntime, model: PageModel, context: ViewContext): any {
            const h = runtime.html;
            const options = recordsFromRows(model, model.pickerRows || []).filter((row: any) => text(row.id) !== 'SYSTEM');
            if (typeof model.metadata.selectedRepositoryId !== 'string') {
                // ?id= (a repository list's Delete… item), or the repository whose menu opened the page (C32d).
                model.metadata.selectedRepositoryId = locationParameter('id')
                    || (context.repositoryId && context.repositoryId !== 'NONE' ? context.repositoryId : '');
            }
            const selectedId = text(model.metadata.selectedRepositoryId);
            const selectedRepository = model.metadata.selectedRepository;
            const selectedVisible = options.some((row: any) => text(row.id) === selectedId);
            return h`<form id="delete-form" class="workbench-island workbench-form-card" action="delete" method="post"
                    @submit=${(event: Event) => {
                        const owns = modelOwner(event.currentTarget, model);
                        submitThroughPageScript(event, 'delete.js', 'checkIsSafeToDelete', owns,
                            (check: any) => check(event, owns));
                    }}>
                <div class="workbench-field"><label for="id">Repository</label>
                    <select id="id" name="id" data-workbench-window-picker="repositories" @change=${(event: any) => {
                        const selected = options.filter((row: any) => text(row.id) === event.target.value)[0];
                        model.metadata.selectedRepositoryId = event.target.value;
                        model.metadata.selectedRepository = selected || null;
                        const outlet = event.target.closest('.workbench-outlet');
                        if (outlet) { render(outlet, model, context, runtime); }
                    }}><option value="" disabled ?selected=${!selectedId}>Choose a repository</option>
                        ${selectedId && !selectedVisible
                            ? h`<option value=${selectedId} selected>${selectedRepository
                                ? text(selectedRepository.id) + ' — ' + text(selectedRepository.description) : selectedId}</option>` : ''}
                        ${options.map((row: any) => h`<option value=${text(row.id)} ?selected=${text(row.id) === selectedId}>
                            ${text(row.id)} — ${text(row.description)}</option>`)}
                    </select>
                    ${pickerWindow(runtime, model, 'repositories', 'repositories')}
                </div>
                <div id="delete-actions" class="workbench-form-actions"><button type="submit"
                        class="workbench-action workbench-action--danger-outline" ?disabled=${!selectedId}>${
                        icon(runtime, 'delete')}<span>Delete repository…</span></button>
                </div><span id="delete-feedback" class="error" role="alert"></span>
            </form>`;
        }

        interface NamespaceEditor {
            /** The prefix of the row being edited, or null. */
            editing: string;
            adding: boolean;
            filter: string;
            /** Why the values in the editor were not sent, or ''. */
            error?: string;
            /** The values to show in the editor instead of its row's (they were sent and refused). */
            draft?: { prefix: string; namespace: string };
            /** The page's error message is shown in the editor's row, not above the list. */
            errorInRow?: boolean;
            /** The namespace the server says the added prefix is bound to: ask before replacing it (C8). */
            replaceAsk?: string;
        }

        /**
         * The change the Namespaces page sent last. The server answers a refused one with the listing and an error
         * message: the page shown then puts the editor back, with what was typed and why (round-3 finding C29).
         */
        let sentNamespaceChange: { repository: string; editing: string; adding: boolean;
            prefix: string; namespace: string } = null;

        /** False when the repository is known to be read-only (its Info model says writeable=false). */
        function repositoryWriteable(context: ViewContext): boolean {
            const info = workbenchData(context);
            return info.writeable !== false && String(info.writeable) !== 'false';
        }

        function namespaceEditor(model: PageModel, context: ViewContext): NamespaceEditor {
            const holder: any = model;
            if (!holder.namespaceEditor) {
                holder.namespaceEditor = { editing: null, adding: false, filter: '' };
                const sent = sentNamespaceChange;
                const refusal = text(pageValue(model, 'error-message'));
                if (sent) {
                    sentNamespaceChange = null;
                    if (refusal && sent.repository === text(context.repositoryId)) {
                        // An add refused because the prefix is bound (another page bound it since this list was shown)
                        // names that binding: ask about it, as for a prefix the list shows (C8).
                        const existing = text(pageValue(model, 'existing-namespace'));
                        holder.namespaceEditor = { editing: sent.editing, adding: sent.adding, filter: '', error: refusal,
                            draft: { prefix: sent.prefix, namespace: sent.namespace }, errorInRow: true,
                            replaceAsk: sent.adding && existing ? existing : undefined };
                    }
                }
            }
            return holder.namespaceEditor;
        }

        /** Posts a namespace change; the server answers with a redirect to the listing or the listing and an error. */
        function postNamespaces(document: any, fields: { [name: string]: string }): void {
            const form = document.createElement('form');
            form.method = 'post';
            form.action = 'namespaces';
            form.hidden = true;
            Object.keys(fields).forEach((name) => {
                const input = document.createElement('input');
                input.type = 'hidden';
                input.name = name;
                input.value = fields[name];
                form.appendChild(input);
            });
            document.body.appendChild(form);
            (workbench as any).submitForm(form);
            // Sent through the router the page stays, so the helper form must not stay with it.
            if (form.parentNode) {
                form.parentNode.removeChild(form);
            }
        }

        /**
         * An IRI with a line-break opportunity (<wbr>) after each / # : ? & = -, so a narrow column wraps it at those
         * boundaries instead of in the middle of a word; WebKit does not break after a hyphen on its own. Copied text
         * is unchanged.
         */
        function breakableIri(runtime: LitRuntime, iri: string): any {
            const parts = iri.match(/[^/#:?&=-]*(?:[/#:?&=-]+|$)/g) || [iri];
            return parts.filter((part: string) => part.length > 0)
                .map((part: string, position: number) => position ? runtime.html`<wbr>${part}` : part);
        }

        /** Namespaces (M6.3, mockup 07): rows in prefix order, edited in place; no field is prefilled from a row. */
        function namespacesPage(runtime: LitRuntime, model: PageModel, context: ViewContext): any {
            const h = runtime.html;
            const state = namespaceEditor(model, context);
            const rows = records(model).map((record: any) => ({ prefix: text(record.prefix), namespace: text(record.namespace) }));
            const needle = state.filter.trim().toLowerCase();
            const visible = rows.filter((row: any) => !needle || row.prefix.toLowerCase().indexOf(needle) >= 0
                || row.namespace.toLowerCase().indexOf(needle) >= 0);
            const rerender = (event: any, focusEditor?: boolean) => {
                const outlet = event.currentTarget.closest('.workbench-outlet');
                if (outlet) {
                    render(outlet, model, context, runtime);
                    const first = focusEditor ? outlet.querySelector('.workbench-namespace-edit input') : null;
                    if (first) { first.focus(); }
                }
            };
            const label = (prefix: string) => prefix || '(default)';
            // A read-only repository offers no changes (the menu leaves its write pages out the same way).
            const writeable = repositoryWriteable(context);
            const send = (document: any, fields: any) => {
                state.error = '';
                sentNamespaceChange = { repository: text(context.repositoryId), editing: state.editing,
                    adding: state.adding, prefix: fields.prefix, namespace: fields.namespace };
                postNamespaces(document, fields);
            };
            /** Ask whether to replace the namespace a prefix is bound to; Replace sends the change with overwrite=true. */
            const askReplace = (document: any, fields: any, bound: string, owns: () => boolean) => {
                (workbench as any).confirmDialog.open({ title: 'Replace prefix ' + label(fields.prefix) + '?',
                    body: 'Prefix \'' + fields.prefix + '\' is already defined as <' + bound + '>. Replace it?',
                    confirmLabel: 'Replace', danger: true }).then((confirmed: boolean) => {
                    if (confirmed && owns()) {
                        send(document, Object.assign({}, fields, { overwrite: 'true' }));
                    }
                });
            };
            if (state.replaceAsk) {
                const bound = state.replaceAsk;
                state.replaceAsk = undefined;
                const draft = state.draft;
                const page: any = typeof window !== 'undefined' ? (window as any).document : null;
                const outlet = page ? page.getElementById('workbench-outlet') : null;
                const fields = { action: 'save', prefix: draft.prefix, namespace: draft.namespace };
                // After this render: the dialog belongs to the page shown, which must still be this one.
                setTimeout(() => askReplace(page, fields, bound, () => !!outlet && shownModels.get(outlet) === model), 0);
            }
            const save = (event: any, row: any) => {
                const inputs = event.currentTarget.closest('tr').querySelectorAll('input');
                const document = event.currentTarget.ownerDocument;
                const fields: any = { action: 'save', prefix: inputs[0].value.trim(), namespace: inputs[1].value.trim() };
                if (row) { fields.previousPrefix = row.prefix; }
                const bound = rows.filter((other: any) => other.prefix === fields.prefix)[0];
                // Renaming onto a prefix that is bound already would replace that binding: the server refuses it too.
                if (row && fields.prefix !== row.prefix && bound) {
                    state.error = 'Prefix \'' + fields.prefix + '\' is already defined';
                    rerender(event);
                    return;
                }
                // Adding a prefix that is bound to another namespace replaces that binding: only when confirmed (C8).
                if (!row && bound && bound.namespace !== fields.namespace) {
                    askReplace(document, fields, bound.namespace, modelOwner(event.currentTarget, model));
                    return;
                }
                send(document, fields);
            };
            const cancel = (event: any) => {
                state.editing = null;
                state.adding = false;
                state.error = '';
                state.draft = null;
                state.errorInRow = false;
                rerender(event);
            };
            const editKeys = (event: any, row: any) => {
                if (event.key === 'Enter') {
                    event.preventDefault();
                    save(event, row);
                } else if (event.key === 'Escape') {
                    event.preventDefault();
                    cancel(event);
                }
            };
            const editRow = (row: any) => h`<tr class="workbench-namespace-edit">
                <td data-label="Prefix"><input type="text" aria-label="Prefix"
                    value=${state.draft ? state.draft.prefix : row ? row.prefix : ''}
                    autocomplete="off" spellcheck="false" @keydown=${(event: any) => editKeys(event, row)} /></td>
                <td data-label="Namespace"><input type="text" aria-label="Namespace"
                    value=${state.draft ? state.draft.namespace : row ? row.namespace : ''}
                    autocomplete="off" spellcheck="false" @keydown=${(event: any) => editKeys(event, row)} /></td>
                <td class="workbench-row-actions" data-label="Actions"><button type="button"
                        class="workbench-action workbench-action--primary workbench-action--icon" aria-label="Save" title="Save"
                        @click=${(event: any) => save(event, row)}>${icon(runtime, 'check')}</button><button type="button"
                        class="workbench-action workbench-action--secondary workbench-action--icon" aria-label="Cancel" title="Cancel"
                        @click=${cancel}>${icon(runtime, 'close')}</button></td>
            </tr>${state.error ? h`<tr class="workbench-namespace-edit-error"><td colspan="3"><p class="workbench-field__error"
                role="alert">${state.error}</p></td></tr>` : ''}`;
            const viewRow = (row: any) => h`<tr>
                <td data-label="Prefix"><code>${row.prefix}</code></td>
                <td data-label="Namespace"><code>${breakableIri(runtime, row.namespace)}</code></td>
                <td class="workbench-row-actions" data-label="Actions">${writeable ? h`<button type="button"
                        class="workbench-action workbench-action--ghost workbench-action--icon" aria-label=${'Edit ' + label(row.prefix)}
                        title="Edit" @click=${(event: any) => {
                            state.editing = row.prefix;
                            state.adding = false;
                            state.error = '';
                            state.draft = null;
                            state.errorInRow = false;
                            rerender(event, true);
                        }}>${icon(runtime, 'edit')}</button><button type="button"
                        class="workbench-action workbench-action--ghost workbench-action--icon workbench-namespace-delete"
                        aria-label=${'Delete ' + label(row.prefix)} title="Delete" @click=${(event: any) => {
                            const document = event.currentTarget.ownerDocument;
                            const owns = modelOwner(event.currentTarget, model);
                            const dialog: any = (workbench as any).confirmDialog;
                            dialog.open({ title: 'Delete prefix ' + label(row.prefix) + '?', body: row.namespace,
                                confirmLabel: 'Delete prefix', danger: true }).then((confirmed: boolean) => {
                                if (confirmed && owns()) { postNamespaces(document, { action: 'delete', prefix: row.prefix }); }
                            });
                        }}>${icon(runtime, 'delete')}</button>` : ''}</td>
            </tr>`;
            return h`${state.errorInRow ? '' : errorCallout(runtime, model)}<section id="namespaces-results"
                    class="workbench-island workbench-responsive-records workbench-browse-card" data-table-wrap=${listTableWrap(model)}>
                <div class="workbench-browse-card__header">
                    <h2>Namespaces</h2><span class="workbench-browse-card__count">${formatCount(String(rows.length), context)}</span>
                    ${listTableDisplay(runtime, model)}
                    <div class="workbench-browse-card__tools">
                    <form class="workbench-browse-card__filter" role="search" @submit=${(event: any) => event.preventDefault()}>
                        <label class="workbench-visually-hidden" for="namespaces-filter">Filter prefixes or IRIs</label>
                        <div class="workbench-search-field">${icon(runtime, 'search', 'workbench-search-field__icon')}<input
                            type="text" id="namespaces-filter" placeholder="Filter prefixes or IRIs" autocomplete="off"
                            spellcheck="false" @input=${(event: any) => {
                                state.filter = event.currentTarget.value;
                                rerender(event);
                            }} /></div>
                    </form>
                    ${writeable ? h`<button type="button" class="workbench-action workbench-action--primary workbench-browse-card__action"
                        @click=${(event: any) => {
                            state.adding = true;
                            state.editing = null;
                            state.error = '';
                            state.draft = null;
                            state.errorInRow = false;
                            rerender(event, true);
                        }}>${icon(runtime, 'add')}<span>Add namespace</span></button>` : ''}
                    </div>
                </div>
                ${tableScroll(runtime, h`<table class="data workbench-namespaces-table">
                    <thead><tr><th scope="col">Prefix</th><th scope="col">Namespace</th>
                        <th scope="col"><span class="workbench-visually-hidden">Actions</span></th></tr></thead>
                    <tbody>
                        ${state.adding ? editRow(null) : ''}
                        ${visible.map((row: any) => state.editing === row.prefix ? editRow(row) : viewRow(row))}
                        ${!visible.length && !state.adding ? h`<tr class="workbench-empty-row"><td class="workbench-empty-table-message" role="status" colspan="3">${
                            rows.length ? 'No namespaces match this filter.' : 'No namespaces.'}</td></tr>` : ''}
                    </tbody>
                </table>`)}
            </section>`;
        }

        /** Types and Graphs (M5.2, mockup 06): what each list shows and the column its counts fill in. */
        const browseLists: { [viewId: string]: any } = {
            types: { title: 'Types', noun: 'types', column: 'type', label: 'Type',
                count: 'instances', countLabel: 'Instances', resort: true },
            contexts: { title: 'Graphs', noun: 'graphs', column: 'context', label: 'Graph',
                count: 'statements', countLabel: 'Statements', actions: true }
        };

        /**
         * Counts load after the list renders: 'pending', then 'done', 'timed-out' or 'failed' ('none' for an empty
         * list). Types adopt the count-ordered rows; Graphs keep their rows and look counts up by N-Triples key, where
         * '' is the default graph.
         */
        interface BrowseCounts {
            state: string;
            listed: number;
            values: { [key: string]: any };
        }

        function browseCounts(model: PageModel): BrowseCounts {
            const holder: any = model;
            if (!holder.browseCounts) {
                // Graphs without a filter always lists the default graph, whose count is asked for as well.
                const defaultGraphListed = model.viewId === 'contexts' && !text(pageValue(model, 'filter'));
                holder.browseCounts = { state: model.rowCount > 0 || defaultGraphListed ? 'pending' : 'none',
                    listed: model.rowCount, values: {} };
            }
            return holder.browseCounts;
        }

        function browseNameCell(runtime: LitRuntime, model: PageModel, list: any, term: any): any {
            const h = runtime.html;
            if (!term) {
                return h`<td data-label=${list.label}>Default graph</td>`;
            }
            if (typeof term !== 'object' || !term.kind) {
                return h`<td data-label=${list.label}>${text(term)}</td>`;
            }
            const stream = workbench.queryStream as any;
            const full = termText(term);
            const prefixed = list.column === 'type' && term.kind === 'iri' ? stream.abbreviateIri(term.value, exploreNamespaces(model)) : '';
            const label = prefixed && prefixed.charAt(0) !== '<' ? prefixed : full;
            return h`<td data-label=${list.label}><a href="${'explore?resource=' + encodeURIComponent(stream.exploreResource(term))}"
                title=${full}>${label}</a></td>`;
        }

        function browseCountCell(runtime: LitRuntime, list: any, counts: BrowseCounts, value: any,
                                 context: ViewContext): any {
            const h = runtime.html;
            if (value !== null && typeof value !== 'undefined') {
                return h`<td class="workbench-count-cell" data-label=${list.countLabel}>${formatCount(value, context)}</td>`;
            }
            if (counts.state === 'pending') {
                return h`<td class="workbench-count-cell" data-label=${list.countLabel} aria-busy="true"><span
                    title="Counting…">…</span></td>`;
            }
            const reason = counts.state === 'timed-out' ? 'Counting took longer than two seconds' : 'No count is available';
            return h`<td class="workbench-count-cell" data-label=${list.countLabel}><span title=${reason}>—</span></td>`;
        }

        /** A graph's actions: Explore it, and Clear it when the Clear page is available (not hidden by the policy). */
        function browseActionsCell(runtime: LitRuntime, term: any, context: ViewContext): any {
            const h = runtime.html;
            if (!term || typeof term !== 'object' || !term.kind) {
                return h`<td class="workbench-row-actions" data-label="Actions"></td>`;
            }
            const key = encodeURIComponent(ntriples(term));
            const name = termText(term);
            return h`<td class="workbench-row-actions" data-label="Actions">${pageEnabled(context, 'explore')
                ? h`<a class="workbench-action workbench-action--ghost workbench-action--icon"
                    href="${'explore?resource=' + key}" aria-label="${'Explore ' + name}" title="Explore">${icon(runtime, 'explore')}</a>` : ''}${
                pageEnabled(context, 'clear') && repositoryWriteable(context) ? h`<a
                    class="workbench-action workbench-action--ghost workbench-action--icon" href="${'clear?context=' + key}"
                    aria-label="${'Clear graph ' + name + '…'}" title="Clear graph…">${icon(runtime, 'clear')}</a>` : ''}</td>`;
        }

        function browseListPage(runtime: LitRuntime, model: PageModel, context: ViewContext): any {
            const h = runtime.html;
            const route = model.viewId;
            const list = browseLists[route];
            const counts = browseCounts(model);
            const filter = text(pageValue(model, 'filter'));
            const byCount = list.resort && counts.state === 'done';
            const columns = [list.column, list.count].concat(list.actions ? ['actions'] : []);
            const header = (name: string) => {
                if (name === 'actions') {
                    return h`<th scope="col"><span class="workbench-visually-hidden">Actions</span></th>`;
                }
                const sort = name === list.column && !byCount ? 'ascending'
                    : name === list.count && byCount ? 'descending' : runtime.nothing;
                return name === list.count
                    ? h`<th scope="col" class="workbench-count-column" aria-sort="${sort}">${list.countLabel}</th>`
                    : h`<th scope="col" aria-sort="${sort}">${list.label}</th>`;
            };
            // The server names the listed column (type or context); the counts add a second one.
            const nameColumn = model.vars && model.vars.length ? model.vars[0] : list.column;
            const cells = (record: any) => {
                const term = record[nameColumn];
                const value = list.resort ? record[list.count] : counts.values[term ? ntriples(term) : ''];
                return h`${browseNameCell(runtime, model, list, term)}${browseCountCell(runtime, list, counts, value, context)}${
                    list.actions ? browseActionsCell(runtime, term, context) : ''}`;
            };
            // The default graph has no name, so it is listed only without a filter (the server omits it too).
            const trailing = list.actions && !filter ? () => h`<tr class="workbench-browse-default-row">${
                browseNameCell(runtime, model, list, null)}${browseCountCell(runtime, list, counts, counts.values[''], context)}${
                browseActionsCell(runtime, null, context)}</tr>` : null;
            const inputId = route + '-filter';
            // The value attribute leaves a field that was typed into alone, so this page rendering again (its counts
            // arriving) keeps what is typed; bindRowWindows sets the field to the filter of each page opened.
            return h`${errorCallout(runtime, model)}<section id=${route + '-results'}
                    class="workbench-island workbench-responsive-records workbench-browse-card" data-table-wrap=${listTableWrap(model)}>
                <div class="workbench-browse-card__header">
                    <h2>${list.title}</h2><span class="workbench-browse-card__count">${formatCount(String(counts.listed), context)}</span>
                    ${listTableDisplay(runtime, model)}
                    <form class="workbench-browse-card__filter" action=${route} method="get" role="search">
                        <label class="workbench-visually-hidden" for="${inputId}">${'Filter ' + list.noun}</label>
                        <div class="workbench-search-field">${icon(runtime, 'search', 'workbench-search-field__icon')}<input
                            type="text" id="${inputId}" name="filter" value=${filter} placeholder=${'Filter ' + list.noun}
                            autocomplete="off" spellcheck="false" /></div>
                    </form>
                </div>
                ${table(runtime, model, context, { columns, header, cells, trailing,
                    emptyText: filter ? 'No ' + list.noun + ' match this filter.'
                        : route === 'contexts' ? 'No named graphs.' : 'No types.' })}
            </section>`;
        }

        const explorePageSize = 40;
        const rdfsNamespace = 'http://www.w3.org/2000/01/rdf-schema#';
        const rdfNamespace = 'http://www.w3.org/1999/02/22-rdf-syntax-ns#';
        /** Pages that fit in one row window are grouped by role; larger pages keep the single windowed table. */
        const exploreGroupedRowLimit = 80;
        const exploreRoles: any[] = [
            { key: 'outgoing', title: 'Outgoing', columns: ['predicate', 'object'] },
            { key: 'incoming', title: 'Incoming', columns: ['subject', 'predicate'] },
            { key: 'predicate', title: 'Used as predicate', columns: ['subject', 'object'] },
            { key: 'graph', title: 'Graph contents', columns: ['subject', 'predicate', 'object'] }
        ];

        function ntriples(term: any): string {
            const stream = workbench.queryStream as any;
            return stream && typeof stream.ntriplesTerm === 'function' ? stream.ntriplesTerm(term) : text(term);
        }

        /** The explored resource in N-Triples form: the server's resolved value, or the typed text when it already
         *  is N-Triples. */
        function exploreResourceKey(model: PageModel): string {
            const resolved = text(pageValue(model, 'explore-resource'));
            if (resolved) {
                return resolved;
            }
            const typed = text(pageValue(model, 'resource')).trim();
            return /^(?:<|_:|"|<<)/.test(typed) ? typed : '';
        }

        function exploreNamespaces(model: PageModel): { prefix: string; name: string }[] {
            const map: any = model.namespaceMap || {};
            return Object.keys(map).map((prefix: string) => ({
                prefix: prefix.charAt(prefix.length - 1) === ':' ? prefix.slice(0, -1) : prefix,
                name: map[prefix]
            }));
        }

        function exploreGraphLabel(term: any, namespaces: any[]): string {
            if (!term) {
                return 'Default graph';
            }
            const stream = workbench.queryStream as any;
            return stream && typeof stream.formatRdfTerm === 'function'
                ? stream.formatRdfTerm(term, { namespaces }).label : text(term);
        }

        /** A page's rows reduced to some of its columns, in their order; a missing value is unbound (null). */
        function projectRows(model: PageModel, rows: any[][], columns: string[]): any[][] {
            const indexes = columns.map((name: string) => (model.vars || []).indexOf(name));
            return rows.map((row: any[]) => indexes.map((index: number) => {
                const value = index >= 0 ? row[index] : null;
                return typeof value === 'undefined' ? null : value;
            }));
        }

        /**
         * The four role groups: Outgoing, Incoming, Used as predicate and Graph contents (mockup 05). Each is the
         * shared result table, so long IRIs wrap and the columns stay inside the card as on the Query page.
         */
        function exploreRoleGroups(runtime: LitRuntime, model: PageModel, context: ViewContext, roles: any): any {
            const h = runtime.html;
            const namespaces = exploreNamespaces(model);
            const index = (name: string) => model.vars.indexOf(name);
            return exploreRoles.filter((role: any) => roles[role.key].length).map((role: any) => {
                const entries: any[] = roles[role.key];
                const rows: any[][] = entries.map((entry: any) => entry.values);
                const graphs = rows.map(row => index('context') >= 0 ? row[index('context')] : null);
                const graphKeys = graphs.map(graph => graph ? ntriples(graph) : '');
                const oneGraph = role.key === 'graph' || graphKeys.every(key => key === graphKeys[0]);
                const columns = role.columns.concat(oneGraph ? [] : ['context']);
                const headingId = 'explore-group-' + role.key;
                return h`<section class="explore-group" data-explore-role=${role.key} aria-labelledby=${headingId}>
                    <h3 id=${headingId} class="explore-group__title">${role.title}
                        <span class="explore-group__count">${rows.length}</span>
                        ${oneGraph && role.key !== 'graph'
                            ? h`<span class="explore-group__graph">Graph: ${exploreGraphLabel(graphs[0], namespaces)}</span>` : ''}</h3>
                    ${resultTable(runtime, model, context, {
                        key: 'explore-' + role.key,
                        label: role.title + ' statements',
                        columns,
                        rows: projectRows(model, rows, columns),
                        rowIndex: (position: number) => entries[position].index,
                        statement: (position: number) => projectRows(model, [rows[position]], statementColumns)[0],
                        namespaces,
                        signature: columns.join(' ') + ':' + entries.map((entry: any) => entry.index).join(',')
                    })}
                </section>`;
            });
        }

        /** The explored resource: label, IRI with Copy, types as chips, comment and "Query this resource". */
        function exploreResourceCard(runtime: LitRuntime, model: PageModel, summary: any, resource: string,
                                     context: ViewContext): any {
            const h = runtime.html;
            const namespaces = exploreNamespaces(model);
            const key = exploreResourceKey(model) || resource;
            const isIri = /^<[^<]/.test(key);
            const comment = summary.comment ? text(summary.comment) : '';
            const types = distinctExploreValues(model, summary.types || []);
            const copy = () => {
                const clipboard = typeof navigator !== 'undefined' && navigator.clipboard;
                if (clipboard && typeof clipboard.writeText === 'function') {
                    clipboard.writeText(isIri ? key.slice(1, -1) : key);
                }
            };
            const toggleComment = (event: any) => {
                const button = event.currentTarget;
                const card = button && button.closest ? button.closest('.explore-resource-card') : null;
                const paragraph = card ? card.querySelector('.explore-resource-card__comment') : null;
                if (paragraph) {
                    const expanded = paragraph.classList.toggle('is-expanded');
                    button.setAttribute('aria-expanded', expanded ? 'true' : 'false');
                    button.textContent = expanded ? 'Show less' : 'Show more';
                }
            };
            return h`<section id="explore-resource-card" class="workbench-island explore-resource-card" aria-label="Explored resource">
                ${summary.label ? h`<h2>${text(summary.label)}</h2>` : ''}
                <div class="explore-resource-card__iri"><code id="explore-resource-iri">${isIri ? key.slice(1, -1) : key}</code>
                    <button class="workbench-action workbench-action--ghost workbench-action--icon" type="button"
                        aria-label="Copy resource" title="Copy resource" @click=${copy}>${icon(runtime, 'copy')}</button></div>
                ${types.length ? h`<ul class="explore-resource-card__types" aria-label="Types">${types.map((type: any) => h`<li>
                    <a class="explore-chip" href=${'explore?resource=' + encodeURIComponent(ntriples(type))}>${exploreGraphLabel(type, namespaces)}</a></li>`)}</ul>` : ''}
                ${comment ? h`<div class="explore-resource-card__description"><p class="explore-resource-card__comment workbench-prose">${comment}</p>
                    ${comment.length > 240 ? h`<button class="workbench-action workbench-action--ghost explore-resource-card__more" type="button"
                        aria-expanded="false" @click=${toggleComment}>Show more</button>` : ''}</div>` : ''}
                ${isIri && pageEnabled(context, 'query') ? h`<a class="workbench-action workbench-action--secondary explore-resource-card__query"
                    href=${'query?query=' + encodeURIComponent('SELECT * WHERE { ' + key + ' ?p ?o }')}>${icon(runtime, 'query')}<span>Query this resource</span></a>` : ''}
            </section>`;
        }
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
            const key = exploreResourceKey(model);
            return {
                groups,
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
            const contextIndex = model.vars.indexOf('context');
            const key = accumulator.resourceKey;
            const matches = (term: any) => !!key && !!term && ntriples(term) === key;
            rows.forEach((row: any[], offset: number) => {
                const subject = subjectIndex >= 0 ? row[subjectIndex] : null;
                const predicate = predicateIndex >= 0 ? text(row[predicateIndex]) : '';
                const object = objectIndex >= 0 ? row[objectIndex] : null;
                const rowIndex = start + offset;
                // Another resource's label must not become the heading: only rows about the explored resource count.
                const aboutResource = !key || matches(subject);
                if (accumulator.roles) {
                    const role = matches(subject) ? 'outgoing'
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
                } else if (predicate === rdfsNamespace + 'comment' && aboutResource) {
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
                roles: accumulator.roles,
                types: accumulator.types,
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

        /** Values in list order without repeats (a statement in several graphs) and without the explored resource. */
        function distinctExploreValues(model: PageModel, values: any[]): any[] {
            const explored = exploreResourceKey(model);
            const seen: { [key: string]: boolean } = Object.create(null);
            return values.filter((value: any) => {
                const key = ntriples(value);
                if (key === explored || seen[key]) {
                    return false;
                }
                seen[key] = true;
                return true;
            });
        }

        function exploreGroupContent(runtime: LitRuntime, model: PageModel, context: ViewContext,
                                     definition: any, page: any): any {
            const h = runtime.html;
            return h`<h3>${definition.title}</h3>
                <ul>${distinctExploreValues(model, page.items)
                    .map((value: any) => h`<li>${renderTerm(runtime, value, context, true)}</li>`)}</ul>
                ${page.count > explorePageSize ? h`<div class="workbench-form-actions workbench-window-controls"
                    role="group" aria-label=${definition.title + ' pages'}>
                    <span role="status">Showing ${page.start + 1}–${page.start + page.items.length} of ${page.count}</span>
                    <button type="button" class="workbench-action workbench-action--secondary" data-workbench-explore-group=${definition.key}
                        data-workbench-explore-action="previous" ?disabled=${!page.hasPrevious}>Previous</button>
                    <button type="button" class="workbench-action workbench-action--secondary" data-workbench-explore-group=${definition.key}
                        data-workbench-explore-action="next" ?disabled=${!page.hasNext}>Next</button>
                </div>` : ''}`;
        }

        /**
         * The Explore limit in use, as ExploreServlet picks it: the request's limit_explore, else 100. The server reads
         * no cookie for it (isParameterPresent), so neither does this.
         */
        function activeExploreLimit(): number {
            const limit = Number(locationParameter('limit_explore') || '100');
            return isFinite(limit) && limit >= 0 ? limit : 100;
        }

        /**
         * An Explore pager button names the page size it moves by (C20), not the rows on this page. Its value keeps the
         * rows shown: paging.correctButtons reads them to know whether a next page exists.
         */
        function pagerLabel(direction: string, pageSize: number): string {
            return pageSize > 0 ? direction + ' ' + pageSize : direction;
        }

        function explorePage(runtime: LitRuntime, model: PageModel, context: ViewContext): any {
            const h = runtime.html;
            // A refused resource (C10) has no page metadata: the field keeps what was asked for.
            const resource = text(pageValue(model, 'resource')) || (model.error ? text(locationParameter('resource')) : '');
            // A rejected Explore answers with an error-message row, or a 400 error: an error, not a result.
            const rejected = isErrorOnlyModel(model) || !!model.error;
            const total = rejected ? 0 : rowCount(model);
            const summary = (model as any).exploreSummary || summarizeVisibleExploreRows(model);
            const info = workbenchData(context);
            const defaults = info.defaults || info;
            const resultLimit = activeExploreLimit();
            // A page as long as the limit in use may have been cut short.
            const resultLimited = !rejected && !!exploreResourceKey(model) && resultLimit > 0 && total === resultLimit;
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
            // Group only when every row of the page has a role; anything unexpected keeps the plain table.
            // Display keeps its toggle in the form row and opens its panel in a track below the row.
            const exploreDisplay = workbench.detailDisclosure.renderSeparated(h, {
                id: 'explore-result-options', toggleId: 'explore-result-options-toggle',
                panelId: 'explore-result-options-panel', label: 'Display', accessibleName: 'Result display options',
                ownerClass: 'workbench-options workbench-form-subgroup'
            }, h`<div class="workbench-field workbench-disclosure__field"><label for="limit_explore">Result limit</label>
                    ${limitSelect(runtime, 'limit_explore', context, String(resultLimit))}
                </div>${tableDisplayFields(runtime, model, { layout: 'explore-result-layout', wrap: 'explore-wrap-values' },
                    (wrap: boolean) => setPageResultPresentation(model, { wrap }))}<label class="workbench-check" for="explore-show-datatypes">
                    <input id="explore-show-datatypes" type="checkbox" name="show-datatypes" value="show-dataypes" checked
                        @change=${(event: any) => showPageResultDatatypes(model, !!event.currentTarget.checked)} />
                    <span>Show datatypes</span></label>`);
            const roles = summary.roles;
            const groupedRows = roles ? exploreRoles.reduce((sum: number, role: any) => sum + roles[role.key].length, 0) : 0;
            const grouped = !!roles && !!total && groupedRows === total;
            // A page short enough to group waits until bindRowWindows has read all its rows, so it never shows the
            // single table first and then the groups.
            const groupingPending = !grouped && !!model.rowStore && !(model as any).exploreSummary
                && (model.rows || []).length < total && total <= exploreGroupedRowLimit && !!exploreResourceKey(model);
            const namespaces = exploreNamespaces(model);
            const statements = () => resultTable(runtime, model, context, {
                key: 'explore-all',
                label: 'Explore statements',
                columns: model.vars || [],
                rows: model.rowStore ? undefined : projectRows(model, model.rows || [], model.vars || []),
                store: model.rowStore,
                count: total,
                namespaces,
                signature: 'all:' + total + ':' + (model.vars || []).join(' ')
            });
            // explore.ts writes the resource and the row range into the summary's spans, so they hold no template
            // values: the router renders the next Explore page in place, and Lit must find its own nodes there (M12.1).
            return h`<form id="explore-form" class="workbench-island explore-form" action="explore">
                    <input id="workbench-total-result-count" type="hidden"
                        value=${text(pageValue(model, 'total-result-count'))} />
                    <div id="explore-controls"><div id="explore-resource-field" class="workbench-field">
                        <label for="resource">Resource</label>
                        <div class="workbench-search-field">${icon(runtime, 'search', 'workbench-search-field__icon')}<input id="resource"
                            name="resource" size="48" type="text" value=${resource} spellcheck="false"
                            placeholder="<http://…>, prefix:name, _:node or &quot;literal&quot;" /></div>
                    </div>
                    <button id="explore-submit" class="workbench-action workbench-action--primary" type="submit">Explore</button>
                    ${exploreDisplay.owner}
                    </div>
                    <div class="workbench-action-toolbar__panels workbench-disclosure-track">${exploreDisplay.panel}</div>
                </form>
                ${resultLimited ? h`<p id="result-limited">The results shown may be truncated.</p>` : ''}
                ${model.error ? callout(runtime, 'error', model.error.message) : errorCallout(runtime, model)}
                ${!model.error && (resource || exploreResourceKey(model))
                    ? exploreResourceCard(runtime, model, summary, resource || exploreResourceKey(model), context) : ''}
                <p id="explore-resource-summary" class="workbench-page-meta" ?hidden=${!resource}>
                    <span id="explore-resource-value" hidden></span><span id="explore-result-count"></span>
                </p>
                ${rejected ? '' : h`<section id="explore-results" class="workbench-island">
                    ${total ? h`${groupedResults}${grouped ? exploreRoleGroups(runtime, model, context, roles)
                        : groupingPending ? h`<p class="workbench-page-meta workbench-result-pending" role="status">Loading rows…</p>`
                            : statements()}`
                        : h`<p class="workbench-empty" role="status">No results to display.</p>`}
                    <div id="explore-pagination" class="workbench-form-actions" ?hidden=${total === 0}>
                        <button id="previousX" class="workbench-action workbench-action--secondary" type="button" value=${'Previous ' + total}
                            @click=${() => invoke('workbench.paging.previousOffset', 'explore')}>${pagerLabel('Previous', resultLimit)}</button>
                        <button id="nextX" class="workbench-action workbench-action--secondary" type="button" value=${'Next ' + total}
                            @click=${() => invoke('workbench.paging.nextOffset', 'explore')}>${pagerLabel('Next', resultLimit)}</button>
                    </div>
                </section>`}`;
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

        /** Saved query details (M5.6): Yes/No values; the query language only when it is not SPARQL. */
        function savedQueryDetails(row: any): [string, any][] {
            const yesNo = (value: any) => text(value) === 'true' ? 'Yes' : 'No';
            const language = text(row.queryLn);
            const details: [string, any][] = language && language.toUpperCase() !== 'SPARQL' ? [['Query language', language]] : [];
            return details.concat([['Include inferred statements', yesNo(row.infer)], ['Shared', yesNo(row.shared)]]);
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
                                aria-expanded="false" aria-controls=${urn + '-metadata'}>Show details</button>
                            <form method="post" action="query"><input type="hidden" name="action" value="edit" />
                                <input type="hidden" name="queryLn" value=${text(row.queryLn)} /><input type="hidden" name="query" value=${queryName} />
                                <input type="hidden" name="ref" value="id" /><input type="hidden" name="owner" value=${owner} />
                                <input type="hidden" name="infer" value=${text(row.infer)} />
                                <input type="hidden" name="query-timeout" value=${queryTimeout} /><button class="workbench-action workbench-action--secondary" type="submit">Edit</button>
                            </form>
                            <form method="post" id=${urn} action="saved-queries"><input type="hidden" name="delete" value=${urn} />
                                <button type="button" class="saved-query-delete workbench-action workbench-action--danger-outline" data-query-owner=${owner} data-query-name=${queryName}
                                    data-query-urn=${urn}>Delete…</button>
                            </form>
                        </div>
                        <div id=${'saved-query-results-' + index} class="query-results"></div>
                        <div class="saved-query-metadata" id=${urn + '-metadata'} style="display: none">${keyValueList(runtime,
                            savedQueryDetails(row))}</div>
                        <textarea id=${urn + '-text'} style="display: none">${query}</textarea>
                    `;
        }

        /** The formats Export can write; the server's `export-formats` adds file extensions and named-graph support. */
        function exportFormats(model: PageModel, info: any): any[] {
            const described = meta(model, 'export-formats');
            if (Array.isArray(described) && described.length) {
                return described.map((format: any) => ({ value: text(format.value), label: text(format.label),
                    extension: text(format.extension), graphs: format.graphs === true || format.graphs === 'true' }));
            }
            return formatOptions(info.graphDownloadFormats || info['graph-download-format'])
                .map((format: any) => ({ value: format.value, label: format.label, extension: '', graphs: null as boolean }));
        }

        /** What happens to the named graphs in the chosen format, so a single-graph format is never a surprise. */
        function exportFormatNote(format: any, graphCount: number): { text: string; warning: boolean } {
            if (format.graphs === true) {
                return { text: 'Keeps the named graphs.', warning: false };
            }
            if (format.graphs !== false) {
                return { text: '', warning: false };
            }
            if (graphCount === 0) {
                return { text: format.label + ' does not record graphs; this repository has no named graphs to lose.', warning: false };
            }
            const which = isFinite(graphCount)
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
        function exportPage(runtime: LitRuntime, model: PageModel, context: ViewContext): any {
            const h = runtime.html;
            const info = workbenchData(context);
            const defaults = info.defaults || info;
            const formats = exportFormats(model, info);
            const locationObject: any = typeof window !== 'undefined' ? window.location : null;
            const urlFormat = locationObject && typeof locationObject.search === 'string'
                ? new URLSearchParams(locationObject.search).get('Accept') : '';
            const explicitFormat = text(urlFormat || meta(model, 'Accept') || pageValue(model, 'Accept'));
            const cookieFormat = currentCookieValue('Accept');
            const configuredFormat = text(meta(model, 'default-export-format') || defaults['default-export-format']);
            const genericFormat = text(meta(model, 'default-Accept') || defaults['default-Accept']);
            const available = (candidate: string) => formats.some((format: any) => format.value === candidate);
            const holder: any = model;
            if (!holder.exportChoice) {
                const urlCompression = locationObject && typeof locationObject.search === 'string'
                    ? new URLSearchParams(locationObject.search).get('compression') : '';
                holder.exportChoice = {
                    format: [explicitFormat, cookieFormat, configuredFormat, genericFormat]
                        .filter((candidate: string) => candidate && available(candidate))[0]
                        || (formats.length ? formats[0].value : 'application/n-quads'),
                    compression: ['none', 'gzip', 'zip'].indexOf(urlCompression) >= 0 ? urlCompression : 'gzip'
                };
            }
            const choice = holder.exportChoice;
            const format = formats.filter((candidate: any) => candidate.value === choice.format)[0]
                || { value: choice.format, label: choice.format, extension: '', graphs: null as boolean };
            const extension = format.extension || 'rdf';
            const fileName = choice.compression === 'zip' ? 'export.zip'
                : 'export.' + extension + (choice.compression === 'gzip' ? '.gz' : '');
            const graphCountValue = meta(model, 'named-graph-count');
            const graphCount = graphCountValue === undefined || graphCountValue === null || text(graphCountValue) === ''
                ? NaN : Number(text(graphCountValue));
            const note = exportFormatNote(format, graphCount);
            const compressionHelp = choice.compression === 'zip' ? 'A .zip archive with export.' + extension + ' inside.'
                : choice.compression === 'gzip' ? 'The smallest download. Add RDF and most RDF tools read .gz files directly.'
                    : 'The file is as large as the data.';
            const timeout = text(meta(model, 'export-timeout')) || '43200';
            const safari = isSafari();
            const requested = text(meta(model, 'statement-preview-requested')) === 'true';
            const previewLimit = text(meta(model, 'statement-preview-limit')) || '100';
            const repositoryName = context.repositoryId || 'this repository';
            const rerender = (event: any) => {
                const outlet = event.currentTarget.closest('.workbench-outlet');
                if (outlet) { render(outlet, model, context, runtime); }
            };
            const option = (candidate: any) => h`<option value=${candidate.value} ?selected=${candidate.value === format.value}>${
                candidate.label}</option>`;
            const keeping = formats.filter((candidate: any) => candidate.graphs === true);
            const merging = formats.filter((candidate: any) => candidate.graphs !== true);
            return h`<form id="export-form" class="workbench-island workbench-form-card export-card" action="export">
                <div class="export-card__header"><h2>Download a file</h2>
                    <p class="workbench-page-meta">Writes every statement in ${repositoryName} to one file.</p></div>
                <div class="workbench-form-grid export-download__fields">
                    <div class="workbench-field"><label for="Accept">Format</label>
                        <div class="workbench-select-control"><select id="Accept" name="Accept" aria-describedby="export-format-note"
                                @change=${(event: any) => { choice.format = event.currentTarget.value; rerender(event); }}>${
                            keeping.length && merging.length
                                ? h`<optgroup label="Keep named graphs">${keeping.map(option)}</optgroup>
                                    <optgroup label="Without named graphs">${merging.map(option)}</optgroup>`
                                : formats.map(option)}</select>${icon(runtime, 'chevron', 'workbench-select-chevron')}</div>
                        <p id="export-format-note" class=${'workbench-field__help' + (note.warning ? ' workbench-field__help--warning' : '')}
                            ?hidden=${!note.text}>${note.warning ? icon(runtime, 'warning-sign', 'workbench-field__help-icon') : ''}${note.text}</p>
                    </div>
                    <fieldset class="workbench-segmented export-compression" aria-describedby="export-compression-help">
                        <legend>Compression</legend>
                        ${[['none', 'None'], ['gzip', 'Gzip'], ['zip', 'Zip']].map((entry: string[]) => h`<label for=${'compression-' + entry[0]}>
                            <input type="radio" id=${'compression-' + entry[0]} name="compression" value=${entry[0]}
                                ?checked=${choice.compression === entry[0]}
                                @change=${(event: any) => { choice.compression = entry[0]; rerender(event); }} /><span>${entry[1]}</span>
                        </label>`)}
                        <p id="export-compression-help" class="workbench-field__help workbench-segmented__help">${compressionHelp}</p>
                        ${safari && choice.compression !== 'none' ? h`<p id="export-safari-note"
                            class="workbench-field__help workbench-segmented__help">Safari expands .gz and .zip downloads after saving
                            them. To keep the compressed file, turn off <em>Open “safe” files after downloading</em> in Safari
                            Settings › General.</p>` : ''}
                    </fieldset>
                </div>
                ${workbench.detailDisclosure.render(h, {
                    id: 'export-advanced', toggleId: 'export-advanced-toggle',
                    panelId: 'export-advanced-panel', label: 'Advanced settings',
                    ownerClass: 'workbench-options workbench-form-subgroup'
                }, h`<div class="workbench-field workbench-disclosure__field"><label for="timeout">Timeout</label>
                        <div class="workbench-input-unit"><input id="timeout" name="timeout" type="number" min="0" step="1" required
                            value=${timeout} aria-describedby="export-timeout-unit export-timeout-help"
                            @input=${(event: any) => {
                                const help = event.target.ownerDocument.getElementById('export-timeout-help');
                                if (help) { help.textContent = durationLabel(event.target.value); }
                            }} /><span id="export-timeout-unit" class="workbench-input-unit__suffix">seconds</span></div>
                        <p class="workbench-field__help"><span id="export-timeout-help" aria-live="polite">${durationLabel(timeout)}</span>
                            · the export stops when it takes longer.</p>
                    </div>`)}
                <div class="workbench-form-actions"><button type="submit" name="action" value="download"
                        class="workbench-action workbench-action--primary">${icon(runtime, 'download')}<span>Download <code
                        class="export-file-name">${fileName}</code></span></button></div>
            </form>
            <section id="export-results" class="workbench-island export-card">
                <div class="export-card__header"><div class="export-card__title"><h2>Preview statements</h2>
                    ${tableDisplay(runtime, model, 'export-preview', { layout: 'export-preview-layout',
                        wrap: 'export-preview-wrap-values' }, (wrap: boolean) => setPageResultPresentation(model, { wrap }),
                        !rowCount(model))}</div>
                    <p class="workbench-page-meta">Shows the first statements of ${repositoryName} here. It does not change the
                        downloaded file.</p></div>
                <form id="export-preview-form" class="export-preview__controls" action="export">
                    <input type="hidden" name="action" value="preview" />
                    <input type="hidden" name="Accept" value=${format.value} />
                    <input type="hidden" name="compression" value=${choice.compression} />
                    <label for="limit_export">Show</label><div class="workbench-select-control">${limitSelect(runtime,
                        'limit_export', context, previewLimit)}${icon(runtime, 'chevron', 'workbench-select-chevron')}</div><span
                        class="export-preview__unit">statements</span>
                    <button class="workbench-action workbench-action--secondary" type="submit">Show preview</button>
                </form>
                <p id="result-limited" class="workbench-field__help" ?hidden=${!(requested && previewLimit !== '0'
                    && rowCount(model) >= Number(previewLimit))}>${'Showing the first ' + previewLimit + ' statements.'}</p>
                ${rowCount(model) ? resultTable(runtime, model, context, {
                    key: 'export-preview',
                    label: 'Export statement preview',
                    columns: model.vars || [],
                    rows: model.rowStore ? undefined : projectRows(model, model.rows || [], model.vars || []),
                    store: model.rowStore,
                    count: rowCount(model),
                    namespaces: exploreNamespaces(model),
                    signature: 'preview:' + rowCount(model) + ':' + (model.vars || []).join(' ')
                }) : h`<p class="workbench-empty" role="status">${requested ? 'No statements to show.'
                        : 'Choose Show preview to see the first statements.'}</p>`}
            </section>`;
        }

        /** Safari (not another browser built on WebKit's user agent string), which expands downloaded archives. */
        function isSafari(): boolean {
            const agent = typeof navigator !== 'undefined' ? String(navigator.userAgent || '') : '';
            return /Safari\//.test(agent) && !/Chrome|Chromium|CriOS|FxiOS|EdgiOS|Edg\/|OPR\/|Android/.test(agent);
        }

        /** A time limit in seconds as people say it: "12 hours", "1 minute 30 seconds", "No limit" for 0. */
        export function durationLabel(value: any): string {
            const seconds = Math.floor(Number(text(value)));
            if (!isFinite(seconds) || seconds < 0 || text(value).trim() === '') {
                return '';
            }
            if (seconds === 0) {
                return 'No limit';
            }
            const parts: string[] = [];
            [[86400, 'day'], [3600, 'hour'], [60, 'minute'], [1, 'second']].reduce((rest: number, unit: any[]) => {
                const count = Math.floor(rest / unit[0]);
                if (count) { parts.push(count + ' ' + unit[1] + (count === 1 ? '' : 's')); }
                return rest - count * unit[0];
            }, seconds);
            return parts.join(' ');
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

        /**
         * A cookie's value, form-decoded ('+' is a space) unless raw: URI-decoded only, for base64 values such as the
         * server-user-password credentials, where '+' is a digit (workbench.getCookie would make it a space).
         */
        function currentCookieValue(name: string, raw?: boolean): string {
            const namespace: any = workbench as any;
            if (raw) {
                const value = documentCookieValue(name, true);
                if (value || typeof namespace.getCookie !== 'function') {
                    return value;
                }
            }
            if (namespace && typeof namespace.getCookie === 'function') {
                // template.ts reads document.cookie; without a document (workers, tests) there are no cookies.
                try {
                    return text(namespace.getCookie(name));
                } catch (error) {
                    return '';
                }
            }
            return documentCookieValue(name, false);
        }

        function documentCookieValue(name: string, raw: boolean): string {
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
                const value = cookie.substring(cookie.indexOf('=') + 1);
                return decodeURIComponent(raw ? value : value.replace(/\+/g, '%20'));
            } catch (error) {
                return cookie.substring(cookie.indexOf('=') + 1);
            }
        }

        /** The Add RDF file field (M5.5, mockup 10): a dropped file becomes the file input's selection. */
        function addDropZone(runtime: LitRuntime): any {
            const h = runtime.html;
            const active = 'add-drop-zone--active';
            const fileInput = (zone: any) => zone.querySelector('#file');
            return h`<div id="add-drop-zone" class="add-drop-zone"
                    @dragover=${(event: any) => {
                        event.preventDefault();
                        event.currentTarget.classList.add(active);
                    }}
                    @dragleave=${(event: any) => {
                        if (!event.currentTarget.contains(event.relatedTarget)) {
                            event.currentTarget.classList.remove(active);
                        }
                    }}
                    @drop=${(event: any) => {
                        event.preventDefault();
                        const zone = event.currentTarget;
                        zone.classList.remove(active);
                        const input = fileInput(zone);
                        const files = event.dataTransfer && event.dataTransfer.files;
                        if (input && !input.disabled && files && files.length) {
                            input.files = files;
                            input.dispatchEvent(new Event('change', { bubbles: true }));
                        }
                    }}
                    @click=${(event: any) => {
                        const input = fileInput(event.currentTarget);
                        if (input && !input.disabled && !event.target.closest('label, input')) {
                            input.click();
                        }
                    }}>
                <span class="add-drop-zone__prompt">${icon(runtime, 'upload')}<span>Drop an RDF file here or <label for="file"
                    class="add-drop-zone__choose">choose a file</label></span></span>
                <span id="add-drop-zone-formats" class="add-drop-zone__formats">Turtle, TriG, N-Triples, N-Quads, RDF/XML, JSON-LD and more; gzip, zip and tar accepted</span>
                <span class="add-drop-zone__file" aria-live="polite"></span>
                <input type="file" id="file" name="content" class="workbench-visually-hidden" aria-describedby="add-drop-zone-formats"
                    @change=${(event: any) => {
                        const input = event.target;
                        const name = input.closest('.add-drop-zone').querySelector('.add-drop-zone__file');
                        name.textContent = input.files && input.files.length ? input.files[0].name : '';
                        invoke('workbench.add.enabledInput', 'file');
                    }} />
            </div>`;
        }

        /** A write sent in place (M14.2): 'idle', 'running', 'done' or 'failed', and what its status says. */
        interface Submission {
            state: string;
            message: string;
        }

        function submissionOf(model: PageModel): Submission {
            const holder: any = model;
            return holder.submission || (holder.submission = { state: 'idle', message: '' });
        }

        /** A DOM node may be reused by Lit for another page; its captured model still owns async work only here. */
        function modelOwner(element: any, model: PageModel): () => boolean {
            const outlet = element && element.closest ? element.closest('.workbench-outlet') : null;
            return () => !!element && element.isConnected !== false
                && (!outlet || shownModels.get(outlet) === model && outlet.contains(element));
        }

        /** Render model again into the outlet that holds element, if that outlet still shows model. */
        function renderAgain(element: any, model: PageModel, context: ViewContext, runtime: LitRuntime): void {
            const outlet = element && element.closest ? element.closest('.workbench-outlet') : null;
            if (outlet && shownModels.get(outlet) === model) {
                render(outlet, model, context, runtime);
            }
        }

        /** The status beside a write's button: a spinner while it runs, a green tick when it is done. */
        function submissionStatus(runtime: LitRuntime, model: PageModel): any {
            const h = runtime.html;
            const submission = submissionOf(model);
            const mark = submission.state === 'running'
                ? h`<span class="workbench-submit-status__spinner" aria-hidden="true"></span>`
                : submission.state === 'done' ? icon(runtime, 'check', 'workbench-submit-status__icon')
                    : submission.state === 'failed' ? icon(runtime, 'error', 'workbench-submit-status__icon') : '';
            return h`<span class="workbench-submit-status" role="status" data-state=${submission.state}>${mark}${
                submission.message}</span>`;
        }

        /** A finished write's status belongs to what was sent: changing the form clears it. */
        function clearSubmission(event: any, model: PageModel, context: ViewContext, runtime: LitRuntime): void {
            const submission = submissionOf(model);
            if (submission.state === 'done' || submission.state === 'failed') {
                submission.state = 'idle';
                submission.message = '';
                renderAgain(event.currentTarget, model, context, runtime);
            }
        }

        /**
         * Send a write form and stay on its page (M14.2): the status says `running` while the server works and `done`
         * with a tick once it accepted the write; then `after` runs (Remove counts again, Clear lists its graphs again).
         * An error answer is shown as the page, with its callout, as before.
         */
        function sendInPlace(form: any, submitter: any, model: PageModel, context: ViewContext, runtime: LitRuntime,
                             running: string, done: string, after?: () => void): void {
            if (!modelOwner(form, model)()) { return; }
            const router: any = (workbench as any).router;
            // What the server said when it did not accept the write ('' when nothing came back).
            let refusal = '';
            // The form is read before its button is disabled.
            const sent: Promise<string> = router && typeof router.send === 'function'
                ? router.send(form, submitter, (failure: any) => { refusal = failure && failure.message || ''; })
                : Promise.resolve('fallback').then((outcome: string) => {
                    (workbench as any).submitForm(form);
                    return outcome;
                });
            const submission = submissionOf(model);
            submission.state = 'running';
            submission.message = running;
            // The refusal this page was answered with belongs to what was sent before (C17).
            (model as any).errorSuperseded = true;
            renderAgain(form, model, context, runtime);
            sent.then((outcome: string) => {
                // 'committed' shows the answer page and 'fallback' loads a document: this page is gone either way.
                if (outcome === 'committed' || outcome === 'fallback') {
                    return;
                }
                submission.state = outcome === 'done' ? 'done' : outcome === 'failed' ? 'failed' : 'idle';
                // A refusal names its reason; without an answer nobody knows whether the write happened.
                submission.message = outcome === 'done' ? done : outcome === 'failed'
                    ? refusal || 'The server did not confirm this; check the repository before trying again.' : '';
                renderAgain(form, model, context, runtime);
                if (outcome === 'done' && after) {
                    after();
                }
            });
        }

        /** Add RDF: the source, its format and target graph, then Upload with Advanced settings to its right. */
        function addPage(runtime: LitRuntime, model: PageModel, context: ViewContext): any {
            const h = runtime.html;
            const rows = records(model);
            const info = workbenchData(context);
            const defaults = info.defaults || info;
            const formats = formatOptions(info.uploadFormats || info['upload-format']);
            const isolationOptions = rows.filter((row: any) => field(row, 'isolation-level-option'));
            const selectedIsolation = text(pageValue(model, 'transaction-setting__org.eclipse.rdf4j.common.transaction.IsolationLevel'));
            const error = pageErrorMessage(model);
            return h`${error ? callout(runtime, 'error', error) : ''}
                ${systemRepositoryCallout(runtime, context)}
                <form id="add-form" method="post" action="add" enctype="multipart/form-data" class="workbench-form-card"
                        aria-busy=${submissionOf(model).state === 'running' ? 'true' : 'false'}
                        @submit=${(event: any) => {
                            event.preventDefault();
                            sendInPlace(event.currentTarget, event.submitter, model, context, runtime, 'Adding data…', 'Data added');
                        }}
                        @input=${(event: any) => clearSubmission(event, model, context, runtime)}
                        @change=${(event: any) => clearSubmission(event, model, context, runtime)}>
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
                    <div class="workbench-field-stack add-source-fields">
                        <div id="add-source-file-panel" class="workbench-field add-source-panel" data-source="file">
                            <label for="file">RDF file</label>${addDropZone(runtime)}
                        </div>
                        <div id="add-source-url-panel" class="workbench-field add-source-panel" data-source="url" hidden>
                            <label for="url">RDF URL</label><input id="url" name="url" type="text" size="48" disabled
                                @change=${() => invoke('workbench.add.enabledInput', 'url')} />
                        </div>
                        <div id="add-source-text-panel" class="workbench-field add-source-panel" data-source="text" hidden>
                            <label for="text">RDF text</label><textarea id="text" name="content" rows="6" cols="70" disabled></textarea>
                        </div>
                    </div>
                    <div class="workbench-form-grid add-target-fields">
                        <div class="workbench-field add-source-format"><label for="Content-Type">Data format</label>
                            <div class="workbench-select-control"><select id="Content-Type" name="Content-Type">
                                <option id="autodetect" value="autodetect" selected>Detect from file name</option>
                                ${formats.map((format: any) => h`<option value=${format.value}>${format.label}</option>`)}
                            </select>${icon(runtime, 'chevron', 'workbench-select-chevron')}</div>
                        </div>
                        <div class="workbench-field add-target-graph"><label for="context">Target graph</label>
                            <input id="context" name="context" type="text" size="48" placeholder="Graphs named in the data"
                                aria-describedby="context-help" value=${text(pageValue(model, 'context'))} />
                            <p id="context-help" class="workbench-field__help">Leave empty to keep the graphs named in the data and put the rest in the default graph; a graph IRI, written as http://example.org/graph or &lt;http://example.org/graph&gt;, puts every statement in that graph.</p>
                        </div>
                    </div>
                    <div id="add-upload-actions" class="workbench-form-actions"><span class="workbench-action workbench-action--primary">
                        <label class="workbench-action-hit-area">${icon(runtime, 'upload')}<span class="workbench-action-label"><input type="submit" value="Upload"
                            ?disabled=${submissionOf(model).state === 'running'} /></span></label>
                    </span>
                    ${workbench.detailDisclosure.render(h, {
                        id: 'add-import-settings', toggleId: 'add-import-settings-toggle',
                        panelId: 'add-import-settings-panel', label: 'Advanced settings',
                        ownerClass: 'workbench-options'
                    }, h`<div class="workbench-form-grid">
                            <div class="workbench-field workbench-disclosure__field"><label for="baseURI">Base URI</label>
                                <input id="baseURI" name="baseURI" type="text" size="48" aria-describedby="baseURI-help"
                                    value=${text(pageValue(model, 'baseURI'))} />
                                <p id="baseURI-help" class="workbench-field__help">Resolves relative IRIs in the data; it does not choose a graph.</p>
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
                        </div>`)}${submissionStatus(runtime, model)}</div>
                </form>`;
        }

        interface RemoveCount {
            /**
             * 'empty' (no values), 'counting' (from the first change until the count of the values now in the form
             * arrives), 'counted', 'timed-out', 'invalid' or 'failed'.
             */
            state: string;
            count: number;
            field: string;
            message: string;
            timer: any;
            controller: any;
            /** The values this state belongs to, as removeKey writes them. */
            key?: string;
            /** Raised by every change of the values; an answer for an older generation is dropped. */
            generation?: number;
        }

        interface RemovePreview {
            /** 'hidden' (not asked for, or the values changed since), 'loading', 'shown' or 'failed'. */
            state: string;
            rows: any[][];
            truncated: boolean;
            timedOut: boolean;
            message: string;
            controller: any;
            /** Raised by every request and every change of the values; an older answer is dropped. */
            generation?: number;
        }

        /** Cancel what a page still has pending when its route is disposed: Remove's count or preview. */
        export function releasePage(model: PageModel): void {
            releasePageResultTables(model);
            const state: RemoveCount = (model as any).removeCount;
            const preview: RemovePreview = (model as any).removePreview;
            if (preview && preview.controller) {
                preview.controller.abort();
                preview.controller = null;
            }
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

        /** What the Remove preview shows above its statements. */
        function removePreviewLabel(preview: RemovePreview): string {
            if (preview.state === 'loading') { return 'Loading the statements…'; }
            if (preview.state === 'failed') { return preview.message || 'The statements could not be loaded.'; }
            if (preview.timedOut) { return 'Listing the statements took longer than 2 seconds.'; }
            if (!preview.rows.length) { return 'No statements match.'; }
            if (preview.truncated) { return 'The first ' + preview.rows.length + ' statements that would be removed; more match.'; }
            return preview.rows.length === 1 ? 'This statement would be removed.'
                : 'These ' + preview.rows.length + ' statements would be removed.';
        }

        const removeFields: string[][] = [['subj', 'Subject', 'Any subject'], ['pred', 'Predicate', 'Any predicate'],
            ['obj', 'Object', 'Any object']];

        /** Remove's values as one string, to tell whether a count belongs to the values now in the form. */
        function removeKey(fields: any): string {
            return ['subj', 'pred', 'obj', 'context'].map((name: string) => fields[name] || '').join('\u0000');
        }

        /** Where a Remove with this graph value removes from, for its confirmation. */
        function removeGraphPhrase(graph: string): string {
            if (!graph) { return 'in every graph'; }
            if (graph === 'null') { return 'in the default graph'; }
            const iri = /^<(.*)>$/.exec(graph);
            return 'in graph ' + (iri ? iri[1] : graph);
        }

        /** Remove cannot run without values, while they are counted, when they are invalid or when nothing matches. */
        function removeBlocked(state: RemoveCount): boolean {
            return state.state === 'empty' || state.state === 'invalid' || state.state === 'counting'
                || state.state === 'counted' && state.count === 0;
        }

        /** "N statements" for a counted Remove, "statements" otherwise. */
        function removeAmount(state: RemoveCount, context: ViewContext): string {
            if (state.state !== 'counted') { return 'statements'; }
            return state.count > 1000000 ? 'more than 1,000,000 statements'
                : state.count === 1 ? '1 statement' : formatCount(String(state.count), context) + ' statements';
        }

        /** "N statements match" for a live Remove count; above the server's limit it says "more than". */
        function removeMatchLabel(state: RemoveCount, context: ViewContext): string {
            if (state.state !== 'counted') {
                return state.state === 'counting' ? 'Counting…' : state.state === 'timed-out' ? '—' : '';
            }
            if (state.count > 1000000) { return 'More than 1,000,000 statements match'; }
            if (state.count === 0) { return 'No statements match'; }
            return state.count === 1 ? '1 statement matches' : formatCount(String(state.count), context) + ' statements match';
        }

        /**
         * Remove (M6.5, mockup 09): the page counts the explicit statements that match as values are typed (400 ms
         * after the last change, cancelling the previous request), names that number on the button and confirms it in a
         * dialog before the existing POST. The button is disabled while nothing is chosen and when nothing matches.
         * Preview statements lists the first statements that would be removed below the form, until the values change.
         */
        function removePage(runtime: LitRuntime, model: PageModel, context: ViewContext): any {
            const h = runtime.html;
            const graphs = (model.vars || []).indexOf('error-message') < 0
                ? records(model).filter((record: any) => !!record.context) : [];
            const holder: any = model;
            const state: RemoveCount = holder.removeCount
                || (holder.removeCount = { state: 'empty', count: 0, field: '', message: '', timer: null, controller: null });
            const preview: RemovePreview = holder.removePreview || (holder.removePreview = { state: 'hidden', rows: [],
                truncated: false, timedOut: false, message: '', controller: null });
            const refresh = (form: any) => {
                const outlet = form.closest('.workbench-outlet');
                if (outlet) { render(outlet, model, context, runtime); }
            };
            const values = (form: any) => {
                const fields: any = {};
                ['subj', 'pred', 'obj', 'context'].forEach((name) => {
                    const control = form.querySelector('[name="' + name + '"]');
                    fields[name] = control ? String(control.value || '').trim() : '';
                });
                return fields;
            };
            const loadPreview = (form: any) => {
                if (preview.controller) { preview.controller.abort(); }
                const app: any = (workbench as any).app;
                const controller = typeof AbortController === 'function' ? new AbortController() : null;
                const generation = preview.generation = (preview.generation || 0) + 1;
                const current = () => preview.generation === generation;
                const fields = values(form);
                Object.assign(preview, { state: 'loading', rows: [], truncated: false, timedOut: false, message: '',
                    controller });
                refresh(form);
                const query = new URLSearchParams({ preview: 'true' });
                Object.keys(fields).forEach((name) => { if (fields[name]) { query.set(name, fields[name]); } });
                const fetcher = (url: string, options: any) => window.fetch(url, controller
                    ? Object.assign({}, options, { signal: controller.signal }) : options);
                app.loadModel(fetcher, new URL('remove?' + query.toString(), window.location.href).toString())
                    .then((answer: any) => {
                        if (!current()) { return answer.rowStore.dispose(); }
                        if (answer.error) {
                            answer.rowStore.dispose();
                            Object.assign(preview, { state: 'failed', message: answer.error.message, controller: null });
                            return refresh(form);
                        }
                        const metadata = answer.metadata || {};
                        return answer.rowStore.read(0, answer.rowCount || 0).then((rows: any[][]) => {
                            answer.rowStore.dispose();
                            // The values may have changed while the rows were read.
                            if (!current()) { return; }
                            Object.assign(preview, { state: 'shown', rows, controller: null,
                                truncated: !!metadata['preview-truncated'], timedOut: !!metadata['preview-timed-out'] });
                            refresh(form);
                        });
                    }, (error: any) => {
                        if (!current() || (error && error.name === 'AbortError')) { return; }
                        Object.assign(preview, { state: 'failed', message: '', controller: null });
                        refresh(form);
                    });
            };
            /**
             * Any change of the values ends the count of the old ones at once: Remove waits (disabled, "Counting…")
             * until the values now in the form are counted, 400 ms after the last change.
             */
            const recount = (form: any) => {
                clearTimeout(state.timer);
                state.timer = null;
                if (state.controller) { state.controller.abort(); }
                state.controller = null;
                const generation = state.generation = (state.generation || 0) + 1;
                const current = () => state.generation === generation;
                // A preview no longer shows what Remove would remove once the values change.
                preview.generation = (preview.generation || 0) + 1;
                if (preview.state !== 'hidden') {
                    if (preview.controller) { preview.controller.abort(); }
                    Object.assign(preview, { state: 'hidden', rows: [], controller: null });
                }
                const fields = values(form);
                state.key = removeKey(fields);
                state.field = '';
                state.state = !fields.subj && !fields.pred && !fields.obj && !fields.context ? 'empty' : 'counting';
                refresh(form);
                if (state.state === 'empty') {
                    return;
                }
                state.timer = setTimeout(() => {
                    state.timer = null;
                    if (!current()) { return; }
                    const app: any = (workbench as any).app;
                    const controller = typeof AbortController === 'function' ? new AbortController() : null;
                    state.controller = controller;
                    const query = new URLSearchParams({ count: 'true' });
                    Object.keys(fields).forEach((name) => { if (fields[name]) { query.set(name, fields[name]); } });
                    const fetcher = (url: string, options: any) => window.fetch(url, controller
                        ? Object.assign({}, options, { signal: controller.signal }) : options);
                    app.loadModel(fetcher, new URL('remove?' + query.toString(), window.location.href).toString())
                        .then((answer: any) => {
                            if (!current()) { return answer.rowStore.dispose(); }
                            if (answer.error) {
                                state.state = 'invalid';
                                state.field = answer.error.code;
                                state.message = answer.error.message;
                                state.controller = null;
                                answer.rowStore.dispose();
                                return refresh(form);
                            }
                            return answer.rowStore.read(0, 1).then((rows: any[][]) => {
                                answer.rowStore.dispose();
                                // The values may have changed while the count was read.
                                if (!current()) { return; }
                                const count = rows.length && rows[0][0] ? Number(text(rows[0][0])) : NaN;
                                state.state = isFinite(count) ? 'counted' : 'timed-out';
                                state.count = isFinite(count) ? count : 0;
                                state.controller = null;
                                refresh(form);
                            });
                        }, (error: any) => {
                            if (!current() || (error && error.name === 'AbortError')) { return; }
                            state.state = 'failed';
                            state.controller = null;
                            refresh(form);
                        });
                }, 400);
            };
            const counted = state.state === 'counted';
            const disabled = removeBlocked(state);
            const amount = removeAmount(state, context);
            const sending = submissionOf(model).state === 'running';
            /**
             * The dialog names the count and the graph of the values in the form when Remove is chosen; values the page
             * has not counted (yet) are counted first, and nothing is sent unless the form still holds what was confirmed.
             */
            const confirmAndSubmit = (event: any) => {
                event.preventDefault();
                const form = event.currentTarget;
                if (submissionOf(model).state === 'running') { return; }
                const fields = values(form);
                const key = removeKey(fields);
                if (state.key !== key) {
                    recount(form);
                    return;
                }
                if (removeBlocked(state)) { return; }
                const liveCounted = state.state === 'counted';
                const liveAmount = removeAmount(state, context);
                const generation = state.generation;
                (workbench as any).confirmDialog.open({
                    title: liveCounted ? 'Remove ' + liveAmount + '?' : 'Remove the matching statements?',
                    body: 'This permanently removes ' + (liveCounted ? liveAmount : 'every explicit statement')
                        + ' that match these values ' + removeGraphPhrase(fields.context) + '.',
                    confirmLabel: 'Remove statements', danger: true
                }).then((confirmed: boolean) => {
                    if (!confirmed || state.generation !== generation || removeKey(values(form)) !== key) {
                        return;
                    }
                    // The matches are gone once the server removed them, so the page counts again.
                    sendInPlace(form, null, model, context, runtime, 'Removing statements…',
                        liveCounted ? 'Removed ' + liveAmount : 'Statements removed', () => recount(form));
                });
            };
            // A rejected Remove names the graph it was sent with; otherwise only ?context= chooses a graph.
            const rejected = (model.vars || []).indexOf('error-message') >= 0;
            const selectedGraph = rejected ? text(pageValue(model, 'context')) : locationParameter('context');
            const listedGraph = selectedGraph === '' || selectedGraph === 'null'
                || graphs.some((record: any) => ntriples(record.context) === selectedGraph);
            const previewDisabled = state.state === 'empty' || state.state === 'invalid' || counted && state.count === 0
                || preview.state === 'loading';
            const namespaces = exploreNamespaces(model);
            const previewColumns = ['subject', 'predicate', 'object', 'context'];
            return h`<form id="remove-form" class="workbench-island workbench-form-card" method="post" action="remove"
                    aria-busy=${sending ? 'true' : 'false'} @submit=${confirmAndSubmit}
                    @input=${(event: any) => clearSubmission(event, model, context, runtime)}
                    @change=${(event: any) => clearSubmission(event, model, context, runtime)}>
                ${systemRepositoryCallout(runtime, context)}
                ${callout(runtime, 'warning', 'Every explicit statement that matches the values below is removed; empty fields match anything.', 'Remove is permanent.', 'remove-warning')}
                <p>Values use RDF syntax: IRIs in angle brackets, blank nodes as _:nodeID, and literals in double quotes with optional language or datatype.</p>
                <details id="remove-examples" class="workbench-options"><summary>Examples${icon(runtime, 'chevron', 'workbench-disclosure-chevron')}</summary>
                    <ul><li>URI: <tt>&lt;http://foo.com/bar&gt;</tt></li><li>BNode: <tt>_:nodeID</tt></li>
                        <li>Literal: <tt>"Hello"</tt>, <tt>"Hello"@en</tt>, or <tt>"Hello"^^&lt;http://bar.com/foo&gt;</tt></li></ul>
                </details>
                ${errorCallout(runtime, model)}
                <div class="workbench-field-stack">
                    ${removeFields.map((entry: string[]) => {
                        const invalid = state.state === 'invalid' && state.field === entry[0];
                        const onInput = (event: any) => recount(event.currentTarget.form);
                        return h`<div class="workbench-field"><label for=${entry[0]}>${entry[1]}</label>${entry[0] === 'obj'
                            ? h`<textarea id="obj" name="obj" rows="3" placeholder=${entry[2]} aria-invalid=${invalid ? 'true' : 'false'}
                                aria-describedby="obj-error" @input=${onInput}>${text(pageValue(model, 'obj'))}</textarea>`
                            : h`<input id=${entry[0]} name=${entry[0]} type="text" placeholder=${entry[2]} autocomplete="off"
                                spellcheck="false" aria-invalid=${invalid ? 'true' : 'false'} aria-describedby=${entry[0] + '-error'}
                                value=${text(pageValue(model, entry[0]))} @input=${onInput} />`}
                            <p id=${entry[0] + '-error'} class="workbench-field__error" ?hidden=${!invalid}>${invalid ? state.message : ''}</p>
                        </div>`;
                    })}
                    <div class="workbench-field"><label for="context">Graph</label>
                        <div class="workbench-select-control"><select id="context" name="context"
                                @change=${(event: any) => recount(event.currentTarget.form)}>
                            <option value="" ?selected=${!selectedGraph}>Any graph</option>
                            <option value="null" ?selected=${selectedGraph === 'null'}>Default graph</option>
                            ${listedGraph ? '' : h`<option value=${selectedGraph} ?selected=${true}>${
                                removeGraphPhrase(selectedGraph).replace(/^in graph /, '')}</option>`}
                            ${graphs.map((record: any) => h`<option value=${ntriples(record.context)}
                                ?selected=${ntriples(record.context) === selectedGraph}>${termText(record.context)}</option>`)}
                        </select>${icon(runtime, 'chevron', 'workbench-select-chevron')}</div>
                    </div>
                </div>
                <div class="workbench-form-actions remove-actions">
                    <button id="remove-preview-button" type="button" class="workbench-action workbench-action--secondary"
                        aria-controls="remove-preview" ?disabled=${previewDisabled || sending}
                        @click=${(event: any) => loadPreview(event.currentTarget.form)}>${icon(runtime, 'eye')}<span>Preview statements</span></button>
                    <button type="submit" class="workbench-action workbench-action--danger-outline" ?disabled=${disabled || sending}>${
                        icon(runtime, 'remove')}<span>${'Remove ' + amount + '…'}</span></button>
                    <span id="remove-count" class="remove-actions__count" role="status"
                        title=${state.state === 'timed-out' ? 'Counting took longer than 2 seconds' : ''}>${removeMatchLabel(state, context)}</span>
                    ${submissionStatus(runtime, model)}
                </div>
            </form>
            <section id="remove-preview" class="workbench-island remove-preview"
                    aria-labelledby="remove-preview-heading" aria-busy=${preview.state === 'loading' ? 'true' : 'false'}
                    ?hidden=${preview.state === 'hidden'}>
                <div class="remove-preview__header"><h2 id="remove-preview-heading">Statements to remove</h2>
                    ${tableDisplay(runtime, model, 'remove-preview', { layout: 'remove-preview-layout',
                        wrap: 'remove-preview-wrap-values' }, (wrap: boolean) => setPageResultPresentation(model, { wrap }),
                        !preview.rows.length)}</div>
                <p id="remove-preview-status" class="workbench-page-meta" role="status">${preview.state === 'hidden' ? ''
                    : removePreviewLabel(preview)}</p>
                ${preview.rows.length ? resultTable(runtime, model, context, {
                    key: 'remove-preview',
                    label: 'Statements to remove preview',
                    columns: previewColumns,
                    rows: preview.rows.map((row: any[]) => previewColumns.map((_name: string, index: number) =>
                        typeof row[index] === 'undefined' ? null : row[index])),
                    namespaces,
                    // Every preview request (and every change of the values) raises the generation.
                    signature: 'preview:' + (preview.generation || 0) + ':' + preview.rows.length
                }) : ''}
            </section>`;
        }

        /** "N statements", or "—" when the server could not count within its budget. */
        /**
         * Clear (M13.2) and Summary (M13.3): counts requested with counts=true once the page is shown, so the page never
         * waits for them. The server stops counting after its budget and says so with counts-timed-out; counts that
         * finished are kept. Clear keys its values by graph in N-Triples, '' for the default graph and '*' for the whole
         * repository; Summary by 'size' and 'contexts'.
         */
        const countedPages: { [viewId: string]: { timedOut: string; absorb(answer: PageModel, rows: any[][],
                                                                            counts: PageCounts): void } } = {
            clear: {
                timedOut: 'Statement counts took longer than five seconds',
                absorb(answer: PageModel, rows: any[][], counts: PageCounts): void {
                    const values = counts.values;
                    if (text(meta(answer, 'context-discovery-complete')) !== 'true') { return; }
                    // Replace choices only after the server has returned a complete graph snapshot (M14.2).
                    counts.listing = recordsFromRows(answer, rows);
                    rows.forEach((row: any[]) => {
                        if (text(row[1])) { values[row[0] ? ntriples(row[0]) : ''] = row[1]; }
                    });
                    const size = meta(answer, 'repository-size');
                    if (text(size)) { values['*'] = size; }
                }
            },
            summary: {
                timedOut: 'Counting took longer than two seconds',
                absorb(_answer: PageModel, rows: any[][], counts: PageCounts): void {
                    const values = counts.values;
                    const row = rows[0] || [];
                    if (text(row[0])) { values.size = row[0]; }
                    if (text(row[1])) { values.contexts = row[1]; }
                }
            }
        };

        interface PageCounts {
            state: string;
            values: { [key: string]: any };
            /** Why the server could not count (its counts-failed metadata), when it said. */
            reason?: string;
            /** Clear's graphs from its latest counts answer, newer than the page model's. */
            listing?: any[];
        }

        function pageCounts(model: PageModel): PageCounts {
            const holder: any = model;
            if (!holder.pageCounts) {
                const discoveryIncomplete = model.viewId === 'clear'
                    && text(meta(model, 'context-discovery-complete')) === 'false';
                holder.pageCounts = {
                    state: discoveryIncomplete ? 'discovery-timed-out' : countedPages[model.viewId] ? 'counting' : 'none',
                    values: {}
                };
            }
            return holder.pageCounts;
        }

        /** A count of a counted page, as text: the count, "Counting…" while it is on its way, or "—". */
        function pageCountLabel(model: PageModel, count: any, context: ViewContext): string {
            const counts = pageCounts(model);
            return text(count) ? statementsLabel(count, context) : counts.state === 'counting' ? 'Counting…' : '—';
        }

        /** Ask for the counts of a counted page and show them in place when they arrive; the result stops that. */
        function loadPageCounts(mount: any, model: PageModel, context: ViewContext, runtime: LitRuntime,
                                targetWindow: any): () => void {
            const counts = pageCounts(model);
            const app: any = (workbench as any).app;
            const href = targetWindow && targetWindow.location && targetWindow.location.href;
            if (counts.state !== 'counting' || !href || !app || typeof app.loadModel !== 'function') {
                return () => {};
            }
            let disposed = false;
            const url = new URL(String(href));
            url.searchParams.set('counts', 'true');
            app.loadModel(targetWindow.fetch.bind(targetWindow), url.toString())
                .then((answer: PageModel) => answer.rowStore.read(0, answer.rowCount).then((rows: any[][]) => {
                    answer.rowStore.dispose();
                    countedPages[model.viewId].absorb(answer, rows, counts);
                    if (model.viewId === 'clear') {
                        counts.state = text(meta(answer, 'context-discovery-complete')) === 'true'
                            ? text(meta(answer, 'statement-counts-timed-out')) === 'true' ? 'timed-out' : 'done'
                            : 'discovery-timed-out';
                    } else if (text(meta(answer, 'counts-failed'))) {
                        // The server could not count (an unreachable SPARQL endpoint, C32c): it says why.
                        counts.state = 'failed';
                        counts.reason = text(meta(answer, 'counts-failed'));
                    } else {
                        counts.state = text(meta(answer, 'counts-timed-out')) === 'true' ? 'timed-out' : 'done';
                    }
                }))
                .then(null, () => { counts.state = 'failed'; })
                .then(() => {
                    const outlet = outletOf(mount) || mount;
                    if (!disposed && shownModels.get(outlet) === model) { render(mount, model, context, runtime); }
                });
            return () => { disposed = true; };
        }

        function statementsLabel(count: any, context: ViewContext): string {
            const raw = text(count);
            return raw ? formatCount(raw, context) + (raw === '1' ? ' statement' : ' statements') : '—';
        }

        /** A query parameter of the current page, or '' without a browser location. */
        function locationParameter(name: string): string {
            const location: any = typeof window !== 'undefined' ? window.location : null;
            return location && typeof location.search === 'string'
                ? new URLSearchParams(location.search).get(name) || '' : '';
        }

        /**
         * Clear (M6.4, mockup 08): what to clear is chosen from the repository, the default graph and each graph, with
         * their sizes; the button names the choice and a dialog confirms it (typing the repository id for everything).
         * The form still posts `context`: empty for the entire repository, "null" for the default graph.
         */
        function clearPage(runtime: LitRuntime, model: PageModel, context: ViewContext): any {
            const h = runtime.html;
            const listing = pageCounts(model).listing
                || ((model.vars || []).indexOf('statements') >= 0 ? records(model) : []);
            const discoveryIncomplete = text(meta(model, 'context-discovery-complete')) === 'false';
            // The counts arrive after the page (M13.2); a page model that already has them shows them at once.
            const counted = pageCounts(model).values;
            const countOf = (key: string, fallback: any) => key in counted ? counted[key] : fallback;
            const targets: any[] = (discoveryIncomplete ? [] : [
                { value: '', label: 'Entire repository', count: countOf('*', meta(model, 'repository-size')) }
            ])
                .concat(listing.filter((record: any) => !record.context)
                    .map((record: any) => ({ value: 'null', label: 'Default graph', count: countOf('', record.statements) })))
                .concat(listing.filter((record: any) => !!record.context)
                    .map((record: any) => ({ value: ntriples(record.context), label: termText(record.context),
                        count: countOf(ntriples(record.context), record.statements) })));
            if (discoveryIncomplete && !targets.length) {
                targets.push({ value: '__unavailable__', label: 'Graph choices unavailable', count: '', unavailable: true });
            }
            const holder: any = model;
            if (typeof holder.clearTarget !== 'string') {
                // Only ?context= (a graph row's Clear link) or a rejected Clear's posted graph chooses a graph; the
                // listing's first graph is not a choice.
                const rejected = (model.vars || []).indexOf('error-message') >= 0;
                const requested = locationParameter('context') || (rejected ? text(pageValue(model, 'context')) : '');
                if (discoveryIncomplete && requested && !targets.some((target: any) => target.value === requested)) {
                    targets.unshift({ value: requested, label: requested + ' (not verified)', count: '', unavailable: true });
                }
                holder.clearTarget = targets.some((target: any) => target.value === requested)
                    ? requested : discoveryIncomplete ? targets[0].value : '';
            }
            const selected = targets.filter((target: any) => target.value === holder.clearTarget)[0] || targets[0];
            const everything = !discoveryIncomplete && selected.value === '';
            const repositoryId = context.repositoryId || '';
            const sending = submissionOf(model).state === 'running';
            // Nothing to clear (C32e): the choice is counted and holds no statement.
            const empty = text(selected.count) === '0';
            /** After a clear, ask for the graphs and their counts again; the tick stays (M14.2). */
            const recount = (form: any) => {
                const counts = pageCounts(model);
                counts.state = 'counting';
                counts.values = {};
                loadPageCounts(form.closest('.workbench-outlet'), model, context, runtime, window);
            };
            const confirmAndSubmit = (event: any) => {
                event.preventDefault();
                const form = event.currentTarget;
                if (sending) { return; }
                const dialog: any = (workbench as any).confirmDialog;
                const size = text(selected.count);
                const removes = size ? 'This permanently removes ' + statementsLabel(size, context) : 'This permanently removes every statement';
                dialog.open(everything
                    ? { title: 'Clear entire repository?', body: removes + ' from ' + repositoryId + '.',
                        confirmLabel: 'Clear repository', danger: true, requireText: repositoryId,
                        requireLabel: 'Type ' + repositoryId + ' to confirm' }
                    : { title: 'Clear graph?', body: removes + ' from ' + (selected.value === 'null' ? 'the default graph' : selected.label) + '.',
                        confirmLabel: 'Clear graph', danger: true }).then((confirmed: boolean) => {
                    if (confirmed) {
                        sendInPlace(form, null, model, context, runtime, 'Clearing…',
                            everything ? 'Repository cleared' : 'Graph cleared', () => recount(form));
                    }
                });
            };
            return h`<form id="clear-form" class="workbench-island workbench-form-card" method="post" action="clear"
                    aria-busy=${sending ? 'true' : 'false'} @submit=${confirmAndSubmit}
                    @change=${(event: any) => clearSubmission(event, model, context, runtime)}>
                ${systemRepositoryCallout(runtime, context)}
                ${callout(runtime, 'warning', discoveryIncomplete
                    ? 'Graph choices are incomplete. Clearing is unavailable until discovery finishes.'
                    : 'Choose one graph, or the entire repository. There is no undo.',
                    discoveryIncomplete ? 'No clear action will run against an incomplete graph list.'
                        : 'This permanently deletes statements.', 'clear-warning')}
                ${errorCallout(runtime, model)}
                <div class="workbench-field-stack">
                    <div class="workbench-field"><label for="context">What to clear</label>
                        <div class="workbench-select-control"><select id="context" name="context" @change=${(event: any) => {
                            holder.clearTarget = event.currentTarget.value;
                            const outlet = event.currentTarget.closest('.workbench-outlet');
                            if (outlet) { render(outlet, model, context, runtime); }
                        }}>${targets.map((target: any) => h`<option value=${target.value} ?disabled=${!!target.unavailable}
                                ?selected=${target === selected}>${
                            target.label + ' — ' + pageCountLabel(model, target.count, context)}</option>`)}</select>${
                            icon(runtime, 'chevron', 'workbench-select-chevron')}</div>
                        ${pageCounts(model).state === 'discovery-timed-out'
                            ? h`<p id="clear-context-discovery-help" class="workbench-field__help">${
                                discoveryIncomplete
                                    ? 'Graph choices could not be refreshed within 60 seconds. Clearing is disabled until discovery completes.'
                                    : 'Graph choices could not be refreshed within 60 seconds; existing choices and selection are kept.'}</p>`
                            : pageCounts(model).state === 'timed-out'
                                ? h`<p id="clear-counts-help" class="workbench-field__help">${countedPages.clear.timedOut}; "—" marks a count that did not finish.</p>`
                                : empty ? h`<p id="clear-empty-help" class="workbench-field__help">${everything
                                    ? 'The repository is empty: there is nothing to clear.'
                                    : 'This graph is empty: there is nothing to clear.'}</p>` : ''}
                    </div>
                </div>
                <div class="workbench-form-actions"><button type="submit" class="workbench-action workbench-action--danger"
                    ?disabled=${sending || discoveryIncomplete || !!selected.unavailable || empty}>${icon(runtime, 'clear')}<span>${
                        discoveryIncomplete ? 'Clear unavailable' : everything ? 'Clear entire repository…' : 'Clear graph…'}</span></button>
                    ${submissionStatus(runtime, model)}</div>
            </form>`;
        }

        function updatePage(runtime: LitRuntime, model: PageModel, context: ViewContext): any {
            const h = runtime.html;
            const error = pageErrorMessage(model);
            const query = text(pageValue(model, 'update')) || '\n\t';
            const mappings = context.linked && context.linked.namespaces
                ? context.linked.namespaces.namespaceMap : model.namespaceMap;
            if (typeof window !== 'undefined') {
                (window as any).namespaces = mappings || {};
                (window as any).sparqlNamespaces = mappings || {};
            }
            const sending = submissionOf(model).state === 'running';
            return h`<form id="update-form" class="workbench-island" action="update" method="post"
                    aria-busy=${sending ? 'true' : 'false'}
                    @submit=${(event: any) => {
                        const globalWorkbench: any = (window as any).workbench;
                        event.preventDefault();
                        if (sending || globalWorkbench.update && globalWorkbench.update.doSubmit
                                && !globalWorkbench.update.doSubmit()) {
                            return;
                        }
                        sendInPlace(event.currentTarget, event.submitter, model, context, runtime, 'Executing update…',
                            'Update executed');
                    }}
                    @input=${(event: any) => clearSubmission(event, model, context, runtime)}>
                ${error ? callout(runtime, 'error', error, undefined, 'updateString.errors')
                    : h`<span id="updateString.errors" class="error" role="alert"></span>`}
                <div id="update-editor" class="workbench-field"><label for="update">SPARQL Update</label>
                    <textarea id="update" name="update" rows="16" cols="80">${query}</textarea>
                    <div id="update-editor-resize" class="query-editor-resize" role="separator"
                        aria-orientation="horizontal" aria-label="Resize editor" tabindex="0"></div>
                </div>
                <div id="update-actions" class="workbench-form-actions"><span class="workbench-action workbench-action--primary">
                    <label class="workbench-action-hit-area">${icon(runtime, 'execute')}<span class="workbench-action-label"><input type="submit" value="Execute"
                        ?disabled=${sending} /></span></label>
                </span>${submissionStatus(runtime, model)}</div>
            </form>`;
        }

        function serverPage(runtime: LitRuntime, model: PageModel, context: ViewContext): any {
            const h = runtime.html;
            const row = firstRecord(model);
            const info = workbenchData(context);
            const server = text(field(row, 'server') || info.server || info.location);
            return h`<form id="server-form" class="workbench-form-card" action="server" method="post"
                    @submit=${(event: Event) => {
                        submitThroughPageScript(event, 'server.js', 'changeServer',
                            modelOwner(event.currentTarget, model), (change: any) => change(event));
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

        /** True on Apple platforms, where the Execute shortcut is Cmd+Enter instead of Ctrl+Enter. */
        function isMacPlatform(): boolean {
            const navigatorObject: any = typeof navigator !== 'undefined' ? navigator : null;
            if (!navigatorObject) {
                return false;
            }
            const platform = String((navigatorObject.userAgentData && navigatorObject.userAgentData.platform)
                || navigatorObject.platform || '');
            // 'MacIntel' (navigator.platform), 'macOS' (userAgentData in Chrome and Edge, C24), 'iPhone', 'iPad'.
            return /mac|iphone|ipad|ipod/i.test(platform);
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
                'explain-highlight-hotspot', 'explain-property-selection', 'query-timeout'
            ]);
        }

        /** The help under the explanation timeout: what an empty field falls back to (query.ts keeps it current). */
        function explanationTimeoutHelp(queryTimeout: string): string {
            const seconds = Number(queryTimeout);
            return 'Empty uses the query timeout: '
                + (seconds > 0 ? seconds + (seconds === 1 ? ' second' : ' seconds') + '. 0 means no limit.' : 'no limit.');
        }

        /** Menu toggle shown at the start of the primary editor while compare mode hides the navigation. */
        function compareSidebarToggle(runtime: LitRuntime, context: ViewContext): any {
            const h = runtime.html;
            return h`<span id="query-sidebar-toggle-action" class="query-sidebar-toggle-action workbench-action workbench-action--secondary"
                    data-workbench-action="menu"><span class="workbench-action-label">
                    <button id="query-sidebar-toggle" class="query-sidebar-toggle" type="button"
                        aria-hidden="true" aria-expanded="false" aria-controls="navigation" tabindex="-1"
                        data-show-label="Show navigation" data-hide-label="Hide navigation"
                        ?hidden=${!queryFeatureEnabled(context, 'editor-sidebar')}
                        @click=${() => invoke('workbench.query.toggleCompareSidebar')}>
                        <span id="query-sidebar-toggle-icon" class="query-sidebar-toggle__icon" aria-hidden="true">
                            ${icon(runtime, 'menu', 'query-sidebar-toggle__svg')}
                        </span>
                    </button></span></span>`;
        }

        /** Editor column of the query form; the explanation lives in the output card below the action row. */
        function queryPane(runtime: LitRuntime, options: any, context: ViewContext): any {
            const h = runtime.html;
            const compare = !!options.compare;
            const queryId = compare ? 'query-compare' : 'query';
            const suffix = compare ? '-compare' : '';
            const paneId = compare ? 'query-compare-pane' : 'query-primary-pane';
            return h`<section id=${paneId} class=${compare
                    ? 'query-compare-pane query-compare-pane--secondary'
                    : 'query-compare-pane query-compare-pane--primary'}>
                <div class="query-form__row query-form__row--stacked">
                    <div class="query-editor-header">${compare ? '' : compareSidebarToggle(runtime, context)}<label
                        class="query-form__label" for=${queryId}>${compare ? 'Compare query' : 'Query'}</label>
                        ${compare ? h`<button id="query-compare-close" class="query-compare-pane__close workbench-action workbench-action--ghost workbench-action--icon" type="button"
                            aria-label="Close comparison" title="Close comparison"
                            @click=${() => invoke('workbench.query.closeComparePane')}>${icon(runtime, 'close', 'query-compare-pane__close-icon')}</button>` : ''}</div>
                    <div class="query-form__field">${compare
                        ? h`<textarea id="query-compare" rows="16" cols="80" wrap="soft"></textarea>`
                        : h`<textarea id="query" name="query" rows="16" cols="80" wrap="soft">${text(options.query)}</textarea>`}
                        <div id=${compare ? 'query-compare-editor-resize' : 'query-editor-resize'} class="query-editor-resize"
                            role="separator" aria-orientation="horizontal" aria-label="Resize editor" tabindex="0"></div></div>
                </div>
                <div class="query-form__row"><span class="query-form__label query-form__label--blank"></span>
                    <div class="query-form__field"><span id=${'queryString.errors' + suffix} class="error">${compare ? '' : text(options.error)}</span></div>
                </div>
            </section>`;
        }

        /** One explanation column: its status line and the text, DOT and JSON views; its time sits above the toolbar. */
        function explanationColumn(runtime: LitRuntime, compare: boolean, explanation: string, selectedFormat: string,
                                   context: ViewContext): any {
            const h = runtime.html;
            const suffix = compare ? '-compare' : '';
            return h`<div id=${'query-explanation-row' + suffix} class="query-explanation-column"
                    ?hidden=${!queryFeatureEnabled(context, 'query-explain')}
                    style=${compare || !explanation ? 'display:none;' : ''}>
                <div class="query-explanation-column__header">
                    <span class="query-explanation-column__label">${compare ? 'Compare query' : 'Query'}</span>
                    ${compare
                        ? h`<button id="copy-explanation-compare" class="workbench-action workbench-action--ghost workbench-action--icon" type="button"
                            aria-label="Copy compare explanation" title="Copy compare explanation"
                            ?hidden=${!queryFeatureEnabled(context, 'explain-copy')}>${icon(runtime, 'copy')}<span
                            class="workbench-visually-hidden">Copy compare explanation</span></button>`
                        : h`<button id="query-compare-copy" class="workbench-action workbench-action--ghost workbench-action--icon" type="button"
                            aria-label="Copy query explanation" title="Copy query explanation"
                            ?hidden=${!queryFeatureEnabled(context, 'explain-copy')}>${icon(runtime, 'copy')}<span
                            class="workbench-visually-hidden">Copy query explanation</span></button>`}
                </div>
                <div id=${'query-explanation-status' + suffix} class="query-explanation-status" aria-live="polite"></div>
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
            </div>`;
        }

        /**
         * Explanation tab panel: the running or final time of each plan, one toolbar for the plan settings and actions,
         * then one or two plan columns. In compare mode the toolbar's Swap and Diff follow Compare.
         */
        function explanationPanel(runtime: LitRuntime, options: any, context: ViewContext): any {
            const h = runtime.html;
            const explanation = text(options.explanation);
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
            const queryTimeout = text(options.queryTimeout) || '0';
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
            // The pane follows its toggle, so Tab from an open Config toggle moves into the pane before the explanation
            // actions. It opens over the explanation (M14.3), so an open pane never moves the other toolbar controls.
            const explanationSettings = workbench.detailDisclosure.render(h, {
                id: 'explanation-settings', toggleId: 'explanation-settings-toggle',
                panelId: 'explanation-settings-panel', label: 'Config', hidden: !queryExplainSettingsEnabled(context),
                ownerClass: 'query-explanation-settings', toggleClass: 'query-explanation-settings__toggle workbench-action workbench-action--secondary',
                panelClass: 'query-explanation-settings__panel',
                panelRole: 'group'
            }, h`
                    <div id="explanation-timeout-section" class="query-explanation-settings__section"
                        ?hidden=${!queryFeatureEnabled(context, 'query-timeout')}>
                        <div class="query-explanation-settings__header"><label for="explanation-timeout"><strong>Timeout</strong></label></div>
                        <div class="query-explanation-timeout">
                            <input id="explanation-timeout" type="number" min="0" step="1" inputmode="numeric"
                                placeholder=${queryTimeout} aria-describedby="explanation-timeout-help" /><span
                                class="query-explanation-timeout__unit">seconds</span>
                        </div>
                        <p id="explanation-timeout-help" class="query-explanation-property-config__hint">${explanationTimeoutHelp(queryTimeout)}</p>
                    </div>
                    <div id="explanation-highlighting-section" class="query-explanation-settings__section" ?hidden=${allHighlightingDisabled}>
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
                `);
            return h`<div id="query-explanation-panel" class="query-output__panel query-explanation-panel workbench-local-progress" role="tabpanel"
                    aria-labelledby="query-output-tab-explanation" tabindex="0" ?hidden=${!explanation}>
                <div id="query-explanation-timings" class="query-explanation-timings">
                    <span class="query-explanation-timings__entry"><span class="query-explanation-timings__label">Query</span><span
                        id="query-explanation-timing" class="query-explanation-timing" role="timer"></span></span>
                    <span class="query-explanation-timings__entry"><span class="query-explanation-timings__label">Compare query</span><span
                        id="query-explanation-timing-compare" class="query-explanation-timing" role="timer"></span></span>
                </div>
                <div class="query-explanation-toolbar workbench-action-toolbar">
                    <div class="query-explanation-toolbar__settings">
                        <select id="explain-level" aria-label="Plan level" ?hidden=${allExplainLevelsDisabled}>
                            ${explainLevels.map(levelOption)}
                        </select><select id="explain-format" name="explain-format" aria-label="Plan format"
                            ?hidden=${allExplainFormatsDisabled}>
                            ${explainFormats.map(formatOption)}
                        </select>
                        ${explanationSettings}
                        <button id="explanation-cancel" class="query-explain-cancel workbench-action workbench-action--warning" type="button"
                            aria-label="Cancel explanation" title="Cancel explanation" aria-hidden="true" disabled ?hidden=${!queryFeatureEnabled(context, 'explain-cancel')}
                            @click=${() => invoke('workbench.query.cancelExplain')}>${icon(runtime, 'stop')}<span>Cancel</span></button>
                    </div>
                    <div id="query-explanation-controls-row" class="query-explanation-controls-row-class query-explanation-toolbar__actions"
                        ?hidden=${!queryFeatureEnabled(context, 'query-explain')}
                        style=${!explanation ? 'display:none;' : ''}>
                        <span id="primary-explain-settings" class="query-form__field--controls-group">
                            <span id="primary-explain-utility-controls" class="query-form__field--controls-group">
                                <button id="copy-explanation" class="workbench-action workbench-action--ghost workbench-action--icon" type="button"
                                    aria-label="Copy explanation" title="Copy explanation"
                                    ?hidden=${!queryFeatureEnabled(context, 'explain-copy')}>${icon(runtime, 'copy')}<span
                                    class="workbench-visually-hidden">Copy explanation</span></button>
                                <button id="download-explanation" class="workbench-action workbench-action--ghost workbench-action--icon" type="button"
                                    aria-label="Download explanation" title="Download explanation" ?disabled=${!explanation}
                                    ?hidden=${!queryFeatureEnabled(context, 'explain-download')}>${icon(runtime, 'download')}<span
                                    class="workbench-visually-hidden">Download explanation</span></button>
                                <button id="compare-toggle" class="workbench-action workbench-action--secondary" type="button" ?hidden=${!queryFeatureEnabled(context, 'query-compare')}
                                    @click=${() => invoke('workbench.query.toggleCompareMode')}>${icon(runtime, 'compare')}<span>Compare</span></button>
                                <span id="query-compare-toolbar" class="query-compare-toolbar" hidden>
                                    <button id="query-compare-swap" class="workbench-action workbench-action--secondary" type="button"
                                        ?hidden=${!queryFeatureEnabled(context, 'query-swap')}>${icon(runtime, 'swap')}<span>Swap</span></button>
                                    <button id="query-diff-trigger" class="query-compare-action workbench-action workbench-action--secondary" type="button" disabled
                                        ?hidden=${!queryFeatureEnabled(context, 'query-diff')}
                                        @click=${() => invoke('workbench.query.openDiffModal')}>
                                        <span id="query-diff-trigger-icon" class="query-compare-action__icon" aria-hidden="true">⇄</span>Diff</button>
                                </span>
                            </span>
                            <span id="primary-explain-repeat-controls" class="query-form__field--controls-group">
                                <button id="rerun-explanation" class="workbench-action workbench-action--secondary" type="button"
                                    ?hidden=${!queryFeatureEnabled(context, 'query-rerun')}
                                    data-query-rerun-enabled=${queryFeatureEnabled(context, 'query-rerun') ? 'true' : 'false'}
                                    @click=${() => invoke('workbench.query.runExplain', null, 'rerun-explanation')}>Explain again</button>
                                <button id="rerun-explanation-cancel" class="query-explain-cancel workbench-action workbench-action--warning" type="button"
                                    aria-label="Cancel explanation" title="Cancel explanation" aria-hidden="true" disabled ?hidden=${!queryFeatureEnabled(context, 'explain-cancel')}
                                    @click=${() => invoke('workbench.query.cancelExplain')}>${icon(runtime, 'stop')}<span>Cancel</span></button>
                            </span>
                        </span>
                    </div>
                </div>
                <div class="query-explanation-columns">
                    ${explanationColumn(runtime, false, explanation, selectedFormat, context)}
                    ${explanationColumn(runtime, true, '', selectedFormat, context)}
                </div>
                <p class="query-output__empty query-explanation-panel__empty" ?hidden=${!!explanation}>Choose Explain to see how the repository plans this query.</p>
            </div>`;
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
            // A private query belongs to the signed-in server user; without one the server would store it for everyone.
            const privateSave = queryFeatureEnabled(context, 'query-private-save');
            const signedIn = !!serverUser();
            // Controls the policy turns off are disabled as well as hidden, so the form never submits them (M-policy).
            const languageEnabled = queryFeatureEnabled(context, 'query-language');
            const timeoutEnabled = queryFeatureEnabled(context, 'query-timeout');
            const inferEnabled = queryFeatureEnabled(context, 'query-inferred-statements');
            // Each pane follows its own toggle, so Tab from an open pane's toggle moves into the pane. The panes open
            // over the page (M14.3), so where they sit in the toolbar does not move its buttons.
            const saveDisclosure = workbench.detailDisclosure.render(h, {
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
                <label class="query-option query-save-disclosure__private" ?hidden=${!privateSave}
                    title=${privateSave && !signedIn ? 'Sign in to the server to save a private query.' : runtime.nothing}>
                    <input id="save-private" name="save-private" type="checkbox" value="true"
                        ?hidden=${!privateSave} ?disabled=${!privateSave || !signedIn} ?checked=${false}
                        aria-describedby=${privateSave && !signedIn ? 'save-private-help' : runtime.nothing} />Private${
                    privateSave && !signedIn ? h`<span id="save-private-help" class="workbench-visually-hidden">Sign in to the server to save a private query.</span>` : ''}</label>
                <div class="workbench-disclosure__actions query-disclosure__actions">
                    <input id="save" type="submit" value="Save" disabled
                        ?hidden=${!queryFeatureEnabled(context, 'query-save')} /> <span id="save-feedback"></span>
                </div>`);
            const optionsDisclosure = workbench.detailDisclosure.render(h, {
                id: 'query-options-disclosure', toggleId: 'query-options-toggle',
                panelId: 'query-options-panel', label: 'Query settings', icon: icon(runtime, 'sliders'),
                ownerClass: 'query-disclosure query-options-disclosure',
                toggleClass: 'query-disclosure__toggle workbench-action workbench-action--secondary',
                panelClass: 'query-disclosure__body query-disclosure__panel',
                contentClass: 'workbench-disclosure__fields',
                toggleHidden: !queryFeatureEnabled(context, 'query-options')
            }, h`<div id="query-timeout-field" class="workbench-disclosure__field query-timeout-field"
                    ?hidden=${!timeoutEnabled}>
                    <label for="query-timeout">Timeout</label>
                    <div class="workbench-input-unit"><input id="query-timeout" name="query-timeout" type="number" min="0" step="1"
                        value=${defaultTimeout} ?hidden=${!timeoutEnabled} ?disabled=${!timeoutEnabled}
                        aria-describedby="query-timeout-unit query-timeout-help" /><span id="query-timeout-unit"
                        class="workbench-input-unit__suffix">seconds</span></div>
                    <p id="query-timeout-help" class="workbench-field__help">0 means no limit</p>
                </div>
                <div class="workbench-disclosure__field">
                    <label class="query-option" for="infer"><input id="infer" name="infer" type="checkbox" value="true"
                        ?checked=${text(defaults['default-infer']) === 'true'}
                        ?hidden=${!inferEnabled} ?disabled=${!inferEnabled} />
                        <span>Include inferred statements</span></label>
                </div>
                <div class="workbench-disclosure__actions query-disclosure__actions"><button id="query-insert-prefixes"
                    class="workbench-action workbench-action--secondary" type="button"
                    title="Add a PREFIX line for every repository namespace the query does not declare yet"
                    data-editor-namespaces-enabled=${queryFeatureEnabled(context, 'editor-namespaces') ? 'true' : 'false'}
                    ?hidden=${!queryFeatureEnabled(context, 'editor-namespaces')}
                    @click=${() => invoke('workbench.query.insertPrefixes')}>Insert prefixes</button></div>`);
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
                            ?hidden=${hideQueryLanguageRow || !languageEnabled}>
                            <label class="query-form__label" for="queryLn">Query language</label>
                            <div class="query-form__field"><select id="queryLn" name="queryLn" ?disabled=${!languageEnabled}
                                @change=${() => invoke('workbench.query.onQlChange')}>
                                ${queryFormats.length ? queryFormats.map((option: any) => h`<option value=${option.value}
                                    ?selected=${option.value === selectedQueryLanguage}>${option.label}</option>`)
                                    : h`<option value="${selectedQueryLanguage}" selected>${selectedQueryLanguage}</option>`}
                            </select></div>
                        </div>
                        <div id="query-compare-layout" class="query-compare-layout"
                            ?hidden=${!queryFeatureEnabled(context, 'query-compare')}>
                            ${queryPane(runtime, { query, error: pageValue(model, 'error-message') }, context)}
                            ${queryPane(runtime, { compare: true }, context)}
                        </div>
                        <div class="query-actions-toolbar workbench-action-toolbar"><div class="query-form__field query-actions-toolbar__primary workbench-action-toolbar__primary">
                            <button id="exec" class="query-action workbench-action workbench-action--primary" type="submit"
                                ?hidden=${!queryFeatureEnabled(context, 'query-execution')}
                                title=${'Execute (' + (isMacPlatform() ? 'Cmd' : 'Ctrl') + '+Enter)'}>${icon(runtime, 'execute')}<span>Execute</span><kbd
                                class="workbench-shortcut-hint" aria-hidden="true">${isMacPlatform() ? '⌘↵' : 'Ctrl+↵'}</kbd></button>
                            <button id="query-cancel" class="query-cancel workbench-action workbench-action--warning" type="button"
                                aria-label="Cancel query" title="Cancel query" aria-hidden="true" disabled
                                ?hidden=${!queryFeatureEnabled(context, 'query-cancel')}
                                @click=${() => invoke('workbench.query.cancelQuery')}>${icon(runtime, 'stop')}<span>Cancel</span></button>
                            <button id="explain-trigger" class="query-action workbench-action workbench-action--secondary" type="button"
                                ?hidden=${!queryExplainEnabled(context)}
                                @click=${() => invoke('workbench.query.runExplain', null, 'explain-trigger')}>${icon(runtime, 'explain')}<span>Explain</span></button>
                            <button id="explain-trigger-cancel" class="query-explain-cancel workbench-action workbench-action--warning" type="button"
                                aria-label="Cancel explanation" title="Cancel explanation" aria-hidden="true" disabled ?hidden=${!queryFeatureEnabled(context, 'explain-cancel')}
                                @click=${() => invoke('workbench.query.cancelExplain')}>${icon(runtime, 'stop')}<span>Cancel</span></button>
                            <span id="query-actions-compare" class="query-actions-compare" hidden>
                                <button id="query-actions-swap" class="query-action workbench-action workbench-action--secondary" type="button"
                                    ?hidden=${!queryFeatureEnabled(context, 'query-swap')}
                                    @click=${() => invoke('workbench.query.swapCompareQueries')}>${icon(runtime, 'swap')}<span>Swap</span></button>
                                <button id="query-actions-diff" class="query-action query-compare-action workbench-action workbench-action--secondary" type="button" disabled
                                    ?hidden=${!queryFeatureEnabled(context, 'query-diff')}
                                    @click=${() => invoke('workbench.query.openDiffModal', 'query-actions-diff')}>
                                    <span class="query-compare-action__icon" aria-hidden="true">⇄</span>Diff</button>
                            </span>
                        </div>
                        <div class="workbench-action-toolbar__actions">
                            <div class="workbench-action-toolbar__group">${saveDisclosure}${optionsDisclosure}</div>
                        </div>
                    </div>
                    </div>
                </form>
                <section id="query-output" class="query-output workbench-island" aria-label="Query output" ?hidden=${!explanation}>
                    <div class="query-output__tabs" role="tablist" aria-label="Query output">
                        <button id="query-output-tab-results" class="query-output__tab" type="button" role="tab"
                            aria-selected=${explanation ? 'false' : 'true'} aria-controls="query-results-panel"
                            tabindex=${explanation ? '-1' : '0'}><span>Results</span><span
                            id="query-results-count" class="query-output__badge" hidden></span></button>
                        <button id="query-output-tab-explanation" class="query-output__tab" type="button" role="tab"
                            aria-selected=${explanation ? 'true' : 'false'} aria-controls="query-explanation-panel"
                            tabindex=${explanation ? '0' : '-1'}
                            ?hidden=${!queryExplainEnabled(context)}>Explanation</button>
                    </div>
                    <div id="query-results-panel" class="query-output__panel query-results-panel workbench-local-progress" role="tabpanel"
                        aria-labelledby="query-output-tab-results" ?hidden=${!!explanation}>
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
                        </section>
                        <p class="query-output__empty query-results-panel__empty">Choose Execute to see the results here.</p>
                    </div>
                    ${explanationPanel(runtime, { explanation, explanationFormat, explanationLevel, queryTimeout: defaultTimeout }, context)}
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

        /**
         * Pages that show a model's error-message themselves, beside the form it belongs to (a rejected Add, Remove,
         * Clear, Update, server change, Explore or query); on any other page such a model is only its error.
         */
        const errorFormViews: { [viewId: string]: boolean } = {
            add: true, remove: true, clear: true, update: true, server: true, explore: true, query: true
        };

        /**
         * True when a page shows its load error itself, in its own form, instead of only the error: Explore refuses a
         * resource it cannot read (400 'Malformed value: …'), and the Resource field must stay to correct it (C10).
         */
        export function showsOwnError(model: PageModel): boolean {
            return !!model && !!model.error && model.viewId === 'explore' && model.error.status === 400;
        }

        /** True for a model whose only variable is error-message (an unauthorized answer, a rejected request). */
        export function isErrorOnlyModel(model: PageModel): boolean {
            const vars = model.vars || [];
            return vars.length === 1 && vars[0] === 'error-message';
        }

        function routeBody(model: PageModel, context: ViewContext, runtime: LitRuntime): any {
            if (isRepositoryNotFound(model)) {
                return repositoryNotFoundPage(runtime, context);
            }
            if (model.error && !showsOwnError(model)) {
                // A page the router could not load (M8.1) shows its error inside the shell.
                return callout(runtime, 'error', model.error.message, 'Unable to load this Workbench page.');
            }
            if (isErrorOnlyModel(model) && !errorFormViews[model.viewId]) {
                // Its row is the error, not data: no table, picker or form is shown for it.
                return errorCallout(runtime, model)
                    || callout(runtime, 'error', 'The server did not return this page.', 'Unable to load this Workbench page.');
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
        export function repositorySwitchTarget(basePath: string, activeView: string, repositoryId: string,
                                               fallback?: string): string {
            const view = repositoryScopedViews[activeView] ? activeView : fallback || 'summary';
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
            // In ID order, as the Repositories list (C32a).
            repositories.slice().sort((left: any, right: any) => String(left.id).localeCompare(String(right.id), undefined,
                { numeric: true, sensitivity: 'base' })).forEach((repository: any) => {
                const item = document.createElement('li');
                const link = document.createElement('a');
                link.className = 'workbench-popover__option';
                link.setAttribute('href', repositorySwitchTarget(basePath, activeView, repository.id,
                    panel.getAttribute('data-workbench-switch-fallback') || 'summary'));
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

        /** True when two repository lists name the same repositories with the same titles, in the same order. */
        function sameRepositories(left: any[], right: any[]): boolean {
            return !!left && !!right && left.length === right.length
                && left.every((repository: any, index: number) => repository.id === right[index].id
                    && repository.title === right[index].title);
        }

        /**
         * Fill the repository switcher. The current repository and the page shown change with in-page navigation
         * (M8.3), and repositories are created and deleted in the page, so every opening renders the list known so far
         * at once and asks the server for the current one (one request at a time), which replaces it when it differs.
         */
        function loadRepositoryOptions(panel: any, currentId: string): Promise<void> {
            if (panel.__rdf4jRepositories) {
                renderRepositoryOptions(panel, panel.__rdf4jRepositories, currentId);
            }
            if (panel.__rdf4jRepositoriesLoading) {
                return panel.__rdf4jRepositoriesLoading;
            }
            const app: any = (workbench as any).app;
            const status = panel.querySelector('.workbench-popover__status');
            if (!app || typeof app.loadModel !== 'function' || typeof window === 'undefined') {
                return Promise.resolve();
            }
            if (status && !panel.__rdf4jRepositories) { status.textContent = 'Loading repositories…'; }
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
                panel.__rdf4jRepositoriesLoading = null;
                panel.__rdf4jRepositoriesLoaded = true;
                const shown = panel.__rdf4jRepositories;
                panel.__rdf4jRepositories = repositories;
                // An unchanged list is left alone, so keyboard focus in it stays where it is.
                if (!sameRepositories(shown, repositories)) {
                    renderRepositoryOptions(panel, repositories, currentId);
                }
            }, (error: any) => {
                panel.__rdf4jRepositoriesLoading = null;
                if (status && !panel.__rdf4jRepositories) {
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

        /** Open the menu sheet from the menu button; close it from its close button or when an item is chosen. */
        function bindMenuSheet(document: any): () => void {
            const button = document.getElementById('workbench-menu-button');
            const sheet = document.getElementById('workbench-menu-sheet');
            const close = document.getElementById('workbench-menu-close');
            if (!button || !sheet || typeof sheet.showModal !== 'function' || sheet.__rdf4jMenuSheetBound) {
                return () => {};
            }
            sheet.__rdf4jMenuSheetBound = true;
            const onOpen = () => {
                if (!sheet.open) {
                    sheet.showModal();
                    button.setAttribute('aria-expanded', 'true');
                }
            };
            const onCloseButton = () => sheet.close();
            const onClosed = () => button.setAttribute('aria-expanded', 'false');
            const onSheetClick = (event: any) => {
                const target = event.target;
                if (target === sheet || (target && target.closest && target.closest('a[href]'))) {
                    sheet.close();
                }
            };
            button.setAttribute('aria-expanded', 'false');
            button.addEventListener('click', onOpen);
            if (close) { close.addEventListener('click', onCloseButton); }
            sheet.addEventListener('close', onClosed);
            sheet.addEventListener('click', onSheetClick);
            return () => {
                button.removeEventListener('click', onOpen);
                if (close) { close.removeEventListener('click', onCloseButton); }
                sheet.removeEventListener('close', onClosed);
                sheet.removeEventListener('click', onSheetClick);
                sheet.__rdf4jMenuSheetBound = false;
            };
        }

        /**
         * Bind the context bar's repository switcher once per shell: its popover (workbench.popover from template.ts),
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
            disposers.push(popover.bind(document.getElementById('workbench-repository-switcher'),
                document.getElementById('workbench-repository-popover'),
                { onOpen: (opened: any) => { loadRepositoryOptions(opened, currentId); } }));
            disposers.push(bindMenuSheet(document));
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
        /** The page model each outlet shows, so work that finishes later renders only the page it belongs to. */
        const shownModels = new WeakMap<Element, PageModel>();

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

        /**
         * Take the current outlet out of the shell, so the next renderShell creates a new one; the router parks the
         * old one while its page is kept alive (M11.3).
         */
        export function detachOutlet(appMount: any): Element | null {
            const outlet = outletOf(appMount);
            outletsByMount.delete(appMount);
            return outlet;
        }

        /** Make outlet the shell's outlet again (a kept page shown again, M11.3). */
        export function attachOutlet(appMount: any, outlet: Element): void {
            outletsByMount.set(appMount, outlet);
            outletElements.add(outlet);
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
         * Render a page template, then lay out the result tables it holds (resultTable). The caller empties
         * regions.resultTables before it builds the template.
         */
        function renderPage(template: any, target: any, model: PageModel, regions: RowRegions, runtime: LitRuntime): void {
            runtime.render(template, target);
            renderRowRegions(regions);
            renderPageResultTables(model, regions);
        }

        /** The shell rendered last, so that it can show what changed behind the page (M13.5). */
        let lastShell: { appMount: Element; shellState: ShellState; runtime: LitRuntime } = null;

        /** Render the shell again as it was, for example when a query running out of sight ends (M13.5). */
        export function refreshShell(): void {
            if (lastShell) {
                renderShell(lastShell.appMount, lastShell.shellState, lastShell.runtime);
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
            lastShell = { appMount, shellState, runtime };
            runtime.render(shellTemplate(shellState, runtime, outlet), appMount);
            const document: any = (appMount as any).ownerDocument;
            if (document && 'title' in document) {
                document.title = shellState.title || documentTitle(shellState.viewId, shellState.context.repositoryId);
            }
            return outlet;
        }

        /** Render a route's title and page surface into the outlet. */
        export function renderOutlet(outletMount: Element, model: PageModel, context: ViewContext,
                                     runtime: LitRuntime): Element {
            shownModels.set(outletMount, model);
            const regions = prepareRowRegions(outletMount, model);
            const renderedContext = regions ? { ...context, rowRegions: regions } : context;
            if (regions) {
                regions.resultTables = {};
            }
            renderPage(outletContentTemplate(model, renderedContext, runtime, routeBody(model, renderedContext, runtime)), outletMount,
                model, regions, runtime);
            return outletMount;
        }

        /** Render a complete route: the shell into the mount and the route into its outlet. */
        export function render(mount: Element, model: PageModel, context: ViewContext,
                               runtime: LitRuntime): Element {
            if (outletElements.has(mount)) {
                renderOutlet(mount, model, context, runtime);
                return mount;
            }
            const outlet = renderShell(mount, { viewId: model.viewId, context,
                title: isRepositoryNotFound(model) ? 'Repository not found — RDF4J Workbench' : undefined }, runtime);
            if (outlet) {
                renderOutlet(outlet, model, context, runtime);
                return mount;
            }
            // Without a DOM document (unit-test fakes) the complete page renders as one template.
            const regions = prepareRowRegions(mount, model);
            const renderedContext = regions ? { ...context, rowRegions: regions } : context;
            if (regions) {
                regions.resultTables = {};
            }
            renderPage(pageTemplate(model, renderedContext, runtime), mount, model, regions, runtime);
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
            // A Types or Graphs page opened in place can keep the previous page's filter field (lit reuses the element),
            // which still shows what was typed there: it shows the filter of the page opened instead.
            if (browseLists[model.viewId] && mount && typeof mount.querySelector === 'function') {
                const filterField = mount.querySelector('#' + model.viewId + '-filter');
                if (filterField) {
                    filterField.value = text(pageValue(model, 'filter'));
                }
            }
            const hasRows = model.rowCount > 0 && model.rowStore
                && hasQuery('[data-workbench-row-table="true"], [data-workbench-row-list="true"]');
            const hasPicker = hasQuery('[data-workbench-window-picker]');
            // Counts a browse list still waits for (Graphs' default graph) are loaded without rows too.
            const countsPending = !!browseLists[model.viewId] && browseCounts(model).state === 'pending';
            // Explore reads its rows to find the resource's label, types and role groups; its statements are shown by
            // result tables that window their own rows (resultTable).
            const exploreRows = model.viewId === 'explore' && model.rowCount > 0;
            if (!targetWindow || !model.rowStore || !hasRows && !hasPicker && !countsPending && !exploreRows) {
                return Promise.resolve(loadPageCounts(mount, model, context, runtime, targetWindow));
            }
            if (hasRows && typeof HeightIndex !== 'function') {
                return Promise.reject(new Error('Measured Workbench row-window geometry is unavailable'));
            }

            let disposed = false;
            let generation = 0;
            let groupGeneration = 0;
            let heights: any = hasRows
                ? new HeightIndex(model.rowCount, model.viewId === 'saved-queries' ? 240 : 44) : null;
            let repositorySortedRows: any[][] = (model as any).repositorySortedRows || null;
            let repositorySortGeneration = 0;
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
                const savedQueries = (workbench as any).savedQueries;
                if (model.viewId === 'saved-queries' && savedQueries && savedQueries.refresh) {
                    savedQueries.refresh(mount);
                }
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

            /**
             * Buttons this binding listens to. Lit reuses the buttons for the next page of the same view, so disposing
             * removes the listeners and the marks that say a button is bound, and the next binding binds them again.
             */
            let controlDisposers: Array<() => void> = [];
            const listenOnce = (button: any, mark: string, listener: (event: any) => void) => {
                button.addEventListener('click', listener);
                button[mark] = true;
                controlDisposers.push(() => {
                    button.removeEventListener('click', listener);
                    delete button[mark];
                });
            };

            function bindExploreControls(): void {
                if (model.viewId !== 'explore') { return; }
                elements('[data-workbench-explore-action]').forEach((button: any) => {
                    if (button.__rdf4jWorkbenchExploreBound || !button.addEventListener) { return; }
                    const groupKey = button.getAttribute('data-workbench-explore-group');
                    const action = button.getAttribute('data-workbench-explore-action');
                    listenOnce(button, '__rdf4jWorkbenchExploreBound', (event: any) => {
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
                });
            }

            let rowMenuDisposers: Array<() => void> = [];
            /** Row action menus (repository list): popovers bound once per rendered row element. */
            const bindRowMenus = () => {
                const popover: any = (workbench as any).popover;
                if (!popover || typeof popover.bind !== 'function') { return; }
                elements('[data-workbench-row-menu]').forEach((button: any) => {
                    const panel = button.nextElementSibling;
                    if (panel && button.getAttribute('data-workbench-popover-bound') !== 'true') {
                        rowMenuDisposers.push(popover.bind(button, panel,
                            { onOpen: (opened: any) => placeRowMenu(button, opened) }));
                    }
                });
            };

            const renderCurrent = () => {
                render(mount, model, context, runtime);
                bindExecutionForms();
                bindPickerControls();
                bindExploreControls();
                bindRowMenus();
            };

            const renderCurrentRows = () => {
                const regions = rowRegionsByMount.get(regionKey);
                if (regions) {
                    if (regions.renderTableRows) { regions.renderTableRows(); }
                    if (regions.renderSavedRows) { regions.renderSavedRows(); }
                }
                bindExecutionForms();
                bindRowMenus();
            };

            /** A click anywhere on a repository row that is not on a control opens it through its Id link. */
            const onRepositoryRowClick = (event: any) => {
                const target = event.target;
                if (!target || !target.closest || target.closest('a, button, input, select, textarea, label, .workbench-popover')) {
                    return;
                }
                const selection = targetWindow.getSelection ? targetWindow.getSelection() : null;
                if (selection && String(selection).length) { return; }
                const row = target.closest('tr');
                const link = row ? row.querySelector('a.workbench-repository-link') : null;
                if (link) { link.click(); }
            };
            const repositoryRows = model.viewId === 'repositories' ? elements('#repositories-results')[0] : null;

            const onRepositorySortClick = (event: any) => {
                const target = event.target;
                const button = target && target.closest ? target.closest('button[data-workbench-sort]') : null;
                if (model.viewId !== 'repositories' || !button || !mount.contains(button)) { return; }
                if (event.preventDefault) { event.preventDefault(); }
                const column = button.getAttribute('data-workbench-sort');
                if (['id', 'title', 'access'].indexOf(column) < 0) { return; }
                const current = (model as any).repositorySort || { column: 'id', direction: 'ascending' };
                const direction = current && current.column === column && current.direction === 'ascending'
                    ? 'descending' : 'ascending';
                const activeSortGeneration = ++repositorySortGeneration;
                generation++;
                model.rowStore.read(0, model.rowCount).then((rows: any[][]) => {
                    if (disposed || activeSortGeneration !== repositorySortGeneration) { return; }
                    repositorySortedRows = sortedRepositoryRows(model, rows, column, direction);
                    (model as any).repositorySortedRows = repositorySortedRows;
                    (model as any).repositorySort = { column, direction };
                    heights = new HeightIndex(model.rowCount, rowHeight(model));
                    return refreshRows(false).then(() => {
                        if (disposed || activeSortGeneration !== repositorySortGeneration) { return; }
                        renderCurrent();
                        return refreshRows();
                    });
                }).then(null, (error: any) => {
                    if (!disposed && activeSortGeneration === repositorySortGeneration && typeof console !== 'undefined') {
                        console.error('Unable to sort repositories.', error);
                    }
                });
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
                    listenOnce(button, '__rdf4jWorkbenchWindowBound', click);
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

            const rowTarget = (): any => {
                const targets = elements('[data-workbench-row-table="true"], [data-workbench-row-list="true"]');
                return targets.length ? targets[0] : null;
            };
            /** How far the rows' top has scrolled past the top of the window. */
            const rowScrollTop = (target: any): number => {
                const rectangle = target.getBoundingClientRect ? target.getBoundingClientRect() : { top: 0 };
                return Math.max(0, -Number(rectangle.top || 0));
            };
            let windowScrollTop = 0;

            const refreshRows = (paint: boolean = true): Promise<void> => {
                if (!hasRows || disposed) { return Promise.resolve(); }
                const activeGeneration = ++generation;
                heights.resize(model.rowCount);
                const target = rowTarget();
                if (!target) { return Promise.resolve(); }
                const scrollTop = rowScrollTop(target);
                windowScrollTop = scrollTop;
                const viewportHeight = Math.max(1, Number(targetWindow.innerHeight) || 600);
                const range = heights.range(scrollTop, viewportHeight, 4, 80);
                const rowWindow = repositorySortedRows
                    ? Promise.resolve(repositorySortedRows.slice(range.start, range.end))
                    : model.rowStore.read(range.start, range.end - range.start);
                return rowWindow.then((rows: any[][]) => {
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

            /** Types and Graphs: request the counts once the list is on screen (M5.2). */
            const loadBrowseCounts = (): void => {
                const list = browseLists[model.viewId];
                const app: any = (workbench as any).app;
                const counts = list ? browseCounts(model) : null;
                const href = targetWindow.location && targetWindow.location.href;
                if (!counts || counts.state !== 'pending' || !href || !app || typeof app.loadModel !== 'function'
                        || typeof targetWindow.fetch !== 'function') { return; }
                const url = new URL(String(href));
                url.searchParams.set('counts', 'true');
                app.loadModel(targetWindow.fetch.bind(targetWindow), url.toString()).then((answer: PageModel) => {
                    const timedOut = answer.metadata && text(answer.metadata['counts-timed-out']) === 'true';
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
                        const previous = model.rowStore;
                        model.vars = answer.vars;
                        model.rowStore = answer.rowStore;
                        model.rowCount = answer.rowCount;
                        counts.state = 'done';
                        previous.dispose();
                        return refreshRows(false).then(() => { if (!disposed) { renderCurrent(); } });
                    }
                    return answer.rowStore.read(0, answer.rowCount).then((rows: any[][]) => {
                        answer.rowStore.dispose();
                        rows.forEach((row: any[]) => {
                            counts.values[row[0] ? ntriples(row[0]) : ''] = row[1];
                        });
                        counts.state = 'done';
                        if (!disposed) { renderCurrent(); }
                    });
                }).then(null, () => {
                    counts.state = 'failed';
                    if (!disposed) { renderCurrent(); }
                });
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
            // The Repositories list starts in ID order (C32a).
            const initialSort = model.viewId === 'repositories' && hasRows && !repositorySortedRows
                ? model.rowStore.read(0, model.rowCount).then((rows: any[][]) => {
                    if (!disposed && !repositorySortedRows) {
                        repositorySortedRows = sortedRepositoryRows(model, rows, 'id', 'ascending');
                        (model as any).repositorySortedRows = repositorySortedRows;
                        (model as any).repositorySort = { column: 'id', direction: 'ascending' };
                    }
                })
                : Promise.resolve();
            return initialPicker.then(() => initialSort).then(() => prepareExploreSummary(model)).then((summary: any) => {
                if (summary) { (model as any).exploreSummary = summary; }
                return refreshRows(false);
            }).then(() => {
                renderCurrent();
                return refreshRows();
            }).then(() => {
                bindPickerControls();
                bindExploreControls();
                loadBrowseCounts();
                if (repositoryRows && repositoryRows.addEventListener) {
                    repositoryRows.addEventListener('click', onRepositoryRowClick);
                }
                if (model.viewId === 'repositories' && mount.addEventListener) {
                    mount.addEventListener('click', onRepositorySortClick);
                }
                if (hasRows && targetWindow.addEventListener) {
                    targetWindow.addEventListener('scroll', onScroll, { passive: true });
                    targetWindow.addEventListener('resize', onResize);
                    // A scroll made while the rows were being bound reached no listener: catch up with it.
                    const target = rowTarget();
                    if (target && rowScrollTop(target) !== windowScrollTop) {
                        refresh();
                    }
                }
                return () => {
                    disposed = true;
                    generation++;
                    groupGeneration++;
                    controlDisposers.forEach((dispose) => dispose());
                    controlDisposers = [];
                    disposeExecutionForms();
                    rowMenuDisposers.forEach((dispose) => dispose());
                    rowMenuDisposers = [];
                    if (repositoryRows && repositoryRows.removeEventListener) {
                        repositoryRows.removeEventListener('click', onRepositoryRowClick);
                    }
                    if (model.viewId === 'repositories' && mount.removeEventListener) {
                        mount.removeEventListener('click', onRepositorySortClick);
                    }
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
