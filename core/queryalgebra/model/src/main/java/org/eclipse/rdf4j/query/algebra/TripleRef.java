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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Triple lookup reference. Allow retrieval of RDF-star triples **/
public class TripleRef extends AbstractQueryModelNode implements TupleExpr {

	private Var subjectVar;
	private Var predicateVar;
	private Var objectVar;
	private Var exprVar;

	public TripleRef() {
	}

	public TripleRef(Var subjectVar, Var predicateVar, Var objectVar, Var exprVar) {
		assert subjectVar != null : "subject must not be null";
		assert predicateVar != null : "predicate must not be null";
		assert objectVar != null : "object must not be null";

		subjectVar.setParentNode(this);
		predicateVar.setParentNode(this);
		objectVar.setParentNode(this);
		if (exprVar != null) {
			exprVar.setParentNode(this);
		}

		this.subjectVar = subjectVar;
		this.predicateVar = predicateVar;
		this.objectVar = objectVar;
		this.exprVar = exprVar;
	}

	public Var getSubjectVar() {
		return subjectVar;
	}

	public void setSubjectVar(Var subject) {
		assert subject != null : "subject must not be null";
		subject.setParentNode(this);
		subjectVar = subject;
	}

	public Var getPredicateVar() {
		return predicateVar;
	}

	public void setPredicateVar(Var predicate) {
		assert predicate != null : "predicate must not be null";
		predicate.setParentNode(this);
		predicateVar = predicate;
	}

	public Var getObjectVar() {
		return objectVar;
	}

	public void setObjectVar(Var object) {
		assert object != null : "object must not be null";
		object.setParentNode(this);
		objectVar = object;
	}

	/**
	 * Returns the context variable, if available.
	 */
	public Var getExprVar() {
		return exprVar;
	}

	public void setExprVar(Var context) {
		if (context != null) {
			context.setParentNode(this);
		}
		exprVar = context;
	}

	@Override
	public Set<String> getBindingNames() {
		return getAssuredBindingNames();
	}

	@Override
	public Set<String> getAssuredBindingNames() {
		Set<String> bindingNames = new HashSet<>(8);

		if (subjectVar != null) {
			bindingNames.add(subjectVar.getName());
		}
		if (predicateVar != null) {
			bindingNames.add(predicateVar.getName());
		}
		if (objectVar != null) {
			bindingNames.add(objectVar.getName());
		}
		if (exprVar != null) {
			bindingNames.add(exprVar.getName());
		}

		return bindingNames;
	}

	public List<Var> getVarList() {
		return getVars(new ArrayList<>(4));
	}

	/**
	 * Adds the variables of this statement pattern to the supplied collection.
	 */
	public <L extends Collection<Var>> L getVars(L varCollection) {
		if (subjectVar != null) {
			varCollection.add(subjectVar);
		}
		if (predicateVar != null) {
			varCollection.add(predicateVar);
		}
		if (objectVar != null) {
			varCollection.add(objectVar);
		}
		if (exprVar != null) {
			varCollection.add(exprVar);
		}

		return varCollection;
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
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
		if (exprVar != null) {
			exprVar.visit(visitor);
		}
	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {
		if (subjectVar == current) {
			setSubjectVar((Var) replacement);
		} else if (predicateVar == current) {
			setPredicateVar((Var) replacement);
		} else if (objectVar == current) {
			setObjectVar((Var) replacement);
		} else if (exprVar == current) {
			setExprVar((Var) replacement);
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
		if (other instanceof TripleRef) {
			TripleRef o = (TripleRef) other;
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
		if (exprVar != null) {
			result ^= exprVar.hashCode();
		}
		return result;
	}

	@Override
	public TripleRef clone() {
		TripleRef clone = (TripleRef) super.clone();
		clone.setSubjectVar(getSubjectVar().clone());
		clone.setPredicateVar(getPredicateVar().clone());
		clone.setObjectVar(getObjectVar().clone());

		if (getExprVar() != null) {
			clone.setExprVar(getExprVar().clone());
		}

		return clone;
	}

	@Override
	protected boolean shouldCacheCardinality() {
		return true;
	}

}
