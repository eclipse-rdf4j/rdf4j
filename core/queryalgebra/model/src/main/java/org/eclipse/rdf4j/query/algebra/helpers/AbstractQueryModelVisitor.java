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
package org.eclipse.rdf4j.query.algebra.helpers;

import org.eclipse.rdf4j.query.algebra.*;

/**
 * Base class for {@link QueryModelVisitor}s. This class implements all <var>meet(... node)</var> methods from the
 * visitor interface, forwarding the call to a method for the node's supertype. This is done recursively until
 * {@link #meetNode} is reached. This allows subclasses to easily define default behaviour for visited nodes of a
 * certain type. The default implementation of {@link #meetNode} is to visit the node's children.
 */
public abstract class AbstractQueryModelVisitor<X extends Exception> implements QueryModelVisitor<X> {

	@Override
	public void meet(Add node) throws X {
		meetUpdateExpr(node);
	}

	@Override
	public void meet(And node) throws X {
		meetBinaryValueOperator(node);
	}

	@Override
	public void meet(ArbitraryLengthPath node) throws X {
		meetNode(node);
	}

	@Override
	public void meet(Avg node) throws X {
		meetUnaryValueOperator(node);
	}

	@Override
	public void meet(BindingSetAssignment node) throws X {
		meetNode(node);
	}

	@Override
	public void meet(BNodeGenerator node) throws X {
		meetNode(node);
	}

	@Override
	public void meet(Bound node) throws X {
		meetNode(node);
	}

	@Override
	public void meet(Clear node) throws X {
		meetUpdateExpr(node);
	}

	@Override
	public void meet(Coalesce node) throws X {
		meetNAryValueOperator(node);
	}

	@Override
	public void meet(Compare node) throws X {
		meetBinaryValueOperator(node);
	}

	@Override
	public void meet(CompareAll node) throws X {
		meetCompareSubQueryValueOperator(node);
	}

	@Override
	public void meet(CompareAny node) throws X {
		meetCompareSubQueryValueOperator(node);
	}

	@Override
	public void meet(DescribeOperator node) throws X {
		meetUnaryTupleOperator(node);
	}

	@Override
	public void meet(Copy node) throws X {
		meetUpdateExpr(node);
	}

	@Override
	public void meet(Count node) throws X {
		meetUnaryValueOperator(node);
	}

	@Override
	public void meet(Create node) throws X {
		meetUpdateExpr(node);
	}

	@Override
	public void meet(Datatype node) throws X {
		meetUnaryValueOperator(node);
	}

	@Override
	public void meet(DeleteData node) throws X {
		meetUpdateExpr(node);
	}

	@Override
	public void meet(Difference node) throws X {
		meetBinaryTupleOperator(node);
	}

	@Override
	public void meet(Distinct node) throws X {
		meetUnaryTupleOperator(node);
	}

	@Override
	public void meet(EmptySet node) throws X {
		meetNode(node);
	}

	@Override
	public void meet(Exists node) throws X {
		meetSubQueryValueOperator(node);
	}

	@Override
	public void meet(Extension node) throws X {
		meetUnaryTupleOperator(node);
	}

	@Override
	public void meet(ExtensionElem node) throws X {
		meetNode(node);
	}

	@Override
	public void meet(Filter node) throws X {
		meetUnaryTupleOperator(node);
	}

	@Override
	public void meet(FunctionCall node) throws X {
		meetNode(node);
	}

	@Override
	public void meet(AggregateFunctionCall node) throws X {
		meetNode(node);
	}

	@Override
	public void meet(Group node) throws X {
		meetUnaryTupleOperator(node);
	}

	@Override
	public void meet(GroupConcat node) throws X {
		meetUnaryValueOperator(node);
	}

	@Override
	public void meet(GroupElem node) throws X {
		meetNode(node);
	}

	@Override
	public void meet(If node) throws X {
		meetNode(node);
	}

	@Override
	public void meet(In node) throws X {
		meetCompareSubQueryValueOperator(node);
	}

	@Override
	public void meet(InsertData node) throws X {
		meetUpdateExpr(node);
	}

	@Override
	public void meet(Intersection node) throws X {
		meetBinaryTupleOperator(node);
	}

	@Override
	public void meet(IRIFunction node) throws X {
		meetUnaryValueOperator(node);
	}

	@Override
	public void meet(IsBNode node) throws X {
		meetUnaryValueOperator(node);
	}

	@Override
	public void meet(IsLiteral node) throws X {
		meetUnaryValueOperator(node);
	}

	@Override
	public void meet(IsNumeric node) throws X {
		meetUnaryValueOperator(node);
	}

	@Override
	public void meet(IsResource node) throws X {
		meetUnaryValueOperator(node);
	}

	@Override
	public void meet(IsURI node) throws X {
		meetUnaryValueOperator(node);
	}

	@Override
	public void meet(Join node) throws X {
		meetBinaryTupleOperator(node);
	}

	@Override
	public void meet(Label node) throws X {
		meetUnaryValueOperator(node);
	}

	@Override
	public void meet(Lateral node) throws X {
		meetBinaryTupleOperator(node);
	}

	@Override
	public void meet(Lang node) throws X {
		meetUnaryValueOperator(node);
	}

	@Override
	public void meet(LangDir node) throws X {
		meetUnaryValueOperator(node);
	}

	@Override
	public void meet(StrLangDir node) throws X {
		meetNAryValueOperator(node);
	}

	@Override
	public void meet(HasLang node) throws X {
		meetUnaryValueOperator(node);
	}

	@Override
	public void meet(HasLangDir node) throws X {
		meetUnaryValueOperator(node);
	}

	@Override
	public void meet(LangMatches node) throws X {
		meetBinaryValueOperator(node);
	}

	@Override
	public void meet(LeftJoin node) throws X {
		meetBinaryTupleOperator(node);
	}

	@Override
	public void meet(Load node) throws X {
		meetUpdateExpr(node);
	}

	@Override
	public void meet(LocalName node) throws X {
		meetUnaryValueOperator(node);
	}

	@Override
	public void meet(MathExpr node) throws X {
		meetBinaryValueOperator(node);
	}

	@Override
	public void meet(Max node) throws X {
		meetUnaryValueOperator(node);
	}

	@Override
	public void meet(Min node) throws X {
		meetUnaryValueOperator(node);
	}

	@Override
	public void meet(Modify node) throws X {
		meetUpdateExpr(node);
	}

	@Override
	public void meet(Move node) throws X {
		meetUpdateExpr(node);
	}

	@Override
	public void meet(MultiProjection node) throws X {
		meetUnaryTupleOperator(node);
	}

	@Override
	public void meet(Namespace node) throws X {
		meetUnaryValueOperator(node);
	}

	@Override
	public void meet(Not node) throws X {
		meetUnaryValueOperator(node);
	}

	@Override
	public void meet(Or node) throws X {
		meetBinaryValueOperator(node);
	}

	@Override
	public void meet(Order node) throws X {
		meetUnaryTupleOperator(node);
	}

	@Override
	public void meet(OrderElem node) throws X {
		meetNode(node);
	}

	@Override
	public void meet(Projection node) throws X {
		meetUnaryTupleOperator(node);
	}

	@Override
	public void meet(ProjectionElem node) throws X {
		meetNode(node);
	}

	@Override
	public void meet(ProjectionElemList node) throws X {
		meetNode(node);
	}

	@Override
	public void meet(QueryRoot node) throws X {
		meetNode(node);
	}

	@Override
	public void meet(Reduced node) throws X {
		meetUnaryTupleOperator(node);
	}

	@Override
	public void meet(Regex node) throws X {
		meetBinaryValueOperator(node);
	}

	@Override
	public void meet(SameTerm node) throws X {
		meetBinaryValueOperator(node);
	}

	@Override
	public void meet(Sample node) throws X {
		meetUnaryValueOperator(node);
	}

	@Override
	public void meet(Service node) throws X {
		meetNode(node);
	}

	@Override
	public void meet(SingletonSet node) throws X {
		meetNode(node);
	}

	@Override
	public void meet(Slice node) throws X {
		meetUnaryTupleOperator(node);
	}

	@Override
	public void meet(StatementPattern node) throws X {
		meetNode(node);
	}

	@Override
	public void meet(Str node) throws X {
		meetUnaryValueOperator(node);
	}

	@Override
	public void meet(Sum node) throws X {
		meetUnaryValueOperator(node);
	}

	@Override
	public void meet(Union node) throws X {
		meetBinaryTupleOperator(node);
	}

	@Override
	public void meet(ValueConstant node) throws X {
		meetNode(node);
	}

	@Override
	public void meet(ListMemberOperator node) throws X {
		meetNAryValueOperator(node);
	}

	@Override
	public void meet(Var node) throws X {
		meetNode(node);
	}

	@Override
	public void meet(ZeroLengthPath node) throws X {
		meetNode(node);
	}

	@Override
	public void meet(TripleRef node) throws X {
		meetNode(node);
	}

	@Override
	public void meet(ValueExprTripleRef node) throws X {
		meetNode(node);
	}

	@Override
	public void meet(IsTriple node) throws X {
		meetUnaryValueOperator(node);
	}

	@Override
	public void meetOther(QueryModelNode node) throws X {
		if (node instanceof UnaryTupleOperator) {
			meetUnaryTupleOperator((UnaryTupleOperator) node);
		} else if (node instanceof BinaryTupleOperator) {
			meetBinaryTupleOperator((BinaryTupleOperator) node);
		} else if (node instanceof CompareSubQueryValueOperator) {
			meetCompareSubQueryValueOperator((CompareSubQueryValueOperator) node);
		} else if (node instanceof SubQueryValueOperator) {
			meetSubQueryValueOperator((SubQueryValueOperator) node);
		} else if (node instanceof UnaryValueOperator) {
			meetUnaryValueOperator((UnaryValueOperator) node);
		} else if (node instanceof BinaryValueOperator) {
			meetBinaryValueOperator((BinaryValueOperator) node);
		} else if (node instanceof UpdateExpr) {
			meetUpdateExpr((UpdateExpr) node);
		} else {
			meetNode(node);
		}
	}

	/**
	 * Method called by all <var>meet</var> methods with a {@link BinaryTupleOperator} node as argument. Forwards the
	 * call to {@link #meetNode} by default.
	 *
	 * @param node The node that is being visited.
	 */
	protected void meetBinaryTupleOperator(BinaryTupleOperator node) throws X {
		meetNode(node);
	}

	/**
	 * Method called by all <var>meet</var> methods with a {@link BinaryValueOperator} node as argument. Forwards the
	 * call to {@link #meetNode} by default.
	 *
	 * @param node The node that is being visited.
	 */
	protected void meetBinaryValueOperator(BinaryValueOperator node) throws X {
		meetNode(node);
	}

	/**
	 * Method called by all <var>meet</var> methods with a {@link CompareSubQueryValueOperator} node as argument.
	 * Forwards the call to {@link #meetSubQueryValueOperator} by default.
	 *
	 * @param node The node that is being visited.
	 */
	protected void meetCompareSubQueryValueOperator(CompareSubQueryValueOperator node) throws X {
		meetSubQueryValueOperator(node);
	}

	/**
	 * Method called by all <var>meet</var> methods with a {@link org.eclipse.rdf4j.query.algebra.NAryValueOperator}
	 * node as argument. Forwards the call to {@link #meetNode} by default.
	 *
	 * @param node The node that is being visited.
	 */
	protected void meetNAryValueOperator(NAryValueOperator node) throws X {
		meetNode(node);
	}

	/**
	 * Method called by all of the other <var>meet</var> methods that are not overridden in subclasses. This method can
	 * be overridden in subclasses to define default behaviour when visiting nodes. The default behaviour of this method
	 * is to visit the node's children.
	 *
	 * @param node The node that is being visited.
	 */
	protected void meetNode(QueryModelNode node) throws X {
		node.visitChildren(this);
	}

	/**
	 * Method called by all <var>meet</var> methods with a {@link SubQueryValueOperator} node as argument. Forwards the
	 * call to {@link #meetNode} by default.
	 *
	 * @param node The node that is being visited.
	 */
	protected void meetSubQueryValueOperator(SubQueryValueOperator node) throws X {
		meetNode(node);
	}

	/**
	 * Method called by all <var>meet</var> methods with a {@link UnaryTupleOperator} node as argument. Forwards the
	 * call to {@link #meetNode} by default.
	 *
	 * @param node The node that is being visited.
	 */
	protected void meetUnaryTupleOperator(UnaryTupleOperator node) throws X {
		meetNode(node);
	}

	/**
	 * Method called by all <var>meet</var> methods with a {@link UnaryValueOperator} node as argument. Forwards the
	 * call to {@link #meetNode} by default.
	 *
	 * @param node The node that is being visited.
	 */
	protected void meetUnaryValueOperator(UnaryValueOperator node) throws X {
		meetNode(node);
	}

	/**
	 * Method called by all <var>meet</var> methods with a {@link UpdateExpr} node as argument. Forwards the call to
	 * {@link #meetNode} by default.
	 *
	 * @param node The node that is being visited.
	 */
	protected void meetUpdateExpr(UpdateExpr node) throws X {
		meetNode(node);
	}
}
