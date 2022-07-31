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
package org.eclipse.rdf4j.sail.model;

import java.util.Map;
import java.util.UUID;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.util.UUIDable;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * A registry to support (de)serialization of objects (over the lifetime of the VM). It uses weak references to allow
 * entries to be garbage-collected when no longer used.
 *
 * @author Mark
 * @apiNote this feature is for internal use only: its existence, signature or behavior may change without warning from
 *          one release to the next.
 */
@InternalUseOnly
public class NonSerializables {

	private static final Cache<UUID, Object> registry = CacheBuilder.newBuilder().weakValues().build();

	/**
	 * Retrieve the object registered with the supplied key.
	 *
	 * @param key the key.
	 * @return the registered object, or <code>null</code> if no matching EvaluationStrategy can be found.
	 */
	public static Object get(UUID key) {
		return registry.getIfPresent(key);
	}

	/**
	 * Retrieves the registry key for the given object.
	 *
	 * @param obj the object for which to retrieve the registry key.
	 * @return the registry key with which the supplied object can be retrieved, or <code>null</code> if the supplied
	 *         object is not in the registry.
	 */
	public static UUID getKey(Object obj) {
		Map<UUID, Object> map = registry.asMap();

		// we could make this lookup more efficient with a WeakHashMap-based
		// reverse index, but we currently prefer this slower but more robust
		// approach (less chance of accidental lingering references that prevent
		// GC)
		for (UUID key : map.keySet()) {
			// we use identity comparison in line with how guava caches behave
			// when softValues are used.
			if (obj == map.get(key)) {
				return key;
			}
		}
		return null;
	}

	/**
	 * Add an object to the registry and returns the registry key. If the object is already present, the operation
	 * simply returns the key with which it is currently registered.
	 *
	 * @param obj the object to register
	 * @return the key with which the object is registered.
	 */
	public static UUID register(Object obj) {
		UUID key;
		if (obj instanceof UUIDable) {
			key = ((UUIDable) obj).getUUID();
			if (get(key) == null) {
				registry.put(key, obj);
			}
		} else {
			key = getKey(obj);
			if (key == null) {
				key = UUID.randomUUID();
				registry.put(key, obj);
			}
		}
		return key;
	}

	/**
	 * Prevent instantiation: util class
	 */
	private NonSerializables() {
	}
}
