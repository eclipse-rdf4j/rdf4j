/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.sparql.tests;

import static org.junit.Assert.fail;

import org.eclipse.rdf4j.model.vocabulary.DC;
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
}
