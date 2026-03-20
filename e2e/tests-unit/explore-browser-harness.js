const { createListBrowserHarness } = require('./list-browser-harness.js');

function createExploreBrowserHarness(options = {}) {
    const harness = createListBrowserHarness(options);
    const { document, registerElement } = harness;

    const content = registerElement('div', { id: 'content' });
    const heading = registerElement('h1', { textContent: 'Explore' });
    const resource = registerElement('input', { id: 'resource', value: '' });
    const limit = registerElement('input', { id: 'limit_explore', value: '10' });
    content.appendChild(heading);
    document.body.appendChild(content);
    document.body.appendChild(resource);
    document.body.appendChild(limit);

    return Object.assign({}, harness, {
        content,
        heading,
        loadExploreScript() {
            harness.loadPagingScripts(['explore.js']);
        }
    });
}

module.exports = {
    createExploreBrowserHarness
};
