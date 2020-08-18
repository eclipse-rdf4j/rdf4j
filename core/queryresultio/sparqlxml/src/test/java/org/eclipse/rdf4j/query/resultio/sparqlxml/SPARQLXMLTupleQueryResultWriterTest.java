/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio.sparqlxml;

import org.eclipse.rdf4j.query.resultio.AbstractTupleQueryResultWriterTest;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultParserFactory;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriterFactory;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Jeen Broekstra
 *
 */
public class SPARQLXMLTupleQueryResultWriterTest extends AbstractTupleQueryResultWriterTest {

	@Override
	protected TupleQueryResultParserFactory getParserFactory() {
		return new SPARQLResultsXMLParserFactory();
	}

	@Override
	protected TupleQueryResultWriterFactory getWriterFactory() {
		return new SPARQLResultsXMLWriterFactory();
	}

	@Override
	@Test
	@Ignore("pending implementation of RDF* extensions for the xml format - see https://github.com/eclipse/rdf4j/issues/2054")
	public void testRDFStarHandling_NoEncoding() throws Exception {
	}
}
