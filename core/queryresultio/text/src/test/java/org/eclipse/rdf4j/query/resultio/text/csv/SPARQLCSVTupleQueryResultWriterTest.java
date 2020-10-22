/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio.text.csv;

import org.eclipse.rdf4j.query.resultio.AbstractTupleQueryResultWriterTest;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultParserFactory;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriterFactory;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Jeen Broekstra
 *
 */
public class SPARQLCSVTupleQueryResultWriterTest extends AbstractTupleQueryResultWriterTest {

	@Override
	protected TupleQueryResultParserFactory getParserFactory() {
		return new SPARQLResultsCSVParserFactory();
	}

	@Override
	protected TupleQueryResultWriterFactory getWriterFactory() {
		return new SPARQLResultsCSVWriterFactory();
	}

	@Override
	@Ignore("pending implementation of RDF* extensions for the csv format")
	@Test
	public void testRDFStarHandling_NoEncoding() throws Exception {
	}

	@Override
	@Ignore("pending implementation of RDF* extensions for the csv format")
	@Test
	public void testRDFStarHandling_DeepNesting() throws Exception {
	}
}
