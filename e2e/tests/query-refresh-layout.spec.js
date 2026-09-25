// @ts-check
const { test, expect } = require('@playwright/test');
const fs = require('fs');
const path = require('path');

const SERVER_BASE_URL = process.env.RDF4J_SERVER_BASE_URL || 'http://127.0.0.1:8080/rdf4j-server';
const WORKBENCH_BASE_URL = process.env.RDF4J_WORKBENCH_BASE_URL || 'http://127.0.0.1:8080/rdf4j-workbench';
const REPOSITORY_ID = 'query-refresh-layout';
const QUERY_URL = `${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/query`;
const DESIGN_DIR = path.resolve(__dirname, '../../design/workbench-query-refresh');
const CHEVRON_DIR = path.resolve(__dirname, '../../design/workbench-polish-20260925/final-chevron-v6-20260925');

function readChevron(toggle) {
    const icon = toggle.querySelector('svg.workbench-disclosure-chevron');
    if (!icon) {
        return { present: false };
    }
    const iconBounds = icon.getBoundingClientRect();
    const toggleBounds = toggle.getBoundingClientRect();
    const iconStyle = getComputedStyle(icon);
    const toggleStyle = getComputedStyle(toggle);
    const transform = iconStyle.transform === 'none' ? new DOMMatrixReadOnly() : new DOMMatrixReadOnly(iconStyle.transform);
    return {
        present: true,
        viewBox: icon.getAttribute('viewBox'),
        cssWidth: parseFloat(iconStyle.width),
        cssHeight: parseFloat(iconStyle.height),
        width: iconBounds.width,
        height: iconBounds.height,
        path: icon.querySelector('path')?.getAttribute('d'),
        focusable: icon.getAttribute('focusable'),
        ariaHidden: icon.getAttribute('aria-hidden'),
        fill: iconStyle.fill,
        stroke: iconStyle.stroke,
        strokeWidth: parseFloat(iconStyle.strokeWidth),
        strokeLinecap: iconStyle.strokeLinecap,
        strokeLinejoin: iconStyle.strokeLinejoin,
        rotation: Math.atan2(transform.b, transform.a) * 180 / Math.PI,
        gap: parseFloat(toggleStyle.gap),
        edgeInset: toggleBounds.right - iconBounds.right,
        centerDelta: Math.abs((toggleBounds.top + toggleBounds.bottom - iconBounds.top - iconBounds.bottom) / 2)
    };
}

function expectChevron(metrics, size = 16, rotation = 0) {
    expect(metrics.present).toBe(true);
    expect(metrics.viewBox).toBe('0 0 24 24');
    expect(metrics.cssWidth).toBe(size);
    expect(metrics.cssHeight).toBe(size);
    expect(metrics.width).toBeCloseTo(size, 0);
    expect(metrics.height).toBeCloseTo(size, 0);
    expect(metrics.path).toBe('m6 9 6 6 6-6');
    expect(metrics.focusable).toBe('false');
    expect(metrics.ariaHidden).toBe('true');
    expect(metrics.fill).toBe('none');
    expect(metrics.stroke).not.toBe('none');
    expect(metrics.strokeWidth).toBe(1.75);
    expect(metrics.strokeLinecap).toBe('round');
    expect(metrics.strokeLinejoin).toBe('round');
    expect(Math.abs(metrics.rotation)).toBeCloseTo(Math.abs(rotation), 0);
    expect(metrics.gap).toBe(8);
    expect(metrics.edgeInset).toBeGreaterThanOrEqual(12);
    expect(metrics.centerDelta).toBeLessThanOrEqual(1);
}

async function expectChevronState(locator, size, rotation) {
    await expect.poll(async () => {
        const actualRotation = Math.abs((await locator.evaluate(readChevron)).rotation);
        return Math.abs(actualRotation - Math.abs(rotation)) < 0.1;
    }, {
        intervals: [16, 32, 64]
    }).toBe(true);
    expectChevron(await locator.evaluate(readChevron), size, rotation);
}

test.beforeEach(async ({ page, request }) => {
    await request.delete(`${SERVER_BASE_URL}/repositories/${REPOSITORY_ID}`);
    const createResponse = await request.put(`${SERVER_BASE_URL}/repositories/${REPOSITORY_ID}`, {
        headers: { 'Content-Type': 'text/turtle' },
        data: nativeRepositoryConfig(REPOSITORY_ID)
    });
    expect([200, 201, 204]).toContain(createResponse.status());
    await page.goto(QUERY_URL);
    await page.locator('.CodeMirror').waitFor({ state: 'visible' });
});

test('desktop query surfaces use flat hierarchy and readable controls', async ({ page }) => {
    const metrics = await page.evaluate(() => {
        const execute = document.querySelector('#exec');
        const results = document.querySelector('#query-results');
        const bodyStyle = getComputedStyle(document.body);
        const resultsStyle = getComputedStyle(results);
        const executeBounds = execute.getBoundingClientRect();
        return {
            bodyBackgroundImage: bodyStyle.backgroundImage,
            resultsBackground: resultsStyle.backgroundColor,
            executeHeight: executeBounds.height,
            executeRadius: parseFloat(getComputedStyle(execute).borderTopLeftRadius),
            resultsBorderWidth: parseFloat(resultsStyle.borderTopWidth)
        };
    });

    expect(metrics.bodyBackgroundImage).toBe('none');
    expect(metrics.resultsBackground).toBe('rgb(255, 255, 255)');
    expect(metrics.executeHeight).toBeGreaterThanOrEqual(32);
    expect(metrics.executeRadius).toBeGreaterThanOrEqual(6);
    expect(metrics.resultsBorderWidth).toBeGreaterThanOrEqual(1);
});

test('narrow query layout keeps navigation above full-width content', async ({ page }) => {
    for (const width of [390, 768]) {
        await page.setViewportSize({ width, height: 900 });
        await page.reload();
        await page.locator('.CodeMirror').waitFor({ state: 'visible' });
        await page.locator('.CodeMirror').evaluate(element =>
            element.CodeMirror.setValue('ASK { VALUES ?s { <http://example.org/alice> } }'));
        await page.locator('#exec').click();
        await page.frameLocator('#query-results-frame').locator('#rdf4j-query-result').waitFor({ state: 'attached' });
        const metrics = await page.evaluate(() => {
            const navigation = document.querySelector('#navigation').getBoundingClientRect();
            const content = document.querySelector('#content').getBoundingClientRect();
            const results = document.querySelector('#query-results').getBoundingClientRect();
            return {
                clientWidth: document.documentElement.clientWidth,
                scrollWidth: document.documentElement.scrollWidth,
                navigationBottom: navigation.bottom,
                contentTop: content.top,
                contentWidth: content.width,
                resultsWidth: results.width
            };
        });

        expect(metrics.scrollWidth).toBeLessThanOrEqual(metrics.clientWidth + 1);
        expect(metrics.navigationBottom).toBeLessThanOrEqual(metrics.contentTop + 1);
        expect(metrics.contentWidth).toBeGreaterThanOrEqual(metrics.clientWidth - 32);
        expect(metrics.resultsWidth).toBeGreaterThanOrEqual(metrics.clientWidth - 32);
    }
});

test('captures the stacked query and embedded result surfaces', async ({ page }) => {
    for (const [width, name] of [[1440, 'implementation-desktop'], [390, 'implementation-narrow']]) {
        await page.setViewportSize({ width, height: 1000 });
        await page.reload();
        await page.locator('.CodeMirror').waitFor({ state: 'visible' });
        await page.locator('.CodeMirror').evaluate((element) => {
            element.CodeMirror.setValue(`SELECT ?s ?p ?o WHERE {
                VALUES (?s ?p ?o) {
                    ("short" <http://example.org/p> <http://example.org/resource/with-a-very-long-name-001>)
                    ("a much longer literal value" <http://example.org/another-predicate> "tiny")
                    (<http://example.org/resource/subject> <http://example.org/predicate/with-a-long-name> "another long literal value")
                }
            }`);
        });
        await page.locator('#exec').click();
        await expect(page.frameLocator('#query-results-frame').locator('table.data tbody tr')).toHaveCount(3);
        await expect(page.locator('#query-results-status')).toBeEmpty();
        await page.screenshot({
            path: path.join(DESIGN_DIR, `${name}.png`),
            fullPage: true,
            animations: 'disabled',
            caret: 'hide'
        });
    }
});

test('captures the expanded query and result disclosures', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 1000 });
    await page.reload();
    await page.locator('.CodeMirror').waitFor({ state: 'visible' });
    await page.locator('.CodeMirror').evaluate(element => {
        element.CodeMirror.setValue('SELECT * WHERE { VALUES ?s { "one" "two" "three" } }');
    });
    await page.locator('#exec').click();
    const resultFrame = page.frameLocator('#query-results-frame');
    await expect(resultFrame.locator('table.data tbody tr')).toHaveCount(3);

    await page.locator('#query-options-toggle').press('Enter');
    await page.locator('#save-query-toggle').press('Enter');
    await resultFrame.locator('#query-result-download-toggle').press('Enter');
    await resultFrame.locator('#query-result-options-toggle').press('Enter');
    await expect(page.locator('#query-name')).toBeVisible();
    await expect(page.locator('#query-timeout')).toBeVisible();
    await expect(resultFrame.locator('#Accept')).toBeVisible();
    await expect(resultFrame.locator('#limit_query')).toBeVisible();
    await page.screenshot({
        path: path.join(DESIGN_DIR, 'implementation-expanded.png'),
        fullPage: true,
        animations: 'disabled',
        caret: 'hide'
    });
});

test('keeps a result pending while its XSL stylesheet response is held', async ({ page }) => {
    let releaseStylesheet;
    let stylesheetSeen = false;
    let holdStylesheet = false;
    const stylesheetGate = new Promise(resolve => {
        releaseStylesheet = resolve;
    });

    await page.route('**/rdf4j-workbench/transformations/tuple.xsl', async route => {
        if (holdStylesheet) {
            stylesheetSeen = true;
            await stylesheetGate;
        }
        await route.continue();
    });

    try {
        await page.locator('.CodeMirror').evaluate(element => {
            element.CodeMirror.setValue('SELECT * WHERE { VALUES ?s { "one" "two" "three" } }');
        });
        holdStylesheet = true;
        const startedAt = Date.now();
        await page.locator('#exec').click();
        await expect.poll(() => stylesheetSeen, { timeout: 5000 }).toBe(true);
        await expect.poll(() => Date.now() - startedAt, { timeout: 1000, intervals: [50] })
            .toBeGreaterThan(300);
        await expect(page.locator('#query-results-loading')).toBeVisible();
        await expect(page.locator('#query-request-id')).not.toHaveValue('');
        await expect(page.locator('#query-results-status')).toBeEmpty();
        releaseStylesheet();
        await expect(page.frameLocator('#query-results-frame').locator('table.data tbody tr'))
            .toHaveCount(3);
    } finally {
        releaseStylesheet();
    }
});

test('keeps navigation state and option controls coherent on a narrow query page', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 900 });
    await page.reload();
    await page.locator('.CodeMirror').waitFor({ state: 'visible' });
    const metrics = await page.evaluate(() => {
        const rect = selector => document.querySelector(selector).getBoundingClientRect();
        const queryLink = document.querySelector('#navigation a[href="query"]');
        const queryItem = queryLink.closest('li');
        const infer = rect('#infer');
        const inferLabel = rect('label[for="infer"]');
        const privateInput = rect('#save-private');
        const privateLabel = rect('label[for="save-private"]');
        return {
            logoTop: rect('#logo').top,
            contextTop: rect('#contentheader').top,
            queryBorderWidth: parseFloat(getComputedStyle(queryItem).borderLeftWidth),
            queryBackground: getComputedStyle(queryItem).backgroundColor,
            inferPairGap: Math.abs((infer.top + infer.bottom) / 2 - (inferLabel.top + inferLabel.bottom) / 2),
            privatePairGap: Math.abs((privateInput.top + privateInput.bottom) / 2
                - (privateLabel.top + privateLabel.bottom) / 2),
            inferLabelFor: document.querySelector('label[for="infer"]').htmlFor,
            privateLabelFor: document.querySelector('label[for="save-private"]').htmlFor
        };
    });

    expect(metrics.logoTop).toBeLessThanOrEqual(metrics.contextTop + 1);
    expect(metrics.queryBorderWidth).toBeGreaterThanOrEqual(2);
    expect(metrics.queryBackground).not.toBe('rgba(0, 0, 0, 0)');
    expect(metrics.inferPairGap).toBeLessThanOrEqual(1);
    expect(metrics.privatePairGap).toBeLessThanOrEqual(1);
    expect(metrics.inferLabelFor).toBe('infer');
    expect(metrics.privateLabelFor).toBe('save-private');
});

test('uses the shared SVG chevron across query and embedded result states', async ({ page }) => {
    fs.mkdirSync(CHEVRON_DIR, { recursive: true });

    for (const [width, height, name] of [[1440, 1000, 'desktop'], [390, 1000, 'mobile'], [320, 900, 'narrow']]) {
        await page.setViewportSize({ width, height });
        await page.goto(QUERY_URL);
        await page.locator('.CodeMirror').waitFor({ state: 'visible' });

        for (const selector of ['#query-options-toggle', '#save-query-toggle']) {
            expectChevron(await page.locator(selector).evaluate(readChevron));
        }
        if (width <= 768) {
            const menu = page.locator('#workbench-navigation-disclosure');
            const menuSummary = page.locator('#workbench-navigation-summary');
            await expect(menu).toHaveJSProperty('open', false);
            await expectChevronState(menuSummary, 18, 0);
            if (width === 390) {
                await page.screenshot({
                    path: path.join(CHEVRON_DIR, 'menu-mobile-closed.png'),
                    fullPage: true,
                    animations: 'disabled'
                });
            }
            await menuSummary.press('Enter');
            await expect(menu).toHaveJSProperty('open', true);
            await expectChevronState(menuSummary, 18, 180);
            if (width === 390) {
                await page.screenshot({
                    path: path.join(CHEVRON_DIR, 'menu-mobile-keyboard-focus.png'),
                    fullPage: true,
                    animations: 'disabled'
                });
                await menuSummary.evaluate(element => element.blur());
                await page.screenshot({
                    path: path.join(CHEVRON_DIR, 'menu-mobile-open.png'),
                    fullPage: true,
                    animations: 'disabled'
                });
            }
            await menuSummary.press('Enter');
            await expect(menu).toHaveJSProperty('open', false);
            await expectChevronState(menuSummary, 18, 0);
            await menuSummary.evaluate(element => element.blur());
        }

        const queryClosedPath = path.join(CHEVRON_DIR, `query-${name}-closed.png`);
        await page.screenshot({ path: queryClosedPath, fullPage: true, animations: 'disabled' });
        await page.locator('#query-options-toggle').press('Enter');
        await expect(page.locator('#query-options-toggle')).toHaveAttribute('aria-expanded', 'true');
        await expectChevronState(page.locator('#query-options-toggle'), 16, 180);
        if (width === 1440) {
            await page.screenshot({
                path: path.join(CHEVRON_DIR, 'query-desktop-options-keyboard-focus.png'),
                fullPage: true,
                animations: 'disabled'
            });
        }
        await page.locator('#query-options-toggle').evaluate(element => element.blur());
        await page.screenshot({
            path: path.join(CHEVRON_DIR, `query-${name}-options-open.png`),
            fullPage: true,
            animations: 'disabled'
        });
        await page.locator('#query-options-toggle').press('Enter');
        await expect(page.locator('#query-options-toggle')).toHaveAttribute('aria-expanded', 'false');

        await page.locator('.CodeMirror').evaluate(element => {
            element.CodeMirror.setValue('SELECT * WHERE { VALUES ?s { "one" "two" "three" } }');
        });
        await page.locator('#exec').click();
        const frame = page.frameLocator('#query-results-frame');
        await expect(frame.locator('table.data tbody tr')).toHaveCount(3);
        for (const selector of ['#query-result-download-toggle', '#query-result-options-toggle']) {
            expectChevron(await frame.locator(selector).evaluate(readChevron));
        }
        await page.locator('#exec').evaluate(element => element.blur());
        await page.screenshot({
            path: path.join(CHEVRON_DIR, `embedded-result-${name}-closed.png`),
            fullPage: true,
            animations: 'disabled'
        });
        await frame.locator('#query-result-options-toggle').press('Enter');
        await expect(frame.locator('#query-result-options-toggle')).toHaveAttribute('aria-expanded', 'true');
        await expectChevronState(frame.locator('#query-result-options-toggle'), 16, 180);
        if (width === 390) {
            await page.screenshot({
                path: path.join(CHEVRON_DIR, 'embedded-result-mobile-options-keyboard-focus.png'),
                fullPage: true,
                animations: 'disabled'
            });
        }
        await frame.locator('#query-result-options-toggle').evaluate(element => element.blur());
        await page.screenshot({
            path: path.join(CHEVRON_DIR, `embedded-result-${name}-options-open.png`),
            fullPage: true,
            animations: 'disabled'
        });
        await frame.locator('#query-result-options-toggle').press('Enter');
        await frame.locator('#query-result-download-toggle').press('Enter');
        await expect(frame.locator('#Accept')).toBeVisible();
        await expectChevronState(frame.locator('#query-result-download-toggle'), 16, 180);
    }
});

test('uses shared chevrons for explanation, native details, and mobile navigation', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 1000 });
    await page.goto(QUERY_URL);
    await page.locator('.CodeMirror').waitFor({ state: 'visible' });
    await page.locator('.CodeMirror').evaluate(element => {
        element.CodeMirror.setValue('SELECT * WHERE { ?s ?p ?o } LIMIT 10');
    });
    await page.locator('#explain-trigger').click();
    await expect(page.locator('#compare-toggle')).toBeVisible({ timeout: 10000 });
    const explanationChevron = page.locator('#explanation-settings-toggle');
    await expect(explanationChevron).toBeVisible();
    expectChevron(await explanationChevron.evaluate(readChevron));
    await explanationChevron.press('Enter');
    await expect(explanationChevron).toHaveAttribute('aria-expanded', 'true');
    await expectChevronState(explanationChevron, 16, 180);

    const addUrl = `${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/add`;
    for (const [width, height, name] of [[1440, 1000, 'desktop'], [390, 1000, 'mobile'], [320, 900, 'narrow']]) {
        await page.setViewportSize({ width, height });
        await page.goto(addUrl);
        const details = page.locator('#add-import-settings');
        const summary = details.locator('summary');
        await expect(details).not.toHaveAttribute('open', '');
        expectChevron(await summary.evaluate(readChevron));
        if (width === 1440 || width === 390) {
            await page.screenshot({
                path: path.join(CHEVRON_DIR, `add-details-${name}-closed.png`),
                fullPage: true,
                animations: 'disabled'
            });
        }
        await summary.press('Enter');
        await expect(details).toHaveAttribute('open', '');
        await expectChevronState(summary, 16, 180);
        await summary.evaluate(element => element.blur());
        if (width === 1440 || width === 390) {
            await page.screenshot({
                path: path.join(CHEVRON_DIR, `add-details-${name}-open.png`),
                fullPage: true,
                animations: 'disabled'
            });
        }
        const overflow = await page.evaluate(() =>
            document.documentElement.scrollWidth <= document.documentElement.clientWidth + 1);
        expect(overflow).toBe(true);
    }
});

test('keeps the collapsed query toolbar compact and gives disclosures visible affordances', async ({ page }) => {
    const metrics = await page.evaluate(() => {
        const actionLabel = Array.from(document.querySelectorAll('.query-form__label'))
            .find(element => element.textContent.trim() === 'Actions');
        const rect = selector => document.querySelector(selector).getBoundingClientRect();
        const execute = rect('#exec');
        const explain = rect('#explain-trigger');
        const options = rect('#query-options-toggle');
        const save = rect('#save-query-toggle');
        const toolbar = document.querySelector('.query-actions-toolbar');
        const editorIcons = Array.from(document.querySelectorAll('.yasqe .yasqe_buttons .svgImg svg path'));
        return {
            actionLabelVisible: Boolean(actionLabel && actionLabel.getBoundingClientRect().height > 0),
            toolbar: Boolean(toolbar),
            toolbarHeight: toolbar ? toolbar.getBoundingClientRect().height : 0,
            maxControlGap: Math.max(explain.top - execute.bottom, options.top - explain.bottom, save.top - options.bottom),
            queryNameLabel: Boolean(document.querySelector('label[for="query-name"]')),
            editorIconFills: editorIcons.map(path => getComputedStyle(path).fill),
            editorIconStrokes: editorIcons.map(path => getComputedStyle(path).stroke)
        };
    });

    expect(metrics.actionLabelVisible).toBe(false);
    expect(metrics.toolbar).toBe(true);
    expect(metrics.toolbarHeight).toBeLessThan(100);
    expect(metrics.maxControlGap).toBeLessThan(24);
    const queryChevronMetrics = await Promise.all([
        page.locator('#query-options-toggle').evaluate(readChevron),
        page.locator('#save-query-toggle').evaluate(readChevron)
    ]);
    queryChevronMetrics.forEach(metrics => expectChevron(metrics));
    await page.locator('#query-options-toggle').press('Enter');
    await expect(page.locator('#query-options-toggle')).toHaveAttribute('aria-expanded', 'true');
    await expectChevronState(page.locator('#query-options-toggle'), 16, 180);
    await page.locator('#query-options-toggle').press('Enter');
    await expectChevronState(page.locator('#query-options-toggle'), 16, 0);
    expect(metrics.queryNameLabel).toBe(true);
    expect(metrics.editorIconFills.every(fill => fill === 'none' || fill === 'rgba(0, 0, 0, 0)')).toBe(true);
    expect(metrics.editorIconStrokes.every(stroke => stroke !== 'none')).toBe(true);

    await page.setViewportSize({ width: 390, height: 900 });
    await page.reload();
    await page.locator('.CodeMirror').waitFor({ state: 'visible' });
    const narrowMetrics = await page.evaluate(() => {
        const toolbar = document.querySelector('.query-actions-toolbar').getBoundingClientRect();
        const controls = ['#exec', '#explain-trigger', '#query-options-toggle', '#save-query-toggle']
            .map(selector => document.querySelector(selector).getBoundingClientRect());
        return {
            toolbarHeight: toolbar.height,
            controlsBottom: Math.max(...controls.map(control => control.bottom)),
            controlsTop: Math.min(...controls.map(control => control.top))
        };
    });
    expect(narrowMetrics.toolbarHeight).toBeLessThan(130);
    expect(narrowMetrics.controlsBottom - narrowMetrics.controlsTop).toBeLessThan(120);
});

test('sizes short results to content and caps large result scrolling', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 1200 });
    await page.reload();
    await page.locator('.CodeMirror').waitFor({ state: 'visible' });

    const setQueryAndExecute = async query => {
        await page.locator('.CodeMirror').evaluate((element, value) => element.CodeMirror.setValue(value), query);
        await page.locator('#exec').click();
        await expect(page.frameLocator('#query-results-frame').locator('table.data tbody tr').first())
            .toBeVisible();
    };

    await setQueryAndExecute('SELECT * WHERE { VALUES ?s { "one" "two" "three" } }');
    const shortMetrics = await page.locator('#query-results-frame').evaluate(frame => ({
        frameHeight: frame.getBoundingClientRect().height,
        documentHeight: frame.contentDocument.documentElement.scrollHeight,
        clientHeight: frame.contentDocument.documentElement.clientHeight
    }));

    const values = Array.from({ length: 80 }, (_, index) => `"row-${index}"`).join(' ');
    await setQueryAndExecute(`SELECT * WHERE { VALUES ?s { ${values} } }`);
    const largeMetrics = await page.locator('#query-results-frame').evaluate(frame => ({
        frameHeight: frame.getBoundingClientRect().height,
        documentHeight: frame.contentDocument.documentElement.scrollHeight,
        clientHeight: frame.contentDocument.documentElement.clientHeight
    }));

    expect(shortMetrics.frameHeight).toBeLessThan(520);
    expect(shortMetrics.frameHeight).toBeGreaterThanOrEqual(shortMetrics.documentHeight - 8);
    expect(largeMetrics.frameHeight).toBeLessThanOrEqual(640);
    expect(largeMetrics.documentHeight).toBeGreaterThan(largeMetrics.clientHeight + 100);
});

test('resizes result content through repeated disclosures and supports real frame scrolling', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 });
    await page.reload();
    await page.locator('.CodeMirror').waitFor({ state: 'visible' });
    const values = Array.from({ length: 80 }, (_, index) => `"row-${index}"`).join(' ');
    await page.locator('.CodeMirror').evaluate((element, value) => element.CodeMirror.setValue(value),
        `SELECT * WHERE { VALUES ?s { ${values} } }`);
    await page.locator('#exec').click();
    const frame = page.locator('#query-results-frame');
    await expect(page.frameLocator('#query-results-frame').locator('table.data tbody tr')).toHaveCount(80);

    const initial = await frame.evaluate(element => ({
        height: element.getBoundingClientRect().height,
        innerHeight: element.contentWindow.innerHeight,
        scrollHeight: element.contentDocument.documentElement.scrollHeight
    }));
    const resultFrame = page.frameLocator('#query-results-frame');
    await resultFrame.locator('#query-result-download-toggle').press('Enter');
    await resultFrame.locator('#query-result-options-toggle').press('Enter');
    await expect(resultFrame.locator('input[name="show-datatypes"]')).toBeVisible();
    await resultFrame.locator('input[name="show-datatypes"]').uncheck();
    await resultFrame.locator('#query-result-download-toggle').press('Enter');
    await resultFrame.locator('#query-result-options-toggle').press('Enter');
    await expect(resultFrame.locator('#query-result-options-toggle')).toHaveAttribute('aria-expanded', 'false');
    await expect(resultFrame.locator('#query-result-download-toggle')).toHaveAttribute('aria-expanded', 'false');

    await page.setViewportSize({ width: 1440, height: 500 });
    await expect.poll(() => frame.evaluate(element => element.getBoundingClientRect().height))
        .toBeLessThanOrEqual(375);
    await page.setViewportSize({ width: 1440, height: 1600 });
    await expect.poll(() => frame.evaluate(element => element.getBoundingClientRect().height)).toBeLessThanOrEqual(640);
    const afterClose = await frame.evaluate(element => ({
        height: element.getBoundingClientRect().height,
        innerHeight: element.contentWindow.innerHeight,
        scrollHeight: element.contentDocument.documentElement.scrollHeight
    }));
    await frame.hover({ position: { x: 300, y: Math.min(afterClose.height - 20, 500) } });
    await page.mouse.wheel(0, 9000);
    await expect.poll(() => frame.evaluate(element => Math.max(
        element.contentWindow.scrollY,
        element.contentDocument.documentElement.scrollTop,
        element.contentDocument.body ? element.contentDocument.body.scrollTop : 0
    ))).toBeGreaterThan(0);
    await expect(resultFrame.locator('table.data tbody tr').last()).toBeInViewport();

    expect(initial.scrollHeight).toBeGreaterThan(initial.innerHeight);
    expect(afterClose.height).toBeLessThanOrEqual(640);
    expect(afterClose.innerHeight).toBeLessThanOrEqual(640);
});

test('keeps three-column result tables aligned inside the narrow result frame', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 900 });
    await page.reload();
    await page.locator('.CodeMirror').waitFor({ state: 'visible' });
    const query = `SELECT ?s ?p ?o WHERE {
        VALUES (?s ?p ?o) {
            ("short" <http://example.org/p> <http://example.org/resource/with-a-very-long-name-001>)
            ("a much longer literal value" <http://example.org/another-predicate> "tiny")
            (<http://example.org/resource/subject> <http://example.org/predicate/with-a-long-name> "another long literal value")
        }
    }`;
    await page.locator('.CodeMirror').evaluate((element, value) => element.CodeMirror.setValue(value), query);
    await page.locator('#exec').click();
    await expect(page.frameLocator('#query-results-frame').locator('table.data thead th')).toHaveCount(3);

    const tableMetrics = await page.locator('#query-results-frame').evaluate(frame => {
        const documentElement = frame.contentDocument.documentElement;
        const resultSurface = frame.contentDocument.querySelector('#query-result-embedded');
        const table = frame.contentDocument.querySelector('table.data');
        const thead = frame.contentDocument.querySelector('table.data thead');
        const tbody = frame.contentDocument.querySelector('table.data tbody');
        return {
            tableDisplay: getComputedStyle(table).display,
            theadDisplay: getComputedStyle(thead).display,
            tbodyDisplay: getComputedStyle(tbody).display,
            tableWidth: table.getBoundingClientRect().width,
            frameWidth: frame.clientWidth,
            effectiveLayout: frame.contentDocument.querySelector('#query-result-layout').getAttribute('data-effective-layout'),
            recordsVisible: !frame.contentDocument.querySelector('#query-result-records').hidden,
            recordFieldCount: frame.contentDocument.querySelectorAll('#query-result-records .query-result-record__fields dd').length,
            bodyScrollWidth: documentElement.scrollWidth,
            bodyClientWidth: documentElement.clientWidth,
            resultSurfaceScrollWidth: resultSurface.scrollWidth,
            resultSurfaceClientWidth: resultSurface.clientWidth,
            tableWrapScrollWidth: frame.contentDocument.querySelector('#query-result-table-wrap').scrollWidth,
            tableWrapClientWidth: frame.contentDocument.querySelector('#query-result-table-wrap').clientWidth
        };
    });

    expect(tableMetrics.effectiveLayout).toBe('records');
    expect(tableMetrics.recordsVisible).toBe(true);
    expect(tableMetrics.recordFieldCount).toBe(9);
    expect(tableMetrics.tableDisplay).toBe('table');
    expect(tableMetrics.theadDisplay).toBe('table-header-group');
    expect(tableMetrics.tbodyDisplay).toBe('table-row-group');
    expect(tableMetrics.bodyScrollWidth).toBeLessThanOrEqual(tableMetrics.bodyClientWidth);
    expect(tableMetrics.resultSurfaceScrollWidth).toBeLessThanOrEqual(tableMetrics.resultSurfaceClientWidth);
    expect(tableMetrics.tableWrapScrollWidth).toBeLessThanOrEqual(tableMetrics.tableWrapClientWidth);

    const resultFrame = page.frameLocator('#query-results-frame');
    await resultFrame.locator('#query-result-options-toggle').press('Enter');
    await resultFrame.locator('#result-layout').selectOption('table');
    await resultFrame.locator('#result-wrap-values').uncheck();
    await expect(resultFrame.locator('#query-result-table-wrap')).toBeVisible();
    const explicitTableMetrics = await page.locator('#query-results-frame').evaluate(frame => {
        const table = frame.contentDocument.querySelector('table.data');
        const tableWrap = frame.contentDocument.querySelector('#query-result-table-wrap');
        return {
            tableWidth: table.getBoundingClientRect().width,
            frameWidth: frame.clientWidth,
            tableWrapScrollWidth: tableWrap.scrollWidth,
            tableWrapClientWidth: tableWrap.clientWidth
        };
    });
    expect(explicitTableMetrics.tableWidth).toBeGreaterThanOrEqual(explicitTableMetrics.frameWidth - 1);
    await expect.poll(() => page.locator('#query-results-frame').evaluate(frame => {
        const tableWrap = frame.contentDocument.querySelector('#query-result-table-wrap');
        return tableWrap.scrollWidth - tableWrap.clientWidth;
    })).toBeGreaterThan(0);
});

test('keeps query and result controls discoverable through keyboard disclosures', async ({ page }) => {
    const disclosureState = await page.evaluate(() => ({
        queryOptions: document.querySelector('#query-options-disclosure'),
        saveQuery: document.querySelector('#save-query-disclosure'),
        optionsToggle: document.querySelector('#query-options-toggle'),
        saveToggle: document.querySelector('#save-query-toggle')
    }));
    expect(disclosureState.queryOptions).toBeTruthy();
    expect(disclosureState.saveQuery).toBeTruthy();
    expect(disclosureState.optionsToggle).toBeTruthy();
    expect(disclosureState.saveToggle).toBeTruthy();
    await expect(page.locator('#query-options-toggle')).toHaveAttribute('aria-expanded', 'false');
    await expect(page.locator('#save-query-toggle')).toHaveAttribute('aria-expanded', 'false');
    await expect(page.locator('#limit_query')).toBeHidden();
    await expect(page.locator('#save')).toBeHidden();

    await page.locator('#query-options-toggle').focus();
    await page.keyboard.press('Enter');
    await expect(page.locator('#query-options-toggle')).toHaveAttribute('aria-expanded', 'true');
    await expect(page.locator('#limit_query')).toBeVisible();
    await expect(page.locator('#query-timeout')).toBeVisible();
    await expect(page.locator('#infer')).toBeVisible();
    await page.locator('#limit_query').selectOption({ value: '50' });

    await page.locator('#save-query-toggle').press('Enter');
    await expect(page.locator('#save-query-toggle')).toHaveAttribute('aria-expanded', 'true');
    await expect(page.locator('#query-name')).toBeVisible();
    await expect(page.locator('#save-private')).toBeVisible();

    await page.locator('#query-options-toggle').press('Enter');
    await expect(page.locator('#query-options-toggle')).toHaveAttribute('aria-expanded', 'false');
    await page.locator('#save-query-toggle').press('Enter');
    await expect(page.locator('#save-query-toggle')).toHaveAttribute('aria-expanded', 'false');

    await page.locator('.CodeMirror').evaluate(element => {
        element.CodeMirror.setValue('ASK { ?s ?p ?o }');
    });
    const executionRequest = page.waitForRequest(request =>
        request.url().includes('/repositories/query-refresh-layout/query')
        && ['GET', 'POST'].includes(request.method())
    );
    await page.locator('#exec').click();
    const request = await executionRequest;
    const requestParameters = new URL(request.url()).searchParams;
    if (request.method() === 'POST') {
        const postParameters = new URLSearchParams(request.postData() || '');
        expect(postParameters.get('limit_query')).toBe('50');
        expect(postParameters.get('embedded')).toBe('true');
    } else {
        expect(requestParameters.get('limit_query')).toBe('50');
        expect(requestParameters.get('embedded')).toBe('true');
    }
});

test('keeps embedded result paging visible and groups download controls', async ({ page }) => {
    await page.locator('.CodeMirror').evaluate(element => {
        element.CodeMirror.setValue('SELECT * WHERE { VALUES ?s { "one" "two" "three" } }');
    });
    await page.locator('#exec').click();
    const frame = page.frameLocator('#query-results-frame');
    await expect(frame.locator('table.data tbody tr')).toHaveCount(3);
    await expect(frame.locator('#query-result-download-disclosure')).toHaveCount(1);
    await expect(frame.locator('#query-result-options-disclosure')).toHaveCount(1);
    await expect(frame.locator('#query-result-embedded-header')).toHaveCount(1);
    await expect(frame.locator('#query-result-embedded-header').locator('h2')).toHaveText(/Query Result/);
    await expect(frame.locator('#query-result-download-toggle')).toHaveAttribute('aria-expanded', 'false');
    await expect(frame.locator('#query-result-options-toggle')).toHaveAttribute('aria-expanded', 'false');
    await expect(frame.locator('#nextX')).toBeVisible();
    const navigationFollowsTable = await frame.locator('#query-result-layout').evaluate(layout => {
        const navigation = layout.parentElement.querySelector('.query-result-navigation');
        return !!navigation && !!(layout.compareDocumentPosition(navigation) & Node.DOCUMENT_POSITION_FOLLOWING);
    });
    expect(navigationFollowsTable).toBe(true);
    const resultChevronMetrics = await Promise.all([
        frame.locator('#query-result-download-toggle').evaluate(readChevron),
        frame.locator('#query-result-options-toggle').evaluate(readChevron)
    ]);
    resultChevronMetrics.forEach(metrics => expectChevron(metrics));
    await frame.locator('#query-result-download-toggle').press('Enter');
    await expect(frame.locator('#Accept')).toBeVisible();
    await expectChevronState(frame.locator('#query-result-download-toggle'), 16, 180);
    await frame.locator('#query-result-options-toggle').press('Enter');
    await expect(frame.locator('#limit_query')).toBeVisible();
    await expectChevronState(frame.locator('#query-result-options-toggle'), 16, 180);
    await expect(frame.locator('#nextX')).toBeVisible();
});

test('uses one embedded result header and stacks open query disclosures', async ({ page }) => {
    await page.locator('.CodeMirror').evaluate(element => {
        element.CodeMirror.setValue('SELECT * WHERE { VALUES ?s { "one" "two" } }');
    });
    await page.locator('#exec').click();
    const frame = page.frameLocator('#query-results-frame');
    await expect(frame.locator('table.data tbody tr')).toHaveCount(2);

    await expect(page.locator('.query-results__header')).toBeHidden();
    await expect(frame.locator('#query-result-embedded-header')).toHaveCount(1);

    await page.locator('#query-options-toggle').press('Enter');
    await page.locator('#save-query-toggle').press('Enter');
    const disclosureMetrics = await page.evaluate(() => {
        const toolbar = document.querySelector('.query-actions-toolbar').getBoundingClientRect();
        const save = document.querySelector('#save-query-panel').getBoundingClientRect();
        const options = document.querySelector('#query-options-panel').getBoundingClientRect();
        return {
            toolbarWidth: toolbar.width,
            saveWidth: save.width,
            optionsWidth: options.width,
            saveTop: save.top,
            optionsTop: options.top,
            saveBottom: save.bottom
        };
    });

    expect(disclosureMetrics.saveWidth).toBeGreaterThanOrEqual(disclosureMetrics.toolbarWidth * 0.8);
    expect(disclosureMetrics.optionsWidth).toBeGreaterThanOrEqual(disclosureMetrics.toolbarWidth * 0.8);
    expect(disclosureMetrics.optionsTop).toBeGreaterThanOrEqual(disclosureMetrics.saveBottom - 1);
    expect(disclosureMetrics.saveTop).toBeGreaterThanOrEqual(0);
});

test('keeps the embedded result header aligned when result disclosures open', async ({ page }) => {
    await page.locator('.CodeMirror').evaluate(element => {
        element.CodeMirror.setValue('SELECT * WHERE { VALUES ?s { "one" "two" } }');
    });
    await page.locator('#exec').click();
    const frame = page.frameLocator('#query-results-frame');
    await expect(frame.locator('table.data tbody tr')).toHaveCount(2);
    await frame.locator('#query-result-download-toggle').press('Enter');
    const metrics = await page.locator('#query-results-frame').evaluate(frameElement => {
        const toolbar = frameElement.contentDocument.querySelector('.query-result-toolbar');
        const title = frameElement.contentDocument.querySelector('#query-result-embedded-header h2');
        const firstAction = frameElement.contentDocument.querySelector('#query-result-download-toggle');
        return {
            toolbarTop: toolbar.getBoundingClientRect().top,
            titleTop: title.getBoundingClientRect().top,
            titleBottom: title.getBoundingClientRect().bottom,
            firstActionTop: firstAction.getBoundingClientRect().top,
            firstActionBottom: firstAction.getBoundingClientRect().bottom
        };
    });

    expect(Math.abs(metrics.titleTop - metrics.toolbarTop)).toBeLessThanOrEqual(2);
    expect(Math.abs(metrics.firstActionTop - metrics.toolbarTop)).toBeLessThanOrEqual(2);
    expect(metrics.titleBottom).toBeLessThanOrEqual(metrics.firstActionBottom);
});

test('keeps the embedded result title readable on a narrow viewport', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 900 });
    await page.reload();
    await page.locator('.CodeMirror').waitFor({ state: 'visible' });
    await page.locator('.CodeMirror').evaluate(element => {
        element.CodeMirror.setValue('SELECT * WHERE { VALUES ?s { "one" "two" } }');
    });
    await page.locator('#exec').click();
    const frame = page.frameLocator('#query-results-frame');
    await expect(frame.locator('table.data tbody tr')).toHaveCount(2);
    await expect(frame.locator('#query-result-embedded-header h2')).toHaveText(/Query Result/);
    const metrics = await page.locator('#query-results-frame').evaluate(frameElement => {
        const header = frameElement.contentDocument.querySelector('#query-result-embedded-header');
        const title = header.querySelector('h2');
        const style = getComputedStyle(title);
        return {
            titleText: title.textContent.trim(),
            titleWidth: title.getBoundingClientRect().width,
            titleHeight: title.getBoundingClientRect().height,
            lineHeight: parseFloat(style.lineHeight),
            horizontalOverflow: frameElement.contentDocument.documentElement.scrollWidth
                - frameElement.contentDocument.documentElement.clientWidth
        };
    });

    expect(metrics.titleText).toMatch(/Query Result/);
    expect(metrics.titleWidth).toBeGreaterThanOrEqual(40);
    expect(metrics.titleHeight).toBeGreaterThanOrEqual(metrics.lineHeight - 1);
    expect(metrics.horizontalOverflow).toBeLessThanOrEqual(1);
});

function nativeRepositoryConfig(repositoryId) {
    return `@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix config: <tag:rdf4j.org,2023:config/>.

[] a config:Repository ;
   config:rep.id "${repositoryId}" ;
   rdfs:label "Query refresh layout test" ;
   config:rep.impl [
      config:rep.type "openrdf:SailRepository" ;
      config:sail.impl [
         config:sail.type "openrdf:MemoryStore"
      ]
   ].
`;
}
