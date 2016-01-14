/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.util;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;

import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A utility class to perform operations on {@link Namespace}s.
 * 
 * @author Peter Ansell
 * @since 2.7.0
 */
public class Namespaces {

	/**
	 * Converts a set of {@link Namespace}s into a map containing the
	 * {@link Namespace#getPrefix()} strings as keys, with the
	 * {@link Namespace#getName()} strings as values in the map for each
	 * namespace in the given set.
	 * 
	 * @param namespaces
	 *        The {@link Set} of {@link Namespace}s to transform.
	 * @return A {@link Map} of {@link String} to {@link String} where the
	 *         key/value combinations are created based on the prefix and names
	 *         from {@link Namespace}s in the input set.
	 * @since 2.7.0
	 */
	public static Map<String, String> asMap(Set<Namespace> namespaces) {
		Map<String, String> result = new HashMap<String, String>();

		for (Namespace nextNamespace : namespaces) {
			result.put(nextNamespace.getPrefix(), nextNamespace.getName());
		}

		return result;
	}

	/**
	 * Wraps the given {@link Set} of {@link Namespace}s as a {@link Map} of
	 * prefix to URI mappings, so that it can be used where a {@link Map} is
	 * required by the API. <br>
	 * NOTE: The Map returned by this method is not synchronized.
	 * 
	 * @param namespaces
	 *        The Set to wrap.
	 * @return A Map of prefix to URI mappings which is backed by the given Set
	 *         of {@link Namespace}s.
	 * @since 2.7.10
	 */
	public static Map<String, String> wrap(final Set<Namespace> namespaces) {
		return new Map<String, String>() {

			@Override
			public void clear() {
				namespaces.clear();
			}

			@Override
			public boolean containsKey(Object nextKey) {
				if (nextKey instanceof String) {
					for (Namespace nextNamespace : namespaces) {
						if (nextNamespace.getPrefix().equals(nextKey)) {
							return true;
						}
					}
				}
				return false;
			}

			@Override
			public boolean containsValue(Object nextValue) {
				if (nextValue instanceof String) {
					for (Namespace nextNamespace : namespaces) {
						if (nextNamespace.getName().equals(nextValue)) {
							return true;
						}
					}
				}
				return false;
			}

			/**
			 * NOTE: This entry set is immutable, and does not update the internal
			 * set through its iterator.
			 */
			@Override
			public Set<java.util.Map.Entry<String, String>> entrySet() {
				Set<java.util.Map.Entry<String, String>> result = new LinkedHashSet<Map.Entry<String, String>>();
				for (Namespace nextNamespace : namespaces) {
					AbstractMap.SimpleImmutableEntry<String, String> nextEntry = new SimpleImmutableEntry<String, String>(
							nextNamespace.getPrefix(), nextNamespace.getName());
					result.add(nextEntry);
				}
				return Collections.unmodifiableSet(result);
			}

			@Override
			public String get(Object nextKey) {
				if (nextKey instanceof String) {
					for (Namespace nextNamespace : namespaces) {
						if (nextNamespace.getPrefix().equals(nextKey)) {
							return nextNamespace.getName();
						}
					}
				}
				return null;
			}

			@Override
			public boolean isEmpty() {
				return namespaces.isEmpty();
			}

			@Override
			public Set<String> keySet() {
				Set<String> result = new LinkedHashSet<String>();
				for (Namespace nextNamespace : namespaces) {
					result.add(nextNamespace.getPrefix());
				}
				return result;
			}

			@Override
			public String put(String nextKey, String nextValue) {
				String result = null;
				for (Namespace nextNamespace : new LinkedHashSet<Namespace>(namespaces)) {
					if (nextNamespace.getPrefix().equals(nextKey)) {
						result = nextNamespace.getName();
						namespaces.remove(nextNamespace);
					}
				}
				namespaces.add(new SimpleNamespace(nextKey, nextValue));
				return result;
			}

			@Override
			public void putAll(Map<? extends String, ? extends String> nextSet) {
				for (Map.Entry<? extends String, ? extends String> nextEntry : nextSet.entrySet()) {
					put(nextEntry.getKey(), nextEntry.getValue());
				}
			}

			@Override
			public String remove(Object nextKey) {
				String result = null;
				for (Namespace nextNamespace : new LinkedHashSet<Namespace>(namespaces)) {
					if (nextNamespace.getPrefix().equals(nextKey)) {
						result = nextNamespace.getName();
						namespaces.remove(nextNamespace);
					}
				}
				return result;
			}

			@Override
			public int size() {
				return namespaces.size();
			}

			@Override
			public Collection<String> values() {
				List<String> result = new ArrayList<String>();
				for (Namespace nextNamespace : namespaces) {
					result.add(nextNamespace.getName());
				}
				return result;
			}
		};
	}

	private Namespaces() {
		// private default constructor, this is a static class
	}

}
