// @ts-check
const { test, expect } = require('@playwright/test');
const { runQuery, serverBaseUrl, uniqueRepositoryId, workbenchBaseUrl } = require('./workbench-test-helpers.js');

// Migrated and retired with the redesign (see .agent/execplans/workbench-stale-spec-migration-20261002.md). The query
// result is read in the page (#query-results [data-query-stream-root]) instead of the retired iframe (plan task M9.1).
// Retired: "short embedded results do not expose the Workbench canvas below their content" measured the iframe's own
// document below a short result, which no longer exists; "embedded tuple result heading remains the paging script
// target" protected the heading that tuple.js paging wrote into, and Load more replaced result paging
// (design/workbench-loadmore-20260930/handoff.md: no Next/Previous or page-size controls for query results).
// "query, update, and saved editor utilities stay outlined and clear of long first lines" still fails: the Query
// editor reserves 3.5rem beside its lines for two 28px overlay buttons, so its Share button covers the end of a long
// first line (a product bug, kept failing on purpose).

const SERVER_BASE_URL = serverBaseUrl();
const WORKBENCH_BASE_URL = workbenchBaseUrl();
const REPOSITORY_ID = uniqueRepositoryId('workbench-consistency');
const REPOSITORY_BASE_URL = `${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}`;
const LONG_LINE_QUERY = `SELECT ?subject ?predicate ?object WHERE { ?subject ?predicate ?object BIND("${'long-value-'.repeat(42)}" AS ?longText) }`;

test.beforeAll(async ({ request }) => {
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

function repositoryConfig(repositoryId) {
	return `@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix config: <tag:rdf4j.org,2023:config/>.

[] a config:Repository ;
   config:rep.id "${repositoryId}" ;
   rdfs:label "Workbench consistency fixture" ;
   config:rep.impl [
      config:rep.type "openrdf:SailRepository" ;
      config:sail.impl [ config:sail.type "openrdf:MemoryStore" ]
   ].`;
}

test.afterAll(async ({ request }) => {
	await request.delete(`${SERVER_BASE_URL}/repositories/${REPOSITORY_ID}`);
});

async function saveQuery(page, queryName, queryText = 'SELECT ?s WHERE { ?s <http://example.org/name> ?name } LIMIT 2') {
	await page.goto(`${REPOSITORY_BASE_URL}/query`, { waitUntil: 'domcontentloaded' });
	await page.locator('#workbench-page-surface').waitFor({ state: 'attached' });
	const editor = page.locator('.CodeMirror').first();
	await editor.waitFor();
	await editor.evaluate((element, value) => element.CodeMirror.setValue(value), queryText);
	await page.locator('#save-query-toggle').press('Enter');
	await page.locator('#query-name').fill(queryName);
	await page.evaluate(() => window.workbench.query.handleNameChange());
	await page.locator('#save').click();
	await expect(page.locator('#save-feedback')).toContainText('Query saved.');
}

async function openWorkbenchPage(page, route) {
	await page.goto(`${WORKBENCH_BASE_URL}/${route}`, { waitUntil: 'domcontentloaded' });
	await page.locator('#workbench-page-surface').waitFor({ state: 'attached' });
}

async function readControlSignature(page, locator) {
	await page.mouse.move(0, 0);
	const panelId = await locator.evaluate(element => element.closest('.query-disclosure__panel')?.id);
	if (panelId) {
		await expect.poll(() => page.locator(`#${panelId}`).evaluate(element => element.getAnimations().length))
			.toBe(0);
	}
	const control = await locator.evaluateHandle(element => {
		const control = element.matches('button, input, select, textarea, a')
			? element
			: element.querySelector('button, input, select, textarea, a');
		if (control && control.disabled) {
			control.disabled = false;
		}
		return control;
	});
	// Safari moves focus to buttons with Option+Tab.
	const nextControl = page.context().browser()?.browserType().name() === 'webkit' ? 'Alt+Tab' : 'Tab';
	await control.focus();
	await page.keyboard.press(nextControl);
	await page.keyboard.press(`Shift+${nextControl}`);
	// Firefox lets Tab leave the document after its last tab stop (Add's Upload), and Shift+Tab then lands on the
	// stop before it; focus the control again, which keeps the keyboard focus indicator.
	if (!(await control.evaluate(element => element === document.activeElement))) {
		await control.focus();
	}
	const signature = await locator.evaluate(element => {
		const style = getComputedStyle(element);
		const bounds = element.getBoundingClientRect();
		return {
			fontFamily: style.fontFamily,
			fontSize: style.fontSize,
			fontWeight: style.fontWeight,
			height: Math.round(bounds.height),
			borderRadius: style.borderRadius,
			borderColor: style.borderColor,
			outlineStyle: style.outlineStyle,
			outlineWidth: style.outlineWidth,
			outlineColor: style.outlineColor,
			outlineOffset: style.outlineOffset,
			focusIndicator: style.outlineStyle !== 'none' && parseFloat(style.outlineWidth) >= 2,
			boxShadow: style.boxShadow
		};
	});
	await control.dispose();
	return signature;
}

test('query, upload, create, saved, and embedded controls share their locked action tokens', async ({ page }) => {
	await page.setViewportSize({ width: 1440, height: 1000 });
	await openWorkbenchPage(page, `repositories/${REPOSITORY_ID}/query`);
	await runQuery(page, 'SELECT * WHERE { ?s ?p ?o } LIMIT 2');
	const result = page.locator('#query-results [data-query-stream-root]');
	await result.locator('.query-result-options-toggle').click();
	const embeddedField = result.locator('[id^="result-layout-"]');
	await expect(embeddedField).toBeVisible();
	await embeddedField.focus();
	await page.keyboard.press('Tab');
	await page.keyboard.press('Shift+Tab');
	const embeddedFieldSignature = await embeddedField.evaluate(element => {
		const style = getComputedStyle(element);
		const bounds = element.getBoundingClientRect();
		return {
			fontFamily: style.fontFamily,
			fontSize: style.fontSize,
			fontWeight: style.fontWeight,
			height: Math.round(bounds.height),
			borderRadius: style.borderRadius,
			borderColor: style.borderColor,
			outlineStyle: style.outlineStyle,
			outlineWidth: style.outlineWidth,
			outlineColor: style.outlineColor,
			outlineOffset: style.outlineOffset,
			boxShadow: style.boxShadow,
			focusIndicator: style.outlineStyle !== 'none' && parseFloat(style.outlineWidth) >= 2
		};
	});
	// The result's paging buttons are gone (Load more replaced result paging); its secondary action is Full screen.
	const embeddedAction = await readControlSignature(page, result.locator('.query-results__fullscreen'));

	const queryAction = await readControlSignature(page, page.locator('#exec'));
	await page.locator('#save-query-toggle').click();
	const queryField = await readControlSignature(page, page.locator('#query-name'));
	await openWorkbenchPage(page, `repositories/${REPOSITORY_ID}/add`);
	await page.locator('#source-file').focus();
	await page.keyboard.press('ArrowRight');
	const addField = await readControlSignature(page, page.locator('#url'));
	const uploadAction = await readControlSignature(page, page.locator('#add-upload-actions .workbench-action--primary'));
	await openWorkbenchPage(page, 'repositories/NONE/create?type=memory-rdfs-dt');
	const createField = await readControlSignature(page, page.locator('form[action="create"] input[name="Repository ID"]'));
	const createAction = await readControlSignature(page, page.locator('form[action="create"] .workbench-action--primary'));
	const cancelAction = await readControlSignature(page, page.locator('form[action="create"] .workbench-action--secondary'));
	const queryName = `shared-style-${Date.now()}`;
	await saveQuery(page, queryName);
	await openWorkbenchPage(page, `repositories/${REPOSITORY_ID}/saved-queries`);
	const savedAction = await readControlSignature(page, page.locator('.saved-query-row .workbench-action--primary'));

	for (const [name, signature] of Object.entries({ queryAction, uploadAction, createAction, savedAction })) {
		expect(signature, name).toMatchObject({
			fontSize: '13px',
			fontWeight: '600',
			height: 36,
			borderRadius: '7px'
		});
		expect(signature.focusIndicator, `${name}: ${JSON.stringify(signature)}`).toBe(true);
	}
	expect(queryAction).toEqual(uploadAction);
	expect(uploadAction).toEqual(createAction);
	expect(createAction).toEqual(savedAction);
	for (const [name, signature] of Object.entries({ queryField, addField, createField, embeddedFieldSignature })) {
		expect(signature, name).toMatchObject({
			fontSize: '13px',
			fontWeight: '400',
			borderRadius: '7px',
			borderColor: 'rgb(119, 138, 146)',
			focusIndicator: true
		});
		expect(signature.height, name).toBeGreaterThanOrEqual(35);
		expect(signature.height, name).toBeLessThanOrEqual(36);
	}
	expect(queryField).toEqual(addField);
	expect(addField).toEqual(createField);
	const fieldTokens = signature => ({
		fontFamily: signature.fontFamily,
		fontSize: signature.fontSize,
		fontWeight: signature.fontWeight,
		borderRadius: signature.borderRadius,
		borderColor: signature.borderColor,
		outlineStyle: signature.outlineStyle,
		outlineWidth: signature.outlineWidth,
		outlineColor: signature.outlineColor,
		outlineOffset: signature.outlineOffset,
		focusIndicator: signature.focusIndicator
	});
	expect(fieldTokens(createField)).toEqual(fieldTokens(embeddedFieldSignature));
	expect(cancelAction).toMatchObject({ fontSize: '13px', fontWeight: '600', height: 36, borderRadius: '7px' });
	expect(cancelAction.focusIndicator).toBe(true);
	expect(embeddedAction.focusIndicator).toBe(true);
	expect(embeddedAction).toEqual(cancelAction);
});

test('Add Source is a keyboard-operable, equal-width segmented radio group on mobile', async ({ page }) => {
	await page.setViewportSize({ width: 390, height: 1000 });
	await openWorkbenchPage(page, `repositories/${REPOSITORY_ID}/add`);
	const radios = page.locator('#add-source-tabs input[type="radio"]');
	await expect(radios).toHaveCount(3);
	const labels = page.locator('#add-source-tabs label');
	const geometry = await labels.evaluateAll(elements => elements.map(element => {
		const rect = element.getBoundingClientRect();
		const style = getComputedStyle(element);
		return { width: rect.width, height: rect.height, radius: style.borderRadius, background: style.backgroundColor };
	}));
	expect(geometry[0].width).toBeCloseTo(geometry[1].width, 0);
	expect(geometry[1].width).toBeCloseTo(geometry[2].width, 0);
	for (const segment of geometry) {
		expect(segment.height).toBeGreaterThanOrEqual(44);
	}
	// The segments join into one control with rounded outer corners (plan task M5.5, mockup 10).
	expect(geometry.map(segment => segment.radius)).toEqual(['7px 0px 0px 7px', '0px', '0px 7px 7px 0px']);
	expect(geometry[0].background).not.toBe(geometry[1].background);
	await page.locator('#source-file').focus();
	await page.keyboard.press('ArrowRight');
	await expect(page.locator('#source-url')).toBeChecked();
	await expect(page.locator('#add-source-url-panel')).toBeVisible();
	await expect(page.locator('#add-source-file-panel')).toBeHidden();
});

test('System Information keeps every section and live value inside one aligned surface', async ({ page }) => {
	await page.setViewportSize({ width: 1440, height: 1000 });
	await openWorkbenchPage(page, 'repositories/NONE/information');
	const island = page.locator('#workbench-information.workbench-island');
	await expect(island).toBeVisible();
	for (const sectionId of ['information-application', 'information-runtime', 'information-memory']) {
		await expect(island.locator(`#${sectionId}`)).toBeVisible();
		await expect(page.locator(`#${sectionId}.workbench-island`)).toHaveCount(0);
	}
	await expect(island.getByText('RDF4J Workbench', { exact: true })).toBeVisible();
	await expect(island.getByText(/MB$/, { exact: false }).first()).toBeVisible();
	const labelWidth = await island.locator('dl.workbench-kv dt').first().evaluate(element => element.getBoundingClientRect().width);
	expect(labelWidth).toBeGreaterThanOrEqual(160);
	const desktopMetrics = await island.evaluate(element => ({
		directSurfaces: element.querySelectorAll(':scope > .workbench-island').length,
		sections: element.querySelectorAll('section').length,
		width: element.getBoundingClientRect().width
	}));
	expect(desktopMetrics.directSurfaces).toBe(0);
	expect(desktopMetrics.sections).toBe(3);
});

test('context bar switchers stay compact and use muted keys', async ({ page }) => {
	await page.setViewportSize({ width: 1440, height: 1000 });
	await openWorkbenchPage(page, `repositories/${REPOSITORY_ID}/query`);
	const switchers = page.locator('#workbench-contextbar .workbench-switcher__button');
	await expect(switchers).toHaveCount(2);
	const measurements = await switchers.evaluateAll(elements => elements.map(element => {
		const key = element.querySelector('.workbench-switcher__key');
		return {
			popup: element.getAttribute('aria-haspopup'),
			expanded: element.getAttribute('aria-expanded'),
			height: element.getBoundingClientRect().height,
			keyColor: key ? getComputedStyle(key).color : null,
			keyTransform: key ? getComputedStyle(key).textTransform : null
		};
	}));
	for (const switcher of measurements) {
		expect(switcher.popup).toBe('dialog');
		expect(switcher.expanded).toBe('false');
		expect(switcher.height).toBeLessThanOrEqual(44);
		if (switcher.keyColor) {
			expect(switcher.keyColor).toBe('rgb(79, 97, 104)');
			expect(switcher.keyTransform).toBe('uppercase');
		}
	}
});

test('desktop navigation surface keeps its locked width and gutter', async ({ page }) => {
	await page.setViewportSize({ width: 1440, height: 1000 });
	await openWorkbenchPage(page, `repositories/${REPOSITORY_ID}/query`);
	const metrics = await page.evaluate(() => {
		const navigation = document.querySelector('.workbench-navigation-disclosure').getBoundingClientRect();
		const main = document.querySelector('#content').getBoundingClientRect();
		return { left: navigation.left, width: navigation.width, gap: main.left - navigation.right };
	});
	expect(metrics.left).toBe(24);
	expect(metrics.width).toBe(184);
	expect(metrics.gap).toBe(24);
});

// The saved-query row offers Execute, Show details, Edit and Delete as one wrapping row of buttons (the streamed page
// model replaced the XSLT row with its Link action and two-column grid; Show details and Edit are text buttons since
// plan tasks M1.3 and M5.6), so the grid positions and the eye and pencil icons are no longer checked.
test('Saved Query actions keep touch-sized controls and wrapping metadata on mobile', async ({ page }) => {
	await page.setViewportSize({ width: 390, height: 1000 });
	const queryName = `mobile-layout-${Date.now()}`;
	await saveQuery(page, queryName);
	await openWorkbenchPage(page, `repositories/${REPOSITORY_ID}/saved-queries`);
	const row = page.locator('.saved-query-row').filter({ hasText: queryName });
	await expect(row).toBeVisible();
	const actions = row.locator('.saved-query-actions .workbench-action');
	const actionBoxes = await actions.evaluateAll(elements => elements.map(element => {
		const rect = element.getBoundingClientRect();
		const row = element.closest('.saved-query-row');
		const rowStyle = getComputedStyle(row);
		const rowBounds = row.getBoundingClientRect();
		return {
			label: element.querySelector('input')?.value || element.textContent.trim(),
			x: rect.x, y: rect.y, width: rect.width, height: rect.height, right: rect.right,
			rowContentLeft: rowBounds.left + parseFloat(rowStyle.borderLeftWidth) + parseFloat(rowStyle.paddingLeft),
			rowContentRight: rowBounds.right - parseFloat(rowStyle.borderRightWidth) - parseFloat(rowStyle.paddingRight)
		};
	}));
	expect(actionBoxes.map(action => action.label)).toEqual(['Execute', 'Show details', 'Edit', 'Delete…']);
	for (const action of actionBoxes) {
		expect(action.height, `${action.label} is a touch target`).toBeGreaterThanOrEqual(44);
		expect(action.x, `${action.label} starts inside its row`).toBeGreaterThanOrEqual(action.rowContentLeft - 1);
		expect(action.right, `${action.label} wraps inside its row`).toBeLessThanOrEqual(action.rowContentRight + 1);
	}
	await expect(actions.nth(0).locator('.workbench-action-icon')).toHaveClass(/workbench-action-icon--execute/);
	await expect(actions.nth(0).locator('.workbench-action-icon path'))
		.toHaveAttribute('d', 'M8 5 19 12 8 19V5Z');
	await row.locator('.saved-query-toggle').click();
	// The details are a key/value list (plan task M5.6).
	const metadata = row.locator('[id$="-metadata"]');
	await expect(metadata).toBeVisible();
	const metadataGeometry = await metadata.evaluate(details => {
		const firstRow = details.querySelector('.workbench-kv__row').getBoundingClientRect();
		const bounds = details.getBoundingClientRect();
		return { tableWidth: bounds.width, rowWidth: firstRow.width, rowRight: firstRow.right, tableRight: bounds.right };
	});
	expect(metadataGeometry.rowWidth).toBeGreaterThanOrEqual(metadataGeometry.tableWidth - 1);
	expect(metadataGeometry.rowRight).toBeCloseTo(metadataGeometry.tableRight, 0);
	const pageMetrics = await page.evaluate(() => ({
		viewport: document.documentElement.clientWidth,
		content: document.documentElement.scrollWidth,
		marginTop: parseFloat(getComputedStyle(document.querySelector('.saved-query-row .saved-query-metadata')).marginTop)
	}));
	expect(pageMetrics.content).toBeLessThanOrEqual(pageMetrics.viewport);
	expect(pageMetrics.marginTop).toBeGreaterThanOrEqual(16);
});

test('expanded saved-query code wraps without horizontal clipping on mobile', async ({ page }) => {
	await page.setViewportSize({ width: 390, height: 1000 });
	const queryName = `mobile-code-wrap-${Date.now()}`;
	await saveQuery(page, queryName);
	await openWorkbenchPage(page, `repositories/${REPOSITORY_ID}/saved-queries`);
	const row = page.locator('.saved-query-row').filter({ hasText: queryName });
	await row.locator('.saved-query-toggle').click();
	const wrapping = await row.locator('.CodeMirror').evaluate(element => ({
		lineWrapping: element.CodeMirror.getOption('lineWrapping'),
		textWidth: element.querySelector('.CodeMirror-code').scrollWidth,
		viewportWidth: element.querySelector('.CodeMirror-scroll').clientWidth
	}));
	expect(wrapping.lineWrapping).toBe(true);
	expect(wrapping.textWidth).toBeLessThanOrEqual(wrapping.viewportWidth);
});

test('administration and embedded result table headers share one typography and row system', async ({ page }) => {
	await page.setViewportSize({ width: 1440, height: 1000 });
	const signatures = [];
	for (const route of ['repositories/NONE/repositories', `repositories/${REPOSITORY_ID}/namespaces`, `repositories/${REPOSITORY_ID}/contexts`, `repositories/${REPOSITORY_ID}/types`]) {
		await openWorkbenchPage(page, route);
		const header = page.locator('#workbench-page-surface table.data th').first();
		await header.waitFor();
		signatures.push(await header.evaluate(element => {
			const style = getComputedStyle(element);
			return {
				fontFamily: style.fontFamily,
				fontSize: style.fontSize,
				fontWeight: style.fontWeight,
				backgroundColor: style.backgroundColor,
				color: style.color,
				borderBottomColor: style.borderBottomColor,
				borderBottomWidth: style.borderBottomWidth,
				padding: style.padding
			};
		}));
	}
	await openWorkbenchPage(page, `repositories/${REPOSITORY_ID}/query`);
	await runQuery(page, 'SELECT * WHERE { ?s ?p ?o } LIMIT 2');
	const embeddedHeader = page.locator('#query-results [data-query-stream-root] [id^="query-result-table-wrap-"] table.data th')
		.first();
	await embeddedHeader.waitFor();
	const codeFont = await page.evaluate(() => {
		const probe = document.createElement('span');
		probe.style.fontFamily = 'var(--workbench-code-font)';
		document.querySelector('#query-results').append(probe);
		const family = getComputedStyle(probe).fontFamily;
		probe.remove();
		return family;
	});
	const embeddedSignature = await embeddedHeader.evaluate(element => {
		const style = getComputedStyle(element);
		return {
			fontFamily: style.fontFamily,
			fontSize: style.fontSize,
			fontWeight: style.fontWeight,
			backgroundColor: style.backgroundColor,
			color: style.color,
			borderBottomColor: style.borderBottomColor,
			borderBottomWidth: style.borderBottomWidth,
			padding: style.padding
		};
	});
	// Result headers show ?variable in the code font since plan task M4.3; everything else matches the page tables.
	expect(embeddedSignature.fontFamily, 'result headers use the code font token').toBe(codeFont);
	signatures.push({ ...embeddedSignature, fontFamily: signatures[0].fontFamily });
	for (const signature of signatures) {
		expect(signature).toMatchObject({ fontSize: '13px', fontWeight: '600' });
	}
	for (const signature of signatures.slice(1)) {
		expect(signature).toEqual(signatures[0]);
	}
});

test('query, forms, information, saved, and table routes fit every locked viewport', async ({ page }) => {
	const queryName = `responsive-layout-${Date.now()}`;
	await saveQuery(page, queryName);
	const routes = [
		`repositories/${REPOSITORY_ID}/query`,
		`repositories/${REPOSITORY_ID}/add`,
		'repositories/NONE/create?type=memory-rdfs-dt',
		'repositories/NONE/information',
		'repositories/NONE/repositories',
		`repositories/${REPOSITORY_ID}/namespaces`,
		`repositories/${REPOSITORY_ID}/contexts`,
		`repositories/${REPOSITORY_ID}/types`,
		`repositories/${REPOSITORY_ID}/saved-queries`
	];
	for (const width of [320, 390, 768, 1440]) {
		await page.setViewportSize({ width, height: width < 600 ? 900 : 1000 });
		for (const route of routes) {
			await openWorkbenchPage(page, route);
			if (route.endsWith('/add')) {
				const actionHeight = await page.locator('#add-upload-actions .workbench-action--primary')
					.evaluate(element => Math.round(element.getBoundingClientRect().height));
				expect(actionHeight, `Add upload action at ${width}px`).toBe(width <= 900 ? 44 : 36);
			}
			const metrics = await page.evaluate(() => ({
				viewport: document.documentElement.clientWidth,
				rootWidth: document.documentElement.scrollWidth,
				bodyWidth: document.body.scrollWidth
			}));
			expect(metrics.rootWidth, `${route} at ${width}px: ${JSON.stringify(metrics)}`).toBeLessThanOrEqual(width);
			expect(metrics.bodyWidth, `${route} at ${width}px: ${JSON.stringify(metrics)}`).toBeLessThanOrEqual(width);
		}
	}
});

test('native file chooser fits the shared 36 and 44 pixel control sizes', async ({ page, browserName }) => {
	// WebKit styles ::file-selector-button but reports the input's own style for that pseudo-element.
	const reportsFileButtonStyle = browserName !== 'webkit';
	await openWorkbenchPage(page, `repositories/${REPOSITORY_ID}/add`);
	for (const viewport of [{ width: 1440, expected: 36 }, { width: 390, expected: 44 }]) {
		await page.setViewportSize({ width: viewport.width, height: 1000 });
		if (viewport.width === 1440) {
			await page.locator('#file').setInputFiles({
				name: 'shared-control-fixture.ttl',
				mimeType: 'text/turtle',
				buffer: Buffer.from('<urn:s> <urn:p> <urn:o> .')
			});
		}
		const controls = await page.evaluate(() => {
			const file = document.querySelector('#file');
			const format = document.querySelector('#Content-Type');
			const fileStyle = getComputedStyle(file, '::file-selector-button');
			return {
				fileHeight: Math.round(file.getBoundingClientRect().height),
				formatHeight: Math.round(format.getBoundingClientRect().height),
				fileButtonHeight: Math.round(parseFloat(fileStyle.height)),
				fileButtonRadius: fileStyle.borderRadius,
				fileButtonFontWeight: fileStyle.fontWeight,
				fileType: file.type,
				fileName: file.files[0]?.name,
				fileNameField: file.name,
				fileTabIndex: file.tabIndex
			};
		});
		expect(controls, `Add Source file control at ${viewport.width}px`).toMatchObject({
			fileHeight: viewport.expected,
			formatHeight: viewport.expected,
			...(reportsFileButtonStyle ? {
				fileButtonHeight: viewport.expected - 2,
				fileButtonRadius: '6px 0px 0px 6px',
				fileButtonFontWeight: '600'
			} : {}),
			fileType: 'file',
			fileName: 'shared-control-fixture.ttl',
			fileNameField: 'content',
			fileTabIndex: 0
		});
		expect(controls.fileHeight, `File chooser and Data format should align at ${viewport.width}px`)
			.toBe(controls.formatHeight);
	}
});

test('query and result utility disclosures share action typography and control geometry', async ({ page }) => {
	await page.setViewportSize({ width: 1440, height: 1000 });
	await openWorkbenchPage(page, `repositories/${REPOSITORY_ID}/query`);
	const queryToggles = await page.locator('#save-query-toggle,#query-options-toggle').evaluateAll(elements =>
		elements.map(element => {
			const style = getComputedStyle(element);
			return {
				fontFamily: style.fontFamily,
				fontSize: style.fontSize,
				fontWeight: style.fontWeight,
				height: Math.round(element.getBoundingClientRect().height),
				borderRadius: style.borderRadius,
				gap: style.gap
			};
		})
	);
	for (const [index, signature] of queryToggles.entries()) {
		expect(signature, `query utility disclosure ${index}`).toMatchObject({
			fontSize: '13px', fontWeight: '600', height: 36, borderRadius: '7px', gap: '8px'
		});
	}
	await runQuery(page, 'SELECT ?s WHERE { ?s ?p ?o } LIMIT 2');
	const resultToggles = await page.locator('#query-results [data-query-stream-root]')
		.locator('.query-result-download-toggle, .query-result-options-toggle')
		.evaluateAll(elements => elements.map(element => {
			const style = getComputedStyle(element);
			return {
				fontFamily: style.fontFamily,
				fontSize: style.fontSize,
				fontWeight: style.fontWeight,
				borderRadius: style.borderRadius,
				gap: style.gap
			};
		}));
	expect(resultToggles).toHaveLength(2);
	for (const [index, signature] of resultToggles.entries()) {
		expect(signature, `embedded result utility disclosure ${index}`).toMatchObject({
			fontSize: '13px', fontWeight: '600', borderRadius: '7px', gap: '8px'
		});
	}
	await openWorkbenchPage(page, 'repositories/NONE/create?type=memory-rdfs-dt');
	const sectionToggle = page.locator('#create-advanced-toggle');
	await expect(sectionToggle).toBeVisible();
	const sectionStyle = await sectionToggle.evaluate(element => ({
		fontSize: getComputedStyle(element).fontSize,
		fontWeight: getComputedStyle(element).fontWeight,
		containerRadius: getComputedStyle(document.getElementById('create-advanced-panel')).borderRadius
	}));
	expect(sectionStyle).toMatchObject({ fontSize: '13px', fontWeight: '600', containerRadius: '10px' });
});

test('saved query metadata exposes separators between all label and value pairs', async ({ page }) => {
	await page.setViewportSize({ width: 390, height: 1000 });
	const queryName = `metadata-rules-${Date.now()}`;
	await saveQuery(page, queryName);
	await openWorkbenchPage(page, `repositories/${REPOSITORY_ID}/saved-queries`);
	const row = page.locator('.saved-query-row').filter({ hasText: queryName });
	await row.locator('.saved-query-toggle').click();
	// The details are a key/value list (plan task M5.6): every row but the last has a separator.
	const rows = await row.locator('[id$="-metadata"] .workbench-kv__row').evaluateAll(items => items.map(item => ({
		borderBottomWidth: getComputedStyle(item).borderBottomWidth,
		borderBottomStyle: getComputedStyle(item).borderBottomStyle,
		borderBottomColor: getComputedStyle(item).borderBottomColor
	})));
	expect(rows).toHaveLength(2);
	expect(rows[0]).toMatchObject({
		borderBottomWidth: '1px',
		borderBottomStyle: 'solid',
		borderBottomColor: 'rgb(227, 236, 239)'
	});
	expect(rows[1].borderBottomWidth).toBe('0px');
});

test('query and update editors and explanations use one responsive code font token', async ({ page }) => {
	const expectedFamily = 'ui-monospace, SFMono-Regular, Menlo, Consolas, monospace';
	const routes = [
		// Code lines, not CodeMirror's measuring pre, which it replaces whenever it measures text.
		{ route: `repositories/${REPOSITORY_ID}/query`, selector: '.query-page .CodeMirror-code pre' },
		{ route: `repositories/${REPOSITORY_ID}/update`, selector: '#update-editor .CodeMirror-code pre' }
	];
	// A line that CodeMirror re-renders after it was found reads as an empty style; read a connected element.
	const readCodeFont = async locator => {
		let signature = null;
		await expect.poll(async () => {
			signature = await locator.evaluate(element => element.isConnected ? {
				fontFamily: getComputedStyle(element).fontFamily,
				fontSize: getComputedStyle(element).fontSize
			} : null);
			return signature !== null;
		}).toBe(true);
		return signature;
	};
	for (const viewport of [{ width: 1440, size: '14px' }, { width: 390, size: '13px' }]) {
		await page.setViewportSize({ width: viewport.width, height: 1000 });
		const signatures = [];
		for (const entry of routes) {
			await openWorkbenchPage(page, entry.route);
			const code = page.locator(entry.selector).first();
			await expect(code).toBeAttached();
			signatures.push(await readCodeFont(code));
		}
		await openWorkbenchPage(page, `repositories/${REPOSITORY_ID}/query`);
		const explanation = page.locator('#query-explanation');
		await expect(explanation).toBeAttached();
		signatures.push(await readCodeFont(explanation));
		for (const signature of signatures) {
			expect(signature).toEqual({ fontFamily: expectedFamily, fontSize: viewport.size });
		}
	}
});

test('saved query entries share one island and stay separated at desktop and mobile widths', async ({ page }) => {
	const firstName = `separation-first-${Date.now()}`;
	const secondName = `separation-second-${Date.now()}`;
	await saveQuery(page, firstName, LONG_LINE_QUERY);
	await saveQuery(page, secondName);
	for (const width of [390, 1440]) {
		await page.setViewportSize({ width, height: 1000 });
		await openWorkbenchPage(page, `repositories/${REPOSITORY_ID}/saved-queries`);
		const firstRow = page.locator('.saved-query-row').filter({ hasText: firstName });
		const secondRow = page.locator('.saved-query-row').filter({ hasText: secondName });
		await firstRow.locator('.saved-query-toggle').click();
		const geometry = await page.evaluate(({ firstId, secondId }) => {
			const first = document.getElementById(`${firstId}-div`);
			const second = document.getElementById(`${secondId}-div`);
			const list = document.getElementById('saved-queries');
			const firstStyle = getComputedStyle(first);
			const secondStyle = getComputedStyle(second);
			const listStyle = getComputedStyle(list);
			return {
				gap: second.getBoundingClientRect().top - first.getBoundingClientRect().bottom,
				separatorWidth: parseFloat(secondStyle.borderTopWidth),
				separatorColor: secondStyle.borderTopColor,
				rowBackground: firstStyle.backgroundColor,
				rowShadow: firstStyle.boxShadow,
				listBackground: listStyle.backgroundColor,
				listBorder: listStyle.borderTopWidth,
				listShadow: listStyle.boxShadow
			};
		}, { firstId: await firstRow.getAttribute('id').then(id => id.replace(/-div$/, '')), secondId: await secondRow.getAttribute('id').then(id => id.replace(/-div$/, '')) });
		expect(geometry.gap, `${width}px row gap`).toBeGreaterThanOrEqual(23);
		expect(geometry.separatorWidth, `${width}px separator`).toBe(1);
		expect(geometry.separatorColor).toBe('rgb(227, 236, 239)');
		expect(geometry.rowBackground).toBe('rgba(0, 0, 0, 0)');
		expect(geometry.rowShadow).toBe('none');
		expect(geometry.listBackground).toBe('rgb(255, 255, 255)');
		expect(geometry.listBorder).toBe('1px');
		expect(geometry.listShadow).not.toBe('none');
	}
});

test('mobile text, select, and multiline fields reach 44px across common forms', async ({ page }) => {
	const routes = [
		['namespaces', `repositories/${REPOSITORY_ID}/namespaces`],
		['server', 'repositories/NONE/server'],
		['create', 'repositories/NONE/create?type=memory-rdfs-dt'],
		['remove', `repositories/${REPOSITORY_ID}/remove`]
	];
	const fields = '#workbench-page-surface input[type="text"], #workbench-page-surface input[type="password"], #workbench-page-surface input[type="number"], #workbench-page-surface select, #workbench-page-surface textarea';
	for (const width of [320, 390]) {
		await page.setViewportSize({ width, height: 1000 });
		for (const [family, route] of routes) {
			await openWorkbenchPage(page, route);
			for (const disclosure of await page.locator('#workbench-page-surface details').all()) {
				if (!(await disclosure.evaluate(element => element.open))) {
					await disclosure.locator('summary').click();
				}
			}
			const measurements = await page.locator(fields).evaluateAll(elements => elements
				.filter(element => element.getClientRects().length > 0 && getComputedStyle(element).visibility !== 'hidden')
				.map(element => ({
					id: element.id || element.name || element.tagName,
					type: element.type || element.tagName.toLowerCase(),
					height: element.getBoundingClientRect().height,
					computedHeight: getComputedStyle(element).height,
					minHeight: getComputedStyle(element).minHeight,
					maxHeight: getComputedStyle(element).maxHeight,
					boxSizing: getComputedStyle(element).boxSizing
				})));
			expect(measurements.length, `${family} at ${width}px should expose fields`).toBeGreaterThan(0);
			for (const field of measurements) {
				expect(field.height, `${family} ${field.id} at ${width}px ${JSON.stringify(field)}`).toBeGreaterThanOrEqual(44);
			}
		}
	}
});

test('execute and explain actions reuse one icon metaphor across query forms', async ({ page }) => {
	const savedName = `icon-metaphors-${Date.now()}`;
	await saveQuery(page, savedName);
	const readShape = async locator => locator.evaluate(element => [...element.children].map(child => ({
		tag: child.tagName.toLowerCase(),
		d: child.getAttribute('d'),
		cx: child.getAttribute('cx'),
		cy: child.getAttribute('cy'),
		r: child.getAttribute('r')
	})));
	await openWorkbenchPage(page, `repositories/${REPOSITORY_ID}/query`);
	// The Query page's actions are buttons of the shared component (plan task M1.3) with the shared icons. "Explain
	// again" is a text action of the explanation toolbar (plan task M3.3; mockup 03 gives it a refresh glyph, not the
	// Explain icon), so instead of comparing the two, the Explain trigger must show the shared Explain icon.
	const queryExecute = await readShape(page.locator('#exec .workbench-action-icon'));
	await expect(page.locator('#explain-trigger .workbench-action-icon')).toHaveClass(/workbench-action-icon--explain/);
	const queryExplain = await readShape(page.locator('#explain-trigger .workbench-action-icon'));
	await openWorkbenchPage(page, `repositories/${REPOSITORY_ID}/update`);
	const updateExecute = await readShape(page.locator('#update-actions .workbench-action-icon'));
	await openWorkbenchPage(page, `repositories/${REPOSITORY_ID}/namespaces`);
	// Namespaces are saved from their row (plan task M6.3), with a check mark rather than the execute triangle.
	await page.getByRole('button', { name: 'Add namespace' }).click();
	const namespaceUpdate = page.locator('#namespaces-results .workbench-namespace-edit button[aria-label="Save"] .workbench-action-icon');
	await expect(namespaceUpdate).toHaveClass(/workbench-action-icon--check/);
	const namespaceUpdateShape = await readShape(namespaceUpdate);
	await openWorkbenchPage(page, `repositories/${REPOSITORY_ID}/saved-queries`);
	const savedRow = page.locator('.saved-query-row').filter({ hasText: savedName });
	const savedExecute = await readShape(savedRow.locator('.workbench-action--primary .workbench-action-icon'));
	expect(queryExecute).toEqual(updateExecute);
	expect(updateExecute).toEqual(savedExecute);
	expect(queryExplain).not.toEqual(queryExecute);
	expect(namespaceUpdateShape).not.toEqual(queryExecute);
});

test('query, update, and saved editor utilities stay outlined and clear of long first lines', async ({ page }) => {
	const savedName = `utilities-${Date.now()}`;
	await saveQuery(page, savedName);
	const inspectEditor = async locator => locator.evaluate(element => {
		const codeLine = element.querySelector('.CodeMirror-code .CodeMirror-line, .CodeMirror-code pre, .CodeMirror-code');
		const utilities = [...element.querySelectorAll('.yasqe_buttons .svgImg')]
			.filter(icon => getComputedStyle(icon).display !== 'none');
		const textNodes = [];
		const walker = document.createTreeWalker(codeLine, NodeFilter.SHOW_TEXT);
		while (walker.nextNode()) {
			textNodes.push(walker.currentNode);
		}
		const glyphRects = [];
		for (const node of textNodes) {
			for (let offset = 0; offset < node.textContent.length; offset++) {
				const range = document.createRange();
				range.setStart(node, offset);
				range.setEnd(node, offset + 1);
				const rect = range.getBoundingClientRect();
				if (rect.width > 0 && rect.height > 0) glyphRects.push(rect);
			}
		}
		const iconRects = utilities.map(icon => icon.getBoundingClientRect());
		const overlaps = glyphRects.some(glyph => iconRects.some(icon =>
			glyph.left < icon.right && glyph.right > icon.left && glyph.top < icon.bottom && glyph.bottom > icon.top));
		const color = getComputedStyle(utilities[0]).color;
		const graphicElements = utilities.flatMap(icon => [...icon.querySelectorAll('svg *')]);
		const graphics = graphicElements.filter(element => ['path', 'rect', 'circle', 'line', 'polyline', 'polygon', 'use', 'ellipse'].includes(element.tagName.toLowerCase()));
		return {
			hasUtilities: utilities.length > 0,
			overlaps,
			color,
			graphics: graphics.map(element => {
				const style = getComputedStyle(element);
				return { fill: style.fill, stroke: style.stroke };
			})
		};
	});
	const savedMetrics = await (async () => {
		await page.setViewportSize({ width: 390, height: 1000 });
		await openWorkbenchPage(page, `repositories/${REPOSITORY_ID}/saved-queries`);
		const savedRow = page.locator('.saved-query-row').filter({ hasText: savedName });
		await savedRow.locator('.saved-query-toggle').click();
		await savedRow.locator('.CodeMirror').first().evaluate((element, query) => element.CodeMirror.setValue(query), LONG_LINE_QUERY);
		return inspectEditor(savedRow.locator('.CodeMirror').first());
	})();
	const editorMetrics = [{ family: 'saved', width: 390, metrics: savedMetrics }];
	const colors = [];
	for (const width of [390, 1440]) {
		await page.setViewportSize({ width, height: 1000 });
		await openWorkbenchPage(page, `repositories/${REPOSITORY_ID}/query`);
		await page.locator('.query-page .CodeMirror').first().evaluate((element, query) => element.CodeMirror.setValue(query), LONG_LINE_QUERY);
		const queryMetrics = await inspectEditor(page.locator('.query-page .CodeMirror').first());
		await openWorkbenchPage(page, `repositories/${REPOSITORY_ID}/update`);
		await page.locator('#update-editor .CodeMirror').first().evaluate((element, query) => element.CodeMirror.setValue(query), LONG_LINE_QUERY);
		const updateMetrics = await inspectEditor(page.locator('#update-editor .CodeMirror').first());
		editorMetrics.push({ family: 'query', width, metrics: queryMetrics }, { family: 'update', width, metrics: updateMetrics });
	}
	for (const { family, width, metrics } of editorMetrics) {
		expect(metrics.hasUtilities, `${family} at ${width}px should retain editor utilities`).toBe(true);
		expect(metrics.overlaps, `${family} at ${width}px utility icons must not cover code`).toBe(false);
	}
	for (const { family, width, metrics } of editorMetrics) {
		expect(metrics.graphics.length, `${family} at ${width}px should expose SVG graphics`).toBeGreaterThan(0);
		for (const graphic of metrics.graphics) {
			expect(graphic.fill, `${family} at ${width}px should use outline glyphs`).toBe('none');
			expect(graphic.stroke, `${family} at ${width}px should use muted strokes`).toBe('rgb(79, 97, 104)');
		}
		colors.push(metrics.color);
	}
	expect(new Set(colors)).toEqual(new Set(['rgb(79, 97, 104)']));
});

test('editor utility glyphs remain muted outlines through fullscreen toggles', async ({ page }) => {
	await page.setViewportSize({ width: 390, height: 1000 });
	const savedName = `fullscreen-${Date.now()}`;
	await saveQuery(page, savedName);
	const measureVisibleUtilities = editor => editor.evaluate(element => {
		const icons = [...element.querySelectorAll('.yasqe_buttons .svgImg')]
			.filter(icon => getComputedStyle(icon).display !== 'none');
		return icons.map(icon => ({
			color: getComputedStyle(icon).color,
			graphics: [...icon.querySelectorAll('svg *')].map(graphic => ({
				fill: getComputedStyle(graphic).fill,
				stroke: getComputedStyle(graphic).stroke
			}))
		}));
	});
	for (const [family, route] of [
		['query', `repositories/${REPOSITORY_ID}/query`],
		['update', `repositories/${REPOSITORY_ID}/update`],
		['saved', `repositories/${REPOSITORY_ID}/saved-queries`]
	]) {
		await openWorkbenchPage(page, route);
		let editor;
		if (family === 'query') {
			editor = page.locator('.query-page .CodeMirror').first();
		} else if (family === 'update') {
			editor = page.locator('#update-editor .CodeMirror').first();
		} else {
			const row = page.locator('.saved-query-row').filter({ hasText: savedName });
			await row.locator('.saved-query-toggle').click();
			editor = row.locator('.CodeMirror').first();
		}
		const fullscreenIcon = editor.locator('.yasqe_buttons .svgImg.yasqe_fullscreenBtn');
		const windowedIcon = editor.locator('.yasqe_buttons .svgImg.yasqe_smallscreenBtn');
		await expect(fullscreenIcon, `${family} should expose enter-fullscreen control`).toBeVisible();
		await fullscreenIcon.click();
		await expect(editor, `${family} editor enters fullscreen state`).toHaveClass(/CodeMirror-fullscreen/);
		await expect(windowedIcon, `${family} should expose exit-fullscreen control`).toBeVisible();
		const fullscreenUtilities = await measureVisibleUtilities(editor);
		expect(fullscreenUtilities.length, `${family} fullscreen utilities`).toBeGreaterThan(0);
		for (const icon of fullscreenUtilities) {
			expect(icon.color).toBe('rgb(79, 97, 104)');
			for (const graphic of icon.graphics) {
				expect(graphic.fill).toBe('none');
				expect(graphic.stroke).toBe('rgb(79, 97, 104)');
			}
		}
		await windowedIcon.click();
		await expect(editor, `${family} editor exits fullscreen state`).not.toHaveClass(/CodeMirror-fullscreen/);
		await expect(fullscreenIcon).toBeVisible();
	}
});
