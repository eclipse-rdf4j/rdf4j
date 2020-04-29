/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

/**
 * An interface for query model visitors, implementing the Visitor pattern. Core query model nodes will call their
 * type-specific method when {@link QueryModelNode#visit(QueryModelVisitor)} is called. The method
 * {@link #meetOther(QueryModelNode)} is provided as a hook for foreign query model nodes.
 */
public interface QueryModelVisitor<X extends Exception> {

	public void meet(QueryRoot node) throws X;

	public void meet(Add add) throws X;

	public void meet(And node) throws X;

	public void meet(ArbitraryLengthPath node) throws X;

	public void meet(Avg node) throws X;

	public void meet(BindingSetAssignment node) throws X;

	public void meet(BNodeGenerator node) throws X;

	public void meet(Bound node) throws X;

	public void meet(Clear clear) throws X;

	public void meet(Coalesce node) throws X;

	public void meet(Compare node) throws X;

	public void meet(CompareAll node) throws X;

	public void meet(CompareAny node) throws X;

	public void meet(DescribeOperator node) throws X;

	public void meet(Copy copy) throws X;

	public void meet(Count node) throws X;

	public void meet(Create create) throws X;

	public void meet(Datatype node) throws X;

	public void meet(DeleteData deleteData) throws X;

	public void meet(Difference node) throws X;

	public void meet(Distinct node) throws X;

	public void meet(EmptySet node) throws X;

	public void meet(Exists node) throws X;

	public void meet(Extension node) throws X;

	public void meet(ExtensionElem node) throws X;

	public void meet(Filter node) throws X;

	public void meet(FunctionCall node) throws X;

	public void meet(Group node) throws X;

	public void meet(GroupConcat node) throws X;

	public void meet(GroupElem node) throws X;

	public void meet(If node) throws X;

	public void meet(In node) throws X;

	public void meet(InsertData insertData) throws X;

	public void meet(Intersection node) throws X;

	public void meet(IRIFunction node) throws X;

	public void meet(IsBNode node) throws X;

	public void meet(IsLiteral node) throws X;

	public void meet(IsNumeric node) throws X;

	public void meet(IsResource node) throws X;

	public void meet(IsURI node) throws X;

	public void meet(Join node) throws X;

	public void meet(Label node) throws X;

	public void meet(Lang node) throws X;

	public void meet(LangMatches node) throws X;

	public void meet(LeftJoin node) throws X;

	public void meet(Like node) throws X;

	public void meet(Load load) throws X;

	public void meet(LocalName node) throws X;

	public void meet(MathExpr node) throws X;

	public void meet(Max node) throws X;

	public void meet(Min node) throws X;

	public void meet(Modify modify) throws X;

	public void meet(Move move) throws X;

	public void meet(MultiProjection node) throws X;

	public void meet(Namespace node) throws X;

	public void meet(Not node) throws X;

	public void meet(Or node) throws X;

	public void meet(Order node) throws X;

	public void meet(OrderElem node) throws X;

	public void meet(Projection node) throws X;

	public void meet(ProjectionElem node) throws X;

	public void meet(ProjectionElemList node) throws X;

	public void meet(Reduced node) throws X;

	public void meet(Regex node) throws X;

	public void meet(SameTerm node) throws X;

	public void meet(Sample node) throws X;

	public void meet(Service node) throws X;

	public void meet(SingletonSet node) throws X;

	public void meet(Slice node) throws X;

	public void meet(StatementPattern node) throws X;

	public void meet(Str node) throws X;

	public void meet(Sum node) throws X;

	public void meet(Union node) throws X;

	public void meet(ValueConstant node) throws X;

	/**
	 */
	public void meet(ListMemberOperator node) throws X;

	public void meet(Var node) throws X;

	public void meet(ZeroLengthPath node) throws X;

	/**
	 * @implNote This temporary default method is only supplied as a stop-gap for backward compatibility. Concrete
	 *           implementations are expected to override.
	 * @since 3.2.0
	 */
	public default void meet(TripleRef node) throws X {
		// no-op
	}

	/**
	 * @implNote This temporary default method is only supplied as a stop-gap for backward compatibility. Concrete
	 *           implementations are expected to override.
	 * @since 3.2.0
	 */
	public default void meet(ValueExprTripleRef node) throws X {
		// no-op
	}

	public void meetOther(QueryModelNode node) throws X;
}
