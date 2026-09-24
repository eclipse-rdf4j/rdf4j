// @ts-check
const { test, expect } = require('@playwright/test');

const WORKBENCH_BASE_URL = process.env.RDF4J_WORKBENCH_BASE_URL || 'http://127.0.0.1:8080/rdf4j-workbench';
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

test.describe('repository creation contracts', () => {
    test('preserves every field and default while Advanced settings opens', async ({ page }) => {
        for (const type of CREATION_TYPES) {
            await page.goto(`${WORKBENCH_BASE_URL}/repositories/NONE/create?type=${type}`, {
                waitUntil: 'domcontentloaded'
            });
            const form = page.locator('form[action="create"]');
            await expect(form).toHaveCount(1);
            await expect(form).toBeVisible();

            const closedDetails = form.locator('details.workbench-advanced');
            await expect(closedDetails).toHaveCount(type === 'remote' || type === 'sparql' || type === 'federate' ? 0 : 1);
            if (await closedDetails.count()) {
                await expect(closedDetails).not.toHaveAttribute('open', '');
            }

            const before = await snapshotForm(form);
            await expect(form.locator('#create')).toBeVisible();
            await expect(form.locator('#create').locator('xpath=ancestor::details')).toHaveCount(0);
            await expect(form.locator('input[type="button"][value="Cancel"], input[type="submit"][value="Cancel"]'))
                .toHaveCount(1);

            if (await closedDetails.count()) {
                await closedDetails.locator('summary').press('Enter');
                await expect(closedDetails).toHaveAttribute('open', '');
            }

            const after = await snapshotForm(form);
            expect(after, `${type} form controls changed when Advanced settings opened`).toEqual(before);

            if (type.includes('customrule')) {
                await expect(form.locator('#sp_text')).toBeVisible();
                await expect(form.locator('#sp_text-2')).toBeVisible();
            }
            if (type === 'remote') {
                await expect(form.locator('#config_http-url')).toHaveCount(1);
                await expect(form.locator('#config_http-url-2')).toHaveCount(1);
            }
            if (type === 'sparql') {
                await expect(form.locator('#config_sparql-queryEndpoint')).toHaveCount(1);
                await expect(form.locator('#config_sparql-updateEndpoint')).toHaveCount(1);
            }
            if (type === 'federate') {
                await expect(form.locator('input[name="memberID"]')).toHaveCount(2);
                await expect(form.locator('#create')).toBeDisabled();
            }
            if (type === 'lmdb') {
                await expect(form.locator('input[id^="lmdb_sketchEstimatorEnabled-"]')).toHaveCount(3);
                await expect(form.locator('input[id^="lmdb_sketchEstimatorEnabled-"]:checked'))
                    .toHaveValue('__workbench_unset__');
            }
        }
    });

    test('keeps creation actions and fields inside the viewport on mobile', async ({ page }) => {
        await page.setViewportSize({ width: 390, height: 900 });
        for (const type of CREATION_TYPES) {
            await page.goto(`${WORKBENCH_BASE_URL}/repositories/NONE/create?type=${type}`, {
                waitUntil: 'domcontentloaded'
            });
            const form = page.locator('form[action="create"]');
            await expect(form).toBeVisible();
            const advanced = form.locator('details.workbench-advanced');
            if (await advanced.count()) {
                await advanced.locator('summary').press('Enter');
            }
            const metrics = await form.evaluate(element => {
                const rect = element.getBoundingClientRect();
                const create = element.querySelector('#create').getBoundingClientRect();
                const cancel = Array.from(element.querySelectorAll('input')).find(input => input.value === 'Cancel')
                    ?.getBoundingClientRect();
                return {
                    formRight: rect.right,
                    viewportWidth: document.documentElement.clientWidth,
                    createRight: create.right,
                    cancelRight: cancel ? cancel.right : rect.right,
                    documentOverflow: document.documentElement.scrollWidth - document.documentElement.clientWidth
                };
            });
            expect(metrics.formRight, `${type} form exceeds viewport`).toBeLessThanOrEqual(metrics.viewportWidth + 1);
            expect(metrics.createRight, `${type} Create exceeds viewport`).toBeLessThanOrEqual(metrics.viewportWidth + 1);
            expect(metrics.cancelRight, `${type} Cancel exceeds viewport`).toBeLessThanOrEqual(metrics.viewportWidth + 1);
            expect(metrics.documentOverflow, `${type} page overflows horizontally`).toBeLessThanOrEqual(1);
        }
    });
});

async function snapshotForm(form) {
    return form.evaluate(element => Array.from(element.querySelectorAll('input, select, textarea')).map(control => ({
        tag: control.tagName,
        id: control.id,
        name: control.getAttribute('name'),
        type: control.getAttribute('type'),
        value: control.value,
        checked: 'checked' in control ? control.checked : null,
        disabled: control.disabled,
        selected: control.tagName === 'SELECT'
            ? Array.from(control.selectedOptions).map(option => option.value)
            : null
    })));
}
