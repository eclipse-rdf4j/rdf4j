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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.function.Supplier;
import java.util.stream.Stream;

import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.testsuite.sparql.AbstractComplianceTest;
import org.junit.jupiter.api.DynamicTest;

/**
 * Tests on SPARQL GROUP BY
 *
 * @author Jeen Broekstra
 */
public class GroupByTest extends AbstractComplianceTest {

	public GroupByTest(Supplier<Repository> repo) {
		super(repo);
	}

	private void testGroupByEmpty(RepositoryConnection conn) {
		// see issue https://github.com/eclipse/rdf4j/issues/573
		String query = "select ?x where {?x ?p ?o} group by ?x";

		TupleQuery tq = conn.prepareTupleQuery(query);
		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);
			assertFalse(result.hasNext());
		}
	}

	public Stream<DynamicTest> tests() {
		return Stream.of(makeTest("GroupByEmpty", this::testGroupByEmpty));
	}

}
