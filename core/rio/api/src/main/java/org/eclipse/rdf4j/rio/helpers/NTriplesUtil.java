/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.helpers;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.common.text.ASCIIUtil;
import org.eclipse.rdf4j.common.text.StringUtil;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.XSD;

/**
 * Utility methods for N-Triples encoding/decoding.
 */
public class NTriplesUtil {
	/*
	 * The following correspond to the N-Triples grammar (https://www.w3.org/TR/n-triples/#n-triples-grammar).
	 */
	private static final String PN_CHARS_BASE = "[A-Za-z\u00C0-\u00D6\u00D8-\u00F6\u00F8-\u02FF\u0370-\u037D\u037F-\u1FFF"
			+ "\u200C-\u200D\u2070-\u218F\u2C00-\u2FEF\u3001-\uD7FF\uF900-\uFDCF\uFDF0-\uFFFD"
			+ "\uD800\uDC00-\uDB7F\uDFFF]"; // <- \u10000-\uEFFFF expressed with surrogate pairs
	private static final String PN_CHARS_U = "(?:" + PN_CHARS_BASE + "|_)";
	private static final String PN_CHARS = "(?:" + PN_CHARS_U + "|[0-9\u0300-\u036F\u203F-\u2040\u00B7-])";
	private static final String BNODE_ID = "(?:" + PN_CHARS_U + "|[0-9])(?:(?:" + PN_CHARS + "|\\.)*" + PN_CHARS + ")?";
	private static final String BNODE = "_:" + BNODE_ID;

	private static final String HEX = "[0-9A-Fa-f]";
	private static final String UCHAR = "(?:\\\\u" + HEX + "{4}|\\\\U" + HEX + "{8})";
	private static final String IRI = "<(?:[^\u0000-\u0020<>\"{}|^`\\\\]|" + UCHAR + ")*>";

	private static final String ECHAR = "\\\\[tbnrf\"'\\\\]";
	private static final String STRING_LITERAL_QUOTE = "\"(?:[^\"\\\\\n\r]|" + ECHAR + "|" + UCHAR + ")*+\"";
	private static final String LANGTAG = "@[a-zA-Z]+(?:-[a-zA-Z0-9]+)*";
	private static final String LITERAL = STRING_LITERAL_QUOTE + "(?:\\^\\^" + IRI + "|" + LANGTAG + ")?";

	private static final Pattern BNODE_ID_PATTERN = Pattern.compile(BNODE_ID);
	private static final Pattern BNODE_PATTERN = Pattern.compile(BNODE);
	private static final Pattern IRI_PATTERN = Pattern.compile(IRI);
	private static final Pattern LITERAL_PATTERN = Pattern.compile(LITERAL);

	static class TripleMatch {
		Triple triple;
		int length;

		TripleMatch(Triple triple, int length) {
			this.triple = triple;
			this.length = length;
		}
	}

	/**
	 * Parses an N-Triples value, creates an object for it using the supplied ValueFactory and returns this object.
	 *
	 * @param nTriplesValue The N-Triples value to parse.
	 * @param valueFactory  The ValueFactory to use for creating the object.
	 * @return An object representing the parsed value.
	 * @throws IllegalArgumentException If the supplied value could not be parsed correctly.
	 */
	public static Value parseValue(String nTriplesValue, ValueFactory valueFactory) throws IllegalArgumentException {
		if (nTriplesValue.startsWith("<<")) {
			return parseTriple(nTriplesValue, valueFactory);
		} else if (nTriplesValue.startsWith("<")) {
			return parseURI(nTriplesValue, valueFactory);
		} else if (nTriplesValue.startsWith("_:")) {
			return parseBNode(nTriplesValue, valueFactory);
		} else if (nTriplesValue.startsWith("\"")) {
			return parseLiteral(nTriplesValue, valueFactory);
		} else {
			throw new IllegalArgumentException("Not a legal N-Triples value: " + nTriplesValue);
		}
	}

	/**
	 * Parses an N-Triples resource, creates an object for it using the supplied ValueFactory and returns this object.
	 *
	 * @param nTriplesResource The N-Triples resource to parse.
	 * @param valueFactory     The ValueFactory to use for creating the object.
	 * @return An object representing the parsed resource.
	 * @throws IllegalArgumentException If the supplied resource could not be parsed correctly.
	 */
	public static Resource parseResource(String nTriplesResource, ValueFactory valueFactory)
			throws IllegalArgumentException {
		if (nTriplesResource.startsWith("<<")) {
			return parseTriple(nTriplesResource, valueFactory);
		} else if (nTriplesResource.startsWith("<")) {
			return parseURI(nTriplesResource, valueFactory);
		} else if (nTriplesResource.startsWith("_:")) {
			return parseBNode(nTriplesResource, valueFactory);
		} else {
			throw new IllegalArgumentException("Not a legal N-Triples resource: " + nTriplesResource);
		}
	}

	/**
	 * Parses an N-Triples URI, creates an object for it using the supplied ValueFactory and returns this object.
	 *
	 * @param nTriplesURI  The N-Triples URI to parse.
	 * @param valueFactory The ValueFactory to use for creating the object.
	 * @return An object representing the parsed URI.
	 * @throws IllegalArgumentException If the supplied URI could not be parsed correctly.
	 */
	public static IRI parseURI(String nTriplesURI, ValueFactory valueFactory) throws IllegalArgumentException {
		if (nTriplesURI.startsWith("<") && nTriplesURI.endsWith(">")) {
			String uri = nTriplesURI.substring(1, nTriplesURI.length() - 1);
			// Disambiguate with RDF-star triple
			if (!uri.startsWith("<")) {
				uri = unescapeString(uri);
				return valueFactory.createIRI(uri);
			}
		}
		throw new IllegalArgumentException("Not a legal N-Triples URI: " + nTriplesURI);
	}

	/**
	 * Parses an N-Triples bNode, creates an object for it using the supplied ValueFactory and returns this object.
	 *
	 * @param nTriplesBNode The N-Triples bNode to parse.
	 * @param valueFactory  The ValueFactory to use for creating the object.
	 * @return An object representing the parsed bNode.
	 * @throws IllegalArgumentException If the supplied bNode could not be parsed correctly.
	 */
	public static BNode parseBNode(String nTriplesBNode, ValueFactory valueFactory) throws IllegalArgumentException {
		if (nTriplesBNode.startsWith("_:")) {
			return valueFactory.createBNode(nTriplesBNode.substring(2));
		} else {
			throw new IllegalArgumentException("Not a legal N-Triples Blank Node: " + nTriplesBNode);
		}
	}

	/**
	 * Parses an N-Triples literal, creates an object for it using the supplied ValueFactory and returns this object.
	 *
	 * @param nTriplesLiteral The N-Triples literal to parse.
	 * @param valueFactory    The ValueFactory to use for creating the object.
	 * @return An object representing the parsed literal.
	 * @throws IllegalArgumentException If the supplied literal could not be parsed correctly.
	 */
	public static Literal parseLiteral(String nTriplesLiteral, ValueFactory valueFactory)
			throws IllegalArgumentException {
		if (nTriplesLiteral.startsWith("\"")) {
			// Find string separation points
			int endLabelIdx = findEndOfLabel(nTriplesLiteral);

			if (endLabelIdx != -1) {
				int startLangIdx = nTriplesLiteral.indexOf('@', endLabelIdx);
				int startDtIdx = nTriplesLiteral.indexOf("^^", endLabelIdx);

				if (startLangIdx != -1 && startDtIdx != -1) {
					throw new IllegalArgumentException("Literals can not have both a language and a datatype");
				}

				// Get label
				String label = nTriplesLiteral.substring(1, endLabelIdx);
				label = unescapeString(label);

				if (startLangIdx != -1) {
					// Get language
					String language = nTriplesLiteral.substring(startLangIdx + 1);
					return valueFactory.createLiteral(label, language);
				} else if (startDtIdx != -1) {
					// Get datatype
					String datatype = nTriplesLiteral.substring(startDtIdx + 2);
					IRI dtURI = parseURI(datatype, valueFactory);
					return valueFactory.createLiteral(label, dtURI);
				} else {
					return valueFactory.createLiteral(label);
				}
			}
		}

		throw new IllegalArgumentException("Not a legal N-Triples literal: " + nTriplesLiteral);
	}

	/**
	 * Parses an RDF-star triple (non-standard N-Triples), creates an object for it using the supplied ValueFactory and
	 * returns this object.
	 *
	 * @param nTriplesTriple The RDF-star triple to parse.
	 * @param valueFactory   The ValueFactory to use for creating the object.
	 * @return An object representing the parsed triple.
	 * @throws IllegalArgumentException If the supplied triple could not be parsed correctly.
	 */
	public static Triple parseTriple(String nTriplesTriple, ValueFactory valueFactory) {
		TripleMatch tm = parseTripleInternal(nTriplesTriple, valueFactory);
		if (tm.length != nTriplesTriple.length()) {
			throw new IllegalArgumentException("Not a valid N-Triples triple: " + nTriplesTriple);
		}
		return tm.triple;
	}

	/**
	 * Parses an RDF-star triple (non-standard N-Triples), creates an object for it using the supplied ValueFactory and
	 * returns an object that contains the parsed triple and the length of the parsed text.
	 *
	 * @param nTriplesTriple The RDF-star triple to parse.
	 * @param valueFactory   The ValueFactory to use for creating the object.
	 * @return An object representing the parsed triple and the length of the matching text.
	 * @throws IllegalArgumentException If the supplied triple could not be parsed correctly.
	 */
	private static TripleMatch parseTripleInternal(String nTriplesTriple, ValueFactory valueFactory) {
		if (nTriplesTriple.startsWith("<<")) {
			String triple = nTriplesTriple.substring(2);
			int offset = 2;

			while (triple.length() > 0 && Character.isWhitespace(triple.charAt(0))) {
				triple = triple.substring(1);
				++offset;
			}

			Resource subject = null;
			IRI predicate = null;
			Value object = null;

			for (int i = 0; i < 3; i++) {
				Value v = null;
				if (triple.startsWith("_:")) {
					Matcher bNodeMatcher = BNODE_PATTERN.matcher(triple);
					if (bNodeMatcher.find() && bNodeMatcher.start() == 0) {
						String value = bNodeMatcher.group();
						v = NTriplesUtil.parseBNode(value, valueFactory);
						triple = triple.substring(bNodeMatcher.end());
						offset += bNodeMatcher.end();
					}
				} else if (triple.startsWith("<<")) {
					TripleMatch tm = parseTripleInternal(triple, valueFactory);
					triple = triple.substring(tm.length);
					offset += tm.length;
					v = tm.triple;
				} else if (triple.startsWith("<")) {
					Matcher iriMatcher = IRI_PATTERN.matcher(triple);
					if (iriMatcher.find() && iriMatcher.start() == 0) {
						String value = iriMatcher.group();
						v = NTriplesUtil.parseURI(value, valueFactory);
						triple = triple.substring(iriMatcher.end());
						offset += iriMatcher.end();
					}
				} else if (triple.startsWith("\"")) {
					Matcher literalMatcher = LITERAL_PATTERN.matcher(triple);
					if (literalMatcher.find() && literalMatcher.start() == 0) {
						String value = literalMatcher.group();
						v = NTriplesUtil.parseLiteral(value, valueFactory);
						triple = triple.substring(literalMatcher.end());
						offset += literalMatcher.end();
					}
				}

				if (i == 0) {
					if (!(v instanceof Resource)) {
						throw new IllegalArgumentException("Not a valid N-Triples triple: " + nTriplesTriple);
					}
					subject = (Resource) v;
				} else if (i == 1) {
					if (!(v instanceof IRI)) {
						throw new IllegalArgumentException("Not a valid N-Triples triple: " + nTriplesTriple);
					}
					predicate = (org.eclipse.rdf4j.model.IRI) v;
				} else if (i == 2) {
					if (v == null) {
						throw new IllegalArgumentException("Not a valid N-Triples triple: " + nTriplesTriple);
					}
					object = v;
				}
				while (triple.length() > 0 && Character.isWhitespace(triple.charAt(0))) {
					triple = triple.substring(1);
					++offset;
				}
			}

			if (triple.endsWith(">>")) {
				offset += 2;
				return new TripleMatch(valueFactory.createTriple(subject, predicate, object), offset);
			}
		}

		throw new IllegalArgumentException("Not a valid N-Triples triple: " + nTriplesTriple);
	}

	/**
	 * Finds the end of the label in a literal string. This method takes into account that characters can be escaped
	 * using backslashes.
	 *
	 * @return The index of the double quote ending the label, or <var>-1</var> if it could not be found.
	 */
	private static int findEndOfLabel(String nTriplesLiteral) {
		// First character of literal is guaranteed to be a double
		// quote, start search at second character.

		boolean previousWasBackslash = false;

		for (int i = 1; i < nTriplesLiteral.length(); i++) {
			char c = nTriplesLiteral.charAt(i);

			if (c == '"' && !previousWasBackslash) {
				return i;
			} else if (c == '\\' && !previousWasBackslash) {
				// start of escape
				previousWasBackslash = true;
			} else if (previousWasBackslash) {
				// c was escaped
				previousWasBackslash = false;
			}
		}

		return -1;
	}

	/**
	 * Creates an N-Triples string for the supplied value.
	 *
	 * @param value
	 * @return string
	 */
	public static String toNTriplesString(Value value) {
		// default to false. Users must call new method directly to remove
		// xsd:string
		return toNTriplesString(value, BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL.getDefaultValue());
	}

	/**
	 * Creates an N-Triples string for the supplied value.If the supplied value is a {@link Literal}, it optionally
	 * ignores the xsd:string datatype, since this datatype is implicit in RDF-1.1.
	 *
	 * @param value                   The value to write.
	 * @param xsdStringToPlainLiteral True to omit serialising the xsd:string datatype and false to always serialise the
	 *                                datatype for literals.
	 * @return string
	 */
	public static String toNTriplesString(Value value, boolean xsdStringToPlainLiteral) {
		if (value instanceof Resource) {
			return toNTriplesString((Resource) value);
		} else if (value instanceof Literal) {
			return toNTriplesString((Literal) value, xsdStringToPlainLiteral);
		} else {
			throw new IllegalArgumentException("Unknown value type: " + value.getClass());
		}
	}

	/**
	 * Appends the N-Triples representation of the given {@link Value} to the given {@link Appendable}.
	 *
	 * @param value      The value to write.
	 * @param appendable The object to append to.
	 * @throws IOException
	 */
	public static void append(Value value, Appendable appendable) throws IOException {
		// default to false. Users must call new method directly to remove
		// xsd:string
		append(value, appendable, BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL.getDefaultValue(),
				NTriplesWriterSettings.ESCAPE_UNICODE.getDefaultValue());
	}

	/**
	 * Appends the N-Triples representation of the given {@link Value} to the given {@link Appendable}, optionally not
	 * serializing the datatype a {@link Literal} with the xsd:string datatype as it is implied for RDF-1.1.
	 *
	 * @param value                   The value to write.
	 * @param appendable              The object to append to.
	 * @param xsdStringToPlainLiteral True to omit serializing the xsd:string datatype and false to always serialize the
	 *                                datatype for literals.
	 * @param escapeUnicode
	 * @throws IOException
	 */
	public static void append(Value value, Appendable appendable, boolean xsdStringToPlainLiteral,
			boolean escapeUnicode) throws IOException {
		if (value instanceof Resource) {
			append((Resource) value, appendable);
		} else if (value instanceof Literal) {
			append((Literal) value, appendable, xsdStringToPlainLiteral, escapeUnicode);
		} else {
			throw new IllegalArgumentException("Unknown value type: " + value.getClass());
		}
	}

	/**
	 * Creates an N-Triples string for the supplied resource.
	 *
	 * @param resource
	 * @return string
	 */
	public static String toNTriplesString(Resource resource) {
		if (resource instanceof IRI) {
			return toNTriplesString((IRI) resource);
		} else if (resource instanceof BNode) {
			return toNTriplesString((BNode) resource);
		} else if (resource instanceof Triple) {
			return toNTriplesString((Triple) resource);
		} else {
			throw new IllegalArgumentException("Unknown resource type: " + resource.getClass());
		}
	}

	/**
	 * Appends the N-Triples representation of the given {@link Resource} to the given {@link Appendable}.
	 *
	 * @param resource   The resource to write.
	 * @param appendable The object to append to.
	 * @throws IOException
	 */
	public static void append(Resource resource, Appendable appendable) throws IOException {
		if (resource instanceof IRI) {
			append((IRI) resource, appendable);
		} else if (resource instanceof BNode) {
			append((BNode) resource, appendable);
		} else if (resource instanceof Triple) {
			append((Triple) resource, appendable);
		} else {
			throw new IllegalArgumentException("Unknown resource type: " + resource.getClass());
		}
	}

	/**
	 * Creates an N-Triples string for the supplied URI.
	 *
	 * @param uri
	 * @return string
	 */
	public static String toNTriplesString(IRI uri) {
		return "<" + escapeString(uri.toString()) + ">";
	}

	/**
	 * Appends the N-Triples representation of the given {@link IRI} to the given {@link Appendable}.
	 *
	 * @param uri        The IRI to write.
	 * @param appendable The object to append to.
	 * @throws IOException
	 */
	public static void append(IRI uri, Appendable appendable) throws IOException {
		append(uri, appendable, true);
	}

	/**
	 * Appends the N-Triples representation of the given {@link IRI} to the given {@link Appendable}.
	 *
	 * @param uri
	 * @param appendable
	 * @param escapeUnicode
	 * @throws IOException
	 */
	public static void append(IRI uri, Appendable appendable, boolean escapeUnicode) throws IOException {
		appendable.append('<');
		StringUtil.simpleEscapeIRI(uri.toString(), appendable, escapeUnicode);
		appendable.append('>');
	}

	/**
	 * Creates an N-Triples string for the supplied blank node.
	 *
	 * @param bNode
	 * @return string
	 */
	public static String toNTriplesString(BNode bNode) {
		try {
			StringBuilder result = new StringBuilder(bNode.getID().length() + 1);
			append(bNode, result);
			return result.toString();
		} catch (IOException e) {
			throw new RuntimeException("Should not receive IOException with StringBuilder", e);
		}
	}

	/**
	 * Appends the N-Triples representation of the given {@link BNode} to the given {@link Appendable}.
	 *
	 * @param bNode
	 * @param appendable
	 * @throws IOException
	 */
	public static void append(BNode bNode, Appendable appendable) throws IOException {
		String nextId = bNode.getID();
		appendable.append("_:");

		if (nextId.isEmpty() || !BNODE_ID_PATTERN.matcher(nextId).matches()) {
			appendable.append("genid");
			appendable.append(Integer.toHexString(bNode.hashCode()));
		} else {
			// The regex check via BNODE_ID_PATTERN also covers SES-2129, previous workaround in Protocol.encodeValue()
			appendable.append(nextId);
		}
	}

	/**
	 * Creates an N-Triples string for the supplied literal.
	 *
	 * @param lit
	 * @return string
	 */
	public static String toNTriplesString(Literal lit) {
		// default to false. Users must call new method directly to remove
		// xsd:string
		return toNTriplesString(lit, BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL.getDefaultValue());
	}

	/**
	 * Creates an N-Triples string for the supplied literal, optionally ignoring the xsd:string datatype as it is
	 * implied for RDF-1.1.
	 *
	 * @param lit                     The literal to write.
	 * @param xsdStringToPlainLiteral True to omit serializing the xsd:string datatype and false to always serialize the
	 *                                datatype for literals.
	 * @return String
	 */
	public static String toNTriplesString(Literal lit, boolean xsdStringToPlainLiteral) {
		try {
			StringBuilder sb = new StringBuilder();
			append(lit, sb, xsdStringToPlainLiteral, NTriplesWriterSettings.ESCAPE_UNICODE.getDefaultValue());
			return sb.toString();
		} catch (IOException e) {
			throw new AssertionError();
		}
	}

	/**
	 * Appends the N-Triples representation of the given {@link Literal} to the given {@link Appendable}.
	 *
	 * @param lit
	 * @param appendable
	 * @throws IOException
	 */
	public static void append(Literal lit, Appendable appendable) throws IOException {
		// default to false. Users must call new method directly to remove
		// xsd:string
		append(lit, appendable, BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL.getDefaultValue(),
				NTriplesWriterSettings.ESCAPE_UNICODE.getDefaultValue());
	}

	/**
	 * Appends the N-Triples representation of the given {@link Literal} to the given {@link Appendable}, optionally
	 * ignoring the xsd:string datatype as it is implied for RDF-1.1.
	 *
	 * @param lit                     The literal to write.
	 * @param appendable              The object to append to.
	 * @param xsdStringToPlainLiteral True to omit serializing the xsd:string datatype and false to always serialize the
	 *                                datatype for literals.
	 * @param escapeUnicode           True to escape non-ascii/non-printable characters using Unicode escapes
	 *                                (<var>&#x5C;uxxxx</var> and <var>&#x5C;Uxxxxxxxx</var>), false to print without
	 *                                escaping.
	 * @throws IOException
	 */
	public static void append(Literal lit, Appendable appendable, boolean xsdStringToPlainLiteral,
			boolean escapeUnicode) throws IOException {
		// Do some character escaping on the label:
		appendable.append("\"");
		escapeString(lit.getLabel(), appendable, escapeUnicode);
		appendable.append("\"");

		if (Literals.isLanguageLiteral(lit)) {
			// Append the literal's language
			appendable.append("@");
			appendable.append(lit.getLanguage().get());
		} else {
			// SES-1917 : In RDF-1.1, all literals have a type, and if they are not
			// language literals we display the type for backwards compatibility
			// Append the literal's datatype
			IRI datatype = lit.getDatatype();
			boolean ignoreDatatype = datatype.equals(XSD.STRING) && xsdStringToPlainLiteral;
			if (!ignoreDatatype) {
				appendable.append("^^");
				append(lit.getDatatype(), appendable);
			}
		}
	}

	/**
	 * Creates an N-Triples (non-standard) string for the supplied RDF-star triple.
	 *
	 * @param triple
	 * @return string
	 */
	public static String toNTriplesString(Triple triple) {
		return "<<" + NTriplesUtil.toNTriplesString(triple.getSubject()) + " "
				+ NTriplesUtil.toNTriplesString(triple.getPredicate()) + " "
				+ NTriplesUtil.toNTriplesString(triple.getObject()) + ">>";
	}

	/**
	 * Appends the N-Triples (non-standard) representation of the given {@link Triple} to the given {@link Appendable}.
	 *
	 * @param triple
	 * @param appendable
	 * @throws IOException
	 */
	public static void append(Triple triple, Appendable appendable) throws IOException {
		appendable.append("<<");
		append(triple.getSubject(), appendable);
		appendable.append(' ');
		append(triple.getPredicate(), appendable);
		appendable.append(' ');
		append(triple.getObject(), appendable);
		appendable.append(">>");
	}

	/**
	 * Checks whether the supplied character is a letter or number according to the N-Triples specification.
	 *
	 * @deprecated use {@link ASCIIUtil#isLetterOrNumber(int)}
	 * @see #isLetter
	 * @see #isNumber
	 * @param c
	 * @return true if it is a letter or a number
	 */
	@Deprecated
	public static boolean isLetterOrNumber(int c) {
		return ASCIIUtil.isLetterOrNumber(c);
	}

	/**
	 * Checks whether the supplied character is a letter according to the N-Triples specification.N-Triples letters are
	 * A - Z and a - z.
	 *
	 * @deprecated use {@link ASCIIUtil#isLetter(int)}
	 * @param c
	 * @return true if c is an ascii leter
	 */
	@Deprecated
	public static boolean isLetter(int c) {
		return ASCIIUtil.isLetter(c);
	}

	/**
	 * Checks whether the supplied character is a number according to the N-Triples specification.N-Triples numbers are
	 * 0 - 9.
	 *
	 * @deprecated use {@link ASCIIUtil#isNumber(int)}
	 * @param c
	 * @return true if the character is a number
	 */
	@Deprecated
	public static boolean isNumber(int c) {
		return ASCIIUtil.isNumber(c);
	}

	/**
	 * Checks whether the supplied character is valid character as per N-Triples specification.
	 *
	 * @see <a href="https://www.w3.org/TR/n-triples/#BNodes">https://www.w3.org/TR/n-triples/#BNodes</a>
	 * @param c
	 * @return true if valid
	 */
	public static boolean isValidCharacterForBNodeLabel(int c) {
		return ASCIIUtil.isLetterOrNumber(c) || isLiberalCharactersButNotDot(c) || isDot(c);
	}

	/**
	 * Checks whether the supplied character is in list of liberal characters according to the N-Triples specification
	 * except Dot.
	 *
	 * @param c
	 * @return true if valid
	 */
	public static boolean isLiberalCharactersButNotDot(int c) {
		return isUnderscore(c) || c == 45 || c == 183 || (c >= 768 && c <= 879) || c == 8255 || c == 8256;
	}

	/**
	 * Checks whether the supplied character is Underscore.
	 *
	 * @param c
	 * @return true if it is an underscore
	 */
	public static boolean isUnderscore(int c) {
		return c == 95;
	}

	/**
	 * Checks whether the supplied character is Dot '.'.
	 *
	 * @param c
	 * @return true if it is a dot
	 */
	public static boolean isDot(int c) {
		return c == 46;
	}

	/**
	 * Escapes a Unicode string to an all-ASCII character sequence.Any special characters are escaped using backslashes
	 * ( <var>"</var> becomes <var>\"</var>, etc.), and non-ascii/non-printable characters are escaped using Unicode
	 * escapes ( <var>&#x5C;uxxxx</var> and <var>&#x5C;Uxxxxxxxx</var>).
	 *
	 * @param label
	 * @return an escaped string (unicode to ascii plus codepoints).
	 */
	public static String escapeString(String label) {
		try {
			StringBuilder sb = new StringBuilder(2 * label.length());
			escapeString(label, sb);
			return sb.toString();
		} catch (IOException e) {
			throw new AssertionError();
		}
	}

	/**
	 * Escapes a Unicode string to an all-ASCII character sequence. Any special characters are escaped using backslashes
	 * ( <var>"</var> becomes <var>\"</var>, etc.), and non-ascii/non-printable characters are escaped using Unicode
	 * escapes ( <var>&#x5C;uxxxx</var> and <var>&#x5C;Uxxxxxxxx</var>).
	 *
	 * @param label
	 * @param appendable
	 * @throws IOException
	 */
	public static void escapeString(String label, Appendable appendable) throws IOException {
		escapeString(label, appendable, true);
	}

	/**
	 * Escapes a Unicode string to an N-Triples compatible character sequence.Any special characters are escaped using
	 * backslashes (<var>"</var> becomes <var>\"</var>, etc.), and non-ascii/non-printable characters are escaped using
	 * Unicode escapes (<var>&#x5C;uxxxx</var> and <var>&#x5C;Uxxxxxxxx</var>) if the option is selected.
	 *
	 * @param label
	 * @param appendable
	 * @param escapeUnicode
	 * @throws IOException
	 */
	public static void escapeString(String label, Appendable appendable, boolean escapeUnicode) throws IOException {
		int labelLength = label.length();

		for (int i = 0; i < labelLength; i++) {
			char c = label.charAt(i);
			int cInt = c;

			if (c == '\\') {
				appendable.append("\\\\");
			} else if (c == '"') {
				appendable.append("\\\"");
			} else if (c == '\n') {
				appendable.append("\\n");
			} else if (c == '\r') {
				appendable.append("\\r");
			} else if (c == '\t') {
				appendable.append("\\t");
			} else if (cInt >= 0x0 && cInt <= 0x8 || cInt == 0xB || cInt == 0xC || cInt >= 0xE && cInt <= 0x1F
					|| cInt >= 0x7F && cInt <= 0xFFFF) {
				if (escapeUnicode) {
					appendable.append("\\u");
					appendable.append(toHexString(cInt, 4));
				} else {
					appendable.append(c);
				}
			} else if (cInt >= 0x10000 && cInt <= 0x10FFFF) {
				if (escapeUnicode) {
					appendable.append("\\U");
					appendable.append(toHexString(cInt, 8));
				} else {
					appendable.append(c);
				}
			} else {
				appendable.append(c);
			}
		}
	}

	/**
	 * Unescapes an escaped Unicode string. Any Unicode sequences ( <var>&#x5C;uxxxx</var> and
	 * <var>&#x5C;Uxxxxxxxx</var>) are restored to the value indicated by the hexadecimal argument and any
	 * backslash-escapes ( <var>\"</var>, <var>\\</var>, etc.) are decoded to their original form.
	 *
	 * @param s An escaped Unicode string.
	 * @return The unescaped string.
	 * @throws IllegalArgumentException If the supplied string is not a correctly escaped N-Triples string.
	 */
	public static String unescapeString(String s) {
		int backSlashIdx = s.indexOf('\\');

		if (backSlashIdx == -1) {
			// No escaped characters found
			return s;
		}

		int startIdx = 0;
		int sLength = s.length();
		StringBuilder sb = new StringBuilder(sLength);

		while (backSlashIdx != -1) {
			sb.append(s.substring(startIdx, backSlashIdx));

			if (backSlashIdx + 1 >= sLength) {
				throw new IllegalArgumentException("Unescaped backslash in: " + s);
			}

			char c = s.charAt(backSlashIdx + 1);

			if (c == 't') {
				sb.append('\t');
				startIdx = backSlashIdx + 2;
			} else if (c == 'b') {
				sb.append('\b');
				startIdx = backSlashIdx + 2;
			} else if (c == 'n') {
				sb.append('\n');
				startIdx = backSlashIdx + 2;
			} else if (c == 'r') {
				sb.append('\r');
				startIdx = backSlashIdx + 2;
			} else if (c == 'f') {
				sb.append('\f');
				startIdx = backSlashIdx + 2;
			} else if (c == '"') {
				sb.append('"');
				startIdx = backSlashIdx + 2;
			} else if (c == '\'') {
				sb.append('\'');
				startIdx = backSlashIdx + 2;
			} else if (c == '\\') {
				sb.append('\\');
				startIdx = backSlashIdx + 2;
			} else if (c == 'u') {
				// \\uxxxx
				if (backSlashIdx + 5 >= sLength) {
					throw new IllegalArgumentException("Incomplete Unicode escape sequence in: " + s);
				}
				String xx = s.substring(backSlashIdx + 2, backSlashIdx + 6);

				try {
					final int codePoint = Integer.parseInt(xx, 16);
					sb.append(Character.toChars(codePoint));

					startIdx = backSlashIdx + 6;
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Illegal Unicode escape sequence '\\u" + xx + "' in: " + s);
				}
			} else if (c == 'U') {
				// \\Uxxxxxxxx
				if (backSlashIdx + 9 >= sLength) {
					throw new IllegalArgumentException("Incomplete Unicode escape sequence in: " + s);
				}
				String xx = s.substring(backSlashIdx + 2, backSlashIdx + 10);

				try {
					final int codePoint = Integer.parseInt(xx, 16);
					sb.append(Character.toChars(codePoint));

					startIdx = backSlashIdx + 10;
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Illegal Unicode escape sequence '\\U" + xx + "' in: " + s);
				}
			} else {
				throw new IllegalArgumentException("Unescaped backslash in: " + s);
			}

			backSlashIdx = s.indexOf('\\', startIdx);
		}

		sb.append(s.substring(startIdx));

		return sb.toString();
	}

	/**
	 * Converts a decimal value to a hexadecimal string representation of the specified length.
	 *
	 * @param decimal      A decimal value.
	 * @param stringLength The length of the resulting string.
	 * @return padded string
	 */
	public static String toHexString(int decimal, int stringLength) {
		StringBuilder sb = new StringBuilder(stringLength);

		String hexVal = Integer.toHexString(decimal).toUpperCase();

		// insert zeros if hexVal has less than stringLength characters:
		int nofZeros = stringLength - hexVal.length();
		for (int i = 0; i < nofZeros; i++) {
			sb.append('0');
		}

		sb.append(hexVal);

		return sb.toString();
	}
}
