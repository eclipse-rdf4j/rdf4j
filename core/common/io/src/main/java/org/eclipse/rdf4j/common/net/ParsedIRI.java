/*******************************************************************************
 * Copyright (c) 2017 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.common.net;

import java.io.Serializable;
import java.net.IDN;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents an Internationalized Resource Identifier (IRI) reference.
 * <p>
 * Aside from some minor deviations noted below, an instance of this class represents a IRI reference as defined by
 * <a href="http://www.ietf.org/rfc/rfc3987.txt"><i>RFC&nbsp;3987: Internationalized Resource Identifiers (IRI): IRI
 * Syntax</i></a>. This class provides constructors for creating IRI instances from their components or by parsing their
 * string forms, methods for accessing the various components of an instance, and methods for normalizing, resolving,
 * and relativizing IRI instances. Instances of this class are immutable.
 * <p>
 * An IRI instance has the following seven components in string form has the syntax <blockquote>
 * [<i>scheme</i><b>{@code :}</b>][<b>{@code //}</b>[<i>user-info</i><b>{@code @}</b>]<i>host</i>[<b>{@code :}</b><i>port</i>]][<i>path</i>][<b>{@code ?}</b><i>query</i>][<b>{@code #}</b><i>fragment</i>]
 * </blockquote>
 * <p>
 * In a given instance any particular component is either <i>undefined</i> or <i>defined</i> with a distinct value.
 * Undefined string components are represented by {@code null}, while undefined integer components are represented by
 * {@code -1}. A string component may be defined to have the empty string as its value; this is not equivalent to that
 * component being undefined.
 * <p>
 * Whether a particular component is or is not defined in an instance depends upon the type of the IRI being
 * represented. An absolute IRI has a scheme component. An opaque IRI has a scheme, a scheme-specific part, and possibly
 * a fragment, but has no other components. A hierarchical IRI always has a path (though it may be empty) and a
 * scheme-specific-part (which at least contains the path), and may have any of the other components.
 * <h4>IRIs, URIs, URLs, and URNs</h4>
 * <p>
 * IRIs are meant to replace URIs in identifying resources for protocols, formats, and software components that use a
 * UCS-based character repertoire.
 * <p>
 * Internationalized Resource Identifier (IRI) is a complement to the Uniform Resource Identifier (URI). An IRI is a
 * sequence of characters from the Universal Character Set (Unicode/ISO 10646). A mapping from IRIs to URIs is defined
 * using {@link #toASCIIString()}, which means that IRIs can be used instead of URIs, where appropriate, to identify
 * resources. While all URIs are also IRIs, the {@link #normalize()} method can be used to convert a URI back into a
 * normalized IRI.
 * <p>
 * A URI is a uniform resource <i>identifier</i> while a URL is a uniform resource <i>locator</i>. Hence every URL is a
 * URI, abstractly speaking, but not every URI is a URL. This is because there is another subcategory of URIs, uniform
 * resource <i>names</i> (URNs), which name resources but do not specify how to locate them. The {@code mailto},
 * {@code news}, and {@code isbn} URIs shown above are examples of URNs.
 * <h4>Deviations</h4>
 * <p>
 * <b>jar:</b> This implementation treats the first colon as part of the scheme if the scheme starts with "jar:". For
 * example the IRI <i>jar:http://www.foo.com/bar/jar.jar!/baz/entry.txt</i> is parsed with the scheme <i>jar:http</i>
 * and the path <i>/bar/jar.jar!/baz/entry.txt</i>.
 *
 * @author James Leigh
 * @since 2.3
 * @see <a href="http://www.ietf.org/rfc/rfc3987.txt"><i>RFC&nbsp;3987: Internationalized Resource Identifiers
 *      (IRIs)</i></a>
 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt"><i>RFC&nbsp;3986: Uniform Resource Identifiers (URI): Generic
 *      Syntax</i></a>
 */
public class ParsedIRI implements Cloneable, Serializable {

	private static final long serialVersionUID = -5681843777254402303L;

	private static final Comparator<int[]> CMP = (int[] o1, int[] o2) -> o1[0] - o2[0];

	private static final int EOF = 0;

	private static final int[][] iprivate = { new int[] { 0xE000, 0xF8FF }, new int[] { 0xF0000, 0xFFFFD },
			new int[] { 0x100000, 0x10FFFD } };

	private static final int[][] ucschar = { new int[] { 0x00A0, 0xD7FF }, new int[] { 0xF900, 0xFDCF },
			new int[] { 0xFDF0, 0xFFEF }, new int[] { 0x10000, 0x1FFFD }, new int[] { 0x20000, 0x2FFFD },
			new int[] { 0x30000, 0x3FFFD }, new int[] { 0x40000, 0x4FFFD }, new int[] { 0x50000, 0x5FFFD },
			new int[] { 0x60000, 0x6FFFD }, new int[] { 0x70000, 0x7FFFD }, new int[] { 0x80000, 0x8FFFD },
			new int[] { 0x90000, 0x9FFFD }, new int[] { 0xA0000, 0xAFFFD }, new int[] { 0xB0000, 0xBFFFD },
			new int[] { 0xC0000, 0xCFFFD }, new int[] { 0xD0000, 0xDFFFD }, new int[] { 0xE1000, 0xEFFFD } };

	private static final int[][] ALPHA = { new int[] { 'A', 'Z' }, new int[] { 'a', 'z' } };

	private static final int[][] DIGIT = { new int[] { '0', '9' } };

	private static final int[][] sub_delims = union('!', '$', '&', '\'', '(', ')', '*', '+', ',', ';', '=');

	private static final int[][] gen_delims = union(':', '/', '?', '#', '[', ']', '@');

	private static final int[][] reserved = union(gen_delims, sub_delims);

	private static final int[][] unreserved_rfc3986 = union(ALPHA, DIGIT, '-', '.', '_', '~');

	private static final int[][] unreserved = union(unreserved_rfc3986, ucschar);

	private static final int[][] schar = union(ALPHA, DIGIT, '+', '-', '.');

	private static final int[][] uchar = union(unreserved, sub_delims, ':');

	private static final int[][] hchar = union(unreserved, sub_delims);

	private static final int[][] pchar = union(unreserved, sub_delims, ':', '@');

	private static final int[][] qchar = union(pchar, iprivate, '/', '?');

	private static final int[][] fchar = union(pchar, '/', '?');

	private static final int[] HEXDIG = flatten(
			union(DIGIT, new int[][] { new int[] { 'A', 'F' }, new int[] { 'a', 'f' } }));

	private static final int[] ascii = flatten(union(unreserved_rfc3986, reserved, '%'));

	private static final int[] common = flatten(
			union(unreserved_rfc3986, reserved, '%', '<', '>', '"', ' ', '{', '}', '|', '\\', '^', '`'));

	private static final String[] common_pct = pctEncode(common);

	private static int[][] union(Object... sets) {
		List<int[]> list = new ArrayList<>();
		for (Object set : sets) {
			if (set instanceof int[][]) {
				int[][] ar = (int[][]) set;
				list.addAll(Arrays.asList(ar));
			} else if (set instanceof Character) {
				char chr = (Character) set;
				list.add(new int[] { chr, chr });
			} else {
				throw new IllegalStateException();
			}
		}
		int[][] dest = list.toArray(new int[][] {});
		Arrays.sort(dest, CMP);
		return dest;
	}

	private static int[] flatten(int[]... arrays) {
		List<Integer> list = new ArrayList<>();
		for (int[] str : arrays) {
			if (str.length == 1) {
				list.add(str[0]); // character
			} else if (str.length == 2) {
				for (int chr = str[0], end = str[1]; chr <= end; chr++) {
					list.add(chr); // range
				}
			} else {
				throw new IllegalStateException();
			}
		}
		int[] chars = new int[list.size()];
		for (int i = 0; i < chars.length; i++) {
			chars[i] = list.get(i);
		}
		Arrays.sort(chars);
		return chars;
	}

	private static String[] pctEncode(int[] unencoded) {
		CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder();
		String[] result = new String[unencoded.length];
		for (int i = 0; i < unencoded.length; i++) {
			String ns = new String(Character.toChars(unencoded[i]));
			ByteBuffer bb;
			try {
				bb = encoder.encode(CharBuffer.wrap(ns));
			} catch (CharacterCodingException x) {
				throw new IllegalStateException();
			}
			StringBuilder sb = new StringBuilder();
			while (bb.hasRemaining()) {
				byte b = (byte) (bb.get() & 0xff);
				sb.append('%');
				sb.appendCodePoint(HEXDIG[(b >> 4) & 0x0f]);
				sb.appendCodePoint(HEXDIG[(b) & 0x0f]);
			}
			result[i] = sb.toString();
		}
		return result;
	}

	/**
	 * Creates a ParsedIRI by parsing the given string.
	 * <p>
	 * This convenience factory method works as if by invoking the {@link #ParsedIRI(String)} constructor; any
	 * {@link URISyntaxException} thrown by the constructor is caught and the error code point is percent encoded. This
	 * process is repeated until a syntactically valid IRI is formed or a {@link IllegalArgumentException} is thrown.
	 * <p>
	 * This method is provided for use in situations where it is known that the given string is an IRI, even if it is
	 * not completely syntactically valid, for example a IRI constants declared within in a program. The constructors,
	 * which throw {@link URISyntaxException} directly, should be used situations where a IRI is being constructed from
	 * user input or from some other source that may be prone to errors.
	 * </p>
	 *
	 * @param str The string to be parsed into an IRI
	 * @return The new ParsedIRI
	 * @throws NullPointerException     If {@code str} is {@code null}
	 * @throws IllegalArgumentException If the given string could not be converted into an IRI
	 */
	public static ParsedIRI create(String str) {
		try {
			return new ParsedIRI(str);
		} catch (URISyntaxException x) {
			int problem = x.getIndex();
			StringBuilder sb = new StringBuilder(str);
			while (true) {
				int end = sb.offsetByCodePoints(problem, 1);
				String decoded = sb.substring(problem, end);
				String encoded = " ".equals(decoded) ? "%20" : URLEncoder.encode(decoded, StandardCharsets.UTF_8);
				sb.replace(problem, end, encoded);
				try {
					return new ParsedIRI(sb.toString());
				} catch (URISyntaxException ex) {
					if (ex.getIndex() <= problem) {
						throw new IllegalArgumentException(x.getMessage(), x);
					} else {
						problem = ex.getIndex();
					}
				}
			}

		}
	}

	private final String iri;

	private int pos;

	private String scheme;

	private String userInfo;

	private String host;

	private int port = -1;

	private String path;

	private String query;

	private String fragment;

	/**
	 * Constructs a ParsedIRI by parsing the given string.
	 *
	 * @param iri The string to be parsed into a IRI
	 * @throws NullPointerException If {@code iri} is {@code null}
	 * @throws URISyntaxException   If the given string violates RFC&nbsp;3987, as augmented by the above deviations
	 */
	public ParsedIRI(String iri) throws URISyntaxException {
		assert iri != null;
		this.iri = iri;
		parse();
	}

	/**
	 * Constructs a hierarchical IRI from the given components.
	 * <p>
	 * This constructor first builds a IRI string from the given components according to the rules specified in
	 * <a href="http://www.ietf.org/rfc/rfc3987.txt">RFC&nbsp;3987</a>
	 * </p>
	 *
	 * @param scheme   Scheme name
	 * @param userInfo User name and authorization information
	 * @param host     Host name
	 * @param port     Port number
	 * @param path     Path
	 * @param query    Query
	 * @param fragment Fragment
	 */
	public ParsedIRI(String scheme, String userInfo, String host, int port, String path, String query,
			String fragment) {
		this.iri = buildIRI(scheme, userInfo, host, port, path, query, fragment);
		this.scheme = scheme;
		this.userInfo = userInfo;
		this.host = host;
		this.port = port;
		this.path = path == null ? "" : path;
		this.query = query;
		this.fragment = fragment;
	}

	@Override
	public int hashCode() {
		return iri.hashCode();
	}

	/**
	 * Tests this IRI for simple string comparison with another object.
	 * <p>
	 * If two IRI strings are identical, then it is safe to conclude that they are equivalent. However, even if the IRI
	 * strings are not identical the IRIs might still be equivalent. Further comparison can be made using the
	 * {@link #normalize()} forms.
	 *
	 * @param obj The object to which this object is to be compared
	 * @return {@code true} if the given object is a ParsedIRI that represents the same IRI
	 */
	@Override
	public boolean equals(Object obj) {
		return obj instanceof ParsedIRI && this.iri.equals(((ParsedIRI) obj).iri);
	}

	/**
	 * Returns the content of this IRI as a string.
	 * <p>
	 * If this URI was created by invoking one of the constructors in this class then a string equivalent to the
	 * original input string, or to the string computed from the originally-given components, as appropriate, is
	 * returned. Otherwise this IRI was created by normalization, resolution, or relativization, and so a string is
	 * constructed from this IRI's components according to the rules specified in
	 * <a href="http://www.ietf.org/rfc/rfc3987.txt">RFC&nbsp;3987</a>
	 * </p>
	 *
	 * @return The string form of this IRI
	 */
	@Override
	public String toString() {
		return iri;
	}

	/**
	 * Returns the content of this IRI as a US-ASCII string.
	 * <p>
	 * If this IRI only contains 8bit characters then an invocation of this method will return the same value as an
	 * invocation of the {@link #toString() toString} method. Otherwise this method works as if by encoding the host via
	 * <a href="http://www.ietf.org/rfc/rfc3490.txt">RFC 3490</a> and all other components by percent encoding their
	 * UTF-8 values.
	 * </p>
	 *
	 * @return The string form of this IRI, encoded as needed so that it only contains characters in the US-ASCII
	 *         charset
	 */
	public String toASCIIString() {
		StringBuilder sb = new StringBuilder(iri.length());
		if (scheme != null) {
			sb.append(scheme).append(':');
		}
		if (host != null) {
			sb.append("//");
			if (userInfo != null) {
				appendAscii(sb, userInfo);
				sb.append('@');
			}
			if (host.length() > 0) {
				sb.append(IDN.toASCII(host, IDN.ALLOW_UNASSIGNED));
			}
			if (port >= 0) {
				sb.append(':').append(port);
			}
		}
		if (path != null) {
			appendAscii(sb, path);
		}
		if (query != null) {
			sb.append('?');
			appendAscii(sb, query);
		}
		if (fragment != null) {
			sb.append('#');
			appendAscii(sb, fragment);
		}
		return sb.toString();
	}

	/**
	 * Tells whether or not this IRI is absolute.
	 *
	 * @return {@code true} if, and only if, this IRI has a scheme component
	 */
	public boolean isAbsolute() {
		return scheme != null;
	}

	/**
	 * Tells whether or not this IRI is opaque.
	 * <p>
	 * A IRI is opaque if, and only if, it is absolute and its path part does not begin with a slash character ('/'). An
	 * opaque IRI has a scheme, a path, and possibly a query or fragment; all other components (userInfo, host, and
	 * port) are undefined.
	 * </p>
	 *
	 * @return {@code true} if, and only if, this IRI is absolute and its path does not start with a slash
	 */
	public boolean isOpaque() {
		return scheme != null && !path.isEmpty() && !path.startsWith("/");
	}

	/**
	 * Returns the scheme component of this IRI.
	 * <p>
	 * The scheme component of a IRI, if defined, only contains characters in the <i>alphanum</i> category and in the
	 * string {@code "-.+"}, unless the scheme starts with {@code "jar:"}, in which case it may also contain one colon.
	 * A scheme always starts with an <i>alpha</i> character.
	 * <p>
	 * The scheme component of a IRI cannot contain escaped octets.
	 *
	 * @return The scheme component of this IRI, or {@code null} if the scheme is undefined
	 */
	public String getScheme() {
		return scheme;
	}

	/**
	 * Returns the raw user-information component of this IRI.
	 *
	 * @return The raw user-information component of this IRI, or {@code null} if the user information is undefined
	 */
	public String getUserInfo() {
		return userInfo;
	}

	/**
	 * Returns the host component of this IRI.
	 *
	 * @return The host component of this IRI, or {@code null} if the host is undefined
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Returns the port number of this IRI.
	 * <p>
	 * The port component of a IRI, if defined, is a non-negative integer.
	 * </p>
	 *
	 * @return The port component of this IRI, or {@code -1} if the port is undefined
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Returns the raw path component of this IRI.
	 *
	 * @return The path component of this IRI (never {@code null})
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Returns the raw query component of this IRI after the first question mark.
	 * <p>
	 * The query component of a IRI, if defined, only contains legal IRI characters.
	 * </p>
	 *
	 * @return The raw query component of this IRI, or {@code null} if the IRI does not contain a question mark
	 */
	public String getQuery() {
		return query;
	}

	/**
	 * Returns the raw fragment component of this IRI after the hash.
	 * <p>
	 * The fragment component of a IRI, if defined, only contains legal IRI characters and does not contain a hash.
	 * </p>
	 *
	 * @return The raw fragment component of this IRI, or {@code null} if the IRI does not contain a hash
	 */
	public String getFragment() {
		return fragment;
	}

	/**
	 * Normalizes this IRI's components.
	 * <p>
	 * Because IRIs exist to identify resources, presumably they should be considered equivalent when they identify the
	 * same resource. However, this definition of equivalence is not of much practical use, as there is no way for an
	 * implementation to compare two resources unless it has full knowledge or control of them. Therefore, IRI
	 * normalization is designed to minimize false negatives while strictly avoiding false positives.
	 * <p>
	 * <b>Case Normalization</b> the hexadecimal digits within a percent-encoding triplet (e.g., "%3a" versus "%3A") are
	 * case-insensitive and are normalized to use uppercase letters for the digits A - F. The scheme and host are case
	 * insensitive and are normalized to lowercase.
	 * <p>
	 * <b>Character Normalization</b> The Unicode Standard defines various equivalences between sequences of characters
	 * for various purposes. Unicode Standard Annex defines various Normalization Forms for these equivalences and is
	 * applied to the IRI components.
	 * <p>
	 * <b>Percent-Encoding Normalization</b> decodes any percent-encoded octet sequence that corresponds to an
	 * unreserved character anywhere in the IRI.
	 * <p>
	 * <b>Path Segment Normalization</b> is the process of removing unnecessary {@code "."} and {@code ".."} segments
	 * from the path component of a hierarchical IRI. Each {@code "."} segment is simply removed. A {@code ".."} segment
	 * is removed only if it is preceded by a non-{@code ".."} segment or the start of the path.
	 * <p>
	 * <b>HTTP(S) Scheme Normalization</b> if the port uses the default port number or not given it is set to undefined.
	 * An empty path is replaced with "/".
	 * <p>
	 * <b>File Scheme Normalization</b> if the host is "localhost" or empty it is set to undefined.
	 * <p>
	 * <b>Internationalized Domain Name Normalization</b> of the host component to Unicode.
	 *
	 * @return normalized IRI
	 */
	public ParsedIRI normalize() {
		String _scheme = toLowerCase(scheme);
		boolean optionalPort = isScheme("http") && 80 == port || isScheme("https") && 443 == port;
		int _port = optionalPort ? -1 : port;
		boolean localhost = isScheme("file") && userInfo == null && -1 == port
				&& ("".equals(host) || "localhost".equals(host));
		String _host = localhost ? null
				: host == null || host.length() == 0 ? host
						: IDN.toUnicode(pctEncodingNormalization(toLowerCase(host)),
								IDN.USE_STD3_ASCII_RULES | IDN.ALLOW_UNASSIGNED);
		String _path = _scheme != null && path == null ? "" : normalizePath(path);
		String _userInfo = pctEncodingNormalization(userInfo);
		String _query = pctEncodingNormalization(query);
		String _fragment = pctEncodingNormalization(fragment);
		ParsedIRI normalized = new ParsedIRI(_scheme, _userInfo, _host, _port, _path, _query, _fragment);
		if (this.iri.equals(normalized.iri)) {
			return this;
		} else {
			return normalized;
		}
	}

	/**
	 * Resolves the given IRI against this ParsedIRI.
	 *
	 * @see #resolve(ParsedIRI)
	 * @param iri The IRI to be resolved against this ParsedIRI
	 * @return The resulting IRI
	 * @throws NullPointerException If {@code relative} is {@code null}
	 */
	public String resolve(String iri) {
		return resolve(ParsedIRI.create(iri)).toString();
	}

	/**
	 * Resolves the given IRI against this ParsedIRI.
	 * <p>
	 * <i>Resolution</i> is the process of resolving one IRI against another, <i>base</i> IRI. The resulting IRI is
	 * constructed from components of both IRIs in the manner specified by RFC&nbsp;3986, taking components from the
	 * base IRI for those not specified in the original. For hierarchical IRIs, the path of the original is resolved
	 * against the path of the base and then normalized.
	 * <p>
	 * If the given IRI is already absolute, or if this IRI is opaque, then the given IRI is returned.
	 * <p>
	 * <a name="resolve-frag"></a> If the given URI's fragment component is defined, its path component is empty, and
	 * its scheme, authority, and query components are undefined, then a URI with the given fragment but with all other
	 * components equal to those of this URI is returned. This allows an IRI representing a standalone fragment
	 * reference, such as {@code "#foo"}, to be usefully resolved against a base IRI.
	 * <p>
	 * Otherwise this method constructs a new hierarchical IRI in a manner consistent with
	 * <a href="http://www.ietf.org/rfc/rfc3987.txt">RFC&nbsp;3987</a>
	 * </p>
	 * <p>
	 * The result of this method is absolute if, and only if, either this IRI is absolute or the given IRI is absolute.
	 * </p>
	 *
	 * @param relative The IRI to be resolved against this ParsedIRI
	 * @return The resulting IRI
	 * @throws NullPointerException If {@code relative} is {@code null}
	 */
	public ParsedIRI resolve(ParsedIRI relative) {
		// This algorithm is based on the algorithm specified in chapter 5 of
		// RFC 2396: URI Generic Syntax. See http://www.ietf.org/rfc/rfc2396.txt

		// RFC, step 3:
		if (relative.isAbsolute()) {
			return relative;
		}

		// relURI._scheme == null

		// RFC, step 2:
		if (relative.getHost() == null && relative.getQuery() == null && relative.getPath().length() == 0) {

			// Inherit any fragment identifier from relURI
			String fragment = relative.getFragment();

			return new ParsedIRI(this.getScheme(), this.getUserInfo(), this.getHost(), this.getPort(), this.getPath(),
					this.getQuery(), fragment);
		} else if (relative.getHost() == null && relative.getPath().length() == 0) {

			// Inherit any query or fragment from relURI
			String query = relative.getQuery();
			String fragment = relative.getFragment();

			return new ParsedIRI(this.getScheme(), this.getUserInfo(), this.getHost(), this.getPort(), this.getPath(),
					query, fragment);
		}

		// We can start combining the URIs
		String scheme, userInfo, host, path, query, fragment;
		int port;
		boolean normalize = false;

		scheme = this.getScheme();
		query = relative.getQuery();
		fragment = relative.getFragment();

		// RFC, step 4:
		if (relative.getHost() != null) {
			userInfo = relative.getUserInfo();
			host = relative.getHost();
			port = relative.getPort();
			path = relative.getPath();
		} else {
			userInfo = this.getUserInfo();
			host = this.getHost();
			port = this.getPort();

			// RFC, step 5:
			if (relative.getPath().startsWith("/")) {
				path = relative.getPath();
			} else {
				// RFC, step 6:
				path = this.getPath();

				if (path == null) {
					path = "/";
				} else {
					if (!path.endsWith("/")) {
						// Remove the last segment of the path. Note: if
						// lastSlashIdx is -1, the path will become empty,
						// which is fixed later.
						int lastSlashIdx = path.lastIndexOf('/');
						path = path.substring(0, lastSlashIdx + 1);
					}

					if (path.length() == 0) {
						// No path means: start at root.
						path = "/";
					}
				}

				// Append the path of the relative URI
				path += relative.getPath();

				// Path needs to be normalized.
				normalize = true;
			}
		}

		if (normalize || path.contains("/./") || path.contains("/../")) {
			path = pathSegmentNormalization(path);
		}

		return new ParsedIRI(scheme, userInfo, host, port, path, query, fragment);
	}

	/**
	 * Relativizes the given IRI against this ParsedIRI.
	 *
	 * @see #relativize(ParsedIRI)
	 * @param iri The IRI to be relativized against this ParsedIRI
	 * @return The resulting IRI
	 * @throws NullPointerException If {@code absolute} is {@code null}
	 */
	public String relativize(String iri) {
		return relativize(ParsedIRI.create(iri)).toString();
	}

	/**
	 * Relativizes the given IRI against this ParsedIRI.
	 * <p>
	 * <i>Relativization</i> is the inverse of resolution. This operation is often useful when constructing a document
	 * containing IRIs that must be made relative to the base IRI of the document wherever possible.
	 * <p>
	 * The relativization of the given URI against this URI is computed as follows:
	 * </p>
	 * <ol>
	 * <li>
	 * <p>
	 * If either this IRI or the given IRI are opaque, or if the scheme and authority components of the two IRIs are not
	 * identical, or if the path of this IRI is not a prefix of the path of the given URI, then the given IRI is
	 * returned.
	 * </p>
	 * </li>
	 * <li>
	 * <p>
	 * Otherwise a new relative hierarchical IRI is constructed with query and fragment components taken from the given
	 * IRI and with a path component computed by removing this IRI's path from the beginning of the given IRI's path.
	 * </p>
	 * </li>
	 * </ol>
	 *
	 * @param absolute The IRI to be relativized against this ParsedIRI
	 * @return The resulting IRI
	 * @throws NullPointerException If {@code absolute} is {@code null}
	 */
	public ParsedIRI relativize(ParsedIRI absolute) {
		// identity URI reference
		String _frag = absolute.getFragment();
		if (iri.equals(absolute.iri) && _frag == null) {
			return new ParsedIRI(null, null, null, -1, "", null, null);
		}
		// different scheme or authority
		if (absolute.getScheme() != null && !absolute.getScheme().equalsIgnoreCase(this.getScheme())) {
			return absolute;
		}
		if (absolute.getUserInfo() != null && !absolute.getUserInfo().equals(this.getUserInfo())) {
			return absolute;
		}
		if (absolute.getHost() != null && !absolute.getHost().equalsIgnoreCase(this.getHost())) {
			return absolute;
		}
		if (absolute.getPort() != this.getPort()) {
			return absolute;
		}
		// fragment URI reference
		if (_frag != null) {
			if (this.getFragment() == null) {
				if (absolute.iri.startsWith(this.iri) && absolute.iri.charAt(iri.length()) == '#') {
					return new ParsedIRI(null, null, null, -1, "", null, _frag);
				}
			} else {
				int this_idx = iri.length() - this.getFragment().length();
				int abs_idx = absolute.iri.length() - _frag.length();
				if (iri.substring(0, this_idx).equals(absolute.iri.substring(0, abs_idx))) {
					return new ParsedIRI(null, null, null, -1, "", null, _frag);
				}
			}
		}
		// opaque IRI
		if (this.isOpaque() || absolute.isOpaque()) {
			return absolute;
		}
		// query string URI reference
		String _query = absolute.getQuery();
		if (_query != null) {
			if (this.getQuery() == null && this.getFragment() == null) {
				if (absolute.iri.startsWith(this.iri) && absolute.iri.charAt(iri.length()) == '?') {
					return new ParsedIRI(null, null, null, -1, "", _query, _frag);
				}
			} else {
				int this_idx = this.getQuery() == null ? iri.indexOf('#') : iri.indexOf('?');
				int abs_idx = absolute.iri.indexOf('?');
				if (iri.substring(0, this_idx).equals(absolute.iri.substring(0, abs_idx))) {
					return new ParsedIRI(null, null, null, -1, "", _query, _frag);
				}
			}
		}
		// last path segment
		String _path = absolute.getPath();
		int this_idx = this.getPath().lastIndexOf('/');
		int abs_idx = _path.lastIndexOf('/');
		if (this_idx < 0 || abs_idx < 0) {
			return absolute;
		}
		if (_path.equals(this.getPath().substring(0, this_idx + 1))) {
			return new ParsedIRI(null, null, null, -1, ".", _query, _frag);
		}
		// within last path segment
		if (_path.startsWith(this.getPath().substring(0, this_idx + 1))) {
			return new ParsedIRI(null, null, null, -1, _path.substring(this_idx + 1), _query, _frag);
		}
		return new ParsedIRI(null, null, null, -1, relativizePath(_path), _query, _frag);
	}

	private void parse() throws URISyntaxException {
		pos = 0;
		scheme = parseScheme();
		if ("jar".equalsIgnoreCase(scheme)) {
			scheme = scheme + ':' + parseScheme();
		}
		int peek = peek();
		if ('/' == peek && '/' == peek(1)) {
			advance(2);
			if (iri.indexOf('@') >= 0) {
				userInfo = parseUserInfo();
			}
			host = parseHost();
			if (':' == peek()) {
				advance(1);
				String p = parseMember(DIGIT, '/');
				if (p.length() > 0) {
					port = Integer.parseInt(p);
				} else {
					port = -1;
				}
			}
			int next = peek();
			if ('/' == next || '?' == next || '#' == next || EOF == next) {
				path = parsePath();
			} else {
				throw error("absolute or empty path expected");
			}
		} else if ('/' == peek || '?' == peek || '#' == peek || EOF == peek) {
			path = parsePath();
		} else if ('%' == peek || ':' != peek && isMember(pchar, peek)) {
			path = parsePath();
		} else if (scheme != null && ':' == peek) {
			path = parsePath();
		}
		if ('?' == peek()) {
			advance(1);
			query = parsePctEncoded(qchar, '#', EOF);
		}
		if ('#' == peek()) {
			advance(1);
			fragment = parsePctEncoded(fchar, '#', EOF);
		}
		if (pos != iri.length()) {
			throw error("Unexpected character");
		}
	}

	private String buildIRI(String scheme, String userInfo, String host, int port, String path, String query,
			String fragment) {
		StringBuilder sb = new StringBuilder();
		if (scheme != null) {
			sb.append(scheme).append(':');
		}
		if (host != null) {
			sb.append("//");
			if (userInfo != null) {
				sb.append(userInfo).append('@');
			}
			sb.append(host);
			if (port >= 0) {
				sb.append(':').append(port);
			}
		}
		if (path != null) {
			sb.append(path);
		}
		if (query != null) {
			sb.append('?').append(query);
		}
		if (fragment != null) {
			sb.append('#').append(fragment);
		}
		return sb.toString();
	}

	private String parseScheme() {
		if (isMember(ALPHA, peek())) {
			int start = pos;
			String scheme = parseMember(schar, ':');
			if (':' == peek()) {
				advance(1);
				return scheme;
			} else {
				pos = start; // reset
			}
		}
		return null;
	}

	private String parseUserInfo() throws URISyntaxException {
		int start = pos;
		String userinfo = parsePctEncoded(uchar, '@', '/');
		if ('@' == peek()) {
			advance(1);
			return userinfo;
		} else {
			pos = start; // reset
			return null;
		}
	}

	private String parseHost() throws URISyntaxException {
		int start = pos;
		if ('[' == peek()) {
			advance(1); // IP-Literal
			parseMember(uchar, ']');
			if (']' == peek()) {
				advance(1);
				return iri.substring(start, pos);
			} else {
				throw error("Invalid host IP address");
			}
		} else if (isMember(DIGIT, peek())) {
			URISyntaxException parsingException = null;
			int startPos = pos;
			for (int i = 0; i < 4; i++) {
				int octet;
				try {
					octet = Integer.parseInt(parseMember(DIGIT, '.'));
				} catch (NumberFormatException e) {
					parsingException = error("Invalid IPv4 address");
					break;
				}
				if (octet < 0 || octet > 255) {
					parsingException = error("Invalid IPv4 address");
					break;
				}
				if ('.' == peek()) {
					advance(1);
				} else {
					if (i == 3 && (EOF == peek() || ':' == peek() || '/' == peek())) {
						// next is end of IRI, a port, or a path
					} else {
						parsingException = error("Invalid IPv4 address");
						break;
					}
				}
			}
			if (parsingException == null) {
				// IPv4 parsed successfully
				return iri.substring(start, pos);
			} else {
				// Reset position and parse host
				pos = startPos;
				String host = parsePctEncoded(hchar, ':', '/');

				// http(s) scheme requires a valid top-level domain
				if (isScheme("http") || isScheme("https")) {
					if (!isTLDValid(start)) {
						throw parsingException;
					}
				}
				return host;
			}
		} else {
			return parsePctEncoded(hchar, ':', '/');
		}
	}

	private boolean isTLDValid(int hostStartPos) {
		int step = 0;
		boolean illegalCharFound = false;

		while (pos + step > hostStartPos) {
			int currChar = peek(--step);
			if ('.' == currChar) {
				return !illegalCharFound;
			}

			// TLDs should start with a letter
			if (!isMember(ALPHA, currChar)) {
				illegalCharFound = true;
			}
		}

		return true;
	}

	private String parsePath() throws URISyntaxException {
		return parsePctEncoded(fchar, '?', '#');
	}

	private String parsePctEncoded(int[][] set, int end1, int end2) throws URISyntaxException {
		int start = pos;
		while (true) {
			int chr = peek();
			if (chr == EOF || chr == end1 || chr == end2) {
				break; // optimize end character
			} else if (('a' <= chr && chr <= 'z') || ('A' <= chr && chr <= 'Z') || ('0' <= chr && chr <= '9')) {
				advance(1);
			} else if ('%' == chr) {
				if (Arrays.binarySearch(HEXDIG, peek(1)) >= 0 && Arrays.binarySearch(HEXDIG, peek(2)) >= 0) {
					advance(3);
				} else {
					throw error("Illegal percent encoding");
				}
			} else if (isMember(set, chr)) {
				advance(1);
			} else {
				break;
			}
		}
		return iri.substring(start, pos);
	}

	private String parseMember(int[][] set, int end) {
		int start = pos;
		while (true) {
			int chr = peek();
			if (chr == EOF || chr == end) {
				break;
			} else if (isMember(set, chr)) {
				advance(1);
			} else {
				break;
			}
		}
		return iri.substring(start, pos);
	}

	private boolean isMember(int[][] set, int chr) {
		int idx = Arrays.binarySearch(set, new int[] { chr }, CMP);
		if (idx >= 0) {
			return true; // lower range matched exactly
		} else if (idx == -1) {
			return false; // insertion point is 0, below lowest range
		} else {
			int i = -idx - 2; // range just before insertion point
			assert set[i][0] <= chr && set[i].length == 2;
			return chr <= set[i][1];
		}
	}

	private int peek() {
		if (pos < iri.length()) {
			return iri.codePointAt(pos);
		} else {
			return EOF;
		}
	}

	private int peek(int ahead) {
		if (pos + ahead < iri.length()) {
			return iri.codePointAt(iri.offsetByCodePoints(pos, ahead));
		} else {
			return EOF;
		}
	}

	private void advance(int ahead) {
		if (pos + ahead < iri.length()) {
			pos = iri.offsetByCodePoints(pos, ahead);
		} else {
			pos += ahead;
		}
	}

	private URISyntaxException error(String reason) {
		if (pos > -1 && pos < iri.length()) {
			int cp = iri.codePointAt(pos);
			reason = reason + " U+" + Integer.toHexString(cp).toUpperCase();
		}
		return new URISyntaxException(iri, reason, pos);
	}

	private void appendAscii(StringBuilder sb, String input) {
		for (int c = 0, n = input.codePointCount(0, input.length()); c < n; c++) {
			int chr = input.codePointAt(input.offsetByCodePoints(0, c));
			if (Arrays.binarySearch(ascii, chr) >= 0) {
				sb.appendCodePoint(chr);
			} else {
				sb.append(pctEncode(chr));
			}
		}
	}

	private String toLowerCase(String string) {
		if (string == null) {
			return null;
		}
		boolean changed = false;
		StringBuilder sb = new StringBuilder(string);
		for (int i = 0; i < sb.length(); i++) {
			char c = sb.charAt(i);
			if ((c >= 'A') && (c <= 'Z')) {
				changed = true;
				sb.setCharAt(i, (char) (c + ('a' - 'A')));
			}
		}
		return changed ? sb.toString() : string;
	}

	private String toUpperCase(String string) {
		if (string == null) {
			return null;
		}
		boolean changed = false;
		StringBuilder sb = new StringBuilder(string);
		for (int i = 0; i < sb.length(); i++) {
			char c = sb.charAt(i);
			if ((c >= 'a') && (c <= 'z')) {
				changed = true;
				sb.setCharAt(i, (char) (c - ('a' - 'A')));
			}
		}
		return changed ? sb.toString() : string;
	}

	private boolean isScheme(String scheme) {
		return scheme.equalsIgnoreCase(this.scheme) || this.scheme != null && this.scheme.indexOf(':') == 3
				&& this.scheme.equalsIgnoreCase("jar:" + scheme);
	}

	private String normalizePath(String path) {
		if ("".equals(path)) {
			return isScheme("http") || isScheme("https") ? "/" : "";
		} else if (isScheme("file")) {
			if (path.contains("%5C")) {
				// replace "/c:\path\to\file" with "/c:/path/to/file"
				return normalizePath(path.replace("%5C", "/"));
			} else if (!path.startsWith("/") && isMember(ALPHA, path.codePointAt(0))
					&& (':' == path.charAt(1) || path.length() >= 4 && "%7C".equals(path.substring(1, 4)))) {
				// replace "c:/path/to/file" with "/c:/path/to/file"
				return normalizePath("/" + path);
			} else if (path.length() >= 5 && "%7C".equals(path.substring(2, 5))
					&& isMember(ALPHA, path.codePointAt(1))) {
				// replace "/c|/path/to/file" with "/c:/path/to/file"
				return normalizePath(path.substring(0, 2) + ':' + path.substring(5));
			}
		}
		return pctEncodingNormalization(pathSegmentNormalization(path));
	}

	private String pctEncodingNormalization(String path) {
		if (path == null || path.length() == 0 || path.indexOf('%') < 0) {
			return path; // no pct encodings
		}
		String[] encodings = listPctEncodings(path);
		StringBuilder sb = new StringBuilder(path);
		int pos = 0;
		for (String encoding : encodings) {
			int idx = sb.indexOf(encoding, pos);
			String decoded = normalizePctEncoding(encoding);
			sb.replace(idx, idx + encoding.length(), decoded);
			pos += decoded.length();
		}
		return Normalizer.normalize(sb, Normalizer.Form.NFC);

	}

	private String[] listPctEncodings(String path) {
		if (path == null || path.indexOf('%') < 0) {
			return new String[0];
		}
		List<String> list = new ArrayList<>();
		int pct = -1;
		while ((pct = path.indexOf('%', pct + 1)) > 0) {
			int start = pct;
			// optimize common encoded members by grouping separately
			if (Arrays.binarySearch(common_pct, path.substring(pct, pct + 3)) < 0) {
				while (pct + 3 < path.length() && path.charAt(pct + 3) == '%'
						&& Arrays.binarySearch(common_pct, path.substring(pct + 3, pct + 6)) < 0) {
					pct += 3;
				}
			}
			list.add(path.substring(start, pct + 3));
		}
		return list.toArray(new String[0]);
	}

	private String normalizePctEncoding(String encoded) {
		int cidx = Arrays.binarySearch(common_pct, encoded);
		if (cidx >= 0 && isMember(unreserved, common[cidx])) {
			return new String(Character.toChars(common[cidx])); // quickly decode unreserved encodings
		} else if (cidx >= 0) {
			return encoded; // pass through reserved encodings
		}
		String decoded = pctDecode(encoded);
		String ns = Normalizer.normalize(decoded, Normalizer.Form.NFC);
		StringBuilder sb = new StringBuilder(ns.length());
		for (int c = 0, n = ns.codePointCount(0, ns.length()); c < n; c++) {
			int chr = ns.codePointAt(ns.offsetByCodePoints(0, c));
			if (isMember(unreserved, chr)) {
				sb.appendCodePoint(chr);
			} else if (n == 1) {
				return toUpperCase(encoded);
			} else {
				sb.append(pctEncode(chr));
			}
		}
		return sb.toString();
	}

	private String pctDecode(String encoded) {
		return URLDecoder.decode(encoded, StandardCharsets.UTF_8);

	}

	private String pctEncode(int chr) {
		return URLEncoder.encode(new String(Character.toChars(chr)), StandardCharsets.UTF_8);

	}

	/**
	 * Normalizes the path of this URI if it has one. Normalizing a path means that any unnecessary '.' and '..'
	 * segments are removed. For example, the URI <var>http://server.com/a/b/../c/./d</var> would be normalized to
	 * <var>http://server.com/a/c/d</var>. A URI doens't have a path if it is opaque.
	 */
	private String pathSegmentNormalization(String _path) {
		if (_path == null) {
			return null;
		}

		// Remove any '.' segments:
		_path = _path.replace("/./", "/");
		if (_path == null) {
			return null;
		}
		if (_path.startsWith("./")) {
			// Remove both characters
			_path = _path.substring(2);
		}

		if (_path.endsWith("/.")) {
			// Remove only the last dot, not the slash!
			_path = _path.substring(0, _path.length() - 1);
		}

		if (!_path.contains("/../") && !_path.endsWith("/..")) {
			// There are no '..' segments that can be removed. We're done and
			// don't have to execute the time-consuming code following this
			// if-statement
			return _path;
		}

		// Split the path into its segments

		LinkedList<String> segments = new LinkedList<>(Arrays.asList(_path.split("/")));
		if (_path.startsWith("/")) {
			segments.remove(0);
		}

		boolean lastSegmentRemoved = false;

		// Remove all unnecessary '..' segments

		int i = 1;
		while (i < segments.size()) {
			String segment = segments.get(i);

			if (segment.equals("..")) {
				String prevSegment = segments.get(i - 1);

				if (prevSegment.equals("..")) {
					// two consecutive '..' segments at position i-1 and i,
					// continue at i + 2
					i += 2;
				} else {
					// Bingo! Remove these two segments...
					if (i == segments.size() - 1) {
						lastSegmentRemoved = true;
					}

					segments.remove(i);
					segments.remove(i - 1);

					// ...and continue at position (i + 1 - 2) == (i - 1)...

					// ...but only if i > 1, position 0 does not need to be
					// checked.

					if (i > 1) {
						i--;
					}
				}
			} else {
				// Not a '..' segment, check next
				i++;
			}
		}

		while (!segments.isEmpty() && (segments.get(0).equals("..") || segments.get(0).equals("."))) {
			segments.remove(0);
		}

		// Construct the normalized path

		StringBuilder newPath = new StringBuilder(_path.length());

		if (_path.startsWith("/")) {
			newPath.append('/');
		}

		int segmentCount = segments.size();
		for (i = 0; i < segmentCount - 1; i++) {
			newPath.append(segments.get(i));
			newPath.append('/');
		}

		if (segmentCount > 0) {
			String lastSegment = segments.get(segmentCount - 1);
			newPath.append(lastSegment);

			if (_path.endsWith("/") || lastSegmentRemoved) {
				newPath.append('/');
			}
		}

		return newPath.toString();
	}

	private String relativizePath(String absolute) {
		assert absolute.charAt(0) == '/';
		String[] paths = path.split("/", Integer.MAX_VALUE);
		String[] seg = absolute.split("/", Integer.MAX_VALUE);
		// first segment is empty string
		int same = 1;
		while (same < paths.length && same < seg.length - 1 && paths[same].equals(seg[same])) {
			same++;
		}
		if (same < 2) {
			return absolute;
		}
		StringBuilder sb = new StringBuilder();
		// last segment is empty or file name
		for (int i = same; i < paths.length - 1; i++) {
			sb.append("../");
		}
		for (int i = same; i < seg.length - 1; i++) {
			sb.append(seg[i]).append('/');
		}
		return sb.append(seg[seg.length - 1]).toString();
	}
}
