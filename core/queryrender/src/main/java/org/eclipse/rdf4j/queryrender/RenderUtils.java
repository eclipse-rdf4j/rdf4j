/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
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
 * Utility methods for rendering (parts of) SeRQL and SPARQL query strings.
 * 
 * @author Michael Grove
 * @since 2.7.0
 */
public final class RenderUtils {

	/**
	 * No instances
	 */
	private RenderUtils() {
	}

	/**
	 * Return the query string rendering of the {@link org.eclipse.rdf4j.model.Value}
	 * 
	 * @param theValue
	 *        the value to render
	 * @return the value rendered in its query string representation
	 * @deprecated since 2.8.0. Use {@link #toSPARQL(Value)} instead.
	 */
	@Deprecated
	public static String getSPARQLQueryString(Value theValue) {
		return toSPARQL(theValue);
	}

	/**
	 * Return the SPARQL query string rendering of the
	 * {@link org.eclipse.rdf4j.model.Value}
	 * 
	 * @param theValue
	 *        the value to render
	 * @return the value rendered in its SPARQL query string representation
	 * @since 2.8.0
	 */
	public static String toSPARQL(Value theValue) {
		StringBuilder aBuffer = toSPARQL(theValue, new StringBuilder());
		return aBuffer.toString();
	}

	/**
	 * Append the SPARQL query string rendering of the
	 * {@link org.eclipse.rdf4j.model.Value} to the supplied {@link StringBuilder}.
	 * 
	 * @param value
	 *        the value to render
	 * @param builder
	 *        the {@link StringBuilder} to append to
	 * @return the original {@link StringBuilder} with the value appended.
	 * @since 2.8.0
	 */
	public static StringBuilder toSPARQL(Value value, StringBuilder builder) {
		if (value instanceof IRI) {
			IRI aURI = (IRI)value;
			builder.append("<").append(aURI.toString()).append(">");
		}
		else if (value instanceof BNode) {
			builder.append("_:").append(((BNode)value).getID());
		}
		else if (value instanceof Literal) {
			Literal aLit = (Literal)value;

			builder.append("\"\"\"").append(escape(aLit.getLabel())).append("\"\"\"");

			if (Literals.isLanguageLiteral(aLit)) {
				builder.append("@").append(aLit.getLanguage());
			}
			else {
				builder.append("^^<").append(aLit.getDatatype().toString()).append(">");
			}
		}

		return builder;
	}

	/**
	 * Return the query string rendering of the {@link Value}
	 * 
	 * @param theValue
	 *        the value to render
	 * @return the value rendered in its query string representation
	 * @deprecated since 2.8.0. Use {{@link #toSeRQL(Value)} instead.
	 */
	@Deprecated
	public static String getSerqlQueryString(Value theValue) {
		return toSeRQL(theValue);
	}

	/**
	 * Return the query string rendering of the {@link Value}
	 * 
	 * @param theValue
	 *        the value to render
	 * @return the value rendered in its query string representation
	 * @since 2.8.0
	 */
	public static String toSeRQL(Value theValue) {
		StringBuilder aBuffer = new StringBuilder();

		if (theValue instanceof IRI) {
			IRI aURI = (IRI)theValue;
			aBuffer.append("<").append(aURI.toString()).append(">");
		}
		else if (theValue instanceof BNode) {
			aBuffer.append("_:").append(((BNode)theValue).getID());
		}
		else if (theValue instanceof Literal) {
			Literal aLit = (Literal)theValue;

			aBuffer.append("\"").append(escape(aLit.getLabel())).append("\"");

			if (Literals.isLanguageLiteral(aLit)) {
				aBuffer.append("@").append(aLit.getLanguage());
			}
			else {
				aBuffer.append("^^<").append(aLit.getDatatype().toString()).append(">");
			}
		}

		return aBuffer.toString();
	}

	/**
	 * Properly escape out any special characters in the query string. Replaces
	 * unescaped double quotes with \" and replaces slashes '\' which are not a
	 * valid escape sequence such as \t or \n with a double slash '\\' so they
	 * are unescaped correctly by a SPARQL parser.
	 * 
	 * @param theString
	 *        the query string to escape chars in
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
