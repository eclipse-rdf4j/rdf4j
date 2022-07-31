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
package org.eclipse.rdf4j.rio.ntriples;

import org.eclipse.rdf4j.rio.RDFParser;

/**
 * JUnit test for the N-Triples parser.
 *
 * @author Arjohn Kampman
 */
public class NTriplesParserUnitTest extends AbstractNTriplesParserUnitTest {

	@Override
	protected RDFParser createRDFParser() {
		return new NTriplesParser();
	}
}
