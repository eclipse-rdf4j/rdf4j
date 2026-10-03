// @ts-check
const { test, expect } = require('@playwright/test');
const {
	deleteRepository,
	memoryRepositoryConfiguration,
	openQueryPage,
	runQuery,
	serverBaseUrl,
	uniqueRepositoryId,
	workbenchBaseUrl
} = require('./workbench-test-helpers.js');

// The query result is streamed into the page since the redesign (plan workbench-app-shell-and-critique-fixes-20260930,
// M9.1 removed the result iframe), so the result tests read #query-results; the retired teal literal is compared with
// the --workbench-primary token instead. A Yes/No answer hides Full screen (M3.5), and long results continue with a
// secondary "Load more" button instead of Previous/Next paging.

const SERVER_BASE_URL = serverBaseUrl();
const WORKBENCH_BASE_URL = workbenchBaseUrl();
const REPOSITORY_ID = uniqueRepositoryId('workbench-second-review');
const REPOSITORY_BASE_URL = `${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}`;

/** The computed color of the --workbench-primary token, to compare with computed styles. */
async function primaryColor(page) {
	const color = await page.evaluate(() => {
		if (!getComputedStyle(document.documentElement).getPropertyValue('--workbench-primary').trim()) {
			return '';
		}
		const probe = document.createElement('span');
		probe.style.color = 'var(--workbench-primary)';
		document.body.append(probe);
		const value = getComputedStyle(probe).color;
		probe.remove();
		return value;
	});
	expect(color, 'the page defines the --workbench-primary token').not.toBe('');
	return color;
}

test.beforeAll(async ({ request }) => {
	const response = await request.put(`${SERVER_BASE_URL}/repositories/${REPOSITORY_ID}`, {
		headers: { 'Content-Type': 'text/turtle' },
		data: memoryRepositoryConfiguration(REPOSITORY_ID, 'Workbench second review fixture')
	});
	expect([200, 201, 204]).toContain(response.status());
	for (const [prefix, namespace] of [
		['ex', 'http://example.org/'],
		['schema', 'http://schema.org/']
	]) {
		const namespaceResponse = await request.put(
			`${SERVER_BASE_URL}/repositories/${REPOSITORY_ID}/namespaces/${prefix}`,
			{ headers: { 'Content-Type': 'text/plain' }, data: namespace }
		);
		expect([200, 201, 204]).toContain(namespaceResponse.status());
	}
	const statements = [
		'<http://example.org/alice> <http://example.org/name> "Alice has a deliberately long readable literal" <urn:workbench:graph-one> .',
		'<http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://schema.org/Person> <urn:workbench:graph-one> .',
		'<http://example.org/bob> <http://example.org/name> "Bob" <urn:workbench:graph-two> .',
		'<http://example.org/bob> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://schema.org/Person> <urn:workbench:graph-two> .'
	].join('\n');
	const statementsResponse = await request.post(`${SERVER_BASE_URL}/repositories/${REPOSITORY_ID}/statements`, {
		headers: { 'Content-Type': 'application/n-quads' },
		data: statements
	});
	expect([200, 201, 204]).toContain(statementsResponse.status());
});

test.afterAll(async ({ request }) => {
	await deleteRepository(request, SERVER_BASE_URL, REPOSITORY_ID);
});

test('shared actions expose semantic classes and composed outline icons', async ({ page }) => {
	await page.goto(`${REPOSITORY_BASE_URL}/update`, { waitUntil: 'domcontentloaded' });
	const updateAction = page.locator('#update-actions input[type="submit"]');
	await expect(updateAction).toBeVisible();
	const primary = await primaryColor(page);
	const actions = await updateAction.evaluate(element => ({
		className: element.closest('.workbench-action')?.className || '',
		backgroundImage: getComputedStyle(element).backgroundImage,
		parentHasIcon: Boolean(element.closest('.workbench-action')?.querySelector('.workbench-action-icon')),
		parentHasLabel: Boolean(element.closest('.workbench-action')?.querySelector('.workbench-action-label')),
		background: getComputedStyle(element.closest('.workbench-action')).backgroundColor
	}));
	expect(actions.className).toContain('workbench-action');
	expect(actions.backgroundImage).toBe('none');
	expect(actions.parentHasIcon).toBe(true);
	expect(actions.parentHasLabel).toBe(true);
	expect(actions.background, 'the Update action uses the Workbench primary color').toBe(primary);

	await page.goto(`${REPOSITORY_BASE_URL}/clear`, { waitUntil: 'domcontentloaded' });
	// Clear's button is a danger action that names its target (plan task M6.4).
	const clearAction = page.locator('#clear-form button[type="submit"]');
	await expect(clearAction).toHaveClass(/workbench-action--danger/);
	await expect(clearAction).not.toHaveCSS('background-color', primary);
});

test('creation actions use compact semantic wrappers and stay inset when Advanced opens', async ({ page }) => {
	await page.setViewportSize({ width: 390, height: 1000 });
	await page.goto(`${WORKBENCH_BASE_URL}/repositories/NONE/create?type=memory-rdfs-dt`, {
		waitUntil: 'domcontentloaded'
	});
	const form = page.locator('form[action="create"]');
	const cancel = form.locator('input[value="Cancel"]');
	const create = form.locator('#create');
	await expect(cancel).toBeVisible();
	await expect(create).toBeVisible();
	await expect(cancel.locator('xpath=ancestor::span[contains(@class, "workbench-action--secondary")]')).toHaveClass(/workbench-action--secondary/);
	await expect(create.locator('xpath=ancestor::span[contains(@class, "workbench-action--primary")]')).toHaveClass(/workbench-action--primary/);
	await expect(cancel.locator('xpath=ancestor::span[contains(@class, "workbench-action")]').locator('.workbench-action-icon')).toHaveCount(1);
	await expect(create.locator('xpath=ancestor::span[contains(@class, "workbench-action")]').locator('.workbench-action-icon')).toHaveCount(1);
	// Advanced settings is a settings pane that opens over the form below its toggle (plan M14.3), no longer a
	// <details> element; its controls stay inset inside the open pane.
	const advancedToggle = form.locator('#create-advanced-toggle');
	const advanced = form.locator('#create-advanced-panel');
	await expect(advancedToggle).toBeVisible();
	await advancedToggle.press('Enter');
	await expect(advanced).toBeVisible();
	await expect.poll(async () => advanced.evaluate(element => {
		const rect = element.getBoundingClientRect();
		const styles = getComputedStyle(element);
		const paddingRight = Number.parseFloat(styles.paddingRight) || 0;
		const right = rect.right - paddingRight;
		const controls = Array.from(element.querySelectorAll('input, select, textarea')).map(control => ({
			right: control.getBoundingClientRect().right,
			width: control.getBoundingClientRect().width
		}));
		return controls.length > 0
			&& controls.every(control => control.right <= right + 1)
			&& controls.every(control => control.width <= 600);
	}), { message: 'Advanced settings controls stay inset inside the open pane and at most 600px wide' }).toBe(true);
	for (const type of ['federate', 'remote', 'sparql', 'lmdb']) {
		await page.goto(`${WORKBENCH_BASE_URL}/repositories/NONE/create?type=${type}`, {
			waitUntil: 'domcontentloaded'
		});
		const actionInputs = page.locator('form[action="create"] input[value="Cancel"], form[action="create"] #create');
		await expect(actionInputs).toHaveCount(2);
		for (const input of await actionInputs.all()) {
			await expect(input.locator('xpath=ancestor::span[contains(@class, "workbench-action")]').locator('.workbench-action-icon')).toHaveCount(1);
		}
	}
});

test('Add keeps compact source choices, selected input, Format, then Advanced', async ({ page }) => {
	await page.setViewportSize({ width: 390, height: 1000 });
	await page.goto(`${REPOSITORY_BASE_URL}/add`, { waitUntil: 'domcontentloaded' });
	await expect(page.locator('#add-source-tabs')).toBeVisible();
	const sourceLabels = await page.locator('#add-source-tabs label').evaluateAll(labels =>
		labels.map(label => label.textContent.replace(/\s+/g, ' ').trim()));
	expect(sourceLabels).toEqual(['File', 'URL', 'Text']);
	await expect(page.locator('#add-source-tabs .workbench-action-icon')).toHaveCount(3);
	await expect(page.locator('#add-source-file-panel')).toBeVisible();
	await expect(page.locator('#add-source-url-panel')).toBeHidden();
	await expect(page.locator('#add-source-text-panel')).toBeHidden();
	const order = await page.evaluate(() => {
		const rect = selector => document.querySelector(selector).getBoundingClientRect();
		return {
			file: rect('#add-source-file-panel').top,
			format: rect('.add-source-format').top,
			advanced: rect('#add-import-settings').top,
			upload: rect('#add-upload-actions').top,
			bodyScrollWidth: document.body.scrollWidth
		};
	});
	expect(order.file).toBeLessThan(order.format);
	expect(order.format).toBeLessThan(order.advanced);
	expect(order.advanced).toBeLessThan(order.upload);
	expect(order.bodyScrollWidth).toBeLessThanOrEqual(390);
});

test('Explore keeps preferences collapsed and pagination below the result island', async ({ page }) => {
	await page.goto(`${REPOSITORY_BASE_URL}/explore?resource=%3Chttp%3A%2F%2Fexample.org%2Falice%3E`, {
		waitUntil: 'domcontentloaded'
	});
	await expect(page.locator('#explore-result-options')).toHaveCount(1);
	// Display is a settings pane with a toggle (plan M5.1, M14.3), no longer a <details> element.
	await expect(page.locator('#explore-result-options-toggle')).toHaveAttribute('aria-expanded', 'false');
	await expect(page.locator('#explore-result-options-panel')).toHaveCount(1);
	await expect(page.locator('#explore-result-options-panel')).toBeHidden();
	await expect(page.locator('label[for="resource"]')).toHaveCount(1);
	const geometry = await page.evaluate(() => {
		const result = document.querySelector('#explore-results table.data, #explore-results table.simple')?.getBoundingClientRect();
		const label = document.querySelector('label[for="resource"]')?.getBoundingClientRect();
		const resource = document.querySelector('#resource')?.getBoundingClientRect();
		const previous = document.querySelector('#previousX').getBoundingClientRect();
		const next = document.querySelector('#nextX').getBoundingClientRect();
		return {
			resultBottom: result?.bottom || 0,
			previousTop: previous.top,
			nextTop: next.top,
			labelBottom: label?.bottom || 0,
			resourceTop: resource?.top || 0
		};
	});
	expect(geometry.previousTop).toBeGreaterThanOrEqual(geometry.resultBottom);
	expect(geometry.nextTop).toBeGreaterThanOrEqual(geometry.resultBottom);
	expect(geometry.labelBottom).toBeLessThanOrEqual(geometry.resourceTop);
});

test('Update editor stays useful on mobile and action controls retain outline icons', async ({ page }) => {
	await page.setViewportSize({ width: 390, height: 1000 });
	await page.goto(`${REPOSITORY_BASE_URL}/update`, { waitUntil: 'domcontentloaded' });
	const editorHeight = await page.locator('#update-editor .CodeMirror').evaluate(element => element.getBoundingClientRect().height);
	expect(editorHeight).toBeLessThanOrEqual(420);
	const utilityIcons = await page.locator('#update-editor .yasqe_buttons .svgImg svg').count();
	expect(utilityIcons).toBeGreaterThan(0);
});

test('ASK result uses the compact status surface and secondary fullscreen action', async ({ page }) => {
	await openQueryPage(page, REPOSITORY_ID, { viewport: { width: 390, height: 1000 } });
	const primary = await primaryColor(page);
	await runQuery(page, 'ASK { VALUES ?s { <http://example.org/alice> } }');
	const results = page.locator('#query-results');
	await expect(results.locator('.query-result-boolean__value')).toHaveText(/Yes/);
	// A Yes/No answer has no rows to show in full screen, so the action is hidden (plan M3.5).
	const fullscreen = results.locator('[id^="query-result-fullscreen-"]');
	await expect(fullscreen).toHaveCount(1);
	await expect(fullscreen).toBeHidden();
	const metrics = await results.evaluate(element => {
		const button = element.querySelector('[id^="query-result-fullscreen-"]');
		return {
			height: element.getBoundingClientRect().height,
			fullscreenBackground: getComputedStyle(button).backgroundColor,
			statusIcons: element.querySelectorAll('.workbench-status-icon').length,
			blackGlyphs: Array.from(element.querySelectorAll('img')).length
		};
	});
	expect(metrics.height).toBeLessThan(420);
	expect(metrics.fullscreenBackground).not.toBe(primary);
	expect(metrics.statusIcons).toBe(1);
	expect(metrics.blackGlyphs).toBe(0);
});

test('embedded tuple results keep paging secondary and reachable after keyboard scroll', async ({ page }) => {
	await openQueryPage(page, REPOSITORY_ID, { viewport: { width: 390, height: 1000 } });
	// Results continue with "Load more" instead of Previous/Next pages; ask for a small first batch, as
	// workbench-query-load-more.spec.js does, so that the result needs a second one and ends below the fold.
	await page.locator('#query-form').evaluate(form => {
		form.querySelector('[name="batch-size"]')?.remove();
		const control = document.createElement('input');
		control.type = 'hidden';
		control.name = 'batch-size';
		control.value = '40';
		form.appendChild(control);
	});
	const values = Array.from({ length: 60 }, (_unused, index) => `(${index} "item-${index}")`).join(' ');
	await runQuery(page, `SELECT ?number ?item WHERE { VALUES (?number ?item) { ${values} } } ORDER BY ?number`);
	const result = page.locator('#query-results [data-query-stream-root]');
	const loadMore = result.locator('.query-result-load-more');
	await expect(loadMore).toBeVisible();
	await expect(loadMore).toHaveClass(/workbench-action--secondary/);
	const before = await loadMore.evaluate(element => element.getBoundingClientRect().top);
	expect(before, 'Load more starts below the fold').toBeGreaterThan(1000);
	// End outside an editable element scrolls the page to the end of the result (plan M4.1).
	await page.evaluate(() => {
		if (document.activeElement instanceof HTMLElement) {
			document.activeElement.blur();
		}
	});
	await page.keyboard.press('End');
	const resultBox = await page.locator('#query-results').boundingBox();
	if (resultBox) {
		await page.mouse.move(resultBox.x + resultBox.width / 2, Math.min(resultBox.y + resultBox.height / 2, 900));
		await page.mouse.wheel(0, 1600);
	}
	await expect.poll(async () => loadMore.evaluate(element => {
		const rect = element.getBoundingClientRect();
		return rect.height > 0 && window.scrollY > 0 && rect.bottom <= window.innerHeight + 2;
	}), { message: 'Load more is on screen after scrolling to the end of the result' }).toBe(true);
});

test('populated fixture keeps list pages and Explore readable', async ({ page }) => {
	for (const route of ['namespaces', 'contexts', 'types']) {
		await page.goto(`${REPOSITORY_BASE_URL}/${route}`, { waitUntil: 'domcontentloaded' });
		await expect(page.locator(`#${route}-results table.data tr, #${route}-results table.simple tr`)).not.toHaveCount(0);
	}
	await page.goto(`${REPOSITORY_BASE_URL}/explore?resource=%3Chttp%3A%2F%2Fexample.org%2Falice%3E`, {
		waitUntil: 'domcontentloaded'
	});
	await expect(page.locator('#explore-results table.data tr, #explore-results table.simple tr')).not.toHaveCount(0);
	// Rows about the explored resource are grouped by role, so the tables show its neighbours by prefixed name and
	// the resource itself is named in the card above them (plan task M5.1). The header row shows before the rows load.
	await expect(page.locator('#explore-results')).toContainText(/\bex:\w+/);
	await expect(page.locator('#explore-resource-iri')).toContainText('example.org/alice');
	await expect(page.locator('#explore-results a[href*="example.org"]')).not.toHaveCount(0);
});

test('Explain Compare Diff is a compact scrollable surface with secondary close', async ({ page }) => {
	await page.setViewportSize({ width: 1440, height: 1000 });
	await page.goto(`${REPOSITORY_BASE_URL}/query`, { waitUntil: 'domcontentloaded' });
	await page.locator('.CodeMirror').first().waitFor({ state: 'visible' });
	await page.locator('.CodeMirror').first().evaluate(element =>
		element.CodeMirror.setValue('SELECT * WHERE { ?s ?p ?o } LIMIT 10'));
	await page.locator('#explain-trigger').click();
	await expect(page.locator('#compare-toggle')).toBeVisible({ timeout: 10000 });
	await expect.poll(async () => page.locator('#query-explanation').textContent()).not.toContain('Loading');
	await page.locator('#compare-toggle').click();
	await expect.poll(async () => page.locator('.CodeMirror').count()).toBe(2);
	await page.locator('.CodeMirror').nth(1).evaluate(element =>
		element.CodeMirror.setValue('ASK { ?s ?p ?o }'));
	await page.locator('#explain-compare-trigger').click();
	await expect(page.locator('#query-diff-trigger')).toBeEnabled({ timeout: 10000 });
	await page.locator('#query-diff-trigger').click();
	await expect(page.locator('#query-diff-modal')).toHaveClass(/query-diff-modal--open/);
	const metrics = await page.evaluate(() => {
		const dialog = document.querySelector('.query-diff-modal__dialog');
		const body = document.querySelector('.query-diff-modal__body');
		const close = document.getElementById('query-diff-close');
		return {
			dialogHeight: dialog.getBoundingClientRect().height,
			viewportHeight: window.innerHeight,
			bodyOverflowY: getComputedStyle(body).overflowY,
			closeWrapper: close.closest('.workbench-action')?.className || ''
		};
	});
	expect(metrics.dialogHeight).toBeLessThan(metrics.viewportHeight - 100);
	expect(metrics.bodyOverflowY).not.toBe('hidden');
	expect(metrics.closeWrapper).toContain('workbench-action--secondary');
});

test('Explain surfaces use muted semantic colors for plan text and secondary controls', async ({ page }) => {
	await page.setViewportSize({ width: 1440, height: 1000 });
	await page.goto(`${REPOSITORY_BASE_URL}/query`, { waitUntil: 'domcontentloaded' });
	await page.locator('.CodeMirror').first().waitFor({ state: 'visible' });
	await page.locator('.CodeMirror').first().evaluate(element =>
		element.CodeMirror.setValue('SELECT * WHERE { ?s ?p ?o } LIMIT 10'));
	await page.locator('#explain-trigger').click();
	await expect(page.locator('#compare-toggle')).toBeVisible({ timeout: 10000 });
	await expect.poll(async () => page.locator('#query-explanation').textContent()).not.toContain('Loading');
	const primary = await primaryColor(page);
	const colors = await page.evaluate(() => {
		const token = document.querySelector('.query-explanation-token');
		const config = document.getElementById('explanation-settings-toggle');
		const compare = document.getElementById('compare-toggle');
		return {
			tokenCount: document.querySelectorAll('.query-explanation-token').length,
			lineCount: document.querySelectorAll('[class*="query-explanation"]').length,
			explanationText: document.getElementById('query-explanation')?.textContent?.slice(0, 120) || '',
			token: token ? getComputedStyle(token).color : '',
			config: config ? getComputedStyle(config).backgroundColor : '',
			compare: compare ? getComputedStyle(compare).backgroundColor : ''
		};
	});
	expect(colors.tokenCount).toBeGreaterThan(0);
	expect(colors.token).not.toBe('rgb(0, 28, 170)');
	expect(colors.config).toMatch(/^rgb/);
	expect(colors.compare).toMatch(/^rgb/);
	expect(colors.config).not.toBe(primary);
	expect(colors.compare).not.toBe(primary);
});

test('embedded result actions keep outline icons in the result document', async ({ page }) => {
	await openQueryPage(page, REPOSITORY_ID, { viewport: { width: 390, height: 1000 } });
	await runQuery(page, 'SELECT ?s ?p ?o WHERE { ?s ?p ?o } LIMIT 3');
	await expect(page.locator('#query-results .query-result-status')).toHaveText(/^3 rows/);
	const icons = await page.locator('#query-results').evaluate(results =>
		Array.from(results.querySelectorAll('.workbench-action-icon, .workbench-status-icon')).map(icon => ({
			className: icon.className.baseVal,
			fill: getComputedStyle(icon).fill,
			stroke: getComputedStyle(icon).stroke,
			pathFill: Array.from(icon.querySelectorAll('path, circle')).map(path => getComputedStyle(path).fill),
			pathStroke: Array.from(icon.querySelectorAll('path, circle')).map(path => getComputedStyle(path).stroke)
		})));
	expect(icons.length).toBeGreaterThan(0);
	for (const icon of icons) {
		expect(icon.fill).toBe('none');
		expect(icon.stroke).not.toBe('none');
		expect(icon.pathFill.every(fill => fill === 'none')).toBe(true);
		expect(icon.pathStroke.every(stroke => stroke !== 'none')).toBe(true);
	}
});

test('query starts without an empty result island and Explain cancel has a labelled action root', async ({ page }) => {
	await page.addInitScript(() => {
		window.addEventListener('pageshow', () => {
			window.__workbenchPageshowSeen = true;
		}, { once: true });
	});
	await page.goto(`${REPOSITORY_BASE_URL}/query`, { waitUntil: 'domcontentloaded' });
	await page.waitForFunction(() => window.__workbenchPageshowSeen === true);
	await expect(page.locator('#query-results')).toHaveCount(1);
	await expect(page.locator('#query-results')).toBeHidden();
	const cancel = page.locator('#explain-trigger-cancel');
	const cancelRoot = cancel.locator(
		'xpath=ancestor::*[contains(concat(" ", normalize-space(@class), " "), " workbench-action ")]'
	);
	await expect(cancelRoot).toHaveCount(1);
	await expect(cancelRoot.locator('.workbench-action-icon')).toHaveCount(1);
	await expect(cancelRoot.locator('.workbench-action-label')).toContainText(/cancel/i);
});

test('Explain cancel action root is hidden before and after a completed explanation', async ({ page }) => {
	await page.goto(`${REPOSITORY_BASE_URL}/query`, { waitUntil: 'domcontentloaded' });
	await page.locator('.CodeMirror').first().waitFor({ state: 'visible' });
	const cancelRoot = page.locator('#explain-trigger-cancel-action');
	await expect(cancelRoot).toBeHidden();
	await page.locator('.CodeMirror').first().evaluate(element =>
		element.CodeMirror.setValue('SELECT * WHERE { VALUES ?s { "ghost-cancel" } }'));
	await page.locator('#explain-trigger').click();
	await page.locator('#query-explanation').waitFor({ state: 'visible', timeout: 10000 });
	await expect.poll(async () => page.locator('#query-explanation').textContent()).not.toContain('Loading');
	await expect(cancelRoot).toBeHidden();
});

test('Explain comparison controls expose labelled Copy, Swap, and Close actions', async ({ page }) => {
	await page.setViewportSize({ width: 1440, height: 1000 });
	await page.goto(`${REPOSITORY_BASE_URL}/query`, { waitUntil: 'domcontentloaded' });
	await page.locator('.CodeMirror').first().waitFor({ state: 'visible' });
	await page.locator('.CodeMirror').first().evaluate(element =>
		element.CodeMirror.setValue('SELECT * WHERE { ?s ?p ?o } LIMIT 10'));
	await page.locator('#explain-trigger').click();
	await expect(page.locator('#compare-toggle')).toBeVisible({ timeout: 10000 });
	await expect.poll(async () => page.locator('#query-explanation').textContent()).not.toContain('Loading');
	await expect(page.locator('#copy-explanation')).toContainText(/copy/i);
	await page.locator('#compare-toggle').click();
	await expect.poll(async () => page.locator('.CodeMirror').count()).toBe(2);
	// In compare mode Swap sits in the explanation toolbar and each plan column has its own Copy icon button
	// (plan M3.3); icon buttons are labelled by aria-label.
	const controlLabels = await page.locator('#query-compare-toolbar button, .query-explanation-column__header button')
		.evaluateAll(buttons => buttons
			.filter(button => button.getClientRects().length > 0)
			.map(button => button.getAttribute('aria-label') || button.textContent.replace(/\s+/g, ' ').trim()));
	expect(controlLabels.join(' ')).toMatch(/copy/i);
	expect(controlLabels.join(' ')).toMatch(/swap/i);
	await page.locator('.CodeMirror').nth(1).evaluate(element =>
		element.CodeMirror.setValue('ASK { ?s ?p ?o }'));
	await page.locator('#explain-compare-trigger').click();
	await expect(page.locator('#query-diff-trigger')).toBeEnabled({ timeout: 10000 });
	await page.locator('#query-diff-trigger').click();
	await expect(page.locator('#query-diff-modal')).toHaveClass(/query-diff-modal--open/);
	// Close is a <button> of the button component now (plan M1.3), not an <input> with a value.
	await expect(page.locator('#query-diff-close')).toHaveAccessibleName(/close/i);
});

test('YASQE utility controls preserve the hidden library state', async ({ page }) => {
	await page.setViewportSize({ width: 390, height: 1000 });
	await page.goto(`${REPOSITORY_BASE_URL}/update`, { waitUntil: 'domcontentloaded' });
	await page.locator('#update-editor').waitFor({ state: 'visible' });
	await page.locator('#update-editor .yasqe_buttons .svgImg').first().waitFor({ state: 'attached' });
	const state = await page.locator('#update-editor .yasqe_buttons .svgImg').evaluateAll(elements => ({
		total: elements.length,
		visible: elements.filter(element => getComputedStyle(element).display !== 'none').length,
		hidden: elements.filter(element => getComputedStyle(element).display === 'none').length
	}));
	expect(state.hidden).toBeGreaterThan(0);
	expect(state.visible).toBeGreaterThan(0);
});

test('YASQE fullscreen controls keep one state-specific icon visible', async ({ page }) => {
	await page.setViewportSize({ width: 390, height: 1000 });
	await page.goto(`${REPOSITORY_BASE_URL}/update`, { waitUntil: 'domcontentloaded' });
	await page.locator('#update-editor').waitFor({ state: 'visible' });
	const fullscreen = page.locator('#update-editor .yasqe_buttons .svgImg.yasqe_fullscreenBtn');
	const smallscreen = page.locator('#update-editor .yasqe_buttons .svgImg.yasqe_smallscreenBtn');
	await fullscreen.waitFor({ state: 'attached' });
	await smallscreen.waitFor({ state: 'attached' });
	await expect(fullscreen).toBeVisible();
	await expect(smallscreen).toBeHidden();
	await fullscreen.click();
	await expect(fullscreen).toBeHidden();
	await expect(smallscreen).toBeVisible();
	await smallscreen.click();
	await expect(fullscreen).toBeVisible();
	await expect(smallscreen).toBeHidden();
});
