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
import org.eclipse.rdf4j.repository.RepositoryConnection;

/**
 * A bloom filter that is exact (no false positives) at the cost of always having to query the repository.
 */
public class AccurateRepositoryBloomFilter implements RepositoryBloomFilter {

	public static final AccurateRepositoryBloomFilter INCLUDE_INFERRED_INSTANCE = new AccurateRepositoryBloomFilter(
			true);

	private final boolean includeInferred;

	public AccurateRepositoryBloomFilter(boolean includeInferred) {
		this.includeInferred = includeInferred;
	}

	@Override
	public boolean mayHaveStatement(RepositoryConnection conn, Resource subj, IRI pred, Value obj,
			Resource... contexts) {
		return conn.hasStatement(subj, pred, obj, includeInferred, contexts);
	}

}
