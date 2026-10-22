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
import {LARGE_GRAPH_NODES} from '../../../main/frontend/graph-explorer/explorer-config.ts';
import {canvasGraph} from '../../../main/frontend/graph-explorer/graph-canvas-scope.ts';
import {routeDrawnGraph} from '../../../main/frontend/graph-explorer/graph-edge-routing.ts';
import {leafClusterForNode, removeGraphNodes} from '../../../main/frontend/graph-explorer/graph-operations.ts';

const card = (id, x, y, kind = 'iri') => ({id, label: id, kind, x, y});
const link = (source, target) => ({id: `${source}-${target}`, source, target, predicate: 'urn:p', label: 'p'});
function largeGraph(kind = 'iri', leaves = false) {
  const extra = Array.from({length: LARGE_GRAPH_NODES - 3}, (_, i) => card(`extra${i}`, 10000 + i * 240, 10000, kind));
  return {
    revision: 1,
    nodes: [card('a', 0, 0), card('b', 300, 0), card('c', 600, 0), ...extra],
    // The triangle keeps a, b and c when a's exclusive leaves are collapsed. Card b obstructs a-c.
    edges: [link('a', 'c'), link('c', 'b'), link('b', 'a'), ...(leaves ? extra.map(node => link('a', node.id)) : [])],
  };
}

function assertRoutesAtExistingPositions(graph, showValues) {
  const before = structuredClone(graph);
  const routed = routeDrawnGraph(graph, showValues);
  assert.ok(routed.routes.has('a-c'), 'the smaller view must recover its obstacle-avoiding arrow');
  assert.deepEqual(graph, before, 'refreshing arrows must not arrange or repair any node');
  assert.deepEqual(routed.routes.get('a-c').source, {x: 0, y: 0});
  assert.deepEqual(routed.routes.get('a-c').target, {x: 600, y: 0});
  return routed;
}

test('hiding a node at the canvas boundary restores SVG routes without a new layout', () => {
  const graph = largeGraph(), before = structuredClone(graph);
  assert.equal(canvasGraph(graph, false).nodes.length, LARGE_GRAPH_NODES);
  assert.deepEqual(routeDrawnGraph(graph, false), {bends: new Map(), routes: new Map()});
  const remaining = removeGraphNodes(graph, new Set(['extra0']));
  assert.equal(remaining.nodes.length, LARGE_GRAPH_NODES - 1);
  assertRoutesAtExistingPositions(remaining, false);
  assert.deepEqual(remaining.nodes, graph.nodes.filter(node => node.id !== 'extra0'));
  assert.deepEqual(graph, before);
});

test('collapsing exclusive leaves restores routes at the retained cards', () => {
  const graph = largeGraph('iri', true), before = structuredClone(graph);
  const cluster = leafClusterForNode(canvasGraph(graph, false), 'a');
  cluster.delete('a');
  assert.equal(cluster.size, LARGE_GRAPH_NODES - 3);
  const remaining = removeGraphNodes(graph, cluster);
  assert.equal(remaining.nodes.length, 3);
  assertRoutesAtExistingPositions(remaining, false);
  assert.deepEqual(remaining.nodes, graph.nodes.slice(0, 3));
  assert.deepEqual(graph, before);
});

test('Values off routes the next visible projection while keeping hidden values in the model', () => {
  const graph = largeGraph('literal', true), before = structuredClone(graph);
  assert.equal(canvasGraph(graph, true).nodes.length, LARGE_GRAPH_NODES);
  assert.equal(routeDrawnGraph(graph, true).bends.size, 0);
  const drawn = canvasGraph(graph, false);
  assert.equal(drawn.nodes.length, 3);
  const routed = assertRoutesAtExistingPositions(graph, false);
  assert.deepEqual([...routed.bends.keys()].sort(), drawn.edges.map(edge => edge.id).sort());
  assert.deepEqual(routed, routeDrawnGraph(drawn, false), 'hidden values must not become route obstacles');
  assert.deepEqual(graph, before);
});
