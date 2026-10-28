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
// Frozen pre-optimization local search: an independent scan oracle for conservative broad-phase changes.
// Keep its scan/order arithmetic unchanged; it intentionally has no spatial pruning.
import {segmentHitsBox, type StressProblem} from '../../../../main/frontend/graph-explorer/graph-layout-stress.ts';

function radius(left: number, right: number, top: number, bottom: number, ux: number, uy: number) {
  const ex = ux >= 0 ? right : left, ey = uy >= 0 ? bottom : top;
  const ax = Math.abs(ux), ay = Math.abs(uy);
  if (ax < 1e-9) return ey;
  if (ay < 1e-9) return ex;
  return Math.min(ex / ax, ey / ay);
}

export function referenceUntangle(problem: StressProblem, passes = 3, {crossings = 1, moveCost = 1 / 12} = {}) {
  const {left, right, top, bottom, x, y, links, bodies} = problem, n = x.length;
  const fixed = problem.fixed ?? new Uint8Array(n);
  const count = links.length / 2;
  const incident: number[][] = Array.from({length: n}, () => []);
  for (let p = 0; p < count; p++) { incident[links[2 * p]].push(p); incident[links[2 * p + 1]].push(p); }
  // Every box that moves with a card: its own, and those of its fan (bodies); card k's boxes are boxStart[k] up to
  // boxStart[k + 1], as offsets from its position.
  const boxStart = new Int32Array(n + 1);
  for (let i = 0; i < n; i++) boxStart[i + 1] = boxStart[i] + 1 + (bodies?.[i]?.length ?? 0);
  const total = boxStart[n];
  const boxMinX = new Float64Array(total), boxMaxX = new Float64Array(total), boxMinY = new Float64Array(total), boxMaxY = new Float64Array(total);
  for (let i = 0; i < n; i++) {
    let e = boxStart[i];
    boxMinX[e] = -left[i]; boxMaxX[e] = right[i]; boxMinY[e] = -top[i]; boxMaxY[e] = bottom[i];
    for (const box of bodies?.[i] ?? []) { e++; boxMinX[e] = box.minX; boxMaxX[e] = box.maxX; boxMinY[e] = box.minY; boxMaxY[e] = box.maxY; }
  }
  // Centres and sizes of the boxes for the overlap test.
  const boxMidX = Float64Array.from(boxMinX, (minX, e) => (minX + boxMaxX[e]) / 2), boxMidY = Float64Array.from(boxMinY, (minY, e) => (minY + boxMaxY[e]) / 2);
  const boxWidth = Float64Array.from(boxMinX, (minX, e) => boxMaxX[e] - minX), boxHeight = Float64Array.from(boxMinY, (minY, e) => boxMaxY[e] - minY);
  /** Boxes of card k that segment a–b passes within `pad` of (boxes more than a pixel beside its box are skipped). */
  const cuts = (ax: number, ay: number, bx: number, by: number, k: number, pad: number) =>
    cutsWithin(ax, ay, bx, by, ax < bx ? ax : bx, ax < bx ? bx : ax, ay < by ? ay : by, ay < by ? by : ay, k, pad);
  /** `cuts` given the segment's box (loX..hiX, loY..hiY). */
  const cutsWithin = (ax: number, ay: number, bx: number, by: number, loX: number, hiX: number, loY: number, hiY: number, k: number, pad: number) => {
    let hits = 0;
    for (let e = boxStart[k]; e < boxStart[k + 1]; e++) {
      const minX = x[k] + boxMinX[e] - pad;
      if (hiX < minX - 1) continue;
      const maxX = x[k] + boxMaxX[e] + pad;
      if (loX > maxX + 1) continue;
      const minY = y[k] + boxMinY[e] - pad;
      if (hiY < minY - 1) continue;
      const maxY = y[k] + boxMaxY[e] + pad;
      if (loY > maxY + 1) continue;
      if (segmentHitsBox(ax, ay, bx, by, minX, minY, maxX, maxY)) hits++;
    }
    return hits;
  };
  // A cost only grows while its conflicts are counted. Given `done`, counting stops (and `stopped` is set) as soon as
  // `done` holds for the cost counted so far: callers that only compare the cost with a bound learn the same answer.
  let stopped = false;
  // `segmentsCross` for link p against every other link, written out: the side of each card of link p's line is the
  // same for every link the card ends, so it is computed once per card; most links then have both ends on one side.
  // The line's own cards have side exactly 0 (b's is ex·ey − ey·ex), so the side test also rejects p itself and the
  // links sharing a card with it.
  const side = new Float64Array(n);
  const linkCost = (p: number, base = 0, done?: (cost: number) => boolean) => {
    const a = links[2 * p], b = links[2 * p + 1];
    const ax = x[a], ay = y[a], bx = x[b], by = y[b];
    let cost = 0;
    if (crossings) {
      const ex = bx - ax, ey = by - ay;
      for (let k = 0; k < n; k++) side[k] = ex * (y[k] - ay) - ey * (x[k] - ax);
      for (let q = 0; q < count; q++) {
        const c = links[2 * q], d = links[2 * q + 1];
        if (!(side[c] * side[d] < -1e-9)) continue;
        const cx = x[c], cy = y[c], fx = x[d] - cx, fy = y[d] - cy;
        if (!((fx * (ay - cy) - fy * (ax - cx)) * (fx * (by - cy) - fy * (bx - cx)) < -1e-9)) continue;
        cost += crossings;
        if (done?.(base + cost)) { stopped = true; return cost; }
      }
    }
    const loX = ax < bx ? ax : bx, hiX = ax < bx ? bx : ax, loY = ay < by ? ay : by, hiY = ay < by ? by : ay;
    for (let k = 0; k < n; k++) {
      if (k === a || k === b) continue;
      const hits = cutsWithin(ax, ay, bx, by, loX, hiX, loY, hiY, k, 4);
      if (!hits) continue;
      cost += 3 * hits;
      if (done?.(base + cost)) { stopped = true; return cost; }
    }
    return cost;
  };
  const cardCost = (i: number, done?: (cost: number) => boolean) => {
    stopped = false;
    let cost = 0;
    // A bound already met with no conflicts counted (a candidate's move costs as much as the best) needs no count.
    if (done?.(cost)) { stopped = true; return cost; }
    for (const p of incident[i]) {
      cost += linkCost(p, cost, done);
      if (stopped) return cost;
    }
    // Links of other cards cutting this card or its fan.
    for (let p = 0; p < count; p++) {
      const a = links[2 * p], b = links[2 * p + 1];
      if (a === i || b === i) continue;
      const hits = cuts(x[a], y[a], x[b], y[b], i, 4);
      if (!hits) continue;
      cost += 3 * hits;
      if (done?.(cost)) { stopped = true; return cost; }
    }
    const xi = x[i], yi = y[i];
    for (let k = 0; k < n; k++) {
      if (k === i) continue;
      const xk = x[k], yk = y[k];
      for (let e = boxStart[i]; e < boxStart[i + 1]; e++) for (let f = boxStart[k]; f < boxStart[k + 1]; f++) {
        const dx = xi + boxMidX[e] - xk - boxMidX[f], dy = yi + boxMidY[e] - yk - boxMidY[f];
        if (!(Math.abs(dx) < (boxWidth[e] + boxMaxX[f] - boxMinX[f]) / 2 + 24 && Math.abs(dy) < (boxHeight[e] + boxMaxY[f] - boxMinY[f]) / 2 + 18)) continue;
        cost += 20;
        if (done?.(cost)) { stopped = true; return cost; }
      }
    }
    return cost;
  };
  // Steps and the cost of moving scale with a typical card, not with the biggest fan.
  const sizes = Array.from({length: n}, (_, i) => (left[i] + right[i] + top[i] + bottom[i]) / 4).sort((a, b) => a - b);
  const scale = Math.max(40, sizes[n >> 1]);
  for (let pass = 0; pass < passes; pass++) {
    let improved = 0;
    for (let i = 0; i < n; i++) {
      if (fixed[i] || !incident[i].length) continue;
      const start = cardCost(i);
      if (!start) continue;
      const x0 = x[i], y0 = y[i];
      const candidates: Array<[number, number]> = [];
      for (const r of [0.6, 1.2, 2, 3.2]) for (let k = 0; k < 8; k++) candidates.push([x0 + Math.cos(k * Math.PI / 4) * r * scale, y0 + Math.sin(k * Math.PI / 4) * r * scale]);
      // A card with one link may swing all the way round the card it hangs from, where it is and at a plain link's
      // distance.
      if (incident[i].length === 1) {
        const o = links[2 * incident[i][0]] === i ? links[2 * incident[i][0] + 1] : links[2 * incident[i][0]];
        const r = Math.hypot(x0 - x[o], y0 - y[o]);
        for (let k = 0; k < 16; k++) {
          const ux = Math.cos(k * Math.PI / 8), uy = Math.sin(k * Math.PI / 8);
          const near = radius(left[o], right[o], top[o], bottom[o], ux, uy) + radius(left[i], right[i], top[i], bottom[i], -ux, -uy) + 90;
          candidates.push([x[o] + ux * r, y[o] + uy * r]);
          if (near < r - 1) candidates.push([x[o] + ux * near, y[o] + uy * near]);
        }
      }
      // Mirror across the neighbours' middle: flips a card to the other side of a crossing.
      let mx = 0, my = 0;
      for (const p of incident[i]) { const o = links[2 * p] === i ? links[2 * p + 1] : links[2 * p]; mx += x[o]; my += y[o]; }
      mx /= incident[i].length; my /= incident[i].length;
      candidates.push([2 * mx - x0, 2 * my - y0]);
      let bestCost = start, bestX = x0, bestY = y0;
      for (const [cx, cy] of candidates) {
        x[i] = cx; y[i] = cy;
        const move = Math.hypot(cx - x0, cy - y0) * moveCost / scale, limit = bestCost - 0.05;
        // A position whose conflicts counted so far already reach the best cost cannot win.
        const conflicts = cardCost(i, cost => cost + move >= limit);
        if (stopped) continue;
        const cost = conflicts + move;
        if (cost < limit) { bestCost = cost; bestX = cx; bestY = cy; }
      }
      x[i] = bestX; y[i] = bestY;
      if (bestX !== x0 || bestY !== y0) { improved++; continue; }
      // Trading places with a nearby card undoes crossings no single move can (two cards on the wrong sides). The card
      // is back where its cost was `start`.
      const here = start;
      let bestSwap = -1, bestGain = 0.5;
      for (let j = 0; j < n; j++) {
        if (j === i || fixed[j] || Math.hypot(x[j] - x0, y[j] - y0) > 4 * scale) continue;
        const before = here + cardCost(j);
        x[i] = x[j]; y[i] = y[j]; x[j] = x0; y[j] = y0;
        // A trade whose new conflicts counted so far already use up the gain to beat is not taken.
        let gain = -Infinity;
        const after = cardCost(i, cost => before - cost <= bestGain);
        if (!stopped) {
          const other = cardCost(j, cost => before - (after + cost) <= bestGain);
          if (!stopped) gain = before - (after + other);
        }
        x[j] = x[i]; y[j] = y[i]; x[i] = x0; y[i] = y0;
        if (gain > bestGain) { bestGain = gain; bestSwap = j; }
      }
      if (bestSwap >= 0) {
        x[i] = x[bestSwap]; y[i] = y[bestSwap]; x[bestSwap] = x0; y[bestSwap] = y0;
        improved++;
      }
    }
    if (!improved) break;
  }
}
