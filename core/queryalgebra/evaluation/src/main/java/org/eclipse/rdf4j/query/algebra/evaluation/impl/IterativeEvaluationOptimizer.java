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
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;

/**
 * @author Arjohn Kampman
 */
@Deprecated(forRemoval = true, since = "4.1.0")
public class IterativeEvaluationOptimizer implements QueryOptimizer {

	public IterativeEvaluationOptimizer() {
	}

	@Override
	public void optimize(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings) {
		tupleExpr.visit(new IEOVisitor());
	}

	protected static class IEOVisitor extends AbstractQueryModelVisitor<RuntimeException> {

		@Override
		public void meet(Union union) {
			super.meet(union);

			TupleExpr leftArg = union.getLeftArg();
			TupleExpr rightArg = union.getRightArg();

			if (leftArg instanceof Join && rightArg instanceof Join) {
				Join leftJoinArg = (Join) leftArg;
				Join rightJoin = (Join) rightArg;

				if (leftJoinArg.getLeftArg().equals(rightJoin.getLeftArg())) {
					// factor out the left-most join argument
					Join newJoin = new Join();
					union.replaceWith(newJoin);
					newJoin.setLeftArg(leftJoinArg.getLeftArg());
					newJoin.setRightArg(union);
					union.setLeftArg(leftJoinArg.getRightArg());
					union.setRightArg(rightJoin.getRightArg());

					union.visit(this);
				}
			}
		}
	}
}
