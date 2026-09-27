// @ts-check
const { test, expect } = require('@playwright/test');
const fs = require('fs');
const path = require('path');

const SERVER_BASE_URL = (process.env.RDF4J_SERVER_BASE_URL ||
	'http://127.0.0.1:8086/rdf4j-server').replace(/\/+$/, '');
const WORKBENCH_BASE_URL = (process.env.RDF4J_WORKBENCH_BASE_URL ||
	'http://127.0.0.1:8086/rdf4j-workbench').replace(/\/+$/, '');
const REPOSITORY_ID = `workbench-disclosure-anchor-${process.pid}-${Date.now()}`;
const REPOSITORY_URL = `${SERVER_BASE_URL}/repositories/${REPOSITORY_ID}`;
const QUERY_URL = `${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/query`;
const SCREENSHOT_DIR = process.env.WORKBENCH_DISCLOSURE_SCREENSHOT_DIR;

test.beforeAll(async ({ request }) => {
	const created = await request.put(REPOSITORY_URL, {
		headers: { 'Content-Type': 'text/turtle' },
		data: `@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix config: <tag:rdf4j.org,2023:config/>.
[] a config:Repository ; config:rep.id "${REPOSITORY_ID}" ;
   rdfs:label "Disclosure anchor test" ;
   config:rep.impl [ config:rep.type "openrdf:SailRepository" ;
      config:sail.impl [ config:sail.type "openrdf:MemoryStore" ] ].`
	});
	expect([200, 201, 204]).toContain(created.status());
	const statements = await request.post(`${REPOSITORY_URL}/statements`, {
		headers: { 'Content-Type': 'application/n-triples' },
		data: '<http://example.org/item> <http://example.org/name> "Disclosure anchor" .'
	});
	expect([200, 201, 204]).toContain(statements.status());
});

test.afterAll(async ({ request }) => {
	await request.delete(REPOSITORY_URL);
});

async function panelGeometry(panel) {
	return panel.evaluate(element => {
		const button = document.getElementById(element.getAttribute('aria-labelledby'));
		const track = element.closest('.query-actions-toolbar, .query-result-disclosure-panels');
		const panelRect = element.getBoundingClientRect();
		const buttonRect = button.getBoundingClientRect();
		const trackRect = track.getBoundingClientRect();
		const trackStyle = getComputedStyle(track);
		const buttonStyle = getComputedStyle(button);
		const connector = getComputedStyle(element, '::before');
		const transformOrigin = getComputedStyle(element).transformOrigin.split(' ')[0];
		const buttonCenter = (buttonRect.left + buttonRect.right) / 2;
		const connectorCenter = panelRect.left + parseFloat(connector.left);
		return {
			buttonCenter,
			buttonTop: buttonRect.top,
			buttonHeight: buttonRect.height,
			buttonMinHeight: buttonStyle.minHeight,
			buttonRight: buttonRect.right,
			buttonBottom: buttonRect.bottom,
			panelLeft: panelRect.left,
			panelRight: panelRect.right,
			panelTop: panelRect.top,
			panelBottom: panelRect.bottom,
			connectorContent: connector.content,
			connectorCenter,
			transformOriginCenter: panelRect.left + parseFloat(transformOrigin),
			trackLeft: trackRect.left,
			trackRight: trackRect.right,
			trackBottom: trackRect.bottom,
			rowGap: trackStyle.rowGap,
			gridRows: trackStyle.gridTemplateRows,
			anchorContained: buttonCenter >= panelRect.left - 1 && buttonCenter <= panelRect.right + 1,
			documentWidth: document.documentElement.clientWidth,
			documentScrollWidth: document.documentElement.scrollWidth
		};
	});
}

async function expectPanelAttached(button, panel, desktopAlignment) {
	await expect(button).toHaveAttribute('aria-expanded', 'true');
	await expect(panel).toBeVisible();
	const geometry = await panelGeometry(panel);
	expect(geometry.anchorContained, `trigger center must meet its panel: ${JSON.stringify(geometry)}`).toBe(true);
	expect(geometry.connectorContent, `panel must draw a visible connector: ${JSON.stringify(geometry)}`)
		.not.toBe('none');
		expect(Math.abs(geometry.connectorCenter - geometry.buttonCenter),
		`connector must track the actual trigger: ${JSON.stringify(geometry)}`).toBeLessThanOrEqual(2);
	expect(Math.abs(geometry.transformOriginCenter - geometry.buttonCenter),
		`panel motion origin must track the actual trigger: ${JSON.stringify(geometry)}`).toBeLessThanOrEqual(2);
	expect(geometry.panelTop - geometry.buttonBottom,
		`panel must sit directly below its trigger: ${JSON.stringify(geometry)}`).toBeGreaterThanOrEqual(-1);
	expect(geometry.panelTop - geometry.buttonBottom,
		`panel must sit directly below its trigger: ${JSON.stringify(geometry)}`).toBeLessThanOrEqual(12);
	if (desktopAlignment) {
		expect(geometry.buttonHeight).toBeGreaterThanOrEqual(32);
		expect(geometry.buttonHeight).toBeLessThanOrEqual(40);
		expect(Math.abs(geometry.panelRight - geometry.buttonRight),
			`panel should right-align with its trigger: ${JSON.stringify(geometry)}`).toBeLessThanOrEqual(3);
	} else {
		expect(geometry.buttonHeight).toBeGreaterThanOrEqual(44);
	}
	expect(geometry.documentScrollWidth).toBeLessThanOrEqual(geometry.documentWidth + 1);
	return geometry;
}

test('query and embedded result panels remain attached to their own toolbar triggers', async ({ page }) => {
	for (const width of [1440, 768, 390, 320]) {
		await page.setViewportSize({ width, height: 1000 });
		await page.emulateMedia({ colorScheme: 'dark' });
		await page.goto(QUERY_URL);
		await page.locator('.CodeMirror').waitFor({ state: 'visible' });

		for (const [triggerId, panelId] of [
			['save-query-toggle', 'save-query-panel'],
			['query-options-toggle', 'query-options-panel']
		]) {
			const trigger = page.locator(`#${triggerId}`);
			const panel = page.locator(`#${panelId}`);
			await trigger.press('Enter');
			await expectPanelAttached(trigger, panel, width > 900);
			if (SCREENSHOT_DIR && (triggerId === 'query-options-toggle')
					&& (width === 1440 || width === 320)) {
				fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });
				for (const colorScheme of ['light', 'dark']) {
					await page.emulateMedia({ colorScheme });
					await page.screenshot({
						path: path.join(SCREENSHOT_DIR, `query-options-${width}-${colorScheme}.png`),
						fullPage: true,
						animations: 'disabled'
					});
				}
				await page.emulateMedia({ colorScheme: 'dark' });
			}
			await trigger.press('Enter');
			await expect(panel).toBeHidden();
		}

		const saveTrigger = page.locator('#save-query-toggle');
		const savePanel = page.locator('#save-query-panel');
		const optionsTrigger = page.locator('#query-options-toggle');
		const optionsPanel = page.locator('#query-options-panel');
		await saveTrigger.press('Enter');
		await expectPanelAttached(saveTrigger, savePanel, width > 900);
		await savePanel.locator('#query-name').fill('Preserve this query title');
		await optionsTrigger.press('Enter');
		await expect(savePanel).toBeHidden();
		await expectPanelAttached(optionsTrigger, optionsPanel, width > 900);
		await expect(optionsTrigger).toBeFocused();
		await optionsPanel.locator('select').first().focus();
		await saveTrigger.evaluate(element => element.click());
		await expect(optionsPanel).toBeHidden();
		await expectPanelAttached(saveTrigger, savePanel, width > 900);
		await expect(savePanel.locator('#query-name')).toHaveValue('Preserve this query title');
		await optionsTrigger.evaluate(element => element.click());
		await expect(savePanel).toBeHidden();
		await expectPanelAttached(optionsTrigger, optionsPanel, width > 900);
		await optionsTrigger.press('Enter');
		await expect(savePanel).toBeHidden();
		await expect(optionsTrigger).toBeFocused();

		await page.locator('.CodeMirror').evaluate(element => {
			element.CodeMirror.setValue('SELECT ?s WHERE { ?s ?p ?o }');
		});
		await page.locator('#exec').click();
		const resultFrame = page.frameLocator('#query-results-frame');
		await expect(resultFrame.locator('table.data tbody tr')).toHaveCount(1);

		for (const [triggerId, panelId] of [
			['query-result-options-toggle', 'query-result-options-panel'],
			['query-result-download-toggle', 'query-result-download-panel']
		]) {
			const trigger = resultFrame.locator(`#${triggerId}`);
			const panel = resultFrame.locator(`#${panelId}`);
			await trigger.press('Enter');
			await expectPanelAttached(trigger, panel, width > 900);
			if (SCREENSHOT_DIR && triggerId === 'query-result-options-toggle'
					&& (width === 1440 || width === 320)) {
				fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });
				for (const colorScheme of ['light', 'dark']) {
					await page.emulateMedia({ colorScheme });
					await resultFrame.locator('body').screenshot({
						path: path.join(SCREENSHOT_DIR, `result-options-${width}-${colorScheme}.png`),
						animations: 'disabled'
					});
				}
				await page.emulateMedia({ colorScheme: 'dark' });
			}
			await trigger.press('Enter');
			await expect(panel).toBeHidden();
		}

		const resultOptionsTrigger = resultFrame.locator('#query-result-options-toggle');
		const resultDownloadTrigger = resultFrame.locator('#query-result-download-toggle');
		const resultOptionsPanel = resultFrame.locator('#query-result-options-panel');
		const resultDownloadPanel = resultFrame.locator('#query-result-download-panel');
		await resultOptionsTrigger.press('Enter');
		await expectPanelAttached(resultOptionsTrigger, resultOptionsPanel, width > 900);
		const retainedLayout = resultOptionsPanel.locator('#result-layout');
		await retainedLayout.selectOption('records');
		await resultDownloadTrigger.press('Enter');
		await expect(resultOptionsPanel).toBeHidden();
		await expectPanelAttached(resultDownloadTrigger, resultDownloadPanel, width > 900);
		await resultOptionsTrigger.evaluate(element => element.click());
		await expect(resultDownloadPanel).toBeHidden();
		await expectPanelAttached(resultOptionsTrigger, resultOptionsPanel, width > 900);
		await expect(retainedLayout).toHaveValue('records');
	}
});
