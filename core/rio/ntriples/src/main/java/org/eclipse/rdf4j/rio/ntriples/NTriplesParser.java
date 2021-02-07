/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.ntriples;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.io.input.BOMInputStream;
import org.eclipse.rdf4j.common.text.ASCIIUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFParser;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.NTriplesParserSettings;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;

/**
 * RDF parser for N-Triples files. A specification of NTriples can be found in
 * <a href="http://www.w3.org/TR/rdf-testcases/#ntriples">this section</a> of the RDF Test Cases document. This parser
 * is not thread-safe, therefore its public methods are synchronized.
 *
 * @author Arjohn Kampman
 */
public class NTriplesParser extends AbstractRDFParser {

	protected PushbackReader reader;
	protected long lineNo;
	protected Resource subject;
	protected IRI predicate;
	protected Value object;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new NTriplesParser that will use a {@link SimpleValueFactory} to create object for resources, bNodes
	 * and literals.
	 */
	public NTriplesParser() {
		super();
	}

	/**
	 * Creates a new NTriplesParser that will use the supplied <tt>ValueFactory</tt> to create RDF model objects.
	 *
	 * @param valueFactory A ValueFactory.
	 */
	public NTriplesParser(ValueFactory valueFactory) {
		super(valueFactory);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.NTRIPLES;
	}

	@Override
	public synchronized void parse(InputStream in, String baseURI)
			throws IOException, RDFParseException, RDFHandlerException {
		if (in == null) {
			throw new IllegalArgumentException("Input stream can not be 'null'");
		}

		try {
			parse(new InputStreamReader(new BOMInputStream(in, false), StandardCharsets.UTF_8), baseURI);
		} catch (UnsupportedEncodingException e) {
			// Every platform should support the UTF-8 encoding...
			throw new RuntimeException(e);
		}
	}

	@Override
	public synchronized void parse(Reader reader, String baseURI)
			throws IOException, RDFParseException, RDFHandlerException {
		clear();

		try {
			if (reader == null) {
				throw new IllegalArgumentException("Reader can not be 'null'");
			}

			if (rdfHandler != null) {
				rdfHandler.startRDF();
			}

			// Allow 1 characters to be pushed back
			this.reader = new PushbackReader(reader);
			lineNo = 1;

			reportLocation(lineNo, 1);

			int c = readCodePoint();
			c = skipWhitespace(c);

			while (c != -1) {
				if (c == '#') {
					// Comment
					c = parseComment(c);
				} else if (c == '\r' || c == '\n') {
					// Empty line, ignore
					c = skipLine(c);
				} else {
					c = parseTriple(c);
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

	/**
	 * Reads characters from reader until it finds a character that is not a space or tab, and returns this last
	 * character code point. In case the end of the character stream has been reached, -1 is returned.
	 */
	protected int skipWhitespace(int c) throws IOException {
		while (c == ' ' || c == '\t') {
			c = readCodePoint();
		}

		return c;
	}

	/**
	 * Verifies that there is only whitespace or comments until the end of the line.
	 */
	protected int assertLineTerminates(int c) throws IOException, RDFParseException {
		c = readCodePoint();

		c = skipWhitespace(c);

		if (c == '#') {
			// c = skipToEndOfLine(c);
		} else {
			if (c != -1 && c != '\r' && c != '\n') {
				reportFatalError("Content after '.' is not allowed");
			}
		}

		return c;
	}

	/**
	 * Reads characters from reader until the first EOL has been read. The EOL character or -1 is returned.
	 */
	protected int skipToEndOfLine(int c) throws IOException {
		while (c != -1 && c != '\r' && c != '\n') {
			c = readCodePoint();
		}

		return c;
	}

	/**
	 * Reads characters from reader until the first EOL has been read. The first character after the EOL is returned. In
	 * case the end of the character stream has been reached, -1 is returned.
	 */
	protected int skipLine(int c, StringBuilder sb) throws IOException {
		while (c != -1 && c != '\r' && c != '\n') {
			c = readCodePoint();
			// make sure c is not EOF
			if (sb != null && c != -1) {
				sb.append(Character.toChars(c));
			}
		}
		// delete last appended char as it is the line break, unless `c` is EOF and then we did not append it
		if (sb != null && c != -1) {
			sb.deleteCharAt(sb.length() - 1);
		}

		// c is equal to -1, \r or \n. In case of a \r, we should
		// check whether it is followed by a \n.

		if (c == '\n') {
			c = readCodePoint();

			lineNo++;

			reportLocation(lineNo, 1);
		} else if (c == '\r') {
			c = readCodePoint();

			if (c == '\n') {
				c = readCodePoint();
			}

			lineNo++;

			reportLocation(lineNo, 1);
		}

		return c;
	}

	protected int skipLine(int c) throws IOException {
		return skipLine(c, null);
	}

	private int parseComment(int c) throws IOException {
		StringBuilder sb = new StringBuilder(100);
		int res = skipLine(c, sb);
		if (rdfHandler != null) {
			rdfHandler.handleComment(sb.toString());
		}
		return res;
	}

	private int parseTriple(int c) throws IOException, RDFParseException, RDFHandlerException {
		boolean ignoredAnError = false;
		try {
			c = parseSubject(c);
			c = skipWhitespace(c);
			c = parsePredicate(c);
			c = skipWhitespace(c);
			c = parseObject(c);
			c = skipWhitespace(c);

			if (c == -1) {
				throwEOFException();
			} else if (c != '.') {
				reportError("Expected '.', found: " + new String(Character.toChars(c)),
						NTriplesParserSettings.FAIL_ON_NTRIPLES_INVALID_LINES);
			}

			c = assertLineTerminates(c);
		} catch (RDFParseException rdfpe) {
			if (!getParserConfig().get(NTriplesParserSettings.FAIL_ON_NTRIPLES_INVALID_LINES)
					|| getParserConfig().isNonFatalError(NTriplesParserSettings.FAIL_ON_NTRIPLES_INVALID_LINES)) {
				reportError(rdfpe, NTriplesParserSettings.FAIL_ON_NTRIPLES_INVALID_LINES);
				ignoredAnError = true;
			} else {
				throw rdfpe;
			}
		}

		c = skipLine(c);

		if (!ignoredAnError) {
			Statement st = createStatement(subject, predicate, object);
			if (rdfHandler != null) {
				rdfHandler.handleStatement(st);
			}
		}

		subject = null;
		predicate = null;
		object = null;

		return c;
	}

	protected int parseSubject(int c) throws IOException, RDFParseException {
		StringBuilder sb = new StringBuilder(100);

		// subject is either an uriref (<foo://bar>) or a nodeID (_:node1)
		if (c == '<') {
			// subject is an uriref
			c = parseUriRef(c, sb);
			subject = createURI(sb.toString());
		} else if (c == '_') {
			// subject is a bNode
			c = parseNodeID(c, sb);
			subject = createNode(sb.toString());
		} else if (c == -1) {
			throwEOFException();
		} else {
			throw new RDFParseException("Expected '<' or '_', found: " + new String(Character.toChars(c)), lineNo, c);
		}

		return c;
	}

	protected int parsePredicate(int c) throws IOException, RDFParseException {
		StringBuilder sb = new StringBuilder(100);

		// predicate must be an uriref (<foo://bar>)
		if (c == '<') {
			// predicate is an uriref
			c = parseUriRef(c, sb);
			predicate = createURI(sb.toString());
		} else if (c == -1) {
			throwEOFException();
		} else {
			throw new RDFParseException("Expected '<', found: " + new String(Character.toChars(c)), lineNo, c);
		}

		return c;
	}

	protected int parseObject(int c) throws IOException, RDFParseException {
		StringBuilder sb = getBuffer();

		// object is either an uriref (<foo://bar>), a nodeID (_:node1) or a
		// literal ("foo"-en or "1"^^<xsd:integer>).
		if (c == '<') {
			// object is an uriref
			c = parseUriRef(c, sb);
			object = createURI(sb.toString());
		} else if (c == '_') {
			// object is a bNode
			c = parseNodeID(c, sb);
			object = createNode(sb.toString());
		} else if (c == '"') {
			// object is a literal
			StringBuilder lang = getLanguageTagBuffer();
			StringBuilder datatype = getDatatypeUriBuffer();
			c = parseLiteral(c, sb, lang, datatype);
			object = createLiteral(sb.toString(), lang.toString(), datatype.toString());
		} else if (c == -1) {
			throwEOFException();
		} else {
			throw new RDFParseException("Expected '<' or '_', found: " + new String(Character.toChars(c)), lineNo, c);
		}

		return c;
	}

	protected int parseUriRef(int c, StringBuilder uriRef) throws IOException, RDFParseException {
		if (c != '<') {
			reportError("Supplied char should be a '<', is: " + new String(Character.toChars(c)),
					NTriplesParserSettings.FAIL_ON_NTRIPLES_INVALID_LINES);
		}
		// Read up to the next '>' character
		c = readCodePoint();
		while (c != '>') {
			if (c == -1) {
				throwEOFException();
			}
			if (c == ' ') {
				reportError("IRI included an unencoded space: " + new String(Character.toChars(c)),
						BasicParserSettings.VERIFY_URI_SYNTAX);
			}
			uriRef.append(Character.toChars(c));

			if (c == '\\') {
				// This escapes the next character, which might be a '>'
				c = readCodePoint();
				if (c == -1) {
					throwEOFException();
				}
				if (c != 'u' && c != 'U') {
					reportError("IRI includes string escapes: '\\" + c + "'", BasicParserSettings.VERIFY_URI_SYNTAX);
				}
				uriRef.append(Character.toChars(c));
			}

			c = readCodePoint();
		}

		// c == '>', read next char
		c = readCodePoint();

		return c;
	}

	protected int parseNodeID(int c, StringBuilder name) throws IOException, RDFParseException {
		if (c != '_') {
			reportError("Supplied char should be a '_', is: " + new String(Character.toChars(c)),
					NTriplesParserSettings.FAIL_ON_NTRIPLES_INVALID_LINES);
		}

		c = readCodePoint();
		if (c == -1) {
			throwEOFException();
		} else if (c != ':') {
			reportError("Expected ':', found: " + new String(Character.toChars(c)),
					NTriplesParserSettings.FAIL_ON_NTRIPLES_INVALID_LINES);
		}

		c = readCodePoint();
		if (c == -1) {
			throwEOFException();
		} else if (!ASCIIUtil.isLetterOrNumber(c) && !NTriplesUtil.isUnderscore(c)) {
			reportError("Expected a letter or number or underscore, found: " + new String(Character.toChars(c)),
					NTriplesParserSettings.FAIL_ON_NTRIPLES_INVALID_LINES);
		}
		name.append(Character.toChars(c));

		// Read all following letter and numbers, they are part of the name
		c = readCodePoint();
		while (c != -1 && NTriplesUtil.isValidCharacterForBNodeLabel(c)) {
			if (NTriplesUtil.isDot(c) && !NTriplesUtil.isValidCharacterForBNodeLabel(peekCodePoint())) {
				break;
			}
			name.append(Character.toChars(c));
			c = readCodePoint();
		}

		return c;
	}

	/**
	 * Peeks at the next Unicode code point without advancing the reader, and returns its value.
	 *
	 * @return the next Unicode code point, or -1 if the end of the stream has been reached.
	 * @throws IOException
	 */
	protected int peekCodePoint() throws IOException {
		int result = readCodePoint();
		unread(result);
		return result;
	}

	/**
	 * Pushes back a single code point by copying it to the front of the buffer. After this method returns, a call to
	 * {@link #readCodePoint()} will return the same code point c again.
	 *
	 * @param codePoint a single Unicode code point.
	 * @throws IOException
	 */
	protected void unread(int codePoint) throws IOException {
		if (codePoint != -1) {
			if (Character.isSupplementaryCodePoint(codePoint)) {
				final char[] surrogatePair = Character.toChars(codePoint);
				reader.unread(surrogatePair);
			} else {
				reader.unread(codePoint);
			}
		}
	}

	private int parseLiteral(int c, StringBuilder value, StringBuilder lang, StringBuilder datatype)
			throws IOException, RDFParseException {
		if (c != '"') {
			reportError("Supplied char should be a '\"', is: " + c,
					NTriplesParserSettings.FAIL_ON_NTRIPLES_INVALID_LINES);
		}

		// Read up to the next '"' character
		c = readCodePoint();
		while (c != '"') {
			if (c == -1) {
				throwEOFException();
			}
			value.append(Character.toChars(c));

			if (c == '\\') {
				// This escapes the next character, which might be a double quote
				c = readCodePoint();
				if (c == -1) {
					throwEOFException();
				}
				value.append(Character.toChars(c));
			}

			c = readCodePoint();
		}

		// c == '"', read next char
		c = readCodePoint();

		if (c == '@') {
			// Read language
			c = readCodePoint();

			if (!ASCIIUtil.isLetter(c)) {
				reportError("Expected a letter, found: " + new String(Character.toChars(c)),
						NTriplesParserSettings.FAIL_ON_NTRIPLES_INVALID_LINES);
			}

			while (c != -1 && c != '.' && c != '^' && c != ' ' && c != '\t') {
				lang.append(Character.toChars(c));
				c = readCodePoint();
			}
		} else if (c == '^') {
			// Read datatype
			c = readCodePoint();

			// c should be another '^'
			if (c == -1) {
				throwEOFException();
			} else if (c != '^') {
				reportError("Expected '^', found: " + new String(Character.toChars(c)),
						NTriplesParserSettings.FAIL_ON_NTRIPLES_INVALID_LINES);
			}

			c = readCodePoint();

			// c should be a '<'
			if (c == -1) {
				throwEOFException();
			} else if (c != '<') {
				reportError("Expected '<', found: " + new String(Character.toChars(c)),
						NTriplesParserSettings.FAIL_ON_NTRIPLES_INVALID_LINES);
			}

			c = parseUriRef(c, datatype);
		}

		return c;
	}

	@Override
	protected IRI createURI(String uri) throws RDFParseException {
		try {
			uri = NTriplesUtil.unescapeString(uri);
		} catch (IllegalArgumentException e) {
			reportError(e.getMessage(), NTriplesParserSettings.FAIL_ON_NTRIPLES_INVALID_LINES);
		}

		return super.createURI(uri);
	}

	/**
	 * Reads the next Unicode code point.
	 *
	 * @return the next Unicode code point, or -1 if the end of the stream has been reached.
	 * @throws IOException
	 */
	protected int readCodePoint() throws IOException {
		int next = reader.read();
		if (Character.isHighSurrogate((char) next)) {
			next = Character.toCodePoint((char) next, (char) reader.read());
		}
		return next;
	}

	protected Literal createLiteral(String label, String lang, String datatype) throws RDFParseException {
		try {
			label = NTriplesUtil.unescapeString(label);
		} catch (IllegalArgumentException e) {
			throw new RDFParseException(e, lineNo, -1);
		}

		if (lang.length() == 0) {
			lang = null;
		}

		if (datatype.length() == 0) {
			datatype = null;
		}

		IRI dtURI = null;
		if (datatype != null) {
			dtURI = createURI(datatype);
		}

		return super.createLiteral(label, lang, dtURI, lineNo, -1);
	}

	/**
	 * Overrides {@link AbstractRDFParser#reportWarning(String)}, adding line number information to the error.
	 */
	@Override
	protected void reportWarning(String msg) {
		reportWarning(msg, lineNo, -1);
	}

	/**
	 * Overrides {@link AbstractRDFParser#reportError(String, RioSetting)}, adding line number information to the error.
	 */
	@Override
	protected void reportError(String msg, RioSetting<Boolean> setting) throws RDFParseException {
		reportError(msg, lineNo, -1, setting);
	}

	@Override
	protected void reportError(Exception e, RioSetting<Boolean> setting) throws RDFParseException {
		reportError(e, lineNo, -1, setting);
	}

	/**
	 * Overrides {@link AbstractRDFParser#reportFatalError(String)}, adding line number information to the error.
	 */
	@Override
	protected void reportFatalError(String msg) throws RDFParseException {
		reportFatalError(msg, lineNo, -1);
	}

	/**
	 * Overrides {@link AbstractRDFParser#reportFatalError(Exception)}, adding line number information to the error.
	 */
	@Override
	protected void reportFatalError(Exception e) throws RDFParseException {
		reportFatalError(e, lineNo, -1);
	}

	protected void throwEOFException() throws RDFParseException {
		throw new RDFParseException("Unexpected end of file");
	}

	/**
	 * Return a buffer of zero length and non-zero capacity. The same buffer is reused for each thing which is parsed.
	 * This reduces the heap churn substantially. However, you have to watch out for side-effects and convert the buffer
	 * to a {@link String} before the buffer is reused.
	 *
	 * @return a buffer of zero length and non-zero capacity.
	 */
	private StringBuilder getBuffer() {
		buffer.setLength(0);
		return buffer;
	}

	private final StringBuilder buffer = new StringBuilder(100);

	/**
	 * Return a buffer for the use of parsing literal language tags. The buffer is of zero length and non-zero capacity.
	 * The same buffer is reused for each tag which is parsed. This reduces the heap churn substantially. However, you
	 * have to watch out for side-effects and convert the buffer to a {@link String} before the buffer is reused.
	 *
	 * @return a buffer of zero length and non-zero capacity, for the use of parsing literal language tags.
	 */
	private StringBuilder getLanguageTagBuffer() {
		languageTagBuffer.setLength(0);
		return languageTagBuffer;
	}

	private final StringBuilder languageTagBuffer = new StringBuilder(8);

	/**
	 * Return a buffer for the use of parsing literal datatype URIs. The buffer is of zero length and non-zero capacity.
	 * The same buffer is reused for each datatype which is parsed. This reduces the heap churn substantially. However,
	 * you have to watch out for side-effects and convert the buffer to a {@link String} before the buffer is reused.
	 *
	 * @return a buffer of zero length and non-zero capacity, for the user of parsing literal datatype URIs.
	 */
	private StringBuilder getDatatypeUriBuffer() {
		datatypeUriBuffer.setLength(0);
		return datatypeUriBuffer;
	}

	private final StringBuilder datatypeUriBuffer = new StringBuilder(40);

	@Override
	protected void clear() {
		super.clear();
		// get rid of anything large left in the buffers.
		buffer.setLength(0);
		buffer.trimToSize();
		languageTagBuffer.setLength(0);
		languageTagBuffer.trimToSize();
		datatypeUriBuffer.setLength(0);
		datatypeUriBuffer.trimToSize();
	}

	/*
	 * N-Triples parser supports these settings.
	 */
	@Override
	public Collection<RioSetting<?>> getSupportedSettings() {
		Collection<RioSetting<?>> result = new HashSet<>(super.getSupportedSettings());

		result.add(NTriplesParserSettings.FAIL_ON_INVALID_LINES);

		return result;
	}

}
