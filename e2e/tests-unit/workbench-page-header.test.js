const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { installDetailDisclosureTemplateRuntime } = require('./workbench-detail-disclosure-runtime.js');

const scripts = process.env.WORKBENCH_SCRIPT_DIR
    || path.resolve(__dirname, '../../tools/workbench/src/main/webapp/scripts');

function loadWorkbench() {
    const window = {};
    const workbench = {};
    installDetailDisclosureTemplateRuntime(workbench);
    const context = vm.createContext({ console, URL, URLSearchParams, Promise, window, workbench, setTimeout });
    for (const filename of ['workbenchViews.js', 'workbenchRoutes.js', 'queryStream.js', 'workbenchApp.js']) {
        vm.runInContext(fs.readFileSync(path.join(scripts, filename), 'utf8'), context, { filename });
    }
    window.workbench = context.workbench;
    return context.workbench;
}

function fakeRuntime() {
    return {
        html(strings, ...values) { return { strings: Array.from(strings), values }; },
        render(template, root) { root.template = template; },
        nothing: ''
    };
}

function flatten(template) {
    if (Array.isArray(template)) {
        return template.map(flatten).join('');
    }
    if (template && Array.isArray(template.strings)) {
        let markup = '';
        for (let index = 0; index < template.strings.length; index++) {
            markup += template.strings[index];
            if (index < template.values.length) {
                markup += flatten(template.values[index]);
            }
        }
        return markup;
    }
    return template === null || typeof template === 'undefined' ? '' : String(template);
}

function render(viewId, context, extra) {
    const workbench = loadWorkbench();
    return flatten(workbench.views.pageTemplate(Object.assign({ viewId, vars: [], rows: [], metadata: {} }, extra || {}),
        Object.assign({ basePath: '/workbench', repositoryId: 'repo-1', workbench: {} }, context || {}), fakeRuntime()));
}

/** The copy the layout preview shows under each title; one line that says what the page is for. */
const descriptions = {
    query: 'Run SPARQL queries against this repository.',
    'saved-queries': 'Run the queries saved for this repository again.',
    explore: 'Look up a resource and see every statement it appears in.',
    summary: 'See where this repository is stored, how much data it holds and how it is configured.',
    namespaces: 'Manage the prefixes this repository uses to shorten IRIs in queries and results.',
    contexts: 'See the named graphs in this repository and how many statements each holds.',
    types: 'See the classes used in this repository and how many resources belong to each.',
    add: 'Load RDF data into this repository.',
    export: 'Download all the data in this repository as an RDF file.',
    update: 'Change the data in this repository with SPARQL Update.',
    remove: 'Remove the statements that match a subject, predicate, object or graph.',
    clear: 'Remove all statements from this repository, or from one of its graphs.',
    create: 'Set up a new repository on this server.',
    delete: 'Permanently delete a repository and all of its data from the server.',
    server: 'Choose which RDF4J Server this Workbench connects to.',
    information: 'Check the Workbench version and the Java runtime, operating system and memory it runs on.'
};

test('every page renders a header with its sidebar icon, title and one-line description', () => {
    for (const viewId of Object.keys(descriptions)) {
        const markup = render(viewId);
        const header = markup.match(/<header class="workbench-page-header"[\s\S]*?<\/header>/);
        assert.ok(header, viewId + ' should render a page header');
        assert.ok(header[0].includes('class="workbench-page-header__icon"'), viewId + ' header should carry an icon tile');
        assert.ok(header[0].includes('<h1 id="title_heading" tabindex="-1">'), viewId + ' header should keep the title heading');
        assert.ok(header[0].includes('<p class="workbench-page-header__description">' + descriptions[viewId] + '</p>'),
            viewId + ' header should describe the page as: ' + descriptions[viewId]);
    }
});

test('the page header icon is the same icon the sidebar shows for that page', () => {
    const markup = render('saved-queries');
    assert.ok(/<svg class="?workbench-action-icon workbench-action-icon--saved workbench-page-header__icon-glyph"?/.test(markup),
        'the saved queries header should reuse the sidebar "saved" icon');
    const custom = render('explore', { workbench: { menu: [
        { id: 'explore', label: 'Explore', icon: 'types', groupId: 'explore', groupLabel: 'Repository', href: 'explore' }
    ] } });
    assert.ok(/workbench-action-icon--types workbench-page-header__icon-glyph/.test(custom),
        'a policy that renames a page icon should change the header icon too');
});

test('the Repositories description names the connected server', () => {
    const markup = render('repositories', { repositoryId: 'NONE', workbench: { server: 'http://127.0.0.1:18833/rdf4j-server' } });
    assert.ok(markup.includes('<p class="workbench-page-header__description">Every repository on 127.0.0.1:18833. '
        + 'Open one to work with its data.</p>'), 'the description should name the server host and port');
    const noServer = render('repositories', { repositoryId: 'NONE' });
    assert.ok(noServer.includes('<p class="workbench-page-header__description">Every repository on this server. '
        + 'Open one to work with its data.</p>'), 'without a server address the description stays generic');
});

test('the repository-not-found page shows no icon tile or description', () => {
    const markup = render('query', { missingRepositoryId: 'nope' }, { error: { code: 'repository-not-found' } });
    assert.ok(markup.includes('<h1 id="title_heading" tabindex="-1">Repository not found</h1>'));
    assert.ok(!markup.includes('workbench-page-header__description'), 'the not-found page has no description');
});
