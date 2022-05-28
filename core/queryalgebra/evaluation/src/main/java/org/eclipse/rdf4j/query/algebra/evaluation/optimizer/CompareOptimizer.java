/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.optimizer;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.Compare.CompareOp;
import org.eclipse.rdf4j.query.algebra.SameTerm;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractSimpleQueryModelVisitor;

/**
 * A query optimizer that replaces {@link Compare} operators with {@link SameTerm}s, if possible.
 *
 * @author Arjohn Kampman
 */
public class CompareOptimizer implements QueryOptimizer {

	/**
	 * Applies generally applicable optimizations to the supplied query: variable assignments are inlined.
	 */
	@Override
	public void optimize(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings) {
		tupleExpr.visit(new CompareVisitor());
	}

	private static class CompareVisitor extends AbstractSimpleQueryModelVisitor<RuntimeException> {

		protected CompareVisitor() {
			super(true);
		}

		@Override
		public void meet(Compare compare) {
			super.meet(compare);

			if (compare.getOperator() == CompareOp.EQ) {
				ValueExpr leftArg = compare.getLeftArg();
				ValueExpr rightArg = compare.getRightArg();

				boolean leftIsVar = isVar(leftArg);
				boolean rightIsVar = isVar(rightArg);
				boolean leftIsResource = isResource(leftArg);
				boolean rightIsResource = isResource(rightArg);

				if (leftIsVar && rightIsResource || leftIsResource && rightIsVar || leftIsResource && rightIsResource) {
					SameTerm sameTerm = new SameTerm(leftArg, rightArg);
					compare.replaceWith(sameTerm);
				}
			}
		}

		protected boolean isVar(ValueExpr valueExpr) {
			if (valueExpr instanceof Var) {
				return true;
			}

			return false;
		}

		protected boolean isResource(ValueExpr valueExpr) {
			if (valueExpr instanceof ValueConstant) {
				Value value = ((ValueConstant) valueExpr).getValue();
				return value instanceof Resource;
			}

			if (valueExpr instanceof Var) {
				Value value = ((Var) valueExpr).getValue();
				return value instanceof Resource;
			}

			return false;
		}
	}
}
