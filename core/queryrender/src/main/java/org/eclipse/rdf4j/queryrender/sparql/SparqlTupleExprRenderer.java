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
package org.eclipse.rdf4j.queryrender.sparql;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.rdf4j.query.algebra.AggregateFunctionCall;
import org.eclipse.rdf4j.query.algebra.And;
import org.eclipse.rdf4j.query.algebra.ArbitraryLengthPath;
import org.eclipse.rdf4j.query.algebra.Bound;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.Datatype;
import org.eclipse.rdf4j.query.algebra.Difference;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.ExtensionElem;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.FunctionCall;
import org.eclipse.rdf4j.query.algebra.IRIFunction;
import org.eclipse.rdf4j.query.algebra.If;
import org.eclipse.rdf4j.query.algebra.In;
import org.eclipse.rdf4j.query.algebra.Intersection;
import org.eclipse.rdf4j.query.algebra.IsBNode;
import org.eclipse.rdf4j.query.algebra.IsLiteral;
import org.eclipse.rdf4j.query.algebra.IsNumeric;
import org.eclipse.rdf4j.query.algebra.IsURI;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.Lang;
import org.eclipse.rdf4j.query.algebra.LangMatches;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.Or;
import org.eclipse.rdf4j.query.algebra.Regex;
import org.eclipse.rdf4j.query.algebra.SameTerm;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Str;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.queryrender.BaseTupleExprRenderer;
import org.eclipse.rdf4j.queryrender.RenderUtils;

/**
 * <p>
 * Extends the BaseTupleExprRenderer to provide support for rendering tuple expressions as SPARQL queries.
 * </p>
 *
 * @author Michael Grove
 */
public final class SparqlTupleExprRenderer extends BaseTupleExprRenderer {

	private StringBuffer mJoinBuffer = new StringBuffer();

	private Map<TupleExpr, Var> mContexts = new HashMap<>();

	private int mIndent = 2;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void reset() {
		super.reset();

		mJoinBuffer = new StringBuffer();
		mContexts.clear();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String render(final TupleExpr theExpr) throws Exception {
		mContexts = ContextCollector.collectContexts(theExpr);

		theExpr.visit(this);

		return mJoinBuffer.toString();
	}

	private String indent() {
		final StringBuilder aBuilder = new StringBuilder();
		for (int i = 0; i < mIndent; i++) {
			aBuilder.append(" ");
		}
		return aBuilder.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String renderValueExpr(final ValueExpr theExpr) throws Exception {
		return new SparqlValueExprRenderer().render(theExpr);
	}

	private void ctxOpen(TupleExpr theExpr) {
		Var aContext = mContexts.get(theExpr);

		if (aContext != null) {
			mJoinBuffer.append(indent()).append("GRAPH ");
			if (aContext.hasValue()) {
				mJoinBuffer.append(RenderUtils.toSPARQL(aContext.getValue()));
			} else {
				mJoinBuffer.append("?").append(aContext.getName());
			}
			mJoinBuffer.append(" {").append(System.lineSeparator());
			mIndent += 2;
		}
	}

	private void ctxClose(TupleExpr theExpr) {
		Var aContext = mContexts.get(theExpr);

		if (aContext != null) {
			mJoinBuffer.append("}");
			mIndent -= 2;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(Join theJoin) throws Exception {
		ctxOpen(theJoin);

		theJoin.getLeftArg().visit(this);

		theJoin.getRightArg().visit(this);

		ctxClose(theJoin);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(LeftJoin theJoin) throws Exception {
		ctxOpen(theJoin);

		// try and reverse engineer the original scoping intent of the query
		final boolean aNeedsNewScope = theJoin.getParentNode() != null
				&& (theJoin.getParentNode() instanceof Join || theJoin.getParentNode() instanceof LeftJoin);

		if (aNeedsNewScope) {
			mJoinBuffer.append("{").append(System.lineSeparator());
		}

		theJoin.getLeftArg().visit(this);

		mJoinBuffer.append(indent()).append("OPTIONAL {").append(System.lineSeparator());

		mIndent += 2;
		theJoin.getRightArg().visit(this);

		if (theJoin.getCondition() != null) {
			mJoinBuffer.append(indent())
					.append("filter")
					.append(renderValueExpr(theJoin.getCondition()))
					.append(System.lineSeparator());
		}

		mIndent -= 2;

		mJoinBuffer.append(indent()).append("}.").append(System.lineSeparator());

		if (aNeedsNewScope) {
			mJoinBuffer.append("}.").append(System.lineSeparator());
		}

		ctxClose(theJoin);
	}

	/**
	 * Renders the tuple expression as a query string. It creates a new SparqlTupleExprRenderer rather than reusing this
	 * one.
	 *
	 * @param theExpr the expr to render
	 * @return the rendered expression
	 * @throws Exception if there is an error while rendering
	 */
	private String renderTupleExpr(TupleExpr theExpr) throws Exception {
		SparqlTupleExprRenderer aRenderer = new SparqlTupleExprRenderer();

		// aRenderer.mProjection = new ArrayList<ProjectionElemList>(mProjection);
		// aRenderer.mDistinct = mDistinct;
		// aRenderer.mReduced = mReduced;
		// aRenderer.mExtensions = new HashMap<String, ValueExpr>(mExtensions);
		// aRenderer.mOrdering = new ArrayList<OrderElem>(mOrdering);
		// aRenderer.mLimit = mLimit;
		// aRenderer.mOffset = mOffset;

		aRenderer.mIndent = mIndent;
		aRenderer.mContexts = new HashMap<>(mContexts);

		return aRenderer.render(theExpr);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(Union theOp) throws Exception {
		ctxOpen(theOp);

		String aLeft = renderTupleExpr(theOp.getLeftArg());
		if (aLeft.endsWith(System.lineSeparator())) {
			aLeft = aLeft.substring(0, aLeft.length() - 1);
		}

		String aRight = renderTupleExpr(theOp.getRightArg());
		if (aRight.endsWith(System.lineSeparator())) {
			aRight = aRight.substring(0, aRight.length() - 1);
		}

		mJoinBuffer.append(indent()).append("{").append(System.lineSeparator());
		mJoinBuffer.append(aLeft).append(System.lineSeparator());
		mJoinBuffer.append(indent()).append("}").append(System.lineSeparator());
		mJoinBuffer.append(indent()).append("union").append(System.lineSeparator());
		mJoinBuffer.append(indent()).append("{").append(System.lineSeparator());
		mJoinBuffer.append(aRight).append(System.lineSeparator());
		mJoinBuffer.append(indent()).append("}.").append(System.lineSeparator());

		ctxClose(theOp);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(Difference theOp) throws Exception {
		String aLeft = renderTupleExpr(theOp.getLeftArg());
		String aRight = renderTupleExpr(theOp.getRightArg());

		mJoinBuffer.append(System.lineSeparator());
		mJoinBuffer.append("{").append(aLeft).append("}");
		mJoinBuffer.append(System.lineSeparator()).append("minus").append(System.lineSeparator());
		mJoinBuffer.append("{").append(aRight).append("}.").append(System.lineSeparator());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(Intersection theOp) throws Exception {
		String aLeft = renderTupleExpr(theOp.getLeftArg());
		String aRight = renderTupleExpr(theOp.getRightArg());

		mJoinBuffer.append(System.lineSeparator());
		// is "{" missing?
		mJoinBuffer.append(aLeft).append("}").append(System.lineSeparator());
		mJoinBuffer.append("intersection").append(System.lineSeparator());
		mJoinBuffer.append("{").append(aRight).append("}.").append(System.lineSeparator());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(final Filter theFilter) throws Exception {
		ctxOpen(theFilter);

		if (theFilter.getArg() != null) {
			theFilter.getArg().visit(this);
		}

		// try and reverse engineer the original scoping intent of the query
		final boolean aNeedsNewScope = theFilter.getParentNode() != null
				&& (theFilter.getParentNode() instanceof Join || theFilter.getParentNode() instanceof LeftJoin);

		String aFilter = renderValueExpr(theFilter.getCondition());
		if (theFilter.getCondition() instanceof ValueConstant || theFilter.getCondition() instanceof Var) {
			// means the filter is something like "filter (true)" or "filter (?v)"
			// so we'll need to wrap it in parens since they can't live
			// in the query w/o them, but we can't always wrap them in parens in
			// the normal renderer

			aFilter = "(" + aFilter + ")";
		}

		mJoinBuffer.append(indent());

		// if (aNeedsNewScope) {
		// mJoinBuffer.append("{ ");
		// }

		mJoinBuffer.append("filter ").append(aFilter).append(".");

		// if (aNeedsNewScope) {
		// mJoinBuffer.append("}.");
		// }

		mJoinBuffer.append(System.lineSeparator());

		ctxClose(theFilter);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(StatementPattern thePattern) throws Exception {
		ctxOpen(thePattern);

		mJoinBuffer.append(indent()).append(renderPattern(thePattern));

		ctxClose(thePattern);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(Extension node) throws Exception {
		node.visitChildren(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(ExtensionElem node) throws Exception {
		mJoinBuffer.append(indent()).append("bind(");
		node.visitChildren(this);
		mJoinBuffer.append(" as ?").append(node.getName()).append(").").append(System.lineSeparator());
	}

	@Override
	public void meet(FunctionCall node) throws Exception {
		mJoinBuffer.append(renderValueExpr(node));
	}

	@Override
	public void meet(AggregateFunctionCall node) throws Exception {
		mJoinBuffer.append(renderValueExpr(node));
	}

	@Override
	public void meet(And node) throws Exception {
		mJoinBuffer.append(renderValueExpr(node));
	}

	@Override
	public void meet(Or node) throws Exception {
		mJoinBuffer.append(renderValueExpr(node));
	}

	@Override
	public void meet(Compare node) throws Exception {
		mJoinBuffer.append(renderValueExpr(node));
	}

	@Override
	public void meet(Bound node) throws Exception {
		mJoinBuffer.append(renderValueExpr(node));
	}

	@Override
	public void meet(If theOp) throws Exception {
		mJoinBuffer.append("if(");
		theOp.getCondition().visit(this);
		mJoinBuffer.append(", ");
		theOp.getResult().visit(this);
		mJoinBuffer.append(", ");
		theOp.getAlternative().visit(this);
		mJoinBuffer.append(")");
	}

	@Override
	public void meet(In node) throws Exception {
		mJoinBuffer.append(renderValueExpr(node));
	}

	// @Override
	// public void meet(Coalesce node) throws Exception {
	// mJoinBuffer.append(renderValueExpr(node));
	// }

	@Override
	public void meet(SameTerm node) throws Exception {
		mJoinBuffer.append(renderValueExpr(node));
	}

	@Override
	public void meet(IsURI node) throws Exception {
		mJoinBuffer.append(renderValueExpr(node));
	}

	@Override
	public void meet(IsBNode node) throws Exception {
		mJoinBuffer.append(renderValueExpr(node));
	}

	@Override
	public void meet(IsLiteral node) throws Exception {
		mJoinBuffer.append(renderValueExpr(node));
	}

	@Override
	public void meet(IsNumeric node) throws Exception {
		mJoinBuffer.append(renderValueExpr(node));
	}

	@Override
	public void meet(Datatype node) throws Exception {
		mJoinBuffer.append(renderValueExpr(node));
	}

	@Override
	public void meet(IRIFunction node) throws Exception {
		mJoinBuffer.append(renderValueExpr(node));
	}

	@Override
	public void meet(Str node) throws Exception {
		mJoinBuffer.append(renderValueExpr(node));
	}

	@Override
	public void meet(Regex node) throws Exception {
		mJoinBuffer.append(renderValueExpr(node));
	}

	@Override
	public void meet(Lang node) throws Exception {
		mJoinBuffer.append(renderValueExpr(node));
	}

	@Override
	public void meet(LangMatches node) throws Exception {
		mJoinBuffer.append(renderValueExpr(node));
	}

	@Override
	public void meet(ArbitraryLengthPath node) throws Exception {
		if (!(node.getPathExpression() instanceof StatementPattern)) {
			// unsupported ArbitraryLengthPath
			return;
		}

		StatementPattern statement = (StatementPattern) node.getPathExpression();

		String plusSymbol = "";
		if (node.getMinLength() == 1) {
			plusSymbol = "+";
		}

		mJoinBuffer.append(renderValueExpr(statement.getSubjectVar())).append(" ");
		mJoinBuffer.append(renderValueExpr(statement.getPredicateVar())).append(plusSymbol).append(" ");
		mJoinBuffer.append(renderValueExpr(statement.getObjectVar())).append(".").append(System.lineSeparator());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(ValueConstant node) throws Exception {
		mJoinBuffer.append(renderValueExpr(node));
	}

	/**
	 * @throws Exception {@inheritDoc}
	 */
	@Override
	public void meet(Var node) throws Exception {
		mJoinBuffer.append(renderValueExpr(node));
	}

	String renderPattern(StatementPattern thePattern) throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append(renderValueExpr(thePattern.getSubjectVar())).append(" ");
		sb.append(renderValueExpr(thePattern.getPredicateVar())).append(" ");
		sb.append(renderValueExpr(thePattern.getObjectVar())).append(".").append(System.lineSeparator());
		return sb.toString();
	}
}
