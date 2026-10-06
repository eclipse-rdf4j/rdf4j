// @ts-check
const { test, expect } = require('@playwright/test');
const {
    deleteRepository,
    memoryRepositoryConfiguration,
    repositoryPageUrl,
    serverBaseUrl,
    uniqueRepositoryId,
    waitForRoute,
    workbenchBaseUrl
} = require('./workbench-test-helpers.js');

// Retired with the redesign (see .agent/execplans/workbench-stale-spec-migration-20261002.md): "dark theme gives the
// unchanged Workbench brand images a light contrast plate" asserted the white logo plate that plan task M2.5 of
// .agent/execplans/workbench-app-shell-and-critique-fixes-20260930.md replaced with light-ink dark logos;
// workbench-shell.spec.js "dark mode shows the light-ink logo without a white plate" checks the dark logo and the
// transparent brand background.

const WORKBENCH_BASE_URL = workbenchBaseUrl();
const REPOSITORY_ID = uniqueRepositoryId('dark-result-theme');

test.beforeAll(async ({ request }) => {
    const repository = `${serverBaseUrl()}/repositories/${REPOSITORY_ID}`;
    const create = await request.put(repository, {
        headers: { 'Content-Type': 'text/turtle' },
        data: memoryRepositoryConfiguration(REPOSITORY_ID, 'Dark result theme fixture')
    });
    expect([200, 201, 204]).toContain(create.status());
    const statements = await request.post(`${repository}/statements`, {
        headers: { 'Content-Type': 'application/n-triples' },
        data: '<http://example.org/person> <http://schema.org/name> "Dark theme row" .'
    });
    expect([200, 201, 204]).toContain(statements.status());
});

test.afterAll(async ({ request }) => {
    await deleteRepository(request, serverBaseUrl(), REPOSITORY_ID);
});

test('Workbench follows system theme and remembers an explicit preference without a selector', async ({ page }) => {
    await page.emulateMedia({ colorScheme: 'dark' });
    await page.addInitScript(() => {
        if (!sessionStorage.getItem('rdf4j-workbench-theme-test-initialized')) {
            localStorage.removeItem('rdf4j-workbench-theme');
            sessionStorage.setItem('rdf4j-workbench-theme-test-initialized', 'true');
        }
    });
    await page.goto(`${WORKBENCH_BASE_URL}/repositories`);

    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark');
    // The shell renders after the document loads; look for a theme selector only once the page is shown.
    await waitForRoute(page, 'repositories');
    await expect(page.locator('#workbench-theme, .workbench-theme-control')).toHaveCount(0);
    await page.evaluate(() => window.RDF4JWorkbenchTheme.setPreference('light'));
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'light');
    await expect.poll(() => page.evaluate(() => localStorage.getItem('rdf4j-workbench-theme')))
        .toBe('light');

    await page.reload();
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'light');
    await page.evaluate(() => window.RDF4JWorkbenchTheme.setPreference('system'));
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark');
    await page.emulateMedia({ colorScheme: 'light' });
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'light');
    await page.evaluate(() => window.RDF4JWorkbenchTheme.setPreference('dark'));
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark');
    await page.emulateMedia({ colorScheme: 'light' });
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark');

    const darkPalette = await page.evaluate(() => {
        const style = getComputedStyle(document.documentElement);
        return {
            canvas: style.getPropertyValue('--workbench-canvas').trim(),
            surface: style.getPropertyValue('--workbench-surface').trim(),
            ink: style.getPropertyValue('--workbench-ink').trim(),
            body: style.getPropertyValue('--workbench-body').trim(),
            muted: style.getPropertyValue('--workbench-muted').trim(),
            outline: style.getPropertyValue('--workbench-outline').trim(),
            primary: style.getPropertyValue('--workbench-primary').trim(),
            hover: style.getPropertyValue('--workbench-primary-hover').trim(),
            selected: style.getPropertyValue('--workbench-selected').trim()
        };
    });
    expect(darkPalette).toEqual({
        canvas: '#080e11',
        surface: '#111a1d',
        ink: '#e8f0f3',
        body: '#cedadf',
        muted: '#9aabb2',
        outline: '#687e87',
        primary: '#73c4e2',
        hover: '#94d6ef',
        selected: '#15323d'
    });
});

// The text cursor follows the theme: in the dark theme the editor's cursor (a border drawn by CodeMirror) and the
// caret of a native field are light, like the text, not the black of yasqe.min.css.
for (const view of ['query', 'update']) {
    test(`the dark theme shows a light cursor in the ${view} editor`, async ({ page }) => {
        await page.emulateMedia({ colorScheme: 'dark' });
        await page.goto(repositoryPageUrl(REPOSITORY_ID, view));
        await waitForRoute(page, view);
        const editor = page.locator('.CodeMirror').first();
        await editor.waitFor({ state: 'visible' });
        await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark');
        const colors = await editor.evaluate((element) => {
            const cursor = element.querySelector('.CodeMirror-cursor');
            const ink = getComputedStyle(document.documentElement).getPropertyValue('--workbench-ink').trim();
            const probe = document.createElement('span');
            probe.style.color = ink;
            document.body.appendChild(probe);
            const inkRgb = getComputedStyle(probe).color;
            probe.remove();
            return { cursor: cursor ? getComputedStyle(cursor).borderLeftColor : 'missing', ink: inkRgb };
        });
        expect(colors.cursor, 'the editor cursor is drawn in the ink colour').toBe(colors.ink);
        expect(colors.cursor).not.toBe('rgb(0, 0, 0)');
    });
}

test('the dark theme shows a light caret in native fields', async ({ page }) => {
    await page.emulateMedia({ colorScheme: 'dark' });
    await page.goto(repositoryPageUrl(REPOSITORY_ID, 'explore'));
    await waitForRoute(page, 'explore');
    const field = page.locator('#explore-resource, input[name="resource"]').first();
    await field.waitFor({ state: 'visible' });
    const colors = await field.evaluate((element) => {
        const style = getComputedStyle(element);
        return { caret: style.caretColor, text: style.color };
    });
    expect(colors.caret).toBe(colors.text);
    expect(colors.caret).not.toBe('rgb(0, 0, 0)');
});

test('uses the calculated blue palette and preserves the original RDF4J logo', async ({ page }) => {
    await page.emulateMedia({ colorScheme: 'light' });
    await page.goto(`${WORKBENCH_BASE_URL}/repositories`);
    // The shell (and with it the logo) renders after the document loads.
    await waitForRoute(page, 'repositories');
    // The brand holds a light and a dark image pair (plan task M2.5); the light theme shows the original logo.
    const visibleLogo = '#logo img:not(.product):visible';
    await expect(page.locator(visibleLogo)).toHaveCount(1);
    await expect.poll(() => page.locator(visibleLogo).evaluate(image => image.complete && image.naturalWidth > 0))
        .toBe(true);

    const palette = await page.evaluate(() => {
        const style = getComputedStyle(document.documentElement);
        const logo = Array.from(document.querySelectorAll('#logo img:not(.product)'))
            .find(image => image.getClientRects().length > 0);
        return {
            canvas: style.getPropertyValue('--workbench-canvas').trim(),
            surface: style.getPropertyValue('--workbench-surface').trim(),
            ink: style.getPropertyValue('--workbench-ink').trim(),
            body: style.getPropertyValue('--workbench-body').trim(),
            muted: style.getPropertyValue('--workbench-muted').trim(),
            outline: style.getPropertyValue('--workbench-outline').trim(),
            primary: style.getPropertyValue('--workbench-primary').trim(),
            hover: style.getPropertyValue('--workbench-primary-hover').trim(),
            selected: style.getPropertyValue('--workbench-selected').trim(),
            logoPath: new URL(logo.src).pathname,
            logoWidth: logo.naturalWidth,
            logoHeight: logo.naturalHeight
        };
    });

    expect(palette).toMatchObject({
        canvas: '#f5f9fb',
        surface: '#ffffff',
        ink: '#14242b',
        body: '#2f4047',
        muted: '#4f6168',
        outline: '#778a92',
        primary: '#20576a',
        hover: '#16495a',
        selected: '#def3fb',
        logoPath: expect.stringMatching(/\/images\/logo\.png$/),
        logoWidth: 132,
        logoHeight: 80
    });
});

test('dark creation forms keep field labels on the form surface', async ({ page }) => {
    await page.emulateMedia({ colorScheme: 'dark' });
    await page.addInitScript(() => localStorage.removeItem('rdf4j-workbench-theme'));
    await page.goto(`${WORKBENCH_BASE_URL}/repositories/NONE/create?type=memory`);

    const labelBackground = await page.locator('form[action="create"] table.dataentry th').first()
        .evaluate(label => getComputedStyle(label).backgroundColor);
    expect(labelBackground).toBe('rgba(0, 0, 0, 0)');
});

test('dark query results keep tuple rows on the dark surface', async ({ page }) => {
    await page.emulateMedia({ colorScheme: 'dark' });
    await page.addInitScript(() => localStorage.removeItem('rdf4j-workbench-theme'));
    await page.goto(repositoryPageUrl(REPOSITORY_ID, 'query'));
    await waitForRoute(page, 'query');
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark');
    await page.locator('.CodeMirror').first().evaluate(element =>
        element.CodeMirror.setValue('SELECT ?person ?name WHERE { ?person <http://schema.org/name> ?name }'));
    await page.locator('#exec').click();

    // The result streams into the page (the embedded result frame is gone), so it shares the page's theme.
    const resultCell = page.locator('#query-results [data-query-stream-root] table.data tbody td').first();
    await expect(resultCell).toContainText('http://example.org/person');
    const resultTheme = await page.locator('html').getAttribute('data-theme');
    const rowBackground = await resultCell.evaluate(element => getComputedStyle(element).backgroundColor);
    expect(resultTheme).toBe('dark');
    expect(rowBackground).not.toBe('rgb(255, 255, 255)');
});
