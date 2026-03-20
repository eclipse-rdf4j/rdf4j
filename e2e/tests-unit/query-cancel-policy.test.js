const test = require('node:test');
const assert = require('node:assert/strict');

const queryCancelPolicy = require('../../tools/workbench/src/main/webapp/scripts/queryCancelPolicy.js');

test('uses compare cancel while compare refresh is pending', () => {
    assert.equal(queryCancelPolicy.getExplainCancelAction(2, false), 'compare');
});

test('maps explain trigger controls', () => {
    assert.deepEqual(queryCancelPolicy.getExplainControlIds('explain-trigger'), {
        buttonId: 'explain-trigger',
        spinnerId: 'explain-trigger-spinner',
        cancelId: 'explain-trigger-cancel'
    });
});

test('maps rerun explanation controls', () => {
    assert.deepEqual(queryCancelPolicy.getExplainControlIds('rerun-explanation'), {
        buttonId: 'rerun-explanation',
        spinnerId: 'rerun-explanation-spinner',
        cancelId: 'rerun-explanation-cancel'
    });
});

test('returns empty control ids for non-primary buttons', () => {
    assert.deepEqual(queryCancelPolicy.getExplainControlIds('explain-compare-trigger'), {
        buttonId: '',
        spinnerId: '',
        cancelId: ''
    });
});

test('uses primary cancel when only primary explain is pending', () => {
    assert.equal(queryCancelPolicy.getExplainCancelAction(0, true), 'primary');
});

test('prefers compare cancel over primary when both flags are present', () => {
    assert.equal(queryCancelPolicy.getExplainCancelAction(1, true), 'compare');
});

test('returns none when nothing is pending', () => {
    assert.equal(queryCancelPolicy.getExplainCancelAction(0, false), 'none');
});

test('shows mirrored primary wait state only for matching compare refresh targets', () => {
    assert.equal(
        queryCancelPolicy.shouldShowComparePrimaryWaitState('explain-trigger', 1, true, 'explain-trigger'),
        true
    );
    assert.equal(
        queryCancelPolicy.shouldShowComparePrimaryWaitState('rerun-explanation', 1, true, 'rerun-explanation'),
        true
    );
});

test('does not show mirrored primary wait state without active compare request', () => {
    assert.equal(
        queryCancelPolicy.shouldShowComparePrimaryWaitState('explain-trigger', 1, false, 'explain-trigger'),
        false
    );
});

test('does not show mirrored primary wait state for mismatched or non-primary targets', () => {
    assert.equal(
        queryCancelPolicy.shouldShowComparePrimaryWaitState('explain-trigger', 1, true, 'rerun-explanation'),
        false
    );
    assert.equal(
        queryCancelPolicy.shouldShowComparePrimaryWaitState('explain-compare-trigger', 1, true, 'explain-compare-trigger'),
        false
    );
    assert.equal(
        queryCancelPolicy.shouldShowComparePrimaryWaitState('rerun-explanation', 0, true, 'rerun-explanation'),
        false
    );
});
