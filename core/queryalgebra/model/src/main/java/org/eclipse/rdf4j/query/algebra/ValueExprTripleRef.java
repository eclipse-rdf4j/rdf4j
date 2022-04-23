/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

// reference to a Triple
public class ValueExprTripleRef extends AbstractQueryModelNode implements ValueExpr {

	private final String exprVarName;
	private final org.eclipse.rdf4j.query.algebra.Var subjectVar;
	private final org.eclipse.rdf4j.query.algebra.Var predicateVar;
	private final org.eclipse.rdf4j.query.algebra.Var objectVar;

	public ValueExprTripleRef(String extName,
			org.eclipse.rdf4j.query.algebra.Var s,
			org.eclipse.rdf4j.query.algebra.Var p,
			org.eclipse.rdf4j.query.algebra.Var o) {
		this.exprVarName = extName;
		subjectVar = s;
		predicateVar = p;
		objectVar = o;
	}

	public String getExtVarName() {
		return exprVarName;
	}

	public org.eclipse.rdf4j.query.algebra.Var getSubjectVar() {
		return subjectVar;
	}

	public org.eclipse.rdf4j.query.algebra.Var getPredicateVar() {
		return predicateVar;
	}

	public org.eclipse.rdf4j.query.algebra.Var getObjectVar() {
		return objectVar;
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		if (subjectVar != null) {
			subjectVar.visit(visitor);
		}
		if (predicateVar != null) {
			predicateVar.visit(visitor);
		}
		if (objectVar != null) {
			objectVar.visit(visitor);
		}
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof ValueExprTripleRef) {
			ValueExprTripleRef o = (ValueExprTripleRef) other;
			return subjectVar.equals(o.getSubjectVar()) && predicateVar.equals(o.getPredicateVar())
					&& objectVar.equals(o.getObjectVar());
		}
		return false;
	}

	@Override
	public int hashCode() {
		int result = subjectVar.hashCode();
		result ^= predicateVar.hashCode();
		result ^= objectVar.hashCode();
		return result;
	}

	@Override
	public ValueExprTripleRef clone() {
		return new ValueExprTripleRef(exprVarName, subjectVar.clone(), predicateVar.clone(), objectVar.clone());
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meetOther(this);
		// visitChildren(visitor);
	}

}
