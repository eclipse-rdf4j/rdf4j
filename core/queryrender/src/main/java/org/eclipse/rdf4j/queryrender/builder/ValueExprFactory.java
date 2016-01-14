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
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.Lang;
import org.eclipse.rdf4j.query.algebra.LangMatches;
import org.eclipse.rdf4j.query.algebra.Not;
import org.eclipse.rdf4j.query.algebra.Or;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;

/**
 * <p>
 * Collection of utility methods for building the various ValueExpr objects in
 * the Sesame query API.
 * </p>
 * 
 * @author Michael Grove
 * @since 2.7.0
 */
public class ValueExprFactory {

	public static LangMatches langMatches(String theVar, String theLang) {
		return new LangMatches(new Lang(new Var(theVar)), new ValueConstant(
				SimpleValueFactory.getInstance().createLiteral(theLang)));
	}

	public static Bound bound(String theVar) {
		return new Bound(new Var(theVar));
	}

	public static Not not(ValueExpr theExpr) {
		return new Not(theExpr);
	}

	public static Or or(ValueExpr theLeft, ValueExpr theRight) {
		return new Or(theLeft, theRight);
	}

	public static And and(ValueExpr theLeft, ValueExpr theRight) {
		return new And(theLeft, theRight);
	}

	public static Compare lt(String theVar, String theOtherVar) {
		return compare(theVar, theOtherVar, Compare.CompareOp.LT);
	}

	public static Compare lt(String theVar, Value theValue) {
		return compare(theVar, theValue, Compare.CompareOp.LT);
	}

	public static Compare gt(String theVar, String theOtherVar) {
		return compare(theVar, theOtherVar, Compare.CompareOp.GT);
	}

	public static Compare gt(String theVar, Value theValue) {
		return compare(theVar, theValue, Compare.CompareOp.GT);
	}

	public static Compare eq(String theVar, String theOtherVar) {
		return compare(theVar, theOtherVar, Compare.CompareOp.EQ);
	}

	public static Compare eq(String theVar, Value theValue) {
		return compare(theVar, theValue, Compare.CompareOp.EQ);
	}

	public static Compare ne(String theVar, String theOtherVar) {
		return compare(theVar, theOtherVar, Compare.CompareOp.NE);
	}

	public static Compare ne(String theVar, Value theValue) {
		return compare(theVar, theValue, Compare.CompareOp.NE);
	}

	public static Compare le(String theVar, String theOtherVar) {
		return compare(theVar, theOtherVar, Compare.CompareOp.LE);
	}

	public static Compare le(String theVar, Value theValue) {
		return compare(theVar, theValue, Compare.CompareOp.LE);
	}

	public static Compare ge(String theVar, String theOtherVar) {
		return compare(theVar, theOtherVar, Compare.CompareOp.GE);
	}

	public static Compare ge(String theVar, Value theValue) {
		return compare(theVar, theValue, Compare.CompareOp.GE);
	}

	private static Compare compare(String theVar, Value theValue, Compare.CompareOp theOp) {
		return compare(new Var(theVar), new ValueConstant(theValue), theOp);
	}

	private static Compare compare(String theVar, String theValue, Compare.CompareOp theOp) {
		return compare(new Var(theVar), new Var(theValue), theOp);
	}

	private static Compare compare(ValueExpr theLeft, ValueExpr theRight, Compare.CompareOp theOp) {
		return new Compare(theLeft, theRight, theOp);
	}
}
