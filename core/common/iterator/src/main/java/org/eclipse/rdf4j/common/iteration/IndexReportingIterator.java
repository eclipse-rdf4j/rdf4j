/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.common.iteration;

import org.eclipse.rdf4j.common.annotation.Experimental;

/**
 * A way to signal which index is in use for a specific iterator (e.g. SPOC, POSC, etc.). Used in the query explanation.
 */
@Experimental
public interface IndexReportingIterator {

	String getIndexName();

	default long getSourceRowsScannedActual() {
		return -1;
	}

	default long getSourceRowsMatchedActual() {
		return -1;
	}

	default long getSourceRowsFilteredActual() {
		return -1;
	}

}
