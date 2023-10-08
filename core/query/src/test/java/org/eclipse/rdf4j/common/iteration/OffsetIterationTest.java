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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

public class OffsetIterationTest extends CloseableIterationTest {

	protected static OffsetIteration<String> createOffsetIteration(int offset) {
		return new OffsetIteration<>(createStringList1Iteration(), offset);
	}

	@Override
	protected CloseableIteration<String> createTestIteration() {
		return createOffsetIteration(5);
	}

	@Override
	protected int getTestIterationSize() {
		return 5;
	}

	@Test
	public void testInRangeOffset() throws Exception {
		for (int offset = 0; offset < stringList1.size(); offset++) {
			CloseableIteration<String> iter = createOffsetIteration(offset);
			List<String> resultList = Iterations.asList(iter);
			List<String> expectedList = stringList1.subList(offset, stringList1.size());
			assertEquals(expectedList, resultList, "test failed for offset: " + offset);
		}
	}

	@Test
	public void testOutOfRangeOffset() throws Exception {
		CloseableIteration<String> iter = createOffsetIteration(2 * stringList1.size());
		List<String> resultList = Iterations.asList(iter);
		assertEquals(Collections.emptyList(), resultList);
	}
}
