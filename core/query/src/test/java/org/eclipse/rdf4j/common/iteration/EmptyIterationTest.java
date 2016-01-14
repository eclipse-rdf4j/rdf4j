/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.common.iteration;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;

public class EmptyIterationTest extends CloseableIterationTest {

	protected static EmptyIteration<String, Exception> createEmptyIteration() {
		return new EmptyIteration<String, Exception>();
	}

	@Override
	protected CloseableIteration<String, Exception> createTestIteration()
	{
		return createEmptyIteration();
	}

	@Override
	protected int getTestIterationSize()
	{
		return 0;
	}
}
