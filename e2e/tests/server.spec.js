// @ts-check
const {test, expect} = require('@playwright/test');
const { serverBaseUrl } = require('./workbench-test-helpers');


test('RDF4J Server has correct title', async ({page}) => {
    await page.goto(`${serverBaseUrl()}/`);

    // Expect a title "to contain" a substring.
    await expect(page).toHaveTitle("RDF4J Server - Home");
});
