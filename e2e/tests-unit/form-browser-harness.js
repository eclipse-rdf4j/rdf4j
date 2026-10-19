const { createScriptHarness } = require('./script-harness.js');

function createFormBrowserHarness(options = {}) {
    const harness = createScriptHarness(options);
    const { document, registerElement } = harness;

    const noScriptMessage = registerElement('div', { id: 'noscript-message' });
    const selectedUser = registerElement('div', { id: 'selected-user' });
    const titleHeading = registerElement('h1', { id: 'title_heading', innerHTML: 'Workbench' });
    document.body.appendChild(noScriptMessage);
    document.body.appendChild(selectedUser);
    document.body.appendChild(titleHeading);

    return Object.assign({}, harness, {
        loadScripts(scriptNames) {
            harness.runScript('tools/workbench/src/main/webapp/scripts/template.js');
            scriptNames.forEach((scriptName) => {
                harness.runScript(`tools/workbench/src/main/webapp/scripts/${scriptName}`);
            });
        }
    });
}

module.exports = {
    createFormBrowserHarness
};
