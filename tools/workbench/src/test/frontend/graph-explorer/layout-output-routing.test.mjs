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
import assert from 'node:assert/strict';
import test from 'node:test';
import {computeGraphLayout} from '../../../main/frontend/graph-explorer/graph-layout-engine.ts';

const card = (id, x, y) => ({id, label: id, kind: 'iri', x, y});
const link = (source, target) => ({id: `${source}-${target}`, source, target, predicate: 'urn:p', label: 'p'});
const fixture = () => ({
  revision: 1,
  nodes: [card('a', 0, 0), card('b', 300, 0), card('c', 600, 0),
    card('d', 0, 220), card('e', 300, 220), card('f', 600, 220)],
  edges: [link('a', 'c'), link('a', 'd'), link('b', 'd'), link('d', 'f'), link('e', 'b'), link('f', 'c')],
});

// Each branch that produces final routes must keep its existing placement and repair behavior when routes are omitted.
const selection = new Set(['d', 'e', 'f']);
const groups = [{id: 'group', nodeIds: ['a', 'b', 'c']}];
for (const [name, options] of [
  ['organic', {}],
  ['incremental organic', {incremental: true, movableNodeIds: selection, newNodeIds: selection}],
  ['new local nodes', {localNodeIds: selection}],
  ['organic selection', {mode: 'network', localNodeIds: selection}],
  ['organic refine', {refine: true}],
  ['overlap repair', {repairOnly: true}],
  ['square', {mode: 'square'}],
  ['hierarchy selection', {mode: 'hierarchy', localNodeIds: selection}],
  ['hierarchy', {mode: 'hierarchy'}],
  ['grouped layout', {mode: 'network', groups}],
  ['nonorganic refine', {mode: 'network', groups, refine: true}],
]) {
  test(`omitting final routes preserves all node geometry: ${name}`, async () => {
    const graph = fixture(), before = structuredClone(graph);
    const {bends, routes, ...withGeometry} = await computeGraphLayout(graph, {...options, seed: 7});
    const {bends: omittedBends, routes: omittedRoutes, ...withoutGeometry} =
      await computeGraphLayout(graph, {...options, seed: 7, includeRoutes: false});
    assert.equal(bends.size, graph.edges.length, 'direct callers still receive arrow geometry by default');
    assert.ok(routes instanceof Map);
    assert.equal(omittedBends.size, 0);
    assert.equal(omittedRoutes.size, 0);
    assert.deepEqual(withoutGeometry, withGeometry, 'positions, repairs and other layout metadata must not change');
    assert.deepEqual(graph, before, 'routing and layout must not mutate the caller graph');
  });
}

test('explicit routing retains detours and pinned coordinates', async () => {
  const graph = fixture();
  graph.nodes = graph.nodes.map(node => ({...node, pinned: true}));
  const implicit = await computeGraphLayout(graph, {repairOnly: true});
  const explicit = await computeGraphLayout(graph, {repairOnly: true, includeRoutes: true});
  const omitted = await computeGraphLayout(graph, {repairOnly: true, includeRoutes: false});
  assert.ok(explicit.routes.has('a-c'), 'the arrow must avoid the pinned card in its path');
  assert.deepEqual(explicit, implicit);
  assert.deepEqual(omitted.positions, new Map(graph.nodes.map(({id, x, y}) => [id, {x, y}])));
});
