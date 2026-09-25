// @ts-check
const { test, expect } = require('@playwright/test');

const SERVER_BASE_URL = process.env.RDF4J_SERVER_BASE_URL || 'http://127.0.0.1:8080/rdf4j-server';
const WORKBENCH_BASE_URL = process.env.RDF4J_WORKBENCH_BASE_URL || 'http://127.0.0.1:8080/rdf4j-workbench';
const REPOSITORY_ID = `workbench-consistency-${Date.now()}`;
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
	const control = await locator.evaluateHandle(element => {
		const control = element.matches('button, input, select, textarea, a')
			? element
			: element.querySelector('button, input, select, textarea, a');
		if (control && control.disabled) {
			control.disabled = false;
		}
		return control;
	});
	await control.focus();
	await page.keyboard.press('Tab');
	await page.keyboard.press('Shift+Tab');
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
	await page.locator('.CodeMirror').first().evaluate(element => element.CodeMirror.setValue('SELECT * WHERE { ?s ?p ?o } LIMIT 2'));
	await page.locator('#exec').click();
	const frame = page.frameLocator('#query-results-frame');
	await frame.locator('#rdf4j-query-result').waitFor({ state: 'attached' });
	await frame.locator('#query-result-options-toggle').click();
	const embeddedField = frame.locator('#result-layout');
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
	const embeddedControl = frame.locator('#previousX');
	await embeddedControl.evaluate(element => { element.disabled = false; });
	await page.mouse.move(0, 0);
	await embeddedControl.focus();
	const embeddedAction = await embeddedControl.evaluate(element => {
		const wrapper = element.closest('.workbench-action');
		const style = getComputedStyle(wrapper);
		const bounds = wrapper.getBoundingClientRect();
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
			height: 36,
			borderRadius: '7px',
			borderColor: 'rgb(203, 213, 225)',
			focusIndicator: true
		});
	}
	expect(queryField).toEqual(addField);
	expect(addField).toEqual(createField);
	const fieldTokens = signature => ({
		fontFamily: signature.fontFamily,
		fontSize: signature.fontSize,
		fontWeight: signature.fontWeight,
		height: signature.height,
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
		expect(segment.radius).toBe('7px');
	}
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
	const labelWidth = await island.locator('table.simple th').first().evaluate(element => element.getBoundingClientRect().width);
	expect(labelWidth).toBeGreaterThanOrEqual(160);
	const desktopMetrics = await island.evaluate(element => ({
		directSurfaces: element.querySelectorAll(':scope > .workbench-island').length,
		sections: element.querySelectorAll('section').length,
		width: element.getBoundingClientRect().width
	}));
	expect(desktopMetrics.directSurfaces).toBe(0);
	expect(desktopMetrics.sections).toBe(3);
});

test('header Change links stay plain, compact, and slate colored', async ({ page }) => {
	await page.setViewportSize({ width: 1440, height: 1000 });
	await openWorkbenchPage(page, `repositories/${REPOSITORY_ID}/query`);
	const changeLinks = page.locator('#contentheader.workbench-context .change a');
	await expect(changeLinks).toHaveCount(3);
	const changeBrackets = await page.locator('#contentheader.workbench-context .change').evaluateAll(elements =>
		elements.map(element => ({
			before: getComputedStyle(element, '::before').content,
			after: getComputedStyle(element, '::after').content
		})));
	for (const brackets of changeBrackets) {
		expect(brackets.before).toBe('none');
		expect(brackets.after).toBe('none');
	}
	const measurements = await changeLinks.evaluateAll(elements => elements.map(element => {
		const style = getComputedStyle(element);
		const bounds = element.getBoundingClientRect();
		return {
			text: element.textContent.trim(),
			fontSize: style.fontSize,
			color: style.color,
			textTransform: style.textTransform,
			height: bounds.height
		};
	}));
	for (const link of measurements) {
		expect(link.text).toMatch(/^change$/i);
		expect(link.text).not.toMatch(/[\[\]]/);
		expect(link.fontSize).toBe('12px');
		expect(link.color).toBe('rgb(100, 116, 139)');
		expect(link.textTransform).toBe('none');
		expect(link.height).toBeLessThanOrEqual(24);
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

test('Saved Query actions use the two-column mobile grid and wrapping metadata', async ({ page }) => {
	await page.setViewportSize({ width: 390, height: 1000 });
	const queryName = `mobile-layout-${Date.now()}`;
	await saveQuery(page, queryName);
	await openWorkbenchPage(page, `repositories/${REPOSITORY_ID}/saved-queries`);
	const row = page.locator('.saved-query-row').filter({ hasText: queryName });
	await expect(row).toBeVisible();
	const actionBoxes = await row.locator('.workbench-action').evaluateAll(elements => elements.map(element => {
		const rect = element.getBoundingClientRect();
		return { x: rect.x, y: rect.y, width: rect.width, height: rect.height };
	}));
	expect(actionBoxes).toHaveLength(5);
	const [execute, link, show, edit, remove] = actionBoxes;
	for (const action of [execute, link, show, edit, remove]) {
		expect(action.height).toBeGreaterThanOrEqual(44);
	}
	expect(execute.y).toBeCloseTo(link.y, 0);
	expect(show.y).toBeCloseTo(edit.y, 0);
	expect(show.y).toBeGreaterThan(execute.y);
	expect(remove.y).toBeGreaterThan(show.y);
	expect(execute.x).toBeCloseTo(show.x, 0);
	expect(remove.x).toBeCloseTo(execute.x, 0);
	await expect(row.locator('.workbench-action').nth(0).locator('.workbench-action-icon')).toHaveClass(/workbench-action-icon--execute/);
	await expect(row.locator('.workbench-action').nth(0).locator('.workbench-action-icon path'))
		.toHaveAttribute('d', 'M8 5 19 12 8 19V5Z');
	await expect(row.locator('.workbench-action').nth(2).locator('.workbench-action-icon')).toHaveClass(/workbench-action-icon--eye/);
	await expect(row.locator('.workbench-action').nth(2).locator('.workbench-action-icon circle')).toHaveCount(1);
	await expect(row.locator('.workbench-action').nth(3).locator('.workbench-action-icon')).toHaveClass(/workbench-action-icon--edit/);
	await expect(row.locator('.workbench-action').nth(3).locator('.workbench-action-icon path').first())
		.toHaveAttribute('d', /10a2\.1 2\.1 0 0 0-3-3l-10 10/);
	await row.locator('.saved-query-toggle').click();
	const metadata = row.locator('table.data[id$="-metadata"]');
	await expect(metadata).toBeVisible();
	const metadataGeometry = await metadata.evaluate(table => {
		const firstRow = table.querySelector('tbody tr').getBoundingClientRect();
		const bounds = table.getBoundingClientRect();
		return { tableWidth: bounds.width, rowWidth: firstRow.width, rowRight: firstRow.right, tableRight: bounds.right };
	});
	expect(metadataGeometry.rowWidth).toBeGreaterThanOrEqual(metadataGeometry.tableWidth - 1);
	expect(metadataGeometry.rowRight).toBeCloseTo(metadataGeometry.tableRight, 0);
	const pageMetrics = await page.evaluate(() => ({
		viewport: document.documentElement.clientWidth,
		content: document.documentElement.scrollWidth,
		marginTop: parseFloat(getComputedStyle(document.querySelector('.saved-query-row table.data')).marginTop)
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
	await page.locator('.CodeMirror').first().evaluate(element => element.CodeMirror.setValue('SELECT * WHERE { ?s ?p ?o } LIMIT 2'));
	await page.locator('#exec').click();
	const embeddedResult = page.frameLocator('#query-results-frame').locator('#query-result-layout');
	await embeddedResult.waitFor({ state: 'attached' });
	const embeddedHeader = page.frameLocator('#query-results-frame')
		.locator('#query-result-table-wrap table.data th').first();
	await embeddedHeader.waitFor();
	signatures.push(await embeddedHeader.evaluate(element => {
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

test('native file chooser fits the shared 36 and 44 pixel control sizes', async ({ page }) => {
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
			fileButtonHeight: viewport.expected - 2,
			fileButtonRadius: '6px 0px 0px 6px',
			fileButtonFontWeight: '600',
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
	await page.locator('.CodeMirror').first().evaluate(element => element.CodeMirror.setValue('SELECT ?s WHERE { ?s ?p ?o } LIMIT 2'));
	await page.locator('#exec').click();
	const frame = page.frameLocator('#query-results-frame');
	await frame.locator('#rdf4j-query-result').waitFor({ state: 'attached' });
	const resultToggles = await frame.locator('#query-result-download-toggle,#query-result-options-toggle')
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
	for (const [index, signature] of resultToggles.entries()) {
		expect(signature, `embedded result utility disclosure ${index}`).toMatchObject({
			fontSize: '13px', fontWeight: '600', borderRadius: '7px', gap: '8px'
		});
	}
	await openWorkbenchPage(page, 'repositories/NONE/create?type=memory-rdfs-dt');
	const sectionSummary = page.locator('#workbench-page-surface details.workbench-advanced > summary').first();
	await expect(sectionSummary).toBeVisible();
	const sectionStyle = await sectionSummary.evaluate(element => ({
		fontSize: getComputedStyle(element).fontSize,
		fontWeight: getComputedStyle(element).fontWeight,
		containerRadius: getComputedStyle(element.parentElement).borderRadius
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
	const metadata = await row.locator('table.data[id$="-metadata"]').evaluate(table => {
		const cells = Array.from(table.querySelectorAll('tr:first-child > *'));
		return {
			rowCount: table.querySelectorAll('tr').length,
			cells: cells.map(cell => ({
				tag: cell.tagName,
				borderBottomWidth: getComputedStyle(cell).borderBottomWidth,
				borderBottomStyle: getComputedStyle(cell).borderBottomStyle,
				borderBottomColor: getComputedStyle(cell).borderBottomColor
			}))
		};
	});
	// This template emits four label/value pairs in one table row; the mobile grid lays
	// those cells into four visual rows, so separators belong to the first three pairs.
	expect(metadata.rowCount).toBe(1);
	expect(metadata.cells).toHaveLength(8);
	for (let pair = 0; pair < 3; pair++) {
		for (const cell of metadata.cells.slice(pair * 2, pair * 2 + 2)) {
			expect(cell).toMatchObject({
				borderBottomWidth: '1px',
				borderBottomStyle: 'solid',
				borderBottomColor: 'rgb(226, 232, 240)'
			});
		}
	}
	for (const cell of metadata.cells.slice(-2)) {
		expect(cell.borderBottomWidth).toBe('0px');
	}
});

test('query and update editors and explanations use one responsive code font token', async ({ page }) => {
	const expectedFamily = 'ui-monospace, SFMono-Regular, Menlo, Consolas, monospace';
	const routes = [
		{ route: `repositories/${REPOSITORY_ID}/query`, selector: '.query-page .CodeMirror pre' },
		{ route: `repositories/${REPOSITORY_ID}/update`, selector: '#update-editor .CodeMirror pre' }
	];
	for (const viewport of [{ width: 1440, size: '14px' }, { width: 390, size: '13px' }]) {
		await page.setViewportSize({ width: viewport.width, height: 1000 });
		const signatures = [];
		for (const entry of routes) {
			await openWorkbenchPage(page, entry.route);
			const code = page.locator(entry.selector).first();
			await expect(code).toBeAttached();
			signatures.push(await code.evaluate(element => ({
				fontFamily: getComputedStyle(element).fontFamily,
				fontSize: getComputedStyle(element).fontSize
			})));
		}
		await openWorkbenchPage(page, `repositories/${REPOSITORY_ID}/query`);
		const explanation = page.locator('#query-explanation');
		await expect(explanation).toBeAttached();
		signatures.push(await explanation.evaluate(element => ({
			fontFamily: getComputedStyle(element).fontFamily,
			fontSize: getComputedStyle(element).fontSize
		})));
		for (const signature of signatures) {
			expect(signature).toEqual({ fontFamily: expectedFamily, fontSize: viewport.size });
		}
	}
});

test('short embedded results do not expose the Workbench canvas below their content', async ({ page }) => {
	await page.setViewportSize({ width: 1440, height: 1000 });
	await openWorkbenchPage(page, `repositories/${REPOSITORY_ID}/query`);
	await page.locator('.CodeMirror').first().evaluate(element => element.CodeMirror.setValue('SELECT ?s WHERE { VALUES ?s { <urn:a> <urn:b> <urn:c> } }'));
	await page.locator('#exec').click();
	const frame = page.frameLocator('#query-results-frame');
	await frame.locator('#rdf4j-query-result').waitFor({ state: 'attached' });
	const metrics = await page.locator('#query-results-frame').evaluate(iframe => {
		const document = iframe.contentDocument;
		const body = document.body;
		const bodyBounds = body.getBoundingClientRect();
		return {
			viewportHeight: document.documentElement.clientHeight,
			documentHeight: document.documentElement.scrollHeight,
			bodyBottom: bodyBounds.bottom,
			bodyBackground: getComputedStyle(body).backgroundColor,
			rootBackground: getComputedStyle(document.documentElement).backgroundColor,
			navigationBottom: document.querySelector('.query-result-navigation').getBoundingClientRect().bottom
		};
	});
	expect(metrics.bodyBackground).toBe('rgb(255, 255, 255)');
	expect(metrics.rootBackground).toBe('rgb(255, 255, 255)');
	expect(metrics.bodyBottom).toBeGreaterThanOrEqual(metrics.viewportHeight - 1);
	expect(metrics.documentHeight).toBeLessThanOrEqual(metrics.viewportHeight + 1);
	expect(metrics.navigationBottom).toBeLessThanOrEqual(metrics.bodyBottom);
});

test('embedded tuple result heading remains the paging script target', async ({ page }) => {
	const tupleErrors = [];
	page.on('pageerror', error => {
		if (error.stack?.includes('/scripts/tuple.js')) {
			tupleErrors.push(error.message);
		}
	});
	await openWorkbenchPage(page, `repositories/${REPOSITORY_ID}/query`);
	await page.locator('.CodeMirror').first().evaluate(element =>
		element.CodeMirror.setValue('SELECT ?s WHERE { VALUES ?s { "paging-title" } }'));
	await page.locator('#exec').click();
	const resultFrame = page.frameLocator('#query-results-frame');
	const resultHeading = resultFrame.locator('#query-result-embedded-header h2');
	await resultHeading.waitFor({ state: 'visible' });
	await expect(resultHeading).toHaveAttribute('id', 'title_heading');
	await expect(resultHeading).toContainText('Query Result');
	expect(tupleErrors).toEqual([]);
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
		expect(geometry.separatorColor).toBe('rgb(226, 232, 240)');
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
	const queryExecute = await readShape(page.locator('#exec .query-action-icon'));
	const queryExplain = await readShape(page.locator('#explain-trigger .query-action-icon'));
	const rerunExplain = await readShape(page.locator('#rerun-explanation').locator('xpath=../..').locator('.workbench-action-icon'));
	await openWorkbenchPage(page, `repositories/${REPOSITORY_ID}/update`);
	const updateExecute = await readShape(page.locator('#update-actions .workbench-action-icon'));
	await openWorkbenchPage(page, `repositories/${REPOSITORY_ID}/namespaces`);
	const namespaceUpdate = page.locator('#namespaces-form .workbench-action--primary .workbench-action-icon');
	await expect(namespaceUpdate).toHaveClass(/workbench-action-icon--update/);
	const namespaceUpdateShape = await readShape(namespaceUpdate);
	await openWorkbenchPage(page, `repositories/${REPOSITORY_ID}/saved-queries`);
	const savedRow = page.locator('.saved-query-row').filter({ hasText: savedName });
	const savedExecute = await readShape(savedRow.locator('.workbench-action--primary .workbench-action-icon'));
	expect(queryExecute).toEqual(updateExecute);
	expect(updateExecute).toEqual(savedExecute);
	expect(queryExplain).toEqual(rerunExplain);
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
			expect(graphic.stroke, `${family} at ${width}px should use slate strokes`).toBe('rgb(100, 116, 139)');
		}
		colors.push(metrics.color);
	}
	expect(new Set(colors)).toEqual(new Set(['rgb(100, 116, 139)']));
});

test('editor utility glyphs remain slate outlines through fullscreen toggles', async ({ page }) => {
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
			expect(icon.color).toBe('rgb(100, 116, 139)');
			for (const graphic of icon.graphics) {
				expect(graphic.fill).toBe('none');
				expect(graphic.stroke).toBe('rgb(100, 116, 139)');
			}
		}
		await windowedIcon.click();
		await expect(editor, `${family} editor exits fullscreen state`).not.toHaveClass(/CodeMirror-fullscreen/);
		await expect(fullscreenIcon).toBeVisible();
	}
});
