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
package org.eclipse.rdf4j.query.parser.sparql;

import java.io.IOException;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.trigstar.TriGStarParser;
import org.eclipse.rdf4j.rio.turtle.TurtleUtil;

/**
 * An extension of {@link TriGStarParser} that processes data in the format specified in the SPARQL 1.1 grammar for Quad
 * data (assuming no variables, as is the case for INSERT DATA and DELETE DATA operations). This format is almost
 * completely compatible with TriG, except for three differences:
 * <ul>
 * <li>it introduces the 'GRAPH' keyword in front of each named graph identifier
 * <li>it does not allow the occurrence of blank nodes.
 * <li>it does not require curly braces around the default graph.
 * <li>it adds support for RDF-star triples (from TriG-star).</li>
 * </ul>
 *
 * @author Jeen Broekstra
 * @see <a href="http://www.w3.org/TR/sparql11-query/#rInsertData">SPARQL 1.1 Grammar production for INSERT DATA</a>
 * @see <a href="http://www.w3.org/TR/sparql11-query/#rDeleteData">SPARQL 1.1 Grammar production for DELETE DATA</a>
 */
public class SPARQLUpdateDataBlockParser extends TriGStarParser {

	private boolean allowBlankNodes = true;
	private int lineNumberOffset;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new parser that will use a {@link SimpleValueFactory} to create RDF model objects.
	 */
	public SPARQLUpdateDataBlockParser() {
		super();
	}

	/**
	 * Creates a new parser that will use the supplied ValueFactory to create RDF model objects.
	 *
	 * @param valueFactory A ValueFactory.
	 */
	public SPARQLUpdateDataBlockParser(ValueFactory valueFactory) {
		super(valueFactory);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public RDFFormat getRDFFormat() {
		// TODO for now, we do not implement this as a fully compatible Rio
		// parser, and we're not introducing a new RDFFormat constant.
		return null;
	}

	@Override
	protected void parseGraph() throws RDFParseException, RDFHandlerException, IOException {
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

		c = peekCodePoint();
		if (c == '.' || c == '}') {
			readCodePoint();
		}
		skipOptionalPeriod();
	}

	@Override
	protected Resource parseImplicitBlank() throws IOException, RDFParseException, RDFHandlerException {
		if (isAllowBlankNodes()) {
			return super.parseImplicitBlank();
		} else {
			throw new RDFParseException("blank nodes not allowed in data block");
		}
	}

	@Override
	protected Resource parseNodeID() throws IOException, RDFParseException {
		if (isAllowBlankNodes()) {
			return super.parseNodeID();
		} else {
			throw new RDFParseException("blank nodes not allowed in data block");
		}
	}

	/**
	 * @return Returns the allowBlankNodes.
	 */
	public boolean isAllowBlankNodes() {
		return allowBlankNodes;
	}

	/**
	 * @param allowBlankNodes The allowBlankNodes to set.
	 */
	public void setAllowBlankNodes(boolean allowBlankNodes) {
		this.allowBlankNodes = allowBlankNodes;
	}

	@Override
	protected int getLineNumber() {
		return super.getLineNumber() - this.lineNumberOffset;
	}

	private void skipOptionalPeriod() throws RDFHandlerException, IOException {
		skipWSC();
		int c = peekCodePoint();
		if (c == '.') {
			readCodePoint();
		}
	}

	/**
	 * @param lineNumberOffset
	 */
	public void setLineNumberOffset(int lineNumberOffset) {
		this.lineNumberOffset = lineNumberOffset;
	}
}
