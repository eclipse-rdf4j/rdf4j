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
package org.eclipse.rdf4j.rio;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Collection;

import org.eclipse.rdf4j.model.ValueFactory;

/**
 * An interface for RDF parsers. All implementing classes should define a public zero-argument constructor to allow them
 * to be created through reflection.
 */
public interface RDFParser {

	/**
	 * Gets the RDF format that this parser can parse.
	 */
	RDFFormat getRDFFormat();

	/**
	 * Sets the ValueFactory that the parser will use to create Value objects for the parsed RDF data.
	 *
	 * @param valueFactory The value factory that the parser should use.
	 * @return Either a copy of this parser, if it is immutable, or this object, to allow chaining of method calls.
	 */
	RDFParser setValueFactory(ValueFactory valueFactory);

	/**
	 * Sets the RDFHandler that will handle the parsed RDF data.
	 *
	 * @param handler The RDFHandler to handle the parsed data.
	 * @return Either a copy of this parser, if it is immutable, or this object, to allow chaining of method calls.
	 */
	RDFParser setRDFHandler(RDFHandler handler);

	/**
	 * Sets the ParseErrorListener that will be notified of any errors that this parser finds during parsing.
	 *
	 * @param el The ParseErrorListener that will be notified of errors or warnings.
	 * @return Either a copy of this parser, if it is immutable, or this object, to allow chaining of method calls.
	 */
	RDFParser setParseErrorListener(ParseErrorListener el);

	/**
	 * Sets the ParseLocationListener that will be notified of the parser's progress during the parse process.
	 *
	 * @param ll The ParseLocationListener that will be notified of the parser's progress.
	 * @return Either a copy of this parser, if it is immutable, or this object, to allow chaining of method calls.
	 */
	RDFParser setParseLocationListener(ParseLocationListener ll);

	/**
	 * Sets all supplied parser configuration options.
	 *
	 * @param config a parser configuration object.
	 * @return Either a copy of this parser, if it is immutable, or this object, to allow chaining of method calls.
	 */
	RDFParser setParserConfig(ParserConfig config);

	/**
	 * Retrieves the current parser configuration as a single object.
	 *
	 * @return a parser configuration object representing the current configuration of the parser.
	 */
	ParserConfig getParserConfig();

	/**
	 * @return A collection of {@link RioSetting}s that are supported by this RDFParser.
	 */
	Collection<RioSetting<?>> getSupportedSettings();

	/**
	 * Set a setting on the parser, and return this parser object to allow chaining.
	 *
	 * @param setting The setting to change.
	 * @param value   The value to change.
	 *
	 * @return Either a copy of this parser, if it is immutable, or this object, to allow chaining of method calls.
	 */
	<T> RDFParser set(RioSetting<T> setting, T value);

	/**
	 * Set whether the parser should preserve bnode identifiers specified in the source (default is <var>false</var>).
	 */
	void setPreserveBNodeIDs(boolean preserveBNodeIDs);

	/**
	 * Parses the data from the supplied InputStream.
	 *
	 * @param in The InputStream from which to read the data.
	 *
	 * @throws IOException         If an I/O error occurred while data was read from the InputStream.
	 * @throws RDFParseException   If the parser has found an unrecoverable parse error.
	 * @throws RDFHandlerException If the configured statement handler has encountered an unrecoverable error.
	 *
	 * @since 3.5.0
	 */
	default void parse(InputStream in) throws IOException, RDFParseException, RDFHandlerException {
		parse(in, null);
	}

	/**
	 * Parses the data from the supplied InputStream, using the supplied baseURI to resolve any relative URI references.
	 *
	 * @param in      The InputStream from which to read the data.
	 * @param baseURI The URI associated with the data in the InputStream. May be <code>null</code>. Parsers for syntax
	 *                formats that do not support relative URIs will ignore this argument.
	 *                <p>
	 *                Note that if the data contains an embedded base URI, that embedded base URI will overrule the
	 *                value supplied here (see <a href="https://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a> section 5.1
	 *                for details).
	 *
	 * @throws IOException         If an I/O error occurred while data was read from the InputStream.
	 * @throws RDFParseException   If the parser has found an unrecoverable parse error.
	 * @throws RDFHandlerException If the configured statement handler has encountered an unrecoverable error.
	 */
	void parse(InputStream in, String baseURI) throws IOException, RDFParseException, RDFHandlerException;

	/**
	 * Parses the data from the supplied Reader.
	 *
	 * @param reader The Reader from which to read the data.
	 *
	 * @throws IOException         If an I/O error occurred while data was read from the InputStream.
	 * @throws RDFParseException   If the parser has found an unrecoverable parse error.
	 * @throws RDFHandlerException If the configured statement handler has encountered an unrecoverable error.
	 *
	 * @since 3.5.0
	 */
	default void parse(Reader reader) throws IOException, RDFParseException, RDFHandlerException {
		parse(reader, null);
	}

	/**
	 * Parses the data from the supplied Reader, using the supplied baseURI to resolve any relative URI references.
	 *
	 * @param reader  The Reader from which to read the data.
	 * @param baseURI The URI associated with the data in the InputStream. May be <code>null</code>. Parsers for syntax
	 *                formats that do not support relative URIs will ignore this argument.
	 *                <p>
	 *                Note that if the data contains an embedded base URI, that embedded base URI will overrule the
	 *                value supplied here (see <a href="https://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a> section 5.1
	 *                for details).
	 *
	 * @throws IOException         If an I/O error occurred while data was read from the InputStream.
	 * @throws RDFParseException   If the parser has found an unrecoverable parse error.
	 * @throws RDFHandlerException If the configured statement handler has encountered an unrecoverable error.
	 */
	void parse(Reader reader, String baseURI) throws IOException, RDFParseException, RDFHandlerException;
}
