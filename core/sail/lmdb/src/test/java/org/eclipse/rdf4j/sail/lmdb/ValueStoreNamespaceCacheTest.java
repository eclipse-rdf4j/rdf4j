/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ValueStoreNamespaceCacheTest {

	@Test
	void getNamespaceUsesLastResult(@TempDir File dataDir) throws Throwable {
		ValueStore valueStore = new ValueStore(new File(dataDir, "values"), new LmdbStoreConfig());
		try {
			MethodHandles.Lookup privateLookup = MethodHandles.privateLookupIn(ValueStore.class,
					MethodHandles.lookup());
			TestConcurrentCache cache = new TestConcurrentCache(32);
			Field namespaceCacheField = ValueStore.class.getDeclaredField("namespaceCache");
			namespaceCacheField.setAccessible(true);
			namespaceCacheField.set(valueStore, cache);

			MethodHandle getNamespace = privateLookup.findVirtual(ValueStore.class, "getNamespace",
					MethodType.methodType(String.class, long.class));

			String namespace = "http://example.com/";
			long id = 123L;
			cache.put(id, namespace);
			String first = (String) getNamespace.invoke(valueStore, id);
			assertEquals(namespace, first);
			cache.failOnFurtherGets();

			String second = (String) getNamespace.invoke(valueStore, id);
			assertEquals(namespace, second);
			assertEquals(1, cache.getInvocations());
		} finally {
			valueStore.close();
		}
	}

	private static final class TestConcurrentCache extends ConcurrentCache<Long, String> {

		private final AtomicInteger invocations = new AtomicInteger();
		private volatile boolean failOnFurtherGets;

		private TestConcurrentCache(int capacity) {
			super(capacity);
		}

		@Override
		public String get(Object key) {
			int count = invocations.incrementAndGet();
			if (failOnFurtherGets && count > 1) {
				throw new AssertionError("namespaceCache#get must not be invoked after caching last namespace");
			}
			return super.get(key);
		}

		private void failOnFurtherGets() {
			failOnFurtherGets = true;
		}

		private int getInvocations() {
			return invocations.get();
		}
	}
}
