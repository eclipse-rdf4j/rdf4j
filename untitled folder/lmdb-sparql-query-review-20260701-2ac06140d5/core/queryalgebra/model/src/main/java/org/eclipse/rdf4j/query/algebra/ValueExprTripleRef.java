/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

// reference to a Triple
public class ValueExprTripleRef extends AbstractQueryModelNode implements ValueExpr {

	private final String exprVarName;
	private Var subjectVar;
	private Var predicateVar;
	private Var objectVar;

	public ValueExprTripleRef(String extName, Var s, Var p, Var o) {
		this.exprVarName = extName;
		setSubjectVar(s);
		setPredicateVar(p);
		setObjectVar(o);
	}

	public String getExtVarName() {
		return exprVarName;
	}

	public Var getSubjectVar() {
		return subjectVar;
	}

	public Var getPredicateVar() {
		return predicateVar;
	}

	public Var getObjectVar() {
		return objectVar;
	}

	private void setSubjectVar(Var s) {
		subjectVar = s;
		subjectVar.setParentNode(this);
	}

	private void setPredicateVar(Var p) {
		predicateVar = p;
		predicateVar.setParentNode(this);
	}

	private void setObjectVar(Var o) {
		objectVar = o;
		objectVar.setParentNode(this);
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
	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {
		if (subjectVar == current) {
			setSubjectVar((Var) replacement);
		} else if (predicateVar == current) {
			setPredicateVar((Var) replacement);
		} else if (objectVar == current) {
			setObjectVar((Var) replacement);
		} else {
			throw new IllegalArgumentException("Node is not a child node: " + current);
		}
	}
}
