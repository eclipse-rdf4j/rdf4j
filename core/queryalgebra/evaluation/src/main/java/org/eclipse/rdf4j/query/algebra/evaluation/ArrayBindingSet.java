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

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.model.Value;
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

	private final String[] bindingNames;

	private final boolean[] whichBindingsHaveBeenSet;

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
		this.whichBindingsHaveBeenSet = new boolean[names.length];
	}

	public ArrayBindingSet(BindingSet toCopy, Set<String> names, String[] namesArray) {
		assert !(toCopy instanceof ArrayBindingSet);

		this.bindingNames = namesArray;
		this.whichBindingsHaveBeenSet = new boolean[this.bindingNames.length];
		this.values = new Value[this.bindingNames.length];
		for (int i = 0; i < this.bindingNames.length; i++) {
			Binding binding = toCopy.getBinding(this.bindingNames[i]);
			if (binding != null) {
				this.values[i] = binding.getValue();
				this.whichBindingsHaveBeenSet[i] = true;
			}
		}

	}

	public ArrayBindingSet(ArrayBindingSet toCopy, String... names) {
		this.bindingNames = names;
		this.values = Arrays.copyOf(toCopy.values, toCopy.values.length);
		this.whichBindingsHaveBeenSet = Arrays.copyOf(toCopy.whichBindingsHaveBeenSet,
				toCopy.whichBindingsHaveBeenSet.length);
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
			a.values[index] = v;
			a.whichBindingsHaveBeenSet[index] = true;
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
			assert !a.whichBindingsHaveBeenSet[index] : "variable already bound: " + bindingName;
			a.values[index] = v;
			a.whichBindingsHaveBeenSet[index] = true;
		};

	}

	public Function<ArrayBindingSet, Binding> getDirectGetBinding(String bindingName) {

		int index = getIndex(bindingName);
		if (index == -1) {
			return null;
		}
		return (a) -> {
			Value value = a.values[index];
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
		return (a) -> a.values[index];

	}

	public Function<ArrayBindingSet, Boolean> getDirectHasBinding(String bindingName) {
		int index = getIndex(bindingName);
		if (index == -1) {
			return null;
		}
		return (a) -> a.whichBindingsHaveBeenSet[index];
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
		final int size = ArrayBindingSet.this.size();
		switch (size) {
		case 0:
			return Collections.emptySet();
		case 1:
			for (int i = 0; i < this.bindingNames.length; i++) {
				if (whichBindingsHaveBeenSet[i]) {
					return Collections.singleton(bindingNames[i]);
				}
			}
			throw new ConcurrentModificationException(
					"An bindingset has been modified during the getBindingNames call");
		default:
			return new MinimallyAllocatingSet(size);
		}
	}

	@Override
	public Value getValue(String bindingName) {
		for (int i = 0; i < bindingNames.length; i++) {
			if (bindingNames[i] == bindingName && whichBindingsHaveBeenSet[i]) {
				return values[i];
			}
		}

		for (int i = 0; i < bindingNames.length; i++) {
			if (bindingNames[i].equals(bindingName) && whichBindingsHaveBeenSet[i]) {
				return values[i];
			}
		}
		return null;
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
		int index = getIndex(bindingName);
		if (index == -1) {
			return false;
		}
		return whichBindingsHaveBeenSet[index];
	}

	@Override
	public Iterator<Binding> iterator() {
		return new ArrayBindingSetIterator();
	}

	@Override
	public int size() {
		int size = 0;

		for (boolean value : whichBindingsHaveBeenSet) {
			if (value) {
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
					if (whichBindingsHaveBeenSet[i]) {
						sortedBindingNames = Collections.singletonList(bindingNames[i]);
					}
				}
			} else {
				ArrayList<String> names = new ArrayList<>(size);
				for (int i = 0; i < bindingNames.length; i++) {
					if (whichBindingsHaveBeenSet[i]) {
						names.add(bindingNames[i]);
					}
				}
				names.sort(String::compareTo);
				sortedBindingNames = names;
			}
		}

		return sortedBindingNames;
	}

	/*------------------------------------*
	 * Inner class ArrayBindingSetIterator *
	 *------------------------------------*/
	private static final class BindingToBindingNameIterator implements Iterator<String> {
		private final Iterator<Binding> nested;

		private BindingToBindingNameIterator(Iterator<Binding> nested) {
			this.nested = nested;
		}

		@Override
		public boolean hasNext() {
			return nested.hasNext();
		}

		@Override
		public String next() {
			return nested.next().getName();
		}
	}

	private final class MinimallyAllocatingSet extends AbstractSet<String> {

		private final int size;

		private MinimallyAllocatingSet(int size) {
			this.size = size;
		}

		@Override
		public int size() {
			return size;
		}

		@Override
		public Iterator<String> iterator() {
			Iterator<Binding> nested = ArrayBindingSet.this.iterator();
			return new BindingToBindingNameIterator(nested);
		}

		@Override
		public boolean add(String e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean addAll(Collection<? extends String> c) {
			throw new UnsupportedOperationException();
		}
	}

	private class ArrayBindingSetIterator implements Iterator<Binding> {

		private int index = 0;

		public ArrayBindingSetIterator() {
		}

		@Override
		public boolean hasNext() {
			for (int at = index; at < values.length; at++) {
				if (whichBindingsHaveBeenSet[at] && values[at] != null) {
					return true;
				}
			}
			return false;
		}

		@Override
		public Binding next() {
			for (; index < values.length; index++) {
				if (whichBindingsHaveBeenSet[index] && values[index] != null) {
					break;
				}
			}

			try {
				String name = bindingNames[index];
				Value value = values[index++];
				if (value != null) {
					return new SimpleBinding(name, value);
				} else {
					return null;
				}
			} catch (ArrayIndexOutOfBoundsException e) {
				throw new NoSuchElementException();
			}
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public void addBinding(Binding binding) {
		int index = getIndex(binding.getName());
		if (index == -1) {
			logger.error(
					"We don't actually support adding a binding. " + binding.getName() + " : " + binding.getValue());
			assert false
					: "We don't actually support adding a binding. " + binding.getName() + " : " + binding.getValue();
			return;
		}

		assert !this.whichBindingsHaveBeenSet[index] : "variable already bound: " + binding.getName();
		this.values[index] = binding.getValue();
		this.whichBindingsHaveBeenSet[index] = true;
	}

	@Override
	public void setBinding(Binding binding) {
		int index = getIndex(binding.getName());
		if (index == -1) {
			return;
		}
		this.values[index] = binding.getValue();
		this.whichBindingsHaveBeenSet[index] = true;
	}

	@Override
	public void setBinding(String name, Value value) {
		int index = getIndex(name);
		if (index == -1) {
			return;
		}

		this.values[index] = value;
		this.whichBindingsHaveBeenSet[index] = value != null;
	}

	@Override
	public boolean isEmpty() {
		for (int index = 0; index < values.length; index++) {
			if (whichBindingsHaveBeenSet[index]) {
				return false;
			}
		}
		return true;
	}
}
