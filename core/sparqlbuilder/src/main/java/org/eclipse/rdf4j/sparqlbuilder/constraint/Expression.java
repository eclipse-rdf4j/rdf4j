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

package org.eclipse.rdf4j.sparqlbuilder.constraint;

import java.util.ArrayList;
import java.util.function.Function;

import org.eclipse.rdf4j.sparqlbuilder.core.Assignable;
import org.eclipse.rdf4j.sparqlbuilder.core.Groupable;
import org.eclipse.rdf4j.sparqlbuilder.core.Orderable;
import org.eclipse.rdf4j.sparqlbuilder.core.StandardQueryElementCollection;
import org.eclipse.rdf4j.sparqlbuilder.util.SparqlBuilderUtils;

/**
 * A SPARQL expression. Used by filters, having clauses, order and group by clauses, assignments, and as arguments to
 * other expressions.
 *
 * @param <T> the type of Expression (ie, Function or Operation). Used to support fluency
 *
 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#termConstraint">SPARQL Filters</a>
 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#having"> SPARQL HAVING</a>
 * @see <a href= "http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#modOrderBy" >SPARQL ORDER BY</a>
 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#groupby"> SPARQL GROUP BY</a>
 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#assignment"> SPARQL Assignments</a>
 */
public abstract class Expression<T extends Expression<T>> extends StandardQueryElementCollection<Operand>
		implements Operand, Orderable, Groupable, Assignable {
	private static final Function<String, String> WRAPPER = SparqlBuilderUtils::getParenthesizedString;

	protected SparqlOperator operator;

	Expression(SparqlOperator operator) {
		this(operator, " " + operator.getQueryString() + " ");
	}

	Expression(SparqlOperator operator, String delimeter) {
		super(delimeter, new ArrayList<>());
		this.operator = operator;
		parenthesize(false);
	}

	@SuppressWarnings("unchecked")
	T addOperand(Operand... operands) {
		addElements(operands);

		return (T) this;
	}

	/**
	 * Indicate that this expression should be wrapped in parentheses when converted to a query string
	 *
	 * @return this
	 */
	public T parenthesize() {
		return parenthesize(true);
	}

	/**
	 * Indicate if this expression should be wrapped in parentheses when converted to a query string
	 *
	 * @param parenthesize
	 * @return this
	 */
	@SuppressWarnings("unchecked")
	public T parenthesize(boolean parenthesize) {
		if (parenthesize) {
			setWrapperMethod(WRAPPER);
		} else {
			resetWrapperMethod();
		}

		return (T) this;
	}

	Operand getOperand(int index) {
		return ((ArrayList<Operand>) elements).get(index);
	}
}
