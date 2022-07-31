/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.core;

import org.eclipse.rdf4j.model.util.URIUtil;

/**
 * A collection of SPARQL Prefix declarations
 *
 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#prefNames"> SPARQL Prefix</a>
 */
public class PrefixDeclarations extends StandardQueryElementCollection<Prefix> {
	/**
	 * Add prefix declarations to this collection
	 *
	 * @param prefixes the prefixes
	 * @return this
	 */
	public PrefixDeclarations addPrefix(Prefix... prefixes) {
		addElements(prefixes);

		return this;
	}

	/**
	 * Replaces all occurrences of all declared namespaces with their prefix labels in the specified query string.
	 *
	 * For example, if the <code>foaf:</code> prefix is declared with {@link PrefixDeclarations}, the query
	 *
	 * <pre>
	 * SELECT ?name WHERE {
	 *   ?x &lt;http://xmlns.com/foaf/0.1/name&gt; ?name .
	 * }
	 * </pre>
	 *
	 * is transformed to
	 *
	 * <pre>
	 * SELECT ?name WHERE {
	 *   ?x foaf:name ?name .
	 * }
	 * </pre>
	 *
	 *
	 * Rules applied:
	 * <ul>
	 * <li>The longest matching namespace wins (if one namespace is a substring of another)</li>
	 * <li>No replacement if the namespace occurs in a string, i.e., within <code>"</code> or <code>'''</code></li>
	 * <li>Only replace if the continuation of the match is a
	 * <a href="https://www.w3.org/TR/sparql11-query/#rPN_LOCAL">local name</a> (</li>
	 * </ul>
	 *
	 * @param queryString the query string
	 * @return the query string, namespaces replaced with prefix label
	 */
	public String replacePrefixesInQuery(String queryString) {
		boolean isInsideDoubleQuotes = false, isInsideMultlineQuotes = false, isInsideString = false;
		StringBuilder sb = new StringBuilder();
		int pos = 0;
		int lastPos = 0;
		while (pos != -1 && pos < queryString.length()) {
			pos = findNextRelevantIndex(queryString, lastPos, isInsideString);
			if (pos == -1) {
				break;
			}
			if (pos >= queryString.length() - 1) {
				break;
			}
			sb.append(queryString, lastPos, pos);
			if (isInsideString) {
				if (isEscapeChar(queryString, pos)) {
					sb.append(queryString, pos, pos + 2);
					pos = pos + 2;
					lastPos = pos;
					continue;
				}
				if (isInsideDoubleQuotes && isDoubleQuote(queryString, pos)) {
					sb.append(queryString, pos, pos + 1);
					isInsideString = false;
					isInsideDoubleQuotes = false;
					pos++;
					lastPos = pos;
					continue;
				}
				if (isInsideMultlineQuotes && isMultilineQuote(queryString, pos)) {
					sb.append(queryString, pos, pos + 3);
					isInsideString = false;
					isInsideMultlineQuotes = false;
					pos = pos + 3;
					lastPos = pos;
					continue;
				}
			} else {
				if (isDoubleQuote(queryString, pos)) {
					sb.append(queryString, pos, pos + 1);
					isInsideString = true;
					isInsideDoubleQuotes = true;
					pos++;
					lastPos = pos;
					continue;
				}
				if (isMultilineQuote(queryString, pos)) {
					sb.append(queryString, pos, pos + 3);
					isInsideString = true;
					isInsideMultlineQuotes = true;
					pos = pos + 3;
					lastPos = pos;
					continue;
				}
			}
			if (isInsideString) {
				sb.append(queryString, pos, pos + 1);
				pos++;
				lastPos = pos;
				continue;
			}
			if (isOpeningAngledBracket(queryString, pos)) { // test not necessary but makes code more readable
				Prefix matchingPrefix = findMatchingPrefix(queryString, pos + 1);
				if (matchingPrefix != null) {
					int posOfClosingBracket = queryString.indexOf('>', pos);
					if (posOfClosingBracket > -1) {
						int replacementLength = matchingPrefix.getIri().getQueryString().length() - 2; // subtract 2 for
																										// '<'
						// and '>'
						sb
								.append(matchingPrefix.getLabel())
								.append(":")
								.append(queryString, pos + 1 + replacementLength, posOfClosingBracket);
						pos = posOfClosingBracket + 1;
					}
				} else {
					sb.append('<');
					pos++;
				}
			}
			lastPos = pos;
		}
		if (pos == -1) {
			sb.append(queryString.substring(lastPos));
		}
		return sb.toString();
	}

	private boolean isOpeningAngledBracket(String queryString, int pos) {
		return queryString.charAt(pos) == '<';
	}

	private boolean isMultilineQuote(String queryString, int pos) {
		return queryString.startsWith("'''", pos);
	}

	private boolean isEscapeChar(String queryString, int pos) {
		return queryString.charAt(pos) == '\\';
	}

	private boolean isDoubleQuote(String queryString, int pos) {
		return queryString.charAt(pos) == '"';
	}

	private int findNextRelevantIndex(String queryString, int lastPos, boolean isInsideString) {
		int[] mins = new int[] {
				isInsideString ? -1 : queryString.indexOf('<', lastPos),
				isInsideString ? queryString.indexOf('\\', lastPos) : -1,
				queryString.indexOf('"', lastPos),
				queryString.indexOf("'''", lastPos)
		};
		int min = Integer.MAX_VALUE;
		for (int j : mins) {
			if (j >= 0) {
				min = Math.min(min, j);
			}
		}
		return min == Integer.MAX_VALUE ? -1 : min;
	}

	/**
	 * Returns the longest prefix that is found starting at position <code>pos</code> in the <code>queryString</code>
	 *
	 * @param queryString the query string
	 * @param pos         the position at which to start looking
	 * @return the longest prefixIri that matches fully (no tie-break for duplicates)
	 */
	private Prefix findMatchingPrefix(String queryString, int pos) {
		return elements
				.stream()
				.filter(p -> queryString.startsWith(getIRIStringFromPrefix(p), pos))
				.filter(p -> isContinuationALocalName(
						queryString.substring(pos + p.getIri().getQueryString().length() - 2)))
				.reduce((r, l) -> r.getIri().getQueryString().length() > l.getIri().getQueryString().length() ? r : l)
				.orElse(null);
	}

	private boolean isContinuationALocalName(String continuation) {
		String localNameCandiate = continuation.substring(0, findNextWhitespace(continuation));
		return URIUtil.isValidLocalName(localNameCandiate);
	}

	private int findNextWhitespace(String continuation) {
		int i = 0;
		while (i < continuation.length()) {
			char cur = continuation.charAt(i);
			if (Character.isWhitespace(cur)) {
				break;
			}
			if ('>' == cur) {
				break;
			}
			i++;
		}
		return i;
	}

	private String getIRIStringFromPrefix(Prefix p) {
		return p.getIri().getQueryString().substring(1, p.getIri().getQueryString().length() - 1);
	}
}
