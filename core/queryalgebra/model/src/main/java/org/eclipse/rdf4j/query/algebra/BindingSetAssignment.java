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

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.eclipse.rdf4j.common.order.AvailableStatementOrder;
import org.eclipse.rdf4j.query.BindingSet;

/**
 *
 */
public class BindingSetAssignment extends AbstractQueryModelNode implements TupleExpr {

	private Set<String> bindingNames;

	private Iterable<BindingSet> bindingSets;

	@Override
	public Set<String> getBindingNames() {
		return getAssuredBindingNames();
	}

	@Override
	public Set<String> getAssuredBindingNames() {
		if (bindingNames == null) {
			bindingNames = findBindingNames();
		}
		return bindingNames;
	}

	private Set<String> findBindingNames() {
		Set<String> result = new HashSet<>();
		if (bindingSets != null) {
			for (BindingSet set : bindingSets) {
				result.addAll(set.getBindingNames());
			}
		}
		return result;
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		// no-op
	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {
		throw new IllegalArgumentException("Node is not a child node: " + current);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || !(obj instanceof BindingSetAssignment)) {
			return false;
		}
		BindingSetAssignment other = (BindingSetAssignment) obj;
		return Objects.equals(this.bindingNames, other.bindingNames)
				&& Objects.equals(this.bindingSets, other.bindingSets);
	}

	@Override
	public int hashCode() {
		return Objects.hash(bindingNames, bindingSets);
	}

	@Override
	public BindingSetAssignment clone() {
		return (BindingSetAssignment) super.clone();
	}

	@Override
	public Set<Var> getSupportedOrders(AvailableStatementOrder tripleSource) {
		// Todo, we should consider if we should sort the bindings, and also if there is only one then we don't have to
		// sort anything.
		return Set.of();
	}

	@Override
	public void setOrder(Var var) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public Var getOrder() {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	/**
	 * @param bindingNames The bindingNames to set if known.
	 */
	public void setBindingNames(Set<String> bindingNames) {
		this.bindingNames = bindingNames;
	}

	/**
	 * @param bindingSets The bindingSets to set.
	 */
	public void setBindingSets(Iterable<BindingSet> bindingSets) {
		this.bindingSets = bindingSets;
	}

	/**
	 * @return Returns the bindingSets.
	 */
	public Iterable<BindingSet> getBindingSets() {
		return bindingSets;
	}

	@Override
	public String getSignature() {
		return super.getSignature() + " (" + this.getBindingSets().toString() + ")";
	}

	@Override
	protected boolean shouldCacheCardinality() {
		return true;
	}
}
