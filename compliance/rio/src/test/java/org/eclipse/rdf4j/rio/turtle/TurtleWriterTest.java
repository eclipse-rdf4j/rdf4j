/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.turtle;

import org.eclipse.rdf4j.rio.RDFWriterTest;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.turtle.TurtleParserFactory;
import org.eclipse.rdf4j.rio.turtle.TurtleWriterFactory;

/**
 * @author Arjohn Kampman
 */
public class TurtleWriterTest extends RDFWriterTest {

	public TurtleWriterTest() {
		super(new TurtleWriterFactory(), new TurtleParserFactory());
	}

	@Override
	protected void setupWriterConfig(WriterConfig config) {
		config.set(BasicWriterSettings.PRETTY_PRINT, false);
	}
}
