/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.queryrender.sparql.util;

import java.util.regex.Pattern;

/** SPARQL name helpers (prefixed names and PN_LOCAL checks). */
public final class SparqlNameUtils {
	private SparqlNameUtils() {
	}

	// Conservative PN_LOCAL segment pattern; overall check also prohibits trailing dots.
	private static final Pattern PN_LOCAL_CHUNK = Pattern
			.compile("(?:%[0-9A-Fa-f]{2}|[-\\p{L}\\p{N}_\\u00B7]|:)+");

	public static boolean isPNLocal(final String s) {
		if (s == null || s.isEmpty()) {
			return false;
		}
		if (s.charAt(s.length() - 1) == '.') {
			return false; // no trailing dot
		}
		char first = s.charAt(0);
		if (!(first == ':' || Character.isLetter(first) || first == '_' || Character.isDigit(first))) {
			return false;
		}
		int i = 0;
		boolean needChunk = true;
		while (i < s.length()) {
			int j = i;
			while (j < s.length() && s.charAt(j) != '.') {
				j++;
			}
			String chunk = s.substring(i, j);
			if (needChunk && chunk.isEmpty()) {
				return false;
			}
			if (!chunk.isEmpty() && !PN_LOCAL_CHUNK.matcher(chunk).matches()) {
				return false;
			}
			i = j + 1; // skip dot (if any)
			needChunk = false;
		}
		return true;
	}
}
