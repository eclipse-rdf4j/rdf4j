/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.common.iteration;

import java.util.HashSet;
import java.util.Set;

public class DistinctIntersectionIterationTest extends CloseableIterationTest {

	@Override
	protected CloseableIteration<String, Exception> createTestIteration() {
		return new IntersectIteration<String, Exception>(createStringList1Iteration(),
				createStringList2Iteration(), true);
	}

	@Override
	protected int getTestIterationSize() {
		Set<String> intersection = new HashSet<String>(stringList1);
		intersection.retainAll(stringList2);
		return intersection.size();
	}
}
