/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Iterables;

/**
 * A tuple operator that groups tuples that have a specific set of equivalent variable bindings, and that can apply
 * aggregate functions on the grouped results.
 *
 * @author David Huynh
 * @author Arjohn Kampman
 */
public class Group extends UnaryTupleOperator {

	/*-----------*
	 * Variables *
	 *-----------*/

	private Set<String> groupBindings = Set.of();

	private List<GroupElem> groupElements = new ArrayList<>();

	/*--------------*
	 * Constructors *
	 *--------------*/

	public Group() {
	}

	public Group(TupleExpr arg) {
		super(arg);
	}

	public Group(TupleExpr arg, Iterable<String> groupBindingNames) {
		this(arg);
		setGroupBindingNames(groupBindingNames);
	}

	public Group(TupleExpr arg, Iterable<String> groupBindingNames, Iterable<GroupElem> groupElements) {
		this(arg, groupBindingNames);
		setGroupElements(groupElements);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public Set<String> getGroupBindingNames() {
		return groupBindings;
	}

	public void addGroupBindingName(String bindingName) {
		if (groupBindings.isEmpty()) {
			groupBindings = Set.of(bindingName);
			return;
		} else if (groupBindings.size() == 1) {
			groupBindings = new HashSet<>(groupBindings);
		}
		groupBindings.add(bindingName);
	}

	public void setGroupBindingNames(List<String> bindingNames) {
		if (bindingNames.isEmpty()) {
			groupBindings = Set.of();
		} else if (bindingNames.size() == 1) {
			groupBindings = Set.of(bindingNames.get(0));
		} else {
			groupBindings = new LinkedHashSet<>(bindingNames);
		}
	}

	public void setGroupBindingNames(Iterable<String> bindingNames) {
		groupBindings = new LinkedHashSet<>();
		Iterables.addAll(groupBindings, bindingNames);
	}

	public List<GroupElem> getGroupElements() {
		return groupElements;
	}

	public void addGroupElement(GroupElem groupElem) {
		groupElements.add(groupElem);
	}

	public void setGroupElements(Iterable<GroupElem> elements) {
		this.groupElements.clear();
		Iterables.addAll(groupElements, elements);
	}

	public Set<String> getAggregateBindingNames() {
		Set<String> bindings = new HashSet<>();

		for (GroupElem binding : groupElements) {
			bindings.add(binding.getName());
		}

		return bindings;
	}

	@Override
	public Set<String> getBindingNames() {
		Set<String> bindingNames = new LinkedHashSet<>(getGroupBindingNames());
		bindingNames.addAll(getAggregateBindingNames());
		return bindingNames;
	}

	@Override
	public Set<String> getAssuredBindingNames() {
		Set<String> bindingNames = new LinkedHashSet<>(getGroupBindingNames());
		bindingNames.retainAll(getArg().getAssuredBindingNames());
		return bindingNames;
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		super.visitChildren(visitor);

		for (GroupElem ge : groupElements) {
			ge.visit(visitor);
		}
	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {
		if (replaceNodeInList(groupElements, current, replacement)) {
			return;
		}
		super.replaceChildNode(current, replacement);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Group && super.equals(other)) {
			Group o = (Group) other;
			return groupBindings.equals(o.getGroupBindingNames()) && groupElements.equals(o.getGroupElements());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ groupBindings.hashCode() ^ groupElements.hashCode();
	}

	@Override
	public Group clone() {
		Group clone = (Group) super.clone();

		clone.groupBindings = new LinkedHashSet<>(getGroupBindingNames());

		clone.groupElements = new ArrayList<>(getGroupElements().size());
		for (GroupElem ge : getGroupElements()) {
			clone.addGroupElement(ge.clone());
		}

		return clone;
	}

	@Override
	public String getSignature() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.getClass().getSimpleName());
		sb.append(" (");

		Set<String> bindingNames = getGroupBindingNames();
		int count = 0;
		for (String name : bindingNames) {
			sb.append(name);
			count++;
			if (count < bindingNames.size()) {
				sb.append(", ");
			}
		}
		sb.append(")");

		return sb.toString();
	}
}
