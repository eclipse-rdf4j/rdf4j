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

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.common.order.AvailableStatementOrder;
import org.eclipse.rdf4j.common.order.StatementOrder;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;

/**
 * A tuple expression that matches a statement pattern against an RDF graph. Statement patterns can be targeted at one
 * of three context scopes: all contexts, null context only, or named contexts only.
 */
public class StatementPattern extends AbstractQueryModelNode implements TupleExpr {

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

	private StatementOrder statementOrder;

	private String indexName;

	private Set<String> assuredBindingNames;
	private List<Var> varList;

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
		Objects.requireNonNull(subjVar).setParentNode(this);
		Objects.requireNonNull(predVar).setParentNode(this);
		Objects.requireNonNull(objVar).setParentNode(this);

		this.scope = Objects.requireNonNull(scope);
		subjectVar = subjVar;
		predicateVar = predVar;
		objectVar = objVar;
		if (conVar != null) {
			conVar.setParentNode(this);
		}
		contextVar = conVar;
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

	public Var getSubjectVar() {
		return subjectVar;
	}

	public Var getPredicateVar() {
		return predicateVar;
	}

	public Var getObjectVar() {
		return objectVar;
	}

	/**
	 * Returns the context variable, if available.
	 */
	public Var getContextVar() {
		return contextVar;
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
		return new SmallStringSet(subjectVar, predicateVar, objectVar, contextVar);
	}

	private static class SmallStringSet extends AbstractSet<String> implements Serializable {

		private static final long serialVersionUID = 8771966058555603264L;

		private final String[] values;

		public SmallStringSet(Var var1, Var var2, Var var3, Var var4) {
			String[] values = new String[(var1 != null ? 1 : 0) + (var2 != null ? 1 : 0) + (var3 != null ? 1 : 0)
					+ (var4 != null ? 1 : 0)];

			int i = 0;
			if (var1 != null) {
				values[i++] = var1.getName();
			}
			i = add(var2, values, i);
			i = add(var3, values, i);
			i = add(var4, values, i);

			if (i == values.length) {
				this.values = values;
			} else {
				this.values = Arrays.copyOfRange(values, 0, i);
			}

		}

		private int add(Var var, String[] names, int i) {
			if (var == null) {
				return i;
			}

			String name = var.getName();
			boolean unique = true;
			for (int j = 0; j < i; j++) {
				if (names[j].equals(name)) {
					unique = false;
					break;
				}
			}
			if (unique) {
				names[i++] = name;
			}
			return i;
		}

		@Override
		public Iterator<String> iterator() {
			return new SmallStringSetIterator(values);
		}

		@Override
		public int size() {
			return values.length;
		}
	}

	private static final class SmallStringSetIterator implements Iterator<String> {

		private int index = 0;
		private final String[] values;
		private final int length;

		public SmallStringSetIterator(String[] values) {
			this.values = values;
			this.length = values.length;
		}

		@Override
		public boolean hasNext() {
			return index < length;
		}

		@Override
		public String next() {
			return values[index++];
		}
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

		Var[] vars = new Var[getSize()];
		int i = 0;

		if (subjectVar != null) {
			vars[i++] = subjectVar;
		}
		if (predicateVar != null) {
			vars[i++] = predicateVar;
		}
		if (objectVar != null) {
			vars[i++] = objectVar;
		}
		if (contextVar != null) {
			vars[i++] = contextVar;
		}

		return Arrays.asList(vars);
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
			Objects.requireNonNull((Var) replacement).setParentNode(this);
			subjectVar = (Var) replacement;
		} else if (predicateVar == current) {
			Objects.requireNonNull((Var) replacement).setParentNode(this);
			predicateVar = (Var) replacement;
		} else if (objectVar == current) {
			Objects.requireNonNull((Var) replacement).setParentNode(this);
			objectVar = (Var) replacement;
		} else if (contextVar == current) {
			if (replacement != null) {
				replacement.setParentNode(this);
			}
			contextVar = (Var) replacement;
		} else {
			throw new IllegalArgumentException("Not a child " + current);
		}

		assuredBindingNames = null;
		varList = null;
		resetCardinality();
	}

	@Override
	public String getSignature() {
		StringBuilder sb = new StringBuilder(128);

		sb.append(super.getSignature());

		if (statementOrder != null) {
			sb.append(" [statementOrder: ").append(statementOrder).append("] ");
		}

		if (indexName != null) {
			sb.append(" [index: ").append(indexName).append("] ");
		}

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
					&& objectVar.equals(o.getObjectVar()) && Objects.equals(contextVar, o.getContextVar())
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
		clone.statementOrder = statementOrder;

		return clone;
	}

	@Override
	public Set<Var> getSupportedOrders(AvailableStatementOrder tripleSource) {
		Value subject = subjectVar.hasValue() ? subjectVar.getValue() : null;
		if (subject != null && !(subject instanceof Resource)) {
			return Set.of();
		}

		Value predicate = predicateVar.hasValue() ? predicateVar.getValue() : null;
		if (predicate != null && !(predicate instanceof IRI)) {
			return Set.of();
		}

		Value context = contextVar != null && contextVar.hasValue() ? contextVar.getValue() : null;
		if (context != null && !(context instanceof Resource)) {
			return Set.of();
		}

		Value object = objectVar.hasValue() ? objectVar.getValue() : null;

		Set<StatementOrder> supportedOrders;
		if (contextVar == null) {
			supportedOrders = tripleSource.getSupportedOrders((Resource) subject, (IRI) predicate, object);
		} else {
			supportedOrders = tripleSource.getSupportedOrders((Resource) subject, (IRI) predicate, object,
					(Resource) context);
		}
		return supportedOrders.stream()
				.map(statementOrder -> {
					switch (statementOrder) {
					case S:
						return subjectVar != null && !subjectVar.hasValue() ? subjectVar : null;
					case P:
						return predicateVar != null && !predicateVar.hasValue() ? predicateVar : null;
					case O:
						return objectVar != null && !objectVar.hasValue() ? objectVar : null;
					case C:
						return contextVar != null && !contextVar.hasValue() ? contextVar : null;
					}
					throw new IllegalStateException("Unknown StatementOrder: " + statementOrder);
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
	}

	@Override
	public void setOrder(Var var) {
		if (var == null) {
			statementOrder = null;
			return;
		}

		if (var == subjectVar) {
			statementOrder = StatementOrder.S;
		} else if (var == predicateVar) {
			statementOrder = StatementOrder.P;
		} else if (var == objectVar) {
			statementOrder = StatementOrder.O;
		} else if (var == contextVar) {
			statementOrder = StatementOrder.C;
		} else {
			if (var.equals(subjectVar)) {
				statementOrder = StatementOrder.S;
			} else if (var.equals(predicateVar)) {
				statementOrder = StatementOrder.P;
			} else if (var.equals(objectVar)) {
				statementOrder = StatementOrder.O;
			} else if (var.equals(contextVar)) {
				statementOrder = StatementOrder.C;
			} else {
				throw new IllegalArgumentException("Unknown variable: " + var);
			}
		}
	}

	@Override
	protected boolean shouldCacheCardinality() {
		return true;
	}

	public StatementOrder getStatementOrder() {
		return statementOrder;
	}

	@Override
	public Var getOrder() {
		if (statementOrder == null) {
			return null;
		}

		switch (statementOrder) {

		case S:
			return subjectVar;
		case P:
			return predicateVar;
		case O:
			return objectVar;
		case C:
			return contextVar;
		}

		throw new IllegalStateException("Unknown StatementOrder: " + statementOrder);
	}

	@Experimental
	public String getIndexName() {
		return indexName;
	}

	@Experimental
	public void setIndexName(String indexName) {
		this.indexName = indexName;
	}
}
