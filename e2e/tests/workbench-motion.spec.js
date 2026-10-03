// @ts-check
const { test, expect } = require('@playwright/test');
const { serverBaseUrl, waitForRoute, workbenchBaseUrl } = require('./workbench-test-helpers.js');

// Retired with the redesign (see .agent/execplans/workbench-stale-spec-migration-20261002.md): "a new query reverses the
// loading indicator exit animation" and the result fade-in that runPrimaryQuery checked belonged to the result iframe
// execution path, removed on purpose (plan workbench-app-shell-and-critique-fixes-20260930, Decision M9.1). The streamed
// renderer shows and hides #query-results-loading with its busy state, without motion, so there is no exit animation
// to reverse; workbench-spacing-audit.spec.js "query output controls ..." checks the loading line while a request is
// pending. Menu groups are no longer disclosures (same plan, M2.4), so the native <details> motion that the menu groups
// showed is checked on the Remove page's Examples disclosure, which uses the same shared code (decoratePage).

const WORKBENCH_BASE_URL = workbenchBaseUrl();
const SERVER_BASE_URL = serverBaseUrl();
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

/** Open a page and wait until its view is mounted: the page renders before its controls are bound. */
async function openRoute(page, url, viewId) {
	await page.goto(url);
	await waitForRoute(page, viewId);
}

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

/**
 * Watches an element on every animation frame for up to two seconds and remembers whether it was seen mid-motion: a
 * running animation with its height ('height'), its open details height ('details') or its opacity ('opacity')
 * between the end states. A short eased motion can finish between two polls from the test process, so the sampling
 * happens in the page. Start recording before the action that starts the motion.
 */
async function recordIntermediateMotion(locator, kind = 'height') {
	await locator.evaluate((element, motionKind) => {
		const record = { seen: false };
		element.__workbenchMotionRecord = record;
		const deadline = performance.now() + 2000;
		const intermediate = () => {
			if (motionKind === 'opacity') {
				const opacity = parseFloat(getComputedStyle(element).opacity);
				return opacity > 0.02 && opacity < 0.98;
			}
			const summary = motionKind === 'details' ? element.querySelector(':scope > summary') : null;
			const lower = summary ? summary.getBoundingClientRect().height : 0;
			const height = element.getBoundingClientRect().height;
			return height > lower + 1 && height < element.scrollHeight - 1;
		};
		const sample = () => {
			const running = element.getAnimations({ subtree: false })
				.some(animation => animation.playState === 'running');
			if (running && intermediate()) {
				record.seen = true;
			} else if (performance.now() < deadline) {
				requestAnimationFrame(sample);
			}
		};
		requestAnimationFrame(sample);
	}, kind);
}

async function wasSeenInIntermediateMotion(locator) {
	return locator.evaluate(element => !!(element.__workbenchMotionRecord && element.__workbenchMotionRecord.seen));
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

async function waitForOwnedAnimations(locator) {
	await expect.poll(() => activeMotionCount(locator)).toBe(0);
}

async function runPrimaryQuery(page) {
	await page.locator('.CodeMirror').first().evaluate(element =>
		element.CodeMirror.setValue('SELECT ?s WHERE { ?s ?p ?o }'));
	await page.locator('#exec').click();
	const results = page.locator('#query-results');
	await expect(results).toBeVisible();
	// The result streams into the page (the fade-in belonged to the removed result iframe path, see the note above).
	await expect(results.locator('[data-query-stream-root]')).toBeVisible();
	await expect(results).toHaveAttribute('aria-busy', 'false');
	await expect(results).toBeVisible();
	await page.locator('#explain-trigger').click();
	await expect.poll(() => page.locator('#query-explanation').textContent()).toMatch(/\S/);
	await expect(page.locator('#compare-toggle')).toBeVisible();
}

test('query explanation grows into view when Explain opens it', async ({ page }) => {
	await page.emulateMedia({ reducedMotion: 'no-preference' });
	await page.setViewportSize({ width: 1440, height: 1000 });
	await openRoute(page, QUERY_URL, 'query');
	await page.locator('.CodeMirror').first().evaluate(element =>
		element.CodeMirror.setValue('SELECT ?s WHERE { ?s ?p ?o }'));
	const explanationRow = page.locator('#query-explanation-row');
	await expect(explanationRow).toBeHidden();

	const opening = await explanationRow.evaluate(element => {
		document.getElementById('explain-trigger').click();
		const animation = element.getAnimations({ subtree: false })
			.find(candidate => candidate.playState === 'running');
		return {
			visible: getComputedStyle(element).display !== 'none',
			keyframes: animation ? animation.effect.getKeyframes() : []
		};
	});
	expect(opening.visible).toBe(true);
	expect(opening.keyframes.length).toBeGreaterThan(1);
	expect(parseFloat(opening.keyframes[0].height)).toBe(0);
	await expect.poll(() => hasVisibleIntermediateMotion(explanationRow)).toBe(true);
	await expect(page.locator('#query-explanation')).toContainText(/\S/);
	await waitForOwnedAnimations(explanationRow);
	await expect(explanationRow).toBeVisible();
	expect(await retainedForwardFillCount(explanationRow)).toBe(0);
});

test('comparison explanation reverses cleanly and respects reduced motion', async ({ page }) => {
	await page.emulateMedia({ reducedMotion: 'no-preference' });
	await openRoute(page, QUERY_URL, 'query');
	await page.locator('.CodeMirror').first().evaluate(element =>
		element.CodeMirror.setValue('SELECT ?s WHERE { ?s ?p ?o }'));
	await page.locator('#explain-trigger').click();
	await expect(page.locator('#query-explanation')).toContainText(/\S/);
	await expect(page.locator('#compare-toggle')).toBeVisible();

	const compareRow = page.locator('#query-explanation-row-compare');
	await expect(compareRow).toBeHidden();
	await recordIntermediateMotion(compareRow, 'opacity');
	const opening = await compareRow.evaluate(element => new Promise(resolve => {
		document.getElementById('compare-toggle').click();
		// The motion may start with the next frame; its keyframes are fixed once it exists.
		const deadline = performance.now() + 1000;
		const read = () => {
			const animation = element.getAnimations({ subtree: false })
				.find(candidate => candidate.playState === 'running');
			if (animation || performance.now() > deadline) {
				resolve(animation ? animation.effect.getKeyframes() : []);
			} else {
				requestAnimationFrame(read);
			}
		};
		read();
	}));
	expect(opening.length).toBeGreaterThan(1);
	expect(parseFloat(opening[0].height)).toBe(0);
	expect(parseFloat(opening[0].opacity)).toBe(0);
	await expect(compareRow).toHaveAttribute('aria-hidden', 'false');
	await expect.poll(() => wasSeenInIntermediateMotion(compareRow)).toBe(true);

	await page.locator('#compare-toggle').evaluate(element => element.click());
	await expect(compareRow).toBeHidden();
	await expect(compareRow).toHaveAttribute('aria-hidden', 'true');
	expect(await activeMotionCount(compareRow)).toBe(0);
	expect(await retainedForwardFillCount(compareRow)).toBe(0);

	await page.locator('#compare-toggle').evaluate(element => element.click());
	expect(await activeMotionCount(compareRow)).toBeGreaterThan(0);
	await waitForOwnedAnimations(compareRow);
	await expect(compareRow).toBeVisible();
	expect(await retainedForwardFillCount(compareRow)).toBe(0);

	await page.locator('#compare-toggle').evaluate(element => element.click());
	await page.emulateMedia({ reducedMotion: 'reduce' });
	await page.locator('#compare-toggle').evaluate(element => element.click());
	await expect(compareRow).toBeVisible();
	expect(await activeMotionCount(compareRow)).toBe(0);
	await expect.poll(() => compareRow.evaluate(element => element.inert)).toBe(false);
});

test('an initial explanation error settles in the animated row', async ({ page }) => {
	await page.emulateMedia({ reducedMotion: 'no-preference' });
	await page.route(`**/repositories/${REPOSITORY_ID}/query**`, async route => {
		if (route.request().method() === 'POST'
				&& new URLSearchParams(route.request().postData() || '').get('action') === 'explain') {
			await route.fulfill({
				status: 500,
				contentType: 'application/json',
				body: JSON.stringify({ error: 'Explanation unavailable' })
			});
		} else {
			await route.continue();
		}
	});
	await openRoute(page, QUERY_URL, 'query');
	await page.locator('.CodeMirror').first().evaluate(element =>
		element.CodeMirror.setValue('SELECT ?s WHERE { ?s ?p ?o }'));
	const explanationRow = page.locator('#query-explanation-row');
	const opening = await explanationRow.evaluate(element => {
		document.getElementById('explain-trigger').click();
		return element.getAnimations({ subtree: false })
			.some(animation => animation.playState === 'running');
	});
	expect(opening).toBe(true);
	await expect(page.locator('#query-explanation')).toContainText('Explanation unavailable');
	await waitForOwnedAnimations(explanationRow);
	await expect(explanationRow).toBeVisible();
	await expect(explanationRow).toHaveAttribute('aria-hidden', 'false');
	expect(await retainedForwardFillCount(explanationRow)).toBe(0);
});

test('reduced motion reveals the initial explanation without animation', async ({ page }) => {
	await page.emulateMedia({ reducedMotion: 'reduce' });
	await openRoute(page, QUERY_URL, 'query');
	await page.locator('.CodeMirror').first().evaluate(element =>
		element.CodeMirror.setValue('SELECT ?s WHERE { ?s ?p ?o }'));
	const state = await page.locator('#query-explanation-row').evaluate(element => {
		document.getElementById('explain-trigger').click();
		return {
			visible: getComputedStyle(element).display !== 'none',
			ariaHidden: element.getAttribute('aria-hidden'),
			inert: element.inert,
			animations: element.getAnimations({ subtree: false }).length
		};
	});
	expect(state).toEqual({ visible: true, ariaHidden: 'false', inert: false, animations: 0 });
	await expect(page.locator('#query-explanation')).toContainText(/\S/);
});

test('refreshing an open explanation does not restart its entrance motion', async ({ page }) => {
	await page.emulateMedia({ reducedMotion: 'no-preference' });
	await openRoute(page, QUERY_URL, 'query');
	await page.locator('.CodeMirror').first().evaluate(element =>
		element.CodeMirror.setValue('SELECT ?s WHERE { ?s ?p ?o }'));
	await page.locator('#explain-trigger').click();
	const explanationRow = page.locator('#query-explanation-row');
	await expect(page.locator('#query-explanation')).toContainText(/\S/);
	await waitForOwnedAnimations(explanationRow);
	await expect(page.locator('#rerun-explanation')).toBeEnabled();
	const previousText = await page.locator('#query-explanation').textContent();

	let releaseRefresh;
	const refreshGate = new Promise(resolve => { releaseRefresh = resolve; });
	await page.route(`**/repositories/${REPOSITORY_ID}/query**`, async route => {
		if (route.request().method() === 'POST'
				&& new URLSearchParams(route.request().postData() || '').get('action') === 'explain') {
			await refreshGate;
		}
		await route.continue();
	});
	try {
		const duringRefresh = await explanationRow.evaluate(element => {
			document.getElementById('rerun-explanation').click();
			return {
				visible: getComputedStyle(element).display !== 'none',
				opacity: Number(getComputedStyle(element).opacity),
				height: element.getBoundingClientRect().height,
				animations: element.getAnimations({ subtree: false }).length
			};
		});
		expect(duringRefresh.visible).toBe(true);
		expect(duringRefresh.opacity).toBe(1);
		expect(duringRefresh.height).toBeGreaterThan(0);
		expect(duringRefresh.animations).toBe(0);
		await expect(page.locator('#query-explanation')).toHaveText(previousText);
		await expect(page.locator('#query-explanation-overlay')).toContainText('Refreshing explanation...');
	} finally {
		releaseRefresh();
	}
	await expect(page.locator('#query-explanation-overlay')).toBeHidden();
	await expect(page.locator('#query-explanation')).toHaveText(previousText);
	expect(await activeMotionCount(explanationRow)).toBe(0);
});

test('closing a focused comparison explanation returns focus to Compare', async ({ page }) => {
	await openRoute(page, QUERY_URL, 'query');
	await page.locator('.CodeMirror').first().evaluate(element =>
		element.CodeMirror.setValue('SELECT ?s WHERE { ?s ?p ?o }'));
	await page.locator('#explain-trigger').click();
	await expect(page.locator('#query-explanation')).toContainText(/\S/);
	await page.locator('#compare-toggle').click();
	await expect(page.locator('#query-explanation-compare')).toContainText(/\S/);
	const compareCopy = page.locator('#copy-explanation-compare');
	// "Loading explanation..." already matches the text check above; Copy is enabled once the explanation arrived.
	await expect(compareCopy).toBeEnabled();
	await compareCopy.focus();
	await expect(compareCopy).toBeFocused();
	await page.evaluate(() => workbench.query.toggleCompareMode());
	await expect(page.locator('#query-explanation-row-compare')).toBeHidden();
	await expect(page.locator('#compare-toggle')).toBeFocused();
});

test('query options animate reversibly while semantic state and focus update immediately', async ({ page }) => {
	await page.setViewportSize({ width: 1440, height: 1000 });
	await page.emulateMedia({ reducedMotion: 'no-preference' });
	await openRoute(page, QUERY_URL, 'query');

	const toggle = page.locator('#query-options-toggle');
	const panel = page.locator('#query-options-panel');
	await expect(toggle).toBeVisible();
	await expect(toggle).toHaveAttribute('aria-expanded', 'false');
	await recordIntermediateMotion(panel);
	await toggle.focus();
	await page.keyboard.press('Enter');
	await expect(toggle).toHaveAttribute('aria-expanded', 'true');
	await expect(panel).toBeVisible();
	await expect.poll(() => wasSeenInIntermediateMotion(panel)).toBe(true);
	await waitForOwnedAnimations(panel);

	// The result limit left Query settings (results stream whole and "Load more" continues them); the timeout is its
	// first control now.
	await panel.locator('#query-timeout').focus();
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
	await openRoute(page, QUERY_URL, 'query');
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

test('native disclosures and compare panes animate their outer content only', async ({ page }) => {
	await page.emulateMedia({ reducedMotion: 'no-preference' });
	await page.setViewportSize({ width: 1440, height: 1000 });
	await openRoute(page, `${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/remove`, 'remove');

	const examples = page.locator('#remove-examples');
	const examplesSummary = examples.locator(':scope > summary');
	const examplesItems = examples.locator(':scope > ul');
	await expect(examplesSummary).toHaveAttribute('aria-expanded', 'false');
	await expect.poll(() => examples.evaluate(element => element.open)).toBe(false);
	await examplesSummary.focus();
	await recordIntermediateMotion(examples, 'details');
	await page.keyboard.press('Enter');
	await expect.poll(() => examples.evaluate(element => element.open)).toBe(true);
	await expect.poll(() => wasSeenInIntermediateMotion(examples)).toBe(true);
	await recordIntermediateMotion(examples, 'details');
	await examplesSummary.press('Enter');
	await expect(examplesSummary).toHaveAttribute('aria-expanded', 'false');
	await expect.poll(() => examplesItems.evaluate(element => element.inert)).toBe(true);
	await expect.poll(() => wasSeenInIntermediateMotion(examples)).toBe(true);
	await waitForOwnedAnimations(examples);
	await expect.poll(() => examples.evaluate(element => element.open)).toBe(false);

	await openRoute(page, QUERY_URL, 'query');
	await runPrimaryQuery(page);
	const comparePane = page.locator('#query-compare-pane');
	await recordIntermediateMotion(comparePane, 'opacity');
	await page.locator('#compare-toggle').click();
	await expect(comparePane).toBeVisible();
	await expect.poll(() => wasSeenInIntermediateMotion(comparePane)).toBe(true);
	await waitForOwnedAnimations(comparePane);
	const compareLayout = page.locator('#query-compare-layout');
	await expect.poll(() => compareLayout.evaluate(element =>
		element.getAnimations().some(animation => animation.playState === 'running'))).toBe(false);
	expect(parseFloat(await compareLayout.evaluate(element => getComputedStyle(element).columnGap))).toBeCloseTo(16, 1);
	await recordIntermediateMotion(comparePane, 'opacity');
	await page.locator('#query-compare-close').click();
	await expect(page.locator('#compare-toggle')).toBeFocused();
	await expect(comparePane).toHaveAttribute('aria-hidden', 'true');
	expect(await comparePane.evaluate(element => element.inert)).toBe(true);
	await expect.poll(() => wasSeenInIntermediateMotion(comparePane)).toBe(true);
	await waitForOwnedAnimations(comparePane);
	await expect.poll(() => compareLayout.evaluate(element =>
		element.getAnimations().some(animation => animation.playState === 'running'))).toBe(false);
	await expect.poll(() => compareLayout.evaluate(element =>
		parseFloat(getComputedStyle(element).columnGap))).toBeLessThan(0.01);
});

/** Record the widest horizontal page overflow on every frame until the compare layout has settled. */
async function recordPageOverflowUntilSettled(page) {
	await page.evaluate(() => {
		const layout = document.querySelector('#query-compare-layout');
		const recording = { widest: 0, moved: false, done: false };
		window.__comparePageOverflow = recording;
		const started = performance.now();
		const sample = () => {
			const root = document.documentElement;
			recording.widest = Math.max(recording.widest, root.scrollWidth - root.clientWidth);
			const moving = layout.getAnimations({ subtree: true }).some(animation => animation.playState === 'running');
			recording.moved = recording.moved || moving;
			if ((moving || !recording.moved) && performance.now() - started < 5000) {
				requestAnimationFrame(sample);
			} else {
				recording.done = true;
			}
		};
		requestAnimationFrame(sample);
	});
}

test('opening and closing the comparison never widens the page', async ({ page }) => {
	await page.emulateMedia({ reducedMotion: 'no-preference' });
	await page.setViewportSize({ width: 1440, height: 1000 });
	await openRoute(page, QUERY_URL, 'query');
	await runPrimaryQuery(page);
	const recorded = () => page.evaluate(() => window.__comparePageOverflow);

	await recordPageOverflowUntilSettled(page);
	await page.locator('#compare-toggle').click();
	await expect.poll(async () => (await recorded()).done).toBe(true);
	const opening = await recorded();
	expect(opening.moved, 'the comparison opens with motion').toBe(true);
	expect(opening.widest, 'page overflow while the comparison opens').toBeLessThanOrEqual(1);

	await recordPageOverflowUntilSettled(page);
	await page.locator('#query-compare-close').click();
	await expect.poll(async () => (await recorded()).done).toBe(true);
	const closing = await recorded();
	expect(closing.moved, 'the comparison closes with motion').toBe(true);
	expect(closing.widest, 'page overflow while the comparison closes').toBeLessThanOrEqual(1);
});

test('completed disclosures release their fill effects and follow natural sizing', async ({ page }) => {
	await page.setViewportSize({ width: 1440, height: 1000 });
	await openRoute(page, QUERY_URL, 'query');
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
	await openRoute(page, `${WORKBENCH_BASE_URL}/repositories/NONE/create?type=memory`, 'create');
	const advancedToggle = page.locator('#create-advanced-toggle');
	const advancedPanel = page.locator('#create-advanced-panel');
	await advancedToggle.click();
	await waitForOwnedAnimations(advancedPanel);
	expect.soft(await retainedForwardFillCount(advancedPanel)).toBe(0);
	const advancedHeight = await advancedPanel.evaluate(element => element.getBoundingClientRect().height);
	await advancedPanel.evaluate(element => {
		const probe = document.createElement('div');
		probe.setAttribute('data-motion-growth-probe', '');
		probe.style.cssText = 'display:block;height:72px;box-sizing:border-box';
		element.appendChild(probe);
	});
	await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => resolve(undefined))));
	const grownHeight = await advancedPanel.evaluate(element => element.getBoundingClientRect().height);
	expect.soft(grownHeight - advancedHeight).toBeGreaterThanOrEqual(70);
});

test('primary query and form actions show compact keyboard press feedback in both themes', async ({ page, browserName }) => {
	test.skip(browserName === 'firefox', 'Firefox does not match :active while a keyboard Space press is held');
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
		await openRoute(page, QUERY_URL, 'query');
		await page.evaluate(theme => window.RDF4JWorkbenchTheme.setPreference(theme), theme);
		await expect(page.locator('html')).toHaveAttribute('data-theme', theme);
		await page.evaluate(() => {
			document.addEventListener('click', event => {
				if (event.target.closest('#exec, #create')) {
					event.preventDefault();
					event.stopImmediatePropagation();
				}
			}, true);
		});
		// Execute uses the shared button component (.query-action--primary became .workbench-action--primary, M1.3).
		await verifyPressedAction('#exec', '.workbench-action--primary', theme);

		await openRoute(page, `${WORKBENCH_BASE_URL}/repositories/NONE/create?type=memory`, 'create');
		await page.evaluate(theme => window.RDF4JWorkbenchTheme.setPreference(theme), theme);
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

test('shared form and mobile navigation disclosures reverse within narrow layouts', async ({ page }) => {
	await page.emulateMedia({ reducedMotion: 'no-preference' });
	await page.setViewportSize({ width: 390, height: 844 });
	await openRoute(page, `${WORKBENCH_BASE_URL}/repositories/NONE/create?type=memory`, 'create');

	const advancedToggle = page.locator('#create-advanced-toggle');
	const advancedPanel = page.locator('#create-advanced-panel');
	await expect(advancedToggle).toBeVisible();
	await expect(advancedToggle).toHaveAttribute('aria-expanded', 'false');
	await expect(advancedPanel).toBeHidden();
	await advancedToggle.focus();
	await page.keyboard.press('Enter');
	expect(await activeMotionCount(advancedPanel)).toBeGreaterThan(0);
	await expect(advancedToggle).toHaveAttribute('aria-expanded', 'true');
	await expect(advancedPanel).toHaveAttribute('aria-hidden', 'false');
	await expect.poll(() => advancedPanel.evaluate(element => element.inert)).toBe(false);
	await expect.poll(() => hasVisibleIntermediateMotion(advancedPanel)).toBe(true);
	await waitForOwnedAnimations(advancedPanel);
	const firstControl = advancedPanel.locator('input:not([type="hidden"]), select, textarea').first();
	await firstControl.focus();
	await advancedToggle.click();
	await expect(advancedToggle).toHaveAttribute('aria-expanded', 'false');
	await expect(advancedToggle).toBeFocused();
	await expect(advancedPanel).toHaveAttribute('aria-hidden', 'true');
	await expect.poll(() => advancedPanel.evaluate(element => element.inert)).toBe(true);
	await expect.poll(() => hasVisibleIntermediateMotion(advancedPanel)).toBe(true);
	const advancedReverse = await advancedPanel.evaluate(element => {
		const before = element.getBoundingClientRect().height;
		document.getElementById('create-advanced-toggle').click();
		const animation = element.getAnimations({ subtree: false })
			.find(candidate => candidate.playState === 'running');
		const frames = animation.effect.getKeyframes();
		const style = getComputedStyle(element);
		const borderBoxInsets = parseFloat(style.paddingTop) + parseFloat(style.paddingBottom)
			+ parseFloat(style.borderTopWidth) + parseFloat(style.borderBottomWidth);
		const firstKeyframeHeight = parseFloat(frames[0].height);
		return {
			before,
			firstKeyframeHeight,
			firstKeyframeBorderBoxHeight: style.boxSizing === 'border-box'
					? firstKeyframeHeight
					: firstKeyframeHeight + borderBoxInsets,
			boxSizing: style.boxSizing,
			borderBoxInsets,
			currentHeight: element.getBoundingClientRect().height
		};
	});
	await expect(advancedToggle).toHaveAttribute('aria-expanded', 'true');
	await expect(advancedPanel).toHaveAttribute('aria-hidden', 'false');
	await expect.poll(() => advancedPanel.evaluate(element => element.inert)).toBe(false);
	console.log(`SHARED_DISCLOSURE_REVERSE_BOX_MODEL ${JSON.stringify(advancedReverse)}`);
	expect(Math.abs(advancedReverse.firstKeyframeBorderBoxHeight - advancedReverse.before)).toBeLessThanOrEqual(0.001);
	expect(advancedReverse.currentHeight).toBeGreaterThan(0);
	await waitForOwnedAnimations(advancedPanel);
	await advancedToggle.click();
	await waitForOwnedAnimations(advancedPanel);
	await expect(advancedPanel).toBeHidden();

	await page.setViewportSize({ width: 320, height: 844 });
	await openRoute(page, `${WORKBENCH_BASE_URL}/repositories/${REPOSITORY_ID}/add`, 'add');
	const importToggle = page.locator('#add-import-settings-toggle');
	const importPanel = page.locator('#add-import-settings-panel');
	await expect(importPanel).toBeHidden();
	await importToggle.click();
	await expect(importToggle).toHaveAttribute('aria-expanded', 'true');
	await expect(importPanel).toHaveAttribute('aria-hidden', 'false');
	await expect.poll(() => importPanel.evaluate(element => element.inert)).toBe(false);
	await expect.poll(() => hasVisibleIntermediateMotion(importPanel)).toBe(true);
	await waitForOwnedAnimations(importPanel);
	await importToggle.click();
	await expect(importToggle).toHaveAttribute('aria-expanded', 'false');
	await expect(importPanel).toHaveAttribute('aria-hidden', 'true');
	await expect.poll(() => importPanel.evaluate(element => element.inert)).toBe(true);
	await expect.poll(() => hasVisibleIntermediateMotion(importPanel)).toBe(true);
	await waitForOwnedAnimations(importPanel);
	await expect(importPanel).toBeHidden();

	// On narrow screens the menu is a modal sheet opened from the context bar.
	const menuButton = page.locator('#workbench-menu-button');
	await menuButton.click();
	await expect(menuButton).toHaveAttribute('aria-expanded', 'true');
	await expect(page.locator('#workbench-menu-sheet')).toBeVisible();
	await page.keyboard.press('Escape');
	await expect(menuButton).toHaveAttribute('aria-expanded', 'false');

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
	await openRoute(page, QUERY_URL, 'query');
	const compareToggle = page.locator('#compare-toggle');
	await runPrimaryQuery(page);
	await compareToggle.click();
	await page.locator('.CodeMirror').nth(1).evaluate(element =>
		element.CodeMirror.setValue('SELECT * WHERE { ?s ?p ?o }'));
	const diffTrigger = page.locator('#query-diff-trigger');
	await expect(diffTrigger).toBeEnabled();
	const modal = page.locator('#query-diff-modal');
	await recordIntermediateMotion(modal, 'opacity');
	await diffTrigger.click();

	await expect(modal).toHaveAttribute('aria-hidden', 'false');
	await expect.poll(() => wasSeenInIntermediateMotion(modal)).toBe(true);
	await waitForOwnedAnimations(modal);
	await recordIntermediateMotion(modal, 'opacity');
	await page.locator('#query-diff-close').click();
	await expect(modal).toHaveAttribute('aria-hidden', 'true');
	await expect(page.locator('#query-diff-trigger')).toBeFocused();
	expect(await modal.evaluate(element => element.inert)).toBe(true);
	await expect.poll(() => wasSeenInIntermediateMotion(modal)).toBe(true);
	await waitForOwnedAnimations(modal);
});
