/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sail.helpers;

import java.io.IOException;
import java.io.StringReader;

import org.eclipse.rdf4j.repository.sail.helpers.SPARQLUpdateDataBlockParser;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.junit.Test;

/**
 * @author Damyan Ognyanov
 */
public class TestSPARQLUpdateDataBlockParser {

	/**
	 * A case reproducing SES-2258 using two cases with optional 'dot'. If not
	 * handled properly by SPARQLUpdateDataBlockParser.parseGraph(), an Exception
	 * is throws and test fails.
	 */
	@Test
	public void testParseGraph()
		throws RDFParseException, RDFHandlerException, IOException
	{
		SPARQLUpdateDataBlockParser parser = new SPARQLUpdateDataBlockParser();
		String blocksToCheck[] = new String[] {
				"graph <u:g1> {<u:1> <p:1> 1 } . <u:2> <p:2> 2.",
				"graph <u:g1> {<u:1> <p:1> 1 .} . <u:2> <p:2> 2." };
		for (String block : blocksToCheck) {
			parser.parse(new StringReader(block), "http://base.org");
		}
	}

}
