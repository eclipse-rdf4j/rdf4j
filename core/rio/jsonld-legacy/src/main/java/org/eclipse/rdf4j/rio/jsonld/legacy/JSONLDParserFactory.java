/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/
package org.eclipse.rdf4j.rio.jsonld.legacy;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFParserFactory;

/**
 * An {@link RDFParserFactory} that creates instances of {@link JSONLDParser}.
 *
 * @author Peter Ansell
 */
public class JSONLDParserFactory implements RDFParserFactory {

	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.JSONLD;
	}

	@Override
	public RDFParser getParser() {
		return new JSONLDParser();
	}

}
