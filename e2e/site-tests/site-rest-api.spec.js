const { test, expect } = require('@playwright/test');
const fs = require('node:fs');
const http = require('node:http');
const path = require('node:path');

const siteRoot = path.resolve(
	process.env.HUGO_TEST_OUTPUT_DIR || path.resolve(__dirname, '../../site/public')
);
const rendererUrl = 'https://cdn.redoc.ly/redoc/v2.5.3/bundles/redoc.standalone.js';
const specHref = './rdf4j-openapi.yaml';
const mimeTypes = {
	'.css': 'text/css; charset=utf-8',
	'.html': 'text/html; charset=utf-8',
	'.js': 'text/javascript; charset=utf-8',
	'.json': 'application/json; charset=utf-8',
	'.svg': 'image/svg+xml',
	'.yaml': 'application/yaml; charset=utf-8',
};

let server;
let baseUrl;

test.beforeAll(async () => {
	const index = path.join(siteRoot, 'documentation/reference/rest-api/index.html');
	if (!fs.existsSync(index)) {
		throw new Error(`Built REST API page not found at ${index}; build the Hugo site first.`);
	}

	server = http.createServer((request, response) => {
		let pathname;
		try {
			pathname = decodeURIComponent(new URL(request.url, 'http://localhost').pathname);
		} catch {
			response.writeHead(400).end();
			return;
		}

		let filename = path.resolve(siteRoot, `.${pathname}`);
		if (filename !== siteRoot && !filename.startsWith(`${siteRoot}${path.sep}`)) {
			response.writeHead(403).end();
			return;
		}
		try {
			if (fs.statSync(filename).isDirectory()) {
				filename = path.join(filename, 'index.html');
			}
		} catch {
			response.writeHead(404).end();
			return;
		}

		const contentType = mimeTypes[path.extname(filename).toLowerCase()] || 'application/octet-stream';
		response.writeHead(200, { 'Content-Type': contentType });
		fs.createReadStream(filename).on('error', () => response.destroy()).pipe(response);
	});

	await new Promise((resolve, reject) => {
		server.once('error', reject);
		server.listen(0, '127.0.0.1', resolve);
	});
	baseUrl = `http://127.0.0.1:${server.address().port}`;
});

test.afterAll(async () => {
	if (server?.listening) {
		await new Promise((resolve, reject) => server.close((error) => error ? reject(error) : resolve()));
	}
});

test('built REST API documentation renders and opens an operation', async ({ page }) => {
	const pageErrors = [];
	const rendererStatuses = [];
	page.on('pageerror', (error) => pageErrors.push(error.message));
	page.on('response', (rendererResponse) => {
		if (rendererResponse.url() === rendererUrl) {
			rendererStatuses.push(rendererResponse.status());
		}
	});

	const response = await page.goto(`${baseUrl}/documentation/reference/rest-api/`, {
		waitUntil: 'networkidle',
	});
	expect(response?.status()).toBe(200);
	expect(rendererStatuses, 'The pinned Redoc asset should load successfully').toEqual([200]);
	expect(pageErrors, 'The REST API page raised a JavaScript page error').toEqual([]);
	await expect(page.getByRole('link', { name: 'download the OpenAPI specification' })).toBeVisible();

	await page.getByRole('menuitem', { name: 'Repositories', exact: true }).click();
	const operation = page.getByRole('menuitem', { name: 'Retrieve a list of Repositories' });
	await expect(operation, 'Redoc should render operation navigation').toBeVisible();

	await operation.click();
	await expect(page).toHaveURL(/\/paths\/~1repositories\/get$/);
	await expect(page.getByRole('heading', { name: 'Retrieve a list of Repositories' })).toBeInViewport();
	expect(pageErrors, 'Opening an operation raised a JavaScript page error').toEqual([]);
	if (process.env.REST_API_SCREENSHOT_PATH) {
		await page.screenshot({ path: process.env.REST_API_SCREENSHOT_PATH, fullPage: false });
	}
});

test('OpenAPI download remains available if Redoc cannot load', async ({ page }) => {
	await page.setViewportSize({ width: 375, height: 812 });
	await page.route(rendererUrl, (route) => route.abort());

	const response = await page.goto(`${baseUrl}/documentation/reference/rest-api/`, {
		waitUntil: 'networkidle',
	});
	expect(response?.status()).toBe(200);

	const fallback = page.getByRole('link', { name: 'download the OpenAPI specification' });
	await expect(fallback).toBeVisible();
	await expect(fallback).toHaveAttribute('href', specHref);
	await expect(page.locator('redoc .redoc-wrap')).toHaveCount(0);

	const specUrl = new URL(specHref, page.url()).toString();
	const specResponse = await page.request.get(specUrl);
	expect(specResponse.status()).toBe(200);
	expect((await specResponse.text()).startsWith('openapi: "3.0.1"')).toBe(true);
});

test('OpenAPI download remains available with JavaScript disabled', async ({ browser }) => {
	const context = await browser.newContext({
		javaScriptEnabled: false,
		viewport: { width: 375, height: 812 },
	});
	try {
		const page = await context.newPage();
		const response = await page.goto(`${baseUrl}/documentation/reference/rest-api/`, {
			waitUntil: 'load',
		});
		expect(response?.status()).toBe(200);

		const fallback = page.getByRole('link', { name: 'download the OpenAPI specification' });
		await expect(fallback).toBeVisible();
		await expect(fallback).toHaveAttribute('href', specHref);

		const specUrl = new URL(specHref, page.url()).toString();
		const specResponse = await page.request.get(specUrl);
		expect(specResponse.status()).toBe(200);
		expect((await specResponse.text()).startsWith('openapi: "3.0.1"')).toBe(true);
	} finally {
		await context.close();
	}
});
