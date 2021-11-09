/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
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

/**
 * An array implementation of the {@link BindingSet} interface.
 *
 * @author Jerven Bolleman
 */
@InternalUseOnly
public class ArrayBindingSet extends AbstractBindingSet implements MutableBindingSet {

	private static final long serialVersionUID = -1L;

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

	public ArrayBindingSet(BindingSet toCopy, String... names) {
		if (toCopy instanceof ArrayBindingSet) {
			ArrayBindingSet abs = (ArrayBindingSet) toCopy;
			this.bindingNames = Arrays.copyOf(abs.bindingNames, abs.bindingNames.length + names.length);
			System.arraycopy(names, 0, bindingNames, abs.bindingNames.length, names.length);
			this.values = Arrays.copyOf(abs.values, abs.bindingNames.length + names.length);
			this.whichBindingsHaveBeenSet = Arrays.copyOf(abs.whichBindingsHaveBeenSet,
					abs.bindingNames.length + names.length);
		} else {
			final int toCopySize = toCopy.size();
			this.bindingNames = new String[toCopySize + names.length];
			this.whichBindingsHaveBeenSet = new boolean[toCopySize + names.length];
			final Iterator<String> iter = toCopy.getBindingNames().iterator();
			for (int i = 0; iter.hasNext(); i++) {
				this.bindingNames[i] = iter.next();
			}
			System.arraycopy(names, 0, bindingNames, toCopySize, names.length);
			this.values = new Value[bindingNames.length];
			for (int i = 0; i < toCopySize; i++) {
				this.values[i] = toCopy.getValue(bindingNames[i]);
				this.whichBindingsHaveBeenSet[i] = toCopy.hasBinding(bindingNames[i]);
			}
		}
	}

	public ArrayBindingSet(ArrayBindingSet toCopy, String... names) {
		this.bindingNames = Arrays.copyOf(toCopy.bindingNames, toCopy.bindingNames.length + names.length);
		System.arraycopy(names, 0, bindingNames, toCopy.bindingNames.length, names.length);
		this.values = Arrays.copyOf(toCopy.values, toCopy.bindingNames.length + names.length);
		this.whichBindingsHaveBeenSet = Arrays.copyOf(toCopy.whichBindingsHaveBeenSet,
				toCopy.bindingNames.length + names.length);
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
		for (int i = 0; i < this.bindingNames.length; i++) {
			if (bindingNames[i].equals(bindingName)) {
				final int idx = i;
				return (v, a) -> {
					a.values[idx] = v;
					a.whichBindingsHaveBeenSet[idx] = true;
				};
			}
		}
		assert false : "variable not known to ArrayBindingSet : " + bindingName;
		return null;
	}

	public BiConsumer<Value, ArrayBindingSet> getDirectAddBinding(String bindingName) {
		for (int i = 0; i < this.bindingNames.length; i++) {
			if (bindingNames[i].equals(bindingName)) {
				final int idx = i;
				return (v, a) -> {
					assert !a.whichBindingsHaveBeenSet[idx] : "variable already bound: " + bindingName;
					a.values[idx] = v;
					a.whichBindingsHaveBeenSet[idx] = true;
				};
			}
		}
		assert false : "variable not known to ArrayBindingSet : " + bindingName;
		return null;
	}

	public Function<ArrayBindingSet, Binding> getDirectGetBinding(String variableName) {
		for (int i = 0; i < this.bindingNames.length; i++) {
			if (bindingNames[i].equals(variableName)) {
				final int idx = i;
				return (a) -> new SimpleBinding(variableName, a.values[idx]);
			}
		}
		return null;
	}

	public Function<ArrayBindingSet, Boolean> getDirectHasBinding(String bindingName) {
		for (int i = 0; i < this.bindingNames.length; i++) {
			if (bindingNames[i].equals(bindingName)) {
				final int idx = i;
				return (a) -> a.values[idx] != null;
			}
		}
		return null;
	}

	@Override
	public Set<String> getBindingNames() {
		final LinkedHashSet<String> bns = new LinkedHashSet<>();
		for (int i = 0; i < this.bindingNames.length; i++) {
			if (values[i] != null)
				bns.add(bindingNames[i]);
		}
		return bns;
	}

	@Override
	public Value getValue(String bindingName) {
		for (int i = 0; i < bindingNames.length; i++) {
			if (bindingNames[i].equals(bindingName) && whichBindingsHaveBeenSet[i])
				return values[i];
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
		for (int i = 0; i < bindingNames.length; i++) {
			if (bindingNames[i].equals(bindingName))
				return whichBindingsHaveBeenSet[i];
		}
		return false;
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

	/*------------------------------------*
	 * Inner class ArrayBindingSetIterator *
	 *------------------------------------*/

	private class ArrayBindingSetIterator implements Iterator<Binding> {

		private int index = 0;

		public ArrayBindingSetIterator() {
		}

		@Override
		public boolean hasNext() {
			for (; index < values.length; index++) {
				if (whichBindingsHaveBeenSet[index]) {
					return true;
				}
			}
			return false;
		}

		@Override
		public Binding next() {
			return new SimpleBinding(bindingNames[index], values[index++]);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public void addBinding(Binding binding) {
		for (int i = 0; i < this.bindingNames.length; i++) {
			if (bindingNames[i].equals(binding.getName())) {
				assert this.whichBindingsHaveBeenSet[i] == false;
				this.values[i] = binding.getValue();
				this.whichBindingsHaveBeenSet[i] = true;
			}
		}
	}

	@Override
	public void setBinding(Binding binding) {
		for (int i = 0; i < this.bindingNames.length; i++) {
			if (bindingNames[i].equals(binding.getName())) {
				this.values[i] = binding.getValue();
				this.whichBindingsHaveBeenSet[i] = true;
			}
		}
	}

	@Override
	public void setBinding(String name, Value value) {
		for (int i = 0; i < this.bindingNames.length; i++) {
			if (bindingNames[i].equals(name)) {
				this.values[i] = value;
				this.whichBindingsHaveBeenSet[i] = value != null;
			}
		}
	}
}
