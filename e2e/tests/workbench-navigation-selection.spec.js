// @ts-check
const { test, expect } = require('@playwright/test');
const fs = require('fs');
const path = require('path');

const SERVER_BASE_URL = process.env.RDF4J_SERVER_BASE_URL || 'http://127.0.0.1:8080/rdf4j-server';
const WORKBENCH_BASE_URL = process.env.RDF4J_WORKBENCH_BASE_URL || 'http://127.0.0.1:8080/rdf4j-workbench';
const REPOSITORY_ID = `workbench-navigation-${process.pid}-${Date.now()}`;
const QUERY_URL = `${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/query`;
const CAPTURE_DIR = process.env.WORKBENCH_NAV_CAPTURE_DIR;
let repositoryCreated = false;

function repositoryConfig(repositoryId) {
    return `@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix config: <tag:rdf4j.org,2023:config/>.

[] a config:Repository ;
   config:rep.id "${repositoryId}" ;
   rdfs:label "Workbench navigation fixture" ;
   config:rep.impl [
      config:rep.type "openrdf:SailRepository" ;
      config:sail.impl [ config:sail.type "openrdf:MemoryStore" ]
   ].`;
}

test.beforeAll(async ({ request }) => {
    const response = await request.put(`${SERVER_BASE_URL}/repositories/${REPOSITORY_ID}`, {
        headers: { 'Content-Type': 'text/turtle' },
        data: repositoryConfig(REPOSITORY_ID)
    });
    expect([200, 201, 204]).toContain(response.status());
    repositoryCreated = true;
});

test.afterAll(async ({ request }) => {
    if (repositoryCreated) {
        await request.delete(`${SERVER_BASE_URL}/repositories/${REPOSITORY_ID}`);
    }
});

const MENU_DESTINATIONS = [
    '../NONE/server',
    'repositories',
    'create',
    'delete',
    'summary',
    'namespaces',
    'contexts',
    'types',
    'explore',
    'query',
    'saved-queries',
    'export',
    'update',
    'add',
    'remove',
    'clear',
    'information'
];

const VIEWPORTS = [
    { name: 'desktop', width: 1440, height: 1000 },
    { name: 'mobile', width: 390, height: 1000 }
];

function readSelectedLink(link) {
    const item = link.closest('li');
    const icon = link.querySelector('svg.query-nav-icon');
    const linkStyle = getComputedStyle(link);
    const itemStyle = getComputedStyle(item);
    const linkBounds = link.getBoundingClientRect();
    const itemBounds = item.getBoundingClientRect();
    const iconBounds = icon.getBoundingClientRect();
    return {
        route: link.getAttribute('data-workbench-nav-href'),
        ariaCurrent: link.getAttribute('aria-current'),
        itemCurrent: item.classList.contains('current'),
        linkBackground: linkStyle.backgroundColor,
        linkColor: linkStyle.color,
        linkWeight: linkStyle.fontWeight,
        linkRadius: linkStyle.borderRadius,
        borderWidth: linkStyle.borderLeftWidth,
        borderColor: linkStyle.borderLeftColor,
        itemLinkInset: Math.round(linkBounds.left - itemBounds.left),
        contentInset: Math.round(iconBounds.left - linkBounds.left),
        itemBackground: itemStyle.backgroundColor,
        itemBorderWidth: itemStyle.borderLeftWidth
    };
}

function treatmentDifferences(metrics, treatment) {
    return Object.fromEntries(Object.entries(treatment)
        .filter(([name, expected]) => metrics[name] !== expected));
}

async function waitForCurrentLink(page, expectedRoute) {
    const currentLinks = page.locator('#navigation a[aria-current="page"]');
    await expect.poll(() => currentLinks.count(), { timeout: 5000 }).toBe(1);
    await expect(currentLinks).toHaveAttribute('data-workbench-nav-href', expectedRoute);
    await expect(currentLinks.locator('xpath=..')).toHaveClass(/\bcurrent\b/);
    return currentLinks;
}

async function showMobileMenu(page, isMobile) {
    const menu = page.locator('#workbench-navigation-disclosure');
    if (!isMobile) {
        await expect.poll(() => menu.evaluate(element => element.open)).toBe(true);
        return;
    }
    if (await menu.evaluate(element => element.open) === false) {
        await page.locator('#workbench-navigation-summary').click();
    }
    await expect.poll(() => menu.evaluate(element => element.open)).toBe(true);
}

async function capture(page, viewportName, routeName) {
    if (!CAPTURE_DIR) {
        return;
    }
    fs.mkdirSync(CAPTURE_DIR, { recursive: true });
    await page.screenshot({
        path: path.join(CAPTURE_DIR, `${viewportName}-${routeName}-selected.png`),
        fullPage: false
    });
}

test('every menu destination has one consistent selected link on desktop and mobile', async ({ page }) => {
    const selectedTreatment = {
        linkBackground: 'rgb(226, 232, 240)',
        linkColor: 'rgb(15, 23, 42)',
        linkWeight: '600',
        linkRadius: '7px',
        borderWidth: '3px',
        borderColor: 'rgb(51, 65, 85)',
        itemLinkInset: 0,
        contentInset: 8,
        itemBackground: 'rgba(0, 0, 0, 0)',
        itemBorderWidth: '0px'
    };
    let routeLinks = [];
    const selectedStyleMismatches = [];
    const interactionStyleMismatches = [];

    for (const viewport of VIEWPORTS) {
        await page.setViewportSize({ width: viewport.width, height: viewport.height });
        await page.goto(QUERY_URL, { waitUntil: 'load' });
        await page.locator('#workbench-page-surface').waitFor({ state: 'attached' });
        await showMobileMenu(page, viewport.name === 'mobile');

        routeLinks = await page.locator('#navigation a[data-workbench-nav-href]').evaluateAll(links =>
            links.map(link => ({
                route: link.getAttribute('data-workbench-nav-href'),
                url: link.href
            }))
        );
        expect(routeLinks.map(link => link.route)).toEqual(MENU_DESTINATIONS);

        const queryRoute = routeLinks.find(link => link.route === 'query');
        const addRoute = routeLinks.find(link => link.route === 'add');
        expect(queryRoute).toBeDefined();
        expect(addRoute).toBeDefined();

        let currentLink = await waitForCurrentLink(page, 'query');
        await expect(page.locator('#navigation a[aria-current="page"]')).toBeVisible();
        await capture(page, viewport.name, 'query');

        await page.goto(addRoute.url, { waitUntil: 'load' });
        await page.locator('#workbench-page-surface').waitFor({ state: 'attached' });
        await showMobileMenu(page, viewport.name === 'mobile');
        currentLink = await waitForCurrentLink(page, 'add');
        await expect(currentLink).toBeVisible();
        await capture(page, viewport.name, 'add');

        for (const destination of routeLinks) {
            await page.goto(destination.url, { waitUntil: 'load' });
            await page.locator('#workbench-page-surface').waitFor({ state: 'attached' });
            await showMobileMenu(page, viewport.name === 'mobile');

            const activeLink = await waitForCurrentLink(page, destination.route);
            await expect(activeLink).toBeVisible();
            const metrics = await activeLink.evaluate(readSelectedLink);
            const differences = treatmentDifferences(metrics, selectedTreatment);
            if (Object.keys(differences).length > 0) {
                selectedStyleMismatches.push({
                    viewport: viewport.width,
                    route: destination.route,
                    differences
                });
            }

            if (destination.route === 'query') {
                const editor = page.locator('.CodeMirror').first();
                await editor.evaluate(element =>
                    element.CodeMirror.setValue('SELECT ?value WHERE { VALUES ?value { "menu" } }')
                );
                await page.locator('#exec').click();
                const resultFrame = page.frameLocator('#query-results-frame');
                await expect(resultFrame.locator('#query-result-table-wrap table.data tbody tr'))
                    .toHaveCount(1, { timeout: 30000 });
                await waitForCurrentLink(page, 'query');
            }
        }

        for (const route of ['query', 'add']) {
            const destination = routeLinks.find(link => link.route === route);
            expect(destination).toBeDefined();
            await page.goto(destination.url, { waitUntil: 'load' });
            await page.locator('#workbench-page-surface').waitFor({ state: 'attached' });
            const currentLink = await waitForCurrentLink(page, route);
            await showMobileMenu(page, viewport.name === 'mobile');
            await currentLink.focus();
            await expect(currentLink).toBeFocused();
            const focusedDifferences = treatmentDifferences(
                await currentLink.evaluate(readSelectedLink), selectedTreatment
            );
            if (Object.keys(focusedDifferences).length > 0) {
                interactionStyleMismatches.push({
                    viewport: viewport.width,
                    route,
                    interaction: 'keyboard focus',
                    differences: focusedDifferences
                });
            }
            await page.keyboard.press('Tab');
            await expect(currentLink).toHaveAttribute('aria-current', 'page');

            await currentLink.hover();
            const hoveredDifferences = treatmentDifferences(
                await currentLink.evaluate(readSelectedLink), selectedTreatment
            );
            if (Object.keys(hoveredDifferences).length > 0) {
                interactionStyleMismatches.push({
                    viewport: viewport.width,
                    route,
                    interaction: 'hover',
                    differences: hoveredDifferences
                });
            }
        }
    }

    expect(selectedStyleMismatches).toEqual([]);
    expect(interactionStyleMismatches).toEqual([]);
});
