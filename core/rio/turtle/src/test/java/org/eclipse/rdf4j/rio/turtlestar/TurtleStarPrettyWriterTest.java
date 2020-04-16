/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.turtlestar;

import org.eclipse.rdf4j.rio.RDFWriterTest;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;

/**
 * @author Pavel Mihaylov
 */
public class TurtleStarPrettyWriterTest extends RDFWriterTest {
	public TurtleStarPrettyWriterTest() {
		super(new TurtleStarWriterFactory(), new TurtleStarParserFactory());
	}

	@Override
	protected void setupWriterConfig(WriterConfig config) {
		config.set(BasicWriterSettings.PRETTY_PRINT, true);
	}
}
