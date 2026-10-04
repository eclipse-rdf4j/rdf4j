// @ts-check
const { test, expect } = require('@playwright/test');
const { runQuery, serverBaseUrl, uniqueRepositoryId, workbenchBaseUrl } = require('./workbench-test-helpers.js');

// Migrated and retired with the redesign (see .agent/execplans/workbench-stale-spec-migration-20261002.md). The query
// result is read in the page (#query-results [data-query-stream-root]) instead of the retired iframe (plan task M9.1),
// and Load more replaced the result's paging (design/workbench-loadmore-20260930/handoff.md: no Next/Previous or
// page-size controls for query results). Retired: "paging wrapper remains wholly visible after real frame scrolling"
// (the iframe's own scrolling and the result paging buttons are both gone; Load more is checked by
// workbench-query-load-more.spec.js); "embedded result paging uses an accessible group without an orphan label" (the
// result has no paging group any more); "mobile result toolbar keeps title and fullscreen above disclosures" (the
// result heading is visually hidden in the output card since plan task M3.5 and the actions share one toolbar row,
// which workbench-dropdown-detail-parity.spec.js "dynamic result details keep distinct triggers and responsive
// Format-style fields" checks at 390px; the rest measured scrolling inside the iframe).

const SERVER_BASE_URL = serverBaseUrl();
const WORKBENCH_BASE_URL = workbenchBaseUrl();
const REPOSITORY_ID = uniqueRepositoryId('workbench-final-closure');
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
	const cancel = page.locator('form[action="create"] [data-workbench-action="cancel"] input[type="button"]');
	const cancelWrapper = cancel.locator('xpath=ancestor::*[contains(concat(" ", normalize-space(@class), " "), " workbench-action ")]');
	await expect(cancel).toBeVisible();
	await page.evaluate(() => {
		window.__closureAction = '';
		const input = document.querySelector('form[action="create"] [data-workbench-action="cancel"] input[type="button"]');
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
	await expect(create).toBeVisible();
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

	// Query results have no paging since Load more replaced it; Explore keeps its paging buttons, which are native
	// buttons of the shared component and disabled on a resource's only page.
	await page.goto(`${REPOSITORY_BASE_URL}/explore?resource=%3Chttp%3A%2F%2Fexample.org%2Falice%3E`,
		{ waitUntil: 'domcontentloaded' });
	const disabledPaging = page.locator('#explore-pagination #previousX');
	await expect(disabledPaging).toBeVisible();
	await expect(disabledPaging).toBeDisabled();
	const pagedUrl = page.url();
	await disabledPaging.evaluate(element => {
		window.__closureAction = '';
		element.addEventListener('click', () => {
			window.__closureAction = 'previous';
		});
	});
	await disabledPaging.click({ force: true });
	await expect.poll(() => page.evaluate(() => window.__closureAction)).toBe('');
	expect(page.url(), 'disabled paging does not navigate').toBe(pagedUrl);
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
	await page.locator('#explain-trigger').click();
	await expect(page.locator('#query-diff-trigger')).toBeEnabled({ timeout: 10000 });
	const toolbar = page.locator('#query-compare-toolbar');
	await expect(toolbar).toContainText(/swap/i);
	await expect(toolbar).toContainText(/diff/i);
	// Explain and Explain again explain both queries in compare mode, so there is no "Refresh explanations".
	await expect(page.locator('#query-explanation-panel')).not.toContainText(/refresh explanations/i);
	// Each plan column's header holds its own labelled Copy icon button since plan task M3.3.
	for (const copy of ['#query-compare-copy', '#copy-explanation-compare']) {
		await expect(page.locator(copy)).toBeVisible();
		await expect(page.locator(copy)).toHaveAccessibleName(/copy/i);
	}
	await page.evaluate(() => {
		window.__compareClicked = false;
		document.querySelector('#query-compare-swap').addEventListener('click', () => {
			window.__compareClicked = true;
		});
	});
	// Swap is a native button of the shared component (plan task M1.3), so its icon is inside the button.
	await page.locator('#query-compare-swap .workbench-action-icon').click();
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

test('mobile compare controls use full native hit areas and a touch-sized navigation toggle', async ({ page }) => {
	await page.setViewportSize({ width: 390, height: 1000 });
	await page.goto(`${REPOSITORY_BASE_URL}/query`, { waitUntil: 'domcontentloaded' });
	await page.locator('.CodeMirror').first().evaluate(element =>
		element.CodeMirror.setValue('SELECT * WHERE { VALUES ?s { "one" } }'));
	await page.locator('#explain-trigger').click();
	await expect(page.locator('#compare-toggle')).toBeVisible({ timeout: 10000 });
	await page.locator('#compare-toggle').click();
	await expect.poll(() => page.locator('.CodeMirror').count()).toBe(2);
	await page.locator('.CodeMirror').nth(1).evaluate(element => element.CodeMirror.setValue('ASK { ?s ?p ?o }'));
	await page.locator('#explain-trigger').click();
	await expect(page.locator('#query-diff-trigger')).toBeEnabled({ timeout: 10000 });
	// In compare mode each plan column has its own Copy icon button and the toolbar's #copy-explanation is hidden
	// (plan task M3.3), so the compare column's copy button takes its place.
	const controlIds = ['#query-compare-copy', '#query-compare-swap', '#query-diff-trigger', '#copy-explanation-compare',
		'#explanation-settings-toggle', '#explain-trigger'];
	const controls = await page.locator(controlIds.join(', ')).evaluateAll(elements => elements.map(element => {
		const style = getComputedStyle(element);
		const rect = element.getBoundingClientRect();
		return {
			id: element.id,
			iconButton: element.classList.contains('workbench-action--icon'),
			width: rect.width,
			height: rect.height,
			padding: parseFloat(style.paddingLeft) + parseFloat(style.paddingRight),
			visible: style.display !== 'none' && style.visibility !== 'hidden' && element.getClientRects().length > 0
		};
	}));
	// Diff draws its arrow as text; every action icon the compare controls draw (not the small
	// disclosure chevron) must fill its 24px view box.
	const icons = await page.locator(controlIds.map(id => `${id} svg:not(.workbench-disclosure-chevron)`).join(', '))
		.evaluateAll(elements =>
			elements.map(element => {
				const bounds = Array.from(element.querySelectorAll('path, circle, rect, line, polyline, polygon'))
					.map(shape => shape.getBoundingClientRect())
					.reduce((union, rect) => ({
						left: Math.min(union.left, rect.left),
						top: Math.min(union.top, rect.top),
						right: Math.max(union.right, rect.right),
						bottom: Math.max(union.bottom, rect.bottom)
					}), { left: Infinity, top: Infinity, right: -Infinity, bottom: -Infinity });
				return {
					control: element.closest('button')?.id,
					viewBox: element.getAttribute('viewBox'),
					shapeWidth: Number.isFinite(bounds.left) ? bounds.right - bounds.left : 0,
					shapeHeight: Number.isFinite(bounds.top) ? bounds.bottom - bounds.top : 0
				};
			}));
	expect(controls).toHaveLength(controlIds.length);
	expect(controls.every(control => control.visible), 'compare controls should remain visible').toBe(true);
	expect(controls.every(control => control.height >= 44), 'native compare controls need coarse-pointer hit targets').toBe(true);
	// Icon buttons are 44px squares on touch layouts (plan task M2.9); text buttons keep horizontal padding.
	expect(controls.every(control => control.iconButton ? control.width >= 44 : control.padding >= 16),
		`native compare controls need horizontal hit areas: ${JSON.stringify(controls)}`).toBe(true);
	expect(icons.length, 'compare controls draw icons').toBeGreaterThan(0);
	for (const icon of icons) {
		expect(icon.viewBox, `${icon.control} icon paths should share their 24px coordinate system`).toBe('0 0 24 24');
		expect(icon.shapeWidth, `${icon.control} icon should occupy its visible control`).toBeGreaterThan(8);
		expect(icon.shapeHeight, `${icon.control} icon should occupy its visible control`).toBeGreaterThan(8);
	}
	// Compare mode keeps its "Show navigation" toggle at the start of the primary editor header on every width (plan
	// task M3.3); on a phone it is a full touch target rather than the old hidden sidebar glyph.
	const sidebarToggle = await page.locator('#query-sidebar-toggle').boundingBox();
	expect(sidebarToggle, 'compare mode shows its navigation toggle').toBeTruthy();
	expect(sidebarToggle.width).toBeGreaterThanOrEqual(44);
	expect(sidebarToggle.height).toBeGreaterThanOrEqual(44);
});

test('mobile explanation actions use their complete button hit area', async ({ page }) => {
	await page.setViewportSize({ width: 390, height: 1000 });
	await page.goto(`${REPOSITORY_BASE_URL}/query`, { waitUntil: 'domcontentloaded' });
	await page.locator('.CodeMirror').first().evaluate(element =>
		element.CodeMirror.setValue('SELECT * WHERE { VALUES ?s { "one" } }'));
	await page.locator('#explain-trigger').click();
	await expect(page.locator('#rerun-explanation')).toBeVisible({ timeout: 10000 });
	const actionIds = ['#rerun-explanation', '#download-explanation', '#compare-toggle'];
	// The explanation actions are native buttons of the shared component (plan task M1.3) instead of inputs inside a
	// label hit area, so the button's own box is the hit area; Download is an icon button (plan task M3.3).
	const actions = await page.locator(actionIds.join(', ')).evaluateAll(elements => elements.map(element => {
		const rect = element.getBoundingClientRect();
		const style = getComputedStyle(element);
		return {
			id: element.id,
			tag: element.tagName,
			action: element.classList.contains('workbench-action'),
			iconButton: element.classList.contains('workbench-action--icon'),
			width: rect.width,
			height: rect.height,
			padding: parseFloat(style.paddingLeft) + parseFloat(style.paddingRight)
		};
	}));
	expect(actions).toHaveLength(actionIds.length);
	expect(actions.every(action => action.height >= 44), 'explanation actions need coarse-pointer hit targets')
		.toBe(true);
	expect(actions.every(action => action.iconButton ? action.width >= 44 : action.padding >= 16),
		`explanation actions need horizontal hit areas: ${JSON.stringify(actions)}`).toBe(true);
	expect(actions.every(action => action.tag === 'BUTTON' && action.action),
		'explanation actions are native buttons whose whole box is the hit area').toBe(true);

	await page.evaluate(() => {
		window.__explanationInputAction = '';
		const input = document.querySelector('#download-explanation');
		input.addEventListener('click', event => {
			window.__explanationInputAction = 'download';
			event.preventDefault();
			event.stopImmediatePropagation();
		}, true);
	});
	const download = page.locator('#download-explanation');
	await download.locator('.workbench-action-icon').click();
	await expect.poll(() => page.evaluate(() => window.__explanationInputAction))
		.toBe('download');
	await page.evaluate(() => { window.__explanationInputAction = ''; });
	const downloadBox = await download.boundingBox();
	expect(downloadBox).toBeTruthy();
	await page.mouse.click(downloadBox.x + downloadBox.width - 2, downloadBox.y + downloadBox.height / 2);
	await expect.poll(() => page.evaluate(() => window.__explanationInputAction))
		.toBe('download');
});

test('result disclosures keep labels above controls inside grouped panels', async ({ page }) => {
	await page.setViewportSize({ width: 1440, height: 1000 });
	await page.goto(`${REPOSITORY_BASE_URL}/query`, { waitUntil: 'domcontentloaded' });
	await runQuery(page, 'SELECT * WHERE { VALUES ?s { "one" "two" } }');
	const result = page.locator('#query-results [data-query-stream-root]');
	// Only one pane of a toolbar is open at a time since plan task M14.3, so Download and Display are measured in turn.
	const measurePanel = (toggleSelector, panelSelector) => result.evaluate((root, selectors) => {
		const panel = root.querySelector(selectors.panel);
		const toggle = root.querySelector(selectors.toggle);
		const radii = style => [style.borderTopLeftRadius, style.borderTopRightRadius,
			style.borderBottomRightRadius, style.borderBottomLeftRadius];
		const panelStyle = getComputedStyle(panel);
		const fieldContainer = panel.querySelector('.query-result-fields');
		const items = Array.from(fieldContainer.children).filter(item => item.getClientRects().length > 0);
		const firstRowBottom = Math.min(...items.map(item => item.getBoundingClientRect().bottom));
		const fields = Array.from(panel.querySelectorAll('.query-result-field'));
		const checks = Array.from(panel.querySelectorAll('.query-result-check'));
		return {
			background: panelStyle.backgroundColor,
			border: panelStyle.borderTopWidth,
			radius: radii(panelStyle),
			toggleRadius: radii(getComputedStyle(toggle)),
			clientWidth: panel.clientWidth,
			scrollWidth: panel.scrollWidth,
			// Items that start above the bottom of the shortest item share the first row: the panel's columns.
			columns: items.filter(item => item.getBoundingClientRect().top < firstRowBottom).length,
			fieldTops: fields.map(field => field.getBoundingClientRect().top),
			fieldStyles: fields.map(field => {
				const style = getComputedStyle(field);
				return { border: style.borderTopWidth, background: style.backgroundColor, radius: radii(style) };
			}),
			labels: fields.map(field => {
				const label = field.querySelector(':scope > span');
				const control = field.querySelector('select, input');
				return {
					labelBottom: label.getBoundingClientRect().bottom,
					controlTop: control.getBoundingClientRect().top
				};
			}),
			checks: checks.map(check => {
				const input = check.querySelector('input');
				// The check's text is the label's own text node since the streamed renderer.
				const text = document.createRange();
				const textNode = Array.from(check.childNodes).find(node => node.nodeType === Node.TEXT_NODE && node.textContent.trim());
				text.selectNodeContents(textNode || check);
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
	}, { toggle: toggleSelector, panel: panelSelector });
	const panels = [];
	for (const [toggle, panel] of [
		['.query-result-download-toggle', '.query-result-download-panel'],
		['.query-result-options-toggle', '.query-result-options-panel']
	]) {
		await result.locator(toggle).press('Enter');
		await expect(result.locator(panel)).toBeVisible();
		await expect.poll(() => result.locator(panel).evaluate(element => element.getAnimations().length)).toBe(0);
		panels.push(await measurePanel(toggle, panel));
	}
	const [download, options] = panels;

	// The result page-size select is gone (Load more replaced it): Download has Format and Limit, Display has Layout
	// and two checkboxes.
	expect(panels.flatMap(panel => panel.fieldTops).length).toBeGreaterThanOrEqual(3);
	expect(panels.flatMap(panel => panel.labels).every(field => field.labelBottom <= field.controlTop + 1),
		'result field labels sit above their controls').toBe(true);
	expect(panels.every(panel => panel.background !== 'rgba(0, 0, 0, 0)' && panel.border === '1px'),
		'open result panels should be visible grouped surfaces').toBe(true);
	expect(panels.every(panel => panel.radius.every(radius => parseFloat(radius) >= 6)),
		'open result panels should keep rounded corners').toBe(true);
	expect(panels.every(panel => panel.toggleRadius.every(radius => parseFloat(radius) >= 6)),
		'open disclosure controls should keep rounded corners').toBe(true);
	const fieldStyles = panels.flatMap(panel => panel.fieldStyles);
	expect(fieldStyles.every(row => row.border === '0px'),
		'result fields should share one inset panel instead of individual cards').toBe(true);
	expect(fieldStyles.every(row => row.background === 'rgba(0, 0, 0, 0)'),
		'result fields should not introduce nested card backgrounds').toBe(true);
	expect(fieldStyles.every(row => row.radius.every(radius => parseFloat(radius) === 0)),
		'result fields should not introduce nested rounded cards').toBe(true);
	expect(panels.every(panel => panel.scrollWidth <= panel.clientWidth + 1),
		'open result panels should stay within the result card').toBe(true);
	expect(options.columns, 'wide result options should use a compact multi-column field row').toBeGreaterThanOrEqual(2);
	expect(download.fieldTops.length).toBeGreaterThanOrEqual(2);
	expect(options.fieldTops.length).toBeGreaterThanOrEqual(1);
	expect(download.columns, 'download controls should keep format, limit, and action in one compact row')
		.toBeGreaterThanOrEqual(3);
	expect(options.checks.length).toBeGreaterThanOrEqual(2);
	expect(panels.flatMap(panel => panel.checks).every(check =>
		['flex', 'inline-flex'].includes(check.display) && Math.abs(check.inputTop - check.textTop) < 10 &&
		Math.abs(check.inputBottom - check.textBottom) < 10),
		'checkboxes should remain inline with their labels').toBe(true);
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
	// The editor overlay buttons use the muted ink token (plan task M1.8) instead of the retired slate rgb(71, 85, 105).
	const mutedInk = await page.evaluate(() => {
		const probe = document.createElement('span');
		probe.style.color = 'var(--workbench-muted)';
		document.querySelector('.query-page').append(probe);
		const color = getComputedStyle(probe).color;
		probe.remove();
		return color;
	});
	// The shared Explain icon draws its three plan nodes as arcs of one path, with the connectors as line segments.
	const explainIcon = await page.locator('#explain-trigger .workbench-action-icon--explain').evaluate(element => {
		const data = Array.from(element.querySelectorAll('path')).map(path => path.getAttribute('d')).join(' ');
		return {
			nodes: (data.match(/a/gi) || []).length + element.querySelectorAll('circle').length,
			connectors: /l/i.test(data)
		};
	});
	expect(editorIcons.length).toBeGreaterThan(0);
	expect(editorIcons.every(icon => icon.color === mutedInk),
		`editor utility icons should use the muted control ink ${mutedInk}: ${JSON.stringify(editorIcons)}`).toBe(true);
	expect(editorIcons.every(icon => icon.opacity === '1' && icon.width >= 18 && icon.height >= 18),
		'editor utility icons should keep a stable visible geometry').toBe(true);
	expect(explainIcon).toEqual({ nodes: 3, connectors: true });
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
	await page.locator('#explain-trigger').click();
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
	await expect(metadata).toContainText(/Rows 1–\d+ of \d+/);
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
	await expect(page.locator('#explore-result-count')).toHaveText('Rows 0 of 2');
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
	await page.locator('#explain-trigger').click();
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
			const header = rect('#workbench-contextbar');
			const logo = rect('#logo');
			const context = rect('#workbench-repository-switcher');
			return {
				header: { top: header.top, height: header.height },
				logo: { top: logo.top, width: logo.width, height: logo.height },
				context: { top: context.top, height: context.height },
				font: style('#workbench-repository-switcher').fontFamily
			};
		});

		await page.goto(`${REPOSITORY_BASE_URL}/query`, { waitUntil: 'domcontentloaded' });
		await page.locator('#workbench-contextbar').waitFor({ state: 'attached' });
		const queryShell = await measureShell();
		await page.goto(`${REPOSITORY_BASE_URL}/explore?resource=%3Chttp%3A%2F%2Fexample.org%2Falice%3E`, {
			waitUntil: 'domcontentloaded'
		});
		await page.locator('#workbench-contextbar').waitFor({ state: 'attached' });
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
