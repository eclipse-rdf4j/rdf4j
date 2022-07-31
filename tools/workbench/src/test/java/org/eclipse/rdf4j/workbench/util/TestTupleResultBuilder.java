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
package org.eclipse.rdf4j.workbench.util;

import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.net.URL;

import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLResultsJSONWriter;
import org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLBooleanXMLWriter;
import org.junit.Test;

/**
 * @author Dale Visser
 */
public class TestTupleResultBuilder {

	@Test
	public final void testSES1780regression() throws Exception {
		TupleResultBuilder builder = new TupleResultBuilder(new SPARQLResultsJSONWriter(new ByteArrayOutputStream()),
				SimpleValueFactory.getInstance());
		builder.start("test");
		builder.namedResult("test", new URL("http://www.foo.org/bar#"));
		builder.end();
	}

	@Test
	public final void testSES1726regression() throws Exception {
		TupleResultBuilder builder = new TupleResultBuilder(new SPARQLResultsJSONWriter(new ByteArrayOutputStream()),
				SimpleValueFactory.getInstance());
		try {
			builder.namedResult("test", new URL("http://www.foo.org/bar#"));
			fail("Did not receive expected exception for calling namedResult before start");
		} catch (IllegalStateException ise) {
			// Expected exception
		}
	}

	@Test
	public final void testSES1846Normal() throws Exception {
		TupleResultBuilder builder = new TupleResultBuilder(new SPARQLBooleanXMLWriter(new ByteArrayOutputStream()),
				SimpleValueFactory.getInstance());
		builder.startBoolean();
		builder.bool(true);
		builder.endBoolean();
	}

	@Test
	public final void testSES1846regression() throws Exception {
		TupleResultBuilder builder = new TupleResultBuilder(new SPARQLBooleanXMLWriter(new ByteArrayOutputStream()),
				SimpleValueFactory.getInstance());
		try {
			builder.start();
			builder.bool(true);
			fail("Did not receive expected exception for calling bool after start");
		} catch (QueryResultHandlerException qrhe) {
			// Expected exception
		}
	}

}
