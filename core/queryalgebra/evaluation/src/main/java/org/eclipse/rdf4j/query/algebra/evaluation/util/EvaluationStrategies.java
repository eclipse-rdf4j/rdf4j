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
package org.eclipse.rdf4j.query.algebra.evaluation.util;

import java.util.Map;
import java.util.UUID;

import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.util.UUIDable;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Registry for currently active {@link EvaluationStrategy} objects. The internal registry uses soft references to allow
 * entries to be garbage-collected when no longer used. Currently, the primary purpose of this is to support
 * (de)serialization of objects (over the lifetime of the VM) that depend on an EvaluationStrategy
 *
 * @author Jeen Broekstra
 */
@Deprecated(forRemoval = true, since = "4.0.0")
public class EvaluationStrategies {

	private static final Cache<UUID, EvaluationStrategy> registry = CacheBuilder.newBuilder().weakValues().build();

	/**
	 * Retrieve the EvaluationStrategy registered with the supplied key.
	 *
	 * @param key the key
	 * @return the registered EvaluationStrategy, or <code>null</code> if no matching EvaluationStrategy can be found.
	 */
	public static EvaluationStrategy get(UUID key) {
		return registry.getIfPresent(key);
	}

	/**
	 * Retrieve the registry key for the given EvaluationStrategy
	 *
	 * @param strategy the EvaluationStrategy for which to retrieve the registry key
	 * @return the registry key with which the supplied strategy can be retrieved, or <code>null</code> if the supplied
	 *         strategy is not in the registry.
	 */
	public static UUID getKey(EvaluationStrategy strategy) {
		Map<UUID, EvaluationStrategy> map = registry.asMap();

		// we could make this lookup more efficient with a WeakHashMap-based
		// reverse index, but we currently prefer this slower but more robust
		// approach (less chance of accidental lingering references that prevent
		// GC)
		for (UUID key : map.keySet()) {
			// we use identity comparison in line with how guava caches behave
			// when softValues are used.
			if (strategy == map.get(key)) {
				return key;
			}
		}
		return null;
	}

	/**
	 * Add a strategy to the registry and returns the registry key. If the strategy is already present, the operation
	 * simply returns the key with which it is currently registered.
	 *
	 * @param strategy the EvaluationStrategy to register
	 * @return the key with which the strategy is registered.
	 */
	public static UUID register(EvaluationStrategy strategy) {
		UUID key;
		if (strategy instanceof UUIDable) {
			key = ((UUIDable) strategy).getUUID();
			if (get(key) == null) {
				registry.put(key, strategy);
			}
		} else {
			key = getKey(strategy);
			if (key == null) {
				key = UUID.randomUUID();
				registry.put(key, strategy);
			}
		}
		return key;
	}

	/**
	 * Prevent instantiation: util class
	 */
	private EvaluationStrategies() {
	}
}
