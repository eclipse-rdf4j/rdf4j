/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sparql.query;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLUtil;

/**
 * Utility class to perfom query string manipulations as used in
 * {@link SPARQLTupleQuery}, {@link SPARQLGraphQuery} and
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
	 * Retrieve a modified queryString into which all bindings of the given
	 * argument are replaced.
	 * 
	 * @param queryString
	 * @param bindings
	 * @return the modified queryString
	 */
	public static String getQueryString(String queryString, BindingSet bindings) {
		if (bindings.size() == 0) {
			return queryString;
		}

		String qry = queryString;
		int b = qry.indexOf('{');
		String select = qry.substring(0, b);
		String where = qry.substring(b);
		for (String name : bindings.getBindingNames()) {
			String replacement = getReplacement(bindings.getValue(name));
			if (replacement != null) {
				String pattern = "[\\?\\$]" + name + "(?=\\W)";
				select = select.replaceAll(pattern, "(" + Matcher.quoteReplacement(replacement) + " as ?" + name
						+ ")");

				// we use Matcher.quoteReplacement to make sure things like newlines
				// in literal values
				// are preserved
				where = where.replaceAll(pattern, Matcher.quoteReplacement(replacement));
			}
		}
		return select + where;
	}

	private static String getReplacement(Value value) {
		StringBuilder sb = new StringBuilder();
		if (value instanceof IRI) {
			return appendValue(sb, (IRI)value).toString();
		}
		else if (value instanceof Literal) {
			return appendValue(sb, (Literal)value).toString();
		}
		else {
			throw new IllegalArgumentException("BNode references not supported by SPARQL end-points");
		}
	}

	private static StringBuilder appendValue(StringBuilder sb, IRI uri) {
		sb.append("<").append(uri.stringValue()).append(">");
		return sb;
	}

	private static StringBuilder appendValue(StringBuilder sb, Literal lit) {
		sb.append('"');
		sb.append(SPARQLUtil.encodeString(lit.getLabel()));
		sb.append('"');

		if (Literals.isLanguageLiteral(lit)) {
			sb.append('@');
			sb.append(lit.getLanguage().get());
		}
		else {
			sb.append("^^<");
			sb.append(lit.getDatatype().stringValue());
			sb.append('>');
		}
		return sb;
	}
}
