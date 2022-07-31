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
package org.eclipse.rdf4j.query.impl;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.AbstractBindingSet;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;

/**
 * A Map-based implementation of the {@link BindingSet} interface.
 */
public class MapBindingSet extends AbstractBindingSet implements MutableBindingSet {

	private static final long serialVersionUID = -8857324525220429607L;

	private final Map<String, Binding> bindings;

	public MapBindingSet() {
		this(8);
	}

	/**
	 * Creates a new Map-based BindingSet with the specified initial capacity. Bindings can be added to this binding set
	 * using the {@link #addBinding} methods.
	 *
	 * @param capacity The initial capacity of the created BindingSet object.
	 */
	public MapBindingSet(int capacity) {
		// Create bindings map, compensating for HashMap's load factor
		bindings = new LinkedHashMap<>(capacity * 2);
	}

	/**
	 * Adds a binding to the binding set.
	 *
	 * @param binding The binding to add to the binding set.
	 */
	@Override
	public void addBinding(Binding binding) {
		assert !bindings.containsKey(binding.getName()) : "variable already bound: " + binding.getName();
		bindings.put(binding.getName(), binding);
	}

	/**
	 * Removes a binding from the binding set.
	 *
	 * @param name The binding's name.
	 */
	public void removeBinding(String name) {
		bindings.remove(name);
	}

	/**
	 * Removes all bindings from the binding set.
	 */
	public void clear() {
		bindings.clear();
	}

	@Override
	public Iterator<Binding> iterator() {
		return bindings.values().iterator();
	}

	@Override
	public Set<String> getBindingNames() {
		return bindings.keySet();
	}

	@Override
	public Binding getBinding(String bindingName) {
		return bindings.get(bindingName);
	}

	@Override
	public boolean hasBinding(String bindingName) {
		return bindings.containsKey(bindingName);
	}

	@Override
	public Value getValue(String bindingName) {
		Binding binding = getBinding(bindingName);

		if (binding != null) {
			return binding.getValue();
		}

		return null;
	}

	@Override
	public int size() {
		return bindings.size();
	}

	@Override
	public void setBinding(String name, Value value) {
		bindings.put(name, new SimpleBinding(name, value));
	}

	@Override
	public void setBinding(Binding binding) {
		bindings.put(binding.getName(), binding);
	}
}
