/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.queryrender.builder;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.And;
import org.eclipse.rdf4j.query.algebra.Bound;
import org.eclipse.rdf4j.query.algebra.LangMatches;
import org.eclipse.rdf4j.query.algebra.Not;
import org.eclipse.rdf4j.query.algebra.Or;
import org.eclipse.rdf4j.query.algebra.Regex;
import org.eclipse.rdf4j.query.algebra.SameTerm;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.parser.ParsedQuery;

/**
 * <p>
 * Builder class for creating a filter expression in a query.
 * </p>
 * 
 * @author Michael Grove
 * @deprecated use {@link org.eclipse.rdf4j.sparqlbuilder} instead.
 */
@Deprecated
public class FilterBuilder<T extends ParsedQuery, E extends SupportsGroups> {

	// TODO: merge this somehow with ValueExprFactory

	private GroupBuilder<T, E> mGroup;

	FilterBuilder(final GroupBuilder<T, E> theGroup) {
		mGroup = theGroup;
	}

	public GroupBuilder<T, E> filter(ValueExpr theExpr) {
		((BasicGroup) mGroup.getGroup()).addFilter(theExpr);

		return mGroup;
	}

	public GroupBuilder<T, E> bound(String theVar) {
		return filter(new Bound(new Var(theVar)));
	}

	public GroupBuilder<T, E> not(ValueExpr theExpr) {
		return filter(new Not(theExpr));
	}

	public GroupBuilder<T, E> and(ValueExpr theLeft, ValueExpr theRight) {
		return filter(new And(theLeft, theRight));
	}

	public GroupBuilder<T, E> or(ValueExpr theLeft, ValueExpr theRight) {
		return filter(new Or(theLeft, theRight));
	}

	public GroupBuilder<T, E> sameTerm(ValueExpr theLeft, ValueExpr theRight) {
		return filter(new SameTerm(theLeft, theRight));
	}

	public GroupBuilder<T, E> regex(ValueExpr theExpr, String thePattern) {
		return regex(theExpr, thePattern, null);
	}

	public GroupBuilder<T, E> langMatches(ValueExpr theLeft, ValueExpr theRight) {
		return filter(new LangMatches(theLeft, theRight));
	}

	public GroupBuilder<T, E> lt(String theVar, String theOtherVar) {
		return filter(ValueExprFactory.lt(theVar, theOtherVar));
	}

	public GroupBuilder<T, E> lt(String theVar, Value theValue) {
		return filter(ValueExprFactory.lt(theVar, theValue));
	}

	public GroupBuilder<T, E> gt(String theVar, String theOtherVar) {
		return filter(ValueExprFactory.gt(theVar, theOtherVar));
	}

	public GroupBuilder<T, E> gt(String theVar, Value theValue) {
		return filter(ValueExprFactory.gt(theVar, theValue));
	}

	public GroupBuilder<T, E> eq(String theVar, String theOtherVar) {
		return filter(ValueExprFactory.eq(theVar, theOtherVar));
	}

	public GroupBuilder<T, E> eq(String theVar, Value theValue) {
		return filter(ValueExprFactory.eq(theVar, theValue));
	}

	public GroupBuilder<T, E> ne(String theVar, String theOtherVar) {
		return filter(ValueExprFactory.ne(theVar, theOtherVar));
	}

	public GroupBuilder<T, E> ne(String theVar, Value theValue) {
		return filter(ValueExprFactory.ne(theVar, theValue));
	}

	public GroupBuilder<T, E> le(String theVar, String theOtherVar) {
		return filter(ValueExprFactory.le(theVar, theOtherVar));
	}

	public GroupBuilder<T, E> le(String theVar, Value theValue) {
		return filter(ValueExprFactory.le(theVar, theValue));
	}

	public GroupBuilder<T, E> ge(String theVar, String theOtherVar) {
		return filter(ValueExprFactory.ge(theVar, theOtherVar));
	}

	public GroupBuilder<T, E> ge(String theVar, Value theValue) {
		return filter(ValueExprFactory.ge(theVar, theValue));
	}

	public GroupBuilder<T, E> regex(ValueExpr theExpr, String thePattern, String theFlags) {
		Regex aRegex = new Regex();
		aRegex.setArg(theExpr);
		aRegex.setPatternArg(new ValueConstant(SimpleValueFactory.getInstance().createLiteral(thePattern)));
		if (theFlags != null) {
			aRegex.setFlagsArg(new ValueConstant(SimpleValueFactory.getInstance().createLiteral(theFlags)));
		}

		return filter(aRegex);
	}
}
