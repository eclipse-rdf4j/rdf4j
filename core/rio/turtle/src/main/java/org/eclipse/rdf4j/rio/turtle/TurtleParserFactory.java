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
package org.eclipse.rdf4j.rio.turtle;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFParserFactory;

/**
 * An {@link RDFParserFactory} for Turtle parsers.
 *
 * @author Arjohn Kampman
 */
public class TurtleParserFactory implements RDFParserFactory {

	/**
	 * Returns {@link RDFFormat#TURTLE}.
	 */
	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.TURTLE;
	}

	/**
	 * Returns a new instance of {@link TurtleParser}.
	 */
	@Override
	public RDFParser getParser() {
		return new TurtleParser();
	}
}
