/// <reference path="template.ts" />
/// <reference path="jquery.d.ts" />
/// <reference path="paging.ts" />

// WARNING: Do not edit the *.js version of this file. Instead, always edit the
// corresponding *.ts source in the ts subfolder, and then invoke the
// compileTypescript.sh bash script to generate new *.js and *.js.map files.

namespace workbench {

    export namespace explore {

        const roleSections = ['outgoing', 'incoming', 'predicate', 'graph'];
        const valueSections = ['types', 'superClasses', 'subClasses', 'properties', 'superProperties',
            'subProperties', 'domain', 'range'];
        const scalarSections = ['label', 'comment'];
        const initialLimit = 10000;
        const expandedLimit = 1000000;

        /** A missing count permits expansion; a known count is compared without numerical rounding. */
        export function canExpandSection(section: any): boolean {
            if (!section || section.totalRows < initialLimit || section.expanded) { return false; }
            if (section.countStatus !== 'ready' || !/^\d+$/.test(String(section.count))) { return true; }
            const count = String(section.count).replace(/^0+(?=\d)/, '');
            const cached = String(section.totalRows);
            return count.length > cached.length || count.length === cached.length && count > cached;
        }

        /** Decimal addition without converting an RDF count to an inexact JavaScript number. */
        function addCounts(first: string, second: string): string {
            let a = first.length - 1;
            let b = second.length - 1;
            let carry = 0;
            let result = '';
            while (a >= 0 || b >= 0 || carry) {
                const sum = (a >= 0 ? Number(first.charAt(a--)) : 0)
                    + (b >= 0 ? Number(second.charAt(b--)) : 0) + carry;
                result = String(sum % 10) + result;
                carry = Math.floor(sum / 10);
            }
            return result.replace(/^0+(?=\d)/, '');
        }

        /** Without a server identity, even a certified blank cannot be compared across two responses. */
        function containsBlankNode(term: any): boolean {
            const pending = term ? [term] : [];
            while (pending.length) {
                const value = pending.pop();
                if (!value) { continue; }
                if (Array.isArray(value)) {
                    if (value[0] === 1) { return true; }
                    if (value[0] === 3) { pending.push(value[1], value[2], value[3]); }
                } else if (value.kind === 'bnode') {
                    return true;
                } else if (value.kind === 'triple') {
                    pending.push(value.subject, value.predicate, value.object);
                }
            }
            return false;
        }

        /** Keep a decoded page reserved for as long as the controller retains its rows. */
        function readOwned(store: any, start: number, count: number): Promise<any> {
            return Promise.resolve().then(() => {
                if (!store || typeof store.readOwned !== 'function') {
                    throw new Error('The Explore row store does not support owned window reads.');
                }
                return store.readOwned(start, count);
            });
        }

        /** A committed first snapshot plus a staged suffix; neither needs to be copied at commit. */
        function joinedStore(first: any, firstCount: number, second: any, secondCount: number): any {
            return {
                count: () => Promise.resolve(firstCount + secondCount),
                read: (start: number, count: number) => {
                    const firstLength = Math.max(0, Math.min(count, firstCount - start));
                    const left = firstLength ? first.read(start, firstLength) : Promise.resolve([]);
                    const rightStart = Math.max(0, start - firstCount);
                    const rightLength = count - firstLength;
                    const right = rightLength ? second.read(rightStart, rightLength) : Promise.resolve([]);
                    return Promise.all([left, right]).then((parts: any[][][]) => parts[0].concat(parts[1]));
                },
                readOwned: (start: number, count: number) => {
                    const firstLength = Math.max(0, Math.min(count, firstCount - start));
                    const rightStart = Math.max(0, start - firstCount);
                    const rightLength = count - firstLength;
                    const empty = () => ({ rows: [] as any[][], release: () => {} });
                    const left = firstLength ? readOwned(first, start, firstLength) : Promise.resolve(empty());
                    return left.then((a: any) => {
                        const right = rightLength ? readOwned(second, rightStart, rightLength) : Promise.resolve(empty());
                        return right.then((b: any) => {
                            let released = false;
                            const combined = { rows: a.rows.concat(b.rows), release: () => {
                                if (released) { return; }
                                released = true;
                                combined.rows = [];
                                a.release();
                                b.release();
                            } };
                            return combined;
                        }, (error: any) => { a.release(); throw error; });
                    });
                },
                dispose: () => Promise.all([first.dispose(), second.dispose()]).then(() => {})
            };
        }

        /** Owns the section requests, caches, count state and pending work of one Explore route. */
        export function createSectionController(options: any): any {
            const stream: any = (workbench as any).queryStream;
            const rowsQueue: any[] = [];
            const countsQueue: any[] = [];
            const active: { [id: string]: any } = Object.create(null);
            const sections: any = Object.create(null);
            let rowRunning = 0;
            let countRunning = 0;
            let disposed = false;
            let started = false;
            let sequence = 0;
            const idPrefix = 'explore-' + Date.now().toString(36) + '-' + Math.random().toString(36).slice(2) + '-';
            const base = new URL(options.url);
            const resource = typeof options.resolvedResource === 'string'
                ? options.resolvedResource : base.searchParams.get('resource') || '';
            base.search = '';
            base.searchParams.set('resource', resource);
            const budget = options.memoryBudget || (stream && typeof stream.createRowStoreBudget === 'function'
                ? stream.createRowStoreBudget(256 * 1024 * 1024) : null);
            const load = options.loadModel || (workbench as any).app.loadModel;
            const fetcher = options.fetcher || window.fetch.bind(window);
            const sectionFetcher = (url: string, init: any): Promise<any> => Promise.resolve(fetcher(url, init))
                .then((response: any) => {
                    if (options.serverIdentity && response && response.ok !== false
                            && (!response.headers || typeof response.headers.get !== 'function'
                                || response.headers.get('X-Workbench-Server-Identity') !== options.serverIdentity)) {
                        throw new Error('The RDF4J server connection changed. Reload this page to reconnect.');
                    }
                    return response;
                });
            const indexFactory = options.createIdentityIndex || ((scope: string) =>
                stream.createRowIdentityIndex(scope, budget));
            const notify = () => { if (!disposed && options.onChange) { options.onChange(sections); } };

            roleSections.concat(valueSections, scalarSections).forEach((key: string) => {
                sections[key] = { key, status: 'idle', countStatus: 'idle', count: null, countError: '',
                    rows: [], pageWindow: null, totalRows: 0, start: 0, store: null, vars: [], error: '', errorKind: '',
                    loadingMore: false, expanded: false, revision: 0, rowGeneration: 0,
                    pageGeneration: 0, countGeneration: 0 };
            });

            function pump(kind: string): void {
                const queue = kind === 'rows' ? rowsQueue : countsQueue;
                while (!disposed && queue.length && (kind === 'rows' ? rowRunning : countRunning) < 4) {
                    const job = queue.shift();
                    if (kind === 'rows') { rowRunning++; } else { countRunning++; }
                    Promise.resolve().then(job.run).then((value: any) => {
                        if (kind === 'rows') { rowRunning--; } else { countRunning--; }
                        job.resolve(value);
                        pump(kind);
                    }, (error: any) => {
                        if (kind === 'rows') { rowRunning--; } else { countRunning--; }
                        job.reject(error);
                        pump(kind);
                    });
                }
            }

            function enqueue(kind: string, run: () => Promise<any>): Promise<any> {
                return new Promise((resolve, reject) => {
                    (kind === 'rows' ? rowsQueue : countsQueue).push({ run, resolve, reject });
                    pump(kind);
                });
            }

            function cancelRequest(id: string): void {
                const request = active[id];
                if (!request) { return; }
                delete active[id];
                request.abort.abort();
                try {
                    if (options.cancel) {
                        Promise.resolve(options.cancel(id)).then(null, () => {});
                    } else {
                        const body = 'action=cancel-query&query-request-id=' + encodeURIComponent(id);
                        const headers = Object.assign({ 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
                            typeof (workbench as any).csrfHeaders === 'function'
                                ? (workbench as any).csrfHeaders(base.toString()) : {});
                        Promise.resolve(fetcher(base.toString(), { method: 'POST', credentials: 'same-origin',
                            headers, body, keepalive: true })).then(null, () => {});
                    }
                } catch (_error) {
                    // A browser leaving the document can refuse the request synchronously.
                }
            }

            function cancelSection(key: string, kind: string): void {
                Object.keys(active).forEach((id: string) => {
                    if (active[id].key === key && active[id].kind === kind) { cancelRequest(id); }
                });
            }

            function request(key: string, kind: string, loadKind?: string, scope?: string,
                             filterRows?: (rows: any[][]) => Promise<any[][]>, current?: () => boolean): Promise<any> {
                return enqueue(kind, () => {
                    if (disposed || current && !current()) { throw new Error('Explore was closed.'); }
                    const id = idPrefix + (++sequence);
                    const url = new URL(base.toString());
                    url.searchParams.set('section', key);
                    url.searchParams.set('kind', kind);
                    if (loadKind) { url.searchParams.set('load', loadKind); }
                    if (scope) { url.searchParams.set('scope', scope); }
                    if (scalarSections.indexOf(key) >= 0) {
                        (options.languages || []).forEach((language: string) => url.searchParams.append('language', language));
                    }
                    url.searchParams.set('query-request-id', id);
                    const abort = new AbortController();
                    active[id] = { abort, key, kind };
                    const maximum = scalarSections.indexOf(key) >= 0 ? 1
                        : loadKind === 'more' ? expandedLimit : initialLimit;
                    let received = 0;
                    const boundedRows = kind !== 'rows' ? undefined : (rows: any[][]) => {
                        received += rows.length;
                        if (received > maximum) {
                            throw new Error('The Explore response exceeded its ' + maximum + '-row limit.');
                        }
                        return Promise.resolve(filterRows ? filterRows(rows) : rows);
                    };
                    return Promise.resolve(load(sectionFetcher, url.toString(), abort.signal, {
                        rowStoreOptions: { memoryBudget: budget }, filterRows: boundedRows
                    })).then((answer: any) => {
                        delete active[id];
                        const invalidCount = kind === 'count' && answer && answer.rowCount !== 0;
                        const invalidSize = kind === 'rows' && answer && answer.rowCount > maximum;
                        if (disposed || answer && answer.error || invalidCount || invalidSize) {
                            const error = answer && answer.error;
                            return Promise.resolve(answer && answer.rowStore && answer.rowStore.dispose()).then(() => {
                                throw new Error(error && error.message || (invalidCount ? 'A count response contained rows.'
                                    : invalidSize ? 'The Explore response exceeded its ' + maximum + '-row limit.'
                                        : 'Explore was closed.'));
                            });
                        }
                        return answer;
                    }, (error: any) => { delete active[id]; throw error; });
                });
            }

            function showPage(section: any, start?: number): Promise<void> {
                const generation = ++section.pageGeneration;
                const size = roleSections.indexOf(section.key) >= 0 ? 100 : 40;
                const target = typeof start === 'number' ? start : section.start;
                const store = section.store;
                return Promise.resolve().then(() => readOwned(store, target, size)).then((window: any) => {
                    if (!disposed && generation === section.pageGeneration && store === section.store) {
                        const previous = section.pageWindow;
                        section.start = target;
                        section.pageWindow = window;
                        section.rows = window.rows;
                        section.revision++;
                        try { notify(); } finally { if (previous) { previous.release(); } }
                    } else {
                        window.release();
                    }
                });
            }

            function pageTo(section: any, target: number): Promise<void> {
                section.error = '';
                const reading = showPage(section, target);
                const generation = section.pageGeneration;
                return reading.then(null, (error: any) => {
                    if (!disposed && generation === section.pageGeneration) {
                        section.error = String(error.message || error);
                        section.errorKind = 'page';
                        section.pageRetryStart = target;
                        notify();
                    }
                });
            }

            function loadRows(key: string): Promise<void> {
                const section = sections[key];
                cancelSection(key, 'rows');
                const generation = ++section.rowGeneration;
                section.pageGeneration++;
                section.loadingMore = false;
                section.status = 'loading';
                section.error = '';
                section.errorKind = 'rows';
                notify();
                return request(key, 'rows', 'initial', undefined, undefined,
                    () => generation === section.rowGeneration).then((answer: any) => {
                    if (disposed || generation !== section.rowGeneration) { return answer.rowStore.dispose(); }
                    const size = roleSections.indexOf(key) >= 0 ? 100 : 40;
                    return readOwned(answer.rowStore, 0, size).then((window: any) => {
                        if (disposed || generation !== section.rowGeneration) {
                            window.release();
                            return answer.rowStore.dispose();
                        }
                        const previous = section.store;
                        const previousWindow = section.pageWindow;
                        section.store = answer.rowStore;
                        section.totalRows = answer.rowCount;
                        section.vars = answer.vars || [];
                        section.pageWindow = window;
                        section.rows = window.rows;
                        section.start = 0;
                        section.expanded = false;
                        section.status = 'ready';
                        section.revision++;
                        try { notify(); } finally { if (previousWindow) { previousWindow.release(); } }
                        return previous ? previous.dispose() : undefined;
                    }, (error: any) => Promise.resolve(answer.rowStore.dispose()).then(() => { throw error; }));
                }).then(null, (error: any) => {
                    if (!disposed && generation === section.rowGeneration) {
                        section.status = section.store ? 'ready' : 'error';
                        section.error = String(error.message || error);
                        notify();
                    }
                });
            }

            function loadCount(key: string): Promise<void> {
                const section = sections[key];
                cancelSection(key, 'count');
                const generation = ++section.countGeneration;
                section.countStatus = 'loading';
                section.countError = '';
                notify();
                const scopes = roleSections.indexOf(key) >= 0 && key !== 'graph' ? ['default', 'named'] : [null];
                return Promise.all(scopes.map((scope: string) => request(key, 'count', undefined, scope, undefined,
                    () => generation === section.countGeneration)
                    .then((answer: any) => {
                        const value = answer && answer.metadata && answer.metadata['explore-count'];
                        return Promise.resolve(answer && answer.rowStore && answer.rowStore.dispose()).then(() => {
                            if (typeof value !== 'string' || !/^\d+$/.test(value)) {
                                throw new Error('The server did not return a valid count.');
                            }
                            return value;
                        });
                    }))).then((values: string[]) => {
                    if (!disposed && generation === section.countGeneration) {
                        section.count = values.reduce((sum, value) => addCounts(sum, value), '0');
                        section.countStatus = 'ready';
                        notify();
                    }
                }, (error: any) => {
                    if (!disposed && generation === section.countGeneration) {
                        section.countStatus = 'error';
                        section.countError = String(error.message || error);
                        notify();
                    }
                });
            }

            function baseline(section: any, index: any, generation: number): Promise<boolean> {
                let unverified = false;
                const scan = (start: number): Promise<boolean> => {
                    if (disposed || generation !== section.rowGeneration) {
                        throw new Error('Explore was closed.');
                    }
                    if (start >= section.totalRows || unverified) { return Promise.resolve(unverified); }
                    return readOwned(section.store, start, Math.min(256, section.totalRows - start)).then((window: any) => {
                        const rows = window.rows;
                        if (!rows.length) { window.release(); throw new Error('The cached Explore result is incomplete.'); }
                        unverified = rows.some((row: any[]) => row.some((term: any) => stream.hasUnverifiedBlankNode(term)
                            || !options.serverIdentity && containsBlankNode(term)));
                        if (unverified) { window.release(); return 0; }
                        return Promise.resolve().then(() => index && index.add(rows)).then(() => {
                            const length = rows.length;
                            window.release();
                            return length;
                        }, (error: any) => { window.release(); throw error; });
                    }).then((length: number) => length ? scan(start + length) : true);
                };
                return scan(0);
            }

            function expand(key: string): Promise<void> {
                const section = sections[key];
                if (!section || !section.store || section.loadingMore || !canExpandSection(section)
                    || scalarSections.indexOf(key) >= 0) { return Promise.resolve(); }
                section.loadingMore = true;
                const generation = ++section.rowGeneration;
                section.error = '';
                section.errorKind = 'more';
                notify();
                let index: any = null;
                let replace = false;
                let available = expandedLimit - section.totalRows;
                return baseline(section, null, generation).then((unverified: boolean) => {
                    if (disposed || generation !== section.rowGeneration) { throw new Error('Explore was closed.'); }
                    replace = unverified;
                    if (replace) { return; }
                    return Promise.resolve(indexFactory(options.repositoryScope || '')).then((created: any) => {
                        index = created;
                        return baseline(section, index, generation).then((changed: boolean) => { replace = changed; });
                    });
                }).then(() => {
                    if (disposed || generation !== section.rowGeneration) { throw new Error('Explore was closed.'); }
                    const filter = replace ? undefined : (rows: any[][]) => Promise.resolve(index.filter(rows)).then((unseen: any[][]) => {
                        const kept = unseen.slice(0, Math.max(0, available));
                        available -= kept.length;
                        return kept;
                    });
                    return request(key, 'rows', 'more', undefined, filter, () => generation === section.rowGeneration);
                }).then((answer: any) => {
                    if (disposed || generation !== section.rowGeneration) { return answer.rowStore.dispose(); }
                    const old = section.store;
                    const candidate = replace ? answer.rowStore
                        : joinedStore(old, section.totalRows, answer.rowStore, answer.rowCount);
                    const total = replace ? answer.rowCount : section.totalRows + answer.rowCount;
                    const size = roleSections.indexOf(key) >= 0 ? 100 : 40;
                    let committed = false;
                    const prepareCommit = (): Promise<void> => {
                        const start = replace ? 0 : section.start;
                        const pageGeneration = section.pageGeneration;
                        return Promise.resolve().then(() => readOwned(candidate, start, size)).then((window: any) => {
                            if (disposed || generation !== section.rowGeneration || section.store !== old) {
                                window.release();
                                return answer.rowStore.dispose();
                            }
                            if (!replace && pageGeneration !== section.pageGeneration) {
                                window.release();
                                return prepareCommit();
                            }
                            if (window.rows.length !== Math.min(size, Math.max(0, total - start))) {
                                window.release();
                                throw new Error('The staged Explore result is incomplete.');
                            }
                            const previousWindow = section.pageWindow;
                            section.pageGeneration++;
                            section.store = candidate;
                            section.totalRows = total;
                            section.vars = answer.vars || section.vars;
                            section.pageWindow = window;
                            section.rows = window.rows;
                            section.start = start;
                            section.expanded = true;
                            section.revision++;
                            committed = true;
                            try { notify(); } finally { if (previousWindow) { previousWindow.release(); } }
                            return replace ? Promise.resolve().then(() => old.dispose()).then(null, (error: any) => {
                                section.error = String(error.message || error);
                                section.errorKind = 'cleanup';
                            }) : undefined;
                        });
                    };
                    return prepareCommit().then(null, (error: any) => {
                        if (committed) { throw error; }
                        return Promise.resolve().then(() => answer.rowStore.dispose()).then(() => { throw error; }, (cleanup: any) => {
                            throw new Error(String(error.message || error) + '; cleanup failed: ' + String(cleanup.message || cleanup));
                        });
                    });
                }).then(null, (error: any) => {
                    if (!disposed && generation === section.rowGeneration) {
                        section.error = String(error.message || error);
                        if (section.expanded) { section.errorKind = 'page'; section.pageRetryStart = section.start; }
                    }
                }).then(() => Promise.resolve().then(() => index && index.dispose()).then(null, (error: any) => {
                    if (!disposed && generation === section.rowGeneration) {
                        section.error = String(error.message || error);
                        section.errorKind = 'cleanup';
                    }
                })).then(() => {
                    if (!disposed && generation === section.rowGeneration) { section.loadingMore = false; notify(); }
                });
            }

            return {
                sections,
                start: () => {
                    if (started || disposed) { return; }
                    started = true;
                    scalarSections.concat(roleSections, valueSections).forEach(loadRows);
                    roleSections.concat(valueSections).forEach(loadCount);
                },
                page: (key: string, direction: string) => {
                    const section = sections[key];
                    if (!section || !section.store) { return Promise.resolve(); }
                    const size = roleSections.indexOf(key) >= 0 ? 100 : 40;
                    const next = section.start + (direction === 'previous' ? -size : size);
                    if (next < 0 || next >= section.totalRows) { return Promise.resolve(); }
                    return pageTo(section, next);
                },
                expand,
                retry: (key: string, kind: string) => kind === 'count' ? loadCount(key)
                    : kind === 'page' ? pageTo(sections[key], sections[key].pageRetryStart) : loadRows(key),
                dispose: () => {
                    if (disposed) { return; }
                    disposed = true;
                    rowsQueue.splice(0).forEach((job: any) => job.reject(new Error('Explore was closed.')));
                    countsQueue.splice(0).forEach((job: any) => job.reject(new Error('Explore was closed.')));
                    Object.keys(active).forEach(cancelRequest);
                    Object.keys(sections).forEach((key: string) => {
                        if (sections[key].pageWindow) {
                            sections[key].pageWindow.release();
                            sections[key].pageWindow = null;
                            sections[key].rows = [];
                        }
                        if (sections[key].store) {
                            Promise.resolve(sections[key].store.dispose()).then(null, (error: any) => {
                                if (typeof console !== 'undefined') { console.error('Unable to dispose Explore storage.', error); }
                            });
                            sections[key].store = null;
                        }
                    });
                }
            };
        }

        /** Mount independent Explore sections and release their requests and caches on navigation. */
        export function mount(outlet: HTMLElement, route: any): () => void {
            const paging: any = (workbench as any).paging;
            if (paging && typeof paging.setShowDataTypesCheckboxAndSetChangeEvent === 'function') {
                paging.setShowDataTypesCheckboxAndSetChangeEvent(outlet);
            }
            const unbindDisplay = () => {
                if (typeof $ === 'function') { $(outlet).find("input[name='show-datatypes']").off('.wbRoute'); }
            };
            if (!route || !route.model || route.model.error) { return unbindDisplay; }
            const model = route.model;
            const url = route.url ? String(route.url) : window.location.href;
            const resource = new URL(url).searchParams.get('resource') || '';
            const input = outlet.querySelector('#resource') as HTMLInputElement;
            if (input) { input.value = resource; }
            if (!resource || !model.metadata || !model.metadata['explore-resource']) { return unbindDisplay; }
            const context = route.context || {};
            const browser: any = window.navigator || {};
            const languages = browser.languages && browser.languages.length
                ? Array.prototype.slice.call(browser.languages) : browser.language ? [browser.language] : [];
            const repositoryScope = JSON.stringify([context.serverIdentity || new URL(url).origin,
                context.basePath || '', context.repositoryId || '']);
            const controller = createSectionController({ url, repositoryScope, languages,
                resolvedResource: model.metadata['explore-resource'],
                serverIdentity: context.serverIdentity,
                onChange: () => (workbench as any).views.renderExploreSections(outlet, model, context, route.runtime) });
            model.exploreController = controller;
            model.exploreSections = controller.sections;
            (workbench as any).views.renderExploreSections(outlet, model, context, route.runtime);
            controller.start();
            return () => { controller.dispose(); unbindDisplay(); };
        }

    }
}
