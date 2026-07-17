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

/**
 * JUnit test for the RDF/JSON parser.
 *
 * @author Peter Ansell
 */
public class NTriplesWriterTest extends AbstractNTriplesWriterTest {

	public NTriplesWriterTest() {
		super(new NTriplesWriterFactory(), new NTriplesParserFactory());
	}
}
