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
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.testsuite.sparql.AbstractComplianceTest;
import org.junit.Test;

/**
 * Test for queries using EXISTS
 *
 * @author Håvard M. Ottestad
 */
public class ExistsTest extends AbstractComplianceTest {

	@Test
	public void testFilterNotExistsBindingToCurrentSolutionMapping() {

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
						"  FILTER NOT EXISTS {\n" +
						"    ?a :predicate2 ?p2 .\n" +
						"    FILTER(?p2 = ?p1)\n" +
						"  }\n" +
						"}\n");

		try (Stream<BindingSet> stream = tupleQuery.evaluate().stream()) {
			List<BindingSet> collect = stream.collect(Collectors.toList());
			assertEquals(1, collect.size());
			assertEquals(a2, collect.get(0).getValue("a"));
		}

	}

}
