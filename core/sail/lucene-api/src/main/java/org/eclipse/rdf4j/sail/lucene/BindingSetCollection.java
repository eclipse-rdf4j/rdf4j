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
package org.eclipse.rdf4j.sail.lucene;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.rdf4j.query.BindingSet;

class BindingSetCollection implements Collection<BindingSet>, Serializable {

	private static final long serialVersionUID = -6021486457076678712L;

	private final Set<String> bindingNames;

	private final LinkedHashSet<BindingSet> bindingSets;

	BindingSetCollection(HashSet<String> bindingNames, LinkedHashSet<BindingSet> bindingSets) {
		this.bindingNames = bindingNames;
		this.bindingSets = bindingSets;
	}

	public Set<String> getBindingNames() {
		return bindingNames;
	}

	@Override
	public boolean add(BindingSet arg0) {
		return bindingSets.add(arg0);
	}

	@Override
	public boolean addAll(Collection<? extends BindingSet> c) {
		return bindingSets.addAll(c);
	}

	@Override
	public void clear() {
		bindingSets.clear();
	}

	@Override
	public boolean contains(Object o) {
		return bindingSets.contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return bindingSets.containsAll(c);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o instanceof BindingSetCollection) {
			BindingSetCollection o1 = (BindingSetCollection) o;
			if (bindingNames.size() != o1.bindingNames.size() || bindingSets.size() != o1.bindingSets.size()) {
				return false;
			}

			if (bindingNames.equals(o1.bindingNames)) {
				if (bindingSets.equals(o1.bindingSets)) {
					return true;
				}
			}
			return false;
		}

		if (!(o instanceof Collection)) {
			return false;
		}

		Collection<?> o1 = (Collection<?>) o;

		if (o1.size() == size()) {
			HashSet<BindingSet> bindingSets1 = new HashSet<>();
			for (Object o2 : o1) {
				if (o2 instanceof BindingSet) {
					bindingSets1.add((BindingSet) o2);
				}
			}

			return bindingSets.equals(bindingSets1);
		}

		return o1.equals(this);
	}

	@Override
	public int hashCode() {
		return bindingSets.hashCode();
	}

	@Override
	public boolean isEmpty() {
		return bindingSets.isEmpty();
	}

	@Override
	public Iterator<BindingSet> iterator() {
		return bindingSets.iterator();
	}

	@Override
	public boolean remove(Object o) {
		return bindingSets.remove(o);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return bindingSets.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return bindingSets.retainAll(c);
	}

	@Override
	public int size() {
		return bindingSets.size();
	}

	@Override
	public Object[] toArray() {
		return bindingSets.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return bindingSets.toArray(a);
	}
}
