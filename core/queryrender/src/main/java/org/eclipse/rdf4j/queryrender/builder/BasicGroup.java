/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.queryrender.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.query.algebra.And;
import org.eclipse.rdf4j.query.algebra.BinaryTupleOperator;
import org.eclipse.rdf4j.query.algebra.EmptySet;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.ValueExpr;

/**
 * <p>
 * Internal class for representing a group within a query.
 * </p>
 * 
 * @author Michael Grove
 * @deprecated use {@link org.eclipse.rdf4j.sparqlbuilder} instead.
 */
@Deprecated
public class BasicGroup implements Group {

	private boolean mIsOptional = false;

	private Collection<TupleExpr> mExpressions = new LinkedHashSet<>();

	private List<Group> mChildren = new ArrayList<>();

	private Collection<ValueExpr> mFilters = new LinkedHashSet<>();

	/**
	 * Create a new BasicGroup
	 * 
	 * @param theOptional whether or not the patterns and filters in this group are optional
	 */
	public BasicGroup(final boolean theOptional) {
		mIsOptional = theOptional;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public int size() {
		int aSize = mExpressions.size();

		for (Group aChild : mChildren) {
			aSize += aChild.size();
		}

		return aSize;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void addChild(Group theGroup) {
		mChildren.add(theGroup);
	}

	/**
	 * Remove a child from this group
	 * 
	 * @param theGroup the child to remove
	 */
	public void removeChild(Group theGroup) {
		mChildren.remove(theGroup);
	}

	/**
	 * Add a Filter to this group
	 * 
	 * @param theExpr the value filter to add
	 */
	public void addFilter(ValueExpr theExpr) {
		mFilters.add(theExpr);
	}

	public boolean isEmpty() {
		return mFilters.isEmpty() && mExpressions.isEmpty() && mChildren.isEmpty();
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public boolean isOptional() {
		return mIsOptional;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public TupleExpr expr() {
		return expr(true);
	}

	private TupleExpr expr(boolean filterExpr) {
		TupleExpr aExpr = null;

		if (mExpressions.isEmpty() && mFilters.isEmpty()) {
			if (mChildren.isEmpty()) {
				return null;
			}
		} else if (mExpressions.isEmpty() && !mFilters.isEmpty()) {
			if (mChildren.isEmpty()) {
				aExpr = new Filter(new EmptySet(), filtersAsAnd());
			}
		} else {
			aExpr = asJoin(mExpressions);

			if (filterExpr) {
				aExpr = filteredTuple(aExpr);
			}
		}

		if (!mChildren.isEmpty()) {
			for (Group aGroup : mChildren) {
				if (aExpr == null) {
					if (mExpressions.isEmpty() && !mFilters.isEmpty()) {
						aExpr = new Filter(aGroup.expr(), filtersAsAnd());
					} else {
						aExpr = aGroup.expr();
					}
				} else {
					BinaryTupleOperator aJoin = aGroup.isOptional() ? new LeftJoin() : new Join();

					aJoin.setLeftArg(aExpr);

					if (aGroup.isOptional() && aJoin instanceof LeftJoin && aGroup instanceof BasicGroup
							&& !((BasicGroup) aGroup).mFilters.isEmpty()) {

						BasicGroup aBasicGroup = (BasicGroup) aGroup;

						aJoin.setRightArg(aBasicGroup.expr(false));

						((LeftJoin) aJoin).setCondition(aBasicGroup.filtersAsAnd());
					} else {
						aJoin.setRightArg(aGroup.expr());
					}

					aExpr = aJoin;
				}

			}
		}

		return aExpr;
	}

	private TupleExpr filteredTuple(TupleExpr theExpr) {
		TupleExpr aExpr = theExpr;

		for (ValueExpr aValEx : mFilters) {
			Filter aFilter = new Filter();
			aFilter.setCondition(aValEx);
			aFilter.setArg(aExpr);
			aExpr = aFilter;
		}

		return aExpr;
	}

	private ValueExpr filtersAsAnd() {
		ValueExpr aExpr = null;

		for (ValueExpr aValEx : mFilters) {
			if (aExpr == null) {
				aExpr = aValEx;
			} else {
				And aAnd = new And();
				aAnd.setLeftArg(aValEx);
				aAnd.setRightArg(aExpr);
				aExpr = aAnd;
			}
		}

		return aExpr;
	}

	public void add(final TupleExpr theExpr) {
		mExpressions.add(theExpr);
	}

	public void addAll(final Collection<? extends TupleExpr> theTupleExprs) {
		mExpressions.addAll(theTupleExprs);
	}

	private TupleExpr asJoin(Collection<TupleExpr> theList) {
		Join aJoin = new Join();

		if (theList.isEmpty()) {
			throw new RuntimeException("Can't have an empty or missing join.");
		} else if (theList.size() == 1) {
			return theList.iterator().next();
		}

		for (TupleExpr aExpr : theList) {
			if (aJoin.getLeftArg() == null) {
				aJoin.setLeftArg(aExpr);
			} else if (aJoin.getRightArg() == null) {
				aJoin.setRightArg(aExpr);
			} else {
				Join aNewJoin = new Join();

				aNewJoin.setLeftArg(aJoin);
				aNewJoin.setRightArg(aExpr);

				aJoin = aNewJoin;
			}
		}

		return aJoin;
	}

	public Collection<StatementPattern> getPatterns() {
		Set<StatementPattern> aPatternSet = new HashSet<>();
		for (TupleExpr aExpr : mExpressions) {
			if (aExpr instanceof StatementPattern) {
				aPatternSet.add((StatementPattern) aExpr);
			}
		}
		return aPatternSet;
	}
}
