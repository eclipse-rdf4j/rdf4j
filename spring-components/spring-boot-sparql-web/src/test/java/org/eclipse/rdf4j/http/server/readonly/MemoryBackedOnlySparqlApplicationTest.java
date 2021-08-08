/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.http.server.readonly;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(MemoryBackedOnlySparqlApplicationTestConfig.class)
public class MemoryBackedOnlySparqlApplicationTest {
	@LocalServerPort
	private int port;

	@Autowired
	private QueryResponder queryResponder;
	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	public void contextLoads() {
		assertThat(queryResponder).isNotNull();
	}

	@Test
	public void testAskQuery() {
		assertThat(this.restTemplate.getForObject("http://localhost:" + port + "/sparql/?query={query}",
				String.class, "ASK { ?s ?p ?o }")).contains("true");

	}

	@Test
	public void testSelectQuery() {
		String forObject = this.restTemplate.getForObject("http://localhost:" + port + "/sparql/?query={query}",
				String.class, "SELECT * WHERE { ?s ?p ?o }");
		assertThat(forObject).contains("http://www.w3.org/1999/02/22-rdf-syntax-ns#Bag");
	}

}
