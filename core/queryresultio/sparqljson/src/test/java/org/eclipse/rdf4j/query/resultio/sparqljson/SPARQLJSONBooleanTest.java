/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio.sparqljson;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.helpers.QueryResultCollector;
import org.eclipse.rdf4j.testsuite.query.resultio.AbstractQueryResultIOBooleanTest;
import org.junit.Test;

/**
 * @author Peter Ansell
 * @author Sebastian Schaffert
 */
public class SPARQLJSONBooleanTest extends AbstractQueryResultIOBooleanTest {

	@Override
	protected String getFileName() {
		return "test.srj";
	}

	@Override
	protected BooleanQueryResultFormat getBooleanFormat() {
		return BooleanQueryResultFormat.JSON;
	}

	@Override
	protected TupleQueryResultFormat getMatchingTupleFormatOrNull() {
		return TupleQueryResultFormat.JSON;
	}

	@Test
	public void testBoolean1() throws Exception {
		SPARQLBooleanJSONParser parser = new SPARQLBooleanJSONParser(SimpleValueFactory.getInstance());
		QueryResultCollector handler = new QueryResultCollector();
		parser.setQueryResultHandler(handler);

		parser.parseQueryResult(this.getClass().getResourceAsStream("/sparqljson/boolean1.srj"));

		assertTrue(handler.getBoolean());
	}

	@Test
	public void testBoolean2() throws Exception {
		SPARQLBooleanJSONParser parser = new SPARQLBooleanJSONParser(SimpleValueFactory.getInstance());
		QueryResultCollector handler = new QueryResultCollector();
		parser.setQueryResultHandler(handler);

		parser.parseQueryResult(this.getClass().getResourceAsStream("/sparqljson/boolean2.srj"));

		assertTrue(handler.getBoolean());
	}

	@Test
	public void testBoolean3() throws Exception {
		SPARQLBooleanJSONParser parser = new SPARQLBooleanJSONParser(SimpleValueFactory.getInstance());
		QueryResultCollector handler = new QueryResultCollector();
		parser.setQueryResultHandler(handler);

		parser.parseQueryResult(this.getClass().getResourceAsStream("/sparqljson/boolean3.srj"));

		assertFalse(handler.getBoolean());
	}

	@Test
	public void testBoolean4() throws Exception {
		SPARQLBooleanJSONParser parser = new SPARQLBooleanJSONParser(SimpleValueFactory.getInstance());
		QueryResultCollector handler = new QueryResultCollector();
		parser.setQueryResultHandler(handler);

		parser.parseQueryResult(this.getClass().getResourceAsStream("/sparqljson/boolean4.srj"));

		assertFalse(handler.getBoolean());
	}

}
