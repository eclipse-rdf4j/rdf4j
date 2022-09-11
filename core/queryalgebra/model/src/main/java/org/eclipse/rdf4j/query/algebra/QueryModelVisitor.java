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
package org.eclipse.rdf4j.query.algebra;

/**
 * An interface for query model visitors, implementing the Visitor pattern. Core query model nodes will call their
 * type-specific method when {@link QueryModelNode#visit(QueryModelVisitor)} is called. The method
 * {@link #meetOther(QueryModelNode)} is provided as a hook for foreign query model nodes.
 */
public interface QueryModelVisitor<X extends Exception> {

	void meet(QueryRoot node) throws X;

	void meet(Add add) throws X;

	void meet(And node) throws X;

	void meet(ArbitraryLengthPath node) throws X;

	void meet(Avg node) throws X;

	void meet(BindingSetAssignment node) throws X;

	void meet(BNodeGenerator node) throws X;

	void meet(Bound node) throws X;

	void meet(Clear clear) throws X;

	void meet(Coalesce node) throws X;

	void meet(Compare node) throws X;

	void meet(CompareAll node) throws X;

	void meet(CompareAny node) throws X;

	void meet(DescribeOperator node) throws X;

	void meet(Copy copy) throws X;

	void meet(Count node) throws X;

	void meet(Create create) throws X;

	void meet(Datatype node) throws X;

	void meet(DeleteData deleteData) throws X;

	void meet(Difference node) throws X;

	void meet(Distinct node) throws X;

	void meet(EmptySet node) throws X;

	void meet(Exists node) throws X;

	void meet(Extension node) throws X;

	void meet(ExtensionElem node) throws X;

	void meet(Filter node) throws X;

	void meet(FunctionCall node) throws X;

	void meet(AggregateFunctionCall node) throws X;

	void meet(Group node) throws X;

	void meet(GroupConcat node) throws X;

	void meet(GroupElem node) throws X;

	void meet(If node) throws X;

	void meet(In node) throws X;

	void meet(InsertData insertData) throws X;

	void meet(Intersection node) throws X;

	void meet(IRIFunction node) throws X;

	void meet(IsBNode node) throws X;

	void meet(IsLiteral node) throws X;

	void meet(IsNumeric node) throws X;

	void meet(IsResource node) throws X;

	void meet(IsURI node) throws X;

	void meet(Join node) throws X;

	void meet(Label node) throws X;

	void meet(Lang node) throws X;

	void meet(LangMatches node) throws X;

	void meet(LeftJoin node) throws X;

	@Deprecated(forRemoval = true)
	void meet(Like node) throws X;

	void meet(Load load) throws X;

	void meet(LocalName node) throws X;

	void meet(MathExpr node) throws X;

	void meet(Max node) throws X;

	void meet(Min node) throws X;

	void meet(Modify modify) throws X;

	void meet(Move move) throws X;

	void meet(MultiProjection node) throws X;

	void meet(Namespace node) throws X;

	void meet(Not node) throws X;

	void meet(Or node) throws X;

	void meet(Order node) throws X;

	void meet(OrderElem node) throws X;

	void meet(Projection node) throws X;

	void meet(ProjectionElem node) throws X;

	void meet(ProjectionElemList node) throws X;

	void meet(Reduced node) throws X;

	void meet(Regex node) throws X;

	void meet(SameTerm node) throws X;

	void meet(Sample node) throws X;

	void meet(Service node) throws X;

	void meet(SingletonSet node) throws X;

	void meet(Slice node) throws X;

	void meet(StatementPattern node) throws X;

	void meet(Str node) throws X;

	void meet(Sum node) throws X;

	void meet(Union node) throws X;

	void meet(ValueConstant node) throws X;

	/**
	 */
	void meet(ListMemberOperator node) throws X;

	void meet(Var node) throws X;

	void meet(ZeroLengthPath node) throws X;

	/**
	 * @implNote This temporary default method is only supplied as a stop-gap for backward compatibility. Concrete
	 *           implementations are expected to override.
	 * @since 3.2.0
	 */
	default void meet(TripleRef node) throws X {
		// no-op
	}

	/**
	 * @implNote This temporary default method is only supplied as a stop-gap for backward compatibility. Concrete
	 *           implementations are expected to override.
	 * @since 3.2.0
	 */
	default void meet(ValueExprTripleRef node) throws X {
		// no-op
	}

	void meetOther(QueryModelNode node) throws X;
}
