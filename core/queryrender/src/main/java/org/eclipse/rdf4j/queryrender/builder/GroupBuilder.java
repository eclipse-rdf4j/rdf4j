/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.queryrender.builder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.parser.ParsedQuery;

/**
 * <p>
 * Builder for creating a grouped set of query atoms and filters in a query.
 * </p>
 * 
 * @author Michael Grove
 * @since 2.7.0
 */
public class GroupBuilder<T extends ParsedQuery, E extends SupportsGroups> {

	private E mBuilder;

	private BasicGroup mGroup;

	private BasicGroup mParent;

	private StatementPattern.Scope mScope = StatementPattern.Scope.DEFAULT_CONTEXTS;

	private Var mContext = null;

	public GroupBuilder(final E theBuilder) {
		this(theBuilder, false, null);
	}

	public GroupBuilder(final E theBuilder, boolean theOptional) {
		this(theBuilder, theOptional, null);
	}

	public int size() {
		return mGroup.size();
	}

	public GroupBuilder(final E theBuilder, boolean theOptional, BasicGroup theParent) {
		mBuilder = theBuilder;
		mGroup = new BasicGroup(theOptional);

		if (theParent == null) {
			if (mBuilder != null) {
				mBuilder.addGroup(mGroup);
			}
		}
		else {
			mParent = theParent;
			theParent.addChild(mGroup);
		}
	}

	public Group getGroup() {
		return mGroup;
	}

	public GroupBuilder<T, E> group() {
		return new GroupBuilder<T, E>(mBuilder, false, mGroup);
	}

	public GroupBuilder<T, E> optional() {
		return new GroupBuilder<T, E>(mBuilder, true, mGroup);
	}

	public E closeGroup() {
		if (mGroup.isEmpty()) {
			if (mParent != null) {
				mParent.removeChild(mGroup);
			}
			else {
				mBuilder.removeGroup(mGroup);
			}
		}

		return mBuilder;
	}

	public UnionBuilder<T, E> union() {
		UnionBuilder<T, E> aBuilder = new UnionBuilder<T, E>(this);

		mGroup.addChild(aBuilder);

		return aBuilder;
	}

	public GroupBuilder<T, E> setScope(StatementPattern.Scope theScope) {
		mScope = theScope;

		for (StatementPattern aPattern : mGroup.getPatterns()) {
			aPattern.setScope(mScope);
		}

		return this;
	}

	public GroupBuilder<T, E> setContext(String theContextVar) {
		mContext = new Var(theContextVar);
		return this;
	}

	public GroupBuilder<T, E> setContext(Value theContextValue) {
		mContext = valueToVar(theContextValue);

		for (StatementPattern aPattern : mGroup.getPatterns()) {
			aPattern.setContextVar(mContext);
		}

		return this;
	}

	public FilterBuilder<T, E> filter() {
		return new FilterBuilder<T, E>(this);
	}

	public GroupBuilder<T, E> filter(ValueExpr theExpr) {
		mGroup.addFilter(theExpr);

		return this;
	}

	public GroupBuilder<T, E> filter(String theVar, Compare.CompareOp theOp, Value theValue) {
		Compare aComp = new Compare(new Var(theVar), new ValueConstant(theValue), theOp);
		mGroup.addFilter(aComp);

		return this;
	}

	public GroupBuilder<T, E> atom(StatementPattern thePattern) {
		return addPattern(thePattern);
	}

	public GroupBuilder<T, E> atom(StatementPattern... thePatterns) {
		return atoms(new HashSet<StatementPattern>(Arrays.asList(thePatterns)));
	}

	public GroupBuilder<T, E> atoms(Set<StatementPattern> thePatterns) {
		for (StatementPattern aPattern : thePatterns) {
			aPattern.setContextVar(mContext);
			aPattern.setScope(mScope);
		}

		mGroup.addAll(thePatterns);

		return this;
	}

	public GroupBuilder<T, E> atom(String theSubjVar, String thePredVar, String theObjVar) {
		return addPattern(newPattern(new Var(theSubjVar), new Var(thePredVar), new Var(theObjVar)));
	}

	public GroupBuilder<T, E> atom(String theSubjVar, String thePredVar, Value theObj) {
		return addPattern(newPattern(new Var(theSubjVar), new Var(thePredVar), valueToVar(theObj)));
	}

	public GroupBuilder<T, E> atom(String theSubjVar, Value thePredVar, String theObj) {
		return addPattern(newPattern(new Var(theSubjVar), valueToVar(thePredVar), new Var(theObj)));
	}

	public GroupBuilder<T, E> atom(String theSubjVar, Value thePred, Value theObj) {
		return addPattern(newPattern(new Var(theSubjVar), valueToVar(thePred), valueToVar(theObj)));
	}

	public GroupBuilder<T, E> atom(Value theSubjVar, Value thePredVar, Value theObj) {
		return addPattern(newPattern(valueToVar(theSubjVar), valueToVar(thePredVar), valueToVar(theObj)));
	}

	public GroupBuilder<T, E> atom(Value theSubjVar, Value thePredVar, String theObj) {
		return addPattern(newPattern(valueToVar(theSubjVar), valueToVar(thePredVar), new Var(theObj)));
	}

	public GroupBuilder<T, E> atom(Value theSubjVar, String thePredVar, String theObj) {
		return addPattern(newPattern(valueToVar(theSubjVar), new Var(thePredVar), new Var(theObj)));
	}

	private GroupBuilder<T, E> addPattern(StatementPattern thePattern) {
		thePattern.setContextVar(mContext);
		thePattern.setScope(mScope);

		mGroup.add(thePattern);

		return this;
	}

	private StatementPattern newPattern(Var theSubj, Var thePred, Var theObj) {
		return new StatementPattern(mScope, theSubj, thePred, theObj, mContext);
	}

	static int cnt = 0;

	public static Var valueToVar(Value theValue) {
		Var aVar = new Var("var" + cnt++, theValue);
		aVar.setAnonymous(true);

		return aVar;
	}
}
