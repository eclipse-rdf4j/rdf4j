/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
import assert from 'node:assert/strict';
import test from 'node:test';
import {initializeStressWasm} from '../../../main/frontend/graph-explorer/graph-layout-wasm.ts';
import {stressLayout} from '../../../main/frontend/graph-explorer/graph-layout-stress.ts';
import {problem, assertBytes} from './support/wasm-kernel-fixture.mjs';

test('a trap after a Wasm write propagates without replaying the partially executed epoch in JavaScript', async () => {
  const string = value => [value.length, ...Buffer.from(value)];
  const section = (id, bytes) => [id, bytes.length, ...bytes];
  // A small test module exports the production interface. epoch writes f64 123 at its first pointer, then traps.
  const value = Buffer.alloc(8); value.writeDoubleLE(123);
  const body = [0, 0x20, 0, 0x44, ...value, 0x39, 3, 0, 0, 0x0b];
  const bytes = new Uint8Array([0, 97, 115, 109, 1, 0, 0, 0,
    ...section(1, [1, 0x60, 1, 0x7f, 0]), ...section(3, [1, 0]),
    ...section(5, [1, 1, 2, 0x80, 8]),
    ...section(6, [1, 0x7f, 0, 0x41, 0xc0, 0, 0x0b]),
    ...section(7, [3, ...string('memory'), 2, 0, ...string('__heap_base'), 3, 0, ...string('epoch'), 0, 0]),
    ...section(10, [1, body.length, ...body])]);
  assert.equal(await initializeStressWasm(async () => bytes), true);
  const input = problem(813), x = input.x.slice(), y = input.y.slice();
  assert.throws(() => stressLayout(input, {initialize: false, epochs: 2, overlap: false, occlusion: false, settleRounds: 0}), WebAssembly.RuntimeError);
  assertBytes(input.x, x, 'failed Wasm work must not be silently replaced with a JS epoch');
  assertBytes(input.y, y, 'failed Wasm work must not be silently replaced with a JS epoch');
});
