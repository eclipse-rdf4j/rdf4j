/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class LmdbNativeMorselExecutorTest {
	@Test
	@Timeout(90)
	void rangeExchangeFailureAndCancellationStress() throws Exception {
		LmdbNativeMorselChecks.main(new String[] { "50" });
	}
}
