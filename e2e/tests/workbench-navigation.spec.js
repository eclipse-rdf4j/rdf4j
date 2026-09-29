// @ts-check
const { test, expect } = require('@playwright/test');

const WORKBENCH_BASE_URL = (process.env.RDF4J_WORKBENCH_BASE_URL ||
	'http://localhost:8080/rdf4j-workbench').replace(/\/+$/, '');

test('navigation opens the active group and keeps other groups collapsed', async ({ page }) => {
	await page.setViewportSize({ width: 1440, height: 1000 });
	await page.goto(`${WORKBENCH_BASE_URL}/repositories/NONE/repositories`);
	await page.locator('#workbench-page-surface').waitFor({ state: 'attached' });

	const groups = page.locator('#navigation .workbench-nav-group');
	const initial = await groups.evaluateAll(elements => elements.map(group => {
		const details = group.querySelector('details');
		return {
			id: group.getAttribute('data-workbench-menu-group'),
			isDisclosure: Boolean(details),
			open: details ? details.open : false,
			current: Boolean(group.querySelector('[aria-current="page"]')),
			text: group.innerText.replace(/\s+/g, ' ').trim()
		};
	}));
	const active = initial.find(group => group.current);
	expect(active).toMatchObject({ id: 'repositories', isDisclosure: true, open: true });
	expect(initial.filter(group => !group.current && group.isDisclosure && group.open)).toEqual([]);

	const serverGroup = page.locator('#navigation .workbench-nav-group[data-workbench-menu-group="server"]');
	await expect(serverGroup.locator('details')).toHaveCount(0);
	await expect(serverGroup.getByRole('link', { name: 'Server' })).toBeVisible();
	await expect(serverGroup).toHaveText('Server');

	const exploreSummary = page.locator(
		'#navigation .workbench-nav-group[data-workbench-menu-group="explore"] details > summary'
	);
	await exploreSummary.focus();
	await exploreSummary.press('Enter');
	await expect.poll(() => page.locator(
		'#navigation .workbench-nav-group[data-workbench-menu-group="explore"] details'
	).evaluate(element => element.open)).toBe(true);
	await expect.poll(() => page.locator(
		'#navigation .workbench-nav-group[data-workbench-menu-group="repositories"] details'
	).evaluate(element => element.open)).toBe(false);
});

test('mobile header omits the theme selector and keeps server context reachable', async ({ page }) => {
	for (const width of [390, 320]) {
		const routePage = await page.context().newPage();
		await routePage.setViewportSize({ width, height: 844 });
		await routePage.goto(`${WORKBENCH_BASE_URL}/repositories/NONE/repositories`);
		await routePage.locator('#workbench-page-surface').waitFor({ state: 'attached' });

		const geometry = await routePage.evaluate(() => {
			const bounds = selector => {
				const rect = document.querySelector(selector).getBoundingClientRect();
				return { top: rect.top, bottom: rect.bottom, height: rect.height };
			};
			return {
				viewport: document.documentElement.clientWidth,
				overflow: document.documentElement.scrollWidth - document.documentElement.clientWidth,
				header: bounds('#header'),
				brand: bounds('#logo'),
				hasThemeControl: document.querySelector('#workbench-theme, .workbench-theme-control') !== null,
				context: bounds('#contentheader')
			};
		});

		expect(geometry.hasThemeControl).toBe(false);
		expect(geometry.overflow).toBeLessThanOrEqual(1);
		expect(geometry.context.top).toBeGreaterThanOrEqual(geometry.brand.bottom - 1);
		expect(geometry.header.height).toBeLessThan(190);
		await routePage.close();
	}
});

test('dark navigation indicators stay readable and the mobile logo plate fits its artwork', async ({ page }) => {
	await page.emulateMedia({ colorScheme: 'dark' });
	for (const width of [390, 320]) {
		const routePage = await page.context().newPage();
		await routePage.setViewportSize({ width, height: 844 });
		await routePage.goto(`${WORKBENCH_BASE_URL}/repositories/NONE/repositories`);
		await routePage.locator('#workbench-page-surface').waitFor({ state: 'attached' });

		const display = await routePage.evaluate(() => {
			const summary = document.querySelector('#navigation .workbench-nav-group__summary');
			const chevronPath = summary.querySelector('.workbench-nav-group__chevron path');
			const brand = document.querySelector('#logo');
			const logo = brand.querySelector('img:not(.product)').getBoundingClientRect();
			const product = brand.querySelector('img.product').getBoundingClientRect();
			const brandBounds = brand.getBoundingClientRect();
			return {
				chevronStroke: getComputedStyle(chevronPath).stroke,
				summaryColor: getComputedStyle(summary).color,
				brandWidth: brandBounds.width,
				artworkWidth: logo.width + product.width,
				viewport: document.documentElement.clientWidth,
				overflow: document.documentElement.scrollWidth - document.documentElement.clientWidth
			};
		});

		expect(display.chevronStroke).toBe(display.summaryColor);
		expect(display.brandWidth).toBeLessThanOrEqual(display.artworkWidth + 32);
		expect(display.overflow).toBeLessThanOrEqual(1);
		await routePage.close();
	}
});
