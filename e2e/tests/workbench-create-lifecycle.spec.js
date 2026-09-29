// @ts-check
const { test, expect } = require('@playwright/test');

const SERVER_BASE_URL = (process.env.RDF4J_SERVER_BASE_URL
	|| 'http://127.0.0.1:8091/rdf4j-server').replace(/\/+$/, '');
const WORKBENCH_BASE_URL = (process.env.RDF4J_WORKBENCH_BASE_URL
	|| 'http://127.0.0.1:8091/rdf4j-workbench').replace(/\/+$/, '');
const REPOSITORY_ID = `create-lifecycle-${process.pid}-${Date.now()}`;
const REPOSITORY_URL = `${SERVER_BASE_URL}/repositories/${REPOSITORY_ID}`;
let cleanupArmed = false;

test.afterEach(async ({ request }) => {
	if (!cleanupArmed) {
		return;
	}
	const response = await request.delete(REPOSITORY_URL);
	expect([200, 204, 400, 404]).toContain(response.status());
});

test('creates a Memory repository after Next without a page error', async ({ page, request }) => {
	const existing = await request.get(REPOSITORY_URL);
	expect([400, 404], 'the unique fixture id must not already exist').toContain(existing.status());
	cleanupArmed = true;
	const pageErrors = [];
	const createRequests = [];
	page.on('pageerror', error => pageErrors.push(error.message));
	const createPath = new URL(`${WORKBENCH_BASE_URL}/repositories/NONE/create`).pathname;
	page.on('request', requestEvent => {
		if (new URL(requestEvent.url()).pathname === createPath) {
			createRequests.push({
				method: requestEvent.method(),
				url: requestEvent.url(),
				accept: requestEvent.headers().accept || '',
				body: requestEvent.postData() || ''
			});
		}
	});

	await page.setViewportSize({ width: 1440, height: 900 });
	await page.goto(`${WORKBENCH_BASE_URL}/repositories/NONE/create`, { waitUntil: 'networkidle' });
	await expect(page.locator('#create-type-form #type')).toBeVisible({ timeout: 15_000 });
	await page.locator('#type').selectOption('memory');
	await Promise.all([
		page.waitForNavigation({ waitUntil: 'networkidle' }),
		page.locator('form[action="create"] input[type="submit"][name="next"]').click()
	]);

	const form = page.locator('form[action="create"]');
	await expect(form).toBeVisible();
	const repositoryId = form.locator('[data-field-role="repository-id"]');
	const repositoryTitle = form.locator('[data-field-role="repository-title"]');
	await expect(repositoryId).toBeVisible();
	await repositoryId.fill(REPOSITORY_ID);
	await repositoryTitle.fill('Workbench create lifecycle regression fixture');
	await form.locator('#create').click();

	await expect(page).toHaveURL(new RegExp(`/repositories/${REPOSITORY_ID}/summary(?:[?#]|$)`));
	const repository = await request.get(`${REPOSITORY_URL}/size`);
	expect(repository.status(), 'the repository size endpoint should be reachable after creation').toBe(200);
	expect(await repository.text()).toBe('0');
	const posts = createRequests.filter(requestEvent => requestEvent.method === 'POST');
	const modelGets = createRequests.filter(requestEvent => requestEvent.method === 'GET'
		&& requestEvent.accept.includes('application/vnd.rdf4j.workbench+ndjson'));
	console.log('CREATE_LIFECYCLE_REQUESTS', JSON.stringify(createRequests));
	expect(modelGets, 'each displayed GET form state should fetch its page model once').toHaveLength(2);
	expect(posts, 'the final Create submission should not be replayed').toHaveLength(1);
	expect(posts[0].body).toContain(`Repository+ID=${REPOSITORY_ID}`);
	expect(pageErrors, 'the Create flow should not raise browser errors').toEqual([]);
});
