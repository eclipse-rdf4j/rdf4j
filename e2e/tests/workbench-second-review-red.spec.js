// @ts-check
const { test, expect } = require('@playwright/test');

const SERVER_BASE_URL = process.env.RDF4J_SERVER_BASE_URL || 'http://127.0.0.1:8080/rdf4j-server';
const WORKBENCH_BASE_URL = process.env.RDF4J_WORKBENCH_BASE_URL || 'http://127.0.0.1:8080/rdf4j-workbench';
const REPOSITORY_ID = 'workbench-second-review-20260924';
const REPOSITORY_BASE_URL = `${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}`;

test.beforeAll(async ({ request }) => {
	await request.delete(`${SERVER_BASE_URL}/repositories/${REPOSITORY_ID}`);
	const response = await request.put(`${SERVER_BASE_URL}/repositories/${REPOSITORY_ID}`, {
		headers: { 'Content-Type': 'text/turtle' },
		data: repositoryConfig(REPOSITORY_ID)
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
	await request.delete(`${SERVER_BASE_URL}/repositories/${REPOSITORY_ID}`);
});

test('shared actions expose semantic classes and composed outline icons', async ({ page }) => {
	await page.goto(`${REPOSITORY_BASE_URL}/update`, { waitUntil: 'domcontentloaded' });
	const updateAction = page.locator('#update-actions input[type="submit"]');
	await expect(updateAction).toBeVisible();
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
	expect(actions.background).toBe('rgb(15, 118, 110)');

	await page.goto(`${REPOSITORY_BASE_URL}/clear`, { waitUntil: 'domcontentloaded' });
	const clearAction = page.locator('#clear-form input[type="submit"]');
	await expect(clearAction.locator('xpath=ancestor::span[contains(@class, "workbench-action--danger")]')).toHaveClass(/workbench-action--danger/);
	await expect(clearAction).not.toHaveCSS('background-color', 'rgb(15, 118, 110)');
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
	const advanced = form.locator('details.workbench-advanced');
	if (await advanced.count()) {
		await advanced.locator('summary').press('Enter');
		const inset = await advanced.evaluate(element => {
			const rect = element.getBoundingClientRect();
			const styles = getComputedStyle(element);
			const paddingRight = Number.parseFloat(styles.paddingRight) || 0;
			const controls = Array.from(element.querySelectorAll('input, select, textarea')).map(control => ({
				right: control.getBoundingClientRect().right,
				width: control.getBoundingClientRect().width
			}));
			return {
				right: rect.right - paddingRight,
				rect: { left: rect.left, right: rect.right, width: rect.width },
				controls
			};
		});
		expect(inset.controls.every(control => control.right <= inset.right + 1)).toBe(true);
		 expect(inset.controls.every(control => control.width <= 600)).toBe(true);
	}
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
	await expect(page.locator('#explore-result-options')).not.toHaveAttribute('open', '');
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
	await page.setViewportSize({ width: 390, height: 1000 });
	await page.goto(`${REPOSITORY_BASE_URL}/query`, { waitUntil: 'domcontentloaded' });
	await page.locator('.CodeMirror').first().waitFor({ state: 'visible' });
	await page.locator('.CodeMirror').first().evaluate(element =>
		element.CodeMirror.setValue('ASK { VALUES ?s { <http://example.org/alice> } }'));
	await page.locator('#exec').click();
	const frame = page.frameLocator('#query-results-frame');
	await frame.locator('#rdf4j-query-result').waitFor({ state: 'attached' });
	await expect(frame.locator('.queryResult')).toHaveText(/Yes/);
	const metrics = await page.locator('#query-results-frame').evaluate(frameElement => {
		const button = frameElement.contentDocument.querySelector('#query-result-fullscreen');
		return {
			height: frameElement.getBoundingClientRect().height,
			fullscreenBackground: getComputedStyle(button).backgroundColor,
			statusIcons: frameElement.contentDocument.querySelectorAll('.workbench-status-icon').length,
			blackGlyphs: Array.from(frameElement.contentDocument.querySelectorAll('img')).length
		};
	});
	expect(metrics.height).toBeLessThan(420);
	expect(metrics.fullscreenBackground).not.toBe('rgb(15, 118, 110)');
	expect(metrics.statusIcons).toBe(1);
	expect(metrics.blackGlyphs).toBe(0);
});

test('embedded tuple results keep paging secondary and reachable after keyboard scroll', async ({ page }) => {
	await page.setViewportSize({ width: 390, height: 1000 });
	await page.goto(`${REPOSITORY_BASE_URL}/query`, { waitUntil: 'domcontentloaded' });
	await page.locator('.CodeMirror').first().waitFor({ state: 'visible' });
	await page.locator('.CodeMirror').first().evaluate(element =>
		element.CodeMirror.setValue('SELECT ?s ?p ?o WHERE { ?s ?p ?o } LIMIT 3'));
	await page.locator('#exec').click();
	const frame = page.frameLocator('#query-results-frame');
	await frame.locator('#rdf4j-query-result').waitFor({ state: 'attached' });
	await expect(frame.locator('table.data tbody tr')).toHaveCount(3, { timeout: 30000 });
	const before = await frame.locator('#nextX').evaluate(element => ({
		wrapper: element.closest('.workbench-action')?.className || '',
		bottom: element.getBoundingClientRect().bottom,
		viewport: element.ownerDocument.defaultView.innerHeight
	}));
	expect(before.wrapper).toContain('workbench-action--secondary');
	await frame.locator('body').press('End');
	const frameBox = await page.locator('#query-results-frame').boundingBox();
	if (frameBox) {
		await page.mouse.move(frameBox.x + frameBox.width / 2, frameBox.y + frameBox.height / 2);
		await page.mouse.wheel(0, 1600);
	}
	await frame.locator('html').press('End');
	await frame.locator('#nextX').hover();
	await page.mouse.wheel(0, 1600);
	const after = await frame.locator('#nextX').evaluate(element => ({
		bottom: element.getBoundingClientRect().bottom,
		viewport: element.ownerDocument.defaultView.innerHeight,
		visible: element.getBoundingClientRect().height > 0,
		documentScrollTop: element.ownerDocument.documentElement.scrollTop,
		bodyScrollTop: element.ownerDocument.body?.scrollTop || 0
	}));
	expect(after.visible).toBe(true);
	expect(after.documentScrollTop).toBeGreaterThan(0);
	expect(after.bottom).toBeLessThanOrEqual(after.viewport + 2);
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
	const text = await page.locator('#explore-results').innerText();
	expect(text).toContain('ex:alice');
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
	expect(colors.config).not.toBe('rgb(15, 118, 110)');
	expect(colors.compare).not.toBe('rgb(15, 118, 110)');
});

test('embedded result actions keep outline icons in the result document', async ({ page }) => {
	await page.setViewportSize({ width: 390, height: 1000 });
	await page.goto(`${REPOSITORY_BASE_URL}/query`, { waitUntil: 'domcontentloaded' });
	await page.locator('.CodeMirror').first().waitFor({ state: 'visible' });
	await page.locator('.CodeMirror').first().evaluate(element =>
		element.CodeMirror.setValue('SELECT ?s ?p ?o WHERE { ?s ?p ?o } LIMIT 3'));
	await page.locator('#exec').click();
	const frame = page.frameLocator('#query-results-frame');
	await frame.locator('#rdf4j-query-result').waitFor({ state: 'attached' });
	const icons = await page.locator('#query-results-frame').evaluate(frameElement =>
		Array.from(frameElement.contentDocument.querySelectorAll('.workbench-action-icon, .workbench-status-icon')).map(icon => ({
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
	const controlLabels = await page.locator('#query-compare-toolbar .workbench-action-label button').allTextContents();
	expect(controlLabels.join(' ')).toMatch(/copy/i);
	expect(controlLabels.join(' ')).toMatch(/swap/i);
	await page.locator('.CodeMirror').nth(1).evaluate(element =>
		element.CodeMirror.setValue('ASK { ?s ?p ?o }'));
	await page.locator('#explain-compare-trigger').click();
	await expect(page.locator('#query-diff-trigger')).toBeEnabled({ timeout: 10000 });
	await page.locator('#query-diff-trigger').click();
	await expect(page.locator('#query-diff-modal')).toHaveClass(/query-diff-modal--open/);
	await expect(page.locator('#query-diff-close')).toHaveValue(/close/i);
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

function repositoryConfig(repositoryId) {
	return `@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix config: <tag:rdf4j.org,2023:config/>.

[] a config:Repository ;
   config:rep.id "${repositoryId}" ;
   rdfs:label "Workbench second review fixture" ;
   config:rep.impl [
      config:rep.type "openrdf:SailRepository" ;
      config:sail.impl [
         config:sail.type "openrdf:MemoryStore"
      ]
   ].`;
}
