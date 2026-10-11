const test = require('node:test');
const assert = require('node:assert/strict');
const childProcess = require('node:child_process');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const compilerScript = path.resolve(__dirname, '../../tools/workbench/compileTypescript.sh');

test('Workbench TypeScript build script propagates compiler failures', () => {
    const temporaryDirectory = fs.mkdtempSync(path.join(os.tmpdir(), 'rdf4j-workbench-tsc-failure-'));
    try {
        const fakeCompiler = path.join(temporaryDirectory, 'tsc');
        fs.writeFileSync(fakeCompiler, '#!/bin/sh\nexit 23\n', { mode: 0o755 });
        const result = childProcess.spawnSync('bash', [compilerScript], {
            encoding: 'utf8',
            env: Object.assign({}, process.env, {
                PATH: temporaryDirectory + path.delimiter + process.env.PATH
            })
        });

        assert.equal(result.error, undefined);
        assert.notEqual(result.status, 0, 'a failed TypeScript compiler must fail the asset build');
        assert.doesNotMatch(result.stdout, /Replaced repository JavaScript files/);
    } finally {
        fs.rmSync(temporaryDirectory, { recursive: true, force: true });
    }
});
