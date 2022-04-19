/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sparql.query;

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
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.impl.SimpleBinding;

/**
 * An implementation of the {@link BindingSet} interface that is used to evaluate query object models. This
 * implementations differs from {@link MapBindingSet} in that it maps variable names to Value objects and that the
 * Binding objects are created lazily. Note that this class is a fully equivalent copy of
 * {@link org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet}, and is only included here to avoid a circular
 * dependency between the algebra-evaluation module and the sparql-repository module.
 */
public class SPARQLQueryBindingSet extends AbstractBindingSet {

	private static final long serialVersionUID = -2010715346095527301L;

	private final Map<String, Value> bindings;

	public SPARQLQueryBindingSet() {
		this(8);
	}

	public SPARQLQueryBindingSet(int capacity) {
		// Create bindings map with some extra space for new bindings and
		// compensating for HashMap's load factor
		bindings = new HashMap<>(capacity * 2);
	}

	public SPARQLQueryBindingSet(BindingSet bindingSet) {
		this(bindingSet.size());
		addAll(bindingSet);
	}

	public void addAll(BindingSet bindingSet) {
		if (bindingSet instanceof SPARQLQueryBindingSet) {
			bindings.putAll(((SPARQLQueryBindingSet) bindingSet).bindings);
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
	public void addBinding(Binding binding) {
		addBinding(binding.getName(), binding.getValue());
	}

	/**
	 * Adds a new binding to the binding set. The binding's name must not already be part of this binding set.
	 *
	 * @param name  The binding's name, must not be bound in this binding set already.
	 * @param value The binding's value.
	 */
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
		Iterator<Map.Entry<String, Value>> entries = bindings.entrySet().iterator();

		return new ConvertingIterator<>(entries) {

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
		if (other instanceof SPARQLQueryBindingSet) {
			return bindings.equals(((SPARQLQueryBindingSet) other).bindings);
		} else {
			return super.equals(other);
		}
	}
}
