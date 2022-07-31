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

import java.util.Arrays;
import java.util.List;

public class ConvertingIterationTest extends CloseableIterationTest {

	private static final List<Integer> intList = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

	protected static CloseableIteration<String, Exception> createConvertingIteration() {
		Iteration<Integer, Exception> intIteration = new CloseableIteratorIteration<>(intList.iterator());
		return new ConvertingIteration<Integer, String, Exception>(intIteration) {

			@Override
			protected String convert(Integer integer) {
				return integer.toString();
			}
		};
	}

	@Override
	protected CloseableIteration<String, Exception> createTestIteration() {
		return createConvertingIteration();
	}

	@Override
	protected int getTestIterationSize() {
		return 10;
	}
}
