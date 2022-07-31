/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.util;

import java.util.List;

import org.eclipse.rdf4j.federated.algebra.ConjunctiveFilterExpr;
import org.eclipse.rdf4j.federated.algebra.FilterExpr;
import org.eclipse.rdf4j.federated.algebra.FilterValueExpr;
import org.eclipse.rdf4j.federated.exception.FilterConversionException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.algebra.And;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;

/**
 * Various utility functions to handle filter expressions.
 *
 * NOTE: currently only implemented for {@link Compare}, other filter expressions need to be added. If an unexpected
 * filter expression occurs, the filter evaluation is done locally.
 *
 * @author Andreas Schwarte
 */
public class FilterUtils {

	/**
	 * Returns a SPARQL representation of the provided expression,
	 *
	 * e.g Compare(?v, "<", 3) is converted to "?v < '3'"
	 *
	 * @param filterExpr
	 * @return the SPARQL string
	 * @throws FilterConversionException
	 */
	public static String toSparqlString(FilterValueExpr filterExpr) throws FilterConversionException {

		if (filterExpr instanceof FilterExpr) {
			return toSparqlString((FilterExpr) filterExpr);
		}
		if (filterExpr instanceof ConjunctiveFilterExpr) {
			return toSparqlString((ConjunctiveFilterExpr) filterExpr);
		}

		throw new RuntimeException("Unsupported type: " + filterExpr.getClass().getCanonicalName());
	}

	public static String toSparqlString(FilterExpr filterExpr) throws FilterConversionException {
		StringBuilder sb = new StringBuilder();
		append(filterExpr.getExpression(), sb);
		return sb.toString();
	}

	public static String toSparqlString(ConjunctiveFilterExpr filterExpr) throws FilterConversionException {

		StringBuilder sb = new StringBuilder();
		int count = 0;
		sb.append("( ");
		for (FilterExpr expr : filterExpr.getExpressions()) {
			append(expr.getExpression(), sb);
			if (++count < filterExpr.getExpressions().size()) {
				sb.append(" && ");
			}
		}
		sb.append(" )");

		return sb.toString();
	}

	public static ValueExpr toFilter(FilterValueExpr filterExpr) throws FilterConversionException {

		if (filterExpr instanceof FilterExpr) {
			return toFilter((FilterExpr) filterExpr);
		}
		if (filterExpr instanceof ConjunctiveFilterExpr) {
			return toFilter((ConjunctiveFilterExpr) filterExpr);
		}

		throw new RuntimeException("Unsupported type: " + filterExpr.getClass().getCanonicalName());
	}

	public static ValueExpr toFilter(FilterExpr filterExpr) throws FilterConversionException {
		return filterExpr.getExpression();
	}

	public static ValueExpr toFilter(ConjunctiveFilterExpr filterExpr) throws FilterConversionException {
		List<FilterExpr> expressions = filterExpr.getExpressions();

		if (expressions.size() == 2) {
			return new And(expressions.get(0).getExpression(), expressions.get(0).getExpression());
		}

		And and = new And();
		and.setLeftArg(expressions.get(0).getExpression());
		And tmp = and;
		int idx;
		for (idx = 1; idx < expressions.size() - 1; idx++) {
			And _a = new And();
			_a.setLeftArg(expressions.get(idx).getExpression());
			tmp.setRightArg(_a);
			tmp = _a;
		}
		tmp.setRightArg(expressions.get(idx).getExpression());

		return and;
	}

	protected static void append(ValueExpr expr, StringBuilder sb) throws FilterConversionException {

		if (expr instanceof Compare) {
			append((Compare) expr, sb);
		} else if (expr instanceof Var) {
			append((Var) expr, sb);
		} else if (expr instanceof ValueConstant) {
			append((ValueConstant) expr, sb);
		} else {
			// TODO add more!
			throw new FilterConversionException("Expression type not supported, fallback to sesame evaluation: "
					+ expr.getClass().getCanonicalName());
		}

	}

	protected static void append(Compare cmp, StringBuilder sb) throws FilterConversionException {

		sb.append("( ");
		append(cmp.getLeftArg(), sb);
		sb.append(" ").append(cmp.getOperator().getSymbol()).append(" ");
		append(cmp.getRightArg(), sb);
		sb.append(" )");
	}

	protected static void append(Var var, StringBuilder sb) {
		if (var.hasValue()) {
			appendValue(sb, var.getValue());
		} else {
			sb.append("?").append(var.getName());
		}
	}

	protected static void append(ValueConstant vc, StringBuilder sb) {
		appendValue(sb, vc.getValue());
	}

	protected static StringBuilder appendValue(StringBuilder sb, Value value) {

		if (value instanceof IRI) {
			return appendURI(sb, (IRI) value);
		}
		if (value instanceof Literal) {
			return appendLiteral(sb, (Literal) value);
		}

		// XXX check for other types ? BNode ?
		throw new RuntimeException("Type not supported: " + value.getClass().getCanonicalName());
	}

	protected static StringBuilder appendURI(StringBuilder sb, IRI uri) {
		sb.append("<").append(uri.stringValue()).append(">");
		return sb;
	}

	protected static StringBuilder appendLiteral(StringBuilder sb, Literal lit) {
		sb.append('\'');
		sb.append(lit.getLabel().replace("\"", "\\\""));
		sb.append('\'');

		if (lit.getLanguage().isPresent()) {
			sb.append('@');
			sb.append(lit.getLanguage());
		}

		if (lit.getDatatype() != null) {
			sb.append("^^<");
			sb.append(lit.getDatatype().stringValue());
			sb.append('>');
		}
		return sb;
	}
}
