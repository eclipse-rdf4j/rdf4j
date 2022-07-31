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
package org.eclipse.rdf4j.rio.trigstar;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFParserFactory;

/**
 * An {@link RDFParserFactory} for TriG-star parsers.
 *
 * @author Pavel Mihaylov
 */
public class TriGStarParserFactory implements RDFParserFactory {
	/**
	 * Returns {@link RDFFormat#TRIGSTAR}.
	 */
	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.TRIGSTAR;
	}

	/**
	 * Returns a new instance of {@link TriGStarParser}.
	 */
	@Override
	public RDFParser getParser() {
		return new TriGStarParser();
	}
}
