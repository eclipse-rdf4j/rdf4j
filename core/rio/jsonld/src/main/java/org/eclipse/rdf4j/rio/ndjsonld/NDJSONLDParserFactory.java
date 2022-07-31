/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.ndjsonld;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFParserFactory;

/**
 * An {@link RDFParserFactory} that creates instances of {@link NDJSONLDParser}.
 */
public class NDJSONLDParserFactory implements RDFParserFactory {

	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.NDJSONLD;
	}

	@Override
	public RDFParser getParser() {
		return new NDJSONLDParser();
	}

}
