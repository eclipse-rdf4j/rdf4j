/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.collection.factory.impl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import org.eclipse.rdf4j.collection.factory.api.BindingSetKey;
import org.eclipse.rdf4j.collection.factory.api.CollectionFactory;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;

/**
 * A DefaultCollectionFactory that provides lists/sets/maps using standard common java in memory types
 */
public class DefaultCollectionFactory implements CollectionFactory {

	@Override
	public <T> List<T> createList() {
		return new ArrayList<T>();
	}

	@Override
	public List<Value> createValueList() {
		return new ArrayList<>();
	}

	@Override
	public <T> Set<T> createSet() {
		return new HashSet<T>();
	}

	@Override
	public Set<Value> createValueSet() {
		return new HashSet<>();
	}

	@Override
	public Set<BindingSet> createSetOfBindingSets(Supplier<MutableBindingSet> create,
			Function<String, Predicate<BindingSet>> getHas, Function<String, Function<BindingSet, Value>> getget,
			Function<String, BiConsumer<Value, MutableBindingSet>> getSet) {
		return new HashSet<>();
	}

	@Override
	public <K, V> Map<K, V> createMap() {
		return new HashMap<K, V>();
	}

	@Override
	public <V> Map<Value, V> createValueKeyedMap() {
		return new HashMap<Value, V>();
	}

	@Override
	public <T> Queue<T> createQueue() {
		return new ArrayDeque<T>();
	}

	@Override
	public Queue<Value> createValueQueue() {
		return new ArrayDeque<Value>();
	}

	@Override
	public void close() throws RDF4JException {
		// Nothing to do here.
	}

	@Override
	public <E> Map<BindingSetKey, E> createGroupByMap() {
		return new LinkedHashMap<>();
	}

	@Override
	public BindingSetKey createBindingSetKey(BindingSet bindingSet, List<Function<BindingSet, Value>> getValues,
			ToIntFunction<BindingSet> hashOfBindingSetCalculator) {
		List<Value> values = new ArrayList<>(getValues.size());
		for (int i = 0; i < getValues.size(); i++) {
			values.add(getValues.get(i).apply(bindingSet));
		}
		return new DefaultBindingSetKey(values, hashOfBindingSetCalculator.applyAsInt(bindingSet));
	}
}
