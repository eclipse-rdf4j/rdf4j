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
package org.eclipse.rdf4j.rio.n3;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFParserFactory;

/**
 * An {@link RDFParserFactory} for N3 parsers.
 *
 * @author Arjohn Kampman
 */
public class N3ParserFactory implements RDFParserFactory {

	/**
	 * Returns {@link RDFFormat#N3}.
	 */
	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.N3;
	}

	/**
	 * Returns a new instance of {@link N3Parser}.
	 */
	@Override
	public RDFParser getParser() {
		return new N3Parser();
	}
}
