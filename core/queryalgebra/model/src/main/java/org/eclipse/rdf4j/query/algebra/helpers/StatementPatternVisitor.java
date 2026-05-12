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
import org.eclipse.rdf4j.query.algebra.*;

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
	public void meet(Lateral node) throws Exception {
		((BinaryTupleOperator) node).visitChildren(this);
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
	public void meet(LangDir node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(StrLangDir node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(HasLang node) throws Exception {
		node.visitChildren(this);
	}

	@Override
	public void meet(HasLangDir node) throws Exception {
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
