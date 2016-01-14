/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.rdfjson;

import static org.junit.Assert.*;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.Test;

/**
 * Custom (non-manifest) tests for RDF/JSON parser.
 * 
 * @author Peter Ansell
 */
public class RDFJSONParserCustomTest {

	@Test
	public void testSupportedSettings()
		throws Exception
	{
		assertEquals(17, Rio.createParser(RDFFormat.RDFJSON).getSupportedSettings().size());
	}

}
