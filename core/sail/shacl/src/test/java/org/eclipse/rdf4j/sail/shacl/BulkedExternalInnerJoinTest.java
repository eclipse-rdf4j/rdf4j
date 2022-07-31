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

package org.eclipse.rdf4j.sail.shacl;

public class BulkedExternalInnerJoinTest {
	/*
	 * @Test public void gapInResultsFromQueryTest() {
	 *
	 * SimpleValueFactory vf = SimpleValueFactory.getInstance(); IRI a = vf.createIRI("http://a"); IRI b =
	 * vf.createIRI("http://b"); IRI c = vf.createIRI("http://c"); IRI d = vf.createIRI("http://d");
	 *
	 * PlanNode left = new MockInputPlanNode( Arrays.asList(new Tuple(Collections.singletonList(a)), new
	 * Tuple(Collections.singletonList(b)), new Tuple(Collections.singletonList(c)), new
	 * Tuple(Collections.singletonList(d))));
	 *
	 * MemoryStore sailRepository = new MemoryStore(); sailRepository.init();
	 *
	 * try (SailConnection connection = sailRepository.getConnection()) { connection.begin(); connection.addStatement(b,
	 * DCAT.ACCESS_URL, RDFS.RESOURCE); connection.addStatement(d, DCAT.ACCESS_URL, RDFS.SUBPROPERTYOF);
	 * connection.commit(); } try (SailConnection connection = sailRepository.getConnection()) {
	 *
	 * BulkedExternalInnerJoin bulkedExternalInnerJoin = new BulkedExternalInnerJoin(left, connection,
	 * "?a <http://www.w3.org/ns/dcat#accessURL> ?c. ", false, null, "?a", "?c");
	 *
	 * List<Tuple> tuples = new MockConsumePlanNode(bulkedExternalInnerJoin).asList();
	 *
	 * tuples.forEach(System.out::println);
	 *
	 * assertEquals("[http://b, http://www.w3.org/2000/01/rdf-schema#Resource]",
	 * Arrays.toString(tuples.get(0).getLine().toArray()));
	 * assertEquals("[http://d, http://www.w3.org/2000/01/rdf-schema#subPropertyOf]",
	 * Arrays.toString(tuples.get(1).getLine().toArray()));
	 *
	 * } }
	 */
}
