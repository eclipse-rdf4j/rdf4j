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
package org.eclipse.rdf4j.rio.hdt;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParserFactory;

/**
 * An {@link RDFParserFactory} for HDT parsers.
 *
 * @author Bart Hanssens
 */
public class HDTParserFactory implements RDFParserFactory {

	/**
	 * Returns {@link RDFFormat#HDT}.
	 */
	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.HDT;
	}

	/**
	 * Returns a new instance of {@link HDTParser}.
	 */
	@Override
	public HDTParser getParser() {
		return new HDTParser();
	}
}
