/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.memory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.junit.Test;

/**
 * Testing the bind logic.
 */
public class MemoryStoreTest3696 {
	/**
	 * reproduces GH-3696
	 * 
	 * @throws Exception
	 */

	@Test
	public void testBind1() throws Exception {
		MemoryStore sail = new MemoryStore();

		NotifyingSailConnection con = sail.getConnection();
		addTestData(sail, con);

		ParsedTupleQuery filterUnit1 = (ParsedTupleQuery) QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL,
				"PREFIX ex: <http://example.org/>\n" + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
						+ "SELECT  * { \n" + "  ?bind rdfs:label ?b1 ;\n" + "        a ex:Unit .\n"
						+ "  FILTER (?b1 = 'Unit1') . \n" + "  BIND(?bind AS ?n0)\n" + "  ?n0 ex:has ?n1 \n" + " }",
				"http://example.com/");
		try (CloseableIteration<? extends BindingSet, QueryEvaluationException> res = con
				.evaluate(filterUnit1.getTupleExpr(), null, EmptyBindingSet.getInstance(), false)) {
			assertTrue("expect a result", res.hasNext());
			int count = 0;
			while (res.hasNext()) {
				BindingSet bs = res.next();
				Value bind = bs.getValue("bind");
				Value n0 = bs.getValue("n0");
				assertTrue("expect non-null value", bind != null);
				assertTrue("expect IRI", bind instanceof IRI);
				assertTrue("expect non-null value", n0 != null);
				assertTrue("expect IRI", n0 instanceof IRI);
				count++;
			}
			assertTrue("expect single solution", count == 1);
		}
	}

	@Test
	public void testBind2() throws Exception {
		MemoryStore sail = new MemoryStore();
		NotifyingSailConnection con = sail.getConnection();
		addTestData(sail, con);
		ParsedTupleQuery filterUnit2 = (ParsedTupleQuery) QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL,
				"PREFIX ex: <http://example.org/>\n" + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
						+ "SELECT  * { \n" + "  ?bind rdfs:label ?b1 ;\n" + "        a ex:Unit .\n"
						+ "  FILTER (?b1 = 'Unit2') . \n" + "  BIND(?bind AS ?n0)\n" + "  ?n0 ex:has ?n1 \n" + " }",
				"http://example.com/");

		try (CloseableIteration<? extends BindingSet, QueryEvaluationException> res = con
				.evaluate(filterUnit2.getTupleExpr(), null, EmptyBindingSet.getInstance(), false)) {
//			BindingSet bs = res.next();
//			Value bind = bs.getValue("bind");
//			Value n0 = bs.getValue("n0");
			assertFalse("expect no result", res.hasNext());
		}
	}

	private void addTestData(MemoryStore sail, NotifyingSailConnection con) {
		IRI unit = sail.getValueFactory().createIRI("http://example.org/Unit");
		IRI unit1 = sail.getValueFactory().createIRI("http://example.org/Unit1");
		IRI has = sail.getValueFactory().createIRI("http://example.org/has");
		Literal label1 = sail.getValueFactory().createLiteral("Unit1");
		IRI unit2 = sail.getValueFactory().createIRI("http://example.org/Unit2");
		Literal label2 = sail.getValueFactory().createLiteral("Unit2");
		con.begin();
		con.addStatement(unit1, RDF.TYPE, unit);
		con.addStatement(unit1, RDFS.LABEL, label1);
		con.addStatement(unit1, has, label1);
		con.addStatement(unit2, RDF.TYPE, unit);
		con.addStatement(unit2, RDFS.LABEL, label2);
		con.commit();
	}

}
