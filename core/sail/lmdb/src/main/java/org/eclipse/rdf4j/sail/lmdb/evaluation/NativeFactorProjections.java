/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import java.io.IOException;

/** A single owned, bounded physical relation with independent demand-projected readers per batch. */
interface NativeFactorProjections extends AutoCloseable {
	/**
	 * Permit repeated binding prefixes in disjoint additive relation partitions. Call before reading input, only when
	 * all consumers merge partitions without changing semantics. Presence channels must deduplicate identities across
	 * partitions; COUNT channels add exact contributions. This does not authorize replaying effectful expressions,
	 * numeric reassociation or using partition counts as distinct-prefix counts. Producers may ignore it.
	 */
	default void permitPrefixPartitions() {
	}

	boolean nextBatch() throws IOException;

	/** Sole varying output in a bounded projection window, or -1 to decline without advancing. */
	default int windowColumn(int projection) {
		return -1;
	}

	/** Advance a window, at most 256 fragments. Zero completes the current projection. */
	default int nextWindow(int projection) throws IOException {
		throw new UnsupportedOperationException("projection windows");
	}

	/** Read-only arrays, valid until the next cursor operation. Non-window columns are scalar. */
	default long[] windowValues() {
		throw new UnsupportedOperationException("projection windows");
	}

	default long[] windowWeights() {
		throw new UnsupportedOperationException("projection windows");
	}

	default int windowStart() {
		return 0;
	}

	/**
	 * True when invariant bindings OR the outside multiplier may differ from the previous window. The first window of a
	 * projection must return true. Do not cache group handles or scales across a true result. This is independent of
	 * the batch-wide invariance contract, which describes the entire batch.
	 */
	default boolean windowPrefixChanged() {
		return true;
	}

	/** Exact outside multiplier; call only when a consumer actually observes the member's weight. */
	default long windowScale() {
		throw new UnsupportedOperationException("projection windows");
	}

	boolean next(int projection) throws IOException;

	long value(int slot);

	long multiplicity();

	@Override
	void close();
}
