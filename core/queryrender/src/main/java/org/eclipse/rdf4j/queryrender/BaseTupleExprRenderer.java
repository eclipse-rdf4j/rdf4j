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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.algebra.Distinct;
import org.eclipse.rdf4j.query.algebra.ExtensionElem;
import org.eclipse.rdf4j.query.algebra.OrderElem;
import org.eclipse.rdf4j.query.algebra.ProjectionElem;
import org.eclipse.rdf4j.query.algebra.ProjectionElemList;
import org.eclipse.rdf4j.query.algebra.Reduced;
import org.eclipse.rdf4j.query.algebra.Slice;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.parser.ParsedQuery;

/**
 * <p>
 * Base class for rendering RDF4J query API objects into strings.
 * </p>
 *
 * @author Michael Grove
 */
public abstract class BaseTupleExprRenderer extends AbstractQueryModelVisitor<Exception> {

	/**
	 * A map of the extensions specified in the query.
	 */
	protected Map<String, ValueExpr> mExtensions = new HashMap<>();

	/**
	 * The list of elements include in the projection of the query
	 */
	protected List<ProjectionElemList> mProjection = new ArrayList<>();

	/**
	 * The elements specified in the order by clause of the query
	 */
	protected List<OrderElem> mOrdering = new ArrayList<>();

	/**
	 * Whether or not the query is distinct
	 */
	protected boolean mDistinct = false;

	/**
	 * Whether or not the query is reduced
	 */
	protected boolean mReduced = false;

	/**
	 * The limit of results for the query, or -1 for no limit
	 */
	protected long mLimit = -1;

	/**
	 * The query offset, or -1 for no offset
	 */
	protected long mOffset = -1;

	/**
	 * Reset the state of the renderer
	 */
	public void reset() {
		mLimit = mOffset = -1;
		mDistinct = mReduced = false;

		mExtensions.clear();
		mOrdering.clear();
		mProjection.clear();
	}

	public Map<String, ValueExpr> getExtensions() {
		return mExtensions;
	}

	public List<ProjectionElemList> getProjection() {
		return mProjection;
	}

	public List<OrderElem> getOrdering() {
		return mOrdering;
	}

	public boolean isDistinct() {
		return mDistinct;
	}

	public boolean isReduced() {
		return mReduced;
	}

	public long getLimit() {
		return mLimit;
	}

	public long getOffset() {
		return mOffset;
	}

	/**
	 * Render the ParsedQuery as a query string
	 *
	 * @param theQuery the parsed query to render
	 * @return the query object rendered in the query language syntax
	 * @throws Exception if there is an error while rendering
	 */
	public String render(ParsedQuery theQuery) throws Exception {
		return render(theQuery.getTupleExpr());
	}

	/**
	 * Render the TupleExpr as a query or query fragment depending on what kind of TupleExpr it is
	 *
	 * @param theExpr the expression to render
	 * @return the TupleExpr rendered in the query language syntax
	 * @throws Exception if there is an error while rendering
	 */
	public abstract String render(TupleExpr theExpr) throws Exception;

	/**
	 * Render the given ValueExpr
	 *
	 * @param theExpr the expr to render
	 * @return the rendered expression
	 * @throws Exception if there is an error while rendering
	 */
	protected abstract String renderValueExpr(final ValueExpr theExpr) throws Exception;

	/**
	 * Turn a ProjectionElemList for a construct query projection (three elements aliased as 'subject', 'predicate' and
	 * 'object' in that order) into a StatementPattern.
	 *
	 * @param theList the elem list to render
	 * @return the elem list for a construct projection as a statement pattern
	 * @throws Exception if there is an exception while rendering
	 */
	public StatementPattern toStatementPattern(ProjectionElemList theList) throws Exception {
		ProjectionElem aSubj = theList.getElements().get(0);
		ProjectionElem aPred = theList.getElements().get(1);
		ProjectionElem aObj = theList.getElements().get(2);

		return new StatementPattern(
				mExtensions.containsKey(aSubj.getName())
						? new Var(scrubVarName(aSubj.getName()), asValue(mExtensions.get(aSubj.getName())))
						: new Var(scrubVarName(aSubj.getName())),
				mExtensions.containsKey(aPred.getName())
						? new Var(scrubVarName(aPred.getName()), asValue(mExtensions.get(aPred.getName())))
						: new Var(scrubVarName(aPred.getName())),
				mExtensions.containsKey(aObj.getName())
						? new Var(scrubVarName(aObj.getName()), asValue(mExtensions.get(aObj.getName())))
						: new Var(scrubVarName(aObj.getName())));
	}

	/**
	 * Scrub any illegal characters out of the variable name
	 *
	 * @param theName the potential variable name
	 * @return the name scrubbed of any illegal characters
	 */
	public static String scrubVarName(String theName) {
		return theName.replace("-", "");
	}

	/**
	 * Return the {@link ValueExpr} as a {@link Value} if possible.
	 *
	 * @param theValue the ValueExpr to convert
	 * @return the expression as a Value, or null if it cannot be converted
	 * @throws Exception if there is an error converting to a Value
	 */
	private Value asValue(ValueExpr theValue) throws Exception {
		if (theValue instanceof ValueConstant) {
			return ((ValueConstant) theValue).getValue();
		} else if (theValue instanceof Var) {
			Var aVar = (Var) theValue;
			if (aVar.hasValue()) {
				return aVar.getValue();
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	/**
	 * Returns whether or not the results of scanning the query model indicates that this represents a select query
	 *
	 * @return true if its a select query, false if its a construct query
	 */
	protected boolean isSelect() {
		boolean aIsSelect = false;

		for (ProjectionElemList aList : mProjection) {
			if (!isSPOElemList(aList)) {
				aIsSelect = true;
				break;
			}
		}

		return aIsSelect;
	}

	/**
	 * Return whether or not this projection looks like an spo binding for a construct query
	 *
	 * @param theList the projection element list to inspect
	 * @return true if it has the format of a spo construct projection element, false otherwise
	 */
	public static boolean isSPOElemList(ProjectionElemList theList) {
		return theList.getElements().size() == 3
				&& theList.getElements().get(0).getProjectionAlias().get().equalsIgnoreCase("subject")
				&& theList.getElements().get(1).getProjectionAlias().get().equalsIgnoreCase("predicate")
				&& theList.getElements().get(2).getProjectionAlias().get().equalsIgnoreCase("object");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(final StatementPattern theStatementPattern) throws Exception {
		theStatementPattern.visitChildren(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(final Slice theSlice) throws Exception {
		if (theSlice.hasOffset()) {
			mOffset = theSlice.getOffset();
		}

		if (theSlice.hasLimit()) {
			mLimit = theSlice.getLimit();
		}

		theSlice.visitChildren(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(final ExtensionElem theExtensionElem) throws Exception {
		mExtensions.put(theExtensionElem.getName(), theExtensionElem.getExpr());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(final ProjectionElemList theProjectionElemList) throws Exception {
		if (!theProjectionElemList.getElements().isEmpty()) {
			mProjection.add(theProjectionElemList.clone());
		}

		theProjectionElemList.visitChildren(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(final OrderElem theOrderElem) throws Exception {
		mOrdering.add(theOrderElem);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(final Distinct theDistinct) throws Exception {
		mDistinct = true;

		theDistinct.getArg().visit(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(final Reduced theReduced) throws Exception {
		mReduced = true;

		theReduced.visitChildren(this);
	}
}
