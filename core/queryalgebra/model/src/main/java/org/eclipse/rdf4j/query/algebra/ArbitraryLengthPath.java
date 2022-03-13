/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.query.algebra.StatementPattern.Scope;

/**
 * A tuple expression that matches a path of arbitrary length against an RDF graph. They can can be targeted at one of
 * three context scopes: all contexts, null context only, or named contexts only.
 */
public class ArbitraryLengthPath extends AbstractQueryModelNode implements TupleExpr {

	/*-----------*
	 * Variables *
	 *-----------*/

	private Scope scope;

	private Var subjectVar;

	private TupleExpr pathExpression;

	private Var objectVar;

	private Var contextVar;

	private long minLength;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public ArbitraryLengthPath() {
	}

	/**
	 * Creates a arbitrary-length path that matches a subject-, predicate- and object variable against statements from
	 * all contexts.
	 */
	public ArbitraryLengthPath(Var subject, TupleExpr pathExpression, Var object, long minLength) {
		this(Scope.DEFAULT_CONTEXTS, subject, pathExpression, object, minLength);
	}

	/**
	 * Creates a arbitrary-length path that matches a subject-, predicate- and object variable against statements from
	 * the specified context scope.
	 */
	public ArbitraryLengthPath(Scope scope, Var subject, TupleExpr pathExpression, Var object, long minLength) {
		this(scope, subject, pathExpression, object, null, minLength);
	}

	/**
	 * Creates a arbitrary-length path that matches a subject-, predicate-, object- and context variable against
	 * statements from all contexts.
	 */
	public ArbitraryLengthPath(Var subject, TupleExpr pathExpression, Var object, Var context, long minLength) {
		this(Scope.DEFAULT_CONTEXTS, subject, pathExpression, object, context, minLength);
	}

	/**
	 * Creates a arbitrary-length path that matches a subject-, predicate-, object- and context variable against
	 * statements from the specified context scope.
	 */
	public ArbitraryLengthPath(Scope scope, Var subjVar, TupleExpr pathExpression, Var objVar, Var conVar,
			long minLength) {
		setScope(scope);
		setSubjectVar(subjVar);
		setPathExpression(pathExpression);
		setObjectVar(objVar);
		setContextVar(conVar);
		setMinLength(minLength);
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Gets the context scope for the arbitrary-length path.
	 */
	public Scope getScope() {
		return scope;
	}

	/**
	 * Sets the context scope for the arbitrary-length path
	 */
	public void setScope(Scope scope) {
		assert scope != null : "scope must not be null";
		this.scope = scope;
	}

	public Var getSubjectVar() {
		return subjectVar;
	}

	public void setSubjectVar(Var subject) {
		assert subject != null : "subject must not be null";
		subject.setParentNode(this);
		subjectVar = subject;
	}

	public TupleExpr getPathExpression() {
		return pathExpression;
	}

	public void setPathExpression(TupleExpr pathExpression) {
		pathExpression.setParentNode(this);
		this.pathExpression = pathExpression;
	}

	public Var getObjectVar() {
		return objectVar;
	}

	public void setObjectVar(Var object) {
		assert object != null : "object must not be null";
		object.setParentNode(this);
		objectVar = object;
	}

	public void setMinLength(long minLength) {
		this.minLength = minLength;
	}

	public long getMinLength() {
		return minLength;
	}

	/**
	 * Returns the context variable, if available.
	 */
	public Var getContextVar() {
		return contextVar;
	}

	public void setContextVar(Var context) {
		if (context != null) {
			context.setParentNode(this);
		}
		contextVar = context;
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
		if (pathExpression != null) {
			bindingNames.addAll(pathExpression.getAssuredBindingNames());
		}
		if (objectVar != null) {
			bindingNames.add(objectVar.getName());
		}
		if (contextVar != null) {
			bindingNames.add(contextVar.getName());
		}

		return bindingNames;
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
		if (pathExpression != null) {
			pathExpression.visit(visitor);
		}
		if (objectVar != null) {
			objectVar.visit(visitor);
		}
		if (contextVar != null) {
			contextVar.visit(visitor);
		}

	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {
		if (subjectVar == current) {
			setSubjectVar((Var) replacement);
		} else if (pathExpression == current) {
			setPathExpression((TupleExpr) replacement);
		} else if (objectVar == current) {
			setObjectVar((Var) replacement);
		} else if (contextVar == current) {
			setContextVar((Var) replacement);
		}
	}

	@Override
	public String getSignature() {
		StringBuilder sb = new StringBuilder(128);

		sb.append(super.getSignature());

		if (scope == Scope.NAMED_CONTEXTS) {
			sb.append(" FROM NAMED CONTEXT");
		}

		return sb.toString();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof ArbitraryLengthPath) {
			ArbitraryLengthPath o = (ArbitraryLengthPath) other;
			return subjectVar.equals(o.getSubjectVar()) && pathExpression.equals(o.getPathExpression())
					&& objectVar.equals(o.getObjectVar()) && nullEquals(contextVar, o.getContextVar())
					&& scope.equals(o.getScope());
		}
		return false;
	}

	@Override
	public int hashCode() {
		int result = subjectVar.hashCode();
		result ^= pathExpression.hashCode();
		result ^= objectVar.hashCode();
		if (contextVar != null) {
			result ^= contextVar.hashCode();
		}
		if (scope == Scope.NAMED_CONTEXTS) {
			result = ~result;
		}
		return result;
	}

	@Override
	public ArbitraryLengthPath clone() {
		ArbitraryLengthPath clone = (ArbitraryLengthPath) super.clone();
		clone.setSubjectVar(getSubjectVar().clone());
		clone.setPathExpression(getPathExpression().clone());
		clone.setObjectVar(getObjectVar().clone());

		if (getContextVar() != null) {
			clone.setContextVar(getContextVar().clone());
		}

		return clone;
	}
}
