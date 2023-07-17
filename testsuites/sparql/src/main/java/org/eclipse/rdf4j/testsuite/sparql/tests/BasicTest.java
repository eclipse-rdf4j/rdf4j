/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.sparql.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.AbstractTupleQueryResultHandler;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.testsuite.sparql.AbstractComplianceTest;
import org.eclipse.rdf4j.testsuite.sparql.vocabulary.EX;
import org.junit.Test;

/**
 * Basic SPARQL functionality tests
 *
 * @author Jeen Broekstra
 *
 */
public class BasicTest extends AbstractComplianceTest {

	@Test
	public void testIdenticalVariablesInStatementPattern() {
		conn.add(EX.ALICE, DC.PUBLISHER, EX.BOB);

		String queryBuilder = "SELECT ?publisher " +
				"{ ?publisher <http://purl.org/dc/elements/1.1/publisher> ?publisher }";

		conn.prepareTupleQuery(QueryLanguage.SPARQL, queryBuilder)
				.evaluate(new AbstractTupleQueryResultHandler() {

					@Override
					public void handleSolution(BindingSet bindingSet) {
						fail("nobody is self published");
					}
				});
	}

	@Test
	public void testIdenticalVariablesSubjectContextInStatementPattern() {
		conn.add(EX.ALICE, FOAF.KNOWS, EX.BOB, EX.ALICE);
		conn.add(EX.ALICE, RDF.TYPE, FOAF.PERSON, EX.ALICE);
		conn.add(EX.ALICE, FOAF.KNOWS, EX.A, EX.BOB);
		conn.add(EX.ALICE, FOAF.KNOWS, EX.B, EX.BOB);
		conn.add(EX.ALICE, FOAF.KNOWS, EX.C, EX.BOB);
		conn.add(EX.ALICE, FOAF.KNOWS, EX.MARY, EX.BOB);

		String queryBuilder = "SELECT ?knows { " +
				"	graph ?alice {" +
				"		?alice a <" + FOAF.PERSON + ">; " +
				"			<" + FOAF.KNOWS + "> ?knows ." +
				"		}" +
				"}";

		try (Stream<BindingSet> stream = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryBuilder)
				.evaluate()
				.stream()) {
			List<Value> knows = stream.map(b -> b.getValue("knows")).collect(Collectors.toList());
			assertEquals(List.of(EX.BOB), knows);
		}
	}

}
