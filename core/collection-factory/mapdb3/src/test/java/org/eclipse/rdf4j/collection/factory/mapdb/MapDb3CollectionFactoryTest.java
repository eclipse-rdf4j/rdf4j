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
package org.eclipse.rdf4j.collection.factory.mapdb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

import org.junit.jupiter.api.Test;

public class MapDb3CollectionFactoryTest {

	@Test
	void queuOfferAnd() {
		try (MapDb3CollectionFactory mapDb3CollectionFactory = new MapDb3CollectionFactory(1)) {
			Queue<String> q = mapDb3CollectionFactory.createQueue();
			int size = 1024;
			for (int i = 0; i < size; i++) {
				assertTrue(q.offer(Integer.toString(i)));
			}
			assertEquals(size, q.size());
			for (int i = 0; i < size; i++) {
				String p = q.peek();
				assertEquals(p, Integer.toString(i));
				String p2 = q.peek();
				assertEquals(p2, Integer.toString(i));
				String s = q.poll();
				assertEquals(s, Integer.toString(i));
			}
			assertEquals(0, q.size());
		}
	}

	@Test
	void iterator() {
		try (MapDb3CollectionFactory mapDb3CollectionFactory = new MapDb3CollectionFactory(1)) {
			Queue<String> q = mapDb3CollectionFactory.createQueue();
			int size = 1024;
			for (int i = 0; i < size; i++) {
				assertTrue(q.offer(Integer.toString(i)));
			}
			assertEquals(size, q.size());
			Iterator<String> iter = q.iterator();
			for (int i = 0; i < size; i++) {
				assertTrue(iter.hasNext());
				assertEquals(iter.next(), Integer.toString(i));
			}
			assertFalse(iter.hasNext());
			assertEquals(size, q.size());
			try {
				iter.next();
				fail();
			} catch (NoSuchElementException e) {
				assertNotNull(e);
			}
		}
	}
}
