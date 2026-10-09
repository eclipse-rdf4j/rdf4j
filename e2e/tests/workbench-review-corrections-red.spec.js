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

// Retired with the redesign (see .agent/execplans/workbench-stale-spec-migration-20261002.md): "mobile result footer
// remains reachable after the last record" measured the Previous/Next footer of the retired result iframe. Results now
// scroll with the page and continue with "Load more" (plan workbench-app-shell-and-critique-fixes-20260930, M4.1 and
// M9.1): workbench-result-scrolling.spec.js "mobile records use the table header labels and the last record is
// reachable" reaches the last record at 390px, workbench-second-review-red.spec.js "embedded tuple results keep paging
// secondary and reachable after keyboard scroll" reaches Load more at 390px, and workbench-query-load-more.spec.js
// "Execute requests one million rows by default and renders no result page controls" checks that no page footer is left.
//
// The query result is streamed into the page (#query-results) instead of the retired iframe, and the retired teal
// literal rgb(15, 118, 110) is compared with the --workbench-primary token.

const SERVER_BASE_URL = serverBaseUrl();
const WORKBENCH_BASE_URL = workbenchBaseUrl();
const REPOSITORY_ID = uniqueRepositoryId('workbench-review-corrections');

test.beforeAll(async ({ request }) => {
	const response = await request.put(`${SERVER_BASE_URL}/repositories/${REPOSITORY_ID}`, {
		headers: { 'Content-Type': 'text/turtle' },
		data: memoryRepositoryConfiguration(REPOSITORY_ID, 'Workbench review corrections test')
	});
	expect([200, 201, 204]).toContain(response.status());
});

test.afterAll(async ({ request }) => {
	await deleteRepository(request, SERVER_BASE_URL, REPOSITORY_ID);
});

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

test('mobile shared header stays compact while retaining aligned context actions', async ({ page }) => {
	await page.setViewportSize({ width: 390, height: 1000 });
	await page.goto(`${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/query`, { waitUntil: 'domcontentloaded' });
	const geometry = await page.locator('#workbench-contextbar').evaluate(element => {
		const rect = element.getBoundingClientRect();
		const switchers = [...element.querySelectorAll('.workbench-switcher__button')]
			.filter(button => button.getClientRects().length > 0)
			.map(button => {
				const box = button.getBoundingClientRect();
				return { top: box.top, bottom: box.bottom };
			});
		return { top: rect.top, bottom: rect.bottom, height: rect.height, switchers };
	});
	expect(geometry.height, 'the mobile shell header should not consume the workspace').toBeLessThanOrEqual(60);
	expect(geometry.switchers.length).toBeGreaterThan(0);
	for (const switcher of geometry.switchers) {
		expect(switcher.top, 'switchers share the context row').toBeGreaterThanOrEqual(geometry.top);
		expect(switcher.bottom, 'switchers share the context row').toBeLessThanOrEqual(geometry.bottom);
	}
});

test('desktop result toolbar keeps title and all actions in one header band', async ({ page }) => {
	await openQueryPage(page, REPOSITORY_ID, { viewport: { width: 1440, height: 1000 } });
	await runQuery(page, 'SELECT * WHERE { VALUES ?s { "one" "two" } }');
	const geometry = await page.locator('#query-results [data-query-stream-root]').evaluate(root => {
		const rect = element => {
			if (!element) return null;
			const value = element.getBoundingClientRect();
			return { top: value.top, left: value.left, width: value.width, bottom: value.bottom };
		};
		const title = root.querySelector('.query-result-toolbar__header h2');
		return {
			toolbar: rect(root.querySelector('.query-result-toolbar')),
			titleText: title?.textContent?.trim() || '',
			download: rect(root.querySelector('.query-result-download-toggle')),
			options: rect(root.querySelector('.query-result-options-toggle')),
			fullscreen: rect(root.querySelector('[id^="query-result-fullscreen-"]'))
		};
	});
	expect(geometry.toolbar).toBeTruthy();
	// The "Query result" heading still names the result region, but the output card's Results tab shows it, so the
	// heading is visually hidden there (plan M3.5); the actions form the toolbar's one row.
	expect(geometry.titleText, 'the result heading still names the result').toMatch(/query result/i);
	const controls = [geometry.download, geometry.options, geometry.fullscreen];
	for (const control of controls) {
		expect(control, 'result action should be rendered').toBeTruthy();
		expect(control.width, 'result action should have visible geometry').toBeGreaterThan(0);
		expect(Math.abs(control.top - controls[0].top), 'result actions should share one header band').toBeLessThan(4);
	}
	expect(geometry.toolbar.bottom - geometry.toolbar.top, 'desktop result actions should fit one row')
		.toBeLessThanOrEqual(60);
});

test('workbench action and data typography uses the shared readable scale', async ({ page }) => {
	await page.setViewportSize({ width: 1440, height: 1000 });
	await page.goto(`${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/information`, { waitUntil: 'domcontentloaded' });
	const dataFont = await page.locator('#workbench-page-surface').evaluate(element => {
		const data = element.querySelector('dl.workbench-kv dd');
		return data ? Number.parseFloat(getComputedStyle(data).fontSize) : 0;
	});
	await page.goto(`${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/update`, { waitUntil: 'domcontentloaded' });
	const fonts = await page.locator('#workbench-page-surface').evaluate(element => {
		const action = element.querySelector('input[type="submit"], input[type="button"], button');
		const probe = document.createElement('span');
		probe.style.fontSize = 'var(--workbench-control-font-size)';
		element.append(probe);
		const control = Number.parseFloat(getComputedStyle(probe).fontSize);
		probe.remove();
		return {
			action: action ? Number.parseFloat(getComputedStyle(action).fontSize) : 0,
			control,
			token: getComputedStyle(document.documentElement).getPropertyValue('--workbench-control-font-size').trim()
		};
	});
	expect(dataFont).toBeGreaterThanOrEqual(13);
	expect(fonts.token, 'the page defines --workbench-control-font-size').not.toBe('');
	// Actions use the shared control size, --workbench-control-font-size (13px), which replaced the 14px floor of this
	// review when the Workbench UI was unified (commit 847789c558); workbench-consistency-contract.spec.js "query,
	// upload, create, saved, and embedded controls share their locked action tokens" checks that 13px signature.
	expect(fonts.control).toBeGreaterThanOrEqual(13);
	expect(fonts.action, 'actions use the shared control font size').toBe(fonts.control);
});

test('repository access cells use text badges', async ({ page }) => {
	await page.setViewportSize({ width: 390, height: 900 });
	await page.goto(`${WORKBENCH_BASE_URL}/repositories/NONE/repositories`, { waitUntil: 'domcontentloaded' });
	await expect(page.locator('#repositories-results table.data tbody tr')).not.toHaveCount(0);
	// Plan task M5.3 replaced the eye and pencil icons (the pencil read as "Edit") with "Read" and "Write" badges.
	await expect(page.locator('#repositories-results .workbench-badge')).not.toHaveCount(0);
	await expect(page.locator('#repositories-results .workbench-status-icon')).toHaveCount(0);
	await expect(page.locator('#repositories-results img[src*="affirmative"], #repositories-results img[src*="negative"]')).toHaveCount(0);
});

test('creation actions stay compact and make Create the primary action', async ({ page }) => {
	await page.setViewportSize({ width: 1440, height: 1000 });
	await page.goto(`${WORKBENCH_BASE_URL}/repositories/NONE/create?type=memory-rdfs-dt`, { waitUntil: 'domcontentloaded' });
	await page.locator('form[action="create"] input[type="button"]').first().waitFor({ state: 'attached' });
	const primary = await primaryColor(page);
	const actions = await page.locator('form[action="create"] input[type="button"]').evaluateAll(elements => elements.map(element => {
		const rect = element.getBoundingClientRect();
		const wrapper = element.closest('.workbench-action');
		return {
			value: element.value,
			top: rect.top,
			background: getComputedStyle(element.closest('.workbench-action')).backgroundColor,
			backgroundImage: getComputedStyle(element).backgroundImage,
			wrapperClass: wrapper?.className || '',
			iconCount: wrapper?.querySelectorAll('.workbench-action-icon').length || 0
		};
	}));
	const cancel = actions.find(action => action.value === 'Cancel');
	const create = actions.find(action => action.value === 'Create');
	expect(cancel).toBeTruthy();
	expect(create).toBeTruthy();
	expect(Math.abs(cancel.top - create.top), 'actions should share a compact row').toBeLessThan(8);
	expect(create.background, 'Create should use the shared primary color').toBe(primary);
	expect(cancel.background, 'Cancel should remain a secondary surface').toBe('rgb(255, 255, 255)');
	expect(create.backgroundImage, 'Create should leave native control backgrounds clear').toBe('none');
	expect(cancel.backgroundImage, 'Cancel should leave native control backgrounds clear').toBe('none');
	expect(create.wrapperClass).toContain('workbench-action--primary');
	expect(cancel.wrapperClass).toContain('workbench-action--secondary');
	expect(create.iconCount).toBe(1);
	expect(cancel.iconCount).toBe(1);
});

test('shared shell keeps desktop gutters and mobile controls usable', async ({ page }) => {
	await page.setViewportSize({ width: 1440, height: 1000 });
	await page.goto(`${WORKBENCH_BASE_URL}/repositories/NONE/create?type=memory-rdfs-dt`, { waitUntil: 'domcontentloaded' });
	await page.locator('form[action="create"] input[type="button"]').first().waitFor({ state: 'attached' });
	const desktop = await page.locator('#navigation.workbench-nav').boundingBox();
	const content = await page.locator('#content').boundingBox();
	expect((content?.x ?? 0) - ((desktop?.x ?? 0) + (desktop?.width ?? 0)),
		'the page should keep a gutter from the desktop navigation rail').toBeGreaterThanOrEqual(16);

	await page.setViewportSize({ width: 390, height: 1000 });
	await page.goto(`${WORKBENCH_BASE_URL}/repositories/NONE/create?type=memory-rdfs-dt`, { waitUntil: 'domcontentloaded' });
	await page.locator('form[action="create"] input[type="button"]').first().waitFor({ state: 'attached' });
	const controlSelector = 'form[action="create"] input[type="text"], form[action="create"] select';
	const renderedHeights = () => page.locator(controlSelector).evaluateAll(elements => elements
		.filter(element => element.getClientRects().length > 0)
		.map(element => ({ id: element.id, height: element.getBoundingClientRect().height })));
	const controlCount = await page.locator(controlSelector).count();
	// The Advanced settings fields are not rendered until their pane is opened (plan M14.3), so measure the fields on
	// the form first and then the ones in the open pane; together they are every field of the form.
	const formControls = await renderedHeights();
	await page.locator('#create-advanced-toggle').press('Enter');
	await expect(page.locator('#create-advanced-panel')).toBeVisible();
	const allControls = await renderedHeights();
	const measured = new Map([...formControls, ...allControls].map(control => [control.id, control.height]));
	expect(measured.size, 'every form control is measured').toBe(controlCount);
	const changeTargets = await page.locator('#workbench-contextbar .workbench-switcher__button').evaluateAll(elements => elements
		.filter(element => element.getClientRects().length > 0).map(element => element.getBoundingClientRect().height));
	expect([...measured.values()].every(height => height >= 44),
		`mobile form controls should retain 44px targets: ${JSON.stringify([...measured])}`).toBe(true);
	expect(changeTargets.length).toBeGreaterThan(0);
	expect(changeTargets.every(height => height >= 40), 'context switchers should have touch-sized hit regions').toBe(true);
});

test('saved queries wrap names and actions without mobile page overflow', async ({ page }) => {
	await page.setViewportSize({ width: 390, height: 1000 });
	await page.goto(`${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/query`, { waitUntil: 'domcontentloaded' });
	await page.locator('.CodeMirror').first().waitFor({ state: 'visible' });
	await page.locator('.CodeMirror').first().evaluate(element => {
		element.CodeMirror.setValue('SELECT ?s WHERE { VALUES ?s { "mobile-saved" } }');
	});
	await page.locator('#save-query-toggle').press('Enter');
	await page.locator('#query-name').fill(`mobile-wrap-${Date.now()}`);
	await page.evaluate(() => window.workbench.query.handleNameChange());
	await page.locator('#save').click();
	await expect(page.locator('#save-feedback')).toHaveText('Query saved.');
	await page.goto(`${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/saved-queries`, { waitUntil: 'domcontentloaded' });
	await page.locator('.saved-query-row').first().waitFor({ state: 'attached' });
	// The saved query's name is a heading above its actions (plan M5.6), no longer a table header cell.
	const geometry = await page.evaluate(() => {
		const row = document.querySelector('.saved-query-row');
		const title = row?.querySelector('.saved-query-row__heading');
		const action = row?.querySelector('input[type="button"], input[type="submit"], button');
		const titleRect = title?.getBoundingClientRect();
		const actionRect = action?.getBoundingClientRect();
		return {
			bodyScrollWidth: document.body.scrollWidth,
			titleWidth: titleRect?.width ?? 0,
			titleBottom: titleRect?.bottom ?? 0,
			actionTop: actionRect?.top ?? 0,
		};
	});
	expect(geometry.bodyScrollWidth).toBeLessThanOrEqual(390);
	expect(geometry.titleWidth).toBeGreaterThan(300);
	expect(geometry.actionTop).toBeGreaterThanOrEqual(geometry.titleBottom);
});

test('embedded result option controls use the shared teal accent', async ({ page }) => {
	await openQueryPage(page, REPOSITORY_ID, { viewport: { width: 390, height: 1000 } });
	const primary = await primaryColor(page);
	await runQuery(page, 'SELECT * WHERE { VALUES ?s { "alpha" "beta" } }');
	const result = page.locator('#query-results [data-query-stream-root]');
	await result.locator('.query-result-options-toggle').press('Enter');
	const panel = result.locator('[id^="query-result-options-panel-"]');
	await expect(panel).toBeVisible();
	const accents = await panel.locator('input[type="checkbox"]').evaluateAll(elements =>
		elements.map(element => getComputedStyle(element).accentColor));
	expect(accents.length).toBeGreaterThan(0);
	expect(accents.every(accent => accent === primary), `${JSON.stringify(accents)} use ${primary}`).toBe(true);
});

test('destructive actions expose outline icons across their labels', async ({ page }) => {
	await page.setViewportSize({ width: 390, height: 1000 });
	// The page-level danger actions are buttons of the button component (plan M1.3) that name what they do (M6.4 to
	// M6.6); Remove and Delete stay disabled until something is chosen.
	for (const [route, label] of [
		['clear', /^Clear entire repository/],
		['remove', /^Remove\b/],
		['delete', /^Delete repository/]
	]) {
		const repository = route === 'delete' ? 'NONE' : REPOSITORY_ID;
		await page.goto(`${WORKBENCH_BASE_URL}/repositories/${repository}/${route}`, { waitUntil: 'domcontentloaded' });
		const action = page.locator('#workbench-outlet').getByRole('button', { name: label });
		await expect(action).toHaveCount(1);
		await expect(action).toBeVisible();
		await expect(action).toHaveCSS('background-image', 'none');
		await expect(action).toHaveClass(/workbench-action--danger/);
		await expect(action.locator('.workbench-action-icon')).toHaveCount(1);
	}
});

test('saved query details remain readable as labelled pairs on mobile', async ({ page }) => {
	await page.setViewportSize({ width: 390, height: 1000 });
	await page.goto(`${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/query`, { waitUntil: 'domcontentloaded' });
	await page.locator('.CodeMirror').first().waitFor({ state: 'visible' });
	await page.locator('.CodeMirror').first().evaluate(element => {
		element.CodeMirror.setValue('SELECT ?s WHERE { VALUES ?s { "saved-detail" } }');
	});
	await page.locator('#save-query-toggle').press('Enter');
	await page.locator('#query-name').fill(`saved-detail-${Date.now()}`);
	await page.evaluate(() => window.workbench.query.handleNameChange());
	await page.locator('#save').click();
	await expect(page.locator('#save-feedback')).toHaveText('Query saved.');
	await page.goto(`${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/saved-queries`, { waitUntil: 'domcontentloaded' });
	const toggle = page.locator('.saved-query-toggle').first();
	await toggle.waitFor({ state: 'visible' });
	await toggle.click();
	const metadata = page.locator('.saved-query-row [id$="-metadata"]').first();
	await expect(metadata).toBeVisible();
	const geometry = await metadata.evaluate(element => {
		const rect = element.getBoundingClientRect();
		return {
			width: rect.width,
			clientWidth: element.clientWidth,
			scrollWidth: element.scrollWidth,
			labels: Array.from(element.querySelectorAll('dt')).map(cell => cell.getBoundingClientRect().width),
			values: Array.from(element.querySelectorAll('dd')).map(cell => cell.getBoundingClientRect().width)
		};
	});
	expect(geometry.scrollWidth).toBeLessThanOrEqual(geometry.clientWidth + 1);
	expect(geometry.labels.every(width => width > 90), 'metadata labels should remain readable').toBe(true);
	expect(geometry.values.every(width => width > 90), 'metadata values should remain readable').toBe(true);
});
