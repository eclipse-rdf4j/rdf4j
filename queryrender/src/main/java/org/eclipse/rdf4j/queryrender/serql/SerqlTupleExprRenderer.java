/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.queryrender.serql;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.rdf4j.query.algebra.And;
import org.eclipse.rdf4j.query.algebra.Difference;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.Intersection;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.OrderElem;
import org.eclipse.rdf4j.query.algebra.ProjectionElem;
import org.eclipse.rdf4j.query.algebra.ProjectionElemList;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.queryrender.BaseTupleExprRenderer;

/**
 * <p>
 * Renders a {@link TupleExpr} into SeRQL Syntax
 * </p>
 * 
 * @author Michael Grove
 */
class SerqlTupleExprRenderer extends BaseTupleExprRenderer {

	private StringBuffer mBuffer = new StringBuffer();

	private StringBuffer mJoinBuffer = new StringBuffer();

	private ValueExpr mFilter;

	private final SerqlValueExprRenderer mValueExprRenderer = new SerqlValueExprRenderer();

	/**
	 * @inheritDoc
	 */
	@Override
	public void reset() {
		super.reset();

		mFilter = null;

		mBuffer = new StringBuffer();
		mJoinBuffer = new StringBuffer();
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public String render(TupleExpr theExpr)
		throws Exception
	{
		theExpr.visit(this);

		if (mBuffer.length() > 0) {
			return mBuffer.toString();
		}

		boolean aFirst = true;

		StringBuffer aQuery = new StringBuffer();

		if (!mProjection.isEmpty()) {
			if (isSelect()) {
				aQuery.append("select ");
			}
			else {
				aQuery.append("construct ");
			}

			if (mDistinct) {
				aQuery.append("distinct ");
			}

			if (mReduced && isSelect()) {
				aQuery.append("reduced ");
			}

			aFirst = true;

			if (!isSelect()) {
				aQuery.append("\n");
			}

			for (ProjectionElemList aList : mProjection) {
				if (isSPOElemList(aList)) {
					if (!aFirst) {
						aQuery.append(",\n");
					}
					else {
						aFirst = false;
					}

					aQuery.append(renderPattern(toStatementPattern(aList)));
				}
				else {
					for (ProjectionElem aElem : aList.getElements()) {
						if (!aFirst) {
							aQuery.append(", ");
						}
						else {
							aFirst = false;
						}

						aQuery.append(mExtensions.containsKey(aElem.getSourceName())
								? mValueExprRenderer.render(mExtensions.get(aElem.getSourceName()))
								: aElem.getSourceName());

						if (!aElem.getSourceName().equals(aElem.getTargetName())
								|| (mExtensions.containsKey(aElem.getTargetName())
										&& !mExtensions.containsKey(aElem.getSourceName())))
						{
							aQuery.append(" as ").append(mExtensions.containsKey(aElem.getTargetName())
									? mValueExprRenderer.render(mExtensions.get(aElem.getTargetName()))
									: aElem.getTargetName());
						}
					}
				}
			}

			aQuery.append("\n");
		}

		if (mJoinBuffer.length() > 0) {
			mJoinBuffer.setCharAt(mJoinBuffer.lastIndexOf(","), ' ');
			aQuery.append("from\n");
			aQuery.append(mJoinBuffer);
		}

		// if (!mQueryAtoms.isEmpty()) {
		// aFirst = true;
		// aQuery.append("from \n");
		// for (StatementPattern thePattern : mQueryAtoms) {
		// if (!aFirst) {
		// aQuery.append(",\n");
		// }
		// else {
		// aFirst = false;
		// }
		//
		// aQuery.append(renderPattern(thePattern));
		// }
		//
		// aQuery.append("\n");
		// }

		if (mFilter != null) {
			aQuery.append("where\n");
			aQuery.append(mValueExprRenderer.render(mFilter));
		}

		if (!mOrdering.isEmpty()) {
			aQuery.append("\norder by ");

			aFirst = true;
			for (OrderElem aOrder : mOrdering) {
				if (!aFirst) {
					aQuery.append(", ");
				}
				else {
					aFirst = false;
				}

				aQuery.append(aOrder.getExpr());
				aQuery.append(" ");
				if (aOrder.isAscending()) {
					aQuery.append("asc");
				}
				else {
					aQuery.append("desc");
				}
			}
		}

		if (mLimit != -1) {
			aQuery.append("\nlimit ").append(mLimit);
		}

		if (mOffset != -1) {
			aQuery.append("\noffset ").append(mOffset);
		}

		return aQuery.toString();
	}

	/**
	 * Renders the tuple expression as a query string. It creates a new SerqlTupleExprRenderer rather than
	 * reusing this one.
	 * 
	 * @param theExpr
	 *        the expr to render
	 * @return the rendered expression
	 * @throws Exception
	 *         if there is an error while rendering
	 */
	private String renderTupleExpr(TupleExpr theExpr)
		throws Exception
	{
		SerqlTupleExprRenderer aRenderer = new SerqlTupleExprRenderer();

		aRenderer.mProjection = new ArrayList<>(mProjection);
		aRenderer.mDistinct = mDistinct;
		aRenderer.mReduced = mReduced;
		aRenderer.mExtensions = new HashMap<>(mExtensions);
		aRenderer.mOrdering = new ArrayList<>(mOrdering);
		aRenderer.mLimit = mLimit;
		aRenderer.mOffset = mOffset;

		return aRenderer.render(theExpr);
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void meet(Union theOp)
		throws Exception
	{
		String aLeft = renderTupleExpr(theOp.getLeftArg());
		String aRight = renderTupleExpr(theOp.getRightArg());

		mBuffer.append(aLeft).append("\nunion\n").append(aRight);
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void meet(Difference theOp)
		throws Exception
	{
		String aLeft = renderTupleExpr(theOp.getLeftArg());
		String aRight = renderTupleExpr(theOp.getRightArg());

		mBuffer.append(aLeft).append("\nminus\n").append(aRight);
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void meet(Intersection theOp)
		throws Exception
	{
		String aLeft = renderTupleExpr(theOp.getLeftArg());
		String aRight = renderTupleExpr(theOp.getRightArg());

		mBuffer.append(aLeft).append("\nintersection\n").append(aRight);
	}

	/**
	 * Render a StatementPattern
	 * 
	 * @param thePattern
	 *        the pattern to render
	 * @return the rendered pattern
	 * @throws Exception
	 *         if there is an error while rendering
	 */
	private String renderPattern(StatementPattern thePattern)
		throws Exception
	{
		return "{" + renderValueExpr(thePattern.getSubjectVar()) + "} "
				+ renderValueExpr(thePattern.getPredicateVar()) + " " + "{"
				+ renderValueExpr(thePattern.getObjectVar()) + "} ";

	}

	/**
	 * @inheritDoc
	 */
	@Override
	protected String renderValueExpr(final ValueExpr theExpr)
		throws Exception
	{
		return mValueExprRenderer.render(theExpr);
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void meet(Join theJoin)
		throws Exception
	{
		theJoin.getLeftArg().visit(this);

		theJoin.getRightArg().visit(this);
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void meet(LeftJoin theJoin)
		throws Exception
	{

		theJoin.getLeftArg().visit(this);

		mJoinBuffer.append(" [");

		theJoin.getRightArg().visit(this);

		if (theJoin.getCondition() != null) {
			mJoinBuffer.append(renderValueExpr(theJoin.getCondition()));
		}

		mJoinBuffer.setCharAt(mJoinBuffer.lastIndexOf(","), ' ');

		mJoinBuffer.append("], ");
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void meet(StatementPattern thePattern)
		throws Exception
	{
		mJoinBuffer.append(renderPattern(thePattern)).append(",\n");
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void meet(final Filter theFilter)
		throws Exception
	{
		if (mFilter == null) {
			mFilter = theFilter.getCondition();
		}
		else {
			mFilter = new And(mFilter, theFilter.getCondition());
		}

		theFilter.visitChildren(this);
	}
}
