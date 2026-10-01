// @ts-check
const { test, expect } = require('@playwright/test');

const WORKBENCH_BASE_URL = (process.env.RDF4J_WORKBENCH_BASE_URL ||
	'http://localhost:8080/rdf4j-workbench').replace(/\/+$/, '');

test('navigation shows every group expanded on desktop and marks the active item', async ({ page }) => {
	await page.setViewportSize({ width: 1440, height: 1000 });
	await page.goto(`${WORKBENCH_BASE_URL}/repositories/NONE/repositories`);
	await page.locator('#workbench-page-surface').waitFor({ state: 'attached' });

	const groups = await page.locator('#navigation .workbench-nav-group').evaluateAll(elements => elements.map(group => ({
		id: group.getAttribute('data-workbench-menu-group'),
		isDisclosure: Boolean(group.querySelector('details')),
		label: group.querySelector('.workbench-nav-group__label').textContent.trim(),
		current: Boolean(group.querySelector('[aria-current="page"]')),
		visibleLinks: Array.from(group.querySelectorAll('a, span.disabled')).filter(link => link.getClientRects().length > 0).length
	})));
	expect(groups.every(group => !group.isDisclosure)).toBe(true);
	expect(groups.every(group => group.visibleLinks > 0)).toBe(true);
	expect(groups.find(group => group.current)).toMatchObject({ id: 'repositories', label: 'Server' });
	const connection = page.locator('#navigation .workbench-nav-group[data-workbench-menu-group="repositories"]')
		.getByRole('link', { name: 'Connection' });
	await expect(connection).toBeVisible();
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
				header: bounds('#workbench-contextbar'),
				brand: bounds('#logo'),
				hasThemeControl: document.querySelector('#workbench-theme, .workbench-theme-control') !== null,
				context: bounds('#workbench-repository-switcher'),
				serverLinks: document.querySelectorAll('#navigation a[href$="/repositories/NONE/server"]').length
			};
		});

		expect(geometry.hasThemeControl).toBe(false);
		expect(geometry.overflow).toBeLessThanOrEqual(1);
		// One compact row; the server stays reachable through the menu's Connection item.
		expect(geometry.context.top).toBeGreaterThanOrEqual(geometry.header.top);
		expect(geometry.context.bottom).toBeLessThanOrEqual(geometry.header.bottom);
		expect(geometry.header.height).toBeLessThanOrEqual(60);
		expect(geometry.serverLinks).toBeGreaterThan(0);
		await routePage.close();
	}
});

test('dark navigation indicators stay readable and the mobile logo fits its artwork', async ({ page }) => {
	await page.emulateMedia({ colorScheme: 'dark' });
	for (const width of [390, 320]) {
		const routePage = await page.context().newPage();
		await routePage.setViewportSize({ width, height: 844 });
		await routePage.goto(`${WORKBENCH_BASE_URL}/repositories/NONE/repositories`);
		await routePage.locator('#workbench-page-surface').waitFor({ state: 'attached' });

		await routePage.locator('#workbench-navigation-summary').click();
		const display = await routePage.evaluate(() => {
			const label = document.querySelector('#navigation .workbench-nav-group__label');
			const brand = document.querySelector('#logo');
			const visible = selector => Array.from(brand.querySelectorAll(selector))
				.find(image => image.getClientRects().length > 0);
			const logo = visible('img:not(.product)').getBoundingClientRect();
			const productImage = visible('img.product');
			const product = productImage ? productImage.getBoundingClientRect() : { width: 0 };
			const brandBounds = brand.getBoundingClientRect();
			return {
				labelColor: getComputedStyle(label).color,
				mutedColor: (() => {
					const probe = document.createElement('span');
					probe.style.color = 'var(--workbench-muted)';
					document.body.appendChild(probe);
					const color = getComputedStyle(probe).color;
					probe.remove();
					return color;
				})(),
				brandWidth: brandBounds.width,
				artworkWidth: logo.width + product.width,
				viewport: document.documentElement.clientWidth,
				overflow: document.documentElement.scrollWidth - document.documentElement.clientWidth
			};
		});

		expect(display.labelColor).toBe(display.mutedColor);
		expect(display.brandWidth).toBeLessThanOrEqual(display.artworkWidth + 32);
		expect(display.overflow).toBeLessThanOrEqual(1);
		await routePage.close();
	}
});

test('the server page shows the same menu and server as every other page', async ({ page }) => {
	await page.setViewportSize({ width: 1440, height: 1000 });
	const groupLabels = async (path) => {
		await page.goto(`${WORKBENCH_BASE_URL}${path}`);
		await page.locator('#navigation .workbench-nav-group').first().waitFor({ state: 'attached' });
		return page.locator('#navigation .workbench-nav-group')
			.evaluateAll(groups => groups.map(group => group.getAttribute('data-workbench-menu-label')));
	};
	const repositories = await groupLabels('/repositories/NONE/repositories');
	const server = await groupLabels('/repositories/NONE/server');
	expect(server).toEqual(repositories);
	const serverUrl = WORKBENCH_BASE_URL.replace(/\/rdf4j-workbench$/, '/rdf4j-server');
	await expect(page.locator('#workbench-server-switcher')).toHaveAttribute('title', serverUrl);
});
