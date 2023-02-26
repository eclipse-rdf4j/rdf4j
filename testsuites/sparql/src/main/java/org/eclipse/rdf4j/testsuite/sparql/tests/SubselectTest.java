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
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.testsuite.sparql.AbstractComplianceTest;
import org.junit.Test;

/**
 * Tests on SPARQL nested SELECT query handling.
 *
 * @author Jeen Broekstra
 */
public class SubselectTest extends AbstractComplianceTest {

	@Test
	public void testSES2373SubselectOptional() {
		conn.prepareUpdate(QueryLanguage.SPARQL,
				"insert data {" + "<u:1> <u:r> <u:subject> ." + "<u:1> <u:v> 1 ." + "<u:1> <u:x> <u:x1> ."
						+ "<u:2> <u:r> <u:subject> ." + "<u:2> <u:v> 2 ." + "<u:2> <u:x> <u:x2> ."
						+ "<u:3> <u:r> <u:subject> ." + "<u:3> <u:v> 3 ." + "<u:3> <u:x> <u:x3> ."
						+ "<u:4> <u:r> <u:subject> ." + "<u:4> <u:v> 4 ." + "<u:4> <u:x> <u:x4> ."
						+ "<u:5> <u:r> <u:subject> ." + "<u:5> <u:v> 5 ." + "<u:5> <u:x> <u:x5> ." + "}")
				.execute();

		String qb = "select ?x { \n" +
				" { select ?v { ?v <u:r> <u:subject> filter (?v = <u:1>) } }.\n" +
				"  optional {  select ?val { ?v <u:v> ?val .} }\n" +
				"  ?v <u:x> ?x \n" +
				"}\n";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, qb);
		try (TupleQueryResult result = tq.evaluate()) {
			assertTrue("The query should return a result", result.hasNext());
			BindingSet b = result.next();
			assertTrue("?x is from the mandatory part of the query and should be bound", b.hasBinding("x"));
		}
	}

	@Test
	public void testSES2154SubselectOptional() {

		String ub = "insert data { \n" +
				" <urn:s1> a <urn:C> .  \n" +
				" <urn:s2> a <urn:C> .  \n" +
				" <urn:s3> a <urn:C> .  \n" +
				" <urn:s4> a <urn:C> .  \n" +
				" <urn:s5> a <urn:C> .  \n" +
				" <urn:s6> a <urn:C> .  \n" +
				" <urn:s7> a <urn:C> .  \n" +
				" <urn:s8> a <urn:C> .  \n" +
				" <urn:s9> a <urn:C> .  \n" +
				" <urn:s10> a <urn:C> .  \n" +
				" <urn:s11> a <urn:C> .  \n" +
				" <urn:s12> a <urn:C> .  \n" +
				" <urn:s1> <urn:p> \"01\" .  \n" +
				" <urn:s2> <urn:p> \"02\" .  \n" +
				" <urn:s3> <urn:p> \"03\" .  \n" +
				" <urn:s4> <urn:p> \"04\" .  \n" +
				" <urn:s5> <urn:p> \"05\" .  \n" +
				" <urn:s6> <urn:p> \"06\" .  \n" +
				" <urn:s7> <urn:p> \"07\" .  \n" +
				" <urn:s8> <urn:p> \"08\" .  \n" +
				" <urn:s9> <urn:p> \"09\" .  \n" +
				" <urn:s10> <urn:p> \"10\" .  \n" +
				" <urn:s11> <urn:p> \"11\" .  \n" +
				" <urn:s12> <urn:p> \"12\" .  \n" +
				"} \n";

		conn.prepareUpdate(QueryLanguage.SPARQL, ub).execute();

		String qb = "SELECT ?s ?label\n" +
				"WHERE { \n" +
				" 	  ?s a <urn:C> \n .\n" +
				" 	  OPTIONAL  { {SELECT ?label  WHERE { \n" +
				"                     ?s <urn:p> ?label . \n" +
				"   	      } ORDER BY ?label LIMIT 2 \n" +
				"		    }\n" +
				"       }\n" +
				"}\n" +
				"ORDER BY ?s\n" +
				"LIMIT 10 \n";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, qb);
		try (TupleQueryResult evaluate = tq.evaluate()) {
			assertTrue("The query should return a result", evaluate.hasNext());

			List<BindingSet> result = QueryResults.asList(evaluate);
			assertEquals(10, result.size());
			for (BindingSet bs : result) {
				Literal label = (Literal) bs.getValue("label");
				assertTrue("wrong label value (expected '01' or '02', but got '" + label.stringValue() + "')",
						label.stringValue().equals("01") || label.stringValue().equals("02"));
			}
		}
	}
}
