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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.testsuite.sparql.AbstractComplianceTest;
import org.junit.Test;

/**
 * Test for queries using MINUS
 *
 * @author Håvard M. Ottestad
 */
public class MinusTest extends AbstractComplianceTest {

	@Test
	public void testScopingOfFilterInMinus() {

		String ex = "http://example/";
		IRI a1 = Values.iri(ex, "a1");
		IRI a2 = Values.iri(ex, "a2");

		IRI both = Values.iri(ex, "both");

		IRI predicate1 = Values.iri(ex, "predicate1");
		IRI predicate2 = Values.iri(ex, "predicate2");

		conn.add(a1, predicate1, both);
		conn.add(a1, predicate2, both);

		conn.add(a2, predicate1, both);
		conn.add(a2, predicate2, Values.bnode());

		TupleQuery tupleQuery = conn.prepareTupleQuery(
				"PREFIX : <http://example/>\n" +
						"SELECT * WHERE {\n" +
						"  ?a :predicate1 ?p1\n" +
						"  MINUS {\n" +
						"    ?a :predicate2 ?p2 .\n" +
						"    FILTER(?p2 = ?p1)\n" +
						"  }\n" +
						"} ORDER BY ?a\n"
		);

		try (Stream<BindingSet> stream = tupleQuery.evaluate().stream()) {
			List<BindingSet> collect = stream.collect(Collectors.toList());
			assertEquals(2, collect.size());

			List<Value> expectedValues = List.of(a1, a2);
			List<Value> actualValues = collect
					.stream()
					.map(b -> b.getValue("a"))
					.collect(Collectors.toList());

			assertEquals(expectedValues, actualValues);
		}

	}

}
