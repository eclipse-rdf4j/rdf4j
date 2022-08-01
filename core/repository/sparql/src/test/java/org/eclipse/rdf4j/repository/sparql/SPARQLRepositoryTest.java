/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sparql;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Executors;

import org.apache.http.client.HttpClient;
import org.eclipse.rdf4j.http.client.HttpClientSessionManager;
import org.eclipse.rdf4j.http.client.RDF4JProtocolSession;
import org.eclipse.rdf4j.http.client.SPARQLProtocolSession;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SPARQLRepositoryTest {

	String endpointUrl = "http://example.org/sparql";
	TupleQueryResultFormat customPreferred = TupleQueryResultFormat.CSV;

	@BeforeEach
	public void setUp() throws Exception {
	}

	@Test
	public void testCustomPreferredTupleQueryResultFormat() {
		SPARQLRepository rep = new SPARQLRepository(endpointUrl);
		rep.setHttpClientSessionManager(new HttpClientSessionManager() {

			@Override
			public void shutDown() {
				// TODO Auto-generated method stub

			}

			@Override
			public HttpClient getHttpClient() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public SPARQLProtocolSession createSPARQLProtocolSession(String queryEndpointUrl,
					String updateEndpointUrl) {
				SPARQLProtocolSession session = new SPARQLProtocolSession(getHttpClient(),
						Executors.newSingleThreadExecutor());
				session.setPreferredTupleQueryResultFormat(customPreferred);
				return session;
			}

			@Override
			public RDF4JProtocolSession createRDF4JProtocolSession(String serverURL) {
				// TODO Auto-generated method stub
				return null;
			}
		});

		assertThat(rep.createSPARQLProtocolSession().getPreferredTupleQueryResultFormat()).isEqualTo(customPreferred);
	}

	public void testPassThroughEnabled() throws Exception {
		SPARQLRepository rep = new SPARQLRepository(endpointUrl);
		assertThat(rep.getPassThroughEnabled()).isNull();
		assertThat(rep.createSPARQLProtocolSession()).isNotNull();

		rep.setPassThroughEnabled(true);
		assertThat(rep.getPassThroughEnabled()).isTrue();
		assertThat(rep.createSPARQLProtocolSession().isPassThroughEnabled()).isTrue();

		rep.setPassThroughEnabled(false);
		assertThat(rep.getPassThroughEnabled()).isFalse();
		assertThat(rep.createSPARQLProtocolSession().isPassThroughEnabled()).isFalse();
	}
}
