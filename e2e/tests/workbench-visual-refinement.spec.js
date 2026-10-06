// @ts-check
const { test, expect } = require('@playwright/test');
const {
	deleteRepository,
	memoryRepositoryConfiguration,
	serverBaseUrl,
	uniqueRepositoryId,
	waitForRoute,
	workbenchBaseUrl
} = require('./workbench-test-helpers');
const { evidencePath, evidenceScreenshots } = require('./workbench-test-helpers');
const path = require('path');

// Changed with the redesign (see .agent/execplans/workbench-stale-spec-migration-20261002.md; decisions in
// .agent/execplans/workbench-app-shell-and-critique-fixes-20260930.md):
// - "empty navigation, query actions, ...": the empty menu on the server page is gone (task M2.1, one navigation
//   model everywhere; workbench-navigation.spec.js "the server page shows the same menu and server as every other
//   page"); Save query and Query settings no longer stack full width at narrow widths (M3.3 action row: they sit at
//   the end of the row and wrap); the panes open over the page below their toolbar and inside their card instead of
//   pushing the next toggle down (M14.3; workbench-settings-panes.spec.js).
// - "explanation toolbar groups ...": the explanation controls are one toolbar (M3.3): the settings group (level,
//   format, Config) and the action group (Copy, Download, Compare | Explain again).
// - Information's key/value list stacks the label above its value below 600 px (M1.4), so the label/value text gap is
//   measured at 1440 px and the 390 px check is that the value starts below its label. Remove's danger action is the
//   button itself (M6.5), so there is no inner submit input whose transparency could be checked.
// - Export no longer shows an empty preview table: an empty preview is the empty state "No statements to show."
//   (Export redesign, 2026-10-01), so the narrow centering check covers the three browse tables only.
// - Query results stream into the page instead of the retired result iframe.

const SERVER_BASE_URL = serverBaseUrl();
const WORKBENCH_BASE_URL = workbenchBaseUrl();
// Screenshots go to WORKBENCH_VISUAL_REFINEMENT_DIRECTORY when it is set, otherwise to each test's output directory.
const ARTIFACT_DIRECTORY = process.env.WORKBENCH_VISUAL_REFINEMENT_DIRECTORY || '';
const REPOSITORY_ID = uniqueRepositoryId('workbench-visual-contract');
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
			data: memoryRepositoryConfiguration(repositoryId, 'Workbench visual contract fixture')
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
	for (const repositoryId of [REPOSITORY_ID, EMPTY_REPOSITORY_ID]) {
		await deleteRepository(request, SERVER_BASE_URL, repositoryId);
	}
});

test('query actions, metadata, and editor retain balanced geometry', async ({ page }) => {
	test.setTimeout(90_000);
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
				// Save query and Query settings wrap at the end of the action row (M3.3): stacked at 320 px, side by
				// side at 390 px. Either way they keep at least the 8 px control gap between them.
				saveToOptionsGap: options.top >= save.bottom ? options.top - save.bottom : options.left - save.right,
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
				const cardBox = document.querySelector('#query-form').getBoundingClientRect();
				const owner = toggle.parentElement;
				const nextToggle = toggleSelector === '#save-query-toggle'
					? document.querySelector('#query-options-toggle')
					: null;
				// The Save query fields in document order (they sit in the pane's content wrapper).
				const saveSequence = Array.from(document.querySelector('#save-query-panel')
					.querySelectorAll('label[for="query-name"], #query-name, .query-option, #save'))
					.map(child => child.id || child.className.split(' ')[0] || child.tagName);
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
					toolbarBottom: toolbarBox.bottom,
					toolbarWidth: toolbarBox.width,
					cardLeft: cardBox.left,
					cardRight: cardBox.right,
					panelOwnedByDisclosure: owner.contains(panel),
					disclosureDisplay: getComputedStyle(owner).display,
					nextToggleBottom: nextToggle ? nextToggle.getBoundingClientRect().bottom : null,
					saveSequence
				};
			}, { toggleSelector: toggle, panelSelector: panel });
			console.log(`QUERY_DISCLOSURE_GEOMETRY ${width}px ${toggle} ${JSON.stringify(panelGeometry)}`);
			expect(panelGeometry.panelTop).toBeGreaterThanOrEqual(panelGeometry.toggleBottom - 1);
			// The pane opens over the page inside its card (M14.3), which can be wider than the action toolbar.
			expect(panelGeometry.panelLeft).toBeGreaterThanOrEqual(panelGeometry.cardLeft - 1);
			expect(panelGeometry.panelRight).toBeLessThanOrEqual(panelGeometry.cardRight + 1);
			expect(panelGeometry.panelOwnedByDisclosure).toBe(true);
			expect.soft(Math.abs(panelGeometry.panelAnchorCenter - panelGeometry.toggleCenter)).toBeLessThanOrEqual(1);
			if (width <= 600) {
				// A pane in a toolbar opens below the whole toolbar, so it covers none of the toolbar's buttons.
				expect.soft(panelGeometry.panelTop - panelGeometry.toolbarBottom).toBeGreaterThanOrEqual(7);
				expect.soft(panelGeometry.panelTop - panelGeometry.toolbarBottom).toBeLessThanOrEqual(10);
				expect.soft(panelGeometry.panelRight - panelGeometry.panelLeft)
					.toBeGreaterThanOrEqual(panelGeometry.toolbarWidth - 2);
				if (panelGeometry.nextToggleBottom !== null) {
					expect.soft(panelGeometry.nextToggleBottom).toBeLessThanOrEqual(panelGeometry.panelTop + 1);
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

	// Information is a key/value list (M1.4): two columns on desktop, the label stacked above its value below 600 px.
	const informationGeometry = async () => page.locator('#workbench-information .workbench-kv__row').first()
		.evaluate(row => {
			const label = row.querySelector('dt').getBoundingClientRect();
			const value = row.querySelector('dd').getBoundingClientRect();
			const labelRange = document.createRange();
			labelRange.selectNodeContents(row.querySelector('dt'));
			const valueRange = document.createRange();
			valueRange.selectNodeContents(row.querySelector('dd'));
			const labelText = labelRange.getBoundingClientRect();
			const valueText = valueRange.getBoundingClientRect();
			return {
				cellGap: value.left - label.right,
				textGap: valueText.left - labelText.right,
				stackedTextGap: valueText.top - labelText.bottom,
				valuePadding: parseFloat(getComputedStyle(row).columnGap)
			};
		});
	await page.setViewportSize({ width: 390, height: 900 });
	await setTheme(page, 'dark');
	await openPage(page, `repositories/${REPOSITORY_ID}/information`);
	const narrowInformation = await informationGeometry();
	console.log(`INFORMATION_VALUE_GAP 390px ${JSON.stringify(narrowInformation)}`);
	expect.soft(narrowInformation.stackedTextGap, 'the value text starts below its label at 390 px')
		.toBeGreaterThanOrEqual(0);
	await captureState(page, 'information-dark-390.png');
	await page.setViewportSize({ width: 1440, height: 1000 });
	const wideInformation = await informationGeometry();
	console.log(`INFORMATION_VALUE_GAP 1440px ${JSON.stringify(wideInformation)}`);
	expect.soft(wideInformation.textGap).toBeGreaterThanOrEqual(8);
	expect.soft(wideInformation.valuePadding).toBeGreaterThanOrEqual(8);

	await page.setViewportSize({ width: 1440, height: 1000 });
	await setTheme(page, 'light');
	// The outlined danger action lives on Remove since Namespaces became an in-row editor (plan task M6.3).
	await openPage(page, `repositories/${REPOSITORY_ID}/remove`);
	const dangerStyle = await page.locator('#remove-form .workbench-action--danger-outline').evaluate(action => {
		const style = getComputedStyle(action);
		const colorProbe = document.createElement('span');
		colorProbe.style.color = 'var(--workbench-danger)';
		colorProbe.style.backgroundColor = 'var(--workbench-surface)';
		action.append(colorProbe);
		const tokenStyle = getComputedStyle(colorProbe);
		const danger = tokenStyle.color;
		const surface = tokenStyle.backgroundColor;
		colorProbe.remove();
		return {
			tagName: action.tagName,
			color: style.color,
			borderColor: style.borderTopColor,
			backgroundColor: style.backgroundColor,
			danger,
			surface
		};
	});
	console.log(`REMOVE_DANGER_STYLE ${JSON.stringify(dangerStyle)}`);
	// Remove's action is the button itself since plan task M6.5 (it no longer wraps a transparent submit input).
	expect.soft(dangerStyle.tagName).toBe('BUTTON');
	expect.soft(dangerStyle.color).toBe(dangerStyle.danger);
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
	// One explanation toolbar (M3.3): the settings group (level, format, Config) and the action group, which holds
	// the utility controls (Copy, Download, Compare) and the repeat controls (Explain again).
	const spacing = await page.locator('.query-explanation-toolbar').evaluate(row => {
		const settings = row.querySelector('.query-explanation-toolbar__settings');
		const controls = row.querySelector('#query-explanation-controls-row');
		const rerun = controls.querySelector('#primary-explain-repeat-controls');
		const utility = controls.querySelector('#primary-explain-utility-controls');
		return {
			marginLeft: parseFloat(getComputedStyle(row).marginLeft),
			controlsMarginLeft: parseFloat(getComputedStyle(controls).marginLeft),
			groupGap: controls.getBoundingClientRect().left - settings.getBoundingClientRect().right,
			settingsGap: parseFloat(getComputedStyle(settings).columnGap),
			rerunGap: parseFloat(getComputedStyle(rerun).columnGap),
			utilityExists: Boolean(utility),
			utilityGap: utility ? parseFloat(getComputedStyle(utility).columnGap) : 0,
			utilityControls: utility ? Array.from(utility.querySelectorAll('button, input[type="button"]'))
				.filter(control => control.getClientRects().length > 0).map(control => control.id) : []
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
	expect(spacing.utilityControls).toEqual(['copy-explanation', 'download-explanation', 'compare-toggle']);
});

test('preview actions stay separate and empty result messages belong after table headings', async ({ page }) => {
	await page.setViewportSize({ width: 1440, height: 1000 });
	await openPage(page, `repositories/${REPOSITORY_ID}/export`);
	// The preview limit has its own form in the preview card since the Export redesign.
	const exportSelects = await page.locator('#workbench-page-surface .workbench-select-control > select').evaluateAll(selects =>
		selects.map(select => ({
			id: select.id,
			wrapperClass: select.parentElement.classList.contains('workbench-select-control'),
			appearance: getComputedStyle(select).appearance,
			chevronCount: select.parentElement.querySelectorAll('.workbench-select-chevron').length
		}))
	);
	expect(exportSelects.map(select => select.id)).toEqual(['Accept', 'limit_export']);
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

	await page.locator('#export-preview-form button[type="submit"]').click();
	await waitForRoute(page, 'export', { url: (url) => url.searchParams.get('action') === 'preview' });
	await expect(page.locator('#export-results table.data tbody tr')).toHaveCount(2);
	const populatedGap = await exportContentGap(page);
	console.log(`EXPORT_POPULATED_ACTION_GAP ${populatedGap}`);
	expect.soft(populatedGap).toBeGreaterThanOrEqual(8);
	await captureState(page, 'export-preview-populated-1440.png');

	for (const route of [
		['namespaces', 'namespaces-results'],
		['contexts', 'contexts-results'],
		['types', 'types-results']
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

	// An empty Export preview is one empty-state message instead of a table (Export redesign).
	await openPage(page, `repositories/${EMPTY_REPOSITORY_ID}/export`);
	await page.locator('#export-preview-form button[type="submit"]').click();
	await waitForRoute(page, 'export', { url: (url) => url.searchParams.get('action') === 'preview' });
	const emptyPreview = page.locator('#export-results .workbench-empty[role="status"]');
	await expect(emptyPreview).toBeVisible();
	await expect(emptyPreview).toHaveText('No statements to show.');
	await expect(page.locator('#export-results table.data')).toHaveCount(0);
	await captureState(page, 'empty-export-preview-1440.png');
});

test('narrow empty table messages center across their responsive table', async ({ page }) => {
	await page.setViewportSize({ width: 390, height: 900 });
	for (const route of [
		['namespaces', 'namespaces-results'],
		['contexts', 'contexts-results'],
		['types', 'types-results']
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
	const cases = [
		['tuple', 'SELECT ?s WHERE { BIND(<urn:visual:absent> AS ?s) FILTER(false) }'],
		['graph', 'CONSTRUCT { <urn:visual:s> <urn:visual:p> <urn:visual:o> } WHERE { FILTER(false) }']
	];
	let previousRoot = null;
	for (const [kind, query] of cases) {
		await page.locator('.CodeMirror').first().evaluate((editor, text) => editor.CodeMirror.setValue(text), query);
		await page.locator('#exec').click();
		// Each execution streams a new result root into the page; wait until this execution's result is complete.
		const resultRoot = await waitForNewCompleteResult(page, previousRoot);
		previousRoot = resultRoot;
		const result = page.locator(`#query-results [data-query-stream-root][id="${resultRoot}"]`);
		const state = await result.locator('table.data').evaluateAll(tables => tables.map(table => ({
			rows: table.querySelectorAll('tbody tr').length,
			statusRows: table.querySelectorAll('tbody [role="status"]').length
		})));
		console.log(`EMPTY_QUERY_RESULT_TABLES ${kind} ${JSON.stringify(state)}`);
		expect(state.length).toBeGreaterThan(0);
		expect(state.every(table => table.rows === 0 && table.statusRows === 0)).toBe(true);
		await captureState(page, `query-empty-${kind}-result-1440.png`);
	}
});

/** Waits for a result root other than previousRoot whose execution has finished, and returns its id. */
async function waitForNewCompleteResult(page, previousRoot) {
	const handle = await page.waitForFunction(previous => {
		const results = document.querySelector('#query-results');
		const root = results && results.querySelector('[data-query-stream-root]');
		const status = root && root.querySelector('.query-result-status');
		return !!root && root.id !== previous && results.getAttribute('aria-busy') === 'false'
			&& !!status && /complete/.test(status.textContent) && root.id;
	}, previousRoot, { timeout: 30000 });
	return handle.jsonValue();
}

async function exportContentGap(page) {
	return page.locator('#export-results').evaluate(section => {
		const actions = section.querySelector('.export-preview__controls').getBoundingClientRect();
		const empty = section.querySelector('.workbench-empty');
		// The statements are the shared result table; its root starts where its table (or records) start.
		const content = empty && getComputedStyle(empty).display !== 'none'
			? empty
			: section.querySelector('.query-result-layout');
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
	// Measure only once the view's scripts have mounted it (the Query editor changes the height above its actions).
	await waitForRoute(page, route.split('?')[0].split('/').pop());
	await page.evaluate(() => document.fonts.ready);
}

async function captureState(page, fileName) {
	if (!evidenceScreenshots()) {
		return;
	}
	const screenshotDirectory = ARTIFACT_DIRECTORY ? path.join(ARTIFACT_DIRECTORY, 'final', 'states') : '';
	await page.screenshot({ path: evidencePath(screenshotDirectory, fileName), fullPage: true, animations: 'disabled' });
}
