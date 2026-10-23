// @ts-check
/*
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Distribution License v1.0.
 * SPDX-License-Identifier: BSD-3-Clause
 */
const { test, expect } = require('@playwright/test');
const { waitForRoute } = require('./workbench-test-helpers');

const SERVER_BASE_URL = (process.env.RDF4J_SERVER_BASE_URL
	|| 'http://127.0.0.1:8080/rdf4j-server').replace(/\/+$/, '');
const WORKBENCH_BASE_URL = (process.env.RDF4J_WORKBENCH_BASE_URL
	|| 'http://127.0.0.1:8080/rdf4j-workbench').replace(/\/+$/, '');

for (const variant of [
	{ name: 'default', experimental: null, neighbors: false },
	{ name: 'experimental-only', experimental: true, neighbors: false },
	{ name: 'neighbors-only', experimental: false, neighbors: true },
	{ name: 'all-enabled', experimental: true, neighbors: true }
]) {
	test(`LMDB experimental optimizations persist independently: ${variant.name}`, async ({ page, request }) => {
		const repositoryId = `lmdb-experimental-${variant.name}-${process.pid}-${Date.now()}`;
		const repositoryUrl = `${SERVER_BASE_URL}/repositories/${repositoryId}`;
		const pageErrors = [];
		page.on('pageerror', error => pageErrors.push(error.message));
		try {
			await page.goto(`${WORKBENCH_BASE_URL}/repositories/NONE/create`, { waitUntil: 'networkidle' });
			await page.locator('#type').selectOption('lmdb');
			await page.locator('form[action="create"] input[name="next"]').click();
			await waitForRoute(page, 'create', { url: /\/create\?/ });
			const form = page.locator('form[action="create"]');
			await form.locator('[data-field-role="repository-id"]').fill(repositoryId);
			await form.locator('[data-field-role="repository-title"]').fill(`LMDB ${variant.name} regression`);
			const toggle = form.locator('#create-advanced-toggle');
			await expect(toggle).toHaveAttribute('aria-expanded', 'false');
			await toggle.click();
			await expect(toggle).toHaveAttribute('aria-expanded', 'true');
			const experimental = form.locator('input[name="Experimental Query Optimizations"]');
			await expect(experimental).toHaveCount(2);
			await expect(experimental.first().locator('..')).toBeVisible();
			await expect(form.locator('#create-advanced-panel')).toContainText('Experimental Query Optimizations');
			await expect(form.locator('input[name="Experimental Query Optimizations"][value="false"]')).toBeChecked();
			if (variant.experimental !== null) {
				await form.locator(`input[name="Experimental Query Optimizations"][value="${variant.experimental}"]`).check();
			}
			for (const name of ['Native query evaluation enabled', 'Direct adjacency index enabled',
				'Compressed value overlay enabled']) {
				await form.locator(`input[name="${name}"][value="${variant.neighbors}"]`).check();
			}
			await toggle.click();
			await expect(toggle).toHaveAttribute('aria-expanded', 'false');
			await toggle.click();
			await expect(form.locator(`input[name="Experimental Query Optimizations"][value="${!!variant.experimental}"]`))
				.toBeChecked();
			if (variant.name === 'experimental-only') {
				const firstField = form.locator('.workbench-advanced__field:has(input[name="Experimental Query Optimizations"])');
				const lastField = form.locator('.workbench-advanced__field:has(input[name="Compressed value overlay enabled"])');
				await lastField.scrollIntoViewIfNeeded();
				const first = await firstField.boundingBox();
				const last = await lastField.boundingBox();
				await page.screenshot({
					path: test.info().outputPath('advanced-settings.png'), animations: 'disabled',
					clip: { x: first.x, y: first.y, width: first.width, height: last.y + last.height - first.y }
				});
			}
			const submission = page.waitForRequest(event => event.method() === 'POST'
				&& new URL(event.url()).pathname.endsWith('/repositories/NONE/create'));
			await form.locator('#create').click();
			const submitted = new URLSearchParams((await submission).postData());
			expect(submitted.get('Experimental Query Optimizations')).toBe(String(!!variant.experimental));
			await waitForRoute(page, 'summary', { url: new RegExp(`/repositories/${repositoryId}/summary`) });

			const response = await request.get(`${repositoryUrl}/config`, {
				headers: { Accept: 'text/turtle' }
			});
			expect(response.status()).toBe(200);
			const config = await response.text();
			if (variant.experimental) {
				expect(config).toMatch(/sail\.experimentalQueryOptimizations\s+true/);
			} else {
				expect(config).not.toMatch(/experimentalQueryOptimizations\s+true/);
			}
			for (const property of ['nativeEvaluationEnabled', 'directAdjacencyEnabled', 'valueOverlayEnabled']) {
				expect(config).toMatch(new RegExp(`${property}\\s+${variant.neighbors}`));
			}
			expect(pageErrors).toEqual([]);
		} finally {
			const removed = await request.delete(repositoryUrl);
			expect([200, 204, 400, 404]).toContain(removed.status());
		}
	});
}
