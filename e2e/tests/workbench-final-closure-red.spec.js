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

test('result disclosures keep labels above controls inside grouped panels', async ({ page }) => {
	await page.setViewportSize({ width: 1440, height: 1000 });
	await page.goto(`${REPOSITORY_BASE_URL}/query`, { waitUntil: 'domcontentloaded' });
	await page.locator('.CodeMirror').first().evaluate(element =>
		element.CodeMirror.setValue('SELECT * WHERE { VALUES ?s { "one" "two" } }'));
	await page.locator('#exec').click();
	const frame = page.frameLocator('#query-results-frame');
	await frame.locator('#rdf4j-query-result').waitFor({ state: 'attached' });
	await frame.locator('#query-result-download-toggle').press('Enter');
	await frame.locator('#query-result-options-toggle').press('Enter');
	await expect(frame.locator('#query-result-download-panel')).toBeVisible();
	await expect(frame.locator('#query-result-options-panel')).toBeVisible();

	const metrics = await page.locator('#query-results-frame').evaluate(frameElement => {
		const document = frameElement.contentDocument;
		const rows = Array.from(document.querySelectorAll(
			'#query-result-download-panel .query-result-controls tr, #query-result-options-panel .query-result-controls tr'));
		const rowMetrics = rows.map(row => {
			const label = row.querySelector('th');
			const controls = Array.from(row.querySelectorAll('select, input, button'));
			if (!label || controls.length === 0) {
				return null;
			}
			const labelRect = label.getBoundingClientRect();
			return {
				labelBottom: labelRect.bottom,
				controlTop: Math.min(...controls.map(control => control.getBoundingClientRect().top))
			};
		}).filter(Boolean);
		const panelStyles = ['#query-result-download-panel', '#query-result-options-panel'].map(selector => {
			const panel = document.querySelector(selector);
			const style = getComputedStyle(panel);
			return {
				background: style.backgroundColor,
				border: style.borderTopWidth,
				radius: [style.borderTopLeftRadius, style.borderTopRightRadius,
					style.borderBottomRightRadius, style.borderBottomLeftRadius]
			};
		});
		const toggleStyles = ['#query-result-download-toggle', '#query-result-options-toggle'].map(selector => {
			const style = getComputedStyle(document.querySelector(selector));
			return [style.borderTopLeftRadius, style.borderTopRightRadius,
				style.borderBottomRightRadius, style.borderBottomLeftRadius];
		});
		const fieldStyles = Array.from(document.querySelectorAll(
			'#query-result-download-panel .query-result-field, #query-result-options-panel .query-result-field'))
			.map(field => {
			const style = getComputedStyle(field);
			return {
				border: style.borderTopWidth,
				background: style.backgroundColor,
				radius: [style.borderTopLeftRadius, style.borderTopRightRadius,
					style.borderBottomRightRadius, style.borderBottomLeftRadius]
			};
		});
		const panelWidths = ['#query-result-download-panel', '#query-result-options-panel'].map(selector => {
			const panel = document.querySelector(selector);
			return {
				clientWidth: panel.clientWidth,
				scrollWidth: panel.scrollWidth
			};
		});
		const optionsGridColumns = getComputedStyle(
			document.querySelector('#query-result-options-panel .query-result-fields')).gridTemplateColumns;
		const fieldMetrics = ['#query-result-download-panel', '#query-result-options-panel'].map(selector => {
			const panel = document.querySelector(selector);
			const fields = Array.from(panel.querySelectorAll('.query-result-field'));
			const checks = Array.from(panel.querySelectorAll('.query-result-check'));
			return {
				fieldCount: fields.length,
				fieldTops: fields.map(field => field.getBoundingClientRect().top),
				gridColumns: getComputedStyle(panel.querySelector('.query-result-fields')).gridTemplateColumns,
				checks: checks.map(check => {
					const input = check.querySelector('input');
					const text = check.querySelector('span');
					const inputRect = input.getBoundingClientRect();
					const textRect = text.getBoundingClientRect();
					return {
						display: getComputedStyle(check).display,
						inputTop: inputRect.top,
						textTop: textRect.top,
						inputBottom: inputRect.bottom,
						textBottom: textRect.bottom
					};
				})
			};
		});
		return { rowMetrics, panelStyles, toggleStyles, fieldStyles, panelWidths, optionsGridColumns, fieldMetrics };
	});

	expect(metrics.fieldMetrics.flatMap(panel => panel.fieldTops).length).toBeGreaterThanOrEqual(4);
	expect(metrics.panelStyles.every(panel => panel.background !== 'rgba(0, 0, 0, 0)' && panel.border === '1px'),
		'open result panels should be visible grouped surfaces').toBe(true);
	expect(metrics.panelStyles.every(panel => panel.radius.every(radius => parseFloat(radius) >= 6)),
		'open result panels should keep rounded corners').toBe(true);
	expect(metrics.toggleStyles.every(radii => radii.every(radius => parseFloat(radius) >= 6)),
		'open disclosure controls should keep rounded corners').toBe(true);
	expect(metrics.fieldStyles.every(row => row.border === '0px'),
		'result fields should share one inset panel instead of individual cards').toBe(true);
	expect(metrics.fieldStyles.every(row => row.background === 'rgba(0, 0, 0, 0)'),
		'result fields should not introduce nested card backgrounds').toBe(true);
	expect(metrics.fieldStyles.every(row => row.radius.every(radius => parseFloat(radius) === 0)),
		'result fields should not introduce nested rounded cards').toBe(true);
	expect(metrics.panelWidths.every(panel => panel.scrollWidth <= panel.clientWidth + 1),
		'open result panels should stay within the result frame').toBe(true);
	expect(metrics.optionsGridColumns.split(' ').filter(Boolean).length,
		'wide result options should use a compact multi-column field grid').toBeGreaterThanOrEqual(2);
	expect(metrics.fieldMetrics[0].fieldCount).toBeGreaterThanOrEqual(2);
	expect(metrics.fieldMetrics[1].fieldCount).toBeGreaterThanOrEqual(2);
	expect(metrics.fieldMetrics[0].gridColumns.split(' ').filter(Boolean).length,
		'download controls should keep format, limit, and action in a compact grid').toBeGreaterThanOrEqual(3);
	expect(metrics.fieldMetrics[1].checks.length).toBeGreaterThanOrEqual(2);
	expect(metrics.fieldMetrics.flatMap(panel => panel.checks).every(check =>
		['flex', 'inline-flex'].includes(check.display) && Math.abs(check.inputTop - check.textTop) < 10 &&
		Math.abs(check.inputBottom - check.textBottom) < 10),
		'checkboxes should remain inline with their labels').toBe(true);
});

test('embedded result paging uses an accessible group without an orphan label', async ({ page }) => {
	await page.setViewportSize({ width: 1440, height: 1000 });
	await page.goto(`${REPOSITORY_BASE_URL}/query`, { waitUntil: 'domcontentloaded' });
	await page.locator('.CodeMirror').first().evaluate(element =>
		element.CodeMirror.setValue('SELECT ?s WHERE { VALUES ?s { "one" "two" } }'));
	await page.locator('#exec').click();
	const frame = page.frameLocator('#query-results-frame');
	await frame.locator('#rdf4j-query-result').waitFor({ state: 'attached' });
	const navigation = frame.locator('.query-result-navigation');
	await expect(navigation).toHaveAttribute('role', 'group');
	await expect(navigation).toHaveAttribute('aria-label', /Results offset/i);
	await expect(navigation.locator('.query-result-navigation__label')).toHaveCount(0);
	await expect(navigation.locator('#previousX')).toBeVisible();
	await expect(navigation.locator('#nextX')).toBeVisible();
});

test('mobile result toolbar keeps title and fullscreen above disclosures', async ({ page }) => {
	await page.setViewportSize({ width: 390, height: 1000 });
	await page.goto(`${REPOSITORY_BASE_URL}/query`, { waitUntil: 'domcontentloaded' });
	await page.locator('.CodeMirror').first().evaluate(element =>
		element.CodeMirror.setValue('SELECT ?s ?p ?o WHERE { VALUES (?s ?p ?o) { ("one" <http://example.org/p> "long literal value one") ("two" <http://example.org/p> "long literal value two") ("three" <http://example.org/p> "long literal value three") } }'));
	await page.locator('#exec').click();
	const frame = page.frameLocator('#query-results-frame');
	await frame.locator('#rdf4j-query-result').waitFor({ state: 'attached' });
	const geometry = await page.locator('#query-results-frame').evaluate(frameElement => {
		const document = frameElement.contentDocument;
		const rect = selector => document.querySelector(selector).getBoundingClientRect();
		return {
			title: rect('#query-result-embedded-header h2'),
			fullscreen: rect('#query-result-fullscreen'),
			download: rect('#query-result-download-toggle'),
			options: rect('#query-result-options-toggle')
		};
	});
	expect(geometry.title.width).toBeGreaterThan(0);
	expect(geometry.fullscreen.width).toBeGreaterThan(0);
	expect(geometry.download.top).toBeGreaterThanOrEqual(
		Math.max(geometry.title.bottom, geometry.fullscreen.bottom) - 1,
		'mobile result disclosures should occupy the row below title and fullscreen');
	expect(geometry.options.top).toBeGreaterThanOrEqual(
		Math.max(geometry.title.bottom, geometry.fullscreen.bottom) - 1);
	await frame.locator('#query-result-download-toggle').press('Enter');
	await frame.locator('#query-result-options-toggle').press('Enter');
	await expect(frame.locator('#query-result-options-panel')).toBeVisible();
	const frameElement = page.locator('#query-results-frame');
	await frameElement.scrollIntoViewIfNeeded();
	const frameBox = await frameElement.boundingBox();
	expect(frameBox).toBeTruthy();
	await page.mouse.move(frameBox.x + frameBox.width / 2, frameBox.y + frameBox.height - 20);
	await page.mouse.wheel(0, 10000);
	await expect.poll(() => frameElement.evaluate(element => element.contentDocument.documentElement.scrollTop))
		.toBeGreaterThan(0);
	const scrolledGeometry = await frameElement.evaluate(frameElement => {
		const document = frameElement.contentDocument;
		const rect = selector => document.querySelector(selector).getBoundingClientRect();
		return {
			title: rect('#query-result-embedded-header h2'),
			fullscreen: rect('#query-result-fullscreen'),
			download: rect('#query-result-download-toggle'),
			options: rect('#query-result-options-toggle')
		};
	});
	expect(scrolledGeometry.title.top).toBeGreaterThanOrEqual(-1,
		'result title should remain visible while scrolling the embedded frame');
	expect(scrolledGeometry.fullscreen.top).toBeGreaterThanOrEqual(-1,
		'fullscreen control should remain visible while scrolling the embedded frame');
	expect(scrolledGeometry.download.top).toBeGreaterThanOrEqual(-1,
		'Download control should remain visible while an embedded panel is scrolled');
	expect(scrolledGeometry.options.top).toBeGreaterThanOrEqual(-1,
		'Result options control should remain visible while an embedded panel is scrolled');
});

test('query editor utilities stay readable beside the Explain tree action', async ({ page }) => {
	await page.setViewportSize({ width: 1440, height: 1000 });
	await page.goto(`${REPOSITORY_BASE_URL}/query`, { waitUntil: 'domcontentloaded' });
	await page.locator('.CodeMirror').first().evaluate(element =>
		element.CodeMirror.setValue('SELECT ?s ?p ?o WHERE { ?s ?p ?o } LIMIT 100'));
	const editorIcons = await page.locator('.query-page .yasqe .yasqe_buttons .svgImg').evaluateAll(elements =>
		elements.filter(element => getComputedStyle(element).display !== 'none').map(element => {
			const style = getComputedStyle(element);
			const rect = element.getBoundingClientRect();
			return { color: style.color, opacity: style.opacity, width: rect.width, height: rect.height };
		}));
	const explainIcon = await page.locator('#explain-trigger .query-action-icon').evaluate(element => ({
		circles: element.querySelectorAll('circle').length,
		connectors: element.querySelectorAll('path').length
	}));
	expect(editorIcons.length).toBeGreaterThan(0);
	expect(editorIcons.every(icon => icon.color === 'rgb(71, 85, 105)'),
		'editor utility icons should use readable slate controls').toBe(true);
	expect(editorIcons.every(icon => icon.opacity === '1' && icon.width >= 18 && icon.height >= 18),
		'editor utility icons should keep a stable visible geometry').toBe(true);
	expect(explainIcon).toEqual({ circles: 3, connectors: 1 });
});

test('Diff stays compact around its rendered content and compare actions stay flat', async ({ page }) => {
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
	await page.locator('#query-diff-trigger').click();
	const metrics = await page.evaluate(() => {
		const dialog = document.querySelector('.query-diff-modal__dialog');
		const body = document.querySelector('.query-diff-modal__body');
		const content = Array.from(document.querySelectorAll('.query-diff-section__title, .query-diff-row'));
		const dialogRect = dialog.getBoundingClientRect();
		const contentBottom = Math.max(...content.map(element => element.getBoundingClientRect().bottom));
		return {
			dialogHeight: dialogRect.height,
			viewportHeight: window.innerHeight,
			tail: dialogRect.bottom - contentBottom,
			bodyScrollHeight: body.scrollHeight,
			bodyClientHeight: body.clientHeight,
			compareShadows: Array.from(document.querySelectorAll('.query-compare-action'))
				.map(element => getComputedStyle(element).boxShadow)
		};
	});

	expect(metrics.dialogHeight).toBeLessThan(metrics.viewportHeight * 0.85);
	expect(metrics.tail).toBeLessThan(64);
	expect(metrics.bodyScrollHeight - metrics.bodyClientHeight).toBeLessThan(64);
	expect(metrics.compareShadows.every(shadow => shadow === 'none'),
		'compare controls should use the shared flat action treatment').toBe(true);
});

test('empty Explore results do not show orphaned pagination controls', async ({ page }) => {
	await page.goto(`${REPOSITORY_BASE_URL}/explore?resource=%3Chttp%3A%2F%2Fexample.org%2Fmissing%3E`, {
		waitUntil: 'domcontentloaded'
	});
	await expect(page.locator('#explore-results .workbench-empty')).toBeVisible();
	await expect(page.locator('#explore-pagination')).toBeHidden();
});

test('Explore uses a short heading and separate readable resource metadata', async ({ page }) => {
	await page.goto(`${REPOSITORY_BASE_URL}/explore?resource=%3Chttp%3A%2F%2Fexample.org%2Falice%3E`, { waitUntil: 'domcontentloaded' });
	await expect(page.locator('#title_heading')).toHaveText('Explore');
	const metadata = page.locator('#explore-resource-summary');
	await expect(metadata).toBeVisible();
	await expect(metadata).toContainText('http://example.org/alice');
	await expect(metadata).toContainText(/1-\d+ of \d+/);
	await expect(page.locator('#explore-pagination')).toHaveCount(1);
	await expect(page.locator('.explore-pagination__label')).toHaveCount(0);
	await expect(page.locator('#previousX')).toBeDisabled();
	await expect(page.locator('#nextX')).toBeDisabled();
	const disabledOpacity = await page.locator('#previousX, #nextX').evaluateAll(elements =>
		elements.map(element => Number.parseFloat(getComputedStyle(element).opacity)));
	expect(disabledOpacity.every(opacity => opacity < 1),
		'disabled Explore paging controls should be visibly muted').toBe(true);
});

test('Explore keeps an empty offset page recoverable with previous navigation', async ({ page }) => {
	await page.goto(`${REPOSITORY_BASE_URL}/explore?resource=%3Chttp%3A%2F%2Fexample.org%2Falice%3E&offset=100`, {
		waitUntil: 'domcontentloaded'
	});
	await expect(page.locator('#explore-results .workbench-empty')).toBeVisible();
	await expect(page.locator('#explore-pagination')).toHaveCount(1);
	await expect(page.locator('.explore-pagination__label')).toHaveCount(0);
	await expect(page.locator('#previousX')).toBeEnabled();
	await expect(page.locator('#nextX')).toBeDisabled();
	await expect(page.locator('#explore-result-count')).toHaveText('0 of 2');
	await expect(page.locator('#explore-results table.data')).toBeHidden();
	await page.locator('#previousX').click();
	await expect(page.locator('#explore-results table.data tbody tr')).not.toHaveCount(0);
});

test('Diff modal locks background focus and restores its trigger on Escape', async ({ page }) => {
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
	await page.locator('#query-diff-trigger').click();

	const presentation = await page.evaluate(() => {
		const modal = document.querySelector('#query-diff-modal');
		const dialog = document.querySelector('.query-diff-modal__dialog');
		const background = document.querySelector('#query-page > form');
		return {
			activeInside: dialog.contains(document.activeElement),
			bodyOverflow: getComputedStyle(document.body).overflow,
			backgroundInert: Boolean(background && background.inert),
			backgroundAriaHidden: background && background.getAttribute('aria-hidden'),
			modalOpen: modal.getAttribute('aria-hidden') === 'false'
		};
	});
	expect(presentation.modalOpen).toBe(true);
	expect(presentation.activeInside).toBe(true);
	expect(presentation.bodyOverflow).toBe('hidden');
	expect(presentation.backgroundInert || presentation.backgroundAriaHidden === 'true',
		'Diff background must be unavailable while the modal is open').toBe(true);

	await page.keyboard.press('Tab');
	await expect.poll(() => page.evaluate(() =>
		document.querySelector('.query-diff-modal__dialog').contains(document.activeElement))).toBe(true);
	await page.keyboard.press('Shift+Tab');
	await expect.poll(() => page.evaluate(() =>
		document.querySelector('.query-diff-modal__dialog').contains(document.activeElement))).toBe(true);
	await page.keyboard.press('Escape');
	await expect(page.locator('#query-diff-modal')).toHaveAttribute('aria-hidden', 'true');
	await expect(page.locator('#query-diff-trigger')).toBeFocused();
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
