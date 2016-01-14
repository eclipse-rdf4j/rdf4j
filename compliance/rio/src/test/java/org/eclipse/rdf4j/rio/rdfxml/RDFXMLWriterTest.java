/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.rdfxml;

import org.eclipse.rdf4j.rio.rdfxml.RDFXMLParserFactory;
import org.eclipse.rdf4j.rio.rdfxml.RDFXMLWriterFactory;
import org.eclipse.rdf4j.rio.rdfxml.RDFXMLWriterTestCase;

public class RDFXMLWriterTest extends RDFXMLWriterTestCase {

	public RDFXMLWriterTest() {
		super(new RDFXMLWriterFactory(), new RDFXMLParserFactory());
	}
}
