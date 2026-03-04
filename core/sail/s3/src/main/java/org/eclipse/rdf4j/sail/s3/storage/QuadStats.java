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
	 * Computes min/max stats from a list of QuadEntry objects. Tombstones are excluded so that deleted entries do not
	 * inflate the range statistics used for pruning.
	 */
	public static QuadStats fromEntries(List<QuadEntry> entries) {
		Accumulator acc = new Accumulator();
		for (QuadEntry e : entries) {
			if (e.flag != MemTable.FLAG_TOMBSTONE) {
				acc.add(e.subject, e.predicate, e.object, e.context);
			}
		}
		return acc.build();
	}

	private static class Accumulator {
		long minS = Long.MAX_VALUE, maxS = Long.MIN_VALUE;
		long minP = Long.MAX_VALUE, maxP = Long.MIN_VALUE;
		long minO = Long.MAX_VALUE, maxO = Long.MIN_VALUE;
		long minC = Long.MAX_VALUE, maxC = Long.MIN_VALUE;

		void add(long s, long p, long o, long c) {
			minS = Math.min(minS, s);
			maxS = Math.max(maxS, s);
			minP = Math.min(minP, p);
			maxP = Math.max(maxP, p);
			minO = Math.min(minO, o);
			maxO = Math.max(maxO, o);
			minC = Math.min(minC, c);
			maxC = Math.max(maxC, c);
		}

		QuadStats build() {
			return new QuadStats(minS, maxS, minP, maxP, minO, maxO, minC, maxC);
		}
	}
}
