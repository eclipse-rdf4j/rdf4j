/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.http.server.readonly;

import org.eclipse.rdf4j.http.client.SPARQLProtocolSession;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;

public class TestSPARQLRepository extends SPARQLRepository {

	public TestSPARQLRepository(String endpointUrl) {
		super(endpointUrl);
	}

	public TestSPARQLRepository(String queryEndpointUrl, String updateEndpointUrl) {
		super(queryEndpointUrl, updateEndpointUrl);
	}

	@Override
	public SPARQLProtocolSession createSPARQLProtocolSession() {
		return super.createSPARQLProtocolSession();
	}

}
