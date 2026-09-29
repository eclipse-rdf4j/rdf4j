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
	test('keeps the required Rule query visible while Advanced settings are collapsed', async ({ page }) => {
		for (const type of ['memory-customrule', 'native-customrule']) {
			await page.goto(`${WORKBENCH_BASE_URL}/repositories/NONE/create?type=${type}`, {
				waitUntil: 'domcontentloaded'
			});
			const form = page.locator('form[action="create"]');
			const ruleQuery = form.locator('#sp_text');
			const matcher = form.locator('#sp_text-2');
			await expect(form.locator('#create-advanced-panel')).toBeHidden();
			await expect(ruleQuery, `${type} required Rule query should remain visible`).toBeVisible();
			await expect(ruleQuery.locator('xpath=ancestor::*[@data-workbench-detail-disclosure]'))
				.toHaveCount(0);
			await expect(matcher, `${type} optional Matcher should remain in Advanced`).toBeHidden();
		}
	});

    test('preserves every field and default while Advanced settings opens', async ({ page }) => {
        for (const type of CREATION_TYPES) {
            await page.goto(`${WORKBENCH_BASE_URL}/repositories/NONE/create?type=${type}`, {
                waitUntil: 'domcontentloaded'
            });
            const form = page.locator('form[action="create"]');
            await expect(form).toHaveCount(1);
            await expect(form).toBeVisible();

			if (type === 'federate') {
				const federationFeedback = form.locator('#create-feedback');
				await expect(federationFeedback, 'Federation validation should stay visible without available members')
					.toBeVisible();
				await expect(federationFeedback.locator('xpath=ancestor::*[@data-workbench-detail-disclosure]'))
					.toHaveCount(0);
			}

			const advanced = form.locator('.workbench-advanced[data-workbench-detail-disclosure="true"]');
			await expect(advanced, `${type} should render its expected Advanced panel count`)
				.toHaveCount(type === 'remote' || type === 'sparql' || type === 'federate' ? 0 : 1);

            const before = await snapshotForm(form);
            await expect(form.locator('#create')).toBeVisible();
			await expect(form.locator('#create').locator('xpath=ancestor::*[@data-workbench-detail-disclosure]')).toHaveCount(0);
            await expect(form.locator('input[type="button"][value="Cancel"], input[type="submit"][value="Cancel"]'))
                .toHaveCount(1);

			if (await advanced.count()) {
				const toggle = advanced.locator(':scope > .workbench-disclosure__toggle');
				const panel = advanced.locator(':scope > .workbench-disclosure__panel');
				await expect(toggle).toHaveAttribute('aria-expanded', 'false');
				await expect(panel).toBeHidden();
				const closedToggleWidth = await toggle.evaluate(element => element.getBoundingClientRect().width);
				await toggle.press('Enter');
				await expect(toggle).toHaveAttribute('aria-expanded', 'true');
				await expect(panel).toBeVisible();
				await expect(panel).toHaveAttribute('aria-hidden', 'false');
				expect(await panel.evaluate(element => element.inert), `${type} controls should be keyboard-accessible`).toBe(false);
				await expect.poll(() => panel.evaluate(element => element.getAnimations({ subtree: false })
					.filter(animation => animation.playState === 'running').length)).toBe(0);
				const openToggleWidth = await toggle.evaluate(element => element.getBoundingClientRect().width);
				expect.soft(Math.abs(openToggleWidth - closedToggleWidth), `${type} trigger should not resize when opened`)
					.toBeLessThanOrEqual(1);
				const stackedFields = await advanced.locator('.workbench-advanced__field').evaluateAll(fields =>
					fields.map(field => {
						const label = field.querySelector(':scope > label, :scope > span');
						const control = field.querySelector('.workbench-advanced__control');
						return label && control ? {
							labelBottom: label.getBoundingClientRect().bottom,
							controlTop: control.getBoundingClientRect().top
						} : null;
					}).filter(Boolean));
				for (const field of stackedFields) {
					expect(field.labelBottom, `${type} field labels should appear above their controls`)
						.toBeLessThanOrEqual(field.controlTop + 1);
				}
				const orphanedAdvancedLabels = await advanced.evaluate(owner => {
					const sourceRows = Array.from(document.querySelectorAll('form[action="create"] table.dataentry tbody > tr'));
					const labels = Array.from(owner.querySelectorAll('.workbench-advanced__field > label, .workbench-advanced__field > span'))
						.map(label => label.textContent.trim()).filter(Boolean);
					return labels.filter(label => sourceRows.some(row => row.textContent.includes(label)));
				});
				expect(orphanedAdvancedLabels, `${type} should remove moved labels from the source table`).toEqual([]);
				if (type.includes('customrule')) {
					const matcher = form.locator('#sp_text-2');
					const geometry = await matcher.evaluate(element => ({
						labelBottom: element.closest('.workbench-advanced__field').querySelector(':scope > label, :scope > span')
							.getBoundingClientRect().bottom,
						controlTop: element.getBoundingClientRect().top,
						controlRight: element.getBoundingClientRect().right,
						panelRight: element.closest('.workbench-disclosure__panel').getBoundingClientRect().right
					}));
					expect(geometry.labelBottom).toBeLessThanOrEqual(geometry.controlTop + 1);
					expect(geometry.controlRight).toBeLessThanOrEqual(geometry.panelRight + 1);
				}
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
                const federationMembers = form.locator('input[name="memberID"]');
                const memberCount = await federationMembers.count();
                for (let index = 0; index < memberCount; index += 1) {
                    const member = federationMembers.nth(index);
                    await expect(member, 'Available federation members should remain visible').toBeVisible();
                    await expect(member.locator('xpath=ancestor::*[@data-workbench-detail-disclosure]'))
                        .toHaveCount(0);
                }
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
            const advanced = form.locator('.workbench-advanced[data-workbench-detail-disclosure="true"]');
            if (await advanced.count()) {
				await advanced.locator(':scope > .workbench-disclosure__toggle').press('Enter');
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
