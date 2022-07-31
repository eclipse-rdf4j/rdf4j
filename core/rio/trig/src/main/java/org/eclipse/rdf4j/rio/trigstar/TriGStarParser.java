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

import java.io.IOException;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.trig.TriGParser;

/**
 * RDF parser for TriG-star (an extension of TriG that adds RDF-star support).
 *
 * @author Pavel Mihaylov
 */
public class TriGStarParser extends TriGParser {
	/**
	 * Creates a new TriGStarParser that will use a {@link SimpleValueFactory} to create RDF-star model objects.
	 */
	public TriGStarParser() {
		super();
	}

	/**
	 * Creates a new TriGStarParser that will use the supplied ValueFactory to create RDF-star model objects.
	 *
	 * @param valueFactory A ValueFactory.
	 */
	public TriGStarParser(ValueFactory valueFactory) {
		super(valueFactory);
	}

	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.TRIGSTAR;
	}

	@Override
	protected Value parseValue() throws IOException, RDFParseException, RDFHandlerException {
		if (peekIsTripleValue()) {
			return parseTripleValue();
		}

		return super.parseValue();
	}

	@Override
	protected void setContext(Resource context) {
		if (context != null && !(context instanceof IRI || context instanceof BNode)) {
			reportFatalError("Illegal context value: " + context);
		}

		super.setContext(context);
	}
}
