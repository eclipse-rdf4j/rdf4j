/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.collection.factory.impl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.eclipse.rdf4j.collection.factory.api.CollectionFactory;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.Value;

/**
 * A DefaultColelctionFactory that provides lists/sets/maps using standard common java in memory types
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
}
