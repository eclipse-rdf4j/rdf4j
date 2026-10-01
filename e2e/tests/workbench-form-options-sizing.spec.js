// @ts-check
const { test, expect } = require('@playwright/test');
const fs = require('node:fs');

const SERVER_BASE_URL = process.env.RDF4J_SERVER_BASE_URL || 'http://127.0.0.1:8080/rdf4j-server';
const WORKBENCH_BASE_URL = process.env.RDF4J_WORKBENCH_BASE_URL || 'http://127.0.0.1:8080/rdf4j-workbench';
const REPOSITORY_ID = `workbench-form-sizing-${process.pid}-${Date.now()}`;
const REPOSITORY_BASE_URL = `${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}`;
const SAVED_QUERY_NAME = `size-${process.pid}-${Date.now()}`.slice(0, 32);
const SCREENSHOT_DIR = `/private/tmp/workbench-config-coverage-20260925`;
let savedQueryUrn;

const CREATE_TYPES = [
	'memory', 'memory-lucene', 'memory-rdfs', 'memory-rdfs-dt', 'memory-rdfs-lucene',
	'memory-customrule', 'memory-shacl', 'native', 'native-lucene', 'native-rdfs',
	'native-rdfs-dt', 'native-rdfs-lucene', 'native-customrule', 'native-shacl', 'remote',
	'sparql', 'federate', 'lmdb'
];

test.beforeAll(async ({ request }) => {
	const response = await request.put(`${SERVER_BASE_URL}/repositories/${REPOSITORY_ID}`, {
		headers: { 'Content-Type': 'text/turtle' },
		data: repositoryConfig(REPOSITORY_ID)
	});
	expect([200, 201, 204]).toContain(response.status());
	const statements = await request.post(`${SERVER_BASE_URL}/repositories/${REPOSITORY_ID}/statements`, {
		headers: { 'Content-Type': 'application/n-triples' },
		data: '<http://example.org/subject> <http://example.org/predicate> "A compact fixture value" .'
	});
	expect([200, 201, 204]).toContain(statements.status());
});

test.afterAll(async ({ request }) => {
	if (savedQueryUrn) {
		const response = await request.post(`${REPOSITORY_BASE_URL}/saved-queries`, {
			form: { delete: savedQueryUrn }
		});
		expect([200, 204]).toContain(response.status());
	}
	await request.delete(`${SERVER_BASE_URL}/repositories/${REPOSITORY_ID}`);
});

function repositoryConfig(repositoryId) {
	return `@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix config: <tag:rdf4j.org,2023:config/>.

[] a config:Repository ;
   config:rep.id "${repositoryId}" ;
   rdfs:label "Workbench form sizing fixture" ;
   config:rep.impl [
      config:rep.type "openrdf:SailRepository" ;
      config:sail.impl [ config:sail.type "openrdf:MemoryStore" ]
   ].`;
}

async function openWorkbenchPage(page, route) {
	await page.goto(`${WORKBENCH_BASE_URL}/${route}`, { waitUntil: 'load' });
	await page.locator('#workbench-page-surface').waitFor({ state: 'attached' });
	if (/\/query(?:$|[?#])/.test(route)) {
		// The query shell is visible before its page model has been loaded and
		// the CodeMirror form, disclosure listeners, and result controller mount.
		await page.locator('.CodeMirror').first().waitFor({ state: 'visible' });
	}
}

async function openWorkbenchPageInNewPage(context, route, viewport = { width: 1440, height: 1000 }) {
	const routePage = await context.newPage();
	await routePage.setViewportSize(viewport);
	await openWorkbenchPage(routePage, route);
	return routePage;
}

function queryResultRoot(page) {
	return page.locator('#query-results [data-query-stream-root]');
}

function queryResultPanels(result) {
	return result.locator('.query-result-disclosure-panels > .workbench-disclosure__panel');
}

async function waitForSettledDisclosure(panel) {
	await expect.poll(() => panel.evaluate(element => element.getAnimations({ subtree: false })
		.filter(animation => animation.playState === 'running').length)).toBe(0);
}

async function captureGeometryScreenshot(page, name, width) {
	fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });
	const engine = test.info().project.name;
	await page.screenshot({
		path: `${SCREENSHOT_DIR}/${name}-${width}-${engine}.png`,
		fullPage: true,
		animations: 'disabled'
	});
}

async function measureContainedRegion(region) {
	return region.evaluate(root => {
		const rootBounds = root.getBoundingClientRect();
		const rootStyle = getComputedStyle(root);
		const contentBounds = {
			left: rootBounds.left + parseFloat(rootStyle.borderLeftWidth) + parseFloat(rootStyle.paddingLeft),
			right: rootBounds.right - parseFloat(rootStyle.borderRightWidth) - parseFloat(rootStyle.paddingRight),
			top: rootBounds.top + parseFloat(rootStyle.borderTopWidth) + parseFloat(rootStyle.paddingTop),
			bottom: rootBounds.bottom - parseFloat(rootStyle.borderBottomWidth) - parseFloat(rootStyle.paddingBottom)
		};
		const visible = element => {
			const bounds = element.getBoundingClientRect();
			return getComputedStyle(element).display !== 'none' && bounds.width > 0 && bounds.height > 0;
		};
		const controls = Array.from(root.querySelectorAll('input:not([type="hidden"]), select, textarea, button'))
			.filter(visible)
			.map(control => {
				const bounds = control.getBoundingClientRect();
				return {
					id: control.id || control.name || control.tagName.toLowerCase(),
					left: Math.round(bounds.left),
					right: Math.round(bounds.right),
					top: Math.round(bounds.top),
					bottom: Math.round(bounds.bottom),
					insideRoot: bounds.left >= rootBounds.left - 1 && bounds.right <= rootBounds.right + 1,
					insideContent: bounds.left >= contentBounds.left - 1 && bounds.right <= contentBounds.right + 1
				};
			});
		const visibleLabels = Array.from(root.querySelectorAll('label, th'))
			.filter(label => label.textContent.trim() && visible(label))
			.map(label => {
				const bounds = label.getBoundingClientRect();
			return {
				text: label.innerText.trim(),
				left: Math.round(bounds.left),
				right: Math.round(bounds.right),
				insideRoot: bounds.left >= rootBounds.left - 1 && bounds.right <= rootBounds.right + 1,
				insideContent: bounds.left >= contentBounds.left - 1 && bounds.right <= contentBounds.right + 1
			};
			});
		const labelControlOverlaps = Array.from(root.querySelectorAll('tr'))
			.flatMap(row => {
				const label = row.querySelector('th');
				const control = row.querySelector('input:not([type="hidden"]):not([type="submit"]):not([type="button"]), select, textarea');
				if (!label || !control || !visible(control)) {
					return [];
				}
				const labelBounds = label.getBoundingClientRect();
				const controlBounds = control.getBoundingClientRect();
				return labelBounds.left < controlBounds.right - 1 && labelBounds.right > controlBounds.left + 1 &&
					labelBounds.top < controlBounds.bottom - 1 && labelBounds.bottom > controlBounds.top + 1
					? [row.innerText.trim()] : [];
			});
		const overflowingChildren = Array.from(root.querySelectorAll(
			'form, article, details, .workbench-options__body, pre'
		)).filter(element => element.scrollWidth > element.clientWidth + 1)
			.map(element => ({
				tag: element.tagName.toLowerCase(),
				id: element.id || element.className.baseVal || element.className,
				overflow: element.scrollWidth - element.clientWidth
			}));
		return {
			width: Math.round(rootBounds.width),
			height: Math.round(rootBounds.height),
			rootOverflow: root.scrollWidth - root.clientWidth,
			controlsOutsideRoot: controls.filter(control => !control.insideRoot),
			controlsOutsideContent: controls.filter(control => !control.insideContent),
			labelsOutsideContent: visibleLabels.filter(label => !label.insideContent),
			labelsOutsideRoot: visibleLabels.filter(label => !label.insideRoot),
			labelControlOverlaps,
			overflowingChildren
		};
	});
}

async function measureDocumentBounds(page) {
	return page.evaluate(() => ({
		viewport: document.documentElement.clientWidth,
		pageOverflow: document.documentElement.scrollWidth - document.documentElement.clientWidth
	}));
}

test.describe('Workbench configuration and option sizing', () => {
	test('sizes short repository fields to their content across every backend template', async ({ page }) => {
		test.setTimeout(240000);
		const sizes = [];
		const violations = [];
		for (const width of [1440, 768, 390, 320]) {
			await page.setViewportSize({ width, height: 1000 });
			for (const type of CREATE_TYPES) {
				await openWorkbenchPage(page, `repositories/NONE/create?type=${type}`);
				const form = page.locator('form[action="create"]');
				await expect(form).toBeVisible();
				const advanced = form.locator('.workbench-advanced[data-workbench-detail-disclosure="true"]');
				const advancedPresent = await advanced.count() === 1;
				if (advancedPresent) {
					await advanced.locator(':scope > .workbench-disclosure__toggle').press('Enter');
				}
				const fields = await form.evaluate(element => {
					const rect = selector => {
						const control = element.querySelector(selector);
						if (!control) {
							return null;
						}
						const bounds = control.getBoundingClientRect();
						return { width: Math.round(bounds.width), height: Math.round(bounds.height) };
					};
					const advanced = element.querySelector('.workbench-advanced[data-workbench-detail-disclosure="true"]');
					const panel = advanced && advanced.querySelector(':scope > .workbench-disclosure__panel');
					const advancedFields = panel && panel.querySelector('.workbench-advanced-fields');
					const visibleFieldRows = Array.from(element.querySelectorAll('.workbench-advanced__field'))
						.filter(field => field.querySelector('.workbench-advanced__control input:not([type="hidden"]):not([type="submit"]):not([type="button"]), .workbench-advanced__control select, .workbench-advanced__control textarea'));
					const rowOverlaps = visibleFieldRows.flatMap(field => {
						const label = field.querySelector(':scope > label, :scope > span');
						const control = field.querySelector('.workbench-advanced__control').getBoundingClientRect();
						if (!label) return [];
						const labelBounds = label.getBoundingClientRect();
						return labelBounds.left < control.right - 1 && labelBounds.right > control.left + 1 &&
							labelBounds.top < control.bottom - 1 && labelBounds.bottom > control.top + 1 ? [field.innerText.trim()] : [];
					});
					const controlsOutsideForm = Array.from(element.querySelectorAll('input:not([type="hidden"]), select, textarea, button'))
						.filter(control => getComputedStyle(control).display !== 'none' && control.getBoundingClientRect().width > 0)
						.filter(control => control.getBoundingClientRect().left < element.getBoundingClientRect().left - 1 ||
							control.getBoundingClientRect().right > element.getBoundingClientRect().right + 1)
						.map(control => control.id || control.name || control.tagName.toLowerCase());
					const box = target => {
						if (!target) {
							return null;
						}
						const bounds = target.getBoundingClientRect();
						return {
							left: Math.round(bounds.left), right: Math.round(bounds.right), width: Math.round(bounds.width),
							clientWidth: target.clientWidth, scrollWidth: target.scrollWidth,
							overflow: target.scrollWidth - target.clientWidth
						};
					};
					const advancedSummary = advanced && advanced.querySelector(':scope > .workbench-disclosure__toggle');
					const firstAdvancedLabel = advancedFields && advancedFields.querySelector('.workbench-advanced__field > label, .workbench-advanced__field > span');
					const detailsBounds = panel && panel.getBoundingClientRect();
					const advancedSummaryBottom = advancedSummary && advancedSummary.getBoundingClientRect().bottom;
					const firstAdvancedLabelTop = firstAdvancedLabel && firstAdvancedLabel.getBoundingClientRect().top;
					const fieldGap = parseFloat(getComputedStyle(element).getPropertyValue('--workbench-field-gap')) || 0;
					const advancedContentOutsidePanel = advanced && Array.from(advanced.querySelectorAll(
						'label, input:not([type="hidden"]), select, textarea'
					)).filter(control => getComputedStyle(control).display !== 'none' && control.getBoundingClientRect().width > 0)
						.filter(control => control.getBoundingClientRect().left < detailsBounds.left - 1 ||
							control.getBoundingClientRect().right > detailsBounds.right + 1)
						.map(control => control.id || control.innerText && control.innerText.trim() || control.name || control.tagName.toLowerCase());
					const labelsOutsideForm = Array.from(element.querySelectorAll('th, label'))
						.filter(label => label.textContent.trim() && getComputedStyle(label).display !== 'none' &&
							label.getBoundingClientRect().width > 0)
						.filter(label => label.getBoundingClientRect().left < element.getBoundingClientRect().left - 1 ||
							label.getBoundingClientRect().right > element.getBoundingClientRect().right + 1)
						.map(label => label.innerText.trim());
					const viewportWidth = element.ownerDocument.documentElement.clientWidth;
					const descendantsOutsideViewport = Array.from(element.querySelectorAll('*'))
						.filter(control => getComputedStyle(control).display !== 'none' && control.getBoundingClientRect().width > 0)
						.filter(control => control.getBoundingClientRect().right > viewportWidth + 1)
						.map(control => {
							const bounds = control.getBoundingClientRect();
							return {
								tag: control.tagName.toLowerCase(), id: control.id, name: control.getAttribute('name'),
								className: typeof control.className === 'string' ? control.className : '',
								right: Math.round(bounds.right), width: Math.round(bounds.width),
								text: (control.innerText || control.value || '').trim().slice(0, 80)
							};
						}).slice(0, 20);
					return {
						id: rect('input[name="Repository ID"], input[name="Local repository ID"]'),
						title: rect('input[name="Repository title"]'),
						delay: rect('input[name="Sync delay"]'),
						url: rect('input[name="URL"]') || rect('input[type="url"]'),
						advancedPresent: Boolean(advanced),
						advancedOpen: advancedSummary ? advancedSummary.getAttribute('aria-expanded') === 'true' : null,
						formOverflow: element.scrollWidth - element.clientWidth,
						advancedOverflow: advancedFields && advancedFields.scrollWidth - advancedFields.clientWidth,
						boxes: {
							form: box(element),
							mainTable: box(element.querySelector(':scope > table.dataentry')),
							advanced: box(panel),
							advancedTable: box(advancedFields),
							advancedBody: box(advancedFields && advancedFields.parentElement),
							advancedSummaryBottom: advancedSummaryBottom && Math.round(advancedSummaryBottom),
							firstAdvancedLabelTop: firstAdvancedLabelTop && Math.round(firstAdvancedLabelTop),
							advancedFirstFieldGap: advancedSummaryBottom !== null && firstAdvancedLabelTop !== null
								? Math.round(firstAdvancedLabelTop - advancedSummaryBottom) : null,
							fieldGap
						},
						advancedContentOutsidePanel,
						pageOverflow: element.ownerDocument.documentElement.scrollWidth - element.ownerDocument.documentElement.clientWidth,
						advancedTextareas: Array.from(advanced ? advanced.querySelectorAll('textarea') : []).map(textarea => ({
							id: textarea.id,
							...box(textarea),
							control: box(textarea.closest('.workbench-advanced__control')),
							field: box(textarea.closest('.workbench-advanced__field'))
						})),
						rowOverlaps,
						controlsOutsideForm,
						labelsOutsideForm,
						descendantsOutsideViewport
					};
				});
				expect(fields.id, `${type} should expose its repository ID at ${width}px`).not.toBeNull();
				expect(fields.title, `${type} should expose its repository title at ${width}px`).not.toBeNull();
				const record = { type, width, ...fields };
				sizes.push(record);
				console.log(`FORM_SIZING ${JSON.stringify(record)}`);
				if (fields.id.width >= 300 || (width > 900 && fields.id.width >= fields.title.width)) {
					violations.push(`${type} at ${width}px: ID ${fields.id.width}px, title ${fields.title.width}px`);
				}
				if (fields.delay && fields.delay.width >= 240) {
					violations.push(`${type} at ${width}px: numeric delay ${fields.delay.width}px`);
				}
				if ((fields.advancedPresent && !fields.advancedOpen) ||
					(fields.advancedPresent && fields.advancedFirstFieldGap < fields.fieldGap - 1) ||
					(fields.advancedContentOutsidePanel || []).length ||
					fields.rowOverlaps.length || fields.controlsOutsideForm.length || fields.labelsOutsideForm.length || fields.pageOverflow > 1) {
					violations.push(`${type} at ${width}px has advanced-field overflow, label overlap, or clipped controls: ${JSON.stringify(fields)}`);
				}
				if (type === 'memory-rdfs-dt' || ((type === 'memory-customrule' || type === 'native-customrule') && width === 320)) {
					await captureGeometryScreenshot(page, `create-advanced-${type}`, width);
				}
			}
		}
		expect(sizes).toHaveLength(CREATE_TYPES.length * 4);
		expect(violations, `Oversized short fields: ${violations.join('; ')}`).toEqual([]);
	});

	test('sizes connection, address, and numeric option controls for their roles', async ({ page }) => {
		test.setTimeout(90000);
		const context = page.context();
		const geometry = {};
		const violations = [];
		let routePage = await openWorkbenchPageInNewPage(context, 'repositories/NONE/server');
		const serverAuth = routePage.locator('#server-auth');
		if (!(await serverAuth.locator(':scope > .workbench-disclosure__toggle').getAttribute('aria-expanded') === 'true')) {
			await serverAuth.locator(':scope > .workbench-disclosure__toggle').click();
		}
		geometry.server = await routePage.locator('#server-user').evaluate(element => Math.round(element.getBoundingClientRect().width));
		geometry.password = await routePage.locator('#server-password').evaluate(element => Math.round(element.getBoundingClientRect().width));
		await routePage.close();
		routePage = await openWorkbenchPageInNewPage(context, `repositories/${REPOSITORY_ID}/add`);
		await routePage.locator('label[for="source-url"]').click();
		geometry.url = await routePage.locator('#url').evaluate(element => Math.round(element.getBoundingClientRect().width));
		const importSettings = routePage.locator('#add-import-settings');
		if (!(await importSettings.locator(':scope > .workbench-disclosure__toggle').getAttribute('aria-expanded') === 'true')) {
			await importSettings.locator(':scope > .workbench-disclosure__toggle').click();
		}
		geometry.baseUri = await routePage.locator('#baseURI').evaluate(element => Math.round(element.getBoundingClientRect().width));
		geometry.context = await routePage.locator('#context').evaluate(element => Math.round(element.getBoundingClientRect().width));
		await routePage.close();
		routePage = await openWorkbenchPageInNewPage(context, `repositories/${REPOSITORY_ID}/export`);
		// The preview limit sits in the preview card since the Export redesign, outside any disclosure.
		geometry.exportLimit = await routePage.locator('#limit_export').evaluate(element => Math.round(element.getBoundingClientRect().width));
		await routePage.close();
		routePage = await openWorkbenchPageInNewPage(context, `repositories/${REPOSITORY_ID}/query`);
		await routePage.locator('#query-options-toggle').click();
		geometry.timeout = await routePage.locator('#query-timeout').evaluate(element => Math.round(element.getBoundingClientRect().width));
		await routePage.close();
		console.log(`SEMANTIC_SIZING ${JSON.stringify(geometry)}`);
		if (geometry.server >= 320) violations.push(`server credential ${geometry.server}px`);
		if (geometry.password >= 320) violations.push(`password ${geometry.password}px`);
		if (geometry.url < 320 || geometry.url > 560) violations.push(`URL ${geometry.url}px`);
		if (geometry.baseUri < 320 || geometry.baseUri > 600) violations.push(`base URI ${geometry.baseUri}px`);
		if (geometry.context < 320 || geometry.context > 600) violations.push(`context ${geometry.context}px`);
		// Namespaces are edited in their table row since plan task M6.3, so the old form widths no longer apply.
		if (geometry.exportLimit >= 200) violations.push(`export result limit ${geometry.exportLimit}px`);
		if (geometry.timeout >= 120) violations.push(`query timeout ${geometry.timeout}px`);
		expect(violations, `Mis-sized semantic controls: ${violations.join('; ')}`).toEqual([]);
	});

	test('keeps controls inside form bounds at desktop, tablet, and narrow widths', async ({ page }) => {
		test.setTimeout(120000);
		const routes = [
			['Server', 'repositories/NONE/server'],
			['Add', `repositories/${REPOSITORY_ID}/add`],
			['Export', `repositories/${REPOSITORY_ID}/export`],
			['Update', `repositories/${REPOSITORY_ID}/update`],
			['Explore', `repositories/${REPOSITORY_ID}/explore`],
			['Namespaces', `repositories/${REPOSITORY_ID}/namespaces`],
			['Query', `repositories/${REPOSITORY_ID}/query`]
		];
		const context = page.context();
		for (const width of [1440, 768, 390, 320]) {
			for (const [name, route] of routes) {
				const routePage = await openWorkbenchPageInNewPage(context, route, { width, height: 1000 });
				if (name === 'Server') {
					await routePage.locator('#server-auth-toggle').click();
				}
				if (name === 'Add') {
					await routePage.locator('label[for="source-text"]').click();
					await routePage.locator('#add-import-settings-toggle').click();
				}
				if (name === 'Export') {
					await routePage.locator('#export-advanced-toggle').click();
				}
				if (name === 'Explore') {
					await routePage.locator('#explore-result-options-toggle').click();
				}
				if (name === 'Query') {
					await routePage.locator('#query-options-toggle').click();
				}
				const geometry = await routePage.evaluate(() => {
					const surface = document.querySelector('#workbench-page-surface');
					const visibleControls = Array.from(surface.querySelectorAll('input, select, textarea, button'))
						.filter(control => !control.hidden && getComputedStyle(control).display !== 'none' &&
							control.getBoundingClientRect().width > 0 && control.getBoundingClientRect().height > 0)
						.map(control => ({
							name: control.name || control.id || control.tagName.toLowerCase(),
							right: Math.round(control.getBoundingClientRect().right),
							width: Math.round(control.getBoundingClientRect().width)
						}));
					return {
						viewport: document.documentElement.clientWidth,
						pageOverflow: document.documentElement.scrollWidth - document.documentElement.clientWidth,
						controlsOutsideViewport: visibleControls.filter(control => control.right > document.documentElement.clientWidth + 1),
						controls: visibleControls,
						editor: surface.querySelector('#update-editor') && (() => {
							const bounds = surface.querySelector('#update-editor').getBoundingClientRect();
							return { right: Math.round(bounds.right), width: Math.round(bounds.width) };
						})()
					};
				});
				console.log(`FORM_BOUNDS ${JSON.stringify({ name, width, ...geometry })}`);
				expect(geometry.pageOverflow, `${name} overflows at ${width}px`).toBeLessThanOrEqual(1);
				expect(geometry.controlsOutsideViewport, `${name} has clipped controls at ${width}px`).toEqual([]);
				if (name === 'Update') {
					expect(geometry.editor.width, `Update editor remains roomy at ${width}px`).toBeGreaterThan(150);
					expect(geometry.editor.right, `Update editor remains visible at ${width}px`).toBeLessThanOrEqual(width + 1);
					if (width >= 768) {
						expect(geometry.editor.width, 'desktop Update editor retains its editing width').toBeGreaterThan(500);
					}
				}
				await routePage.close();
			}
		}
	});

	test('preserves query-option values while their disclosure opens and labels focus native controls', async ({ page }) => {
		await page.setViewportSize({ width: 1440, height: 1000 });
		await openWorkbenchPage(page, `repositories/${REPOSITORY_ID}/query`);
		await page.locator('#query-options-toggle').click();
		const timeout = page.locator('#query-timeout');
		await timeout.fill('7');
		const before = await page.locator('#query-options-panel').evaluate(element => Array.from(element.querySelectorAll('input, select'))
			.map(control => ({ id: control.id, value: control.value, checked: control.checked, disabled: control.disabled })));
		await page.locator('#query-options-toggle').click();
		await page.locator('#query-options-toggle').click();
		const after = await page.locator('#query-options-panel').evaluate(element => Array.from(element.querySelectorAll('input, select'))
			.map(control => ({ id: control.id, value: control.value, checked: control.checked, disabled: control.disabled })));
		expect(after).toEqual(before);
		const inferredLabel = page.locator('label[for="infer"]');
		const infer = page.locator('#infer');
		const initialInferred = await infer.isChecked();
		await inferredLabel.click();
		await expect(infer).toHaveJSProperty('checked', !initialInferred);
		await infer.focus();
		await expect(infer).toBeFocused();
		await infer.press('Space');
		await expect(infer).toHaveJSProperty('checked', initialInferred);
	});

		test('sizes query save and option disclosures to their field content', async ({ page }) => {
		for (const width of [1440, 768, 320]) {
			const routePage = await openWorkbenchPageInNewPage(page.context(), `repositories/${REPOSITORY_ID}/query`, {
				width,
				height: 1000
			});
			await routePage.locator('#save-query-toggle').click();
			await expect(routePage.locator('#save-query-panel')).toBeVisible();
			const saveGeometry = await routePage.evaluate(() => {
				const width = selector => Math.round(document.querySelector(selector).getBoundingClientRect().width);
				return {
					viewport: document.documentElement.clientWidth,
					pageOverflow: document.documentElement.scrollWidth - document.documentElement.clientWidth,
					panel: width('#save-query-panel'),
					queryName: width('#query-name')
				};
			});
			await routePage.locator('#query-options-toggle').click();
			await expect(routePage.locator('#query-options-panel')).toBeVisible();
			const geometry = await routePage.evaluate(() => {
				const width = selector => Math.round(document.querySelector(selector).getBoundingClientRect().width);
				return {
					viewport: document.documentElement.clientWidth,
					pageOverflow: document.documentElement.scrollWidth - document.documentElement.clientWidth,
					optionsPanel: width('#query-options-panel'),
					settings: width('#query-options-panel .workbench-disclosure__fields')
				};
			});
			console.log(`QUERY_DISCLOSURE_SIZING ${JSON.stringify({ width, save: saveGeometry, options: geometry })}`);
			const desktop = width > 900;
			expect(saveGeometry.panel, 'save controls should follow their field widths').toBeLessThan(desktop ? 840 : width);
			expect(saveGeometry.queryName, 'query name should honor its 32-character size').toBeLessThan(desktop ? 420 : width);
			expect(geometry.optionsPanel, 'query options should not fill the editor width').toBeLessThan(desktop ? 920 : width);
			expect(geometry.settings, 'query settings row should fit its controls').toBeLessThan(desktop ? 900 : width);
			expect(saveGeometry.pageOverflow, `Save should not overflow at ${width}px`).toBeLessThanOrEqual(1);
			expect(geometry.pageOverflow, `Options should not overflow at ${width}px`).toBeLessThanOrEqual(1);
			await routePage.close();
		}
	});

	test('Add preserves uploaded graph contexts by default and only overrides them when asked', async ({ page, request }) => {
		await openWorkbenchPage(page, `repositories/${REPOSITORY_ID}/add`);
		await page.locator('#add-import-settings-toggle').click();
		await page.locator('label[for="source-text"]').click();
		await expect(page.locator('#source-text')).toBeChecked();
		await page.locator('#baseURI').fill('https://example.org/base/');
		// An empty "Target graph" keeps the graphs named in the data (plan task M5.5 removed the override checkbox).
		await expect(page.locator('#context')).toBeEnabled();
		await expect(page.locator('#context')).toHaveValue('');
		await page.locator('#Content-Type').selectOption('application/trig');
		await page.locator('#text').fill([
			'<default> <http://example.org/p> "default" .',
			'<http://example.org/graph> { <named> <http://example.org/p> "named" . }'
		].join('\n'));
		await page.locator('#text').blur();
		await expect(page.locator('#Content-Type')).toHaveValue('application/trig');
		await page.getByRole('button', { name: 'Upload' }).click();
		await page.waitForURL(/\/summary$/);

		let response = await request.get(`${SERVER_BASE_URL}/repositories/${REPOSITORY_ID}/statements`, {
			headers: { Accept: 'application/n-quads' }
		});
		expect(response.ok()).toBe(true);
		let nquads = await response.text();
		expect(nquads).toMatch(/^<https:\/\/example\.org\/base\/default> <http:\/\/example\.org\/p> "default" \.\s*$/m);
		expect(nquads).toContain('<https://example.org/base/named> <http://example.org/p> "named" <http://example.org/graph> .');
		expect(nquads).not.toContain('<https://example.org/base/default> <http://example.org/p> "default" <https://example.org/base/> .');

		await openWorkbenchPage(page, `repositories/${REPOSITORY_ID}/add`);
		await page.locator('#add-import-settings-toggle').click();
		await page.locator('label[for="source-text"]').click();
		await expect(page.locator('#source-text')).toBeChecked();
		await page.locator('#baseURI').fill('https://example.org/other/');
		await page.locator('#context').fill('<http://example.org/override>');
		await page.locator('#Content-Type').selectOption('application/trig');
		await page.locator('#text').fill([
			'<default-2> <http://example.org/p> "default-2" .',
			'<http://example.org/graph-2> { <named-2> <http://example.org/p> "named-2" . }'
		].join('\n'));
		await expect(page.locator('#Content-Type')).toHaveValue('application/trig');
		await page.getByRole('button', { name: 'Upload' }).click();
		await page.waitForURL(/\/summary$/);

		response = await request.get(`${SERVER_BASE_URL}/repositories/${REPOSITORY_ID}/statements`, {
			headers: { Accept: 'application/n-quads' }
		});
		expect(response.ok()).toBe(true);
		nquads = await response.text();
		expect(nquads).toContain('<https://example.org/other/default-2> <http://example.org/p> "default-2" <http://example.org/override> .');
		expect(nquads).toContain('<https://example.org/other/named-2> <http://example.org/p> "named-2" <http://example.org/override> .');
		expect(nquads).not.toContain('<https://example.org/other/named-2> <http://example.org/p> "named-2" <http://example.org/graph-2> .');
	});

	test('keeps embedded result settings sized to their values and retains a usable editor', async ({ page }) => {
		test.setTimeout(60000);
		await page.setViewportSize({ width: 1440, height: 1000 });
		await openWorkbenchPage(page, `repositories/${REPOSITORY_ID}/query`);
		await page.locator('.CodeMirror').first().evaluate(element =>
			element.CodeMirror.setValue('SELECT ?s ?p ?o WHERE { ?s ?p ?o }'));
		await page.locator('#exec').click();
		const result = queryResultRoot(page);
		await expect(result).toBeVisible();
		await result.locator('.query-result-options-toggle').click();
		const optionsPanel = queryResultPanels(result).nth(1);
		await expect(optionsPanel).toBeVisible();
		await waitForSettledDisclosure(optionsPanel);
		const options = await optionsPanel.evaluate(element => {
			const size = selector => Math.round(element.querySelector(selector).getBoundingClientRect().width);
			return { layout: size('[id^="result-layout-"]'), limit: size('[id^="stream-result-limit-"]') };
		});
		console.log(`EMBEDDED_RESULT_OPTIONS ${JSON.stringify(options)}`);
		expect(options.layout, 'result layout selector should not fill its grid track').toBeLessThan(260);
		expect(options.limit, 'result limit should fit its short options').toBeLessThan(180);
		await result.locator('.query-result-download-toggle').click();
		const downloadPanel = queryResultPanels(result).nth(0);
		await expect(downloadPanel).toBeVisible();
		await waitForSettledDisclosure(downloadPanel);
		const download = await downloadPanel.locator('[id^="Accept-"]')
			.evaluate(element => Math.round(element.getBoundingClientRect().width));
		console.log(`EMBEDDED_DOWNLOAD_FORMAT ${download}`);
		expect(download, 'download format selector should fit its option labels').toBeLessThan(520);
		await expect(page.locator('.CodeMirror').first()).toBeVisible();
	});

	test('normalizes single-row select geometry across standalone and embedded result controls', async ({ page }) => {
		test.setTimeout(120000);
		const context = page.context();
		const violations = [];
		for (const width of [1440, 768, 320]) {
			for (const [name, route] of [
				['Add', `repositories/${REPOSITORY_ID}/add`],
				['Export', `repositories/${REPOSITORY_ID}/export`],
				['Contexts', `repositories/${REPOSITORY_ID}/contexts`],
				['Types', `repositories/${REPOSITORY_ID}/types`]
			]) {
				const routePage = await openWorkbenchPageInNewPage(context, route, { width, height: 1000 });
				if (name === 'Add') {
					await routePage.locator('#add-import-settings-toggle').click();
				}
				if (name === 'Export') {
					await routePage.locator('#export-advanced-toggle').click();
				}
				const controls = await routePage.locator('#workbench-page-surface select:not([multiple])').evaluateAll(elements =>
				elements.filter(element => element.getBoundingClientRect().width > 0).map(element => ({
					id: element.id || element.name,
					height: Math.round(element.getBoundingClientRect().height),
					centerY: Math.round(element.getBoundingClientRect().top + element.getBoundingClientRect().height / 2)
				})));
				const geometry = await routePage.evaluate(() => ({
					viewport: document.documentElement.clientWidth,
					pageOverflow: document.documentElement.scrollWidth - document.documentElement.clientWidth,
					resultOptionsWidth: document.querySelector('#export-advanced') &&
						Math.round(document.querySelector('#export-advanced').getBoundingClientRect().width)
				}));
				const expectedHeight = width <= 900 ? 44 : 36;
				console.log(`MAIN_SINGLE_SELECTS ${JSON.stringify({ name, width, expectedHeight, controls, ...geometry })}`);
				for (const control of controls) {
					if (Math.abs(control.height - expectedHeight) > 1) {
						violations.push(`${name} ${control.id} at ${width}px is ${control.height}px, expected ${expectedHeight}px`);
					}
				}
				if (name === 'Export' && width > 600 && geometry.resultOptionsWidth > 500) {
					violations.push(`Export result options are ${geometry.resultOptionsWidth}px wide`);
				}
				await routePage.close();
			}

			const queryPage = await openWorkbenchPageInNewPage(context, `repositories/${REPOSITORY_ID}/query`, { width, height: 1000 });
			await queryPage.locator('#query-options-toggle').click();
			const queryOptionHeight = await queryPage.locator('#limit_query')
				.evaluate(element => Math.round(element.getBoundingClientRect().height));
			const expectedHeight = width <= 900 ? 44 : 36;
			console.log(`QUERY_OPTION_SELECT_HEIGHT ${JSON.stringify({ width, expectedHeight, queryOptionHeight })}`);
			if (Math.abs(queryOptionHeight - expectedHeight) > 1) {
				violations.push(`Query options selector at ${width}px is ${queryOptionHeight}px, expected ${expectedHeight}px`);
			}
			await queryPage.locator('.CodeMirror').first().evaluate(element =>
				element.CodeMirror.setValue('SELECT ?s ?p ?o WHERE { ?s ?p ?o }'));
			await queryPage.locator('#exec').click();
			const result = queryResultRoot(queryPage);
			await expect(result).toBeVisible();
			const mainListboxHeight = await queryPage.evaluate(() => {
				const listbox = document.createElement('select');
				listbox.id = 'geometry-main-listbox';
				listbox.size = 4;
				listbox.innerHTML = '<option>One</option><option>Two</option><option>Three</option><option>Four</option>';
				document.querySelector('#workbench-page-surface').appendChild(listbox);
				return Math.round(listbox.getBoundingClientRect().height);
			});
			const embeddedListboxHeight = await result.evaluate(element => {
				const listbox = document.createElement('select');
				listbox.id = 'geometry-embedded-listbox';
				listbox.size = 4;
				listbox.innerHTML = '<option>One</option><option>Two</option><option>Three</option><option>Four</option>';
				element.appendChild(listbox);
				return Math.round(listbox.getBoundingClientRect().height);
			});
			console.log(`NONMULTIPLE_LISTBOX_HEIGHTS ${JSON.stringify({ width, mainListboxHeight, embeddedListboxHeight })}`);
			if (mainListboxHeight < 60 || embeddedListboxHeight < 60) {
				violations.push(`A four-row single-selection listbox collapsed at ${width}px: main ${mainListboxHeight}px, embedded ${embeddedListboxHeight}px`);
			}
			await result.locator('.query-result-options-toggle').click();
			const optionsPanel = queryResultPanels(result).nth(1);
			await expect(optionsPanel).toBeVisible();
			await waitForSettledDisclosure(optionsPanel);
			const resultControls = await optionsPanel.evaluate(element => {
				const bounds = selector => {
					const rect = element.querySelector(selector).getBoundingClientRect();
					return { height: Math.round(rect.height), centerY: Math.round(rect.top + rect.height / 2) };
				};
				return {
					layout: bounds('[id^="result-layout-"]'),
					limit: bounds('[id^="stream-result-limit-"]'),
					labelTops: Array.from(element.querySelectorAll('.query-result-field > span'))
						.slice(0, 2).map(label => Math.round(label.getBoundingClientRect().top))
				};
			});
			await result.locator('.query-result-download-toggle').click();
			const downloadPanel = queryResultPanels(result).nth(0);
			await expect(downloadPanel).toBeVisible();
			await waitForSettledDisclosure(downloadPanel);
			const downloadControls = await downloadPanel.evaluate(element => {
				const select = element.querySelector('[id^="Accept-"]').getBoundingClientRect();
				const button = element.querySelector('.query-result-download-action').getBoundingClientRect();
				return {
					selectHeight: Math.round(select.height),
					buttonHeight: Math.round(button.height),
					centerDifference: Math.abs(Math.round(select.top + select.height / 2) -
						Math.round(button.top + button.height / 2))
				};
			});
			console.log(`EMBEDDED_CONTROL_GEOMETRY ${JSON.stringify({ width, resultControls, downloadControls })}`);
			const resultExpectedHeight = width <= 900 ? 44 : 36;
			if (Math.abs(resultControls.layout.height - resultExpectedHeight) > 1 ||
				Math.abs(resultControls.limit.height - resultExpectedHeight) > 1) {
				violations.push(`Embedded result selectors at ${width}px have heights ${resultControls.layout.height}/${resultControls.limit.height}px`);
			}
			if (width > 900 && (Math.abs(resultControls.layout.centerY - resultControls.limit.centerY) > 1 ||
				Math.abs(resultControls.labelTops[0] - resultControls.labelTops[1]) > 1)) {
				violations.push(`Embedded result option fields are vertically misaligned at ${width}px`);
			}
			if (Math.abs(downloadControls.selectHeight - resultExpectedHeight) > 1 ||
				Math.abs(downloadControls.buttonHeight - resultExpectedHeight) > 1 ||
				(width > 900 && downloadControls.centerDifference > 1)) {
				violations.push(`Embedded download controls are misaligned at ${width}px: ${JSON.stringify(downloadControls)}`);
			}

			const listboxHeight = await queryPage.evaluate(() => {
				const listbox = document.createElement('select');
				listbox.multiple = true;
				listbox.size = 4;
				listbox.innerHTML = '<option>One</option><option>Two</option><option>Three</option><option>Four</option>';
				document.querySelector('#workbench-page-surface').appendChild(listbox);
				const height = Math.round(listbox.getBoundingClientRect().height);
				listbox.remove();
				return height;
			});
			console.log(`MULTIROW_SELECT_HEIGHT ${JSON.stringify({ width, listboxHeight })}`);
			if (listboxHeight < 60) {
				violations.push(`A four-row multiple select collapsed to ${listboxHeight}px at ${width}px`);
			}
			await queryPage.close();
		}
		expect(violations, violations.join('; ')).toEqual([]);
	});

	test('keeps option wrappers compact and their padded contents inset', async ({ page }) => {
		test.setTimeout(90000);
		const context = page.context();
		const violations = [];
		for (const width of [1440, 320]) {
			const addPage = await openWorkbenchPageInNewPage(context, `repositories/${REPOSITORY_ID}/add`, { width, height: 1000 });
			await addPage.locator('label[for="source-text"]').click();
			await addPage.locator('#add-import-settings-toggle').click();
			const addGeometry = await addPage.locator('#add-import-settings').evaluate(element => {
				const panel = element.querySelector('.workbench-disclosure__panel').getBoundingClientRect();
				const body = element.querySelector('.workbench-disclosure__content').getBoundingClientRect();
				const control = element.querySelector('#baseURI').getBoundingClientRect();
				return {
					panel: { left: Math.round(panel.left), right: Math.round(panel.right), width: Math.round(panel.width) },
					body: { left: Math.round(body.left), right: Math.round(body.right), width: Math.round(body.width) },
					control: { left: Math.round(control.left), right: Math.round(control.right), width: Math.round(control.width) }
				};
			});
			console.log(`ADD_ADVANCED_INSET ${JSON.stringify({ width, ...addGeometry })}`);
			if (addGeometry.control.left < addGeometry.panel.left + 8 || addGeometry.control.right > addGeometry.panel.right - 8) {
				violations.push(`Add Advanced control escapes its panel inset at ${width}px`);
			}
			await addPage.close();

			const exportPage = await openWorkbenchPageInNewPage(context, `repositories/${REPOSITORY_ID}/export`, { width, height: 1000 });
			// Export's disclosure holds the timeout since the Export redesign.
			await exportPage.locator('#export-advanced-toggle').click();
			const exportResultPanel = exportPage.locator('#export-advanced-panel');
			await expect(exportResultPanel).toBeVisible();
			await waitForSettledDisclosure(exportResultPanel);
			const exportGeometry = await exportPage.locator('#export-advanced').evaluate(element => {
				const panel = element.querySelector('.workbench-disclosure__panel').getBoundingClientRect();
				const summary = element.querySelector('.workbench-disclosure__toggle').getBoundingClientRect();
				const control = element.querySelector('#timeout').getBoundingClientRect();
				const field = element.querySelector('.workbench-field').getBoundingClientRect();
				return {
					panelWidth: Math.round(panel.width),
					panelHeight: Math.round(panel.height),
					summaryHeight: Math.round(summary.height),
					controlHeight: Math.round(control.height),
					panelRight: Math.round(panel.right),
					controlRight: Math.round(control.right),
					controlBottom: Math.round(control.bottom),
					fieldBottom: Math.round(field.bottom),
					panelBottom: Math.round(panel.bottom)
				};
			});
			console.log(`EXPORT_RESULT_OPTIONS ${JSON.stringify({ width, ...exportGeometry })}`);
			if (width > 600 && exportGeometry.panelWidth > 500) {
				violations.push(`Export Result options are ${exportGeometry.panelWidth}px wide at desktop`);
			}
			const contentAllowance = exportGeometry.panelHeight - exportGeometry.summaryHeight - exportGeometry.controlHeight;
			if (contentAllowance > 100) {
				violations.push(`Export Result options have ${contentAllowance}px of extra vertical space at ${width}px`);
			}
			if (exportGeometry.controlRight > exportGeometry.panelRight - 8) {
				violations.push(`Export paging control is too close to its panel edge at ${width}px`);
			}
			if (exportGeometry.panelBottom - exportGeometry.fieldBottom > 32) {
				violations.push(`Export Result options have excess space below the paging field at ${width}px`);
			}
			await exportPage.close();

			for (const route of [`repositories/${REPOSITORY_ID}/contexts`, `repositories/${REPOSITORY_ID}/types`]) {
				const routePage = await openWorkbenchPageInNewPage(context, route, { width, height: 1000 });
				const bounds = await routePage.evaluate(() => ({
					pageWidth: document.documentElement.clientWidth,
					pageOverflow: document.documentElement.scrollWidth - document.documentElement.clientWidth,
					surfaceWidth: Math.round(document.querySelector('#workbench-page-surface').getBoundingClientRect().width)
				}));
				console.log(`RESULT_PAGE_BOUNDS ${JSON.stringify({ route, width, ...bounds })}`);
				expect(bounds.pageOverflow, `${route} overflows at ${width}px`).toBeLessThanOrEqual(1);
				await routePage.close();
			}
		}
		expect(violations, violations.join('; ')).toEqual([]);
	});

	test('keeps nested creation fields stacked and sizes Explain Config to its contents', async ({ page }) => {
		test.setTimeout(120000);
		const context = page.context();
		const violations = [];
		const createPage = await openWorkbenchPageInNewPage(context, 'repositories/NONE/create?type=memory-rdfs-dt');
		await createPage.locator('#create-advanced-toggle').click();
		const creationGeometry = await createPage.locator(
			'.workbench-advanced__field:last-child'
		).evaluate(row => {
			const label = row.querySelector(':scope > label, :scope > span').getBoundingClientRect();
			const control = row.querySelector('.workbench-advanced__control').getBoundingClientRect();
			return {
				rowDisplay: getComputedStyle(row).display,
				labelBottom: Math.round(label.bottom),
				controlTop: Math.round(control.top),
				fieldCount: row.parentElement.children.length
			};
		});
		console.log(`CREATE_ADVANCED_FIELD_RHYTHM ${JSON.stringify(creationGeometry)}`);
		if (creationGeometry.controlTop < creationGeometry.labelBottom + 3) {
			violations.push('The last advanced Create field places its label inline with the control');
		}
		await createPage.close();
		for (const width of [1440, 320]) {
			const queryPage = await openWorkbenchPageInNewPage(context, `repositories/${REPOSITORY_ID}/query`, { width, height: 1000 });
			await queryPage.locator('.CodeMirror').first().evaluate(element =>
				element.CodeMirror.setValue('SELECT * WHERE { ?s ?p ?o } LIMIT 10'));
			await queryPage.locator('#explain-trigger').click();
			await queryPage.locator('#query-explanation .query-explanation-token--node-type').first().waitFor();
			await queryPage.locator('#explanation-settings-toggle').click();
			const explanationGeometry = await queryPage.locator('#explanation-settings-panel').evaluate(element => {
				const bounds = element.getBoundingClientRect();
				return {
					panelWidth: Math.round(bounds.width),
					panelRight: Math.round(bounds.right),
					viewportWidth: document.documentElement.clientWidth,
					scrollWidth: document.documentElement.scrollWidth
				};
			});
			console.log(`EXPLAIN_CONFIG_BOUNDS ${JSON.stringify({ width, ...explanationGeometry })}`);
			if (width > 600 && explanationGeometry.panelWidth >= 544) {
				violations.push(`Explain Config keeps its fixed ${explanationGeometry.panelWidth}px width`);
			}
			if (explanationGeometry.panelRight > width + 1 || explanationGeometry.scrollWidth > width + 1) {
				violations.push(`Explain Config clips at ${width}px`);
			}
			await queryPage.close();
		}
		expect(violations, violations.join('; ')).toEqual([]);
	});

	test('renders remaining administration forms and disclosures at desktop and narrow widths', async ({ page }) => {
		test.setTimeout(180000);
		const context = page.context();
		const violations = [];
		const viewports = [1440, 320];
		const cases = [
			['Clear', `repositories/${REPOSITORY_ID}/clear`, '#clear-form'],
			['Remove', `repositories/${REPOSITORY_ID}/remove`, '#remove-form'],
			['Delete', 'repositories/NONE/delete', '#delete-form']
		];
		for (const width of viewports) {
			for (const [name, route, selector] of cases) {
				const routePage = await openWorkbenchPageInNewPage(context, route, { width, height: 1000 });
				const form = routePage.locator(selector);
				await expect(form, `${name} form is rendered`).toBeVisible();
				if (name === 'Remove') {
					await routePage.locator('#remove-examples summary').click();
					const examples = await measureContainedRegion(routePage.locator('#remove-examples'));
					const exampleDoc = await measureDocumentBounds(routePage);
					console.log(`ADMIN_FORM_GEOMETRY ${JSON.stringify({ name: 'Remove examples', width, examples, ...exampleDoc })}`);
					if (examples.rootOverflow > 1 || examples.controlsOutsideRoot.length ||
						examples.labelsOutsideRoot.length || exampleDoc.pageOverflow > 1) {
						violations.push(`Remove examples disclosure escapes its content at ${width}px`);
					}
				}
				if (name === 'Delete') {
					await expect(routePage.locator(`#id option[value="${REPOSITORY_ID}"]`),
						'the delete selector includes only the fixture repository under inspection').toHaveCount(1);
				}
				const geometry = await measureContainedRegion(form);
				const document = await measureDocumentBounds(routePage);
				console.log(`ADMIN_FORM_GEOMETRY ${JSON.stringify({ name, width, geometry, ...document })}`);
				if (geometry.rootOverflow > 1 || geometry.controlsOutsideRoot.length || geometry.labelsOutsideRoot.length ||
					geometry.labelControlOverlaps.length || geometry.overflowingChildren.length || document.pageOverflow > 1) {
					violations.push(`${name} form has internal overflow, row overlap, or viewport clipping at ${width}px: ${JSON.stringify({ geometry, document })}`);
				}
				await captureGeometryScreenshot(routePage, name.toLowerCase(), width);
				await routePage.close();
			}

			const summaryPage = await openWorkbenchPageInNewPage(context,
				`repositories/${REPOSITORY_ID}/summary`, { width, height: 1000 });
			const config = summaryPage.locator('#summary-config-model');
			await expect(config, 'repository summary exposes its configuration model').toHaveCount(1);
			await config.locator('summary').click();
			await expect(config.locator('pre')).toBeVisible();
			const summaryGeometry = await measureContainedRegion(config);
			const summaryDocument = await measureDocumentBounds(summaryPage);
			console.log(`SUMMARY_CONFIG_GEOMETRY ${JSON.stringify({ width, summaryGeometry, ...summaryDocument })}`);
			if (summaryGeometry.rootOverflow > 1 || summaryGeometry.overflowingChildren.length ||
				summaryGeometry.controlsOutsideRoot.length || summaryGeometry.labelsOutsideRoot.length || summaryDocument.pageOverflow > 1) {
				violations.push(`Summary configuration content overflows its disclosure at ${width}px`);
			}
			await captureGeometryScreenshot(summaryPage, 'summary-config', width);
			await summaryPage.close();
		}

		await openWorkbenchPage(page, `repositories/${REPOSITORY_ID}/query`);
		await page.locator('.CodeMirror').first().evaluate(element =>
			element.CodeMirror.setValue('SELECT ?s WHERE { ?s ?p ?o } LIMIT 1'));
		await page.locator('#save-query-toggle').press('Enter');
		await page.locator('#query-name').fill(SAVED_QUERY_NAME);
		await page.evaluate(() => window.workbench.query.handleNameChange());
		await page.locator('#save').click();
		await expect(page.locator('#save-feedback')).toHaveText('Query saved.');
		for (const width of viewports) {
			const savedPage = await openWorkbenchPageInNewPage(context,
				`repositories/${REPOSITORY_ID}/saved-queries`, { width, height: 1000 });
			const savedRow = savedPage.locator('.saved-query-row').filter({ hasText: SAVED_QUERY_NAME });
			await expect(savedRow, 'the fixture-owned saved query is visible').toHaveCount(1);
			const toggle = savedRow.locator('.saved-query-toggle');
			savedQueryUrn = await toggle.getAttribute('data-query-urn');
			expect(savedQueryUrn, 'the owned query can be removed through its known identifier after inspection').toBeTruthy();
			await toggle.click();
			await expect(savedRow.locator('.yasqe')).toBeVisible();
			const geometry = await measureContainedRegion(savedRow);
			const document = await measureDocumentBounds(savedPage);
			console.log(`SAVED_QUERY_GEOMETRY ${JSON.stringify({ width, geometry, ...document })}`);
			if (geometry.rootOverflow > 1 || geometry.controlsOutsideRoot.length || geometry.labelsOutsideRoot.length ||
				geometry.labelControlOverlaps.length || geometry.overflowingChildren.length || document.pageOverflow > 1) {
				violations.push(`Saved query settings/action row overflows at ${width}px: ${JSON.stringify({ geometry, document })}`);
			}
			await captureGeometryScreenshot(savedPage, 'saved-query-settings', width);
			await savedPage.close();
		}

		expect(violations, violations.join('; ')).toEqual([]);
	});

	test('renders graph-result option and download forms without changing query data', async ({ page }) => {
		test.setTimeout(90000);
		const violations = [];
		for (const width of [1440, 320]) {
			const graphPage = await openWorkbenchPageInNewPage(page.context(),
				`repositories/${REPOSITORY_ID}/query`, { width, height: 1000 });
			await graphPage.locator('.CodeMirror').first().evaluate(element =>
				element.CodeMirror.setValue('CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }'));
			await graphPage.locator('#exec').click();
			const result = queryResultRoot(graphPage);
			await expect(result).toBeVisible();
			await result.locator('.query-result-options-toggle').click();
			const optionsPanel = queryResultPanels(result).nth(1);
			await expect(optionsPanel).toBeVisible();
			await waitForSettledDisclosure(optionsPanel);
			const options = await measureContainedRegion(optionsPanel);
			await result.locator('.query-result-download-toggle').click();
			const downloadPanel = queryResultPanels(result).nth(0);
			await expect(downloadPanel).toBeVisible();
			await waitForSettledDisclosure(downloadPanel);
			const download = await measureContainedRegion(downloadPanel);
			const resultDocument = await graphPage.evaluate(() => ({
				viewport: document.documentElement.clientWidth,
				pageOverflow: document.documentElement.scrollWidth - document.documentElement.clientWidth
			}));
			console.log(`GRAPH_RESULT_FORM_GEOMETRY ${JSON.stringify({ width, options, download, ...resultDocument })}`);
			if (options.rootOverflow > 1 || download.rootOverflow > 1 ||
				options.controlsOutsideRoot.length || download.controlsOutsideRoot.length ||
				options.labelsOutsideRoot.length || download.labelsOutsideRoot.length ||
				options.labelControlOverlaps.length || download.labelControlOverlaps.length ||
				options.overflowingChildren.length || download.overflowingChildren.length || resultDocument.pageOverflow > 1) {
				violations.push(`Graph result settings or download controls overflow at ${width}px`);
			}
			await captureGeometryScreenshot(graphPage, 'graph-result-options', width);
			await graphPage.close();
		}
		expect(violations, violations.join('; ')).toEqual([]);
	});
});
