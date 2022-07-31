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
package org.eclipse.rdf4j.rio.binary;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFParserFactory;

/**
 * An {@link RDFParserFactory} for Binary RDF parsers.
 *
 * @author Arjohn Kampman
 */
public class BinaryRDFParserFactory implements RDFParserFactory {

	/**
	 * Returns {@link RDFFormat#BINARY}.
	 */
	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.BINARY;
	}

	/**
	 * Returns a new instance of {@link BinaryRDFParser}.
	 */
	@Override
	public RDFParser getParser() {
		return new BinaryRDFParser();
	}
}
