// Review fixes (TS-A): stylesheet rules found by the CSS sweep.
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const styles = path.resolve(__dirname, '../../tools/workbench/src/main/webapp/styles');
const css = (name) => fs.readFileSync(path.join(styles, name), 'utf8').replace(/\/\*[\s\S]*?\*\//g, '');

/** Top-level rules (selector list and body) of a stylesheet, or of the blocks of one at-rule. */
function rules(source) {
    const found = [];
    let depth = 0;
    let start = 0;
    let selector = '';
    for (let index = 0; index < source.length; index++) {
        const character = source[index];
        if (character === '{') {
            if (depth === 0) { selector = source.slice(start, index).trim(); start = index + 1; }
            depth++;
        } else if (character === '}') {
            depth--;
            if (depth === 0) { found.push({ selector, body: source.slice(start, index) }); start = index + 1; }
        }
    }
    return found;
}

function selectorsOf(rule) {
    const list = [];
    let depth = 0;
    let current = '';
    for (const character of rule.selector) {
        if (character === '(') { depth++; }
        if (character === ')') { depth--; }
        if (character === ',' && depth === 0) { list.push(current.replace(/\s+/g, ' ').trim()); current = ''; continue; }
        current += character;
    }
    list.push(current.replace(/\s+/g, ' ').trim());
    return list;
}

function zIndexOf(source, selectorPattern) {
    const rule = rules(source).find((candidate) => selectorsOf(candidate).some((selector) => selectorPattern.test(selector))
        && /z-index:/.test(candidate.body));
    return rule ? Number(/z-index:\s*(\d+)/.exec(rule.body)[1]) : null;
}

test('A33: the completion list stays above a full-screen editor', () => {
    const refresh = css('workbench-refresh.css');
    const fullscreen = zIndexOf(refresh, /\.yasqe \.CodeMirror-fullscreen$/);
    assert.ok(fullscreen, 'the full-screen editor has a stacking level');
    const hints = zIndexOf(refresh, /\.CodeMirror-hints$/);
    assert.ok(hints !== null, 'the Workbench sets the stacking level of the completion list (yasqe.min.css uses 10)');
    assert.ok(hints > fullscreen, `hints (${hints}) above the full-screen editor (${fullscreen})`);
});

test('A35: under reduced motion every press-feedback transition is switched off by a rule that wins', () => {
    const refresh = css('workbench-refresh.css');
    const all = rules(refresh);
    const reduced = all.filter((rule) => /prefers-reduced-motion:\s*reduce/.test(rule.selector))
        .flatMap((block) => rules(block.body))
        .filter((rule) => /transition:\s*none/.test(rule.body))
        .flatMap(selectorsOf);
    const motion = all.filter((rule) => !rule.selector.startsWith('@') && /var\(--workbench-motion-press\)/.test(rule.body)
        && /transition:/.test(rule.body)).flatMap(selectorsOf);
    assert.ok(motion.length > 4, 'the press-feedback rules are found');
    const missing = motion.filter((selector) => reduced.indexOf(selector) < 0);
    assert.deepEqual(missing, [], 'each press-feedback selector is repeated (same specificity, later) with transition: none');
});

// Round 2 (R15): a browser without :has() drops a whole selector list that contains one; the reduced-motion rules keep
// their :has() selectors in lists of their own, so the other controls still lose their transitions there.
test('no reduced-motion rule mixes :has() selectors with selectors that work without it', () => {
    const refresh = css('workbench-refresh.css');
    const mixed = rules(refresh).filter((rule) => /prefers-reduced-motion:\s*reduce/.test(rule.selector))
        .flatMap((block) => rules(block.body))
        .map(selectorsOf)
        .filter((list) => list.some((selector) => selector.includes(':has('))
            && list.some((selector) => !selector.includes(':has(')));
    assert.deepEqual(mixed, []);
});

function bodyOf(source, selector) {
    const rule = rules(source).find((candidate) => selectorsOf(candidate).indexOf(selector) >= 0);
    return rule ? rule.body : '';
}

test('A36: the diff modal shows added and removed lines and their changed words apart from context', () => {
    const compare = css('query-compare.css');
    const added = bodyOf(compare, '.query-diff-row--added');
    const removed = bodyOf(compare, '.query-diff-row--removed');
    const context = bodyOf(compare, '.query-diff-row--context');
    assert.doesNotMatch(added, /background:\s*var\(--workbench-surface\)/, 'an added line is not on the plain surface');
    assert.doesNotMatch(removed, /background:\s*var\(--workbench-surface\)/, 'a removed line is not on the plain surface');
    assert.doesNotMatch(context, /background:\s*var\(--workbench-(selected|danger-surface)\)/);
    for (const kind of ['added', 'removed']) {
        const changed = bodyOf(compare, `.query-diff-row--${kind} .query-diff-row__segment--changed`);
        assert.doesNotMatch(changed, /var\(--workbench-inset\)/, kind + ': the inset token is invisible on these rows');
        assert.match(changed, /background:\s*color-mix\(in srgb, var\(--workbench-(teal|danger)\)/,
            kind + ': the changed words are tinted with the line colour (both themes)');
        assert.match(changed, /font-weight:\s*600/, kind + ': and marked by weight, not by colour alone');
    }
});

test('A37: the navigation list has no whitespace of its own, so an empty menu matches :empty', () => {
    const views = fs.readFileSync(path.resolve(__dirname,
        '../../tools/workbench/src/main/webapp/scripts/ts/workbenchViews.ts'), 'utf8');
    assert.match(views, /<div id="navigation" class="workbench-nav"><ul class="maingroup">\$\{\s*navigation\([^}]*\)\}<\/ul>/,
        'nothing but the navigation items between <ul class="maingroup"> and </ul>');
});

/** CSS specificity of one selector as [ids, classes/attributes/pseudo-classes, elements/pseudo-elements]. */
function specificity(selector) {
    let rest = selector.replace(/::?[a-z-]+\([^)]*\)/g, (match) => match.startsWith('::') ? ' ::x' : ' .x');
    const ids = (rest.match(/#[\w-]+/g) || []).length;
    rest = rest.replace(/#[\w-]+/g, ' ');
    const classes = (rest.match(/\.[\w-]+|\[[^\]]*\]|:[\w-]+/g) || []).length;
    rest = rest.replace(/\.[\w-]+|\[[^\]]*\]|::?[\w-]+/g, ' ');
    const elements = (rest.match(/(^|[\s>+~])[a-z][\w-]*/gi) || []).length;
    return [ids, classes, elements];
}

function outranks(left, right) {
    for (let index = 0; index < 3; index++) {
        if (left[index] !== right[index]) { return left[index] > right[index]; }
    }
    return false;
}

// The dark theme's editor cursor: yasqe.min.css (loaded after workbench-refresh.css) paints the CodeMirror cursor black
// with `.yasqe .CodeMirror div.CodeMirror-cursor`, so the Workbench rule that colours it with the ink must outrank it
// for the Workbench and the embedded result document.
test('the editor cursor takes the ink colour with a rule that outranks yasqe.min.css in both documents', () => {
    const refresh = css('workbench-refresh.css');
    const yasqe = specificity('.yasqe .CodeMirror div.CodeMirror-cursor');
    assert.deepEqual(yasqe, [0, 3, 1], 'the specificity helper reads yasqe\'s selector');
    const winners = rules(refresh)
        .filter((rule) => /border-left-color:\s*var\(--workbench-ink\)/.test(rule.body))
        .flatMap(selectorsOf)
        .filter((selector) => /CodeMirror-cursor$/.test(selector) && outranks(specificity(selector), yasqe));
    for (const body of ['body.workbench-body', 'body.query-result-embedded-body']) {
        assert.ok(winners.some((selector) => selector.startsWith(body + ' ')),
            body + ': a cursor rule with the ink colour outranks yasqe (found: ' + JSON.stringify(winners) + ')');
    }
});
