/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

import java.util.Collection;
import java.util.Set;

public class ReifiedTripleRef extends TripleRef {

	private Var reifVar;

	public ReifiedTripleRef() {
	}

	public ReifiedTripleRef(Var subjectVar, Var predicateVar, Var objectVar, Var exprVar, Var reifVar) {
		super(subjectVar, predicateVar, objectVar, exprVar);
		this.reifVar = reifVar;
	}

	public Var getReifVar() {
		return reifVar;
	}

	public void setReifVar(Var reifVar) {
		assert reifVar != null : "reifier must not be null";
		reifVar.setParentNode(this);
		this.reifVar = reifVar;
	}

	@Override
	public Set<String> getAssuredBindingNames() {
		var bindingNames = super.getAssuredBindingNames();
		if (reifVar != null) {
			bindingNames.add(reifVar.getName());
		}
		return bindingNames;
	}

	@Override
	public <L extends Collection<Var>> L getVars(L varCollection) {
		super.getVars(varCollection);

		if (reifVar != null) {
			varCollection.add(reifVar);
		}
		return varCollection;
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		super.visitChildren(visitor);
		if (reifVar != null) {
			reifVar.visit(visitor);
		}
	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {
		if (reifVar == current) {
			setReifVar((Var) replacement);
		} else {
			super.replaceChildNode(current, replacement);
		}
	}

	@Override
	public String getSignature() {
		StringBuilder sb = new StringBuilder(128);

		sb.append(super.getSignature());

		return sb.toString();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof ReifiedTripleRef) {
			ReifiedTripleRef o = (ReifiedTripleRef) other;
			return getSubjectVar().equals(o.getSubjectVar()) && getPredicateVar().equals(o.getPredicateVar())
					&& getObjectVar().equals(o.getObjectVar()) && getReifVar().equals(o.getReifVar());
		}
		return false;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		if (reifVar != null) {
			result ^= reifVar.hashCode();
		}
		return result;
	}

	@Override
	public TripleRef clone() {
		ReifiedTripleRef clone = (ReifiedTripleRef) super.clone();
		clone.setSubjectVar(getSubjectVar().clone());
		clone.setPredicateVar(getPredicateVar().clone());
		clone.setObjectVar(getObjectVar().clone());

		if (getExprVar() != null) {
			clone.setExprVar(getExprVar().clone());
		}
		clone.setReifVar(getReifVar().clone());

		return clone;
	}
}
