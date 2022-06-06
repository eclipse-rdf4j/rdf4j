/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.optimizer;

import org.eclipse.rdf4j.federated.algebra.ExclusiveArbitraryLengthPath;
import org.eclipse.rdf4j.federated.algebra.ExclusiveStatement;
import org.eclipse.rdf4j.federated.algebra.ExclusiveTupleExpr;
import org.eclipse.rdf4j.federated.exception.OptimizationException;
import org.eclipse.rdf4j.query.algebra.ArbitraryLengthPath;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractSimpleQueryModelVisitor;

/**
 * A specialized optimizer which identifies and marks {@link ExclusiveTupleExpr}.
 *
 * @author Andreas Schwarte
 *
 */
public class ExclusiveTupleExprOptimizer extends AbstractSimpleQueryModelVisitor<OptimizationException>
		implements FedXOptimizer {

	public ExclusiveTupleExprOptimizer() {
		super(true);
	}

	@Override
	public void optimize(TupleExpr tupleExpr) {
		tupleExpr.visit(this);
	}

	@Override
	public void meet(ArbitraryLengthPath node) throws OptimizationException {

		if (node.getPathExpression() instanceof ExclusiveStatement) {
			ExclusiveStatement st = (ExclusiveStatement) node.getPathExpression();
			ExclusiveArbitraryLengthPath eNode = new ExclusiveArbitraryLengthPath(node, st.getOwner(),
					st.getQueryInfo());
			node.replaceWith(eNode);
			return;
		}
		super.meet(node);
	}

	@Override
	public void meet(Service node) throws OptimizationException {
		// do not optimize anything within SERVICE
	}

}
