const test = require('node:test');
const assert = require('node:assert/strict');

const { typeIntoCodeMirror } = require('../tests/workbench-test-helpers.js');

test('typeIntoCodeMirror clears editors with a cross-platform select-all shortcut', async () => {
    const calls = [];
    const page = {
        locator(selector) {
            calls.push(['locator', selector]);
            return {
                nth(index) {
                    calls.push(['nth', index]);
                    return {
                        async click() {
                            calls.push(['click']);
                        }
                    };
                }
            };
        },
        keyboard: {
            async press(key) {
                calls.push(['press', key]);
            },
            async type(value) {
                calls.push(['type', value]);
            }
        },
        async waitForFunction(_fn, args) {
            calls.push(['waitForFunction', args]);
        }
    };

    await typeIntoCodeMirror(page, 1, 'ASK { ?s ?p ?o }');

    assert.deepEqual(calls, [
        ['locator', '.CodeMirror'],
        ['nth', 1],
        ['click'],
        ['press', 'ControlOrMeta+A'],
        ['press', 'Backspace'],
        ['type', 'ASK { ?s ?p ?o }'],
        ['waitForFunction', [1, 'ASK { ?s ?p ?o }']]
    ]);
});
