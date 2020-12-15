/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.nquads;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.input.BOMInputStream;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
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

	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.NQUADS;
	}

	@Override
	public synchronized void parse(final InputStream inputStream, final String baseURI)
			throws IOException, RDFParseException, RDFHandlerException {
		if (inputStream == null) {
			throw new IllegalArgumentException("Input stream can not be 'null'");
		}

		try {
			parse(new InputStreamReader(new BOMInputStream(inputStream, false), StandardCharsets.UTF_8), baseURI);
		} catch (UnsupportedEncodingException e) {
			// Every platform should support the UTF-8 encoding...
			throw new RuntimeException(e);
		}
	}

	@Override
	public synchronized void parse(final Reader reader, final String baseURI)
			throws IOException, RDFParseException, RDFHandlerException {
		clear();

		try {
			if (reader == null) {
				throw new IllegalArgumentException("Reader can not be 'null'");
			}

			if (rdfHandler != null) {
				rdfHandler.startRDF();
			}

			this.reader = new PushbackReader(reader);
			lineNo = 1;

			reportLocation(lineNo, 1);

			int c = readCodePoint();
			c = skipWhitespace(c);

			while (c != -1) {
				if (c == '#') {
					// Comment, ignore
					c = skipLine(c);
				} else if (c == '\r' || c == '\n') {
					// Empty line, ignore
					c = skipLine(c);
				} else {
					c = parseQuad(c);
				}

				c = skipWhitespace(c);
			}
		} finally {
			clear();
		}

		if (rdfHandler != null) {
			rdfHandler.endRDF();
		}
	}

	private int parseQuad(int c) throws IOException, RDFParseException, RDFHandlerException {

		boolean ignoredAnError = false;
		try {
			c = parseSubject(c);

			c = skipWhitespace(c);

			c = parsePredicate(c);

			c = skipWhitespace(c);

			c = parseObject(c);

			c = skipWhitespace(c);

			// Context is not required
			if (c != '.') {
				c = parseContext(c);
				c = skipWhitespace(c);
			}
			if (c == -1) {
				throwEOFException();
			} else if (c != '.') {
				reportFatalError("Expected '.', found: " + new String(Character.toChars(c)));
			}

			c = assertLineTerminates(c);
		} catch (RDFParseException rdfpe) {
			if (getParserConfig().isNonFatalError(NTriplesParserSettings.FAIL_ON_NTRIPLES_INVALID_LINES)) {
				reportError(rdfpe, NTriplesParserSettings.FAIL_ON_NTRIPLES_INVALID_LINES);
				ignoredAnError = true;
			} else {
				throw rdfpe;
			}
		}

		c = skipLine(c);

		if (!ignoredAnError) {
			Statement st = createStatement(subject, predicate, object, context);
			if (rdfHandler != null) {
				rdfHandler.handleStatement(st);
			}
		}

		subject = null;
		predicate = null;
		object = null;
		context = null;

		return c;
	}

	protected int parseContext(int c) throws IOException, RDFParseException {
		StringBuilder sb = new StringBuilder(100);

		// subject is either an uriref (<foo://bar>) or a nodeID (_:node1)
		if (c == '<') {
			// subject is an uriref
			c = parseUriRef(c, sb);
			context = createURI(sb.toString());
		} else if (c == '_') {
			// subject is a bNode
			c = parseNodeID(c, sb);
			context = createNode(sb.toString());
		} else if (c == -1) {
			throwEOFException();
		} else {
			reportFatalError("Expected '<' or '_', found: " + new String(Character.toChars(c)));
		}

		return c;
	}

}
