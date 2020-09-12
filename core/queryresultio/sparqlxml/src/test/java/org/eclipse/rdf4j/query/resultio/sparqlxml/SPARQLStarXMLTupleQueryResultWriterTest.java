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

/**
 * @author Jeen Broekstra
 *
 */
public class SPARQLStarXMLTupleQueryResultWriterTest extends AbstractTupleQueryResultWriterTest {

	@Override
	protected TupleQueryResultParserFactory getParserFactory() {
		return new SPARQLStarResultsXMLParserFactory();
	}

	@Override
	protected TupleQueryResultWriterFactory getWriterFactory() {
		return new SPARQLStarResultsXMLWriterFactory();
	}

}
