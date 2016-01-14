/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.ntriples;

import org.eclipse.rdf4j.rio.RDFWriterTest;
import org.eclipse.rdf4j.rio.ntriples.NTriplesParserFactory;
import org.eclipse.rdf4j.rio.ntriples.NTriplesWriterFactory;

/**
 * JUnit test for the RDF/JSON parser.
 * @author Peter Ansell
 */
public class NTriplesWriterTest extends RDFWriterTest {

	public NTriplesWriterTest() {
		super(new NTriplesWriterFactory(), new NTriplesParserFactory());
	}
}
