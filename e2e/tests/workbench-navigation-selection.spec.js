// @ts-check
const { test, expect } = require('@playwright/test');
const fs = require('fs');
const path = require('path');
const {
    deleteRepository,
    memoryRepositoryConfiguration,
    repositoryPageUrl,
    serverBaseUrl,
    uniqueRepositoryId,
    waitForRoute,
    workbenchBaseUrl
} = require('./workbench-test-helpers');

const WORKBENCH_MOUNT = new URL(workbenchBaseUrl()).pathname.replace(/\/+$/, '');
const REPOSITORY_ID = uniqueRepositoryId('workbench-navigation');
const QUERY_URL = repositoryPageUrl(REPOSITORY_ID, 'query');
const CAPTURE_DIR = process.env.WORKBENCH_NAV_CAPTURE_DIR;
let repositoryCreated = false;

test.beforeAll(async ({ request }) => {
    const response = await request.put(`${serverBaseUrl()}/repositories/${REPOSITORY_ID}`, {
        headers: { 'Content-Type': 'text/turtle' },
        data: memoryRepositoryConfiguration(REPOSITORY_ID, 'Workbench navigation fixture')
    });
    expect([200, 201, 204]).toContain(response.status());
    repositoryCreated = true;
});

test.afterAll(async ({ request }) => {
    if (repositoryCreated) {
        await deleteRepository(request, serverBaseUrl(), REPOSITORY_ID);
    }
});

// The default menu groups and order of plan task M2.4: Repository, Data, Server, System.
const menuDestinations = () => [
    'query',
    'saved-queries',
    'explore',
    'summary',
    'namespaces',
    'contexts',
    'types',
    'add',
    'export',
    'update',
    'remove',
    'clear',
    `${WORKBENCH_MOUNT}/repositories/NONE/repositories`,
    'create',
    'delete',
    `${WORKBENCH_MOUNT}/repositories/NONE/server`,
    'information'
];

/** The view a menu destination shows (the server-level items link by absolute path). */
function viewOf(route) {
    return route.split('/').pop();
}

// Below 900 px the menu is the menu sheet opened by the context bar's Menu button (plan task M2.8); it lists the same
// links as the sidebar with its own 44 px rows, and its selected link follows mockup 14 (the selected fill and a
// primary bar), while the sidebar's keeps the soft rule fill and a bar in navigation ink.
const VIEWPORTS = [
    { name: 'desktop', width: 1440, height: 1000, menu: '#navigation',
        fill: '--workbench-soft-rule', bar: '--workbench-nav', contentInset: 8 },
    { name: 'mobile', width: 390, height: 1000, menu: '#workbench-menu-sheet',
        fill: '--workbench-selected', bar: '--workbench-primary', contentInset: 12 }
];

/** The selected-link treatment of a viewport's menu, with colors resolved from the design tokens. */
async function selectedTreatmentFor(page, viewport) {
    const colors = await page.evaluate(tokens => {
        const probe = document.createElement('span');
        document.body.append(probe);
        const resolved = tokens.map(token => {
            probe.style.color = `var(${token})`;
            return getComputedStyle(probe).color;
        });
        probe.remove();
        return resolved;
    }, [viewport.fill, '--workbench-ink', viewport.bar]);
    return {
        linkBackground: colors[0],
        linkColor: colors[1],
        linkWeight: '600',
        linkRadius: '7px',
        borderWidth: '3px',
        borderColor: colors[2],
        itemLinkInset: 0,
        contentInset: viewport.contentInset,
        itemBackground: 'rgba(0, 0, 0, 0)',
        itemBorderWidth: '0px'
    };
}

function readSelectedLink(link) {
    const item = link.closest('li');
    const icon = link.querySelector('svg.workbench-action-icon');
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

async function waitForCurrentLink(page, viewport, expectedRoute) {
    const currentLinks = page.locator(`${viewport.menu} a[aria-current="page"]`);
    await expect.poll(() => currentLinks.count(), { timeout: 5000 }).toBe(1);
    await expect(currentLinks).toHaveAttribute('data-workbench-nav-href', expectedRoute);
    await expect(currentLinks.locator('xpath=..')).toHaveClass(/\bcurrent\b/);
    return currentLinks;
}

async function showMobileMenu(page, isMobile) {
    if (!isMobile) {
        await expect(page.locator('#navigation')).toBeVisible();
        return;
    }
    await page.locator('#workbench-menu-button').click();
    await expect(page.locator('#workbench-menu-sheet')).toBeVisible();
}

/** The menu sheet is a modal dialog; it is closed before the page under it is used. */
async function hideMobileMenu(page, isMobile) {
    if (isMobile) {
        await page.keyboard.press('Escape');
        await expect(page.locator('#workbench-menu-sheet')).toBeHidden();
    }
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
    let routeLinks = [];
    const selectedStyleMismatches = [];
    const interactionStyleMismatches = [];

    for (const viewport of VIEWPORTS) {
        const isMobile = viewport.name === 'mobile';
        const menu = viewport.menu;
        await page.setViewportSize({ width: viewport.width, height: viewport.height });
        await page.goto(QUERY_URL, { waitUntil: 'load' });
        await page.locator('#workbench-page-surface').waitFor({ state: 'attached' });
        await waitForRoute(page, 'query');
        await showMobileMenu(page, isMobile);
        const selectedTreatment = await selectedTreatmentFor(page, viewport);

        routeLinks = await page.locator(`${menu} a[data-workbench-nav-href]`).evaluateAll(links =>
            links.map(link => ({
                route: link.getAttribute('data-workbench-nav-href'),
                url: link.href
            }))
        );
        expect(routeLinks.map(link => link.route)).toEqual(menuDestinations());

        const queryRoute = routeLinks.find(link => link.route === 'query');
        const addRoute = routeLinks.find(link => link.route === 'add');
        expect(queryRoute).toBeDefined();
        expect(addRoute).toBeDefined();

        let currentLink = await waitForCurrentLink(page, viewport, 'query');
        await expect(page.locator(`${menu} a[aria-current="page"]`)).toBeVisible();
        await capture(page, viewport.name, 'query');

        await page.goto(addRoute.url, { waitUntil: 'load' });
        await page.locator('#workbench-page-surface').waitFor({ state: 'attached' });
        await waitForRoute(page, 'add');
        await showMobileMenu(page, isMobile);
        currentLink = await waitForCurrentLink(page, viewport, 'add');
        await expect(currentLink).toBeVisible();
        await capture(page, viewport.name, 'add');

        for (const destination of routeLinks) {
            await page.goto(destination.url, { waitUntil: 'load' });
            await page.locator('#workbench-page-surface').waitFor({ state: 'attached' });
            await waitForRoute(page, viewOf(destination.route));
            await showMobileMenu(page, isMobile);

            if (destination.route === `${WORKBENCH_MOUNT}/repositories/NONE/server`) {
                await expect(page.locator('#workbench-server')).toBeVisible();
                // The server page uses the same policy menu as every page, so its own item is selected.
                await expect(page.locator(`${menu} a[aria-current="page"]`)).toHaveCount(1);
                await expect(page.locator(`${menu} a[aria-current="page"]`))
                    .toHaveAttribute('href', /\/repositories\/NONE\/server$/);
                continue;
            }

            const activeLink = await waitForCurrentLink(page, viewport, destination.route);
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
                await hideMobileMenu(page, isMobile);
                const editor = page.locator('.CodeMirror').first();
                await editor.evaluate(element =>
                    element.CodeMirror.setValue('SELECT ?value WHERE { VALUES ?value { "menu" } }')
                );
                await page.locator('#exec').click();
                // The result streams into the page (the result iframe is gone, plan task M9.1).
                await expect(page.locator(
                    '#query-results [data-query-stream-root] [id^="query-result-table-wrap-"] table.data tbody tr'))
                    .toHaveCount(1, { timeout: 30000 });
                await waitForCurrentLink(page, viewport, 'query');
            }
        }

        for (const route of ['query', 'add']) {
            const destination = routeLinks.find(link => link.route === route);
            expect(destination).toBeDefined();
            await page.goto(destination.url, { waitUntil: 'load' });
            await page.locator('#workbench-page-surface').waitFor({ state: 'attached' });
            await waitForRoute(page, route);
            const currentLink = await waitForCurrentLink(page, viewport, route);
            await showMobileMenu(page, isMobile);
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
