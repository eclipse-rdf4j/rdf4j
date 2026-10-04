// @ts-check
const {test, expect} = require('@playwright/test');
const {
    deleteRepository,
    repositoryPageUrl,
    serverBaseUrl,
    typeIntoCodeMirror,
    uniqueRepositoryId,
    waitForRoute,
    waitForWriteDone,
    workbenchBaseUrl
} = require('./workbench-test-helpers');

const WORKBENCH_URL = `${workbenchBaseUrl()}/`;

// Every test creates its own repository through the Workbench and deletes it afterwards.
let repositoryId = '';

test.beforeEach(async ({page}) => {
    repositoryId = uniqueRepositoryId('workbench-ui');
    await page.goto(WORKBENCH_URL);
});

test.afterEach(async ({request}) => {
    await deleteRepository(request, serverBaseUrl(), repositoryId);
});

test('RDF4J Workbench has correct title', async ({page}) => {
    await page.goto(WORKBENCH_URL);

    // The tab title is "<page> — RDF4J Workbench" (plan task M2.6); the Workbench opens on the repository list.
    await expect(page).toHaveTitle('Repositories — RDF4J Workbench');
});

// The menu item is "Create repository" (plan task M2.4). Its first step chooses the type (Memory Store by default);
// the repository id is asked on the next step, whose button is #create.
async function createRepo(page) {
    await page.getByRole('link', {name: 'Create repository'}).click();
    await waitForRoute(page, 'create');
    await page.getByText('Next').click();
    await waitForRoute(page, 'create', {url: url => url.searchParams.has('type')});

    await page.getByRole('textbox', {name: 'Repository ID'}).fill(repositoryId);
    await page.locator('#create').click();
    await waitForRoute(page, 'summary');
    // The Summary card's first section is "Repository" (plan task M5.4).
    await expect(page.locator('#workbench-summary h2').first()).toHaveText('Repository');
    await expect(page).toHaveURL(repositoryPageUrl(repositoryId, 'summary'));
}

async function delayExplainRequests(page, delayMs = 2200) {
    await page.route(repositoryPageUrl(repositoryId, 'query'), async route => {
        const request = route.request();
        if (request.method() === 'POST' && request.postData()?.includes('action=explain')) {
            await new Promise(resolve => setTimeout(resolve, delayMs));
        }
        await route.continue();
    });
}

async function trackExplainTraffic(page, delayMs = 2200) {
    const explainRequests = [];
    const cancelRequests = [];

    await page.route(repositoryPageUrl(repositoryId, 'query'), async route => {
        const request = route.request();
        if (request.method() !== 'POST') {
            await route.continue();
            return;
        }

        const params = new URLSearchParams(request.postData() || '');
        const action = params.get('action');

        if (action === 'explain') {
            explainRequests.push({
                requestId: params.get('explain-request-id'),
                query: params.get('query'),
                level: params.get('explain'),
                format: params.get('explain-format')
            });
            await new Promise(resolve => setTimeout(resolve, delayMs));
        } else if (action === 'cancel-explain') {
            cancelRequests.push({
                requestId: params.get('explain-request-id')
            });
        }

        await route.continue();
    });

    return { explainRequests, cancelRequests };
}

function getTrackedRequestIds(requests) {
    return [...new Set(requests.map(request => request.requestId).filter(Boolean))].sort();
}

const LOADING_EXPLANATION_TEXT = 'Loading explanation...';

async function waitForStableExplanation(page, selector = '#query-explanation') {
    await page.waitForFunction(({ selector, loadingText }) => {
        const explanation = document.querySelector(selector);
        const text = explanation && explanation.textContent.trim();
        return text && text.length > 0 && text !== loadingText;
    }, { selector, loadingText: LOADING_EXPLANATION_TEXT });

    return (await page.locator(selector).textContent()).trim();
}

async function waitForChangedStableExplanation(page, selector, previousExplanation) {
    await page.waitForFunction(({ selector, loadingText, previousExplanation }) => {
        const explanation = document.querySelector(selector);
        const text = explanation && explanation.textContent.trim();
        return text && text.length > 0 && text !== loadingText && text !== previousExplanation;
    }, { selector, loadingText: LOADING_EXPLANATION_TEXT, previousExplanation });

    return (await page.locator(selector).textContent()).trim();
}

async function waitForStableExplanations(page, selectors) {
    for (const selector of selectors) {
        await waitForStableExplanation(page, selector);
    }
}

test('Create repo', async ({page}) => {
    await page.goto(WORKBENCH_URL);
    page.on('dialog', dialog => {
        console.log(dialog.message());
        dialog.dismiss();
    });

    await page.getByRole('link', {name: 'Create repository'}).click();
    await waitForRoute(page, 'create');
    await page.getByText('Next').click();
    await waitForRoute(page, 'create', {url: url => url.searchParams.has('type')});

    await page.getByRole('textbox', {name: 'Repository ID'}).fill(repositoryId);
    await page.locator('#create').click();
    await waitForRoute(page, 'summary');
    // The Summary card's first section is "Repository" (plan task M5.4).
    await expect(page.locator('#workbench-summary h2').first()).toHaveText('Repository');
    await expect(page).toHaveURL(repositoryPageUrl(repositoryId, 'summary'));
});


test('SPARQL update', async ({page}) => {
    await page.goto(WORKBENCH_URL);
    page.on('dialog', dialog => {
        console.log(dialog.message());
        dialog.dismiss();
    });

    await createRepo(page);

    await page.getByRole('link', {name: 'SPARQL Update', exact: true}).click();
    await page.waitForSelector('.CodeMirror > div > textarea');

    // magic code that lets us type in the CodeMirror editor
    await page.evaluate(() => {
        let CM = document.getElementsByClassName("CodeMirror")[0];
        CM.CodeMirror.setValue("INSERT DATA {\n" +
            "\t<http://exampleSub> a <http://exampletype> .\n" +
            "}");
    });


    await page.getByRole('button', { name: 'Execute' }).click();
    // The update stays on its page and shows a tick when it is done (plan task M14.2).
    await waitForWriteDone(page, 'update');

    await page.getByRole('link', {name: 'Types', exact: true}).click();
    await waitForRoute(page, 'types');

    // Types writes an IRI without a known prefix in full, without angle brackets (plan task M5.2).
    let type = await page.getByText('http://exampletype', {exact: true});
    await expect(type).toHaveText('http://exampletype');

});


test('Add Turtle data to repository', async ({page}) => {
    await page.goto(WORKBENCH_URL);
    page.on('dialog', dialog => {
        console.log(dialog.message());
        dialog.dismiss();
    });

    await createRepo(page);

    await page.getByRole('link', {name: 'Add RDF', exact: true}).click();
    await waitForRoute(page, 'add');

    // The source is a segmented control (plan task M5.5). The base URI sits under the import settings, which open over
    // the form and its buttons (plan task M14.3), so they are closed again before the upload.
    await page.locator('label[for="source-text"]').click();
    await expect(page.locator('#source-text')).toBeChecked();
    await page.locator('#add-import-settings-toggle').click();
    await page.locator('#baseURI').fill('http://example.org/ns#');
    await page.locator('#add-import-settings-toggle').click();
    await page.locator('#Content-Type').selectOption('text/turtle');

    const turtleData = '@prefix ex: <http://example.org/ns#> .\n\n' +
        'ex:alice a ex:Person ;\n' +
        '        ex:name "Alice" .';

    await page.locator('#text').fill(turtleData);

    await page.getByRole('button', { name: 'Upload' }).click();
    // Add stays on its page and shows a tick when it is done (plan task M14.2).
    await waitForWriteDone(page, 'add');

    await page.getByRole('link', {name: 'Types', exact: true}).click();
    await waitForRoute(page, 'types');

    let type = await page.getByText('ex:Person');
    await expect(type).toHaveText('ex:Person');
});

test('Query page keeps separate drafts per browser page on refresh', async ({page}) => {
    const queryUrl = repositoryPageUrl(repositoryId, 'query');
    const queryA = 'SELECT * WHERE { ?s ?p ?o } LIMIT 1';
    const queryB = 'ASK { ?s ?p ?o }';

    page.on('dialog', dialog => {
        dialog.dismiss();
    });

    await createRepo(page);

    const page2 = await page.context().newPage();
    page2.on('dialog', dialog => {
        dialog.dismiss();
    });

    try {
        await page.goto(queryUrl);
        await page2.goto(queryUrl);
        await page.waitForSelector('.CodeMirror');
        await page2.waitForSelector('.CodeMirror');

        await typeIntoCodeMirror(page, 0, queryA);
        await typeIntoCodeMirror(page2, 0, queryB);

        await page.reload();
        await page.waitForSelector('.CodeMirror');
        await expect.poll(async () => page.evaluate(() => {
            return document.querySelector('.CodeMirror').CodeMirror.getValue();
        })).toBe(queryA);

        await page2.reload();
        await page2.waitForSelector('.CodeMirror');
        await expect.poll(async () => page2.evaluate(() => {
            return document.querySelector('.CodeMirror').CodeMirror.getValue();
        })).toBe(queryB);
    } finally {
        await page2.close();
    }
});

test('Query compare mode diffs query and explanation', async ({page}) => {
    await page.goto(WORKBENCH_URL);
    page.on('dialog', dialog => {
        console.log(dialog.message());
        dialog.dismiss();
    });

    await createRepo(page);
    await page.goto(repositoryPageUrl(repositoryId, 'query'));
    await page.waitForSelector('.CodeMirror');
    await expect(page.locator('#query-explanation-panel')).not.toHaveAttribute('aria-busy', 'true');
    await expect(page.locator('#explain-trigger-cancel')).toBeHidden();
    await expect(page.locator('#compare-toggle')).toBeHidden();
    await expect(page.locator('#query-sidebar-toggle')).toBeHidden();

    await page.evaluate(() => {
        const editors = document.getElementsByClassName('CodeMirror');
        editors[0].CodeMirror.setValue('SELECT * WHERE { ?s ?p ?o } LIMIT 10');
    });

    await page.locator('#explain-trigger').click();
    await waitForStableExplanation(page);
    await expect(page.locator('#compare-toggle')).toBeVisible();
    await expect.poll(async () => page.evaluate(() => {
        const compareButton = document.getElementById('compare-toggle');
        const downloadButton = document.getElementById('download-explanation');
        return compareButton
            && downloadButton
            && compareButton.parentElement === downloadButton.parentElement
            && downloadButton.nextElementSibling === compareButton;
    })).toBeTruthy();

    await page.locator('#compare-toggle').click();
    await page.waitForFunction(() => document.querySelectorAll('.CodeMirror').length === 2);
    await expect(page.locator('#navigation')).toBeHidden();
    await expect(page.locator('#query-sidebar-toggle')).toBeVisible();
    await expect(page.locator('#explain-trigger')).toBeVisible();
    await expect(page.locator('#rerun-explanation')).toBeVisible();
    await expect(page.locator('#compare-explain-format')).toHaveCount(0);
    await expect(page.locator('#compare-explain-level')).toHaveCount(0);
    // Explain and Explain again explain both queries in compare mode, so there is no "Refresh explanations" button.
    // Swap and Diff are named by their visible text and also follow Explain under the editors.
    await expect(page.locator('#explain-compare-trigger')).toHaveCount(0);
    await expect(page.locator('#query-diff-trigger')).toHaveAccessibleName(/Diff/i);
    await expect(page.locator('#query-actions-swap')).toHaveAccessibleName(/Swap/i);
    await expect(page.locator('#query-actions-diff')).toHaveAccessibleName(/Diff/i);
    await expect.poll(async () => page.evaluate(() => {
        const compareCode = document.querySelectorAll('.CodeMirror-code')[1];
        return compareCode ? compareCode.textContent.replace(/\s+/g, ' ').trim() : '';
    })).toContain('SELECT * WHERE { ?s ?p ?o } LIMIT 10');
    await waitForStableExplanation(page, '#query-explanation-compare');

    // In compare mode the menu opens over the page below its toggle and moves nothing; that geometry is checked by
    // workbench-visual-refinement-followup.spec.js ("compare mode collapses and restores navigation ...").
    await page.locator('#query-sidebar-toggle').click();
    await expect(page.locator('#navigation')).toBeVisible();

    await expect.poll(async () => page.evaluate(() => {
        return document.querySelectorAll('.CodeMirror')[1].CodeMirror.getValue();
    })).toBe('SELECT * WHERE { ?s ?p ?o } LIMIT 10');

    await page.evaluate(() => {
        const editors = document.querySelectorAll('.CodeMirror');
        editors[1].CodeMirror.setValue('ASK { ?s ?p ?o }');
    });

    await page.locator('#explain-trigger').click();
    await waitForStableExplanations(page, ['#query-explanation', '#query-explanation-compare']);

    await page.locator('#query-diff-trigger').click();
    await expect(page.locator('#query-diff-modal')).toHaveClass(/query-diff-modal--open/);
    await expect(page.locator('#query-diff-query .query-diff-row').first()).toBeVisible();
    await expect(page.locator('#query-diff-explanation .query-diff-row').first()).toBeVisible();

    await page.keyboard.press('Escape');
    await expect(page.locator('#query-diff-modal')).not.toHaveClass(/query-diff-modal--open/);
});

test('Explain wait state shows the panel loading bar and cancel for primary and compare actions', async ({page}) => {
    await page.goto(WORKBENCH_URL);
    page.on('dialog', dialog => {
        console.log(dialog.message());
        dialog.dismiss();
    });

    await createRepo(page);
    await page.goto(repositoryPageUrl(repositoryId, 'query'));
    await page.waitForSelector('.CodeMirror');
    await delayExplainRequests(page);

    await typeIntoCodeMirror(page, 0, 'SELECT * WHERE { ?s ?p ?o } LIMIT 10');

    const explanationPanel = page.locator('#query-explanation-panel');
    await page.locator('#explain-trigger').click();
    await expect(explanationPanel).toHaveAttribute('aria-busy', 'true');
    await expect(page.locator('#explain-trigger-cancel')).toBeVisible();
    await expect(page.locator('#explain-trigger-cancel')).toHaveAccessibleName('Cancel explanation');
    // The Explanation toolbar repeats Cancel right after its Config dropdown.
    await expect(page.locator('#explanation-cancel')).toBeVisible();
    await expect(page.locator('#explanation-cancel')).toHaveAccessibleName('Cancel explanation');
    await expect.poll(() => explanationPanel.evaluate(panel => getComputedStyle(panel, '::before').opacity))
        .toBe('1');
    await waitForStableExplanation(page);
    await expect(explanationPanel).toHaveAttribute('aria-busy', 'false');

    await page.locator('#rerun-explanation').click();
    await expect(explanationPanel).toHaveAttribute('aria-busy', 'true');
    await expect(page.locator('#rerun-explanation-cancel')).toBeVisible();
    await waitForStableExplanation(page);

    await page.locator('#compare-toggle').click();
    await page.waitForFunction(() => document.querySelectorAll('.CodeMirror').length === 2);
    await typeIntoCodeMirror(page, 1, 'ASK { ?s ?p ?o }');
    await page.locator('#explain-trigger').click();
    await expect(explanationPanel).toHaveAttribute('aria-busy', 'true');
    await expect(page.locator('#explain-trigger-cancel')).toBeVisible();
    await expect(page.locator('#explanation-cancel')).toBeVisible();
    await waitForStableExplanation(page, '#query-explanation-compare');

    await page.locator('#rerun-explanation').click();
    await expect(explanationPanel).toHaveAttribute('aria-busy', 'true');
    await expect(page.locator('#rerun-explanation-cancel')).toBeVisible();
    await expect(page.locator('#explanation-cancel')).toBeVisible();
    await waitForStableExplanation(page, '#query-explanation-compare');
});

test('Primary cancel posts matching request id and stale responses do not repaint', async ({page}) => {
    await page.goto(WORKBENCH_URL);
    page.on('dialog', dialog => {
        console.log(dialog.message());
        dialog.dismiss();
    });

    await createRepo(page);
    await page.goto(repositoryPageUrl(repositoryId, 'query'));
    await page.waitForSelector('.CodeMirror');

    await typeIntoCodeMirror(page, 0, 'SELECT * WHERE { ?s ?p ?o } LIMIT 10');
    await page.locator('#explain-trigger').click();
    const initialExplanation = await waitForStableExplanation(page);

    const traffic = await trackExplainTraffic(page);
    await page.locator('#explain-format').selectOption('json');
    await page.locator('#explain-trigger').click();

    await expect(page.locator('#explain-trigger-cancel')).toBeVisible();
    await expect.poll(() => getTrackedRequestIds(traffic.explainRequests).length).toBe(1);

    await page.locator('#explain-trigger-cancel').click();

    await expect.poll(() => getTrackedRequestIds(traffic.cancelRequests).length).toBe(1);
    await expect(getTrackedRequestIds(traffic.cancelRequests)).toEqual(getTrackedRequestIds(traffic.explainRequests));

    await page.waitForTimeout(2500);
    await expect.poll(async () => page.evaluate(() => {
        return document.getElementById('query-explanation').textContent.trim();
    })).toBe(initialExplanation);
});

test('Compare-mode left cancel buttons abort explanation refresh', async ({page}) => {
    await page.goto(WORKBENCH_URL);
    page.on('dialog', dialog => {
        console.log(dialog.message());
        dialog.dismiss();
    });

    await createRepo(page);
    await page.goto(repositoryPageUrl(repositoryId, 'query'));
    await page.waitForSelector('.CodeMirror');

    await typeIntoCodeMirror(page, 0, 'SELECT * WHERE { ?s ?p ?o } LIMIT 10');
    await page.locator('#explain-trigger').click();
    await waitForStableExplanation(page);

    await page.locator('#compare-toggle').click();
    await page.waitForFunction(() => document.querySelectorAll('.CodeMirror').length === 2);
    await typeIntoCodeMirror(page, 1, 'ASK { ?s ?p ?o }');
    const initialCompareExplanation = await waitForStableExplanation(page, '#query-explanation-compare');

    const traffic = await trackExplainTraffic(page);
    await page.locator('#explain-trigger').click();
    await expect(page.locator('#explain-trigger-cancel')).toBeVisible();
    await expect(page.locator('#explanation-cancel')).toBeVisible();
    await expect.poll(() => getTrackedRequestIds(traffic.explainRequests).length).toBe(2);
    await page.locator('#explain-trigger-cancel').click();
    await expect.poll(() => getTrackedRequestIds(traffic.cancelRequests).length).toBe(2);
    await expect(getTrackedRequestIds(traffic.cancelRequests)).toEqual(getTrackedRequestIds(traffic.explainRequests));
    await expect(page.locator('#explain-trigger-cancel')).toBeHidden();
    await expect(page.locator('#explanation-cancel')).toBeHidden();
    await page.waitForTimeout(2500);
    await expect.poll(async () => page.evaluate(() => {
        return document.getElementById('query-explanation-compare').textContent.trim();
    })).toBe(initialCompareExplanation);

    await page.locator('#explain-trigger').click();
    const refreshedCompareExplanation = await waitForChangedStableExplanation(
        page, '#query-explanation-compare', initialCompareExplanation
    );

    await page.locator('#rerun-explanation').click();
    await expect(page.locator('#rerun-explanation-cancel')).toBeVisible();
    await expect(page.locator('#explanation-cancel')).toBeVisible();
    await page.locator('#explanation-cancel').click();
    await expect(page.locator('#rerun-explanation-cancel')).toBeHidden();
    await expect(page.locator('#explanation-cancel')).toBeHidden();
    await page.waitForTimeout(2500);
    await expect.poll(async () => page.evaluate(() => {
        return document.getElementById('query-explanation-compare').textContent.trim();
    })).toBe(refreshedCompareExplanation);
});

test('Changing explain level implicitly cancels pending explain with matching request id', async ({page}) => {
    await page.goto(WORKBENCH_URL);
    page.on('dialog', dialog => {
        console.log(dialog.message());
        dialog.dismiss();
    });

    await createRepo(page);
    await page.goto(repositoryPageUrl(repositoryId, 'query'));
    await page.waitForSelector('.CodeMirror');

    await typeIntoCodeMirror(page, 0, 'SELECT * WHERE { ?s ?p ?o } LIMIT 10');
    await page.locator('#explain-trigger').click();
    const initialExplanation = await waitForStableExplanation(page);

    const traffic = await trackExplainTraffic(page);
    await page.locator('#explain-format').selectOption('json');
    await page.locator('#explain-trigger').click();

    await expect(page.locator('#explain-trigger-cancel')).toBeVisible();
    await expect.poll(() => getTrackedRequestIds(traffic.explainRequests).length).toBe(1);

    await page.locator('#explain-level').selectOption('Executed');

    await expect.poll(() => getTrackedRequestIds(traffic.cancelRequests).length).toBe(1);
    await expect(getTrackedRequestIds(traffic.cancelRequests)).toEqual(getTrackedRequestIds(traffic.explainRequests));
    await expect(page.locator('#explain-trigger-cancel')).toBeHidden();

    await page.waitForTimeout(2500);
    await expect.poll(async () => page.evaluate(() => {
        return document.getElementById('query-explanation').textContent.trim();
    })).toBe(initialExplanation);
});

test('Query compare mode keeps primary query on reload without persisting secondary query', async ({page}) => {
    await page.goto(WORKBENCH_URL);
    page.on('dialog', dialog => {
        console.log(dialog.message());
        dialog.dismiss();
    });

    await createRepo(page);
    await page.goto(repositoryPageUrl(repositoryId, 'query'));
    await page.waitForSelector('.CodeMirror');

    const primaryQuery = 'SELECT * WHERE { ?s ?p ?o } LIMIT 10';
    const updatedPrimaryQuery = 'SELECT * WHERE { ?s ?p ?o } LIMIT 5';
    const compareQuery = 'ASK { ?s ?p ?o }';

    await typeIntoCodeMirror(page, 0, primaryQuery);

    await page.locator('#explain-trigger').click();
    await waitForStableExplanation(page);

    await page.locator('#compare-toggle').click();
    await page.waitForFunction(() => document.querySelectorAll('.CodeMirror').length === 2);
    await typeIntoCodeMirror(page, 1, compareQuery);

    await typeIntoCodeMirror(page, 0, updatedPrimaryQuery);

    await page.reload();
    await page.waitForSelector('.CodeMirror');
    await expect.poll(async () => page.evaluate(() => {
        return document.querySelectorAll('.CodeMirror')[0].CodeMirror.getValue();
    })).toBe(updatedPrimaryQuery);

    await expect.poll(async () => page.evaluate(query => {
        const storages = [window.localStorage, window.sessionStorage];
        return storages.some(storage => {
            for (let index = 0; index < storage.length; index++) {
                const value = storage.getItem(storage.key(index));
                if (value && value.includes(query)) {
                    return true;
                }
            }
            return false;
        });
    }, compareQuery)).toBe(false);
});

test('Query page keeps explanation panes hidden until explain runs', async ({page}) => {
    await page.goto(WORKBENCH_URL);
    page.on('dialog', dialog => {
        console.log(dialog.message());
        dialog.dismiss();
    });

    await createRepo(page);
    await page.goto(repositoryPageUrl(repositoryId, 'query'));
    await page.waitForSelector('.CodeMirror');

    await expect(page.locator('#query-explanation-row')).toBeHidden();
    await expect(page.locator('#query-explanation-row-compare')).toBeHidden();
});
