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
package org.eclipse.rdf4j.query.algebra.evaluation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.common.iterator.ConvertingIterator;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.AbstractBindingSet;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.impl.SimpleBinding;

/**
 * An implementation of the {@link BindingSet} interface that is used to evalate query object models. This
 * implementations differs from {@link MapBindingSet} in that it maps variable names to Value objects and that the
 * Binding objects are created lazily.
 */
public class QueryBindingSet extends AbstractBindingSet implements MutableBindingSet {

	private static final long serialVersionUID = -2010715346095527301L;

	private final Map<String, Value> bindings;

	public QueryBindingSet() {
		this(8);
	}

	public QueryBindingSet(int capacity) {
		// Create bindings map with some extra space for new bindings and
		// compensating for HashMap's load factor
		bindings = new HashMap<>(capacity * 2);
	}

	public QueryBindingSet(BindingSet bindingSet) {
		this(bindingSet.size());
		addAll(bindingSet);
	}

	public void addAll(BindingSet bindingSet) {
		if (bindingSet instanceof QueryBindingSet) {
			bindings.putAll(((QueryBindingSet) bindingSet).bindings);
		} else {
			for (Binding binding : bindingSet) {
				this.addBinding(binding);
			}
		}
	}

	/**
	 * Adds a new binding to the binding set. The binding's name must not already be part of this binding set.
	 *
	 * @param binding The binding to add this this BindingSet.
	 */
	@Override
	public void addBinding(Binding binding) {
		addBinding(binding.getName(), binding.getValue());
	}

	/**
	 * Adds a new binding to the binding set. The binding's name must not already be part of this binding set.
	 *
	 * @param name  The binding's name, must not be bound in this binding set already.
	 * @param value The binding's value.
	 */
	@Override
	public void addBinding(String name, Value value) {
		assert !bindings.containsKey(name) : "variable already bound: " + name;
		setBinding(name, value);
	}

	public void setBinding(Binding binding) {
		setBinding(binding.getName(), binding.getValue());
	}

	public void setBinding(String name, Value value) {
		bindings.put(name, value);
	}

	public void removeBinding(String name) {
		bindings.remove(name);
	}

	public void removeAll(Collection<String> bindingNames) {
		bindings.keySet().removeAll(bindingNames);
	}

	public void retainAll(Collection<String> bindingNames) {
		bindings.keySet().retainAll(bindingNames);
	}

	@Override
	public Set<String> getBindingNames() {
		return bindings.keySet();
	}

	@Override
	public Value getValue(String bindingName) {
		return bindings.get(bindingName);
	}

	@Override
	public Binding getBinding(String bindingName) {
		Value value = getValue(bindingName);

		if (value != null) {
			return new SimpleBinding(bindingName, value);
		}

		return null;
	}

	@Override
	public boolean hasBinding(String bindingName) {
		return bindings.containsKey(bindingName);
	}

	@Override
	public Iterator<Binding> iterator() {
		Iterator<Map.Entry<String, Value>> entries = bindings.entrySet()
				.stream()
				.filter(entry -> entry.getValue() != null)
				.iterator();

		return new ConvertingIterator<Map.Entry<String, Value>, Binding>(entries) {

			@Override
			protected Binding convert(Map.Entry<String, Value> entry) {
				return new SimpleBinding(entry.getKey(), entry.getValue());
			}
		};
	}

	@Override
	public int size() {
		return bindings.size();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof QueryBindingSet) {
			return bindings.equals(((QueryBindingSet) other).bindings);
		} else {
			return super.equals(other);
		}
	}
}
