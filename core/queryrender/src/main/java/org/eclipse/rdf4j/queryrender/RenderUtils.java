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
package org.eclipse.rdf4j.queryrender;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Literals;

/**
 * Utility methods for rendering (parts of) SPARQL query strings.
 *
 * @author Michael Grove
 */
public final class RenderUtils {

	/**
	 * No instances
	 */
	private RenderUtils() {
	}

	/**
	 * Return the SPARQL query string rendering of the {@link org.eclipse.rdf4j.model.Value}
	 *
	 * @param theValue the value to render
	 * @return the value rendered in its SPARQL query string representation
	 */
	public static String toSPARQL(Value theValue) {
		StringBuilder aBuffer = toSPARQL(theValue, new StringBuilder());
		return aBuffer.toString();
	}

	/**
	 * Append the SPARQL query string rendering of the {@link org.eclipse.rdf4j.model.Value} to the supplied
	 * {@link StringBuilder}.
	 *
	 * @param value   the value to render
	 * @param builder the {@link StringBuilder} to append to
	 * @return the original {@link StringBuilder} with the value appended.
	 */
	public static StringBuilder toSPARQL(Value value, StringBuilder builder) {
		if (value instanceof IRI) {
			IRI aURI = (IRI) value;
			builder.append("<").append(aURI.toString()).append(">");
		} else if (value instanceof BNode) {
			builder.append("_:").append(((BNode) value).getID());
		} else if (value instanceof Literal) {
			Literal aLit = (Literal) value;

			builder.append("\"\"\"").append(escape(aLit.getLabel())).append("\"\"\"");

			if (Literals.isLanguageLiteral(aLit)) {
				aLit.getLanguage().ifPresent(lang -> builder.append("@").append(lang));
			} else {
				builder.append("^^<").append(aLit.getDatatype().toString()).append(">");
			}
		}

		return builder;
	}

	/**
	 * Properly escape out any special characters in the query string. Replaces unescaped double quotes with \" and
	 * replaces slashes '\' which are not a valid escape sequence such as \t or \n with a double slash '\\' so they are
	 * unescaped correctly by a SPARQL parser.
	 *
	 * @param theString the query string to escape chars in
	 * @return the escaped query string
	 */
	public static String escape(String theString) {
		theString = theString.replaceAll("\"", "\\\\\"");

		StringBuffer aBuffer = new StringBuffer();
		Matcher aMatcher = Pattern.compile("\\\\([^tnrbf\"'\\\\])").matcher(theString);
		while (aMatcher.find()) {
			aMatcher.appendReplacement(aBuffer, String.format("\\\\\\\\%s", aMatcher.group(1)));
		}
		aMatcher.appendTail(aBuffer);

		return aBuffer.toString();
	}
}
