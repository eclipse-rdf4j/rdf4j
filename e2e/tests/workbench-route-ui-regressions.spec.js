/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

// @ts-check
const { test, expect } = require('@playwright/test');
const { runQuery, serverBaseUrl, waitForRoute, workbenchBaseUrl } = require('./workbench-test-helpers');

const SERVER_BASE_URL = serverBaseUrl();
const WORKBENCH_BASE_URL = workbenchBaseUrl();
const RUN_ID = `${process.pid}-${Date.now().toString(36)}`;
const REPOSITORY_ID = `route-ui-${RUN_ID}`;
const REPOSITORY_URL = `${SERVER_BASE_URL}/repositories/${REPOSITORY_ID}`;

let repositoryCreated = false;

test.setTimeout(120_000);

async function waitForDisclosureMotion(page, panelSelector) {
	await page.waitForFunction(selector => {
		const panel = document.querySelector(selector);
		return panel && !panel.hidden && panel.getAnimations().length === 0;
	}, panelSelector);
}

async function panelViewportGeometry(page, panelId) {
	return page.evaluate(id => {
		const panel = document.getElementById(id);
		const rect = panel.getBoundingClientRect();
		return { x: rect.x, y: rect.y, right: rect.right, bottom: rect.bottom,
			width: rect.width, height: rect.height, scrollHeight: panel.scrollHeight,
			clientHeight: panel.clientHeight, viewport: { width: innerWidth, height: innerHeight } };
	}, panelId);
}

test.beforeAll(async ({ request }) => {
	const existing = await request.get(REPOSITORY_URL);
	if (existing.status() !== 400 && existing.status() !== 404) {
		throw new Error(`Refusing to use non-absent route-test repository ${REPOSITORY_ID}: ${existing.status()}`);
	}
	const created = await request.put(REPOSITORY_URL, {
		headers: { 'Content-Type': 'text/turtle' },
		data: `@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix config: <tag:rdf4j.org,2023:config/>.
[] a config:Repository ; config:rep.id "${REPOSITORY_ID}" ; rdfs:label "Workbench route UI regression fixture" ;
   config:rep.impl [ config:rep.type "openrdf:SailRepository" ;
      config:sail.impl [ config:sail.type "openrdf:MemoryStore" ] ].`
	});
	expect([200, 201, 204]).toContain(created.status());
	repositoryCreated = true;
	const loaded = await request.post(`${REPOSITORY_URL}/statements`, {
		headers: { 'Content-Type': 'application/n-triples' },
		data: [
			'<urn:route-ui:item> <urn:route-ui:name> "Fixture item" .',
			'<urn:route-ui:item> <http://www.w3.org/2000/01/rdf-schema#label> "Route UI Item" .',
			'<urn:route-ui:item> <http://www.w3.org/2000/01/rdf-schema#type> <urn:route-ui:Thing> .'
		].join('\n')
	});
	expect([200, 201, 204]).toContain(loaded.status());
});

test.afterAll(async ({ request }) => {
	if (repositoryCreated) {
		const deleted = await request.delete(REPOSITORY_URL);
		expect([200, 204, 400, 404]).toContain(deleted.status());
	}
});

test('route templates preserve paging, saved-query streams, export selection, and query controls', async ({ page, request }) => {
	page.setDefaultTimeout(8_000);
	page.setDefaultNavigationTimeout(12_000);
	await page.setViewportSize({ width: 1280, height: 900 });
	const pageErrors = [];
	const consoleErrors = [];
	const failedRequests = [];
	page.on('pageerror', error => pageErrors.push(error.message));
	page.on('console', message => {
		if (message.type() === 'error') {
			consoleErrors.push(message.text());
		}
	});
	page.on('requestfailed', requestEvent => {
		const error = requestEvent.failure()?.errorText || 'unknown';
		if (error !== 'net::ERR_ABORTED') {
			failedRequests.push({ url: requestEvent.url(), error });
		}
	});

	const repositoryBase = `${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}`;
	console.log('ROUTE_UI_FIXTURE', REPOSITORY_ID);

	await page.goto(`${repositoryBase}/explore`, { waitUntil: 'domcontentloaded' });
	await expect(page.locator('#workbench-app')).toHaveAttribute('data-workbench-view', 'explore');
	const exploreError = await page.locator('#workbench-app [role="alert"]').textContent().catch(() => null);
	console.log('EXPLORE_PAGING_RED', JSON.stringify({
		error: exploreError,
		previousValue: await page.locator('#previousX').getAttribute('value').catch(() => null),
		nextValue: await page.locator('#nextX').getAttribute('value').catch(() => null),
		consoleErrors
	}));
	await expect.soft(page.locator('#workbench-page-surface')).toBeVisible();
	expect.soft(exploreError, 'Explore must not fail while binding legacy paging controls').toBeNull();
	expect.soft(consoleErrors.join('\n')).not.toContain("Cannot read properties of null (reading '0')");

	const savedResponsePromise = page.waitForResponse(response => response.request().url().includes(`/repositories/${REPOSITORY_ID}/saved-queries`)
		&& response.request().headers().accept?.includes('application/vnd.rdf4j.workbench+ndjson'), { timeout: 15_000 });
	const savedWireResponse = await request.get(`${repositoryBase}/saved-queries`, {
		headers: { Accept: 'application/vnd.rdf4j.workbench+ndjson' }
	});
	const savedWireBody = await savedWireResponse.text();
	const savedWireRecords = savedWireBody.trim().split(/\r?\n/).map(line => JSON.parse(line));
	const savedWireTerminal = savedWireRecords[savedWireRecords.length - 1]?.type || null;
	console.log('SAVED_QUERIES_WIRE', JSON.stringify({
		status: savedWireResponse.status(),
		contentType: savedWireResponse.headers()['content-type'] || null,
		terminal: savedWireTerminal,
		body: savedWireBody
	}));
	await page.goto(`${repositoryBase}/saved-queries`, { waitUntil: 'domcontentloaded' });
	const savedResponse = await savedResponsePromise;
	const savedState = {
		status: savedResponse.status(),
		contentType: savedResponse.headers()['content-type'] || null,
		wireTerminal: savedWireTerminal,
		pageError: await page.locator('#workbench-app [role="alert"]').textContent().catch(() => null),
		emptyState: await page.locator('#saved-queries .saved-queries-empty').count(),
		requestFailures: failedRequests.filter(entry => entry.url.includes('/saved-queries'))
	};
	console.log('SAVED_QUERIES_STREAM_RED', JSON.stringify(savedState));
	expect.soft(savedState.status).toBe(200);
	expect.soft(savedState.contentType).toContain('application/vnd.rdf4j.workbench+ndjson');
	expect.soft(savedState.wireTerminal, 'Saved Queries page model must end with a terminal record').toBe('end');
	expect.soft(savedState.pageError, 'Saved Queries must render a usable route').toBeNull();
	expect.soft(savedState.emptyState + await page.locator('#saved-queries .saved-query-row').count()).toBeGreaterThan(0);

	await page.context().clearCookies();
	await page.goto(`${repositoryBase}/export`, { waitUntil: 'domcontentloaded' });
	const configuredDefault = await page.locator('#Accept').inputValue();
	expect.soft(configuredDefault, 'configured export default should be N-Quads').toBe('application/n-quads');
	await page.context().addCookies([{ name: 'Accept', value: 'application/n-triples', url: WORKBENCH_BASE_URL }]);
	await page.goto(`${repositoryBase}/export`, { waitUntil: 'domcontentloaded' });
	const explicitCookieFormat = await page.locator('#Accept').inputValue();
	expect.soft(explicitCookieFormat, 'an explicit Accept cookie remains an honored preference').toBe('application/n-triples');
	const exportButtonState = await page.evaluate(() => {
		const form = document.querySelector('#export-form');
		const candidates = form ? Array.from(form.querySelectorAll('button,input[type="submit"]')) : [];
		return {
			formHtml: form?.outerHTML || null,
			candidates: candidates.map(element => ({
				tag: element.tagName.toLowerCase(),
				text: element.textContent?.trim() || element.getAttribute('value') || '',
				name: element.getAttribute('name'),
				value: element.getAttribute('value'),
				ariaLabel: element.getAttribute('aria-label'),
				icon: element.querySelector('svg')?.getAttribute('data-workbench-icon') || null
			}))
		};
	});
	// The Download button names its file since the Export redesign ("Download export.nq.gz").
	const exportDownloadIcon = exportButtonState.candidates.find(candidate => candidate.text.startsWith('Download'))?.icon || null;
	console.log('EXPORT_DOWNLOAD_CONTROL_RED', JSON.stringify(exportButtonState));
	expect.soft(await page.getByRole('button', { name: 'Download' }).count(),
		'the export download button has a computed accessible name').toBe(1);
	expect.soft(exportDownloadIcon, 'the export download button uses the shared download icon').toBe('download');
	await page.locator('#Accept').selectOption('application/n-quads');
	const previewResponsePromise = page.waitForResponse(response => response.request().url().includes(`/repositories/${REPOSITORY_ID}/export`)
		&& response.request().headers().accept?.includes('application/vnd.rdf4j.workbench+ndjson'), { timeout: 20_000 });
	await page.getByRole('button', { name: 'Show preview' }).click();
	await waitForRoute(page, 'export', { url: (url) => url.searchParams.get('action') === 'preview', timeout: 20_000 });
	const previewResponse = await previewResponsePromise;
	await expect(page.locator('#export-results')).toBeVisible();
	const previewState = {
		status: previewResponse.status(),
		contentType: previewResponse.headers()['content-type'] || null,
		requestMethod: previewResponse.request().method(),
		requestUrl: previewResponse.url(),
		selectedFormat: await page.locator('#Accept').inputValue(),
		rowCount: await page.locator('#export-results table.data tbody tr').count(),
		emptyState: await page.locator('#export-results .workbench-empty').count()
	};
	console.log('EXPORT_PREVIEW_SELECTION_RED', JSON.stringify(previewState));
	expect.soft(previewState.status).toBe(200);
	expect.soft(previewState.contentType).toContain('application/vnd.rdf4j.workbench+ndjson');
	expect.soft(previewState.selectedFormat, 'preview preserves the newly selected N-Quads format').toBe('application/n-quads');
	expect.soft(previewState.rowCount + previewState.emptyState).toBeGreaterThan(0);

	await page.context().clearCookies();
	await page.goto(`${repositoryBase}/query`, { waitUntil: 'domcontentloaded' });
	await expect(page.locator('#query-form')).toBeVisible();
	await page.locator('.CodeMirror').first().waitFor({ state: 'visible' });
	const iconState = await page.evaluate(() => {
		const icon = selector => {
			const button = document.querySelector(selector);
			const svg = button && button.querySelector('svg');
			return {
				accessibleName: button?.getAttribute('aria-label') || button?.innerText.trim() || '',
				iconName: svg?.getAttribute('data-workbench-icon') || null,
				path: svg?.querySelector('path')?.getAttribute('d') || null
			};
		};
		return {
			execute: icon('#exec'),
			explain: icon('#explain-trigger')
		};
	});
	expect.soft(iconState.execute.accessibleName).toContain('Execute');
	expect.soft(iconState.execute.iconName).toBe('execute');
	expect.soft(iconState.explain.accessibleName).toContain('Explain');
	expect.soft(iconState.explain.iconName).toBe('explain');
	console.log('QUERY_ICONS_RED', JSON.stringify(iconState));

	await page.locator('.CodeMirror').first().evaluate(element => element.CodeMirror.setValue('SELECT ?s WHERE { ?s ?p ?o } LIMIT 1'));
	await page.locator('#explain-trigger').click();
	await expect(page.locator('#query-explanation-row')).toBeVisible({ timeout: 20_000 });
	const configToggle = page.locator('#explanation-settings-toggle');
	await configToggle.click();
	await expect(configToggle).toHaveAttribute('aria-expanded', 'true');
	const explanationPanel = page.locator('#explanation-settings-panel');
	await expect(explanationPanel).toBeVisible();
	await waitForDisclosureMotion(page, '#explanation-settings-panel');
	const explanationGeometry = await page.evaluate(() => {
		const trigger = document.querySelector('#explanation-settings-toggle').getBoundingClientRect();
		const panel = document.querySelector('#explanation-settings-panel').getBoundingClientRect();
		return { trigger: { x: trigger.x, y: trigger.y, bottom: trigger.bottom },
			panel: { x: panel.x, y: panel.y, right: panel.right, bottom: panel.bottom, width: panel.width, height: panel.height },
			viewport: { width: innerWidth, height: innerHeight } };
	});
	console.log('EXPLANATION_POPUP_GEOMETRY_RED', JSON.stringify(explanationGeometry));
	expect.soft(explanationGeometry.panel.x, 'explanation settings remain within the viewport horizontally').toBeGreaterThanOrEqual(0);
	expect.soft(explanationGeometry.panel.right, 'explanation settings remain within the viewport horizontally')
		.toBeLessThanOrEqual(explanationGeometry.viewport.width);
	expect.soft(explanationGeometry.panel.y, 'explanation settings remain within viewport').toBeGreaterThanOrEqual(0);
	expect.soft(explanationGeometry.panel.bottom, 'explanation settings remain within viewport').toBeLessThanOrEqual(explanationGeometry.viewport.height);
	expect.soft(Math.abs(explanationGeometry.panel.y - explanationGeometry.trigger.bottom), 'settings popup stays anchored to its Config control')
		.toBeLessThan(120);
	await configToggle.click();
	await page.setViewportSize({ width: 390, height: 844 });
	await configToggle.click();
	await expect(configToggle).toHaveAttribute('aria-expanded', 'true');
	await expect(explanationPanel).toBeVisible();
	await waitForDisclosureMotion(page, '#explanation-settings-panel');
	const narrowExplanationGeometry = await page.evaluate(() => {
		const trigger = document.querySelector('#explanation-settings-toggle').getBoundingClientRect();
		const panel = document.querySelector('#explanation-settings-panel').getBoundingClientRect();
		return { trigger: { x: trigger.x, y: trigger.y, bottom: trigger.bottom },
			panel: { x: panel.x, y: panel.y, right: panel.right, bottom: panel.bottom,
				width: panel.width, height: panel.height },
			viewport: { width: innerWidth, height: innerHeight } };
	});
	console.log('NARROW_EXPLANATION_POPUP_GEOMETRY_RED', JSON.stringify(narrowExplanationGeometry));
	expect.soft(narrowExplanationGeometry.panel.x, 'narrow explanation settings remain within viewport horizontally').toBeGreaterThanOrEqual(0);
	expect.soft(narrowExplanationGeometry.panel.right, 'narrow explanation settings remain within viewport horizontally')
		.toBeLessThanOrEqual(narrowExplanationGeometry.viewport.width);
	expect.soft(narrowExplanationGeometry.panel.y, 'narrow explanation settings remain within viewport vertically').toBeGreaterThanOrEqual(0);
	expect.soft(narrowExplanationGeometry.panel.bottom, 'narrow explanation settings remain within viewport vertically')
		.toBeLessThanOrEqual(narrowExplanationGeometry.viewport.height);
	expect.soft(Math.abs(narrowExplanationGeometry.panel.y - narrowExplanationGeometry.trigger.bottom),
		'narrow explanation settings remain anchored below Config').toBeLessThan(120);
	await configToggle.click();
	await page.setViewportSize({ width: 1280, height: 900 });
	await page.locator('#compare-toggle').click();
	await expect(page.locator('#query-compare-toolbar')).toBeVisible();
	await configToggle.click();
	await waitForDisclosureMotion(page, '#explanation-settings-panel');
	const compareExplanationGeometry = await page.evaluate(() => {
		const trigger = document.querySelector('#explanation-settings-toggle').getBoundingClientRect();
		const panel = document.querySelector('#explanation-settings-panel').getBoundingClientRect();
		return { triggerBottom: trigger.bottom, panel: { x: panel.x, y: panel.y, right: panel.right, bottom: panel.bottom },
			viewport: { width: innerWidth, height: innerHeight } };
	});
	console.log('COMPARE_EXPLANATION_POPUP_GEOMETRY_RED', JSON.stringify(compareExplanationGeometry));
	expect.soft(compareExplanationGeometry.panel.x).toBeGreaterThanOrEqual(0);
	expect.soft(compareExplanationGeometry.panel.right).toBeLessThanOrEqual(compareExplanationGeometry.viewport.width);
	expect.soft(compareExplanationGeometry.panel.y).toBeGreaterThanOrEqual(0);
	expect.soft(compareExplanationGeometry.panel.bottom).toBeLessThanOrEqual(compareExplanationGeometry.viewport.height);
	expect.soft(Math.abs(compareExplanationGeometry.panel.y - compareExplanationGeometry.triggerBottom)).toBeLessThan(120);
	await configToggle.click();
	const compareToolbar = await page.evaluate(() => {
		const toggle = document.querySelector('#query-sidebar-toggle');
		const wrapper = document.querySelector('#query-sidebar-toggle-action');
		const close = document.querySelector('#query-compare-close');
		const rect = close.getBoundingClientRect();
		return {
			toggleLabel: toggle.getAttribute('aria-label'),
			toggleShowLabel: toggle.getAttribute('data-show-label'),
			toggleHideLabel: toggle.getAttribute('data-hide-label'),
			wrapperBorder: getComputedStyle(wrapper).borderTopWidth,
			buttonBorder: getComputedStyle(toggle).borderTopWidth,
			closeLabel: close.getAttribute('aria-label'),
			closeText: close.innerText.trim(),
			closeWidth: rect.width,
			closeScrollWidth: close.scrollWidth,
			secondaryControls: document.querySelectorAll('#query-compare-pane #explanation-settings-toggle').length
		};
	});
	console.log('COMPARE_CONTROLS_RED', JSON.stringify(compareToolbar));
	expect.soft(compareToolbar.toggleLabel, 'sidebar toggle has an accessible name').toMatch(/show|hide/i);
	expect.soft(compareToolbar.wrapperBorder, 'sidebar toggle avoids a double border').toBe('0px');
	expect.soft(compareToolbar.closeLabel).toBe('Close comparison');
	expect.soft(compareToolbar.closeScrollWidth, 'close control content fits its hit area').toBeLessThanOrEqual(compareToolbar.closeWidth);
	expect.soft(compareToolbar.secondaryControls, 'right comparison pane retains its no-controls contract').toBe(0);

	await page.locator('#query-compare-close').click();
	await page.locator('#query-compare-toolbar').waitFor({ state: 'hidden' });
	// The result toolbar shows Download and Display only once rows are on screen (plan task M3.5), so the result is read
	// after the streamed query has finished.
	await runQuery(page, 'SELECT ?s WHERE { ?s ?p ?o } LIMIT 1');
	await expect(page.locator('#query-results [data-query-stream-root="true"]')).toBeVisible({ timeout: 20_000 });
	const queryDownloadButton = page.locator('#query-results .query-result-download-button');
	const resultOptions = page.locator('#query-results .query-result-toolbar__disclosures .query-result-disclosure:nth-child(2) .query-disclosure__toggle');
	const resultIconState = {
		downloadNameCount: await page.getByRole('button', { name: 'Download' }).count(),
		downloadIcon: await queryDownloadButton.evaluate(button => button.querySelector('svg')?.getAttribute('data-workbench-icon') || null),
		optionsName: await resultOptions.evaluate(button => button.getAttribute('aria-label') || button.innerText.trim()),
		optionsIcon: await resultOptions.evaluate(button => button.querySelector('svg')?.getAttribute('data-workbench-icon') || null)
	};
	console.log('QUERY_RESULT_ICONS_RED', JSON.stringify(resultIconState));
	expect.soft(resultIconState.downloadNameCount, 'query result Download has a computed accessible name').toBe(1);
	expect.soft(resultIconState.downloadIcon).toBe('download');
	expect.soft(resultIconState.optionsName).toBe('Result display options');
	expect.soft(resultIconState.optionsIcon).toBe('chevron');
	await expect(resultOptions).toBeVisible({ timeout: 20_000 });
	await resultOptions.click();
	await expect(resultOptions).toHaveAttribute('aria-expanded', 'true');
	const resultPanelId = await resultOptions.getAttribute('aria-controls');
	await waitForDisclosureMotion(page, `#${resultPanelId}`);
	const resultGeometry = await page.evaluate(id => {
		const trigger = document.querySelector('#query-results .query-result-toolbar__disclosures .query-result-disclosure:nth-child(2) .query-disclosure__toggle').getBoundingClientRect();
		const panel = document.getElementById(id).getBoundingClientRect();
		return { trigger: { x: trigger.x, y: trigger.y, bottom: trigger.bottom },
			panel: { x: panel.x, y: panel.y, right: panel.right, bottom: panel.bottom },
			viewport: { width: innerWidth, height: innerHeight } };
	}, resultPanelId);
	console.log('RESULT_OPTIONS_POPUP_GEOMETRY', JSON.stringify(resultGeometry));
	expect.soft(resultGeometry.panel.x, 'result options remain within viewport horizontally').toBeGreaterThanOrEqual(0);
	expect.soft(resultGeometry.panel.right, 'result options remain within viewport horizontally')
		.toBeLessThanOrEqual(resultGeometry.viewport.width);
	expect.soft(resultGeometry.panel.y, 'result options remain within viewport').toBeGreaterThanOrEqual(0);
	expect.soft(resultGeometry.panel.bottom, 'result options remain within viewport').toBeLessThanOrEqual(resultGeometry.viewport.height);
	expect.soft(Math.abs(resultGeometry.panel.y - resultGeometry.trigger.bottom), 'result options popup stays anchored to its control')
		.toBeLessThan(120);
	await resultOptions.click();
	await page.setViewportSize({ width: 390, height: 844 });
	await resultOptions.click();
	await waitForDisclosureMotion(page, `#${resultPanelId}`);
	const narrowResultGeometry = await page.evaluate(id => {
		const trigger = document.querySelector('#query-results .query-result-toolbar__disclosures .query-result-disclosure:nth-child(2) .query-disclosure__toggle').getBoundingClientRect();
		const panel = document.getElementById(id).getBoundingClientRect();
		return { trigger: { x: trigger.x, y: trigger.y, bottom: trigger.bottom },
			panel: { x: panel.x, y: panel.y, right: panel.right, bottom: panel.bottom },
			viewport: { width: innerWidth, height: innerHeight } };
	}, resultPanelId);
	console.log('NARROW_RESULT_OPTIONS_POPUP_GEOMETRY_RED', JSON.stringify(narrowResultGeometry));
	expect.soft(narrowResultGeometry.panel.x).toBeGreaterThanOrEqual(0);
	expect.soft(narrowResultGeometry.panel.right).toBeLessThanOrEqual(narrowResultGeometry.viewport.width);
	expect.soft(narrowResultGeometry.panel.y).toBeGreaterThanOrEqual(0);
	expect.soft(narrowResultGeometry.panel.bottom).toBeLessThanOrEqual(narrowResultGeometry.viewport.height);
	await resultOptions.click();
	await expect(resultOptions).toHaveAttribute('aria-expanded', 'false');
	await page.emulateMedia({ reducedMotion: 'reduce' });
	await resultOptions.click();
	await waitForDisclosureMotion(page, `#${resultPanelId}`);
	const reducedMotionGeometry = await panelViewportGeometry(page, resultPanelId);
	console.log('REDUCED_MOTION_RESULT_OPTIONS_GEOMETRY', JSON.stringify(reducedMotionGeometry));
	expect.soft(reducedMotionGeometry.bottom, 'reduced-motion result options remain within viewport')
		.toBeLessThanOrEqual(reducedMotionGeometry.viewport.height);
	await resultOptions.click();
	await expect(resultOptions).toHaveAttribute('aria-expanded', 'false');
	await page.evaluate(id => {
		const panel = document.getElementById(id);
		const toggle = document.querySelector('#query-results .query-result-options-toggle');
		workbench.setDisclosureExpanded(toggle, panel, toggle.parentElement, true, false);
	}, resultPanelId);
	const immediateGeometry = await panelViewportGeometry(page, resultPanelId);
	console.log('NON_ANIMATED_RESULT_OPTIONS_GEOMETRY', JSON.stringify(immediateGeometry));
	expect.soft(immediateGeometry.bottom, 'non-animated result options remain within viewport')
		.toBeLessThanOrEqual(immediateGeometry.viewport.height);
	await page.evaluate(id => {
		const panel = document.getElementById(id);
		const toggle = document.querySelector('#query-results .query-result-options-toggle');
		workbench.setDisclosureExpanded(toggle, panel, toggle.parentElement, false, false);
	}, resultPanelId);
	await page.emulateMedia({ reducedMotion: 'no-preference' });
	await page.setViewportSize({ width: 390, height: 480 });
	await resultOptions.click();
	await waitForDisclosureMotion(page, `#${resultPanelId}`);
	const shortViewportGeometry = await page.evaluate(id => {
		const panel = document.getElementById(id);
		panel.scrollTop = panel.scrollHeight;
		const content = panel.querySelector('.workbench-disclosure__content');
		const lastControl = content.lastElementChild.getBoundingClientRect();
		const rect = panel.getBoundingClientRect();
		return { x: rect.x, y: rect.y, right: rect.right, bottom: rect.bottom,
			width: rect.width, height: rect.height, scrollHeight: panel.scrollHeight,
			clientHeight: panel.clientHeight,
			lastControlBottom: lastControl.bottom,
			scrollportBottom: rect.top + parseFloat(getComputedStyle(panel).borderTopWidth) + panel.clientHeight,
			viewport: { width: innerWidth, height: innerHeight } };
	}, resultPanelId);
	console.log('SHORT_VIEWPORT_RESULT_OPTIONS_GEOMETRY', JSON.stringify(shortViewportGeometry));
	expect.soft(shortViewportGeometry.bottom, 'short-viewport result options remain within viewport')
		.toBeLessThanOrEqual(shortViewportGeometry.viewport.height);
	expect.soft(shortViewportGeometry.scrollHeight, 'short-viewport result options remain internally scrollable')
		.toBeGreaterThan(shortViewportGeometry.clientHeight);
	expect.soft(shortViewportGeometry.lastControlBottom, 'short-viewport result options keep the final control reachable')
		.toBeLessThanOrEqual(shortViewportGeometry.scrollportBottom);
	await resultOptions.click();
	await expect(resultOptions).toHaveAttribute('aria-expanded', 'false');
	await page.setViewportSize({ width: 1280, height: 900 });
	const fullscreen = page.locator('#query-results .query-result-toolbar .query-results__fullscreen');
	await fullscreen.click();
	await expect(page.locator('#query-results')).toHaveAttribute('data-fullscreen', 'true');
	await resultOptions.click();
	await waitForDisclosureMotion(page, `#${resultPanelId}`);
	const fullscreenGeometry = await panelViewportGeometry(page, resultPanelId);
	console.log('FULLSCREEN_RESULT_OPTIONS_GEOMETRY', JSON.stringify(fullscreenGeometry));
	expect.soft(fullscreenGeometry.bottom, 'fullscreen result options remain within viewport')
		.toBeLessThanOrEqual(fullscreenGeometry.viewport.height);
	await resultOptions.click();
	await fullscreen.click();
	await expect(page.locator('#query-results')).not.toHaveAttribute('data-fullscreen', 'true');
	expect.soft(pageErrors).toEqual([]);
});
