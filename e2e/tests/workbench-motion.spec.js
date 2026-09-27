// @ts-check
const { test, expect } = require('@playwright/test');

const WORKBENCH_BASE_URL = (process.env.RDF4J_WORKBENCH_BASE_URL ||
	'http://127.0.0.1:8080/rdf4j-workbench').replace(/\/+$/, '');
const SERVER_BASE_URL = (process.env.RDF4J_SERVER_BASE_URL ||
	'http://127.0.0.1:8080/rdf4j-server').replace(/\/+$/, '');
const REPOSITORY_ID = `workbench-motion-${process.pid}-${Date.now()}`;
const REPOSITORY_URL = `${SERVER_BASE_URL}/repositories/${REPOSITORY_ID}`;
const QUERY_URL = `${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/query`;

test.beforeAll(async ({ request }) => {
	const absent = await request.get(REPOSITORY_URL);
	expect(absent.status()).toBe(404);
	const created = await request.put(REPOSITORY_URL, {
		headers: { 'Content-Type': 'text/turtle' },
		data: `@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix config: <tag:rdf4j.org,2023:config/>.
[] a config:Repository ; config:rep.id "${REPOSITORY_ID}" ; rdfs:label "Motion test repository" ;
   config:rep.impl [ config:rep.type "openrdf:SailRepository" ;
      config:sail.impl [ config:sail.type "openrdf:MemoryStore" ; config:mem.persist false ] ].`
	});
	expect([200, 201, 204]).toContain(created.status());
	const statements = await request.post(`${REPOSITORY_URL}/statements`, {
		headers: { 'Content-Type': 'application/n-triples' },
		data: '<http://example.org/motion> <http://example.org/label> "motion result" .'
	});
	expect([200, 201, 204]).toContain(statements.status());
});

test.afterAll(async ({ request }) => {
	await request.delete(REPOSITORY_URL);
});

async function activeMotionCount(locator) {
	return locator.evaluate(element => element.getAnimations({ subtree: false })
		.filter(animation => animation.playState === 'running').length);
}

async function retainedForwardFillCount(locator) {
	return locator.evaluate(element => element.getAnimations({ subtree: false })
		.filter(animation => animation.effect && animation.effect.getTiming().fill === 'forwards').length);
}

async function hasVisibleIntermediateMotion(locator, lowerBound = 0, upperBound = null) {
	return locator.evaluate((element, bounds) => {
		const running = element.getAnimations({ subtree: false })
			.some(animation => animation.playState === 'running');
		const height = element.getBoundingClientRect().height;
		const upper = bounds.upperBound === null ? element.scrollHeight : bounds.upperBound;
		return running && height > bounds.lowerBound + 1 && height < upper - 1;
	}, { lowerBound, upperBound });
}

async function hasVisibleIntermediateDetailsMotion(locator) {
	return locator.evaluate(element => {
		const summary = element.querySelector(':scope > summary');
		const running = element.getAnimations({ subtree: false })
			.some(animation => animation.playState === 'running');
		const height = element.getBoundingClientRect().height;
		const summaryHeight = summary ? summary.getBoundingClientRect().height : 0;
		return running && height > summaryHeight + 1 && height < element.scrollHeight - 1;
	});
}

async function hasIntermediateOpacityMotion(locator) {
	return locator.evaluate(element => {
		const running = element.getAnimations({ subtree: false })
			.some(animation => animation.playState === 'running');
		const opacity = parseFloat(getComputedStyle(element).opacity);
		return running && opacity > 0.02 && opacity < 0.98;
	});
}

async function waitForOwnedAnimations(locator) {
	await expect.poll(() => activeMotionCount(locator)).toBe(0);
}

async function runPrimaryQuery(page) {
	await page.locator('.CodeMirror').first().evaluate(element =>
		element.CodeMirror.setValue('SELECT ?s WHERE { ?s ?p ?o }'));
	await page.locator('#exec').click();
	const results = page.locator('#query-results');
	await expect(results).toBeVisible();
	expect(await activeMotionCount(results)).toBeGreaterThan(0);
	await expect.poll(() => hasIntermediateOpacityMotion(results)).toBe(true);
	await page.frameLocator('#query-results-frame').locator('#rdf4j-query-result').waitFor({ state: 'attached' });
	await expect(results).toBeVisible();
	await page.locator('#explain-trigger').click();
	await expect.poll(() => page.locator('#query-explanation').textContent()).toMatch(/\S/);
	await expect(page.locator('#compare-toggle')).toBeVisible();
}

test('query options animate reversibly while semantic state and focus update immediately', async ({ page }) => {
	await page.setViewportSize({ width: 1440, height: 1000 });
	await page.emulateMedia({ reducedMotion: 'no-preference' });
	await page.goto(QUERY_URL);

	const toggle = page.locator('#query-options-toggle');
	const panel = page.locator('#query-options-panel');
	await expect(toggle).toBeVisible();
	await expect(toggle).toHaveAttribute('aria-expanded', 'false');
	await toggle.focus();
	await page.keyboard.press('Enter');
	await expect(toggle).toHaveAttribute('aria-expanded', 'true');
	await expect(panel).toBeVisible();
	expect(await activeMotionCount(panel)).toBeGreaterThan(0);
	await expect.poll(() => hasVisibleIntermediateMotion(panel)).toBe(true);
	await waitForOwnedAnimations(panel);

	await panel.locator('#limit_query').focus();
	await page.evaluate(() => document.getElementById('query-options-toggle').click());
	await expect(toggle).toHaveAttribute('aria-expanded', 'false');
	await expect(toggle).toBeFocused();
	const closingState = await panel.evaluate(element => ({
		hidden: element.hidden,
		inert: element.inert,
		ariaHidden: element.getAttribute('aria-hidden'),
		active: element.getAnimations({ subtree: false })
			.some(animation => animation.playState === 'running')
	}));
	expect(closingState).toMatchObject({ hidden: false, inert: true, ariaHidden: 'true', active: true });
	await expect.poll(() => hasVisibleIntermediateMotion(panel)).toBe(true);
	const queryReverse = await panel.evaluate(element => {
		const before = element.getBoundingClientRect().height;
		document.getElementById('query-options-toggle').click();
		const animation = element.getAnimations({ subtree: false })
			.find(candidate => candidate.playState === 'running');
		const frames = animation.effect.getKeyframes();
		return {
			before,
			firstKeyframeHeight: parseFloat(frames[0].height),
			currentHeight: element.getBoundingClientRect().height
		};
	});
	await expect(toggle).toHaveAttribute('aria-expanded', 'true');
	await expect(panel).toHaveAttribute('aria-hidden', 'false');
	await expect.poll(() => panel.evaluate(element => element.inert)).toBe(false);
	expect(Math.abs(queryReverse.firstKeyframeHeight - queryReverse.before)).toBeLessThanOrEqual(0.001);
	expect(queryReverse.currentHeight).toBeGreaterThan(0);
	await waitForOwnedAnimations(panel);
	await toggle.evaluate(element => element.click());
	await expect.poll(() => panel.evaluate(element => element.hidden)).toBe(true);
});

test('live reduced-motion changes settle only the requested disclosure state', async ({ page }) => {
	await page.emulateMedia({ reducedMotion: 'no-preference' });
	await page.goto(QUERY_URL);
	const toggle = page.locator('#query-options-toggle');
	const panel = page.locator('#query-options-panel');
	await toggle.click();
	expect(await activeMotionCount(panel)).toBeGreaterThan(0);

	await page.emulateMedia({ reducedMotion: 'reduce' });
	await expect.poll(() => activeMotionCount(panel)).toBe(0);
	await expect(toggle).toHaveAttribute('aria-expanded', 'true');
	await expect(panel).toBeVisible();
	await toggle.click();
	await expect(toggle).toHaveAttribute('aria-expanded', 'false');
	await expect(panel).toBeHidden();
});

test('navigation groups and compare panes animate their outer content only', async ({ page }) => {
	await page.emulateMedia({ reducedMotion: 'no-preference' });
	await page.setViewportSize({ width: 1440, height: 1000 });
	await page.goto(QUERY_URL);

	const repositoryGroup = page.locator(
		'#navigation .workbench-nav-group[data-workbench-menu-group="repositories"] details');
	const repositorySummary = repositoryGroup.locator('summary');
	const repositoryItems = repositoryGroup.locator('ul.group');
	await expect.poll(() => repositoryGroup.evaluate(element => element.open)).toBe(false);
	await repositorySummary.focus();
	await page.keyboard.press('Enter');
	await expect.poll(() => repositoryGroup.evaluate(element => element.open)).toBe(true);
	expect(await activeMotionCount(repositoryGroup)).toBeGreaterThan(0);
	await expect.poll(() => hasVisibleIntermediateDetailsMotion(repositoryGroup)).toBe(true);
	await repositorySummary.press('Enter');
	await expect(repositorySummary).toHaveAttribute('aria-expanded', 'false');
	await expect.poll(() => repositoryItems.evaluate(element => element.inert)).toBe(true);
	await expect.poll(() => hasVisibleIntermediateDetailsMotion(repositoryGroup)).toBe(true);
	await waitForOwnedAnimations(repositoryGroup);
	await expect.poll(() => repositoryGroup.evaluate(element => element.open)).toBe(false);

	await runPrimaryQuery(page);
	await page.locator('#compare-toggle').click();
	const comparePane = page.locator('#query-compare-pane');
	await expect(comparePane).toBeVisible();
	expect(await activeMotionCount(comparePane)).toBeGreaterThan(0);
	await expect.poll(() => hasIntermediateOpacityMotion(comparePane)).toBe(true);
	const compareLayout = page.locator('#query-compare-layout');
	await expect.poll(() => compareLayout.evaluate(element =>
		element.getAnimations().some(animation => animation.playState === 'running'))).toBe(false);
	expect(parseFloat(await compareLayout.evaluate(element => getComputedStyle(element).columnGap))).toBeCloseTo(16, 1);
	await page.locator('#query-compare-close').click();
	await expect(page.locator('#compare-toggle')).toBeFocused();
	await expect(comparePane).toHaveAttribute('aria-hidden', 'true');
	expect(await comparePane.evaluate(element => element.inert)).toBe(true);
	await expect.poll(() => hasIntermediateOpacityMotion(comparePane)).toBe(true);
	await waitForOwnedAnimations(comparePane);
	await expect.poll(() => compareLayout.evaluate(element =>
		element.getAnimations().some(animation => animation.playState === 'running'))).toBe(false);
	await expect.poll(() => compareLayout.evaluate(element =>
		parseFloat(getComputedStyle(element).columnGap))).toBeLessThan(0.01);
});

test('completed disclosures release their fill effects and follow natural sizing', async ({ page }) => {
	await page.setViewportSize({ width: 1440, height: 1000 });
	await page.goto(QUERY_URL);
	const toggle = page.locator('#query-options-toggle');
	const panel = page.locator('#query-options-panel');
	await toggle.click();
	await waitForOwnedAnimations(panel);
	expect.soft(await retainedForwardFillCount(panel)).toBe(0);

	await toggle.click();
	await expect(panel).toBeHidden();
	await waitForOwnedAnimations(panel);
	expect.soft(await retainedForwardFillCount(panel)).toBe(0);
	await toggle.click();
	await waitForOwnedAnimations(panel);
	await page.setViewportSize({ width: 320, height: 844 });
	await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => resolve(undefined))));
	const responsivePanel = await panel.evaluate(element => ({
		height: element.getBoundingClientRect().height,
		scrollHeight: element.scrollHeight,
		width: element.getBoundingClientRect().width,
		viewport: document.documentElement.clientWidth
	}));
	expect.soft(responsivePanel.scrollHeight - responsivePanel.height).toBeLessThanOrEqual(1);
	expect.soft(responsivePanel.width).toBeLessThanOrEqual(responsivePanel.viewport + 1);
	expect.soft(await retainedForwardFillCount(panel)).toBe(0);

	await page.setViewportSize({ width: 390, height: 844 });
	await page.goto(`${WORKBENCH_BASE_URL}/repositories/NONE/create?type=memory`);
	const advanced = page.locator('details.workbench-advanced');
	await advanced.locator(':scope > summary').click();
	await waitForOwnedAnimations(advanced);
	expect.soft(await retainedForwardFillCount(advanced)).toBe(0);
	const advancedHeight = await advanced.evaluate(element => element.getBoundingClientRect().height);
	await advanced.evaluate(element => {
		const probe = document.createElement('div');
		probe.setAttribute('data-motion-growth-probe', '');
		probe.style.cssText = 'display:block;height:72px;box-sizing:border-box';
		element.appendChild(probe);
	});
	await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => resolve(undefined))));
	const grownHeight = await advanced.evaluate(element => element.getBoundingClientRect().height);
	expect.soft(grownHeight - advancedHeight).toBeGreaterThanOrEqual(70);
});

test('a new query reverses the loading indicator exit animation', async ({ page }) => {
	await page.addInitScript(() => {
		const animate = Element.prototype.animate;
		Element.prototype.animate = function(keyframes, options) {
			const animation = animate.call(this, keyframes, options);
			const frames = Array.isArray(keyframes) ? keyframes : [keyframes];
			const lastFrame = frames[frames.length - 1] || {};
			if (this.id === 'query-results-loading' && String(lastFrame.opacity) === '0') {
				animation.pause();
				window.dispatchEvent(new Event('workbench-test-loading-exit-paused'));
			}
			return animation;
		};
	});
	await page.goto(QUERY_URL);
	const exitPaused = page.evaluate(() => new Promise(resolve =>
		window.addEventListener('workbench-test-loading-exit-paused', resolve, { once: true })));
	await page.locator('.CodeMirror').first().evaluate(element =>
		element.CodeMirror.setValue('SELECT ?s WHERE { VALUES ?s { "first" } }'));
	await page.locator('#exec').click();
	await exitPaused;
	await expect(page.frameLocator('#query-results-frame').locator('body')).toContainText('first');

	let releaseSecondRequest;
	let resolveSecondRequestStarted;
	const secondRequestGate = new Promise(resolve => { releaseSecondRequest = resolve; });
	const secondRequestStarted = new Promise(resolve => { resolveSecondRequestStarted = resolve; });
	const queryRoute = `**/repositories/${REPOSITORY_ID}/query**`;
	await page.route(queryRoute, async route => {
		const request = route.request();
		const parameters = request.method() === 'GET'
			? new URL(request.url()).searchParams
			: new URLSearchParams(request.postData() || '');
		if (parameters.get('action') === 'exec') {
			resolveSecondRequestStarted();
			await secondRequestGate;
		}
		await route.continue();
	});
	try {
		await page.locator('.CodeMirror').first().evaluate(element =>
			element.CodeMirror.setValue('SELECT ?s WHERE { VALUES ?s { "second" } }'));
		await page.locator('#exec').click();
		await secondRequestStarted;
		const loadingState = await page.locator('#query-results-loading').evaluate(element => ({
			hidden: element.hidden,
			ariaHidden: element.getAttribute('aria-hidden'),
			animationStates: element.getAnimations({ subtree: false }).map(animation => animation.playState)
		}));
		expect(loadingState.hidden).toBe(false);
		expect(loadingState.ariaHidden).toBe('false');
		expect(loadingState.animationStates).toContain('running');
	} finally {
		releaseSecondRequest();
		await page.unroute(queryRoute);
		await page.locator('#query-results-loading').evaluate(element => {
			element.getAnimations({ subtree: false }).forEach(animation => animation.play());
		}).catch(() => {});
	}
});

test('primary query and form actions show compact keyboard press feedback in both themes', async ({ page }) => {
	await page.emulateMedia({ reducedMotion: 'no-preference' });
	await page.setViewportSize({ width: 1440, height: 1000 });
	await page.mouse.move(1400, 950);

	async function verifyPressedAction(control, activeTarget, theme) {
		const initial = await page.evaluate(({ controlSelector, targetSelector }) => {
			const button = document.querySelector(controlSelector);
			const target = button.matches(targetSelector) ? button : button.closest(targetSelector);
			const buttonStyle = getComputedStyle(button);
			const targetStyle = getComputedStyle(target);
			return {
				transitionProperty: targetStyle.transitionProperty.split(',').map(property => property.trim()),
				transitionDurations: targetStyle.transitionDuration.split(',').map(value => {
					const duration = parseFloat(value);
					return value.trim().endsWith('ms') ? duration : duration * 1000;
				}),
				boxShadow: targetStyle.boxShadow,
				color: buttonStyle.color,
				background: targetStyle.backgroundColor
			};
		}, { controlSelector: control, targetSelector: activeTarget });
		expect(initial.transitionProperty).toContain('box-shadow');
		expect(initial.transitionDurations.some(duration => duration > 0 && duration <= 130)).toBe(true);

		const action = page.locator(control);
		await action.focus();
		await page.keyboard.down('Space');
		const pressed = await page.evaluate(({ controlSelector, targetSelector }) => {
			const button = document.querySelector(controlSelector);
			const target = button.matches(targetSelector) ? button : button.closest(targetSelector);
			const buttonStyle = getComputedStyle(button);
			const targetStyle = getComputedStyle(target);
			const color = buttonStyle.color.match(/[\d.]+/g).map(Number).slice(0, 3);
			const background = targetStyle.backgroundColor.match(/[\d.]+/g).map(Number).slice(0, 3);
			const luminance = rgb => {
				const channels = rgb.map(value => value / 255).map(value =>
					value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4));
				return channels[0] * 0.2126 + channels[1] * 0.7152 + channels[2] * 0.0722;
			};
			const foregroundLuminance = luminance(color);
			const backgroundLuminance = luminance(background);
			return {
				active: button.matches(':active'),
				boxShadow: targetStyle.boxShadow,
				color: buttonStyle.color,
				background: targetStyle.backgroundColor,
				contrast: (Math.max(foregroundLuminance, backgroundLuminance) + 0.05)
					/ (Math.min(foregroundLuminance, backgroundLuminance) + 0.05)
			};
		}, { controlSelector: control, targetSelector: activeTarget });
		await page.keyboard.up('Space');
		expect(pressed.active).toBe(true);
		expect(pressed.boxShadow).not.toBe(initial.boxShadow);
		expect(pressed.contrast, `${control} in ${theme}: ${pressed.color} on ${pressed.background}`)
			.toBeGreaterThanOrEqual(4.5);
	}

	for (const theme of ['light', 'dark']) {
		await page.goto(QUERY_URL);
		await page.locator('#workbench-theme').selectOption(theme);
		await expect(page.locator('html')).toHaveAttribute('data-theme', theme);
		await page.evaluate(() => {
			document.addEventListener('click', event => {
				if (event.target.closest('#exec, #create')) {
					event.preventDefault();
					event.stopImmediatePropagation();
				}
			}, true);
		});
		await verifyPressedAction('#exec', '.query-action--primary', theme);

		await page.goto(`${WORKBENCH_BASE_URL}/repositories/NONE/create?type=memory`);
		await page.locator('#workbench-theme').selectOption(theme);
		await expect(page.locator('html')).toHaveAttribute('data-theme', theme);
		await page.evaluate(() => {
			document.addEventListener('click', event => {
				if (event.target.closest('#exec, #create')) {
					event.preventDefault();
					event.stopImmediatePropagation();
				}
			}, true);
		});
		await verifyPressedAction('#create', '.workbench-action--primary', theme);
	}
});

test('native form and mobile navigation disclosures reverse within narrow layouts', async ({ page }) => {
	await page.emulateMedia({ reducedMotion: 'no-preference' });
	await page.setViewportSize({ width: 390, height: 844 });
	await page.goto(`${WORKBENCH_BASE_URL}/repositories/NONE/create?type=memory`);

	const advanced = page.locator('details.workbench-advanced');
	await expect(advanced).toHaveCount(1);
	const advancedSummary = advanced.locator(':scope > summary');
	const advancedContent = advanced.locator(':scope > table.workbench-advanced-fields');
	await expect(advanced).not.toHaveAttribute('open', '');
	await advancedSummary.focus();
	await page.keyboard.press('Enter');
	expect(await activeMotionCount(advanced)).toBeGreaterThan(0);
	await expect(advancedSummary).toHaveAttribute('aria-expanded', 'true');
	await expect.poll(() => hasVisibleIntermediateDetailsMotion(advanced)).toBe(true);
	await waitForOwnedAnimations(advanced);
	await advancedSummary.evaluate(element => element.click());
	await expect(advancedSummary).toHaveAttribute('aria-expanded', 'false');
	await expect(advanced).toHaveAttribute('open', '');
	await expect.poll(() => advancedContent.evaluate(element => element.inert)).toBe(true);
	await expect.poll(() => advancedContent.getAttribute('aria-hidden')).toBe('true');
	await expect.poll(() => hasVisibleIntermediateDetailsMotion(advanced)).toBe(true);
	const advancedReverse = await advanced.evaluate(element => {
		const before = element.getBoundingClientRect().height;
		element.querySelector(':scope > summary').click();
		const animation = element.getAnimations({ subtree: false })
			.find(candidate => candidate.playState === 'running');
		const frames = animation.effect.getKeyframes();
		return {
			before,
			firstKeyframeHeight: parseFloat(frames[0].height),
			currentHeight: element.getBoundingClientRect().height
		};
	});
	await expect(advancedSummary).toHaveAttribute('aria-expanded', 'true');
	await expect.poll(() => advancedContent.evaluate(element => element.inert)).toBe(false);
	expect(Math.abs(advancedReverse.firstKeyframeHeight - advancedReverse.before)).toBeLessThanOrEqual(0.001);
	expect(advancedReverse.currentHeight).toBeGreaterThan(0);
	await waitForOwnedAnimations(advanced);
	await advancedSummary.evaluate(element => element.click());
	await waitForOwnedAnimations(advanced);
	await expect(advanced).not.toHaveAttribute('open', '');

	await page.setViewportSize({ width: 320, height: 844 });
	await page.goto(`${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/add`);
	const importOptions = page.locator('#add-import-settings');
	const importOptionsSummary = importOptions.locator(':scope > summary');
	const importOptionsBody = importOptions.locator(':scope > .workbench-options__body');
	await expect(importOptions).not.toHaveAttribute('open', '');
	await importOptionsSummary.click();
	await expect(importOptionsSummary).toHaveAttribute('aria-expanded', 'true');
	await expect.poll(() => hasVisibleIntermediateDetailsMotion(importOptions)).toBe(true);
	await waitForOwnedAnimations(importOptions);
	await importOptionsSummary.click();
	await expect(importOptionsSummary).toHaveAttribute('aria-expanded', 'false');
	await expect.poll(() => importOptionsBody.evaluate(element => element.inert)).toBe(true);
	await expect.poll(() => hasVisibleIntermediateDetailsMotion(importOptions)).toBe(true);
	await waitForOwnedAnimations(importOptions);
	await expect(importOptions).not.toHaveAttribute('open', '');

	const navigation = page.locator('#workbench-navigation-disclosure');
	const navigationSummary = page.locator('#workbench-navigation-summary');
	await expect(navigation).not.toHaveAttribute('open', '');
	await navigationSummary.click();
	await expect(navigationSummary).toHaveAttribute('aria-expanded', 'true');
	expect(await activeMotionCount(navigation)).toBeGreaterThan(0);
	await expect.poll(() => hasVisibleIntermediateDetailsMotion(navigation)).toBe(true);

	const bounds = await page.evaluate(() => ({
		viewport: document.documentElement.clientWidth,
		overflow: document.documentElement.scrollWidth - document.documentElement.clientWidth,
		formRight: document.querySelector('#workbench-page-surface form').getBoundingClientRect().right
	}));
	expect(bounds.overflow).toBeLessThanOrEqual(1);
	expect(bounds.formRight).toBeLessThanOrEqual(bounds.viewport + 1);
});

test('diff overlay fades while modal accessibility and focus restore synchronously', async ({ page }) => {
	await page.emulateMedia({ reducedMotion: 'no-preference' });
	await page.goto(QUERY_URL);
	const compareToggle = page.locator('#compare-toggle');
	await runPrimaryQuery(page);
	await compareToggle.click();
	await page.locator('.CodeMirror').nth(1).evaluate(element =>
		element.CodeMirror.setValue('SELECT * WHERE { ?s ?p ?o }'));
	const diffTrigger = page.locator('#query-diff-trigger');
	await expect(diffTrigger).toBeEnabled();
	await diffTrigger.click();

	const modal = page.locator('#query-diff-modal');
	await expect(modal).toHaveAttribute('aria-hidden', 'false');
	expect(await activeMotionCount(modal)).toBeGreaterThan(0);
	await expect.poll(() => hasIntermediateOpacityMotion(modal)).toBe(true);
	await page.locator('#query-diff-close').click();
	await expect(modal).toHaveAttribute('aria-hidden', 'true');
	await expect(page.locator('#query-diff-trigger')).toBeFocused();
	expect(await modal.evaluate(element => element.inert)).toBe(true);
	await expect.poll(() => hasIntermediateOpacityMotion(modal)).toBe(true);
	await waitForOwnedAnimations(modal);
});
