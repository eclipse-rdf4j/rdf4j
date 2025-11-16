// @ts-check
const {test, expect} = require('@playwright/test');

const baseUrl = process.env.RDF4J_BASE_URL || 'http://localhost:8080/';
const serverUrl = new URL('rdf4j-server/', baseUrl).toString();

test('RDF4J Server has correct title', async ({page}) => {
    await page.goto(serverUrl);

    // Expect a title "to contain" a substring.
    await expect(page).toHaveTitle("RDF4J Server - Home");
});
