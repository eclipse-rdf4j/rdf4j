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
package org.eclipse.rdf4j.repository.sparql.query;

import java.util.regex.Matcher;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLQueries;

/**
 * Utility class to perfom query string manipulations as used in {@link SPARQLTupleQuery}, {@link SPARQLGraphQuery} and
 * {@link SPARQLBooleanQuery}.
 *
 * @author Andreas Schwarte
 * @see SPARQLTupleQuery
 * @see SPARQLGraphQuery
 * @see SPARQLBooleanQuery
 */
public class QueryStringUtil {

	// TODO maybe add BASE declaration here as well?

	/**
	 * Retrieve a modified queryString into which all bindings of the given argument are replaced.
	 *
	 * @param queryString
	 * @param bindings
	 * @return the modified queryString
	 * @deprecated since 2.0.use {@link #getTupleQueryString(String, BindingSet)}
	 */
	@Deprecated
	public static String getQueryString(String queryString, BindingSet bindings) {
		return getTupleQueryString(queryString, bindings);
	}

	/**
	 * Retrieve a modified queryString into which all bindings of the given argument are replaced, with the binding
	 * names included in the SELECT clause.
	 *
	 * @param queryString
	 * @param bindings
	 * @return the modified queryString
	 */
	public static String getTupleQueryString(String queryString, BindingSet bindings) {
		if (bindings.size() == 0) {
			return queryString;
		}

		String qry = queryString;
		int b = qry.indexOf('{');
		String select = qry.substring(0, b);
		String where = qry.substring(b);
		for (String name : bindings.getBindingNames()) {
			String replacement = valueToString(bindings.getValue(name));
			if (replacement != null) {
				String pattern = "[\\?\\$]" + name + "(?=\\W)";
				select = select.replaceAll(pattern, "(" + Matcher.quoteReplacement(replacement) + " as ?" + name + ")");

				// we use Matcher.quoteReplacement to make sure things like newlines
				// in literal values
				// are preserved
				where = where.replaceAll(pattern, Matcher.quoteReplacement(replacement));
			}
		}
		return select + where;
	}

	/**
	 * Retrieve a modified queryString into which all bindings of the given argument are replaced with their value.
	 *
	 * @param queryString
	 * @param bindings
	 * @return the modified queryString
	 */
	public static String getUpdateString(String queryString, BindingSet bindings) {
		return getGraphQueryString(queryString, bindings);
	}

	/**
	 * Retrieve a modified queryString into which all bindings of the given argument are replaced with their value.
	 *
	 * @param queryString
	 * @param bindings
	 * @return the modified queryString
	 */
	public static String getBooleanQueryString(String queryString, BindingSet bindings) {
		return getGraphQueryString(queryString, bindings);
	}

	/**
	 * Retrieve a modified queryString into which all bindings of the given argument are replaced with their value.
	 *
	 * @param queryString
	 * @param bindings
	 * @return the modified queryString
	 */
	public static String getGraphQueryString(String queryString, BindingSet bindings) {
		if (bindings.size() == 0) {
			return queryString;
		}

		String qry = queryString;
		for (String name : bindings.getBindingNames()) {
			String replacement = valueToString(bindings.getValue(name));
			if (replacement != null) {
				String pattern = "[\\?\\$]" + name + "(?=\\W)";
				// we use Matcher.quoteReplacement to make sure things like newlines
				// in literal values are preserved
				qry = qry.replaceAll(pattern, Matcher.quoteReplacement(replacement));
			}
		}
		return qry;
	}

	/**
	 * Converts a value to its SPARQL string representation.
	 *
	 * Null will be converted to UNDEF (may be used in VALUES only).
	 *
	 * @param value the value to convert
	 * @return the converted value as a string
	 */
	public static String valueToString(Value value) {
		return appendValueAsString(new StringBuilder(), value).toString();
	}

	/**
	 * Converts a value to its SPARQL string representation and appends it to a StringBuilder.
	 *
	 * Null will be converted to UNDEF (may be used in VALUES only).
	 *
	 * @param sb    StringBuilder to append to
	 * @param value the value to convert
	 * @return the provided StringBuilder
	 */
	public static StringBuilder appendValueAsString(StringBuilder sb, Value value) {
		if (value == null) {
			return sb.append("UNDEF"); // see grammar for BINDINGs def
		} else if (value instanceof IRI) {
			return appendValue(sb, (IRI) value);
		} else if (value instanceof Literal) {
			return appendValue(sb, (Literal) value);
		} else {
			throw new IllegalArgumentException("BNode references not supported by SPARQL end-points");
		}
	}

	private static StringBuilder appendValue(StringBuilder sb, IRI uri) {
		sb.append("<").append(uri.stringValue()).append(">");
		return sb;
	}

	private static StringBuilder appendValue(StringBuilder sb, Literal lit) {
		sb.append('"');
		sb.append(SPARQLQueries.escape(lit.getLabel()));
		sb.append('"');

		if (Literals.isLanguageLiteral(lit)) {
			sb.append('@');
			sb.append(lit.getLanguage().get());
		} else if (!lit.getDatatype().equals(XSD.STRING)) {
			// Don't append type if it's xsd:string, this keeps it compatible with RDF 1.0
			sb.append("^^<");
			sb.append(lit.getDatatype().stringValue());
			sb.append('>');
		}
		return sb;
	}
}
