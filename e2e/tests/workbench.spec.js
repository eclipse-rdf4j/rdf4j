// @ts-check
const {test, expect} = require('@playwright/test');

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

async function typeIntoCodeMirror(page, index, value) {
    await page.locator('.CodeMirror').nth(index).click();
    await page.keyboard.press('Meta+A');
    await page.keyboard.press('Backspace');
    await page.keyboard.type(value);
    await expect.poll(async () => page.evaluate(editorIndex => {
        return document.querySelectorAll('.CodeMirror')[editorIndex].CodeMirror.getValue();
    }, index)).toBe(value);
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
    await expect(page.locator('#explain-compare-spinner')).toBeHidden();
    await expect(page.locator('#explain-compare-cancel')).toBeHidden();
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
