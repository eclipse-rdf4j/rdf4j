/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A tuple expression that matches a statement pattern against an RDF graph. Statement patterns can be targeted at one
 * of three context scopes: all contexts, null context only, or named contexts only.
 */
public class StatementPattern extends AbstractQueryModelNode implements TupleExpr {

	public static final double CARDINALITY_NOT_SET = Double.MIN_VALUE;

	/**
	 * Indicates the scope of the statement pattern.
	 */
	public enum Scope {
		/**
		 * Scope for patterns that should be matched against statements from the default contexts.
		 */
		DEFAULT_CONTEXTS,

		/**
		 * Scope for patterns that should be matched against statements from named contexts only.
		 */
		NAMED_CONTEXTS
	}

	/*-----------*
	 * Variables *
	 *-----------*/

	private Scope scope;

	private Var subjectVar;

	private Var predicateVar;

	private Var objectVar;

	private Var contextVar;

	private Set<String> assuredBindingNames;
	private List<Var> varList;

	private double cardinality = CARDINALITY_NOT_SET;

	/*--------------*
	 * Constructors *
	 *--------------*/

	@Deprecated(since = "4.0.0", forRemoval = true)
	public StatementPattern() {
	}

	/**
	 * Creates a statement pattern that matches a subject-, predicate- and object variable against statements from all
	 * contexts.
	 */
	public StatementPattern(Var subject, Var predicate, Var object) {
		this(Scope.DEFAULT_CONTEXTS, subject, predicate, object);
	}

	/**
	 * Creates a statement pattern that matches a subject-, predicate- and object variable against statements from the
	 * specified context scope.
	 */
	public StatementPattern(Scope scope, Var subject, Var predicate, Var object) {
		this(scope, subject, predicate, object, null);
	}

	/**
	 * Creates a statement pattern that matches a subject-, predicate-, object- and context variable against statements
	 * from all contexts.
	 */
	public StatementPattern(Var subject, Var predicate, Var object, Var context) {
		this(Scope.DEFAULT_CONTEXTS, subject, predicate, object, context);
	}

	/**
	 * Creates a statement pattern that matches a subject-, predicate-, object- and context variable against statements
	 * from the specified context scope.
	 */
	public StatementPattern(Scope scope, Var subjVar, Var predVar, Var objVar, Var conVar) {
		setScope(scope);
		setSubjectVar(subjVar);
		setPredicateVar(predVar);
		setObjectVar(objVar);
		setContextVar(conVar);
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Gets the context scope for the statement pattern.
	 */
	public Scope getScope() {
		return scope;
	}

	/**
	 * Sets the context scope for the statement pattern.
	 */
	@Deprecated(since = "4.0.0", forRemoval = true)
	public void setScope(Scope scope) {
		this.scope = Objects.requireNonNull(scope);
		assuredBindingNames = null;
		varList = null;
		cardinality = CARDINALITY_NOT_SET;
	}

	public Var getSubjectVar() {
		return subjectVar;
	}

	@Deprecated(since = "4.0.0", forRemoval = true)
	public void setSubjectVar(Var subject) {
		Objects.requireNonNull(subject).setParentNode(this);
		subjectVar = subject;
		assuredBindingNames = null;
		varList = null;
		cardinality = CARDINALITY_NOT_SET;
	}

	public Var getPredicateVar() {
		return predicateVar;
	}

	@Deprecated(since = "4.0.0", forRemoval = true)
	public void setPredicateVar(Var predicate) {
		Objects.requireNonNull(predicate).setParentNode(this);
		predicateVar = predicate;
		assuredBindingNames = null;
		varList = null;
		cardinality = CARDINALITY_NOT_SET;
	}

	public Var getObjectVar() {
		return objectVar;
	}

	@Deprecated(since = "4.0.0", forRemoval = true)
	public void setObjectVar(Var object) {
		Objects.requireNonNull(object).setParentNode(this);
		objectVar = object;
		assuredBindingNames = null;
		varList = null;
		cardinality = CARDINALITY_NOT_SET;
	}

	/**
	 * Returns the context variable, if available.
	 */
	public Var getContextVar() {
		return contextVar;
	}

	@Deprecated(since = "4.0.0", forRemoval = true)
	public void setContextVar(Var context) {
		if (context != null) {
			context.setParentNode(this);
		}
		contextVar = context;
		assuredBindingNames = null;
		varList = null;
		cardinality = CARDINALITY_NOT_SET;
	}

	@Override
	public Set<String> getBindingNames() {
		return getAssuredBindingNames();
	}

	@Override
	public Set<String> getAssuredBindingNames() {
		Set<String> assuredBindingNames = this.assuredBindingNames;
		if (assuredBindingNames == null) {
			assuredBindingNames = getBindingsInternal();
			this.assuredBindingNames = assuredBindingNames;
		}
		return assuredBindingNames;
	}

	private Set<String> getBindingsInternal() {
		Set<String> bindingNames = new HashSet<>();

		if (subjectVar != null) {
			bindingNames.add(subjectVar.getName());
		}
		if (predicateVar != null) {
			bindingNames.add(predicateVar.getName());
		}
		if (objectVar != null) {
			bindingNames.add(objectVar.getName());
		}
		if (contextVar != null) {
			bindingNames.add(contextVar.getName());
		}

		return Collections.unmodifiableSet(bindingNames);
	}

	public List<Var> getVarList() {
		List<Var> varList = this.varList;
		if (varList == null) {
			varList = getVarListInternal();
			this.varList = varList;
		}
		return varList;
	}

	private List<Var> getVarListInternal() {
		int size = getSize();
		return Collections.unmodifiableList(getVars(new ArrayList<>(size)));
	}

	private int getSize() {
		int size = 0;
		if (subjectVar != null) {
			size++;
		}
		if (predicateVar != null) {
			size++;
		}
		if (objectVar != null) {
			size++;
		}
		if (contextVar != null) {
			size++;
		}
		return size;
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
		if (contextVar != null) {
			varCollection.add(contextVar);
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
		if (contextVar != null) {
			contextVar.visit(visitor);
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
		} else if (contextVar == current) {
			setContextVar((Var) replacement);
		} else {
			super.replaceChildNode(current, replacement);
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
		if (other instanceof StatementPattern) {
			StatementPattern o = (StatementPattern) other;
			return subjectVar.equals(o.getSubjectVar()) && predicateVar.equals(o.getPredicateVar())
					&& objectVar.equals(o.getObjectVar()) && nullEquals(contextVar, o.getContextVar())
					&& scope.equals(o.getScope());
		}
		return false;
	}

	@Override
	public int hashCode() {
		int result = subjectVar.hashCode();
		result ^= predicateVar.hashCode();
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
	public StatementPattern clone() {
		StatementPattern clone = (StatementPattern) super.clone();

		Var subjectClone = getSubjectVar().clone();
		subjectClone.setParentNode(clone);
		clone.subjectVar = subjectClone;

		Var predicateClone = getPredicateVar().clone();
		predicateClone.setParentNode(clone);
		clone.predicateVar = predicateClone;

		Var objectClone = getObjectVar().clone();
		objectClone.setParentNode(clone);
		clone.objectVar = objectClone;

		if (getContextVar() != null) {
			Var contextClone = getContextVar().clone();
			if (contextClone != null) {
				contextClone.setParentNode(clone);
			}
			clone.contextVar = contextClone;
		}

		clone.setResultSizeEstimate(getResultSizeEstimate());

		clone.assuredBindingNames = assuredBindingNames;
		clone.varList = null;
		clone.cardinality = CARDINALITY_NOT_SET;

		return clone;
	}

	public double getCardinality() {
		return cardinality;
	}

	public void setCardinality(double cardinality) {
		this.cardinality = cardinality;
	}

	public boolean isCardinalitySet() {
		return cardinality != CARDINALITY_NOT_SET;
	}
}
