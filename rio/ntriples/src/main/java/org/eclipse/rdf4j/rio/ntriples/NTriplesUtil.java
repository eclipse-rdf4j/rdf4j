/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.ntriples;

import java.io.IOException;

import org.eclipse.rdf4j.common.text.StringUtil;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.NTriplesWriterSettings;

/**
 * Utility methods for N-Triples encoding/decoding.
 */
public class NTriplesUtil {

	/**
	 * Parses an N-Triples value, creates an object for it using the supplied ValueFactory and returns this object.
	 * 
	 * @param nTriplesValue The N-Triples value to parse.
	 * @param valueFactory  The ValueFactory to use for creating the object.
	 * @return An object representing the parsed value.
	 * @throws IllegalArgumentException If the supplied value could not be parsed correctly.
	 */
	public static Value parseValue(String nTriplesValue, ValueFactory valueFactory) throws IllegalArgumentException {
		if (nTriplesValue.startsWith("<")) {
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
		if (nTriplesResource.startsWith("<")) {
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
			uri = unescapeString(uri);
			return valueFactory.createIRI(uri);
		} else {
			throw new IllegalArgumentException("Not a legal N-Triples URI: " + nTriplesURI);
		}
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
				int startLangIdx = nTriplesLiteral.indexOf("@", endLabelIdx);
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
	 * Finds the end of the label in a literal string. This method takes into account that characters can be escaped
	 * using backslashes.
	 * 
	 * @return The index of the double quote ending the label, or <tt>-1</tt> if it could not be found.
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
	 */
	public static String toNTriplesString(Value value) {
		// default to false. Users must call new method directly to remove
		// xsd:string
		return toNTriplesString(value, BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL.getDefaultValue());
	}

	/**
	 * Creates an N-Triples string for the supplied value. If the supplied value is a {@link Literal}, it optionally
	 * ignores the xsd:string datatype, since this datatype is implicit in RDF-1.1.
	 * 
	 * @param value                   The value to write.
	 * @param xsdStringToPlainLiteral True to omit serialising the xsd:string datatype and false to always serialise the
	 *                                datatype for literals.
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

	public static void append(Value value, Appendable appendable) throws IOException {
		// default to false. Users must call new method directly to remove
		// xsd:string
		append(value, appendable, BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL.getDefaultValue(),
				NTriplesWriterSettings.ESCAPE_UNICODE.getDefaultValue());
	}

	/**
	 * Appends the N-Triples representation of the given {@link Value} to the given {@link Appendable}, optionally not
	 * serialising the datatype a {@link Literal} with the xsd:string datatype as it is implied for RDF-1.1.
	 * 
	 * @param value                   The value to write.
	 * @param appendable              The object to append to.
	 * @param xsdStringToPlainLiteral True to omit serialising the xsd:string datatype and false to always serialise the
	 *                                datatype for literals.
	 * @throws IOException
	 */
	public static void append(Value value, Appendable appendable, boolean xsdStringToPlainLiteral,
			boolean escapeUnicode) throws IOException {
		if (value instanceof Resource) {
			append((Resource) value, appendable);
		} else if (value instanceof Literal) {
			append((Literal) value, appendable, xsdStringToPlainLiteral);
		} else {
			throw new IllegalArgumentException("Unknown value type: " + value.getClass());
		}
	}

	/**
	 * Creates an N-Triples string for the supplied resource.
	 */
	public static String toNTriplesString(Resource resource) {
		if (resource instanceof IRI) {
			return toNTriplesString((IRI) resource);
		} else if (resource instanceof BNode) {
			return toNTriplesString((BNode) resource);
		} else {
			throw new IllegalArgumentException("Unknown resource type: " + resource.getClass());
		}
	}

	public static void append(Resource resource, Appendable appendable) throws IOException {
		if (resource instanceof IRI) {
			append((IRI) resource, appendable);
		} else if (resource instanceof BNode) {
			append((BNode) resource, appendable);
		} else {
			throw new IllegalArgumentException("Unknown resource type: " + resource.getClass());
		}
	}

	/**
	 * Creates an N-Triples string for the supplied URI.
	 */
	public static String toNTriplesString(IRI uri) {
		return "<" + escapeString(uri.toString()) + ">";
	}

	public static void append(IRI uri, Appendable appendable) throws IOException {
		StringBuilder sb = new StringBuilder();
		escapeString(uri.toString(), sb);
		String s = sb.toString();
		s = StringUtil.gsub("<", "\\u003C", s);
		s = StringUtil.gsub(">", "\\u003E", s);
		appendable.append("<").append(s).append(">");
	}

	/**
	 * Creates an N-Triples string for the supplied bNode.
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

	public static void append(BNode bNode, Appendable appendable) throws IOException {
		String nextId = bNode.getID();
		appendable.append("_:");

		if (nextId.isEmpty()) {
			appendable.append("genid");
			appendable.append(Integer.toHexString(bNode.hashCode()));
		} else {
			if (!isLetter(nextId.charAt(0))) {
				appendable.append("genid");
				appendable.append(Integer.toHexString(nextId.charAt(0)));
			}

			for (int i = 0; i < nextId.length(); i++) {
				if (isLetterOrNumber(nextId.charAt(i))) {
					appendable.append(nextId.charAt(i));
				} else {
					// Append the character as its hex representation
					appendable.append(Integer.toHexString(nextId.charAt(i)));
				}
			}
		}
	}

	/**
	 * Creates an N-Triples string for the supplied literal.
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
	 * @param xsdStringToPlainLiteral True to omit serialising the xsd:string datatype and false to always serialise the
	 *                                datatype for literals.
	 */
	public static String toNTriplesString(Literal lit, boolean xsdStringToPlainLiteral) {
		try {
			StringBuilder sb = new StringBuilder();
			append(lit, sb, xsdStringToPlainLiteral);
			return sb.toString();
		} catch (IOException e) {
			throw new AssertionError();
		}
	}

	public static void append(Literal lit, Appendable appendable) throws IOException {
		// default to false. Users must call new method directly to remove
		// xsd:string
		append(lit, appendable, BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL.getDefaultValue());
	}

	/**
	 * Appends the N-Triples representation of the given {@link Literal} to the given {@link Appendable}, optionally
	 * ignoring the xsd:string datatype as it is implied for RDF-1.1.
	 * 
	 * @param lit                     The literal to write.
	 * @param appendable              The object to append to.
	 * @param xsdStringToPlainLiteral True to omit serialising the xsd:string datatype and false to always serialise the
	 *                                datatype for literals.
	 * @throws IOException
	 */
	public static void append(Literal lit, Appendable appendable, boolean xsdStringToPlainLiteral) throws IOException {
		// Do some character escaping on the label:
		appendable.append("\"");
		escapeString(lit.getLabel(), appendable);
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
			boolean ignoreDatatype = datatype.equals(XMLSchema.STRING) && xsdStringToPlainLiteral;
			if (!ignoreDatatype) {
				appendable.append("^^");
				append(lit.getDatatype(), appendable);
			}
		}
	}

	/**
	 * Checks whether the supplied character is a letter or number according to the N-Triples specification.
	 * 
	 * @see #isLetter
	 * @see #isNumber
	 */
	public static boolean isLetterOrNumber(int c) {
		return isLetter(c) || isNumber(c);
	}

	/**
	 * Checks whether the supplied character is a letter according to the N-Triples specification. N-Triples letters are
	 * A - Z and a - z.
	 */
	public static boolean isLetter(int c) {
		return (c >= 65 && c <= 90) || // A - Z
				(c >= 97 && c <= 122); // a - z
	}

	/**
	 * Checks whether the supplied character is a number according to the N-Triples specification. N-Triples numbers are
	 * 0 - 9.
	 */
	public static boolean isNumber(int c) {
		return (c >= 48 && c <= 57); // 0 - 9
	}

	/**
	 * Checks whether the supplied character is valid character as per N-Triples specification. See
	 * <a href="https://www.w3.org/TR/n-triples/#BNodes">https://www.w3.org/TR/n-triples/#BNodes</a>.
	 *
	 */
	public static boolean isValidCharacterForBNodeLabel(int c) {
		return isLetterOrNumber(c) || isLiberalCharactersButNotDot(c) || isDot(c);
	}

	/**
	 * Checks whether the supplied character is in list of liberal characters according to the N-Triples specification
	 * except Dot.
	 */
	public static boolean isLiberalCharactersButNotDot(int c) {
		return isUnderscore(c) || c == 45 || c == 183 || (c >= 768 && c <= 879) || c == 8255 || c == 8256;
	}

	/**
	 * Checks whether the supplied character is Underscore.
	 */
	public static boolean isUnderscore(int c) {
		return c == 95;
	}

	/**
	 * Checks whether the supplied character is Dot '.'.
	 */
	public static boolean isDot(int c) {
		return c == 46;
	}

	/**
	 * Escapes a Unicode string to an all-ASCII character sequence. Any special characters are escaped using backslashes
	 * (<tt>"</tt> becomes <tt>\"</tt>, etc.), and non-ascii/non-printable characters are escaped using Unicode escapes
	 * (<tt>&#x5C;uxxxx</tt> and <tt>&#x5C;Uxxxxxxxx</tt>).
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
	 * (<tt>"</tt> becomes <tt>\"</tt>, etc.), and non-ascii/non-printable characters are escaped using Unicode escapes
	 * (<tt>&#x5C;uxxxx</tt> and <tt>&#x5C;Uxxxxxxxx</tt>).
	 * 
	 * @throws IOException
	 */
	public static void escapeString(String label, Appendable appendable) throws IOException {
		escapeString(label, appendable, true);
	}

	/**
	 * Escapes a Unicode string to an N-Triples compatible character sequence. Any special characters are escaped using
	 * backslashes (<tt>"</tt> becomes <tt>\"</tt>, etc.), and non-ascii/non-printable characters are escaped using
	 * Unicode escapes (<tt>&#x5C;uxxxx</tt> and <tt>&#x5C;Uxxxxxxxx</tt>) if the option is selected.
	 * 
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
	 * Unescapes an escaped Unicode string. Any Unicode sequences ( <tt>&#x5C;uxxxx</tt> and <tt>&#x5C;Uxxxxxxxx</tt>)
	 * are restored to the value indicated by the hexadecimal argument and any backslash-escapes ( <tt>\"</tt>,
	 * <tt>\\</tt>, etc.) are decoded to their original form.
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
	 * Converts a decimal value to a hexadecimal string represention of the specified length.
	 * 
	 * @param decimal      A decimal value.
	 * @param stringLength The length of the resulting string.
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
