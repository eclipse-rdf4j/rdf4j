/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.federation.optimizers;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.sail.federation.algebra.OwnedTupleExpr;

/**
 * Remove redundant {@link OwnedTupleExpr}.
 * 
 * @author James Leigh
 */
public class OwnedTupleExprPruner extends AbstractQueryModelVisitor<RuntimeException> implements QueryOptimizer {

	private OwnedTupleExpr owned;

	public void optimize(TupleExpr query, Dataset dataset, BindingSet bindings) {
		owned = null; // NOPMD
		query.visit(this);
	}

	@Override
	public void meetOther(QueryModelNode node) {
		if (node instanceof OwnedTupleExpr) {
			meetOwnedTupleExpr((OwnedTupleExpr)node);
		}
		else {
			super.meetOther(node);
		}
	}

	private void meetOwnedTupleExpr(OwnedTupleExpr node) {
		if (owned == null) {
			owned = node;
			super.meetOther(node);
			owned = null; // NOPMD
		}
		else {
			// no nested OwnedTupleExpr
			TupleExpr replacement = node.getArg().clone();
			node.replaceWith(replacement);
			replacement.visit(this);
		}
	}

}
