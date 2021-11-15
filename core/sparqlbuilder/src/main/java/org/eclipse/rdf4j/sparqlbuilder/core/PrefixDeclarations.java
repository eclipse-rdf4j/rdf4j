/*******************************************************************************
 Copyright (c) 2018 Eclipse RDF4J contributors.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the Eclipse Distribution License v1.0
 which accompanies this distribution, and is available at
 http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.core;

/**
 * A collection of SPARQL Prefix declarations
 *
 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#prefNames"> SPARQL Prefix</a>
 */
public class PrefixDeclarations extends StandardQueryElementCollection<Prefix> {
	/**
	 * Add prefix declarations to this collection
	 *
	 * @param prefixes
	 * @return this
	 */
	public PrefixDeclarations addPrefix(Prefix... prefixes) {
		addElements(prefixes);

		return this;
	}

	public String replaceInQuery(String queryString) {

		StringBuilder sb = new StringBuilder();
		int pos = 0;
		int lastPos = 0;
		while (pos != -1 && pos < queryString.length()) {
			pos = queryString.indexOf('<', lastPos);
			if (pos == -1) {
				break;
			}
			if (pos >= queryString.length() - 1) {
				break;
			}
			sb.append(queryString, lastPos, pos);
			Prefix matchingPrefix = findMatchingPrefix(queryString, pos + 1);
			if (matchingPrefix != null) {
				int posOfClosingBracket = queryString.indexOf('>', pos);
				if (posOfClosingBracket > -1) {
					int replacementLength = matchingPrefix.getIri().getQueryString().length() - 2; // subtract 2 for '<'
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
			lastPos = pos;
		}
		if (pos == -1) {
			sb.append(queryString.substring(lastPos));
		}
		return sb.toString();
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
				.filter(p -> queryString.startsWith(
						p.getIri().getQueryString().substring(1, p.getIri().getQueryString().length() - 1), pos))
				.reduce((r, l) -> r.getIri().getQueryString().length() > l.getIri().getQueryString().length() ? r : l)
				.orElse(null);
	}
}
