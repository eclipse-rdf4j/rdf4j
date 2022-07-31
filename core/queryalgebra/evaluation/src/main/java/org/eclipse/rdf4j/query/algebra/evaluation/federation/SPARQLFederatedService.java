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

import org.eclipse.rdf4j.http.client.HttpClientSessionManager;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;

/**
 * Federated Service wrapping the {@link SPARQLRepository} to communicate with a SPARQL endpoint.
 *
 * @author Andreas Schwarte
 * @deprecated since 2.3 use {@link org.eclipse.rdf4j.repository.sparql.federation.SPARQLFederatedService}
 */
@Deprecated
public class SPARQLFederatedService extends org.eclipse.rdf4j.repository.sparql.federation.SPARQLFederatedService {

	public SPARQLFederatedService(String serviceUrl, HttpClientSessionManager client) {
		super(serviceUrl, client);
	}
}
