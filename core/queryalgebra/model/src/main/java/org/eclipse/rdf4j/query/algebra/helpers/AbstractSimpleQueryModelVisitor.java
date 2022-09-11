/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.helpers;

import org.eclipse.rdf4j.query.algebra.Add;
import org.eclipse.rdf4j.query.algebra.AggregateFunctionCall;
import org.eclipse.rdf4j.query.algebra.And;
import org.eclipse.rdf4j.query.algebra.ArbitraryLengthPath;
import org.eclipse.rdf4j.query.algebra.Avg;
import org.eclipse.rdf4j.query.algebra.BNodeGenerator;
import org.eclipse.rdf4j.query.algebra.BinaryTupleOperator;
import org.eclipse.rdf4j.query.algebra.BinaryValueOperator;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Bound;
import org.eclipse.rdf4j.query.algebra.Clear;
import org.eclipse.rdf4j.query.algebra.Coalesce;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.CompareAll;
import org.eclipse.rdf4j.query.algebra.CompareAny;
import org.eclipse.rdf4j.query.algebra.CompareSubQueryValueOperator;
import org.eclipse.rdf4j.query.algebra.Copy;
import org.eclipse.rdf4j.query.algebra.Count;
import org.eclipse.rdf4j.query.algebra.Create;
import org.eclipse.rdf4j.query.algebra.Datatype;
import org.eclipse.rdf4j.query.algebra.DeleteData;
import org.eclipse.rdf4j.query.algebra.DescribeOperator;
import org.eclipse.rdf4j.query.algebra.Difference;
import org.eclipse.rdf4j.query.algebra.Distinct;
import org.eclipse.rdf4j.query.algebra.EmptySet;
import org.eclipse.rdf4j.query.algebra.Exists;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.ExtensionElem;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.FunctionCall;
import org.eclipse.rdf4j.query.algebra.Group;
import org.eclipse.rdf4j.query.algebra.GroupConcat;
import org.eclipse.rdf4j.query.algebra.GroupElem;
import org.eclipse.rdf4j.query.algebra.IRIFunction;
import org.eclipse.rdf4j.query.algebra.If;
import org.eclipse.rdf4j.query.algebra.In;
import org.eclipse.rdf4j.query.algebra.InsertData;
import org.eclipse.rdf4j.query.algebra.Intersection;
import org.eclipse.rdf4j.query.algebra.IsBNode;
import org.eclipse.rdf4j.query.algebra.IsLiteral;
import org.eclipse.rdf4j.query.algebra.IsNumeric;
import org.eclipse.rdf4j.query.algebra.IsResource;
import org.eclipse.rdf4j.query.algebra.IsURI;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.Label;
import org.eclipse.rdf4j.query.algebra.Lang;
import org.eclipse.rdf4j.query.algebra.LangMatches;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.Like;
import org.eclipse.rdf4j.query.algebra.ListMemberOperator;
import org.eclipse.rdf4j.query.algebra.Load;
import org.eclipse.rdf4j.query.algebra.LocalName;
import org.eclipse.rdf4j.query.algebra.MathExpr;
import org.eclipse.rdf4j.query.algebra.Max;
import org.eclipse.rdf4j.query.algebra.Min;
import org.eclipse.rdf4j.query.algebra.Modify;
import org.eclipse.rdf4j.query.algebra.Move;
import org.eclipse.rdf4j.query.algebra.MultiProjection;
import org.eclipse.rdf4j.query.algebra.NAryValueOperator;
import org.eclipse.rdf4j.query.algebra.Namespace;
import org.eclipse.rdf4j.query.algebra.Not;
import org.eclipse.rdf4j.query.algebra.Or;
import org.eclipse.rdf4j.query.algebra.Order;
import org.eclipse.rdf4j.query.algebra.OrderElem;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.ProjectionElem;
import org.eclipse.rdf4j.query.algebra.ProjectionElemList;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.Reduced;
import org.eclipse.rdf4j.query.algebra.Regex;
import org.eclipse.rdf4j.query.algebra.SameTerm;
import org.eclipse.rdf4j.query.algebra.Sample;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.SingletonSet;
import org.eclipse.rdf4j.query.algebra.Slice;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Str;
import org.eclipse.rdf4j.query.algebra.SubQueryValueOperator;
import org.eclipse.rdf4j.query.algebra.Sum;
import org.eclipse.rdf4j.query.algebra.TripleRef;
import org.eclipse.rdf4j.query.algebra.UnaryTupleOperator;
import org.eclipse.rdf4j.query.algebra.UnaryValueOperator;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.UpdateExpr;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.ValueExprTripleRef;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.ZeroLengthPath;

/**
 * Base class for {@link QueryModelVisitor}s. This class implements all <var>meet(... node)</var> methods from the
 * visitor interface, forwarding the call to a method for the node's supertype. This is done recursively until. This
 * allows subclasses to easily define default behaviour for visited nodes of a certain type.
 */
public abstract class AbstractSimpleQueryModelVisitor<X extends Exception> implements QueryModelVisitor<X> {

	private final boolean meetStatementPatternChildren;
	private final boolean meetProjectionElemListChildren;

	public AbstractSimpleQueryModelVisitor() {
		this(true);
	}

	public AbstractSimpleQueryModelVisitor(boolean meetStatementPatternChildren) {
		this.meetStatementPatternChildren = meetStatementPatternChildren;
		this.meetProjectionElemListChildren = true;
	}

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
		node.visitChildren(this);
	}

	@Override
	public void meet(Avg node) throws X {
		meetUnaryValueOperator(node);
	}

	@Override
	public void meet(BindingSetAssignment node) throws X {

	}

	@Override
	public void meet(BNodeGenerator node) throws X {

	}

	@Override
	public void meet(Bound node) throws X {
		node.visitChildren(this);

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
		node.visitChildren(this);
	}

	@Override
	public void meet(Filter node) throws X {
		meetUnaryTupleOperator(node);
	}

	@Override
	public void meet(FunctionCall node) throws X {
		node.visitChildren(this);
	}

	@Override
	public void meet(AggregateFunctionCall node) throws X {
		node.visitChildren(this);
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
		node.visitChildren(this);
	}

	@Override
	public void meet(If node) throws X {
		node.visitChildren(this);
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
	public void meet(Lang node) throws X {
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
	@Deprecated(forRemoval = true)
	public void meet(Like node) throws X {
		// From SERQL should not be seen
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
		node.visitChildren(this);
	}

	@Override
	public void meet(Projection node) throws X {
		meetUnaryTupleOperator(node);
	}

	@Override
	public void meet(ProjectionElem node) throws X {
	}

	@Override
	public void meet(ProjectionElemList node) throws X {
		if (meetProjectionElemListChildren) {
			node.visitChildren(this);
		}
	}

	@Override
	public void meet(QueryRoot node) throws X {
		node.visitChildren(this);
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
		node.visitChildren(this);

	}

	@Override
	public void meet(SingletonSet node) throws X {
	}

	@Override
	public void meet(Slice node) throws X {
		meetUnaryTupleOperator(node);
	}

	@Override
	public void meet(StatementPattern node) throws X {
		if (meetStatementPatternChildren) {
			node.visitChildren(this);
		}
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
	}

	@Override
	public void meet(ListMemberOperator node) throws X {
		meetNAryValueOperator(node);
	}

	@Override
	public void meet(Var node) throws X {
	}

	@Override
	public void meet(ZeroLengthPath node) throws X {
		node.visitChildren(this);
	}

	@Override
	public void meet(TripleRef node) throws X {
		node.visitChildren(this);
	}

	@Override
	public void meet(ValueExprTripleRef node) throws X {
		node.visitChildren(this);
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
			meetUnsupported(node);
		}
	}

	public void meetUnsupported(QueryModelNode node) throws X {
		node.visitChildren(this);
	}

	/**
	 * Method called by all <var>meet</var> methods with a {@link BinaryTupleOperator} node as argument.
	 *
	 * @param node The node that is being visited.
	 */
	protected void meetBinaryTupleOperator(BinaryTupleOperator node) throws X {
		node.visitChildren(this);
	}

	/**
	 * Method called by all <var>meet</var> methods with a {@link BinaryValueOperator} node as argument.
	 *
	 * @param node The node that is being visited.
	 */
	protected void meetBinaryValueOperator(BinaryValueOperator node) throws X {
		node.visitChildren(this);
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
	 * Method called by all <var>meet</var> methods with a {@link NAryValueOperator} node as argument.
	 *
	 * @param node The node that is being visited.
	 */
	protected void meetNAryValueOperator(NAryValueOperator node) throws X {
		node.visitChildren(this);
	}

	/**
	 * Method called by all <var>meet</var> methods with a {@link SubQueryValueOperator} node as argument.
	 *
	 * @param node The node that is being visited.
	 */
	protected void meetSubQueryValueOperator(SubQueryValueOperator node) throws X {
		node.visitChildren(this);
	}

	/**
	 * Method called by all <var>meet</var> methods with a {@link UnaryTupleOperator} node as argument.
	 *
	 * @param node The node that is being visited.
	 */
	protected void meetUnaryTupleOperator(UnaryTupleOperator node) throws X {
		node.visitChildren(this);
	}

	/**
	 * Method called by all <var>meet</var> methods with a {@link UnaryValueOperator} node as argument.
	 *
	 * @param node The node that is being visited.
	 */
	protected void meetUnaryValueOperator(UnaryValueOperator node) throws X {
		node.visitChildren(this);
	}

	/**
	 * Method called by all <var>meet</var> methods with a {@link UpdateExpr} node as argument.
	 *
	 * @param node The node that is being visited.
	 */
	protected void meetUpdateExpr(UpdateExpr node) throws X {

	}
}
