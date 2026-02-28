/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.s3.storage;

import java.util.List;

/**
 * Min/max statistics for all four quad components (subject, predicate, object, context).
 */
public final class QuadStats {

	public final long minSubject, maxSubject;
	public final long minPredicate, maxPredicate;
	public final long minObject, maxObject;
	public final long minContext, maxContext;

	public QuadStats(long minSubject, long maxSubject,
			long minPredicate, long maxPredicate,
			long minObject, long maxObject,
			long minContext, long maxContext) {
		this.minSubject = minSubject;
		this.maxSubject = maxSubject;
		this.minPredicate = minPredicate;
		this.maxPredicate = maxPredicate;
		this.minObject = minObject;
		this.maxObject = maxObject;
		this.minContext = minContext;
		this.maxContext = maxContext;
	}

	/**
	 * Computes min/max stats from a list of long[5] arrays (s, p, o, c, flag).
	 */
	public static QuadStats fromQuads(List<long[]> quads) {
		long minS = Long.MAX_VALUE, maxS = Long.MIN_VALUE;
		long minP = Long.MAX_VALUE, maxP = Long.MIN_VALUE;
		long minO = Long.MAX_VALUE, maxO = Long.MIN_VALUE;
		long minC = Long.MAX_VALUE, maxC = Long.MIN_VALUE;
		for (long[] q : quads) {
			minS = Math.min(minS, q[0]);
			maxS = Math.max(maxS, q[0]);
			minP = Math.min(minP, q[1]);
			maxP = Math.max(maxP, q[1]);
			minO = Math.min(minO, q[2]);
			maxO = Math.max(maxO, q[2]);
			minC = Math.min(minC, q[3]);
			maxC = Math.max(maxC, q[3]);
		}
		return new QuadStats(minS, maxS, minP, maxP, minO, maxO, minC, maxC);
	}

	/**
	 * Computes min/max stats from a list of QuadEntry objects.
	 */
	public static QuadStats fromEntries(List<ParquetFileBuilder.QuadEntry> entries) {
		long minS = Long.MAX_VALUE, maxS = Long.MIN_VALUE;
		long minP = Long.MAX_VALUE, maxP = Long.MIN_VALUE;
		long minO = Long.MAX_VALUE, maxO = Long.MIN_VALUE;
		long minC = Long.MAX_VALUE, maxC = Long.MIN_VALUE;
		for (ParquetFileBuilder.QuadEntry e : entries) {
			minS = Math.min(minS, e.subject);
			maxS = Math.max(maxS, e.subject);
			minP = Math.min(minP, e.predicate);
			maxP = Math.max(maxP, e.predicate);
			minO = Math.min(minO, e.object);
			maxO = Math.max(maxO, e.object);
			minC = Math.min(minC, e.context);
			maxC = Math.max(maxC, e.context);
		}
		return new QuadStats(minS, maxS, minP, maxP, minO, maxO, minC, maxC);
	}
}
