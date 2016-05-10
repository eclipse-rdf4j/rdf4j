/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.nquads;

import org.eclipse.rdf4j.rio.nquads.AbstractNQuadsWriterTest;
import org.eclipse.rdf4j.rio.nquads.NQuadsParserFactory;
import org.eclipse.rdf4j.rio.nquads.NQuadsWriterFactory;

public class NQuadsWriterTest extends AbstractNQuadsWriterTest {

	public NQuadsWriterTest() {
		super(new NQuadsWriterFactory(), new NQuadsParserFactory());
	}

}
