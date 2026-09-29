// @ts-check
const { test, expect } = require('@playwright/test');
const fs = require('node:fs');
const path = require('node:path');

const WORKBENCH_BASE_URL = (process.env.RDF4J_WORKBENCH_BASE_URL
	|| 'http://127.0.0.1:8091/rdf4j-workbench').replace(/\/+$/, '');
const SCREENSHOT_DIRECTORY = path.resolve(__dirname, '../../output/playwright');

test('repository landing shell owns full-width layout and loads server navigation', async ({ page }) => {
	fs.mkdirSync(SCREENSHOT_DIRECTORY, { recursive: true });
	const observations = {};
	for (const [name, width] of [['desktop', 1280], ['mobile', 390]]) {
		await page.setViewportSize({ width, height: 720 });
		await page.goto(`${WORKBENCH_BASE_URL}/repositories/NONE/repositories`, { waitUntil: 'networkidle' });
		await page.locator('#workbench-page-surface').waitFor({ state: 'attached' });
		observations[name] = await page.evaluate(() => {
			const rect = selector => {
				const element = document.querySelector(selector);
				if (!element) return null;
				const box = element.getBoundingClientRect();
				return { x: box.x, y: box.y, width: box.width, height: box.height };
			};
			const server = document.querySelector('#contentheader tr:first-child td:nth-child(2)');
			const createLink = Array.from(document.querySelectorAll('#navigation a[data-workbench-nav-href]'))
				.find(anchor => new URL(anchor.href).pathname.replace(/\/+$/, '').endsWith('/create'));
			return {
				viewport: document.documentElement.clientWidth,
				documentWidth: document.documentElement.scrollWidth,
				app: rect('#workbench-app'),
				header: rect('#header'),
				navigation: rect('#navigation'),
				content: rect('#content'),
				groups: document.querySelectorAll('#navigation .workbench-nav-group').length,
				createHref: createLink?.href || null,
				server: server?.textContent?.trim() || null
			};
		});
		await page.screenshot({ path: path.join(SCREENSHOT_DIRECTORY, `workbench-page-shell-grid-${name}.png`), fullPage: true });
	}
	console.log(`Workbench landing shell geometry: ${JSON.stringify(observations)}`);

	expect(observations.desktop.content?.width, 'desktop content should occupy the page area beside navigation').toBeGreaterThan(800);
	expect(observations.desktop.groups, 'server-provided navigation groups should render').toBeGreaterThan(0);
	expect(observations.desktop.createHref, 'the Create repository route should be reachable from navigation').toContain('/create');
	expect(observations.desktop.server, 'server identity should come from the linked information model').not.toBe('None');
	expect(observations.mobile.content?.width, 'mobile content should use the viewport width').toBeGreaterThan(300);
	expect(observations.mobile.groups, 'navigation groups should remain available on mobile').toBeGreaterThan(0);
	expect(observations.mobile.createHref, 'Create should remain reachable from the mobile menu').toContain('/create');
	expect(observations.mobile.documentWidth - observations.mobile.viewport, 'mobile shell must not overflow horizontally').toBeLessThanOrEqual(1);
});
