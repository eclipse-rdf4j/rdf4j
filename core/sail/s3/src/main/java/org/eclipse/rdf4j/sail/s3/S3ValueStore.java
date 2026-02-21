/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.s3;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.AbstractValueFactory;

/**
 * In-memory value store that maps RDF {@link Value} objects to long IDs and vice-versa. Uses {@link ConcurrentHashMap}
 * for thread-safe bidirectional lookup.
 */
class S3ValueStore extends AbstractValueFactory {

	static final long UNKNOWN_ID = -1;

	private final ConcurrentHashMap<Value, Long> valueToId = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Long, Value> idToValue = new ConcurrentHashMap<>();
	private final AtomicLong nextId = new AtomicLong(1);

	/**
	 * Stores the supplied value and returns the ID assigned to it. If the value already exists, returns the existing
	 * ID.
	 *
	 * @param value the value to store
	 * @return the ID assigned to the value
	 */
	public long storeValue(Value value) {
		Long existing = valueToId.get(value);
		if (existing != null) {
			return existing;
		}
		long id = nextId.getAndIncrement();
		Long previous = valueToId.putIfAbsent(value, id);
		if (previous != null) {
			// another thread stored it first
			return previous;
		}
		idToValue.put(id, value);
		return id;
	}

	/**
	 * Gets the ID for the specified value.
	 *
	 * @param value a value
	 * @return the ID for the value, or {@link #UNKNOWN_ID} if not found
	 */
	public long getId(Value value) {
		Long id = valueToId.get(value);
		return id != null ? id : UNKNOWN_ID;
	}

	/**
	 * Gets the value for the specified ID.
	 *
	 * @param id a value ID
	 * @return the value, or {@code null} if not found
	 */
	public Value getValue(long id) {
		return idToValue.get(id);
	}

	/**
	 * Removes all stored values and resets the ID counter.
	 */
	public void clear() {
		valueToId.clear();
		idToValue.clear();
		nextId.set(1);
	}

	/**
	 * Closes the value store, releasing all resources.
	 */
	public void close() {
		clear();
	}
}
