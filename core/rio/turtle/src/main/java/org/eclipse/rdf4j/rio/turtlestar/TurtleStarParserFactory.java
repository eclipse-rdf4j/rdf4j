/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.turtlestar;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFParserFactory;

/**
 * An {@link RDFParserFactory} for Turtle-star parsers.
 *
 * @author Pavel Mihaylov
 */
public class TurtleStarParserFactory implements RDFParserFactory {
	/**
	 * Returns {@link RDFFormat#TURTLESTAR}.
	 */
	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.TURTLESTAR;
	}

	/**
	 * Returns a new instance of {@link TurtleStarParser}.
	 */
	@Override
	public RDFParser getParser() {
		return new TurtleStarParser();
	}
}
