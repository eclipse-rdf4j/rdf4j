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
import java.util.HashSet;
import java.util.List;

public class IntersectionIterationTest extends CloseableIterationTest {

	@Override
	protected CloseableIteration<String, Exception> createTestIteration() {
		return new IntersectIteration<>(createStringList1Iteration(), createStringList2Iteration(), false,
				new HashSet<>());
	}

	@Override
	protected int getTestIterationSize() {
		List<String> intersection = new ArrayList<>(stringList1);
		intersection.retainAll(stringList2);
		return intersection.size();
	}
}
