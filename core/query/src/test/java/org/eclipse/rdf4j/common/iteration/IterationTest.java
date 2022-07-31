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
package org.eclipse.rdf4j.common.iteration;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public abstract class IterationTest {

	protected static final List<String> stringList1 = Arrays.asList("1", "2", "3", "4", "5", "1", "2", "3", "4", "5");

	protected static final List<String> stringList2 = Arrays.asList("4", "5", "6", "7", "8");

	protected static CloseableIteration<String, Exception> createStringList1Iteration() {
		return new CloseableIteratorIteration<>(stringList1.iterator());
	}

	protected static CloseableIteration<String, Exception> createStringList2Iteration() {
		return new CloseableIteratorIteration<>(stringList2.iterator());
	}

	protected abstract Iteration<String, Exception> createTestIteration() throws Exception;

	protected abstract int getTestIterationSize();

	@Test
	public void testFullIteration() throws Exception {
		Iteration<String, Exception> iter = createTestIteration();
		int count = 0;

		while (iter.hasNext()) {
			iter.next();
			count++;
		}

		assertEquals("test iteration contains incorrect number of elements", getTestIterationSize(), count);
	}
}
