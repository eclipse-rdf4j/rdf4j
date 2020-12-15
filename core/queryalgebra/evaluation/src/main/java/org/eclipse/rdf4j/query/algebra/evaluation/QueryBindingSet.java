/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.AbstractBindingSet;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.impl.SimpleBinding;
import org.eclipse.rdf4j.util.iterators.ConvertingIterator;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An implementation of the {@link BindingSet} interface that is used to evalate query object models. This
 * implementations differs from {@link MapBindingSet} in that it maps variable names to Value objects and that the
 * Binding objects are created lazily.
 */
public class QueryBindingSet extends AbstractBindingSet {

	private static final long serialVersionUID = -2010715346095527301L;

	private final Binding[] bindings;

	public QueryBindingSet() {
		this(3);
	}

	public QueryBindingSet(int capacity) {
		// Create bindings map with some extra space for new bindings and
		// compensating for HashMap's load factor
		bindings = new Binding[capacity];
	}

	public QueryBindingSet(BindingSet bindingSet) {
		if (bindingSet instanceof QueryBindingSet) {
			bindings = ((QueryBindingSet) bindingSet).bindings;
		} else {
			bindings = new Binding[bindingSet.size()];
			for (Binding binding : bindingSet) {
				this.addBinding(binding);
			}
		}
	}

	public void addAll(BindingSet bindingSet) {

		for (Binding binding : bindingSet) {
			this.addBinding(binding);
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
		setBinding(name, value);
	}

	public void setBinding(Binding binding) {
		setBinding(binding.getName(), binding.getValue());
	}

	public void setBinding(String name, Value value) {
		for (int i = 0; i < bindings.length; i++) {
			if (bindings[i] == null || bindings[i].getName() == name) {
				bindings[i] = new SimpleBinding(name, value);
				break;
			}
		}


	}

	public void removeBinding(String name) {
		throw new UnsupportedOperationException();
	}

	public void removeAll(Collection<String> bindingNames) {
		throw new UnsupportedOperationException();
	}

	public void retainAll(Collection<String> bindingNames) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<String> getBindingNames() {
		return Arrays.stream(bindings).filter(Objects::nonNull).map(Binding::getName).collect(Collectors.toSet());
	}

	@Override
	public Value getValue(String bindingName) {
		for (Binding binding : bindings) {
			if (binding != null && binding.getName() == bindingName) {
				return binding.getValue();
			}
		}
		return null;
	}

	@Override
	public Binding getBinding(String bindingName) {
		for (Binding binding : bindings) {
			if (binding != null && binding.getName() == bindingName) {
				return binding;
			}
		}
		return null;
	}

	@Override
	public boolean hasBinding(String bindingName) {
		return getValue(bindingName) != null;
	}

	@Override
	public Iterator<Binding> iterator() {
		return Arrays.stream(bindings).filter(Objects::nonNull).iterator();
	}

	@Override
	public int size() {
		return (int) Arrays.stream(bindings).filter(Objects::nonNull).count();
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
