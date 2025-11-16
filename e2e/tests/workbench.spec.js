// @ts-check
const {test, expect} = require('@playwright/test');

const baseUrl = process.env.RDF4J_BASE_URL || 'http://localhost:8080/';
const workbenchUrl = new URL('rdf4j-workbench/', baseUrl).toString();

test.beforeEach(async ({page}, testInfo) => {
    await page.goto(workbenchUrl);
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
    await page.goto(workbenchUrl);

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

test('Create repo', async ({page}) => {
    await page.goto(workbenchUrl);
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
    await page.goto(workbenchUrl);
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

