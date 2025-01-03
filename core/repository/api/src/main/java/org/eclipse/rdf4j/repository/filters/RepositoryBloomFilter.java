/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.repository.filters;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;

/**
 * A bloom filter for statements in a {@link Repository}.
 */
public interface RepositoryBloomFilter {

	/**
	 * Returns true if the repository may have such a statement or false if it definitely does not.
	 *
	 * @param conn connection to the repository to check.
	 * @param subj subject of the statement to check for (can be null).
	 * @param pred predicate of the statement to check for (can be null).
	 * @param obj  object of the statement to check for (can be null).
	 * @param ctxs contexts of the statement to check for.
	 */
	boolean mayHaveStatement(RepositoryConnection conn, Resource subj, IRI pred, Value obj, Resource... ctxs);
}
