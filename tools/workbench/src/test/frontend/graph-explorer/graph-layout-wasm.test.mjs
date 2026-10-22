/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
import assert from 'node:assert/strict';
import test from 'node:test';
import {readFileSync} from 'node:fs';
import {createHash} from 'node:crypto';
import {stressLayout} from '../../../main/frontend/graph-explorer/graph-layout-stress.ts';
import {createStressPairKernel, initializeStressWasm} from '../../../main/frontend/graph-explorer/graph-layout-wasm.ts';
import {wasmBytes, wasmUrl, random, shuffle, jsEpoch, sample, clone, assertBytes, problem} from './support/wasm-kernel-fixture.mjs';

test('the optional backend preserves complete stress results, cache reuse and all JS phases', async () => {
  const cases = [];
  for (const shape of ['mixed', 'horizontal', 'vertical', 'coincident']) for (const randomStart of [false, true]) {
    const input = problem(37, shape);
    const options = {seed: randomStart ? 0xffffffff : 0, epochs: 6, settleRounds: 4,
      randomStart, startFraction: randomStart ? 0.5 : 0, initialize: shape !== 'coincident'};
    const expected = clone(input);
    // Hold the same link identity across both backends to exercise the distance-cache templates.
    expected.links = input.links;
    const stats = stressLayout(expected, options);
    cases.push({input, options, expected, stats});
  }
  let loads = 0;
  const first = initializeStressWasm(async () => { loads++; return wasmBytes; });
  assert.equal(initializeStressWasm(async () => { throw new Error('must not reload'); }), first);
  assert.equal(await first, true);
  assert.equal(loads, 1);
  for (const {input, options, expected, stats} of cases) {
    for (let repeat = 0; repeat < 2; repeat++) {
      const actual = clone(input); actual.links = input.links;
      assert.deepEqual(stressLayout(actual, options), stats);
      assertBytes(actual.x, expected.x, 'full solver x');
      assertBytes(actual.y, expected.y, 'full solver y');
    }
  }
});

test('the compiled shuffle preserves unsigned endpoints, Float32 hop bits and xorshift state', () => {
  const {exports} = new WebAssembly.Instance(new WebAssembly.Module(wasmBytes));
  const start = Number(exports.__heap_base.value);
  const pairs = Uint32Array.from([0xffff0000, 0xffffffff, 0x80010001, 0x00020001, 0x1234ffff, 0xf000ffff]);
  const hops = Float32Array.from([0.25, 1, 1.125, 2.2, 9999.875, -0]);
  const wp = new Uint32Array(exports.memory.buffer, start, pairs.length);
  const wh = new Float32Array(exports.memory.buffer, start + pairs.byteLength, hops.length);
  const stateOffset = start + pairs.byteLength + hops.byteLength;
  const state = new Uint32Array(exports.memory.buffer, stateOffset, 1);
  wp.set(pairs); wh.set(hops);
  const next = random(0x80000001); state[0] = next.getState();
  for (let epoch = 0; epoch < 12; epoch++) {
    shuffle(pairs, hops, next);
    exports.shuffle(start, start + pairs.byteLength, pairs.length, stateOffset);
    assertBytes(wp, pairs, 'packed endpoints'); assertBytes(wh, hops, 'Float32 hop bits');
    assert.equal(state[0], next.getState());
  }
});

test('private pair templates stay immutable and edits between epochs are copied in before the next epoch', () => {
  for (const shape of ['mixed', 'horizontal', 'vertical', 'coincident']) {
    const source = sample(32, 731, shape), expected = clone(source), actual = clone(source);
    const pairs = actual.pairNodes.slice(), hops = actual.pairHops.slice();
    const kernel = createStressPairKernel(actual); assert.ok(kernel);
    const next = random(0x8f13411e); let state = next.getState();
    for (const eta of [1000000, 10000, 100, 1]) {
      jsEpoch(expected, eta, next);
      state = kernel.epoch(eta, state);
      assert.equal(state, next.getState());
      assertBytes(actual.x, expected.x, 'epoch x'); assertBytes(actual.y, expected.y, 'epoch y');
      assertBytes(actual.pairNodes, pairs, 'cached pair template'); assertBytes(actual.pairHops, hops, 'cached hop template');
      // Models a JS hinge/anchor update and a coincident pair before the following epoch.
      actual.x[1] = expected.x[1] = expected.x[2];
      actual.y[1] = expected.y[1] = expected.y[2];
      actual.x[4] += 15.25; expected.x[4] += 15.25;
    }
  }
});

test('unsuitable shapes, overlapping inputs, shared buffers and oversized memory requests use JS', () => {
  const input = sample(8);
  assert.equal(createStressPairKernel({...input, y: new Float64Array(7)}), undefined);
  assert.equal(createStressPairKernel({...input, fixed: new Uint8Array(7)}), undefined);
  assert.equal(createStressPairKernel({...input, x: new Float32Array(8)}), undefined);
  assert.equal(createStressPairKernel({...input, y: input.x}), undefined);
  assert.equal(createStressPairKernel({...input, left: input.x}), undefined);
  const common = new ArrayBuffer(16 * 8);
  assert.equal(createStressPairKernel({...input, x: new Float64Array(common, 0, 8), left: new Float64Array(common, 8, 8)}), undefined);
  assert.ok(createStressPairKernel({...input, x: new Float64Array(common, 0, 8), y: new Float64Array(common, 8 * 8, 8)}));
  assert.ok(createStressPairKernel({...input, right: input.left}), 'read-only extents may share storage');
  if (typeof SharedArrayBuffer !== 'undefined') {
    assert.equal(createStressPairKernel({...input, fixed: new Uint8Array(new SharedArrayBuffer(8))}), undefined);
  }
  const largeReadOnlyBuffer = new ArrayBuffer(32 * 1024 * 1024);
  const Instance = WebAssembly.Instance; let constructions = 0;
  try {
    WebAssembly.Instance = function () { constructions++; throw new Error('must preflight before constructing'); };
    assert.equal(createStressPairKernel({...input, pairNodes: new Uint32Array(largeReadOnlyBuffer), pairHops: new Float32Array(largeReadOnlyBuffer)}), undefined);
    assert.equal(constructions, 0);
    assert.equal(createStressPairKernel(input), undefined, 'instance setup failure uses JS');
    assert.equal(constructions, 1);
  } finally { WebAssembly.Instance = Instance; }
});

test('the committed module matches its reproducible build metadata', () => {
  const metadata = JSON.parse(readFileSync(new URL('build-metadata.json', wasmUrl), 'utf8'));
  assert.equal(wasmBytes.byteLength, metadata.bytes);
  assert.equal(createHash('sha256').update(wasmBytes).digest('hex'), metadata.sha256);
  const module = new WebAssembly.Module(wasmBytes);
  assert.deepEqual(WebAssembly.Module.imports(module), []);
  assert.deepEqual(WebAssembly.Module.exports(module).map(value => value.name).sort(), metadata.exports.toSorted());
});
