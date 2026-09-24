// @ts-check
const { test, expect } = require('@playwright/test');

const SERVER_BASE_URL = process.env.RDF4J_SERVER_BASE_URL || 'http://127.0.0.1:8080/rdf4j-server';
const WORKBENCH_BASE_URL = process.env.RDF4J_WORKBENCH_BASE_URL || 'http://127.0.0.1:8080/rdf4j-workbench';
const REPOSITORY_ID = 'workbench-final-closure-20260924';
const REPOSITORY_BASE_URL = `${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}`;

test.beforeAll(async ({ request }) => {
	await request.delete(`${SERVER_BASE_URL}/repositories/${REPOSITORY_ID}`);
	const response = await request.put(`${SERVER_BASE_URL}/repositories/${REPOSITORY_ID}`, {
		headers: { 'Content-Type': 'text/turtle' },
		data: repositoryConfig(REPOSITORY_ID)
	});
	expect([200, 201, 204]).toContain(response.status());
	const statements = [
		'<http://example.org/alice> <http://example.org/name> "Alice" .',
		'<http://example.org/alice> <http://example.org/type> <http://example.org/Person> .',
		'<http://example.org/bob> <http://example.org/name> "Bob" .',
		'<http://example.org/bob> <http://example.org/type> <http://example.org/Person> .'
	].join('\n');
	const statementsResponse = await request.post(`${SERVER_BASE_URL}/repositories/${REPOSITORY_ID}/statements`, {
		headers: { 'Content-Type': 'application/n-triples' },
		data: statements
	});
	expect([200, 201, 204]).toContain(statementsResponse.status());
});

test.afterAll(async ({ request }) => {
	await request.delete(`${SERVER_BASE_URL}/repositories/${REPOSITORY_ID}`);
});

test('semantic action icons and padding activate native controls while disabled paging stays inert', async ({ page }) => {
	await page.setViewportSize({ width: 390, height: 1000 });
	await page.goto(`${WORKBENCH_BASE_URL}/repositories/NONE/create?type=memory-rdfs-dt`, { waitUntil: 'domcontentloaded' });
	const cancel = page.locator('form[action="create"] input[data-href="repositories"]');
	const cancelWrapper = cancel.locator('xpath=ancestor::*[contains(concat(" ", normalize-space(@class), " "), " workbench-action ")]');
	await expect(cancel).toBeVisible();
	await page.evaluate(() => {
		window.__closureAction = '';
		const input = document.querySelector('input[data-href="repositories"]');
		input.addEventListener('click', event => {
			window.__closureAction = 'cancel';
			event.preventDefault();
			event.stopImmediatePropagation();
		}, true);
	});
	await cancelWrapper.locator('.workbench-action-icon').click();
	await expect.poll(() => page.evaluate(() => window.__closureAction)).toBe('cancel');

	await page.goto(`${WORKBENCH_BASE_URL}/repositories/NONE/create?type=memory-rdfs-dt`, { waitUntil: 'domcontentloaded' });
	const create = page.locator('form[action="create"] #create');
	const createWrapper = create.locator('xpath=ancestor::*[contains(concat(" ", normalize-space(@class), " "), " workbench-action ")]');
	await page.evaluate(() => {
		window.__closureAction = '';
		const input = document.querySelector('form[action="create"] #create');
		input.addEventListener('click', event => {
			window.__closureAction = 'create';
			event.preventDefault();
			event.stopImmediatePropagation();
		}, true);
	});
	const box = await createWrapper.boundingBox();
	expect(box).toBeTruthy();
	await page.mouse.click(box.x + box.width - 2, box.y + box.height / 2);
	await expect.poll(() => page.evaluate(() => window.__closureAction)).toBe('create');

	await page.goto(`${REPOSITORY_BASE_URL}/query`, { waitUntil: 'domcontentloaded' });
	await page.locator('.CodeMirror').first().evaluate(element =>
		element.CodeMirror.setValue('SELECT ?s WHERE { ?s <http://example.org/name> ?o } LIMIT 2'));
	await page.locator('#exec').click();
	const frame = page.frameLocator('#query-results-frame');
	await frame.locator('#rdf4j-query-result').waitFor({ state: 'attached' });
	await frame.locator('#previousX').waitFor({ state: 'visible' });
	const disabledPaging = frame.locator('#previousX');
	await expect(disabledPaging).toBeDisabled();
	await page.locator('#query-results-frame').evaluate(frameElement => {
		frameElement.contentDocument.querySelector('#previousX').ownerDocument.defaultView.__closureAction = '';
	});
	await frame.locator('#previousX').evaluate(element => {
		element.addEventListener('click', () => {
			element.ownerDocument.defaultView.__closureAction = 'previous';
		});
	});
	const disabledWrapper = frame.locator('#previousX').locator('xpath=ancestor::*[contains(concat(" ", normalize-space(@class), " "), " workbench-action ")]');
	await disabledWrapper.locator('.workbench-action-icon').click({ force: true });
	await expect.poll(() => page.locator('#query-results-frame').evaluate(frameElement =>
		frameElement.contentDocument.defaultView.__closureAction)).toBe('');
});

test('paging wrapper remains wholly visible after real frame scrolling', async ({ page }) => {
	await page.setViewportSize({ width: 390, height: 1000 });
	await page.goto(`${REPOSITORY_BASE_URL}/query`, { waitUntil: 'domcontentloaded' });
	const values = Array.from({ length: 120 }, (_, index) =>
		`("row-${index}" <http://example.org/p> "value-${index}")`).join(' ');
	const pagingQuery = `SELECT ?s ?p ?o WHERE { VALUES (?s ?p ?o) { ${values} } }`;
	await page.locator('.CodeMirror').first().evaluate((element, query) =>
		element.CodeMirror.setValue(query), pagingQuery);
	await page.locator('#exec').click();
	const frame = page.frameLocator('#query-results-frame');
	await frame.locator('#rdf4j-query-result').waitFor({ state: 'attached' });
	await frame.locator('#query-result-layout').waitFor({ state: 'attached' });
	const frameElement = page.locator('#query-results-frame');
	await frameElement.scrollIntoViewIfNeeded();
	const frameBox = await frameElement.boundingBox();
	expect(frameBox).toBeTruthy();
	await page.mouse.move(frameBox.x + frameBox.width / 2, frameBox.y + frameBox.height - 24);
	await page.mouse.wheel(0, 50000);
	await expect.poll(() => frameElement.evaluate(element => element.contentDocument.documentElement.scrollTop))
		.toBeGreaterThan(0);
	const geometry = await frame.locator('#nextX').evaluate(element => {
		const wrapper = element.closest('.workbench-action');
		const wrapperRect = wrapper.getBoundingClientRect();
		return {
			wrapperBottom: wrapperRect.bottom,
			controlBottom: element.getBoundingClientRect().bottom,
			viewport: element.ownerDocument.defaultView.innerHeight,
			scrollTop: element.ownerDocument.documentElement.scrollTop
		};
	});
	expect(geometry.scrollTop).toBeGreaterThan(0);
	expect(geometry.wrapperBottom).toBeLessThanOrEqual(geometry.viewport + 1);
	expect(geometry.controlBottom).toBeLessThanOrEqual(geometry.viewport + 1);
});

test('compare actions are labelled together and activate from their icon hit area', async ({ page }) => {
	await page.setViewportSize({ width: 1440, height: 1000 });
	await page.goto(`${REPOSITORY_BASE_URL}/query`, { waitUntil: 'domcontentloaded' });
	await page.locator('.CodeMirror').first().evaluate(element =>
		element.CodeMirror.setValue('SELECT * WHERE { VALUES ?s { "one" } }'));
	await page.locator('#explain-trigger').click();
	await expect(page.locator('#compare-toggle')).toBeVisible({ timeout: 10000 });
	await page.locator('#compare-toggle').click();
	await expect.poll(() => page.locator('.CodeMirror').count()).toBe(2);
	await page.locator('.CodeMirror').nth(1).evaluate(element => element.CodeMirror.setValue('ASK { ?s ?p ?o }'));
	await page.locator('#explain-compare-trigger').click();
	await expect(page.locator('#query-diff-trigger')).toBeEnabled({ timeout: 10000 });
	const toolbar = page.locator('#query-compare-toolbar');
	await expect(toolbar).toContainText(/copy/i);
	await expect(toolbar).toContainText(/swap/i);
	await expect(toolbar).toContainText(/refresh|compare/i);
	await expect(toolbar).toContainText(/diff/i);
	await page.evaluate(() => {
		window.__compareClicked = false;
		document.querySelector('#query-compare-swap').addEventListener('click', () => {
			window.__compareClicked = true;
		});
	});
	const swapWrapper = page.locator('#query-compare-swap').locator('xpath=ancestor::*[contains(concat(" ", normalize-space(@class), " "), " workbench-action ")]');
	await swapWrapper.locator('.workbench-action-icon').click();
	await expect.poll(() => page.evaluate(() => window.__compareClicked)).toBe(true);
});

test('saved query replaces legacy bookmark and keeps one visible YASQE fullscreen state', async ({ page }) => {
	await page.goto(`${REPOSITORY_BASE_URL}/query`, { waitUntil: 'domcontentloaded' });
	await page.locator('.CodeMirror').first().evaluate(element =>
		element.CodeMirror.setValue('SELECT ?s WHERE { ?s <http://example.org/name> ?o }'));
	await page.locator('#save-query-toggle').press('Enter');
	const queryName = `closure-saved-${Date.now()}`;
	await page.locator('#query-name').fill(queryName);
	await page.evaluate(() => window.workbench.query.handleNameChange());
	await page.locator('#save').click();
	await expect(page.locator('#save-feedback')).toHaveText('Query saved.');
	await page.goto(`${REPOSITORY_BASE_URL}/saved-queries`, { waitUntil: 'domcontentloaded' });
	const row = page.locator('.saved-query-row').filter({ hasText: queryName });
	await expect(row).toHaveCount(1);
	await expect(row.locator('img[src*="bookmark"], img[src*="cancel"]')).toHaveCount(0);
	await row.locator('.saved-query-toggle').click();
	await row.locator('.yasqe').waitFor({ state: 'visible' });
	const state = await row.locator('.yasqe_buttons .svgImg').evaluateAll(elements => {
		const visibleElements = elements.filter(element => getComputedStyle(element).display !== 'none');
		const shapes = visibleElements.flatMap(element => Array.from(
			element.querySelectorAll('path, rect, circle, line, polyline, polygon'))
			.map(shape => ({
				fill: getComputedStyle(shape).fill,
				stroke: getComputedStyle(shape).stroke,
				color: getComputedStyle(element).color
			})));
		return {
			visible: visibleElements.length,
			hidden: elements.filter(element => getComputedStyle(element).display === 'none').length,
			shapes
		};
	});
	expect(state.visible).toBeGreaterThan(0);
	expect(state.hidden).toBeGreaterThan(0);
	expect(state.shapes.length, 'the visible saved-editor icon should contain drawable geometry').toBeGreaterThan(0);
	expect(state.shapes.every(shape => shape.fill === 'none' || shape.fill === 'rgba(0, 0, 0, 0)')).toBe(true);
	expect(state.shapes.every(shape => shape.stroke !== 'none' && shape.stroke === shape.color)).toBe(true);
});

test('mobile compare controls use full native hit areas and hide the obsolete sidebar glyph', async ({ page }) => {
	await page.setViewportSize({ width: 390, height: 1000 });
	await page.goto(`${REPOSITORY_BASE_URL}/query`, { waitUntil: 'domcontentloaded' });
	await page.locator('.CodeMirror').first().evaluate(element =>
		element.CodeMirror.setValue('SELECT * WHERE { VALUES ?s { "one" } }'));
	await page.locator('#explain-trigger').click();
	await expect(page.locator('#compare-toggle')).toBeVisible({ timeout: 10000 });
	await page.locator('#compare-toggle').click();
	await expect.poll(() => page.locator('.CodeMirror').count()).toBe(2);
	await page.locator('.CodeMirror').nth(1).evaluate(element => element.CodeMirror.setValue('ASK { ?s ?p ?o }'));
	await page.locator('#explain-compare-trigger').click();
	await expect(page.locator('#query-diff-trigger')).toBeEnabled({ timeout: 10000 });
	const controls = await page.locator(
		'#query-compare-copy, #query-compare-swap, #explain-compare-trigger, #copy-explanation, #explanation-settings-toggle, #explain-trigger'
	).evaluateAll(elements => elements.map(element => {
		const style = getComputedStyle(element);
		const rect = element.getBoundingClientRect();
		return {
			id: element.id,
			height: rect.height,
			padding: parseFloat(style.paddingLeft) + parseFloat(style.paddingRight),
			visible: style.display !== 'none' && style.visibility !== 'hidden'
		};
	}));
	const refreshIcon = await page.locator('#explain-compare-trigger-icon').evaluate(element => {
		const bounds = Array.from(element.querySelectorAll('path'))
			.map(shape => shape.getBoundingClientRect())
			.reduce((union, rect) => ({
				left: Math.min(union.left, rect.left),
				top: Math.min(union.top, rect.top),
				right: Math.max(union.right, rect.right),
				bottom: Math.max(union.bottom, rect.bottom)
			}), { left: Infinity, top: Infinity, right: -Infinity, bottom: -Infinity });
		return {
			viewBox: element.getAttribute('viewBox'),
			shapeWidth: Number.isFinite(bounds.left) ? bounds.right - bounds.left : 0,
			shapeHeight: Number.isFinite(bounds.top) ? bounds.bottom - bounds.top : 0
		};
	});
	expect(controls.every(control => control.visible), 'compare controls should remain visible').toBe(true);
	expect(controls.every(control => control.height >= 44), 'native compare controls need coarse-pointer hit targets').toBe(true);
	expect(controls.every(control => control.padding >= 16), 'native compare controls need horizontal hit-area padding').toBe(true);
	expect(refreshIcon.viewBox, 'refresh icon paths should share their 24px coordinate system').toBe('0 0 24 24');
	expect(refreshIcon.shapeWidth, 'refresh icon should occupy its visible control').toBeGreaterThan(8);
	expect(refreshIcon.shapeHeight, 'refresh icon should occupy its visible control').toBeGreaterThan(8);
	await expect(page.locator('#query-sidebar-toggle')).toBeHidden();
});

test('mobile explanation input actions use the complete label hit area', async ({ page }) => {
	await page.setViewportSize({ width: 390, height: 1000 });
	await page.goto(`${REPOSITORY_BASE_URL}/query`, { waitUntil: 'domcontentloaded' });
	await page.locator('.CodeMirror').first().evaluate(element =>
		element.CodeMirror.setValue('SELECT * WHERE { VALUES ?s { "one" } }'));
	await page.locator('#explain-trigger').click();
	await expect(page.locator('#rerun-explanation')).toBeVisible({ timeout: 10000 });
	const actionIds = ['#rerun-explanation', '#download-explanation', '#compare-toggle'];
	const actions = await page.locator(actionIds.join(', ')).evaluateAll(elements => elements.map(element => {
		const wrapper = element.closest('.workbench-action');
		const hitArea = wrapper?.querySelector('.workbench-action-hit-area');
		const rect = element.getBoundingClientRect();
		const style = getComputedStyle(element);
		return {
			id: element.id,
			height: rect.height,
			padding: parseFloat(style.paddingLeft) + parseFloat(style.paddingRight),
			hitArea: Boolean(hitArea),
			wrapper: wrapper ? {
				top: wrapper.getBoundingClientRect().top,
				bottom: wrapper.getBoundingClientRect().bottom,
				width: wrapper.getBoundingClientRect().width
			} : null
		};
	}));
	expect(actions).toHaveLength(actionIds.length);
	expect(actions.every(action => action.height >= 44), 'input explanation actions need coarse-pointer hit targets')
		.toBe(true);
	expect(actions.every(action => action.padding >= 16), 'input explanation actions need horizontal hit-area padding')
		.toBe(true);
	expect(actions.every(action => action.hitArea), 'input explanation actions need a label hit area')
		.toBe(true);

	await page.evaluate(() => {
		window.__explanationInputAction = '';
		const input = document.querySelector('#download-explanation');
		input.addEventListener('click', event => {
			window.__explanationInputAction = 'download';
			event.preventDefault();
			event.stopImmediatePropagation();
		}, true);
	});
	const downloadWrapper = page.locator('#download-explanation')
		.locator('xpath=ancestor::*[contains(concat(" ", normalize-space(@class), " "), " workbench-action ")]');
	await downloadWrapper.locator('.workbench-action-icon').click();
	await expect.poll(() => page.evaluate(() => window.__explanationInputAction))
		.toBe('download');
	const wrapperBox = await downloadWrapper.boundingBox();
	expect(wrapperBox).toBeTruthy();
	await page.mouse.click(wrapperBox.x + wrapperBox.width - 2, wrapperBox.y + wrapperBox.height / 2);
	await expect.poll(() => page.evaluate(() => window.__explanationInputAction))
		.toBe('download');
});

test('Explore uses a short heading and separate readable resource metadata', async ({ page }) => {
	await page.goto(`${REPOSITORY_BASE_URL}/explore?resource=%3Chttp%3A%2F%2Fexample.org%2Falice%3E`, { waitUntil: 'domcontentloaded' });
	await expect(page.locator('#title_heading')).toHaveText('Explore');
	const metadata = page.locator('#explore-resource-summary');
	await expect(metadata).toBeVisible();
	await expect(metadata).toContainText('http://example.org/alice');
	await expect(metadata).toContainText(/1-\d+ of \d+/);
});

test('query stylesheet preserves shared header geometry across Workbench routes', async ({ page }) => {
	for (const width of [1440, 390]) {
		await page.setViewportSize({ width, height: 1000 });
		const measureShell = async () => page.evaluate(() => {
			const rect = selector => document.querySelector(selector).getBoundingClientRect();
			const style = selector => getComputedStyle(document.querySelector(selector));
			const header = rect('#header');
			const logo = rect('#logo');
			const context = rect('#contentheader');
			return {
				header: { top: header.top, height: header.height },
				logo: { top: logo.top, width: logo.width, height: logo.height },
				context: { top: context.top, height: context.height },
				font: style('#contentheader').fontFamily
			};
		});

		await page.goto(`${REPOSITORY_BASE_URL}/query`, { waitUntil: 'domcontentloaded' });
		await page.locator('#header').waitFor({ state: 'attached' });
		const queryShell = await measureShell();
		await page.goto(`${REPOSITORY_BASE_URL}/explore?resource=%3Chttp%3A%2F%2Fexample.org%2Falice%3E`, {
			waitUntil: 'domcontentloaded'
		});
		await page.locator('#header').waitFor({ state: 'attached' });
		const exploreShell = await measureShell();

		expect(Math.abs(queryShell.header.height - exploreShell.header.height)).toBeLessThanOrEqual(1);
		expect(Math.abs(queryShell.logo.width - exploreShell.logo.width)).toBeLessThanOrEqual(1);
		expect(Math.abs(queryShell.logo.height - exploreShell.logo.height)).toBeLessThanOrEqual(1);
		expect(Math.abs(queryShell.context.top - exploreShell.context.top)).toBeLessThanOrEqual(1);
		expect(queryShell.font).toBe(exploreShell.font);
	}
});

function repositoryConfig(repositoryId) {
	return `@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix config: <tag:rdf4j.org,2023:config/>.

[] a config:Repository ;
   config:rep.id "${repositoryId}" ;
   rdfs:label "Workbench final closure fixture" ;
   config:rep.impl [
      config:rep.type "openrdf:SailRepository" ;
      config:sail.impl [
         config:sail.type "openrdf:MemoryStore"
      ]
   ].`;
}
