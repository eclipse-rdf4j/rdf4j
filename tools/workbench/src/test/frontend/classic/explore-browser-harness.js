const { createListBrowserHarness } = require('./list-browser-harness.js');
const { createExploreModel } = require('./explore-section-fixtures.js');

function createExploreBrowserHarness(options = {}) {
    const harness = createListBrowserHarness(options);
    const { document, registerElement } = harness;

    const content = registerElement('main', { id: 'workbench-outlet', className: 'workbench-outlet' });
    const heading = registerElement('h1', { textContent: 'Explore' });
    const form = registerElement('form', { id: 'explore-form' });
    const resource = registerElement('input', { id: 'resource', value: '', name: 'resource' });
    const sectionOutput = registerElement('div', { id: 'explore-section-output' });
    content.appendChild(heading);
    form.appendChild(resource);
    content.appendChild(form);
    content.appendChild(sectionOutput);
    document.body.appendChild(content);

    return Object.assign({}, harness, {
        content,
        heading,
        form,
        resource,
        sectionOutput,
        loadExploreScript() {
            harness.loadPagingScripts(['explore.js']);
            harness.context.AbortController = AbortController;
        },
        mountExplore(model = createExploreModel({ metadata: {} }), context = {}) {
            const route = { model, context, url: new URL(document.location.href) };
            return harness.workbench.explore.mount(content, route);
        }
    });
}

module.exports = {
    createExploreBrowserHarness
};
