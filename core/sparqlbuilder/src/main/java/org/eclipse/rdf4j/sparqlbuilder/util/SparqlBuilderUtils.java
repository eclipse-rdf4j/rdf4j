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

package org.eclipse.rdf4j.sparqlbuilder.util;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.eclipse.rdf4j.sparqlbuilder.core.QueryElement;

/**
 * Utility functions for the SparqlBuilder
 *
 */
public class SparqlBuilderUtils {
	private static final String PAD = " ";

	public static <O> Optional<O> getOrCreateAndModifyOptional(Optional<O> optional, Supplier<O> getter,
			UnaryOperator<O> operator) {
		return Optional.of(operator.apply(optional.orElseGet(getter)));
	}

	public static void appendAndNewlineIfPresent(Optional<? extends QueryElement> elementOptional,
			StringBuilder builder) {
		appendQueryElementIfPresent(elementOptional, builder, null, "\n");
	}

	public static void appendQueryElementIfPresent(Optional<? extends QueryElement> queryElementOptional,
			StringBuilder builder, String prefix, String suffix) {
		appendStringIfPresent(queryElementOptional.map(QueryElement::getQueryString), builder, prefix, suffix);
	}

	public static void appendStringIfPresent(Optional<String> stringOptional, StringBuilder builder, String prefix,
			String suffix) {
		Optional<String> preOpt = Optional.ofNullable(prefix);
		Optional<String> sufOpt = Optional.ofNullable(suffix);

		stringOptional.ifPresent(string -> {
			preOpt.ifPresent(builder::append);
			builder.append(string);
			sufOpt.ifPresent(builder::append);
		});
	}

	public static String getBracedString(String contents) {
		return getEnclosedString("{", "}", contents);
	}

	public static String getBracketedString(String contents) {
		return getEnclosedString("[", "]", contents);
	}

	public static String getParenthesizedString(String contents) {
		return getEnclosedString("(", ")", contents);
	}

	public static String getQuotedString(String contents) {
		return getEnclosedString("\"", "\"", contents, false);
	}

	/**
	 * For string literals that contain single- or double-quotes
	 *
	 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#QSynLiterals"> RDF Literal Syntax</a>
	 * @param contents
	 * @return a "long quoted" string
	 */
	public static String getLongQuotedString(String contents) {
		return getEnclosedString("'''", "'''", contents, false);
	}

	private static String getEnclosedString(String open, String close, String contents) {
		return getEnclosedString(open, close, contents, true);
	}

	private static String getEnclosedString(String open, String close, String contents, boolean pad) {
		StringBuilder es = new StringBuilder();

		es.append(open);
		if (contents != null && !contents.isEmpty()) {
			es.append(contents);
			if (pad) {
				es.insert(open.length(), PAD).append(PAD);
			}
		}
		es.append(close);

		return es.toString();
	}

	/**
	 * Escape the specified String value according to the SPARQL 1.1 Spec
	 * https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#grammarEscapes
	 *
	 * Note that there is no special handling for Codepoint escape sequences as described by
	 * https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#codepointEscape
	 *
	 * @param value The String to escape
	 * @return the escaped String
	 */
	public static String getEscapedString(String value) {
		if (value == null) {
			return null;
		}
		return value
				.replace("\\", "\\\\")
				.replace("\n", "\\n")
				.replace("\t", "\\t")
				.replace("\b", "\\b")
				.replace("\r", "\\r")
				.replace("\f", "\\f")
				.replace("\"", "\\\"")
				.replace("'", "\\'");
	}
}
