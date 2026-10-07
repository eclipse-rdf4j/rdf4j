const fs = require('node:fs');
const path = require('node:path');
const { spawnSync } = require('node:child_process');

const major = Number.parseInt(process.versions.node.split('.')[0], 10);
if (major < 22) {
    console.error(`Workbench unit coverage requires Node 22+. Found ${process.versions.node}.`);
    process.exit(1);
}

const e2eDir = path.resolve(__dirname, '..');
const repoRoot = path.resolve(e2eDir, '..');
const scriptsDir = path.resolve(repoRoot, 'tools/workbench/src/main/webapp/scripts');
// Every script compiled from scripts/ts (declaration files produce none), so the list cannot name a deleted script or
// miss a new one. Only scripts a test loads under their absolute path are attributed; tests that evaluate a script
// under a relative vm filename are not counted, so these totals understate what the tests exercise.
const workbenchScripts = fs.readdirSync(path.join(scriptsDir, 'ts'))
    .filter((fileName) => fileName.endsWith('.ts') && !fileName.endsWith('.d.ts'))
    .sort()
    .map((fileName) => path.join(scriptsDir, fileName.replace(/\.ts$/, '.js')));
// Floors for the totals over those scripts, a little below what the suite reaches (about 59 % lines, 77 % branches,
// 64 % functions in October 2026), so a drop fails the run. Override them with WORKBENCH_COVERAGE_LINES,
// WORKBENCH_COVERAGE_BRANCHES and WORKBENCH_COVERAGE_FUNCTIONS.
const thresholds = {
    lines: process.env.WORKBENCH_COVERAGE_LINES || '55',
    branches: process.env.WORKBENCH_COVERAGE_BRANCHES || '70',
    functions: process.env.WORKBENCH_COVERAGE_FUNCTIONS || '60'
};
const testFiles = fs.readdirSync(__dirname)
    .filter((fileName) => fileName.endsWith('.test.js'))
    .sort()
    .map((fileName) => path.resolve(__dirname, fileName));

const args = [
    '--test',
    '--experimental-test-coverage',
    `--test-coverage-lines=${thresholds.lines}`,
    `--test-coverage-branches=${thresholds.branches}`,
    `--test-coverage-functions=${thresholds.functions}`
];
workbenchScripts.forEach((scriptPath) => {
    args.push(`--test-coverage-include=${scriptPath}`);
});
args.push(...testFiles);

const result = spawnSync(process.execPath, args, {
    cwd: e2eDir,
    stdio: 'inherit'
});

process.exit(result.status === null ? 1 : result.status);
