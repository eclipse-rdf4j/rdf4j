package org.eclipse.rdf4j.model.collections;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

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
	public <T extends Value> List<T> createValueList() {
		return new ArrayList<T>();
	}

	@Override
	public <T> Set<T> createSet() {
		return new HashSet<T>();
	}

	@Override
	public <T extends Value> Set<T> createValueSet() {
		return new HashSet<T>();
	}

	@Override
	public <K, V> Map<K, V> createMap() {
		return new HashMap<K, V>();
	}

	@Override
	public <K extends Value, V> Map<K, V> createValueKeyedMap() {
		return new HashMap<K, V>();
	}

	@Override
	public <T> Queue<T> createQueue() {
		return new ArrayDeque<T>();
	}

	@Override
	public <T extends Value> Queue<T> createValueQueue() {
		return new ArrayDeque<T>();
	}
}
