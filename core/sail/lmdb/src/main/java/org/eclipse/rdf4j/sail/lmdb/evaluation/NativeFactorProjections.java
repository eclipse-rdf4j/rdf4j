/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import java.io.IOException;

/** A single owned, bounded physical relation with independent demand-projected readers per batch. */
interface NativeFactorProjections extends AutoCloseable {
	boolean nextBatch() throws IOException;
	boolean next(int projection) throws IOException;
	long value(int slot);
	long multiplicity();
	@Override void close();
}
