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
const { serverBaseUrl, waitForRoute, workbenchBaseUrl } = require('./workbench-test-helpers');

const SERVER_BASE_URL = serverBaseUrl();
const WORKBENCH_BASE_URL = workbenchBaseUrl();
const REPOSITORY_ID = `initial-model-${process.pid}-${Date.now().toString(36)}`;
const REPOSITORY_URL = `${SERVER_BASE_URL}/repositories/${REPOSITORY_ID}`;
let repositoryCreated = false;

test.setTimeout(90_000);

test.beforeAll(async ({ request }) => {
	const existing = await request.get(REPOSITORY_URL);
	if (existing.status() !== 400 && existing.status() !== 404) {
		throw new Error(`Refusing to use non-absent inline-model fixture ${REPOSITORY_ID}: ${existing.status()}`);
	}
	const created = await request.put(REPOSITORY_URL, {
		headers: { 'Content-Type': 'text/turtle' },
		data: `@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix config: <tag:rdf4j.org,2023:config/>.
[] a config:Repository ; config:rep.id "${REPOSITORY_ID}" ; rdfs:label "Workbench inline model regression fixture" ;
   config:rep.impl [ config:rep.type "openrdf:SailRepository" ;
      config:sail.impl [ config:sail.type "openrdf:MemoryStore" ] ].`
	});
	expect([200, 201, 204]).toContain(created.status());
	repositoryCreated = true;
});

test.afterAll(async ({ request }) => {
	if (repositoryCreated) {
		const deleted = await request.delete(REPOSITORY_URL);
		expect([200, 204, 400, 404]).toContain(deleted.status());
	}
});

// The router sends forms from the page (plan task M10.1); with ?router=off (M8.1) the browser posts them itself, which
// keeps the server's inline page model covered.
test('with the router off, native Add validation renders its inline page model once', async ({ page }) => {
	page.setDefaultTimeout(10_000);
	page.setDefaultNavigationTimeout(15_000);
	await page.setViewportSize({ width: 1280, height: 900 });
	const pageErrors = [];
	const addRequests = [];
	const addUrl = `${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/add`;
	const addPath = new URL(addUrl).pathname;
	page.on('pageerror', error => pageErrors.push(error.message));
	page.on('request', requestEvent => {
		if (new URL(requestEvent.url()).pathname === addPath) {
			addRequests.push({
				method: requestEvent.method(),
				accept: requestEvent.headers().accept || ''
			});
		}
	});

	await page.goto(`${addUrl}?router=off`, { waitUntil: 'domcontentloaded' });
	await expect(page.locator('#workbench-app')).toHaveAttribute('data-workbench-view', 'add');
	await expect(page.locator('#add-source-tabs')).toBeVisible();
	await page.locator('#add-import-settings-toggle').click();
	const isolation = page.locator('select[name="transaction-setting__org.eclipse.rdf4j.common.transaction.IsolationLevel"]');
	const isolationOptions = await isolation.locator('option').evaluateAll(options => options.map(option => ({
		value: option.value,
		label: option.textContent.trim()
	})));
	expect(isolationOptions.length, 'the Memory repository reports its supported isolation options').toBeGreaterThan(1);
	expect(isolationOptions.slice(1).every(option => option.value && option.label && option.label !== option.value),
		'Add should render human-readable labels for supported isolation values').toBe(true);
	// The target graph starts empty and editable; empty keeps the graphs named in the data (plan task M5.5).
	expect(await page.locator('#context').isDisabled()).toBe(false);
	expect(await page.locator('#context').inputValue()).toBe('');
	expect(await page.locator('#context-help').textContent()).toContain('keep the graphs named in the data');

	const dataGetCountBeforeSubmit = addRequests.filter(request => request.method === 'GET'
		&& request.accept.includes('application/vnd.rdf4j.workbench+ndjson')).length;
	expect(dataGetCountBeforeSubmit, 'the normal Add shell fetches its page model once').toBe(1);
	await page.locator('label[for="source-url"]').click();
	await expect(page.locator('#source-url')).toBeChecked();
	await page.locator('#url').fill('not a valid URL');
	const [validationResponse] = await Promise.all([
		page.waitForNavigation({ waitUntil: 'domcontentloaded', timeout: 15_000 }),
		page.locator('form[enctype="multipart/form-data"] input[type="submit"][value="Upload"]').click()
	]);
	expect(validationResponse, 'the native validation submit should return its shell response').not.toBeNull();
	expect(validationResponse.url()).toBe(addUrl);
	expect(validationResponse.request().method()).toBe('POST');
	const responseHtml = await validationResponse.text();
	await expect(page.locator('#workbench-app')).toHaveAttribute('data-workbench-view', 'add');
	await expect(page.locator('#add-source-tabs')).toBeVisible();
	// Page errors render as error callouts (plan workbench-app-shell-and-critique-fixes-20260930, M1.7).
	const validationError = page.locator('#workbench-app .workbench-callout--error[role="alert"]').first();
	await expect(validationError).toBeVisible();
	await expect(validationError).not.toHaveText('');

	const postCount = addRequests.filter(request => request.method === 'POST').length;
	const dataGetCountAfterSubmit = addRequests.filter(request => request.method === 'GET'
		&& request.accept.includes('application/vnd.rdf4j.workbench+ndjson')).length;
	console.log('INLINE_ADD_MODEL_RESULT', JSON.stringify({
		status: validationResponse.status(),
		contentType: validationResponse.headers()['content-type'] || null,
		inlineModel: responseHtml.includes('data-workbench-initial-model='),
		postCount,
		dataGetCountBeforeSubmit,
		dataGetCountAfterSubmit,
		pageErrors
	}));
	expect(validationResponse.status()).toBe(200);
	expect(responseHtml).toContain('data-workbench-initial-model=');
	expect(postCount, 'the rejected submission must execute only once').toBe(1);
	expect(dataGetCountAfterSubmit, 'the inline page model replaces a follow-up route GET').toBe(dataGetCountBeforeSubmit);
	expect(pageErrors).toEqual([]);
});

test('Add validation stays in the page and shows its error once', async ({ page }) => {
	await page.setViewportSize({ width: 1280, height: 900 });
	const pageErrors = [];
	const documents = [];
	const posts = [];
	const addUrl = `${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/add`;
	page.on('pageerror', error => pageErrors.push(error.message));
	page.on('request', requestEvent => {
		if (requestEvent.resourceType() === 'document' && requestEvent.frame() === page.mainFrame()) {
			documents.push(requestEvent.url());
		}
		if (requestEvent.method() === 'POST' && new URL(requestEvent.url()).pathname === new URL(addUrl).pathname) {
			posts.push(requestEvent.headers().accept || '');
		}
	});

	await page.goto(addUrl, { waitUntil: 'domcontentloaded' });
	await waitForRoute(page, 'add');
	await page.locator('label[for="source-url"]').click();
	await expect(page.locator('#source-url')).toBeChecked();
	await page.locator('#url').fill('not a valid URL');
	await page.locator('form[enctype="multipart/form-data"] input[type="submit"][value="Upload"]').click();

	const validationError = page.locator('#workbench-app .workbench-callout--error[role="alert"]').first();
	await expect(validationError).toBeVisible();
	await expect(validationError).not.toHaveText('');
	await waitForRoute(page, 'add');
	await expect(page).toHaveURL(addUrl);
	await expect(page.locator('#add-source-tabs')).toBeVisible();
	expect(posts, 'the rejected submission is sent once, asking for the page model').toHaveLength(1);
	expect(posts[0]).toContain('application/vnd.rdf4j.workbench+ndjson');
	expect(documents, 'the answer is shown without loading a document').toEqual([addUrl]);
	expect(pageErrors).toEqual([]);
});
