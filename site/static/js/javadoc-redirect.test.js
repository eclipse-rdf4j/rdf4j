import test from 'node:test';
import assert from 'node:assert/strict';
import { findRedirectPath } from './javadoc-redirect.js';

const versions = ['5.1.0', '5.1.2', '5.2.0', '4.3.16'];

test('redirects to the next patch version when available', () => {
  const result = findRedirectPath('/javadoc/5.1.1/org/example/Foo.html', versions);
  assert.equal(result, '/javadoc/5.1.2/org/example/Foo.html');
});

test('redirects to the next minor or major version when patch is missing', () => {
  const result = findRedirectPath('/javadoc/5.1.2/org/example/Foo.html', ['5.1.0', '5.2.0']);
  assert.equal(result, '/javadoc/5.2.0/org/example/Foo.html');
});

test('falls back to the highest available lower version when no newer version exists', () => {
  const result = findRedirectPath('/javadoc/9.0.0/org/example/Foo.html', versions);
  assert.equal(result, '/javadoc/5.2.0/org/example/Foo.html');
});

test('ignores non-semver entries when calculating redirects', () => {
  const result = findRedirectPath('/javadoc/5.1.1/org/example/Foo.html', ['latest', '5.2.0']);
  assert.equal(result, '/javadoc/5.2.0/org/example/Foo.html');
});
