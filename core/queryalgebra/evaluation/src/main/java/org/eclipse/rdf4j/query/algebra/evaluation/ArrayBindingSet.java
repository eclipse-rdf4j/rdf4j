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
package org.eclipse.rdf4j.query.algebra.evaluation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.query.AbstractBindingSet;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.impl.SimpleBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An array implementation of the {@link BindingSet} interface.
 *
 * @author Jerven Bolleman
 */
@InternalUseOnly
public class ArrayBindingSet extends AbstractBindingSet implements MutableBindingSet {

	private static final long serialVersionUID = -1L;

	private static final Logger logger = LoggerFactory.getLogger(ArrayBindingSet.class);
	private static final Value NULL_VALUE = Values
			.iri("urn:null:d57c56f3-41a9-468e-8dce-5706ebdef84c_e88d9e52-27cb-4056-a889-1ea353fa6f0c");

	private final String[] bindingNames;

	// Creating a LinkedHashSet is expensive, so we should cache the binding names set
	private Set<String> bindingNamesSetCache;
	private boolean empty;

	private final Value[] values;

	/**
	 * Creates a new Array-based BindingSet for the supplied bindings names. <em>The supplied list of binding names is
	 * assumed to be constant</em>; care should be taken that the contents of this array doesn't change after supplying
	 * it to this solution.
	 *
	 * @param names The binding names.
	 */
	public ArrayBindingSet(String... names) {
		this.bindingNames = names;
		this.values = new Value[names.length];
		this.empty = true;
	}

	public ArrayBindingSet(BindingSet toCopy, Set<String> names, String[] namesArray) {
		assert !(toCopy instanceof ArrayBindingSet);

		this.bindingNames = namesArray;
		this.values = new Value[this.bindingNames.length];
		for (int i = 0; i < this.bindingNames.length; i++) {
			Binding binding = toCopy.getBinding(this.bindingNames[i]);

			if (binding != null) {
				this.values[i] = binding.getValue();
				if (this.values[i] == null) {
					this.values[i] = NULL_VALUE;
				}
			} else if (hasBinding(this.bindingNames[i])) {
				this.values[i] = NULL_VALUE;
			}
		}
		this.empty = toCopy.isEmpty();
		assert !this.empty || size() == 0;

	}

	public ArrayBindingSet(ArrayBindingSet toCopy, String... names) {
		this.bindingNames = names;

		this.values = Arrays.copyOf(toCopy.values, toCopy.values.length);
		this.empty = toCopy.empty;
		assert !this.empty || size() == 0;
	}

	/**
	 * This is used to generate a direct setter into the array to put a binding value into. Can be used to avoid many
	 * comparisons to the bindingNames.
	 *
	 * @param bindingName for which you want the setter
	 * @return the setter biconsumer which can operate on any ArrayBindingSet but should only be used on ones with an
	 *         identical bindingNames array. Otherwise returns null.
	 */
	public BiConsumer<Value, ArrayBindingSet> getDirectSetBinding(String bindingName) {
		int index = getIndex(bindingName);
		if (index == -1) {
			logger.error("Variable not known to ArrayBindingSet : " + bindingName);
			assert false : "Variable not known to ArrayBindingSet : " + bindingName;
			return null;
		}
		return (v, a) -> {
			a.values[index] = v == null ? NULL_VALUE : v;
			a.empty = false;
			a.clearCache();
		};
	}

	public BiConsumer<Value, ArrayBindingSet> getDirectAddBinding(String bindingName) {
		int index = getIndex(bindingName);
		if (index == -1) {
			logger.error("Variable not known to ArrayBindingSet : " + bindingName);
			assert false : "Variable not known to ArrayBindingSet : " + bindingName;
			return null;
		}
		return (v, a) -> {
			assert a.values[index] == null;
			a.values[index] = v == null ? NULL_VALUE : v;
			a.empty = false;
			a.clearCache();
		};

	}

	public Function<ArrayBindingSet, Binding> getDirectGetBinding(String bindingName) {

		int index = getIndex(bindingName);
		if (index == -1) {
			return null;
		}
		return a -> {
			Value value = a.values[index];
			if (value == NULL_VALUE) {
				value = null;
			}
			if (value != null) {
				return new SimpleBinding(bindingName, value);
			} else {
				return null;
			}
		};

	}

	public Function<ArrayBindingSet, Value> getDirectGetValue(String bindingName) {
		int index = getIndex(bindingName);
		if (index == -1) {
			return null;
		}
		return a -> a.values[index] == NULL_VALUE ? null : a.values[index];

	}

	public Function<ArrayBindingSet, Boolean> getDirectHasBinding(String bindingName) {
		int index = getIndex(bindingName);
		if (index == -1) {
			return null;
		}
		return a -> a.values[index] != null;
	}

	private int getIndex(String bindingName) {
		for (int i = 0; i < bindingNames.length; i++) {
			if (bindingNames[i] == bindingName) {
				return i;
			}
		}
		for (int i = 0; i < bindingNames.length; i++) {
			if (bindingNames[i].equals(bindingName)) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public Set<String> getBindingNames() {
		if (isEmpty()) {
			return Collections.emptySet();
		}

		if (bindingNamesSetCache == null) {
			int size = size();
			if (size == 0) {
				this.bindingNamesSetCache = Collections.emptySet();
			} else if (size == 1) {
				for (int i = 0; i < this.bindingNames.length; i++) {
					if (values[i] != null) {
						this.bindingNamesSetCache = Collections.singleton(bindingNames[i]);
						break;
					}
				}
				assert this.bindingNamesSetCache != null;
			} else {
				LinkedHashSet<String> bindingNamesSetCache = new LinkedHashSet<>(size * 2);
				for (int i = 0; i < this.bindingNames.length; i++) {
					if (values[i] != null) {
						bindingNamesSetCache.add(bindingNames[i]);
					}
				}
				this.bindingNamesSetCache = Collections.unmodifiableSet(bindingNamesSetCache);
			}
		}

		return bindingNamesSetCache;
	}

	@Override
	public Value getValue(String bindingName) {
		if (isEmpty()) {
			return null;
		}

		for (int i = 0; i < bindingNames.length; i++) {
			if (bindingNames[i] == bindingName && values[i] != null) {
				return values[i] == NULL_VALUE ? null : values[i];
			}
		}

		for (int i = 0; i < bindingNames.length; i++) {
			if (bindingNames[i].equals(bindingName) && values[i] != null) {
				return values[i] == NULL_VALUE ? null : values[i];
			}
		}
		return null;
	}

	@Override
	public Binding getBinding(String bindingName) {
		if (isEmpty()) {
			return null;
		}

		Value value = getValue(bindingName);
		if (value == NULL_VALUE) {
			value = null;
		}

		if (value != null) {
			return new SimpleBinding(bindingName, value);
		}

		return null;
	}

	@Override
	public boolean hasBinding(String bindingName) {
		if (isEmpty()) {
			return false;
		}

		int index = getIndex(bindingName);
		if (index == -1) {
			return false;
		}
		return values[index] != null;
	}

	@Override
	public Iterator<Binding> iterator() {
		if (isEmpty()) {
			return Collections.emptyIterator();
		}

		return new ArrayBindingSetIterator();
	}

	@Override
	public int size() {
		if (isEmpty()) {
			return 0;
		}

		int size = 0;

		for (Value value : values) {
			if (value != null) {
				size++;
			}
		}

		return size;
	}

	List<String> sortedBindingNames = null;

	public List<String> getSortedBindingNames() {

		if (sortedBindingNames == null) {
			int size = size();

			if (size == 1) {
				for (int i = 0; i < bindingNames.length; i++) {
					if (values[i] != null) {
						sortedBindingNames = Collections.singletonList(bindingNames[i]);
					}
				}
			} else {
				ArrayList<String> names = new ArrayList<>(size);
				for (int i = 0; i < bindingNames.length; i++) {
					if (values[i] != null) {
						names.add(bindingNames[i]);
					}
				}
				names.sort(String::compareTo);
				sortedBindingNames = names;
			}
		}

		return sortedBindingNames;
	}

	@Override
	public void addBinding(Binding binding) {
		int index = getIndex(binding.getName());
		Value value = binding.getValue();
		if (index == -1) {
			logger.error(
					"We don't actually support adding a binding. " + binding.getName() + " : " + value);
			assert false
					: "We don't actually support adding a binding. " + binding.getName() + " : " + value;
			return;
		}

		assert this.values[index] == null;
		this.values[index] = value == null ? NULL_VALUE : value;
		empty = false;
		clearCache();
	}

	@Override
	public void setBinding(Binding binding) {
		int index = getIndex(binding.getName());
		if (index == -1) {
			return;
		}
		Value value = binding.getValue();
		this.values[index] = value == null ? NULL_VALUE : value;
		empty = false;
		clearCache();
	}

	@Override
	public void setBinding(String name, Value value) {
		int index = getIndex(name);
		if (index == -1) {
			return;
		}

		this.values[index] = value;
		if (value == null) {
			this.empty = true;
			for (Value value1 : this.values) {
				if (value1 != null) {
					this.empty = false;
					break;
				}
			}
		} else {
			this.empty = false;
		}
		clearCache();
	}

	@Override
	public boolean isEmpty() {
		return empty;
	}

	private void clearCache() {
		bindingNamesSetCache = null;
	}

	public void addAll(ArrayBindingSet other) {
		if (other.bindingNames == bindingNames) {
			for (int i = 0; i < bindingNames.length; i++) {
				if (other.values[i] != null) {
					this.values[i] = other.values[i];
					this.empty = false;
				}
			}
		} else {
			for (int i = 0; i < bindingNames.length; i++) {
				if (other.hasBinding(bindingNames[i])) {
					Value value = other.getValue(bindingNames[i]);
					this.values[i] = value == null ? NULL_VALUE : value;
					this.empty = false;
				}
			}
		}

		clearCache();

	}

	private class ArrayBindingSetIterator implements Iterator<Binding> {

		private int index = 0;

		public ArrayBindingSetIterator() {
		}

		@Override
		public boolean hasNext() {
			while (index < values.length) {
				if (values[index] != null) {
					return true;
				}
				index++;
			}
			return false;
		}

		@Override
		public Binding next() {
			while (index < values.length) {
				if (values[index] != null) {
					String name = bindingNames[index];
					Value value = values[index++];
					if (value == NULL_VALUE) {
						value = null;
					}
					if (value != null) {
						return new SimpleBinding(name, value);
					} else {
						return null;
					}
				}
				index++;
			}

			throw new NoSuchElementException();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

}
