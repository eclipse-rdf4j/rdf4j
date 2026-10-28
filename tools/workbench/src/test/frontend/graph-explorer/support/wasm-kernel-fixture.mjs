/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import ts from 'typescript';

export const wasmUrl = new URL('../../../../main/frontend/graph-explorer/wasm/stress-pairs.wasm', import.meta.url);
export const wasmBytes = readFileSync(wasmUrl);
export const loaderUrl = new URL('../../../../main/frontend/graph-explorer/graph-layout-wasm.ts', import.meta.url);
// The retained JS kernel is the oracle for the independently compiled C port.
const source = readFileSync(new URL('../../../../main/frontend/graph-explorer/graph-layout-stress.ts', import.meta.url), 'utf8');
const start = source.indexOf('function pairEpoch('), end = source.indexOf('\nfunction clamp(', start);
assert.ok(start >= 0 && end > start);
const js = ts.transpileModule(source.slice(start, end), {compilerOptions: {target: ts.ScriptTarget.ES2022}}).outputText;
const pairEpoch = Function(`${js}\nreturn pairEpoch;`)();

export function random(seed) {
  let state = (seed >>> 0) || 0x9e3779b9;
  const next = () => {
    state ^= state << 13; state >>>= 0;
    state ^= state >>> 17;
    state ^= state << 5; state >>>= 0;
    return state / 4294967296;
  };
  next.getState = () => state;
  return next;
}
export function shuffle(pairs, hops, next) {
  for (let p = pairs.length - 1; p > 0; p--) {
    const q = Math.floor(next() * (p + 1));
    const t = pairs[p]; pairs[p] = pairs[q]; pairs[q] = t;
    const h = hops[p]; hops[p] = hops[q]; hops[q] = h;
  }
}
export function jsEpoch(input, eta, next) {
  const {x, y, fixed, left, right, top, bottom, pairNodes, pairHops, L,
    typicalHalfWidth, typicalHalfHeight, linkWeight} = input;
  shuffle(pairNodes, pairHops, next);
  pairEpoch(x, y, fixed, left, right, top, bottom, pairNodes, pairHops,
    L, typicalHalfWidth, typicalHalfHeight, linkWeight, eta, next);
}
export function sample(n, seed = 1, shape = 'mixed') {
  const next = random(seed);
  const input = {x: new Float64Array(n), y: new Float64Array(n), fixed: new Uint8Array(n),
    left: new Float64Array(n), right: new Float64Array(n), top: new Float64Array(n), bottom: new Float64Array(n),
    pairNodes: new Uint32Array(n * (n - 1) / 2), pairHops: new Float32Array(n * (n - 1) / 2),
    L: 90, typicalHalfWidth: 100, typicalHalfHeight: 65, linkWeight: 1.4};
  for (let i = 0; i < n; i++) {
    input.x[i] = shape === 'vertical' || shape === 'coincident' ? 0 : (next() - 0.5) * 1500;
    input.y[i] = shape === 'horizontal' || shape === 'coincident' ? -0 : (next() - 0.5) * 1300;
    input.fixed[i] = i % 5 === 0 || i % 9 === 0 ? 1 : 0;
    for (const key of ['left', 'right', 'top', 'bottom']) input[key][i] = 20 + next() * 160;
  }
  for (let i = 0, k = 0; i < n; i++) for (let j = i + 1; j < n; j++, k++) {
    input.pairNodes[k] = i | j << 16;
    input.pairHops[k] = k % 7 === 0 ? 0.25 : k % 5 === 0 ? 1 : 1 + next() * 12;
  }
  return input;
}
export const clone = input => Object.fromEntries(Object.entries(input).map(([key, value]) =>
  [key, ArrayBuffer.isView(value) ? value.slice() : value]));
export function assertBytes(a, b, label) {
  assert.equal(Buffer.compare(Buffer.from(a.buffer, a.byteOffset, a.byteLength),
    Buffer.from(b.buffer, b.byteOffset, b.byteLength)), 0, label);
}
export function problem(seed = 1, shape = 'mixed') {
  const input = sample(24, seed, shape), links = [];
  for (let i = 0; i < 23; i++) { links.push(i, i + 1); if (i + 3 < 24) links.push(i, i + 3); }
  return {...input, links: new Int32Array(links),
    linkLengths: Float64Array.from({length: links.length / 2}, (_, i) => i % 5 === 0 ? 1.25 : 1),
    anchorX: input.x.slice(), anchorY: input.y.slice(), anchorWeight: new Float64Array(24).fill(0.03),
    minX: new Float64Array(24).fill(-850), minY: new Float64Array(24).fill(-650),
    springs: [{a: 1, b: 12, length: 200, weight: 0.7}],
    bodies: Array.from({length: 24}, (_, i) => i % 8 ? undefined : [{minX: 90, maxX: 170, minY: -20, maxY: 55}]),
  };
}
