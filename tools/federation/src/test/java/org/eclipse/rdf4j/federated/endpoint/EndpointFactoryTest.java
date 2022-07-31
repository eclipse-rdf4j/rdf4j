/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.endpoint;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.eclipse.rdf4j.federated.SPARQLBaseTest;
import org.eclipse.rdf4j.federated.server.SPARQLEmbeddedServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class EndpointFactoryTest extends SPARQLBaseTest {

	@Test
	public void testValidSparqlEndpoint() throws Exception {

		assumeSparqlEndpoint();

		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));

		String endpointUrl = ((SPARQLEmbeddedServer) server).getRepositoryUrl("endpoint1");
		EndpointBase e = (EndpointBase) EndpointFactory.loadSPARQLEndpoint(endpointUrl);

		Assertions.assertEquals("http://localhost_18080", e.getName());
		Assertions.assertEquals("sparql_localhost:18080_repositories_endpoint1", e.getId());
		Assertions.assertEquals("http://localhost:18080/repositories/endpoint1", e.getEndpoint());
		Assertions.assertEquals(EndpointType.SparqlEndpoint, e.getType());
	}

	@Test
	@Disabled // needs to be fixed, connection timeout needs to be set
	public void testNotReachableEndpoint() throws Exception {

		try {
			EndpointFactory.loadSPARQLEndpoint("http://invalid.org/not_sparql");
			Assertions.fail("Expected exception that endpoint is invalid");
		} catch (Exception expected) {

		}

	}

	@Test
	public void testDataConfig() throws Exception {

		File baseDir = new File("target/tmp/fedxTest");

		File dataConfig = new File(
				EndpointFactoryTest.class.getResource("/tests/dataconfig/endpointfactoryTest.ttl").toURI());

		List<Endpoint> endpoints = EndpointFactory.loadFederationMembers(dataConfig, baseDir);

		endpoints.sort((e1, e2) -> e1.getName().compareTo(e2.getName()));

		Assertions.assertEquals(3, endpoints.size());

		Endpoint dbpediaSparql = endpoints.get(0);
		Assertions.assertEquals("http://dbpedia", dbpediaSparql.getName());
		Assertions.assertEquals("sparql_dbpedia", dbpediaSparql.getId());
		Assertions.assertEquals("http://dbpedia.org/sparql", dbpediaSparql.getEndpoint());

		Endpoint dbpediaLocal = endpoints.get(1);
		Assertions.assertEquals("http://dbpedia.local", dbpediaLocal.getName());
		Assertions.assertEquals("remote_dbpedia.local", dbpediaLocal.getId());
		Assertions.assertEquals("http://10.212.10.29:8088/rdf4j-server/dbpedia", dbpediaLocal.getEndpoint());

		Endpoint nativeStore = endpoints.get(2);
		Assertions.assertEquals("http://dbpedia.native", nativeStore.getName());
		Assertions.assertEquals("dbmodel", nativeStore.getId());
		Assertions.assertEquals("dbmodel", nativeStore.getEndpoint());
		Assertions.assertEquals(new File("target/tmp/fedxTest", "repositories/dbmodel"),
				((ManagedRepositoryEndpoint) nativeStore).repository.getDataDir());

	}

	@Test
	public void testDataConfig_writableEndpoint() throws Exception {

		File baseDir = new File("target/tmp/fedxTest");

		File dataConfig = new File(
				EndpointFactoryTest.class.getResource("/tests/dataconfig/endpointfactoryTest_writable.ttl").toURI());

		List<Endpoint> endpoints = EndpointFactory.loadFederationMembers(dataConfig, baseDir);

		endpoints.sort((e1, e2) -> e1.getName().compareTo(e2.getName()));

		assertThat(endpoints.size()).isEqualTo(3);

		Endpoint nativeStore = endpoints.get(2);
		assertThat(nativeStore.getName()).isEqualTo("http://dbpedia.native");
		assertThat(nativeStore.getId()).isEqualTo("dbmodel");
		assertThat(nativeStore.getEndpoint()).isEqualTo("dbmodel");
		assertThat(nativeStore.isWritable()).isTrue();
	}

}
