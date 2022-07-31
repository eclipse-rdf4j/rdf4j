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

package org.eclipse.rdf4j.common.net;

import java.util.LinkedList;
import java.util.StringTokenizer;

/**
 * A replacement for Java's own URI: java.net.URI. Java's implementation is quite buggy in that it doesn't resolve
 * relative URIs correctly.
 * <p>
 * Note: this implementation is not guaranteed to handle ipv6 addresses correctly (yet).
 *
 * @deprecated use {@link ParsedIRI} instead
 */
@Deprecated
public class ParsedURI implements java.lang.Cloneable {

	/*
	 * // Tesing method public static void main(String[] args) throws Exception { URI baseURI = new URI(args[0]);
	 * baseURI.normalize(); URI uri = null; for (int i = 0; i < 100; i++) { uri = baseURI.resolve(args[1]); } try {
	 * Thread.sleep(1000); } catch (Exception e) {} long startTime = System.currentTimeMillis(); for (int i = 0; i <
	 * 100; i++) { uri = baseURI.resolve(args[1]); } long endTime = System.currentTimeMillis();
	 * System.out.println(args[0] + " was parsed as:"); System.out.println("scheme = " + uri.getScheme());
	 * System.out.println("schemeSpecificPart = " + uri.getSchemeSpecificPart()); System.out.println("authority = " +
	 * uri.getAuthority()); System.out.println("path = " + uri.getPath()); System.out.println("query = " +
	 * uri.getQuery()); System.out.println("fragment = " + uri.getFragment()); System.out.println("full URI = " +
	 * uri.toString()); System.out.println(" parsed 100 times in " + (endTime-startTime) + "ms"); }
	 */

	/*-----------*
	 * Variables *
	 *-----------*/

	// For all URIs:
	private String _scheme;

	private String _schemeSpecificPart;

	private String _fragment;

	// For hierarchical URIs:
	private String _authority;

	private String _path;

	private String _query;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public ParsedURI(String uriSpec) {
		_parse(uriSpec);
	}

	public ParsedURI(String scheme, String schemeSpecificPart, String fragment) {
		_scheme = scheme;
		_schemeSpecificPart = schemeSpecificPart;
		_fragment = fragment;
	}

	public ParsedURI(String scheme, String authority, String path, String query, String fragment) {
		_scheme = scheme;
		_authority = authority;
		_path = path;
		_query = query;
		_fragment = fragment;
	}

	/*-----------------------*
	 * Public access methods *
	 *-----------------------*/

	public boolean isHierarchical() {
		return _path != null;
	}

	public boolean isOpaque() {
		return _path == null;
	}

	public boolean isAbsolute() {
		return _scheme != null;
	}

	public boolean isRelative() {
		return _scheme == null;
	}

	/**
	 * Checks whether this URI is a relative URI that references itself (i.e. it only contains an anchor).
	 */
	public boolean isSelfReference() {
		return _scheme == null && _authority == null && _query == null && _path.length() == 0;
	}

	public String getScheme() {
		return _scheme;
	}

	public String getSchemeSpecificPart() {
		return _schemeSpecificPart;
	}

	public String getAuthority() {
		return _authority;
	}

	public String getPath() {
		return _path;
	}

	public String getQuery() {
		return _query;
	}

	public String getFragment() {
		return _fragment;
	}

	/*------------------------------*
	 * Methods for normalizing URIs *
	 *------------------------------*/

	/**
	 * Normalizes the path of this URI if it has one. Normalizing a path means that any unnecessary '.' and '..'
	 * segments are removed. For example, the URI <var>http://server.com/a/b/../c/./d</var> would be normalized to
	 * <var>http://server.com/a/c/d</var>. A URI doens't have a path if it is opaque.
	 */
	public void normalize() {
		if (_path == null) {
			return;
		}

		// Remove any '.' segments:

		_path = _path.replace("/./", "/");

		if (_path.startsWith("./")) {
			// Remove both characters
			_path = _path.substring(2);
		}

		if (_path.endsWith("/.")) {
			// Remove only the last dot, not the slash!
			_path = _path.substring(0, _path.length() - 1);
		}

		if (_path.indexOf("/../") == -1 && !_path.endsWith("/..")) {
			// There are no '..' segments that can be removed. We're done and
			// don't have to execute the time-consuming code following this
			// if-statement
			return;
		}

		// Split the path into its segments

		LinkedList<String> segments = new LinkedList<>();

		StringTokenizer st = new StringTokenizer(_path, "/");

		while (st.hasMoreTokens()) {
			segments.add(st.nextToken());
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

		_path = newPath.toString();
	}

	/**
	 * Resolves a relative URI using this URI as the base URI.
	 */
	public ParsedURI resolve(String relURISpec) {
		// This algorithm is based on the algorithm specified in chapter 5 of
		// RFC 2396: URI Generic Syntax. See http://www.ietf.org/rfc/rfc2396.txt

		// RFC, step 1:
		ParsedURI relURI = new ParsedURI(relURISpec);

		return this.resolve(relURI);
	}

	/**
	 * Resolves a relative URI using this URI as the base URI.
	 */
	public ParsedURI resolve(ParsedURI relURI) {
		// This algorithm is based on the algorithm specified in chapter 5 of
		// RFC 2396: URI Generic Syntax. See http://www.ietf.org/rfc/rfc2396.txt

		// RFC, step 3:
		if (relURI.isAbsolute()) {
			return relURI;
		}

		// relURI._scheme == null

		// RFC, step 2:
		if (relURI._authority == null && relURI._query == null && relURI._path.length() == 0) {
			// Reference to this URI
			ParsedURI result = (ParsedURI) this.clone();

			// Inherit any fragment identifier from relURI
			result._fragment = relURI._fragment;

			return result;
		}

		// We can start combining the URIs
		String scheme, authority, path, query, fragment;
		boolean normalizeURI = false;

		scheme = this._scheme;
		query = relURI._query;
		fragment = relURI._fragment;

		// RFC, step 4:
		if (relURI._authority != null) {
			authority = relURI._authority;
			path = relURI._path;
		} else {
			authority = this._authority;

			// RFC, step 5:
			if (relURI._path.startsWith("/")) {
				path = relURI._path;
			} else if (relURI._path.length() == 0) {
				path = this._path;
			} else {
				// RFC, step 6:
				path = this._path;

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
				path += relURI._path;

				// Path needs to be normalized.
				normalizeURI = true;
			}
		}

		ParsedURI result = new ParsedURI(scheme, authority, path, query, fragment);

		if (normalizeURI) {
			result.normalize();
		}

		return result;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(64);

		if (_scheme != null) {
			sb.append(_scheme);
			if (!isJarScheme(_scheme)) {
				sb.append(':');
			}
		}

		if (isOpaque()) {
			// Opaque URI
			if (_schemeSpecificPart != null) {
				sb.append(_schemeSpecificPart);
			}
		} else {
			// Hierachical URI
			if (_authority != null) {
				sb.append("//");
				sb.append(_authority);
			}

			sb.append(_path);

			if (_query != null) {
				sb.append('?');
				sb.append(_query);
			}
		}

		if (_fragment != null) {
			sb.append('#');
			sb.append(_fragment);
		}

		return sb.toString();
	}

	// Overrides Object.clone()
	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	/*--------------------------*
	 * Methods for parsing URIs *
	 *--------------------------*/

	private static boolean isJarScheme(String s) {
		return (s.length() > 4 && s.substring(0, 4).equalsIgnoreCase("jar:"));
	}

	private void _parse(String uri) {
		if (isJarScheme(uri)) {
			// uriString is e.g.
			// jar:http://www.foo.com/bar/baz.jar!/COM/foo/Quux.class
			// Treat the part up to and including the exclamation mark as the
			// scheme and
			// the rest as the path to enable 'correct' resolving of relative URIs
			int idx = uri.indexOf('!');
			if (idx != -1) {
				String scheme = uri.substring(0, idx + 1);
				String path = uri.substring(idx + 1);

				_scheme = scheme;
				_authority = null;
				_path = path;
				_query = null;
				_fragment = null;

				return;
			}
		}

		if (_parseScheme(uri)) {
			// A scheme was found; _scheme and _schemeSpecificPart are now set
			if (_schemeSpecificPart.startsWith("/")) {
				// Hierachical URI
				String rest = _schemeSpecificPart;
				rest = _parseAuthority(rest);
				rest = _parsePath(rest);
				rest = _parseQuery(rest);
				_parseFragment(rest);
			} else {
				// Opaque URI
				String rest = _schemeSpecificPart;
				rest = _parseOpaquePart(rest);
				_parseFragment(rest);
			}
		} else {
			// No scheme was found
			String rest = uri;
			rest = _parseAuthority(rest);
			rest = _parsePath(rest);
			rest = _parseQuery(rest);
			_parseFragment(rest);
		}
	}

	private boolean _parseScheme(String uri) {
		// Query cannot contain a ':', '/', '?' or '#' character

		// Try to find the scheme in the URI
		char c = 0;
		int i = 0;

		for (; i < uri.length(); i++) {
			c = uri.charAt(i);
			if (c == ':' || c == '/' || c == '?' || c == '#') {
				// c is equal to one of the illegal chars
				break;
			}
		}

		if (c == ':' && i > 0) {
			// We've found a scheme
			_scheme = uri.substring(0, i);
			_schemeSpecificPart = uri.substring(i + 1);
			return true;
		}

		// No scheme found, uri is relative
		return false;
	}

	private String _parseAuthority(String s) {
		// Query cannot contain a '/', '?' or '#' character

		if (s.startsWith("//")) {
			// Authority present, could be empty though.
			int i = 2;
			for (; i < s.length(); i++) {
				char c = s.charAt(i);
				if (c == '/' || c == '?' || c == '#') {
					// c is equal to one of the illegal chars
					break;
				}
			}

			_authority = s.substring(2, i);
			return s.substring(i);
		}

		return s;
	}

	private String _parsePath(String s) {
		// Query cannot contain a '?' or '#' character

		int i = 0;
		for (; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '?' || c == '#') {
				// c is equal to one of the illegal chars
				break;
			}
		}

		_path = s.substring(0, i);

		return s.substring(i);
	}

	private String _parseQuery(String s) {
		// Query must start with a '?' and cannot contain a '#' character

		if (s.startsWith("?")) {
			int i = 1;
			for (; i < s.length(); i++) {
				char c = s.charAt(i);
				if (c == '#') {
					// c is equal to one of the illegal chars
					break;
				}
			}

			_query = s.substring(1, i);
			return s.substring(i);
		} else {
			return s;
		}
	}

	private String _parseOpaquePart(String s) {
		// Opaque part cannot contain a '#' character

		int i = 0;
		for (; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '#') {
				// c is equal to one of the illegal chars
				break;
			}
		}

		_schemeSpecificPart = s.substring(0, i);

		return s.substring(i);
	}

	private void _parseFragment(String s) {
		// Fragment must start with a '#'
		if (s.startsWith("#")) {
			_fragment = s.substring(1);
		}
	}
}
