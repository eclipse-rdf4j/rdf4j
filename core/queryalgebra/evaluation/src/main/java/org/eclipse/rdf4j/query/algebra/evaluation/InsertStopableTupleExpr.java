/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation;

import java.util.function.BooleanSupplier;

import org.eclipse.rdf4j.query.algebra.Add;
import org.eclipse.rdf4j.query.algebra.ArbitraryLengthPath;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.DescribeOperator;
import org.eclipse.rdf4j.query.algebra.Difference;
import org.eclipse.rdf4j.query.algebra.Distinct;
import org.eclipse.rdf4j.query.algebra.EmptySet;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.Group;
import org.eclipse.rdf4j.query.algebra.Intersection;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.MultiProjection;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.Reduced;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.SingletonSet;
import org.eclipse.rdf4j.query.algebra.Slice;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.StopableTupleExpr;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.ZeroLengthPath;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;

/**
 * Utility class to insert {@link StopableTupleExpr} into a {@link TupleExpr} tree.
 *
 * The idea being that each normal tuple expression does not need to concern itself with stopping and closing due to
 * outside events, but rather that the {@link StopableTupleExpr} will handle that.
 *
 *
 */
public class InsertStopableTupleExpr {

	/**
	 * This method will traverse the given {@link TupleExpr} and wrap all nodes with a {@link StopableTupleExpr} that
	 * has the looks at the {@link BooleanSupplier} to decide to stop further evaluation. QueryRoot nodes will not be
	 * wrapped, but their children will be.
	 *
	 * @param <X>  the type of exception that can be thrown during the visit
	 * @param e    the {@link TupleExpr} and it's children to be wrapped
	 * @param stop the {@link BooleanSupplier} that will be used to determine if the evaluation should stop
	 * @return a {@link StopableTupleExpr} that wrapped the original {@link TupleExpr} and all its children
	 * @throws X
	 */
	public static <X extends Exception> StopableTupleExpr makeStopable(TupleExpr e, BooleanSupplier stop) throws X {
		QueryModelVisitor<X> f = new InsertStopableQueryModelVisitorExtension<X>(stop);
		e.visit(f);
		if (e instanceof StopableTupleExpr) {
			return (StopableTupleExpr) e;
		} else {
			return new StopableTupleExpr(e, stop);
		}
	}

	private static final class InsertStopableQueryModelVisitorExtension<X extends Exception>
			extends AbstractQueryModelVisitor<X> {
		private final BooleanSupplier stop;

		private InsertStopableQueryModelVisitorExtension(BooleanSupplier stop) {
			this.stop = stop;
		}

		@Override
		public void meet(QueryRoot node) throws X {
			super.meet(node);
			// node.replaceWith(new StopableTupleExpr(node));
		}

		@Override
		public void meet(Add add) throws X {
			super.meet(add);
		}

		@Override
		public void meet(ArbitraryLengthPath node) throws X {
			super.meet(node);
			node.replaceWith(new StopableTupleExpr(node, stop));
		}

		@Override
		public void meet(BindingSetAssignment node) throws X {
			super.meet(node);
			node.replaceWith(new StopableTupleExpr(node, stop));
		}

		@Override
		public void meet(DescribeOperator node) throws X {
			super.meet(node);
			node.replaceWith(new StopableTupleExpr(node, stop));
		}

		@Override
		public void meet(Difference node) throws X {
			super.meet(node);
			node.replaceWith(new StopableTupleExpr(node, stop));
		}

		@Override
		public void meet(Distinct node) throws X {
			super.meet(node);
			node.replaceWith(new StopableTupleExpr(node, stop));
		}

		@Override
		public void meet(EmptySet node) throws X {
			super.meet(node);
			node.replaceWith(new StopableTupleExpr(node, stop));
		}

		@Override
		public void meet(Extension node) throws X {
			super.meet(node);
			node.replaceWith(new StopableTupleExpr(node, stop));
		}

		@Override
		public void meet(Filter node) throws X {
			super.meet(node);
			node.replaceWith(new StopableTupleExpr(node, stop));
		}

		@Override
		public void meet(Group node) throws X {
			super.meet(node);
			node.replaceWith(new StopableTupleExpr(node, stop));
		}

		@Override
		public void meet(Intersection node) throws X {
			super.meet(node);
			node.replaceWith(new StopableTupleExpr(node, stop));
		}

		@Override
		public void meet(Join node) throws X {
			super.meet(node);
			node.replaceWith(new StopableTupleExpr(node, stop));
		}

		@Override
		public void meet(LeftJoin node) throws X {
			super.meet(node);
			node.replaceWith(new StopableTupleExpr(node, stop));

		}

		@Override
		public void meet(MultiProjection node) throws X {
			super.meet(node);
			node.replaceWith(new StopableTupleExpr(node, stop));
		}

		@Override
		public void meet(Projection node) throws X {
			super.meet(node);
			node.replaceWith(new StopableTupleExpr(node, stop));
		}

		@Override
		public void meet(Reduced node) throws X {
			super.meet(node);
			node.replaceWith(new StopableTupleExpr(node, stop));
		}

		@Override
		public void meet(Service node) throws X {
			super.meet(node);
			node.replaceWith(new StopableTupleExpr(node, stop));
		}

		@Override
		public void meet(SingletonSet node) throws X {
			super.meet(node);
			node.replaceWith(new StopableTupleExpr(node, stop));
		}

		@Override
		public void meet(Slice node) throws X {
			super.meet(node);
			node.replaceWith(new StopableTupleExpr(node, stop));

		}

		@Override
		public void meet(StatementPattern node) throws X {
			super.meet(node);
			node.replaceWith(new StopableTupleExpr(node, stop));
		}

		@Override
		public void meet(Union node) throws X {
			super.meet(node);
			node.replaceWith(new StopableTupleExpr(node, stop));
		}

		@Override
		public void meet(StopableTupleExpr node) throws X {
			super.meet(node);

		}

		@Override
		public void meet(ZeroLengthPath node) throws X {
			super.meet(node);
			node.replaceWith(new StopableTupleExpr(node, stop));
		}
	}
}
