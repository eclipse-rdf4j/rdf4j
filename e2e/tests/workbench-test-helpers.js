const fs = require('node:fs');
const path = require('node:path');

const REPOSITORY_ROOT = path.resolve(__dirname, '../..');
const SEED_GRAPHS = {
    bsbm: {
        context: 'http://example.org/graph/bsbm',
        file: path.join(REPOSITORY_ROOT,
            'testsuites/sparql/src/main/resources/testcases-sparql-1.1/bsbm/bsbm-100.ttl')
    },
    spl: {
        context: 'http://example.org/graph/spl',
        file: path.join(REPOSITORY_ROOT, 'core/spin/src/main/resources/schema/spl.spin.ttl')
    }
};

async function typeIntoCodeMirror(page, index, value) {
    await page.locator('.CodeMirror').nth(index).click();
    await page.keyboard.press('ControlOrMeta+A');
    await page.keyboard.press('Backspace');
    await page.keyboard.type(value);
    await page.waitForFunction(([editorIndex, expectedValue]) => {
        return document.querySelectorAll('.CodeMirror')[editorIndex].CodeMirror.getValue() === expectedValue;
    }, [index, value]);
}

function workbenchBaseUrl() {
    return (process.env.RDF4J_WORKBENCH_BASE_URL || 'http://127.0.0.1:18090/rdf4j-workbench').replace(/\/+$/, '');
}

function serverBaseUrl() {
    return (process.env.RDF4J_SERVER_BASE_URL || 'http://127.0.0.1:18090/rdf4j-server').replace(/\/+$/, '');
}

/** Returns a repository id that no other spec run uses. */
function uniqueRepositoryId(prefix) {
    return `${prefix}-${process.pid}-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 6)}`;
}

function memoryRepositoryConfiguration(repositoryId, label) {
    return `@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix config: <tag:rdf4j.org,2023:config/> .
[] a config:Repository ;
   config:rep.id "${repositoryId}" ;
   rdfs:label "${label}" ;
   config:rep.impl [
      config:rep.type "openrdf:SailRepository" ;
      config:sail.impl [ config:sail.type "openrdf:MemoryStore" ; config:mem.persist false ]
   ] .`;
}

/**
 * Creates a disposable memory repository and loads the BSBM review sample into the graph
 * http://example.org/graph/bsbm (and the SPIN library schema into http://example.org/graph/spl when
 * options.graphs contains 'spl'). Specs call it in test.beforeAll and deleteRepository in test.afterAll.
 */
async function createSeededRepository(request, baseUrl, repositoryId, options = {}) {
    const graphs = options.graphs || ['bsbm'];
    const repositoryUrl = `${baseUrl.replace(/\/+$/, '')}/repositories/${encodeURIComponent(repositoryId)}`;
    const existing = await request.get(repositoryUrl);
    if (![400, 404].includes(existing.status())) {
        throw new Error(`Refusing to use non-absent disposable repository ${repositoryId}: GET ${existing.status()}`);
    }
    const created = await request.put(repositoryUrl, {
        headers: { 'Content-Type': 'text/turtle' },
        data: memoryRepositoryConfiguration(repositoryId, options.label || 'Workbench seeded fixture')
    });
    if (![200, 201, 204].includes(created.status())) {
        throw new Error(`Creating repository ${repositoryId} failed with HTTP ${created.status()}`);
    }
    for (const name of graphs) {
        const graph = SEED_GRAPHS[name];
        if (!graph) {
            throw new Error(`Unknown seed graph ${name}`);
        }
        const loaded = await request.post(
            `${repositoryUrl}/statements?context=${encodeURIComponent('<' + graph.context + '>')}`, {
                headers: { 'Content-Type': 'text/turtle' },
                data: fs.readFileSync(graph.file)
            });
        if (![200, 204].includes(loaded.status())) {
            throw new Error(`Loading ${name} into ${repositoryId} failed with HTTP ${loaded.status()}`);
        }
    }
    return repositoryUrl;
}

function repositoryPageUrl(repositoryId, view) {
    return `${workbenchBaseUrl()}/repositories/${encodeURIComponent(repositoryId)}/${view}`;
}

async function openQueryPage(page, repositoryId, options = {}) {
    if (options.viewport !== false) {
        await page.setViewportSize(options.viewport || { width: 1440, height: 900 });
    }
    await page.goto(repositoryPageUrl(repositoryId, 'query') + (options.search || ''),
        { waitUntil: 'domcontentloaded' });
    await page.locator('#query-form').waitFor({ state: 'visible', timeout: 15000 });
    await page.locator('.CodeMirror').first().waitFor({ state: 'visible' });
}

async function setQueryEditor(page, query, index = 0) {
    await page.locator('.CodeMirror').nth(index).evaluate((element, value) => {
        element.CodeMirror.setValue(value);
    }, query);
}

/** Runs a query with the Execute button and waits until the streamed result has finished. */
async function runQuery(page, query) {
    await setQueryEditor(page, query);
    await page.locator('#exec').click();
    await page.locator('#query-results').waitFor({ state: 'visible' });
    await page.waitForFunction(() => {
        const results = document.querySelector('#query-results');
        const status = results && results.querySelector('.query-result-status');
        return !!results && results.getAttribute('aria-busy') === 'false' && !!status
            && !!status.textContent && !/Receiving|Loading/.test(status.textContent);
    }, null, { timeout: 30000 });
}

/**
 * Waits until the page shows a view. Links and forms change the page without loading a document, so wait for this
 * instead of a navigation. The router marks the outlet with the view id, and as ready once the view's scripts have
 * mounted it; the URL changes in the same step, so pass options.url (a string, RegExp or predicate, as for
 * page.waitForURL) when the page already shows the same view, for example the next step of a form.
 */
async function waitForRoute(page, viewId, options = {}) {
    const timeout = options.timeout || 15000;
    if (options.url) {
        await page.waitForURL(options.url, { timeout, waitUntil: 'commit' });
    }
    await page.locator(`#workbench-outlet[data-workbench-route="${viewId}"][data-workbench-route-ready="true"]`)
        .waitFor({ state: 'attached', timeout });
}

async function deleteRepository(request, baseUrl, repositoryId) {
    const repositoryUrl = `${baseUrl.replace(/\/+$/, '')}/repositories/${encodeURIComponent(repositoryId)}`;
    const deleted = await request.delete(repositoryUrl).catch(() => null);
    return deleted ? deleted.status() : null;
}

module.exports = {
    SEED_GRAPHS,
    createSeededRepository,
    deleteRepository,
    memoryRepositoryConfiguration,
    openQueryPage,
    repositoryPageUrl,
    runQuery,
    serverBaseUrl,
    setQueryEditor,
    typeIntoCodeMirror,
    uniqueRepositoryId,
    waitForRoute,
    workbenchBaseUrl
};
