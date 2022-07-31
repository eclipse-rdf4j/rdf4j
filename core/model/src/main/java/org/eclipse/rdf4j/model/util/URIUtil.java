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
package org.eclipse.rdf4j.model.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.common.text.ASCIIUtil;

/**
 * Utility functions for working with {@link URI URIs}.
 *
 * @author Arjohn Kampman
 */
public class URIUtil {

	/**
	 * Reserved characters: their usage within the URI component is limited to their reserved purpose. If the data for a
	 * URI component would conflict with the reserved purpose, then the conflicting data must be escaped before forming
	 * the URI. http://www.isi.edu/in-notes/rfc2396.txt section 2.2.
	 */
	private static final Set<Character> reserved = new HashSet<>(
			Arrays.asList(new Character[] { ';', '/', '?', ':', '@', '&', '=', '+', '$', ',' }));

	/**
	 * Punctuation mark characters, which are part of the set of unreserved chars and therefore allowed to occur in
	 * unescaped form. See http://www.isi.edu/in-notes/rfc2396.txt
	 */
	private static final Set<Character> mark = new HashSet<>(
			Arrays.asList(new Character[] { '-', '_', '.', '!', '~', '*', '\'', '(', ')' }));

	/**
	 * Regular expression pattern for matching unicode control characters.
	 */
	private static final Pattern unicodeControlCharPattern = Pattern.compile(".*[\u0000-\u001F\u007F-\u009F].*");

	/**
	 * Finds the index of the first local name character in an (non-relative) URI. This index is determined by the
	 * following the following steps:
	 * <ul>
	 * <li>Find the <em>first</em> occurrence of the '#' character,
	 * <li>If this fails, find the <em>last</em> occurrence of the '/' character,
	 * <li>If this fails, find the <em>last</em> occurrence of the ':' character.
	 * <li>Add <var>1<var> to the found index and return this value.
	 * </ul>
	 * Note that the third step should never fail as every legal (non-relative) URI contains at least one ':' character
	 * to seperate the scheme from the rest of the URI. If this fails anyway, the method will throw an
	 * {@link IllegalArgumentException}.
	 *
	 * @param uri A URI string.
	 * @return The index of the first local name character in the URI string. Note that this index does not reference an
	 *         actual character if the algorithm determines that there is not local name. In that case, the return index
	 *         is equal to the length of the URI string.
	 * @throws IllegalArgumentException If the supplied URI string doesn't contain any of the separator characters.
	 *                                  Every legal (non-relative) URI contains at least one ':' character to seperate
	 *                                  the scheme from the rest of the URI.
	 */
	public static int getLocalNameIndex(String uri) {
		int separatorIdx = uri.indexOf('#');

		if (separatorIdx < 0) {
			separatorIdx = uri.lastIndexOf('/');
		}

		if (separatorIdx < 0) {
			separatorIdx = uri.lastIndexOf(':');
		}

		if (separatorIdx < 0) {
			throw new IllegalArgumentException("No separator character founds in URI: " + uri);
		}

		return separatorIdx + 1;
	}

	/**
	 * Checks whether the URI consisting of the specified namespace and local name has been split correctly according to
	 * the URI splitting rules specified in {@link URI}.
	 *
	 * @param namespace The URI's namespace, must not be <var>null</var>.
	 * @param localName The URI's local name, must not be <var>null</var>.
	 * @return <var>true</var> if the specified URI has been correctly split into a namespace and local name,
	 *         <var>false</var> otherwise.
	 * @see URI
	 * @see #getLocalNameIndex(String)
	 */
	public static boolean isCorrectURISplit(String namespace, String localName) {
		assert namespace != null : "namespace must not be null";
		assert localName != null : "localName must not be null";

		if (namespace.length() == 0) {
			return false;
		}

		int nsLength = namespace.length();
		char lastNsChar = namespace.charAt(nsLength - 1);

		if (lastNsChar == '#') {
			// correct split if namespace has no other '#'
			return namespace.lastIndexOf('#', nsLength - 2) == -1 && localName.indexOf('#') == -1;
		} else if (lastNsChar == '/') {
			// correct split if local name has no '/' and URI contains no '#'
			return localName.indexOf('/') == -1 && localName.indexOf('#') == -1 && namespace.indexOf('#') == -1;
		} else if (lastNsChar == ':') {
			// correct split if local name has no ':' and URI contains no '#' or
			// '/'
			return localName.indexOf(':') == -1 && localName.indexOf('#') == -1 && localName.indexOf('/') == -1
					&& namespace.indexOf('#') == -1 && namespace.indexOf('/') == -1;
		}

		return false;
	}

	/**
	 * Verifies that the supplied string is a valid RDF (1.0) URI reference, as defined in
	 * <a href= "http://www.w3.org/TR/2004/REC-rdf-concepts-20040210/#section-Graph-URIref" >section 6.4 of the RDF
	 * Concepts and Abstract Syntax specification</a> (RDF 1.0 Recommendation of February 10, 2004).
	 * <p>
	 * An RDF URI reference is valid if it is a Unicode string that:
	 * <ul>
	 * <li>does not contain any control characters ( #x00 - #x1F, #x7F-#x9F)
	 * <li>and would produce a valid URI character sequence (per RFC2396 , section 2.1) representing an absolute URI
	 * with optional fragment identifier when subjected to the encoding described below
	 * </ul>
	 * The encoding consists of:
	 * <ol>
	 * <li>encoding the Unicode string as UTF-8, giving a sequence of octet values.
	 * <li>%-escaping octets that do not correspond to permitted US-ASCII characters.
	 * </ol>
	 *
	 * @param uriRef a string representing an RDF URI reference.
	 * @return <code>true</code> iff the supplied string is a syntactically valid RDF URI reference, <code>false</code>
	 *         otherwise.
	 * @see <a href="http://www.w3.org/TR/2004/REC-rdf-concepts-20040210/#section-Graph-URIref">section 6.4 of the RDF
	 *      Concepts and Abstract Syntax specification</a>
	 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>
	 * @see <a href="http://www.ietf.org/rfc/rfc2396.txt">RFC 2396</a>
	 */
	public static boolean isValidURIReference(String uriRef) {
		// check that string contains no Unicode control characters.
		boolean valid = !unicodeControlCharPattern.matcher(uriRef).matches();
		if (valid) {
			// check that proper encoding/escaping would yield a valid absolute
			// RFC 2396 URI
			final String escaped = escapeExcludedChars(uriRef);
			try {
				/*
				 * NOTE we use java.net.URI parsing to check compliance to the RFC, which is almost, but not completely,
				 * in alignment with RFC 2396, and has not been updated for compatibility with RFC 3986. See the
				 * java.net.URI javadoc ( https://docs.oracle.com/javase/8/docs/api/java/net/URI.html ) for details."
				 */
				final java.net.URI uri = new java.net.URI(escaped);
				valid = uri.isAbsolute();
			} catch (URISyntaxException e) {
				valid = false;
			}
		}

		return valid;
	}

	/**
	 * Escapes any character that is not either reserved or in the legal range of unreserved characters, according to
	 * RFC 2396.
	 *
	 * @param unescaped a (relative or absolute) uri reference.
	 * @return a (relative or absolute) uri reference with all characters that can not appear as-is in a URI %-escaped.
	 * @see <a href="http://www.ietf.org/rfc/rfc2396.txt">RFC 2396</a>
	 */
	private static String escapeExcludedChars(String unescaped) {
		final StringBuilder escaped = new StringBuilder();
		for (int i = 0; i < unescaped.length(); i++) {
			char c = unescaped.charAt(i);
			if (!isUnreserved(c) && !reserved.contains(c)) {
				escaped.append("%" + Integer.toHexString((int) c));
			} else {
				escaped.append(c);
			}
		}
		return escaped.toString();
	}

	/**
	 * A character is unreserved according to RFC 2396 if it is either an alphanumeric char or a punctuation mark.
	 */
	private static boolean isUnreserved(char c) {
		final int n = (int) c;
		// check if alphanumeric
		boolean unreserved = (47 < n && n < 58) || (96 < n && n < 123) || (64 < n && n < 91);
		if (!unreserved) {
			// check if punctuation mark
			unreserved = mark.contains(c);
		}

		return unreserved;
	}

	/**
	 * Checks whether the specified name is allowed as the local name part of an IRI according to the SPARQL 1.1/Turtle
	 * 1.1 spec.
	 *
	 * @param name the candidate local name
	 * @return true if it is a local name
	 */
	public static boolean isValidLocalName(String name) {
		// Empty names are legal
		if (name.length() == 0) {
			return true;
		}

		if (!isPN_CHARS_U(name.charAt(0)) && name.charAt(0) != ':' && !ASCIIUtil.isNumber(name.charAt(0))
				&& !isPLX_START(name)) {
			return false;
		}

		if (!isNameStartChar(name.charAt(0))) {
			return false;
		}

		for (int i = 1; i < name.length(); i++) {
			if (!isNameChar(name.charAt(i))) {
				return false;
			}

			// Check if the percent encoding was less than two characters from the
			// end of the prefix, in which case it is invalid
			if (name.charAt(i) == '%' && (name.length() - i) < 3) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Check if the supplied code point represents either a valid prefixed name base character or an underscore.
	 * <p>
	 * From Turtle Spec:
	 * <p>
	 * http://www.w3.org/TR/turtle/#grammar-production-PN_CHARS_U
	 * <p>
	 * [164s] PN_CHARS_U ::= PN_CHARS_BASE | '_'
	 */
	private static boolean isPN_CHARS_U(int codePoint) {
		return isPN_CHARS_BASE(codePoint) || codePoint == '_';
	}

	private static boolean isPLX_START(String name) {
		if (name.length() >= 3 && isPERCENT(name.substring(0, 3))) {
			return true;
		}

		if (name.length() >= 2 && isPN_LOCAL_ESC(name.substring(0, 2))) {
			return true;
		}

		return false;
	}

	private static boolean isPERCENT(String name) {
		if (name.length() != 3) {
			return false;
		}

		if (name.charAt(0) != '%') {
			return false;
		}

		if (!ASCIIUtil.isHex(name.charAt(1)) || !ASCIIUtil.isHex(name.charAt(2))) {
			return false;
		}

		return true;
	}

	private static boolean isPN_LOCAL_ESC(String name) {
		if (name.length() != 2) {
			return false;
		}

		if (!name.startsWith("\\")) {
			return false;
		}

		if (!(Arrays.binarySearch(LOCAL_ESCAPED_CHARS, name.charAt(1)) > -1)) {
			return false;
		}

		return true;
	}

	private static final char[] LOCAL_ESCAPED_CHARS = new char[] { '_', '~', '.', '-', '!', '$', '&', '\'', '(', ')',
			'*', '+', ',', ';', '=', '/', '?', '#', '@', '%' };

	/**
	 * Check if the supplied code point represents a valid prefixed name base character.
	 * <p>
	 * From Turtle Spec:
	 * <p>
	 * http://www.w3.org/TR/turtle/#grammar-production-PN_CHARS_BASE
	 * <p>
	 * [163s] PN_CHARS_BASE ::= [A-Z] | [a-z] | [#x00C0-#x00D6] | [#x00D8-#x00F6] | [#x00F8-#x02FF] | [#x0370-#x037D] |
	 * [#x037F-#x1FFF] | [#x200C-#x200D] | [#x2070-#x218F] | [#x2C00-#x2FEF] | [#x3001-#xD7FF] | [#xF900-#xFDCF] |
	 * [#xFDF0-#xFFFD] | [#x10000-#xEFFFF]
	 */
	private static boolean isPN_CHARS_BASE(int codePoint) {
		return ASCIIUtil.isLetter(codePoint) || codePoint >= 0x00C0 && codePoint <= 0x00D6
				|| codePoint >= 0x00D8 && codePoint <= 0x00F6 || codePoint >= 0x00F8 && codePoint <= 0x02FF
				|| codePoint >= 0x0370 && codePoint <= 0x037D || codePoint >= 0x037F && codePoint <= 0x1FFF
				|| codePoint >= 0x200C && codePoint <= 0x200D || codePoint >= 0x2070 && codePoint <= 0x218F
				|| codePoint >= 0x2C00 && codePoint <= 0x2FEF || codePoint >= 0x3001 && codePoint <= 0xD7FF
				|| codePoint >= 0xF900 && codePoint <= 0xFDCF || codePoint >= 0xFDF0 && codePoint <= 0xFFFD
				|| codePoint >= 0x10000 && codePoint <= 0xEFFFF;
	}

	/**
	 * Check if the supplied code point represents a valid name start character.
	 *
	 * @param codePoint a Unicode code point.
	 * @return <code>true</code> if the supplied code point represents a valid name start char, <code>false</code>
	 *         otherwise.
	 */
	private static boolean isNameStartChar(int codePoint) {
		return isPN_CHARS_U(codePoint) || codePoint == ':' || ASCIIUtil.isNumber(codePoint) || codePoint == '\\'
				|| codePoint == '%';
	}

	/**
	 * Check if the supplied code point represents a valid name character.
	 *
	 * @param codePoint a Unicode code point.
	 * @return <code>true</code> if the supplied code point represents a valid name char, <code>false</code> otherwise.
	 */
	private static boolean isNameChar(int codePoint) {
		return isPN_CHARS(codePoint) || codePoint == '.' || codePoint == ':' | codePoint == '\\' || codePoint == '%';
	}

	/**
	 * Check if the supplied code point represents a valid prefixed name character.
	 * <p>
	 * From Turtle Spec:
	 * <p>
	 * http://www.w3.org/TR/turtle/#grammar-production-PN_CHARS
	 * <p>
	 * [166s] PN_CHARS ::= PN_CHARS_U | '-' | [0-9] | #x00B7 | [#x0300-#x036F] | [#x203F-#x2040]
	 */
	private static boolean isPN_CHARS(int codePoint) {
		return isPN_CHARS_U(codePoint) || ASCIIUtil.isNumber(codePoint) || codePoint == '-' || codePoint == 0x00B7
				|| codePoint >= 0x0300 && codePoint <= 0x036F || codePoint >= 0x203F && codePoint <= 0x2040;
	}

}
