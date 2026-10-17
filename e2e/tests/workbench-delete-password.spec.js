// @ts-check
const { test, expect } = require('@playwright/test');
const os = require('node:os');
const path = require('node:path');
const {
    createSeededRepository, serverBaseUrl, uniqueRepositoryId, waitForRoute, workbenchBaseUrl
} = require('./workbench-test-helpers.js');

// Run against either a normal deployment or a protected Server with a separate Workbench process.
// The latter's Server startup password is supplied only to this test process through the environment.
const password = process.env.RDF4J_DELETION_PASSWORD;
const protectedServer = password !== undefined;
const deletionHeader = 'X-RDF4J-Repository-Delete-Password';
const pageType = 'application/vnd.rdf4j.workbench+ndjson';
const failureMessage = 'Repository deletion requires a valid deletion password.';

async function repositoryExists(request, id) {
    const response = await request.get(`${serverBaseUrl()}/repositories`, {
        headers: { Accept: 'application/sparql-results+json' }
    });
    expect(response.ok()).toBe(true);
    const result = await response.json();
    return result.results.bindings.some((row) => row.id.value === id);
}

test('Delete retains typed confirmation, enforces the Server password, and clears secrets for retry and history', async ({ page, request }) => {
    const repositoryId = uniqueRepositoryId('delete-password');
    const otherId = uniqueRepositoryId('delete-password-other');
    const base = workbenchBaseUrl();
    const deleteUrl = `${base}/repositories/NONE/delete?id=${encodeURIComponent(repositoryId)}`;
    const errors = [];
    const consoleErrors = [];
    page.on('pageerror', (error) => errors.push(error.message));
    page.on('console', (message) => {
        if (message.type() === 'error' && !/status of 403/.test(message.text())) {
            consoleErrors.push(message.text());
        }
    });
    await createSeededRepository(request, serverBaseUrl(), repositoryId, { graphs: [] });
    await createSeededRepository(request, serverBaseUrl(), otherId, { graphs: [] });
    const cleanup = async (id) => {
        // Existing RDF4J deployments answer DELETE of an absent repository with 400; prove absence first.
        if (!await repositoryExists(request, id)) { return; }
        const response = await request.delete(`${serverBaseUrl()}/repositories/${id}`, {
            headers: protectedServer ? { [deletionHeader]: Buffer.from(password, 'utf8').toString('base64') } : {}
        });
        expect([204, 404]).toContain(response.status());
        expect(await repositoryExists(request, id)).toBe(false);
    };
    try {
        await page.setViewportSize({ width: 1440, height: 900 });
        if (new URL(base).origin !== new URL(serverBaseUrl()).origin) {
            await page.goto(`${base}/repositories/NONE/server`, { waitUntil: 'domcontentloaded' });
            await page.locator('#workbench-server').fill(serverBaseUrl());
            await page.locator('#server-form input[type="submit"]').click();
            await expect(page).toHaveURL(/\/repositories\/NONE\/repositories(?:[?#]|$)/);
        }
        await page.goto(deleteUrl, { waitUntil: 'domcontentloaded' });
        await waitForRoute(page, 'delete');
        await expect(page).toHaveTitle(/Delete Repository|Delete repository/i);
        const form = page.locator('#delete-form');
        const selected = form.locator('#id');
        const action = form.getByRole('button', { name: 'Delete repository…' });
        const secret = form.locator('#delete-password');
        await expect(selected).toHaveValue(repositoryId);
        if (protectedServer) {
            await expect(secret).toHaveAttribute('type', 'password');
            await expect(secret).toHaveValue('');
            await expect(action).toBeDisabled();
            const denied = await page.request.post(deleteUrl, {
                headers: { Accept: pageType }, form: { id: repositoryId }
            });
            expect(denied.status()).toBe(403);
            const body = await denied.text();
            expect(body).toContain(failureMessage);
            expect(body).toContain('repository-deletion-password-required');
            expect(body).toContain(repositoryId);
            expect(body).not.toContain(password);
            await secret.fill('wrong deletion password');
        } else {
            await expect(secret).toHaveCount(0);
        }
        await expect(action).toBeEnabled();

        // Keyboard opening, typed-ID enablement, Escape, settled close, focus return, and no surrounding movement.
        const before = await form.boundingBox();
        let safetyRequests = 0;
        let releaseSafety;
        const safetyReady = new Promise((resolve) => { releaseSafety = resolve; });
        const safetyMatch = (url) => url.pathname.endsWith('/delete') && url.searchParams.get('checkSafe') === repositoryId;
        await page.route(safetyMatch, async (route) => {
            safetyRequests++;
            await safetyReady;
            await route.continue();
        });
        await action.focus();
        await page.keyboard.press('Enter');
        await form.evaluate((element) => { element.requestSubmit(); element.requestSubmit(); });
        await expect.poll(() => safetyRequests).toBe(1);
        releaseSafety();
        const dialog = page.getByRole('dialog', { name: `Delete repository ${repositoryId}?` });
        const confirm = dialog.getByRole('button', { name: 'Delete repository', exact: true });
        await expect(dialog).toBeVisible();
        await expect(dialog.getByRole('button', { name: 'Cancel' })).toBeFocused();
        await expect(confirm).toBeDisabled();
        await page.screenshot({ path: path.join(os.tmpdir(), 'rdf4j-delete-password-desktop-dialog.png') });
        await page.keyboard.press('Escape');
        await expect(dialog).toHaveCount(0);
        await expect(action).toBeFocused();
        expect(await form.boundingBox()).toEqual(before);
        await page.unroute(safetyMatch);

        // A confirmation belongs to the selected repository even if another script changes the underlying form.
        await action.click();
        await expect(dialog).toBeVisible();
        await selected.evaluate((element, next) => {
            element.value = next;
            element.dispatchEvent(new Event('change', { bubbles: true }));
        }, otherId);
        await dialog.getByRole('textbox').fill(repositoryId);
        await confirm.click();
        await expect(dialog).toHaveCount(0);
        expect((await request.get(`${serverBaseUrl()}/repositories/${repositoryId}/size`)).ok()).toBe(true);
        expect((await request.get(`${serverBaseUrl()}/repositories/${otherId}/size`)).ok()).toBe(true);
        await selected.selectOption(repositoryId);

        if (protectedServer) {
            await expect(secret).toHaveValue('');
            await expect(action).toBeDisabled();
            await secret.fill('wrong deletion password');
            await action.click();
            await dialog.getByRole('textbox').fill(repositoryId);
            const denial = page.waitForResponse((response) => response.request().method() === 'POST'
                && new URL(response.url()).pathname.endsWith('/delete'));
            await confirm.click();
            expect((await denial).status()).toBe(403);
            await expect(page.getByRole('alert').filter({ hasText: failureMessage })).toBeVisible();
            await expect(selected).toHaveValue(repositoryId);
            await expect(selected.locator(`option[value="${otherId}"]`)).toHaveCount(1);
            await expect(secret).toHaveValue('');
            await expect(action).toBeDisabled();
            await page.screenshot({ path: path.join(os.tmpdir(), 'rdf4j-delete-password-retry.png') });

            // Passwords stay out of the router's saved drafts and browser storage.
            await secret.fill(password);
            await page.locator('.workbench-nav a[href$="/repositories"]').first().click();
            await expect(page).toHaveURL(/\/repositories\/NONE\/repositories(?:[?#]|$)/);
            await expect(form).toHaveCount(0);
            await page.goBack();
            await waitForRoute(page, 'delete');
            await expect(secret).toHaveValue('');
            expect(await page.evaluate(() => JSON.stringify({ local: { ...localStorage }, session: { ...sessionStorage },
                cookie: document.cookie }))).not.toContain(password);
            await selected.selectOption(repositoryId);
            await secret.fill(password);
        }

        await page.setViewportSize({ width: 390, height: 844 });
        await expect(action).toBeEnabled();
        await action.click();
        await expect(dialog).toBeVisible();
        await page.screenshot({ path: path.join(os.tmpdir(), 'rdf4j-delete-password-mobile-dialog.png') });
        expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
        await dialog.getByRole('textbox').fill(repositoryId);
        await expect(confirm).toBeEnabled();
        await dialog.getByRole('textbox').press('Enter');
        await expect(page).toHaveURL(/\/repositories\/NONE\/repositories(?:[?#]|$)/);
        expect(await repositoryExists(request, repositoryId)).toBe(false);
        expect(errors).toEqual([]);
        expect(consoleErrors).toEqual([]);
    } finally {
        await cleanup(repositoryId);
        await cleanup(otherId);
    }
});
