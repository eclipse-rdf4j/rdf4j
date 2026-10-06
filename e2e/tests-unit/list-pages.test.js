const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { spawnSync } = require('node:child_process');

const { createExploreBrowserHarness } = require('./explore-browser-harness.js');
const { createListBrowserHarness } = require('./list-browser-harness.js');

function compilePagingSource() {
    const outputDirectory = fs.mkdtempSync(path.join(os.tmpdir(), 'rdf4j-paging-source-contract-'));
    const outputPath = path.join(outputDirectory, 'paging.js');
    const sourcePath = path.resolve(__dirname,
        '../../tools/workbench/src/main/webapp/scripts/ts/paging.ts');
    const compilation = spawnSync('tsc', [
        '--target', 'ES2017',
        '--lib', 'ES2017,DOM',
        '--skipLibCheck',
        '--outFile', outputPath,
        sourcePath
    ], { encoding: 'utf8' });
    assert.equal(compilation.status, 0,
        `paging.ts must compile for its in-repo source contract:\n${compilation.stdout}${compilation.stderr}`);
    return outputPath;
}

test('export page defaults to its own preview limit instead of the Explore limit cookie', () => {
	const harness = createListBrowserHarness({
		href: 'http://localhost:8080/rdf4j-workbench/repositories/test/export'
	});
	const limitExport = harness.registerElement('select', { id: 'limit_export', value: '0' });
	const limitExplore = harness.registerElement('select', { id: 'limit_explore', value: '0' });
	harness.document.cookie = 'limit_explore=7';
	harness.document.body.appendChild(limitExport);
	harness.document.body.appendChild(limitExplore);

	harness.loadPagingScripts(['export.js']);
	harness.workbench.exportPage.mount(harness.document.body);

	assert.equal(limitExport.value, '100');
	assert.equal(limitExplore.value, '0');
});

test('export page prefers its own preview-limit query parameter', () => {
	const harness = createListBrowserHarness({
		href: 'http://localhost:8080/rdf4j-workbench/repositories/test/export?limit_export=50'
	});
	const limitExport = harness.registerElement('select', { id: 'limit_export', value: '0' });
	harness.document.cookie = 'limit_export=10; limit_explore=7';
	harness.document.body.appendChild(limitExport);

	harness.loadPagingScripts(['export.js']);
	harness.workbench.exportPage.mount(harness.document.body);

	assert.equal(limitExport.value, '50');
});

// Repeated list items are left out by the view since M12.1 (the router renders Explore pages again in place).
test('explore page leaves its lists to the view, restores limits, and renders ranges', () => {
    const harness = createExploreBrowserHarness({
        href: 'http://localhost:8080/rdf4j-workbench/repositories/test/explore?resource=http%3A%2F%2Fexample.com%2Fa&offset=2'
            + '&limit_explore=4'
    });
    const firstListWrapper = harness.registerElement('div', { id: 'wrapper-1' });
    const firstList = harness.registerElement('ul', { id: 'list-1' });
    const itemA = harness.registerElement('li', { innerHTML: 'http://example.com/a', textContent: 'http://example.com/a' });
    const itemB = harness.registerElement('li', { innerHTML: 'http://example.com/b', textContent: 'http://example.com/b' });
    const itemBDuplicate = harness.registerElement('li', { innerHTML: 'http://example.com/b', textContent: 'http://example.com/b' });
    firstList.appendChild(itemA);
    firstList.appendChild(itemB);
    firstList.appendChild(itemBDuplicate);
    firstListWrapper.appendChild(firstList);
    const surface = harness.registerElement('div', { id: 'workbench-page-surface' });
    surface.appendChild(firstListWrapper);
    harness.document.body.appendChild(surface);
    // The shell's (initially empty) repository list must not be touched.
    const popover = harness.registerElement('div', { id: 'workbench-repository-popover' });
    const shellList = harness.registerElement('ul', { id: 'workbench-repository-options' });
    popover.appendChild(shellList);
    harness.document.body.appendChild(popover);
    // The limit is the request's (ExploreServlet reads no limit_explore cookie; round-2 R10), not an older page's cookie.
    harness.document.cookie = 'limit_explore=7; total_result_count=9';

    harness.loadExploreScript();
    harness.workbench.explore.mount(harness.document.body);

    assert.equal(harness.document.getElementById('resource').value, 'http://example.com/a');
    assert.equal(harness.document.getElementById('limit_explore').value, '4');
    assert.equal(firstList.getElementsByTagName('li').length, 3, 'rendered lists are not edited');
    assert.equal(shellList.parentNode, popover, 'shell lists outside the page surface stay mounted');
    assert.equal(harness.heading.textContent, 'Explore');
    assert.equal(harness.document.getElementById('explore-resource-value').textContent, 'http://example.com/a');
    assert.equal(harness.document.getElementById('explore-result-count').textContent, 'Rows 3–6 of 9');
    assert.equal(harness.document.getElementById('explore-resource-summary').hidden, false);
});

test('explore page without a resource empties the summary a previous page filled', () => {
    const harness = createExploreBrowserHarness({
        href: 'http://localhost:8080/rdf4j-workbench/repositories/test/explore'
    });
    const resourceValue = harness.document.getElementById('explore-resource-value');
    const resultCount = harness.document.getElementById('explore-result-count');
    resourceValue.textContent = 'http://example.com/a';
    resultCount.textContent = 'Rows 1–4 of 9';

    harness.loadExploreScript();
    harness.workbench.explore.mount(harness.document.body);

    assert.equal(resourceValue.textContent, '');
    assert.equal(resultCount.textContent, '');
    assert.equal(harness.summary.hidden, true);
});

test('explore page whose summary has no spans still restores the resource', () => {
    const harness = createExploreBrowserHarness({
        href: 'http://localhost:8080/rdf4j-workbench/repositories/test/explore?resource=http%3A%2F%2Fexample.com%2Fa',
        withoutSummarySpans: true
    });

    harness.loadExploreScript();
    harness.workbench.explore.mount(harness.document.body);

    assert.equal(harness.document.getElementById('resource').value, 'http://example.com/a');
    assert.equal(harness.summary.hidden, true, 'without its spans the summary stays hidden');
});

test('paging helpers cover url, query, and cookie branches', () => {
    const harness = createListBrowserHarness({
        href: 'http://localhost:8080/rdf4j-workbench/repositories/test/tuple?offset=3&know_total=false&dup=1&dup=2'
    });
    const downloadLimit = harness.registerElement('input', { id: 'download_limit', value: '25' });
    const wbQuery = harness.registerElement('textarea', { id: 'wb-query-text', value: 'SELECT * WHERE {?s ?p ?o}' });
    const limitQuery = harness.registerElement('input', { id: 'limit_query', value: '7' });
    const showDataType = harness.document.body.getElementsByTagName('input')[2];
    const resource = harness.registerElement('div', {
        className: 'resource',
        attributes: {
            'data-longform': 'http%3A%2F%2Fexample.com%2Flong',
            'data-shortform': 'ex:short'
        }
    });
    const link = harness.registerElement('a', { textContent: 'placeholder' });
    resource.appendChild(link);
    harness.document.body.appendChild(downloadLimit);
    harness.document.body.appendChild(wbQuery);
    harness.document.body.appendChild(limitQuery);
    harness.document.body.appendChild(resource);
    harness.document.cookie = 'query=SELECT%20*; ref=cookie-ref; owner=alice; queryLn=SPARQL; infer=true; total_result_count=11; show-datatypes=false';

    harness.loadPagingScripts([]);

    const paging = harness.context.workbench.paging;
    assert.equal(paging.getOffset(), 3);
    assert.equal(paging.getQueryParameter('dup'), '2');
    assert.equal(paging.hasQueryParameter('dup'), true);
    assert.equal(paging.getQueryString('http://x/test?a=1;b=2'), 'b=2');
    assert.ok(Number.isNaN(paging.getTotalResultCount()));
    harness.document.location.href = 'http://localhost:8080/rdf4j-workbench/repositories/test/tuple?offset=3';
    assert.equal(paging.getTotalResultCount(), 11);

    const resultTotalMetadata = harness.registerElement('input', {
        id: 'workbench-total-result-count',
        value: '37'
    });
    harness.document.body.appendChild(resultTotalMetadata);
    assert.equal(paging.getTotalResultCount(), 37);
    resultTotalMetadata.value = '';
    assert.equal(paging.getTotalResultCount(), 11);

    paging.addGraphParam('Accept');
    assert.match(harness.document.location.href, /download_limit=25/);
    assert.match(harness.document.location.href, /Accept=/);

    harness.document.location.href = 'http://localhost:8080/rdf4j-workbench/repositories/test/query';
    paging.addGraphParam('Accept');
    assert.equal(harness.document.lastSubmittedForm.action, 'query');
    assert.equal(harness.document.lastSubmittedForm.formControls.find((control) => control.name === 'query').value, 'SELECT * WHERE {?s ?p ?o}');
    assert.equal(harness.document.lastSubmittedForm.formControls.find((control) => control.name === 'ref').value, 'text');
    assert.equal(harness.document.lastSubmittedForm.formControls.find((control) => control.name === 'download_limit').value, '25');

    harness.document.location.href = 'http://localhost:8080/rdf4j-workbench/repositories/test/tuple?offset=3&know_total=false';
    paging.addPagingParam('offset', 10);
    assert.match(harness.document.location.href, /offset=10/);
    assert.match(harness.document.location.href, /know_total=NaN/);
    assert.match(harness.document.location.href, /query=SELECT/);
    assert.match(harness.document.location.href, /ref=cookie-ref/);

    harness.document.location.href = 'http://localhost:8080/rdf4j-workbench/repositories/test/tuple?offset=3';
    paging.addPagingParam('offset', 8);
    assert.match(harness.document.location.href, /know_total=11/);

    harness.document.location.href = 'http://localhost:8080/rdf4j-workbench/repositories/test/query?know_total=false';
    paging.addPagingParam('offset', 4);
    assert.equal(harness.document.lastSubmittedForm.formControls.find((control) => control.name === 'offset').value, '4');
    assert.equal(harness.document.lastSubmittedForm.formControls.find((control) => control.name === 'know_total').value, 'NaN');

    harness.document.location.href = 'http://localhost:8080/rdf4j-workbench/repositories/test/query';
    paging.addPagingParam('offset', 6);
    assert.equal(harness.document.lastSubmittedForm.formControls.find((control) => control.name === 'know_total').value, '11');

    paging.nextOffset('query');
    assert.equal(harness.document.lastSubmittedForm.formControls.find((control) => control.name === 'offset').value, '7');
    paging.previousOffset('query');
    assert.equal(harness.document.lastSubmittedForm.formControls.find((control) => control.name === 'offset').value, '0');

    // Datatype tags are hidden with a page class; cells are not rewritten (plan task M5.1).
    showDataType.checked = false;
    paging.setShowDataTypesCheckboxAndSetChangeEvent();
    assert.equal(showDataType.checked, false);
    assert.equal(harness.document.body.classList.contains('workbench-hide-datatypes'), true);
    assert.equal(link.textContent, 'placeholder', 'cell text is left alone');
    showDataType.checked = true;
    showDataType.trigger('change');
    assert.equal(harness.document.body.classList.contains('workbench-hide-datatypes'), false);
    assert.match(harness.document.cookie, /show-datatypes=true/);
});

// Review fix B1: the Query page pages with Load more, so a mounted (or parked) Query page never takes over
// the shared paging buttons; paging requests and downloads stay ordinary page requests.
test('a mounted query page does not take over paging while explicit downloads remain native', () => {
    const calls = [];
    const harness = createListBrowserHarness({
        href: 'http://localhost:8080/rdf4j-workbench/repositories/test/query',
        workbench: {
            queryPage: {
                isMounted() { return true; },
                nextPage() { calls.push('next'); return true; },
                previousPage() { calls.push('previous'); return true; },
                changePageParameter(name, value) { calls.push([name, value]); return true; }
            }
        }
    });
    const limit = harness.registerElement('select', { id: 'limit_query', value: '25' });
    const downloadLimit = harness.registerElement('input', { id: 'download_limit', value: '100' });
    harness.document.body.appendChild(limit);
    harness.document.body.appendChild(downloadLimit);
    harness.runScript(compilePagingSource());

    const paging = harness.context.workbench.paging;
    paging.nextOffset('query');
    assert.equal(harness.document.lastSubmittedForm.serializeArray()
        .find((entry) => entry.name === 'offset').value, '25');
    paging.previousOffset('query');
    assert.equal(harness.document.lastSubmittedForm.serializeArray()
        .find((entry) => entry.name === 'offset').value, '0');
    paging.addPagingParam('limit_query', 50);
    assert.equal(harness.document.lastSubmittedForm.serializeArray()
        .find((entry) => entry.name === 'limit_query').value, '50');
    paging.addGraphParam('Accept');

    assert.deepEqual(calls, [], 'the query page is never asked to page');
    assert.equal(harness.document.lastSubmittedForm.action, 'query',
        'raw Accept downloads remain explicit browser POSTs');
});

test('the datatype toggle starts hidden when the cookie says so and binds only inside its page', () => {
    const harness = createListBrowserHarness({
        href: 'http://localhost:8080/rdf4j-workbench/repositories/test/explore'
    });
    harness.document.cookie = 'show-datatypes=false';
    harness.loadPagingScripts([]);
    const page = harness.registerElement('div', { id: 'page' });
    const inside = harness.registerElement('input', { type: 'checkbox', name: 'show-datatypes', checked: true });
    page.appendChild(inside);
    harness.document.body.appendChild(page);

    harness.context.workbench.paging.setShowDataTypesCheckboxAndSetChangeEvent(page);

    assert.equal(inside.checked, false);
    // Review fix B3: the class marks the page (its outlet), not the body every later page shares.
    assert.equal(page.classList.contains('workbench-hide-datatypes'), true);
    assert.equal(harness.document.body.classList.contains('workbench-hide-datatypes'), false);
    assert.equal(inside.listenerCount('change'), 1);
    inside.checked = true;
    inside.trigger('change');
    assert.equal(page.classList.contains('workbench-hide-datatypes'), false);
});

// Plan task M10.1: paging that leaves the Query page goes through the router when it runs.
test('paging links navigate in the page when the router runs, and downloads still load natively', () => {
    const navigations = [];
    const harness = createListBrowserHarness({
        href: 'http://localhost:8080/rdf4j-workbench/repositories/test/explore?resource=x&offset=0',
        workbench: { router: { isRunning: () => true, navigate: (url, options) => navigations.push([url, options.history]) } }
    });
    const accept = harness.registerElement('select', { id: 'Accept', value: 'text/turtle' });
    const limit = harness.registerElement('select', { id: 'limit_explore', value: '50' });
    harness.document.body.appendChild(accept);
    harness.document.body.appendChild(limit);
    harness.loadPagingScripts([]);
    const paging = harness.context.workbench.paging;
    const start = harness.document.location.href;

    paging.addPagingParam('offset', 10);
    paging.addGraphParam('limit_explore');
    assert.equal(navigations.length, 2);
    assert.match(navigations[0][0], /offset=10/);
    assert.equal(navigations[0][1], 'push');
    assert.match(navigations[1][0], /limit_explore=50/);
    assert.equal(harness.document.location.href, start, 'the page itself did not load anything');

    paging.addGraphParam('Accept');
    assert.equal(navigations.length, 2, 'a download is not a navigation');
    assert.match(harness.document.location.href, /Accept=text%2Fturtle/);
});
