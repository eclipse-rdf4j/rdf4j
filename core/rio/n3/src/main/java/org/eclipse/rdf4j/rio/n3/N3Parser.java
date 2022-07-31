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
package org.eclipse.rdf4j.rio.n3;

import java.io.IOException;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.turtle.TurtleParser;
import org.eclipse.rdf4j.rio.turtle.TurtleUtil;

public class N3Parser extends TurtleParser {

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new N3Parser that will use a {@link SimpleValueFactory} to create RDF model objects.
	 */
	public N3Parser() {
		super();
	}

	/**
	 * Creates a new N3Parser that will use the supplied ValueFactory to create RDF model objects.
	 *
	 * @param valueFactory A ValueFactory.
	 */
	public N3Parser(ValueFactory valueFactory) {
		super(valueFactory);
	}

	@Override
	protected IRI parsePredicate() throws IOException, RDFParseException, RDFHandlerException {
		// Check if the short-cut 'a' or '=' is used
		int c1 = readCodePoint();

		if (c1 == 'a') {
			int c2 = readCodePoint();

			if (TurtleUtil.isWhitespace(c2)) {
				// Short-cut is used, return the rdf:type URI
				return RDF.TYPE;
			}

			// Short-cut is not used, unread all characters
			unread(c2);
		}
		if (c1 == '=') {
			int c2 = readCodePoint();

			if (TurtleUtil.isWhitespace(c2)) {
				// Short-cut is used, return the owl:sameAs URI
				return OWL.SAMEAS;
			}

			// Short-cut is not used, unread all characters
			unread(c2);
		}
		unread(c1);

		// Predicate is a normal resource
		Value predicate = parseValue();
		if (predicate instanceof IRI) {
			return (IRI) predicate;
		} else {
			reportFatalError("Illegal predicate value: " + predicate);
			return null;
		}
	}

}
