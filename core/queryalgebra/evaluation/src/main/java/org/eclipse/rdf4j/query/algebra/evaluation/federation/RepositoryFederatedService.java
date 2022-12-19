/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.federation;

import org.eclipse.rdf4j.repository.Repository;

/**
 * Federated Service wrapping the {@link Repository} to communicate with a SPARQL endpoint.
 *
 * @author Andreas Schwarte
 * @deprecated Use {@link org.eclipse.rdf4j.repository.sparql.federation.RepositoryFederatedService}
 */
@Deprecated(since = "2.3")
public class RepositoryFederatedService
		extends org.eclipse.rdf4j.repository.sparql.federation.RepositoryFederatedService {

	public RepositoryFederatedService(Repository repo, boolean shutDown) {
		super(repo, shutDown);
	}

	public RepositoryFederatedService(Repository repo) {
		super(repo);
	}
}
