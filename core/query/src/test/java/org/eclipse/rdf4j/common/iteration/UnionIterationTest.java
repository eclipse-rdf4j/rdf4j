/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.common.iteration;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.junit.Test;

public class UnionIterationTest extends CloseableIterationTest {

	@Override
	protected CloseableIteration<? extends String, Exception> createTestIteration() {
		return UnionIteration.getInstance(createStringList1Iteration(), createStringList2Iteration());
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
		try (CloseableIteration<String, Exception> unionIter = UnionIteration
				.getInstance(List.of(iter1, iter2, iter3))) {
			unionIter.next();
		}

		assertFalse(iter1.hasNext(), "iter1 should have been closed");
		assertFalse(iter2.hasNext(), "iter2 should have been closed");
		assertFalse(iter3.hasNext(), "iter3 should have been closed");
	}
}
