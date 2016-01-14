/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.trig;

import org.eclipse.rdf4j.rio.RDFWriterTest;
import org.eclipse.rdf4j.rio.trig.TriGParserFactory;
import org.eclipse.rdf4j.rio.trig.TriGWriterFactory;

/**
 * @author Arjohn Kampman
 */
public class TriGWriterTest extends RDFWriterTest {

	public TriGWriterTest() {
		super(new TriGWriterFactory(), new TriGParserFactory());
	}
}
