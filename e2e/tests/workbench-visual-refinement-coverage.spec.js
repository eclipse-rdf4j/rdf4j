// @ts-check
const { test, expect } = require('@playwright/test');
const fs = require('fs');
const path = require('path');
const { evidencePath, evidenceScreenshots } = require('./workbench-test-helpers.js');

const SERVER_BASE_URL = (process.env.RDF4J_SERVER_BASE_URL || 'http://127.0.0.1:8080/rdf4j-server').replace(/\/+$/, '');
const WORKBENCH_BASE_URL = (process.env.RDF4J_WORKBENCH_BASE_URL || 'http://127.0.0.1:8080/rdf4j-workbench').replace(/\/+$/, '');
// Screenshots go to WORKBENCH_VISUAL_REFINEMENT_DIRECTORY when it is set, otherwise to each test's output directory;
// the coverage report is written only to that directory (an afterAll hook has no test output directory).
const ARTIFACT_DIRECTORY = process.env.WORKBENCH_VISUAL_REFINEMENT_DIRECTORY || '';
const SCREENSHOT_DIRECTORY = ARTIFACT_DIRECTORY ? path.join(ARTIFACT_DIRECTORY, 'final') : '';
const REPOSITORY_ID = `workbench-visual-${process.pid}-${Date.now()}`;
const REPOSITORY_URL = `${SERVER_BASE_URL}/repositories/${REPOSITORY_ID}`;

const BUILT_IN_ROUTES = [
	['server', 'repositories/NONE/server'],
	['repositories', 'repositories/NONE/repositories'],
	['create', 'repositories/NONE/create?type=memory'],
	['delete', 'repositories/NONE/delete'],
	['summary', `repositories/${REPOSITORY_ID}/summary`],
	['namespaces', `repositories/${REPOSITORY_ID}/namespaces`],
	['contexts', `repositories/${REPOSITORY_ID}/contexts`],
	['types', `repositories/${REPOSITORY_ID}/types`],
	['explore', `repositories/${REPOSITORY_ID}/explore`],
	['query', `repositories/${REPOSITORY_ID}/query`],
	['saved-queries', `repositories/${REPOSITORY_ID}/saved-queries`],
	['export', `repositories/${REPOSITORY_ID}/export`],
	['update', `repositories/${REPOSITORY_ID}/update`],
	['add', `repositories/${REPOSITORY_ID}/add`],
	['remove', `repositories/${REPOSITORY_ID}/remove`],
	['clear', `repositories/${REPOSITORY_ID}/clear`],
	['information', `repositories/${REPOSITORY_ID}/information`]
];

const CREATION_TYPES = [
	'memory',
	'memory-lucene',
	'memory-rdfs',
	'memory-rdfs-dt',
	'memory-rdfs-lucene',
	'memory-customrule',
	'memory-shacl',
	'native',
	'native-lucene',
	'native-rdfs',
	'native-rdfs-dt',
	'native-rdfs-lucene',
	'native-customrule',
	'native-shacl',
	'remote',
	'sparql',
	'federate',
	'lmdb'
];

const ALL_ROUTE_WIDTHS = [320, 390, 768, 1024, 1440, 1920];
const BOUNDARY_WIDTHS = [359, 360, 479, 480, 599, 600, 601, 899, 900, 901];
const BOUNDARY_ROUTES = new Set(['repositories', 'create', 'query', 'add']);
const coverageRows = [];
const layoutViolations = [];
const browserErrors = [];
let configuredNavigationLinks = [];

test.beforeAll(async ({ request }) => {
	const created = await request.put(REPOSITORY_URL, {
		headers: { 'Content-Type': 'text/turtle' },
		data: repositoryConfiguration(REPOSITORY_ID)
	});
	expect([200, 201, 204]).toContain(created.status());

	const statements = [
		'<urn:visual:alice> <urn:visual:name> "Alice" .',
		'<urn:visual:alice> <urn:visual:type> <urn:visual:Person> <urn:visual:people> .',
		'<urn:visual:bob> <urn:visual:name> "Bob" .',
		'<urn:visual:bob> <urn:visual:type> <urn:visual:Person> <urn:visual:people> .',
		'<urn:visual:carol> <urn:visual:name> "Carol" <urn:visual:people> .',
		'<urn:visual:graph> <urn:visual:kind> "named" <urn:visual:metadata> .'
	].join('\n');
	const loaded = await request.post(`${REPOSITORY_URL}/statements`, {
		headers: { 'Content-Type': 'application/n-quads' },
		data: statements
	});
	expect([200, 201, 204]).toContain(loaded.status());
});

test.afterAll(async ({ request }) => {
	if (coverageRows.length && ARTIFACT_DIRECTORY) {
		const routeCount = coverageRows.filter(row => row.kind === 'route').length;
		const creationCount = coverageRows.filter(row => row.kind === 'creation').length;
		const lines = [
			'# Workbench visual refinement coverage',
			'',
			`Captured ${coverageRows.length} fresh rendered states on ${new Date().toISOString()}.`,
			`Built-in routes: ${new Set(coverageRows.filter(row => row.kind === 'route').map(row => row.route)).size}/17; route viewport/theme captures: ${routeCount}.`,
			`Repository creation variants: ${new Set(coverageRows.filter(row => row.kind === 'creation').map(row => row.route)).size}/18; variant captures: ${creationCount}.`,
			`Configured navigation links observed: ${JSON.stringify(configuredNavigationLinks)}.`,
			`Layout findings: ${JSON.stringify(layoutViolations)}.`,
			`Browser errors observed: ${JSON.stringify(browserErrors)}.`,
			'',
			'Every screenshot is fresh output from the isolated task server. `overflow` is document scroll width minus viewport width; boundary captures are performed on the shared shell and the most-used form/editor families.',
			'',
			'| Kind | Route/state | Theme | Width | Status | Overflow | Screenshot |',
			'| --- | --- | --- | ---: | ---: | ---: | --- |',
			...coverageRows.map(row => `| ${row.kind} | ${row.route} / ${row.state} | ${row.theme} | ${row.width} | ${row.status} | ${row.overflow} | ${row.screenshot ? `[image](${path.relative(ARTIFACT_DIRECTORY, row.screenshot)})` : 'not saved'} |`)
		];
		fs.mkdirSync(ARTIFACT_DIRECTORY, { recursive: true });
		fs.writeFileSync(path.join(ARTIFACT_DIRECTORY, 'coverage-final.md'), `${lines.join('\n')}\n`);
	}

	const removed = await request.delete(REPOSITORY_URL);
	expect([200, 204, 404]).toContain(removed.status());
});

test('captures all built-in Workbench route layouts across themes and viewport widths', async ({ page }) => {
	test.setTimeout(15 * 60 * 1000);
	page.on('pageerror', error => browserErrors.push(error.message));
	page.on('console', message => {
		if (message.type() === 'error') {
			browserErrors.push(message.text());
		}
	});

	for (const theme of ['light', 'dark']) {
		await setTheme(page, theme);
		const widths = theme === 'light' ? ALL_ROUTE_WIDTHS : [390, 1440];
		for (const [route, pathSuffix] of BUILT_IN_ROUTES) {
			const routeWidths = [...widths];
			if (theme === 'light' && BOUNDARY_ROUTES.has(route)) {
				routeWidths.push(...BOUNDARY_WIDTHS);
			}
			for (const width of routeWidths) {
				await page.setViewportSize({ width, height: width > 900 ? 1000 : 900 });
				await captureRoute(page, route, pathSuffix, theme, width, 'default');
			}
		}
	}

	await setTheme(page, 'light');
	await page.setViewportSize({ width: 1440, height: 1000 });
	await navigate(page, 'repositories/NONE/repositories');
	configuredNavigationLinks = await page.locator('#navigation a[href]').evaluateAll(anchors => anchors.map(anchor => ({
		label: anchor.textContent.trim(),
		href: anchor.getAttribute('href')
	})));
	await captureCurrentPage(page, 'repositories', 'repositories/NONE/repositories', 'light', 1440,
		'configured-links');
	expect(layoutViolations, 'route pages must stay within the viewport without horizontal overflow').toEqual([]);
});

test('captures every repository creation form and its expanded advanced state', async ({ page }) => {
	test.setTimeout(15 * 60 * 1000);
	page.on('pageerror', error => browserErrors.push(error.message));
	page.on('console', message => {
		if (message.type() === 'error') {
			browserErrors.push(message.text());
		}
	});

	const startingViolationCount = layoutViolations.length;
	for (const type of CREATION_TYPES) {
		for (const theme of ['light', 'dark']) {
			await setTheme(page, theme);
			const widths = theme === 'light' ? ALL_ROUTE_WIDTHS : [390, 1440];
			for (const width of widths) {
				await page.setViewportSize({ width, height: width > 900 ? 1000 : 900 });
				await captureRoute(page, `create:${type}`, `repositories/NONE/create?type=${type}`, theme, width, 'advanced-closed');
			}
		}

		for (const width of [390, 1440]) {
			await setTheme(page, 'light');
			await page.setViewportSize({ width, height: width > 900 ? 1000 : 900 });
			await navigate(page, `repositories/NONE/create?type=${type}`);
			const advanced = page.locator('.workbench-advanced[data-workbench-detail-disclosure="true"]');
			const hasAdvanced = await advanced.count() > 0;
			if (hasAdvanced) {
				const toggle = advanced.locator(':scope > .workbench-disclosure__toggle');
				await toggle.press('Enter');
				await expect(toggle).toHaveAttribute('aria-expanded', 'true');
			}
			await captureCurrentPage(page, `create:${type}`, `repositories/NONE/create?type=${type}`, 'light', width,
				hasAdvanced ? 'advanced-open' : 'advanced-not-configurable');
		}
	}
	expect(layoutViolations.slice(startingViolationCount), 'creation forms must stay within the viewport without horizontal overflow')
		.toEqual([]);
});

async function setTheme(page, theme) {
	await page.emulateMedia({ colorScheme: theme });
	await navigate(page, 'repositories/NONE/repositories');
	await page.evaluate(theme => window.RDF4JWorkbenchTheme.setPreference(theme), theme);
	await expect(page.locator('html')).toHaveAttribute('data-theme', theme);
}

async function captureRoute(page, route, pathSuffix, theme, width, state) {
	const response = await navigate(page, pathSuffix);
	expect(response?.status(), `${route} should render at ${width}px in ${theme}`).toBeLessThan(400);
	return captureCurrentPage(page, route, pathSuffix, theme, width, state);
}

async function navigate(page, pathSuffix) {
	const response = await page.goto(`${WORKBENCH_BASE_URL}/${pathSuffix}`, { waitUntil: 'domcontentloaded' });
	await page.locator('#workbench-page-surface').waitFor({ state: 'visible', timeout: 15000 });
	await page.evaluate(() => document.fonts.ready);
	return response;
}

async function captureCurrentPage(page, route, pathSuffix, theme, width, state) {
	const metrics = await page.evaluate(() => {
		const surface = document.querySelector('#workbench-page-surface');
		const bounds = surface.getBoundingClientRect();
		const customProperties = getComputedStyle(document.documentElement);
		return {
			title: document.title,
			actualTheme: document.documentElement.getAttribute('data-theme'),
			viewport: document.documentElement.clientWidth,
			overflow: Math.max(0, document.documentElement.scrollWidth - document.documentElement.clientWidth),
			surfaceLeft: Math.round(bounds.left),
			surfaceRight: Math.round(bounds.right),
			surfaceWidth: Math.round(bounds.width),
			primaryActions: Array.from(surface.querySelectorAll('button[type="submit"], input[type="submit"]'))
				.filter(element => element.getClientRects().length).length,
			fields: Array.from(surface.querySelectorAll('input:not([type="hidden"]), select, textarea'))
				.filter(element => element.getClientRects().length).length,
			canvas: customProperties.getPropertyValue('--workbench-canvas').trim(),
			surfaceColor: customProperties.getPropertyValue('--workbench-surface').trim()
		};
	});
	// The screenshot is evidence for the coverage report; the checks below use the measured metrics.
	let screenshot = null;
	if (evidenceScreenshots()) {
		const fileName = `${safeName(route)}__${safeName(state)}__${theme}__${width}.png`;
		screenshot = evidencePath(SCREENSHOT_DIRECTORY, 'routes', fileName);
		await page.screenshot({ path: screenshot, fullPage: true, animations: 'disabled' });
	}
	if (metrics.actualTheme !== theme) {
		layoutViolations.push(`${route}/${state} at ${width}px requested ${theme}, rendered ${metrics.actualTheme}`);
	}
	if (metrics.surfaceLeft < -1 || metrics.surfaceRight > metrics.viewport + 1 || metrics.overflow > 1) {
		layoutViolations.push(`${route}/${state} ${theme} ${width}px ${JSON.stringify(metrics)}`);
	}
	coverageRows.push({
		kind: route.startsWith('create:') ? 'creation' : 'route',
		route,
		state,
		theme,
		width,
		status: metrics.title,
		overflow: metrics.overflow,
		screenshot
	});
	return metrics;
}

function safeName(value) {
	return value.replace(/[^a-zA-Z0-9-]+/g, '-').replace(/^-+|-+$/g, '');
}

function repositoryConfiguration(repositoryId) {
	return `@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix config: <tag:rdf4j.org,2023:config/>.
[] a config:Repository ; config:rep.id "${repositoryId}" ; rdfs:label "Workbench visual refinement fixture" ;
   config:rep.impl [ config:rep.type "openrdf:SailRepository" ;
      config:sail.impl [ config:sail.type "openrdf:MemoryStore" ] ].`;
}
