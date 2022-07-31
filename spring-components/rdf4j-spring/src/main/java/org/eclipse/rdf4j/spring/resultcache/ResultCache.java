/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.spring.resultcache;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 * @param <K>
 * @param <T>
 */
public interface ResultCache<K, T> extends Clearable {

	T get(K key);

	void put(K key, T cachedObject);

	/**
	 * Calling this method instructs the cache to return <code>null</code> to all {@link #get(K)} calls and ignore any
	 * {@link #put(K, T)} calls from the current thread until the cache is cleared. Context: after a write operation on
	 * a connection (which is assumed to be handled exclusively by a dedicated thread), the local cache must be cleared
	 * and the global cache bypassed until the connection is returned.
	 */
	void bypassForCurrentThread();
}
