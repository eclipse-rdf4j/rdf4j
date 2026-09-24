// @ts-check
const { test, expect } = require('@playwright/test');

const SERVER_BASE_URL = process.env.RDF4J_SERVER_BASE_URL || 'http://127.0.0.1:8080/rdf4j-server';
const WORKBENCH_BASE_URL = process.env.RDF4J_WORKBENCH_BASE_URL || 'http://127.0.0.1:8080/rdf4j-workbench';
const REPOSITORY_ID = 'query-refresh-layout';
const MEMBER_REPOSITORY_ID = 'query-refresh-layout-member';

test.beforeEach(async ({ request }) => {
	for (const repositoryId of [REPOSITORY_ID, MEMBER_REPOSITORY_ID]) {
		await request.delete(`${SERVER_BASE_URL}/repositories/${repositoryId}`);
		const response = await request.put(`${SERVER_BASE_URL}/repositories/${repositoryId}`, {
			headers: { 'Content-Type': 'text/turtle' },
			data: nativeRepositoryConfig(repositoryId)
		});
		expect([200, 201, 204]).toContain(response.status());
	}
});

async function setQuery(page, query) {
	await page.locator('.CodeMirror').first().evaluate((element, value) => {
		element.CodeMirror.setValue(value);
	}, query);
}

async function executeSelect(page) {
	const request = page.waitForRequest(candidate => {
		if (!candidate.url().includes('/query')) {
			return false;
		}
		if (candidate.method() === 'GET') {
			return new URL(candidate.url()).searchParams.get('action') === 'exec';
		}
		return candidate.method() === 'POST'
			&& new URLSearchParams(candidate.postData() || '').get('action') === 'exec';
	});
	await page.locator('#exec').click();
	await request;
	await page.frameLocator('#query-results-frame').locator('#rdf4j-query-result').waitFor({ state: 'attached', timeout: 30000 });
	await expect(page.locator('#query-request-id')).toHaveValue('', { timeout: 30000 });
}

function nativeRepositoryConfig(repositoryId) {
	return `@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix config: <tag:rdf4j.org,2023:config/>.

[] a config:Repository ;
   config:rep.id "${repositoryId}" ;
   rdfs:label "Workbench review corrections test" ;
   config:rep.impl [
      config:rep.type "openrdf:SailRepository" ;
      config:sail.impl [
         config:sail.type "openrdf:MemoryStore"
      ]
   ].
`;
}

test('mobile shared header stays compact while retaining aligned context actions', async ({ page }) => {
	await page.setViewportSize({ width: 390, height: 1000 });
	await page.goto(`${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/query`, { waitUntil: 'domcontentloaded' });
	const geometry = await page.locator('#header').evaluate(element => {
		const rect = element.getBoundingClientRect();
		const rows = [...element.querySelectorAll('#contentheader tr')].map(row => {
			const rowRect = row.getBoundingClientRect();
			const change = row.querySelector('.change')?.getBoundingClientRect();
			return { height: rowRect.height, changeTop: change?.top ?? -1, rowTop: rowRect.top };
		});
		return { height: rect.height, rows };
	});
	expect(geometry.height, 'the mobile shell header should not consume the workspace').toBeLessThanOrEqual(190);
	for (const row of geometry.rows) {
		expect(Math.abs(row.changeTop - row.rowTop), 'Change should share the context row').toBeLessThan(8);
	}
});

test('desktop result toolbar keeps title and all actions in one header band', async ({ page }) => {
	await page.setViewportSize({ width: 1440, height: 1000 });
	await page.goto(`${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/query`, { waitUntil: 'domcontentloaded' });
	await page.locator('.CodeMirror').first().waitFor();
	await setQuery(page, 'SELECT * WHERE { VALUES ?s { "one" "two" } }');
	await executeSelect(page);
	const frame = page.frames().find(candidate => candidate !== page.mainFrame() && candidate.url().includes('/query'));
	expect(frame, 'embedded result frame should be available').toBeTruthy();
	const geometry = await frame.evaluate(() => {
		const rect = selector => {
			const element = document.querySelector(selector);
			if (!element) return null;
			const value = element.getBoundingClientRect();
			return { top: value.top, left: value.left, width: value.width, bottom: value.bottom };
		};
		const title = document.querySelector('.query-result-toolbar__header h2, .query-result-embedded-header h2');
		const titleRect = title?.getBoundingClientRect();
		const toolbarElement = document.querySelector('.query-result-toolbar');
		const toolbarStyle = toolbarElement ? getComputedStyle(toolbarElement) : null;
		return {
			toolbar: rect('.query-result-toolbar'),
			gridTemplateAreas: toolbarStyle?.gridTemplateAreas || '',
			title: titleRect ? {
				top: titleRect.top,
				left: titleRect.left,
				width: titleRect.width,
				bottom: titleRect.bottom,
				text: title.textContent?.trim() || ''
			} : null,
			download: rect('#query-result-download-toggle'),
			options: rect('#query-result-options-toggle'),
			fullscreen: rect('#query-result-fullscreen')
		};
	});
	expect(geometry.toolbar).toBeTruthy();
	expect(geometry.gridTemplateAreas.replace(/\s+/g, ' '), 'desktop result toolbar should use one row')
		.toContain('"title download options fullscreen"');
	expect(geometry.title, 'embedded result title should be rendered').toBeTruthy();
	expect(geometry.title.text).toMatch(/query result/i);
	expect(geometry.title.width, 'embedded result title should have visible geometry').toBeGreaterThan(0);
	for (const control of [geometry.download, geometry.options, geometry.fullscreen]) {
		expect(control, 'result action should be rendered').toBeTruthy();
		expect(control.width, 'result action should have visible geometry').toBeGreaterThan(0);
		expect(Math.abs(control.top - geometry.title.top), 'result actions should share the title band').toBeLessThan(4);
	}
	expect(geometry.toolbar.bottom - geometry.toolbar.top, 'desktop result actions should fit one row')
		.toBeLessThanOrEqual(60);
});

test('mobile result footer remains reachable after the last record', async ({ page }) => {
	await page.setViewportSize({ width: 390, height: 1000 });
	await page.goto(`${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/query`, { waitUntil: 'domcontentloaded' });
	await page.locator('.CodeMirror').first().waitFor();
	await setQuery(page, 'SELECT * WHERE { VALUES ?s { "one" "two" "three" } }');
	await executeSelect(page);
	const frame = page.frames().find(candidate => candidate !== page.mainFrame() && candidate.url().includes('/query'));
	expect(frame).toBeTruthy();
	const geometry = await frame.evaluate(() => {
		const navigation = document.querySelector('.query-result-navigation');
		const table = document.querySelector('#query-result-table-wrap');
		if (!navigation || !table) return null;
		const navRect = navigation.getBoundingClientRect();
		const tableRect = table.getBoundingClientRect();
		return { navigationBottom: navRect.bottom, tableBottom: tableRect.bottom, bodyHeight: document.body.scrollHeight };
	});
	expect(geometry).toBeTruthy();
	expect(geometry.navigationBottom).toBeLessThanOrEqual(geometry.bodyHeight + 1);
	await frame.locator('#nextX').scrollIntoViewIfNeeded();
	await expect(frame.locator('#nextX')).toBeVisible();
});

test('workbench action and data typography uses the shared readable scale', async ({ page }) => {
	await page.setViewportSize({ width: 1440, height: 1000 });
	await page.goto(`${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/information`, { waitUntil: 'domcontentloaded' });
	const dataFont = await page.locator('#workbench-page-surface').evaluate(element => {
		const data = element.querySelector('table.simple td');
		return data ? Number.parseFloat(getComputedStyle(data).fontSize) : 0;
	});
	await page.goto(`${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/update`, { waitUntil: 'domcontentloaded' });
	const actionFont = await page.locator('#workbench-page-surface').evaluate(element => {
		const action = element.querySelector('input[type="submit"], input[type="button"], button');
		return action ? Number.parseFloat(getComputedStyle(action).fontSize) : 0;
	});
	expect(dataFont).toBeGreaterThanOrEqual(13);
	expect(actionFont).toBeGreaterThanOrEqual(14);
});

test('repository status cells use accessible outline icons', async ({ page }) => {
	await page.setViewportSize({ width: 390, height: 900 });
	await page.goto(`${WORKBENCH_BASE_URL}/repositories/NONE/repositories`, { waitUntil: 'domcontentloaded' });
	await expect(page.locator('#repositories-results table.data tbody tr')).not.toHaveCount(0);
	await expect(page.locator('#repositories-results .workbench-status-icon')).not.toHaveCount(0);
	await expect(page.locator('#repositories-results img[src*="affirmative"], #repositories-results img[src*="negative"]')).toHaveCount(0);
});

test('creation actions stay compact and make Create the primary action', async ({ page }) => {
	await page.setViewportSize({ width: 1440, height: 1000 });
	await page.goto(`${WORKBENCH_BASE_URL}/repositories/NONE/create?type=memory-rdfs-dt`, { waitUntil: 'domcontentloaded' });
	await page.locator('form[action="create"] input[type="button"]').first().waitFor({ state: 'attached' });
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
	expect(create.background, 'Create should use the shared teal primary').toBe('rgb(15, 118, 110)');
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
	expect(desktop?.x ?? 0, 'desktop navigation should keep an outer gutter').toBeGreaterThanOrEqual(16);

	await page.setViewportSize({ width: 390, height: 1000 });
	await page.goto(`${WORKBENCH_BASE_URL}/repositories/NONE/create?type=memory-rdfs-dt`, { waitUntil: 'domcontentloaded' });
	await page.locator('form[action="create"] input[type="button"]').first().waitFor({ state: 'attached' });
	const controls = await page.locator('form[action="create"] input[type="text"], form[action="create"] select').evaluateAll(elements => elements.map(element => element.getBoundingClientRect().height));
	const changeTargets = await page.locator('#contentheader .change a').evaluateAll(elements => elements.map(element => element.getBoundingClientRect().height));
	expect(controls.every(height => height >= 44), 'mobile form controls should retain 44px targets').toBe(true);
	expect(changeTargets.length).toBeGreaterThan(0);
	expect(changeTargets.every(height => height >= 40), 'context Change links should have touch-sized hit regions').toBe(true);
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
	const geometry = await page.evaluate(() => {
		const row = document.querySelector('.saved-query-row');
		const title = row?.querySelector('th');
		const action = row?.querySelector('input[type="button"], input[type="submit"]');
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
	await page.setViewportSize({ width: 390, height: 1000 });
	await page.goto(`${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/query`, { waitUntil: 'domcontentloaded' });
	await page.locator('.CodeMirror').first().waitFor({ state: 'visible' });
	await page.locator('.CodeMirror').first().evaluate(element => {
		element.CodeMirror.setValue('SELECT * WHERE { VALUES ?s { "alpha" "beta" } }');
	});
	await page.locator('#exec').click();
	const frame = page.frameLocator('#query-results-frame');
	await frame.locator('#rdf4j-query-result').waitFor({ state: 'attached' });
	await frame.locator('#query-result-options-toggle').press('Enter');
	const accents = await frame.locator('input[type="checkbox"]').evaluateAll(elements =>
		elements.map(element => getComputedStyle(element).accentColor));
	expect(accents.length).toBeGreaterThan(0);
	expect(accents.every(accent => accent === 'rgb(15, 118, 110)')).toBe(true);
});

test('destructive actions expose outline icons across their labels', async ({ page }) => {
	await page.setViewportSize({ width: 390, height: 1000 });
	for (const [route, label] of [
		['clear', 'Clear Context(s)'],
		['remove', 'Remove'],
		['delete', 'Delete']
	]) {
		const repository = route === 'delete' ? 'NONE' : REPOSITORY_ID;
		await page.goto(`${WORKBENCH_BASE_URL}/repositories/${repository}/${route}`, { waitUntil: 'domcontentloaded' });
		const action = page.locator(`input[value="${label}"]`).first();
		await action.waitFor({ state: 'visible' });
		await expect(action).toHaveCSS('background-image', 'none');
		await expect(action.locator('xpath=ancestor::span[contains(@class, "workbench-action--danger")]'))
			.toHaveClass(/workbench-action--danger/);
		await expect(action.locator('xpath=ancestor::span[contains(@class, "workbench-action")]').locator('.workbench-action-icon'))
			.toHaveCount(1);
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
	const metadata = page.locator('.saved-query-row table[id$="-metadata"]').first();
	await expect(metadata).toBeVisible();
	const geometry = await metadata.evaluate(element => {
		const rect = element.getBoundingClientRect();
		return {
			width: rect.width,
			clientWidth: element.clientWidth,
			scrollWidth: element.scrollWidth,
			labels: Array.from(element.querySelectorAll('th')).map(cell => cell.getBoundingClientRect().width),
			values: Array.from(element.querySelectorAll('td')).map(cell => cell.getBoundingClientRect().width)
		};
	});
	expect(geometry.scrollWidth).toBeLessThanOrEqual(geometry.clientWidth + 1);
	expect(geometry.labels.every(width => width > 90), 'metadata labels should remain readable').toBe(true);
	expect(geometry.values.every(width => width > 90), 'metadata values should remain readable').toBe(true);
});
