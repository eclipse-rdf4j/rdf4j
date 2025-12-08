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

package org.eclipse.rdf4j.model.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

/**
 * Reproduces overflow in SimpleValueFactory#createBNode() when the atomic counter wraps to Long.MIN_VALUE, which
 * results in a negative index into the RANDOMIZE_LENGTH array and throws ArrayIndexOutOfBoundsException.
 */
public class SimpleValueFactoryOverflowTest {

	@Test
	void overflowAtMinValue() throws Exception {
		// Access the private static counter
		Field f = SimpleValueFactory.class.getDeclaredField("uniqueIdSuffix");
		f.setAccessible(true);
		AtomicLong counter = (AtomicLong) f.get(null);

		// Preserve original value to avoid leaking state across tests
		long original = counter.get();

		synchronized (SimpleValueFactory.class) {
			try {
				// Force next increment to wrap from Long.MAX_VALUE to Long.MIN_VALUE
				counter.set(Long.MAX_VALUE);

				SimpleValueFactory.getInstance().createBNode();
			} finally {
				// Restore the original value
				counter.set(original);
			}
		}
	}
}
