const { createListBrowserHarness } = require('./list-browser-harness.js');

function createExploreBrowserHarness(options = {}) {
    const harness = createListBrowserHarness(options);
    const { document, registerElement } = harness;

    const content = registerElement('div', { id: 'content' });
    const heading = registerElement('h1', { textContent: 'Explore' });
    const summary = registerElement('p', { id: 'explore-resource-summary', hidden: true });
    const resource = registerElement('input', { id: 'resource', value: '' });
    const limit = registerElement('input', { id: 'limit_explore', value: '10' });
    content.appendChild(heading);
    // options.withoutSummarySpans: a page whose summary has no resource and range spans.
    if (!options.withoutSummarySpans) {
        summary.appendChild(registerElement('span', { id: 'explore-resource-value' }));
        summary.appendChild(registerElement('span', { id: 'explore-result-count' }));
    }
    content.appendChild(summary);
    document.body.appendChild(content);
    document.body.appendChild(resource);
    document.body.appendChild(limit);

    return Object.assign({}, harness, {
        content,
        heading,
        summary,
        loadExploreScript() {
            harness.loadPagingScripts(['explore.js']);
        }
    });
}

module.exports = {
    createExploreBrowserHarness
};
