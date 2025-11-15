/**
 *******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.common.iteration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class QueueIterationHandleCloseTest {

	private static final class TestQI extends QueueIteration<Object, RuntimeException> {
		TestQI(int capacity) {
			super(capacity);
		}

		@Override
		protected RuntimeException convert(Exception e) {
			return new RuntimeException(e);
		}

		@Override
		public Object getNextElement() {
			return null;
		}
	}

	@Test
	void closeDrainsAndSignalsWithoutBlocking() throws Exception {
		TestQI qi = new TestQI(1);

		// Enqueue a single element so the queue is full
		qi.put(new Object());

		// Close should not block and should make the iteration report no further elements
		long start = System.nanoTime();
		qi.close();
		long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

		assertThat(elapsedMs).isLessThan(250);
		assertThat(qi.hasNext()).isFalse();
	}
}
