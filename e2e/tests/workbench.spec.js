// @ts-check
const {test, expect} = require('@playwright/test');
const { typeIntoCodeMirror } = require('./workbench-test-helpers');

test.beforeEach(async ({page}, testInfo) => {
    await page.goto('http://localhost:8080/rdf4j-workbench/');
    await page.getByText("Delete repository").click();
    await page.waitForSelector('#id');
    const optionExists = await page.locator('#id option[value="testrepo1"]').count() > 0;

    if (optionExists) {
        await page.locator("#id").selectOption("testrepo1");
        await page.getByRole('button', {name: 'Delete'}).click();
        await page.getByText("List of Repositories").click();
    }
});

test('RDF4J Workbench has correct title', async ({page}) => {
    await page.goto('http://localhost:8080/rdf4j-workbench/');

    // Expect a title "to contain" a substring.
    await expect(page).toHaveTitle("RDF4J Workbench - List of Repositories");
});

async function createRepo(page) {
    await page.getByText('New repository').click();

    await page.locator("#id").fill('testrepo1');
    await page.getByText('Next').click();

    await page.getByText('Create').click();
    let titleHeading = await page.getByText('Repository Location');
    await expect(titleHeading).toHaveText('Repository Location')

}

async function delayExplainRequests(page, delayMs = 2200) {
    await page.route('**/rdf4j-workbench/repositories/testrepo1/query', async route => {
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

    await page.route('**/rdf4j-workbench/repositories/testrepo1/query', async route => {
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

test('Create repo', async ({page}) => {
    await page.goto('http://localhost:8080/rdf4j-workbench/');
    page.on('dialog', dialog => {
        console.log(dialog.message());
        dialog.dismiss();
    });

    await page.getByText('New repository').click();

    await page.locator("#id").fill('testrepo1');
    await page.getByText('Next').click();

    await page.getByText('Create').click();
    let titleHeading = await page.getByText('Repository Location');
    await expect(titleHeading).toHaveText('Repository Location')

});


test('SPARQL update', async ({page}) => {
    await page.goto('http://localhost:8080/rdf4j-workbench/');
    page.on('dialog', dialog => {
        console.log(dialog.message());
        dialog.dismiss();
    });

    await createRepo(page);

    await page.getByText('SPARQL Update').click();
    await page.waitForSelector('.CodeMirror > div > textarea');

    // magic code that lets us type in the CodeMirror editor
    await page.evaluate(() => {
        let CM = document.getElementsByClassName("CodeMirror")[0];
        CM.CodeMirror.setValue("INSERT DATA {\n" +
            "\t<http://exampleSub> a <http://exampletype> .\n" +
            "}");
    });


    await page.getByRole('button', { name: 'Execute' }).click();
    await page.waitForSelector('table');

    await page.getByText('Types').click();

    let type = await page.getByText('<http://exampletype>');
    await expect(type).toHaveText('<http://exampletype>');

});


test('Add Turtle data to repository', async ({page}) => {
    await page.goto('http://localhost:8080/rdf4j-workbench/');
    page.on('dialog', dialog => {
        console.log(dialog.message());
        dialog.dismiss();
    });

    await createRepo(page);

    await page.getByText('Add').click();
    await page.waitForSelector('#text');

    await page.locator('#source-text').check();
    await page.locator('#baseURI').fill('http://example.org/ns#');
    await page.locator('#Content-Type').selectOption('text/turtle');

    const turtleData = '@prefix ex: <http://example.org/ns#> .\n\n' +
        'ex:alice a ex:Person ;\n' +
        '        ex:name "Alice" .';

    await page.locator('#text').fill(turtleData);

    await page.getByRole('button', { name: 'Upload' }).click();

    await page.getByText('Types').click();

    let type = await page.getByText('ex:Person');
    await expect(type).toHaveText('ex:Person');
});

test('Query page keeps separate drafts per browser page on refresh', async ({page}) => {
    const queryUrl = 'http://localhost:8080/rdf4j-workbench/repositories/testrepo1/query';
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
    await page.goto('http://localhost:8080/rdf4j-workbench/');
    page.on('dialog', dialog => {
        console.log(dialog.message());
        dialog.dismiss();
    });

    await createRepo(page);
    await page.goto('http://localhost:8080/rdf4j-workbench/repositories/testrepo1/query');
    await page.waitForSelector('.CodeMirror');
    await expect(page.locator('#explain-trigger-spinner')).toBeHidden();
    await expect(page.locator('#explain-trigger-cancel')).toBeHidden();
    await expect(page.locator('#compare-toggle')).toBeHidden();
    await expect(page.locator('#query-sidebar-toggle')).toBeHidden();

    await page.evaluate(() => {
        const editors = document.getElementsByClassName('CodeMirror');
        editors[0].CodeMirror.setValue('SELECT * WHERE { ?s ?p ?o } LIMIT 10');
    });

    await page.locator('#explain-trigger').click();
    await page.waitForFunction(() => {
        const explanation = document.getElementById('query-explanation');
        return explanation && explanation.textContent.trim().length > 0;
    });
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
    await expect(page.locator('#explain-compare-trigger')).toHaveAttribute('aria-label', /Refresh/i);
    await expect(page.locator('#query-diff-trigger')).toHaveAttribute('aria-label', /Diff/i);
    await expect.poll(async () => page.evaluate(() => {
        const compareCode = document.querySelectorAll('.CodeMirror-code')[1];
        return compareCode ? compareCode.textContent.replace(/\s+/g, ' ').trim() : '';
    })).toContain('SELECT * WHERE { ?s ?p ?o } LIMIT 10');
    await page.waitForFunction(() => {
        const compareExplanation = document.getElementById('query-explanation-compare');
        return compareExplanation && compareExplanation.textContent.trim().length > 0;
    });

    const collapsedLayout = await page.evaluate(() => {
        const queryForm = document.querySelector('.query-form');
        const toggle = document.getElementById('query-sidebar-toggle');
        return {
            contentLeft: queryForm.getBoundingClientRect().left,
            toggleLeft: toggle.getBoundingClientRect().left
        };
    });

    await page.locator('#query-sidebar-toggle').click();
    await expect(page.locator('#navigation')).toBeVisible();
    await expect.poll(async () => page.evaluate(() => {
        return document.querySelector('.query-form').getBoundingClientRect().left;
    })).toBeGreaterThan(collapsedLayout.contentLeft + 100);
    await expect.poll(async () => page.evaluate(() => {
        return document.getElementById('query-sidebar-toggle').getBoundingClientRect().left;
    })).toBeGreaterThan(collapsedLayout.toggleLeft + 100);

    await expect.poll(async () => page.evaluate(() => {
        return document.querySelectorAll('.CodeMirror')[1].CodeMirror.getValue();
    })).toBe('SELECT * WHERE { ?s ?p ?o } LIMIT 10');

    await page.evaluate(() => {
        const editors = document.querySelectorAll('.CodeMirror');
        editors[1].CodeMirror.setValue('ASK { ?s ?p ?o }');
    });

    await page.locator('#explain-compare-trigger').click();
    await page.waitForFunction(() => {
        const primary = document.getElementById('query-explanation');
        const compare = document.getElementById('query-explanation-compare');
        return primary && compare && primary.textContent.trim().length > 0 && compare.textContent.trim().length > 0;
    });

    await page.locator('#query-diff-trigger').click();
    await expect(page.locator('#query-diff-modal')).toHaveClass(/query-diff-modal--open/);
    await expect(page.locator('#query-diff-query .query-diff-row').first()).toBeVisible();
    await expect(page.locator('#query-diff-explanation .query-diff-row').first()).toBeVisible();

    await page.keyboard.press('Escape');
    await expect(page.locator('#query-diff-modal')).not.toHaveClass(/query-diff-modal--open/);
});

test('Explain wait state shows spinner and cancel for primary and compare actions', async ({page}) => {
    await page.goto('http://localhost:8080/rdf4j-workbench/');
    page.on('dialog', dialog => {
        console.log(dialog.message());
        dialog.dismiss();
    });

    await createRepo(page);
    await page.goto('http://localhost:8080/rdf4j-workbench/repositories/testrepo1/query');
    await page.waitForSelector('.CodeMirror');
    await delayExplainRequests(page);

    await typeIntoCodeMirror(page, 0, 'SELECT * WHERE { ?s ?p ?o } LIMIT 10');

    await page.locator('#explain-trigger').click();
    await expect(page.locator('#explain-trigger-spinner')).toBeVisible();
    await expect(page.locator('#explain-trigger-cancel')).toBeVisible();
    await page.waitForFunction(() => {
        const explanation = document.getElementById('query-explanation');
        return explanation && explanation.textContent.trim().length > 0;
    });

    await page.locator('#rerun-explanation').click();
    await expect(page.locator('#rerun-explanation-spinner')).toBeVisible();
    await expect(page.locator('#rerun-explanation-cancel')).toBeVisible();
    await page.waitForFunction(() => {
        const explanation = document.getElementById('query-explanation');
        return explanation && explanation.textContent.trim().length > 0;
    });

    await page.locator('#compare-toggle').click();
    await page.waitForFunction(() => document.querySelectorAll('.CodeMirror').length === 2);
    await typeIntoCodeMirror(page, 1, 'ASK { ?s ?p ?o }');
    await page.locator('#explain-trigger').click();
    await expect(page.locator('#explain-trigger-spinner')).toBeVisible();
    await expect(page.locator('#explain-trigger-cancel')).toBeVisible();
    await expect(page.locator('#explain-compare-cancel')).toBeVisible();
    await expect(page.locator('#explain-compare-trigger')).toHaveClass(/query-compare-action--spinning/);
    await page.waitForFunction(() => {
        const explanation = document.getElementById('query-explanation-compare');
        return explanation && explanation.textContent.trim().length > 0;
    });

    await page.locator('#rerun-explanation').click();
    await expect(page.locator('#rerun-explanation-spinner')).toBeVisible();
    await expect(page.locator('#rerun-explanation-cancel')).toBeVisible();
    await expect(page.locator('#explain-compare-cancel')).toBeVisible();
    await expect(page.locator('#explain-compare-trigger')).toHaveClass(/query-compare-action--spinning/);
    await page.waitForFunction(() => {
        const explanation = document.getElementById('query-explanation-compare');
        return explanation && explanation.textContent.trim().length > 0;
    });
});

test('Primary cancel posts matching request id and stale responses do not repaint', async ({page}) => {
    await page.goto('http://localhost:8080/rdf4j-workbench/');
    page.on('dialog', dialog => {
        console.log(dialog.message());
        dialog.dismiss();
    });

    await createRepo(page);
    await page.goto('http://localhost:8080/rdf4j-workbench/repositories/testrepo1/query');
    await page.waitForSelector('.CodeMirror');

    await typeIntoCodeMirror(page, 0, 'SELECT * WHERE { ?s ?p ?o } LIMIT 10');
    await page.locator('#explain-trigger').click();
    await page.waitForFunction(() => {
        const explanation = document.getElementById('query-explanation');
        return explanation && explanation.textContent.trim().length > 0;
    });

    const initialExplanation = await page.evaluate(() => {
        return document.getElementById('query-explanation').textContent.trim();
    });

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
    await page.goto('http://localhost:8080/rdf4j-workbench/');
    page.on('dialog', dialog => {
        console.log(dialog.message());
        dialog.dismiss();
    });

    await createRepo(page);
    await page.goto('http://localhost:8080/rdf4j-workbench/repositories/testrepo1/query');
    await page.waitForSelector('.CodeMirror');

    await typeIntoCodeMirror(page, 0, 'SELECT * WHERE { ?s ?p ?o } LIMIT 10');
    await page.locator('#explain-trigger').click();
    await page.waitForFunction(() => {
        const explanation = document.getElementById('query-explanation');
        return explanation && explanation.textContent.trim().length > 0;
    });

    await page.locator('#compare-toggle').click();
    await page.waitForFunction(() => document.querySelectorAll('.CodeMirror').length === 2);
    await typeIntoCodeMirror(page, 1, 'ASK { ?s ?p ?o }');
    const initialCompareExplanation = await page.evaluate(() => {
        return document.getElementById('query-explanation-compare').textContent.trim();
    });

    const traffic = await trackExplainTraffic(page);
    await page.locator('#explain-trigger').click();
    await expect(page.locator('#explain-trigger-cancel')).toBeVisible();
    await expect(page.locator('#explain-compare-cancel')).toBeVisible();
    await expect.poll(() => getTrackedRequestIds(traffic.explainRequests).length).toBe(2);
    await page.locator('#explain-trigger-cancel').click();
    await expect.poll(() => getTrackedRequestIds(traffic.cancelRequests).length).toBe(2);
    await expect(getTrackedRequestIds(traffic.cancelRequests)).toEqual(getTrackedRequestIds(traffic.explainRequests));
    await expect(page.locator('#explain-trigger-cancel')).toBeHidden();
    await expect(page.locator('#explain-compare-cancel')).toBeHidden();
    await page.waitForTimeout(2500);
    await expect.poll(async () => page.evaluate(() => {
        return document.getElementById('query-explanation-compare').textContent.trim();
    })).toBe(initialCompareExplanation);

    await page.locator('#explain-compare-trigger').click();
    await page.waitForFunction(previousExplanation => {
        const explanation = document.getElementById('query-explanation-compare');
        const text = explanation && explanation.textContent.trim();
        return text && text.length > 0
            && text !== 'Loading explanation...'
            && text !== previousExplanation;
    }, initialCompareExplanation);

    const refreshedCompareExplanation = await page.evaluate(() => {
        return document.getElementById('query-explanation-compare').textContent.trim();
    });

    await page.locator('#rerun-explanation').click();
    await expect(page.locator('#rerun-explanation-cancel')).toBeVisible();
    await expect(page.locator('#explain-compare-cancel')).toBeVisible();
    await page.locator('#rerun-explanation-cancel').click();
    await expect(page.locator('#rerun-explanation-cancel')).toBeHidden();
    await expect(page.locator('#explain-compare-cancel')).toBeHidden();
    await page.waitForTimeout(2500);
    await expect.poll(async () => page.evaluate(() => {
        return document.getElementById('query-explanation-compare').textContent.trim();
    })).toBe(refreshedCompareExplanation);
});

test('Changing explain level implicitly cancels pending explain with matching request id', async ({page}) => {
    await page.goto('http://localhost:8080/rdf4j-workbench/');
    page.on('dialog', dialog => {
        console.log(dialog.message());
        dialog.dismiss();
    });

    await createRepo(page);
    await page.goto('http://localhost:8080/rdf4j-workbench/repositories/testrepo1/query');
    await page.waitForSelector('.CodeMirror');

    await typeIntoCodeMirror(page, 0, 'SELECT * WHERE { ?s ?p ?o } LIMIT 10');
    await page.locator('#explain-trigger').click();
    await page.waitForFunction(() => {
        const explanation = document.getElementById('query-explanation');
        return explanation && explanation.textContent.trim().length > 0;
    });

    const initialExplanation = await page.evaluate(() => {
        return document.getElementById('query-explanation').textContent.trim();
    });

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
    await page.goto('http://localhost:8080/rdf4j-workbench/');
    page.on('dialog', dialog => {
        console.log(dialog.message());
        dialog.dismiss();
    });

    await createRepo(page);
    await page.goto('http://localhost:8080/rdf4j-workbench/repositories/testrepo1/query');
    await page.waitForSelector('.CodeMirror');

    const primaryQuery = 'SELECT * WHERE { ?s ?p ?o } LIMIT 10';
    const updatedPrimaryQuery = 'SELECT * WHERE { ?s ?p ?o } LIMIT 5';
    const compareQuery = 'ASK { ?s ?p ?o }';

    await typeIntoCodeMirror(page, 0, primaryQuery);

    await page.locator('#explain-trigger').click();
    await page.waitForFunction(() => {
        const explanation = document.getElementById('query-explanation');
        return explanation && explanation.textContent.trim().length > 0;
    });

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
    await page.goto('http://localhost:8080/rdf4j-workbench/');
    page.on('dialog', dialog => {
        console.log(dialog.message());
        dialog.dismiss();
    });

    await createRepo(page);
    await page.goto('http://localhost:8080/rdf4j-workbench/repositories/testrepo1/query');
    await page.waitForSelector('.CodeMirror');

    await expect(page.locator('#query-explanation-row')).toBeHidden();
    await expect(page.locator('#query-explanation-row-compare')).toBeHidden();
});
