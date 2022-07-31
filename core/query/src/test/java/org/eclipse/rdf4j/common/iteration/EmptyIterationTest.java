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

public class EmptyIterationTest extends CloseableIterationTest {

	protected static EmptyIteration<String, Exception> createEmptyIteration() {
		return new EmptyIteration<>();
	}

	@Override
	protected CloseableIteration<String, Exception> createTestIteration() {
		return createEmptyIteration();
	}

	@Override
	protected int getTestIterationSize() {
		return 0;
	}
}
