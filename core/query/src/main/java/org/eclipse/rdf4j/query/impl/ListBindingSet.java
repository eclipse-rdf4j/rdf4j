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

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.AbstractBindingSet;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;

/**
 * A List-based implementation of the {@link BindingSet} interface.
 *
 * @author Arjohn Kampman
 */
public class ListBindingSet extends AbstractBindingSet {

	private static final long serialVersionUID = -2907809218835403743L;

	private final List<String> bindingNames;
	private Set<String> bindingNamesSetCache;

	private final List<? extends Value> values;

	/**
	 * Creates a new List-based BindingSet containing the supplied bindings. <em>The supplied list of binding names is
	 * assumed to be constant</em>; care should be taken that the contents of this list doesn't change after supplying
	 * it to this solution. The number of supplied values must be equal to the number of the binding names.
	 *
	 * @param names  The binding names.
	 * @param values The binding values.
	 */
	public ListBindingSet(List<String> names, Value... values) {
		this(names, Arrays.asList(values));
	}

	/**
	 * Creates a new List-based BindingSet containing the supplied bindings. <em>The supplied lists are assumed to be
	 * constant</em>; care should be taken that the contents of these lists don't change after supplying them to this
	 * solution. The number of supplied values must be equal to the number of the binding names.
	 *
	 * @param bindingNames The binding names.
	 * @param values       The binding values.
	 */
	public ListBindingSet(List<String> bindingNames, List<? extends Value> values) {
		assert bindingNames.size() == values.size() : "number of binding names and values not equal";

		this.bindingNames = bindingNames;
		this.values = values;
	}

	@Override
	public Set<String> getBindingNames() {
		if (bindingNamesSetCache == null) {
			bindingNamesSetCache = new LinkedHashSet<>(bindingNames);
		}
		return bindingNamesSetCache;
	}

	@Override
	public Value getValue(String bindingName) {
		int idx = bindingNames.indexOf(bindingName);

		if (idx != -1) {
			return values.get(idx);
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
		return getValue(bindingName) != null;
	}

	@Override
	public Iterator<Binding> iterator() {
		return new ListBindingSetIterator();
	}

	@Override
	public int size() {
		int size = 0;

		for (Value value : values) {
			if (value != null) {
				size++;
			}
		}

		return size;
	}

	/*------------------------------------*
	 * Inner class ListBindingSetIterator *
	 *------------------------------------*/

	private class ListBindingSetIterator implements Iterator<Binding> {

		private int index = -1;

		public ListBindingSetIterator() {
			findNextElement();
		}

		private void findNextElement() {
			for (index++; index < values.size(); index++) {
				if (values.get(index) != null) {
					break;
				}
			}
		}

		@Override
		public boolean hasNext() {
			return index < values.size();
		}

		@Override
		public Binding next() {
			Binding result = new SimpleBinding(bindingNames.get(index), values.get(index));
			findNextElement();
			return result;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
