/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.TripleTerm;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RDF12Tests extends SPARQLBaseTest {

	@Test
	public void testRetrieval_openVar() throws Exception {

		prepareTest(Arrays.asList("/tests/rdf1_2/data01endpoint1.ttl", "/tests/rdf1_2/data01endpoint2.ttl"));

		var fedxRepo = fedxRule.getRepository();

		try (var conn = fedxRepo.getConnection()) {
			var tq = conn.prepareTupleQuery(
					"SELECT * WHERE { ?node <http://www.w3.org/1999/02/22-rdf-syntax-ns#reifies> ?tripleTerm }");
			var res = Iterations.asList(tq.evaluate());

			Assertions.assertEquals(
					Set.of(ex("bob"), ex("alice")),
					res.stream()
							.map(bs -> bs.getValue("tripleTerm"))
							.map(TripleTerm.class::cast)
							.map(tt -> tt.getSubject())
							.collect(Collectors.toSet()));
		}
	}

	@Test
	public void testRetrieval_sparql1_2_singleSource() throws Exception {

		prepareTest(Arrays.asList("/tests/rdf1_2/data01endpoint1.ttl", "/tests/rdf1_2/data01endpoint2.ttl"));

		var fedxRepo = fedxRule.getRepository();

		try (var conn = fedxRepo.getConnection()) {
			var tq = conn.prepareTupleQuery(
					"""
							PREFIX ex:    <http://www.example.org/>
							PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
							SELECT * WHERE {
							   ?node rdf:reifies <<( ex:bob ex:jobTitle ?jobTitle )>>
							}
							""");
			var res = Iterations.asList(tq.evaluate());

			Assertions.assertEquals(1, res.size());
			Assertions.assertEquals("Designer", res.get(0).getValue("jobTitle").stringValue());
		}
	}

	@Test
	public void testRetrieval_sparql1_2_join_singleSource() throws Exception {

		prepareTest(Arrays.asList("/tests/rdf1_2/data01endpoint1.ttl", "/tests/rdf1_2/data01endpoint2.ttl"));

		var fedxRepo = fedxRule.getRepository();

		try (var conn = fedxRepo.getConnection()) {
			var tq = conn.prepareTupleQuery(
					"""
							PREFIX ex:    <http://www.example.org/>
							PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
							SELECT * WHERE {
							   ?node rdf:reifies <<( ex:bob ex:jobTitle ?jobTitle )>> ;
							      ex:accordingTo ex:sourceBobTitle .
							}
							""");
			var res = Iterations.asList(tq.evaluate());

			Assertions.assertEquals(1, res.size());
			Assertions.assertEquals("Designer", res.get(0).getValue("jobTitle").stringValue());
		}
	}

	@Test
	public void testRetrieval_sparql1_2_variant_singleSource() throws Exception {

		prepareTest(Arrays.asList("/tests/rdf1_2/data01endpoint1.ttl", "/tests/rdf1_2/data01endpoint2.ttl"));

		var fedxRepo = fedxRule.getRepository();

		try (var conn = fedxRepo.getConnection()) {
			var tq = conn.prepareTupleQuery(
					"""
							PREFIX ex:    <http://www.example.org/>
							PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
							SELECT * WHERE {
							   << ex:bob ex:jobTitle ?jobTitle >> ex:accordingTo ex:sourceBobTitle
							}
							""");
			var res = Iterations.asList(tq.evaluate());

			Assertions.assertEquals(1, res.size());
			Assertions.assertEquals("Designer", res.get(0).getValue("jobTitle").stringValue());
		}
	}

	// real federated queries

	static IRI ex(String localName) {
		return Values.iri("http://www.example.org/", localName);
	}
}
