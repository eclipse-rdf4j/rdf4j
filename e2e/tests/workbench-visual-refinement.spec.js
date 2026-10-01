// @ts-check
const { test, expect } = require('@playwright/test');
const fs = require('fs');
const path = require('path');

const SERVER_BASE_URL = (process.env.RDF4J_SERVER_BASE_URL || 'http://127.0.0.1:8090/rdf4j-server').replace(/\/+$/, '');
const WORKBENCH_BASE_URL = (process.env.RDF4J_WORKBENCH_BASE_URL || 'http://127.0.0.1:8090/rdf4j-workbench').replace(/\/+$/, '');
const ARTIFACT_DIRECTORY = process.env.WORKBENCH_VISUAL_REFINEMENT_DIRECTORY
	|| path.resolve(__dirname, '../../output/workbench-visual-refinement');
const REPOSITORY_ID = `workbench-visual-contract-${process.pid}-${Date.now()}`;
const EMPTY_REPOSITORY_ID = `${REPOSITORY_ID}-empty`;
const REPOSITORY_URL = `${SERVER_BASE_URL}/repositories/${REPOSITORY_ID}`;
const EMPTY_REPOSITORY_URL = `${SERVER_BASE_URL}/repositories/${EMPTY_REPOSITORY_ID}`;

test.beforeAll(async ({ request }) => {
	for (const [repositoryId, repositoryUrl] of [
		[REPOSITORY_ID, REPOSITORY_URL],
		[EMPTY_REPOSITORY_ID, EMPTY_REPOSITORY_URL]
	]) {
		const response = await request.put(repositoryUrl, {
			headers: { 'Content-Type': 'text/turtle' },
			data: repositoryConfiguration(repositoryId)
		});
		expect([200, 201, 204]).toContain(response.status());
	}

	const loaded = await request.post(`${REPOSITORY_URL}/statements`, {
		headers: { 'Content-Type': 'application/n-quads' },
		data: [
			'<urn:visual:alice> <urn:visual:name> "Alice" .',
			'<urn:visual:bob> <urn:visual:type> <urn:visual:Person> <urn:visual:people> .'
		].join('\n')
	});
	expect([200, 201, 204]).toContain(loaded.status());
});

test.afterAll(async ({ request }) => {
	for (const repositoryUrl of [REPOSITORY_URL, EMPTY_REPOSITORY_URL]) {
		const response = await request.delete(repositoryUrl);
		expect([200, 204, 404]).toContain(response.status());
	}
});

test('empty navigation, query actions, metadata, and editor retain balanced geometry', async ({ page }) => {
	test.setTimeout(90_000);
	await page.setViewportSize({ width: 1440, height: 1000 });
	await openPage(page, 'repositories/NONE/server');
	const navigation = await page.evaluate(() => {
		const disclosure = document.querySelector('#workbench-navigation-disclosure');
		const main = document.querySelector('#content');
		const nav = document.querySelector('#navigation');
					return {
			items: nav.querySelectorAll('ul.maingroup > li').length,
			navigationHeight: nav.getBoundingClientRect().height,
			disclosureHeight: disclosure.getBoundingClientRect().height,
			mainLeft: main.getBoundingClientRect().left
		};
	});
	console.log(`EMPTY_NAV_GEOMETRY ${JSON.stringify(navigation)}`);
	expect.soft(navigation.items).toBe(0);
	expect.soft(navigation.navigationHeight).toBe(0);
	expect.soft(navigation.disclosureHeight).toBe(0);
	expect.soft(navigation.mainLeft).toBeLessThan(80);

	await page.setViewportSize({ width: 320, height: 900 });
	await openPage(page, `repositories/${REPOSITORY_ID}/query`);
	for (const width of [320, 390]) {
		await page.setViewportSize({ width, height: 900 });
		await openPage(page, `repositories/${REPOSITORY_ID}/query`);
		const queryActions = await page.evaluate(() => {
			const saveButton = document.querySelector('#save-query-toggle');
			const optionsButton = document.querySelector('#query-options-toggle');
			const save = saveButton.getBoundingClientRect();
			const options = optionsButton.getBoundingClientRect();
			const primary = document.querySelector('.query-actions-toolbar__primary').getBoundingClientRect();
			const toolbar = document.querySelector('.query-actions-toolbar').getBoundingClientRect();
			const text = saveButton.querySelector('span');
			const range = document.createRange();
			range.selectNodeContents(text);
			const style = getComputedStyle(saveButton);
			return {
				saveHeight: save.height,
				optionsHeight: options.height,
				saveTextLines: range.getClientRects().length,
				paddingLeft: parseFloat(style.paddingLeft),
				paddingRight: parseFloat(style.paddingRight),
				controlGap: parseFloat(style.columnGap),
				saveWidth: save.width,
				optionsWidth: options.width,
				primaryToSaveGap: save.top - primary.bottom,
				saveToOptionsGap: options.top - save.bottom,
				toolbarWidth: toolbar.width,
				stacked: options.top >= save.bottom
			};
		});
		console.log(`QUERY_ACTION_GEOMETRY ${width}px ${JSON.stringify(queryActions)}`);
		expect.soft(Math.abs(queryActions.saveHeight - queryActions.optionsHeight)).toBeLessThanOrEqual(1);
		expect.soft(queryActions.saveTextLines).toBe(1);
		expect.soft(queryActions.paddingLeft).toBeGreaterThanOrEqual(12);
		expect.soft(queryActions.paddingRight).toBeGreaterThanOrEqual(12);
		expect.soft(queryActions.controlGap).toBeGreaterThanOrEqual(8);
		expect.soft(queryActions.stacked).toBe(true);
		expect.soft(queryActions.saveWidth).toBeGreaterThanOrEqual(queryActions.toolbarWidth - 2);
		expect.soft(queryActions.optionsWidth).toBeGreaterThanOrEqual(queryActions.toolbarWidth - 2);
		expect.soft(queryActions.primaryToSaveGap).toBeGreaterThanOrEqual(8);
		expect.soft(queryActions.saveToOptionsGap).toBeGreaterThanOrEqual(8);
		await captureState(page, `query-actions-${width}-closed.png`);

		for (const [toggle, panel] of [
			['#save-query-toggle', '#save-query-panel'],
			['#query-options-toggle', '#query-options-panel']
		]) {
			await page.locator(toggle).press('Enter');
			await expect(page.locator(panel)).toBeVisible();
			await expect.poll(() => page.locator(panel).evaluate(element =>
				Array.from(element.getAnimations()).some(animation => animation.playState === 'running'))).toBe(false);
			const panelGeometry = await page.evaluate(({ toggleSelector, panelSelector }) => {
				const toggleBox = document.querySelector(toggleSelector).getBoundingClientRect();
				const panelBox = document.querySelector(panelSelector).getBoundingClientRect();
				const toolbarBox = document.querySelector('.query-actions-toolbar').getBoundingClientRect();
				const toggle = document.querySelector(toggleSelector);
				const panel = document.querySelector(panelSelector);
				const owner = toggle.parentElement;
				const nextToggle = toggleSelector === '#save-query-toggle'
					? document.querySelector('#query-options-toggle')
					: null;
				const saveChildren = Array.from(document.querySelector('#save-query-panel').children);
				const anchorX = parseFloat(getComputedStyle(panel).getPropertyValue('--workbench-disclosure-anchor-x'));
				return {
					panelTop: panelBox.top,
					panelLeft: panelBox.left,
					panelRight: panelBox.right,
					panelBottom: panelBox.bottom,
					toggleBottom: toggleBox.bottom,
					toggleLeft: toggleBox.left,
					toggleWidth: toggleBox.width,
					toggleCenter: (toggleBox.left + toggleBox.right) / 2,
					panelAnchorCenter: panelBox.left + anchorX,
					toolbarLeft: toolbarBox.left,
					toolbarRight: toolbarBox.right,
					toolbarWidth: toolbarBox.width,
					panelOwnedByDisclosure: owner.contains(panel),
					disclosureDisplay: getComputedStyle(owner).display,
					nextToggleTop: nextToggle ? nextToggle.getBoundingClientRect().top : null,
					saveSequence: saveChildren.map(child => child.id || child.className || child.tagName)
				};
			}, { toggleSelector: toggle, panelSelector: panel });
			console.log(`QUERY_DISCLOSURE_GEOMETRY ${width}px ${toggle} ${JSON.stringify(panelGeometry)}`);
			expect(panelGeometry.panelTop).toBeGreaterThanOrEqual(panelGeometry.toggleBottom - 1);
			expect(panelGeometry.panelLeft).toBeGreaterThanOrEqual(panelGeometry.toolbarLeft - 1);
			expect(panelGeometry.panelRight).toBeLessThanOrEqual(panelGeometry.toolbarRight + 1);
			expect(panelGeometry.panelOwnedByDisclosure).toBe(true);
			expect.soft(Math.abs(panelGeometry.panelAnchorCenter - panelGeometry.toggleCenter)).toBeLessThanOrEqual(1);
			if (width <= 600) {
				expect.soft(panelGeometry.panelTop - panelGeometry.toggleBottom).toBeGreaterThanOrEqual(7);
				expect.soft(panelGeometry.panelTop - panelGeometry.toggleBottom).toBeLessThanOrEqual(10);
				expect.soft(Math.abs(panelGeometry.panelLeft - panelGeometry.toggleLeft)).toBeLessThanOrEqual(1);
				expect.soft(panelGeometry.panelRight - panelGeometry.panelLeft)
					.toBeGreaterThanOrEqual(panelGeometry.toolbarWidth - 2);
				if (panelGeometry.nextToggleTop !== null) {
					expect.soft(panelGeometry.nextToggleTop).toBeGreaterThanOrEqual(panelGeometry.panelBottom - 1);
				}
			}
			if (toggle === '#save-query-toggle') {
				const saveSequence = panelGeometry.saveSequence;
				expect.soft(saveSequence.indexOf('query-name')).toBeGreaterThan(saveSequence.indexOf('query-form__label'));
				expect.soft(saveSequence.indexOf('query-option')).toBeGreaterThan(saveSequence.indexOf('query-name'));
				expect.soft(saveSequence.lastIndexOf('save')).toBeGreaterThan(saveSequence.indexOf('query-option'));
			}
			await captureState(page, `query-actions-${width}-${panel.slice(1).replace('-panel', '')}-open.png`);
		}
	}

	await page.setViewportSize({ width: 390, height: 900 });
	await setTheme(page, 'dark');
	await openPage(page, `repositories/${REPOSITORY_ID}/information`);
	const informationGap = await page.locator('#workbench-information .workbench-kv__row').first().evaluate(row => {
		const label = row.querySelector('dt').getBoundingClientRect();
		const value = row.querySelector('dd').getBoundingClientRect();
		const labelRange = document.createRange();
		labelRange.selectNodeContents(row.querySelector('dt'));
		const valueRange = document.createRange();
		valueRange.selectNodeContents(row.querySelector('dd'));
		return {
			cellGap: value.left - label.right,
			textGap: valueRange.getBoundingClientRect().left - labelRange.getBoundingClientRect().right,
			valuePadding: parseFloat(getComputedStyle(row).columnGap)
		};
	});
	console.log(`INFORMATION_VALUE_GAP ${JSON.stringify(informationGap)}`);
	expect.soft(informationGap.textGap).toBeGreaterThanOrEqual(8);
	expect.soft(informationGap.valuePadding).toBeGreaterThanOrEqual(8);
	await captureState(page, 'information-dark-390.png');

	await page.setViewportSize({ width: 1440, height: 1000 });
	await setTheme(page, 'light');
	// The outlined danger action lives on Remove since Namespaces became an in-row editor (plan task M6.3).
	await openPage(page, `repositories/${REPOSITORY_ID}/remove`);
	const dangerStyle = await page.locator('#remove-form .workbench-action--danger-outline').evaluate(action => {
		const style = getComputedStyle(action);
		// Remove's action is a button since plan task M6.5; older actions wrap an input.
		const input = action.querySelector('input[type="submit"]') || action;
		const inputStyle = getComputedStyle(input);
		const colorProbe = document.createElement('span');
		colorProbe.style.color = 'var(--workbench-danger)';
		colorProbe.style.backgroundColor = 'var(--workbench-surface)';
		action.append(colorProbe);
		const tokenStyle = getComputedStyle(colorProbe);
		const danger = tokenStyle.color;
		const surface = tokenStyle.backgroundColor;
		colorProbe.remove();
		return {
			color: style.color,
			inputColor: inputStyle.color,
			inputBackgroundColor: inputStyle.backgroundColor,
			borderColor: style.borderTopColor,
			backgroundColor: style.backgroundColor,
			danger,
			surface
		};
	});
	console.log(`REMOVE_DANGER_STYLE ${JSON.stringify(dangerStyle)}`);
	expect.soft(dangerStyle.color).toBe(dangerStyle.danger);
	expect.soft(dangerStyle.inputColor).toBe(dangerStyle.danger);
	expect.soft(dangerStyle.inputBackgroundColor).toBe('rgba(0, 0, 0, 0)');
	expect.soft(dangerStyle.borderColor).toBe(dangerStyle.danger);
	expect.soft(dangerStyle.backgroundColor).toBe(dangerStyle.surface);

	await setTheme(page, 'dark');
	await openPage(page, `repositories/${REPOSITORY_ID}/update`);
	await page.locator('#update-editor .CodeMirror').waitFor({ state: 'visible' });
	const editorBorder = await page.locator('#update-editor .CodeMirror').evaluate(editor => {
		const probe = document.createElement('span');
		probe.style.color = 'var(--workbench-editor-gutter-border)';
		document.body.append(probe);
		const expected = getComputedStyle(probe).color;
		probe.remove();
		return { border: getComputedStyle(editor).borderTopColor, expected };
	});
	console.log(`DARK_UPDATE_EDITOR_BORDER ${JSON.stringify(editorBorder)}`);
	expect.soft(editorBorder.border).toBe(editorBorder.expected);
});

test('explanation toolbar groups use deliberate spacing without an offset', async ({ page }) => {
	await page.setViewportSize({ width: 1440, height: 1000 });
	await openPage(page, `repositories/${REPOSITORY_ID}/query`);
	await page.locator('.CodeMirror').first().evaluate(editor => editor.CodeMirror.setValue('SELECT ?s WHERE { ?s ?p ?o }'));
	await page.locator('#explain-trigger').click();
	await expect.poll(() => page.locator('#query-explanation').textContent()).toMatch(/\S/);
	const spacing = await page.locator('#query-explanation-controls-row').evaluate(row => {
		const controls = row.querySelector('.query-form__field--controls');
		const settings = controls.querySelector('#primary-explain-settings');
		const rerun = controls.querySelector('#primary-explain-repeat-controls');
		const utility = controls.querySelector('#primary-explain-utility-controls');
		return {
			marginLeft: parseFloat(getComputedStyle(row).marginLeft),
			controlsMarginLeft: parseFloat(getComputedStyle(controls).marginLeft),
			groupGap: parseFloat(getComputedStyle(controls).columnGap),
			settingsGap: parseFloat(getComputedStyle(settings).columnGap),
			rerunGap: parseFloat(getComputedStyle(rerun).columnGap),
			utilityExists: Boolean(utility),
			utilityGap: utility ? parseFloat(getComputedStyle(utility).columnGap) : 0,
			utilityControls: utility ? utility.querySelectorAll('button, input[type="button"]').length : 0
		};
	});
	console.log(`EXPLANATION_TOOLBAR_SPACING ${JSON.stringify(spacing)}`);
	expect(spacing.marginLeft).toBeGreaterThanOrEqual(0);
	expect(spacing.controlsMarginLeft).toBeGreaterThanOrEqual(0);
	expect(spacing.groupGap).toBeGreaterThanOrEqual(16);
	expect(spacing.settingsGap).toBeGreaterThanOrEqual(8);
	expect(spacing.rerunGap).toBeGreaterThanOrEqual(8);
	expect(spacing.utilityExists).toBe(true);
	expect(spacing.utilityGap).toBeGreaterThanOrEqual(8);
	expect(spacing.utilityControls).toBe(2);
});

test('preview actions stay separate and empty result messages belong after table headings', async ({ page }) => {
	await page.setViewportSize({ width: 1440, height: 1000 });
	await openPage(page, `repositories/${REPOSITORY_ID}/export`);
	const exportSelects = await page.locator('#export-form .workbench-select-control > select').evaluateAll(selects =>
		selects.map(select => ({
			id: select.id,
			wrapperClass: select.parentElement.classList.contains('workbench-select-control'),
			appearance: getComputedStyle(select).appearance,
			chevronCount: select.parentElement.querySelectorAll('.workbench-select-chevron').length
		}))
	);
	expect(exportSelects.map(select => select.id)).toEqual(['Accept', 'compression', 'limit_export']);
	await expect(page.locator('#timeout')).toHaveValue('43200');
	for (const select of exportSelects) {
		expect(select.wrapperClass).toBe(true);
		expect(select.appearance).toBe('none');
		expect(select.chevronCount).toBe(1);
	}
	await expect(page.locator('#export-results .workbench-empty')).toBeVisible();
	const emptyGap = await exportContentGap(page);
	console.log(`EXPORT_EMPTY_ACTION_GAP ${emptyGap}`);
	expect.soft(emptyGap).toBeGreaterThanOrEqual(8);
	await captureState(page, 'export-preview-empty-1440.png');

	await Promise.all([
		page.waitForNavigation({ waitUntil: 'domcontentloaded' }),
		page.locator('#export-results button[name="action"][value="preview"]').click()
	]);
	await expect(page.locator('#export-results table.data tbody tr')).toHaveCount(2);
	const populatedGap = await exportContentGap(page);
	console.log(`EXPORT_POPULATED_ACTION_GAP ${populatedGap}`);
	expect.soft(populatedGap).toBeGreaterThanOrEqual(8);
	await captureState(page, 'export-preview-populated-1440.png');

	for (const route of [
		['namespaces', 'namespaces-results'],
		['contexts', 'contexts-results'],
		['types', 'types-results'],
		['export', 'export-results']
	]) {
		await openPage(page, `repositories/${EMPTY_REPOSITORY_ID}/${route[0]}`);
		const table = page.locator(`#${route[1]} table.data`);
		const tableState = await table.evaluate(element => ({
			head: Boolean(element.querySelector('thead')),
			statusCount: element.querySelectorAll('tbody [role="status"]').length,
			statusVisible: Array.from(element.querySelectorAll('tbody [role="status"]'))
				.some(status => status.getClientRects().length > 0)
		}));
		console.log(`EMPTY_TABLE_STATE ${route[0]} ${JSON.stringify(tableState)}`);
		expect.soft(tableState.head).toBe(true);
		expect.soft(tableState.statusCount).toBe(1);
		expect.soft(tableState.statusVisible).toBe(true);
		await captureState(page, `empty-${route[0]}-table-1440.png`);
	}
});

test('narrow empty table messages center across their responsive table', async ({ page }) => {
	await page.setViewportSize({ width: 390, height: 900 });
	for (const route of [
		['namespaces', 'namespaces-results'],
		['contexts', 'contexts-results'],
		['types', 'types-results'],
		['export', 'export-results']
	]) {
		await openPage(page, `repositories/${EMPTY_REPOSITORY_ID}/${route[0]}`);
		const alignment = await page.locator(`#${route[1]} table.data`).evaluate(table => {
			const tableBounds = table.getBoundingClientRect();
			const cell = table.querySelector('tbody td[role="status"]');
			const cellBounds = cell.getBoundingClientRect();
			const range = document.createRange();
			range.selectNodeContents(cell);
			const textBounds = range.getBoundingClientRect();
			return {
				tableCenter: (tableBounds.left + tableBounds.right) / 2,
				cellCenter: (cellBounds.left + cellBounds.right) / 2,
				textCenter: (textBounds.left + textBounds.right) / 2,
				beforeDisplay: getComputedStyle(cell, '::before').display,
				tableWidth: tableBounds.width,
				cellWidth: cellBounds.width
			};
		});
		console.log(`NARROW_EMPTY_TABLE_ALIGNMENT ${route[0]} ${JSON.stringify(alignment)}`);
		expect(Math.abs(alignment.textCenter - alignment.tableCenter)).toBeLessThanOrEqual(2);
		expect(Math.abs(alignment.cellCenter - alignment.tableCenter)).toBeLessThanOrEqual(2);
		expect(alignment.beforeDisplay).toBe('none');
	}
});

test('preset repository IDs remain accessible and readable at desktop and narrow widths', async ({ page }) => {
	for (const [width, theme] of [[390, 'light'], [1440, 'dark']]) {
		await page.setViewportSize({ width, height: 1000 });
		await setTheme(page, theme);
		await openPage(page, 'repositories/NONE/create?type=memory-customrule');
		const input = page.locator('#config_rep-id');
		const visibleText = await input.evaluate(element => {
			const canvas = document.createElement('canvas');
			const context = canvas.getContext('2d');
			const style = getComputedStyle(element);
			context.font = style.font;
			return {
				value: element.value,
				textWidth: context.measureText(element.value).width,
				availableWidth: element.clientWidth - parseFloat(style.paddingLeft) - parseFloat(style.paddingRight),
				withinViewport: element.getBoundingClientRect().left >= 0
					&& element.getBoundingClientRect().right <= document.documentElement.clientWidth
			};
		});
		const accessibleMatches = await page.getByRole('textbox', { name: 'Repository ID' }).count();
		console.log(`PRESET_ID_READABILITY ${width}px ${theme} ${JSON.stringify({ ...visibleText, accessibleMatches })}`);
		expect.soft(visibleText.value).toBe('memory-customrule');
		expect.soft(visibleText.withinViewport).toBe(true);
		expect.soft(accessibleMatches).toBe(1);
		expect.soft(visibleText.textWidth).toBeLessThanOrEqual(visibleText.availableWidth);
		const advanced = page.locator('.workbench-advanced[data-workbench-detail-disclosure="true"]');
		const toggle = advanced.locator(':scope > .workbench-disclosure__toggle');
		const disclosureChevron = toggle.locator(':scope > .workbench-disclosure-chevron');
		const chevronCount = await disclosureChevron.count();
		const chevron = chevronCount ? await disclosureChevron.evaluate(element => ({
			width: element.getBoundingClientRect().width,
			height: element.getBoundingClientRect().height,
			rotation: getComputedStyle(element).transform
		})) : null;
		await captureState(page, `customrule-id-${width}-${theme}-advanced-closed.png`);
		console.log(`CREATE_ADVANCED_DISCLOSURE ${width}px ${theme} ${JSON.stringify({ chevronCount, chevron })}`);
		expect.soft(chevronCount).toBe(1);
		if (chevron) {
			expect.soft(chevron.width).toBe(16);
			expect.soft(chevron.height).toBe(16);
		}
		await toggle.press('Enter');
		await expect(toggle).toHaveAttribute('aria-expanded', 'true');
		if (chevronCount) {
			await expect.poll(() => disclosureChevron.evaluate(element => getComputedStyle(element).transform))
				.toBe('matrix(-1, 0, 0, -1, 0, 0)');
		}
		await captureState(page, `customrule-id-${width}-${theme}-advanced-open.png`);
	}
});

test('Add format selection keeps its shared chevron clear of supported labels', async ({ page }) => {
	for (const width of [320, 390, 1440]) {
		await page.setViewportSize({ width, height: 1000 });
		await openPage(page, `repositories/${REPOSITORY_ID}/add`);
		const select = page.locator('#Content-Type');
		await expect(select).toBeVisible();
		const longestValue = await select.evaluate(element => {
			const canvas = document.createElement('canvas');
			const context = canvas.getContext('2d');
			context.font = getComputedStyle(element).font;
			return Array.from(element.options)
				.map(option => ({ value: option.value, width: context.measureText(option.text).width }))
				.sort((left, right) => right.width - left.width)[0].value;
		});
		for (const selected of ['autodetect', longestValue]) {
			await select.selectOption(selected);
		const selectChrome = await select.evaluate(element => {
				const wrapper = element.parentElement;
				const icon = wrapper.querySelector('.workbench-select-chevron');
				const box = element.getBoundingClientRect();
				const iconBox = icon?.getBoundingClientRect();
				const style = getComputedStyle(element);
				const canvas = document.createElement('canvas');
				const context = canvas.getContext('2d');
				context.font = style.font;
				const selectedLabel = element.selectedOptions[0].text.trim();
				const labelWidth = context.measureText(selectedLabel).width;
				const availableTextWidth = element.clientWidth - parseFloat(style.paddingLeft) - parseFloat(style.paddingRight);
				return {
					sharedWrapper: wrapper.classList.contains('workbench-select-control'),
					selected: element.value,
					selectedLabel,
					labelWidth,
					availableTextWidth,
					labelClearance: availableTextWidth - labelWidth,
					labelFits: labelWidth <= availableTextWidth,
					appearance: style.appearance,
					paddingRight: parseFloat(style.paddingRight),
					iconCount: wrapper.querySelectorAll('.workbench-select-chevron').length,
					iconInside: Boolean(iconBox && iconBox.left >= box.left && iconBox.right <= box.right),
					iconWidth: iconBox?.width || 0,
					pointerEvents: icon ? getComputedStyle(icon).pointerEvents : ''
				};
			});
			console.log(`ADD_FORMAT_SELECT ${width}px ${JSON.stringify(selectChrome)}`);
			expect.soft(selectChrome.sharedWrapper).toBe(true);
			expect.soft(selectChrome.selected).toBe(selected);
			expect.soft(selectChrome.labelFits).toBe(true);
			expect.soft(selectChrome.labelClearance).toBeGreaterThanOrEqual(8);
			expect.soft(selectChrome.appearance).toBe('none');
			expect.soft(selectChrome.paddingRight).toBeGreaterThanOrEqual(36);
			expect.soft(selectChrome.iconCount).toBe(1);
			expect.soft(selectChrome.iconInside).toBe(true);
			expect.soft(selectChrome.iconWidth).toBe(16);
			expect.soft(selectChrome.pointerEvents).toBe('none');
			await captureState(page, `add-format-${width}-${selected === 'autodetect' ? 'autodetect' : 'longest'}.png`);
		}
	}
});

test('empty tuple and graph query tables remain free of preview-only status rows', async ({ page }) => {
	await page.setViewportSize({ width: 1440, height: 1000 });
	await openPage(page, `repositories/${REPOSITORY_ID}/query`);
	const resultFrame = page.frameLocator('#query-results-frame');
	const cases = [
		['tuple', 'SELECT ?s WHERE { BIND(<urn:visual:absent> AS ?s) FILTER(false) }'],
		['graph', 'CONSTRUCT { <urn:visual:s> <urn:visual:p> <urn:visual:o> } WHERE { FILTER(false) }']
	];
	for (const [kind, query] of cases) {
		await page.locator('.CodeMirror').first().evaluate((editor, text) => editor.CodeMirror.setValue(text), query);
		await page.locator('#exec').click();
		await expect(resultFrame.locator('#rdf4j-query-result')).toBeAttached();
		const state = await resultFrame.locator('table.data').evaluateAll(tables => tables.map(table => ({
			rows: table.querySelectorAll('tbody tr').length,
			statusRows: table.querySelectorAll('tbody [role="status"]').length
		})));
		console.log(`EMPTY_QUERY_RESULT_TABLES ${kind} ${JSON.stringify(state)}`);
		expect(state.length).toBeGreaterThan(0);
		expect(state.every(table => table.rows === 0 && table.statusRows === 0)).toBe(true);
		await captureState(page, `query-empty-${kind}-result-1440.png`);
	}
});

async function exportContentGap(page) {
	return page.locator('#export-results').evaluate(section => {
		const actions = section.querySelector('.workbench-form-actions').getBoundingClientRect();
		const empty = section.querySelector('.workbench-empty');
		const content = empty && getComputedStyle(empty).display !== 'none'
			? empty
			: section.querySelector('table.data');
		return content.getBoundingClientRect().top - actions.bottom;
	});
}

async function setTheme(page, theme) {
	await openPage(page, 'repositories/NONE/repositories');
	await page.evaluate(theme => window.RDF4JWorkbenchTheme.setPreference(theme), theme);
	await expect(page.locator('html')).toHaveAttribute('data-theme', theme);
}

async function openPage(page, route) {
	await page.goto(`${WORKBENCH_BASE_URL}/${route}`, { waitUntil: 'domcontentloaded' });
	await page.locator('#workbench-page-surface').waitFor({ state: 'visible', timeout: 15000 });
	await page.evaluate(() => document.fonts.ready);
}

function repositoryConfiguration(repositoryId) {
	return `@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix config: <tag:rdf4j.org,2023:config/>.
[] a config:Repository ; config:rep.id "${repositoryId}" ; rdfs:label "Workbench visual contract fixture" ;
   config:rep.impl [ config:rep.type "openrdf:SailRepository" ;
      config:sail.impl [ config:sail.type "openrdf:MemoryStore" ] ].`;
}

async function captureState(page, fileName) {
	const screenshotDirectory = path.join(ARTIFACT_DIRECTORY, 'final', 'states');
	fs.mkdirSync(screenshotDirectory, { recursive: true });
	await page.screenshot({ path: path.join(screenshotDirectory, fileName), fullPage: true, animations: 'disabled' });
}
