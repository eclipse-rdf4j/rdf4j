/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.util;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.model.URI;

/**
 * Utility functions for working with {@link URI URIs}.
 * @author Arjohn Kampman
 */
public class URIUtil {

	/**
	 * Reserved characters: their usage within the URI component is limited to
	 * their reserved purpose. If the data for a URI component would conflict
	 * with the reserved purpose, then the conflicting data must be escaped
	 * before forming the URI. http://www.isi.edu/in-notes/rfc2396.txt section
	 * 2.2.
	 */
	private static final Set<Character> reserved = new HashSet<Character>(Arrays.asList(new Character[] {
			';',
			'/',
			'?',
			':',
			'@',
			'&',
			'=',
			'+',
			'$',
			',' }));

	/**
	 * Punctuation mark characters, which are part of the set of unreserved chars
	 * and therefore allowed to occur in unescaped form. See
	 * http://www.isi.edu/in-notes/rfc2396.txt
	 */
	private static final Set<Character> mark = new HashSet<Character>(Arrays.asList(new Character[] {
			'-',
			'_',
			'.',
			'!',
			'~',
			'*',
			'\'',
			'(',
			')' }));

	/**
	 * Finds the index of the first local name character in an (non-relative)
	 * URI. This index is determined by the following the following steps:
	 * <ul>
	 * <li>Find the <em>first</em> occurrence of the '#' character,
	 * <li>If this fails, find the <em>last</em> occurrence of the '/' character,
	 * <li>If this fails, find the <em>last</em> occurrence of the ':' character.
	 * <li>Add <tt>1<tt> to the found index and return this value.
	 * </ul>
	 * Note that the third step should never fail as every legal (non-relative)
	 * URI contains at least one ':' character to seperate the scheme from the
	 * rest of the URI. If this fails anyway, the method will throw an
	 * {@link IllegalArgumentException}.
	 * 
	 * @param uri
	 *        A URI string.
	 * @return The index of the first local name character in the URI string.
	 *         Note that this index does not reference an actual character if the
	 *         algorithm determines that there is not local name. In that case,
	 *         the return index is equal to the length of the URI string.
	 * @throws IllegalArgumentException
	 *         If the supplied URI string doesn't contain any of the separator
	 *         characters. Every legal (non-relative) URI contains at least one
	 *         ':' character to seperate the scheme from the rest of the URI.
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
	 * Checks whether the URI consisting of the specified namespace and local
	 * name has been split correctly according to the URI splitting rules
	 * specified in {@link URI}.
	 * 
	 * @param namespace
	 *        The URI's namespace, must not be <tt>null</tt>.
	 * @param localName
	 *        The URI's local name, must not be <tt>null</tt>.
	 * @return <tt>true</tt> if the specified URI has been correctly split into a
	 *         namespace and local name, <tt>false</tt> otherwise.
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
			return namespace.lastIndexOf('#', nsLength - 2) == -1;
		}
		else if (lastNsChar == '/') {
			// correct split if local name has no '/' and URI contains no '#'
			return localName.indexOf('/') == -1 && localName.indexOf('#') == -1 && namespace.indexOf('#') == -1;
		}
		else if (lastNsChar == ':') {
			// correct split if local name has no ':' and URI contains no '#' or
			// '/'
			return localName.indexOf(':') == -1 && localName.indexOf('#') == -1 && localName.indexOf('/') == -1
					&& namespace.indexOf('#') == -1 && namespace.indexOf('/') == -1;
		}

		return false;
	}

	/**
	 * Verifies that the supplied string is a valid RDF (1.0) URI reference, as
	 * defined in <a href=
	 * "http://www.w3.org/TR/2004/REC-rdf-concepts-20040210/#section-Graph-URIref"
	 * >section 6.4 of the RDF Concepts and Abstract Syntax specification</a>
	 * (RDF 1.0 Recommendation of February 10, 2004).
	 * <p>
	 * An RDF URI reference is valid if it is a Unicode string that:
	 * <ul>
	 * <li>does not contain any control characters ( #x00 - #x1F, #x7F-#x9F)
	 * <li>and would produce a valid URI character sequence (per RFC2396 ,
	 * section 2.1) representing an absolute URI with optional fragment
	 * identifier when subjected to the encoding described below
	 * </ul>
	 * The encoding consists of:
	 * <ol>
	 * <li>encoding the Unicode string as UTF-8, giving a sequence of octet
	 * values.
	 * <li>%-escaping octets that do not correspond to permitted US-ASCII
	 * characters.
	 * </ol>
	 * 
	 * @param uriRef
	 *        a string representing an RDF URI reference.
	 * @return <code>true</code> iff the supplied string is a syntactically valid
	 *         RDF URI reference, <code>false</code> otherwise.
	 * @see <a
	 *      href="http://www.w3.org/TR/2004/REC-rdf-concepts-20040210/#section-Graph-URIref">section
	 *      6.4 of the RDF Concepts and Abstract Syntax specification</a>
	 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>
	 * @see <a href="http://www.ietf.org/rfc/rfc2396.txt">RFC 2396</a>
	 * @since 2.7.14
	 */
	public static boolean isValidURIReference(String uriRef) {

		// check that string contains no Unicode control characters.
		boolean valid = !uriRef.matches("[\u0000-\u001F\u007F-\u009F]");
		if (valid) {
			// check that proper encoding/escaping would yield a valid absolute
			// RFC 2396 URI
			final String escaped = escapeExcludedChars(uriRef);
			try {
				/* 
				 * NOTE we use java.net.URI parsing to check compliance to the RFC, which is almost, but not completely, in alignment
				 * with RFC 2396, and has not been updated for compatibility with RFC 3986. From the java.net.URI Javadoc: 
				 * 
				 * "This constructor parses the given string exactly as specified by the grammar in RFC 2396, Appendix A, 
				 * except for the following deviations:
				 * 
				 * - An empty authority component is permitted as long as it is followed by a non-empty path, a query component, 
				 *   or a fragment component. This allows the parsing of URIs such as "file:///foo/bar", which seems to be the 
				 *   intent of RFC 2396 although the grammar does not permit it. If the authority component is empty then the 
				 *   user-information, host, and port components are undefined.
				 * - Empty relative paths are permitted; this seems to be the intent of RFC 2396 although the grammar does not 
				 *   permit it. The primary consequence of this deviation is that a standalone fragment such as "#foo" parses as 
				 *   a relative URI with an empty path and the given fragment, and can be usefully resolved against a base URI.
				 * - IPv4 addresses in host components are parsed rigorously, as specified by RFC 2732: Each element of a dotted-quad 
				 *   address must contain no more than three decimal digits. Each element is further constrained to have a value no 
				 *   greater than 255.
				 *  - Hostnames in host components that comprise only a single domain label are permitted to start with an alphanum 
				 *    character. This seems to be the intent of RFC 2396 section 3.2.2 although the grammar does not permit it. The 
				 *    consequence of this deviation is that the authority component of a hierarchical URI such as s://123, will parse 
				 *    as a server-based authority.
				 *  - IPv6 addresses are permitted for the host component. An IPv6 address must be enclosed in square brackets 
				 *    ('[' and ']') as specified by RFC 2732. The IPv6 address itself must parse according to RFC 2373. IPv6 addresses 
				 *    are further constrained to describe no more than sixteen bytes of address information, a constraint implicit in 
				 *    RFC 2373 but not expressible in the grammar.
				 *  - Characters in the /other/ category are permitted wherever RFC 2396 permits escaped octets, that is, in the user-information, 
				 *     path, query, and fragment components, as well as in the authority component if the authority is registry-based. 
				 *     This allows URIs to contain Unicode characters beyond those in the US-ASCII character set."
				 */
				final java.net.URI uri = new java.net.URI(escaped);
				valid = uri.isAbsolute();
			}
			catch (URISyntaxException e) {
				valid = false;
			}
		}

		return valid;
	}

	/**
	 * Escapes any character that is not either reserved or in the legal range of
	 * unreserved characters, according to RFC 2396.
	 * 
	 * @param unescaped
	 *        a (relative or absolute) uri reference.
	 * @return a (relative or absolute) uri reference with all characters that
	 *         can not appear as-is in a URI %-escaped.
	 * @see <a href="http://www.ietf.org/rfc/rfc2396.txt">RFC 2396</a>
	 */
	private static String escapeExcludedChars(String unescaped) {
		final StringBuilder escaped = new StringBuilder();
		for (int i = 0; i < unescaped.length(); i++) {
			char c = unescaped.charAt(i);
			if (!isUnreserved(c) && !reserved.contains(c)) {
				escaped.append("%" + Integer.toHexString((int)c));
			}
			else {
				escaped.append(c);
			}
		}
		return escaped.toString();
	}

	/**
	 * A character is unreserved according to RFC 2396 if it is either an
	 * alphanumeric char or a punctuation mark.
	 */
	private static boolean isUnreserved(char c) {
		final int n = (int)c;
		// check if alphanumeric
		boolean unreserved = (47 < n && n < 58) || (96 < n && n < 123) || (64 < n && n < 91);
		if (!unreserved) {
			// check if punctuation mark
			unreserved = mark.contains(c);
		}

		return unreserved;
	}
}
