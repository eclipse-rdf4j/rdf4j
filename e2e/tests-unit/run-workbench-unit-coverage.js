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
const workbenchScripts = [
    'add.js',
    'create-federate.js',
    'create.js',
    'delete.js',
    'explore.js',
    'export.js',
    'namespaces.js',
    'paging.js',
    'query.js',
    'queryCancelPolicy.js',
    'saved-queries.js',
    'server.js',
    'template.js',
    'tuple.js',
    'update.js',
    'yasqeHelper.js'
].map((fileName) => path.resolve(repoRoot, 'tools/workbench/src/main/webapp/scripts', fileName));
const testFiles = fs.readdirSync(__dirname)
    .filter((fileName) => fileName.endsWith('.test.js'))
    .sort()
    .map((fileName) => path.resolve(__dirname, fileName));

const args = [
    '--test',
    '--experimental-test-coverage',
    '--test-coverage-lines=100',
    '--test-coverage-branches=100',
    '--test-coverage-functions=100'
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
