/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.AbstractBindingSet;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.impl.SimpleBinding;
import org.eclipse.rdf4j.util.iterators.ConvertingIterator;

/**
 * An implementation of the {@link BindingSet} interface that is used to evalate query object models. This
 * implementations differs from {@link MapBindingSet} in that it maps variable names to Value objects and that the
 * Binding objects are created lazily.
 *
 * Also has an optimisation that it tries to use a very small map backed by a single array. And only switches to a
 * heavier hash map once we have more contents.
 * 
 */
public class QueryBindingSet extends AbstractBindingSet {

	private static final long serialVersionUID = -2010715346095527301L;

	private static final int SWITCH_TO_MAP = 16;

	private Map<String, Value> bindings;

	public QueryBindingSet() {
		this(8);
	}

	public QueryBindingSet(int capacity) {
		// Create bindings map with some extra space for new bindings and
		// compensating for HashMap's load factor
		if (capacity > SWITCH_TO_MAP) {
			bindings = new HashMap<String, Value>(capacity * 2);
		} else {
			bindings = new SmallMap();
		}
	}

	public QueryBindingSet(BindingSet bindingSet) {
		bindings = bindingsFrom(bindingSet);
	}

	private static Map<String, Value> bindingsFrom(BindingSet bindingSet) {
		if (bindingSet instanceof QueryBindingSet) {
			final Map<String, Value> map = ((QueryBindingSet) bindingSet).bindings;
			if (map instanceof SmallMap) {
				return new SmallMap((SmallMap) map);
			} else {
				return new HashMap<>(map);
			}
		} else {
			Map<String, Value> bindings;
//			if (bindingSet.size() > SWITCH_TO_MAP) {
			bindings = new HashMap<>(bindingSet.size() * 2);
//			} else {
//				bindings = new SmallMap();
//			}
			for (Binding binding : bindingSet) {
				bindings.put(binding.getName(), binding.getValue());
			}
			return bindings;
		}
	}

	public void addAll(BindingSet bindingSet) {
		for (Binding binding : bindingSet) {
			this.setBinding(binding.getName(), binding.getValue());
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
		try {
			bindings.put(name, value);
		} catch (RuntimeException e) {
			bindings = new HashMap<>(bindings);
			bindings.put(name, value);
		}
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

	private static class SmallMap
			extends AbstractMap<String, Value> {
		private final Object[] keysValues = new Object[SWITCH_TO_MAP];

		public SmallMap() {
		}

		public SmallMap(SmallMap map) {
			System.arraycopy(map.keysValues, 0, keysValues, 0, SWITCH_TO_MAP);
		}

		@Override
		public int size() {

			int size = 0;
			for (int i = 0; i < keysValues.length; i = i + 2) {
				if (keysValues[i] != null) {
					size++;
				}
			}
			return size;
		}

		private Iterator<Entry<String, Value>> iterator() {
			return new Iterator<Entry<String, Value>>() {
				int cursor = 0;

				@Override
				public boolean hasNext() {
					while (cursor < keysValues.length) {
						if (keysValues[cursor] != null && keysValues[cursor + 1] != null) {
							return true;
						} else {
							cursor += 2;
						}
					}
					return false;
				}

				@Override
				public Entry<String, Value> next() {
					String key = (String) keysValues[cursor++];
					int valueCursor = cursor;
					Value value = (Value) keysValues[cursor++];
					return new Entry<String, Value>() {

						@Override
						public String getKey() {
							return key;
						}

						@Override
						public Value getValue() {
							return value;
						}

						@Override
						public Value setValue(Value value) {
							Value old = (Value) keysValues[valueCursor];
							keysValues[valueCursor] = value;
							return old;
						}
					};
				}
			};
		}

		@Override
		public boolean isEmpty() {
			return size() == 0;
		}

		@Override
		public boolean containsKey(Object key) {
			for (int i = 0; i < keysValues.length; i = i + 2) {
				if (key.equals(keysValues[i])) {
					return true;
				}
			}
			return false;
		}

		@Override
		public Value get(Object key) {
			for (int i = 0; i < keysValues.length; i = i + 2) {
				if (key.equals(keysValues[i])) {
					return (Value) keysValues[i + 1];
				}
			}
			return null;
		}

		@Override
		public Value put(String key, Value value) {
			for (int i = 0; i < keysValues.length; i = i + 2) {
				if (key.equals(keysValues[i])) {
					keysValues[i] = key;
					Value oldValue = (Value) keysValues[i + 1];
					keysValues[i + 1] = value;
					return oldValue;
				}
			}
			for (int i = 0; i < keysValues.length; i = i + 2) {
				if (keysValues[i] == null) {
					keysValues[i] = key;
					keysValues[i + 1] = value;
					return null;
				}
			}
			throw new RuntimeException("Out of space");
		}

		@Override
		public Value remove(Object key) {
			for (int i = 0; i < keysValues.length; i = i + 2) {
				if (key.equals(keysValues[i])) {
					keysValues[i] = null;
					Value old = (Value) keysValues[i + 1];
					keysValues[i + 1] = null;
					return old;
				}
			}
			return null;
		}

		@Override
		public void clear() {
			Arrays.fill(keysValues, null);
		}

		@Override
		public Set<String> keySet() {
			Set<String> keys = new HashSet<>();
			for (int i = 0; i < keysValues.length; i = i + 2) {
				if (keysValues[i] != null) {
					keys.add((String) keysValues[i]);
				}
			}
			return keys;
		}

		@Override
		public Collection<Value> values() {
			List<Value> values = new ArrayList<>();
			for (int i = 1; i < keysValues.length; i = i + 2) {
				if (keysValues[i] != null) {
					values.add((Value) keysValues[i]);
				}
			}
			return values;
		}

		@Override
		public Set<Entry<String, Value>> entrySet() {
			SmallMap map = this;
			return new AbstractSet<Entry<String, Value>>() {

				@Override
				public int size() {
					return map.size();
				}

				@Override
				public boolean isEmpty() {
					return map.isEmpty();
				}

				@Override
				public boolean contains(Object o) {
					return map.containsKey(o);
				}

				@Override
				public Iterator<Entry<String, Value>> iterator() {
					return map.iterator();
				}

				@Override
				public boolean retainAll(Collection<?> c) {
					boolean changed = false;
					for (int i = 0; i < keysValues.length; i = i + 2) {
						if (keysValues[i] != null) {
							if (!c.contains(keysValues[i])) {
								keysValues[i] = null;
								keysValues[i + 1] = null;
								changed = true;
							}
						}
					}
					return changed;
				}

				@Override
				public boolean removeAll(Collection<?> c) {
					boolean changed = false;
					for (int i = 0; i < keysValues.length; i = i + 2) {
						if (keysValues[i] != null) {
							if (c.contains(keysValues[i])) {
								keysValues[i] = null;
								keysValues[i + 1] = null;
								changed = true;
							}
						}
					}
					return changed;
				}

				@Override
				public void clear() {
					Arrays.fill(keysValues, null);
				}
			};
		}

	}
}