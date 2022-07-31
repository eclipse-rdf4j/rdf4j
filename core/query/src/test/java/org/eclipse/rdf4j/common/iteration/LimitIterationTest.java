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

import java.util.List;

import org.junit.Test;

public class LimitIterationTest extends CloseableIterationTest {

	protected static LimitIteration<String, Exception> createLimitIteration(int limit) {
		return new LimitIteration<>(createStringList1Iteration(), limit);
	}

	@Override
	protected CloseableIteration<String, Exception> createTestIteration() {
		return createLimitIteration(5);
	}

	@Override
	protected int getTestIterationSize() {
		return 5;
	}

	@Test
	public void testInRangeOffset() throws Exception {
		for (int limit = 0; limit < stringList1.size(); limit++) {
			Iteration<String, Exception> iter = createLimitIteration(limit);
			List<String> resultList = Iterations.asList(iter);
			List<String> expectedList = stringList1.subList(0, limit);
			assertEquals("testInRangeOffset failed for limit: " + limit, expectedList, resultList);
		}
	}

	@Test
	public void testOutOfRangeOffset() throws Exception {
		Iteration<String, Exception> iter = createLimitIteration(2 * stringList1.size());
		List<String> resultList = Iterations.asList(iter);
		assertEquals(stringList1, resultList);
	}
}
