/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.queryrender.sparql;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.rdf4j.query.algebra.Difference;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.Intersection;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;

/**
 * <p>
 * Visitor implementation for the query algebra which walks the tree and figures out the context for nodes in the
 * algebra. The context for a node is set on the highest node in the tree. That is, everything below it shares the same
 * context.
 * </p>
 *
 * @author Blazej Bulka
 */
public class ContextCollector extends AbstractQueryModelVisitor<Exception> {

	/**
	 * Maps TupleExpr to contexts. This map contains only top-level expression elements that share the given context
	 * (i.e., all elements below share the same context) -- this is because of where contexts are being introduced into
	 * a SPARQL query -- all elements sharing the same contexts are grouped together with a "GRAPH <ctx> { ... }"
	 * clause.
	 */
	private final Map<TupleExpr, Var> mContexts = new HashMap<>();

	private ContextCollector() {
	}

	static Map<TupleExpr, Var> collectContexts(TupleExpr theTupleExpr) throws Exception {
		ContextCollector aContextVisitor = new ContextCollector();

		theTupleExpr.visit(aContextVisitor);

		return aContextVisitor.mContexts;
	}

	@Override
	public void meet(Join theJoin) throws Exception {
		binaryOpMeet(theJoin, theJoin.getLeftArg(), theJoin.getRightArg());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(LeftJoin theJoin) throws Exception {
		binaryOpMeet(theJoin, theJoin.getLeftArg(), theJoin.getRightArg());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(Union theOp) throws Exception {
		binaryOpMeet(theOp, theOp.getLeftArg(), theOp.getRightArg());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(Difference theOp) throws Exception {
		binaryOpMeet(theOp, theOp.getLeftArg(), theOp.getRightArg());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(Intersection theOp) throws Exception {
		binaryOpMeet(theOp, theOp.getLeftArg(), theOp.getRightArg());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(final Filter theFilter) throws Exception {
		theFilter.getArg().visit(this);

		if (mContexts.containsKey(theFilter.getArg())) {
			Var aCtx = mContexts.get(theFilter.getArg());
			mContexts.remove(theFilter.getArg());
			mContexts.put(theFilter, aCtx);
		}
	}

	private void binaryOpMeet(TupleExpr theCurrentExpr, TupleExpr theLeftExpr, TupleExpr theRightExpr)
			throws Exception {
		theLeftExpr.visit(this);

		Var aLeftCtx = mContexts.get(theLeftExpr);

		theRightExpr.visit(this);

		Var aRightCtx = mContexts.get(theRightExpr);

		sameCtxCheck(theCurrentExpr, theLeftExpr, aLeftCtx, theRightExpr, aRightCtx);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(StatementPattern thePattern) throws Exception {
		Var aCtxVar = thePattern.getContextVar();

		if (aCtxVar != null) {
			mContexts.put(thePattern, aCtxVar);
		}
	}

	private void sameCtxCheck(TupleExpr theCurrentExpr, TupleExpr theLeftExpr, Var theLeftCtx, TupleExpr theRightExpr,
			Var theRightCtx) {
		if ((theLeftCtx != null) && (theRightCtx != null) && isSameCtx(theLeftCtx, theRightCtx)) {
			mContexts.remove(theLeftExpr);
			mContexts.remove(theRightExpr);
			mContexts.put(theCurrentExpr, theLeftCtx);
		}
	}

	private boolean isSameCtx(Var v1, Var v2) {
		if ((v1 != null && v1.getValue() != null) && (v2 != null && v2.getValue() != null)) {
			return v1.getValue().equals(v2.getValue());
		} else if ((v1 != null && v1.getName() != null) && (v2 != null && v2.getName() != null)) {
			return v1.getName().equals(v2.getName());
		}

		return false;
	}
}
