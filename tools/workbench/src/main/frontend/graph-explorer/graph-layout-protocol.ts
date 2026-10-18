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
import type { Graph } from "./graph-types.ts";
import type { computeGraphLayout, LayoutOptions } from "./graph-layout-engine.ts";

export type GraphLayoutResult = Awaited<ReturnType<typeof computeGraphLayout>>;
export type GraphLayoutRequest = { requestId: number; graph: Graph; options: LayoutOptions };
export type GraphLayoutReply =
  | { requestId: number; result: GraphLayoutResult }
  | { requestId: number; error: string };
