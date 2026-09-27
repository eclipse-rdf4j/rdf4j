// @ts-check
const { test, expect } = require('@playwright/test');

const WORKBENCH_BASE_URL = (process.env.RDF4J_WORKBENCH_BASE_URL ||
    'http://localhost:8080/rdf4j-workbench').replace(/\/+$/, '');
const SERVER_BASE_URL = (process.env.RDF4J_SERVER_BASE_URL ||
    WORKBENCH_BASE_URL.replace(/\/rdf4j-workbench$/, '/rdf4j-server')).replace(/\/+$/, '');

test('Workbench follows system theme and remembers an explicit override', async ({ page }) => {
    await page.emulateMedia({ colorScheme: 'dark' });
    await page.addInitScript(() => {
        if (!sessionStorage.getItem('rdf4j-workbench-theme-test-initialized')) {
            localStorage.removeItem('rdf4j-workbench-theme');
            sessionStorage.setItem('rdf4j-workbench-theme-test-initialized', 'true');
        }
    });
    await page.goto(`${WORKBENCH_BASE_URL}/repositories`);

    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark');
    const themeControl = page.getByRole('combobox', { name: 'Workbench theme' });
    await expect(themeControl).toBeVisible();
    await themeControl.selectOption('light');
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'light');
    await expect.poll(() => page.evaluate(() => localStorage.getItem('rdf4j-workbench-theme')))
        .toBe('light');

    await page.reload();
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'light');
    await themeControl.selectOption('system');
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark');
    await page.emulateMedia({ colorScheme: 'light' });
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'light');
    await themeControl.selectOption('dark');
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

test('uses the calculated blue palette and preserves the original RDF4J logo', async ({ page }) => {
    await page.emulateMedia({ colorScheme: 'light' });
    await page.goto(`${WORKBENCH_BASE_URL}/repositories`);

    const palette = await page.evaluate(() => {
        const style = getComputedStyle(document.documentElement);
        const logo = document.querySelector('#logo img');
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

test('dark theme gives the unchanged Workbench brand images a light contrast plate', async ({ page }) => {
    await page.emulateMedia({ colorScheme: 'dark' });
    await page.addInitScript(() => localStorage.removeItem('rdf4j-workbench-theme'));
    await page.goto(`${WORKBENCH_BASE_URL}/repositories`);

    const brandStyle = await page.locator('#logo').evaluate(brand => ({
        background: getComputedStyle(brand).backgroundColor,
        logoFilter: getComputedStyle(brand.querySelector('img:not(.product)')).filter,
        productFilter: getComputedStyle(brand.querySelector('img.product')).filter
    }));

    expect(brandStyle.background).toBe('rgb(255, 255, 255)');
    expect(brandStyle.logoFilter).toBe('none');
    expect(brandStyle.productFilter).toBe('none');
});

test('dark creation forms keep field labels on the form surface', async ({ page }) => {
    await page.emulateMedia({ colorScheme: 'dark' });
    await page.addInitScript(() => localStorage.removeItem('rdf4j-workbench-theme'));
    await page.goto(`${WORKBENCH_BASE_URL}/repositories/NONE/create?type=memory`);

    const labelBackground = await page.locator('form[action="create"] table.dataentry th').first()
        .evaluate(label => getComputedStyle(label).backgroundColor);
    expect(labelBackground).toBe('rgba(0, 0, 0, 0)');
});

test('dark query results keep tuple rows on the dark surface inside the embedded frame', async ({ page, request }) => {
    const repositoryId = `dark-result-theme-${process.pid}-${Date.now()}`;
    const repositoryConfig = `@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix config: <tag:rdf4j.org,2023:config/>.
[] a config:Repository ; config:rep.id "${repositoryId}" ; rdfs:label "Dark result theme fixture" ;
   config:rep.impl [ config:rep.type "openrdf:SailRepository" ;
      config:sail.impl [ config:sail.type "openrdf:MemoryStore" ] ].`;
    const repository = `${SERVER_BASE_URL}/repositories/${repositoryId}`;
    const create = await request.put(repository, {
        headers: { 'Content-Type': 'text/turtle' },
        data: repositoryConfig
    });
    expect([200, 201, 204]).toContain(create.status());

    try {
        const statements = await request.post(`${repository}/statements`, {
            headers: { 'Content-Type': 'application/n-triples' },
            data: '<http://example.org/person> <http://schema.org/name> "Dark theme row" .'
        });
        expect([200, 201, 204]).toContain(statements.status());
        await page.emulateMedia({ colorScheme: 'dark' });
        await page.addInitScript(() => localStorage.removeItem('rdf4j-workbench-theme'));
        await page.goto(`${WORKBENCH_BASE_URL}/repositories/${repositoryId}/query`);
        await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark');
        await page.locator('.CodeMirror').first().evaluate(element =>
            element.CodeMirror.setValue('SELECT ?person ?name WHERE { ?person <http://schema.org/name> ?name }'));
        await page.locator('#exec').click();

        const resultFrame = page.frameLocator('#query-results-frame');
        const resultCell = resultFrame.locator('#query-result-embedded table.data tbody td').first();
        await expect(resultCell).toContainText('http://example.org/person');
        const resultFrameTheme = await resultFrame.locator('html').getAttribute('data-theme');
        const rowBackground = await resultCell.evaluate(element => getComputedStyle(element).backgroundColor);
        expect(resultFrameTheme).toBe('dark');
        expect(rowBackground).not.toBe('rgb(255, 255, 255)');
    } finally {
        const remove = await request.delete(repository);
        expect([200, 204, 404]).toContain(remove.status());
    }
});
