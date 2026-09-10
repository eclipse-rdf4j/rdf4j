/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import java.io.IOException;

import org.eclipse.rdf4j.sail.lmdb.factor.FactorEnvironment;

/**
 * Scalar-prefix rows plus retained child relations. Not a RowCursor: treating a deferred variable as either UNKNOWN or
 * a scalar ID at an unaware consumer would silently change query semantics. A successful next installs only the scalar
 * prefix in RowState. The sidecar is valid until the next call or close. Multiplicity is stable (not consume-on-read)
 * and excludes the child factors.
 */
interface LmdbNativeFactorCursor extends AutoCloseable {
	boolean next() throws IOException;

	/** Conservative whole-cursor capability: false guarantees that every emitted relation is scalar. */
	default boolean mayHaveFactors() {
		return true;
	}

	long multiplicity();

	FactorEnvironment factors();

	@Override
	void close();
}
