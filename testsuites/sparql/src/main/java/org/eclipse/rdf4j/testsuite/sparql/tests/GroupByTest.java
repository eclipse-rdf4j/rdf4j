/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.sparql.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.testsuite.sparql.AbstractComplianceTest;
import org.junit.Test;

/**
 * Tests on SPARQL GROUP BY
 * 
 * @author Jeen Broekstra
 */
public class GroupByTest extends AbstractComplianceTest {

	@Test
	public void testGroupByEmpty() {
		// see issue https://github.com/eclipse/rdf4j/issues/573
		String query = "select ?x where {?x ?p ?o} group by ?x";

		TupleQuery tq = conn.prepareTupleQuery(query);
		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);
			assertFalse(result.hasNext());
		}
	}

}
