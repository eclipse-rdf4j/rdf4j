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
package org.eclipse.rdf4j.rio.turtle;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.input.BOMInputStream;
import org.eclipse.rdf4j.common.text.ASCIIUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFParser;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.TurtleParserSettings;

/**
 * RDF parser for <a href="https://www.w3.org/TR/turtle/">RDF-1.1 Turtle</a> files. This parser is not thread-safe,
 * therefore its public methods are synchronized.
 * <p>
 * <li>Normalization of integer, floating point and boolean values is dependent on the specified datatype handling.
 * According to the specification, integers and booleans should be normalized, but floats don't.</li>
 * <li>Comments can be used anywhere in the document, and extend to the end of the line. The Turtle grammar doesn't
 * allow comments to be used inside triple constructs that extend over multiple lines, but the author's own parser
 * deviates from this too.</li>
 * </ul>
 *
 * @author Arjohn Kampman
 * @author Peter Ansell
 */
public class TurtleParser extends AbstractRDFParser {

	/*-----------*
	 * Variables *
	 *-----------*/

	private PushbackReader reader;

	protected Resource subject;

	protected IRI predicate;

	protected Value object;

	private int lineNumber = 1;

	private final StringBuilder parsingBuilder = new StringBuilder();

	/**
	 * The most recently read complete statement.
	 */
	private Statement previousStatement;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new TurtleParser that will use a {@link SimpleValueFactory} to create RDF model objects.
	 */
	public TurtleParser() {
		super();
	}

	/**
	 * Creates a new TurtleParser that will use the supplied ValueFactory to create RDF model objects.
	 *
	 * @param valueFactory A ValueFactory.
	 */
	public TurtleParser(ValueFactory valueFactory) {
		super(valueFactory);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.TURTLE;
	}

	@Override
	public Collection<RioSetting<?>> getSupportedSettings() {
		Set<RioSetting<?>> result = new HashSet<>(super.getSupportedSettings());
		result.add(TurtleParserSettings.CASE_INSENSITIVE_DIRECTIVES);
		result.add(TurtleParserSettings.ACCEPT_TURTLESTAR);
		return result;
	}

	@Override
	public synchronized void parse(InputStream in, String baseURI)
			throws IOException, RDFParseException, RDFHandlerException {
		if (in == null) {
			throw new IllegalArgumentException("Input stream must not be 'null'");
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
				throw new IllegalArgumentException("Reader must not be 'null'");
			}

			if (rdfHandler != null) {
				rdfHandler.startRDF();
			}

			// Start counting lines at 1:
			lineNumber = 1;

			// Allow at most 8 characters to be pushed back:
			this.reader = new PushbackReader(reader, 10);

			if (baseURI != null) {
				// Store normalized base URI
				setBaseURI(baseURI);
			}

			reportLocation();

			int c = skipWSC();

			while (c != -1) {
				parseStatement();
				c = skipWSC();
			}
		} finally {
			clear();
		}

		if (rdfHandler != null) {
			rdfHandler.endRDF();
		}
	}

	protected void parseStatement() throws IOException, RDFParseException, RDFHandlerException {

		StringBuilder sb = new StringBuilder(8);

		int codePoint;
		// longest valid directive @prefix
		do {
			codePoint = readCodePoint();
			if (codePoint == -1 || TurtleUtil.isWhitespace(codePoint)) {
				unread(codePoint);
				break;
			}
			appendCodepoint(sb, codePoint);
		} while (sb.length() < 8);

		String directive = sb.toString();

		if (directive.startsWith("@") || directive.equalsIgnoreCase("prefix") || directive.equalsIgnoreCase("base")) {
			parseDirective(directive);
			skipWSC();
			// SPARQL BASE and PREFIX lines do not end in .
			if (directive.startsWith("@")) {
				verifyCharacterOrFail(readCodePoint(), ".");
			}
		} else {
			unread(directive);
			parseTriples();
			skipWSC();
			verifyCharacterOrFail(readCodePoint(), ".");
		}
	}

	protected void parseDirective(String directive) throws IOException, RDFParseException, RDFHandlerException {
		if (directive.length() >= 7 && directive.substring(0, 7).equals("@prefix")) {
			if (directive.length() > 7) {
				unread(directive.substring(7));
			}
			parsePrefixID();
		} else if (directive.length() >= 5 && directive.substring(0, 5).equals("@base")) {
			if (directive.length() > 5) {
				unread(directive.substring(5));
			}
			parseBase();
		} else if (directive.length() >= 6 && directive.substring(0, 6).equalsIgnoreCase("prefix")) {
			// SPARQL doesn't require whitespace after directive, so must unread
			// if
			// we found part of the prefixID
			if (directive.length() > 6) {
				unread(directive.substring(6));
			}
			parsePrefixID();
		} else if ((directive.length() >= 4 && directive.substring(0, 4).equalsIgnoreCase("base"))) {
			if (directive.length() > 4) {
				unread(directive.substring(4));
			}
			parseBase();
		} else if (directive.length() >= 7 && directive.substring(0, 7).equalsIgnoreCase("@prefix")) {
			if (!this.getParserConfig().get(TurtleParserSettings.CASE_INSENSITIVE_DIRECTIVES)) {
				reportFatalError("Cannot strictly support case-insensitive @prefix directive in compliance mode.");
			}
			if (directive.length() > 7) {
				unread(directive.substring(7));
			}
			parsePrefixID();
		} else if (directive.length() >= 5 && directive.substring(0, 5).equalsIgnoreCase("@base")) {
			if (!this.getParserConfig().get(TurtleParserSettings.CASE_INSENSITIVE_DIRECTIVES)) {
				reportFatalError("Cannot strictly support case-insensitive @base directive in compliance mode.");
			}
			if (directive.length() > 5) {
				unread(directive.substring(5));
			}
			parseBase();
		} else if (directive.length() == 0) {
			reportFatalError("Directive name is missing, expected @prefix or @base");
		} else {
			reportFatalError("Unknown directive \"" + directive + "\"");
		}
	}

	protected void parsePrefixID() throws IOException, RDFParseException, RDFHandlerException {
		skipWSC();

		// Read prefix ID (e.g. "rdf:" or ":")
		StringBuilder prefixID = new StringBuilder(8);

		while (true) {
			int c = readCodePoint();

			if (c == ':') {
				unread(c);
				break;
			} else if (TurtleUtil.isWhitespace(c)) {
				break;
			} else if (c == -1) {
				throwEOFException();
			}

			appendCodepoint(prefixID, c);
		}

		skipWSC();

		verifyCharacterOrFail(readCodePoint(), ":");

		skipWSC();

		// Read the namespace URI
		String namespaceStr = parseURI().toString();

		String prefixStr = prefixID.toString();

		// Store and report this namespace mapping
		setNamespace(prefixStr, namespaceStr);

		if (rdfHandler != null) {
			rdfHandler.handleNamespace(prefixStr, namespaceStr);
		}
	}

	protected void parseBase() throws IOException, RDFParseException, RDFHandlerException {
		skipWSC();

		IRI baseURI = parseURI();

		setBaseURI(baseURI.toString());
	}

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
			if (c != '.') {
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

	protected void parsePredicateObjectList() throws IOException, RDFParseException, RDFHandlerException {
		predicate = parsePredicate();

		skipWSC();

		parseObjectList();

		while (skipWSC() == ';') {
			readCodePoint();

			int c = skipWSC();

			if (c == '.' || // end of triple
					c == ']' || c == '}') // end of predicateObjectList inside
			// blank
			// node
			{
				break;
			} else if (c == ';') {
				// empty predicateObjectList, skip to next
				continue;
			}

			predicate = parsePredicate();

			skipWSC();

			parseObjectList();
		}
	}

	protected void parseObjectList() throws IOException, RDFParseException, RDFHandlerException {
		parseObject();

		if (skipWSC() == '{') {
			parseAnnotation();
		}
		while (skipWSC() == ',') {
			readCodePoint();
			skipWSC();
			parseObject();
			if (skipWSC() == '{') {
				parseAnnotation();
			}
		}
	}

	protected void parseSubject() throws IOException, RDFParseException, RDFHandlerException {
		int c = peekCodePoint();

		if (c == '(') {
			subject = parseCollection();
		} else if (c == '[') {
			subject = parseImplicitBlank();
		} else {
			Value value = parseValue();

			if (value instanceof Resource) {
				subject = (Resource) value;
			} else if (value != null) {
				reportFatalError("Illegal subject value: " + value);
			}
		}
	}

	protected IRI parsePredicate() throws IOException, RDFParseException, RDFHandlerException {
		// Check if the short-cut 'a' is used
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

	/**
	 * Parse an object
	 *
	 * @throws IOException
	 * @throws RDFParseException
	 * @throws RDFHandlerException
	 */
	protected void parseObject() throws IOException, RDFParseException, RDFHandlerException {
		int c = peekCodePoint();

		switch (c) {
		case '(':
			object = parseCollection();
			break;
		case '[':
			object = parseImplicitBlank();
			break;
		default:
			object = parseValue();
			reportStatement(subject, predicate, object);
			break;
		}
	}

	/**
	 * Parses a collection, e.g. <var>( item1 item2 item3 )</var>.
	 */
	protected Resource parseCollection() throws IOException, RDFParseException, RDFHandlerException {
		verifyCharacterOrFail(readCodePoint(), "(");

		int c = skipWSC();

		if (c == ')') {
			// Empty list
			readCodePoint();
			if (subject != null) {
				reportStatement(subject, predicate, RDF.NIL);
			}
			return RDF.NIL;
		} else {
			Resource listRoot = createNode();
			if (subject != null) {
				reportStatement(subject, predicate, listRoot);
			}

			// Remember current subject and predicate
			Resource oldSubject = subject;
			IRI oldPredicate = predicate;

			// generated bNode becomes subject, predicate becomes rdf:first
			subject = listRoot;
			predicate = RDF.FIRST;

			parseObject();

			Resource bNode = listRoot;

			while (skipWSC() != ')') {
				// Create another list node and link it to the previous
				Resource newNode = createNode();
				reportStatement(bNode, RDF.REST, newNode);

				// New node becomes the current
				subject = bNode = newNode;

				parseObject();
			}

			// Skip ')'
			readCodePoint();

			// Close the list
			reportStatement(bNode, RDF.REST, RDF.NIL);

			// Restore previous subject and predicate
			subject = oldSubject;
			predicate = oldPredicate;

			return listRoot;
		}
	}

	/**
	 * Parses an implicit blank node. This method parses the token <var>[]</var> and predicateObjectLists that are
	 * surrounded by square brackets.
	 */
	protected Resource parseImplicitBlank() throws IOException, RDFParseException, RDFHandlerException {
		verifyCharacterOrFail(readCodePoint(), "[");

		Resource bNode = createNode();
		if (subject != null) {
			reportStatement(subject, predicate, bNode);
		}

		skipWSC();
		int c = readCodePoint();
		if (c != ']') {
			unread(c);

			// Remember current subject and predicate
			Resource oldSubject = subject;
			IRI oldPredicate = predicate;

			// generated bNode becomes subject
			subject = bNode;

			// Enter recursion with nested predicate-object list
			skipWSC();

			parsePredicateObjectList();

			skipWSC();

			// Read closing bracket
			verifyCharacterOrFail(readCodePoint(), "]");

			// Restore previous subject and predicate
			subject = oldSubject;
			predicate = oldPredicate;
		}

		return bNode;
	}

	/**
	 * Parses an RDF value. This method parses uriref, qname, node ID, quoted literal, integer, double and boolean.
	 */
	protected Value parseValue() throws IOException, RDFParseException, RDFHandlerException {
		if (getParserConfig().get(TurtleParserSettings.ACCEPT_TURTLESTAR) && peekIsTripleValue()) {
			return parseTripleValue();
		}

		int c = peekCodePoint();

		if (c == '<') {
			// uriref, e.g. <foo://bar>
			return parseURI();
		} else if (c == ':' || TurtleUtil.isPrefixStartChar(c)) {
			// qname or boolean
			return parseQNameOrBoolean();
		} else if (c == '_') {
			// node ID, e.g. _:n1
			return parseNodeID();
		} else if (c == '"' || c == '\'') {
			// quoted literal, e.g. "foo" or """foo""" or 'foo' or '''foo'''
			return parseQuotedLiteral();
		} else if (ASCIIUtil.isNumber(c) || c == '.' || c == '+' || c == '-') {
			// integer or double, e.g. 123 or 1.2e3
			return parseNumber();
		} else if (c == -1) {
			throwEOFException();
			return null;
		} else {
			reportFatalError("Expected an RDF value here, found '" + new String(Character.toChars(c)) + "'");
			return null;
		}
	}

	/**
	 * Parses a quoted string, optionally followed by a language tag or datatype.
	 */
	protected Literal parseQuotedLiteral() throws IOException, RDFParseException, RDFHandlerException {
		String label = parseQuotedString();

		// Check for presence of a language tag or datatype
		int c = peekCodePoint();

		if (c == '@') {
			readCodePoint();

			// Read language
			StringBuilder lang = getBuilder();

			c = readCodePoint();
			if (c == -1) {
				throwEOFException();
			}

			boolean verifyLanguageTag = getParserConfig().get(BasicParserSettings.VERIFY_LANGUAGE_TAGS);
			if (verifyLanguageTag && !TurtleUtil.isLanguageStartChar(c)) {
				reportError("Expected a letter, found '" + new String(Character.toChars(c)) + "'",
						BasicParserSettings.VERIFY_LANGUAGE_TAGS);
			}

			appendCodepoint(lang, c);

			c = readCodePoint();
			while (!TurtleUtil.isWhitespace(c)) {
				// SES-1887 : Flexibility introduced for SES-1985 and SES-1821
				// needs
				// to be counterbalanced against legitimate situations where
				// Turtle
				// language tags do not need whitespace following the language
				// tag
				if (c == '.' || c == ';' || c == ',' || c == ')' || c == ']' || c == '>' || c == -1) {
					break;
				}
				if (verifyLanguageTag && !TurtleUtil.isLanguageChar(c)) {
					reportError("Illegal language tag char: '" + new String(Character.toChars(c)) + "'",
							BasicParserSettings.VERIFY_LANGUAGE_TAGS);
				}
				appendCodepoint(lang, c);
				c = readCodePoint();
			}

			unread(c);

			return createLiteral(label, lang.toString(), null, getLineNumber(), -1);
		} else if (c == '^') {
			readCodePoint();

			// next character should be another '^'
			verifyCharacterOrFail(readCodePoint(), "^");

			skipWSC();

			// Read datatype
			Value datatype = parseValue();
			if (datatype == null) {
				// the datatype IRI could not be parsed. report as error only if VERIFY_URI_SYNTAX is enabled, silently
				// skip otherwise.
				reportError("Invalid datatype IRI for literal '" + label + "'", BasicParserSettings.VERIFY_URI_SYNTAX);
				return null;
			} else if (!(datatype instanceof IRI)) {
				reportFatalError("Illegal datatype value: " + datatype);
			}
			return createLiteral(label, null, (IRI) datatype, getLineNumber(), -1);
		} else {
			return createLiteral(label, null, null, getLineNumber(), -1);
		}
	}

	/**
	 * Parses a quoted string, which is either a "normal string" or a """long string""".
	 *
	 * @return string
	 * @throws IOException
	 * @throws RDFParseException
	 */
	protected String parseQuotedString() throws IOException, RDFParseException {
		String result;

		int c1 = readCodePoint();

		// First character should be '"' or "'"
		verifyCharacterOrFail(c1, "\"\'");

		// Check for long-string, which starts and ends with three double quotes
		int c2 = readCodePoint();
		int c3 = readCodePoint();

		if ((c1 == '"' && c2 == '"' && c3 == '"') || (c1 == '\'' && c2 == '\'' && c3 == '\'')) {
			// Long string
			result = parseLongString(c2);
		} else {
			// Normal string
			unread(c3);
			unread(c2);

			result = parseString(c1);
		}

		// Unescape any escape sequences
		try {
			result = TurtleUtil.decodeString(result);
		} catch (IllegalArgumentException e) {
			reportError(e.getMessage(), BasicParserSettings.VERIFY_DATATYPE_VALUES);
		}

		return result;
	}

	/**
	 * Parses a "normal string". This method requires that the opening character has already been parsed.
	 *
	 * @return parsed string
	 * @throws IOException
	 * @throws RDFParseException
	 */
	protected String parseString(int closingCharacter) throws IOException, RDFParseException {
		StringBuilder sb = getBuilder();

		while (true) {
			int c = readCodePoint();

			if (c == closingCharacter) {
				break;
			} else if (c == -1) {
				throwEOFException();
			}

			if (c == '\r' || c == '\n') {
				reportFatalError("Illegal carriage return or new line in literal");
			}

			if (c == '\r' || c == '\n') {
				reportFatalError("Illegal carriage return or new line in literal");
			}

			appendCodepoint(sb, c);

			if (c == '\\') {
				// This escapes the next character, which might be a '"'
				c = readCodePoint();
				if (c == -1) {
					throwEOFException();
				}
				appendCodepoint(sb, c);
			}
		}

		return sb.toString();
	}

	/**
	 * Parses a """long string""". This method requires that the first three characters have already been parsed.
	 */
	protected String parseLongString(int closingCharacter) throws IOException, RDFParseException {
		StringBuilder sb = getBuilder();

		int doubleQuoteCount = 0;
		int c;

		while (doubleQuoteCount < 3) {
			c = readCodePoint();

			if (c == -1) {
				throwEOFException();
			} else if (c == closingCharacter) {
				doubleQuoteCount++;
			} else {
				doubleQuoteCount = 0;
			}

			appendCodepoint(sb, c);

			if (c == '\n') {
				lineNumber++;
				reportLocation();
			}

			if (c == '\\') {
				// This escapes the next character, which might be a '"'
				c = readCodePoint();
				if (c == -1) {
					throwEOFException();
				}
				appendCodepoint(sb, c);
			}
		}

		return sb.substring(0, sb.length() - 3);
	}

	protected Literal parseNumber() throws IOException, RDFParseException {
		StringBuilder value = getBuilder();
		IRI datatype = XSD.INTEGER;

		int c = readCodePoint();

		// read optional sign character
		if (c == '+' || c == '-') {
			appendCodepoint(value, c);
			c = readCodePoint();
		}

		while (ASCIIUtil.isNumber(c)) {
			appendCodepoint(value, c);
			c = readCodePoint();
		}

		if (c == '.' || c == 'e' || c == 'E') {

			// read optional fractional digits
			if (c == '.') {

				if (TurtleUtil.isWhitespace(peekCodePoint())) {
					// We're parsing an integer that did not have a space before
					// the
					// period to end the statement
				} else {
					appendCodepoint(value, c);

					c = readCodePoint();

					while (ASCIIUtil.isNumber(c)) {
						appendCodepoint(value, c);
						c = readCodePoint();
					}

					if (value.length() == 1) {
						// We've only parsed a '.'
						reportFatalError("Object for statement missing");
					}

					// We're parsing a decimal or a double
					datatype = XSD.DECIMAL;
				}
			} else {
				if (value.length() == 0) {
					// We've only parsed an 'e' or 'E'
					reportFatalError("Object for statement missing");
				}
			}

			// read optional exponent
			if (c == 'e' || c == 'E') {
				datatype = XSD.DOUBLE;
				appendCodepoint(value, c);

				c = readCodePoint();
				if (c == '+' || c == '-') {
					appendCodepoint(value, c);
					c = readCodePoint();
				}

				if (!ASCIIUtil.isNumber(c)) {
					reportError("Exponent value missing", BasicParserSettings.VERIFY_DATATYPE_VALUES);
				}

				appendCodepoint(value, c);

				c = readCodePoint();
				while (ASCIIUtil.isNumber(c)) {
					appendCodepoint(value, c);
					c = readCodePoint();
				}
			}
		}

		// Unread last character, it isn't part of the number
		unread(c);

		// String label = value.toString();
		// if (datatype.equals(XMLSchema.INTEGER)) {
		// try {
		// label = XMLDatatypeUtil.normalizeInteger(label);
		// }
		// catch (IllegalArgumentException e) {
		// // Note: this should never happen because of the parse constraints
		// reportError("Illegal integer value: " + label);
		// }
		// }
		// return createLiteral(label, null, datatype);

		// Return result as a typed literal
		return createLiteral(value.toString(), null, datatype, getLineNumber(), -1);
	}

	protected IRI parseURI() throws IOException, RDFParseException {
		StringBuilder uriBuf = getBuilder();

		// First character should be '<'
		int c = readCodePoint();
		verifyCharacterOrFail(c, "<");

		boolean uriIsIllegal = false;
		// Read up to the next '>' character
		while (true) {
			c = readCodePoint();

			if (c == '>') {
				break;
			} else if (c == -1) {
				throwEOFException();
			}

			if (c == ' ') {
				reportError("IRI included an unencoded space: '" + c + "'", BasicParserSettings.VERIFY_URI_SYNTAX);
				uriIsIllegal = true;
			}

			appendCodepoint(uriBuf, c);

			if (c == '\\') {
				// This escapes the next character, which might be a '>'
				c = readCodePoint();
				if (c == -1) {
					throwEOFException();
				}
				if (c != 'u' && c != 'U') {
					reportError("IRI includes string escapes: '\\" + c + "'", BasicParserSettings.VERIFY_URI_SYNTAX);
					uriIsIllegal = true;
				}
				appendCodepoint(uriBuf, c);
			}
		}

		if (c == '.') {
			reportError("IRI must not end in a '.'", BasicParserSettings.VERIFY_URI_SYNTAX);
			uriIsIllegal = true;
		}

		// do not report back the actual URI if it's illegal and the parser is
		// configured to verify URI syntax.
		if (!(uriIsIllegal && getParserConfig().get(BasicParserSettings.VERIFY_URI_SYNTAX))) {
			String uri = uriBuf.toString();

			// Unescape any escape sequences
			try {
				// FIXME: The following decodes \n and similar in URIs, which
				// should
				// be
				// invalid according to test <turtle-syntax-bad-uri-04.ttl>
				uri = TurtleUtil.decodeString(uri);
			} catch (IllegalArgumentException e) {
				reportError(e.getMessage(), BasicParserSettings.VERIFY_DATATYPE_VALUES);
			}

			return super.resolveURI(uri);
		}

		return null;
	}

	/**
	 * Parses qnames and boolean values, which have equivalent starting characters.
	 */
	protected Value parseQNameOrBoolean() throws IOException, RDFParseException {
		// First character should be a ':' or a letter
		int c = readCodePoint();
		if (c == -1) {
			throwEOFException();
		}
		if (c != ':' && !TurtleUtil.isPrefixStartChar(c)) {
			reportError("Expected a ':' or a letter, found '" + new String(Character.toChars(c)) + "'",
					BasicParserSettings.VERIFY_RELATIVE_URIS);
		}

		String namespace;

		if (c == ':') {
			// qname using default namespace
			namespace = getNamespace("");
		} else {
			// c is the first letter of the prefix
			StringBuilder prefix = new StringBuilder(8);
			appendCodepoint(prefix, c);

			int previousChar = c;
			c = readCodePoint();
			while (TurtleUtil.isPrefixChar(c)) {
				appendCodepoint(prefix, c);
				previousChar = c;
				c = readCodePoint();
			}
			while (previousChar == '.' && prefix.length() > 0) {
				// '.' is a legal prefix name char, but can not appear at the end
				unread(c);
				c = previousChar;
				prefix.setLength(prefix.length() - 1);
				previousChar = prefix.codePointAt(prefix.codePointCount(0, prefix.length()) - 1);
			}

			if (c != ':') {
				// prefix may actually be a boolean value
				String value = prefix.toString();

				if (value.equals("true")) {
					unread(c);
					return createLiteral("true", null, XSD.BOOLEAN, getLineNumber(), -1);
				} else if (value.equals("false")) {
					unread(c);
					return createLiteral("false", null, XSD.BOOLEAN, getLineNumber(), -1);
				}
			}

			verifyCharacterOrFail(c, ":");

			namespace = getNamespace(prefix.toString());
		}

		// c == ':', read optional local name
		StringBuilder localName = new StringBuilder(16);
		c = readCodePoint();
		if (TurtleUtil.isNameStartChar(c)) {
			if (c == '\\') {
				localName.append(readLocalEscapedChar());
			} else {
				appendCodepoint(localName, c);
			}

			int previousChar = c;
			c = readCodePoint();
			while (TurtleUtil.isNameChar(c)) {
				if (c == '\\') {
					localName.append(readLocalEscapedChar());
				} else {
					appendCodepoint(localName, c);
				}
				previousChar = c;
				c = readCodePoint();
			}

			// Unread last character
			unread(c);

			if (previousChar == '.') {
				// '.' is a legal name char, but can not appear at the end, so
				// is
				// not actually part of the name
				unread(previousChar);
				localName.deleteCharAt(localName.length() - 1);
			}
		} else {
			// Unread last character
			unread(c);
		}

		String localNameString = localName.toString();

		for (int i = 0; i < localNameString.length(); i++) {
			if (localNameString.charAt(i) == '%') {
				if (i > localNameString.length() - 3 || !ASCIIUtil.isHex(localNameString.charAt(i + 1))
						|| !ASCIIUtil.isHex(localNameString.charAt(i + 2))) {
					reportFatalError("Found incomplete percent-encoded sequence: " + localNameString);
				}
			}
		}

		// if (c == '.') {
		// reportFatalError("Blank node identifier must not end in a '.'");
		// }

		// Note: namespace has already been resolved
		return createURI(namespace + localNameString);
	}

	private char readLocalEscapedChar() throws RDFParseException, IOException {
		int c = readCodePoint();

		if (TurtleUtil.isLocalEscapedChar(c)) {
			return (char) c;
		} else {
			throw new RDFParseException("found '" + new String(Character.toChars(c)) + "', expected one of: "
					+ Arrays.toString(TurtleUtil.LOCAL_ESCAPED_CHARS));
		}
	}

	/**
	 * Parses a blank node ID, e.g. <var>_:node1</var>.
	 */
	protected Resource parseNodeID() throws IOException, RDFParseException {
		// Node ID should start with "_:"
		verifyCharacterOrFail(readCodePoint(), "_");
		verifyCharacterOrFail(readCodePoint(), ":");

		// Read the node ID
		int c = readCodePoint();
		if (c == -1) {
			throwEOFException();
		} else if (!TurtleUtil.isBLANK_NODE_LABEL_StartChar(c)) {
			reportError("Expected a letter, found '" + (char) c + "'", BasicParserSettings.PRESERVE_BNODE_IDS);
		}

		StringBuilder name = getBuilder();
		appendCodepoint(name, c);

		// Read all following letter and numbers, they are part of the name
		c = readCodePoint();

		// If we would never go into the loop we must unread now
		if (!TurtleUtil.isBLANK_NODE_LABEL_Char(c)) {
			unread(c);
		}

		while (TurtleUtil.isBLANK_NODE_LABEL_Char(c)) {
			int previous = c;
			c = readCodePoint();

			if (previous == '.' && (c == -1 || TurtleUtil.isWhitespace(c) || c == '<' || c == '_')) {
				unread(c);
				unread(previous);
				break;
			}
			appendCodepoint(name, previous);
			if (!TurtleUtil.isBLANK_NODE_LABEL_Char(c)) {
				unread(c);
			}
		}

		return createNode(name.toString());
	}

	protected void reportStatement(Resource subj, IRI pred, Value obj) throws RDFParseException, RDFHandlerException {
		if (subj != null && pred != null && obj != null) {
			previousStatement = createStatement(subj, pred, obj);
			if (rdfHandler != null) {
				rdfHandler.handleStatement(previousStatement);
			}
		}
	}

	/**
	 * Verifies that the supplied character code point <var>codePoint</var> is one of the expected characters specified
	 * in <var>expected</var>. This method will throw a <var>ParseException</var> if this is not the case.
	 */
	protected void verifyCharacterOrFail(int codePoint, String expected) throws RDFParseException {
		if (codePoint == -1) {
			throwEOFException();
		}

		final String supplied = new String(Character.toChars(codePoint));

		if (expected.indexOf(supplied) == -1) {
			StringBuilder msg = new StringBuilder(32);
			msg.append("Expected ");
			for (int i = 0; i < expected.length(); i++) {
				if (i > 0) {
					msg.append(" or ");
				}
				msg.append('\'');
				msg.append(expected.charAt(i));
				msg.append('\'');
			}
			msg.append(", found '");
			msg.append(supplied);
			msg.append("'");

			reportFatalError(msg.toString());
		}
	}

	/**
	 * Consumes any white space characters (space, tab, line feed, newline) and comments (#-style) from
	 * <var>reader</var>. After this method has been called, the first character that is returned by <var>reader</var>
	 * is either a non-ignorable character, or EOF. For convenience, this character is also returned by this method.
	 *
	 * @return The next character code point that will be returned by <var>reader</var>.
	 */
	protected int skipWSC() throws IOException, RDFHandlerException {
		int c = readCodePoint();
		while (TurtleUtil.isWhitespace(c) || c == '#') {
			if (c == '#') {
				processComment();
			} else if (c == '\n') {
				// we only count line feeds (LF), not carriage return (CR), as
				// normally a CR is immediately followed by a LF.
				lineNumber++;
				reportLocation();
			}

			c = readCodePoint();
		}

		unread(c);

		return c;
	}

	/**
	 * Consumes characters from reader until the first EOL has been read. This line of text is then passed to the
	 * {@link #rdfHandler} as a comment.
	 */
	protected void processComment() throws IOException, RDFHandlerException {
		StringBuilder comment = getBuilder();
		int c = readCodePoint();
		while (c != -1 && c != 0xD && c != 0xA) {
			appendCodepoint(comment, c);
			c = readCodePoint();
		}

		if (c == 0xA) {
			lineNumber++;
		}

		// c is equal to -1, \r or \n.
		// In case c is equal to \r, we should also read a following \n.
		if (c == 0xD) {
			c = readCodePoint();
			lineNumber++;

			if (c != 0xA) {
				unread(c);
			}
		}
		if (rdfHandler != null) {
			rdfHandler.handleComment(comment.toString());
		}
		reportLocation();
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

	/**
	 * Pushes back the supplied string by copying it to the front of the buffer. After this method returns, successive
	 * calls to {@link #readCodePoint()} will return the code points in the supplied string again, starting at the first
	 * in the String..
	 *
	 * @param string the string to un-read.
	 * @throws IOException
	 */
	protected void unread(String string) throws IOException {
		int i = string.length();
		while (i > 0) {
			final int codePoint = string.codePointBefore(i);
			if (Character.isSupplementaryCodePoint(codePoint)) {
				final char[] surrogatePair = Character.toChars(codePoint);
				reader.unread(surrogatePair);
				i -= surrogatePair.length;
			} else {
				reader.unread(codePoint);
				i--;
			}
		}
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

	protected void reportLocation() {
		reportLocation(getLineNumber(), -1);
	}

	/**
	 * Overrides {@link AbstractRDFParser#reportWarning(String)}, adding line number information to the error.
	 */
	@Override
	protected void reportWarning(String msg) {
		reportWarning(msg, getLineNumber(), -1);
	}

	/**
	 * Overrides {@link AbstractRDFParser#reportError(String, RioSetting)}, adding line number information to the error.
	 */
	@Override
	protected void reportError(String msg, RioSetting<Boolean> setting) throws RDFParseException {
		reportError(msg, getLineNumber(), -1, setting);
	}

	/**
	 * Overrides {@link AbstractRDFParser#reportFatalError(String)}, adding line number information to the error.
	 */
	@Override
	protected void reportFatalError(String msg) throws RDFParseException {
		reportFatalError(msg, getLineNumber(), -1);
	}

	/**
	 * Overrides {@link AbstractRDFParser#reportFatalError(Exception)}, adding line number information to the error.
	 */
	@Override
	protected void reportFatalError(Exception e) throws RDFParseException {
		reportFatalError(e, getLineNumber(), -1);
	}

	protected void throwEOFException() throws RDFParseException {
		throw new RDFParseException("Unexpected end of file");
	}

	protected int getLineNumber() {
		return lineNumber;
	}

	private StringBuilder getBuilder() {
		parsingBuilder.setLength(0);
		return parsingBuilder;
	}

	/**
	 * Appends the characters from codepoint into the string builder. This is the same as Character#toChars but prevents
	 * the additional char array garbage for BMP codepoints.
	 *
	 * @param dst       the destination in which to append the characters
	 * @param codePoint the codepoint to be appended
	 */
	private static void appendCodepoint(StringBuilder dst, int codePoint) {
		if (Character.isBmpCodePoint(codePoint)) {
			dst.append((char) codePoint);
		} else if (Character.isValidCodePoint(codePoint)) {
			dst.append(Character.highSurrogate(codePoint));
			dst.append(Character.lowSurrogate(codePoint));
		} else {
			throw new IllegalArgumentException("Invalid codepoint " + codePoint);
		}
	}

	/**
	 * Peeks at the next two Unicode code points without advancing the reader and returns true if they indicate the
	 * start of an RDF-star triple value. Such values start with '<<'.
	 *
	 * @return true if the next code points indicate the beginning of an RDF-star triple value, false otherwise
	 * @throws IOException
	 */
	protected boolean peekIsTripleValue() throws IOException {
		int c0 = readCodePoint();
		int c1 = readCodePoint();
		unread(c1);
		unread(c0);

		return c0 == '<' && c1 == '<';
	}

	/**
	 * Parser an RDF-star triple value and returns it.
	 *
	 * @return An RDF-star triple.
	 * @throws IOException
	 */
	protected Triple parseTripleValue() throws IOException {
		verifyCharacterOrFail(readCodePoint(), "<");
		verifyCharacterOrFail(readCodePoint(), "<");
		skipWSC();
		Value subject = parseValue();
		if (subject instanceof Resource) {
			skipWSC();
			Value predicate = parseValue();
			if (predicate instanceof IRI) {
				skipWSC();
				Value object = parseValue();
				if (object != null) {
					skipWSC();
					verifyCharacterOrFail(readCodePoint(), ">");
					verifyCharacterOrFail(readCodePoint(), ">");
					return valueFactory.createTriple((Resource) subject, (IRI) predicate, object);
				} else {
					reportFatalError("Missing object in RDF-star triple");
				}
			} else {
				reportFatalError("Illegal predicate value in RDF-star triple: " + predicate);
			}
		} else {
			reportFatalError("Illegal subject val in RDF-star triple: " + subject);
		}

		return null;
	}

	protected void parseAnnotation() throws IOException {
		verifyCharacterOrFail(readCodePoint(), "{");
		verifyCharacterOrFail(readCodePoint(), "|");
		skipWSC();

		// keep reference to original subject and predicate while processing the annotation content
		final Resource currentSubject = subject;
		final IRI currentPredicate = predicate;
		subject = Values.triple(previousStatement);
		parsePredicateObjectList();
		verifyCharacterOrFail(readCodePoint(), "|");
		verifyCharacterOrFail(readCodePoint(), "}");
		subject = currentSubject;
		predicate = currentPredicate;
	}

}
