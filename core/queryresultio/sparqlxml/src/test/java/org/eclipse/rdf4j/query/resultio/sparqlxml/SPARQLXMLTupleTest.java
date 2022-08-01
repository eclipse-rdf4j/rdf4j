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
package org.eclipse.rdf4j.query.resultio.sparqlxml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;

import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.helpers.QueryResultCollector;
import org.eclipse.rdf4j.testsuite.query.resultio.AbstractQueryResultIOTupleTest;
import org.junit.Test;

/**
 * @author Peter Ansell
 */
public class SPARQLXMLTupleTest extends AbstractQueryResultIOTupleTest {

	@Override
	protected String getFileName() {
		return "test.srx";
	}

	@Override
	protected TupleQueryResultFormat getTupleFormat() {
		return TupleQueryResultFormat.SPARQL;
	}

	@Override
	protected BooleanQueryResultFormat getMatchingBooleanFormatOrNull() {
		return BooleanQueryResultFormat.SPARQL;
	}

	@Test
	public void testRDFStar_extendedFormatRDF4J() throws Exception {
		SPARQLResultsXMLParser parser = new SPARQLResultsXMLParser(SimpleValueFactory.getInstance());
		QueryResultCollector handler = new QueryResultCollector();
		parser.setQueryResultHandler(handler);

		InputStream stream = this.getClass().getResourceAsStream("/sparqlxml/rdfstar-extendedformat-rdf4j.srx");
		assertNotNull("Could not find test resource", stream);
		parser.parseQueryResult(stream);

		assertThat(handler.getBindingNames().size()).isEqualTo(3);
		assertThat(handler.getBindingSets()).hasSize(1).allMatch(bs -> bs.getValue("a") instanceof Triple);
		Triple a = (Triple) handler.getBindingSets().get(0).getValue("a");
		assertThat(a.getSubject().stringValue()).isEqualTo("http://example.org/bob");
		assertThat(a.getPredicate().stringValue()).isEqualTo("http://xmlns.com/foaf/0.1/age");
		assertThat(a.getObject().stringValue()).isEqualTo("23");
	}

	@Test
	public void testRDFStar_extendedFormatStardog() throws Exception {
		SPARQLResultsXMLParser parser = new SPARQLResultsXMLParser(SimpleValueFactory.getInstance());
		QueryResultCollector handler = new QueryResultCollector();
		parser.setQueryResultHandler(handler);

		InputStream stream = this.getClass().getResourceAsStream("/sparqlxml/rdfstar-extendedformat-stardog.srx");
		assertNotNull("Could not find test resource", stream);
		parser.parseQueryResult(stream);

		assertThat(handler.getBindingNames().size()).isEqualTo(3);
		assertThat(handler.getBindingSets()).hasSize(1).allMatch(bs -> bs.getValue("a") instanceof Triple);
		Triple a = (Triple) handler.getBindingSets().get(0).getValue("a");
		assertThat(a.getSubject().stringValue()).isEqualTo("http://example.org/bob");
		assertThat(a.getPredicate().stringValue()).isEqualTo("http://xmlns.com/foaf/0.1/age");
		assertThat(a.getObject().stringValue()).isEqualTo("23");
	}
}
