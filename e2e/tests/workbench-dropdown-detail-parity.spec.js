// @ts-check
const { test, expect } = require('@playwright/test');
const fs = require('fs');
const path = require('path');

const SERVER_BASE_URL = (process.env.RDF4J_SERVER_BASE_URL
	|| 'http://127.0.0.1:18808/rdf4j-server').replace(/\/+$/, '');
const WORKBENCH_BASE_URL = (process.env.RDF4J_WORKBENCH_BASE_URL
	|| 'http://127.0.0.1:18808/rdf4j-workbench').replace(/\/+$/, '');
const EXTERNAL_REPOSITORY_ID = process.env.RDF4J_DROPDOWN_REPOSITORY_ID || '';
const REPOSITORY_ID = EXTERNAL_REPOSITORY_ID || `workbench-dropdown-detail-${process.pid}-${Date.now()}`;
const REPOSITORY_URL = `${SERVER_BASE_URL}/repositories/${REPOSITORY_ID}`;
const QUERY_URL = `${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/query`;
const SCREENSHOT_DIR = '/tmp/rdf4j-dropdown-preview-20260929';
const CAPTURE_SCREENSHOTS = process.env.RDF4J_DROPDOWN_SCREENSHOTS === 'true';

async function capture(page, name) {
	if (!CAPTURE_SCREENSHOTS) {
		return;
	}
	fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });
	await page.screenshot({
		path: path.join(SCREENSHOT_DIR, name),
		fullPage: true,
		animations: 'disabled'
	});
}

async function waitForSettledPanel(panel) {
	await expect.poll(() => panel.evaluate(element => element.getAnimations({ subtree: false })
		.filter(animation => animation.playState === 'running').length)).toBe(0);
}

function expectHorizontalFlow(items, description) {
	expect(items.length, `${description} should expose its fields and actions`).toBeGreaterThan(1);
	const distinctColumns = new Set(items.map(item => Math.round(item.left))).size;
	expect(distinctColumns, `${description} should use more than one desktop column`).toBeGreaterThan(1);
	for (let index = 1; index < items.length; index += 1) {
		expect(items[index].left, `${description} item ${index + 1} should follow the previous item`)
			.toBeGreaterThanOrEqual(items[index - 1].right - 1);
	}
}

function expectVerticalFlow(items, description) {
	expect(items.length, `${description} should expose its fields and actions`).toBeGreaterThan(1);
	for (let index = 1; index < items.length; index += 1) {
		expect(items[index].top, `${description} item ${index + 1} should stack below the previous item`)
			.toBeGreaterThanOrEqual(items[index - 1].bottom - 1);
	}
}

test.beforeAll(async ({ request }) => {
	if (EXTERNAL_REPOSITORY_ID) {
		return;
	}
	const created = await request.put(REPOSITORY_URL, {
		headers: { 'Content-Type': 'text/turtle' },
		data: `@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix config: <tag:rdf4j.org,2023:config/>.
[] a config:Repository ; config:rep.id "${REPOSITORY_ID}" ;
   rdfs:label "Dropdown detail consistency test" ;
   config:rep.impl [ config:rep.type "openrdf:SailRepository" ;
      config:sail.impl [ config:sail.type "openrdf:MemoryStore" ] ].`
	});
	expect([200, 201, 204]).toContain(created.status());
});

test.afterAll(async ({ request }) => {
	if (!EXTERNAL_REPOSITORY_ID) {
		await request.delete(REPOSITORY_URL);
	}
});

test('query detail panels use the Format surface and responsive field flow', async ({ page }) => {
	test.setTimeout(30000);
	for (const width of [1440, 390]) {
		await page.setViewportSize({ width, height: 1000 });
		await page.goto(QUERY_URL, { waitUntil: 'domcontentloaded' });
		await page.locator('.CodeMirror').waitFor({ state: 'visible' });

		const saveToggle = page.locator('#save-query-toggle');
		const savePanel = page.locator('#save-query-panel');
		const optionsToggle = page.locator('#query-options-toggle');
		const optionsPanel = page.locator('#query-options-panel');
		const closedToolbar = await page.locator('.query-actions-toolbar').evaluate(toolbar => {
			const bounds = element => {
				const rect = element.getBoundingClientRect();
				return { left: rect.left, right: rect.right, top: rect.top, bottom: rect.bottom, width: rect.width };
			};
			return {
				save: bounds(document.getElementById('save-query-toggle')),
				options: bounds(document.getElementById('query-options-toggle')),
				toolbar: bounds(toolbar)
			};
		});
		await saveToggle.click();
		await expect(savePanel).toBeVisible();
		await expect(savePanel).toHaveAttribute('aria-hidden', 'false');
		expect(await savePanel.evaluate(panel => panel.inert), 'opened Save query fields should accept keyboard focus').toBe(false);
		await waitForSettledPanel(savePanel);
		await page.keyboard.press('Tab');
		await expect(page.locator('#query-name')).toBeFocused();
		const appearance = await savePanel.evaluate(panel => {
			const style = getComputedStyle(panel);
			const expectedSurface = document.createElement('div');
			expectedSurface.style.backgroundColor = 'var(--query-soft)';
			document.body.append(expectedSurface);
			const formatSurface = getComputedStyle(expectedSurface).backgroundColor;
			expectedSurface.remove();
			const label = panel.querySelector('label[for="query-name"]');
			const control = panel.querySelector('#query-name');
			const labelRect = label.getBoundingClientRect();
			const controlRect = control.getBoundingClientRect();
			const panelRect = panel.getBoundingClientRect();
			const itemBounds = Array.from(panel.querySelector('.workbench-disclosure__fields').children)
				.filter(item => item.getClientRects().length > 0)
				.map(item => {
					const rect = item.getBoundingClientRect();
					return { left: rect.left, right: rect.right, top: rect.top, bottom: rect.bottom };
				});
			const lowerRow = [control, panel.querySelector('.query-save-disclosure__private'),
				panel.querySelector('.workbench-disclosure__actions')].map(item => {
				const rect = item.getBoundingClientRect();
				return { top: rect.top, bottom: rect.bottom };
			});
			return {
				background: style.backgroundColor,
				formatSurface,
				padding: style.padding,
				labelFontWeight: getComputedStyle(label).fontWeight,
				labelBottom: labelRect.bottom,
				controlTop: controlRect.top,
				panel: { left: panelRect.left, right: panelRect.right, top: panelRect.top, width: panelRect.width },
				control: { left: controlRect.left, right: controlRect.right, width: controlRect.width },
				itemBounds,
				lowerRow
			};
		});
		const saveAfterOpen = await saveToggle.evaluate(toggle => {
			const rect = toggle.getBoundingClientRect();
			return { left: rect.left, right: rect.right, top: rect.top, bottom: rect.bottom, width: rect.width };
		});
		const pageOverflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);

		expect.soft(appearance.background, 'Save query should use the Format panel surface')
			.toBe(appearance.formatSurface);
		expect.soft(appearance.padding, 'Format detail panels use 0.75rem padding')
			.toBe('12px');
		expect.soft(appearance.labelFontWeight, 'Detail field labels use the standard label weight')
			.toBe('600');
		expect.soft(appearance.labelBottom, 'Query name label should sit above its input')
			.toBeLessThanOrEqual(appearance.controlTop + 1);
		expect.soft(appearance.panel.top, 'Save query panel should open below its trigger')
			.toBeGreaterThanOrEqual(saveAfterOpen.bottom - 1);
		expect.soft(appearance.control.right, 'Save query input should fit inside the panel')
			.toBeLessThanOrEqual(appearance.panel.right - 8);
		expect.soft(Math.abs(saveAfterOpen.width - closedToolbar.save.width), 'Save trigger width should stay stable')
			.toBeLessThanOrEqual(1);
		expect.soft(Math.abs(saveAfterOpen.top - closedToolbar.save.top), 'Save trigger should stay in its toolbar row')
			.toBeLessThanOrEqual(1);
		expect.soft(pageOverflow, 'Save query should not create horizontal overflow').toBeLessThanOrEqual(1);
		if (width > 640) {
			expectHorizontalFlow(appearance.itemBounds, 'Save query');
			const lowerRowBottoms = appearance.lowerRow.map(item => item.bottom);
			expect(Math.max(...lowerRowBottoms) - Math.min(...lowerRowBottoms),
				'Save query input, Private checkbox, and action should share a lower row').toBeLessThanOrEqual(2);
		} else {
			expectVerticalFlow(appearance.itemBounds, 'Save query');
		}
		await capture(page, `query-save-${width > 640 ? 'desktop' : 'mobile'}.png`);

		await optionsToggle.click();
		await expect(optionsPanel).toBeVisible();
		await waitForSettledPanel(optionsPanel);
		const optionsGeometry = await optionsPanel.evaluate(panel => {
			const panelRect = panel.getBoundingClientRect();
			const toggleRect = document.getElementById('query-options-toggle').getBoundingClientRect();
			const controls = Array.from(panel.querySelectorAll('select, input:not([type="checkbox"])'))
				.filter(control => control.getClientRects().length > 0)
				.map(control => ({ left: control.getBoundingClientRect().left, right: control.getBoundingClientRect().right }));
			const itemBounds = Array.from(panel.querySelector('.workbench-disclosure__fields').children)
				.filter(item => item.getClientRects().length > 0)
				.map(item => {
					const rect = item.getBoundingClientRect();
					return { left: rect.left, right: rect.right, top: rect.top, bottom: rect.bottom };
				});
			return {
				panel: { left: panelRect.left, right: panelRect.right, top: panelRect.top },
				toggleBottom: toggleRect.bottom,
				controls,
				itemBounds,
				contentOverflow: panel.querySelector('.workbench-disclosure__fields').scrollWidth
					- panel.querySelector('.workbench-disclosure__fields').clientWidth
			};
		});
		const optionsOverflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
		expect.soft(optionsGeometry.panel.top, 'Options panel should open below its trigger')
			.toBeGreaterThanOrEqual(optionsGeometry.toggleBottom - 1);
		for (const control of optionsGeometry.controls) {
			expect.soft(control.left, 'Options controls should stay inside their panel').toBeGreaterThanOrEqual(optionsGeometry.panel.left + 8);
			expect.soft(control.right, 'Options controls should stay inside their panel').toBeLessThanOrEqual(optionsGeometry.panel.right - 8);
		}
		expect.soft(optionsOverflow, 'Options should not create horizontal overflow').toBeLessThanOrEqual(1);
		expect.soft(optionsGeometry.contentOverflow, 'Options content should not overflow its shared field flow')
			.toBeLessThanOrEqual(1);
		if (width > 640) {
			expectHorizontalFlow(optionsGeometry.itemBounds, 'Query Options');
		} else {
			expectVerticalFlow(optionsGeometry.itemBounds, 'Query Options');
		}
		await capture(page, `query-options-${width > 640 ? 'desktop' : 'mobile'}.png`);
	}
});

test('dynamic result details keep distinct triggers and responsive Format-style fields', async ({ page }) => {
	test.setTimeout(30000);
	const runtimeMessages = [];
	page.on('pageerror', error => runtimeMessages.push(`pageerror: ${error.message}`));
	page.on('console', message => {
		if (message.type() === 'error' || message.type() === 'warning') {
			runtimeMessages.push(`${message.type()}: ${message.text()}`);
		}
	});
	for (const width of [1440, 390]) {
		await page.setViewportSize({ width, height: 1000 });
		await page.goto(QUERY_URL, { waitUntil: 'domcontentloaded' });
		await page.locator('.CodeMirror').waitFor({ state: 'visible' });
		await page.locator('.CodeMirror').evaluate(element => {
			element.CodeMirror.setValue('SELECT ?item WHERE { VALUES ?item { "Download alignment review" } }');
		});
		await page.locator('#exec').click();
		const result = page.locator('#query-results [data-query-stream-root]');
		await expect(result).toBeVisible();
		await expect(result).toContainText('Download alignment review');
		const downloadToggle = result.locator('.query-result-download-toggle');
		const optionsToggle = result.locator('.query-result-options-toggle');
		await expect(downloadToggle).toBeVisible();
		await expect(optionsToggle).toBeVisible();
		const triggerGeometry = await result.locator('.query-result-toolbar').evaluate(toolbar => {
			const bounds = selector => {
				const element = toolbar.querySelector(selector);
				const rect = element.getBoundingClientRect();
				const labelRect = element.querySelector('.workbench-disclosure__toggle-label').getBoundingClientRect();
				const iconRect = element.querySelector('.workbench-disclosure-chevron').getBoundingClientRect();
				return {
					left: rect.left, right: rect.right, top: rect.top, bottom: rect.bottom,
					labelRight: labelRect.right, iconLeft: iconRect.left
				};
			};
			return { download: bounds('.query-result-download-toggle'), options: bounds('.query-result-options-toggle') };
		});
		expect(triggerGeometry.download.right, 'Download and Options triggers should not overlap')
			.toBeLessThanOrEqual(triggerGeometry.options.left - 1);
		expect(Math.abs(triggerGeometry.download.top - triggerGeometry.options.top),
			'Download and Options should occupy the same toolbar row').toBeLessThanOrEqual(1);
		expect(triggerGeometry.download.labelRight, 'Download label should not collide with its chevron')
			.toBeLessThanOrEqual(triggerGeometry.download.iconLeft - 1);
		expect(triggerGeometry.options.labelRight, 'Options label should not collide with its chevron')
			.toBeLessThanOrEqual(triggerGeometry.options.iconLeft - 1);

		await downloadToggle.click();
		const downloadPanel = result.locator('[id^="query-result-download-panel-"]');
		await expect(downloadPanel).toBeVisible();
		await waitForSettledPanel(downloadPanel);
		const downloadGeometry = await downloadPanel.evaluate(panel => {
			const panelRect = panel.getBoundingClientRect();
			const items = Array.from(panel.querySelector('.workbench-disclosure__fields').children).map(item => {
				const rect = item.getBoundingClientRect();
				return { left: rect.left, right: rect.right, top: rect.top, bottom: rect.bottom };
			});
			return {
				panel: { left: panelRect.left, right: panelRect.right },
				items,
				overflow: panel.scrollWidth - panel.clientWidth,
				documentOverflow: document.documentElement.scrollWidth - document.documentElement.clientWidth
			};
		});
		expect(downloadGeometry.overflow, 'Download panel should contain its fields').toBeLessThanOrEqual(1);
		expect(downloadGeometry.documentOverflow, 'Download panel should not widen the result document').toBeLessThanOrEqual(1);
		if (width > 640) {
			expectHorizontalFlow(downloadGeometry.items, 'Result Download');
		} else {
			expectVerticalFlow(downloadGeometry.items, 'Result Download');
		}
		await capture(page, `result-download-${width > 640 ? 'desktop' : 'mobile'}.png`);

		await optionsToggle.click();
		const optionsPanel = result.locator('[id^="query-result-options-panel-"]');
		await expect(optionsPanel).toBeVisible();
		await expect(downloadPanel).toBeHidden();
		await waitForSettledPanel(optionsPanel);
		const optionsGeometry = await optionsPanel.evaluate(panel => {
			const items = Array.from(panel.querySelector('.workbench-disclosure__fields').children).map(item => {
				const rect = item.getBoundingClientRect();
				return { left: rect.left, right: rect.right, top: rect.top, bottom: rect.bottom };
			});
			return {
				items,
				overflow: panel.scrollWidth - panel.clientWidth,
				documentOverflow: document.documentElement.scrollWidth - document.documentElement.clientWidth
			};
		});
		expect(optionsGeometry.overflow, 'Result Options should contain its fields').toBeLessThanOrEqual(1);
		expect(optionsGeometry.documentOverflow, 'Result Options should not widen the result document').toBeLessThanOrEqual(1);
		if (width > 640) {
			expectHorizontalFlow(optionsGeometry.items, 'Result Options');
		} else {
			expectVerticalFlow(optionsGeometry.items, 'Result Options');
		}
		await optionsToggle.press('Tab');
		await expect(result.locator('[id^="result-layout-"]')).toBeFocused();
		await capture(page, `result-options-${width > 640 ? 'desktop' : 'mobile'}.png`);
		await optionsToggle.press('Enter');
		await expect(optionsPanel).toBeHidden();
		await expect(optionsToggle).toHaveAttribute('aria-expanded', 'false');
		await optionsToggle.press('Enter');
		await expect(optionsPanel).toBeVisible();
		await waitForSettledPanel(optionsPanel);
	}
	expect(runtimeMessages, 'query result interactions should not report runtime warnings or errors').toEqual([]);
});
