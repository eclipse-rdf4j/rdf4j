const test = require('node:test');
const assert = require('node:assert/strict');

const { createExploreBrowserHarness } = require('./explore-browser-harness.js');
const { createListBrowserHarness } = require('./list-browser-harness.js');

function option(harness, select, value, text, selected) {
    const element = harness.registerElement('option', {
        value,
        textContent: text,
        selected: !!selected,
        attributes: { value }
    });
    select.appendChild(element);
    if (selected) {
        select.value = value;
    }
    return element;
}

test('namespaces page copies selected prefix and namespace', () => {
    const harness = createListBrowserHarness();
    const select = harness.registerElement('select', { id: 'prefix-select', value: 'http://xmlns.com/foaf/0.1/' });
    const prefix = harness.registerElement('input', { id: 'prefix' });
    const namespace = harness.registerElement('input', { id: 'namespace' });
    option(harness, select, 'http://example.com/', 'ex', false);
    option(harness, select, 'http://xmlns.com/foaf/0.1/', 'foaf', true);
    [select, prefix, namespace].forEach((element) => harness.document.body.appendChild(element));

    harness.loadPagingScripts(['namespaces.js']);
    harness.context.workbench.namespaces.updatePrefix();

    assert.equal(prefix.value, 'foaf');
    assert.equal(namespace.value, 'http://xmlns.com/foaf/0.1/');
});

test('export and tuple pages prefer query params then cookies and update result headings', () => {
    const harness = createListBrowserHarness({
        href: 'http://localhost:8080/rdf4j-workbench/repositories/test/tuple?limit_query=20&offset=5&know_total=12'
    });
    const limitQuery = harness.registerElement('input', { id: 'limit_query', value: '0' });
    const limitExplore = harness.registerElement('input', { id: 'limit_explore', value: '0' });
    harness.document.getElementById('title_heading').innerHTML = 'Results (';
    harness.document.cookie = 'limit_explore=7; total_result_count=99';
    harness.document.body.appendChild(limitQuery);
    harness.document.body.appendChild(limitExplore);

    harness.loadPagingScripts(['export.js', 'tuple.js']);
    harness.runLoadHandlers();

    assert.equal(limitExplore.value, '7');
    assert.equal(limitQuery.value, '20');
    assert.equal(harness.document.getElementById('nextX').value, 'Next 20');
    assert.equal(harness.document.getElementById('previousX').value, 'Previous 20');
    assert.equal(harness.document.getElementById('previousX').disabled, false);
    assert.equal(harness.document.getElementById('nextX').disabled, true);
    assert.equal(harness.document.getElementById('title_heading').innerHTML, 'Results (6-12 of 12)');
});

test('explore page trims duplicates, restores limits, and renders ranges', () => {
    const harness = createExploreBrowserHarness({
        href: 'http://localhost:8080/rdf4j-workbench/repositories/test/explore?resource=http%3A%2F%2Fexample.com%2Fa&offset=2'
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
    harness.document.body.appendChild(firstListWrapper);
    harness.document.cookie = 'limit_explore=4; total_result_count=9';

    harness.loadExploreScript();
    harness.runLoadHandlers();

    assert.equal(harness.document.getElementById('resource').value, 'http://example.com/a');
    assert.equal(harness.document.getElementById('limit_explore').value, '4');
    assert.equal(firstList.getElementsByTagName('li').length, 1);
    assert.equal(harness.heading.textContent, 'Explore (http://example.com/a)(3-6 of 9)');
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
    paging.addLimit('query');
    assert.equal(harness.document.lastSubmittedForm.formControls.find((control) => control.name === 'limit_query').value, '7');

    showDataType.checked = false;
    paging.setShowDataTypesCheckboxAndSetChangeEvent();
    assert.equal(showDataType.checked, false);
    assert.equal(link.textContent, 'ex:short');
    showDataType.checked = true;
    showDataType.trigger('change');
    assert.equal(link.textContent, 'http://example.com/long');
    assert.match(harness.document.cookie, /show-datatypes=true/);
});
