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
package org.eclipse.rdf4j.rio.trig;

import java.io.IOException;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.turtle.TurtleParser;
import org.eclipse.rdf4j.rio.turtle.TurtleUtil;

/**
 * RDF parser for <a href="https://www.w3.org/TR/trig/">RDF-1.1 TriG</a> files. This parser is not thread-safe,
 * therefore its public methods are synchronized.
 *
 * @author Arjohn Kampman
 * @author Peter Ansell
 * @see TurtleParser
 */
public class TriGParser extends TurtleParser {

	/*-----------*
	 * Variables *
	 *-----------*/

	private Resource context;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new TriGParser that will use a {@link SimpleValueFactory} to create RDF model objects.
	 */
	public TriGParser() {
		super();
	}

	/**
	 * Creates a new TriGParser that will use the supplied ValueFactory to create RDF model objects.
	 *
	 * @param valueFactory A ValueFactory.
	 */
	public TriGParser(ValueFactory valueFactory) {
		super(valueFactory);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.TRIG;
	}

	@Override
	protected void parseStatement() throws IOException, RDFParseException, RDFHandlerException {
		StringBuilder sb = new StringBuilder(8);

		int c;
		// longest valid directive @prefix
		do {
			c = readCodePoint();
			if (c == -1 || TurtleUtil.isWhitespace(c)) {
				unread(c);
				break;
			}
			sb.append((char) c);
		} while (sb.length() < 8);

		String directive = sb.toString();

		if (directive.startsWith("@")) {
			parseDirective(directive);
			skipWSC();
			verifyCharacterOrFail(readCodePoint(), ".");
		} else if ((directive.length() >= 6 && directive.substring(0, 6).equalsIgnoreCase("prefix"))
				|| (directive.length() >= 4 && directive.substring(0, 4).equalsIgnoreCase("base"))) {
			parseDirective(directive);
			skipWSC();
			// SPARQL BASE and PREFIX lines do not end in .
		} else if (directive.length() >= 6 && directive.substring(0, 5).equalsIgnoreCase("GRAPH")
				&& directive.substring(5, 6).equals(":")) {
			// If there was a colon immediately after the graph keyword then
			// assume it was a pname and not the SPARQL GRAPH keyword
			unread(directive);
			parseGraph();
		} else if (directive.length() >= 5 && directive.substring(0, 5).equalsIgnoreCase("GRAPH")) {
			// Do not unread the directive if it was SPARQL GRAPH
			// Just continue with TriG parsing at this point
			skipWSC();

			parseGraph();
			if (getContext() == null) {
				reportFatalError("Missing GRAPH label or subject");
			}
		} else {
			unread(directive);
			parseGraph();
		}
	}

	protected void parseGraph() throws IOException, RDFParseException, RDFHandlerException {
		int c = readCodePoint();
		int c2 = peekCodePoint();
		Resource contextOrSubject = null;
		boolean foundContextOrSubject = false;
		if (c == '[') {
			skipWSC();
			c2 = readCodePoint();
			if (c2 == ']') {
				contextOrSubject = createNode();
				foundContextOrSubject = true;
				skipWSC();
			} else {
				unread(c2);
				unread(c);
			}
			c = readCodePoint();
		} else if (c == '<' || TurtleUtil.isPrefixStartChar(c) || (c == ':' && c2 != '-') || (c == '_' && c2 == ':')) {
			unread(c);

			Value value = parseValue();

			if (value instanceof Resource) {
				contextOrSubject = (Resource) value;
				foundContextOrSubject = true;
			} else {
				// NOTE: If a user parses Turtle using TriG, then the following
				// could actually be "Illegal subject name", but it should still
				// hold
				reportFatalError("Illegal graph name: " + value);
			}

			skipWSC();
			c = readCodePoint();
		} else {
			setContext(null);
		}

		if (c == '{') {
			setContext(contextOrSubject);

			c = skipWSC();

			if (c != '}') {
				parseTriples();

				c = skipWSC();

				while (c == '.') {
					readCodePoint();

					c = skipWSC();

					if (c == '}') {
						break;
					}

					parseTriples();

					c = skipWSC();
				}

				verifyCharacterOrFail(c, "}");
			}
		} else {
			setContext(null);

			// Did not turn out to be a graph, so assign it to subject instead
			// and
			// parse from here to triples
			if (foundContextOrSubject) {
				subject = contextOrSubject;
				unread(c);
				parsePredicateObjectList();
			}
			// Or if we didn't recognise anything, just parse as Turtle
			else {
				unread(c);
				parseTriples();
			}
		}

		readCodePoint();
	}

	@Override
	protected void parseTriples() throws IOException, RDFParseException, RDFHandlerException {
		int c = peekCodePoint();

		// If the first character is an open bracket we need to decide which of
		// the two parsing methods for blank nodes to use
		if (c == '[') {
			c = readCodePoint();
			skipWSC();
			c = peekCodePoint();
			if (c == ']') {
				c = readCodePoint();
				subject = createNode();
				skipWSC();
				parsePredicateObjectList();
			} else {
				unread('[');
				subject = parseImplicitBlank();
			}
			skipWSC();
			c = peekCodePoint();

			// if this is not the end of the statement, recurse into the list of
			// predicate and objects, using the subject parsed above as the
			// subject
			// of the statement.
			if (c != '.' && c != '}') {
				parsePredicateObjectList();
			}
		} else {
			parseSubject();
			skipWSC();
			parsePredicateObjectList();
		}

		subject = null;
		predicate = null;
		object = null;
	}

	@Override
	protected void reportStatement(Resource subj, IRI pred, Value obj) throws RDFParseException, RDFHandlerException {
		Statement st = createStatement(subj, pred, obj, getContext());
		if (rdfHandler != null) {
			rdfHandler.handleStatement(st);
		}
	}

	protected void setContext(Resource context) {
		this.context = context;
	}

	protected Resource getContext() {
		return context;
	}
}
