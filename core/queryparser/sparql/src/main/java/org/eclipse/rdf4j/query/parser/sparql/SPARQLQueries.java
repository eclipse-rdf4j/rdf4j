/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.sparql;

import org.eclipse.rdf4j.common.text.StringUtil;
import org.eclipse.rdf4j.model.Namespace;

/**
 * Utility functions for working with SPARQL query strings.
 *
 * @author Jeen Broekstra
 */
public class SPARQLQueries {

	/**
	 * Creates a string representing of the supplied {@link Namespace namespaces} as SPARQL prefix declarations. This
	 * can be used when composing a SPARQL query string in code, for example:
	 *
	 * <pre>
	 * <code>
	 * String query = SPARQLQueries.getPrefixClauses(connection.getNamespaces()) + "SELECT * WHERE { ?s ex:myprop ?o }";
	 * </code>
	 * </pre>
	 *
	 * @param namespaces one or more {@link Namespace} objects.
	 * @return one or more SPARQL prefix declarations (each separated by a newline), as a String.
	 *
	 * @since 3.6.0
	 */
	public static String getPrefixClauses(Iterable<Namespace> namespaces) {
		final StringBuilder sb = new StringBuilder();
		for (Namespace namespace : namespaces) {
			sb.append(String.format("PREFIX %s: <%s>\n", namespace.getPrefix(), namespace.getName()));
		}
		return sb.toString();
	}

	/**
	 * Escape the supplied string with backslashes for any special characters, so it can be used as a string literal
	 * value in a SPARQL query.
	 *
	 * @see <a href="https://www.w3.org/TR/sparql11-query/#grammarEscapes">SPAQL 1.1 grammar escapes</a>
	 *
	 * @since 3.6.0
	 */
	public static String escape(String s) {
		s = StringUtil.gsub("\\", "\\\\", s);
		s = StringUtil.gsub("\t", "\\t", s);
		s = StringUtil.gsub("\n", "\\n", s);
		s = StringUtil.gsub("\r", "\\r", s);
		s = StringUtil.gsub("\b", "\\b", s);
		s = StringUtil.gsub("\f", "\\f", s);
		s = StringUtil.gsub("\"", "\\\"", s);
		s = StringUtil.gsub("'", "\\'", s);
		return s;
	}

	/**
	 * Un-escapes a backslash-escaped SPARQL literal value string. Any recognized \-escape sequences are substituted
	 * with their un-escaped value.
	 *
	 * @param s An SPARQL literal string with backslash escapes.
	 * @return The un-escaped string.
	 * @exception IllegalArgumentException If the supplied string is not a correctly escaped SPARQL string.
	 *
	 * @see <a href="https://www.w3.org/TR/sparql11-query/#grammarEscapes">SPAQL 1.1 grammar escapes</a>
	 *
	 * @since 3.6.0
	 */
	public static String unescape(String s) {
		int backSlashIdx = s.indexOf('\\');

		if (backSlashIdx == -1) {
			// No escaped characters found
			return s;
		}

		int startIdx = 0;
		int sLength = s.length();
		StringBuilder sb = new StringBuilder(sLength);

		while (backSlashIdx != -1) {
			sb.append(s, startIdx, backSlashIdx);

			if (backSlashIdx + 1 >= sLength) {
				throw new IllegalArgumentException("Unescaped backslash in: " + s);
			}

			char c = s.charAt(backSlashIdx + 1);
			switch (c) {
			case 't':
				sb.append('\t');
				startIdx = backSlashIdx + 2;
				break;
			case 'n':
				sb.append('\n');
				startIdx = backSlashIdx + 2;
				break;
			case 'r':
				sb.append('\r');
				startIdx = backSlashIdx + 2;
				break;
			case 'b':
				sb.append('\b');
				startIdx = backSlashIdx + 2;
				break;
			case 'f':
				sb.append('\f');
				startIdx = backSlashIdx + 2;
				break;
			case '"':
				sb.append('"');
				startIdx = backSlashIdx + 2;
				break;
			case '\'':
				sb.append('\'');
				startIdx = backSlashIdx + 2;
				break;
			case '\\':
				sb.append('\\');
				startIdx = backSlashIdx + 2;
				break;
			default:
				throw new IllegalArgumentException("Unescaped backslash in: " + s);
			}

			backSlashIdx = s.indexOf('\\', startIdx);
		}

		sb.append(s.substring(startIdx));

		return sb.toString();
	}
}
