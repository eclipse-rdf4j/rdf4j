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

import java.util.ArrayList;
import java.util.List;

public class MinusIterationTest extends CloseableIterationTest {

	@Override
	protected CloseableIteration<String, Exception> createTestIteration() {
		return new MinusIteration<>(createStringList1Iteration(), createStringList2Iteration());
	}

	@Override
	protected int getTestIterationSize() {
		List<String> difference = new ArrayList<>(stringList1);
		difference.removeAll(stringList2);
		return difference.size();
	}
}
