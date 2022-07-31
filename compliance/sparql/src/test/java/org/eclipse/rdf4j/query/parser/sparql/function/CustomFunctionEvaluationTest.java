/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.sparql.function;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringReader;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for evaluation of custom functions in SPARQL
 *
 * @author Jeen Broekstra
 */
public class CustomFunctionEvaluationTest {

	private SailRepository rep;

	@Before
	public void setUp() {
		rep = new SailRepository(new MemoryStore());
	}

	@Test
	public void testTriplesourceRetrieval() throws Exception {
		String data = "<ex:s1> a <ex:CustomClass> . <ex:s1> <ex:related> <ex:s2>, <ex:s3> .";
		String query = "SELECT ?s ?result WHERE { ?s a <ex:CustomClass>. BIND(<urn:triplesourceCustomFunction>(?s) as ?result) }";

		try (RepositoryConnection conn = rep.getConnection()) {
			conn.add(new StringReader(data), "", RDFFormat.TURTLE);

			try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
				BindingSet bs = result.next();
				assertThat(bs.getValue("result").stringValue()).isEqualTo("related to ex:s2, ex:s3");

			}
		}

	}

}
