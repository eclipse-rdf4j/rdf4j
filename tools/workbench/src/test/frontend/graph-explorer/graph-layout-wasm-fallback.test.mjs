/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
import assert from 'node:assert/strict';
import test from 'node:test';
import {stressLayout} from '../../../main/frontend/graph-explorer/graph-layout-stress.ts';
import {initializeStressWasm, createStressPairKernel} from '../../../main/frontend/graph-explorer/graph-layout-wasm.ts';
import {loaderUrl, wasmBytes, problem, clone, sample, assertBytes} from './support/wasm-kernel-fixture.mjs';

test('a failed optional fetch is cached once and preserves the exact JavaScript solver result', async () => {
  const source = problem(19), expected = clone(source), actual = clone(source);
  const options = {initialize: false, epochs: 5, settleRounds: 3, seed: 31};
  const stats = stressLayout(expected, options);
  let attempts = 0;
  const failed = initializeStressWasm(async () => { attempts++; throw new Error('network unavailable'); });
  assert.equal(await failed, false);
  assert.equal(initializeStressWasm(async () => wasmBytes), failed);
  assert.equal(await initializeStressWasm(async () => wasmBytes), false);
  assert.equal(attempts, 1);
  assert.equal(createStressPairKernel(sample(8)), undefined);
  assert.deepEqual(stressLayout(actual, options), stats);
  assertBytes(actual.x, expected.x, 'fallback x'); assertBytes(actual.y, expected.y, 'fallback y');
});

test('unavailable WebAssembly, invalid modules and CSP compile failures decline the backend', async () => {
  const original = globalThis.WebAssembly;
  try {
    globalThis.WebAssembly = undefined;
    const unavailable = await import(`${loaderUrl.href}?case=unavailable`);
    let calls = 0;
    assert.equal(await unavailable.initializeStressWasm(async () => { calls++; return wasmBytes; }), false);
    assert.equal(calls, 0);
    assert.equal(unavailable.createStressPairKernel(sample(8)), undefined);
  } finally { globalThis.WebAssembly = original; }
  const invalid = await import(`${loaderUrl.href}?case=invalid`);
  assert.equal(await invalid.initializeStressWasm(async () => new Uint8Array([0, 1, 2])), false);
  const missing = await import(`${loaderUrl.href}?case=missing-exports`);
  assert.equal(await missing.initializeStressWasm(async () => new Uint8Array([0, 97, 115, 109, 1, 0, 0, 0])), false);
  const csp = await import(`${loaderUrl.href}?case=csp`), compile = WebAssembly.compile;
  try {
    WebAssembly.compile = async () => { throw new WebAssembly.CompileError('blocked by CSP'); };
    assert.equal(await csp.initializeStressWasm(async () => wasmBytes), false);
  } finally { WebAssembly.compile = compile; }
});

test('a pending optional load leaves synchronous pair selection immediately available as JS', async () => {
  const pending = await import(`${loaderUrl.href}?case=pending`);
  let loads = 0;
  const ready = pending.initializeStressWasm(() => { loads++; return new Promise(() => {}); });
  assert.equal(pending.initializeStressWasm(async () => wasmBytes), ready);
  await Promise.resolve();
  assert.equal(loads, 1);
  assert.equal(pending.createStressPairKernel(sample(8)), undefined);
});
