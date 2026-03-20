const { createScriptHarness } = require('./script-harness.js');

function createListBrowserHarness(options = {}) {
    const harness = createScriptHarness(options);
    const { document, registerElement } = harness;

    const noScriptMessage = registerElement('div', { id: 'noscript-message' });
    const selectedUser = registerElement('div', { id: 'selected-user' });
    const titleHeading = registerElement('h1', { id: 'title_heading', innerHTML: 'Results (range)' });
    const nextButton = registerElement('input', { id: 'nextX', value: 'Next 10' });
    const previousButton = registerElement('input', { id: 'previousX', value: 'Previous 10' });
    const showDataTypes = registerElement('input', {
        type: 'checkbox',
        name: 'show-datatypes',
        checked: true
    });

    document.body.appendChild(noScriptMessage);
    document.body.appendChild(selectedUser);
    document.body.appendChild(titleHeading);
    document.body.appendChild(nextButton);
    document.body.appendChild(previousButton);
    document.body.appendChild(showDataTypes);

    return Object.assign({}, harness, {
        loadPagingScripts(scriptNames) {
            harness.runScript('tools/workbench/src/main/webapp/scripts/template.js');
            harness.runScript('tools/workbench/src/main/webapp/scripts/paging.js');
            scriptNames.forEach((scriptName) => {
                harness.runScript(`tools/workbench/src/main/webapp/scripts/${scriptName}`);
            });
        }
    });
}

module.exports = {
    createListBrowserHarness
};
