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

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UnionIterationTest extends CloseableIterationTest {

	@Override
	protected CloseableIteration<String, Exception> createTestIteration() {
		return new UnionIteration<>(createStringList1Iteration(), createStringList2Iteration());
	}

	@Override
	protected int getTestIterationSize() {
		return stringList1.size() + stringList2.size();
	}

	@Test
	public void testArgumentsClosed() throws Exception {
		SingletonIteration<String, Exception> iter1 = new SingletonIteration<>("1");
		SingletonIteration<String, Exception> iter2 = new SingletonIteration<>("2");
		SingletonIteration<String, Exception> iter3 = new SingletonIteration<>("3");
		try (UnionIteration<String, Exception> unionIter = new UnionIteration<>(iter1, iter2, iter3)) {
			unionIter.next();
		}

		assertTrue("iter1 should have been closed", iter1.isClosed());
		assertTrue("iter2 should have been closed", iter2.isClosed());
		assertTrue("iter3 should have been closed", iter3.isClosed());
	}
}
