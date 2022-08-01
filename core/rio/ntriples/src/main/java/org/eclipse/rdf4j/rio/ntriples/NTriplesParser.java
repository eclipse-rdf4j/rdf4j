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
package org.eclipse.rdf4j.rio.ntriples;

import static org.eclipse.rdf4j.rio.helpers.NTriplesUtil.unescapeString;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.io.input.BOMInputStream;
import org.eclipse.rdf4j.common.text.ASCIIUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
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

	protected BufferedReader reader;
	protected char[] lineChars;
	protected int currentIndex;
	protected long lineNo;
	protected Resource subject;
	protected IRI predicate;
	protected Value object;

	/**
	 * Creates a new NTriplesParser that will use a {@link SimpleValueFactory} to create object for resources, bNodes
	 * and literals.
	 */
	public NTriplesParser() {
		super();
	}

	/**
	 * Creates a new NTriplesParser that will use the supplied <var>ValueFactory</var> to create RDF model objects.
	 *
	 * @param valueFactory A ValueFactory.
	 */
	public NTriplesParser(ValueFactory valueFactory) {
		super(valueFactory);
	}

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
			parse(new BufferedReader(new InputStreamReader(new BOMInputStream(in, false), StandardCharsets.UTF_8)),
					baseURI);
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

			if (reader instanceof BufferedReader) {
				this.reader = (BufferedReader) reader;
			} else {
				this.reader = new BufferedReader(reader);
			}
			lineNo = 0;

			reportLocation(lineNo, 1);

			while (readLine()) {
				parseStatement();
			}
		} finally {
			clear();
		}

		if (rdfHandler != null) {
			rdfHandler.endRDF();
		}
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

			assertLineTerminates();
		} catch (RDFParseException e) {
			if (!getParserConfig().get(NTriplesParserSettings.FAIL_ON_INVALID_LINES)
					|| getParserConfig().isNonFatalError(NTriplesParserSettings.FAIL_ON_INVALID_LINES)) {
				reportError(e, NTriplesParserSettings.FAIL_ON_INVALID_LINES);
				ignoredAnError = true;
			} else {
				throw e;
			}
		}
		handleStatement(ignoredAnError);
	}

	protected void skipWhitespace(boolean throwEOF) {
		while (currentIndex < lineChars.length && (lineChars[currentIndex] == ' ' || lineChars[currentIndex] == '\t')) {
			currentIndex++;
		}
		if (currentIndex >= lineChars.length && throwEOF) {
			throwEOFException();
		}
	}

	protected boolean shouldParseLine() {
		if (currentIndex < lineChars.length - 1) {
			if (lineChars[currentIndex] != '#') {
				return true;
			} else {
				if (rdfHandler != null) {
					rdfHandler.handleComment(
							new String(lineChars, currentIndex + 1, lineChars.length - currentIndex - 1));
				}
			}
		}
		return false;
	}

	protected void parseSubject() {
		if (lineChars[currentIndex] == '<') {
			subject = parseIRI();
		} else if (lineChars[currentIndex] == '_') {
			subject = parseNode();
		} else {
			throw new RDFParseException(
					"Expected '<' or '_', found: " + new String(Character.toChars(lineChars[currentIndex])), lineNo,
					lineChars[currentIndex]);
		}
	}

	protected void parsePredicate() {
		if (lineChars[currentIndex] == '<') {
			predicate = parseIRI();
		} else {
			throw new RDFParseException(
					"Expected '<', found: " + new String(Character.toChars(lineChars[currentIndex])), lineNo,
					lineChars[currentIndex]);
		}
	}

	protected void parseObject() {
		if (lineChars[currentIndex] == '<') {
			object = parseIRI();
		} else if (lineChars[currentIndex] == '_') {
			object = parseNode();
		} else if (lineChars[currentIndex] == '"') {
			parseLiteral();
		} else {
			throw new RDFParseException(
					"Expected '<' or '_', found: " + new String(Character.toChars(lineChars[currentIndex])), lineNo,
					lineChars[currentIndex]);
		}
	}

	/**
	 * Verifies that there is only whitespace or comments until the end of the line.
	 */
	protected void assertLineTerminates() throws RDFParseException {
		if (!NTriplesUtil.isDot(lineChars[currentIndex])) {
			if (lineChars[currentIndex] != '#') {
				reportFatalError("Content after '.' is not allowed");
			} else {
				return;
			}
		}
		if (lineChars.length - 1 > currentIndex) {
			currentIndex++;
			skipWhitespace(false);
			if (currentIndex >= lineChars.length) {
				return;
			}
			if (lineChars[currentIndex] != ' ' && lineChars[currentIndex] != '\t' && lineChars[currentIndex] != '#') {
				throw new RDFParseException("line must end with '.'", lineNo, currentIndex);
			}
		}
	}

	protected void handleStatement(boolean ignoredAnError) {
		if (rdfHandler != null && !ignoredAnError) {
			rdfHandler.handleStatement(valueFactory.createStatement(subject, predicate, object));
		}
		subject = null;
		predicate = null;
		object = null;
	}

	protected IRI parseIRI() {
		if (lineChars[currentIndex] != '<') {
			reportError("Supplied char should be a '<', is: " + new String(Character.toChars(lineChars[currentIndex])),
					NTriplesParserSettings.FAIL_ON_INVALID_LINES);
		}
		int startIndex = currentIndex + 1;
		moveToIRIEndIndex();
		IRI iri = createURI(new String(lineChars, startIndex, currentIndex - startIndex));
		currentIndex++;
		return iri;
	}

	protected Resource parseNode() {
		if (lineChars[currentIndex] != '_') {
			reportError("Supplied char should be a '_', is: " + new String(Character.toChars(lineChars[currentIndex])),
					NTriplesParserSettings.FAIL_ON_INVALID_LINES);
		}
		int startIndex = currentIndex + 2;
		moveToBNodeEndIndex();
		return createNode(new String(lineChars, startIndex, currentIndex - startIndex));
	}

	private void parseLiteral() {
		String label = parseLabel();
		incrementIndexOrThrowEOF();
		if (currentIndex < lineChars.length - 1 && lineChars[currentIndex] == '^') {
			parseLiteralWithDatatype(label);
		} else if (lineChars[currentIndex] == '@') {
			parseLangLiteral(label);
		} else {
			object = createLiteral(label, null, null, lineNo, lineChars[currentIndex]);
		}
	}

	private String parseLabel() {
		int startIndex = currentIndex;
		incrementIndexOrThrowEOF();
		while (lineChars[currentIndex] != '\"') {
			if (lineChars[currentIndex] == '\\') {
				currentIndex++;
			}
			incrementIndexOrThrowEOF();
		}
		try {
			return unescapeString(new String(lineChars, startIndex + 1, currentIndex - startIndex - 1));
		} catch (IllegalArgumentException e) {
			throw new RDFParseException("Illegal unicode escape sequence", lineNo, -1);
		}
	}

	private void parseLiteralWithDatatype(String label) {
		if (lineChars[currentIndex + 1] != '^') {
			reportError("Expected '^', found: " + new String(Character.toChars(lineChars[currentIndex + 1])),
					NTriplesParserSettings.FAIL_ON_INVALID_LINES);
		}
		currentIndex += 2;
		if (currentIndex >= lineChars.length || lineChars[currentIndex] != '<') {
			reportError("Expected '<', found: " + new String(Character.toChars(lineChars[currentIndex])),
					NTriplesParserSettings.FAIL_ON_INVALID_LINES);
		}
		object = createLiteral(label, null, parseIRI(), lineNo, lineChars[currentIndex]);
	}

	private void parseLangLiteral(String label) {
		incrementIndexOrThrowEOF();
		if (!ASCIIUtil.isLetter(lineChars[currentIndex])) {
			reportError("Expected a letter, found: " + new String(Character.toChars(lineChars[currentIndex])),
					NTriplesParserSettings.FAIL_ON_INVALID_LINES);
		}
		int startIndex = currentIndex;
		while (currentIndex < lineChars.length && (!NTriplesUtil.isDot(lineChars[currentIndex])
				&& lineChars[currentIndex] != '^'
				&& lineChars[currentIndex] != ' '
				&& lineChars[currentIndex] != '\t')) {
			currentIndex++;
		}
		if (currentIndex >= lineChars.length) {
			throwEOFException();
		}
		object = createLiteral(label, new String(lineChars, startIndex, currentIndex - startIndex), null,
				lineNo, lineChars[currentIndex]);
	}

	/**
	 * Moves the current line index position to the end of the IRI.
	 */
	private void moveToIRIEndIndex() throws RDFParseException {
		currentIndex++;
		while (currentIndex < lineChars.length && lineChars[currentIndex] != '>') {
			if (lineChars[currentIndex] == ' ') {
				reportError(
						"IRI included an unencoded space: " + new String(Character.toChars(lineChars[currentIndex])),
						BasicParserSettings.VERIFY_URI_SYNTAX);
			}
			if (lineChars[currentIndex] == '\\') {
				// This escapes the next character, which might be a '>'
				incrementIndexOrThrowEOF();
				if (lineChars[currentIndex] != 'u' && lineChars[currentIndex] != 'U') {
					reportError("IRI includes string escapes: '\\" + lineChars[currentIndex] + "'",
							BasicParserSettings.VERIFY_URI_SYNTAX);
				}
			}
			currentIndex++;
		}
		if (currentIndex >= lineChars.length) {
			throwEOFException();
		}
	}

	/**
	 * Moves the current line index position to the end of the BNode ID.
	 */
	private void moveToBNodeEndIndex() throws RDFParseException {
		incrementIndexOrThrowEOF();
		if (lineChars[currentIndex] != ':') {
			reportError("Expected ':', found: " + new String(Character.toChars(lineChars[currentIndex])),
					NTriplesParserSettings.FAIL_ON_INVALID_LINES);
		}
		currentIndex++;
		if (!ASCIIUtil.isLetterOrNumber(lineChars[currentIndex]) && !NTriplesUtil.isUnderscore(
				lineChars[currentIndex])) {
			reportError("Expected a letter or number or underscore, found: " + new String(
					Character.toChars(lineChars[currentIndex])),
					NTriplesParserSettings.FAIL_ON_INVALID_LINES);
		}
		while (currentIndex < lineChars.length && NTriplesUtil.isValidCharacterForBNodeLabel(lineChars[currentIndex])) {
			if (NTriplesUtil.isDot(lineChars[currentIndex])) {
				if (currentIndex + 1 >= lineChars.length || !NTriplesUtil.isValidCharacterForBNodeLabel(
						lineChars[currentIndex + 1])) {
					break;
				}
			}
			currentIndex++;
		}

		if (currentIndex == lineChars.length) {
			if (NTriplesUtil.isDot(lineChars[currentIndex - 1])) {
				currentIndex--;
			} else {
				throwEOFException();
			}
		}
	}

	/**
	 * Increments the current line index position and asserts EOF is not reached.
	 */
	private void incrementIndexOrThrowEOF() {
		currentIndex++;
		if (currentIndex >= lineChars.length) {
			throwEOFException();
		}
	}

	/**
	 * Attempts to read the next line from the buffered reader.
	 */
	private boolean readLine() throws IOException {
		String line = reader.readLine();
		if (line != null) {
			lineChars = line.toCharArray();
			lineNo++;
			currentIndex = 0;
			reportLocation(lineNo, 1);
			return true;
		}
		lineChars = null;
		currentIndex = -1;
		return false;
	}

	@Override
	protected IRI createURI(String uri) throws RDFParseException {
		try {
			uri = unescapeString(uri);
		} catch (IllegalArgumentException e) {
			reportError(e.getMessage(), NTriplesParserSettings.FAIL_ON_INVALID_LINES);
		}
		return super.createURI(uri);
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

	@Override
	protected void clear() {
		currentIndex = -1;
		lineChars = null;
		super.clear();
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
