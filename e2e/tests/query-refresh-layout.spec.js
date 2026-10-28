// @ts-check
const { test, expect } = require('@playwright/test');
const {
    createSeededRepository,
    deleteRepository,
    repositoryPageUrl,
    runQuery,
    serverBaseUrl,
    uniqueRepositoryId
} = require('./workbench-test-helpers.js');

// Retired with the redesign (see .agent/execplans/workbench-stale-spec-migration-20261002.md):
// - "keeps a result pending while its XSL stylesheet response is held": the Workbench no longer uses XSL stylesheets
//   (.agent/execplans/workbench-xslt-removal.md); a streamed result that is still arriving keeps the result busy, which
//   workbench-query-load-more.spec.js "query progress keeps loaded rows browsable and Load more waits for terminal
//   metadata" checks.
// - "sizes short results to content and caps large result scrolling" and "resizes result content through repeated
//   disclosures and supports real frame scrolling" measured the retired result iframe; results now scroll with the page
//   (app-shell plan, decision M4.1), which workbench-result-scrolling.spec.js "results scroll with the page under a
//   pinned header" checks down to the last row, and workbench-dropdown-detail-parity.spec.js "dynamic result details
//   keep distinct triggers and responsive Format-style fields" opens and closes the Download and Display panes in turn.
// - "keeps embedded result paging visible and groups download controls": the Next/Previous paging and the result's
//   "results per page" control were replaced by Load more (workbench-query-load-more.spec.js "Execute requests one
//   million rows by default and renders no result page controls"); the Download and Display panes are checked by
//   workbench-dropdown-detail-parity.spec.js ("dynamic result details ...") and their chevrons by "uses the shared SVG
//   chevron across query and embedded result states" below.
// - "keeps the embedded result header aligned when result disclosures open" and "keeps the embedded result title
//   readable on a narrow viewport": the result's "Query result" heading is visually hidden in the output card (app-shell
//   plan, decision M3.5); the result toolbar row is checked by workbench-dropdown-detail-parity.spec.js ("dynamic result
//   details ...") and workbench-settings-panes.spec.js (a result pane opens without moving its toolbar).
// Screenshots go to the test output directory instead of the design folders they were first captured for.

const REPOSITORY_ID = uniqueRepositoryId('query-refresh-layout');
const QUERY_URL = repositoryPageUrl(REPOSITORY_ID, 'query');

/** The streamed result on the Query page (it replaced the result iframe). */
function resultRoot(page) {
    return page.locator('#query-results [data-query-stream-root]');
}

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

test.beforeAll(async ({ request }) => {
    await createSeededRepository(request, serverBaseUrl(), REPOSITORY_ID,
        { graphs: [], label: 'Query refresh layout test' });
});

test.afterAll(async ({ request }) => {
    await deleteRepository(request, serverBaseUrl(), REPOSITORY_ID);
});

test.beforeEach(async ({ page }) => {
    await page.goto(QUERY_URL);
    await page.locator('.CodeMirror').waitFor({ state: 'visible' });
});

test('desktop query surfaces use flat hierarchy and readable controls', async ({ page }) => {
    const metrics = await page.evaluate(() => {
        const execute = document.querySelector('#exec');
        // The result card is the output card that holds the Results and Explanation tabs.
        const results = document.querySelector('#query-output');
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
        await runQuery(page, 'ASK { VALUES ?s { <http://example.org/alice> } }');
        // On narrow screens the menu is the Menu button in the context bar (app-shell plan M2.8); the side menu is not
        // shown beside the content.
        await expect(page.locator('#workbench-menu-button')).toBeVisible();
        await expect(page.locator('#workbench-navigation-disclosure')).toBeHidden();
        const metrics = await page.evaluate(() => {
            const navigation = document.querySelector('#workbench-menu-button').getBoundingClientRect();
            const content = document.querySelector('#content').getBoundingClientRect();
            // The result card is the output card that holds the Results and Explanation tabs.
            const results = document.querySelector('#query-output').getBoundingClientRect();
            return {
                clientWidth: document.documentElement.clientWidth,
                scrollWidth: document.documentElement.scrollWidth,
                // The width left for the page once a classic scrollbar's stable gutter is reserved.
                layoutWidth: document.body.clientWidth,
                navigationBottom: navigation.bottom,
                contentTop: content.top,
                contentWidth: content.width,
                resultsWidth: results.width
            };
        });

        expect(metrics.scrollWidth).toBeLessThanOrEqual(metrics.clientWidth + 1);
        expect(metrics.navigationBottom).toBeLessThanOrEqual(metrics.contentTop + 1);
        expect(metrics.contentWidth).toBeGreaterThanOrEqual(metrics.layoutWidth - 32);
        expect(metrics.resultsWidth).toBeGreaterThanOrEqual(metrics.layoutWidth - 32);
    }
});

test('captures the stacked query and embedded result surfaces', async ({ page }, testInfo) => {
    for (const [width, name] of [[1440, 'implementation-desktop'], [390, 'implementation-narrow']]) {
        await page.setViewportSize({ width, height: 1000 });
        await page.reload();
        await page.locator('.CodeMirror').waitFor({ state: 'visible' });
        await runQuery(page, `SELECT ?s ?p ?o WHERE {
                VALUES (?s ?p ?o) {
                    ("short" <http://example.org/p> <http://example.org/resource/with-a-very-long-name-001>)
                    ("a much longer literal value" <http://example.org/another-predicate> "tiny")
                    (<http://example.org/resource/subject> <http://example.org/predicate/with-a-long-name> "another long literal value")
                }
            }`);
        // Auto shows the rows as a table on the desktop and as records on the narrow page.
        await expect(resultRoot(page).locator('table.data tbody tr[data-query-row-index], [data-query-record-index]'))
            .toHaveCount(3);
        await expect(page.locator('#query-results-status')).toBeEmpty();
        await page.screenshot({
            path: testInfo.outputPath(`${name}.png`),
            fullPage: true,
            animations: 'disabled',
            caret: 'hide'
        });
    }
});

test('captures the expanded query and result disclosures', async ({ page }, testInfo) => {
    await page.setViewportSize({ width: 1440, height: 1000 });
    await page.reload();
    await page.locator('.CodeMirror').waitFor({ state: 'visible' });
    await runQuery(page, 'SELECT * WHERE { VALUES ?s { "one" "two" "three" } }');
    const result = resultRoot(page);
    await expect(result.locator('table.data tbody tr')).toHaveCount(3);

    await page.locator('#query-options-toggle').press('Enter');
    await expect(page.locator('#query-options-toggle')).toHaveAttribute('aria-expanded', 'true');
    await expect(page.locator('#query-timeout')).toBeVisible();
    await expect(page.locator('#query-name')).toBeHidden();
    await result.locator('.query-result-download-toggle').press('Enter');
    await expect(result.locator('select[name="Accept"]')).toBeVisible();
    await expect(result.locator('select[name="result-layout"]')).toBeHidden();
    await page.screenshot({
        path: testInfo.outputPath('implementation-query-options-and-download.png'),
        fullPage: true,
        animations: 'disabled',
        caret: 'hide'
    });

    await page.locator('#save-query-toggle').press('Enter');
    await expect(page.locator('#save-query-toggle')).toHaveAttribute('aria-expanded', 'true');
    await expect(page.locator('#query-options-toggle')).toHaveAttribute('aria-expanded', 'false');
    await expect(page.locator('#query-name')).toBeVisible();
    await expect(page.locator('#query-timeout')).toBeHidden();
    await result.locator('.query-result-options-toggle').press('Enter');
    await expect(result.locator('.query-result-options-toggle')).toHaveAttribute('aria-expanded', 'true');
    await expect(result.locator('.query-result-download-toggle')).toHaveAttribute('aria-expanded', 'false');
    await expect(result.locator('select[name="result-layout"]')).toBeVisible();
    await expect(result.locator('select[name="Accept"]')).toBeHidden();
    await page.screenshot({
        path: testInfo.outputPath('implementation-expanded.png'),
        fullPage: true,
        animations: 'disabled',
        caret: 'hide'
    });
});

test('keeps navigation state and option controls coherent on a narrow query page', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 900 });
    await page.reload();
    await page.locator('.CodeMirror').waitFor({ state: 'visible' });
    // The narrow menu is the sheet opened by the Menu button (app-shell plan M2.8); the logo and the repository
    // switcher share the one-row context bar, which workbench-navigation.spec.js ("mobile header omits the theme
    // selector and keeps server context reachable") checks.
    await page.locator('#workbench-menu-button').click();
    const queryLink = page.locator('#workbench-menu-sheet a[aria-current="page"]');
    await expect(queryLink).toBeVisible();
    await expect(queryLink).toHaveText(/Query/);
    const navigation = await queryLink.evaluate(link => {
        const style = getComputedStyle(link);
        return { borderWidth: parseFloat(style.borderLeftWidth), background: style.backgroundColor };
    });
    await page.keyboard.press('Escape');
    await expect(page.locator('#workbench-menu-sheet')).toBeHidden();

    const pairGeometry = (inputSelector) => page.evaluate((selector) => {
        const input = document.querySelector(selector);
        const label = input.labels[0];
        const inputRect = input.getBoundingClientRect();
        const labelRect = label.getBoundingClientRect();
        return {
            labels: input.labels.length,
            visible: inputRect.width > 0 && labelRect.width > 0,
            gap: Math.abs((inputRect.top + inputRect.bottom) / 2 - (labelRect.top + labelRect.bottom) / 2)
        };
    }, inputSelector);
    await page.locator('#query-options-toggle').click();
    await expect(page.locator('#infer')).toBeVisible();
    const infer = await pairGeometry('#infer');
    await page.locator('#save-query-toggle').click();
    await expect(page.locator('#save-private')).toBeVisible();
    const privateInput = await pairGeometry('#save-private');

    expect(navigation.borderWidth).toBeGreaterThanOrEqual(2);
    expect(navigation.background).not.toBe('rgba(0, 0, 0, 0)');
    expect(infer.visible).toBe(true);
    expect(infer.gap).toBeLessThanOrEqual(1);
    expect(privateInput.visible).toBe(true);
    expect(privateInput.gap).toBeLessThanOrEqual(1);
    expect(infer.labels).toBe(1);
    expect(privateInput.labels).toBe(1);
});

test('stacks query option labels and contains controls on desktop and mobile', async ({ page }) => {
    // "Query settings" no longer holds a results-per-page select (results load with Load more) or the "Clear" button
    // ("Insert prefixes" replaced it in app-shell plan task M3.4).
    await page.setViewportSize({ width: 1440, height: 900 });
    await page.reload();
    await page.locator('.CodeMirror').waitFor({ state: 'visible' });
    await page.locator('#query-options-toggle').press('Enter');
    await expect(page.locator('#query-options-toggle')).toHaveAttribute('aria-expanded', 'true');

    const desktop = await page.evaluate(() => {
        const bounds = selector => document.querySelector(selector).getBoundingClientRect();
        const controls = [
            bounds('#query-timeout'),
            document.querySelector('#infer').parentElement.getBoundingClientRect(),
            bounds('#query-options-panel .workbench-disclosure__actions button')
        ];
        const stackedLabels = [
            ['label[for="query-timeout"]', '#query-timeout']
        ].map(([labelSelector, controlSelector]) => ({
            labelBottom: bounds(labelSelector).bottom,
            controlTop: bounds(controlSelector).top
        }));
        return {
            panel: bounds('#query-options-panel'),
            controls,
            stackedLabels,
            timeoutLabelFor: document.querySelector('#query-options-panel label[for="query-timeout"]')?.htmlFor ?? null,
            inferredLabelFor: document.querySelector('#query-options-panel label[for="infer"]')?.htmlFor ?? null
        };
    });

    console.log('Desktop query options geometry:', desktop);
    for (const field of desktop.stackedLabels) {
        expect(field.labelBottom).toBeLessThanOrEqual(field.controlTop + 1);
    }
    for (const control of desktop.controls) {
        expect(control.left).toBeGreaterThanOrEqual(desktop.panel.left + 8);
        expect(control.right).toBeLessThanOrEqual(desktop.panel.right - 8);
    }
    expect(desktop.timeoutLabelFor).toBe('query-timeout');
    expect(desktop.inferredLabelFor).toBe('infer');

    const infer = page.locator('#infer');
    const inferredBeforeLabelClick = await infer.isChecked();
    await page.locator('label[for="infer"]').click();
    expect(await infer.isChecked()).toBe(!inferredBeforeLabelClick);

    const timeout = page.locator('#query-timeout');
    await page.locator('label[for="query-timeout"]').click();
    await expect(timeout).toBeFocused();
    await timeout.fill('23');
    await expect(timeout).toHaveValue('23');

    for (const width of [390, 320]) {
        await page.setViewportSize({ width, height: 900 });
        await page.reload();
        await page.locator('.CodeMirror').waitFor({ state: 'visible' });
        await page.locator('#query-options-toggle').press('Enter');
        await expect(page.locator('#query-options-panel')).toBeVisible();
        const narrow = await page.evaluate(() => {
            const panel = document.querySelector('#query-options-panel').getBoundingClientRect();
            const settingsElement = document.querySelector('#query-options-panel .workbench-disclosure__fields');
            const settings = settingsElement.getBoundingClientRect();
            const childRects = [
                'label[for="query-timeout"]', '#query-timeout',
                '.query-option', '#infer',
                '.workbench-disclosure__actions button'
            ].map(selector => settingsElement.querySelector(selector).getBoundingClientRect());
            return {
                clientWidth: document.documentElement.clientWidth,
                scrollWidth: document.documentElement.scrollWidth,
                panelRight: panel.right,
                settingsRight: settings.right,
                settingsScrollWidth: settingsElement.scrollWidth,
                settingsClientWidth: settingsElement.clientWidth,
                childOverflow: Math.max(...childRects.map(rect => rect.right)) - settings.right
            };
        });

        console.log(`Narrow query options geometry (${width}px):`, narrow);
        expect(narrow.scrollWidth).toBeLessThanOrEqual(narrow.clientWidth + 1);
        expect(narrow.settingsScrollWidth).toBeLessThanOrEqual(narrow.settingsClientWidth + 1);
        expect(narrow.settingsRight).toBeLessThanOrEqual(narrow.panelRight + 1);
        expect(narrow.childOverflow).toBeLessThanOrEqual(1);
    }
});

test('uses the shared SVG chevron across query and embedded result states', async ({ page }, testInfo) => {
    // The narrow side-menu disclosure with its own chevron became the Menu button and sheet (app-shell plan M2.8),
    // which workbench-shell.spec.js ("on a phone the header is one compact bar and the menu opens as a sheet") checks.
    for (const [width, height, name] of [[1440, 1000, 'desktop'], [390, 1000, 'mobile'], [320, 900, 'narrow']]) {
        await page.setViewportSize({ width, height });
        await page.goto(QUERY_URL);
        await page.locator('.CodeMirror').waitFor({ state: 'visible' });

        for (const selector of ['#query-options-toggle', '#save-query-toggle']) {
            expectChevron(await page.locator(selector).evaluate(readChevron));
        }

        await page.screenshot({ path: testInfo.outputPath(`query-${name}-closed.png`), fullPage: true, animations: 'disabled' });
        await page.locator('#query-options-toggle').press('Enter');
        await expect(page.locator('#query-options-toggle')).toHaveAttribute('aria-expanded', 'true');
        await expectChevronState(page.locator('#query-options-toggle'), 16, 180);
        if (width === 1440) {
            await page.screenshot({
                path: testInfo.outputPath('query-desktop-options-keyboard-focus.png'),
                fullPage: true,
                animations: 'disabled'
            });
        }
        await page.locator('#query-options-toggle').evaluate(element => element.blur());
        await page.screenshot({
            path: testInfo.outputPath(`query-${name}-options-open.png`),
            fullPage: true,
            animations: 'disabled'
        });
        await page.locator('#query-options-toggle').press('Enter');
        await expect(page.locator('#query-options-toggle')).toHaveAttribute('aria-expanded', 'false');

        await runQuery(page, 'SELECT * WHERE { VALUES ?s { "one" "two" "three" } }');
        const result = resultRoot(page);
        await expect(result.locator('table.data tbody tr')).toHaveCount(3);
        for (const selector of ['.query-result-download-toggle', '.query-result-options-toggle']) {
            expectChevron(await result.locator(selector).evaluate(readChevron));
        }
        if (width === 320) {
            const toolbar = await result.locator('.query-result-toolbar').evaluate(element => {
                const toolbarBounds = element.getBoundingClientRect();
                const triggers = Array.from(element.querySelectorAll(
                    '.query-result-download-toggle, .query-result-options-toggle, [id^="query-result-fullscreen-"]'))
                    .map(trigger => {
                        const bounds = trigger.getBoundingClientRect();
                        return {
                            id: trigger.id,
                            left: bounds.left,
                            right: bounds.right
                        };
                    });
                return {
                    width: toolbarBounds.width,
                    clientWidth: element.clientWidth,
                    scrollWidth: element.scrollWidth,
                    left: toolbarBounds.left,
                    right: toolbarBounds.right,
                    triggers
                };
            });
            expect(toolbar.triggers).toHaveLength(3);
            expect(toolbar.scrollWidth).toBeLessThanOrEqual(toolbar.clientWidth + 1);
            for (const trigger of toolbar.triggers) {
                expect(trigger.left, `${trigger.id} starts outside the result toolbar`).toBeGreaterThanOrEqual(toolbar.left - 1);
                expect(trigger.right, `${trigger.id} ends outside the result toolbar`).toBeLessThanOrEqual(toolbar.right + 1);
            }
        }
        await page.locator('#exec').evaluate(element => element.blur());
        await page.screenshot({
            path: testInfo.outputPath(`embedded-result-${name}-closed.png`),
            fullPage: true,
            animations: 'disabled'
        });
        const optionsToggle = result.locator('.query-result-options-toggle');
        const downloadToggle = result.locator('.query-result-download-toggle');
        await optionsToggle.press('Enter');
        await expect(optionsToggle).toHaveAttribute('aria-expanded', 'true');
        await expectChevronState(optionsToggle, 16, 180);
        if (width === 390) {
            await page.screenshot({
                path: testInfo.outputPath('embedded-result-mobile-options-keyboard-focus.png'),
                fullPage: true,
                animations: 'disabled'
            });
        }
        await optionsToggle.evaluate(element => element.blur());
        await page.screenshot({
            path: testInfo.outputPath(`embedded-result-${name}-options-open.png`),
            fullPage: true,
            animations: 'disabled'
        });
        await optionsToggle.press('Enter');
        await downloadToggle.press('Enter');
        await expect(result.locator('select[name="Accept"]')).toBeVisible();
        await expectChevronState(downloadToggle, 16, 180);
    }
});

test('uses shared chevrons for explanation, native details, and mobile navigation', async ({ page }, testInfo) => {
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

    const addUrl = repositoryPageUrl(REPOSITORY_ID, 'add');
    for (const [width, height, name] of [[1440, 1000, 'desktop'], [390, 1000, 'mobile'], [320, 900, 'narrow']]) {
        await page.setViewportSize({ width, height });
        await page.goto(addUrl);
        const panel = page.locator('#add-import-settings-panel');
        const toggle = page.locator('#add-import-settings-toggle');
        await expect(panel).toBeHidden();
        expectChevron(await toggle.evaluate(readChevron));
        if (width === 1440 || width === 390) {
            await page.screenshot({
                path: testInfo.outputPath(`add-details-${name}-closed.png`),
                fullPage: true,
                animations: 'disabled'
            });
        }
        await toggle.press('Enter');
        await expect(toggle).toHaveAttribute('aria-expanded', 'true');
        await expect(panel).toBeVisible();
        await expectChevronState(toggle, 16, 180);
        await toggle.evaluate(element => element.blur());
        if (width === 1440 || width === 390) {
            await page.screenshot({
                path: testInfo.outputPath(`add-details-${name}-open.png`),
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

test('keeps three-column result tables aligned inside the narrow result', async ({ page }) => {
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
    await runQuery(page, query);
    const result = resultRoot(page);
    await expect(result.locator('table.data thead th')).toHaveCount(3);

    const tableMetrics = await result.evaluate(resultSurface => {
        const documentElement = document.documentElement;
        const table = resultSurface.querySelector('[id^="query-result-table-wrap-"] table.data');
        const thead = table.querySelector('thead');
        const tbody = table.querySelector('tbody');
        const tableWrap = resultSurface.querySelector('[id^="query-result-table-wrap-"]');
        const records = resultSurface.querySelector('[id^="query-result-records-"]');
        return {
            tableDisplay: getComputedStyle(table).display,
            theadDisplay: getComputedStyle(thead).display,
            tbodyDisplay: getComputedStyle(tbody).display,
            tableWidth: table.getBoundingClientRect().width,
            effectiveLayout: resultSurface.getAttribute('data-effective-layout'),
            recordsVisible: !records.hidden,
            recordFieldCount: records.querySelectorAll('.query-result-record__fields dd').length,
            bodyScrollWidth: documentElement.scrollWidth,
            bodyClientWidth: documentElement.clientWidth,
            resultSurfaceScrollWidth: resultSurface.scrollWidth,
            resultSurfaceClientWidth: resultSurface.clientWidth,
            tableWrapScrollWidth: tableWrap.scrollWidth,
            tableWrapClientWidth: tableWrap.clientWidth
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

    await result.locator('.query-result-options-toggle').press('Enter');
    await result.locator('select[name="result-layout"]').selectOption('table');
    await result.locator('input[name="result-wrap-values"]').uncheck();
    await expect(result.locator('[id^="query-result-table-wrap-"]')).toBeVisible();
    const explicitTableMetrics = await result.evaluate(resultSurface => {
        const tableWrap = resultSurface.querySelector('[id^="query-result-table-wrap-"]');
        const table = tableWrap.querySelector('table.data');
        return {
            tableWidth: table.getBoundingClientRect().width,
            resultWidth: resultSurface.clientWidth,
            tableWrapScrollWidth: tableWrap.scrollWidth,
            tableWrapClientWidth: tableWrap.clientWidth
        };
    });
    expect(explicitTableMetrics.tableWidth).toBeGreaterThanOrEqual(explicitTableMetrics.resultWidth - 1);
    await expect.poll(() => result.evaluate(resultSurface => {
        const tableWrap = resultSurface.querySelector('[id^="query-result-table-wrap-"]');
        return tableWrap.scrollWidth - tableWrap.clientWidth;
    })).toBeGreaterThan(0);
});

test('keeps query and result controls discoverable through keyboard disclosures', async ({ page }) => {
    // "Query settings" no longer holds a results-per-page select (results load with Load more), so the setting sent
    // with the execution is the timeout; the iframe-only "embedded" request parameter is gone with the iframe.
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
    await expect(page.locator('#query-timeout')).toBeHidden();
    await expect(page.locator('#save')).toBeHidden();

    await page.locator('#query-options-toggle').focus();
    await page.keyboard.press('Enter');
    await expect(page.locator('#query-options-toggle')).toHaveAttribute('aria-expanded', 'true');
    await expect(page.locator('#query-timeout')).toBeVisible();
    await expect(page.locator('#infer')).toBeVisible();
    await page.locator('#query-timeout').fill('23');

    await page.locator('#save-query-toggle').press('Enter');
    await expect(page.locator('#save-query-toggle')).toHaveAttribute('aria-expanded', 'true');
    await expect(page.locator('#query-name')).toBeVisible();
    await expect(page.locator('#save-private')).toBeVisible();

    await page.locator('#query-options-toggle').press('Enter');
    await expect(page.locator('#query-options-toggle')).toHaveAttribute('aria-expanded', 'true');
    await expect(page.locator('#save-query-toggle')).toHaveAttribute('aria-expanded', 'false');
    await page.locator('#save-query-toggle').press('Enter');
    await expect(page.locator('#save-query-toggle')).toHaveAttribute('aria-expanded', 'true');
    await expect(page.locator('#query-options-toggle')).toHaveAttribute('aria-expanded', 'false');
    await page.locator('#save-query-toggle').press('Enter');
    await expect(page.locator('#save-query-toggle')).toHaveAttribute('aria-expanded', 'false');

    await page.locator('.CodeMirror').evaluate(element => {
        element.CodeMirror.setValue('ASK { ?s ?p ?o }');
    });
    const executionRequest = page.waitForRequest(request =>
        request.url().includes(`/repositories/${REPOSITORY_ID}/query`)
        && ['GET', 'POST'].includes(request.method())
    );
    await page.locator('#exec').click();
    const request = await executionRequest;
    const requestParameters = new URL(request.url()).searchParams;
    if (request.method() === 'POST') {
        const postParameters = new URLSearchParams(request.postData() || '');
        expect(postParameters.get('action')).toBe('exec');
        expect(postParameters.get('query-timeout')).toBe('23');
    } else {
        expect(requestParameters.get('action')).toBe('exec');
        expect(requestParameters.get('query-timeout')).toBe('23');
    }
});

test('uses one embedded result header and anchors the active query disclosure', async ({ page }) => {
    await runQuery(page, 'SELECT * WHERE { VALUES ?s { "one" "two" } }');
    const result = resultRoot(page);
    await expect(result.locator('table.data tbody tr')).toHaveCount(2);

    await expect(page.locator('.query-results__header')).toBeHidden();
    await expect(result.locator('.query-result-toolbar__header')).toHaveCount(1);

    const readDisclosureGeometry = async (toggleSelector, panelSelector) => page.evaluate(([toggleSelector, panelSelector]) => {
        const toolbar = document.querySelector('.query-actions-toolbar').getBoundingClientRect();
        const toggle = document.querySelector(toggleSelector).getBoundingClientRect();
        const panel = document.querySelector(panelSelector).getBoundingClientRect();
        return {
            toolbar: { left: toolbar.left, right: toolbar.right },
            toggleBottom: toggle.bottom,
            panel: { left: panel.left, right: panel.right, top: panel.top, width: panel.width }
        };
    }, [toggleSelector, panelSelector]);

    await page.locator('#query-options-toggle').press('Enter');
    await expect(page.locator('#query-options-toggle')).toHaveAttribute('aria-expanded', 'true');
    await expect(page.locator('#save-query-toggle')).toHaveAttribute('aria-expanded', 'false');
    const optionsGeometry = await readDisclosureGeometry('#query-options-toggle', '#query-options-panel');
    expect(optionsGeometry.panel.width).toBeGreaterThan(0);
    expect(optionsGeometry.panel.left).toBeGreaterThanOrEqual(optionsGeometry.toolbar.left - 1);
    expect(optionsGeometry.panel.right).toBeLessThanOrEqual(optionsGeometry.toolbar.right + 1);
    expect(optionsGeometry.panel.top).toBeGreaterThanOrEqual(optionsGeometry.toggleBottom - 1);

    await page.locator('#save-query-toggle').press('Enter');
    await expect(page.locator('#save-query-toggle')).toHaveAttribute('aria-expanded', 'true');
    await expect(page.locator('#query-options-toggle')).toHaveAttribute('aria-expanded', 'false');
    const saveGeometry = await readDisclosureGeometry('#save-query-toggle', '#save-query-panel');
    expect(saveGeometry.panel.width).toBeGreaterThan(0);
    expect(saveGeometry.panel.left).toBeGreaterThanOrEqual(saveGeometry.toolbar.left - 1);
    expect(saveGeometry.panel.right).toBeLessThanOrEqual(saveGeometry.toolbar.right + 1);
    expect(saveGeometry.panel.top).toBeGreaterThanOrEqual(saveGeometry.toggleBottom - 1);
});
