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
 * The index name of the underlying data structure, e.g. SPOC, POSC, etc.
 */
@Experimental
public interface IndexReportingIterator {

	String getIndexName();

}
