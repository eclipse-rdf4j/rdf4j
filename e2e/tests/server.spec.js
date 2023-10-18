// @ts-check
const {test, expect} = require('@playwright/test');


test('RDF4J Server has correct title', async ({page}) => {
    await page.goto('http://localhost:8080/rdf4j-server/');

    // Expect a title "to contain" a substring.
    await expect(page).toHaveTitle("RDF4J Server - Home");
});
