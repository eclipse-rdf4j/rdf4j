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
package org.eclipse.rdf4j.rio.nquads;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.helpers.NTriplesParserSettings;
import org.eclipse.rdf4j.rio.ntriples.NTriplesParser;

/**
 * RDF parser implementation for the {@link RDFFormat#NQUADS N-Quads} RDF format, extending the Rio N-Triples parser. A
 * specification of N-Quads can be found <a href="http://sw.deri.org/2008/07/n-quads/">here</a>. This parser is not
 * thread-safe, therefore its public methods are synchronized.
 *
 * @author Joshua Shinavier
 */
public class NQuadsParser extends NTriplesParser {

	protected Resource context;

	public NQuadsParser() {
		super();
	}

	public NQuadsParser(ValueFactory valueFactory) {
		super(valueFactory);
	}

	public RDFFormat getRDFFormat() {
		return RDFFormat.NQUADS;
	}

	protected void parseStatement() throws RDFParseException, RDFHandlerException {
		boolean ignoredAnError = false;
		try {
			skipWhitespace(false);
			if (!shouldParseLine()) {
				return;
			}
			parseSubject();

			skipWhitespace(true);

			parsePredicate();

			skipWhitespace(true);

			parseObject();

			skipWhitespace(true);

			parseContext();

			skipWhitespace(true);

			assertLineTerminates();
		} catch (RDFParseException e) {
			if (getParserConfig().isNonFatalError(NTriplesParserSettings.FAIL_ON_INVALID_LINES)) {
				reportError(e, NTriplesParserSettings.FAIL_ON_INVALID_LINES);
				ignoredAnError = true;
			} else {
				throw e;
			}
		}
		handleStatement(ignoredAnError);
	}

	protected void parseContext() {
		if (lineChars[currentIndex] == '<') {
			// context uri
			context = parseIRI();
		} else if (lineChars[currentIndex] == '_') {
			// context bnode
			context = parseNode();
		}
	}

	protected void handleStatement(boolean ignoredAnError) {
		if (rdfHandler != null && !ignoredAnError) {
			if (context == null) {
				rdfHandler.handleStatement(valueFactory.createStatement(subject, predicate, object));
			} else {
				rdfHandler.handleStatement(valueFactory.createStatement(subject, predicate, object, context));
			}
		}
		subject = null;
		predicate = null;
		object = null;
		context = null;
	}
}
