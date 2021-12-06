/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.AbstractBindingSet;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.ModifiableBindingSet;
import org.eclipse.rdf4j.query.impl.SimpleBinding;

public class DynamicQueryBindingSet implements ModifiableBindingSet {

	private static final long serialVersionUID = -2010715738095234301L;

	private static final int LIST_LIMIT = 12;

	private ModifiableBindingSet bindingSet;

	public DynamicQueryBindingSet() {
		bindingSet = new EmptyBindingSet();
	}

	public DynamicQueryBindingSet(int capacity) {
		if (capacity > LIST_LIMIT) {
			bindingSet = new QueryBindingSet(capacity);
		} else {
			bindingSet = new ListBasedBindingSet(capacity);
		}
	}

	public DynamicQueryBindingSet(BindingSet bindingSet) {
		if (bindingSet instanceof DynamicQueryBindingSet) {
			this.bindingSet = getInstance(((DynamicQueryBindingSet) bindingSet));
		} else if (bindingSet.size() > LIST_LIMIT) {
			this.bindingSet = new QueryBindingSet(bindingSet);
		} else if (bindingSet.size() > 0) {
			this.bindingSet = new ListBasedBindingSet(bindingSet);
		} else {
			this.bindingSet = new EmptyBindingSet();
		}
	}

	private ModifiableBindingSet getInstance(DynamicQueryBindingSet bindingSet) {
		BindingSet innerBindingSet = bindingSet.bindingSet;
		if (innerBindingSet instanceof EmptyBindingSet) {
			return new EmptyBindingSet();
		} else if (innerBindingSet instanceof QueryBindingSet) {
			return new QueryBindingSet(((QueryBindingSet) innerBindingSet));
		} else if (innerBindingSet instanceof ListBasedBindingSet) {
			return new ListBasedBindingSet(((ListBasedBindingSet) innerBindingSet));
		} else {
			throw new IllegalStateException();
		}
	}

	@Override
	public Iterator<Binding> iterator() {
		return bindingSet.iterator();
	}

	@Override
	public Set<String> getBindingNames() {
		return bindingSet.getBindingNames();
	}

	@Override
	public Binding getBinding(String bindingName) {
		return bindingSet.getBinding(bindingName);
	}

	@Override
	public boolean hasBinding(String bindingName) {
		return bindingSet.hasBinding(bindingName);
	}

	@Override
	public Value getValue(String bindingName) {
		return bindingSet.getValue(bindingName);
	}

	@Override
	public int size() {
		return bindingSet.size();
	}

	@Override
	public boolean equals(Object o) {
		return bindingSet.equals(o);
	}

	@Override
	public int hashCode() {
		return bindingSet.hashCode();
	}

	@Override
	public void forEach(Consumer<? super Binding> action) {
		bindingSet.forEach(action);
	}

	@Override
	public Spliterator<Binding> spliterator() {
		return bindingSet.spliterator();
	}

	@Override
	public String toString() {
		return bindingSet.toString();
	}

	@Override
	public void addAll(BindingSet bindingSet) {
		if (this.bindingSet instanceof EmptyBindingSet) {
			if (bindingSet instanceof DynamicQueryBindingSet) {
				this.bindingSet = getInstance((DynamicQueryBindingSet) bindingSet);
			} else {
				if (bindingSet.size() > LIST_LIMIT) {
					this.bindingSet = new QueryBindingSet(bindingSet);
				} else {
					this.bindingSet = new ListBasedBindingSet(bindingSet);
				}
			}
		} else {
			if (this.bindingSet instanceof QueryBindingSet) {
				this.bindingSet.addAll(bindingSet);
			} else {
				if (this.bindingSet.size() + bindingSet.size() > LIST_LIMIT) {
					QueryBindingSet queryBindingSet = new QueryBindingSet(this.bindingSet.size() + bindingSet.size());
					queryBindingSet.addAll(this.bindingSet);
					queryBindingSet.addAll(bindingSet);
					this.bindingSet = queryBindingSet;
				} else {
					this.bindingSet.addAll(bindingSet);
				}
			}

		}
	}

	@Override
	public void addBinding(Binding binding) {
		if (this.bindingSet instanceof EmptyBindingSet) {
			this.bindingSet = new ListBasedBindingSet(4);
		} else if (!(this.bindingSet instanceof QueryBindingSet) && this.bindingSet.size() + 1 > LIST_LIMIT) {
			QueryBindingSet queryBindingSet = new QueryBindingSet(this.bindingSet.size() + 4);
			queryBindingSet.addAll(this.bindingSet);
			this.bindingSet = queryBindingSet;
		}

		bindingSet.addBinding(binding);
	}

	@Override
	public void addBinding(String name, Value value) {
		if (this.bindingSet instanceof EmptyBindingSet) {
			this.bindingSet = new ListBasedBindingSet(4);
		} else if (!(this.bindingSet instanceof QueryBindingSet) && this.bindingSet.size() + 1 > LIST_LIMIT) {
			QueryBindingSet queryBindingSet = new QueryBindingSet(this.bindingSet.size() + 4);
			queryBindingSet.addAll(this.bindingSet);
			this.bindingSet = queryBindingSet;
		}

		bindingSet.addBinding(name, value);
	}

	@Override
	public void setBinding(Binding binding) {
		setBinding(binding.getName(), binding.getValue());
	}

	@Override
	public void setBinding(String name, Value value) {
		if (this.bindingSet instanceof EmptyBindingSet) {
			addBinding(name, value);
		} else {
			bindingSet.setBinding(name, value);
		}
	}

	@Override
	public void removeBinding(String name) {
		if (this.bindingSet instanceof EmptyBindingSet) {
			return;
		}

		bindingSet.removeBinding(name);
	}

	@Override
	public void removeAll(Collection<String> bindingNames) {
		if (this.bindingSet instanceof EmptyBindingSet) {
			return;
		}

		bindingSet.removeAll(bindingNames);
	}

	@Override
	public void retainAll(Collection<String> bindingNames) {
		if (this.bindingSet instanceof EmptyBindingSet) {
			return;
		}

		bindingSet.retainAll(bindingNames);
	}

	static class ListBasedBindingSet extends AbstractBindingSet implements ModifiableBindingSet {

		final List<String> names;
		final List<Value> values;
		Set<String> namesSetCache;

		public ListBasedBindingSet(BindingSet bindingSet) {
			int size = bindingSet.size();
			names = new ArrayList<>(size);
			values = new ArrayList<>(size);

			for (Binding binding : bindingSet) {
				names.add(binding.getName());
				values.add(binding.getValue());
			}
		}

		public ListBasedBindingSet(ListBasedBindingSet bindingSet) {
			names = new ArrayList<>(bindingSet.names);
			values = new ArrayList<>(bindingSet.values);
		}

		public ListBasedBindingSet(int capacity) {
			names = new ArrayList<>(capacity);
			values = new ArrayList<>(capacity);
		}

		@Override
		public Iterator<Binding> iterator() {
			return new Iterator<>() {

				final Iterator<String> nameIterator = names.iterator();
				final Iterator<Value> valuesIterator = values.iterator();

				@Override
				public boolean hasNext() {
					return nameIterator.hasNext();
				}

				@Override
				public Binding next() {
					return new SimpleBinding(nameIterator.next(), valuesIterator.next());
				}
			};
		}

		@Override
		public Set<String> getBindingNames() {
			if (namesSetCache == null) {
				if (names.isEmpty()) {
					namesSetCache = Collections.emptySet();
				}
				if (names.size() == 1) {
					namesSetCache = Collections.singleton(names.get(0));
				} else {
					namesSetCache = Set.copyOf(names);
				}
			}
			return namesSetCache;
		}

		@Override
		public Binding getBinding(String name) {
			int index = getIndex(name);
			if (index >= 0) {
				return new SimpleBinding(name, values.get(index));
			}
			return null;
		}

		@Override
		public boolean hasBinding(String name) {
			return getIndex(name) >= 0;
		}

		@Override
		public Value getValue(String name) {
			int index = getIndex(name);
			if (index >= 0) {
				return values.get(index);
			}
			return null;
		}

		@Override
		public int size() {
			return names.size();
		}

		@Override
		public void addAll(BindingSet bindingSet) {
			namesSetCache = null;
			if (names.isEmpty()) {
				for (Binding binding : bindingSet) {
					names.add(binding.getName());
					values.add(binding.getValue());
				}
			} else {
				for (Binding binding : bindingSet) {
					if (!hasBinding(binding.getName())) {
						addDistinctBinding(binding.getName(), binding.getValue());
					}
				}
			}
		}

		@Override
		public void addBinding(Binding binding) {
			namesSetCache = null;
			addBinding(binding.getName(), binding.getValue());
		}

		@Override
		public void addBinding(String name, Value value) {
			namesSetCache = null;
			if (names.isEmpty()) {
				names.add(name);
				values.add(value);
			} else {
				if (!hasBinding(name)) {
					addDistinctBinding(name, value);
				}
			}
		}

		@Override
		public void setBinding(Binding binding) {
			namesSetCache = null;
			setBinding(binding.getName(), binding.getValue());
		}

		@Override
		public void setBinding(String name, Value value) {
			namesSetCache = null;
			int index = getIndex(name);
			if (index >= 0) {
				values.set(index, value);
			} else {
				addDistinctBinding(name, value);
			}
		}

		private void addDistinctBinding(String name, Value value) {
			names.add(name);
			values.add(value);
		}

		@Override
		public void removeBinding(String name) {
			namesSetCache = null;
			int index = getIndex(name);
			if (index >= 0) {
				names.remove(index);
				values.remove(index);
			}
		}

		@Override
		public void removeAll(Collection<String> bindingNames) {
			namesSetCache = null;
			for (String name : bindingNames) {
				removeBinding(name);
			}
		}

		@Override
		public void retainAll(Collection<String> bindingNames) {
			namesSetCache = null;

			if (bindingNames.isEmpty()) {
				names.clear();
				values.clear();
			} else if (names.isEmpty()) {
				// no-op
			} else {
				Deque<Integer> indexesToRemove = new ArrayDeque<>();
				for (int i = 0; i < names.size(); i++) {
					if (!bindingNames.contains(names.get(i))) {
						indexesToRemove.addFirst(i);
					}
				}

				for (int i : indexesToRemove) {
					names.remove(i);
					values.remove(i);
				}

			}
		}

		private int getIndex(String name) {
			for (int i = 0; i < names.size(); i++) {
				if (names.get(i) == name) {
					return i;
				}
			}

			for (int i = 0; i < names.size(); i++) {
				if (names.get(i).equals(name)) {
					return i;
				}
			}

			return -1;
		}

	}

	static class EmptyBindingSet implements ModifiableBindingSet {

		BindingSet emptyBindingSet = org.eclipse.rdf4j.query.impl.EmptyBindingSet.getInstance();

		@Override
		public Iterator<Binding> iterator() {
			return emptyBindingSet.iterator();
		}

		@Override
		public Set<String> getBindingNames() {
			return emptyBindingSet.getBindingNames();
		}

		@Override
		public Binding getBinding(String bindingName) {
			return emptyBindingSet.getBinding(bindingName);
		}

		@Override
		public boolean hasBinding(String bindingName) {
			return emptyBindingSet.hasBinding(bindingName);
		}

		@Override
		public Value getValue(String bindingName) {
			return emptyBindingSet.getValue(bindingName);
		}

		@Override
		public int size() {
			return emptyBindingSet.size();
		}

		@Override
		public boolean equals(Object o) {
			return emptyBindingSet.equals(o);
		}

		@Override
		public int hashCode() {
			return emptyBindingSet.hashCode();
		}

		@Override
		public void forEach(Consumer<? super Binding> action) {
			emptyBindingSet.forEach(action);
		}

		@Override
		public Spliterator<Binding> spliterator() {
			return emptyBindingSet.spliterator();
		}

		@Override
		public void addAll(BindingSet bindingSet) {
			throw new IllegalStateException();
		}

		@Override
		public void addBinding(Binding binding) {
			throw new IllegalStateException();

		}

		@Override
		public void addBinding(String name, Value value) {
			throw new IllegalStateException();

		}

		@Override
		public void setBinding(Binding binding) {
			throw new IllegalStateException();

		}

		@Override
		public void setBinding(String name, Value value) {
			throw new IllegalStateException();

		}

		@Override
		public void removeBinding(String name) {
			throw new IllegalStateException();

		}

		@Override
		public void removeAll(Collection<String> bindingNames) {
			throw new IllegalStateException();

		}

		@Override
		public void retainAll(Collection<String> bindingNames) {
			throw new IllegalStateException();

		}
	}

}
