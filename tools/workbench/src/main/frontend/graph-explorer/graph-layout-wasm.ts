/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

export type StressPairInput = {
  x: Float64Array;
  y: Float64Array;
  fixed: Uint8Array;
  left: Float64Array;
  right: Float64Array;
  top: Float64Array;
  bottom: Float64Array;
  /** Cached templates, copied into the instance before they are shuffled. */
  pairNodes: Uint32Array;
  pairHops: Float32Array;
  L: number;
  typicalHalfWidth: number;
  typicalHalfHeight: number;
  linkWeight: number;
};

export type StressPairKernel = {epoch(eta: number, randomState: number): number};

// Keep this bound in sync with wasm/build.sh. Arithmetic is checked as JavaScript numbers before any Wasm32 offset.
const MAX_MEMORY_BYTES = 64 * 1024 * 1024;
let kernel: {module: WebAssembly.Module; heapBase: number} | undefined;
let preparation: Promise<boolean> | undefined;

async function fetchKernelBytes(): Promise<ArrayBuffer> {
  const response = await fetch(new URL('./wasm/stress-pairs.wasm', import.meta.url));
  if (!response.ok) throw new Error(`Could not load the stress kernel (${response.status}).`);
  return response.arrayBuffer();
}

/**
 * Optional, once per worker: unavailable Wasm, failed fetches, invalid modules and CSP failures keep the JS backend.
 * A requested layout never waits for this prewarm. Tests and Node benchmarks can explicitly await initialization
 * with a byte loader, for example `initializeStressWasm(async () => readFile(kernelPath))`.
 */
export function initializeStressWasm(loadBytes: () => Promise<BufferSource> = fetchKernelBytes): Promise<boolean> {
  if (preparation) return preparation;
  // Starting through a microtask also makes a reentrant loader observe the already cached preparation promise.
  preparation = Promise.resolve().then(async () => {
    try {
      if (typeof WebAssembly === 'undefined') return false;
      const compiled = await WebAssembly.compile(await loadBytes());
      // Validate construction and required exports before selecting this optional backend for any calculation.
      const probe = new WebAssembly.Instance(compiled);
      if (!(probe.exports.memory instanceof WebAssembly.Memory) ||
          !(probe.exports.__heap_base instanceof WebAssembly.Global) || typeof probe.exports.epoch !== 'function') return false;
      const heapBase = Number(probe.exports.__heap_base.value);
      if (!Number.isSafeInteger(heapBase) || heapBase < 0 || heapBase > MAX_MEMORY_BYTES ||
          probe.exports.memory.buffer.byteLength > MAX_MEMORY_BYTES) return false;
      kernel = {module: compiled, heapBase};
      return true;
    } catch {
      return false;
    }
  });
  return preparation;
}

type InputView = Float64Array | Uint8Array | Uint32Array | Float32Array;
const overlaps = (a: InputView, b: InputView) => a.buffer === b.buffer &&
  a.byteOffset < b.byteOffset + b.byteLength && b.byteOffset < a.byteOffset + a.byteLength;

/**
 * Independent memory per synchronous solver call. Setup may decline the optional backend before any pair moves;
 * once an epoch starts its errors propagate, because its shuffle, random state and positions may already have moved.
 */
export function createStressPairKernel(input: StressPairInput): StressPairKernel | undefined {
  if (!kernel || typeof WebAssembly === 'undefined') return undefined;
  const {x, y, fixed, left, right, top, bottom, pairNodes, pairHops} = input;
  if (!(x instanceof Float64Array) || !(y instanceof Float64Array) || !(fixed instanceof Uint8Array) ||
      !(left instanceof Float64Array) || !(right instanceof Float64Array) || !(top instanceof Float64Array) ||
      !(bottom instanceof Float64Array) || !(pairNodes instanceof Uint32Array) || !(pairHops instanceof Float32Array)) return undefined;
  const n = x.length, count = pairNodes.length;
  if (!n || n > 0x10000 || !count || y.length !== n || fixed.length !== n || left.length !== n ||
      right.length !== n || top.length !== n || bottom.length !== n || pairHops.length !== count) return undefined;
  // The JS solver supports aliased views. Copying a coordinate separately from an overlapping input would change it.
  const readOnly: InputView[] = [fixed, left, right, top, bottom, pairNodes, pairHops];
  if (overlaps(x, y) || readOnly.some(view => overlaps(x, view) || overlaps(y, view))) return undefined;
  // Shared inputs can be changed by another thread during an epoch; private copies would change that behavior.
  if (typeof SharedArrayBuffer !== 'undefined' && [x, y, ...readOnly].some(view => view.buffer instanceof SharedArrayBuffer)) return undefined;

  try {
    let end = kernel.heapBase;
    const reserve = (length: number, bytes: number) => {
      const offset = Math.ceil(end / 8) * 8;
      end = offset + length * bytes;
      if (!Number.isSafeInteger(end) || end > MAX_MEMORY_BYTES) throw new RangeError('Stress kernel memory limit');
      return offset;
    };
    const xOffset = reserve(n, 8), yOffset = reserve(n, 8), fixedOffset = reserve(n, 1);
    const leftOffset = reserve(n, 8), rightOffset = reserve(n, 8), topOffset = reserve(n, 8), bottomOffset = reserve(n, 8);
    const pairOffset = reserve(count, 4), hopOffset = reserve(count, 4), stateOffset = reserve(1, 4);
    const instance = new WebAssembly.Instance(kernel.module);
    const memory = instance.exports.memory as WebAssembly.Memory;
    const run = instance.exports.epoch as (...args: number[]) => void;
    const extraPages = Math.ceil(end / 65536) - memory.buffer.byteLength / 65536;
    if (extraPages > 0) memory.grow(extraPages);
    const wx = new Float64Array(memory.buffer, xOffset, n), wy = new Float64Array(memory.buffer, yOffset, n);
    new Uint8Array(memory.buffer, fixedOffset, n).set(fixed);
    new Float64Array(memory.buffer, leftOffset, n).set(left);
    new Float64Array(memory.buffer, rightOffset, n).set(right);
    new Float64Array(memory.buffer, topOffset, n).set(top);
    new Float64Array(memory.buffer, bottomOffset, n).set(bottom);
    new Uint32Array(memory.buffer, pairOffset, count).set(pairNodes);
    new Float32Array(memory.buffer, hopOffset, count).set(pairHops);
    const state = new Uint32Array(memory.buffer, stateOffset, 1);
    return {
      epoch(eta, randomState) {
        // The unchanged JS springs, anchors, bounds and hinges may have edited these since the previous epoch.
        wx.set(x); wy.set(y);
        state[0] = randomState;
        run(xOffset, yOffset, fixedOffset, leftOffset, rightOffset, topOffset, bottomOffset, pairOffset, hopOffset,
          count, input.L, input.typicalHalfWidth, input.typicalHalfHeight, input.linkWeight, eta, stateOffset);
        x.set(wx); y.set(wy);
        return state[0];
      },
    };
  } catch {
    // Only allocation/instance setup is optional. The returned epoch intentionally has no catch or replay path.
    return undefined;
  }
}
