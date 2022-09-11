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

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
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

@InternalUseOnly
public abstract class StatementPatternVisitor implements QueryModelVisitor<Exception> {

	@Override
	public void meet(Add node) throws Exception {

	}

	@Override
	public void meet(And node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(ArbitraryLengthPath node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(Avg node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(BindingSetAssignment node) throws Exception {

	}

	@Override
	public void meet(BNodeGenerator node) throws Exception {

	}

	@Override
	public void meet(Bound node) throws Exception {
		node.visitChildren(this);

	}

	@Override
	public void meet(Clear node) throws Exception {

	}

	@Override
	public void meet(Coalesce node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(Compare node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(CompareAll node) throws Exception {
		((SubQueryValueOperator) node).visitChildren(this);
	}

	@Override
	public void meet(CompareAny node) throws Exception {
		((SubQueryValueOperator) node).visitChildren(this);
	}

	@Override
	public void meet(DescribeOperator node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(Copy node) throws Exception {

	}

	@Override
	public void meet(Count node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(Create node) throws Exception {

	}

	@Override
	public void meet(Datatype node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(DeleteData node) throws Exception {

	}

	@Override
	public void meet(Difference node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(Distinct node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(EmptySet node) throws Exception {
	}

	@Override
	public void meet(Exists node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(Extension node) throws Exception {
		((UnaryTupleOperator) node).visitChildren(this);
	}

	@Override
	public void meet(ExtensionElem node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(Filter node) throws Exception {
		((UnaryTupleOperator) node).visitChildren(this);
	}

	@Override
	public void meet(FunctionCall node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(AggregateFunctionCall node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(Group node) throws Exception {
		((UnaryTupleOperator) node).visitChildren(this);
	}

	@Override
	public void meet(GroupConcat node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(GroupElem node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(If node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(In node) throws Exception {
		((SubQueryValueOperator) node).visitChildren(this);
	}

	@Override
	public void meet(InsertData node) throws Exception {

	}

	@Override
	public void meet(Intersection node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(IRIFunction node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(IsBNode node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(IsLiteral node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(IsNumeric node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(IsResource node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(IsURI node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(Join node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(Label node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(Lang node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(LangMatches node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(LeftJoin node) throws Exception {
		((BinaryTupleOperator) node).visitChildren(this);
	}

	@Override
	@Deprecated(forRemoval = true)
	public void meet(Like node) throws Exception {
		// From SERQL should not be seen
	}

	@Override
	public void meet(Load node) throws Exception {

	}

	@Override
	public void meet(LocalName node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(MathExpr node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(Max node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(Min node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(Modify node) throws Exception {

	}

	@Override
	public void meet(Move node) throws Exception {

	}

	@Override
	public void meet(MultiProjection node) throws Exception {
		((UnaryTupleOperator) node).visitChildren(this);
	}

	@Override
	public void meet(Namespace node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(Not node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(Or node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(Order node) throws Exception {
		((UnaryTupleOperator) node).visitChildren(this);
	}

	@Override
	public void meet(OrderElem node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(Projection node) throws Exception {
		((UnaryTupleOperator) node).visitChildren(this);
	}

	@Override
	public void meet(ProjectionElem node) throws Exception {
	}

	@Override
	public void meet(ProjectionElemList node) throws Exception {
	}

	@Override
	public void meet(QueryRoot node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(Reduced node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(Regex node) throws Exception {
		((BinaryValueOperator) node).visitChildren(this);
	}

	@Override
	public void meet(SameTerm node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(Sample node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(Service node) throws Exception {
		node.visitChildren(this);

	}

	@Override
	public void meet(SingletonSet node) throws Exception {
	}

	@Override
	public void meet(Slice node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(StatementPattern node) throws Exception {
		accept(node);
	}

	protected abstract void accept(StatementPattern node);

	@Override
	public void meet(Str node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(Sum node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(Union node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(ValueConstant node) throws Exception {
	}

	@Override
	public void meet(ListMemberOperator node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(Var node) throws Exception {
	}

	@Override
	public void meet(ZeroLengthPath node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(TripleRef node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(ValueExprTripleRef node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meetOther(QueryModelNode node) throws Exception {
		if (node instanceof UnaryTupleOperator) {
			((UnaryTupleOperator) node).visitChildren(this);
		} else if (node instanceof BinaryTupleOperator) {
			((BinaryTupleOperator) node).visitChildren(this);
		} else if (node instanceof CompareSubQueryValueOperator) {
			((CompareSubQueryValueOperator) node).visitChildren(this);
		} else if (node instanceof SubQueryValueOperator) {
			((SubQueryValueOperator) node).visitChildren(this);
		} else if (node instanceof UnaryValueOperator) {
			((UnaryValueOperator) node).visitChildren(this);
		} else if (node instanceof BinaryValueOperator) {
			((BinaryValueOperator) node).visitChildren(this);
		} else if (node instanceof UpdateExpr) {

		}
	}

}
